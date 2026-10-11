package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/** Shared list plus right-side command column for Today and Start settings.
 * Control relationships follow shellres14421; dimensions retain the current
 * provisional VGA adaptation. At480px the list is296px; extra width goes to
 * the list while commands retain their VGA size and right-edge alignment.
 * The owner supplies scrolling, selection, commands and persistence.
 */
public final class WmListSettingsPane extends LinearLayout {
    public final FrameLayout listFrame;
    public final LinearLayout commands;

    public WmListSettingsPane(Context context,View list,View... buttons){
        super(context);
        if(list==null||list.getParent()!=null)throw new IllegalArgumentException("Unattached list required");
        if(buttons==null)throw new IllegalArgumentException("Command array required");
        for(int i=0;i<buttons.length;i++){
            if(buttons[i]==null||buttons[i]==list||buttons[i].getParent()!=null)throw new IllegalArgumentException("Unattached command required");
            for(int j=0;j<i;j++)if(buttons[i]==buttons[j])throw new IllegalArgumentException("Duplicate command");
        }
        setOrientation(HORIZONTAL);setPadding(16,4,16,4);
        listFrame=new FrameLayout(context);
        GradientDrawable border=new GradientDrawable();border.setColor(0xffffffff);border.setStroke(WmMetrics.BORDER,0xff808080);
        listFrame.setBackgroundDrawable(border);listFrame.setPadding(WmMetrics.BORDER,WmMetrics.BORDER,WmMetrics.BORDER,WmMetrics.BORDER);
        listFrame.addView(list,new FrameLayout.LayoutParams(-1,-1));addView(listFrame,new LayoutParams(0,-1,1));
        commands=new LinearLayout(context);commands.setOrientation(VERTICAL);commands.setPadding(8,0,0,0);
        for(View button:buttons){LayoutParams size=new LayoutParams(144,40);size.bottomMargin=8;commands.addView(button,size);}
        addView(commands,new LayoutParams(152,-1));
    }
}
