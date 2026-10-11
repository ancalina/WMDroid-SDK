---
title: "WmContactDetailsView"
slug: en/api/wmcontactdetailsview
---
[API index](/en/api/)

```java
public final class WmContactDetailsView extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener,WmControls.BackHandler
```

Edits contact drafts in Details and Notes tabs. The owner supplies field order, labels, and persistence. Value changes report through callbacks without automatic storage.

## Field

```java
public static final int TEXT=0,SUBCARD=1,ACTION=2,DATE=3;
public static final class Field
public final String key,label,value;
public final int kind,inputType;
public Field(String key,String label,String value,int kind)
public Field(String key,String label,String value,int kind,int inputType)
```

`key` is a non-null, nonempty ID; label/value must be non-null. Kind is 0..3; violations throw `IllegalArgumentException`. The four-argument constructor uses `InputType.TYPE_CLASS_TEXT`. TEXT has a single-line editor; SUBCARD adds an expansion arrow; ACTION invokes `open`; DATE uses a date or empty-date string validated by `WmDateField.parseValue`. `inputType` applies to text editors.

## Listener

```java
public interface Listener {
    default void changed(Field field){}
    default void committed(Field field){}
    default void open(Field field){}
    default void notesChanged(String notes){}
    default void tabChanged(int tab){}
}
```

`changed` reports user field edits; `committed` reports the current draft when an active text/date field loses focus; `open` reports ACTION or SUBCARD-arrow activation. `notesChanged` reports note edits. `tabChanged` uses 0=Details and 1=Notes. Default implementations do nothing. A committed callback does not mean persistence succeeded.

## Constructor

```java
public WmContactDetailsView(Context context,Typeface regular,Field[] fields,String notes,int selection,Listener listener)
```

Regular font/listener are required; null throws `IllegalArgumentException`. Initial fields follow `setFields` rules below; null notes become empty. Rows are 39 logical pixels high. Text editors are allocated when activated.

## Draft access and updates

```java
public Field[] fields()
public Field field(String key)
public void editField(String key)
public String notes()
public void setNotes(String value)
public void setFieldValue(String key,String value)
public void setFields(Field[] values)
```

`fields` returns an array copy; `field` returns the current Field by key and throws `IllegalArgumentException` for an unknown key. `editField` selects Details. TEXT/SUBCARD activate the editor and select all text; ACTION invokes `open`. DATE shows and focuses the date editor; this call does not itself open a calendar popup. An empty DATE changes to today clamped to the allowed range and can invoke `changed`. The date field's separate opening gesture opens the calendar. `notes` returns the current draft; `setNotes(null)` uses an empty string. `setFieldValue` rejects null values and invalid DATE values. `setFields` copies and fully validates the array, rejecting null arrays/entries and duplicate keys before mutation. Compatible key/kind/inputType/order updates existing controls; other replacements rebuild and restore compatible focus/scroll state. Owner setters do not issue user changed/notesChanged callbacks. Text-focus save/restore targets TEXT fields; text-editor focus on SUBCARD fields is not included in the interaction Bundle.

## Tabs

```java
public int selectedTab()
public void setTab(int tab)
```

0=Details and 1=Notes. Other values throw `IllegalArgumentException`. Changing tabs closes the date popup.

## Subcard

```java
public void showSubcard(WmContactSubcardView card,Runnable apply,Runnable cancel)
public WmContactSubcardView subcard()
public boolean closeSubcard(boolean apply)
```

Requires an unattached card and non-null apply/cancel callbacks; violations throw `IllegalArgumentException`. It closes a date popup and cancels the existing subcard before displaying the new card. If cancellation opens another subcard, it throws `IllegalStateException`. `subcard()` returns the open card or null. An outside tap applies; Back cancels; these dismissal semantics remain unverified against the original. `closeSubcard` removes the card before invoking the chosen callback and returns true if a card existed. Detachment removes the card without invoking a completion callback.

## Date popup and Back

```java
public WmDateField dateField(String key)
public WmCalendarPicker datePopup()
public boolean closeDatePopup()
public boolean back()
```

`dateField` returns the actual control for a known DATE key; an unknown key or another kind throws `IllegalArgumentException`. `datePopup` returns the open calendar or null. Calendar bounds are 1752-09-14 through 9999-12-31. `closeDatePopup` finishes a nested year editor before removing the popup; it returns true if open. `back` handles nested date editing, date-popup closure, then subcard cancellation, returning true when consumed.

## Interaction state

```java
public Bundle saveInteractionState()
public static void validateInteractionState(Bundle state,Field[] ownerValues)
public void restoreInteractionState(Bundle state)
```

The Bundle stores tabs, both scroll positions, focused field/notes, caret positions, DATE control state, and nested open-calendar state. It excludes field/note values. Restore owner drafts before interaction state. `validateInteractionState` validates schemas 1..3, exact key counts/types, nonnegative positions, compatible fields, and DATE state without mutation; failures throw `IllegalArgumentException`. `restoreInteractionState` also validates first; restoring while closing a date popup throws `IllegalStateException`. With schema>=2, a nonempty date_popup naming a disabled DATE to reopen also throws `IllegalStateException`. This is not a blanket disabled check for all DATE state restoration. Restoration suppresses user callbacks and clamps carets to current lengths. An open subcard retains its focus.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
```

Restyling updates editors, notes, tabs, and an open date popup/subcard while preserving both scroll positions. Null regular font throws `IllegalArgumentException`. Language changes update tab captions and DATE/calendar locale. Owner field labels remain unchanged.

## View lifecycle

```java
protected void onAttachedToWindow()
protected void onDetachedFromWindow()
protected void onMeasure(int widthSpec,int heightSpec)
```

Attachment moves an open date popup to an ancestor `WmWindow.popupLayer`. Detachment removes date popups and subcards. Measurement clamps subcard size/position to the current surface.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmContactDetailsView.java`
