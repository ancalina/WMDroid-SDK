---
title: "WmPalette"
slug: ko/api/wmpalette
---
[API 색인](/ko/api/)

```java
public final class WmPalette
```

테마 선택색에서 컨트롤 강조색·배경색을 계산하고 ARGB 색을 보간한다. 모든 색은 Android ARGB int다.

## highlight

```java
public static int highlight(int selection);
```

선택 행·선택 필드·체크 표시 등에 사용할 불투명 강조색을 반환한다. 입력 alpha를 `255`로 바꾼 뒤 RGB가 `#149ca7` 또는 `#15afbc`이면 `0xff37c7d3`, 그 외에는 입력 RGB를 반환한다.

## face

```java
public static int face(int selection);
```

버튼·스크롤바 등 컨트롤 배경에 사용할 불투명 색을 반환한다. 입력 alpha는 무시한다.

RGB가 `#149ca7` 또는 `#15afbc`이면 기본 배경색 `0xffc9e7e9`를 반환한다. 그 외에는 입력의 HSV hue와 기본 배경색의 saturation/value를 결합한다. 입력 saturation이 `0.01` 이하이면 hue 대신 기본 배경색의 가중 밝기 `222`를 사용하여 `0xffdedede`를 반환한다. 입력의 밝기·채도는 배경색에 그대로 적용하지 않는다.

## mix

```java
public static int mix(int a,int b,int weightB);
```

`weightB`를 `0..255`로 제한한 뒤 alpha·red·green·blue를 각각 보간한 ARGB int를 반환한다. 가중치 `0`은 `a`, `255`는 `b`다. 범위 밖 가중치는 가까운 끝값으로 제한한다.

각 채널은 정수식 `(aChannel*(255−t)+bChannel*t+127)/255`로 계산한다. `t`는 제한한 가중치이며 `+127`은 반올림용이다. alpha도 독립적으로 보간하므로 반환색이 항상 불투명한 것은 아니다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmPalette.java`
