---
title: "WmStrings"
slug: ko/api/wmstrings
---
[API 색인](/ko/api/)

```java
public final class WmStrings
```

작성 UI 문구를 Context의 `assets/i18n/<tag>.json`에서 조회한다. JSON은 객체이며 모든 값은 문자열이어야 한다. key는 정확히 비교하고 공백, 대소문자를 바꾸지 않는다. 빈 번역 문자열도 유효한 결과이다. 사용자 입력, provider 데이터는 번역 key로 사용하지 않는다.

파일 목록, JSON, 없는 선택적 파일을 AssetManager별로 캐시한다. 처음 접근하는 파일은 호출 스레드에서 읽으며 파일 로딩은 동기화한다. 공개 캐시 무효화 API는 없다. 서로 다른 AssetManager의 catalog는 공유하지 않는다.

## languages

```java
public static synchronized String[] languages(Context context);
```

packaged 언어 태그의 새 배열을 반환한다. 처음 두 값은 ko/en이고 나머지는 사전순이다. `i18n`의 `.json` 파일 중 정규화된 태그와 파일명이 일치하는 항목만 포함한다. 대문자나 `_`를 가진 파일명은 제외한다. 배열을 수정해도 캐시는 바뀌지 않는다.

파일 목록 오류는 `IllegalStateException("UI catalogs could not be listed")`, ko/en 파일명이 없으면 `IllegalStateException("Korean and English UI catalogs required")`. 이 호출은 JSON을 읽거나 값의 타입을 검증하지 않는다. context null은 `NullPointerException`.

## language

```java
public static String language(Context context,String value);
```

value의 `_`를 `-`로 바꾸고 Locale.US 소문자로 정규화한다. 유효 형식은 `[a-z]{2,3}(-[a-z0-9]{2,8})*`; null/잘못된 형식은 ko이다. 정규화한 태그가 packaged 목록에 없으면 마지막 subtag부터 제거하며 부모를 찾고 최종적으로 ko를 반환한다.

예: zh-hant-tw→zh-hant→zh→ko. [languages](#languages)의 목록 오류를 그대로 전파한다. 선택한 JSON 내용은 검증하지 않는다.

## locale

```java
public static Locale locale(String language);
```

같은 태그 정규화를 사용하되 packaged 목록은 조회하지 않는다. API ≥21은 `Locale.forLanguageTag`로 변환한다. 이전 버전은 primary language와 첫 2글자/3숫자 국가 subtag만 사용하며 국가를 대문자로 바꾼다. script/variant는 이전 버전에서 유지하지 않는다. null/잘못된 태그는 한국어 Locale을 만든다.

## text

```java
public static String text(Context context,String language,String authored);
```

authored null은 asset 접근 전 null을 반환한다. 그 외에는 정규화한 요청 태그→부모 태그→ko 순으로 authored key를 찾는다. [language](#language)의 packaged 언어 선택을 거치지 않는다. key가 없으면 원문을 그대로 반환한다.

처음 읽는 파일의 JSON/문자열 값/읽기 오류는 `IllegalStateException("UI catalog could not be loaded: <tag>")`. ko/en 필수 파일의 부재는 `IllegalStateException("UI catalog missing: <tag>")`이며 fallback으로 숨기지 않는다. FileNotFoundException인 다른 태그의 부재는 캐시하고 다음 fallback을 찾는다. 실패한 파일 로딩은 결과로 캐시하지 않는다. authored가 null이 아닌데 context가 null이면 `NullPointerException`.

반환 문자열의 format 지시자는 그대로 유지한다. 인수 삽입, 언어 전환 binding은 [WmControls.bindFormat](/ko/api/wmcontrols/#bindformat)을 사용한다.

## text(Context, String, String, String)

```java
public static String text(Context context,String language,String authored,String key);
```

authored null은 key가 있어도 null이다. key가 null이면 일반 authored 조회를 사용한다. 그 외에는 contextual key의 전체 요청 태그→부모→ko 경로를 먼저 검색하고 결과가 없을 때만 authored의 전체 경로를 검색한다. 따라서 ko의 contextual 번역이 요청 언어의 authored 번역보다 우선할 수 있다.

ko catalog에서 contextual key와 값이 동일하면 해당 항목을 건너뛴다. ko-*나 다른 언어의 동일 key/value는 건너뛰지 않는다. 빈 문자열은 fallback을 일으키지 않는다. 실패, 캐시 계약은 [일반 text](#text)와 같다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmStrings.java`
