---
title: "WmAppointmentEditor"
slug: en/api/wmappointmenteditor
---
[API index](/en/api/)

```java
public final class WmAppointmentEditor extends FrameLayout implements WmControls.BackHandler,WmControls.RestyleListener,WmControls.ModalOwner,WmControls.LanguageListener
```

Two-page Appointment/Notes editor for subject, location, start/end, all-day, recurrence, reminder, busy status, sensitivity, and an owner-connected category field. Attendees remain unfinished. The owner manages provider identity/capabilities, saving, timezone, and shell commands. The bundled Calendar app does not yet connect categories to provider storage. Call View and interaction APIs on the UI thread.

## Setup

### Constants and fields

```java
public static final int FREE = 0;
public static final int TENTATIVE = 1;
public static final int BUSY = 2;
public static final int OUT_OF_OFFICE = 3;
public static final int NORMAL = 0;
public static final int PERSONAL = 1;
public static final int PRIVATE = 2;
public static final int CONFIDENTIAL = 3;
```

Busy:FREE=0/TENTATIVE=1/BUSY=2/OUT_OF_OFFICE=3. Sensitivity:NORMAL=0/PERSONAL=1/PRIVATE=2/CONFIDENTIAL=3.

### Constructors

```java
public WmAppointmentEditor(Context context,Typeface font,Locale locale,Value initial,int accent,int tabColor);
```

Null font/locale/initial throws `IllegalArgumentException`. Start/end editor range1900..2999. User start changes preserve civil-minute duration by moving end. Earlier typed end moves start to end; only selecting an earlier end-time candidate on the same day wraps it to tomorrow.

### Choice popups

