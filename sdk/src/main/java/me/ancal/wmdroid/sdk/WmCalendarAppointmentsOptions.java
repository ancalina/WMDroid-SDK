package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.HashSet;
import java.util.Locale;

/** Calendar property page20005. Native IDs, quantity0..99, unit multipliers
 * and six display bits are preserved. The native7/14 DLU profile supplies the
 * default coordinates and42px icon cells; narrow widths and large font metrics
 * expand rows. Caller owns persistence, mail accounts and reminder scheduling.
 * This view never accesses CalendarProvider or sends meeting requests. */
public final class WmCalendarAppointmentsOptions extends FrameLayout implements WmControls.BackHandler,WmControls.RestyleListener,WmControls.LanguageListener {
    public static final int REMINDER_ENABLED=24020,REMINDER_QUANTITY=24021,REMINDER_UNIT=24022,DISPLAY_ICONS=25010,MAIL_ACCOUNT=21165;
    public static final int MINUTES=0,HOURS=1,DAYS=2,WEEKS=3;
    private static final int[] MULTIPLIERS={1,60,1440,10080};
    private static final String[] QUANTITIES={"1","5","10","15","30","45"};
    public static final class Options {
        public final boolean enabled;
        public final int quantity,unit,displayMask;
        public Options(boolean enabled,int quantity,int unit,int displayMask){
            if(quantity<0||quantity>99||unit<0||unit>3||displayMask<0||displayMask>63)throw new IllegalArgumentException("Invalid appointment options");
            this.enabled=enabled;this.quantity=quantity;this.unit=unit;this.displayMask=displayMask;
        }
        /** Quantity converted to minutes; enabled is a separate policy flag. */
        public int minutes(){return quantity*MULTIPLIERS[unit];}
    }
    /** Synchronous notification of changed valid settings; identical settings are
     * deduplicated before callback. Exceptions do not roll back values/baseline.
     * Owner applies and persists; setOptions restores silently. */
    public interface Listener {void changed(Options options);}
    public interface OnAccountListener {void chosen(String id);}
    private Typeface font;
    private final WmCalendarOptionsLayout form;
    private String language;
    private int selection,popupGeneration,quantityValue,displayMask;
    private final Listener listener;
    private final TextView heading,iconsLabel,accountLabel;
    private final CheckBox enabled;
    private final WmEditableChoice quantity;
    private final WmChoice unit,account;
    private final IconBox[] icons=new IconBox[6];
    private String[] accountIds=new String[0],accountLabels=new String[0];
    private String accountId="";
    private OnAccountListener accountListener;
    private Options lastNotified;
    private View popup;
    private boolean updating;

