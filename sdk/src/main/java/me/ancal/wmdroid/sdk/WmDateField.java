package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import java.text.AttributedCharacterIterator;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/** Nullable, date-only segmented field. The owner owns the dropdown and persistence.
 * Flat face follows native Contacts; gutter28 and Android font raster remain adaptations.
 * Arithmetic is proleptic Gregorian; native historical cutover keyboard behavior is not reproduced. */
public final class WmDateField extends View implements WmControls.RestyleListener,WmControls.LanguageListener {
    public interface OnValueChangedListener {void changed(WmDateTimeField.Value oldValue,WmDateTimeField.Value newValue);}
    public interface OnOpenListener {void open(WmDateField field);}
    private static final TimeZone UTC=TimeZone.getTimeZone("UTC");
    private static final WmDateTimeField.Value MIN=new WmDateTimeField.Value(1752,9,14,0,0),MAX=new WmDateTimeField.Value(9999,12,31,0,0);
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics metrics=new Paint.FontMetrics();
    private final Rect ink=new Rect();
    private final WmComboGlyph dropdown=new WmComboGlyph(this);
    private final ArrayList<Segment> segments=new ArrayList<>();
    private final ArrayList<Segment> entries=new ArrayList<>();
    private float[] widths=new float[0];
    private boolean geometryDirty=true;
    private float textWidth,scrollOffset;
    private final boolean shortDateFormat;
    private final boolean recurrenceDateFormat;
    private boolean shortYear,expandedYear;
    private Typeface font;
    private Locale locale;
    private String language;
    private SimpleDateFormat formatter,expandedFormatter;
    private char separator;
    private WmDateTimeField.Value date;
    private WmDateTimeField.Value minimum=MIN,maximum=MAX;
    private boolean allowEmpty=true,bordered,pimInline;
    private OnValueChangedListener changed;
    private OnOpenListener opened;
    private int accent,field=Calendar.MONTH,entry=-1,touchTarget=-1;
    private boolean tracking,touchActive;
    private String display="",pimCaption="",digits="";
    private static final class Segment {
        final int field;int start,end;float left,right;
        Segment(int field,int start,int end){this.field=field;this.start=start;this.end=end;}
    }

