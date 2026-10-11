package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.View;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Read-only binary PWI text adaptation. Fresh native wrapping/coordinate kernels
 * use Android font measurements; this is not GDI, ink, editor or writer fidelity.
 * The owner supplies DPI, scale, native break profile and explicit persisted-font
 * mapping. No device-density inference or silent document/font fallback occurs.
 * Saved line caches are never used to lay out this view. Plain streams, bullets,
 * highlight, unknown underline variants and extended character formats are rejected.
 */
public final class WmPwiView extends View implements WmControls.LanguageListener {
    public static final class Profile {
        public final int mode, dpiX, dpiY, documentScale;
        public final boolean extendedBreaks, gridTextAdjustment;
        public Profile(int mode, int dpiX, int dpiY, int documentScale,
                       boolean extendedBreaks, boolean gridTextAdjustment) {
            if (dpiX <= 0 || dpiY <= 0 || documentScale <= 0
                    || (long)dpiX*documentScale > Integer.MAX_VALUE
                    || (long)dpiY*documentScale > Integer.MAX_VALUE
                    || WmPwiLayout.unitScale(0, dpiX, documentScale) <= 0
                    || WmPwiLayout.unitScale(0, dpiY, documentScale) <= 0)
                throw new IllegalArgumentException("Positive DPI/scale and positive native unit0 coefficients required");
            this.mode=mode; this.dpiX=dpiX; this.dpiY=dpiY; this.documentScale=documentScale;
            this.extendedBreaks=extendedBreaks; this.gridTextAdjustment=gridTextAdjustment;
        }
    }
    public interface FontResolver {
        /** Resolve the persisted face and complete weight/italic style; null is not a fallback. */
        Typeface resolve(WmPwiDocument.Font font, WmPwiDocument.CharacterFormat format) throws IOException;
    }
    private final WmPwiDocument document;
    private final Profile profile;
    private final FontResolver resolver;
    private final ArrayList<ParagraphData> paragraphs=new ArrayList<ParagraphData>();
    private final HashMap<String,Style> styles=new HashMap<String,Style>();
    private final Rect clip=new Rect();
    private ArrayList<DrawLine> drawing=new ArrayList<DrawLine>();
    private int layoutWidth=-1, contentHeight;
    private String layoutError;
    private String language="ko";
    private boolean measuring;

    public WmPwiView(Context context, WmPwiDocument document, Profile profile,
                     FontResolver resolver) throws IOException {
        super(context);
        if (document==null || profile==null || resolver==null)
            throw new IllegalArgumentException("Document, explicit profile and font resolver required");
        this.document=document; this.profile=profile; this.resolver=resolver;
        if (!document.isBinary()) throw new IOException("Plain PWI streams are not supported by the styled reader");
        for (WmPwiDocument.Paragraph paragraph:document.getParagraphs()) {
            if (paragraph.hasBullet()) throw new IOException("Native PWI bullet drawing unsupported");
            ParagraphData data=new ParagraphData(paragraph);
            WmPwiDocument.CharacterFormat format=WmPwiDocument.CharacterFormat.initial();
            Style current=style(format); boolean boundary=false;
            for (WmPwiDocument.Token token:paragraph.getTokens()) {
                if (token.nativeType==5) {
                    format=format.apply(token); current=style(format); boundary=true;
                } else {
                    if (token.nativeType!=1 && token.nativeType!=2 && token.nativeType!=4 && token.nativeType!=6)
                        throw new IOException("PWI object/attachment drawing unsupported: "+token.nativeType);
                    data.units.add(new Unit(token,current,boundary)); boundary=false;
                }
            }
            paragraphs.add(data);
        }
        setBackgroundColor(Color.WHITE);
        setContentDescription(document.getPlainText());
        setFocusable(false);
    }
    public WmPwiDocument getDocument() { return document; }
    public Profile getProfile() { return profile; }
    public int getLineCount() { return drawing.size(); }
    public int getContentHeight() { return contentHeight; }
    /** Null on success; failed automatic reflow draws no flattened substitute. */
    public String getLayoutError() { return layoutError; }
    /** UI-thread contentDescription update only: catalog error caption or original
     * plain document text. Does not translate text/font mappings or reflow. */
    @Override public void onWmLanguageChanged(String value) {
        language=WmStrings.language(getContext(),value);
        updateDescription();
    }
    private void updateDescription() {
        setContentDescription(layoutError==null?document.getPlainText():String.format(
                WmStrings.locale(language),WmStrings.text(getContext(),language,
                        "PWI 레이아웃 오류: %s","pwi.layout_error"),layoutError));
    }

