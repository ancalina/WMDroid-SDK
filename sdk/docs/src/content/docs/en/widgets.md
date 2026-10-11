---
title: "Today modules"
slug: en/widgets
---


## Integration

Keep an ordinary Android Activity and add only `TodayProvider`. Do not declare `WM_STYLE` for an ordinary Activity. The row's `activity` names an exported Activity in the same package. Use an empty string if no launch is required.

```java
package org.example.demo;

import android.database.MatrixCursor;
import me.ancal.wmdroid.sdk.TodayProvider;

public final class DemoProvider extends TodayProvider {
    @Override protected String getAuthority() {
        return "org.example.demo.today";
    }
    @Override protected void fillRows(MatrixCursor rows, String language) {
        boolean english = "en".equals(language);
        rows.addRow(new Object[] {
            "status", english ? "Status" : "상태",
            english ? "Ready" : "준비됨", 0,
            "org.example.demo.DemoActivity"
        });
    }
}
```

This DemoProvider supplies only `ko`/`en` text. `language` is a catalog tag normalized to lowercase with underscores replaced by hyphens. An unavailable catalog falls back to the nearest packaged parent, then `ko` if none matches. For example, with `en` but no `en-gb` catalog, an `en-GB` request is passed as `en`.

```xml
<provider android:name=".DemoProvider"
    android:authorities="org.example.demo.today"
    android:exported="true"
    android:label="Demo">
    <intent-filter>
        <action android:name="me.ancal.wmdroid.sdk.TODAY_MODULE" />
    </intent-filter>
    <meta-data android:name="me.ancal.wmdroid.sdk.VERSION"
        android:value="1" />
</provider>
```

Put the manifest element inside `<application>`. Declare `DemoActivity` as exported too. Enable the module in the launcher Today item settings. Installation does not enable it.

## Provider API

```text
protected abstract String getAuthority();
protected abstract void fillRows(MatrixCursor rows, String language);
protected TodayContract.Widget createWidget(String id, Bundle options);
protected final void changed();
// TodayContract
public static Uri rows(String authority);
// TodayContract.Widget
public Widget(RemoteViews view, int height);
```

- `getAuthority()`: returns the unique authority declared in the manifest.
- `fillRows()`: appends public rows. `language` is a normalized catalog tag packaged in the provider app. An unavailable regional tag uses its nearest packaged parent; an invalid or unsupported tag uses `ko`. Return cached data promptly.
- `createWidget()`: returns custom content for a stable row ID. null selects the text row.
- `changed()`: notifies the rows URI to request a host reload.
- `TodayContract.rows()`: returns `content://<authority>/today/v1/rows`.
- `Widget()`: retains the view reference and height. A null view or out-of-range height throws `IllegalArgumentException`.

Never wait for network work or large file reads in provider calls. Do not assume the UI thread in provider callbacks. Do not assume that an Activity exists. Call `changed()` after background work completes. Track request order so that old results cannot replace newer state.

`TodayProvider` synchronizes resource changes on a worker. Call the superclass when you override `onCreate()` or `shutdown()`.

During a custom-view refresh, the host keeps the last validated view. Its content can remain stale until the new response arrives. A null or invalid result, or a failed call, selects the text row. Removed rows and views from another app version are not reused.

If inflation fails, the host uses the text row and does not inflate that same `RemoteViews` object again. A new object is attempted normally.

## v1 rows

Column order: `id`, `title`, `detail`, `icon`, `activity`.

| Column | Value |
| --- | --- |
| `id` | A unique, stable string within the provider. It must not be empty. |
| `title` | A non-empty public title. |
| `detail` | Public detail text. It can be empty. |
| `icon` | A drawable resource ID in the provider package. `0`: default. |
| `activity` | An exported Activity in the same package. Empty: no launch. |

