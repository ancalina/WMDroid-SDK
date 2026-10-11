---
title: "WmCalendarPicker"
slug: en/api/wmcalendarpicker
---
[API index](/en/api/)

```java
public final class WmCalendarPicker extends LinearLayout implements WmControls.RestyleListener,WmControls.BackHandler
```

Date dropdown combining month title, calendar, month list, and inline year editor. The owner places and dismisses it.

### Constructors

```java
public WmCalendarPicker(Context context,Typeface font,Locale locale,int year,int month,int day,int selection,OnDateListener listener);
public WmCalendarPicker(Context context,Typeface regular,Typeface bold,Locale locale,int year,int month,int day,int selection,OnDateListener listener);
public WmCalendarPicker(Context context,Typeface regular,Typeface bold,Locale locale,int year,int month,int day,int selection,OnDateListener listener,WmDateTimeField.Value minimum,WmDateTimeField.Value maximum);
```

Requires locale and confirmation listener, with the initial date inside bounds. Default1900-01-01..2999-12-31; final overload accepts date-only full-date bounds. Invalid dates/bounds or null locale/listener throw `IllegalArgumentException`. The one-face overload derives bold.

### naturalSize

```java
public android.graphics.Point naturalSize();
```

Returns a new Point containing the preferred client size. `x` and `y` use SDK logical pixels and exclude the owner's outer frame. Clamp the size to available space before placement.

The calculation uses current bold-font Android text metrics and Locale month, weekday, and footer text. It also accounts for week-number visibility and the None footer. It adapts the original 192DPI sizing formula to Android metrics; it does not guarantee identical GDI pixels.

### onMeasure

```java
protected void onMeasure(int width,int height);
```

Uses the larger of `naturalSize()` and the View minimum for each preferred dimension. AT_MOST caps it to the parent limit; UNSPECIFIED uses the preferred size. EXACTLY uses the size specified by the parent.

### back

```java
public boolean back();
```

Commits and finishes an open year editor (does not cancel its draft), or dismisses the month list. Returns false with no nested selection.

### dispatchTouchEvent

```java
public boolean dispatchTouchEvent(MotionEvent event);
```

Android touch dispatch hook. DOWN outside an open inline year editor finishes the editor and commits its input first. The gesture is consumed through UP/CANCEL, so that same tap does not activate the underlying date or toolbar button; tap again to select it. Gestures starting inside the editor are forwarded with local coordinate conversion. Application code normally does not call this directly.

### saveInteractionState

```java
public Bundle saveInteractionState();
```

Returns schema1 nested month/year state, scroll, year draft, and selection. Save the selected date separately through date().

### validateInteractionState

```java
public static void validateInteractionState(Bundle state);
```

Validates schema1 choice/scroll/ASCII year draft/caret. Allows empty/out-of-range pending years without committing. Malformed structure throws `IllegalArgumentException`.

### restoreInteractionState

```java
public void restoreInteractionState(Bundle state);
```

Validates, replaces nested choice state, and restores month scroll or year draft while invalidating old callbacks. Invalid snapshots throw `IllegalArgumentException`. Does not restore the date itself.

### setOnDateChangeListener

```java
public void setOnDateChangeListener(OnDateListener listener);
```

Reports changed dates from month/year navigation, date taps, or [calendar keyboard movement](/en/api/wmmonthcalendar/#onkeydown). Grid changes update the current date and header first. A changed-date tap notifies before the constructor confirmation callback; a tap on the selected date calls only confirmation. Keyboard movement does not call confirmation. Null removes the listener.

### setOnNoneListener

```java
public void setOnNoneListener(Runnable listener);
```

Adds/removes the None footer. None runs its listener without changing date().

### setFirstDayOfWeek

```java
public void setFirstDayOfWeek(int day);
```

Sets the body’s first weekday to Calendar.SUNDAY/MONDAY; other values throw IllegalArgumentException.

### setWeekNumbersVisible

```java
public void setWeekNumbersVisible(boolean visible);
```

Controls the body’s week-number gutter visibility.

### date

```java
public int[] date();
```

Returns a new `[year,month,day]` array including the navigated date, with one-based month.

### setLocale

```java
public void setLocale(Locale locale);
```

Updates title/body captions and an open month list/year editor, retaining month-list scroll. Null throws `IllegalArgumentException`.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Restyles header, month grid, and open choice menu; updates inline year-editor position.

## OnDateListener

```java
public interface OnDateListener
```

### choose

```java
void choose(int year,int month,int day);
```

The constructor listener confirms date-cell or Today-footer taps. The separate change listener reports changed dates from navigation, taps, or keyboard movement. Month is one-based.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmCalendarPicker.java`
