package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import java.util.HashSet;

/** WM6.1.4 Favorites:34px rows, tap selection, Open softkey and double tap.
 * Owner supplies entries, folder navigation, commands and persistence. */
public final class WmBrowserFavoritesView extends FrameLayout implements WmControls.RestyleListener {
    public static final class Entry {
        public final String key,name;public final boolean folder;
        public Entry(String key,String name,boolean folder){if(key==null||name==null)throw new IllegalArgumentException("Favorite key/name required");this.key=key;this.name=name;this.folder=folder;}
    }
    public interface Listener {void open(Entry entry);}
    private final Listener listener;
    private final ListView list;
    private final WmScrollBar bar;
    private Bitmap folderIcon,linkIcon;
    private String resourceRevision;
    private Typeface font;
    private int accent;
    private Entry[] entries=new Entry[0];
    private Entry selected;
    private String lastTap;
    private long lastTapTime;
    private final BaseAdapter adapter=new BaseAdapter(){
        public int getCount(){return entries.length;}public Object getItem(int n){return entries[n];}public long getItemId(int n){return n;}
        public View getView(int n,View old,ViewGroup parent){Row row=old instanceof Row?(Row)old:new Row();row.entry=entries[n];row.dirty=true;row.setContentDescription(row.entry.name);row.setLayoutParams(new AbsListView.LayoutParams(-1,34));row.invalidate();return row;}
    };
    public WmBrowserFavoritesView(Context context,Typeface font,int accent,Listener listener){
        super(context);if(font==null||listener==null)throw new IllegalArgumentException("Favorite font/listener required");this.font=font;this.accent=accent;this.listener=listener;setBackgroundColor(Color.WHITE);
        refreshIcons();
        list=new ListView(context);list.setDivider(null);list.setDividerHeight(0);list.setSelector(new ColorDrawable(Color.TRANSPARENT));list.setCacheColorHint(Color.TRANSPARENT);list.setVerticalScrollBarEnabled(false);list.setOverScrollMode(OVER_SCROLL_NEVER);list.setAdapter(adapter);addView(list,new LayoutParams(-1,-1));
        list.setOnItemClickListener((p,v,n,id)->{if(!isEnabled())return;selected=entries[n];list.invalidateViews();long now=android.os.SystemClock.uptimeMillis();boolean twice=selected.key.equals(lastTap)&&now-lastTapTime<=ViewConfiguration.getDoubleTapTimeout();lastTap=twice?null:selected.key;lastTapTime=now;if(twice)listener.open(selected);});
        bar=new WmScrollBar(context,new WmScrollBar.Host(){
            public int extent(){return list.getHeight();}public int range(){return entries.length*34;}public int offset(){View first=list.getChildAt(0);return list.getFirstVisiblePosition()*34-(first==null?0:first.getTop());}public int lineStep(){return 34;}
            public void scrollTo(int y){int n=Math.max(0,y);list.setSelectionFromTop(n/34,-n%34);}public void scrollBy(int d){scrollTo(offset()+d);}
            public void stopFling(){long now=android.os.SystemClock.uptimeMillis();android.view.MotionEvent cancel=android.view.MotionEvent.obtain(now,now,android.view.MotionEvent.ACTION_CANCEL,0,0,0);try{list.onTouchEvent(cancel);}finally{cancel.recycle();}}
        },WmPalette.face(accent));addView(bar,new LayoutParams(26,-1,Gravity.RIGHT));bar.setVisibility(GONE);
        list.setOnScrollListener(new AbsListView.OnScrollListener(){public void onScrollStateChanged(AbsListView v,int state){}public void onScroll(AbsListView v,int first,int count,int total){bar.refresh();}});
    }
    @Override protected void onMeasure(int width,int height){boolean scroll=MeasureSpec.getSize(height)>0&&(long)entries.length*34>MeasureSpec.getSize(height);((LayoutParams)list.getLayoutParams()).rightMargin=scroll?26:0;bar.setVisibility(scroll?VISIBLE:GONE);super.onMeasure(width,height);}
    public void setEntries(Entry[] values){
        if(values==null)throw new IllegalArgumentException("Favorite entries required");HashSet<String> keys=new HashSet<>();Entry[] copy=values.clone();Entry retained=null;
        for(Entry entry:copy){if(entry==null||!keys.add(entry.key))throw new IllegalArgumentException("Duplicate/null favorite key");if(selected!=null&&entry.key.equals(selected.key))retained=entry;}
        entries=copy;selected=retained!=null?retained:copy.length==0?null:copy[0];lastTap=null;adapter.notifyDataSetChanged();requestLayout();
    }
    public Entry selected(){return selected;}
    public void openSelection(){if(isEnabled()&&selected!=null)listener.open(selected);}
    @Override public void setEnabled(boolean enabled){super.setEnabled(enabled);lastTap=null;if(list!=null)list.setEnabled(enabled);if(bar!=null)bar.setEnabled(enabled);}
    private void refreshIcons(){String revision=WmResources.revision(getContext());if(revision.equals(resourceRevision))return;folderIcon=WmResources.iconBitmap(getContext(),"files/folder-closed.png",32,32,"📁");linkIcon=WmResources.iconBitmap(getContext(),"launcher/icons/browser.png",32,32,"🌐");resourceRevision=revision;}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){font=regular;accent=selection;refreshIcons();bar.onWmRestyle(regular,bold,selection,tabColor);list.invalidateViews();}
    private final class Row extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final android.graphics.Rect icon=new android.graphics.Rect();private Entry entry;private boolean dirty=true;private WmFonts.PreparedLine caption;private float baseline;
        Row(){super(WmBrowserFavoritesView.this.getContext());setSoundEffectsEnabled(false);}
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){dirty=true;}
        @Override protected void onDraw(Canvas canvas){
            if(entry==null)return;boolean active=selected!=null&&entry.key.equals(selected.key);if(active)canvas.drawColor(WmPalette.highlight(accent));
            icon.set(4,1,36,33);canvas.drawBitmap(entry.folder?folderIcon:linkIcon,null,icon,paint);
            if(dirty){paint.setTypeface(font);paint.setTextSize(24);paint.setColor(active?Color.WHITE:Color.BLACK);Paint.FontMetrics metric=paint.getFontMetrics();baseline=(getHeight()-metric.top-metric.bottom)/2;caption=WmFonts.prepare(paint,WmFonts.ellipsize(entry.name,paint,Math.max(0,getWidth()-44)));dirty=false;}
            caption.draw(canvas,40,baseline);
        }
    }
}
