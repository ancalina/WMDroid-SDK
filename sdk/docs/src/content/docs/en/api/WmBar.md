---
title: "WmBar"
slug: en/api/wmbar
---
[API](/en/api/)

Drawable for title bars, command bars, and their button backgrounds.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmBar.java`

```java
public class WmBar extends Drawable
```

## Constructor

```java
public WmBar(Context context,boolean softkey,int base);
```

`softkey=true` selects the command bar; false selects the title bar. `base` is the ARGB theme color. Reads `themes/gradients.json` through [WmResources.openAsset](/en/api/wmresources/) synchronously. Valid tables contain73 unique indices0–72 with integer HSB values0–0xffffff; reads over256KiB or invalid JSON/records use the fallback. The base hue replaces table hue while saturation/brightness come from each record; gray base colors produce grayscale slots. The fallback darkens base luminance to at most100, forces opaque alpha, and uses white foreground slots14/15/37/38/39. Colors are fixed for this instance; create another bar after a theme change.

## color

```java
public int color(int index);
```

`index` must be 0–72. Returns that slot's ARGB color. Other indices throw `ArrayIndexOutOfBoundsException`.

## softButton

```java
public Drawable softButton(View cell);
```

Pass a command-bar button View. A title bar throws `IllegalStateException`. The returned Drawable highlights only enabled pressed or selected states. It uses the cell's parent width and position to draw the matching command-bar region. Use it as that bar's cell background. The Drawable retains the cell and bar references. An inactive background draws nothing; keep the bar background underneath. Highlighting is clipped to a 180-logical-pixel key at the left or right edge, chosen by the cell position.

## titleButton

```java
public Drawable titleButton(View cell,boolean start,boolean close);
```

Pass a title-bar button View. A command bar throws `IllegalStateException`. `start` enables the Start highlight and separator; `close` enables the close-button border. Pressed or selected state enables the highlight even without enabled state. Only `start=true` limits the highlight to `min(180, cellWidth)` logical pixels; other title cells use their clipped shared-bar region. The normal Start state draws a separator; `close=true` draws its border in both states. Flags are independent and may both be true. The Drawable retains the cell and bar references.

Button backgrounds use the parent View width when positive, otherwise the bar bounds width or `WmMetrics.WIDTH`. Cell position must use the same parent coordinates as the bar. Null cell is not rejected by the factory but fails with `NullPointerException` when drawing accesses it. Each returned background has independent alpha/filter, invalidates on their changes, and reports `PixelFormat.TRANSLUCENT`. Bar alpha/filter changes do not propagate to these backgrounds.

## draw · setAlpha · setColorFilter · getOpacity

```java
public void draw(Canvas canvas);
public void setAlpha(int alpha);
public void setColorFilter(ColorFilter filter);
public int getOpacity();
```

Draws the normal-state bitmap across the supplied bounds; the 52-row source is scaled to the bounds height. Widths below360 reuse the480-wide bitmap; other widths retain180-pixel edge ramps and expand the middle. Changes to alpha or ColorFilter invalidate this Drawable. `getOpacity()` always returns `PixelFormat.OPAQUE`, including after alpha changes. Change View-background state and bounds on the UI thread.
