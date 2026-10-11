---
title: "WmPaint"
slug: en/api/wmpaint
---
[API](/en/api/)

Container that reveals a client area from top to bottom using the old-device behavior settings.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmPaint.java`

```text
public class WmPaint extends FrameLayout
```

## Options

```text
public static final class Options
public boolean enabled = true;
public boolean scan = true;
public boolean repaint = false;
public int delay = 120;
public int lines = 64;
public int scale = 1000;
```

## Role

```text
public enum Role { MENU, POPUP, FORM, GRID, CALENDAR }
```

## Constructor · readOptions

```text
public WmPaint(Context context,boolean delayAlreadyServed);
public WmPaint(Context context,boolean delayAlreadyServed,Role role);
protected Options readOptions();
```

Create and change the container on the UI thread. The default role is `FORM`. Do not pass a null role. `delayAlreadyServed` skips the initial wait on attachment. `readOptions()` reads the current `WmShell.State`. Subclasses can override this hook to supply Options.

## Options · Role

```text
public static Options from(WmShell.State state);
```

`enabled` enables the effect. `scan` reveals from top to bottom. `repaint` adds one blank interval after completion. Defaults: delay 120 ms, lines 64 logical pixels, scale 1000. Scale 1000 means 1×. `Options.from(state)` copies current settings into new Options.

Execution clamps raw delay to 0–1500 ms, lines to 8–256, and scale to 250–4000. The initial wait is `round(clamp(delay,0,1500)*clamp(scale,250,4000)/1000)` ms, at most 6000 ms. delayServed makes it 0. Each band's base interval is 16 ms. Role multipliers: MENU 0.5, POPUP 1, FORM 1.25, GRID 1.5, CALENDAR 1.75. The band interval uses both role and scale. MENU uses one quarter of the initial delay. Repaint applies scale to a 60 ms wait and a 40 ms blank interval.

## replay

```text
public void replay();
public void replay(boolean delayServed);
public void replay(View client);
public void replay(View client,boolean delayServed);
```

Calls without a client reveal the complete area. `delayServed=true` skips the initial delay. A client must be a descendant of this container. Null, the container itself, or an outside View throws `IllegalArgumentException`. Only the client area is revealed; the surrounding area remains visible. A detached or hidden container does not start. The effect waits for layout and uses the clipped area. Disabled effects do not delay display.

## isActive · cancel · stopReplay

```text
public boolean isActive();
public static void stopReplay(View root);
public void cancel();
```

`isActive()` reports an active effect. `cancel()` removes scheduled callbacks, clipping, and gesture blocking. `stopReplay(root)` completes active effects in root and its descendants. It still blocks an existing touch until UP or CANCEL.

## Lifecycle · drawing · touch

```text
protected void onAttachedToWindow();
protected void onLayout(boolean changed,int left,int top,int right,int bottom);
protected void onDetachedFromWindow();
protected void onVisibilityChanged(View changedView,int visibility);
protected void onWindowVisibilityChanged(int visibility);
protected void dispatchDraw(Canvas canvas);
public boolean dispatchTouchEvent(MotionEvent event);
```

Attachment starts the effect with the constructor settings. Detachment or hiding cancels scheduled work. Active effects consume touches. Starting an effect during a forwarded gesture sends CANCEL to the child and consumes the remainder. Call the superclass from lifecycle, drawing, and touch overrides to retain this behavior.
