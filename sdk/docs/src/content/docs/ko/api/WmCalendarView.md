---
title: "WmCalendarView"
slug: ko/api/wmcalendarview
---
[API 색인](/ko/api/)

```java
public final class WmCalendarView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

일/5일/6일/주 시간표를 표시하며 다중 열에서는 선택 상세도 표시한다. 호출자는 각 civil 날짜에 맞춰 자른 일정을 제공하고 callback에서 약속 카드를 연다. Provider 조회·드래그 편집은 미구현이다. 논리 VGA 좌표를 사용하며 GDI 글꼴 지표·범주 색·overflow 재현은 부분적이다. View 상태는 UI 스레드에서 변경하며 callback은 터치/키 dispatch에서 동기 실행한다.

## 사용 준비

### 상수와 필드

```java
public static final int DAY = 1;
public static final int WEEK = 7;
```

DAY=1, WEEK=7은 날짜 열 수이다.

### 생성자

```java
public WmCalendarView(Context context,Typeface font,Locale locale);
```

font/locale는 null 불가(`IllegalArgumentException`). 초기 모드는 DAY, 간격은 60분, 12/24시간은 시스템 설정이다. 작업 시간 기본은 월~금 08:00..17:00.

### setRange

```java
public void setRange(int year,int month,int day,int days);
```

실제 시작 날짜와 연속된 열 수 1/5/6/7을 지정한다. 5/6열도 주말을 건너뛰지 않는다. month는 1 기반이며 잘못된 날짜/열 수는 범위 변경 전 `IllegalArgumentException`. Event·약속/시간 선택·키보드 anchor·터치를 지우고 두 스크롤 위치를 0으로 요청한다. 이후 `setEvents`를 호출한다. listener는 통지하지 않는다.

## 일정·시각 선택

### setEvents

```java
public void setEvents(List<Event> values);
```

검증 후 목록을 복사하며 Event/tag 객체와 입력 순서를 유지한다. null 목록/항목 또는 현재 범위 밖 Event.day는 기존 목록 교체 전 `IllegalArgumentException`. 약속/시간 선택·키보드 anchor·터치를 지우며 callback은 호출하지 않는다. geometry를 다시 계산하되 요청된 스크롤 위치는 초기화하지 않는다.

DAY에서는 종일 일정이 별도 스크롤 헤더 목록을 이룬다. 다중 열에서는 개별 종일 행 대신 날짜마다 전체 높이의 busy 띠 하나를 표시한다. 집계 우선순위는 BUSY > OUT_OF_OFFICE > TENTATIVE > FREE. 이 집계 띠는 개별 종일 일정의 터치 대상을 제공하지 않는다.

### selectEvent

```java
public boolean selectEvent(Event event);
```

마지막 setEvents 호출에서 등록한 동일 Event 인스턴스만 선택한다. null은 약속·시간 선택을 지우고 true를 반환한다. 등록되지 않은 인스턴스는 상태를 바꾸지 않고 false를 반환한다. 성공하면 진행 중 터치 선택과 시간 선택의 키보드 anchor를 지우고 다시 그리며 약속·시간 callback은 호출하지 않는다. null이 아닌 선택은 터치 모드에서도 초점을 요청하고 시작 분/현재 interval로 키보드 행을 설정한다. 시간 일정은 해당 시작 행이 보이도록 스크롤한다. DAY의 종일 일정은 해당 종일 행이 보이도록 header 목록을 스크롤한다. 여러 열에서는 선택 열의 왼쪽에 폭10논리 픽셀 frame을 실제 스크롤 위치·viewport 높이로 그린다. 정수 나눗셈으로 top=scroll.getScrollPosition(), height=scroll.getHeight(), rows=height/rowHeight를 계산한다. top/rowHeight+rows>=24이면 frame 높이는 rows*rowHeight, 아니면 height이다. 두 행 간격 모두 같은 상수24 기준을 사용한다. 스크롤·layout 변경 시 viewport frame을 다시 그리며 개별 종일 touch target은 추가하지 않는다.

### setOnEventListener

```java
public void setOnEventListener(OnEventListener listener);
```

약속 선택/열기 callback을 동기 호출한다. null로 해제한다. 터치·키보드 발생 조건은 [OnEventListener](#oneventlistener)를 따른다. 프로그램 선택은 통지하지 않는다.

### setOnTimeListener

```java
public void setOnTimeListener(OnTimeListener listener);
```

빈 시간칸의 완성 탭과 키보드 시간 선택을 동기 통지한다. null로 해제한다. `setTimeSelection`, `selectEvent`, 범위/목록 초기화는 통지하지 않는다.

### getTimeSelection

```java
public int[] getTimeSelection();
```

새 `[day,startMinute,endMinute]` 배열. 약속이 선택됐거나 선택이 없으면 null.

### setTimeSelection

```java
public void setTimeSelection(int day,int startMinute,int endMinute);
```

현재 열 범위, 0<=start<end<=1440, 현재 간격에 정렬된 분을 요구한다. 잘못된 선택은 상태 변경 전 `IllegalArgumentException`. 약속 선택과 터치를 지우고 첫 선택 행을 키보드 anchor, 마지막 행을 cursor로 설정한다. 초점·스크롤을 요청하거나 listener를 통지하지 않는다.

## 주 탐색·커서 상태

### setOnNavigateListener

```java
public void setOnNavigateListener(OnNavigateListener listener);
```

날짜 이동 callback을 설정한다. null로 해제한다. DAY에서 시간 일정이 선택됐으면 그 일정의 text/busy 영역을 요청 방향으로 10 논리 픽셀 확장하고 정렬된 배치 순서에서 교차하는 첫 시간 일정을 선택한다. 해당 일정이 없으면 날짜를 바꾸지 않고 `onNavigate(-1/+1)`을 호출한다. tag로 이웃 관계를 판단하지 않는다. 다중 열도 범위를 소진하면 같은 listener를 호출한다. 범위 교체 전에 WeekNavigation을 캡처하고, 소유자가 첫 날짜를 direction*7일 이동·조회한 뒤 snapshot을 복원한다.

### selectInitialWeek

```java
public void selectInitialWeek(int preferredColumn, long nowMillis);
```

주 보기 최초 열기·보기 활성화의 자료 로딩 후 호출한다. `preferredColumn` 허용 범위: 0..getDayCount()-1. DAY는 IllegalStateException, 잘못된 열은 선택 변경 전에 IllegalArgumentException을 발생시킨다. `nowMillis`는 epoch 시각이며, 뷰 생성 시 저장한 시간대로 변환한다.

preferredColumn부터 뒤쪽의 첫 비어 있지 않은 열을 찾는다. 없으면 앞쪽의 가장 가까운 열을 찾는다. 찾은 열부터 뒤쪽으로 검사한다. 각 열에서 시간 일정을 우선하고, 없으면 첫 종일 일정을 선택한다. preferredColumn에서는 현재 시각 이후의 첫 일정을 선택한다. 모두 지났으면 마지막 시간 일정을 선택한다. 다른 열에서는 첫 시간 일정을 선택한다. 시작 시각은 분 정밀도이고 현재 시각은 초·밀리초를 유지한다. 같은 분이라도 현재 초·밀리초가 0이 아니면 지난 일정이다. UTC epoch 순서 대신 로컬 날짜·시각 순서를 사용한다.

일정 선택은 원시 커서 행을 유지하고 onSelected를 동기로 한 번 호출한다. 일정이 없으면 preferredColumn에 원시 커서 행을 복원한다. 현재 간격으로 행을 제한하고 끝 행을 화면에 표시한 뒤 시간 콜백을 한 번 호출한다. onOpen은 호출하지 않는다. 소유 화면은 명시적으로 요청된 일정을 우선해야 한다. 일반 새로고침·경계 이동에는 이 API를 호출하지 않는다.

### getSelectedEvent / getWeekRemovalSuccessor

```java
public Event getSelectedEvent();
public Event getWeekRemovalSuccessor();
```

getSelectedEvent는 현재 선택한 입력 기록 객체를 반환한다. 시간 커서·선택 없음은 null. setEvents는 선택을 지운다.

주 보기에서 선택한 기록을 삭제하기 전에 getWeekRemovalSuccessor를 호출한다. 같은 열의 다음 기록, 없으면 이전 기록, 둘 다 없으면 null 반환. 시간 커서·선택 없음은 null. DAY는 IllegalStateException 발생. 열 안의 순서: 종일 먼저, 시작 시각 오름차순, 바쁨 상태 내림차순, 동률은 입력 순서 유지. 선택·스크롤·콜백은 변경하지 않는다.

반환한 기록의 식별자를 재조회까지 보관한다. 새 Event와 매칭하여 selectEvent로 우선 선택한 뒤 자동 재조회 선택을 적용한다. 반환 객체는 이전 기록 집합에 속한다. 새 setEvents 목록에 자동으로 포함되지 않는다. SDK는 제공자 데이터를 삭제하지 않는다.

### selectPopulatedWeek

```java
public void selectPopulatedWeek(int preferredColumn);
```

setEvents로 새 다중 열 기록을 설정한 뒤 명시 대상이 없을 때 적용하는 자동 선택입니다. 특정 기록은 새 Event 인스턴스에 매칭하여 selectEvent로 먼저 선택합니다. selectInitialWeek와 같은 점유 열 탐색·시간 우선/종일 대체 규칙이지만 현재 시각 없이 첫 시간 일정을 고릅니다. 일정이 없으면 저장된 시간 행을 복원하며 일정 또는 시간 선택 callback이 한 번 발생합니다. preferredColumn은0..getDayCount()-1; DAY는 IllegalStateException, 범위 밖 열은 변경 전 IllegalArgumentException입니다.

| 호출 상황 | 순서 |
| --- | --- |
| 첫 표시·보기 활성화 | setEvents→selectInitialWeek(현재 시각 반영) |
| 기록 교체·명시 선택 없음 | setEvents→selectPopulatedWeek |
| 주간 경계 이동 | 기존 captureWeekNavigation→새 범위/기록→restoreWeekNavigation |

외형 갱신·기존 선택 유지 편집에는 자동 선택을 호출하지 않습니다. 삭제 전 getWeekRemovalSuccessor 식별자를 보관해 새 기록에 매칭하고 매칭이 없으면 이 정책을 적용할 수 있습니다. 데이터 조회·적용 시점은 호출자가 결정합니다.

### getTimeCursorRows / setTimeCursorRows

```java
public int[] getTimeCursorRows();
public void setTimeCursorRows(int startRow, int endRow);
```

`getTimeCursorRows`는 마지막 시간 커서의 시작·끝 행을 새 배열로 반환한다. 양 끝을 포함한다. 일정 선택은 이 값을 바꾸지 않는다. `setTimeCursorRows`는 이 상태만 복원한다. 화면 선택·스크롤·포커스를 바꾸거나 콜백을 호출하지 않는다. 허용 범위: 0 <= startRow <= endRow < 48. 잘못된 값은 상태 변경 전에 IllegalArgumentException을 발생시킨다.

활성 시간 선택 없이 간격을 바꾸면 원시 행 번호를 유지한다. captureWeekNavigation은 현재 행 수로 제한한다. 소유 화면을 닫을 때 반환값을 저장하고, 경계 상태를 캡처하기 전에 복원한다. 시각의 분 단위로 변환하지 않는다.

### captureWeekNavigation / restoreWeekNavigation

```java
public WeekNavigation captureWeekNavigation(int direction);
public void restoreWeekNavigation(WeekNavigation navigation);
public static final class WeekNavigation {
    public final int direction, startRow, endRow;
    public final boolean preferAllDay;
}
```

소유자가 범위·일정 목록을 교체하기 전에 캡처한다. direction은 -1 또는 1이며 그 외에는 IllegalArgumentException. 두 메서드 모두 5/6/7열에서만 사용한다. DAY는 IllegalStateException, null 복원 인수는 IllegalArgumentException. snapshot에는 public 생성자가 없다.

preferAllDay는 종일 일정 선택 여부다. startRow/endRow는 양 끝을 포함한 마지막 시간 cursor 행이며, 약속 선택 중에도 유지한다. 이전 cursor가 없으면 뷰 생성 시 로컬 시각의 hour를 두 행의 초기값으로 사용한다. 30분 모드에서도 분으로 변환하지 않는 raw 행 번호다. recordId나 이전 약속 시작 시각은 포함하지 않는다.

setRange/setEvents 후 복원한다. 오른쪽 이동은 첫 열부터, 왼쪽은 마지막 열부터 검색한다. 각 열에서 캡처한 일정 유형을 우선하고 없으면 다른 유형으로 대체한다. 시간 일정은 civil 정렬의 첫/마지막 배치, 종일은 양 방향 모두 첫 입력 record다. 시간 일정 선택은 화면에 표시하며, 약속 선택은 onSelected를 동기 1회 호출하며 onOpen은 호출하지 않는다. 일정이 없으면 경계 열에서 cursor 범위를 복원하고 시간 listener를 1회 호출한다. 새 interval의 행 수에 맞춰 clamp하며, interval 변경 시 분이 아닌 행 번호를 유지한다. 조회·날짜 이동·이전 record ID 비교·소유자 조회 실패 rollback은 수행하지 않는다.

## 날짜·표시·언어

### setOnDateListener

```java
public void setOnDateListener(OnDateListener listener);
```

5/6/7열의 날짜 헤더 callback을 설정한다. null로 해제한다. setter는 진행 중 헤더 터치를 취소한다. 한 포인터 탭이 원래 열 안에서 touch slop 이내로 끝나야 통지하며 다른 열로 이동하면 확정을 취소한다. 원본 DOWN 대신 UP에서 확정한다. 범위나 약속/시간 선택은 바꾸지 않는다.

### setDisplayIconMask

```java
public void setDisplayIconMask(int mask);
```

심볼 표시 mask(0..63)를 교체하고 터치를 취소한다. 범위 밖이면 IllegalArgumentException. 일정 metadata는 유지한다.

### getDisplayIconMask

```java
public int getDisplayIconMask();
```

현재 심볼 표시 mask를 반환한다. 기본값은 ALL(63).

### getDayCount

```java
public int getDayCount();
```

현재 범위의 1/5/6/7 열 수.

### setWorkingHours

```java
public void setWorkingHours(int dayMask,int startMinute,int endMinute);
```

dayMask 1..127(일요일 bit0), 0<=start<end<1440이며 30분 배수인 경계를 요구한다. end는 exclusive. 잘못된 값은 `IllegalArgumentException`. 작업 시간 배경을 갱신한다.

### dateHeaderBounds

```java
public RectF dateHeaderBounds(int column);
```

현재 View 좌표의 고정 헤더 RectF 사본을 반환한다. DAY는 null. 현재 layout된 시간표 grid 너비를 사용하며 시간 숫자 열은 제외한다. 유효한 가로 좌표가 필요하면 layout 후 조회한다. 범위 밖 열은 `IllegalArgumentException`.

### setInterval

```java
public void setInterval(int minutes);
```

30/60분 행만 허용한다(`IllegalArgumentException`). 현재 값이면 아무 동작도 하지 않는다. 변경 시 터치를 취소하고 요청된 civil 분 스크롤 위치를 보존한다. 기존 시간 선택은 시작을 새 간격으로 내림한 한 칸이 되며 키보드 anchor를 초기화한다. 약속 선택은 유지하고 listener는 통지하지 않는다.

### setLocale

```java
public void setLocale(Locale locale);
```

기본 빈 문구와 Locale을 교체한다. Event.title/detail은 호출자 데이터 그대로이다. null은 `IllegalArgumentException`.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

공유 catalog 문구만 갱신한다. 명시적 Locale, 호출자 일정 원문, 선택·스크롤은 유지한다.

### set24Hour

```java
public void set24Hour(boolean enabled);
```

시간축 숫자의 12/24시간 모드만 바꾼다.

### setShowWeekText

```java
public void setShowWeekText(boolean enabled);
```

기본 false면 5/6/7열에서 busy 블록만, true면 제목/심볼 박스를 표시한다. DAY는 항상 제목/심볼 박스를 표시한다. 배치 geometry를 다시 계산하고 터치를 취소한다. busy-only 영역은 실제 분 비율을 따르므로 옵션 변경으로 `eventBounds`와 터치 영역이 달라질 수 있다.

### setColors

```java
public void setColors(int selection,int face,int eventColor);
```

ARGB 선택색·face·약속 박스색을 직접 설정한다. 작업 시간과 busy 상태의 원본 고정색은 별도이다.

## 좌표·스크롤

### getRowHeight

```java
public int getRowHeight();
```

현재 글꼴을 `WmMetrics.FONT` 크기로 측정한 `max(ceil(descent-ascent),28)+6` 논리 픽셀이다. 30/60분 한 행의 높이이며 원본 GDI 측정값 대신 Android 글꼴 지표를 사용한다.

### getTimeColumnWidth

```java
public int getTimeColumnWidth();
```

현재 글꼴을 `WmMetrics.FONT` 크기로 측정한 `ceil(WmFonts.measure(paint,"12"))+6` 논리 픽셀이다.

### getHeaderHeight

```java
public int getHeaderHeight();
```

5/6/7열은 36 논리 픽셀. DAY는 `min(allDayCount,6) * listSpacing`이며 없으면 0이다. `listSpacing`은 `ceil(descent-ascent)+WmMetrics.BORDER+2*WmMetrics.EDGE`; 초과 종일 행은 헤더 스크롤로 접근한다.

### getContentHeight

```java
public int getContentHeight();
```

24시간 시간표의 전체 스크롤 content 높이(논리 픽셀), 헤더/상세 제외.

### dateAt

```java
public int[] dateAt(int column);
```

0 기반 열의 새 `[year,month,day]`, month는1 기반. 범위 밖이면 `IllegalArgumentException`.

### scrollToMinute

```java
public void scrollToMinute(int minute);
public void scrollToMinute(float minute);
```

civil 분 위치를0..1440으로 clamp하여 스크롤한다. float overload는 소수 분을 받으며 스크롤 요청은 가장 가까운 content 픽셀로 반올림한다. NaN/Infinity는 `IllegalArgumentException`. 실제 끝 위치는 viewport 범위로 제한된다.

### getScrollMinute

```java
public float getScrollMinute();
```

요청된 content 스크롤 위치를 civil 분으로 반환한다.

### eventBounds

```java
public RectF eventBounds(Event event);
```

시간 일정의 RectF 사본을 grid content 좌표로 반환하며 고정 헤더와 스크롤 offset은 제외한다. 객체 identity로 찾으며 null·미등록·종일 일정은 null. 현재 grid 너비로 배치하므로 layout 후 조회한다. 겹친 일정은 가로 lane을 나눈다. 반환값은 배치 사각형이며 아래의 터치 검사에 쓰는 busy 사각형과의 enclosing union은 아니다. 다중 열 busy-only 모드에서는 제목 박스 대신 busy 블록 영역을 반환한다.

## 스타일·입력·수명

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

regular를 새 글꼴로 사용하며 bold/tabColor는 반영하지 않는다. 선택색을 바꾸고 `WmPalette.face(selection)`으로 face를 계산하며 eventColor는 유지한다. 터치를 취소하고 geometry를 다시 계산한다. 요청된 civil 분 스크롤 위치와 DAY 종일 헤더의 분수 행 위치를 보존한다. listener는 통지하지 않는다.

### setEnabled

```java
public void setEnabled(boolean enabled);
```

grid·header·두 scroll view의 활성 상태를 함께 바꾸고 터치를 취소한다. 일정과 선택은 유지한다. 비활성 상태면 터치 dispatch와 아래에서 처리하는 행 이동 키를 거부하며 그 외 키는 Android로 전달한다.

### dispatchTouchEvent

```java
public boolean dispatchTouchEvent(MotionEvent event);
```

비활성 상태면 false를 반환하고 그 외에는 자식 터치 경로로 전달한다. 시간 일정의 터치 검사는 text/busy 사각형을 감싸는 영역을 사용하며 오른쪽/아래 경계는 제외한다. 너비나 높이가 0 이하인 busy 사각형은 터치 영역을 확장하지 않는다. 대상이 겹치면 해당 날짜의 첫 시간 배치를 선택한다. 모든 모드는 아래의 civil 시작/busy 정렬 순서를 따른다. 약속 DOWN은 동기 선택하며 같은 약속의 UP이 touch slop 이내면 연다. 빈 grid DOWN은 한 시간칸을 선택하지만 callback은 같은 열/행의 slop 이내 UP에서만 호출한다. slop 초과 이동·CANCEL·다중 포인터는 확정을 취소하되 DOWN에서 적용한 선택은 유지한다. 자식 detach/크기 변경은 해당 자식의 진행 중 터치를 취소한다.

### dispatchKeyEvent

```java
public boolean dispatchKeyEvent(KeyEvent event);
```

Alt/Ctrl 없는 다음 키를 처리한다. DAY 좌/우와 시간 행 키는 UP을 선택 변경 없이 소비하며 DOWN에 동작을 적용한다. 다중 열 좌/우는 약속을 선택하거나 소유자 날짜 이동 listener를 호출한 DOWN과 그 대응 UP을 소비한다. 비활성 상태에서는 이 키들을 거부하며 나머지 키/수정자는 superclass로 전달한다.

| 키 | 결과 |
|---|---|
| DAY 좌/우 | 교차하는 시간 일정 선택 또는 소유자의 날짜 이동 callback. Shift로 시간 선택을 확장하지 않는다. |
| 다중 열 좌/우 | 시간 일정은 같은 열의 text/busy 영역을 요청 방향으로 8 논리 픽셀 확장하고 교차하는 첫 배치 일정을 선택한다. 없으면 진행 방향의 다른 열을 검색한다. 약속 선택 callback은 DOWN에 한 번 발생하며 대응 UP을 소비한다. Shift로 시간 선택을 확장하지 않는다. 전체 범위에 후보가 없으면 소유자 날짜 이동 listener를 호출하며, listener가 없으면 superclass로 전달한다. |
| DAY 위/아래 | 시간 일정과 일정이 덮지 않은 시간 행을 시작 행 순서로 이동한다. 종일 일정은 제외하며 겹친 일정도 각각 선택 항목을 이루며 원본 civil 시작/busy 정렬 순서를 따른다. Shift로 선택을 확장하지 않는다. |
| 다중 열 위/아래 | 현재 열의 이전/다음 약속을 선택한다. 양 끝에서는 반대 끝으로 돌아간다. Shift로 시간 선택을 확장하지 않는다. |
| PageUp/PageDown | `max(1,viewportHeight/rowHeight)` 행 이동한다. |
| Home/End | 첫/마지막 시간 행으로 이동한다. |

#### 시간 행 선택

시간 행 이동은 0..1440/interval-1로 제한하며 현재 시간/약속 열을 사용한다. 선택이 없으면 0열이다. cursor가 보이도록 스크롤하고 시간 listener를 동기 통지한다. Shift는 anchor를 유지하여 양 끝 행을 포함한 범위를 선택하며 반환되는 끝 분은 exclusive이다. Shift가 없으면 한 행을 선택하며 anchor가 없으면 Shift도 이동한 행에서 범위를 시작한다.

#### DAY 보기의 위·아래

DAY 위/아래는 정렬된 시간 일정 배치와 빈 시간 행을 이동한다. 겹친 일정은 원본 civil 시작 오름차순, busyStatus 내림차순이며 같은 키는 입력 순서를 유지한다. 약속에 `onSelected`, 빈 행에 시간 callback을 호출한다. DAY 목록 경계에서는 선택을 유지하며 일부만 보이는 약속은 추가 한 행이 보이도록 스크롤할 수 있다.

#### 다중 열 보기의 위·아래

약속 선택 열, 시간 선택 열, 0열 순으로 사용한다. 종일 일정을 먼저 두고 원본 civil 시작 오름차순·busyStatus 내림차순으로 정렬한다. 같은 키는 입력 순서를 유지한다. 약속 선택이 없으면 원본 시작 시각의 시(hour)를 첫 선택 raw 행(`timeStart/interval`, 선택 없으면 0)과 비교한다. 아래는 더 큰 시, 위는 더 작은 시를 찾는다. 후보가 없으면 아래는 첫 약속, 위는 마지막 약속을 선택한다. 30분 간격에서도 시와 행을 그대로 비교한다.

선택마다 `onSelected`를 한 번 호출한다. 약속 하나만 있어도 돌아올 때 재통지한다. 시간 선택을 지우며 시간/열기/날짜 listener는 호출하지 않는다. callback 뒤 시간 일정의 시작 행이 첫 표시 행 이상이면 끝 제외 행을, 이전이면 시작 행을 표시한다. 길이0 일정은 한 행이다. 종일 일정은 스크롤 행0에서 `floor(viewportHeight/rowHeight)/2+12` 행을 표시하며 다른 스크롤 위치는 유지한다. callback이 선택을 바꾸면 이 스크롤 단계는 생략한다. 빈 열은 선택을 유지하며 callback이 없다. WinCE VK_F20/F21 하드웨어 메시지 처리는 미구현이다.

#### 다른 날짜의 약속 찾기

다중 열의 다른 날짜 검색은 빈 열을 건너뛴다. 시간 일정 선택 시 시작 분을 기준으로 가장 가까운 시작 분을 고른다. 시간 선택은 현재 interval과 관계없이 `60*(timeStart/interval)`을 기준으로 하며 선택이 없으면 0이다. 가장 가까운 후보에서 진행 반대 방향의 교차 칸을 따라간다. Android 최소 폭으로 칸이 겹치면 최대 배치 수만큼만 탐색한다. 종일 일정 선택 시 같은 `recordId`를 우선하고 없으면 첫 종일 일정을 고른다. 각 다른 열에서 해당 일정 유형이 없으면 다른 유형으로 대체한다. tag는 비교하지 않는다.

#### 정렬과 시간대

모든 모드의 시간 일정 배치·좌우 검색은 원본 civil 년/월/일/시/분 오름차순, busyStatus 내림차순이다. 같은 키는 입력 순서를 유지한다. 원본 epoch 시각은 뷰 생성 당시 기본 시간대로 변환한다. setLocale는 시간대를 바꾸지 않는다. 시스템 시간대가 바뀌면 뷰를 재생성한다. 원본 시각이 없으면 열 날짜와 clipped startMinute를 사용한다. 좌우 검색에서 선택·후보의 원본 날짜가 그 열 날짜와 다르면 0분으로 계산한다. DST 중복 시간에서도 epoch 정렬로 대체하지 않는다.

#### WM 동작 대응과 범위 이동

같은 열에 종일 일정이 있으면 text 모드의 시간 후보 검색은 원본 신규 node의 빈 사각형 union 실패처럼 중단한다. busy-only 모드는 계속 검색한다. 원본 재사용 node의 이전 geometry는 재현하지 않는다. 범위 소진은 소유자 날짜 이동 listener를 호출하며, 소유자는 새 주를 조회한 뒤 캡처한 WeekNavigation을 적용한다.

civil은 시간대 변환 후 연·월·일·시·분 값입니다. 신규 native node는 원본의 새 배치 노드, 이전 geometry는 재사용 노드의 과거 좌표입니다.

[시간 범위](/ko/api/wmcalendarview/#settimeselection) · [Event](/ko/api/wmcalendarview/#event) · [WeekNavigation](/ko/api/wmcalendarview/#captureweeknavigation--restoreweeknavigation)

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
public final int day;
public final int startMinute;
public final int endMinute;
public final int busyStatus;
public final boolean allDay;
public final String title;
public final String detail;
public final Object tag;
public final int symbolMask;
public final boolean recurrenceException;
public final String recordId;
public final boolean hasSourceTimes;
public final long sourceStartMillis;
public final long sourceEndMillis;
```

