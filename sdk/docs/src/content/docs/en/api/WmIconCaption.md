---
title: "WmIconCaption"
slug: en/api/wmiconcaption
---
[API index](/en/api/)

```java
public final class WmIconCaption extends TextView implements WmControls.RestyleListener
```

TextView that paints a selection rectangle around displayed line regions computed by Android Layout. The owner handles icon feedback and selection policy.

## Constructor

```java
public WmIconCaption(Context context,Typeface font,int selection)
```

Uses top/center-horizontal alignment, at most two lines, and end ellipsis. Disabled text uses SHADOW; enabled pressed/selected/activated text is white, otherwise black. selection is the selection-background ARGB color.

## Selection color and padding

```java
public void setSelectionColor(int color)
public void setSelectionPadding(int horizontal,int vertical)
```

Color changes invalidate the view. Padding defaults to zero and expands displayed line bounds in logical pixels. Negative padding throws IllegalArgumentException. Expanded bounds are clipped to the text viewport.

## Line limits

```java
public void setMaxLines(int maxLines)
public void setLines(int lines)
```

Applies the TextView line limit and uses that limit when measuring selection bounds. TextView handles text rendering.

## copySelectionBounds

```java
public boolean copySelectionBounds(Rect target)
```

Call after measure/layout. Copies the painted viewport-local rectangle into target and returns true. Requires enabled plus pressed, selected, or activated. Empty text, missing layout, zero size, or no visible line region clears target and returns false. Null target throws IllegalArgumentException. Does not change view state. Bounds round outward and account for padding, scroll, line limits, and clipping.

## Drawing and style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
protected void drawableStateChanged()
protected void onDraw(Canvas canvas)
```

Restyle applies only selection color; restyleTree handles TextView typography. State changes invalidate. onDraw paints the selection background before text.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmIconCaption.java`

Unions each displayed line’s getLineLeft/Right and getLineTop/Bottom bounds. It does not measure glyph raster pixel outlines.
