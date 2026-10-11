---
title: "WmMediaLibraryView"
slug: ko/api/wmmedialibraryview
---
[API 색인](/ko/api/)

```java
public final class WmMediaLibraryView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

소유 앱의 미디어 계층을 제목·아이콘 행으로 표시한다. 저장소 탐색과 색인 작성은 소유 앱이 수행한다.

## Row and commands

```java
public static final int OPEN=20001,BACK=-1,SOURCE=-2,CONTEXT=0;
public static final int LIBRARY=0,MUSIC=1,VIDEO=2,TV=3,PLAYLIST=4,NOW_PLAYING=5,ARTIST=6,ALBUM=7,GENRE=8;
public static final class Row
public final String key,title;public final int icon;public final boolean authored;
public Row(String key,String title,int icon,boolean authored)
```

key는 non-null·비어 있지 않은 식별자, title non-null, icon 0..8 필요; 위반 시 `IllegalArgumentException`. authored=true 제목은 SDK 문자열 번역 조회, false는 문자 그대로 표시. 행 탭은 선택 후 OPEN, 길게 누르면 CONTEXT. 제목줄 왼쪽은 SOURCE, 오른쪽 92 논리 픽셀은 BACK 명령. 콜백 타입은 `WmMediaPlaylistView.Listener`.

## 생성자

```java
public WmMediaLibraryView(Context context,Typeface font,WmMediaPlaylistView.Listener listener)
```

null listener는 `IllegalArgumentException`. 제목 높이 46, 행 높이 34 논리 픽셀.

## Page and selection

```java
public void setPage(String title,int icon,Row[] values)
public void setPage(String title,int icon,Row[] values,boolean authored)
public Row selected()
public void select(int index)
```

세 인수는 제목 authored=true. 제목 non-null·icon 0..8·배열/항목 non-null 필요; 위반 시 `IllegalArgumentException`. 배열을 복사하고 첫 항목 선택/맨 위 스크롤, 빈 목록이면 선택 없음. 중복 키는 거부하지 않는다. `selected`는 선택 항목 또는 null. `select` 범위 위반은 `IllegalArgumentException`.

## Icons and lifetime

```java
public void loadIcons(Executor worker)
public void close()
```

제공 Executor에서 아이콘을 읽고 main Looper Handler에서 기존 비트맵을 recycle한 뒤 교체한다. window에 붙기 전에도 결과를 적용한다. 누락 아이콘은 회색 사각형. close는 반복 호출 가능, 현재 비트맵 recycle; 닫힌 뒤 로드 결과도 recycle. Executor 종료는 호출자 책임.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int color,int tab)
public void onWmLanguageChanged(String value)
```

재스타일은 글꼴·선택 색·배경 갱신. 언어 변경은 authored 행/제목과 뒤로 캡션에 적용하며 데이터 제목은 보존한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmMediaLibraryView.java`
