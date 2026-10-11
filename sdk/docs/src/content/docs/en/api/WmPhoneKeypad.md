---
title: "WmPhoneKeypad"
slug: en/api/wmphonekeypad
---
[API index](/en/api/)

```java
public final class WmPhoneKeypad extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

4×4 telephone keypad. Reports digits and Backspace, Speed Dial, Call History, and Talk actions to owner callbacks.

## Constructor and geometry

```java
public static final int WIDTH=480,HEIGHT=294;
public WmPhoneKeypad(Context context,Typeface regular,Typeface bold,Listener listener)
protected void onMeasure(int widthSpec,int heightSpec)
protected void onLayout(boolean changed,int left,int top,int right,int bottom)
```

Both fonts and listener are required; null throws `IllegalArgumentException`. WIDTH/HEIGHT use logical pixels. Smaller constraints shrink keys proportionally; larger surfaces do not enlarge the keys. Extra width centers the keypad; extra height stays above it.

## Listener

```java
public interface Listener {
    void digit(char digit);
    void backspace();
    void speedDial();
    void history();
    void talk();
}
```

Digit reports only `0`..`9`, `*`, and `#`. The `+` caption on the 0 key has no long-press input action. Other callbacks run on a completed click; leaving the key, adding another pointer, or cancellation cancels the press.

## Enable and style

```java
public void setEnabled(boolean enabled)
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
```

Enabled propagates to all keys; disabling clears pressed state. Restyling applies the two fonts and rejects null fonts. Language changes update action captions/accessibility descriptions. Selection/tab colors do not change the fixed keypad skin.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmPhoneKeypad.java`
