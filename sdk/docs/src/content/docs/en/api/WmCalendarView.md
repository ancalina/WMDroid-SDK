---
title: "WmCalendarView"
slug: en/api/wmcalendarview
---
[API index](/en/api/)

```java
public final class WmCalendarView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

Displays day/5-day/6-day/week grids; multi-column modes include selection details. Supply records clipped to each civil day and open appointment cards from callbacks. Provider queries and drag editing are not implemented. Uses logical VGA coordinates; GDI font metrics, category colors, and overflow behavior remain partial. Change View state on the UI thread; callbacks run synchronously in touch/key dispatch.

## Setup

### Constants and fields

```java
public static final int DAY = 1;
public static final int WEEK = 7;
```

DAY=1 and WEEK=7 specify date-column counts.

### Constructors

```java
public WmCalendarView(Context context,Typeface font,Locale locale);
```

Requires non-null font/locale (`IllegalArgumentException`). Starts in DAY with 60-minute rows and system 12/24-hour mode. Default working hours: Monday–Friday 08:00..17:00.

### setRange

```java
public void setRange(int year,int month,int day,int days);
```

Sets a real start date and 1/5/6/7 consecutive columns; 5/6 columns do not skip weekends. Month is one-based. Invalid dates/counts throw `IllegalArgumentException` before the range changes. Clears records, appointment/time selection, keyboard anchor, and gestures; requests both scroll offsets at zero. Call `setEvents` after this call. Does not notify listeners.

## Event and time selection

### setEvents

```java
public void setEvents(List<Event> values);
```

Copies the list after validation, retaining Event/tag objects and input order. Null lists/entries or Event.day outside the current range throw `IllegalArgumentException` without replacing the records. Clears appointment/time selection, keyboard anchor, and gestures without callbacks. Rebuilds geometry without resetting requested scroll positions.

In DAY, all-day records form a separate scrollable header list. In multi-column modes, each day gets one full-height all-day busy stripe, not individual all-day rows. Its status is the highest priority present: BUSY > OUT_OF_OFFICE > TENTATIVE > FREE. This aggregate does not provide individual all-day touch targets.

### selectEvent

```java
public boolean selectEvent(Event event);
```

Selects only the same Event instance retained by the latest setEvents call. Null clears appointment/time selection and returns true. An unregistered instance returns false without changing state. Successful calls cancel current touch selection, clear the time-selection keyboard anchor, and redraw without event/time callbacks. A non-null selection requests focus, including in touch mode, and sets the keyboard row from startMinute/current interval. Timed selection scrolls that starting row into view. In DAY, all-day selection scrolls the header list to reveal its all-day row. In multi-column mode, all-day selection draws a 10-logical-pixel-wide frame at the selected column’s left edge, using the actual scroll position and viewport height. Let top=scroll.getScrollPosition(), height=scroll.getHeight(), and rows=height/rowHeight with integer division. If top/rowHeight+rows>=24, frame height is rows*rowHeight; otherwise it is height. The literal24 threshold remains the same for both row intervals. Scroll/layout changes redraw this viewport frame. It adds no individual all-day touch targets.

### setOnEventListener

```java
public void setOnEventListener(OnEventListener listener);
```

Installs synchronous appointment-selection/open callbacks; null removes them. See [OnEventListener](#oneventlistener) for touch and keyboard triggers. Programmatic selection does not notify.

### setOnTimeListener

```java
public void setOnTimeListener(OnTimeListener listener);
```

Installs synchronous callbacks for completed empty-slot taps and keyboard time selection; null removes them. `setTimeSelection`, `selectEvent`, and range/record resets do not notify.

### getTimeSelection

```java
public int[] getTimeSelection();
```

Returns a new `[day,startMinute,endMinute]` array, or null for appointment/no selection.

### setTimeSelection

```java
public void setTimeSelection(int day,int startMinute,int endMinute);
```

Requires a current column, 0<=start<end<=1440, and endpoints aligned to the current interval. Invalid selection throws `IllegalArgumentException` before state changes. Clears appointment selection and gestures; sets the keyboard anchor at the first selected row and cursor at the last. Does not request focus, scroll, or notify listeners.

## Week navigation and cursor state

### setOnNavigateListener

```java
public void setOnNavigateListener(OnNavigateListener listener);
```

Installs a date-navigation callback; null removes it. In DAY, if a timed appointment is selected, the control extends its text/busy bounds by 10 logical pixels on the requested side and selects the first intersecting timed record in sorted placement order. Otherwise it calls `onNavigate(-1/+1)` without changing the date. Tags do not determine adjacency. Multi-column range exhaustion calls the same listener. Capture WeekNavigation before replacing the range; the owner moves the first date by direction*7, loads records, then restores the snapshot.

### selectInitialWeek

```java
public void selectInitialWeek(int preferredColumn, long nowMillis);
```

Apply after loading records for first Week opening or view activation. `preferredColumn` must be 0..getDayCount()-1. DAY throws IllegalStateException; an invalid column throws IllegalArgumentException before selection changes. `nowMillis` is an epoch timestamp converted to the view's captured time zone.

Find the first occupied column at or after preferredColumn. If none exists, find the nearest occupied earlier column. From that starting column, scan forward. In each column, prefer timed records, then the first all-day record. Only preferredColumn uses current-time selection: first civil start at or after now, or last passed timed record. Other columns use the first timed record. Source starts have minute precision; now retains seconds/milliseconds. Equal minute with nonzero seconds/milliseconds is passed. Ordering uses local civil time, not UTC epoch order.

A record selection retains raw cursor rows and synchronously calls onSelected once. If no record exists, restore raw cursor rows in preferredColumn, clamp to the current interval, scroll the end row into view, and call the time listener once. Never calls onOpen. Owners must give an explicit pending record precedence and must not call this for ordinary refresh or boundary navigation.

### getSelectedEvent / getWeekRemovalSuccessor

```java
public Event getSelectedEvent();
public Event getWeekRemovalSuccessor();
```

getSelectedEvent returns the supplied record object currently selected, or null for a time cursor or no selection. setEvents clears selection.

Call getWeekRemovalSuccessor before deleting the selected Week record. Returns the next record in the same column, otherwise the previous record, otherwise null. A time cursor or no selection returns null. DAY throws IllegalStateException. The column order is all-day before timed, then civil source start ascending, busy status descending and stable input order for ties. It does not change selection, scroll or callbacks.

Keep the returned identity across the data reload. Match it to a new Event and call selectEvent before automatic population fallback. The returned Event still refers to the old data set; it does not become a record in a new setEvents list automatically. The SDK does not delete provider data.

### selectPopulatedWeek

```java
public void selectPopulatedWeek(int preferredColumn);
```

Automatic selection after setEvents replaces multi-column records when no explicit target exists. Match a specific record to its new Event instance and use selectEvent first. Uses selectInitialWeek’s occupied-column search and timed-before-all-day fallback, but chooses the first timed event without current-time filtering. With no events it restores stored time rows; one event or time selection callback results. preferredColumn is0..getDayCount()-1; DAY throws IllegalStateException and an invalid column throws IllegalArgumentException before changes.

| Situation | Sequence |
| --- | --- |
| Initial display/view activation | setEvents→selectInitialWeek with current time |
| Record replacement without explicit target | setEvents→selectPopulatedWeek |
| Week boundary navigation | Capture old WeekNavigation→new range/records→restoreWeekNavigation |

Do not auto-select for appearance updates or edits retaining selection. Capture getWeekRemovalSuccessor identifiers before deletion, match refreshed records, and optionally apply this policy if none matches. The owner decides data loading/application timing.

### getTimeCursorRows / setTimeCursorRows

```java
public int[] getTimeCursorRows();
public void setTimeCursorRows(int startRow, int endRow);
```

`getTimeCursorRows` returns a new array with the last inclusive start/end time rows. Appointment selection does not replace them. `setTimeCursorRows` restores only this owner state; it does not change visible selection, scroll, focus, or emit callbacks. Valid rows: 0 <= startRow <= endRow < 48. Invalid rows throw IllegalArgumentException before mutation.

Rows remain raw when the interval changes without an active time selection. captureWeekNavigation clamps them to the current row count. Save the returned rows when the owner closes; restore them before boundary capture. Do not convert them to civil minutes.

### captureWeekNavigation / restoreWeekNavigation

```java
public WeekNavigation captureWeekNavigation(int direction);
public void restoreWeekNavigation(WeekNavigation navigation);
public static final class WeekNavigation {
    public final int direction, startRow, endRow;
    public final boolean preferAllDay;
}
```

Capture before the owner replaces the range or records. direction must be -1 or 1; otherwise throws IllegalArgumentException. Both methods require 5/6/7-column mode; DAY throws IllegalStateException. A null restore argument throws IllegalArgumentException. The snapshot has no public constructor.

preferAllDay records whether an all-day appointment is selected. startRow/endRow are the inclusive last time-cursor rows, including when an appointment is selected. With no previous cursor, both start at the local hour captured at view construction, as raw row indices even in 30-minute mode. The snapshot contains no recordId or old appointment start time.

After setRange/setEvents, restore scans from the first column for Right, or the last column for Left. In each column, prefers the captured appointment class and falls back to the other class. Timed records use the first/last civil-sorted placement; all-day records use the first input record for either direction. Timed selection scrolls into view; any appointment selection synchronously calls onSelected once; onOpen is not called. No records restores the cursor range in the boundary column and calls the time listener once. Rows clamp to the new interval's row count; row indices, not civil minutes, survive an interval change. The method does not load data, move dates, compare old record IDs, or roll back owner query failures.

## Date, display, and language

### setOnDateListener

```java
public void setOnDateListener(OnDateListener listener);
```

Installs date-header callbacks for 5/6/7-column modes; null removes them. Setting the listener cancels current header touch. A single-pointer tap must finish within touch slop in the original column; movement into another column cancels confirmation. Commits on UP rather than native DOWN. Does not change the range or appointment/time selection.

### setDisplayIconMask

```java
public void setDisplayIconMask(int mask);
```

Replaces the symbol-display mask (0..63) and cancels gestures. Invalid masks throw IllegalArgumentException; retains event metadata.

### getDisplayIconMask

```java
public int getDisplayIconMask();
```

Returns the current symbol-display mask; default ALL (63).

### getDayCount

```java
public int getDayCount();
```

Returns the current 1/5/6/7 column count.

### setWorkingHours

```java
public void setWorkingHours(int dayMask,int startMinute,int endMinute);
```

Requires dayMask 1..127 (Sunday bit0), 0<=start<end<1440, and 30-minute-aligned endpoints; end is exclusive. Invalid values throw `IllegalArgumentException`. Updates working-hour shading.

### dateHeaderBounds

```java
public RectF dateHeaderBounds(int column);
```

Returns a copied pinned-header RectF in this View coordinates, or null in DAY. Bounds use the current laid-out timed-grid width and exclude its time-axis column; query after layout for usable horizontal coordinates. Out-of-range columns throw `IllegalArgumentException`.

### setInterval

```java
public void setInterval(int minutes);
```

Accepts only 30/60-minute rows (`IllegalArgumentException`). The current value is a no-op. Otherwise cancels gestures and preserves requested civil-minute scroll position. Existing time selection becomes one slot whose start is rounded down to the new interval; resets its keyboard anchor. Appointment selection remains. Does not notify listeners.

### setLocale

```java
public void setLocale(Locale locale);
```

Changes locale and empty-selection captions; Event.title/detail remain caller data. Null throws `IllegalArgumentException`.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

Updates catalog captions only; retains explicit Locale, caller event text, selection, and scroll.

### set24Hour

```java
public void set24Hour(boolean enabled);
```

Changes 12/24-hour time-axis labels only.

### setShowWeekText

```java
public void setShowWeekText(boolean enabled);
```

False (default) draws only busy blocks in 5/6/7-column modes; true draws title/symbol boxes. DAY always draws title/symbol boxes. Rebuilds placement geometry and cancels gestures. Busy-only bounds follow actual minute fractions, so changing this option can change `eventBounds` and hit targets.

### setColors

```java
public void setColors(int selection,int face,int eventColor);
```

Sets ARGB selection, face, and appointment-box colors. Native fixed work-hour/busy-status colors are separate.

## Geometry and scrolling

### getRowHeight

```java
public int getRowHeight();
```

Returns `max(ceil(descent-ascent),28)+6` logical pixels, measured with the current font at `WmMetrics.FONT`. This is the height of one 30/60-minute row; it uses Android font metrics, not measured native GDI metrics.

### getTimeColumnWidth

```java
public int getTimeColumnWidth();
```

Returns `ceil(WmFonts.measure(paint,"12"))+6` logical pixels with the current font at `WmMetrics.FONT`.

### getHeaderHeight

```java
public int getHeaderHeight();
```

Returns 36 logical pixels for 5/6/7-column modes. DAY returns `min(allDayCount,6) * listSpacing`, or zero when empty. `listSpacing` is `ceil(descent-ascent)+WmMetrics.BORDER+2*WmMetrics.EDGE`; additional all-day rows remain accessible by header scrolling.

### getContentHeight

```java
public int getContentHeight();
```

Returns full 24-hour scroll-content height in logical pixels, excluding header/details.

### dateAt

```java
public int[] dateAt(int column);
```

Returns a new `[year,month,day]` for a zero-based column; month is one-based. Out-of-range columns throw `IllegalArgumentException`.

### scrollToMinute

```java
public void scrollToMinute(int minute);
public void scrollToMinute(float minute);
```

Scrolls to civil minutes clamped to0..1440. The float overload accepts fractional minutes; the scroll request rounds to the nearest content pixel. NaN/Infinity throw `IllegalArgumentException`. The final position is limited by the viewport.

### getScrollMinute

```java
public float getScrollMinute();
```

Returns requested content scroll position converted to civil minutes.

### eventBounds

```java
public RectF eventBounds(Event event);
```

Returns a copied timed-event RectF in grid-content coordinates, excluding the pinned header and scroll offset. Matches object identity; null, absent, and all-day records return null. Placement uses the current grid width, so query after layout. Overlapping records share horizontal lanes. The return value is the placement rectangle, not its enclosing union with the busy rectangle used for touch testing below. In multi-column busy-only mode, bounds cover the busy block rather than a title box.

## Styling, input, and lifetime

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Uses regular as the new font; bold/tabColor do not affect this control. Replaces selection color and derives face with `WmPalette.face(selection)`; retains eventColor. Cancels gestures and rebuilds geometry. Preserves requested civil-minute scroll position and, in DAY, the fractional all-day header row position. Does not notify listeners.

### setEnabled

```java
public void setEnabled(boolean enabled);
```

Updates grid/header/both scroll views together and cancels gestures; retains records and selection. Disabled controls reject touch dispatch and the row keys handled below; unhandled key paths still delegate to Android.

### dispatchTouchEvent

```java
public boolean dispatchTouchEvent(MotionEvent event);
```

Returns false while disabled; otherwise delegates to the child touch paths. Timed appointment hit testing uses the enclosing rectangle of its text bounds and busy bounds, with right/bottom edges excluded. A busy rectangle with zero/negative width or height does not expand the hit area. If targets overlap, the first timed placement for that day wins: All modes use the civil-start/busy order described below. Appointment DOWN selects synchronously; a same-record UP within touch slop opens it. Empty-grid DOWN selects one time slot, but its callback requires UP in the same column/row within touch slop. Movement beyond slop, CANCEL, or multiple pointers cancels confirmation; the selection already made on DOWN remains. Child detachment/size changes cancel their pending gestures.

### dispatchKeyEvent

```java
public boolean dispatchKeyEvent(KeyEvent event);
```

Handles the following keys without Alt/Ctrl. DAY Left/Right and time-row keys consume UP without changing selection and apply the action on DOWN. Multi-column Left/Right consume a DOWN that selects an appointment or calls the owner navigation listener, and its matching UP. Disabled controls reject these keys; other keys/modifiers use superclass dispatch.

| Keys | Result |
|---|---|
| DAY Left/Right | Selects an intersecting timed appointment or calls the owner navigation callback. Shift does not extend time selection. |
| Multi-column Left/Right | A selected timed appointment first searches its same-column text/busy bounds extended by 8 logical pixels on the requested side. Without a match, searches other columns in that direction. An appointment choice calls the selection listener once on DOWN; the matching UP is consumed. Shift does not extend time selection. Without a candidate anywhere in the range, calls the owner navigation listener; without a listener, delegates to the superclass. |
| DAY Up/Down | Moves through timed appointments and uncovered time rows in start-row order; excludes all-day records. Overlapping appointments each form a selectable entry in source civil-start/busy order. Shift does not extend selection. |
| Multi-column Up/Down | Selects the previous/next appointment in the current column; wraps at either end. Shift does not extend time selection. |
| PageUp/PageDown | Moves by `max(1,viewportHeight/rowHeight)` rows. |
| Home/End | Moves to the first/last time row. |

#### Time-row selection

Time-row movement clamps to 0..1440/interval-1, uses the current time/appointment column (otherwise column 0), scrolls the cursor into view, and synchronously notifies the time listener. Shift retains the anchor and selects an inclusive row range, returned as an exclusive end minute; without Shift it selects one row. If no anchor exists, Shift starts the range at the destination row.

#### DAY Up/Down

DAY Up/Down traverses sorted timed placements and uncovered time rows. Overlapping appointments use source civil start ascending, then busyStatus descending; equal keys retain input order. Selects an appointment with `onSelected`, or one empty row with the time callback. At a DAY list boundary, selection stays; a partly visible appointment can scroll another row into view.

#### Multi-column Up/Down

Uses the selected appointment column, otherwise the time-selection column, otherwise column 0. Orders all-day records first, then source civil start ascending and busyStatus descending; equal keys retain input order. With no appointment selected, compares each source start hour with the first selected raw row (`timeStart/interval`, otherwise 0): Down requires a greater hour, Up a smaller hour. Without a match, Down selects the first record and Up the last. This hour-versus-row comparison also applies at 30-minute intervals.

Each selection calls `onSelected` once, including a one-record wrap. Clears time selection; does not call time/open/date listeners. After the callback, timed selection reveals the end-exclusive row if its start is at or below the first visible row; otherwise it reveals the starting row. A zero-duration event has one row. All-day selection at scroll row0 reveals row `floor(viewportHeight/rowHeight)/2+12`; other scroll positions remain. If the callback changes selection, this scroll step is skipped. An empty column keeps selection and makes no callback. WinCE VK_F20/F21 hardware message routing is not implemented.

#### Finding appointments in other dates

Multi-column search skips empty columns. For a selected timed appointment, finds the closest start minute to its start minute. A time selection uses `60*(timeStart/interval)` regardless of interval; no selection uses 0. From the nearest candidate, follows intersecting lanes opposite the travel direction. If Android minimum-width boxes overlap, the walk stops after at most the placement count. For a selected all-day appointment, prefers the same `recordId`, otherwise the first all-day record. In each other column, falls back to the other appointment class when the preferred class is absent. Tags are not compared.

#### Ordering and timezone

Timed placement and Left/Right search in all modes use unclipped source civil year/month/day/hour/minute ascending, then busyStatus descending; equal keys retain input order. Source times use the default time zone captured when the view is constructed. setLocale does not change it; recreate the view after a system time-zone change. Without source times, uses the column date and clipped startMinute. For selected/candidate nearest-time search, a source date different from its column date contributes minute0. Epoch order is not substituted for civil order, including repeated DST hours.

#### WM behavior and range navigation

An all-day record in the same column stops text-mode timed-neighbor search, matching empty-rectangle union failure on fresh native nodes. Busy-only mode continues. Stale geometry of reused native nodes is not reproduced. Range exhaustion calls the owner navigation listener; the owner loads the new week and applies the captured WeekNavigation.

Civil means year/month/day/hour/minute after timezone conversion. Fresh native nodes are newly created original placement nodes; stale geometry means coordinates retained from reused nodes.

[Time range](/en/api/wmcalendarview/#settimeselection) · [Event](/en/api/wmcalendarview/#event) · [WeekNavigation](/en/api/wmcalendarview/#captureweeknavigation--restoreweeknavigation)

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
public final int day;
public final int startMinute;
public final int endMinute;
public final int busyStatus;
public final boolean allDay;
public final String title;
public final String detail;
public final Object tag;
public final int symbolMask;
public final boolean recurrenceException;
public final String recordId;
public final boolean hasSourceTimes;
public final long sourceStartMillis;
public final long sourceEndMillis;
```

