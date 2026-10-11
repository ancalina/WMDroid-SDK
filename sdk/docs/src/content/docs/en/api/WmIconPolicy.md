---
title: "WmIconPolicy"
slug: en/api/wmiconpolicy
---
[API](/en/api/)

Value object for per-Activity adaptive-icon selection and hidden-app lists.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmIconPolicy.java`

## Declaration

```text
public final class WmIconPolicy
public final boolean enabled;
public final String mode;
public final String json;
public WmIconPolicy(boolean enabled,String mode,String json);
public static WmIconPolicy read(SharedPreferences preferences);
public boolean allows(String component);
public static boolean hidden(SharedPreferences preferences,String component);
public static String canonical(String value);
```

mode accepts `all`, `blacklist`, or `whitelist`; other values select all. json is an array of component strings; null becomes `[]`. Invalid JSON is caught and leaves the list empty.

`allows(component)` returns false when disabled, null, or empty. all allows remaining components; whitelist allows listed components; blacklist allows unlisted components. Comparison uses canonical component names.

`read()` reads preferences `android_icons_unframed`(false), `android_icons_mode`(all), and `android_icons_list`([]). `hidden()` checks the separate `hidden_apps` list. `canonical()` normalizes ComponentName strings, retains unparseable strings, and returns an empty string for null.
