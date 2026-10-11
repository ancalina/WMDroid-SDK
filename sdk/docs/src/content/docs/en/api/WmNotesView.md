---
title: "WmNotesView"
slug: en/api/wmnotesview
---
[API index](/en/api/)

```java
public final class WmNotesView extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener,WmControls.BackHandler
```

Client for a document list, folder filtering, sorting, and plain-text editing. The owner persists entries/folders and opens documents. This editor handles plain text, not PWI drawing, recordings, or formatted editing.

## Entry

```java
public static final class Entry
public final String key,title,type,folder;public final long modified,size;
public Entry(String key,String title,long modified)
public Entry(String key,String title,long modified,long size,String type)
public Entry(String key,String title,long modified,long size,String type,String folder)
```

Key is a non-null, nonempty ID. Title is the display name without extension; type is an owner-supplied extension/category; folder is empty for root. Strings must be non-null and size≥0; violations throw `IllegalArgumentException`. Modified is Unix epoch milliseconds; size is owner-supplied bytes. Shorter constructors default omitted size to 0 and type/folder to empty strings.

## Folder and callbacks

```java
public static final class Folder
public final String id,name;
public Folder(String id,String name)
public static final int ALL_FOLDERS=13729,MANAGE_FOLDERS=13730;
public interface FolderListener {void manage();}
public interface Listener {void open(Entry entry);}
public static final int RENAME=25513,DELETE=25512;
public interface ActionListener {void command(Entry entry,int command);}
```

Folder ID/name must be non-null/nonempty or `IllegalArgumentException` is thrown. A single entry tap selects then invokes open. Setting FolderListener adds the folder-management command; manage opens the owner’s management UI. Setting ActionListener enables a long-press RENAME/DELETE menu; the owner performs the actual changes.

## Constructor

```java
public WmNotesView(Context context,Typeface regular,Typeface bold,int selection,Listener listener)
```

Both fonts/listener are required; null throws `IllegalArgumentException`. Defaults: sort=NAME, ascending=true, zoom=100%, all folders visible. Base row height is 34 logical pixels and grows to fit actual font glyph bounds.

## Catalog and folders

```java
public void setEntries(Entry[] values)
public void setFolders(Folder[] values)
public void setOnFolderListener(FolderListener listener)
public String folderId()
public void setFolder(String id)
public Entry selected()
public void setOnActionListener(ActionListener listener)
```

SetEntries/setFolders copy arrays and reject null arrays/entries or duplicate keys/IDs (`IllegalArgumentException`). Replacement rebuilds the current filter and retains entry selection by key, or clears it when that key is absent. `selected()` returns the selected entry, or null. A folder removed from the catalog resets filtering to all folders. FolderId uses null=all folders, empty=root, otherwise a registered ID. SetFolder rejects unknown IDs, clears selection, and scrolls to the top. Root has no toolbar menu item and is selected through the API. Null optional listeners remove their feature; changing them closes related popups.

## Sort and metadata

```java
public static final int SORT_NAME=0,SORT_DATE=1,SORT_SIZE=2,SORT_TYPE=3;
public int sortOrder()
public boolean sortAscending()
public void setSortOrder(int order,boolean ascending)
public void refreshMetadata()
public static String formatSize(long bytes)
```

SetSortOrder rejects values outside 0..3 (`IllegalArgumentException`) and sorts in the requested direction. Choosing the current criterion in the menu reverses direction. Names/types use the locale Collator; dates/sizes use numeric comparison. Type sorting puts empty types last when ascending, first when descending, and compares titles when types tie. Remaining ties use modified date, then key. RefreshMetadata refreshes display for system time zone/date/12-or-24-hour settings while retaining scroll position; no storage I/O. Entries modified today show a time, others a short date. FormatSize rejects negatives: <1024 uses `b`, <1048576 uses `k`, otherwise `m`, rounding integer 1024/1048576 units upward at half a unit.

## Text editor

```java
public void edit(String text)
public void showList()
public boolean isEditing()
public EditText editor()
public String text()
public int zoomPercent()
public void setZoomPercent(int value)
public void setMessage(String text)
```

Edit fills the editor with non-null text, moves caret/scroll to the start, and hides the list; null throws `IllegalArgumentException`. ShowList hides the keyboard, clears editor focus, and restores the list without saving or discarding text. Editor returns the actual control; text returns its draft. Zoom accepts only 75/100/150/200/300; other values throw `IllegalArgumentException`. At 100% text size is 28 logical pixels. Non-null setMessage text covers the client with a literal message; null removes it. Edit/showList also remove messages.

## Interaction state and Back

```java
public Bundle saveInteractionState()
public void restoreInteractionState(Bundle state)
public boolean back()
```

The Bundle stores selected key, folder, zoom, sort criterion/direction, and editor caret/scroll/focus only. The owner separately restores content, catalog, list scroll, and list/editor mode. Supply entries/folders/draft and set mode before restoration. A null Bundle does nothing. Missing/filtered-out selected keys are ignored and carets clamp to current length. Invalid folders/zoom/sort fail through setter exceptions; restoration is not transactional. Back closes only folder/action/sort popups and returns true when handled; the owner handles returning from editing to the list.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
```

Restyling/language changes close open popups and update fonts, captions, sorting, and date/size column widths. English lists use bold 8pt at 192dpi; other default lists use regular 24 logical pixels. Document titles/content are not translated.

## View lifecycle

```java
protected void onWindowVisibilityChanged(int visibility)
protected void onSizeChanged(int w,int h,int oldw,int oldh)
protected void onDetachedFromWindow()
```

Visibility refreshes metadata. Size changes align the editor’s minimum height with the view. Detachment removes open popups.


Only saved focus=true calls editor.requestFocus. False retains current focus without clearFocus. To reproduce unfocused state the owner prepares editor().clearFocus() before restore. Negative scroll clamps to0; nontransactional failures can retain earlier applied state.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmNotesView.java`
