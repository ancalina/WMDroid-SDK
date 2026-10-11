package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

/** caldll YEARVIEW: 02916AA4 layout, 02915EEC atlas, 0291640C today,
 * 02915764 hit testing. Coordinates are native 192-DPI logical pixels.
 * Original atlas glyphs are retained; Android title/bubble font metrics and
 * SHGetMetric(12)'s unmeasured runtime border use explicit SDK adaptations. */
public final class WmYearView extends View implements WmControls.RestyleListener {
    public interface OnDayClickListener { void choose(int year,int month,int day); }
    public interface OnRangeChangedListener { void changed(int year,int month,int day); }
    private static final int A=160, MIN_YEAR=1900, MAX_YEAR=2999;
    private final int borderMetric=WmMetrics.BORDER;
    private final Bitmap[] atlas=new Bitmap[2];
    private String artworkRevision;
    private final Paint text=new Paint(Paint.ANTI_ALIAS_FLAG), bitmapPaint=new Paint(), ink=new Paint();
    private final Paint inverted=new Paint();
    private Typeface font,bold;
    private Locale locale;
    private int firstDayOfWeek;
    private int year,month,day,offset,mode,columns=1,visible=1,titleHeight,top,body=92,cellWidth=22,cellHeight=18;
    private int face=WmPalette.face(0xff149ca7);
    private boolean dirty=true,resetPage=true,tracking,yearStartPending;
    private int[] candidate;
    private float bubbleX,bubbleY;
    private OnDayClickListener dayListener;
    private OnRangeChangedListener rangeListener;

