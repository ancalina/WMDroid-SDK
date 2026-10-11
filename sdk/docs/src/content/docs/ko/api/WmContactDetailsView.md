---
title: "WmContactDetailsView"
slug: ko/api/wmcontactdetailsview
---
[API 색인](/ko/api/)

```java
public final class WmContactDetailsView extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener,WmControls.BackHandler
```

연락처 초안을 자세히·메모 탭으로 편집한다. 필드 순서·레이블·데이터 저장은 소유 앱이 제공한다. 값 변경은 콜백으로 전달하며 자동 저장하지 않는다.

## Field

```java
public static final int TEXT=0,SUBCARD=1,ACTION=2,DATE=3;
public static final class Field
public final String key,label,value;
public final int kind,inputType;
public Field(String key,String label,String value,int kind)
public Field(String key,String label,String value,int kind,int inputType)
```

`key`는 non-null·비어 있지 않은 ID; 레이블·값 non-null. kind는 0..3이며 위반 시 `IllegalArgumentException`. 네 인수 생성자는 `InputType.TYPE_CLASS_TEXT`. TEXT는 한 줄 입력, SUBCARD는 텍스트 입력과 확장 화살표, ACTION은 `open` 콜백, DATE는 `WmDateField.parseValue`로 검증한 날짜 또는 빈 날짜 문자열. `inputType`은 텍스트 입력기에 적용한다.

## Listener

```java
public interface Listener {
    default void changed(Field field){}
    default void committed(Field field){}
    default void open(Field field){}
    default void notesChanged(String notes){}
    default void tabChanged(int tab){}
}
```

`changed`는 사용자 필드 값 변경, `committed`는 활성 텍스트/날짜 필드 포커스 해제 시 현재 초안, `open`은 ACTION 또는 SUBCARD 화살표 선택. `notesChanged`는 메모 변경. `tabChanged`의 값은 0=자세히, 1=메모. 기본 구현은 아무 작업도 하지 않는다. committed는 영구 저장 성공을 뜻하지 않는다.

## 생성자

```java
public WmContactDetailsView(Context context,Typeface regular,Field[] fields,String notes,int selection,Listener listener)
```

regular·listener 필수, null이면 `IllegalArgumentException`. 초기 fields는 아래 `setFields` 규칙을 따르며 null 메모는 빈 문자열. 행 높이 39 논리 픽셀; 텍스트 입력기는 해당 행을 활성화할 때 생성한다.

## Draft access and updates

```java
public Field[] fields()
public Field field(String key)
public void editField(String key)
public String notes()
public void setNotes(String value)
public void setFieldValue(String key,String value)
public void setFields(Field[] values)
```

`fields`는 배열 복사, `field`는 키의 현재 Field; 알 수 없는 키는 `IllegalArgumentException`. `editField`는 자세히 탭으로 이동한다. TEXT·SUBCARD는 입력기를 활성화하고 전체 텍스트를 선택한다. ACTION은 `open`을 호출한다. DATE는 날짜 입력기를 표시하고 포커스를 주며, 이 호출 자체로 달력 팝업을 열지는 않는다. 빈 DATE는 허용 범위로 제한한 오늘 날짜로 바뀌며 `changed`가 발생할 수 있다. 달력은 날짜 필드의 별도 열기 동작으로 연다. `notes`는 현재 초안, `setNotes(null)`은 빈 문자열. `setFieldValue`는 null 값과 잘못된 DATE 값을 거부한다. `setFields`는 배열 복사·null 배열/항목·중복 키 검증을 변경 전에 완료한다. 키·kind·inputType·순서가 호환되면 기존 컨트롤 갱신, 아니면 다시 만들고 가능한 포커스/스크롤 복원. 소유 앱의 setter는 사용자 changed/notesChanged 콜백을 보내지 않는다. 일반 텍스트 포커스 저장/복원 대상은 TEXT kind이며 SUBCARD의 텍스트 입력 포커스는 interaction Bundle에 포함하지 않는다.

## Tabs

```java
public int selectedTab()
public void setTab(int tab)
```

0=자세히, 1=메모. 범위 밖은 `IllegalArgumentException`. 탭 이동은 날짜 팝업을 닫는다.

## Subcard

```java
public void showSubcard(WmContactSubcardView card,Runnable apply,Runnable cancel)
public WmContactSubcardView subcard()
public boolean closeSubcard(boolean apply)
```

부모에 붙지 않은 card와 non-null apply/cancel 콜백 필요; 위반 시 `IllegalArgumentException`. 기존 날짜 팝업 닫기, 기존 subcard 취소 후 새 카드 표시. 취소 콜백이 새 subcard를 열면 `IllegalStateException`. `subcard()`는 열린 카드 또는 null. 바깥 탭은 Apply, Back은 Cancel; 원본의 해당 dismissal 동작은 미검증. `closeSubcard`는 카드를 먼저 제거한 뒤 선택한 콜백 실행, 닫을 카드가 있었으면 true. detach는 카드 제거만 수행하고 완료 콜백은 실행하지 않는다.

## Date popup and Back

```java
public WmDateField dateField(String key)
public WmCalendarPicker datePopup()
public boolean closeDatePopup()
public boolean back()
```

`dateField`는 알려진 DATE 키의 실제 컨트롤; 다른 kind/없는 키는 `IllegalArgumentException`. `datePopup`은 열린 달력 또는 null. 달력 범위 1752-09-14..9999-12-31. `closeDatePopup`은 중첩 연도 편집을 먼저 종료한 뒤 팝업 제거; 열려 있었으면 true. `back`은 중첩 날짜 편집, 날짜 팝업, subcard 취소 순서로 처리하며 소비했으면 true.

## Interaction state

```java
public Bundle saveInteractionState()
public static void validateInteractionState(Bundle state,Field[] ownerValues)
public void restoreInteractionState(Bundle state)
```

저장 Bundle은 탭·두 스크롤 위치·포커스 필드/메모·커서, DATE 컨트롤 상태와 열린 달력의 중첩 상태를 포함한다. 필드/메모 값은 포함하지 않는다. 먼저 소유 앱의 초안을 복원한 뒤 interaction state 적용. `validateInteractionState`는 변경 없이 schema 1..3·정확한 키 수/타입·비음수 위치·필드 호환성·DATE 상태를 검증하며 실패 시 `IllegalArgumentException`. `restoreInteractionState`도 먼저 검증하며 날짜 팝업을 닫는 중이면 `IllegalStateException`. schema>=2에서 비어 있지 않은 date_popup이 지정한 달력을 다시 열어야 하는데 해당 DATE가 disabled인 경우도 `IllegalStateException`. 단순 DATE 상태 복원 전체에 적용되는 비활성 검사는 아닙니다. 복원 중 사용자 콜백 억제, 커서는 현재 값 길이로 제한. subcard가 열려 있으면 그 카드의 포커스를 유지한다.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
```

재스타일은 입력기·메모·탭·열린 날짜 팝업/subcard에 적용하며 두 스크롤 위치를 유지한다. null regular는 `IllegalArgumentException`. 언어 변경은 탭 캡션과 DATE 표시/달력 locale을 갱신한다. 소유 앱의 필드 레이블은 그대로 유지한다.

## View lifecycle

```java
protected void onAttachedToWindow()
protected void onDetachedFromWindow()
protected void onMeasure(int widthSpec,int heightSpec)
```

attach는 열린 날짜 팝업을 상위 `WmWindow.popupLayer`에 붙인다. detach는 날짜 팝업과 subcard를 제거한다. 측정 시 subcard 폭·높이·위치를 현재 화면에 맞춰 제한한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmContactDetailsView.java`
