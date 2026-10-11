---
title: "WmMonthView"
slug: en/api/wmmonthview
---
[API index](/en/api/)

```java
public final class WmMonthView extends View implements WmControls.RestyleListener
```

Calendar month view with42 dates, touch highlighting, and appointment decorations. Distinct from popup WmMonthCalendar. Fits cells to the client size.

## Setup

### Constructors

```java
public WmMonthView(Context context,Typeface font,Locale locale);
public WmMonthView(Context context,Typeface regular,Typeface bold,Locale locale);
```

Starts in the current month with its first day as the window anchor. The 42 cells start at the first weekday on or before that anchor. Until explicitly configured, the first weekday follows `locale`. The single-font overload derives bold. CJK calendar numbers/labels use regular; other locales use bold.

## Date and week selection

### setOnDayClickListener

```java
public void setOnDayClickListener(OnDayClickListener listener);
```

Reports the actual date on completed same-cell UP, including adjacent months. Month is one-based. Null removes the listener.

### setOnCommandListener

```java
public void setOnCommandListener(WmCalendarHeader.OnCommandListener listener);
```

An enabled control with a non-null listener sends `WmCalendarHeader.PREVIOUS` on each delivered Left/PageUp DOWN and `NEXT` on Right/PageDown DOWN. Repeat deliveries each send one command; the event repeat count does not multiply commands. UP adds no command. For these horizontal commands, the owner updates the selected date and month; the widget does not change its month independently. No day/week click is emitted. Null removes the listener. Disabled controls and horizontal keys without a listener use the normal View key path. Up/Down use the separate window-navigation contract below; other keys use the normal View key path. OS repeat generation and native focus traversal are outside this contract.

### setOnRangeChangedListener

```java
public void setOnRangeChangedListener(OnRangeChangedListener listener);
```

An enabled control handles each delivered Up/Down DOWN without requiring a listener. Up subtracts 7 days from the window anchor; Down adds 7. Rebuilds the 42 cells, cancels touch selection, and updates the displayed representative month. A non-null listener receives that month synchronously before the pulse restart and invalidation, even when the representative month is unchanged. Null removes only the callback, not navigation. No day/week click or horizontal command is emitted. Key release adds no movement; delivered repeats each move once regardless of the event repeat count.

The year guard probes the old first cell minus 1 day for Up (year must be >=1900), or plus 42 days for Down (year must be <=2999). A rejected DOWN is consumed without state change or callback. This native edge rule does not guarantee that every cell in the shifted window remains within 1900..2999.

After shifting, the representative month is the new first cell's month unless `daysInMonth - firstCellDay < 21`; then it is the following month. Exactly 21 retains the first cell's month. These civil-date rules do not claim complete native focus traversal or rendering equivalence.

### setWeekNumbersVisible

```java
public void setWeekNumbersVisible(boolean visible);
```

Shows the week gutter (default false), resetting touch/geometry.

### setOnWeekClickListener

```java
public void setOnWeekClickListener(OnWeekClickListener listener);
```

A completed week-gutter tap reports the second date of that row (Monday/Tuesday depending on ordering). Null removes the listener.

### getWeekColumnWidth

```java
public int getWeekColumnWidth();
```

Returns current gutter width in logical pixels, zero when hidden.

### rowWeekNumber

```java
public int rowWeekNumber(int row);
```

Returns native Jan4-based week number for the second date in row0..5. Invalid rows throw `IllegalArgumentException`.

## Month, language, and display

### setMonth

```java
public void setMonth(int year,int month);
```

Requires year>=1/month1..12; invalid values throw `IllegalArgumentException`. Sets the displayed month and resets the window anchor to its first day, even when the month is unchanged. Rebuilds cells/today pulse/touch state while retaining day decorations and the lunar converter. Does not emit a range callback. To restore a shifted window, call `setMonth` before `setWindowAnchor`.

### getWindowAnchor

```java
public int[] getWindowAnchor();
```

Returns a new `[year,month,day]` array, with month1..12. The anchor determines the first cell through the configured first weekday; it is independent of the displayed representative month and is not necessarily the first cell. Editing the returned array does not alter the View.

### setWindowAnchor

```java
public void setWindowAnchor(int year,int month,int day);
```

Requires year>=1, month1..12, and a valid calendar day. Invalid input throws `IllegalArgumentException` before changing the existing anchor. The setter does not impose the keyboard's 1900..2999 edge guard.

Sets the anchor and rebuilds its 42-cell window without changing the displayed month. Cancels pending touch selection and restarts today's pulse. Retains day decorations and the lunar converter; emits no range/day/week callback. `setMonth` resets this anchor, so restore it afterward.

### setLocale

```java
public void setLocale(Locale locale);
```

Refreshes weekday labels, font role, cell metrics, and pulse. Preserves the displayed month and window anchor. Recomputes the first cell from that anchor and the new locale's first weekday unless explicitly configured. Null throws `IllegalArgumentException`; no range callback is emitted.

### setFirstDayOfWeek

```java
public void setFirstDayOfWeek(int day);
```

Sets Calendar.SUNDAY or Calendar.MONDAY; other values throw IllegalArgumentException. Preserves the displayed month and window anchor, recomputes its first cell, and cancels pending touch selection. Does not emit a range callback.

