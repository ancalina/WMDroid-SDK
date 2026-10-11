---
title: "WmFonts"
slug: en/api/wmfonts
---
[API](/en/api/)

Loads fonts, links hybrid fallback faces, attaches TextViews, and measures Canvas text.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmFonts.java`

```java
public final class WmFonts
public static final String SYSTEM_SOURCE = "system:default";
public static final String DEFAULT_SOURCE = "fonts/gulim/Gulim-Regular.ttf";
```

## PreparedLine

```java
public static final class PreparedLine
public final float width;
```

## load(Context, String, boolean, boolean) · family(...)

```java
@Deprecated public static boolean isCached(String source,boolean hybrid,boolean heavy);
public static boolean isCached(Context context,String source,boolean hybrid,boolean heavy);
public static Typeface load(Context c,String source,boolean hybrid,boolean heavy);
public static Typeface family(Context c,String id,String fallbackPath,boolean heavy);
```

source is system:default, an asset path, or a WmFontFile URI. System source returns DEFAULT or DEFAULT_BOLD. Prepare other sources on a worker. Cache keys include app, pack revision, source, hybrid, and heavy. isCached(context,...) checks without loading. The deprecated context-free overload always returns false.

hybrid=true prioritizes Tahoma and uses the selected face for missing glyphs. heavy selects Tahoma Bold. It does not synthetically embolden selected outlines. Pass a bold source for the bold role. Missing Tahoma uses the selected face. family is a legacy entry point that loads fallbackPath with hybrid=true; id does not distinguish its cache.

A missing selected asset tries DEFAULT_SOURCE Gulim, then the system face. Preparation failure uses the system face. After successful preparation, an invalid imported source throws `IllegalArgumentException`.

## attach(TextView, Typeface)

```java
public static void attach(TextView view,Typeface face);
```

Call on the main thread after setTypeface. Applies needed fallback spans to current and future text and applies smoothing. Other spans, selection, and IME composition remain. Call again after changing fonts.

## measure(Paint, String) · ellipsize(String, Paint, float)

```java
public static float measure(Paint paint,String text);
public static String ellipsize(String text,Paint paint,float width);
```

Measure with the linked faces or return an END-ellipsized string. width uses Paint-coordinate pixels. Use the same font and textSize as Canvas drawing.

## prepare(Paint, String) · PreparedLine.draw(Canvas, float, float)

```java
public static PreparedLine prepare(Paint paint,String text);
public void draw(Canvas canvas,float x,float baseline);
```

Null paint or text throws `IllegalArgumentException`. Captures Paint and text state and stores width. Repeated drawing avoids measurement. x follows Paint.Align; baseline is the text baseline. Prepare a new line after font, text, or size changes. draw changes internal Paint state; do not draw one object concurrently.

## draw(...) · drawCenteredListCaption(...)

```java
public static void draw(Canvas canvas,String text,float x,float baseline,Paint paint);
public static void drawCenteredListCaption(Canvas canvas,String text,RectF bounds,Paint paint);
```

draw applies linked faces and shaping. x and baseline use Canvas coordinates. Centered captions center within bounds, then use left alignment and ASCII ... on overflow. Prefixes are limited to 259 UTF-16 units without splitting code points. Empty bounds do not paint. Null caption-drawing arguments throw `IllegalArgumentException`.

## centerBaseline(...) · textBounds(...)

```java
public static float centerBaseline(Paint paint,float top,float bottom,String text);
public static void textBounds(Paint paint,String text,Rect ink);
```

centerBaseline returns an ink-centered baseline; empty text uses font metrics. textBounds empties the caller's Rect and unions each linked-face run’s ink measured at the origin. It does not add horizontal run advances, so it is not the horizontal bounds of the complete drawn string. Use measure or PreparedLine.width for full advance. For textBounds, null paint, text, or ink throws `IllegalArgumentException`. centerBaseline throws `NullPointerException` for null text or for null paint with empty text; with nonempty text, null paint throws `IllegalArgumentException`.

## Smoothing

```java
public static void setSmoothing(boolean enabled);
public static boolean isSmoothing();
public static void applySmoothingLayer(android.view.View view);
public static void applySmoothing(TextView view);
```

setSmoothing sets a process-wide policy, default true. Apply applySmoothing or restyle to existing Views. Before API 18, off uses a software surface through applySmoothingLayer; on restores its previous layer. Off controls text AA and does not guarantee original bitmap-font pixels.
