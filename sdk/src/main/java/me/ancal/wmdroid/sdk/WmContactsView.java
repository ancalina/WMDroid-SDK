package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

/** Captured native Contacts client with owner-supplied immutable rows.
 * Native 54px rows and completed single-click opening are evidenced by the
 * synthetic emulator fixture. No provider, permission or inferred long press.
 */
public final class WmContactsView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener {
    public static final class Entry {
        public final String key,name,secondary;
        private final String normalizedName;
        public Entry(String key,String name){this(key,name,null);}
        public Entry(String key,String name,String secondary){if(key==null||key.isEmpty())throw new IllegalArgumentException("Contact key required");this.key=key;this.name=name==null?"":name;this.secondary=secondary==null?"":secondary;normalizedName=this.name.toLowerCase(Locale.ROOT);}
    }
    public interface Listener {void selected(Entry entry);void open(Entry entry);}
    private static final String EMPTY="새 연락처를 만들려면 새로 만들기를 선택하세요.",NO_RESULTS="검색 결과가 없습니다.",HINT="이름 입력...";
    private static final String[] GROUPS={"#ab","cde","fgh","ijk","lmn","opq","rst","uvw","xyz"};
    private static final int ROW_HEIGHT=54;
    private final Listener listener;
    private final EditText search;
    private final FrameLayout header,body;
    private final TextView message;
    private final TextView[] groups=new TextView[9];
    private final ListView list;
    private final WmScrollBar bar;
    private final ContactsAdapter adapter=new ContactsAdapter();
    private final ArrayList<Entry> filtered=new ArrayList<>();
    private Entry[] entries=new Entry[0];
    private Entry selected;
    private Typeface regular,bold;
    private int selection;
    private String language="ko",authoredMessage;
    private Drawable contactIcon;