day는 setRange 시작일에서 0 기반 열이다. startMinute/endMinute는 해당 civil 날짜의 분이며 끝은 exclusive이다. 같은 시작/끝은 최소 높이의 시간 표시를 만든다. allDay는 DAY에서 헤더 목록, 다중 열에서 집계 busy 띠로 표시한다. title/detail은 호출자 원문을 유지한다. 다중 열 상세에는 title/detail을 두 줄로 잘라 표시하며 CR/LF에서 끊고 잘리면 "..."을 붙인다.

tag는 null을 포함한 임의의 호출자 객체를 유지한다. 컨트롤은 tag를 해석하거나 일정 identity·반복·다일 관계를 추론하지 않는다. `selectEvent`와 `eventBounds`에는 등록한 Event 인스턴스를 사용한다. symbolMask는 [WmCalendarSymbols](/ko/api/wmcalendarsymbols/) 비트이며 recurrenceException은 표시 중인 반복 비트의 심볼을 바꾼다. busyStatus는 FREE=0/TENTATIVE=1/BUSY=2/OUT_OF_OFFICE=3.

`recordId`는 호출자의 원본 record를 식별한다. `hasSourceTimes`는 전체 발생 시간 제공 여부를 구분한다. `sourceStartMillis`/`sourceEndMillis`는 화면 날짜로 잘리지 않은 epoch milliseconds이며 표시 시간대와 독립적이다. 표시 열·분은 별도로 전달한다. 생성자는 원본 시간에서 이를 계산하거나 서로의 일관성을 검사하지 않는다.

