package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.app.Activity;
import android.graphics.Canvas;
import android.view.WindowManager;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;

/** Shared Android surface for the ROM-based logical VGA coordinate contract.
 * The default logical width is480; smaller UI scales expand the logical width.
 * Uniform scaling fills the available physical width and height.
 * This is an Android adaptation, not a claim about native WinCE rendering.
 */
public class WmViewport extends ViewGroup {
    private int touchSoundLevel;
    private WmTouchFeedback touchFeedback;
    private String touchRevision;
    /** ROM Screen low word: 0 off, 1 soft, 2 loud. Applies immediately. */
    public final void setTouchSoundLevel(int level){
        if(level<0||level>2)throw new IllegalArgumentException("Touch sound level outside0..2");
        touchSoundLevel=level;
        if(level==0){releaseTouchFeedback();}else{
            String revision=WmResources.revision(getContext());if(!revision.equals(touchRevision))releaseTouchFeedback();
            if(getWindowToken()!=null&&touchFeedback==null){touchFeedback=new WmTouchFeedback(getContext());touchRevision=WmResources.revision(getContext());}
        }
    }
    public final int getTouchSoundLevel(){return touchSoundLevel;}
    private void releaseTouchFeedback(){if(touchFeedback!=null){touchFeedback.release();touchFeedback=null;}}
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        int action=event.getActionMasked();boolean handled=super.dispatchTouchEvent(event);
        if(handled&&action==MotionEvent.ACTION_DOWN&&touchSoundLevel!=0&&touchFeedback!=null)touchFeedback.play(touchSoundLevel);
        return handled;
    }
    private int percent=100;
    private String screenMode="full";
    private android.view.Window appliedWindow;
    private boolean imeInsetsObserved;
    private ViewTreeObserver imeObserver;
    private final android.graphics.Rect visibleFrame=new android.graphics.Rect();
    private final int[] screenLocation=new int[2];
    private final ViewTreeObserver.OnGlobalLayoutListener legacyIme=()->{
        getWindowVisibleDisplayFrame(visibleFrame);if(visibleFrame.bottom<=0)return;
        getLocationOnScreen(screenLocation);
        int obscured=Math.max(0,screenLocation[1]+getHeight()-visibleFrame.bottom);
        // ponytail: pre30 has no public IME inset; ignore small system bars.
        // Floating keyboards need API30 visibility/insets for exact detection.
        setBottomInset(obscured>100*getResources().getDisplayMetrics().density?obscured:0);
    };
    public static int scalePercent(int value){return Math.max(50,Math.min(100,value));}
    public static String screenMode(String value){return "black".equals(value)||"status".equals(value)?value:"full";}
    public final int getLogicalWidth(){return Math.round(WmMetrics.WIDTH*100f/percent);}
    public final void setScalePercent(int value){int next=scalePercent(value);if(percent==next)return;onViewportBoundsChanged();percent=next;requestLayout();}
    public final void setTopInset(int pixels){setInsets(pixels,getPaddingBottom());}
    private void setInsets(int top,int bottom){top=Math.max(0,top);bottom=Math.max(0,bottom);if(getPaddingTop()==top&&getPaddingBottom()==bottom)return;onViewportBoundsChanged();setPadding(0,top,0,bottom);requestLayout();}
    /** Apply the same display policy to hosted and independent SDK windows. */
    public final void applyDisplay(Activity owner,int scale,String mode){
        // Rasterize logical pixels before the viewport scales the result.
        View logical=content();if(logical!=null)WmFonts.applySmoothingLayer(logical);
        setScalePercent(scale);String next=screenMode(mode);
        android.view.Window window=owner.getWindow();applyImePolicy(window);if(appliedWindow==window&&next.equals(screenMode))return;
        appliedWindow=window;screenMode=next;boolean status="status".equals(screenMode);
        if(status)window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);else window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        int flags=View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if(!status)flags|=View.SYSTEM_UI_FLAG_FULLSCREEN;
        if(Build.VERSION.SDK_INT>=16)flags|=View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        if(Build.VERSION.SDK_INT>=19)flags|=View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        window.getDecorView().setSystemUiVisibility(flags);
        if(Build.VERSION.SDK_INT>=21){window.setStatusBarColor(0xff000000);window.setNavigationBarColor(0xff000000);}
        if(Build.VERSION.SDK_INT>=28){WindowManager.LayoutParams attributes=window.getAttributes();attributes.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;window.setAttributes(attributes);}
        if(Build.VERSION.SDK_INT>=30)window.setDecorFitsSystemWindows(false);
        setTopInset("black".equals(screenMode)||(status&&Build.VERSION.SDK_INT>=16)?statusHeight():0);
        if(Build.VERSION.SDK_INT>=20)requestApplyInsets();
    }
    private int statusHeight(){int id=getResources().getIdentifier("status_bar_height","dimen","android");return id==0?0:getResources().getDimensionPixelSize(id);}
    public WmViewport(Context context){
        super(context);setBackgroundColor(0xff000000);setClipChildren(false);setClipToPadding(false);
    }
    /** Exactly one logical window may occupy the viewport. */
    public final void setContent(View content){
        if(content==null)throw new IllegalArgumentException("Viewport content required");
        if(content()==content)return;
        removeAllViews();addView(content,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        WmFonts.applySmoothingLayer(content);
    }
    public final View content(){return getChildCount()==0?null:getChildAt(0);}
    @Override public void addView(View child,int index,ViewGroup.LayoutParams params){
        if(getChildCount()!=0)throw new IllegalStateException("Viewport accepts one logical window");
        super.addView(child,index,params);
    }
    /** Uniform physical pixels per logical VGA pixel; zero before a physical width exists. */
    public final float getScale(){return getWidth()/(float)getLogicalWidth();}
    public final int getUsableHeight(){return Math.max(0,getHeight()-getPaddingTop()-getPaddingBottom());}
    public final int getLogicalHeight(){View child=content();return child==null?0:child.getMeasuredHeight();}
    /** Physical bottom inset, applied exactly once before logical-height calculation. */
    public final void setBottomInset(int pixels){
        setInsets(getPaddingTop(),pixels);
    }
    /** Opt in for IME-aware layout: API30+ ADJUST_NOTHING and explicit insets;
     * earlier versions require ADJUST_RESIZE for the visible window frame.
     * applyDisplay configures the owning window policy.
     */
    public final void observeImeInsets(){
        imeInsetsObserved=true;attachLegacyIme();
        if(appliedWindow!=null)applyImePolicy(appliedWindow);
        if(Build.VERSION.SDK_INT>=21)setOnApplyWindowInsetsListener((view,insets)->{
            int top=0,bottom=getPaddingBottom();
            if(!"full".equals(screenMode)){
                top=Math.max(statusHeight(),insets.getSystemWindowInsetTop());
                if(Build.VERSION.SDK_INT>=28&&insets.getDisplayCutout()!=null)top=Math.max(top,insets.getDisplayCutout().getSafeInsetTop());
            }
            if(Build.VERSION.SDK_INT>=30)bottom=insets.getInsets(WindowInsets.Type.ime()).bottom;
            setInsets(top,bottom);return insets;
        });
    }
    private void applyImePolicy(android.view.Window window){
        if(!imeInsetsObserved)return;
        int current=window.getAttributes().softInputMode;
        int adjust=Build.VERSION.SDK_INT>=30?WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING:WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        if((current&WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)!=adjust)
            window.setSoftInputMode((current&~WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)|adjust);
    }
    private void attachLegacyIme(){
        if(!imeInsetsObserved||Build.VERSION.SDK_INT>=30||getWindowToken()==null||imeObserver!=null)return;
        imeObserver=getViewTreeObserver();imeObserver.addOnGlobalLayoutListener(legacyIme);
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attachLegacyIme();setTouchSoundLevel(touchSoundLevel);}
    @Override protected void onDetachedFromWindow(){
        releaseTouchFeedback();
        if(imeObserver!=null&&imeObserver.isAlive())imeObserver.removeGlobalOnLayoutListener(legacyIme);
        imeObserver=null;setBottomInset(0);super.onDetachedFromWindow();
    }
    /** Cancel host-owned coordinate-dependent feedback before a bounds change. */
    protected void onViewportBoundsChanged(){}
    @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){
        super.onSizeChanged(width,height,oldWidth,oldHeight);onViewportBoundsChanged();
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getSize(widthSpec),height=MeasureSpec.getSize(heightSpec);setMeasuredDimension(width,height);
        View child=content();if(child==null)return;
        int logicalWidth=getLogicalWidth();float scale=width/(float)logicalWidth;
        int usable=Math.max(0,height-getPaddingTop()-getPaddingBottom());int logical=scale>0?Math.max(1,Math.round(usable/scale)):1;
        child.measure(MeasureSpec.makeMeasureSpec(logicalWidth,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(logical,MeasureSpec.EXACTLY));
    }
    @Override protected void dispatchDraw(Canvas canvas){int saved=canvas.save();canvas.clipRect(0,getPaddingTop(),getWidth(),Math.max(getPaddingTop(),getHeight()-getPaddingBottom()));super.dispatchDraw(canvas);canvas.restoreToCount(saved);}
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
        View child=content();if(child==null)return;
        child.layout(0,getPaddingTop(),getLogicalWidth(),getPaddingTop()+child.getMeasuredHeight());float scale=getScale();
        child.setPivotX(0);child.setPivotY(0);child.setScaleX(scale);child.setScaleY(scale);child.setTranslationX(0);child.setTranslationY(0);
    }
}
