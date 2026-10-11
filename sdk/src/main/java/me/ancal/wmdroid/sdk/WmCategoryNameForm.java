package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

/** PIMUTIL New Category client; observed WM6.1.4 VGA frame and divider.
 * Construct/change on the UI thread. Use localizeTree/restyleTree for the complete form;
 * onWmRestyle alone updates heading/label fonts and heading color, not input styling.
 * Owner retains its app title and supplies Done/Cancel, validation and persistence.
 * Exact GDI raster and Korean/OEM geometry are not established by the USA capture. */
public final class WmCategoryNameForm extends FrameLayout implements WmControls.LanguageListener,WmControls.RestyleListener,WmControls.BackHandler {
    public static final int HEADING=32850,LABEL=25960,NAME=24033,NAME_LIMIT=255;
    private final TextView heading,label;
    private final EditText name;
    private Typeface regular,bold;
    private int accent;
    private String language;
    private WmMessageBox warning;
    public WmCategoryNameForm(Context context,Typeface regular,Typeface bold,int accent){
        super(context);if(regular==null||bold==null)throw new IllegalArgumentException("Category fonts required");
        this.regular=regular;this.bold=bold;this.accent=accent;setBackgroundColor(Color.WHITE);setSoundEffectsEnabled(false);
        heading=WmControls.text(context,bold,"");heading.setId(HEADING);WmControls.lockFont(heading);place(heading,-1,36,8,10,52);
        View divider=new View(context);divider.setBackgroundColor(Color.BLACK);place(divider,-1,2,0,50,0);
        label=WmControls.text(context,bold,"");label.setId(LABEL);WmControls.lockFont(label);label.setTextSize(TypedValue.COMPLEX_UNIT_PX,26);place(label,120,40,8,72,0);
        name=new EditText(context);name.setId(NAME);WmControls.input(name,regular);name.setTextSize(TypedValue.COMPLEX_UNIT_PX,26);name.setSingleLine(true);name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(NAME_LIMIT)});place(name,-1,40,132,72,8);
        heading.setTextColor(accent|0xff000000);onWmLanguageChanged(null);
    }
    private void place(View view,int width,int height,int left,int top,int right){LayoutParams fit=new LayoutParams(width,height);fit.leftMargin=left;fit.topMargin=top;fit.rightMargin=right;addView(view,fit);}
    public EditText nameField(){return name;}
    /** Original PIMUTIL403/404 warning; the owner still validates and stores names. */
    public void showNameWarning(Runnable acknowledged){
        if(warning!=null)return;
        final int start=name.getSelectionStart(),end=name.getSelectionEnd();
        InputMethodManager keyboard=(InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.hideSoftInputFromWindow(name.getWindowToken(),0);
        name.setEnabled(false);
        warning=new WmMessageBox(getContext(),regular,WmStrings.locale(WmStrings.language(getContext(),language)),"범주","범주 이름에는 쉼표(,) 또는 세미콜론(;)을 포함할 수 없습니다.",accent,()->{
            WmMessageBox closed=warning;warning=null;removeView(closed);name.setEnabled(true);name.requestFocus();if(start>=0&&end>=0)name.setSelection(Math.min(start,name.length()),Math.min(end,name.length()));if(acknowledged!=null)acknowledged.run();
        });
        warning.setCaptionContext("category.warning.caption");warning.onWmRestyle(regular,bold,accent,0);addView(warning,new LayoutParams(-1,-1));
        // Modal focus can collapse EditText selection before the owner saves its draft.
        if(start>=0&&end>=0)name.setSelection(Math.min(start,name.length()),Math.min(end,name.length()));
    }
    @Override public boolean back(){return warning!=null&&warning.back();}
    @Override public void onWmLanguageChanged(String language){this.language=language;WmControls.bindText(heading,language,"새 범주");WmControls.bindText(label,language,"범주:","category.name.caption");WmControls.bindDescription(name,language,"범주");}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){this.regular=regular;this.bold=bold==null?regular:bold;accent=selection;for(TextView view:new TextView[]{heading,label}){view.setTypeface(this.bold);WmFonts.attach(view,this.bold);}heading.setTextColor(selection|0xff000000);}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(warning==null)name.requestFocus();name.post(()->{if(warning==null&&name.isShown()){InputMethodManager keyboard=(InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.showSoftInput(name,InputMethodManager.SHOW_IMPLICIT);}});}
    @Override protected void onDetachedFromWindow(){if(warning!=null){removeView(warning);warning=null;name.setEnabled(true);}InputMethodManager keyboard=(InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.hideSoftInputFromWindow(name.getWindowToken(),0);super.onDetachedFromWindow();}
}
