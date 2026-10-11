---
title: "WmSpeedDialView"
slug: en/api/wmspeeddialview
---
[API index](/en/api/)

```java
public final class WmSpeedDialView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

List of speed-dial slots and contact names. Taps change selection only; the owner handles dialing, editing, and storage.

## Entry

```java
public static final class Entry
public final int slot;public final String name,number;
public Entry(int slot,String name,String number)
```

Slot is 1..99; name must be non-null and nonempty after `String.trim()`. Violations throw `IllegalArgumentException`; the original name is retained. Null number becomes empty, then is validated/normalized with `WmPhoneView.dialable`. Slot 1 displays the localized voicemail caption.

## Constructor and selection

```java
public WmSpeedDialView(Context context,Typeface regular,Typeface bold,int selection,Entry[] entries)
public Entry selected()
public void setEntries(Entry[] entries)
```

SetEntries rejects null arrays/entries or duplicate slots before mutation (`IllegalArgumentException`). Copies the array and preserves input order. Retains selection by slot, otherwise selects the first entry; an empty list yields null. Selected returns the current entry.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
```

Restyling/language changes rebuild rows while preserving selection. Regular styles slot numbers; bold styles names; selection colors the selected row.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmSpeedDialView.java`
