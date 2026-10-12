---
title: "WmEditableChoice"
slug: en/api/wmeditablechoice
---
[API](/en/api/)

Field combining direct editing, candidate selection, and a caller-owned popup.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmEditableChoice.java`

```text
public final class WmEditableChoice extends EditText implements WmControls.RestyleListener
```

## OnOpenListener

```text
public interface OnOpenListener
void open(WmEditableChoice source);
```

## OnValueChangedListener

```text
public interface OnValueChangedListener
void changed(String value);
```

## Constructor, value, setValue, choose

```text
public String value();
public void setValue(String value);
public void choose(int index);
public WmEditableChoice(Context context,Typeface font,String[] candidates,String value,int selection);
public int dropdownSelectedIndex();
public void setOnValueChangedListener(OnValueChangedListener listener);
```

Null candidate arrays, labels, or value throw `IllegalArgumentException`. Empty candidate arrays are allowed. candidates() returns a copy. setValue is silent. Direct editing reports actual string changes while enabled. choose requires a valid index or throws `IndexOutOfBoundsException`. When enabled, it applies the candidate and calls the listener only for a string change. An identical string still updates the candidate index. Operations and callbacks use the main thread.

## setCandidates, setCandidateSort, addCandidate, insertCandidate, removeCandidate, clearCandidates

```text
public String[] candidates();
public void setCandidates(String[] candidates);
public void setCandidateSort(java.util.Locale locale);
public int addCandidate(String label);
public int insertCandidate(int index,String label);
public int removeCandidate(int index);
public void clearCandidates();
```

setCandidates copies the array and resets the candidate selection index to -1 and candidate search position to 0 when candidate content changes. It leaves input text and its text selection unchanged. Existing nonempty captured popup rows are not replaced automatically. setCandidateSort affects future addCandidate calls only; null appends. It does not sort existing arrays. insertCandidate uses -1 for append and bypasses sorting; it returns the inserted index.

removeCandidate returns the remaining count. Deleting the selected candidate from a longer list clears the caption; deleting the last candidate preserves literal text. clearCandidates clears both candidates and caption. Mutations are silent. Invalid indices throw `IndexOutOfBoundsException`. Close captured, reordered, or non-keyboard popups before mutation. A mismatched bound list throws `IllegalStateException`. Bound callbacks must resolve current source indices.

## open, bindDropdown, isDroppedDown

```text
public void bindDropdown(View popup);
public void open();
public boolean isDroppedDown();
public void setOnOpenListener(OnOpenListener listener);
```

open completes text from an exact match, then a prefix match, before calling the open listener. Completion uses the normal edit notification path. Focused fields select all text. The caller creates, attaches, and closes the popup. bindDropdown links keys and lifetime. dropdownSelectedIndex returns the draft's first prefix match, or -1.

Call `menu.enableChoiceKeyboard(selected, live, close)` before `field.bindDropdown(popup)` when binding a `WmRadioMenu`. Only a menu prepared at `WmEditableChoice.bindDropdown` time is configured to keep the field focused. The caller manages popup creation, attachment, removal, and the close callback. Call `field.bindDropdown(null)` when closing to clear the binding.

The following fragment runs inside the open listener. `field` is an attached input field, `popupLayer` is the caller-owned `FrameLayout`, and `font`/`accent` are current styles. It assumes nonempty candidates and enough space for the entire popup. Run on the UI thread and keep candidates unchanged while open. The owner positions the popup and calls `close.run()` for outside touches or screen teardown.

```java
String[] choices = field.candidates();
int selected = field.dropdownSelectedIndex();
FrameLayout popup = new FrameLayout(field.getContext());
Runnable close = () -> {
    field.bindDropdown(null);
    popupLayer.removeView(popup);
};
WmRadioMenu menu = new WmRadioMenu(field.getContext(), font,
        choices, selected, "", accent, index -> {
    field.choose(index);
    close.run();
});
menu.enableChoiceKeyboard(selected, field::choose, close);
popup.addView(menu, new FrameLayout.LayoutParams(
        menu.menuWidth(), menu.menuHeight()));
field.bindDropdown(popup);
popupLayer.addView(popup, new FrameLayout.LayoutParams(
        menu.menuWidth(), menu.menuHeight()));
```

## setExtendedUi, isExtendedUi, keyboard

```text
public void setExtendedUi(boolean enabled);
public boolean onKeyDown(int key,android.view.KeyEvent event);
public boolean onKeyUp(int key,android.view.KeyEvent event);
public boolean onTouchEvent(MotionEvent event);
public void setOnCandidateNavigationListener(WmRadioMenu.Selection listener);
public boolean isExtendedUi();
public void setClosedDropdownClientHeight(int height);
public int getClosedDropdownClientHeight();
```

setOnCandidateNavigationListener delegates closed candidate commits to the owner; absent listeners use choose. setExtendedUi(true) opens on closed Up/Down and ignores Page and F4. Default Up/Down navigates candidates. Closed Page navigation requires setClosedDropdownClientHeight. Height excludes frame and caption and uses logical pixels. -1 delegates to the editor; 0 is valid; values below -1 throw `IllegalArgumentException`. Open Enter/Escape closes while retaining the last commit. getClosedDropdownClientHeight returns the configured value; the default is -1.

## saveInteractionState, validateInteractionState, restoreInteractionState

```text
public Bundle saveInteractionState();
public void restoreInteractionState(Bundle state);
public static void validateInteractionState(Bundle state,int candidateCount,int textLength);
```

schema=1 stores candidate count, selected index, caret, and text selection. Restore owner candidates and literal draft first. Validation checks schema, count, and ranges; failure throws `IllegalArgumentException`. Restore is silent and throws `IllegalStateException` if the popup is open. Popup, gesture, and extendedUi policies are absent from the snapshot.

## setEnabled, onWmRestyle, lifecycle

```text
protected void onMeasure(int widthSpec,int heightSpec);
protected void onFocusChanged(boolean focused,int direction,android.graphics.Rect old);
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
protected void onDetachedFromWindow();
public void setEnabled(boolean enabled);
```

Clear arrow gestures, retained keys, and dropdown bindings. Restyle updates font and frame without value callbacks.
