---
title: "WmDateTimeField"
slug: en/api/wmdatetimefield
---
[API index](/en/api/)

```java
public final class WmDateTimeField extends FrameLayout implements WmControls.RestyleListener
```

Combines a segmented date field and editable time combo. Values are civil date/time, without timezone or epoch. The owner places popups and saves data. Use view mutations and callbacks on the UI thread.

The public constructor uses 1900..2999. A control returned by `WmTaskEditor.reminderField()` instead uses 1752-09-14..9999-12-31, a required date, and the task card appearance. Its value setters and interaction-state validation use that range.

### Constructors

```java
public WmDateTimeField(Context context,Typeface font,Locale locale,Value value,int accent);
```

Requires non-null font, locale, and value, with year 1900..2999; otherwise throws `IllegalArgumentException`. Date is required. Initial time candidates are 48 half-hour slots starting at 00:00.

### dateChoice

```java
public WmDateField dateChoice();
```

Returns the owned `WmDateField`; do not reparent it. Do not replace its open/value-change listeners: they connect date changes to composite state. Register popup opening with `setOnDateOpen` and value observation with the composite `setOnValueChangedListener`.

### timeChoice

```java
public WmEditableChoice timeChoice();
```

Returns the owned `WmEditableChoice`; do not reparent it. Its value-change listener parses time and synchronizes the last valid/notified values. Do not replace this listener or its open listener. Register popup opening with `setOnTimeOpen` and value observation with the composite `setOnValueChangedListener`. Incomplete time text remains a draft.

### dateValue

```java
public Value dateValue();
```

Returns the displayed date with the last valid clock, including during incomplete time input. Invalid/out-of-range dates throw `IllegalArgumentException`.

### getValue

```java
public Value getValue();
```

Returns the displayed date and strictly parsed complete time text. Empty/incomplete time or a date outside this control’s range throws `IllegalArgumentException`.

### value

```java
public Value value();
```

Returns the displayed date and strictly parsed complete time text. Empty/incomplete time or a date outside this control’s range throws `IllegalArgumentException`.

### setValue

```java
public void setValue(Value value);
```

Silently replaces the value, time draft, and last-notified value, using the current format. Requires a date within this control’s range and does not invoke the change callback.

### selectDate

```java
public void selectDate(int year,int month,int day);
```

Changes only the date, unless disabled. Preserves invalid time drafts and reports the combined change once time becomes valid. Invalid dates throw `IllegalArgumentException`.

### setOnDateOpen

```java
public void setOnDateOpen(OnDateOpen listener);
```

Registers an opening request for an enabled field. The source is this composite. Null leaves the request unhandled.

### setOnTimeOpen

```java
public void setOnTimeOpen(OnTimeOpen listener);
```

Registers an opening request for an enabled field. The source is this composite. Null leaves the request unhandled.

### setOnValueChangedListener

```java
public void setOnValueChangedListener(OnValueChangedListener listener);
```

Reports valid user changes differing from the last-notified value as `(oldValue,newValue)`. Programmatic replacement/restoration is silent. Null removes the listener.

### setTimeOrigin

```java
public void setTimeOrigin(int hour,int minute);
```

Sets candidate origin (hour 0..23, minute 0..59), then advances by 30 minutes modulo one day. Does not change the current value. Invalid times throw `IllegalArgumentException`.

### setLocale

```java
public void setLocale(Locale locale);
```

Reformats valid time and retains bounded caret positions. Invalid drafts retain their previous parser until the next valid edit. Null locale throws `IllegalArgumentException`.

### saveInteractionState

```java
public Bundle saveInteractionState();
```

Returns a schema1 Bundle with civil/last-notified values, literal time draft, candidates, caret/focus, parser, and origin. Excludes popups, IME composition, theme, and enabled policy.

### validateInteractionState

```java
public void validateInteractionState(Bundle state);
```

Validates the complete schema1 snapshot against this date format/range before mutation. Malformed types, schema, candidates, parser, origin, or focus throw `IllegalArgumentException`.

### restoreInteractionState

```java
public void restoreInteractionState(Bundle state);
```

Validates, then silently replaces value, drafts, and interaction state. Close date/time popups first. Open popups or reentry throw `IllegalStateException`; malformed snapshots throw `IllegalArgumentException` without replacing state.

### setEnabled

```java
public void setEnabled(boolean enabled);
```

Propagates enabled state to owned inputs and blocks user changes while disabled.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Restyles date/time children and remeasures their combined width.

### onMeasure

```java
protected void onMeasure(int widthSpec,int heightSpec);
```

Android measurement/layout/drawing hooks update control geometry and selection using logical coordinates/current font metrics. Do not invoke directly.

### onLayout

```java
protected void onLayout(boolean changed,int left,int top,int right,int bottom);
```

Android measurement/layout/drawing hooks update control geometry and selection using logical coordinates/current font metrics. Do not invoke directly.

## Value

```java
public static final class Value
```

### Constants and fields

```java
public final int year;
public final int month;
public final int day;
public final int hour;
public final int minute;
```

Immutable civil year/month/day/hour/minute. Months are 1..12, hours 0..23, minutes 0..59.

### Constructors

```java
public Value(int year,int month,int day,int hour,int minute);
```

Requires year ≥1 and a real proleptic Gregorian date/time. Invalid values throw `IllegalArgumentException`. The value type accepts a wider range than the editor, whose range depends on its owner as described above.

### equals

```java
public boolean equals(Object other);
```

Uses the five civil fields for value equality.

### hashCode

```java
public int hashCode();
```

Hashes the five civil fields; equal values have equal hashes.

## OnDateOpen

```java
public interface OnDateOpen
```

### open

```java
void open(WmDateTimeField source);
```

Requests the owner to present the corresponding popup. Commit its selection through the field selection APIs.

## OnTimeOpen

```java
public interface OnTimeOpen
```

### open

```java
void open(WmDateTimeField source);
```

Requests the owner to present the corresponding popup. Commit its selection through the field selection APIs.

## OnValueChangedListener

```java
public interface OnValueChangedListener
```

### changed

```java
void changed(Value oldValue,Value newValue);
```

Synchronous notification of a completed civil-value change.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmDateTimeField.java`