    /** Rebuilds cached measurement/draw runs; drawing itself performs no text layout. */
    public void reflow(int width) throws IOException {
        if (width<0) throw new IllegalArgumentException("Nonnegative view width required");
        try {
            int previousHeight=contentHeight;
            ArrayList<DrawLine> fresh=new ArrayList<DrawLine>();
            int nativeWidth=toNative(width);
            if (nativeWidth<0) throw new IOException("PWI frame conversion overflow");
            for (ParagraphData data:paragraphs) {
                List<WmPwiTextLayout.Line> lines=WmPwiTextLayout.layout(data.paragraph,profile.mode,
                        0,nativeWidth,profile.extendedBreaks,this::measure);
                for (int index=0;index<lines.size();index++) {
                    WmPwiTextLayout.Line measured=lines.get(index);
                    int start=WmPwiLayout.lineStart(data.paragraph,profile.mode,index,0,nativeWidth,measured.width);
                    DrawLine line=new DrawLine();
                    line.advance=(profile.mode==0 || profile.mode==1)
                            ? Math.max(72,toNative(measured.ascent+measured.descent)):72;
                    prepareRuns(data,measured,start,line);
                    fresh.add(line);
                }
            }
            int[] advances=new int[fresh.size()];
            long extent=0;
            for (int i=0;i<advances.length;i++) {
                advances[i]=fresh.get(i).advance; extent+=advances[i];
                if (extent>Integer.MAX_VALUE) throw new IOException("PWI document height overflows");
            }
            int[] bottoms=WmPwiLayout.positionLineBottoms(0,0,advances);
            int height=toPixelsY((int)extent);
            if (height<0) throw new IOException("PWI pixel height overflows");
            for (int i=0;i<fresh.size();i++) {
                DrawLine line=fresh.get(i); int bottom=toPixelsY(-bottoms[i]);
                line.top=bottom; line.bottom=bottom;
                for (DrawRun run:line.runs) {
                    run.baseline=bottom-run.style.ascent-run.style.descent
                            +(profile.mode==0 && profile.gridTextAdjustment?1:0)-run.style.fontAscent;
                    line.top=Math.min(line.top,run.baseline+run.style.fontTop);
                    line.bottom=Math.max(line.bottom,run.baseline+run.style.fontBottom);
                }
                height=Math.max(height,(int)Math.ceil(line.bottom));
            }
            drawing=fresh; contentHeight=height; layoutWidth=width; layoutError=null;
            setContentDescription(document.getPlainText());
            if (!measuring && previousHeight!=contentHeight) requestLayout();
            invalidate();
        } catch (IOException error) {
            boolean heightChanged=contentHeight!=0;
            drawing=new ArrayList<DrawLine>(); contentHeight=0; layoutWidth=width;
            layoutError=error.getMessage()==null?error.getClass().getSimpleName():error.getMessage();
            updateDescription();
            if (!measuring && heightChanged) requestLayout();
            invalidate();
            throw error;
        }
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec) {
        int width=MeasureSpec.getMode(widthSpec)==MeasureSpec.UNSPECIFIED
                ? getSuggestedMinimumWidth():MeasureSpec.getSize(widthSpec);
        if (layoutWidth!=width) {
            measuring=true;
            try { reflow(width); } catch (IOException rejected) { /* Public error and empty drawing retain failure. */ }
            finally { measuring=false; }
        }
        setMeasuredDimension(resolveSize(width,widthSpec),resolveSize(contentHeight,heightSpec));
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!canvas.getClipBounds(clip)) return;
        int save=canvas.save(); canvas.clipRect(0,0,getWidth(),getHeight());
        for (DrawLine line:drawing) {
            if (line.bottom<clip.top || line.top>clip.bottom) continue;
            for (DrawRun run:line.runs) run.prepared.draw(canvas,run.x,run.baseline);
        }
        canvas.restoreToCount(save);
    }
    private Style style(WmPwiDocument.CharacterFormat format) throws IOException {
        if (format.isHighlighted() || format.getNativeOperand(15)!=0 || format.getNativeOperand(16)!=0)
            throw new IOException("PWI highlight/underline variant drawing unsupported");
        StringBuilder key=new StringBuilder();
        for (int kind=7;kind<=16;kind++) key.append(format.getNativeOperand(kind)).append('/');
        Style saved=styles.get(key.toString()); if (saved!=null) return saved;
        int color=format.getColorIndex();
        if (color<0 || color>=32) throw new IOException("PWI native palette index unsupported: "+color);
        WmPwiDocument.Font font=document.resolveFont(format.getFontId());
        Typeface face=resolver.resolve(font,format);
        if (face==null) throw new IOException("Font resolver returned null for PWI font "+font.id);
        int points=document.resolveFontSizePoints(format.getFontSizePoints());
        long base=((long)profile.dpiY*points+36)/72;
        if (base>Long.MAX_VALUE/profile.documentScale) throw new IOException("PWI font height overflows");
        long scaled=base*profile.documentScale/(profile.extendedBreaks?260:240);
        if (scaled>Integer.MAX_VALUE) throw new IOException("PWI font height overflows");
        int height=(int)scaled;
        if (profile.mode!=0 && profile.mode!=1) {
            int cap=convert(profile.documentScale,4,0,profile.dpiX)
                    -(profile.gridTextAdjustment?1:0)-1;
            if (cap<0) throw new IOException("PWI grid font cap is negative");
            height=Math.min(height,cap);
        }
        if (height<=0) throw new IOException("PWI zero-height native font substitution unsupported");
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(face); paint.setTextSize(height); paint.setColor(WmPwiDocument.getNativeDefaultColorArgb(color));
        paint.setTextAlign(Paint.Align.LEFT); paint.setFakeBoldText(false);
        // Android decoration is an explicit adaptation; resolver owns weight and italic face selection.
        paint.setUnderlineText(format.isUnderlined()); paint.setStrikeThruText(format.isStruckOut());
        Paint.FontMetrics fm=paint.getFontMetrics();
        int leading=Math.max(0,(int)Math.ceil(fm.leading));
        int a=((int)Math.ceil(-fm.ascent)+leading-leading/2)&255;
        int b=((int)Math.ceil(fm.descent)+leading/2)&255;
        Style result=new Style(paint,a,b,fm);
        styles.put(key.toString(),result); return result;
    }
    private WmPwiTextLayout.Measurement measure(String text,WmPwiDocument.CharacterFormat format,int available) throws IOException {
        Style style=style(format);
        if (available<0) return new WmPwiTextLayout.Measurement(0,available,0,0);
        int fitted=text.length();
        if (available!=Integer.MAX_VALUE) {
            int pixels=toPixelsX(available);
            if (pixels<0) throw new IOException("PWI fit width conversion overflow");
            fitted=0;
            // UTF16 prefix fit deliberately follows the recovered native callback contract.
            while (fitted<text.length() && WmFonts.measure(style.paint,text.substring(0,fitted+1))<=pixels) fitted++;
        }
        float width=WmFonts.measure(style.paint,text.substring(0,fitted));
        if (Float.isNaN(width) || Float.isInfinite(width) || width>Integer.MAX_VALUE)
            throw new IOException("PWI measured width unsupported");
        int pixels=(int)Math.ceil(width);
        int measured=available!=Integer.MAX_VALUE && pixels>=toPixelsX(available)
                ? available:toNative(pixels);
        if (measured<0) throw new IOException("PWI measured width conversion overflow");
        return new WmPwiTextLayout.Measurement(fitted,measured,style.ascent,style.descent);
    }
    private void prepareRuns(ParagraphData data,WmPwiTextLayout.Line line,int start,DrawLine output) throws IOException {
        int x=start,origin=start; Style current=null; StringBuilder text=new StringBuilder(128);
        int end=line.logicalStart+line.logicalCount;
        for (int i=line.logicalStart;i<end;i++) {
            Unit unit=data.units.get(i); int type=unit.token.nativeType;
            if (text.length()!=0 && (type!=1 && type!=2 || unit.style!=current || unit.boundary || text.length()==128)) {
                x=flush(text,current,x,output); text.setLength(0);
            }
            if (type==1 || type==2) { current=unit.style; text.append(unit.token.character); }
            else if (type==4) {
                byte[] bytes=unit.token.getEncodedBytes(); x=checkedAdd(x,(bytes[1]&255)|((bytes[2]&255)<<8));
            } else if (type==6 && unit.token.controlCode!=0)
                x=checkedAdd(x,WmPwiLayout.controlAdvance(unit.token.controlCode,x,origin,0));
        }
        if (text.length()!=0) flush(text,current,x,output);
    }
    private int flush(StringBuilder text,Style style,int x,DrawLine output) throws IOException {
        WmFonts.PreparedLine prepared=WmFonts.prepare(style.paint,text.toString());
        if (Float.isNaN(prepared.width) || Float.isInfinite(prepared.width) || prepared.width>Integer.MAX_VALUE)
            throw new IOException("PWI prepared width unsupported");
        output.runs.add(new DrawRun(prepared,style,toPixelsX(x)));
        return checkedAdd(x,toNative((int)Math.ceil(prepared.width)));
    }
    private int toNative(int pixels) throws IOException { return convert(pixels,0,5,profile.dpiX); }
    private int toPixelsX(int nativeUnits) throws IOException { return convert(nativeUnits,5,0,profile.dpiX); }
    private int toPixelsY(int nativeUnits) throws IOException { return convert(nativeUnits,5,0,profile.dpiY); }
    private int convert(int value,int source,int target,int dpi) throws IOException {
        int numerator=WmPwiLayout.unitScale(target,dpi,profile.documentScale);
        int denominator=WmPwiLayout.unitScale(source,dpi,profile.documentScale);
        long doubled=2L*value*numerator;
        long rounded=doubled+(doubled>0?denominator:doubled<0?-denominator:0);
        if(doubled<Integer.MIN_VALUE||doubled>Integer.MAX_VALUE||rounded<Integer.MIN_VALUE||rounded>Integer.MAX_VALUE)
            throw new IOException("PWI screen coordinate conversion overflows");
        return WmPwiLayout.convertUnits(value,source,target,dpi,profile.documentScale);
    }
    private static int checkedAdd(int a,int b) throws IOException {
        long value=(long)a+b;
        if (value<Integer.MIN_VALUE || value>Integer.MAX_VALUE) throw new IOException("PWI run coordinate overflows");
        return (int)value;
    }
    private static final class ParagraphData {
        final WmPwiDocument.Paragraph paragraph; final ArrayList<Unit> units=new ArrayList<Unit>();
        ParagraphData(WmPwiDocument.Paragraph p) { paragraph=p; }
    }
    private static final class Unit {
        final WmPwiDocument.Token token; final Style style; final boolean boundary;
        Unit(WmPwiDocument.Token token,Style style,boolean boundary) { this.token=token;this.style=style;this.boundary=boundary; }
    }
    private static final class Style {
        final Paint paint; final int ascent,descent;
        final float fontAscent,fontTop,fontBottom;
        Style(Paint paint,int a,int b,Paint.FontMetrics fm) {
            this.paint=paint;ascent=a;descent=b;
            fontAscent=fm.ascent;fontTop=fm.top;fontBottom=fm.bottom;
        }
    }
    private static final class DrawLine {
        final ArrayList<DrawRun> runs=new ArrayList<DrawRun>(); int advance; float top,bottom;
    }
    private static final class DrawRun {
        final WmFonts.PreparedLine prepared; final Style style; final float x; float baseline;
        DrawRun(WmFonts.PreparedLine prepared,Style style,float x) { this.prepared=prepared;this.style=style;this.x=x; }
    }
}
