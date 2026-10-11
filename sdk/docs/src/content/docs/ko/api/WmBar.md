---
title: "WmBar"
slug: ko/api/wmbar
---
[API](/ko/api/)

제목바·하단 명령바와 버튼 배경을 그리는 Drawable.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmBar.java`

```java
public class WmBar extends Drawable
```

## 생성자

```java
public WmBar(Context context,boolean softkey,int base);
```

`softkey=true`는 하단 명령바, false는 제목바입니다. `base`는 ARGB 테마색입니다. [WmResources.openAsset](/ko/api/wmresources/)으로 `themes/gradients.json`을 동기적으로 읽는다. 유효한 표는 중복 없는 index0–72의73개 record와 정수 HSB 값0–0xffffff를 가진다. 256KiB 초과 읽기·잘못된 JSON/record는 fallback을 사용한다. 표의 hue를 base hue로 바꾸고 각 record의 saturation/brightness는 유지한다. 회색 base는 회색조 슬롯을 만든다. Fallback은 base 휘도를 최대100으로 낮추고 alpha를 불투명으로 만들며 전경 슬롯14/15/37/38/39를 흰색으로 둔다. 인스턴스 색은 고정되므로 테마 변경 후 새 bar를 생성한다.

## color

```java
public int color(int index);
```

`index`는 0–72입니다. 해당 슬롯의 ARGB를 반환합니다. 범위 밖 값은 `ArrayIndexOutOfBoundsException`입니다.

## softButton

```java
public Drawable softButton(View cell);
```

하단 명령바의 버튼 View를 전달하세요. 제목바에서 호출하면 `IllegalStateException`입니다. 반환 Drawable은 enabled 상태에서 pressed 또는 selected일 때만 강조 배경을 그립니다. `cell`의 parent 폭과 위치로 원래 명령바의 해당 부분을 선택합니다. 같은 명령바의 cell 배경으로 사용하세요. cell·bar 참조는 Drawable 수명 동안 유지한다. 비활성 배경은 아무것도 그리지 않으므로 아래에 bar 배경을 유지한다. 강조는 cell 위치로 선택한 왼쪽 또는 오른쪽 끝의180논리 픽셀 key 영역으로 제한한다.

## titleButton

```java
public Drawable titleButton(View cell,boolean start,boolean close);
```

제목바의 버튼 View를 전달하세요. 하단 명령바에서 호출하면 `IllegalStateException`입니다. `start`는 시작 버튼의 강조·구분선, `close`는 닫기 버튼 테두리를 적용합니다. pressed 또는 selected는 enabled 상태 없이도 강조를 켠다. `start=true`일 때만 강조를 `min(180, cellWidth)` 논리 픽셀로 제한한다. 다른 제목 cell은 cell로 잘린 공유 bar 영역을 사용한다. 보통 Start 상태는 구분선을 그리며 `close=true`는 두 상태 모두 테두리를 그린다. 두 flag는 독립적이며 함께 true여도 된다. cell·bar 참조는 Drawable 수명 동안 유지한다.

버튼 배경은 parent View 폭이 양수면 그 폭을, 아니면 bar bounds 폭 또는 `WmMetrics.WIDTH`를 사용한다. Cell 위치는 bar와 같은 parent 좌표를 사용해야 한다. Factory는 null cell을 거부하지 않지만 그릴 때 cell에 접근하면 `NullPointerException`이 발생한다. 반환 배경마다 독립 alpha/filter를 가지고 변경 시 invalidate하며 `PixelFormat.TRANSLUCENT`를 반환한다. Bar alpha/filter 변경은 버튼 배경에 전파되지 않는다.

## draw · setAlpha · setColorFilter · getOpacity

```java
public void draw(Canvas canvas);
public void setAlpha(int alpha);
public void setColorFilter(ColorFilter filter);
public int getOpacity();
```

bounds에 보통 상태 bitmap을 그린다. 52행 원본은 bounds 높이에 맞춰 확대·축소한다. 폭360 미만은480폭 bitmap을 재사용하고 다른 폭은180픽셀 양끝 ramp를 유지하며 중앙을 늘린다. Alpha·ColorFilter 변경은 이 Drawable을 invalidate한다. `getOpacity()`는 alpha 변경 후에도 항상 `PixelFormat.OPAQUE`를 반환한다. View 배경의 상태·bounds 변경은 UI thread에서 수행한다.
