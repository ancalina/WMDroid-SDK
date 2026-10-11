---
title: "WmTitleBar"
slug: en/api/wmtitlebar
---
[API index](/en/api/)

```java
public final class WmTitleBar extends LinearLayout
```

Logical-VGA title assembly containing Start, status slots, clock, and X/OK. The host owns data updates, click actions, and timers. Default height is 52 logical pixels.

## Fields

```java
public static final int CLOSE_WIDTH=50;
public final LinearLayout start;
public final ImageView logo;
public final TextView caption;
```

CLOSE_WIDTH is the right close/reserved-slot width. start, logo, and caption are actual child views; the host sets Start clicks and caption text. The logo uses resource start at 42×36px.

## Constructors

```java
public WmTitleBar(Context context,Typeface font,String text,WmBar paint)
public WmTitleBar(Context context,TextView caption,WmBar paint)
```

The first overload creates an SDK TextView and delegates. The second requires a non-null, unattached caption and non-null paint; violations throw IllegalArgumentException. Caption becomes single-line with end ellipsis and 14/4px horizontal padding. Start retains remaining layout width but limits touch targeting to caption width and activates on ACTION_DOWN.

## startTouchWidth / onLayout

```java
public int startTouchWidth()
protected void onLayout(boolean changed,int left,int top,int right,int bottom)
```

Computes touch width from caption ink, logo, insets, and 8px allowance. The 72..180px target is capped by actual Start width. After layout, captions fully inside the target use pressed/selected colors; overflowing captions stay white to avoid split feedback.

## addTray

```java
public WmTraySlot addTray(int kind,ImageView image)
```

Inserts and returns a slot in CONNECTION, PHONE, VOLUME, BATTERY order. kind must be WmTraySlot 0..3; other values throw IllegalArgumentException. Adding a duplicate kind throws IllegalStateException. Supply an unattached ImageView. The host registers slot click actions.

## setClock

```java
public void setClock(WmTrayClock replacement)
```

Removes the old clock and inserts its replacement before the right slot. Null removes it. The same instance is a no-op; another attached instance throws IllegalArgumentException. Uses WRAP_CONTENT width, 52px height, and 4px right margin. The caller owns refreshes and clicks.

## Close and reserved space

```java
public ImageView closeButton(boolean ok)
public void reserveCloseSpace()
```

closeButton selects OK when ok=true, X otherwise, and returns the actual borderless button. The caller supplies click action and accessibility description. It replaces reserved space. reserveCloseSpace removes the button and retains a 50px blank slot. A previous removed button instance is not reused on recreation.

## Resources and style

```java
public void reloadResources()
public void restyle(WmBar paint)
```

reloadResources reloads only the Start logo; update slot resources/state separately. restyle requires non-null paint (otherwise IllegalArgumentException) and updates existing backgrounds, feedback colors, slots, clock, and close glyph while retaining views/actions.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmTitleBar.java`
