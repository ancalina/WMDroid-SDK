package me.ancal.wmdroid.sdk;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Immutable original PWI input, with text, scalar format and saved layout access for the recovered
 * text-only record family. Unsupported objects fail explicitly. This is not an
 * editor or a general PWI writer: getOriginalBytes() returns the unchanged input.
 * Native evidence: design/notes-pwi-stream-evidence.json and the text/edit/format native evidence files.
 */
public final class WmPwiDocument {
    // RichInk 034BEA14, runtime object 14. This palette is not a PWI record or a theme palette.
    private static final int[] NATIVE_DEFAULT_COLORREF = {
        0x000000, 0x808080, 0xc0c0c0, 0xffffff, 0x0000ff, 0x00ff00, 0xff0000, 0xffff00,
        0xff00ff, 0x00ffff, 0x000080, 0x008000, 0x800000, 0x808000, 0x800080, 0x008080,
        0x00ffff, 0xffffff, 0x808080, 0x000080, 0x808080, 0xc0c0c0, 0xe0e0e0, 0x80ffff,
        0xff0000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000
    };
    private final byte[] original;
    private final boolean binary;
    private final String text;
    private final List<Paragraph> paragraphs;
    private final List<Font> fonts;
    private final int defaultFontId, defaultFontSizePoints;
    private final SavedView savedView;

    private WmPwiDocument(byte[] data, boolean binary, String text, List<Paragraph> paragraphs,
                          List<Font> fonts, int defaultFontId, int defaultFontSizePoints, SavedView savedView) {
        original = data;
        this.binary = binary;
        this.text = text;
        this.paragraphs = Collections.unmodifiableList(paragraphs);
        this.fonts = Collections.unmodifiableList(fonts);
        this.defaultFontId = defaultFontId;
        this.defaultFontSizePoints = defaultFontSizePoints;
        this.savedView = savedView;
    }

