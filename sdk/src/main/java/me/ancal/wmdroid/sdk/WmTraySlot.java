package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

/** Shared VGA tray hit target. Artwork stays32px; connection ink is packed.
 * Native shell normal slots38, battery42, gap4; narrow connection is a user override.
 */
public final class WmTraySlot extends FrameLayout {
    public static final int CONNECTION=0,VOLUME=1,BATTERY=2,PHONE=3;
    public final ImageView image;
    private final int kind;
    private String connectionId="";
    private android.graphics.drawable.AnimationDrawable connectionAnimation;
    private static final int[] CONNECTION_FRAMES={1,2,3,4,5,6,7,8,9,10,11,12,12,13,14,15,16,17,18,19,20,21,22,1,1,1};
    public WmTraySlot(Context context,int kind,ImageView image){
        super(context);if(kind<CONNECTION||kind>PHONE)throw new IllegalArgumentException("Unknown tray kind");
        this.kind=kind;this.image=image;
        FrameLayout.LayoutParams art=new FrameLayout.LayoutParams(32,32,Gravity.TOP|Gravity.LEFT);
        art.leftMargin=kind==CONNECTION||kind==BATTERY?2:4;art.topMargin=10;addView(image,art);
        LinearLayout.LayoutParams slot=new LinearLayout.LayoutParams(kind==CONNECTION?22:kind==BATTERY?42:38,WmMetrics.BAR);slot.rightMargin=4;setLayoutParams(slot);
        setSoundEffectsEnabled(false);if(kind!=BATTERY)activateOnDown(this);
    }
    public void style(WmBar bar){setBackgroundDrawable(bar.titleButton(this,false,false));}
    /** Preserves all ink; never stretches the18px Wi-Fi drawing to slot width. */
    public void connectionIcon(String id){
        if(kind!=CONNECTION)throw new IllegalStateException("Not a connection slot");
        if(!"signal".equals(id)&&!"network_connected".equals(id)&&!"network_data_connected".equals(id)&&!"network_disconnected".equals(id))throw new IllegalArgumentException("Not a connection icon");
        if(id.equals(connectionId))return;
        stopConnectionAnimation();connectionId=id;
        image.setImageDrawable(WmIcons.load(getContext(),id));
        int width="signal".equals(id)?22:"network_connected".equals(id)?32:34,inset="network_disconnected".equals(id)?1:2;
        sizeConnection(width,inset);
    }
    private void sizeConnection(int width,int inset){
        LinearLayout.LayoutParams slot=(LinearLayout.LayoutParams)getLayoutParams();if(slot.width!=width){slot.width=width;setLayoutParams(slot);}
        FrameLayout.LayoutParams art=(FrameLayout.LayoutParams)image.getLayoutParams();if(art.leftMargin!=inset){art.leftMargin=inset;image.setLayoutParams(art);}
    }
    /** Native tray6 frames/timing, with Android connection states as the backend.
     * Internet validation is separate: a captive portal is still a connected link.
     */
    public void connectionState(WmDevice.Network network){
        if(kind!=CONNECTION)throw new IllegalStateException("Not a connection slot");
        if(network==null||!network.connected&&!network.connecting){connectionIcon("network_disconnected");return;}
        if(!network.connecting){connectionIcon(network.wifi?"signal":network.cellular?"network_data_connected":"network_connected");return;}
        if(!"connecting".equals(connectionId)){
            stopConnectionAnimation();connectionId="connecting";sizeConnection(34,1);
            if(connectionAnimation==null)connectionAnimation=loadConnectionAnimation();
            image.setImageDrawable(connectionAnimation==null?WmIcons.load(getContext(),"network_data_connected"):connectionAnimation);
        }
        if(connectionAnimation!=null&&getWindowToken()!=null&&getWindowVisibility()==VISIBLE)connectionAnimation.start();
    }
    private android.graphics.drawable.AnimationDrawable loadConnectionAnimation(){
        android.graphics.drawable.AnimationDrawable sequence=new android.graphics.drawable.AnimationDrawable();
        java.util.Map<Integer,android.graphics.Bitmap> bitmaps=new java.util.HashMap<>();
        try{for(int frame:CONNECTION_FRAMES){android.graphics.Bitmap bitmap=bitmaps.get(frame);
            if(bitmap==null){bitmap=connectionFrame(frame);bitmaps.put(frame,bitmap);}
            android.graphics.drawable.BitmapDrawable art=new android.graphics.drawable.BitmapDrawable(getResources(),bitmap);art.setFilterBitmap(true);sequence.addFrame(art,150);
        }return sequence;}catch(java.io.IOException unavailable){return null;}
    }
    private android.graphics.Bitmap connectionFrame(int frame)throws java.io.IOException {
        return WmResources.bitmap(getContext(),"status/connection/connecting-"+(frame<10?"0"+frame:Integer.toString(frame))+".png",32,32);
    }
    public void stopConnectionAnimation(){if(connectionAnimation!=null)connectionAnimation.stop();}
    /** Discard per-view artwork after pack activation; retain geometry and listeners. */
    public void reloadResources(){stopConnectionAnimation();connectionAnimation=null;connectionId="";}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if("connecting".equals(connectionId)&&connectionAnimation!=null)connectionAnimation.start();}
    @Override protected void onDetachedFromWindow(){stopConnectionAnimation();super.onDetachedFromWindow();}
    /** shell32 tray flags activate Start/data/phone/volume on DOWN; battery on UP. */
    public static void activateOnDown(View button){
        button.setSoundEffectsEnabled(false);
        button.setOnTouchListener((view,event)->{
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:view.setPressed(true);view.performClick();return true;
                case MotionEvent.ACTION_MOVE:view.setPressed(event.getX()>=0&&event.getX()<view.getWidth()&&event.getY()>=0&&event.getY()<view.getHeight());return true;
                case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:view.setPressed(false);return true;
                default:return true;
            }
        });
    }
}
