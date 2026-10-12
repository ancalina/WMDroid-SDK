---
title: "WmDateTimeField"
slug: ko/api/wmdatetimefield
---
[API 색인](/ko/api/)

```java
public final class WmDateTimeField extends FrameLayout implements WmControls.RestyleListener
```

날짜 분할 필드와 편집 가능한 시각 콤보를 결합한다. civil 값이며 시간대나 epoch를 포함하지 않는다. 팝업 배치, 저장은 호출자가 담당한다. View 변경과 콜백은 UI 스레드에서 사용한다.

공개 생성자는 1900..2999를 사용한다. `WmTaskEditor.reminderField()`가 반환한 컨트롤은 1752-09-14..9999-12-31, 필수 날짜, 작업 카드 외형을 사용한다. 값 설정, 상호작용 상태 검증에도 이 범위를 적용한다.

### 생성자

```java
public WmDateTimeField(Context context,Typeface font,Locale locale,Value value,int accent);
```

font/locale/value는 null 불가. 초기 날짜는 1900..2999. 오류는 `IllegalArgumentException`. 날짜는 필수이고 시각 후보는 기본 00:00부터 30분 간격 48개이다.

### dateChoice

```java
public WmDateField dateChoice();
```

소유된 `WmDateField`를 반환한다. 별도 부모에 다시 붙이지 않는다. 자식의 open/value-change listener는 날짜 변경과 composite 상태를 연결하므로 교체하지 않는다. 팝업은 `setOnDateOpen`, 값 관찰은 composite의 `setOnValueChangedListener`에 연결한다.

### timeChoice

```java
public WmEditableChoice timeChoice();
```

소유된 `WmEditableChoice`를 반환한다. 별도 부모에 다시 붙이지 않는다. 자식의 value-change listener는 시각 파싱과 마지막 유효 값, 통지 값을 동기화한다. 이 listener나 open listener를 교체하지 않는다. 팝업은 `setOnTimeOpen`, 값 관찰은 composite의 `setOnValueChangedListener`에 연결한다. 미완성 시각 문자열은 초안으로 유지한다.

### dateValue

```java
public Value dateValue();
```

현재 표시 날짜와 마지막 유효 시각을 결합한다. 시각 입력이 미완성이어도 읽을 수 있다. 잘못되거나 범위 밖 날짜는 `IllegalArgumentException`.

### getValue

```java
public Value getValue();
```

표시 날짜와 전체 문자열을 엄격히 파싱한 시각을 반환한다. 빈/미완성 시각이나 해당 컨트롤 범위 밖 날짜는 `IllegalArgumentException`.

### value

```java
public Value value();
```

표시 날짜와 전체 문자열을 엄격히 파싱한 시각을 반환한다. 빈/미완성 시각이나 해당 컨트롤 범위 밖 날짜는 `IllegalArgumentException`.

### setValue

```java
public void setValue(Value value);
```

컨트롤 범위 안의 유효한 값을 조용히 교체한다. 시각 초안과 이전 통지 값을 함께 교체하며 현재 형식으로 표시한다. 변경 콜백은 호출하지 않는다.

### selectDate

```java
public void selectDate(int year,int month,int day);
```

날짜만 변경한다. 비활성이면 변경하지 않는다. 시각 초안이 잘못되면 보존하고, 다시 유효해질 때 결합된 변경을 한 번 통지한다. 잘못된 날짜는 `IllegalArgumentException`.

### setOnDateOpen

```java
public void setOnDateOpen(OnDateOpen listener);
```

활성 필드의 열기 요청을 전달한다. 콜백의 source는 이 composite이다. null이면 열기 요청을 처리하지 않는다.

### setOnTimeOpen

```java
public void setOnTimeOpen(OnTimeOpen listener);
```

활성 필드의 열기 요청을 전달한다. 콜백의 source는 이 composite이다. null이면 열기 요청을 처리하지 않는다.

### setOnValueChangedListener

```java
public void setOnValueChangedListener(OnValueChangedListener listener);
```

유효하고 이전 통지 값과 다른 사용자 변경만 `(oldValue,newValue)`로 통지한다. 프로그램 값 교체, 복원은 조용하다. null로 해제한다.

