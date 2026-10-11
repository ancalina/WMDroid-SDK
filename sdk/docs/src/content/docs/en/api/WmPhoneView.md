---
title: "WmPhoneView"
slug: en/api/wmphoneview
---
[API index](/en/api/)

```java
public final class WmPhoneView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

Client for number entry, status, voicemail, and keypad. The owner supplies actual dialing, voicemail configuration, and contact/history data.

## Constructor and Listener

```java
public interface Listener {void dial(String number);void voicemail();void speedDial();void history();}
public final WmPhoneKeypad keypad;
public WmPhoneView(Context context,Typeface regular,Typeface bold,int selection,Listener listener)
```

Both fonts and listener are required; null throws `IllegalArgumentException`. Keypad is the owned control. Digit/backspace keys edit the internal buffer. Talk passes a normalized number to dial only when nonempty. Tapping an available voicemail row with an empty buffer invokes voicemail. Speed Dial/Call History invoke their corresponding callbacks.

## Number

```java
public static String dialable(String value)
public String number()
public void setNumber(String value)
public void setCountryIso(String country)
```

Dialable accepts at most 256 UTF-16 code units and produces at most 80. It retains ASCII `0`..`9` and `*#+,;`, removing ASCII spaces, parentheses, hyphens, and dots. Null, other characters, or excess length throws `IllegalArgumentException`. Empty strings are valid. Number returns the unformatted buffer; setNumber validates and redraws. SetCountryIso accepts two ASCII letters or an empty string and rejects null/invalid syntax. Stores uppercase independently of UI language. API21+ with a country uses regional Android number formatting; otherwise it uses legacy formatting.

## Status and keypad

```java
public void setStatus(String authored)
public void setVoicemailAvailable(boolean available)
public boolean isKeypadVisible()
public void setKeypadVisible(boolean visible)
```

SetStatus requires a non-null status string or throws `IllegalArgumentException`; it displays through SDK translation lookup. Voicemail=true shows the voicemail row for an empty number; false omits it. It does not create or query a voicemail number. KeypadVisible switches the keypad between VISIBLE/GONE while preserving the number.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
```

Updates fonts and selection color; null fonts throw `IllegalArgumentException`. Language changes apply to SDK-authored captions and preserve user data.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmPhoneView.java`
