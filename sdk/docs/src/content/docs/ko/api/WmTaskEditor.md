---
title: "WmTaskEditor"
slug: ko/api/wmtaskeditor
---
[API 색인](/ko/api/)

```java
public final class WmTaskEditor extends FrameLayout
    implements WmControls.BackHandler,WmControls.RestyleListener,WmControls.LanguageListener
public WmTaskEditor(Context context,Typeface font,Locale locale,Value initial,int accent,int tabColor)
```

작업·메모 2개 페이지. 소유자가 셸, 확인/취소, 검증, 저장을 담당한다. `font`, `locale`, `initial`이 null이면 `IllegalArgumentException`.

복원한 작업 필드 순서를 따른다. 되풀이 미구현. 미리 알림은 절대 날짜·시간을 편집한다. 알림 전달은 소유자 책임이다. 범주는 소유자가 선택기를 연결하거나 텍스트로 입력한다. 비활성 카드 행은 VGA 작업·연락처에서 측정한 39 논리 픽셀 간격, 148픽셀 설명 열, 2픽셀 밑줄을 사용한다. 비활성 선택 필드는 평면이다. 포커스를 받으면 테두리, 목록을 펼치면 화살표를 표시한다. 스크롤바가 숨겨져도 오른쪽 공간을 예약한다. 활성 컨트롤 크기와 Android 글꼴 렌더링은 이식 값이다. 날짜 계산 경계의 동등성은 미검증이다. Activity 재생성 후 초안 복원은 제공하지 않는다.

## Value

```java
public static final class Value {
    public final String subject,categories,notes;
    public final WmDateTimeField.Value start,due,reminder;
    public final int priority,sensitivity;
    public final boolean completed;
    public Value(String subject,int priority,boolean completed,
        WmDateTimeField.Value start,WmDateTimeField.Value due,
        String categories,int sensitivity,String notes);
    public Value(String subject,int priority,boolean completed,
        WmDateTimeField.Value start,WmDateTimeField.Value due,
        String categories,int sensitivity,String notes,WmDateTimeField.Value reminder);
}
public Value value()
```

문자열은 null·NUL 금지. sensitivity는 signed short 범위이며 0=보통, 1=개인, 2=비공개, 3=기밀이다. 그 외 signed-short 값은 사용자가 선택하기 전까지 유지한다. priority는 양수=높음, 0=보통, 음수=낮음. 위반은 `IllegalArgumentException`.

| 필드 | Value 생성 시 처리 |
| --- | --- |
| start / due | null 허용. 유효한 1752-09-14..9999-12-31 날짜, 시·분은 00:00으로 정규화 |
| reminder | null이면 비활성. 같은 날짜 범위의 지역 날짜·시간, 시·분 유지 |

날짜 범위 위반은 `IllegalArgumentException`.

`priority`: 양수=높음, 0=보통, 음수=낮음. 선택 시 +1/0/-1을 기록한다. 선택하지 않은 기존 값은 그대로 유지한다. `sensitivity`: 0=보통, 1=개인, 2=비공개, 3=기밀. 다른 signed-short 값은 선택 표시 없이 보존하며, 사용자 선택 시 교체한다.

`value()`는 현재 입력·선택을 반환하며 저장하지 않는다. 현재 문자열에 NUL이 있으면 `IllegalArgumentException`. 날짜 하나만 null이면 반환값의 빈 날짜를 다른 날짜로 채운다. 원본 작업 저장 규칙이다. 둘 다 null이면 그대로 반환한다. 화면의 날짜 값은 변경하지 않는다. 시작·기한의 시·분은 0으로 정규화한다. 문자열 trim·빈 제목 검사는 소유자 책임이다.

## 소유된 컨트롤

```java
public EditText subjectField()
public EditText categoriesField()
public EditText notesField()
public WmChoice priorityChoice()
public WmChoice statusChoice()
public WmChoice sensitivityChoice()
public WmDateField startField()
public WmChoice reminderChoice()
public WmDateTimeField reminderField()
public WmDateField dueField()
public WmPropertySheet propertySheet()
```

이미 연결된 컨트롤을 반환한다. 다른 부모로 옮기지 않는다. 우선 순위는 높음/보통/낮음, 상태는 완료되지 않음/완료. 탭 전환 시 작업·메모 초안을 유지한다. 페이지 ID는 20007·20044.

