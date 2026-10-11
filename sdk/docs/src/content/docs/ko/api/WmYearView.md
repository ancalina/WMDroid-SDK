---
title: "WmYearView"
slug: ko/api/wmyearview
---
[API 색인](/ko/api/)

```java
public final class WmYearView extends View implements WmControls.RestyleListener
```

Calendar 연도 보기. 폭160 논리 픽셀 단위의 작은 월 타일을 배치하고 높이에 맞는 atlas/월 수를 선택한다. 팩의 숫자 bitmap을 사용하고 누락 시 시스템 글꼴로 대체한다.

## 사용 준비

### 생성자

```java
public WmYearView(Context context,Typeface font,Locale locale);
```

현재 날짜로 시작한다. null Locale은 `IllegalArgumentException`. 작은 날짜 숫자는 atlas, 제목·탭 날짜 말풍선은 제공한 글꼴이다.

## 날짜·범위 선택

### setOnDayClickListener

```java
public void setOnDayClickListener(OnDayClickListener listener);
```

누른 뒤 움직이는 동안 후보 날짜 말풍선을 보여주고 UP에서 최종 후보를 전달한다. 빈 공간/취소는 통지하지 않는다. month1 기반. null로 해제한다.

### setOnRangeChangedListener

```java
public void setOnRangeChangedListener(OnRangeChangedListener listener);
```

날짜·월 페이지·visible 수가 달라지거나 showYearStart에서 현재 year/month/day를 동기 통지한다. geometry getter/resize에서도 발생할 수 있다. null로 해제한다.

### setDate

```java
public void setDate(int year,int month,int day);
```

실제 날짜1900..2999를 요구한다(`IllegalArgumentException`). 해당 월이 있는 페이지로 재배치하고 범위 변경을 통지한다.

### date

```java
public int[] date();
```

현재 저장된 날짜 상태의 새 [year,month,day] 배열입니다. 페이지 첫 날짜·터치 후보와 구분하며 터치만으로 이 상태를 바꾸지 않습니다. 확정 날짜를 반영하려면 소유자가 setDate를 호출합니다.

## 언어·표시

### setLocale

```java
public void setLocale(Locale locale);
```

월 제목과 지역 첫 요일을 바꾼다. 명시적 첫 요일은 유지한다. null은 `IllegalArgumentException`.

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

### setColors

```java
public void setColors(int selection);
```

월 제목 face를 선택색에서 파생한다. atlas 날짜색은 바꾸지 않는다.

## 연도 배치·좌표

### getMonthOffset

```java
public int getMonthOffset();
```

현재 year의1월 기준 첫 표시 월 offset. 페이지가 인접 연도를 걸치면 음수일 수 있다.

### getVisibleMonthCount

```java
public int getVisibleMonthCount();
```

geometry로 결정한 표시 월 수1..12.

### getColumnCount

```java
public int getColumnCount();
```

폭160 논리 픽셀당 열 하나, 최소1.

### getAtlasMode

```java
public int getAtlasMode();
```

0은 월 숫자 몸체92픽셀,1은132픽셀 atlas. client 크기에 맞춰 선택한다.

### getTitleHeight

```java
public int getTitleHeight();
```

현재 제목 글꼴 지표로 계산한 논리 픽셀 높이.

### monthTitleBounds

```java
public Rect monthTitleBounds(int index);
```

현재 page의0..11 타일 제목 Rect. 화면 밖 타일도 반환한다. 범위 밖이면 `IllegalArgumentException`.

### onSizeChanged

```java
protected void onSizeChanged(int w,int h,int oldw,int oldh);
```

Android 측정·배치·그리기 hook. 논리 좌표와 현재 글꼴 지표로 control geometry 및 선택 표시를 갱신한다. 직접 호출하지 않는다.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

글꼴·파생 배경색을 바꾸고 터치를 취소한다. 글꼴 변경 시 표시 월 수를 다시 측정하며 첫 표시 월을 유지한다.

## 탐색·입력·수명

### showYearStart

```java
public void showYearStart();
```

현재 연도의1월부터 표시한다. selected month는 유지한다. layout 전 호출도 보존하며 range callback을 호출한다.

### movePage

```java
public void movePage(int direction);
```

direction<0은 visible 수만큼 이전,>0은 이후,0은 유지. 1900..2999 밖 연도 이동은 무시한다. 바뀌면 range callback.

### onKeyDown

```java
public boolean onKeyDown(int keyCode,KeyEvent event);
```

DPAD_UP/DOWN은 표시 월을 한 tile 행(현재 columns개)만큼 이동한다. 그 외 키는 Android로 전달한다.

### onDraw

```java
protected void onDraw(Canvas canvas);
```

Android 측정·배치·그리기 hook. 논리 좌표와 현재 글꼴 지표로 control geometry 및 선택 표시를 갱신한다. 직접 호출하지 않는다.

### dateAt

```java
public int[] dateAt(float x,float y);
```

View 좌표의 새 `[year,month,day]` 또는 빈 영역이면 null. 작은 달력의 분할 날짜 셀은 삼각 영역에 맞춰 판정한다.

### onTouchEvent

```java
public boolean onTouchEvent(MotionEvent event);
```

활성 상태의 DOWN에서 키보드 포커스를 요청한다. 빈 영역도 포함한다. 기본적으로 터치 모드에서도 포커스를 받을 수 있다.

DOWN/MOVE에서 날짜 후보·확대 말풍선을 갱신하며 빈 영역으로 이동하면 후보를 지웁니다. UP은 마지막 DOWN/MOVE의 후보가 남아 있을 때만 choose를 동기로 호출하며 UP 좌표를 다시 계산하지 않습니다. 취소·다중 터치는 후보를 지우고 통지하지 않습니다. 저장된 date()는 바꾸지 않으므로 확정 날짜는 소유자가 setDate로 반영합니다.

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

## OnDayClickListener

```java
public interface OnDayClickListener
```

### choose

```java
void choose(int year,int month,int day);
```

누른 뒤 움직이는 동안 후보 날짜 말풍선을 보여주고 UP에서 최종 후보를 전달한다. 빈 공간/취소는 통지하지 않는다. month1 기반. null로 해제한다.

## OnRangeChangedListener

```java
public interface OnRangeChangedListener
```

### changed

```java
void changed(int year,int month,int day);
```

날짜·월 페이지·visible 수가 달라지거나 showYearStart에서 현재 year/month/day를 동기 통지한다. geometry getter/resize에서도 발생할 수 있다. null로 해제한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmYearView.java`
