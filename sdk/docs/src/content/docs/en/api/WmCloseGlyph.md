---
title: "WmCloseGlyph"
slug: en/api/wmcloseglyph
---
[API](/en/api/)

Provides the title bar OK or X glyph and pressed color.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmCloseGlyph.java`

## Declaration

```text
public final class WmCloseGlyph extends Drawable
public WmCloseGlyph(boolean ok,int normalColor,int pressedColor);
public boolean isStateful();
protected boolean onStateChange(int[] states);
public void draw(Canvas canvas);
public int getIntrinsicWidth();
public int getIntrinsicHeight();
public void setAlpha(int value);
public int getAlpha();
public void setColorFilter(ColorFilter filter);
public ColorFilter getColorFilter();
public int getOpacity();
```

ok=true selects OK; false selects X. Intrinsic size is 46×52 logical pixels. Preserve this size, including the internal padding.

The pressed or selected state uses pressedColor; other states use normalColor. onStateChange returns true when the active state changes. setAlpha clamps to 0–255 and multiplies the color alpha. getColorFilter returns the current filter reference. getOpacity returns TRANSLUCENT.
