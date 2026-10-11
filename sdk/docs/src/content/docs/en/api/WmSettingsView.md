---
title: "WmSettingsView"
slug: en/api/wmsettingsview
---
[API index](/en/api/)

```java
public class WmSettingsView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

Builds settings tabs and icon grids from owner-supplied categories and artwork. Does not read or write settings values. Retains page/control instances and lays them out in logical pixels.

## Item / Category

```java
public static final class Item
public final String key,authoredCaption;
public final Drawable icon;
public Item(String key,String authoredCaption,Drawable icon)
public static final class Category
public final String authoredTitle;
public Category(String authoredTitle,Item[] items)
public Item[] items()
```

Item key/caption and Category title must be non-null/nonempty. Icon, item array, and each item must be non-null; violations throw IllegalArgumentException. Item fields are immutable references; the Drawable remains caller-owned. Category copies the array and items() returns another copy. Keys must be unique across all categories.

## Constructor and Listener

```java
public interface Listener {void open(String key);}
public WmSettingsView(Context context,Typeface font,String language,int selection,int face,Category[] categories,Runnable onTab,Listener open)
```

Requires a non-null, nonempty category array with no null categories or duplicate global keys, and non-null onTab/open callbacks. Violations throw IllegalArgumentException. Icon clicks call open(key). Page changes call onTab, then replay an attached page. All pages are created eagerly. Icons are 64px, horizontal spacing 150px, minimum row height 136px; larger fonts increase row height. Width determines column count. Icons sort by current displayed captions using the WM lowercase map and UTF-16 comparison. Incoming captions compare at most260 UTF-16 code units; ties insert before existing items. Item[] order is not retained. Language changes sort again with translated captions. See [insertion comparison and NUL rules](/en/api/wmiconorder/).

## Page access and selection

```java
public WmPropertySheet propertySheet()
public int selectedPage()
public void select(int group)
protected WmPaint createSurface(Context context)
```

propertySheet returns the actual tab container. selectedPage returns its selected zero-based index. select uses property-sheet activation rules and can be vetoed; an invalid index throws IndexOutOfBoundsException. createSurface is the page-construction extension point and returns a GRID-role WmPaint by default. Subclasses can adjust surface options without replacing page content.

## setIcon

```java
public void setIcon(String key,Drawable icon)
```

Replaces displayed artwork in place, retaining pages, scroll, selection, and callbacks. Null/empty key, null icon, or an unknown key throws IllegalArgumentException. Does not change metadata Item.icon.

## Interaction state

```java
public Bundle saveInteractionState()
public void restoreInteractionState(Bundle state)
```

Stores only schema=1, selected category index, and each nonnegative scroll offset. Recreate with the same category/item order. Null restore does nothing. Validates exactly three keys, types, schema, page bounds, offset-array length, and nonnegative offsets before mutation; invalid snapshots throw IllegalArgumentException. Activation veto throws IllegalStateException and leaves offsets unchanged. Restoration suppresses onTab/replay, but property-sheet notifications still run. Offsets apply after layout and clamp to range. Persist settings values separately.

## Appearance and language

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
public void refreshAppearance(Typeface font,String language)
```

Restyle updates stored font/selection/tab colors and row heights. The language callback updates category titles and ordering of current captions. refreshAppearance runs restyleTree and localizeTree across existing controls. Page, scroll, and selection identities remain.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmSettingsView.java`
