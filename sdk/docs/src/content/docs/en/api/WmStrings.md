---
title: "WmStrings"
slug: en/api/wmstrings
---
[API index](/en/api/)

```java
public final class WmStrings
```

Looks up authored UI text in the Context's `assets/i18n/<tag>.json`. Each catalog must be a JSON object with string values. Keys match exactly, including spaces/case; an empty translation is a valid result. Keep user input/provider data out of translation keys.

Caches file lists, JSON, and absent optional files per AssetManager. Reads a file on its first access, on the calling thread; file loading is synchronized. There is no public cache invalidation API. Separate AssetManagers do not share catalogs.

## languages

```java
public static synchronized String[] languages(Context context);
```

Returns a new array of packaged language tags: ko/en first, then remaining tags in lexical order. Includes only `.json` files under `i18n` whose basename equals its normalized tag. Excludes names containing uppercase letters or `_`. Editing the returned array does not change the cache.

Listing failures throw `IllegalStateException("UI catalogs could not be listed")`; missing ko/en filenames throw `IllegalStateException("Korean and English UI catalogs required")`. This call does not read JSON or validate value types. Null context throws `NullPointerException`.

## language

```java
public static String language(Context context,String value);
```

Replaces `_` with `-` and lowercases with Locale.US. Valid tags match `[a-z]{2,3}(-[a-z0-9]{2,8})*`; null/invalid values become ko. If the normalized tag is not packaged, removes trailing subtags to search its parents, then returns ko.

Example: zh-hant-tw→zh-hant→zh→ko. Propagates [languages](#languages) listing failures without validating the selected JSON contents.

## locale

```java
public static Locale locale(String language);
```

Uses the same tag normalization without checking packaged catalogs. API ≥21 converts with `Locale.forLanguageTag`. Earlier versions use the primary language and first two-letter/three-digit country subtag, uppercasing the country; they discard script/variant subtags. Null/invalid tags produce a Korean Locale.

## text

```java
public static String text(Context context,String language,String authored);
```

Null authored returns null before asset access. Otherwise searches the authored key in normalized requested tag→parent tags→ko order. Does not use [language](#language) to select a packaged tag first. A missing key returns authored unchanged.

JSON/value/read failures during first loading throw `IllegalStateException("UI catalog could not be loaded: <tag>")`. Missing required ko/en files throw `IllegalStateException("UI catalog missing: <tag>")` instead of falling back. A FileNotFoundException for another tag is cached as absence and lookup continues. Failed file loads are not cached as results. Null context throws `NullPointerException` when authored is non-null.

Returns format directives literally. Use [WmControls.bindFormat](/en/api/wmcontrols/#bindformat) for argument insertion and language-change bindings.

## text(Context, String, String, String)

```java
public static String text(Context context,String language,String authored,String key);
```

Null authored returns null even when key is supplied. A null key uses ordinary authored lookup. Otherwise searches the entire requested tag→parents→ko hierarchy for the contextual key before searching the entire authored-key hierarchy. A contextual translation in ko can therefore precede an authored translation in the requested language.

Skips a contextual entry whose key equals its value only in the ko catalog. Identity entries in ko-* or other languages are not skipped. An empty string does not trigger fallback. Failure/cache contracts match [ordinary text](#text).

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmStrings.java`
