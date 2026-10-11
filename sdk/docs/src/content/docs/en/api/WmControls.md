---
title: "WmControls"
slug: en/api/wmcontrols
---
[API index](/en/api/)

```java
public final class WmControls
```

Applies fonts, themes, authored text bindings, and modal input routing to Android Views. Call factories, bindings, and tree traversal on the UI thread. Dimensions use [WmMetrics](/en/api/wmmetrics/) logical pixels. Styles use weak View keys and persist across later calls on that View. Each binding setter also reapplies existing text/hint/description bindings on that View using the supplied language. A stored format can fail after a new hint/description binding has been recorded.

## Setup

### Constants

```java
public static final int ROW_HEIGHT = 44;
public static final int TAB_HEIGHT = WmMetrics.TAB;
public static final int SURFACE = Color.WHITE;
public static final int FACE = 0xffc9e7e9;
public static final int SHADOW = 0xff808080;
public static final int FRAME = Color.BLACK;
```

ROW_HEIGHT/TAB_HEIGHT are default logical-pixel row/tab heights; other constants are default ARGB colors. For live themes, use `WmPalette.face(selection)` rather than fixed FACE.

## Text binding and language

### bindText

```java
public static void bindText(TextView view,String language,String authored);
public static void bindText(TextView view,String language,String authored,String key);
```

Translates authored UI text immediately and stores its binding for `localizeTree`. A null key uses the authored-text lookup; otherwise [WmStrings](/en/api/wmstrings/) uses the context key. Replaces any format binding. Null authored text or an EditText throws `IllegalArgumentException` before changes. Keep user input/provider values unbound.

### bindFormat

```java
public static void bindFormat(TextView view,String language,String pattern,Object... values);
```

Formats the translated pattern with `String.format`. The Locale comes from the app catalog language selected by `WmStrings.language(view.getContext(), language)`. An unavailable requested tag falls back to a parent language or ko before Locale selection. EditText or null pattern/values throws `IllegalArgumentException`; invalid formats throw `IllegalFormatException`. Initial format validation precedes replacement of the existing binding. A malformed translated format can also fail during a later language change.

Copies the values array shallowly; retains its elements without translation until another `bindText`/`bindFormat`. An element that directly/indirectly refers to the bound View can prevent collection despite the style registry's weak keys.

### bindHint

```java
public static void bindHint(TextView view,String language,String authored);
```

Stores the authored placeholder binding and applies its translated hint immediately. Null authored text throws `IllegalArgumentException`. Accepts EditText without changing its input, caret, or listeners.

### bindDescription

```java
public static void bindDescription(View view,String language,String authored);
```

Stores the authored accessibility description and applies its translated contentDescription immediately. Null authored text throws `IllegalArgumentException`.

### localizeTree

```java
public static void localizeTree(View root,String language);
```

Null root is a no-op. Updates root's registered text/hint/description, traverses children in index order, then calls that View's `LanguageListener`. Includes hidden/disabled Views. Skips setters when the text is unchanged. Recalculates registered tab widths when their explicit LayoutParams.width is nonnegative.

Leaves unbound text and EditText input unchanged. Callback/format exceptions propagate without rolling back earlier updates. Passes the original language argument to callbacks.

## Controls and styling

### text

```java
public static TextView text(Context context,Typeface font,CharSequence value);
```

