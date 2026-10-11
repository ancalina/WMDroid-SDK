package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.ArrayList;

/** Literal Name/Address components; the owner supplies labels and assembles results. */
public final class WmContactSubcardView extends FrameLayout implements WmControls.RestyleListener {
    public static final int NAME=0,ADDRESS=1;
    public interface OnChangedListener {void changed(int index,String value);}
    private final int kind;
    private final TextView[] labels;
    private final EditText[] editors;
    private final ArrayList<FieldRow> rowViews=new ArrayList<>();
    private final Paint ink=new Paint();
    private String[] values;
    private Typeface regular;
    private int selection;
    private boolean silent;
    private OnChangedListener changed;

    public WmContactSubcardView(Context context,Typeface regular,int kind,String[] labels,String[] values,int selection){
        super(context);if(regular==null||kind!=NAME&&kind!=ADDRESS)throw new IllegalArgumentException("Subcard font and kind required");
        int count=kind==NAME?6:5;String[] captions=checked(labels,count),copy=checked(values,count);
        this.kind=kind;this.regular=regular;this.selection=selection;this.values=copy;this.labels=new TextView[count];editors=new EditText[count];
        setBackgroundColor(Color.WHITE);setWillNotDraw(false);setFocusableInTouchMode(true);
        for(int index=0;index<count;index++){
            final int slot=index;FieldRow row=new FieldRow(context,index,captions[index],copy[index]);rowViews.add(row);addView(row);this.labels[index]=row.label;editors[index]=row.editor;
            row.editor.addTextChangedListener(new TextWatcher(){
                public void beforeTextChanged(CharSequence text,int start,int count,int after){}
                public void onTextChanged(CharSequence text,int start,int before,int count){}
                public void afterTextChanged(Editable text){String next=text.toString();if(silent||next.equals(WmContactSubcardView.this.values[slot]))return;WmContactSubcardView.this.values[slot]=next;if(changed!=null)changed.changed(slot,next);}
            });
        }
    }
    private static String[] checked(String[] values,int count){if(values==null||values.length!=count)throw new IllegalArgumentException("Subcard field count required");String[] copy=values.clone();for(String value:copy)if(value==null)throw new IllegalArgumentException("Subcard strings required");return copy;}
    public String[] values(){return values.clone();}
    public int desiredHeight(){return kind==NAME?248:328;}
    public EditText editor(int index){return editors[index];}
    /** Reports actual TextWatcher value changes synchronously, including direct editor
     * setText. setValues is silent; null clears this listener. */
    public void setOnChangedListener(OnChangedListener listener){changed=listener;}
    public void setValues(String[] values){
        String[] copy=checked(values,editors.length);boolean prior=silent;silent=true;
        try{for(int index=0;index<copy.length;index++)if(!copy[index].equals(this.values[index])){EditText editor=editors[index];int start=editor.getSelectionStart(),end=editor.getSelectionEnd();this.values[index]=copy[index];editor.setText(copy[index]);editor.setSelection(Math.max(0,Math.min(start,editor.length())),Math.max(0,Math.min(end,editor.length())));}}finally{silent=prior;}
    }
    public void setLabels(String[] values){String[] copy=checked(values,labels.length);for(int index=0;index<copy.length;index++)if(!copy[index].contentEquals(labels[index].getText())){labels[index].setText(copy[index]);editors[index].setContentDescription(copy[index]);}}
    private int rowHeight(int index){return kind==ADDRESS&&index==0?117:kind==ADDRESS&&index==4?78:39;}
    @Override protected void onMeasure(int widthSpec,int heightSpec){int width=resolveSize(350,widthSpec);for(int index=0;index<rowViews.size();index++)rowViews.get(index).measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(rowHeight(index),MeasureSpec.EXACTLY));setMeasuredDimension(width,resolveSize(desiredHeight(),heightSpec));}
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){int y=2;for(int index=0;index<rowViews.size();index++){int height=rowHeight(index);rowViews.get(index).layout(0,y,getWidth(),y+height);y+=height;}}
    @Override protected void dispatchDraw(Canvas canvas){super.dispatchDraw(canvas);ink.setColor(Color.BLACK);canvas.drawRect(0,0,getWidth(),2,ink);canvas.drawRect(0,getHeight()-2,getWidth(),getHeight(),ink);canvas.drawRect(0,0,2,getHeight(),ink);canvas.drawRect(getWidth()-2,0,getWidth(),getHeight(),ink);}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){if(regular==null)throw new IllegalArgumentException("Subcard font required");boolean prior=silent;silent=true;try{this.regular=regular;this.selection=selection;for(FieldRow row:rowViews)row.restyle();}finally{silent=prior;}}
    private void font(TextView text,int size){text.setTypeface(regular);WmFonts.attach(text,regular);text.setTextSize(TypedValue.COMPLEX_UNIT_PX,size);}
    private final class FieldRow extends FrameLayout {
        final int index;
        final TextView label;
        final EditText editor;
        final Paint line=new Paint();
        FieldRow(Context context,int index,String caption,String value){
            super(context);this.index=index;setWillNotDraw(false);
            label=WmControls.text(context,regular,caption);label.setSingleLine(!(kind==ADDRESS&&index==4));label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);label.setPadding(0,0,0,0);addView(label);
            editor=new EditText(context);WmControls.input(editor,regular);boolean multiline=kind==ADDRESS&&(index==0||index==4);editor.setInputType(InputType.TYPE_CLASS_TEXT|(multiline?InputType.TYPE_TEXT_FLAG_MULTI_LINE:0));if(!multiline)editor.setSingleLine(true);editor.setGravity((multiline?Gravity.TOP:Gravity.CENTER_VERTICAL)|Gravity.LEFT);editor.setBackgroundColor(Color.TRANSPARENT);editor.setMinWidth(0);editor.setMinimumWidth(0);editor.setMinHeight(0);editor.setMinimumHeight(0);editor.setPadding(1,0,1,0);editor.setText(value);editor.setContentDescription(caption);addView(editor);restyle();
        }
        void restyle(){font(label,24);font(editor,28);editor.setHighlightColor(WmPalette.highlight(selection));requestLayout();invalidate();}
        @Override protected void onMeasure(int widthSpec,int heightSpec){int width=MeasureSpec.getSize(widthSpec),height=rowHeight(index);label.measure(MeasureSpec.makeMeasureSpec(114,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(kind==ADDRESS&&index==4?height-2:37,MeasureSpec.EXACTLY));editor.measure(MeasureSpec.makeMeasureSpec(Math.max(0,Math.min(312,width-2)-130),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(height-2,MeasureSpec.EXACTLY));setMeasuredDimension(width,height);}
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){label.layout(8,0,122,label.getMeasuredHeight());editor.layout(130,0,130+editor.getMeasuredWidth(),getHeight()-2);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);line.setColor(0xff848284);canvas.drawRect(131,getHeight()-2,Math.max(131,Math.min(312,getWidth()-2)),getHeight(),line);}
    }
}
