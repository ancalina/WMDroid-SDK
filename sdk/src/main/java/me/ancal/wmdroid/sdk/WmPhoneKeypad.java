package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/** WM phone keypad in logical VGA units. The owner handles dialing and permissions.
 * Canvas skins approximate the captured WM614 buttons; font rasterization differs
 * between Android and the original device. No inferred long-press actions.
 */
public final class WmPhoneKeypad extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener {
    public static final int WIDTH=480,HEIGHT=294;
    public interface Listener {
        void digit(char digit);
        void backspace();
        void speedDial();
        void history();
        void talk();
    }
    private static final int[] X={12,118,224,326},Y={4,78,152,226};
    private static final String[] DIGITS={"1","2","3","4","5","6","7","8","9","*","0","#"};
    private static final String[] LETTERS={"","abc","def","ghi","jkl","mno","pqrs","tuv","wxyz","","+",""};
    private final Listener listener;
    private final Key[] keys=new Key[16];
    private Typeface regular,bold;
    private String language="en";
    private final String[] captions=new String[3];
    private float keyScale=1;

    public WmPhoneKeypad(Context context,Typeface regular,Typeface bold,Listener listener){
        super(context);
        if(regular==null||bold==null||listener==null)throw new IllegalArgumentException("Phone keypad fonts and listener required");
        this.regular=regular;this.bold=bold;this.listener=listener;
        setBackgroundColor(0xff29383e);setSoundEffectsEnabled(false);
        for(int row=0;row<4;row++)for(int col=0;col<4;col++){
            int index=row*4+col;Key key=new Key(context,row,col);keys[index]=key;
            addView(key,new LayoutParams(col==3?142:86,56));
        }
        updateDescriptions();
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=resolveSize(WIDTH,widthSpec),height=resolveSize(Math.round(HEIGHT*Math.min(1,width/(float)WIDTH)),heightSpec);setMeasuredDimension(width,height);
        // Extra logical width belongs to the dark surface, not stretched native keys.
        keyScale=Math.min(1,Math.min(width/(float)WIDTH,height/(float)HEIGHT));
        for(Key key:keys)key.measure(MeasureSpec.makeMeasureSpec(Math.round(key.nativeWidth*keyScale),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.round(56*keyScale),MeasureSpec.EXACTLY));
    }
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
        int offsetX=(getWidth()-Math.round(WIDTH*keyScale))/2,offsetY=getHeight()-Math.round(HEIGHT*keyScale);
        for(Key key:keys){int x=offsetX+Math.round(X[key.col]*keyScale),y=offsetY+Math.round(Y[key.row]*keyScale);key.layout(x,y,x+key.getMeasuredWidth(),y+key.getMeasuredHeight());}
    }
    @Override public void setEnabled(boolean enabled){super.setEnabled(enabled);if(keys!=null)for(Key key:keys)if(key!=null)key.setEnabled(enabled);}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        if(regular==null||bold==null)throw new IllegalArgumentException("Phone keypad fonts required");
        this.regular=regular;this.bold=bold;for(Key key:keys)key.invalidate();
    }
    @Override public void onWmLanguageChanged(String language){this.language=WmStrings.language(getContext(),language);updateDescriptions();for(Key key:keys)key.invalidate();}
    private void updateDescriptions(){
        captions[0]=WmStrings.text(getContext(),language,"단축 다이얼");captions[1]=WmStrings.text(getContext(),language,"통화 기록");captions[2]=WmStrings.text(getContext(),language,"통화","phone.keypad.talk");
        String backspace=WmStrings.text(getContext(),language,"지우기");
        for(Key key:keys){String description;
        if(key.col<3){int n=key.row*3+key.col;description=DIGITS[n]+(LETTERS[n].isEmpty()?"":" "+LETTERS[n]);}
        else description=key.row==0?backspace:caption(key.row);
        key.setContentDescription(description);
    }}
    private String caption(int row){return captions[row-1];}
    private void clicked(Key key){
        if(key.col<3)listener.digit(DIGITS[key.row*3+key.col].charAt(0));
        else if(key.row==0)listener.backspace();else if(key.row==1)listener.speedDial();else if(key.row==2)listener.history();else listener.talk();
    }
    private final class Key extends View {
        final int row,col,nativeWidth;
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF shape=new RectF();
        final Path symbol=new Path();
        final LinearGradient normal,pressed;
        boolean armed;
        Key(Context context,int row,int col){
            super(context);this.row=row;this.col=col;nativeWidth=col==3?142:86;
            boolean talk=col==3&&row==3;
            normal=new LinearGradient(0,0,0,56,talk?new int[]{0xffc0e3cd,0xff39995d,0xff078d2a,0xff005d15,0xff15a92b}:new int[]{0xffc8cdd1,0xff939da7,0xff737f8a,0xff43515d,0xff576771},new float[]{0,.40f,.44f,.70f,1},Shader.TileMode.CLAMP);
            pressed=new LinearGradient(0,0,0,56,talk?new int[]{0xff005d15,0xff078d2a,0xff39995d}:new int[]{0xff35434d,0xff586875,0xff8d99a2},null,Shader.TileMode.CLAMP);
            setClickable(true);setFocusable(true);setSoundEffectsEnabled(false);
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);int save=canvas.save();canvas.scale(keyScale,keyScale);
            shape.set(1,1,nativeWidth-1,55);paint.setStyle(Paint.Style.FILL);paint.setShader(isPressed()?pressed:normal);canvas.drawRoundRect(shape,8,8,paint);paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);paint.setColor(isPressed()?0xff35434d:0xffdde2e4);canvas.drawRoundRect(shape,8,8,paint);paint.setStyle(Paint.Style.FILL);
            paint.setColor(isEnabled()?Color.WHITE:0xffb0b8ba);
            if(col<3){int index=row*3+col;paint.setTypeface(regular);paint.setTextSize(40);String digit=DIGITS[index];float x=nativeWidth-5-WmFonts.measure(paint,digit);WmFonts.draw(canvas,digit,x,45,paint);
                paint.setTextSize(22);String letters=LETTERS[index];WmFonts.draw(canvas,letters,x-1-WmFonts.measure(paint,letters),45,paint);
            }else if(row==0){symbol.reset();symbol.moveTo(52,19);symbol.lineTo(44,27);symbol.lineTo(52,35);symbol.lineTo(52,29);symbol.lineTo(82,29);symbol.lineTo(82,25);symbol.lineTo(52,25);symbol.close();canvas.drawPath(symbol,paint);
            }else{
                paint.setTypeface(bold);paint.setTextSize(24);String text=caption(row);float textWidth=WmFonts.measure(paint,text),offset=row==3?12:0;
                WmFonts.draw(canvas,text,(nativeWidth-textWidth)/2+offset,36,paint);
                if(row==3){symbol.reset();symbol.moveTo(14,15);symbol.lineTo(10,20);symbol.cubicTo(11,28,20,37,28,39);symbol.lineTo(35,35);symbol.lineTo(29,29);symbol.lineTo(24,32);symbol.cubicTo(20,30,17,27,16,23);symbol.lineTo(19,20);symbol.close();canvas.drawPath(symbol,paint);}
            }
            canvas.restoreToCount(save);
        }
        @Override public boolean onTouchEvent(MotionEvent event){
            if(!isEnabled())return false;
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:armed=true;setPressed(true);invalidate();return true;
                case MotionEvent.ACTION_MOVE:if(event.getPointerCount()!=1||event.getX()<0||event.getX()>=getWidth()||event.getY()<0||event.getY()>=getHeight())cancel();return true;
                case MotionEvent.ACTION_POINTER_DOWN:cancel();return true;
                case MotionEvent.ACTION_UP:boolean click=armed&&event.getX()>=0&&event.getX()<getWidth()&&event.getY()>=0&&event.getY()<getHeight();cancel();if(click)performClick();return true;
                case MotionEvent.ACTION_CANCEL:cancel();return true;
                default:return true;
            }
        }
        @Override public boolean performClick(){if(!isEnabled())return false;super.performClick();clicked(this);return true;}
        private void cancel(){armed=false;setPressed(false);invalidate();}
        @Override public void setEnabled(boolean enabled){if(!enabled)cancel();super.setEnabled(enabled);invalidate();}
        @Override protected void onDetachedFromWindow(){cancel();super.onDetachedFromWindow();}
    }
}
