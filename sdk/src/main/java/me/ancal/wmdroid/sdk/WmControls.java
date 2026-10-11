package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import java.util.WeakHashMap;

/** Shared logical-VGA controls; immediate GDI selection, no ripple or fade.
 * Call factories and restyleTree on the UI thread. Restyling does not rebuild views.
 */
public final class WmControls {
    private WmControls(){}
    public static final int ROW_HEIGHT=44, TAB_HEIGHT=WmMetrics.TAB;
    public static final int SURFACE=Color.WHITE, FACE=0xffc9e7e9, SHADOW=0xff808080, FRAME=Color.BLACK;
    private static final int DEFAULT_SELECTION=0xff149ca7;
    // Values never retain their view: detached controls can be collected normally.
    private static final WeakHashMap<View,Style> STYLES=new WeakHashMap<>();
    private static final class Style {
        boolean text,bold,locked,input,check,listCheck,entry,tab,button,entryBackground,faceBackground,defaultButton;
        String authoredText,textKey,authoredDescription,authoredHint;
        Object[] formatValues;
        int indicatorGap,indicatorColor=0xff37c7d3,buttonSelection=DEFAULT_SELECTION;
    }
    private static Style style(View view){Style result=STYLES.get(view);if(result==null){result=new Style();STYLES.put(view,result);}return result;}

