package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

/** Shared editor for Calendar command69 / dialog20037 / edit21212.
 * The native four-character and1900..2999 rules are preserved. This embedded
 * control's placement and inline error are adaptations; DLU pixels are unmapped.
 */
public final class WmYearChooser extends WmDialog implements WmControls.RestyleListener,WmControls.BackHandler,WmControls.LanguageListener {
    /** At most one completion callback per instance; owner removes the View. */
    public interface Listener {
        /** Valid submit, synchronous after marking complete and hiding IME; never cancellation. */
        void chosen(int year);
        /** Cancel/back before completion, synchronous; never after successful submit. */
        void cancel();
    }
    public static final int MIN_YEAR=1900, MAX_YEAR=2999;
    private final Listener listener;
    private final EditText yearInput;
    private final TextView title,yearLabel,error,ok,cancel;
    private String language;
    private boolean completed;
    private final Runnable showKeyboard=this::showInputKeyboard;
    private void showInputKeyboard(){
        if(getWindowToken()!=null&&yearInput.hasFocus()){
            InputMethodManager keyboard=(InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if(keyboard!=null)keyboard.showSoftInput(yearInput,InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public WmYearChooser(Context context,Typeface font,Locale locale,int year,int selection,Listener listener){
        super(context);checkYear(year);if(locale==null||listener==null)throw new IllegalArgumentException("locale and year listener required");
        this.language=language(context,locale);this.listener=listener;setOrientation(VERTICAL);setPadding(8,6,8,6);WmControls.faceBackground(this,selection);
        title=WmControls.text(context,font,"");addView(title,new LayoutParams(-1,44));
        LinearLayout row=new LinearLayout(context);row.setOrientation(HORIZONTAL);
        yearLabel=WmControls.text(context,font,"");row.addView(yearLabel,new LayoutParams(0,44,1));
        yearInput=new EditText(context);WmControls.input(yearInput,font);yearInput.setSingleLine(true);
        yearInput.setInputType(InputType.TYPE_CLASS_NUMBER);yearInput.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
        yearInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        yearInput.setImeOptions(EditorInfo.IME_ACTION_DONE|EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        yearInput.setText(String.valueOf(year));yearInput.setSelectAllOnFocus(true);
        row.addView(yearInput,new LayoutParams(0,44,1));addView(row,new LayoutParams(-1,44));
        error=WmControls.text(context,font,"");error.setTextColor(Color.BLACK);error.setVisibility(GONE);addView(error,new LayoutParams(-1,-2));
        LinearLayout commands=new LinearLayout(context);commands.setOrientation(HORIZONTAL);
        ok=WmControls.text(context,font,"");WmControls.button(ok,selection);ok.setOnClickListener(view->submit());commands.addView(ok,new LayoutParams(0,44,1));
        cancel=WmControls.text(context,font,"");WmControls.button(cancel,selection);cancel.setOnClickListener(view->cancel());commands.addView(cancel,new LayoutParams(0,44,1));
        addView(commands,new LayoutParams(-1,44));setCancelButton(cancel);ok.setFocusableInTouchMode(true);cancel.setFocusableInTouchMode(true);updateLabels();yearInput.requestFocus();yearInput.selectAll();
        yearInput.setOnEditorActionListener((view,action,event)->{
            if(action==EditorInfo.IME_ACTION_DONE||event!=null&&WmControls.isEnterKey(event.getKeyCode())&&event.getAction()==KeyEvent.ACTION_DOWN){submit();return true;}return false;
        });
    }
    private static void checkYear(int year){if(year<MIN_YEAR||year>MAX_YEAR)throw new IllegalArgumentException("year outside 1900..2999");}
    public static int[] nearbyYears(int current){checkYear(current);int first=Math.max(MIN_YEAR,current-1),last=Math.min(MAX_YEAR,current+3);int[] years=new int[last-first+1];for(int i=0;i<years.length;i++)years[i]=first+i;return years;}
    public static String otherLabel(Locale locale){if(locale==null)throw new IllegalArgumentException("locale required");return "ko".equals(locale.getLanguage())?"기타...":"Other...";}
    public EditText input(){return yearInput;}
    /** Catalog-aware command caption. The Locale-only overload retains its legacy two-language contract. */
    public static String otherLabel(Context context,Locale locale){return WmStrings.text(context,language(context,locale),"기타...");}
    private static String language(Context context,Locale locale){if(locale==null)throw new IllegalArgumentException("locale required");return WmStrings.language(context,android.os.Build.VERSION.SDK_INT>=21?Modern.tag(locale):locale.toString().replace('_','-'));}
    private static final class Modern {static String tag(Locale locale){return locale.toLanguageTag();}}
    public void setLocale(Locale locale){language=language(getContext(),locale);updateLabels();}
    @Override public void onWmLanguageChanged(String language){this.language=WmStrings.language(getContext(),language);updateLabels();}
    private void updateLabels(){WmControls.bindText(title,language,"연도 선택");WmControls.bindText(yearLabel,language,"연도:");WmControls.bindText(error,language,"연도는 1900에서 2999 사이여야 합니다.");WmControls.bindText(ok,language,"확인");WmControls.bindText(cancel,language,"취소");}
    public boolean submit(){
        if(completed)return false;String value=yearInput.getText().toString();int year=-1;
        if(value.matches("[0-9]{1,4}"))try{year=Integer.parseInt(value);}catch(NumberFormatException invalid){}
        if(year<MIN_YEAR||year>MAX_YEAR){error.setVisibility(VISIBLE);yearInput.requestFocus();yearInput.selectAll();return false;}
        completed=true;hideKeyboard();listener.chosen(year);return true;
    }
    public void cancel(){if(completed)return;completed=true;hideKeyboard();listener.cancel();}
    @Override public boolean back(){if(completed)return false;cancel();return true;}
    private void hideKeyboard(){removeCallbacks(showKeyboard);InputMethodManager keyboard=(InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);if(keyboard!=null&&yearInput.getWindowToken()!=null)keyboard.hideSoftInputFromWindow(yearInput.getWindowToken(),0);}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();post(showKeyboard);}
    @Override protected void onDetachedFromWindow(){hideKeyboard();super.onDetachedFromWindow();}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        WmControls.faceBackground(this,selection); // Existing input/text and selection are retained.
    }
}
