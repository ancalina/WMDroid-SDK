package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native VGA commands [0,180), [300,480); the middle120px belongs to SIP.
 * Ordinary commands activate on completed UP, menu commands on DOWN.
 * The reserved centre has no command action; inputMethods binds an editor owner.
 */
public final class WmCommandBar extends LinearLayout implements WmControls.RestyleListener {
    public static final int KEY_WIDTH=180, CENTRE_WIDTH=120;
    private final TextView left,right;
    public final LinearLayout centre;
    private final KeyTouch[] touches=new KeyTouch[2];
    private WmBar paint;
    private WmSipButton sip;
    public WmCommandBar(Context context,Typeface font,int accent){this(context,font,new WmBar(context,true,accent));}
    public WmCommandBar(Context context,Typeface font,WmBar paint){
        super(context);setOrientation(HORIZONTAL);
        left=new CommandKey(context);right=new CommandKey(context);WmControls.textStyle(left,font);WmControls.textStyle(right,font);
        centre=new LinearLayout(context);centre.setGravity(Gravity.CENTER);
        addView(left,new LayoutParams(KEY_WIDTH,WmMetrics.BAR));addView(centre,new LayoutParams(0,WmMetrics.BAR,1));addView(right,new LayoutParams(KEY_WIDTH,WmMetrics.BAR));
        setCommand(false,"",null);setCommand(true,"",null);restyle(font,paint);
    }
    public TextView key(boolean right){return right?this.right:left;}
    /** null hides SIP. A custom centre must be cleared before enabling SIP. */
    public void inputMethods(View owner){
        if(owner!=null)for(int i=0;i<centre.getChildCount();i++)if(centre.getChildAt(i)!=sip)throw new IllegalStateException("Command bar centre already owned");
        if(sip!=null){centre.removeView(sip);sip=null;}
        if(owner==null)return;
        sip=new WmSipButton(getContext(),owner,paint);centre.addView(sip,new LayoutParams(LayoutParams.WRAP_CONTENT,WmMetrics.BAR));
    }
    boolean inputMethodTouch(android.view.ViewGroup window,float x,float y){
        if(sip==null)return false;sip.refresh();if(!sip.isShown())return false;
        android.graphics.Rect bounds=new android.graphics.Rect(0,0,sip.getWidth(),sip.getHeight());window.offsetDescendantRectToMyCoords(sip,bounds);return bounds.contains((int)x,(int)y);
    }
    public void setCommands(String left,Runnable leftAction,String right,Runnable rightAction){setCommand(false,left,leftAction);setCommand(true,right,rightAction);}
    public void setCommand(boolean right,String label,Runnable action){bind(right,label,action,false);}
    public void setMenu(boolean right,String label,Runnable action){bind(right,label,action,true);}
    private void bind(boolean right,String label,Runnable action,boolean menu){
        TextView key=key(right);key.setText(label==null?"":label);key.setEnabled(action!=null);key.setPressed(false);key.setSelected(false);
        key.setOnClickListener(v->{if(action!=null)action.run();});
        KeyTouch touch=new KeyTouch(menu);touches[right?1:0]=touch;key.setOnTouchListener(touch);
    }
    private static final class CommandKey extends TextView {
        CommandKey(Context context){super(context);}
        @Override public boolean performClick(){return isEnabled()&&WmControls.ownerCommandsAllowed(this)&&super.performClick();}
    }
    static final class KeyTouch implements View.OnTouchListener {
            private boolean armed;private final boolean menu;
            KeyTouch(boolean menu){this.menu=menu;}
            void cancel(View view){armed=false;view.setPressed(false);}
            public boolean onTouch(View view,MotionEvent event){
                if(!view.isEnabled()){armed=false;view.setPressed(false);return true;}
                if(event.getPointerCount()!=1){armed=false;view.setPressed(false);return true;}
                switch(event.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:armed=true;view.setPressed(true);if(menu)view.performClick();return true;
                    case MotionEvent.ACTION_MOVE:
                        if(event.getX()<0||event.getX()>=view.getWidth()||event.getY()<0||event.getY()>=view.getHeight()){armed=false;view.setPressed(false);}return true;
                    case MotionEvent.ACTION_UP:
                        boolean invoke=armed&&!menu&&event.getX()>=0&&event.getX()<view.getWidth()&&event.getY()>=0&&event.getY()<view.getHeight();armed=false;view.setPressed(false);if(invoke)view.performClick();return true;
                    case MotionEvent.ACTION_CANCEL:armed=false;view.setPressed(false);return true;
                    default:return true;
                }
            }
    }
    public void restyle(Typeface font,WmBar paint){
        this.paint=paint;setBackgroundDrawable(paint);
        if(sip!=null)sip.restyle(paint);
        for(boolean right:new boolean[]{false,true}){TextView key=key(right);key.setTypeface(font);WmFonts.attach(key,font);styleKey(key,paint);}
    }
    public void styleKey(TextView key){styleKey(key,paint);}
    public static void styleKey(TextView key,WmBar paint){
        key.setGravity(Gravity.CENTER);key.setPadding(8,0,8,0);key.setSoundEffectsEnabled(false);key.setBackgroundDrawable(paint.softButton(key));
        key.setTextColor(new ColorStateList(new int[][]{{-android.R.attr.state_enabled},{android.R.attr.state_pressed},{android.R.attr.state_selected},{}},new int[]{0xff808080,paint.color(15),paint.color(15),paint.color(14)}));
    }
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){restyle(regular,new WmBar(getContext(),true,selection));}
    @Override public boolean shouldDelayChildPressedState(){return false;}
    @Override protected void onDetachedFromWindow(){for(KeyTouch touch:touches)if(touch!=null)touch.armed=false;for(TextView key:new TextView[]{left,right}){key.setPressed(false);key.setSelected(false);}super.onDetachedFromWindow();}
}
