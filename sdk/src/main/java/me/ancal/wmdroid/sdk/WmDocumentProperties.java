package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

/** DocList Rename/Move dialog13444 fields; owner supplies real destinations and
 * metadata, validates on explicit OK and owns persistence/Cancel. No I/O occurs.
 * Geometry follows the live480-wide Rename/Move capture: heading48, columns
 * x22/x138, shared44 input rows. Widths flex on smaller surfaces; this Android
 * adaptation is not an assumed DLU-to-pixel conversion. Hidden native
 * static1025 and SIPPREF are omitted. Restyling/localizing preserves the draft.
 */
public final class WmDocumentProperties extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener,WmControls.BackHandler {
    public static final int NAME=1015,FOLDER=1016,LOCATION=1018,SIZE=2065,MODIFIED=1024,FORMAT=1019;
    public static final int NAME_LABEL=2060,FOLDER_LABEL=2062,LOCATION_LABEL=2063,SIZE_LABEL=2064,MODIFIED_LABEL=2059,FORMAT_LABEL=2058;
    public static final int NAME_LIMIT=128;
    private Typeface font;
    private String language;
    private int accent,popupGeneration;
    private final EditText name;
    private final WmChoice folder,location;
    private final TextView heading;
    private final TextView[] labels=new TextView[6];
    private View popup;

