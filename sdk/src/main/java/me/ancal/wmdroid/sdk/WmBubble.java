package me.ancal.wmdroid.sdk;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** SHPaintBubbleFrame 3856130: white window brush, round corners and asymmetric tail.
 * VGA corner diameter 36, tail base 52, pen 4. Header SHColor IDs13/14 are
 * distinct from the LauncherBar style table. Captured GDI body top is25px below tip.
 */
public class WmBubble extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int anchor;
    private final int from,to;
    public WmBubble(int anchor,int from,int to){this.anchor=anchor;this.from=from;this.to=to;}
    @Override public void draw(Canvas c){
        float w=getBounds().width(),h=getBounds().height(),left,right;
        // SHPaintBubbleFrame 03856130: clamp the base, never the supplied tip.
        // Near the left edge the 52px base extends to the right of the anchor.
        if(anchor<getBounds().width()/4){left=Math.max(18,anchor);right=left+52;}
        else{right=Math.min(w-18,anchor);left=right-52;}
        int frame=c.save();c.translate(getBounds().left,getBounds().top);
        // One contour shares the fill, header clip and frame. Separate body/tail
        // strokes leave a seam when their antialiased edges meet after scaling.
        Path shape=new Path();shape.moveTo(20,27);shape.lineTo(left,27);
        shape.lineTo(anchor,2);shape.lineTo(right,27);shape.lineTo(w-20,27);
        shape.arcTo(new RectF(w-38,27,w-2,63),270,90);
        shape.lineTo(w-2,h-20);shape.arcTo(new RectF(w-38,h-38,w-2,h-2),0,90);
        shape.lineTo(20,h-2);shape.arcTo(new RectF(2,h-38,38,h-2),90,90);
        shape.lineTo(2,45);shape.arcTo(new RectF(2,27,38,63),180,90);shape.close();
        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);c.drawPath(shape,paint);
        // Hardware clipPath is unsupported before API18. Draw the contour
        // itself, with a rectangular height clip supported on every API.
        int header=c.save();c.clipRect(0,0,w,72);
        paint.setShader(new LinearGradient(4,0,w-4,0,from,to,Shader.TileMode.CLAMP));
        c.drawPath(shape,paint);paint.setShader(null);c.restoreToCount(header);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(4);paint.setColor(Color.BLACK);
        c.drawPath(shape,paint);
        c.restoreToCount(frame);
    }
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(android.graphics.ColorFilter filter){}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
