package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.TextView;

/** Icon-list label selection measured by Android Layout, not native GDI metrics.
 * The owner supplies icon feedback and selection policy; this view only paints
 * the visible Layout-line rectangle, not raster glyph outlines. All coordinates are local logical pixels.
 */
public final class WmIconCaption extends TextView implements WmControls.RestyleListener {
    private final Paint selectionPaint=new Paint();
    private final Rect selectionBounds=new Rect();
    private int visibleMaxLines=Integer.MAX_VALUE;
    private int horizontalSelectionPadding,verticalSelectionPadding;

    public WmIconCaption(Context context,Typeface font,int selection){
        super(context);
        WmControls.textStyle(this,font);
        setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        setMaxLines(2);setEllipsize(TextUtils.TruncateAt.END);
        setBackgroundColor(Color.TRANSPARENT);
        setTextColor(new ColorStateList(new int[][]{
                {-android.R.attr.state_enabled},{android.R.attr.state_pressed},
                {android.R.attr.state_selected},{android.R.attr.state_activated},{}
        },new int[]{WmControls.SHADOW,Color.WHITE,Color.WHITE,Color.WHITE,Color.BLACK}));
        setSelectionColor(selection);
    }

    public void setSelectionColor(int color){if(selectionPaint.getColor()!=color){selectionPaint.setColor(color);invalidate();}}

    /** Defaults to zero; callers may supply their own measured system inset. */
    public void setSelectionPadding(int horizontal,int vertical){
        if(horizontal<0||vertical<0)throw new IllegalArgumentException("Selection padding must be nonnegative");
        horizontalSelectionPadding=horizontal;verticalSelectionPadding=vertical;invalidate();
    }

    // getMaxLines is API16+. Track the API1 setters instead.
    @Override public void setMaxLines(int maxLines){super.setMaxLines(maxLines);visibleMaxLines=maxLines;invalidate();}
    @Override public void setLines(int lines){super.setLines(lines);visibleMaxLines=lines;invalidate();}

    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        // restyleTree has already updated registered TextView typography.
        setSelectionColor(selection);
    }

    @Override protected void drawableStateChanged(){super.drawableStateChanged();invalidate();}

    private boolean selectionActive(){
        boolean enabled=false,selected=false;
        for(int state:getDrawableState()){
            if(state==android.R.attr.state_enabled)enabled=true;
            if(state==android.R.attr.state_pressed||state==android.R.attr.state_selected
                    ||state==android.R.attr.state_activated)selected=true;
        }
        return enabled&&selected;
    }

    /** Copies the rectangle actually painted, in viewport-local coordinates.
     * Returns false and clears target when there is no visible active caption.
     * Call after measure/layout; no view, text or selection state is changed.
     */
    public boolean copySelectionBounds(Rect target){
        if(target==null)throw new IllegalArgumentException("A target rectangle is required");
        target.setEmpty();
        Layout layout=getLayout();
        if(!selectionActive()||getText().length()==0||layout==null||getWidth()<=0||getHeight()<=0)return false;
        int clipLeft=getCompoundPaddingLeft(),clipRight=getWidth()-getCompoundPaddingRight();
        int clipTop=getExtendedPaddingTop(),clipBottom=getHeight()-getExtendedPaddingBottom();
        if(clipRight<=clipLeft||clipBottom<=clipTop)return false;
        int originX=clipLeft-getScrollX(),originY=getTotalPaddingTop()-getScrollY();
        float left=Float.POSITIVE_INFINITY,right=Float.NEGATIVE_INFINITY;
        int top=Integer.MAX_VALUE,bottom=Integer.MIN_VALUE;
        int lines=Math.min(layout.getLineCount(),Math.max(0,visibleMaxLines));
        for(int line=0;line<lines;line++){
            int lineTop=originY+layout.getLineTop(line),lineBottom=originY+layout.getLineBottom(line);
            float lineLeft=originX+layout.getLineLeft(line),lineRight=originX+layout.getLineRight(line);
            if(lineBottom<=clipTop||lineTop>=clipBottom||lineRight<=lineLeft
                    ||lineRight<=clipLeft||lineLeft>=clipRight)continue;
            left=Math.min(left,lineLeft);right=Math.max(right,lineRight);
            top=Math.min(top,lineTop);bottom=Math.max(bottom,lineBottom);
        }
        if(top==Integer.MAX_VALUE)return false;
        // Round outward once so the inspected rectangle and raster agree.
        target.set((int)Math.max(clipLeft,Math.floor(left-horizontalSelectionPadding)),
                (int)Math.max(clipTop,(long)top-verticalSelectionPadding),
                (int)Math.min(clipRight,Math.ceil(right+horizontalSelectionPadding)),
                (int)Math.min(clipBottom,(long)bottom+verticalSelectionPadding));
        return !target.isEmpty();
    }

    @Override protected void onDraw(Canvas canvas){
        if(copySelectionBounds(selectionBounds)){
            // View drawing is already offset by its scroll; restore viewport
            // coordinates for the bounded rectangle before TextView draws text.
            int save=canvas.save();canvas.translate(getScrollX(),getScrollY());
            canvas.drawRect(selectionBounds,selectionPaint);canvas.restoreToCount(save);
        }
        super.onDraw(canvas);
    }
}
