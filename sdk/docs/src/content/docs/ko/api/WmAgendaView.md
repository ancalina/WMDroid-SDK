---
title: "WmAgendaView"
slug: ko/api/wmagendaview
---
[API 색인](/ko/api/)

```java
public final class WmAgendaView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

하루 약속을 종일 목록, 읽기 전용 시각 strip, 시간순 두 줄 목록으로 표시한다. 호출자가 표시 날짜로 자른 분 값과 약속 문구를 제공하며 데이터 조회와 카드 열기도 담당한다. 원본의 선택적 날짜 footer는 미구현이다. 원본 GDI 지표와 Android 래스터의 픽셀 일치는 미검증이다. View 상태는 UI 스레드에서 변경하며 터치 callback은 동기 실행한다.

## 사용 준비

### 생성자

```java
public WmAgendaView(Context context,Typeface font,Locale locale);
```

font/locale는 null 불가(`IllegalArgumentException`). 현재 날짜로 시작하고 시스템 12/24시간 설정을 읽는다. 목록 정렬 Locale은 이후 호출자가 관리한다.

### setOnEventListener

```java
public void setOnEventListener(OnEventListener listener);
```

완성된 행 탭의 callback을 설정한다. null로 해제한다. 터치, callback 순서는 [OnEventListener](#oneventlistener)를 따른다. 프로그램 선택은 통지하지 않는다.

### setDisplayIconMask

```java
public void setDisplayIconMask(int mask);
```

시간 행 심볼 mask(0..63)를 교체하고 진행 중 행 터치를 취소하며 시간 행을 다시 그린다. 범위 밖이면 변경 전 `IllegalArgumentException`. Event metadata는 유지하며 충돌 표시는 이 mask와 별개이다.

### getDisplayIconMask

```java
public int getDisplayIconMask();
```

현재 심볼 표시 mask를 반환한다. 기본값은 ALL(63).

## 일정, 날짜

### setEvents

```java
public void setEvents(List<Event> values);
```

모든 항목을 검증한 뒤 non-null 목록을 복사한다. null 목록/항목은 기존 기록 교체 전 `IllegalArgumentException`. Event/tag 객체는 유지한다. 시간 일정은 원래 시작 epoch가 있으면 그 값, 없으면 표시 날짜 자정+startMinute*60000으로 정렬한다. 시작이 같으면 현재 Locale Collator로 title을 비교하며 종일 순서는 유지한다.

선택과 행 터치를 지우고 두 스크롤 위치를 0으로 요청하며 strip phase를 1로 초기화한다. 충돌과 경과 갱신도 다시 계산한다. listener를 통지하거나 strip offset을 즉시 이동하지 않는다. 동일 Event의 중복 등록은 허용하며 선택 identity를 공유하고 `rowBounds`는 첫 항목을 반환한다. strip에는 약속 터치 대상이 없으며 종일 상단 띠는 마지막 종일 항목의 busyColor를 사용한다.

### selectEvent

```java
public boolean selectEvent(Event event);
```

마지막 `setEvents`에 등록된 Event 인스턴스를 선택한다. null은 선택을 지우고 true, 미등록 인스턴스는 상태 변경 없이 false를 반환한다. 성공하면 행 터치를 취소하고 callback 없이 다시 그린다. null이 아닌 선택은 해당 목록에 초점을 요청하고 행이 보이도록 스크롤한다. 행이 viewport보다 크면 행 상단에 맞추며 실제 위치는 viewport 범위로 제한된다. 시간 일정은 strip phase도 선택하며 부착되고 strip 너비가 양수일 때만 pan을 시작한다. 선택 해제는 기존 strip pan을 중지하지 않는다.

내부 행 목록에 초점이 있고 목록과 agenda가 모두 enabled이면 UP/DOWN으로 그 목록 안에서 선택을 이동한다. 마지막 종일 행에서 DOWN은 첫 시간 행으로, 첫 시간 행에서 UP은 마지막 종일 행으로 이동한다. 초점 목록 안에 선택된 Event가 없으면 두 키 모두 그 목록의 첫 행을 선택한다. 빈 목록은 선택 변경 없이 키를 소비한다.

새 누름(`KeyEvent.getRepeatCount()==0`)은 전체 마지막 행에서 첫 행으로, 첫 행에서 마지막 행으로 순환한다. 한 partition만 있으면 행이 둘 이상일 때 그 안에서 순환한다. 반복 key-down은 내부 이동과 중간 경계 이동을 허용하지만 전체 양끝 순환은 억제한다. 이는 SDK의 Android repeat 정책이며 원본 raw `0x4000` flag와의 대응은 미검증이다. enabled 상태의 UP/DOWN key-up은 추가 이동 없이 소비한다. 키 선택은 `selectEvent`를 사용하며 listener callback을 호출하거나 약속을 열지 않는다.

### setDate

```java
public void setDate(int year,int month,int date);
```

1 기반 month의 실제 Gregorian 날짜를 상태 변경 전 검증한다. 잘못된 날짜는 `IllegalArgumentException`. 유효한 호출은 항상 행 터치를 취소하고 경과 갱신을 재예약한다. 날짜가 달라지면 기록, 충돌, 선택을 지우고 두 스크롤 위치를 0으로 요청하며 strip phase를 1로 초기화한다. 이후 `setEvents`를 호출한다. 같은 날짜는 해당 상태를 유지한다. 두 경로 모두 listener는 통지하지 않는다.

### date

```java
public int[] date();
```

새 `[year,month,day]` 배열. month는 1 기반.

## 언어, 표시

### setLocale

```java
public void setLocale(Locale locale);
```

null은 변경 전 `IllegalArgumentException`. catalog 문구를 갱신하고 새 Locale로 시간 목록을 다시 정렬하며 충돌을 재계산한다. 행 터치를 취소하고 선택된 Event를 유지한다. 두 목록의 요청된 분수 행 위치를 content 픽셀로 반올림하여 복원한다. 스크롤 위치에 있던 행 identity는 보존하지 않으며 Event 문구도 바꾸지 않는다.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

공유 catalog 문구만 갱신한다. 명시적 Locale, 호출자 일정 원문, 선택, 스크롤은 유지한다.

### setShowBusyInDescription

```java
public void setShowBusyInDescription(boolean enabled);
```

기본 false는 시간 제목의 BUSY 접두어를 생략하며 true는 붙인다. FREE/TENTATIVE/OUT_OF_OFFICE 접두어는 항상 붙인다. 원래 시작 epoch가 있는 시간 일정에만 적용한다. 조합한 접두어/제목을 surrogate pair를 나누지 않고 최대 259 UTF-16 단위로 자른 뒤 행 너비에 맞춰 줄인다. legacy 기록과 종일 제목은 유지한다.

### set24Hour

```java
public void set24Hour(boolean enabled);
```

strip 숫자를 바꾸고 행 터치를 취소한다. 24시간 모드의 자정은 24, 12시간 모드는 12로 표시한다. Event.time은 호출자가 만든 문구를 유지한다.

### setColors

```java
public void setColors(int selection);
```

ARGB 선택색을 설정하고 `WmPalette.face(selection)`으로 face를 계산하며 두 스크롤 트리를 restyle한다. strip의 busyColor=0 기록은 새 선택색을 따르며 0 아닌 색은 유지한다. 행 터치를 취소하되 선택을 지우거나 listener를 통지하지 않는다.

## 행, strip 좌표

### getRowHeight

```java
public int getRowHeight();
```

`2*(line+WmMetrics.EDGE)+WmMetrics.EDGE` 논리 픽셀이다. line은 현재 regular 글꼴을 `WmMetrics.FONT` 크기로 측정한 `ceil(descent-ascent)+WmMetrics.BORDER`이며 시간 행 두 줄의 간격을 계산한다.

### getAllDayRowHeight

```java
public int getAllDayRowHeight();
```

[getRowHeight](#getrowheight)와 같은 line 지표의 `line+2*WmMetrics.EDGE` 논리 픽셀이다.

### getAllDayViewportHeight

```java
public int getAllDayViewportHeight();
```

종일 기록이 없으면 0; 있으면 최대 6행 높이+2 논리 픽셀. 목록은 그 안에서 스크롤한다.

### getStripHeight

```java
public int getStripHeight();
```

스트립 고정 높이 38 논리 픽셀.

### getStripBackingWidth

```java
public int getStripBackingWidth();
```

현재 스트립 너비*2.4를 내림한 전체 배경 너비. layout 전에는 0일 수 있다.

### getStripOffset

```java
public int getStripOffset();
```

현재 offset을 논리 픽셀로 반환하며 그릴 때는 음수 방향으로 이동한다. 일정 선택은 offset을 즉시 설정하는 대신 목표까지 pan할 수 있다.

### setStripPhase

```java
public void setStripPhase(int phase);
```

phase는 0/1/2만 허용하며 그 외는 상태 변경 전 `IllegalArgumentException`. 행 터치를 취소하고 pan을 중지한 뒤 아래 offset(논리 픽셀)을 즉시 적용한다.

| phase | offset |
| --- | --- |
| 0 | `0` |
| 1 | `8 * step` |
| 2 | `14 * step` |

`step = floor(backingWidth / 24.5)`, `backingWidth = floor(현재 strip 너비 * 2.4)`. strip 너비가 0이면 다음 양수 너비 layout에서 적용한다. 기록을 선택하거나 listener를 통지하지 않는다.

시간 행 DOWN, 완성 탭, `selectEvent`는 시작/기간과 이전 phase로 phase를 선택한다. 이 pan은 1 ms 간격 callback을 요청하며 남은 거리가 4픽셀보다 크면 정수 distance/4만큼 이동한다. 4픽셀 이내에서는 이전 이동량을 남은 거리로 제한하므로 이전 이동량이 0이면 목표 전에 멈출 수 있다. callback 시점은 Android 스케줄링이며 실기기 WM의 측정 시점은 아니다.

### rowBounds

```java
public Rect rowBounds(Event event);
```

해당 Event의 종일/시간 목록 안에서 스크롤 전 새 Rect를 반환한다. 등록된 인스턴스 identity로 찾으며 null/미등록은 null. 현재 행 간격과 layout된 목록 너비를 사용하므로 layout 전 너비는 0일 수 있다. 중복 인스턴스는 첫 행을 반환한다.

### busyBounds

```java
public Rect busyBounds(Event event);
```

phase 이동 전 전체 strip 배경의 새 Rect이다. null/미등록은 null. 종일은 `(0,0,backingWidth,4)`, 시간 일정은 y=4..32와 분 비율의 x좌표를 사용한다. 시작/끝이 같아도 너비는 최소 1픽셀이다. 유효한 가로 geometry는 strip layout 후 조회한다. 그리기 영역이며 strip 터치로 기록을 선택하지 않는다.

### scrollToRow

```java
public void scrollToRow(int index);
```

정렬된 시간 목록의 index*rowHeight 위치를 요청하며 실제 끝 위치는 viewport로 제한된다. index는 0 기반이며 빈 목록은 0만 허용한다. 범위 밖이면 요청 전 `IllegalArgumentException`. 선택, 초점, 통지는 하지 않는다. [WmScrollView](/ko/api/wmscrollview/#scrolltoposition)는 layout까지 요청을 지연할 수 있다.

### hasConflict

```java
public boolean hasConflict(Event event);
```

Event identity의 캐시된 충돌 여부를 반환한다. null, 미등록, 종일, FREE, 원래 시작 epoch 없는 legacy 기록은 false. 후보는 자르기 전 원래 epoch 구간을 사용한다. FREE 아닌 종일 기록이 있으면 모든 시간 후보가 충돌하며 그 외는 시작/끝을 엄격 비교한다. 끝/시작이 맞닿기만 하면 충돌하지 않으며 기간 0인 후보를 별도로 제외하지 않는다. 중복 참조는 같은 Event의 다른 항목을 건너뛰는 identity 기반 검사로 계산한다.

목록 분리/정렬 시 재계산하며 시간 경과로 결과를 바꾸지 않는다. 경과 기록도 결과는 유지하되 행 충돌 아이콘/빨간 시간 문구를 숨긴다. 선택된 strip 괄호는 여전히 충돌 빨간색을 사용할 수 있다.

## 스타일, 수명

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

regular null은 변경 전 `IllegalArgumentException`. 목록에는 `WmMetrics.FONT`, strip에는 19 논리 픽셀 크기의 regular를 사용하며 bold/tabColor는 반영하지 않는다. 행 터치를 취소하고 geometry와 선택색 기반 face를 갱신하며 기록, 선택은 유지한다. 두 목록의 요청된 분수 행 위치를 content 픽셀로 반올림하여 복원한다. 원본 GDI 지표와 strip face는 미검증이다.

### onAttachedToWindow

```java
protected void onAttachedToWindow();
```

superclass 부착 후 경과 갱신을 설정한다. 부착, window VISIBLE, isShown이며 epoch 종료를 가진 시간 기록이 있을 때만 작동한다. 모두 경과했어도 TIME_CHANGED/TIMEZONE_CHANGED receiver를 등록하며 API 33+는 RECEIVER_NOT_EXPORTED를 사용한다. 아직 경과하지 않은 다음 종료에 end-now+1 ms로 예약하되 한 번의 지연은 최대 Integer.MAX_VALUE+1 ms로 제한한다. timer는 시간 행을 무효화하고 재예약하며 종료와 같은 시각은 경과가 아니다. 정적 boolean past 기록을 시각에 맞게 바꾸려면 소유자가 기록을 교체해야 한다.

### onWindowVisibilityChanged

```java
protected void onWindowVisibilityChanged(int visibility);
```

superclass 처리 후 [경과 갱신 조건](#onattachedtowindow)을 다시 평가한다. 숨김은 해당 timer와 시각 receiver를 해제하며 strip pan이나 행 터치는 중지하지 않는다.

### onVisibilityChanged

```java
protected void onVisibilityChanged(View changed,int visibility);
```

superclass 처리 후 자신/상위 View의 표시 변경에 같은 [경과 갱신 조건](#onattachedtowindow)을 다시 평가한다. 숨김은 해당 timer와 시각 receiver를 해제하며 strip pan이나 행 터치는 중지하지 않는다.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

superclass detach 전에 경과/strip callback, 시각 receiver, 행 터치를 해제한다. 기록, 선택, 현재 offset은 유지한다. 재부착은 표시 조건에 맞춰 경과 갱신을 다시 시작하며 중지된 strip pan은 재시작하지 않는다.

## Event

```java
public static final class Event
```

### 상수와 필드

```java
public static final int FREE = 0;
public static final int TENTATIVE = 1;
public static final int BUSY = 2;
public static final int OUT_OF_OFFICE = 3;
public final int busyStatus;
public final int startMinute;
public final int endMinute;
public final int busyColor;
public final boolean allDay;
public final String title;
public final String time;
public final String location;
public final Object tag;
public final int symbolMask;
public final boolean recurrenceException;
public final boolean past;
```

`startMinute/endMinute`는 표시 civil 날짜로 자른 분이며 종료는 exclusive이고 같은 시작/끝도 허용한다. `allDay`는 종일 목록을 선택한다. `title/time/location`은 호출자 원문을 유지하며 시간 행은 제목 다음에 시간과 비어 있지 않은 괄호 위치를 너비에 맞춰 줄여 표시한다. `busyColor=0`은 선택색을 따르고 그 외 값은 strip에 직접 사용한다. `tag`는 null 가능한 호출자 객체를 유지한다.

`symbolMask`는 [WmCalendarSymbols](/ko/api/wmcalendarsymbols/) 비트이며 표시 mask로 제한한다. recurrenceException은 반복 비트가 보일 때 예외 심볼을 선택한다. 심볼은 시간 행에만 표시한다. `past`는 snapshot 또는 호출자가 준 flag이며 실시간 속성이 아니다. 그리기는 [isPast](#ispast)를 사용한다. 선택되지 않은 경과 시간 문구는 회색과 경과 심볼을 사용하며 선택/눌림 문구는 흰색이다. 종일 문구는 past로 회색 처리하지 않는다. busyStatus는 FREE=0/TENTATIVE=1/BUSY=2/OUT_OF_OFFICE=3.

### 생성자

```java
public Event(int startMinute,int endMinute,boolean allDay,String title,String time,String location,int busyColor,Object tag);
public Event(int startMinute,int endMinute,boolean allDay,String title,String time,String location,int busyColor,Object tag,int symbolMask,boolean recurrenceException,boolean past);
public Event(int startMinute,int endMinute,boolean allDay,String title,String time,String location,int busyColor,Object tag,int symbolMask,boolean recurrenceException,long endTimeMillis);
public Event(int startMinute,int endMinute,boolean allDay,String title,String time,String location,int busyColor,Object tag,int symbolMask,boolean recurrenceException,long startTimeMillis,long endTimeMillis,int busyStatus);
```

분 범위는 0<=start<1440, start<=end<=1440. title/time/location은 null 불가, mask는 0..63, busyStatus는 0..3이다. 원래 epoch 양 끝을 제공하면 end>=start를 요구한다. 위반은 `IllegalArgumentException`. epoch 값과 `isPast(now)`의 단위는 밀리초이다. 분 범위는 별도로 잘라 제공하며 컨트롤이 epoch에서 분을 계산하지 않는다.

| overload 끝 인수 | 경과 상태 | 기타 기본값/metadata |
|---|---|---|
| busyColor, tag | 정적 past=false | symbolMask=0, recurrenceException=false, FREE; 원래 구간 없음. |
| symbolMask, recurrenceException, boolean past | 전달한 정적 past | 심볼/예외 유지; FREE; 원래 구간 없음. |
| symbolMask, recurrenceException, long endTimeMillis | 실시간 `endTimeMillis < now`; past는 생성 시 snapshot | 심볼/예외 유지; FREE; 원래 시작/충돌 metadata 없음. |
| long startTimeMillis, long endTimeMillis, int busyStatus | 실시간 `endTimeMillis < now`; past는 생성 시 snapshot | 원래 구간으로 정렬/충돌 계산; 전달한 busyStatus 유지. |

### isPast

```java
public boolean isPast(long now);
```

epoch 종료가 있으면 `endTimeMillis < now`를 반환하며 같은 시각은 경과가 아니다. 없으면 전달받거나 저장한 past를 반환한다. now는 epoch 밀리초이다. 공개 past snapshot을 바꾸거나 redraw를 요청하지 않는다.

## OnEventListener

```java
public interface OnEventListener
```

### onSelected

```java
void onSelected(Event event);
```

같은 행의 완성된 UP이 touch slop 이내면 등록된 Event를 전달한다. DOWN은 눌림 표시, 목록 초점, 시간 strip pan을 시작할 수 있지만 선택을 확정하지 않는다. slop 초과 이동, 행 이탈, CANCEL, 다중 터치, 크기 변경, detach는 탭을 취소한다. DOWN이 취소돼도 변경된 strip phase/pan은 남을 수 있다. callback 전에 선택을 확정하며 프로그램 선택은 통지하지 않는다.

### onOpen

```java
void onOpen(Event event);
```

onSelected 직후 같은 Event를 동일하게 저장된 listener로 전달한다. onSelected에서 listener나 기록을 변경/해제해도 이 호출은 다른 대상에 전달되거나 생략되지 않는다. onSelected 예외는 전파되며 onOpen은 실행하지 않는다. 소유자가 카드를 연다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmAgendaView.java`
