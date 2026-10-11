---
title: "WmSipButton"
slug: en/api/wmsipbutton
---
[API](/en/api/)

```java
public final class WmSipButton extends LinearLayout
```

Connects keyboard visibility and input-method selection to an eligible editor within the owner.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmSipButton.java`

## Constructor

```java
public WmSipButton(Context context,View owner,WmBar bar);
```

Construct and modify on the UI thread. `owner` is an editor or a UI root containing editors; null throws `IllegalArgumentException`. The owner reference cannot change after construction. `bar` must be a non-null, softkey-mode [WmBar](/en/api/wmbar/). Title mode throws `IllegalStateException` during background application.

The initial state is GONE. The keyboard icon occupies `52×52` SDK logical pixels; the arrow occupies `20×52`. The icon is centered. Both children are non-focusable and disable Android touch sound. Accessibility descriptions are initially bound in Korean; change them later with [WmControls.localizeTree](/en/api/wmcontrols/).

The icon loads `sip_keyboard` through [WmIcons.load](/en/api/wmicons/). An absent or unreadable pack vector falls back to the bundled `sip_keyboard` drawing.

## Touch actions

The keyboard icon activates on a completed UP inside its bounds. It calls `refresh()` first and returns if no eligible editor exists. If the keyboard is open, it requests hiding with that editor's token. If closed, it requests `SHOW_IMPLICIT` only when the editor's `requestFocus()` succeeds. It does not return IME request success; a later `refresh()` observes the resulting state.

The arrow refreshes on DOWN and requests the Android input-method picker only when an eligible editor and an open keyboard both exist. UP does not repeat the request. Pointer count, leaving the bounds, and CANCEL follow [WmCommandBar](/en/api/wmcommandbar/) key-touch cancellation. CANCEL does not undo a picker request already made on DOWN.

## refresh

```java
public void refresh();
```

Resolves the current input owner with [WmControls.inputMethodOwner](/en/api/wmcontrols/). An editor must satisfy all of these conditions.

- `isShown()` and `isEnabled()` return true.
- `onCheckIsTextEditor()` returns true, and a window token exists.
- The editor is within both the constructor's `owner` and the current input owner.

Selects the current focus if it qualifies. Otherwise, retains the previous editor while it remains eligible. If that editor is no longer eligible, searches the input-owner tree recursively from the last child. Without an input owner or eligible editor, the whole button becomes GONE.

Also updates keyboard visibility. API30 and later use root `WindowInsets.Type.ime()`; null insets mean false. Earlier versions treat `rootHeight - visibleFrame.bottom > 100*density` as an open keyboard. This heuristic can differ from actual IME visibility. The arrow is VISIBLE only when the keyboard is considered open.

## isInputMethodVisible

```java
public boolean isInputMethodVisible();
```

Returns the keyboard visibility determined by the last `refresh()`. This call does not refresh the result. It does not indicate editor eligibility or this button's VISIBLE state. The result is false immediately after construction or detachment.

## restyle

```java
public void restyle(WmBar bar);
```

Replaces both children's pressed backgrounds with the supplied softkey `bar`. The constructor's `bar` requirements apply. Reloads the icon when `WmResources.revision` changes; retains it when the revision is unchanged. Does not refresh the editor, keyboard state, owner, or accessibility language.

## onAttachedToWindow

```java
protected void onAttachedToWindow();
```

Calls the superclass, registers a global-layout listener, and runs `refresh()`. While attached, each global layout refreshes the editor and keyboard state.

## onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Removes the listener from a live ViewTreeObserver. Clears the observer/editor references, sets keyboard state to false, cancels both children's pressed/armed touch states, then calls the superclass. Does not hide the keyboard. Reattachment registers the observer again.
