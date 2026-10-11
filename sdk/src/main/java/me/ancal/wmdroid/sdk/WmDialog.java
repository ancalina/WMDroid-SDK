package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Explicit embedded dialog owner. Native8EAF0 promotes the focused push button
 * and restores the stored default when focus returns to a non-button control.
 * Editors keep their own input; this is not a full IsDialogMessage replacement.
 */
public class WmDialog extends LinearLayout {
    private TextView storedDefault,currentDefault,cancelCommand;
    private ViewTreeObserver observedTree;
    private final ViewTreeObserver.OnGlobalFocusChangeListener focusChanged=(oldFocus,newFocus)->refreshDialogDefault();
    public WmDialog(Context context){super(context);}
    /** null clears the stored fallback; a focused button can still be promoted. */
    public final void setDefaultButton(TextView button){validateCommand(button);storedDefault=button;refreshDialogDefault();}
    public final void setCancelButton(TextView button){validateCommand(button);cancelCommand=button;}
    private void validateCommand(TextView button){
        if(button!=null&&(!WmControls.isPushButton(button)||!WmControls.dialogOwns(this,button)))
            throw new IllegalArgumentException("Registered button inside this dialog required");
    }
    protected final void refreshDialogDefault(){
        View focus=findFocus();TextView next=storedDefault;
        if(WmControls.dialogOwns(this,focus)&&WmControls.isPushButton(focus)&&WmControls.dialogButtonEnabled(this,focus))next=(TextView)focus;
        if(next!=null&&!WmControls.dialogOwns(this,next))next=null;
        if(currentDefault==next)return;
        if(currentDefault!=null)WmControls.setDefaultButton(currentDefault,false);
        currentDefault=next;
        if(currentDefault!=null)WmControls.setDefaultButton(currentDefault,true);
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        refreshDialogDefault();return WmControls.dialogButtonKey(this,event,cancelCommand,storedDefault)||super.dispatchKeyEvent(event);
    }
    @Override protected void onAttachedToWindow(){
        super.onAttachedToWindow();observedTree=getViewTreeObserver();observedTree.addOnGlobalFocusChangeListener(focusChanged);refreshDialogDefault();
    }
    @Override protected void onDetachedFromWindow(){
        if(observedTree!=null&&observedTree.isAlive())observedTree.removeOnGlobalFocusChangeListener(focusChanged);
        observedTree=null;
        if(currentDefault!=null)WmControls.setDefaultButton(currentDefault,false);
        currentDefault=null;super.onDetachedFromWindow();
    }
}
