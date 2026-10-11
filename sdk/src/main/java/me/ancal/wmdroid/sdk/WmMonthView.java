package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Client-fitted MONTHVIEW, from caldll 0291AB78/0291959C/02919E2C.
 * VGA logical pixels. Android font rasterization is not GDI pixel equivalence.
 * Native flag decoration: low 12 bits / next 12 bits / bit 24 outline.
 */
public final class WmMonthView extends View implements WmControls.RestyleListener {
    public interface OnDayClickListener { void onDayClick(int year,int month,int day); }
    public interface OnWeekClickListener { void onWeekClick(int year,int month,int day); }
    public interface OnRangeChangedListener { void changed(int year,int month); }
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lunarPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private WmLunarCalendar lunar;
    private final String[] lunarCaptions=new String[42];
    private final float[] lunarWidths=new float[42];
    private final Paint.FontMetrics lunarMetrics=new Paint.FontMetrics();
    private int lunarHeight;
    private final Map<String,Integer> flags=new HashMap<String,Integer>();
    private final Calendar first=new GregorianCalendar();
    private final Calendar windowAnchor=new GregorianCalendar();
    private Typeface font,bold;
    private boolean geometryDirty=true;
    private final int[][] days=new int[42][3];
    private final String[] dayKeys=new String[42],dayNumbers=new String[42],weekNumbers=new String[6],weekdays=new String[7];
    private final int[] weekValues=new int[6];
    private final float[] weekdayWidths=new float[7],weekWidths=new float[6];
    private final Paint.FontMetrics fontMetrics=new Paint.FontMetrics(),weekMetrics=new Paint.FontMetrics();
    private final Rect drawCell=new Rect();
    private Locale locale;
    private int year,month,headerHeight,rowHeight,columnWidth,weekColumnWidth,pressed=-1,firstDayOfWeek;
    private int selection=0xff000080,face=WmControls.FACE;
    private boolean tracking,attached,weekNumbersVisible;
    private int pulseStep,pulseColor,todayIndex=-1;
    private static final int[] TODAY_COLORS={0xffffc8c8,0xffffafaf,0xffff7d7d,0xffff4b4b,0xffff1919,0xffff0000,0xffff0000,0xffff0000,0xffff0000,0xffff0000,0xffff0000};
    private final Runnable pulse=new Runnable(){public void run(){
        if(!attached||!isShown()||getWindowVisibility()!=VISIBLE||todayIndex<0)return;
        int phase=pulseStep%21;if(phase>=11)phase=21-phase;pulseColor=TODAY_COLORS[phase];invalidate();
        if(pulseStep++<200)postDelayed(this,100);
    }};
    private OnDayClickListener listener;
    private OnWeekClickListener weekListener;
    private OnRangeChangedListener rangeListener;
    private WmCalendarHeader.OnCommandListener commandListener;

