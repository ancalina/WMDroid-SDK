---
title: "WmCategoryPicker"
slug: en/api/wmcategorypicker
---
[API index](/en/api/)

```java
public final class WmCategoryPicker extends FrameLayout implements WmControls.RestyleListener
```

Edits a category-selection draft in a scrollable checked list. The caller supplies the category catalog, New/OK/Cancel commands, and persistence. Construct and update on the UI thread. Read `result()` on OK; discard the draft on Cancel.

## Constructor

```java
public WmCategoryPicker(Context context,Typeface font,Locale locale,String[] catalog,String selected);
```

`font`, `locale`, `catalog`, and `selected` must be non-null. Catalog entries cannot be null, empty, or contain NUL, commas, or semicolons. `selected` cannot contain NUL. Violations throw `IllegalArgumentException`. Empty catalogs and selections are allowed. Existing catalog entries do not receive new-name trimming or the 255-unit limit.

Copies the catalog into an internal list and adds categories found only in the selection string. Commas and semicolons separate selections; leading/trailing U+0020 spaces are removed from tokens. The original selection string remains unchanged until the first edit.

Sorting, duplicate detection, and selection matching use `Collator.SECONDARY` created with the constructor `locale`. Equivalent catalog names retain the first added spelling. The Locale remains fixed after construction.

## saveInteractionState

```java
public Bundle saveInteractionState();
```

Returns a new schema2 Bundle for the unaccepted selection draft. Includes original text, nullable draft, a copy of the current catalog, requested scroll position, and nullable reveal. The reveal value names the last new row awaiting layout; it is cleared to null when a valid layout requests scrolling. Preserves the distinction between an untouched null draft, an empty draft with all categories unchecked, and a changed-then-reverted draft. The catalog includes newly created names. Does not change the owner's accepted value.

## fromInteractionState

```java
public static WmCategoryPicker fromInteractionState(Context context,Typeface font,Locale locale,Bundle state);
```

Creates a new unattached picker from a snapshot. Restores raw values, changed state, catalog, and requested scroll position. A non-null schema2 reveal restores the row to show at the next valid layout. [WmScrollView](/en/api/wmscrollview/) clamps the restored scroll to available space during layout. Restoration does not accept or persist the owner's category value.

| Key | Required value |
| --- | --- |
| schema | Integer `1` or `2` |
| original | String without NUL |
| draft | null or String without NUL; key is required |
| catalog | String[] satisfying constructor conditions |
| scroll | Integer at least `0` |
| reveal | Required only in schema2; null or String without NUL |

Schema1 requires exactly five keys without reveal; schema2 requires exactly six keys including reveal. Null state, invalid schema/types/values, or missing/extra keys throw `IllegalArgumentException`. Every token in a non-null draft must resolve in the restored catalog; the constructor also adds original-selection categories to that catalog. Unknown draft categories throw `IllegalArgumentException`. A non-null reveal must name a known row in the restored catalog and requires a non-null draft. Violations throw `IllegalArgumentException`. A valid layout clears the reveal when it requests scrolling.

The snapshot does not include Context, fonts, or Locale. The supplied Locale creates a new Collator. Pass the original Locale to retain the sorting and name-matching policy. The owner reconnects New/OK/Cancel UI and applies `result()` only on OK.

## value, result, isChanged

```java
public String value();
public String result();
public boolean isChanged();
```

| State | value() | result() | isChanged() |
| --- | --- | --- | --- |
| Untouched | Original selection string | null | false |
| Changed, with selections | Current draft | Current draft | true |
| Changed, all unchecked | Empty string | Empty string | true |

`result()==null` means unchanged, not clear selection. Toggling back to the original state retains a changed draft. A same-state `setChecked` call does not create a change.

## contains

```java
public static boolean contains(String selected,String name,Locale locale);
```

Returns membership of one category name without creating a View. Uses the constructor token grammar and `Collator.SECONDARY`. Null arguments or NUL in selected/name throw `IllegalArgumentException`. Empty selections and names return false.

## categoryNames

```java
public String[] categoryNames();
```

Returns a new array of the current sorted catalog. Modifying the array does not change the internal list. It includes names originally found only in the selection and newly added names.

## isChecked, setChecked

```java
public boolean isChecked(String name);
public void setChecked(String name,boolean checked);
```

Resolve `name` in the catalog with the same Collator. Null or unknown names throw `IllegalArgumentException`. `isChecked` returns whether the current selection string contains an equivalent token.

`setChecked` does nothing when the state matches. Checking appends `", "` and the catalog spelling; an empty selection receives only the name. Unchecking removes a matching token and adjacent separators while retaining other categories. If the selection contains the same category more than once, one call is not guaranteed to remove every occurrence. The corresponding CheckBox is also updated. Programmatic `setChecked()` and `addCategory()` do not check enabled state or visibility; they can change the draft while the picker is disabled or hidden.

## validateNewName

```java
public static String validateNewName(String input);
```

Returns the name with only leading/trailing U+0020 spaces removed. Tabs and other whitespace remain. Null, NUL, an empty trimmed name, commas, semicolons, or more than 255 UTF-16 code units throw `IllegalArgumentException`. Length uses `String.length()`, not Unicode code points. Does not check catalog duplicates.

## addCategory

```java
public boolean addCategory(String input);
```

Validates with `validateNewName`. If the catalog contains a Collator-equivalent name, returns false without changing the catalog or selection. Otherwise adds the name, sorts and rebuilds rows, checks the new category, and returns true. The new category enters the changed draft; the caller persists the catalog.

After rebuilding, callbacks from old CheckBoxes cannot change the current draft. Sorting retains the constructor Locale.

The next valid layout requests scrolling to reveal the new row. If several names are added before layout, it reveals the last name. If the row is taller than the viewport, it reveals the row top. Duplicate input does not request scrolling.

## onLayout

```java
protected void onLayout(boolean changed,int left,int top,int right,int bottom);
```

After child layout, handles the pending new-row reveal even when `changed` is false. Retains the pending row while scroll or row height is not positive. Clears it if the row no longer belongs to the current rows container. Otherwise clears it before requesting scrolling; [WmScrollView](/en/api/wmscrollview/) may defer that request. No pending row means no reveal request.

## onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int face);
```

Updates shared fonts and colors on current CheckBoxes. The regular font is also used for subsequent new rows. Retains the catalog, selection draft, and sorting Locale. Rows use a single line, minimum height `44` logical pixels, and WRAP_CONTENT, allowing larger fonts to increase row height. This sizing is an Android adaptation; it does not guarantee identical native GDI pixels.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmCategoryPicker.java`

## setEnabled

```java
@Override public void setEnabled(boolean enabled);
```

`setEnabled(false)` disables existing and newly built checkboxes and the shared scroll input. Disabling cancels an active touch; reenabling does not commit its release. Explicit `setChecked()` and `addCategory()` still update the draft.
