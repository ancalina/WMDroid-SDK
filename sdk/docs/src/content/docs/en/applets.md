---
title: "Applets and shell"
slug: en/applets
---


## Create an applet

```java
package org.example.demo;

import android.os.Bundle;
import android.widget.LinearLayout;
import me.ancal.wmdroid.sdk.WmApplet;

public final class DemoActivity extends WmApplet {
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        caption("Demo");
        content.addView(label("Ready"),
            new LinearLayout.LayoutParams(-1, -2));
        closeButton(false, null);
        softKeys("Close", this::finish, "", null);
    }
}
```

```xml
<activity android:name=".DemoActivity"
    android:exported="true"
    android:screenOrientation="portrait"
    android:theme="@style/WmAppletTheme">
    <meta-data android:name="me.ancal.wmdroid.sdk.WM_STYLE"
        android:value="true" />
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

Put the manifest element inside `<application>`. Copy `sdk/templates/res/values/wm_applet_theme.xml` into the app's `res/values/` directory. Set `android:theme="@style/WmAppletTheme"` on the app or Activity.

## Subclass API

```text
protected LinearLayout content;
protected Typeface regular, bold;
protected int accent;
protected WmWindow shellWindow;
protected final void caption(String value);
protected final TextView label(String value);
protected final void softKeys(String left, Runnable leftAction,
                              String right, Runnable rightAction);
protected final void closeButton(boolean ok, Runnable action);
protected void onShellStateChanged(WmShell.State state);
protected void onResourcesChanged();
```

Call UI APIs on the main thread. Use `content` only after `super.onCreate(saved)`.

- `label()`: returns an unattached TextView with the current regular face.
- `softKeys()`: replaces bottom commands and closes the open command menu. A null callback disables its command.
- `closeButton()`: `ok=false` shows X; `true` shows OK. A null action finishes the Activity. Implement saving in the callback. An open `WmInputDialog` takes priority with OK and accept behavior.
- `onShellStateChanged()`: update app display after shell-state application. Retain editor values.
- `onResourcesChanged()`: refresh artwork caches after pack changes. Do not recreate editors.

## State and restoration

While resumed, `WmApplet` observes launcher font, theme, language, scale, and menu changes. Implement `WmControls.RestyleListener` and `LanguageListener` for custom containers.

The app owns data and draft storage. Use `onSaveInstanceState()` or app storage. If a control supplies `saveInteractionState()`, restore it with the matching `restoreInteractionState()`.

Start uses the launcher menu snapshot. Host state synchronization, resource synchronization, and host commands require an available launcher.

Save values in app storage or instance state, and pair interaction save/restore on a control of the same type/configuration. This fragment uses an app-owned WmDateField `dateField` on the UI thread. In onCreate, recreate it with the same range, locale, and format before restoring value→interaction. The app handles IllegalArgumentException for out-of-range values or invalid state. restoreInteractionState does not request focus.

```java
// onSaveInstanceState, before the superclass call:
out.putString("date_value", me.ancal.wmdroid.sdk.WmDateField.formatValue(dateField.value()));
out.putBundle("date_interaction", dateField.saveInteractionState());
// onCreate, after recreating dateField and its configuration:
if (saved != null && saved.containsKey("date_value")) {
    dateField.setValue(me.ancal.wmdroid.sdk.WmDateField.parseValue(saved.getString("date_value")));
    dateField.restoreInteractionState(saved.getBundle("date_interaction"));
}
```

[WmDateField contracts](/en/api/wmdatefield/#saveinteractionstate)

## Shell commands

[WmShell.command](/en/api/wmshell/#commandstring) only creates an Intent; the caller launches it. The current WMDroid host’s TodayActivity.runShellCommand handles the following values. Other hosts/versions need not support them; unknown values execute no command. Commands can be ignored while contacts are saving.

| command | Target |
| --- | --- |
| `today` | Close current dialogs; a dismissal veto retains them. |
| `tasks`, `calendar`, `contacts`, `notes` | Tasks, calendar, contacts, notes. |
| `messages`, `phone`, `browser`, `media` | Messages, phone, browser, media. |
| `programs`, `settings`, `help` | Programs, settings, help. |

## Runnable example connection

Guide snippets are explanatory fragments. Complete Activity/Provider/Manifest integration is in `sample/` in the separate `WMDroid-examples-sources.zip`: `sample/app/src/main/java/me/ancal/wmdroid/sample/SampleActivity.java`, `SampleProvider.java`, and `sample/app/src/main/AndroidManifest.xml`. The SDK source archive provides `sdk/templates/res/values/wm_applet_theme.xml`. See [project build and distribution inputs](/en/getting-started/#examples-and-distribution-inputs).
