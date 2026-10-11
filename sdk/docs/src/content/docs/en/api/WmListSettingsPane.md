---
title: "WmListSettingsPane"
slug: en/api/wmlistsettingspane
---
[API index](/en/api/)

```java
public final class WmListSettingsPane extends LinearLayout
```

Places a list beside a right command-button column. The caller supplies list scrolling, selection, persistence, and button actions. Construct on the UI thread. Dimensions below use logical pixels.

## Constructor

```java
public WmListSettingsPane(Context context,View list,View... buttons);
```

Adds `list` and each button as children of the container. The list and buttons must have no parent. A null list, button array, or button, a View with a parent, duplicate buttons, or reuse of the list as a button throws `IllegalArgumentException`. An empty button array is allowed.

Outer padding is `16` on the left/right and `4` on the top/bottom. Buttons appear vertically in array order, with size `144×40` and bottom margin `8`. Both columns fill the container's remaining height. The container itself does not scroll.

## listFrame

```java
public final FrameLayout listFrame;
```

Parent of the supplied list. Fills the width left after the command column. It has a white background, a gray `2`-pixel border, and equal inner padding to keep the list clear of the border. The list fills this inner area.

## commands

```java
public final LinearLayout commands;
```

Parent of the supplied buttons. Width is `152`, with left padding `8`. Increasing the container width expands only the list area; the command column retains its width.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmListSettingsPane.java`
