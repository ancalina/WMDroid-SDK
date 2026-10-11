---
title: "WmCalendarSymbols"
slug: ko/api/wmcalendarsymbols
---
[API 색인](/ko/api/)

```java
public final class WmCalendarSymbols extends Drawable
```

약속 metadata 심볼 Drawable. 옵션용6칸 atlas와 실제 약속용 atlas를 구분한다. 각 셀은32×28 논리 픽셀이고 팩의 빨강 transparency key를 제거한다. 팩 누락은 emoji fallback이다.

### 상수와 필드

```java
public static final int REMINDER = 1;
public static final int RECURRENCE = 2;
public static final int NOTES = 4;
public static final int LOCATION = 8;
public static final int ATTENDEES = 16;
public static final int PRIVATE = 32;
public static final int ALL = 63;
```

REMINDER=1, RECURRENCE=2, NOTES=4, LOCATION=8, ATTENDEES=16, PRIVATE=32, ALL=63. 함께 표시할 때 bitwise OR로 결합한다.

### 생성자

```java
public WmCalendarSymbols(Context context,int index);
```

옵션 atlas의 index0..5를 받는다(비트 값이 아님). 위 상수 순서이다. null Context 또는 범위 밖 index는 `IllegalArgumentException`.

### forEvent

```java
public static WmCalendarSymbols forEvent(Context context,int symbol,boolean recurrenceException);
public static WmCalendarSymbols forEvent(Context context,int symbol,boolean recurrenceException,boolean past);
```

symbol은 정확히 심볼 비트 하나. RECURRENCE이면 exception 분기를 적용하며 past=true는 경과 atlas bank를 사용한다. 짧은 overload는 past=false. 잘못된 bit/null Context는 `IllegalArgumentException`.

### draw

```java
public void draw(Canvas canvas);
```

Drawable bounds에 셀을 그린다. 팩 identity 변경을 확인하여 새 atlas를 읽는다. atlas는 Context별 캐시에서 공유하므로 호출자가 recycle하지 않는다.

### getIntrinsicWidth

```java
public int getIntrinsicWidth();
```

intrinsic 너비32를 반환한다. 실제 출력 크기는 Drawable bounds를 따른다.

### getIntrinsicHeight

```java
public int getIntrinsicHeight();
```

intrinsic 높이28을 반환한다. 실제 출력 크기는 Drawable bounds를 따른다.

### setAlpha

```java
public void setAlpha(int alpha);
```

Paint alpha를 적용하고 invalidateSelf를 호출한다.

### setColorFilter

```java
public void setColorFilter(ColorFilter filter);
```

Paint ColorFilter를 교체하고 invalidateSelf를 호출한다. null은 filter 해제.

### getOpacity

```java
public int getOpacity();
```

항상 PixelFormat.TRANSLUCENT.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmCalendarSymbols.java`
