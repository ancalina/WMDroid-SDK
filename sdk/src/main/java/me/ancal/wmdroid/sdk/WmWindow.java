package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.view.View;
import android.view.ViewParent;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/** One window contract for hosted applets and independent WM-style Activities.
 * No inset between the 52px title, client area and 52px command bar.
 * The host supplies its title/tray and commands; this class owns their geometry.
 */
public final class WmWindow extends LinearLayout {
    public final FrameLayout client;
    private View title,commands;
    private FrameLayout popups;
    private boolean blockedOwnerTouch;
    public WmWindow(Context context){super(context);setOrientation(VERTICAL);setBackgroundColor(0xffffffff);setClickable(true);
        client=new FrameLayout(context);addView(client,new LayoutParams(LayoutParams.MATCH_PARENT,0,1));}
    private void validateReplacement(View view,View current){
        if(view==null||view==this)throw new IllegalArgumentException("Window slot view required");
        for(ViewParent parent=getParent();parent!=null;parent=parent.getParent())if(parent==view)throw new IllegalArgumentException("Window slot view is an ancestor");
        if(view.getParent()!=null&&(view!=current||view.getParent()!=this))throw new IllegalStateException("Window slot view already has a parent");
    }
    public void title(View view){validateReplacement(view,title);if(title!=null)removeView(title);title=view;addView(view,0,new LayoutParams(LayoutParams.MATCH_PARENT,WmMetrics.BAR));}
    public void commands(View view){validateReplacement(view,commands);if(view instanceof WmCommandBar)((WmCommandBar)view).inputMethods(client);if(commands!=null)removeView(commands);commands=view;addView(view,popups==null?getChildCount():indexOfChild(popups),new LayoutParams(LayoutParams.MATCH_PARENT,WmMetrics.BAR));}
    /** Modal controls share logical screen coordinates, including title and commands. */
    public FrameLayout popupLayer(){if(popups==null){popups=new FrameLayout(getContext());addView(popups,new LayoutParams(LayoutParams.MATCH_PARENT,0));}return popups;}
    @Override protected void onMeasure(int width,int height){super.onMeasure(width,height);if(popups!=null)popups.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(getMeasuredHeight(),MeasureSpec.EXACTLY));}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){super.onLayout(changed,l,t,r,b);if(popups!=null)popups.layout(0,0,r-l,b-t);}
    public boolean ownerCommandsAllowed(){return !WmControls.blocksOwnerCommands(client);}
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN)blockedOwnerTouch=!ownerCommandsAllowed()&&
            commands!=null&&event.getX()>=commands.getLeft()&&event.getX()<commands.getRight()&&event.getY()>=commands.getTop()&&event.getY()<commands.getBottom()&&
            !(commands instanceof WmCommandBar&&((WmCommandBar)commands).inputMethodTouch(this,event.getX(),event.getY()));
        if(blockedOwnerTouch){if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)blockedOwnerTouch=false;return true;}
        return super.dispatchTouchEvent(event);
    }
    @Override protected void onDetachedFromWindow(){blockedOwnerTouch=false;super.onDetachedFromWindow();}

}
