package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.view.*;
import android.widget.FrameLayout;

/** Opt-in relative trackpad on a logical VGA surface. No accessibility service,
 * global input injection or system permissions. It controls this surface only.
 */
public final class WmPointer extends View {
    private final FrameLayout target;private final Runnable disable;private final Handler handler=new Handler();
    private final Paint paint=new Paint();private float x=240,y=320,lastX,lastY,startX,startY;private boolean moved,held,multiTouchCanceled;
    private final Runnable hold=new Runnable(){public void run(){held=true;disable.run();}};
    public WmPointer(Context context,FrameLayout target,Runnable disable){super(context);this.target=target;this.disable=disable;WmControls.bindDescription(this,"en","핑거마우스: 탭하여 클릭, 길게 눌러 끄기");setSoundEffectsEnabled(false);setClickable(true);}
    public float pointerX(){return x;}public float pointerY(){return y;}
    @Override protected void onDraw(Canvas canvas){
        Path arrow=new Path();arrow.moveTo(x,y);arrow.lineTo(x,y+30);arrow.lineTo(x+7,y+23);arrow.lineTo(x+13,y+35);arrow.lineTo(x+19,y+32);arrow.lineTo(x+13,y+21);arrow.lineTo(x+24,y+21);arrow.close();
        paint.setAntiAlias(false);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);canvas.drawPath(arrow,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(Color.BLACK);canvas.drawPath(arrow,paint);
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){handler.removeCallbacks(hold);multiTouchCanceled=false;}
        if(event.getPointerCount()>1||action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_POINTER_UP){multiTouchCanceled=true;handler.removeCallbacks(hold);}
        // Index zero can change when a finger lifts. Consume the complete
        // multi-touch gesture rather than jumping the cursor or dispatching tap.
        if(multiTouchCanceled){if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)multiTouchCanceled=false;return true;}
        switch(action){
        case MotionEvent.ACTION_DOWN:held=false;moved=false;startX=lastX=event.getX();startY=lastY=event.getY();handler.postDelayed(hold,ViewConfiguration.getLongPressTimeout());return true;
        case MotionEvent.ACTION_MOVE:
            float dx=event.getX()-lastX,dy=event.getY()-lastY;lastX=event.getX();lastY=event.getY();
            if(Math.abs(lastX-startX)+Math.abs(lastY-startY)>8){moved=true;handler.removeCallbacks(hold);}
            x=Math.max(1,Math.min(getWidth()-2,x+dx));y=Math.max(1,Math.min(getHeight()-2,y+dy));invalidate();return true;
        case MotionEvent.ACTION_UP:handler.removeCallbacks(hold);if(!held&&!moved)performClick();return true;
        case MotionEvent.ACTION_CANCEL:handler.removeCallbacks(hold);return true;
    }return true;}
    @Override public boolean performClick(){super.performClick();clickAt(x,y);return true;}
    private void clickAt(float x,float y){
        setVisibility(INVISIBLE);long now=android.os.SystemClock.uptimeMillis();
        MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0),up=MotionEvent.obtain(now,now+20,MotionEvent.ACTION_UP,x,y,0);
        try{target.dispatchTouchEvent(down);target.dispatchTouchEvent(up);}finally{down.recycle();up.recycle();if(getParent()!=null){setVisibility(VISIBLE);bringToFront();}}
    }
    @Override protected void onDetachedFromWindow(){handler.removeCallbacks(hold);multiTouchCanceled=false;super.onDetachedFromWindow();}
}
