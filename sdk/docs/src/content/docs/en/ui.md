---
title: "Shared UI"
slug: en/ui
---

## Coordinates and scale

[WmMetrics](/en/api/wmmetrics/) defines logical VGA coordinates. [WmViewport](/en/api/wmviewport/) applies whole-surface scaling.
Never apply `sp` or a second device scale to children. Smaller UI scales can increase logical width beyond 480. Use the current width for the right edge.

| Constant | Logical pixels |
| --- | ---: |
| `WIDTH` | 480 |
| `BAR` | 52 |
| `FONT` / `LINE` | 24 / 30 |
| `INSET` / `BORDER` / `EDGE` | 8 / 2 / 4 |
| `TEXT_LEFT` / `ICON` | 56 / 32 |
| `TODAY_FONT` / `TODAY_LINE` | 23 / 28 |
| `TODAY_MIN` / `TAB` | 40 / 40 |

## Control selection

| Need | API | Selection criterion |
| --- | --- | --- |
| Window chrome | [WmWindow](/en/api/wmwindow/), [WmTitleBar](/en/api/wmtitlebar/), [WmCommandBar](/en/api/wmcommandbar/) | Compose title, client, and bottom commands. Use [WmApplet](/en/api/wmapplet/) for the Activity shell. |
| Tabs | [WmTabStrip](/en/api/wmtabstrip/), [WmPropertySheet](/en/api/wmpropertysheet/) | Use the sheet for retained pages and Apply/Cancel gates. |
| Scrolling | [WmScrollView](/en/api/wmscrollview/), [WmScrollBar](/en/api/wmscrollbar/) | Use the scrollbar alone for a caller-owned pixel-offset Host. |
| Text and input styles | [WmControls](/en/api/wmcontrols/) | Style Android text, input, check, and radio Views. |
| Choice fields | [WmChoice](/en/api/wmchoice/), [WmEditableChoice](/en/api/wmeditablechoice/) | Closed selection or editable input; the caller owns the popup. |
| Text-cell selection | [WmBoxSelect](/en/api/wmboxselect/) | Single or multiple selection stored as a bit mask. |
| Command menus | [WmMenuItem](/en/api/wmmenuitem/), [WmCommandMenu](/en/api/wmcommandmenu/) | Commands, checks, separators, and submenus. |
| Tailed popups | [WmPopup](/en/api/wmpopup/), [WmBubble](/en/api/wmbubble/) | Popup container and background Drawable. |
| Modal dialogs | [WmDialog](/en/api/wmdialog/), [WmMessageBox](/en/api/wmmessagebox/) | Embedded dialog or acknowledgment/Yes-No choice. |

Mixing Android default ripples or theme widgets can change press feedback and dimensions. Prefer shared controls.

## Compose a minimal screen

Run this inside [WmApplet](/en/api/wmapplet/).onCreate after the superclass call. It uses the parent’s current available width rather than fixing it at480px.

```java
android.widget.LinearLayout row = new android.widget.LinearLayout(this);
row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
content.addView(row, new android.widget.LinearLayout.LayoutParams(-1, -2));
android.widget.TextView status = me.ancal.wmdroid.sdk.WmControls.text(
        this, regular, "Ready");
row.addView(status, new android.widget.LinearLayout.LayoutParams(0, -2, 1));
```

Use WmChoice for selection without direct input, WmEditableChoice for input plus candidates, and WmBoxSelect for cell bit masks. The caller creates, positions, and removes popups. Follow preparation→bindDropdown→attachment in the [WmEditableChoice example](/en/api/wmeditablechoice/#open-binddropdown-isdroppeddown). See [WmControls.restyleTree](/en/api/wmcontrols/#restyletree) for callback and state-preservation limits.

## Today layout

[WmToday](/en/api/wmtoday/) provides `Layout.register()` and `Layout.arrange()`.
`WmToday.Row`: `setCaption()`, `setCaptionLines()`, and `setAppointments()`.

```text
public Module register(String id, View content, int height, Divider divider);
public void arrange(List<String> preferredOrder, Set<String> hidden);
public void setCaption(CharSequence text);
public void setCaptionLines(CharSequence text);
public void setAppointments(List<Appointment> entries);
```

Create and change Views on the main thread.

- `register()`: returns a module. Sets content LayoutParams and background without attaching it to the Layout. The same ID replaces its registration.
- `register()` inputs: non-empty ID, non-null content and divider, height≥0. Violations throw `IllegalArgumentException`.
- `arrange()`: applies the preferred order. Ignores unknown or duplicate IDs. Appends new registrations. Hides IDs in hidden.
- `setCaption()`: measures and lays out normal row text.
- `setCaptionLines()`: retains explicit lines. Clips horizontal overflow without wrapping or ellipsis.
- `setAppointments()`: lays out subjects and details separately. Null or empty lists and null entries throw `IllegalArgumentException`.

| Value | Behavior |
| --- | --- |
| `Divider.BETWEEN` | No divider below the last visible module. |
| `Divider.WHEN_FOLLOWED` | The compatibility behavior matches `BETWEEN`. |
| `Divider.ALWAYS` | Also draws below the final module. |
| `TextStyle.STANDARD` | The default Today text contract. |
| `TextStyle.NATIVE_LINES` | Reserves a 26-logical-pixel right gutter. |

Never apply appointment-specific heights to other row types. The host owns dividers outside module content.

## Style, language, and commands

`WmControls`:

```text
public static void restyleTree(View root, Typeface regular, Typeface bold,
                               int selection, int tabColor);
public static void localizeTree(View root, String language);
public static boolean back(View root);
```

Update shared styles with `restyleTree()`. `localizeTree()` updates only bound text. Never translate app data or user input. Implement `RestyleListener` and `LanguageListener` for custom Views.

`ModalOwner` blocks owner commands. `BackHandler` handles internal Back. `back(root)` searches handlers and returns whether they handled Back. Never assign the same save behavior to Back and OK.
