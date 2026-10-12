---
title: "WmBrowserFavoriteForm"
slug: en/api/wmbrowserfavoriteform
---
[API index](/en/api/)

```java
public final class WmBrowserFavoriteForm extends LinearLayout implements WmControls.LanguageListener
```

Form for a favorite name, address, and destination. The owner supplies destinations, validation, storage, and Add/Cancel commands.

## Constructor

```java
public WmBrowserFavoriteForm(Context context,Typeface font,String name,String url,String[] locations,int selected,int accent)
```

`name` and `url` are initial drafts. `locations` and `selected` supply the source choices and selection index for `WmChoice`. `accent` is the selection color. Null `font`, `name`, or `url` throws `IllegalArgumentException`. Input limits are 256 UTF-16 code units for the name and 2048 for the address. Locations must be nonempty without null entries; violations throw `IllegalArgumentException`. An out-of-range selected index throws `IndexOutOfBoundsException`. The destination selector is at most 330 logical pixels and shrinks to the available width.

## Controls

```java
public static final int NAME=12337,URL=12338,LOCATION=12340;
public static final int NAME_LABEL=12356,URL_LABEL=12357,LOCATION_LABEL=12358;
public static final int NAME_LIMIT=256,URL_LIMIT=2048;
public EditText nameField()
public EditText urlField()
public WmChoice locationField()
```

Constants identify input/label views and input limits. Accessors return the controls owned by the form. Read their current text and `locationField().selectedIndex()` when saving. The form does not normalize URLs or persist data.

## Language and measurement

```java
public void onWmLanguageChanged(String language)
protected void onMeasure(int widthSpec,int heightSpec)
```

Language changes update only the name, address, and location labels. `onMeasure` clamps the destination selector width.


The owner supplies the location popup. Register opening through locationField().[setOnOpenListener](/en/api/wmchoice/#onopenlistener) and attach/remove the popup under the [bindDropdown](/en/api/wmchoice/#binddropdown-isdroppeddown-setextendedui-isextendedui-searchcharacter) contract. Commit with choose(sourceIndex) using the original locations index. The form stores lists/selection; it does not create popups or persist.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmBrowserFavoriteForm.java`
