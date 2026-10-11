---
title: "WmColor"
slug: en/api/wmcolor
---
[API index](/en/api/)

```java
public final class WmColor
public WmColor();
```

Integer HSB conversion and theme Bitmap hue changes. Color inputs use Android ARGB ints. HSB hue uses a 240-unit scale, unlike Android HSV degrees.

## hue

```java
public static int hue(int rgb);
```

Returns integer hue `0..239` from RGB channels, ignoring alpha. Achromatic R=G=B returns `123`; this value alone does not identify achromatic colors. Converts RGB channels to `0..10000` before integer arithmetic, so rounding can differ from Android HSV hue.

## rgb

```java
public static int rgb(int hue,int saturation,int brightness);
```

Converts integer HSB to ARGB with alpha `255`. Use hue `0..239` and saturation/brightness `0..240`. Saturation `0` gives gray; brightness `0` gives black. Channel calculations use integer division.

Does not clamp inputs or wrap hue. Hue `240` does not become `0`. Do not interpret out-of-range results as valid HSB colors.

## isGray

```java
public static boolean isGray(int color);
```

Returns true when RGB channels, excluding alpha, satisfy `max==0 || (max−min)*100<=max`. Includes both exact gray and colors whose channel difference is at most 1% of the maximum. Used by `tint` to select a grayscale theme.

## gray

```java
public static int gray(int color);
```

Calculates integer brightness `v=(299*R+587*G+114*B)/1000` and places it in all three RGB channels. Returns ARGB with the original alpha.

## tint

```java
public static android.graphics.Bitmap tint(android.graphics.Bitmap source,int base,int chosen);
```

Converts synchronously. Process large Bitmaps on a worker thread. Ignores alpha in `base` and `chosen`. If `isGray(chosen)` is false, applies the Android HSV hue difference `chosenHue−baseHue` to each pixel, wrapping from `0` inclusive to `360` exclusive. Retains pixel HSV saturation/value and alpha. If `isGray(chosen)` is true, applies `gray` instead of changing hue.

Leaves alpha-`0` pixels and exact R=G=B pixels unchanged. Near-gray pixels that satisfy only `isGray` are not excluded.

For a non-gray theme with hue difference `0`, returns source without reading it. Also returns the same source when no pixel changes. Otherwise returns a new ARGB_8888 Bitmap preserving size, density, and hasAlpha. Never modifies or recycles source. The caller owns a distinct returned Bitmap; source ownership remains unchanged.

The conversion path requires a readable Bitmap. A null source returns null unchanged on the zero-hue-difference fast path; otherwise it throws `NullPointerException`. Bitmap read or allocation failures propagate to the caller.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmColor.java`