### setTimeOrigin

```java
public void setTimeOrigin(int hour,int minute);
```

시각 후보의 시작점을 hour 0..23/minute 0..59로 지정한다. 30분씩 증가하며 하루를 순환한다. 현재 값은 바꾸지 않는다. 잘못된 시각은 `IllegalArgumentException`.

### setLocale

```java
public void setLocale(Locale locale);
```

유효 시각은 새 형식으로 바꾸고 캐럿을 가능한 범위 안에서 유지한다. 잘못된 초안은 이전 parser와 함께 유지하여 다음 유효 입력 때 새 형식으로 전환한다. null은 `IllegalArgumentException`.

### saveInteractionState

```java
public Bundle saveInteractionState();
```

날짜, 유효 시각, 원문 시각, 후보, 캐럿, 초점, parser, origin의 schema1 Bundle을 반환한다. 팝업, IME 조합, 테마, 사용 정책은 포함하지 않는다.

### validateInteractionState

```java
public void validateInteractionState(Bundle state);
```

현재 날짜 형식/범위와 전체 schema1 snapshot을 변경 전에 검증한다. 잘못된 타입, 스키마, 후보, parser, origin, 초점은 `IllegalArgumentException`.

### restoreInteractionState

```java
public void restoreInteractionState(Bundle state);
```

검증 후 조용히 값, 초안, 상호작용을 교체한다. 날짜/시각 팝업을 먼저 닫는다. 열린 팝업 또는 재진입은 `IllegalStateException`; 잘못된 snapshot은 `IllegalArgumentException`이고 상태를 교체하지 않는다.

### setEnabled

```java
public void setEnabled(boolean enabled);
```

자식 입력의 활성 상태를 함께 바꾼다. 비활성 중 사용자 변경을 차단한다.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

날짜/시각 자식에 style을 전달하고 결합 폭을 다시 측정한다.

### onMeasure

```java
protected void onMeasure(int widthSpec,int heightSpec);
```

Android 측정, 배치, 그리기 hook. 논리 좌표와 현재 글꼴 지표로 control geometry 및 선택 표시를 갱신한다. 직접 호출하지 않는다.

### onLayout

```java
protected void onLayout(boolean changed,int left,int top,int right,int bottom);
```

Android 측정, 배치, 그리기 hook. 논리 좌표와 현재 글꼴 지표로 control geometry 및 선택 표시를 갱신한다. 직접 호출하지 않는다.

## Value

```java
public static final class Value
```

### 상수와 필드

```java
public final int year;
public final int month;
public final int day;
public final int hour;
public final int minute;
```

`year/month/day/hour/minute`는 불변 civil 값이다. month는 1..12, hour는 0..23, minute는 0..59.

### 생성자

```java
public Value(int year,int month,int day,int hour,int minute);
```

연도 1 이상과 실제 proleptic Gregorian 날짜를 요구한다. 잘못된 날짜/시간은 `IllegalArgumentException`. 이 값 자체와 달리 편집 View는 위에 설명한 소유자별 범위를 적용한다.

### equals

```java
public boolean equals(Object other);
```

다섯 civil 필드로 값 동등성을 계산한다.

### hashCode

```java
public int hashCode();
```

다섯 civil 필드로 해시를 계산한다. 동등한 값은 같은 해시를 반환한다.

## OnDateOpen

```java
public interface OnDateOpen
```

### open

```java
void open(WmDateTimeField source);
```

소유자가 날짜 또는 시각 팝업을 배치할 때 호출된다. 선택 결과는 필드의 선택 API로 반영한다.

## OnTimeOpen

```java
public interface OnTimeOpen
```

### open

```java
void open(WmDateTimeField source);
```

소유자가 날짜 또는 시각 팝업을 배치할 때 호출된다. 선택 결과는 필드의 선택 API로 반영한다.

## OnValueChangedListener

```java
public interface OnValueChangedListener
```

### changed

```java
void changed(Value oldValue,Value newValue);
```

완성된 civil 값 변경. source 내부에서 동기 호출된다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmDateTimeField.java`
