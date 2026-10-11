package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.View;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/** Single-month picker body, distinct from caldll MONTHVIEW.
 * commctrl03287EAC: centered digits, retained selection, no cell grid,
 * title-colored weekday labels, trailing gray text and rectangular today marker.
 * Android metrics, footer wording and regional week arithmetic are adaptations.
 */
public final class WmMonthCalendar extends View implements WmControls.RestyleListener,WmControls.LanguageListener {
    public interface OnDateListener {void choose(int year,int month,int day);}
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics regularMetrics=new Paint.FontMetrics(),boldMetrics=new Paint.FontMetrics();
    private final float[] numberWidths=new float[42],weekdayWidths=new float[7],weekWidths=new float[6];
    private boolean geometryDirty=true,extentDirty=true;
    private final Point natural=new Point();
    private final Rect cell=new Rect();
    private final Calendar first=new GregorianCalendar(TimeZone.getTimeZone("UTC"),Locale.US),selected=new GregorianCalendar(TimeZone.getTimeZone("UTC"),Locale.US),today=new GregorianCalendar();
    private final WmDateTimeField.Value minimum,maximum;
    private final int[][] dates=new int[42][3];
    private final String[] numbers=new String[42],weekdays=new String[7],weeks=new String[6];
    private Typeface font,bold;private Locale locale;private OnDateListener listener,dateChanged;
    private Runnable noneListener;
    private int year,month,firstDay,headerHeight,footerHeight,footerRowHeight,gutter,rowHeight,pressed=-1,accent=0xff000080;
    private boolean weekNumbers,tracking;
    private String language,todayLabel,noneLabel;
    public WmMonthCalendar(Context context,Typeface font,Typeface bold,Locale locale){
        this(context,font,bold,locale,new WmDateTimeField.Value(1900,1,1,0,0),new WmDateTimeField.Value(2999,12,31,0,0));
    }
    /** Owner full-date bounds. Arithmetic remains proleptic Gregorian. */
    public WmMonthCalendar(Context context,Typeface font,Typeface bold,Locale locale,WmDateTimeField.Value minimum,WmDateTimeField.Value maximum){
        super(context);if(locale==null)throw new IllegalArgumentException("locale required");this.font=font;this.bold=bold;this.locale=locale;this.language=language(locale);
        this.minimum=bound(minimum);this.maximum=bound(maximum);if(compareDate(this.minimum.year,this.minimum.month,this.minimum.day,this.maximum)>0)throw new IllegalArgumentException("Ordered date bounds required");
        for(Calendar calendar:new Calendar[]{first,selected,today})((GregorianCalendar)calendar).setGregorianChange(new java.util.Date(Long.MIN_VALUE));
        int y=today.get(Calendar.YEAR),m=today.get(Calendar.MONTH)+1,d=today.get(Calendar.DAY_OF_MONTH);WmDateTimeField.Value initial=compareDate(y,m,d,this.minimum)<0?this.minimum:compareDate(y,m,d,this.maximum)>0?this.maximum:new WmDateTimeField.Value(y,m,d,0,0);
        setSelectedDate(initial.year,initial.month,initial.day);setMonth(initial.year,initial.month);setFocusable(true);setClickable(true);setSoundEffectsEnabled(false);
    }
    static WmDateTimeField.Value bound(WmDateTimeField.Value value){if(value==null)throw new IllegalArgumentException("Date bound required");return WmDateField.normalized(value);}
    static int compareDate(int year,int month,int day,WmDateTimeField.Value value){if(year!=value.year)return year<value.year?-1:1;if(month!=value.month)return month<value.month?-1:1;return day==value.day?0:day<value.day?-1:1;}
    private boolean within(int year,int month,int day){return compareDate(year,month,day,minimum)>=0&&compareDate(year,month,day,maximum)<=0;}
    public void setOnDateListener(OnDateListener listener){this.listener=listener;}
    /** User selection changes; programmatic setters remain silent. */
    public void setOnDateChangeListener(OnDateListener listener){dateChanged=listener;}
    /** Optional second footer row. The owner clears/dismisses without changing the selected date. */
    public void setOnNoneListener(Runnable listener){noneListener=listener;geometryDirty=true;extentDirty=true;cancelTouch();requestLayout();invalidate();}
    public void setSelectedDate(int year,int month,int day){new WmDateTimeField.Value(year,month,day,0,0);if(!within(year,month,day))throw new IllegalArgumentException("date outside owner range");Calendar date=(Calendar)selected.clone();date.clear();date.setLenient(false);date.set(year,month-1,day,12,0,0);date.getTimeInMillis();selected.setTimeInMillis(date.getTimeInMillis());cancelTouch();invalidate();}
    public int[] selectedDate(){return new int[]{selected.get(Calendar.YEAR),selected.get(Calendar.MONTH)+1,selected.get(Calendar.DAY_OF_MONTH)};}
    public void setMonth(int year,int month){if(month<1||month>12||year<minimum.year||year>maximum.year||year==minimum.year&&month<minimum.month||year==maximum.year&&month>maximum.month)throw new IllegalArgumentException("month outside owner range");this.year=year;this.month=month;calculate();cancelTouch();invalidate();}
    public void setLocale(Locale locale){if(locale==null)throw new IllegalArgumentException("locale required");this.locale=locale;this.language=language(locale);calculate();cancelTouch();requestLayout();invalidate();}
    private String language(Locale locale){return WmStrings.language(getContext(),android.os.Build.VERSION.SDK_INT>=21?Modern.tag(locale):locale.toString().replace('_','-'));}
    private static final class Modern {static String tag(Locale locale){return locale.toLanguageTag();}}
    @Override public void onWmLanguageChanged(String language){if(language==null)throw new IllegalArgumentException("Language required");this.language=WmStrings.language(getContext(),language);footerLabels();cancelTouch();requestLayout();invalidate();}
    public void setFirstDayOfWeek(int day){if(day!=Calendar.SUNDAY&&day!=Calendar.MONDAY)throw new IllegalArgumentException("first weekday must be Sunday or Monday");firstDay=day;calculate();cancelTouch();invalidate();}
    public int getFirstDayOfWeek(){return firstDay==0?Calendar.getInstance(locale).getFirstDayOfWeek():firstDay;}
    public void setWeekNumbersVisible(boolean visible){weekNumbers=visible;geometryDirty=true;extentDirty=true;cancelTouch();requestLayout();invalidate();}
    public void setColors(int accent){this.accent=accent;invalidate();}
    public int[] dateAt(int index){if(index<0||index>=42)throw new IllegalArgumentException("cell index outside0..41");return dates[index].clone();}
    public int getHeaderHeight(){geometry();return headerHeight;}
    public int getWeekColumnWidth(){geometry();return gutter;}
    public Rect cellBounds(int index){if(index<0||index>=42)throw new IllegalArgumentException("cell index outside0..41");geometry();Rect result=new Rect();bounds(result,index);return result;}
    private void calculate(){
        first.clear();first.set(year,month-1,1,12,0,0);first.add(Calendar.DATE,-((first.get(Calendar.DAY_OF_WEEK)-getFirstDayOfWeek()+7)%7));
        Calendar date=(Calendar)first.clone();date.setFirstDayOfWeek(getFirstDayOfWeek());date.setMinimalDaysInFirstWeek(Calendar.getInstance(locale).getMinimalDaysInFirstWeek());String[] names=new DateFormatSymbols(locale).getShortWeekdays();
        for(int i=0;i<42;i++){dates[i][0]=date.get(Calendar.YEAR);dates[i][1]=date.get(Calendar.MONTH)+1;dates[i][2]=date.get(Calendar.DAY_OF_MONTH);numbers[i]=String.valueOf(dates[i][2]);if(i<7){String name=names[date.get(Calendar.DAY_OF_WEEK)];weekdays[i]=name.length()>1?name.substring(0,1):name;}if(i%7==0)weeks[i/7]=String.valueOf(date.get(Calendar.WEEK_OF_YEAR));date.add(Calendar.DATE,1);}
        geometryDirty=true;footerLabels();
    }
    private void footerLabels(){todayLabel=WmStrings.text(getContext(),language,"오늘:")+" "+WmDateFormats.shortDate(locale).format(today.getTime());noneLabel=WmStrings.text(getContext(),language,"없음");extentDirty=true;geometryDirty=true;}
    // 032877BC at logical192DPI; Android extents replace GDI text extents.
    Point naturalSize(){
        if(extentDirty){
            extentDirty=false;paint.setTextSize(WmMetrics.FONT);paint.setTypeface(bold);paint.getFontMetrics(boldMetrics);
            int columns=weekNumbers?8:7,cellWidth=(int)Math.ceil(WmFonts.measure(paint,"00"));
            for(String weekday:weekdays)cellWidth=Math.max(cellWidth,(int)Math.ceil(WmFonts.measure(paint,weekday)));
            String[] names=new DateFormatSymbols(locale).getMonths();String language=locale.getLanguage();
            boolean cjk="ko".equals(language)||"ja".equals(language)||"zh".equals(language);
            String year="0000"+("ko".equals(language)?"년":cjk?"年":"");
            for(int i=0;i<12;i++){String title=cjk?year+" "+names[i]:names[i]+" "+year;cellWidth=Math.max(cellWidth,((int)Math.ceil(WmFonts.measure(paint,title))+104)/columns);}
            cellWidth+=2;int textHeight=(int)Math.ceil(boldMetrics.descent-boldMetrics.ascent),cellHeight=textHeight+2;
            natural.x=Math.max(2+columns*cellWidth,24+(int)Math.ceil(WmFonts.measure(paint,todayLabel))+cellWidth);
            natural.x=Math.max(natural.x,(int)Math.ceil(WmFonts.measure(paint,noneLabel)));
            natural.y=9*cellHeight-6+(textHeight+8)*(noneListener==null?1:2);
        }
        return new Point(natural.x,natural.y);
    }
    private void geometry(){
        if(!geometryDirty)return;geometryDirty=false;paint.setTextSize(WmMetrics.FONT);paint.setTypeface(bold);paint.getFontMetrics(boldMetrics);headerHeight=(int)Math.ceil(boldMetrics.descent-boldMetrics.ascent)+2;footerRowHeight=headerHeight+6;footerHeight=footerRowHeight*(noneListener==null?1:2);
        for(int i=0;i<7;i++)weekdayWidths[i]=WmFonts.measure(paint,weekdays[i]);paint.setTypeface(font);paint.getFontMetrics(regularMetrics);
        for(int i=0;i<42;i++)numberWidths[i]=WmFonts.measure(paint,numbers[i]);for(int i=0;i<6;i++)weekWidths[i]=WmFonts.measure(paint,weeks[i]);
        gutter=weekNumbers?Math.max(0,getWidth()-2)/8:0;rowHeight=Math.max(1,(getHeight()-headerHeight-footerHeight)/6);
    }
    private void bounds(Rect target,int index){int col=index%7,row=index/7,width=getWidth()-gutter;target.set(gutter+col*width/7,headerHeight+row*rowHeight,gutter+(col+1)*width/7,headerHeight+(row+1)*rowHeight);}
    private static boolean same(int[] date,Calendar other){return date[0]==other.get(Calendar.YEAR)&&date[1]==other.get(Calendar.MONTH)+1&&date[2]==other.get(Calendar.DAY_OF_MONTH);}
    private void centered(Canvas canvas,String text,Rect area,float width,Paint.FontMetrics fm){int save=canvas.save();canvas.clipRect(area);WmFonts.draw(canvas,text,area.exactCenterX()-width/2,area.exactCenterY()-(fm.ascent+fm.descent)/2,paint);canvas.restoreToCount(save);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);geometry();paint.setTypeface(bold);paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.FILL);canvas.drawRect(0,0,getWidth(),getHeight(),paint);
        int width=getWidth()-gutter;paint.setColor(accent);for(int col=0;col<7;col++){cell.set(gutter+col*width/7,0,gutter+(col+1)*width/7,headerHeight);centered(canvas,weekdays[col],cell,weekdayWidths[col],boldMetrics);}
        paint.setColor(Color.BLACK);canvas.drawRect(gutter,headerHeight-2,getWidth(),headerHeight,paint);
        paint.setTypeface(font);for(int i=0;i<42;i++){bounds(cell,i);boolean available=within(dates[i][0],dates[i][1],dates[i][2]),chosen=available&&same(dates[i],selected);if(chosen){paint.setColor(accent);canvas.drawRect(cell,paint);}if(same(dates[i],today)){paint.setColor(Color.BLACK);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);canvas.drawRect(cell.left+1,cell.top+1,cell.right-1,cell.bottom-1,paint);paint.setStyle(Paint.Style.FILL);}paint.setColor(chosen?Color.WHITE:!available||dates[i][1]!=month?0xffb0b0b0:Color.BLACK);centered(canvas,numbers[i],cell,numberWidths[i],regularMetrics);if(chosen&&hasFocus())focusRect(canvas,cell);}
        if(gutter>0){paint.setColor(Color.BLACK);canvas.drawRect(gutter-2,headerHeight,gutter,headerHeight+6*rowHeight,paint);for(int row=0;row<6;row++){cell.set(0,headerHeight+row*rowHeight,gutter-2,headerHeight+(row+1)*rowHeight);centered(canvas,weeks[row],cell,weekWidths[row],regularMetrics);}}
        paint.setTypeface(bold);paint.setColor(Color.BLACK);int footerTop=headerHeight+6*rowHeight;cell.set(4,footerTop,getWidth(),noneListener==null?getHeight():footerTop+footerRowHeight);footer(canvas,todayLabel);
        if(noneListener!=null){cell.set(4,footerTop+footerRowHeight,getWidth(),getHeight());footer(canvas,noneLabel);}
    }
    private void footer(Canvas canvas,String label){int save=canvas.save();canvas.clipRect(cell);WmFonts.draw(canvas,label,cell.left,cell.exactCenterY()-(boldMetrics.ascent+boldMetrics.descent)/2,paint);canvas.restoreToCount(save);}
    private void focusRect(Canvas canvas,Rect area){paint.setColor(Color.BLACK);for(int x=area.left;x<area.right;x+=4){canvas.drawRect(x,area.top,Math.min(x+2,area.right),area.top+2,paint);canvas.drawRect(x,area.bottom-2,Math.min(x+2,area.right),area.bottom,paint);}for(int y=area.top;y<area.bottom;y+=4){canvas.drawRect(area.left,y,area.left+2,Math.min(y+2,area.bottom),paint);canvas.drawRect(area.right-2,y,area.right,Math.min(y+2,area.bottom),paint);}}
    private int hit(float x,float y){geometry();if(x<0||x>=getWidth()||y<0||y>=getHeight())return -1;if(y>=headerHeight+6*rowHeight)return noneListener!=null&&y>=headerHeight+6*rowHeight+footerRowHeight?43:42;if(x<gutter||y<headerHeight||getWidth()<=gutter)return -1;return Math.min(5,(int)(y-headerHeight)/rowHeight)*7+Math.min(6,(((int)(x-gutter)+1)*7-1)/(getWidth()-gutter));}
    private void cancelTouch(){tracking=false;pressed=-1;}
    @Override public boolean onTouchEvent(MotionEvent event){int action=event.getActionMasked();if(!isEnabled()){cancelTouch();return false;}if(action==MotionEvent.ACTION_DOWN){pressed=hit(event.getX(),event.getY());tracking=pressed>=0;if(tracking){requestFocus();if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);}return tracking;}if(!tracking)return false;if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN||event.getPointerCount()>1){cancelTouch();return true;}if(action==MotionEvent.ACTION_MOVE){if(hit(event.getX(),event.getY())!=pressed)pressed=-1;return true;}if(action==MotionEvent.ACTION_UP){int index=pressed;cancelTouch();if(index>=0&&hit(event.getX(),event.getY())==index){if(index==43){Runnable none=noneListener;if(none!=null){performClick();none.run();}}else{int[] date=index==42?new int[]{today.get(Calendar.YEAR),today.get(Calendar.MONTH)+1,today.get(Calendar.DAY_OF_MONTH)}:dates[index];int y=date[0],m=date[1],d=date[2];if(within(y,m,d)){int[] previous=selectedDate();setSelectedDate(y,m,d);notifyDateChanged(previous);performClick();if(listener!=null)listener.choose(y,m,d);}}}return true;}return true;}
    private void notifyDateChanged(int[] previous){
        int[] current=selectedDate();if(dateChanged!=null&&!java.util.Arrays.equals(previous,current))dateChanged.choose(current[0],current[1],current[2]);
    }
    @Override public boolean onKeyDown(int code,KeyEvent event){
        if(!isEnabled()||event.isAltPressed()||event.isCtrlPressed()&&code!=KeyEvent.KEYCODE_PAGE_UP&&code!=KeyEvent.KEYCODE_PAGE_DOWN)return super.onKeyDown(code,event);
        Calendar next=(Calendar)selected.clone();
        switch(code){
            case KeyEvent.KEYCODE_DPAD_LEFT:next.add(Calendar.DAY_OF_MONTH,-1);break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:next.add(Calendar.DAY_OF_MONTH,1);break;
            case KeyEvent.KEYCODE_DPAD_UP:next.add(Calendar.DAY_OF_MONTH,-7);break;
            case KeyEvent.KEYCODE_DPAD_DOWN:next.add(Calendar.DAY_OF_MONTH,7);break;
            case KeyEvent.KEYCODE_PAGE_UP:next.add(event.isCtrlPressed()?Calendar.YEAR:Calendar.MONTH,-1);break;
            case KeyEvent.KEYCODE_PAGE_DOWN:next.add(event.isCtrlPressed()?Calendar.YEAR:Calendar.MONTH,1);break;
            case KeyEvent.KEYCODE_MOVE_HOME:next.set(Calendar.DAY_OF_MONTH,1);break;
            case KeyEvent.KEYCODE_MOVE_END:next.set(Calendar.DAY_OF_MONTH,next.getActualMaximum(Calendar.DAY_OF_MONTH));break;
            default:return super.onKeyDown(code,event);
        }
        int y=next.get(Calendar.YEAR),m=next.get(Calendar.MONTH)+1,d=next.get(Calendar.DAY_OF_MONTH);
        if(within(y,m,d)){int[] previous=selectedDate();setSelectedDate(y,m,d);setMonth(y,m);notifyDateChanged(previous);}
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);geometryDirty=true;cancelTouch();}
    @Override protected void onFocusChanged(boolean gain,int direction,Rect previous){super.onFocusChanged(gain,direction,previous);invalidate();}
    @Override protected void onDetachedFromWindow(){cancelTouch();super.onDetachedFromWindow();}
    @Override public void onWmRestyle(Typeface font,Typeface bold,int selection,int tabColor){this.font=font;this.bold=bold;accent=selection;geometryDirty=true;extentDirty=true;cancelTouch();requestLayout();invalidate();}
}
