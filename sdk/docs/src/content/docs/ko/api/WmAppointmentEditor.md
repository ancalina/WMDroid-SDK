---
title: "WmAppointmentEditor"
slug: ko/api/wmappointmenteditor
---
[API 색인](/ko/api/)

```java
public final class WmAppointmentEditor extends FrameLayout implements WmControls.BackHandler,WmControls.RestyleListener,WmControls.ModalOwner,WmControls.LanguageListener
```

약속/메모 두 페이지 편집 카드. 제목, 장소, 시작/끝, 종일, 반복, 알림, busy, sensitivity와 소유자 연결 범주 필드를 제공한다. 참석자는 미완성이다. 소유자가 Provider identity, capability, 저장, 시간대, 셸 명령을 담당한다. 내장 일정 앱의 범주 Provider 저장 연결은 아직 구현되지 않았다. View, 상호작용 API는 UI 스레드에서 호출한다.

## 사용 준비

### 상수와 필드

```java
public static final int FREE = 0;
public static final int TENTATIVE = 1;
public static final int BUSY = 2;
public static final int OUT_OF_OFFICE = 3;
public static final int NORMAL = 0;
public static final int PERSONAL = 1;
public static final int PRIVATE = 2;
public static final int CONFIDENTIAL = 3;
```

busy FREE=0/TENTATIVE=1/BUSY=2/OUT_OF_OFFICE=3; sensitivity NORMAL=0/PERSONAL=1/PRIVATE=2/CONFIDENTIAL=3.

### 생성자

```java
public WmAppointmentEditor(Context context,Typeface font,Locale locale,Value initial,int accent,int tabColor);
```

font/locale/initial은 null 불가(`IllegalArgumentException`). start/end 편집 범위1900..2999. 시작 사용자 변경은 civil 분 기간을 유지하여 끝을 이동한다. 직접 입력한 끝이 시작보다 빠르면 시작을 끝으로 옮긴다. 끝 시각 후보 선택만 같은 날 더 이른 시각을 다음 날로 감싼다.

### 선택 팝업

