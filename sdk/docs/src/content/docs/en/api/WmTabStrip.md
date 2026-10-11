---
title: "WmTabStrip"
slug: en/api/wmtabstrip
---
[API](/en/api/)

```java
public final class WmTabStrip extends LinearLayout implements WmControls.RestyleListener
```

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmTabStrip.java`

## Constructor

```java
public WmTabStrip(Context context,int color);
```

Construct and modify on the UI thread; callbacks run there. `color` is the strip and arrow face color. Default height is `40` SDK logical pixels and width is MATCH_PARENT. Layout has no padding and bottom-aligns tabs. The owner manages page content and page transitions.

## addTab

```java
public TextView addTab(String title,Typeface font,Runnable action);
```

Creates an initially unselected TextView and returns its internal reference. Displays the supplied title with shared font/tab styles. Initial width is `ceil(WmFonts.measure(...))+32`; height is `40` logical pixels. Allows keyboard focus but not touch-mode focus.

`action` must be non-null. Construction does not validate it; null causes `NullPointerException` on activation. Click invokes it synchronously only when both the strip and tab are enabled. The owner must update content and selection with [WmControls.tab](/en/api/wmcontrols/) or [WmPropertySheet](/en/api/wmpropertysheet/). Apply the shared tab style rather than only setSelected to notify selection changes.

Caption changes recalculate width and request selected-tab reveal. Font-only changes still measure the existing LayoutParams width. Use [WmControls.restyleTree](/en/api/wmcontrols/) or reapply `WmControls.tabWidth(tab)` to layout width. Do not replace the internal tabs, key listeners, or overflow structure.

## Keyboard navigation

On an enabled tab in an enabled strip, Left/Right move one index from the first selected tab. At the first selected tab, Left DOWN returns false from the listener for owner handling; Right DOWN does the same at the last selected tab. Selection stays unchanged at these boundaries. Home/End target the first/last tab. Navigation does not use the focused tab as its starting index or skip disabled targets. Modifiers are not checked.

Other navigation DOWN calls performClick on a different enabled target, then requests focus on the actual first selected tab. This follows page-gate vetoes, redirects, and reentrant selections. With no selection, Left/Right target the first tab. Matching UP remains consumed, including at these boundaries. The listener does not handle other keys/actions or disabled states.

## onMeasure

```java
protected void onMeasure(int widthSpec,int heightSpec);
```

Arguments are Android MeasureSpecs. Measures each tab with EXACTLY `max(0,LayoutParams.width)×40`. Resolves the strip's total tab width and height `40` against parent constraints.

Shows right-side overflow when total tab width exceeds measured width. Each arrow's `side` is the first tab's `round(descent-ascent)+8`, clamped to `0..min(stripHeight,stripWidth/2)`. Tab viewport width is `stripWidth−2*side`. This font calculation is an Android adaptation.

Width, font-metric, or selection changes adjust the viewport at the next measurement to reveal the selected tab. A tab wider than the viewport aligns its leading position without guaranteeing complete visibility. Arrow scrolling does not change selection or pages.

## onLayout · dispatchDraw

```java
protected void onLayout(boolean changed,int l,int t,int r,int b);
protected void dispatchDraw(Canvas canvas);
```

Offsets tabs left by the cumulative width before the first visible index and bottom-aligns them. Places overflow at the bottom-right; left/right arrow hit areas overlap by `2` logical pixels.

Clips tab drawing to the viewport and draws overflow outside it. Tab selection backgrounds/borders follow [WmTabDrawable](/en/api/wmtabdrawable/). Arrows have a black `2`-pixel border and the strip face color. Pressed faces are black; arrow ink is normally black, pressed white, and disabled SHADOW.

## dispatchTouchEvent

```java
public boolean dispatchTouchEvent(MotionEvent event);
```

Returns false for a disabled strip. With overflow, consumes DOWN in the area right of the viewport and above the arrows so hidden tabs cannot be tapped. Other events delegate to the superclass.

Arrow DOWN immediately moves one index. Repetition starts after `530ms` and runs every `100ms` thereafter. Repeated movement depends on elapsed time since DOWN.

| Elapsed time | Index step |
| --- | --- |
| `0..2047ms` | `1` |
| `2048..5119ms` | `5` |
| At least `5120ms` | `20` |

Both arrows are enabled when the strip is enabled and a scrollable index range exists. Movement at a range boundary does nothing. UP, CANCEL, tracked-pointer UP, leaving bounds, disabling, hiding, window hiding, and detachment cancel repetition and pressed state. UP does not add a step.

## setEnabled

```java
public void setEnabled(boolean enabled);
```

On an enabled→disabled transition, sends CANCEL through the superclass to cancel the active child gesture, then updates arrows. Retains each tab's enabled value. A disabled parent blocks tab actions, keyboard navigation, and touch dispatch. Re-enabling leaves independently disabled tabs unchanged.

## onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Updates strip/arrow face color to `tabColor` and requests measurement. Does not directly use `regular`, `bold`, or `selection`. Update tab fonts, backgrounds, and text colors through `WmControls.restyleTree` or owner styling.

![Android API 14 render](/WmTabStrip.png)
