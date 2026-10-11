package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import java.io.IOException;
import java.io.InputStream;

/** Original outres.192 BITMAP11029; native mask WORDs confirmed in caldll.
 * RGB red is the native ImageList transparency key. The six32x28 source cells
 * are the VGA atlas adaptation; native cell width is runtime SM_CXSMICON.
 * This component does not assert original-device GDI scaling or icon placement.
 */
public final class WmCalendarSymbols extends Drawable {
    public static final int REMINDER=1,RECURRENCE=2,NOTES=4,LOCATION=8,ATTENDEES=16,PRIVATE=32,ALL=63;
    private static final java.util.Map<Context,Cache> CACHE=new java.util.WeakHashMap<>();
    private static final class Cache {final WmResourcePack pack;Bitmap atlas,eventAtlas;Cache(WmResourcePack pack){this.pack=pack;}}
    private Bitmap image;
    private final Context context;
    private final boolean event;
    private WmResourcePack resourcePack;
    private final Rect source;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    /** Options symbol index0..5, not bits: reminder, recurrence, notes, location,
     * attendees, private. Null context or out-of-range index is invalid. */
    public WmCalendarSymbols(Context context,int index){
        this(context,index,false);
    }
    private WmCalendarSymbols(Context context,int index,boolean event){
        if(context==null||index<0||index>=(event?20:6))throw new IllegalArgumentException("Invalid Calendar symbol index");
        Context app=context.getApplicationContext();this.context=app==null?context:app;this.event=event;
        Cache loaded=load(this.context,event);image=event?loaded.eventAtlas:loaded.atlas;resourcePack=loaded.pack;
        source=new Rect(index*32,0,(index+1)*32,28);
    }
    /** Event symbol: one of REMINDER, RECURRENCE, NOTES, LOCATION, ATTENDEES, PRIVATE,
     * never OR-combined. recurrenceException applies only to RECURRENCE.
     * Equivalent to the four-argument overload with past=false. Null context or
     * invalid symbol throws IllegalArgumentException. */
    public static WmCalendarSymbols forEvent(Context context,int symbol,boolean recurrenceException){
        return forEvent(context,symbol,recurrenceException,false);
    }
    /** One event symbol bit (no OR combination); recurrenceException affects only
     * RECURRENCE. past selects the past-event bank. Null context or invalid
     * symbol throws IllegalArgumentException. */
    public static WmCalendarSymbols forEvent(Context context,int symbol,boolean recurrenceException,boolean past){
        int index;
        switch(symbol){case REMINDER:index=0;break;case RECURRENCE:index=recurrenceException?2:1;break;case NOTES:index=3;break;case LOCATION:index=4;break;case ATTENDEES:index=7;break;case PRIVATE:index=8;break;default:throw new IllegalArgumentException("One Calendar symbol bit required");}
        return new WmCalendarSymbols(context,index+(past?10:0),true);
    }
    private static Cache load(Context context,boolean event){
        Context app=context.getApplicationContext();if(app==null)app=context;
        synchronized(CACHE){
            try{
                WmResourcePack pack=WmResources.prepare(context);Cache cache=CACHE.get(app);
                if(cache==null||cache.pack!=pack){cache=new Cache(pack);CACHE.put(app,cache);}
                Bitmap current=event?cache.eventAtlas:cache.atlas;if(current!=null)return cache;
                int width=event?640:192;
                Bitmap value;
                try {
                    Bitmap decoded=WmResources.bitmap(context,event?"calendar/event-symbols.png":"calendar/appointment-symbols.png",width,28);
                    value=decoded.copy(Bitmap.Config.ARGB_8888,true);decoded.recycle();value.setHasAlpha(true);
                    int[] colors=new int[width*28];value.getPixels(colors,0,width,0,0,width,28);
                    for(int i=0;i<colors.length;i++)if((colors[i]&0x00ffffff)==0x00ff0000)colors[i]=0;
                    value.setPixels(colors,0,width,0,0,width,28);
                }catch(IOException absent){value=fallback(event);}
                value.setDensity(Bitmap.DENSITY_NONE);if(event)cache.eventAtlas=value;else cache.atlas=value;return cache;
            }catch(IOException failure){Cache cache=new Cache(null);if(event)cache.eventAtlas=fallback(true);else cache.atlas=fallback(false);return cache;}
        }
    }
    private static Bitmap fallback(boolean event){
        String[] symbols=event?new String[]{"⏰","🔁","↪","📝","📍","📅","📅","👥","🔒","📅"}:new String[]{"⏰","🔁","📝","📍","👥","🔒"};
        int count=event?20:6;Bitmap image=Bitmap.createBitmap(count*32,28,Bitmap.Config.ARGB_8888);image.setDensity(Bitmap.DENSITY_NONE);
        Canvas canvas=new Canvas(image);Paint paint=new Paint();
        for(int i=0;i<count;i++){Bitmap cell=WmResources.emojiBitmap(symbols[i%symbols.length],32,28);paint.setAlpha(event&&i>=10?140:255);canvas.drawBitmap(cell,i*32,0,paint);cell.recycle();}return image;
    }
    @Override public void draw(Canvas canvas){
        // Retained calendar controls share the atlas cache; a new pack changes its identity.
        try{WmResourcePack current=WmResources.prepare(context);if(current!=resourcePack){Cache loaded=load(context,event);image=event?loaded.eventAtlas:loaded.atlas;resourcePack=loaded.pack;}}catch(IOException unavailable){}
        canvas.drawBitmap(image,source,getBounds(),paint);
    }
    @Override public int getIntrinsicWidth(){return 32;}
    @Override public int getIntrinsicHeight(){return 28;}
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
