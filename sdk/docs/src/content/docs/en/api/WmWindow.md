---
title: "WmWindow"
slug: en/api/wmwindow
---
[API](/en/api/)

```java
public final class WmWindow extends LinearLayout
public final FrameLayout client;
```

Lays out title, body, bottom commands, and popups on one logical surface.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmWindow.java`

## Constructor · client

```java
public WmWindow(Context context);
```

Construct and modify on the UI thread. Starts as a vertical, white, clickable container. Add body Views to `client`.

The title and command bar each have height `52` SDK logical pixels and MATCH_PARENT width. `client` has MATCH_PARENT width, height `0`, and weight `1` to use the remaining height. The default layout adds no gap between title, body, and commands. The parent determines available window size. Do not remove or reparent `client`.

## title

```java
public void title(View view);
```

Validates `view`, then removes the current title and inserts it as the first child. Null, the window itself, or an ancestor View throws `IllegalArgumentException`. A parentless ancestor is also rejected. An attached View is allowed only when it is the current title and its parent is this window; otherwise throws `IllegalStateException`. Validation failure retains the existing title. Rebinding the current title is allowed.

Rollback is not guaranteed for every failure after validation. The supplied View and owner implement its logo, clock, status, and close behavior.

## commands

```java
public void commands(View view);
```

Applies the same argument validation as `title` to the current command slot. For a [WmCommandBar](/en/api/wmcommandbar/), calls `inputMethods(client)` before removing the current command View. If custom center children cause that call to fail, the existing command View remains.

After validation and input-method binding, removes the current command View and attaches the supplied View below the body and before popupLayer. Rebinding the current command View is allowed. The input owner is set to client. Rollback is not guaranteed for every later failure. The owner binds softkey/menu behavior for an ordinary View.

## popupLayer

```java
public FrameLayout popupLayer();
```

Creates the internal FrameLayout on first call and returns the same reference thereafter. It follows the command bar in child order but overlays the complete window. Popup child coordinates start at the window's top-left, including the title, rather than at the client top.

The owner manages popup child placement, replacement, removal, background touches, and dismissal. This method does not fill a backdrop, block touches, or register modal state. Do not remove or reparent the returned layer.

## onMeasure · onLayout

```java
protected void onMeasure(int width,int height);
protected void onLayout(boolean changed,int l,int t,int r,int b);
```

`width` and `height` are Android MeasureSpecs. After superclass measurement of the vertical layout, remeasures popupLayer with EXACTLY the window's measured width and height. Popup content does not enlarge the window.

After ordinary child layout, `onLayout` overlays popupLayer at `(0,0,r-l,b-t)`. These are window-local coordinates, not external screen coordinates.

## ownerCommandsAllowed

```java
public boolean ownerCommandsAllowed();
```

Returns `!WmControls.blocksOwnerCommands(client)`. Returns false when a visible [ModalOwner](/en/api/wmcontrols/) in the client tree requests command blocking. Children in popupLayer alone do not change this result.

## dispatchTouchEvent

```java
public boolean dispatchTouchEvent(MotionEvent event);
```

Decides command-bar gesture blocking only on DOWN. Consumes the subsequent gesture when all of these conditions hold.

- `ownerCommandsAllowed()` is false.
- The DOWN coordinate is inside the current command bar's `[left,right)×[top,bottom)` bounds.
- It is outside the current WmCommandBar's visible input-method button.

The input-method exception refreshes SIP before testing its bounds in window coordinates. A blocked gesture remains consumed until UP or CANCEL even if it leaves the bounds or modal state changes; this path returns true. Other events go to the superclass. This method does not directly block title, client, or popup touches.

## onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Clears the retained gesture-blocking state to false, then calls the superclass. Does not remove client, title, commands, or popupLayer children. Each control and its owner manage child observers, work, and popup lifetime.
