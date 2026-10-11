---
title: "WmMediaLibraryView"
slug: en/api/wmmedialibraryview
---
[API index](/en/api/)

```java
public final class WmMediaLibraryView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

Displays an owner-supplied media hierarchy with titled icon rows. The owner traverses storage and builds the index.

## Row and commands

```java
public static final int OPEN=20001,BACK=-1,SOURCE=-2,CONTEXT=0;
public static final int LIBRARY=0,MUSIC=1,VIDEO=2,TV=3,PLAYLIST=4,NOW_PLAYING=5,ARTIST=6,ALBUM=7,GENRE=8;
public static final class Row
public final String key,title;public final int icon;public final boolean authored;
public Row(String key,String title,int icon,boolean authored)
```

Key must be non-null/nonempty, title non-null, and icon 0..8; violations throw `IllegalArgumentException`. Authored=true titles use SDK string translation; false preserves literal text. A row tap selects then sends OPEN; long press sends CONTEXT. The header sends SOURCE on the left and BACK in its rightmost 92 logical pixels. Callbacks use `WmMediaPlaylistView.Listener`.

## Constructor

```java
public WmMediaLibraryView(Context context,Typeface font,WmMediaPlaylistView.Listener listener)
```

Null listener throws `IllegalArgumentException`. Header height is 46 logical pixels; row height is 34.

## Page and selection

```java
public void setPage(String title,int icon,Row[] values)
public void setPage(String title,int icon,Row[] values,boolean authored)
public Row selected()
public void select(int index)
```

The three-argument overload uses authored=true for the page title. Requires a non-null title, icon 0..8, and non-null array/rows; violations throw `IllegalArgumentException`. Copies the array, selects the first row, and scrolls to the top; an empty page has no selection. Duplicate keys are not rejected. `selected` returns the selected row or null. An invalid `select` index throws `IllegalArgumentException`.

## Icons and lifetime

```java
public void loadIcons(Executor worker)
public void close()
```

Reads icons on the supplied Executor, then replaces and recycles old bitmaps through a main Looper Handler. Results apply before window attachment too. Missing icons use gray squares. Close is idempotent and recycles current bitmaps; late results are recycled after closure. The caller owns Executor shutdown.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int color,int tab)
public void onWmLanguageChanged(String value)
```

Restyling updates the font, selection color, and background. Language changes affect authored rows/titles and the Back caption, preserving data titles.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmMediaLibraryView.java`
