---
title: "WmMonthView"
slug: ko/api/wmmonthview
---
[API 색인](/ko/api/)

```java
public final class WmMonthView extends View implements WmControls.RestyleListener
```

Calendar 월 보기의42칸 날짜·선택 효과·일정 장식을 표시한다. 날짜 팝업용 WmMonthCalendar와 구분한다. 전체 client 크기에 맞춰 셀을 계산한다.

## 사용 준비

### 생성자

```java
public WmMonthView(Context context,Typeface font,Locale locale);
public WmMonthView(Context context,Typeface regular,Typeface bold,Locale locale);
```

현재 월의 1일을 window anchor로 시작한다. 42칸은 anchor 당일 또는 그 이전의 첫 요일부터 시작한다. 첫 요일을 명시하기 전에는 `locale`을 따른다. 한 font overload는 bold를 파생한다. CJK 숫자/요일은 regular, 나머지는 bold를 사용한다.

## 날짜·주 선택

### setOnDayClickListener

```java
public void setOnDayClickListener(OnDayClickListener listener);
```

같은 날짜칸의 완성된 UP을 실제 날짜로 전달한다. 앞뒤 달도 선택 가능하다. month1 기반. null로 해제한다.

### setOnCommandListener

```java
public void setOnCommandListener(WmCalendarHeader.OnCommandListener listener);
```

활성 control에 non-null listener가 있으면 전달된 Left/PageUp DOWN마다 `WmCalendarHeader.PREVIOUS`, Right/PageDown DOWN마다 `NEXT`를 보낸다. 반복 전달도 각 1회이며 event repeat count를 곱하지 않는다. UP은 명령을 추가하지 않는다. 이 수평 명령은 owner가 선택 날짜와 월뷰를 갱신하며 widget이 단독으로 월을 바꾸지 않는다. 날짜·주 클릭은 발생하지 않는다. null은 listener를 해제한다. disabled control과 listener 없는 수평 키는 정상 View 키 경로로 위임한다. Up/Down은 아래 window 이동 계약을 따르며 다른 키는 정상 View 키 경로로 위임한다. OS 반복 생성과 원본 포커스 탐색은 이 계약에 포함하지 않는다.

### setOnRangeChangedListener

```java
public void setOnRangeChangedListener(OnRangeChangedListener listener);
```

활성 control은 listener 없이도 전달된 Up/Down DOWN을 처리한다. Up은 window anchor를 7일 앞당기고 Down은 7일 늦춘다. 42칸을 다시 계산하고 터치 선택을 취소하며 표시 대표 월을 갱신한다. non-null listener는 pulse 재시작·invalidate 전에 대표 월을 동기로 받는다. 대표 월이 그대로여도 통지한다. null은 callback만 해제하며 이동은 유지한다. 날짜·주 클릭과 수평 명령은 발생하지 않는다. 키를 놓으면 추가 이동하지 않으며 반복 전달도 event repeat count와 무관하게 각 1회 이동한다.

연도 guard는 이동 전 첫 칸의 날짜에서 Up은 1일을 뺀 날짜(연도 >=1900), Down은 42일을 더한 날짜(연도 <=2999)를 검사한다. 거부한 DOWN도 소비하며 상태 변경과 callback은 없다. 이 원본 경계식은 이동 후 42칸 전체가 1900..2999 안에 있다고 보장하지 않는다.

이동 후 대표 월은 새 첫 칸의 월이다. 단, `daysInMonth - firstCellDay < 21`이면 다음 월이다. 정확히 21이면 첫 칸의 월을 유지한다. 이 날짜 산식은 원본 포커스 탐색·렌더링 전체 동등성을 주장하지 않는다.

### setWeekNumbersVisible

```java
public void setWeekNumbersVisible(boolean visible);
```

기본 false인 주 gutter를 표시한다. 현재 터치와 geometry를 초기화한다.

### setOnWeekClickListener

```java
public void setOnWeekClickListener(OnWeekClickListener listener);
```

주 gutter의 완성 탭은 해당 행의 두 번째 날짜(설정에 따라 월/화요일)를 전달한다. null로 해제한다.

### getWeekColumnWidth

```java
public int getWeekColumnWidth();
```

현재 gutter 너비(논리 픽셀), 숨기면0.

### rowWeekNumber

```java
public int rowWeekNumber(int row);
```

row0..5의 두 번째 날짜에 원본 Jan4 산식을 적용한 주 번호이다. 범위 밖 row는 `IllegalArgumentException`.

## 월·언어·표시

### setMonth

```java
public void setMonth(int year,int month);
```

year>=1, month1..12. 잘못된 값은 `IllegalArgumentException`. 표시 월을 설정하고 window anchor를 해당 월 1일로 초기화한다. 같은 월을 다시 설정해도 초기화한다. 셀·현재 날짜 pulse·터치를 갱신하며 날짜별 장식 map과 음력 converter는 유지한다. range callback은 발생하지 않는다. 이동한 window를 복원하려면 `setMonth` 다음에 `setWindowAnchor`를 호출한다.

