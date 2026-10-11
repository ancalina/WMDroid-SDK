---
title: "WmCalendarSymbols"
slug: en/api/wmcalendarsymbols
---
[API index](/en/api/)

```java
public final class WmCalendarSymbols extends Drawable
```

Appointment-metadata Drawable. Distinguishes the six-cell Options atlas from the event atlas. Cells are32×28 logical pixels; removes the pack red transparency key. Missing packs use emoji fallback.

### Constants and fields

```java
public static final int REMINDER = 1;
public static final int RECURRENCE = 2;
public static final int NOTES = 4;
public static final int LOCATION = 8;
public static final int ATTENDEES = 16;
public static final int PRIVATE = 32;
public static final int ALL = 63;
```

REMINDER=1, RECURRENCE=2, NOTES=4, LOCATION=8, ATTENDEES=16, PRIVATE=32, ALL=63. Combine display metadata with bitwise OR.

### Constructors

```java
public WmCalendarSymbols(Context context,int index);
```

Takes an Options-atlas index0..5, not a bit value, in the constant order above. Null context/out-of-range index throws `IllegalArgumentException`.

### forEvent

```java
public static WmCalendarSymbols forEvent(Context context,int symbol,boolean recurrenceException);
public static WmCalendarSymbols forEvent(Context context,int symbol,boolean recurrenceException,boolean past);
```

Requires exactly one symbol bit. RECURRENCE supports exception presentation; past=true uses the elapsed atlas bank. Short overload defaults past=false. Invalid bit/null context throws `IllegalArgumentException`.

### draw

```java
public void draw(Canvas canvas);
```

Draws the cell into Drawable bounds and refreshes the atlas when pack identity changes. Context-scoped caches share the atlas; callers must not recycle it.

### getIntrinsicWidth

```java
public int getIntrinsicWidth();
```

Returns intrinsic width 32; output size follows Drawable bounds.

### getIntrinsicHeight

```java
public int getIntrinsicHeight();
```

Returns intrinsic height 28; output size follows Drawable bounds.

### setAlpha

```java
public void setAlpha(int alpha);
```

Applies Paint alpha and invokes invalidateSelf.

### setColorFilter

```java
public void setColorFilter(ColorFilter filter);
```

Replaces Paint ColorFilter and invokes invalidateSelf. Null clears the filter.

### getOpacity

```java
public int getOpacity();
```

Always returns PixelFormat.TRANSLUCENT.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmCalendarSymbols.java`
