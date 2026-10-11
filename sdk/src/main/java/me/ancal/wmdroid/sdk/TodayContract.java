package me.ancal.wmdroid.sdk;

import android.net.Uri;
import android.widget.RemoteViews;

/** Read-only Today protocol. Host owns chrome; optional standard RemoteViews supply content. */
public final class TodayContract {
    private TodayContract(){}
    public static final int VERSION=1, MAX_ROWS=8, MAX_TEXT=512;
    public static final String DISCOVER="me.ancal.wmdroid.sdk.TODAY_MODULE";
    public static final String VERSION_META="me.ancal.wmdroid.sdk.VERSION";
    public static final String VIEW_META="me.ancal.wmdroid.sdk.CUSTOM_VIEWS";
    public static final String VIEW_METHOD="today_widget", VIEW_KEY="view", HEIGHT_KEY="height";
    public static final int WIDTH=480, MIN_HEIGHT=32, MAX_HEIGHT=480;
    /** Immutable result envelope; finish configuring the RemoteViews before returning it.
     * Height uses logical VGA pixels, not dp. The host supplies the available physical width.
     */
    public static final class Widget {
        public final RemoteViews view;
        public final int height;
        public Widget(RemoteViews view,int height){
            if(view==null)throw new IllegalArgumentException("Widget view required");
            if(height<MIN_HEIGHT||height>MAX_HEIGHT)throw new IllegalArgumentException("Widget height outside 32..480");
            this.view=view;this.height=height;
        }
    }
    public static final String[] COLUMNS={"id","title","detail","icon","activity"};
    public static Uri rows(String authority){return new Uri.Builder().scheme("content").authority(authority).appendPath("today").appendPath("v1").appendPath("rows").build();}
}