day is the zero-based column from setRange. startMinute/endMinute are civil-day minutes with exclusive end; equal endpoints create a minimum-height timed mark. allDay routes DAY records to the header list and multi-column records to the aggregated busy stripe. title/detail remain caller text. Multi-column details show title/detail as two clipped lines, stopping at CR/LF and adding "..." when truncated.

tag retains an arbitrary caller object, including null; the control neither interprets it nor derives record identity, recurrence, or multi-day links from it. Keep the Event instance for `selectEvent` and `eventBounds`. symbolMask uses [WmCalendarSymbols](/en/api/wmcalendarsymbols/) bits; recurrenceException changes the recurrence symbol when that bit is displayed. Busy status: FREE=0/TENTATIVE=1/BUSY=2/OUT_OF_OFFICE=3.

`recordId` identifies the caller’s source record. `hasSourceTimes` distinguishes supplied full-occurrence endpoints from absent metadata. `sourceStartMillis`/`sourceEndMillis` are unclipped epoch milliseconds, independent of display timezone. Supply the display column/minutes separately; the constructor does not derive them or check consistency against source times.

### Constructors

```java
public Event(int day,int startMinute,int endMinute,boolean allDay,String title,String detail,Object tag);
public Event(int day,int startMinute,int endMinute,boolean allDay,String title,String detail,Object tag,int symbolMask,boolean recurrenceException);
public Event(int day,int startMinute,int endMinute,boolean allDay,String title,String detail,Object tag,int symbolMask,boolean recurrenceException,int busyStatus);
public Event(int day,int startMinute,int endMinute,boolean allDay,String title,String detail,Object tag,int symbolMask,boolean recurrenceException,int busyStatus,String recordId,long sourceStartMillis,long sourceEndMillis);
```

