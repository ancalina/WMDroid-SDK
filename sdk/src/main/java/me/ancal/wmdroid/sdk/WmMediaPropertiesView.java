package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.DashPathEffect;
import android.view.View;
import android.view.KeyEvent;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

/** Native Media property order/roles from wmploc1116..1132 and emulator capture.
 * Values are literal provider metadata. Unknown codec/DRM/rating/play-count data
 * is omitted; Android decoding information is not a Windows codec engine. */
public final class WmMediaPropertiesView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener {
    // Native dialog102 Tahoma8pt at logical VGA192dpi.
    private static final float TEXT_SIZE=8f*192f/72f;
    private static final String[] LABELS={"음악가","앨범","앨범 음악가","제목","장르","길이","오디오 코덱","오디오 형식","비디오 코덱","비디오 형식","위치","트랙 번호","비트 전송률"};
    private final LinearLayout rows;
    private final WmScrollView scroll;
    private Typeface regular,bold;
    private String language="ko";
    private int caret=-1,selection=0xff149ca7;
    private boolean selected;
    private String[] values=new String[LABELS.length];
    public WmMediaPropertiesView(Context context,Typeface regular,Typeface bold){super(context);setFocusable(true);setFocusableInTouchMode(true);this.regular=regular;this.bold=bold;setOrientation(VERTICAL);rows=new LinearLayout(context);rows.setOrientation(VERTICAL);scroll=new WmScrollView(context,rows);addView(scroll,new LayoutParams(-1,-1));onWmRestyle(regular,bold,0xff149ca7,0xffc9e7e9);}
    public void setValues(String[] snapshot){if(snapshot==null||snapshot.length!=LABELS.length)throw new IllegalArgumentException("Property snapshot required");int position=scroll.getRequestedScrollPosition();values=snapshot.clone();rebuild();scroll.scrollToPosition(position);}
    public String[] values(){return values.clone();}
    public void resetScroll(){scroll.scrollToPosition(0);}
    /** Native20301/20302. WMDroid has no legacy album-shop service. */
    public WmMenuItem[] menuItems(){return new WmMenuItem[]{WmMenuItem.command(20302,WmStrings.text(getContext(),language,"CD 구입")).withEnabled(false)};}

