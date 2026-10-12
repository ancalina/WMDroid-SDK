---
title: "WmMonthCalendarHeader"
slug: ko/api/wmmonthcalendarheader
---
[API 색인](/ko/api/)

```java
public final class WmMonthCalendarHeader extends View implements WmControls.RestyleListener
```

날짜 드롭다운의 월, 연도 제목과 좌우 버튼. Calendar 도구 모음과 별개이다. 제목은 bold 지표로 측정하며 월/연도 순서는 Locale에 따른다.

### 생성자

```java
public WmMonthCalendarHeader(Context context,Typeface bold,Locale locale);
```

제목 bold face와 non-null Locale을 제공한다. null Locale은 `IllegalArgumentException`.

### setOnCommandListener

```java
public void setOnCommandListener(WmCalendarHeader.OnCommandListener listener);
```

완성된 탭을 UI 터치 처리 중 동기로 PREVIOUS/NEXT/MONTH_PICKER/YEAR_PICKER로 전달합니다. null은 리스너를 제거합니다. 기본 경계에서는 표시 월이1900-01일 때 PREVIOUS,2999-12일 때 NEXT를 비활성화합니다. 해당 월과 같은 경우의 버튼 동작이며 setDate의 허용 연도1752..9999와 별개입니다. 소유자가 명령에 맞게 날짜를 선택하고 setDate를 호출합니다.

### setDate

```java
public void setDate(int year,int month,int day);
```

제목의 year1752..9999와 month1..12를 요구한다. day는 사용하지 않는다. 잘못된 연/월은 `IllegalArgumentException`.

### setLocale

```java
public void setLocale(Locale locale);
```

CJK는 연도 먼저, 나머지는 월 먼저 배치한다. 글꼴 측정과 터치를 갱신한다. null은 `IllegalArgumentException`.

### setColors

```java
public void setColors(int selection);
```

제목 배경 ARGB 색. 문자색은 흰색이다.

### commandBounds

```java
public Rect commandBounds(int id);
```

PREVIOUS/NEXT/MONTH_PICKER/YEAR_PICKER의 Rect 사본. 알 수 없는 id는 빈 Rect.

### onMeasure

```java
protected void onMeasure(int width,int height);
```

Android 측정, 배치, 그리기 hook. 논리 좌표와 현재 글꼴 지표로 control geometry 및 선택 표시를 갱신한다. 직접 호출하지 않는다.

### onSizeChanged

```java
protected void onSizeChanged(int w,int h,int oldw,int oldh);
```

Android 측정, 배치, 그리기 hook. 논리 좌표와 현재 글꼴 지표로 control geometry 및 선택 표시를 갱신한다. 직접 호출하지 않는다.

### onDraw

```java
protected void onDraw(Canvas canvas);
```

Android 측정, 배치, 그리기 hook. 논리 좌표와 현재 글꼴 지표로 control geometry 및 선택 표시를 갱신한다. 직접 호출하지 않는다.

### onTouchEvent

```java
public boolean onTouchEvent(MotionEvent e);
```

Android touch 경로. 같은 대상의 완성된 탭만 확정하며 취소, 다중 터치, 이동은 class의 선택 계약을 따른다. 직접 호출 대신 정상 View dispatch를 사용한다.

### performClick

```java
public boolean performClick();
```

Android 접근성/클릭 dispatch hook. 날짜/명령의 실제 선택은 위 class의 사용자 선택 계약을 따른다.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

터치를 취소하고 굵은 글꼴, 선택색을 갱신한 뒤 geometry를 다시 측정한다. regular와 tabColor는 사용하지 않는다.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Android 분리 hook. 진행 중인 터치 선택을 취소한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmMonthCalendarHeader.java`
