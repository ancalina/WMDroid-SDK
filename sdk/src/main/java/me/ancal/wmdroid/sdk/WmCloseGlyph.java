package me.ancal.wmdroid.sdk;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Hand-authored outlines after visual inspection of shellres.192.dll 4610/4615.
 * Both resources have a 46x52 canvas. Their opaque bounds are OK (10,16)-(36,34)
 * and X (14,16)-(32,34); keep that padding when sizing the title-bar icon.
 * The O's smooth ellipses replace 1bpp steps; exact GDI pixels are not claimed.
 * Geometry is also recorded in design/icons/close-glyphs.svg. No font substitution.
 */
public final class WmCloseGlyph extends Drawable {
    private static final int WIDTH=46, HEIGHT=52;
    private final Path glyph=new Path();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int normalColor, pressedColor;
    private boolean active;
    private int alpha=255;

    public WmCloseGlyph(boolean ok,int normalColor,int pressedColor){
        this.normalColor=normalColor;this.pressedColor=pressedColor;
        paint.setStyle(Paint.Style.FILL);
        glyph.setFillType(Path.FillType.EVEN_ODD);
        if(ok){
            glyph.addOval(new android.graphics.RectF(10,22,22,34),Path.Direction.CW);
            glyph.addOval(new android.graphics.RectF(14,25,18,31),Path.Direction.CW);
            glyph.moveTo(24,16);glyph.lineTo(28,16);glyph.lineTo(28,26);
            glyph.lineTo(31,22);glyph.lineTo(36,22);glyph.lineTo(31,27);
            glyph.lineTo(36,34);glyph.lineTo(31,34);glyph.lineTo(28,29);
            glyph.lineTo(28,34);glyph.lineTo(24,34);glyph.close();
        }else{
            glyph.moveTo(14,16);glyph.lineTo(18,16);glyph.lineTo(23,21);
            glyph.lineTo(28,16);glyph.lineTo(32,16);glyph.lineTo(32,20);
            glyph.lineTo(27,25);glyph.lineTo(32,30);glyph.lineTo(32,34);
            glyph.lineTo(28,34);glyph.lineTo(23,29);glyph.lineTo(18,34);
            glyph.lineTo(14,34);glyph.lineTo(14,30);glyph.lineTo(19,25);
            glyph.lineTo(14,20);glyph.close();
        }
    }

    @Override public boolean isStateful(){return true;}
    /** True only when the combined pressed-or-selected active value changes. */
    @Override protected boolean onStateChange(int[] states){
        boolean next=false;
        if(states!=null)for(int state:states)if(state==android.R.attr.state_pressed||state==android.R.attr.state_selected){next=true;break;}
        if(active==next)return false;active=next;invalidateSelf();return true;
    }
    @Override public void draw(Canvas canvas){
        Rect bounds=getBounds();if(bounds.isEmpty())return;
        int color=active?pressedColor:normalColor;
        paint.setColor(color);paint.setAlpha(alpha*(color>>>24)/255);
        int saved=canvas.save();canvas.translate(bounds.left,bounds.top);
        canvas.scale(bounds.width()/(float)WIDTH,bounds.height()/(float)HEIGHT);
        canvas.drawPath(glyph,paint);canvas.restoreToCount(saved);
    }
    @Override public int getIntrinsicWidth(){return WIDTH;}
    @Override public int getIntrinsicHeight(){return HEIGHT;}
    @Override public void setAlpha(int value){int next=Math.max(0,Math.min(255,value));if(alpha!=next){alpha=next;invalidateSelf();}}
    @Override public int getAlpha(){return alpha;}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public ColorFilter getColorFilter(){return paint.getColorFilter();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
