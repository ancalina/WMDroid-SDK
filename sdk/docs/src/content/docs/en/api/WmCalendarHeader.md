---
title: "WmCalendarHeader"
slug: en/api/wmcalendarheader
---
[API index](/en/api/)

```java
public final class WmCalendarHeader extends View implements WmControls.RestyleListener
```

View-dependent Calendar toolbar. Reports date/month/year/week picker and navigation commands. The owner changes dates and places popups. Height44 and hit coordinates are logical pixels.

## Setup

### Constants and fields

```java
public static final int HEIGHT = 44;
public static final int AGENDA = 0;
public static final int DAY = 1;
public static final int WEEK = 2;
public static final int MONTH = 3;
public static final int YEAR = 4;
public static final int PREVIOUS = 20000;
public static final int NEXT = 20001;
public static final int TODAY = 20052;
public static final int MONTH_PICKER = 20012;
public static final int YEAR_PICKER = 20022;
public static final int WEEK_PICKER = 20032;
public static final int DATE_PICKER = 20053;
public static final int FIRST_WEEKDAY = 20054;
```

Modes AGENDA=0/DAY=1/WEEK=2/MONTH=3/YEAR=4. Commands PREVIOUS=20000/NEXT=20001/TODAY=20052/MONTH_PICKER=20012/YEAR_PICKER=20022/WEEK_PICKER=20032/DATE_PICKER=20053. FIRST_WEEKDAY=20054 through +6 identify weekday buttons in displayed order. HEIGHT=44.

### Constructors

```java
public WmCalendarHeader(Context context,Typeface font,Locale locale);
```

Null locale throws `IllegalArgumentException`. Loads toolbar icons from the current resource pack, with fallback. Initial mode is AGENDA.

## Buttons and commands

### setTodayButtonVisible

```java
public void setTodayButtonVisible(boolean visible);
```

Controls Today button visibility, default true. This is an explicit SDK option. Changes cancel active touch.

### setOnCommandListener

```java
public void setOnCommandListener(OnCommandListener listener);
```

Reports command id on a completed same-button UP. Leaving the button or multi-touch cancels activation. Null removes the listener.

## Language and week numbers

### setLocale

```java
public void setLocale(Locale locale);
```

Updates date captions, month names, weekday layout, and autosizing. Null throws `IllegalArgumentException`.

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

### toolbarWeekNumber

```java
public static int toolbarWeekNumber(int year,int month,int day,int firstDay);
```

Applies the native Jan4 calculation to Monday within the configured Sunday/Monday week. Month is one-based. Invalid dates/first weekdays throw `IllegalArgumentException`. Native late-December week0 is possible; this is not general locale WEEK_OF_YEAR.

### setWeekNumbersVisible

```java
public void setWeekNumbersVisible(boolean visible);
```

Controls only the Week toolbar week picker, separately from view gutters. Default true.

## Date and mode

### setDate

```java
public void setDate(int year,int month,int day);
```

Sets a real date (one-based month), clears preview, and resets year-range text to this year. Invalid dates throw `IllegalArgumentException`.

### setDatePreview

```java
public void setDatePreview(int year,int month,int day);
```

Previews popup dates in toolbar captions without changing the committed date/checked weekday. clear restores committed captions. Invalid preview dates throw `IllegalArgumentException`.

### clearDatePreview

```java
public void clearDatePreview();
```

Clears the preview date and restores committed-date captions; does nothing without a preview.

### setMode

```java
public void setMode(int mode);
```

Accepts AGENDA..YEAR (0..4). A mode change clears preview/touch and rebuilds geometry. Other values throw `IllegalArgumentException`.

### setYearRange

```java
public void setYearRange(int first,int last);
```

Sets Year caption range. Requires first>=1, last>=first, difference0 or1; otherwise `IllegalArgumentException`. Does not change the committed date.

## Styling, geometry, and lifetime

### setColors

```java
public void setColors(int selection);
```

Applies selection color and its derived toolbar face.

### commandBounds

```java
public Rect commandBounds(int id);
```

Returns copied current command bounds, or an empty Rect for an absent command. Width is not finalized before layout.

### onMeasure

```java
protected void onMeasure(int width,int height);
```

Android measurement/layout/drawing hooks update control geometry and selection using logical coordinates/current font metrics. Do not invoke directly.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Cancels gestures and updates regular font, selection, and derived face color; recalculates command geometry at layout.

### onDraw

```java
protected void onDraw(Canvas canvas);
```

Android measurement/layout/drawing hooks update control geometry and selection using logical coordinates/current font metrics. Do not invoke directly.

### onTouchEvent

```java
public boolean onTouchEvent(MotionEvent event);
```

Android touch path. Completed taps commit according to the class selection contract; cancellation, multi-touch, and movement use that contract. Use normal View dispatch rather than direct calls.

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

## OnCommandListener

```java
public interface OnCommandListener
```

### command

```java
void command(int id);
```

Runs synchronously during UI touch handling after a completed same-button tap. id is PREVIOUS/NEXT/TODAY/MONTH_PICKER/YEAR_PICKER/WEEK_PICKER/DATE_PICKER or FIRST_WEEKDAY+0..6. Weekday0 is getFirstDayOfWeek(); later ids follow displayed order. If the first day is Calendar.MONDAY, FIRST_WEEKDAY means Monday. The callback does not change dates or open popups; the owner handles the command.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmCalendarHeader.java`
