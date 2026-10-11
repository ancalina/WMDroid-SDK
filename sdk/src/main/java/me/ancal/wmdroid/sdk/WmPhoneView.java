package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.telephony.PhoneNumberUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Captured WM614 phone client; the owner supplies telephony and contact data.
 * No provider query, permission prompt or outgoing call belongs to this view.
 */
public final class WmPhoneView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener {
    public interface Listener {void dial(String number);void voicemail();void speedDial();void history();}
    private final TextView status,accumulator,suggestion;
    private final LinearLayout results;
    public final WmPhoneKeypad keypad;
    private final Listener listener;
    private String number="",language="ko",statusCaption="",voicemailCaption;
    private int selection;
    private String countryIso;
    private boolean keypadVisible=true;

    public WmPhoneView(Context context,Typeface regular,Typeface bold,int selection,Listener listener){
        super(context);if(listener==null)throw new IllegalArgumentException("Phone listener required");this.listener=listener;
        countryIso=context.getResources().getConfiguration().locale.getCountry();
        setOrientation(VERTICAL);setSoundEffectsEnabled(false);
        LinearLayout header=new LinearLayout(context);header.setOrientation(VERTICAL);header.setBackgroundColor(0xff989fa3);
        status=WmControls.text(context,regular,"");status.setTextColor(Color.WHITE);status.setPadding(12,0,12,0);status.setGravity(Gravity.CENTER_VERTICAL);header.addView(status,new LayoutParams(-1,42));
        accumulator=WmControls.text(context,regular,"");accumulator.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,48);accumulator.setTextColor(Color.WHITE);accumulator.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);accumulator.setPadding(12,0,22,0);accumulator.setSingleLine(true);accumulator.setEllipsize(android.text.TextUtils.TruncateAt.START);header.addView(accumulator,new LayoutParams(-1,82));addView(header,new LayoutParams(-1,124));
        results=new LinearLayout(context);results.setOrientation(VERTICAL);
        suggestion=WmControls.text(context,bold,"");suggestion.setGravity(Gravity.CENTER);suggestion.setSingleLine(true);suggestion.setEllipsize(android.text.TextUtils.TruncateAt.END);suggestion.setSoundEffectsEnabled(false);suggestion.setOnClickListener(v->{if(number.isEmpty()&&voicemailCaption!=null)listener.voicemail();});results.addView(suggestion,new LayoutParams(-1,44));addView(results,new LayoutParams(-1,0,1));
        keypad=new WmPhoneKeypad(context,regular,bold,new WmPhoneKeypad.Listener(){
            public void digit(char digit){if(number.length()<80)setNumber(number+digit);}
            public void backspace(){if(!number.isEmpty())setNumber(number.substring(0,number.length()-1));}
            public void speedDial(){listener.speedDial();}public void history(){listener.history();}public void talk(){if(!number.isEmpty())listener.dial(number);}
        });addView(keypad,new LayoutParams(-1,WmPhoneKeypad.HEIGHT));onWmRestyle(regular,bold,selection,WmPalette.face(selection));onWmLanguageChanged(language);
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int height=WmPhoneKeypad.HEIGHT;
        if(MeasureSpec.getMode(heightSpec)!=MeasureSpec.UNSPECIFIED){
            int reserved=getPaddingTop()+getPaddingBottom()+getChildAt(0).getLayoutParams().height+suggestion.getLayoutParams().height;
            height=Math.min(height,Math.max(0,MeasureSpec.getSize(heightSpec)-reserved));
        }
        // Pass the remaining height to the keypad's uniform fit instead of clipping its last row.
        keypad.getLayoutParams().height=height;
        super.onMeasure(widthSpec,heightSpec);
    }
    /** Formatting separators are accepted; URI payloads, letters and multiline values are not. */
    public static String dialable(String value){
        if(value==null||value.length()>256)throw new IllegalArgumentException("Invalid phone number");StringBuilder out=new StringBuilder();
        for(int i=0;i<value.length();i++){char c=value.charAt(i);if(c>='0'&&c<='9'||c=='*'||c=='#'||c=='+'||c==','||c==';')out.append(c);else if(c!=' '&&c!='('&&c!=')'&&c!='-'&&c!='.')throw new IllegalArgumentException("Invalid phone number");}
        if(out.length()>80)throw new IllegalArgumentException("Phone number too long");return out.toString();
    }
    /** Number formatting region, independent of UI language. Empty uses Android legacy formatting. */
    public void setCountryIso(String country){
        if(country==null||!country.matches("[A-Za-z]{2}|"))throw new IllegalArgumentException("Invalid country ISO");
        countryIso=country.toUpperCase(java.util.Locale.US);render();
    }
    public String number(){return number;}
    public void setNumber(String value){String next=dialable(value);number=next;render();}
    public void setStatus(String authored){if(authored==null)throw new IllegalArgumentException("Status required");statusCaption=authored;render();}
    /** true enables the voicemail caption/action only while the number is empty;
     * false omits it. The owner supplies subscription data and voicemail behavior. */
    public void setVoicemailAvailable(boolean available){voicemailCaption=available?"음성 사서함":null;render();}
    public boolean isKeypadVisible(){return keypadVisible;}
    public void setKeypadVisible(boolean visible){keypadVisible=visible;keypad.setVisibility(visible?View.VISIBLE:View.GONE);}
    private void render(){
        status.setText(WmStrings.text(getContext(),language,statusCaption));String formatted=number;
        if(!number.isEmpty()){String converted=Build.VERSION.SDK_INT>=21&&!countryIso.isEmpty()?Modern.format(number,countryIso):PhoneNumberUtils.formatNumber(number);if(converted!=null)formatted=converted;}
        accumulator.setText(formatted);accumulator.setBackgroundColor(number.isEmpty()?Color.TRANSPARENT:WmPalette.highlight(selection));
        boolean voicemail=number.isEmpty()&&voicemailCaption!=null;
        suggestion.setText(voicemail?"1   "+WmStrings.text(getContext(),language,voicemailCaption):number.isEmpty()?"":number);
        // ponytail: no Contacts provider in the SDK; host-provided matching rows replace
        // this neutral number caption when the native Contacts adapter is implemented.
        suggestion.setTextColor(voicemail?Color.WHITE:Color.BLACK);suggestion.setBackgroundColor(voicemail?WmPalette.highlight(selection):Color.TRANSPARENT);suggestion.setClickable(voicemail);
    }
    private static final class Modern {static String format(String number,String country){return PhoneNumberUtils.formatNumber(number,country);}}
    @Override public void onWmLanguageChanged(String language){this.language=WmStrings.language(getContext(),language);keypad.onWmLanguageChanged(this.language);render();}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        this.selection=selection;status.setTypeface(regular);accumulator.setTypeface(regular);suggestion.setTypeface(bold);WmFonts.attach(status,regular);WmFonts.attach(accumulator,regular);WmFonts.attach(suggestion,bold);
        results.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.WHITE,WmPalette.face(selection)}));keypad.onWmRestyle(regular,bold,selection,tabColor);render();
    }
}
