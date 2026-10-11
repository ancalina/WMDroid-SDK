---
title: "WmTouchSoundOptions"
slug: en/api/wmtouchsoundoptions
---
[API](/en/api/)

```java
public final class WmTouchSoundOptions extends LinearLayout
```

UI for enabling touch sound and selecting its level. The owner saves settings and calls [WmTouchFeedback](/en/api/wmtouchfeedback/) for playback.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmTouchSoundOptions.java`

## Constructor

```java
public WmTouchSoundOptions(Context context,Typeface font,String language,boolean on,int level,Listener listener);
```

Construct on the UI thread. `on` sets the initial enabled state. `level` must be `1` (soft) or `2` (loud). An out-of-range `level` or null `listener` throws `IllegalArgumentException`. A valid `level` is required even when sound is disabled.

Listeners are registered after the initial selection is set, so construction does not invoke the callback. `on == false` disables both level choices and retains the selected level.

`font` applies to the shared checkbox, radio, and text styles. `language` supplies the [WmControls.bindText](/en/api/wmcontrols/#bindtext) language binding. Apply later language changes with `WmControls.localizeTree`.

The label, checkbox, and level choices each occupy `44` SDK logical pixels in height. Outer padding is `8` on the left, top, and right, and `0` on the bottom. The level row has left padding `36` and two radios of width `140` each.

## Listener.changed

```java
public interface Listener
void changed(boolean enabled,int level);
```

Checkbox or radio selection changes invoke this callback synchronously within the change handler. `enabled` is the current enabled state. `level` is the selected `1` or `2`.

A checkbox change updates both radios' enabled state before invoking the callback. Disabling or enabling sound retains the selected level. A level selection does not change the enabled state.

Save settings in the callback and update the playback policy for subsequent handled touches. This control does not play sound.
