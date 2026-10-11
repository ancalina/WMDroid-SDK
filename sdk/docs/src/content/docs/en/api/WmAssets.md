---
title: "WmAssets"
slug: en/api/wmassets
---
[API](/en/api/)

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmAssets.java`

```java
public final class WmAssets
```

## catalog

```java
public static List<Entry> catalog(Context context)throws IOException;
```

Read the `assets` array in `metadata/library.json`, then the `icons` array in `metadata/folders.json`, and return a read-only List in that order. A missing catalog contributes no entries. Folder entries use type `"folder"`; library entries use their `kind` value.

The official extractor uses role-based canonical folder IDs such as `yellow-upright-folder-variant-01`. Library IDs still include original-form identifiers; do not assume that all IDs describe roles. Select entries through their actual `Entry.id`.

The cache uses the application Context, or the supplied Context when no application Context exists, and the active pack object. A changed pack object reloads the catalogs. Directly editing files in the same pack does not refresh the cache; reinstall the edited ZIP. Previously returned Lists retain old entries, but `open` and `loadDrawable` resolve IDs in the current pack. Perform initial catalog reads and image decoding on a worker.

Each catalog is limited to 4 MiB; the two catalogs together allow at most 8192 entries. IDs must be nonempty and at most 160 UTF-16 code units; types must be nonempty and at most 80 UTF-16 code units. Catalog width and height must be 0–4096. Duplicate IDs, invalid paths, and format errors throw `IOException`. Failed catalog loads are not cached.

## list

```java
public static List<Entry> list(Context context,String type)throws IOException;
```

Null type returns the full catalog. No match returns an empty read-only List. Matching is case-sensitive. Known types include icon, folder, bitmap, cursor, png, gif, jpeg, bmp, animation_avi, and audio_wav; they are not a fixed enum.

## byId

```java
public static Entry byId(Context context,String id)throws IOException;
```

Return the cached Entry for an exact ID match in the current catalog, or null for an unregistered ID. Lookup can read catalogs; catalog errors propagate as `IOException`.

## Entry

```java
public static final class Entry
public final String id;
public final String type;
public final String label;
public final String labelKo;
public final String provider;
public final String resourceId;
public final String asset;
public final String originalAsset;
public final String sourceHash;
public final int width;
public final int height;
```

Entry fields are catalog snapshot values; there is no public constructor. An empty label falls back to the ID. Missing labelKo, provider, resourceId, originalAsset, or sourceHash values become empty strings. width and height are catalog dimensions; zero is allowed.

provider, resourceId, and sourceHash retain source provenance. Catalog `resource_id` or `group_resource_id` supplies resourceId; `source_hash` or `source_sha256` supplies sourceHash. Reading a user pack does not compare this hash with an original image hash. Edited images, fonts, and sounds still must pass safety checks.

asset is the logical preview or primary-file path; originalAsset is the optional original-file path. Both must start with `library/` or `folders/`. Empty components, `.`, `..`, backslash, colon, and NUL are rejected. The [resource-pack mapping](/en/api/wmresourcepack/) connects `assets/<path>` IDs to physical ZIP paths.

## Entry.label

```java
public String label(Locale locale);
```

Return labelKo when locale language is `"ko"` and labelKo is nonempty; otherwise return label. A null locale also returns label.

## open

```java
public static InputStream open(Context context,String id)throws IOException;
```

Open originalAsset when present, otherwise asset. GIF/JPEG/CUR and AVI/WAVE are returned as byte streams without decoding. An unregistered ID throws `FileNotFoundException`; file and I/O errors propagate. The caller closes the InputStream.

## loadDrawable

```java
public static Drawable loadDrawable(Context context,String id,int requestedPixels)throws IOException;
```

Limit the output Bitmap’s largest side to requestedPixels. A nonpositive value throws `IllegalArgumentException` before lookup. An unregistered ID throws `FileNotFoundException`. A non-PNG asset, invalid image bounds, or decode failure throws `IOException`.

Previews are limited to 8 MiB, 4096 pixels per side, and 4×1024×1024 pixels total. After a bounds pass and power-of-two sampling, the method shrinks the result if needed. It retains aspect ratio, never enlarges artwork, and uses DENSITY_NONE. Validation uses actual decoded bounds rather than metadata width/height. The caller manages the Bitmap lifetime in the returned BitmapDrawable.
