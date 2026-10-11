---
title: "WmTabDrawable"
slug: ko/api/wmtabdrawable
---
[API 색인](/ko/api/)

```java
public final class WmTabDrawable extends Drawable
```

property-sheet 하단 탭의 배경과 경계를 그린다.

## 생성자

```java
public WmTabDrawable(boolean selected,int face);
```

`selected`와 ARGB `face` 색을 저장한다. 선택 탭은 흰색, 비선택 탭은 `face`를 사용한다. 두 값은 생성 후 유지하므로 선택 상태나 색을 바꾸려면 Drawable을 교체한다.

## draw

```java
public void draw(Canvas c);
```

현재 bounds의 왼쪽·위쪽으로 Canvas를 이동하여 그린다. 아래 좌표는 bounds 내부 논리 픽셀이다.

| 상태 | 배경 | 검은 경계 |
| --- | --- | --- |
| 선택 | y=`0`부터 높이 전체 | 좌우 너비 `2`, y=`0`부터 |
| 비선택 | y=`1`부터 높이 전체 | 위쪽 y=`1`·높이 `2`, 좌우 너비 `2`, y=`3`부터 |

선택 탭은 위쪽 경계를 생략하여 본문의 흰 배경과 이어진다. 두 상태 모두 아래쪽 경계를 그리지 않는다. 비선택 탭의 첫 `1`픽셀 줄은 그대로 남는다. 정상 반환 시 Canvas 상태를 복원한다. bounds는 호출자가 설정한다.

## setAlpha · setColorFilter

```java
public void setAlpha(int a);
public void setColorFilter(ColorFilter f);
```

두 메서드는 그리기 결과를 바꾸지 않는다.

## getOpacity

```java
public int getOpacity();
```

항상 `PixelFormat.OPAQUE`를 반환한다. `face`의 alpha나 비선택 탭의 그리지 않은 첫 줄을 반영하지 않는다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmTabDrawable.java`
