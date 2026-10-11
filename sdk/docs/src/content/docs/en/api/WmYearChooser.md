---
title: "WmYearChooser"
slug: en/api/wmyearchooser
---
[API index](/en/api/)

```java
public final class WmYearChooser extends WmDialog implements WmControls.RestyleListener,WmControls.BackHandler,WmControls.LanguageListener
```

Embedded year-input dialog View for1900..2999. The owner removes the View after completion/cancel.

### Constants and fields

```java
public static final int MIN_YEAR = 1900;
public static final int MAX_YEAR = 2999;
```

MIN_YEAR=1900, MAX_YEAR=2999.

### Constructors

```java
public WmYearChooser(Context context,Typeface font,Locale locale,int year,int selection,Listener listener);
```

Requires an in-range initial year and non-null locale/listener (`IllegalArgumentException`). Accepts up to four ASCII digits. Requests the keyboard after attachment.

### nearbyYears

```java
public static int[] nearbyYears(int current);
```

Returns a new current-1..current+3 array clipped to1900..2999. Out-of-range current throws `IllegalArgumentException`.

### otherLabel

```java
public static String otherLabel(Locale locale);
public static String otherLabel(Context context,Locale locale);
```

Locale-only returns Korean `기타...` or `Other...` otherwise. The Context overload uses the language catalog. Null locale throws `IllegalArgumentException`.

### input

```java
public EditText input();
```

Returns the owned EditText with initial select-all and four-digit numeric input constraints.

### setLocale

```java
public void setLocale(Locale locale);
```

Relabels title, field, error, and commands while preserving draft text. Null throws `IllegalArgumentException`.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

Relabels year/OK/Cancel captions while retaining entered year text.

### submit

```java
public boolean submit();
```

For a valid1900..2999 year: marks complete, hides keyboard, calls chosen, and returns true. Invalid text shows an error/selects all and returns false. Already-completed calls return false.

### cancel

```java
public void cancel();
```

If incomplete, marks complete, hides keyboard, and calls cancel. Later calls do nothing.

### back

```java
public boolean back();
```

Cancels an incomplete dialog and returns true; returns false when already completed.

### onAttachedToWindow

```java
protected void onAttachedToWindow();
```

Posts a soft-keyboard request for the year input.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Removes the pending keyboard request and hides the keyboard.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Updates only the derived face background, retaining input/selection. Update child fonts through WmControls.restyleTree.

## Listener

```java
public interface Listener
```

### chosen

```java
void chosen(int year);
```

Runs synchronously inside successful submit() for a valid1900..2999 year, after marking complete and hiding the keyboard. Cancellation does not call chosen. The owner removes the dialog View.

### cancel

```java
void cancel();
```

Runs synchronously when cancel() or back() cancels an incomplete dialog. It is not called after successful submit. At most one of chosen or cancel runs once per instance. The owner removes the View.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmYearChooser.java`
