---
title: "WmComboGlyph"
slug: en/api/wmcomboglyph
---
[API](/en/api/)

Package-private arrow, popup binding, and key routing for choice fields.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmComboGlyph.java`

## Declaration

```text
final class WmComboGlyph implements View.OnAttachStateChangeListener
public void onViewAttachedToWindow(View view);
public void onViewDetachedFromWindow(View view);
```

Use the popup-binding APIs of [WmChoice](/en/api/wmchoice/) or [WmEditableChoice](/en/api/wmeditablechoice/). Popup detachment clears the binding.
