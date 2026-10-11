---
title: "WmContactsView"
slug: en/api/wmcontactsview
---
[API index](/en/api/)

```java
public final class WmContactsView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

Displays contact search, ASCII initial groups, and contact rows. The owner supplies data and opens contacts. Search matches a case-insensitive substring of the name and preserves input order.

## Entry and Listener

```java
public static final class Entry
public final String key,name,secondary;
public Entry(String key,String name)
public Entry(String key,String name,String secondary)
public interface Listener {void selected(Entry entry);void open(Entry entry);}
```

`key` is a nonempty unique ID; a null/empty key throws `IllegalArgumentException`. Null name/secondary text becomes empty. The two-argument constructor omits secondary text. A completed single tap passes the same entry to `selected`, then `open`.

## Constructor

```java
public WmContactsView(Context context,Typeface regular,Typeface bold,int selection,Listener listener)
```

Both fonts and the listener are required; null throws `IllegalArgumentException`. Rows are 54 logical pixels high. Initial buttons scroll to the first visible entry in `#ab`, `cde`, `fgh`, `ijk`, `lmn`, `opq`, `rst`, `uvw`, or `xyz`. They do not perform Korean initial-consonant navigation.

## Data and query

```java
public void setEntries(Entry[] entries)
public Entry selected()
public EditText search()
public String query()
public void setQuery(String query)
public void setMessage(String authored)
```

`setEntries` copies the array; a null array/entry or duplicate key throws `IllegalArgumentException` before mutation. Selection is retained by key or becomes null when removed. Data changes restore the previous list scroll position. `search()` returns the actual editor. `query()` returns its text; `setQuery` rejects null and filters/scrolls to the top when changed. Search does not clear selection, so `selected()` can return an entry hidden by the current query. Non-null `setMessage` text replaces the list with an owner status message; null restores data.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
```

Updates fonts and selection color. Language changes apply to SDK-authored captions and preserve user data.

## Detachment

```java
protected void onDetachedFromWindow()
```

Stops scrollbar repeat work on detachment.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmContactsView.java`
