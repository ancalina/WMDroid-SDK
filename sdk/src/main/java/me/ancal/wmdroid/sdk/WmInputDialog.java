package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Full-client name-entry dialog; host hides its commands while this owns them.
 * Native Media template114: existing title, white client, Done/Cancel footer.
 * Parent surface and decoder remain attached. */
public final class WmInputDialog extends FrameLayout implements WmControls.BackHandler,WmControls.ModalOwner,WmControls.RestyleListener,WmControls.LanguageListener {
    public interface Submit {void submit(String value);}
    private final LinearLayout panel;
    private final TextView label;
    private final EditText editor;
    private final WmCommandBar commands;
    private final String labelText;
    private Submit submit;
    private Runnable dismissed;
    private boolean busy,closed;
    public WmInputDialog(Context context,Typeface font,Typeface bold,String labelText,int accent,int maxLength,Submit submit){
        super(context);
        if(font==null||bold==null||labelText==null||submit==null||maxLength<=0)throw new IllegalArgumentException("Input dialog arguments required");
        this.labelText=labelText;this.submit=submit;setClickable(true);setSoundEffectsEnabled(false);setBackgroundColor(Color.WHITE);
        panel=new LinearLayout(context);panel.setOrientation(LinearLayout.VERTICAL);addView(panel,new LayoutParams(-1,-1));
        LinearLayout body=new LinearLayout(context);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(20,20,20,0);panel.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        label=WmControls.text(context,bold,"");body.addView(label,new LinearLayout.LayoutParams(-1,32));
        editor=new EditText(context);WmControls.input(editor,font);editor.setSingleLine(true);editor.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(maxLength)});LinearLayout.LayoutParams field=new LinearLayout.LayoutParams(-1,38);field.topMargin=16;body.addView(editor,field);
        commands=new WmCommandBar(context,font,accent);panel.addView(commands,new LinearLayout.LayoutParams(-1,WmMetrics.BAR));
        editor.setOnEditorActionListener((v,id,event)->{accept();return true;});onWmLanguageChanged(WmStrings.language(context,"en"));
    }
    /** Topmost visible input owner, including controls nested in an applet. */
    public static WmInputDialog find(View root){
        if(root==null||root.getVisibility()!=View.VISIBLE)return null;
        if(root instanceof WmInputDialog&&!((WmInputDialog)root).closed)return (WmInputDialog)root;
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int n=group.getChildCount()-1;n>=0;n--){WmInputDialog input=find(group.getChildAt(n));if(input!=null)return input;}}
        return null;
    }
    public EditText editor(){return editor;}
    public boolean isClosed(){return closed;}
    public void onDismiss(Runnable action){dismissed=action;}
    /** Submit String.trim() text (ends at or below U+0020 removed); empty input refocuses.
     * Busy/closed does not submit; callback does not automatically close the dialog. */
    public void accept(){if(busy||closed)return;String value=editor.getText().toString().trim();if(value.length()==0){editor.requestFocus();return;}submit.submit(value);}
    public void setBusy(boolean value){busy=value;editor.setEnabled(!value);commands.setEnabled(!value);}
    /** No-op while busy/closed. Otherwise hide IME, detach and notify onDismiss once.
     * External detach also notifies once, even when busy, and clears submit. */
    public void dismiss(){if(busy||closed)return;closed=true;submit=null;android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.hideSoftInputFromWindow(getWindowToken(),0);Runnable action=dismissed;dismissed=null;if(getParent() instanceof ViewGroup)((ViewGroup)getParent()).removeView(this);if(action!=null)action.run();}
    @Override public boolean back(){dismiss();return true;}
    @Override public boolean blocksOwnerCommands(){return true;}
    @Override public View inputMethodOwner(){return panel;}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();editor.requestFocus();}
    @Override protected void onDetachedFromWindow(){closed=true;submit=null;Runnable action=dismissed;dismissed=null;if(action!=null)action.run();final android.view.ViewParent parent=getParent();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{if(parent instanceof ViewGroup&&getParent()==parent)((ViewGroup)parent).removeView(this);});super.onDetachedFromWindow();}
    @Override public void onWmLanguageChanged(String language){WmControls.bindText(label,language,labelText);WmControls.bindDescription(editor,language,"이름");commands.setCommands(WmStrings.text(getContext(),language,"완료"),this::accept,WmStrings.text(getContext(),language,"취소"),this::dismiss);}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){label.setTypeface(bold);WmFonts.attach(label,bold);WmControls.input(editor,regular);commands.restyle(regular,new WmBar(getContext(),true,selection));requestLayout();}
}
