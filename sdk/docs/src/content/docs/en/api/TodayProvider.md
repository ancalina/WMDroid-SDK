---
title: "TodayProvider"
slug: en/api/todayprovider
---
[API](/en/api/)

`sdk/src/main/java/me/ancal/wmdroid/sdk/TodayProvider.java`

```java
public abstract class TodayProvider extends ContentProvider
```

A `ContentProvider` base for cached public Today rows and optional `RemoteViews`. [Register the provider](/en/widgets/) and implement the two abstract hooks below.

## getAuthority

```java
protected abstract String getAuthority();
```

Return the manifest `android:authorities` value. Keep it stable for the provider lifetime; query validation and change notifications use this authority.

## fillRows

```java
protected abstract void fillRows(MatrixCursor rows,String language);
```

Append rows to the supplied cursor in [v1 column order](/en/api/todaycontract/#columns). `language` is a normalized catalog tag packaged in the provider app. An unavailable regional tag uses its nearest packaged parent; an invalid or unsupported tag uses `"ko"`. The base class does not validate row count, string length, or duplicate IDs. Follow the [host limits](/en/api/todaycontract/#row-limits).

Do not assume the UI thread in `query`, `call`, or their hooks. Protect shared cached data against concurrent calls and do not wait for network responses. Hook exceptions propagate to the caller. Do not retain or mutate the cursor after returning.

## query

```java
public final Cursor query(Uri uri,String[] projection,String selection,String[] args,String order);
```

`uri` must be the [rows URI](/en/api/todaycontract/#rows). Validation removes only query parameters; fragments are not accepted. `selection`, `args`, and `order` must all be `null`. Violations throw `IllegalArgumentException("Unsupported Today query")`.

The method ignores `projection` and calls `fillRows` with a new `MatrixCursor` containing all `COLUMNS`. The `language` parameter uses the catalog normalization described above. For example, `en-GB` becomes `en` when the app packages `en` but not `en-gb`. Other query parameters do not select rows.

The returned cursor uses the input `uri`, including its parameters, as its notification URI. The caller must close the cursor. Querying requests asynchronous resource synchronization without waiting for completion.

## createWidget

```java
protected TodayContract.Widget createWidget(String id,Bundle options);
```

The default implementation returns `null`. Override it and declare `CUSTOM_VIEWS=true` in provider metadata to supply custom content. Return `null` when no content matches `id`; the host retains the text row. `call` does not check whether the ID exists in queried rows, so handle it in this hook.

`options` is a fresh Bundle for each call. It contains only the normalized values below. A font path does not grant file access. Content that uses [shared resources](/en/api/wmresources/) must handle fallback resources before synchronization completes.

| Option | Normalization and default |
| --- | --- |
| `width` | Always 480 logical pixels, regardless of input. |
| `pixel_width` | Clamped to 480–1440 physical pixels. Default: 480. |
| `language` | Normalized packaged catalog tag, with parent fallback. Default: `"ko"`. |
| `accent` / `text_color` | ARGB int. Defaults: `0xff149ca7` / `0xff000000`. |
| `font` | Null or absent: `"fonts/malgun/regular.ttf"`. An empty string is retained. |
| `font_bold` | Default: an empty string. |
| `hybrid` | Boolean. Default: `true`. |

Other `extras` keys are not passed to the hook.

## call

```java
public final Bundle call(String method,String arg,Bundle extras);
```

`method` must be `VIEW_METHOD` (`"today_widget"`). `arg` must be a nonempty row ID of at most 512 UTF-16 code units. Another method throws `IllegalArgumentException("Unsupported Today call")`; an invalid ID throws `IllegalArgumentException("Invalid Today row ID")`.

`extras=null` uses default options. The method normalizes options, then calls `createWidget(arg, options)`. A null result produces an empty Bundle. Otherwise, the result contains a `RemoteViews` under `VIEW_KEY` and its height under `HEIGHT_KEY`. A view from another package throws `IllegalArgumentException("Widget resources must belong to provider package")`. The call does not wait for resource synchronization.

## changed

```java
protected final void changed();
```

Call after asynchronous data updates. The method calls `notifyChange(uri, null)` on `rows(getAuthority())` without query parameters to request a host reload.

## onCreate

```java
public boolean onCreate();
```

Record the current resource revision, request asynchronous synchronization, and return `true`. Call the superclass when overriding this method.

Each provider instance observes launcher `WmShell.STATE`. `onCreate`, `query`, `call`, and state changes request synchronization on a shared single worker. Repeated requests during a running sync coalesce into one follow-up request. Completion calls `changed()` if the revision changed and the provider has not stopped when completion is checked. Providers in the launcher package skip this synchronization.

Observer registration failures (`RuntimeException`) and synchronization `IOException` are ignored. Use existing local resources or fallback resources. Other unchecked failures are not covered by this error handling.

## shutdown

```java
public void shutdown();
```

Mark the provider stopped, remove its observer and follow-up synchronization request, then call the superclass. This does not cancel an already running synchronization task or shut down the shared worker. Call the superclass when overriding this method.

## getType

```java
public final String getType(Uri uri);
```

Return `"vnd.android.cursor.dir/vnd.me.ancal.wmdroid.module.v1"` without URI validation. `query` validates whether a URI is supported.

## insert, update, delete

```java
public final Uri insert(Uri u,ContentValues v);
public final int update(Uri u,ContentValues v,String s,String[] a);
public final int delete(Uri u,String s,String[] a);
```

Every call throws `UnsupportedOperationException("Read-only")`.
