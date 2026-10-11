---
title: "WmTraySlot"
slug: en/api/wmtrayslot
---
[API index](/en/api/)

```java
public final class WmTraySlot extends FrameLayout
```

Title-bar slot combining status artwork, touch target, and connection animation. Coordinates use logical pixels. Battery uses normal UP click handling; other kinds activate on DOWN.

## Constants and constructor

```java
public static final int CONNECTION=0,VOLUME=1,BATTERY=2,PHONE=3;
public final ImageView image;
public WmTraySlot(Context context,int kind,ImageView image)
```

kind must be 0..3; other values throw IllegalArgumentException. Supply a non-null, unattached ImageView, which becomes the actual child. Artwork is 32×32px at top inset 10px; slot height is 52px with right margin 4px. Initial width: connection22, battery42, others38px. Use image to update status artwork and accessibility text.

## style

```java
public void style(WmBar bar)
```

Applies the bar title-button background. Requires a non-null bar. Retains image and actions.

## connectionIcon

```java
public void connectionIcon(String id)
```

Connection slots only; another kind throws IllegalStateException. IDs: signal, network_connected, network_data_connected, network_disconnected; other IDs, including null, throw IllegalArgumentException. Same ID is a no-op. A change stops animation and loads WmIcons artwork. Slot widths are respectively22/32/34/34px; image left inset is1px for disconnected,2px otherwise. Artwork itself is not stretched.

## connectionState

```java
public void connectionState(WmDevice.Network network)
```

Another kind throws IllegalStateException. Null or neither connected nor connecting selects disconnected. Connecting uses a34px slot,1px inset, and a repeating26-frame animation at150ms/frame; unavailable pack frames fall back to static network_data_connected. Connected Wi-Fi uses signal, cellular data_connected, other transports connected. Internet validation is separate from link connectivity. Artwork loads on the transition into connecting; playback starts when a window token exists and window visibility is VISIBLE. It does not check the slot’s own visibility.

## Animation and resource lifetime

```java
public void stopConnectionAnimation()
public void reloadResources()
protected void onAttachedToWindow()
protected void onDetachedFromWindow()
```

stop halts playback but retains artwork/state. reloadResources stops playback and clears cached animation/ID; it does not immediately replace the ImageView drawable, so call connectionState/connectionIcon to apply the new pack. Detach stops; attach restarts a cached connecting animation. A subsequent connectionState call or reattachment can restart playback after stop. Reattachment does not check window visibility.

## activateOnDown

```java
public static void activateOnDown(View button)
```

Replaces the existing OnTouchListener and disables Android sound effects. DOWN sets pressed=true, performs one click, and consumes the event. MOVE updates pressed according to whether coordinates remain inside. UP/CANCEL clears pressed without another click. Other touch events are consumed too. Register the click listener separately.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmTraySlot.java`
