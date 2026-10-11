package me.ancal.wmdroid.sdk;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;

/** Renders launcher icons at physical pixel resolution without changing their bounds. */
public final class WmAppIcons {
    private WmAppIcons(){}
    /** Returns caller-owned pixels; source bounds are restored. Adaptive foregrounds
     * crop alpha&gt;16 bounds, retaining aspect with at least 4% padding per side.
     * Fall back if no bounds or over 95% of alpha&gt;0 pixels have all RGB channels
     * greater than 235. Do not render a shared source concurrently. */
    public static Bitmap render(Drawable source,int pixels,boolean unframed){
        if(source==null)return null;
        if(pixels<=0)throw new IllegalArgumentException("pixels must be positive");
        if(unframed&&Build.VERSION.SDK_INT>=26){
            Drawable foreground=Api26.foreground(source);
            if(foreground!=null){
                Bitmap image=whole(foreground,pixels);
                try{Bitmap cropped=crop(image,pixels);if(cropped!=null)return cropped;}
                finally{image.recycle();}
            }
        }
        return whole(source,pixels);
    }
    private static Bitmap whole(Drawable source,int pixels){
        Rect bounds=new Rect(source.getBounds());Bitmap image=Bitmap.createBitmap(pixels,pixels,Bitmap.Config.ARGB_8888);image.setDensity(Bitmap.DENSITY_NONE);
        try{source.setBounds(0,0,pixels,pixels);source.draw(new Canvas(image));return image;}
        catch(RuntimeException failure){image.recycle();throw failure;}
        finally{source.setBounds(bounds);}
    }
    private static Bitmap crop(Bitmap image,int pixels){
        int[] colors=new int[pixels*pixels];image.getPixels(colors,0,pixels,0,0,pixels,pixels);
        int left=pixels,top=pixels,right=-1,bottom=-1;long visible=0,white=0;
        for(int y=0;y<pixels;y++)for(int x=0;x<pixels;x++){
            int color=colors[y*pixels+x],alpha=color>>>24;
            if(alpha>0){visible++;if((color>>16&255)>235&&(color>>8&255)>235&&(color&255)>235)white++;}
            if(alpha>16){left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);}
        }
        // White foregrounds need their original adaptive background to remain visible.
        if(right<left||bottom<top||white*100>visible*95)return null;
        Rect bounds=new Rect(left,top,right+1,bottom+1);float padding=pixels*.04f,available=pixels-2*padding;
        float scale=Math.min(available/bounds.width(),available/bounds.height()),width=bounds.width()*scale,height=bounds.height()*scale;
        RectF target=new RectF((pixels-width)/2,(pixels-height)/2,(pixels+width)/2,(pixels+height)/2);
        Bitmap result=Bitmap.createBitmap(pixels,pixels,Bitmap.Config.ARGB_8888);result.setDensity(Bitmap.DENSITY_NONE);
        try{new Canvas(result).drawBitmap(image,bounds,target,new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG|Paint.DITHER_FLAG));return result;}
        catch(RuntimeException failure){result.recycle();throw failure;}
    }
    // Keep class verification on API 14..25 independent of the API 26 type.
    private static final class Api26 {
        static Drawable foreground(Drawable source){return source instanceof android.graphics.drawable.AdaptiveIconDrawable?((android.graphics.drawable.AdaptiveIconDrawable)source).getForeground():null;}
    }
}
