---
title: "WmMediaPropertiesView"
slug: en/api/wmmediapropertiesview
---
[API index](/en/api/)

```java
public final class WmMediaPropertiesView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

Displays a media metadata snapshot as label/value pairs. It uses only data supplied through `setValues`, without reading files.

## Constructor

```java
public WmMediaPropertiesView(Context context,Typeface regular,Typeface bold)
```

Regular font styles values; bold styles labels. Requests focus on attachment/visibility for keyboard navigation.

## Snapshot

```java
public void setValues(String[] snapshot)
public String[] values()
public void resetScroll()
```

Requires exactly 13 string slots; a null array/wrong length throws `IllegalArgumentException`. Order: artist, album, album artist, title, genre, duration, audio codec, audio format, video codec, video format, location, track number, bitrate. A null slot omits its label/value pair; an empty string displays an empty value. Copies the array and preserves scroll position. `values` returns a snapshot copy; `resetScroll` moves to the top. Units/formatting are the owner’s literal strings.

## Menu

```java
public WmMenuItem[] menuItems()
```

Returns a disabled Buy CD item with ID 20302. There is no linked album-store service.

## Keyboard and focus

```java
public boolean onKeyDown(int key,KeyEvent event)
public boolean onKeyUp(int key,KeyEvent event)
protected void onAttachedToWindow()
protected void onVisibilityChanged(View changed,int visibility)
protected void onFocusChanged(boolean focus,int direction,Rect previous)
```

When enabled, owner commands are allowed, and no modifiers are held, DPAD Up/Down moves selection through individual labels/values and scrolls offscreen fields into view. Handled keys return true; others delegate to the superclass. Focus changes redraw the active field.

## Style

```java
public void onWmLanguageChanged(String value)
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int face)
```

Language changes update metadata labels only. Restyling updates fonts, selection color, and the background gradient from white to face.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmMediaPropertiesView.java`