### 생성자

```java
public Event(int day,int startMinute,int endMinute,boolean allDay,String title,String detail,Object tag);
public Event(int day,int startMinute,int endMinute,boolean allDay,String title,String detail,Object tag,int symbolMask,boolean recurrenceException);
public Event(int day,int startMinute,int endMinute,boolean allDay,String title,String detail,Object tag,int symbolMask,boolean recurrenceException,int busyStatus);
public Event(int day,int startMinute,int endMinute,boolean allDay,String title,String detail,Object tag,int symbolMask,boolean recurrenceException,int busyStatus,String recordId,long sourceStartMillis,long sourceEndMillis);
```

day 0..6, 0<=start<1440, start<=end<=1440, non-null title/detail, mask 0..63, status 0..3. 위반은 `IllegalArgumentException`. 첫 overload의 기본값은 symbolMask=0, recurrenceException=false이다. busyStatus 없는 두 overload는 BUSY를 사용하며 두 번째는 전달한 symbolMask/recurrenceException을 유지한다. 기존 세 overload는 recordId=null, hasSourceTimes=false, 원본 시간 두 값0을 사용한다. 13인수 overload는 hasSourceTimes=true이며 null·빈 recordId 또는 sourceEndMillis<sourceStartMillis면 `IllegalArgumentException`이다. 공백을 포함한 recordId 원문을 보존하고 같거나 음수인 epoch 끝점도 허용한다.

