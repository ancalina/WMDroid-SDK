---
title: "WmAppIcons"
slug: en/api/wmappicons
---
[API](/en/api/)

Renders Android app icons into square Bitmaps.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmAppIcons.java`

## Declaration

```text
public final class WmAppIcons
public static Bitmap render(Drawable source,int pixels,boolean unframed);
```

pixels is the output side length in pixels. A null source returns null. Otherwise pixels≤0 throws `IllegalArgumentException`.

Returns a new ARGB_8888 Bitmap with DENSITY_NONE. Restores source bounds even when drawing fails. The caller owns the returned Bitmap. Do not render a shared Drawable concurrently.

On API 26 or later, unframed attempts to extract the AdaptiveIconDrawable foreground. It crops the bounds of alpha>16 pixels, preserves aspect ratio, and centers them with at least `pixels*0.04` padding on each output side. No valid bounds, or more than 95% of alpha>0 pixels having R, G, and B all greater than235, uses the complete icon. Other Drawables use their complete content.
