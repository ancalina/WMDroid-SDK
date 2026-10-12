---
title: "WmBubble"
slug: ko/api/wmbubble
---
[API 색인](/ko/api/)

```java
public class WmBubble extends Drawable
```

흰 본문, 수평 gradient 제목, 꼬리, 검은 외곽선을 그리는 팝업 배경. 제목과 본문 View를 함께 배치하려면 [WmPopup](/ko/api/wmpopup/)을 사용한다.

## 생성자

```java
public WmBubble(int anchor,int from,int to);
```

`anchor`는 Drawable bounds 왼쪽에서 꼬리 끝까지의 논리 픽셀 거리다. `from`, `to`는 제목과 꼬리의 좌우 ARGB 색이다. 세 값은 생성 후 바뀌지 않는다. 색, 꼬리 위치를 바꾸려면 새 Drawable을 만든다.

## draw

```java
public void draw(Canvas c);
```

그리기 전에 bounds를 설정한다. 도형을 bounds 왼쪽, 위쪽 좌표로 이동하여 그리며, 크기는 bounds의 너비, 높이를 사용한다. 꼬리 끝은 내부 좌표 `(anchor, 2)`, 본문 상단은 `27`, 제목 하단은 `72`, 외곽선 너비는 `4` 논리 픽셀이다. 제목 gradient는 x=`4`에서 `width−4`까지 이어진다.

꼬리 밑변은 너비 `52`이며 `anchor < width/4`이면 오른쪽으로, 그 외에는 왼쪽으로 뻗는다. `width/4`는 정수 나눗셈이다. 밑변 위치만 조정하고 꼬리 끝은 제한하지 않는다. bounds 밖 꼬리가 필요 없으면 호출자가 적절한 `anchor`를 선택하거나 Canvas를 clip한다. 본문, 꼬리, 제목이 같은 윤곽을 사용하므로 연결 경계에 별도 선을 그리지 않는다. 정상 반환 시 Canvas의 이전 상태를 복원한다.

## setAlpha, setColorFilter

```java
public void setAlpha(int alpha);
public void setColorFilter(android.graphics.ColorFilter filter);
```

두 메서드는 그리기 결과나 저장된 색을 바꾸지 않는다. `setAlpha`로 전체 배경을 흐리게 할 수 없다.

## getOpacity

```java
public int getOpacity();
```

항상 `PixelFormat.TRANSLUCENT`를 반환한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmBubble.java`
