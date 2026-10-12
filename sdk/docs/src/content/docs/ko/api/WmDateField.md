---
title: "WmDateField"
slug: ko/api/wmdatefield
---
[API 색인](/ko/api/)

```java
public final class WmDateField extends View implements WmControls.RestyleListener,WmControls.LanguageListener
```

nullable 날짜 전용 분할 입력 View. 기본 범위 1752-09-14..9999-12-31, proleptic Gregorian 연산이다. 소유자가 날짜 팝업과 저장을 담당한다. 시간 필드는 00:00으로 정규화한다.

## 사용 준비

### 생성자

```java
public WmDateField(Context context,Typeface font,Locale locale,WmDateTimeField.Value value,int accent);
public WmDateField(Context context,Typeface font,Locale locale,WmDateTimeField.Value value,int accent,boolean shortDateFormat);
```

font/locale는 null 불가. value는 null 또는 기본 범위의 실제 날짜. `shortDateFormat=true`는 지역 짧은 날짜, false(기본)는 4자리 연도 형식이다. 잘못된 값은 `IllegalArgumentException`. 짧은 yy 형식은 선택 시 yyyy로 펼치지만 연도 숫자 입력을 무시한다.

## 값, 범위, 직렬화

### value

```java
public WmDateTimeField.Value value();
```

정규화한 현재 날짜 또는 null. 미완성 숫자 입력과 마지막 유효 값은 구분된다.

### setRange

```java
public void setRange(WmDateTimeField.Value minimum,WmDateTimeField.Value maximum);
```

시간을 무시한 순서 있는 non-null 날짜 경계. 기본 전체 범위 안이어야 한다. 현재 날짜를 조용히 범위에 맞춰 clamp하고 초안, 터치를 취소한다. 잘못된 경계는 `IllegalArgumentException`이며 상태를 바꾸지 않는다.

### setAllowEmpty

```java
public void setAllowEmpty(boolean allowEmpty);
```

null 날짜 허용 여부. 현재 값이 null인 채 false로 바꾸면 `IllegalArgumentException`.

### parseValue

```java
public static WmDateTimeField.Value parseValue(String canonical);
```

지역 형식과 무관한 ASCII `yyyy-MM-dd`를 읽는다. 빈 문자열은 null. null/잘못된 형식/불가능한 날짜/기본 범위 밖 값은 `IllegalArgumentException`.

### formatValue

```java
public static String formatValue(WmDateTimeField.Value value);
```

날짜를 ASCII `yyyy-MM-dd`로 반환한다. null은 빈 문자열. 시간은 무시하며 기본 범위 밖 값은 `IllegalArgumentException`.

## 상태 보존

### saveInteractionState

```java
public Bundle saveInteractionState();
```

schema2의 선택 segment, 미완성 숫자만 반환한다. 현재 날짜는 `formatValue`로 별도 보존한다.

### validateInteractionState

```java
public static void validateInteractionState(Bundle state,WmDateTimeField.Value value);
```

schema1/2와 주어진 nullable 날짜를 검증한다. 숫자는 아직 완성되지 않은 ASCII 입력이어야 한다. 잘못된 스키마, 역할, 숫자/날짜 조합은 `IllegalArgumentException`; 상태를 변경하지 않는다.

### restoreInteractionState

```java
public void restoreInteractionState(Bundle state);
```

현재 값, 형식, range에 맞춰 검증한 뒤 선택과 숫자 초안을 조용히 복원한다. 날짜를 먼저 복원한다. 잘못된 snapshot은 `IllegalArgumentException`.

## 날짜 편집, 구간 선택

### setValue

```java
public void setValue(WmDateTimeField.Value value);
```

범위/nullable 정책을 검증한 뒤 조용히 교체한다. 초안 숫자, 터치는 취소하며 변경 콜백은 호출하지 않는다. 잘못된 값은 `IllegalArgumentException`.

### selectDate

```java
public void selectDate(int year,int month,int day);
```

실제 날짜(month1..12), 현재 범위를 먼저 검증합니다. 잘못된 값은 비활성이어도 IllegalArgumentException입니다. 검증 후 disabled이면 값을 바꾸지 않습니다. 활성 상태는 값을 반영하고 이전 값과 다르면 변경 callback을 동기로 호출합니다.

### clearDate

```java
public void clearDate();
```

활성이고 null 허용이면 날짜를 지우고 변경을 통지한다. 그 외에는 유지한다.

### selectedField

```java
public int selectedField();
```

선택 중인 Calendar.YEAR/MONTH/DAY_OF_MONTH 역할. 선택 해제 시 fallback MONTH이다.

### selectField

```java
public void selectField(int field);
```

YEAR/MONTH/DAY_OF_MONTH만 허용한다. 다른 segment로 바꿀 때 활성 필드의 미완성 숫자를 먼저 commit한다. 잘못된 역할은 `IllegalArgumentException`.

## 언어, 팝업, 콜백

### setLocale

```java
public void setLocale(Locale locale);
```

지역별 segment 순서, 문구, 날짜 형식을 바꾼다. 값은 유지하고 터치를 취소한다. null 또는 SimpleDateFormat으로 표현 불가능한 형식은 `IllegalArgumentException`.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

공유 catalog 언어에 해당하는 Locale을 setLocale에 전달한다. 상태 보존, popup 처리는 setLocale 계약을 따른다.