    public WmDateField(Context context,Typeface font,Locale locale,WmDateTimeField.Value value,int accent){this(context,font,locale,value,accent,false);}
    /** True selects native locale short format; false keeps existing PIM kind5 full year.
     * Native type3 yy ignores ordinary year digits in the traced Gregorian build;
     * year arrows/Home/End remain available. Native runtime equivalence is unverified. */
    public WmDateField(Context context,Typeface font,Locale locale,WmDateTimeField.Value value,int accent,boolean shortDateFormat){
        this(context,font,locale,value,accent,shortDateFormat,false);
    }
    WmDateField(Context context,Typeface font,Locale locale,WmDateTimeField.Value value,int accent,boolean shortDateFormat,boolean recurrenceDateFormat){
        super(context);if(font==null||locale==null)throw new IllegalArgumentException("Font and locale required");
        this.shortDateFormat=shortDateFormat;
        this.recurrenceDateFormat=recurrenceDateFormat;
        this.font=font;this.locale=locale;this.language=language(locale);this.accent=accent;date=normalized(value);formats();
        setFocusable(true);setFocusableInTouchMode(true);setClickable(true);setSoundEffectsEnabled(false);refresh();
    }
    public WmDateTimeField.Value value(){return date;}
    /** Owner range. Valid bounds silently clamp the current date, like DTM_SETRANGE.
     * Invalid bounds throw before changing value, pending input or interaction. */
    public void setRange(WmDateTimeField.Value minimum,WmDateTimeField.Value maximum){
        WmDateTimeField.Value low=normalized(minimum),high=normalized(maximum);
        if(low==null||high==null||compare(low,high)>0)throw new IllegalArgumentException("Ordered date bounds required");
        cancelTouch();digits="";this.minimum=low;this.maximum=high;if(date!=null)date=clamped(date);refresh();
    }
    /** Native appointment dates forbid None; existing nullable owners remain unchanged. */
    public void setAllowEmpty(boolean allowEmpty){if(!allowEmpty&&date==null)throw new IllegalArgumentException("Required date must have a value");this.allowEmpty=allowEmpty;}
    private WmDateTimeField.Value checked(WmDateTimeField.Value value){
        WmDateTimeField.Value next=normalized(value);
        if(next==null){if(!allowEmpty)throw new IllegalArgumentException("Required date cannot be empty");}
        else if(compare(next,minimum)<0||compare(next,maximum)>0)throw new IllegalArgumentException("Date outside owner range");
        return next;
    }
    private WmDateTimeField.Value clamped(WmDateTimeField.Value value){return compare(value,minimum)<0?minimum:compare(value,maximum)>0?maximum:value;}
    /** Canonical owner value, independent of the displayed locale. Empty means no date. */
    public static WmDateTimeField.Value parseValue(String canonical){
        if(canonical==null)throw new IllegalArgumentException("Canonical date required");if(canonical.isEmpty())return null;
        if(canonical.length()!=10||canonical.charAt(4)!='-'||canonical.charAt(7)!='-')throw new IllegalArgumentException("Canonical yyyy-MM-dd required");
        for(int i=0;i<canonical.length();i++)if(i!=4&&i!=7&&(canonical.charAt(i)<'0'||canonical.charAt(i)>'9'))throw new IllegalArgumentException("ASCII date digits required");
        return normalized(new WmDateTimeField.Value(Integer.parseInt(canonical.substring(0,4)),Integer.parseInt(canonical.substring(5,7)),Integer.parseInt(canonical.substring(8,10)),0,0));
    }
    public static String formatValue(WmDateTimeField.Value value){WmDateTimeField.Value normalized=normalized(value);return normalized==null?"":String.format(Locale.US,"%04d-%02d-%02d",normalized.year,normalized.month,normalized.day);}
    /** Interaction only. The owner saves/restores the nullable canonical value separately. */
    public Bundle saveInteractionState(){Bundle state=new Bundle();state.putInt("schema",2);state.putInt("field",field);state.putInt("entry",entry);state.putString("digits",digits);return state;}
    /** Fully validate before replacing selection/pending digits. No activation, focus or callbacks. */
    public static void validateInteractionState(Bundle state,WmDateTimeField.Value value){validateInteractionState(state,value,false);}
    private static void validateInteractionState(Bundle state,WmDateTimeField.Value value,boolean weekday){
        normalized(value);
        if(state==null||!(state.get("schema") instanceof Integer)||!(state.get("field") instanceof Integer)||!(state.get("digits") instanceof String))throw new IllegalArgumentException("Date interaction schema1/2 required");
        int schema=(Integer)state.get("schema");if(schema!=1&&schema!=2||state.size()!=(schema==1?3:4)||schema==2&&!(state.get("entry") instanceof Integer))throw new IllegalArgumentException("Date interaction schema1/2 required");
        int selected=(Integer)state.get("field");checkField(selected);int kind=stateEntry(state);String pending=(String)state.get("digits");
        if(kind==-1){if(selected!=Calendar.MONTH||!pending.isEmpty())throw new IllegalArgumentException("Unselected date requires empty input and fallback month");}
        else if(kind==Calendar.DAY_OF_WEEK){if(!weekday||selected!=Calendar.DAY_OF_MONTH)throw new IllegalArgumentException("Weekday selection requires recurrence DATE");}
        else{checkField(kind);if(kind!=selected)throw new IllegalArgumentException("Date field and entry disagree");}
        if(pending.length()>=(selected==Calendar.YEAR?4:2)||!pending.isEmpty()&&value==null)throw new IllegalArgumentException("Pending date digits require a value and incomplete segment");
        for(int i=0;i<pending.length();i++)if(pending.charAt(i)<'0'||pending.charAt(i)>'9')throw new IllegalArgumentException("Pending ASCII digits required");
    }
    private static int stateEntry(Bundle state){return (Integer)state.get("schema")==1?(Integer)state.get("field"):(Integer)state.get("entry");}
    void validateInteractionStateForValue(Bundle state,WmDateTimeField.Value value){validateInteractionState(state,checked(value),recurrenceDateFormat);if(shortYear&&(Integer)state.get("field")==Calendar.YEAR&&!((String)state.get("digits")).isEmpty())throw new IllegalArgumentException("Short year has no pending numeric input");}
    public void restoreInteractionState(Bundle state){validateInteractionStateForValue(state,date);cancelTouch();entry=stateEntry(state);field=(Integer)state.get("field");digits=(String)state.get("digits");refresh();}
    /** Silent replacement. Null is a real empty date, never a sentinel date. */
    public void setValue(WmDateTimeField.Value value){WmDateTimeField.Value next=checked(value);cancelTouch();digits="";date=next;refresh();}
    /** Validates date and range before enabled checks; invalid values throw even
     * when disabled. Valid enabled changes notify synchronously when different. */
    public void selectDate(int year,int month,int day){WmDateTimeField.Value next=checked(new WmDateTimeField.Value(year,month,day,0,0));if(!isEnabled())return;digits="";cancelTouch();change(next);}
    public void clearDate(){if(!isEnabled()||!allowEmpty)return;digits="";cancelTouch();change(null);}
    public int selectedField(){return field;}
    /** Selecting another segment commits pending user digits; it is not a silent value setter. */
    public void selectField(int field){checkField(field);if(entry==field)return;if(isEnabled())commitDigits();select(field);}
    public void setLocale(Locale locale){if(locale==null)throw new IllegalArgumentException("Locale required");cancelTouch();this.locale=locale;this.language=language(locale);formats();refresh();}
    private String language(Locale locale){return WmStrings.language(getContext(),android.os.Build.VERSION.SDK_INT>=21?Modern.tag(locale):locale.toString().replace('_','-'));}
    private static final class Modern {static String tag(Locale locale){return locale.toLanguageTag();}}
    @Override public void onWmLanguageChanged(String language){setLocale(WmStrings.locale(WmStrings.language(getContext(),language)));}
    public void bindDropdown(View popup){dropdown.bind(popup);}
    public boolean isDroppedDown(){return dropdown.isDropped();}
    public void setOnOpenListener(OnOpenListener listener){opened=listener;}
    public void setOnValueChangedListener(OnValueChangedListener listener){changed=listener;}