    /** Explicit opt-in for custom Canvas views and page-owned future rendering. */
    public interface RestyleListener {
        void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
    }
    /** Explicit authored UI updates; user/provider values stay unbound. */
    public interface LanguageListener {void onWmLanguageChanged(String language);}
    public static void bindText(TextView view,String language,String authored){bindText(view,language,authored,null);}
    public static void bindText(TextView view,String language,String authored,String key){
        if(view instanceof EditText)throw new IllegalArgumentException("Editable values cannot be bound as authored UI text");
        if(authored==null)throw new IllegalArgumentException("Authored UI text is required");
        Style state=style(view);state.authoredText=authored;state.textKey=key;state.formatValues=null;localize(view,state,language);
    }
    /** Translate the template; literal caller values survive language changes.
     * String.format uses the app catalog language selected by WmStrings.language,
     * including parent-language or ko fallback, rather than the raw requested tag. */
    public static void bindFormat(TextView view,String language,String pattern,Object... values){
        if(view instanceof EditText)throw new IllegalArgumentException("Editable values cannot be bound as authored UI text");
        if(pattern==null||values==null)throw new IllegalArgumentException("Authored pattern and values required");
        Object[] copy=values.clone();
        String.format(WmStrings.locale(WmStrings.language(view.getContext(),language)),WmStrings.text(view.getContext(),language,pattern),copy);
        Style state=style(view);state.authoredText=pattern;state.textKey=null;state.formatValues=copy;localize(view,state,language);
    }
    /** Authored placeholder only; editable text/caret/listeners are untouched. */
    public static void bindHint(TextView view,String language,String authored){
        if(authored==null)throw new IllegalArgumentException("Authored UI hint is required");
        Style state=style(view);state.authoredHint=authored;localize(view,state,language);
    }
    public static void bindDescription(View view,String language,String authored){
        if(authored==null)throw new IllegalArgumentException("Authored UI description is required");
        Style state=style(view);state.authoredDescription=authored;localize(view,state,language);
    }
    public static void localizeTree(View root,String language){
        if(root==null)return;Style state=STYLES.get(root);if(state!=null)localize(root,state,language);
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++)localizeTree(group.getChildAt(i),language);}
        if(root instanceof LanguageListener)((LanguageListener)root).onWmLanguageChanged(language);
    }
    private static void localize(View view,Style state,String language){
        if(state.authoredText!=null&&view instanceof TextView){
            TextView text=(TextView)view;String caption=state.textKey==null?WmStrings.text(view.getContext(),language,state.authoredText):WmStrings.text(view.getContext(),language,state.authoredText,state.textKey);
            if(state.formatValues!=null)caption=String.format(WmStrings.locale(WmStrings.language(view.getContext(),language)),caption,state.formatValues);
            if(!TextUtils.equals(text.getText(),caption))text.setText(caption);
            if(state.tab){ViewGroup.LayoutParams size=text.getLayoutParams();if(size!=null&&size.width>=0){int width=tabWidth(text);if(size.width!=width){size.width=width;text.setLayoutParams(size);}}}
        }
        if(state.authoredHint!=null&&view instanceof TextView){TextView text=(TextView)view;String hint=WmStrings.text(view.getContext(),language,state.authoredHint);if(!TextUtils.equals(text.getHint(),hint))text.setHint(hint);}
        if(state.authoredDescription!=null){String description=WmStrings.text(view.getContext(),language,state.authoredDescription);if(!TextUtils.equals(view.getContentDescription(),description))view.setContentDescription(description);}
    }

    /** A nested SDK control consumes Back before its containing window closes. */
    public interface BackHandler {boolean back();}
    /** A child modal dialog blocks its containing window's commands, not its own controls. */
    public interface ModalOwner {boolean blocksOwnerCommands();default View inputMethodOwner(){return null;}}
    /** A modal may nominate its own editors; underlying page editors stay blocked. */
    public static View inputMethodOwner(View root){
        java.util.ArrayList<View> visited=null;
        while(root!=null&&root.getVisibility()==View.VISIBLE){
            if(visited!=null){for(int i=0;i<visited.size();i++)if(visited.get(i)==root)return null;visited.add(root);}
            if(root instanceof ModalOwner&&((ModalOwner)root).blocksOwnerCommands()){
                View child=((ModalOwner)root).inputMethodOwner();if(child==null||child==root)return null;
                // Only nominations can re-enter an otherwise acyclic View hierarchy.
                if(visited==null){visited=new java.util.ArrayList<View>();visited.add(root);}
                root=child;continue;
            }
            View next=null;
            if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=group.getChildCount()-1;i>=0;i--){View child=group.getChildAt(i);if(blocksOwnerCommands(child)){next=child;break;}}}
            if(next==null)return root;
            root=next;
        }
        return null;
    }
    public static boolean blocksOwnerCommands(View root){
        if(root==null||root.getVisibility()!=View.VISIBLE)return false;
        if(root instanceof ModalOwner&&((ModalOwner)root).blocksOwnerCommands())return true;
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=group.getChildCount()-1;i>=0;i--)if(blocksOwnerCommands(group.getChildAt(i)))return true;}
        return false;
    }
    /** Use at custom title/command callbacks as well as touch dispatch. */
    public static boolean ownerCommandsAllowed(View command){
        for(android.view.ViewParent parent=command.getParent();parent!=null;parent=parent.getParent()){
            if(parent instanceof ModalOwner&&((ModalOwner)parent).blocksOwnerCommands())return true;
            if(parent instanceof WmWindow)return ((WmWindow)parent).ownerCommandsAllowed();
        }
        return true;
    }

    public static boolean back(View root){
        if(root==null||root.getVisibility()!=View.VISIBLE)return false;
        if(root instanceof BackHandler)return ((BackHandler)root).back();
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=group.getChildCount()-1;i>=0;i--)if(back(group.getChildAt(i)))return true;}
        return false;
    }

    /** Android splits the main and numeric keypad keys that both represent Enter. */
    /** Native list F4 / combo Alt+Up/Down; Android has no WinCE extended-key lParam. */
    static boolean isComboToggleKey(int key,android.view.KeyEvent event){
        return key==android.view.KeyEvent.KEYCODE_F4&&event.hasNoModifiers()
            ||(key==android.view.KeyEvent.KEYCODE_DPAD_UP||key==android.view.KeyEvent.KEYCODE_DPAD_DOWN)&&event.hasModifiers(android.view.KeyEvent.META_ALT_ON);
    }
    public static boolean isEnterKey(int keyCode){return keyCode==KeyEvent.KEYCODE_ENTER||keyCode==KeyEvent.KEYCODE_NUMPAD_ENTER;}

    /** Explicit dialog scope: Enter activates its focused SDK push button;
     * Escape activates its Cancel button. Editors retain their own key handling.
     * This is not a replacement for the full native IsDialogMessage loop.
     */
    public static boolean dialogButtonKey(ViewGroup dialog,KeyEvent event,TextView cancel){
        return dialogButtonKey(dialog,event,cancel,null);
    }
    /** Stored defaults apply to owner/plain Static focus; editors keep their keys. */
    public static boolean dialogButtonKey(ViewGroup dialog,KeyEvent event,TextView cancel,TextView storedDefault){
        if(dialog==null||event==null)throw new IllegalArgumentException("dialog and key required");
        int key=event.getKeyCode();
        if(!isEnterKey(key)&&key!=KeyEvent.KEYCODE_ESCAPE)return false;
        if(event.getAction()!=KeyEvent.ACTION_DOWN&&event.getAction()!=KeyEvent.ACTION_UP)return false;
        for(int i=0;i<dialog.getChildCount();i++)if(blocksOwnerCommands(dialog.getChildAt(i)))return false;
        View focus=dialog.findFocus();
        if(focus!=null&&!dialogOwns(dialog,focus))return false;
        View target=key==KeyEvent.KEYCODE_ESCAPE?cancel:
            focus==dialog||isStaticText(focus)?storedDefault:focus;
        if(!isPushButton(target)||!dialogOwns(dialog,target))return false;
        if(dialogButtonEnabled(dialog,target)&&event.getAction()==KeyEvent.ACTION_DOWN&&!event.isCanceled())target.performClick();
        return true;
    }
    static boolean isPushButton(View view){Style state=STYLES.get(view);return state!=null&&state.button;}
    private static boolean isStaticText(View view){
        Style state=STYLES.get(view);return view!=null&&view.getClass()==TextView.class&&state!=null&&state.text&&
            !state.input&&!state.check&&!state.button&&!state.entry&&!state.tab;
    }
    static boolean dialogOwns(ViewGroup dialog,View target){
        for(View current=target;current!=null;){
            if(current==dialog)return true;
            if(current instanceof WmDialog||!(current.getParent() instanceof View))return false;
            current=(View)current.getParent();
        }
        return false;
    }
    static boolean dialogButtonEnabled(ViewGroup dialog,View target){
        View current=target;
        while(current!=dialog){
            if(current==null||!current.isEnabled()||current.getVisibility()!=View.VISIBLE)return false;
            if(!(current.getParent() instanceof View))return false;
            current=(View)current.getParent();
        }
        return dialog.isEnabled()&&dialog.getVisibility()==View.VISIBLE;
    }

    public static TextView text(Context context,Typeface font,CharSequence value){TextView view=new TextView(context);textStyle(view,font);view.setText(value);return view;}
    public static void textStyle(TextView view,Typeface font){
        view.setSoundEffectsEnabled(false);
        if(Build.VERSION.SDK_INT>=26)view.setDefaultFocusHighlightEnabled(false);
        Style state=style(view);state.text=true;state.bold=font!=null&&font.isBold();
        // ICS BoringLayout reuses metrics without clearing earlier larger font extents.
        if(Build.VERSION.SDK_INT<=15&&!(view instanceof EditText)&&!(view.getText() instanceof android.text.Spannable))view.setText(view.getText(),TextView.BufferType.SPANNABLE);
        view.setTypeface(font);WmFonts.attach(view,font);view.setTextSize(TypedValue.COMPLEX_UNIT_PX,WmMetrics.FONT);
        view.setTextColor(plainTextColors());view.setIncludeFontPadding(false);view.setLineSpacing(0,1);
        if(Build.VERSION.SDK_INT>=21)view.setFontFeatureSettings("kern");
        if(Build.VERSION.SDK_INT>=23)view.setBreakStrategy(0);
        view.setGravity(Gravity.CENTER_VERTICAL);
    }
    public static void input(EditText view,Typeface font){
        textStyle(view,font);style(view).input=true;view.setBackgroundDrawable(rectangle(SURFACE));
        if(Build.VERSION.SDK_INT>=21)view.setBackgroundTintList(null);
        view.setPadding(4,2,4,2);clearMinimums(view);
    }
    public static void check(CheckBox view,Typeface font){
        check(view,font,false);
    }
    /** Fixed 32-pixel list state image, without standalone checkbox focus or gap. */
    public static void listCheck(CheckBox view,Typeface font){
        check(view,font,true);
    }
    private static void check(CheckBox view,Typeface font,boolean listStyle){
        textStyle(view,font);Style state=style(view);state.check=true;state.listCheck=listStyle;view.setButtonDrawable(new CheckMark(listStyle));
        if(Build.VERSION.SDK_INT>=21)view.setButtonTintList(null);
        if(listStyle&&Build.VERSION.SDK_INT>=21){view.setBackgroundTintList(null);view.setStateListAnimator(null);}
        view.setBackgroundColor(Color.TRANSPARENT);view.setPadding(0,0,0,0);clearMinimums(view);view.setSoundEffectsEnabled(false);
        reserveIndicator(view);
    }
    /** Classic radio selection, without platform tint, ripple or animations. */
    public static void radio(android.widget.RadioButton view,Typeface font){
        textStyle(view,font);style(view).check=true;style(view).listCheck=false;view.setButtonDrawable(new RadioMark());
        if(Build.VERSION.SDK_INT>=21){view.setButtonTintList(null);view.setBackgroundTintList(null);view.setStateListAnimator(null);}
        view.setBackgroundColor(Color.TRANSPARENT);view.setPadding(0,0,0,0);clearMinimums(view);view.setSoundEffectsEnabled(false);
        reserveIndicator(view);
    }
    // Older CompoundButton implementations reserve the indicator through padding;
    // newer ones add its width in getCompoundPaddingLeft(). Fill only the deficit.
    private static int indicatorGap(TextView view){return WmMetrics.EDGE+Math.round(WmFonts.measure(view.getPaint(),"0"))/2;}
    private static void reserveIndicator(android.widget.CompoundButton view){
        int gap=style(view).listCheck?0:indicatorGap(view),width=style(view).listCheck?32:WmMetrics.INDICATOR;
        int missing=Math.max(0,width+gap-view.getCompoundPaddingLeft());
        if(missing>0)view.setPadding(view.getPaddingLeft()+missing,view.getPaddingTop(),view.getPaddingRight(),view.getPaddingBottom());
        style(view).indicatorGap=gap;
    }
    private static void restyleIndicator(android.widget.CompoundButton view,int selection){
        if(style(view).listCheck)return;
        Style state=style(view);int gap=indicatorGap(view),delta=gap-state.indicatorGap;
        if(delta!=0)view.setPadding(Math.max(0,view.getPaddingLeft()+delta),view.getPaddingTop(),view.getPaddingRight(),view.getPaddingBottom());
        state.indicatorGap=gap;
        int color=WmPalette.highlight(selection);if(state.indicatorColor!=color){state.indicatorColor=color;view.invalidate();}
    }
    public static int tabWidth(TextView view){return (int)Math.ceil(WmFonts.measure(view.getPaint(),view.getText().toString()))+32;}
    public static void tab(TextView view,boolean selected,int tabColor){
        boolean changed=view.isSelected()!=selected;
        style(view).tab=true;view.setBackgroundDrawable(new WmTabDrawable(selected,tabColor));view.setSelected(selected);view.setSoundEffectsEnabled(false);
        if(changed&&view.getParent() instanceof WmTabStrip)((WmTabStrip)view.getParent()).onTabSelectionChanged(view);
    }
    public static StateListDrawable background(int normal,int selected){
        StateListDrawable result=new StateListDrawable();result.addState(new int[]{-android.R.attr.state_enabled},new ColorDrawable(normal));
        for(int state:new int[]{android.R.attr.state_pressed,android.R.attr.state_selected,android.R.attr.state_focused})result.addState(new int[]{state},new ColorDrawable(selected));
        result.addState(new int[]{},new ColorDrawable(normal));result.setEnterFadeDuration(0);result.setExitFadeDuration(0);return result;
    }
    public static void entry(TextView view,int selection){view.setSoundEffectsEnabled(false);Style state=style(view);state.entry=true;state.button=false;state.tab=false;entryAppearance(view,selection);}
    public static void entryBackground(View view,int selection){view.setSoundEffectsEnabled(false);style(view).entryBackground=true;view.setBackgroundDrawable(background(SURFACE,selection));}
    /** Opt a page header or control container into live semantic face updates. */
    public static void faceBackground(View view,int selection){style(view).faceBackground=true;view.setBackgroundColor(WmPalette.face(selection));}
    public static void button(TextView view){button(view,DEFAULT_SELECTION);}
    public static void button(TextView view,int selection){
        if(!style(view).text)textStyle(view,view.getTypeface());Style state=style(view);state.button=true;state.entry=false;state.tab=false;
        int inset=WmMetrics.EDGE+(state.defaultButton?WmMetrics.BORDER:0);
        view.setGravity(Gravity.CENTER);view.setPadding(inset,inset,inset,inset);clearMinimums(view);view.setSoundEffectsEnabled(false);
        if(Build.VERSION.SDK_INT>=21){view.setBackgroundTintList(null);view.setStateListAnimator(null);view.setElevation(0);}
        buttonAppearance(view,selection);
    }
    /** BS_DEFPUSHBUTTON appearance; owner still decides which command Enter invokes. */
    public static void setDefaultButton(TextView view,boolean enabled){
        Style state=STYLES.get(view);if(state==null||!state.button)throw new IllegalArgumentException("Registered button required");
        if(state.defaultButton==enabled)return;int delta=enabled?WmMetrics.BORDER:-WmMetrics.BORDER;state.defaultButton=enabled;
        view.setPadding(view.getPaddingLeft()+delta,view.getPaddingTop()+delta,view.getPaddingRight()+delta,view.getPaddingBottom()+delta);buttonAppearance(view,state.buttonSelection);
    }
    /** Keeps a font preview's own face while still allowing explicit theme updates. */
    public static void lockFont(View view){style(view).locked=true;}
    public static void select(View container,View chosen){
        if(container instanceof ViewGroup){ViewGroup group=(ViewGroup)container;for(int i=0;i<group.getChildCount();i++)group.getChildAt(i).setSelected(group.getChildAt(i)==chosen);}
    }
    public static void restyleTree(View root,Typeface font,int selection,int tabColor){restyleTree(root,font,font,selection,tabColor);}
    /** Reuses the tree and retains authored bindings and click listeners. Font/width
     * changes and RestyleListener callbacks can affect layout, focus and scrolling.
     * Callback exceptions propagate; prior changes are not rolled back.
     * Supply the original regular/bold families; default push buttons use the bold
     * role (native GWE Button FontWeight700). Locked fonts remain explicit
     * overrides. No synthetic bold is generated.
     */
    public static void restyleTree(View root,Typeface regular,Typeface bold,int selection,int tabColor){
        if(root==null)return;if(root instanceof TextView)WmFonts.applySmoothing((TextView)root);Style state=STYLES.get(root);
        if(state!=null){
            if(state.text&&!state.locked&&root instanceof TextView){TextView text=(TextView)root;Typeface font=(state.bold||state.button)&&bold!=null?bold:regular;text.setTypeface(font);WmFonts.attach(text,font);}
            if(state.entryBackground)root.setBackgroundDrawable(background(SURFACE,selection));
            if(state.faceBackground)root.setBackgroundColor(WmPalette.face(selection));
            if(root instanceof TextView){TextView text=(TextView)root;
                if(state.tab){text.setBackgroundDrawable(new WmTabDrawable(text.isSelected(),tabColor));text.setTextColor(plainTextColors());ViewGroup.LayoutParams params=text.getLayoutParams();if(params!=null&&params.width>=0){int width=tabWidth(text);if(params.width!=width){params.width=width;text.setLayoutParams(params);}}}
                else if(state.button)buttonAppearance(text,selection);
                else if(state.entry)entryAppearance(text,selection);
                else if(state.check){text.setTextColor(plainTextColors());restyleIndicator((android.widget.CompoundButton)text,selection);}
            }
        }
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++)restyleTree(group.getChildAt(i),regular,bold,selection,tabColor);}
        if(root instanceof RestyleListener)((RestyleListener)root).onWmRestyle(regular,bold,selection,tabColor);
    }
    private static ColorStateList plainTextColors(){return new ColorStateList(new int[][]{{-android.R.attr.state_enabled},{}},new int[]{SHADOW,Color.BLACK});}
    private static ColorStateList selectionTextColors(){return new ColorStateList(new int[][]{{-android.R.attr.state_enabled},{android.R.attr.state_pressed},{android.R.attr.state_selected},{android.R.attr.state_focused},{}},new int[]{SHADOW,SURFACE,SURFACE,SURFACE,Color.BLACK});}
    private static void entryAppearance(TextView view,int selection){view.setBackgroundDrawable(background(SURFACE,selection));view.setTextColor(selectionTextColors());}
    private static void buttonAppearance(TextView view,int selection){
        // AB800→C3988→BC0EC: BLACK_BRUSH only while pressed; focus keeps the face.
        Style state=style(view);state.buttonSelection=selection;
        int normal=WmPalette.face(selection),border=WmMetrics.BORDER*(state.defaultButton?2:1);StateListDrawable face=new StateListDrawable();
        // C3988 skips the extra default frame for WS_DISABLED, including forced press.
        face.addState(new int[]{-android.R.attr.state_enabled,android.R.attr.state_pressed},buttonFace(Color.BLACK,WmMetrics.BORDER));
        face.addState(new int[]{-android.R.attr.state_enabled},buttonFace(normal,WmMetrics.BORDER));
        face.addState(new int[]{android.R.attr.state_pressed},buttonFace(Color.BLACK,border));
        face.addState(new int[]{},buttonFace(normal,border));face.setEnterFadeDuration(0);face.setExitFadeDuration(0);view.setBackgroundDrawable(face);
        view.setTextColor(new ColorStateList(new int[][]{{-android.R.attr.state_enabled},{android.R.attr.state_pressed},{}},new int[]{0xffbfbfbf,Color.WHITE,Color.BLACK}));
    }
    private static GradientDrawable buttonFace(int color,int border){GradientDrawable face=rectangle(color);face.setStroke(border,Color.BLACK);return face;}
    private static GradientDrawable rectangle(int color){GradientDrawable shape=new GradientDrawable();shape.setShape(GradientDrawable.RECTANGLE);shape.setColor(color);shape.setStroke(WmMetrics.BORDER,FRAME);return shape;}
    private static void clearMinimums(TextView view){view.setMinWidth(0);view.setMinHeight(0);view.setMinimumWidth(0);view.setMinimumHeight(0);}

    private static int focusColor(Drawable owner){
        Style state=owner.getCallback() instanceof View?STYLES.get((View)owner.getCallback()):null;
        return state==null?0xff37c7d3:state.indicatorColor;
    }
    private static void indicatorFocus(Canvas canvas,Drawable owner,Paint paint,int alpha,android.graphics.Path mask){
        paint.setColor(focusColor(owner));paint.setAlpha(alpha);canvas.drawPath(mask,paint);
    }
    private static void captionFocus(Canvas canvas,Drawable owner,Paint paint,int alpha){
        if(!(owner.getCallback() instanceof TextView))return;
        TextView view=(TextView)owner.getCallback();
        if(view instanceof WmThreeStateCheck&&!((WmThreeStateCheck)view).isAutomatic())return;
        android.text.Layout layout=view.getLayout();if(TextUtils.isEmpty(view.getText())||layout==null)return;
        float first=Float.MAX_VALUE,last=-Float.MAX_VALUE;
        for(int line=0;line<layout.getLineCount();line++){first=Math.min(first,layout.getLineLeft(line));last=Math.max(last,layout.getLineRight(line));}
        // C2FE0/A5968: clamp the origin first, then extend; solid PATCOPY after text.
        // ponytail: Android Layout supplies extents until native GDI text metrics are recovered.
        int border=WmMetrics.BORDER;
        int left=Math.max(0,view.getCompoundPaddingLeft()+Math.round(first)-view.getScrollX()-2*border);
        int top=Math.max(0,view.getBaseline()-layout.getLineBaseline(0)-view.getScrollY()-border);
        int right=Math.min(view.getWidth(),left+Math.round(last-first)+4*border);
        int bottom=Math.min(view.getHeight(),top+layout.getHeight()+1+2*border);
        if(left>=right||top>=bottom)return;
        paint.setColor(focusColor(owner));paint.setAlpha(alpha);
        canvas.drawRect(right-border,top,right,bottom,paint);right-=border;
        canvas.drawRect(left,bottom-border,right,bottom,paint);bottom-=border;
        canvas.drawRect(left,top,left+border,bottom,paint);left+=border;
        canvas.drawRect(left,top,right,top+border,paint);
    }
    private static android.graphics.Path indicatorFocusMask(boolean radio){
        // Original bitmap4 row36, columns6/7: black mask pixels select the brush (ROP3 B8).
        int[] rects=radio?new int[]{11,2,19,3,9,3,21,4,7,4,11,5,19,4,23,5,6,5,9,6,21,5,24,6,5,6,7,7,23,6,25,7,4,7,6,8,24,7,26,8,4,8,6,9,24,8,26,9,3,9,5,10,25,9,27,10,3,10,5,11,25,10,27,11,2,11,4,12,26,11,28,12,2,12,4,13,26,12,28,13,2,13,4,14,26,13,28,14,2,14,4,15,26,14,28,15,2,15,4,16,26,15,28,16,2,16,4,17,26,16,28,17,2,17,4,18,26,17,28,18,2,18,4,19,26,18,28,19,3,19,5,20,25,19,27,20,3,20,5,21,25,20,27,21,4,21,6,22,24,21,26,22,4,22,6,23,24,22,26,23,5,23,7,24,23,23,25,24,6,24,9,25,21,24,24,25,7,25,11,26,19,25,23,26,9,26,21,27,11,27,19,28}:new int[]{2,2,28,4,2,26,28,28,2,4,4,26,26,4,28,26};
        android.graphics.Path path=new android.graphics.Path();for(int i=0;i<rects.length;i+=4)path.addRect(rects[i],rects[i+1],rects[i+2],rects[i+3],android.graphics.Path.Direction.CW);return path;
    }

    /** Original GWE bitmap4 radio glyph, vectorized without platform tint or animation. */
    private static final class RadioMark extends Drawable {
        private static final int[] OUTER={11,19,8,22,7,23,5,25,4,26,3,27,3,27,2,28,1,29,1,29,1,29,0,30,0,30,0,30,0,30,0,30,0,30,0,30,0,30,1,29,1,29,1,29,2,28,3,27,3,27,4,26,5,25,7,23,8,22,11,19};
        private static final int[] INNER={0,0,0,0,11,19,9,21,7,23,6,24,5,25,4,26,4,26,3,27,3,27,2,28,2,28,2,28,2,28,2,28,2,28,2,28,2,28,3,27,3,27,4,26,4,26,5,25,6,24,7,23,9,21,11,19,0,0,0,0};
        private static final int[] DOT={0,0,0,0,0,0,0,0,0,0,0,0,12,18,10,20,8,22,8,22,7,23,7,23,6,24,6,24,6,24,6,24,6,24,6,24,7,23,7,23,8,22,8,22,10,20,12,18,0,0,0,0,0,0,0,0,0,0,0,0};
        private final Paint paint=new Paint();private boolean checked,enabled=true,pressed,focused;private int alpha=255;
        @Override public boolean isStateful(){return true;}
        @Override protected boolean onStateChange(int[] states){
            boolean c=false,e=false,p=false,f=false;if(states!=null)for(int state:states){if(state==android.R.attr.state_checked)c=true;else if(state==android.R.attr.state_enabled)e=true;else if(state==android.R.attr.state_pressed)p=true;else if(state==android.R.attr.state_focused)f=true;}
            if(c==checked&&e==enabled&&p==pressed&&f==focused)return false;checked=c;enabled=e;pressed=p;focused=f;invalidateSelf();return true;
        }
        private static android.graphics.Path shape(int[] rows){android.graphics.Path path=new android.graphics.Path();for(int y=0;y<30;y++)if(rows[2*y]<rows[2*y+1])path.addRect(rows[2*y],y,rows[2*y+1],y+1,android.graphics.Path.Direction.CW);return path;}
        private final android.graphics.Path inner=shape(INNER),dot=shape(DOT),border=border(),focus=indicatorFocusMask(true);
        private static android.graphics.Path border(){
            android.graphics.Path path=new android.graphics.Path();
            for(int y=0;y<30;y++){int left=OUTER[2*y],right=OUTER[2*y+1],insideLeft=INNER[2*y],insideRight=INNER[2*y+1];
                if(insideLeft==insideRight)path.addRect(left,y,right,y+1,android.graphics.Path.Direction.CW);
                else{path.addRect(left,y,insideLeft,y+1,android.graphics.Path.Direction.CW);path.addRect(insideRight,y,right,y+1,android.graphics.Path.Direction.CW);}
            }return path;
        }
        private void band(Canvas canvas,android.graphics.Path shape,int color){paint.setColor(color);paint.setAlpha(alpha);canvas.drawPath(shape,paint);}
        @Override public void draw(Canvas canvas){
            if(getBounds().isEmpty())return;int size=Math.min(getBounds().width(),getBounds().height()),save=canvas.save();canvas.translate(getBounds().left,getBounds().top+(getBounds().height()-size)/2f);canvas.scale(size/30f,size/30f);
            paint.setStyle(Paint.Style.FILL);band(canvas,border,Color.BLACK);if(enabled&&!pressed)band(canvas,inner,Color.WHITE);if(checked)band(canvas,dot,enabled?Color.BLACK:0xff808080);if(focused)indicatorFocus(canvas,this,paint,alpha,focus);canvas.restoreToCount(save);if(focused)captionFocus(canvas,this,paint,alpha);
        }
        @Override public int getIntrinsicWidth(){return WmMetrics.INDICATOR;}@Override public int getIntrinsicHeight(){return WmMetrics.INDICATOR;}
        @Override public void setAlpha(int value){alpha=Math.max(0,Math.min(255,value));invalidateSelf();}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    private static final class CheckMark extends Drawable {
        private final boolean listStyle;
        private final android.graphics.Path listTick;
        private CheckMark(){this(false);}
        private CheckMark(boolean listStyle){this.listStyle=listStyle;listTick=listStyle?listTick():null;}
        private static android.graphics.Path listTick(){
            // Native DrawFrameControl: 16-pixel inner rect, integer PATCOPY bands.
            android.graphics.Path path=new android.graphics.Path();
            for(int x=8;x<=11;x++){int y=15+x-8;path.addRect(x,y,x+1,y+6,android.graphics.Path.Direction.CW);}
            for(int x=12;x<=22;x++){int y=19-(x-12);path.addRect(x,y,x+1,y+6,android.graphics.Path.Direction.CW);}
            return path;
        }
        // Actual stock Button bitmap4 row2; parent brush remains visible when inactive/pressed.
        private static final int[] TICK={22,8,24,9,21,9,24,10,20,10,24,11,19,11,24,12,6,12,8,13,18,12,24,13,6,13,9,14,17,13,24,14,6,14,10,15,16,14,23,15,6,15,11,16,15,15,22,16,6,16,12,17,14,16,21,17,6,17,20,18,7,18,19,19,8,19,18,20,9,20,17,21,10,21,16,22,11,22,15,23,12,23,14,24};
        private static final int[] MIXED_OUTLINE={22,8,24,9,21,9,24,10,20,10,22,11,23,10,24,11,19,11,21,12,23,11,24,12,6,12,8,13,18,12,20,13,23,12,24,13,6,13,9,14,17,13,19,14,22,13,24,14,6,14,7,15,8,14,10,15,16,14,18,15,21,14,23,15,6,15,7,16,9,15,11,16,15,15,17,16,20,15,22,16,6,16,7,17,10,16,12,17,14,16,16,17,19,16,21,17,6,17,8,18,11,17,15,18,18,17,20,18,7,18,9,19,12,18,14,19,17,18,19,19,8,19,10,20,16,19,18,20,9,20,11,21,15,20,17,21,10,21,12,22,14,21,16,22,11,22,15,23,12,23,14,24};
        private static android.graphics.Path shape(int[] rects){android.graphics.Path path=new android.graphics.Path();for(int i=0;i<rects.length;i+=4)path.addRect(rects[i],rects[i+1],rects[i+2],rects[i+3],android.graphics.Path.Direction.CW);return path;}
        private final android.graphics.Path tick=shape(TICK),mixedOutline=shape(MIXED_OUTLINE),border=border(),focus=indicatorFocusMask(false);
        // Explicit border bands keep the interior transparent across Android rasterizers.
        private static android.graphics.Path border(){return shape(new int[]{0,0,30,2,0,28,30,30,0,2,2,28,28,2,30,28});}
        private final Paint paint=new Paint();private boolean checked,enabled=true,pressed,focused;private int alpha=255;
        @Override public boolean isStateful(){return true;}
        @Override protected boolean onStateChange(int[] states){
            boolean c=false,e=false,p=false,f=false;if(states!=null)for(int state:states){if(state==android.R.attr.state_checked)c=true;else if(state==android.R.attr.state_enabled)e=true;else if(state==android.R.attr.state_pressed)p=true;else if(state==android.R.attr.state_focused)f=true;}
            if(listStyle){if(c==checked)return false;checked=c;invalidateSelf();return true;}
            if(c==checked&&e==enabled&&p==pressed&&f==focused)return false;checked=c;enabled=e;pressed=p;focused=f;invalidateSelf();return true;
        }
        @Override public void draw(Canvas canvas){
            if(listStyle){
                if(getBounds().isEmpty())return;int saved=canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/32f,getBounds().height()/32f);
                paint.setStyle(Paint.Style.FILL);paint.setColor(Color.BLACK);paint.setAlpha(alpha);canvas.drawRect(2,2,30,30,paint);
                paint.setColor(Color.WHITE);paint.setAlpha(alpha);canvas.drawRect(4,4,28,28,paint);
                if(checked){
                    paint.setColor(Color.BLACK);paint.setAlpha(alpha);canvas.drawPath(listTick,paint);
                }
                canvas.restoreToCount(saved);return;
            }
            if(getBounds().isEmpty())return;int saved=canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/30f,getBounds().height()/30f);
            paint.setStyle(Paint.Style.FILL);paint.setColor(Color.BLACK);paint.setAlpha(alpha);canvas.drawPath(border,paint);
            if(enabled&&!pressed){paint.setColor(Color.WHITE);paint.setAlpha(alpha);canvas.drawRect(2,2,28,28,paint);}
            boolean mixed=getCallback() instanceof WmThreeStateCheck&&((WmThreeStateCheck)getCallback()).getCheckState()==WmThreeStateCheck.INDETERMINATE;
            if(mixed){paint.setColor(Color.WHITE);paint.setAlpha(alpha);canvas.drawPath(tick,paint);paint.setColor(0xff808080);canvas.drawPath(mixedOutline,paint);}
            else if(checked){paint.setColor(enabled?Color.BLACK:0xff808080);paint.setAlpha(alpha);canvas.drawPath(tick,paint);}
            if(focused)indicatorFocus(canvas,this,paint,alpha,focus);
            canvas.restoreToCount(saved);if(focused)captionFocus(canvas,this,paint,alpha);
        }
        @Override public int getIntrinsicWidth(){return listStyle?32:WmMetrics.INDICATOR;}@Override public int getIntrinsicHeight(){return listStyle?32:WmMetrics.INDICATOR;}
        @Override public void setAlpha(int value){alpha=Math.max(0,Math.min(255,value));invalidateSelf();}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
