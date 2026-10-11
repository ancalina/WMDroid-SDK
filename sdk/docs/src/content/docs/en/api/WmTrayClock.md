---
title: "WmTrayClock"
slug: en/api/wmtrayclock
---
[API index](/en/api/)

```java
public final class WmTrayClock extends View
```

Digital/analog title-bar clock. The parent scales the entire title bar. No internal timer or popup; the host calls refresh.

## Constructor

```java
public WmTrayClock(Context context, Typeface regular, int pressedTextColor)
```

Creates a regular-font 24-logical-pixel digital clock, white by default, then refreshes current time. pressedTextColor applies to digital pressed/selected text. Default language is ko; keyboard focus is enabled.

## language / refresh

```java
public void language(String language)
public void refresh(Date now, boolean analog)
```

language stores the language for the next refresh. refresh applies a non-null Date and analog mode, invalidates, and requests layout when natural width changes. Uses system time zone and 12/24-hour preference (HH:mm or h:mm, without AM/PM). ko/en use Locale.KOREAN numeric formatting; other languages use catalog locale. Analog width is 40px; digital width is rounded-up measured text width plus 6px. Analog hands use integer tables at five-minute and whole-hour resolution. Accessibility text includes time and mode.

## Measurement and drawing

```java
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
protected void drawableStateChanged()
protected void onDraw(Canvas canvas)
```

Resolves natural width and 52px height against parent measure specs. State changes invalidate. Digital text centers using glyph bounds. Analog uses a white ring/hands; exact native pen rasterization and runtime hand color remain unverified.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmTrayClock.java`
