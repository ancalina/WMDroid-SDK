---
title: "WmPwiView"
slug: en/api/wmpwiview
---
[API index](/en/api/)

```java
public final class WmPwiView extends View implements WmControls.LanguageListener
```

Read-only binary PWI text view using recovered wrapping/coordinate kernels with Android font measurement. It does not use saved line caches. The owner supplies device DPI, scale, break profile, and explicit persisted-font mappings.

## Profile

```java
public static final class Profile
public final int mode, dpiX, dpiY, documentScale;
public final boolean extendedBreaks, gridTextAdjustment;
public Profile(int mode, int dpiX, int dpiY, int documentScale, boolean extendedBreaks, boolean gridTextAdjustment)
```

DpiX/dpiY/documentScale must be positive, DPI×scale must fit signed int, and native unit0 coefficients must be positive; violations throw `IllegalArgumentException`. Mode is the native view mode without a separate range clamp. ExtendedBreaks selects text wrapping behavior and normal font scale 260/240. GridTextAdjustment applies a +1 baseline adjustment in mode0 and -1 in the grid font cap. DPI is not inferred from launcher display density.

## FontResolver

```java
public interface FontResolver {
    Typeface resolve(WmPwiDocument.Font font, WmPwiDocument.CharacterFormat format) throws IOException;
}
```

Returns the actual Typeface for the persisted faceName and complete weight/italic state. Null causes IOException instead of font substitution. Report mapping failure with IOException. Required styles are resolved during construction and cached by style.

## Constructor

```java
public WmPwiView(Context context, WmPwiDocument document, Profile profile, FontResolver resolver) throws IOException
```

Requires non-null document/profile/resolver or throws `IllegalArgumentException`. Plain PWI storage, bullets, objects/attachments, extended formats, highlight, unknown underline operands, unsupported color indices, unresolved font/size, zero-height fonts, and font overflow throw IOException. Underline/strike use Android decorations. It does not silently substitute fonts or flatten documents. Construct/reflow on the UI thread; document/profile remain fixed for the view lifetime.

## Layout state

```java
public WmPwiDocument getDocument()
public Profile getProfile()
public int getLineCount()
public int getContentHeight()
public String getLayoutError()
```

Document/profile return the supplied objects. LineCount is the number of draw lines from the last successful reflow; contentHeight is the drawing extent in Android pixels. Both are 0 before reflow or after failure. LayoutError is null on success, otherwise the failure message.

## Language

```java
public void onWmLanguageChanged(String language)
```

Updates the error content description through `pwi.layout_error`. `WmControls.localizeTree` calls this method. Document text and `getLayoutError()` remain unchanged; no reflow occurs. Successful reflow restores document text as the content description. Call on the UI thread.

## reflow and drawing

```java
public void reflow(int width) throws IOException
protected void onMeasure(int widthSpec,int heightSpec)
protected void onDraw(Canvas canvas)
```

Width is nonnegative Android pixels; negatives throw `IllegalArgumentException`. Converts to native width, remeasures/wraps, and rebuilds draw runs. Coordinate/height overflow or measurement/format failures throw IOException after clearing previous drawing, setting height=0 and the error. OnMeasure automatically reflows when width changes; that path retains IOException as error state and measures empty content. OnDraw draws prepared runs within the clip without measuring/wrapping. Automatic measurement caches failures at the same width too; explicitly call reflow to retry.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmPwiView.java`
