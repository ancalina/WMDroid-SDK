package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.text.Collator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

/** Notes list/text client in 480-wide SDK coordinates. Owner supplies data and persistence.
 * Plain text only; owner supplies folder identities. RichInk drawing/recording is not implemented. */
public final class WmNotesView extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener,WmControls.BackHandler {
    public static final int SORT_NAME=0,SORT_DATE=1,SORT_SIZE=2,SORT_TYPE=3;
    public static final class Entry {
        public final String key,title,type,folder;public final long modified,size;
        public Entry(String key,String title,long modified){this(key,title,modified,0,"");}
        /** Display title excludes the extension; type is the owner-supplied extension/category.
         * Size is owner-supplied bytes, not Android database allocation or PWI file size. */
        public Entry(String key,String title,long modified,long size,String type){this(key,title,modified,size,type,"");}
        public Entry(String key,String title,long modified,long size,String type,String folder){if(key==null||key.isEmpty()||title==null||type==null||folder==null||size<0)throw new IllegalArgumentException("Note key/title/type/folder and nonnegative size required");this.key=key;this.title=title;this.modified=modified;this.size=size;this.type=type;this.folder=folder;}
    }
    public static final class Folder {
        public final String id,name;
        public Folder(String id,String name){if(id==null||id.isEmpty()||name==null||name.isEmpty())throw new IllegalArgumentException("User folder ID/name required");this.id=id;this.name=name;}
    }
    public static final int ALL_FOLDERS=13729,MANAGE_FOLDERS=13730;
    public interface FolderListener {void manage();}
    private FolderListener folderListener;
    private Folder[] folders=new Folder[0];
    private String folderId;
    private FrameLayout folderPopup;
    public interface Listener {void open(Entry entry);}
    public static final int RENAME=25513,DELETE=25512;
    public interface ActionListener {void command(Entry entry,int command);}
    private ActionListener actions;
    private Entry selected;
    private FrameLayout actionPopup;
    private final LinearLayout overview,header;
    private final TextView folder,sort,message;
    private final ListView list;
    private final EditText editor;
    private final WmScrollView editorScroll;
    private final WmScrollBar bar;
    private final BaseAdapter adapter=new BaseAdapter(){
        public int getCount(){return entries.length;}public Object getItem(int p){return entries[p];}public long getItemId(int p){return p;}
        public View getView(int p,View old,ViewGroup parent){Row row=old instanceof Row?(Row)old:new Row(getContext());row.bind(entries[p],dates[p],sizes[p]);return row;}
    };
    private Entry[] allEntries=new Entry[0],entries=new Entry[0];
    private String[] dates=new String[0],sizes=new String[0];
    private int rowHeight=34,dateWidth=128,sizeWidth=90;
    private float lineTop,lineBottom;
    private final Paint measure=new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap noteIcon,allFoldersIcon,folderIcon,otherFolderIcon,manageFoldersIcon;
    private Typeface regular,bold;
    private int selection,zoom=100;
    private boolean editing,ascending=true;
    private int sortOrder=SORT_NAME;
    private FrameLayout sortPopup;
    private String language="ko";
    private String text(String authored){return WmStrings.text(getContext(),language,authored);}
    private Locale locale(){return WmStrings.locale("en".equals(language)?"en-US":"ko".equals(language)?"ko-KR":language);}
    private boolean english(){return "en".equals(language)||language.startsWith("en-");}
    public WmNotesView(Context context,Typeface regular,Typeface bold,int selection,Listener listener){
        super(context);if(regular==null||bold==null||listener==null)throw new IllegalArgumentException("Fonts/listener required");this.regular=regular;this.bold=bold;this.selection=selection;setBackgroundColor(Color.WHITE);
        refreshArtwork();
        overview=new LinearLayout(context);overview.setOrientation(LinearLayout.VERTICAL);addView(overview,new LayoutParams(-1,-1));
        header=new LinearLayout(context){private final Paint line=new Paint();
            @Override protected void onMeasure(int width,int height){int available=MeasureSpec.getSize(width),right=Math.min(available,(int)Math.ceil(WmFonts.measure(sort.getPaint(),sort.getText().toString()))+44);sort.setMaxWidth(right);folder.setMaxWidth(Math.max(0,available-right));super.onMeasure(width,height);}
            @Override protected void dispatchDraw(Canvas canvas){super.dispatchDraw(canvas);line.setColor(Color.BLACK);canvas.drawRect(0,getHeight()-2,getWidth(),getHeight(),line);}};
        folder=new TextView(context){private final android.graphics.Rect bounds=new android.graphics.Rect();@Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);Bitmap icon=folderId==null?allFoldersIcon:folderIcon;bounds.set(2,(getHeight()-32)/2,34,(getHeight()-32)/2+32);if(folderPopup==null&&icon!=null)canvas.drawBitmap(icon,null,bounds,null);
            Paint paint=getPaint();int color=paint.getColor();paint.setColor(getCurrentTextColor());int left=Math.min(getWidth()-28,getPaddingLeft()+(int)Math.ceil(WmFonts.measure(paint,getText().toString()))+2);if(left>=getPaddingLeft())WmComboGlyph.draw(canvas,paint,left,10,left+26,36,folderPopup!=null);paint.setColor(color);
        }};WmControls.textStyle(folder,bold);folder.setId(530);folder.setSingleLine(true);folder.setEllipsize(android.text.TextUtils.TruncateAt.END);folder.setPadding(36,0,30,0);folder.setFocusable(true);folder.setOnClickListener(v->openFolders());header.addView(folder,new LinearLayout.LayoutParams(-2,-1));header.addView(new View(context),new LinearLayout.LayoutParams(0,-1,1));
        sort=new TextView(context){@Override protected void onDraw(android.graphics.Canvas canvas){super.onDraw(canvas);android.graphics.Paint paint=getPaint();int color=paint.getColor();paint.setColor(getCurrentTextColor());WmComboGlyph.draw(canvas,paint,getWidth()-30,11,getWidth()-4,37,false);paint.setColor(color);}};WmControls.textStyle(sort,bold);sort.setSingleLine(true);sort.setEllipsize(android.text.TextUtils.TruncateAt.END);sort.setPadding(8,0,36,0);sort.setFocusable(true);sort.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);sort.setOnClickListener(v->openSort());WmControls.entry(sort,selection);header.addView(sort,new LinearLayout.LayoutParams(-2,-1));overview.addView(header,new LinearLayout.LayoutParams(-1,46));
        FrameLayout body=new FrameLayout(context){@Override protected void onMeasure(int width,int height){
            // Reserve the scrollbar before ListView measures rows; onScroll is too late for its first frame.
            if(list!=null&&bar!=null){boolean scrolling=MeasureSpec.getSize(height)>0&&(long)entries.length*rowHeight>MeasureSpec.getSize(height);((LayoutParams)list.getLayoutParams()).rightMargin=scrolling?26:0;bar.setVisibility(scrolling?VISIBLE:GONE);}
            super.onMeasure(width,height);
        }};overview.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        list=new ListView(context);list.setDivider(null);list.setDividerHeight(0);list.setSelector(new ColorDrawable(Color.TRANSPARENT));list.setCacheColorHint(Color.TRANSPARENT);list.setBackgroundColor(Color.TRANSPARENT);list.setVerticalScrollBarEnabled(false);list.setOverScrollMode(OVER_SCROLL_NEVER);list.setAdapter(adapter);list.setOnItemClickListener((p,v,index,id)->{select(entries[index]);listener.open(selected);});list.setOnItemLongClickListener((p,v,index,id)->{if(actions==null)return false;select(entries[index]);openActions(v);return true;});body.addView(list,new LayoutParams(-1,-1));
        bar=new WmScrollBar(context,new WmScrollBar.Host(){
            public int extent(){return list.getHeight();}public int range(){return entries.length*rowHeight;}public int offset(){View first=list.getChildAt(0);return list.getFirstVisiblePosition()*rowHeight-(first==null?0:first.getTop());}
            public int lineStep(){return rowHeight;}public void scrollTo(int y){int n=Math.max(0,y);list.setSelectionFromTop(n/rowHeight,-n%rowHeight);}public void scrollBy(int d){scrollTo(offset()+d);}public void stopFling(){long now=android.os.SystemClock.uptimeMillis();android.view.MotionEvent cancel=android.view.MotionEvent.obtain(now,now,android.view.MotionEvent.ACTION_CANCEL,0,0,0);try{list.onTouchEvent(cancel);}finally{cancel.recycle();}}
        },WmPalette.face(selection));body.addView(bar,new LayoutParams(26,-1,Gravity.RIGHT));bar.setVisibility(GONE);
        list.setOnScrollListener(new AbsListView.OnScrollListener(){public void onScrollStateChanged(AbsListView v,int s){}public void onScroll(AbsListView v,int f,int n,int total){bar.refresh();LayoutParams p=(LayoutParams)list.getLayoutParams();int inset=bar.getVisibility()==VISIBLE?26:0;if(p.rightMargin!=inset){p.rightMargin=inset;list.setLayoutParams(p);}}});
        editor=new EditText(context);WmControls.input(editor,regular);editor.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);editor.setGravity(Gravity.TOP|Gravity.LEFT);editor.setHorizontallyScrolling(false);editor.setVerticalScrollBarEnabled(false);editor.setMinLines(1);
        editorScroll=new WmScrollView(context,editor);addView(editorScroll,new LayoutParams(-1,-1));editorScroll.setVisibility(GONE);
        message=WmControls.text(context,regular,"");message.setGravity(Gravity.CENTER);message.setPadding(12,8,12,8);message.setBackgroundColor(Color.WHITE);addView(message,new LayoutParams(-1,-1));message.setVisibility(GONE);
        onWmRestyle(regular,bold,selection,WmPalette.face(selection));captions();
    }
    private String artworkRevision;
    private void refreshArtwork(){
        if(artworkRevision==null)try{WmResources.prepare(getContext());}catch(java.io.IOException absent){}
        String revision=WmResources.revision(getContext());if(revision.equals(artworkRevision))return;
        Context context=getContext();noteIcon=icon(context,"apps/notes.png");allFoldersIcon=icon(context,"files/all-folders.png");folderIcon=icon(context,"files/folder.png");
        otherFolderIcon=icon(context,"files/other-folder.png");manageFoldersIcon=icon(context,"files/manage-folders.png");
        artworkRevision=revision;
    }
    private static Bitmap icon(Context context,String name){return WmResources.iconBitmap(context,name,32,32,"apps/notes.png".equals(name)?"📝":"files/manage-folders.png".equals(name)?"🗂️":"📁");}
    public void setEntries(Entry[] values){if(values==null)throw new IllegalArgumentException("Entries required");Entry[] copy=values.clone();HashSet<String> keys=new HashSet<>();for(Entry e:copy)if(e==null||!keys.add(e.key))throw new IllegalArgumentException("Duplicate/null note");closeActions();allEntries=copy;filterEntries();}
    public void setOnFolderListener(FolderListener listener){closeFolders();folderListener=listener;}
    public void setFolders(Folder[] values){
        if(values==null)throw new IllegalArgumentException("Folders required");Folder[] copy=values.clone();HashSet<String> keys=new HashSet<>();for(Folder value:copy)if(value==null||!keys.add(value.id))throw new IllegalArgumentException("Duplicate/null folder");
        closeFolders();closeActions();folders=copy;if(folderId!=null&&!folderId.isEmpty()&&!keys.contains(folderId))folderId=null;sortFolders();filterEntries();captions();
    }
    /** null selects all folders; empty selects the root, which has no toolbar menu item. */
    public String folderId(){return folderId;}
    public void setFolder(String id){
        if(id!=null&&!id.isEmpty()){boolean known=false;for(Folder folder:folders)if(folder.id.equals(id)){known=true;break;}if(!known)throw new IllegalArgumentException("Unknown note folder");}
        closeFolders();closeActions();closeSort();folderId=id;selected=null;filterEntries();captions();list.setSelection(0);
    }
    private void filterEntries(){ArrayList<Entry> visible=new ArrayList<>();Entry retained=null;for(Entry value:allEntries)if(folderId==null||folderId.equals(value.folder)){visible.add(value);if(selected!=null&&selected.key.equals(value.key))retained=value;}selected=retained;entries=visible.toArray(new Entry[visible.size()]);sortEntries();}
    private void sortFolders(){Collator collator=Collator.getInstance(locale());collator.setStrength(Collator.SECONDARY);Arrays.sort(folders,(a,b)->{int result=collator.compare(a.name,b.name);return result==0?a.id.compareTo(b.id):result;});}
    public Entry selected(){return selected;}
    /** Optional mutable-document commands. Read-only clients leave this unset. */
    public void setOnActionListener(ActionListener listener){closeActions();actions=listener;}
    private void select(Entry entry){selected=entry;list.invalidateViews();}
    private void openActions(View row){
        closeFolders();closeSort();closeActions();if(actions==null||selected==null||getWidth()==0)return;
        final Entry target=selected;final ActionListener callback=actions;FrameLayout overlay=new FrameLayout(getContext());actionPopup=overlay;
        WmRadioMenu menu=new WmRadioMenu(getContext(),regular,new WmMenuItem[]{WmMenuItem.command(RENAME,text("이름 바꾸기/이동...")),WmMenuItem.command(DELETE,text("삭제"))},"",selection,index->{if(actionPopup!=overlay)return;closeActions();callback.command(target,index==0?RENAME:DELETE);});
        android.graphics.Rect anchor=new android.graphics.Rect(0,0,row.getWidth(),row.getHeight());offsetDescendantRectToMyCoords(row,anchor);
        int width=Math.min(getWidth(),menu.menuWidth()),height=Math.min(getHeight(),menu.menuHeight());LayoutParams fit=new LayoutParams(width,height);fit.leftMargin=Math.max(0,Math.min(36,getWidth()-width));fit.topMargin=Math.max(0,Math.min(anchor.bottom,getHeight()-height));
        overlay.setOnClickListener(v->closeActions());overlay.addView(new WmScrollView(getContext(),menu),fit);addView(overlay,new LayoutParams(-1,-1));
    }
    private boolean closeActions(){if(actionPopup==null)return false;removeView(actionPopup);actionPopup=null;return true;}
    private void openFolders(){
        closeActions();closeSort();if(editing||getWidth()==0||getHeight()<=header.getHeight())return;if(closeFolders())return;
        final FolderListener manager=folderListener;final String[] targets=new String[folders.length+1];final WmMenuItem[] items=new WmMenuItem[targets.length+(manager==null?0:2)];
        boolean en=english();items[0]=WmMenuItem.checked(ALL_FOLDERS,text("모든 폴더"),folderId==null);int checked=folderId==null?0:-1;
        for(int i=0;i<folders.length;i++){targets[i+1]=folders[i].id;boolean active=folders[i].id.equals(folderId);items[i+1]=WmMenuItem.checked(i+1,folders[i].name,active);if(active)checked=i+1;}
        if(manager!=null){items[targets.length]=WmMenuItem.separator();items[targets.length+1]=WmMenuItem.command(MANAGE_FOLDERS,text("추가/삭제..."));}
        FrameLayout overlay=new FrameLayout(getContext());folderPopup=overlay;folder.setSelected(false);folder.setPressed(false);folder.clearFocus();captions();
        WmRadioMenu menu=new WmRadioMenu(getContext(),listFont(),items,text(" 선택됨"),selection,index->{
            if(folderPopup!=overlay)return;if(index<targets.length){setFolder(targets[index]);folder.requestFocus();}else if(index==items.length-1&&manager!=null){closeFolders();manager.manage();}
        });
        Bitmap[] icons=new Bitmap[items.length];icons[0]=allFoldersIcon;for(int i=1;i<targets.length;i++)icons[i]=i==checked?folderIcon:otherFolderIcon;if(manager!=null)icons[items.length-1]=manageFoldersIcon;
        // Older clients may omit optional ROM assets; their text menu stays usable.
        if(allFoldersIcon!=null&&folderIcon!=null&&otherFolderIcon!=null&&(manager==null||manageFoldersIcon!=null))menu.setIconRows(icons,regular,bold,en,listTextSize(),selection);
        // This is a command menu, not an ordinary combo: navigation skips the
        // separator and Enter explicitly activates Add/Delete instead of moving.
        final int[] focused={checked<0?0:checked};
        View.OnKeyListener keys=(view,key,event)->{
            if(folderPopup!=overlay||!event.hasNoModifiers())return false;
            boolean enter=WmControls.isEnterKey(key),escape=key==android.view.KeyEvent.KEYCODE_ESCAPE;
            boolean up=key==android.view.KeyEvent.KEYCODE_DPAD_UP,down=key==android.view.KeyEvent.KEYCODE_DPAD_DOWN,home=key==android.view.KeyEvent.KEYCODE_MOVE_HOME,end=key==android.view.KeyEvent.KEYCODE_MOVE_END;
            if(!enter&&!escape&&!up&&!down&&!home&&!end)return false;if(event.getAction()==android.view.KeyEvent.ACTION_UP)return true;if(event.getAction()!=android.view.KeyEvent.ACTION_DOWN)return false;
            if(escape){closeFolders();folder.requestFocus();return true;}if(enter){if(event.getRepeatCount()==0)menu.row(focused[0]).performClick();return true;}
            int next=home?0:end?items.length-1:Math.max(0,Math.min(items.length-1,focused[0]+(down?1:-1)));
            while(items[next].separator)next+=down?1:-1;menu.row(focused[0]).setSelected(false);focused[0]=next;menu.row(next).setSelected(true);menu.row(next).requestRectangleOnScreen(new android.graphics.Rect(0,0,menu.row(next).getWidth(),menu.row(next).getHeight()),true);return true;
        };
        menu.setFocusable(true);menu.setFocusableInTouchMode(true);menu.setOnKeyListener(keys);menu.row(focused[0]).setSelected(true);
        overlay.setOnClickListener(v->closeFolders());LayoutParams fit=new LayoutParams(Math.min(getWidth(),menu.menuWidth()),Math.min(getHeight()-header.getHeight(),menu.menuHeight()));fit.topMargin=header.getHeight();
        overlay.addView(new WmScrollView(getContext(),menu),fit);addView(overlay,new LayoutParams(-1,-1));menu.requestFocus();
        menu.post(()->{if(folderPopup==overlay){View selected=menu.row(focused[0]);selected.requestRectangleOnScreen(new android.graphics.Rect(0,0,selected.getWidth(),selected.getHeight()),true);}});
    }
    private boolean closeFolders(){if(folderPopup==null)return false;removeView(folderPopup);folderPopup=null;folder.setSelected(false);folder.invalidate();captions();return true;}
    private void sortEntries(){Collator col=Collator.getInstance(locale());col.setStrength(Collator.SECONDARY);Arrays.sort(entries,(a,b)->{int n=sortOrder==SORT_DATE?compare(a.modified,b.modified):sortOrder==SORT_SIZE?compare(a.size,b.size):sortOrder==SORT_TYPE?(a.type.isEmpty()!=b.type.isEmpty()?(a.type.isEmpty()?1:-1):col.compare(a.type,b.type)):col.compare(a.title,b.title);if(n==0&&sortOrder==SORT_TYPE)n=col.compare(a.title,b.title);if(n==0)n=compare(a.modified,b.modified);if(n==0)n=a.key.compareTo(b.key);return ascending?n:-Integer.signum(n);});refreshMetadata();}
    private Typeface listFont(){return english()?bold:regular;}
    private float listTextSize(){return english()?8*192f/72:24;}
    private void styleListFont(){for(TextView label:new TextView[]{folder,sort}){label.setTypeface(listFont());WmFonts.attach(label,listFont());label.setTextSize(TypedValue.COMPLEX_UNIT_PX,listTextSize());}}
    /** Refresh date/time labels after owner time-zone, day or clock-format changes. No storage IO. */
    public void refreshMetadata(){
        int first=list.getFirstVisiblePosition();View top=list.getChildAt(0);int offset=top==null?0:top.getTop();
        Locale locale=locale();TimeZone zone=TimeZone.getDefault();
        Calendar day=Calendar.getInstance(zone,locale);day.set(Calendar.HOUR_OF_DAY,0);day.set(Calendar.MINUTE,0);day.set(Calendar.SECOND,0);day.set(Calendar.MILLISECOND,0);long start=day.getTimeInMillis();day.add(Calendar.DAY_OF_MONTH,1);long end=day.getTimeInMillis();
        DateFormat shortDate=DateFormat.getDateInstance(DateFormat.SHORT,locale),shortTime=android.text.format.DateFormat.is24HourFormat(getContext())?new SimpleDateFormat("HH:mm",locale):DateFormat.getTimeInstance(DateFormat.SHORT,locale);shortDate.setTimeZone(zone);shortTime.setTimeZone(zone);
        dates=new String[entries.length];sizes=new String[entries.length];measure.setTypeface(listFont());measure.setTextSize(listTextSize());dateWidth=128;sizeWidth=90;Paint.FontMetrics metrics=measure.getFontMetrics();lineTop=metrics.ascent;lineBottom=metrics.descent;android.graphics.Rect bounds=new android.graphics.Rect();
        for(int i=0;i<entries.length;i++){long time=entries[i].modified;dates[i]=(time>=start&&time<end?shortTime:shortDate).format(new Date(time)).replace('\u202f',' ').replace('\u00a0',' ').replace(" AM"," a").replace(" PM"," p");sizes[i]=formatSize(entries[i].size);dateWidth=Math.max(dateWidth,(int)Math.ceil(WmFonts.measure(measure,dates[i]))+30);sizeWidth=Math.max(sizeWidth,(int)Math.ceil(WmFonts.measure(measure,sizes[i]))+30);WmFonts.textBounds(measure,entries[i].title+dates[i]+sizes[i],bounds);lineTop=Math.min(lineTop,bounds.top);lineBottom=Math.max(lineBottom,bounds.bottom);}
        rowHeight=Math.max(34,(int)Math.ceil(lineBottom-lineTop)+2);adapter.notifyDataSetChanged();list.setSelectionFromTop(first,offset);bar.refresh();
    }
    /** ROM DocList integer b/k/m units. Long arithmetic extends the native DWORD size range. */
    public static String formatSize(long bytes){if(bytes<0)throw new IllegalArgumentException("Nonnegative note size required");if(bytes<1024)return bytes+"b";long unit=bytes<1048576?1024:1048576;return (bytes/unit+(bytes%unit>=unit/2?1:0))+(unit==1024?"k":"m");}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility==VISIBLE&&list!=null&&bar!=null)refreshMetadata();}
    private final class Row extends View {
        private final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);
        private Entry entry;private String date="",size="",caption="";private boolean dirty=true;
        private int dateStart,sizeStart;private float baseline,sizeX;
        private final android.graphics.Rect iconBounds=new android.graphics.Rect();
        Row(Context context){super(context);setSoundEffectsEnabled(false);}
        void bind(Entry value,String date,String size){entry=value;this.date=date;this.size=size;dirty=true;setContentDescription(value.title+", "+date+", "+size);setLayoutParams(new AbsListView.LayoutParams(-1,rowHeight));invalidate();}
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){dirty=true;}
        @Override protected void drawableStateChanged(){super.drawableStateChanged();invalidate();}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);if(entry==null)return;ink.setTypeface(listFont());ink.setTextSize(listTextSize());
            if(dirty){
                // Preserve a usable title on narrow/custom-font clients; native defaults are 262/128/90 at VGA.
                int available=Math.max(0,getWidth()-84),dw=dateWidth,sw=sizeWidth,total=dw+sw;if(total>available){dw=(int)((long)dw*available/total);sw=available-dw;}
                sizeStart=getWidth()-sw;dateStart=sizeStart-dw;caption=WmFonts.ellipsize(entry.title,ink,Math.max(0,dateStart-42));baseline=(getHeight()-lineTop-lineBottom)/2;sizeX=getWidth()-6-WmFonts.measure(ink,size);iconBounds.set(2,(getHeight()-32)/2,34,(getHeight()-32)/2+32);dirty=false;
            }
            boolean pressed=isPressed()||isSelected()||selected!=null&&selected.key.equals(entry.key);if(pressed)canvas.drawColor(WmPalette.highlight(selection));ink.setColor(pressed?Color.WHITE:Color.BLACK);
            if(noteIcon!=null)canvas.drawBitmap(noteIcon,null,iconBounds,null);
            int save=canvas.save();canvas.clipRect(36,0,Math.max(36,dateStart-6),getHeight());WmFonts.draw(canvas,caption,36,baseline,ink);canvas.restoreToCount(save);
            save=canvas.save();canvas.clipRect(dateStart,0,sizeStart,getHeight());WmFonts.draw(canvas,date,dateStart+6,baseline,ink);canvas.restoreToCount(save);
            save=canvas.save();canvas.clipRect(sizeStart,0,getWidth(),getHeight());WmFonts.draw(canvas,size,sizeX,baseline,ink);canvas.restoreToCount(save);
        }
    }
    private static int compare(long a,long b){return a<b?-1:a==b?0:1;}
    public int sortOrder(){return sortOrder;}
    public boolean sortAscending(){return ascending;}
    /** Explicit state setter; choosing the current criterion in the native menu reverses it. */
    public void setSortOrder(int order,boolean ascending){if(order<SORT_NAME||order>SORT_TYPE)throw new IllegalArgumentException("Notes sort criterion required");closeFolders();closeSort();this.sortOrder=order;this.ascending=ascending;sortEntries();captions();}
    private String[] sortLabels(){return new String[]{text("이름"),text("날짜"),text("크기"),text("종류")};}
    private void openSort(){
        closeFolders();closeActions();
        if(editing||getWidth()==0||getHeight()<=header.getHeight())return;if(closeSort())return;
        FrameLayout overlay=new FrameLayout(getContext());sortPopup=overlay;captions();
        WmRadioMenu menu=new WmRadioMenu(getContext(),regular,sortLabels(),sortOrder,"",selection,index->{if(sortPopup==overlay)setSortOrder(index,index==sortOrder?!ascending:ascending);});
        overlay.setOnClickListener(v->closeSort());WmScrollView content=new WmScrollView(getContext(),menu);
        LayoutParams fit=new LayoutParams(Math.min(getWidth(),menu.menuWidth()),Math.min(getHeight()-header.getHeight(),menu.menuHeight()),Gravity.RIGHT|Gravity.TOP);fit.topMargin=header.getHeight();overlay.addView(content,fit);addView(overlay,new LayoutParams(-1,-1));
    }
    private boolean closeSort(){if(sortPopup==null)return false;removeView(sortPopup);sortPopup=null;captions();return true;}
    @Override public boolean back(){return closeFolders()||closeActions()||closeSort();}
    @Override protected void onDetachedFromWindow(){closeFolders();closeActions();closeSort();super.onDetachedFromWindow();}
    public void edit(String text){if(text==null)throw new IllegalArgumentException("Text required");closeFolders();closeActions();closeSort();editor.setText(text);editor.setSelection(0);editing=true;overview.setVisibility(GONE);editorScroll.setVisibility(VISIBLE);setMessage(null);editorScroll.scrollToPosition(0);}
    public void showList(){android.view.inputmethod.InputMethodManager input=(android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);if(input!=null)input.hideSoftInputFromWindow(editor.getWindowToken(),0);editing=false;editor.clearFocus();editorScroll.setVisibility(GONE);overview.setVisibility(VISIBLE);setMessage(null);}
    public boolean isEditing(){return editing;}
    public EditText editor(){return editor;}
    public String text(){return editor.getText().toString();}
    public int zoomPercent(){return zoom;}
    public void setZoomPercent(int value){if(value!=75&&value!=100&&value!=150&&value!=200&&value!=300)throw new IllegalArgumentException("Native Notes menu zoom required");zoom=value;styleEditor();}
    public void setMessage(String text){message.setText(text==null?"":text);message.setVisibility(text==null?GONE:VISIBLE);}
    private void styleEditor(){editor.setTypeface(regular);WmFonts.attach(editor,regular);editor.setTextSize(TypedValue.COMPLEX_UNIT_PX,28*zoom/100f);editor.setBackgroundColor(Color.TRANSPARENT);editor.setPadding(8,0,8,0);editor.setMinHeight(getHeight());editor.setMinimumHeight(getHeight());editor.setHighlightColor(selection);}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);editor.setMinimumHeight(h);editor.setMinHeight(h);}
    public Bundle saveInteractionState(){Bundle state=new Bundle();state.putString("selected",selected==null?null:selected.key);state.putString("folder",folderId);state.putInt("zoom",zoom);state.putInt("sort",sortOrder);state.putBoolean("ascending",ascending);state.putInt("start",Math.max(0,editor.getSelectionStart()));state.putInt("end",Math.max(0,editor.getSelectionEnd()));state.putInt("scroll",editorScroll.getRequestedScrollPosition());state.putBoolean("focus",editor.hasFocus());return state;}
    /** Supply the owner entries and folder catalog before restoring. Missing or filtered-out selection keys are ignored. */
    /** Saved focus=true requests editor focus; false leaves current focus intact.
     * Owner prepares mode/drafts/focus before restore; application is not transactional. */
    public void restoreInteractionState(Bundle state){if(state==null)return;setFolder(state.getString("folder"));setSortOrder(state.getInt("sort",SORT_NAME),state.getBoolean("ascending",true));String key=state.getString("selected");for(Entry entry:entries)if(entry.key.equals(key)){select(entry);break;}setZoomPercent(state.getInt("zoom",100));int n=editor.length();editor.setSelection(Math.max(0,Math.min(n,state.getInt("start"))),Math.max(0,Math.min(n,state.getInt("end"))));editorScroll.scrollToPosition(Math.max(0,state.getInt("scroll")));if(state.getBoolean("focus"))editor.requestFocus();}
    private void captions(){String caption=folderId==null?text("모든 폴더"):text("없음");if(folderId!=null)for(Folder value:folders)if(value.id.equals(folderId)){caption=value.name;break;}folder.setPadding(folderPopup!=null?4:36,0,30,0);folder.setText(folderPopup!=null?text("표시"):caption);sort.setText(sortPopup!=null?text("정렬 기준"):sortLabels()[sortOrder]);editor.setContentDescription(text("메모 내용"));}
    @Override public void onWmLanguageChanged(String language){closeFolders();closeActions();closeSort();this.language=WmStrings.language(getContext(),language);styleListFont();sortFolders();captions();sortEntries();}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){refreshArtwork();closeFolders();closeActions();closeSort();this.regular=regular;this.bold=bold;this.selection=selection;styleListFont();header.setBackgroundColor(tabColor);for(TextView label:new TextView[]{folder,sort}){WmControls.entry(label,selection);android.graphics.drawable.StateListDrawable bg=new android.graphics.drawable.StateListDrawable();for(int state:new int[]{android.R.attr.state_pressed,android.R.attr.state_focused,android.R.attr.state_selected})bg.addState(new int[]{state},new ColorDrawable(selection));bg.addState(new int[]{},new ColorDrawable(Color.TRANSPARENT));label.setBackgroundDrawable(bg);}bar.onWmRestyle(regular,bold,selection,tabColor);overview.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.WHITE,tabColor}));styleEditor();refreshMetadata();}
}
