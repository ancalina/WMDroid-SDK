---
title: "WmPopup"
slug: ko/api/wmpopup
---
[API 색인](/ko/api/)

```java
public final class WmPopup extends FrameLayout
```

꼬리 있는 팝업의 제목과 본문을 배치한다. 호출자가 overlay에 추가하고 위치, 크기, 닫기, 버튼 callback을 관리한다. UI 스레드에서 생성, 갱신한다. 좌표와 크기는 논리 픽셀이다.

## 생성자

```java
public WmPopup(Context context,Typeface font,String caption,int anchor,int from,int to,View content);
```

`content`는 null이 아니고 부모가 없는 View여야 한다. 본문 컨테이너의 자식으로 추가하며 높이는 내용에 맞춘다. 팝업 내부의 빈 영역을 누르는 동작은 기본적으로 아무 작업도 하지 않는다.

`anchor`는 팝업 내부의 꼬리 x좌표이며 생성 후 유지한다. surface 좌표를 사용할 때는 [leftForAnchor](#leftforanchor) 반환값을 빼서 전달한다. `from`, `to`는 제목 배경의 왼쪽, 오른쪽 ARGB 색이다.

제목은 y=`28`, 높이 `44`, 좌우 여백 `20`이다. 본문은 y=`72`, 좌우, 아래 여백 `4`이며 아래 모서리를 반지름 `16`으로 clip한다. API 18 미만에서는 본문의 software layer로 clipping을 처리한다. 본문 스크롤과 버튼은 `content`에 구성한다.

## heading

```java
public final TextView heading;
```

팝업이 소유한 제목 View. 흰 글자, 단일 줄, 세로 중앙 정렬을 사용한다. `font`는 제목에만 적용한다. 내용의 글꼴은 호출자가 설정한다.

## leftForAnchor

```java
public static int leftForAnchor(int width,int anchor);
public static int leftForAnchor(int width,int anchor,int surfaceWidth);
```

surface에서 팝업의 왼쪽 x좌표를 반환한다. 여기의 `anchor`는 surface 좌표다. `width`는 `1..480`, `surfaceWidth`는 `480` 이상이어야 하며 위반은 `IllegalArgumentException`이다. 두 인자 overload는 `surfaceWidth=480`을 사용한다.

폭 `154`이면 후보 좌표는 `anchor−90`이다. 그 외에는 `max((surfaceWidth−width)/2, anchor+20−width)`를 사용하며 정수 나눗셈을 적용한다. 반환값은 `0..surfaceWidth−width`로 제한한다. 호출자가 반환값을 실제 배치에 적용해야 한다.

## setCaption

```java
public void setCaption(String caption);
```

제목 텍스트를 바꾼다. null은 빈 제목으로 표시한다.

## setColors

```java
public void setColors(int from,int to);
```

저장된 꼬리 위치를 유지하고 새 [WmBubble](/ko/api/wmbubble/) 배경을 적용한다. 두 ARGB 색은 제목과 꼬리의 수평 gradient에 사용하며 흰 본문, 검은 테두리는 유지한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmPopup.java`
