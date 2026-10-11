---
title: "WmCalendarHeader"
slug: ko/api/wmcalendarheader
---
[API 색인](/ko/api/)

```java
public final class WmCalendarHeader extends View implements WmControls.RestyleListener
```

Calendar의 보기별 날짜 도구 모음. 날짜/월/연도/주 선택과 탐색 명령을 전달한다. 날짜 변경이나 팝업 배치는 소유자가 수행한다. 높이44와 hit 좌표는 논리 픽셀이다.

## 사용 준비

### 상수와 필드

```java
public static final int HEIGHT = 44;
public static final int AGENDA = 0;
public static final int DAY = 1;
public static final int WEEK = 2;
public static final int MONTH = 3;
public static final int YEAR = 4;
public static final int PREVIOUS = 20000;
public static final int NEXT = 20001;
public static final int TODAY = 20052;
public static final int MONTH_PICKER = 20012;
public static final int YEAR_PICKER = 20022;
public static final int WEEK_PICKER = 20032;
public static final int DATE_PICKER = 20053;
public static final int FIRST_WEEKDAY = 20054;
```

보기 AGENDA=0/DAY=1/WEEK=2/MONTH=3/YEAR=4. 명령 PREVIOUS=20000/NEXT=20001/TODAY=20052/MONTH_PICKER=20012/YEAR_PICKER=20022/WEEK_PICKER=20032/DATE_PICKER=20053. FIRST_WEEKDAY=20054부터 +6은 표시 순서의 요일 버튼이다. HEIGHT=44.

### 생성자

```java
public WmCalendarHeader(Context context,Typeface font,Locale locale);
```

Locale은 null 불가(`IllegalArgumentException`). 도구 아이콘은 현재 리소스팩에서 읽고 누락 시 fallback을 사용한다. 초기 mode는 AGENDA.

## 버튼·명령

### setTodayButtonVisible

```java
public void setTodayButtonVisible(boolean visible);
```

Today 버튼 표시 여부. 기본 true이며 원본 보기별 명령과 별개인 명시적 SDK 선택이다. 변경 시 진행 중 터치를 취소한다.

### setOnCommandListener

```java
public void setOnCommandListener(OnCommandListener listener);
```

같은 버튼에서 완성된 터치 UP을 command id로 통지한다. 움직여 다른 버튼으로 나가거나 다중 터치면 취소한다. null로 해제한다.

## 언어·주 번호

### setLocale

```java
public void setLocale(Locale locale);
```

날짜 문구·월 이름·요일 배치와 autosize를 갱신한다. null은 `IllegalArgumentException`.

### setFirstDayOfWeek

```java
public void setFirstDayOfWeek(int day);
```

Calendar.SUNDAY 또는 Calendar.MONDAY를 설정한다. 그 외 값은 IllegalArgumentException. 날짜 표시와 진행 중 선택을 갱신한다.

### getFirstDayOfWeek

```java
public int getFirstDayOfWeek();
```

명시한 첫 요일을 반환한다. 지정 전에는 현재 Locale의 첫 요일을 반환한다.

### toolbarWeekNumber

```java
public static int toolbarWeekNumber(int year,int month,int day,int firstDay);
```

지정한 Sunday/Monday 주의 Monday를 기준으로 원본 Jan4 산식을 적용한다. month는1 기반. 잘못된 날짜/첫 요일은 `IllegalArgumentException`. 연말에는 원본 산식의 week0도 나올 수 있으며 일반 Locale WEEK_OF_YEAR와 구분한다.

### setWeekNumbersVisible

```java
public void setWeekNumbersVisible(boolean visible);
```

Week 도구 모음의 주 선택기만 숨기거나 표시한다. 다른 View의 gutter 설정과 별개이다. 기본 true.

## 날짜·모드

### setDate

```java
public void setDate(int year,int month,int day);
```

실제 날짜(month1 기반)를 교체하고 preview를 지운다. 연도 범위 문구도 해당 연도 하나로 초기화한다. 잘못된 날짜는 `IllegalArgumentException`.

### setDatePreview

```java
public void setDatePreview(int year,int month,int day);
```

선택 팝업의 날짜를 도구 문구에만 미리 표시한다. 실제 checked 요일/기준 날짜는 유지한다. clear는 기준 날짜 문구로 복귀한다. 잘못된 preview 날짜는 `IllegalArgumentException`.

### clearDatePreview

```java
public void clearDatePreview();
```

미리보기 날짜를 지우고 확정된 기준 날짜 문구로 돌아간다. 미리보기가 없으면 변경하지 않는다.

### setMode

```java
public void setMode(int mode);
```

AGENDA..YEAR(0..4)만 허용한다. mode 변경 시 preview와 터치를 지우고 geometry를 갱신한다. 그 외는 `IllegalArgumentException`.

### setYearRange

```java
public void setYearRange(int first,int last);
```

Year 문구용 first..last. first>=1, last>=first, 차이는0 또는1. 잘못된 범위는 `IllegalArgumentException`. 기준 날짜는 바꾸지 않는다.

## 스타일·좌표·수명

### setColors

```java
public void setColors(int selection);
```

선택색과 파생 face를 도구 모음에 반영한다.

### commandBounds

```java
public Rect commandBounds(int id);
```

현재 geometry에서 명령의 Rect 사본. 표시되지 않는 id는 빈 Rect. layout 전 너비는 확정되지 않는다.

### onMeasure

```java
protected void onMeasure(int width,int height);
```

Android 측정·배치·그리기 hook. 논리 좌표와 현재 글꼴 지표로 control geometry 및 선택 표시를 갱신한다. 직접 호출하지 않는다.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

터치를 취소하고 일반 글꼴·선택색·파생 배경색을 갱신한다. 다음 배치에서 command geometry를 다시 계산한다.

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

### performClick

```java
public boolean performClick();
```

Android 접근성/클릭 dispatch hook. 날짜/명령의 실제 선택은 위 class의 사용자 선택 계약을 따른다.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Android 분리 hook. 진행 중인 터치 선택을 취소한다.

## OnCommandListener

```java
public interface OnCommandListener
```

### command

```java
void command(int id);
```

같은 버튼의 탭 완료 시 UI 터치 처리 중 동기로 호출합니다. id는 PREVIOUS/NEXT/TODAY/MONTH_PICKER/YEAR_PICKER/WEEK_PICKER/DATE_PICKER 또는 FIRST_WEEKDAY+0..6입니다. 요일0은 getFirstDayOfWeek()이며 이후는 표시 순서입니다. 첫 요일이 Calendar.MONDAY이면 FIRST_WEEKDAY는 월요일입니다. callback 자체는 날짜를 바꾸거나 팝업을 열지 않으므로 소유자가 명령을 처리합니다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmCalendarHeader.java`
