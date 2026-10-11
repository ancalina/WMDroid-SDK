package me.ancal.wmdroid.sdk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Text-only C8994/E2984 wrapping with owner-supplied font measurement. Coordinates
 * are native logical units; this does not provide GDI-equivalent Android shaping.
 * Type3/7 objects and extended styles fail explicitly. See notes-pwi-wrap-native-evidence.json.
 */
public final class WmPwiTextLayout {
    private WmPwiTextLayout() {}

    public interface Measurer {
        /**
         * availableWidth can be negative. Integer.MAX_VALUE requests unlimited measurement.
         * BD1C8 negative-limit empty fit may return width=availableWidth; other widths are nonnegative.
         * Metrics are native unsigned-byte values0..255. A zero fit is valid; paragraph progress is separate.
         */
        Measurement measure(String text, WmPwiDocument.CharacterFormat format, int availableWidth) throws IOException;
    }
    public static final class Measurement {
        public final int fittedCharacters, width, ascent, descent;
        public Measurement(int fittedCharacters, int width, int ascent, int descent) {
            this.fittedCharacters = fittedCharacters;
            this.width = width;
            this.ascent = ascent;
            this.descent = descent;
        }
    }
    /** Glyph reconstruction is deliberately owner-owned; counts include invisible native tokens. */
    public static final class Line {
        public final int logicalStart, logicalCount, width, ascent, descent;
        private Line(int start, int count, int width, int ascent, int descent) {
            logicalStart = start; logicalCount = count; this.width = width;
            this.ascent = ascent; this.descent = descent;
        }
    }

    public static List<Line> layout(WmPwiDocument.Paragraph paragraph, int mode, int frameLeft,
                                    int frameWidth, boolean extendedBreaks, Measurer measurer) throws IOException {
        if (paragraph == null || measurer == null || frameWidth < 0)
            throw new IllegalArgumentException("Paragraph, measurer and nonnegative frame width required");
        List<WmPwiDocument.Token> tokens = paragraph.getTokens();
        int total = paragraph.logicalCharacterCount;
        int[] tokenAt = new int[total];
        WmPwiDocument.CharacterFormat[] formats = new WmPwiDocument.CharacterFormat[total];
        WmPwiDocument.Token[] logicalTokens = new WmPwiDocument.Token[total];
        WmPwiDocument.CharacterFormat format = WmPwiDocument.CharacterFormat.initial();
        int logical = 0;
        for (int i = 0; i < tokens.size(); i++) {
            WmPwiDocument.Token token = tokens.get(i);
            if (token.nativeType == 5) format = format.apply(token);
            else {
                if (token.nativeType == 3 || token.nativeType == 7)
                    throw new IOException("PWI object/field measurement unsupported: " + token.nativeType);
                if (token.nativeType != 1 && token.nativeType != 2 && token.nativeType != 4 && token.nativeType != 6)
                    throw new IOException("PWI layout token unsupported: " + token.nativeType);
                tokenAt[logical] = i; formats[logical] = format; logicalTokens[logical++] = token;
            }
        }
        List<Line> lines = new ArrayList<Line>();
        int start = 0;
        while (start < total) {
            int origin = WmPwiLayout.wrapStart(paragraph, mode, lines.size(), frameLeft, frameWidth);
            long available = (long) frameWidth - paragraph.getRightIndentNative() - origin;
            if (available > Integer.MAX_VALUE) throw new IOException("PWI available width overflows");
            int width = (int) Math.max(0L, available);
            int absoluteOrigin = add(frameLeft, origin);
            Line line = scan(tokens, tokenAt[start], formats[start], start, total, absoluteOrigin,
                    frameLeft, width, extendedBreaks, measurer);
            if (extendedBreaks) {
                int boundary = start + line.logicalCount;
                int revised = boundary;
                while (revised > start) {
                    boolean closing = revised < total && forbidden(logicalTokens[revised], false);
                    boolean opening = forbidden(logicalTokens[revised - 1], true);
                    if (!closing && !opening) break;
                    revised--;
                }
                // E2984 retains the original split if punctuation correction would empty the line.
                if (revised != start && revised != boundary)
                    line = scan(tokens, tokenAt[start], formats[start], start, revised, absoluteOrigin,
                            frameLeft, width, true, measurer);
            }
            lines.add(line);
            start += line.logicalCount;
        }
        return Collections.unmodifiableList(lines);
    }

