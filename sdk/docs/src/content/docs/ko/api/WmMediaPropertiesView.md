---
title: "WmMediaPropertiesView"
slug: ko/api/wmmediapropertiesview
---
[API 색인](/ko/api/)

```java
public final class WmMediaPropertiesView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

미디어 메타데이터 스냅샷을 레이블/값 쌍으로 표시한다. 파일 읽기 없이 `setValues`로 받은 데이터만 사용한다.

## 생성자

```java
public WmMediaPropertiesView(Context context,Typeface regular,Typeface bold)
```

regular는 값, bold는 레이블 글꼴. 키보드 탐색을 위해 attach/표시 시 포커스를 요청한다.

## Snapshot

```java
public void setValues(String[] snapshot)
public String[] values()
public void resetScroll()
```

정확히 13개 문자열 슬롯 필요; null 배열/다른 길이는 `IllegalArgumentException`. 순서는 음악가, 앨범, 앨범 음악가, 제목, 장르, 길이, 오디오 코덱, 오디오 형식, 비디오 코덱, 비디오 형식, 위치, 트랙 번호, 비트 전송률. null 슬롯은 레이블/값 쌍 생략, 빈 문자열은 빈 값 표시. 배열 복사, 기존 스크롤 위치 유지. `values`는 스냅샷 복사, `resetScroll`은 맨 위 이동. 단위/서식은 소유 앱이 만든 문자열 그대로 표시한다.

## Menu

```java
public WmMenuItem[] menuItems()
```

ID 20302의 CD 구입 항목을 disabled 상태로 반환. 연결된 앨범 구입 서비스는 없다.

## Keyboard and focus

```java
public boolean onKeyDown(int key,KeyEvent event)
public boolean onKeyUp(int key,KeyEvent event)
protected void onAttachedToWindow()
protected void onVisibilityChanged(View changed,int visibility)
protected void onFocusChanged(boolean focus,int direction,Rect previous)
```

활성 상태, owner 명령 허용, modifier 없음 조건에서 DPAD 위/아래는 레이블/값별로 선택 이동하고 화면 밖 항목을 스크롤한다. 처리한 키는 true, 나머지는 상위 View에 전달. 포커스 변경은 현재 필드 표시를 다시 그린다.

## Style

```java
public void onWmLanguageChanged(String value)
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int face)
```

언어 변경은 메타데이터 레이블만 갱신. 재스타일은 글꼴, 선택 색, 흰색에서 face 색으로 가는 배경에 적용한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmMediaPropertiesView.java`
