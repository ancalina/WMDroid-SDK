package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** Shared core of the native two-page appointment card (PIMDLG 20006/20044).
 * Subject/location, grouped start/end and yes/no all-day follow the recovered
 * field descriptors. Row/label/popup extents are provisional VGA adaptations.
 * Recurrence, native busy status, sensitivity and caller-owned category values
 * are shared; attendees remain unfinished. Caller must validate provider capabilities.
 * Caller owns provider identity, saving, reminder policy and shell commands.
 * Editor dates use the shared picker range 1900..2999; value() rejects values
 * outside it, including values set directly through the exposed fields.
 */
public final class WmAppointmentEditor extends FrameLayout implements WmControls.BackHandler,WmControls.RestyleListener,WmControls.ModalOwner,WmControls.LanguageListener {
    public static final int FREE=0,TENTATIVE=1,BUSY=2,OUT_OF_OFFICE=3;
    public static final int NORMAL=0,PERSONAL=1,PRIVATE=2,CONFIDENTIAL=3;
    public static final class Value {
        public final String subject,location,notes,categories;
        public final WmDateTimeField.Value start,end;
        public final boolean allDay;
        /** Native signed-short values, preserved even when absent from the four choices. */
        public final int busyStatus,sensitivity;
        public final WmReminder reminder;
        public final WmRecurrenceRule recurrenceRule;
        public final WmDateTimeField.Value recurrenceUntil;
        public Value(String subject,String location,String notes,WmDateTimeField.Value start,WmDateTimeField.Value end,boolean allDay){
            this(subject,location,notes,start,end,allDay,WmRecurrenceRule.none(),end);
        }
        public Value(String subject,String location,String notes,WmDateTimeField.Value start,WmDateTimeField.Value end,boolean allDay,WmRecurrenceRule rule,WmDateTimeField.Value until){
            this(subject,location,notes,start,end,allDay,rule,until,BUSY,NORMAL);
        }
        public Value(String subject,String location,String notes,WmDateTimeField.Value start,WmDateTimeField.Value end,boolean allDay,WmRecurrenceRule rule,WmDateTimeField.Value until,int busyStatus,int sensitivity){
            this(subject,location,notes,start,end,allDay,rule,until,busyStatus,sensitivity,new WmReminder(false,15,WmReminder.MINUTES));
        }
        public Value(String subject,String location,String notes,WmDateTimeField.Value start,WmDateTimeField.Value end,boolean allDay,WmRecurrenceRule rule,WmDateTimeField.Value until,int busyStatus,int sensitivity,WmReminder reminder){
            this(subject,location,notes,start,end,allDay,rule,until,busyStatus,sensitivity,reminder,"");
        }
        public Value(String subject,String location,String notes,WmDateTimeField.Value start,WmDateTimeField.Value end,boolean allDay,WmRecurrenceRule rule,WmDateTimeField.Value until,int busyStatus,int sensitivity,WmReminder reminder,String categories){
            checkCategories(categories);
            if(subject==null||location==null||notes==null||start==null||end==null||rule==null||until==null||reminder==null)throw new IllegalArgumentException("Appointment values required");
            if(busyStatus<Short.MIN_VALUE||busyStatus>Short.MAX_VALUE||sensitivity<Short.MIN_VALUE||sensitivity>Short.MAX_VALUE)throw new IllegalArgumentException("Native status values must be signed shorts");
            this.subject=subject;this.location=location;this.notes=notes;this.start=start;this.end=end;this.allDay=allDay;recurrenceRule=rule;recurrenceUntil=until;this.busyStatus=busyStatus;this.sensitivity=sensitivity;this.reminder=reminder;this.categories=categories;
        }
    }
    private final EditText subject,location,notes;
    private final WmDateTimeField start,end;
    private final WmChoice allDay,busy,sensitivity;
    private final View busyRow,sensitivityRow,reminderRow,categoriesRow;
    private final WmAppointmentReminder reminder;
    private int retainedBusy,retainedSensitivity;
    private final WmRecurrenceChoice recurrence;
    private WmDateTimeField.Value recurrenceUntil;
    private final WmPropertySheet sheet;
    private final LinearLayout form;
    private final WmScrollView formScroll;
    private final TextView calendar,error,categoriesField;
    private String categories;
    private final Map<TextView,String> captions=new LinkedHashMap<TextView,String>();
    private Locale locale;
    private String language;
    private Typeface font,buttonFont;
    private int accent,popupGeneration;
    private FrameLayout overlay;
    private View popup;
    private String calendarName;
    private Runnable calendarAction,categoriesAction,pageChanged;
    private boolean choosingEnd,restoring;
    public WmAppointmentEditor(Context context,Typeface font,Locale locale,Value initial,int accent,int tabColor){
        super(context);if(font==null||locale==null||initial==null)throw new IllegalArgumentException("Editor arguments required");
        this.font=font;buttonFont=font;this.locale=locale;language=language(locale);this.accent=accent;setBackgroundColor(Color.WHITE);
        form=new LinearLayout(context);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(8,8,8,0);
        subject=input(initial.subject,false);location=input(initial.location,false);notes=input(initial.notes,true);notes.setPadding(8,8,8,8);
        start=new WmDateTimeField(context,font,locale,initial.start,accent);end=new WmDateTimeField(context,font,locale,initial.end,accent);
        // PIMDLG_27D2718 -> PIMUTIL_135: outer300 - stock caption40 - post-font frame4.
        start.timeChoice().setClosedDropdownClientHeight(256);end.timeChoice().setClosedDropdownClientHeight(256);
        row("제목:",subject);row("장소:",location);row("시작:",start);row("종료:",end);
        allDay=new WmChoice(context,font,yesNo(),initial.allDay?1:0,accent);row("종일 행사:",allDay);
        recurrenceUntil=initial.recurrenceUntil;
        recurrence=new WmRecurrenceChoice(context,font,locale,initial.start,initial.recurrenceRule,accent);row("되풀이:",recurrence);
        recurrence.choice().setOnOpenListener(source->openRecurrenceChoice());recurrence.setOnEdit(this::openRecurrenceEditor);
        reminder=new WmAppointmentReminder(context,font,locale,initial.reminder,accent);reminderRow=row("미리 알림:",reminder,-2);
        reminder.enabledChoice().setOnOpenListener(source->openChoice(source));reminder.unitChoice().setOnOpenListener(source->openChoice(source));reminder.quantityField().setOnOpenListener(source->openReminderQuantity());
        retainedBusy=initial.busyStatus;retainedSensitivity=initial.sensitivity;
        busy=new WmChoice(context,font,busyLabels(),known(retainedBusy)?retainedBusy:0,accent);if(!known(retainedBusy))busy.clearSelection();busyRow=row("상태:",busy);
        sensitivity=new WmChoice(context,font,sensitivityLabels(),known(retainedSensitivity)?retainedSensitivity:0,accent);if(!known(retainedSensitivity))sensitivity.clearSelection();sensitivityRow=row("보안 수준:",sensitivity);
        busy.setOnOpenListener(source->openChoice(source));sensitivity.setOnOpenListener(source->openChoice(source));
        busy.setOnChoiceListener(index->retainedBusy=index);sensitivity.setOnChoiceListener(index->retainedSensitivity=index);
        categories=initial.categories;categoriesField=WmControls.text(context,font,"");WmControls.entry(categoriesField,accent);categoriesField.setEnabled(false);
        categoriesField.setOnClickListener(v->{closePopup();if(categoriesAction!=null)categoriesAction.run();});categoriesRow=row("범주:",categoriesField);categoriesRow.setVisibility(GONE);updateCategories();
        calendar=WmControls.text(context,font,"");WmControls.entry(calendar,accent);calendar.setEnabled(false);calendar.setOnClickListener(v->{closePopup();if(calendarAction!=null)calendarAction.run();});calendar.setMinimumHeight(44);form.addView(calendar,new LinearLayout.LayoutParams(-1,-2));
        error=WmControls.text(context,font,"");error.setTextColor(0xff800000);error.setGravity(Gravity.TOP);form.addView(error,new LinearLayout.LayoutParams(-1,-2));
        formScroll=new WmScrollView(context,form);sheet=new WmPropertySheet(context,font,tabColor);sheet.addPage(text("약속"),formScroll);sheet.addPage(text("메모"),notes);
        sheet.setOnPageChangedListener(index->{closePopup();if(!restoring&&pageChanged!=null)pageChanged.run();});addView(sheet,new FrameLayout.LayoutParams(-1,-1));
        start.setOnDateOpen(this::openDate);end.setOnDateOpen(this::openDate);start.setOnTimeOpen(this::openTime);end.setOnTimeOpen(this::openTime);
        start.timeChoice().setOnCandidateNavigationListener(index->chooseTimeCandidate(start,start.timeChoice().candidates()[index]));
        end.timeChoice().setOnCandidateNavigationListener(index->chooseTimeCandidate(end,end.timeChoice().candidates()[index]));
        // Native start edits preserve civil-minute duration. Provider timezone/DST
        // validation remains the caller's responsibility; UTC is arithmetic only.
        start.setOnValueChangedListener((oldValue,newValue)->{if(restoring)return;try{WmDateTimeField.Value last=end.getValue();end.setValue(at(millis(last)+millis(newValue)-millis(oldValue)));}catch(IllegalArgumentException invalid){}updateOrigin();});
        end.setOnValueChangedListener((oldValue,newValue)->{if(choosingEnd||restoring)return;try{if(millis(newValue)<millis(start.getValue()))start.setValue(newValue);}catch(IllegalArgumentException invalid){}updateOrigin();});
        allDay.setOnOpenListener(source->openChoice(source));allDay.setOnChoiceListener(index->updateAllDay());
        updateAllDay();updateOrigin();updateCalendar();
    }
    public EditText subjectField(){return subject;}
    public EditText locationField(){return location;}
    public EditText notesField(){return notes;}
    /** Start adjustment requires a valid existing end. Invalid/incomplete end or
     * unrepresentable result retains the end draft; direct setValue is silent.
     * Owner validates storage timezone/DST. */
    public WmDateTimeField startField(){return start;}
    public WmDateTimeField endField(){return end;}
    public WmChoice allDayChoice(){return allDay;}
    public WmRecurrenceChoice recurrenceChoice(){return recurrence;}
    public WmAppointmentReminder reminderField(){return reminder;}
    public void setReminderVisible(boolean visible){closePopup();reminderRow.setVisibility(visible?VISIBLE:GONE);}
    public WmChoice busyChoice(){return busy;}
    public WmChoice sensitivityChoice(){return sensitivity;}
    /** Hide both rows when the owner cannot preserve native status metadata. */
    public void setStatusFieldsVisible(boolean visible){closePopup();busyRow.setVisibility(visible?VISIBLE:GONE);sensitivityRow.setVisibility(visible?VISIBLE:GONE);}
    public void setRecurrenceEnabled(boolean enabled){recurrence.setEnabled(enabled);recurrence.choice().setEnabled(enabled);if(!enabled)closePopup();}
    public WmPropertySheet propertySheet(){return sheet;}
    /** Owner supplies selection UI and persistence; absent callback hides the row. */
    public void setOnCategoriesOpen(Runnable listener){categoriesAction=listener;categoriesField.setEnabled(listener!=null);categoriesRow.setVisibility(listener==null?GONE:VISIBLE);}
    public TextView categoriesField(){return categoriesField;}
    public String categories(){return categories;}
    /** Apply only an accepted picker value; cancellation must leave this value unchanged. */
    public void setCategories(String categories){checkCategories(categories);this.categories=categories;updateCategories();}
    private static void checkCategories(String value){if(value==null||value.indexOf('\0')>=0)throw new IllegalArgumentException("Category string without NUL required");}
    private void updateCategories(){categoriesField.setText(categories.length()==0?text("없음"):categories);}
    public TextView calendarField(){return calendar;}
    public void setOnCalendarOpen(Runnable listener){calendarAction=listener;calendar.setEnabled(listener!=null);}
    public void setOnPageChanged(Runnable listener){pageChanged=listener;}
    public void setCalendarName(String name){calendarName=name;updateCalendar();}
    public void setError(String authored){captions.put(error,authored==null?"":authored);error.setText(text(authored==null?"":authored));}
    public Value value(){return snapshot(true);}
    /** Draft snapshot without parsing invalid time, committing reminder blur or closing popups.
     * Provider identity/capabilities, transient choice/calendar popups and IME composition remain owner concerns. */
    public Bundle saveInteractionState(){
        Bundle state=new Bundle();state.putInt("schema",3);state.putString("categories",categories);state.putBundle("recurrence_popup",popup instanceof WmRecurrenceEditor?((WmRecurrenceEditor)popup).saveInteractionState():null);state.putBundle("start",start.saveInteractionState());state.putBundle("end",end.saveInteractionState());
        state.putString("subject",subject.getText().toString());state.putString("location",location.getText().toString());state.putString("notes",notes.getText().toString());state.putIntArray("carets",new int[]{subject.getSelectionStart(),subject.getSelectionEnd(),location.getSelectionStart(),location.getSelectionEnd(),notes.getSelectionStart(),notes.getSelectionEnd()});
        state.putString("focus",focusKey());state.putInt("page",sheet.selectedPage());state.putInt("scroll",formScroll.getRequestedScrollPosition());state.putInt("all_day",allDay.selectedIndex());state.putInt("busy",busy.selectedIndex()<0?retainedBusy:busy.selectedIndex());state.putInt("sensitivity",sensitivity.selectedIndex()<0?retainedSensitivity:sensitivity.selectedIndex());
        WmRecurrenceRule rule=recurrence.rule();state.putIntArray("rule",ruleParts(rule));state.putIntArray("until",dateParts(recurrenceUntil));state.putIntArray("recurrence_start",dateParts(recurrence.startValue()));state.putBundle("reminder",reminder.saveInteractionState());state.putString("error",captions.containsKey(error)?captions.get(error):"");return state;
    }
    private static Bundle recurrencePopupState(Bundle state){
        if(state==null||!(state.get("schema") instanceof Integer))throw new IllegalArgumentException("Appointment interaction schema required");
        int schema=state.getInt("schema");
        if(schema==1&&state.size()==18)return null;
        if(schema==3){if(state.size()!=20||!(state.get("categories") instanceof String))throw new IllegalArgumentException("Appointment categories required");checkCategories(state.getString("categories"));}
        if((schema!=2&&schema!=3)||(schema==2&&state.size()!=19)||!state.containsKey("recurrence_popup")||(state.get("recurrence_popup")!=null&&!(state.get("recurrence_popup") instanceof Bundle)))throw new IllegalArgumentException("Appointment interaction schema1/2/3 required");
        return state.getBundle("recurrence_popup");
    }
    public void validateInteractionState(Bundle state){validatedRecurrenceEditor(state);}
    private WmRecurrenceEditor validatedRecurrenceEditor(Bundle state){
        Bundle child=recurrencePopupState(state);
        for(String key:new String[]{"subject","location","notes","focus","error"})if(!(state.get(key) instanceof String))throw new IllegalArgumentException("Appointment text required");
        for(String key:new String[]{"page","scroll","all_day","busy","sensitivity"})if(!(state.get(key) instanceof Integer))throw new IllegalArgumentException("Appointment integer required");
        if(!(state.get("start") instanceof Bundle)||!(state.get("end") instanceof Bundle)||!(state.get("reminder") instanceof Bundle))throw new IllegalArgumentException("Appointment child states required");
        start.validateInteractionState(state.getBundle("start"));end.validateInteractionState(state.getBundle("end"));WmAppointmentReminder.validateInteractionState(state.getBundle("reminder"));
        String focus=state.getString("focus");for(String key:new String[]{"start","end"}){String expected=focus.equals(key+"_date")?"date":focus.equals(key+"_time")?"time":"none";if(!expected.equals(state.getBundle(key).getString("focus")))throw new IllegalArgumentException("Inconsistent date/time focus");}
        if(state.getInt("page")<0||state.getInt("page")>=sheet.pageCount()||state.getInt("scroll")<0||state.getInt("all_day")<0||state.getInt("all_day")>1||state.getInt("busy")<Short.MIN_VALUE||state.getInt("busy")>Short.MAX_VALUE||state.getInt("sensitivity")<Short.MIN_VALUE||state.getInt("sensitivity")>Short.MAX_VALUE||focusView(state.getString("focus"))==null&&!state.getString("focus").equals("none"))throw new IllegalArgumentException("Invalid appointment page/status/focus");
        Object raw=state.get("carets");if(!(raw instanceof int[])||((int[])raw).length!=6)throw new IllegalArgumentException("Three text selections required");int[] carets=(int[])raw;String[] keys={"subject","location","notes"};for(int i=0;i<3;i++){int a=carets[i*2],b=carets[i*2+1],length=state.getString(keys[i]).length();if(!((a==-1&&b==-1)||(a>=0&&b>=0&&a<=length&&b<=length)))throw new IllegalArgumentException("Invalid text selection");}
        stateRule(state);stateDate(state,"until");stateDate(state,"recurrence_start");
        if(child==null)return null;
        if(!recurrence.isEnabled()||state.getInt("page")!=0)throw new IllegalArgumentException("Invalid recurrence popup owner");
        WmRecurrenceEditor editor=new WmRecurrenceEditor(getContext(),font,locale,WmRecurrenceEditor.initialValue(child),accent,accent);
        editor.validateInteractionState(child);return editor;
    }
    /** Restore into a closed owner; an included recurrence draft reopens after validation. */
    public void restoreInteractionState(Bundle state){
        if(restoring)throw new IllegalStateException("Reentrant appointment restoration");WmRecurrenceEditor restoredEditor=validatedRecurrenceEditor(state);
        if(popup!=null||start.dateChoice().isDroppedDown()||start.timeChoice().isDroppedDown()||end.dateChoice().isDroppedDown()||end.timeChoice().isDroppedDown()||allDay.isDroppedDown()||busy.isDroppedDown()||sensitivity.isDroppedDown()||recurrence.choice().isDroppedDown()||reminder.enabledChoice().isDroppedDown()||reminder.unitChoice().isDroppedDown()||reminder.quantityField().isDroppedDown())throw new IllegalStateException("Close appointment popups before restoring");
        if(restoredEditor!=null)restoredEditor.restoreInteractionState(state.getBundle("recurrence_popup"));
        WmRecurrenceRule rule=stateRule(state);WmDateTimeField.Value until=stateDate(state,"until"),recurrenceStart=stateDate(state,"recurrence_start");restoring=true;
        try{
            setCategories(state.getInt("schema")==3?state.getString("categories"):"");
            sheet.restoreSelectedPage(state.getInt("page"));subject.setText(state.getString("subject"));location.setText(state.getString("location"));notes.setText(state.getString("notes"));
            allDay.setSelectedIndex(state.getInt("all_day"));updateAllDay();start.restoreInteractionState(state.getBundle("start"));end.restoreInteractionState(state.getBundle("end"));
            retainedBusy=state.getInt("busy");retainedSensitivity=state.getInt("sensitivity");if(known(retainedBusy))busy.setSelectedIndex(retainedBusy);else busy.clearSelection();if(known(retainedSensitivity))sensitivity.setSelectedIndex(retainedSensitivity);else sensitivity.clearSelection();
            recurrenceUntil=until;recurrence.setStart(recurrenceStart);recurrence.setRule(rule);reminder.restoreInteractionState(state.getBundle("reminder"),()->{View focused=focusView(state.getString("focus"));if(focused==null)clearFocus();else focused.requestFocus();});setError(state.getString("error"));
            int[] carets=state.getIntArray("carets");EditText[] inputs={subject,location,notes};for(int i=0;i<3;i++)if(carets[i*2]<0)android.text.Selection.removeSelection(inputs[i].getText());else inputs[i].setSelection(carets[i*2],carets[i*2+1]);formScroll.scrollToPosition(state.getInt("scroll"));
            if(restoredEditor!=null)presentRecurrenceEditor(restoredEditor);
        }finally{restoring=false;}
    }
    private String focusKey(){for(String key:FOCUS_KEYS){View view=focusView(key);if(view!=null&&view.hasFocus())return key;}return "none";}
    private static final String[] FOCUS_KEYS={"subject","location","notes","start_date","start_time","end_date","end_time","all_day","recurrence","reminder_enabled","reminder_quantity","reminder_unit","busy","sensitivity","categories"};
    private View focusView(String key){switch(key){case "subject":return subject;case "location":return location;case "notes":return notes;case "start_date":return start.dateChoice();case "start_time":return start.timeChoice();case "end_date":return end.dateChoice();case "end_time":return end.timeChoice();case "all_day":return allDay;case "recurrence":return recurrence.choice();case "reminder_enabled":return reminder.enabledChoice();case "reminder_quantity":return reminder.quantityField();case "reminder_unit":return reminder.unitChoice();case "categories":return categoriesAction==null?null:categoriesField;case "busy":return busy;case "sensitivity":return sensitivity;default:return null;}}
    static int[] ruleParts(WmRecurrenceRule r){return new int[]{r.pattern,r.interval,r.dayMask,r.day,r.month,r.ordinal,r.endMode,r.count};}
    static WmRecurrenceRule stateRule(Bundle state){
        Object raw=state.get("rule");if(!(raw instanceof int[])||((int[])raw).length!=8)throw new IllegalArgumentException("Recurrence parts required");int[] p=(int[])raw;WmRecurrenceRule rule;
        switch(p[0]){case WmRecurrenceRule.NONE:rule=WmRecurrenceRule.none();break;case WmRecurrenceRule.DAILY:if(p[2]!=0&&p[2]!=WmRecurrenceRule.WEEKDAYS)throw new IllegalArgumentException("Daily weekday mask required");rule=WmRecurrenceRule.daily(p[1],p[2]!=0);break;case WmRecurrenceRule.WEEKLY:rule=WmRecurrenceRule.weekly(p[1],p[2]);break;case WmRecurrenceRule.MONTHLY_DAY:rule=WmRecurrenceRule.monthlyDay(p[1],p[3]);break;case WmRecurrenceRule.MONTHLY_ORDINAL:rule=WmRecurrenceRule.monthlyOrdinal(p[1],p[5],p[2]);break;case WmRecurrenceRule.YEARLY_DAY:rule=WmRecurrenceRule.yearlyDay(p[4],p[3]);break;case WmRecurrenceRule.YEARLY_ORDINAL:rule=WmRecurrenceRule.yearlyOrdinal(p[4],p[5],p[2]);break;default:throw new IllegalArgumentException("Unknown recurrence pattern");}
        if(p[6]==WmRecurrenceRule.COUNT)rule=rule.withCount(p[7]);else if(p[6]==WmRecurrenceRule.UNTIL)rule=rule.withUntil();else if(p[6]!=WmRecurrenceRule.NEVER)throw new IllegalArgumentException("Unknown recurrence range");if(!java.util.Arrays.equals(p,ruleParts(rule)))throw new IllegalArgumentException("Noncanonical recurrence parts");return rule;
    }
    static int[] dateParts(WmDateTimeField.Value v){return new int[]{v.year,v.month,v.day,v.hour,v.minute};}
    static WmDateTimeField.Value stateDate(Bundle state,String key){Object raw=state.get(key);if(!(raw instanceof int[])||((int[])raw).length!=5)throw new IllegalArgumentException("Recurrence date parts required");int[] p=(int[])raw;return new WmDateTimeField.Value(p[0],p[1],p[2],p[3],p[4]);}
    private Value snapshot(boolean validateReminder){
        Value result;
        try{
            checkRange(start.dateValue());checkRange(end.dateValue());boolean fullDay=allDay.selectedIndex()==1;
            result=new Value(subject.getText().toString(),location.getText().toString(),notes.getText().toString(),fullDay?dateOnly(start.dateValue()):start.getValue(),fullDay?dateOnly(end.dateValue()):end.getValue(),fullDay,recurrence.rule(),recurrenceUntil,busy.selectedIndex()<0?retainedBusy:busy.selectedIndex(),sensitivity.selectedIndex()<0?retainedSensitivity:sensitivity.selectedIndex(),reminder.value(),categories);
        }catch(IllegalArgumentException invalid){throw new IllegalArgumentException("올바른 시각을 입력하세요.",invalid);}
        if(validateReminder)result.recurrenceRule.validateAppointmentReminder(result.reminder.enabled,result.reminder.minutes());
        return result;
    }
    public void setLocale(Locale locale){
        if(locale==null)throw new IllegalArgumentException("Locale required");if(this.locale.equals(locale))return;this.locale=locale;language=language(locale);
        if(popup instanceof WmRecurrenceEditor)((WmRecurrenceEditor)popup).setLocale(locale);else closePopup();start.setLocale(locale);end.setLocale(locale);allDay.setChoices(yesNo(),allDay.selectedIndex());recurrence.setLocale(locale);
        reminder.setLocale(locale);
        relabel(busy,busyLabels());relabel(sensitivity,sensitivityLabels());
        for(Map.Entry<TextView,String> entry:captions.entrySet())entry.getKey().setText(text(entry.getValue()));
        sheet.setPageTitle(0,text("약속"));sheet.setPageTitle(1,text("메모"));updateCalendar();updateCategories();
    }
    private String language(Locale locale){return WmStrings.language(getContext(),android.os.Build.VERSION.SDK_INT>=21?Modern.tag(locale):locale.toString().replace('_','-'));}
    private static final class Modern {static String tag(Locale locale){return locale.toLanguageTag();}}
    /** Shared language changes follow the existing locale/edit preservation policy. */
    @Override public void onWmLanguageChanged(String language){setLocale(WmStrings.locale(WmStrings.language(getContext(),language)));}
    private String text(String authored){return WmStrings.text(getContext(),language,authored);}
    private static boolean known(int value){return value>=0&&value<4;}
    private static void relabel(WmChoice field,String[] labels){int index=field.selectedIndex();field.setChoices(labels,index<0?0:index);if(index<0)field.clearSelection();}
    private String[] busyLabels(){return statusLabels(new String[]{"약속 없음","미정","약속 있음","부재 중"},"calendar.busy.");}
    private String[] sensitivityLabels(){return statusLabels(new String[]{"보통","개인","비공개","기밀"},"calendar.sensitivity.");}
    private String[] statusLabels(String[] labels,String prefix){for(int i=0;i<labels.length;i++)labels[i]=WmStrings.text(getContext(),language,labels[i],prefix+i);return labels;}
    private String[] yesNo(){return new String[]{text("아니요"),text("예")};}
    private void updateCalendar(){calendar.setText(calendarName==null?text("캘린더 선택"):String.format(locale,text("캘린더: %s"),calendarName));}
    private EditText input(String value,boolean multi){EditText field=new EditText(getContext());field.setInputType(InputType.TYPE_CLASS_TEXT|(multi?InputType.TYPE_TEXT_FLAG_MULTI_LINE:0));field.setSingleLine(!multi);WmControls.input(field,font);field.setText(value);if(multi)field.setGravity(Gravity.TOP);return field;}
    private View row(String authored,View field){return row(authored,field,44);}
    private View row(String authored,View field,int height){
        LinearLayout row=new LinearLayout(getContext());row.setGravity(height<0?Gravity.TOP:Gravity.CENTER_VERTICAL);
        row.setPadding(0,height<0?0:2,0,height<0?4:2);
        if(height>=0){field.setMinimumHeight(height);row.setMinimumHeight(height+4);}
        TextView label=WmControls.text(getContext(),font,text(authored));captions.put(label,authored);label.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);label.setPadding(0,0,8,0);label.setMinimumHeight(44);
        row.addView(label,new LinearLayout.LayoutParams(128,-2));row.addView(field,new LinearLayout.LayoutParams(0,-2,1));form.addView(row,new LinearLayout.LayoutParams(-1,-2));return row;
    }
    private void updateAllDay(){boolean enabled=allDay.selectedIndex()==0;start.timeChoice().setEnabled(enabled);end.timeChoice().setEnabled(enabled);}
    private void updateOrigin(){try{WmDateTimeField.Value value=start.getValue();end.setTimeOrigin(value.hour,value.minute);recurrence.setStart(value);}catch(IllegalArgumentException invalid){}}
    private void openRecurrenceChoice(){
        try{recurrence.setStart(allDay.selectedIndex()==1?start.dateValue():start.getValue());}catch(IllegalArgumentException invalid){setError("올바른 시각을 입력하세요.");return;}
        final int generation=popupGeneration+1;
        WmRadioMenu menu=new WmRadioMenu(getContext(),font,recurrence.choice().choices(),recurrence.choice().selectedIndex(),"",accent,index->{if(generation!=popupGeneration)return;closePopup();recurrence.choose(index);});
        menu.enableChoiceKeyboard(recurrence.choice().selectedIndex(),index->{if(generation==popupGeneration&&recurrence.isEnabled()){if(index==4)closePopup();recurrence.choose(index);}},()->{if(generation==popupGeneration){closePopup();if(recurrence.choice().isShown())recurrence.choice().requestFocus();}});
        menu.enableChoiceCharacterSearch(null,index->{if(generation==popupGeneration&&recurrence.isEnabled()&&recurrence.choice().isEnabled()){if(index==4)closePopup();recurrence.choice().chooseCharacterDisplayIndex(index);}});
        int height=Math.min(menu.choiceDropdownHeight(256),Math.max(0,getHeight()-WmMetrics.TAB));
        present(menu.menuHeight()>height?new WmScrollView(getContext(),menu):menu,recurrence.choice(),Math.max(200,menu.menuWidth()),height);menu.requestFocus();
    }
    private void openRecurrenceEditor(){
        try{
            Value initial=snapshot(false);long length=millis(initial.end)-millis(initial.start)+(initial.allDay?86400000L:0);
            if(length<0||length%60000L!=0||length/60000L>Integer.MAX_VALUE)throw new IllegalArgumentException("유효한 지속 시간을 입력하십시오.");
            WmRecurrenceEditor editor=new WmRecurrenceEditor(getContext(),font,locale,new WmRecurrenceEditor.Value(initial.recurrenceRule,initial.start,initial.recurrenceUntil,(int)(length/60000L),initial.allDay),accent,accent);
            presentRecurrenceEditor(editor);
        }catch(IllegalArgumentException invalid){setError(invalid.getMessage());}
    }
    private void presentRecurrenceEditor(WmRecurrenceEditor editor){
            WmControls.restyleTree(editor,font,buttonFont,accent,accent);
            closePopup();final int generation=popupGeneration;
            editor.setOnCancel(()->{if(generation==popupGeneration)closePopup();});
            editor.setOnFinish(()->{
                if(generation!=popupGeneration)return;
                try{
                    WmRecurrenceEditor.Value result=editor.value();
                    WmDateTimeField.Value last=at(millis(result.start)+(long)result.durationMinutes*60000L-(result.allDay?86400000L:0));checkRange(last);
                    start.setValue(result.start);end.setValue(last);recurrenceUntil=result.until;recurrence.setStart(result.start);recurrence.setRule(result.rule);updateOrigin();setError("");closePopup();
                }catch(IllegalArgumentException invalid){editor.setError(invalid.getMessage());}
            });
            popup=editor;overlay=new FrameLayout(getContext());overlay.setBackgroundColor(Color.WHITE);overlay.addView(editor,new FrameLayout.LayoutParams(-1,-1));addView(overlay,new FrameLayout.LayoutParams(-1,-1));
    }
    private void openDate(WmDateTimeField field){
        WmDateTimeField.Value value=field.dateValue();if(value.year<WmYearChooser.MIN_YEAR||value.year>WmYearChooser.MAX_YEAR){setError("날짜는 1900년부터 2999년까지 선택할 수 있습니다.");return;}final int generation=popupGeneration+1;
        WmCalendarPicker picker=new WmCalendarPicker(getContext(),font,locale,value.year,value.month,value.day,accent,(y,m,d)->{if(generation!=popupGeneration)return;closePopup();field.selectDate(y,m,d);});picker.setOnDateChangeListener((y,m,d)->{if(generation==popupGeneration)field.selectDate(y,m,d);});android.graphics.Point size=picker.naturalSize();present(picker,field.dateChoice(),size.x,size.y);
    }
    private void openTime(WmDateTimeField field){
        if(field==end)updateOrigin();
        final String[] choices=field.timeChoice().candidates();final int generation=popupGeneration+1;int selected=field.timeChoice().dropdownSelectedIndex();
        WmRadioMenu.Selection commit=index->chooseTimeCandidate(field,choices[index]);
        WmRadioMenu menu=new WmRadioMenu(getContext(),font,choices,selected,"",accent,index->{if(generation!=popupGeneration)return;closePopup();commit.choose(index);});
        menu.enableChoiceKeyboard(selected,index->{if(generation==popupGeneration)commit.choose(index);},()->{if(generation==popupGeneration){closePopup();if(field.timeChoice().isShown())field.timeChoice().requestFocus();}});
        present(new WmScrollView(getContext(),menu),field.timeChoice(),Math.max(200,menu.menuWidth()),menu.choiceDropdownHeight(field.timeChoice().getClosedDropdownClientHeight()));menu.requestFocus();
    }
    private void chooseTimeCandidate(WmDateTimeField field,String label){
        if(!field.timeChoice().isEnabled())return;
        // End correction can reorder candidates while the keyboard list stays
        // open. Resolve the captured label against the current field order.
        String[] current=field.timeChoice().candidates();int candidateIndex=-1;
        for(int i=0;i<current.length;i++)if(current[i].equals(label)){candidateIndex=i;break;}
        if(candidateIndex<0)return;
        // Only selecting an end-time candidate wraps an earlier clock time
        // to tomorrow. Typed values use the native final range correction.
        if(field==end){
            choosingEnd=true;try{field.timeChoice().choose(candidateIndex);}finally{choosingEnd=false;}
            try{
                WmDateTimeField.Value s=start.getValue(),e=end.getValue();
                if(e.year==s.year&&e.month==s.month&&e.day==s.day&&e.hour*60+e.minute<s.hour*60+s.minute)e=at(millis(e)+86400000L);
                // An earlier date does not wrap. Native moves start to end
                // and keeps a zero-length draft for the owner to validate.
                if(millis(e)<millis(s))start.setValue(e);
                end.setValue(e);
            }catch(IllegalArgumentException invalid){}
            updateOrigin();return;
        }
        field.timeChoice().choose(candidateIndex);
    }
    private void openReminderQuantity(){
        WmEditableChoice field=reminder.quantityField();if(!field.isEnabled())return;
        String[] labels=field.candidates();int selected=field.dropdownSelectedIndex();
        final int generation=popupGeneration+1;
        WmRadioMenu menu=new WmRadioMenu(getContext(),font,labels,selected,"",accent,index->{if(generation!=popupGeneration||!field.isEnabled())return;closePopup();field.choose(index);reminder.commitQuantity();});
        menu.enableChoiceKeyboard(selected,index->{if(generation==popupGeneration&&field.isEnabled()){field.choose(index);reminder.commitQuantity();}},()->{if(generation==popupGeneration){closePopup();if(field.isShown())field.requestFocus();}});
        present(new WmScrollView(getContext(),menu),field,Math.max(160,menu.menuWidth()),menu.choiceDropdownHeight(field.getClosedDropdownClientHeight()));reminder.focusQuantityPopup(menu);
    }
    private void openChoice(WmChoice field){final int generation=popupGeneration+1;WmRadioMenu menu=new WmRadioMenu(getContext(),font,field.displayChoices(),field.displaySelectedIndex(),"",accent,index->{if(generation!=popupGeneration)return;closePopup();field.chooseDisplayIndex(index);});menu.enableChoiceKeyboard(field.displaySelectedIndex(),index->{if(generation==popupGeneration)field.chooseDisplayIndex(index);},()->{if(generation==popupGeneration){closePopup();if(field.isShown())field.requestFocus();}});menu.enableChoiceCharacterSearch(field.displaySortLocale(),index->{if(generation==popupGeneration&&field.isEnabled())field.chooseCharacterDisplayIndex(index);});int height=Math.min(menu.choiceDropdownHeight(256),Math.max(0,getHeight()-WmMetrics.TAB));present(menu.menuHeight()>height?new WmScrollView(getContext(),menu):menu,field,Math.max(160,menu.menuWidth()),height);menu.requestFocus();}
    private void present(View content,View anchor,int desiredWidth,int desiredHeight){
        closePopup();popup=content;overlay=new FrameLayout(getContext());overlay.setOnClickListener(v->{if(!(popup instanceof WmCalendarPicker)||!WmControls.back(popup))closePopup();});content.setClickable(true);View presented=content;if(content instanceof WmCalendarPicker){FrameLayout frame=new FrameLayout(getContext());frame.setBackgroundColor(android.graphics.Color.BLACK);frame.setPadding(2,2,2,2);frame.setClickable(true);frame.addView(content,new FrameLayout.LayoutParams(-1,-1));presented=frame;desiredWidth+=4;desiredHeight+=4;}
        int width=Math.max(0,Math.min(getWidth(),desiredWidth)),height=Math.max(0,Math.min(Math.max(0,getHeight()-WmMetrics.TAB),desiredHeight));
        Rect bounds=new Rect(0,0,anchor.getWidth(),anchor.getHeight());offsetDescendantRectToMyCoords(anchor,bounds);
        int x=Math.max(0,Math.min(getWidth()-width,bounds.left)),y=bounds.bottom;if(y+height>getHeight()-WmMetrics.TAB)y=bounds.top-height;y=Math.max(0,Math.min(Math.max(0,getHeight()-WmMetrics.TAB-height),y));
        FrameLayout.LayoutParams position=new FrameLayout.LayoutParams(width,height);position.leftMargin=x;position.topMargin=y;overlay.addView(presented,position);addView(overlay,new FrameLayout.LayoutParams(-1,-1));WmChoice.bindDropdown(anchor,overlay);
    }
    public boolean closePopup(){popupGeneration++;if(overlay==null)return false;removeView(overlay);overlay=null;popup=null;return true;}
    @Override public boolean blocksOwnerCommands(){return popup instanceof WmRecurrenceEditor;}
    @Override public View inputMethodOwner(){return popup;}
    @Override public boolean back(){if(popup!=null&&WmControls.back(popup))return true;return closePopup();}
    @Override protected void onDetachedFromWindow(){closePopup();super.onDetachedFromWindow();}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){font=regular;buttonFont=bold==null?regular:bold;accent=selection;if(!(popup instanceof WmRecurrenceEditor))closePopup();}
    private static void checkRange(WmDateTimeField.Value value){if(value.year<WmYearChooser.MIN_YEAR||value.year>WmYearChooser.MAX_YEAR)throw new IllegalArgumentException("Editor date outside1900..2999");}
    private static WmDateTimeField.Value dateOnly(WmDateTimeField.Value value){return new WmDateTimeField.Value(value.year,value.month,value.day,0,0);}
    private static long millis(WmDateTimeField.Value value){GregorianCalendar c=new GregorianCalendar(TimeZone.getTimeZone("UTC"),Locale.US);c.setGregorianChange(new Date(Long.MIN_VALUE));c.clear();c.setLenient(false);c.set(value.year,value.month-1,value.day,value.hour,value.minute,0);return c.getTimeInMillis();}
    private static WmDateTimeField.Value at(long millis){GregorianCalendar c=new GregorianCalendar(TimeZone.getTimeZone("UTC"),Locale.US);c.setGregorianChange(new Date(Long.MIN_VALUE));c.setTimeInMillis(millis);return new WmDateTimeField.Value(c.get(java.util.Calendar.YEAR),c.get(java.util.Calendar.MONTH)+1,c.get(java.util.Calendar.DAY_OF_MONTH),c.get(java.util.Calendar.HOUR_OF_DAY),c.get(java.util.Calendar.MINUTE));}
}
