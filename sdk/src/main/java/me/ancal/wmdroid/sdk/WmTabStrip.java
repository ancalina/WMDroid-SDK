package me.ancal.wmdroid.sdk;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared property-sheet tabs in logical VGA; native font/DLU metrics remain provisional. */
public final class WmTabStrip extends LinearLayout implements WmControls.RestyleListener {
    private int color,firstVisibleIndex,upperBound,viewportWidth,overflowSize,widthSignature;
    private boolean revealPending=true;
    private final FrameLayout overflow;
    private final Arrow left,right;
    public WmTabStrip(Context context,int color){
        super(context);this.color=color;setGravity(Gravity.BOTTOM);setPadding(0,0,0,0);setBackgroundColor(color);
        setLayoutParams(new LinearLayout.LayoutParams(-1,WmMetrics.TAB));
        overflow=new FrameLayout(context);overflow.setClickable(true);
        left=new Arrow(context,-1);right=new Arrow(context,1);
        overflow.addView(left);overflow.addView(right);overflow.setVisibility(GONE);
        addView(overflow,new LayoutParams(0,WmMetrics.TAB));
    }
    public TextView addTab(String title,Typeface font,Runnable action){
        TextView tab=WmControls.text(getContext(),font,title);tab.setGravity(Gravity.CENTER);WmControls.tab(tab,false,color);
        tab.setFocusable(true);tab.setFocusableInTouchMode(false);tab.setOnKeyListener(this::onTabKey);
        tab.setOnClickListener(v->{if(isEnabled()&&v.isEnabled())action.run();});addView(tab,tabCount(),new LayoutParams(WmControls.tabWidth(tab),WmMetrics.TAB));
        tab.addTextChangedListener(new TextWatcher(){
            private String caption=tab.getText().toString();
            public void beforeTextChanged(CharSequence text,int start,int count,int after){}
            public void onTextChanged(CharSequence text,int start,int before,int count){}
            public void afterTextChanged(Editable text){
                boolean changed=!caption.contentEquals(text);caption=text.toString();
                ViewGroup.LayoutParams size=tab.getLayoutParams();if(size!=null&&size.width>=0){int width=WmControls.tabWidth(tab);if(size.width!=width){size.width=width;tab.setLayoutParams(size);}}
                if(changed){revealPending=true;requestLayout();}
            }
        });revealPending=true;return tab;
    }
    private int tabCount(){return indexOfChild(overflow);}
    // Native32A5070/32A4F3C: ordinary horizontal tabs select through the owner,
    // unlike button-style tabs whose keyboard caret is separate from selection.
    private boolean onTabKey(View source,int key,KeyEvent event){
        if(key!=KeyEvent.KEYCODE_DPAD_LEFT&&key!=KeyEvent.KEYCODE_DPAD_RIGHT&&key!=KeyEvent.KEYCODE_MOVE_HOME&&key!=KeyEvent.KEYCODE_MOVE_END)return false;
        if(!isEnabled()||!source.isEnabled())return false;
        if(event.getAction()==KeyEvent.ACTION_UP)return true;
        if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
        int count=tabCount(),current=-1;if(count==0)return true;
        for(int i=0;i<count;i++)if(getChildAt(i).isSelected()){current=i;break;}
        // Native32BE160 drops WANTARROWS at either horizontal boundary;
        // the owner decides where an unhandled DOWN goes next.
        if(key==KeyEvent.KEYCODE_DPAD_LEFT&&current==0||key==KeyEvent.KEYCODE_DPAD_RIGHT&&current==count-1)return false;
        int next=key==KeyEvent.KEYCODE_MOVE_HOME?0:key==KeyEvent.KEYCODE_MOVE_END?count-1:Math.max(0,Math.min(count-1,current+(key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1)));
        if(next!=current&&getChildAt(next).isEnabled())getChildAt(next).performClick();
        // The page gate may veto, redirect or reentrantly select another page.
        for(int i=0;i<tabCount();i++)if(getChildAt(i).isSelected()){getChildAt(i).requestFocus();break;}
        return true;
    }
    void onTabSelectionChanged(TextView tab){if(tab.isSelected()){revealPending=true;requestLayout();}}
    private int tabWidth(int index){return getChildAt(index).getMeasuredWidth();}
    private int prefix(int index){int width=0;for(int i=0;i<index;i++)width+=tabWidth(i);return width;}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int total=0,signature=1,count=tabCount();
        for(int i=0;i<count;i++){
            View tab=getChildAt(i);ViewGroup.LayoutParams size=tab.getLayoutParams();
            int width=Math.max(0,size.width);tab.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(WmMetrics.TAB,MeasureSpec.EXACTLY));
            total+=tab.getMeasuredWidth();signature=31*signature+tab.getMeasuredWidth();
            TextView caption=(TextView)tab;signature=31*signature+System.identityHashCode(caption.getTypeface());
            signature=31*signature+Float.floatToIntBits(caption.getTextSize());
            signature=31*signature+Float.floatToIntBits(caption.getTextScaleX());
            signature=31*signature+Float.floatToIntBits(caption.getPaint().ascent());
            signature=31*signature+Float.floatToIntBits(caption.getPaint().descent());
        }
        int width=resolveSize(total,widthSpec),height=resolveSize(WmMetrics.TAB,heightSpec);
        // ponytail: Android ascent/descent stands in for native tmHeight; clamp to
        // the available SDK strip. Replace when the original runtime metric is proved.
        int fontHeight=count==0?WmMetrics.FONT:Math.round(((TextView)getChildAt(0)).getPaint().descent()-((TextView)getChildAt(0)).getPaint().ascent());
        int side=Math.max(0,Math.min(fontHeight+2*WmMetrics.EDGE,Math.min(height,width/2)));
        boolean needed=total>width&&count>0;
        int available=needed?Math.max(0,width-2*side):width;
        if(available!=viewportWidth||signature!=widthSignature||side!=overflowSize)revealPending=true;
        viewportWidth=available;widthSignature=signature;overflowSize=side;
        overflow.setVisibility(needed?VISIBLE:GONE);
        overflow.measure(MeasureSpec.makeMeasureSpec(2*side,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(side,MeasureSpec.EXACTLY));
        left.measure(MeasureSpec.makeMeasureSpec(side+1,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(side,MeasureSpec.EXACTLY));
        right.measure(MeasureSpec.makeMeasureSpec(side+1,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(side,MeasureSpec.EXACTLY));
        upperBound=0;
        if(needed){int suffix=tabWidth(count-1);upperBound=count-1;while(upperBound>0&&suffix+tabWidth(upperBound-1)<=viewportWidth)suffix+=tabWidth(--upperBound);}
        firstVisibleIndex=Math.max(0,Math.min(firstVisibleIndex,upperBound));
        if(revealPending){revealSelected();revealPending=false;}
        updateArrows();setMeasuredDimension(width,height);
    }
    private void revealSelected(){
        if(overflow.getVisibility()!=VISIBLE)return;
        for(int i=0;i<tabCount();i++)if(getChildAt(i).isSelected()){
            int start=prefix(i)-prefix(firstVisibleIndex),end=start+tabWidth(i);
            if(start<0||tabWidth(i)>viewportWidth)firstVisibleIndex=i;
            else while(end>viewportWidth&&firstVisibleIndex<i)end-=tabWidth(firstVisibleIndex++);
            firstVisibleIndex=Math.max(0,Math.min(firstVisibleIndex,upperBound));return;
        }
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        int x=-prefix(firstVisibleIndex),height=b-t;
        for(int i=0;i<tabCount();i++){View tab=getChildAt(i);int width=tab.getMeasuredWidth();tab.layout(x,height-tab.getMeasuredHeight(),x+width,height);x+=width;}
        overflow.layout(viewportWidth,height-overflowSize,viewportWidth+2*overflowSize,height);
        // Native half rectangles overlap by two pixels; the central dead pixel is
        // deliberately removed for capacitive touch and keyboard accessibility.
        left.layout(0,0,overflowSize+1,overflowSize);
        right.layout(overflowSize-1,0,2*overflowSize,overflowSize);
    }
    @Override protected void dispatchDraw(Canvas canvas){
        int save=canvas.save();canvas.clipRect(0,0,viewportWidth,getHeight());
        for(int i=0;i<tabCount();i++)drawChild(canvas,getChildAt(i),getDrawingTime());
        canvas.restoreToCount(save);if(overflow.getVisibility()==VISIBLE)drawChild(canvas,overflow,getDrawingTime());
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        if(!isEnabled())return false;
        // Hidden tab portions cannot accept a tap above the shorter arrow control.
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN&&overflow.getVisibility()==VISIBLE&&event.getX()>=viewportWidth&&event.getY()<overflow.getTop())return true;
        return super.dispatchTouchEvent(event);
    }
    private void updateArrows(){boolean enabled=isEnabled()&&upperBound>0;left.setEnabled(enabled);right.setEnabled(enabled);}
    @Override public void setEnabled(boolean enabled){
        boolean cancel=isEnabled()&&!enabled;super.setEnabled(enabled);
        if(cancel){long now=SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(now,now,MotionEvent.ACTION_CANCEL,0,0,0);try{super.dispatchTouchEvent(event);}finally{event.recycle();}}
        if(left!=null&&right!=null)updateArrows();
    }
    private void scrollTabs(int delta){
        if(!isEnabled()||overflow.getVisibility()!=VISIBLE)return;
        int next=Math.max(0,Math.min(upperBound,firstVisibleIndex+delta));
        if(next==firstVisibleIndex)return;firstVisibleIndex=next;updateArrows();requestLayout();invalidate();
    }
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        color=tabColor;setBackgroundColor(color);requestLayout();left.invalidate();right.invalidate();
    }
    private final class Arrow extends TextView {
        private final int direction;
        private final Paint paint=new Paint();
        private boolean held;
        private int pointer=-1;
        private long downTime;
        private final Runnable repeat=new Runnable(){public void run(){
            if(!held||!isEnabled()||!isShown())return;
            long elapsed=(SystemClock.uptimeMillis()-downTime)>>10;
            scrollTabs(direction*(elapsed>=5?20:elapsed>=2?5:1));
            if(held)postDelayed(this,100);
        }};
        Arrow(Context context,int direction){
            super(context);this.direction=direction;WmControls.button(this);setBackgroundColor(Color.TRANSPARENT);
            setFocusable(true);setFocusableInTouchMode(false);
            WmControls.bindDescription(this,"ko",direction<0?"이전 탭":"다음 탭");
            setOnClickListener(v->scrollTabs(direction));
        }
        private void stop(){held=false;pointer=-1;removeCallbacks(repeat);setPressed(false);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}
        @Override public void setEnabled(boolean enabled){super.setEnabled(enabled);if(!enabled)stop();invalidate();}
        @Override public boolean onTouchEvent(MotionEvent event){
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){
                if(!isEnabled())return false;
                stop();held=true;pointer=event.getPointerId(0);downTime=SystemClock.uptimeMillis();setPressed(true);
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
                performClick();if(held)postDelayed(repeat,530);return true;
            }
            if(action==MotionEvent.ACTION_MOVE){
                int index=event.findPointerIndex(pointer);
                // Capacitive leave-cancel differs from native captured stylus repeats.
                if(index<0||event.getX(index)<0||event.getX(index)>=getWidth()||event.getY(index)<0||event.getY(index)>=getHeight())stop();
                return true;
            }
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){stop();return true;}
            if(action==MotionEvent.ACTION_POINTER_UP&&event.getPointerId(event.getActionIndex())==pointer){stop();return true;}
            return held;
        }
        @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);if(visibility!=VISIBLE)stop();}
        @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility!=VISIBLE)stop();}
        @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
        @Override protected void onDraw(Canvas canvas){
            int width=getWidth(),height=getHeight();paint.setColor(Color.BLACK);canvas.drawRect(0,0,width,height,paint);
            // Native initial DC brush is unproved; use the shared themed tab face.
            paint.setColor(isPressed()?Color.BLACK:color);canvas.drawRect(WmMetrics.BORDER,WmMetrics.BORDER,width-WmMetrics.BORDER,height-WmMetrics.BORDER,paint);
            int m=Math.min(width,height);if(m<6)return;int k=m>12?6+(m-13)/4:4;
            int x=(width-k)/2,y=(height-1)/2;paint.setColor(!isEnabled()?WmControls.SHADOW:isPressed()?Color.WHITE:Color.BLACK);
            for(int i=0;i<k;i++){int column=direction<0?x+i:x+k-1-i;canvas.drawRect(column,y-i,column+1,y+i+1,paint);}
        }
    }
}
