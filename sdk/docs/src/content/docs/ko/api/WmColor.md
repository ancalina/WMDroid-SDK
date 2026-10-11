---
title: "WmColor"
slug: ko/api/wmcolor
---
[API 색인](/ko/api/)

```java
public final class WmColor
public WmColor();
```

정수 HSB 색 변환과 테마 Bitmap 색조 변경. 색 입력은 Android ARGB int다. HSB의 hue 단위는 240척도이며 Android HSV의 도 단위와 다르다.

## hue

```java
public static int hue(int rgb);
```

RGB 채널에서 정수 hue `0..239`를 반환한다. alpha는 무시한다. R=G=B인 무채색은 `123`을 반환하며 이 값만으로 무채색 여부를 판단하지 않는다. RGB 채널을 `0..10000`으로 변환하여 정수 계산하므로 Android HSV hue와 반올림 결과가 다를 수 있다.

## rgb

```java
public static int rgb(int hue,int saturation,int brightness);
```

정수 HSB를 alpha `255`인 ARGB로 변환한다. hue는 `0..239`, saturation·brightness는 `0..240`을 사용한다. saturation `0`이면 무채색, brightness `0`이면 검은색이다. 채널 계산은 정수 나눗셈을 사용한다.

입력을 clamp하거나 hue를 순환시키지 않는다. hue `240`도 `0`으로 바뀌지 않는다. 범위 밖 값은 정상 HSB 색으로 해석하지 않는다.

## isGray

```java
public static boolean isGray(int color);
```

alpha를 제외한 RGB의 `max`·`min`을 구해 `max==0 || (max−min)*100<=max`이면 true를 반환한다. 완전한 무채색뿐 아니라 최대 채널 대비 차이가 1% 이하인 색도 포함한다. `tint`의 무채색 테마 분기에 사용한다.

## gray

```java
public static int gray(int color);
```

밝기 `v=(299*R+587*G+114*B)/1000`을 정수 계산하여 R·G·B 모두에 적용한 ARGB를 반환한다. 원래 alpha를 보존한다.

## tint

```java
public static android.graphics.Bitmap tint(android.graphics.Bitmap source,int base,int chosen);
```

동기 변환이다. 큰 Bitmap은 worker 스레드에서 처리한다. `base`와 `chosen`의 alpha는 무시한다. `isGray(chosen)`이 false이면 Android HSV hue 차이 `chosenHue−baseHue`를 각 픽셀에 적용하고 `0` 이상 `360` 미만으로 순환한다. 픽셀의 HSV saturation/value와 alpha는 유지한다. `isGray(chosen)`이 true이면 색조 대신 `gray` 변환을 적용한다.

alpha가 `0`인 픽셀과 R=G=B인 픽셀은 그대로 둔다. `isGray`에만 해당하는 근접 무채색 픽셀은 이 제외 조건에 포함하지 않는다.

무채색 테마가 아니고 hue 차이가 `0`이면 source를 읽지 않고 그대로 반환한다. 그 외에도 변경된 픽셀이 없으면 같은 source를 반환한다. 변경이 있으면 크기·density·hasAlpha를 보존한 새 ARGB_8888 Bitmap을 반환한다. 원본은 수정·recycle하지 않는다. source와 다른 새 반환 Bitmap은 호출자가 소유한다. source의 기존 소유권은 유지한다.

변환 경로에서는 읽을 수 있는 Bitmap이 필요하다. null source는 hue 차이 `0`의 빠른 반환에서는 null 그대로 반환하고, 그 외에는 `NullPointerException`이다. Bitmap 읽기·할당 실패는 호출자에게 전파한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmColor.java`
