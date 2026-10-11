---
title: "WmContactSubcardView"
slug: en/api/wmcontactsubcardview
---
[API index](/en/api/)

```java
public final class WmContactSubcardView extends FrameLayout implements WmControls.RestyleListener
```

Edits individual components of a contact name or address. The owner defines label order and assembles the result; `WmContactDetailsView.showSubcard` can display the card.

## Constructor

```java
public static final int NAME=0,ADDRESS=1;
public WmContactSubcardView(Context context,Typeface regular,int kind,String[] labels,String[] values,int selection)
```

`NAME` requires exactly 6 labels/values; `ADDRESS` requires 5. Arrays are copied. Null font, another kind, wrong array length, or null arrays/strings throw `IllegalArgumentException`. Address fields 0 and 4 accept multiple lines; other fields are single-line.

## Draft

```java
public String[] values()
public EditText editor(int index)
public void setValues(String[] values)
public void setLabels(String[] values)
```

`values()` returns a copy of the current drafts. `editor(index)` returns the actual zero-based editor; an out-of-range index causes an array-index exception. `setValues` validates the same string count as the constructor and replaces drafts without user-change callbacks. Caret positions are clamped to the new lengths. `setLabels` updates labels/accessibility descriptions and preserves values.

## Changes

```java
public interface OnChangedListener {void changed(int index,String value);}
public void setOnChangedListener(OnChangedListener listener)
```

A TextWatcher reports the zero-based field index and new string synchronously when the value changes. This includes editor(index).setText(...) as well as user input. setValues suppresses notification; unchanged values do not notify. Null disables callbacks. The owner handles persistence and Apply/Cancel.

## Geometry and style

```java
public int desiredHeight()
protected void onMeasure(int widthSpec,int heightSpec)
protected void onLayout(boolean changed,int left,int top,int right,int bottom)
protected void dispatchDraw(Canvas canvas)
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
```

`desiredHeight` returns 248 logical pixels for NAME or 328 for ADDRESS. Preferred width is 350 and obeys measurement constraints. Layout places fields in order; `dispatchDraw` draws the 2-pixel border last. Restyling updates the regular font/selection color and rejects a null regular font.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmContactSubcardView.java`
