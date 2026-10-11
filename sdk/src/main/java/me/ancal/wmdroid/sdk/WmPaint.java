package me.ancal.wmdroid.sdk;

import android.content.Context;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/** Synthetic presentation costs; these are not measurements of original hardware. */
public class WmPaint extends FrameLayout {
    public enum Role {
        MENU(0.5),POPUP(1.0),FORM(1.25),GRID(1.5),CALENDAR(1.75);
        final double cost;
        Role(double cost){this.cost=cost;}
    }
    private final boolean delayAlreadyServed;
    private final Role role;
    private View replayClient;
    private final Rect clientBounds=new Rect();
    private boolean active,blank,repaint,scan,blockedGesture,forwardedGesture,paintStarted,attached,layoutReady;
    private int bandLines,scale,phase,contentTop,contentHeight,visibleLines;
    private double stepMillis;
    private long readyAt,paintStartedAt;
    private final Runnable advance=new Runnable(){public void run(){
        if(!active)return;
        if(!attached||getWindowToken()==null||!isShown()||getWindowVisibility()!=VISIBLE){cancel();return;}
        if(replayClient!=null&&!validClient(replayClient)){cancel();return;}
        if(phase==0){
            // Initial bounds and a tab's replacement children must finish layout first.
            if(!layoutReady||isLayoutRequested())return;
            long now=SystemClock.uptimeMillis();
            if(now<readyAt){postDelayed(this,readyAt-now);return;}
            if(!paintStarted){
                measureContent();paintStartedAt=now;paintStarted=true;
            }
            long elapsed=Math.max(0,now-paintStartedAt);
            long completed=(long)Math.floor(elapsed/stepMillis);
            visibleLines=scan?(int)Math.min(contentHeight,completed*bandLines):contentHeight;
            if(visibleLines<contentHeight){
                long next=Math.max(1,(long)Math.ceil((completed+1)*stepMillis-elapsed));
                postDelayed(this,next);
            }else if(repaint){phase=1;postDelayed(this,scaled(60,scale));}
            else{finish();return;}
        }else if(phase==1){
            blank=true;phase=2;postDelayed(this,scaled(40,scale));
        }else{finish();return;}
        invalidate();
    }};

