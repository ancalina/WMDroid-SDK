---
title: "WmTabStrip"
slug: ko/api/wmtabstrip
---
[API](/ko/api/)

```java
public final class WmTabStrip extends LinearLayout implements WmControls.RestyleListener
```

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmTabStrip.java`

## 생성자

```java
public WmTabStrip(Context context,int color);
```

UI 스레드에서 생성·변경하고 콜백도 같은 스레드에서 실행한다. `color`는 strip과 화살표의 기본 면 색이다. 기본 높이는 `40` SDK 논리 픽셀, 너비는 MATCH_PARENT다. 배치에는 여백이 없으며 탭을 아래쪽에 정렬한다. 페이지 본문·페이지 전환은 소유자가 관리한다.

## addTab

```java
public TextView addTab(String title,Typeface font,Runnable action);
```

초기 미선택 TextView를 만들고 내부 참조를 반환한다. 제목은 그대로 표시하고 공유 글꼴·탭 스타일을 적용한다. 초기 너비는 `ceil(WmFonts.measure(...))+32`, 높이는 `40` 논리 픽셀이다. 터치 모드에서는 포커스를 받지 않으며 키보드 포커스는 허용한다.

`action`은 null이 아니어야 한다. 생성 시 검사하지 않으므로 null은 실행 시 `NullPointerException`을 발생시킨다. 클릭 시 strip과 탭이 모두 enabled이면 동기 호출한다. 소유자가 본문과 선택 표시를 갱신해야 하며 [WmControls.tab](/ko/api/wmcontrols/) 또는 [WmPropertySheet](/ko/api/wmpropertysheet/)를 사용한다. 선택 변경을 알리려면 직접 setSelected만 호출하지 않고 공유 tab 스타일을 적용한다.

제목 변경은 너비를 다시 계산하고 선택 탭 표시를 요청한다. 글꼴만 바꾸면 측정은 기존 LayoutParams 너비를 사용한다. [WmControls.restyleTree](/ko/api/wmcontrols/)를 사용하거나 `WmControls.tabWidth(tab)`를 배치 너비에 다시 적용한다. 내부 탭·키 리스너·overflow 구조를 교체하지 않는다.

## 키보드 이동

enabled strip의 enabled 탭에서 Left/Right는 첫 selected 탭을 기준으로 한 index 이동한다. 첫 탭이 selected일 때 Left DOWN, 마지막 탭이 selected일 때 Right DOWN은 listener가 false를 반환하여 owner 처리를 허용한다. 이 경계에서는 선택을 바꾸지 않는다. Home/End는 첫/마지막 탭을 대상으로 한다. 포커스 탭을 기준으로 삼지 않으며 비활성 대상은 건너뛰지 않는다. modifier는 검사하지 않는다.

그 외 이동 DOWN에서 다른 enabled 대상의 performClick을 호출한 뒤 실제 첫 selected 탭에 포커스를 요청한다. 선택 gate의 거부·redirect·재진입 결과를 따른다. 선택이 없으면 Left/Right는 첫 탭을 대상으로 한다. 해당 UP은 경계에서도 계속 소비한다. 다른 키·action·비활성 상태는 listener에서 처리하지 않는다.

## onMeasure

```java
protected void onMeasure(int widthSpec,int heightSpec);
```

인수는 Android MeasureSpec이다. 각 탭을 `max(0,LayoutParams.width)×40` EXACTLY로 측정한다. strip은 전체 탭 너비와 높이 `40`을 부모 제약에 맞춰 resolve한다.

전체 탭 너비가 측정 너비보다 크면 오른쪽 overflow를 표시한다. 화살표 한쪽 크기 `side`는 첫 탭의 `round(descent-ascent)+8`을 `0..min(stripHeight,stripWidth/2)`로 제한한다. 탭 viewport는 `stripWidth−2*side`다. 이 글꼴 계산은 Android 적응값이다.

너비·글꼴 metric·선택 변경 시 다음 측정에서 선택 탭을 viewport에 들어오도록 조정한다. 탭 자체가 viewport보다 넓으면 시작 위치를 맞추지만 전체 노출을 보장하지 않는다. 화살표 스크롤은 선택·페이지를 바꾸지 않는다.

## onLayout · dispatchDraw

```java
protected void onLayout(boolean changed,int l,int t,int r,int b);
protected void dispatchDraw(Canvas canvas);
```

탭을 첫 표시 index의 누적 너비만큼 왼쪽으로 옮기고 아래쪽에 정렬한다. overflow는 오른쪽 아래에 배치하며 좌우 화살표의 히트 영역은 `2` 논리 픽셀 겹친다.

탭은 viewport로 clip하여 그리고 overflow는 그 밖에서 그린다. 탭 선택 배경·테두리는 [WmTabDrawable](/ko/api/wmtabdrawable/)을 따른다. 화살표는 검은 `2` 픽셀 테두리와 strip 면 색을 사용한다. 눌림 면은 검정, 화살표는 기본 검정·눌림 흰색·비활성 SHADOW다.

## dispatchTouchEvent

```java
public boolean dispatchTouchEvent(MotionEvent event);
```

비활성 strip은 false를 반환한다. overflow가 있고 DOWN이 viewport 오른쪽·화살표 위 영역이면 소비하여 숨은 탭을 누르지 못하게 한다. 그 외에는 superclass에 전달한다.

화살표 DOWN은 한 index 즉시 이동한다. `530ms` 후 반복을 시작하고 이후 `100ms`마다 이동한다. DOWN 이후 경과 시간에 따른 반복 이동량은 아래와 같다.

| 경과 시간 | index 이동량 |
| --- | --- |
| `0..2047ms` | `1` |
| `2048..5119ms` | `5` |
| `5120ms` 이상 | `20` |

양쪽 화살표는 strip이 enabled이고 이동 가능한 index 범위가 있을 때 함께 활성화된다. 범위 끝에서는 이동하지 않는다. UP·CANCEL·추적 포인터 UP·영역 이탈·비활성화·숨김·window 숨김·detach는 반복과 눌림을 취소한다. UP에서 추가 이동하지 않는다.

## setEnabled

```java
public void setEnabled(boolean enabled);
```

enabled→disabled 전환에서 superclass로 CANCEL을 보내 진행 중인 자식 터치를 취소한 뒤 화살표 상태를 갱신한다. 개별 탭의 enabled 값은 유지한다. 비활성 부모에서는 탭 action·키보드 이동·터치 dispatch를 막는다. 다시 켜도 개별 비활성 탭은 그대로다.

## onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

strip과 화살표 면 색을 `tabColor`로 갱신하고 재측정을 요청한다. `regular`·`bold`·`selection`은 직접 사용하지 않는다. 탭 글꼴·배경·텍스트 색 변경은 `WmControls.restyleTree` 또는 소유자의 스타일 갱신으로 처리한다.

![Android API 14 렌더](/WmTabStrip.png)
