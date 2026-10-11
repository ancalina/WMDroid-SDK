---
title: "WmWizard"
slug: en/api/wmwizard
---
[API](/en/api/)

Manages retained step pages with Cancel, Back, Next, and Finish commands.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmWizard.java`

```text
public final class WmWizard extends WmDialog implements WmControls.BackHandler,WmControls.RestyleListener
```

## NavigationGate

```text
public interface NavigationGate
boolean allow(int from,int to);
```

## OnPageChangedListener

```text
public interface OnPageChangedListener
void selected(int index);
```

## Constructor · addPage · selectPage · page access

```text
public int addPage(View page);
public void selectPage(int index);
public WmWizard(Context context,Typeface font,Locale locale,int accent);
public int pageCount();
public int selectedPage();
```

font and locale are required. page must be unattached; addPage returns its index. Invalid arguments throw `IllegalArgumentException`; invalid indices throw `IndexOutOfBoundsException`. The first page is selected automatically. selectPage bypasses the gate. An actual page change resets finishHere=false, refreshes display, then calls selected synchronously. Selecting the current index does nothing and calls no callback.

## Navigation · completion callbacks

```text
public void setFinishHere(boolean finishHere);
public void setNavigationGate(NavigationGate gate);
public void setOnPageChangedListener(OnPageChangedListener listener);
public void setOnCancel(Runnable listener);
public void setOnFinish(Runnable listener);
public TextView cancelButton();
public TextView errorField();
public void setError(CharSequence message);
public TextView backButton();
public TextView nextButton();
public TextView finishButton();
public boolean back();
```

Next and Back first call NavigationGate.allow(from,to); false prevents navigation. setFinishHere(true) shows Finish early for the current step. Actual page changes clear it; set it again in selected for another step that permits early completion. Cancel and Finish run caller callbacks without closing the dialog. setError hides null or empty messages and displays others. Button accessors return internal TextViews. BackHandler first delegates to the current page, then attempts the preceding step.

## saveInteractionState · validateInteractionState · restoreInteractionState

```text
public Bundle saveInteractionState();
public void validateInteractionState(Bundle state);
public void restoreInteractionState(Bundle state);
```

The schema=1 Bundle stores page count, selection, finishHere, error, and requested scroll positions only when the page passed directly to addPage is itself a WmScrollView. Other pages store -1; descendants are not searched. The caller saves nested scroll, input drafts, focus, and popup state separately. Different page counts, scroll-owner status at each index, or schema values throw `IllegalArgumentException`. restore validates before mutation and skips gates and callbacks. Restoring during notification throws `IllegalStateException`.

## setLocale · onWmRestyle · onMeasure

```text
public void setLocale(Locale locale);
protected void onMeasure(int widthSpec,int heightSpec);
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

setLocale updates command text and layout; null throws `IllegalArgumentException`. The current onWmRestyle hook does nothing. Use subtree restyling for child fonts. Construction, changes, and callbacks use the main thread.

Call setLocale(locale) for language changes. It updates command wording and language-dependent button layout; no onWmLanguageChanged hook is provided.
