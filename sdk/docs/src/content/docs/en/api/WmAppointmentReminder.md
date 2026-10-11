---
title: "WmAppointmentReminder"
slug: en/api/wmappointmentreminder
---
[API index](/en/api/)

```java
public final class WmAppointmentReminder extends LinearLayout implements WmControls.LanguageListener
```

Displays relative reminder enablement, quantity, and unit. Disabled reminders hide the numeric row. The owner supplies each field popup.

### Constructors

```java
public WmAppointmentReminder(Context context,Typeface font,Locale locale,WmReminder initial,int selection);
```

Null font/locale/initial throws `IllegalArgumentException`. The initial enabled, quantity, and unit initialize their corresponding fields. selection is an Android selection highlight color int, not a unit index. Quantity accepts up to four ASCII digits; unit changes preserve quantity.

### value

```java
public WmReminder value();
```

Returns a new reminder using valid current digits or the last committed quantity. Does not commit or rewrite incomplete drafts.

### saveInteractionState

```java
public Bundle saveInteractionState();
```

Returns schema1 state containing committed fallback, literal draft, candidates, and edit interaction. Does not trigger a blur commit.

### validateInteractionState

```java
public static void validateInteractionState(Bundle state);
```

Validates schema1 completely. Drafts may be empty or contain 1..4 ASCII digits. Invalid schema, values, candidates, or edit state throw `IllegalArgumentException`.

### restoreInteractionState

```java
public void restoreInteractionState(Bundle state);
```

Silently restores with popups closed; the owner manages focus. Open enabled/unit/quantity popups throw `IllegalStateException`; malformed snapshots throw `IllegalArgumentException`.

### commitQuantity

```java
public void commitQuantity();
```

Commits 1..4 valid ASCII digits, including zero. Empty/invalid text reverts to the last quantity. Ordinary blur also commits.

### enabledChoice

```java
public WmChoice enabledChoice();
```

Returns the owned reminder off/on combo; supply its popup through the opening listener.

### quantityField

```java
public WmEditableChoice quantityField();
```

Returns the owned quantity editor; supply its candidate popup through the opening listener.

### unitChoice

```java
public WmChoice unitChoice();
```

Returns the owned minutes/hours/days/weeks combo; supply its popup through the opening listener.

### setEditingEnabled

```java
public void setEditingEnabled(boolean enabled);
```

Controls editing without changing reminder values. Numeric-row visibility still follows reminder enablement.

### setLocale

```java
public void setLocale(Locale locale);
```

Relabels enablement and units through the language catalog while retaining quantity/selections. Null throws `IllegalArgumentException`.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

Passes the shared catalog language’s Locale to setLocale, relabeling enablement/units while retaining quantity and selection.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmAppointmentReminder.java`
