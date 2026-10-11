package me.ancal.wmdroid.sdk;

/** v1 logical VGA units. Scale the entire surface once; never apply Android sp. */
public final class WmMetrics {
    private WmMetrics(){}
    // GWES border/edge formulas at logical192 DPI; see native-system-metrics-evidence.md.
    public static final int WIDTH=480, BAR=52, FONT=24, LINE=30, GAP=2, INSET=8, BORDER=2, EDGE=4;
    public static final int INDICATOR=30; // GWES ADDA8:15px at96DPI, independent of Button font.
    public static final int TEXT_LEFT=56, ICON=32, ICON_INSET=4, TODAY_MIN=40, TAB=40;
    // Today style47: positive Tahoma cell28 links to Gulim cell23 at192DPI.
    public static final int TODAY_FONT=23, TODAY_LINE=28;
    /** Plain combo at logical192 DPI: A1DE8 font/min32 + inset4 + border4. */
    public static int comboHeight(int textHeight){return (int)Math.min(Integer.MAX_VALUE,(long)Math.max(32,textHeight)+EDGE+2*BORDER);}
    public static int todayHeight(int lines){return lines<=1?TODAY_MIN:lines*TODAY_LINE+(lines-1)*GAP+2*INSET;}
}