    public WmPaint(Context context,boolean delayAlreadyServed){this(context,delayAlreadyServed,Role.FORM);}
    public WmPaint(Context context,boolean delayAlreadyServed,Role role){
        super(context);this.delayAlreadyServed=delayAlreadyServed;this.role=role;
    }
    public static final class Options {
        public boolean enabled=true,scan=true,repaint=false;public int delay=120,lines=64,scale=1000;
        public static Options from(WmShell.State state){Options o=new Options();o.enabled=state.retro;o.scan=state.scan;o.repaint=state.repaint;o.delay=state.delay;o.lines=state.lines;o.scale=state.scale;return o;}
    }
    protected Options readOptions(){return Options.from(WmShell.state(getContext()));}
    private static int scaled(int millis,int scale){return Math.round(millis*scale/1000f);}
    public boolean isActive(){return active;}
    /** Reveal active surfaces after disabling replay. Keep a swallowed gesture
     * blocked until its UP/CANCEL, so disabling cannot click newly revealed content.
     * Hosts call this on their existing settings/state notification path. */
    public static void stopReplay(View root){
        if(root instanceof WmPaint&&((WmPaint)root).active)((WmPaint)root).finish();
        if(root instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)root;for(int i=0;i<group.getChildCount();i++)stopReplay(group.getChildAt(i));}
    }


    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;layoutReady=false;start(delayAlreadyServed);}
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
        super.onLayout(changed,left,top,right,bottom);
        layoutReady=true;
        if(active&&phase==0){removeCallbacks(advance);post(advance);}
    }
    public void replay(){replay(false);}
    public void replay(boolean delayServed){if(attached&&getWindowToken()!=null)start(delayServed);}
    /** Reveals only a visible descendant client; surrounding tabs/chrome remain drawn.
     * A hidden/detached client cancels presentation. Foreign views are rejected. */
    public void replay(View client){replay(client,false);}
    public void replay(View client,boolean delayServed){
        if(client==null||!isDescendant(client))throw new IllegalArgumentException("Visible descendant client required");
        if(!attached||getWindowToken()==null||!validClient(client)){cancel();return;}
        start(delayServed,client);
    }
    private boolean isDescendant(View client){
        for(android.view.ViewParent parent=client.getParent();parent!=null;parent=parent.getParent())if(parent==this)return true;
        return false;
    }
    private boolean validClient(View client){return isDescendant(client)&&client.isShown()&&client.getWindowToken()!=null;}
    private void clientBounds(){
        clientBounds.set(0,0,replayClient.getWidth(),replayClient.getHeight());
        offsetDescendantRectToMyCoords(replayClient,clientBounds);
        if(!clientBounds.intersect(0,0,getWidth(),getHeight()))clientBounds.setEmpty();
    }
    private void start(boolean delayServed){start(delayServed,null);}
    private void start(boolean delayServed,View client){
        // A gesture swallowed before replay must not release into new content.
        boolean wasBlocked=blockedGesture;
        cancel();blockedGesture=wasBlocked;
        Options preferences=readOptions();
        if(!preferences.enabled||!isShown()||getWindowVisibility()!=VISIBLE)return;
        replayClient=client;
        scale=Math.max(250,Math.min(4000,preferences.scale));
        int delay=delayServed?0:scaled(Math.max(0,Math.min(1500,preferences.delay)),scale);
        // ponytail: synthetic menu calibration, replace ratios with physical-device timing when measured.
        if(role==Role.MENU)delay=Math.round(delay/4f);
        scan=preferences.scan;
        repaint=preferences.repaint;
        if(delay==0&&!scan&&!repaint){replayClient=null;return;}
        // A replay can begin while a child is pressed. End that child's stream
        // and swallow its remaining events instead of leaving it pressed.
        if(forwardedGesture){cancelForwardedGesture();blockedGesture=true;}
        bandLines=Math.max(8,Math.min(256,preferences.lines));
        stepMillis=16d*role.cost*scale/1000d;
        readyAt=SystemClock.uptimeMillis()+delay;
        contentTop=contentHeight=visibleLines=0;phase=0;blank=false;paintStarted=false;active=true;
        // A retained applet can reattach with unchanged bounds. Its old layout
        // is no longer valid, but an unchanged parent need not lay it out again.
        // Ensure onLayout resumes advance instead of leaving a zero-height clip.
        layoutReady=false;requestLayout();
        invalidate();post(advance);
    }
    private void measureContent(){
        if(replayClient!=null){clientBounds();contentTop=clientBounds.top;contentHeight=clientBounds.height();return;}
        int first=getHeight(),last=0;
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);
            if(child.getVisibility()!=GONE){first=Math.min(first,child.getTop());last=Math.max(last,child.getBottom());}
        }
        contentTop=Math.max(0,Math.min(getHeight(),first));
        contentHeight=Math.max(0,Math.min(getHeight(),last)-contentTop);
    }
    private void finish(){removeCallbacks(advance);active=false;blank=false;replayClient=null;invalidate();}
    public void cancel(){
        removeCallbacks(advance);active=false;blank=false;blockedGesture=false;replayClient=null;
        invalidate();
    }
    @Override protected void onDetachedFromWindow(){attached=false;layoutReady=false;cancelForwardedGesture();cancel();super.onDetachedFromWindow();}
    @Override protected void onVisibilityChanged(View changedView,int visibility){
        super.onVisibilityChanged(changedView,visibility);if(visibility!=VISIBLE)cancel();
    }
    @Override protected void onWindowVisibilityChanged(int visibility){
        super.onWindowVisibilityChanged(visibility);if(visibility!=VISIBLE)cancel();
    }
    @Override protected void dispatchDraw(Canvas canvas){
        if(!active){super.dispatchDraw(canvas);return;}
        if(replayClient!=null){
            if(!validClient(replayClient)){cancel();super.dispatchDraw(canvas);return;}
            clientBounds();
            int outside=canvas.save();
            try{canvas.clipRect(clientBounds,Region.Op.DIFFERENCE);super.dispatchDraw(canvas);}
            finally{canvas.restoreToCount(outside);}
            int inside=canvas.save();
            try{
                int bottom=blank?clientBounds.top:Math.min(clientBounds.bottom,clientBounds.top+visibleLines);
                canvas.clipRect(clientBounds.left,clientBounds.top,clientBounds.right,bottom);
                super.dispatchDraw(canvas);
            }finally{canvas.restoreToCount(inside);}
            return;
        }
        int save=canvas.save();
        try{
            // The surface background remains visible while only its children reveal.
            int bottom=blank?0:contentTop+visibleLines;
            canvas.clipRect(0,0,getWidth(),bottom);super.dispatchDraw(canvas);
        }finally{canvas.restoreToCount(save);}
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN)blockedGesture=active;
        if(active||blockedGesture){
            if(forwardedGesture){cancelForwardedGesture();blockedGesture=true;}
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)blockedGesture=false;
            return true;
        }
        // Record before dispatch: a child can synchronously request replay.
        if(action==MotionEvent.ACTION_DOWN)forwardedGesture=true;
        boolean handled=super.dispatchTouchEvent(event);
        if(action==MotionEvent.ACTION_DOWN){
            forwardedGesture=handled;
            // A child handling DOWN is registered as a touch target only after
            // returning. Cancel again here if it requested replay during DOWN.
            if(active&&forwardedGesture){cancelForwardedGesture();blockedGesture=true;}
        }else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)forwardedGesture=false;
        return handled;
    }
    private void cancelForwardedGesture(){
        if(!forwardedGesture)return;forwardedGesture=false;
        long now=SystemClock.uptimeMillis();MotionEvent cancel=MotionEvent.obtain(now,now,MotionEvent.ACTION_CANCEL,0,0,0);
        try{super.dispatchTouchEvent(cancel);}finally{cancel.recycle();}
    }
}
