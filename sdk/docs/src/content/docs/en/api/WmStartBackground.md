---
title: "WmStartBackground"
slug: en/api/wmstartbackground
---
[API index](/en/api/)

```java
public class WmStartBackground extends Drawable
```

Draws the Start-menu repeating texture and black border.

## Constructor

```java
public WmStartBackground(Bitmap bitmap);
```

Null throws `IllegalArgumentException`. The shader references the Bitmap without copying it. Do not recycle the Bitmap until the Drawable is no longer used. The caller retains Bitmap ownership.

## draw

```java
public void draw(Canvas canvas);
```

Fills current bounds with texture repeated in both directions. The Bitmap is not stretched; the shader offset is `(-2, -2)`. Tiles use current Canvas coordinates without a separate translation to the bounds' left/top. Scaling the whole surface scales the tiles by the same factor.

Draws a black border with width `2` logical pixels and its centerline `1` pixel inside each bound. The caller sets bounds.

## setAlpha, setColorFilter

```java
public void setAlpha(int alpha);
public void setColorFilter(ColorFilter filter);
```

Apply only to the texture Paint and call `invalidateSelf()`. The black border is unchanged. `alpha` uses Paint's `0..255` range; a null filter removes the existing filter.

## getOpacity

```java
public int getOpacity();
```

Always returns `PixelFormat.OPAQUE`. It does not reflect Bitmap transparency or configured alpha; do not use it to establish that the entire shape is opaque.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmStartBackground.java`
