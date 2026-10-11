---
title: "WmCalendarGeneralOptions"
slug: en/api/wmcalendargeneraloptions
---
[API index](/en/api/)

```java
public final class WmCalendarGeneralOptions extends FrameLayout implements WmControls.BackHandler,WmControls.RestyleListener,WmControls.LanguageListener
```

Calendar settings page for start view, first weekday, work-week columns, half-hour rows, and week numbers. Reports user changes immediately; the caller persists/applies them. Constructor Options define initial values; use on the UI thread.

### Constants and fields

```java
public static final int START_VIEW = 21163;
public static final int FIRST_DAY = 21161;
public static final int WEEK_DAYS = 21126;
public static final int HALF_HOUR = 21175;
public static final int WEEK_NUMBERS = 21162;
```

START_VIEW=21163, FIRST_DAY=21161, WEEK_DAYS=21126, HALF_HOUR=21175, WEEK_NUMBERS=21162 are field View ids. Accessing these children with `findViewById()` returns live controls. Replacing their internal open/choice/checked listeners disconnects popup handling or option notifications; use `setOptions()` for silent model updates.

### Constructors

```java
public WmCalendarGeneralOptions(Context context,Typeface font,Locale locale,Options options,int selection,Listener listener);
public WmCalendarGeneralOptions(Context context,Typeface font,Typeface bold,Locale locale,Options options,int selection,Listener listener);
```

The two-face constructor uses the supplied bold face for Western headings. The single-face constructor uses font for both roles; it does not generate bold. Null bold throws IllegalArgumentException. Korean, Japanese and Chinese page locales use24px regular text; other locales use21px regular labels/checks and21px bold headings. Combos remain24px. These are192dpi requested font heights; native GDI metric adjustment is not reproduced. Static headings/labels align top-left. setLocale changes these roles immediately; restyleTree updates both faces.

Null font/locale/options throws `IllegalArgumentException`. The supplied Options defines every initial setting; no separate defaults are applied. Initial assignment is silent; listener may be null. The page retains this callback for its lifetime and provides no callback replacement API. Use View operations on the UI thread.

### options

```java
public Options options();
```

Returns a new immutable snapshot of the fields.

### setOptions

```java
public void setOptions(Options options);
```

Closes popup and silently restores all fields without calling the listener. Null throws `IllegalArgumentException`.

### setLocale

```java
public void setLocale(Locale locale);
```

Relabels captions/weekdays and closes popup, retaining selections; an equal Locale still performs this update. Captions use the available catalog selected by [WmStrings.language](/en/api/wmstrings/#language); weekday names use DateFormatSymbols with the supplied Locale directly. Null throws `IllegalArgumentException`.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

Passes the shared catalog language’s Locale to setLocale; state preservation and popup handling follow that method’s contract.

### Choice popups

A choice opens only when that child is enabled and the page width/height are positive. The menu uses available space above or below its field and a [WmScrollView](/en/api/wmscrollview/). Keyboard navigation applies choices immediately while the popup remains open; dismissing it does not restore previous options. A completed touch choice closes the popup before applying the selection.

### closePopup

```java
public boolean closePopup();
```

Invalidates popup callbacks and removes the overlay. Returns true when an actual popup was removed, otherwise false.

### back

```java
public boolean back();
```

Returns the result of closing the current popup; false if none is open.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Closes the popup and stores font/selection color for subsequent popups. Update existing children through WmControls.restyleTree.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Android detach hook; closes the current popup overlay.

## Options

```java
public static final class Options
```

### Constants and fields

```java
public final int startView;
public final int firstDayOfWeek;
public final int weekDays;
public final boolean halfHour;
public final boolean weekNumbers;
```

startView: Agenda0/Day1/Week2/Month3/Year4. firstDayOfWeek:Sunday/Monday. weekDays:5/6/7. halfHour selects30-minute rather than60-minute rows; weekNumbers controls week numbers.

### Constructors

```java
public Options(int startView,int firstDayOfWeek,int weekDays,boolean halfHour,boolean weekNumbers);
```

Validates startView0..4, Sunday/Monday, and weekDays5..7; invalid values throw `IllegalArgumentException`.

## Listener

```java
public interface Listener
```

### changed

```java
void changed(Options options);
```

Synchronously reports combo/check notifications with new Options after changing the field. Programmatic `choose()` and a changed CheckBox `setChecked()` also notify; `setOptions()` suppresses the page callback. A choice may notify even when its selected index is unchanged. Callback exceptions propagate without reverting the changed fields. The owner applies/persists the options.


## Size and theme

DLU is the original dialog’s font-based layout unit. RCML roles identify original theme color/font roles.

At480 logical pixels, fields follow the native dialog coordinates. Korean choices start at212px; other locales use210px. Wider pages extend the field’s right edge. Narrow pages shrink columns; wrapped labels and oversized fonts expand rows. [WmScrollView](/en/api/wmscrollview/) owns vertical overflow.

Choice popups use a222px hidden-list client profile and round down to complete font-derived rows. Page edges can further reduce their height. This is the native7/14 DLU stock-font creation profile after RCML font assignment; live GDI rectangles and other native creation fonts remain unmeasured.

The heading uses the RCML role15 color:240-step theme hue, full saturation and brightness144/240. Grayscale themes use native integer luminance. Theme updates retain the heading and option values.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmCalendarGeneralOptions.java`
