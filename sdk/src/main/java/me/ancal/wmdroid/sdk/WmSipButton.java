package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

/** ROM icon505 and completed-UP/arrow-DOWN actions, adapted to Android IMEs.
 * The original shell can show SIP without an editor. Android requires a served
 * editor: this control is visible only while its owner contains an eligible one.
 * The picker is Android-owned; ordinary apps cannot switch IMEs silently.
 */
public final class WmSipButton extends LinearLayout {
    private final View owner;
    private final ImageView icon;
    private final View arrow;
    private final WmCommandBar.KeyTouch iconTouch=new WmCommandBar.KeyTouch(false),arrowTouch=new WmCommandBar.KeyTouch(true);
    private View editor;
    private View activeOwner;
    private boolean up;
    private ViewTreeObserver observed;
    private String artworkRevision;
    private final ViewTreeObserver.OnGlobalLayoutListener layout=this::refresh;
    public WmSipButton(Context context,View owner,WmBar bar){
        super(context);if(owner==null)throw new IllegalArgumentException("Input owner required");this.owner=owner;
        setGravity(Gravity.CENTER);setOrientation(HORIZONTAL);setVisibility(GONE);
        icon=new ImageView(context);icon.setScaleType(ImageView.ScaleType.CENTER);icon.setFocusable(false);
        icon.setImageDrawable(WmIcons.load(getContext(),"sip_keyboard"));
        artworkRevision=WmResources.revision(context);
        WmControls.bindDescription(icon,"ko","입력 패널");icon.setSoundEffectsEnabled(false);icon.setOnClickListener(v->toggle());
        icon.setOnTouchListener(iconTouch);
        addView(icon,new LayoutParams(52,WmMetrics.BAR));
        arrow=new View(context){private final Paint ink=new Paint();private final Path triangle=new Path();
            @Override protected void onDraw(Canvas canvas){ink.setColor(0xffffffff);triangle.reset();float x=getWidth()/2f,y=getHeight()/2f;triangle.moveTo(x-4,y-2);triangle.lineTo(x+4,y-2);triangle.lineTo(x,y+2);triangle.close();canvas.drawPath(triangle,ink);}
        };
        arrow.setFocusable(false);arrow.setSoundEffectsEnabled(false);WmControls.bindDescription(arrow,"ko","입력 방법 선택");
        arrow.setOnClickListener(v->{refresh();if(editor!=null&&up)manager().showInputMethodPicker();});
        arrow.setOnTouchListener(arrowTouch);
        addView(arrow,new LayoutParams(20,WmMetrics.BAR));restyle(bar);
    }
    public void restyle(WmBar bar){String revision=WmResources.revision(getContext());if(!revision.equals(artworkRevision)){artworkRevision=revision;icon.setImageDrawable(WmIcons.load(getContext(),"sip_keyboard"));}icon.setBackgroundDrawable(bar.softButton(icon));arrow.setBackgroundDrawable(bar.softButton(arrow));}
    public boolean isInputMethodVisible(){return up;}
    private InputMethodManager manager(){return (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);}
    private boolean eligible(View view){
        if(view==null||!view.isShown()||!view.isEnabled()||!view.onCheckIsTextEditor()||view.getWindowToken()==null)return false;
        boolean active=false;for(View current=view;current!=null;){if(current==activeOwner)active=true;if(current==owner)return active;android.view.ViewParent parent=current.getParent();current=parent instanceof View?(View)parent:null;}
        return false;
    }
    private View firstEditor(View view){
        if(eligible(view))return view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=group.getChildCount()-1;i>=0;i--){View found=firstEditor(group.getChildAt(i));if(found!=null)return found;}}
        return null;
    }
    public void refresh(){
        activeOwner=WmControls.inputMethodOwner(owner);
        View focus=owner.findFocus();if(eligible(focus))editor=focus;else if(!eligible(editor))editor=activeOwner==null?null:firstEditor(activeOwner);
        boolean visible=false;
        if(Build.VERSION.SDK_INT>=30)visible=Api30.visible(this);
        else{Rect frame=new Rect();getWindowVisibleDisplayFrame(frame);visible=getRootView().getHeight()-frame.bottom>100*getResources().getDisplayMetrics().density;}
        up=visible;int next=editor!=null?VISIBLE:GONE;if(getVisibility()!=next)setVisibility(next);
        next=up?VISIBLE:GONE;if(arrow.getVisibility()!=next)arrow.setVisibility(next);
    }
    private void toggle(){
        refresh();if(editor==null)return;
        if(up)manager().hideSoftInputFromWindow(editor.getWindowToken(),0);
        else if(editor.requestFocus())manager().showSoftInput(editor,InputMethodManager.SHOW_IMPLICIT);
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();observed=getViewTreeObserver();observed.addOnGlobalLayoutListener(layout);refresh();}
    @Override protected void onDetachedFromWindow(){if(observed!=null&&observed.isAlive())observed.removeGlobalOnLayoutListener(layout);observed=null;editor=null;up=false;iconTouch.cancel(icon);arrowTouch.cancel(arrow);super.onDetachedFromWindow();}
    private static final class Api30 {static boolean visible(View view){WindowInsets insets=view.getRootWindowInsets();return insets!=null&&insets.isVisible(WindowInsets.Type.ime());}}
}
