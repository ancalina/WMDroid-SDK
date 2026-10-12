---
title: "WmResourcePack"
slug: en/api/wmresourcepack
---
[API](/en/api/)

Validates and installs v2 ZIP resource packs and exposes immutable snapshots.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmResourcePack.java`

```java
public final class WmResourcePack
public static final int MAX_FILES = 4096;
public static final int MAX_FILE_BYTES = 64 * 1024 * 1024;
public static final long MAX_BYTES = 256L * 1024 * 1024;
```

## install(File, InputStream)

```java
public static synchronized WmResourcePack install(File store,InputStream input)throws IOException;
```

Call on a worker. The caller owns input; it remains open. Null input, format, path, file, and I/O errors throw `IOException`. Replaces the active pointer only after validating every file. Failure preserves the previous active pack. The returned snapshot is independent of prior snapshots.

Limits: 4096 files, 64 MiB per file, and 256 MiB each for compressed and inflated totals. pack.json is limited to 256 KiB with version=2 and an assets mapping of 1–4095 IDs. IDs match `[A-Za-z0-9][A-Za-z0-9._/-]{0,159}`. Paths allow at most 240 characters and reject absolute paths, empty components, `.` and `..`. Missing or unmapped files and duplicate ZIP entries are rejected. Extensions are png, gif, bmp, jpg, jpeg, webp, ico, cur, svg, ttf, otf, wav, ogg, avi, skn, pwi, and json.

Installation and active-pack reads accept only version 2; other versions throw `IOException("Unsupported pack version")`. `assets` maps logical IDs to ZIP paths. IDs need not equal paths, and multiple IDs can share one file. Paths must match `[A-Za-z0-9._/-]+` and remain within the canonical store boundary. At most 8192 ZIP entries, including directories, are accepted. Extensions must use the lowercase values above.

The official extractor writes `assets/<role-path>` IDs and role-based ZIP paths. Catalog paths are `metadata/library.json` and `metadata/folders.json`. User packs can map IDs to other safe paths. You can edit images, fonts, sounds, and mappings before import; matching original pixel hashes is not an installation condition. File size, path, extension, and mapping checks still apply.

## active(File), deactivate(File)

```java
public static synchronized WmResourcePack active(File store)throws IOException;
public static synchronized void deactivate(File store)throws IOException;
```

No active pointer returns null. An unreadable installed pointer or manifest throws `IOException`. deactivate clears only selection. Previous snapshot files remain available to readers. Old generations are not deleted automatically.

## ids(), generation(), open(String)

```java
public Set<String> ids();
public String generation();
public InputStream open(String id)throws IOException;
```

ids is a read-only Set. generation is the snapshot directory name. open returns a mapped ID InputStream; the caller closes it. Missing IDs throw `FileNotFoundException`. Do not change snapshot files directly.

## writeZip(OutputStream)

```java
public void writeZip(OutputStream output)throws IOException;
```

Exports mapped files and manifest as ZIP. Stored IDs and paths remain unchanged; export does not rename them to canonical role paths. The caller owns output; it remains open. Checks size limits and file availability; failure throws `IOException`. Call on a worker. For launcher and app use, install through [WmResources](/en/api/wmresources/) to update caches.
