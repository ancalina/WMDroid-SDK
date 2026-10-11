package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.*;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native portrait player geometry from the 192-DPI WPC skin. The owner supplies
 * file selection, library and shell commands. Android decoders determine supported formats.
 * Vendor bitmaps are optional resource-pack assets, never embedded in the SDK. */
public final class WmMediaView extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener,WmControls.BackHandler {
    public enum State { EMPTY, PREPARING, READY, PLAYING, PAUSED, ENDED, ERROR, CLOSED }
    public interface Listener { void changed(WmMediaView view); }
    // API14 worker View.post before attachment uses the worker thread run queue.
    private final android.os.Handler ui=new android.os.Handler(android.os.Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AudioManager audio;
    private final TextureView video;
    private final SkinCanvas skin;
    private final SeekBar seek;
    private final WmMediaPlaylistView playlistView;
    private final WmMediaLibraryView libraryView;
    private WmMediaPlaylistView.Listener playlistListener;
    private final WmMediaPropertiesView propertiesView;
    private int propertyGeneration;
    private final View[] buttons=new View[7];
    private final int[][] rects={{202,440,76,86},{136,450,58,66},{286,450,58,66},{380,404,28,32},{424,440,50,58},{368,454,50,58},{8,440,50,58}};
    private Bitmap background,pushed,disabled,thumb;
    private String artworkRevision;
    private int artworkGeneration;
    private String language="ko";
    private MediaPlayer player;
    private Surface surface;
    private Listener listener;
    private final WmMediaQueue queue=new WmMediaQueue();
    private State state=State.EMPTY;
    private int volume=70,duration,position,videoWidth,videoHeight;
    private boolean muted,wantsPlay,repeat,fullScreen,dragging;
    private String name="",error="";
    private float scale=1,left,top,extra;
    private Typeface titleFont;
    private final AudioManager.OnAudioFocusChangeListener focus=change->{if(change<0)pause();};
    private final Runnable tick=new Runnable(){public void run(){if(state==State.CLOSED)return;updatePosition();if(state==State.PLAYING)postDelayed(this,200);}};
    public WmMediaView(Context context,Typeface font){this(context,font,Typeface.create(font,Typeface.BOLD));}
    public WmMediaView(Context context,Typeface font,Typeface bold){
        super(context);titleFont=bold;setBackgroundColor(0xff000000);audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        skin=new SkinCanvas(context,font);addView(skin,new LayoutParams(-1,-1));
        video=new TextureView(context);video.setOpaque(false);video.setAlpha(0);addView(video,new LayoutParams(460,310));
        video.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){
            public void onSurfaceTextureAvailable(SurfaceTexture texture,int w,int h){surface=new Surface(texture);if(player!=null)player.setSurface(surface);fitVideo();}
            public void onSurfaceTextureSizeChanged(SurfaceTexture texture,int w,int h){fitVideo();}
            public boolean onSurfaceTextureDestroyed(SurfaceTexture texture){if(player!=null)player.setSurface(null);if(surface!=null){surface.release();surface=null;}return true;}
            public void onSurfaceTextureUpdated(SurfaceTexture texture){}
        });
        String[] labels={"재생/일시 중지","이전","다음","음소거","볼륨 높이기","볼륨 낮추기","전체 화면"};
        for(int i=0;i<buttons.length;i++){final int command=i;TextView button=new TextView(context);button.setContentDescription(labels[i]);button.setFocusable(true);button.setClickable(true);button.setOnClickListener(v->{if(command==0)toggle();else if(command==1)previous();else if(command==2)next();else if(command==3)setMuted(!muted);else if(command==4)setVolume(volume+5);else if(command==5)setVolume(volume-5);else setFullScreen(!fullScreen);});button.setOnTouchListener((v,event)->{skin.invalidate();return false;});buttons[i]=button;addView(button,new LayoutParams(1,1));}
        seek=new SeekBar(context);seek.setPadding(0,0,0,0);android.graphics.drawable.GradientDrawable invisibleThumb=new android.graphics.drawable.GradientDrawable();invisibleThumb.setSize(0,0);invisibleThumb.setColor(Color.TRANSPARENT);seek.setThumb(invisibleThumb);seek.setProgressDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));seek.setMax(1000);seek.setContentDescription("재생 위치");addView(seek,new LayoutParams(256,20));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar bar){dragging=true;}public void onStopTrackingTouch(SeekBar bar){dragging=false;seekTo((int)((long)duration*bar.getProgress()/1000));}public void onProgressChanged(SeekBar bar,int progress,boolean user){if(user){position=(int)((long)duration*progress/1000);skin.invalidate();}}});
        playlistView=new WmMediaPlaylistView(context,font,id->{
            int index=playlistViewIndex();
            if(id==WmMediaPlaylistView.UP){moveSource(index,index-1);selectPlaylist(index-1);}
            else if(id==WmMediaPlaylistView.DOWN){moveSource(index,index+1);selectPlaylist(index+1);}
            else if(id==WmMediaPlaylistView.REMOVE)removeSource(index);
            else if(id==WmMediaPlaylistView.PLAY)playSelection();
            else if(playlistListener!=null)playlistListener.command(id);
        });playlistView.setVisibility(GONE);addView(playlistView,new LayoutParams(-1,-1));playlistView.loadIcons(worker);
        libraryView=new WmMediaLibraryView(context,font,id->{if(playlistListener!=null)playlistListener.command(id);});libraryView.setVisibility(GONE);addView(libraryView,new LayoutParams(-1,-1));libraryView.loadIcons(worker);
        propertiesView=new WmMediaPropertiesView(context,font,bold);propertiesView.setVisibility(GONE);addView(propertiesView,new LayoutParams(-1,-1));
        refresh();refreshArtwork();
    }
    @Override protected boolean drawChild(Canvas canvas,View child,long drawingTime){
        // API14 TextureView software drawing can dereference a missing hardware layer.
        return child==video&&!canvas.isHardwareAccelerated()||super.drawChild(canvas,child,drawingTime);
    }
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){skin.paint.setTypeface(regular);titleFont=bold;refreshArtwork();skin.invalidate();}
    private void refreshArtwork(){
        if(state==State.CLOSED)return;final String revision=WmResources.revision(getContext());if(revision.equals(artworkRevision))return;
        artworkRevision=revision;final int token=++artworkGeneration;
        worker.execute(()->{
            final String loadedRevision;
            synchronized(WmResources.class){try{WmResources.prepare(getContext());}catch(IOException absent){}loadedRevision=WmResources.revision(getContext());}
            Bitmap bg=asset("media/player-background.gif",480,536),press=asset("media/player-pushed.gif",724,86),off=asset("media/player-disabled.gif",292,86),knob=seekThumb();
            final boolean consistent=loadedRevision.equals(WmResources.revision(getContext()));
            ui.post(()->{if(state==State.CLOSED||token!=artworkGeneration||!consistent||!loadedRevision.equals(WmResources.revision(getContext()))){recycle(bg);recycle(press);recycle(off);recycle(knob);if(state!=State.CLOSED&&token==artworkGeneration){artworkRevision=null;refreshArtwork();}return;}
                artworkRevision=loadedRevision;recycle(background);recycle(pushed);recycle(disabled);recycle(thumb);background=bg;pushed=press;disabled=off;thumb=knob;skin.invalidate();
            });
        });
    }
    @Override public void onWmLanguageChanged(String value){language=WmStrings.language(getContext(),value);String[] names={"재생/일시 중지","이전","다음","음소거","볼륨 높이기","볼륨 낮추기","전체 화면"};for(int i=0;i<buttons.length;i++)buttons[i].setContentDescription(WmStrings.text(getContext(),language,names[i]));seek.setContentDescription(WmStrings.text(getContext(),language,"재생 위치"));skin.invalidate();}
    private Bitmap asset(String path,int w,int h){try{return WmResources.bitmap(getContext(),path,w,h);}catch(IOException absent){return null;}}
    private Bitmap seekThumb(){
        Bitmap source=asset("media/seek-thumb.gif",120,20);if(source==null)return null;
        // Native magenta is an additional color key, separate from the GIF transparent index.
        int[] pixels=new int[2400];source.getPixels(pixels,0,120,0,0,120,20);
        // API16 Skia can decode palette FF00FF as FE00FF. Use that exact palette key,
        // bounded to the magenta corner; do not erase other pink shades in user skins.
        int key=pixels[0]&0xffffff;boolean magenta=(key>>>16)>=248&&((key>>>8)&255)<=7&&(key&255)>=248;
        if(magenta)for(int i=0;i<pixels.length;i++)if((pixels[i]&0xffffff)==key)pixels[i]=0;
        Bitmap result=Bitmap.createBitmap(pixels,120,20,Bitmap.Config.ARGB_8888);result.setDensity(Bitmap.DENSITY_NONE);source.recycle();return result;
    }
    private static void recycle(Bitmap bitmap){if(bitmap!=null)bitmap.recycle();}
    public void setListener(Listener value){listener=value;}
    public void setPlaylistCommandListener(WmMediaPlaylistView.Listener value){playlistListener=value;}
    /** Legacy compatibility; owner menus now control Save independently of Properties. */
    @Deprecated public void setPlaylistSaveEnabled(boolean value){playlistView.setSaveEnabled(value);}
    private int playlistViewIndex(){return playlistView.selectedIndex();}
    public int selectedIndex(){return playlistViewIndex();}
    public void selectPlaylist(int index){playlistView.select(index);}
    public WmMediaLibraryView library(){return libraryView;}
    public boolean isLibraryVisible(){return libraryView.getVisibility()==VISIBLE;}
    public void showLibrary(boolean value){if(state==State.CLOSED)return;propertiesView.setVisibility(GONE);propertyGeneration++;libraryView.setVisibility(value?VISIBLE:GONE);if(value)playlistView.setVisibility(GONE);panelsChanged();}
    private boolean panelVisible(){return isPlaylistVisible()||isLibraryVisible()||isPropertiesVisible();}
    private void panelsChanged(){skin.setVisibility(panelVisible()?INVISIBLE:VISIBLE);video.setVisibility(panelVisible()?INVISIBLE:VISIBLE);requestLayout();refresh();}
    public boolean isPlaylistVisible(){return playlistView.getVisibility()==VISIBLE;}
    public void showPlaylist(boolean value){if(state==State.CLOSED)return;propertiesView.setVisibility(GONE);propertyGeneration++;playlistView.setVisibility(value?VISIBLE:GONE);libraryView.setVisibility(GONE);panelsChanged();}
    public void playSelection(){int index=selectedIndex();if(index<0)return;playIndex(index);showPlaylist(false);}
    @Override public boolean back(){for(int i=getChildCount()-1;i>=0;i--){View child=getChildAt(i);if(child instanceof WmInputDialog&&WmControls.back(child))return true;}if(isPropertiesVisible()){propertyGeneration++;propertiesView.setVisibility(GONE);panelsChanged();return true;}if(isLibraryVisible()){showLibrary(false);return true;}if(isPlaylistVisible()){showPlaylist(false);return true;}if(fullScreen){setFullScreen(false);return true;}return false;}
    public boolean isPropertiesVisible(){return propertiesView.getVisibility()==VISIBLE;}
    public WmMediaPropertiesView properties(){return propertiesView;}
    /** Show title/URI and known current duration immediately. Content URIs receive
     * worker metadata reads; HTTP(S) sources are not downloaded for metadata.
     * Apply results on the UI thread only for the same visible request while not CLOSED.
     * Keep the decoder and underlying panel attached. */
    public void showProperties(int index){
        if(state==State.CLOSED)return;WmMediaQueue.Entry[] entries=queue.entries();if(index<0||index>=entries.length)throw new IllegalArgumentException("Selection outside sources");
        final WmMediaQueue.Entry entry=entries[index];final int length=index==queue.currentIndex()&&duration>0?duration:-1,token=++propertyGeneration;
        propertiesView.resetScroll();propertiesView.resetSelection();propertiesView.setValues(WmMediaPropertiesView.seed(entry,length));propertiesView.setVisibility(VISIBLE);panelsChanged();
        worker.execute(()->{String[] values=WmMediaPropertiesView.read(getContext().getApplicationContext(),entry,length);ui.post(()->{if(state!=State.CLOSED&&token==propertyGeneration&&isPropertiesVisible())propertiesView.setValues(values);});});
    }
    public State state(){return state;} public int duration(){return duration;} public int position(){return position;} public String error(){return error;}
    public String title(){return name;} public int volume(){return volume;} public boolean muted(){return muted;}
    /** A picker-granted content URI or HTTP(S) URL. No raw paths or file:// input. */
    public void open(Uri uri,String title){
        WmMediaQueue.Entry entry=new WmMediaQueue.Entry(uri,title);setPlaylist(new WmMediaQueue.Entry[]{entry},0,true);showPlaylist(false);
    }
    public WmMediaQueue.Entry[] playlist(){return queue.entries();}public int currentIndex(){return queue.currentIndex();}
    public void setPlaylist(WmMediaQueue.Entry[] entries,int index,boolean autoPlay){if(state==State.CLOSED)throw new IllegalStateException("Player closed");queue.replace(entries,index);if(index<0)clearSource();else loadSource(queue.entries()[index],autoPlay);}
    public void enqueue(WmMediaQueue.Entry[] entries){if(state==State.CLOSED)throw new IllegalStateException("Player closed");queue.append(entries);looping();refresh();}
    public void playIndex(int index){if(state==State.CLOSED)throw new IllegalStateException("Player closed");if(index==queue.currentIndex()&&prepared()){play();return;}queue.select(index);loadSource(queue.entries()[index],true);}
    public void removeSource(int index){if(state==State.CLOSED)throw new IllegalStateException("Player closed");boolean resume=state==State.PLAYING||state==State.PREPARING&&wantsPlay;if(queue.remove(index)){if(queue.size()==0)clearSource();else loadSource(queue.entries()[queue.currentIndex()],resume);}else{looping();refresh();}}
    public void moveSource(int from,int to){if(state==State.CLOSED)throw new IllegalStateException("Player closed");queue.move(from,to);refresh();}
    public void clearPlaylist(){if(state==State.CLOSED)return;queue.clear();clearSource();}
    private void clearSource(){retire();state=State.EMPTY;duration=position=videoWidth=videoHeight=0;name=error="";fullScreen=wantsPlay=false;video.setAlpha(0);requestLayout();refresh();}
    public boolean shuffle(){return queue.shuffle();}public void setShuffle(boolean value){queue.setShuffle(value);refresh();}
    public boolean canNext(){return queue.canAdvance(1,repeat);}public boolean canPrevious(){return queue.canAdvance(-1,repeat);}
    public void next(){advance(1,state==State.PLAYING);}public void previous(){advance(-1,state==State.PLAYING);}
    private boolean advance(int direction,boolean resume){int index=queue.advance(direction,repeat);if(index<0)return false;loadSource(queue.entries()[index],resume);return true;}
    private void loadSource(WmMediaQueue.Entry entry,boolean autoPlay){
        if(state==State.CLOSED)throw new IllegalStateException("Player closed");retire();name=entry.title;Uri uri=entry.uri;
        duration=position=videoWidth=videoHeight=0;fullScreen=false;requestLayout();video.setAlpha(0);error="";wantsPlay=autoPlay;state=State.PREPARING;
        final MediaPlayer next=new MediaPlayer();player=next;next.setAudioStreamType(AudioManager.STREAM_MUSIC);next.setLooping(repeat&&queue.size()==1);if(surface!=null)next.setSurface(surface);applyVolume();
        next.setOnPreparedListener(p->{if(player!=p)return;duration=p.getDuration();state=State.READY;fitVideo();refresh();if(wantsPlay)play();});
        next.setOnVideoSizeChangedListener((p,w,h)->{if(player!=p)return;videoWidth=w;videoHeight=h;video.setAlpha(w>0&&h>0?1:0);fitVideo();});
        next.setOnCompletionListener(p->{if(player!=p)return;if(advance(1,true))return;state=State.ENDED;position=duration;audio.abandonAudioFocus(focus);removeCallbacks(tick);refresh();});
        next.setOnErrorListener((p,what,extra)->{if(player!=p)return true;fail("MediaPlayer "+what+"/"+extra);return true;});
        refresh();final Context app=getContext().getApplicationContext();worker.execute(()->{try{next.setDataSource(app,uri);next.prepareAsync();}catch(IOException|RuntimeException failure){ui.post(()->{if(player==next)fail("미디어를 열 수 없습니다.");});}});
    }
    private boolean prepared(){return state==State.READY||state==State.PLAYING||state==State.PAUSED||state==State.ENDED;}
    public void play(){if(state==State.PREPARING){wantsPlay=true;return;}if(!prepared())return;if(audio.requestAudioFocus(focus,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED)return;try{if(state==State.ENDED)player.seekTo(0);player.start();state=State.PLAYING;removeCallbacks(tick);post(tick);refresh();}catch(IllegalStateException invalid){fail("재생할 수 없습니다.");}}
    public void pause(){wantsPlay=false;if(state!=State.PLAYING)return;player.pause();state=State.PAUSED;removeCallbacks(tick);updatePosition();audio.abandonAudioFocus(focus);refresh();}
    public void toggle(){if(state==State.PLAYING)pause();else play();}
    /** Clears autoplay intent. During PREPARING, preparation continues and finishes
     * in READY. Prepared sources seek to zero and remain READY. */
    public void stop(){wantsPlay=false;if(!prepared())return;pause();seekTo(0);state=State.READY;refresh();}
    public void seekTo(int millis){if(!prepared())return;position=Math.max(0,Math.min(duration,millis));player.seekTo(position);if(state==State.ENDED)state=State.PAUSED;refresh();}
    public void setVolume(int percent){volume=Math.max(0,Math.min(100,percent));applyVolume();refresh();}
    public void setMuted(boolean value){muted=value;applyVolume();refresh();}
    private void applyVolume(){if(player!=null&&state!=State.ERROR){float gain=muted?0:volume/100f;player.setVolume(gain,gain);}}
    public void setRepeat(boolean value){repeat=value;looping();refresh();}
    private void looping(){if(player!=null&&state!=State.ERROR)player.setLooping(repeat&&queue.size()==1);}
    public boolean repeat(){return repeat;}
    public boolean hasVideo(){return prepared()&&videoWidth>0&&videoHeight>0;}
    public void setFullScreen(boolean value){if(value&&!hasVideo())return;fullScreen=value;requestLayout();skin.invalidate();}
    public boolean fullScreen(){return fullScreen;}
    private void updatePosition(){if(prepared()&&!dragging){position=player.getCurrentPosition();if(duration>0)seek.setProgress((int)((long)position*1000/duration));skin.invalidate();}}
    private void fail(String message){state=State.ERROR;error=message;wantsPlay=false;removeCallbacks(tick);audio.abandonAudioFocus(focus);refresh();}
    private void refresh(){playlistView.setEntries(queue.entries(),queue.currentIndex());seek.setEnabled(prepared()&&duration>0);buttons[0].setEnabled(prepared());buttons[1].setEnabled(prepared()&&canPrevious());buttons[2].setEnabled(prepared()&&canNext());buttons[6].setEnabled(videoWidth>0);skin.invalidate();if(listener!=null)listener.changed(this);}
    private void retire(){removeCallbacks(tick);audio.abandonAudioFocus(focus);final MediaPlayer previous=player;player=null;if(previous!=null)worker.execute(previous::release);}
    /** Idempotent; retiring on the serial worker also covers in-flight source setup. */
    public void close(){if(state==State.CLOSED)return;state=State.CLOSED;listener=null;playlistListener=null;playlistView.close();libraryView.close();retire();worker.shutdown();if(surface!=null){surface.release();surface=null;}recycle(background);recycle(pushed);recycle(disabled);recycle(thumb);background=pushed=disabled=thumb=null;}
    @Override protected void onDetachedFromWindow(){pause();super.onDetachedFromWindow();}
    private void fitVideo(){if(videoWidth<=0||videoHeight<=0||video.getWidth()<=0||video.getHeight()<=0)return;float w=video.getWidth(),h=video.getHeight(),fit=Math.min(w/videoWidth,h/videoHeight);Matrix matrix=new Matrix();matrix.setScale(videoWidth*fit/w,videoHeight*fit/h);matrix.postTranslate((w-videoWidth*fit)/2,(h-videoHeight*fit)/2);video.setTransform(matrix);}
    @Override protected void onMeasure(int width,int height){
        super.onMeasure(width,height);float fit=Math.min(1,Math.min(getMeasuredWidth()/480f,getMeasuredHeight()/536f));
        if(fit>0){int w=MeasureSpec.makeMeasureSpec(Math.round(getMeasuredWidth()/fit),MeasureSpec.EXACTLY),h=MeasureSpec.makeMeasureSpec(Math.round(getMeasuredHeight()/fit),MeasureSpec.EXACTLY);playlistView.measure(w,h);libraryView.measure(w,h);propertiesView.measure(w,h);for(int i=0;i<getChildCount();i++)if(getChildAt(i) instanceof WmInputDialog)getChildAt(i).measure(w,h);}
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        scale=Math.min(1,Math.min(getWidth()/480f,getHeight()/536f));if(scale<=0)return;left=(getWidth()-480*scale)/2;top=0;extra=Math.max(0,getHeight()/scale-536);skin.layout(0,0,getWidth(),getHeight());
        if(fullScreen)video.layout(0,0,getWidth(),getHeight());else place(video,10,74,460,310+extra);
        for(int i=0;i<buttons.length;i++){int[] rect=rects[i];place(buttons[i],rect[0],rect[1]+extra,rect[2],rect[3]);buttons[i].setVisibility(fullScreen||panelVisible()?GONE:VISIBLE);}place(seek,114,408+extra,256,20);seek.setVisibility(fullScreen||panelVisible()?GONE:VISIBLE);for(View panel:new View[]{playlistView,libraryView,propertiesView}){panel.setPivotX(0);panel.setPivotY(0);panel.setScaleX(scale);panel.setScaleY(scale);panel.layout(0,0,panel.getMeasuredWidth(),panel.getMeasuredHeight());}for(int i=0;i<getChildCount();i++){View modal=getChildAt(i);if(modal instanceof WmInputDialog){modal.setPivotX(0);modal.setPivotY(0);modal.setScaleX(scale);modal.setScaleY(scale);modal.layout(0,0,modal.getMeasuredWidth(),modal.getMeasuredHeight());}}fitVideo();
    }
    private void place(View v,float x,float y,float w,float h){v.layout(Math.round(left+x*scale),Math.round(top+y*scale),Math.round(left+(x+w)*scale),Math.round(top+(y+h)*scale));}
    private final class SkinCanvas extends View {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        SkinCanvas(Context context,Typeface font){super(context);paint.setTypeface(font);if(android.os.Build.VERSION.SDK_INT>=16)setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        private void label(Canvas c,String text,float x,float y,float size,Paint.Align align){paint.setColor(Color.WHITE);paint.setTextSize(size);paint.setTextAlign(align);Paint.FontMetrics f=paint.getFontMetrics();WmFonts.draw(c,text,x,y-(f.ascent+f.descent)/2,paint);}
        @Override protected void onDraw(Canvas c){if(fullScreen)return;c.save();c.translate(left,top);c.scale(scale,scale);if(background!=null){c.drawBitmap(background,new Rect(0,0,480,404),new RectF(0,0,480,404+extra),paint);c.drawBitmap(background,new Rect(0,404,480,536),new RectF(0,404+extra,480,536+extra),paint);}else{paint.setColor(0xff173953);c.drawRect(0,0,480,536+extra,paint);paint.setColor(Color.BLACK);c.drawRect(10,74,470,384+extra,paint);}
            c.save();c.clipRect(10,10,321,43);Typeface body=paint.getTypeface();paint.setTypeface(titleFont);label(c,name,10,26,26.667f,Paint.Align.LEFT);paint.setTypeface(body);c.restore();
            String status=state==State.ERROR?error:state==State.PREPARING?"여는 중...":state==State.PLAYING?"재생 중":state==State.PAUSED?"일시 중지":"";
            c.save();c.clipRect(10,44,321,70);label(c,WmStrings.text(getContext(),language,status),10,57,21.333f,Paint.Align.LEFT);c.restore();
            c.translate(0,extra);
            label(c,String.format(java.util.Locale.ROOT,"%02d:%02d",position/60000,position/1000%60),10,419,21.333f,Paint.Align.LEFT);label(c,(muted?0:volume)+"%",474,419,21.333f,Paint.Align.RIGHT);
            int[] spriteX={158,100,234,392,342,292,0};String[] fallback={state==State.PLAYING?"Ⅱ":"▶","|◀","▶|",muted?"×":"♪","+","−","□"};
            for(int i=0;i<buttons.length;i++){int[] a=rects[i];boolean pressed=buttons[i].isPressed();int source=spriteX[i];if(i==0&&state==State.PLAYING)source=pressed?496:420;else if(pressed&&i==0)source=158;
                if(disabled!=null&&i<3&&!buttons[i].isEnabled()){c.drawBitmap(disabled,new Rect(spriteX[i],0,spriteX[i]+a[2],a[3]),new Rect(a[0],a[1],a[0]+a[2],a[1]+a[3]),paint);}else if(pushed!=null&&(pressed||i==0&&state==State.PLAYING||i==3&&muted)){c.drawBitmap(pushed,new Rect(source,0,source+a[2],a[3]),new Rect(a[0],a[1],a[0]+a[2],a[1]+a[3]),paint);}else if(background==null){paint.setColor(pressed?0xff477fa1:0xff204e70);c.drawRect(a[0],a[1],a[0]+a[2],a[1]+a[3],paint);label(c,fallback[i],a[0]+a[2]/2f,a[1]+a[3]/2f,24,Paint.Align.CENTER);}}
            float x=114+(duration>0?216f*position/duration:0);if(thumb!=null)c.drawBitmap(thumb,new Rect(dragging?40:0,0,dragging?80:40,20),new RectF(x,408,x+40,428),paint);else{paint.setColor(Color.WHITE);c.drawRect(114,416,370,420,paint);c.drawRect(x,408,x+20,428,paint);}c.restore();
        }
    }
}
