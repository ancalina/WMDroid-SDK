package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.format.DateFormat;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** shell32 203FC clock in VGA coordinates. The parent scales the complete title bar.
 * Hand tables preserve the ROM's integer arithmetic, including its negative-angle
 * sine behavior; see design/tray-clock-hands.json. This view owns no timer or popup.
 */
public final class WmTrayClock extends View {
    private static final int HEIGHT = 52;
    private static final int[] MINUTE_X = {0,6,11,13,11,6,0,-6,-11,-13,-11,-6};
    private static final int[] MINUTE_Y = {-21,-14,-7,0,6,11,13,11,6,0,-6,-11};
    private static final int[] HOUR_X = {0,4,7,8,7,4,0,-4,-7,-8,-7,-4,0,4,7,8,7,4,0,-4,-7,-8,-7,-4};
    private static final int[] HOUR_Y = {-13,-8,-4,0,4,7,8,7,4,0,-4,-7,-8,-7,-4,0,4,7,8,7,4,0,-4,-7};

    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path face = new Path();
    private final int pressedTextColor;
    private String time = "";
    private boolean analog;
    private String language="ko";
    private int naturalWidth, minuteIndex, hour;

    public WmTrayClock(Context context, Typeface regular, int pressedTextColor) {
        super(context);
        this.pressedTextColor = pressedTextColor;
        textPaint.setTypeface(regular);
        textPaint.setTextSize(24);
        facePaint.setColor(Color.WHITE);
        // Same manually authored geometry as design/icons/clock-face.svg.
        // Smooth 17/14-radius circles preserve the ring silhouette, not its 1bpp steps.
        face.setFillType(Path.FillType.EVEN_ODD);
        face.addCircle(17, 17, 17, Path.Direction.CW);
        face.addCircle(17, 17, 14, Path.Direction.CW);
        // ROM requests SHGetSysColor(0x40000005). White is provisional: its runtime
        // RGB and GDI's exact pen rasterization have not been recovered.
        handPaint.setColor(Color.WHITE);
        handPaint.setStrokeWidth(2);
        handPaint.setStrokeCap(Paint.Cap.BUTT);
        setFocusable(true);
        refresh(new Date(), false);
    }

    /** The host supplies the timestamp/mode in its next refresh. */
    public void language(String language){this.language=WmStrings.language(getContext(),language);}
    public void refresh(Date now, boolean analog) {
        this.analog = analog;
        String pattern = DateFormat.is24HourFormat(getContext()) ? "HH:mm" : "h:mm";
        // Both original catalogs used Korean numeric formatting; regional
        // catalogs may supply their own locale without changing those pixels.
        Locale locale="ko".equals(language)||"en".equals(language)?Locale.KOREAN:WmStrings.locale(language);
        time = new SimpleDateFormat(pattern, locale).format(now);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        minuteIndex = calendar.get(Calendar.MINUTE) / 5;
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        int width = analog ? 40 : (int)Math.ceil(me.ancal.wmdroid.sdk.WmFonts.measure(textPaint,time)) + 6;
        if (naturalWidth != width) {
            naturalWidth = width;
            requestLayout();
        }
        setContentDescription(WmStrings.text(getContext(),language,"시계 메뉴, ") + time + ", " + WmStrings.text(getContext(),language,analog?"아날로그":"디지털"));
        invalidate();
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(resolveSize(naturalWidth, widthMeasureSpec),
                resolveSize(HEIGHT, heightMeasureSpec));
    }

    @Override protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (analog) {
            int saved = canvas.save();
            canvas.translate((getWidth()-34)/2f,9);
            canvas.drawPath(face, facePaint);
            canvas.restoreToCount(saved);
            float cx=getWidth()/2f;canvas.drawLine(cx,26,cx+MINUTE_X[minuteIndex],26+MINUTE_Y[minuteIndex],handPaint);
            canvas.drawLine(cx,26,cx+HOUR_X[hour],26+HOUR_Y[hour],handPaint);
        } else {
            textPaint.setColor(isPressed() || isSelected() ? pressedTextColor : Color.WHITE);
            float baseline=me.ancal.wmdroid.sdk.WmFonts.centerBaseline(textPaint,0,HEIGHT,time);
            me.ancal.wmdroid.sdk.WmFonts.draw(canvas,time,(getWidth()-me.ancal.wmdroid.sdk.WmFonts.measure(textPaint,time))/2f,baseline,textPaint);
        }
    }
}
