---
title: "WmCalendarDuration"
slug: ko/api/wmcalendarduration
---
[API 색인](/ko/api/)

```java
public final class WmCalendarDuration
```

약속 편집기의 지속 시간과 CalendarProvider `DURATION`을 변환한다. Provider 단위는 고정 경과 시간이다. DST 경계의 달력 날짜 연산과 구분한다.

### parseEditorMinutes

```java
public static int parseEditorMinutes(String value,Locale locale);
```

지역별 소수 표기로 입력한 지속 시간을 분으로 반환한다. 단위 없는 숫자는 시간이다. 분/시간/일/주에 해당하는 영문 약어와 `분`, `시간`, `일`, `주`를 허용한다. 결과는 94370400분으로 제한한 뒤 정수 분으로 절삭한다. null, 빈 입력, 255자 초과, 음수, 알 수 없는 단위, 단위 없는 0분 결과는 `IllegalArgumentException`. 명시적 단위의 0은 허용한다.

Grouping은 비활성화하며 입력 전체가 숫자와 아래 단위에 해당해야 합니다. 단위는 Locale.US 소문자 변환 후 비교하므로 영문 대소문자를 구분하지 않습니다. 입력 양끝을 trim한 뒤 길이를 검사합니다.

| 단위 | 허용 접미어 |
| --- | --- |
| 분 | `min`, `mins`, `minute`, `minutes`, `minute(s)`, `m`, `분` |
| 시간 | 없음, `hour`, `hours`, `hr`, `hrs`, `hour(s)`, `h`, `시간` |
| 일 | `day`, `days`, `day(s)`, `d`, `일` |
| 주 | `week`, `weeks`, `wk`, `wks`, `week(s)`, `w`, `주` |

예: `1.5`는 소수점 Locale에서90분, `90m`은90분, `0min`은0분입니다. 단위 없는 `0`은 오류입니다.

### parseRfcMillis

```java
public static long parseRfcMillis(String value);
```

`+` 선택 접두어와 `P<n>W`, `P<n>D`, `T` 아래의 시간, 분, 초 조합을 밀리초로 반환한다. 연/월, 음수, 소수, 빈 구성요소, 끝의 빈 `T`, long 범위 초과는 `IllegalArgumentException`.

### parseProviderMillis

```java
public static long parseProviderMillis(String value);
```

`parseRfcMillis` 형식에 더해 Android의 기존 `P<n>S` 초 형식을 읽는다. 오류 조건은 동일하다.

### timed

```java
public static String timed(long elapsedMillis);
```

경과 밀리초를 `PT<n>S`로 반환한다. 음수 또는 1000의 배수가 아닌 값은 `IllegalArgumentException`; 반올림하지 않는다.

### allDay

```java
public static String allDay(int days);
```

양의 일수를 `P<n>D`로 반환한다. 0 이하이면 `IllegalArgumentException`.

### eventEnd

```java
public static long eventEnd(long begin,Long explicitEnd,String duration);
```

`explicitEnd`가 있으면 검증이나 duration 파싱 없이 그대로 반환한다. 없으면 `begin + parseProviderMillis(duration)`을 반환한다. 덧셈 오버플로 또는 잘못된 duration은 `IllegalArgumentException`.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmCalendarDuration.java`
