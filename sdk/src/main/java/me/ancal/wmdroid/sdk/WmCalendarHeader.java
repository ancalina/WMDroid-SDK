package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/** CALENDAR top ToolbarWindow32 roles from caldll. Source logical VGA height44,
 * date reservation140(CJK)/160(other), seven32px weekdays; text controls autosize.
 * Navigation buttons36px, separator12px follow
 * the native192DPI model; horizontal gaps project native ARM; Android text extents remain adaptations.
 * The caller owns date selection and popup placement; no PIM access here.
 */
public final class WmCalendarHeader extends View implements WmControls.RestyleListener {
    public static final int HEIGHT=44, AGENDA=0, DAY=1, WEEK=2, MONTH=3, YEAR=4;
    public static final int PREVIOUS=20000,NEXT=20001,TODAY=20052,MONTH_PICKER=20012,YEAR_PICKER=20022,WEEK_PICKER=20032,DATE_PICKER=20053,FIRST_WEEKDAY=20054;
    private static final int BUTTON_WIDTH=36,NAV_SEPARATOR_WIDTH=12,WEEK_RESERVE=140;
    /** Calendar command, synchronous in the control's input handling. Header IDs are the
     * navigation/picker constants or FIRST_WEEKDAY+0..6 in displayed order:
     * zero is getFirstDayOfWeek(). Owner changes dates and opens popups. */
    public interface OnCommandListener {void command(int id);}
    private static final class Item {final int id;final Rect bounds;final String text;final boolean picker;Item(int id,Rect bounds,String text,boolean picker){this.id=id;this.bounds=bounds;this.text=text;this.picker=picker;}}
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Item> items=new ArrayList<>();
    private final Calendar date=new GregorianCalendar();
    private Calendar datePreview;
    private Typeface font;private Locale locale;private int mode,selection=0xff149ca7,face=WmControls.FACE,down=-1;
    private int firstYear,lastYear,firstDayOfWeek;
    private boolean tracking,todayVisible=true,weekNumbersVisible=true;private OnCommandListener listener;private final Bitmap[] arrows=new Bitmap[3];
    private String artworkRevision;
    public WmCalendarHeader(Context context,Typeface font,Locale locale){super(context);this.font=font;setLocale(locale);setClickable(true);setSoundEffectsEnabled(false);
        refreshArtwork();
    }
    private void refreshArtwork(){
        if(artworkRevision==null)try{WmResources.prepare(getContext());}catch(IOException unavailable){}
        String revision=WmResources.revision(getContext());if(revision.equals(artworkRevision))return;
        String[] names={"previous","next","today"},symbols={"◀","▶","📅"};for(int i=0;i<3;i++)arrows[i]=WmResources.iconBitmap(getContext(),"calendar/toolbar-"+names[i]+".png",26,32,symbols[i]);
        artworkRevision=revision;
    }
    public void setTodayButtonVisible(boolean visible){if(todayVisible!=visible)cancelGesture();todayVisible=visible;invalidate();}
    public void setOnCommandListener(OnCommandListener listener){this.listener=listener;}
    public void setLocale(Locale locale){if(locale==null)throw new IllegalArgumentException("locale required");cancelGesture();this.locale=locale;requestLayout();invalidate();}
    /** Until explicitly set, weekday order follows the current locale. */
    public void setFirstDayOfWeek(int day){if(day!=Calendar.SUNDAY&&day!=Calendar.MONDAY)throw new IllegalArgumentException("first weekday must be Sunday or Monday");cancelGesture();firstDayOfWeek=day;requestLayout();invalidate();}
    public int getFirstDayOfWeek(){return firstDayOfWeek==0?Calendar.getInstance(locale).getFirstDayOfWeek():firstDayOfWeek;}
    /** caldll toolbar numbers the Monday within the configured Sunday/Monday week.
     * PIMUTIL ordinal59 ignores locale arguments and uses a fixed Jan4 boundary.
     * This is the toolbar contract, not the native function for arbitrary dates. */
    public static int toolbarWeekNumber(int year,int month,int day,int firstDay){
        if(firstDay!=Calendar.SUNDAY&&firstDay!=Calendar.MONDAY)throw new IllegalArgumentException("first weekday must be Sunday or Monday");
        Calendar week=new GregorianCalendar(Locale.US);week.clear();week.setLenient(false);
        week.setFirstDayOfWeek(Calendar.MONDAY);week.setMinimalDaysInFirstWeek(4);
        week.set(year,month-1,day,12,0,0);
        int weekday=week.get(Calendar.DAY_OF_WEEK);
        week.add(Calendar.DAY_OF_MONTH,-(weekday-firstDay+7)%7+(firstDay==Calendar.SUNDAY?1:0));
        return dateWeekNumber(week);
    }
    /** Literal PIMUTIL ordinal59 arithmetic; includes native late-December week0.
     * Verified source range1900..2999. Java division, like ARM, truncates toward0. */
    static int dateWeekNumber(Calendar date){
        int year=date.get(Calendar.YEAR),month=date.get(Calendar.MONTH),day=date.get(Calendar.DAY_OF_MONTH);
        Calendar anchor=(Calendar)date.clone();anchor.set(year,Calendar.JANUARY,4);int anchorDow=anchor.get(Calendar.DAY_OF_WEEK)-1;
        if(month==Calendar.JANUARY&&day<4&&anchorDow>0&&anchorDow-1<4-day){anchor.set(Calendar.YEAR,year-1);anchorDow=anchor.get(Calendar.DAY_OF_WEEK)-1;}
        if(month==Calendar.DECEMBER&&day>28){anchor.set(Calendar.YEAR,year+1);int nextDow=anchor.get(Calendar.DAY_OF_WEEK)-1;if(33-day<nextDow||nextDow==0)anchorDow=nextDow;else anchor.set(Calendar.YEAR,year);}
        int delta=date.get(Calendar.DAY_OF_YEAR)-4,anchorYear=anchor.get(Calendar.YEAR);
        if(anchorYear<year)delta+=anchor.getActualMaximum(Calendar.DAY_OF_YEAR);else if(anchorYear>year)delta-=date.getActualMaximum(Calendar.DAY_OF_YEAR);
        int dow=date.get(Calendar.DAY_OF_WEEK)-1,difference=dow-anchorDow;
        if(anchorDow!=0){if(dow==0)difference+=7;}else if(dow!=0)difference-=7;
        return (delta-difference)/7+1;
    }
    /** Native week-number preference suppresses only the Week toolbar picker. */
    public void setWeekNumbersVisible(boolean visible){cancelGesture();weekNumbersVisible=visible;requestLayout();invalidate();}
    public void setDate(int year,int month,int day){cancelGesture();datePreview=null;date.clear();date.setLenient(false);date.set(year,month-1,day,12,0,0);date.getTimeInMillis();firstYear=lastYear=year;invalidate();}
    /** Stock DTP changes its caption before Calendar commits the selected date. */
    public void setDatePreview(int year,int month,int day){Calendar next=new GregorianCalendar();next.clear();next.setLenient(false);next.set(year,month-1,day,12,0,0);next.getTimeInMillis();datePreview=next;invalidate();}
    public void clearDatePreview(){if(datePreview!=null){datePreview=null;invalidate();}}
    public void setMode(int mode){if(mode<AGENDA||mode>YEAR)throw new IllegalArgumentException("Unsupported Calendar mode");if(this.mode!=mode)datePreview=null;this.mode=mode;cancelGesture();requestLayout();invalidate();}
    /** Native Year toolbar can span the preceding/current or current/following year. */
    public void setYearRange(int first,int last){if(first<1||last<first||last-first>1)throw new IllegalArgumentException("invalid year range");firstYear=first;lastYear=last;invalidate();}
    public void setColors(int selection){this.selection=selection;face=WmPalette.face(selection);invalidate();}
    public Rect commandBounds(int id){layoutItems();for(Item item:items)if(item.id==id)return new Rect(item.bounds);return new Rect();}
    private boolean cjk(){String language=locale.getLanguage();return "ko".equals(language)||"ja".equals(language)||"zh".equals(language);}
    private int textItem(int id,String text,int left){int width=(int)Math.ceil(WmFonts.measure(paint,text))+10;items.add(new Item(id,new Rect(left,0,left+width,HEIGHT),text,false));return left+width;}
    private void layoutItems(){
        // Native LIST toolbar:26px glyph+10px padding,12px separator; its
        // WS_BORDER consumes window overscan: client x0/width=parent width.
        items.clear();paint.setTypeface(font);paint.setTextSize(WmMetrics.FONT);int navigation=(todayVisible?3:2)*BUTTON_WIDTH+NAV_SEPARATOR_WIDTH;int left=0,right=Math.max(left,getWidth()-navigation);
        if(mode==AGENDA||mode==DAY){
            int width=cjk()?140:160;items.add(new Item(DATE_PICKER,new Rect(left,WmMetrics.EDGE,left+width,HEIGHT+1),WmDateFormats.pickerDate(locale).format((datePreview==null?date:datePreview).getTime()),true));left+=width;
            int first=getFirstDayOfWeek();String[] names=new DateFormatSymbols(locale).getShortWeekdays();
            for(int i=0;i<7;i++){String name=names[(first-1+i)%7+1];if(name.length()>1)name=name.substring(0,1);items.add(new Item(FIRST_WEEKDAY+i,new Rect(left,0,left+(cjk()?32:(int)Math.ceil(WmFonts.measure(paint,name))+8),HEIGHT),name,false));left+=cjk()?32:(int)Math.ceil(WmFonts.measure(paint,name))+8;}
            // caldll28F81A0 flags1: hide old gap20048, measure weekdays,
            // then split the remaining blank space using signed division.
            int trailing=Math.max(1,getWidth()-WmMetrics.EDGE-left-navigation);
            int leading=Math.max(0,(navigation-(items.get(0).bounds.right)+trailing)/2-WmMetrics.EDGE);
            trailing=Math.max(1,trailing-leading);
            for(int i=1;i<items.size();i++)items.get(i).bounds.offset(leading,0);
            left+=leading;right=left+trailing;
        }else{
            String year=String.valueOf(date.get(Calendar.YEAR))+("ko".equals(locale.getLanguage())||"ja".equals(locale.getLanguage())?"년":"zh".equals(locale.getLanguage())?"年":"");
            if("ja".equals(locale.getLanguage()))year=String.valueOf(date.get(Calendar.YEAR))+"年";
            String month=new DateFormatSymbols(locale).getMonths()[date.get(Calendar.MONTH)];
            if(mode==YEAR){
                String suffix="ko".equals(locale.getLanguage())?"년":("ja".equals(locale.getLanguage())||"zh".equals(locale.getLanguage()))?"年":"";
                String years=firstYear>0?String.valueOf(firstYear)+suffix:year;
                if(lastYear>firstYear)years+="-"+lastYear+suffix;
                left=textItem(YEAR_PICKER,years,left);
            }else if(cjk()){left=textItem(YEAR_PICKER,year,left);left=textItem(MONTH_PICKER,month,left);}else{left=textItem(MONTH_PICKER,month,left);left=textItem(YEAR_PICKER,year,left);}
            if(mode==WEEK&&weekNumbersVisible){
                // caldll28F81A0 flags8: gap20049 reserves70px scaled to192DPI;
                // gap20051 then uses the newly measured Week end and CXEDGE.
                left=Math.max(left+1,getWidth()-WEEK_RESERVE-navigation);
                String week=String.valueOf(toolbarWeekNumber(date.get(Calendar.YEAR),date.get(Calendar.MONTH)+1,date.get(Calendar.DAY_OF_MONTH),getFirstDayOfWeek()))+("ko".equals(locale.getLanguage())?"주":"");left=textItem(WEEK_PICKER,week,left);}
            right=Math.max(left+1,getWidth()-WmMetrics.EDGE-navigation);
        }
        // Original initialization/view flags retain Today; explicit SDK opt-out remains available.
        if(todayVisible){items.add(new Item(TODAY,new Rect(right,0,right+BUTTON_WIDTH,HEIGHT),"",false));right+=BUTTON_WIDTH;}items.add(new Item(PREVIOUS,new Rect(right,0,right+BUTTON_WIDTH,HEIGHT),"",false));int nextLeft=right+BUTTON_WIDTH+NAV_SEPARATOR_WIDTH;items.add(new Item(NEXT,new Rect(nextLeft,0,nextLeft+BUTTON_WIDTH,HEIGHT),"",false));
    }
    @Override protected void onMeasure(int width,int height){setMeasuredDimension(MeasureSpec.getSize(width),resolveSize(HEIGHT,height));}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){refreshArtwork();cancelGesture();font=regular;setColors(selection);requestLayout();}
    private void drawWeekday(Canvas canvas,Item item,boolean checked,boolean today,int weekday){
        // caldll28F9438 custom draw skips stock pressed painting; command bounds remain full-height.
        int top=item.bounds.top+WmMetrics.EDGE,bottom=item.bounds.bottom-12;
        if(checked){paint.setColor(selection);canvas.drawRect(item.bounds.left,top,item.bounds.right,bottom,paint);}
        else if(today){paint.setColor(Color.WHITE);canvas.drawRect(item.bounds.left,top,item.bounds.right,bottom,paint);paint.setColor(Color.BLACK);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);canvas.drawRect(item.bounds.left+1,top+1,item.bounds.right-1,bottom-1,paint);paint.setStyle(Paint.Style.FILL);}
        paint.setColor(checked?Color.WHITE:today?selection:weekday==Calendar.SUNDAY?Color.RED:weekday==Calendar.SATURDAY?Color.BLUE:Color.BLACK);
        Paint.FontMetrics metrics=paint.getFontMetrics();float baseline=(top+bottom)/2f-(metrics.ascent+metrics.descent)/2f;
        canvas.clipRect(item.bounds.left,top,item.bounds.right,bottom);
        WmFonts.draw(canvas,item.text,item.bounds.centerX()-WmFonts.measure(paint,item.text)/2f,baseline,paint);
    }
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);layoutItems();paint.setColor(face);canvas.drawRect(0,0,getWidth(),getHeight(),paint);int first=getFirstDayOfWeek(),checked=(date.get(Calendar.DAY_OF_WEEK)-first+7)%7;
        Calendar weekdayDate=null,today=null;if(mode==AGENDA||mode==DAY){weekdayDate=(Calendar)date.clone();weekdayDate.add(Calendar.DAY_OF_MONTH,-checked);today=Calendar.getInstance();}
        for(Item item:items){boolean pressed=item.id==down,weekday=item.id>=FIRST_WEEKDAY&&item.id<FIRST_WEEKDAY+7,active=weekday&&item.id-FIRST_WEEKDAY==checked;int saved=canvas.save();canvas.clipRect(item.bounds);
            if(weekday){boolean isToday=weekdayDate.get(Calendar.YEAR)==today.get(Calendar.YEAR)&&weekdayDate.get(Calendar.DAY_OF_YEAR)==today.get(Calendar.DAY_OF_YEAR);drawWeekday(canvas,item,active,isToday,weekdayDate.get(Calendar.DAY_OF_WEEK));weekdayDate.add(Calendar.DAY_OF_MONTH,1);canvas.restoreToCount(saved);continue;}
            if(pressed||active){paint.setColor(selection);canvas.drawRect(item.bounds,paint);paint.setColor(selection);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);canvas.drawRect(item.bounds.left+.5f,item.bounds.top+.5f,item.bounds.right-.5f,item.bounds.bottom-.5f,paint);paint.setStyle(Paint.Style.FILL);}
            if(item.id==PREVIOUS||item.id==NEXT||item.id==TODAY){int index=item.id==PREVIOUS?0:item.id==NEXT?1:2;if(arrows[index]!=null){paint.setFilterBitmap(true);canvas.drawBitmap(arrows[index],item.bounds.centerX()-13,item.bounds.centerY()-16,paint);}else{paint.setColor(pressed?Color.WHITE:Color.BLACK);Path arrow=new Path();int cx=item.bounds.centerX(),cy=item.bounds.centerY(),direction=index==0?-1:1;arrow.moveTo(cx+direction*7,cy);arrow.lineTo(cx-direction*5,cy-10);arrow.lineTo(cx-direction*5,cy+10);arrow.close();canvas.drawPath(arrow,paint);}}
            else{paint.setColor(pressed||active?Color.WHITE:Color.BLACK);Paint.FontMetrics metrics=paint.getFontMetrics();float baseline=item.bounds.centerY()-(metrics.ascent+metrics.descent)/2f;float textRight=item.bounds.right-(item.picker?20:0),x=item.picker?item.bounds.left+4:item.bounds.left+(textRight-item.bounds.left-WmFonts.measure(paint,item.text))/2f;WmFonts.draw(canvas,item.text,x,baseline,paint);
                if(item.picker){Path triangle=new Path();float cx=item.bounds.right-11,cy=item.bounds.centerY();triangle.moveTo(cx-4,cy-2);triangle.lineTo(cx+4,cy-2);triangle.lineTo(cx,cy+3);triangle.close();canvas.drawPath(triangle,paint);}}
            canvas.restoreToCount(saved);
        }
    }
    private int hit(float x,float y){layoutItems();if(x<0||x>=getWidth()||y<0||y>=getHeight())return -1;for(int i=items.size()-1;i>=0;i--)if(items.get(i).bounds.contains((int)x,(int)y))return items.get(i).id;return -1;}
    @Override public boolean onTouchEvent(MotionEvent event){int action=event.getActionMasked();if(action==MotionEvent.ACTION_DOWN){down=hit(event.getX(),event.getY());tracking=down>=0;if(tracking){if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);invalidate();}return tracking;}if(!tracking)return false;
        if(action==MotionEvent.ACTION_CANCEL||event.getPointerCount()>1){tracking=false;down=-1;invalidate();return true;}
        if(action==MotionEvent.ACTION_MOVE){if(hit(event.getX(),event.getY())!=down){down=-1;invalidate();}return true;}
        if(action==MotionEvent.ACTION_UP){int command=down;down=-1;tracking=false;invalidate();if(command>=0&&hit(event.getX(),event.getY())==command){performClick();if(listener!=null)listener.command(command);}return true;}return true;}
    @Override public boolean performClick(){super.performClick();return true;}
    private void cancelGesture(){tracking=false;down=-1;invalidate();}
    @Override protected void onDetachedFromWindow(){cancelGesture();super.onDetachedFromWindow();}
}
