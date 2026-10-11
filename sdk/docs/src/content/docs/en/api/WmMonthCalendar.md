---
title: "WmMonthCalendar"
slug: en/api/wmmonthcalendar
---
[API index](/en/api/)

```java
public final class WmMonthCalendar extends View implements WmControls.RestyleListener,WmControls.LanguageListener
```

A42-cell body for date-selection popups, separate from Calendar-screen [WmMonthView](/en/api/wmmonthview/). Provides adjacent months, selection, Today highlighting/footer. Today is the device’s local date read at construction and is not recomputed by setMonth or locale changes. Initial selection clamps that date to owner bounds.

## Setup

### Constructors

```java
public WmMonthCalendar(Context context,Typeface font,Typeface bold,Locale locale);
public WmMonthCalendar(Context context,Typeface font,Typeface bold,Locale locale,WmDateTimeField.Value minimum,WmDateTimeField.Value maximum);
```

Requires non-null locale. Default range1900-01-01..2999-12-31; the overload accepts ordered full-date bounds within the DATE supported range and ignores time. Invalid bounds throw `IllegalArgumentException`. Initial selection clamps today to the range.

## Date selection

### setOnDateListener

```java
public void setOnDateListener(OnDateListener listener);
```

Confirms a completed selectable-date or Today-footer tap, including a tap on the selected date. Keyboard movement and programmatic setters do not notify. A date-change notification runs first when applicable. Null removes the listener.

### setOnDateChangeListener

```java
public void setOnDateChangeListener(OnDateListener listener);
```

Reports selection changes from user taps or keyboard movement. Programmatic setters and taps on the selected date do not notify. Null removes the listener. Month is one-based.

### setOnNoneListener

```java
public void setOnNoneListener(Runnable listener);
```

Adds a separate None footer for a non-null listener. A tap runs the listener without changing selection. Null removes the row.

### setSelectedDate

```java
public void setSelectedDate(int year,int month,int day);
```

Silently selects a real date; out-of-owner-range values throw `IllegalArgumentException`. Does not change the displayed month.

### selectedDate

```java
public int[] selectedDate();
```

Returns a new selected `[year,month,day]` array with one-based month.

### setMonth

```java
public void setMonth(int year,int month);
```

Rebuilds42 cells for a year/month within owner limits. Invalid year/month throws `IllegalArgumentException`. Retains selected date.

## Language and display

### setLocale

```java
public void setLocale(Locale locale);
```

Updates weekday labels, regional week arithmetic, footer, and geometry. Retains explicitly configured first weekday. Null throws `IllegalArgumentException`.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

Updates footer captions and cancels touch; retains the explicit date/weekday Locale. Null throws IllegalArgumentException.

### setFirstDayOfWeek

```java
public void setFirstDayOfWeek(int day);
```

Sets Calendar.SUNDAY or Calendar.MONDAY; other values throw IllegalArgumentException. Updates date presentation and pending selection.

### getFirstDayOfWeek

```java
public int getFirstDayOfWeek();
```

Returns the configured first weekday, or the current Locale’s first weekday before explicit configuration.

### setWeekNumbersVisible

```java
public void setWeekNumbersVisible(boolean visible);
```

Shows the week-number gutter (default false). Numbers use locale minimum-days-in-first-week and the configured first weekday.

### setColors

```java
public void setColors(int accent);
```

Sets the accent for selected cells and weekday labels.

## Geometry

### dateAt

```java
public int[] dateAt(int index);
```

Returns a new `[year, month, day]` array for `index`, with one-based month. Includes adjacent-month dates and dates outside owner bounds. `index` must be `0..41`; otherwise throws `IllegalArgumentException`.

### getHeaderHeight

```java
public int getHeaderHeight();
```

Returns font-derived weekday-header height in logical pixels.

### getWeekColumnWidth

```java
public int getWeekColumnWidth();
```

Returns logical-pixel week-gutter width, zero when hidden.

### cellBounds

```java
public Rect cellBounds(int index);
```

Returns a new Rect for `index`, calculated from the current View size, font, and week-number gutter. Coordinates use View-local logical pixels, independent of date availability. `index` must be `0..41`; otherwise throws `IllegalArgumentException`.

## Input, focus, and lifetime

### onDraw

```java
protected void onDraw(Canvas canvas);
```

Android measurement/layout/drawing hooks update control geometry and selection using logical coordinates/current font metrics. Do not invoke directly.

### onTouchEvent

```java
public boolean onTouchEvent(MotionEvent event);
```

Disabled state returns false and cancels touch tracking. Only a completed single-pointer tap on the same target selects a date. Movement outside the target, cancellation, and multi-touch do not select. A changed date calls the change listener before the confirmation listener. The selected date calls only the confirmation listener. Use normal View dispatch.

### onKeyDown

```java
public boolean onKeyDown(int code,KeyEvent event);
```

Moves the selected date, updates the displayed month, then notifies the change listener. Does not call the confirmation listener.

| Key | Movement |
| --- | --- |
| Left / Right | −1 day / +1 day |
| Up / Down | −7 days / +7 days |
| PageUp / PageDown | −1 month / +1 month |
| Ctrl+PageUp / Ctrl+PageDown | −1 year / +1 year |
| Home / End | First / last day of the selected month |

Month/year movement uses GregorianCalendar date adjustment. A result outside owner bounds leaves the date unchanged but consumes the key. Disabled state, Alt combinations, Ctrl combinations other than Ctrl+Page, and unlisted keys pass to the superclass. Ctrl+Home/End behavior is not implemented.

### performClick

```java
public boolean performClick();
```

Android accessibility/click dispatch hook. Actual date/command selection follows the user-selection contract above.

### onSizeChanged

```java
protected void onSizeChanged(int w,int h,int oldw,int oldh);
```

Android measurement/layout/drawing hooks update control geometry and selection using logical coordinates/current font metrics. Do not invoke directly.

### onFocusChanged

```java
protected void onFocusChanged(boolean gain,int direction,Rect previous);
```

Android measurement/layout/drawing hooks update control geometry and selection using logical coordinates/current font metrics. Do not invoke directly.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Android detach hook; cancels the current touch selection.

### onWmRestyle

```java
public void onWmRestyle(Typeface font,Typeface bold,int selection,int tabColor);
```

Updates regular/bold fonts and accent, cancels touch, and remeasures date-grid geometry.

## OnDateListener

```java
public interface OnDateListener
```

### choose

```java
void choose(int year,int month,int day);
```

Passes the date as `[year, month, day]`, with one-based month. Confirmation and change conditions follow the corresponding listener registration methods.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmMonthCalendar.java`
