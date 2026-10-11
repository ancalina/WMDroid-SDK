package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native BOXSELECT family: short horizontal exclusive/multiple text cells.
 * Black selected fill and white text follow commctrl paint; 48px cells and
 * border geometry are provisional logical VGA extents, not measured DLU.
 * Exclusive selection can be cleared by selecting the selected cell (style10).
 * Labels and bit ordering are caller-owned; setters never notify. */
public final class WmBoxSelect extends LinearLayout implements WmControls.RestyleListener {
    public interface OnSelectionListener {void selected(int mask);}
    private String[] labels;
    private int mask;
    private final boolean multi;
    private Typeface font;
    private boolean proportional;
    private OnSelectionListener listener;
    public WmBoxSelect(Context context,Typeface font,String[] labels,int mask,boolean multi,int accent){
        super(context);if(font==null)throw new IllegalArgumentException("Font required");this.font=font;this.multi=multi;setOrientation(HORIZONTAL);setChoices(labels,mask);
    }
    public String[] choices(){return labels.clone();}
    /** Native pattern cells use measured text proportions; weekday cells equal widths. */
    public void setProportionalWidths(boolean enabled){proportional=enabled;updateWidths();}
    private void updateWidths(){for(int i=0;i<getChildCount();i++){TextView cell=cell(i);LayoutParams lp=(LayoutParams)cell.getLayoutParams();lp.width=proportional?(int)Math.ceil(WmFonts.measure(cell.getPaint(),labels[i])):0;lp.weight=1;cell.setLayoutParams(lp);}}
    public int selectedMask(){return mask;}
    public TextView cell(int index){if(index<0||index>=labels.length)throw new IndexOutOfBoundsException("Cell index");return (TextView)getChildAt(index);}
    public void setChoices(String[] values,int selected){
        if(values==null||values.length==0||values.length>30)throw new IllegalArgumentException("1..30 labels required");
        String[] copy=values.clone();for(String value:copy)if(value==null)throw new IllegalArgumentException("Label required");check(selected,copy.length);labels=copy;mask=selected;removeAllViews();
        for(int i=0;i<labels.length;i++){final int index=i;TextView cell=WmControls.text(getContext(),font,labels[i]);cell.setGravity(Gravity.CENTER);cell.setSingleLine(true);cell.setPadding(0,0,0,0);cell.setSoundEffectsEnabled(false);cell.setFocusable(true);cell.setBackgroundDrawable(new CellFrame(this,index));cell.setOnClickListener(v->choose(index));addView(cell,new LayoutParams(0,48,1));}updateWidths();refresh();
    }
    public void setSelectedMask(int selected){check(selected,labels.length);mask=selected;refresh();}
    /** Validate index first; toggle only when this container is enabled.
     * Does not check the individual cell enabled state; notify synchronously after a change. */
    public void choose(int index){cell(index);if(!isEnabled())return;int bit=1<<index;int next=multi?mask^bit:(mask==bit?0:bit);if(next==mask)return;setSelectedMask(next);if(listener!=null)listener.selected(mask);}
    public void setOnSelectionListener(OnSelectionListener listener){this.listener=listener;}
    private void check(int value,int length){if(value<0||(value&~((1<<length)-1))!=0||(!multi&&value!=0&&(value&(value-1))!=0))throw new IllegalArgumentException("Invalid BOXSELECT mask");}
    private void refresh(){for(int i=0;i<getChildCount();i++){TextView cell=cell(i);boolean selected=(mask&(1<<i))!=0;cell.setSelected(selected);cell.setEnabled(isEnabled());cell.setTextColor(!isEnabled()?WmControls.SHADOW:selected?Color.WHITE:Color.BLACK);cell.invalidate();}}
    @Override public void setEnabled(boolean enabled){super.setEnabled(enabled);if(labels!=null)refresh();}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){font=regular;for(int i=0;i<getChildCount();i++){cell(i).setTypeface(regular);WmFonts.attach(cell(i),regular);}updateWidths();refresh();}
    private static final class CellFrame extends Drawable {
        private final WmBoxSelect owner;private final int index;private final Paint paint=new Paint();private int alpha=255;
        CellFrame(WmBoxSelect owner,int index){this.owner=owner;this.index=index;}
        @Override public void draw(Canvas canvas){paint.setStyle(Paint.Style.FILL);paint.setColor((owner.mask&(1<<index))!=0&&owner.isEnabled()?Color.BLACK:Color.WHITE);paint.setAlpha(alpha);canvas.drawRect(getBounds(),paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);paint.setColor(WmControls.SHADOW);paint.setAlpha(alpha);canvas.drawRect(getBounds().left+.5f,getBounds().top+.5f,getBounds().right-.5f,getBounds().bottom-.5f,paint);}
        @Override public void setAlpha(int value){alpha=Math.max(0,Math.min(255,value));invalidateSelf();}
        @Override public void setColorFilter(ColorFilter value){paint.setColorFilter(value);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
