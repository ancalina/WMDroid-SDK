---
title: "WmScrollView"
slug: ko/api/wmscrollview
---
[API](/ko/api/)

```java
public class WmScrollView extends FrameLayout
```

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmScrollView.java`

## 생성자

```java
public WmScrollView(Context context,View contents);
public WmScrollView(Context context,View contents,int contentHeight);
public WmScrollView(Context context,View contents,int contentHeight,int face);
```

UI 스레드에서 생성·변경한다. `contents`는 null이 아닌 부모 없는 View여야 한다. 생성자가 내부 Android ScrollView에 연결하므로 다른 부모로 옮기지 않는다. 내용과 View 수명·데이터는 소유자가 관리한다.

기본 `contentHeight`는 WRAP_CONTENT이며 다른 overload는 전달한 Android LayoutParams 높이를 사용한다. 양수이면 contents의 minimumHeight도 같은 값으로 설정한다. 내용 너비는 MATCH_PARENT다. fillViewport는 false이므로 짧은 내용을 viewport 높이로 늘리지 않는다. Android 스크롤바와 overscroll 효과는 사용하지 않는다.

기본 `face`는 `0xffc9e7e9`이며 [WmScrollBar](/ko/api/wmscrollbar/)의 컨트롤 면 색이다. 오른쪽 스크롤바 너비는 `26` SDK 논리 픽셀이다. 내용이 넘칠 때만 해당 공간을 예약한다.

스크롤바의 기본 line 이동량은 `44`다. choice keyboard가 활성화된 WmRadioMenu는 첫 행의 LayoutParams 높이를 line 이동량으로 사용하고, page 이동은 메뉴와 viewport 중 작은 높이에서 위·아래 padding을 뺀 영역에 들어가는 완전한 행 수를 기준으로 합니다. 두 행 이상이면 한 행을 겹치도록 한 행 적게, 한 행 이하이면 그 행 수만큼 이동합니다. 이동 거리는 이동 행 수×첫 행의 실제 높이입니다. 빈 메뉴의 page 이동량은 `0`이다.

## onMeasure

```java
protected void onMeasure(int width,int height);
```

인수는 Android MeasureSpec이다. 첫 superclass 측정 후 viewport 측정 높이가 양수이고 내용 측정 높이가 더 크면 스크롤바를 VISIBLE로 하고 내부 ScrollView 오른쪽 margin을 `26`으로 지정한다. 넘치지 않으면 GONE·margin `0`이며 진행 중인 스크롤바 동작도 멈춘다. 상태가 바뀌면 다시 측정하므로 첫 draw 전에 내용 너비를 결정한다.

스크롤·내용/viewport layout 변경은 스크롤바 갱신을 예약한다. API18 이상은 layout 실행 중이면 갱신을 다시 예약한다. API14–17은 `isInLayout()`을 호출하지 않는다.

## scrollToPosition

```java
public void scrollToPosition(int y);
```

fling을 멈추고 음수 `y`를 `0`으로 제한한 요청을 저장한다. viewport 높이가 양수이고 이 컨테이너·내부 ScrollView·contents에 layout 요청이 없으면 즉시 적용한다. 그 외에는 UI 큐 갱신과 layout을 기다린다. 새 요청은 이전 대기 요청을 대체한다.

최대 offset은 적용 시 Android ScrollView가 당시 내용/viewport 범위로 제한한다. 아직 적용하지 않은 요청은 실제 최대 offset보다 클 수 있다. enabled 상태의 터치 DOWN이나 detach는 대기 요청을 지운다.

## getScrollPosition · getRequestedScrollPosition

```java
public int getScrollPosition();
public int getRequestedScrollPosition();
```

두 값의 단위는 논리 픽셀이다. `getScrollPosition()`은 내부 ScrollView의 현재 실제 Y offset이다. `getRequestedScrollPosition()`은 대기 요청이 있으면 그 요청값, 없으면 실제 offset을 반환한다. layout 전 복원·글꼴 변경 위치를 보존하려면 요청값을 사용한다.

## setContentHeight

```java
public void setContentHeight(int height);
```

`height <= 0`이면 `IllegalArgumentException`이다. 양수 높이를 contents의 minimumHeight와 LayoutParams.height에 적용하고 스크롤바 갱신을 예약한다. 글꼴로 높이를 계산하는 Canvas 본문에 사용할 수 있다. 기존 offset을 새 범위에 맞추는 처리는 layout과 Android ScrollView가 수행한다.

## stopFling

```java
public void stopFling();
```

내부 ScrollView의 터치 처리에 DOWN→CANCEL과 intercept CANCEL을 직접 전달하여 fling을 중단한다. contents로 이벤트를 dispatch하지 않으므로 내용 클릭·선택 콜백을 실행하지 않는다. 저장된 위치 요청을 지우지는 않는다.

## setEnabled · dispatchKeyEvent

```java
public void setEnabled(boolean enabled);
public boolean dispatchKeyEvent(android.view.KeyEvent event);
```

내부 ScrollView와 스크롤바에 enabled 상태를 전달한다. contents의 enabled 값은 바꾸지 않는다. false를 설정하면 thumb gesture 상태를 지우고 fling을 멈춘다. enabled→disabled 전환은 진행 중인 자식 터치에 CANCEL도 보낸다. 대기 위치 요청은 유지한다.

`dispatchKeyEvent`는 비활성 상태에서 false를 반환하고, 활성 상태에서는 superclass 결과를 반환한다.

## dispatchTouchEvent

```java
public boolean dispatchTouchEvent(MotionEvent event);
```

비활성 상태에서는 false다. 활성 상태의 DOWN은 대기 위치 요청을 지운 뒤 [WmScrollBar.hitThumb](/ko/api/wmscrollbar/)의 확장 히트 영역을 검사한다. thumb gesture면 스크롤바 로컬 좌표로 변환하여 해당 이벤트와 이후 이벤트를 전달하고 스크롤바의 처리 결과를 반환한다. UP·CANCEL에서 thumb gesture를 종료한다.

그 외에는 superclass에 전달하여 일반 손가락 스크롤·내용 터치를 처리한다. 확장 thumb 영역은 보이는 스크롤바의 왼쪽 내용 일부와 겹칠 수 있다.

## onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

예약된 스크롤바 갱신을 제거하고 대기 위치 요청·thumb gesture를 지운다. 스크롤바 동작을 멈춘 뒤 superclass를 호출한다. contents를 제거하거나 실제 offset을 명시적으로 초기화하지 않는다. 재연결 복원에 필요한 위치는 소유자가 저장한다.
