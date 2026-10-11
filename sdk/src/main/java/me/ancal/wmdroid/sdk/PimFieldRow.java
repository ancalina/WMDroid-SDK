package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.FrameLayout;

/** Inactive PIM card geometry measured in the official VGA Tasks and Contacts editors. */
abstract class PimFieldRow extends FrameLayout {
    static final int HEIGHT=39, CLIENT=37, LABEL_RIGHT=148, VALUE_LEFT=155, EDIT_LEFT=157, RIGHT_INSET=10;
    private final Paint underline=new Paint();
    PimFieldRow(Context context){super(context);setWillNotDraw(false);setBackgroundColor(Color.WHITE);underline.setColor(0xff848284);}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.drawRect(EDIT_LEFT,CLIENT,Math.max(EDIT_LEFT,getWidth()-RIGHT_INSET),HEIGHT,underline);}
}
