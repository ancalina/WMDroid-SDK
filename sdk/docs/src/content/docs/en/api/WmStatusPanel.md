---
title: "WmStatusPanel"
slug: en/api/wmstatuspanel
---
[API index](/en/api/)

```java
public class WmStatusPanel extends LinearLayout implements WmControls.RestyleListener
```

Popup body for Android connection, volume, battery, or phone status. WmPopup supplies chrome. Polls state every second while attached. The host supplies launch/dismiss/settings actions, feedback, translation, and theme mapping.

## Host

```java
public interface Host {
    void launch(Intent intent);
    void changed();
    void backlight();
    void hide();
    void settings();
    String text(String authored);
    int color(int original);
    void feedback(boolean ring,int step);
    void release();
    default void attached(){}
}
```

launch handles Android settings Intents; backlight opens illumination controls; hide dismisses the popup; settings opens host settings. text translates authored labels; color maps an original ARGB color to the theme. changed reports volume/ringer state changes and can also run after a ringer-change SecurityException. feedback runs when volume actually changes: ring=true identifies STREAM_RING, false STREAM_MUSIC; step is 0..5. attached runs on attachment (default no-op); release runs on each detach for host feedback/resource cleanup. Callbacks execute synchronously from UI events and UI polling.

## Constructor

```java
public WmStatusPanel(Context context,Typeface regular,int kind,Host host)
```

kind=0 selects connection, 1 volume, 3 phone; all other values select battery. A non-null host is needed during construction. Reads initial state immediately. Connection shows transport/connecting/internet validation; phone uses WmDevice.Phone; battery shows charge, percentage, and API21+ power-saving state. Volume provides music/ring controls in 0..5 steps and normal/vibrate/silent modes. Leaving normal mode saves music volume and sets it to zero; returning restores it. Connection/battery setting links invoke Host.launch.

## refresh

```java
public void refresh()
```

Rereads and displays Android state for the selected kind. Volume changes require platform MODIFY_AUDIO_SETTINGS permission. On API24+, transitions into/out of silent mode without notification-policy access show guidance and an access-settings link instead. Audio-change SecurityException also shows guidance. The host handles permission approval, settings-Intent execution, and brightness controls.

## Style and lifetime

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
protected void onAttachedToWindow()
protected void onDetachedFromWindow()
```

Restyle updates the regular-font reference, hide-button theme, and volume controls, reloading volume artwork when resource revision changes. Attachment calls Host.attached and schedules one-second polling; volume mode focuses the current mode control. Detach cancels polling and calls Host.release. Reattachment schedules polling again.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmStatusPanel.java`
