package me.ancal.wmdroid.sdk;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Shared ROM Start texture, repeated in logical VGA coordinates in every shell. */
public class WmStartBackground extends Drawable {
    private final Paint texture=new Paint(),border=new Paint();
    public WmStartBackground(Bitmap bitmap){
        if(bitmap==null)throw new IllegalArgumentException("Start bitmap required");
        BitmapShader shader=new BitmapShader(bitmap,Shader.TileMode.REPEAT,Shader.TileMode.REPEAT);
        Matrix offset=new Matrix();offset.setTranslate(-2,-2);shader.setLocalMatrix(offset);texture.setShader(shader);
        border.setColor(0xff000000);border.setStyle(Paint.Style.STROKE);border.setStrokeWidth(2);
    }
    @Override public void draw(Canvas canvas){canvas.drawRect(getBounds(),texture);canvas.drawRect(getBounds().left+1,getBounds().top+1,getBounds().right-1,getBounds().bottom-1,border);}
    @Override public void setAlpha(int alpha){texture.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){texture.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
