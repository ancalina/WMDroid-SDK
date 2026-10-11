---
title: "WmBrowserFolderForm"
slug: en/api/wmbrowserfolderform
---
[API index](/en/api/)

```java
public final class WmBrowserFolderForm extends LinearLayout implements WmControls.LanguageListener
```

Name form for a favorites folder. The owner validates, creates, and stores the folder.

## Constructor

```java
public WmBrowserFolderForm(Context context,Typeface font)
public WmBrowserFolderForm(Context context,Typeface font,String draft)
```

The first constructor uses an empty draft. Null `font` or `draft` throws `IllegalArgumentException`. The single-line editor limits input to `WmDocumentProperties.NAME_LIMIT`, 128 UTF-16 code units.

## Name control

```java
public static final int NAME=12339,NAME_LABEL=12356;
public EditText nameField()
public void onWmLanguageChanged(String language)
```

`NAME` and `NAME_LABEL` are the input and label view IDs. `nameField()` returns the editor for reading or changing the draft. Language changes update only the name label.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmBrowserFolderForm.java`
