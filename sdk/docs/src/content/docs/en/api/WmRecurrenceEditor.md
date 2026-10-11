---
title: "WmRecurrenceEditor"
slug: en/api/wmrecurrenceeditor
---
[API index](/en/api/)

```java
public final class WmRecurrenceEditor extends FrameLayout implements WmControls.BackHandler,WmControls.RestyleListener,WmControls.LanguageListener
```

Provider-free recurrence wizard for time/pattern/end range; all-day omits the time page. The caller owns timezone, RRULE compatibility, and saving. Dates1900..2999; interval/count input limited to three digits.

## Setup

### Constructors

```java
public WmRecurrenceEditor(Context context,Typeface font,Locale locale,Value initial,int accent,int tabColor);
```

Requires font/locale/initial and duration0..94370400 minutes; all-day requires positive multiples of1440. Invalid durations/dates throw `IllegalArgumentException`. Intervals/counts above999 or undisplayable ordinal/groups remain errors rather than silently saving substitute rules. A NONE seed initially selects weekly recurrence on the start weekday, with interval1 and default count10; start/until/duration retain the seed values. The caller must accept or cancel that draft.

View accessors return Views owned by the editor. Reparenting them or replacing their internal listeners can break wizard synchronization. Silent `setSelectedMask()`, date/time `setValue()`, and RadioButton `setChecked()` do not run the editor’s selection/click synchronization. Use notified selection/click paths to refresh panels, paired radios, and start-date linkage. Count input notifications select COUNT; UNTIL date notifications select UNTIL. Those two fields remain enabled regardless of the current end mode.

Numeric fields use a default text size of 24 logical pixels. Construction uses the supplied font; paired restyling uses the regular face.

### PageUp/PageDown