편집기가 선택·날짜 드롭다운 및 날짜 변경 콜백을 소유한다. 교체하면 기본 팝업 동작도 교체된다. 선택 변경은 `WmChoice.choose()`를 사용한다. 같은 인덱스 선택은 변경 알림을 발생시키지 않는다. 편집기의 실제 드롭다운 선택은 같은 항목을 다시 골라도 우선 순위·보안 수준을 해당 표준값으로 확정한다. silent setter는 보존된 우선 순위·보안 수준을 갱신하지 않는다. 날짜 팝업은 없음 선택을 제공한다. 빈 날짜를 열면 `WmDateField`가 오늘 날짜를 활성화하며, 팝업을 닫아도 그 초안 날짜를 유지한다. 날짜 필드 어느 곳이든 탭을 완료하면 달력을 연다. 닫힌 카드는 연도 네 자리, 열린 DATE는 지역별 짧은 날짜를 표시한다. 달력에서 월을 이동하면 초안을 즉시 바꾸고 상대 날짜 연동 규칙을 적용한다. 팝업을 닫아도 이동한 날짜를 유지한다. 취소된 터치·비활성 입력은 날짜를 활성화하거나 달력을 열지 않는다.

날짜 변경 규칙: 기한을 지우면 시작도 지운다. 시작만 지우면 기한을 유지한다. 기한이 비어 있을 때 시작을 지정하면 기한도 채운다. 시작 변경은 기존 간격을 unsigned 16-bit 일 수로 보존한다. 간격 65535는 미정이다. 이때 기한은 유지하되 새 시작이 더 늦으면 기한도 시작으로 옮긴다. 기한을 시작보다 앞당기면 시작도 기한으로 옮긴다. 상대 날짜 갱신은 콜백 없이 처리한다. `WmDateField.setValue()`는 이 규칙을 실행하지 않는다. 알림을 발생시키는 편집은 `selectDate()`·`clearDate()`를 사용한다.

일 수 계산은 UTC Gregorian 날짜를 사용하며 언어·일광 절약 시간과 무관하다. 이동한 기한이 SDK 날짜 범위를 벗어나면 시작 변경을 되돌리고 기한을 유지한다. 이 경계 정책·원본의 역방향 연도 계산·장기간 넘침은 원본과의 동등성을 검증하지 않았다. 시작 변경으로 기한이 이동하면 활성 알림도 같은 일 수만큼 이동하며 시각을 유지한다. 기한을 시작보다 앞당기면 활성 알림을 그 날짜 08:00으로 재설정한다. 기한을 지워도 알림은 유지한다. 이동할 알림이 범위를 벗어나면 기존 알림을 유지한다. 원본 넘침 동작과의 동등성은 미검증이다.

## 미리 알림

`Value.reminder == null`은 비활성. 인수 8개 생성자는 null로 설정한다. 인수 9개 생성자는 1752-09-14..9999-12-31 범위의 절대 날짜·시간을 받는다. `value()`는 활성 상태에서만 시각을 엄격히 파싱한다. 불완전한 시각은 `IllegalArgumentException`.

모드와 날짜·시간은 각각 39픽셀 행을 차지한다. 비활성 상태에서도 빈 두 번째 행을 유지한다. 미리 알림을 선택하면 시작, 없으면 기한, 둘 다 없으면 로컬 오늘 날짜의 08:00으로 초기화한다. 다시 활성화하면 재설정한다. 복원한 기본 대체값을 사용하며 OEM 레지스트리 기본값·소리·진동 플래그는 이식하지 않았다.

필수 알림 날짜에는 없음 선택이 없다. 달력의 월 이동은 초안을 즉시 갱신한다. 시각은 직접 입력하거나 48개 30분 간격 후보에서 선택한다. 팝업을 닫아도 초안을 유지한다. 열기 콜백은 편집기가 소유한다. 언어·테마 변경은 알림 값을 유지한다.



## 범주

```java
public void setOnCategoriesOpen(Runnable listener)
public String categories()
public void setCategories(String value)
```