    public WmDocumentProperties(Context context,Typeface font,Locale locale,String name,String[] folders,int folder,String[] locations,int location,String size,String modified,String format,int accent){
        super(context);
        if(font==null||locale==null||name==null||size==null||modified==null||format==null)throw new IllegalArgumentException("Document appearance and values required");
        checkChoices(folders,folder);checkChoices(locations,location);
        this.font=font;this.accent=accent;setBackgroundColor(Color.WHITE);
        LinearLayout form=new LinearLayout(context);form.setOrientation(LinearLayout.VERTICAL);
        FrameLayout headingBand=new FrameLayout(context);
        heading=WmControls.text(context,font,"");heading.setTextColor(accent);heading.setPadding(20,0,0,0);
        headingBand.addView(heading,new LayoutParams(-1,46));
        View rule=new View(context);rule.setBackgroundColor(Color.BLACK);headingBand.addView(rule,new LayoutParams(-1,2,Gravity.BOTTOM));
        form.addView(headingBand,new LinearLayout.LayoutParams(-1,48));
        this.name=new EditText(context);WmControls.input(this.name,font);this.name.setId(NAME);this.name.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);this.name.setSingleLine(true);
        // Preserve an existing owner's value; limit subsequent input, then
        // validate the complete candidate only when the owner receives OK.
        this.name.setText(name);this.name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(NAME_LIMIT)});
        Locale systemLocale=Locale.getDefault();
        // Source folder0 is the owner-supplied None/root destination; pin it before CBS_SORT.
        this.folder=new WmChoice(context,font,folders,folder,accent);this.folder.setDisplaySort(systemLocale,1);this.folder.setId(FOLDER);this.folder.setEnabled(folders.length>1);
        this.location=new WmChoice(context,font,locations,location,accent);this.location.setDisplaySort(systemLocale,0);this.location.setId(LOCATION);this.location.setEnabled(locations.length>1);
        TextView sizeValue=WmControls.text(context,font,size);sizeValue.setId(SIZE);
        TextView modifiedValue=WmControls.text(context,font,modified);modifiedValue.setId(MODIFIED);
        TextView formatValue=WmControls.text(context,font,format);formatValue.setId(FORMAT);
        View[] fields={this.name,this.folder,this.location,sizeValue,modifiedValue,formatValue};
        int[] ids={NAME_LABEL,FOLDER_LABEL,LOCATION_LABEL,SIZE_LABEL,MODIFIED_LABEL,FORMAT_LABEL};
        for(int i=0;i<fields.length;i++){
            labels[i]=WmControls.text(context,font,"");labels[i].setId(ids[i]);if(Build.VERSION.SDK_INT>=17)labels[i].setLabelFor(fields[i].getId());
            LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(22,0,16,0);
            row.addView(labels[i],new LinearLayout.LayoutParams(0,-2,116));
            row.addView(fields[i],new LinearLayout.LayoutParams(0,i<3?WmControls.ROW_HEIGHT:-2,326));
            row.setMinimumHeight(i<3?WmControls.ROW_HEIGHT:42);
            LinearLayout.LayoutParams fit=new LinearLayout.LayoutParams(-1,-2);fit.topMargin=i==0?18:i<3?4:i==3?22:0;form.addView(row,fit);
        }
        addView(new WmScrollView(context,form),new LayoutParams(-1,-1));setLocale(locale);
        this.folder.setOnOpenListener(this::open);this.location.setOnOpenListener(this::open);
    }
    private static void checkChoices(String[] values,int selected){
        if(values==null||values.length==0)throw new IllegalArgumentException("Real document destinations required");
        if(selected<0||selected>=values.length)throw new IllegalArgumentException("Invalid document destination index");
        for(String value:values)if(value==null)throw new IllegalArgumentException("Null document destination");
    }
    public EditText nameField(){return name;}
    public WmChoice folderField(){return folder;}
    public WmChoice locationField(){return location;}
    public String name(){return name.getText().toString();}
    public int folderIndex(){return folder.selectedIndex();}
    public int locationIndex(){return location.selectedIndex();}
    /** Native File_IsValid plus the edit limit. Owner removes surrounding literal
     * spaces before calling; this method neither trims nor mutates the draft. */
    public static boolean validName(String value){
        if(value==null||value.isEmpty()||value.length()>NAME_LIMIT||value.charAt(0)=='.'||value.charAt(0)==' '||value.charAt(value.length()-1)==' ')return false;
        for(int i=0;i<value.length();i++){char c=value.charAt(i);if((c<=0x20&&c!=' ')||"\"*/:<>?\\|".indexOf(c)>=0)return false;}
        return true;
    }
    public void setLocale(Locale locale){
        if(locale==null)throw new IllegalArgumentException("Locale required");
        onWmLanguageChanged(Build.VERSION.SDK_INT>=21?Modern.tag(locale):locale.toString().replace('_','-'));
    }
    private static final class Modern {static String tag(Locale locale){return locale.toLanguageTag();}}
    @Override public void onWmLanguageChanged(String language){
        if(language==null)throw new IllegalArgumentException("Language required");
        this.language=WmStrings.language(getContext(),language);closePopup();
        WmControls.bindText(heading,this.language,"이름 바꾸기/이동");
        String[] captions={"이름:","폴더:","위치:","크기:","수정한 날짜:","형식:"};
        for(int i=0;i<labels.length;i++)WmControls.bindText(labels[i],this.language,captions[i]);
    }
    private void open(WmChoice source){
        if(!source.isEnabled()||source.displayChoices().length<2||getWidth()<=0||getHeight()<=0)return;closePopup();final int generation=popupGeneration;
        WmRadioMenu menu=new WmRadioMenu(getContext(),font,source.displayChoices(),source.displaySelectedIndex(),WmStrings.text(getContext(),language," 선택됨"),accent,index->{if(generation!=popupGeneration)return;closePopup();source.chooseDisplayIndex(index);});
        menu.enableChoiceKeyboard(source.displaySelectedIndex(),index->{if(generation==popupGeneration)source.chooseDisplayIndex(index);},()->{if(generation==popupGeneration){closePopup();if(source.isShown())source.requestFocus();}});
        menu.enableChoiceCharacterSearch(source.displaySortLocale(),index->{if(generation==popupGeneration&&source.isEnabled())source.chooseCharacterDisplayIndex(index);});
        Rect anchor=new Rect(0,0,source.getWidth(),source.getHeight());offsetDescendantRectToMyCoords(source,anchor);
        int width=Math.min(getWidth(),Math.max(anchor.width(),menu.menuWidth())),desired=menu.menuHeight();
        int below=Math.max(0,getHeight()-anchor.bottom),above=Math.max(0,anchor.top);boolean down=below>=desired||below>=above;
        int height=Math.min(desired,Math.max(1,down?below:above));int top=down?anchor.bottom:Math.max(0,anchor.top-height);
        FrameLayout overlay=new FrameLayout(getContext());overlay.setOnClickListener(v->closePopup());WmScrollView contents=new WmScrollView(getContext(),menu);
        LayoutParams fit=new LayoutParams(width,height);fit.leftMargin=Math.max(0,Math.min(getWidth()-width,anchor.left));fit.topMargin=Math.max(0,Math.min(getHeight()-height,top));overlay.addView(contents,fit);
        popup=overlay;addView(overlay,new LayoutParams(-1,-1));WmChoice.bindDropdown(source,overlay);menu.requestFocus();
    }
    public boolean closePopup(){popupGeneration++;if(popup==null)return false;removeView(popup);popup=null;return true;}
    @Override public boolean back(){return closePopup();}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){closePopup();font=regular;accent=selection;heading.setTextColor(selection);}
    @Override protected void onDetachedFromWindow(){closePopup();super.onDetachedFromWindow();}
}