All-day, recurrence, busy, sensitivity, reminder-enabled, and reminder-unit menus use [choiceDropdownHeight(256)](/en/api/wmradiomenu/#choicedropdownheight) to calculate complete font-derived rows plus frame height. `256` is the client cap in logical pixels, excluding the frame. Start/end time and reminder-quantity menus pass [getClosedDropdownClientHeight()](/en/api/wmeditablechoice/), which defaults to `256` in this editor. Final height is capped at `max(0, editorHeight − WmMetrics.TAB)`. If an ordinary choice menu's full height exceeds this viewport, the editor wraps it in [WmScrollView](/en/api/wmscrollview/) so scrolling and keyboard navigation can reach the last row. Otherwise, it places the menu directly. Time and quantity menus always use WmScrollView.

### Field access and callbacks

Field getters return the live child objects, not copies. Text edits and choices affect later `value()` and snapshots. Keep children in the editor; replacing their internal open/change/focus listeners disconnects popup handling or synchronization. Callback setters replace the previous callback and retain it until replacement; null removes it. Callback exceptions propagate after any preceding editor changes.

## Appointment fields

### subjectField

```java
public EditText subjectField();
```

Single-line raw subject editor. Changes to its Editable are read without trimming.

### locationField

```java
public EditText locationField();
```

Single-line raw location editor. Changes to its Editable are read without trimming.

### notesField

```java
public EditText notesField();
```

Multiline editor on the Notes page. Changes to its Editable become the saved notes.

### startField

```java
public WmDateTimeField startField();
```

Actual child for start civil date/time. A valid change notification adjusts end by the start’s civil-minute difference only if the existing end also parses successfully. Incomplete end drafts or unrepresentable adjustment results skip movement and retain the end draft. It then refreshes recurrence start and end-candidate origin when current start is readable. The owner validates storage timezone/DST. Direct setValue is silent and does not run this linkage.

### endField

```java
public WmDateTimeField endField();
```

End civil date/time. Reported valid changes earlier than start move start to end; direct `setValue()` is silent. The end-time list contains 48 candidates at 30-minute intervals from the start time, wrapping at midnight. Only an exact match with the current input is selected; the current input does not reorder the candidates.

## All-day start and end dates

For all-day entries start is the first date and end is the last included date. value() normalizes both times to00:00.

| All-day display | start | end | Recurrence-editor duration |
| --- | --- | --- | --- |
|2026-10-11 only|2026-10-11 00:00|2026-10-11 00:00|1440minutes|
|October11–12|2026-10-11 00:00|2026-10-12 00:00|2880minutes|

Timed duration is the end−start civil-minute difference; all-day adds1440minutes. Value does not enforce this relationship. The owner converts storage end rules and validates duration.


### allDayChoice

```java
public WmChoice allDayChoice();
```

No0/Yes1 selector. `choose(1)` disables start/end clock fields without erasing their text. Direct `setSelectedIndex()` does not notify the editor, so it does not update clock enabled state. `value()` uses the current selected index.

### recurrenceChoice

```java
public WmRecurrenceChoice recurrenceChoice();
```

Recurrence preset/Edit combo tied to start date. Changing its rule changes the next model/snapshot. Keep its installed Edit/open callbacks to use the nested wizard.

### reminderField

```java
public WmAppointmentReminder reminderField();
```

Relative reminder composite. Its enabled/unit selections and quantity draft feed the next model/snapshot. Row visibility does not disable its data.

### setReminderVisible

```java
public void setReminderVisible(boolean visible);
```

Shows/hides the reminder row and closes popup; hidden values remain retained.

### busyChoice

```java
public WmChoice busyChoice();
```

Busy selector for values0..3. Initial unknown signed-short values have no selection. A selected index overrides the retained value in `value()`; clearing selection uses the last value recorded by the editor’s choice callback.

### sensitivityChoice

```java
public WmChoice sensitivityChoice();
```

Sensitivity selector for values0..3. Initial unknown signed-short values have no selection. A selected index overrides the retained value in `value()`; clearing selection uses the last value recorded by the editor’s choice callback.

### setStatusFieldsVisible

```java
public void setStatusFieldsVisible(boolean visible);
```

Shows/hides both busy/sensitivity rows. Hide them when the provider cannot preserve metadata. Retains values and closes popup.

### setRecurrenceEnabled

```java
public void setRecurrenceEnabled(boolean enabled);
```

Sets enabled state on the recurrence composite and its choice. False closes any editor popup while retaining the rule; true does not reopen one.

## Pages, categories, and calendar

### propertySheet

```java
public WmPropertySheet propertySheet();
```

Appointment0/Notes1 page controller. Notifying page selection closes the editor popup and invokes the registered page callback. Replacing the sheet’s listener disconnects this handling; use `setOnPageChanged()` instead.

### setOnCategoriesOpen

```java
public void setOnCategoriesOpen(Runnable listener);
```

Non-null makes the category row VISIBLE and enabled. Clicking closes the current SDK popup before running the callback. Null makes the row GONE and disabled. The category value remains. The owner supplies selection UI such as [WmCategoryPicker](/en/api/wmcategorypicker/), New/OK/Cancel commands, the catalog, and persistence. Register the callback before validating or restoring a snapshot that contains category focus.

### categoriesField

```java
public TextView categoriesField();
```

Returns the category-display TextView, not its containing row. An empty value displays the translated None caption; other values display their original text. This caption is not the category value to store. Read the raw value with `categories()`. Direct `setText()` changes only the caption; `setCategories()` changes the model and caption.

### categories

```java
public String categories();
```

Returns the current raw category value without parsing or committing time/reminder fields. An empty string means no categories. Pass this value when opening selection UI; preserve the owner's value separately from the picker draft during cancellation/restoration.

### setCategories

```java
public void setCategories(String categories);
```

Updates the raw category value and display. Null or embedded NUL throws `IllegalArgumentException`. An empty string means no categories. Delimiters, spaces, and spelling are not normalized. Apply only values accepted with OK in the selection UI; Cancel retains the existing value.

### calendarField

```java
public TextView calendarField();
```

Calendar-selection TextView, initially disabled. Direct `setText()` changes only its caption; `setCalendarName()` retains a name for later relabeling.

### setOnCalendarOpen

```java
public void setOnCalendarOpen(Runnable listener);
```

Non-null enables the calendar field. Clicking closes the current popup before invoking the callback; null disables the field. Neither operation changes the calendar name or provider account.

### setOnPageChanged

```java
public void setOnPageChanged(Runnable listener);
```

Reports committed notifying page changes, including programmatic sheet selection, after closing the popup. Same-page selection and silent snapshot restoration do not report a change. Null removes the callback.

### setCalendarName

```java
public void setCalendarName(String name);
```

Sets displayed calendar name, or the default selection caption for null. Does not select an account.

## Values, errors, and interaction state

### setError

```java
public void setError(String authored);
```

Displays a catalog-translated authored error; null/empty clears it.

### value

```java
public Value value();
```

Builds the current model, normalizing all-day clocks to00:00. Invalid date/time or reminder construction is wrapped in `IllegalArgumentException("올바른 시각을 입력하세요.")`. Then validates recurring reminder lead. Reminder quantity uses valid raw input or its last valid quantity without committing or correcting the displayed draft. Preserves raw subject and unknown signed-short status. The owner validates duration/order and provider storage.

### saveInteractionState

```java
public Bundle saveInteractionState();
```

Returns schema3 draft state including raw categories, text/carets/focus/page/scroll/date-time/recurrence/reminder/error, and an open recurrence-wizard draft. Does not parse invalid time or commit blur. Save account identity/capabilities, ordinary popups, and IME composition separately.

### validateInteractionState

```java
public void validateInteractionState(Bundle state);
```

Validates schema1/2/3 and child/caret/status/focus consistency. Schema3 requires a String category value without NUL. Category focus requires a registered callback. Recurrence-popup drafts require enabled recurrence and the Appointment page. Schema1/2/3 require exactly18/19/20 keys, respectively; wrong types, invalid child states, noncanonical recurrence parts, negative scroll, or invalid page/carets/status/focus throw `IllegalArgumentException`. Validation does not apply the draft, but constructs a temporary recurrence editor when that child draft is present.

### restoreInteractionState

```java
public void restoreInteractionState(Bundle state);
```

Close all current popups first. Validates before silent restoration and reopens an included recurrence wizard. Schema1/2 restore categories as an empty string; schema3 restores the raw value. Category callbacks, row visibility policy, and owner selection UI are not included in the snapshot. Reentry is checked first; full state validation precedes the open-popup check. Open popups/reentry throw `IllegalStateException`; malformed snapshots throw `IllegalArgumentException`. No rollback is provided if applying validated values or an external listener throws. Carets, focus, and requested form-scroll position are restored through the child controls.

## Language, popups, and lifetime

### setLocale

```java
public void setLocale(Locale locale);
```

Relabels while retaining drafts, unknown status, and recurrence wizard. Closes ordinary popups. An equal Locale is a no-op, including popup handling. Null throws `IllegalArgumentException`. Raw calendar names and categories remain untranslated.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

Passes the shared catalog language’s Locale to setLocale; state preservation and popup handling follow that method’s contract.

### closePopup

```java
public boolean closePopup();
```

Increments the callback generation even when no overlay exists, invalidating callbacks from dismissed popups, and removes the current overlay. Returns true when an actual popup was removed, otherwise false.

### blocksOwnerCommands

```java
public boolean blocksOwnerCommands();
```

Returns true while a recurrence wizard is open, for owner command blocking.

### inputMethodOwner

```java
public View inputMethodOwner();
```

Returns current popup View or null, allowing the owner to route IME handling to a nested editor.

### back

```java
public boolean back();
```

Returns true if a nested popup handles Back; otherwise returns the popup dismissal result, false when absent.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Android detach hook; closes the current popup overlay.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Stores regular font, bold font (regular when null), and selection color for later popups. Retains an open recurrence wizard and closes other popups. This hook does not apply tabColor or restyle existing children; use [WmControls.restyleTree](/en/api/wmcontrols/#restyletree).

## Value

```java
public static final class Value
```

### Constants and fields

```java
public final String subject;
public final String location;
public final String notes;
public final String categories;
public final WmDateTimeField.Value start;
public final WmDateTimeField.Value end;
public final boolean allDay;
public final int busyStatus;
public final int sensitivity;
public final WmReminder reminder;
public final WmRecurrenceRule recurrenceRule;
public final WmDateTimeField.Value recurrenceUntil;
```

Immutable raw subject/location/notes/categories, civil start/end, allDay, busyStatus/sensitivity, reminder, recurrenceRule/recurrenceUntil. The editor treats all-day end as the last included date; Value itself does not enforce this convention. Apply WmAppointmentText separately at the save boundary.

### Constructors

```java
public Value(String subject,String location,String notes,WmDateTimeField.Value start,WmDateTimeField.Value end,boolean allDay);
public Value(String subject,String location,String notes,WmDateTimeField.Value start,WmDateTimeField.Value end,boolean allDay,WmRecurrenceRule rule,WmDateTimeField.Value until);
public Value(String subject,String location,String notes,WmDateTimeField.Value start,WmDateTimeField.Value end,boolean allDay,WmRecurrenceRule rule,WmDateTimeField.Value until,int busyStatus,int sensitivity);
public Value(String subject,String location,String notes,WmDateTimeField.Value start,WmDateTimeField.Value end,boolean allDay,WmRecurrenceRule rule,WmDateTimeField.Value until,int busyStatus,int sensitivity,WmReminder reminder);
public Value(String subject,String location,String notes,WmDateTimeField.Value start,WmDateTimeField.Value end,boolean allDay,WmRecurrenceRule rule,WmDateTimeField.Value until,int busyStatus,int sensitivity,WmReminder reminder,String categories);
```

Requires all reference values non-null and status/sensitivity within signed-short range, retaining values outside0..3. Violations throw `IllegalArgumentException`. Short overloads default to no recurrence/until=end, BUSY/NORMAL, disabled15-minute reminder. Overloads without a categories argument use an empty category value. Categories cannot contain NUL and retain their original text. Model construction does not validate the editor year range1900..2999, endpoint order, duration, or provider compatibility.



Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmAppointmentEditor.java`
