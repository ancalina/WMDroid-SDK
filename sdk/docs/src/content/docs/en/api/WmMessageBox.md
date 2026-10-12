---
title: "WmMessageBox"
slug: en/api/wmmessagebox
---
[API](/en/api/)

Modal dialog for acknowledgment or a Yes/No choice.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmMessageBox.java`

```text
public final class WmMessageBox extends FrameLayout implements WmControls.BackHandler,WmControls.RestyleListener,WmControls.LanguageListener,WmControls.ModalOwner
```

## Icon

```text
public enum Icon { WARNING, QUESTION }
```

## Constructor, confirm

```text
public WmMessageBox(Context context,Typeface font,Locale locale,String caption,String message,int accent,Runnable acknowledged);
public static WmMessageBox confirm(Context context,Typeface font,Locale locale,String caption,String message,int accent,boolean defaultYes,WmRadioMenu.Selection answer);
public static WmMessageBox confirm(Context context,Typeface font,Locale locale,String caption,String message,int accent,boolean defaultYes,Icon icon,WmRadioMenu.Selection answer);
```

Create and change the dialog on the UI thread. Null font, locale, caption, or message throws `IllegalArgumentException`. The constructor displays an OK button. acknowledged can be null.

`confirm()` displays Yes and No buttons. The default icon is WARNING. Null icon or answer throws `IllegalArgumentException`. `defaultYes` selects the initial focus. answer receives 0 for Yes and 1 for No. A selection or OK callback runs once. Remove the View in the callback; the class does not remove itself. Detachment releases callbacks without calling them.

## captionField, messageField, okButton, yesButton, noButton

```text
public TextView captionField();
public TextView messageField();
public ImageView okButton();
public TextView yesButton();
public TextView noButton();
```

Returns the internal display Views. yesButton and noButton return null for an acknowledgment dialog. Use the binding methods below for text changes that must survive language changes.

## setCaptionContext, setMessageFormat, setLocale

```text
public void setCaptionContext(String key);
public void setMessageFormat(String pattern,Object... values);
public void setLocale(Locale locale);
```

The caption key selects a translation-catalog context. Null restores authored-text lookup. Message formatting follows [WmControls.bindFormat](/en/api/wmcontrols/#bindformat). The values array is copied. `setLocale(null)` throws `IllegalArgumentException`. Language changes preserve format values.

## back, blocksOwnerCommands, focusSearch

```text
public boolean back();
public boolean blocksOwnerCommands();
public View focusSearch(View focused,int direction);
```

`back()` acknowledges a message or selects No in a confirmation, then returns true. `blocksOwnerCommands()` returns true. Focus stays within OK or Yes/No buttons. The input method cannot target an underlying editor.

## Lifecycle, onMeasure, onWmLanguageChanged, onWmRestyle

```text
protected void onMeasure(int widthSpec,int heightSpec);
protected void onAttachedToWindow();
protected void onDetachedFromWindow();
public void onWmLanguageChanged(String language);
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Attachment focuses the default button. Long messages scroll within the available height. Language and theme callbacks update captions, body text, buttons, and icons. Titles use bold; body text uses regular.
