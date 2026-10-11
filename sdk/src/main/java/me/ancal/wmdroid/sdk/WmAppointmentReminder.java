package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.widget.LinearLayout;
import android.view.View;
import java.util.Locale;

/** Native appointment25059 relative reminder composite. Owner supplies popups.
 * Enabled line remains; disabled hides the numeric/unit line. Closed height
 * follows shared combo font metrics; width split remains provisional VGA geometry. No task absolute date/time reminder. */
public final class WmAppointmentReminder extends LinearLayout implements WmControls.LanguageListener {
    private final WmChoice enabled,unit;
    private final WmEditableChoice quantity;
    private final LinearLayout amount;
    private int lastQuantity;
    private boolean editing=true,movingPopupFocus;
    /** Initializes enabled/quantity/unit from initial; selection is an Android
     * highlight color, not a unit index. Null font/locale/initial is invalid. */
    public WmAppointmentReminder(Context context,Typeface font,Locale locale,WmReminder initial,int selection){
        super(context);if(font==null||locale==null||initial==null)throw new IllegalArgumentException("Reminder arguments required");
        setOrientation(VERTICAL);lastQuantity=initial.quantity;
        enabled=new WmChoice(context,font,new String[]{"",""},initial.enabled?1:0,selection);
        addView(enabled,new LayoutParams(-1,LayoutParams.WRAP_CONTENT));
        amount=new LinearLayout(context);
        quantity=new WmEditableChoice(context,font,WmReminder.candidates(initial.unit),String.valueOf(initial.quantity),selection);
        // Original192-DPI PIMUTIL_135: outer300-caption40-list border4.
        // Keep this initial client extent while live font changes alter row capacity.
        quantity.setClosedDropdownClientHeight(256);
        quantity.setInputType(InputType.TYPE_CLASS_NUMBER);quantity.setKeyListener(DigitsKeyListener.getInstance("0123456789"));quantity.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        unit=new WmChoice(context,font,new String[]{"","","",""},initial.unit,selection);
        amount.addView(quantity,new LayoutParams(0,LayoutParams.WRAP_CONTENT,1));amount.addView(unit,new LayoutParams(0,LayoutParams.WRAP_CONTENT,1));addView(amount,new LayoutParams(-1,LayoutParams.WRAP_CONTENT));
        quantity.setOnFocusChangeListener((view,focused)->{if(!focused&&!movingPopupFocus)commitQuantity();});
        quantity.setOnCandidateNavigationListener(index->{quantity.choose(index);commitQuantity();});
        enabled.setOnChoiceListener(index->{commitQuantity();refresh();});
        unit.setOnChoiceListener(index->{commitQuantity();quantity.setCandidates(WmReminder.candidates(index));});
        setLocale(locale);refresh();
    }
    private static boolean valid(String raw){if(raw.length()==0||raw.length()>4)return false;for(int i=0;i<raw.length();i++)if(raw.charAt(i)<'0'||raw.charAt(i)>'9')return false;return true;}
    /** Invalid/incomplete unsigned numeric commit retains the previous quantity. */
    public WmReminder value(){String raw=quantity.value();return new WmReminder(enabled.selectedIndex()==1,valid(raw)?Integer.parseInt(raw):lastQuantity,unit.selectedIndex());}
    /** Includes the committed fallback and literal numeric draft, without committing blur. */
    public Bundle saveInteractionState(){Bundle state=new Bundle();state.putInt("schema",1);state.putInt("enabled",enabled.selectedIndex());state.putInt("unit",unit.selectedIndex());state.putInt("last",lastQuantity);state.putString("draft",quantity.value());state.putStringArray("candidates",quantity.candidates());state.putBundle("quantity",quantity.saveInteractionState());return state;}
    public static void validateInteractionState(Bundle state){
        if(state==null||state.size()!=7||!(state.get("schema") instanceof Integer)||state.getInt("schema")!=1||!(state.get("enabled") instanceof Integer)||!(state.get("unit") instanceof Integer)||!(state.get("last") instanceof Integer)||!(state.get("draft") instanceof String)||!(state.get("candidates") instanceof String[])||!(state.get("quantity") instanceof Bundle))throw new IllegalArgumentException("Reminder interaction schema1 required");
        int on=state.getInt("enabled");if(on<0||on>1)throw new IllegalArgumentException("Reminder enabled index required");new WmReminder(on==1,state.getInt("last"),state.getInt("unit"));
        String raw=state.getString("draft");if(!raw.isEmpty()&&!valid(raw))throw new IllegalArgumentException("Unsigned four-digit reminder draft required");String[] labels=state.getStringArray("candidates");if(labels.length==0)throw new IllegalArgumentException("Reminder candidates required");for(String label:labels)if(label==null)throw new IllegalArgumentException("Null reminder candidate");
        WmEditableChoice.validateInteractionState(state.getBundle("quantity"),labels.length,raw.length());
    }
    /** Silent restoration; caller owns focus and closes popups first. */
    public void restoreInteractionState(Bundle state){restoreInteractionState(state,null);}
    void restoreInteractionState(Bundle state,Runnable restoreFocus){
        validateInteractionState(state);if(enabled.isDroppedDown()||unit.isDroppedDown()||quantity.isDroppedDown())throw new IllegalStateException("Close reminder popup before restoring");
        boolean previous=movingPopupFocus;movingPopupFocus=true;try{enabled.setSelectedIndex(state.getInt("enabled"));unit.setSelectedIndex(state.getInt("unit"));lastQuantity=state.getInt("last");quantity.setCandidates(state.getStringArray("candidates"));quantity.setValue(state.getString("draft"));refresh();if(restoreFocus!=null)restoreFocus.run();quantity.restoreInteractionState(state.getBundle("quantity"));}finally{movingPopupFocus=previous;}
    }
    /** Complete a numeric editor/candidate selection. Intermediate keystrokes
     * never replace the previously committed number on failed parsing. */
    public void commitQuantity(){String raw=quantity.value();if(valid(raw))lastQuantity=Integer.parseInt(raw);else quantity.setValue(String.valueOf(lastQuantity));}
    public WmChoice enabledChoice(){return enabled;}
    public WmEditableChoice quantityField(){return quantity;}
    /** Internal list focus is not leaving the native editable combo. Keep an
     * incomplete draft untouched; ordinary editor blur still commits normally. */
    void focusQuantityPopup(View popup){boolean previous=movingPopupFocus;movingPopupFocus=true;try{popup.requestFocus();}finally{movingPopupFocus=previous;}}
    public WmChoice unitChoice(){return unit;}
    public void setEditingEnabled(boolean enabled){editing=enabled;refresh();}
    private void refresh(){boolean on=enabled.selectedIndex()==1;enabled.setEnabled(editing);amount.setVisibility(on?VISIBLE:GONE);quantity.setEnabled(editing&&on);unit.setEnabled(editing&&on);}
    public void setLocale(Locale locale){
        if(locale==null)throw new IllegalArgumentException("Locale required");
        String language=WmStrings.language(getContext(),android.os.Build.VERSION.SDK_INT>=21?Modern.tag(locale):locale.toString().replace('_','-'));
        enabled.setChoices(new String[]{WmStrings.text(getContext(),language,"없음","calendar.reminder.off"),WmStrings.text(getContext(),language,"알림","calendar.reminder.on")},enabled.selectedIndex());
        String[] labels={"분","시간","일","주"};for(int i=0;i<labels.length;i++)labels[i]=WmStrings.text(getContext(),language,labels[i],"calendar.reminder.unit."+i);unit.setChoices(labels,unit.selectedIndex());
    }
    private static final class Modern {static String tag(Locale locale){return locale.toLanguageTag();}}
    @Override public void onWmLanguageChanged(String language){setLocale(WmStrings.locale(WmStrings.language(getContext(),language)));}
}
