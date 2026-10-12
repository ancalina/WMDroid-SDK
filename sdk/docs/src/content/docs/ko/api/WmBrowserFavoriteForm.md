---
title: "WmBrowserFavoriteForm"
slug: ko/api/wmbrowserfavoriteform
---
[API 색인](/ko/api/)

```java
public final class WmBrowserFavoriteForm extends LinearLayout implements WmControls.LanguageListener
```

즐겨찾기 이름, 주소, 저장 위치를 입력하는 폼. 소유 앱이 위치 목록, 검증, 저장, 추가/취소 명령을 제공한다.

## 생성자

```java
public WmBrowserFavoriteForm(Context context,Typeface font,String name,String url,String[] locations,int selected,int accent)
```

`name`과 `url`은 초기 초안. `locations`와 `selected`는 `WmChoice`의 원본 목록과 선택 인덱스. `accent`는 선택 색. `font`, `name`, `url`이 null이면 `IllegalArgumentException`. 입력 길이 제한은 이름 256, 주소 2048 UTF-16 코드 단위. locations는 비어 있지 않고 null 항목이 없어야 하며 위반 시 `IllegalArgumentException`. selected 범위 위반은 `IndexOutOfBoundsException`. 위치 선택기는 최대 330 논리 픽셀이며 좁은 폼에서는 가용 폭에 맞춘다.

## Controls

```java
public static final int NAME=12337,URL=12338,LOCATION=12340;
public static final int NAME_LABEL=12356,URL_LABEL=12357,LOCATION_LABEL=12358;
public static final int NAME_LIMIT=256,URL_LIMIT=2048;
public EditText nameField()
public EditText urlField()
public WmChoice locationField()
```

상수는 입력/레이블의 View ID와 입력 길이 제한. 접근자는 폼이 소유한 실제 컨트롤을 반환한다. 저장 시 현재 텍스트와 `locationField().selectedIndex()`를 읽는다. URL 정규화나 저장은 수행하지 않는다.

## Language and measurement

```java
public void onWmLanguageChanged(String language)
protected void onMeasure(int widthSpec,int heightSpec)
```

언어 변경은 이름, 주소, 위치 레이블만 갱신한다. `onMeasure`는 위치 선택기의 폭을 제한한다.


위치 팝업은 소유 앱이 제공합니다. locationField().[setOnOpenListener](/ko/api/wmchoice/#onopenlistener)에 열기를 등록하고 [bindDropdown](/ko/api/wmchoice/#binddropdown-isdroppeddown-setextendedui-isextendedui-searchcharacter) 계약에 따라 팝업을 연결, 제거합니다. 확정 시 choose(sourceIndex)에 원본 locations 인덱스를 전달합니다. 폼은 목록, 선택을 보관하며 팝업 생성, 저장은 하지 않습니다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmBrowserFavoriteForm.java`