    private static Line scan(List<WmPwiDocument.Token> tokens, int tokenIndex,
                             WmPwiDocument.CharacterFormat format, int start, int end,
                             int origin, int frameLeft, int available, boolean extended,
                             Measurer measurer) throws IOException {
        State s = new State(start, end, available, measurer);
        boolean space = false, previousSpace = false, high = false, previousHigh = false;
        int logical = start;
        while (logical < end) {
            WmPwiDocument.Token token = tokens.get(tokenIndex++);
            int type = token.nativeType;
            int beforePending = s.pending;
            if (type != 5) {
                previousSpace = space;
                previousHigh = high;
                space = whitespace(token);
                if (extended) high = (type == 1 || type == 2) && token.character >= 0x7f;
                if (space != previousSpace || (extended && high != previousHigh)) {
                    if (s.buffer.length() != 0) {
                        int count = s.buffer.length();
                        Measurement m = s.flush(format);
                        if (m.fittedCharacters < count) {
                            if (!s.boundary || (extended && previousHigh)) {
                                s.count = logical - start - count + m.fittedCharacters;
                                s.width = add(s.width, s.pending);
                            }
                            if (!space && (!extended || (!high && !previousHigh))) s.count = logical - start;
                            return s.finish();
                        }
                    }
                    if (s.count != 0 && (space || (extended && high && !previousHigh))) {
                        if ((long)s.width + s.pending >= available) return s.finish();
                        s.boundary = true;
                    }
                    s.count = logical - start;
                    if (!previousSpace) {
                        s.boundary = true;
                        s.width = add(s.width, s.pending);
                        s.pending = 0;
                    }
                }
            }
            if (s.buffer.length() >= 128 || (type != 1 && type != 2 && s.buffer.length() != 0)) {
                int count = s.buffer.length();
                Measurement m = s.flush(format);
                if (m.fittedCharacters < count) {
                    if (!s.boundary || (extended && previousHigh)) {
                        s.count = logical - start - count + m.fittedCharacters;
                        s.width = add(s.width, s.pending);
                        return s.finish();
                    }
                    if (!space) return s.finish();
                }
                if (s.count == 0) {
                    s.count = logical - start;
                    s.width = add(s.width, s.pending);
                    s.pending = 0;
                }
            }
            if (type == 1 || type == 2) s.buffer.append(token.character);
            else if (type == 4) {
                byte[] raw = token.getEncodedBytes();
                s.pending = add(s.pending, (raw[1] & 255) | ((raw[2] & 255) << 8));
            } else if (type == 5) format = format.apply(token);
            else if (type == 6 && token.controlCode != 0)
                s.pending = add(s.pending, WmPwiLayout.controlAdvance(token.controlCode,
                        add(origin, add(s.width, s.pending)), origin, frameLeft));
            if (s.buffer.length() == 0 && (long)s.width + s.pending > available && !space) {
                if (s.count == 0) { s.count = 1; s.width = add(s.width, s.pending); }
                else if (!previousSpace) { s.count = logical - start; s.width = add(s.width, beforePending); }
                return s.finish();
            }
            if (type != 5) logical++;
        }
        if (s.buffer.length() != 0) {
            int count = s.buffer.length();
            Measurement m = s.flush(format);
            if (m.fittedCharacters < count) {
                if (s.boundary && (!extended || !previousHigh)) return s.finish();
                s.count = logical - start - count + m.fittedCharacters;
                s.width = add(s.width, s.pending);
                return s.finish();
            }
        }
        if (s.ascent == 0) s.metrics(measure(measurer, " ", format, Integer.MAX_VALUE));
        s.count = logical - start;
        s.width = add(s.width, s.pending);
        return s.finish();
    }

    private static final class State {
        final int start, end, available;
        final Measurer measurer;
        final StringBuilder buffer = new StringBuilder(128);
        int count, width, pending, ascent, descent;
        boolean boundary;
        State(int start, int end, int available, Measurer measurer) {
            this.start=start; this.end=end; this.available=available; this.measurer=measurer;
        }
        Measurement flush(WmPwiDocument.CharacterFormat format) throws IOException {
            long remaining = (long)available - width - pending;
            if (remaining < Integer.MIN_VALUE || remaining > Integer.MAX_VALUE)
                throw new IOException("PWI remaining width overflows");
            Measurement m = measure(measurer, buffer.toString(), format, (int)remaining);
            pending = add(pending, m.width);
            if (m.fittedCharacters != 0) metrics(m);
            buffer.setLength(0);
            return m;
        }
        void metrics(Measurement m) { ascent=Math.max(ascent,m.ascent); descent=Math.max(descent,m.descent); }
        Line finish() { return new Line(start, count == 0 && end != 0 ? 1 : count, width, ascent, descent); }
    }

    private static Measurement measure(Measurer measurer, String text, WmPwiDocument.CharacterFormat format,
                                       int available) throws IOException {
        Measurement m = measurer.measure(text, format, available);
        if (m == null || m.fittedCharacters < 0 || m.fittedCharacters > text.length()
                || (m.width < 0 && !(available < 0 && m.fittedCharacters == 0 && m.width == available))
                || m.ascent < 0 || m.ascent > 255 || m.descent < 0 || m.descent > 255)
            throw new IOException("Invalid PWI measurement result");
        return m;
    }
    private static int add(int a, int b) throws IOException {
        long value = (long)a + b;
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) throw new IOException("PWI layout arithmetic overflows");
        return (int)value;
    }
    private static boolean whitespace(WmPwiDocument.Token token) {
        if (token.nativeType == 1 || token.nativeType == 2) return token.character == ' ';
        if (token.nativeType == 6) return token.controlCode == 0 || token.controlCode == 3 || token.controlCode == 4;
        return token.nativeType != 3 && token.nativeType != 7;
    }
    private static boolean forbidden(WmPwiDocument.Token token, boolean opening) {
        if (token.nativeType != 1 && token.nativeType != 2) return false;
        String list = opening ? "([{<（［｛＜「｢〔《『【" : ",.?!)]}>、。，．､｡？！）］｝＞゜ﾟ゛ﾞ」｣〕》』】";
        return list.indexOf(token.character) >= 0;
    }
}