Returns a new TextView after applying [textStyle](#textstyle) and setting value. Does not create a text binding; register one separately for language changes.

### textStyle

```java
public static void textStyle(TextView view,Typeface font);
```

Applies font and registers it with [WmFonts.attach](/en/api/wmfonts/). Records the bold role from font.isBold(); null font records the regular role. Uses `WmMetrics.FONT` pixels, CENTER_VERTICAL gravity, no font padding, and line spacing 0/1. Text is black or SHADOW when disabled; disables platform touch sounds.

On API ≤15, converts non-Spannable text outside EditText to a SPANNABLE buffer. API ≥21 sets font feature "kern"; API ≥23 sets break strategy 0. API ≥26 disables Android’s default focus highlight overlay; SDK-drawn focus indicators remain. Does not reset minimum sizes, padding, tint, or earlier input/check/button/entry/tab roles.

### input

```java
public static void input(EditText view,Typeface font);
```

Applies [textStyle](#textstyle), then records the input role. Sets a SURFACE background with FRAME-colored `WmMetrics.BORDER`, padding (4,2,4,2), and zero minimum width/height. API ≥21 clears background tint. Does not replace input text, input type, or listeners.

### check

```java
public static void check(CheckBox view,Typeface font);
```

Applies [textStyle](#textstyle), records the check role, and installs the SDK check indicator with intrinsic size `WmMetrics.INDICATOR`. Retains checked state/listeners. Sets a transparent background, resets padding/minimum sizes, then reserves indicator-caption spacing. The gap is integer `WmMetrics.EDGE+round(measure("0"))/2`; adds only the deficit in existing compound padding. API ≥21 clears button tint.

### listCheck

```java
public static void listCheck(CheckBox view,Typeface font);
```

Applies [textStyle](#textstyle) with a two-state list indicator. Its intrinsic cell is 32×32 logical pixels; the 28×28 black/white glyph has a 2-pixel margin on each side. Ordinary [check](#check) uses a 30×30 indicator (`WmMetrics.INDICATOR`).

Retains checked state/listeners. Sets a transparent background, resets padding/minimum sizes, then adds only the compound-padding deficit needed to reserve 32 pixels. Adds no font-derived caption gap, including during restyling. API ≥21 clears button/background tint and the state-list animator.

Only checked changes the indicator image; enabled/pressed/focused states do not change its black/white drawing. Disabled captions follow textStyle; click behavior follows Android CheckBox. Does not change enabled state, row height, or LayoutParams.

### radio

```java
public static void radio(android.widget.RadioButton view,Typeface font);
```

Uses the caption/spacing rules of [check](#check) with the SDK radio indicator. Retains checked state/listeners. API ≥21 clears button/background tint and the state-list animator.

### tabWidth

```java
public static int tabWidth(TextView view);
```

Returns `ceil(width)+32` logical pixels from the current Paint and caption measured by [WmFonts.measure](/en/api/wmfonts/). Recalculate after font/caption changes; this method does not change LayoutParams.

### tab

```java
public static void tab(TextView view,boolean selected,int tabColor);
```

Records the tab role, applies a [WmTabDrawable](/en/api/wmtabdrawable/) background and selected state, and disables touch sounds. Notifies a WmTabStrip parent only when selected changes. Does not set width/click listeners or deselect siblings.

### background

```java
public static StateListDrawable background(int normal,int selected);
```

Returns a new StateListDrawable using ARGB colors. Disabled state takes precedence and uses normal. Enabled pressed/selected/focused states use selected; other states use normal. Enter/exit fade durations are zero.

### entry

```java
public static void entry(TextView view,int selection);
```

Records the entry role and clears button/tab roles. Uses [background](#background) with SURFACE/selection. Enabled pressed/selected/focused text is white, normal text black, and disabled text SHADOW. Disables platform touch sounds without setting font, padding, or click listeners.

### entryBackground

```java
public static void entryBackground(View view,int selection);
```

Applies the SURFACE/selection state background and registers it for later restyling. Disables platform touch sounds without changing TextView text colors.

### faceBackground

```java
public static void faceBackground(View view,int selection);
```

Applies `WmPalette.face(selection)` as the background and registers that color for later restyling.

### button

```java
public static void button(TextView view);
public static void button(TextView view,int selection);
```

Records the push-button role and clears entry/tab roles. Applies textStyle with the current Typeface if no text role exists. Default selection is `0xff149ca7`. Uses CENTER gravity and `WmMetrics.EDGE` padding on each side, plus BORDER for a default button. Clears minimum sizes and platform touch sounds. API ≥21 clears background tint, state animator, and elevation.

Normal face derives from selection with a black BORDER frame. Enabled pressed state is black with white text; focus alone keeps the normal face. Disabled text is `0xffbfbfbf`; even a forced press uses the ordinary frame thickness. Set the click listener and Enter target separately.

### setDefaultButton

```java
public static void setDefaultButton(TextView view,boolean enabled);
```

Requires a registered push button; other/null Views throw `IllegalArgumentException`. An unchanged value is a no-op. True doubles the frame to 2*BORDER and adds BORDER to each padding; false removes that added padding. Disabled appearance retains the ordinary frame. This enabled argument controls the default marker, not View enabled state. Specify the Enter target separately through [dialogButtonKey](#dialogbuttonkey).

### lockFont

```java
public static void lockFont(View view);
```

Locks only this View's registered text-role font against later restyle replacement; does not propagate to descendants. Theme/smoothing updates and custom RestyleListener calls still run, and direct textStyle calls remain effective. There is no public unlock API.

### restyleTree

```java
public static void restyleTree(View root,Typeface font,int selection,int tabColor);
public static void restyleTree(View root,Typeface regular,Typeface bold,int selection,int tabColor);
```

Null root is a no-op. Applies smoothing and registered styles to the current View, traverses children in index order, then calls its `RestyleListener`. Includes hidden/disabled Views. The single-font overload supplies the same regular/bold value.

Unlocked registered text with a bold/button role uses bold when non-null, otherwise regular; no synthetic bold is created. Recalculates explicit nonnegative tab widths and check/radio caption gaps/highlights. Overlapping background roles apply in entryBackground→faceBackground→tab/button/entry order. Text-role branch priority is tab→button→entry→check.

Does not replace the View tree, text bindings, or click listeners. Font/width changes and custom callbacks can affect layout/scroll results. Callback exceptions propagate without rollback.

### select

```java
public static void select(View container,View chosen);
```

Sets selected=true only for the direct child identical to chosen; sets other direct children false. Null/nonchild chosen clears all. Null/non-ViewGroup container is a no-op. Does not move focus, select descendants, or invoke SDK callbacks.

## Back, modals, and keyboard

### back

```java
public static boolean back(View root);
```

Null or a root whose visibility is not VISIBLE returns false. If root implements BackHandler, returns its result immediately without searching descendants even when false. Otherwise searches ViewGroup children in reverse order and stops at the first true. Returns false when none handle it. Does not check enabled state or visibility outside the supplied subtree; callback exceptions propagate.

### blocksOwnerCommands

```java
public static boolean blocksOwnerCommands(View root);
```

Null or a root whose visibility is not VISIBLE returns false. Returns true for a blocking ModalOwner at root, otherwise searches children in reverse order for the first blocker. Returns false if none block. Does not check enabled state or visibility outside the supplied subtree. Use [ownerCommandsAllowed](#ownercommandsallowed) at owner command callbacks.

### inputMethodOwner

```java
public static View inputMethodOwner(View root);
```

Null or a root whose visibility is not VISIBLE returns null. For a blocking ModalOwner at root, repeats the search in its nominated input subtree; null/self/hidden nominations return null. Otherwise searches children in reverse order for the first blocking subtree. Returns root itself when no blocker exists.

The result is an input candidate subtree, not necessarily an editor or enabled View. Does not validate descendant membership. Revisiting the same View after a nomination returns null without falling through to underlying page editors.

### ownerCommandsAllowed

```java
public static boolean ownerCommandsAllowed(View command);
```

Walks command's parent chain. A blocking ModalOwner encountered before WmWindow permits the modal's own command. A WmWindow encountered first supplies its ownerCommandsAllowed result; neither ancestor means true. Null command throws `NullPointerException`. Does not inspect ModalOwner state on command itself or validate command enabled/visibility state; check other command preconditions separately.

### isEnterKey

```java
public static boolean isEnterKey(int keyCode);
```

Returns true only for KEYCODE_ENTER or KEYCODE_NUMPAD_ENTER; takes no event action/modifier state.

### dialogButtonKey

```java
public static boolean dialogButtonKey(ViewGroup dialog,KeyEvent event,TextView cancel);
public static boolean dialogButtonKey(ViewGroup dialog,KeyEvent event,TextView cancel,TextView storedDefault);
```

Null dialog/event throws `IllegalArgumentException`. Handles only Enter/NUMPAD_ENTER/Escape DOWN/UP. Other keys/actions, a blocking child modal, or focus outside this dialog's scope return false. The three-argument overload supplies storedDefault=null.

Escape targets cancel. Enter uses the focused push button; falls back to storedDefault only when focus is dialog itself or registered plain Static text. Static text must be exactly TextView with the text role and no input/check/button/entry/tab role. EditText, other focus, and null focus do not use that fallback.

Returns true when the target is a registered push button owned by this dialog. A button beyond a child WmDialog boundary is excluded. Calls performClick only on a non-canceled DOWN with enabled/VISIBLE state from target through dialog. A disabled/hidden/canceled DOWN or UP still returns true for a valid target without clicking. Modifiers/repeat count are unrestricted, so repeated DOWN can click again. Click exceptions propagate.

## BackHandler

```java
public interface BackHandler
```

### back

```java
boolean back();
```

Return true when the nested control handles Back. [back(root)](#back) returns this result immediately; false does not forward to that control's descendants.

## LanguageListener

```java
public interface LanguageListener
```

### onWmLanguageChanged

```java
void onWmLanguageChanged(String language);
```

[localizeTree](#localizetree) calls synchronously with its language argument after updating this View's bindings and descendants. Update authored custom-drawn UI; keep user/provider data owner-controlled. Exceptions stop traversal.

## RestyleListener

```java
public interface RestyleListener
```

### onWmRestyle

```java
void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

[restyleTree](#restyletree) calls synchronously with the original arguments after applying registered styles to this View and descendants. The implementation updates custom-drawn colors/geometry. lockFont does not suppress this callback; exceptions stop traversal.

## ModalOwner

```java
public interface ModalOwner
```

### ModalOwner.blocksOwnerCommands

```java
boolean blocksOwnerCommands();
```

Return true to block owner commands outside this modal subtree. A recursive search can call this repeatedly and synchronously without caching the result. The modal's own controls remain allowed through [ownerCommandsAllowed](#ownercommandsallowed) ancestor rules.

### ModalOwner.inputMethodOwner

```java
default View inputMethodOwner();
```

Defaults to null, blocking access to underlying page editors while the modal blocks. Nominate the modal's input subtree when needed. Public [inputMethodOwner(root)](#inputmethodowner) repeats modal search there; direct self/hidden nominations and indirect cycles return null.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmControls.java`
