---
title: "WmLunarCalendar"
slug: ko/api/wmlunarcalendar
---
[API](/ko/api/)

리소스팩 데이터로 Gregorian 날짜를 한국 음력 날짜로 변환한다. SDK는 연도표를 포함하지 않으며 중국 표로 대체하지 않는다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmLunarCalendar.java`

```java
public final class WmLunarCalendar
public static final String RESOURCE_ID = "assets/calendar/lunar/korean.json";
public static WmLunarCalendar load(WmResourcePack pack) throws IOException;
public Date convert(int year, int month, int day);
public String caption(Date date);

public static final class Date {
    public final int year, month, day, monthLength;
    public final boolean leapMonth;
}
```

## 로딩과 수명

`load(pack)`은 UI 스레드 밖에서 호출한다. pack이 null이거나 `RESOURCE_ID`가 없으면 null을 반환한다. 잘못된 데이터와 I/O 실패는 `IOException`을 발생시킨다. 자체 스트림을 열고 닫으며 활성 팩을 변경하지 않는다. 반환 객체는 불변이며 선택된 팩이 바뀌어도 유효하다.

공식 추출기는 사용자가 제공한 한국어 WM ROM 팩을 통해서만 원본 한국 연도표를 제공한다. WM 입력은 선택 사항이며 이 리소스가 없는 팩도 유효하다.

## 날짜와 캡션

`convert(year, month, day)`에는 Gregorian 날짜 값을 전달한다. 1월은 month 1이며 day는 1부터 시작한다. 기기 시간대는 변환에 영향을 주지 않는다. 잘못된 Gregorian 날짜와 로딩한 표 범위 밖 날짜는 null을 반환한다. 마지막 변환 가능 날짜는 표의 월 길이에 따라 정해진다.

`Date.year`는 음력 연도다. `month`는 1–12이며 `day`는 1부터 시작한다. `leapMonth`는 같은 번호의 평달과 삽입된 윤달을 구분한다. `monthLength`는 29 또는 30이다. 다섯 필드는 모두 final이며 호출자가 `Date`를 생성할 수 없다.

`caption(null)`은 null을 반환한다. 1일은 평달 또는 윤달 이름을 사용한다. 2–30일은 해당 날짜 캡션을 사용한다.

## 팩 데이터 계약

논리 ID는 `assets/calendar/lunar/korean.json`, 공식 ZIP 경로는 `calendar/lunar/korean.json`이다. 최대 32768바이트 UTF-8 JSON을 읽는다. JSON 객체 하나만 허용하며 뒤에 공백 외 내용이 있으면 거부한다.

| 필드 | 필수 값 |
| --- | --- |
| `version` | 정수 `1` |
| `epoch` | 문자열 `1900-01-31` |
| `first_year` | 정수 `1900` |
| `years` | 음력 1900–2049년의 정수 150개 |
| `captions.days` | 2–30일 문자열 29개 |
| `captions.months` | 평달 1–12월 문자열 12개 |
| `captions.leap_months` | 윤달 1–12월 문자열 12개 |

연도 word는 0–`0x1ffff`다. 하위 4비트는 윤달을 삽입할 평달 번호다. 0은 윤달 없음이며 1–12를 허용한다. bit 15–4는 평달 1–12월 길이, bit 16은 윤달 길이를 지정한다. 월 길이는 29에 해당 bit 값을 더한다.

각 캡션은 공백 외 문자를 포함하고 UTF-16 code unit 64개 이하여야 한다. ISO 제어 문자와 짝 없는 surrogate는 거부한다. 올바른 surrogate pair는 허용한다. 정수 필드에 부동소수점 값은 허용하지 않는다.

## 예제

기존 팩 snapshot을 전달한다. UI 스레드 밖에서 호출하고 호출자가 `IOException`을 처리하거나 전달한다.

```java
static String lunarCaption(WmResourcePack pack, int year, int month, int day)
        throws IOException {
    WmLunarCalendar lunar = WmLunarCalendar.load(pack);
    return lunar == null ? null : lunar.caption(lunar.convert(year, month, day));
}
```
