---
title: "WmRecurrenceRule"
slug: ko/api/wmrecurrencerule
---
[API 색인](/ko/api/)

```java
public final class WmRecurrenceRule
```

반복 유형과 종료 조건을 보존하는 불변 값이다. 새 RRULE을 직렬화하며 기존 RRULE 파싱이나 반복 인스턴스 확장은 하지 않는다.

### 상수와 필드

```java
public static final int NONE = 0;
public static final int DAILY = 1;
public static final int WEEKLY = 2;
public static final int MONTHLY_DAY = 3;
public static final int MONTHLY_ORDINAL = 4;
public static final int YEARLY_DAY = 5;
public static final int YEARLY_ORDINAL = 6;
public static final int SUNDAY = 1;
public static final int MONDAY = 2;
public static final int TUESDAY = 4;
public static final int WEDNESDAY = 8;
public static final int THURSDAY = 16;
public static final int FRIDAY = 32;
public static final int SATURDAY = 64;
public static final int ALL = 127;
public static final int WEEKDAYS = 62;
public static final int WEEKEND = 65;
public static final int NEVER = 0;
public static final int COUNT = 1;
public static final int UNTIL = 2;
public final int pattern;
public final int interval;
public final int dayMask;
public final int day;
public final int month;
public final int ordinal;
public final int endMode;
public final int count;
```

유형: NONE=0, DAILY=1, WEEKLY=2, MONTHLY_DAY=3, MONTHLY_ORDINAL=4, YEARLY_DAY=5, YEARLY_ORDINAL=6. 요일 비트: 일요일 1부터 토요일 64; ALL=127, WEEKDAYS=62, WEEKEND=65. 종료: NEVER=0, COUNT=1, UNTIL=2. `interval`은 유형의 반복 간격; `dayMask`는 요일 비트; `day`/`month`는 1 기반 날짜; `ordinal`은 순번; `count`는 COUNT의 횟수이다. 종료 날짜 자체는 저장하지 않는다.

### minimumPeriodMinutes

```java
public long minimumPeriodMinutes();
```

약속 기간, 알림 검증 한계를 분 단위로 반환한다. 실제 반복 일정을 확장해 구한 최단 간격은 아니다.

| 반복 유형 | 반환값(분) |
| --- | --- |
| NONE | `0` |
| DAILY | `interval * 1440` |
| DAILY, 주중만 | `1440` |
| WEEKLY | 선택 요일 간 검증 간격(일) × `1440` |
| MONTHLY_DAY / MONTHLY_ORDINAL | `interval * 28 * 1440` |
| YEARLY_DAY / YEARLY_ORDINAL | `12 * 28 * 1440` |

주 반복은 `7 * interval`일에서 시작한다. `interval == 1`이면 일요일부터 2주, 그보다 크면 일요일부터 1주를 검사한다. 서로 다른 선택 요일 사이의 더 작은 간격을 사용한다. NONE의 `0`은 기간 상한을 적용하지 않는다는 뜻이다.

### validateAppointmentDuration

```java
public void validateAppointmentDuration(long civilMillis);
```

`civilMillis`는 시간대 오프셋 변화를 제외하고 연, 월, 일, 시, 분 표시값 사이에서 계산한 기간(밀리초)이다. 음수이면 반복 유형과 관계없이 `IllegalArgumentException`. 반복이 있으면 `minimumPeriodMinutes() * 60000`을 초과할 때 예외를 던지며 같은 값은 허용한다. NONE은 상한 검사 없이 0 이상의 기간을 허용한다. DST를 가로지르는 실제 경과 밀리초를 그대로 전달하지 않는다.

### validateAppointmentReminder

```java
public void validateAppointmentReminder(boolean enabled,long minutes);
```

비활성이면 즉시 반환한다. 활성 음수 분 또는 반복 규칙에서 알림 분이 주기 한계 이상이면 `IllegalArgumentException`. 약속 기간과 알림을 더하지 않는다.

### none

```java
public static WmRecurrenceRule none();
```

반복 없는 값. 직렬화하면 null.

