---
title: "WmVectorDrawable"
slug: en/api/wmvectordrawable
---
[API](/en/api/)

Reads pack v1 vector JSON into a Drawable and draws it within its bounds.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmVectorDrawable.java`

## Declaration

```text
public final class WmVectorDrawable extends Drawable
public static final int MAX_BYTES = 256 * 1024;
public static WmVectorDrawable read(InputStream input)throws IOException;
public ConstantState getConstantState();
public void draw(Canvas canvas);
public int getIntrinsicWidth();
public int getIntrinsicHeight();
public void setAlpha(int value);
public void setColorFilter(ColorFilter value);
public int getOpacity();
```

`read()` leaves the InputStream open. Input is limited to 256 KiB. Format, numeric, and JSON errors throw `IOException`.

Required values: version=1; width and height (>0, ≤4096); intrinsicWidth and intrinsicHeight (1–4096); scale (fit or stretch); pixelGrid and compositeFilter (boolean); parts (1–512 entries). Total path commands are limited to 10000. ARGB colors use eight hex digits without #.

Path commands are M and L (two values), C (six), Q (four), O (center and positive radius, three), and Z (zero). fillRule is winding or evenodd. Alpha is 0–255. Stroke width is >0 and ≤4096. Gradients have 2–64 colors with the same number of ordered stops in 0–1. All numbers must be finite with absolute value≤100000. A transform is a nine-value Matrix.

fit retains aspect ratio and centers the content; stretch scales each axis separately. pixelGrid disables AA only for integer transforms without skew. compositeFilter applies alpha and filter to the complete drawing.

`getConstantState().newDrawable()` clones Paint state. Use a new Drawable for each consumer. `setAlpha()` clamps to 0–255. `getOpacity()` returns TRANSLUCENT. Parse on a worker.

## JSON structure

All root fields except matrix are required. Each parts object requires fillRule, path, color, and alpha.

| Object | Fields and values |
| --- | --- |
| root | `version`: integer1; `width`, `height`: positive≤4096; `intrinsicWidth`, `intrinsicHeight`: integers1–4096; `scale`: `fit` or `stretch`; `pixelGrid`, `compositeFilter`: booleans; `parts`: array of1–512 objects; optional `matrix`: array of9 numbers |
| part | `fillRule`: `winding` or `evenodd`; `path`: array of command arrays; `color`: `AARRGGBB` string; `alpha`: integer0–255 |
| optional stroke | part `stroke`: positive≤4096; required companion `join`: `round` or `miter`; cap is BUTT |
| optional gradient | part `gradient` object: `type` is `linear` or `radial`; `points` is linear `[x0,y0,x1,y1]` or radial `[cx,cy,radius]` with radius>0; `colors` has2–64 ARGB strings; `stops` has the same number of nondecreasing numbers in0–1; optional `matrix` is an array of9 numbers |

Each path command begins with its command string followed by coordinates, such as `['M',x,y]`. This minimal JSON draws a filled black triangle.

```json
{"version":1,"width":24,"height":24,"intrinsicWidth":24,"intrinsicHeight":24,"scale":"fit","pixelGrid":false,"compositeFilter":false,"parts":[{"fillRule":"winding","color":"ff000000","alpha":255,"path":[["M",0,0],["L",24,0],["L",24,24],["Z"]]}]}
```
