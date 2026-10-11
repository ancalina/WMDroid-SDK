package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.graphics.Rect;
import android.view.View;
import android.util.SparseBooleanArray;
import java.text.Collator;
import java.util.Locale;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared command/radio popup body. Row44/padding32,12/dot8 remain provisional VGA
 * metrics. The14px submenu mask/placement trace GWES BITMAP5 at192dpi
 * with default WidgSz=0; separator9px/1px rule remains an adaptation.
 * Selection callbacks use entry indices. */
public final class WmRadioMenu extends LinearLayout implements WmControls.RestyleListener {
    public interface Selection { void choose(int index); }
    private int menuWidth;
    private WmMenuItem[] entries;
    private Label[] labels;
    private Typeface menuFont;
    private int selectionColor;
    private final Selection touchAction;
    private Selection keyboardSelection;
    View choiceOwner;
    private Runnable keyboardClose;
    private int choiceIndex,choiceCaret,mutationCaret=-1;
    private Selection characterSelection;
    private String[] searchLabels;
    private Collator characterComparator;
    private boolean sortedSearch;
    private final SparseBooleanArray characterKeys=new SparseBooleanArray();
    private boolean choiceTouchActive;
    private String selectedSuffix;
    private Bitmap[] rowIcons;
    private boolean iconBold;
    private float iconTextSize;
    public WmRadioMenu(Context context,Typeface font,String[] labels,int selected,String selectedSuffix,int selection,Selection action){
        this(context,font,legacy(labels,selected),selectedSuffix,selection,action,true);
    }
    private static WmMenuItem[] legacy(String[] labels,int selected){
        if(labels==null||selected < -1||selected>=labels.length)throw new IllegalArgumentException("Invalid radio menu");
        WmMenuItem[] items=new WmMenuItem[labels.length];for(int i=0;i<labels.length;i++)items[i]=WmMenuItem.checked(i,labels[i],i==selected);return items;
    }
    public WmRadioMenu(Context context,Typeface font,WmMenuItem[] items,String selectedSuffix,int selection,Selection action){
        this(context,font,items,selectedSuffix,selection,action,false);
    }
    private WmRadioMenu(Context context,Typeface font,WmMenuItem[] items,String selectedSuffix,int selection,Selection action,boolean allowEmpty){
        super(context);if(items==null||!allowEmpty&&items.length==0||action==null)throw new IllegalArgumentException("Invalid command menu");
        menuFont=font;selectionColor=selection;touchAction=action;
        this.selectedSuffix=selectedSuffix==null?"":selectedSuffix;
        entries=items.clone();labels=new Label[entries.length];setOrientation(VERTICAL);setPadding(2,2,2,2);
        GradientDrawable border=new GradientDrawable();border.setColor(Color.WHITE);border.setStroke(2,Color.BLACK);setBackgroundDrawable(border);
        buildRows();
    }
    private void buildRows(){buildRows(0,entries.length);}
    private void buildRows(int first,int limit){
        Context context=getContext();Typeface font=menuFont;int selection=selectionColor;Selection action=touchAction;
        for(int i=first;i<limit;i++){
            WmMenuItem entry=entries[i];if(entry==null)throw new IllegalArgumentException("Null menu entry");
            FrameLayout row=new FrameLayout(context);
            if(entry.separator){View line=new View(context);line.setBackgroundColor(0xff808080);FrameLayout.LayoutParams rule=new FrameLayout.LayoutParams(-1,1,Gravity.CENTER_VERTICAL);rule.leftMargin=8;rule.rightMargin=8;row.addView(line,rule);addView(row,new LayoutParams(-1,9));continue;}
            Label item=new Label(context);item.index=i;labels[i]=item;WmControls.textStyle(item,font);item.setText(entry.label);WmControls.entry(item,selection);
            item.setPadding(32,0,entry.hasChildren()?24:12,0);item.setSingleLine(true);item.setEllipsize(android.text.TextUtils.TruncateAt.END);item.setSoundEffectsEnabled(false);
            item.setContentDescription(entry.label+(entry.checked?selectedSuffix:""));item.setEnabled(entry.enabled);
            if(entry.enabled){
                item.setOnClickListener(v->{int index=item.index;if(isEnabled()&&index>=0&&index<labels.length&&labels[index]==v)action.choose(index);});
                // GWES popup rows open their child on DOWN; consume UP without reopening.
                if(entry.hasChildren())item.setOnTouchListener((v,event)->{
                    int index=item.index;if(index<0||index>=labels.length||labels[index]!=v)return true;
                    int type=event.getActionMasked();
                    if(type==android.view.MotionEvent.ACTION_UP||type==android.view.MotionEvent.ACTION_CANCEL||type==android.view.MotionEvent.ACTION_POINTER_DOWN)v.setPressed(false);
                    else if(type==android.view.MotionEvent.ACTION_DOWN&&isEnabled()){v.setPressed(true);action.choose(index);}
                    return true;
                });
            }
            row.addView(item,new FrameLayout.LayoutParams(-1,44));
            if(entry.checked)addMark(context,row,item,false);
            if(entry.hasChildren())addMark(context,row,item,true);
            addView(row,i,new LayoutParams(-1,44));
            if(keyboardSelection!=null)bindChoiceTouch(item);
        }
        measureWidth();
    }
    private static void addMark(Context context,FrameLayout row,Label label,boolean arrow){
        Mark mark=new Mark(context,label,arrow);label.marks.add(mark);
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(arrow?14:8,arrow?14:8,arrow?Gravity.RIGHT:Gravity.LEFT);p.topMargin=arrow?14:18;if(arrow)p.rightMargin=5;else p.leftMargin=12;row.addView(mark,p);
    }
    /** DocList owner-drawn command rows at192dpi:36px high, icon(4,2),
     * text left40, measured text+60 width. Icons replace radio marks, not state.
     * Separators require a null icon. Caller retains bitmap ownership. */
    public void setIconRows(Bitmap[] icons,Typeface regular,Typeface bold,boolean useBold,float textSize,int selection){
        if(keyboardSelection!=null||regular==null||bold==null||icons==null||icons.length!=entries.length||textSize<=0||Float.isInfinite(textSize)||Float.isNaN(textSize))throw new IllegalArgumentException("Invalid icon rows");
        for(int i=0;i<entries.length;i++)if(entries[i].hasChildren()||(entries[i].separator?icons[i]!=null:icons[i]==null||icons[i].isRecycled()))throw new IllegalArgumentException("Icon required for each command");
        rowIcons=icons.clone();iconBold=useBold;iconTextSize=textSize;setPadding(1,1,1,1);
        for(int i=0;i<labels.length;i++){
            FrameLayout row=(FrameLayout)getChildAt(i);Label label=labels[i];
            if(label==null){View line=row.getChildAt(0);line.setBackgroundColor(Color.BLACK);FrameLayout.LayoutParams rule=(FrameLayout.LayoutParams)line.getLayoutParams();rule.leftMargin=11;rule.rightMargin=15;line.setLayoutParams(rule);continue;}
            for(Mark mark:label.marks)row.removeView(mark);label.marks.clear();label.icon=rowIcons[i];label.setPadding(40,0,8,0);label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,textSize);
            label.setLayoutParams(new FrameLayout.LayoutParams(-1,36));row.setLayoutParams(new LayoutParams(-1,36));
        }
        onWmRestyle(regular,bold,selection,WmPalette.face(selection));
    }
    private void styleIconRows(int selection){
        GradientDrawable border=new GradientDrawable();border.setColor(WmPalette.face(selection));border.setStroke(1,Color.BLACK);setBackgroundDrawable(border);
        for(Label label:labels)if(label!=null){
            android.graphics.drawable.StateListDrawable background=new android.graphics.drawable.StateListDrawable();
            for(int state:new int[]{android.R.attr.state_pressed,android.R.attr.state_selected,android.R.attr.state_focused})background.addState(new int[]{state},new android.graphics.drawable.ColorDrawable(WmPalette.highlight(selection)));
            background.addState(new int[]{},new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));label.setBackgroundDrawable(background);
            label.setTextColor(new android.content.res.ColorStateList(new int[][]{{-android.R.attr.state_enabled},{android.R.attr.state_pressed},{android.R.attr.state_selected},{android.R.attr.state_focused},{}},new int[]{0xff808080,Color.WHITE,Color.WHITE,Color.WHITE,Color.BLACK}));
        }
    }
    /** Opt-in keyboard behavior for an ordinary combo list, not a command menu.
     * GWES A89D8/A300C commits navigation immediately without closing; Escape
     * keeps that last committed value. Touch still uses the constructor callback.
     * ponytail: native typeahead/owner-draw/multiselect are not implemented here. */
    public void enableChoiceKeyboard(int selected,Selection live,Runnable close){
        if(rowIcons!=null||selected < -1||selected>=entries.length||live==null||close==null)throw new IllegalArgumentException("Invalid choice keyboard");
        for(WmMenuItem entry:entries)if(entry.separator||entry.hasChildren()||!entry.enabled)throw new IllegalArgumentException("Ordinary choices required");
        choiceIndex=selected;choiceCaret=entries.length==0?0:selected;choiceTouchActive=false;keyboardSelection=live;keyboardClose=close;setFocusable(true);setFocusableInTouchMode(true);
        for(Label label:labels)bindChoiceTouch(label);
        styleChoiceRows();showChoice(selected);measureWidth();
    }
    private void bindChoiceTouch(Label label){
        label.setOnTouchListener((view,event)->{
            int index=label.index;
            if(index<0||index>=labels.length||labels[index]!=view)return true;
            int action=event.getActionMasked();
            if(action==android.view.MotionEvent.ACTION_DOWN&&isEnabled())choiceTouchActive=true;
            else if(action==android.view.MotionEvent.ACTION_UP||action==android.view.MotionEvent.ACTION_CANCEL)choiceTouchActive=false;
            // UP/CANCEL still reach View to clear an already-pressed row.
            return !isEnabled()&&action!=android.view.MotionEvent.ACTION_UP&&action!=android.view.MotionEvent.ACTION_CANCEL;
        });
    }
    /** Explicit ordinary-list WM_CHAR search. Null locale cycles unsorted labels;
     * non-null locale accumulates sorted queries. The owner guards callback lifetime
     * and maps display indices; command menus and editable IME keep their own input. */
    public void enableChoiceCharacterSearch(Locale sortedLocale,Selection live){
        if(keyboardSelection==null||live==null)throw new IllegalArgumentException("Choice keyboard required");
        searchLabels=new String[entries.length];for(int i=0;i<entries.length;i++)searchLabels[i]=entries[i].label;
        sortedSearch=sortedLocale!=null;characterComparator=Collator.getInstance(sortedSearch?sortedLocale:Locale.getDefault());characterComparator.setStrength(Collator.SECONDARY);
        characterSelection=live;WmCharacterSearch.reset();
    }
    /** Returns whether input can be processed, not whether it matches.
     * Requires prepared search, enabled/attached/shown content and no touch capture.
     * A match synchronously invokes the callback with its display-order index. */
    public boolean searchCharacter(char value){
        if(characterSelection==null||!isEnabled()||choiceTouchActive||getWindowToken()==null||!isShown())return false;
        int match=WmCharacterSearch.search(value,searchLabels,null,choiceIndex,sortedSearch,characterComparator);
        if(match>=0){choiceIndex=choiceCaret=match;showChoice(match);characterSelection.choose(match);}return true;
    }
    boolean keepEditableFocus(){
        if(keyboardSelection==null)return false;
        setFocusableInTouchMode(false);setFocusable(false);return true;
    }
    boolean choiceKeyboardEnabled(){return keyboardSelection!=null;}
    int choiceSelectedIndex(){return choiceIndex;}
    int choiceCaretIndex(){return choiceCaret;}
    /** CB_SETCURSEL: silent selection and top index; clear retains the caret and top. */
    boolean syncChoiceSelection(int index){
        if(keyboardSelection==null||index < -1||index>=entries.length)return false;
        choiceIndex=index;
        if(index>=0)choiceCaret=index;
        else choiceCaret=Math.max(0,Math.min(choiceCaret,entries.length-1));
        showChoice(index);if(index>=0)scrollChoiceTop(index);return true;
    }
    /** A2958(edit): no selected row; matching caret and top index are separate. */
    boolean syncEditableQuery(String query){
        // Programmatic list messages still apply to disabled native controls.
        if(keyboardSelection==null)return false;
        String[] values=new String[entries.length];for(int i=0;i<values.length;i++)values[i]=entries[i].label;
        choiceCaret=Math.max(0,WmEditableChoice.findCandidate(values,query,false));choiceIndex=-1;showChoice(-1);
        // Use the opened list's labels, even if its owner reordered candidates.
        // Ordinary choice rows share the font-derived fixed height. Child
        // bounds are still zero before layout, or stale after a live restyle.
        scrollChoiceTop(choiceCaret);return true;
    }
    private void scrollChoiceTop(int index){
        int top=entries.length==0?0:index*getChildAt(0).getLayoutParams().height;
        if(getParent() instanceof android.widget.ScrollView){
            android.widget.ScrollView scroll=(android.widget.ScrollView)getParent();
            if(scroll.getParent() instanceof WmScrollView)((WmScrollView)scroll.getParent()).scrollToPosition(top);
            else scroll.scrollTo(0,top);
        }
    }
    /** Ordinary bulk reset/add/select adapter retains this popup and its callbacks. */
    void replaceChoiceCandidates(String[] candidates,int selected,Locale locale){
        if(keyboardSelection==null)return;
        mutationCaret=-1;entries=legacy(candidates,selected);labels=new Label[entries.length];removeAllViews();buildRows();
        enableChoiceKeyboard(selected,keyboardSelection,keyboardClose);choiceCaret=Math.max(0,selected);characterKeys.clear();
        if(characterSelection!=null)enableChoiceCharacterSearch(locale,characterSelection);else WmCharacterSearch.reset();
        scrollChoiceTop(choiceCaret);requestLayout();
    }
    /** Bulk editable-owner replacement retains this popup and its close/action callbacks.
     * Incremental native insert/delete selection rules are a separate operation. */
    void replaceEditableCandidates(String[] candidates){
        if(keyboardSelection==null)return;
        mutationCaret=-1;WmMenuItem[] next=legacy(candidates,-1);boolean editable=!isFocusable();
        entries=next;labels=new Label[next.length];removeAllViews();buildRows();
        enableChoiceKeyboard(-1,keyboardSelection,keyboardClose);if(editable)keepEditableFocus();
        if(characterSelection!=null){searchLabels=candidates.clone();WmCharacterSearch.reset();characterKeys.clear();}
        syncEditableQuery("");requestLayout();
    }
    boolean matchesEditableCandidates(String[] values){
        if(keyboardSelection==null||values.length!=entries.length)return false;
        for(int i=0;i<values.length;i++)if(!values[i].equals(entries[i].label))return false;
        return true;
    }
    /** Only the inserted/deleted row changes identity; callbacks use live indices. */
    void mutateEditableCandidates(String[] values,int selected,int caret,int index,boolean insert){
        Label[] old=labels,next=new Label[values.length];
        if(insert){System.arraycopy(old,0,next,0,index);System.arraycopy(old,index,next,index+1,old.length-index);}
        else{old[index].index=-1;removeViewAt(index);System.arraycopy(old,0,next,0,index);System.arraycopy(old,index+1,next,index,old.length-index-1);}
        entries=legacy(values,selected);labels=next;
        if(insert)buildRows(index,index+1);
        for(int i=0;i<labels.length;i++)labels[i].index=i;
        choiceIndex=selected;choiceCaret=caret;mutationCaret=insert?-1:caret;choiceTouchActive=false;characterKeys.clear();
        if(characterSelection!=null){searchLabels=values.clone();WmCharacterSearch.reset();}
        // Native mutation adjusts the list, without applying edit-prefix search.
        styleChoiceRows();showChoice(selected);measureWidth();requestLayout();
    }
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
        super.onLayout(changed,left,top,right,bottom);
        // A82DC ends with A7B80(caret): reveal even an unselected stored caret.
        int caret=mutationCaret;mutationCaret=-1;
        if(caret>=0&&caret<labels.length){Label label=labels[caret];label.requestRectangleOnScreen(new Rect(0,0,label.getWidth(),label.getHeight()),true);}
    }
    private void showChoice(int selected){
        for(int i=0;i<labels.length;i++){
            Label label=labels[i];FrameLayout row=(FrameLayout)label.getParent();
            for(int m=label.marks.size()-1;m>=0;m--)if(!label.marks.get(m).arrow){row.removeView(label.marks.remove(m));}

            label.setSelected(i==selected);label.setContentDescription(entries[i].label+(i==selected?selectedSuffix:""));
        }
        if(selected>=0)labels[selected].requestRectangleOnScreen(new Rect(0,0,labels[selected].getWidth(),labels[selected].getHeight()),true);
    }
    private void styleChoiceRows(){
        // GWES A7464/C2A90 uses tmHeight; A5204 draws at client-left+1,
        // row-top. Android ascent/descent substitute for GDI font metrics.
        for(Label label:labels){
            int height=choiceRowHeight(label.getPaint());
            label.setPadding(1,0,0,0);label.setGravity(Gravity.TOP|Gravity.LEFT);label.setEllipsize(null);
            FrameLayout row=(FrameLayout)label.getParent();
            label.setLayoutParams(new FrameLayout.LayoutParams(-1,height));row.setLayoutParams(new LayoutParams(-1,height));
        }
    }
    private static boolean choiceKey(int key){return key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_MOVE_HOME||key==KeyEvent.KEYCODE_MOVE_END||key==KeyEvent.KEYCODE_PAGE_UP||key==KeyEvent.KEYCODE_PAGE_DOWN||key==KeyEvent.KEYCODE_ESCAPE||WmControls.isEnterKey(key);}
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        // Editable combo list is a linked key target while the editor keeps
        // focus; ViewGroup otherwise ignores direct dispatch to this list.
        if(keyboardSelection!=null&&!isFocusable()){
            if(event.getAction()==KeyEvent.ACTION_DOWN)return onKeyDown(event.getKeyCode(),event);
            if(event.getAction()==KeyEvent.ACTION_UP)return onKeyUp(event.getKeyCode(),event);
            if(event.getAction()==KeyEvent.ACTION_MULTIPLE)return onKeyMultiple(event.getKeyCode(),event.getRepeatCount(),event);
        }
        return super.dispatchKeyEvent(event);
    }
    @Override public boolean onKeyMultiple(int key,int repeat,KeyEvent event){
        String text=WmCharacterSearch.characters(event);if(text==null)return super.onKeyMultiple(key,repeat,event);
        boolean handled=false;for(int i=0;i<text.length();i++){if(!searchCharacter(text.charAt(i)))break;handled=true;}return handled;
    }
    @Override public boolean onKeyDown(int key,KeyEvent event){
        if(keyboardSelection!=null&&isEnabled()&&WmControls.isComboToggleKey(key,event)){
            // A89D8 rejects F4 under the bound combo's extended flag, even while shown.
            if(key==KeyEvent.KEYCODE_F4&&(choiceOwner instanceof WmEditableChoice&&((WmEditableChoice)choiceOwner).isExtendedUi()||choiceOwner instanceof WmChoice&&((WmChoice)choiceOwner).isExtendedUi()))return true;
            // A89D8 routes F4 after the list pointer-capture guard; repeats also toggle.
            if(!choiceTouchActive&&getWindowToken()!=null&&isShown())keyboardClose.run();return true;
        }
        if(characterSelection!=null&&!choiceKey(key)&&(event.getMetaState()&(KeyEvent.META_CTRL_MASK|KeyEvent.META_ALT_MASK|KeyEvent.META_META_MASK))==0){
            int value=key==KeyEvent.KEYCODE_DEL?8:event.getUnicodeChar();
            if((value>=32&&value<=0xffff||value==8)&&(searchCharacter((char)value)||isEnabled()&&choiceTouchActive&&getWindowToken()!=null&&isShown())){characterKeys.put(key,true);return true;}
        }
        if(keyboardSelection==null||!isEnabled()||!choiceKey(key)||!event.hasNoModifiers())return super.onKeyDown(key,event);
        // Combo owner A38F4 closes on Return/Escape directly; list capture is separate.
        if(key==KeyEvent.KEYCODE_ESCAPE||WmControls.isEnterKey(key)){if(event.getRepeatCount()==0)keyboardClose.run();return true;}
        // GWES A89D8 blocks list navigation while capture bit0x100 is set.
        if(choiceTouchActive||entries.length==0)return true;
        int delta=key==KeyEvent.KEYCODE_DPAD_DOWN?1:-1;
        boolean pageKey=key==KeyEvent.KEYCODE_PAGE_UP||key==KeyEvent.KEYCODE_PAGE_DOWN;
        // A89D8 keeps zero page delta but still selects an unselected caret.
        if(pageKey){int step=choicePageStep();delta=key==KeyEvent.KEYCODE_PAGE_DOWN?step:-step;}
        int next=key==KeyEvent.KEYCODE_MOVE_HOME?0:key==KeyEvent.KEYCODE_MOVE_END?entries.length-1:Math.max(0,Math.min(entries.length-1,(pageKey?Math.max(0,choiceCaret):choiceCaret)+delta));
        // A89D8/A5478: the first arrow selects an unselected caret;
        // only an already-selected caret advances to its adjacent row.
        if((key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN)&&choiceIndex!=choiceCaret)next=Math.max(0,choiceCaret);
        if(next!=choiceIndex){choiceIndex=choiceCaret=next;showChoice(next);keyboardSelection.choose(next);}return true;
    }
    @Override public boolean onKeyUp(int key,KeyEvent event){
        if(characterKeys.get(key)){characterKeys.delete(key);return true;}
        if(keyboardSelection!=null&&isEnabled()&&WmControls.isComboToggleKey(key,event))return true;
        if(keyboardSelection!=null&&isEnabled()&&choiceKey(key)&&event.hasNoModifiers())return true;
        return super.onKeyUp(key,event);
    }
    int choicePageStep(){
        // Native GetClientRect counts logical client rows, not screen-scaled pixels.
        int clientHeight=getParent() instanceof android.widget.ScrollView?((View)getParent()).getHeight():getHeight();
        int height=Math.max(0,Math.min(clientHeight,getHeight())-getPaddingTop()-getPaddingBottom());
        return entries.length==0?0:choicePageStep(height,getChildAt(0).getHeight());
    }
    static int choiceRowHeight(Paint paint){Paint.FontMetricsInt metrics=paint.getFontMetricsInt();return Math.max(1,metrics.descent-metrics.ascent);}
    static int choicePageStep(int height,int rowHeight){
        int rows=rowHeight>0?height/rowHeight:0;
        return rows>1?rows-1:rows;
    }
    @Override public void setEnabled(boolean enabled){super.setEnabled(enabled);if(!enabled&&characterSelection!=null){WmCharacterSearch.reset();characterKeys.clear();}}
    @Override protected void onFocusChanged(boolean focused,int direction,Rect old){super.onFocusChanged(focused,direction,old);if(!focused&&characterSelection!=null){WmCharacterSearch.reset();characterKeys.clear();}}
    @Override protected void onDetachedFromWindow(){choiceOwner=null;choiceTouchActive=false;if(characterSelection!=null)WmCharacterSearch.reset();characterKeys.clear();super.onDetachedFromWindow();}
    public int menuWidth(){return menuWidth;}
    /** Unclipped content height; owners cap this to their available viewport. */
    public int menuHeight(){int height=getPaddingTop()+getPaddingBottom();for(int i=0;i<getChildCount();i++)height+=getChildAt(i).getLayoutParams().height;return height;}
    /** Outer height of a nonempty keyboard choice list. Client cap excludes frame;
     * shrink to item count, then round down to complete font-derived rows. */
    public int choiceDropdownHeight(int clientHeight){
        if(clientHeight<0)throw new IllegalArgumentException("Negative choice client height");
        if(keyboardSelection==null||entries.length==0)throw new IllegalStateException("Nonempty keyboard choices required");
        int row=getChildAt(0).getLayoutParams().height;
        return getPaddingTop()+getPaddingBottom()+Math.min(entries.length,clientHeight/row)*row;
    }
    /** Label hit target; separators have no target. */
    public View row(int index){return index<0||index>=labels.length?null:labels[index];}
    /** -1 clears the parent-row highlight. */
    public void setSubmenuSelected(int index){for(int i=0;i<labels.length;i++)if(labels[i]!=null){labels[i].setPressed(false);labels[i].setSelected(i==index&&entries[i].enabled&&entries[i].hasChildren());}}
    private void measureWidth(){menuWidth=entries.length==0?getPaddingLeft()+getPaddingRight():0;for(int i=0;i<labels.length;i++)if(labels[i]!=null)menuWidth=Math.min(WmMetrics.WIDTH,Math.max(menuWidth,(int)Math.ceil(WmFonts.measure(labels[i].getPaint(),entries[i].label))+(rowIcons!=null?60:keyboardSelection!=null?5:entries[i].hasChildren()?60:48)));}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        WmScrollView scroll=keyboardSelection!=null&&getParent() instanceof android.widget.ScrollView&&getParent().getParent() instanceof WmScrollView?(WmScrollView)getParent().getParent():null;
        int oldHeight=scroll==null||entries.length==0?0:getChildAt(0).getLayoutParams().height;
        int offset=scroll==null?0:scroll.getRequestedScrollPosition();
        Typeface font=rowIcons!=null&&iconBold?bold:regular;menuFont=font;selectionColor=selection;
        for(Label label:labels)if(label!=null){label.setTypeface(font);WmFonts.attach(label,font);WmControls.entry(label,selection);if(rowIcons!=null)label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,iconTextSize);}if(rowIcons!=null)styleIconRows(selection);if(keyboardSelection!=null)styleChoiceRows();measureWidth();requestLayout();
        // GWES AA1DC/A9F34 retains list top index across font/row-height
        // changes. Keep modern finger scrolling's fractional row as well.
        if(oldHeight>0){int height=getChildAt(0).getLayoutParams().height;if(height!=oldHeight)scroll.scrollToPosition(Math.round((float)offset*height/oldHeight));}
    }
    private static final class Label extends TextView {
        final java.util.ArrayList<Mark> marks=new java.util.ArrayList<Mark>();
        int index=-1;
        Bitmap icon;
        final Rect iconBounds=new Rect(4,2,36,34);
        Label(Context context){super(context);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(icon!=null)canvas.drawBitmap(icon,null,iconBounds,null);}
        @Override protected void drawableStateChanged(){super.drawableStateChanged();if(marks!=null)for(Mark mark:marks)mark.invalidate();}
    }
    private static final class Mark extends View {
        final Label label;final boolean arrow;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final Path mask=new Path();
        Mark(Context context,Label label,boolean arrow){super(context);this.label=label;this.arrow=arrow;
            // Lossless outline of the original1bpp14x14 crop at(42,0), not a font glyph.
            if(arrow)for(int y=0;y<14;y++)mask.addRect(4,y,4+(y<=7?y+1:15-y),y+1,Path.Direction.CW);
            setImportantForAccessibilityCompat();}
        private void setImportantForAccessibilityCompat(){if(android.os.Build.VERSION.SDK_INT>=16)setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas canvas){paint.setColor(label.getCurrentTextColor());if(arrow){paint.setAntiAlias(false);canvas.drawPath(mask,paint);}else canvas.drawCircle(getWidth()/2f,getHeight()/2f,4,paint);}
    }
}
