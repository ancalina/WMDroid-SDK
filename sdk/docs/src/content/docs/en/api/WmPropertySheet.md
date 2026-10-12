---
title: "WmPropertySheet"
slug: en/api/wmpropertysheet
---
[API](/en/api/)

Manages retained pages, bottom tabs, selection, and Apply or Cancel gates.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmPropertySheet.java`

```text
public final class WmPropertySheet extends LinearLayout implements WmControls.BackHandler,WmControls.RestyleListener
public static final int PSN_SETACTIVE = -200;
public static final int PSN_KILLACTIVE = -201;
public static final int PSN_APPLY = -202;
public static final int PSN_RESET = -203;
public static final int PSN_QUERYCANCEL = -209;
public static final int PSNRET_NOERROR = 0;
public static final int PSNRET_INVALID = 1;
public static final int PSNRET_INVALID_NOCHANGEPAGE = 2;
```

## OnPageChangedListener

```text
public interface OnPageChangedListener
void selected(int index);
```

## OnPageNotificationListener

```text
public interface OnPageNotificationListener
int notifyPage(int index,int notification);
default int notifyPage(int index,int notification,int parameter);
```

## Constructor, addPage

```text
public int addPage(CharSequence title,View page);
public int addPage(int pageId,CharSequence title,View page);
public WmPropertySheet(Context context,Typeface font,int tabColor);
```

Construct and modify on the UI thread; callbacks run synchronously there. The body fills remaining height above the `40`-logical-pixel tab strip. The owner supplies page scrolling, commands, and data persistence.

Title and View must be non-null; the View must be unattached. Invalid input throws `IllegalArgumentException`. Pages fill the body. The first page becomes selected without page notifications or the selected callback. Other pages become GONE but remain attached.

Explicit pageId values are `1–65535`; duplicates are allowed. The automatic-ID overload first tries `pageCount()+1`, then the first unused ID from `1`. Exhausting IDs throws `IllegalStateException`. Both overloads return the zero-based page index. Adding during notification throws `IllegalStateException`.

## selectPage, trySelectPage, restoreSelectedPage

```text
public void selectPage(int index);
public void restoreSelectedPage(int index);
public boolean trySelectPage(int index);
```

`trySelectPage` returns false during a page notification, before validating the index. Otherwise, an out-of-range index throws `IndexOutOfBoundsException`. Selecting the current page returns true without notifications.

Selection orders KILLACTIVE on the current page, SETACTIVE on candidate pages, UI commit/focus, then the selected callback. Nonzero KILLACTIVE vetoes the operation. SETACTIVE resolves as follows.

| Return | Result |
| --- | --- |
| `0` | Accept the candidate. |
| `-1` | Try the next index; from the last page, return to the currently selected index. |
| Matching pageId | Redirect to the first page with that ID. |
| Other value | Accept the candidate. |

Resolution stops after at most `pageCount()` notifications; unresolved redirects return false. An accepted current page does not invoke the selected callback. The notification listener is captured for the operation; replacing it affects later operations. Notification exceptions propagate, and the notification guard clears in `finally`.

Activation focus handling runs only when selection changes and the window is attached. If the tab strip previously had focus, requests focus on the selected tab. Otherwise, requests forward focus on the page, falling back to its tab if focus fails. A focused EditText selects all text before the app callback. The callback can select again, so the index after return can differ from the committed index reported to it.

`selectPage` discards the boolean result. `restoreSelectedPage` validates and commits without notifications, callbacks, or activation focus handling. During notification, restoration throws `IllegalStateException` before index validation.

## applyPages, cancelPages

```text
public boolean applyPages(boolean closing);
public boolean cancelPages();
public boolean cancelPages(boolean exit);
```

Empty sheets and notification reentry return false. Apply first sends KILLACTIVE to the selected page; Cancel sends QUERYCANCEL. A nonzero gate result returns false without processing pages.

After approval, pages receive APPLY or RESET in index order, including pages whose changed flag is false. Parameter `0` means Apply/Cancel; `1` means OK/exit. `cancelPages()` uses `exit=false`. The three-argument listener default forwards to the two-argument method.

Return `0` clears that page's changed flag. Any nonzero result stops processing and returns false. Result `2` leaves page selection unchanged; every other nonzero value attempts SETACTIVE resolution from the failing page. Earlier successful pages stay clean. Listener exceptions propagate; earlier effects and flags are not rolled back.

After all pages succeed, `applyPages(false)` resolves SETACTIVE again from the current page. This can redirect selection or return false if resolution fails. `applyPages(true)` and Cancel do not perform this final reactivation. Selection changes use the same focus/callback rules as `trySelectPage`.

True reports operation approval. The owner performs any Activity/window completion; the sheet does not close it.

## setPageChanged, isPageChanged, hasChanges, page access

```text
public int pageCount();
public View getClientView();
public int selectedPage();
public void setPageChanged(int index,boolean changed);
public boolean isPageChanged(int index);
public boolean hasChanges();
public void setPageTitle(int index,CharSequence title);
```

`pageCount` returns the number of attached pages; `selectedPage` is `-1` before the first page is added. `getClientView` returns the retained body FrameLayout without tabs; do not remove or reparent it.

`setPageChanged` only sets a flag. `isPageChanged` reads it; `hasChanges` reports whether any flag is true. These flags do not automatically track edits. Index-taking methods here throw `IndexOutOfBoundsException` for an invalid index.

`setPageTitle` checks the index, then rejects a null title with `IllegalArgumentException`. It displays the supplied text without translation and recalculates tab width.

## listeners, notifyPage, back, onWmRestyle

```text
public boolean dispatchKeyEvent(KeyEvent event);
public void setPageNotificationListener(OnPageNotificationListener listener);
public void setOnPageChangedListener(OnPageChangedListener listener);
public boolean back();
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

Both listener setters accept null to unregister. OnPageChangedListener receives an index only after the selection changes and the notification guard clears. It may start another selection. OnPageNotificationListener returns the selection/apply/cancel decisions described above.

`dispatchKeyEvent` handles Ctrl+Tab and Ctrl+Page Down forward, Ctrl+Shift+Tab and Ctrl+Page Up backward, wrapping at either end. On DOWN it calls `trySelectPage`; it consumes the event even when selection is vetoed. Matching UP is consumed. Disabled sheets, empty sheets, other keys, and other actions delegate to the superclass. Additional modifiers do not disable this Ctrl-key path.

`back` delegates to `WmControls.back` for the selected page, or returns false when empty. It does not select another page or call Cancel.

`onWmRestyle` updates the tab strip, tab fonts/font bindings, widths, colors, and page visibility. It retains selection and changed flags. It does not restyle page content; the owner must apply page styling separately.
