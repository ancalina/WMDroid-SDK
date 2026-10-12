---
title: "WmViewport"
slug: en/api/wmviewport
---
[API](/en/api/)

Places one logical window on a physical screen with uniform scaling.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmViewport.java`

```text
public class WmViewport extends ViewGroup
```

## Constructor, setContent, content, addView

```text
public final void setContent(View content);
public final View content();
public WmViewport(Context context);
public void addView(View child,int index,ViewGroup.LayoutParams params);
```

Null content throws `IllegalArgumentException`. Replaces the previous child and applies a smoothing layer. The same View remains unchanged. content returns the current child or null. Adding a second child throws `IllegalStateException`. Detach Views from other parents first. All View changes use the main thread.

## scalePercent, setScalePercent, getLogicalWidth, getLogicalHeight, getScale

```text
public final int getLogicalWidth();
public final void setScalePercent(int value);
public final float getScale();
public static int scalePercent(int value);
public final int getLogicalHeight();
```

Percent is clamped to 50–100 and defaults to 100. Logical width is round(480*100/percent). Physical scale is physical width/logical width; getScale returns 0 before physical width exists. Height uses the same scale and insets. getLogicalHeight returns the measured child height.

## setTopInset, setBottomInset, getUsableHeight

```text
public final void setTopInset(int pixels);
public final int getUsableHeight();
public final void setBottomInset(int pixels);
```

Insets use physical pixels; negative values become 0. usableHeight is height minus both insets, with a minimum of 0. Do not apply insets again during coordinate conversion.

## applyDisplay, screenMode

```text
public final void applyDisplay(Activity owner,int scale,String mode);
public static String screenMode(String value);
```

Applies scale and display policy to the owner window. Modes are full (complete screen), black (black top safe area), and status (Android status bar). Other strings select full. Keyboard policy applies after observeImeInsets.

## observeImeInsets

```text
public final void observeImeInsets();
```

API 30 or later uses ADJUST_NOTHING and IME insets. Earlier versions use ADJUST_RESIZE and visible-frame differences. The legacy path may not detect floating-keyboard height accurately. Detachment clears the observer and bottom inset.

## setTouchSoundLevel, getTouchSoundLevel

```text
public final void setTouchSoundLevel(int level);
public final int getTouchSoundLevel();
```

0=off, 1=soft, 2=loud. Out-of-range values throw `IllegalArgumentException`. Sound plays only for handled DOWN events. Attachment creates the player; off or detachment releases it.

## onViewportBoundsChanged, lifecycle, drawing, touch

```text
public boolean dispatchTouchEvent(MotionEvent event);
protected void onAttachedToWindow();
protected void onDetachedFromWindow();
protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight);
protected void onMeasure(int widthSpec,int heightSpec);
protected void dispatchDraw(Canvas canvas);
protected void onLayout(boolean changed,int left,int top,int right,int bottom);
protected void onViewportBoundsChanged();
```

Protected hook for size, scale, and inset changes. Cancel subclass coordinate-dependent feedback here.