종일, 반복, busy, sensitivity, 알림 활성, 알림 단위 메뉴는 [choiceDropdownHeight(256)](/ko/api/wmradiomenu/#choicedropdownheight)으로 글꼴 행 높이에 맞춘 완전한 행과 테두리 높이를 계산한다. `256`은 테두리를 제외한 논리 픽셀 상한이다. 시작/끝 시각, 알림 수량 메뉴는 [getClosedDropdownClientHeight()](/ko/api/wmeditablechoice/)를 전달하며 이 편집기의 기본값은 `256`이다. 최종 높이는 `max(0, editorHeight − WmMetrics.TAB)`으로 제한한다. 일반 선택 메뉴의 전체 높이가 이 viewport를 넘으면 [WmScrollView](/ko/api/wmscrollview/)로 감싸 마지막 행까지 스크롤, 키보드로 접근할 수 있게 한다. 넘지 않으면 메뉴를 직접 배치한다. 시각, 수량 메뉴는 항상 WmScrollView로 감싼다.

### 필드 접근과 callback

필드 getter는 복사본 대신 실제 자식 객체를 반환한다. 텍스트, 선택 변경은 다음 `value()`와 snapshot에 반영된다. 자식을 편집기에 유지한다. 내부 open/change/focus listener를 교체하면 팝업 처리, 동기화가 끊어진다. Callback setter는 기존 callback을 교체하며 다음 교체까지 참조를 보존한다. null은 해제한다. Callback 예외는 선행 편집기 변경 이후 전파된다.

## 약속 필드

### subjectField

```java
public EditText subjectField();
```

한 줄 제목 원문 편집기. Editable 변경을 공백 정리 없이 읽는다.

### locationField

```java
public EditText locationField();
```

한 줄 장소 원문 편집기. Editable 변경을 공백 정리 없이 읽는다.

### notesField

```java
public EditText notesField();
```

메모 페이지의 여러 줄 편집기. Editable 변경이 저장할 메모가 된다.

### startField

```java
public WmDateTimeField startField();
```

시작 civil 날짜, 시각의 실제 자식입니다. 유효 변경 통지를 받고 기존 끝도 유효하게 파싱되면 시작의 civil 분 차이만큼 끝을 이동합니다. 끝 draft가 미완성이거나 보정값을 만들 수 없으면 끝 이동을 생략하고 기존 초안을 보존합니다. 이어서 현재 시작을 읽을 수 있으면 반복 시작과 끝 후보 기준을 갱신합니다. 저장 시간대, DST 검증은 소유자 책임입니다. 직접 setValue는 무통지이므로 이 연동을 실행하지 않습니다.

### endField

```java
public WmDateTimeField endField();
```

끝 civil 날짜/시각. 시작보다 이른 유효한 변경 통지를 받으면 시작을 끝으로 옮긴다. 직접 `setValue()`는 통지하지 않는다. 끝 시각 후보는 시작 시각부터 30분 간격으로 48개를 만들고 자정에서 순환한다. 현재 입력과 정확히 일치하는 후보만 선택 표시하며, 현재 입력에 맞춰 후보 순서를 바꾸지 않는다.

## 종일 시작, 끝 날짜

종일 start는 첫 날짜, end는 마지막 포함 날짜입니다. value()는 시, 분을00:00으로 정규화합니다.

| 종일 표시 | start | end | 반복 편집 기간 |
| --- | --- | --- | --- |
|2026-10-11 하루|2026-10-11 00:00|2026-10-11 00:00|1440분|
|10월11~12일|2026-10-11 00:00|2026-10-12 00:00|2880분|

일반 약속은 끝−시작 civil 분, 종일은 여기에1440분을 더합니다. Value 생성자는 관계를 강제하지 않으며 저장소 종료 규칙 변환, 기간 검증은 소유자 책임입니다.


### allDayChoice

```java
public WmChoice allDayChoice();
```

아니요0/예1 선택. `choose(1)`은 시작, 끝 시각 필드를 비활성화하며 입력 문구는 유지한다. 직접 `setSelectedIndex()`는 편집기에 통지하지 않아 시각 활성 상태를 갱신하지 않는다. `value()`는 현재 선택 인덱스를 사용한다.

### recurrenceChoice

```java
public WmRecurrenceChoice recurrenceChoice();
```

시작 날짜에 연결된 반복 preset/편집 콤보. rule 변경은 다음 model, snapshot에 반영된다. 내장 마법사를 사용하려면 설치된 Edit/open callback을 유지한다.

### reminderField

```java
public WmAppointmentReminder reminderField();
```

상대 알림 composite. 활성, 단위 선택과 수량 초안을 다음 model, snapshot에서 읽는다. 행을 숨겨도 데이터는 비활성화하지 않는다.

### setReminderVisible

```java
public void setReminderVisible(boolean visible);
```

알림 줄을 숨기거나 표시하고 popup을 닫는다. 숨겨도 알림 값은 보존한다.

### busyChoice

```java
public WmChoice busyChoice();
```

0..3 busy 선택. 초기의 알 수 없는 signed-short 값은 미선택으로 표시한다. 선택 인덱스가 있으면 `value()`에서 보존 값 대신 사용한다. 미선택으로 바꾸면 편집기 choice callback이 마지막으로 기록한 값을 사용한다.

### sensitivityChoice

```java
public WmChoice sensitivityChoice();
```

0..3 보안 수준 선택. 초기의 알 수 없는 signed-short 값은 미선택으로 표시한다. 선택 인덱스가 있으면 `value()`에서 보존 값 대신 사용한다. 미선택으로 바꾸면 편집기 choice callback이 마지막으로 기록한 값을 사용한다.

### setStatusFieldsVisible

```java
public void setStatusFieldsVisible(boolean visible);
```

busy/sensitivity 두 줄을 함께 숨기거나 표시한다. metadata를 보존할 수 없는 Provider에서 숨긴다. 값은 유지하며 popup을 닫는다.

### setRecurrenceEnabled

```java
public void setRecurrenceEnabled(boolean enabled);
```

반복 composite와 choice의 활성 상태를 설정한다. false면 편집기의 모든 popup을 닫고 rule은 유지한다. true는 popup을 다시 열지 않는다.

## 페이지, 범주, 달력

### propertySheet

```java
public WmPropertySheet propertySheet();
```

약속0/메모1 페이지 관리기. 통지하는 페이지 선택은 편집기 popup을 닫고 등록된 페이지 callback을 실행한다. sheet listener를 교체하면 이 처리가 끊어지므로 `setOnPageChanged()`를 사용한다.

### setOnCategoriesOpen

```java
public void setOnCategoriesOpen(Runnable listener);
```

non-null이면 범주 행을 VISIBLE, 활성으로 만들고, 클릭 시 현재 SDK popup을 닫은 뒤 callback을 실행한다. null이면 행을 GONE, 비활성으로 만든다. 범주 값은 유지한다. 소유자가 [WmCategoryPicker](/ko/api/wmcategorypicker/) 등 선택 UI와 New/OK/Cancel 명령, catalog, 저장을 구성한다. 범주 초점이 포함된 snapshot을 검증, 복원하려면 callback을 먼저 연결한다.

### categoriesField

```java
public TextView categoriesField();
```

범주 표시 TextView를 반환한다. 이를 포함한 행 View는 아니다. 값이 비었으면 번역된 “없음”을 표시하며, 그 외에는 원문을 표시한다. 이 표시 문구는 저장할 범주 값이 아니다. 원문은 `categories()`로 읽는다. 직접 `setText()`는 표시만 바꾸며 `setCategories()`는 model, 표시를 함께 바꾼다.

### categories

```java
public String categories();
```

현재 범주 원문을 반환한다. 시간, 알림을 파싱하거나 commit하지 않는다. 빈 문자열은 범주 없음이다. 선택 UI를 열 때 이 값을 전달하고 취소, 복원 동안 소유자 값과 picker 초안을 구분하여 보존한다.

### setCategories

```java
public void setCategories(String categories);
```

범주 원문과 표시를 갱신한다. null, NUL 포함은 `IllegalArgumentException`이다. 빈 문자열은 범주 없음을 뜻한다. 구분자, 공백, 표기를 정규화하지 않는다. 선택 UI에서 OK로 수락한 값만 적용하며 Cancel은 기존 값을 유지한다.

### calendarField

```java
public TextView calendarField();
```

처음에는 비활성인 계정/캘린더 선택 TextView. 직접 `setText()`는 표시만 바꾸며 `setCalendarName()`은 이후 재번역할 이름을 보존한다.

### setOnCalendarOpen

```java
public void setOnCalendarOpen(Runnable listener);
```

non-null이면 calendar 필드를 활성화한다. 클릭은 현재 popup을 닫은 뒤 callback을 실행한다. null은 필드를 비활성화한다. 캘린더 이름, Provider 계정은 바꾸지 않는다.

### setOnPageChanged

```java
public void setOnPageChanged(Runnable listener);
```

통지하는 페이지 변경을 commit한 뒤 popup을 닫고 callback을 실행한다. sheet의 프로그램적 선택도 포함한다. 같은 페이지 선택, 조용한 snapshot 복원은 변경을 통지하지 않는다. null로 해제한다.

### setCalendarName

```java
public void setCalendarName(String name);
```

표시할 캘린더 이름. null이면 기본 선택 문구. 실제 계정 선택은 바꾸지 않는다.

## 값, 오류, 상태 보존

### setError

```java
public void setError(String authored);
```

authored 오류를 catalog로 번역하여 표시한다. null/빈 문자열은 비운다.

### value

```java
public Value value();
```

현재 model을 만든다. 종일은 시각을00:00으로 정규화한다. 잘못된 날짜/시각, 알림 생성은 `IllegalArgumentException("올바른 시각을 입력하세요.")`으로 감싼다. 이후 반복 알림 lead를 검증한다. 알림 수량은 유효한 입력 또는 마지막 유효 수량을 사용하며 표시 초안을 commit, 교정하지 않는다. 제목 원문과 알 수 없는 signed-short 상태는 보존한다. 시작/끝 기간, Provider 저장 검증은 호출자가 수행한다.

### saveInteractionState

```java
public Bundle saveInteractionState();
```

schema3 초안 snapshot. 범주 원문, 문자열, 캐럿, 초점, 페이지, 스크롤, 날짜/시각, 반복, 알림, 오류와 열린 반복 마법사 초안을 포함한다. 잘못된 시각을 파싱하거나 blur commit하지 않는다. 계정 identity/capability, 일반 popup, IME 조합은 별도 보존한다.

### validateInteractionState

```java
public void validateInteractionState(Bundle state);
```

schema1/2/3과 모든 child/caret/status/focus를 검증한다. schema3 범주는 String, NUL 없음 조건을 검사한다. 범주 초점은 연결된 callback이 필요하다. 반복 popup 초안은 반복 편집 활성, 약속 페이지에서만 허용한다. schema1/2/3은 각각 정확히18/19/20개의 key가 필요하다. 잘못된 타입, child 상태, 비정규 반복 구성, 음수 스크롤, 잘못된 페이지/캐럿/상태/초점은 `IllegalArgumentException`이다. 검증은 초안을 적용하지 않지만 반복 child 초안이 있으면 임시 반복 편집기를 생성한다.

### restoreInteractionState

```java
public void restoreInteractionState(Bundle state);
```

현재 popup을 모두 닫은 뒤 호출한다. 전체 검증 후 조용히 복원하며 포함된 반복 마법사는 다시 연다. schema1/2의 범주는 빈 문자열로 복원하며 schema3은 원문을 복원한다. 범주 callback, 행 표시 정책과 소유자 선택 UI는 snapshot에 포함하지 않는다. 재진입부터 검사하고 전체 상태 검증 후 열린 popup을 검사한다. 열린 popup/재진입은 `IllegalStateException`; 잘못된 snapshot은 `IllegalArgumentException`이다. 검증된 값을 적용하거나 외부 listener가 실행될 때 예외가 나도 rollback하지 않는다. 캐럿, 초점, 요청한 form 스크롤 위치는 자식 control로 복원한다.

## 언어, 팝업, 수명

### setLocale

```java
public void setLocale(Locale locale);
```

입력 초안, 알 수 없는 상태, 반복 마법사를 보존하고 설정 문구를 바꾼다. 일반 popup은 닫는다. 같은 Locale은 popup 처리까지 포함하여 no-op이다. null은 `IllegalArgumentException`이다. 캘린더 이름, 범주 원문은 번역하지 않는다.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

공유 catalog 언어에 해당하는 Locale을 setLocale에 전달한다. 상태 보존, popup 처리는 setLocale 계약을 따른다.

### closePopup

```java
public boolean closePopup();
```

overlay가 없어도 callback 세대를 증가시켜 닫힌 popup의 callback을 무효화하고 현재 overlay를 제거한다. 실제 팝업을 제거했으면 true, 없으면 false.

### blocksOwnerCommands

```java
public boolean blocksOwnerCommands();
```

반복 마법사가 popup이면 true. 저장 등 소유자 명령 차단에 사용한다.

### inputMethodOwner

```java
public View inputMethodOwner();
```

현재 popup View 또는 null. 소유자가 IME 처리를 해당 하위 편집에 위임할 때 사용한다.

### back

```java
public boolean back();
```

하위 popup이 Back을 처리하면 true; 그 외 현재 popup을 닫은 결과. popup이 없으면 false.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Android 분리 hook. 현재 popup overlay를 닫는다.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

다음 popup의 regular 글꼴, bold 글꼴(null이면 regular), 선택 색을 저장한다. 열린 반복 마법사는 유지하고 다른 popup은 닫는다. 이 hook은 tabColor를 적용하거나 기존 자식을 restyle하지 않는다. [WmControls.restyleTree](/ko/api/wmcontrols/#restyletree)를 사용한다.

## Value

```java
public static final class Value
```

### 상수와 필드

```java
public final String subject;
public final String location;
public final String notes;
public final String categories;
public final WmDateTimeField.Value start;
public final WmDateTimeField.Value end;
public final boolean allDay;
public final int busyStatus;
public final int sensitivity;
public final WmReminder reminder;
public final WmRecurrenceRule recurrenceRule;
public final WmDateTimeField.Value recurrenceUntil;
```

불변 원문 subject/location/notes/categories, civil start/end, allDay, busyStatus/sensitivity, reminder, recurrenceRule/recurrenceUntil. 편집기는 종일 end를 마지막 포함 날짜로 처리하지만 Value 자체는 이 규약을 강제하지 않는다. 입력 raw 제목은 저장 경계에서 WmAppointmentText로 별도 변환한다.

### 생성자

```java
public Value(String subject,String location,String notes,WmDateTimeField.Value start,WmDateTimeField.Value end,boolean allDay);
public Value(String subject,String location,String notes,WmDateTimeField.Value start,WmDateTimeField.Value end,boolean allDay,WmRecurrenceRule rule,WmDateTimeField.Value until);
public Value(String subject,String location,String notes,WmDateTimeField.Value start,WmDateTimeField.Value end,boolean allDay,WmRecurrenceRule rule,WmDateTimeField.Value until,int busyStatus,int sensitivity);
public Value(String subject,String location,String notes,WmDateTimeField.Value start,WmDateTimeField.Value end,boolean allDay,WmRecurrenceRule rule,WmDateTimeField.Value until,int busyStatus,int sensitivity,WmReminder reminder);
public Value(String subject,String location,String notes,WmDateTimeField.Value start,WmDateTimeField.Value end,boolean allDay,WmRecurrenceRule rule,WmDateTimeField.Value until,int busyStatus,int sensitivity,WmReminder reminder,String categories);
```

모든 참조값은 null 불가. busyStatus/sensitivity는 signed short 범위를 허용한다(0..3 밖 값도 유지). 위반은 `IllegalArgumentException`. 짧은 overload 기본값: 반복없음/until=end, BUSY/NORMAL, 비활성15분 알림. categories 인자가 없는 overload는 빈 범주를 사용한다. categories는 NUL을 허용하지 않으며 원문을 보존한다. model 생성은 편집기 연도 범위1900..2999, 시각 순서, 기간, Provider 호환성을 검증하지 않는다.



소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmAppointmentEditor.java`
