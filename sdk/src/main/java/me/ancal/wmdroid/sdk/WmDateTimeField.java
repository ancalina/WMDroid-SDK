package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/** Civil date/time composite for native start25055/end25056 roles. Caller owns
 * popups, persistence and UI-thread callbacks. Width distribution and the4px
 * gap adapt to available space; they are not measured native DLU geometry. */
public final class WmDateTimeField extends FrameLayout implements WmControls.RestyleListener {
    public static final class Value {
        public final int year,month,day,hour,minute;
        public Value(int year,int month,int day,int hour,int minute){
            calendar(year,month,day,hour,minute);
            this.year=year;this.month=month;this.day=day;this.hour=hour;this.minute=minute;
        }
        @Override public boolean equals(Object other){if(!(other instanceof Value))return false;Value v=(Value)other;return year==v.year&&month==v.month&&day==v.day&&hour==v.hour&&minute==v.minute;}
        @Override public int hashCode(){return ((((year*31+month)*31+day)*31+hour)*31+minute);}
    }
    public interface OnDateOpen {void open(WmDateTimeField source);}
    public interface OnTimeOpen {void open(WmDateTimeField source);}
    public interface OnValueChangedListener {void changed(Value oldValue,Value newValue);}
    private static final TimeZone UTC=TimeZone.getTimeZone("UTC");
    private static final int GAP=4;
    private final WmDateField date;
    private final WmEditableChoice time;
    private Locale locale,parserLocale;
    private boolean restoring;
    private Value civil,notified;
    private SimpleDateFormat parser,desiredParser;
    private final boolean pimInline;
    private int origin;
    private OnDateOpen dateOpen;
    private OnTimeOpen timeOpen;
    private OnValueChangedListener changed;

