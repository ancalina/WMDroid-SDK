package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import java.util.Locale;

/** SysMonthCal32's temporary Gregorian EDIT and UpDown, in logical VGA pixels. */
final class WmMonthYearEditor extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener {
    interface Listener {void changed(int year);void finished(boolean valid,int year);}
    private final EditText input;
    private final Arrow up,down;
    private final Listener listener;
    private final int min,max;
    private int year;
    private Locale locale;
    private boolean finished;

    WmMonthYearEditor(Context context,Typeface regular,Locale locale,int year,int min,int max,int selection,Listener listener){
        super(context);
        if(locale==null||listener==null||min<0||max<min||year<min||year>max)throw new IllegalArgumentException("invalid year editor arguments");
        this.locale=locale;this.year=year;this.min=min;this.max=max;this.listener=listener;
        input=new EditText(context);input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
        input.setImeOptions(EditorInfo.IME_ACTION_DONE|EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        input.setGravity(Gravity.CENTER);input.setIncludeFontPadding(false);input.setTextSize(TypedValue.COMPLEX_UNIT_PX,WmMetrics.FONT);
        input.setMinWidth(0);input.setMinimumWidth(0);input.setMinHeight(0);input.setMinimumHeight(0);
        if(Build.VERSION.SDK_INT>=21)input.setBackgroundTintList(null);
        GradientDrawable border=new GradientDrawable();border.setColor(Color.WHITE);border.setStroke(2,Color.BLACK);input.setBackgroundDrawable(border);input.setPadding(2,2,2,2);
        input.setTextColor(Color.BLACK);input.setSelectAllOnFocus(true);input.setText(String.valueOf(year));
        addView(input,new LayoutParams(0,0));up=new Arrow(context,1);down=new Arrow(context,-1);addView(up,new LayoutParams(32,0));addView(down,new LayoutParams(32,0));
        input.setOnEditorActionListener((view,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){finish();return true;}return false;});
        input.setOnKeyListener((view,key,event)->{
            if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN){if(event.getAction()==KeyEvent.ACTION_DOWN)step(key==KeyEvent.KEYCODE_DPAD_UP?1:-1);return true;}
            if(WmControls.isEnterKey(key)||key==KeyEvent.KEYCODE_ESCAPE){if(event.getAction()==KeyEvent.ACTION_DOWN)finish();return true;}return false;
        });
        input.setOnFocusChangeListener((view,focused)->{if(!focused)finish();});
        onWmRestyle(regular,null,selection,0);setLocale(locale);input.requestFocus();input.selectAll();
    }
    EditText input(){return input;}
    void setLocale(Locale locale){
        if(locale==null)throw new IllegalArgumentException("locale required");this.locale=locale;
        String language=WmStrings.language(getContext(),Build.VERSION.SDK_INT>=21?Modern.tag(locale):locale.toString().replace('_','-'));
        WmControls.bindDescription(input,language,"연도");WmControls.bindDescription(up,language,"다음 연도");WmControls.bindDescription(down,language,"이전 연도");
    }
    private static final class Modern {static String tag(Locale locale){return locale.toLanguageTag();}}
    @Override public void onWmLanguageChanged(String language){setLocale(WmStrings.locale(WmStrings.language(getContext(),language)));}
    void setYear(int year){if(year<min||year>max)throw new IllegalArgumentException("year outside range");this.year=year;input.setText(String.valueOf(year));input.selectAll();}
    private Long parsed(){
        CharSequence text=input.getText();if(text.length()==0)return null;
        long value=0;for(int i=0;i<text.length();i++){char c=text.charAt(i);if(c<'0'||c>'9')return null;value=Math.min((long)max+1,value*10+c-'0');}
        return value;
    }
    private void step(int delta){
        if(finished)return;int old=year;
        // Native WM_VSCROLL rewrites the buddy with the selected year after every step.
        setYear((int)Math.max(min,Math.min(max,(long)year+delta)));if(year!=old)listener.changed(year);
    }
    static int repeatMagnitude(long elapsed){return elapsed>=5120?20:elapsed>=2048?5:1;}
    void finish(){
        if(finished)return;finished=true;up.stop();down.stop();Long typed=parsed();boolean valid=typed!=null&&typed>=min&&typed<=max;if(valid)year=typed.intValue();
        hideKeyboard();listener.finished(valid,year);
    }
    private void hideKeyboard(){InputMethodManager ime=(InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);if(ime!=null&&input.getWindowToken()!=null)ime.hideSoftInputFromWindow(input.getWindowToken(),0);}
    @Override protected void onMeasure(int width,int height){
        int w=MeasureSpec.getSize(width),h=MeasureSpec.getSize(height);
        LayoutParams edit=(LayoutParams)input.getLayoutParams();edit.width=Math.max(0,w-28);edit.height=Math.max(0,h-16);edit.topMargin=8;
        LayoutParams upper=(LayoutParams)up.getLayoutParams();upper.width=32;upper.height=h/2;upper.leftMargin=Math.max(0,w-32);
        LayoutParams lower=(LayoutParams)down.getLayoutParams();lower.width=32;lower.height=h-h/2;lower.leftMargin=Math.max(0,w-32);lower.topMargin=h/2;
        super.onMeasure(width,height);
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);up.stop();down.stop();}
    @Override protected void onDetachedFromWindow(){up.stop();down.stop();hideKeyboard();super.onDetachedFromWindow();}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        int start=input.getSelectionStart(),end=input.getSelectionEnd();input.setTypeface(regular);WmFonts.attach(input,regular);input.setHighlightColor(selection);
        if(start>=0&&end>=0)input.setSelection(Math.min(start,input.length()),Math.min(end,input.length()));
    }
    private final class Arrow extends View {
        private final int delta;private boolean held;private long heldAt;private final Paint paint=new Paint();private final Path triangle=new Path();
        private final Runnable repeat=new Runnable(){public void run(){if(!held||finished)return;step(delta*repeatMagnitude(SystemClock.uptimeMillis()-heldAt));if(held&&!finished)postDelayed(this,100);}};
        Arrow(Context context,int delta){super(context);this.delta=delta;setClickable(true);setFocusable(false);setSoundEffectsEnabled(false);}
        void stop(){held=false;removeCallbacks(repeat);}
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);stop();float x=w/2f,y=h/2f;triangle.reset();triangle.moveTo(x-5,y+delta*3);triangle.lineTo(x+5,y+delta*3);triangle.lineTo(x,y-delta*3);triangle.close();}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.drawColor(Color.WHITE);paint.setColor(Color.BLACK);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);canvas.drawRect(1,delta>0?1:0,getWidth()-1,delta>0?getHeight():getHeight()-1,paint);paint.setStyle(Paint.Style.FILL);canvas.drawPath(triangle,paint);}
        @Override public boolean onTouchEvent(MotionEvent event){
            if(!isEnabled()||finished)return false;
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){stop();held=true;heldAt=SystemClock.uptimeMillis();step(delta);if(held&&!finished)postDelayed(repeat,530);return true;}
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_POINTER_UP||event.getPointerCount()!=1||event.getX()<0||event.getY()<0||event.getX()>=getWidth()||event.getY()>=getHeight())stop();
            return true;
        }
        @Override public boolean performClick(){super.performClick();step(delta);return true;}
        @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    }
}
