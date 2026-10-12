---
title: "WmTabDrawable"
slug: en/api/wmtabdrawable
---
[API index](/en/api/)

```java
public final class WmTabDrawable extends Drawable
```

Draws the background and borders of a bottom property-sheet tab.

## Constructor

```java
public WmTabDrawable(boolean selected,int face);
```

Stores `selected` and the ARGB `face` color. Selected tabs use white; unselected tabs use `face`. Both values remain fixed, so replace the Drawable to change selection or color.

## draw

```java
public void draw(Canvas c);
```

Translates the Canvas to the current bounds' left/top before drawing. Coordinates below use bounds-local logical pixels.

| State | Background | Black borders |
| --- | --- | --- |
| Selected | Full height from y=`0` | Left/right width `2`, starting at y=`0` |
| Unselected | Full height from y=`1` | Top y=`1`, height `2`; left/right width `2`, starting at y=`3` |

The selected tab omits its top border to join the white page. Neither state draws a bottom border. The first `1`-pixel row of an unselected tab remains untouched. A normal return restores Canvas state. The caller sets bounds.

## setAlpha, setColorFilter

```java
public void setAlpha(int a);
public void setColorFilter(ColorFilter f);
```

Neither method changes the rendering.

## getOpacity

```java
public int getOpacity();
```

Always returns `PixelFormat.OPAQUE`. It does not account for `face` alpha or the unpainted first row of an unselected tab.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmTabDrawable.java`
