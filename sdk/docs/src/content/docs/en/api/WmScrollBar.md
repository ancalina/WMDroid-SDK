---
title: "WmScrollBar"
slug: en/api/wmscrollbar
---
[API](/en/api/)

```java
public class WmScrollBar extends View implements WmControls.RestyleListener
```

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmScrollBar.java`

## Constructor

```java
public WmScrollBar(Context context,Host host,int face);
```

Construct and modify on the UI thread. `host` must be non-null; construction does not validate it. `face` colors arrow/thumb faces. The parent supplies size; standard width is `26` SDK logical pixels. Allows keyboard focus. The accessibility description is bound in English; API16 and later set importantForAccessibility to YES.

Touch DOWN requires a parent. Attach the View before directly delivering DOWN. Construction does not calculate overflow visibility; call `refresh()` after layout.

## Host

```java
public interface Host
int extent();
int range();
int offset();
int lineStep();
void scrollTo(int offset);
void scrollBy(int delta);
public void stopFling();
```

All values and arguments use the same SDK logical-pixel coordinates.

| API | Contract |
| --- | --- |
| extent | Visible content height. |
| range | Total content height. |
| offset | Current Y offset. |
| lineStep | One line movement. |
| scrollTo | Apply an absolute offset. |
| scrollBy | Apply a delta to the current offset. |
| stopFling | Stop active content flinging. |

The owner supplies consistent heights/offsets and a positive lineStep. The bar does not store content position; subsequent offset queries must reflect callback results. Host calls run synchronously on the UI thread; exceptions propagate. DOWN and keyboard control call Host.stopFling before movement.

## refresh

```java
public void refresh();
```

Sets VISIBLE for `extent > 0 && range > extent`, otherwise GONE, and invalidates drawing. A transition to GONE also calls `stop()`. Does not change parent gutters, width, or content layout. [WmScrollView](/en/api/wmscrollview/) or the owner manages that layout.

## hitThumb

```java
public boolean hitThumb(float x,float y);
```

Returns whether bar-local `x,y` lie in the expanded thumb area. Requires enabled state and its own VISIBLE visibility. The `x` interval is `[-24,width)`. The `y` coordinate must lie between arrow buttons and within the thumb area extended by `24` above/below.

To use the area outside the bar, the parent must test DOWN and forward coordinate-adjusted events. The painted width remains unchanged.

## onDraw

```java
protected void onDraw(Canvas canvas);
```

Uses a white track, black `2`-pixel frames, and face-colored controls. Pressed arrow faces are black; arrow ink is normally black, pressed white, and disabled SHADOW.

| Bar height | Painting |
| --- | --- |
| Below `50` | No painting. |
| `50..79` | Overlapping half-height arrows; no ordinary thumb face. |
| At least `80` | Arrow height `40`, track, thumb, and grips. |

Logical thumb height is `min(trackHeight,max(26,round(trackHeight*extent/range)))`; range `0` uses trackHeight. Disabled state omits the thumb face/grips. The painted thumb frame extends `2` above/below logical thumb bounds. Omitting paint below height `50` does not disable input.

## pageStep

```java
protected int pageStep();
```

Defaults to `max(host.lineStep(),extent−host.lineStep())`. Track taps, Page Up/Down, and accessibility page actions use it. Override to supply content-specific page geometry.

## onTouchEvent

```java
public boolean onTouchEvent(MotionEvent event);
```

When disabled, calls `stop()` and returns false. Enabled DOWN cancels existing bar behavior, records the first pointer/current offset, stops Host flinging, and disallows parent interception. Arrows move one line immediately; a changed offset starts repetition after `100ms`, then every `100ms`. Repetition ends at range boundaries.

Thumb DOWN uses relative MOVE distance to calculate an absolute offset. Track DOWN does not immediately page. UP moves by pageStep above/below the current thumb. Vertical movement past touchSlop converts a track gesture to thumb dragging, subtracting slop distance. Horizontal-only movement past slop cancels it.

Dragging adds `round(deltaY*max(0,range−extent)/thumbTravel)` to the starting offset and clamps to `0..max(0,range−extent)`. Uses neither smooth scrolling nor row snapping. Nonpositive thumbTravel produces no movement.

Arrows cancel on leaving their button area or the horizontal `24`-pixel extension. Horizontal departure alone does not cancel thumb dragging. When the tracked pointer lifts, dragging continues with another pointer; other operations cancel. A missing pointer or CANCEL cancels. UP calls stop, then performClick, and returns true.

## stop, setEnabled, onDetachedFromWindow

```java
public void stop();
public void setEnabled(boolean enabled);
protected void onDetachedFromWindow();
```

`stop()` clears repeat callbacks, pressed state, and the tracked pointer; releases parent interception and invalidates drawing. It does not change content offset or call Host.stopFling.

`setEnabled(false)` and detachment call stop. Self, ancestor, and window hiding also stop through the hooks below. Showing again does not resume canceled repetition or dragging. A remaining UP from the pre-hide gesture does not page, but the existing UP path can still call performClick.

## onVisibilityChanged, onWindowVisibilityChanged

```java
protected void onVisibilityChanged(View changedView,int visibility);
protected void onWindowVisibilityChanged(int visibility);
```

Calls each superclass hook, then `stop()` when `visibility != VISIBLE`. Self/ancestor changes to INVISIBLE or GONE and non-VISIBLE window changes cancel scheduled repetition, pressed state, and the tracked pointer. Becoming VISIBLE does not restore the previous gesture.

## onKeyDown, onKeyUp

```java
public boolean onKeyDown(int key,KeyEvent event);
public boolean onKeyUp(int key,KeyEvent event);
```

Handles supported keys only when enabled and without modifiers; other events delegate to the superclass. DOWN stops Host flinging, moves as below, and returns true even at a range boundary.

| Keys | Movement |
| --- | --- |
| Home / End | `0` / `max(0,range−extent)` |
| Page Up / Down | `−pageStep` / `+pageStep` |
| Up, Left / Down, Right | `−lineStep` / `+lineStep` |

Consumes enabled, unmodified UP for supported keys. Line/page steps use a long intermediate sum to clamp offset to the range, then call Host.scrollBy. No callback runs when already at the target offset.

## performClick

```java
public boolean performClick();
```

Runs the superclass click callback and always returns true. Does not itself scroll. Direct calls do not separately check enabled state.

## Accessibility

```java
public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info);
public boolean performAccessibilityAction(int action,Bundle args);
```

Marks the node scrollable and registers forward/backward page actions. Disabled actions return false. Forward/backward requests move by pageStep and return true even at a range boundary. This path does not call Host.stopFling. Other actions delegate to the superclass on API16 and later, or return false on earlier versions.

## onWmRestyle

```java
public void onWmRestyle(android.graphics.Typeface regular,android.graphics.Typeface bold,int selection,int tabColor);
```

Changes face color to `tabColor` and invalidates drawing. Does not change `regular`, `bold`, `selection`, or Host geometry.

![Android API 14 render](/WmScrollBar.png)