| Host condition | Limit |
| --- | --- |
| Provider | Exported, enabled, version 1, without readPermission. |
| Row count | Only the first 8 rows per provider are read. |
| Strings | Clipped to 512 characters. |
| Skipped rows | Empty IDs, empty titles, or duplicate IDs. |
| Language query | Normalized to a provider app catalog tag, with nearest packaged parent fallback. Default: `ko`. |

Other apps can query a public provider. Never put secret keys, personal notifications, or account tokens in rows or options.

```text
public final Cursor query(Uri uri, String[] projection, String selection,
                          String[] args, String order);
public final Bundle call(String method, String arg, Bundle extras);
```

`query()` accepts only the rows URI. Non-null selection, args, or order throws `IllegalArgumentException`. URI comparison ignores query parameters. The result contains all v1 columns regardless of projection. The caller must close the Cursor. insert, update, and delete throw `UnsupportedOperationException("Read-only")`.

## Multiple modules in one APK

- Multiple rows: return a unique ID for each row from one provider.
- Independent enable settings: use separate providers with unique authorities and discovery metadata.
- Current example: `WidgetsProvider.Weather`, `.Transit`, `.Splatnet`, and `.Battery`.


## Custom content

Add `me.ancal.wmdroid.sdk.CUSTOM_VIEWS=true` to the provider metadata.

```java
@Override protected me.ancal.wmdroid.sdk.TodayContract.Widget createWidget(
        String id, android.os.Bundle options) {
    if (!"status".equals(id)) return null;
    android.widget.RemoteViews view = new android.widget.RemoteViews(
        getContext().getPackageName(), R.layout.today_widget);
    view.setTextViewText(R.id.caption, "Ready");
    return new me.ancal.wmdroid.sdk.TodayContract.Widget(view, 40);
}
```

Create the app’s `res/layout/today_widget.xml` as below. Also add `CUSTOM_VIEWS=true` to the Provider metadata above.

```xml
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/caption"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center_vertical" />
```

The app must supply the `today_widget` layout and `caption` ID. Use only supported `RemoteViews` controls. The layout must belong to the provider package. A view from another package throws `IllegalArgumentException`.

`Widget.height`: 32–480 logical pixels, not dp. Finish configuring the view before returning it. Failed host validation or an unsupported layout falls back to the text row. Large Bitmaps affect Binder transfer limits. Keep responses small.

| options | Normalization and defaults |
| --- | --- |
| `width` | 480 logical pixels. |
| `pixel_width` | Clamped to 480–1440. Default: 480. |
| `language` | Normalized catalog tag packaged in the provider app, with parent fallback. Default: `ko`. |
| `accent` | ARGB. Default: `0xff149ca7`. |
| `text_color` | ARGB. Default: `0xff000000`. |
| `font` | Default: `fonts/malgun/regular.ttf`. |
| `font_bold` | Default: an empty string. |
| `hybrid` | Default: true. |

These are `TodayProvider.call()` defaults. Supplied host values replace them. A font path does not grant file access.

`call()` accepts only method `today_widget` and a non-empty ID of up to 512 characters. Other values throw `IllegalArgumentException`. Custom content returns Bundle keys `view` and `height`. No content returns an empty Bundle.

The host owns row selection and outer dividers. Never add a duplicate divider to content. The [shared UI](/en/ui/) `BETWEEN` policy removes the divider below the final item.

## Actual provider examples

The separate `WMDroid-examples-sources.zip` contains `widgets/app/src/main/java/me/ancal/wmdroid/widgets/WidgetsProvider.java` and `widgets/app/src/main/AndroidManifest.xml` for Weather/Transit/Splatnet/Battery integration. The minimal provider is `sample/app/src/main/java/me/ancal/wmdroid/sample/SampleProvider.java`. `today-probe/` demonstrates an ordinary Android Activity publishing two independent Today modules without inheriting `WmApplet`.

The RemoteViews XML above explains IDs and construction; it is a fragment, not the complete WidgetsProvider project. Use the [separate examples build](/en/getting-started/#build-the-separate-examples-project) with the binary AAR.
