---
title: "WmScrollView"
slug: en/api/wmscrollview
---
[API](/en/api/)

```java
public class WmScrollView extends FrameLayout
```

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmScrollView.java`

## Constructors

```java
public WmScrollView(Context context,View contents);
public WmScrollView(Context context,View contents,int contentHeight);
public WmScrollView(Context context,View contents,int contentHeight,int face);
```

Construct and modify on the UI thread. `contents` must be a non-null, unattached View. Construction attaches it to an internal Android ScrollView; do not reparent it. The owner manages content/View lifetime and data.

The default `contentHeight` is WRAP_CONTENT. Other overloads use the supplied Android LayoutParams height. Positive values also set contents' minimumHeight. Content width is MATCH_PARENT. fillViewport is false, so short content is not expanded to viewport height. Android scrollbars and overscroll effects are disabled.

Default `face` is `0xffc9e7e9`, the control face color for [WmScrollBar](/en/api/wmscrollbar/). The right scrollbar is `26` SDK logical pixels wide. Its space is reserved only for overflowing content.

Default scrollbar line step is `44`. A WmRadioMenu with choice keyboard enabled uses its first row's LayoutParams height for line steps and complete rows fitting within the smaller of menu and viewport heights, minus top/bottom padding, for page steps. Two or more rows move one fewer row to overlap one row; one or fewer moves that row count. Distance is moved rows×the first row’s actual height. An empty menu has page step `0`.

## onMeasure

```java
protected void onMeasure(int width,int height);
```

Arguments are Android MeasureSpecs. After superclass measurement, makes the scrollbar VISIBLE and sets the internal ScrollView's right margin to `26` when measured viewport height is positive and content height exceeds it. Otherwise uses GONE and margin `0`, and stops active scrollbar behavior. Remeasures when this state changes, so content width is decided before first draw.

Scrolling and content/viewport layout changes schedule scrollbar updates. On API18 and later, an update during layout is rescheduled. API14–17 do not call `isInLayout()`.

## scrollToPosition

```java
public void scrollToPosition(int y);
```

Stops flinging and stores the requested `y`, clamping negative values to `0`. Applies immediately when viewport height is positive and neither this container, the internal ScrollView, nor contents has a layout request. Otherwise waits for UI-queue updates and layout. A new request replaces the previous pending request.

Android ScrollView clamps the maximum offset against content/viewport geometry when applied. A pending request can exceed the current maximum offset. A touch DOWN while enabled, or detachment, clears the pending request.

## getScrollPosition, getRequestedScrollPosition

```java
public int getScrollPosition();
public int getRequestedScrollPosition();
```

Both values use logical pixels. `getScrollPosition()` returns the internal ScrollView's actual Y offset. `getRequestedScrollPosition()` returns the pending request when present, or the actual offset otherwise. Use the requested value to retain restoration/font-change positions before layout.

## setContentHeight

```java
public void setContentHeight(int height);
```

`height <= 0` throws `IllegalArgumentException`. Applies a positive height to contents' minimumHeight and LayoutParams.height, then schedules a scrollbar update. Use for Canvas content whose height depends on font measurement. Layout and Android ScrollView reconcile the current offset with the new range.

## stopFling

```java
public void stopFling();
```

Directly sends DOWN→CANCEL to the internal ScrollView's touch handler and CANCEL to its interception handler to stop flinging. Does not dispatch events to contents or invoke content click/selection callbacks. Does not clear a pending position request.

## setEnabled, dispatchKeyEvent

```java
public void setEnabled(boolean enabled);
public boolean dispatchKeyEvent(android.view.KeyEvent event);
```

Propagates enabled state to the internal ScrollView and scrollbar, without changing contents' enabled value. Setting false clears thumb gesture state and stops flinging. An enabled→disabled transition also sends CANCEL to the active child gesture. Pending position requests remain.

`dispatchKeyEvent` returns false when disabled; otherwise returns the superclass result.

## dispatchTouchEvent

```java
public boolean dispatchTouchEvent(MotionEvent event);
```

Returns false when disabled. An enabled DOWN clears the pending position request and tests the expanded [WmScrollBar.hitThumb](/en/api/wmscrollbar/) area. For a thumb gesture, converts coordinates to scrollbar-local coordinates, forwards this and subsequent events, and returns the scrollbar's result. UP or CANCEL ends thumb routing.

Other events go to the superclass for ordinary finger scrolling and content touches. The expanded thumb area can overlap content to the left of the visible scrollbar.

## onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Removes scheduled scrollbar updates and clears pending position requests/thumb routing. Stops scrollbar behavior, then calls the superclass. Does not remove contents or explicitly reset the actual offset. The owner saves positions needed for reattachment restoration.
