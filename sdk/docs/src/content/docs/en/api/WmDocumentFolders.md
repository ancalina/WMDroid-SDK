---
title: "WmDocumentFolders"
slug: en/api/wmdocumentfolders
---
[API index](/en/api/)

```java
public final class WmDocumentFolders extends FrameLayout implements WmControls.LanguageListener,WmControls.RestyleListener,WmControls.BackHandler,WmControls.ModalOwner
```

Flat document-folder list with New, Rename, and Delete commands. The owner performs all folder writes.

## Commands and Listener

```java
public static final int NEW=1009,RENAME=1010,DELETE=1012,LIST=1044,NAME_LIMIT=64;
public interface Listener {void command(int command,String folder);void rename(String folder,String name);}
```

NEW/DELETE buttons pass their command ID and selected folder ID (or null) to `command`. RENAME starts in-place label editing. `rename` reports the validated draft and folder ID; the owner must refresh with `setFolders` after completion. After handling NEW, the owner can call `editName` on the created folder. LIST is the list view ID; NAME_LIMIT is 64 UTF-16 code units.

## Constructor

```java
public WmDocumentFolders(Context context,Typeface regular,Typeface bold,int accent,Listener listener)
```

Both fonts/listener are required; null throws `IllegalArgumentException`. `accent` colors the heading and selection.

## Catalog and selection

```java
public void setFolders(WmNotesView.Folder[] values,String current)
public String selectedId()
public void editName(String id)
public boolean isEditing()
```

Copies the array and rejects null arrays/entries or duplicate IDs (`IllegalArgumentException`). `current` identifies the document’s folder icon state, separate from list selection. Existing selection is retained by ID, otherwise null; active name editing ends. Display sorting uses the locale Collator with an ID tie-break. `editName` selects a known ID and all its name text; null/unknown IDs or busy state do nothing. `isEditing` reports whether the label editor exists.

## Commit and Back

```java
public boolean commitEditing()
public boolean back()
```

The return value does not indicate persistence success. Busy returns false; no editor or an unchanged name ending editing returns true. Surrounding ASCII spaces are removed; the64 UTF-16 code-unit limit and WmDocumentProperties.validName are checked. Invalid names show a message and return false. A changed valid name calls Listener.rename synchronously and always returns false, even if the callback immediately ends editing with setFolders.

Busy is not set automatically. For asynchronous storage the owner calls setBusy(true), saves, then calls setFolders and setBusy(false) on the UI thread. Release busy after failure before retry. Interrupted commands/row selection do not replay automatically. Back closes messages first, consumes while busy, and cancels label editing without deleting a created folder. No edit to cancel returns false.

## Busy and input ownership

```java
public void setBusy(boolean value)
public boolean blocksOwnerCommands()
public View inputMethodOwner()
```

Busy disables buttons/editor and blocks owner commands. `blocksOwnerCommands` returns the busy flag. `inputMethodOwner` returns the active name editor or null.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
```

Language changes update heading/button captions and row text sizes. Restyling updates stored fonts/selection color and current row colors. Existing drafts are preserved.

## Detachment

```java
protected void onDetachedFromWindow()
```

Removes added message boxes on detachment.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmDocumentFolders.java`
