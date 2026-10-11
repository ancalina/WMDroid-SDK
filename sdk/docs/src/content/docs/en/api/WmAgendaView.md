---
title: "WmAgendaView"
slug: en/api/wmagendaview
---
[API index](/en/api/)

```java
public final class WmAgendaView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

Displays one day as all-day rows, a read-only time strip, and sorted two-line timed rows. Supply day-clipped minutes and appointment captions; the owner queries data and opens cards. The optional native date footer is absent. Native GDI metrics and Android font rasterization are not verified as pixel-equivalent. Change View state on the UI thread; touch callbacks execute synchronously.

## Setup

### Constructors

```java
public WmAgendaView(Context context,Typeface font,Locale locale);
```

Requires non-null font/locale (`IllegalArgumentException`). Starts on the current date and reads the system 12/24-hour preference. The caller subsequently controls sorting locale.

### setOnEventListener

```java
public void setOnEventListener(OnEventListener listener);
```

Installs completed-row-tap callbacks; null removes them. Touch and callback order follow [OnEventListener](#oneventlistener). Programmatic selection does not notify.

### setDisplayIconMask

```java
public void setDisplayIconMask(int mask);
```

Replaces the timed-row symbol mask (0..63), cancels current row gestures, and redraws timed rows. Invalid masks throw `IllegalArgumentException` before changes. Retains Event metadata; the conflict indicator is separate from this mask.

### getDisplayIconMask

```java
public int getDisplayIconMask();
```

Returns the current symbol-display mask; default ALL (63).

## Events and date

### setEvents

```java
public void setEvents(List<Event> values);
```

Copies a non-null list after checking every entry; a null list/entry throws `IllegalArgumentException` before replacing records. Retains Event/tag objects. Timed records sort by original start epoch when supplied, otherwise displayed-date midnight plus startMinute*60000; equal starts use the current Locale Collator on title. All-day order stays unchanged.

Clears selection and row gestures, requests both scroll positions at zero, resets strip phase to 1, and recalculates conflicts/elapsed refresh. Does not notify listeners or immediately move the strip offset. Repeated Event references are accepted; they share selection identity and `rowBounds` resolves the first occurrence. The strip has no appointment touch targets; its all-day top bar uses the last all-day record's busyColor.

### selectEvent

```java
public boolean selectEvent(Event event);
```

Selects an Event instance retained by the latest `setEvents`. Null clears selection and returns true. An unregistered instance returns false without changing state. Successful calls cancel row gestures and redraw without callbacks. Non-null selection requests focus on its list and scrolls the row into view; if the row is taller than the viewport, it aligns the row top. Actual scroll position remains viewport-limited. Timed selection also chooses a strip phase and starts panning only when attached with a positive strip width. Clearing selection does not stop an existing strip pan.

### setDate

```java
public void setDate(int year,int month,int date);
```

Validates a real Gregorian date with one-based month before changing state; invalid dates throw `IllegalArgumentException`. Every valid call cancels row gestures and reschedules elapsed refresh. A changed date clears records, conflicts, and selection, requests both scroll positions at zero, and resets strip phase to 1; call `setEvents` afterward. The same date retains those states. Neither path notifies listeners.

### date

```java
public int[] date();
```

Returns a new `[year,month,day]` array with one-based month.

## Language and display

### setLocale

```java
public void setLocale(Locale locale);
```

Null throws `IllegalArgumentException` before changes. Updates catalog captions, re-sorts timed records with the new Locale, and recalculates conflicts. Cancels row gestures, retains the selected Event, and restores each list's requested fractional row position, rounded to content pixels. Does not preserve the identity of the row at that scroll position or rewrite Event captions.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

Updates catalog captions only; retains explicit Locale, caller event text, selection, and scroll.

### setShowBusyInDescription

```java
public void setShowBusyInDescription(boolean enabled);
```

Defaults false: omits the BUSY prefix before timed titles; true includes it. FREE/TENTATIVE/OUT_OF_OFFICE prefixes remain present. Applies only to timed records with an original start epoch. The assembled prefix/title is capped at 259 UTF-16 units without splitting a surrogate pair, then ellipsized to row width. Legacy records and all-day titles remain unchanged.

### set24Hour

```java
public void set24Hour(boolean enabled);
```

Changes strip labels and cancels row gestures. In 24-hour mode midnight is labeled 24; 12-hour mode uses 12. Event.time remains caller-formatted text.

### setColors

```java
public void setColors(int selection);
```

Sets the ARGB selection color, derives face with `WmPalette.face(selection)`, and restyles both scroll trees. Strip records with busyColor=0 use the new selection color; nonzero colors remain. Cancels row gestures without clearing selection or notifying listeners.

## Row and strip geometry

### getRowHeight

```java
public int getRowHeight();
```

Returns `2*(line+WmMetrics.EDGE)+WmMetrics.EDGE` logical pixels, where line is `ceil(descent-ascent)+WmMetrics.BORDER` for the current regular font at `WmMetrics.FONT`. This is the two-line timed-row spacing.

### getAllDayRowHeight

```java
public int getAllDayRowHeight();
```

Returns `line+2*WmMetrics.EDGE` logical pixels, using the same line metric as [getRowHeight](#getrowheight).

### getAllDayViewportHeight

```java
public int getAllDayViewportHeight();
```

Returns zero when no all-day records exist, otherwise up to six row heights plus 2 logical pixels. Additional rows scroll within that viewport.

### getStripHeight

```java
public int getStripHeight();
```

Returns the fixed strip height, 38 logical pixels.

### getStripBackingWidth

```java
public int getStripBackingWidth();
```

Returns floor(current strip width*2.4), possibly zero before layout.

### getStripOffset

```java
public int getStripOffset();
```

Returns the current offset in logical pixels; drawing translates the strip by its negative. Record selection can animate toward a target rather than placing this offset immediately.

### setStripPhase

```java
public void setStripPhase(int phase);
```

Accepts phase 0/1/2; other values throw `IllegalArgumentException` before changing state. Cancels row gestures, stops panning, and immediately applies the offset below in logical pixels.

| phase | offset |
| --- | --- |
| 0 | `0` |
| 1 | `8 * step` |
| 2 | `14 * step` |

`step = floor(backingWidth / 24.5)`, where `backingWidth = floor(current strip width * 2.4)`. A zero-width strip defers the offset until the next positive-width layout. Does not select records or notify listeners.

Timed-row DOWN, completed taps, and `selectEvent` instead choose a phase from start/duration and the previous phase. Their pan requests use 1 ms callbacks, taking distance/4 integer steps above four pixels; within four pixels the previous step is capped by the remaining distance. A zero previous step can stop short. Callback timing is Android scheduling, not measured WM hardware timing.

### rowBounds

```java
public Rect rowBounds(Event event);
```

Returns a new Rect in the Event's all-day/timed list before scrolling. Matches retained instance identity; null/unregistered records return null. Uses current row spacing and laid-out list width, which can be zero before layout. Repeated references return the first matching row.

### busyBounds

```java
public Rect busyBounds(Event event);
```

Returns a new Rect in the full strip backing surface before phase translation. Null/unregistered records return null. All-day records return `(0,0,backingWidth,4)`; timed records use y=4..32 and minute-proportional x coordinates with at least one pixel of width, including equal endpoints. Query after strip layout for usable horizontal geometry. These are drawing bounds; the strip does not select records by touch.

### scrollToRow

```java
public void scrollToRow(int index);
```

Requests index*rowHeight in the sorted timed list, with the final position limited by the viewport. Index is zero-based; an empty list accepts only zero. Other out-of-range indices throw `IllegalArgumentException` before a scroll request. Does not select, focus, or notify. [WmScrollView](/en/api/wmscrollview/#scrolltoposition) can defer the request until layout.

### hasConflict

```java
public boolean hasConflict(Event event);
```

Returns cached conflict membership by Event identity; null, unregistered, all-day, FREE, and legacy records without an original start epoch return false. Candidates use the original unclipped epoch interval. Any non-FREE all-day record marks timed candidates conflicted; otherwise strict start/end comparisons detect overlaps, so touching endpoints alone do not conflict. Zero-duration candidates are not filtered out. Repeated references use an identity-based scan that skips other occurrences of the same Event.

Recomputed when records are partitioned, not when time elapses. Past records retain the result but hide their row conflict icon/red time text; a selected strip bracket can still use conflict red.

## Styling and lifetime

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Null regular throws `IllegalArgumentException` before changes. Uses regular for list text at `WmMetrics.FONT` and strip text at 19 logical pixels; bold/tabColor are unused. Cancels row gestures, updates geometry and selection-derived face, and retains records/selection. Restores each list's requested fractional row position, rounded to content pixels. Native GDI metrics and the original strip face remain unverified.

### onAttachedToWindow

```java
protected void onAttachedToWindow();
```

After superclass attachment, updates elapsed refresh. It runs only while attached, window-visible, shown, and supplied with timed records that have epoch ends. Registers TIME_CHANGED/TIMEZONE_CHANGED reception even if all such records are already past; API 33+ uses RECEIVER_NOT_EXPORTED. Schedules the next non-past endpoint at end-now+1 ms, limiting each delay to Integer.MAX_VALUE+1 ms. The timer invalidates timed rows and reschedules; end equality remains current. Static boolean-past records require replacement by the owner for clock-dependent changes.

### onWindowVisibilityChanged

```java
protected void onWindowVisibilityChanged(int visibility);
```

After superclass dispatch, reevaluates the [elapsed refresh conditions](#onattachedtowindow). Hidden state removes that timer and unregisters clock reception. It does not stop strip panning or cancel row gestures.

### onVisibilityChanged

```java
protected void onVisibilityChanged(View changed,int visibility);
```

After superclass dispatch, reevaluates the same [elapsed refresh conditions](#onattachedtowindow) for self/ancestor visibility changes. Hidden state removes that timer and unregisters clock reception; it does not stop strip panning or cancel row gestures.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Removes elapsed/strip callbacks, unregisters clock reception, and cancels row gestures before superclass detachment. Retains records, selection, and current offsets. Reattachment restarts elapsed refresh under its visibility conditions; it does not restart the stopped strip pan.

## Event

```java
public static final class Event
```

### Constants and fields

```java
public static final int FREE = 0;
public static final int TENTATIVE = 1;
public static final int BUSY = 2;
public static final int OUT_OF_OFFICE = 3;
public final int busyStatus;
public final int startMinute;
public final int endMinute;
public final int busyColor;
public final boolean allDay;
public final String title;
public final String time;
public final String location;
public final Object tag;
public final int symbolMask;
public final boolean recurrenceException;
public final boolean past;
```

`startMinute/endMinute` are minutes clipped to the displayed civil day; end is exclusive and equal endpoints are allowed. `allDay` selects the all-day list. `title/time/location` remain caller captions; timed rows display title, then time with nonempty location in parentheses, ellipsized to width. `busyColor=0` follows selection color; any nonzero value is used directly for the strip. `tag` retains a nullable caller object.

`symbolMask` uses [WmCalendarSymbols](/en/api/wmcalendarsymbols/) bits, filtered by the display mask; recurrenceException selects the exception symbol when its recurrence bit is shown. Only timed rows show symbols. `past` is a snapshot or caller-supplied flag, not a live property; drawing uses [isPast](#ispast). Unselected elapsed timed text is gray and uses elapsed symbols; selection/pressed text is white. All-day text is not gray based on past. Busy status: FREE=0/TENTATIVE=1/BUSY=2/OUT_OF_OFFICE=3.

### Constructors

```java
public Event(int startMinute,int endMinute,boolean allDay,String title,String time,String location,int busyColor,Object tag);
public Event(int startMinute,int endMinute,boolean allDay,String title,String time,String location,int busyColor,Object tag,int symbolMask,boolean recurrenceException,boolean past);
public Event(int startMinute,int endMinute,boolean allDay,String title,String time,String location,int busyColor,Object tag,int symbolMask,boolean recurrenceException,long endTimeMillis);
public Event(int startMinute,int endMinute,boolean allDay,String title,String time,String location,int busyColor,Object tag,int symbolMask,boolean recurrenceException,long startTimeMillis,long endTimeMillis,int busyStatus);
```

Requires 0<=start<1440, start<=end<=1440, non-null title/time/location, mask 0..63, and busy status 0..3. When both original epoch endpoints are supplied, end>=start is required. Violations throw `IllegalArgumentException`. Epoch values and `isPast(now)` use milliseconds. Clip the minute range separately; the control does not derive minutes from epoch values.

| Overload ending | Elapsed state | Other defaults/metadata |
|---|---|---|
| busyColor, tag | Static past=false | symbolMask=0, recurrenceException=false, FREE; no original endpoints. |
| symbolMask, recurrenceException, boolean past | Static supplied past | Retains supplied symbols/exception; FREE; no original endpoints. |
| symbolMask, recurrenceException, long endTimeMillis | Live `endTimeMillis < now`; past snapshots construction time | Retains symbols/exception; FREE; no original start or conflict metadata. |
| long startTimeMillis, long endTimeMillis, int busyStatus | Live `endTimeMillis < now`; past snapshots construction time | Original endpoints drive sorting/conflicts; retains supplied busyStatus. |

### isPast

```java
public boolean isPast(long now);
```

Returns `endTimeMillis < now` when an epoch end exists; equality remains current. Otherwise returns the supplied/saved past flag. now is epoch milliseconds. Does not mutate the public past snapshot or trigger redraw.

## OnEventListener

```java
public interface OnEventListener
```

### onSelected

```java
void onSelected(Event event);
```

Receives the retained Event after a completed same-row UP within touch slop. DOWN shows pressed feedback, requests list focus, and can pan the timed strip, but does not commit selection. Moving beyond slop, leaving the row, CANCEL, multi-touch, size change, or detach cancels the tap; a canceled DOWN can leave the changed strip phase/pan. Selection is committed before this callback. Programmatic selection does not notify.

### onOpen

```java
void onOpen(Event event);
```

Runs immediately after onSelected on the same captured listener with the same Event. Changing/removing the listener or records inside onSelected does not redirect or suppress this call. If onSelected throws, the exception propagates and onOpen is not reached. The owner opens the card.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmAgendaView.java`
