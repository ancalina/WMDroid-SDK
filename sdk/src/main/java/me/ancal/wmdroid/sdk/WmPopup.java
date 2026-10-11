package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/** SHBoxEx popup body; coordinates follow AYGSHELL03855938/038566D0.
 * Supply plain content or a WmPaint wrapper, then attach to a modal overlay.
 */
public final class WmPopup extends FrameLayout {
    private final int anchor;
    public final TextView heading;
    public WmPopup(Context context,Typeface font,String caption,int anchor,int from,int to,View content){
        super(context);this.anchor=anchor;setColors(from,to);setOnClickListener(v->{});
        heading=WmControls.text(context,font,caption);heading.setTextColor(Color.WHITE);heading.setSingleLine(true);heading.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams title=new FrameLayout.LayoutParams(-1,44);title.leftMargin=20;title.rightMargin=20;title.topMargin=28;addView(heading,title);
        // Arbitrary SDK content may paint an opaque background. Keep it inside
        // the bubble's inner curve: outer radius18 minus half the4px pen.
        FrameLayout client=new FrameLayout(context){
            @Override protected void dispatchDraw(android.graphics.Canvas canvas){
                android.graphics.Path clip=new android.graphics.Path();clip.addRoundRect(new android.graphics.RectF(0,0,getWidth(),getHeight()),new float[]{0,0,0,0,16,16,16,16},android.graphics.Path.Direction.CW);
                int save=canvas.save();canvas.clipPath(clip);super.dispatchDraw(canvas);canvas.restoreToCount(save);
            }
        };
        // Legacy hardware Canvas cannot clip the opaque child to a Path.
        if(android.os.Build.VERSION.SDK_INT<18)client.setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        client.addView(content,new FrameLayout.LayoutParams(-1,-2));
        FrameLayout.LayoutParams body=new FrameLayout.LayoutParams(-1,-2);body.leftMargin=4;body.rightMargin=4;body.topMargin=72;body.bottomMargin=4;addView(client,body);
    }
    /** Host placement policy: wide SHBoxEx or narrow volume bubble. */
    public static int leftForAnchor(int width,int anchor){return leftForAnchor(width,anchor,WmMetrics.WIDTH);}
    /** Anchor in the actual logical surface; default VGA placement is unchanged. */
    public static int leftForAnchor(int width,int anchor,int surfaceWidth){
        if(width<=0||width>WmMetrics.WIDTH||surfaceWidth<WmMetrics.WIDTH)throw new IllegalArgumentException("popup width outside VGA surface");
        int left=width==154?anchor-90:Math.max((surfaceWidth-width)/2,anchor+20-width);
        return Math.max(0,Math.min(surfaceWidth-width,left));
    }
    public void setCaption(String caption){heading.setText(caption);}
    public void setColors(int from,int to){setBackgroundDrawable(new WmBubble(anchor,from,to));}
}
