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
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

/** Editable candidate field; caller owns candidates, validation and popup.
 * Frame2/gutter28 match WmChoice; automatic height follows the plain combo font formula.
 * Use setValue for silent programmatic changes; direct editing reports deltas. */
public final class WmEditableChoice extends EditText implements WmControls.RestyleListener {
    public interface OnValueChangedListener {void changed(String value);}
    public interface OnOpenListener {void open(WmEditableChoice source);}
    private String[] candidates;
    private String lastValue="";
    private java.util.Locale candidateSortLocale;
    private int selection;
    private int candidateIndex=-1,candidateCaret,closedDropdownClientHeight=-1;
    private boolean updating,arrowGesture,arrowActive,extendedUi,pimInline;
    private final android.util.SparseBooleanArray comboKeys=new android.util.SparseBooleanArray();
    private OnValueChangedListener valueListener;
    private OnOpenListener openListener;
    private WmRadioMenu.Selection candidateNavigation;
    private final ChoiceFrame frame;
    final WmComboGlyph dropdown=new WmComboGlyph(this);

    public WmEditableChoice(Context context,Typeface font,String[] candidates,String value,int selection){
        super(context);this.selection=selection;
        // A3460 places edit1001 at the same bordered combo caption origin.
        // Plain edit1001:9DAEC retains its format top;991EC draws there.
        WmControls.textStyle(this,font);setGravity(Gravity.TOP|Gravity.LEFT);setSingleLine(true);setPadding(WmMetrics.BORDER+WmMetrics.EDGE,WmMetrics.BORDER+WmMetrics.EDGE/2,32,2);
        setMinWidth(0);setMinimumWidth(0);setMinHeight(WmMetrics.comboHeight(0));setMinimumHeight(WmMetrics.comboHeight(0));setSoundEffectsEnabled(false);
        frame=new ChoiceFrame(this);setBackgroundDrawable(frame);
        if(Build.VERSION.SDK_INT>=21){setBackgroundTintList(null);setStateListAnimator(null);setElevation(0);}
        setCandidates(candidates);setValue(value);
        addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){}
            public void afterTextChanged(Editable text){
                String next=text.toString();if(next.equals(lastValue))return;lastValue=next;
                if(!updating&&isEnabled()){
                    View popup=dropdown.popup();if(valueListener!=null)valueListener.changed(next);
                    // A300C notifies the owner before A2958; callbacks may close,
                    // replace or disable the popup. A2958 still updates hidden state
                    // after disable; only a retained popup lifetime may be synchronized.
                    candidateIndex=-1;candidateCaret=Math.max(0,findCandidate(value(),false));
                    if(popup!=null&&dropdown.popup()==popup)syncDropdown(popup,value());
                }
            }
        });
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        super.onMeasure(widthSpec,heightSpec);
        if(View.MeasureSpec.getMode(heightSpec)!=View.MeasureSpec.EXACTLY){
            int height=Math.max(getMeasuredHeight(),WmMetrics.comboHeight(WmRadioMenu.choiceRowHeight(getPaint())));
            setMeasuredDimension(getMeasuredWidthAndState(),resolveSizeAndState(height,heightSpec,getMeasuredState()<<MEASURED_HEIGHT_STATE_SHIFT));
        }
    }
    public String value(){return getText().toString();}
    public String[] candidates(){return candidates.clone();}
    /** Interaction only; restore owner candidates and literal draft first. No popup/gesture state. */
    public Bundle saveInteractionState(){Bundle state=new Bundle();state.putInt("schema",1);state.putInt("count",candidates.length);state.putInt("selected",candidateIndex);state.putInt("caret",candidateCaret);state.putInt("start",getSelectionStart());state.putInt("end",getSelectionEnd());return state;}
    public static void validateInteractionState(Bundle state,int candidateCount,int textLength){
        if(state==null||state.size()!=6||candidateCount<0||textLength<0)throw new IllegalArgumentException("Editable interaction schema1 required");
        for(String key:new String[]{"schema","count","selected","caret","start","end"})if(!(state.get(key) instanceof Integer))throw new IllegalArgumentException("Integer editable interaction required");
        int selected=state.getInt("selected"),caret=state.getInt("caret"),start=state.getInt("start"),end=state.getInt("end");
        if(state.getInt("schema")!=1||state.getInt("count")!=candidateCount||selected<-1||selected>=candidateCount||caret<0||(candidateCount==0?caret!=0:caret>=candidateCount)||!((start==-1&&end==-1)||(start>=0&&end>=0&&start<=textLength&&end<=textLength)))throw new IllegalArgumentException("Invalid editable interaction");
    }
    /** Silent; validation precedes gesture/key cancellation or selection changes. */
    public void restoreInteractionState(Bundle state){
        validateInteractionState(state,candidates.length,length());if(isDroppedDown())throw new IllegalStateException("Close editable popup before restoring interaction");cancelArrow();comboKeys.clear();dropdown.clear();candidateIndex=state.getInt("selected");candidateCaret=state.getInt("caret");
        if(state.getInt("start")==-1)android.text.Selection.removeSelection(getText());else setSelection(state.getInt("start"),state.getInt("end"));invalidate();
    }
    /** GWES A2958/LB_FINDSTRING: first prefix match, without changing the draft.
     * ponytail: Android case/NLS substitute; full native NLS remains unported. */
    public int dropdownSelectedIndex(){return findCandidate(value(),false);}
    private int findCandidate(String query,boolean exact){return findCandidate(candidates,query,exact);}
    static int findCandidate(String[] candidates,String query,boolean exact){
        if(query.isEmpty())return -1;
        java.text.Collator comparator=java.text.Collator.getInstance(java.util.Locale.getDefault());comparator.setStrength(java.text.Collator.SECONDARY);
        for(int i=0;i<candidates.length;i++){
            String label=candidates[i];int first=0;
            if(!exact&&query.charAt(0)!='['&&!label.isEmpty()&&label.charAt(0)=='['){first=1;if(first<label.length()&&label.charAt(first)=='-')first++;}
            for(int unit=0;unit<=query.length();unit++){
                if(unit==query.length()){if(!exact||first+unit==label.length())return i;break;}
                if(first+unit>=label.length())break;
                char a=Character.toUpperCase(query.charAt(unit)),b=Character.toUpperCase(label.charAt(first+unit));
                if(a!=b){if(comparator.compare(String.valueOf(a),String.valueOf(b))==0)return i;break;}
            }
        }
        return -1;
    }
    /** Copy candidates; changed content resets candidate index to -1 and search position to 0.
     * Input text and text selection remain; captured nonempty popup rows are not replaced. */
    public void setCandidates(String[] candidates){
        if(candidates==null)throw new IllegalArgumentException("Candidates required");
        String[] copy=candidates.clone();for(String label:copy)if(label==null)throw new IllegalArgumentException("Null candidate");boolean emptyTransition=this.candidates!=null&&(this.candidates.length==0||copy.length==0);boolean changed=!java.util.Arrays.equals(this.candidates,copy);if(changed){candidateIndex=-1;candidateCaret=0;}this.candidates=copy;
        // Existing nonempty popup rows are caller-owned captured choices. Only new
        // empty transitions update rows here; incremental mutations stay separate.
        if(changed&&emptyTransition){WmRadioMenu menu=WmComboGlyph.choiceMenu(dropdown.popup());if(menu!=null)menu.replaceEditableCandidates(copy);}
    }
    /** CBS_SORT applies only to future add operations; explicit insertion bypasses it.
     * Null restores append order. Existing rows are not reordered. */
    public void setCandidateSort(java.util.Locale locale){candidateSortLocale=locale;}
    public int addCandidate(String label){
        if(label==null)throw new IllegalArgumentException("Null candidate");
        int low=0,high=candidates.length-1;
        if(candidateSortLocale==null)low=candidates.length;
        else{
            java.text.Collator comparator=java.text.Collator.getInstance(candidateSortLocale);comparator.setStrength(java.text.Collator.SECONDARY);
            while(low<=high){int middle=(low+high)>>>1,compared=WmChoice.compareDisplay(label,candidates[middle],comparator);if(compared==0){low=middle;break;}if(compared<0)high=middle-1;else low=middle+1;}
        }
        return insertCandidate(low,label);
    }
    /** GWES A84C4: -1 appends, selected index shifts, stored caret does not.
     * Programmatic mutations are silent. Bound popup callbacks must resolve live
     * source indices; captured/reordered lists must be closed before mutation. */
    public int insertCandidate(int index,String label){
        if(label==null)throw new IllegalArgumentException("Null candidate");
        if(index==-1)index=candidates.length;
        if(index<0||index>candidates.length)throw new IndexOutOfBoundsException("Invalid candidate insertion");
        WmRadioMenu menu=mutationMenu();String[] next=new String[candidates.length+1];
        System.arraycopy(candidates,0,next,0,index);next[index]=label;System.arraycopy(candidates,index,next,index+1,candidates.length-index);
        if(candidateIndex>=index)candidateIndex++;candidates=next;
        if(menu!=null)menu.mutateEditableCandidates(next,candidateIndex,candidateCaret,index,true);return index;
    }
    /** A82DC: returns remaining count. Final deletion retains literal text;
     * deleting the selected row from a longer list clears its caption. */
    public int removeCandidate(int index){
        if(index<0||index>=candidates.length)throw new IndexOutOfBoundsException("Invalid candidate deletion");
        WmRadioMenu menu=mutationMenu();String[] next=new String[candidates.length-1];
        System.arraycopy(candidates,0,next,0,index);System.arraycopy(candidates,index+1,next,index,candidates.length-index-1);
        boolean clear=next.length>0&&candidateIndex==index;
        if(candidateIndex==index)candidateIndex=-1;else if(candidateIndex>index)candidateIndex--;
        if(candidateCaret!=0&&candidateCaret==index)candidateCaret--;
        candidateCaret=next.length==0?0:Math.min(candidateCaret,next.length-1);candidates=next;
        if(menu!=null)menu.mutateEditableCandidates(next,candidateIndex,candidateCaret,index,false);
        if(clear)setValue("");return next.length;
    }
    /** Explicit combo reset differs from final deletion: it clears the caption. */
    public void clearCandidates(){mutationMenu();setCandidates(new String[0]);candidateIndex=-1;candidateCaret=0;setValue("");}
    private WmRadioMenu mutationMenu(){
        WmRadioMenu menu=WmComboGlyph.choiceMenu(dropdown.popup());
        if(menu!=null&&!menu.matchesEditableCandidates(candidates))throw new IllegalStateException("Close captured or non-keyboard candidate popup before mutation");
        return menu;
    }
    public void setValue(String value){
        if(value==null)throw new IllegalArgumentException("Value required");boolean previous=updating;updating=true;
        try{if(!value.equals(value()))setText(value);lastValue=value();}finally{updating=previous;}
    }
    /** Commit a completed candidate selection; same values do not notify. */
    public void choose(int index){
        if(index<0||index>=candidates.length)throw new IndexOutOfBoundsException("Invalid candidate index");
        if(!isEnabled())return;candidateIndex=candidateCaret=index;String before=value(),next=candidates[index];if(next.equals(before))return;
        setValue(next);setSelection(getText().length());if(!before.equals(value())&&valueListener!=null)valueListener.changed(value());
    }
    private static void syncDropdown(View popup,String query){WmRadioMenu menu=WmComboGlyph.choiceMenu(popup);if(menu!=null)menu.syncEditableQuery(query);}
    /** For WmRadioMenu, call enableChoiceKeyboard before binding so editable
     * focus is retained. Caller creates, attaches and removes the popup; bind null on close. */
    public void bindDropdown(View popup){
        dropdown.bind(popup);WmRadioMenu menu=WmComboGlyph.choiceMenu(popup);
        if(menu!=null&&menu.keepEditableFocus()&&isEnabled()&&isDroppedDown()){candidateIndex=menu.choiceSelectedIndex();candidateCaret=Math.max(0,menu.choiceCaretIndex());requestFocus();}
    }
    public boolean isDroppedDown(){return dropdown.isDropped();}
    public void setOnValueChangedListener(OnValueChangedListener listener){valueListener=listener;}
    public void setOnOpenListener(OnOpenListener listener){openListener=listener;}
    void setPimInline(){pimInline=true;setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);setPadding(4,0,32,0);setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,28);setMinHeight(0);setMinimumHeight(0);setSelectAllOnFocus(true);setHighlightColor(selection);frame.invalidateSelf();}
    /** Optional owner candidate commit, shared with its popup selection policy.
     * Without an override closed arrow navigation calls choose(index). */
    public void setOnCandidateNavigationListener(WmRadioMenu.Selection listener){candidateNavigation=listener;}
    /** Optional native CB_SETEXTENDEDUI policy. Closed unmodified Up/Down
     * opens the caller-owned list; closed Page keys do not move selection.
     * F4 does not toggle it. Alt arrows retain
     * normal toggling. Like popup/enabled policy, this is not snapshot state. */
    public void setExtendedUi(boolean enabled){extendedUi=enabled;}
    public boolean isExtendedUi(){return extendedUi;}
    /** Hidden list client height in logical surface pixels, excluding frame and
     * caption. The popup owner supplies matching geometry; -1 leaves closed
     * Page keys to the editor until original geometry is known. Zero is valid.
     * Font changes recompute capacity using the shared ordinary-list row metric. */
    public void setClosedDropdownClientHeight(int height){
        if(height<-1)throw new IllegalArgumentException("Invalid dropdown client height");closedDropdownClientHeight=height;
    }
    /** Configured hidden-list client height; -1 means unspecified. */
    public int getClosedDropdownClientHeight(){return closedDropdownClientHeight;}
    public void open(){
        if(!isEnabled()||openListener==null)return;
        // A23BC completes exact first, prefix second; WM_SETTEXT follows the
        // normal edit notification path, then focused edit1001 selects all.
        if(!isDroppedDown()){
            String before=value();int index=findCandidate(before,true);if(index<0)index=findCandidate(before,false);
            if(index>=0){String next=candidates[index];if(!next.equals(before))getText().replace(0,length(),next);if(hasFocus())setSelection(0,length());}
        }
        if(isEnabled()&&openListener!=null)openListener.open(this);
    }
    private static boolean listNavigationKey(int key){return key==android.view.KeyEvent.KEYCODE_DPAD_UP||key==android.view.KeyEvent.KEYCODE_DPAD_DOWN||key==android.view.KeyEvent.KEYCODE_PAGE_UP||key==android.view.KeyEvent.KEYCODE_PAGE_DOWN;}
    @Override public boolean onKeyDown(int key,android.view.KeyEvent event){
        boolean dropped=isDroppedDown();
        // GWES9B448 temporarily disables extended UI to send opening F4 for
        // closed Up/Down. A89D8 rejects direct F4 while combo flag0x800 is set.
        if(isEnabled()&&extendedUi&&!arrowGesture&&event.hasNoModifiers()){
            if(key==android.view.KeyEvent.KEYCODE_F4){comboKeys.put(key,true);return true;}
            if(!dropped&&(key==android.view.KeyEvent.KEYCODE_PAGE_UP||key==android.view.KeyEvent.KEYCODE_PAGE_DOWN)){comboKeys.put(key,true);return true;}
            if(!dropped&&(key==android.view.KeyEvent.KEYCODE_DPAD_UP||key==android.view.KeyEvent.KEYCODE_DPAD_DOWN)){open();comboKeys.put(key,true);return true;}
        }
        // GWES9F8CC links edit1001 to list1000 even while closed.9B448
        // forwards Up/Down; A89D8 navigates from the draft's prefix caret,
        // then A300C/A2648 propagate the selected label back into the edit.
        boolean page=key==android.view.KeyEvent.KEYCODE_PAGE_UP||key==android.view.KeyEvent.KEYCODE_PAGE_DOWN;
        if(isEnabled()&&!dropped&&!arrowGesture&&event.hasNoModifiers()&&(key==android.view.KeyEvent.KEYCODE_DPAD_UP||key==android.view.KeyEvent.KEYCODE_DPAD_DOWN||page&&closedDropdownClientHeight>=0)){
            if(candidates.length==0){comboKeys.put(key,true);return true;}
            int step=page?WmRadioMenu.choicePageStep(closedDropdownClientHeight,WmRadioMenu.choiceRowHeight(getPaint())):1;
            long offset=(key==android.view.KeyEvent.KEYCODE_DPAD_DOWN||key==android.view.KeyEvent.KEYCODE_PAGE_DOWN)?step:-step;
            int next=!page&&candidateIndex!=candidateCaret?candidateCaret:(int)Math.max(0,Math.min(candidates.length-1,(long)candidateCaret+offset));
            if(next!=candidateIndex){candidateIndex=candidateCaret=next;if(candidateNavigation!=null)candidateNavigation.choose(next);else choose(next);}
            comboKeys.put(key,true);return true;
        }
        if(isEnabled()&&(WmControls.isComboToggleKey(key,event)||dropped&&event.hasNoModifiers()&&(WmControls.isEnterKey(key)||key==android.view.KeyEvent.KEYCODE_ESCAPE||listNavigationKey(key)))){
            if(arrowGesture)return true;
            if(dropped?dropdown.key(event):openListener!=null){if(!dropped)open();comboKeys.put(key,true);return true;}
        }
        return super.onKeyDown(key,event);
    }
    @Override public boolean onKeyUp(int key,android.view.KeyEvent event){
        if(comboKeys.get(key)){comboKeys.delete(key);return true;}
        return super.onKeyUp(key,event);
    }
    @Override protected void onFocusChanged(boolean focused,int direction,android.graphics.Rect old){super.onFocusChanged(focused,direction,old);if(!focused&&comboKeys!=null)comboKeys.clear();}
    private boolean inArrow(MotionEvent event){return event.getX()>=Math.max(0,getWidth()-28)&&event.getX()<getWidth()&&event.getY()>=0&&event.getY()<getHeight();}
    private void cancelArrow(){arrowGesture=false;arrowActive=false;setPressed(false);if(frame!=null)frame.invalidateSelf();}
    @Override public boolean onTouchEvent(MotionEvent event){
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){cancelArrow();if(isEnabled()&&inArrow(event)){
            // GWES A38F4 takes focus before arrow capture; WM_SETFOCUS goes to edit1001.
            if(!hasFocus()&&!requestFocus())return true;
            arrowGesture=true;arrowActive=true;setPressed(true);return true;}}
        if(arrowGesture){
            if(action==MotionEvent.ACTION_MOVE&&!inArrow(event)){arrowActive=false;setPressed(false);}
            else if(action==MotionEvent.ACTION_UP){boolean activate=arrowActive&&inArrow(event)&&isEnabled();cancelArrow();if(activate)open();}
            else if(action==MotionEvent.ACTION_CANCEL)cancelArrow();
            else if(action==MotionEvent.ACTION_POINTER_DOWN){arrowActive=false;setPressed(false);}
            return true;
        }
        return super.onTouchEvent(event);
    }
    @Override public void setEnabled(boolean enabled){super.setEnabled(enabled);if(!enabled){cancelArrow();if(comboKeys!=null)comboKeys.clear();if(dropdown!=null)dropdown.clear();}}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        cancelArrow();this.selection=selection;boolean previous=updating;updating=true;
        try{setTypeface(regular);WmFonts.attach(this,regular);lastValue=value();if(pimInline)setHighlightColor(selection);}finally{updating=previous;}frame.invalidateSelf();
    }
    @Override protected void onDetachedFromWindow(){cancelArrow();comboKeys.clear();dropdown.clear();super.onDetachedFromWindow();}
    private static final class ChoiceFrame extends Drawable {
        private final WmEditableChoice owner;private final Paint paint=new Paint();private int alpha=255;
        ChoiceFrame(WmEditableChoice owner){this.owner=owner;}
        @Override public boolean isStateful(){return true;}
        @Override protected boolean onStateChange(int[] states){invalidateSelf();return true;}
        private void color(int value){paint.setColor(value);paint.setAlpha(alpha);}
        @Override public void draw(Canvas canvas){
            int l=getBounds().left,t=getBounds().top,r=getBounds().right,b=getBounds().bottom;if(r<=l||b<=t)return;
            if(owner.pimInline){
                if(!owner.hasFocus()&&!owner.dropdown.isDropped())return;
                paint.setStyle(Paint.Style.FILL);color(Color.WHITE);canvas.drawRect(l,t,r,b,paint);
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);color(Color.BLACK);canvas.drawRect(l+1,t+1,r-1,b-1,paint);
                color(owner.isEnabled()?Color.BLACK:WmControls.SHADOW);WmComboGlyph.draw(canvas,paint,Math.max(l,r-28),t+2,r-2,b-2,false);return;
            }
            int divider=Math.max(l,r-28);boolean pressed=owner.isEnabled()&&(owner.arrowActive||owner.dropdown.isDropped());
            paint.setStyle(Paint.Style.FILL);color(Color.WHITE);canvas.drawRect(l,t,r,b,paint);
            color(pressed?Color.BLACK:WmControls.SURFACE);canvas.drawRect(divider,t+2,r-2,b-2,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);color(WmControls.FRAME);canvas.drawRect(l+1,t+1,r-1,b-1,paint);canvas.drawLine(divider,t+2,divider,b-2,paint);
            paint.setStyle(Paint.Style.FILL);color(!owner.isEnabled()?WmControls.SHADOW:pressed?Color.WHITE:Color.BLACK);
            WmComboGlyph.draw(canvas,paint,divider,t+2,r-2,b-2,pressed);
        }
        @Override public void setAlpha(int alpha){this.alpha=Math.max(0,Math.min(255,alpha));invalidateSelf();}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
