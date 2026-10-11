package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import java.io.IOException;
import java.util.IdentityHashMap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/** Shared single-day native Agenda presentation, in logical VGA pixels.
 * caldll 029144F8 supplies the two text lines; 029143F8/0291BE38 font spacing;
 * 0292085C/0292011C the read-only DAYVIEWSTRIP. No PIM queries or exported tags.
 * List font roles51/52/57 and the strip's stock-derived seven-point font
 * (SHCreateSystemFont(0,7,0):7 is point size, not a role) currently share the
 * supplied regular Android face: list24px, seven-point strip19px at logical192 DPI; final native GDI metrics/face are unverified.
 * Android font
 * extents and all-day padding/border are adaptations. Native strip phase and
 * discrete easing arithmetic are retained; Android dispatch cadence is not
 * measured WM hardware cadence. The optional native date footer is absent.
 */
public final class WmAgendaView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener {
    public static final class Event {
        public static final int FREE=0,TENTATIVE=1,BUSY=2,OUT_OF_OFFICE=3;
        public final int busyStatus;
        private final Long startTimeMillis;
        public final int startMinute,endMinute,busyColor;
        public final boolean allDay;
        public final String title,time,location;
        public final Object tag;
        public final int symbolMask;
        public final boolean recurrenceException,past;
        private final Long endTimeMillis;
        /** End is exclusive; busyColor0 follows the current selection color. */
        public Event(int startMinute,int endMinute,boolean allDay,String title,String time,String location,int busyColor,Object tag){
            this(startMinute,endMinute,allDay,title,time,location,busyColor,tag,0,false,false);
        }
        /** Timed-row symbols use WmCalendarSymbols bits; past selects the native
         * elapsed icon bank and gray unselected text. Caller owns clock updates. */
        public Event(int startMinute,int endMinute,boolean allDay,String title,String time,String location,int busyColor,Object tag,int symbolMask,boolean recurrenceException,boolean past){
            this(startMinute,endMinute,allDay,title,time,location,busyColor,tag,symbolMask,recurrenceException,past,null);
        }
        /** Original, unclipped epoch end: current at equality, elapsed only afterwards.
         * The view refreshes at the endpoint and after system clock changes.
         * past is the construction snapshot; isPast(now) supplies live classification. */
        public Event(int startMinute,int endMinute,boolean allDay,String title,String time,String location,int busyColor,Object tag,int symbolMask,boolean recurrenceException,long endTimeMillis){
            this(startMinute,endMinute,allDay,title,time,location,busyColor,tag,symbolMask,recurrenceException,endTimeMillis<System.currentTimeMillis(),Long.valueOf(endTimeMillis));
        }
        /** Original endpoints and native busy status; strip minutes remain clipped to the visible day.
         * Legacy constructors have no conflict metadata and retain their previous rendering. */
        public Event(int startMinute,int endMinute,boolean allDay,String title,String time,String location,int busyColor,Object tag,int symbolMask,boolean recurrenceException,long startTimeMillis,long endTimeMillis,int busyStatus){
            this(startMinute,endMinute,allDay,title,time,location,busyColor,tag,symbolMask,recurrenceException,endTimeMillis<System.currentTimeMillis(),Long.valueOf(endTimeMillis),Long.valueOf(startTimeMillis),busyStatus);
        }
        private Event(int startMinute,int endMinute,boolean allDay,String title,String time,String location,int busyColor,Object tag,int symbolMask,boolean recurrenceException,boolean past,Long endTimeMillis){
            this(startMinute,endMinute,allDay,title,time,location,busyColor,tag,symbolMask,recurrenceException,past,endTimeMillis,null,Event.FREE);
        }
        private Event(int startMinute,int endMinute,boolean allDay,String title,String time,String location,int busyColor,Object tag,int symbolMask,boolean recurrenceException,boolean past,Long endTimeMillis,Long startTimeMillis,int busyStatus){
            if(busyStatus<FREE||busyStatus>OUT_OF_OFFICE||startTimeMillis!=null&&endTimeMillis.longValue()<startTimeMillis.longValue())throw new IllegalArgumentException("invalid agenda busy interval");
            if(startMinute<0||startMinute>=1440||endMinute<startMinute||endMinute>1440)throw new IllegalArgumentException("invalid agenda event");
            if(title==null||time==null||location==null)throw new IllegalArgumentException("agenda captions required");
            if(symbolMask<0||symbolMask>WmCalendarSymbols.ALL)throw new IllegalArgumentException("invalid agenda symbol mask");
            this.startMinute=startMinute;this.endMinute=endMinute;this.allDay=allDay;
            this.title=title;this.time=time;this.location=location;this.busyColor=busyColor;this.tag=tag;
            this.symbolMask=symbolMask;this.recurrenceException=recurrenceException;this.past=past;
            this.endTimeMillis=endTimeMillis;this.startTimeMillis=startTimeMillis;this.busyStatus=busyStatus;
        }
        public boolean isPast(long now){return endTimeMillis==null?past:endTimeMillis.longValue()<now;}
    }
    public interface OnEventListener {void onSelected(Event event);void onOpen(Event event);}
    // SHCreateSystemFont(0,7,0): round(7*192/72), separate from list roles.
    private static final int STRIP_HEIGHT=38,STRIP_FONT=19;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Calendar day=new GregorianCalendar();
    private final Strip strip;
    private final Rows allDayRows,timedRows;
    private boolean clockRegistered;
    private final Runnable elapsedRefresh=new Runnable(){@Override public void run(){timedRows.invalidate();scheduleElapsedRefresh();}};
    private final BroadcastReceiver clockChanges=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){elapsedRefresh.run();}};
    private final WmScrollView allDayScroll,timedScroll;
    private Typeface font;
    private Locale locale;
    private String emptyCaption;
    private String[] busyLabels;
    private boolean showBusyInDescription;
    private int selection=0xff149ca7,face=WmControls.FACE,measuredLine,rowHeight,allDaySpacing,stripPhase;
    private int stripOffset,stripTarget,stripStepDelta;
    private boolean stripPanning,stripPositionPending;
    private final Runnable stripPan=new Runnable(){@Override public void run(){
        if(!stripPanning||getWindowToken()==null){stopStripPan();return;}
        if(advanceStrip())postDelayed(this,1);else stopStripPan();
    }};
    private boolean hour24;
    private List<Event> events=Collections.emptyList(),allDayEvents=Collections.emptyList(),timedEvents=Collections.emptyList();
    private Event selected;
    private final IdentityHashMap<Event,Boolean> conflicts=new IdentityHashMap<Event,Boolean>();
    private Bitmap conflictIcon;
    private String conflictRevision;
    private OnEventListener listener;
    private int displayIconMask=WmCalendarSymbols.ALL;
    private final WmCalendarSymbols[] symbols=new WmCalendarSymbols[14];

    public WmAgendaView(Context context,Typeface font,Locale locale){
        super(context);if(font==null||locale==null)throw new IllegalArgumentException("font and locale required");
        this.font=font;this.locale=locale;updateCaptions(WmStrings.language(getContext(),android.os.Build.VERSION.SDK_INT>=21?Modern.tag(locale):locale.toString().replace('_','-')));hour24=android.text.format.DateFormat.is24HourFormat(context);
        Calendar now=Calendar.getInstance();day.clear();day.set(now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH),12,0,0);day.setLenient(false);
        setOrientation(VERTICAL);setBackgroundColor(Color.WHITE);measureFont();
        strip=new Strip(context);allDayRows=new Rows(context,true);timedRows=new Rows(context,false);
        allDayScroll=new WmScrollView(context,allDayRows,1,face);timedScroll=new WmScrollView(context,timedRows,rowHeight,face);
        addView(strip,new LayoutParams(-1,STRIP_HEIGHT));addView(allDayScroll,new LayoutParams(-1,0));addView(timedScroll,new LayoutParams(-1,0,1));updateGeometry();
    }
    public void setOnEventListener(OnEventListener listener){this.listener=listener;}
    public void setDisplayIconMask(int mask){if(mask<0||mask>WmCalendarSymbols.ALL)throw new IllegalArgumentException("invalid agenda display mask");displayIconMask=mask;cancelGestures();timedRows.invalidate();}
    public int getDisplayIconMask(){return displayIconMask;}
    /** Copies the list; immutable records and their caller-owned tags stay local. */
    public void setEvents(List<Event> values){
        if(values==null)throw new IllegalArgumentException("events required");
        List<Event> copy=new ArrayList<Event>(values);for(Event event:copy)if(event==null)throw new IllegalArgumentException("null agenda event");
        events=Collections.unmodifiableList(copy);partition();selected=null;stripPhase=1;stripPositionPending=false;cancelGestures();updateGeometry();allDayScroll.scrollToPosition(0);timedScroll.scrollToPosition(0);
        scheduleElapsedRefresh();
    }
    /** Selects a supplied record without callbacks; null clears, an absent record returns false. */
    public boolean selectEvent(Event event){
        if(event!=null&&!events.contains(event))return false;
        cancelGestures();selected=event;
        if(event!=null){
            Rows rows=event.allDay?allDayRows:timedRows;
            Rect bounds=rowBounds(event);WmScrollView scroll=event.allDay?allDayScroll:timedScroll;
            int offset=scroll.getRequestedScrollPosition();
            if(bounds.top<offset)scroll.scrollToPosition(bounds.top);
            else if(bounds.bottom>offset+scroll.getHeight())scroll.scrollToPosition(Math.max(0,Math.min(bounds.top,bounds.bottom-scroll.getHeight())));
            if(!event.allDay){selectStripPhase(event);startStripPan();}
            rows.requestFocus();
        }
        invalidateViews();return true;
    }
    public void setDate(int year,int month,int date){
        Calendar next=new GregorianCalendar();next.clear();next.setLenient(false);next.set(year,month-1,date,12,0,0);next.getTimeInMillis();
        boolean changed=year!=day.get(Calendar.YEAR)||month!=day.get(Calendar.MONTH)+1||date!=day.get(Calendar.DAY_OF_MONTH);
        day.setTimeInMillis(next.getTimeInMillis());cancelGestures();
        if(changed){events=Collections.emptyList();allDayEvents=Collections.emptyList();timedEvents=Collections.emptyList();conflicts.clear();selected=null;stripPhase=1;stripPositionPending=false;updateGeometry();allDayScroll.scrollToPosition(0);timedScroll.scrollToPosition(0);}
        scheduleElapsedRefresh();
    }
    public int[] date(){return new int[]{day.get(Calendar.YEAR),day.get(Calendar.MONTH)+1,day.get(Calendar.DAY_OF_MONTH)};}
    public void setLocale(Locale locale){
        if(locale==null)throw new IllegalArgumentException("locale required");
        float timed=timedFraction(),allDay=allDayFraction();this.locale=locale;updateCaptions(WmStrings.language(getContext(),android.os.Build.VERSION.SDK_INT>=21?Modern.tag(locale):locale.toString().replace('_','-')));cancelGestures();partition();updateGeometry();restoreScroll(timed,allDay);
    }
    private void updateCaptions(String language){
        emptyCaption=WmStrings.text(getContext(),language,"약속 없음","calendar.agenda.empty");
        busyLabels=new String[]{"약속 없음","미정","약속 있음","부재 중"};
        for(int i=0;i<busyLabels.length;i++)busyLabels[i]=WmStrings.text(getContext(),language,busyLabels[i],"calendar.busy."+i);
    }
    private static final class Modern {static String tag(Locale locale){return locale.toLanguageTag();}}
    /** Authored captions only; explicit sorting/formatting Locale remains caller-owned. */
    @Override public void onWmLanguageChanged(String language){updateCaptions(WmStrings.language(getContext(),language));timedRows.invalidate();}
    /** Native ShowBusyInDescription defaults off; free/tentative/out-of-office still show their labels. */
    public void setShowBusyInDescription(boolean enabled){if(showBusyInDescription!=enabled){showBusyInDescription=enabled;timedRows.invalidate();}}
    private String timedCaption(Event event){
        if(event.allDay||event.startTimeMillis==null)return event.title;
        String value=(event.busyStatus==Event.BUSY&&!showBusyInDescription?"":busyLabels[event.busyStatus]+": ")+event.title;
        int end=Math.min(259,value.length());
        if(end<value.length()&&end>0&&Character.isHighSurrogate(value.charAt(end-1))&&Character.isLowSurrogate(value.charAt(end)))end--;
        return value.substring(0,end);
    }
    public void set24Hour(boolean enabled){hour24=enabled;cancelGestures();strip.invalidate();}
    public void setColors(int selection){
        cancelGestures();this.selection=selection;face=WmPalette.face(selection);allDayScroll.setBackgroundColor(face);
        WmControls.restyleTree(allDayScroll,font,font,selection,face);WmControls.restyleTree(timedScroll,font,font,selection,face);invalidateViews();
    }
    public int getRowHeight(){return rowHeight;}
    public int getAllDayRowHeight(){return allDaySpacing;}
    public int getAllDayViewportHeight(){return allDayEvents.isEmpty()?0:Math.min(6,allDayEvents.size())*allDaySpacing+2;}
    public int getStripHeight(){return STRIP_HEIGHT;}
    public int getStripBackingWidth(){return (int)Math.floor(strip.getWidth()*2.4d);}
    public int getStripOffset(){return stripOffset;}
    /** Accepts phases0..2, rejecting other values before mutation. Cancels gestures/pan
     * and sets logical-pixel offset0,8*step,14*step, where step=floor(backingWidth/24.5)
     * and backingWidth=floor(stripWidth*2.4). Zero width defers to positive-width layout.
     * Does not select records or notify listeners; user selection uses native discrete easing. */
    public void setStripPhase(int phase){if(phase<0||phase>2)throw new IllegalArgumentException("strip phase must be 0..2");cancelGestures();stopStripPan();stripPhase=phase;updateStripTarget();stripOffset=stripTarget;stripStepDelta=0;stripPositionPending=strip.getWidth()==0;strip.invalidate();}
    /** Bounds in the event's own all-day/timed list, before vertical scrolling. */
    public Rect rowBounds(Event event){
        if(event==null)return null;List<Event> rows=event.allDay?allDayEvents:timedEvents;int index=rows.indexOf(event);if(index<0)return null;
        int spacing=event.allDay?allDaySpacing:rowHeight,width=event.allDay?allDayRows.getWidth():timedRows.getWidth();return new Rect(0,index*spacing,width,(index+1)*spacing);
    }
    /** Bounds in the full strip backing surface, before phase translation. */
    public Rect busyBounds(Event event){
        if(event==null||!events.contains(event))return null;
        if(event.allDay)return new Rect(0,0,getStripBackingWidth(),4);
        int step=stripStep(),left=step/2+step*(event.startMinute/60)+step*(event.startMinute%60)/60;
        int width=Math.max(1,(30+step*(event.endMinute-event.startMinute))/60);return new Rect(left,4,left+width,32);
    }
    /** Zero-based sorted timed list only, excluding all-day records. Start key is
     * original startTimeMillis or displayed midnight epoch+startMinute*60000;
     * ties compare title with the current Locale Collator. Empty list allows0.
     * Does not select, focus or notify; see setEvents for partition order. */
    public void scrollToRow(int index){if(index<0||index>=Math.max(1,timedEvents.size()))throw new IllegalArgumentException("row outside agenda");timedScroll.scrollToPosition(index*rowHeight);}
    private int stripStep(){return (int)Math.floor(getStripBackingWidth()/24.5d);}
    private void updateStripTarget(){int step=stripStep();stripTarget=stripPhase==0?0:stripPhase==1?8*step:14*step;}
    /** caldll0291F774 +0291EEA8: phase hysteresis followed by duration adjustment. */
    private void selectStripPhase(Event event){
        int hour=event.startMinute/60;
        if(hour<8)stripPhase=0;
        else if(hour>=18)stripPhase=2;
        else if(hour>=10&&hour<=13||hour<10&&stripPhase==2||hour>=14&&stripPhase==0)stripPhase=1;
        if(stripPhase==0&&event.startMinute>=450&&event.endMinute/60>=10)stripPhase=1;
        else if(stripPhase!=2&&event.startMinute>=810&&event.endMinute/60>=18)stripPhase=2;
    }
    /** caldll0291EFD0; a zero previous step within four pixels really stops short. */
    private boolean advanceStrip(){
        int distance=Math.abs(stripTarget-stripOffset);
        stripStepDelta=distance>4?distance/4:Math.min(stripStepDelta,distance);
        stripOffset+=stripStepDelta*(stripOffset<=stripTarget?1:-1);strip.invalidate();
        return stripStepDelta!=0;
    }
    private void startStripPan(){
        removeCallbacks(stripPan);stripPositionPending=false;updateStripTarget();stripPanning=getWindowToken()!=null&&strip.getWidth()>0;
        if(stripPanning)postDelayed(stripPan,1);
    }
    private void stopStripPan(){stripPanning=false;removeCallbacks(stripPan);}
    private void partition(){
        List<Event> all=new ArrayList<Event>(),timed=new ArrayList<Event>();for(Event event:events)(event.allDay?all:timed).add(event);
        final Collator collator=Collator.getInstance(locale);Calendar midnight=(Calendar)day.clone();midnight.set(Calendar.HOUR_OF_DAY,0);long origin=midnight.getTimeInMillis();
        Collections.sort(timed,(a,b)->{long first=a.startTimeMillis==null?origin+a.startMinute*60000L:a.startTimeMillis.longValue(),second=b.startTimeMillis==null?origin+b.startMinute*60000L:b.startTimeMillis.longValue();return first==second?collator.compare(a.title,b.title):first<second?-1:1;});
        allDayEvents=Collections.unmodifiableList(all);timedEvents=Collections.unmodifiableList(timed);
        refreshConflicts();
    }
    /** Raw native overlap result. Elapsed rows retain metadata but suppress its presentation. */
    public boolean hasConflict(Event event){return conflicts.containsKey(event);}
    private void refreshConflicts(){
        conflicts.clear();IdentityHashMap<Event,Boolean> seen=new IdentityHashMap<Event,Boolean>();boolean duplicate=false,allDayBusy=false;
        for(Event event:allDayEvents)if(event.busyStatus!=Event.FREE)allDayBusy=true;
        for(Event event:timedEvents)if(seen.put(event,Boolean.TRUE)!=null)duplicate=true;
        // Native pointer-identity behavior for repeated references; normal provider rows use the linear sweep below.
        if(duplicate){for(Event event:timedEvents)if(nativeConflict(event))conflicts.put(event,Boolean.TRUE);return;}
        long latestEnd=Long.MIN_VALUE;boolean previous=false;for(Event event:timedEvents)if(event.busyStatus!=Event.FREE&&event.startTimeMillis!=null){
            if(allDayBusy||previous&&latestEnd>event.startTimeMillis.longValue())conflicts.put(event,Boolean.TRUE);
            latestEnd=Math.max(latestEnd,event.endTimeMillis.longValue());previous=true;
        }
        Long followingStart=null;for(int index=timedEvents.size()-1;index>=0;index--){Event event=timedEvents.get(index);if(event.busyStatus!=Event.FREE&&event.startTimeMillis!=null){
            if(followingStart!=null&&followingStart.longValue()<event.endTimeMillis.longValue())conflicts.put(event,Boolean.TRUE);
            followingStart=event.startTimeMillis;
        }}
    }
    private boolean nativeConflict(Event target){
        if(target.busyStatus==Event.FREE||target.startTimeMillis==null)return false;
        for(Event event:allDayEvents)if(event!=target&&event.busyStatus!=Event.FREE)return true;
        boolean before=true;for(Event event:timedEvents){
            if(event==target)before=false;
            else if(event.busyStatus!=Event.FREE&&event.startTimeMillis!=null){
                if(!before)return event.startTimeMillis.longValue()<target.endTimeMillis.longValue();
                if(event.endTimeMillis.longValue()>target.startTimeMillis.longValue())return true;
            }
        }
        return false;
    }
    private void drawConflict(Canvas canvas,int right,int top){
        if(conflictRevision==null)try{WmResources.prepare(getContext());}catch(java.io.IOException absent){}
        String revision=WmResources.revision(getContext());
        if(conflictIcon==null||!revision.equals(conflictRevision)){conflictIcon=WmResources.iconBitmap(getContext(),"calendar/agenda-conflict.png",32,32,"⚠️");conflictRevision=revision;}
        canvas.drawBitmap(conflictIcon,right-4-32,top+4,paint);
    }
    private void font(){paint.setStyle(Paint.Style.FILL);paint.setTypeface(font);paint.setTextSize(WmMetrics.FONT);paint.setTextAlign(Paint.Align.LEFT);paint.setStrokeWidth(1);paint.setFakeBoldText(false);}
    private void measureFont(){font();Paint.FontMetrics metrics=paint.getFontMetrics();int extent=(int)Math.ceil(metrics.descent-metrics.ascent);measuredLine=extent+WmMetrics.BORDER;rowHeight=2*(measuredLine+WmMetrics.EDGE)+WmMetrics.EDGE;allDaySpacing=measuredLine+2*WmMetrics.EDGE;}
    private void updateGeometry(){
        allDayScroll.setContentHeight(Math.max(1,allDayEvents.size()*allDaySpacing+2));timedScroll.setContentHeight(Math.max(1,timedEvents.size())*rowHeight);
        LayoutParams size=(LayoutParams)allDayScroll.getLayoutParams();size.height=getAllDayViewportHeight();allDayScroll.setLayoutParams(size);
        // 0291D150 positions the default bordered timed window above the upper
        // list bottom. GWES border insets leave its client overlapping BORDER,
        // not a blank BORDER gap. Our ScrollView already represents the client.
        // A zero-height weighted child discards a negative margin in Android's
        // first measure pass. Equal base height keeps the client bottom intact.
        LayoutParams timed=(LayoutParams)timedScroll.getLayoutParams();timed.height=allDayEvents.isEmpty()?0:WmMetrics.BORDER;timed.topMargin=-timed.height;timedScroll.setLayoutParams(timed);invalidateViews();
    }
    private float timedFraction(){return timedScroll.getRequestedScrollPosition()/(float)Math.max(1,rowHeight);}
    private float allDayFraction(){return allDayScroll.getRequestedScrollPosition()/(float)Math.max(1,allDaySpacing);}
    private void restoreScroll(float timed,float allDay){timedScroll.scrollToPosition(Math.round(timed*rowHeight));allDayScroll.scrollToPosition(Math.round(allDay*allDaySpacing));}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        if(regular==null)throw new IllegalArgumentException("font required");float timed=timedFraction(),allDay=allDayFraction();cancelGestures();font=regular;measureFont();setColors(selection);updateGeometry();restoreScroll(timed,allDay);
    }
    private void cancelGestures(){allDayRows.cancelGesture();timedRows.cancelGesture();}
    private void invalidateViews(){strip.invalidate();allDayRows.invalidate();timedRows.invalidate();}
    private int busyColor(Event event){return event.busyColor==0?selection:event.busyColor;}
    private void activate(Event event){
        selected=event;
        if(!event.allDay){selectStripPhase(event);startStripPan();}
        invalidateViews();OnEventListener callback=listener;if(callback!=null){callback.onSelected(event);callback.onOpen(event);}
    }
    private String fit(String value,float width){if(width<=0)return "";return WmFonts.measure(paint,value)<=width?value:WmFonts.ellipsize(value,paint,width);}
    private int drawSymbols(Canvas canvas,Event event,int right,int top,boolean past){
        int shown=event.symbolMask&displayIconMask,x=right-4;
        // Agenda's PIMUTIL186 cache gap is0; Day/Week's spacing is different.
        for(int bit=5;bit>=0;bit--)if((shown&(1<<bit))!=0){
            int index=(past?7:0)+(bit==1&&event.recurrenceException?6:bit);
            if(symbols[index]==null)symbols[index]=WmCalendarSymbols.forEvent(getContext(),1<<bit,event.recurrenceException,past);
            WmCalendarSymbols symbol=symbols[index];symbol.setBounds(x-32,top+3,x,top+31);symbol.draw(canvas);x-=32;
        }
        return shown==0?right-4:x-4;
    }
    private final class Rows extends View {
        private final boolean allDay;private final int slop;
        private boolean tracking;private Event downEvent;private float downX,downY;
        Rows(Context context,boolean allDay){super(context);this.allDay=allDay;slop=ViewConfiguration.get(context).getScaledTouchSlop();setClickable(true);setFocusableInTouchMode(true);setSoundEffectsEnabled(false);}
        private List<Event> records(){return allDay?allDayEvents:timedEvents;}
        private int spacing(){return allDay?allDaySpacing:rowHeight;}
        private int contentHeight(){return allDay?Math.max(1,records().size()*spacing()+2):Math.max(1,records().size())*spacing();}
        @Override protected void onMeasure(int width,int height){setMeasuredDimension(MeasureSpec.getSize(width),resolveSize(contentHeight(),height));}
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){cancelGesture();}
        @Override protected void onDraw(Canvas canvas){
            long now=System.currentTimeMillis();
            font();Paint.FontMetrics metrics=paint.getFontMetrics();paint.setColor(allDay?face:Color.WHITE);canvas.drawRect(0,0,getWidth(),getHeight(),paint);
            List<Event> values=records();Rect clip=new Rect();canvas.getClipBounds(clip);int spacing=spacing(),first=Math.max(0,clip.top/spacing),last=Math.min(values.size(),(clip.bottom+spacing-1)/spacing);
            for(int index=first;index<last;index++){
                Event event=values.get(index);int top=index*spacing;boolean active=event==selected||event==downEvent,past=event.isPast(now);
                paint.setColor(active?selection:allDay?face:Color.WHITE);canvas.drawRect(0,top,getWidth(),top+spacing,paint);
                // caldll2914A64→291C360: selected text overrides elapsed BTNSHADOW.
                paint.setColor(active?Color.WHITE:!allDay&&past?WmControls.SHADOW:Color.BLACK);
                int saved=canvas.save();canvas.clipRect(0,top,getWidth(),top+spacing);
                if(allDay)WmFonts.drawCenteredListCaption(canvas,event.title,new RectF(0,top,getWidth(),top+spacing),paint);
                else{
                    boolean conflict=!past&&hasConflict(event);if(conflict)drawConflict(canvas,getWidth(),top);
                    int right=drawSymbols(canvas,event,getWidth()-(conflict?32:0),top,past),textClip=canvas.save();canvas.clipRect(0,top,Math.max(0,right),top+spacing);
                    WmFonts.draw(canvas,fit(timedCaption(event),right-4),4,top+2-metrics.ascent,paint);canvas.restoreToCount(textClip);
                    // Native PIMUTIL192 height28 + VGA inset4: release line2's
                    // reserved width only when the measured line clears icons.
                    if(measuredLine>32)right=getWidth();
                    textClip=canvas.save();canvas.clipRect(0,top,Math.max(0,right),top+spacing);
                    int textColor=paint.getColor();if(conflict&&!active)paint.setColor(Color.RED);
                    String time=fit(event.time,getWidth()-4);WmFonts.draw(canvas,time,4,top+measuredLine+2-metrics.ascent,paint);paint.setColor(textColor);
                    float x=4+WmFonts.measure(paint,time)+5;if(!event.location.isEmpty())WmFonts.draw(canvas,fit("("+event.location+")",right-x),x,top+measuredLine+2-metrics.ascent,paint);
                    canvas.restoreToCount(textClip);
                }
                canvas.restoreToCount(saved);paint.setColor(0xffc0c0c0);canvas.drawRect(0,top+spacing-(allDay?2:1),getWidth(),top+spacing,paint);
            }
            if(!allDay&&values.isEmpty()){
                paint.setColor(Color.BLACK);WmFonts.draw(canvas,fit(emptyCaption,getWidth()-8),4,2-metrics.ascent,paint);
                paint.setColor(0xffc0c0c0);canvas.drawRect(0,rowHeight-1,getWidth(),rowHeight,paint);
            }
        }
        private Event hit(float x,float y){if(x<0||x>=getWidth()||y<0||y>=getHeight())return null;int index=(int)y/spacing();return index<records().size()?records().get(index):null;}
        void cancelGesture(){tracking=false;downEvent=null;setPressed(false);invalidate();}
        @Override protected void onFocusChanged(boolean gain,int direction,Rect previous){super.onFocusChanged(gain,direction,previous);if(strip!=null)strip.invalidate();}
        @Override public boolean onKeyDown(int keyCode,KeyEvent event){
            if(!isEnabled()||!WmAgendaView.this.isEnabled()||(keyCode!=KeyEvent.KEYCODE_DPAD_UP&&keyCode!=KeyEvent.KEYCODE_DPAD_DOWN))return super.onKeyDown(keyCode,event);
            List<Event> values=records();if(values.isEmpty())return true;
            int index=values.indexOf(selected),direction=keyCode==KeyEvent.KEYCODE_DPAD_DOWN?1:-1;
            if(index<0){selectEvent(values.get(0));return true;}
            int next=index+direction;
            if(next>=0&&next<values.size()){selectEvent(values.get(next));return true;}
            List<Event> other=allDay?timedEvents:allDayEvents;
            // Cross the middle boundary while held; require a fresh press to wrap the whole list.
            if(!other.isEmpty()&&(allDay&&direction>0||!allDay&&direction<0||event.getRepeatCount()==0))selectEvent(other.get(direction>0?0:other.size()-1));
            else if(other.isEmpty()&&values.size()>1&&event.getRepeatCount()==0)selectEvent(values.get(direction>0?0:values.size()-1));
            return true;
        }
        @Override public boolean onKeyUp(int keyCode,KeyEvent event){
            if(isEnabled()&&WmAgendaView.this.isEnabled()&&(keyCode==KeyEvent.KEYCODE_DPAD_UP||keyCode==KeyEvent.KEYCODE_DPAD_DOWN))return true;
            return super.onKeyUp(keyCode,event);
        }
        @Override public boolean onTouchEvent(MotionEvent event){
            int action=event.getActionMasked();if(action==MotionEvent.ACTION_DOWN){cancelGesture();if(event.getPointerCount()!=1)return false;downX=event.getX();downY=event.getY();downEvent=hit(downX,downY);tracking=downEvent!=null;if(tracking)requestFocus();if(tracking&&!allDay){selectStripPhase(downEvent);startStripPan();}invalidate();return tracking;}
            if(!tracking)return false;
            if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN||event.getPointerCount()>1){cancelGesture();return true;}
            if(action==MotionEvent.ACTION_MOVE){if(Math.abs(event.getX()-downX)>slop||Math.abs(event.getY()-downY)>slop||hit(event.getX(),event.getY())!=downEvent){downEvent=null;invalidate();}return true;}
            if(action==MotionEvent.ACTION_UP){Event target=downEvent;boolean matches=target!=null&&Math.abs(event.getX()-downX)<=slop&&Math.abs(event.getY()-downY)<=slop&&hit(event.getX(),event.getY())==target;cancelGesture();if(matches){performClick();activate(target);}return true;}return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
        @Override protected void onDetachedFromWindow(){cancelGesture();super.onDetachedFromWindow();}
    }
    private final class Strip extends View {
        Strip(Context context){super(context);setSoundEffectsEnabled(false);}
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){updateStripTarget();if(stripPositionPending&&w>0){stripOffset=stripTarget;stripPositionPending=false;}}
        @Override protected void onDraw(Canvas canvas){
            updateStripTarget();font();paint.setTextSize(STRIP_FONT);paint.setColor(Color.WHITE);canvas.drawRect(0,0,getWidth(),getHeight(),paint);int saved=canvas.save();canvas.clipRect(0,0,getWidth(),STRIP_HEIGHT);canvas.translate(-getStripOffset(),0);
            if(!allDayEvents.isEmpty()){paint.setColor(busyColor(allDayEvents.get(allDayEvents.size()-1)));canvas.drawRect(0,0,getStripBackingWidth(),4,paint);}
            for(Event event:timedEvents){paint.setColor(busyColor(event));canvas.drawRect(busyBounds(event),paint);}
            if(selected!=null&&!selected.allDay&&timedRows.hasFocus()){Rect box=busyBounds(selected);if(box!=null){bracket(canvas,box,hasConflict(selected)?Color.RED:selection,4);Rect inner=new Rect(box);if(inner.width()<8){inner.left+=inner.width()/2;inner.right=inner.left+1;}else{inner.left+=4;inner.right-=4;}inner.top+=2;inner.bottom-=2;bracket(canvas,inner,Color.WHITE,2);}}
            paint.setColor(Color.BLACK);Paint.FontMetrics metrics=paint.getFontMetrics();int step=stripStep();for(int hour=0;hour<24;hour++){String label=String.valueOf(hour24?(hour==0?24:hour):(hour%12==0?12:hour%12));WmFonts.draw(canvas,label,step/2+step*hour-WmFonts.measure(paint,label)/2,STRIP_HEIGHT-metrics.descent,paint);}
            canvas.drawRect(0,STRIP_HEIGHT-1,getStripBackingWidth(),STRIP_HEIGHT,paint);canvas.restoreToCount(saved);
        }
        private void bracket(Canvas canvas,Rect box,int color,int thickness){
            paint.setColor(color);int half=thickness/2;
            canvas.drawRect(box.left,box.top-half,box.right,box.top+thickness-half,paint);canvas.drawRect(box.left,box.bottom-half,box.right,box.bottom+thickness-half,paint);
            canvas.drawRect(box.left,box.top-half,box.left+thickness,box.top-half+2*thickness,paint);canvas.drawRect(box.right-thickness,box.top-half,box.right,box.top-half+2*thickness,paint);
            canvas.drawRect(box.left,box.bottom+half-2*thickness,box.left+thickness,box.bottom+half,paint);canvas.drawRect(box.right-thickness,box.bottom+half-2*thickness,box.right,box.bottom+half,paint);
        }
    }
    private void stopElapsedRefresh(){
        removeCallbacks(elapsedRefresh);
        if(clockRegistered){getContext().unregisterReceiver(clockChanges);clockRegistered=false;}
    }
    private void scheduleElapsedRefresh(){
        if(timedRows==null)return;
        removeCallbacks(elapsedRefresh);
        if(getWindowToken()==null||getWindowVisibility()!=VISIBLE||!isShown()){stopElapsedRefresh();return;}
        long now=System.currentTimeMillis(),next=Long.MAX_VALUE;boolean dynamic=false,pending=false;
        for(Event event:timedEvents)if(event.endTimeMillis!=null){
            dynamic=true;long end=event.endTimeMillis.longValue();if(end>=now&&end<=next){next=end;pending=true;}
        }
        if(!dynamic){stopElapsedRefresh();return;}
        if(!clockRegistered){
            IntentFilter filter=new IntentFilter(Intent.ACTION_TIME_CHANGED);filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            if(android.os.Build.VERSION.SDK_INT>=33)ClockApi33.register(getContext(),clockChanges,filter);
            else getContext().registerReceiver(clockChanges,filter);
            clockRegistered=true;
        }
        if(pending){long delay=next-now;if(delay<0||delay>Integer.MAX_VALUE)delay=Integer.MAX_VALUE;postDelayed(elapsedRefresh,delay+1);}
    }
    private static final class ClockApi33 {static void register(Context context,BroadcastReceiver receiver,IntentFilter filter){context.registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED);}}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();scheduleElapsedRefresh();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);scheduleElapsedRefresh();}
    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);scheduleElapsedRefresh();}
    @Override protected void onDetachedFromWindow(){stopElapsedRefresh();stopStripPan();cancelGestures();super.onDetachedFromWindow();}
}
