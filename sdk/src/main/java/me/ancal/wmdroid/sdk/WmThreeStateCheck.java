package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.CheckBox;

/** Stock BS_3STATE/BS_AUTO3STATE:0→1→2→0, original outlined mixed tick.
 * Setters do not notify the state listener; automatic user toggles do.
 * The inherited boolean checked value is true only for CHECKED.
 */
public final class WmThreeStateCheck extends CheckBox implements WmControls.LanguageListener {
    public static final int UNCHECKED=0,CHECKED=1,INDETERMINATE=2;
    public interface OnCheckStateListener {void changed(int state);}
    private int checkState;
    private boolean automatic=true,broadcasting;
    private String language="ko";
    private OnCheckStateListener listener;
    public WmThreeStateCheck(Context context,Typeface font){super(context);WmControls.check(this,font);}
    public int getCheckState(){return checkState;}
    /** Like BM_SETCHECK, positive values above2 clamp to2; negative inputs are rejected. */
    public void setCheckState(int state){
        if(state<0)throw new IllegalArgumentException("Check state must be nonnegative");
        int next=Math.min(INDETERMINATE,state);boolean changed=next!=checkState;
        checkState=next;super.setChecked(checkState==CHECKED);if(changed)invalidate();
    }
    @Override public void setChecked(boolean checked){setCheckState(checked?CHECKED:UNCHECKED);}
    public boolean isAutomatic(){return automatic;}
    public void setAutomatic(boolean enabled){if(automatic!=enabled){automatic=enabled;invalidate();}}
    public void setOnCheckStateListener(OnCheckStateListener listener){this.listener=listener;}
    @Override public void toggle(){
        if(!automatic||!isEnabled())return;
        int before=checkState;setCheckState((checkState+1)%3);
        if(before!=checkState&&listener!=null&&!broadcasting){broadcasting=true;try{listener.changed(checkState);}finally{broadcasting=false;}}
    }
    @Override public void onWmLanguageChanged(String language){this.language=WmStrings.language(getContext(),language);}
    private String mixedLabel(){return WmStrings.text(getContext(),language,"부분 선택됨");}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){
        super.onInitializeAccessibilityNodeInfo(info);info.setClassName(CheckBox.class.getName());
        if(checkState==INDETERMINATE){
            if(Build.VERSION.SDK_INT>=30)info.setStateDescription(mixedLabel());
            else{CharSequence label=getContentDescription()!=null?getContentDescription():getText();info.setContentDescription(label+", "+mixedLabel());}
        }
    }
    @Override public void onInitializeAccessibilityEvent(AccessibilityEvent event){
        super.onInitializeAccessibilityEvent(event);if(checkState==INDETERMINATE)event.getText().add(mixedLabel());
    }
    @Override public Parcelable onSaveInstanceState(){SavedState state=new SavedState(super.onSaveInstanceState());state.check=checkState;state.automatic=automatic;return state;}
    @Override public void onRestoreInstanceState(Parcelable value){
        if(!(value instanceof SavedState)){super.onRestoreInstanceState(value);return;}
        SavedState state=(SavedState)value;super.onRestoreInstanceState(state.getSuperState());setAutomatic(state.automatic);setCheckState(state.check);
    }
    public static final class SavedState extends BaseSavedState {
        int check;boolean automatic;
        SavedState(Parcelable parent){super(parent);}
        SavedState(Parcel parcel){super(parcel);check=parcel.readInt();automatic=parcel.readInt()!=0;}
        @Override public void writeToParcel(Parcel parcel,int flags){super.writeToParcel(parcel,flags);parcel.writeInt(check);parcel.writeInt(automatic?1:0);}
        public static final Parcelable.Creator<SavedState> CREATOR=new Parcelable.Creator<SavedState>(){
            public SavedState createFromParcel(Parcel parcel){return new SavedState(parcel);}
            public SavedState[] newArray(int size){return new SavedState[size];}
        };
    }
}
