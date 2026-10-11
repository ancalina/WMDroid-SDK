package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.io.IOException;
import java.util.concurrent.Executor;

/** Now Playing list. Native empty-screen capture supplies the 46px header and
 * toolbar. The 34px rows reuse the shared small-icon list metric; populated
 * native row selection/raster and disabled-toolbar GDI remain unverified. */
public final class WmMediaPlaylistView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener {
    public static final int UP=30002,DOWN=30003,REMOVE=30004,ADD=30009,SAVE=30006,PROPERTIES=30010,PLAY=30001;
    public interface Listener {void command(int id);}
    // API14 worker View.post before attachment uses the worker thread run queue.
    private final android.os.Handler ui=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Listener listener;
    private final Header header;
    private final ListView list;
    private final WmScrollBar bar;
    private final Tool[] tools=new Tool[5];
    private Bitmap[] icons=new Bitmap[6];
    private WmMediaQueue.Entry[] entries=new WmMediaQueue.Entry[0];
    private int selected=-1,current=-1,selection=0xff149ca7,face=0xffc9e7e9;
    private Typeface font;
    private String language="ko";
    private Executor iconWorker;
    private String iconRevision,pendingIconRevision;
    private int iconGeneration;
    private boolean closed;
    private final BaseAdapter adapter=new BaseAdapter(){public int getCount(){return entries.length;}public Object getItem(int index){return entries[index];}public long getItemId(int index){return index;}public View getView(int index,View old,ViewGroup parent){TextView row=old instanceof TextView?(TextView)old:new TextView(getContext());WmControls.textStyle(row,font);row.setText(entries[index].title);row.setSingleLine(true);row.setEllipsize(android.text.TextUtils.TruncateAt.END);row.setPadding(10,0,8,0);row.setLayoutParams(new AbsListView.LayoutParams(-1,34));row.setBackgroundColor(index==selected?WmPalette.highlight(selection):Color.TRANSPARENT);row.setTextColor(index==selected?Color.WHITE:Color.BLACK);row.setContentDescription(entries[index].title+(index==current?" "+text("지금 재생"):""));return row;}};
    private String text(String value){return WmStrings.text(getContext(),language,value);}
    public WmMediaPlaylistView(Context context,Typeface font,Listener listener){
        super(context);if(listener==null)throw new IllegalArgumentException("Listener required");this.font=font;this.listener=listener;setOrientation(VERTICAL);
        header=new Header(context);addView(header,new LayoutParams(-1,46));
        FrameLayout body=new FrameLayout(context){@Override protected void onMeasure(int w,int h){boolean scroll=(long)entries.length*34>MeasureSpec.getSize(h);((FrameLayout.LayoutParams)list.getLayoutParams()).rightMargin=scroll?26:0;bar.setVisibility(scroll?VISIBLE:GONE);super.onMeasure(w,h);}};addView(body,new LayoutParams(-1,0,1));
        list=new ListView(context);list.setDivider(null);list.setDividerHeight(0);list.setSelector(new ColorDrawable(Color.TRANSPARENT));list.setCacheColorHint(Color.TRANSPARENT);list.setVerticalScrollBarEnabled(false);list.setOverScrollMode(OVER_SCROLL_NEVER);list.setAdapter(adapter);list.setOnItemClickListener((p,v,index,id)->select(index));list.setOnItemLongClickListener((p,v,index,id)->{select(index);listener.command(0);return true;});body.addView(list,new FrameLayout.LayoutParams(-1,-1));
        bar=new WmScrollBar(context,new WmScrollBar.Host(){public int extent(){return list.getHeight();}public int range(){return entries.length*34;}public int offset(){View first=list.getChildAt(0);return list.getFirstVisiblePosition()*34-(first==null?0:first.getTop());}public int lineStep(){return 34;}public void scrollTo(int y){int n=Math.max(0,y);list.setSelectionFromTop(n/34,-n%34);}public void scrollBy(int delta){scrollTo(offset()+delta);}public void stopFling(){long now=android.os.SystemClock.uptimeMillis();android.view.MotionEvent event=android.view.MotionEvent.obtain(now,now,android.view.MotionEvent.ACTION_CANCEL,0,0,0);try{list.onTouchEvent(event);}finally{event.recycle();}}},face);body.addView(bar,new FrameLayout.LayoutParams(26,-1,Gravity.RIGHT));bar.setVisibility(GONE);
        list.setOnScrollListener(new AbsListView.OnScrollListener(){public void onScrollStateChanged(AbsListView view,int state){}public void onScroll(AbsListView view,int first,int count,int total){bar.refresh();}});
        LinearLayout toolbar=new LinearLayout(context);int[] ids={UP,DOWN,ADD,REMOVE,PROPERTIES};String[] names={"위로 이동","아래로 이동","라이브러리...","제거","속성"};String[] symbols={"↑","↓","+","×","▣"};
        for(int n=0;n<5;n++){final int command=ids[n];tools[n]=new Tool(context,n+1,symbols[n]);tools[n].setContentDescription(names[n]);tools[n].setOnClickListener(v->listener.command(command));toolbar.addView(tools[n],new LayoutParams(46,46));}addView(toolbar,new LayoutParams(-1,46));onWmRestyle(font,font,selection,face);updateButtons();
    }
    public void loadIcons(Executor worker){
        if(closed)return;if(worker==null)throw new IllegalArgumentException("Icon worker required");
        iconWorker=worker;refreshIcons(true);
    }
    private void refreshIcons(boolean force){
        if(closed||iconWorker==null)return;
        String revision=WmResources.revision(getContext());
        if(revision.equals(pendingIconRevision))return;
        if(!force&&revision.equals(iconRevision)){
            if(pendingIconRevision!=null){iconGeneration++;pendingIconRevision=null;}
            return;
        }
        final int generation=++iconGeneration;pendingIconRevision=revision;
        try{iconWorker.execute(()->{
            final String loadedRevision;
            // Initial prepare can replace the no-I/O bundled revision with a pack.
            synchronized(WmResources.class){try{WmResources.prepare(getContext());}catch(IOException absent){}loadedRevision=WmResources.revision(getContext());}
            String[] names={"now-playing-header","queue-up","queue-down","queue-add","queue-remove","queue-save"};Bitmap[] loaded=new Bitmap[6];
            for(int n=0;n<names.length;n++)try{loaded[n]=WmResources.bitmap(getContext(),"media/"+names[n]+".png",16,16);}catch(IOException absent){}
            final boolean consistent=loadedRevision.equals(WmResources.revision(getContext()));
            ui.post(()->{
                if(closed||generation!=iconGeneration||!consistent||!loadedRevision.equals(WmResources.revision(getContext()))){
                    recycleIcons(loaded);
                    if(!closed&&generation==iconGeneration){pendingIconRevision=null;refreshIcons(false);}
                    return;
                }
                pendingIconRevision=null;iconRevision=loadedRevision;recycleIcons(icons);icons=loaded;header.invalidate();for(Tool tool:tools)tool.invalidate();
            });
        });}catch(RuntimeException rejected){if(generation==iconGeneration)pendingIconRevision=null;throw rejected;}
    }
    private static void recycleIcons(Bitmap[] values){for(Bitmap icon:values)if(icon!=null)icon.recycle();}
    public void close(){if(closed)return;closed=true;iconGeneration++;pendingIconRevision=null;iconWorker=null;recycleIcons(icons);icons=new Bitmap[6];}
    public void setEntries(WmMediaQueue.Entry[] values,int current){if(values==null)throw new IllegalArgumentException("Sources required");boolean changed=!java.util.Arrays.equals(entries,values)||this.current!=current;entries=values.clone();this.current=current;if(selected>=entries.length)selected=entries.length-1;if(selected<0&&entries.length>0)selected=current>=0?current:0;if(changed){adapter.notifyDataSetChanged();requestLayout();}updateButtons();}
    /** Legacy source compatibility: Save is a menu command, not a toolbar button. */
    @Deprecated public void setSaveEnabled(boolean value){}
    public int selectedIndex(){return selected;}
    public void select(int index){if(index<0||index>=entries.length)throw new IllegalArgumentException("Selection outside sources");selected=index;adapter.notifyDataSetChanged();updateButtons();}
    private void updateButtons(){tools[0].setEnabled(selected>0);tools[1].setEnabled(selected>=0&&selected<entries.length-1);tools[2].setEnabled(true);tools[3].setEnabled(selected>=0);tools[4].setEnabled(selected>=0);for(Tool tool:tools)tool.invalidate();}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int color,int tabColor){font=regular;selection=color;face=tabColor;setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.WHITE,face}));header.setBackgroundColor(face);((View)tools[0].getParent()).setBackgroundColor(face);adapter.notifyDataSetChanged();header.invalidate();bar.onWmRestyle(regular,bold,color,tabColor);for(Tool tool:tools)tool.invalidate();refreshIcons(false);}
    @Override public void onWmLanguageChanged(String value){language=WmStrings.language(getContext(),value);String[] names={"위로 이동","아래로 이동","라이브러리...","제거","속성"};for(int n=0;n<5;n++)tools[n].setContentDescription(text(names[n]));header.setContentDescription(text("지금 재생"));header.invalidate();adapter.notifyDataSetChanged();}
    private class Header extends View {final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);Header(Context context){super(context);setContentDescription("지금 재생");}protected void onDraw(Canvas c){paint.setColor(Color.BLACK);paint.setTypeface(font);paint.setTextSize(24);paint.setAlpha(255);if(icons[0]!=null)c.drawBitmap(icons[0],null,new Rect(4,7,36,39),paint);else WmFonts.draw(c,"▶",10,31,paint);Paint.FontMetrics metrics=paint.getFontMetrics();WmFonts.draw(c,text("지금 재생"),40,23-(metrics.ascent+metrics.descent)/2,paint);}}
    private class Tool extends View {final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final int icon;final String symbol;Tool(Context context,int icon,String symbol){super(context);this.icon=icon;this.symbol=symbol;setFocusable(true);setClickable(true);}protected void onDraw(Canvas c){paint.setAlpha(255);if(isPressed()){paint.setColor(WmPalette.highlight(selection));c.drawRect(0,0,getWidth(),getHeight(),paint);}paint.setAlpha(isEnabled()?255:100);if(icons[icon]!=null)c.drawBitmap(icons[icon],null,new Rect(7,7,39,39),paint);else{paint.setColor(isEnabled()?Color.BLACK:0xff808080);paint.setTypeface(font);paint.setTextSize(30);paint.setTextAlign(Paint.Align.CENTER);WmFonts.draw(c,symbol,23,34,paint);}paint.setAlpha(255);}}
}
