package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import java.text.Collator;
import java.util.Locale;

/** Closed single-selection field. Caller supplies labels and owns the popup.
 * Frame2/gutter28/down-arrow geometry is provisional; native combo DLU/font
 * conversion has not been verified. No platform Spinner, ripple or animation. */
public final class WmChoice extends TextView implements WmControls.RestyleListener {
    public interface OnChoiceListener {void chosen(int index);}
    public interface OnOpenListener {void open(WmChoice source);}
    private String[] choices;
    private int selected,selection;
    private boolean extendedUi;
    private boolean pimInline;
    private Locale displaySortLocale;
    private int displayPinnedPrefix;
    private int[] displayOrder;
    private Collator characterComparator;
    private final SparseBooleanArray searchKeys=new SparseBooleanArray();
    private OnChoiceListener choiceListener;
    private OnOpenListener openListener;
    private final ChoiceFrame frame;
    final WmComboGlyph dropdown=new WmComboGlyph(this);

    public WmChoice(Context context,Typeface font,String[] choices,int selected,int selection){
        super(context);this.selection=selection;
        characterComparator=Collator.getInstance(Locale.getDefault());characterComparator.setStrength(Collator.SECONDARY);
        WmControls.textStyle(this,font);setGravity(Gravity.TOP|Gravity.LEFT);setSingleLine(true);
        // A2300/C2640 force WS_BORDER; A1DE8 caption starts at client inset4.
        // A2648 uses ETO_CLIPPED4 without modifying the selected label.
        // C2638 draws at caption.top, not at the center of the outer row.
        setEllipsize(null);setPadding(WmMetrics.BORDER+WmMetrics.EDGE,WmMetrics.BORDER+WmMetrics.EDGE/2,32,2);
        setMinWidth(0);setMinimumWidth(0);setMinHeight(WmMetrics.comboHeight(0));setMinimumHeight(WmMetrics.comboHeight(0));
        setSoundEffectsEnabled(false);setFocusable(true);setFocusableInTouchMode(true);
        frame=new ChoiceFrame(this);setBackgroundDrawable(frame);
        if(Build.VERSION.SDK_INT>=21){setBackgroundTintList(null);setStateListAnimator(null);setElevation(0);}
        updateColors();setChoices(choices,selected);
        setOnClickListener(v->{if(isEnabled()&&openListener!=null)openListener.open(this);});
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        super.onMeasure(widthSpec,heightSpec);
        if(View.MeasureSpec.getMode(heightSpec)!=View.MeasureSpec.EXACTLY){
            int height=Math.max(getMeasuredHeight(),WmMetrics.comboHeight(WmRadioMenu.choiceRowHeight(getPaint())));
            setMeasuredDimension(getMeasuredWidthAndState(),resolveSizeAndState(height,heightSpec,getMeasuredState()<<MEASURED_HEIGHT_STATE_SHIFT));
        }
    }
    // PIM card owns the 39px row; ordinary standalone combos keep their original face.
    void setPimInline(){pimInline=true;setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);setPadding(4,0,32,0);setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,28);invalidate();}
    @Override public boolean onTouchEvent(MotionEvent event){
        // Native combo takes focus on DOWN. Otherwise Android's first UP only
        // focuses this touch-focusable field and skips its click callback.
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN&&isEnabled()&&event.getX()>=0&&event.getX()<getWidth()&&event.getY()>=0&&event.getY()<getHeight())requestFocus();
        return super.onTouchEvent(event);
    }
    private static boolean navigationKey(int key){return key==KeyEvent.KEYCODE_MOVE_HOME||key==KeyEvent.KEYCODE_MOVE_END;}
    @Override public boolean onKeyDown(int key,KeyEvent event){
        // A89D8 delivered ordinary keys: closed extended Up ignores, Down opens;
        // Home/End/Page ignore and F4 ignores even when shown. Editable9B448 differs.
        if(isEnabled()&&extendedUi&&event.hasNoModifiers()&&(key==KeyEvent.KEYCODE_F4||!isDroppedDown()&&(navigationKey(key)||key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_PAGE_UP||key==KeyEvent.KEYCODE_PAGE_DOWN))){
            if(key==KeyEvent.KEYCODE_DPAD_DOWN&&!isPressed())performClick();
            searchKeys.put(key,true);return true;
        }
        if(isEnabled()&&WmControls.isComboToggleKey(key,event)){
            if(isPressed())return true;
            boolean handled=isDroppedDown()?dropdown.key(event):performClick();
            if(handled){searchKeys.put(key,true);return true;}
        }
        if(isEnabled()&&!isDroppedDown()){
            if(navigationKey(key)&&event.hasNoModifiers()){
                // A38F4 forwards Home/End to the list; C2654 leaves closed arrows to the host.
                int next=key==KeyEvent.KEYCODE_MOVE_HOME?0:choices.length-1;
                chooseDisplayIndex(next);return true;
            }
            if((event.getMetaState()&(KeyEvent.META_CTRL_MASK|KeyEvent.META_ALT_MASK|KeyEvent.META_META_MASK))==0){
                int value=event.getUnicodeChar();
                if(key==KeyEvent.KEYCODE_DEL)value=8;
                else if(key==KeyEvent.KEYCODE_ESCAPE)value=27;
                if((value>=32&&value<=0xffff||value==8||value==27)&&searchCharacter((char)value)){
                    searchKeys.put(key,true);return true;
                }
            }
        }
        return super.onKeyDown(key,event);
    }
    @Override public boolean onKeyUp(int key,KeyEvent event){
        if(searchKeys.get(key)){searchKeys.delete(key);return true;}
        if(isEnabled()&&!isDroppedDown()&&navigationKey(key)&&event.hasNoModifiers())return true;
        return super.onKeyUp(key,event);
    }
    @Override public boolean onKeyMultiple(int key,int repeat,KeyEvent event){
        String text=WmCharacterSearch.characters(event);if(text==null)return super.onKeyMultiple(key,repeat,event);
        boolean handled=false;for(int i=0;i<text.length();i++){if(!searchCharacter(text.charAt(i)))break;handled=true;}return handled;
    }
    /** Closed WM_CHAR search, one UTF-16 code unit at a time.
     * Character.toUpperCase and system-locale Collator SECONDARY approximate native
     * CharUpper/CompareStringW NLS; IME and dead-key composition remain unsupported.
     * ACTION_MULTIPLE character strings use this same unit-by-unit path.
     * False when disabled, dropped or pressed; otherwise true even without a match.
     * A match commits and synchronously invokes the chosen callback. */
    public boolean searchCharacter(char value){
        if(!isEnabled()||isDroppedDown()||isPressed())return false;
        int match=WmCharacterSearch.search(value,choices,displayOrder,displaySelectedIndex(),displaySortLocale!=null,characterComparator);
        if(match>=0)chooseCharacterDisplayIndex(match);
        return true;
    }
    /** Character-search commit; unlike ordinary choose, a repeated unique match notifies. */
    public void chooseCharacterDisplayIndex(int index){checkIndex(index);if(!isEnabled())return;int source=displayOrder[index];updateSelection(source);if(choiceListener!=null)choiceListener.chosen(source);}
    /** Search locale for an explicitly enabled ordinary dropdown list. Null means unsorted. */
    public Locale displaySortLocale(){return displaySortLocale;}
    @Override protected void onFocusChanged(boolean focused,int direction,android.graphics.Rect old){super.onFocusChanged(focused,direction,old);if(!focused){WmCharacterSearch.reset();if(searchKeys!=null)searchKeys.clear();}}
    public void setChoices(String[] choices,int selected){
        if(choices==null||choices.length==0)throw new IllegalArgumentException("Choices required");
        if(selected<0||selected>=choices.length)throw new IndexOutOfBoundsException("Invalid choice index");
        String[] copy=choices.clone();for(String label:copy)if(label==null)throw new IllegalArgumentException("Null choice label");
        int[] order=displayOrder(copy,displaySortLocale,displayPinnedPrefix);
        this.choices=copy;displayOrder=order;this.selected=selected;setText(copy[selected]);invalidate();syncPopupChoices();
    }
    /** A defensive label copy for the caller-owned popup. */
    public String[] choices(){return choices.clone();}
    public int selectedIndex(){return selected;}
    /** Opt-in CBS_SORT display order; caller arrays, indexes and callbacks retain source identity.
     * The pinned prefix is excluded from sorted insertion (DocList pins source0/None).
     * Android Collator SECONDARY approximates CompareStringW(system locale,NORM_IGNORECASE);
     * bracket-last groups and immediate equal-midpoint insertion follow GWES A4C7C. */
    public void setDisplaySort(Locale locale,int pinnedPrefixCount){
        if(locale==null)throw new IllegalArgumentException("Sort locale required");
        int[] order=displayOrder(choices,locale,pinnedPrefixCount);
        displaySortLocale=locale;displayPinnedPrefix=pinnedPrefixCount;displayOrder=order;invalidate();
        characterComparator=Collator.getInstance(locale);characterComparator.setStrength(Collator.SECONDARY);
        syncPopupChoices();
    }
    public String[] displayChoices(){String[] labels=new String[displayOrder.length];for(int i=0;i<labels.length;i++)labels[i]=choices[displayOrder[i]];return labels;}
    public int displaySelectedIndex(){for(int i=0;i<displayOrder.length;i++)if(displayOrder[i]==selected)return i;return -1;}
    public void chooseDisplayIndex(int index){checkIndex(index);choose(displayOrder[index]);}
    private static int[] displayOrder(String[] labels,Locale locale,int pinned){
        if(pinned<0||pinned>labels.length)throw new IllegalArgumentException("Pinned prefix outside choices");
        int[] order=new int[labels.length];
        if(locale==null){for(int i=0;i<order.length;i++)order[i]=i;return order;}
        Collator comparator=Collator.getInstance(locale);comparator.setStrength(Collator.SECONDARY);
        for(int i=0;i<pinned;i++)order[i]=i;
        int size=pinned;
        for(int source=pinned;source<labels.length;source++){
            int low=pinned,high=size-1;
            while(low<=high){int middle=(low+high)>>>1;int compared=compareDisplay(labels[source],labels[order[middle]],comparator);if(compared==0){low=middle;break;}if(compared<0)high=middle-1;else low=middle+1;}
            System.arraycopy(order,low,order,low+1,size-low);order[low]=source;size++;
        }
        return order;
    }
    static int compareDisplay(String a,String b,Collator comparator){boolean aBracket=!a.isEmpty()&&a.charAt(0)=='[',bBracket=!b.isEmpty()&&b.charAt(0)=='[';return aBracket==bBracket?comparator.compare(a,b):aBracket?1:-1;}
    /** Native CB_ERR state: blank closed field and no checked popup item.
     * The owner retains its raw value until a valid choice is committed. */
    public void clearSelection(){updateSelection(-1);syncPopupSelection();}
    public void setSelectedIndex(int index){checkIndex(index);updateSelection(index);syncPopupSelection();}
    private void updateSelection(int index){selected=index;setText(index<0?"":choices[index]);invalidate();}
    private void syncPopupSelection(){WmRadioMenu menu=WmComboGlyph.choiceMenu(dropdown.popup());if(menu!=null)menu.syncChoiceSelection(displaySelectedIndex());}
    private void syncPopupChoices(){WmRadioMenu menu=WmComboGlyph.choiceMenu(dropdown.popup());if(menu!=null)menu.replaceChoiceCandidates(displayChoices(),displaySelectedIndex(),displaySortLocale);}
    /** Commit a completed popup selection. Programmatic setters are silent. */
    public void choose(int index){
        checkIndex(index);if(!isEnabled()||index==selected)return;
        updateSelection(index);if(choiceListener!=null)choiceListener.chosen(index);
    }
    /** Opt-in native extended policy for keys delivered to this ordinary combo.
     * Closed Up is ignored, Down opens, closed Home/End/Page and all F4 are ignored.
     * This does not implement WinCE WM_GETDLGCODE/parent dialog pre-dispatch. */
    public void setExtendedUi(boolean enabled){extendedUi=enabled;}
    public boolean isExtendedUi(){return extendedUi;}
    public void bindDropdown(View popup){if(popup!=null)WmCharacterSearch.reset();dropdown.bind(popup);}
    public boolean isDroppedDown(){return dropdown.isDropped();}
    /** Bind shared combo/date controls; other anchors retain caller-owned behavior. */
    public static void bindDropdown(View anchor,View popup){
        if(anchor instanceof WmChoice)((WmChoice)anchor).bindDropdown(popup);
        else if(anchor instanceof WmEditableChoice)((WmEditableChoice)anchor).bindDropdown(popup);
        else if(anchor instanceof WmDateField)((WmDateField)anchor).bindDropdown(popup);
    }
    @Override public void setEnabled(boolean enabled){super.setEnabled(enabled);if(!enabled&&dropdown!=null){dropdown.clear();searchKeys.clear();WmCharacterSearch.reset();}}
    @Override protected void onDetachedFromWindow(){dropdown.clear();searchKeys.clear();WmCharacterSearch.reset();super.onDetachedFromWindow();}
    public void setOnChoiceListener(OnChoiceListener listener){choiceListener=listener;}
    public void setOnOpenListener(OnOpenListener listener){openListener=listener;}
    private void checkIndex(int index){if(index<0||index>=choices.length)throw new IndexOutOfBoundsException("Invalid choice index");}
    // A2648 highlights only focused, closed ordinary combos; a pressed arrow
    // or visible dropdown does not highlight the closed caption.
    private boolean captionFocused(){return !pimInline&&isEnabled()&&isFocused()&&(dropdown==null||!dropdown.isDropped());}
    private void updateColors(){int color=!isEnabled()?WmControls.SHADOW:captionFocused()?Color.WHITE:Color.BLACK;if(getCurrentTextColor()!=color)setTextColor(color);}
    @Override protected void drawableStateChanged(){super.drawableStateChanged();updateColors();}
    @Override protected void onDraw(Canvas canvas){
        updateColors();int saved=canvas.save();
        // Some Android TextView versions extend the text clip into the arrow gutter.
        // A2648 confines the selected label to the caption; the frame draws separately.
        canvas.clipRect(getScrollX()+getPaddingLeft(),getScrollY(),getScrollX()+getWidth()-getPaddingRight(),getScrollY()+getHeight());
        super.onDraw(canvas);canvas.restoreToCount(saved);
    }
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        this.selection=selection;setTypeface(regular);WmFonts.attach(this,regular);updateColors();frame.invalidateSelf();
    }
    private static final class ChoiceFrame extends Drawable {
        private final WmChoice owner;
        private final Paint paint=new Paint();
        private int alpha=255;
        ChoiceFrame(WmChoice owner){this.owner=owner;}
        @Override public boolean isStateful(){return true;}
        @Override protected boolean onStateChange(int[] state){invalidateSelf();return true;}
        private void color(int color){paint.setColor(color);paint.setAlpha(alpha);}
        @Override public void draw(Canvas canvas){
            int left=getBounds().left,top=getBounds().top,right=getBounds().right,bottom=getBounds().bottom;
            if(right<=left||bottom<=top)return;
            boolean active=owner.captionFocused();
            boolean pressed=owner.isEnabled()&&(owner.isPressed()||owner.dropdown.isDropped());
            int divider=Math.max(left,right-28);
            if(owner.pimInline&&!owner.isFocused()&&!owner.dropdown.isDropped())return;
            paint.setStyle(Paint.Style.FILL);color(Color.WHITE);canvas.drawRect(left,top,right,bottom,paint);
            if(owner.pimInline&&!owner.dropdown.isDropped()){
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);color(WmControls.FRAME);canvas.drawRect(left+1,top+1,right-1,bottom-1,paint);return;
            }
            // A1DE8/C2588: highlight the caption rect, leaving its surrounding
            // client inset white. Android font height adapts the native0 extent.
            if(active){
                int captionTop=top+Math.max(WmMetrics.BORDER,owner.getPaddingTop());
                int captionBottom=Math.min(bottom-Math.max(WmMetrics.BORDER,owner.getPaddingBottom()),captionTop+Math.max(32,WmRadioMenu.choiceRowHeight(owner.getPaint())));
                color(WmPalette.highlight(owner.selection));canvas.drawRect(left+Math.max(WmMetrics.BORDER,owner.getPaddingLeft()),captionTop,Math.min(divider-WmMetrics.BORDER,right-owner.getPaddingRight()),captionBottom,paint);
            }
            color(pressed?Color.BLACK:WmControls.SURFACE);canvas.drawRect(divider,top+2,right-2,bottom-2,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);color(WmControls.FRAME);
            canvas.drawRect(left+1,top+1,right-1,bottom-1,paint);canvas.drawLine(divider,top+2,divider,bottom-2,paint);
            paint.setStyle(Paint.Style.FILL);color(!owner.isEnabled()?WmControls.SHADOW:pressed?Color.WHITE:Color.BLACK);
            WmComboGlyph.draw(canvas,paint,divider,top+2,right-2,bottom-2,pressed);
        }
        @Override public void setAlpha(int alpha){this.alpha=Math.max(0,Math.min(255,alpha));invalidateSelf();}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
