---
title: "WmComboGlyph"
slug: ko/api/wmcomboglyph
---
[API](/ko/api/)

선택 필드의 화살표·팝업 연결·키 전달을 관리하는 package-private 구현입니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmComboGlyph.java`

## 선언

```text
final class WmComboGlyph implements View.OnAttachStateChangeListener
public void onViewAttachedToWindow(View view);
public void onViewDetachedFromWindow(View view);
```

외부 앱은 [WmChoice](/ko/api/wmchoice/) 또는 [WmEditableChoice](/ko/api/wmeditablechoice/)의 popup binding API를 사용하세요. 팝업 detach 시 연결을 해제합니다.
