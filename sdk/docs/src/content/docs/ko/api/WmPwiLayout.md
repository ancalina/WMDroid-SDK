---
title: "WmPwiLayout"
slug: ko/api/wmpwilayout
---
[API 색인](/ko/api/)

```java
public final class WmPwiLayout
```

복구한 RichInk의 좌표 변환·들여쓰기·줄 위치 계산. 입력은 native 논리 좌표와 명시한 DPI/scale이며 Android dp/sp가 아니다. 글꼴 측정·줄바꿈·그리기는 호출자가 수행한다.

## Zoom

```java
public static int zoomScale(int percent, boolean extendedBreaks)
public static int zoomPercent(int scale, boolean extendedBreaks)
```

percent↔native documentScale 변환. 표준 값: 75→198, 100→240/260, 125→300/325, 150→396, 200→550, 300→793; / 오른쪽은 extendedBreaks=true. 나머지는 zoomScale=`(2380*percent+500)/1000`, zoomPercent=`(1000*scale+1190)/2380`. 범위 clamp 없음, Java signed int overflow를 유지한다. legacy zoom 메뉴 인덱스 0..5를 입력하지 않는다.

## Coordinate conversion

```java
public static int scaleCoordinate(int value, int offset, int numerator, int denominator, int origin)
public static int unitScale(int unit, int dpi, int documentScale)
public static int convertUnits(int value, int sourceUnit, int targetUnit, int dpi, int documentScale)
```

scaleCoordinate는 offset 추가·비율 변환·반올림·origin 추가를 native signed32 계산으로 수행한다. denominator=0이면 origin을 더하지 않고 `Integer.MAX_VALUE`; 중간 overflow는 wrap. 양수 분모·중간 overflow 없음 조건에서 nearest rounding. unitScale의 unit 0..6 계수는 각각 `dpi*scale/1440`, `2540*scale/1440`, `1000*scale/1440`, `72*scale/1440`, `scale`, `72`, `4*dpi*scale/1440`; 그 외 `IllegalArgumentException`. dpi는 해당 축 원기기의 DPI. convertUnits는 target/source 계수를 scaleCoordinate에 전달하며 dpi와 documentScale을 추정하지 않는다.

## Paragraph placement

```java
public static int wrapStart(WmPwiDocument.Paragraph paragraph, int mode, int lineIndex, int frameLeft, int frameWidth)
public static int lineStart(WmPwiDocument.Paragraph paragraph, int mode, int lineIndex, int frameLeft, int frameWidth, int measuredWidth)
```

paragraph non-null·lineIndex≥0 필요; 위반은 IllegalArgumentException입니다. mode1은 level indent, 나머지는 left indent+bullet이면108+첫 줄 delta를 기본값으로 씁니다.

**wrapStart:** 기본값을0 이상으로 제한한 뒤 RIGHT가 아니면 frameWidth+frameLeft-72 상한을 적용하므로 좁은 프레임의 결과는 음수일 수 있습니다.

**lineStart:** mode1은 level indent로 시작하며 RIGHT/CENTER 계산을 건너뜁니다. 다른 mode의 RIGHT/CENTER는 실제 measuredWidth로 계산합니다. RIGHT가 아닐 때만 같은 상한 적용 후 마지막에0 이상으로 제한합니다. dirty saved width를 대신 쓰지 않으며 좌표·폭은 native 논리 단위입니다.

## Control and saved line advance

```java
public static int controlAdvance(int control, int currentX, int suppliedOrigin, int frameLeft)
public static int savedLineAdvance(WmPwiDocument.Paragraph paragraph, int lineIndex, int mode, int dpiX, int documentScale)
```

control 0..255 필요; 그 외 `IllegalArgumentException`. 0(CR)은 36, 4(TAB)는 suppliedOrigin(0이면 frameLeft) 기준 다음 170단위 정지점까지 이동, 그 외 0. savedLineAdvance는 mode 0/1·깨끗한 cache·존재하는 줄이면 저장 ascent+descent를 X축 coefficient로 unit0→unit5 변환한 값과 72 중 큰 값. 그 외는 72. 저장 cache를 새로 계산하거나 현재 값으로 만들지 않는다.

## positionLineBottoms

```java
public static int[] positionLineBottoms(int referenceLine, int referenceBottom, int[] advances)
```

referenceLine은 기준 다음 줄의0 기반 인덱스(0..advances.length)입니다. >0이면 결과[referenceLine-1]은 referenceBottom입니다. 아래는 각 advance를 빼고 위는 world-Y 규칙으로 더합니다.0은 모든 줄을 기준 아래에 놓고 배열 길이는 마지막 줄 anchor를 기준에 놓습니다.

`positionLineBottoms(1,500,new int[]{72,100,80})` 결과는 `{500,400,320}`입니다. native world-Y anchor이며 Android baseline이 아닙니다. advances는 non-null·각 값≥72, 위반은 IllegalArgumentException입니다. 반환 길이는 같고 입력은 바꾸지 않습니다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmPwiLayout.java`
