package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared logical-VGA title assembly. Data, actions and timers belong to its host.
 * Native shell geometry: height52, logo42x36 at x4, caption insets14/4,
 * tray gap4 and close width50. Narrow connection and borderless close retain
 * WMDroid's device-reference policies; this is not a WinCE drawing backend.
 */
public final class WmTitleBar extends LinearLayout {
    public static final int CLOSE_WIDTH=50;
    public final LinearLayout start;
    public final ImageView logo;
    public final TextView caption;
    private final WmTraySlot[] trays=new WmTraySlot[4];
    private WmBar paint;
    private WmTrayClock clock;
    private View right;
    private ImageView close;
    private boolean closeOk;
    private boolean captionFeedback=true;

    public WmTitleBar(Context context,Typeface font,String text,WmBar paint){
        this(context,WmControls.text(context,font,text),paint);
    }
    /** Accept an unattached tagged label to preserve the host's font contract. */
    public WmTitleBar(Context context,TextView caption,WmBar paint){
        super(context);
        if(caption==null||caption.getParent()!=null)throw new IllegalArgumentException("Unattached caption required");
        if(paint==null)throw new IllegalArgumentException("Title paint required");
        this.caption=caption;this.paint=paint;setGravity(Gravity.CENTER_VERTICAL);
        start=new LinearLayout(context){
            @Override public boolean dispatchTouchEvent(MotionEvent event){
                boolean inside=event.getX()>=0&&event.getX()<startTouchWidth()&&event.getY()>=0&&event.getY()<getHeight();
                if(event.getActionMasked()==MotionEvent.ACTION_DOWN&&!inside){setPressed(false);return false;}
                if(event.getActionMasked()==MotionEvent.ACTION_MOVE&&!inside){setPressed(false);return true;}
                return super.dispatchTouchEvent(event);
            }
        };start.setGravity(Gravity.CENTER_VERTICAL);start.setPadding(4,0,0,0);
        logo=new ImageView(context);logo.setImageDrawable(WmIcons.load(context,"start"));logo.setScaleType(ImageView.ScaleType.FIT_XY);
        start.addView(logo,new LinearLayout.LayoutParams(42,36));
        caption.setSingleLine(true);caption.setEllipsize(TextUtils.TruncateAt.END);caption.setPadding(14,0,4,0);caption.setDuplicateParentStateEnabled(true);
        start.addView(caption,new LinearLayout.LayoutParams(0,WmMetrics.BAR,1));
        addView(start,new LinearLayout.LayoutParams(0,WmMetrics.BAR,1));
        WmTraySlot.activateOnDown(start);restyle(paint);
    }
    /** Label-sized Start target with8px touch allowance, capped at180 logical pixels.
     * Keep the title's remaining layout space so trays never shift with short labels. */
    public int startTouchWidth(){
        float text=caption.getLayout()!=null?caption.getLayout().getLineWidth(0):caption.getPaint().measureText(caption.getText().toString());
        return Math.min(start.getWidth(),Math.min(180,Math.max(72,(int)Math.ceil(4+42+14+text+4+8))));
    }
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
        super.onLayout(changed,left,top,right,bottom);
        float ink=caption.getLayout()==null?0:caption.getLayout().getLineWidth(0);
        boolean contained=caption.getLeft()+caption.getPaddingLeft()+ink<=startTouchWidth();
        if(captionFeedback!=contained){captionFeedback=contained;styleCaption();}
    }
    /** Inserts in shell order: connection, phone, volume, battery, clock, close. */
    public WmTraySlot addTray(int kind,ImageView image){
        if(kind<WmTraySlot.CONNECTION||kind>WmTraySlot.PHONE)throw new IllegalArgumentException("Unknown tray kind");
        if(trays[kind]!=null)throw new IllegalStateException("Tray already present");
        WmTraySlot slot=new WmTraySlot(getContext(),kind,image);slot.style(paint);
        int index=1;
        for(int existing:new int[]{WmTraySlot.CONNECTION,WmTraySlot.PHONE,WmTraySlot.VOLUME,WmTraySlot.BATTERY}){
            if(existing==kind)break;
            if(trays[existing]!=null)index++;
        }
        trays[kind]=slot;addView(slot,index);return slot;
    }
    /** Replaces the clock in place before the right slot. Caller owns callbacks. */
    public void setClock(WmTrayClock replacement){
        if(replacement==clock)return;
        if(replacement!=null&&replacement.getParent()!=null)throw new IllegalArgumentException("Unattached clock required");
        if(clock!=null)removeView(clock);clock=replacement;
        if(clock!=null){
            clock.setSoundEffectsEnabled(false);clock.setBackgroundDrawable(paint.titleButton(clock,false,false));
            LinearLayout.LayoutParams position=new LinearLayout.LayoutParams(-2,WmMetrics.BAR);position.rightMargin=4;
            addView(clock,right==null?getChildCount():indexOfChild(right),position);
        }
    }
    /** Borderless X/OK; caller supplies click action and localized description. */
    public ImageView closeButton(boolean ok){
        closeOk=ok;
        if(close==null){
            if(right!=null)removeView(right);
            close=new ImageView(getContext());close.setScaleType(ImageView.ScaleType.FIT_CENTER);close.setSoundEffectsEnabled(false);
            right=close;addView(close,new LinearLayout.LayoutParams(CLOSE_WIDTH,WmMetrics.BAR));
        }
        styleClose();return close;
    }
    /** Today reserves the applet's right slot, following the observed device. */
    public void reserveCloseSpace(){
        if(right!=null&&close==null)return;
        if(right!=null)removeView(right);close=null;
        right=new View(getContext());addView(right,new LinearLayout.LayoutParams(CLOSE_WIDTH,WmMetrics.BAR));
    }
    /** Restyles existing cells without replacing labels, clocks or actions. */
    public void reloadResources(){logo.setImageDrawable(WmIcons.load(getContext(),"start"));}
    public void restyle(WmBar paint){
        if(paint==null)throw new IllegalArgumentException("Title paint required");this.paint=paint;
        setBackgroundDrawable(paint);start.setBackgroundDrawable(paint.titleButton(start,true,false));
        styleCaption();
        for(WmTraySlot tray:trays)if(tray!=null)tray.style(paint);
        if(clock!=null)clock.setBackgroundDrawable(paint.titleButton(clock,false,false));
        if(close!=null)styleClose();
    }
    private void styleCaption(){
        caption.setTextColor(captionFeedback?new ColorStateList(new int[][]{{android.R.attr.state_pressed},{android.R.attr.state_selected},{}},new int[]{paint.color(39),paint.color(39),Color.WHITE}):ColorStateList.valueOf(Color.WHITE));
    }
    private void styleClose(){
        close.setImageDrawable(new WmCloseGlyph(closeOk,paint.color(37),paint.color(38)));
        close.setBackgroundDrawable(paint.titleButton(close,false,false));
    }
}
