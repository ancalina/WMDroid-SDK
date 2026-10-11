---
title: "WmCategoryNameForm"
slug: en/api/wmcategorynameform
---
```java
public final class WmCategoryNameForm extends FrameLayout implements WmControls.LanguageListener,WmControls.RestyleListener,WmControls.BackHandler
public WmCategoryNameForm(Context context, Typeface regular, Typeface bold, int accent)
public EditText nameField()
public void showNameWarning(Runnable acknowledged)
public void onWmLanguageChanged(String language)
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public boolean back()
```

New Category body. `regular` and `bold` must be non-null. The owner supplies the app title, Done/Cancel commands, validation and storage. The form requests input focus and the software keyboard when attached; detach requests keyboard dismissal.

`nameField()` returns the single-line editor. It limits input to 255 UTF-16 code units. It does not trim, validate or save the draft. Language and theme updates retain text and selection. The heading follows `accent`; heading and caption use the bold role.

| Constant | Value | Control |
|---|---:|---|
| `HEADING` | 32850 | New Category heading |
| `LABEL` | 25960 | Category caption |
| `NAME` | 24033 | Name editor |
| `NAME_LIMIT` | 255 | Editor input limit |

Coordinates use logical VGA pixels. Scale the whole surface once.

| Element | x, y | Width × height at 480px |
|---|---|---|
| Heading | 8, 10 | 420 × 36 |
| Divider | 0, 50 | 480 × 2 |
| Caption | 8, 72 | 120 × 40 |
| Editor | 132, 72 | 340 × 40 |

The divider fills the client width. The editor keeps an 8px trailing inset. Editor frame and divider coordinates were measured in the official USA WM6.1.4 emulator. Heading/caption rectangles project the dialog template around measured ink. Font sizes 24/26 are Android adaptations; exact GDI raster and Korean/OEM geometry remain unverified.

For the native New flow, Done with empty or ASCII-space-only text closes without insertion. An existing name also closes without changing checks. Otherwise, call [WmCategoryPicker.addCategory](/en/api/wmcategorypicker/#addcategory). Invalid input keeps New open. Cancel discards the input. The form does not implement these owner actions.

`showNameWarning()` displays the comma/semicolon warning. It does not validate input. Only one warning can be open. Underlying input and owner commands are blocked. OK or `back()` removes the warning, restores the caret, and invokes the nullable callback once. Detachment removes the warning without invoking the callback. The owner must save and restore a pending error across Activity recreation. Warning geometry uses the shared MessageBox adaptation; exact native GDI equivalence is unverified.

`back()` returns false when no warning is open.

Construct and modify on the UI thread. Update the complete form through `WmControls.localizeTree(form, language)` and `WmControls.restyleTree(form, regular, bold, selection, tabColor)`. onWmLanguageChanged updates authored labels and the input description. onWmRestyle updates heading/label bold faces and heading color; use the tree path to restyle the input too.
