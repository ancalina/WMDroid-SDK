---
title: "WmTimeFormat"
slug: ko/api/wmtimeformat
---
[API 색인](/ko/api/)

```java
public final class WmTimeFormat
```

일정 설명과 분 단위 시각 문구를 만든다. 시각·날짜는 호출자가 해당 지역의 civil 값으로 변환한다.

### calendarDetails

```java
public static String calendarDetails(String time,String location);
```

장소가 비면 `time`, 아니면 `time + " (" + location + ")"`을 반환한다. 장소의 공백·줄바꿈을 정리하지 않는다. null 인수는 `IllegalArgumentException`.

### minutePattern

```java
public static String minutePattern(Locale locale,boolean hour24);
```

24시간이면 `HH:mm`. 12시간의 한국어/한국 지역 또는 국가 없는 한국어는 `a h:mm`; 나머지는 `h:mm a`. null Locale은 `IllegalArgumentException`. Android 12/24시간 설정은 호출자가 전달한다. 사용자 지정 지역 시각 패턴은 반영하지 않는다.

## Agenda

```java
public static final class Agenda
```

### 생성자

```java
public Agenda(Locale locale,boolean hour24,String multiday);
```

UI 스레드에서 재사용하는 formatter. `multiday`는 비어 있지 않은 계속 일정 문구이다. 공백만 있는 문구도 허용한다. null/빈 문구 또는 null Locale은 `IllegalArgumentException`. 내부 Calendar와 formatter를 공유하므로 동시 호출하지 않는다.

### format

```java
public String format(WmDateTimeField.Value visibleDay,WmDateTimeField.Value start,WmDateTimeField.Value end);
```

입력은 호출자가 시간대 변환을 마친 civil 날짜·분 값입니다. visibleDay의 시각은 무시합니다. 다음 날짜 00:00 종료는 종료 판정에서 이전 civil 날짜에 속하지만 표시값은 실제00:00입니다.

| 보이는 날짜에 해당하는 끝점 | 반환 문구 |
| --- | --- |
| 시작·종료 모두 | `시작-종료` |
| 시작만 | `시작-multiday` |
| 종료만 | `multiday-종료` |
| 둘 다 아님 | `multiday` |

동일한 시작·종료 값이 보이는 날짜에 해당하면 시각 하나입니다. 예:24시간 형식으로10월10일10:00 시작·10월11일00:00 종료를10월10일에 표시하면 `10:00-00:00`입니다.

첫 영문 ` AM`이 있으면 그 앞 문자열에 `a`를 붙이고 뒤 내용을 버립니다. 없으면 첫 ` PM`에 같은 처리를 하여 `p`를 붙입니다. null 값은 `IllegalArgumentException`; 순서·기간·시간대는 검증하지 않으므로 호출자가 담당합니다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmTimeFormat.java`
