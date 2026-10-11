---
title: "WmPalette"
slug: en/api/wmpalette
---
[API index](/en/api/)

```java
public final class WmPalette
```

Derives control highlight/face colors from a theme selection color and interpolates ARGB colors. All colors use Android ARGB ints.

## highlight

```java
public static int highlight(int selection);
```

Returns an opaque highlight for selected rows, fields, and check indicators. Sets input alpha to `255`; RGB `#149ca7` or `#15afbc` maps to `0xff37c7d3`. Other inputs retain their RGB.

## face

```java
public static int face(int selection);
```

Returns an opaque face color for controls such as buttons and scrollbars. Ignores input alpha.

RGB `#149ca7` or `#15afbc` returns the default face `0xffc9e7e9`. Other colors combine the input HSV hue with the default face saturation/value. Input saturation at or below `0.01` instead uses the default face's weighted brightness `222`, returning `0xffdedede`. Input brightness and saturation are not copied into the face color.

## mix

```java
public static int mix(int a,int b,int weightB);
```

Clamps `weightB` to `0..255`, then returns an ARGB int interpolated separately across alpha, red, green, and blue. Weight `0` returns `a`; `255` returns `b`. Out-of-range weights use the nearest endpoint.

Each channel uses integer arithmetic `(aChannel*(255−t)+bChannel*t+127)/255`. `t` is the clamped weight; `+127` rounds the result. Alpha is also interpolated independently, so the result is not always opaque.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmPalette.java`
