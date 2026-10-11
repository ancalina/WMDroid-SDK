---
title: "WmCategoryPicker"
slug: ko/api/wmcategorypicker
---
[API 색인](/ko/api/)

```java
public final class WmCategoryPicker extends FrameLayout implements WmControls.RestyleListener
```

스크롤 가능한 체크 목록으로 범주 선택 초안을 편집한다. 호출자가 범주 catalog·New/OK/Cancel 명령·저장을 담당한다. UI 스레드에서 생성·갱신한다. OK에서 `result()`를 읽고, Cancel에서는 초안을 버린다.

## 생성자

```java
public WmCategoryPicker(Context context,Typeface font,Locale locale,String[] catalog,String selected);
```

`font`·`locale`·`catalog`·`selected`는 null 불가다. catalog 항목은 null·빈 문자열·NUL·쉼표·세미콜론을 허용하지 않는다. `selected`도 NUL을 허용하지 않는다. 위반은 `IllegalArgumentException`이다. 빈 catalog·빈 선택은 허용한다. 기존 catalog 항목에는 새 이름의 공백 제거·255자 제한을 적용하지 않는다.

catalog를 내부 목록에 복사하고, 선택 문자열에만 있는 범주도 목록에 추가한다. 쉼표·세미콜론으로 선택을 구분하며 토큰 앞뒤 U+0020 공백을 제거한다. 원래 선택 문자열은 첫 변경까지 그대로 보존한다.

정렬·중복·선택 비교는 생성 시 `locale`로 만든 `Collator.SECONDARY`를 사용한다. 동등한 catalog 이름은 처음 추가한 표기를 유지한다. Locale은 생성 후 고정된다.

## saveInteractionState

```java
public Bundle saveInteractionState();
```

새 schema2 Bundle에 아직 수락하지 않은 선택 초안을 저장한다. original·nullable draft·현재 catalog 사본·요청 스크롤 위치·nullable reveal을 포함한다. reveal은 layout을 기다리는 마지막 새 행의 이름이며 유효한 layout에서 스크롤을 요청하면 null로 지운다. 변경 없음의 null draft, 모두 해제한 빈 draft, 변경 후 되돌린 draft를 구분하여 유지한다. catalog에는 새로 만든 이름도 포함한다. 소유자의 수락된 값은 바꾸지 않는다.

## fromInteractionState

```java
public static WmCategoryPicker fromInteractionState(Context context,Typeface font,Locale locale,Bundle state);
```

snapshot에서 부모 없는 새 picker를 만든다. 원문·변경 상태·catalog·요청 스크롤 위치를 복원한다. schema2의 non-null reveal은 다음 유효한 layout에서 표시할 행을 복원한다. 저장된 스크롤은 [WmScrollView](/ko/api/wmscrollview/)의 배치 시 가용 범위로 제한한다. 복원만으로 소유자의 범주 값을 수락하거나 저장하지 않는다.

| key | 필수 값 |
| --- | --- |
| schema | Integer `1` 또는 `2` |
| original | NUL 없는 String |
| draft | null 또는 NUL 없는 String; key는 필수 |
| catalog | 생성자 조건을 만족하는 String[] |
| scroll | `0` 이상 Integer |
| reveal | schema2에만 필수; null 또는 NUL 없는 String |

schema1은 reveal 없는 5개 key, schema2는 reveal 포함 6개 key만 허용한다. null state·잘못된 schema/타입/값·누락 또는 추가 key는 `IllegalArgumentException`이다. non-null draft의 각 범주 토큰은 복원 catalog에서 찾아야 하며, original의 범주도 생성자에서 catalog에 추가한다. 알 수 없는 draft 범주는 `IllegalArgumentException`이다. non-null reveal은 복원 catalog의 알려진 행이어야 하며 non-null draft가 필요하다. 이를 위반하면 `IllegalArgumentException`이다. 유효한 layout에서 스크롤을 요청할 때 reveal을 지운다.

Context·글꼴·Locale은 snapshot에 포함하지 않는다. 호출자가 전달한 Locale로 Collator를 새로 만든다. 기존 정렬·이름 비교 정책을 유지하려면 원래 Locale을 전달한다. 소유자가 New/OK/Cancel UI를 다시 연결하고 OK에서만 `result()`를 적용한다.

## value · result · isChanged

```java
public String value();
public String result();
public boolean isChanged();
```

| 상태 | value() | result() | isChanged() |
| --- | --- | --- | --- |
| 변경 없음 | 원래 선택 문자열 | null | false |
| 변경 후 선택 있음 | 현재 초안 | 현재 초안 | true |
| 변경 후 모두 해제 | 빈 문자열 | 빈 문자열 | true |

