---
title: "WmRecurrenceRule"
slug: en/api/wmrecurrencerule
---
[API index](/en/api/)

```java
public final class WmRecurrenceRule
```

Immutable recurrence pattern and end range. Serializes newly authored RRULEs; does not parse existing rules or expand occurrences.

### Constants and fields

```java
public static final int NONE = 0;
public static final int DAILY = 1;
public static final int WEEKLY = 2;
public static final int MONTHLY_DAY = 3;
public static final int MONTHLY_ORDINAL = 4;
public static final int YEARLY_DAY = 5;
public static final int YEARLY_ORDINAL = 6;
public static final int SUNDAY = 1;
public static final int MONDAY = 2;
public static final int TUESDAY = 4;
public static final int WEDNESDAY = 8;
public static final int THURSDAY = 16;
public static final int FRIDAY = 32;
public static final int SATURDAY = 64;
public static final int ALL = 127;
public static final int WEEKDAYS = 62;
public static final int WEEKEND = 65;
public static final int NEVER = 0;
public static final int COUNT = 1;
public static final int UNTIL = 2;
public final int pattern;
public final int interval;
public final int dayMask;
public final int day;
public final int month;
public final int ordinal;
public final int endMode;
public final int count;
```

Patterns: NONE=0, DAILY=1, WEEKLY=2, MONTHLY_DAY=3, MONTHLY_ORDINAL=4, YEARLY_DAY=5, YEARLY_ORDINAL=6. Weekday bits run Sunday=1 through Saturday=64; ALL=127, WEEKDAYS=62, WEEKEND=65. End modes: NEVER=0, COUNT=1, UNTIL=2. Fields store interval, weekday mask, one-based day/month, ordinal, and COUNT count. The UNTIL date itself is not stored.

### minimumPeriodMinutes

```java
public long minimumPeriodMinutes();
```

Returns the appointment-duration/reminder validation bound in minutes, not the shortest interval obtained by expanding occurrences.

| Pattern | Return value (minutes) |
| --- | --- |
| NONE | `0` |
| DAILY | `interval * 1440` |
| DAILY, weekdays only | `1440` |
| WEEKLY | Selected-day validation gap (days) × `1440` |
| MONTHLY_DAY / MONTHLY_ORDINAL | `interval * 28 * 1440` |
| YEARLY_DAY / YEARLY_ORDINAL | `12 * 28 * 1440` |

The weekly gap starts at `7 * interval` days. Scan two Sunday-based weeks when `interval == 1`, otherwise one week, and use any smaller gap between different selected weekdays. NONE returns `0` to disable the duration upper bound.

### validateAppointmentDuration

```java
public void validateAppointmentDuration(long civilMillis);
```

`civilMillis` is the duration in milliseconds between displayed year/month/day/hour/minute values, excluding changes in timezone offset. Negative values throw `IllegalArgumentException` for every pattern. Recurring rules reject values above `minimumPeriodMinutes() * 60000`; equality is allowed. NONE accepts any nonnegative duration without this upper-bound check. Do not pass elapsed milliseconds across a DST transition unchanged.

### validateAppointmentReminder

```java
public void validateAppointmentReminder(boolean enabled,long minutes);
```

Returns immediately when disabled. Enabled negative minutes or recurring reminders at or above the period bound throw `IllegalArgumentException`. Does not add appointment duration to the lead time.

### none

```java
public static WmRecurrenceRule none();
```

Returns a nonrecurring value, which serializes to null.

### daily

```java
public static WmRecurrenceRule daily(int interval,boolean weekdays);
```

Positive daily interval. `weekdays=true` requires interval 1 and selects Monday–Friday. Invalid intervals throw `IllegalArgumentException`.

### weekly

```java
public static WmRecurrenceRule weekly(int interval,int dayMask);
```

Requires a positive weekly interval and a nonempty seven-bit weekday mask (1..127). Invalid values throw `IllegalArgumentException`.

### monthlyDay

```java
public static WmRecurrenceRule monthlyDay(int interval,int day);
```

Requires a positive monthly interval and day 1..31. The provider determines short-month expansion. Invalid values throw `IllegalArgumentException`.

### monthlyOrdinal

```java
public static WmRecurrenceRule monthlyOrdinal(int interval,int ordinal,int dayMask);
```

Requires a positive monthly interval, ordinal -5..-1 or 1..5, and weekday mask 1..127. Negative ordinals count from the end. Invalid values throw `IllegalArgumentException`.

### yearlyDay

```java
public static WmRecurrenceRule yearlyDay(int month,int day);
```

Requires month 1..12 and a possible day in that month. Accepts February 29 using a leap-year reference. Impossible month/day values throw `IllegalArgumentException`.

### yearlyOrdinal

```java
public static WmRecurrenceRule yearlyOrdinal(int month,int ordinal,int dayMask);
```

Requires month 1..12, ordinal -5..-1 or 1..5, and weekday mask 1..127. Invalid values throw `IllegalArgumentException`.

### withCount

```java
public WmRecurrenceRule withCount(int count);
```

Returns a new count-limited rule. NONE or nonpositive counts throw `IllegalArgumentException`.

### withUntil

```java
public WmRecurrenceRule withUntil();
```

Returns a rule with UNTIL end mode. Supply the date/UTC conversion when serializing. NONE throws `IllegalArgumentException`.

### withoutEnd

```java
public WmRecurrenceRule withoutEnd();
```

Returns a NEVER rule, or the same instance if already NEVER.

### toRrule

```java
public String toRrule(String untilUtc);
```

Returns a timed-event RRULE without the `RRULE:` prefix. UNTIL requires a caller-converted 16-character UTC timestamp, `yyyyMMddTHHmmssZ`, such as `20261011T090000Z`. A date-only value such as `20261011` is rejected.

### toAllDayRrule

```java
public String toAllDayRrule(String untilDate);
```

Returns an all-day-event RRULE without the `RRULE:` prefix. UNTIL requires an 8-character date, `yyyyMMdd`, such as `20261011`. A time or `Z` suffix is rejected.

Both serializers require a real Gregorian date (years 0001..9999) and ASCII digits, and reject leap seconds. Pass null for every non-UNTIL end mode, including NONE; NONE then returns null. Invalid arguments throw `IllegalArgumentException`. Multi-day ordinal rules use BYSETPOS; a single weekday uses ordinal BYDAY. Validate storage compatibility: API14 provider behavior differs for YEARLY BYSETPOS and weekly anchors.

### equals

```java
public boolean equals(Object other);
```

Uses all pattern, interval, weekday, date, ordinal, end-mode, and count fields for value equality.

### hashCode

```java
public int hashCode();
```

Hashes all pattern, interval, weekday, date, ordinal, end-mode, and count fields; equal rules have equal hashes.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmRecurrenceRule.java`
