---
title: "WmContactsView"
slug: ko/api/wmcontactsview
---
[API 색인](/ko/api/)

```java
public final class WmContactsView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

검색·영문 첫 글자 그룹·연락처 목록을 표시한다. 데이터 공급과 연락처 열기는 소유 앱이 수행한다. 이름에 대한 대소문자 무시 부분 문자열 검색이며 입력 배열 순서를 유지한다.

## Entry and Listener

```java
public static final class Entry
public final String key,name,secondary;
public Entry(String key,String name)
public Entry(String key,String name,String secondary)
public interface Listener {void selected(Entry entry);void open(Entry entry);}
```

`key`는 비어 있지 않은 고유 ID; null/빈 키는 `IllegalArgumentException`. null 이름/보조 문자열은 빈 문자열로 변환한다. 두 인수 생성자는 보조 문자열 없음. 완료된 한 번 탭은 `selected` 다음 `open` 순서로 같은 항목 전달.

## 생성자

```java
public WmContactsView(Context context,Typeface regular,Typeface bold,int selection,Listener listener)
```

두 글꼴과 리스너 필수; null이면 `IllegalArgumentException`. 행 높이 54 논리 픽셀. 첫 글자 버튼은 `#ab`, `cde`, `fgh`, `ijk`, `lmn`, `opq`, `rst`, `uvw`, `xyz`에 속하는 첫 표시 항목으로 스크롤한다. 한글 초성 탐색은 하지 않는다.

## Data and query

```java
public void setEntries(Entry[] entries)
public Entry selected()
public EditText search()
public String query()
public void setQuery(String query)
public void setMessage(String authored)
```

`setEntries`는 배열 복사, null 배열/항목·중복 키는 변경 전 `IllegalArgumentException`. 선택은 키로 유지하며 사라지면 null. 데이터 변경 후 기존 목록 스크롤 위치 복원. `search()`는 실제 입력 컨트롤. `query()`는 현재 텍스트; `setQuery`는 null 거부, 변경 시 필터링하고 맨 위로 스크롤. 선택은 검색 때문에 자동 해제되지 않으므로 `selected()`가 현재 검색에서 숨겨진 항목일 수 있다. `setMessage`에 non-null 문자열을 주면 목록 대신 소유 앱의 상태 메시지 표시; null은 데이터 화면 복원.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
```

글꼴과 선택 색을 갱신한다. 언어 변경은 SDK가 작성한 캡션에 적용하며 사용자 데이터는 그대로 유지한다.

## Detachment

```java
protected void onDetachedFromWindow()
```

분리 시 스크롤바의 반복 스크롤 작업을 중지한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmContactsView.java`
