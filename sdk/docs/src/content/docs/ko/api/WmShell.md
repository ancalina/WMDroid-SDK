---
title: "WmShell"
slug: ko/api/wmshell
---
[API](/ko/api/)

런처의 외형, 시작 메뉴, 리소스 상태를 조회하고 shell 명령 Intent를 만듭니다.

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

새 State snapshot을 반환합니다. provider의 첫 행을 읽기 시작하면 available이 true가 됩니다. 조회 전에 analog는 이 앱의 시계 preference에서 읽고 다른 필드는 선언된 기본값으로 시작합니다. 조회, 필드 해석 실패 시 이미 읽은 값과 available은 유지합니다. 조회 결과는 자동 갱신하지 않습니다. STATE URI 변경을 관찰한 뒤 다시 조회하세요. delay는 ms, lines는 논리 scan 줄 수, scale은 1000 기준 시간 배수, uiScalePercent는 50–100%입니다. font, fontBold는 source 문자열, 색 필드는 ARGB입니다.

## menu(Context)

```java
public static List<Entry> menu(Context context);
```

새 Entry List를 반환합니다. provider가 없으면 빈 List이며, 순회 도중 실패하면 이미 읽은 항목을 유지합니다. Entry.parent는 상위 id, component는 flattened ComponentName, command는 shell 명령, folder는 폴더 여부입니다. caller가 List를 변경할 수 있습니다.

## startBackground(Context[, State])

```java
public static Drawable startBackground(Context context);
public static Drawable startBackground(Context context,State state);
```

메뉴 배경 Drawable을 반환합니다. 로컬 팩 template 또는 START_BACKGROUND PNG를 조회하고 없으면 선택색, 흰색 ColorDrawable을 반환합니다. decode, 색 변경을 하므로 worker에서 준비하세요. State overload는 로컬 template과 ColorDrawable fallback에 snapshot의 texture, tint를 사용합니다. PNG 경로는 provider 이미지를 반환하며 snapshot의 tint를 다시 적용하지 않습니다.

## prepareLaunch(Context, Intent)

```java
public static Intent prepareLaunch(Context context,Intent original);
```

Intent를 복제합니다. 대상 Activity의 WM_STYLE metadata가 true이면 NEW_TASK, RESET_TASK_IF_NEEDED, MULTIPLE_TASK, NEW_DOCUMENT를 제거하고 NO_ANIMATION을 추가합니다. 그 외 flags는 유지합니다. 원본을 수정하지 않습니다.

## launchWithoutAnimation(Activity, Intent)

```java
public static void launchWithoutAnimation(android.app.Activity owner,Intent original);
```

prepareLaunch 결과를 시작하고 전환을 0으로 지정합니다. main thread에서 호출하세요. 시작 실패 예외는 caller가 처리해야 합니다. 플랫폼, OEM의 Activity 전환 정책까지 보장하지 않습니다.

## command(String)

```java
public static Intent command(String value);
```

COMMAND extra와 NEW_TASK | CLEAR_TOP flags를 가진 TodayActivity의 명시적 Intent를 반환합니다. 실행 시 기존 런처 task로 돌아가 TodayActivity 위의 Activity를 제거할 수 있습니다. 실행은 caller가 담당합니다. 명령 목록은 [shell 사용](/ko/applets/)을 참고하세요.

## clockMode(Context, boolean)

```java
public static void clockMode(Context context,boolean analog);
```

공유 시계 모드를 저장합니다. host 저장이 불가능하면 이 앱의 로컬 preference를 사용합니다.

## rememberMusic(Context, int), savedMusic(Context, int)

```java
public static void rememberMusic(Context context,int current);
public static int savedMusic(Context context,int fallback);
```

현재 media 음량을 저장, 조회합니다. rememberMusic은 시스템 음량을 변경하지 않습니다. host가 없으면 로컬 저장값, 값도 없으면 savedMusic의 fallback을 반환합니다. current는 AudioManager에서 읽은 실제 level을 전달하세요.
