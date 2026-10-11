package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;

/** GWE registry metrics at 192 DPI: cxVScr=26, cyVScr=40, cyThMin=26.
 * GWE AE0C8/ADE0C supplies flat frames, inverted arrows and three thumb grips;
 * see design/scrollbar-paint.md for source and GDI raster limits.
 * Invisible finger margins and relative dragging deliberately
 * adapt the stylus-sized control for capacitive touch without widening its paint.
 */
public class WmScrollBar extends View implements WmControls.RestyleListener {
    public interface Host {
        int extent();
        int range();
        int offset();
        int lineStep();
        void scrollTo(int offset);
        void scrollBy(int delta);
        public void stopFling();
    }
    private final Host host;
    private int faceColor;
    private static final int FINGER_MARGIN=24;
    // GWES 94C5C -> 48AB0 -> 48770: initial and recurring timer are both100ms.
    private static final int REPEAT_MS=100;
    private final Paint paint=new Paint();
    private final int touchSlop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
    private int pressed,pointer=-1,dragStartOffset,dragTarget;
    private float dragStartY,touchStartX;
    private final Runnable repeat=new Runnable(){public void run(){if((pressed==1||pressed==2)&&step(pressed==1?-host.lineStep():host.lineStep()))postDelayed(this,REPEAT_MS);}};
    public WmScrollBar(Context context,Host host,int face){super(context);this.host=host;faceColor=face;WmControls.bindDescription(this,"en","세로 스크롤");setFocusable(true);if(android.os.Build.VERSION.SDK_INT>=16)setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    public void onWmRestyle(android.graphics.Typeface regular,android.graphics.Typeface bold,int selection,int tabColor){faceColor=tabColor;invalidate();}

    private int extent(){return host.extent();}
    private int range(){return host.range();}
    private int offset(){return host.offset();}
    public void refresh(){
        int visibility=extent()>0&&range()>extent()?VISIBLE:GONE;
        if(getVisibility()!=visibility){if(visibility==GONE)stop();setVisibility(visibility);}
        invalidate();
    }
    /** Coordinates are local to this bar; the extra left margin is invisible. */
    public boolean hitThumb(float x,float y){return isEnabled()&&getVisibility()==VISIBLE&&x>=-FINGER_MARGIN&&x<getWidth()&&nearThumb(y);}
    @Override public void setEnabled(boolean enabled){super.setEnabled(enabled);if(!enabled)stop();}
    @Override protected void onVisibilityChanged(View changedView,int visibility){super.onVisibilityChanged(changedView,visibility);if(visibility!=VISIBLE)stop();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility!=VISIBLE)stop();}
    @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    private int buttonHeight(){return Math.min(40,getHeight()/2);}
    private int trackHeight(){return Math.max(0,getHeight()-2*buttonHeight());}
    private int thumbHeight(){return Math.min(trackHeight(),Math.max(26,range()==0?trackHeight():Math.round(trackHeight()*extent()/(float)range())));}
    private int thumbTop(){return buttonHeight()+(int)Math.round(Math.max(0,trackHeight()-thumbHeight())*Math.min(Math.max(0,range()-extent()),offset())/(double)Math.max(1,range()-extent()));}
    private boolean nearThumb(float y){return y>=buttonHeight()&&y<getHeight()-buttonHeight()&&y>=thumbTop()-FINGER_MARGIN&&y<thumbTop()+thumbHeight()+FINGER_MARGIN;}
    @Override protected void onDraw(Canvas canvas){
        int w=getWidth(),h=getHeight(),button=buttonHeight(),top=thumbTop();
        // AE34C sets flag8 when compressed arrow height (h/2+1) is below26;
        // AE0C8 suppresses all paint. At192DPI this means h<50.
        if(h<50)return;
        // AE34C compresses overlapping arrows at h<80: upper bottom is
        // midpoint+1, lower top is midpoint (also for odd heights).
        int upper=h<80?h/2+1:button,lower=h<80?h/2:h-button;
        // ADFBC uses WHITE_BRUSH for the track; only control faces are themed.
        fill(canvas,0,0,w,h,Color.WHITE);frame(canvas,0,upper-2,w,lower+2);
        box(canvas,0,0,w,upper,pressed==1);box(canvas,0,lower,w,h,pressed==2);
        int bottom=top+thumbHeight();
        // AE34C extends the painted thumb by metricY2 above/below; AE808
        // removes those borders before tracking. Keep logical drag geometry unchanged.
        // AE0C8 disabled flag4 makes ADFBC fill the entire white track,
        // bypassing ADE0C's thumb face/frame/grips.
        if(isEnabled()&&h>=80&&bottom>top)box(canvas,0,top-2,w,bottom+2,false);
        // ADE0C: black2px pen; x inset=metricX2+extraX4, pitch=2*metricY2.
        int middle=top+(bottom-top)/2;
        if(isEnabled())for(int y=middle-4;y<=middle+4;y+=4)if(y-1>=top&&y+1<=bottom)fill(canvas,6,y-1,w-6,y+1,Color.BLACK);
        arrow(canvas,0,0,w,upper,false,pressed==1);arrow(canvas,0,lower,w,h,true,pressed==2);
    }
    private void fill(Canvas canvas,int l,int t,int r,int b,int color){if(r<=l||b<=t)return;paint.setColor(color);canvas.drawRect(l,t,r,b,paint);}
    private void box(Canvas canvas,int l,int t,int r,int b,boolean down){
        fill(canvas,l,t,r,b,down?Color.BLACK:faceColor);frame(canvas,l,t,r,b);
    }
    private void frame(Canvas canvas,int l,int t,int r,int b){if(r<=l||b<=t)return;fill(canvas,l,t,r,Math.min(b,t+2),Color.BLACK);fill(canvas,l,t,Math.min(r,l+2),b,Color.BLACK);fill(canvas,l,Math.max(t,b-2),r,b,Color.BLACK);fill(canvas,Math.max(l,r-2),t,r,b,Color.BLACK);}
    private void arrow(Canvas canvas,int l,int t,int r,int b,boolean down,boolean pressed){
        // 91A08 inflates by2; 914FC sizes/centres; 90BA4 paints 1px scanlines.
        // At26x40 this is19x10, local bounds[4,15,23,25). Press changes colour only.
        int side=Math.min(r-l+4,b-t+4);if(side<6)return;
        int rows=side<=12?4:6+(side-13)/4,width=2*rows-1;
        // 914FC inactive0x100 finishes with EB7CC (COLOR_GRAYTEXT17).
        int x=l+(r-l-width+1)/2,y=t+(b-t-rows+1)/2,color=!isEnabled()?WmControls.SHADOW:pressed?Color.WHITE:Color.BLACK;
        for(int row=0;row<rows;row++){int inset=down?row:rows-1-row;fill(canvas,x+inset,y+row,x+width-inset,y+row+1,color);}
    }
    private boolean step(int amount){if(!isEnabled())return false;int before=offset(),target=(int)Math.max(0,Math.min(Math.max(0,range()-extent()),(long)before+amount));if(target==before)return false;host.scrollBy(target-before);invalidate();return true;}
    /** Content owners may supply their native page geometry; Host stays unchanged. */
    protected int pageStep(){return Math.max(host.lineStep(),extent()-host.lineStep());}
    private void seek(float y){
        int travel=trackHeight()-thumbHeight();if(travel<=0)return;
        int max=Math.max(0,range()-extent());
        int destination=Math.max(0,Math.min(max,dragStartOffset+Math.round((y-dragStartY)*max/travel)));
        if(destination==dragTarget)return;dragTarget=destination;
        // Absolute pixel position, no duration-zero smooth-scroller or row snapping.
        host.scrollTo(destination);invalidate();
    }
    public void stop(){removeCallbacks(repeat);pressed=0;pointer=-1;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);invalidate();}
    @Override public boolean onTouchEvent(MotionEvent event){
        if(!isEnabled()){stop();return false;}
        int index=event.getActionMasked()==MotionEvent.ACTION_DOWN||pointer<0?0:event.findPointerIndex(pointer);float y=index<0?0:event.getY(index),x=index<0?0:event.getX(index);int button=buttonHeight();
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                stop();pointer=event.getPointerId(0);dragStartY=y;touchStartX=x;dragStartOffset=dragTarget=offset();
                host.stopFling(); // Stop an existing content fling before taking the thumb.
                getParent().requestDisallowInterceptTouchEvent(true);
                if(y<button){pressed=1;if(step(-host.lineStep()))postDelayed(repeat,REPEAT_MS);}
                else if(y>=getHeight()-button){pressed=2;if(step(host.lineStep()))postDelayed(repeat,REPEAT_MS);}
                else pressed=nearThumb(y)?3:4;
                invalidate();return true;
            case MotionEvent.ACTION_MOVE:
                if(index<0){stop();return true;}
                if(pressed==3)seek(y);
                else if(pressed==4){
                    if(Math.abs(y-dragStartY)>touchSlop){pressed=3;dragStartY+=Math.copySign(touchSlop,y-dragStartY);dragStartOffset=dragTarget=offset();seek(y);invalidate();}
                    else if(Math.abs(x-touchStartX)>touchSlop)stop();
                }else if((pressed==1&&(y<0||y>=button))||(pressed==2&&(y<getHeight()-button||y>=getHeight()))||x<-FINGER_MARGIN||x>=getWidth()+FINGER_MARGIN)stop();
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                if(event.getPointerId(event.getActionIndex())==pointer){int next=event.getActionIndex()==0?1:0;pointer=event.getPointerId(next);dragStartY=event.getY(next);touchStartX=event.getX(next);dragStartOffset=pressed==3?dragTarget:offset();removeCallbacks(repeat);if(pressed!=3)stop();}return true;
            case MotionEvent.ACTION_UP:
                if(pressed==4&&index>=0)step((y<thumbTop()?-1:1)*pageStep());
                stop();performClick();return true;
            case MotionEvent.ACTION_CANCEL:stop();return true;
            default:return true;
        }
    }
    // GWES94C5C maps VK_PRIOR/NEXT/HOME/END and all four arrows to scroll codes.
    private static boolean scrollKey(int key){return key==KeyEvent.KEYCODE_PAGE_UP||key==KeyEvent.KEYCODE_PAGE_DOWN||key==KeyEvent.KEYCODE_MOVE_HOME||key==KeyEvent.KEYCODE_MOVE_END||key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT;}
    @Override public boolean onKeyDown(int key,KeyEvent event){
        if(!isEnabled()||!event.hasNoModifiers()||!scrollKey(key))return super.onKeyDown(key,event);
        host.stopFling();
        switch(key){
            case KeyEvent.KEYCODE_MOVE_HOME:host.scrollTo(0);invalidate();break;
            case KeyEvent.KEYCODE_MOVE_END:host.scrollTo(Math.max(0,range()-extent()));invalidate();break;
            case KeyEvent.KEYCODE_PAGE_UP:step(-pageStep());break;
            case KeyEvent.KEYCODE_PAGE_DOWN:step(pageStep());break;
            case KeyEvent.KEYCODE_DPAD_UP:case KeyEvent.KEYCODE_DPAD_LEFT:step(-host.lineStep());break;
            default:step(host.lineStep());break;
        }
        return true;
    }
    @Override public boolean onKeyUp(int key,KeyEvent event){return isEnabled()&&event.hasNoModifiers()&&scrollKey(key)||super.onKeyUp(key,event);}
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setScrollable(true);info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);}
    @Override public boolean performAccessibilityAction(int action,Bundle args){if(!isEnabled())return false;if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD||action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD){step((action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD?1:-1)*pageStep());return true;}return android.os.Build.VERSION.SDK_INT>=16&&super.performAccessibilityAction(action,args);}
}
