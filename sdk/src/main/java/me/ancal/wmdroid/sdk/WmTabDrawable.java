package me.ancal.wmdroid.sdk;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.PixelFormat;
import android.graphics.ColorFilter;

/** Bottom property-sheet tab: selected face joins the white page, no top edge. */
public final class WmTabDrawable extends Drawable {
    private final boolean selected;private final int face;private final Paint paint=new Paint();
    public WmTabDrawable(boolean selected,int face){this.selected=selected;this.face=face;}
    @Override public void draw(Canvas c){int w=getBounds().width(),h=getBounds().height(),save=c.save();c.translate(getBounds().left,getBounds().top);paint.setColor(selected?Color.WHITE:face);c.drawRect(0,selected?0:1,w,h,paint);paint.setColor(Color.BLACK);if(!selected)c.drawRect(0,1,w,3,paint);c.drawRect(0,selected?0:3,2,h,paint);c.drawRect(w-2,selected?0:3,w,h,paint);c.restoreToCount(save);}
    @Override public void setAlpha(int a){}@Override public void setColorFilter(ColorFilter f){}@Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
