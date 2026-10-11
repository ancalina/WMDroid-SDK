package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

/** Supported Screen controls from shellres dialog14399; caller owns persistence.
 * Shared row metrics adapt the template; this is not the full Sounds CPL. */
public final class WmTouchSoundOptions extends LinearLayout {
    public interface Listener {void changed(boolean enabled,int level);}
    private final CheckBox enabled;
    private final RadioButton soft,loud;
    public WmTouchSoundOptions(Context context,Typeface font,String language,boolean on,int level,Listener listener){
        super(context);if(level<1||level>2||listener==null)throw new IllegalArgumentException("Level1..2 and listener required");
        setOrientation(VERTICAL);setPadding(WmMetrics.INSET,WmMetrics.INSET,WmMetrics.INSET,0);
        android.widget.TextView label=WmControls.text(context,font,"");WmControls.bindText(label,language,"다음 경우에 소리 사용");addView(label,new LayoutParams(-1,WmControls.ROW_HEIGHT));
        enabled=new CheckBox(context);enabled.setId(653);WmControls.check(enabled,font);WmControls.bindText(enabled,language,"화면 누르기");enabled.setChecked(on);addView(enabled,new LayoutParams(-1,WmControls.ROW_HEIGHT));
        RadioGroup choices=new RadioGroup(context);choices.setOrientation(HORIZONTAL);choices.setPadding(36,0,0,0);
        soft=radio(context,font,language,"작게",655);loud=radio(context,font,language,"크게",654);
        choices.addView(soft,new RadioGroup.LayoutParams(140,WmControls.ROW_HEIGHT));choices.addView(loud,new RadioGroup.LayoutParams(140,WmControls.ROW_HEIGHT));
        choices.check(level==1?655:654);soft.setEnabled(on);loud.setEnabled(on);addView(choices,new LayoutParams(-1,WmControls.ROW_HEIGHT));
        enabled.setOnCheckedChangeListener((view,value)->{soft.setEnabled(value);loud.setEnabled(value);listener.changed(value,soft.isChecked()?1:2);});
        choices.setOnCheckedChangeListener((group,id)->listener.changed(enabled.isChecked(),id==655?1:2));
    }
    private static RadioButton radio(Context c,Typeface font,String language,String text,int id){RadioButton view=new RadioButton(c);view.setId(id);WmControls.radio(view,font);WmControls.bindText(view,language,text);return view;}
}
