---
title: "WmReminder"
slug: en/api/wmreminder
---
[API index](/en/api/)

```java
public final class WmReminder
```

Immutable relative appointment reminder. The caller schedules notifications. Changing units preserves the quantity and changes total minutes.

### Constants and fields

```java
public static final int MINUTES = 0;
public static final int HOURS = 1;
public static final int DAYS = 2;
public static final int WEEKS = 3;
public static final int MAX_MINUTES = 9999 * 10080;
public final boolean enabled;
public final int quantity;
public final int unit;
```

`MINUTES=0`, `HOURS=1`, `DAYS=2`, `WEEKS=3`. `MAX_MINUTES=9999*10080` (100789920 minutes). `enabled` controls scheduling; `quantity` is 0..9999; `unit` selects the multiplier.

### Constructors

```java
public WmReminder(boolean enabled,int quantity,int unit);
```

Requires quantity 0..9999 and unit 0..3, even when disabled. Throws `IllegalArgumentException` outside these ranges.

### minutes

```java
public int minutes();
```

Returns quantity multiplied by 1/60/1440/10080 minutes. Still returns the configured duration when disabled.

### withEnabled

```java
public WmReminder withEnabled(boolean enabled);
```

Returns a new value preserving quantity and unit.

### withUnit

```java
public WmReminder withUnit(int unit);
```

Returns a new value preserving quantity and enabled state, not total minutes. Invalid units throw `IllegalArgumentException`.

### fromMinutes

```java
public static WmReminder fromMinutes(boolean enabled,int minutes);
```

Chooses the largest unit representing the minute count exactly. Zero becomes zero weeks. Negative or nonrepresentable four-digit quantities throw `IllegalArgumentException`.

### candidates

```java
public static String[] candidates(int unit);
```

Returns a new candidate array: minutes 1,5,10,15,30,45; hours/days 1..6; weeks 1..3. Throws `IllegalArgumentException` for other units.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmReminder.java`