    public WmContactsView(Context context,Typeface regular,Typeface bold,int selection,Listener listener){
        super(context);if(regular==null||bold==null||listener==null)throw new IllegalArgumentException("Contacts fonts and listener required");
        this.regular=regular;this.bold=bold;this.selection=selection;this.listener=listener;
        setOrientation(VERTICAL);setSoundEffectsEnabled(false);
        refreshArtwork();
        header=new FrameLayout(context);addView(header,new LayoutParams(-1,54));
        search=new EditText(context);WmControls.input(search,bold);search.setSingleLine(true);search.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,24);search.setHintTextColor(Color.BLACK);search.setSelectAllOnFocus(false);
        WmControls.bindHint(search,language,HINT);WmControls.bindDescription(search,language,HINT);
        FrameLayout.LayoutParams editBounds=new FrameLayout.LayoutParams(-1,40);editBounds.leftMargin=6;editBounds.rightMargin=6;editBounds.topMargin=6;header.addView(search,editBounds);
        LinearLayout alphabet=new LinearLayout(context);alphabet.setOrientation(HORIZONTAL);addView(alphabet,new LayoutParams(-1,35));
        for(int i=0;i<groups.length;i++){
            final int group=i;TextView tab=WmControls.text(context,regular,GROUPS[i]);tab.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,24);tab.setGravity(Gravity.CENTER);tab.setSingleLine(true);tab.setPadding(0,0,0,0);tab.setFocusable(true);tab.setContentDescription(GROUPS[i]);tab.setOnClickListener(v->scrollGroup(group));tab.setOnTouchListener(new WmCommandBar.KeyTouch(false));groups[i]=tab;alphabet.addView(tab,new LayoutParams(0,-1,1));
        }
        body=new FrameLayout(context);addView(body,new LayoutParams(-1,0,1));
        list=new ListView(context);list.setDivider(null);list.setDividerHeight(0);list.setSelector(new ColorDrawable(Color.TRANSPARENT));list.setCacheColorHint(Color.TRANSPARENT);list.setBackgroundColor(Color.TRANSPARENT);list.setVerticalScrollBarEnabled(false);list.setHorizontalScrollBarEnabled(false);list.setOverScrollMode(OVER_SCROLL_NEVER);list.setSoundEffectsEnabled(false);list.setAdapter(adapter);list.setOnItemClickListener((parent,row,position,id)->{if(position>=0&&position<filtered.size())choose(filtered.get(position));});body.addView(list,new FrameLayout.LayoutParams(-1,-1));
        bar=new WmScrollBar(context,new WmScrollBar.Host(){
            public int extent(){return list.getHeight();}public int range(){return filtered.size()*ROW_HEIGHT;}
            public int offset(){View first=list.getChildAt(0);return list.getFirstVisiblePosition()*ROW_HEIGHT-(first==null?0:first.getTop());}
            public int lineStep(){return ROW_HEIGHT;}
            public void scrollTo(int offset){int target=Math.max(0,offset);list.setSelectionFromTop(target/ROW_HEIGHT,-target%ROW_HEIGHT);}
            public void scrollBy(int delta){scrollTo(offset()+delta);}
            public void stopFling(){long now=android.os.SystemClock.uptimeMillis();android.view.MotionEvent down=android.view.MotionEvent.obtain(now,now,android.view.MotionEvent.ACTION_DOWN,0,0,0),cancel=android.view.MotionEvent.obtain(now,now,android.view.MotionEvent.ACTION_CANCEL,0,0,0);try{list.onTouchEvent(down);list.onTouchEvent(cancel);list.onInterceptTouchEvent(cancel);}finally{down.recycle();cancel.recycle();}}
        },WmPalette.face(selection));body.addView(bar,new FrameLayout.LayoutParams(26,-1,Gravity.RIGHT));bar.setVisibility(GONE);
        message=WmControls.text(context,regular,"");message.setGravity(Gravity.CENTER);message.setPadding(12,0,12,0);body.addView(message,new FrameLayout.LayoutParams(-1,-1));
        list.setOnScrollListener(new AbsListView.OnScrollListener(){public void onScrollStateChanged(AbsListView view,int state){}public void onScroll(AbsListView view,int first,int visible,int total){refreshBar();}});
        list.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->refreshBar());
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){filter();}public void afterTextChanged(Editable text){}});
        onWmRestyle(regular,bold,selection,WmPalette.face(selection));renderMessage();
    }
    public EditText search(){return search;}
    public String query(){return search.getText().toString();}
    public void setQuery(String query){if(query==null)throw new IllegalArgumentException("Query required");if(!query.equals(query()))search.setText(query);}
    public Entry selected(){return selected;}
    /** Validates the complete replacement before changing any client state. */
    public void setEntries(Entry[] entries){
        if(entries==null)throw new IllegalArgumentException("Contacts required");Entry[] copy=entries.clone();HashSet<String> keys=new HashSet<>();Entry retained=null;
        for(Entry entry:copy){if(entry==null||!keys.add(entry.key))throw new IllegalArgumentException("Null or duplicate contact key");if(selected!=null&&selected.key.equals(entry.key))retained=entry;}
        boolean same=copy.length==this.entries.length;
        for(int i=0;same&&i<copy.length;i++)same=copy[i].key.equals(this.entries[i].key)&&copy[i].name.equals(this.entries[i].name)&&copy[i].secondary.equals(this.entries[i].secondary);
        int first=list.getFirstVisiblePosition();View row=list.getChildAt(0);int top=row==null?0:row.getTop();
        this.entries=copy;selected=retained;if(same)return;filter();list.setSelectionFromTop(first,top);
    }
    /** Non-null loading/permission/error text owns the body; null restores data. */
    public void setMessage(String authored){authoredMessage=authored;renderMessage();}
    private void filter(){
        String needle=query().toLowerCase(Locale.ROOT);filtered.clear();for(Entry entry:entries)if(entry.normalizedName.contains(needle))filtered.add(entry);
        adapter.notifyDataSetChanged();list.setSelectionFromTop(0,0);renderMessage();refreshBar();
    }
    private void choose(Entry entry){selected=entry;adapter.notifyDataSetChanged();listener.selected(entry);listener.open(entry);}
    private void scrollGroup(int group){
        if(authoredMessage!=null)return;
        for(int i=0;i<filtered.size();i++){
            String name=filtered.get(i).name;if(name.isEmpty())continue;char initial=Character.toLowerCase(name.charAt(0));
            // ASCII buckets only: Korean collation was not established by evidence.
            boolean match=group==0?(initial>='0'&&initial<='9'||initial=='a'||initial=='b'||initial=='#'):GROUPS[group].indexOf(initial)>=0;
            if(match){list.setSelectionFromTop(i,0);return;}
        }
    }
    private void renderMessage(){
        boolean show=authoredMessage!=null||filtered.isEmpty();message.setVisibility(show?VISIBLE:GONE);list.setVisibility(show?GONE:VISIBLE);
        message.setText(show?WmStrings.text(getContext(),language,authoredMessage!=null?authoredMessage:entries.length==0?EMPTY:NO_RESULTS):"");if(show)bar.setVisibility(GONE);else refreshBar();
    }
    private void refreshBar(){
        if(bar==null||message==null)return;if(list.getVisibility()!=VISIBLE){bar.setVisibility(GONE);return;}bar.refresh();
        FrameLayout.LayoutParams size=(FrameLayout.LayoutParams)list.getLayoutParams();int inset=bar.getVisibility()==VISIBLE?26:0;if(size.rightMargin!=inset){size.rightMargin=inset;list.setLayoutParams(size);}
    }
    @Override public void onWmLanguageChanged(String language){this.language=language;WmControls.bindHint(search,language,HINT);WmControls.bindDescription(search,language,HINT);renderMessage();}
    private String artworkRevision;
    private void refreshArtwork(){
        if(artworkRevision==null)try{WmResources.prepare(getContext());}catch(java.io.IOException absent){}
        String revision=WmResources.revision(getContext());if(revision.equals(artworkRevision))return;
        contactIcon=new android.graphics.drawable.BitmapDrawable(getResources(),WmResources.iconBitmap(getContext(),"apps/contacts.png",64,64,"👥"));
        artworkRevision=revision;
    }
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        if(regular==null||bold==null)throw new IllegalArgumentException("Contacts fonts required");this.regular=regular;this.bold=bold;this.selection=selection;
        refreshArtwork();
        int first=list.getFirstVisiblePosition();View firstRow=list.getChildAt(0);int top=firstRow==null?0:firstRow.getTop();
        header.setBackgroundColor(WmPalette.face(selection));GradientDrawable edit=new GradientDrawable();edit.setColor(Color.WHITE);edit.setStroke(2,Color.BLACK);search.setBackgroundDrawable(edit);search.setTypeface(bold);WmFonts.attach(search,bold);
        for(TextView group:groups){group.setTypeface(regular);WmFonts.attach(group,regular);StateListDrawable skin=new StateListDrawable();skin.addState(new int[]{android.R.attr.state_pressed},groupFace(selection));skin.addState(new int[]{},groupFace(Color.WHITE));skin.setEnterFadeDuration(0);skin.setExitFadeDuration(0);group.setBackgroundDrawable(skin);}
        body.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.WHITE,WmPalette.face(selection)}));message.setTypeface(regular);WmFonts.attach(message,regular);bar.onWmRestyle(regular,bold,selection,tabColor);adapter.notifyDataSetChanged();list.setSelectionFromTop(first,top);
    }
    private static GradientDrawable groupFace(int color){GradientDrawable face=new GradientDrawable();face.setColor(color);face.setStroke(1,Color.BLACK);return face;}
    private final class ContactsAdapter extends BaseAdapter {
        public int getCount(){return filtered.size();}public Entry getItem(int position){return filtered.get(position);}public long getItemId(int position){return position;}
        public View getView(int position,View recycled,ViewGroup parent){Row row=recycled instanceof Row?(Row)recycled:new Row(getContext());row.touch.cancel(row);row.entry=getItem(position);row.captionDirty=true;row.setContentDescription(row.entry.name+(row.entry.secondary.isEmpty()?"":" "+row.entry.secondary));row.setSelected(selected!=null&&selected.key.equals(row.entry.key));row.invalidate();return row;}
    }
    private final class Row extends View {
        Entry entry;final WmCommandBar.KeyTouch touch=new WmCommandBar.KeyTouch(false);
        final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);boolean captionDirty=true;String nameCaption="",secondaryCaption="";
        Row(Context context){super(context);setLayoutParams(new AbsListView.LayoutParams(-1,ROW_HEIGHT));setClickable(true);setSoundEffectsEnabled(false);setOnTouchListener(touch);}
        @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){captionDirty=true;}
        @Override protected void drawableStateChanged(){super.drawableStateChanged();invalidate();}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);if(entry==null)return;
            boolean highlight=isPressed()||isSelected();if(highlight)canvas.drawColor(WmPalette.highlight(selection));
            if(contactIcon!=null){contactIcon.setBounds(16,12,48,44);contactIcon.draw(canvas);}
            float available=Math.max(0,getWidth()-60-8);
            if(captionDirty){ink.setTypeface(bold);ink.setTextSize(24);nameCaption=WmFonts.ellipsize(entry.name,ink,available);ink.setTypeface(regular);ink.setTextSize(20);secondaryCaption=WmFonts.ellipsize(entry.secondary,ink,available);captionDirty=false;}
            ink.setColor(highlight?Color.WHITE:Color.BLACK);ink.setTypeface(bold);ink.setTextSize(24);WmFonts.draw(canvas,nameCaption,60,24,ink);
            ink.setTypeface(regular);ink.setTextSize(20);if(!secondaryCaption.isEmpty())WmFonts.draw(canvas,secondaryCaption,60,49,ink);
            ink.setColor(0xff808080);canvas.drawRect(0,ROW_HEIGHT-1,getWidth(),ROW_HEIGHT,ink);
        }
        @Override public boolean performClick(){if(!isEnabled()||entry==null)return false;super.performClick();choose(entry);return true;}
        @Override protected void onDetachedFromWindow(){touch.cancel(this);super.onDetachedFromWindow();}
    }
    @Override protected void onDetachedFromWindow(){bar.stop();super.onDetachedFromWindow();}
}
