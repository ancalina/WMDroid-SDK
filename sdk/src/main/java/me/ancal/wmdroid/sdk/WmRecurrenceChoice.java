package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.FrameLayout;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/** Native appointment recurrence combo: once, weekly, monthly, yearly, edit.
 * Native sources: pimdlg027D50AC/027D8418/027D8578/027D86D0/027D87BC/027D9618.
 * Caller owns popup placement and wizard lifetime; commits must call choose().
 * Native closed-field case540999780 uses 027D8578; index4 loads resource4002,
 * the edit label. 027D8418 recognizes start-matching no-end presets, else index4.
 * English wording is authored; Korean labels are ROM-backed.
 * Geometry is inherited from WmChoice and remains provisional DLU conversion. */
public final class WmRecurrenceChoice extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener {
    public interface OnRuleChangedListener {void changed(WmRecurrenceRule rule);}
    private final WmChoice choice;
    private Locale locale;
    private String language;
    private WmDateTimeField.Value start;
    private WmRecurrenceRule rule;
    private OnRuleChangedListener changed;
    private Runnable edit;

    public WmRecurrenceChoice(Context context,Typeface font,Locale locale,
                              WmDateTimeField.Value start,WmRecurrenceRule rule,int accent){
        super(requiredContext(context));
        if(context==null||font==null||locale==null||start==null||rule==null)
            throw new IllegalArgumentException("Context, font, locale, start and rule required");
        this.locale=locale;this.language=language(locale);this.start=start;this.rule=rule;
        choice=new WmChoice(context,font,labels(),presetIndex(),accent);
        addView(choice,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        choice.setOnChoiceListener(this::choose);
    }
    public WmChoice choice(){return choice;}
    public WmRecurrenceRule rule(){return rule;}
    /** Last start supplied by the owner; an invalid time draft can defer its update. */
    public WmDateTimeField.Value startValue(){return start;}
    public void setRule(WmRecurrenceRule rule){
        if(rule==null)throw new IllegalArgumentException("Rule required");
        this.rule=rule;refresh();
    }
    /** Changes start only, retaining rule without notification; recomputes presets.
     * A rule not matching the new start preset displays Edit(index4); null is invalid. */
    public void setStart(WmDateTimeField.Value start){
        if(start==null)throw new IllegalArgumentException("Start required");
        this.start=start;refresh();
    }
    public void setLocale(Locale locale){
        if(locale==null)throw new IllegalArgumentException("Locale required");
        this.locale=locale;this.language=language(locale);refresh();
    }
    private String language(Locale locale){return WmStrings.language(getContext(),android.os.Build.VERSION.SDK_INT>=21?Modern.tag(locale):locale.toString().replace('_','-'));}
    private static final class Modern {static String tag(Locale locale){return locale.toLanguageTag();}}
    @Override public void onWmLanguageChanged(String language){setLocale(WmStrings.locale(WmStrings.language(getContext(),language)));}
    public void setOnRuleChanged(OnRuleChangedListener listener){changed=listener;}
    public void setOnEdit(Runnable callback){edit=callback;}
    private static Context requiredContext(Context context){
        if(context==null)throw new IllegalArgumentException("Context required");return context;
    }
    /** A repeated edit selection still opens the wizard; edit never changes rule. */
    public void choose(int index){
        if(index<0||index>4)throw new IndexOutOfBoundsException("Invalid recurrence choice index");
        if(!isEnabled()||!choice.isEnabled())return;
        if(index==4){refresh();if(edit!=null)edit.run();return;}
        WmRecurrenceRule next=preset(index);
        boolean different=!rule.equals(next);rule=next;refresh();
        if(different&&changed!=null)changed.changed(next);
    }
    private WmRecurrenceRule preset(int index){
        switch(index){
            case 0:return WmRecurrenceRule.none();
            case 1:return WmRecurrenceRule.weekly(1,1<<weekday());
            case 2:return WmRecurrenceRule.monthlyDay(1,start.day);
            case 3:return WmRecurrenceRule.yearlyDay(start.month,start.day);
            default:throw new IndexOutOfBoundsException("Invalid recurrence preset index");
        }
    }
    private int presetIndex(){
        for(int index=0;index<4;index++)if(rule.equals(preset(index)))return index;
        return 4;
    }
    private int weekday(){
        GregorianCalendar date=new GregorianCalendar(TimeZone.getTimeZone("UTC"),Locale.US);
        date.setGregorianChange(new Date(Long.MIN_VALUE));date.setLenient(false);date.clear();
        date.set(start.year,start.month-1,start.day);
        return date.get(Calendar.DAY_OF_WEEK)-Calendar.SUNDAY;
    }
    private String[] labels(){
        DateFormatSymbols symbols=new DateFormatSymbols(locale);
        String month=symbols.getMonths()[start.month-1];
        String[] weekdays={"일요일","월요일","화요일","수요일","목요일","금요일","토요일"};
        String weekly="매주 "+weekdays[weekday()];
        return new String[]{text("한 번"),text(weekly),
            text("매월 %1!d!일").replace("%1!d!",Integer.toString(start.day)),
            text("%1 %2!d!일마다").replace("%2!d!",Integer.toString(start.day)).replace("%1",month),
            text("< 유형 편집... >")};
    }
    private String text(String authored){return WmStrings.text(getContext(),language,authored);}
    private void refresh(){choice.setChoices(labels(),presetIndex());requestLayout();}
    @Override public void setEnabled(boolean enabled){super.setEnabled(enabled);if(choice!=null)choice.setEnabled(enabled);}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        choice.onWmRestyle(regular,bold,selection,tabColor);requestLayout();
    }
}
