---
title: "WmShell"
slug: en/api/wmshell
---
[API](/en/api/)

Queries launcher appearance, Start menu, and resource state and creates shell-command Intents.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmShell.java`

```java
public final class WmShell
public static final String PACKAGE = "me.ancal.wmdroid";
public static final String ACTION = "me.ancal.wmdroid.sdk.SHELL_ACTION";
public static final String COMMAND = "command";
public static final String WM_STYLE = "me.ancal.wmdroid.sdk.WM_STYLE";
public static final Uri STATE = Uri.parse("content://me.ancal.wmdroid.shell/state");
public static final Uri MENU = Uri.parse("content://me.ancal.wmdroid.shell/menu");
public static final Uri START_BACKGROUND = Uri.parse("content://me.ancal.wmdroid.shell/start/background");
public static final Uri RESOURCES = Uri.parse("content://me.ancal.wmdroid.shell/resources");
public static final String[] STATE_COLUMNS = {"font", "hybrid", "accent", "language", "clock", "analog", "retro", "delay", "scan", "lines", "scale", "repaint", "hold", "icons_unframed", "icons_mode", "icons_list", "start_text_color", "ui_scale_percent", "screen_mode", "font_bold", "touch_sound_level", "start_texture", "start_tint", "start_theme_color", "font_smoothing"};
public static final String[] MENU_COLUMNS = {"id", "parent", "title", "icon", "component", "command", "folder"};
```

## Entry

```java
public static final class Entry
public final String id;
public final String parent;
public final String title;
public final String icon;
public final String component;
public final String command;
public final boolean folder;
public Entry(String id,String parent,String title,String icon,String component,String command,boolean folder);
```

## State

```java
public static final class State
public WmIconPolicy iconPolicy = new WmIconPolicy(false, "all", "[]");
public String font = WmFonts.DEFAULT_SOURCE;
public String fontBold = WmFonts.DEFAULT_SOURCE;
public String language = "ko";
public boolean hybrid = true;
public boolean clock = true;
public boolean analog = false;
public boolean fontSmoothing = true;
public int accent = -15355972;
public boolean iconsUnframed;
public boolean available;
public boolean retro = true;
public boolean scan = true;
public boolean repaint = false;
public boolean hold = true;
public int delay = 120;
public int lines = 64;
public int scale = 1000;
public String startTexture = "";
public boolean startTint;
public int startThemeColor = -15355972;
public int touchSoundLevel = 0;
public int startTextColor = Color.BLACK;
public int uiScalePercent = 100;
public String screenMode = "full";
```

## state(Context)

```java
public static State state(Context context);
```

Returns a new State snapshot. available becomes true when the provider returns its first row. Before querying, analog loads this app’s clock preference; other fields start with the declared defaults. A query or decoding failure retains values already read, including available. Results do not update automatically; observe STATE and query again after changes. delay is in ms; lines is the logical scan-line count; scale is a timing multiplier with 1000=1×; uiScalePercent is 50–100%. font and fontBold are source strings; color fields use ARGB.

## menu(Context)

```java
public static List<Entry> menu(Context context);
```

Returns a new Entry List. An unavailable provider yields an empty list; a failure during iteration retains rows already read. Entry.parent is the parent ID; component is a flattened ComponentName; command is a shell command; folder marks folders. The caller may change the List.

## startBackground(Context[, State])

```java
public static Drawable startBackground(Context context);
public static Drawable startBackground(Context context,State state);
```

Returns a menu-background Drawable. It reads a local pack template or START_BACKGROUND PNG, then falls back to a selected-color or white ColorDrawable. Prepare it on a worker because it decodes and transforms images. The State overload uses its texture and tint for local templates and the ColorDrawable fallback. The PNG route returns the provider’s image without applying snapshot tint.

## prepareLaunch(Context, Intent)

```java
public static Intent prepareLaunch(Context context,Intent original);
```

Copies the Intent. When target Activity WM_STYLE metadata is true, removes NEW_TASK, RESET_TASK_IF_NEEDED, MULTIPLE_TASK, and NEW_DOCUMENT and adds NO_ANIMATION. Other flags remain. The original is unchanged.

## launchWithoutAnimation(Activity, Intent)

```java
public static void launchWithoutAnimation(android.app.Activity owner,Intent original);
```

Starts the prepareLaunch result and requests zero transition animations. Call on the main thread. The caller handles launch exceptions. Platform and OEM Activity-transition policies can still affect the result.

## command(String)

```java
public static Intent command(String value);
```

Returns an explicit TodayActivity Intent with a COMMAND extra and NEW_TASK | CLEAR_TOP flags. Launching can return to an existing launcher task and remove Activities above TodayActivity. The caller launches it. See [shell usage](/en/applets/) for commands.

## clockMode(Context, boolean)

```java
public static void clockMode(Context context,boolean analog);
```

Stores the shared clock mode. If the host cannot save it, uses this app's local preference.

## rememberMusic(Context, int) · savedMusic(Context, int)

```java
public static void rememberMusic(Context context,int current);
public static int savedMusic(Context context,int fallback);
```

Store or read the current media volume. rememberMusic does not change system volume. Without the host, uses local memory; absent memory returns the savedMusic fallback. Pass the actual AudioManager level as current.
