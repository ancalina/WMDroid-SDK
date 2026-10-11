---
title: "TodayContract"
slug: en/api/todaycontract
---
[API](/en/api/)

`sdk/src/main/java/me/ancal/wmdroid/sdk/TodayContract.java`

```java
public final class TodayContract
```

## Discovery

```java
public static final int VERSION = 1;
public static final String DISCOVER = "me.ancal.wmdroid.sdk.TODAY_MODULE";
public static final String VERSION_META = "me.ancal.wmdroid.sdk.VERSION";
public static final String VIEW_META = "me.ancal.wmdroid.sdk.CUSTOM_VIEWS";
```

Declare the `DISCOVER` action in the provider intent filter and `VERSION_META=1` in metadata. Add `VIEW_META=true` for custom content. Follow the [manifest example](/en/widgets/).

The host discovers enabled, exported providers with one authority, version 1 metadata, and `readPermission=null`. API 19 and later use the discovery action; API 14–18 scan provider metadata. Enable the installed module in Today item settings before the host queries it.

## rows

```java
public static Uri rows(String authority);
```

Return `content://<authority>/today/v1/rows`. Pass the manifest authority. This helper does not validate the authority or add a language parameter. Add `language=en` to the returned URI for English queries. Follow the [query contract](/en/api/todayprovider/#query).

## COLUMNS

```java
public static final String[] COLUMNS = {"id", "title", "detail", "icon", "activity"};
```

The column order for `MatrixCursor` rows. Do not change the public array contents; provider queries and host column lookup use this array.

| Column | Value and behavior |
| --- | --- |
| `id` | A unique, stable string within the provider. Empty IDs are skipped. |
| `title` | Public title. Empty titles are skipped. |
| `detail` | Public detail string. An empty string is allowed. |
| `icon` | Drawable resource ID in the provider package. `0` selects the default icon. |
| `activity` | Activity class name in the same package. A leading `.` adds the package prefix. Empty means no launch. |

The target Activity must be enabled and exported, with `permission=null`. Otherwise, the host does not launch it.

The host launches the target Activity with an explicit `Intent.ACTION_VIEW` and passes the row's `id` in the string extra `me.ancal.wmdroid.sdk.ROW`. Read `getIntent().getStringExtra("me.ancal.wmdroid.sdk.ROW")` in the Activity to identify the selected row.

Do not expose secret keys, account tokens, or personal notifications through a public provider.

## Row limits

```java
public static final int MAX_ROWS = 8;
public static final int MAX_TEXT = 512;
```

The host inspects only the first 8 cursor rows per provider, including skipped rows. It converts null strings to empty strings and truncates them to 512 UTF-16 code units. It then skips rows with empty IDs, empty titles, or duplicate IDs. Duplicate detection uses the truncated ID within each provider. Keep original strings within the limit; truncation can merge long IDs or split surrogate pairs.

[`TodayProvider.fillRows`](/en/api/todayprovider/#fillrows) does not enforce these limits.

## Custom content

```java
public static final String VIEW_METHOD = "today_widget";
public static final String VIEW_KEY = "view";
public static final String HEIGHT_KEY = "height";
public static final int WIDTH = 480;
public static final int MIN_HEIGHT = 32;
public static final int MAX_HEIGHT = 480;
```

Call [`TodayProvider.call`](/en/api/todayprovider/#call) with `VIEW_METHOD`. Custom content returns a `RemoteViews` under `VIEW_KEY` and its height under `HEIGHT_KEY`; no content returns an empty Bundle. `WIDTH` and height use SDK logical VGA pixels, not dp. The host passes physical width through the `pixel_width` option.

The host accepts only a `RemoteViews` from the provider package with height 32–480. A failed custom call or invalid result retains the same text row.

## Widget

```java
public static final class Widget
public final RemoteViews view;
public final int height;
public Widget(RemoteViews view,int height);
```

The result retains the `view` reference and `height`. It does not clone the `RemoteViews`; finish configuring it before returning. A null view throws `IllegalArgumentException("Widget view required")`. Height outside 32–480 throws `IllegalArgumentException("Widget height outside 32..480")`.

The constructor does not check the view package. `TodayProvider.call` validates the provider package.
