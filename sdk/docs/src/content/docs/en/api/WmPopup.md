---
title: "WmPopup"
slug: en/api/wmpopup
---
[API index](/en/api/)

```java
public final class WmPopup extends FrameLayout
```

Places the title and content inside a tailed popup. The caller attaches it to an overlay and manages placement, size, dismissal, and button callbacks. Construct and update on the UI thread. Coordinates and dimensions use logical pixels.

## Constructor

```java
public WmPopup(Context context,Typeface font,String caption,int anchor,int from,int to,View content);
```

`content` must be non-null and have no parent. It becomes a child of the body container with height fitted to its content. Clicking an empty area inside the popup does nothing by default.

`anchor` is the tail x coordinate inside the popup and remains fixed after construction. For a surface-coordinate anchor, subtract the [leftForAnchor](#leftforanchor) result before passing it here. `from` and `to` are the left and right ARGB colors of the title background.

The title has y=`28`, height `44`, and side margins `20`. The body has y=`72`, side/bottom margins `4`, and lower corners clipped to radius `16`. Before API 18, a software layer clips the body. Put scrolling and buttons inside `content`.

## heading

```java
public final TextView heading;
```

The popup-owned title View uses white text, one line, and vertical center alignment. `font` applies only to the title. The caller sets content fonts.

## leftForAnchor

```java
public static int leftForAnchor(int width,int anchor);
public static int leftForAnchor(int width,int anchor,int surfaceWidth);
```

Returns the popup's left x coordinate on the surface. Here, `anchor` uses surface coordinates. `width` must be `1..480` and `surfaceWidth` must be at least `480`; violations throw `IllegalArgumentException`. The two-argument overload uses `surfaceWidth=480`.

For width `154`, the candidate position is `anchor−90`. Other widths use `max((surfaceWidth−width)/2, anchor+20−width)` with integer division. The result is clamped to `0..surfaceWidth−width`. The caller must apply the returned position to the layout.

## setCaption

```java
public void setCaption(String caption);
```

Changes the title text. Null displays an empty title.

## setColors

```java
public void setColors(int from,int to);
```

Applies a new [WmBubble](/en/api/wmbubble/) background while retaining the stored tail position. The two ARGB colors form the horizontal title/tail gradient; the white body and black border remain.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmPopup.java`
