---
title: "WmDocumentProperties"
slug: ko/api/wmdocumentproperties
---
[API 색인](/ko/api/)

```java
public final class WmDocumentProperties extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener,WmControls.BackHandler
```

문서 이름 바꾸기, 폴더/저장 위치 이동 폼. 파일 정보와 실제 목적지는 소유 앱이 제공하고 명시적 OK에서 검증, 저장한다.

## 생성자

```java
public WmDocumentProperties(Context context,Typeface font,Locale locale,String name,String[] folders,int folder,String[] locations,int location,String size,String modified,String format,int accent)
```

font, locale, 이름, 메타데이터 문자열은 non-null. 목적지 배열은 non-null, 비어 있지 않음, null 항목 없음, 선택 인덱스는 배열 범위 안. 위반 시 `IllegalArgumentException`. 이름 초기값은 입력 제한 설정 전에 넣어 보존한다. 이후 입력은 128 UTF-16 코드 단위. 폴더 원본 인덱스 0은 root/None 목적지로 표시 순서 맨 앞에 고정; 나머지 폴더와 위치는 시스템 locale로 표시 정렬한다. 단일 목적지 선택기는 disabled.

## IDs

```java
public static final int NAME=1015,FOLDER=1016,LOCATION=1018,SIZE=2065,MODIFIED=1024,FORMAT=1019;
public static final int NAME_LABEL=2060,FOLDER_LABEL=2062,LOCATION_LABEL=2063,SIZE_LABEL=2064,MODIFIED_LABEL=2059,FORMAT_LABEL=2058;
public static final int NAME_LIMIT=128;
```

입력/메타데이터 컨트롤과 대응 레이블의 View ID. `NAME_LIMIT`은 이름 검증과 후속 입력의 상한.

## Draft

```java
public EditText nameField()
public WmChoice folderField()
public WmChoice locationField()
public String name()
public int folderIndex()
public int locationIndex()
```

Field 접근자는 실제 소유 컨트롤. `name()`은 trim하지 않은 현재 입력. 두 index는 화면 정렬 위치가 아닌 소유 앱의 원본 배열 인덱스. 메타데이터는 표시 전용이며 이 폼에서 파일 I/O를 수행하지 않는다.

## validName

```java
public static boolean validName(String value)
```

null, 빈 값, 128 UTF-16 코드 단위 초과, 점으로 시작, 앞뒤 ASCII 공백, ASCII 공백 이외의 U+0020 이하 문자, `"*/:<>?\|` 중 하나를 포함하면 false. 값을 trim하거나 바꾸지 않는다. 소유 앱이 앞뒤 ASCII 공백 제거 후 호출한다. 파일 충돌이나 목적지 쓰기 가능 여부는 검사하지 않는다.

## Popup, locale, and style

```java
public void setLocale(Locale locale)
public void onWmLanguageChanged(String language)
public boolean closePopup()
public boolean back()
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
protected void onDetachedFromWindow()
```

locale/language가 null이면 `IllegalArgumentException`. locale, 언어 변경은 캡션을 갱신하고 선택 팝업 닫기; 입력값은 유지. `closePopup`/`back`은 열린 선택 팝업을 제거했으면 true. 재스타일과 detach도 팝업을 닫는다. 재스타일은 다음 팝업 글꼴/선택 색과 제목 색을 갱신한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmDocumentProperties.java`
