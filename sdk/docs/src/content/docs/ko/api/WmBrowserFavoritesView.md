---
title: "WmBrowserFavoritesView"
slug: ko/api/wmbrowserfavoritesview
---
[API 색인](/ko/api/)

```java
public final class WmBrowserFavoritesView extends FrameLayout implements WmControls.RestyleListener
```

폴더와 링크를 같은 목록에서 표시한다. 한 번 탭은 선택, 같은 키를 Android double-tap 시간 안에 다시 탭하면 열기 콜백. 폴더 이동과 URL 열기는 소유 앱이 처리한다.

## Entry and Listener

```java
public static final class Entry
public final String key,name;
public final boolean folder;
public Entry(String key,String name,boolean folder)
public interface Listener {void open(Entry entry);}
```

`key`는 선택 유지용 식별자, `name`은 표시 문자열, `folder`는 폴더 아이콘 선택. null 키/이름은 `IllegalArgumentException`. `Listener.open`은 선택된 항목을 받으며 컨트롤 내부에서 실제 탐색을 수행하지 않는다.

## 생성자

```java
public WmBrowserFavoritesView(Context context,Typeface font,int accent,Listener listener)
```

`font` 또는 `listener`가 null이면 `IllegalArgumentException`. 행 높이 34, 스크롤바 폭 26 논리 픽셀.

## Selection

```java
public void setEntries(Entry[] values)
public Entry selected()
public void openSelection()
public void setEnabled(boolean enabled)
```

`setEntries`는 배열을 복사하고 null 배열/항목, 중복 키를 변경 전에 거부한다(`IllegalArgumentException`). 기존 키가 남으면 새 항목으로 선택을 유지하고, 아니면 첫 항목 선택; 빈 배열에서는 null. 교체 시 double-tap 기록 초기화. `selected()`는 현재 항목 또는 null. `openSelection()`은 활성 상태이며 선택이 있을 때만 콜백 호출. `setEnabled`는 목록, 스크롤바에도 적용하며 double-tap 기록을 초기화한다.

## Layout and style

```java
protected void onMeasure(int width,int height)
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
```

측정 시 넘치는 목록에 스크롤바 공간을 예약한다. 재스타일은 `regular`와 `selection`을 적용하고 리소스팩 revision이 바뀌면 아이콘을 갱신한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmBrowserFavoritesView.java`
