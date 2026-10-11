---
title: "WmLunarCalendar"
slug: en/api/wmlunarcalendar
---
[API](/en/api/)

Converts Gregorian civil dates to Korean lunar dates using data from a resource pack. The SDK includes no year table and does not substitute a Chinese table.

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

## Loading and lifetime

Call `load(pack)` off the UI thread. Null pack or missing `RESOURCE_ID` returns null. Invalid data and I/O failures throw `IOException`. The method opens and closes its own stream and does not change the active pack. The immutable result remains valid after the selected pack changes.

The official extractor supplies the original Korean table only through a user-provided Korean WM ROM pack. WM input is optional; packs without this resource remain valid.

## Civil dates and captions

`convert(year, month, day)` uses Gregorian civil values: January is month 1; day starts at 1. Device timezone does not affect conversion. Invalid Gregorian dates and dates outside the loaded table return null. The upper date boundary depends on the table's month lengths.

`Date.year` is the lunar year. `month` is 1–12; `day` starts at 1. `leapMonth` distinguishes an inserted leap month from the regular month with the same number. `monthLength` is 29 or 30. All five fields are final; callers cannot construct a `Date`.

`caption(null)` returns null. Day 1 uses the regular or leap month caption. Days 2–30 use the corresponding day caption.

## Pack data contract

The logical ID is `assets/calendar/lunar/korean.json`; the official ZIP path is `calendar/lunar/korean.json`. Loading requires UTF-8 JSON of at most 32768 bytes, one JSON object, and no trailing non-whitespace content.

| Field | Required value |
| --- | --- |
| `version` | Integer `1` |
| `epoch` | String `1900-01-31` |
| `first_year` | Integer `1900` |
| `years` | 150 integers for lunar years 1900–2049 |
| `captions.days` | 29 strings for days 2–30 |
| `captions.months` | 12 strings for regular months 1–12 |
| `captions.leap_months` | 12 strings for leap months 1–12 |

Each year word is 0–`0x1ffff`. Its low four bits specify the month after which the leap month is inserted: 0 means none; 1–12 are valid. Bits 15–4 specify regular month 1–12 lengths; bit 16 specifies leap-month length. Each length is 29 plus its bit value.

Each caption must contain non-whitespace text and at most 64 UTF-16 code units. ISO control characters and unpaired surrogates are rejected. Valid surrogate pairs are allowed. Floating-point values are not accepted for integer fields.

## Example

Pass an existing pack snapshot. Run this method off the UI thread; propagate or handle `IOException` at the caller.

```java
static String lunarCaption(WmResourcePack pack, int year, int month, int day)
        throws IOException {
    WmLunarCalendar lunar = WmLunarCalendar.load(pack);
    return lunar == null ? null : lunar.caption(lunar.convert(year, month, day));
}
```
