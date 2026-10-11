package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

/** Internal Calendar property-page coordinates, projected from the native7/14 DLU profile. */
final class WmCalendarOptionsLayout extends ViewGroup {
    private static final class Field {
        final View view;final int group,x,y,width,height;final boolean stretch,combo;
        int left,top,measuredWidth,measuredHeight;
        Field(View view,int group,int x,int y,int width,int height,boolean stretch,boolean combo){this.view=view;this.group=group;this.x=x;this.y=y;this.width=width;this.height=height;this.stretch=stretch;this.combo=combo;}
    }
    private final ArrayList<Field> fields=new ArrayList<Field>();
    private final Paint rule=new Paint();
    private int headingExtra;
    private Typeface regular,bold;
    private Locale locale;
    WmCalendarOptionsLayout(Context context){super(context);setWillNotDraw(false);rule.setColor(WmControls.FRAME);}
    void field(View view,int group,int x,int y,int width,int height,boolean stretch,boolean combo){
        fields.add(new Field(view,group,x,y,width,height,stretch,combo));addView(view,new LayoutParams(width,height));
    }
    void fonts(Typeface regular,Typeface bold){this.regular=regular;this.bold=bold;applyFonts();}
    void fontLocale(Locale locale){this.locale=locale;applyFonts();}
    private void applyFonts(){
        if(locale==null||regular==null)return;
        String language=locale.getLanguage();boolean east=language.equals("ko")||language.equals("ja")||language.equals("zh");
        for(Field field:fields){
            if(!(field.view instanceof TextView))continue;
            TextView text=(TextView)field.view;Typeface face=field.group==0&&!east?bold:regular;
            text.setTypeface(face);WmFonts.attach(text,face);
            text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,east||field.combo?24:21);
            if(!(text instanceof android.widget.CompoundButton)&&!field.combo)text.setGravity(Gravity.LEFT|Gravity.TOP);
        }
    }
    void generalColumn(boolean korean){
        // Coordinates are language-specific, independently of the selected font.
        generalPage=true;koreanColumn=korean;requestLayout();
    }
    private boolean koreanColumn,generalPage;
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getMode(widthSpec)==MeasureSpec.UNSPECIFIED?480:MeasureSpec.getSize(widthSpec);
        int[] extra=new int[6];float ratio=Math.min(1,width/480f);
        for(Field field:fields){
            int sourceX=field.combo&&generalPage?(koreanColumn?212:210):field.x;
            int sourceWidth=field.combo&&generalPage?(koreanColumn?248:252):field.width;
            field.left=Math.round(sourceX*ratio);
            int margin=Math.max(0,480-sourceX-sourceWidth);
            field.measuredWidth=Math.max(1,field.stretch?width-field.left-Math.round(margin*ratio):Math.min(width-field.left,Math.round(sourceWidth*ratio)));
            int exactWidth=MeasureSpec.makeMeasureSpec(field.measuredWidth,MeasureSpec.EXACTLY);
            if(field.combo){
                TextView text=(TextView)field.view;Paint.FontMetricsInt metrics=text.getPaint().getFontMetricsInt();
                // Keep the native34px caption unless an imported font exceeds its32px line envelope.
                int line=metrics.descent-metrics.ascent;
                field.measuredHeight=line>32?Math.max(field.height,line+text.getPaddingTop()+text.getPaddingBottom()):field.height;
            }else{
                field.view.measure(exactWidth,MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));
                field.measuredHeight=Math.max(field.height,field.view.getMeasuredHeight());
            }
            field.view.measure(exactWidth,MeasureSpec.makeMeasureSpec(field.measuredHeight,MeasureSpec.EXACTLY));
            extra[field.group]=Math.max(extra[field.group],field.measuredHeight-field.height);
        }
        int height=0;
        for(Field field:fields){int offset=0;for(int group=0;group<field.group;group++)offset+=extra[group];field.top=field.y+offset;height=Math.max(height,field.top+field.measuredHeight);}
        headingExtra=extra[0];setMeasuredDimension(resolveSize(width,widthSpec),resolveSize(height+8,heightSpec));
    }
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){for(Field field:fields)field.view.layout(field.left,field.top,field.left+field.measuredWidth,field.top+field.measuredHeight);}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.drawRect(0,52+headingExtra,getWidth(),54+headingExtra,rule);}
}
