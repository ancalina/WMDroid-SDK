---
title: "WmBrowserView"
slug: en/api/wmbrowserview
---
[API index](/en/api/)

```java
public final class WmBrowserView extends LinearLayout implements WmControls.BackHandler
```

Browser client containing an address editor and Android `WebView`. The owner supplies the title, menus, command bar, and Activity lifecycle integration. Rendering uses Android WebView.

## Constructor

```java
public WmBrowserView(Context context,Typeface font)
```

The address editor accepts at most 2048 UTF-16 code units. JavaScript and DOM storage are enabled; file/content access and geolocation are disabled. API16+ also disables file-URL access; API21+ blocks mixed content. Network pages require the app’s `INTERNET` permission. Call View/WebView operations on the UI thread.

## normalizeAddress

```java
public static String normalizeAddress(String value)
```

Applies `String.trim()` (removes surrounding characters at or below U+0020). Empty input and exact `about:blank` return the internal blank document. Adds `https://` when no scheme exists, then returns an ASCII URI. Null input, more than 2048 characters, control characters, invalid URI syntax, a non-HTTP(S) scheme, missing host, user-info, or a port above 65535 throws `IllegalArgumentException`. The length limit applies before adding the scheme.

## Navigation

```java
public void navigate(String value)
public boolean canForward()
public boolean canBack()
public void forward()
public void refresh()
public void stop()
public boolean back()
```

`navigate` validates the address, updates the editor, loads the page, focuses the web view, and hides the keyboard. It validates the address even after closure. `canForward`/`canBack` return false after closure. `forward` moves only when possible; `refresh` reloads. `stop` stops loading and reports loading=false. `back` returns true after navigating back, or false when no history exists.

## State and controls

```java
public EditText addressField()
public WebView webView()
public String url()
public String pageTitle()
public boolean isLoading()
public void showAddress(boolean visible)
public boolean isAddressVisible()
```

Control accessors return owned views. `url()` returns the current URL, or an empty string when absent or closed. `pageTitle()` and `isLoading()` expose the last callback state. `showAddress(true)` focuses the editor and selects all text. Changing the returned `webView()` directly can change the security/navigation policy above.

## Listener

```java
public interface Listener {void changed(WmBrowserView browser);void invalidAddress(String address);}
public void setListener(Listener listener)
```

A null listener disables callbacks. `changed` reports page start/finish, title updates, and explicit stop. `invalidAddress` receives the original editor input when Go fails validation. Errors from a direct `navigate` call propagate to its caller.

## Lifecycle

```java
public void pause()
public void resume()
public void close()
protected void onDetachedFromWindow()
protected void onAttachedToWindow()
```

`pause`/`resume` call the WebView lifecycle methods; detach/attach also call them automatically. `close` is idempotent: it stops loading, clears the listener, removes the WebView, and destroys it. The owner must call `close` when permanently discarding the client. Navigation and pause/resume do not run afterward.


Schemes match initial `[A-Za-z][A-Za-z0-9+.-]*:`. Include HTTP(S) explicitly with ports. Length is2048 UTF-16 code units after trim and before adding a scheme.

| Input | Result |
| --- | --- |
|example.com/path|https://example.com/path|
|https://example.com:8080/path|Same address|
|example.com:8080/path|IllegalArgumentException: example.com is parsed as a scheme|
|Empty or about:blank|about:blank|
|https://user@example.com/|IllegalArgumentException: user-info rejected|

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmBrowserView.java`
