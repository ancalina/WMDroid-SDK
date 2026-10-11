---
title: "WmScrollBar"
slug: ko/api/wmscrollbar
---
[API](/ko/api/)

```java
public class WmScrollBar extends View implements WmControls.RestyleListener
```

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmScrollBar.java`

## 생성자

```java
public WmScrollBar(Context context,Host host,int face);
```

UI 스레드에서 생성·변경한다. `host`는 null이 아니어야 하며 생성 시 검사하지 않는다. `face`는 화살표·thumb 면 색이다. 부모가 크기를 지정하며 기본 표준 너비는 `26` SDK 논리 픽셀이다. 키보드 포커스를 허용한다. 접근성 설명은 영어 바인딩이며 API16 이상은 importantForAccessibility를 YES로 지정한다.

터치 DOWN 처리에는 부모가 필요하다. 단독 View에 직접 DOWN을 호출하지 말고 부모에 연결한다. 생성만으로 overflow 표시 상태를 계산하지 않으므로 배치 후 `refresh()`를 호출한다.

## Host

```java
public interface Host
int extent();
int range();
int offset();
int lineStep();
void scrollTo(int offset);
void scrollBy(int delta);
public void stopFling();
```

모든 값·인수는 같은 SDK 논리 픽셀 좌표계를 사용한다.

| API | 계약 |
| --- | --- |
| extent | 보이는 본문 높이. |
| range | 전체 본문 높이. |
| offset | 현재 Y offset. |
| lineStep | 한 줄 이동량. |
| scrollTo | 절대 offset 적용. |
| scrollBy | 현재 offset에 delta 적용. |
| stopFling | 본문의 진행 중인 fling 중단. |

소유자는 일관된 높이·offset·양수 lineStep을 제공한다. 이 컨트롤은 값을 저장하는 본문이 아니며 callback 결과를 다음 offset 조회에 반영해야 한다. Host 호출은 UI 스레드에서 동기 실행하며 예외를 잡지 않는다. DOWN·키보드 제어는 이동 전에 Host.stopFling을 호출한다.

## refresh

```java
public void refresh();
```

`extent > 0 && range > extent`이면 VISIBLE, 그 외 GONE으로 갱신하고 다시 그린다. GONE으로 바뀌면 `stop()`도 호출한다. 부모의 gutter·너비·본문 배치는 바꾸지 않는다. 이 배치는 [WmScrollView](/ko/api/wmscrollview/) 또는 소유자가 관리한다.

## hitThumb

```java
public boolean hitThumb(float x,float y);
```

bar 로컬 `x,y`가 확장 thumb 영역에 들어오는지 반환한다. enabled이고 자신의 visibility가 VISIBLE이어야 한다. `x` 범위는 `[-24,width)`다. `y`는 위아래 화살표 사이이면서 thumb의 위·아래 `24` 확장 범위다.

확장 영역을 bar 밖에서도 사용하려면 부모가 DOWN을 검사하고 이벤트 좌표를 변환해 전달해야 한다. 보이는 그림 너비는 넓히지 않는다.

## onDraw

```java
protected void onDraw(Canvas canvas);
```

트랙은 흰색, 프레임은 검은 `2` 픽셀, 컨트롤 면은 face 색이다. 눌린 화살표 면은 검정, 화살표는 기본 검정·눌림 흰색·비활성 SHADOW다.

| bar 높이 | 그리기 |
| --- | --- |
| `50` 미만 | 아무것도 그리지 않음. |
| `50..79` | 절반 높이의 겹친 화살표; 일반 thumb 면 없음. |
| `80` 이상 | 화살표 높이 `40`, 트랙·thumb·grip. |

논리 thumb 높이는 `min(trackHeight,max(26,round(trackHeight*extent/range)))`이며 range가 `0`이면 trackHeight를 사용한다. 비활성 상태에서는 thumb 면·grip을 생략한다. 그려진 thumb 프레임은 논리 thumb 영역보다 위아래 `2`씩 확장한다. 높이 `50` 미만의 paint 생략이 입력 처리를 비활성화하지는 않는다.

## pageStep

```java
protected int pageStep();
```

기본 반환값은 `max(host.lineStep(),extent−host.lineStep())`이다. 트랙 tap·Page Up/Down·접근성 page action에 사용한다. 본문별 page 이동 단위를 제공하려면 override한다.

## onTouchEvent

```java
public boolean onTouchEvent(MotionEvent event);
```

비활성 상태에서는 `stop()` 후 false다. enabled DOWN은 기존 bar 동작을 취소하고 첫 포인터·현재 offset을 저장하며 Host fling을 멈춘다. 부모의 intercept를 막는다. 화살표는 한 line 즉시 이동하고 실제 offset이 바뀌면 `100ms` 뒤부터 `100ms`마다 반복한다. 범위 끝에서는 반복을 중단한다.

thumb DOWN은 MOVE에서 상대 거리만큼 절대 offset을 계산한다. 트랙 DOWN은 바로 page 이동하지 않는다. UP 시 현재 thumb 위/아래 방향으로 pageStep을 이동한다. 트랙에서 세로 touchSlop을 넘기면 thumb drag로 전환하고 slop만큼 거리를 제외한다. 가로 slop만 넘기면 취소한다.

드래그는 시작 offset에 `round(deltaY*max(0,range−extent)/thumbTravel)`을 더하고 `0..max(0,range−extent)`로 제한한다. smooth scroll·행 snapping은 사용하지 않는다. thumbTravel이 양수가 아니면 이동하지 않는다.

화살표는 해당 버튼 영역 이탈 또는 좌우 `24` 확장 영역 이탈로 취소한다. thumb drag는 가로 이탈만으로 취소하지 않는다. 추적 포인터가 올라가면 다른 포인터로 drag를 이어가며 drag 이외 동작은 취소한다. 포인터를 찾지 못하거나 CANCEL이면 취소한다. UP은 stop 후 performClick을 호출하고 true다.

## stop · setEnabled · onDetachedFromWindow

```java
public void stop();
public void setEnabled(boolean enabled);
protected void onDetachedFromWindow();
```

`stop()`은 반복 callback·눌림·추적 포인터를 지우고 부모 intercept 금지를 해제한 뒤 다시 그린다. 본문 offset을 바꾸거나 Host.stopFling을 호출하지 않는다.

`setEnabled(false)`와 detach는 stop한다. 자신·조상·window 숨김도 아래 hook에서 stop한다. 다시 표시해도 취소한 반복·drag를 재개하지 않는다. 숨김 전 gesture의 남은 UP은 page 이동을 만들지 않지만 기존 UP 경로의 performClick은 실행할 수 있다.

## onVisibilityChanged · onWindowVisibilityChanged

```java
protected void onVisibilityChanged(View changedView,int visibility);
protected void onWindowVisibilityChanged(int visibility);
```

각 superclass를 호출한 뒤 `visibility != VISIBLE`이면 `stop()`한다. 자신 또는 조상의 INVISIBLE·GONE 변경과 window의 non-VISIBLE 변경은 예약 반복·눌림·포인터를 취소한다. VISIBLE 변경은 이전 gesture를 복원하지 않는다.

## onKeyDown · onKeyUp

```java
public boolean onKeyDown(int key,KeyEvent event);
public boolean onKeyUp(int key,KeyEvent event);
```

enabled이고 modifier 없는 지원 키만 처리한다. 그 외에는 superclass에 전달한다. DOWN은 Host fling을 멈춘 뒤 아래처럼 이동하고 범위 끝이어도 true다.

| 키 | 이동 |
| --- | --- |
| Home / End | `0` / `max(0,range−extent)` |
| Page Up / Down | `−pageStep` / `+pageStep` |
| Up·Left / Down·Right | `−lineStep` / `+lineStep` |

지원 키의 modifier 없는 enabled UP은 소비한다. line/page 이동은 long 중간 합으로 offset을 범위에 제한한 뒤 Host.scrollBy를 호출한다. 이미 목표 offset이면 호출하지 않는다.

## performClick

```java
public boolean performClick();
```

superclass 클릭 callback을 실행하고 항상 true를 반환한다. 스크롤 자체는 이 메서드에서 하지 않는다. 직접 호출의 enabled 여부는 별도로 검사하지 않는다.

## 접근성

```java
public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info);
public boolean performAccessibilityAction(int action,Bundle args);
```

노드에 scrollable과 앞/뒤 page action을 등록한다. 비활성 action은 false다. 앞/뒤 스크롤 요청은 pageStep으로 이동하고 범위 끝이어도 true다. 이 경로는 Host.stopFling을 호출하지 않는다. 그 외 action은 API16 이상에서 superclass, 이전 버전에서는 false다.

## onWmRestyle

```java
public void onWmRestyle(android.graphics.Typeface regular,android.graphics.Typeface bold,int selection,int tabColor);
```

face 색을 `tabColor`로 바꾸고 다시 그린다. `regular`·`bold`·`selection`과 Host geometry는 바꾸지 않는다.

![Android API 14 렌더](/WmScrollBar.png)
