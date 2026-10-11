package me.ancal.wmdroid.sdk;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;

/** Export this provider with the discovery action and VERSION metadata=1.
 * Public rows only: callers other than the launcher may query an exported provider.
 * No module view code executes in the host: custom content uses standard RemoteViews layouts.
 * Row actions must name an exported activity in your package.
 */
public abstract class TodayProvider extends ContentProvider {
    private static final java.util.concurrent.ExecutorService RESOURCE_WORKER=java.util.concurrent.Executors.newSingleThreadExecutor();
    private android.database.ContentObserver resourceObserver;
    private volatile boolean resourcePending;
    private boolean resourceAgain,resourceStopped;
    private String resourceRevision;
    /** Queries stay cached and prompt; a completed pack change invalidates public rows. */
    private synchronized void queueResources(){
        if(resourceStopped||WmShell.PACKAGE.equals(getContext().getPackageName()))return;
        if(resourceObserver==null){
            resourceObserver=new android.database.ContentObserver(new android.os.Handler(android.os.Looper.getMainLooper())){
                @Override public void onChange(boolean self){queueResources();}
            };
            try{getContext().getContentResolver().registerContentObserver(WmShell.STATE,false,resourceObserver);}
            catch(RuntimeException unavailable){resourceObserver=null;}
        }
        if(resourcePending){resourceAgain=true;return;}resourcePending=true;
        RESOURCE_WORKER.execute(()->{
            try{WmResources.syncFromShell(getContext());}catch(java.io.IOException unavailable){}
            String revision=WmResources.revision(getContext());
            boolean notify;
            synchronized(TodayProvider.this){
                resourcePending=false;notify=!resourceStopped&&!revision.equals(resourceRevision);resourceRevision=revision;
                if(resourceAgain){resourceAgain=false;queueResources();}
            }
            if(notify)changed();
        });
    }
    protected abstract void fillRows(MatrixCursor rows,String language);
    /** Opt in with CUSTOM_VIEWS metadata and override for a stable row ID.
     * Options are a fresh Bundle: width=480 logical pixels, pixel_width=480..1440,
     * language=normalized packaged catalog tag (lowercase, underscores become hyphens),
     * with nearest packaged parent then ko fallback; accent/text_color ARGB ints, font asset path String,
     * font_bold source String (default empty), hybrid boolean. font defaults to
     * fonts/malgun/regular.ttf; font/font_bold do not grant file access. Other defaults:
     * Korean, teal accent, black text, hybrid.
     * Return null to retain the ordinary v1 text row. Only your package's resources
     * may back the RemoteViews. Queries/calls should return cached public data promptly.
     */
    protected TodayContract.Widget createWidget(String id,Bundle options){return null;}
    protected final void changed(){getContext().getContentResolver().notifyChange(TodayContract.rows(getAuthority()),null);}
    protected abstract String getAuthority();
    @Override public boolean onCreate(){queueResources();return true;}
    @Override public void shutdown(){
        synchronized(this){resourceStopped=true;resourceAgain=false;if(resourceObserver!=null)getContext().getContentResolver().unregisterContentObserver(resourceObserver);resourceObserver=null;}
        super.shutdown();
    }
    @Override public final Bundle call(String method,String arg,Bundle extras){
        if(!TodayContract.VIEW_METHOD.equals(method))throw new IllegalArgumentException("Unsupported Today call");
        if(arg==null||arg.isEmpty()||arg.length()>TodayContract.MAX_TEXT)throw new IllegalArgumentException("Invalid Today row ID");
        queueResources();
        Bundle supplied=extras==null?new Bundle():new Bundle(extras),options=new Bundle();
        options.putInt("width",TodayContract.WIDTH);
        options.putInt("pixel_width",Math.max(480,Math.min(1440,supplied.getInt("pixel_width",480))));
        options.putString("language",WmStrings.language(getContext(),supplied.getString("language")));
        options.putInt("accent",supplied.getInt("accent",0xff149ca7));
        options.putInt("text_color",supplied.getInt("text_color",0xff000000));
        String font=supplied.getString("font");options.putString("font",font==null?"fonts/malgun/regular.ttf":font);
        options.putString("font_bold",supplied.getString("font_bold",""));
        options.putBoolean("hybrid",supplied.getBoolean("hybrid",true));
        TodayContract.Widget widget=createWidget(arg,options);Bundle result=new Bundle();
        if(widget!=null){
            if(!getContext().getPackageName().equals(widget.view.getPackage()))throw new IllegalArgumentException("Widget resources must belong to provider package");
            result.putParcelable(TodayContract.VIEW_KEY,widget.view);result.putInt(TodayContract.HEIGHT_KEY,widget.height);
        }
        return result;
    }
    @Override public final Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){
        if(!TodayContract.rows(getAuthority()).equals(uri.buildUpon().clearQuery().build())||selection!=null||args!=null||order!=null)throw new IllegalArgumentException("Unsupported Today query");
        queueResources();
        MatrixCursor rows=new MatrixCursor(TodayContract.COLUMNS);fillRows(rows,WmStrings.language(getContext(),uri.getQueryParameter("language")));rows.setNotificationUri(getContext().getContentResolver(),uri);return rows;
    }
    @Override public final String getType(Uri uri){return "vnd.android.cursor.dir/vnd.me.ancal.wmdroid.module.v1";}
    @Override public final Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException("Read-only");}
    @Override public final int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException("Read-only");}
    @Override public final int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException("Read-only");}
}
