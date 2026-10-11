---
title: "WmMediaPlaylistView"
slug: en/api/wmmediaplaylistview
---
[API index](/en/api/)

```java
public final class WmMediaPlaylistView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

Now Playing list with selection and Move Up/Down, Add, Remove, and Properties toolbar actions. The owner changes the queue and performs playback through command callbacks.

## Commands

```java
public static final int UP=30002,DOWN=30003,REMOVE=30004,ADD=30009,SAVE=30006,PROPERTIES=30010,PLAY=30001;
public interface Listener {void command(int id);}
```

A tap only changes selection. A long press selects and requests a context menu with ID 0. The toolbar reports UP/DOWN/ADD/REMOVE/PROPERTIES. SAVE and PLAY are IDs for owner menus, not toolbar buttons in this view.

## Constructor

```java
public WmMediaPlaylistView(Context context,Typeface font,Listener listener)
```

Null listener throws `IllegalArgumentException`. Header/toolbar height is 46 logical pixels; row height is 34.

## Entries and selection

```java
public void setEntries(WmMediaQueue.Entry[] values,int current)
public int selectedIndex()
public void select(int index)
@Deprecated public void setSaveEnabled(boolean value)
```

`setEntries` explicitly rejects only a null array and copies it. Callers must supply non-null entries and current=-1 or an in-range index. Current marks the playing source for accessibility and is separate from list selection. A valid selection is retained; otherwise it is adjusted to the last entry or current/first entry. `selectedIndex` returns -1 when unselected. Invalid `select` indices throw `IllegalArgumentException`. Deprecated `setSaveEnabled` does nothing; control Save in the owner menu.

## Icons and lifetime

```java
public void loadIcons(Executor worker)
public void close()
```

`loadIcons` reads resource-pack bitmaps on the supplied Executor and applies results through a main Looper Handler, including before window attachment. Missing images use symbols. Idempotent `close` recycles applied bitmaps; late load results are also recycled. Call close when discarding the view. It does not shut down the Executor. Executor submission failures propagate. Applying a new load recycles the previously applied bitmaps before replacement.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int color,int tabColor)
public void onWmLanguageChanged(String value)
```

Restyling updates row font/selection color and header/toolbar backgrounds. Language changes update toolbar/header accessibility captions while preserving media titles.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmMediaPlaylistView.java`