    public WmCalendarAppointmentsOptions(Context context,Typeface font,Locale locale,Options options,int selection,Listener listener){
        this(context,font,font,locale,options,selection,listener);
    }
    public WmCalendarAppointmentsOptions(Context context,Typeface font,Typeface bold,Locale locale,Options options,int selection,Listener listener){
        super(context);if(font==null||bold==null||locale==null||options==null)throw new IllegalArgumentException("Appointment appearance and options required");
        this.font=font;this.selection=selection;this.listener=listener;setBackgroundColor(Color.WHITE);
        form=new WmCalendarOptionsLayout(context);
        heading=WmControls.text(context,font,"");heading.setId(21002);heading.setTextColor(WmPalette.calendarHeading(selection));form.field(heading,0,14,10,490,38,true,false);
        enabled=new CheckBox(context);WmControls.check(enabled,font);enabled.setId(REMINDER_ENABLED);form.field(enabled,1,16,66,436,34,true,false);
        quantity=new WmEditableChoice(context,font,QUANTITIES,"0",selection);quantity.setId(REMINDER_QUANTITY);quantity.setInputType(InputType.TYPE_CLASS_NUMBER);quantity.setKeyListener(DigitsKeyListener.getInstance("0123456789"));quantity.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        // Native7/14 DLU profile: floor(73*14/8)*2 minus initial40px caption and4px list frame.
        // shortcut: stock-font creation profile; remeasure for other native system-font profiles.
        quantity.setClosedDropdownClientHeight(210);
        unit=new WmChoice(context,font,new String[]{"","","",""},0,selection);unit.setId(REMINDER_UNIT);
        form.field(quantity,2,52,114,146,34,false,true);form.field(unit,2,210,114,252,34,true,true);
        iconsLabel=WmControls.text(context,font,"");iconsLabel.setId(32853);form.field(iconsLabel,3,16,182,188,38,false,false);
        SymbolRow iconRow=new SymbolRow(context);iconRow.setId(DISPLAY_ICONS);
        for(int i=0;i<icons.length;i++){final int index=i;IconBox box=new IconBox(context,new WmCalendarSymbols(context,i));icons[i]=box;WmControls.textStyle(box,font);WmControls.entry(box,selection);box.setOnClickListener(v->{displayMask^=1<<index;refreshIcons();changed();});iconRow.addView(box,new ViewGroup.LayoutParams(42,42));}
        form.field(iconRow,3,210,174,252,42,true,false);
        accountLabel=WmControls.text(context,font,"");accountLabel.setId(32854);form.field(accountLabel,4,16,248,188,76,false,false);
        account=new WmChoice(context,font,new String[]{""},0,selection);account.setId(MAIL_ACCOUNT);form.field(account,4,210,258,252,34,true,true);
        addView(new WmScrollView(context,form),new LayoutParams(-1,-1));form.fonts(font,bold);setLocale(locale);setOptions(options);
        enabled.setOnCheckedChangeListener((button,on)->{quantity.setEnabled(on);unit.setEnabled(on);if(!on)closePopup();changed();});
        quantity.setOnOpenListener(this::openQuantity);quantity.setOnValueChangedListener(value->{if(value.length()==0)return;quantityValue=Integer.parseInt(value);changed();});
        quantity.setOnFocusChangeListener((view,focused)->{if(!focused&&quantity.value().length()==0)quantity.setValue(String.valueOf(quantityValue));});
        unit.setOnOpenListener(this::openChoice);unit.setOnChoiceListener(index->changed());
        account.setOnOpenListener(this::openChoice);account.setOnChoiceListener(index->{String id=accountIds[index];if(id.equals(accountId))return;accountId=id;if(accountListener!=null)accountListener.chosen(id);});
    }
    public Options options(){return new Options(enabled.isChecked(),quantityValue,unit.selectedIndex(),displayMask);}
    public void setOptions(Options options){
        if(options==null)throw new IllegalArgumentException("Options required");closePopup();updating=true;
        try{quantityValue=options.quantity;displayMask=options.displayMask;quantity.setValue(String.valueOf(quantityValue));unit.setSelectedIndex(options.unit);enabled.setChecked(options.enabled);quantity.setEnabled(options.enabled);unit.setEnabled(options.enabled);refreshIcons();lastNotified=options;}finally{updating=false;}
    }
    private void changed(){
        if(updating)return;Options next=options();if(lastNotified!=null&&lastNotified.enabled==next.enabled&&lastNotified.quantity==next.quantity&&lastNotified.unit==next.unit&&lastNotified.displayMask==next.displayMask)return;
        lastNotified=next;if(listener!=null)listener.changed(next);
    }
    private void refreshIcons(){for(int i=0;i<icons.length;i++)icons[i].setSelected((displayMask&(1<<i))!=0);}
    public String accountId(){return accountId;}
    /** Labels remain caller data. Empty arrays require selectedId=""; nonempty
     * arrays require a selectedId matching exactly one unique, nonempty ID. */
    public void setAccounts(String[] ids,String[] labels,String selectedId,OnAccountListener listener){
        if(ids==null||labels==null||selectedId==null||ids.length!=labels.length)throw new IllegalArgumentException("Account IDs and labels required");
        String[] idCopy=ids.clone(),labelCopy=labels.clone();HashSet<String> unique=new HashSet<String>();int selected=-1;
        for(int i=0;i<idCopy.length;i++){if(idCopy[i]==null||idCopy[i].length()==0||labelCopy[i]==null||!unique.add(idCopy[i]))throw new IllegalArgumentException("Invalid account IDs or labels");if(idCopy[i].equals(selectedId))selected=i;}
        if(idCopy.length==0?selectedId.length()!=0:selected<0)throw new IllegalArgumentException("Unknown selected account ID");
        closePopup();accountIds=idCopy;accountLabels=labelCopy;accountId=selectedId;accountListener=listener;
        account.setChoices(idCopy.length==0?new String[]{emptyAccountLabel()}:labelCopy,idCopy.length==0?0:selected);account.setEnabled(idCopy.length!=0);
    }
    private String emptyAccountLabel(){return text("사용 가능한 계정 없음");}
    public void setLocale(Locale locale){
        if(locale==null)throw new IllegalArgumentException("Locale required");closePopup();form.fontLocale(locale);
        language=WmStrings.language(getContext(),android.os.Build.VERSION.SDK_INT>=21?Modern.tag(locale):locale.toString().replace('_','-'));
        WmControls.bindText(heading,language,"옵션");WmControls.bindText(enabled,language,"새 항목에 대해 미리 알림 설정");
        String[] units={"분","시간","일","주"};for(int i=0;i<units.length;i++)units[i]=WmStrings.text(getContext(),language,units[i],"calendar.reminder.unit."+i);unit.setChoices(units,unit.selectedIndex());
        WmControls.bindText(iconsLabel,language,"아이콘 표시:");WmControls.bindText(accountLabel,language,"다음을 통해 모임 요청 보내기:");
        String[] names={"미리 알림","되풀이","메모","장소","참석자","비공개"};for(int i=0;i<icons.length;i++)WmControls.bindDescription(icons[i],language,names[i]);
        if(accountIds.length==0){account.setChoices(new String[]{emptyAccountLabel()},0);account.setEnabled(false);}
    }
    private static final class Modern {static String tag(Locale locale){return locale.toLanguageTag();}}
    @Override public void onWmLanguageChanged(String language){setLocale(WmStrings.locale(WmStrings.language(getContext(),language)));}
    private String text(String authored){return WmStrings.text(getContext(),language,authored);}
    private String selectedSuffix(){return text(" 선택됨");}
    private void openQuantity(WmEditableChoice source){
        if(!source.isEnabled())return;String[] labels=source.candidates();int selected=source.dropdownSelectedIndex();
        openMenu(source,labels,selected,index->source.choose(index));
    }
    private void openChoice(WmChoice source){if(source.isEnabled())openMenu(source,source.displayChoices(),source.displaySelectedIndex(),index->source.chooseDisplayIndex(index));}
    private interface Choose {void choose(int index);}
    private void openMenu(View source,String[] labels,int selected,Choose action){
        if(getWidth()<=0||getHeight()<=0)return;closePopup();final int generation=popupGeneration;
        WmRadioMenu menu=new WmRadioMenu(getContext(),font,labels,selected,selectedSuffix(),selection,index->{if(generation!=popupGeneration||!source.isEnabled())return;closePopup();action.choose(index);});
        menu.enableChoiceKeyboard(selected,index->{if(generation==popupGeneration&&source.isEnabled())action.choose(index);},()->{if(generation==popupGeneration){closePopup();if(source.isShown()&&source.isEnabled())source.requestFocus();}});
        if(source instanceof WmChoice){WmChoice field=(WmChoice)source;menu.enableChoiceCharacterSearch(field.displaySortLocale(),index->{if(generation==popupGeneration&&field.isEnabled())field.chooseCharacterDisplayIndex(index);});}
        Rect anchor=new Rect(0,0,source.getWidth(),source.getHeight());offsetDescendantRectToMyCoords(source,anchor);
        int width=Math.min(getWidth(),Math.max(anchor.width(),menu.menuWidth())),desired=menu.menuHeight();
        if(source instanceof WmEditableChoice&&labels.length>0){int client=((WmEditableChoice)source).getClosedDropdownClientHeight();if(client>=0)desired=menu.choiceDropdownHeight(client);}
        // shortcut: stock7/14 DLU creation and later RCML font profile; remeasure for other native system fonts.
        else if(labels.length>0)desired=menu.choiceDropdownHeight(source==unit?214:166);
        int below=Math.max(0,getHeight()-anchor.bottom),above=Math.max(0,anchor.top);boolean down=below>=desired||below>=above;
        int height=Math.min(desired,Math.max(1,down?below:above)),top=down?anchor.bottom:Math.max(0,anchor.top-height);
        FrameLayout overlay=new FrameLayout(getContext());overlay.setOnClickListener(v->closePopup());WmScrollView contents=new WmScrollView(getContext(),menu);
        LayoutParams fit=new LayoutParams(width,height);fit.leftMargin=Math.max(0,Math.min(getWidth()-width,anchor.left));fit.topMargin=Math.max(0,Math.min(getHeight()-height,top));overlay.addView(contents,fit);popup=overlay;addView(overlay,new LayoutParams(-1,-1));WmChoice.bindDropdown(source,overlay);menu.requestFocus();
    }
    public boolean closePopup(){popupGeneration++;if(popup==null)return false;removeView(popup);popup=null;return true;}
    @Override public boolean back(){return closePopup();}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){closePopup();font=regular;form.fonts(regular,bold==null?regular:bold);this.selection=selection;heading.setTextColor(WmPalette.calendarHeading(selection));}
    @Override protected void onDetachedFromWindow(){closePopup();super.onDetachedFromWindow();}
    /** Preserve44px hit targets and source aspect; narrow callers get more rows. */
    private static final class SymbolRow extends ViewGroup {
        private int columns=6;
        SymbolRow(Context context){super(context);}
        @Override protected void onMeasure(int widthSpec,int heightSpec){
            int count=getChildCount(),cell=42;
            int width=MeasureSpec.getMode(widthSpec)==MeasureSpec.UNSPECIFIED?count*cell:MeasureSpec.getSize(widthSpec);
            columns=Math.max(1,Math.min(Math.max(1,count),width/cell));
            int exact=MeasureSpec.makeMeasureSpec(cell,MeasureSpec.EXACTLY);
            for(int i=0;i<count;i++)getChildAt(i).measure(exact,exact);
            setMeasuredDimension(resolveSize(width,widthSpec),resolveSize((count+columns-1)/columns*cell,heightSpec));
        }
        @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
            int cell=42;for(int i=0;i<getChildCount();i++){int x=i%columns*cell,y=i/columns*cell;getChildAt(i).layout(x,y,x+cell,y+cell);}
        }
    }
    private static final class IconBox extends TextView {
        private final Drawable symbol;
        IconBox(Context context,Drawable symbol){super(context);this.symbol=symbol;setGravity(Gravity.CENTER);setSoundEffectsEnabled(false);setFocusable(true);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);int width=Math.min(getWidth(),symbol.getIntrinsicWidth()),height=Math.min(getHeight(),symbol.getIntrinsicHeight());int x=(getWidth()-width)/2,y=(getHeight()-height)/2;symbol.setBounds(x,y,x+width,y+height);symbol.draw(canvas);}
    }
}
