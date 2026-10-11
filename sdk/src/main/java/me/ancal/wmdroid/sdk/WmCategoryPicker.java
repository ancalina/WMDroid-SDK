package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/** PIM category checked-list draft. The owner supplies its catalog, New/OK/Cancel
 * commands and persistence. Android collation/row metrics replace native NLS/GDI. */
public final class WmCategoryPicker extends FrameLayout implements WmControls.RestyleListener {
    private final String original;
    private String draft;
    private final Collator collator;
    private final ArrayList<String> names=new ArrayList<String>();
    private final ArrayList<CheckBox> checks=new ArrayList<CheckBox>();
    private final LinearLayout rows;
    private final WmScrollView scroll;
    private Typeface font;
    private boolean binding;
    private CheckBox pendingNewRow;

    public WmCategoryPicker(Context context,Typeface font,Locale locale,String[] catalog,String selected){
        super(context);
        if(font==null||locale==null||catalog==null||selected==null)throw new IllegalArgumentException("Font, locale, catalog and selection required");
        rejectNul(selected);this.font=font;original=selected;
        collator=Collator.getInstance(locale);collator.setStrength(Collator.SECONDARY);
        for(String name:catalog){if(name==null||name.isEmpty()||name.indexOf(',')>=0||name.indexOf(';')>=0)throw new IllegalArgumentException("Nonempty category name required");rejectNul(name);include(name);}
        for(Token token:tokens(selected))include(token.text);
        rows=new LinearLayout(context);rows.setOrientation(LinearLayout.VERTICAL);setBackgroundColor(Color.WHITE);
        scroll=new WmScrollView(context,rows);addView(scroll,new LayoutParams(-1,-1));rebuild();
    }
    /** Snapshot the unaccepted draft, including an untouched null result. */
    public Bundle saveInteractionState(){
        Bundle state=new Bundle();state.putInt("schema",2);state.putString("reveal",pendingNewRow==null?null:pendingNewRow.getText().toString());state.putString("original",original);state.putString("draft",draft);state.putStringArray("catalog",categoryNames());state.putInt("scroll",scroll.getRequestedScrollPosition());return state;
    }
    /** Recreate a detached selector; the owner retains its committed appointment. */
    public static WmCategoryPicker fromInteractionState(Context context,Typeface font,Locale locale,Bundle state){
        if(state==null||!(state.get("schema") instanceof Integer)||(state.getInt("schema")==1?state.size()!=5:state.getInt("schema")!=2||state.size()!=6||!state.containsKey("reveal")||(state.get("reveal")!=null&&!(state.get("reveal") instanceof String)))||!(state.get("original") instanceof String)||!state.containsKey("draft")||(state.get("draft")!=null&&!(state.get("draft") instanceof String))||!(state.get("catalog") instanceof String[])||!(state.get("scroll") instanceof Integer)||state.getInt("scroll")<0)throw new IllegalArgumentException("Category interaction schema1 or schema2 required");
        WmCategoryPicker picker=new WmCategoryPicker(context,font,locale,state.getStringArray("catalog"),state.getString("original"));
        String draft=state.getString("draft");if(draft!=null){rejectNul(draft);for(Token token:tokens(draft))picker.requireKnown(token.text);picker.draft=draft;picker.rebuild();}
        String reveal=state.getInt("schema")==2?state.getString("reveal"):null;
        if(reveal!=null){if(draft==null)throw new IllegalArgumentException("Pending category reveal requires a changed draft");rejectNul(reveal);picker.pendingNewRow=picker.checks.get(picker.requireKnown(reveal));}
        picker.scroll.scrollToPosition(state.getInt("scroll"));return picker;
    }
    /** Current selection; before the first change this retains the exact input. */
    public String value(){return draft==null?original:draft;}
    /** null means untouched. Empty string means all categories were unchecked. */
    public String result(){return draft;}
    public boolean isChanged(){return draft!=null;}
    public String[] categoryNames(){return names.toArray(new String[names.size()]);}
    /** Query membership without creating a selector. Uses the same token grammar and collation. */
    public static boolean contains(String selected,String name,Locale locale){
        if(selected==null||name==null||locale==null)throw new IllegalArgumentException("Selection, name and locale required");rejectNul(selected);rejectNul(name);
        Collator order=Collator.getInstance(locale);order.setStrength(Collator.SECONDARY);
        for(Token token:tokens(selected))if(order.compare(token.text,name)==0)return true;return false;
    }
    public boolean isChecked(String name){requireKnown(name);return match(value(),name)!=null;}
    /** A same-state call is silent. Toggle-and-revert retains a changed draft. */
    public void setChecked(String name,boolean checked){
        int index=requireKnown(name);name=names.get(index);boolean old=match(value(),name)!=null;if(old==checked)return;
        String current=value();
        if(checked)draft=current.isEmpty()?name:current+", "+name;
        else{
            Token[] match=match(current,name);Token found=match[0],next=match[1];
            if(next!=null)draft=current.substring(0,found.start)+current.substring(next.start);
            else{int end=found.start;while(end>0&&separator(current.charAt(end-1)))end--;draft=current.substring(0,end);}
        }
        binding=true;try{checks.get(index).setChecked(checked);}finally{binding=false;}
    }
    /** Trim U+0020; reject empty, delimiters, NUL and more than255 UTF16 units.
     * A duplicate returns false and changes neither selection nor catalog. */
    public boolean addCategory(String input){
        String name=validateNewName(input);if(index(name)>=0)return false;
        names.add(name);Collections.sort(names,collator);rebuild();setChecked(name,true);pendingNewRow=checks.get(requireKnown(name));return true;
    }
    public static String validateNewName(String input){
        if(input==null)throw new IllegalArgumentException("Category name required");rejectNul(input);
        int left=0,right=input.length();while(left<right&&input.charAt(left)==' ')left++;while(right>left&&input.charAt(right-1)==' ')right--;
        String name=input.substring(left,right);
        if(name.isEmpty()||name.length()>255||name.indexOf(',')>=0||name.indexOf(';')>=0)throw new IllegalArgumentException("Category name must have1..255 UTF16 units and no comma or semicolon");return name;
    }
    private void include(String name){if(index(name)<0)names.add(name);}
    private int index(String name){for(int i=0;i<names.size();i++)if(collator.compare(names.get(i),name)==0)return i;return -1;}
    private int requireKnown(String name){if(name==null)throw new IllegalArgumentException("Category required");int found=index(name);if(found<0)throw new IllegalArgumentException("Unknown category");return found;}
    private void rebuild(){
        Collections.sort(names,collator);rows.removeAllViews();checks.clear();
        for(String name:names){CheckBox check=new CheckBox(getContext());WmControls.check(check,font);check.setText(name);check.setEnabled(isEnabled());check.setSingleLine(true);check.setMinimumHeight(44);check.setChecked(match(value(),name)!=null);check.setPadding(check.getPaddingLeft()+8,2,8,2);rows.addView(check,new LinearLayout.LayoutParams(-1,-2));checks.add(check);check.setOnCheckedChangeListener((view,checked)->{if(!binding&&check.getParent()==rows&&checks.contains(check))setChecked(name,checked);});}
    }
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
        super.onLayout(changed,left,top,right,bottom);
        if(pendingNewRow==null)return;
        CheckBox row=pendingNewRow;if(row.getParent()!=rows){pendingNewRow=null;return;}
        int extent=scroll.getHeight();if(extent<=0||row.getHeight()<=0)return;
        // Native New calls LVM_ENSUREVISIBLE; wait for the sorted row's final layout.
        int position=scroll.getRequestedScrollPosition();
        if(row.getHeight()>extent||row.getTop()<position)position=row.getTop();
        else if(row.getBottom()>position+extent)position=row.getBottom()-extent;
        pendingNewRow=null;scroll.scrollToPosition(position);
    }
    private static final class Token {final int start;final String text;Token(int start,String text){this.start=start;this.text=text;}}
    private static boolean separator(char c){return c==' '||c==','||c==';';}
    private static void rejectNul(String value){if(value.indexOf('\0')>=0)throw new IllegalArgumentException("NUL is not a category character");}
    private static ArrayList<Token> tokens(String value){
        ArrayList<Token> result=new ArrayList<Token>();int cursor=0;
        while(cursor<value.length()){
            while(cursor<value.length()&&separator(value.charAt(cursor)))cursor++;int start=cursor;
            while(cursor<value.length()&&value.charAt(cursor)!=','&&value.charAt(cursor)!=';')cursor++;int end=cursor;
            while(end>start&&value.charAt(end-1)==' ')end--;if(end>start)result.add(new Token(start,value.substring(start,end)));
        }
        return result;
    }
    // Native03535C18 advances past the following token after each match.
    private Token[] match(String value,String name){
        ArrayList<Token> list=tokens(value);Token[] result=null;
        for(int i=0;i<list.size();i++)if(collator.compare(name,list.get(i).text)==0){Token found=list.get(i),next=++i<list.size()?list.get(i):null;result=new Token[]{found,next};}
        return result;
    }
    /** Disable user input without restricting explicit owner draft updates. */
    @Override public void setEnabled(boolean enabled){
        super.setEnabled(enabled);if(scroll==null)return;
        for(CheckBox check:checks)check.setEnabled(enabled);
        scroll.setEnabled(enabled);
    }
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int face){font=regular;for(CheckBox check:checks)WmControls.restyleTree(check,regular,bold,selection,face);}
}
