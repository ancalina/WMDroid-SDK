---
title: "WmResources"
slug: en/api/wmresources
---
[API](/en/api/)

Resolves the app resource pack, launcher snapshot, and asset fallbacks.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmResources.java`

```text
public final class WmResources
```

## prepare(Context) · revision(Context)

```text
public static String revision(Context context);
public static WmResourcePack prepare(Context context)throws IOException;
```

prepare returns the selected snapshot or null and caches per app. The first call reads disk; call on a worker. revision reads no disk and returns the currently prepared app/generation identity. Direct store changes do not update caches. Use install and reset.

## syncFromShell(Context)

```text
public static synchronized boolean syncFromShell(Context context)throws IOException;
```

Mirrors the launcher pack into an immutable local snapshot on a worker. Returns true when selection changes. An unavailable host leaves the current selection unchanged and returns false. An explicit empty host generation sets mirror selection to null and uses bundled assets; it does not automatically select a remaining local pack. Invalid generations, transfers, or installation throw `IOException`.

## install(Context, InputStream) · reset(Context)

```text
public static synchronized WmResourcePack install(Context context,InputStream input)throws IOException;
public static synchronized void reset(Context context)throws IOException;
```

install returns a validated local snapshot and updates reader caches and prepared icons. The caller closes input. reset deactivates the local pack and recalculates selection; a mirror can remain selected. Call both on a worker. Errors throw `IOException`.

For external apps, the first prepare and selection after reset prioritize the saved mirror source record. install immediately selects the verified local pack in the current process. A later successful syncFromShell can select the host mirror again; do not assume the local install survives the next synchronization. reset deactivates the local pack and recalculates this selection priority.

## open(Context, String) · openAsset(Context, String)

```text
public static InputStream open(Context context,String id)throws IOException;
public static InputStream openAsset(Context context,String path)throws IOException;
```

open resolves only pack IDs. Missing packs or IDs throw `FileNotFoundException`. openAsset tries the pack alias `assets/<path>`, then the Android asset. The caller closes streams.

## vector(Context, String)

```text
public static android.graphics.drawable.Drawable vector(Context context,String id)throws IOException;
```

Parses v1 JSON once per generation and returns a new Drawable each time. Missing values are cached too. Invalid IDs throw `IllegalArgumentException`; missing values throw `FileNotFoundException`; parse failures throw `IOException`. Prepare cold parsing on a worker.

## assetFile(Context, String, int)

```text
public static File assetFile(Context context,String path,int limit)throws IOException;
```

`WmResources.assetFile` returns an immutable pack file or cached asset copy. limit is the maximum file size in bytes, from 1 through 67,108,864 (64 MiB); out-of-range limits throw `IllegalArgumentException`. Size overflow, content collision, and I/O errors throw `IOException`. Call on a worker. Do not modify returned files used by fonts or sound players.

## bitmap(Context, String, int, int) · iconBitmap(...)

```text
public static android.graphics.Bitmap bitmap(Context context,String path,int width,int height)throws IOException;
public static android.graphics.Bitmap iconBitmap(Context context,String path,int width,int height,String symbol);
```

width and height are 1–4096, with a product≤1048576. Invalid geometry throws `IllegalArgumentException`. Rasters are limited to 256 KiB and must match the requested dimensions; failure throws `IOException`. Results use DENSITY_NONE. iconBitmap replaces I/O failures with a same-size emoji Bitmap. The caller owns returned Bitmaps.

## emojiBitmap(String, int, int)

```text
public static android.graphics.Bitmap emojiBitmap(String symbol,int width,int height);
```

symbol must be nonempty after trimming and contain at most 32 UTF-16 units. Geometry limits match bitmap. Invalid values throw `IllegalArgumentException`. Uses the system font; unsupported glyphs use an outlined square.
