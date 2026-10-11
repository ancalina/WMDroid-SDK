package me.ancal.wmdroid.sdk;

/**
 * Recovered RichInk coordinate and line-placement arithmetic. Inputs are native
 * logical coordinates and explicit runtime metrics, never Android dp/sp. This
 * kernel does not measure fonts, wrap text, reflow a dirty cache or draw a view.
 * See design/notes-pwi-{paragraph,coordinate,line-position}-native-evidence.json.
 */
public final class WmPwiLayout {
    private WmPwiLayout() {}

    /** B35FC percent-to-scale helper, including signed32 arithmetic; no range clamp.
     * This is not the window-message dispatcher, which also accepts legacy indices0..5.
     * The six standard percentages are75,100,125,150,200,300.
     */
    public static int zoomScale(int percent, boolean extendedBreaks) {
        switch(percent) {
            case 75: return 198;
            case 100: return extendedBreaks ? 260 : 240;
            case 125: return extendedBreaks ? 325 : 300;
            case 150: return 396;
            case 200: return 550;
            case 300: return 793;
            default: return (2380*percent+500)/1000;
        }
    }

    /** B3558 scale-to-percent helper. Saved scale550 means200%, not normal100%. */
    public static int zoomPercent(int scale, boolean extendedBreaks) {
        if(scale==198)return 75;
        if(scale==(extendedBreaks?260:240))return 100;
        if(scale==(extendedBreaks?325:300))return 125;
        if(scale==396)return 150;
        if(scale==550)return 200;
        if(scale==793)return 300;
        return (1000*scale+1190)/2380;
    }

    /**
     * EF24C, including ARM signed32 wrap. Nearest rounding requires a positive
     * denominator and no intermediate overflow; other inputs retain native arithmetic.
     * Zero denominator returns INT_MAX even when value is zero; no origin is added.
     * Owners must validate their drawing profile before using results as screen extents.
     */
    public static int scaleCoordinate(int value, int offset, int numerator, int denominator, int origin) {
        if (denominator == 0) return Integer.MAX_VALUE;
        int doubled = ((value + offset) * numerator) << 1;
        if (doubled == 0) return origin;
        // Doubled is even: with denominator -1 the dividend is odd, never INT_MIN.
        int quotient = (doubled + (doubled > 0 ? denominator : -denominator)) / denominator;
        return origin + quotient / 2;
    }

    /** BD9A0 per-axis coefficient. Supply that axis's original device DPI, not a guessed96. */
    public static int unitScale(int unit, int dpi, int documentScale) {
        switch (unit) {
            case 0: return dpi * documentScale / 1440;
            case 1: return 2540 * documentScale / 1440;
            case 2: return 1000 * documentScale / 1440;
            case 3: return 72 * documentScale / 1440;
            case 4: return documentScale;
            case 5: return 72;
            case 6: return (4 * dpi) * documentScale / 1440;
            default: throw new IllegalArgumentException("Native coordinate unit must be 0..6");
        }
    }

    /** Scalar BDC08 uses X-axis coefficients; BDD04 uses each axis separately. */
    public static int convertUnits(int value, int sourceUnit, int targetUnit, int dpi, int documentScale) {
        return scaleCoordinate(value, 0, unitScale(targetUnit, dpi, documentScale),
                unitScale(sourceUnit, dpi, documentScale), 0);
    }

    /** E283C measurement start. Narrow frames can produce a negative result after the upper cap. */
    public static int wrapStart(WmPwiDocument.Paragraph paragraph, int mode, int lineIndex,
                                int frameLeft, int frameWidth) {
        checkLine(paragraph, lineIndex);
        int x = indent(paragraph, mode, lineIndex);
        if (x < 0) x = 0;
        int cap = frameWidth + frameLeft - 72;
        if (x > cap && paragraph.getAlignment() != WmPwiDocument.Paragraph.ALIGN_RIGHT) x = cap;
        return x;
    }

