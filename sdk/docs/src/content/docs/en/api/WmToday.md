---
title: "WmToday"
slug: en/api/wmtoday
---
[API](/en/api/)

Today module layout, row text, appointment selection, and rasterization for RemoteViews.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmToday.java`

Create and change Views on the UI thread. Dimensions use logical pixels. Appointment timestamps use epoch milliseconds.

```java
public final class WmToday
```

## TextStyle

```java
public static final class TextStyle
public static final TextStyle STANDARD = new TextStyle(WmMetrics.TODAY_FONT, WmMetrics.INSET, WmMetrics.TEXT_LEFT, WmMetrics.ICON_INSET, Gravity.LEFT);
public static final TextStyle NATIVE_LINES = new TextStyle(WmMetrics.TODAY_FONT, WmMetrics.INSET, WmMetrics.TEXT_LEFT, 26, Gravity.LEFT);
public final int size;
public final int inset;
public final int left;
public final int right;
public final int alignment;
```

```java
public TextStyle(int size,int inset,int left,int right,int alignment);
public int lineHeight();
public int height(int lines);
```

size must be 1–480. inset, left, and right must be nonnegative; left+right must be below 480. alignment accepts LEFT, CENTER_HORIZONTAL, or RIGHT. Other values throw `IllegalArgumentException`. STANDARD uses size 23, inset 8, left 56, right 4, and LEFT alignment. NATIVE_LINES changes only the right gutter to 26. Insets and gutters stay fixed when size changes.

lineHeight is `max(1,round(TODAY_LINE*size/TODAY_FONT))`. For one line or less, height is `max(TODAY_MIN,lineHeight)`. Otherwise it is `lines*lineHeight+(lines-1)*GAP+2*inset`.

## Style, Divider, Module

```java
public static final class Style
public final int text;
public final int selectedText;
public final int selection;
public final int divider;
```

```java
public static final class Module
public final String id;
public final View content;
public final Divider divider;
```

```java
public enum Divider { ALWAYS, WHEN_FOLLOWED, BETWEEN }
```

```java
public Style(int text, int selectedText, int selection, int divider);
```

Style retains the four ARGB values. Enabled pressed, selected, or focused content uses the selection background; normal or disabled content is transparent. Background state changes have no fade. Row caption text uses selectedText only for pressed or selected states; focus alone keeps text.

Divider.ALWAYS includes a line after the last visible row. WHEN_FOLLOWED and BETWEEN are equivalent: they show a line only before another visible row. The Layout owns each 2-pixel divider; do not add another divider inside module content. Module retains the registered ID, content reference, and divider policy.

## Layout constructor

```java
public static final class Layout extends LinearLayout
```

```java
public Layout(Context context, Style style);
```

The constructor creates a vertical layout and retains Style; supply a non-null Style before registering content. It does not create click actions or load provider data.

## Layout.register

```java
public Module register(String id, View content, int height, Divider divider);
```

register requires a nonempty ID, non-null content and divider, and height≥0; violations throw `IllegalArgumentException`. It replaces content LayoutParams with MATCH_PARENT×height and replaces its background with the Style state background. Height is not clamped to the external widget limit of 32–480.

Reusing an ID replaces its registration without changing registration order. Reusing the same content View also retains its divider View. Registration does not attach content or remove the old attached content. Call arrange to display the replacement. Content must be unattached or already owned by this Layout when arrange attaches it; another parent causes an addView failure. Registration does not validate that parent condition.

## Layout.rows

```java
public Map<String,View[]> rows();
```

rows returns the same unmodifiable live Map in registration order. Each array contains content at 0 and its divider View at 1. The arrays and Views are mutable references; do not replace array elements or reparent the Views.

## Layout.remove

```java
public void remove(String id);
```

Removes the registration for id. Unknown IDs are ignored. Attached content and divider remain until the next arrange.

## Layout.removePrefix

```java
public void removePrefix(String prefix);
```

removePrefix removes matching registrations from both maps. An empty prefix removes all registrations; null throws `NullPointerException` when a registered ID is tested. Attached content and dividers remain until the next arrange.

## Layout.arrange

```java
public void arrange(List<String> preferredOrder, Set<String> hidden);
```

arrange uses preferredOrder, then appends remaining modules in registration order. Unknown and duplicate IDs are ignored. Supply non-null preferredOrder and hidden. It detaches removed registrations and moves displaced content/divider pairs. Unchanged children remain attached. It sets content visibility to VISIBLE or GONE from hidden. Dividers follow the visible order; hidden content never shows its divider.

An attachment failure can leave a partially rebuilt layout; there is no rollback. Do not share one content View between registered IDs or attach unrelated children to this Layout.

## Row constructor, caption, setCaption, setCaptionLines

```java
public static final class Row extends FrameLayout
public final TextView caption;
```

```java
public Row(Context context, ImageView icon, Typeface font, Style style, CharSequence text);
public Row(Context context, ImageView icon, Typeface font, Style style, CharSequence text,TextStyle typography);
public void setCaption(CharSequence text);
public void setCaptionLines(CharSequence text);
```

The first constructor uses STANDARD typography. Null typography throws `IllegalArgumentException`. Supply a non-null Style and an unattached icon. The icon is 32×32 at left/top inset 4, independent of TextStyle. caption exposes the owned TextView; changing its typeface, size, padding, or listeners can alter subsequent measurements and state behavior.

Text uses the supplied face, WmFonts.attach fallback spans/smoothing, logical-pixel size, and no font padding. API 21+ enables kern; API 23+ uses break strategy 0. The row does not install a click listener or make itself focusable. The owner supplies actions and focus/selection policy.

Row retains the original font and TextStyle; Layout retains its original Style. WmControls.restyleTree does not replace these retained values. When changing font or TextStyle, rebuild/re-register Rows, then arrange the Layout. For a Style change, also construct a Layout with the new Style and reapply modules, preferredOrder, and hidden. Replacing Rows alone retains the old Layout background and divider colors. Directly changing caption typeface does not change the font used by future appointment blocks.

Both setters remove existing appointment blocks, show caption, and clear the row contentDescription. They measure against width 480−left−right, including attached font spans, and update row LayoutParams to MATCH_PARENT×TextStyle.height(lines). Caption starts at left and reserves right padding; horizontal alignment uses that same available width. Actual parent width does not change this 480-pixel measurement.

setCaption wraps with StaticLayout. setCaptionLines counts LF line breaks and clips horizontal overflow without wrapping or ellipsis. One line is vertically centered; multiple lines use TOP with inset padding. Caption line spacing targets lineHeight()+GAP. With STANDARD, one/two/three lines produce row heights 40/74/104.

## Appointment, Row.setAppointments

```java
public static final class Appointment
public final String subject;
public final String detail;
```

```java
public Appointment(String subject,String detail);
public void setAppointments(List<Appointment> entries);
```

Appointment changes null subject or detail to an empty string and LF (`\n`) to spaces; other characters remain. setAppointments validates a nonempty list of non-null entries before replacing existing blocks; invalid lists throw `IllegalArgumentException`. It creates new text Views and does not retain the list.

Subjects wrap within 480−left−right and occupy at least one line. Nonempty details occupy one line, align at its bottom, and reserve the width of ASCII `...` during prefix fitting. A shortened prefix receives `...` without splitting a surrogate pair. The full detail remains in its contentDescription. No gap separates subject/detail; later appointments start after lineHeight()/2 using integer division.

The row height is max(TODAY_MIN, 2×inset + all subject/detail heights + inter-appointment gaps). caption becomes GONE but stores the full joined text. The row contentDescription stores that same full text. New blocks use the constructor font and current caption colors.

## CalendarEntry, selectAppointments

```java
public static final class CalendarEntry
public final long start;
public final long end;
public final boolean allDay;
public final int dayOffset;
public final int minuteOfDay;
public final Appointment appointment;
```

```java
public static List<CalendarEntry> selectAppointments(List<CalendarEntry> entries,long now,int flags);
public CalendarEntry(long start,long end,boolean allDay,int dayOffset,int minuteOfDay,Appointment appointment);
```

CalendarEntry requires end≥start, dayOffset≥0, minuteOfDay 0–1439, and a non-null appointment. Invalid values throw `IllegalArgumentException`. dayOffset 0 means today. Compute dayOffset and minuteOfDay in the required time zone.

selectAppointments returns a new list without changing the input. Null lists or entries throw `IllegalArgumentException`. Flag bit 1 includes today's all-day entries. Bit 2 selects the next start-minute group. Bit 4 is an internal selection mode. Timed entries with end<now are excluded; end==now and ongoing entries remain. All-day entries sort first, followed by timed entries in start order. Future-date timed entries retain only the nearest positive dayOffset. With bit 2, further future entries must match the first future entry's dayOffset and minuteOfDay. With neither bit 2 nor bit 4, entries beyond the first future entry's dayOffset are excluded.

## appointmentDetails

```java
public static String appointmentDetails(long start,long end,boolean allDay,String location,java.text.DateFormat timeFormat);
public static String appointmentDetails(long start,long end,int displayFlags,String location,java.text.DateFormat timeFormat,String continuation);
```

Null timeFormat throws `IllegalArgumentException`. All-day entries return an empty string. Timed entries show the start, an end in a different civil minute, and optional ` (location)`. Only literal U+0020 spaces are removed from formatted times. Civil-minute comparison uses cloned Calendars from timeFormat; a formatter without a Calendar uses floor(epochMillis/60000). Location text is retained unchanged. The helpers do not validate end≥start.

displayFlags: bit 2 all-day, 4 midnight boundary, 8 ongoing, 32 starts today, 64 ends today. Without bit 4, the boolean overload is used. With bit 4, timeFormat and a nonempty continuation are required. continuation replaces the start of an ongoing entry that did not start today. If that entry also does not end today, only one continuation label appears. Otherwise, an entry that does not end today appends `-continuation`. Required formatter/continuation validation occurs before the all-day empty result.

## rasterize

```java
public static Bitmap rasterize(Row row,int pixelWidth,int height);
```

Call on the UI thread; other threads throw `IllegalStateException`. row must have no parent. pixelWidth must be 1–1440; height must be 32–480. Null or attached rows and invalid dimensions throw `IllegalArgumentException`. The method lays out row at 480×height and returns an ARGB_8888 Bitmap. Bitmap height is `max(1,round(height*pixelWidth/480))`. Density is NONE. The method changes the row measured size and bounds to 480×height; it does not restore previous bounds. LayoutParams remain unchanged. It draws the current row state without a divider. The caller owns and recycles the returned Bitmap.
