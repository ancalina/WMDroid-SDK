---
title: "WmMonthCalendar"
slug: ko/api/wmmonthcalendar
---
[API 색인](/ko/api/)

```java
public final class WmMonthCalendar extends View implements WmControls.RestyleListener,WmControls.LanguageListener
```

날짜 선택 팝업의42칸 몸체이며 Calendar 화면의 [WmMonthView](/ko/api/wmmonthview/)와 구분합니다. 앞뒤 달, 선택 날짜, Today 표시와 footer를 제공합니다. Today는 생성 시 읽은 기기 로컬 날짜이며 setMonth나 Locale 변경만으로 다시 계산하지 않습니다. 초기 선택은 이 날짜를 소유자의 허용 범위로 제한한 값입니다.

## 사용 준비

### 생성자

```java
public WmMonthCalendar(Context context,Typeface font,Typeface bold,Locale locale);
public WmMonthCalendar(Context context,Typeface font,Typeface bold,Locale locale,WmDateTimeField.Value minimum,WmDateTimeField.Value maximum);
```

Locale은 null 불가. 기본 범위1900-01-01..2999-12-31, overload는 기본 DATE 전체 범위 안의 순서 있는 full-date 경계를 받는다. 시간은 무시한다. 잘못된 경계는 `IllegalArgumentException`. 초기 선택은 오늘을 범위로 clamp한다.

## 날짜 선택

### setOnDateListener

```java
public void setOnDateListener(OnDateListener listener);
```

선택 가능한 날짜 또는 Today footer의 완성된 탭을 확정한다. 같은 날짜를 다시 눌러도 통지한다. 키보드 이동과 프로그램 setter는 통지하지 않는다. 날짜 변경 알림이 있으면 먼저 실행한다. null로 해제한다.

### setOnDateChangeListener

```java
public void setOnDateChangeListener(OnDateListener listener);
```

사용자 탭, 키보드 이동으로 선택 날짜가 달라질 때 통지한다. 프로그램 setter와 같은 날짜 탭은 통지하지 않는다. null로 해제한다. 월은 1 기반이다.

### setOnNoneListener

```java
public void setOnNoneListener(Runnable listener);
```

non-null이면 별도의 None footer 줄을 추가한다. 탭은 listener만 실행하고 날짜 선택을 유지한다. null이면 제거한다.

### setSelectedDate

```java
public void setSelectedDate(int year,int month,int day);
```

실제 날짜를 조용히 선택한다. owner 범위 밖이면 `IllegalArgumentException`. 표시 월은 바꾸지 않는다.

### selectedDate

```java
public int[] selectedDate();
```

새 `[year,month,day]` 선택 배열; month1 기반.

### setMonth

```java
public void setMonth(int year,int month);
```

owner 경계의 연/월 안에서42칸을 다시 계산한다. 잘못된 연/월은 `IllegalArgumentException`. 선택 날짜는 유지한다.

## 언어, 표시

### setLocale

```java
public void setLocale(Locale locale);
```

요일, 지역 week 산식, footer, geometry를 갱신한다. 명시적 첫 요일은 유지한다. null은 `IllegalArgumentException`.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

footer 문구를 갱신하고 터치를 취소한다. 날짜, 요일의 명시적 Locale은 유지한다. null은 IllegalArgumentException.

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

### setWeekNumbersVisible

```java
public void setWeekNumbersVisible(boolean visible);
```

기본 false인 주 번호 gutter를 표시한다. week 번호는 Locale의 최소 첫 주 일수와 설정한 첫 요일로 계산한다.

### setColors

```java
public void setColors(int accent);
```

선택 셀, 요일 글자의 accent를 지정한다.

## 좌표

### dateAt

```java
public int[] dateAt(int index);
```

`index` 셀의 새 `[year, month, day]` 배열을 반환한다. 월은 1 기반이다. 앞뒤 달, 소유자 범위 밖 날짜도 포함한다. `index`는 `0..41`이며 범위 밖이면 `IllegalArgumentException`이다.

