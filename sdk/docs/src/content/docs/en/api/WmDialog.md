---
title: "WmDialog"
slug: en/api/wmdialog
---
[API index](/en/api/)

```java
public class WmDialog extends LinearLayout
```

Manages default-button appearance and Enter/Escape commands in an embedded dialog. Use on the UI thread.

## Constructor

```java
public WmDialog(Context context);
```

Creates an empty container. The caller supplies child layout, content, and command callbacks.

## setDefaultButton, setCancelButton

```java
public final void setDefaultButton(TextView button);
public final void setCancelButton(TextView button);
```

Add the button as a child, apply [WmControls.button](/en/api/wmcontrols/), then register it. Only SDK push buttons descended from this dialog are allowed; buttons in a nested WmDialog are excluded. Violations throw `IllegalArgumentException`. Null clears the corresponding registration.

`setDefaultButton` changes the stored default and immediately refreshes its appearance. A focused button can still become the displayed default after null clears the registration. `setCancelButton` changes the Escape command without refreshing default-button appearance.

## refreshDialogDefault

```java
protected final void refreshDialogDefault();
```

Displays the focused owned SDK push button as default when every View from that button through the dialog is enabled and VISIBLE. Otherwise, uses the stored default. If the stored button no longer belongs to this dialog, clears the displayed default. Removes the previous default marker before applying the next one. Default appearance does not guarantee that a key can activate the button.

## dispatchKeyEvent

```java
public boolean dispatchKeyEvent(KeyEvent event);
```

Refreshes default appearance, tries [WmControls.dialogButtonKey](/en/api/wmcontrols/), then passes unhandled events to the superclass. Returns the handling result.

Enter and NUMPAD_ENTER target the focused SDK push button. When focus is on the dialog itself or SDK plain text, they target the stored default. Editors, other controls, and absent focus do not fall back to the stored default. Escape targets the registered cancel button.

Commands do not run when a child modal blocks owner commands or focus belongs to a nested dialog. An owned push-button target consumes DOWN and UP. Only an uncanceled DOWN clicks the target, and every View from the button through the dialog must be enabled and VISIBLE. Disabled or hidden targets still consume these keys without a click.

## onAttachedToWindow, onDetachedFromWindow

```java
protected void onAttachedToWindow();
protected void onDetachedFromWindow();
```

Attachment registers a global focus-change observer and refreshes the default marker. Detachment removes the observer and current marker. Stored default and cancel registrations remain. Call the superclass hook from overrides to preserve this lifetime handling.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmDialog.java`
