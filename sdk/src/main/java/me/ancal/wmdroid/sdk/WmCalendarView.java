package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/** Shared DAYVIEW/WEEKVIEW presentation. All coordinates are logical VGA pixels.
 * caldll 0290F1C4 supplies font measurements; 02903454 supplies the pinned week
 * header; 0290C5AC supplies density-first lanes. This is not a WinCE control ABI.
 * The app supplies clipped civil-day records, owns their tags and opens cards.
 * Native week date-header selection (029041B8) occurs on DOWN; this shared
 * capacitive control commits on a completed same-column UP instead.
 * Native GDI metrics, category colors, overflow and drag editing are partial.
 */
public final class WmCalendarView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener {
    public static final int DAY=1,WEEK=7;
    public static final class Event {
        public static final int FREE=0,TENTATIVE=1,BUSY=2,OUT_OF_OFFICE=3;
        public final int day,startMinute,endMinute,busyStatus;
        public final boolean allDay;
        public final String title,detail;
        public final Object tag;
        public final int symbolMask;
        public final boolean recurrenceException;
        public final String recordId;
        public final boolean hasSourceTimes;
        public final long sourceStartMillis,sourceEndMillis;
        /** day is a zero-based offset from setRange; end is exclusive. */
        public Event(int day,int startMinute,int endMinute,boolean allDay,String title,String detail,Object tag){
            this(day,startMinute,endMinute,allDay,title,detail,tag,0,false);
        }
        /** Caller adapts record metadata and future reminder eligibility; no provider access here. */
        public Event(int day,int startMinute,int endMinute,boolean allDay,String title,String detail,Object tag,int symbolMask,boolean recurrenceException){
            this(day,startMinute,endMinute,allDay,title,detail,tag,symbolMask,recurrenceException,BUSY);
        }
        /** Native appointment status0..3; legacy callers default to BUSY as new WM appointments do. */
        public Event(int day,int startMinute,int endMinute,boolean allDay,String title,String detail,Object tag,int symbolMask,boolean recurrenceException,int busyStatus){
            this(day,startMinute,endMinute,allDay,title,detail,tag,symbolMask,recurrenceException,busyStatus,null,0,0,false);
        }
        /** Caller-scoped record identity and unclipped occurrence endpoints in epoch milliseconds. */
        public Event(int day,int startMinute,int endMinute,boolean allDay,String title,String detail,Object tag,int symbolMask,boolean recurrenceException,int busyStatus,String recordId,long sourceStartMillis,long sourceEndMillis){
            this(day,startMinute,endMinute,allDay,title,detail,tag,symbolMask,recurrenceException,busyStatus,recordId,sourceStartMillis,sourceEndMillis,true);
        }
        private Event(int day,int startMinute,int endMinute,boolean allDay,String title,String detail,Object tag,int symbolMask,boolean recurrenceException,int busyStatus,String recordId,long sourceStartMillis,long sourceEndMillis,boolean hasSourceTimes){
            if(hasSourceTimes&&(recordId==null||recordId.isEmpty()||sourceEndMillis<sourceStartMillis))throw new IllegalArgumentException("Invalid Calendar source record");
            this.recordId=recordId;this.hasSourceTimes=hasSourceTimes;this.sourceStartMillis=sourceStartMillis;this.sourceEndMillis=sourceEndMillis;
            if(busyStatus<FREE||busyStatus>OUT_OF_OFFICE)throw new IllegalArgumentException("Invalid Calendar busy status");
            this.busyStatus=busyStatus;
            if(day<0||day>=7||startMinute<0||startMinute>=1440||endMinute<startMinute||endMinute>1440)throw new IllegalArgumentException("invalid calendar event");
            if(title==null||detail==null)throw new IllegalArgumentException("calendar captions required");
            if(symbolMask<0||symbolMask>63)throw new IllegalArgumentException("Invalid Calendar symbol mask");
            this.day=day;this.startMinute=startMinute;this.endMinute=endMinute;this.allDay=allDay;this.title=title;this.detail=detail;this.tag=tag;this.symbolMask=symbolMask;this.recurrenceException=recurrenceException;
        }
    }
    public interface OnEventListener { void onSelected(Event event); void onOpen(Event event); }
    public interface OnDateListener { void onDate(int year,int month,int day); }
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Calendar first=new GregorianCalendar();
    private final Header header;
    private final Grid grid;
    private final Details details;
    private final WmScrollView scroll;
    private final WmScrollView headerScroll;
    private Typeface font;
    private Locale locale;
    private String noAppointments,noSelected;
    private int days=DAY,interval=60,rowHeight,axisWidth,listSpacing;
    private int selection=0xff000080,face=WmControls.FACE,eventColor=Color.WHITE;
    private boolean hour24,showWeekText;
    private List<Event> events=Collections.emptyList();
    private final List<Placement> placements=new ArrayList<Placement>();
    private boolean placementDirty=true;
    private Event selected;
    private OnEventListener listener;
    public interface OnTimeListener { void onSelected(int day,int startMinute,int endMinute); }
    private OnTimeListener timeListener;
    private int timeDay=-1,timeStart,timeEnd,keyAnchor=-1,keyRow,weekHorizontalKey,lastTimeStartRow,lastTimeEndRow;
    private OnDateListener dateListener;
    public static final class WeekNavigation {
        public final int direction,startRow,endRow;
        public final boolean preferAllDay;
        private WeekNavigation(int direction,boolean preferAllDay,int startRow,int endRow){this.direction=direction;this.preferAllDay=preferAllDay;this.startRow=startRow;this.endRow=endRow;}
    }
    public interface OnNavigateListener { void onNavigate(int direction); }
    private OnNavigateListener navigateListener;
    // caldll0290B5BC creates four COLORREF brushes; convert BGR to Android ARGB.
    private static final int[] BUSY_COLORS={0xffffffff,0xffc8c8f0,0xff9696e1,0xffd3b0eb};
    private final int[] allDayBusy=new int[7];
    private int workingDayMask=0x3e,workStart=480,workEnd=1020;
    private final boolean[] workingDays=new boolean[7];
    private int displayIconMask=WmCalendarSymbols.ALL;
    private final WmCalendarSymbols[] symbols=new WmCalendarSymbols[6];
    private WmCalendarSymbols recurrenceExceptionSymbol;