    /**
     * The caller supplies the originating device's code page for plain PWI.
     * Binary PWI contains its own UTF-16 character tokens. RTF/PWD input and
     * unknown binary structures are rejected, never silently flattened.
     */
    public static WmPwiDocument read(byte[] input, Charset plainTextCharset) throws IOException {
        if (input == null || plainTextCharset == null)
            throw new IllegalArgumentException("Input and original-device charset required");
        byte[] data = input.clone();
        if (prefix(data, "{\\rtf") || prefix(data, "{\\pwd"))
            throw new IOException("RTF/PWD conversion is not implemented");
        if (!prefix(data, "{\\pwi")) {
            String text = plainTextCharset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data)).toString();
            return new WmPwiDocument(data, false, text, new ArrayList<Paragraph>(),
                    new ArrayList<Font>(), 0, 0, null);
        }
        return new Reader(data).read();
    }

    public boolean isBinary() { return binary; }
    /** RichInk SF_UTEXT projection for the supported binary family, without its terminal NUL. */
    public String getPlainText() { return text; }
    public byte[] getOriginalBytes() { return original.clone(); }
    /** Native slot order, not record-ID order. Empty for the plain-text storage variant. */
    public List<Paragraph> getParagraphs() { return paragraphs; }
    /** Persisted font definitions in file order, before native device-specific font substitution. */
    public List<Font> getFonts() { return fonts; }
    public int getDefaultFontId() { return defaultFontId; }
    public int getDefaultFontSizePoints() { return defaultFontSizePoints; }
    /**
     * Persisted view defaults, which native runtime settings can override. Null
     * for plain input; no device DPI or global runtime flags are implied.
     */
    public SavedView getSavedView() { return savedView; }
    public static final class SavedView {
        public final int documentScale, mode;
        private SavedView(int documentScale, int mode) {
            this.documentScale = documentScale;
            this.mode = mode;
        }
    }

    /**
     * Original RichInk factory palette, converted from COLORREF to opaque ARGB.
     * Explicitly selects that fixed runtime profile; it does not infer custom
     * runtime palettes, document RGB values, highlight choice or launcher theme colors.
     */
    public static int getNativeDefaultColorArgb(int index) {
        if (index < 0 || index >= NATIVE_DEFAULT_COLORREF.length)
            throw new IllegalArgumentException("Native default color index must be 0..31");
        int color = NATIVE_DEFAULT_COLORREF[index];
        return 0xff000000 | ((color & 255) << 16) | (color & 0xff00) | ((color >>> 16) & 255);
    }

    /** Resolves an ID, not a list index; zero selects the persisted document default. */
    public Font resolveFont(int id) throws IOException {
        unsigned16(id);
        if (id == 0) id = defaultFontId;
        if (id == 0) throw new IOException("PWI font requires an original-device runtime default");
        for (Font font : fonts) if (font.id == id) return font; // Native first matching ID wins.
        throw new IOException("PWI font ID not found: " + id);
    }

    /** Zero selects the stored default. No guessed size replaces an unresolved runtime default. */
    public int resolveFontSizePoints(int points) throws IOException {
        unsigned16(points);
        if (points == 0) points = defaultFontSizePoints;
        if (points == 0) throw new IOException("PWI size requires an original-device runtime default");
        return points;
    }

    private static void unsigned16(int value) {
        if (value < 0 || value > 65535) throw new IllegalArgumentException("Unsigned 16-bit value required");
    }

    /** Serialized metadata only; no font file lookup, substitution or Android Typeface allocation. */
    public static final class Font {
        public final int id, family, charset, pitch;
        /** Native bounded face-name view: at most 31 UTF-16 code units, stopping at NUL. */
        public final String faceName;
        private Font(int id, int family, int charset, int pitch, String faceName) {
            this.id = id;
            this.family = family;
            this.charset = charset;
            this.pitch = pitch;
            this.faceName = faceName;
        }
    }

    public static final class Paragraph {
        public static final int ALIGN_LEFT = 0, ALIGN_RIGHT = 1, ALIGN_CENTER = 2;
        public final int id;
        public final int logicalCharacterCount;
        /** Saved layout, not a guarantee that these lines match the current font or width. */
        public final int cachedLineCount;
        /** Native first line needing reflow; 0xffff means the saved cache is marked clean. */
        public final int layoutDirtyFromLine;
        private final String text;
        private final List<Token> tokens;
        private final List<CachedLine> cachedLines;
        private final int firstLineIndent, leftIndent, rightIndent, alignment;
        private final int indentLevel, indentFlags;
        private final boolean bullet;

        private Paragraph(int id, int count, String text, List<Token> tokens,
                          int dirtyFromLine, List<CachedLine> lines, int firstLineIndent,
                          int leftIndent, int rightIndent, int alignment, boolean bullet,
                          int indentLevel, int indentFlags) {
            this.id = id;
            logicalCharacterCount = count;
            cachedLineCount = lines.size();
            layoutDirtyFromLine = dirtyFromLine;
            cachedLines = Collections.unmodifiableList(lines);
            this.firstLineIndent = firstLineIndent;
            this.leftIndent = leftIndent;
            this.rightIndent = rightIndent;
            this.alignment = alignment;
            this.bullet = bullet;
            this.indentLevel = indentLevel;
            this.indentFlags = indentFlags;
            this.text = text;
            this.tokens = Collections.unmodifiableList(tokens);
        }
        public String getPlainText() { return text; }
        public List<Token> getTokens() { return tokens; }
        public boolean isLayoutDirty() { return layoutDirtyFromLine != 0xffff; }
        /** Native 0=left, 1=right, 2=center; other stored values resolve to left. */
        public int getAlignment() { return alignment <= ALIGN_CENTER ? alignment : ALIGN_LEFT; }
        /** Signed first-line delta, in native logical units, not Android pixels/dp/points. */
        public int getFirstLineIndentNative() { return firstLineIndent; }
        /** Native getter clamps negative stored values; original bytes remain unchanged. */
        public int getLeftIndentNative() { return Math.max(0, leftIndent); }
        public int getRightIndentNative() { return Math.max(0, rightIndent); }
        /** Native bullet flag, distinct from literal '*' characters in stock todo.pwi. */
        public boolean hasBullet() { return bullet; }
        /** CBFC4 level-based indent used in native mode1; unsigned WORD result in logical units. */
        public int getLevelIndentNative() {
            int delta;
            if (indentLevel != 0) delta = indentLevel == 1 ? 0 : (short) (216 * (indentLevel - 1));
            else if ((indentFlags & 248) > 8) delta = (short) (216 * ((indentFlags >>> 3) - 1) + 108);
            else delta = 108;
            return (delta + 180) & 65535;
        }
        /** Saved device/font-dependent metrics; dirty lines can be stale. No reflow occurs. */
        public List<CachedLine> getCachedLines() { return cachedLines; }
        /** Counts can be stale when dirty; they are not text ranges for an editor. */
        public int[] getCachedLineCharacterCounts() {
            int[] counts = new int[cachedLines.size()];
            for (int i = 0; i < counts.length; i++) counts[i] = cachedLines.get(i).logicalCharacterCount;
            return counts;
        }

        /**
         * Native scalar character format immediately before the logical token at
         * offset (or after trailing formats at logicalCharacterCount). Format
         * tokens do not advance this offset; a CRLF control advances it once.
         * Not a getPlainText String index: A,control0,B counts3 but projects to
         * A\r\nB(length4); offset2 is before B, not the LF character.
         * Extended formats fail explicitly rather than returning an incomplete style.
         * This lookup scans the paragraph; a renderer should walk tokens once per layout.
         */
        public CharacterFormat getCharacterFormat(int logicalOffset) throws IOException {
            if (logicalOffset < 0 || logicalOffset > logicalCharacterCount)
                throw new IllegalArgumentException("Logical character offset outside paragraph");
            CharacterFormat format = CharacterFormat.initial();
            int logical = 0;
            for (Token token : tokens) {
                if (token.nativeType == 5) {
                    format = format.apply(token);
                } else if (logical++ == logicalOffset) {
                    break;
                }
            }
            return format;
        }
    }

    /** Saved kind42 entry. Values describe the original cache, not measured Android text. */
    public static final class CachedLine {
        public final int logicalCharacterCount;
        /** Native logical units; a zero horizontal offset triggers native alignment calculation. */
        public final int horizontalOffsetNative, widthNative;
        /** Raw signed descriptor Y field; its exact vertical meaning is not proven. Not a baseline. */
        public final int verticalOffsetNative;
        /** Saved byte metrics: ascent+ceil(externalLeading/2), descent+floor(externalLeading/2).
         * Each is the independent maximum across runs, subject to native byte truncation. */
        public final int ascentWithLeadingNative, descentWithLeadingNative;

        private CachedLine(int count, int x, int y, int width, int ascent, int descent) {
            logicalCharacterCount = count;
            horizontalOffsetNative = x;
            verticalOffsetNative = y;
            widthNative = width;
            ascentWithLeadingNative = ascent;
            descentWithLeadingNative = descent;
        }
    }

    /** Scalar native format state; font IDs and palette indices are not Android resources. */
    public static final class CharacterFormat {
        private final int[] values;
        private CharacterFormat(int[] values) { this.values = values; }
        static CharacterFormat initial() {
            int[] values = new int[10];
            values[3] = 4; // RichInk 034C4AEC: normal weight; other operands start at zero.
            return new CharacterFormat(values);
        }
        CharacterFormat apply(Token token) throws IOException {
            if (token.nativeType != 5) throw new IllegalArgumentException("Format token required");
            if (token.formatKind > 16)
                throw new IOException("Extended PWI character format unsupported: " + token.formatKind);
            int value = token.data[token.start + 1] & 255;
            if (token.formatKind <= 10) value |= (token.data[token.start + 2] & 255) << 8;
            int[] updated = values.clone();
            updated[token.formatKind - 7] = value;
            return new CharacterFormat(updated);
        }
        /** Kinds 7..16 only. Unknown underline variants 15/16 retain their raw operands. */
        public int getNativeOperand(int kind) {
            if (kind < 7 || kind > 16) throw new IllegalArgumentException("Scalar format kind must be 7..16");
            return values[kind - 7];
        }
        /** Zero refers to the document's default font; it is not a face-name index. */
        public int getFontId() { return values[0]; }
        /** Zero refers to the document default, which may itself require a native runtime default. */
        public int getFontSizePoints() { return values[1]; }
        public int getColorIndex() { return values[2]; }
        /** Matches native LOGFONT conversion; operands outside 1..9 yield weight zero. */
        public int getFontWeight() { return values[3] >= 1 && values[3] <= 9 ? values[3] * 100 : 0; }
        public boolean isItalic() { return values[4] != 0; }
        public boolean isUnderlined() { return values[5] != 0; }
        public boolean isStruckOut() { return values[6] != 0; }
        public boolean isHighlighted() { return values[7] != 0; }
    }

    /** Native numeric types/format kinds; unproved style meanings are not invented. */
    public static final class Token {
        public final int nativeType;
        public final int formatKind;
        public final int controlCode;
        public final char character;
        private final byte[] data;
        private final int start, length;

        private Token(byte[] data, int start, int length, int type, int format, int control, int character) {
            this.data = data;
            this.start = start;
            this.length = length;
            nativeType = type;
            formatKind = format;
            controlCode = control;
            this.character = (char) character;
        }
        /** Exact token bytes, including fields that are not interpreted by this reader. */
        public byte[] getEncodedBytes() { return Arrays.copyOfRange(data, start, start + length); }
    }

    private static boolean prefix(byte[] data, String value) {
        if (data.length < value.length()) return false;
        for (int i = 0; i < value.length(); i++) if ((data[i] & 255) != value.charAt(i)) return false;
        return true;
    }

    private static final class Record {
        final int id, payload, size;
        Record(int id, int payload, int size) { this.id = id; this.payload = payload; this.size = size; }
    }

    private static final class Reader {
        final byte[] data;
        int position = 14;
        Reader(byte[] data) { this.data = data; }

        WmPwiDocument read() throws IOException {
            need(0, 14, data.length);
            require((u8(5) == 20 || u8(5) == 21) && u8(6) == 0 && u8(7) == 0
                    && u8(8) == 1 && u8(9) == 1 && (u16(10) == 6 || u16(10) == 7)
                    && u16(12) == 0, "Unsupported PWI header profile");
            // Deliberately narrower than RichInk: each accepted section's framing is proven.
            Record fontTable = record(0, 2);
            List<Font> fonts = fonts(fontTable);
            record(0x14, 4);
            record(0x15, 5);
            Record settings = record(7, 6);
            require(settings.size >= 44, "Short settings reference table");
            for (int i = 0; i < 6; i++)
                require(u16(settings.payload + 32 + 2 * i) == 0, "Optional PWI settings objects unsupported");
            Record view = record(8, 7);
            require(view.size >= 8, "Short saved view record");
            // Loader values: signed DWORD scale and unsigned WORD mode, without guessed clamping.
            SavedView savedView = new SavedView((int) u32(settings.payload + 12), u16(view.payload + 4));
            Record table = record(0x40, 8);
            require(table.size >= 24, "Short paragraph table");
            long count = u32(table.payload);
            require(count <= (table.size - 24) / 8 && table.size == 24L + 8L * count,
                    "Invalid paragraph table extent");
            require(u32(table.payload + 4) == count, "Unsupported represented paragraph count");
            List<Paragraph> paragraphs = new ArrayList<Paragraph>();
            StringBuilder text = new StringBuilder();
            long logicalTotal = 0, lineTotal = 0;
            for (int i = 0; i < (int) count; i++) {
                int id = u16(table.payload + 24 + 8 * i + 4);
                require(id != 0, "Zero-reference paragraph slots not yet supported");
                Record body = record(0x41, id);
                require(body.size >= 24, "Short paragraph header");
                int companion = u16(body.payload + 8);
                require(companion != 0, "Missing paragraph companion reference");
                Record layout = record(0x42, companion);
                Paragraph paragraph = paragraph(body, layout);
                require(u16(table.payload + 24 + 8 * i + 2) == paragraph.logicalCharacterCount,
                        "Paragraph table character count mismatch");
                require(u16(table.payload + 24 + 8 * i) == paragraph.cachedLineCount,
                        "Paragraph table cached line count mismatch");
                logicalTotal += paragraph.logicalCharacterCount;
                lineTotal += paragraph.cachedLineCount;
                paragraphs.add(paragraph);
                text.append(paragraph.getPlainText());
            }
            require(u32(table.payload + 8) == logicalTotal, "Document logical character count mismatch");
            require(u32(table.payload + 12) == lineTotal, "Document cached line count mismatch");
            Record tail = record(0x82, 9);
            require(tail.size >= 4 && u16(tail.payload + 2) == 0, "PWI trailing objects unsupported");
            require(position == data.length, "Unhandled trailing PWI records");
            return new WmPwiDocument(data, true, text.toString(), paragraphs, fonts,
                    u16(fontTable.payload), u16(fontTable.payload + 2), savedView);
        }

        List<Font> fonts(Record table) throws IOException {
            need(table.payload, 16, table.payload + table.size);
            int count = u16(table.payload + 4);
            require(table.size >= 96 && count <= (table.size - 16) / 80,
                    "Font table entries exceed record");
            List<Font> fonts = new ArrayList<Font>(count);
            for (int i = 0; i < count; i++) {
                int at = table.payload + 16 + 80 * i;
                StringBuilder name = new StringBuilder(31);
                for (int c = 0; c < 31; c++) {
                    int value = u16(at + 16 + 2 * c);
                    if (value == 0) break;
                    name.append((char) value);
                }
                fonts.add(new Font(u16(at), u16(at + 4), u8(at + 6), u8(at + 7), name.toString()));
            }
            return fonts;
        }

        Record record(int kind, int id) throws IOException {
            need(position, 6, data.length);
            require(u8(position) == kind && u8(position + 1) == 0 && u16(position + 2) == id,
                    "Unsupported PWI record/order/reference at " + position);
            int size = u16(position + 4) * 4;
            int payload = position + 6;
            need(payload, size, data.length);
            position = payload + size;
            return new Record(id, payload, size);
        }

        Paragraph paragraph(Record body, Record layout) throws IOException {
            int count = u16(body.payload), bytes = u16(body.payload + 2);
            require(bytes <= body.size - 24, "Paragraph token extent exceeds record");
            int lineCount = u16(body.payload + 4);
            require(lineCount <= layout.size / 10, "Paragraph line cache exceeds companion record");
            List<CachedLine> lines = new ArrayList<CachedLine>(lineCount);
            long cachedCharacters = 0;
            for (int i = 0; i < lineCount; i++) {
                int at = layout.payload + 10 * i;
                int characters = u16(at);
                lines.add(new CachedLine(characters, (short) u16(at + 2), (short) u16(at + 4),
                        (short) u16(at + 6), u8(at + 8), u8(at + 9)));
                cachedCharacters += characters;
            }
            int dirtyFromLine = u16(body.payload + 6);
            require(dirtyFromLine != 0xffff || cachedCharacters == count,
                    "Clean paragraph line cache character count mismatch");
            int start = body.payload + 24, end = start + bytes, logical = 0;
            List<Token> tokens = new ArrayList<Token>();
            StringBuilder text = new StringBuilder();
            while (start < end) {
                Token token = token(start, end);
                tokens.add(token);
                if (token.nativeType != 5) logical++;
                if (token.nativeType == 1 || token.nativeType == 2) {
                    if (token.character >= 32) text.append(token.character);
                } else if (token.nativeType == 4) {
                    text.append(' ');
                } else if (token.nativeType == 6) {
                    if (token.controlCode == 0) text.append("\r\n");
                    else if (token.controlCode == 4) text.append('\t');
                }
                start += token.length;
            }
            require(logical == count, "Paragraph logical character count mismatch");
            return new Paragraph(body.id, count, text.toString(), tokens,
                    dirtyFromLine, lines, (short) u16(body.payload + 12),
                    (short) u16(body.payload + 14), (short) u16(body.payload + 16),
                    u8(body.payload + 19), u8(body.payload + 18) != 0,
                    u8(body.payload + 22), u8(body.payload + 23));
        }

        Token token(int start, int end) throws IOException {
            int first = u8(start), size, type, format = -1, control = -1, character = 0;
            if (first < 0x80) {
                type = 1; size = 1; character = first;
            } else if (first < 0xc0) {
                type = 2; size = 2;
                need(start, size, end);
                character = (first & 63) | (u8(start + 1) << 6);
            } else if (first == 0xc0 || first == 0xc1 || first == 0xc3) {
                type = first == 0xc0 ? 2 : first == 0xc1 ? 1 : 4;
                size = first == 0xc1 ? 2 : 3;
                need(start, size, end);
                if (type == 1) character = u8(start + 1);
                else if (type == 2) character = u16(start + 1);
            } else if (first == 0xc4) {
                type = 6;
                need(start, 2, end);
                control = u8(start + 1);
                size = control == 25 ? 4 : control == 26 ? 7 : control == 27 ? 8 : 2;
            } else if (first >= 0xe5 && first <= 0xf1) {
                type = 5; format = first - 0xde;
                size = first <= 0xe8 ? 3 : first <= 0xee ? 2 : first == 0xef ? 4 : first == 0xf0 ? 7 : 8;
            } else {
                throw new IOException("Unsupported PWI token/object at " + start + ": " + first);
            }
            need(start, size, end);
            return new Token(data, start, size, type, format, control, character);
        }

        int u8(int at) { return data[at] & 255; }
        int u16(int at) { return u8(at) | (u8(at + 1) << 8); }
        long u32(int at) { return (long) u16(at) | ((long) u16(at + 2) << 16); }
        void need(int at, int size, int end) throws IOException {
            require(at >= 0 && size >= 0 && at <= end && size <= end - at, "Truncated PWI data at " + at);
        }
        void require(boolean condition, String message) throws IOException {
            if (!condition) throw new IOException(message);
        }
    }
}
