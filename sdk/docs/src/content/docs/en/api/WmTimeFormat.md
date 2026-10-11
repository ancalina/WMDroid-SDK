---
title: "WmTimeFormat"
slug: en/api/wmtimeformat
---
[API index](/en/api/)

```java
public final class WmTimeFormat
```

Builds Calendar details and minute-level time captions. The caller converts timestamps to the required civil date/time.

### calendarDetails

```java
public static String calendarDetails(String time,String location);
```

Returns `time` for an empty location, otherwise `time + " (" + location + ")"`. Does not trim or normalize the location. Null arguments throw `IllegalArgumentException`.

### minutePattern

```java
public static String minutePattern(Locale locale,boolean hour24);
```

Returns `HH:mm` for 24-hour time, `a h:mm` for Korean with KR or no country, and `h:mm a` for other 12-hour locales. Null locale throws `IllegalArgumentException`. Supply the Android 12/24-hour preference. Custom regional time patterns are not applied.

## Agenda

```java
public static final class Agenda
```

### Constructors

```java
public Agenda(Locale locale,boolean hour24,String multiday);
```

Reusable UI-thread formatter. `multiday` is a nonempty continuation caption; whitespace-only captions are accepted. Null/empty caption or null locale throws `IllegalArgumentException`. Do not call concurrently; the formatter shares mutable Calendar state.

### format

```java
public String format(WmDateTimeField.Value visibleDay,WmDateTimeField.Value start,WmDateTimeField.Value end);
```

Supply civil dates and minutes after caller-owned timezone conversion. visibleDay time fields are ignored. A later-date midnight end belongs to the previous civil day for endpoint comparison, while its displayed time remains actual midnight.

| Endpoints belonging to visibleDay | Caption |
| --- | --- |
| Both | `start-end` |
| Start only | `start-multiday` |
| End only | `multiday-end` |
| Neither | `multiday` |

Equal endpoints produce one time when they belong to visibleDay. For example, a24-hour event from October10 at10:00 to October11 at00:00 displays `10:00-00:00` on October10.

If the first literal English ` AM` exists, keep its preceding text, append `a`, and discard the remaining text. Otherwise apply the same rule to the first ` PM` with suffix `p`. Null values throw `IllegalArgumentException`. The caller owns endpoint order, duration, and timezone validation.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmTimeFormat.java`