### getHeaderHeight

```java
public int getHeaderHeight();
```

글꼴에 따라 계산한 요일 헤더 높이(논리 픽셀).

### getWeekColumnWidth

```java
public int getWeekColumnWidth();
```

주 gutter 논리 픽셀 너비. 주 번호가 숨겨졌으면0.

### cellBounds

```java
public Rect cellBounds(int index);
```

현재 View 크기, 글꼴, 주 번호 영역으로 계산한 `index` 셀의 새 Rect를 반환한다. 좌표는 View 내부 논리 픽셀이며 날짜 선택 가능 여부와 무관하다. `index`는 `0..41`이며 범위 밖이면 `IllegalArgumentException`이다.

## 입력, 초점, 수명

### onDraw

```java
protected void onDraw(Canvas canvas);
```

Android 측정, 배치, 그리기 hook. 논리 좌표와 현재 글꼴 지표로 control geometry 및 선택 표시를 갱신한다. 직접 호출하지 않는다.

### onTouchEvent

```java
public boolean onTouchEvent(MotionEvent event);
```

비활성 상태는 false를 반환하고 추적 중 터치를 취소한다. 같은 대상에서 완료한 단일 탭만 선택한다. 대상 밖 이동, 취소, 다중 터치는 선택하지 않는다. 날짜가 달라지면 변경 listener를 호출한 뒤 확인 listener를 호출한다. 같은 날짜는 확인 listener만 호출한다. 정상 View dispatch를 사용한다.

### onKeyDown

```java
public boolean onKeyDown(int code,KeyEvent event);
```

선택 날짜를 이동하고 표시 월을 맞춘 뒤 변경 listener를 통지한다. 확인 listener는 호출하지 않는다.

| 키 | 이동 |
| --- | --- |
| Left / Right | −1일 / +1일 |
| Up / Down | −7일 / +7일 |
| PageUp / PageDown | −1개월 / +1개월 |
| Ctrl+PageUp / Ctrl+PageDown | −1년 / +1년 |
| Home / End | 선택된 월의 첫날 / 마지막 날 |

월, 연도 이동은 GregorianCalendar의 날짜 보정 규칙을 따른다. 이동 결과가 소유자 범위 밖이면 날짜를 유지하고 키는 소비한다. 비활성 상태, Alt 조합, Ctrl+Page 이외 Ctrl 조합, 표에 없는 키는 상위 구현에 전달한다. Ctrl+Home/End 동작은 구현하지 않았다.

### performClick

```java
public boolean performClick();
```

Android 접근성/클릭 dispatch hook. 날짜/명령의 실제 선택은 위 class의 사용자 선택 계약을 따른다.

### onSizeChanged

```java
protected void onSizeChanged(int w,int h,int oldw,int oldh);
```

Android 측정, 배치, 그리기 hook. 논리 좌표와 현재 글꼴 지표로 control geometry 및 선택 표시를 갱신한다. 직접 호출하지 않는다.

### onFocusChanged

```java
protected void onFocusChanged(boolean gain,int direction,Rect previous);
```

Android 측정, 배치, 그리기 hook. 논리 좌표와 현재 글꼴 지표로 control geometry 및 선택 표시를 갱신한다. 직접 호출하지 않는다.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Android 분리 hook. 진행 중인 터치 선택을 취소한다.

### onWmRestyle

```java
public void onWmRestyle(Typeface font,Typeface bold,int selection,int tabColor);
```

일반/굵은 글꼴, accent를 갱신하고 터치를 취소한다. 날짜 격자를 다시 측정한다.

## OnDateListener

```java
public interface OnDateListener
```

### choose

```java
void choose(int year,int month,int day);
```

날짜를 `[year, month, day]`로 전달한다. 월은 1 기반이다. 확인, 변경 통지 조건은 listener 등록 메서드의 계약을 따른다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmMonthCalendar.java`
