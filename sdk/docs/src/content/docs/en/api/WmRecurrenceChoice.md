---
title: "WmRecurrenceChoice"
slug: en/api/wmrecurrencechoice
---
[API index](/en/api/)

```java
public final class WmRecurrenceChoice extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

Combo for once/weekly/monthly/yearly presets derived from start date, plus Edit Pattern. The caller owns popup and wizard lifetime.

### Constructors

```java
public WmRecurrenceChoice(Context context,Typeface font,Locale locale,
                              WmDateTimeField.Value start,WmRecurrenceRule rule,int accent);
```

Requires context/font/locale/start/rule (`IllegalArgumentException`). Displays preset0..3 for matching no-end rules; other rules select Edit index4.

### choice

```java
public WmChoice choice();
```

Returns the owned WmChoice. Commit through choose(index) to update rule and callback together.

### rule

```java
public WmRecurrenceRule rule();
```

Returns the current immutable rule; invoking Edit does not replace it.

### startValue

```java
public WmDateTimeField.Value startValue();
```

Returns the last owner-supplied start value; owners may defer updates during incomplete time input.

### setRule

```java
public void setRule(WmRecurrenceRule rule);
```

Silently replaces the model and recomputes preset captions/selection. Null throws `IllegalArgumentException`. Does not notify rule changes.

### setStart

```java
public void setStart(WmDateTimeField.Value start);
```

Changes only the start date/time and recomputes preset captions/selection. Retains rule() without a rule-change callback. Moving a Monday weekly rule’s start to Tuesday retains Monday recurrence; it can show Edit(index4) because the rule differs from the new start’s preset. Null start throws IllegalArgumentException.

### setLocale

```java
public void setLocale(Locale locale);
```

Relabels month/weekday/preset captions while retaining rule/start. Null throws `IllegalArgumentException`.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

Passes the shared catalog language’s Locale to setLocale; state preservation and popup handling follow that method’s contract.

### setOnRuleChanged

```java
public void setOnRuleChanged(OnRuleChangedListener listener);
```

Synchronously reports a new rule only when a preset commit changes it. Null removes the listener.

### setOnEdit

```java
public void setOnEdit(Runnable callback);
```

Registers the index4 Edit callback, including repeated selections. Null leaves it unopened.

### choose

```java
public void choose(int index);
```

0:none;1:weekly on start weekday;2:monthly on start day;3:yearly on start month/day;4:Edit callback. Presets use interval1/no end. Disabled controls ignore selection; invalid indices throw `IndexOutOfBoundsException`. Edit does not change the rule.

### setEnabled

```java
public void setEnabled(boolean enabled);
```

Propagates enabled state to owned inputs and blocks user changes while disabled.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Restyles the owned WmChoice and requests layout.

## OnRuleChangedListener

```java
public interface OnRuleChangedListener
```

### changed

```java
void changed(WmRecurrenceRule rule);
```

Synchronously reports a new rule only when a preset commit changes it. Null removes the listener.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmRecurrenceChoice.java`
