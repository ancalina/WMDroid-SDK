package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Captured WM614 Speed Dial row layout. Host owns storage, editing and dialing. */
public final class WmSpeedDialView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener {
    public static final class Entry {
        public final int slot;public final String name,number;
        public Entry(int slot,String name,String number){
            if(slot<1||slot>99||name==null||name.trim().isEmpty())throw new IllegalArgumentException("Invalid speed dial entry");
            this.slot=slot;this.name=name;this.number=WmPhoneView.dialable(number==null?"":number);
        }
    }
    private final LinearLayout rows;
    private Entry[] entries;
    private int selected=-1,selection;
    private Typeface regular,bold;
    private String language="ko";
    public WmSpeedDialView(Context context,Typeface regular,Typeface bold,int selection,Entry[] entries){
        super(context);setOrientation(VERTICAL);this.regular=regular;this.bold=bold;this.selection=selection;
        rows=new LinearLayout(context);rows.setOrientation(VERTICAL);addView(new WmScrollView(context,rows),new LayoutParams(-1,-1));setEntries(entries);
    }
    public Entry selected(){return selected<0?null:entries[selected];}
    public void setEntries(Entry[] entries){
        if(entries==null)throw new IllegalArgumentException("Speed dial entries required");boolean[] slots=new boolean[100];
        for(Entry entry:entries){if(entry==null||slots[entry.slot])throw new IllegalArgumentException("Duplicate speed dial slot");slots[entry.slot]=true;}
        int old=selected()==null?-1:selected().slot;this.entries=entries.clone();selected=entries.length==0?-1:0;
        for(int i=0;i<entries.length;i++)if(entries[i].slot==old)selected=i;
        rebuild();
    }
    private void rebuild(){
        rows.removeAllViews();
        for(int i=0;i<entries.length;i++){
            final int index=i;Entry entry=entries[i];LinearLayout row=new LinearLayout(getContext());row.setOrientation(HORIZONTAL);row.setSoundEffectsEnabled(false);
            TextView key=WmControls.text(getContext(),regular,String.valueOf(entry.slot));key.setTextColor(Color.WHITE);key.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,40);key.setGravity(Gravity.CENTER);
            GradientDrawable skin=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xffc8cdd1,0xff737f8a,0xff43515d,0xff576771});skin.setCornerRadius(8);skin.setStroke(1,0xffdde2e4);key.setBackgroundDrawable(skin);LayoutParams keySize=new LayoutParams(86,56);keySize.setMargins(0,8,6,4);row.addView(key,keySize);
            TextView name=WmControls.text(getContext(),bold,entry.slot==1?WmStrings.text(getContext(),language,"음성 사서함"):entry.name);name.setGravity(Gravity.TOP);name.setPadding(0,6,4,0);name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(name,new LayoutParams(0,68,1));
            row.setContentDescription(key.getText()+" "+name.getText());row.setOnClickListener(v->{selected=index;paintSelection();});rows.addView(row,new LayoutParams(-1,68));
        }paintSelection();
    }
    private void paintSelection(){for(int i=0;i<rows.getChildCount();i++){LinearLayout row=(LinearLayout)rows.getChildAt(i);boolean chosen=i==selected;row.setSelected(chosen);row.setBackgroundColor(chosen?WmPalette.highlight(selection):Color.WHITE);((TextView)row.getChildAt(1)).setTextColor(chosen?Color.WHITE:Color.BLACK);}}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){this.regular=regular;this.bold=bold;this.selection=selection;rebuild();}
    @Override public void onWmLanguageChanged(String language){this.language=language;rebuild();}
}
