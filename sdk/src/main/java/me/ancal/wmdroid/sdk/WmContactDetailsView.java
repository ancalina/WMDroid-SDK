package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

/** Owner-ordered contact drafts. Formatting, subcards and persistence belong to the owner.
 * Text selection follows the theme; the insertion cursor remains platform-owned on min14. */
public final class WmContactDetailsView extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener,WmControls.BackHandler {
    public static final int TEXT=0,SUBCARD=1,ACTION=2,DATE=3;
    public static final class Field {
        public final String key,label,value;
        public final int kind,inputType;
        public Field(String key,String label,String value,int kind){this(key,label,value,kind,InputType.TYPE_CLASS_TEXT);}
        public Field(String key,String label,String value,int kind,int inputType){
            if(key==null||key.isEmpty()||label==null||value==null||kind<TEXT||kind>DATE)throw new IllegalArgumentException("Field key, label, value and valid kind required");
            if(kind==DATE)WmDateField.parseValue(value);
            this.key=key;this.label=label;this.value=value;this.kind=kind;this.inputType=inputType;
        }
    }
    public interface Listener {
        default void changed(Field field){}
        default void committed(Field field){}
        default void open(Field field){}
        default void notesChanged(String notes){}
        default void tabChanged(int tab){}
    }
    private final Listener listener;
    private final WmPropertySheet sheet;
    private final LinearLayout rows;
    private final WmScrollView detailsScroll,notesScroll;
    private final EditText notes;
    private final ArrayList<Row> rowViews=new ArrayList<>();
    private Field[] fields=new Field[0];
    private Typeface regular;
    private int selection;
    private String language="ko",notesText="";
    private boolean silent;
    private FrameLayout subcardOverlay;
    private WmContactSubcardView subcard;
    private Runnable subcardApply,subcardCancel;
    private FrameLayout dateOverlay;
    private FrameLayout dateParent;
    private ViewGroup dateSpace;
    private WmCalendarPicker datePicker;
    private Row dateRow;
    private boolean closingDate;
    private Typeface bold;
    private Locale dateLocale(){return "ko".equals(language)?Locale.KOREA:"en".equals(language)?Locale.US:WmStrings.locale(language);}


