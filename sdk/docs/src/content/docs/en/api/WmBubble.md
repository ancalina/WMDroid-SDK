---
title: "WmBubble"
slug: en/api/wmbubble
---
[API index](/en/api/)

```java
public class WmBubble extends Drawable
```

Popup background with a white body, horizontal gradient header, tail, and black outline. Use [WmPopup](/en/api/wmpopup/) to place title and content Views together.

## Constructor

```java
public WmBubble(int anchor,int from,int to);
```

`anchor` is the logical-pixel distance from the Drawable bounds' left edge to the tail tip. `from` and `to` are the left and right ARGB colors of the header and tail. These values remain fixed after construction. Create a new Drawable to change the colors or tail position.

## draw

```java
public void draw(Canvas c);
```

Set bounds before drawing. The shape is translated to the bounds' left/top coordinates and uses their width/height. The tail tip is at local `(anchor, 2)`, the body starts at `27`, the header ends at `72`, and outline width is `4` logical pixels. The header gradient runs from x=`4` to `width−4`.

The tail base is `52` wide and extends right when `anchor < width/4`, or left otherwise. `width/4` uses integer division. Only the base position is adjusted; the tip is not clamped. To prevent a tail outside the bounds, choose an appropriate `anchor` or clip the Canvas. Body, tail, and header share one contour, with no separate stroke at their junction. A normal return restores the previous Canvas state.

## setAlpha, setColorFilter

```java
public void setAlpha(int alpha);
public void setColorFilter(android.graphics.ColorFilter filter);
```

Neither method changes the rendering or stored colors. `setAlpha` cannot fade the whole background.

## getOpacity

```java
public int getOpacity();
```

Always returns `PixelFormat.TRANSLUCENT`.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmBubble.java`
