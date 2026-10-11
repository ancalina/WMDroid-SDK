---
title: "WmMetrics"
slug: en/api/wmmetrics
---
[API](/en/api/)

```java
public final class WmMetrics
```

Shared UI dimensions in logical pixels. Scale the complete surface once; do not apply `sp` or a second device scale to children.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmMetrics.java`

## Dimension constants

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

All values are SDK logical pixels before scaling. Apply text sizes with Android `COMPLEX_UNIT_PX`.

| Constants | Use |
| --- | --- |
| `WIDTH` | Reference width of the logical surface. |
| `BAR`, `TAB` | Title/command bar height and tab height. |
| `FONT`, `LINE` | Default text size and default line height. |
| `GAP`, `INSET` | Gap between lines and default inset. |
| `BORDER`, `EDGE` | Border thickness and control edge dimension. |
| `INDICATOR` | Shared checkbox/radio indicator width and height. |
| `TEXT_LEFT` | Left position of default Today text. |
| `ICON`, `ICON_INSET` | Today icon size and its left/top inset. |
| `TODAY_MIN` | Minimum Today item height. |
| `TODAY_FONT`, `TODAY_LINE` | Default Today text size and line height. |

## comboHeight

```java
public static int comboHeight(int textHeight);
```

Returns the single-line combo height `max(32,textHeight)+EDGE+2*BORDER`. `textHeight` is the font row height in logical pixels. Negative input also uses `32`, so the minimum result is `40`.

The intermediate sum uses `long`; the result is capped at `Integer.MAX_VALUE`. The owner must constrain the actual View layout to available parent space.

## todayHeight

```java
public static int todayHeight(int lines);
```

Returns `40` for `lines <= 1`; otherwise returns `lines*28+(lines-1)*2+16`. Negative and zero input also return `40`. The method does not validate the line count.

The calculation uses Java `int` arithmetic without overflow saturation. The owner must bound the line count. For Today items with a changed text size, use the size-specific height from [WmToday.TextStyle.height](/en/api/wmtoday/#textstyle). Build appointment subject/detail blocks with `WmToday.Row.setAppointments()`.
