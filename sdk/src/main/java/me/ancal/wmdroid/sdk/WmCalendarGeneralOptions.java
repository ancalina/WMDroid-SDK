package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.Calendar;
import java.util.Locale;

/** Calendar property page20004: native field order, IDs and value contracts.
 * Caller owns persistence and immediate application. Native applies on commit;
 * this SDK reports each completed user change. Coordinates use the original
 * template and the observed7/14 DLU profile; narrow widths and oversized font
 * metrics expand rows. No CalendarProvider access occurs here.
 */
public final class WmCalendarGeneralOptions extends FrameLayout implements WmControls.BackHandler,WmControls.RestyleListener,WmControls.LanguageListener {
    public static final int START_VIEW=21163,FIRST_DAY=21161,WEEK_DAYS=21126,HALF_HOUR=21175,WEEK_NUMBERS=21162;
    public static final class Options {
        public final int startView,firstDayOfWeek,weekDays;
        public final boolean halfHour,weekNumbers;
        public Options(int startView,int firstDayOfWeek,int weekDays,boolean halfHour,boolean weekNumbers){
            if(startView<0||startView>4||(firstDayOfWeek!=Calendar.SUNDAY&&firstDayOfWeek!=Calendar.MONDAY)||weekDays<5||weekDays>7)throw new IllegalArgumentException("Invalid Calendar options");
            this.startView=startView;this.firstDayOfWeek=firstDayOfWeek;this.weekDays=weekDays;this.halfHour=halfHour;this.weekNumbers=weekNumbers;
        }
    }
    public interface Listener {void changed(Options options);}
    private Typeface font;
    private String language;
    private int selection,popupGeneration;
    private final Listener listener;
    private final TextView heading,startLabel,firstLabel,weekLabel;
    private final WmChoice start,first,week;
    private final CheckBox halfHour,weekNumbers;
    private final WmCalendarOptionsLayout form;
    private View popup;
    private boolean updating;