### getWindowAnchor

```java
public int[] getWindowAnchor();
```

새 `[year,month,day]` 배열을 반환하며 month는1..12다. anchor와 첫 요일로 첫 칸을 계산한다. 표시 대표 월과 별개이며 anchor가 첫 칸 날짜인 것은 아니다. 반환 배열을 변경해도 View는 바뀌지 않는다.

### setWindowAnchor

```java
public void setWindowAnchor(int year,int month,int day);
```

year>=1, month1..12 및 유효한 날짜가 필요하다. 잘못된 입력은 기존 anchor 변경 전에 `IllegalArgumentException`을 던진다. 키보드의 1900..2999 경계 검사는 이 setter에 적용하지 않는다.

표시 월을 바꾸지 않고 anchor와 42칸 window를 갱신한다. 진행 중 터치 선택을 취소하고 오늘 pulse를 재시작한다. 날짜 장식·음력 converter를 유지하며 range/날짜/주 callback은 발생하지 않는다. `setMonth`가 anchor를 초기화하므로 그 뒤에 복원한다.

### setLocale

```java
public void setLocale(Locale locale);
```

요일·글꼴 역할·셀 측정·pulse를 갱신한다. 표시 월과 window anchor를 보존한다. 첫 요일을 명시하지 않았다면 새 locale의 첫 요일을 적용해 같은 anchor의 첫 칸을 다시 계산한다. null은 `IllegalArgumentException`이며 range callback은 발생하지 않는다.

### setFirstDayOfWeek

```java
public void setFirstDayOfWeek(int day);
```

Calendar.SUNDAY 또는 Calendar.MONDAY를 설정한다. 그 외 값은 IllegalArgumentException. 표시 월과 window anchor를 보존하고 첫 칸을 다시 계산하며 진행 중 터치 선택을 취소한다. range callback은 발생하지 않는다.

### getFirstDayOfWeek

```java
public int getFirstDayOfWeek();
```

명시한 첫 요일을 반환한다. 지정 전에는 현재 Locale의 첫 요일을 반환한다.

### setColors

```java
public void setColors(int selection,int face);
```

선택색·요일 헤더 face의 ARGB 값을 직접 설정한다.

### clearDayFlags

```java
public void clearDayFlags();
```

모든 날짜 장식을 지운다.

### setDayFlags

```java
public void setDayFlags(int year,int month,int day,int value);
```

주어진 날짜 키의 정수 장식을 교체한다. low12 bits는 위 삼각, 다음12 bits는 아래 삼각, bit24는 외곽선이다. 두 삼각 비트가 모두 있으면 채운 사각형. 이 API는 날짜나 다른 bit를 검증하지 않는다.

### setLunarCalendar

```java
public void setLunarCalendar(WmLunarCalendar calendar);
```

기본 null이면 음력 캡션이 없다. non-null은 Gregorian 42칸 위에 캡션을 추가하며 null은 지운다. [WmLunarCalendar](/ko/api/wmlunarcalendar/)를 worker에서 로딩한 뒤 UI 스레드에서 setter를 호출한다. setter는 팩 I/O를 수행하지 않는다. 불변 converter를 보관하며 SDK는 원본 연도표를 포함하지 않는다.

Gregorian 날짜·장식·날짜/주 hit target은 바꾸지 않는다. 앞뒤 달을 포함해 행 우선 순서로 캡션을 계산한다. 첫 칸의 변환 실패는 모든 캡션을 생략한다. 이후 실패는 앞 캡션을 유지하고 나머지 칸을 중단한다.

converter나 셀 날짜가 바뀌면 캡션 문자열을 캐시한다. geometry 갱신 시 너비·글꼴 지표를 캐시한다. 그리기 중 날짜를 변환하지 않는다. restyle은 제공된 regular font를 갱신하며 locale 변경은 팩 캡션을 번역하지 않는다.

제공된 regular font를 논리 24픽셀로 사용하고 Android 글꼴 지표를 적용한다. 눌린 칸·오늘 pulse를 포함한 모든 상태에서 검정색이다. 측정한 텍스트 오른쪽 끝은 셀 오른쪽에서 논리 4픽셀, 아래 끝은 셀 아래에서 2픽셀 떨어진다. 너비·높이는 올림한다. 말줄임표는 넣지 않는다. clip의 왼쪽/위는 계산한 텍스트 x/y이고 오른쪽/아래는 원래 셀 경계다. 긴 캡션은 셀 왼쪽 밖으로 확장될 수 있다. 날짜 숫자·장식·grid·pulse 뒤에 그린다.

