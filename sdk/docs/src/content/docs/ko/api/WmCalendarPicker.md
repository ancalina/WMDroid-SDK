---
title: "WmCalendarPicker"
slug: ko/api/wmcalendarpicker
---
[API 색인](/ko/api/)

```java
public final class WmCalendarPicker extends LinearLayout implements WmControls.RestyleListener,WmControls.BackHandler
```

월 제목·달력·월 목록·인라인 연도 편집을 결합한 날짜 드롭다운. 소유자가 팝업의 배치와 해제를 수행한다.

### 생성자

```java
public WmCalendarPicker(Context context,Typeface font,Locale locale,int year,int month,int day,int selection,OnDateListener listener);
public WmCalendarPicker(Context context,Typeface regular,Typeface bold,Locale locale,int year,int month,int day,int selection,OnDateListener listener);
public WmCalendarPicker(Context context,Typeface regular,Typeface bold,Locale locale,int year,int month,int day,int selection,OnDateListener listener,WmDateTimeField.Value minimum,WmDateTimeField.Value maximum);
```

Locale과 선택 listener는 필수. 초기 날짜는 경계 안이어야 한다. 기본1900-01-01..2999-12-31; 마지막 overload는 시간 무시한 full-date 경계를 받는다. 잘못된 날짜/경계/null Locale·listener는 `IllegalArgumentException`. 한 face overload는 bold를 파생한다.

### naturalSize

```java
public android.graphics.Point naturalSize();
```

선호 client 크기의 새 Point를 반환한다. `x`·`y`는 SDK 논리 픽셀이며 소유자의 외부 테두리는 제외한다. 배치 전에 사용 가능한 영역으로 제한해야 한다.

현재 bold 글꼴의 Android 텍스트 지표와 Locale의 월·요일·footer 문구로 계산한다. 주 번호 표시와 None footer 유무도 반영한다. 원본 192DPI 크기 공식을 Android 지표로 적용한 값이며, 원본 GDI 픽셀과 동일함을 보장하지 않는다.

### onMeasure

```java
protected void onMeasure(int width,int height);
```

각 축에서 `naturalSize()`와 View 최소 크기 중 큰 값을 선호 크기로 사용한다. AT_MOST는 부모 상한까지 제한하고, UNSPECIFIED는 선호 크기를 사용한다. EXACTLY는 부모가 지정한 크기를 사용한다.

### back

```java
public boolean back();
```

열린 연도 편집은 입력을 commit하고 종료한다(취소가 아님). 월 목록은 닫는다. 하위 선택이 없으면 false.

### dispatchTouchEvent

```java
public boolean dispatchTouchEvent(MotionEvent event);
```

Android가 호출하는 터치 전달 메서드입니다. 인라인 연도 편집기 밖에서 DOWN이 시작되면 먼저 편집을 종료하고 입력을 확정합니다. 해당 DOWN부터 UP/CANCEL까지 소비하므로 같은 탭으로 아래 날짜나 도구 버튼을 누르지 않습니다. 원하는 항목은 다시 탭하세요. 편집기 안에서 시작한 터치는 좌표를 변환하여 해당 편집기에 전달합니다. 일반 앱 코드에서 직접 호출하지 않습니다.

### saveInteractionState

```java
public Bundle saveInteractionState();
```

schema1로 열린 month/year 상태, 스크롤, 연도 원문·선택을 반환한다. 현재 날짜는 date()로 따로 보존한다.

### validateInteractionState

```java
public static void validateInteractionState(Bundle state);
```

schema1와 choice/scroll/연도 ASCII draft/캐럿을 검증한다. 연도 초안은 비거나 범위 밖이어도 허용하며 commit하지 않는다. 잘못된 구조는 `IllegalArgumentException`.

### restoreInteractionState

```java
public void restoreInteractionState(Bundle state);
```

검증 후 기존 nested choice를 교체하고 월 스크롤 또는 연도 초안을 복원한다. 이전 callback 세대를 무효화한다. 잘못된 snapshot은 `IllegalArgumentException`. 날짜 자체는 복원하지 않는다.

### setOnDateChangeListener

```java
public void setOnDateChangeListener(OnDateListener listener);
```

월·연도·좌우 탐색, 날짜 탭, [달력 키보드 이동](/ko/api/wmmonthcalendar/#onkeydown)으로 날짜가 달라질 때 통지한다. 격자 변경은 현재 날짜와 헤더를 먼저 갱신한다. 날짜 탭의 변경 알림은 constructor 확인 callback보다 먼저 실행하며, 같은 날짜 탭은 확인 callback만 실행한다. 키보드 이동은 확인 callback을 호출하지 않는다. null로 해제한다.

### setOnNoneListener

```java
public void setOnNoneListener(Runnable listener);
```

None footer를 추가/제거한다. None 탭은 date()를 바꾸지 않고 listener를 실행한다.

### setFirstDayOfWeek

```java
public void setFirstDayOfWeek(int day);
```

몸체의 첫 요일을 Calendar.SUNDAY/MONDAY로 설정한다. 다른 값은 IllegalArgumentException.

### setWeekNumbersVisible

```java
public void setWeekNumbersVisible(boolean visible);
```

몸체의 주 번호 gutter 표시를 설정한다.

### date

```java
public int[] date();
```

새 `[year,month,day]` 배열. 탐색 중인 날짜까지 포함하며 month1 기반.

### setLocale

```java
public void setLocale(Locale locale);
```

제목·달력 문구·열린 월 목록 또는 연도 편집 문구를 바꾼다. 월 목록 스크롤을 보존한다. null은 `IllegalArgumentException`.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

header·month·열린 선택 메뉴에 style을 전달하고 인라인 연도 입력의 위치를 갱신한다.

## OnDateListener

```java
public interface OnDateListener
```

### choose

```java
void choose(int year,int month,int day);
```

constructor listener는 날짜·Today footer 탭을 확정한다. 별도 change listener는 탐색·탭·키보드 이동으로 바뀐 날짜를 전달한다. 월은 1 기반이다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmCalendarPicker.java`