    void resetSelection(){caret=-1;selected=false;invalidateFields();}
    private TextView field(int key,Typeface font,CharSequence value){Field text=new Field(key);WmControls.textStyle(text,font);text.setText(value);return text;}
    private int caretRow(){for(int n=0;n<rows.getChildCount();n++)if(((Field)rows.getChildAt(n)).key==caret)return n;return -1;}
    private void invalidateFields(){if(rows!=null)for(int n=0;n<rows.getChildCount();n++)rows.getChildAt(n).invalidate();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(isShown())requestFocus();}
    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);if(rows!=null&&visibility==VISIBLE&&isShown())requestFocus();}
    @Override protected void onFocusChanged(boolean focus,int direction,Rect previous){super.onFocusChanged(focus,direction,previous);invalidateFields();}
    @Override public boolean onKeyDown(int key,KeyEvent event){
        if((key!=KeyEvent.KEYCODE_DPAD_DOWN&&key!=KeyEvent.KEYCODE_DPAD_UP)||!event.hasNoModifiers()||!isEnabled()||!WmControls.ownerCommandsAllowed(this))return super.onKeyDown(key,event);
        int count=rows.getChildCount();if(count==0)return true;int row=caretRow();if(row<0)row=0;
        row=Math.max(0,Math.min(count-1,row+(key==KeyEvent.KEYCODE_DPAD_DOWN?1:-1)));Field field=(Field)rows.getChildAt(row);caret=field.key;selected=true;invalidateFields();
        int top=scroll.getRequestedScrollPosition(),height=scroll.getHeight();if(height>0&&field.getHeight()>0){if(field.getTop()<top)scroll.scrollToPosition(field.getTop());else if(field.getBottom()>top+height)scroll.scrollToPosition(field.getBottom()-height);}return true;
    }
    @Override public boolean onKeyUp(int key,KeyEvent event){if((key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN)&&event.hasNoModifiers()&&isEnabled()&&WmControls.ownerCommandsAllowed(this))return true;return super.onKeyUp(key,event);}
    private final class Field extends TextView {
        final int key;final Paint mark=new Paint();
        Field(int key){super(WmMediaPropertiesView.this.getContext());this.key=key;}
        @Override protected void onDraw(Canvas canvas){
            boolean active=caret==key&&WmMediaPropertiesView.this.hasFocus();int color=active&&selected?Color.WHITE:Color.BLACK;if(getCurrentTextColor()!=color)setTextColor(color);
            android.text.Layout layout=getLayout();if(active&&layout!=null){
                mark.setStyle(selected?Paint.Style.FILL:Paint.Style.STROKE);mark.setStrokeWidth(1);mark.setPathEffect(selected?null:new DashPathEffect(new float[]{1,1},0));mark.setColor(selected?WmPalette.highlight(selection):Color.BLACK);
                float y=getExtendedPaddingTop();for(int line=0;line<layout.getLineCount();line++){float left=getPaddingLeft()+layout.getLineLeft(line)-2,right=Math.min(getWidth(),getPaddingLeft()+layout.getLineRight(line)+2);canvas.drawRect(left,y+layout.getLineTop(line),right,Math.min(getHeight()-1,y+layout.getLineBottom(line)),mark);}
            }
            super.onDraw(canvas);
        }
    }
    private void rebuild(){rows.removeAllViews();for(int n=0;n<LABELS.length;n++){if(values[n]==null)continue;TextView label=field(n*2,bold,"");WmControls.bindText(label,language,LABELS[n]);label.setTextSize(TypedValue.COMPLEX_UNIT_PX,TEXT_SIZE);label.setPadding(6,0,4,0);rows.addView(label,new LayoutParams(-1,27));TextView value=field(n*2+1,regular,values[n]);value.setTextSize(TypedValue.COMPLEX_UNIT_PX,TEXT_SIZE);value.setPadding(10,0,8,0);value.setMinimumHeight(27);rows.addView(value,new LayoutParams(-1,-2));}if(caretRow()<0){caret=rows.getChildCount()==0?-1:((Field)rows.getChildAt(0)).key;selected=false;}}
    @Override public void onWmLanguageChanged(String value){language=WmStrings.language(getContext(),value);for(int n=0,row=0;n<LABELS.length;n++)if(values[n]!=null){WmControls.bindText((TextView)rows.getChildAt(row),language,LABELS[n]);row+=2;}}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int face){this.regular=regular;this.bold=bold;this.selection=selection;setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.WHITE,face}));WmControls.restyleTree(scroll,regular,bold,selection,face);for(int n=0;n<rows.getChildCount();n++){TextView field=(TextView)rows.getChildAt(n);Typeface font=n%2==0?bold:regular;field.setTypeface(font);WmFonts.attach(field,font);field.invalidate();}requestLayout();}
    private static String bounded(String value){return value==null?null:value.length()<=1024?value:value.substring(0,1023)+"…";}
    static String[] seed(WmMediaQueue.Entry entry,int duration){String[] result=new String[LABELS.length];result[3]=entry.title;result[10]=bounded(entry.uri.toString());if(duration>=0)result[5]=length(duration);return result;}
    private static String length(long millis){long seconds=Math.max(0,millis/1000);return String.format(Locale.ROOT,seconds>=3600?"%d:%02d:%02d":"%02d:%02d",seconds>=3600?new Object[]{seconds/3600,seconds/60%60,seconds%60}:new Object[]{seconds/60,seconds%60});}
    /** Called only on the owner's worker. HTTP properties avoid a second download. */
    static String[] read(Context context,WmMediaQueue.Entry entry,int duration){
        String[] result=seed(entry,duration);if(!"content".equals(entry.uri.getScheme()))return result;
        MediaMetadataRetriever metadata=new MediaMetadataRetriever();try{metadata.setDataSource(context,entry.uri);int[] keys={MediaMetadataRetriever.METADATA_KEY_ARTIST,MediaMetadataRetriever.METADATA_KEY_ALBUM,MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST,MediaMetadataRetriever.METADATA_KEY_TITLE,MediaMetadataRetriever.METADATA_KEY_GENRE};for(int n=0;n<keys.length;n++){String value=bounded(metadata.extractMetadata(keys[n]));if(value!=null&&!value.isEmpty())result[n]=value;}
            String millis=metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);if(millis!=null)try{result[5]=length(Long.parseLong(millis));}catch(NumberFormatException ignored){}
            result[11]=bounded(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER));String bitrate=metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);if(bitrate!=null)try{result[12]=Long.parseLong(bitrate)/1000+" kbps";}catch(NumberFormatException ignored){}
        }catch(RuntimeException unreadable){}finally{try{metadata.release();}catch(Exception unavailable){}}
        if(Build.VERSION.SDK_INT>=16)Tracks.read(context,entry.uri,result);return result;
    }
    private static final class Tracks {
        static void read(Context context,Uri uri,String[] result){android.media.MediaExtractor extractor=new android.media.MediaExtractor();try{extractor.setDataSource(context,uri,null);for(int n=0;n<extractor.getTrackCount();n++){android.media.MediaFormat format=extractor.getTrackFormat(n);String mime=format.getString(android.media.MediaFormat.KEY_MIME);if(mime==null)continue;if(mime.startsWith("audio/")&&result[6]==null){result[6]=bounded(mime);String text="";if(format.containsKey(android.media.MediaFormat.KEY_SAMPLE_RATE))text=format.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE)+" Hz";if(format.containsKey(android.media.MediaFormat.KEY_CHANNEL_COUNT))text+=(text.isEmpty()?"":", ")+format.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT)+" ch";if(!text.isEmpty())result[7]=text;}else if(mime.startsWith("video/")&&result[8]==null){result[8]=bounded(mime);if(format.containsKey(android.media.MediaFormat.KEY_WIDTH)&&format.containsKey(android.media.MediaFormat.KEY_HEIGHT))result[9]=format.getInteger(android.media.MediaFormat.KEY_WIDTH)+" × "+format.getInteger(android.media.MediaFormat.KEY_HEIGHT);}}}catch(java.io.IOException|RuntimeException unreadable){}finally{extractor.release();}}
    }
}
