---
title: "WmContactSummaryView"
slug: ko/api/wmcontactsummaryview
---
[API 색인](/ko/api/)

```java
public final class WmContactSummaryView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

연락처 이름·회사·직함과 전화/링크/주소 작업, 읽기 전용 메모 탭을 표시한다. 실제 전화·문자·메일·주소 편집은 콜백을 받은 소유 앱이 수행한다.

## Phone

```java
public static final int CALL=0,SMS=1,ICON_NONE=-1,ICON_MOBILE=0,ICON_WORK=1,ICON_HOME=2,ICON_SMS=3;
public static final class Phone
public final String key,label,number;
public final int action,icon;
public Phone(String key,String label,String number)
public Phone(String key,String label,String number,int action,int icon)
```

세 인수 생성자는 CALL·ICON_WORK. `key`는 비어 있지 않은 작업 ID, `label`과 `number`는 표시 문자열. null/빈 키·null 문자열, CALL/SMS 외 action, -1..3 밖 icon은 `IllegalArgumentException`. 번호 형식은 여기서 검증하지 않는다.

## Link and Address

```java
public static final int EMAIL=0,WEB=1;
public static final class Link
public final String key,label,value;public final int kind;
public Link(String key,String label,String value,int kind)
public static final class Address
public final String key,label,value;
public Address(String key,String label,String value)
```

두 데이터 타입은 non-null·비어 있지 않은 키, non-null 레이블/값 필수. Link kind는 EMAIL/WEB만 허용. 위반 시 `IllegalArgumentException`. 값은 문자열로 유지되므로 URI/주소 파싱과 작업 허용 여부는 소유 앱이 결정한다.

## Callbacks

```java
public interface Listener {void selected(Phone phone);void dial(Phone phone);default void message(Phone phone){}default void selectedLink(Link link){}default void openLink(Link link){}default void selectedAddress(Address address){}default void editAddress(Address address){}}
public interface OnTabChangedListener {void changed(int tab);}
```

전화 행을 누르면 `selected` 다음에 CALL은 `dial`, SMS는 `message`를 호출합니다. 링크 행은 `selectedLink` 다음 `openLink`, 주소 행은 `selectedAddress` 다음 `editAddress`를 호출합니다. 기본 메서드는 아무 작업도 하지 않습니다. 요약·메모 탭이 실제로 바뀌었을 때만 탭 변경 callback을 호출합니다.

## 생성자

```java
public WmContactSummaryView(Context context,Typeface regular,Typeface bold,int selection,Listener listener)
```

두 글꼴·리스너 필수; null은 `IllegalArgumentException`. 선택 색은 행 하이라이트에 사용한다.

## setContact

```java
public void setContact(String name,Phone[] phones,String notes)
public void setContact(String name,String company,String jobTitle,Phone[] phones,Link[] links,String notes)
public void setContact(String name,String company,String jobTitle,Phone[] phones,Link[] links,Address[] addresses,String notes)
```

짧은 오버로드는 생략된 문자열/배열을 빈 값으로 사용한다. null 이름·회사·직함·메모는 빈 문자열. 배열은 복사하며 null 배열/항목·모든 작업 배열에 걸친 중복 키를 변경 전에 거부한다(`IllegalArgumentException`). 선택 키가 남으면 유지; 아니면 첫 전화, 첫 링크, 첫 주소 순서로 선택. 내용이 같으면 다시 만들지 않고, 변경 시 두 탭의 스크롤 위치를 유지한다.

## Selection and tabs

```java
public Phone selected()
public Link selectedLink()
public Address selectedAddress()
public int selectedTab()
public void setOnTabChangedListener(OnTabChangedListener listener)
public void setTab(int tab)
public void setMessage(String authored)
```

선택 getter는 해당 타입이 활성 선택일 때 항목, 아니면 null. 탭 0=요약, 1=메모; 다른 값은 `IllegalArgumentException`. null 탭 리스너는 해제. `setMessage(non-null)`은 탭은 유지한 채 내용 영역을 메시지로 덮는다. null은 선택 탭의 내용을 복원한다.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
```

글꼴과 선택 색을 갱신한다. null 글꼴은 `IllegalArgumentException`. 언어 변경은 SDK가 작성한 캡션에 적용하며 사용자 데이터는 그대로 유지한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmContactSummaryView.java`
