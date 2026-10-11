---
title: "WmClockPanel"
slug: en/api/wmclockpanel
---
[API index](/en/api/)

```java
public class WmClockPanel extends LinearLayout implements WmControls.RestyleListener
```

Read-only popup body showing date, time, battery, and two appointment lines supplied by the owner. The owner controls popup chrome, dismissal, and timeout. No timer or calendar query. Dimensions and text sizes use logical pixels.

## Constructor

```java
public WmClockPanel(Context context, Typeface regular, String language)
```

Resolves language through WmStrings and reads current date, time, and battery. Body text is regular 24px with 8px padding. The initial appointment caption requests calendar integration; its time is empty.

## refreshClock / time

```java
public final void refreshClock()
protected final String time(long millis)
```

refreshClock rereads system time, time zone, 12/24-hour preference, and the sticky battery broadcast, then updates artwork and accessibility text. Appointment content is retained. time formats Unix epoch milliseconds in the current zone/language: HH:mm for 24-hour mode, a h:mm otherwise.

## setAppointment

```java
public final void setAppointment(String title, String when)
```

Displays title and appointment time as supplied. No date parsing or persistence. Null clears the corresponding TextView.

## language / text

```java
public final void language(String value)
protected final String text(String authored)
```

language resolves the language and immediately refreshes clock/battery labels. It does not translate existing appointment content. text resolves an SDK-authored string in the current catalog.

## batteryIcon

```java
public static String batteryIcon(int percent, boolean external, boolean charging)
```

Returns a String catalog key for `WmIcons.load(context, key)`. It is not an Android integer resource ID. External power yields battery_charge when charging, otherwise battery_full. On battery: 0..20 critical, 21..40 low, 41..60 half, 61..80 medium, 81..100 full, all prefixed battery_. Out-of-range percentages yield full.

## Style and attachment

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
protected void onAttachedToWindow()
```

Restyle calls refreshClock only when the resource revision changed; this implementation does not directly apply its font/color arguments. Attachment refreshes the clock. The owner must schedule repeated updates.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmClockPanel.java`