    public WmYearView(Context context,Typeface font,Locale locale){
        super(context);
        if(locale==null)throw new IllegalArgumentException("locale required");
        this.font=font;this.bold=Typeface.create(font,Typeface.BOLD);this.locale=locale;
        refreshArtwork();
        bitmapPaint.setFilterBitmap(false);inverted.setFilterBitmap(false);
        inverted.setColorFilter(new ColorMatrixColorFilter(new float[]{-1,0,0,0,255,0,-1,0,0,255,0,0,-1,0,255,0,0,0,1,0}));
        Calendar now=Calendar.getInstance();setDate(now.get(Calendar.YEAR),now.get(Calendar.MONTH)+1,now.get(Calendar.DAY_OF_MONTH));
        setFocusable(true);setClickable(true);setSoundEffectsEnabled(false);
    }
    private void refreshArtwork(){
        if(artworkRevision==null)try{WmResources.prepare(getContext());}catch(IOException unavailable){}
        String revision=WmResources.revision(getContext());if(revision.equals(artworkRevision))return;
        for(int i=0;i<2;i++){
            String path="calendar/year-grid-"+(i==0?"compact":"tall")+".png";
            try{atlas[i]=WmResources.bitmap(getContext(),path,2240,i==0?92:132);}
            catch(IOException absent){atlas[i]=fallbackAtlas(i);}
        }
        artworkRevision=revision;
    }
    /** Own system-font fallback; original packs retain their native glyph bitmaps. */
    private Bitmap fallbackAtlas(int mode){
        int height=mode==0?92:132,cw=(A-2)/7,ch=(height+2)/5;
        Bitmap bitmap=Bitmap.createBitmap(2240,height,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);bitmap.setDensity(Bitmap.DENSITY_NONE);
        Canvas canvas=new Canvas(bitmap);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setTypeface(Typeface.DEFAULT);paint.setTextAlign(Paint.Align.CENTER);
        for(int frame=0;frame<14;frame++)for(int number=1;number<=31;number++){
            int index=number+frame%7-1,row=index/7,col=index%7;boolean overflow=row==5,split=row==4&&number+7<=31;
            int weekday=(col+(frame<7?0:1))%7;paint.setColor(weekday==0?Color.RED:weekday==6?Color.BLUE:Color.BLACK);
            paint.setTextSize(overflow||split?ch*0.48f:ch*0.72f);Paint.FontMetrics fm=paint.getFontMetrics();
            float x=frame*A+2+col*cw+cw/2f,y=1+Math.min(4,row)*ch+ch/2f;
            if(overflow)y+=ch*0.23f;else if(split)y-=ch*0.23f;
            WmFonts.draw(canvas,String.valueOf(number),x,y-(fm.ascent+fm.descent)/2,paint);
        }return bitmap;
    }
    public void setOnDayClickListener(OnDayClickListener listener){dayListener=listener;}
    public void setOnRangeChangedListener(OnRangeChangedListener listener){rangeListener=listener;}
    public void setDate(int year,int month,int day){
        if(year<MIN_YEAR||year>MAX_YEAR||month<1||month>12||day<1||day>days(year,month))throw new IllegalArgumentException("Year date outside 1900..2999 or invalid day");
        boolean changed=this.year!=year||this.month!=month||this.day!=day;
        this.year=year;this.month=month;this.day=day;cancelGesture();yearStartPending=false;resetPage=true;
        boolean rangeChanged=geometry();if(changed&&!rangeChanged&&rangeListener!=null)rangeListener.changed(year,month,day);invalidate();
    }
    /** Stored date copy, distinct from displayed page start and touch candidate.
     * Touch notification does not update this state; owner calls setDate. */
    public int[] date(){return new int[]{year,month,day};}
    public void setLocale(Locale locale){if(locale==null)throw new IllegalArgumentException("locale required");this.locale=locale;cancelGesture();invalidate();}
    public void setFirstDayOfWeek(int day){if(day!=Calendar.SUNDAY&&day!=Calendar.MONDAY)throw new IllegalArgumentException("first weekday must be Sunday or Monday");cancelGesture();firstDayOfWeek=day;invalidate();}
    public int getFirstDayOfWeek(){return firstDayOfWeek==0?Calendar.getInstance(locale).getFirstDayOfWeek():firstDayOfWeek;}
    public void setColors(int selection){face=WmPalette.face(selection);cancelGesture();invalidate();}
    public int getMonthOffset(){geometry();return offset;}
    public int getVisibleMonthCount(){geometry();return visible;}
    public int getColumnCount(){geometry();return columns;}
    public int getAtlasMode(){geometry();return mode;}
    public int getTitleHeight(){geometry();return titleHeight;}
    public Rect monthTitleBounds(int index){if(index<0||index>=12)throw new IllegalArgumentException("month index outside 0..11");geometry();return title(index);}
    private Rect title(int index){int x=index%columns*A,y=top+index/columns*(body+titleHeight);return new Rect(x,y,x+A-borderMetric,y+titleHeight);}
    private static Calendar civil(int year,int month,int day){Calendar c=new GregorianCalendar();c.clear();c.set(year,month-1,day,12,0,0);return c;}
    private static int days(int year,int month){return civil(year,month,1).getActualMaximum(Calendar.DAY_OF_MONTH);}
    private Calendar displayed(int index){Calendar c=civil(year,1,1);c.add(Calendar.MONTH,offset+index);return c;}
    private int firstWeekday(){return getFirstDayOfWeek();}
    private int displacement(Calendar c){return (c.get(Calendar.DAY_OF_WEEK)-firstWeekday()+7)%7;}
    private boolean geometry(){
        int previousOffset=offset,previousVisible=visible;
        if(dirty){
            text.setTypeface(font);text.setTextSize(WmMetrics.FONT);Paint.FontMetrics fm=text.getFontMetrics();
            // Native role 56 tmHeight+tmExternalLeading+2; Android font metrics adaptation.
            titleHeight=(int)Math.ceil(fm.descent-fm.ascent+fm.leading)+2;
            int w=getWidth(),h=getHeight();if(w<=0||h<=0)return false;
            int best=0,remainder=h,chosen=-1;
            for(int i=0;i<2;i++){int b=i==0?92:132;if(w%A!=0)continue;int count=(w/A)*(h/(titleHeight+b)),r=h%(titleHeight+b);
                if(count<=12&&(count>best||(count==best&&r<remainder))){chosen=i;best=count;remainder=r;}}
            mode=chosen<0?0:chosen;body=mode==0?92:132;top=chosen<0?h%(titleHeight+body):remainder;
            columns=Math.max(1,w/A);visible=Math.min(12,Math.max(1,columns*((h-top)/(body+titleHeight))));
            cellWidth=(A-2)/7;cellHeight=(body+2)/5;dirty=false;
        }
        if(resetPage&&getWidth()>0&&getHeight()>0){int page=(month-1)/visible;offset=yearStartPending?0:page>=11/visible?12-visible:page*visible;yearStartPending=false;resetPage=false;}
        boolean changed=previousOffset!=offset||previousVisible!=visible;
        if(changed&&rangeListener!=null)rangeListener.changed(year,month,day);
        return changed;
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);cancelGesture();dirty=true;resetPage=true;geometry();}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        refreshArtwork();
        boolean fontChanged=font==null?regular!=null:!font.equals(regular);
        this.font=regular;this.bold=bold;face=WmPalette.face(selection);cancelGesture();
        // A live font change may alter the month count, but must retain the first displayed month.
        if(fontChanged){dirty=true;geometry();requestLayout();}invalidate();
    }
    /** Native 0291732C command 1: year selection begins at January, not the selected month's page. */
    public void showYearStart(){
        cancelGesture();offset=0;yearStartPending=getWidth()<=0||getHeight()<=0;resetPage=yearStartPending;invalidate();
        if(rangeListener!=null)rangeListener.changed(year,month,day);
    }
    public void movePage(int direction){geometry();move(direction==0?0:(direction<0?-visible:visible));}
    private void move(int delta){
        if(delta==0)return;int next=offset+delta,nextYear=year;
        if(next>7){next-=12;nextYear++;}else if(next<-5){next+=12;nextYear--;}
        if(nextYear<MIN_YEAR||nextYear>MAX_YEAR)return;
        year=nextYear;day=Math.min(day,days(year,month));offset=next;resetPage=false;cancelGesture();invalidate();
        if(rangeListener!=null)rangeListener.changed(year,month,day);
    }
    @Override public boolean onKeyDown(int keyCode,KeyEvent event){geometry();if(keyCode==KeyEvent.KEYCODE_DPAD_UP||keyCode==KeyEvent.KEYCODE_DPAD_DOWN){move(keyCode==KeyEvent.KEYCODE_DPAD_UP?-columns:columns);return true;}return super.onKeyDown(keyCode,event);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);geometry();canvas.drawColor(Color.WHITE);
        text.setTypeface(font);text.setTextSize(WmMetrics.FONT);text.setColor(Color.BLACK);
        String[] names=new DateFormatSymbols(locale).getMonths();Calendar today=Calendar.getInstance();
        for(int i=0;i<12;i++){
            Rect r=title(i);if(r.top>=getHeight())break;Calendar c=displayed(i);int d=displacement(c),n=c.getActualMaximum(Calendar.DAY_OF_MONTH),frame=d+(firstWeekday()==Calendar.SUNDAY?0:7);
            ink.setColor(face);canvas.drawRect(r,ink);
            String name=names[c.get(Calendar.MONTH)];Paint.FontMetrics fm=text.getFontMetrics();
            WmFonts.draw(canvas,name,r.left+(r.width()-WmFonts.measure(text,name))/2,r.top+(titleHeight-fm.descent-fm.ascent)/2,text);
            int left=r.left-borderMetric,y=r.bottom;
            canvas.drawBitmap(atlas[mode],new Rect(frame*A,0,(frame+1)*A,body),new Rect(left,y,left+A,y+body),bitmapPaint);
            masks(canvas,left,y,d,n);
            if(c.get(Calendar.YEAR)==today.get(Calendar.YEAR)&&c.get(Calendar.MONTH)==today.get(Calendar.MONTH))drawToday(canvas,r,frame,d,n,today.get(Calendar.DAY_OF_MONTH));
        }
        ink.setColor(Color.BLACK);for(int col=1;col<columns;col++)canvas.drawRect(col*A-1,0,col*A-1+borderMetric,getHeight(),ink);
        if(candidate!=null)drawBubble(canvas);
    }
    private void erase(Canvas canvas,int x,int y){ink.setColor(Color.WHITE);canvas.drawRect(x,y,x+cellWidth-borderMetric,y+cellHeight-borderMetric,ink);}
    private void split(Canvas canvas,int x,int y,boolean first){
        erase(canvas,x,y);ink.setColor(first&&firstWeekday()==Calendar.SUNDAY?Color.RED:Color.BLACK);
        // SHPolyline/SHBorderPolyline use native metric pen; Android line endpoint rasterization is an adaptation.
        ink.setStrokeWidth(borderMetric);canvas.drawLine(x+borderMetric,y+cellHeight-borderMetric,x+borderMetric+cellWidth-6,y+cellHeight-borderMetric-cellWidth+6,ink);
    }
    private void masks(Canvas canvas,int left,int y,int d,int n){
        int last=y+body-cellHeight,x=left+2*borderMetric;
        if(d<=4){if(n<31)erase(canvas,x+(d+2)*cellWidth,last);if(n<30)erase(canvas,x+(d+1)*cellWidth,last);if(n<29)erase(canvas,x+d*cellWidth,last);}
        else if(d==5){if(n==31)split(canvas,x,last,true);else if(n<=29){erase(canvas,x+6*cellWidth,last);if(n==28)erase(canvas,x+5*cellWidth,last);}}
        else{if(n>=30){split(canvas,x,last,true);if(n==31)split(canvas,x+cellWidth,last,false);}else if(n==28)erase(canvas,x+6*cellWidth,last);}
    }
    private void drawToday(Canvas canvas,Rect title,int frame,int d,int n,int day){
        int index=day+d-1,row=index/7,col=index%7;boolean overflow=row==5;if(overflow)row=4;
        boolean early=row==4&&(day==23||day==24)&&(n==31||(n==30&&day==23));
        if(!overflow&&!early){int sx=cellWidth*col+1,sy=cellHeight*row+1,x=title.left+cellWidth*col,y=title.bottom+sy,w=cellWidth+1,h=cellHeight+1;
            if(col==0){sx++;x++;w--;}if(row==4)h--;
            canvas.drawBitmap(atlas[mode],new Rect(frame*A+sx,sy,frame*A+sx+w,sy+h),new Rect(x,y,x+w,y+h),inverted);
        }else{int x=title.left-1+cellWidth*col+6,z=title.bottom+body-1,q=cellWidth-6;Path p=new Path();p.moveTo(x,z);p.lineTo(x+q,z-q);p.lineTo(overflow?x+q:x,overflow?z:z-q);p.close();ink.setColor(Color.BLACK);canvas.drawPath(p,ink);}
    }
    /** Returns a fresh [year, one-based month, day], or null for native dead space. */
    public int[] dateAt(float x,float y){
        geometry();if(x<0||y<0||x>=getWidth()||y>=getHeight())return null;
        Rect first=title(0);int hitLeft=first.left-2; // Native literal, independent of SHGetMetric(12).
        int col=((int)x-hitLeft)/A,relativeY=(int)y-first.top;
        if(relativeY<0)return null;int row=relativeY/(body+titleHeight),rem=relativeY%(body+titleHeight);
        if(col<0||col>=columns||row<0||row>=12/columns||rem<=titleHeight)return null;
        Calendar c=displayed(col+columns*row);int yy=c.get(Calendar.YEAR),mm=c.get(Calendar.MONTH)+1;
        if(yy<MIN_YEAR||yy>MAX_YEAR)return null;
        int gridTop=first.bottom+(body+titleHeight)*row,gridLeft=hitLeft+A*col;
        int rr=5*((int)y-gridTop-1)/body,cc=7*((int)x-gridLeft-1)/A,d=displacement(c),n=c.getActualMaximum(Calendar.DAY_OF_MONTH),value=7*rr+cc-d+1;
        if(value<1||value>n)return null;
        boolean split=n==30&&value==23&&d==6||n==31&&(value==23&&d==6||value==24&&d>=5);
        if(split&&(((int)y-gridTop)%cellHeight)*cellWidth<(((int)x-gridLeft)%cellWidth)*cellHeight)value+=7;
        return value<=n?new int[]{yy,mm,value}:null;
    }
    private void updateCandidate(float x,float y){int[] next=dateAt(x,y);if(same(candidate,next))return;candidate=next;bubbleX=x;bubbleY=y;invalidate();}
    private static boolean same(int[] a,int[] b){return a==b||(a!=null&&b!=null&&a[0]==b[0]&&a[1]==b[1]&&a[2]==b[2]);}
    private void drawBubble(Canvas canvas){
        // Native bold 12pt at 192 DPI; Android glyph/extent metrics are an adaptation.
        text.setTypeface(bold);text.setTextSize(32);String number=String.valueOf(candidate[2]);Paint.FontMetrics fm=text.getFontMetrics();
        int tw=(int)Math.ceil(WmFonts.measure(text,number)),th=(int)Math.ceil(fm.descent-fm.ascent),w=tw+4,h=th+4;
        int x=(int)bubbleX-2*tw/3-2,y=(int)bubbleY-th-4;x=Math.max(0,Math.min(x,getWidth()-w));y=Math.max(0,Math.min(y,getHeight()-h));
        ink.setColor(Color.BLACK);canvas.drawRect(x,y,x+w,y+h,ink);ink.setColor(Color.WHITE);canvas.drawRect(x+borderMetric,y+borderMetric,x+w-borderMetric,y+h-borderMetric,ink);
        int dow=civil(candidate[0],candidate[1],candidate[2]).get(Calendar.DAY_OF_WEEK);text.setColor(dow==Calendar.SUNDAY?Color.RED:dow==Calendar.SATURDAY?Color.BLUE:Color.BLACK);
        WmFonts.draw(canvas,number,x+(w-WmFonts.measure(text,number))/2,y+(h-fm.descent-fm.ascent)/2,text);
    }
    private void cancelGesture(){tracking=false;candidate=null;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);invalidate();}
    /** DOWN/MOVE replaces candidate; dead space clears it. UP reports the retained
     * candidate synchronously without recomputing its coordinates or stored date.
     * Cancellation/multi-touch clears it without notification. */
    @Override public boolean onTouchEvent(MotionEvent event){
        int action=event.getActionMasked();if(action==MotionEvent.ACTION_DOWN){cancelGesture();tracking=true;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);updateCandidate(event.getX(),event.getY());return true;}
        if(!tracking)return false;
        if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN||event.getPointerCount()>1){cancelGesture();return true;}
        if(action==MotionEvent.ACTION_MOVE){updateCandidate(event.getX(),event.getY());return true;}
        if(action==MotionEvent.ACTION_UP){int[] chosen=candidate;cancelGesture();if(chosen!=null){performClick();if(dayListener!=null)dayListener.choose(chosen[0],chosen[1],chosen[2]);}return true;}
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override protected void onDetachedFromWindow(){cancelGesture();super.onDetachedFromWindow();}
}