## OnEventListener

```java
public interface OnEventListener
```

### onSelected

```java
void onSelected(Event event);
```

약속 DOWN 또는 키보드 약속 선택에서 등록된 Event를 전달한다. callback 전에 선택을 변경하고 시간 선택을 해제하며 키보드 cursor를 갱신한다. DAY 키보드는 다른 약속으로 이동할 때만 통지한다. 다중 열 위/아래는 같은 약속으로 돌아와도 통지한다. 선택된 약속을 다시 터치하면 재통지할 수 있다. `selectEvent`는 통지하지 않는다.

### onOpen

```java
void onOpen(Event event);
```

동일 약속의 완성된 UP이 touch slop 이내면 같은 Event를 전달한다. 이동·취소·다중 터치·다른 UP 대상은 열기를 막지만 앞선 DOWN 선택은 유지한다. 키보드 선택은 이 callback을 호출하지 않는다.

## OnDateListener

```java
public interface OnDateListener
```

### onDate

```java
void onDate(int year,int month,int day);
```

5/6/7열 헤더의 같은 열에서 완성된 탭을 해당 civil 날짜로 전달한다. month는 1 기반이다. 소유자가 변경하기 전까지 범위는 유지한다.

## OnTimeListener

```java
public interface OnTimeListener
```

### onSelected

```java
void onSelected(int day,int startMinute,int endMinute);
```

빈 시간칸의 완성 탭 또는 키보드 시간 선택 후 `(day,startMinute,endMinute)`를 전달한다. day는 0 기반이고 분은 현재 간격에 정렬되며 끝은 exclusive이다. callback 전에 상태를 반영한다. 프로그램 변경은 통지하지 않는다.

## OnNavigateListener

```java
public interface OnNavigateListener
```

### onNavigate

```java
void onNavigate(int direction);
```

DAY 좌/우에서 이웃 시간 일정을 선택하지 못하거나 다중 열 검색이 범위를 소진하면 좌 -1, 우 +1을 전달한다. 날짜/범위 변경은 소유자가 수행하며 컨트롤이 자동 이동하지 않는다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmCalendarView.java`
