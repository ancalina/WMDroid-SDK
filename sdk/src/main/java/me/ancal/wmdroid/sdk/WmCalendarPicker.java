package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.graphics.Rect;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/** Shared non-platform month dropdown. Caller places and dismisses it.
 * Font-derived client extent uses the native logical192DPI formula.
 */
public final class WmCalendarPicker extends LinearLayout implements WmControls.RestyleListener,WmControls.BackHandler {
    public interface OnDateListener {void choose(int year,int month,int day);}
    private final GregorianCalendar displayed=new GregorianCalendar(TimeZone.getTimeZone("UTC"),Locale.US);
    private final WmDateTimeField.Value minimum,maximum;
    private final WmMonthCalendarHeader header;private final WmMonthCalendar month;
    private final FrameLayout body,headerHost;private final Rect editorHit=new Rect();private boolean swallowedYearTouch;private View yearTouchTarget;private Locale locale;private Typeface font;
    private OnDateListener dateChanged;
    private int selection,choicesId,choiceGeneration;private View choices;
    public WmCalendarPicker(Context context,Typeface font,Locale locale,int year,int month,int day,int selection,OnDateListener listener){
        this(context,font,Typeface.create(font,Typeface.BOLD),locale,year,month,day,selection,listener);
    }
    public WmCalendarPicker(Context context,Typeface regular,Typeface bold,Locale locale,int year,int month,int day,int selection,OnDateListener listener){
        this(context,regular,bold,locale,year,month,day,selection,listener,new WmDateTimeField.Value(1900,1,1,0,0),new WmDateTimeField.Value(2999,12,31,0,0));
    }
    /** Full-date owner bounds; default constructors retain the existing PIM range. */
    public WmCalendarPicker(Context context,Typeface regular,Typeface bold,Locale locale,int year,int month,int day,int selection,OnDateListener listener,WmDateTimeField.Value minimum,WmDateTimeField.Value maximum){
        super(context);Typeface font=regular;if(listener==null||locale==null)throw new IllegalArgumentException("locale and date listener required");
        this.minimum=WmMonthCalendar.bound(minimum);this.maximum=WmMonthCalendar.bound(maximum);if(WmMonthCalendar.compareDate(this.minimum.year,this.minimum.month,this.minimum.day,this.maximum)>0)throw new IllegalArgumentException("Ordered date bounds required");
        new WmDateTimeField.Value(year,month,day,0,0);if(WmMonthCalendar.compareDate(year,month,day,this.minimum)<0||WmMonthCalendar.compareDate(year,month,day,this.maximum)>0)throw new IllegalArgumentException("picker date outside owner range");
        displayed.setGregorianChange(new java.util.Date(Long.MIN_VALUE));displayed.clear();displayed.setLenient(false);displayed.set(year,month-1,day,12,0,0);displayed.getTimeInMillis();
        this.font=font;this.locale=locale;this.selection=selection;setOrientation(VERTICAL);setBackgroundColor(android.graphics.Color.WHITE);
        header=new WmMonthCalendarHeader(context,bold,locale);header.setRange(this.minimum,this.maximum);header.setColors(selection);headerHost=new FrameLayout(context){@Override protected void onMeasure(int width,int height){super.onMeasure(width,height);setMeasuredDimension(getMeasuredWidth(),header.getMeasuredHeight());}};headerHost.setClipChildren(false);setClipChildren(false);headerHost.addView(header,new FrameLayout.LayoutParams(-1,-2));addView(headerHost,new LayoutParams(-1,-2));header.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->positionYearEditor());
        body=new FrameLayout(context);addView(body,new LayoutParams(-1,0,1));this.month=new WmMonthCalendar(context,font,bold,locale,this.minimum,this.maximum);this.month.setFocusableInTouchMode(true);this.month.setColors(selection);body.addView(this.month,new FrameLayout.LayoutParams(-1,-1));this.month.setOnDateChangeListener((y,m,d)->{long previous=displayed.getTimeInMillis();displayed.set(y,m-1,d,12,0,0);refresh();notifyDateChanged(previous);});this.month.setOnDateListener(listener::choose);
        header.setOnCommandListener(id->{if(id==WmCalendarHeader.PREVIOUS||id==WmCalendarHeader.NEXT){
            int y=displayed.get(Calendar.YEAR),m=displayed.get(Calendar.MONTH)+1;if(id==WmCalendarHeader.PREVIOUS&&y==this.minimum.year&&m==this.minimum.month||id==WmCalendarHeader.NEXT&&y==this.maximum.year&&m==this.maximum.month)return;
            clearChoices();long previous=displayed.getTimeInMillis();displayed.add(Calendar.MONTH,id==WmCalendarHeader.PREVIOUS?-1:1);clamp();refresh();notifyDateChanged(previous);
        }else if(id==WmCalendarHeader.YEAR_PICKER||id==WmCalendarHeader.MONTH_PICKER)choices(id);});refresh();this.month.requestFocus();
    }
    /** Preferred client size in SDK logical pixels, excluding the owner's outer frame.
     * The caller must clamp it to available space. The returned Point is a copy. */
    public android.graphics.Point naturalSize(){return month.naturalSize();}
    @Override protected void onMeasure(int width,int height){
        android.graphics.Point size=naturalSize();
        super.onMeasure(MeasureSpec.makeMeasureSpec(resolveSize(Math.max(getSuggestedMinimumWidth(),size.x),width),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(resolveSize(Math.max(getSuggestedMinimumHeight(),size.y),height),MeasureSpec.EXACTLY));
    }
    private void refresh(){header.setDate(displayed.get(Calendar.YEAR),displayed.get(Calendar.MONTH)+1,displayed.get(Calendar.DAY_OF_MONTH));month.setMonth(displayed.get(Calendar.YEAR),displayed.get(Calendar.MONTH)+1);month.setSelectedDate(displayed.get(Calendar.YEAR),displayed.get(Calendar.MONTH)+1,displayed.get(Calendar.DAY_OF_MONTH));positionYearEditor();}
    private void choices(int id){
        if(id==WmCalendarHeader.YEAR_PICKER){yearEditor();return;}
        clearChoices();choicesId=id;final int generation=choiceGeneration;
        String[] labels=java.util.Arrays.copyOf(new DateFormatSymbols(locale).getMonths(),12);
        WmRadioMenu menu=new WmRadioMenu(getContext(),font,labels,displayed.get(Calendar.MONTH),"",selection,index->{
            if(generation!=choiceGeneration)return;clearChoices();change(Calendar.MONTH,index);
        });choices=new WmScrollView(getContext(),menu);body.addView(choices,new FrameLayout.LayoutParams(-1,-1));
    }
    private void clamp(){int y=displayed.get(Calendar.YEAR),m=displayed.get(Calendar.MONTH)+1,d=displayed.get(Calendar.DAY_OF_MONTH);WmDateTimeField.Value edge=WmMonthCalendar.compareDate(y,m,d,minimum)<0?minimum:WmMonthCalendar.compareDate(y,m,d,maximum)>0?maximum:null;if(edge!=null)displayed.set(edge.year,edge.month-1,edge.day,12,0,0);}
    private void change(int field,int value){long previous=displayed.getTimeInMillis();int day=displayed.get(Calendar.DAY_OF_MONTH);displayed.set(Calendar.DAY_OF_MONTH,1);displayed.set(field,value);displayed.set(Calendar.DAY_OF_MONTH,Math.min(day,displayed.getActualMaximum(Calendar.DAY_OF_MONTH)));clamp();refresh();notifyDateChanged(previous);}
    private void notifyDateChanged(long previous){if(dateChanged!=null&&previous!=displayed.getTimeInMillis())dateChanged.choose(displayed.get(Calendar.YEAR),displayed.get(Calendar.MONTH)+1,displayed.get(Calendar.DAY_OF_MONTH));}
    private void yearEditor(){
        View previousFocus=getRootView().findFocus();clearChoices();choicesId=WmCalendarHeader.YEAR_PICKER;
        final int generation=choiceGeneration;
        WmMonthYearEditor editor=new WmMonthYearEditor(getContext(),font,locale,displayed.get(Calendar.YEAR),minimum.year,maximum.year,selection,new WmMonthYearEditor.Listener(){
            public void changed(int year){if(generation==choiceGeneration)change(Calendar.YEAR,year);}
            public void finished(boolean valid,int year){
                if(generation!=choiceGeneration)return;
                clearChoices();if(valid)change(Calendar.YEAR,year);
                if(previousFocus==null||previousFocus.getWindowToken()==null||!previousFocus.requestFocus())month.requestFocus();
            }
        });choices=editor;headerHost.addView(editor,new FrameLayout.LayoutParams(0,0));positionYearEditor();editor.input().requestFocus();editor.input().selectAll();
    }
    private void positionYearEditor(){
        if(!(choices instanceof WmMonthYearEditor))return;
        // Font-derived size is valid before header layout; 0x0 drops restored input focus.
        Rect bounds=header.yearEditorBounds();FrameLayout.LayoutParams old=(FrameLayout.LayoutParams)choices.getLayoutParams();
        if(old.leftMargin==bounds.left&&old.topMargin==bounds.top&&old.width==bounds.width()&&old.height==bounds.height())return;
        FrameLayout.LayoutParams fit=new FrameLayout.LayoutParams(bounds.width(),bounds.height());fit.leftMargin=bounds.left;fit.topMargin=bounds.top;choices.setLayoutParams(fit);
    }
    // Package owner may suspend nested focus before moving this popup between layers.
    void clearChoices(){choiceGeneration++;if(choices!=null&&choices.getParent() instanceof ViewGroup)((ViewGroup)choices.getParent()).removeView(choices);choices=null;}
    /** Native inline editor commits on exit, including ESC/Back; month menu dismisses. */
    public boolean back(){if(choices==null)return false;if(choices instanceof WmMonthYearEditor)((WmMonthYearEditor)choices).finish();else clearChoices();return true;}
    /** DOWN outside the inline year editor commits it and consumes through
     * UP/CANCEL, requiring another tap for the underlying target. Inside-start
     * gestures route to the editor with local coordinates. Android dispatch hook. */
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){
            swallowedYearTouch=false;yearTouchTarget=null;
            if(choices instanceof WmMonthYearEditor){
                editorHit.set(0,0,choices.getWidth(),choices.getHeight());offsetDescendantRectToMyCoords(choices,editorHit);
                if(!editorHit.contains((int)event.getX(),(int)event.getY())){((WmMonthYearEditor)choices).finish();swallowedYearTouch=true;}else yearTouchTarget=choices;
            }
        }
        if(swallowedYearTouch){if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)swallowedYearTouch=false;return true;}
        if(yearTouchTarget!=null){
            View target=yearTouchTarget;MotionEvent local=MotionEvent.obtain(event);local.offsetLocation(-editorHit.left,-editorHit.top);target.dispatchTouchEvent(local);local.recycle();
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)yearTouchTarget=null;return true;
        }
        return super.dispatchTouchEvent(event);
    }
    /** Nested interaction only; the owner separately preserves the selected date. */
    public Bundle saveInteractionState(){
        String choice="none",text="";int scroll=0,start=0,end=0;
        if(choices instanceof WmMonthYearEditor){choice="year";android.widget.EditText input=((WmMonthYearEditor)choices).input();text=input.getText().toString();start=Math.max(0,Math.min(input.getSelectionStart(),text.length()));end=Math.max(0,Math.min(input.getSelectionEnd(),text.length()));}
        else if(choices instanceof WmScrollView){choice="month";scroll=((WmScrollView)choices).getRequestedScrollPosition();}
        Bundle state=new Bundle();state.putInt("schema",1);state.putString("choice",choice);state.putInt("scroll",scroll);state.putString("text",text);state.putInt("start",start);state.putInt("end",end);return state;
    }
    /** Pending year text may be empty or out of range; validation never commits it. */
    public static void validateInteractionState(Bundle state){
        if(state==null||state.size()!=6||!(state.get("schema") instanceof Integer)||((Integer)state.get("schema"))!=1||!(state.get("choice") instanceof String)||!(state.get("scroll") instanceof Integer)||!(state.get("text") instanceof String)||!(state.get("start") instanceof Integer)||!(state.get("end") instanceof Integer))throw new IllegalArgumentException("Calendar interaction schema1 required");
        String choice=(String)state.get("choice"),text=(String)state.get("text");int scroll=(Integer)state.get("scroll"),start=(Integer)state.get("start"),end=(Integer)state.get("end");
        if(!"none".equals(choice)&&!"month".equals(choice)&&!"year".equals(choice)||scroll<0||!"month".equals(choice)&&scroll!=0||start<0||end<0||start>text.length()||end>text.length()||!"year".equals(choice)&&(!text.isEmpty()||start!=0||end!=0))throw new IllegalArgumentException("Invalid Calendar interaction");
        for(int i=0;i<text.length();i++)if(text.charAt(i)<'0'||text.charAt(i)>'9')throw new IllegalArgumentException("Pending ASCII year required");
    }
    /** Silent replacement, with generation invalidation before old focus is removed. */
    public void restoreInteractionState(Bundle state){
        validateInteractionState(state);String choice=state.getString("choice");swallowedYearTouch=false;yearTouchTarget=null;
        if("none".equals(choice))clearChoices();
        else if("month".equals(choice)){choices(WmCalendarHeader.MONTH_PICKER);((WmScrollView)choices).scrollToPosition(state.getInt("scroll"));}
        else{yearEditor();android.widget.EditText input=((WmMonthYearEditor)choices).input();input.setText(state.getString("text"));input.setSelection(state.getInt("start"),state.getInt("end"));}
    }
    /** MCN_SELCHANGE-style heading/edit updates; day confirmation still uses constructor listener. */
    public void setOnDateChangeListener(OnDateListener listener){dateChanged=listener;}
    /** Adds/removes the owner-controlled None footer; a None tap leaves date() unchanged. */
    public void setOnNoneListener(Runnable listener){month.setOnNoneListener(listener);}
    /** Direct Week monthcal follows regional first day and optional week gutter. */
    public void setFirstDayOfWeek(int day){month.setFirstDayOfWeek(day);}
    public void setWeekNumbersVisible(boolean visible){month.setWeekNumbersVisible(visible);}
    public int[] date(){return new int[]{displayed.get(Calendar.YEAR),displayed.get(Calendar.MONTH)+1,displayed.get(Calendar.DAY_OF_MONTH)};}
    public void setLocale(Locale locale){if(locale==null)throw new IllegalArgumentException("locale required");this.locale=locale;header.setLocale(locale);month.setLocale(locale);if(choices instanceof WmMonthYearEditor){((WmMonthYearEditor)choices).setLocale(locale);positionYearEditor();}else if(choices!=null){int scroll=((WmScrollView)choices).getRequestedScrollPosition();choices(choicesId);((WmScrollView)choices).scrollToPosition(scroll);}}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){font=regular;this.selection=selection;header.onWmRestyle(regular,bold,selection,tabColor);month.onWmRestyle(regular,bold,selection,tabColor);if(choices!=null)WmControls.restyleTree(choices,regular,bold,selection,tabColor);positionYearEditor();}
}