    public WmContactDetailsView(Context context,Typeface regular,Field[] fields,String notes,int selection,Listener listener){
        super(context);if(regular==null||listener==null)throw new IllegalArgumentException("Details font and listener required");
        this.regular=regular;this.bold=Typeface.create(regular,Typeface.BOLD);this.selection=selection;this.listener=listener;setBackgroundColor(Color.WHITE);setFocusableInTouchMode(true);
        rows=new LinearLayout(context);rows.setOrientation(LinearLayout.VERTICAL);rows.setBackgroundColor(Color.WHITE);
        detailsScroll=new WmScrollView(context,rows);
        this.notes=new EditText(context);WmControls.input(this.notes,regular);plainInput(this.notes);this.notes.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);this.notes.setGravity(Gravity.TOP|Gravity.LEFT);this.notes.setPadding(8,8,8,8);font(this.notes,28);
        notesScroll=new WmScrollView(context,this.notes);
        sheet=new WmPropertySheet(context,regular,WmPalette.face(selection));sheet.addPage(WmStrings.text(context,language,"자세히"),detailsScroll);sheet.addPage(WmStrings.text(context,language,"메모"),notesScroll);addView(sheet,new LayoutParams(-1,-1));
        sheet.setOnPageChangedListener(tab->{closeDatePopup();if(silent)return;if(tab==1)WmContactDetailsView.this.requestFocus();listener.tabChanged(tab);});
        this.notes.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence text,int start,int count,int after){}
            public void onTextChanged(CharSequence text,int start,int before,int count){}
            public void afterTextChanged(Editable text){String value=text.toString();if(silent||value.equals(notesText))return;notesText=value;listener.notesChanged(value);}
        });
        setFields(fields);setNotes(notes);
    }
    /** Outside UP applies the owner draft; Back cancels it. Native dismissal semantics remain unverified. */
    public void showSubcard(WmContactSubcardView card,Runnable apply,Runnable cancel){
        if(card==null||apply==null||cancel==null||card.getParent()!=null)throw new IllegalArgumentException("Unattached subcard and completion callbacks required");
        closeDatePopup();closeSubcard(false);if(subcardOverlay!=null)throw new IllegalStateException("Cancellation opened another subcard");
        requestFocus();card.setClickable(true);subcard=card;subcardApply=apply;subcardCancel=cancel;subcardOverlay=new FrameLayout(getContext());
        WmCommandBar.KeyTouch outside=new WmCommandBar.KeyTouch(false);subcardOverlay.setOnTouchListener(outside);subcardOverlay.setOnClickListener(view->{if(view==subcardOverlay)closeSubcard(true);});
        FrameLayout.LayoutParams position=new FrameLayout.LayoutParams(350,card.desiredHeight());position.leftMargin=70;position.topMargin=42;subcardOverlay.addView(card,position);addView(subcardOverlay,new LayoutParams(-1,-1));
    }
    public WmContactSubcardView subcard(){return subcard;}
    public boolean closeSubcard(boolean apply){
        if(subcardOverlay==null)return false;Runnable completion=apply?subcardApply:subcardCancel;FrameLayout overlay=subcardOverlay;
        subcardOverlay=null;subcard=null;subcardApply=null;subcardCancel=null;removeView(overlay);overlay.removeAllViews();if(completion!=null)completion.run();return true;
    }
    public WmDateField dateField(String key){Field field=field(key);if(field.kind!=DATE)throw new IllegalArgumentException("DATE field required");for(Row row:rowViews)if(row.field.key.equals(key))return row.date;throw new IllegalArgumentException("Unknown DATE key");}
    public WmCalendarPicker datePopup(){return datePicker;}
    /** Finish the nested year editor before dismissing its owner popup. */
    public boolean closeDatePopup(){
        if(dateOverlay==null||closingDate)return false;closingDate=true;
        try{datePicker.back();removeDatePopup();}finally{closingDate=false;}return true;
    }
    private void removeDatePopup(){if(dateOverlay==null)return;FrameLayout overlay=dateOverlay;Row row=dateRow;dateOverlay=null;datePicker=null;dateRow=null;if(row!=null)row.date.bindDropdown(null);FrameLayout parent=dateParent;dateParent=null;dateSpace=null;parent.removeView(overlay);overlay.removeAllViews();}
    private void openDatePopup(Row row){openDatePopup(row,false);}
    private void openDatePopup(Row row,boolean restoring){
        if(closingDate||(silent&&!restoring)||!rowViews.contains(row)||!row.date.isEnabled()||row.date.value()==null)return;
        closeDatePopup();closeSubcard(false);if(!rowViews.contains(row)||subcard!=null||row.date.value()==null)return;
        WmDateTimeField.Value value=row.date.value();
        final WmCalendarPicker[] source=new WmCalendarPicker[1];
        WmCalendarPicker picker=new WmCalendarPicker(getContext(),regular,bold,dateLocale(),value.year,value.month,value.day,selection,(y,m,d)->{
            if(datePicker!=source[0]||dateRow!=row||!rowViews.contains(row))return;WmCalendarPicker current=datePicker;row.date.selectDate(y,m,d);if(datePicker==current){closeDatePopup();if(rowViews.contains(row))row.activate(false);}
        },new WmDateTimeField.Value(1752,9,14,0,0),new WmDateTimeField.Value(9999,12,31,0,0));
        source[0]=picker;
        picker.setOnDateChangeListener((y,m,d)->{if(datePicker==source[0]&&dateRow==row&&rowViews.contains(row))row.date.selectDate(y,m,d);});
        picker.setOnNoneListener(()->{if(datePicker!=source[0]||dateRow!=row||!rowViews.contains(row))return;WmCalendarPicker current=datePicker;row.date.clearDate();if(datePicker==current){closeDatePopup();if(rowViews.contains(row))row.activate(false);}});
        FrameLayout panel=new FrameLayout(getContext());panel.setBackgroundColor(Color.BLACK);panel.setPadding(2,2,2,2);panel.setClickable(true);panel.addView(picker,new LayoutParams(-1,-1));
        datePicker=picker;dateRow=row;dateOverlay=new FrameLayout(getContext()){
            @Override protected void onMeasure(int width,int height){super.onMeasure(width,height);android.graphics.Point size=picker.naturalSize();panel.measure(MeasureSpec.makeMeasureSpec(Math.min(size.x+4,getMeasuredWidth()),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.min(size.y+4,getMeasuredHeight()),MeasureSpec.EXACTLY));}
            @Override protected void onLayout(boolean changed,int l,int t,int r,int b){super.onLayout(changed,l,t,r,b);Rect anchor=new Rect();row.date.getDrawingRect(anchor);dateSpace.offsetDescendantRectToMyCoords(row.date,anchor);Rect placed=datePopupBounds(anchor,r-l,b-t,panel.getMeasuredWidth(),panel.getMeasuredHeight(),28);panel.layout(placed.left,placed.top,placed.right,placed.bottom);}
        };dateOverlay.setOnTouchListener(new WmCommandBar.KeyTouch(false));dateOverlay.setOnClickListener(view->{if(view==dateOverlay)closeDatePopup();});
        dateOverlay.addView(panel,new LayoutParams(-2,-2));attachDateOverlay();row.date.bindDropdown(picker);
    }
    private void attachDateOverlay(){
        ViewGroup space=this;for(ViewParent parent=getParent();parent instanceof View;parent=parent.getParent())if(parent instanceof WmWindow){space=(WmWindow)parent;break;}
        FrameLayout target=space instanceof WmWindow?((WmWindow)space).popupLayer():this;dateSpace=space;
        if(target!=dateParent){
            Bundle nested=dateParent==null?null:datePicker.saveInteractionState();
            if(nested!=null)datePicker.clearChoices();
            if(dateParent!=null)dateParent.removeView(dateOverlay);dateParent=target;target.addView(dateOverlay,new LayoutParams(-1,-1));
            if(nested!=null)datePicker.restoreInteractionState(nested);
        }
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(dateOverlay!=null)attachDateOverlay();}
    // Original03286EF4 / SDK032EF098: full screen, below then above, top/side fallback.
    // ponytail: SDK frame is the anchor; native text/arrow inner RECT pixels need measurement.
    private static Rect datePopupBounds(Rect anchor,int screenWidth,int screenHeight,int width,int height,int arrowWidth){
        int left=Math.max(0,Math.min(anchor.left,screenWidth-width)),top=anchor.bottom+2;
        if(top+height>screenHeight)top=anchor.top-height;
        if(top<0){top=0;left=screenWidth-(left+width)>=left?anchor.right+arrowWidth:anchor.left-width;if(left<0||left+width>screenWidth)left=screenWidth/2-width/2;}
        // Host resize can temporarily leave the anchor outside its visible scroll area.
        top=Math.max(0,Math.min(top,screenHeight-height));return new Rect(left,top,left+width,top+height);
    }
    @Override public boolean back(){if(datePicker!=null){if(datePicker.back())return true;return closeDatePopup();}return closeSubcard(false);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        if(subcardOverlay!=null){int width=Math.min(350,MeasureSpec.getSize(widthSpec)),height=Math.min(subcard.desiredHeight(),MeasureSpec.getSize(heightSpec));FrameLayout.LayoutParams position=(FrameLayout.LayoutParams)subcard.getLayoutParams();int x=Math.min(70,Math.max(0,MeasureSpec.getSize(widthSpec)-width)),y=Math.min(42,Math.max(0,MeasureSpec.getSize(heightSpec)-height));if(position.width!=width||position.height!=height||position.leftMargin!=x||position.topMargin!=y){position.width=width;position.height=height;position.leftMargin=x;position.topMargin=y;subcard.setLayoutParams(position);}}

        super.onMeasure(widthSpec,heightSpec);
    }
    @Override protected void onDetachedFromWindow(){removeDatePopup();if(subcardOverlay!=null){FrameLayout overlay=subcardOverlay;subcardOverlay=null;subcard=null;subcardApply=null;subcardCancel=null;removeView(overlay);overlay.removeAllViews();}super.onDetachedFromWindow();}
    public Field[] fields(){return fields.clone();}
    public Field field(String key){for(Field field:fields)if(field.key.equals(key))return field;throw new IllegalArgumentException("Unknown field key");}
    /** Selects Details and activates the field. TEXT/SUBCARD select all text;
     * ACTION invokes open. DATE focuses the inline editor without opening its calendar.
     * An empty DATE becomes range-clamped today and can invoke changed. */
    public void editField(String key){field(key);setTab(0);for(Row row:rowViews)if(row.field.key.equals(key)){row.activate(true);return;}}
    public String notes(){return notesText;}
    public int selectedTab(){return sheet.selectedPage();}
    public void setTab(int tab){if(tab<0||tab>1)throw new IllegalArgumentException("Details tab 0 or 1 required");sheet.selectPage(tab);}
    /** Owner values are separate. An open DATE popup adds schema3 nested calendar state. */
    public Bundle saveInteractionState(){
        String focus="none",key="";EditText focused=null;Bundle dates=new Bundle();
        for(Row row:rowViews)if(row.date!=null)dates.putBundle(row.field.key,row.date.saveInteractionState());
        if(selectedTab()==0&&dateRow!=null){focus="field";key=dateRow.field.key;}
        else if(selectedTab()==1&&notes.hasFocus()){focus="notes";focused=notes;}
        else if(selectedTab()==0)for(Row row:rowViews){
            if(row.field.kind==TEXT&&row.editor!=null&&row.editor.hasFocus()){focus="field";key=row.field.key;focused=row.editor;break;}
            if(row.date!=null&&(row.date.hasFocus()||dateRow==row)){focus="field";key=row.field.key;break;}
        }
        Bundle state=new Bundle();state.putInt("schema",dates.size()==0?1:datePicker==null?2:3);state.putInt("tab",selectedTab());
        state.putInt("details_scroll",detailsScroll.getRequestedScrollPosition());state.putInt("notes_scroll",notesScroll.getRequestedScrollPosition());
        state.putString("focus",focus);state.putString("field",key);state.putInt("selection_start",focused==null?0:Math.max(0,focused.getSelectionStart()));state.putInt("selection_end",focused==null?0:Math.max(0,focused.getSelectionEnd()));
        if(dates.size()!=0){state.putBundle("dates",dates);state.putString("date_popup",dateRow==null?"":dateRow.field.key);}if(datePicker!=null)state.putBundle("calendar",datePicker.saveInteractionState());return state;
    }
    /** Pure validation against the owner's prospective values, before any UI/draft mutation. */
    public static void validateInteractionState(Bundle state,Field[] ownerValues){
        if(state==null||ownerValues==null)throw new IllegalArgumentException("Details interaction and fields required");
        int schema=stateInt(state,"schema"),tab=stateInt(state,"tab"),detailsOffset=stateInt(state,"details_scroll"),notesOffset=stateInt(state,"notes_scroll"),start=stateInt(state,"selection_start"),end=stateInt(state,"selection_end");
        Object focusValue=state.get("focus"),keyValue=state.get("field");
        if(schema<1||schema>3||state.size()!=(schema==1?8:schema==2?10:11)||tab<0||tab>1||detailsOffset<0||notesOffset<0||start<0||end<0||!(focusValue instanceof String)||!(keyValue instanceof String))throw new IllegalArgumentException("Invalid Details interaction state");
        HashSet<String> keys=new HashSet<>();Field focused=null;String key=(String)keyValue,focus=(String)focusValue;int dateCount=0;
        for(Field field:ownerValues){if(field==null||!keys.add(field.key))throw new IllegalArgumentException("Unique owner fields required");if(field.kind==DATE){WmDateField.parseValue(field.value);dateCount++;}if(field.key.equals(key))focused=field;}
        if("field".equals(focus)){
            if(tab!=0||focused==null||(focused.kind!=TEXT&&(schema<2||focused.kind!=DATE))||focused.kind==DATE&&(start!=0||end!=0))throw new IllegalArgumentException("Compatible focused Details field required");
        }else if("notes".equals(focus)){if(tab!=1||!key.isEmpty())throw new IllegalArgumentException("Focused Notes tab required");}
        else if(!"none".equals(focus)||!key.isEmpty()||start!=0||end!=0)throw new IllegalArgumentException("Invalid Details focus state");
        if(schema>=2){
            Object datesValue=state.get("dates"),popupValue=state.get("date_popup");if(!(datesValue instanceof Bundle)||!(popupValue instanceof String))throw new IllegalArgumentException("DATE interaction map required");
            Bundle dates=(Bundle)datesValue;String popup=(String)popupValue;if(dateCount==0||dates.size()!=dateCount)throw new IllegalArgumentException("Exact DATE interaction keys required");boolean popupFound=popup.isEmpty();
            for(Field field:ownerValues)if(field.kind==DATE){Object date=dates.get(field.key);if(!(date instanceof Bundle))throw new IllegalArgumentException("DATE interaction required");WmDateTimeField.Value value=WmDateField.parseValue(field.value);WmDateField.validateInteractionState((Bundle)date,value);if(field.key.equals(popup)){if(tab!=0||value==null)throw new IllegalArgumentException("Visible nonempty DATE popup required");popupFound=true;}}
            if(!popupFound)throw new IllegalArgumentException("Unknown DATE popup key");
            if(schema==3){if(popup.isEmpty()||!(state.get("calendar") instanceof Bundle))throw new IllegalArgumentException("Open DATE calendar state required");WmCalendarPicker.validateInteractionState(state.getBundle("calendar"));}
        }
    }
    /** Validates first. Closing a DATE popup is invalid; a nonempty schema>=2
     * date_popup target must be enabled to reopen. Other DATE states are not
     * rejected merely because their controls are disabled. */
    public void restoreInteractionState(Bundle state){
        validateInteractionState(state,fields);int schema=stateInt(state,"schema"),tab=stateInt(state,"tab"),start=stateInt(state,"selection_start"),end=stateInt(state,"selection_end");String focus=state.getString("focus"),key=state.getString("field");
        if(closingDate)throw new IllegalStateException("Cannot restore Details while closing its DATE popup");
        if(schema>=2&&!state.getString("date_popup").isEmpty()&&!dateField(state.getString("date_popup")).isEnabled())throw new IllegalStateException("Enabled DATE popup target required");
        boolean prior=silent;silent=true;
        try{
            removeDatePopup();sheet.selectPage(tab);
            if(subcard==null){
                if("field".equals(focus)){for(Row row:rowViews)if(row.field.key.equals(key)){row.activate(false);if(row.editor!=null)caret(row.editor,start,end);break;}}
                else if("notes".equals(focus)){notes.requestFocus();caret(notes,start,end);}else requestFocus();
            }
            if(schema>=2&&!state.getString("date_popup").isEmpty()&&subcard==null){for(Row row:rowViews)if(row.field.key.equals(state.getString("date_popup"))){openDatePopupRestored(row);if(schema==3)datePicker.restoreInteractionState(state.getBundle("calendar"));break;}}
            Bundle dates=schema>=2?state.getBundle("dates"):null;for(Row row:rowViews)if(row.date!=null){row.date.setValue(WmDateField.parseValue(row.field.value));if(dates!=null)row.date.restoreInteractionState(dates.getBundle(row.field.key));row.value.setText(row.date.getContentDescription());}
            detailsScroll.scrollToPosition(stateInt(state,"details_scroll"));notesScroll.scrollToPosition(stateInt(state,"notes_scroll"));
        }finally{silent=prior;}
    }
    private void openDatePopupRestored(Row row){openDatePopup(row,true);}
    private static int stateInt(Bundle state,String key){Object value=state.get(key);if(!(value instanceof Integer))throw new IllegalArgumentException("Integer Details state "+key+" required");return (Integer)value;}
    public void setNotes(String value){
        String next=value==null?"":value;if(next.equals(notesText))return;int start=notes.getSelectionStart(),end=notes.getSelectionEnd();
        boolean prior=silent;silent=true;try{notesText=next;notes.setText(next);caret(notes,start,end);}finally{silent=prior;}
    }
    public void setFieldValue(String key,String value){
        if(value==null)throw new IllegalArgumentException("Field value required");Field current=field(key);if(current.value.equals(value))return;
        for(Row row:rowViews)if(row.field.key.equals(key)){Field next=new Field(current.key,current.label,value,current.kind,current.inputType);replace(next);row.update(next);return;}
    }
    /** Validate the entire replacement before changing drafts or controls. */
    public void setFields(Field[] values){
        if(values==null)throw new IllegalArgumentException("Fields required");Field[] copy=values.clone();HashSet<String> keys=new HashSet<>();
        for(Field field:copy)if(field==null||!keys.add(field.key))throw new IllegalArgumentException("Null or duplicate field key");
        boolean same=copy.length==fields.length;for(int i=0;same&&i<copy.length;i++)same=equal(copy[i],fields[i]);if(same)return;
        boolean compatible=copy.length==fields.length;for(int i=0;compatible&&i<copy.length;i++)compatible=copy[i].key.equals(fields[i].key)&&copy[i].kind==fields[i].kind&&copy[i].inputType==fields[i].inputType;
        if(compatible){int offset=detailsScroll.getRequestedScrollPosition();boolean prior=silent;silent=true;try{fields=copy;for(int i=0;i<copy.length;i++)rowViews.get(i).update(copy[i]);detailsScroll.scrollToPosition(offset);}finally{silent=prior;}return;}
        String focused=null;int start=0,end=0;for(Row row:rowViews)if(row.date!=null&&row.date.hasFocus()){focused=row.field.key;break;}else if(row.editor!=null&&row.editor.hasFocus()){focused=row.field.key;start=row.editor.getSelectionStart();end=row.editor.getSelectionEnd();break;}
        int offset=detailsScroll.getRequestedScrollPosition();boolean prior=silent;silent=true;
        try{
            closeDatePopup();fields=copy;rows.removeAllViews();rowViews.clear();
            for(Field field:copy){Row row=new Row(getContext(),field);rowViews.add(row);rows.addView(row,new LinearLayout.LayoutParams(-1,39));}
            if(focused!=null)for(Row row:rowViews)if(row.field.key.equals(focused)&&row.field.kind!=ACTION){row.activate(false);if(row.editor!=null)caret(row.editor,start,end);break;}
            detailsScroll.scrollToPosition(offset);
        }finally{silent=prior;}
    }
    private static boolean equal(Field a,Field b){return a.key.equals(b.key)&&a.label.equals(b.label)&&a.value.equals(b.value)&&a.kind==b.kind&&a.inputType==b.inputType;}
    private void replace(Field next){for(int i=0;i<fields.length;i++)if(fields[i].key.equals(next.key)){fields[i]=next;return;}}
    private static void caret(EditText text,int start,int end){int length=text.length();text.setSelection(Math.max(0,Math.min(start,length)),Math.max(0,Math.min(end,length)));}
    private void font(TextView text,int size){if(text instanceof EditText)text.setHighlightColor(WmPalette.highlight(selection));text.setTypeface(regular);WmFonts.attach(text,regular);text.setTextSize(TypedValue.COMPLEX_UNIT_PX,size);}
    private static void plainInput(EditText editor){editor.setBackgroundColor(Color.TRANSPARENT);editor.setMinWidth(0);editor.setMinimumWidth(0);editor.setMinHeight(0);editor.setMinimumHeight(0);editor.setPadding(4,0,4,0);}
    @Override public void onWmLanguageChanged(String language){language=WmStrings.language(getContext(),language);this.language=language;for(Row row:rowViews)if(row.date!=null){row.date.setLocale(dateLocale());row.value.setText(row.date.getContentDescription());}if(datePicker!=null)datePicker.setLocale(dateLocale());sheet.setPageTitle(0,WmStrings.text(getContext(),language,"자세히"));sheet.setPageTitle(1,WmStrings.text(getContext(),language,"메모"));}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        if(regular==null)throw new IllegalArgumentException("Details font required");int offset=detailsScroll.getRequestedScrollPosition(),notesOffset=notesScroll.getRequestedScrollPosition();boolean prior=silent;silent=true;
        try{this.regular=regular;this.bold=bold;this.selection=selection;for(Row row:rowViews)row.restyle();font(notes,28);if(datePicker!=null)datePicker.onWmRestyle(regular,bold,selection,tabColor);if(subcard!=null)subcard.onWmRestyle(regular,bold,selection,tabColor);sheet.onWmRestyle(regular,bold,selection,tabColor);detailsScroll.scrollToPosition(offset);notesScroll.scrollToPosition(notesOffset);}finally{silent=prior;}
    }
    private final class Row extends PimFieldRow {
        Field field;
        final TextView label,value;
        EditText editor;
        final WmDateField date;
        final Arrow arrow;
        final WmCommandBar.KeyTouch touch=new WmCommandBar.KeyTouch(false),labelTouch=new WmCommandBar.KeyTouch(false),valueTouch=new WmCommandBar.KeyTouch(false);
        Row(Context context,Field field){
            super(context);this.field=field;setWillNotDraw(false);setBackgroundColor(Color.WHITE);setClickable(true);setSoundEffectsEnabled(false);setOnTouchListener(touch);setContentDescription(field.label+" "+field.value);
            label=WmControls.text(context,regular,field.label);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);label.setPadding(0,0,0,0);addView(label);
            value=WmControls.text(context,regular,field.value);value.setSingleLine(true);value.setEllipsize(android.text.TextUtils.TruncateAt.END);value.setPadding(0,0,0,0);addView(value);
            label.setOnClickListener(view->performClick());value.setOnClickListener(view->performClick());label.setOnTouchListener(labelTouch);value.setOnTouchListener(valueTouch);
            if(field.kind==DATE){
                editor=null;date=new WmDateField(context,regular,dateLocale(),WmDateField.parseValue(field.value),selection);date.setVisibility(GONE);addView(date);value.setText(date.getContentDescription());
                date.setOnOpenListener(view->{if(!silent&&rowViews.contains(Row.this))openDatePopup(Row.this);});
                date.setOnValueChangedListener((oldValue,newValue)->{
                    if(silent||!rowViews.contains(Row.this))return;String next=WmDateField.formatValue(newValue);if(next.equals(Row.this.field.value))return;
                    Field old=Row.this.field;Row.this.field=new Field(old.key,old.label,next,old.kind,old.inputType);replace(Row.this.field);value.setText(date.getContentDescription());setContentDescription(old.label+" "+next);listener.changed(Row.this.field);
                });
                date.setOnFocusChangeListener((view,focused)->{value.setVisibility(focused?INVISIBLE:VISIBLE);if(!focused){date.setVisibility(GONE);value.setText(date.getContentDescription());if(!silent&&rowViews.contains(Row.this))listener.committed(Row.this.field);}invalidate();});
            }else date=null;
            arrow=field.kind==SUBCARD?new Arrow(context,this):null;if(arrow!=null)addView(arrow);restyle();
        }
        // Hidden drafts live in Field; allocate the Android editor only on activation.
        private void ensureEditor(){
            if(editor!=null||field.kind==ACTION||field.kind==DATE)return;
                editor=new EditText(getContext());WmControls.input(editor,regular);editor.setInputType(field.inputType);editor.setSingleLine(true);plainInput(editor);editor.setText(field.value);editor.setVisibility(GONE);editor.setEnabled(isEnabled());addView(editor);
                editor.setOnFocusChangeListener((view,focused)->{value.setVisibility(focused?INVISIBLE:VISIBLE);if(!focused){editor.setVisibility(GONE);if(!silent&&rowViews.contains(Row.this))listener.committed(Row.this.field);}invalidate();});
                editor.addTextChangedListener(new TextWatcher(){
                    public void beforeTextChanged(CharSequence text,int start,int count,int after){}
                    public void onTextChanged(CharSequence text,int start,int before,int count){}
                    public void afterTextChanged(Editable text){String next=text.toString();if(silent||next.equals(Row.this.field.value)||!rowViews.contains(Row.this))return;Field old=Row.this.field;Row.this.field=new Field(old.key,old.label,next,old.kind,old.inputType);replace(Row.this.field);value.setText(next);setContentDescription(old.label+" "+next);listener.changed(Row.this.field);}
                });
            font(editor,28);
        }
        void restyle(){font(label,24);font(value,28);if(editor!=null)font(editor,28);if(date!=null)date.onWmRestyle(regular,bold,selection,selection);requestLayout();invalidate();}
        void update(Field next){boolean changed=!field.value.equals(next.value);int start=editor==null?0:editor.getSelectionStart(),end=editor==null?0:editor.getSelectionEnd();boolean prior=silent;silent=true;try{field=next;if(!next.label.contentEquals(label.getText()))label.setText(next.label);if(changed){if(dateRow==this)removeDatePopup();value.setText(next.value);if(date!=null){date.setValue(WmDateField.parseValue(next.value));value.setText(date.getContentDescription());}if(editor!=null){editor.setText(next.value);caret(editor,start,end);}}setContentDescription(next.label+" "+next.value);if(arrow!=null)arrow.setContentDescription(next.label);}finally{silent=prior;}}
        void activate(boolean selectAll){if(date!=null){date.setVisibility(VISIBLE);value.setVisibility(INVISIBLE);date.requestFocus();if(selectAll)date.performClick();return;}ensureEditor();if(editor==null){listener.open(field);return;}editor.setVisibility(VISIBLE);value.setVisibility(INVISIBLE);editor.requestFocus();if(selectAll)editor.selectAll();}
        @Override public boolean performClick(){if(!isEnabled())return false;super.performClick();activate(true);return true;}
        @Override protected void onMeasure(int widthSpec,int heightSpec){int width=MeasureSpec.getSize(widthSpec),right=Math.max(155,width-(arrow==null?10:42));label.measure(MeasureSpec.makeMeasureSpec(Math.min(148,width),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(37,MeasureSpec.EXACTLY));value.measure(MeasureSpec.makeMeasureSpec(Math.max(0,right-155),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(37,MeasureSpec.EXACTLY));if(editor!=null)editor.measure(MeasureSpec.makeMeasureSpec(Math.max(0,right-157),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(37,MeasureSpec.EXACTLY));if(date!=null)date.measure(MeasureSpec.makeMeasureSpec(Math.max(0,width-167),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(35,MeasureSpec.EXACTLY));if(arrow!=null)arrow.measure(MeasureSpec.makeMeasureSpec(32,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(37,MeasureSpec.EXACTLY));setMeasuredDimension(width,39);}
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){label.layout(0,0,label.getMeasuredWidth(),37);value.layout(155,0,155+value.getMeasuredWidth(),37);if(editor!=null)editor.layout(157,0,157+editor.getMeasuredWidth(),37);if(date!=null)date.layout(157,2,157+date.getMeasuredWidth(),37);if(arrow!=null)arrow.layout(getWidth()-42,0,getWidth()-10,37);}
        @Override protected void onDetachedFromWindow(){touch.cancel(this);labelTouch.cancel(label);valueTouch.cancel(value);super.onDetachedFromWindow();}
    }
    private final class Arrow extends View {
        final Row row;final Paint ink=new Paint();final WmCommandBar.KeyTouch touch=new WmCommandBar.KeyTouch(false);
        Arrow(Context context,Row row){super(context);this.row=row;setClickable(true);setFocusable(true);setSoundEffectsEnabled(false);setContentDescription(row.field.label);setOnTouchListener(touch);setOnClickListener(view->listener.open(row.field));}
        @Override protected void drawableStateChanged(){super.drawableStateChanged();invalidate();}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);ink.setColor(Color.BLACK);WmComboGlyph.draw(canvas,ink,0,0,getWidth(),getHeight(),isPressed());}
        @Override protected void onDetachedFromWindow(){touch.cancel(this);super.onDetachedFromWindow();}
    }
}
