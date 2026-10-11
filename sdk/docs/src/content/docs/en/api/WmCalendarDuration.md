---
title: "WmCalendarDuration"
slug: en/api/wmcalendarduration
---
[API index](/en/api/)

```java
public final class WmCalendarDuration
```

Converts appointment-editor durations and CalendarProvider `DURATION`. Provider units represent fixed elapsed time, separate from civil-date arithmetic across DST.

### parseEditorMinutes

```java
public static int parseEditorMinutes(String value,Locale locale);
```

Parses locale-specific decimal text into minutes. Bare numbers mean hours. Accepts English aliases for minute/hour/day/week and Korean unit names. Caps at 94370400 minutes, then truncates fractional minutes. Throws `IllegalArgumentException` for null/empty input, more than 255 characters, negative values, unknown units, or a bare number producing zero minutes. Explicit-unit zero is allowed.

Grouping is disabled; the complete input must match a number followed by one of these units. Units are compared after Locale.US lowercasing, so English aliases are case-insensitive. Length is checked after trimming the input ends.

| Unit | Accepted suffixes |
| --- | --- |
| Minute | `min`, `mins`, `minute`, `minutes`, `minute(s)`, `m`, `분` |
| Hour | none, `hour`, `hours`, `hr`, `hrs`, `hour(s)`, `h`, `시간` |
| Day | `day`, `days`, `day(s)`, `d`, `일` |
| Week | `week`, `weeks`, `wk`, `wks`, `week(s)`, `w`, `주` |

Examples: `1.5` means90minutes in a decimal-point locale; `90m` means90minutes; `0min` means0minutes. Bare `0` fails.

### parseRfcMillis

```java
public static long parseRfcMillis(String value);
```

Returns milliseconds for optional `+` followed by `P<n>W` or day/hour/minute/second components. Rejects years/months, negatives, fractions, empty durations, a trailing empty `T`, and long overflow with `IllegalArgumentException`.

### parseProviderMillis

```java
public static long parseProviderMillis(String value);
```

Also reads the legacy Android seconds form `P<n>S`. Otherwise follows `parseRfcMillis`.

### timed

```java
public static String timed(long elapsedMillis);
```

Returns `PT<n>S` from elapsed milliseconds. Throws `IllegalArgumentException` for negative values or values not divisible by 1000; does not round.

### allDay

```java
public static String allDay(int days);
```

Returns `P<n>D` for a positive day count. Throws `IllegalArgumentException` for zero or negative days.

### eventEnd

```java
public static long eventEnd(long begin,Long explicitEnd,String duration);
```

Returns non-null `explicitEnd` without validating it or parsing duration. Otherwise returns `begin + parseProviderMillis(duration)`. Throws `IllegalArgumentException` for invalid duration or addition overflow.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmCalendarDuration.java`
