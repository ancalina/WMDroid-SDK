package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.io.IOException;
import java.util.concurrent.Executor;

/** Native Media Library client: 46px header, 34px icon rows and text-only selection.
 * Owner supplies hierarchy and indexing. Populated metadata pages remain subject
 * to native raster comparison; no system-wide storage permission is implied. */
public final class WmMediaLibraryView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener {
    public static final int OPEN=20001,BACK=-1,SOURCE=-2,CONTEXT=0;
    public static final int LIBRARY=0,MUSIC=1,VIDEO=2,TV=3,PLAYLIST=4,NOW_PLAYING=5,ARTIST=6,ALBUM=7,GENRE=8;
    public static final class Row {
        public final String key,title;public final int icon;public final boolean authored;
        public Row(String key,String title,int icon,boolean authored){if(key==null||key.isEmpty()||title==null||icon<0||icon>8)throw new IllegalArgumentException("Library row key/title/icon required");this.key=key;this.title=title;this.icon=icon;this.authored=authored;}
    }
    // API14 worker View.post before attachment uses the worker thread run queue.
    private final android.os.Handler ui=new android.os.Handler(android.os.Looper.getMainLooper());
    private final WmMediaPlaylistView.Listener listener;
    private final Header header;
    private final ListView list;
    private final WmScrollBar bar;
    private Row[] rows=new Row[0];
    private String title="라이브러리",language="ko";
    private int titleIcon=LIBRARY,selected=-1,selection=0xff149ca7,face=0xffc9e7e9;
    private Typeface font;
    private Bitmap[] icons=new Bitmap[10];
    private Executor iconWorker;
    private String iconRevision,pendingIconRevision;
    private int iconGeneration;
    private boolean closed,titleAuthored=true;
    private final BaseAdapter adapter=new BaseAdapter(){public int getCount(){return rows.length;}public Object getItem(int index){return rows[index];}public long getItemId(int index){return index;}public View getView(int index,View old,ViewGroup parent){Cell cell=old instanceof Cell?(Cell)old:new Cell(getContext());cell.index=index;cell.setContentDescription(caption(rows[index]));cell.setLayoutParams(new AbsListView.LayoutParams(-1,34));cell.invalidate();return cell;}};
    public WmMediaLibraryView(Context context,Typeface font,WmMediaPlaylistView.Listener listener){
        super(context);if(listener==null)throw new IllegalArgumentException("Library listener required");this.font=font;this.listener=listener;setOrientation(VERTICAL);
        header=new Header(context);addView(header,new LayoutParams(-1,46));
        FrameLayout body=new FrameLayout(context){protected void onMeasure(int width,int height){boolean scroll=(long)rows.length*34>MeasureSpec.getSize(height);((FrameLayout.LayoutParams)list.getLayoutParams()).rightMargin=scroll?26:0;bar.setVisibility(scroll?VISIBLE:GONE);super.onMeasure(width,height);}};addView(body,new LayoutParams(-1,0,1));
        list=new ListView(context);list.setDivider(null);list.setSelector(new ColorDrawable(Color.TRANSPARENT));list.setCacheColorHint(Color.TRANSPARENT);list.setVerticalScrollBarEnabled(false);list.setOverScrollMode(OVER_SCROLL_NEVER);list.setAdapter(adapter);list.setOnItemClickListener((p,v,index,id)->{select(index);listener.command(OPEN);});list.setOnItemLongClickListener((p,v,index,id)->{select(index);listener.command(CONTEXT);return true;});body.addView(list,new FrameLayout.LayoutParams(-1,-1));
        bar=new WmScrollBar(context,new WmScrollBar.Host(){public int extent(){return list.getHeight();}public int range(){return rows.length*34;}public int offset(){View first=list.getChildAt(0);return list.getFirstVisiblePosition()*34-(first==null?0:first.getTop());}public int lineStep(){return 34;}public void scrollTo(int y){int offset=Math.max(0,y);list.setSelectionFromTop(offset/34,-offset%34);}public void scrollBy(int delta){scrollTo(offset()+delta);}public void stopFling(){long now=android.os.SystemClock.uptimeMillis();MotionEvent cancel=MotionEvent.obtain(now,now,MotionEvent.ACTION_CANCEL,0,0,0);try{list.onTouchEvent(cancel);}finally{cancel.recycle();}}},face);body.addView(bar,new FrameLayout.LayoutParams(26,-1,Gravity.RIGHT));bar.setVisibility(GONE);list.setOnScrollListener(new AbsListView.OnScrollListener(){public void onScrollStateChanged(AbsListView v,int state){}public void onScroll(AbsListView v,int first,int count,int total){bar.refresh();}});
        onWmRestyle(font,font,selection,face);
    }
    private String text(String value){return WmStrings.text(getContext(),language,value);}
    private String caption(Row row){return row.authored?text(row.title):row.title;}
    public Row selected(){return selected<0?null:rows[selected];}
    public void select(int index){if(index<0||index>=rows.length)throw new IllegalArgumentException("Library selection outside rows");selected=index;adapter.notifyDataSetChanged();}
    public void setPage(String title,int icon,Row[] values){setPage(title,icon,values,true);}
    public void setPage(String title,int icon,Row[] values,boolean authored){titleAuthored=authored;if(title==null||icon<0||icon>8||values==null)throw new IllegalArgumentException("Library page required");for(Row row:values)if(row==null)throw new IllegalArgumentException("Null library row");this.title=title;titleIcon=icon;rows=values.clone();selected=rows.length==0?-1:0;list.setSelection(0);adapter.notifyDataSetChanged();header.invalidate();requestLayout();}
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
            String[] names={"library","music","video","tv","now-playing-header","library-now-playing","artist","album","genre","library-back"};Bitmap[] loaded=new Bitmap[10];
            for(int n=0;n<names.length;n++)try{loaded[n]=WmResources.bitmap(getContext(),"media/"+names[n]+".png",16,16);}catch(IOException absent){}
            final boolean consistent=loadedRevision.equals(WmResources.revision(getContext()));
            ui.post(()->{
                if(closed||generation!=iconGeneration||!consistent||!loadedRevision.equals(WmResources.revision(getContext()))){
                    recycleIcons(loaded);
                    if(!closed&&generation==iconGeneration){pendingIconRevision=null;refreshIcons(false);}
                    return;
                }
                pendingIconRevision=null;iconRevision=loadedRevision;recycleIcons(icons);icons=loaded;header.invalidate();list.invalidateViews();
            });
        });}catch(RuntimeException rejected){if(generation==iconGeneration)pendingIconRevision=null;throw rejected;}
    }
    private static void recycleIcons(Bitmap[] values){for(Bitmap icon:values)if(icon!=null)icon.recycle();}
    public void close(){if(closed)return;closed=true;iconGeneration++;pendingIconRevision=null;iconWorker=null;recycleIcons(icons);icons=new Bitmap[10];}
    public void onWmRestyle(Typeface regular,Typeface bold,int color,int tab){font=regular;selection=color;face=tab;setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.WHITE,face}));header.setBackgroundColor(face);adapter.notifyDataSetChanged();header.invalidate();bar.onWmRestyle(regular,bold,color,tab);refreshIcons(false);}
    public void onWmLanguageChanged(String value){language=WmStrings.language(getContext(),value);adapter.notifyDataSetChanged();header.invalidate();header.setContentDescription((titleAuthored?text(title):title)+", "+text("뒤로"));}
    private class Cell extends View {int index;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);Cell(Context context){super(context);}protected void onDraw(Canvas canvas){if(index>=rows.length)return;Row row=rows[index];paint.setTypeface(font);paint.setTextSize(24);paint.setTextAlign(Paint.Align.LEFT);String value=caption(row);Paint.FontMetrics metrics=paint.getFontMetrics();float baseline=17-(metrics.ascent+metrics.descent)/2;float width=WmFonts.measure(paint,value);if(index==selected){paint.setColor(WmPalette.highlight(selection));canvas.drawRect(34,1,Math.min(getWidth(),34+width+2),33,paint);}paint.setColor(index==selected?Color.WHITE:Color.BLACK);if(icons[row.icon]!=null)canvas.drawBitmap(icons[row.icon],null,new Rect(2,1,34,33),paint);else{paint.setColor(0xff808080);canvas.drawRect(6,5,30,29,paint);paint.setColor(index==selected?Color.WHITE:Color.BLACK);}canvas.save();canvas.clipRect(34,0,getWidth(),34);WmFonts.draw(canvas,value,34,baseline,paint);canvas.restore();}}
    private class Header extends View {final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);Header(Context context){super(context);setClickable(true);setFocusable(true);setOnClickListener(v->listener.command(SOURCE));setOnTouchListener((v,event)->{if(event.getAction()==MotionEvent.ACTION_UP){if(event.getX()>=getWidth()-92)listener.command(BACK);else performClick();}return true;});}public boolean performClick(){super.performClick();return true;}protected void onDraw(Canvas canvas){paint.setTypeface(font);paint.setTextSize(24);paint.setTextAlign(Paint.Align.LEFT);paint.setColor(Color.BLACK);if(icons[titleIcon]!=null)canvas.drawBitmap(icons[titleIcon],null,new Rect(6,7,38,39),paint);String value=titleAuthored?text(title):title;float edge=Math.max(40,getWidth()-96);canvas.save();canvas.clipRect(40,0,edge,46);Paint.FontMetrics metrics=paint.getFontMetrics();WmFonts.draw(canvas,value,40,23-(metrics.ascent+metrics.descent)/2,paint);float x=Math.min(edge-18,40+WmFonts.measure(paint,value)+12);Path arrow=new Path();arrow.moveTo(x,21);arrow.lineTo(x+18,21);arrow.lineTo(x+9,30);arrow.close();canvas.drawPath(arrow,paint);canvas.restore();String back=text("뒤로");float width=WmFonts.measure(paint,back),right=getWidth()-6;if(icons[9]!=null)canvas.drawBitmap(icons[9],null,new Rect(Math.round(right-width)-32,7,Math.round(right-width),39),paint);WmFonts.draw(canvas,back,right-width,23-(metrics.ascent+metrics.descent)/2,paint);}}
}