`result()==null`은 변경 없음이며 선택 비우기가 아니다. 선택을 바꾼 뒤 원래 상태로 되돌려도 변경 초안은 남는다. 동일 상태의 `setChecked` 호출은 변경을 만들지 않는다.

## contains

```java
public static boolean contains(String selected,String name,Locale locale);
```

뷰를 만들지 않고 개별 범주 이름의 포함 여부를 반환한다. 생성자와 같은 토큰 문법·`Collator.SECONDARY`를 사용한다. null 인수 또는 selected/name의 NUL은 `IllegalArgumentException`. 빈 선택·빈 이름은 false.

## categoryNames

```java
public String[] categoryNames();
```

현재 정렬된 catalog의 새 배열을 반환한다. 배열 수정은 내부 목록을 바꾸지 않는다. 원래 선택에만 있던 이름과 새로 추가한 이름도 포함한다.

## isChecked · setChecked

```java
public boolean isChecked(String name);
public void setChecked(String name,boolean checked);
```

`name`을 같은 Collator로 catalog에서 찾는다. null·알 수 없는 이름은 `IllegalArgumentException`이다. `isChecked`는 현재 선택 문자열에서 해당 이름과 동등한 토큰이 있는지 반환한다.

`setChecked`는 상태가 같으면 아무 작업도 하지 않는다. 체크하면 catalog의 표기를 사용하여 끝에 `", "`와 이름을 추가한다. 현재 선택이 비었으면 이름만 사용한다. 체크 해제는 일치 토큰과 인접 구분자를 제거하며 다른 범주는 유지한다. 같은 범주가 선택 문자열에 여러 번 있으면 호출 한 번으로 모두 제거되는 것은 보장하지 않는다. 해당 행의 CheckBox도 갱신한다. 프로그램적 `setChecked()`·`addCategory()`는 활성 상태·표시 여부를 검사하지 않아 picker가 비활성·숨김 상태여도 초안을 바꿀 수 있다.

## validateNewName

```java
public static String validateNewName(String input);
```

앞뒤 U+0020 공백만 제거한 이름을 반환한다. 탭 등 다른 공백은 유지한다. null·NUL·제거 후 빈 이름·쉼표·세미콜론·255 UTF-16 code unit 초과는 `IllegalArgumentException`이다. 길이는 Unicode code point 수가 아닌 `String.length()` 기준이다. catalog 중복은 검사하지 않는다.

## addCategory

```java
public boolean addCategory(String input);
```

`validateNewName`으로 검증한다. catalog에 Collator 기준으로 동등한 이름이 있으면 false를 반환하며 목록과 선택을 바꾸지 않는다. 새 이름이면 추가·정렬·행 재구성 후 체크하고 true를 반환한다. 새 범주는 변경 초안에 포함되며 실제 catalog 저장은 호출자가 수행한다.

행 재구성 후 이전 CheckBox callback은 현재 초안을 변경하지 못한다. 생성 시 Locale의 정렬 규칙은 유지한다.

새 행은 다음 유효한 layout에서 화면에 들어오도록 스크롤을 요청한다. 여러 이름을 layout 전에 추가하면 마지막 이름을 표시한다. 행이 화면보다 크면 행의 상단을 표시한다. 중복 입력은 스크롤을 요청하지 않는다.

## onLayout

```java
protected void onLayout(boolean changed,int left,int top,int right,int bottom);
```

자식 배치 후 `changed=false`여도 대기 중인 새 행 reveal을 처리한다. Scroll 또는 행 높이가 양수가 아니면 대기 행을 유지한다. 행이 현재 rows container에서 분리됐으면 대기를 지운다. 그 외에는 대기를 지운 뒤 스크롤을 요청하며 [WmScrollView](/ko/api/wmscrollview/)는 요청을 연기할 수 있다. 대기 행이 없으면 reveal을 요청하지 않는다.

## onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int face);
```

현재 CheckBox들의 공유 글꼴·색을 갱신한다. regular 글꼴은 이후 새 행에도 사용한다. catalog·선택 초안·정렬 Locale은 유지한다. 행은 단일 줄, 최소 높이 `44` 논리 픽셀, WRAP_CONTENT를 사용하여 큰 글꼴에 맞춰 높아질 수 있다. 이 크기는 Android 적응값이며 원본 GDI 픽셀과의 일치를 보장하지 않는다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmCategoryPicker.java`

## setEnabled

```java
@Override public void setEnabled(boolean enabled);
```

`setEnabled(false)`는 현재 체크박스·새로 만든 체크박스·공통 스크롤 입력을 비활성화한다. 진행 중인 터치를 취소한다. 다시 활성화해도 이전 터치의 release는 값을 바꾸지 않는다. 명시적인 `setChecked()`·`addCategory()`는 계속 초안을 갱신한다.
