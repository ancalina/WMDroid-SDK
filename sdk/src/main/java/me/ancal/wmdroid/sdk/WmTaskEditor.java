package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Task card core from PIMDLG 20007/20044. Owner saves values and supplies shell commands.
 * Inactive rows follow the captured VGA PIM card; ordinary date coupling follows the recovered task owner.
 * shortcut: recurrence and native category database services are not ported. */
public final class WmTaskEditor extends FrameLayout implements WmControls.BackHandler,WmControls.RestyleListener,WmControls.LanguageListener {
    public static final class Value {
        public final String subject,categories,notes;
        public final WmDateTimeField.Value start,due,reminder;
        /** priority: positive high, zero normal, negative low. sensitivity: signed short,
         * 0 normal,1 personal,2 private,3 confidential; other values persist until user selection. */
        public final int priority,sensitivity;
        public final boolean completed;
        public Value(String subject,int priority,boolean completed,WmDateTimeField.Value start,WmDateTimeField.Value due,String categories,int sensitivity,String notes){
            this(subject,priority,completed,start,due,categories,sensitivity,notes,null);
        }
        /** start/due are nullable dates normalized to00:00. reminder keeps its local
         * hour/minute; null disables it. All dates require1752-09-14..9999-12-31.
         * Invalid dates, null/NUL strings or non-short sensitivity throw IllegalArgumentException.
         * The owner schedules reminder delivery. */
        public Value(String subject,int priority,boolean completed,WmDateTimeField.Value start,WmDateTimeField.Value due,String categories,int sensitivity,String notes,WmDateTimeField.Value reminder){
            if(subject==null||categories==null||notes==null||subject.indexOf('\0')>=0||categories.indexOf('\0')>=0||notes.indexOf('\0')>=0)throw new IllegalArgumentException("Task strings without NUL required");
            if(sensitivity<Short.MIN_VALUE||sensitivity>Short.MAX_VALUE)throw new IllegalArgumentException("Sensitivity must be a signed short");
            this.start=WmDateField.normalized(start);this.due=WmDateField.normalized(due);if(reminder!=null)WmDateField.normalized(reminder);this.reminder=reminder;this.subject=subject;this.priority=priority;this.completed=completed;this.categories=categories;this.sensitivity=sensitivity;this.notes=notes;
        }
    }
    private final EditText subject,categories,notes;
    private final android.text.method.KeyListener categoriesInput;
    private Runnable categoriesAction;
    private final WmChoice priority,status,sensitivity,reminderMode;
    private final WmDateTimeField reminder;
    private final WmDateField start,due;
    private final WmPropertySheet sheet;
    private final LinearLayout form;
    private final Map<TextView,String> captions=new LinkedHashMap<TextView,String>();
    private Typeface font,bold;
    private Locale locale;
    private String language;
    private int accent,retainedPriority,retainedSensitivity,generation;
    private View popup;
    private FrameLayout overlay;
    public WmTaskEditor(Context context,Typeface font,Locale locale,Value initial,int accent,int tabColor){
        super(context);if(font==null||locale==null||initial==null)throw new IllegalArgumentException("Task editor arguments required");
        this.font=font;bold=Typeface.create(font,Typeface.BOLD);this.locale=locale;language=WmStrings.language(context,locale.toString().replace('_','-'));this.accent=accent;setBackgroundColor(Color.WHITE);
        form=new LinearLayout(context){@Override protected void onMeasure(int width,int height){int scrollInset=Math.max(0,WmTaskEditor.this.getWidth()-MeasureSpec.getSize(width));setPadding(0,0,Math.max(0,25-scrollInset),0);super.onMeasure(width,height);}};form.setOrientation(LinearLayout.VERTICAL);form.setPadding(0,0,25,0);
        subject=input(initial.subject,false);categories=input(initial.categories,false);categoriesInput=categories.getKeyListener();notes=input(initial.notes,true);
        categories.setOnClickListener(v->{if(categoriesAction!=null&&categories.isEnabled()){closePopup();categoriesAction.run();}});
        retainedPriority=initial.priority;retainedSensitivity=initial.sensitivity;
        priority=new WmChoice(context,font,labels("높음","보통","낮음"),initial.priority>0?0:initial.priority<0?2:1,accent);
        status=new WmChoice(context,font,labels("완료되지 않음","완료"),initial.completed?1:0,accent);
        sensitivity=new WmChoice(context,font,labels("보통","개인","비공개","기밀"),initial.sensitivity>=0&&initial.sensitivity<4?initial.sensitivity:0,accent);if(initial.sensitivity<0||initial.sensitivity>=4)sensitivity.clearSelection();
        start=new WmDateField(context,font,locale,initial.start,accent,true);due=new WmDateField(context,font,locale,initial.due,accent,true);
        reminderMode=new WmChoice(context,font,reminderLabels(),initial.reminder==null?0:1,accent);
        reminder=new WmDateTimeField(context,font,locale,initial.reminder==null?defaultReminder():initial.reminder,accent,false,true);
        for(WmChoice choice:new WmChoice[]{priority,status,sensitivity,reminderMode})choice.setPimInline();start.setPimInline();due.setPimInline();
        row("제목:",subject);row("우선 순위:",priority);row("상태:",status);row("시작:",start);row("기한:",due);row("미리 알림:",reminderMode);row("",reminder);row("범주:",categories);row("보안 수준:",sensitivity);
        priority.setOnChoiceListener(index->retainedPriority=1-index);sensitivity.setOnChoiceListener(index->retainedSensitivity=index);
        for(WmChoice choice:new WmChoice[]{priority,status,sensitivity,reminderMode})choice.setOnOpenListener(this::openChoice);
        reminderMode.setOnChoiceListener(index->{if(index==1)reminder.setValue(defaultReminder());refreshReminder();});
        reminder.setOnDateOpen(this::openReminderDate);reminder.setOnTimeOpen(this::openReminderTime);refreshReminder();
        start.setOnOpenListener(this::openDate);due.setOnOpenListener(this::openDate);
        start.setOnValueChangedListener((previous,next)->startChanged(previous,next));
        due.setOnValueChangedListener((previous,next)->{if(next==null)start.setValue(null);else if(start.value()!=null&&dateMillis(start.value())>dateMillis(next)){start.setValue(next);if(reminderMode.selectedIndex()==1)reminder.setValue(atEight(next));}});
        sheet=new WmPropertySheet(context,font,tabColor);sheet.addPage(20007,text("작업"),new WmScrollView(context,form));sheet.addPage(20044,text("메모"),notes);sheet.setOnPageChangedListener(index->closePopup());addView(sheet,new FrameLayout.LayoutParams(-1,-1));
    }
    public EditText subjectField(){return subject;}
    public EditText categoriesField(){return categories;}
    /** The owner supplies a catalog, selector, acceptance and persistence. Null restores text input. */
    public void setOnCategoriesOpen(Runnable listener){categoriesAction=listener;categories.setKeyListener(listener==null?categoriesInput:null);categories.setFocusable(listener==null);categories.setFocusableInTouchMode(listener==null);categories.setCursorVisible(listener==null);categories.setHint(listener==null?"":text("없음"));categories.setHintTextColor(Color.BLACK);}
    public String categories(){return categories.getText().toString();}
    public void setCategories(String value){if(value==null||value.indexOf('\0')>=0)throw new IllegalArgumentException("Task categories without NUL required");categories.setText(value);}
    public EditText notesField(){return notes;}
    public WmChoice priorityChoice(){return priority;}
    public WmChoice statusChoice(){return status;}
    public WmChoice sensitivityChoice(){return sensitivity;}
    public WmDateField startField(){return start;}
    public WmDateField dueField(){return due;}
    public WmChoice reminderChoice(){return reminderMode;}
    public WmDateTimeField reminderField(){return reminder;}
    public WmPropertySheet propertySheet(){return sheet;}
    /** Snapshot only. Native save copies the sole nonempty date into the empty peer. */
    public Value value(){WmDateTimeField.Value a=start.value(),b=due.value();if(a==null)a=b;if(b==null)b=a;return new Value(subject.getText().toString(),retainedPriority,status.selectedIndex()==1,a,b,categories.getText().toString(),retainedSensitivity,notes.getText().toString(),reminderMode.selectedIndex()==1?reminder.value():null);}
    private String[] reminderLabels(){return new String[]{WmStrings.text(getContext(),language,"없음","calendar.reminder.off"),WmStrings.text(getContext(),language,"알림","calendar.reminder.on")};}
    private WmDateTimeField.Value defaultReminder(){WmDateTimeField.Value base=start.value();if(base==null)base=due.value();if(base==null){Calendar today=Calendar.getInstance();base=new WmDateTimeField.Value(today.get(Calendar.YEAR),today.get(Calendar.MONTH)+1,today.get(Calendar.DAY_OF_MONTH),0,0);}return atEight(base);}
    private static WmDateTimeField.Value atEight(WmDateTimeField.Value date){return new WmDateTimeField.Value(date.year,date.month,date.day,8,0);}
    private void refreshReminder(){reminder.setEnabled(reminderMode.selectedIndex()==1);reminder.setVisibility(reminderMode.selectedIndex()==1?VISIBLE:INVISIBLE);}
    private void shiftReminder(WmDateTimeField.Value previous,WmDateTimeField.Value next){
        if(reminderMode.selectedIndex()!=1||previous==null||next==null)return;
        GregorianCalendar shifted=dateCalendar(reminder.dateValue());shifted.add(Calendar.DAY_OF_MONTH,(int)((dateMillis(next)-dateMillis(previous))/86400000L));
        // shortcut: retain an out-of-range reminder until native overflow behavior is measured.
        try{reminder.selectDate(shifted.get(Calendar.YEAR),shifted.get(Calendar.MONTH)+1,shifted.get(Calendar.DAY_OF_MONTH));}catch(IllegalArgumentException outside){}
    }
    private void startChanged(WmDateTimeField.Value previous,WmDateTimeField.Value next){
        if(next==null)return;WmDateTimeField.Value last=due.value();if(last==null){due.setValue(next);return;}
        int duration=previous==null?65535:(int)((dateMillis(last)-dateMillis(previous))/86400000L)&65535;
        if(duration==65535){if(dateMillis(next)>dateMillis(last)){due.setValue(next);shiftReminder(last,next);}return;}
        GregorianCalendar shifted=dateCalendar(next);shifted.add(Calendar.DAY_OF_MONTH,duration);
        // shortcut: reject shifts outside the SDK date range until native range/overflow behavior is measured.
        try{due.setValue(new WmDateTimeField.Value(shifted.get(Calendar.YEAR),shifted.get(Calendar.MONTH)+1,shifted.get(Calendar.DAY_OF_MONTH),0,0));shiftReminder(last,due.value());}catch(IllegalArgumentException outside){start.setValue(previous);}
    }
    private static GregorianCalendar dateCalendar(WmDateTimeField.Value value){GregorianCalendar result=new GregorianCalendar(TimeZone.getTimeZone("UTC"),Locale.US);result.setGregorianChange(new java.util.Date(Long.MIN_VALUE));result.clear();result.setLenient(false);result.set(value.year,value.month-1,value.day,0,0,0);return result;}
    private static long dateMillis(WmDateTimeField.Value value){return dateCalendar(value).getTimeInMillis();}
    private String text(String authored){return WmStrings.text(getContext(),language,authored);}
    private String[] labels(String... authored){String[] result=new String[authored.length];for(int i=0;i<result.length;i++)result[i]=text(authored[i]);return result;}
    private EditText input(String value,boolean multi){EditText field=new EditText(getContext());field.setInputType(InputType.TYPE_CLASS_TEXT|(multi?InputType.TYPE_TEXT_FLAG_MULTI_LINE:0));field.setSingleLine(!multi);WmControls.input(field,font);field.setText(value);if(multi)field.setGravity(Gravity.TOP);return field;}
    private void row(String authored,View field){form.addView(new Row(authored,field),new LinearLayout.LayoutParams(-1,PimFieldRow.HEIGHT));}
    private final class Row extends PimFieldRow {
        private final TextView label;
        private final View field;
        private final WmCommandBar.KeyTouch labelTouch=new WmCommandBar.KeyTouch(false);
        Row(String authored,View field){
            super(WmTaskEditor.this.getContext());this.field=field;label=WmControls.text(getContext(),font,text(authored));captions.put(label,authored);label.setSingleLine(true);label.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);label.setPadding(0,0,0,0);addView(label);addView(field);
            if(field instanceof EditText){EditText input=(EditText)field;input.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,28);input.setPadding(4,0,4,0);android.graphics.drawable.GradientDrawable focused=new android.graphics.drawable.GradientDrawable();focused.setColor(Color.WHITE);focused.setStroke(2,Color.BLACK);android.graphics.drawable.StateListDrawable face=new android.graphics.drawable.StateListDrawable();face.addState(new int[]{android.R.attr.state_focused},focused);face.addState(new int[]{},new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));input.setBackgroundDrawable(face);}
            label.setOnTouchListener(labelTouch);label.setOnClickListener(v->{if(field.isEnabled()){field.requestFocus();field.performClick();}});
        }
        @Override protected void onMeasure(int widthSpec,int heightSpec){int width=MeasureSpec.getSize(widthSpec);label.measure(MeasureSpec.makeMeasureSpec(Math.min(LABEL_RIGHT,width),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(CLIENT,MeasureSpec.EXACTLY));boolean date=field instanceof WmDateField||field instanceof WmDateTimeField;int left=date?EDIT_LEFT:VALUE_LEFT,height=date?CLIENT-2:field instanceof WmChoice?HEIGHT:CLIENT;field.measure(MeasureSpec.makeMeasureSpec(Math.max(0,width-RIGHT_INSET-left),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY));setMeasuredDimension(width,HEIGHT);}
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){label.layout(0,0,label.getMeasuredWidth(),CLIENT);boolean date=field instanceof WmDateField||field instanceof WmDateTimeField;int left=date?EDIT_LEFT:VALUE_LEFT,top=date?2:0;field.layout(left,top,left+field.getMeasuredWidth(),top+field.getMeasuredHeight());}
        @Override protected void onDetachedFromWindow(){labelTouch.cancel(label);super.onDetachedFromWindow();}
    }
    private void choose(WmChoice field,int index){field.chooseDisplayIndex(index);if(field==priority)retainedPriority=1-field.selectedIndex();else if(field==sensitivity)retainedSensitivity=field.selectedIndex();}
    private void openChoice(WmChoice field){
        final int token=generation+1;WmRadioMenu menu=new WmRadioMenu(getContext(),font,field.displayChoices(),field.displaySelectedIndex(),"",accent,index->{if(token==generation&&field.isEnabled()){closePopup();choose(field,index);}});
        menu.enableChoiceKeyboard(field.displaySelectedIndex(),index->{if(token==generation&&field.isEnabled())choose(field,index);},()->{if(token==generation){closePopup();field.requestFocus();}});
        menu.enableChoiceCharacterSearch(field.displaySortLocale(),index->{if(token==generation&&field.isEnabled())field.chooseCharacterDisplayIndex(index);});int height=Math.min(menu.choiceDropdownHeight(256),Math.max(0,getHeight()-WmMetrics.TAB));present(menu.menuHeight()>height?new WmScrollView(getContext(),menu):menu,field,Math.max(160,menu.menuWidth()),height);menu.requestFocus();
    }
    private void openDate(WmDateField field){
        WmDateTimeField.Value initial=field.value();if(initial==null){Calendar today=Calendar.getInstance();initial=new WmDateTimeField.Value(today.get(Calendar.YEAR),today.get(Calendar.MONTH)+1,today.get(Calendar.DAY_OF_MONTH),0,0);}
        final int token=generation+1;WmCalendarPicker picker=new WmCalendarPicker(getContext(),font,bold,locale,initial.year,initial.month,initial.day,accent,(y,m,d)->{if(token==generation){closePopup();field.selectDate(y,m,d);}},new WmDateTimeField.Value(1752,9,14,0,0),new WmDateTimeField.Value(9999,12,31,0,0));
        picker.setOnDateChangeListener((y,m,d)->{if(token==generation&&field.isEnabled())field.selectDate(y,m,d);});
        picker.setOnNoneListener(()->{if(token==generation){closePopup();field.clearDate();}});android.graphics.Point size=picker.naturalSize();present(picker,field,size.x,size.y);
    }
    private void openReminderDate(WmDateTimeField field){
        WmDateTimeField.Value date=field.dateValue();final int token=generation+1;
        WmCalendarPicker picker=new WmCalendarPicker(getContext(),font,bold,locale,date.year,date.month,date.day,accent,(y,m,d)->{if(token==generation&&field.isEnabled()){closePopup();field.selectDate(y,m,d);}},new WmDateTimeField.Value(1752,9,14,0,0),new WmDateTimeField.Value(9999,12,31,0,0));
        picker.setOnDateChangeListener((y,m,d)->{if(token==generation&&field.isEnabled())field.selectDate(y,m,d);});android.graphics.Point size=picker.naturalSize();present(picker,field.dateChoice(),size.x,size.y);
    }
    private void openReminderTime(WmDateTimeField field){
        final int token=generation+1;WmEditableChoice time=field.timeChoice();final String[] choices=time.candidates();int selected=time.dropdownSelectedIndex();
        WmRadioMenu menu=new WmRadioMenu(getContext(),font,choices,selected,"",accent,index->{if(token==generation&&time.isEnabled()){closePopup();time.choose(index);}});
        menu.enableChoiceKeyboard(selected,index->{if(token==generation&&time.isEnabled())time.choose(index);},()->{if(token==generation){closePopup();time.requestFocus();}});
        present(new WmScrollView(getContext(),menu),time,Math.max(time.getWidth(),menu.menuWidth()),menu.choiceDropdownHeight(time.getClosedDropdownClientHeight()));menu.requestFocus();
    }
    private void present(View content,View anchor,int desiredWidth,int desiredHeight){
        closePopup();popup=content;overlay=new FrameLayout(getContext());overlay.setOnClickListener(v->{if(!WmControls.back(popup))closePopup();});content.setClickable(true);View presented=content;
        if(content instanceof WmCalendarPicker){FrameLayout frame=new FrameLayout(getContext());frame.setBackgroundColor(Color.BLACK);frame.setPadding(2,2,2,2);frame.setClickable(true);frame.addView(content,new FrameLayout.LayoutParams(-1,-1));presented=frame;desiredWidth+=4;desiredHeight+=4;}
        int clientHeight=Math.max(0,getHeight()-WmMetrics.TAB),width=Math.max(0,Math.min(getWidth(),desiredWidth)),height=Math.max(0,Math.min(clientHeight,desiredHeight));Rect bounds=new Rect(0,0,anchor.getWidth(),anchor.getHeight());offsetDescendantRectToMyCoords(anchor,bounds);
        int x=Math.max(0,Math.min(getWidth()-width,bounds.left)),y=bounds.bottom;if(y+height>clientHeight)y=bounds.top-height;y=Math.max(0,Math.min(clientHeight-height,y));FrameLayout.LayoutParams position=new FrameLayout.LayoutParams(width,height);position.leftMargin=x;position.topMargin=y;overlay.addView(presented,position);addView(overlay,new FrameLayout.LayoutParams(-1,-1));if(anchor instanceof WmDateField)((WmDateField)anchor).bindDropdown(overlay);else WmChoice.bindDropdown(anchor,overlay);
    }
    public boolean closePopup(){generation++;if(overlay==null)return false;removeView(overlay);overlay=null;popup=null;return true;}
    @Override public boolean back(){return popup!=null&&WmControls.back(popup)||closePopup();}
    @Override protected void onDetachedFromWindow(){closePopup();super.onDetachedFromWindow();}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){font=regular;this.bold=bold==null?regular:bold;accent=selection;closePopup();}
    @Override public void onWmLanguageChanged(String language){closePopup();this.language=WmStrings.language(getContext(),language);locale=WmStrings.locale(this.language);categories.setHint(categoriesAction==null?"":text("없음"));start.setLocale(locale);due.setLocale(locale);reminder.setLocale(locale);reminderMode.setChoices(reminderLabels(),reminderMode.selectedIndex());priority.setChoices(labels("높음","보통","낮음"),priority.selectedIndex());status.setChoices(labels("완료되지 않음","완료"),status.selectedIndex());int selected=sensitivity.selectedIndex();sensitivity.setChoices(labels("보통","개인","비공개","기밀"),selected<0?0:selected);if(selected<0)sensitivity.clearSelection();for(Map.Entry<TextView,String> caption:captions.entrySet())caption.getKey().setText(text(caption.getValue()));sheet.setPageTitle(0,text("작업"));sheet.setPageTitle(1,text("메모"));}
}