    /**
     * E1B18 alignment from a supplied measured line width; never substitutes a dirty saved width.
     * Unlike wrapStart, its lower clamp comes last. Mode1 uses level indent instead of alignment.
     */
    public static int lineStart(WmPwiDocument.Paragraph paragraph, int mode, int lineIndex,
                                int frameLeft, int frameWidth, int measuredWidth) {
        checkLine(paragraph, lineIndex);
        int x = indent(paragraph, mode, lineIndex);
        int alignment = paragraph.getAlignment();
        if (mode != 1) {
            if (alignment == WmPwiDocument.Paragraph.ALIGN_RIGHT)
                x = frameWidth - measuredWidth - paragraph.getRightIndentNative();
            else if (alignment == WmPwiDocument.Paragraph.ALIGN_CENTER)
                x += (frameWidth - measuredWidth - paragraph.getRightIndentNative() - x) / 2;
        }
        int cap = frameWidth + frameLeft - 72;
        if (x > cap && alignment != WmPwiDocument.Paragraph.ALIGN_RIGHT) x = cap;
        return Math.max(0, x);
    }

    private static int indent(WmPwiDocument.Paragraph paragraph, int mode, int lineIndex) {
        if (mode == 1) return paragraph.getLevelIndentNative();
        return paragraph.getLeftIndentNative() + (paragraph.hasBullet() ? 108 : 0)
                + (lineIndex == 0 ? paragraph.getFirstLineIndentNative() : 0);
    }

    /** F3648: CR contributes36, TAB advances to a170-unit stop, other controls contribute zero. */
    public static int controlAdvance(int control, int currentX, int suppliedOrigin, int frameLeft) {
        if (control < 0 || control > 255) throw new IllegalArgumentException("Native control must be 0..255");
        if (control == 0) return 36;
        if (control != 4) return 0;
        int origin = suppliedOrigin == 0 ? frameLeft : suppliedOrigin;
        return 170 * ((currentX - origin) / 170 + 1) - currentX + origin;
    }

    /**
     * E1F04 saved-cache advance. Stock mode2 always uses72.
     * Modes0/1 use clean cached GDI metrics converted with the X coefficient.
     * Dirty or missing lines use72. This does not make the saved cache current.
     */
    public static int savedLineAdvance(WmPwiDocument.Paragraph paragraph, int lineIndex, int mode,
                                       int dpiX, int documentScale) {
        checkLine(paragraph, lineIndex);
        if ((mode != 0 && mode != 1) || paragraph.isLayoutDirty() || lineIndex >= paragraph.cachedLineCount)
            return 72;
        WmPwiDocument.CachedLine line = paragraph.getCachedLines().get(lineIndex);
        return Math.max(72, convertUnits(line.ascentWithLeadingNative + line.descentWithLeadingNative,
                0, 5, dpiX, documentScale));
    }

    /**
     * A4C74/A8850 world-Y anchors, in one linear pass. Above the reference, native
     * sums [line,reference-1); at/below it sums [reference,line+1) and subtracts.
     * These are not Android baselines. The caller supplies current line advances.
     */
    public static int[] positionLineBottoms(int referenceLine, int referenceBottom, int[] advances) {
        if (advances == null || referenceLine < 0 || referenceLine > advances.length)
            throw new IllegalArgumentException("Reference line outside supplied advances");
        for (int advance : advances)
            if (advance < 72) throw new IllegalArgumentException("Native line advance must be at least72");
        int[] bottoms = new int[advances.length];
        int y = referenceBottom;
        for (int i = referenceLine - 1; i >= 0; i--) {
            bottoms[i] = y;
            if (i != 0) y += advances[i - 1];
        }
        y = referenceBottom;
        for (int i = referenceLine; i < advances.length; i++) {
            y -= advances[i];
            bottoms[i] = y;
        }
        return bottoms;
    }

    private static void checkLine(WmPwiDocument.Paragraph paragraph, int lineIndex) {
        if (paragraph == null || lineIndex < 0)
            throw new IllegalArgumentException("Paragraph and nonnegative line index required");
    }
}