### bindDropdown

```java
public void bindDropdown(View popup);
```

소유자가 배치한 popup View를 연결한다. 소유자가 popup을 생성하고 해제한다.

### isDroppedDown

```java
public boolean isDroppedDown();
```

연결된 popup이 표시 중이면 true.

### setOnOpenListener

```java
public void setOnOpenListener(OnOpenListener listener);
```

드롭다운 열기 전에 숫자를 commit하고 segment 선택을 해제한다. 활성 상태에서 source 필드를 전달한다. listener null은 요청을 처리하지 않는다.

### setOnValueChangedListener

```java
public void setOnValueChangedListener(OnValueChangedListener listener);
```

사용자 변경의 이전/새 nullable 날짜를 동기 통지한다. 값이 같거나 조용한 setter/복원이면 호출하지 않는다. null로 해제한다.

## 키보드, 터치

### onKeyDown

```java
public boolean onKeyDown(int key,KeyEvent event);
```

DPAD_CENTER/F4/PAGE_DOWN은 dropdown 요청. 좌우는 날짜 segment 이동, 위아래, numpad +/-는 값 증감, HOME/END는 해당 segment 경계, Ctrl+HOME은 오늘 날짜. 구분자는 다음 segment, 현지 숫자, 전각 숫자는 숫자 초안에 입력한다. 날짜 확정, 범위 처리는 위 입력 계약을 따른다.

### onKeyUp

```java
public boolean onKeyUp(int key,KeyEvent event);
```

활성 상태에서 위 날짜 편집 키의 UP을 소비한다. 그 외 키는 Android에 전달한다.

### onTouchEvent

```java
public boolean onTouchEvent(MotionEvent event);
```

일반 필드의 본문 탭은 날짜 구간을 선택하고, 화살표는 소유자의 드롭다운을 연다. `WmTaskEditor`에서 반환한 필드는 본문 전체에서 달력을 열고, 닫힌 동안 연도 네 자리의 날짜를 표시한다. 취소, 다중 터치, 대상 밖 이동은 활성화를 취소한다. 일반 View 이벤트 전달을 사용한다.

### performClick

```java
public boolean performClick();
```

비활성이면 false. 초점을 요청하고 비어 있는 날짜를 범위 안의 오늘 날짜로 활성화한 뒤 Android click을 전달한다. callback 중 비활성화되면 false.

## 스타일, 초점, 수명

### setBordered

```java
public void setBordered(boolean value);
```

true이면 독립 DATE용 테두리를 그린다. 기본 inline 필드는 테두리 없다. 값, 패딩, 범위는 바꾸지 않는다.

### onMeasure

```java
protected void onMeasure(int widthSpec,int heightSpec);
```

Android 측정, 배치, 그리기 hook. 논리 좌표와 현재 글꼴 지표로 control geometry 및 선택 표시를 갱신한다. 직접 호출하지 않는다.

### onDraw

```java
protected void onDraw(Canvas canvas);
```

Android 측정, 배치, 그리기 hook. 논리 좌표와 현재 글꼴 지표로 control geometry 및 선택 표시를 갱신한다. 직접 호출하지 않는다.

### setEnabled

```java
public void setEnabled(boolean enabled);
```

비활성화하면 터치, 숫자 초안, dropdown pressed 상태를 취소한다. 확정된 날짜는 유지한다.

### onFocusChanged

```java
protected void onFocusChanged(boolean gain,int direction,Rect previous);
```

Android가 초점 변경 시 호출한다. 선택 구간 없이 초점을 얻으면 일반 필드는 일 구간, 반복 편집기 날짜 형식은 요일 구간을 선택한다. 활성 상태에서 초점을 잃으면 미완성 숫자를 확정한다. 유효한 날짜가 달라지면 value-change listener가 동기 호출될 수 있다. 네 자리 연도 형식의 짧은 입력은 0..29를 2000..2029, 30..99를 1930..1999로 해석한다. 예: 연도 구간에 `25`를 입력하고 초점을 옮기면 2025년. 유효하지 않은 결과는 마지막 날짜를 유지한다. 두 자리 `yy` 형식은 연도 숫자 입력을 받지 않는다. 직접 호출하지 않는다.

### onSizeChanged

```java
protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight);
```

Android 측정, 배치, 그리기 hook. 논리 좌표와 현재 글꼴 지표로 control geometry 및 선택 표시를 갱신한다. 직접 호출하지 않는다.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

터치와 dropdown pressed 상태를 취소한다.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

regular null은 IllegalArgumentException. 일반 글꼴, 선택색을 갱신하고 터치를 취소하며 표시 geometry를 다시 측정한다. 날짜는 유지한다.

## OnValueChangedListener

```java
public interface OnValueChangedListener
```

### changed

```java
void changed(WmDateTimeField.Value oldValue,WmDateTimeField.Value newValue);
```

사용자 변경의 이전/새 nullable 날짜를 동기 통지한다. 값이 같거나 조용한 setter/복원이면 호출하지 않는다. null로 해제한다.

## OnOpenListener

```java
public interface OnOpenListener
```

### open

```java
void open(WmDateField field);
```

드롭다운 열기 전에 숫자를 commit하고 segment 선택을 해제한다. 활성 상태에서 source 필드를 전달한다. listener null은 요청을 처리하지 않는다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmDateField.java`
