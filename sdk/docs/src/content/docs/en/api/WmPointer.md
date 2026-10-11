---
title: "WmPointer"
slug: en/api/wmpointer
---
[API](/en/api/)

Finger-mouse View for relative motion and tap clicks within a logical surface.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmPointer.java`

## Declaration

```text
public final class WmPointer extends View
public WmPointer(Context context,FrameLayout target,Runnable disable);
public float pointerX();
public float pointerY();
protected void onDraw(Canvas canvas);
public boolean onTouchEvent(MotionEvent event);
public boolean performClick();
protected void onDetachedFromWindow();
```

target is the receiving FrameLayout; disable runs after a long press. Align target coordinates with pointer View coordinates. Construct and handle events on the main thread.

pointerX and pointerY use logical pixels and start at (240,320). Movement is clamped inside the View. On MOVE, start-relative `abs(x−startX)+abs(y−startY)>8` cancels tapping and long pressing. Units are logical pixels; this is not accumulated travel. Once exceeded, cancellation lasts for the gesture even if the pointer returns to its start. The timeout is ViewConfiguration.getLongPressTimeout().

`performClick()` sends DOWN and UP to target, with a 20 ms event-time difference. Multiple pointers cancel the complete gesture. Detachment cancels pending long presses. Input does not reach apps outside this surface.
