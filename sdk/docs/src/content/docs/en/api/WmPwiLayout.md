---
title: "WmPwiLayout"
slug: en/api/wmpwilayout
---
[API index](/en/api/)

```java
public final class WmPwiLayout
```

Recovered RichInk coordinate conversion, indentation, and line-position arithmetic. Inputs use native logical coordinates and explicit DPI/scale, not Android dp/sp. The caller measures fonts, wraps text, and draws.

## Zoom

```java
public static int zoomScale(int percent, boolean extendedBreaks)
public static int zoomPercent(int scale, boolean extendedBreaks)
```

Converts percentages and native documentScale. Standard mappings: 75→198, 100→240/260, 125→300/325, 150→396, 200→550, 300→793; values after / use extendedBreaks=true. Other values use zoomScale=`(2380*percent+500)/1000`, zoomPercent=`(1000*scale+1190)/2380`. No range clamp; Java signed-int overflow is retained. Do not pass legacy zoom-menu indices 0..5.

## Coordinate conversion

```java
public static int scaleCoordinate(int value, int offset, int numerator, int denominator, int origin)
public static int unitScale(int unit, int dpi, int documentScale)
public static int convertUnits(int value, int sourceUnit, int targetUnit, int dpi, int documentScale)
```

ScaleCoordinate adds offset, scales/rounds, then adds origin using native signed32 arithmetic. A zero denominator returns `Integer.MAX_VALUE` without origin; intermediate overflow wraps. Nearest rounding requires a positive denominator and no intermediate overflow. UnitScale coefficients for units 0..6 are `dpi*scale/1440`, `2540*scale/1440`, `1000*scale/1440`, `72*scale/1440`, `scale`, `72`, and `4*dpi*scale/1440`; other units throw `IllegalArgumentException`. Supply the original device DPI for the axis. ConvertUnits passes target/source coefficients to scaleCoordinate without inferring DPI or documentScale.

## Paragraph placement

```java
public static int wrapStart(WmPwiDocument.Paragraph paragraph, int mode, int lineIndex, int frameLeft, int frameWidth)
public static int lineStart(WmPwiDocument.Paragraph paragraph, int mode, int lineIndex, int frameLeft, int frameWidth, int measuredWidth)
```

Requires non-null paragraph and lineIndex≥0; otherwise IllegalArgumentException. Base indent is level indent in mode1; other modes use left indent plus108 for a bullet and the first-line delta.

**wrapStart:** Clamps the base to0 first, then applies frameWidth+frameLeft-72 cap unless RIGHT. Narrow frames may therefore return negatives.

**lineStart:** Mode1 uses level indent and skips RIGHT/CENTER computation. Other modes calculate RIGHT/CENTER from actual measuredWidth. The same upper cap applies only when not RIGHT, then the final value clamps to0. Never substitutes dirty saved width; coordinates/widths are native logical units.

## Control and saved line advance

```java
public static int controlAdvance(int control, int currentX, int suppliedOrigin, int frameLeft)
public static int savedLineAdvance(WmPwiDocument.Paragraph paragraph, int lineIndex, int mode, int dpiX, int documentScale)
```

Control must be 0..255 or `IllegalArgumentException` is thrown. 0 (CR) advances 36; 4 (TAB) advances to the next 170-unit stop relative to suppliedOrigin (frameLeft when 0); others advance 0. SavedLineAdvance in mode 0/1 with a clean cache and existing line returns at least 72 after converting saved ascent+descent from unit0 to unit5 with the X coefficient. Otherwise it returns 72. It does not refresh the saved cache.

## positionLineBottoms

```java
public static int[] positionLineBottoms(int referenceLine, int referenceBottom, int[] advances)
```

referenceLine is the zero-based index after the reference boundary(0..advances.length). If >0, result[referenceLine-1] equals referenceBottom. Subtract each advance below; add under world-Y rules above. Zero places every line below the reference; array length anchors the last line at it.

`positionLineBottoms(1,500,new int[]{72,100,80})` returns `{500,400,320}`. Results are native world-Y anchors, not Android baselines. advances must be non-null with every value≥72 or IllegalArgumentException is thrown. Output length is unchanged and input is not modified.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmPwiLayout.java`
