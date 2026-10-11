---
title: "WmCommandBar"
slug: en/api/wmcommandbar
---
[API](/en/api/)

```java
public final class WmCommandBar extends LinearLayout implements WmControls.RestyleListener
```

Bottom bar with two command keys and a central input-method area.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmCommandBar.java`

## Layout and centre

```java
public static final int KEY_WIDTH = 180;
public static final int CENTRE_WIDTH = 120;
public final LinearLayout centre;
```

Each outer key is `180×52` SDK logical pixels. `centre` takes the remaining width between the keys and centers its children. At logical width `480`, its width is `CENTRE_WIDTH == 120`. A different bar width changes only the center width.

`centre` is the internal container. Do not combine custom center UI with the input-method button installed by `inputMethods`.

## Constructors

```java
public WmCommandBar(Context context,Typeface font,int accent);
public WmCommandBar(Context context,Typeface font,WmBar paint);
```

Construct and modify on the UI thread; handle callbacks there as well. Both keys initially have empty labels and are disabled.

The `accent` constructor uses `new WmBar(context,true,accent)`. The `paint` constructor uses the supplied background directly. `paint` must be a non-null, softkey-mode [WmBar](/en/api/wmbar/). A title-mode background throws `IllegalStateException` during key styling.

## key

```java
public TextView key(boolean right);
```

Returns the internal right TextView for `right == true`, or the left one for false. Replacing its click/touch listeners removes the command behavior below; bind commands with `setCommand` or `setMenu`.

## inputMethods

```java
public void inputMethods(View owner);
```

`owner == null` removes only the existing input-method button and retains custom center children. A non-null owner first checks the current center children. Any child other than the existing input-method button causes `IllegalStateException` and preserves the existing button, input owner, and observer. After validation, removes the existing button and installs a new [WmSipButton](/en/api/wmsipbutton/).

`owner` is an editor or a UI root containing editors. Editor selection and button visibility follow `WmSipButton`. The owner must remove custom center children.

## setCommands · setCommand

```java
public void setCommands(String left,Runnable leftAction,String right,Runnable rightAction);
public void setCommand(boolean right,String label,Runnable action);
```

`setCommands` binds two ordinary commands, left then right. `setCommand` selects one side with `right`. A null `label` becomes an empty string; a null `action` disables the key. Labels appear as supplied without translation. Rebinding replaces the click/touch listeners and clears pressed/selected state.

A single-pointer DOWN followed by UP inside the key invokes `action` once. MOVE outside cancels the gesture; moving back inside does not rearm it. Multiple pointers, CANCEL, or a disabled key clear the press without activation.

Activation uses `key.performClick()` and checks the enabled state and [WmControls.ownerCommandsAllowed](/en/api/wmcontrols/). The callback runs directly without a delay or task queue.

## setMenu

```java
public void setMenu(boolean right,String label,Runnable action);
```

Binds a menu command that activates on DOWN. Arguments, labels, disabling, and command-owner checks follow `setCommand`. UP does not activate it again. Leaving the bounds or CANCEL after DOWN clears the press but does not undo the callback that already ran.

The owner creates, shows, and dismisses the menu in `action`.

## restyle · onWmRestyle

```java
public void restyle(Typeface font,WmBar paint);
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

`restyle` updates the bar background, both keys' fonts/font bindings/styles, and the attached input-method button background. Command labels, callbacks, enabled states, and custom center children remain unchanged. The constructor's `paint` requirements apply.

`onWmRestyle` uses `regular` and `new WmBar(getContext(),true,selection)`. It does not use `bold` or `tabColor`.

## styleKey

```java
public void styleKey(TextView key);
public static void styleKey(TextView key,WmBar paint);
```

The instance method styles a TextView with the current bar background; the static method uses the supplied softkey background. `key` and `paint` must not be null. A title-mode background throws `IllegalStateException`.

Applies center gravity, left/right padding `8`, top/bottom padding `0`, disabled touch sound, and the background's softButton. Text color precedence is disabled gray `0xff808080`, pressed/selected `paint.color(15)`, then normal `paint.color(14)`. It does not change fonts, sizes, layout, labels, listeners, or enabled state. The owner binds custom key behavior.

## shouldDelayChildPressedState

```java
public boolean shouldDelayChildPressedState();
```

Always returns false so the parent does not delay child press display.

## onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Clears the registered touches' armed state and both keys' pressed/selected states, then calls the superclass. Command bindings and center children remain unchanged.