| Fields | List client height (logical pixels) |
| --- | ---: |
| [durationField](#durationfield), [countField](#countfield) | 166 |
| [dailyIntervalField](#dailyintervalfield), [weeklyIntervalField](#weeklyintervalfield), [monthlyDayIntervalField](#monthlydayintervalfield), [monthlyOrdinalIntervalField](#monthlyordinalintervalfield), [dayField](#dayfield), [yearDayField](#yeardayfield) | 170 |

When enabled and closed, with `isExtendedUi()==false`, no modifiers, and no active arrow press, PageUp/PageDown moves backward/forward by a page and clamps to the candidate range. `rowHeight=max(1,fontMetrics.descent-fontMetrics.ascent)` and `rows=height/rowHeight` use integer division. The step is rows−1 when rows>1, otherwise rows. Height stays fixed; the step follows the current font.

Opening a numeric field passes its tabled client height to [choiceDropdownHeight](/en/api/wmradiomenu/#choicedropdownheight), which computes the outer height from item count and font rows. ordinalSelector, groupSelector, yearlyDayMonthSelector, yearlyOrdinalMonthSelector, yearOrdinalSelector, and yearGroupSelector use a client cap of 170 logical pixels. Final popup height is capped at the editor's `max(0,height−60)`.

## Editor fields and patterns

### wizard

```java
public WmWizard wizard();
```

Page/navigation/finish/cancel controller.

### patternSelector

```java
public WmBoxSelect patternSelector();
```

Single-bit daily/weekly/monthly/yearly1/2/4/8 selector.

### weekdaySelector

```java
public WmBoxSelect weekdaySelector();
```

Locale-ordered weekday selector (result rules use Sunday-based bits).

### startField

```java
public WmDateTimeField startField();
```

Returns the editor-owned WmDateTimeField holding start date/time. The Time page displays a start–end caption instead of this View. Do not treat it as an attached input or move it to another parent. Programmatic changes follow the owned-view and notification contracts above.

### rangeStartField

```java
public WmDateTimeField rangeStartField();
```

Range-page start date. A notified date change later than the current end date also raises the end date. This correction is silent: it retains the end mode and the end field's time. Silent `setValue()` and interaction restoration do not apply this correction.

### untilField

```java
public WmDateTimeField untilField();
```

UNTIL date candidate.

### durationField

```java
public WmEditableChoice durationField();
```

Literal duration with unit. [PageUp/PageDown](#pageuppagedown).

### intervalField

```java
public WmEditableChoice intervalField();
```

Returns [dailyIntervalField](#dailyintervalfield) for daily, [weeklyIntervalField](#weeklyintervalfield) for weekly, or [monthlyIntervalField](#monthlyintervalfield) otherwise.

### dailyIntervalField

```java
public WmEditableChoice dailyIntervalField();
```

Daily interval. [PageUp/PageDown](#pageuppagedown).

### weeklyIntervalField

```java
public WmEditableChoice weeklyIntervalField();
```

Weekly interval. [PageUp/PageDown](#pageuppagedown).

### monthlyIntervalField

```java
public WmEditableChoice monthlyIntervalField();
```

Returns [monthlyOrdinalIntervalField](#monthlyordinalintervalfield) when monthlyOrdinalRadio is selected, or [monthlyDayIntervalField](#monthlydayintervalfield) otherwise.

### monthlyDayIntervalField

```java
public WmEditableChoice monthlyDayIntervalField();
```

Monthly day interval. [PageUp/PageDown](#pageuppagedown).

### monthlyOrdinalIntervalField

```java
public WmEditableChoice monthlyOrdinalIntervalField();
```

Monthly ordinal interval. [PageUp/PageDown](#pageuppagedown).

### countField

```java
public WmEditableChoice countField();
```

End occurrence count. [PageUp/PageDown](#pageuppagedown).

### dayField

```java
public WmEditableChoice dayField();
```

Monthly day1..31. [PageUp/PageDown](#pageuppagedown).

### yearDayField

```java
public WmEditableChoice yearDayField();
```

Yearly day1..31. [PageUp/PageDown](#pageuppagedown).

### ordinalSelector

```java
public WmChoice ordinalSelector();
```

Monthly first/second/third/fourth/last ordinal.

### groupSelector

```java
public WmChoice groupSelector();
```

Monthly day/weekday/weekend/single-weekday group.

### monthSelector

```java
public WmChoice monthSelector();
```

Month for current yearly day/ordinal mode.

### yearlyDayMonthSelector

```java
public WmChoice yearlyDayMonthSelector();
```

Month for yearly day mode.

### yearlyOrdinalMonthSelector

```java
public WmChoice yearlyOrdinalMonthSelector();
```

Month for yearly ordinal mode.

### yearOrdinalSelector

```java
public WmChoice yearOrdinalSelector();
```

Yearly ordinal.

### yearGroupSelector

```java
public WmChoice yearGroupSelector();
```

Yearly weekday group.

### dailyIntervalRadio

```java
public RadioButton dailyIntervalRadio();
```

Daily interval mode.

### dailyWeekdaysRadio

```java
public RadioButton dailyWeekdaysRadio();
```

Every weekday mode.

### monthlyDayRadio

```java
public RadioButton monthlyDayRadio();
```

Monthly day mode.

### monthlyOrdinalRadio

```java
public RadioButton monthlyOrdinalRadio();
```

Monthly ordinal mode.

### yearlyDayRadio

```java
public RadioButton yearlyDayRadio();
```

Yearly day mode.

### yearlyOrdinalRadio

```java
public RadioButton yearlyOrdinalRadio();
```

Yearly ordinal mode.

### neverRadio

```java
public RadioButton neverRadio();
```

Never-end range.

### untilRadio

```java
public RadioButton untilRadio();
```

UNTIL range.

### countRadio

```java
public RadioButton countRadio();
```

COUNT range.

## Finish, cancel, and values

### setOnFinish

```java
public void setOnFinish(Runnable listener);
```

Runs synchronously after validated Finish or Remove Recurrence closes the popup. Read value(), save, and dismiss in the owner. Null removes the callback; a non-null callback is retained until replaced. Finish catches `IllegalArgumentException` from both validation and the callback and displays its message through the validation box. Remove Recurrence first marks the draft removed and clears its pattern; callback exceptions propagate from that path. Neither path rolls back callback side effects.

### setOnCancel

```java
public void setOnCancel(Runnable listener);
```

Closes popup and synchronously invokes the callback on Cancel without saving original data. Null removes it; a non-null callback is retained until replaced. Callback exceptions propagate after popup dismissal.

### value

```java
public Value value();
```

Validates duration, active pattern/interval/weekdays/date, and range into a new Value. UNTIL cannot precede start date; COUNT is positive with at most three digits; duration cannot exceed the rule period bound. Violations throw `IllegalArgumentException`. Removed recurrence returns NONE with original start/until/duration.

## Interaction state

### saveInteractionState

```java
public Bundle saveInteractionState();
```

Returns schema1 original seed, all literal drafts/selections, wizard page, error, and focus without parsing/blur commit. Excludes nested popups and IME composition.

### initialValue

```java
public static Value initialValue(Bundle state);
```

Extracts/validates the original Value to construct a replacement editor. Validate full drafts afterward through that editor. Schema1 requires exactly16 keys and its original seed exactly5 keys. Invalid seed/schema throws `IllegalArgumentException`.

### validateInteractionState

```java
public void validateInteractionState(Bundle state);
```

Requires an original seed matching this editor. Validates child states, draft lengths, candidates, selections, weekdays, and focus consistency. Invalid structure throws `IllegalArgumentException`. Final semantics of incomplete drafts remain for value().

### restoreInteractionState

```java
public void restoreInteractionState(Bundle state);
```

Close every nested popup first. Validates, then silently restores the whole draft. Reentry is checked first; full validation precedes the open-popup checks and owner mutations. Open popups/reentry throw `IllegalStateException`; malformed snapshots throw `IllegalArgumentException`. Applying values or external listener exceptions has no rollback guarantee.

## Language, popups, and lifetime

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

Passes the shared catalog language’s Locale to setLocale; state preservation and popup handling follow that method’s contract.

### setLocale

```java
public void setLocale(Locale next);
```

Relabels while retaining weekday model bits, duration input grammar, drafts/caret. Reformats a valid duration only when its raw input equals the old locale’s `durationLabel(parsedMinutes)`, including formatted durations outside the candidate list. Other literal input retains its parsing Locale until edited. Closes ordinary popups and relabels error message boxes. An equal Locale is a no-op, including popup handling. Null throws `IllegalArgumentException`.

### setError

```java
public void setError(String authored);
```

Maps errors to guidance captions and shows a WmMessageBox with OK. Null/empty clears the error. End-date ordering,1..31 range, nonexistent month/day, and empty weekday selection errors map to their guidance. Any other nonempty string without a Hangul syllable(가..힣) becomes the catalog translation of "올바른 되풀이 값을 입력하세요." rather than the input. Arbitrary English messages are not displayed literally. Acknowledgment restores prior input focus/caret.

### closePopup

```java
public boolean closePopup();
```

Invalidates popup callbacks and removes the overlay. Returns true when an actual popup was removed, otherwise false.

### back

```java
public boolean back();
```

Handles a nested popup first, otherwise delegates to wizard Back/cancel. Returns whether handled.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Android detach hook; closes the current popup overlay.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Updates popup font/accent and bold range captions; retains error message boxes and closes other popups. Restyle the whole child tree through WmControls.restyleTree.

## Value

```java
public static final class Value
```

### Constants and fields

```java
public final WmRecurrenceRule rule;
public final WmDateTimeField.Value start;
public final WmDateTimeField.Value until;
public final int durationMinutes;
public final boolean allDay;
```

rule/start/until form the model; durationMinutes is civil duration; allDay is the all-day policy. until is an end-date candidate used according to rule.endMode.

### Constructors

```java
public Value(WmRecurrenceRule rule,WmDateTimeField.Value start,WmDateTimeField.Value until,int durationMinutes,boolean allDay);
```

Null rule/start/until throws `IllegalArgumentException`. The editor constructor/completion validates date/duration policy.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmRecurrenceEditor.java`
