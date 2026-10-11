package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

/** Original 73-color table, title 1FBC4 and softkey 38815D4 region descriptors.
 * GDI GradientFill endpoint quantization still needs a WinCE runtime comparison.
 */
public class WmBar extends Drawable {
    private final int[] colors=new int[73];
    private final boolean softkey;
    private final Bitmap pixels;
    private final Bitmap pressedPixels, startSeparator, closeNormal, closePressed;
    private Bitmap widePixels,widePressed;
    private int wideWidth;
    private Bitmap startPixels;
    private int startWidth,startRamp;
    // Keep ROM bands and 2px borders crisp when the VGA canvas is scaled.
    // Filtering mixes the dark border into neighbouring content/gradient rows.
    private final Paint paint=new Paint();
    public WmBar(Context context,boolean softkey,int base) {
        this.softkey=softkey;
        boolean grayscale=WmColor.isGray(base);
        int hue=grayscale?123:WmColor.hue(base);
        try(java.io.InputStream input=WmResources.openAsset(context,"themes/gradients.json")) {
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;while((count=input.read(buffer))!=-1){if(bytes.size()+count>256*1024)throw new IOException("Gradient table too large");bytes.write(buffer,0,count);}
            JSONObject source=new JSONObject(new String(bytes.toByteArray(),java.nio.charset.Charset.forName("UTF-8")));JSONArray records=source.getJSONArray("records");
            if(records.length()!=colors.length)throw new IOException("Gradient table requires73 records");
            int[] verified=new int[colors.length];boolean[] seen=new boolean[colors.length];
            // 3854A8C: retain each original S/B pair; neutral themes additionally
            // convert the native RGB result to gray. Bands and geometry stay intact.
            for(int i=0;i<records.length();i++){JSONObject entry=records.getJSONObject(i);double rawIndex=entry.getDouble("index"),rawHsb=entry.getDouble("hsb");int index=(int)rawIndex,hsb=(int)rawHsb;
                if(rawIndex!=index||index<0||index>=colors.length||seen[index]||rawHsb!=hsb||hsb<0||hsb>0xffffff)throw new IOException("Invalid gradient record");
                seen[index]=true;int color=WmColor.rgb(hue,(hsb>>>8)&255,(hsb>>>16)&255);verified[index]=grayscale?WmColor.gray(color):color;}
            System.arraycopy(verified,0,colors,0,colors.length);
        } catch(IOException|org.json.JSONException error) {fallback(base);}
        pixels=barPixels(480,false);pressedPixels=barPixels(480,true);
        if(softkey) {
            startSeparator=null;closeNormal=null;closePressed=null;
        } else {
            int[] separator=new int[2*40];
            for(int y=0;y<40;y++) {
                int color=y<20?mix(colors[12],colors[11],y,20):mix(colors[11],colors[13],y-20,20);
                separator[y*2]=color;separator[y*2+1]=color;
            }
            startSeparator=Bitmap.createBitmap(separator,2,40,Bitmap.Config.ARGB_8888);
            closeNormal=closeBox(false);closePressed=closeBox(true);
        }
    }
    private void fallback(int base){
        int luminance=WmColor.gray(base)&255;
        if(luminance>100)base=0xff000000|((((base>>>16)&255)*100/luminance)<<16)|((((base>>>8)&255)*100/luminance)<<8)|((base&255)*100/luminance);
        java.util.Arrays.fill(colors,base|0xff000000);
        int pressed=mix(base,luminance<32?0xff606060:0xff000000,1,3);
        for(int index:new int[]{9,18,19,31,32,54,55,70,71})colors[index]=pressed;
        // Functional foreground slots must remain visible without the native table.
        for(int index:new int[]{14,15,37,38,39})colors[index]=android.graphics.Color.WHITE;
    }
    public int color(int index){return colors[index];}
    /** 3881E34/385CA64: copy the button's absolute part of the shared softkey bitmap. */
    public Drawable softButton(View cell) {
        if(!softkey)throw new IllegalStateException("Softkey buttons require a softkey bar");
        return new Drawable() {
            private final Paint buttonPaint=new Paint();
            private final Rect destination=new Rect();
            private boolean active;
            @Override public boolean isStateful(){return true;}
            @Override protected boolean onStateChange(int[] state) {
                boolean enabled=false,pressed=false;
                if(state!=null)for(int value:state) {
                    if(value==android.R.attr.state_enabled)enabled=true;
                    if(value==android.R.attr.state_pressed||value==android.R.attr.state_selected)pressed=true;
                }
                boolean next=enabled&&pressed;
                if(active==next)return false;
                active=next;invalidateSelf();return true;
            }
            @Override public void draw(Canvas canvas) {
                if(!active)return;
                Rect bounds=getBounds();int save=canvas.save();canvas.clipRect(bounds);
                int left=bounds.left-cell.getLeft(),top=bounds.top-cell.getTop();
                // 38809A0/387FE64:180px keys anchored to either screen edge.
                // Smaller UI scales expand the logical width and centre gap.
                int width=barWidth(cell);
                boolean right=cell.getLeft()>=width/2;
                int nativeLeft=right?width-WmCommandBar.KEY_WIDTH:0,nativeRight=right?width:WmCommandBar.KEY_WIDTH;
                canvas.clipRect(left+nativeLeft,bounds.top,left+nativeRight,bounds.bottom);
                destination.set(left,top,left+width,top+pressedPixels.getHeight());
                canvas.drawBitmap(barBitmap(width,true),null,destination,buttonPaint);
                canvas.restoreToCount(save);
            }
            @Override public void setAlpha(int alpha){buttonPaint.setAlpha(alpha);invalidateSelf();}
            @Override public void setColorFilter(ColorFilter filter){buttonPaint.setColorFilter(filter);invalidateSelf();}
            @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
        };
    }
    /** 246A0/24E44: use the cell's absolute title-bar x, including while selected. */
    public Drawable titleButton(View cell,boolean start,boolean close) {
        if(softkey)throw new IllegalStateException("Title buttons require a title bar");
        return new Drawable() {
            private final Paint buttonPaint=new Paint();
            private final Rect destination=new Rect();
            private boolean active;
            @Override public boolean isStateful(){return true;}
            @Override protected boolean onStateChange(int[] state) {
                boolean next=false;
                if(state!=null)for(int value:state)if(value==android.R.attr.state_pressed||value==android.R.attr.state_selected){next=true;break;}
                if(active==next)return false;
                active=next;invalidateSelf();return true;
            }
            @Override public void draw(Canvas canvas) {
                Rect bounds=getBounds();int save=canvas.save();canvas.clipRect(bounds);
                // Use the same full-bar width as the normal background, then clip the cell.
                if(active){
                    // The highlight fades at180, independently of the label-sized touch target.
                    int ramp=Math.min(180,cell.getWidth());
                    if(start)canvas.clipRect(bounds.left,bounds.top,bounds.left+ramp,bounds.bottom);
                    int left=bounds.left-cell.getLeft(),top=bounds.top-cell.getTop();
                    int width=barWidth(cell);destination.set(left,top,left+width,top+pressedPixels.getHeight());
                    canvas.drawBitmap(start?startBitmap(width,ramp):barBitmap(width,true),null,destination,buttonPaint);
                }
                if(start&&!active)drawAt(canvas,startSeparator,bounds.left+50-cell.getLeft(),bounds.top+6-cell.getTop());
                if(close)drawAt(canvas,active?closePressed:closeNormal,bounds.left+6,bounds.top+8);
                canvas.restoreToCount(save);
            }
            private void drawAt(Canvas canvas,Bitmap bitmap,int left,int top) {
                // An explicit destination also prevents Android bitmap-density scaling.
                destination.set(left,top,left+bitmap.getWidth(),top+bitmap.getHeight());
                canvas.drawBitmap(bitmap,null,destination,buttonPaint);
            }
            @Override public void setAlpha(int alpha){buttonPaint.setAlpha(alpha);invalidateSelf();}
            @Override public void setColorFilter(ColorFilter filter){buttonPaint.setColorFilter(filter);invalidateSelf();}
            @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
        };
    }
    private Bitmap startBitmap(int width,int ramp){
        if(startWidth!=width||startRamp!=ramp||startPixels==null){
            Bitmap normal=barBitmap(width,false);int[] data=new int[width*52];
            normal.getPixels(data,0,width,0,0,width,52);
            // A clock can leave less than180px for Start. Fade before its actual edge.
            if(ramp>0)for(int y=0;y<50;y++){
                int end=data[y*width+Math.min(ramp,width-1)];
                for(int x=0;x<ramp;x++)data[y*width+x]=mix(colors[9],end,x,ramp);
            }
            startPixels=Bitmap.createBitmap(data,width,52,Bitmap.Config.ARGB_8888);startWidth=width;startRamp=ramp;
        }
        return startPixels;
    }
    private Bitmap barBitmap(int width,boolean active){
        if(width==480||width<360)return active?pressedPixels:pixels;
        if(wideWidth!=width){
            widePixels=barPixels(width,false);widePressed=barPixels(width,true);wideWidth=width;
        }
        return active?widePressed:widePixels;
    }
    private Bitmap barPixels(int width,boolean active){
        int[] data=new int[width*52];
        int middle=width-360;
        int[] main={180,middle/2,middle-middle/2,180},edge={width/2,0,0,width-width/2};
        // Preserve native edge ramps; expand only the middle. Both states meet at180.
        if(softkey){
            band(data,colors,0,2,new int[]{56,57,57,57,58},null,edge);
            band(data,colors,2,2,new int[]{59,60,60,60,61},null,edge);
            band(data,colors,4,20,active?new int[]{31,25,54,25,32}:new int[]{48,25,24,25,49},active?new int[]{31,29,30,29,32}:new int[]{50,29,28,29,51},main);
            band(data,colors,24,28,active?new int[]{31,27,55,27,32}:new int[]{52,27,26,27,53},null,main);
        }else{
            band(data,colors,0,2,new int[]{65,66,66,66,67},null,edge);
            if(active){
                // Retain native pressed corner9; converge before the bounded edge ramp.
                int end=horizontal(colors,new int[]{65,66,66,66,67},edge,180);
                for(int x=0;x<180;x++)for(int y=0;y<2;y++)data[y*width+x]=mix(colors[9],end,x,180);
            }
            band(data,colors,2,20,new int[]{active?9:42,3,2,3,43},new int[]{active?9:44,7,6,7,45},main);
            band(data,colors,22,28,new int[]{active?9:46,5,4,5,47},null,main);
            band(data,colors,50,2,new int[]{62,63,63,63,64},null,edge);
        }
        return Bitmap.createBitmap(data,width,52,Bitmap.Config.ARGB_8888);
    }
    private int barWidth(View cell){
        if(cell.getParent() instanceof View&&((View)cell.getParent()).getWidth()>0)return ((View)cell.getParent()).getWidth();
        return getBounds().width()>0?getBounds().width():WmMetrics.WIDTH;
    }
    private Bitmap closeBox(boolean active) {
        // 246A0: (6,8), 38x34; wm_ok is only the transparent glyph drawn above this box.
        // Border uses SHGetMetric(12) -> GetSystemMetrics(SM_CXBORDER). Keep the existing
        // VGA 2px convention; its actual WinCE runtime metric/rasterization is unverified.
        int[] data=new int[38*34];
        for(int y=0;y<34;y++)for(int x=0;x<38;x++) {
            int value=colors[72];
            if(x>=2&&x<36&&y>=2&&y<32)value=mix(colors[active?70:68],colors[active?71:69],y-2,30);
            if(x>=4&&x<34&&y>=4&&y<30)value=mix(colors[active?18:16],colors[active?19:17],y-4,26);
            data[y*38+x]=value;
        }
        return Bitmap.createBitmap(data,38,34,Bitmap.Config.ARGB_8888);
    }
    private static int mix(int a,int b,int n,int denominator) {
        if(a==b||n==0)return a|0xff000000;
        if(n==denominator)return b|0xff000000;
        int inverse=denominator-n;
        int red=(((a>>>16)&255)*inverse+((b>>>16)&255)*n)/denominator;
        int green=(((a>>>8)&255)*inverse+((b>>>8)&255)*n)/denominator;
        int blue=((a&255)*inverse+(b&255)*n)/denominator;
        return 0xff000000|(red<<16)|(green<<8)|blue;
    }
    private static int horizontal(int[] colors,int[] ids,int[] lengths,int x) {
        int left=0;
        for(int segment=0;segment<4;segment++) {
            int width=lengths[segment];
            if(width>0&&x<left+width)return mix(colors[ids[segment]],colors[ids[segment+1]],x-left,width+(segment<3?1:0));
            left+=width;
        }
        return colors[ids[4]];
    }
    private static void band(int[] data,int[] colors,int top,int height,int[] upper,int[] lower,int[] lengths) {
        if(height<=0)return;
        int width=data.length/52;
        if(lower==null){
            int row=top*width;
            for(int x=0;x<width;x++)data[row+x]=horizontal(colors,upper,lengths,x)|0xff000000;
            for(int y=1;y<height;y++)System.arraycopy(data,row,data,row+y*width,width);
            return;
        }
        for(int x=0;x<width;x++) {int a=horizontal(colors,upper,lengths,x),b=horizontal(colors,lower,lengths,x);for(int y=0;y<height;y++)data[(top+y)*width+x]=mix(a,b,y,height);}
    }
    @Override public void draw(Canvas canvas){canvas.drawBitmap(barBitmap(getBounds().width(),false),null,getBounds(),paint);}
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
