---
title: "WmBrowserFavoritesView"
slug: en/api/wmbrowserfavoritesview
---
[API index](/en/api/)

```java
public final class WmBrowserFavoritesView extends FrameLayout implements WmControls.RestyleListener
```

Displays folders and links in one list. A tap selects; a second tap on the same key within the Android double-tap timeout invokes the open callback. The owner handles folder navigation and URL loading.

## Entry and Listener

```java
public static final class Entry
public final String key,name;
public final boolean folder;
public Entry(String key,String name,boolean folder)
public interface Listener {void open(Entry entry);}
```

`key` identifies a retained selection, `name` is literal display text, and `folder` selects the folder icon. Null key/name throws `IllegalArgumentException`. `Listener.open` receives the selected entry; the control does not navigate by itself.

## Constructor

```java
public WmBrowserFavoritesView(Context context,Typeface font,int accent,Listener listener)
```

Null `font` or `listener` throws `IllegalArgumentException`. Rows are 34 logical pixels high; the scrollbar is 26 logical pixels wide.

## Selection

```java
public void setEntries(Entry[] values)
public Entry selected()
public void openSelection()
public void setEnabled(boolean enabled)
```

`setEntries` copies the array and rejects a null array/entry or duplicate key before mutation (`IllegalArgumentException`). It retains an existing key using the new entry, otherwise selects the first entry; an empty array selects null. Replacement resets double-tap tracking. `selected()` returns the current entry or null. `openSelection()` invokes the callback only when enabled with a selection. `setEnabled` also controls the list and scrollbar and resets double-tap tracking.

## Layout and style

```java
protected void onMeasure(int width,int height)
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
```

Measurement reserves scrollbar space for an overflowing list. Restyling applies `regular` and `selection` and refreshes icons when the resource-pack revision changes.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmBrowserFavoritesView.java`
