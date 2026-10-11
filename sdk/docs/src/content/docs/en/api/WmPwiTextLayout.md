---
title: "WmPwiTextLayout"
slug: en/api/wmpwitextlayout
---
[API index](/en/api/)

```java
public final class WmPwiTextLayout
```

Native wrapping for PWI text tokens using owner-supplied measurement. Font results depend on that callback, including Android/GDI differences. Coordinates/widths use native logical units.

## Measurer and Measurement

```java
public interface Measurer {
    Measurement measure(String text, WmPwiDocument.CharacterFormat format, int availableWidth) throws IOException;
}
public static final class Measurement
public final int fittedCharacters, width, ascent, descent;
public Measurement(int fittedCharacters, int width, int ascent, int descent)
```

The callback returns the fitted UTF-16 prefix length, its width, and ascent/descent. FittedCharacters must be 0..text.length; ascent/descent must be 0..255. Integer.MAX_VALUE means unlimited measurement; availableWidth can also be negative. Width must be nonnegative except the native empty-fit case: availableWidth<0, fit=0, width=availableWidth. The constructor does not validate, but layout throws `IOException` for null/invalid results. Zero fit is valid; layout guarantees progress separately. Callback IOException propagates.

## layout and Line

```java
public static List<Line> layout(WmPwiDocument.Paragraph paragraph, int mode, int frameLeft, int frameWidth, boolean extendedBreaks, Measurer measurer) throws IOException
public static final class Line
public final int logicalStart, logicalCount, width, ascent, descent;
```

Requires non-null paragraph/measurer and frameWidth≥0, otherwise `IllegalArgumentException`. Returns an unmodifiable list. Line contains the logical token range, measured width, and maximum ascent/descent; counts include invisible native tokens but exclude format tokens. The caller reconstructs glyphs. Mode uses the `WmPwiLayout.wrapStart` indentation rules. ExtendedBreaks enables non-ASCII boundaries and opening/closing punctuation correction. Type3/7 objects, unsupported tokens/extended formats, arithmetic overflow, and invalid measurement yield `IOException`.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmPwiTextLayout.java`