    public WmCalendarGeneralOptions(Context context,Typeface font,Locale locale,Options options,int selection,Listener listener){
        this(context,font,font,locale,options,selection,listener);
    }
    public WmCalendarGeneralOptions(Context context,Typeface font,Typeface bold,Locale locale,Options options,int selection,Listener listener){
        super(context);if(font==null||bold==null||locale==null||options==null)throw new IllegalArgumentException("Calendar appearance and options required");
        this.font=font;this.selection=selection;this.listener=listener;setBackgroundColor(Color.WHITE);
        form=new WmCalendarOptionsLayout(context);
        heading=WmControls.text(context,font,"");heading.setId(21002);heading.setTextColor(WmPalette.calendarHeading(selection));form.field(heading,0,14,10,490,38,true,false);
        startLabel=WmControls.text(context,font,"");firstLabel=WmControls.text(context,font,"");weekLabel=WmControls.text(context,font,"");startLabel.setId(32851);firstLabel.setId(32852);weekLabel.setId(32853);
        start=new WmChoice(context,font,new String[]{""},0,selection);start.setId(START_VIEW);
        first=new WmChoice(context,font,new String[]{""},0,selection);first.setId(FIRST_DAY);
        week=new WmChoice(context,font,new String[]{""},0,selection);week.setId(WEEK_DAYS);
        form.field(startLabel,1,16,66,192,38,false,false);form.field(start,1,212,58,248,34,true,true);
        form.field(firstLabel,2,16,118,192,38,false,false);form.field(first,2,212,112,248,34,true,true);
        form.field(weekLabel,3,16,170,188,38,false,false);form.field(week,3,212,164,248,34,true,true);
        halfHour=new CheckBox(context);WmControls.check(halfHour,font);halfHour.setId(HALF_HOUR);form.field(halfHour,4,16,224,436,34,true,false);
        weekNumbers=new CheckBox(context);WmControls.check(weekNumbers,font);weekNumbers.setId(WEEK_NUMBERS);form.field(weekNumbers,5,16,262,436,34,true,false);
        addView(new WmScrollView(context,form),new LayoutParams(-1,-1));
        // Set all labels/values before installing user-change listeners.
        form.fonts(font,bold);setLocale(locale);setOptions(options);
        for(WmChoice choice:new WmChoice[]{start,first,week}){choice.setOnOpenListener(this::open);choice.setOnChoiceListener(index->changed());}
        halfHour.setOnCheckedChangeListener((button,on)->changed());weekNumbers.setOnCheckedChangeListener((button,on)->changed());
    }
    public Options options(){return new Options(start.selectedIndex(),first.selectedIndex()==0?Calendar.SUNDAY:Calendar.MONDAY,week.selectedIndex()+5,halfHour.isChecked(),weekNumbers.isChecked());}
    /** Silent model update; user callbacks never fire while restoring values. */
    public void setOptions(Options options){
        if(options==null)throw new IllegalArgumentException("Options required");closePopup();updating=true;
        try{start.setSelectedIndex(options.startView);first.setSelectedIndex(options.firstDayOfWeek==Calendar.SUNDAY?0:1);week.setSelectedIndex(options.weekDays-5);halfHour.setChecked(options.halfHour);weekNumbers.setChecked(options.weekNumbers);}finally{updating=false;}
    }
    public void setLocale(Locale locale){
        if(locale==null)throw new IllegalArgumentException("Locale required");closePopup();form.fontLocale(locale);
        language=WmStrings.language(getContext(),android.os.Build.VERSION.SDK_INT>=21?Modern.tag(locale):locale.toString().replace('_','-'));
        form.generalColumn("ko".equals(locale.getLanguage()));
        WmControls.bindText(heading,language,"옵션");WmControls.bindText(startLabel,language,"시작 위치:");WmControls.bindText(firstLabel,language,"시작 요일:");WmControls.bindText(weekLabel,language,"주 보기:");
        start.setChoices(new String[]{text("일정 보기"),WmStrings.text(getContext(),language,"일 보기","calendar.options.start.1"),WmStrings.text(getContext(),language,"주 보기","calendar.options.start.2"),WmStrings.text(getContext(),language,"월 보기","calendar.options.start.3"),text("연도 보기")},start.selectedIndex());
        String[] weekdays=new java.text.DateFormatSymbols(locale).getWeekdays();first.setChoices(new String[]{weekdays[Calendar.SUNDAY],weekdays[Calendar.MONDAY]},first.selectedIndex());
        week.setChoices(new String[]{text("주 5일"),text("주 6일"),text("주 7일")},week.selectedIndex());
        WmControls.bindText(halfHour,language,"30분 단위로 표시");WmControls.bindText(weekNumbers,language,"몇째 주인지 표시");
    }
    private static final class Modern {static String tag(Locale locale){return locale.toLanguageTag();}}
    @Override public void onWmLanguageChanged(String language){setLocale(WmStrings.locale(WmStrings.language(getContext(),language)));}
    private String text(String authored){return WmStrings.text(getContext(),language,authored);}
    private void changed(){if(!updating&&listener!=null)listener.changed(options());}
    private void open(WmChoice source){
        if(!source.isEnabled()||getWidth()<=0||getHeight()<=0)return;closePopup();final int generation=popupGeneration;
        WmRadioMenu menu=new WmRadioMenu(getContext(),font,source.choices(),source.selectedIndex(),text(" 선택됨"),selection,index->{if(generation!=popupGeneration)return;closePopup();source.choose(index);});
        menu.enableChoiceKeyboard(source.selectedIndex(),index->{if(generation==popupGeneration)source.choose(index);},()->{if(generation==popupGeneration){closePopup();if(source.isShown())source.requestFocus();}});
        menu.enableChoiceCharacterSearch(source.displaySortLocale(),index->{if(generation==popupGeneration&&source.isEnabled())source.chooseCharacterDisplayIndex(index);});
        Rect anchor=new Rect(0,0,source.getWidth(),source.getHeight());offsetDescendantRectToMyCoords(source,anchor);
        // shortcut: stock7/14 DLU creation and later RCML font profile; remeasure for other native system fonts.
        int width=Math.min(getWidth(),Math.max(anchor.width(),menu.menuWidth())),desired=menu.choiceDropdownHeight(222);
        int below=Math.max(0,getHeight()-anchor.bottom),above=Math.max(0,anchor.top);boolean down=below>=desired||below>=above;
        int height=Math.min(desired,Math.max(1,down?below:above));int top=down?anchor.bottom:Math.max(0,anchor.top-height);
        FrameLayout overlay=new FrameLayout(getContext());overlay.setOnClickListener(v->closePopup());WmScrollView contents=new WmScrollView(getContext(),menu);
        LayoutParams fit=new LayoutParams(width,height);fit.leftMargin=Math.max(0,Math.min(getWidth()-width,anchor.left));fit.topMargin=Math.max(0,Math.min(getHeight()-height,top));overlay.addView(contents,fit);
        popup=overlay;addView(overlay,new LayoutParams(-1,-1));WmChoice.bindDropdown(source,overlay);menu.requestFocus();
    }
    public boolean closePopup(){popupGeneration++;if(popup==null)return false;removeView(popup);popup=null;return true;}
    @Override public boolean back(){return closePopup();}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){closePopup();font=regular;form.fonts(regular,bold==null?regular:bold);this.selection=selection;heading.setTextColor(WmPalette.calendarHeading(selection));}
    @Override protected void onDetachedFromWindow(){closePopup();super.onDetachedFromWindow();}
}
