---
title: "WmDocumentProperties"
slug: en/api/wmdocumentproperties
---
[API index](/en/api/)

```java
public final class WmDocumentProperties extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener,WmControls.BackHandler
```

Form for renaming a document and choosing its folder/storage destination. The owner supplies metadata/destinations and validates/persists on explicit OK.

## Constructor

```java
public WmDocumentProperties(Context context,Typeface font,Locale locale,String name,String[] folders,int folder,String[] locations,int location,String size,String modified,String format,int accent)
```

Font, locale, name, and metadata strings must be non-null. Destination arrays must be non-null/nonempty without null items; selected indices must be in range. Violations throw `IllegalArgumentException`. The initial name is assigned before input filters to preserve it; later input is limited to 128 UTF-16 code units. Source folder index 0 is the root/None destination pinned first; remaining folders and locations use system-locale display sorting. Single-destination controls are disabled.

## IDs

```java
public static final int NAME=1015,FOLDER=1016,LOCATION=1018,SIZE=2065,MODIFIED=1024,FORMAT=1019;
public static final int NAME_LABEL=2060,FOLDER_LABEL=2062,LOCATION_LABEL=2063,SIZE_LABEL=2064,MODIFIED_LABEL=2059,FORMAT_LABEL=2058;
public static final int NAME_LIMIT=128;
```

View IDs for inputs/metadata and their labels. `NAME_LIMIT` bounds name validation and subsequent input.

## Draft

```java
public EditText nameField()
public WmChoice folderField()
public WmChoice locationField()
public String name()
public int folderIndex()
public int locationIndex()
```

Field accessors return owned controls. `name()` returns the untrimmed draft. Both indices refer to the owner’s source arrays, not sorted display positions. Metadata is display-only; the form performs no file I/O.

## validName

```java
public static boolean validName(String value)
```

Returns false for null/empty values, more than 128 code units, a leading dot, surrounding ASCII spaces, characters at or below U+0020 other than space, or any of `"*/:<>?\|`. It neither trims nor mutates the value. The owner removes surrounding ASCII spaces before calling. It does not check name collisions or destination writability.

## Popup, locale, and style

```java
public void setLocale(Locale locale)
public void onWmLanguageChanged(String language)
public boolean closePopup()
public boolean back()
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
protected void onDetachedFromWindow()
```

Null locale/language throws `IllegalArgumentException`. Locale/language changes update captions and close the choice popup while preserving drafts. `closePopup`/`back` return true if they removed a popup. Restyling and detachment also close it. Restyling updates the next popup’s font/selection color and the heading color.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmDocumentProperties.java`
