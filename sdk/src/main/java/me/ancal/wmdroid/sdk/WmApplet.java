package me.ancal.wmdroid.sdk;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Independent WM-style activity using the same window and controls as the host. */
public abstract class WmApplet extends Activity {
    public static final String LANGUAGE="me.ancal.wmdroid.sdk.LANGUAGE";
    public static final String ACCENT="me.ancal.wmdroid.sdk.ACCENT",FONT="me.ancal.wmdroid.sdk.FONT",HYBRID="me.ancal.wmdroid.sdk.HYBRID";
    protected LinearLayout content;protected Typeface regular,bold;protected int accent;protected WmWindow shellWindow;
    private TextView caption;private WmTrayClock clock;private ImageView close;private WmTitleBar title;private LinearLayout start;private WmCommandBar footer;
    private WmBar titlePaint;private FrameLayout screen,menuOverlay;private WmViewport viewport;private WmStartMenu startMenu;private WmCommandMenu commandMenu;
    private WmStartMenu preparedStart;private android.graphics.drawable.Drawable preparedStartBackground;
    private int startBackgroundVersion;private boolean startBackgroundPending;
    private FrameLayout popupOverlay;private WmPopup popup;
    private final WmTraySlot[] trays=new WmTraySlot[4];private final String[] trayIcons=new String[4];private int statusKind=-1,statusAnchor;private boolean menuAnalog;
    private final Runnable trayTick=new Runnable(){public void run(){if(!resumed)return;refreshTray();clockHandler.postDelayed(this,1000);}};
    private WmShell.State shellState;private boolean resumed,analogClock,closeOk,observing,fontDestroyed;
    private final java.util.concurrent.ExecutorService fontWorker=java.util.concurrent.Executors.newSingleThreadExecutor();
    private String requestedFontKey,loadingFontKey,failedFontKey,loadedFontKey;private Runnable closeAction;
    private boolean resourceSyncPending,resourceSyncAgain;private String chromeResources;
    private final Handler clockHandler=new Handler(Looper.getMainLooper());
    // One preference commit can notify once per key. Read its latest snapshot once,
    // after those notifications, rather than repeatedly reshaping retained editors.
    private final Runnable stateRefresh=()->{if(resumed){queueResourceSync();retryFonts();applyShellState(WmShell.state(WmApplet.this));}};
    private final ContentObserver stateObserver=new ContentObserver(clockHandler){@Override public void onChange(boolean self){if(resumed){clockHandler.removeCallbacks(stateRefresh);clockHandler.post(stateRefresh);}}};
    private final Runnable clockTick=new Runnable(){@Override public void run(){if(!resumed)return;clock.refresh(new java.util.Date(),analogClock);clockHandler.postDelayed(this,60000-System.currentTimeMillis()%60000);}};

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);if(android.os.Build.VERSION.SDK_INT>=31)Api31.splash(this);getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);getWindow().getDecorView().setSystemUiVisibility(5894);getWindow().setWindowAnimations(0);overridePendingTransition(0,0);
        if(android.os.Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);}
        applyShellState(WmShell.state(this));
        screen=new FrameLayout(this);shellWindow=new WmWindow(this);screen.addView(shellWindow,new FrameLayout.LayoutParams(-1,-1));
        titlePaint=new WmBar(this,false,accent);title=new WmTitleBar(this,label(""),titlePaint);caption=title.caption;start=title.start;start.setSoundEffectsEnabled(false);
        start.setOnClickListener(v->{if(startMenu==null)showStart();else dismissStart();});
        for(int kind:new int[]{WmTraySlot.CONNECTION,WmTraySlot.PHONE,WmTraySlot.VOLUME}){
            ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            WmTraySlot slot=title.addTray(kind,image);trays[kind]=slot;slot.setOnClickListener(v->onTray(kind,slot.getLeft()+slot.getWidth()/2));
        }
        createClock();close=title.closeButton(closeOk||WmInputDialog.find(shellWindow.client)!=null);close.setOnClickListener(v->{WmInputDialog input=WmInputDialog.find(shellWindow.client);if(input!=null){input.accept();return;}if(!shellWindow.ownerCommandsAllowed())return;if(closeAction==null)finish();else closeAction.run();});
        shellWindow.title(title);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);WmPaint paint=new WmPaint(this,false,WmPaint.Role.FORM);paint.addView(content,new FrameLayout.LayoutParams(-1,-1));shellWindow.client.addView(paint,new FrameLayout.LayoutParams(-1,-1));
        footer=new WmCommandBar(this,regular,accent);shellWindow.commands(footer);footer.inputMethods(screen);refreshChrome();
        viewport=new WmViewport(this);viewport.setContent(screen);viewport.observeImeInsets();viewport.applyDisplay(this,shellState.uiScalePercent,shellState.screenMode);setContentView(viewport);
    }
    @Override protected void onResume(){
        super.onResume();resumed=true;retryFonts();queueResourceSync();
        try{getContentResolver().registerContentObserver(WmShell.STATE,false,stateObserver);observing=true;}catch(RuntimeException absent){}
        applyShellState(WmShell.state(this));clockHandler.removeCallbacks(clockTick);clockTick.run();clockHandler.removeCallbacks(trayTick);trayTick.run();
    }
    @Override protected void onPause(){resumed=false;if(trays[0]!=null)trays[0].stopConnectionAnimation();clockHandler.removeCallbacks(clockTick);clockHandler.removeCallbacks(trayTick);stopObserving();dismissStart();dismissPopup();dismissCommandMenu();super.onPause();}
    @Override protected void onDestroy(){fontDestroyed=true;fontWorker.shutdownNow();clockHandler.removeCallbacks(clockTick);clockHandler.removeCallbacks(trayTick);stopObserving();super.onDestroy();}
    private void stopObserving(){clockHandler.removeCallbacks(stateRefresh);if(observing){getContentResolver().unregisterContentObserver(stateObserver);observing=false;}}
    @Override public void onBackPressed(){if(commandMenu!=null){if(!commandMenu.back())dismissCommandMenu();return;}if(popupOverlay!=null){if(!WmControls.back(popup))dismissPopup();return;}if(startMenu!=null){if(!WmControls.back(startMenu))dismissStart();return;}if(WmControls.back(content))return;finish();}
    private static final class Api31 {static void splash(Activity activity){activity.getSplashScreen().setOnExitAnimationListener(view->view.remove());}}
    @Override public void finish(){super.finish();overridePendingTransition(0,0);}
    private final Runnable clockPopupTimeout=()->dismissPopup();
    protected void onClock(){
        if(statusKind==4&&popup!=null){dismissPopup();return;}
        WmClockPanel panel=new WmClockPanel(this,regular,shellState.language);
        int anchor=clock.getLeft()+clock.getWidth()/2;
        showPopup(text("시간 및 다음 약속"),panel,444,anchor);statusKind=4;statusAnchor=anchor;
        onClockPanelShown(panel);clockHandler.postDelayed(clockPopupTimeout,7000);
    }
    protected void onClockMenu(){
        dismissStart();dismissPopup();dismissCommandMenu();menuAnalog=analogClock;
        WmRadioMenu menu=new WmRadioMenu(this,regular,new String[]{text("아날로그"),text("디지털")},analogClock?0:1,text(" 선택됨"),shade(0xff149ca7),index->{
            dismissPopup();WmShell.clockMode(this,index==0);applyShellState(WmShell.state(this));
        });
        FrameLayout frame=new FrameLayout(this);frame.setBackgroundDrawable(menu.getBackground());menu.setBackgroundDrawable(null);frame.setOnClickListener(v->{});
        WmPaint paint=new WmPaint(this,false,WmPaint.Role.MENU);paint.addView(menu,new FrameLayout.LayoutParams(-1,-2));frame.addView(paint,new FrameLayout.LayoutParams(-1,-2));
        popupOverlay=new FrameLayout(this);popupOverlay.setOnClickListener(v->{if(!WmControls.back(popup))dismissPopup();});
        FrameLayout.LayoutParams position=new FrameLayout.LayoutParams(menu.menuWidth(),-2);position.leftMargin=Math.max(0,clock.getRight()-menu.menuWidth());position.topMargin=WmMetrics.BAR;popupOverlay.addView(frame,position);
        popupOverlay.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{FrameLayout.LayoutParams current=(FrameLayout.LayoutParams)frame.getLayoutParams();int left=Math.max(0,clock.getRight()-menu.menuWidth());if(current.leftMargin!=left){current.leftMargin=left;frame.setLayoutParams(current);}});
        screen.addView(popupOverlay,new FrameLayout.LayoutParams(-1,-1));statusKind=5;
    }
    /** Supply appointments using this app's own permission/data. Called on the UI thread.
     * Async providers must cancel/ignore updates after panel detachment; no host PIM is shared.
     */
    protected void onClockPanelShown(WmClockPanel panel){}
    /** UI-thread hook after every appearance application, including unchanged values.
     * Not a font-ready signal: initial loading can be synchronous, later cold changes use a worker.
     * Localized content remains the app's responsibility; editors are not rebuilt. */
    protected void onShellStateChanged(WmShell.State state){}
    /** UI-thread hook on a chrome-observed revision change, including initial application,
     * before onShellStateChanged. Refresh cached artwork without rebuilding editors. */
    protected void onResourcesChanged(){}
    private void queueResourceSync(){
        if(fontDestroyed)return;
        if(resourceSyncPending){resourceSyncAgain=true;return;}resourceSyncPending=true;
        fontWorker.execute(()->{
            boolean changed=false;try{changed=WmResources.syncFromShell(this);}catch(java.io.IOException unavailable){}
            final boolean updated=changed;
            clockHandler.post(()->{
                resourceSyncPending=false;if(fontDestroyed)return;
                // A widget provider can mirror first in the same process; chrome still needs the new revision.
                if(updated||!WmResources.revision(this).equals(chromeResources)){retryFonts();requestedFontKey=null;if(resumed)applyShellState(WmShell.state(this));}
                if(resourceSyncAgain){resourceSyncAgain=false;if(resumed)queueResourceSync();}
            });
        });
    }
    private void applyShellState(WmShell.State next){
        preparedStart=null;if(startMenu==null)preparedStartBackground=null;startBackgroundVersion++;
        if(!next.available){String font=getIntent().getStringExtra(FONT);if(font!=null)next.font=font;next.hybrid=getIntent().getBooleanExtra(HYBRID,true);next.accent=getIntent().getIntExtra(ACCENT,next.accent);String language=getIntent().getStringExtra(LANGUAGE);if(language!=null)next.language=language;}
        next.font=fontPath(next.font);next.language=WmStrings.language(this,next.language);
        boolean languageChanged=shellState!=null&&!next.language.equals(shellState.language);
        String heavy=next.fontBold==null||next.fontBold.isEmpty()?("fonts/malgun/regular.ttf".equals(next.font)?"fonts/malgun/bold.ttf":next.font):fontPath(next.fontBold);
        String wanted=next.font+"|"+heavy+"|"+next.hybrid+"|"+WmResources.revision(this);
        boolean ready=!wanted.equals(loadingFontKey)&&(loadedFontKey==null||(WmFonts.isCached(this,next.font,next.hybrid,false)&&WmFonts.isCached(this,heavy,next.hybrid,true)))&&(!WmFontFile.isSource(next.font)||WmFontFile.available(this,next.font))&&(!WmFontFile.isSource(heavy)||WmFontFile.available(this,heavy));
        if(!ready&&!wanted.equals(failedFontKey))queueFont(next.font,heavy,next.hybrid,wanted);
        String actual=ready?wanted:loadedFontKey==null?"fallback":loadedFontKey;
        boolean smoothingChanged=shellState==null||next.fontSmoothing!=shellState.fontSmoothing;
        WmFonts.setSmoothing(next.fontSmoothing);
        boolean fontChanged=!actual.equals(loadedFontKey);
        boolean themeChanged=shellState==null||next.accent!=shellState.accent;
        boolean clockChanged=fontChanged||themeChanged||shellState==null||next.clock!=shellState.clock;
        if(fontChanged){
            try{regular=WmFonts.load(this,ready?next.font:"fonts/malgun/regular.ttf",next.hybrid,false);bold=WmFonts.load(this,ready?heavy:"fonts/malgun/bold.ttf",next.hybrid,true);loadedFontKey=actual;}
            catch(RuntimeException unavailable){regular=WmFonts.load(this,"fonts/malgun/regular.ttf",next.hybrid,false);bold=WmFonts.load(this,"fonts/malgun/bold.ttf",next.hybrid,true);failedFontKey=wanted;loadedFontKey="fallback";}
        }
        shellState=next;accent=next.accent;analogClock=next.analog;
        if(viewport!=null){viewport.applyDisplay(this,next.uiScalePercent,next.screenMode);viewport.setTouchSoundLevel(next.touchSoundLevel);}
        getIntent().putExtra(FONT,next.font).putExtra(HYBRID,next.hybrid).putExtra(ACCENT,accent).putExtra(LANGUAGE,next.language);
        if(shellWindow==null)return;
        String resources=WmResources.revision(this);boolean resourcesChanged=!resources.equals(chromeResources);
        if(resourcesChanged){chromeResources=resources;title.reloadResources();java.util.Arrays.fill(trayIcons,null);for(WmTraySlot tray:trays)if(tray!=null)tray.reloadResources();}
        if(!next.retro)WmPaint.stopReplay(viewport==null?shellWindow:viewport);
        WmControls.localizeTree(footer,next.language);
        if(languageChanged)WmControls.localizeTree(content,next.language);
        if(fontChanged||themeChanged||resourcesChanged||smoothingChanged){WmControls.restyleTree(shellWindow,regular,bold,shade(0xff149ca7),shade(0xffc9e7e9));refreshChrome();}
        if(clockChanged)createClock();
        clock.language(next.language);clock.refresh(new java.util.Date(),analogClock);closeButton(closeOk,closeAction);
        start.setContentDescription(WmStrings.text(this,shellState.language,"시작 메뉴","shell.start_menu.description"));
        if(startMenu!=null){refreshStartMenu(preparedStartBackground);refreshStartBackground();}
        if(popup!=null){WmControls.restyleTree(popup,regular,bold,shade(0xff149ca7),shade(0xffc9e7e9));popup.setColors(shade(0xff15afbc),shade(0xff149ca7));}
        if(commandMenu!=null){if(languageChanged)dismissCommandMenu();else if(fontChanged||themeChanged||smoothingChanged)WmControls.restyleTree(commandMenu,regular,bold,shade(0xff149ca7),shade(0xffc9e7e9));}
        refreshTray();
        if(statusKind==5&&(fontChanged||themeChanged||languageChanged||next.analog!=menuAnalog)){onClockMenu();}
        else if(languageChanged&&statusKind>=0){int kind=statusKind,anchor=statusAnchor;dismissPopup();if(kind==4)onClock();else onTray(kind,anchor);}
        if(resourcesChanged)onResourcesChanged();onShellStateChanged(next);
    }
    private void refreshStartMenu(android.graphics.drawable.Drawable background){
        startMenu.refresh(WmShell.menu(this),regular,accent,background);
        startMenu.setTextColor(background instanceof ColorDrawable?Color.BLACK:shellState.startTextColor);
        startMenu.setBackLabel(text("뒤로"));startMenu.setIconPolicy(shellState.iconPolicy);
    }
    private void refreshStartBackground(){
        if(startBackgroundPending||fontDestroyed)return;
        startBackgroundPending=true;final int version=startBackgroundVersion;
        // Keep the attached texture while the host encodes its PNG. Reuse the
        // existing worker; one in flight and, if superseded, only the latest next.
        final WmShell.State state=shellState;
        fontWorker.execute(()->{final android.graphics.drawable.Drawable background=WmShell.startBackground(this,state);
            clockHandler.post(()->{
                startBackgroundPending=false;if(fontDestroyed)return;
                if(version!=startBackgroundVersion){if(startMenu!=null)refreshStartBackground();return;}
                if(startMenu!=null){preparedStartBackground=background;refreshStartMenu(background);}
            });
        });
    }
    private void retryFonts(){if(failedFontKey!=null){failedFontKey=null;requestedFontKey=null;}}
    private void queueFont(String regular,String bold,boolean hybrid,String key){
        if(key.equals(requestedFontKey)||fontDestroyed)return;requestedFontKey=key;loadingFontKey=key;
        fontWorker.execute(()->{boolean ok=true;try{if(WmFontFile.isSource(regular))WmFontFile.resolve(this,regular);if(WmFontFile.isSource(bold))WmFontFile.resolve(this,bold);WmFonts.load(this,regular,hybrid,false);WmFonts.load(this,bold,hybrid,true);}catch(java.io.IOException|RuntimeException unavailable){ok=false;}final boolean success=ok;
            clockHandler.post(()->{if(fontDestroyed||!key.equals(requestedFontKey))return;loadingFontKey=null;if(!success)failedFontKey=key;if(resumed)applyShellState(WmShell.state(this));});
        });
    }
    private static String fontPath(String path){return WmFontFile.isSource(path)||WmFonts.DEFAULT_SOURCE.equals(path)||WmFonts.SYSTEM_SOURCE.equals(path)||"fonts/malgun/bold.ttf".equals(path)||"fonts/samsung/samsung-gothic.ttf".equals(path)||"fonts/samsung/ssgd.ttf".equals(path)?path:"fonts/malgun/regular.ttf";}
    private void createClock(){clock=new WmTrayClock(this,regular,titlePaint.color(39));clock.language(shellState.language);clock.setVisibility(shellState.clock?View.VISIBLE:View.GONE);clock.setOnClickListener(v->onClock());clock.setOnLongClickListener(v->{onClockMenu();return true;});title.setClock(clock);}
    private void refreshChrome(){
        titlePaint=new WmBar(this,false,accent);title.restyle(titlePaint);
        footer.restyle(regular,new WmBar(this,true,accent));
        refreshTray();closeButton(closeOk,closeAction);
    }
    private void showStart(){
        dismissPopup();dismissCommandMenu();
        java.util.List<WmShell.Entry> entries=WmShell.menu(this);
        if(entries.isEmpty())entries.add(new WmShell.Entry("today","",text("오늘"),"","","today",false));
        if(preparedStart==null){preparedStartBackground=WmShell.startBackground(this,shellState);preparedStart=new WmStartMenu(this,entries,regular,accent,preparedStartBackground,this::openEntry);}
        else preparedStart.refresh(entries,regular,accent,preparedStartBackground);
        startMenu=preparedStart;
        if(startMenu.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)startMenu.getParent()).removeView(startMenu);
        startMenu.dismissFolders();startMenu.setTextColor(preparedStartBackground instanceof ColorDrawable?Color.BLACK:shellState.startTextColor);startMenu.setBackLabel(text("뒤로"));startMenu.setIconPolicy(shellState.iconPolicy);
        menuOverlay=new FrameLayout(this);menuOverlay.setOnClickListener(v->dismissStart());FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(startMenu.menuWidth(),-2);p.topMargin=WmMetrics.BAR;menuOverlay.addView(startMenu,p);
        menuOverlay.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(startMenu!=null)startMenu.setMaxHeight(shellWindow.client.getHeight());});
        screen.addView(menuOverlay,new FrameLayout.LayoutParams(-1,-1));start.setSelected(true);
    }
    private void dismissStart(){if(menuOverlay!=null)screen.removeView(menuOverlay);menuOverlay=null;startMenu=null;if(start!=null)start.setSelected(false);}
    private void openEntry(WmShell.Entry entry){
        dismissStart();Intent action=null;
        if(entry.command!=null&&!entry.command.isEmpty())action=WmShell.command(entry.command);
        else if(entry.component!=null&&!entry.component.isEmpty()){
            ComponentName component=ComponentName.unflattenFromString(entry.component);if(component==null)return;
            try{ActivityInfo info=getPackageManager().getActivityInfo(component,0);if(!info.exported||!info.enabled||!info.applicationInfo.enabled||info.permission!=null)return;action=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);}catch(android.content.pm.PackageManager.NameNotFoundException removed){return;}
        }
        if(action==null)return;try{WmShell.launchWithoutAnimation(this,action);}catch(RuntimeException absent){if("today".equals(entry.command))try{startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(RuntimeException noHome){}}
    }
    /** Shared WM bubble below the title bar. Anchor and width are VGA pixels.
     * Popups dismiss on outside tap, Back, app pause, or opening Start.
     */
    protected final WmPopup showPopup(String caption,View body,int width,int anchor){
        if(width<=0||width>WmMetrics.WIDTH)throw new IllegalArgumentException("popup width outside VGA surface");
        dismissStart();dismissPopup();dismissCommandMenu();
        WmControls.restyleTree(body,regular,bold,shade(0xff149ca7),shade(0xffc9e7e9));
        int left=WmPopup.leftForAnchor(width,anchor,Math.max(480,screen.getWidth()));
        WmPaint paint=new WmPaint(this,false,WmPaint.Role.POPUP);paint.addView(body,new FrameLayout.LayoutParams(-1,-2));
        popup=new WmPopup(this,regular,caption,anchor-left,shade(0xff15afbc),shade(0xff149ca7),paint);
        popupOverlay=new FrameLayout(this);popupOverlay.setOnClickListener(v->{if(!WmControls.back(popup))dismissPopup();});
        FrameLayout.LayoutParams position=new FrameLayout.LayoutParams(width,-2);position.leftMargin=left;popupOverlay.addView(popup,position);
        FrameLayout.LayoutParams overlay=new FrameLayout.LayoutParams(-1,-1);overlay.topMargin=WmMetrics.BAR;screen.addView(popupOverlay,overlay);
        return popup;
    }
    protected final void dismissPopup(){clockHandler.removeCallbacks(clockPopupTimeout);if(popupOverlay!=null)screen.removeView(popupOverlay);popupOverlay=null;popup=null;statusKind=-1;}
    private String text(String authored){return WmStrings.text(this,shellState.language,authored);}
    private void trayIcon(int kind,String id){WmTraySlot slot=trays[kind];if(slot==null||id.equals(trayIcons[kind]))return;trayIcons[kind]=id;if(kind==0)slot.connectionIcon(id);else slot.image.setImageDrawable(WmIcons.load(this,id));}
    private void refreshTray(){
        if(trays[0]==null)return;
        WmDevice.Network network=WmDevice.network(this);trays[0].connectionState(network);trays[0].setContentDescription(text(network==null?"연결 안 됨 — 연결 메뉴":network.connecting?"연결 중 — 연결 메뉴":"연결됨 — 연결 메뉴"));
        android.media.AudioManager audio=(android.media.AudioManager)getSystemService(AUDIO_SERVICE);int mode=audio==null?android.media.AudioManager.RINGER_MODE_NORMAL:audio.getRingerMode();
        trayIcon(1,mode==android.media.AudioManager.RINGER_MODE_NORMAL?"volume":mode==android.media.AudioManager.RINGER_MODE_VIBRATE?"volume_vibrate":"volume_muted");trays[1].setContentDescription(text("볼륨 메뉴")+" — "+text(mode==android.media.AudioManager.RINGER_MODE_NORMAL?"켜기":mode==android.media.AudioManager.RINGER_MODE_VIBRATE?"진동":"끄기"));
        WmDevice.Phone phone=WmDevice.phone(this);trayIcon(3,phone.icon());trays[3].setContentDescription(text("전화 메뉴")+" — "+text(phone.caption()));
    }
    /** Same live popup body as the host; apps may override navigation explicitly. */
    protected void onTray(final int kind,int anchor){
        if(statusKind==kind&&popup!=null){dismissPopup();return;}
        WmStatusPanel panel=new WmStatusPanel(this,regular,kind,new WmStatusPanel.Host(){
            private WmVolumeFeedback feedback;
            public void launch(Intent intent){dismissPopup();launchSystem(intent);}
            public void changed(){refreshTray();}
            public void backlight(){launch(new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS));}
            public void hide(){dismissPopup();}
            public void settings(){launch(new Intent(kind==0?android.provider.Settings.ACTION_WIRELESS_SETTINGS:kind==1?android.provider.Settings.ACTION_SOUND_SETTINGS:kind==2?android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS:android.provider.Settings.ACTION_NETWORK_OPERATOR_SETTINGS));}
            public String text(String authored){return WmApplet.this.text(authored);}
            public int color(int original){return shade(original);}
            public void attached(){if(kind==1&&feedback==null)feedback=new WmVolumeFeedback(WmApplet.this);}
            public void feedback(boolean ring,int step){if(feedback!=null)feedback.play(ring,step);}
            public void release(){if(feedback!=null){feedback.release();feedback=null;}}
        });
        showPopup(text(kind==0?"연결":kind==1?"볼륨":kind==2?"전원":"전화"),panel,kind==1?154:444,anchor);statusKind=kind;statusAnchor=anchor;
    }
    /** Android-owned settings retain their native UI; missing activities fall back. */
    protected void launchSystem(Intent intent){try{WmShell.launchWithoutAnimation(this,intent);}catch(RuntimeException unavailable){try{startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));overridePendingTransition(0,0);}catch(RuntimeException absent){}}}
    /** X for ordinary applets; OK for a property sheet. A null action finishes. */
    protected final void closeButton(boolean ok,Runnable action){closeOk=ok;closeAction=action;if(close==null)return;boolean inputOk=ok||WmInputDialog.find(shellWindow.client)!=null;close=title.closeButton(inputOk);close.setContentDescription(text(inputOk?"확인":"닫기"));}
    protected final void analogClock(boolean analog){analogClock=analog;clock.refresh(new java.util.Date(),analog);}
    protected final int shade(int original){if(WmColor.isGray(accent))return WmColor.gray(original);float[] source=new float[3],theme=new float[3];Color.colorToHSV(original,source);Color.colorToHSV(accent,theme);source[0]=theme[0];return Color.HSVToColor(original>>>24,source);}
    protected final void caption(String value){caption.setText(value);}
    protected final TextView label(String value){return WmControls.text(this,regular,value);}
    protected final TextView entry(String value,Runnable action){TextView t=label(value);t.setPadding(WmMetrics.INSET,0,WmMetrics.INSET,0);WmControls.entry(t,shade(0xff149ca7));t.setSoundEffectsEnabled(false);t.setOnClickListener(v->action.run());return t;}
    protected final TextView tab(String value,boolean selected,Runnable action){TextView t=label(value);t.setGravity(Gravity.CENTER);t.setPadding(16,0,16,0);t.setLayoutParams(new LinearLayout.LayoutParams(WmControls.tabWidth(t),WmMetrics.TAB));tabSelected(t,selected);t.setOnClickListener(v->action.run());return t;}
    protected final void tabSelected(TextView tab,boolean selected){WmControls.tab(tab,selected,shade(0xffc9e7e9));}
    protected final TextView softKey(String value,boolean right,Runnable action){TextView t=label(value==null?"":value);footer.styleKey(t);t.setEnabled(action!=null);if(action!=null)t.setOnClickListener(v->action.run());t.setLayoutParams(new LinearLayout.LayoutParams(WmCommandBar.KEY_WIDTH,WmMetrics.BAR));return t;}
    protected final void softKeys(String left,Runnable leftAction,String right,Runnable rightAction){dismissCommandMenu();footer.setCommands(left,leftAction,right,rightAction);}
    /** Menu keys open on DOWN; ordinary softKeys continue to activate on UP. */
    protected final void softKeyMenu(boolean right,String caption,Runnable action){footer.setMenu(right,caption,action);}
    /** App-authored localized choices; selected=-1 means no radio marker. */
    protected final WmCommandMenu showCommandMenu(boolean right,String[] labels,int selected,WmRadioMenu.Selection action){
        dismissStart();dismissPopup();dismissCommandMenu();
        commandMenu=new WmCommandMenu(this,footer,right,regular,labels,selected,text(" 선택됨"),shade(0xff149ca7),action,()->commandMenu=null);
        screen.addView(commandMenu,new FrameLayout.LayoutParams(-1,-1));return commandMenu;
    }
    /** Native-style hierarchy; callbacks receive leaf command IDs, not row indices. */
    protected final WmCommandMenu showCommandMenu(boolean right,WmMenuItem[] items,WmRadioMenu.Selection action){
        dismissStart();dismissPopup();dismissCommandMenu();
        commandMenu=new WmCommandMenu(this,footer,right,regular,items,text(" 선택됨"),shade(0xff149ca7),action,()->commandMenu=null);
        screen.addView(commandMenu,new FrameLayout.LayoutParams(-1,-1));return commandMenu;
    }
    protected final void dismissCommandMenu(){if(commandMenu!=null){WmCommandMenu old=commandMenu;commandMenu=null;old.dismiss();}}
    /** Shared physical-to-logical window surface for app-owned overlays and insets. */
    protected final WmViewport viewport(){return viewport;}
}
