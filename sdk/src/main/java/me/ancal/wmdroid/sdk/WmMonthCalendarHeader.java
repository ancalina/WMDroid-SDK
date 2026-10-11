package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import java.text.DateFormatSymbols;
import java.util.Locale;

/** SysMonthCal32 title, separate from Calendar's toolbar. Native 192-DPI button
 * extents and font-cell title formula; Android font metrics are an adaptation.
 * Commands use completed UP; native DOWN/repeat timer is deliberately not used. */
public final class WmMonthCalendarHeader extends View implements WmControls.RestyleListener {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics metrics=new Paint.FontMetrics();
    private final Rect previous=new Rect(),next=new Rect(),monthBounds=new Rect(),yearBounds=new Rect();
    private final Path arrow=new Path();
    private Typeface bold;private Locale locale;private int year=2026,month=1,selection=0xff149ca7,down=-1,cellHeight;
    private String first="",second="";private float firstX,secondX,baseline;private boolean dirty=true,tracking,cjk;
    private WmCalendarHeader.OnCommandListener listener;
    private int minimumYear=1900,minimumMonth=1,maximumYear=2999,maximumMonth=12;
    public WmMonthCalendarHeader(Context context,Typeface bold,Locale locale){super(context);this.bold=bold;setLocale(locale);setClickable(true);setSoundEffectsEnabled(false);}
    /** Completed taps report navigation/picker IDs synchronously; null clears.
     * Default boundary arrows disable at exactly1900-01 and2999-12, separately
     * from setDate accepting1752..9999. Owner changes the displayed date. */
    public void setOnCommandListener(WmCalendarHeader.OnCommandListener listener){this.listener=listener;}
    void setRange(WmDateTimeField.Value minimum,WmDateTimeField.Value maximum){
        WmDateTimeField.Value low=WmMonthCalendar.bound(minimum),high=WmMonthCalendar.bound(maximum);
        if(WmMonthCalendar.compareDate(low.year,low.month,low.day,high)>0)throw new IllegalArgumentException("Ordered date bounds required");
        cancelGesture();minimumYear=low.year;minimumMonth=low.month;maximumYear=high.year;maximumMonth=high.month;invalidate();
    }
    public void setDate(int year,int month,int day){if(year<1752||year>9999||month<1||month>12)throw new IllegalArgumentException("invalid month title");cancelGesture();this.year=year;this.month=month;dirty=true;invalidate();}
    public void setLocale(Locale locale){if(locale==null)throw new IllegalArgumentException("locale required");cancelGesture();this.locale=locale;dirty=true;requestLayout();invalidate();}
    public void setColors(int selection){this.selection=selection;invalidate();}
    public Rect commandBounds(int id){geometry();return new Rect(bounds(id));}
    /** Temporary EDIT+UpDown placement: native192-DPI offsets and bold0000 width.
     * UpDown32px is the observed emulator extent; system metrics remain adapted. */
    Rect yearEditorBounds(){
        geometry();int left=yearBounds.left-6,top=yearBounds.top-10;
        int width=(int)Math.ceil(WmFonts.measure(paint,"0000"))+12+28;
        return new Rect(left,top,left+width,top+cellHeight+20);
    }
    private Rect bounds(int id){switch(id){case WmCalendarHeader.PREVIOUS:return previous;case WmCalendarHeader.NEXT:return next;case WmCalendarHeader.MONTH_PICKER:return monthBounds;case WmCalendarHeader.YEAR_PICKER:return yearBounds;default:return new Rect();}}
    private void fontMetrics(){paint.setTypeface(bold);paint.setTextSize(WmMetrics.FONT);paint.setTextAlign(Paint.Align.LEFT);paint.getFontMetrics(metrics);cellHeight=(int)Math.ceil(metrics.descent-metrics.ascent)+2;}
    @Override protected void onMeasure(int width,int height){fontMetrics();setMeasuredDimension(MeasureSpec.getSize(width),resolveSize(Math.max(30,2*cellHeight-6),height));dirty=true;}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){dirty=true;cancelGesture();}
    private void geometry(){if(!dirty)return;dirty=false;fontMetrics();int inset=Math.min(10,Math.max(0,getWidth()/12)),button=Math.min(40,Math.max(0,(getWidth()-2*inset)/4)),top=Math.max(0,Math.min(getHeight()-30,cellHeight/2-2));
        previous.set(inset,top,inset+button,top+Math.min(30,getHeight()));next.set(getWidth()-inset-button,top,getWidth()-inset,previous.bottom);
        String language=locale.getLanguage(),y=String.valueOf(year),m=new DateFormatSymbols(locale).getMonths()[month-1];cjk="ko".equals(language)||"ja".equals(language)||"zh".equals(language);if(cjk)y+="ko".equals(language)?"년":"年";
        first=cjk?y:m;second=cjk?m:y;float a=WmFonts.measure(paint,first),space=WmFonts.measure(paint," "),b=WmFonts.measure(paint,second);firstX=(getWidth()-a-space-b)/2;secondX=firstX+a+space;baseline=getHeight()/2f-(metrics.ascent+metrics.descent)/2;
        Rect firstBounds=cjk?yearBounds:monthBounds,secondBounds=cjk?monthBounds:yearBounds;int left=previous.right+2,right=next.left-2,hitTop=cellHeight/2,hitBottom=Math.min(getHeight(),hitTop+cellHeight);
        firstBounds.set(Math.max(left,(int)Math.floor(firstX)),hitTop,Math.min(right,(int)Math.ceil(firstX+a)),hitBottom);secondBounds.set(Math.max(left,(int)Math.floor(secondX)),hitTop,Math.min(right,(int)Math.ceil(secondX+b)),hitBottom);
    }
    @Override protected void onDraw(Canvas canvas){geometry();paint.setColor(selection);canvas.drawRect(0,0,getWidth(),getHeight(),paint);int saved=canvas.save();canvas.clipRect(previous.right+2,0,next.left-2,getHeight());paint.setColor(Color.WHITE);WmFonts.draw(canvas,first,firstX,baseline,paint);WmFonts.draw(canvas,second,secondX,baseline,paint);canvas.restoreToCount(saved);button(canvas,previous,-1,year==minimumYear&&month==minimumMonth);button(canvas,next,1,year==maximumYear&&month==maximumMonth);}
    private void button(Canvas canvas,Rect rect,int direction,boolean disabled){paint.setColor(Color.BLACK);canvas.drawRect(rect,paint);paint.setColor(Color.WHITE);canvas.drawRect(rect.left+2,rect.top+2,rect.right-2,rect.bottom-2,paint);paint.setColor(disabled?Color.GRAY:Color.BLACK);int x=rect.centerX(),y=rect.centerY();arrow.reset();arrow.moveTo(x+direction*6,y);arrow.lineTo(x-direction*4,y-9);arrow.lineTo(x-direction*4,y+9);arrow.close();canvas.drawPath(arrow,paint);}
    private int hit(float x,float y){geometry();if(x<0||x>=getWidth()||y<0||y>=getHeight())return -1;if(previous.contains((int)x,(int)y))return year==minimumYear&&month==minimumMonth?-1:WmCalendarHeader.PREVIOUS;if(next.contains((int)x,(int)y))return year==maximumYear&&month==maximumMonth?-1:WmCalendarHeader.NEXT;if(monthBounds.contains((int)x,(int)y))return WmCalendarHeader.MONTH_PICKER;if(yearBounds.contains((int)x,(int)y))return WmCalendarHeader.YEAR_PICKER;return -1;}
    @Override public boolean onTouchEvent(MotionEvent e){int action=e.getActionMasked();if(action==MotionEvent.ACTION_DOWN){down=hit(e.getX(),e.getY());tracking=down>=0;if(tracking&&getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);return tracking;}if(!tracking)return false;if(action==MotionEvent.ACTION_CANCEL||e.getPointerCount()>1||action==MotionEvent.ACTION_POINTER_DOWN){cancelGesture();return true;}if(action==MotionEvent.ACTION_MOVE&&hit(e.getX(),e.getY())!=down)down=-1;if(action==MotionEvent.ACTION_UP){int command=down;cancelGesture();if(command>=0&&hit(e.getX(),e.getY())==command){performClick();if(listener!=null)listener.command(command);}}return true;}
    @Override public boolean performClick(){super.performClick();return true;}
    private void cancelGesture(){tracking=false;down=-1;}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){cancelGesture();this.bold=bold;this.selection=selection;dirty=true;requestLayout();invalidate();}
    @Override protected void onDetachedFromWindow(){cancelGesture();super.onDetachedFromWindow();}
}