### getFirstDayOfWeek

```java
public int getFirstDayOfWeek();
```

Returns the configured first weekday, or the current Locale’s first weekday before explicit configuration.

### setColors

```java
public void setColors(int selection,int face);
```

Sets ARGB selection and weekday-header face colors directly.

### clearDayFlags

```java
public void clearDayFlags();
```

Clears every stored date decoration.

### setDayFlags

```java
public void setDayFlags(int year,int month,int day,int value);
```

Replaces a date-key decoration. Low12 bits select the upper triangle; next12 bits the lower triangle; bit24 the outline. Both triangle groups produce a filled square. This API does not validate date keys or other bits.

### setLunarCalendar

```java
public void setLunarCalendar(WmLunarCalendar calendar);
```

Default null: no lunar captions. Non-null adds captions over the 42 Gregorian cells; null clears them. Load [WmLunarCalendar](/en/api/wmlunarcalendar/) on a worker, then call this setter on the UI thread. The setter performs no pack I/O. It retains the immutable converter; the SDK includes no original year table.

Gregorian dates, decorations and day/week hit targets remain unchanged. Captions follow row-major order, including adjacent months. If the first cell cannot convert, no captions appear. A later conversion failure keeps earlier captions and stops the remaining cells.

Caption strings are cached when the converter or cell dates change. Widths and font metrics are cached when geometry refreshes. Drawing does not convert dates. Restyling updates the provided regular font; locale changes do not translate the pack's captions.

Captions use the provided regular font at 24 logical pixels, Android font metrics, and black in every state, including pressed cells and today's pulse. The text's measured right edge is 4 logical pixels before the cell's right edge; its measured bottom edge is 2 before the cell's bottom. Width and height are rounded up. No ellipsis is added. The clip starts at the calculated text x/y and ends at the original cell right/bottom; a long caption can extend left of the cell. Painting follows date numbers, decorations, grid lines and the pulse.

These offsets use the SDK's logical 192-DPI border metric. The native lunar paint DC's inherited font remains unidentified; the regular font and Android rasterization are adaptations, not GDI pixel-equivalence claims. This API uses the SDK's API14 baseline and requires no Calendar provider write.

## Geometry

### getHeaderHeight

```java
public int getHeaderHeight();
```

Returns font-derived weekday-header height in logical pixels.

### dateAt

```java
public int[] dateAt(int index);
```

Returns a new [year,month,day] array for cell0..41; month is1..12. Includes adjacent months in row-major order from top left. Editing the array does not alter View dates. Other indices throw IllegalArgumentException.

### cellBounds

```java
public Rect cellBounds(int index);
```

Returns a copied logical-pixel Rect for cell0..41 relative to the View’s top-left origin. Reflects current size and week-number gutter; final size is unknown before layout. Editing it does not alter layout. Other indices throw IllegalArgumentException.

## Styling, input, and lifetime

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Updates regular/bold fonts, selection and derived face color; cancels touch and remeasures grid geometry. Preserves the displayed month, window anchor, day decorations, and lunar converter.

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
public boolean onTouchEvent(MotionEvent event);
```

Android touch path. Completed taps commit according to the class selection contract; cancellation, multi-touch, and movement use that contract. Use normal View dispatch rather than direct calls.

### onKeyDown

```java
public boolean onKeyDown(int keyCode,KeyEvent event);
```

Applies the horizontal owner-command and Up/Down window-navigation contracts above. Handled DOWN returns true, including an Up/Down rejected by its year guard. Other paths delegate to `View.onKeyDown`. No separate key-release navigation is installed.

### performClick

```java
public boolean performClick();
```

Android accessibility/click dispatch hook. Actual date/command selection follows the user-selection contract above.

### onAttachedToWindow

```java
protected void onAttachedToWindow();
```

Starts today-highlight pulses while visible; removes pending pulses when hidden.

### onVisibilityChanged

```java
protected void onVisibilityChanged(View view,int visibility);
```

Starts today-highlight pulses while visible; removes pending pulses when hidden.

### onWindowVisibilityChanged

```java
protected void onWindowVisibilityChanged(int visibility);
```

Starts today-highlight pulses while visible; removes pending pulses when hidden.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Stops pulses and touch selection.

## OnDayClickListener

```java
public interface OnDayClickListener
```

### onDayClick

```java
void onDayClick(int year,int month,int day);
```

Reports the actual date on completed same-cell UP, including adjacent months. Month is one-based. Null removes the listener.

## OnWeekClickListener

```java
public interface OnWeekClickListener
```

### onWeekClick

```java
void onWeekClick(int year,int month,int day);
```

A completed week-gutter tap reports the second date of that row (Monday/Tuesday depending on ordering). Null removes the listener.

## OnRangeChangedListener

```java
public interface OnRangeChangedListener
```

### changed

```java
void changed(int year,int month);
```

Reports the representative month after an accepted Up/Down DOWN; month is1..12. Called synchronously even if that month did not change. The View has already updated its anchor and cells. An owner that replaces the View must retain `getWindowAnchor()`, configure the replacement's month/locale/first weekday, then restore the anchor with `setWindowAnchor`. Calling `setMonth` alone discards the shifted window.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmMonthView.java`
