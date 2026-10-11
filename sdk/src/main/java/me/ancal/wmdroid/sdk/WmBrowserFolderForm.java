package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** browsres4168 name12339/label12356; field rectangle measured in WM6.1.4. */
public final class WmBrowserFolderForm extends LinearLayout implements WmControls.LanguageListener {
    public static final int NAME=12339,NAME_LABEL=12356;
    private final TextView label;
    private final EditText name;
    public WmBrowserFolderForm(Context context,Typeface font){
        this(context,font,"");
    }
    public WmBrowserFolderForm(Context context,Typeface font,String draft){
        super(context);if(font==null||draft==null)throw new IllegalArgumentException("Folder font/draft required");
        setOrientation(VERTICAL);setPadding(12,12,12,12);setBackgroundColor(WmControls.SURFACE);
        label=WmControls.text(context,font,"");label.setId(NAME_LABEL);addView(label,new LayoutParams(-1,42));
        name=new EditText(context);WmControls.input(name,font);name.setId(NAME);name.setSingleLine(true);
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(WmDocumentProperties.NAME_LIMIT)});name.setText(draft);addView(name,new LayoutParams(-1,42));
        onWmLanguageChanged(null);
    }
    public EditText nameField(){return name;}
    @Override public void onWmLanguageChanged(String language){WmControls.bindText(label,language,"이름:");}
}
