---
title: "WmDateField"
slug: en/api/wmdatefield
---
[API index](/en/api/)

```java
public final class WmDateField extends View implements WmControls.RestyleListener,WmControls.LanguageListener
```

Nullable segmented date-only View. Default range: 1752-09-14..9999-12-31, with proleptic Gregorian arithmetic. The owner presents the popup and saves values. Normalizes time fields to 00:00.

## Setup

### Constructors

```java
public WmDateField(Context context,Typeface font,Locale locale,WmDateTimeField.Value value,int accent);
public WmDateField(Context context,Typeface font,Locale locale,WmDateTimeField.Value value,int accent,boolean shortDateFormat);
```

Requires non-null font/locale and a null or valid date in the default range. `shortDateFormat=true` uses regional short dates; false (default) uses a four-digit year. Invalid values throw `IllegalArgumentException`. Short yy expands to yyyy when selected but ignores numeric year input.

## Values, range, and serialization

### value

```java
public WmDateTimeField.Value value();
```

Returns the normalized current date or null. Incomplete numeric text is separate from the last valid value.

### setRange

```java
public void setRange(WmDateTimeField.Value minimum,WmDateTimeField.Value maximum);
```

Requires ordered, non-null date bounds within the default full range; ignores time fields. Silently clamps the current date and clears drafts/touch state. Invalid bounds throw `IllegalArgumentException` without mutation.

### setAllowEmpty

```java
public void setAllowEmpty(boolean allowEmpty);
```

Controls nullable dates. Setting false while the current date is null throws `IllegalArgumentException`.

### parseValue

```java
public static WmDateTimeField.Value parseValue(String canonical);
```

Parses locale-independent ASCII `yyyy-MM-dd`; empty text means null. Null, malformed, impossible, or out-of-range dates throw `IllegalArgumentException`.

### formatValue

```java
public static String formatValue(WmDateTimeField.Value value);
```

Returns ASCII `yyyy-MM-dd`, or empty text for null. Ignores time; dates outside the default range throw `IllegalArgumentException`.

## Interaction state

### saveInteractionState

```java
public Bundle saveInteractionState();
```

Returns schema2 selected-segment and pending-digit state only. Save the date separately with `formatValue`.

### validateInteractionState

```java
public static void validateInteractionState(Bundle state,WmDateTimeField.Value value);
```

Validates schema1/2 against the supplied nullable date. Pending digits must be incomplete ASCII input. Invalid schema, field, or digit/date combinations throw `IllegalArgumentException` without mutation.

### restoreInteractionState

```java
public void restoreInteractionState(Bundle state);
```

Validates against current value, format, and range, then silently restores selection and digits. Restore the date first. Invalid snapshots throw `IllegalArgumentException`.

## Date editing and segment selection

### setValue

```java
public void setValue(WmDateTimeField.Value value);
```

Validates range/nullability, then silently replaces the value and clears pending digits/touch state. Does not invoke the change callback. Invalid values throw `IllegalArgumentException`.

### selectDate

```java
public void selectDate(int year,int month,int day);
```

Validates the real date(month1..12) and current range first. Invalid input throws IllegalArgumentException even while disabled. Valid selections do not change disabled controls. Enabled controls apply the date and synchronously notify change when different.

### clearDate

```java
public void clearDate();
```

Clears and notifies when enabled and nullable; otherwise retains the value.

### selectedField

```java
public int selectedField();
```

Returns Calendar.YEAR/MONTH/DAY_OF_MONTH; an unselected field uses MONTH as fallback.

### selectField

```java
public void selectField(int field);
```

Accepts only YEAR/MONTH/DAY_OF_MONTH. Switching segments commits pending digits when enabled. Invalid roles throw `IllegalArgumentException`.

## Language, popups, and callbacks

### setLocale

```java
public void setLocale(Locale locale);
```

Changes segment order, captions, and date format while retaining value and canceling touch. Null or a format unavailable as SimpleDateFormat throws `IllegalArgumentException`.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

Passes the shared catalog language’s Locale to setLocale; state preservation and popup handling follow that method’s contract.

