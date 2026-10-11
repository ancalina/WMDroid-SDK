---
title: "WmCloseGlyph"
slug: ko/api/wmcloseglyph
---
[API](/ko/api/)

제목바의 OK 또는 X 그림과 눌림 색을 제공합니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmCloseGlyph.java`

## 선언

```text
public final class WmCloseGlyph extends Drawable
public WmCloseGlyph(boolean ok,int normalColor,int pressedColor);
public boolean isStateful();
protected boolean onStateChange(int[] states);
public void draw(Canvas canvas);
public int getIntrinsicWidth();
public int getIntrinsicHeight();
public void setAlpha(int value);
public int getAlpha();
public void setColorFilter(ColorFilter filter);
public ColorFilter getColorFilter();
public int getOpacity();
```

ok=true는 OK, false는 X입니다. intrinsic 크기는 46×52 논리 픽셀입니다. 그림 내부의 여백을 포함해 이 크기로 배치하세요.

pressed 또는 selected state에서 pressedColor를 사용합니다. 다른 상태에서는 normalColor를 사용합니다. onStateChange는 pressed 또는 selected 여부를 합친 활성 값이 바뀔 때만 true를 반환합니다. 다른 state 변화는 false입니다. setAlpha는 0–255로 제한하며 색 자체의 alpha와 곱합니다. getColorFilter는 현재 filter 참조를 반환합니다. getOpacity는 TRANSLUCENT입니다.
