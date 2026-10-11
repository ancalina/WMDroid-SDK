package me.ancal.wmdroid.sdk;

/** Integer HSB conversion recovered from AYGSHELL 3851D44/3851F2C.
 * ARM 3851EAC..3851EFC is authoritative: hue arithmetic is not an 8-bit char.
 */
public final class WmColor {
    public static int hue(int rgb) {
        int r=10000*((rgb>>>16)&255)/255,g=10000*((rgb>>>8)&255)/255,b=10000*(rgb&255)/255;
        int max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b)),delta=max-min;
        if(delta==0)return 123;
        int h=r==max?10000*(g-b)/delta:g==max?20000+10000*(b-r)/delta:40000+10000*(r-g)/delta;
        int scaled=40*h;if(scaled<0)scaled+=2400000;return scaled/10000;
    }
    public static int rgb(int hue,int saturation,int brightness) {
        int v=10000*brightness/240,s=10000*saturation/240;
        int sector=6*(10000*hue/240),f=sector%10000;
        int p=(10000-s)*v/10000,q=(10000-f*s/10000)*v/10000,t=(10000-(10000-f)*s/10000)*v/10000;
        int r=v,g=v,b=v;
        if(s!=0)switch(sector/10000){case 0:r=v;g=t;b=p;break;case 1:r=q;g=v;b=p;break;case 2:r=p;g=v;b=t;break;case 3:r=p;g=q;b=v;break;case 4:r=t;g=p;b=v;break;default:r=v;g=p;b=q;}
        return 0xff000000|((255*r/10000)<<16)|((255*g/10000)<<8)|(255*b/10000);
    }
    public static boolean isGray(int color){int r=(color>>>16)&255,g=(color>>>8)&255,b=color&255,max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b));return max==0||(max-min)*100<=max;}
    public static int gray(int color){int v=(299*((color>>>16)&255)+587*((color>>>8)&255)+114*(color&255))/1000;return (color&0xff000000)|(v<<16)|(v<<8)|v;}
    /** Android bitmap adaptation: preserve source S/V and hue offsets; gray uses the WM weights. Returns source or a new bitmap; never mutates/recycles source. */
    public static android.graphics.Bitmap tint(android.graphics.Bitmap source,int base,int chosen){
        boolean grayscale=isGray(chosen);
        float[] target=new float[3],original=new float[3],hsv=new float[3];
        android.graphics.Color.colorToHSV(chosen,target);android.graphics.Color.colorToHSV(base,original);
        float delta=target[0]-original[0];
        if(!grayscale&&delta==0)return source;
        int width=source.getWidth(),height=source.getHeight();int[] pixels=new int[width*height];
        source.getPixels(pixels,0,width,0,0,width,height);boolean changed=false;
        android.util.SparseIntArray converted=new android.util.SparseIntArray(256);
        for(int i=0;i<pixels.length;i++){
            int pixel=pixels[i];
            if((pixel>>>24)==0||(((pixel>>>16)&255)==((pixel>>>8)&255)&&((pixel>>>8)&255)==(pixel&255)))continue;
            int found=converted.indexOfKey(pixel),result;
            if(found>=0)result=converted.valueAt(found);
            else{
                if(grayscale)result=gray(pixel); // Intentional exception to S/V preservation for a gray theme.
                else{android.graphics.Color.colorToHSV(pixel,hsv);hsv[0]=(hsv[0]+delta+360)%360;result=android.graphics.Color.HSVToColor(pixel>>>24,hsv);}
                // GIF palettes fit here; arbitrary source bitmaps must not grow this cache.
                if(converted.size()<256)converted.put(pixel,result);
            }
            pixels[i]=result;changed|=result!=pixel;
        }
        if(!changed)return source;
        android.graphics.Bitmap result=android.graphics.Bitmap.createBitmap(pixels,width,height,android.graphics.Bitmap.Config.ARGB_8888);
        result.setDensity(source.getDensity());result.setHasAlpha(source.hasAlpha());return result;
    }
}
