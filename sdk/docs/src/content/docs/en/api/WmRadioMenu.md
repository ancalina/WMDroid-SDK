---
title: "WmRadioMenu"
slug: en/api/wmradiomenu
---
[API](/en/api/)

Provides rows and keyboard behavior for command, radio, and choice lists.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmRadioMenu.java`

```text
public final class WmRadioMenu extends LinearLayout implements WmControls.RestyleListener
```

## Selection

```text
public interface Selection
void choose(int index);
```

## Constructor, menuWidth, menuHeight, row, setSubmenuSelected

```text
public WmRadioMenu(Context context,Typeface font,String[] labels,int selected,String selectedSuffix,int selection,Selection action);
public WmRadioMenu(Context context,Typeface font,WmMenuItem[] items,String selectedSuffix,int selection,Selection action);
public View row(int index);
public int menuWidth();
public int menuHeight();
public void setSubmenuSelected(int index);
```

action is required. Tree items must be nonempty and contain no null entries. Flat labels may be empty; selected is -1 or a valid index. Invalid arguments throw `IllegalArgumentException`. Constructor callbacks receive row indices, not IDs; WmCommandMenu maps them to leaf IDs.

menuWidth is label-measured and capped at 480 logical pixels. menuHeight is the unclipped content height; the caller caps viewport height. row returns the label View, or null for separators and invalid indices. setSubmenuSelected highlights only an enabled submenu row; -1 clears it. Views and callbacks use the main thread.

## enableChoiceKeyboard, choose, keyboard

```text
public void enableChoiceKeyboard(int selected,Selection live,Runnable close);
public boolean onKeyDown(int key,KeyEvent event);
public boolean onKeyUp(int key,KeyEvent event);
public boolean dispatchKeyEvent(KeyEvent event);
```

Use only for ordinary choice lists. All entries must be enabled without separators or submenus. Icon rows are invalid. selected is -1 or a valid index; live and close are required. Errors throw `IllegalArgumentException`. Navigation commits immediately through live without closing. Enter, Escape, and toggle call close. Escape does not restore the previous value. Touch uses the constructor callback. The caller manages popup and callback lifetime.

## choiceDropdownHeight

```text
public int choiceDropdownHeight(int clientHeight);
```

clientHeight caps the list area excluding its frame, in logical pixels. Negative values throw `IllegalArgumentException`. Call [enableChoiceKeyboard](#enablechoicekeyboard-choose-keyboard) first. Unconfigured keyboard choices or an empty list throw `IllegalStateException`.

Returns `paddingTop+paddingBottom+min(itemCount,clientHeight/rowHeight)*rowHeight`. Integer division includes only complete rows. rowHeight is the first row's layout height, updated from current font metrics during keyboard-choice setup and restyling. A clientHeight of 0 returns only top and bottom padding.

## enableChoiceCharacterSearch, searchCharacter

```text
public void enableChoiceCharacterSearch(Locale sortedLocale,Selection live);
public boolean searchCharacter(char value);
```

Enable choice keyboard first; live is required. Errors throw `IllegalArgumentException`. Null locale uses unsorted cycling; other locales accumulate search for one second. Accepts one UTF-16 unit. Processing requires enabled, attached, shown content without touch capture; otherwise returns false. In a processable state it returns true even without a match. A match invokes the registered character-search callback synchronously. Callback indices follow display order. The caller maps source indices.

ACTION_MULTIPLE with KEYCODE_UNKNOWN searches a nonempty character string one UTF-16 unit at a time. Ctrl/Alt/Meta or control units other than Backspace/Escape reject the whole string before search. If a callback disables or detaches the menu, processing stops; earlier selections remain. IME composition remains unsupported.

## setIconRows

```text
public void setIconRows(Bitmap[] icons,Typeface regular,Typeface bold,boolean useBold,float textSize,int selection);
```

Uses 36-high command rows, 32×32 icons, and text left=40 in logical pixels. Array length must match entries; only separators use null. Recycled Bitmaps, submenus, choice keyboard, null fonts, and nonpositive or nonfinite textSize throw `IllegalArgumentException`. Only the array is copied; the caller owns Bitmaps. Do not recycle during use. useBold selects the bold font role.

## setEnabled, onWmRestyle, lifecycle

```text
protected void onLayout(boolean changed,int left,int top,int right,int bottom);
protected void onFocusChanged(boolean focused,int direction,Rect old);
protected void onDetachedFromWindow();
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
public void setEnabled(boolean enabled);
```

Updates row fonts, colors, and measured width. Choice lists preserve fractional-row scroll position when font height changes. Detachment clears owner binding, touch capture, and character-search state.
