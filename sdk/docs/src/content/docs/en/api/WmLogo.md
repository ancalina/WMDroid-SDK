---
title: "WmLogo"
slug: en/api/wmlogo
---
[API](/en/api/)

Drawable for Start-button artwork within the assigned bounds.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmLogo.java`

## Declaration

```text
public final class WmLogo extends Drawable
public WmLogo();
public WmLogo(Context context);
protected void onBoundsChange(Rect bounds);
public void draw(Canvas canvas);
public int getIntrinsicWidth();
public int getIntrinsicHeight();
public void setAlpha(int value);
public void setColorFilter(ColorFilter value);
public int getOpacity();
```

The no-argument constructor uses the fallback. The Context constructor loads the start icon from the current pack.

Intrinsic size is41×36. Raster artwork retains its aspect ratio and is centered in bounds. WmVectorDrawable uses the complete bounds. Alpha, colorFilter, and opacity delegate to the artwork.