Requires day 0..6, 0<=start<1440, start<=end<=1440, non-null title/detail, mask 0..63, status 0..3. Violations throw `IllegalArgumentException`. The first overload defaults to symbolMask=0 and recurrenceException=false. Both overloads without busyStatus default to BUSY; the second retains its supplied symbolMask/recurrenceException. The three existing overloads use recordId=null, hasSourceTimes=false, and both source times0. The 13-argument overload sets hasSourceTimes=true and requires a non-null, nonempty recordId and sourceEndMillis>=sourceStartMillis; violations throw `IllegalArgumentException`. It preserves the recordId verbatim, including whitespace, and accepts equal or negative epoch endpoints.

## OnEventListener

```java
public interface OnEventListener
```

### onSelected

```java
void onSelected(Event event);
```

Receives the retained Event after appointment DOWN or keyboard appointment selection. Selection has already changed; it clears time selection and updates the keyboard cursor. DAY keyboard selection notifies only when changing appointments. Multi-column Up/Down also notifies when wrapping to the same appointment. Touching the selected appointment can notify again. `selectEvent` is silent.

### onOpen

```java
void onOpen(Event event);
```

Receives the same Event on completed appointment UP within touch slop. Movement, cancellation, multi-touch, or a different UP hit prevents opening; the earlier DOWN selection remains. Keyboard selection does not call this method.

## OnDateListener

```java
public interface OnDateListener
```

### onDate

```java
void onDate(int year,int month,int day);
```

Receives the tapped header date after a completed same-column tap in 5/6/7-column modes. year/month/day are civil date components; month is one-based. The range stays unchanged until the owner changes it.

## OnTimeListener

```java
public interface OnTimeListener
```

### onSelected

```java
void onSelected(int day,int startMinute,int endMinute);
```

Receives `(day,startMinute,endMinute)` after a completed empty-slot tap or keyboard time selection. day is zero-based; minutes are aligned to the current interval and end is exclusive. State is updated before the callback. Programmatic changes are silent.

## OnNavigateListener

```java
public interface OnNavigateListener
```

### onNavigate

```java
void onNavigate(int direction);
```

Receives -1 for Left or +1 for Right when DAY has no adjacent timed appointment, or multi-column search exhausts its range. The owner changes the date/range; the control does not move it automatically.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmCalendarView.java`
