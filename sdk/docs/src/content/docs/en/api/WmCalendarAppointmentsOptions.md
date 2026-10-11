---
title: "WmCalendarAppointmentsOptions"
slug: en/api/wmcalendarappointmentsoptions
---
[API index](/en/api/)

```java
public final class WmCalendarAppointmentsOptions extends FrameLayout implements WmControls.BackHandler,WmControls.RestyleListener,WmControls.LanguageListener
```

At480 logical pixels, fields follow the native dialog coordinates. Display symbols share a252px right column with six42px cells. Narrow pages wrap these cells and move the account row below them. Wrapped labels and oversized fonts expand their rows. [WmScrollView](/en/api/wmscrollview/) owns vertical overflow.

Edits default new-appointment reminder, display symbols, and meeting-request account. The caller schedules reminders, supplies accounts, sends mail, and persists values.

The quantity field handles closed Page Up/Down with a210px hidden-list client profile. At the default font size, these keys move between the first and last of its six candidates. Larger fonts reduce page capacity. The height follows the native7/14 DLU stock-font profile; other native creation fonts and the live GDI rectangle remain unmeasured. The opened quantity list uses the same client-height profile and rounds down to complete font-derived rows. Page edges can further reduce its visible height.

Unit and account popups use214px and166px hidden-list client profiles, respectively, with complete-row rounding and page-edge clipping. These profiles use the same native stock-font assumptions as the quantity field.

### Constants and fields

```java
public static final int REMINDER_ENABLED = 24020;
public static final int REMINDER_QUANTITY = 24021;
public static final int REMINDER_UNIT = 24022;
public static final int DISPLAY_ICONS = 25010;
public static final int MAIL_ACCOUNT = 21165;
public static final int MINUTES = 0;
public static final int HOURS = 1;
public static final int DAYS = 2;
public static final int WEEKS = 3;
```

The field IDs locate owned child controls. Do not replace their value, choice, checked-change, or focus listeners; the page uses those listeners to synchronize state and restore empty quantity drafts. Use setOptions or setAccounts for silent restoration instead of changing the child controls directly.

### Constructors

```java
public WmCalendarAppointmentsOptions(Context context,Typeface font,Locale locale,Options options,int selection,Listener listener);
public WmCalendarAppointmentsOptions(Context context,Typeface font,Typeface bold,Locale locale,Options options,int selection,Listener listener);
```

The two-face constructor uses the supplied bold face for Western headings. The single-face constructor uses font for both roles; it does not generate bold. Null bold throws IllegalArgumentException. Korean, Japanese and Chinese page locales use24px regular text; other locales use21px regular labels/checks and21px bold headings. Combos remain24px. These are192dpi requested font heights; native GDI metric adjustment is not reproduced. Static headings/labels align top-left. setLocale changes these roles immediately; restyleTree updates both faces.

Null font/locale/options throws `IllegalArgumentException`. The supplied Options defines every initial reminder/display setting; initialization does not notify. Account combo starts disabled with no accounts. Listener may be null; the page retains it for its lifetime and has no replacement API. Use View operations on the UI thread.

### options

```java
public Options options();
```

Returns new Options with last-valid quantity and current enabled/unit/mask. Clearing the numeric draft retains the previous quantity without notifying for that edit. Other field changes still notify using this retained quantity. Losing focus restores that quantity to an empty field without notifying. Reminder-disabled state disables quantity/unit editing and closes the popup, retaining their values.

### setOptions

```java
public void setOptions(Options options);
```

Closes popup and silently restores fields, also updating change-deduplication state. Null throws `IllegalArgumentException`. This does not change the selected account or account callback.

### accountId

```java
public String accountId();
```

Returns selected account id, or empty text when no accounts exist.

### setAccounts

```java
public void setAccounts(String[] ids,String[] labels,String selectedId,OnAccountListener listener);
```

Copies equal-length ids/labels arrays. IDs must be unique, nonempty, non-null; labels non-null. Nonempty lists require a matching selectedId; empty arrays require selectedId="". Violations throw `IllegalArgumentException` before replacement. Restoration does not notify. A successful call closes the popup, replaces the account callback, and enables the combo only for nonempty arrays. Null listener removes account notifications; the page retains a non-null listener until the next successful `setAccounts()` call.

### setLocale

```java
public void setLocale(Locale locale);
```

Relabels settings, units, and symbol descriptions; closes popup. Retains caller account labels. Null throws `IllegalArgumentException`.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

Passes the shared catalog language’s Locale to setLocale; state preservation and popup handling follow that method’s contract.

### closePopup

```java
public boolean closePopup();
```

Invalidates popup callbacks and removes the overlay. Returns true when an actual popup was removed, otherwise false. Keyboard navigation applies choices while the popup is open; closing does not restore those values.

### back

```java
public boolean back();
```

Returns the result of closing the current popup; false if none is open.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Closes the popup and stores font/selection color for subsequent popups. Update existing children through WmControls.restyleTree.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Android detach hook; closes the current popup overlay.

## Options

```java
public static final class Options
```

### Constants and fields

```java
public final boolean enabled;
public final int quantity;
public final int unit;
public final int displayMask;
```

enabled is the new-appointment reminder default. quantity is 0–99; unit is MINUTES, HOURS, DAYS, or WEEKS. displayMask bits 0–5 control reminder, recurrence, notes, location, attendees, and private symbols, respectively.

### Constructors

```java
public Options(boolean enabled,int quantity,int unit,int displayMask);
```

Throws `IllegalArgumentException` for quantity outside0..99, unit outside0..3, or mask outside0..63.

### minutes

```java
public int minutes();
```

Returns minutes using multiplier1/60/1440/10080, independently of enabled.

## Listener

```java
public interface Listener
```

### changed

```java
void changed(Options options);
```

Delivers new Options synchronously when valid settings change. Use setOptions for ordinary restoration.

| Action | options() | Notification |
| --- | --- | --- |
| Valid digits in enabled quantity | New quantity | If settings differ |
| Empty quantity draft | Last valid quantity | None from this edit alone |
| Enabled quantity/unit choose | Chosen value | If settings differ |
| Reminder check/symbol change | New value | If settings differ |
| setOptions | Entire supplied settings | None |
| Child quantity setValue | Retained quantity; display draft only | None |
| Child unit setSelectedIndex | New unit | None |

Disabled quantity/unit choices are ignored. Empty quantity restores its last value silently on blur. The duplicate-comparison baseline updates before callback; exceptions propagate without reverting values/baseline, so retrying identical settings does not notify. The owner applies/persists settings.

## OnAccountListener

```java
public interface OnAccountListener
```

### chosen

```java
void chosen(String id);
```

Reports id synchronously after selecting a different account, including programmatic account-choice `choose()`. Updates `accountId()` before the callback; callback exceptions propagate without restoring the previous account. Selecting that same id again does not notify; account choices are ignored while the combo is disabled. Silent account `setSelectedIndex()` changes the child selection without updating accountId(); restore the account through `setAccounts()`. Account labels remain caller-provided text without translation.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmCalendarAppointmentsOptions.java`

The heading uses the RCML role15 color:240-step theme hue, full saturation and brightness144/240. Grayscale themes use native integer luminance. Theme updates retain the heading and option values.
