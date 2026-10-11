package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.graphics.fonts.FontFamily;
import android.graphics.fonts.FontStyle;
import android.os.Build;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.Editable;
import android.text.style.MetricAffectingSpan;
import android.widget.TextView;
import java.text.BreakIterator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** WM font linking: original Tahoma first, selected local face for missing glyphs.
 * API 14–28 need spans because CustomFallbackBuilder arrived in API 29.
 */
public final class WmFonts {
    public static final String SYSTEM_SOURCE="system:default";
    public static final String DEFAULT_SOURCE="fonts/gulim/Gulim-Regular.ttf";
    private static final String LATIN="fonts/tahoma/regular.ttf", LATIN_BOLD="fonts/tahoma/bold.ttf";
    private static volatile boolean smoothing=true;
    /** Process-wide WM text policy; standalone applets follow the launcher state. */
    public static void setSmoothing(boolean enabled){smoothing=enabled;}
    public static boolean isSmoothing(){return smoothing;}
    private static final Map<android.view.View,Integer> SMOOTHING_LAYERS=new WeakHashMap<>();
    /** Before API18, Canvas text needs a software surface when smoothing is off. */
    public static void applySmoothingLayer(android.view.View view){
        if(Build.VERSION.SDK_INT>=18)return;
        boolean software=!smoothing;
        if(software)for(android.view.ViewParent parent=view.getParent();parent instanceof android.view.View;parent=parent.getParent())
            if(((android.view.View)parent).getLayerType()==android.view.View.LAYER_TYPE_SOFTWARE){software=false;break;}
        if(software&&!SMOOTHING_LAYERS.containsKey(view)){SMOOTHING_LAYERS.put(view,view.getLayerType());view.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE,null);}
        else if(!software){Integer original=SMOOTHING_LAYERS.remove(view);if(original!=null&&view.getLayerType()==android.view.View.LAYER_TYPE_SOFTWARE)view.setLayerType(original,null);}
    }
    public static void applySmoothing(TextView view){
        applySmoothingLayer(view);
        if(view.getPaint().isAntiAlias()!=smoothing){view.getPaint().setAntiAlias(smoothing);view.invalidate();}
    }
    private static final Map<String,Typeface> CACHE=new ConcurrentHashMap<>();
    private static final Map<Typeface,Pair> PAIRS=java.util.Collections.synchronizedMap(new WeakHashMap<Typeface,Pair>());
    private static final Map<TextView,Adapter> ADAPTERS=new WeakHashMap<>();
    // Exact original tahoma.ttf cmap, SHA f515a9bc…; not just ASCII.
    private static final int[] TAHOMA={0x20,0x7e,0xa0,0x17f,0x192,0x192,0x1fa,0x1ff,0x2c6,0x2c7,0x2c9,0x2c9,0x2d8,0x2dd,0x37e,0x37e,0x384,0x38a,0x38c,0x38c,0x38e,0x3a1,0x3a3,0x3ce,0x401,0x40c,0x40e,0x44f,0x451,0x45c,0x45e,0x45f,0x490,0x491,0x1e80,0x1e85,0x1ef2,0x1ef3,0x2013,0x2015,0x2017,0x201e,0x2020,0x2022,0x2026,0x2026,0x2030,0x2030,0x2032,0x2033,0x2039,0x203a,0x203c,0x203c,0x203e,0x203e,0x2044,0x2044,0x207f,0x207f,0x20a3,0x20a4,0x20a7,0x20a7,0x20ac,0x20ac,0x2105,0x2105,0x2113,0x2113,0x2116,0x2116,0x2122,0x2122,0x2126,0x2126,0x212e,0x212e,0x215b,0x215e,0x2202,0x2202,0x2206,0x2206,0x220f,0x220f,0x2211,0x2212,0x2215,0x2215,0x2219,0x221a,0x221e,0x221e,0x222b,0x222b,0x2248,0x2248,0x2260,0x2260,0x2264,0x2265,0x25a1,0x25a1,0x25aa,0x25ab,0x25ca,0x25ca,0x25cf,0x25cf,0x25e6,0x25e6,0xf001,0xf002,0xf004,0xf005,0xfb01,0xfb02};
    private static final class Pair {
        final Typeface fallback;final boolean unhinted;
        Pair(Typeface fallback,boolean unhinted){this.fallback=fallback;this.unhinted=unhinted;}
    }
    // These bundled Malgun hints drop Hangul strokes in old FreeType/Skia.
    // Keep original outlines, Latin hints, Samsung and imported faces unchanged.
    private static boolean unhinted(String source){return Build.VERSION.SDK_INT<=28&&(source.equals("fonts/malgun/regular.ttf")||source.equals("fonts/malgun/bold.ttf"));}
    private static void runFace(Paint target,Paint primary,Pair pair,boolean latin){
        // Old getHinting() collapses native Normal/Full to ON; setHinting(ON)
        // restores only Slight. Copy native state instead of round-tripping it.
        target.set(primary);target.setTextAlign(Paint.Align.LEFT);
        target.setTypeface(latin?primary.getTypeface():pair.fallback);
        target.setFakeBoldText(false);if(!latin&&pair.unhinted)target.setHinting(Paint.HINTING_OFF);
    }

    /** A context-free query cannot identify an app/pack; conservatively cold. */
    @Deprecated public static boolean isCached(String source,boolean hybrid,boolean heavy){return false;}
    /** Whether this app/pack role is cached; does not load a face or wait. */
    public static boolean isCached(Context context,String source,boolean hybrid,boolean heavy){return SYSTEM_SOURCE.equals(source)||CACHE.containsKey(key(context,source,hybrid,heavy));}
    private static String key(Context context,String source,boolean hybrid,boolean heavy){return WmResources.revision(context)+"|load:"+source+":"+heavy+":"+hybrid;}
    /** Load an asset or immutable imported face; hybrid preserves original Tahoma
     * glyphs and uses the selected face only for missing glyphs. Heavy selects
     * Tahoma Bold; imported outlines are never synthetically emboldened.
     */
    public static Typeface load(Context c,String source,boolean hybrid,boolean heavy){
        if(SYSTEM_SOURCE.equals(source))return heavy?Typeface.DEFAULT_BOLD:Typeface.DEFAULT;
        try{WmResources.prepare(c);}catch(java.io.IOException unavailable){return heavy?Typeface.DEFAULT_BOLD:Typeface.DEFAULT;}
        String key=key(c,source,hybrid,heavy);
        Typeface cached=CACHE.get(key);if(cached!=null)return cached;
        synchronized(WmFonts.class){
        cached=CACHE.get(key);if(cached!=null)return cached;
        try {
            File file=WmFontFile.isSource(source)?WmFontFile.resolve(c,source):assetFont(c,source);
            String resolvedSource=source;
            if(file==null&&!WmFontFile.isSource(source)&&!DEFAULT_SOURCE.equals(source)){resolvedSource=DEFAULT_SOURCE;file=assetFont(c,resolvedSource);}
            Typeface face=hybrid?family(c,key,resolvedSource,file,heavy)
                :file!=null?face(file):heavy?Typeface.DEFAULT_BOLD:Typeface.DEFAULT;
            if(face==null)throw new java.io.IOException("Cannot create selected font");
            if(file!=null&&!hybrid&&unhinted(resolvedSource))PAIRS.put(face,new Pair(face,true));
            CACHE.put(key,face);return face;
        } catch(java.io.IOException failure){if(WmFontFile.isSource(source))throw new IllegalArgumentException("Cannot load selected font",failure);Typeface fallback=heavy?Typeface.DEFAULT_BOLD:Typeface.DEFAULT;CACHE.put(key,fallback);return fallback;}
        }
    }
    private static File assetFont(Context c,String source){try{File file=WmResources.assetFile(c,source,WmFontFile.MAX_BYTES);WmFontFile.validate(file);return file;}catch(java.io.IOException absent){return null;}}
    private static Typeface face(File file)throws java.io.IOException {try{Typeface face=Typeface.createFromFile(file);if(face==null)throw new java.io.IOException("Cannot create font");return face;}catch(RuntimeException invalid){throw new java.io.IOException("Invalid font face",invalid);}}
    public static Typeface family(Context c,String id,String fallbackPath,boolean heavy){
        // Keep this historical entry point; all role caching now includes app/pack identity.
        return load(c,fallbackPath,true,heavy);
    }
    private static Typeface family(Context c,String id,String fallbackPath,File fallbackFile,boolean heavy){
        String key=id+":"+fallbackPath+":"+heavy;
        Typeface cached=CACHE.get(key);if(cached!=null)return cached;
        String latinPath=heavy?LATIN_BOLD:LATIN;
        Typeface fallback=heavy?Typeface.DEFAULT_BOLD:Typeface.DEFAULT,face;
        try {
            if(fallbackFile!=null)fallback=face(fallbackFile);
            File latin=assetFont(c,latinPath);if(latin==null){CACHE.put(key,fallback);return fallback;}
            if(Build.VERSION.SDK_INT>=29)face=NativeFallback.create(latin,fallbackFile,heavy);
            else {
                // Distinct files avoid the asset Typeface cache: each Korean/weight
                // pair must have its own identity, including Samsung Regular/Bold.
                // Imported sources use their immutable SHA; old asset identities stay unchanged.
                String safeId=identity(key+"|"+latin.getAbsolutePath());
                File file=new File(c.getCacheDir(),"wm-"+safeId+(heavy?"-tahomabd.ttf":"-tahoma.ttf"));
                if(!file.isFile()){
                    File pending=new File(c.getCacheDir(),file.getName()+".new");
                    try {
                        try(InputStream in=new java.io.FileInputStream(latin);FileOutputStream out=new FileOutputStream(pending)){byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);out.getFD().sync();}
                        if(!pending.renameTo(file))throw new java.io.IOException("Cannot cache original Tahoma face");
                    } finally {if(pending.exists())pending.delete();}
                }
                face=face(file);
                if(face==null)throw new java.io.IOException("Cannot create original Tahoma face");
                PAIRS.put(face,new Pair(fallback,fallbackFile!=null&&unhinted(fallbackPath)));
            }
        } catch(java.io.IOException failure){face=fallback;}
        CACHE.put(key,face);return face;
    }
    private static String identity(String value){try{byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.Charset.forName("UTF-8")));StringBuilder hex=new StringBuilder(64);for(byte b:bytes)hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return hex.toString();}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
    private static final class NativeFallback {
        static Typeface create(File latin,File fallbackFile,boolean heavy)throws java.io.IOException {
            int weight=heavy?700:400;
            Font first=new Font.Builder(latin).build();
            // Assign this fallback the requested role weight without altering its
            // outlines. A Samsung Regular-only family must not get synthetic bold.
            Typeface.CustomFallbackBuilder builder=new Typeface.CustomFallbackBuilder(new FontFamily.Builder(first).build());
            if(fallbackFile!=null)builder.addCustomFallback(new FontFamily.Builder(new Font.Builder(fallbackFile).setWeight(weight).build()).build());
            return builder.setStyle(new FontStyle(weight,FontStyle.FONT_SLANT_UPRIGHT)).build();
        }
    }

    /** Call after setTypeface; handles current and future TextView/EditText text. */
    public static void attach(TextView view,Typeface face){
        applySmoothing(view);view.getPaint().setFakeBoldText(false);
        Pair pair=PAIRS.get(face);
        Adapter old=ADAPTERS.get(view);if(old!=null&&old.pair==pair)return;
        ADAPTERS.remove(view);if(old!=null)view.removeTextChangedListener(old);
        CharSequence current=view.getText();
        if(current instanceof Spannable){
            // ICS setTypeface already invalidated this layout. Its old watcher
            // otherwise reflows the discarded layout for every removed font span.
            // DynamicLayout's next constructor removes these same old watchers.
            // ponytail: only the verified ICS watcher name; other implementations
            // keep the normal path until their lifecycle is checked.
            if(Build.VERSION.SDK_INT<=15&&view.getLayout()==null){
                Spannable text=(Spannable)current;
                for(android.text.SpanWatcher watcher:text.getSpans(0,text.length(),android.text.SpanWatcher.class))
                    if("android.text.DynamicLayout$ChangeWatcher".equals(watcher.getClass().getName()))text.removeSpan(watcher);
            }
            clear((Spannable)current);
        }
        if(pair==null)return;
        Adapter adapter=new Adapter(pair);ADAPTERS.put(view,adapter);view.addTextChangedListener(adapter);
        // A listener makes ordinary TextView text Editable. Preserve other spans,
        // selection and IME composition; later edits modify only our own spans.
        if(current instanceof Editable)adapter.afterTextChanged((Editable)current);
        else view.setText(current);
    }
    private static final class Adapter implements TextWatcher {
        final Pair pair;boolean busy;Adapter(Pair pair){this.pair=pair;}
        public void beforeTextChanged(CharSequence s,int start,int count,int after){}
        public void onTextChanged(CharSequence s,int start,int before,int count){}
        public void afterTextChanged(Editable text){if(busy)return;busy=true;try{apply(text,pair);}finally{busy=false;}}
    }
    private static final class FaceSpan extends MetricAffectingSpan {
        final Typeface face;final boolean unhinted;FaceSpan(Typeface face,boolean unhinted){this.face=face;this.unhinted=unhinted;}
        public void updateMeasureState(TextPaint paint){paint.setTypeface(face);paint.setFakeBoldText(false);if(unhinted)paint.setHinting(Paint.HINTING_OFF);}
        public void updateDrawState(TextPaint paint){updateMeasureState(paint);}
    }
    private static void clear(Spannable text){for(FaceSpan span:text.getSpans(0,text.length(),FaceSpan.class))text.removeSpan(span);}
    private interface Run {void accept(int start,int end,boolean latin);}
    private static boolean supported(int cp){for(int i=0;i<TAHOMA.length;i+=2){if(cp<TAHOMA[i])return false;if(cp<=TAHOMA[i+1])return true;}return false;}
    private static boolean extendsCluster(int cp){int type=Character.getType(cp);return type==Character.NON_SPACING_MARK||type==Character.COMBINING_SPACING_MARK||type==Character.ENCLOSING_MARK||cp==0x200d||(cp>=0xfe00&&cp<=0xfe0f)||(cp>=0xe0100&&cp<=0xe01ef)||(cp>=0x1f3fb&&cp<=0x1f3ff);}
    private static void runs(String text,Run output){
        if(text.isEmpty())return;
        // These scalars have no combining boundaries; use the same face runs
        // as the Canvas fast path without allocating a character iterator.
        if(simpleLine(text)){
            for(int start=0;start<text.length();){boolean latin=text.charAt(start)<128;int end=start+1;while(end<text.length()&&(text.charAt(end)<128)==latin)end++;output.accept(start,end,latin);start=end;}
            return;
        }
        BreakIterator it=BreakIterator.getCharacterInstance(java.util.Locale.ROOT);it.setText(text);
        int start=it.first(),runStart=0;boolean previous=true,have=false;
        for(int end=it.next();end!=BreakIterator.DONE;end=it.next()){
            // Older character iterators can split ZWJ/variation/mark sequences.
            while(end<text.length()&&(text.codePointBefore(end)==0x200d||extendsCluster(text.codePointAt(end)))){int next=it.next();if(next==BreakIterator.DONE){end=text.length();break;}end=next;}
            boolean latin=true;for(int i=start;i<end;){int cp=text.codePointAt(i);if(!supported(cp))latin=false;i+=Character.charCount(cp);}
            if(have&&latin!=previous){output.accept(runStart,start,previous);runStart=start;}
            previous=latin;have=true;start=end;
        }
        if(have)output.accept(runStart,text.length(),previous);
    }
    private static void apply(Spannable text,Pair pair){clear(text);String value=text.toString();runs(value,(start,end,latin)->{if(!latin)text.setSpan(new FaceSpan(pair.fallback,pair.unhinted),start,end,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);});}
    private static CharSequence styled(Paint paint,String text){Pair pair=PAIRS.get(paint.getTypeface());if(pair==null)return text;SpannableString result=new SpannableString(text);apply(result,pair);return result;}

    // ICS AndroidBidi can exhaust its pinned char[] table during repeated
    // StaticLayout measurements. Basic Latin and precomposed Hangul need no
    // bidi/shaping pass; keep the same linked faces and baseline directly.
    private static boolean simpleLine(String text){for(int i=0;i<text.length();i++){char c=text.charAt(i);if(!((c>=0x20&&c<=0x7e)||(c>=0xac00&&c<=0xd7a3)))return false;}return true;}
    private static float simpleText(Canvas canvas,String text,float x,float baseline,Paint paint,Pair pair){
        Paint copy=new Paint(paint);copy.setTextAlign(Paint.Align.LEFT);float width=0;
        for(int start=0;start<text.length();){boolean latin=text.charAt(start)<0x80;int end=start+1;while(end<text.length()&&(text.charAt(end)<0x80)==latin)end++;
            runFace(copy,paint,pair,latin);
            if(canvas!=null)canvas.drawText(text,start,end,x+width,baseline,copy);
            if(canvas==null||end<text.length())width+=copy.measureText(text,start,end);start=end;
        }return width;
    }
    public static float measure(Paint paint,String text){Pair pair=PAIRS.get(paint.getTypeface());return pair==null?paint.measureText(text):simpleLine(text)?simpleText(null,text,0,0,paint,pair):Layout.getDesiredWidth(styled(paint,text),new TextPaint(paint));}
    /** Snapshot a single line for repeated drawing without measuring or shaping again. */
    public static PreparedLine prepare(Paint paint,String text){
        if(paint==null||text==null)throw new IllegalArgumentException("paint and text required");
        return new PreparedLine(paint,text);
    }
    public static final class PreparedLine {
        public final float width;
        private final String text;
        private final Paint paint;
        private final Paint.Align align;
        private final boolean antialias;
        private final PreparedRun[] runs;
        private final StaticLayout layout;
        private PreparedLine(Paint source,String text){
            this.text=text;paint=new Paint(source);antialias=paint.isAntiAlias();align=paint.getTextAlign();paint.setTextAlign(Paint.Align.LEFT);
            Pair pair=PAIRS.get(paint.getTypeface());
            if(pair==null){width=paint.measureText(text);runs=null;layout=null;}
            else if(simpleLine(text)){
                java.util.ArrayList<PreparedRun> prepared=new java.util.ArrayList<>();float advance=0;
                for(int start=0;start<text.length();){boolean latin=text.charAt(start)<0x80;int end=start+1;while(end<text.length()&&(text.charAt(end)<0x80)==latin)end++;
                    Paint runPaint=new Paint(paint);runFace(runPaint,paint,pair,latin);
                    prepared.add(new PreparedRun(start,end,advance,runPaint));advance+=runPaint.measureText(text,start,end);start=end;
                }
                width=advance;runs=prepared.toArray(new PreparedRun[prepared.size()]);layout=null;
            }else{
                TextPaint tp=new TextPaint(paint);CharSequence value=styled(paint,text);width=Layout.getDesiredWidth(value,tp);
                int pixels=Math.max(1,(int)Math.ceil(width));
                layout=Build.VERSION.SDK_INT>=23?ModernLayout.create(value,tp,pixels):new StaticLayout(value,tp,pixels,Layout.Alignment.ALIGN_NORMAL,1,0,false);runs=null;
            }
        }
        public void draw(Canvas canvas,float x,float baseline){
            paint.setAntiAlias(smoothing&&antialias);
            if(layout!=null)layout.getPaint().setAntiAlias(smoothing&&antialias);
            if(runs!=null)for(PreparedRun run:runs)run.paint.setAntiAlias(smoothing&&antialias);
            if(align==Paint.Align.CENTER)x-=width/2;else if(align==Paint.Align.RIGHT)x-=width;
            if(layout!=null){int save=canvas.save();canvas.translate(x,baseline-layout.getLineBaseline(0));layout.draw(canvas);canvas.restoreToCount(save);}
            else if(runs!=null){for(PreparedRun run:runs)canvas.drawText(text,run.start,run.end,x+run.x,baseline,run.paint);}
            else canvas.drawText(text,x,baseline,paint);
        }
    }
    private static final class PreparedRun {
        final int start,end;final float x;final Paint paint;
        PreparedRun(int start,int end,float x,Paint paint){this.start=start;this.end=end;this.x=x;this.paint=paint;}
    }
    /** Single-line Canvas text; Layout preserves shaping and bidirectional runs. */
    public static String ellipsize(String text,Paint paint,float width){return android.text.TextUtils.ellipsize(styled(paint,text),new TextPaint(paint),width,android.text.TextUtils.TruncateAt.END).toString();}
    public static void draw(Canvas canvas,String text,float x,float baseline,Paint paint){
        boolean antialias=paint.isAntiAlias();if(!smoothing)paint.setAntiAlias(false);
        try{drawText(canvas,text,x,baseline,paint);}finally{paint.setAntiAlias(antialias);}
    }
    private static void drawText(Canvas canvas,String text,float x,float baseline,Paint paint){
        Pair pair=PAIRS.get(paint.getTypeface());
        if(pair==null){canvas.drawText(text,x,baseline,paint);return;}
        if(simpleLine(text)){float width=paint.getTextAlign()==Paint.Align.LEFT?0:simpleText(null,text,0,0,paint,pair);if(paint.getTextAlign()==Paint.Align.CENTER)x-=width/2;else if(paint.getTextAlign()==Paint.Align.RIGHT)x-=width;simpleText(canvas,text,x,baseline,paint,pair);return;}
        TextPaint tp=new TextPaint(paint);tp.setTextAlign(Paint.Align.LEFT);
        CharSequence value=styled(paint,text);float width=Layout.getDesiredWidth(value,tp);
        int pixels=Math.max(1,(int)Math.ceil(width));
        StaticLayout layout=Build.VERSION.SDK_INT>=23?ModernLayout.create(value,tp,pixels)
            :new StaticLayout(value,tp,pixels,Layout.Alignment.ALIGN_NORMAL,1,0,false);
        if(paint.getTextAlign()==Paint.Align.CENTER)x-=width/2;else if(paint.getTextAlign()==Paint.Align.RIGHT)x-=width;
        int save=canvas.save();canvas.translate(x,baseline-layout.getLineBaseline(0));layout.draw(canvas);canvas.restoreToCount(save);
    }
    /** Calendar SHDrawText flags0x2042: centered short text, left-aligned ASCII
     * dots on overflow, inset3*CXEDGE. Android metrics substitute for GDI.
     * Prefix cap259 follows the native buffer; code points are never split.
     */
    public static void drawCenteredListCaption(Canvas canvas,String text,RectF bounds,Paint paint){
        if(canvas==null||text==null||bounds==null||paint==null)throw new IllegalArgumentException("caption drawing arguments required");
        float inset=3*WmMetrics.EDGE,width=bounds.width()-2*inset;if(bounds.isEmpty()||width<=0)return;
        Paint left=paint;if(paint.getTextAlign()!=Paint.Align.LEFT){left=new Paint(paint);left.setTextAlign(Paint.Align.LEFT);}
        boolean overflow=measure(left,text)>width;String caption=text;
        if(overflow){
            float available=width-measure(left,"...");int high=text.codePointCount(0,Math.min(259,text.length()));if(text.offsetByCodePoints(0,high)>259)high--;
            int low=0;while(low<high){int mid=(low+high+1)/2;if(measure(left,text.substring(0,text.offsetByCodePoints(0,mid)))<=available)low=mid;else high=mid-1;}
            caption=text.substring(0,text.offsetByCodePoints(0,Math.max(1,low)))+"...";
        }
        float x=overflow?bounds.left+inset:(float)Math.floor((bounds.left+bounds.right-Math.ceil(measure(left,caption)))/2);
        Paint.FontMetrics fm=left.getFontMetrics();float baseline=bounds.top+Math.max(0,(int)(bounds.height()-Math.ceil(fm.descent-fm.ascent))/2)-fm.ascent;
        int save=canvas.save();canvas.clipRect(bounds);draw(canvas,caption,x,baseline,left);canvas.restoreToCount(save);
    }
    private static final class ModernLayout {
        static StaticLayout create(CharSequence value,TextPaint paint,int width){
            return StaticLayout.Builder.obtain(value,0,value.length(),paint,width)
                .setIncludePad(false).setBreakStrategy(0).setMaxLines(1).build();
        }
    }
    /** Optical center for a fixed-height title/clock slot, independent of font ascent. */
    public static float centerBaseline(Paint paint,float top,float bottom,String text){
        if(text.isEmpty()){Paint.FontMetrics fm=paint.getFontMetrics();return (top+bottom-fm.ascent-fm.descent)/2;}
        Rect ink=new Rect();textBounds(paint,text,ink);
        return (top+bottom-ink.top-ink.bottom)/2;
    }
    /** Unions linked-face run ink at the origin, without horizontal advances.
     * Use measure() or PreparedLine.width for the complete string advance. */
    public static void textBounds(Paint paint,String text,Rect ink){
        if(paint==null||text==null||ink==null)throw new IllegalArgumentException("paint/text/bounds required");
        ink.setEmpty();Pair pair=PAIRS.get(paint.getTypeface());
        if(pair==null)paint.getTextBounds(text,0,text.length(),ink);
        else {Paint copy=new Paint(paint);runs(text,(start,end,latin)->{runFace(copy,paint,pair,latin);Rect run=new Rect();copy.getTextBounds(text,start,end,run);ink.union(run);});}
    }

}
