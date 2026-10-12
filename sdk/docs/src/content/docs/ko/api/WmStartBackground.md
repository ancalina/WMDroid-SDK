---
title: "WmStartBackground"
slug: ko/api/wmstartbackground
---
[API 색인](/ko/api/)

```java
public class WmStartBackground extends Drawable
```

시작 메뉴의 반복 텍스처와 검은 테두리를 그린다.

## 생성자

```java
public WmStartBackground(Bitmap bitmap);
```

null은 `IllegalArgumentException`이다. Bitmap을 복사하지 않고 shader에서 참조한다. Drawable 사용이 끝날 때까지 Bitmap을 recycle하지 않는다. Bitmap 소유권은 호출자에게 남는다.

## draw

```java
public void draw(Canvas canvas);
```

현재 bounds를 양방향 반복 텍스처로 채운다. Bitmap을 늘리지 않고 shader offset `(-2, -2)`를 적용한다. 타일 기준은 현재 Canvas 좌표이며 bounds 왼쪽, 위쪽으로 별도 이동하지 않는다. 표면 전체를 배율로 확대하면 타일도 같은 배율로 확대된다.

bounds 각 변에서 `1` 논리 픽셀 안쪽에 중심선을 둔 너비 `2`의 검은 테두리를 그린다. bounds는 호출자가 설정한다.

## setAlpha, setColorFilter

```java
public void setAlpha(int alpha);
public void setColorFilter(ColorFilter filter);
```

텍스처 Paint에만 적용하고 `invalidateSelf()`를 호출한다. 검은 테두리는 바꾸지 않는다. `alpha`는 Paint의 `0..255` 값이며, null filter는 기존 필터를 해제한다.

## getOpacity

```java
public int getOpacity();
```

항상 `PixelFormat.OPAQUE`를 반환한다. Bitmap의 투명도나 설정한 alpha를 반영하지 않으므로 전체 도형이 불투명하다는 판정에 사용하지 않는다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmStartBackground.java`