콜백을 연결하면 범주 필드는 읽기 전용이며, 활성 필드 클릭은 열린 팝업을 닫고 콜백을 실행한다. 빈 값은 없음으로 표시한다. null 콜백은 직접 입력을 복원한다. `categoriesField()`의 기존 EditText 반환형은 유지한다.

`categories()`는 원문을 반환하며 날짜·시각을 파싱하지 않는다. `setCategories()`는 null·NUL을 거부하며 실패 시 기존 값을 유지한다. 선택기는 [WmCategoryPicker](/ko/api/wmcategorypicker/)를 사용한다. 소유자가 catalog, 새 이름 입력, 확인·취소, 저장을 담당한다. 확인한 non-null `result()`만 적용한다. 취소·untouched null 결과는 원문을 유지한다.


## 팝업·실시간 외형

```java
public boolean closePopup()
public boolean back()
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
```

`closePopup()`은 연결된 팝업 제거 시 true. `back()`은 팝업에 먼저 전달하고, 처리하지 않으면 팝업을 닫는다. false이면 소유자가 Back을 처리한다. 편집기를 분리하면 팝업을 닫는다.

글꼴·색 적용은 자식도 갱신하는 `WmControls.restyleTree()`를 사용한다. 언어 변경은 입력·선택값을 유지하면서 라벨·선택 항목·탭·날짜를 갱신한다. 두 동작 모두 임시 팝업을 닫는다.

## 호출자의 알림·저장 책임

편집기는 알림 예약·시스템 표시를 수행하지 않습니다. value().reminder는 시간대 없는 지역 날짜·시각이며 null이면 꺼짐입니다. 호출자가 시간대 정책·권한·예약·전달 표식·저장을 처리합니다. 범주 확인도 draft에만 반영하며 최종 저장은 호출자가 결정합니다.

## 런처 통합 예: 작업 알림과 저장

아래는 런처 통합 정책이며 SDK 공통 동작과 구분합니다. 다른 앱은 자신의 저장소·전달 정책을 적용합니다.

런처는 미전달·미완료 작업 중 가장 이른 알림을 예약하고 시스템 알림을 표시한다. 알림을 누르면 작업 애플릿을 연다. 완료·삭제는 예약을 갱신한다. 시작·재부팅·패키지 갱신·시계/시간대 변경·정확한 알림 접근 허용 시 다시 예약한다. Android 13 이상은 알림 권한이 필요하다. 작업 옵션에서 알림·정확한 알림 접근 설정을 연다. 시작 시 권한을 요청하지 않는다. Android 12 이상에서 정확한 알림 접근이 없으면 절전 중 전달 가능한 비정확 알림을 사용한다. 시스템·OEM 제한으로 전달이 늦어질 수 있다.

런처는 현재 시간대의 civil 시각을 사용한다. DST 공백은 앞으로 이동하며 중복 시각은 표준시로 해석한다. 표시 성공 후 해당 알림 값의 전달 표식을 저장한다. 같은 알림 값을 저장하면 표식을 유지한다. 비활성화하거나 값을 바꾸면 표식을 지운다. 표시와 저장은 단일 트랜잭션이 아니므로 그 사이에 종료되면 알림이 반복될 수 있다. API 14..23은 공개 API로 앱 전체의 알림 차단을 확실히 확인할 수 없다. 원본 알림 소리·진동 플래그·말풍선은 아직 이식하지 않았다. SDK 사용 앱은 알림 전달을 직접 구현해야 한다.

런처는 다른 작업과 저장한 이름을 목록에 합친다. 범주 선택의 확인은 작업 초안만 변경한다. 작업 확인 시 범주·목록을 함께 저장하며 원문을 trim하지 않는다. 작업 취소는 선택한 범주·새 이름을 저장하지 않는다. Today 필터는 범주 문자열 전체가 아닌 개별 이름을 비교한다. 잘못된 저장 목록은 편집을 차단하고 원본을 보존한다. WinCE 전역 범주 DB는 이식하지 않았다.

런처는 편집·완료·삭제 저장 직전에 작업 저장소를 다시 읽는다. 화면을 연 뒤 추가된 알림 전달 기록은 유지한다. 알림 시각 변경은 해당 작업의 기록만 지운다. 다른 내용 변경이나 잘못된 저장 데이터는 저장을 거부하고 기존 데이터를 보존한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmTaskEditor.java`
