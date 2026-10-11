package me.ancal.wmdroid.sdk;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** GWES down-arrow scanlines at the SDK's logical192-DPI VGA baseline.
 * Popup ownership remains with the caller; detach ends one dropdown lifetime. */
final class WmComboGlyph implements View.OnAttachStateChangeListener {
    private final View owner;
    private View popup;
    private final java.util.List<WmRadioMenu> linkedMenus=new java.util.ArrayList<>();
    private boolean dropped;
    WmComboGlyph(View owner){this.owner=owner;}
    boolean key(android.view.KeyEvent event){
        View target=popup;if(!isDropped()||target==owner)return false;
        // A caller can bind any View; never redispatch into the owner through an ancestor.
        for(android.view.ViewParent parent=owner.getParent();parent!=null;parent=parent.getParent())if(parent==target)return false;
        if(owner instanceof WmEditableChoice){WmRadioMenu menu=choiceMenu(target);if(menu!=null)return menu.dispatchKeyEvent(event);}
        return target.dispatchKeyEvent(event);
    }
    static WmRadioMenu choiceMenu(View view){
        if(view instanceof WmRadioMenu&&((WmRadioMenu)view).choiceKeyboardEnabled())return (WmRadioMenu)view;
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){WmRadioMenu menu=choiceMenu(group.getChildAt(i));if(menu!=null)return menu;}}
        return null;
    }
    void bind(View view){
        clear();if(view==null)return;popup=view;linkMenus(view);view.addOnAttachStateChangeListener(this);
        dropped=view.getWindowToken()!=null;owner.invalidate();
    }
    private void linkMenus(View view){
        // Link even before keyboard opt-in; mode is read from the owner at key delivery.
        if(view instanceof WmRadioMenu){WmRadioMenu menu=(WmRadioMenu)view;menu.choiceOwner=owner;linkedMenus.add(menu);}
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)linkMenus(group.getChildAt(i));}
    }
    boolean isDropped(){return dropped&&popup!=null&&popup.isShown();}
    View popup(){return isDropped()?popup:null;}
    void clear(){if(popup!=null)popup.removeOnAttachStateChangeListener(this);for(WmRadioMenu menu:linkedMenus)if(menu.choiceOwner==owner)menu.choiceOwner=null;linkedMenus.clear();popup=null;dropped=false;owner.invalidate();}
    @Override public void onViewAttachedToWindow(View view){if(view==popup){dropped=true;owner.invalidate();}}
    @Override public void onViewDetachedFromWindow(View view){if(view==popup)clear();}
    static void draw(Canvas canvas,Paint paint,int left,int top,int right,int bottom,boolean pressed){
        if(pressed){left+=2;top+=2;right-=2;bottom-=2;}
        int width=right-left,height=bottom-top,extent=Math.min(width,height),threshold=6*(192+8)/96;
        if(extent<threshold/2)return;
        int rows=extent>threshold?3*(192+16)/96+(extent-threshold-1)/4:2*(192+24)/96;
        int glyphWidth=2*rows-1,x=left+(width-glyphWidth+1)/2,y=top+(height-rows+1)/2;
        paint.setStyle(Paint.Style.FILL);
        for(int j=0;j<rows;j++)canvas.drawRect(x+j,y+j,x+glyphWidth-j,y+j+1,paint);
    }
}