### daily

```java
public static WmRecurrenceRule daily(int interval,boolean weekdays);
```

양의 일 간격. `weekdays=true`는 간격 1만 허용하며 월~금 반복이다. 잘못된 간격은 `IllegalArgumentException`.

### weekly

```java
public static WmRecurrenceRule weekly(int interval,int dayMask);
```

양의 주 간격과 비어 있지 않은 7비트 요일 마스크. 1..127 밖 마스크 또는 0 이하 간격은 `IllegalArgumentException`.

### monthlyDay

```java
public static WmRecurrenceRule monthlyDay(int interval,int day);
```

양의 월 간격과 날짜 1..31. 짧은 달의 처리, 인스턴스 확장은 Provider 책임이다. 잘못된 값은 `IllegalArgumentException`.

### monthlyOrdinal

```java
public static WmRecurrenceRule monthlyOrdinal(int interval,int ordinal,int dayMask);
```

양의 월 간격, 순번 -5..-1 또는 1..5, 요일 마스크 1..127. 음수 순번은 뒤에서 센다. 잘못된 값은 `IllegalArgumentException`.

### yearlyDay

```java
public static WmRecurrenceRule yearlyDay(int month,int day);
```

월 1..12와 실제 가능한 날짜를 요구한다. 윤년 기준으로 2월 29일은 허용한다. 불가능한 월/일은 `IllegalArgumentException`.

### yearlyOrdinal

```java
public static WmRecurrenceRule yearlyOrdinal(int month,int ordinal,int dayMask);
```

월 1..12, 순번 -5..-1 또는 1..5, 요일 마스크 1..127. 잘못된 값은 `IllegalArgumentException`.

### withCount

```java
public WmRecurrenceRule withCount(int count);
```

양의 횟수 종료를 설정한 새 값. NONE 또는 0 이하 횟수는 `IllegalArgumentException`.

### withUntil

```java
public WmRecurrenceRule withUntil();
```

UNTIL 종료 모드의 새 값. 날짜/UTC 변환은 직렬화할 때 제공한다. NONE은 `IllegalArgumentException`.

### withoutEnd

```java
public WmRecurrenceRule withoutEnd();
```

종료 모드를 NEVER로 변경한다. 이미 NEVER이면 같은 객체를 반환한다.

### toRrule

```java
public String toRrule(String untilUtc);
```

시간이 있는 일정의 RRULE을 `RRULE:` 접두어 없이 반환한다. UNTIL 모드는 호출자가 UTC로 변환한 16자 `yyyyMMddTHHmmssZ`를 요구한다. 예: `20261011T090000Z`. 날짜만 있는 `20261011`은 허용하지 않는다.

### toAllDayRrule

```java
public String toAllDayRrule(String untilDate);
```

종일 일정의 RRULE을 `RRULE:` 접두어 없이 반환한다. UNTIL 모드는 8자 날짜 `yyyyMMdd`를 요구한다. 예: `20261011`. 시간이나 `Z` 접미사는 허용하지 않는다.

두 직렬화 메서드 모두 실제 그레고리력 날짜(연도 0001..9999)와 ASCII 숫자를 요구하며 윤초는 허용하지 않는다. UNTIL 이외 종료 모드에는 null을 전달한다. NONE도 null이 필수이며 반환값은 null이다. 잘못된 인수는 `IllegalArgumentException`. 여러 요일의 순번은 BYSETPOS, 단일 요일은 BYDAY 순번으로 출력한다. API14 Provider의 YEARLY BYSETPOS와 주 시작 처리에는 차이가 있으므로 저장 대상 호환성을 확인한다.

### equals

```java
public boolean equals(Object other);
```

모든 유형, 간격, 요일, 날짜, 순번, 종료, 횟수 필드로 값 동등성을 계산한다.

### hashCode

```java
public int hashCode();
```

모든 유형, 간격, 요일, 날짜, 순번, 종료, 횟수 필드로 해시를 계산한다. 동등한 규칙은 같은 해시를 반환한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmRecurrenceRule.java`
