package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/** GWES MessageBox adaptation: pack icon64; MB_OK keeps caption OK
 * without bottom buttons, confirmation adds Yes/No. No outside dismissal. Native builder00045C7C uses
 * DPI-scaled10px margins and a5/8-screen text cap. VGA geometry/font measurement
 * here is provisional; this is not the native GDI dialog backend. */
public final class WmMessageBox extends FrameLayout implements WmControls.BackHandler,WmControls.RestyleListener,WmControls.LanguageListener,WmControls.ModalOwner {
    public enum Icon { WARNING, QUESTION }
    private final Icon iconKind;
    private final ImageView icon;
    private String iconRevision;
    private final LinearLayout panel,title;
    private final TextView caption,message;
    private final WmScrollView messageScroll;
    private final ImageView ok;
    private final TextView yes,no;
    private final boolean defaultYes;
    private final String authoredCaption;
    private String authoredMessage;
    private Object[] messageValues;
    private Locale locale;
    private String language,captionKey;
    private Runnable acknowledged;
    private WmRadioMenu.Selection answer;
    private boolean complete;
    private int accent;
    public WmMessageBox(Context context,Typeface font,Locale locale,String caption,String message,int accent,Runnable acknowledged){
        this(context,font,locale,caption,message,accent,acknowledged,false,null,Icon.WARNING);
    }
    /** Warning MB_YESNO; index0 is Yes, index1 is No. Native0x134 starts on No. */
    public static WmMessageBox confirm(Context context,Typeface font,Locale locale,String caption,String message,int accent,boolean defaultYes,WmRadioMenu.Selection answer){
        return confirm(context,font,locale,caption,message,accent,defaultYes,Icon.WARNING,answer);
    }
    /** Native question/warning selection; existing callers retain the warning default. */
    public static WmMessageBox confirm(Context context,Typeface font,Locale locale,String caption,String message,int accent,boolean defaultYes,Icon icon,WmRadioMenu.Selection answer){
        if(answer==null||icon==null)throw new IllegalArgumentException("Confirmation answer/icon required");
        return new WmMessageBox(context,font,locale,caption,message,accent,null,defaultYes,answer,icon);
    }
    private WmMessageBox(Context context,Typeface font,Locale locale,String caption,String message,int accent,Runnable acknowledged,boolean defaultYes,WmRadioMenu.Selection answer,Icon iconKind){
        super(context);
        if(font==null||locale==null||caption==null||message==null)throw new IllegalArgumentException("Message box arguments required");
        this.iconKind=iconKind;this.locale=locale;authoredCaption=caption;authoredMessage=message;this.accent=accent;this.acknowledged=acknowledged;this.defaultYes=defaultYes;this.answer=answer;
        setClickable(true);setSoundEffectsEnabled(false);
        panel=answer==null?new LinearLayout(context):new WmDialog(context);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(2,2,2,2);panel.setClickable(true);
        addView(panel,new LayoutParams(-2,-2,Gravity.CENTER));
        title=new LinearLayout(context);title.setGravity(Gravity.CENTER_VERTICAL);
        this.caption=WmControls.text(context,font,"");this.caption.setTextColor(Color.WHITE);this.caption.setSingleLine(true);this.caption.setPadding(8,0,4,0);
        int captionHeight=answer==null?WmMetrics.BAR:44;
        title.addView(this.caption,new LinearLayout.LayoutParams(0,captionHeight,1));
        ok=new ImageView(context);ok.setScaleType(ImageView.ScaleType.FIT_CENTER);ok.setFocusableInTouchMode(true);ok.setSoundEffectsEnabled(false);ok.setOnClickListener(v->acknowledge());
        title.addView(ok,new LinearLayout.LayoutParams(WmTitleBar.CLOSE_WIDTH,captionHeight));panel.addView(title,new LinearLayout.LayoutParams(-1,captionHeight));
        if(answer!=null){View separator=new View(context);separator.setBackgroundColor(Color.BLACK);panel.addView(separator,new LinearLayout.LayoutParams(-1,2));}
        // GWES45C7C adds scaleY(10) at both v108 and the final MB_OK template.
        // Settled official VGA IE confirmations: client inset16, icon gap22.
        LinearLayout body=new LinearLayout(context);body.setPadding(answer==null?20:16,answer==null?20:16,answer==null?20:16,answer==null?40:14);body.setGravity(Gravity.TOP);body.setBackgroundColor(Color.WHITE);
        icon=new ImageView(context);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        refreshIcon();
        LinearLayout.LayoutParams image=new LinearLayout.LayoutParams(64,64);image.rightMargin=answer==null?20:22;body.addView(icon,image);
        this.message=WmControls.text(context,font,"");this.message.setGravity(Gravity.TOP);this.message.setTextColor(Color.BLACK);
        LinearLayout.LayoutParams textPosition=new LinearLayout.LayoutParams(0,-2);textPosition.gravity=Gravity.CENTER_VERTICAL;
        messageScroll=new WmScrollView(context,this.message);body.addView(messageScroll,textPosition);panel.addView(body,new LinearLayout.LayoutParams(-1,-2));
        if(answer==null){yes=null;no=null;}else{
            ok.setVisibility(GONE);ok.setFocusable(false);ok.setOnClickListener(null);
            LinearLayout commands=new LinearLayout(context);commands.setGravity(Gravity.LEFT);commands.setPadding(16,0,0,22);
            yes=WmControls.text(context,font,"");no=WmControls.text(context,font,"");
            for(TextView button:new TextView[]{yes,no}){WmControls.button(button,accent);button.setFocusableInTouchMode(true);}
            yes.setOnClickListener(v->choose(0));no.setOnClickListener(v->choose(1));
            LinearLayout.LayoutParams first=new LinearLayout.LayoutParams(112,56);first.rightMargin=14;commands.addView(yes,first);commands.addView(no,new LinearLayout.LayoutParams(112,56));panel.addView(commands,new LinearLayout.LayoutParams(-1,-2));
            ((WmDialog)panel).setDefaultButton(defaultYes?yes:no);((WmDialog)panel).setCancelButton(no);
        }
        setLocale(locale);style();
    }
    public TextView captionField(){return caption;}
    public TextView messageField(){return message;}
    public ImageView okButton(){return ok;}
    public TextView yesButton(){return yes;}
    public TextView noButton(){return no;}
    /** Optional semantic caption key; authored caption remains the fallback. */
    public void setCaptionContext(String key){captionKey=key;WmControls.bindText(caption,language,authoredCaption,captionKey);}
    /** Keep a translated message template and literal caller arguments while this box stays open. */
    public void setMessageFormat(String pattern,Object... values){
        WmControls.bindFormat(message,language,pattern,values);authoredMessage=pattern;messageValues=values.clone();requestLayout();
    }
    public void setLocale(Locale locale){
        if(locale==null)throw new IllegalArgumentException("Locale required");this.locale=locale;
        language=WmStrings.language(getContext(),android.os.Build.VERSION.SDK_INT>=21?Modern.tag(locale):locale.toString().replace('_','-'));
        WmControls.bindText(caption,language,authoredCaption,captionKey);
        if(messageValues==null)WmControls.bindText(message,language,authoredMessage);else WmControls.bindFormat(message,language,authoredMessage,messageValues);
        WmControls.bindDescription(ok,language,"확인");if(yes!=null){WmControls.bindText(yes,language,"예");WmControls.bindText(no,language,"아니요");}requestLayout();
    }
    private static final class Modern {static String tag(Locale locale){return locale.toLanguageTag();}}
    private void style(){
        GradientDrawable border=new GradientDrawable();border.setColor(Color.WHITE);border.setStroke(2,yes==null?accent:Color.BLACK);panel.setBackgroundDrawable(border);
        WmBar paint=new WmBar(getContext(),false,accent);title.setBackgroundDrawable(yes==null?paint:new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{paint.color(40),paint.color(41)}));
        ok.setImageDrawable(new WmCloseGlyph(true,paint.color(37),paint.color(38)));ok.setBackgroundDrawable(paint.titleButton(ok,false,false));
        if(yes!=null){WmControls.button(yes,accent);WmControls.button(no,accent);}
    }
    private void refreshIcon(){
        String revision=WmResources.revision(getContext());if(revision.equals(iconRevision))return;
        icon.setImageBitmap(WmResources.iconBitmap(getContext(),iconKind==Icon.QUESTION?"dialogs/question.png":"dialogs/warning.png",64,64,iconKind==Icon.QUESTION?"❔":"⚠️"));iconRevision=revision;
    }
    private void acknowledge(){if(complete)return;complete=true;Runnable action=acknowledged;acknowledged=null;if(action!=null)action.run();}
    private void choose(int index){if(complete)return;complete=true;WmRadioMenu.Selection action=answer;answer=null;if(action!=null)action.choose(index);}
    @Override public boolean back(){if(yes==null)acknowledge();else choose(1);return true;}
    @Override public boolean blocksOwnerCommands(){return true;}
    /** A modal warning must not send keyboard focus into its underlying page. */
    @Override public View focusSearch(View focused,int direction){if(yes==null)return ok;if(focused==yes)return no;if(focused==no)return yes;return defaultYes?yes:no;}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int available=MeasureSpec.getSize(widthSpec),textCap=Math.max(1,available*5/8),natural=0;
        // GWES45C7C measures wrapped text at the5/8 cap before sizing the dialog.
        StaticLayout wrapped=new StaticLayout(message.getText(),message.getPaint(),textCap,Layout.Alignment.ALIGN_NORMAL,1,0,false);
        for(int line=0;line<wrapped.getLineCount();line++)natural=Math.max(natural,(int)Math.ceil(yes==null?wrapped.getLineWidth(line):wrapped.getLineMax(line)));
        int desired=Math.max(yes==null?114:280,(int)Math.ceil(WmFonts.measure(caption.getPaint(),caption.getText().toString()))+62);
        int textWidth=Math.min(textCap,natural);
        desired=Math.max(desired,yes==null?textWidth+128:confirmationWidth(textWidth));
        int panelWidth=Math.max(0,Math.min(Math.max(0,available-16),desired));panel.getLayoutParams().width=panelWidth;
        LinearLayout body=(LinearLayout)messageScroll.getParent();LinearLayout.LayoutParams image=(LinearLayout.LayoutParams)icon.getLayoutParams();
        int inset=panel.getPaddingLeft()+panel.getPaddingRight()+body.getPaddingLeft()+body.getPaddingRight()+image.width+image.rightMargin;
        // Keep the measured wrap cap: spare dialog padding must not reflow the text.
        int clientWidth=Math.max(0,Math.min(textCap,panelWidth-inset));messageScroll.getLayoutParams().width=clientWidth;
        if(yes!=null){
            // GWES converts each button origin independently; centering rounded widths shifts them.
            int rawWidth=panelWidth==confirmationWidth(textWidth)?textWidth+131:Math.max(0,panelWidth-4);
            int first=confirmationX(Math.max(0,(rawWidth-236)/2)),second=confirmationX(Math.max(0,(rawWidth-236)/2)+124);
            LinearLayout commands=(LinearLayout)yes.getParent();commands.setPadding(first,0,0,22);
            ((LinearLayout.LayoutParams)yes.getLayoutParams()).rightMargin=Math.max(0,second-first-112);
            StaticLayout client=new StaticLayout(message.getText(),message.getPaint(),Math.max(1,clientWidth),Layout.Alignment.ALIGN_NORMAL,1,0,false);
            boolean shortText=client.getHeight()+3L<=64;
            body.setPadding(16,16,16,shortText?20:14);
            // Caption/frame, body insets and commands occupy158 VGA pixels for tall text.
            int limit=Math.max(0,MeasureSpec.getSize(heightSpec)-158);
            messageScroll.getLayoutParams().height=shortText?LayoutParams.WRAP_CONTENT:(int)Math.min(limit,((2L+8L*(client.getHeight()+3L)/28)*14/8)*2);
        }
        super.onMeasure(widthSpec,heightSpec);
    }
    // shortcut: standard VGA SysFnt cached7x14 units; custom-font GDI/DC units remain unmeasured.
    private static int confirmationWidth(int textWidth){return confirmationX(textWidth+131)+4;}
    private static int confirmationX(int raw){return (int)(((4L*raw/14)*7/4)*2);}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();(yes==null?ok:defaultYes?yes:no).requestFocus();}
    @Override protected void onDetachedFromWindow(){complete=true;acknowledged=null;answer=null;super.onDetachedFromWindow();}
    @Override public void onWmLanguageChanged(String language){setLocale(WmStrings.locale(WmStrings.language(getContext(),language)));}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){accent=selection;refreshIcon();Typeface heading=bold==null?regular:bold;caption.setTypeface(heading);WmFonts.attach(caption,heading);message.setTypeface(regular);WmFonts.attach(message,regular);if(yes!=null)for(TextView button:new TextView[]{yes,no}){button.setTypeface(heading);WmFonts.attach(button,heading);}style();requestLayout();}
}
