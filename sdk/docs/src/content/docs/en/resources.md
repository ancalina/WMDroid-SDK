---
title: "Fonts and resources"
slug: en/resources
---

For class contracts, use [WmResources](/en/api/wmresources/), [WmResourcePack](/en/api/wmresourcepack/), [WmAssets](/en/api/wmassets/), and [WmFonts](/en/api/wmfonts/).

## Install a pack

1. On a worker, pass the app-opened InputStream to [WmResources.install(context, input)](/en/api/wmresources/#installcontext-inputstream-resetcontext), handle IOException, and close the stream in the app.
2. Read revision after installation completes. The local pack immediately becomes this process’s selection. Lower-level [WmResourcePack.install](/en/api/wmresourcepack/) operates on a store and is distinct from this selection entry point.
3. On the UI thread, reapply screen resources and invalidate app-owned artwork caches. Completing install alone does not replace existing View artwork.
4. Later successful host synchronization can select the mirror again. Follow [WmResources](/en/api/wmresources/) for unavailable-host, empty-generation, and reset selection rules.

## Apply an imported font

On a worker, use [WmFontFile.importFont](/en/api/wmfontfile/) and obtain the file’s `source()` URI: `content://me.ancal.wmdroid.shell/fonts/<64 lowercase hex digits>`. `resolve()` validates and stores a provider file locally. The caller closes its InputStream. Prepare [WmFonts.load](/en/api/wmfonts/#loadcontext-string-boolean-boolean-family) on a worker, then apply setTypeface/attach or restyleTree on the UI thread. Do not change or delete a file while a Typeface uses it.

## Resource API

```text
public static WmResourcePack prepare(Context context) throws IOException;
public static synchronized boolean syncFromShell(Context context) throws IOException;
public static String revision(Context context);
public static synchronized WmResourcePack install(Context context, InputStream input)
    throws IOException;
public static synchronized void reset(Context context) throws IOException;
public static InputStream open(Context context, String id) throws IOException;
public static InputStream openAsset(Context context, String path) throws IOException;
```

Run disk reads, installation, and synchronization on a worker. `revision()` does not read disk. Run UI changes on the main thread.

- `prepare()`: returns and caches the selected pack, or null if absent.
- `syncFromShell()`: returns true when the selected snapshot changes; false for the launcher itself.
- `revision()`: identifies the prepared cache. Use it to invalidate artwork caches.
- `install()`: returns the validated, activated pack. The caller must close input.
- `reset()`: deactivates the local pack. A host mirror can become selected. Existing snapshots remain usable.
- `open()`: returns a stream for the pack ID. A missing pack or ID throws `FileNotFoundException`.
- `openAsset()`: uses pack ID `assets/<path>` first, then app assets.

Handle `IOException`. The caller must close returned streams. Never modify managed store files directly. Existing readers retain their previous snapshot during installation.

## Launcher pack synchronization

`WmApplet` and `TodayProvider` synchronize on a worker. A separate renderer can call `syncFromShell()`. Bundling the original pack again is not required.

- Host unavailable: retain the last verified snapshot.
- Host explicitly empty: clear the mirror.
- Revision changed: refresh app-specific artwork caches.

## Pack ZIP

Put `pack.json` at the ZIP root. version is numeric `2`. assets maps IDs to relative paths inside the ZIP.

```json
{
  "version": 2,
  "assets": {
    "assets/example.png": "images/example.png"
  }
}
```

This mapping requires an actual `images/example.png` file.

| Condition | Limit |
| --- | --- |
| ID | `[A-Za-z0-9][A-Za-z0-9._/-]{0,159}` |
| Files | At most 4096 files including `pack.json`; at most 8192 ZIP entries including directories. 64 MiB per resource file, 256 MiB compressed input and total extracted bytes. |
| Manifest | `pack.json` at most 256 KiB; 1–4095 asset mappings. |
| Validation | Safe ZIP paths, no duplicate entries; files must be exactly `pack.json` plus the mapped paths. Unmapped files are rejected. |
| Failure | Validation, file, or activation failures throw `IOException`. |

Use public fallbacks without a pack. The default font is OFL Gulim. Check redistribution terms for additional assets.

## Font API

```text
public static Typeface load(Context c, String source, boolean hybrid, boolean heavy);
public static void attach(TextView view, Typeface face);
public static float measure(Paint paint, String text);
public static void draw(Canvas canvas, String text, float x, float baseline, Paint paint);
public static float centerBaseline(Paint paint, float top, float bottom, String text);
public static void setSmoothing(boolean enabled);
```

| Parameter | Contract |
| --- | --- |
| `source` | `SYSTEM_SOURCE="system:default"`, `DEFAULT_SOURCE="fonts/gulim/Gulim-Regular.ttf"`, an asset path, or an imported source. |
| `hybrid` | Tahoma takes priority. The selected face supplies missing glyphs. Without a Tahoma file, the selected face is used. |
| `heavy` | Requests the bold role. It does not convert the weight of a non-hybrid file face. Select a matching source when bold is required. |

`load()` returns and caches a face. Cold loads read disk: use a worker. An `IOException` during pack preparation selects a system fallback. After preparation, an `IOException` during imported-file resolution or face creation becomes `IllegalArgumentException`. Asset-load failures can select a system fallback.

Call `attach()` on the main thread. Use `measure()` and `draw()` for the same hybrid measurement and drawing contract. `centerBaseline()` returns a baseline for top, bottom, and text.

`setSmoothing(false)` disables the process antialiasing policy. Its output can differ from original Gulim bitmap strikes.

Using only `setTypeface()` or `Paint.measureText()` can change hybrid width and baselines. Android version, hinting, and scale can change glyphs. Run font and layout regression checks after font changes.

## Theme and language

Use `WmPalette`, `WmColor`, `WmPaint`, and shared controls. Never replace every color with the accent. Retain surface, border, and selection roles. Use `WmStrings` and app locale resources. Update dynamic text through the language callback.