간격은 SDK의 논리 192-DPI border metric을 사용한다. 원본 음력 paint DC의 inherited font는 아직 식별되지 않았다. regular font와 Android 래스터화는 adaptation이며 GDI 픽셀 동등성을 주장하지 않는다. SDK의 API14 기준에서 사용하며 Calendar provider 쓰기는 필요하지 않다.

## 좌표

### getHeaderHeight

```java
public int getHeaderHeight();
```

글꼴 기반 요일 헤더 높이(논리 픽셀).

### dateAt

```java
public int[] dateAt(int index);
```

셀0..41의 새 [year,month,day] 배열이며 month는1..12입니다. 앞뒤 달도 포함하고 왼쪽 위부터 행 우선 순서입니다. 배열 변경은 View 날짜를 바꾸지 않습니다. 범위 밖 index는 IllegalArgumentException입니다.

### cellBounds

```java
public Rect cellBounds(int index);
```

셀0..41의 View 왼쪽 위 원점 기준 논리 픽셀 Rect 사본입니다. 현재 크기·주 번호 열을 반영하며 배치 전 최종 크기는 미정입니다. Rect 변경은 배치를 바꾸지 않습니다. 범위 밖 index는 IllegalArgumentException입니다.

## 스타일·입력·수명

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

일반/굵은 글꼴·선택색·파생 배경색을 갱신한다. 터치를 취소하고 행/열 geometry를 다시 측정한다. 표시 월·window anchor·날짜 장식·음력 converter는 보존한다.

### onSizeChanged

```java
protected void onSizeChanged(int w,int h,int oldw,int oldh);
```

Android 측정·배치·그리기 hook. 논리 좌표와 현재 글꼴 지표로 control geometry 및 선택 표시를 갱신한다. 직접 호출하지 않는다.

### onDraw

```java
protected void onDraw(Canvas canvas);
```

Android 측정·배치·그리기 hook. 논리 좌표와 현재 글꼴 지표로 control geometry 및 선택 표시를 갱신한다. 직접 호출하지 않는다.

### onTouchEvent

```java
public boolean onTouchEvent(MotionEvent event);
```

Android touch 경로. 같은 대상의 완성된 탭만 확정하며 취소·다중 터치·이동은 class의 선택 계약을 따른다. 직접 호출 대신 정상 View dispatch를 사용한다.

### onKeyDown

```java
public boolean onKeyDown(int keyCode,KeyEvent event);
```

위 수평 owner-command와 Up/Down window 이동 계약을 적용한다. 연도 guard가 거부한 Up/Down도 DOWN은 true를 반환한다. 그 외 경로는 `View.onKeyDown`에 위임한다. 키를 놓을 때 별도 탐색은 수행하지 않는다.

### performClick

```java
public boolean performClick();
```

Android 접근성/클릭 dispatch hook. 날짜/명령의 실제 선택은 위 class의 사용자 선택 계약을 따른다.

### onAttachedToWindow

```java
protected void onAttachedToWindow();
```

표시 중 오늘 날짜 강조 pulse를 시작한다. 숨김 상태에서는 pending pulse를 제거한다.

### onVisibilityChanged

```java
protected void onVisibilityChanged(View view,int visibility);
```

표시 중 오늘 날짜 강조 pulse를 시작한다. 숨김 상태에서는 pending pulse를 제거한다.

### onWindowVisibilityChanged

```java
protected void onWindowVisibilityChanged(int visibility);
```

표시 중 오늘 날짜 강조 pulse를 시작한다. 숨김 상태에서는 pending pulse를 제거한다.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

pulse와 터치 선택을 중지한다.

## OnDayClickListener

```java
public interface OnDayClickListener
```

### onDayClick

```java
void onDayClick(int year,int month,int day);
```

같은 날짜칸의 완성된 UP을 실제 날짜로 전달한다. 앞뒤 달도 선택 가능하다. month1 기반. null로 해제한다.

## OnWeekClickListener

```java
public interface OnWeekClickListener
```

### onWeekClick

```java
void onWeekClick(int year,int month,int day);
```

주 gutter의 완성 탭은 해당 행의 두 번째 날짜(설정에 따라 월/화요일)를 전달한다. null로 해제한다.

## OnRangeChangedListener

```java
public interface OnRangeChangedListener
```

### changed

```java
void changed(int year,int month);
```

허용한 Up/Down DOWN 뒤 대표 월을 전달하며 month는1..12다. 대표 월이 그대로여도 동기로 호출한다. 이때 View의 anchor와 날짜칸은 이미 갱신된 상태다. View를 교체하는 owner는 `getWindowAnchor()`를 보관하고 새 View의 월·locale·첫 요일을 설정한 뒤 `setWindowAnchor`로 복원한다. `setMonth`만 호출하면 이동한 window를 잃는다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmMonthView.java`
