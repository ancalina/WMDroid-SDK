---
title: "WmMonthCalendarHeader"
slug: en/api/wmmonthcalendarheader
---
[API index](/en/api/)

```java
public final class WmMonthCalendarHeader extends View implements WmControls.RestyleListener
```

Month/year title and navigation buttons for a date dropdown, separate from the Calendar toolbar. Measures title using the bold face and orders month/year by locale.

### Constructors

```java
public WmMonthCalendarHeader(Context context,Typeface bold,Locale locale);
```

Supplies the title bold face and non-null locale. Null locale throws `IllegalArgumentException`.

### setOnCommandListener

```java
public void setOnCommandListener(WmCalendarHeader.OnCommandListener listener);
```

Reports PREVIOUS/NEXT/MONTH_PICKER/YEAR_PICKER synchronously during UI touch handling after a completed tap. Null removes the listener. Default boundaries disable PREVIOUS when the displayed month equals1900-01 and NEXT when it equals2999-12. These equality-based button checks are separate from setDate’s accepted years1752..9999. The owner chooses a date and calls setDate in response.

### setDate

```java
public void setDate(int year,int month,int day);
```

Sets title year1752..9999/month1..12; day is unused. Invalid year/month throws `IllegalArgumentException`.

### setLocale

```java
public void setLocale(Locale locale);
```

Places year first for CJK and month first otherwise. Refreshes metrics/touch state. Null throws `IllegalArgumentException`.

### setColors

```java
public void setColors(int selection);
```

Sets the ARGB title background; text remains white.

### commandBounds

```java
public Rect commandBounds(int id);
```

Returns copied PREVIOUS/NEXT/MONTH_PICKER/YEAR_PICKER bounds, or an empty Rect for other ids.

### onMeasure

```java
protected void onMeasure(int width,int height);
```

Android measurement/layout/drawing hooks update control geometry and selection using logical coordinates/current font metrics. Do not invoke directly.

### onSizeChanged

```java
protected void onSizeChanged(int w,int h,int oldw,int oldh);
```

Android measurement/layout/drawing hooks update control geometry and selection using logical coordinates/current font metrics. Do not invoke directly.

### onDraw

```java
protected void onDraw(Canvas canvas);
```

Android measurement/layout/drawing hooks update control geometry and selection using logical coordinates/current font metrics. Do not invoke directly.

### onTouchEvent

```java
public boolean onTouchEvent(MotionEvent e);
```

Android touch path. Completed taps commit according to the class selection contract; cancellation, multi-touch, and movement use that contract. Use normal View dispatch rather than direct calls.

### performClick

```java
public boolean performClick();
```

Android accessibility/click dispatch hook. Actual date/command selection follows the user-selection contract above.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Cancels touch, updates bold font/selection color, and remeasures geometry. regular and tabColor are unused.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Android detach hook; cancels the current touch selection.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmMonthCalendarHeader.java`
