---
title: "WmWindow"
slug: ko/api/wmwindow
---
[API](/ko/api/)

```java
public final class WmWindow extends LinearLayout
public final FrameLayout client;
```

제목·본문·하단 명령·팝업을 하나의 논리 화면에 배치한다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmWindow.java`

## 생성자 · client

```java
public WmWindow(Context context);
```

UI 스레드에서 생성·변경한다. 세로 방향, 흰 배경, clickable 컨테이너로 시작한다. `client`에 본문 View를 넣는다.

제목·명령바는 각각 높이 `52` SDK 논리 픽셀, 너비 MATCH_PARENT다. `client`는 너비 MATCH_PARENT, 높이 `0`, weight `1`로 남은 높이를 사용한다. 기본 배치는 제목·본문·명령바 사이에 여백을 추가하지 않는다. 창의 사용 가능한 크기는 부모가 결정한다. `client`를 다른 부모로 옮기거나 제거하지 않는다.

## title

```java
public void title(View view);
```

`view`를 검사한 뒤 현재 제목을 제거하고 첫 번째 자식으로 연결한다. null·창 자신·이 창의 조상 View는 `IllegalArgumentException`이다. 부모 없는 조상도 거부한다. 부모가 있으면 현재 제목이면서 그 부모가 이 창인 경우만 허용하며, 그 외에는 `IllegalStateException`이다. 이 검사 실패는 기존 제목을 유지한다. 현재 제목의 재연결도 허용한다.

검사를 통과한 뒤 발생하는 모든 실패의 rollback을 보장하지는 않는다. 제목 내부의 로고·시계·상태·닫기 동작은 전달한 View와 소유자가 구현한다.

## commands

```java
public void commands(View view);
```

`title`과 같은 인수 검사를 현재 명령 슬롯에 적용한다. [WmCommandBar](/ko/api/wmcommandbar/)이면 기존 명령 View를 제거하기 전에 `inputMethods(client)`를 호출한다. 커스텀 중앙 자식 충돌로 이 호출이 실패하면 기존 명령 View는 유지한다.

검사와 입력기 연결을 통과하면 기존 명령 View를 제거하고 본문 아래, popupLayer 앞에 연결한다. 현재 명령 View도 다시 연결할 수 있다. 입력 소유자는 client로 설정한다. 이후 모든 실패의 rollback을 보장하지는 않는다. 일반 View의 소프트키·메뉴 동작은 소유자가 연결한다.

## popupLayer

```java
public FrameLayout popupLayer();
```

첫 호출에 내부 FrameLayout을 만들고 이후 같은 참조를 반환한다. 자식 목록에서는 명령바 뒤에 있지만 창 전체에 겹쳐 배치한다. popup 자식 좌표의 원점은 client 상단이 아니라 제목을 포함한 창의 왼쪽 위다.

소유자가 팝업 자식의 배치·교체·제거·배경 터치·닫기 동작을 관리한다. 이 메서드 자체는 배경을 채우거나 터치를 막거나 modal 상태를 등록하지 않는다. 반환 레이어를 다른 부모로 옮기거나 제거하지 않는다.

## onMeasure · onLayout

```java
protected void onMeasure(int width,int height);
protected void onLayout(boolean changed,int l,int t,int r,int b);
```

`width`·`height`는 Android MeasureSpec이다. superclass로 일반 세로 배치를 측정한 뒤 popupLayer만 창의 측정 너비·높이와 같은 EXACTLY 크기로 다시 측정한다. 팝업 내용이 창 크기를 늘리지는 않는다.

`onLayout`은 일반 자식을 배치한 뒤 popupLayer를 `(0,0,r-l,b-t)`에 겹쳐 놓는다. 외부 화면 좌표가 아닌 창 내부 좌표다.

## ownerCommandsAllowed

```java
public boolean ownerCommandsAllowed();
```

`!WmControls.blocksOwnerCommands(client)`를 반환한다. client 트리의 보이는 [ModalOwner](/ko/api/wmcontrols/)가 명령 차단을 요청하면 false다. popupLayer의 자식만으로는 이 판정이 바뀌지 않는다.

## dispatchTouchEvent

```java
public boolean dispatchTouchEvent(MotionEvent event);
```

DOWN에서만 명령바 gesture 차단 여부를 결정한다. 아래 조건을 모두 만족하면 이후 gesture를 소비한다.

- `ownerCommandsAllowed()`가 false다.
- DOWN 좌표가 현재 명령바의 `[left,right)×[top,bottom)` 안에 있다.
- 현재 WmCommandBar의 보이는 입력기 버튼 영역이 아니다.

입력기 예외 판정은 SIP를 refresh한 뒤 창 좌표의 버튼 영역을 검사한다. 차단 gesture는 영역을 떠나거나 modal 상태가 바뀌어도 UP 또는 CANCEL까지 소비하며 true를 반환한다. 그 외에는 superclass에 전달한다. 제목·client·팝업 터치를 직접 차단하지 않는다.

## onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

저장한 gesture 차단 상태를 false로 지운 뒤 superclass를 호출한다. client·제목·명령바·popupLayer 자식은 제거하지 않는다. 자식의 observer·작업·팝업 수명은 각 컨트롤과 소유자가 관리한다.