    public WmDateTimeField(Context context,Typeface font,Locale locale,Value value,int accent){
        this(context,font,locale,value,accent,false);
    }
    WmDateTimeField(Context context,Typeface font,Locale locale,Value value,int accent,boolean recurrenceDateFormat){
        this(context,font,locale,value,accent,recurrenceDateFormat,false);
    }
    WmDateTimeField(Context context,Typeface font,Locale locale,Value value,int accent,boolean recurrenceDateFormat,boolean pimInline){
        super(context);if(font==null||locale==null||value==null)throw new IllegalArgumentException("Font, locale and value required");
        this.pimInline=pimInline;
        checkDate(value);this.locale=locale;civil=notified=value;formats();parser=desiredParser;parserLocale=locale;
        date=new WmDateField(context,font,locale,value,accent,!recurrenceDateFormat&&!pimInline,recurrenceDateFormat);
        if(!pimInline)date.setRange(new Value(WmYearChooser.MIN_YEAR,1,1,0,0),new Value(WmYearChooser.MAX_YEAR,12,31,0,0));date.setAllowEmpty(false);
        time=new WmEditableChoice(context,font,candidates(),timeText(value,parser),accent);
        if(pimInline){date.setPimInline();time.setPimInline();time.setClosedDropdownClientHeight(256);}
        addView(date,new LayoutParams(-1,-1));addView(time,new LayoutParams(-1,-1));
        date.setOnOpenListener(source->{if(isEnabled()&&dateOpen!=null)dateOpen.open(this);});
        date.setOnValueChangedListener((oldValue,newValue)->commitDate());
        time.setOnOpenListener(source->{if(isEnabled()&&timeOpen!=null)timeOpen.open(this);});
        time.setOnValueChangedListener(raw->{if(restoring)return;Value next;try{next=parsedValue();}catch(IllegalArgumentException invalid){return;}Value previous=notified;civil=notified=next;
            if(parser!=desiredParser){int start=time.getSelectionStart(),end=time.getSelectionEnd();parser=desiredParser;parserLocale=locale;replaceTime(timeText(next,parser),start,end);}
            if(!previous.equals(next)&&changed!=null)changed.changed(previous,next);
        });
    }
    /** Owned segmented DATE control; do not reparent or replace its open/value
     * listeners. Use setOnDateOpen and the composite setOnValueChangedListener.
     * Consumers of the former WmChoice return type must rebuild. */
    public WmDateField dateChoice(){return date;}
    /** Owned time editor; do not reparent or replace its open/value listeners.
     * They parse time and synchronize valid/notified values; incomplete text stays a draft.
     * Use setOnTimeOpen and the composite setOnValueChangedListener. */
    public WmEditableChoice timeChoice(){return time;}
    /** Displayed date with last valid clock; accessible during incomplete time editing. */
    public Value dateValue(){Value current=date.value();checkDate(current);return new Value(current.year,current.month,current.day,civil.hour,civil.minute);}
    /** Snapshot of the displayed date and strictly parsed time; incomplete input is an error. */
    public Value getValue(){return parsedValue();}
    public Value value(){return getValue();}
    public void setValue(Value value){
        checkDate(value);date.setValue(value);civil=notified=value;parser=desiredParser;parserLocale=locale;
        time.setValue(timeText(value,parser));requestLayout();
    }
    /** A completed date choice changes only the date. Invalid time remains untouched;
     * its combined change is reported once when time editing becomes valid. */
    public void selectDate(int year,int month,int day){
        Value selected=new Value(year,month,day,civil.hour,civil.minute);checkDate(selected);if(!isEnabled()||!date.isEnabled())return;
        date.setValue(selected);commitDate();
    }
    private void commitDate(){
        if(restoring)return;
        civil=dateValue();requestLayout();
        Value next;try{next=parsedValue();}catch(IllegalArgumentException invalid){return;}Value previous=notified;civil=notified=next;if(!previous.equals(next)&&changed!=null)changed.changed(previous,next);
    }
    public void setOnDateOpen(OnDateOpen listener){dateOpen=listener;}
    public void setOnTimeOpen(OnTimeOpen listener){timeOpen=listener;}
    public void setOnValueChangedListener(OnValueChangedListener listener){changed=listener;}
    /** Popup presets start at origin, advancing thirty minutes modulo one day. */
    public void setTimeOrigin(int hour,int minute){
        if(hour<0||hour>23||minute<0||minute>59)throw new IllegalArgumentException("Invalid time origin");origin=hour*60+minute;time.setCandidates(candidates());requestLayout();
    }
    public void setLocale(Locale locale){
        if(locale==null)throw new IllegalArgumentException("Locale required");Value valid=null;try{valid=parsedValue();}catch(IllegalArgumentException invalid){}
        this.locale=locale;formats();date.setLocale(locale);time.setCandidates(candidates());
        if(valid!=null){civil=notified=valid;int start=time.getSelectionStart(),end=time.getSelectionEnd();parser=desiredParser;parserLocale=locale;replaceTime(timeText(valid,parser),start,end);}
        // With invalid input, keep its Editable and previous parser until valid.
        requestLayout();
    }
    /** Self-contained civil/draft interaction snapshot. Popups, gesture capture,
     * theme, enabled policy and IME composition belong to the owner/platform. */
    public Bundle saveInteractionState(){
        Bundle state=new Bundle();state.putInt("schema",1);state.putIntArray("civil",parts(dateValue()));state.putIntArray("notified",parts(notified));
        state.putBundle("date",date.saveInteractionState());state.putString("time",time.value());state.putStringArray("candidates",time.candidates());state.putBundle("time_interaction",time.saveInteractionState());
        state.putSerializable("parser_locale",parserLocale);state.putString("parser_pattern",parser.toPattern());state.putInt("origin",origin);state.putString("focus",date.hasFocus()?"date":time.hasFocus()?"time":"none");return state;
    }
    /** Validate the complete snapshot against this DATE format/range before mutation. */
    public void validateInteractionState(Bundle state){
        if(state==null||state.size()!=11||!(state.get("schema") instanceof Integer)||state.getInt("schema")!=1||!(state.get("date") instanceof Bundle)||!(state.get("time_interaction") instanceof Bundle)||!(state.get("time") instanceof String)||!(state.get("candidates") instanceof String[])||!(state.get("parser_locale") instanceof Locale)||!(state.get("parser_pattern") instanceof String)||!(state.get("origin") instanceof Integer)||!(state.get("focus") instanceof String))throw new IllegalArgumentException("Date/time interaction schema1 required");
        Value value=stateValue(state,"civil");stateValue(state,"notified");date.validateInteractionStateForValue(state.getBundle("date"),value);
        String[] candidates=state.getStringArray("candidates");if(candidates.length==0)throw new IllegalArgumentException("Time candidates required");for(String candidate:candidates)if(candidate==null)throw new IllegalArgumentException("Null time candidate");
        WmEditableChoice.validateInteractionState(state.getBundle("time_interaction"),candidates.length,state.getString("time").length());
        Locale parsing=(Locale)state.get("parser_locale");String pattern=state.getString("parser_pattern"),focus=state.getString("focus");
        if(!pattern.equals(WmTimeFormat.minutePattern(parsing,true))&&!pattern.equals(WmTimeFormat.minutePattern(parsing,false))||state.getInt("origin")<0||state.getInt("origin")>=1440||!focus.equals("date")&&!focus.equals("time")&&!focus.equals("none"))throw new IllegalArgumentException("Invalid time parser/origin/focus");
    }
    /** Silent restoration; malformed data never replaces any date/time/interaction state. */
    public void restoreInteractionState(Bundle state){
        if(restoring)throw new IllegalStateException("Reentrant date/time restoration");validateInteractionState(state);if(date.isDroppedDown()||time.isDroppedDown())throw new IllegalStateException("Close date/time popups before restoring interaction");
        Value restoredCivil=stateValue(state,"civil"),restoredNotified=stateValue(state,"notified");Locale parsing=(Locale)state.get("parser_locale");String pattern=state.getString("parser_pattern");
        SimpleDateFormat restoredParser=parsing.equals(locale)&&pattern.equals(desiredParser.toPattern())?desiredParser:new SimpleDateFormat(pattern,parsing);restoredParser.setTimeZone(UTC);restoredParser.setLenient(false);
        restoring=true;
        try{
            String focus=state.getString("focus");if(focus.equals("date"))date.requestFocus();else if(focus.equals("time"))time.requestFocus();else clearFocus();
            date.setValue(restoredCivil);civil=restoredCivil;notified=restoredNotified;origin=state.getInt("origin");parser=restoredParser;parserLocale=parsing;
            time.setCandidates(state.getStringArray("candidates"));time.setValue(state.getString("time"));time.restoreInteractionState(state.getBundle("time_interaction"));date.restoreInteractionState(state.getBundle("date"));requestLayout();
        }finally{restoring=false;}
    }
    private static int[] parts(Value value){return new int[]{value.year,value.month,value.day,value.hour,value.minute};}
    private Value stateValue(Bundle state,String key){Object raw=state.get(key);if(!(raw instanceof int[])||((int[])raw).length!=5)throw new IllegalArgumentException("Civil date/time parts required");int[] p=(int[])raw;Value value=new Value(p[0],p[1],p[2],p[3],p[4]);checkDate(value);return value;}
    private void formats(){
        desiredParser=new SimpleDateFormat(WmTimeFormat.minutePattern(locale,android.text.format.DateFormat.is24HourFormat(getContext())),locale);desiredParser.setTimeZone(UTC);desiredParser.setLenient(false);
    }
    private void checkDate(Value value){if(pimInline){if(value==null)throw new IllegalArgumentException("Task reminder date required");WmDateField.normalized(value);}else if(value==null||value.year<WmYearChooser.MIN_YEAR||value.year>WmYearChooser.MAX_YEAR)throw new IllegalArgumentException("Date outside1900..2999");}
    private static String timeText(Value value,SimpleDateFormat formatter){return formatter.format(calendar(2000,1,1,value.hour,value.minute).getTime());}
    private String[] candidates(){String[] values=new String[48];for(int i=0;i<values.length;i++){int minute=(origin+i*30)%1440;values[i]=desiredParser.format(calendar(2000,1,1,minute/60,minute%60).getTime());}return values;}
    private Value parsedValue(){
        String raw=time.value().trim();Date parsed=parse(raw,parser);if(parsed==null&&parser!=desiredParser)parsed=parse(raw,desiredParser);
        if(parsed==null)throw new IllegalArgumentException("Invalid time");Calendar result=new GregorianCalendar(UTC,Locale.US);result.setTime(parsed);
        Value current=date.value();checkDate(current);return new Value(current.year,current.month,current.day,result.get(Calendar.HOUR_OF_DAY),result.get(Calendar.MINUTE));
    }
    private static Date parse(String raw,SimpleDateFormat formatter){if(raw.length()==0)return null;ParsePosition position=new ParsePosition(0);Date result=formatter.parse(raw,position);return result!=null&&position.getIndex()==raw.length()?result:null;}
    private void replaceTime(String value,int start,int end){time.setValue(value);if(start>=0&&end>=0)time.setSelection(Math.min(start,value.length()),Math.min(end,value.length()));}
    private static GregorianCalendar calendar(int year,int month,int day,int hour,int minute){
        if(year<1||month<1||month>12||day<1||hour<0||hour>23||minute<0||minute>59)throw new IllegalArgumentException("Invalid civil date/time");
        GregorianCalendar result=new GregorianCalendar(UTC,Locale.US);result.setGregorianChange(new Date(Long.MIN_VALUE));result.setLenient(false);result.clear();result.set(year,month-1,day,hour,minute,0);
        try{result.getTimeInMillis();if(result.get(Calendar.YEAR)!=year||result.get(Calendar.MONTH)!=month-1||result.get(Calendar.DAY_OF_MONTH)!=day)throw new IllegalArgumentException("Invalid civil date/time");}catch(IllegalArgumentException invalid){throw new IllegalArgumentException("Invalid civil date/time");}return result;
    }
    @Override public void setEnabled(boolean enabled){super.setEnabled(enabled);if(date!=null)date.setEnabled(enabled);if(time!=null)time.setEnabled(enabled);}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){date.onWmRestyle(regular,bold,selection,tabColor);time.onWmRestyle(regular,bold,selection,tabColor);requestLayout();}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        date.measure(View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));int dateWidth=pimInline?date.pimCaptionWidth():date.getMeasuredWidth();
        int timeWidth=0;for(String candidate:time.candidates())timeWidth=Math.max(timeWidth,(int)Math.ceil(WmFonts.measure(time.getPaint(),candidate)));timeWidth+=time.getPaddingLeft()+time.getPaddingRight();
        int width=resolveSize(dateWidth+timeWidth+GAP+getPaddingLeft()+getPaddingRight(),widthSpec);
        int available=Math.max(0,width-getPaddingLeft()-getPaddingRight()),gap=Math.min(GAP,available),fields=available-gap;
        int right=pimInline?Math.max(0,fields-dateWidth):fields>=dateWidth+timeWidth?timeWidth:(int)((long)fields*timeWidth/Math.max(1,dateWidth+timeWidth));
        int dateSpec=View.MeasureSpec.makeMeasureSpec(fields-right,View.MeasureSpec.EXACTLY),timeSpec=View.MeasureSpec.makeMeasureSpec(right,View.MeasureSpec.EXACTLY);
        int padding=getPaddingTop()+getPaddingBottom(),height;
        if(View.MeasureSpec.getMode(heightSpec)==View.MeasureSpec.EXACTLY)height=View.MeasureSpec.getSize(heightSpec);
        else{
            int naturalSpec=getChildMeasureSpec(heightSpec,padding,LayoutParams.WRAP_CONTENT);
            date.measure(dateSpec,naturalSpec);time.measure(timeSpec,naturalSpec);
            height=resolveSize(Math.max(getSuggestedMinimumHeight(),Math.max(date.getMeasuredHeight(),time.getMeasuredHeight())+padding),heightSpec);
        }
        int childSpec=View.MeasureSpec.makeMeasureSpec(Math.max(0,height-padding),View.MeasureSpec.EXACTLY);
        date.measure(dateSpec,childSpec);time.measure(timeSpec,childSpec);setMeasuredDimension(width,height);
    }
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){int x=getPaddingLeft(),y=getPaddingTop(),height=Math.max(0,getHeight()-y-getPaddingBottom());date.layout(x,y,x+date.getMeasuredWidth(),y+height);x+=date.getMeasuredWidth()+Math.min(GAP,Math.max(0,getWidth()-getPaddingLeft()-getPaddingRight()));time.layout(x,y,x+time.getMeasuredWidth(),y+height);}
}
