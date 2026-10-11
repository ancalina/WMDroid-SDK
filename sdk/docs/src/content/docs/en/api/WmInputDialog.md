---
title: "WmInputDialog"
slug: en/api/wminputdialog
---
[API](/en/api/)

Full-client dialog for a single name field and Done or Cancel commands.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmInputDialog.java`

## WmInputDialog

```text
public final class WmInputDialog extends FrameLayout implements WmControls.BackHandler,WmControls.ModalOwner,WmControls.RestyleListener,WmControls.LanguageListener
public WmInputDialog(Context context,Typeface font,Typeface bold,String labelText,int accent,int maxLength,Submit submit);
public static WmInputDialog find(View root);
public EditText editor();
public boolean isClosed();
public void onDismiss(Runnable action);
public void accept();
public void setBusy(boolean value);
public void dismiss();
public boolean back();
public boolean blocksOwnerCommands();
public View inputMethodOwner();
protected void onAttachedToWindow();
protected void onDetachedFromWindow();
public void onWmLanguageChanged(String language);
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```



font, bold, labelText, and submit are required; maxLength must be positive. Invalid arguments throw `IllegalArgumentException`. editor is a single-line EditText limited by Android LengthFilter in UTF-16 code units: `A` is1unit, `😀` is2units.

`accept()` passes text after String.trim() removes leading/trailing characters at or below U+0020 to submit. Empty text restores focus. busy or closed prevents submission. The callback runs on the main thread and does not close the dialog automatically. The caller chooses dismissal or error display.

`setBusy(true)` disables input and commands. While busy, dismiss and Back do not close it. dismiss hides the keyboard, removes the dialog from its parent, and runs onDismiss once. External detachment also marks it closed regardless of busy, clears submit, and calls registered onDismiss once. An onDismiss already handled by dismiss is not called again. Do not reuse a closed dialog.

`find()` searches visible subtrees from the last child and returns the topmost open dialog, or null. Owner commands are blocked; the internal panel owns input-method requests. Language and restyle hooks update the label, editor, and commands.

## WmInputDialog.Submit

```text
public interface Submit
void submit(String value);
```
