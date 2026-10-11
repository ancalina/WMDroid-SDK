---
title: "WmIconCaption"
slug: ko/api/wmiconcaption
---
[API 색인](/ko/api/)

```java
public final class WmIconCaption extends TextView implements WmControls.RestyleListener
```

아이콘 목록의 Android Layout이 계산한 표시 줄 영역에 선택 사각형을 그리는 TextView. Android Layout으로 측정한다. 아이콘 선택 효과·선택 정책은 소유 앱이 담당한다.

## 생성자

```java
public WmIconCaption(Context context,Typeface font,int selection)
```

위쪽·가로 중앙 정렬, 최대 두 줄, 끝 말줄임. 비활성 텍스트는 SHADOW, 활성 pressed/selected/activated는 흰색, 나머지는 검정. selection은 선택 배경 ARGB 색.

## 선택 색·여백

```java
public void setSelectionColor(int color)
public void setSelectionPadding(int horizontal,int vertical)
```

색 변경 시 다시 그린다. 여백은 기본 0이며 논리 픽셀 단위로 표시 줄 사각형을 확장한다. 음수 여백은 IllegalArgumentException. 확장 결과는 텍스트 viewport 안으로 자른다.

## 줄 제한

```java
public void setMaxLines(int maxLines)
public void setLines(int lines)
```

TextView의 줄 제한을 설정하며 선택 영역 계산에도 같은 제한을 적용한다. 렌더링은 TextView가 담당한다.

## copySelectionBounds

```java
public boolean copySelectionBounds(Rect target)
```

측정·배치 후 호출. 실제 칠할 viewport-local 사각형을 target에 복사하고 true 반환. enabled와 pressed/selected/activated 중 하나가 필요. 빈 텍스트·layout 없음·크기 0·표시 줄 영역 없음은 target을 비우고 false. null target은 IllegalArgumentException. 뷰 상태는 변경하지 않는다. 경계는 바깥쪽 정수로 반올림하고 패딩·스크롤·줄 제한·clip 반영.

## 그리기·스타일

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
protected void drawableStateChanged()
protected void onDraw(Canvas canvas)
```

restyle은 선택 색만 적용한다. 글꼴은 restyleTree의 TextView 처리에 맡긴다. 상태 변경은 invalidate. onDraw는 선택 배경을 먼저, 텍스트를 나중에 그린다.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmIconCaption.java`

각 표시 줄의 getLineLeft/Right·getLineTop/Bottom 경계를 합칩니다. 글리프 raster의 픽셀 외곽을 측정하지 않습니다.
