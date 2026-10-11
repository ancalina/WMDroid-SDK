package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Provider-free native recurrence wizard (PIMDLG20020/20200..20202).
 * Native pattern/order/presets are preserved; logical row and popup dimensions
 * remain provisional. Caller owns timezone, RRULE compatibility and saving. */
public final class WmRecurrenceEditor extends FrameLayout implements WmControls.BackHandler,WmControls.RestyleListener,WmControls.LanguageListener {
    public static final class Value {
        public final WmRecurrenceRule rule;public final WmDateTimeField.Value start,until;public final int durationMinutes;public final boolean allDay;
        public Value(WmRecurrenceRule rule,WmDateTimeField.Value start,WmDateTimeField.Value until,int durationMinutes,boolean allDay){if(rule==null||start==null||until==null)throw new IllegalArgumentException("Recurrence values required");this.rule=rule;this.start=start;this.until=until;this.durationMinutes=durationMinutes;this.allDay=allDay;}
    }
    private final WmWizard wizard;
    private final WmBoxSelect pattern,weekdays;
    private final WmDateTimeField start,rangeStart,until;
    private final WmEditableChoice duration,dailyInterval,weeklyInterval,monthlyInterval,monthlyOrdinalInterval,day,yearDay,count;
    private final WmChoice ordinal,group,month,yearMonth,yearOrdinal,yearGroup;
    private final RadioButton dailyEvery,dailyWeekdays,monthlyDay,monthlyOrdinal,yearlyDay,yearlyOrdinal,never,untilRadio,countRadio;
    private final LinearLayout dailyPanel,weeklyPanel,monthlyPanel,yearlyPanel;
    private final Map<TextView,String> captions=new LinkedHashMap<TextView,String>();
    private final boolean allDay;
    private final Value original;private boolean removed,restoring;
    private String language;private Locale locale,durationLocale;private Typeface font,bold;private int accent,generation;private int[] weekdayBits;
    private FrameLayout overlay;private View popup;private Runnable finish,cancel;private String unsupported;
    private final TextView error,timeRange,remove;
    private final FrameLayout timePage,rangePage;private final TextView timeInstruction,timeCaption,durationCaption,rangeInstruction,rangeStartCaption,rangeEndCaption,countSuffix;
    public WmRecurrenceEditor(Context context,Typeface font,Locale locale,Value initial,int accent,int tabColor){
        super(context);if(font==null||locale==null||initial==null)throw new IllegalArgumentException("Editor arguments required");this.font=font;bold=font;this.locale=locale;language=language(locale);durationLocale=locale;this.accent=accent;allDay=initial.allDay;original=initial;
        if(initial.durationMinutes<0||initial.durationMinutes>94370400||(allDay&&(initial.durationMinutes==0||initial.durationMinutes%1440!=0)))throw new IllegalArgumentException("유효한 지속 시간을 입력하십시오.");checkDate(initial.start);checkDate(initial.until);setBackgroundColor(Color.WHITE);
        WmRecurrenceRule r=initial.rule;int p=r.pattern==WmRecurrenceRule.NONE?2:r.pattern==WmRecurrenceRule.DAILY?1:r.pattern==WmRecurrenceRule.WEEKLY?2:r.pattern<=WmRecurrenceRule.MONTHLY_ORDINAL?4:8;
        int defaultMask=1<<(Calendar.getInstance(locale).getFirstDayOfWeek()-1);Calendar date=Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"),Locale.US);date.clear();date.set(initial.start.year,initial.start.month-1,initial.start.day);defaultMask=1<<(date.get(Calendar.DAY_OF_WEEK)-1);
        String initialError=r.interval>999?"되풀이 간격을 표시할 수 없습니다.":r.endMode==1&&r.count>999?"되풀이 횟수를 표시할 수 없습니다.":"";
        pattern=new WmBoxSelect(context,font,patternLabels(),p,false,accent);pattern.setProportionalWidths(true);
        weekdays=new WmBoxSelect(context,font,weekdayLabels(),displayMask(r.pattern==WmRecurrenceRule.WEEKLY?r.dayMask:defaultMask),true,accent);
        start=new WmDateTimeField(context,font,locale,initial.start,accent,true);rangeStart=new WmDateTimeField(context,font,locale,initial.start,accent,true);until=new WmDateTimeField(context,font,locale,initial.until,accent,true);
        duration=number(durationLabels(),durationLabel(initial.durationMinutes),255);dailyInterval=number(new String[]{"1","2","3","4","5","10","20"},Integer.toString(r.pattern==WmRecurrenceRule.DAILY?r.interval:1),3);weeklyInterval=number(new String[]{"1","2","3","4","5","10","20"},Integer.toString(r.pattern==WmRecurrenceRule.WEEKLY?r.interval:1),3);monthlyInterval=number(new String[]{"1","2","3","4","6","12","18"},Integer.toString(r.pattern==3?r.interval:1),3);monthlyOrdinalInterval=number(new String[]{"1","2","3","4","6","12","18"},Integer.toString(r.pattern==4?r.interval:1),3);
        day=number(numbers(31),Integer.toString(r.pattern==3?r.day:initial.start.day),2);yearDay=number(numbers(31),Integer.toString(r.pattern==5?r.day:initial.start.day),2);count=number(new String[]{"1","2","3","4","5","10","20"},Integer.toString(r.endMode==1?r.count:10),3);
        duration.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){}public void afterTextChanged(Editable text){if(!restoring){durationLocale=WmRecurrenceEditor.this.locale;updateTimeRange();}}});
        int ordinalIndex=ordinalIndex(r.ordinal);int groupIndex=groupIndex(r.dayMask);
        if((r.pattern==4||r.pattern==6)&&(ordinalIndex<0||groupIndex<0))unsupported="되풀이 유형을 표시할 수 없습니다.";
        // RCML role6 sends WM_SETFONT: stock VGA outer210 - caption40 - list frame4.
        duration.setClosedDropdownClientHeight(166);count.setClosedDropdownClientHeight(166);
        // Pattern pages skip RCML font delivery and retain creation-time list inflation.
        dailyInterval.setClosedDropdownClientHeight(170);weeklyInterval.setClosedDropdownClientHeight(170);
        monthlyInterval.setClosedDropdownClientHeight(170);monthlyOrdinalInterval.setClosedDropdownClientHeight(170);
        day.setClosedDropdownClientHeight(170);yearDay.setClosedDropdownClientHeight(170);
        ordinal=choice(ordinalLabels(),r.pattern==4?Math.max(0,ordinalIndex):(initial.start.day-1)/7);yearOrdinal=choice(ordinalLabels(),r.pattern==6?Math.max(0,ordinalIndex):(initial.start.day-1)/7);group=choice(groupLabels(),r.pattern==4?Math.max(0,groupIndex):groupIndex(defaultMask));yearGroup=choice(groupLabels(),r.pattern==6?Math.max(0,groupIndex):groupIndex(defaultMask));month=choice(monthLabels(),r.pattern==5?r.month-1:initial.start.month-1);yearMonth=choice(monthLabels(),r.pattern==6?r.month-1:initial.start.month-1);
        dailyEvery=radio("",r.pattern!=1||r.dayMask!=62);dailyWeekdays=radio("주중 매일",r.pattern==1&&r.dayMask==62);monthlyDay=radio("매",r.pattern!=4);monthlyOrdinal=radio("매",r.pattern==4);yearlyDay=radio("매년",r.pattern!=6);yearlyOrdinal=radio("매년",r.pattern==6);never=radio("종료 안 함",r.endMode==0);untilRadio=radio("종료 날짜",r.endMode==2);countRadio=radio("다음 횟수 실행 후 종료",r.endMode==1);
        timePage=new FrameLayout(context){@Override protected void onMeasure(int width,int height){
            // Keep observed DLU origins; larger custom glyphs expand downward.
            placeTime(timeInstruction,20,2,Math.max(1,View.MeasureSpec.getSize(width)-40),-2);timeInstruction.measure(View.MeasureSpec.makeMeasureSpec(Math.max(1,View.MeasureSpec.getSize(width)-40),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
            int timeY=Math.max(80,2+timeInstruction.getMeasuredHeight()+10);placeTime(timeCaption,20,timeY,70,34);placeTime(timeRange,104,timeY,Math.max(1,View.MeasureSpec.getSize(width)-124),34);
            duration.measure(View.MeasureSpec.makeMeasureSpec(210,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));int durationY=Math.max(128,timeY+48);placeTime(durationCaption,20,durationY+8,"ko".equals(language)?156:104,34);placeTime(duration,"ko".equals(language)?184:136,durationY,210,-2);
            int removeY=Math.max(196,durationY+duration.getMeasuredHeight()+26);placeTime(remove,20,removeY,314,42);setMinimumHeight(Math.max(310,removeY+42));super.onMeasure(width,height);
        }};
        timeInstruction=label("언제 약속을 시작하고 종료할지 정하십시오.");timeInstruction.setSingleLine(false);timeCaption=label("시간:");durationCaption=label("지속 시간:");timeRange=WmControls.text(context,font,"");
        for(TextView view:new TextView[]{timeInstruction,timeCaption,durationCaption,timeRange})view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,20);
        placeTime(timeInstruction,20,2,440,-2);placeTime(timeCaption,20,80,70,34);placeTime(timeRange,104,80,350,34);updateTimeRange();placeTime(durationCaption,20,136,"ko".equals(language)?156:104,34);placeTime(duration,"ko".equals(language)?184:136,128,210,-2);
        remove=WmControls.text(context,font,text("되풀이 제거"));captions.put(remove,"되풀이 제거");WmControls.button(remove,accent);remove.setFocusableInTouchMode(true);placeTime(remove,20,196,314,42);remove.setOnClickListener(v->{removed=true;pattern.setSelectedMask(0);closePopup();if(finish!=null)finish.run();});
        LinearLayout typePage=page();typePage.addView(pattern,new LinearLayout.LayoutParams(-1,48));
        dailyPanel=page();line(dailyPanel,dailyEvery,40,dailyInterval,88,label("일마다"),-1);line(dailyPanel,dailyWeekdays,-1);typePage.addView(dailyPanel);
        weeklyPanel=page();line(weeklyPanel,weeklyInterval,88,label("주마다"),-1);weeklyPanel.addView(weekdays,new LinearLayout.LayoutParams(-1,48));typePage.addView(weeklyPanel);
        monthlyPanel=page();line(monthlyPanel,monthlyDay,84,monthlyInterval,88,label("개월마다"),-1);line(monthlyPanel,label(""),40,day,88,label("일에"),-1);line(monthlyPanel,monthlyOrdinal,84,monthlyOrdinalInterval,88,label("개월마다"),-1);line(monthlyPanel,label(""),40,ordinal,140,group,-1,label("에"),40);typePage.addView(monthlyPanel);
        yearlyPanel=page();line(yearlyPanel,yearlyDay,112,month,-1,yearDay,72,label("일에"),52);line(yearlyPanel,yearlyOrdinal,112,yearMonth,-1);line(yearlyPanel,label(""),40,yearOrdinal,140,yearGroup,-1,label("에"),40);typePage.addView(yearlyPanel);
        rangeInstruction=label("되풀이 시작/종료 날짜를 선택하십시오.");rangeInstruction.setSingleLine(false);rangeStartCaption=label("되풀이 시작");rangeEndCaption=label("되풀이 종료");countSuffix=label("calendar.recurrence.occurrences");
        for(TextView view:new TextView[]{rangeInstruction,rangeStartCaption,rangeEndCaption,never,untilRadio,countRadio,countSuffix})view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,20);
        // Owner supplies real bold faces during paired restyle; a one-face caller keeps its supplied face.
        WmControls.lockFont(rangeStartCaption);WmControls.lockFont(rangeEndCaption);
        rangeStart.removeView(rangeStart.dateChoice());until.removeView(until.dateChoice());rangeStart.dateChoice().setBordered(true);until.dateChoice().setBordered(true);
        rangePage=new FrameLayout(context){@Override protected void onMeasure(int width,int height){
            boolean ko="ko".equals(language);int available=View.MeasureSpec.getSize(width),labelX=ko?20:16;
            rangeInstruction.measure(View.MeasureSpec.makeMeasureSpec(Math.max(1,available-32),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
            int shift=Math.max(0,rangeInstruction.getMeasuredHeight()-34),headingHeight=Math.max(34,textHeight(rangeStartCaption));
            placeRange(rangeInstruction,16,0,available-32,Math.max(34,rangeInstruction.getMeasuredHeight()));
            placeRange(rangeStartCaption,labelX,34+shift,available-labelX,headingHeight);shift+=headingHeight-34;
            int dateHeight=rangeDateHeight(rangeStart.dateChoice());
            placeRange(rangeStart.dateChoice(),ko?16:14,70+shift,ko?336:294,dateHeight);shift+=dateHeight-44;
            headingHeight=Math.max(34,textHeight(rangeEndCaption));placeRange(rangeEndCaption,labelX,118+shift,available-labelX,headingHeight);shift+=headingHeight-34;
            int radioHeight=Math.max(34,textHeight(never));placeRange(never,ko?24:20,154+shift,ko?318:280,radioHeight);shift+=radioHeight-34;
            dateHeight=rangeDateHeight(until.dateChoice());radioHeight=Math.max(ko?44:34,textHeight(untilRadio));
            placeRange(untilRadio,ko?24:20,(ko?202:196)+shift,ko?160:140,radioHeight);placeRange(until.dateChoice(),ko?198:174,(ko?196:192)+shift,ko?266:294,dateHeight);shift+=Math.max(dateHeight-44,radioHeight-(ko?44:34));
            int countHeight=Math.max(34,textHeight(count)+8),countY=(ko?258:238)+shift;
            placeRange(count,ko?318:188,countY,ko?112:98,countHeight);
            radioHeight=Math.max(ko?42:34,textHeight(countRadio));placeRange(countRadio,ko?24:20,(ko?258:240)+shift,ko?290:156,radioHeight);placeRange(countSuffix,ko?436:294,(ko?258:240)+shift,ko?44:174,Math.max(ko?42:34,textHeight(countSuffix)));
            setMinimumHeight(Math.max(310,countY+Math.max(countHeight,radioHeight)));super.onMeasure(width,height);
        }};
        for(View view:new View[]{rangeInstruction,rangeStartCaption,rangeStart.dateChoice(),rangeEndCaption,never,untilRadio,until.dateChoice(),countRadio,count,countSuffix})rangePage.addView(view,new LayoutParams(1,1));
        wizard=new WmWizard(context,font,locale,accent);if(!allDay)wizard.addPage(new WmScrollView(context,timePage));wizard.addPage(new WmScrollView(context,typePage));wizard.addPage(new WmScrollView(context,rangePage));addView(wizard,new LayoutParams(-1,-1));error=wizard.errorField();captions.put(error,unsupported==null?initialError:unsupported);wizard.setError(text(unsupported==null?initialError:unsupported));
        wizard.setOnPageChangedListener(index->{closePopup();wizard.setFinishHere(index==(allDay?0:1)&&pattern.selectedMask()==0);});wizard.setOnCancel(()->{closePopup();if(cancel!=null)cancel.run();});wizard.setOnFinish(()->{try{value();showError("");closePopup();if(finish!=null)finish.run();}catch(IllegalArgumentException invalid){showError(invalid.getMessage());}});
        wizard.setNavigationGate((from,to)->{try{if(to>from){if(!allDay&&from==0){checkDate(start.getValue());durationMinutes();}else snapshot(false);showError("");}return true;}catch(IllegalArgumentException invalid){showError(invalid.getMessage());return false;}});
        pair(dailyEvery,dailyWeekdays);pair(monthlyDay,monthlyOrdinal);pair(yearlyDay,yearlyOrdinal);never.setOnClickListener(v->rangeMode(never));untilRadio.setOnClickListener(v->rangeMode(untilRadio));countRadio.setOnClickListener(v->rangeMode(countRadio));
        pattern.setOnSelectionListener(mask->{removed=false;closePopup();unsupported=null;showError("");updateEnabled();wizard.setFinishHere(false);});start.setOnValueChangedListener((oldValue,newValue)->{if(restoring)return;if(!rangeStart.dateValue().equals(newValue))rangeStart.setValue(newValue);updateTimeRange();});rangeStart.setOnValueChangedListener((oldValue,newValue)->{
            if(restoring)return;start.selectDate(newValue.year,newValue.month,newValue.day);
            WmDateTimeField.Value end=until.dateValue();
            // Native Range uses silent DTM_SETSYSTEMTIME; retain the selected end mode.
            if(compareDate(newValue,end)>0)until.setValue(new WmDateTimeField.Value(newValue.year,newValue.month,newValue.day,end.hour,end.minute));
        });
        until.setOnValueChangedListener((oldValue,newValue)->{if(!restoring)rangeMode(untilRadio,false);});count.setOnValueChangedListener(raw->{if(!restoring)rangeMode(countRadio,false);});
        start.timeChoice().addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){}public void afterTextChanged(Editable text){if(!restoring)updateTimeRange();}});rangeStart.setOnDateOpen(this::openDate);until.setOnDateOpen(this::openDate);updateEnabled();
    }
    public WmWizard wizard(){return wizard;}public WmBoxSelect patternSelector(){return pattern;}public WmBoxSelect weekdaySelector(){return weekdays;}
    /** Owned backing start value; Time page shows a caption instead of this View.
     * Do not move it to another parent; follow owned-view notification contracts. */
    public WmDateTimeField startField(){return start;}public WmDateTimeField rangeStartField(){return rangeStart;}public WmDateTimeField untilField(){return until;}
    public WmEditableChoice durationField(){return duration;}public WmEditableChoice intervalField(){return pattern.selectedMask()==1?dailyInterval:pattern.selectedMask()==2?weeklyInterval:monthlyIntervalField();}public WmEditableChoice dailyIntervalField(){return dailyInterval;}public WmEditableChoice weeklyIntervalField(){return weeklyInterval;}public WmEditableChoice monthlyIntervalField(){return monthlyOrdinal.isChecked()?monthlyOrdinalInterval:monthlyInterval;}public WmEditableChoice monthlyDayIntervalField(){return monthlyInterval;}public WmEditableChoice monthlyOrdinalIntervalField(){return monthlyOrdinalInterval;}public WmEditableChoice countField(){return count;}public WmEditableChoice dayField(){return day;}public WmEditableChoice yearDayField(){return yearDay;}
    public WmChoice ordinalSelector(){return ordinal;}public WmChoice groupSelector(){return group;}public WmChoice monthSelector(){return yearlyOrdinal.isChecked()?yearMonth:month;}public WmChoice yearlyDayMonthSelector(){return month;}public WmChoice yearlyOrdinalMonthSelector(){return yearMonth;}public WmChoice yearOrdinalSelector(){return yearOrdinal;}public WmChoice yearGroupSelector(){return yearGroup;}
    public RadioButton dailyIntervalRadio(){return dailyEvery;}public RadioButton dailyWeekdaysRadio(){return dailyWeekdays;}public RadioButton monthlyDayRadio(){return monthlyDay;}public RadioButton monthlyOrdinalRadio(){return monthlyOrdinal;}public RadioButton yearlyDayRadio(){return yearlyDay;}public RadioButton yearlyOrdinalRadio(){return yearlyOrdinal;}public RadioButton neverRadio(){return never;}public RadioButton untilRadio(){return untilRadio;}public RadioButton countRadio(){return countRadio;}
    public void setOnFinish(Runnable listener){finish=listener;}public void setOnCancel(Runnable listener){cancel=listener;}
    public Value value(){return snapshot(true);}
    private static final String[] NUMBER_KEYS={"duration","daily","weekly","monthly_day","monthly_ordinal","day","year_day","count"};
    private WmEditableChoice[] numberFields(){return new WmEditableChoice[]{duration,dailyInterval,weeklyInterval,monthlyInterval,monthlyOrdinalInterval,day,yearDay,count};}
    private WmChoice[] choiceFields(){return new WmChoice[]{ordinal,group,month,yearMonth,yearOrdinal,yearGroup};}
    private RadioButton[] radioFields(){return new RadioButton[]{dailyEvery,dailyWeekdays,monthlyDay,monthlyOrdinal,yearlyDay,yearlyOrdinal,never,untilRadio,countRadio};}
    private View[] focusViews(){
        java.util.ArrayList<View> views=new java.util.ArrayList<>();for(View field:numberFields())views.add(field);for(View field:choiceFields())views.add(field);views.add(start.timeChoice());views.add(rangeStart.dateChoice());views.add(until.dateChoice());for(int i=0;i<4;i++)views.add(pattern.cell(i));
        // Weekday focus follows its model bit across locale-dependent display ordering.
        for(int bit=0;bit<7;bit++)for(int i=0;i<7;i++)if(weekdayBits[i]==1<<bit)views.add(weekdays.cell(i));for(View field:radioFields())views.add(field);views.add(wizard.cancelButton());views.add(wizard.backButton());views.add(wizard.nextButton());views.add(wizard.finishButton());views.add(remove);return views.toArray(new View[0]);
    }
    /** No parsing/blur commit. Nested popups and IME composition are not included. */
    public Bundle saveInteractionState(){
        Bundle state=new Bundle();state.putInt("schema",1);Bundle seed=new Bundle();seed.putIntArray("rule",WmAppointmentEditor.ruleParts(original.rule));seed.putIntArray("start",WmAppointmentEditor.dateParts(original.start));seed.putIntArray("until",WmAppointmentEditor.dateParts(original.until));seed.putInt("duration",original.durationMinutes);seed.putBoolean("all_day",allDay);state.putBundle("original",seed);
        state.putBundle("wizard",wizard.saveInteractionState());state.putBundle("start",start.saveInteractionState());state.putBundle("range_start",rangeStart.saveInteractionState());state.putBundle("until",until.saveInteractionState());Bundle numbers=new Bundle();WmEditableChoice[] fields=numberFields();for(int i=0;i<fields.length;i++){Bundle item=new Bundle();item.putString("text",fields[i].value());item.putStringArray("candidates",fields[i].candidates());item.putBundle("interaction",fields[i].saveInteractionState());numbers.putBundle(NUMBER_KEYS[i],item);}state.putBundle("numbers",numbers);
        WmChoice[] choices=choiceFields();int[] selected=new int[choices.length];for(int i=0;i<selected.length;i++)selected[i]=choices[i].selectedIndex();state.putIntArray("choices",selected);RadioButton[] radios=radioFields();boolean[] checked=new boolean[radios.length];for(int i=0;i<checked.length;i++)checked[i]=radios[i].isChecked();state.putBooleanArray("radios",checked);state.putInt("pattern",pattern.selectedMask());state.putInt("weekdays",modelMask());state.putBoolean("removed",removed);state.putSerializable("duration_locale",durationLocale);state.putString("unsupported",unsupported);state.putString("error",captions.get(error));int focus=-1;View[] focusViews=focusViews();for(int i=0;i<focusViews.length;i++)if(focusViews[i].hasFocus()){focus=i;break;}state.putInt("focus",focus);return state;
    }
    /** Seed for a new editor; full draft validation still belongs to the constructed editor. */
    public static Value initialValue(Bundle state){
        if(state==null||state.size()!=16||!(state.get("schema") instanceof Integer)||state.getInt("schema")!=1||!(state.get("original") instanceof Bundle))throw new IllegalArgumentException("Recurrence interaction schema1 required");Bundle seed=state.getBundle("original");if(seed.size()!=5||!(seed.get("duration") instanceof Integer)||!(seed.get("all_day") instanceof Boolean))throw new IllegalArgumentException("Recurrence seed required");Value value=new Value(WmAppointmentEditor.stateRule(seed),WmAppointmentEditor.stateDate(seed,"start"),WmAppointmentEditor.stateDate(seed,"until"),seed.getInt("duration"),seed.getBoolean("all_day"));checkDate(value.start);checkDate(value.until);if(value.durationMinutes<0||value.durationMinutes>94370400||value.allDay&&(value.durationMinutes==0||value.durationMinutes%1440!=0))throw new IllegalArgumentException("Invalid recurrence seed duration");return value;
    }
    public void validateInteractionState(Bundle state){
        Value seed=initialValue(state);if(!original.rule.equals(seed.rule)||!original.start.equals(seed.start)||!original.until.equals(seed.until)||original.durationMinutes!=seed.durationMinutes||allDay!=seed.allDay)throw new IllegalArgumentException("Recurrence origin changed");
        for(String key:new String[]{"wizard","start","range_start","until","numbers"})if(!(state.get(key) instanceof Bundle))throw new IllegalArgumentException("Recurrence child states required");wizard.validateInteractionState(state.getBundle("wizard"));start.validateInteractionState(state.getBundle("start"));rangeStart.validateInteractionState(state.getBundle("range_start"));until.validateInteractionState(state.getBundle("until"));
        if(!(state.get("choices") instanceof int[])||!(state.get("radios") instanceof boolean[])||!(state.get("pattern") instanceof Integer)||!(state.get("weekdays") instanceof Integer)||!(state.get("removed") instanceof Boolean)||!(state.get("duration_locale") instanceof Locale)||!state.containsKey("unsupported")||state.get("unsupported")!=null&&!(state.get("unsupported") instanceof String)||!(state.get("error") instanceof String)||!(state.get("focus") instanceof Integer))throw new IllegalArgumentException("Recurrence fields required");
        int pattern=state.getInt("pattern"),mask=state.getInt("weekdays"),focus=state.getInt("focus");if(pattern!=0&&pattern!=1&&pattern!=2&&pattern!=4&&pattern!=8||mask<0||mask>127||focus< -1||focus>=focusViews().length||state.getBoolean("removed")&&pattern!=0)throw new IllegalArgumentException("Invalid recurrence mask/focus");
        WmChoice[] choices=choiceFields();int[] selected=state.getIntArray("choices");if(selected.length!=choices.length||state.getBooleanArray("radios").length!=radioFields().length)throw new IllegalArgumentException("Recurrence selections required");for(int i=0;i<selected.length;i++)if(selected[i]< -1||selected[i]>=choices[i].choices().length)throw new IllegalArgumentException("Invalid recurrence choice");
        Bundle numbers=state.getBundle("numbers");if(numbers.size()!=NUMBER_KEYS.length)throw new IllegalArgumentException("Recurrence numeric drafts required");int[] limits={255,3,3,3,3,2,2,3};for(int i=0;i<NUMBER_KEYS.length;i++){if(!(numbers.get(NUMBER_KEYS[i]) instanceof Bundle))throw new IllegalArgumentException("Numeric draft required");Bundle item=numbers.getBundle(NUMBER_KEYS[i]);if(item.size()!=3||!(item.get("text") instanceof String)||item.getString("text").length()>limits[i]||!(item.get("candidates") instanceof String[])||!(item.get("interaction") instanceof Bundle))throw new IllegalArgumentException("Invalid numeric draft");String[] candidates=item.getStringArray("candidates");if(candidates.length==0)throw new IllegalArgumentException("Numeric candidates required");for(String candidate:candidates)if(candidate==null)throw new IllegalArgumentException("Null numeric candidate");WmEditableChoice.validateInteractionState(item.getBundle("interaction"),candidates.length,item.getString("text").length());}
        String[] keys={"start","range_start","until"};for(int i=0;i<keys.length;i++)if(!state.getBundle(keys[i]).getString("focus").equals(focus==14+i?(i==0?"time":"date"):"none"))throw new IllegalArgumentException("Inconsistent recurrence focus");
    }
    /** Silent closed-popup restoration; validation precedes every owner mutation. */
    public void restoreInteractionState(Bundle state){
        if(restoring)throw new IllegalStateException("Reentrant recurrence restoration");validateInteractionState(state);if(popup!=null)throw new IllegalStateException("Close recurrence popups before restoring");for(WmEditableChoice field:numberFields())if(field.isDroppedDown())throw new IllegalStateException("Close numeric popup before restoring");for(WmChoice field:choiceFields())if(field.isDroppedDown())throw new IllegalStateException("Close choice popup before restoring");for(WmDateTimeField field:new WmDateTimeField[]{start,rangeStart,until})if(field.dateChoice().isDroppedDown()||field.timeChoice().isDroppedDown())throw new IllegalStateException("Close date/time popup before restoring");
        restoring=true;try{
            wizard.restoreInteractionState(state.getBundle("wizard"));pattern.setSelectedMask(state.getInt("pattern"));weekdays.setSelectedMask(displayMask(state.getInt("weekdays")));WmChoice[] choices=choiceFields();int[] selected=state.getIntArray("choices");for(int i=0;i<choices.length;i++)if(selected[i]<0)choices[i].clearSelection();else choices[i].setSelectedIndex(selected[i]);RadioButton[] radios=radioFields();boolean[] checked=state.getBooleanArray("radios");for(int i=0;i<radios.length;i++)radios[i].setChecked(checked[i]);removed=state.getBoolean("removed");unsupported=state.getString("unsupported");updateEnabled();start.restoreInteractionState(state.getBundle("start"));rangeStart.restoreInteractionState(state.getBundle("range_start"));until.restoreInteractionState(state.getBundle("until"));
            Bundle numbers=state.getBundle("numbers");WmEditableChoice[] fields=numberFields();for(int i=0;i<fields.length;i++){Bundle item=numbers.getBundle(NUMBER_KEYS[i]);fields[i].setCandidates(item.getStringArray("candidates"));fields[i].setValue(item.getString("text"));fields[i].restoreInteractionState(item.getBundle("interaction"));}durationLocale=(Locale)state.get("duration_locale");captions.put(error,state.getString("error"));wizard.setError(text(state.getString("error")));int focus=state.getInt("focus");if(focus<0)clearFocus();else focusViews()[focus].requestFocus();
        }finally{restoring=false;}updateTimeRange();
    }
    private Value snapshot(boolean validateRange){
        if(removed)return new Value(WmRecurrenceRule.none(),original.start,original.until,original.durationMinutes,allDay);
        WmDateTimeField.Value date=rangeStart.dateValue(),clock=allDay?date:start.getValue(),s=new WmDateTimeField.Value(date.year,date.month,date.day,clock.hour,clock.minute),u=until.dateValue();checkDate(s);int minutes=durationMinutes();if(allDay&&(minutes==0||minutes%1440!=0))throw new IllegalArgumentException("유효한 지속 시간을 입력하십시오.");
        int p=pattern.selectedMask();WmRecurrenceRule rule;if(p==0)rule=WmRecurrenceRule.none();else{if(unsupported!=null)throw new IllegalArgumentException(unsupported);
            if(p==1)rule=WmRecurrenceRule.daily(dailyWeekdays.isChecked()?1:interval(dailyInterval.value()),dailyWeekdays.isChecked());else if(p==2)rule=WmRecurrenceRule.weekly(interval(weeklyInterval.value()),modelMask());else if(p==4)rule=monthlyDay.isChecked()?WmRecurrenceRule.monthlyDay(interval(monthlyInterval.value()),positive(day.value())):WmRecurrenceRule.monthlyOrdinal(interval(monthlyOrdinalInterval.value()),ordinalValue(ordinal.selectedIndex()),groupValue(group.selectedIndex()));else rule=yearlyDay.isChecked()?WmRecurrenceRule.yearlyDay(month.selectedIndex()+1,positive(yearDay.value())):WmRecurrenceRule.yearlyOrdinal(yearMonth.selectedIndex()+1,ordinalValue(yearOrdinal.selectedIndex()),groupValue(yearGroup.selectedIndex()));
            if(validateRange&&countRadio.isChecked()){if(count.value().length()>3)throw new IllegalArgumentException("되풀이 횟수를 표시할 수 없습니다.");rule=rule.withCount(positive(count.value()));}else if(validateRange&&untilRadio.isChecked()){checkDate(u);if(compareDate(u,s)<0)throw new IllegalArgumentException("종료 날짜는 시작 날짜보다 빠를 수 없습니다.");rule=rule.withUntil();}}
        if(validateRange)rule.validateAppointmentDuration((long)minutes*60000L);
        return new Value(rule,s,u,minutes,allDay);
    }
    // PIMDLG25205 is STATIC; resource4062 joins two TIME_NOSECONDS captions.
    // Invalid duration drafts retain the last valid range, as the native commit does.
    private void updateTimeRange(){
        try{
            int minutes=durationMinutes();WmDateTimeField.Value value=start.getValue();
            Calendar clock=Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"),Locale.US);clock.clear();clock.set(2000,0,1,value.hour,value.minute,0);
            java.text.SimpleDateFormat format=new java.text.SimpleDateFormat(WmTimeFormat.minutePattern(locale,android.text.format.DateFormat.is24HourFormat(getContext())),locale);format.setTimeZone(clock.getTimeZone());
            String first=format.format(clock.getTime());clock.add(Calendar.MINUTE,minutes);timeRange.setText(first+" - "+format.format(clock.getTime()));
        }catch(IllegalArgumentException invalid){}
    }
    private int durationMinutes(){return WmCalendarDuration.parseEditorMinutes(duration.value(),durationLocale);}
    private static int interval(String raw){if(raw.length()>3)throw new IllegalArgumentException("되풀이 간격을 표시할 수 없습니다.");return positive(raw);}
    private static int positive(String raw){if(raw.length()==0)throw new IllegalArgumentException("이 필드에는 양의 정수를 입력해야 합니다.");int result=0;for(int i=0;i<raw.length();i++){char c=raw.charAt(i);if(c<'0'||c>'9'||result>(Integer.MAX_VALUE-(c-'0'))/10)throw new IllegalArgumentException("이 필드에는 양의 정수를 입력해야 합니다.");result=result*10+c-'0';}if(result<=0)throw new IllegalArgumentException("이 필드에는 양의 정수를 입력해야 합니다.");return result;}
    private static int compareDate(WmDateTimeField.Value a,WmDateTimeField.Value b){if(a.year!=b.year)return a.year<b.year?-1:1;if(a.month!=b.month)return a.month<b.month?-1:1;return a.day-b.day;}
    private static void checkDate(WmDateTimeField.Value v){if(v.year<1900||v.year>2999)throw new IllegalArgumentException("날짜는 1900년부터 2999년까지 선택할 수 있습니다.");}
    private void rangeMode(RadioButton chosen){rangeMode(chosen,true);}
    private void rangeMode(RadioButton chosen,boolean dismiss){never.setChecked(chosen==never);untilRadio.setChecked(chosen==untilRadio);countRadio.setChecked(chosen==countRadio);if(dismiss)closePopup();updateEnabled();}
    private void pair(RadioButton a,RadioButton b){a.setOnClickListener(v->{a.setChecked(true);b.setChecked(false);closePopup();updateEnabled();});b.setOnClickListener(v->{a.setChecked(false);b.setChecked(true);closePopup();updateEnabled();});}
    private void updateEnabled(){int p=pattern.selectedMask();dailyPanel.setVisibility(p==1?VISIBLE:GONE);weeklyPanel.setVisibility(p==2?VISIBLE:GONE);monthlyPanel.setVisibility(p==4?VISIBLE:GONE);yearlyPanel.setVisibility(p==8?VISIBLE:GONE);dailyInterval.setEnabled(dailyEvery.isChecked());day.setEnabled(monthlyDay.isChecked());monthlyInterval.setEnabled(monthlyDay.isChecked());monthlyOrdinalInterval.setEnabled(monthlyOrdinal.isChecked());ordinal.setEnabled(monthlyOrdinal.isChecked());group.setEnabled(monthlyOrdinal.isChecked());yearDay.setEnabled(yearlyDay.isChecked());month.setEnabled(yearlyDay.isChecked());yearMonth.setEnabled(yearlyOrdinal.isChecked());yearOrdinal.setEnabled(yearlyOrdinal.isChecked());yearGroup.setEnabled(yearlyOrdinal.isChecked());until.dateChoice().setEnabled(true);count.setEnabled(true);}
    private void placeTime(View view,int x,int y,int width,int height){boolean attached=view.getParent()==timePage;FrameLayout.LayoutParams bounds=attached?(FrameLayout.LayoutParams)view.getLayoutParams():new FrameLayout.LayoutParams(width,height);if(bounds.width!=width||bounds.height!=height||bounds.leftMargin!=x||bounds.topMargin!=y){bounds.width=width;bounds.height=height;bounds.leftMargin=x;bounds.topMargin=y;view.forceLayout();timePage.forceLayout();}if(!attached)timePage.addView(view,bounds);}
    private static int rangeDateHeight(WmDateField view){view.measure(View.MeasureSpec.makeMeasureSpec(336,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));return Math.max(44,view.getMeasuredHeight());}
    private static int textHeight(TextView view){android.graphics.Paint.FontMetricsInt metrics=view.getPaint().getFontMetricsInt();return metrics.descent-metrics.ascent;}
    private void placeRange(View view,int x,int y,int width,int height){FrameLayout.LayoutParams bounds=(FrameLayout.LayoutParams)view.getLayoutParams();if(bounds.width!=width||bounds.height!=height||bounds.leftMargin!=x||bounds.topMargin!=y){bounds.width=Math.max(1,width);bounds.height=height;bounds.leftMargin=x;bounds.topMargin=y;view.forceLayout();rangePage.forceLayout();}}
    private LinearLayout page(){LinearLayout p=new LinearLayout(getContext());p.setOrientation(LinearLayout.VERTICAL);p.setPadding(8,8,8,0);return p;}
    private TextView label(String authored){TextView label=WmControls.text(getContext(),font,text(authored));captions.put(label,authored);label.setGravity(Gravity.CENTER_VERTICAL);label.setSingleLine(true);return label;}
    /** Explicit dialog widths/minima are provisional VGA; shared controls own font-driven height. */
    private void line(LinearLayout page,Object... pieces){LinearLayout line=new LinearLayout(getContext());line.setGravity(Gravity.CENTER_VERTICAL);line.setPadding(0,2,0,2);for(int i=0;i<pieces.length;i+=2){View view=(View)pieces[i];int width=((Integer)pieces[i+1]).intValue();view.setMinimumHeight(44);LinearLayout.LayoutParams lp=width<0?new LinearLayout.LayoutParams(0,-2,1):new LinearLayout.LayoutParams(width,-2);if(width>=0&&captions.containsKey(view)){view.setMinimumWidth(width);lp.width=-2;}lp.rightMargin=4;line.addView(view,lp);}page.addView(line,new LinearLayout.LayoutParams(-1,-2));}
    private TextView caption(LinearLayout page,String authored){TextView label=WmControls.text(getContext(),font,text(authored));captions.put(label,authored);page.addView(label,new LinearLayout.LayoutParams(-1,-2));return label;}
    private RadioButton radio(String authored,boolean checked){RadioButton button=new RadioButton(getContext());WmControls.radio(button,font);button.setSingleLine(true);button.setText(text(authored));button.setChecked(checked);captions.put(button,authored);return button;}
    private WmEditableChoice number(String[] candidates,String initial,int limit){WmEditableChoice field=new WmEditableChoice(getContext(),font,candidates,initial,accent);field.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);if(limit>0)field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(limit)});field.setOnOpenListener(this::openNumber);return field;}
    private WmChoice choice(String[] labels,int selected){WmChoice field=new WmChoice(getContext(),font,labels,selected,accent);field.setOnOpenListener(this::openChoice);return field;}
    private String language(Locale locale){return WmStrings.language(getContext(),android.os.Build.VERSION.SDK_INT>=21?Modern.tag(locale):locale.toString().replace('_','-'));}
    private static final class Modern {static String tag(Locale locale){return locale.toLanguageTag();}}
    /** Reuse locale changes: retain typed duration grammar and weekday model mask. */
    @Override public void onWmLanguageChanged(String language){setLocale(WmStrings.locale(WmStrings.language(getContext(),language)));}
    private String text(String s){return WmStrings.text(getContext(),language,s);}
    private String[] patternLabels(){return new String[]{text("매일"),text("매주"),text("매월"),text("매년")};}
    private String[] weekdayLabels(){String[] raw=new DateFormatSymbols(locale).getWeekdays(),labels=new String[7];weekdayBits=new int[7];int first=Calendar.getInstance(locale).getFirstDayOfWeek()-1;for(int i=0;i<7;i++){int day=(first+i)%7;labels[i]=raw[day+1].substring("zh".equals(locale.getLanguage())&&raw[day+1].length()>=3?2:0,"zh".equals(locale.getLanguage())&&raw[day+1].length()>=3?3:1);weekdayBits[i]=1<<day;}return labels;}
    private int displayMask(int model){int result=0;for(int i=0;i<7;i++)if((model&weekdayBits[i])!=0)result|=1<<i;return result;}
    private int modelMask(){int result=0;for(int i=0;i<7;i++)if((weekdays.selectedMask()&(1<<i))!=0)result|=weekdayBits[i];return result;}
    private String[] ordinalLabels(){return new String[]{text("첫째"),text("둘째"),text("셋째"),text("넷째"),text("마지막")};}
    private String[] groupLabels(){String[] raw=new DateFormatSymbols(locale).getWeekdays(),result=new String[10];result[0]=text("날");result[1]=text("평일");result[2]=text("주말");for(int i=0;i<7;i++)result[i+3]=raw[i+1];return result;}
    private String[] monthLabels(){String[] raw=new DateFormatSymbols(locale).getMonths(),result=new String[12];System.arraycopy(raw,0,result,0,12);return result;}
    private static int ordinalValue(int i){return i==4?-1:i+1;}private static int ordinalIndex(int v){return v==-1?4:v>=1&&v<=4?v-1:-1;}
    private static int groupValue(int i){return i==0?127:i==1?62:i==2?65:1<<(i-3);}private static int groupIndex(int mask){if(mask==127)return 0;if(mask==62)return 1;if(mask==65)return 2;for(int i=0;i<7;i++)if(mask==(1<<i))return i+3;return -1;}
    private static String[] numbers(int n){String[] result=new String[n];for(int i=0;i<n;i++)result[i]=Integer.toString(i+1);return result;}
    private String durationLabel(int minutes){if(minutes==30)return text("30분");if(minutes>=60&&minutes<=1410&&minutes%30==0){java.text.NumberFormat format=java.text.NumberFormat.getNumberInstance(locale);format.setGroupingUsed(false);format.setMaximumFractionDigits(1);format.setMinimumFractionDigits(0);return format.format(minutes/60.0)+text("시간");}return Integer.toString(minutes)+" "+text("분");}
    private String[] durationLabels(){String[] result=new String[47];for(int i=0;i<47;i++)result[i]=durationLabel((i+1)*30);return result;}
    public void setLocale(Locale next){if(next==null)throw new IllegalArgumentException("Locale required");if(next.equals(locale))return;int mask=modelMask();String old=duration.value();int durationValue=-1;try{durationValue=durationMinutes();}catch(IllegalArgumentException ignored){}boolean localizedDuration=durationValue>=0&&old.equals(durationLabel(durationValue));int caretStart=duration.getSelectionStart(),caretEnd=duration.getSelectionEnd();locale=next;language=language(next);if(popup instanceof WmMessageBox)((WmMessageBox)popup).setLocale(next);else closePopup();start.setLocale(next);rangeStart.setLocale(next);until.setLocale(next);wizard.setLocale(next);pattern.setChoices(patternLabels(),pattern.selectedMask());weekdays.setChoices(weekdayLabels(),displayMask(mask));ordinal.setChoices(ordinalLabels(),ordinal.selectedIndex());yearOrdinal.setChoices(ordinalLabels(),yearOrdinal.selectedIndex());group.setChoices(groupLabels(),group.selectedIndex());yearGroup.setChoices(groupLabels(),yearGroup.selectedIndex());month.setChoices(monthLabels(),month.selectedIndex());yearMonth.setChoices(monthLabels(),yearMonth.selectedIndex());duration.setCandidates(durationLabels());if(localizedDuration){duration.setValue(durationLabel(durationValue));if(caretStart>=0&&caretEnd>=0)duration.setSelection(Math.min(caretStart,duration.length()),Math.min(caretEnd,duration.length()));}for(Map.Entry<TextView,String> c:captions.entrySet())c.getKey().setText(text(c.getValue()));wizard.setError(error.getText());if(popup instanceof WmMessageBox)error.setVisibility(GONE);updateTimeRange();timePage.requestLayout();}
    /** Maps known validation errors to guidance. Other nonempty strings without
     * Hangul syllables use generic recurrence guidance; null/empty clears.
     * OK acknowledgment restores previous input focus/caret. */
    public void setError(String authored){showError(authored);}
    private void showError(String s){
        String authored=s==null?"":s;
        if("종료 날짜는 시작 날짜보다 빠를 수 없습니다.".equals(authored))authored="저장할 수 없습니다. 끝 날짜를 시작 날짜 후로 선택해야 합니다.";
        else if("Day outside1..31".equals(authored))authored="31보다 작거나 같은 숫자를 입력하십시오.";
        else if("Impossible month/day".equals(authored))authored="이 달에 날짜 수가 너무 많습니다. 날짜 수를 줄이십시오.";
        else if("Invalid weekday mask".equals(authored))authored="요일을 하나 이상 선택하세요.";
        else if(authored.length()>0&&!containsHangul(authored))authored="올바른 되풀이 값을 입력하세요.";
        captions.put(error,authored);wizard.setError(text(authored));
        if(authored.length()==0){if(popup instanceof WmMessageBox)closePopup();return;}
        error.setVisibility(GONE);
        View focus=findFocus();
        if("유효한 지속 시간을 입력하십시오.".equals(authored))focus=duration;
        else if("저장할 수 없습니다. 끝 날짜를 시작 날짜 후로 선택해야 합니다.".equals(authored))focus=until.dateChoice();
        final View restore=focus;final int caretStart=restore instanceof android.widget.EditText?((android.widget.EditText)restore).getSelectionStart():-1,caretEnd=restore instanceof android.widget.EditText?((android.widget.EditText)restore).getSelectionEnd():-1;
        android.view.inputmethod.InputMethodManager input=(android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);if(input!=null)input.hideSoftInputFromWindow(getWindowToken(),0);
        closePopup();final int token=generation;
        WmMessageBox box=new WmMessageBox(getContext(),font,locale,"일정",authored,accent,()->{if(token!=generation)return;closePopup();if(restore!=null&&restore.isShown()){restore.requestFocus();if(restore instanceof android.widget.EditText&&caretStart>=0&&caretEnd>=0){android.widget.EditText field=(android.widget.EditText)restore;field.setSelection(Math.min(caretStart,field.length()),Math.min(caretEnd,field.length()));}}});
        box.onWmRestyle(font,bold,accent,0);popup=box;overlay=new FrameLayout(getContext());overlay.addView(box,new LayoutParams(-1,-1));addView(overlay,new LayoutParams(-1,-1));
    }
    private static boolean containsHangul(String value){for(int i=0;i<value.length();i++)if(value.charAt(i)>='가'&&value.charAt(i)<='힣')return true;return false;}
    private void openDate(WmDateTimeField field){WmDateTimeField.Value v=field.dateValue();try{checkDate(v);}catch(IllegalArgumentException invalid){showError(invalid.getMessage());return;}final int token=generation+1;WmCalendarPicker picker=new WmCalendarPicker(getContext(),font,locale,v.year,v.month,v.day,accent,(y,m,d)->{if(token!=generation)return;closePopup();field.selectDate(y,m,d);});picker.setOnDateChangeListener((y,m,d)->{if(token==generation)field.selectDate(y,m,d);});android.graphics.Point size=picker.naturalSize();present(picker,field.dateChoice(),size.x,size.y);}
    private void openNumber(WmEditableChoice field){final int token=generation+1;String[] values=field.candidates();int selected=field.dropdownSelectedIndex();WmRadioMenu menu=new WmRadioMenu(getContext(),font,values,selected,"",accent,index->{if(token!=generation)return;closePopup();field.choose(index);});menu.enableChoiceKeyboard(selected,index->{if(token==generation)field.choose(index);},()->{if(token==generation){closePopup();if(field.isShown())field.requestFocus();}});present(new WmScrollView(getContext(),menu),field,Math.max(200,menu.menuWidth()),menu.choiceDropdownHeight(field.getClosedDropdownClientHeight()));menu.requestFocus();}
    private void openChoice(WmChoice field){final int token=generation+1;WmRadioMenu menu=new WmRadioMenu(getContext(),font,field.displayChoices(),field.displaySelectedIndex(),"",accent,index->{if(token!=generation)return;closePopup();field.chooseDisplayIndex(index);});menu.enableChoiceKeyboard(field.displaySelectedIndex(),index->{if(token==generation)field.chooseDisplayIndex(index);},()->{if(token==generation){closePopup();if(field.isShown())field.requestFocus();}});menu.enableChoiceCharacterSearch(field.displaySortLocale(),index->{if(token==generation&&field.isEnabled())field.chooseCharacterDisplayIndex(index);});present(new WmScrollView(getContext(),menu),field,Math.max(160,menu.menuWidth()),menu.choiceDropdownHeight(170));menu.requestFocus();}
    private void present(View view,View anchor,int desiredWidth,int desiredHeight){closePopup();popup=view;overlay=new FrameLayout(getContext());overlay.setOnClickListener(v->{if(!(popup instanceof WmCalendarPicker)||!WmControls.back(popup))closePopup();});view.setClickable(true);View presented=view;if(view instanceof WmCalendarPicker){FrameLayout frame=new FrameLayout(getContext());frame.setBackgroundColor(android.graphics.Color.BLACK);frame.setPadding(2,2,2,2);frame.setClickable(true);frame.addView(view,new FrameLayout.LayoutParams(-1,-1));presented=frame;desiredWidth+=4;desiredHeight+=4;}int width=Math.min(getWidth(),desiredWidth),available=Math.max(0,getHeight()-60),height=Math.min(available,desiredHeight);Rect bounds=new Rect(0,0,anchor.getWidth(),anchor.getHeight());offsetDescendantRectToMyCoords(anchor,bounds);int x=Math.max(0,Math.min(getWidth()-width,bounds.left)),y=bounds.bottom;if(y+height>available)y=bounds.top-height;y=Math.max(0,Math.min(available-height,y));FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(width,height);lp.leftMargin=x;lp.topMargin=y;overlay.addView(presented,lp);addView(overlay,new LayoutParams(-1,-1));WmChoice.bindDropdown(anchor,overlay);}
    public boolean closePopup(){generation++;if(overlay==null)return false;removeView(overlay);overlay=null;popup=null;return true;}
    @Override public boolean back(){if(popup!=null){if(WmControls.back(popup))return true;return closePopup();}return wizard.back();}
    @Override protected void onDetachedFromWindow(){closePopup();super.onDetachedFromWindow();}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){font=regular;accent=selection;Typeface heading=bold==null?regular:bold;this.bold=heading;for(TextView view:new TextView[]{rangeStartCaption,rangeEndCaption}){view.setTypeface(heading);WmFonts.attach(view,heading);}if(!(popup instanceof WmMessageBox))closePopup();}
}