### bindDropdown

```java
public void bindDropdown(View popup);
```

Binds the owner-presented popup View; the owner creates and dismisses it.

### isDroppedDown

```java
public boolean isDroppedDown();
```

Returns true while the bound popup is shown.

### setOnOpenListener

```java
public void setOnOpenListener(OnOpenListener listener);
```

Commits digits and clears segment selection before requesting dropdown opening. Passes the enabled source field. A null listener leaves the request unhandled.

### setOnValueChangedListener

```java
public void setOnValueChangedListener(OnValueChangedListener listener);
```

Synchronously reports old/new nullable dates for user changes. Equal values, silent setters, and restoration do not notify. Null removes the listener.

## Keyboard and touch

### onKeyDown

```java
public boolean onKeyDown(int key,KeyEvent event);
```

DPAD_CENTER/F4/PAGE_DOWN requests dropdown. Left/right changes date segment; up/down and numpad +/- adjust values; HOME/END uses the segment endpoint; Ctrl+HOME selects today. Separators advance segments; localized/fullwidth digits enter the draft. Commit/range handling follows the input contract above.

### onKeyUp

```java
public boolean onKeyUp(int key,KeyEvent event);
```

Consumes releases of date-editing keys while enabled; other keys pass to Android.

### onTouchEvent

```java
public boolean onTouchEvent(MotionEvent event);
```

Completed body taps select a segment; the arrow opens the owner dropdown. Fields returned by `WmTaskEditor` instead open the calendar from the entire body and show a full-year caption while closed. Cancellation, multi-touch and movement outside the target cancel activation. Use normal View dispatch.

### performClick

```java
public boolean performClick();
```

Returns false when disabled. Requests focus, activates an empty date as today clamped to the range, then dispatches Android click. Returns false if callbacks disable the field.

## Styling, focus, and lifetime

### setBordered

```java
public void setBordered(boolean value);
```

Enables the standalone DATE outline. Inline fields are unbordered by default. Does not change value, padding, or range.

### onMeasure

```java
protected void onMeasure(int widthSpec,int heightSpec);
```

Android measurement/layout/drawing hooks update control geometry and selection using logical coordinates/current font metrics. Do not invoke directly.

### onDraw

```java
protected void onDraw(Canvas canvas);
```

Android measurement/layout/drawing hooks update control geometry and selection using logical coordinates/current font metrics. Do not invoke directly.

### setEnabled

```java
public void setEnabled(boolean enabled);
```

Disabling cancels touch, digit drafts, and dropdown pressed state while retaining the committed date.

### onFocusChanged

```java
protected void onFocusChanged(boolean gain,int direction,Rect previous);
```

Android calls this on focus changes. Gaining focus with no selected segment selects the day segment, or the weekday segment in recurrence-date format. Losing focus while enabled commits incomplete digits. A changed valid date can synchronously invoke the value-change listener. In four-digit year format, short input 0..29 expands to 2000..2029 and 30..99 to 1930..1999; entering `25` then moving focus commits 2025. Invalid results retain the last date. Two-digit `yy` format does not accept year-digit input. Do not invoke directly.

### onSizeChanged

```java
protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight);
```

Android measurement/layout/drawing hooks update control geometry and selection using logical coordinates/current font metrics. Do not invoke directly.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Cancels touch and dropdown pressed state.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Null regular throws IllegalArgumentException. Updates regular font/selection color, cancels touch, and remeasures display geometry while retaining the date.

## OnValueChangedListener

```java
public interface OnValueChangedListener
```

### changed

```java
void changed(WmDateTimeField.Value oldValue,WmDateTimeField.Value newValue);
```

Synchronously reports old/new nullable dates for user changes. Equal values, silent setters, and restoration do not notify. Null removes the listener.

## OnOpenListener

```java
public interface OnOpenListener
```

### open

```java
void open(WmDateField field);
```

Commits digits and clears segment selection before requesting dropdown opening. Passes the enabled source field. A null listener leaves the request unhandled.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmDateField.java`
