package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ScrollView;

/** Shared applet scrolling: classic paint, ordinary finger swipes, larger thumb hit area.
 * Geometry is expressed in VGA pixels; {@link WmViewport} owns the only display scale.
 */
public class WmScrollView extends FrameLayout {
    private final ScrollView scroll;
    private final View contents;
    private final WmScrollBar bar;
    private final Rect barBounds=new Rect();
    private final Runnable updateBarAfterLayout=this::updateBar;
    private boolean thumbGesture;
    private Integer pendingPosition;

    public WmScrollView(Context context,View contents){this(context,contents,LayoutParams.WRAP_CONTENT,0xffc9e7e9);}
    public WmScrollView(Context context,View contents,int contentHeight){this(context,contents,contentHeight,0xffc9e7e9);}
    public WmScrollView(Context context,View contents,int contentHeight,int face){
        super(context);this.contents=contents;
        scroll=new ScrollView(context);scroll.setFillViewport(false);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);scroll.setVerticalScrollBarEnabled(false);scroll.setHorizontalScrollBarEnabled(false);
        scroll.setPadding(0,0,0,0);
        // A bare Canvas View (calendar time grid) otherwise measures to zero
        // under ScrollView's UNSPECIFIED child height despite LayoutParams.height.
        if(contentHeight>0)contents.setMinimumHeight(contentHeight);
        scroll.addView(contents,new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT,contentHeight));
        addView(scroll,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        bar=new WmScrollBar(context,new WmScrollBar.Host(){
            public int extent(){return scroll.getHeight();}
            public int range(){return WmScrollView.this.contents.getHeight();}
            public int offset(){return scroll.getScrollY();}
            // GWES A8764 advances one ordinary-list top index; A79D8
            // converts it using the current font-derived row height.
            public int lineStep(){return contents instanceof WmRadioMenu&&((WmRadioMenu)contents).choiceKeyboardEnabled()&&((WmRadioMenu)contents).row(0)!=null?((WmRadioMenu)contents).row(0).getLayoutParams().height:WmControls.ROW_HEIGHT;}
            public void scrollTo(int offset){scroll.scrollTo(0,offset);}
            public void scrollBy(int delta){scroll.scrollBy(0,delta);}
            public void stopFling(){WmScrollView.this.stopFling();}
        },face){
            @Override protected int pageStep(){
                if(contents instanceof WmRadioMenu&&((WmRadioMenu)contents).choiceKeyboardEnabled()){
                    WmRadioMenu menu=(WmRadioMenu)contents;return menu.row(0)==null?0:menu.choicePageStep()*menu.row(0).getHeight();
                }
                return super.pageStep();
            }
        };
        LayoutParams position=new LayoutParams(26,LayoutParams.MATCH_PARENT,android.view.Gravity.RIGHT);addView(bar,position);
        bar.setVisibility(GONE);
        scroll.getViewTreeObserver().addOnScrollChangedListener(this::updateBar);
        scroll.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->scheduleBarUpdate());
        contents.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->scheduleBarUpdate());
    }
    @Override protected void onMeasure(int width,int height){
        super.onMeasure(width,height);
        // Decide the client width before its first layout/draw, rather than
        // revealing the bar in a posted task and shifting an already drawn page.
        boolean overflow=scroll.getMeasuredHeight()>0&&contents.getMeasuredHeight()>scroll.getMeasuredHeight();
        int visibility=overflow?VISIBLE:GONE,inset=overflow?26:0;
        LayoutParams position=(LayoutParams)scroll.getLayoutParams();
        if(bar.getVisibility()!=visibility||position.rightMargin!=inset){
            if(!overflow)bar.stop();bar.setVisibility(visibility);position.rightMargin=inset;scroll.setLayoutParams(position);
            super.onMeasure(width,height);
        }
    }
    public void scrollToPosition(int y){
        stopFling();pendingPosition=Math.max(0,y);applyPosition();
        if(pendingPosition!=null)scheduleBarUpdate();
    }
    /** Use ScrollView's native touch cancellation, without reflecting its Scroller.
     * fling(0) starts a new animation and can overwrite a subsequent scrollTo.
     * These calls bypass child dispatch, so no content click/selection is sent.
     */
    public void stopFling(){
        long now=android.os.SystemClock.uptimeMillis();
        MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,0,0,0);
        MotionEvent cancel=MotionEvent.obtain(now,now,MotionEvent.ACTION_CANCEL,0,0,0);
        try{scroll.onTouchEvent(down);scroll.onTouchEvent(cancel);scroll.onInterceptTouchEvent(cancel);}
        finally{down.recycle();cancel.recycle();}
    }
    private void applyPosition(){
        if(pendingPosition==null||scroll.getHeight()<=0||isLayoutRequested()||scroll.isLayoutRequested()||contents.isLayoutRequested())return;
        int target=pendingPosition;pendingPosition=null;scroll.scrollTo(0,target);
    }
    public int getScrollPosition(){return scroll.getScrollY();}
    /** Latest requested offset, including restoration awaiting the next layout.
     * Geometry changes use this value so successive restyles preserve position.
     */
    public int getRequestedScrollPosition(){return pendingPosition==null?scroll.getScrollY():pendingPosition;}
    /** Font-measured Canvas controls can change their document extent live. */
    public void setContentHeight(int height){
        if(height<=0)throw new IllegalArgumentException("positive content height required");
        contents.setMinimumHeight(height);
        android.view.ViewGroup.LayoutParams size=contents.getLayoutParams();
        if(size.height!=height){size.height=height;contents.setLayoutParams(size);}
        scheduleBarUpdate();
    }
    private void scheduleBarUpdate(){
        // Visibility and the scroll inset need a fresh measure pass. Requests
        // inside a child's layout can leave the newly visible bar at 0x0.
        removeCallbacks(updateBarAfterLayout);post(updateBarAfterLayout);
    }
    private void updateBar(){
        if(android.os.Build.VERSION.SDK_INT>=18&&isInLayout()){scheduleBarUpdate();return;}
        bar.refresh();int inset=bar.getVisibility()==VISIBLE?26:0;
        LayoutParams position=(LayoutParams)scroll.getLayoutParams();
        if(position.rightMargin!=inset){position.rightMargin=inset;scroll.setLayoutParams(position);}
        applyPosition();
    }
    @Override public void setEnabled(boolean enabled){
        boolean changed=isEnabled()!=enabled;super.setEnabled(enabled);if(bar==null)return;
        scroll.setEnabled(enabled);bar.setEnabled(enabled);
        if(!enabled){thumbGesture=false;stopFling();}
        if(!enabled&&changed){
            long now=android.os.SystemClock.uptimeMillis();MotionEvent cancel=MotionEvent.obtain(now,now,MotionEvent.ACTION_CANCEL,0,0,0);
            try{super.dispatchTouchEvent(cancel);}finally{cancel.recycle();}
        }
    }
    @Override public boolean dispatchKeyEvent(android.view.KeyEvent event){return isEnabled()&&super.dispatchKeyEvent(event);}
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        if(!isEnabled())return false;
        barBounds.set(0,0,bar.getWidth(),bar.getHeight());offsetDescendantRectToMyCoords(bar,barBounds);
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){pendingPosition=null;thumbGesture=bar.hitThumb(event.getX()-barBounds.left,event.getY()-barBounds.top);}
        if(!thumbGesture)return super.dispatchTouchEvent(event);
        MotionEvent local=MotionEvent.obtain(event);local.offsetLocation(-barBounds.left,-barBounds.top);
        boolean handled=bar.dispatchTouchEvent(local);local.recycle();
        if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL)thumbGesture=false;
        return handled;
    }
    @Override protected void onDetachedFromWindow(){removeCallbacks(updateBarAfterLayout);pendingPosition=null;thumbGesture=false;bar.stop();super.onDetachedFromWindow();}
}
