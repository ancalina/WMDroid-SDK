---
title: "WmMediaPlaylistView"
slug: ko/api/wmmediaplaylistview
---
[API 색인](/ko/api/)

```java
public final class WmMediaPlaylistView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

지금 재생 목록, 선택 행, 위/아래 이동·추가·제거·속성 툴바. 실제 큐 변경과 재생은 소유 앱이 명령 콜백에서 수행한다.

## Commands

```java
public static final int UP=30002,DOWN=30003,REMOVE=30004,ADD=30009,SAVE=30006,PROPERTIES=30010,PLAY=30001;
public interface Listener {void command(int id);}
```

한 번 탭은 행 선택만 변경. 길게 누르면 선택 후 ID 0으로 컨텍스트 메뉴 요청. 툴바는 UP/DOWN/ADD/REMOVE/PROPERTIES를 전달. SAVE와 PLAY는 소유 앱 메뉴에서 사용할 ID이며 이 뷰의 툴바 버튼이 아니다.

## 생성자

```java
public WmMediaPlaylistView(Context context,Typeface font,Listener listener)
```

null listener는 `IllegalArgumentException`. 제목/툴바 높이 46, 행 높이 34 논리 픽셀.

## Entries and selection

```java
public void setEntries(WmMediaQueue.Entry[] values,int current)
public int selectedIndex()
public void select(int index)
@Deprecated public void setSaveEnabled(boolean value)
```

`setEntries`는 null 배열만 명시적으로 거부하고 배열 복사. 호출자는 null 항목 없이 current=-1 또는 배열 범위 내 값을 제공해야 한다. current는 지금 재생 접근성 표시에 사용하며 목록 선택과 별개. 기존 선택이 범위 안이면 유지; 아니면 마지막 항목 또는 current/첫 항목으로 조절. `selectedIndex`는 선택 없음 -1. `select` 범위 위반은 `IllegalArgumentException`. `setSaveEnabled`는 호환성용 무동작; Save 활성 여부는 소유 메뉴에서 제어한다.

## Icons and lifetime

```java
public void loadIcons(Executor worker)
public void close()
```

`loadIcons`는 제공 Executor에서 리소스팩 비트맵을 읽고 main Looper Handler로 적용한다. window에 붙기 전에도 결과를 적용한다. 누락 이미지에는 기호 사용. `close`는 반복 호출 가능하며 적용된 비트맵을 recycle한다. 닫힌 뒤 도착한 로드 결과도 recycle. 버리는 뷰에서 close 호출; 이 메서드는 Executor를 종료하지 않는다. Executor 제출 실패는 호출자에게 전파된다. 새 로드 결과를 적용할 때 이전 비트맵을 recycle한 뒤 교체한다.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int color,int tabColor)
public void onWmLanguageChanged(String value)
```

재스타일은 행 글꼴·선택 색과 제목/툴바 배경을 갱신한다. 언어 변경은 툴바·제목 접근성 캡션을 갱신하며 미디어 제목은 그대로 유지한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmMediaPlaylistView.java`
