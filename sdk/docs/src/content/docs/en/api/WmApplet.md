---
title: "WmApplet"
slug: en/api/wmapplet
---
[API](/en/api/)

Activity base with a shared title bar, command bar, and Start menu.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmApplet.java`

```text
public abstract class WmApplet extends Activity
public static final String LANGUAGE = "me.ancal.wmdroid.sdk.LANGUAGE";
public static final String ACCENT = "me.ancal.wmdroid.sdk.ACCENT";
public static final String FONT = "me.ancal.wmdroid.sdk.FONT";
public static final String HYBRID = "me.ancal.wmdroid.sdk.HYBRID";
protected LinearLayout content;
protected Typeface regular;
protected Typeface bold;
protected int accent;
protected WmWindow shellWindow;
```

## onCreate(Bundle), lifecycle

```text
public void onCreate(Bundle saved);
protected void onResume();
protected void onPause();
protected void onDestroy();
```

After super.onCreate, add body Views to content. regular, bold, accent, and shellWindow are available then. content is a vertical LinearLayout. See [Applets](/en/applets/) for registration. Views, hooks, and callbacks use the main thread.

Resume updates host state, fonts, and resources and starts clock, tray, and STATE observation. Pause stops observation and timers and closes menus and popups. Destroy shuts down the font worker. Call the superclass in lifecycle overrides.

## onShellStateChanged(State), onResourcesChanged()

```text
protected void onShellStateChanged(WmShell.State state);
protected void onResourcesChanged();
```

Hooks run after appearance state is applied. Update body language and app-owned artwork caches here. Editors can remain attached. Resource synchronization runs on a worker. The first font can load synchronously on the main thread during onCreate state application. Later font switches with unprepared caches use worker preparation. Hooks run on the main thread. onResourcesChanged runs when the resource revision observed by chrome changes, including initial application. onShellStateChanged follows on every state application, even with unchanged values. It is not a font-ready notification; reapplying state after worker completion can invoke the hooks again.

## caption(String), label(String), entry(String, Runnable)

```text
protected final void caption(String value);
protected final TextView label(String value);
protected final TextView entry(String value,Runnable action);
```

caption updates the title. label returns a new TextView with the shared font. entry returns a new TextView with selection background, padding, and click action. action is required. The caller attaches returned Views to content.

## closeButton(boolean, Runnable)

```text
protected final void closeButton(boolean ok,Runnable action);
```

ok=true uses OK; false uses X. A null action calls finish. An active WmInputDialog uses OK and accepts input. A blocking modal owner prevents the close action.

## softKeys(...), softKeyMenu(...)

```text
protected final TextView softKey(String value,boolean right,Runnable action);
protected final void softKeys(String left,Runnable leftAction,String right,Runnable rightAction);
protected final void softKeyMenu(boolean right,String caption,Runnable action);
```

softKeys replaces left and right commands and closes the current command menu. A null action disables the command. softKeyMenu opens on DOWN. softKey returns a separate TextView without attaching it to the footer.

## showCommandMenu(...), dismissCommandMenu()

```text
protected final WmCommandMenu showCommandMenu(boolean right,String[] labels,int selected,WmRadioMenu.Selection action);
protected final WmCommandMenu showCommandMenu(boolean right,WmMenuItem[] items,WmRadioMenu.Selection action);
protected final void dismissCommandMenu();
```

The flat overload delivers row indices; the tree overload delivers leaf IDs. Opens after closing Start, popups, and the previous menu. The returned menu lives until dismissal or pause. selected=-1 omits the radio marker. Follow [WmCommandMenu](/en/api/wmcommandmenu/) for argument rules.

## showPopup(String, View, int, int), dismissPopup()

```text
protected final WmPopup showPopup(String caption,View body,int width,int anchor);
protected final void dismissPopup();
```

width is 1–480; anchor is a logical-surface x coordinate. Out-of-range width throws `IllegalArgumentException`. body must be unattached. Closes Start, the previous popup, and command menus, then restyles body. Outside taps, Back, pause, and Start dismiss the popup. Cancel or ignore asynchronous updates after dismissal.

## onTray(int, int), launchSystem(Intent)

```text
protected void onTray(final int kind,int anchor);
protected void launchSystem(Intent intent);
```

The default tray hook opens connection, volume, power, or phone panels. kind uses WmTraySlot constants. launchSystem attempts a zero-animation system Activity, then basic Settings if launch fails. Override for app-specific navigation.

## onClock(), onClockMenu(), onClockPanelShown(WmClockPanel)

```text
protected void onClock();
protected void onClockMenu();
protected void onClockPanelShown(WmClockPanel panel);
protected final void analogClock(boolean analog);
```

The default clock popup closes after 7 seconds. Supply this app's permission-backed appointment data in onClockPanelShown. Cancel or ignore asynchronous updates after panel detachment. The clock menu saves the shared analog mode. analogClock changes only the current applet display.

## shade(int), tab(...), tabSelected(...), viewport()

```text
protected final int shade(int original);
protected final TextView tab(String value,boolean selected,Runnable action);
protected final void tabSelected(TextView tab,boolean selected);
protected final WmViewport viewport();
```

shade changes an authored ARGB hue to accent; gray accents use grayscale. tab returns a new tab TextView. tabSelected updates an existing selected marker. viewport returns the WmViewport for scale and insets.

## onBackPressed(), finish()

```text
public void onBackPressed();
public void finish();
```

Back handles child command menu, popup, Start, body BackHandler, then finish, in that order. finish requests zero transition animation.