    private static void checkField(int field){if(field!=Calendar.YEAR&&field!=Calendar.MONTH&&field!=Calendar.DAY_OF_MONTH)throw new IllegalArgumentException("Year, month or day field required");}
    private static int compare(WmDateTimeField.Value a,WmDateTimeField.Value b){if(a.year!=b.year)return a.year<b.year?-1:1;if(a.month!=b.month)return a.month<b.month?-1:1;return a.day==b.day?0:a.day<b.day?-1:1;}
    static WmDateTimeField.Value normalized(WmDateTimeField.Value value){
        if(value==null)return null;WmDateTimeField.Value result=new WmDateTimeField.Value(value.year,value.month,value.day,0,0);
        if(compare(result,MIN)<0||compare(result,MAX)>0)throw new IllegalArgumentException("Date outside 1752-09-14..9999-12-31");return result;
    }
    private static GregorianCalendar calendar(WmDateTimeField.Value value){GregorianCalendar result=new GregorianCalendar(UTC,Locale.US);result.setGregorianChange(new java.util.Date(Long.MIN_VALUE));result.setLenient(false);result.clear();result.set(value.year,value.month-1,value.day,12,0,0);return result;}
    private void formats(){
        geometryDirty=true;
        DateFormat format=recurrenceDateFormat?WmDateFormats.recurrenceDate(locale):shortDateFormat?WmDateFormats.shortDate(locale):WmDateFormats.fullDate(locale);
        if(!(format instanceof SimpleDateFormat))throw new IllegalArgumentException("Locale date pattern required");formatter=(SimpleDateFormat)format;
        String pattern=formatter.toPattern();boolean quoted=false;separator=0;shortYear=false;
        for(int i=0;i<pattern.length();i++){char ch=pattern.charAt(i);if(ch=='\''){if(i+1<pattern.length()&&pattern.charAt(i+1)=='\'')i++;else quoted=!quoted;}else if(!quoted&&ch=='y'){int start=i;while(i+1<pattern.length()&&pattern.charAt(i+1)=='y')i++;shortYear=i-start<2;}else if(!quoted&&separator==0&&!(ch>='A'&&ch<='Z'||ch>='a'&&ch<='z'))separator=ch;}
        if(shortYear&&field==Calendar.YEAR)digits="";
        GregorianCalendar gregorian=new GregorianCalendar(UTC,locale);gregorian.setGregorianChange(new java.util.Date(Long.MIN_VALUE));gregorian.setLenient(false);formatter.setCalendar(gregorian);formatter.setTimeZone(UTC);formatter.setLenient(false);
        expandedFormatter=(SimpleDateFormat)formatter.clone();expandedFormatter.applyPattern(WmDateFormats.fullYearPattern(pattern));
    }
    /** Native type3 uses yyyy for its selected entry, even after focus loss. */
    private SimpleDateFormat displayFormatter(){
        boolean expanded=shortYear&&entry==Calendar.YEAR;if(expanded!=expandedYear){expandedYear=expanded;geometryDirty=true;}
        return expanded?expandedFormatter:formatter;
    }
    private void select(int next){
        boolean yearTransition=shortYear&&(entry==Calendar.YEAR||next==Calendar.YEAR);entry=next;field=next<0?Calendar.MONTH:next==Calendar.DAY_OF_WEEK?Calendar.DAY_OF_MONTH:next;
        if(yearTransition)refresh();else invalidate();
    }
    private static int dateField(AttributedCharacterIterator iterator){if(iterator.getAttribute(DateFormat.Field.YEAR)!=null)return Calendar.YEAR;if(iterator.getAttribute(DateFormat.Field.MONTH)!=null)return Calendar.MONTH;if(iterator.getAttribute(DateFormat.Field.DAY_OF_MONTH)!=null)return Calendar.DAY_OF_MONTH;return -1;}
    private static ArrayList<Segment> entries(AttributedCharacterIterator iterator){
        ArrayList<Segment> result=new ArrayList<>();int prior=-2;
        for(char ch=iterator.first();ch!=AttributedCharacterIterator.DONE;ch=iterator.next()){
            int current=dateField(iterator);if(iterator.getAttribute(DateFormat.Field.DAY_OF_WEEK)!=null)current=Calendar.DAY_OF_WEEK;
            int offset=iterator.getIndex()-iterator.getBeginIndex();
            if(current==prior)result.get(result.size()-1).end=offset+1;else result.add(new Segment(current,offset,offset+1));prior=current;
        }
        return result;
    }
    private void refresh(){
        segments.clear();entries.clear();if(date==null){display=WmStrings.text(getContext(),language,"없음");geometryDirty=true;scrollOffset=0;}
        else{
            AttributedCharacterIterator iterator=displayFormatter().formatToCharacterIterator(calendar(date).getTime());StringBuilder text=new StringBuilder();
            for(char ch=iterator.first();ch!=AttributedCharacterIterator.DONE;ch=iterator.next())text.append(ch);
            entries.addAll(entries(iterator));for(Segment entry:entries)if(entry.field>=0)segments.add(entry);
            display=text.toString();
            if(!digits.isEmpty()&&entry!=Calendar.DAY_OF_WEEK)for(Segment segment:segments)if(segment.field==entry){NumberFormat number=NumberFormat.getIntegerInstance(locale);number.setGroupingUsed(false);number.setMinimumIntegerDigits(digits.length());String replacement=number.format(Integer.parseInt(digits));int oldEnd=segment.end,delta=replacement.length()-(segment.end-segment.start);display=display.substring(0,segment.start)+replacement+display.substring(segment.end);segment.end=segment.start+replacement.length();for(Segment other:entries)if(other!=segment&&other.start>=oldEnd){other.start+=delta;other.end+=delta;}break;}
        }
        pimCaption=pimInline&&date!=null?expandedFormatter.format(calendar(date).getTime()):display;
        setContentDescription(pimInline?pimCaption:display);requestLayout();invalidate();
    }
    /** Native3289114 measures year once, months1..12 and day/weekday10..17.
     * Keep candidate widths until font/format changes, not each value/key/draw. */
    private void measureCandidate(int part,int value){
        GregorianCalendar candidate=calendar(new WmDateTimeField.Value(date.year,1,1,0,0));candidate.set(part,value);
        AttributedCharacterIterator iterator=displayFormatter().formatToCharacterIterator(candidate.getTime());StringBuilder text=new StringBuilder();
        for(char ch=iterator.first();ch!=AttributedCharacterIterator.DONE;ch=iterator.next())text.append(ch);
        ArrayList<Segment> runs=entries(iterator);
        for(int i=0;i<runs.size();i++){
            Segment run=runs.get(i);boolean matches=part==Calendar.DAY_OF_MONTH?(run.field==Calendar.DAY_OF_MONTH||run.field==Calendar.DAY_OF_WEEK):run.field==part;
            if(matches)widths[i]=Math.max(widths[i],(float)Math.ceil(WmFonts.measure(paint,text.substring(run.start,run.end))));
        }
    }
    private void ensureGeometry(){
        paint.setTypeface(font);paint.setTextSize(pimInline?28:WmMetrics.FONT);
        if(geometryDirty||widths.length!=entries.size()){
            widths=new float[entries.size()];
            for(int i=0;i<entries.size();i++){Segment entry=entries.get(i);if(entry.field<0)widths[i]=(float)Math.ceil(WmFonts.measure(paint,display.substring(entry.start,entry.end)));}
            if(date!=null){measureCandidate(Calendar.YEAR,date.year);for(int month=0;month<12;month++)measureCandidate(Calendar.MONTH,month);for(int day=10;day<=17;day++)measureCandidate(Calendar.DAY_OF_MONTH,day);}
            geometryDirty=false;
        }
        textWidth=0;for(int i=0;i<entries.size();i++){Segment entry=entries.get(i);entry.left=textWidth;entry.right=textWidth+=widths[i];}
        if(date==null)textWidth=WmFonts.measure(paint,display);
        float available=Math.max(0,getWidth()-36);scrollOffset=Math.max(0,Math.min(scrollOffset,Math.max(0,textWidth-available)));
        // Native328A130 keeps the selected cached rectangle within the clipped text client.
        if(available>0)for(Segment segment:segments)if(segment.field==entry){if(segment.left<scrollOffset)scrollOffset=segment.left;else if(segment.right-scrollOffset>available)scrollOffset=segment.right-available;break;}
    }
    private void change(WmDateTimeField.Value next){WmDateTimeField.Value previous=date;date=next;refresh();if(previous==null?next!=null:!previous.equals(next))if(changed!=null)changed.changed(previous,next);}
    private void activate(){if(date==null)today();}
    private void today(){Calendar today=new GregorianCalendar(TimeZone.getDefault(),Locale.US);change(clamped(new WmDateTimeField.Value(today.get(Calendar.YEAR),today.get(Calendar.MONTH)+1,today.get(Calendar.DAY_OF_MONTH),0,0)));}
    // Native328AD6C clears entry selection before handing focus to MonthCal.
    private void open(){commitDigits();if(!isEnabled())return;performClick();if(isEnabled()&&opened!=null){select(-1);opened.open(this);}}
    private int days(int year,int month){GregorianCalendar result=calendar(new WmDateTimeField.Value(year,month,1,0,0));return result.getActualMaximum(Calendar.DAY_OF_MONTH);}
    private void adjust(int direction){
        commitDigits();if(!isEnabled())return;activate();if(!isEnabled()||date==null)return;int year=date.year,month=date.month,day=date.day;
        if(field==Calendar.YEAR)year=Math.max(minimum.year,Math.min(maximum.year,year+direction));else if(field==Calendar.MONTH)month=(month-1+direction+12)%12+1;else day=(day-1+direction+days(year,month))%days(year,month)+1;
        day=Math.min(day,days(year,month));change(clamped(new WmDateTimeField.Value(year,month,day,0,0)));
    }
    private void endpoint(boolean end){
        commitDigits();if(!isEnabled()||date==null)return;
        int value=field==Calendar.YEAR?(end?maximum.year:minimum.year):field==Calendar.MONTH?(end?12:1):(end?days(date.year,date.month):1);
        changePart(value,true);
    }
    // Native yyyy keeps an invalid prefix without notifying. Retain it in digits,
    // while Value remains the last valid owner date; the four-digit cycle validates literally.
    private void digit(int value){
        activate();if(!isEnabled()||date==null)return;if(shortYear&&field==Calendar.YEAR)return;int limit=field==Calendar.YEAR?4:2;
        int maximum=field==Calendar.YEAR?9999:field==Calendar.MONTH?12:days(date.year,date.month);
        String next=digits+value;int number=Integer.parseInt(next);if(number>maximum){next=String.valueOf(value);number=value;}
        digits=next;if(field!=Calendar.YEAR&&number<1){digits="";refresh();return;}
        if(digits.length()==limit)commitDigits(false);else if(field==Calendar.YEAR)refresh();else changePart(number,true);
    }
    private void commitDigits(){commitDigits(true);}
    private void commitDigits(boolean expandYear){
        if(digits.isEmpty())return;int input=Integer.parseInt(digits);digits="";
        if(date==null){refresh();return;}
        if(expandYear&&field==Calendar.YEAR&&input<100)input+=input<=29?2000:1900;
        if(!expandYear&&field==Calendar.YEAR&&input<1752){refresh();return;}
        changePart(input,!expandYear);
    }
    private void changePart(int input,boolean clampRange){
        int year=date.year,month=date.month,day=date.day;
        if(field==Calendar.YEAR)year=input;else if(field==Calendar.MONTH)month=input;else day=input;
        try{
            if(field!=Calendar.DAY_OF_MONTH&&year>=1&&month>=1&&month<=12)day=Math.min(day,days(year,month));
            WmDateTimeField.Value next=new WmDateTimeField.Value(year,month,day,0,0);
            if(clampRange)next=clamped(next);
            change(checked(next));
        }catch(IllegalArgumentException invalid){refresh();}
    }
    private void adjacent(int direction){commitDigits();if(!isEnabled()||segments.isEmpty())return;for(int i=0;i<segments.size();i++)if(segments.get(i).field==entry){select(segments.get((i+direction+segments.size())%segments.size()).field);return;}select(segments.get(direction<0?segments.size()-1:0).field);}
    private static int keyCharacter(KeyEvent event){int ch=event.getUnicodeChar();return ch>=0xff01&&ch<=0xff5e?ch-0xfee0:ch;}
    private boolean isSeparator(int ch){return ch!=0&&(ch==separator||"/.,-:".indexOf(ch)>=0);}
    private static boolean isOpenKey(int key){return key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_F4||key==KeyEvent.KEYCODE_PAGE_DOWN;}
    @Override public boolean onKeyDown(int key,KeyEvent event){
        if(!isEnabled())return super.onKeyDown(key,event);
        if(isOpenKey(key)){if(event.getRepeatCount()==0)open();return true;}
        if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){adjacent(key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1);return true;}
        if(key==KeyEvent.KEYCODE_MOVE_HOME||key==KeyEvent.KEYCODE_MOVE_END){
            if(key==KeyEvent.KEYCODE_MOVE_HOME&&event.isCtrlPressed()){commitDigits();if(isEnabled())today();}else endpoint(key==KeyEvent.KEYCODE_MOVE_END);return true;
        }
        if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_NUMPAD_ADD||key==KeyEvent.KEYCODE_NUMPAD_SUBTRACT){adjust(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_NUMPAD_ADD?1:-1);return true;}
        int character=keyCharacter(event);if(isSeparator(character)){adjacent(1);return true;}
        int numeric=Character.digit(character,10);if(numeric>=0){digit(numeric);return true;}return super.onKeyDown(key,event);
    }
    @Override public boolean onKeyUp(int key,KeyEvent event){if(isEnabled()&&(isOpenKey(key)||key>=KeyEvent.KEYCODE_DPAD_UP&&key<=KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_MOVE_HOME||key==KeyEvent.KEYCODE_MOVE_END||key==KeyEvent.KEYCODE_NUMPAD_ADD||key==KeyEvent.KEYCODE_NUMPAD_SUBTRACT||isSeparator(keyCharacter(event))||Character.digit(keyCharacter(event),10)>=0))return true;return super.onKeyUp(key,event);}
    private int target(float x,float y){
        if(x<0||x>=getWidth()||y<0||y>=getHeight())return -1;if(pimInline||x>=Math.max(0,getWidth()-28))return -2;if(segments.isEmpty())return field;
        ensureGeometry();float position=x-4+scrollOffset;for(int i=segments.size()-1;i>=0;i--){Segment segment=segments.get(i);if(position>=segment.left)return segment.field;}return -1;
    }
    private void cancelTouch(){tracking=false;touchActive=false;touchTarget=-1;invalidate();}
    @Override public boolean onTouchEvent(MotionEvent event){
        if(!isEnabled()){cancelTouch();return false;}int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){cancelTouch();if(event.getPointerCount()!=1||event.getX()<0||event.getX()>=getWidth()||event.getY()<0||event.getY()>=getHeight())return false;requestFocus();if(!isEnabled())return false;touchTarget=target(event.getX(),event.getY());tracking=touchActive=true;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);invalidate();return true;}
        if(!tracking)return false;if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN||event.getPointerCount()>1){cancelTouch();return true;}
        if(action==MotionEvent.ACTION_MOVE){if(target(event.getX(),event.getY())!=touchTarget)touchActive=false;invalidate();return true;}
        if(action==MotionEvent.ACTION_UP){boolean complete=touchActive&&target(event.getX(),event.getY())==touchTarget;int selected=touchTarget;cancelTouch();if(complete){if(selected==-2)open();else if(selected>=0){commitDigits();if(isEnabled()){select(selected);if(isEnabled())performClick();}}}return true;}return true;
    }
    @Override public boolean performClick(){if(!isEnabled())return false;requestFocus();if(!isEnabled())return false;activate();if(!isEnabled())return false;super.performClick();return true;}
    private int textHeight(){
        paint.setTypeface(font);paint.setTextSize(pimInline?28:WmMetrics.FONT);paint.getFontMetrics(metrics);
        // Imported linked faces may extend beyond the primary font's metrics.
        WmFonts.textBounds(paint,pimInline&&!isDroppedDown()?pimCaption:display,ink);metrics.ascent=Math.min(metrics.ascent,ink.top);metrics.descent=Math.max(metrics.descent,ink.bottom);
        return (int)Math.ceil(metrics.descent-metrics.ascent);
    }
    /** WS_BORDER-style outline for standalone native DATE controls; inline fields stay unbordered. */
    public void setBordered(boolean value){if(bordered!=value){bordered=value;invalidate();}}
    void setPimInline(){pimInline=true;geometryDirty=true;refresh();}
    int pimCaptionWidth(){paint.setTypeface(font);paint.setTextSize(28);return (int)Math.ceil(WmFonts.measure(paint,pimCaption)+WmFonts.measure(paint," ")*Math.max(0,12-pimCaption.length()))+4;}
    @Override protected void onMeasure(int widthSpec,int heightSpec){int textHeight=textHeight();ensureGeometry();float measured=pimInline?Math.max(textWidth,WmFonts.measure(paint,pimCaption)):textWidth;setMeasuredDimension(resolveSize((int)Math.ceil(measured)+36,widthSpec),resolveSize(Math.max(getSuggestedMinimumHeight(),Math.max(WmControls.ROW_HEIGHT,textHeight)),heightSpec));}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);int width=getWidth(),height=getHeight(),divider=Math.max(0,width-28);int textHeight=textHeight();paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);canvas.drawRect(0,0,width,height,paint);
        int save=canvas.save();canvas.clipRect(0,0,divider,height);int top=(height-textHeight)/2;float baseline=top-metrics.ascent;
        ensureGeometry();
        // Native32894C8 None path uses DT_SINGLELINE at text-client top+4,
        // independently of the centered populated entry rectangles.
        if(pimInline&&!isDroppedDown()){paint.setColor(isEnabled()?Color.BLACK:WmControls.SHADOW);WmFonts.draw(canvas,pimCaption,4,baseline,paint);}
        else if(date==null){paint.setColor(isEnabled()?Color.BLACK:WmControls.SHADOW);WmFonts.draw(canvas,display,4,4-metrics.ascent,paint);}
        else for(Segment entry:entries){
            float left=4+entry.left-scrollOffset,right=4+entry.right-scrollOffset;String text=display.substring(entry.start,entry.end);
            boolean selected=isEnabled()&&hasFocus()&&entry.field==this.entry;
            if(selected){paint.setColor(accent);canvas.drawRect(left,top,right,top+textHeight,paint);}
            paint.setColor(selected?Color.WHITE:isEnabled()?Color.BLACK:WmControls.SHADOW);
            int clipped=canvas.save();canvas.clipRect(left,0,right,height);WmFonts.draw(canvas,text,left+(right-left-WmFonts.measure(paint,text))/2,baseline,paint);canvas.restoreToCount(clipped);
        }
        canvas.restoreToCount(save);
        // SysDateTimePick draws the same enabled arrow while held or dropped; no combo face.
        if(!pimInline||hasFocus()||isDroppedDown()){paint.setColor(isEnabled()?Color.BLACK:WmControls.SHADOW);WmComboGlyph.draw(canvas,paint,divider,4,width,height,false);}
        if(bordered||pimInline&&hasFocus()){paint.setColor(Color.BLACK);int edge=WmMetrics.BORDER;canvas.drawRect(0,0,width,edge,paint);canvas.drawRect(0,height-edge,width,height,paint);canvas.drawRect(0,edge,edge,height-edge,paint);canvas.drawRect(width-edge,edge,width,height-edge,paint);}
    }
    @Override public void setEnabled(boolean enabled){if(!enabled){cancelTouch();digits="";if(dropdown!=null)dropdown.clear();}super.setEnabled(enabled);if(!enabled&&formatter!=null)refresh();invalidate();}
    /** Focus gain without a selected segment selects day (weekday in recurrence format).
     * Enabled focus loss commits digits and can synchronously notify a valid date change.
     * Short yyyy input expands 0..29 to2000..2029,30..99 to1930..1999; invalid
     * results retain the last date. yy format rejects year-digit input. */
    @Override protected void onFocusChanged(boolean gain,int direction,Rect previous){if(gain&&entry<0)select(recurrenceDateFormat?Calendar.DAY_OF_WEEK:Calendar.DAY_OF_MONTH);else if(!gain&&isEnabled())commitDigits();super.onFocusChanged(gain,direction,previous);invalidate();}
    @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){super.onSizeChanged(width,height,oldWidth,oldHeight);cancelTouch();}
    @Override protected void onDetachedFromWindow(){cancelTouch();dropdown.clear();super.onDetachedFromWindow();}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){if(regular==null)throw new IllegalArgumentException("Font required");cancelTouch();font=regular;accent=selection;geometryDirty=true;refresh();}
}