    public WmMonthView(Context context,Typeface font,Locale locale){
        this(context,font,Typeface.create(font,Typeface.BOLD),locale);
    }
    public WmMonthView(Context context,Typeface regular,Typeface bold,Locale locale){
        super(context);this.font=regular;this.bold=bold;this.locale=locale;
        Calendar now=Calendar.getInstance();setMonth(now.get(Calendar.YEAR),now.get(Calendar.MONTH)+1);
        setFocusable(true);setClickable(true);setSoundEffectsEnabled(false);
    }
    public void setOnDayClickListener(OnDayClickListener listener){this.listener=listener;}
    /** Left/PageUp and Right/PageDown request the owner's month navigation. */
    public void setOnCommandListener(WmCalendarHeader.OnCommandListener listener){commandListener=listener;}
    /** Native week gutter is optional; date picker consumers keep it disabled. */
    public void setWeekNumbersVisible(boolean visible){if(weekNumbersVisible==visible)return;weekNumbersVisible=visible;geometryDirty=true;tracking=false;pressed=-1;invalidate();}
    public void setOnWeekClickListener(OnWeekClickListener listener){weekListener=listener;}
    /** UP/DOWN shifts the six-week window; callback supplies its representative month. */
    public void setOnRangeChangedListener(OnRangeChangedListener listener){rangeListener=listener;}
    /** Defensive year, one-based month, day; independent of the displayed month. */
    public int[] getWindowAnchor(){return new int[]{windowAnchor.get(Calendar.YEAR),windowAnchor.get(Calendar.MONTH)+1,windowAnchor.get(Calendar.DAY_OF_MONTH)};}
    /** Restore a shifted window after setMonth, without changing its displayed month. */
    public void setWindowAnchor(int year,int month,int day){
        Calendar value=new GregorianCalendar();value.clear();value.setLenient(false);value.set(year,month-1,day,12,0,0);
        if(year<1||month<1||month>12)throw new IllegalArgumentException("invalid window anchor");
        windowAnchor.setTimeInMillis(value.getTimeInMillis());pressed=-1;tracking=false;calculateFirst();restartPulse();invalidate();
    }
    public int getWeekColumnWidth(){geometry();return weekColumnWidth;}
    /** Number for native row column1 (Monday or Tuesday), with fixed Jan4 rule. */
    public int rowWeekNumber(int row){if(row<0||row>=6)throw new IllegalArgumentException("row outside 0..5");return weekValues[row];}
    public void setMonth(int year,int month){
        if(month<1||month>12||year<1)throw new IllegalArgumentException("invalid month");
        this.year=year;this.month=month;windowAnchor.clear();windowAnchor.set(year,month-1,1,12,0,0);pressed=-1;tracking=false;calculateFirst();restartPulse();invalidate();
    }
    public void setLocale(Locale locale){if(locale==null)throw new IllegalArgumentException("locale required");this.locale=locale;geometryDirty=true;pressed=-1;tracking=false;calculateFirst();restartPulse();requestLayout();invalidate();}
    /** Until explicitly set, weekday order follows the current locale. */
    public void setFirstDayOfWeek(int day){if(day!=Calendar.SUNDAY&&day!=Calendar.MONDAY)throw new IllegalArgumentException("first weekday must be Sunday or Monday");firstDayOfWeek=day;pressed=-1;tracking=false;calculateFirst();restartPulse();invalidate();}
    public int getFirstDayOfWeek(){return firstDayOfWeek==0?Calendar.getInstance(locale).getFirstDayOfWeek():firstDayOfWeek;}
    public void setColors(int selection,int face){this.selection=selection;this.face=face;invalidate();}
    /** Optional pack-backed captions; null removes them. Gregorian dates stay unchanged.
     * Call on the UI thread after loading the immutable calendar on a worker.
     */
    public void setLunarCalendar(WmLunarCalendar calendar){
        if(lunar==calendar)return;lunar=calendar;calculateLunar();geometryDirty=true;invalidate();
    }
    public void clearDayFlags(){flags.clear();invalidate();}
    public void setDayFlags(int year,int month,int day,int value){flags.put(key(year,month,day),value);invalidate();}
    public int getHeaderHeight(){geometry();return headerHeight;}
    /** Calendar array uses year, one-based month, day; includes adjacent months. */
    public int[] dateAt(int index){
        if(index<0||index>=42)throw new IllegalArgumentException("cell index outside 0..41");
        return days[index].clone();
    }
    /** Read-only copy; shared by rendering, hit testing and consumers. */
    public Rect cellBounds(int index){
        if(index<0||index>=42)throw new IllegalArgumentException("cell index outside 0..41");
        geometry();Rect cell=new Rect();setCellBounds(cell,index);return cell;
    }
    private void setCellBounds(Rect cell,int index){
        int col=index%7,row=index/7;
        cell.set(weekColumnWidth+col*columnWidth+2,headerHeight+row*rowHeight+2,col==6?getWidth()-2:weekColumnWidth+(col+1)*columnWidth,row==5?getHeight():headerHeight+(row+1)*rowHeight);
    }
    private static String key(int year,int month,int day){return year+":"+month+":"+day;}
    private void calculateFirst(){
        first.setTimeInMillis(windowAnchor.getTimeInMillis());
        int start=getFirstDayOfWeek();
        first.add(Calendar.DAY_OF_MONTH,-((first.get(Calendar.DAY_OF_WEEK)-start+7)%7));
        Calendar date=(Calendar)first.clone();String[] names=new DateFormatSymbols(locale).getShortWeekdays();
        for(int index=0;index<42;index++){
            int[] day=days[index];day[0]=date.get(Calendar.YEAR);day[1]=date.get(Calendar.MONTH)+1;day[2]=date.get(Calendar.DAY_OF_MONTH);dayKeys[index]=key(day[0],day[1],day[2]);dayNumbers[index]=String.valueOf(day[2]);
            if(index%7==0){int row=index/7;Calendar anchor=(Calendar)date.clone();anchor.add(Calendar.DAY_OF_MONTH,1);weekValues[row]=WmCalendarHeader.dateWeekNumber(anchor);weekNumbers[row]=String.valueOf(weekValues[row]);}
            if(index<7){String name=names[date.get(Calendar.DAY_OF_WEEK)];weekdays[index]=name.length()>1?name.substring(0,1):name;}
            date.add(Calendar.DAY_OF_MONTH,1);
        }
        calculateLunar();geometryDirty=true;
    }
    private void calculateLunar(){
        java.util.Arrays.fill(lunarCaptions,null);
        if(lunar==null)return;
        for(int index=0;index<42;index++){
            int[] day=days[index];WmLunarCalendar.Date date=lunar.convert(day[0],day[1],day[2]);
            // Native first failure suppresses all captions; later failure preserves the prefix.
            if(date==null)break;lunarCaptions[index]=lunar.caption(date);
        }
    }
    // PIMUTIL170 COLORREF: Sunday red, Saturday blue, other WINDOWTEXT.
    private static int weekdayColor(int weekday){return weekday==Calendar.SUNDAY?Color.RED:weekday==Calendar.SATURDAY?Color.BLUE:Color.BLACK;}
    private Typeface calendarFont(){String language=locale.getLanguage();return "ko".equals(language)||"ja".equals(language)||"zh".equals(language)?font:bold;}
    private void geometry(){
        if(!geometryDirty)return;geometryDirty=false;paint.setTypeface(calendarFont());paint.setTextSize(WmMetrics.FONT);
        paint.getFontMetrics(fontMetrics);Paint.FontMetrics metrics=fontMetrics;
        headerHeight=(int)Math.ceil(metrics.descent-metrics.ascent+metrics.leading)+2;
        // 0291A7AC measures "28" with font slot54 and rounds up to a multiple of7.
        // Native SHPreProcessLogFont maps CJK bold700 to regular400.
        paint.setTypeface(calendarFont());paint.getFontMetrics(weekMetrics);for(int row=0;row<6;row++)weekWidths[row]=WmFonts.measure(paint,weekNumbers[row]);
        weekColumnWidth=weekNumbersVisible?Math.min(Math.max(0,getWidth()-7),7*((int)Math.ceil(WmFonts.measure(paint,"28"))/7+1)):0;
        columnWidth=(getWidth()-weekColumnWidth)/7;rowHeight=Math.max(1,(getHeight()-headerHeight+3)/6);
        paint.setTypeface(calendarFont());for(int col=0;col<7;col++)weekdayWidths[col]=WmFonts.measure(paint,weekdays[col]);
        // Android regular font adaptation: native lunar drawing inherits an unresolved DC font.
        lunarPaint.setTypeface(font);lunarPaint.setTextSize(WmMetrics.FONT);lunarPaint.getFontMetrics(lunarMetrics);
        lunarHeight=(int)Math.ceil(lunarMetrics.descent-lunarMetrics.ascent);
        for(int index=0;index<42;index++)lunarWidths[index]=lunarCaptions[index]==null?0:(float)Math.ceil(WmFonts.measure(lunarPaint,lunarCaptions[index]));
    }
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){tracking=false;pressed=-1;font=regular;this.bold=bold;geometryDirty=true;this.selection=selection;face=WmPalette.face(selection);requestLayout();invalidate();}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);geometryDirty=true;}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);geometry();paint.setStyle(Paint.Style.FILL);paint.setColor(face);canvas.drawRect(0,0,getWidth(),headerHeight,paint);
        int firstWeekday=first.get(Calendar.DAY_OF_WEEK);
        float baseline=(headerHeight-fontMetrics.descent-fontMetrics.ascent)/2;
        for(int column=0;column<7;column++){
            int weekday=(firstWeekday-1+column)%7+1;String name=weekdays[column];
            paint.setColor(weekdayColor(weekday));
            WmFonts.draw(canvas,name,weekColumnWidth+column*columnWidth+(columnWidth-weekdayWidths[column])/2,baseline,paint);
        }
        if(weekColumnWidth>0){
            paint.setTypeface(calendarFont());paint.setColor(face);canvas.drawRect(0,headerHeight,weekColumnWidth,getHeight(),paint);paint.setColor(Color.BLACK);
            for(int row=0;row<6;row++){String number=weekNumbers[row];int save=canvas.save();canvas.clipRect(0,headerHeight+row*rowHeight,weekColumnWidth,Math.min(getHeight(),headerHeight+(row+1)*rowHeight));WmFonts.draw(canvas,number,(weekColumnWidth-weekWidths[row])/2,headerHeight+row*rowHeight+1-weekMetrics.ascent,paint);canvas.restoreToCount(save);}
        }
        paint.setTypeface(calendarFont());Calendar now=Calendar.getInstance();
        for(int index=0;index<42;index++){
            int[] day=days[index];setCellBounds(drawCell,index);Rect cell=drawCell;boolean down=index==pressed;
            boolean today=day[0]==now.get(Calendar.YEAR)&&day[1]==now.get(Calendar.MONTH)+1&&day[2]==now.get(Calendar.DAY_OF_MONTH);
            paint.setColor(down?selection:day[1]!=month?face:Color.WHITE);canvas.drawRect(cell,paint);
            String number=dayNumbers[index];
            Paint.FontMetrics metrics=fontMetrics;
            if(today){float width=WmFonts.measure(paint,number);paint.setColor(down?Color.WHITE:Color.BLACK);canvas.drawRect(cell.left,cell.top,Math.min(cell.right,cell.left+width+8),Math.min(cell.bottom,cell.top+metrics.descent-metrics.ascent+4),paint);}
            paint.setColor(today?(down?Color.BLACK:Color.WHITE):down?Color.WHITE:weekdayColor((firstWeekday-1+index)%7+1));
            int save=canvas.save();canvas.clipRect(cell);WmFonts.draw(canvas,number,cell.left+2,cell.top-1-metrics.ascent,paint);canvas.restoreToCount(save);
            Integer value=flags.get(dayKeys[index]);if(value!=null)drawFlags(canvas,cell,value,down);
        }
        // Native PatBlt grid: 1@96 DPI becomes two logical VGA pixels.
        paint.setColor(Color.BLACK);for(int col=0;col<8;col++){int x=col==7?getWidth()-2:weekColumnWidth+col*columnWidth;canvas.drawRect(x,headerHeight,x+2,getHeight(),paint);}
        for(int row=0;row<6;row++){int y=headerHeight+row*rowHeight;canvas.drawRect(weekColumnWidth,y,getWidth(),y+2,paint);}
        if(todayIndex>=0&&pulseColor!=0){setCellBounds(drawCell,todayIndex);Rect cell=drawCell;paint.setColor(pulseColor);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);canvas.drawRect(cell.left-1,cell.top-1,cell.right+1,cell.bottom+1,paint);paint.setStyle(Paint.Style.FILL);}
        lunarPaint.setColor(Color.BLACK);
        for(int index=0;index<42;index++)if(lunarCaptions[index]!=null){
            setCellBounds(drawCell,index);float x=drawCell.right-2*WmMetrics.BORDER-lunarWidths[index];int y=drawCell.bottom-WmMetrics.BORDER-lunarHeight;
            int saved=canvas.save();canvas.clipRect(x,y,drawCell.right,drawCell.bottom);
            WmFonts.draw(canvas,lunarCaptions[index],x,y-lunarMetrics.ascent,lunarPaint);canvas.restoreToCount(saved);
        }
    }
    private void drawFlags(Canvas canvas,Rect cell,int value,boolean down){
        // 0290844C and 029196A4: at192 DPI inner18, inset4, outer26.
        float outer=Math.min(26,Math.min(cell.width(),cell.height())),inset=4,inner=Math.max(0,outer-2*inset);
        float right=cell.right-4,bottom=cell.bottom-4,left=right-outer,top=bottom-outer;
        paint.setColor(down?Color.WHITE:selection);
        if((value&0x1000000)!=0){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);canvas.drawRect(left+1,top+1,right-1,bottom-1,paint);paint.setStyle(Paint.Style.FILL);}
        boolean lower=(value&0xfff000)!=0,upper=(value&0xfff)!=0;if(!lower&&!upper)return;
        left+=inset;top+=inset;right=left+inner;bottom=top+inner;
        if(lower&&upper)canvas.drawRect(left,top,right,bottom,paint);
        else{Path path=new Path();path.moveTo(left,bottom);path.lineTo(right,top);path.lineTo(upper?left:right,upper?top:bottom);path.close();canvas.drawPath(path,paint);}
    }
    private int hit(float x,float y){geometry();if(columnWidth<=0||x<0||x>=getWidth()||y<headerHeight||y>=getHeight())return -1;int row=Math.min(5,(int)(y-headerHeight)/rowHeight);return x<weekColumnWidth?42+row:row*7+Math.min(6,(int)(x-weekColumnWidth)/columnWidth);}
    @Override public boolean onTouchEvent(MotionEvent event){
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){pressed=hit(event.getX(),event.getY());tracking=pressed>=0;if(tracking){if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);invalidate();}return tracking;}
        if(!tracking)return false;
        if(event.getPointerCount()>1||action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_CANCEL){tracking=false;pressed=-1;invalidate();return true;}
        if(action==MotionEvent.ACTION_MOVE){if(hit(event.getX(),event.getY())!=pressed){pressed=-1;invalidate();}return true;}
        if(action==MotionEvent.ACTION_UP){int index=pressed;pressed=-1;tracking=false;invalidate();if(index>=0&&hit(event.getX(),event.getY())==index){int[] day=dateAt(index>=42?(index-42)*7+1:index);performClick();if(index>=42){if(weekListener!=null)weekListener.onWeekClick(day[0],day[1],day[2]);}else if(listener!=null)listener.onDayClick(day[0],day[1],day[2]);}return true;}
        return true;
    }
    @Override public boolean onKeyDown(int keyCode,KeyEvent event){
        if(isEnabled()&&(keyCode==KeyEvent.KEYCODE_DPAD_UP||keyCode==KeyEvent.KEYCODE_DPAD_DOWN)){
            int direction=keyCode==KeyEvent.KEYCODE_DPAD_DOWN?1:-1;
            Calendar edge=(Calendar)first.clone();edge.add(Calendar.DAY_OF_MONTH,direction>0?42:-1);
            if(direction>0?edge.get(Calendar.YEAR)>2999:edge.get(Calendar.YEAR)<1900)return true;
            windowAnchor.add(Calendar.DAY_OF_MONTH,direction*7);pressed=-1;tracking=false;calculateFirst();
            Calendar display=(Calendar)first.clone();if(display.getActualMaximum(Calendar.DAY_OF_MONTH)-display.get(Calendar.DAY_OF_MONTH)<21)display.add(Calendar.MONTH,1);
            year=display.get(Calendar.YEAR);month=display.get(Calendar.MONTH)+1;
            if(rangeListener!=null)rangeListener.changed(year,month);
            restartPulse();invalidate();return true;
        }
        if(isEnabled()&&commandListener!=null){
            if(keyCode==KeyEvent.KEYCODE_DPAD_LEFT||keyCode==KeyEvent.KEYCODE_PAGE_UP){commandListener.command(WmCalendarHeader.PREVIOUS);return true;}
            if(keyCode==KeyEvent.KEYCODE_DPAD_RIGHT||keyCode==KeyEvent.KEYCODE_PAGE_DOWN){commandListener.command(WmCalendarHeader.NEXT);return true;}
        }
        return super.onKeyDown(keyCode,event);
    }
    @Override public boolean performClick(){super.performClick();return true;}
    private void restartPulse(){
        removeCallbacks(pulse);pulseStep=0;pulseColor=0;todayIndex=-1;
        if(first==null)return;
        Calendar now=Calendar.getInstance();
        for(int index=0;index<42;index++){int[] day=dateAt(index);if(day[0]==now.get(Calendar.YEAR)&&day[1]==now.get(Calendar.MONTH)+1&&day[2]==now.get(Calendar.DAY_OF_MONTH)){todayIndex=index;break;}}
        if(attached&&isShown()&&getWindowVisibility()==VISIBLE&&todayIndex>=0)postDelayed(pulse,100);
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;restartPulse();}
    @Override protected void onVisibilityChanged(View view,int visibility){super.onVisibilityChanged(view,visibility);if(first!=null){if(visibility==VISIBLE)restartPulse();else removeCallbacks(pulse);}}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(first!=null){if(visibility==VISIBLE)restartPulse();else removeCallbacks(pulse);}}
    @Override protected void onDetachedFromWindow(){attached=false;removeCallbacks(pulse);tracking=false;pressed=-1;super.onDetachedFromWindow();}
}
