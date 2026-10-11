---
title: "WmTaskEditor"
slug: en/api/wmtaskeditor
---
[API index](/en/api/)

```java
public final class WmTaskEditor extends FrameLayout
    implements WmControls.BackHandler,WmControls.RestyleListener,WmControls.LanguageListener
public WmTaskEditor(Context context,Typeface font,Locale locale,Value initial,int accent,int tabColor)
```

Two pages: Task and Notes. The owner supplies the shell, OK/Cancel actions, validation and storage. Null `font`, `locale` or `initial` throws `IllegalArgumentException`.

Fields follow the recovered task descriptor order. Recurrence is not implemented. Reminder controls edit an absolute civil date/time; notification delivery belongs to the owner. Categories use an owner-supplied selector or text input. Inactive card rows use the measured VGA Tasks/Contacts pitch of 39 logical pixels, a 148-pixel caption column and a two-pixel underline. Choices are flat while inactive. Focus adds a border; an open dropdown adds its arrow. A right gutter remains reserved when the scrollbar is hidden. Active-control extents and Android font raster remain adaptations; arithmetic boundary equivalence is not verified. Draft restoration after Activity recreation is not provided.

## Value

```java
public static final class Value {
    public final String subject,categories,notes;
    public final WmDateTimeField.Value start,due,reminder;
    public final int priority,sensitivity;
    public final boolean completed;
    public Value(String subject,int priority,boolean completed,
        WmDateTimeField.Value start,WmDateTimeField.Value due,
        String categories,int sensitivity,String notes);
    public Value(String subject,int priority,boolean completed,
        WmDateTimeField.Value start,WmDateTimeField.Value due,
        String categories,int sensitivity,String notes,WmDateTimeField.Value reminder);
}
public Value value()
```

Strings reject null/NUL. sensitivity must fit a signed short: 0=normal, 1=personal, 2=private, 3=confidential. Other signed-short values are preserved until user selection. priority is positive=high, zero=normal, negative=low. Violations throw `IllegalArgumentException`.

| Field | Value construction |
| --- | --- |
| start / due | Nullable. Valid date in 1752-09-14..9999-12-31; hour/minute normalized to 00:00 |
| reminder | Null disables the reminder. Local date/time in the same date range; hour/minute retained |

Dates outside the range throw `IllegalArgumentException`.

`priority`: positive = High, zero = Normal, negative = Low. Selecting a choice writes +1, 0 or -1; an untouched value is retained exactly. `sensitivity`: 0 = Normal, 1 = Personal, 2 = Private, 3 = Confidential. Other signed-short values have no selected choice and remain unchanged until selection.

`value()` returns the current text and selections without saving. Current text containing NUL throws `IllegalArgumentException`. If exactly one date is null, the returned value copies the other date into it, matching native task save. Both null dates remain null. Visible date fields remain unchanged. Starts/Due hours and minutes are normalized to zero. The method does not trim text or require a nonempty subject; the owner validates those conditions.

## Owned controls

```java
public EditText subjectField()
public EditText categoriesField()
public EditText notesField()
public WmChoice priorityChoice()
public WmChoice statusChoice()
public WmChoice sensitivityChoice()
public WmDateField startField()
public WmChoice reminderChoice()
public WmDateTimeField reminderField()
public WmDateField dueField()
public WmPropertySheet propertySheet()
```

Accessors return attached controls; do not reparent them. Priority choices: High, Normal, Low. Status choices: Not completed, Completed. Task and Notes drafts remain attached when switching tabs. Page IDs are 20007 and 20044.

The editor owns choice/date dropdown and date-change callbacks. Replacing them replaces its popup behavior. Use `WmChoice.choose()` to change a choice. Choosing the current index emits no change notification. Selecting an actual editor dropdown row commits its standard priority/sensitivity value even when the index is unchanged. Silent choice setters do not update retained priority/sensitivity. Date popups include None. Opening an empty date activates today through `WmDateField`; closing the popup retains that draft date. A completed tap anywhere in a date field opens its calendar. Closed cards show the full year; an open DATE uses the locale short date. Calendar month browsing updates the draft immediately and applies peer-date rules. Closing the popup retains the browsed date. Touch cancellation and disabled input do not open or activate a date.

Interactive date changes follow the task owner: clearing Due clears Starts; clearing Starts retains Due. Setting Starts with empty Due fills Due. Changing Starts preserves the previous interval in unsigned 16-bit days. Interval 65535 means unknown: retain Due unless the new Starts is later. Setting Due before Starts moves Starts to Due. Peer updates are silent and do not recurse. `WmDateField.setValue()` skips these rules; use `selectDate()` or `clearDate()` for notified edits.

