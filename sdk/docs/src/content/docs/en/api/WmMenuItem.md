---
title: "WmMenuItem"
slug: en/api/wmmenuitem
---
[API](/en/api/)

Immutable data for commands, checks, separators, and submenus.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmMenuItem.java`

## Declaration

```text
public final class WmMenuItem
public final int id;
public final String label;
public final boolean enabled;
public final boolean checked;
public final boolean separator;
public static WmMenuItem command(int id,String label);
public static WmMenuItem checked(int id,String label,boolean checked);
public static WmMenuItem submenu(String label,WmMenuItem... children);
public static WmMenuItem separator();
public WmMenuItem withEnabled(boolean enabled);
public WmMenuItem[] children();
public boolean hasChildren();
```

command and checked use caller-assigned IDs delivered to the selection callback. label is required. A submenu needs at least one non-null child. Invalid values throw `IllegalArgumentException`.

Construction copies children; children() returns another copied array. `withEnabled()` returns a new item and preserves the original. A separator is disabled with an empty label and id=0. A submenu also uses id=0. [WmCommandMenu](/en/api/wmcommandmenu/) displays and selects entries.
