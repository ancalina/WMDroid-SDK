---
title: "WmToday"
slug: ko/api/wmtoday
---
[API](/ko/api/)

Today 모듈 배치, 행 텍스트, 일정 선택, RemoteViews용 raster 변환.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmToday.java`

View 생성, 변경은 UI thread에서 수행하세요. 치수는 논리 픽셀, 일정 timestamp는 epoch millisecond입니다.

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

size는 1–480, inset, left, right는 0 이상, left+right는 480 미만이어야 합니다. alignment는 LEFT, CENTER_HORIZONTAL, RIGHT만 허용합니다. 다른 값은 `IllegalArgumentException`입니다. STANDARD는 size 23, inset 8, left 56, right 4, LEFT 정렬을 사용합니다. NATIVE_LINES는 오른쪽 gutter만 26으로 바꿉니다. size를 바꿔도 inset, gutter는 그대로입니다.

lineHeight는 `max(1,round(TODAY_LINE*size/TODAY_FONT))`입니다. height는 한 줄 이하에서 `max(TODAY_MIN,lineHeight)`, 그 외 `lines*lineHeight+(lines-1)*GAP+2*inset`입니다.

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

Style은 네 ARGB 값을 보관합니다. enabled 상태에서 pressed, selected, focused content는 선택 배경, 일반, disabled content는 투명 배경입니다. 배경 상태 변경에는 fade가 없습니다. Row caption은 pressed, selected에서만 selectedText를 사용하며 focus만 있으면 text를 유지합니다.

Divider.ALWAYS는 마지막 표시 행 뒤에도 선을 표시합니다. WHEN_FOLLOWED, BETWEEN은 동일하게 보이는 다음 행이 있을 때만 선을 표시합니다. Layout이 2픽셀 divider를 소유하므로 content 안에 divider를 추가하지 마세요. Module은 등록 ID, content 참조, divider 정책을 보관합니다.

## Layout 생성자

```java
public static final class Layout extends LinearLayout
```

```java
public Layout(Context context, Style style);
```

생성자는 세로 layout을 만들고 Style을 보관합니다. content 등록 전에 null이 아닌 Style을 제공하세요. 클릭 동작 생성, provider 데이터 조회는 하지 않습니다.

## Layout.register

```java
public Module register(String id, View content, int height, Divider divider);
```

register는 빈 문자열이 아닌 ID, null이 아닌 content, divider, height≥0을 요구하며 위반 시 `IllegalArgumentException`입니다. content LayoutParams를 MATCH_PARENT×height로, 배경을 Style 상태 배경으로 교체합니다. 외부 위젯의 높이 제한 32–480은 적용하지 않습니다.

같은 ID는 등록 순서를 유지하며 등록을 교체합니다. content View도 같으면 divider View를 유지합니다. 등록만으로 content를 붙이거나 기존 content를 제거하지 않습니다. arrange를 호출해 교체 결과를 표시하세요. arrange가 붙일 때 content는 parent가 없거나 이 Layout에 속해야 합니다. 다른 parent가 있으면 addView에서 실패하며 register는 이 조건을 검증하지 않습니다.

## Layout.rows

```java
public Map<String,View[]> rows();
```

rows는 등록 순서의 같은 수정 불가 live Map을 반환합니다. 각 배열의 0은 content, 1은 divider View입니다. 배열, View는 mutable 참조이므로 배열 원소를 교체하거나 View의 parent를 바꾸지 마세요.

## Layout.remove

```java
public void remove(String id);
```

ID의 등록을 제거합니다. 없는 ID는 무시합니다. 붙어 있는 content, divider는 다음 arrange까지 남습니다.

## Layout.removePrefix

```java
public void removePrefix(String prefix);
```

removePrefix는 두 map에서 일치하는 등록을 제거합니다. 빈 prefix는 모두 제거하며 null은 등록 ID를 검사할 때 `NullPointerException`입니다. 붙어 있는 content, divider는 다음 arrange까지 남습니다.

## Layout.arrange

```java
public void arrange(List<String> preferredOrder, Set<String> hidden);
```

arrange는 preferredOrder대로 배치하고 나머지는 등록 순서로 추가합니다. 알 수 없거나 중복인 ID는 무시합니다. preferredOrder, hidden은 null이면 안 됩니다. 제거된 등록의 자식만 떼고 순서가 바뀐 content/divider 쌍만 이동합니다. 변경 없는 자식은 그대로 붙어 있습니다. hidden에 따라 content를 VISIBLE 또는 GONE으로 설정합니다. divider는 표시 행 순서를 따르며 hidden content의 divider는 표시하지 않습니다.

붙이기 실패 시 일부만 재구성된 layout이 남을 수 있으며 rollback하지 않습니다. 하나의 content View를 여러 ID에 등록하거나 이 Layout에 별도 자식을 붙이지 마세요.

## Row 생성자, caption, setCaption, setCaptionLines

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

첫 생성자는 STANDARD typography를 사용합니다. null typography는 `IllegalArgumentException`입니다. null이 아닌 Style과 parent가 없는 icon을 제공하세요. icon은 TextStyle과 무관하게 32×32, 왼쪽/위 inset 4입니다. caption은 소유한 TextView이며 typeface, 크기, padding, listener 변경은 이후 측정, 상태 동작에 영향을 줄 수 있습니다.

텍스트는 전달한 face, WmFonts.attach fallback span/smoothing, 논리 픽셀 크기, font padding 없음을 사용합니다. API 21 이상은 kern, API 23 이상은 break strategy 0입니다. Row는 클릭 listener를 설치하거나 focusable로 설정하지 않습니다. owner가 동작, focus/selection 정책을 정하세요.

Row는 최초 font, TextStyle, Layout은 최초 Style을 보관합니다. WmControls.restyleTree는 이 보관 값을 교체하지 않습니다. font, TextStyle 변경 시 Row를 다시 만들고 등록한 뒤 arrange를 호출하세요. Style 변경 시에는 새 Style로 Layout도 다시 만들고 모듈, preferredOrder, hidden을 다시 적용하세요. Row만 교체하면 기존 Layout의 background, divider 색은 유지됩니다. caption typeface만 바꿔도 이후 일정 블록의 font는 바뀌지 않습니다.

두 setter는 일정 블록을 제거하고 caption을 표시하며 행 contentDescription을 지웁니다. 붙인 font span을 포함해 폭 480−left−right에서 측정하고 행 LayoutParams를 MATCH_PARENT×TextStyle.height(lines)로 갱신합니다. caption은 left에서 시작하고 right padding을 예약하며 같은 가용 폭에서 수평 정렬합니다. 실제 parent 폭이 달라도 480픽셀 기준 측정은 바뀌지 않습니다.

setCaption은 StaticLayout으로 wrap합니다. setCaptionLines는 LF 줄바꿈을 세며 수평 overflow를 wrap, ellipsis 없이 자릅니다. 단일 줄은 수직 중앙, 여러 줄은 TOP과 inset padding을 사용합니다. caption 줄 간격은 lineHeight()+GAP을 목표로 합니다. STANDARD의 한/두/세 줄 행 높이는 40/74/104입니다.

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

Appointment는 subject, detail의 null을 빈 문자열로, LF (`\n`)를 공백으로 바꾸며 다른 문자는 유지합니다. setAppointments는 기존 블록 교체 전에 비어 있지 않은 목록, null이 아닌 항목을 검증합니다. 위반 시 `IllegalArgumentException`입니다. 새 text View를 만들며 목록은 보관하지 않습니다.

subject는 480−left−right에서 wrap하며 최소 한 줄입니다. 비어 있지 않은 detail은 한 줄 하단에 정렬하고 prefix 측정 시 ASCII `...` 폭을 예약합니다. prefix가 짧아지면 surrogate pair를 자르지 않고 `...`을 붙입니다. 전체 detail은 contentDescription에 남습니다. subject/detail 사이 gap은 없으며 다음 일정 앞에는 정수 나눗셈 lineHeight()/2 gap을 둡니다.

행 높이는 max(TODAY_MIN, 2×inset + 모든 subject/detail 높이 + 일정 간 gap)입니다. caption은 GONE이지만 전체 연결 텍스트를 보관하며 행 contentDescription도 같은 전체 텍스트입니다. 새 블록은 생성자 font와 현재 caption 색을 사용합니다.

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

CalendarEntry는 end≥start, dayOffset≥0, minuteOfDay 0–1439, appointment non-null을 요구합니다. 잘못된 값은 `IllegalArgumentException`입니다. dayOffset 0은 오늘입니다. caller가 실제 시간대에 맞춰 dayOffset, minuteOfDay를 계산하세요.

selectAppointments는 입력을 수정하지 않고 새 목록을 반환합니다. null 목록, 항목은 `IllegalArgumentException`입니다. flags bit 1은 오늘 종일 일정, bit 2는 다음 시작 minute 그룹입니다. bit 4는 내부 선택 모드에 사용됩니다. end<now인 timed 일정은 제외하고 end==now와 진행 중 일정은 유지합니다. 종일을 먼저, timed를 시작 순서로 정렬합니다. 미래 날짜의 timed 일정은 가장 가까운 양수 dayOffset만 남깁니다. bit 2이면 첫 미래 일정과 같은 dayOffset, minuteOfDay만 추가합니다. bit 2와 bit 4가 모두 없으면 첫 미래 일정보다 먼 dayOffset은 제외합니다.

## appointmentDetails

```java
public static String appointmentDetails(long start,long end,boolean allDay,String location,java.text.DateFormat timeFormat);
public static String appointmentDetails(long start,long end,int displayFlags,String location,java.text.DateFormat timeFormat,String continuation);
```

timeFormat이 null이면 `IllegalArgumentException`입니다. 종일은 빈 문자열입니다. timed는 시작 시간, 같은 civil minute이 아닌 종료 시간, 선택적 ` (location)`을 표시합니다. 시간 문자열의 literal U+0020 공백만 제거합니다. civil-minute 비교는 timeFormat의 Calendar를 복제해 사용하며 Calendar 없는 formatter는 floor(epochMillis/60000)으로 비교합니다. location 원문은 유지합니다. helper는 end≥start를 검증하지 않습니다.

displayFlags bit 2는 종일, 4는 자정 경계, 8은 진행 중, 32는 오늘 시작, 64는 오늘 종료입니다. bit 4가 없으면 boolean overload를 사용합니다. bit 4가 있으면 timeFormat과 비어 있지 않은 continuation이 필요합니다. 오늘 시작하지 않은 진행 중 일정의 시작에 continuation을 사용합니다. 그 일정이 오늘 끝나지도 않으면 continuation 하나만 표시합니다. 그 외 오늘 끝나지 않는 일정은 `-continuation`을 붙입니다. formatter/continuation 필수 조건은 종일의 빈 문자열 반환 전에도 검증합니다.

## rasterize

```java
public static Bitmap rasterize(Row row,int pixelWidth,int height);
```

UI thread에서 호출해야 하며 다른 thread는 `IllegalStateException`입니다. row는 parent가 없어야 하고 pixelWidth는 1–1440, height는 32–480이어야 합니다. null row, 붙어 있는 row, 잘못된 크기는 `IllegalArgumentException`입니다. row를 480×height로 배치한 뒤 ARGB_8888 Bitmap을 반환합니다. 실제 Bitmap 높이는 `max(1,round(height*pixelWidth/480))`입니다. density는 NONE입니다. row의 measured size, bounds를 480×height로 바꾸며 이전 bounds를 복원하지 않습니다. LayoutParams는 그대로입니다. 현재 row 상태를 divider 없이 그립니다. caller가 반환 Bitmap을 소유하고 recycle합니다.