Day arithmetic uses UTC Gregorian dates, independent of locale and daylight saving. If a shifted Due exceeds the SDK date range, the Starts change is reverted and Due is retained. This boundary policy, reverse multi-year native arithmetic and large-interval overflow are not proven equivalent to the original. If Starts moves Due, an enabled reminder shifts by the same number of days and retains its clock. Moving Due before Starts resets an enabled reminder to that date at 08:00. Clearing Due retains the reminder. A reminder shift outside the supported range retains the previous reminder; native overflow equivalence is unverified.

## Reminder

`Value.reminder == null` means disabled. The eight-argument constructor sets it to null. The nine-argument constructor accepts an absolute civil date/time within 1752-09-14..9999-12-31. `value()` strictly parses time only when enabled; incomplete time throws `IllegalArgumentException`.

The mode and date/time occupy two 39-pixel rows. Disabled mode retains the empty second row. Choosing Remind me initializes 08:00 on Starts, otherwise Due, otherwise local today. Re-enabling resets that value. This implements the recovered fallback default; OEM registry defaults and sound/vibration flags are not ported.

The required reminder date has no None option. Its calendar browsing updates the draft immediately. The editable time combo offers 48 half-hour candidates. Closing either popup retains the draft. The editor owns their opening callbacks. Language/theme changes preserve the reminder.



## Categories

```java
public void setOnCategoriesOpen(Runnable listener)
public String categories()
public void setCategories(String value)
```

A connected callback makes the category field read-only. Clicking an enabled field closes the editor popup and calls the owner. Empty values display None. A null callback restores direct text input. `categoriesField()` keeps its existing EditText return type.

`categories()` returns raw text without parsing dates or times. `setCategories()` rejects null and NUL before changing the value. Use [WmCategoryPicker](/en/api/wmcategorypicker/) for selection. The owner supplies the catalog, New entry, OK/Cancel and storage. Apply only a confirmed non-null `result()`. Cancel and untouched null results retain the raw value.


## Popup and live appearance

```java
public boolean closePopup()
public boolean back()
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
```

`closePopup()` returns true if an attached popup was removed. `back()` first delegates to the popup, then closes it; false means the owner must handle Back. Detaching the editor closes its popup.

Use `WmControls.restyleTree()` to apply font/colors to the editor and children. Language changes relabel captions, choices, tabs and dates, retaining text and selected values. Both operations close transient popups.

## Owner reminder and persistence responsibilities

The editor does not schedule or display system notifications. value().reminder is a local date/time without timezone; null disables it. The owner handles timezone policy, permissions, scheduling, delivery markers and persistence. Category confirmation changes drafts only; the owner decides final persistence.

## Launcher integration example: task reminders and storage

The following describes launcher integration policy separately from shared SDK behavior. Other apps apply their own storage/delivery policy.

The launcher schedules the earliest undelivered incomplete task and posts a system notification. Notification taps open Tasks. Completing/deleting tasks updates the pending alarm; startup, reboot, package update, clock/timezone changes and exact-alarm access changes trigger rescheduling. Android13+ requires notification permission. Task options opens notification/exact-alarm access settings; startup does not request access. Without exact-alarm access on Android12+, delivery uses an inexact idle-capable alarm. System/OEM restrictions can delay delivery.

The launcher interprets civil time in the current timezone: DST gaps move forward; overlaps use standard time. Successful posting records a marker for that reminder value. Saving an identical reminder retains the marker; disabling or changing it clears the marker. Posting and persistence are not one transaction; a crash between them can repeat a notification. API14..23 cannot reliably detect app-wide notification blocking through the public API. Original reminder sound/vibration flags and native reminder balloons are not ported. SDK consumers still implement delivery themselves.

The launcher merges names from other tasks and its saved catalog. Selector OK changes the task draft only. Task OK saves the category value and catalog together without trimming the raw text. Task Cancel does not save selection or new names. The Today filter compares individual names, not the entire category string. A malformed saved catalog blocks editing and remains intact. The WinCE global category database is not ported.

The launcher reloads task storage before editing, completion or deletion is saved. It retains delivery markers added while the screen was open. A changed reminder clears only that task’s marker. Other content changes or malformed storage reject the save without overwriting the stored data.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmTaskEditor.java`
