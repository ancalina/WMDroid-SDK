---
title: "WmFontFile"
slug: en/api/wmfontfile
---
[API](/en/api/)

Stores imported fonts under SHA-256 names and resolves launcher font URIs.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmFontFile.java`

## Declaration

```text
public final class WmFontFile
public static final int MAX_BYTES = 16 * 1024 * 1024;
public static boolean isSource(String source);
public static File file(Context context,String hash);
public static String source(File file);
public static boolean available(Context context,String source);
public static synchronized File importFont(Context context,InputStream input)throws IOException;
public static synchronized File resolve(Context context,String source)throws IOException;
```

`importFont()` leaves the caller's InputStream open. It validates TTF or OTF data up to 16 MiB and stores it in private `files/wm-fonts/<sha256>.font`. Identical content reuses the existing file. A null stream, invalid font, size overflow, hash collision, or storage failure throws `IOException`. Perform file I/O on a worker.

sha256 is the imported content’s SHA-256 as 64 lowercase hex digits.

`isSource()` accepts only `content://me.ancal.wmdroid.shell/fonts/<64 lowercase hex digits>`. `file()` returns the hash's storage path without creating a file. `source()` converts a .font filename of that form to a URI. Invalid hashes or filenames throw `IllegalArgumentException`.

`available()` checks the URI and a file length of 12 bytes–16 MiB. It does not validate the font again.

`resolve()` returns an existing local File. Otherwise it downloads from the launcher provider, verifies the hash, and stores the file. It closes the provider stream it opens. Invalid URIs, invalid content, and access failures throw `IOException`. Do not change or delete a file while a Typeface uses it.
