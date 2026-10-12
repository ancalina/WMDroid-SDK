---
title: "WmStartMenu"
slug: en/api/wmstartmenu
---
[API](/en/api/)

View for scrolling through folders and program entries and reporting selections.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmStartMenu.java`

```text
public final class WmStartMenu extends FrameLayout implements WmControls.BackHandler
```

## Select

```text
public interface Select
void accept(WmShell.Entry entry);
```

## Constructor, refresh

```text
public WmStartMenu(Context context,List<WmShell.Entry> entries,Typeface font,int accent,Drawable background,Select selected);
public void refresh(List<WmShell.Entry> entries,Typeface font,int accent,Drawable background);
```

Call on the UI thread. Null entries mean an empty list. The list is copied; Entry objects remain shared. Selecting a leaf calls the selection callback. Selecting a folder opens it within the menu.

`ListView` creates visible rows and recycles them during scrolling. Menu width uses every caption in the current folder, including offscreen entries and the Back caption. Font, color, and caption changes through `refresh()` update visible rows and preserve the viewport. Structural changes and folder navigation reset the viewport to the start. Removing the current folder returns to the root. A null background reads the Start background from launcher settings. New rows can read icons and backgrounds synchronously. Entry.icon accepts `android.resource://` and `content://me.ancal.wmdroid.shell/...` only. Other content authorities, file URIs, and HTTP URIs return no icon.

## setTextColor, setBackLabel

```text
public void setTextColor(int color);
public void setBackLabel(String value);
```

Updates current row text colors or the Back caption. A null Back caption becomes an empty string. These methods do not reload icons.

## setMaxHeight, menuWidth, onMeasure

```text
public void setMaxHeight(int logicalPixels);
public int menuWidth();
protected void onMeasure(int widthSpec,int heightSpec);
```

Height and width use logical pixels. Negative maximum heights become 0. Menu width starts at 188, grows with captions, and stops at 342. Measurement does not exceed the parent's available size.

## setIconsUnframed, setIconPolicy

```text
public void setIconsUnframed(boolean value);
public void setIconPolicy(WmIconPolicy value);
```

`setIconsUnframed()` applies one policy to all apps. `setIconPolicy()` accepts app-specific policy. Null disables it for all apps. Only icons whose policy changes are processed again. See [WmIconPolicy](/en/api/wmiconpolicy/).

## back, dismissFolders

```text
public boolean back();
public void dismissFolders();
```

`back()` returns true after opening the previous folder, or false at the root. Deleted folders are skipped. `dismissFolders()` returns to the root and clears folder history, scroll position, pressed state, selection, and focus.

## windowBackground

```text
public static Drawable windowBackground(int accent);
```

Returns a white ColorDrawable. The accent argument does not affect the result.

## Select

`accept(entry)` receives the selected program entry. Launch the app or dismiss the menu in the callback.
