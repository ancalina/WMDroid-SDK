---
title: "WmYearView"
slug: en/api/wmyearview
---
[API index](/en/api/)

```java
public final class WmYearView extends View implements WmControls.RestyleListener
```

Calendar year view. Arranges160-logical-pixel month tiles and chooses atlas/visible count for the height. Uses pack numeral bitmaps, with a system-font fallback.

## Setup

### Constructors

```java
public WmYearView(Context context,Typeface font,Locale locale);
```

Starts on the current date. Null locale throws `IllegalArgumentException`. Small numerals use the atlas; titles/touch bubbles use the supplied font.

## Date and range selection

### setOnDayClickListener

```java
public void setOnDayClickListener(OnDayClickListener listener);
```

Shows a candidate-date bubble during touch movement and reports the final candidate on UP. Dead space/cancel does not notify. Month is one-based. Null removes the listener.

### setOnRangeChangedListener

```java
public void setOnRangeChangedListener(OnRangeChangedListener listener);
```

Synchronously reports current year/month/day when date, month page, or visible count changes, and on showYearStart. Geometry getters/resizing can also trigger this callback. Null removes the listener.

### setDate

```java
public void setDate(int year,int month,int day);
```

Requires a real date in1900..2999 (`IllegalArgumentException`). Resets to its month page and notifies range changes.

### date

```java
public int[] date();
```

Returns a new [year,month,day] array of stored date state, distinct from the page’s first date and touch candidate. Touch alone does not update it; the owner calls setDate to apply a confirmed date.

## Language and display

### setLocale

```java
public void setLocale(Locale locale);
```

Changes month titles and regional first weekday, retaining explicit weekday configuration. Null throws `IllegalArgumentException`.

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

### setColors

```java
public void setColors(int selection);
```

Derives month-title face from selection color; does not recolor atlas numerals.

## Year layout and geometry

### getMonthOffset

```java
public int getMonthOffset();
```

Returns first displayed month offset relative to January of current year; may be negative on cross-year pages.

### getVisibleMonthCount

```java
public int getVisibleMonthCount();
```

Returns geometry-selected visible month count1..12.

### getColumnCount

```java
public int getColumnCount();
```

Returns column count, one per160 logical pixels, minimum1.

### getAtlasMode

```java
public int getAtlasMode();
```

Returns atlas mode0 (92-pixel body) or1 (132-pixel body), chosen for client size.

### getTitleHeight

```java
public int getTitleHeight();
```

Returns font-derived logical-pixel title height.

### monthTitleBounds

```java
public Rect monthTitleBounds(int index);
```

Returns title Rect for tile0..11 on the current page, including offscreen tiles. Invalid indices throw `IllegalArgumentException`.

### onSizeChanged

```java
protected void onSizeChanged(int w,int h,int oldw,int oldh);
```

Android measurement/layout/drawing hooks update control geometry and selection using logical coordinates/current font metrics. Do not invoke directly.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Updates fonts/derived face color and cancels touch. Font changes remeasure the visible month count while retaining the first displayed month.

## Navigation, input, and lifetime

### showYearStart

```java
public void showYearStart();
```

Displays from January of the current year without changing selected month. Retains requests before layout and notifies the range callback.

### movePage

```java
public void movePage(int direction);
```

Negative direction moves back by the visible count; positive moves forward; zero does nothing. Ignores years outside1900..2999. Changed pages notify the range callback.

### onKeyDown

```java
public boolean onKeyDown(int keyCode,KeyEvent event);
```

DPAD_UP/DOWN moves the displayed months by one tile row (current columns). Other keys pass to Android.

### onDraw

```java
protected void onDraw(Canvas canvas);
```

Android measurement/layout/drawing hooks update control geometry and selection using logical coordinates/current font metrics. Do not invoke directly.

### dateAt

```java
public int[] dateAt(float x,float y);
```

Returns a new date array for View coordinates or null for dead space. Split date cells use their triangular hit regions.

### onTouchEvent

```java
public boolean onTouchEvent(MotionEvent event);
```

DOWN/MOVE updates the date candidate and enlarged bubble; moving to dead space clears it. UP synchronously calls choose only if the last DOWN/MOVE candidate remains, without recalculating UP coordinates. Cancellation/multi-touch clears it without notification. Stored date() is unchanged; the owner applies a confirmed date with setDate.

### performClick

```java
public boolean performClick();
```

Android accessibility/click dispatch hook. Actual date/command selection follows the user-selection contract above.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Android detach hook; cancels the current touch selection.

## OnDayClickListener

```java
public interface OnDayClickListener
```

### choose

```java
void choose(int year,int month,int day);
```

Shows a candidate-date bubble during touch movement and reports the final candidate on UP. Dead space/cancel does not notify. Month is one-based. Null removes the listener.

## OnRangeChangedListener

```java
public interface OnRangeChangedListener
```

### changed

```java
void changed(int year,int month,int day);
```

Synchronously reports current year/month/day when date, month page, or visible count changes, and on showYearStart. Geometry getters/resizing can also trigger this callback. Null removes the listener.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmYearView.java`
