---
title: "WmBoxSelect"
slug: en/api/wmboxselect
---
[API](/en/api/)

Manages single or multiple text-cell selections with a bit mask.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmBoxSelect.java`

## Declaration

```text
public final class WmBoxSelect extends LinearLayout implements WmControls.RestyleListener
public WmBoxSelect(Context context,Typeface font,String[] labels,int mask,boolean multi,int accent);
public String[] choices();
public void setProportionalWidths(boolean enabled);
public int selectedMask();
public TextView cell(int index);
public void setChoices(String[] values,int selected);
public void setSelectedMask(int selected);
public void choose(int index);
public void setOnSelectionListener(OnSelectionListener listener);
public void setEnabled(boolean enabled);
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```



labels must contain 1–30 non-null strings. Construction and setChoices copy them; choices returns a copied array. font is required. Invalid values throw `IllegalArgumentException`.

Bit i in mask selects cell i. Negative masks, nonexistent bits, and multiple bits in single-selection mode are invalid. `choose(index)` first checks the index, then returns without changes if WmBoxSelect itself is disabled. It toggles the bit when the container is enabled, without checking the individual cell’s enabled value; selecting the current cell can clear single selection. The listener runs only after a change. Setters do not call it. Invalid cell indices throw `IndexOutOfBoundsException`.

Default cells have equal widths and a height of 48 logical pixels. proportionalWidths adds equal shares of remaining space to measured text widths. cell returns the internal TextView. Do not change the child structure directly. accent is currently unused by drawing.

## WmBoxSelect.OnSelectionListener

```text
public interface OnSelectionListener
void selected(int mask);
```
