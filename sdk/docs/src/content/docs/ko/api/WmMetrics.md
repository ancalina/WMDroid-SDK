---
title: "WmMetrics"
slug: ko/api/wmmetrics
---
[API](/ko/api/)

```java
public final class WmMetrics
```

공유 UI 치수를 논리 픽셀로 제공한다. 전체 surface를 한 번 배율 변환하고 자식에 `sp`나 기기 배율을 다시 적용하지 않는다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmMetrics.java`

## 치수 상수

```java
public static final int WIDTH = 480;
public static final int BAR = 52;
public static final int FONT = 24;
public static final int LINE = 30;
public static final int GAP = 2;
public static final int INSET = 8;
public static final int BORDER = 2;
public static final int EDGE = 4;
public static final int INDICATOR = 30;
public static final int TEXT_LEFT = 56;
public static final int ICON = 32;
public static final int ICON_INSET = 4;
public static final int TODAY_MIN = 40;
public static final int TAB = 40;
public static final int TODAY_FONT = 23;
public static final int TODAY_LINE = 28;
```

모든 값은 배율 변환 전 SDK 논리 픽셀이다. 텍스트 크기는 Android `COMPLEX_UNIT_PX`로 적용한다.

| 상수 | 용도 |
| --- | --- |
| `WIDTH` | 논리 surface 기준 너비. |
| `BAR`, `TAB` | 제목, 명령 표시줄 높이, 탭 높이. |
| `FONT`, `LINE` | 기본 텍스트 크기, 기본 줄 높이. |
| `GAP`, `INSET` | 줄 사이 간격, 기본 안쪽 여백. |
| `BORDER`, `EDGE` | 테두리 두께, 컨트롤 가장자리 치수. |
| `INDICATOR` | 공유 체크박스, 라디오 표시기의 너비와 높이. |
| `TEXT_LEFT` | 기본 Today 텍스트의 왼쪽 위치. |
| `ICON`, `ICON_INSET` | Today 아이콘 크기, 왼쪽, 위 여백. |
| `TODAY_MIN` | Today 항목 최소 높이. |
| `TODAY_FONT`, `TODAY_LINE` | Today 기본 텍스트 크기, 줄 높이. |

## comboHeight

```java
public static int comboHeight(int textHeight);
```

단일 줄 콤보의 높이 `max(32,textHeight)+EDGE+2*BORDER`를 반환한다. `textHeight`는 글꼴 행의 논리 픽셀 높이다. 음수도 `32`로 올리므로 최소 반환값은 `40`이다.

중간 합을 `long`으로 계산하고 결과를 `Integer.MAX_VALUE`로 제한한다. 실제 View 배치는 부모의 사용 가능한 공간에 맞춰 소유자가 제한한다.

## todayHeight

```java
public static int todayHeight(int lines);
```

`lines <= 1`이면 `40`을 반환한다. 그 외에는 `lines*28+(lines-1)*2+16`을 반환한다. 음수, 0도 `40`이며 줄 수를 검증하지 않는다.

계산은 Java `int` 연산이다. 큰 줄 수의 오버플로를 제한하지 않으므로 소유자가 줄 수를 제한해야 한다. 텍스트 크기를 바꾼 Today 항목은 [WmToday.TextStyle.height](/ko/api/wmtoday/#textstyle)의 크기별 높이를 사용한다. 일정 제목, 상세 블록은 `WmToday.Row.setAppointments()`로 구성한다.
