package me.ancal.wmdroid.sdk;

import android.graphics.Color;

/** Semantic control colors. ROM COLOR_3DFACE (index15) is #c9e7e9.
 * Hue follows the shared theme policy while original face saturation/value stay.
 */
public final class WmPalette {
    private WmPalette(){}
    /** ROM COLOR_HIGHLIGHT (index13); custom themes supply their shared selection. */
    public static int highlight(int selection){
        int opaque=selection|0xff000000;
        return opaque==0xff149ca7||opaque==0xff15afbc?0xff37c7d3:opaque;
    }
    public static int face(int selection){
        int opaque=selection|0xff000000;
        int source=0xffc9e7e9;
        if(opaque==0xff149ca7||opaque==0xff15afbc)return source;
        float[] chosen=new float[3],original=new float[3];Color.colorToHSV(opaque,chosen);
        if(chosen[1]<=.01f){int gray=(299*201+587*231+114*233)/1000;return Color.rgb(gray,gray,gray);}
        Color.colorToHSV(source,original);original[0]=chosen[0];return Color.HSVToColor(original);
    }
    /** RCML role15: native240-step hue, full saturation and brightness144. */
    static int calendarHeading(int selection){
        int r=Color.red(selection)*10000/255,g=Color.green(selection)*10000/255,b=Color.blue(selection)*10000/255;
        int max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b)),delta=max-min;
        boolean gray=max==0||delta==0||((10000*delta/max)*240/10000)==0;
        int hue=123;
        if(delta!=0&&max!=0){
            int sector=r==max?10000*(g-b)/delta:g==max?20000+10000*(b-r)/delta:40000+10000*(r-g)/delta;
            int scaled=40*sector;if(scaled<0)scaled+=2400000;hue=scaled/10000;
        }
        int position=6*(10000*hue/240),sector=position/10000,fraction=position%10000;
        int rising=fraction*6000/10000,falling=(10000-fraction)*6000/10000;
        switch(sector){
            case 0:r=6000;g=rising;b=0;break;
            case 1:r=falling;g=6000;b=0;break;
            case 2:r=0;g=6000;b=rising;break;
            case 3:r=0;g=falling;b=6000;break;
            case 4:r=rising;g=0;b=6000;break;
            default:r=6000;g=0;b=falling;break;
        }
        r=255*r/10000;g=255*g/10000;b=255*b/10000;
        if(gray){int luminance=(299*r+587*g+114*b)/1000;return Color.rgb(luminance,luminance,luminance);}
        return Color.rgb(r,g,b);
    }
    /** Linear ARGB interpolation; 0 selects a, 255 selects b. */
    public static int mix(int a,int b,int weightB){
        int t=Math.max(0,Math.min(255,weightB)),result=0;
        for(int shift=0;shift<=24;shift+=8){int value=(((a>>>shift)&255)*(255-t)+((b>>>shift)&255)*t+127)/255;result|=value<<shift;}
        return result;
    }
}
