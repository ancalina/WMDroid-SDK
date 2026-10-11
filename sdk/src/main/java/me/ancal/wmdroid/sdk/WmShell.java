package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Optional launcher bridge. Appearance/menu queries are read-only; clock mode is a public cosmetic setter. Audio memory only
 * remembers the actual system media level, never caller-provided preferences.
 * No owner, messages, tasks or PIM data.
 * Android activities remain ordinary independent apps; WM activities opt in.
 */
public final class WmShell {
    private WmShell(){}
    public static final String PACKAGE="me.ancal.wmdroid",ACTION="me.ancal.wmdroid.sdk.SHELL_ACTION",COMMAND="command";
    public static final String WM_STYLE="me.ancal.wmdroid.sdk.WM_STYLE";
    /** Explicit zero-animation launch options supplement window/Intent settings. */
    public static void launchWithoutAnimation(android.app.Activity owner,Intent original){
        Intent action=prepareLaunch(owner,original).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        if(android.os.Build.VERSION.SDK_INT>=16)Api16.launch(owner,action);else owner.startActivity(action);
        owner.overridePendingTransition(0,0);
    }
    private static final class Api16 {static void launch(android.app.Activity owner,Intent action){owner.startActivity(action,android.app.ActivityOptions.makeCustomAnimation(owner,0,0).toBundle());}}
    /** WM activities opt into an in-task handoff; ordinary Android apps keep their launch flags. */
    public static Intent prepareLaunch(Context context,Intent original){
        Intent action=new Intent(original);
        try{
            android.content.pm.ResolveInfo resolved=context.getPackageManager().resolveActivity(action,android.content.pm.PackageManager.GET_META_DATA);
            if(resolved!=null&&resolved.activityInfo!=null&&resolved.activityInfo.metaData!=null&&resolved.activityInfo.metaData.getBoolean(WM_STYLE,false)){
                action.setFlags((action.getFlags()&~(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED|Intent.FLAG_ACTIVITY_MULTIPLE_TASK|Intent.FLAG_ACTIVITY_NEW_DOCUMENT))|Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
        }catch(SecurityException unavailable){}
        return action;
    }
    public static final Uri STATE=Uri.parse("content://me.ancal.wmdroid.shell/state"),MENU=Uri.parse("content://me.ancal.wmdroid.shell/menu");
    public static final Uri START_BACKGROUND=Uri.parse("content://me.ancal.wmdroid.shell/start/background");
    /** Query generation, then open /resources/{generation}; data-only, read-only. */
    public static final Uri RESOURCES=Uri.parse("content://me.ancal.wmdroid.shell/resources");
    public static final String[] STATE_COLUMNS={"font","hybrid","accent","language","clock","analog","retro","delay","scan","lines","scale","repaint","hold","icons_unframed","icons_mode","icons_list","start_text_color","ui_scale_percent","screen_mode","font_bold","touch_sound_level","start_texture","start_tint","start_theme_color","font_smoothing"};
    public static final String[] MENU_COLUMNS={"id","parent","title","icon","component","command","folder"};
    public static final class State {
        public WmIconPolicy iconPolicy=new WmIconPolicy(false,"all","[]");
        public String font=WmFonts.DEFAULT_SOURCE,fontBold=WmFonts.DEFAULT_SOURCE,language="ko";public boolean hybrid=true,clock=true,analog=false,fontSmoothing=true;public int accent=0xff15afbc;public boolean iconsUnframed,available,retro=true,scan=true,repaint=false,hold=true;public int delay=120,lines=64,scale=1000;
        public String startTexture="";public boolean startTint;public int startThemeColor=0xff15afbc;
        public int touchSoundLevel=0;public int startTextColor=Color.BLACK,uiScalePercent=100;public String screenMode="full";
    }
    public static final class Entry {
        public final String id,parent,title,icon,component,command;public final boolean folder;
        public Entry(String id,String parent,String title,String icon,String component,String command,boolean folder){this.id=id;this.parent=parent;this.title=title;this.icon=icon;this.component=component;this.command=command;this.folder=folder;}
    }
    public static State state(Context context){State state=new State();state.analog=context.getSharedPreferences("wmdroid_shell",0).getBoolean("analog_clock",false);try(Cursor c=context.getContentResolver().query(STATE,STATE_COLUMNS,null,null,null)){if(c!=null&&c.moveToFirst()){state.available=true;state.startTextColor=Color.WHITE;state.font=c.getString(0);state.hybrid=c.getInt(1)!=0;state.accent=c.getInt(2);state.language=c.getString(3);state.clock=c.getInt(4)!=0;state.analog=c.getInt(5)!=0;state.retro=c.getInt(6)!=0;state.delay=c.getInt(7);state.scan=c.getInt(8)!=0;state.lines=c.getInt(9);state.scale=c.getInt(10);state.repaint=c.getInt(11)!=0;state.hold=c.getInt(12)!=0;int icons=c.getColumnIndex("icons_unframed");if(icons>=0)state.iconsUnframed=c.getInt(icons)!=0;int mode=c.getColumnIndex("icons_mode"),list=c.getColumnIndex("icons_list");state.iconPolicy=new WmIconPolicy(state.iconsUnframed,mode>=0?c.getString(mode):"all",list>=0?c.getString(list):"[]");int startColor=c.getColumnIndex("start_text_color");if(startColor>=0&&!c.isNull(startColor))state.startTextColor=c.getInt(startColor);int uiScale=c.getColumnIndex("ui_scale_percent"),screenMode=c.getColumnIndex("screen_mode");if(uiScale>=0&&!c.isNull(uiScale))state.uiScalePercent=WmViewport.scalePercent(c.getInt(uiScale));if(screenMode>=0)state.screenMode=WmViewport.screenMode(c.getString(screenMode));int fontBold=c.getColumnIndex("font_bold");if(fontBold>=0&&!c.isNull(fontBold))state.fontBold=c.getString(fontBold);int touchSound=c.getColumnIndex("touch_sound_level");if(touchSound>=0&&!c.isNull(touchSound))state.touchSoundLevel=Math.max(0,Math.min(2,c.getInt(touchSound)));int texture=c.getColumnIndex("start_texture"),tint=c.getColumnIndex("start_tint"),theme=c.getColumnIndex("start_theme_color");if(texture>=0&&!c.isNull(texture))state.startTexture=c.getString(texture);if(tint>=0)state.startTint=c.getInt(tint)!=0;if(theme>=0)state.startThemeColor=c.getInt(theme);int smoothing=c.getColumnIndex("font_smoothing");if(smoothing>=0&&!c.isNull(smoothing))state.fontSmoothing=c.getInt(smoothing)!=0;}}catch(RuntimeException absent){}return state;}
    /** Read-only host Start texture; local known templates, otherwise the compatible PNG route. */
    public static Drawable startBackground(Context context){return startBackground(context,state(context));}
    private static final java.util.Map<Context,StartCache> START_CACHE=new java.util.WeakHashMap<>();
    private static final class StartCache {
        final WmResourcePack pack;final String asset;final Bitmap decoded;String key;Bitmap transformed;
        StartCache(WmResourcePack pack,String asset,Bitmap decoded){this.pack=pack;this.asset=asset;this.decoded=decoded;}
    }
    /** Known original templates render locally; older hosts retain their PNG bridge. */
    public static Drawable startBackground(Context context,State state){
        String asset="classic".equals(state.startTexture)?"backgrounds/classic.gif":"mugunghwa".equals(state.startTexture)?"start_mugunghwa.gif":null;
        if(asset!=null)try{return localStartBackground(context,asset,state);}catch(IOException|RuntimeException absent){}
        try(InputStream input=context.getContentResolver().openInputStream(START_BACKGROUND)){
            if(input!=null){Bitmap bitmap=BitmapFactory.decodeStream(input);if(bitmap!=null)return new WmStartBackground(bitmap);}
        }catch(IOException|RuntimeException absent){}
        return new ColorDrawable(state.startTint?state.startThemeColor:Color.WHITE);
    }
    private static synchronized Drawable localStartBackground(Context context,String asset,State state)throws IOException{
        String key=asset+"|"+state.startTint+"|"+(state.startTint?state.startThemeColor:0);
        Context app=context.getApplicationContext();if(app==null)app=context;
        WmResourcePack pack=WmResources.prepare(app);StartCache cache=START_CACHE.get(app);
        if(cache==null||cache.pack!=pack||!asset.equals(cache.asset)){
            Bitmap bitmap=WmResources.bitmap(context,asset,"classic".equals(state.startTexture)?480:40,"classic".equals(state.startTexture)?640:588);
            cache=new StartCache(pack,asset,bitmap);START_CACHE.put(app,cache);
        }
        if(!key.equals(cache.key)){cache.transformed=state.startTint?WmColor.tint(cache.decoded,"classic".equals(state.startTexture)?0xff15afbc:0xff94010c,state.startThemeColor):cache.decoded;cache.key=key;}
        // One raw template and one current transformed bitmap. Retained menus
        // may still own older shaders; never recycle those bitmaps.
        return new WmStartBackground(cache.transformed);
    }
    public static List<Entry> menu(Context context){List<Entry> rows=new ArrayList<>();try(Cursor c=context.getContentResolver().query(MENU,MENU_COLUMNS,null,null,null)){if(c!=null)while(c.moveToNext())rows.add(new Entry(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getInt(6)!=0));}catch(RuntimeException absent){}return rows;}
    public static Intent command(String value){return new Intent(ACTION).setClassName(PACKAGE,PACKAGE+".TodayActivity").putExtra(COMMAND,value).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);}
    /** Set the shared cosmetic clock preference; absent host falls back locally. */
    public static void clockMode(Context context,boolean analog){
        try{android.os.Bundle result=context.getContentResolver().call(STATE,"clock_mode",analog?"analog":"digital",null);if(result!=null&&result.getBoolean("saved"))return;}catch(RuntimeException absent){}
        context.getSharedPreferences("wmdroid_shell",0).edit().putBoolean("analog_clock",analog).apply();
    }
    /** No volume changes here. Absent launcher falls back to this app's memory. */
    public static void rememberMusic(Context context,int current){
        context.getSharedPreferences("today",0).edit().putInt("volume_saved_music",current).apply();
        try{context.getContentResolver().call(STATE,"remember_music",null,null);}catch(RuntimeException absent){}
    }
    public static int savedMusic(Context context,int fallback){
        try{android.os.Bundle result=context.getContentResolver().call(STATE,"saved_music",null,null);if(result!=null&&result.containsKey("level"))return result.getInt("level");}catch(RuntimeException absent){}
        return context.getSharedPreferences("today",0).getInt("volume_saved_music",fallback);
    }
}
