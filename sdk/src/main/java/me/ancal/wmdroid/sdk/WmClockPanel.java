package me.ancal.wmdroid.sdk;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/** Read-only clock bubble body in the launcher's VGA coordinate space.
 * ROM evidence: shell32/000214FC.c and shellres CLOCK_TOOLTIP.HTM / CLOCK_BATTERY.HTM;
 * aygshell/03855938.c -> 03867774.c -> 03867740.c and design/korean-font-preprocess.json.
 * The Korean title's requested weight 700 is preprocessed to 400. Body CSS has no bold.
 * Host owns the 444px WmPopup, inside-tap consumption and 7000ms timeout.
 * No calendar query or permission delegation: caller supplies its own appointment text.
 */
public class WmClockPanel extends LinearLayout implements WmControls.RestyleListener {
    private String artworkRevision;
    private static final int TEXT_SIZE = 24;
    private String language;
    private final TextView dateAndTime, appointment, appointmentTime;
    private final ImageView battery;
    public WmClockPanel(Context context, Typeface regular, String language) {
        super(context);
        this.language = WmStrings.language(context, language);
        setOrientation(VERTICAL);
        // bubblestyle.css: BODY margin 0.042in 0.042in 0 0.042in, about 8px at 192DPI.
        setPadding(8, 8, 8, 8);

        LinearLayout clock = new LinearLayout(context);
        clock.setGravity(Gravity.TOP);
        // CLOCK_TOOLTIP.HTM: first column 0.17in; top spacer 1em; date cell 2.67em.
        clock.setMinimumHeight(Math.round(TEXT_SIZE * 2.67f));
        LinearLayout imageColumn = new LinearLayout(context);
        imageColumn.setOrientation(VERTICAL);
        View spacer = new View(context);
        imageColumn.addView(spacer, new LayoutParams(1, TEXT_SIZE));
        battery = new ImageView(context);
        battery.setScaleType(ImageView.ScaleType.FIT_CENTER);
        // 1F7D4 supplies black-on-white BITMAP5082..5091 to the HTML body.
        // The matching tray vectors are white, so use their same silhouette in black.
        battery.setColorFilter(Color.BLACK);
        imageColumn.addView(battery, new LayoutParams(32, 32));
        clock.addView(imageColumn, new LayoutParams(33, LayoutParams.WRAP_CONTENT));
        dateAndTime = label(regular, "");
        dateAndTime.setGravity(Gravity.TOP | Gravity.RIGHT);
        clock.addView(dateAndTime, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));
        addView(clock, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        View separator = new View(context);
        separator.setBackgroundColor(0xff808080);
        LayoutParams separatorParams = new LayoutParams(LayoutParams.MATCH_PARENT, 2);
        // HTML hr: border-width 0.011in, vertical margins 0.17em.
        separatorParams.topMargin = 4;
        separatorParams.bottomMargin = 4;
        addView(separator, separatorParams);
        appointment = label(regular, "");
        appointmentTime = label(regular, "");
        appointmentTime.setMinLines(1);
        addView(appointment, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(appointmentTime, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        refreshClock();
        setAppointment(text("일정 연결 필요"), "");
    }

    private TextView label(Typeface regular, String text) {
        TextView view = WmControls.text(getContext(), regular, text);
        view.setText(text);
        view.setTypeface(regular);me.ancal.wmdroid.sdk.WmFonts.attach(view,regular);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, TEXT_SIZE);
        view.setTextColor(Color.BLACK);
        view.setIncludeFontPadding(false);
        view.setGravity(Gravity.TOP | Gravity.LEFT);
        return view;
    }

    protected final String time(long millis) {
        String pattern = DateFormat.is24HourFormat(getContext()) ? "HH:mm" : "a h:mm";
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern(pattern, clockLocale()));
    }

    public final void refreshClock() {
        long now = System.currentTimeMillis();
        String date = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern(text("yyyy년 M월 d일 EEEE"), clockLocale()));
        dateAndTime.setText(date + "\n" + time(now));
        Intent state = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = state == null ? -1 : state.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = state == null ? 0 : state.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
        int percent = level < 0 || scale <= 0 ? -1 : (int) (100L * level / scale);
        boolean external = state != null && state.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        boolean charging = state != null && state.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                == BatteryManager.BATTERY_STATUS_CHARGING;
        battery.setImageDrawable(WmIcons.load(getContext(),batteryIcon(percent, external, charging)));
        artworkRevision=WmResources.revision(getContext());
        battery.setContentDescription(percent < 0 ? text("배터리 상태 확인 필요") : text("배터리 ") + percent + "%");
    }

    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){if(!WmResources.revision(getContext()).equals(artworkRevision))refreshClock();}

    public final void setAppointment(String title, String when) {
        appointment.setText(title);
        appointmentTime.setText(when);
    }

    protected final String text(String authored) { return WmStrings.text(getContext(), language, authored); }
    private Locale clockLocale() {
        // Keep the original default countries while allowing regional catalogs.
        return WmStrings.locale("en".equals(language) ? "en-us" : "ko".equals(language) ? "ko-kr" : language);
    }
    /** Update SDK-authored labels only; appointment content belongs to the caller. */
    public final void language(String value) { language=WmStrings.language(getContext(),value); refreshClock(); }
    /** WmIcons String catalog key, not an Android integer resource ID.
     * shell32 1F7D4 thresholds; AC without charging selects full, including unknown charge. */
    public static String batteryIcon(int percent, boolean external, boolean charging) {
        if(external)return charging?"battery_charge":"battery_full";
        if(percent<0||percent>100)return "battery_full";
        return percent<21?"battery_critical":percent<41?"battery_low":percent<61?"battery_half":percent<81?"battery_medium":"battery_full";
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); refreshClock(); }
}