    public WmCalendarView(Context context,Typeface font,Locale locale){
        super(context);if(font==null||locale==null)throw new IllegalArgumentException("font and locale required");
        this.font=font;this.locale=locale;updateCaptions(WmStrings.language(context,locale.toString().replace('_','-')));hour24=android.text.format.DateFormat.is24HourFormat(context);
        setOrientation(VERTICAL);setFocusableInTouchMode(true);setBackgroundColor(Color.WHITE);lastTimeStartRow=lastTimeEndRow=first.get(Calendar.HOUR_OF_DAY);first.set(Calendar.HOUR_OF_DAY,12);
        header=new Header(context);grid=new Grid(context);details=new Details(context);
        measureFont();scroll=new WmScrollView(context,grid,getContentHeight());headerScroll=new WmScrollView(context,header,1);
        addView(headerScroll,new LayoutParams(-1,0));addView(scroll,new LayoutParams(-1,0,1));addView(details,new LayoutParams(-1,0));
        scroll.getViewTreeObserver().addOnScrollChangedListener(()->{if(days!=DAY&&selected!=null&&selected.allDay)grid.invalidate();});
        scroll.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(days!=DAY&&selected!=null&&selected.allDay)grid.invalidate();});
        updateGeometry();
    }
    public void setRange(int year,int month,int day,int days){
        if(days!=DAY&&days!=5&&days!=6&&days!=WEEK)throw new IllegalArgumentException("day count must be 1, 5, 6 or 7");
        Calendar date=new GregorianCalendar();date.setLenient(false);date.clear();date.set(year,month-1,day,12,0,0);date.getTime();
        this.days=days;first.setTimeInMillis(date.getTimeInMillis());events=Collections.emptyList();selected=null;timeDay=-1;keyAnchor=-1;keyRow=0;cancelGestures();updateGeometry();headerScroll.scrollToPosition(0);scrollToMinute(0);
    }
    public void setEvents(List<Event> values){
        if(values==null)throw new IllegalArgumentException("events required");
        List<Event> copy=new ArrayList<Event>(values);
        for(Event value:copy)if(value==null||value.day>=days)throw new IllegalArgumentException("event outside calendar range");
        events=Collections.unmodifiableList(copy);selected=null;timeDay=-1;keyAnchor=-1;keyRow=0;cancelGestures();updateGeometry();
    }
    public void setOnEventListener(OnEventListener listener){this.listener=listener;}
    /** Selects a supplied record without callbacks; null clears, an absent record returns false. */
    public boolean selectEvent(Event event){
        if(event!=null&&!events.contains(event))return false;
        cancelGestures();timeDay=-1;keyAnchor=-1;selected=event;
        if(event!=null){
            keyRow=event.startMinute/interval;
            if(!event.allDay)ensureRowVisible(keyRow);
            else if(days==DAY){
                int row=0;for(Event value:events){if(value==event)break;if(value.allDay)row++;}
                int top=row*listSpacing,bottom=top+listSpacing,offset=headerScroll.getRequestedScrollPosition();
                if(top<offset)headerScroll.scrollToPosition(top);
                else if(bottom>offset+headerScroll.getHeight())headerScroll.scrollToPosition(Math.max(0,Math.min(top,bottom-headerScroll.getHeight())));
            }
            requestFocus();
        }
        invalidateViews();return true;
    }
    /** Current supplied record identity; null for a time cursor or no selection. */
    public Event getSelectedEvent(){return selected;}
    /** Capture before deleting the selected Week record; no selection, scroll or callback changes. */
    public Event getWeekRemovalSuccessor(){
        if(days==DAY)throw new IllegalStateException("Week removal requires multiple columns");
        if(selected==null)return null;
        List<Event> column=weekColumn(selected.day);
        int at=column.indexOf(selected);return at+1<column.size()?column.get(at+1):at>0?column.get(at-1):null;
    }
    public void setOnTimeListener(OnTimeListener listener){timeListener=listener;}
    /** Defensive day offset/start minute/exclusive end; null for an appointment or no selection. */
    public int[] getTimeSelection(){return timeDay<0?null:new int[]{timeDay,timeStart,timeEnd};}
    public void setTimeSelection(int day,int startMinute,int endMinute){
        if(day<0||day>=days||startMinute<0||endMinute>1440||startMinute>=endMinute||startMinute%interval!=0||endMinute%interval!=0)throw new IllegalArgumentException("Invalid Calendar time selection");
        timeDay=day;timeStart=startMinute;timeEnd=endMinute;keyAnchor=startMinute/interval;keyRow=endMinute/interval-1;selected=null;rememberTimeRows();cancelGestures();invalidateViews();
    }

    /** Inclusive raw rows, independent of the visible selection and current interval. */
    public int[] getTimeCursorRows(){return new int[]{lastTimeStartRow,lastTimeEndRow};}
    /** Restore owner state only; boundary fallback clamps rows to the current interval. */
    public void setTimeCursorRows(int startRow,int endRow){
        if(startRow<0||endRow<startRow||endRow>=48)throw new IllegalArgumentException("Invalid Calendar cursor rows");
        lastTimeStartRow=startRow;lastTimeEndRow=endRow;
    }

    /** Initial/view-activation Week policy; ordinary boundary navigation uses restoreWeekNavigation. */
    public void selectInitialWeek(int preferredColumn,long nowMillis){selectPopulatedWeek(preferredColumn,nowMillis,true);}
    /** After setEvents replaces records, prefer explicit selectEvent; otherwise use
     * this no-current-time automatic policy. Initial display uses selectInitialWeek,
     * boundary navigation uses capture/restoreWeekNavigation. Owner loads data. */
    public void selectPopulatedWeek(int preferredColumn){selectPopulatedWeek(preferredColumn,0,false);}
    private void selectPopulatedWeek(int preferredColumn,long nowMillis,boolean initial){
        if(days==DAY)throw new IllegalStateException("Week selection requires multiple columns");
        if(preferredColumn<0||preferredColumn>=days)throw new IllegalArgumentException("column outside range");
        layoutEvents();int start=preferredColumn;
        while(start<days&&!hasColumnRecords(start))start++;
        if(start==days){start=preferredColumn;while(start>=0&&!hasColumnRecords(start))start--;}
        long order=0;boolean minuteStart=false;
        if(initial){Calendar now=(Calendar)first.clone();now.setTimeInMillis(nowMillis);order=civilMinuteOrder(now);minuteStart=now.get(Calendar.SECOND)==0&&now.get(Calendar.MILLISECOND)==0;}
        for(int day=start;day>=0&&day<days;day++){
            Event next=null;
            for(Placement place:placements)if(place.event.day==day){
                next=place.event;
                if(!initial||day!=preferredColumn||place.sourceOrder>order||(place.sourceOrder==order&&minuteStart))break;
            }
            if(next==null)for(Event event:events)if(event.day==day&&event.allDay){next=event;break;}
            if(next!=null){selectEvent(next);if(listener!=null)listener.onSelected(next);return;}
        }
        int last=1440/interval-1,startRow=Math.min(last,lastTimeStartRow),endRow=Math.max(startRow,Math.min(last,lastTimeEndRow));
        setTimeSelection(preferredColumn,startRow*interval,(endRow+1)*interval);ensureRowVisible(endRow);notifyTime();
    }
    private boolean hasColumnRecords(int day){for(Event event:events)if(event.day==day)return true;return false;}
    private static long civilMinuteOrder(Calendar date){
        long year=date.get(Calendar.YEAR);if(date.get(Calendar.ERA)==GregorianCalendar.BC)year=1-year;
        long day=(year*13+date.get(Calendar.MONTH))*32+date.get(Calendar.DAY_OF_MONTH);
        return (day*24+date.get(Calendar.HOUR_OF_DAY))*60+date.get(Calendar.MINUTE);
    }

    /** Capture before the owner replaces the range/events; rows are inclusive and retain the last time cursor. */
    public WeekNavigation captureWeekNavigation(int direction){
        if(days==DAY)throw new IllegalStateException("Week navigation requires multiple columns");
        if(direction!=-1&&direction!=1)throw new IllegalArgumentException("direction must be -1 or 1");
        int start=Math.max(0,Math.min(1440/interval-1,lastTimeStartRow));
        int end=Math.max(start,Math.min(1440/interval-1,lastTimeEndRow));
        return new WeekNavigation(direction,selected!=null&&selected.allDay,start,end);
    }
    /** Apply after the owner loads a new week. Native boundary selection is first/last eligible, not nearest-time. */
    public void restoreWeekNavigation(WeekNavigation navigation){
        if(navigation==null)throw new IllegalArgumentException("Week navigation required");
        if(days==DAY)throw new IllegalStateException("Week navigation requires multiple columns");
        lastTimeStartRow=Math.min(1440/interval-1,navigation.startRow);lastTimeEndRow=Math.max(lastTimeStartRow,Math.min(1440/interval-1,navigation.endRow));
        layoutEvents();int step=navigation.direction,edge=step>0?0:days-1;
        for(int day=edge;day>=0&&day<days;day+=step){
            Event allDay=null,timed=null;
            for(Event event:events)if(event.day==day&&event.allDay){allDay=event;break;}
            for(Placement place:placements)if(place.event.day==day){timed=place.event;if(step>0)break;}
            Event next=navigation.preferAllDay?(allDay!=null?allDay:timed):(timed!=null?timed:allDay);
            if(next!=null){selectEvent(next);if(listener!=null)listener.onSelected(next);return;}
        }
        int last=1440/interval-1,start=Math.min(last,navigation.startRow),end=Math.max(start,Math.min(last,navigation.endRow));
        setTimeSelection(edge,start*interval,(end+1)*interval);ensureRowVisible(end);notifyTime();
    }
    /** Day lane exhaustion or Week range exhaustion requests owner date navigation. */
    public void setOnNavigateListener(OnNavigateListener listener){navigateListener=listener;}
    public void setOnDateListener(OnDateListener listener){header.cancelGesture();dateListener=listener;}
    public void setDisplayIconMask(int mask){if(mask<0||mask>63)throw new IllegalArgumentException("Invalid Calendar icon display mask");displayIconMask=mask;cancelGestures();invalidateViews();}
    public int getDisplayIconMask(){return displayIconMask;}
    public int getDayCount(){return days;}
    /** Native work-week mask: bit0 Sunday through bit6 Saturday; half-hour boundaries, end exclusive. */
    public void setWorkingHours(int dayMask,int startMinute,int endMinute){
        if(dayMask<=0||dayMask>127||startMinute<0||endMinute>=1440||startMinute>=endMinute||startMinute%30!=0||endMinute%30!=0)throw new IllegalArgumentException("Invalid Calendar working hours");
        workingDayMask=dayMask;workStart=startMinute;workEnd=endMinute;cancelGestures();updateWorkingDays();grid.invalidate();
    }
    private void updateWorkingDays(){
        Calendar date=(Calendar)first.clone();
        for(int day=0;day<days;day++){workingDays[day]=(workingDayMask&(1<<(date.get(Calendar.DAY_OF_WEEK)-Calendar.SUNDAY)))!=0;date.add(Calendar.DAY_OF_MONTH,1);}
    }
    /** Defensive pinned-header bounds in this view's coordinates; Day has no date header. */
    public RectF dateHeaderBounds(int column){
        if(column<0||column>=days)throw new IllegalArgumentException("column outside range");
        return days==DAY?null:new RectF(columnLeft(column),0,columnLeft(column+1),getHeaderHeight());
    }
    /** Native style bit2 selects 30-minute rows; otherwise the default is 60. */
    public void setInterval(int minutes){if(minutes!=30&&minutes!=60)throw new IllegalArgumentException("interval must be 30 or 60");if(minutes==interval)return;float minute=currentMinute();int cursorMinute=keyRow*interval;interval=minutes;keyRow=Math.min(1440/interval-1,cursorMinute/interval);if(timeDay>=0){timeStart=timeStart/interval*interval;timeEnd=timeStart+interval;keyAnchor=keyRow=timeStart/interval;rememberTimeRows();}cancelGestures();updateGeometry();restoreMinute(minute);}
    public void setLocale(Locale locale){if(locale==null)throw new IllegalArgumentException("locale required");this.locale=locale;updateCaptions(WmStrings.language(getContext(),locale.toString().replace('_','-')));cancelGestures();invalidateViews();}
    private void updateCaptions(String language){
        noAppointments=WmStrings.text(getContext(),language,"약속 없음","calendar.agenda.empty");
        noSelected=WmStrings.text(getContext(),language,"선택된 약속이 없습니다.","calendar.week.none_selected");
    }
    /** Authored captions only; caller Locale, records, selection, gestures and scroll remain unchanged. */
    @Override public void onWmLanguageChanged(String language){updateCaptions(WmStrings.language(getContext(),language));details.invalidate();}
    public void set24Hour(boolean enabled){hour24=enabled;grid.invalidate();}
    /** The original ShowTextInWeekView preference defaults to false. */
    public void setShowWeekText(boolean enabled){showWeekText=enabled;placementDirty=true;cancelGestures();grid.invalidate();}
    public void setColors(int selection,int face,int eventColor){this.selection=selection;this.face=face;this.eventColor=eventColor;invalidateViews();}
    public int getRowHeight(){return rowHeight;}
    public int getTimeColumnWidth(){return axisWidth;}
    public int getHeaderHeight(){return days!=DAY?36:Math.min(allDayCount(),6)*listSpacing;}
    public int getContentHeight(){return (1440/interval)*rowHeight;}
    public int[] dateAt(int column){if(column<0||column>=days)throw new IllegalArgumentException("column outside range");Calendar date=(Calendar)first.clone();date.add(Calendar.DAY_OF_MONTH,column);return new int[]{date.get(Calendar.YEAR),date.get(Calendar.MONTH)+1,date.get(Calendar.DAY_OF_MONTH)};}
    public void scrollToMinute(int minute){scrollToMinute((float)minute);}
    /** Fractional civil-minute position survives options-driven range rebuilding. */
    public void scrollToMinute(float minute){if(Float.isNaN(minute)||Float.isInfinite(minute))throw new IllegalArgumentException("finite minute required");restoreMinute(minute);}
    public float getScrollMinute(){return currentMinute();}
    /** Defensive content-space bounds, excluding pinned header and scroll offset. */
    public RectF eventBounds(Event event){layoutEvents();for(Placement place:placements)if(place.event==event)return new RectF(place.bounds);return null;}
    private float currentMinute(){return scroll==null?0:scroll.getRequestedScrollPosition()*interval/(float)Math.max(1,rowHeight);}
    private void restoreMinute(float minute){scroll.scrollToPosition(Math.round(Math.max(0,Math.min(1440,minute))*rowHeight/interval));}
    private int allDayCount(){int count=0;for(Event event:events)if(event.allDay)count++;return count;}
    private void measureFont(){
        paint.setTypeface(font);paint.setTextSize(WmMetrics.FONT);paint.setTextAlign(Paint.Align.LEFT);
        Paint.FontMetrics fm=paint.getFontMetrics();
        // Native measures "0" with GetTextExtentPoint; Android's analogous line
        // extent differs from GDI. The original minimum28 + padding6 is retained.
        int height=(int)Math.ceil(fm.descent-fm.ascent);
        rowHeight=Math.max(height,28)+6;axisWidth=(int)Math.ceil(WmFonts.measure(paint,"12"))+6;
        // Native report ListView adds CYEDGE after the font-spacing request.
        // Image minima start at0; Android extent still substitutes for GDI role52.
        listSpacing=height+WmMetrics.BORDER+2*WmMetrics.EDGE;
    }
    private void updateGeometry(){
        // 0290C334 ranks BUSY as4, takes the maximum, then maps4 back to2.
        updateWorkingDays();
        java.util.Arrays.fill(allDayBusy,-1);
        for(Event event:events)if(event.allDay)allDayBusy[event.day]=Math.max(allDayBusy[event.day],event.busyStatus==Event.BUSY?4:event.busyStatus);
        for(int day=0;day<allDayBusy.length;day++)if(allDayBusy[day]==4)allDayBusy[day]=Event.BUSY;
        placementDirty=true;if(scroll!=null)scroll.setContentHeight(getContentHeight());
        if(headerScroll!=null){headerScroll.setContentHeight(days!=DAY?36:Math.max(1,allDayCount()*listSpacing));headerScroll.getLayoutParams().height=getHeaderHeight();headerScroll.requestLayout();}
        if(details.getLayoutParams()!=null){details.getLayoutParams().height=days!=DAY?detailsHeight():0;details.requestLayout();}
        invalidateViews();
    }
    private int detailsHeight(){Paint.FontMetrics fm=paint.getFontMetrics();return 2*(int)Math.ceil(fm.descent-fm.ascent)+(int)Math.ceil(fm.leading)+10;}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        float minute=currentMinute(),allDayRow=headerScroll.getRequestedScrollPosition()/(float)Math.max(1,listSpacing);
        font=regular;this.selection=selection;face=WmPalette.face(selection);cancelGestures();measureFont();updateGeometry();restoreMinute(minute);
        if(days==DAY)headerScroll.scrollToPosition(Math.round(allDayRow*listSpacing));
    }
    private void cancelGestures(){weekHorizontalKey=0;grid.cancelGesture();header.cancelGesture();}
    @Override public void setEnabled(boolean enabled){
        super.setEnabled(enabled);if(grid==null||headerScroll==null)return;
        cancelGestures();grid.setEnabled(enabled);header.setEnabled(enabled);
        scroll.setEnabled(enabled);headerScroll.setEnabled(enabled);invalidateViews();
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event){return isEnabled()&&super.dispatchTouchEvent(event);}
    /** caldll0291293C routes row keys before scrolling; Day arrows use02910F04's appointment/empty-row index. */
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        int key=event.getKeyCode();
        if(days!=DAY&&(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)&&!event.isAltPressed()&&!event.isCtrlPressed()){
            if(!isEnabled())return false;
            if(event.getAction()==KeyEvent.ACTION_UP&&weekHorizontalKey==key){weekHorizontalKey=0;return true;}
            if(event.getAction()==KeyEvent.ACTION_DOWN){
                cancelGestures();
                if(moveWeekHorizontal(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1)){weekHorizontalKey=key;return true;}
            }
            return super.dispatchKeyEvent(event);
        }
        boolean horizontal=days==DAY&&(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT);
        boolean rowKey=horizontal||key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_PAGE_UP||key==KeyEvent.KEYCODE_PAGE_DOWN||key==KeyEvent.KEYCODE_MOVE_HOME||key==KeyEvent.KEYCODE_MOVE_END;
        if(!rowKey||event.isAltPressed()||event.isCtrlPressed())return super.dispatchKeyEvent(event);
        if(!isEnabled())return false;
        if(event.getAction()==KeyEvent.ACTION_UP)return true;
        if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
        cancelGestures();
        if(horizontal){moveDayHorizontal(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1);return true;}
        if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN){
            int direction=key==KeyEvent.KEYCODE_DPAD_DOWN?1:-1;
            if(days==DAY)moveDaySelection(direction);else moveWeekSelection(direction);
            return true;
        }
        int page=Math.max(1,scroll.getHeight()/Math.max(1,rowHeight)),last=1440/interval-1;
        int next=key==KeyEvent.KEYCODE_MOVE_HOME?0:key==KeyEvent.KEYCODE_MOVE_END?last:
            keyRow+(key==KeyEvent.KEYCODE_DPAD_UP?-1:key==KeyEvent.KEYCODE_DPAD_DOWN?1:key==KeyEvent.KEYCODE_PAGE_UP?-page:page);
        next=Math.max(0,Math.min(last,next));
        int day=timeDay>=0?timeDay:selected!=null?selected.day:0;
        if(!event.isShiftPressed()||keyAnchor<0)keyAnchor=next;
        keyRow=next;timeDay=day;timeStart=Math.min(keyAnchor,next)*interval;timeEnd=(Math.max(keyAnchor,next)+1)*interval;selected=null;
        ensureRowVisible(next);invalidateViews();notifyTime();return true;
    }
    private void rememberTimeRows(){lastTimeStartRow=timeStart/interval;lastTimeEndRow=Math.max(lastTimeStartRow,timeEnd/interval-1);}
    private void notifyTime(){if(timeDay>=0)rememberTimeRows();if(timeListener!=null)timeListener.onSelected(timeDay,timeStart,timeEnd);}
    private void ensureRowVisible(int row){
        int firstRow=(int)(currentMinute()/interval),page=Math.max(1,scroll.getHeight()/Math.max(1,rowHeight));
        if(row<firstRow)scrollToMinute(row*interval);else if(row>=firstRow+page)scrollToMinute((row-page+1)*interval);
    }
    private static final class KeySlot {
        final Event event;final int row;
        KeySlot(Event event,int row){this.event=event;this.row=row;}
    }
    private void moveDaySelection(int direction){
        layoutEvents();List<Event> timed=new ArrayList<Event>();for(Placement place:placements)timed.add(place.event);
        List<KeySlot> slots=new ArrayList<KeySlot>();int row=0,index=0,count=1440/interval;
        while(row<count){
            if(index<timed.size()&&timed.get(index).startMinute/interval<=row){
                int end=row+1;
                do{Event event=timed.get(index++);slots.add(new KeySlot(event,event.startMinute/interval));end=Math.max(end,Math.max(event.startMinute/interval+1,(event.endMinute+interval-1)/interval));}
                while(index<timed.size()&&timed.get(index).startMinute/interval<end);
                row=end;
            }else slots.add(new KeySlot(null,row++));
        }
        int current=-1;
        for(int n=0;n<slots.size();n++){KeySlot slot=slots.get(n);if(selected!=null?slot.event==selected:slot.event==null&&slot.row==keyRow){current=n;break;}}
        boolean exact=current>=0;
        if(current<0){
            for(int n=0;n<slots.size();n++){Event event=slots.get(n).event;if(event!=null&&event.startMinute/interval<=keyRow&&Math.max(event.startMinute/interval+1,(event.endMinute+interval-1)/interval)>keyRow){current=n;break;}}
            if(current<0)current=0;
        }
        int next=exact?Math.max(0,Math.min(slots.size()-1,current+direction)):current;
        KeySlot slot=slots.get(next);
        if(exact&&next==current){
            if(selected!=null){
                int firstRow=(int)(currentMinute()/interval),lastRow=Math.min(count-1,firstRow+Math.max(1,scroll.getHeight()/Math.max(1,rowHeight))-1);
                int endRow=Math.max(selected.startMinute/interval+1,(selected.endMinute+interval-1)/interval);
                if(direction>0&&lastRow<endRow)ensureRowVisible(lastRow+1);
                else if(direction<0&&firstRow>selected.startMinute/interval)ensureRowVisible(firstRow-1);
            }
            return;
        }
        ensureRowVisible(slot.row);
        if(slot.event!=null){if(selected!=slot.event)choose(slot.event);return;}
        keyAnchor=keyRow=slot.row;timeDay=0;timeStart=slot.row*interval;timeEnd=timeStart+interval;selected=null;invalidateViews();notifyTime();
    }
    private List<Event> weekColumn(int day){
        List<Event> column=new ArrayList<Event>();for(Event event:events)if(event.day==day)column.add(event);
        Collections.sort(column,(a,b)->{
            if(a.allDay!=b.allDay)return a.allDay?-1:1;
            long left=civilMinuteOrder(sourceStart(a)),right=civilMinuteOrder(sourceStart(b));
            return left<right?-1:left>right?1:b.busyStatus-a.busyStatus;
        });
        return column;
    }
    private void moveWeekSelection(int direction){
        List<Event> column=weekColumn(selected!=null?selected.day:timeDay>=0?timeDay:0);
        if(column.isEmpty())return;
        Event next=null;
        if(selected!=null){
            int index=column.indexOf(selected)+direction;
            next=column.get((index+column.size())%column.size());
        }else{
            int anchor=timeDay>=0?timeStart/interval:0;
            for(int index=direction>0?0:column.size()-1;index>=0&&index<column.size();index+=direction){
                Event event=column.get(index);
                // caldll02CE1E24 compares source hour with raw selected row, even in half-hour mode.
                int hour=sourceStart(event).get(Calendar.HOUR_OF_DAY);
                if(direction>0?hour>anchor:hour<anchor){next=event;break;}
            }
            if(next==null)next=column.get(direction>0?0:column.size()-1);
        }
        // shortcut: no WinCE VK_F20/F21 envelope; add boundary routing only when a host supplies it.
        choose(next);
        if(selected!=next)return;
        int firstRow=(int)(currentMinute()/interval);
        if(next.allDay){
            if(firstRow==0)ensureRowVisible(scroll.getHeight()/Math.max(1,rowHeight)/2+12);
        }else ensureWeekTimedVisible(next);
    }
    private void ensureWeekTimedVisible(Event event){
        int start=event.startMinute/interval,end=Math.max(start+1,(event.endMinute+interval-1)/interval);
        ensureRowVisible(start>=(int)(currentMinute()/interval)?end:start);
    }
    private boolean moveWeekHorizontal(int direction){
        layoutEvents();
        boolean allDay=selected!=null&&selected.allDay;
        int column=selected!=null?selected.day:timeDay>=0?timeDay:0;
        if(selected!=null&&!allDay){
            Event neighbor=weekTimedNeighbor(selected,direction);
            if(neighbor!=null){ensureWeekTimedVisible(neighbor);choose(neighbor);return true;}
        }
        // caldll029066B8 uses60*first selected raw row, including30-minute mode.
        int anchor=selected!=null?weekStartMinute(selected,sourceStart(selected)):timeDay>=0?60*(timeStart/interval):0;
        for(int day=column+direction;day>=0&&day<days;day+=direction){
            Event next=allDay?weekAllDay(day):weekTimedColumn(day,anchor,direction);
            if(next==null)next=allDay?weekTimedColumn(day,anchor,direction):weekAllDay(day);
            if(next!=null){if(!next.allDay)ensureWeekTimedVisible(next);choose(next);return true;}
        }
        if(navigateListener!=null){navigateListener.onNavigate(direction);return true;}
        return false;
    }
    private Event weekAllDay(int day){
        Event firstMatch=null;
        for(Event event:events)if(event.day==day&&event.allDay){
            if(firstMatch==null)firstMatch=event;
            if(selected!=null&&selected.allDay&&selected.recordId!=null&&selected.recordId.equals(event.recordId))return event;
        }
        return firstMatch;
    }
    private Calendar sourceStart(Event event){
        Calendar source=(Calendar)first.clone();
        if(event.hasSourceTimes)source.setTimeInMillis(event.sourceStartMillis);
        else{source.add(Calendar.DAY_OF_MONTH,event.day);source.set(Calendar.HOUR_OF_DAY,event.startMinute/60);source.set(Calendar.MINUTE,event.startMinute%60);}
        return source;
    }
    private int weekStartMinute(Event event,Calendar source){
        Calendar column=(Calendar)first.clone();column.add(Calendar.DAY_OF_MONTH,event.day);
        return source.get(Calendar.ERA)==column.get(Calendar.ERA)&&source.get(Calendar.YEAR)==column.get(Calendar.YEAR)
            &&source.get(Calendar.MONTH)==column.get(Calendar.MONTH)&&source.get(Calendar.DAY_OF_MONTH)==column.get(Calendar.DAY_OF_MONTH)
            ?60*source.get(Calendar.HOUR_OF_DAY)+source.get(Calendar.MINUTE):0;
    }
    private Event weekTimedColumn(int day,int anchor,int direction){
        Event next=null;int distance=9999;
        for(Placement place:placements)if(place.event.day==day){
            int delta=Math.abs(anchor-place.sourceMinute);
            if(delta<distance){distance=delta;next=place.event;}
        }
        // Android's minimum-width boxes can overlap; bound the native opposite-side lane walk.
        for(int remaining=placements.size();next!=null&&remaining>0;remaining--){
            Event adjacent=weekTimedNeighbor(next,-direction);
            if(adjacent==null)break;
            next=adjacent;
        }
        return next;
    }
    private Event weekTimedNeighbor(Event event,int direction){
        // Fresh native all-day nodes have empty rectangles; text-mode union failure stops the scan.
        if(showWeekText)for(Event candidate:events)if(candidate.allDay&&candidate.day==event.day)return null;
        RectF current=null;
        for(Placement place:placements)if(place.event==event){current=new RectF(place.bounds);current.union(place.busy);break;}
        if(current==null)return null;
        // caldll029065D4 expands one requested side by8px, unlike the Day10px route.
        if(direction>0)current.right+=8;else current.left=Math.max(0,current.left-8);
        for(Placement place:placements)if(place.event!=event&&place.event.day==event.day){
            RectF bounds=new RectF(place.bounds);bounds.union(place.busy);
            if(RectF.intersects(current,bounds))return place.event;
        }
        return null;
    }
    private void moveDayHorizontal(int direction){
        // caldll029111F0 expands only the requested side by5*(horizontalDPI)/96 (10px at logical192dpi).
        if(selected!=null&&!selected.allDay){
            layoutEvents();RectF current=null;
            for(Placement place:placements)if(place.event==selected){current=new RectF(place.bounds);current.union(place.busy);break;}
            if(current!=null){
                if(direction>0)current.right+=10;else current.left=Math.max(0,current.left-10);
                for(Placement place:placements)if(place.event!=selected){
                    RectF bounds=new RectF(place.bounds);bounds.union(place.busy);
                    if(RectF.intersects(current,bounds)){choose(place.event);return;}
                }
            }
        }
        if(navigateListener!=null)navigateListener.onNavigate(direction);
    }
    private void invalidateViews(){header.invalidate();grid.invalidate();details.invalidate();}
    private float columnLeft(int column){return axisWidth+(days!=DAY?4:0)+(grid.getWidth()-axisWidth-(days!=DAY?4:0))*column/days;}
    private void font(){paint.setStyle(Paint.Style.FILL);paint.setTypeface(font);paint.setTextSize(WmMetrics.FONT);paint.setTextAlign(Paint.Align.LEFT);paint.setStrokeWidth(1);}
    private void choose(Event event){timeDay=-1;keyAnchor=-1;keyRow=event.startMinute/interval;selected=event;grid.invalidate();header.invalidate();details.invalidate();if(listener!=null)listener.onSelected(event);}
    private void open(Event event){if(listener!=null)listener.onOpen(event);}
    private int drawSymbols(Canvas canvas,Event event,RectF box){
        int shown=event.symbolMask&displayIconMask,consumed=0,x=(int)box.right-1,y=(int)box.top+(-6-28+rowHeight)/2;
        // Native PIMUTIL188 consumes low draw-mask bits right-to-left; reminder follows.
        int[] order={5,4,3,2,1,0};
        for(int index:order)if((shown&(1<<index))!=0){
            WmCalendarSymbols symbol;
            if(index==1&&event.recurrenceException){if(recurrenceExceptionSymbol==null)recurrenceExceptionSymbol=WmCalendarSymbols.forEvent(getContext(),2,true);symbol=recurrenceExceptionSymbol;}
            else{if(symbols[index]==null)symbols[index]=WmCalendarSymbols.forEvent(getContext(),1<<index,false);symbol=symbols[index];}
            symbol.setBounds(x-32,y,x,y+28);symbol.draw(canvas);x-=34;consumed+=34;
        }
        return consumed;
    }
    private void drawBusySelection(Canvas canvas,RectF busy){
        // caldll0290E27C/0290ACC8: logical192-DPI bottom+2, outer4, inner2 at inset4; short rectangles fill.
        drawSelectionFrame(canvas,busy.left,busy.top,busy.right,busy.bottom+2);
    }
    private void drawSelectionFrame(Canvas canvas,float left,float top,float right,float bottom){
        paint.setColor(selection);
        if(bottom-top<10){canvas.drawRect(left,top,right,bottom,paint);return;}
        for(int pass=0;pass<2;pass++){
            int thickness=pass==0?4:2;paint.setColor(pass==0?selection:Color.WHITE);
            if(right<=left||bottom<=top)return;
            canvas.drawRect(left,top,right,Math.min(bottom,top+thickness),paint);
            canvas.drawRect(left,Math.max(top,bottom-thickness),right,bottom,paint);
            canvas.drawRect(left,top,Math.min(right,left+thickness),bottom,paint);
            canvas.drawRect(Math.max(left,right-thickness),top,right,bottom,paint);
            left+=4;top+=4;right-=4;bottom-=4;
        }
    }
    private static final class Placement {
        final Event event;final RectF bounds=new RectF(),busy=new RectF();int lane=-1,firstSlot,lastSlot,sourceMinute;long sourceOrder;
        Placement(Event event,int step){this.event=event;firstSlot=event.startMinute/step;lastSlot=Math.max(firstSlot+1,(event.endMinute+step-1)/step);}
    }
    private void layoutEvents(){
        if(!placementDirty)return;placementDirty=false;placements.clear();
        int step=days!=DAY?30:interval,slots=1440/step;
        for(int day=0;day<days;day++){
            List<Placement> column=new ArrayList<Placement>();int[] counts=new int[slots];boolean[] visited=new boolean[slots];
            for(Event event:events)if(event.day==day&&!event.allDay){Placement place=new Placement(event,step);column.add(place);for(int slot=place.firstSlot;slot<place.lastSlot;slot++)counts[slot]++;}
            for(Placement place:column){
                Calendar source=sourceStart(place.event);
                place.sourceOrder=civilMinuteOrder(source);
                place.sourceMinute=weekStartMinute(place.event,source);
            }
            // Native enum insertion compares civil start, then descending busy; equal keys stay stable.
            Collections.sort(column,(a,b)->a.sourceOrder<b.sourceOrder?-1:a.sourceOrder>b.sourceOrder?1:b.event.busyStatus-a.event.busyStatus);
            // Native starts with the highest-density slot, then propagates that
            // denominator through each assigned interval. Adjacent empty lanes
            // are expanded instead of imposing one width on an overlap chain.
            for(;;){
                int peak=-1,max=0;for(int slot=0;slot<slots;slot++)if(!visited[slot]&&counts[slot]>max){peak=slot;max=counts[slot];}
                if(peak<0)break;visited[peak]=true;
                for(Placement place:column)if(place.lane<0&&place.firstSlot<=peak&&place.lastSlot>peak){
                    for(int slot=place.firstSlot;slot<place.lastSlot;slot++)counts[slot]=max;
                    int lane=0;while(used(column,place,lane))lane++;place.lane=lane;
                }
            }
            float left=columnLeft(day),width=columnLeft(day+1)-left;
            for(Placement place:column){
                int denominator=Math.max(place.lane+1,counts[place.firstSlot]),rightLane=place.lane+1;
                while(rightLane<denominator&&!used(column,place,rightLane))rightLane++;
                float x=left+place.lane*width/denominator+(place.lane==0?2:0);
                float right=left+rightLane*width/denominator-(rightLane==denominator?5:days!=DAY&&!showWeekText?2:6);
                int top=place.event.startMinute/interval*rowHeight+2;
                int bottom=Math.max(place.event.startMinute/interval+1,(place.event.endMinute+interval-1)/interval)*rowHeight-2;
                place.bounds.set(x,top,Math.max(x+1,right),Math.min(getContentHeight(),Math.max(top+1,bottom)));
                // 0290C5AC maintains separate text and busy rectangles. Busy
                // occupies the actual minute fraction even with hourly rows.
                int busyTop=top+(place.event.startMinute%interval)*rowHeight/interval;
                int duration=place.event.endMinute-place.event.startMinute;
                int busyBottom=duration==0?busyTop+(days!=DAY&&!showWeekText?22:10):Math.min(bottom,busyTop+duration*rowHeight/interval);
                if(days!=DAY&&interval==60){if((place.firstSlot&1)!=0)busyTop+=2;if((place.lastSlot&1)!=0)busyBottom-=2;}
                place.busy.set(x,busyTop,days!=DAY&&!showWeekText?Math.max(x+1,right):Math.min(right,x+10),Math.min(getContentHeight(),Math.max(busyTop+1,busyBottom)));
                if(days!=DAY&&!showWeekText)place.bounds.set(place.busy);
                placements.add(place);
            }
        }
    }
    private boolean used(List<Placement> column,Placement target,int lane){for(Placement other:column)if(other!=target&&other.lane==lane&&other.firstSlot<target.lastSlot&&target.firstSlot<other.lastSlot)return true;return false;}
    private final class Grid extends View {
        private final RectF hitBounds=new RectF();
        private Event downEvent;
        private boolean tracking,downTime;
        private float downX,downY;
        private final int slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
        Grid(Context context){super(context);setClickable(true);setSoundEffectsEnabled(false);}
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){placementDirty=true;cancelGesture();}
        @Override protected void onMeasure(int width,int height){setMeasuredDimension(MeasureSpec.getSize(width),resolveSize(getContentHeight(),height));}
        @Override protected void onDraw(Canvas canvas){
            font();boolean enabled=WmCalendarView.this.isEnabled();paint.setColor(enabled?Color.WHITE:face);canvas.drawRect(0,0,getWidth(),getHeight(),paint);
            float axis=columnLeft(0);Paint.FontMetrics fm=paint.getFontMetrics();
            for(int row=0;row<1440/interval;row++){
                int minute=row*interval,y=row*rowHeight;
                // caldll02908884/0290F5D4 fixed work colors; 02908FB8 checks civil weekday and half-hour range.
                for(int day=0;day<days;day++){
                    // caldll0290F5D4 uses palette2/BTNFACE when the DAYVIEW is disabled.
                    paint.setColor(!enabled?face:workingDays[day]&&minute>=workStart&&minute<workEnd?0xffffffcc:0xffdddddd);
                    canvas.drawRect(days==DAY?0:columnLeft(day),y,columnLeft(day+1),y+rowHeight,paint);
                }
                // WEEK's native DAYVIEW children carry style0x80: no row lines.
                // SHPolyline flag0x40 aligns the native2px pen inside the row.
                if(days==DAY&&!(timeDay>=0&&minute>=timeStart&&minute<timeEnd)){paint.setColor(!enabled||workingDays[0]&&minute>=workStart&&minute<workEnd?face:WmControls.SHADOW);canvas.drawRect(0,y,getWidth(),y+WmMetrics.BORDER,paint);}
                if(minute%60==0){int hour=minute/60;String label=String.valueOf(hour24?hour:hour%12==0?12:hour%12);boolean chosen=timeDay>=0&&minute>=timeStart&&minute<timeEnd;
                    if(chosen){paint.setColor(selection);canvas.drawRect(0,y+2,axisWidth,y+rowHeight,paint);}paint.setColor(chosen?Color.WHITE:Color.BLACK);WmFonts.draw(canvas,label,axisWidth-4-WmFonts.measure(paint,label),y+3-fm.ascent,paint);}
            }
            paint.setColor(days==DAY?0xff808080:Color.BLACK);for(int day=0;day<=days;day++){float x=columnLeft(day);canvas.drawRect(x,0,x+2,getHeight(),paint);}
            // WM6.1 Week captures: preserve the child border, then status fill and SHBorderRectangle's right border.
            if(days!=DAY)for(int day=0;day<days;day++)if(allDayBusy[day]>=0){float x=columnLeft(day);paint.setColor(BUSY_COLORS[allDayBusy[day]]);canvas.drawRect(x+2,0,x+4,getHeight(),paint);paint.setColor(Color.BLACK);canvas.drawRect(x+4,0,x+6,getHeight(),paint);}
            // caldll0290F5D4 uses a4px VGA highlight pen; adjacent selected rows omit inner edges.
            if(timeDay>=0){float top=timeStart/(float)interval*rowHeight,bottom=timeEnd/(float)interval*rowHeight;
                if(days==DAY)drawTimeFrame(canvas,0,top,getWidth(),bottom);
                else{drawTimeFrame(canvas,0,top,axisWidth,bottom);drawTimeFrame(canvas,columnLeft(timeDay),top,columnLeft(timeDay+1),bottom);}}
            layoutEvents();
            // caldll0290DB68 inverts text appointments with COLOR_WINDOWTEXT; busy brushes stay independent.
            // caldll0290DBCC uses penD24/BTNSHADOW for text boxes; busy-only uses penD20/WINDOWTEXT.
            for(Placement place:placements){RectF box=place.bounds;paint.setColor(place.event==selected?Color.BLACK:eventColor);canvas.drawRect(box,paint);paint.setColor(days==DAY||showWeekText?WmControls.SHADOW:Color.BLACK);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);canvas.drawRect(box,paint);paint.setStyle(Paint.Style.FILL);
                if(days==DAY||showWeekText){paint.setColor(BUSY_COLORS[place.event.busyStatus]);canvas.drawRect(place.busy,paint);int saved=canvas.save();canvas.clipRect(box);int reserved=drawSymbols(canvas,place.event,box);canvas.clipRect(box.left,box.top,Math.max(box.left,box.right-reserved),box.bottom);paint.setColor(place.event==selected?Color.WHITE:Color.BLACK);WmFonts.draw(canvas,place.event.title,box.left+12,box.top+3-fm.ascent,paint);canvas.restoreToCount(saved);}
                else{paint.setColor(BUSY_COLORS[place.event.busyStatus]);canvas.drawRect(box,paint);}
                if(days!=DAY&&place.event==selected&&place.event.endMinute>place.event.startMinute)drawBusySelection(canvas,place.busy);
            }
            if(days!=DAY&&selected!=null&&selected.allDay&&scroll.getHeight()>0){
                // Native child HWND frame is viewport-local, unlike this scroll-content grid.
                int top=scroll.getScrollPosition(),height=scroll.getHeight(),rows=height/rowHeight;
                if(top/rowHeight+rows>=24)height=rows*rowHeight;
                float left=columnLeft(selected.day);
                int saved=canvas.save();canvas.clipRect(left+2,top,columnLeft(selected.day+1),top+scroll.getHeight());
                drawSelectionFrame(canvas,left,top,left+10,top+height);canvas.restoreToCount(saved);
            }
        }
        private void drawTimeFrame(Canvas canvas,float left,float top,float right,float bottom){
            paint.setColor(selection);canvas.drawRect(left,top,left+4,bottom,paint);canvas.drawRect(right-4,top,right,bottom,paint);canvas.drawRect(left,top,right,top+4,paint);canvas.drawRect(left,bottom-4,right,bottom,paint);
        }
        private int timeColumn(float x,float y){
            if(y<0||y>=getContentHeight()||x<(days==DAY?0:columnLeft(0))||x>=getWidth())return -1;
            for(int day=0;day<days;day++)if(x<columnLeft(day+1))return day;return -1;
        }
        private Event hit(float x,float y){
            layoutEvents();
            // caldll02910458 tests the enclosing text/busy union, first record wins.
            for(Placement place:placements){
                hitBounds.set(place.bounds);hitBounds.union(place.busy);
                if(hitBounds.contains(x,y))return place.event;
            }
            return null;
        }
        void cancelGesture(){tracking=false;downTime=false;downEvent=null;setPressed(false);}
        @Override public boolean onTouchEvent(MotionEvent event){
            if(!WmCalendarView.this.isEnabled()){cancelGesture();return false;}
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){tracking=true;downTime=false;downX=event.getX();downY=event.getY();downEvent=hit(downX,downY);if(downEvent!=null)choose(downEvent);else{int day=timeColumn(downX,downY);if(day>=0){downTime=true;timeDay=day;timeStart=(int)(downY/rowHeight)*interval;timeEnd=timeStart+interval;keyAnchor=keyRow=timeStart/interval;selected=null;invalidateViews();}}return true;}
            if(!tracking)return false;
            if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN||event.getPointerCount()>1){cancelGesture();return true;}
            if(action==MotionEvent.ACTION_MOVE){if(Math.abs(event.getX()-downX)>slop||Math.abs(event.getY()-downY)>slop){downEvent=null;tracking=false;}return true;}
            if(action==MotionEvent.ACTION_UP){Event target=downEvent;boolean tap=Math.abs(event.getX()-downX)<=slop&&Math.abs(event.getY()-downY)<=slop,timeTap=downTime;cancelGesture();if(tap&&target!=null&&hit(event.getX(),event.getY())==target){performClick();open(target);}else if(tap&&timeTap&&target==null&&timeDay>=0&&timeColumn(event.getX(),event.getY())==timeDay&&(int)(event.getY()/rowHeight)*interval==timeStart){performClick();notifyTime();}return true;}
            return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
        @Override protected void onDetachedFromWindow(){cancelGesture();super.onDetachedFromWindow();}
    }
    private final class Header extends View {
        private Event downEvent;
        private int downColumn=-1;
        private boolean trackingDate;
        private float downX,downY;
        private final int slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
        Header(Context context){super(context);setSoundEffectsEnabled(false);}
        @Override protected void onDraw(Canvas canvas){
            font();Paint.FontMetrics fm=paint.getFontMetrics();
            if(days!=DAY){
                paint.setColor(face);canvas.drawRect(0,0,getWidth(),getHeight(),paint);Calendar today=Calendar.getInstance();
                for(int day=0;day<days;day++){int[] date=dateAt(day);float left=columnLeft(day),right=columnLeft(day+1);if(date[0]==today.get(Calendar.YEAR)&&date[1]==today.get(Calendar.MONTH)+1&&date[2]==today.get(Calendar.DAY_OF_MONTH)){paint.setColor(Color.WHITE);canvas.drawRect(left,0,right,getHeight(),paint);}
                    String text=String.valueOf(date[2]);paint.setColor(Color.BLACK);WmFonts.draw(canvas,text,(left+right-WmFonts.measure(paint,text))/2,(getHeight()-fm.ascent-fm.descent)/2,paint);}
            }else{int row=0;for(Event event:events)if(event.allDay){paint.setColor(event==selected?selection:Color.WHITE);canvas.drawRect(0,row*listSpacing,getWidth(),(row+1)*listSpacing,paint);paint.setColor(event==selected?Color.WHITE:Color.BLACK);int save=canvas.save();canvas.clipRect(0,row*listSpacing,getWidth(),(row+1)*listSpacing);WmFonts.drawCenteredListCaption(canvas,event.title,new RectF(0,row*listSpacing,getWidth(),(row+1)*listSpacing),paint);canvas.restoreToCount(save);row++;}}
        }
        private void drawTimeFrame(Canvas canvas,float left,float top,float right,float bottom){
            paint.setColor(selection);canvas.drawRect(left,top,left+4,bottom,paint);canvas.drawRect(right-4,top,right,bottom,paint);canvas.drawRect(left,top,right,top+4,paint);canvas.drawRect(left,bottom-4,right,bottom,paint);
        }
        private int timeColumn(float x,float y){
            if(y<0||y>=getContentHeight()||x<(days==DAY?0:columnLeft(0))||x>=getWidth())return -1;
            for(int day=0;day<days;day++)if(x<columnLeft(day+1))return day;return -1;
        }
        private Event hit(float x,float y){if(days!=DAY||x<0||x>=getWidth()||y<0||y>=getHeight())return null;int row=(int)y/listSpacing,index=0;for(Event event:events)if(event.allDay&&index++==row)return event;return null;}
        void cancelGesture(){downEvent=null;downColumn=-1;trackingDate=false;setPressed(false);}
        private int dateColumn(float x,float y){
            if(x<0||x>=getWidth()||y<0||y>=getHeight())return -1;
            for(int column=0;column<days;column++)if(dateHeaderBounds(column).contains(x,y))return column;
            return -1;
        }
        private boolean dateTouch(MotionEvent event){
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){
                cancelGesture();if(event.getPointerCount()!=1)return false;
                downX=event.getX();downY=event.getY();downColumn=dateColumn(downX,downY);
                trackingDate=downColumn>=0;return trackingDate;
            }
            if(!trackingDate)return false;
            if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN||event.getPointerCount()>1){cancelGesture();return true;}
            if(action==MotionEvent.ACTION_MOVE){
                if(Math.abs(event.getX()-downX)>slop||Math.abs(event.getY()-downY)>slop||dateColumn(event.getX(),event.getY())!=downColumn)downColumn=-1;
                return true;
            }
            if(action==MotionEvent.ACTION_UP){
                int target=downColumn;
                boolean tap=target>=0&&Math.abs(event.getX()-downX)<=slop&&Math.abs(event.getY()-downY)<=slop&&dateColumn(event.getX(),event.getY())==target;
                cancelGesture();if(tap){int[] date=dateAt(target);performClick();if(dateListener!=null)dateListener.onDate(date[0],date[1],date[2]);}return true;
            }
            return true;
        }
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){cancelGesture();}
        @Override public boolean onTouchEvent(MotionEvent event){
            if(!WmCalendarView.this.isEnabled()){cancelGesture();return false;}
            if(days!=DAY)return dateTouch(event);
            int action=event.getActionMasked();if(action==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();downEvent=hit(downX,downY);if(downEvent!=null)choose(downEvent);return downEvent!=null;}
            if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN||event.getPointerCount()>1){downEvent=null;return true;}
            if(action==MotionEvent.ACTION_MOVE&&(Math.abs(event.getX()-downX)>ViewConfiguration.get(getContext()).getScaledTouchSlop()||Math.abs(event.getY()-downY)>ViewConfiguration.get(getContext()).getScaledTouchSlop()))downEvent=null;
            if(action==MotionEvent.ACTION_UP){Event target=downEvent;downEvent=null;int slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();if(target!=null&&Math.abs(event.getX()-downX)<=slop&&Math.abs(event.getY()-downY)<=slop&&hit(event.getX(),event.getY())==target){performClick();open(target);}return target!=null;}return downEvent!=null;
        }
        @Override public boolean performClick(){super.performClick();return true;}
        @Override protected void onDetachedFromWindow(){cancelGesture();super.onDetachedFromWindow();}
    }
    private final class Details extends View {
        Details(Context context){super(context);}
        @Override protected void onDraw(Canvas canvas){
            font();paint.setColor(face);canvas.drawRect(0,0,getWidth(),getHeight(),paint);
            // KOR GetSysColor(0x4000001a) defaults to black; distinct from shell SHColor26.
            paint.setColor(Color.BLACK);canvas.drawRect(0,0,getWidth(),WmMetrics.BORDER,paint);
            Paint.FontMetrics fm=paint.getFontMetrics();int height=(int)Math.ceil(fm.descent-fm.ascent);paint.setColor(Color.BLACK);
            // caldll02904B90: x/top5, right client-10; row2 starts after role51 height+external leading.
            line(canvas,selected==null?(timeDay<0?noAppointments:noSelected):selected.title,5,height,fm);
            if(selected!=null)line(canvas,selected.detail,5+height+(int)Math.ceil(fm.leading),height,fm);
        }
        private void line(Canvas canvas,String value,int top,int height,Paint.FontMetrics fm){
            if(getWidth()<=15||value.isEmpty())return;
            int end=value.length(),cr=value.indexOf('\r'),lf=value.indexOf('\n');if(cr>=0)end=Math.min(end,cr);if(lf>=0)end=Math.min(end,lf);
            String text=value.substring(0,end);float width=getWidth()-15;boolean shortened=end<value.length()||WmFonts.measure(paint,text)>width;
            if(shortened)text=fit(text,width-WmFonts.measure(paint,"..."));
            int saved=canvas.save();canvas.clipRect(5,top,getWidth()-10,top+height);WmFonts.draw(canvas,text,5,top-fm.ascent,paint);
            if(shortened)WmFonts.draw(canvas,"...",5+WmFonts.measure(paint,text),top-fm.ascent,paint);
            canvas.restoreToCount(saved);
        }
        private String fit(String text,float width){
            if(text.isEmpty())return text;
            // GWES92D10 retains the first character even when only clipped dots fit; keep surrogate pairs intact.
            int low=1,high=text.codePointCount(0,text.length());
            while(low<high){int middle=(low+high+1)/2,end=text.offsetByCodePoints(0,middle);if(WmFonts.measure(paint,text.substring(0,end))<=width)low=middle;else high=middle-1;}
            return text.substring(0,text.offsetByCodePoints(0,low));
        }
    }
}
