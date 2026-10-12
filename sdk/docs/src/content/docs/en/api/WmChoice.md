---
title: "WmChoice"
slug: en/api/wmchoice
---
[API](/en/api/)

Closed single-selection field linked to a caller-owned popup.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmChoice.java`

```text
public final class WmChoice extends TextView implements WmControls.RestyleListener
```

## OnChoiceListener

```text
public interface OnChoiceListener
void chosen(int index);
```

## OnOpenListener

```text
public interface OnOpenListener
void open(WmChoice source);
```

## Constructor, setChoices, choices

```text
public WmChoice(Context context,Typeface font,String[] choices,int selected,int selection);
public void setChoices(String[] choices,int selected);
public String[] choices();
```

choices must contain at least one non-null string. selected is a valid source index. Invalid arrays or labels throw `IllegalArgumentException`; invalid indices throw `IndexOutOfBoundsException`. Arrays are copied. choices() returns another copy. View changes and callbacks use the main thread.

With a bound ordinary keyboard-enabled popup, `setChoices()` and `setDisplaySort()` replace its rows in place, reset character search, and apply the current display selection without calling the choice listener. Replaced row views cannot commit. The popup and its callbacks remain; caller-owned width and height do not change. With no selection, replacement resets the caret and scroll position to the first row. This bulk operation adapts reset/add/select behavior; it is not an individual native combo message API.

## selectedIndex, setSelectedIndex, clearSelection, choose, chooseDisplayIndex, chooseCharacterDisplayIndex

```text
public void clearSelection();
public void setSelectedIndex(int index);
public void chooseCharacterDisplayIndex(int index);
public int selectedIndex();
public void chooseDisplayIndex(int index);
public void choose(int index);
public void setOnChoiceListener(OnChoiceListener listener);
```

selectedIndex is the source index, or -1 after clearSelection. setSelectedIndex and clearSelection are silent. choose calls the listener only when enabled and changed. chooseDisplayIndex maps display to source index. chooseCharacterDisplayIndex calls the listener even for the same value.

For an open list with choice keyboard handling enabled, `setSelectedIndex()` updates selection and caret in display order and scrolls that row to the top, clamped to the scroll range. `clearSelection()` clears list selection while retaining the caret, scroll position and popup. User selection only scrolls the row into view.

## setDisplaySort, displaySortLocale, displayChoices, displaySelectedIndex

```text
public void setDisplaySort(Locale locale,int pinnedPrefixCount);
public Locale displaySortLocale();
public String[] displayChoices();
public int displaySelectedIndex();
```

locale is required; pinnedPrefixCount is 0–choices.length. Invalid values throw `IllegalArgumentException`. Preserves the prefix and sorts later display entries with Collator SECONDARY; labels beginning with [ follow other labels. Source indices and callback identity remain. displayChoices returns a new display array. displaySelectedIndex returns its index or -1. New choices retain the current sorting policy.

## bindDropdown, isDroppedDown, setExtendedUi, isExtendedUi, searchCharacter

```text
public boolean onTouchEvent(MotionEvent event);
public boolean onKeyDown(int key,KeyEvent event);
public boolean onKeyUp(int key,KeyEvent event);
protected void onFocusChanged(boolean focused,int direction,android.graphics.Rect old);
public void setExtendedUi(boolean enabled);
protected void onDetachedFromWindow();
public boolean searchCharacter(char value);
public boolean isExtendedUi();
public void bindDropdown(View popup);
public boolean isDroppedDown();
public static void bindDropdown(View anchor,View popup);
public void setEnabled(boolean enabled);
public void setOnOpenListener(OnOpenListener listener);
```

setOnOpenListener registers the click callback that creates the popup. bindDropdown links visibility and key routing; the caller creates, attaches, and closes the popup. The static overload binds Choice, EditableChoice, or DateField. Disabling or detaching clears the binding.

Default closed Home/End selects the first or last entry. setExtendedUi(true) opens on closed Down and ignores Up, Home, End, Page, and F4. Ordinary toggle keys are unmodified F4 and Alt+Up/Down. When closed, they call the open callback; when open, they forward the key to the bound popup. extendedUi=true ignores F4 and opens from closed state only on unmodified Down. Alt+Up/Down retains the toggle path. searchCharacter accepts one UTF-16 unit and returns false while disabled, dropped, or pressed. Otherwise it returns true even without a match. A match applies selection and calls chosen synchronously; the return value does not indicate a match. Sorted search accumulates for one second; unsorted search cycles. ACTION_MULTIPLE with KEYCODE_UNKNOWN and a nonempty character string searches each UTF-16 unit in order. Ctrl/Alt/Meta or control units other than Backspace/Escape reject the whole string before search. Processing stops if the target rejects a later unit; earlier callbacks remain committed. IME composition remains unsupported.

## onWmRestyle, onMeasure, onDraw, drawableStateChanged

```text
protected void onMeasure(int widthSpec,int heightSpec);
protected void drawableStateChanged();
protected void onDraw(Canvas canvas);
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Updates font, selection color, and artwork. Automatic height uses shared comboHeight and font row metrics.

![Android API 14 render](/WmChoice.png)
