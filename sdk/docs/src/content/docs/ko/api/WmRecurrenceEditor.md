---
title: "WmRecurrenceEditor"
slug: ko/api/wmrecurrenceeditor
---
[API 색인](/ko/api/)

```java
public final class WmRecurrenceEditor extends FrameLayout implements WmControls.BackHandler,WmControls.RestyleListener,WmControls.LanguageListener
```

Provider 없는 반복 편집 마법사. 시간/유형/종료 범위를 편집하며 종일은 시간 페이지를 생략한다. 시간대, RRULE 호환성, 저장은 호출자가 담당한다. 입력 날짜1900..2999, 간격, 횟수 최대3자리이다.

## 사용 준비

### 생성자

```java
public WmRecurrenceEditor(Context context,Typeface font,Locale locale,Value initial,int accent,int tabColor);
```

font/locale/initial 필수. 기간0..94370400분; 종일은 양의1440분 배수. 잘못된 기간/날짜는 `IllegalArgumentException`. 간격, 횟수999 초과나 표시 불가능한 ordinal/group은 오류 상태로 남고 임의 규칙으로 저장하지 않는다.

NONE seed는 시작 요일의 매주 반복을 선택하며 간격1, 기본 횟수10으로 초기화한다. start/until/duration은 seed 값을 유지한다. 호출자가 이 초안을 수락하거나 취소한다.

View accessor는 editor가 소유한 View를 반환한다. 반환 View의 parent 또는 내부 listener를 교체하면 마법사 동기화가 깨질 수 있다. 조용한 `setSelectedMask()`, 날짜/시각 `setValue()`, RadioButton `setChecked()`는 편집기의 선택/click 동기화를 실행하지 않는다. Panel, 짝지은 radio, 시작 날짜 연결을 갱신하려면 통지하는 선택/click 경로를 사용한다. Count 입력 통지는 COUNT를, UNTIL 날짜 통지는 UNTIL을 선택한다. 두 필드는 현재 종료 mode와 무관하게 활성 상태를 유지한다.

숫자 필드의 기본 글자 크기는 24 논리 픽셀이다. 생성자는 전달된 font를, paired restyle은 regular face를 사용한다.

### PageUp/PageDown

| 필드 | 목록 client 높이 (논리 픽셀) |
| --- | ---: |
| [durationField](#durationfield), [countField](#countfield) | 166 |
| [dailyIntervalField](#dailyintervalfield), [weeklyIntervalField](#weeklyintervalfield), [monthlyDayIntervalField](#monthlydayintervalfield), [monthlyOrdinalIntervalField](#monthlyordinalintervalfield), [dayField](#dayfield), [yearDayField](#yeardayfield) | 170 |

활성화, 닫힌 상태에서 `isExtendedUi()==false`이고 modifier, 화살표 터치가 없으면 PageUp/PageDown은 후보를 이전/다음 페이지로 이동하고 후보 범위로 제한한다. `rowHeight=max(1,fontMetrics.descent-fontMetrics.ascent)`, `rows=height/rowHeight`의 정수 나눗셈으로 계산하며 이동 폭은 rows>1이면 rows−1, 그 외 rows이다. 높이는 고정이고 이동 폭은 현재 글꼴에 따라 달라진다.

숫자 필드를 열면 표의 client 높이를 [choiceDropdownHeight](/ko/api/wmradiomenu/#choicedropdownheight)에 전달해 행 수, 글꼴에 맞춘 바깥 높이를 계산한다. ordinalSelector, groupSelector, yearlyDayMonthSelector, yearlyOrdinalMonthSelector, yearOrdinalSelector, yearGroupSelector는 client 상한 170 논리 픽셀을 사용한다. 최종 팝업 높이는 editor의 `max(0,height−60)`을 넘지 않는다.

## 편집 필드, 반복 유형

### wizard

```java
public WmWizard wizard();
```

페이지, 앞/뒤/완료/취소 관리.

### patternSelector

```java
public WmBoxSelect patternSelector();
```

daily/weekly/monthly/yearly의1/2/4/8 단일 bit 선택.

### weekdaySelector

```java
public WmBoxSelect weekdaySelector();
```

Locale 표시 순서의 여러 요일 선택(반환 rule은 Sunday 기반 bit).

### startField

```java
public WmDateTimeField startField();
```

시작 날짜, 시각 값을 보관하는 편집기 소유 WmDateTimeField입니다. 시간 페이지에는 이 View 대신 시작~종료 시각 문구를 표시합니다. 화면에 붙은 입력란으로 간주하거나 다른 부모로 옮기지 마세요. 프로그램 변경은 위 소유 View, 변경 통지 계약을 따릅니다.

### rangeStartField

```java
public WmDateTimeField rangeStartField();
```

종료 범위 페이지 시작 날짜. 변경 알림이 있는 날짜 선택이 현재 종료 날짜보다 늦으면 종료 날짜도 올립니다. 이 보정은 종료 모드와 종료 필드의 시각을 유지하며 변경 알림을 보내지 않습니다. 알림 없는 `setValue()`와 상호작용 상태 복원에는 보정을 적용하지 않습니다.

### untilField

```java
public WmDateTimeField untilField();
```

종료 날짜 후보.

### durationField

```java
public WmEditableChoice durationField();
```

단위 포함 기간 원문. [PageUp/PageDown](#pageuppagedown).

### intervalField

```java
public WmEditableChoice intervalField();
```

daily는 [dailyIntervalField](#dailyintervalfield), weekly는 [weeklyIntervalField](#weeklyintervalfield), 그 외는 [monthlyIntervalField](#monthlyintervalfield)를 반환한다.

### dailyIntervalField

```java
public WmEditableChoice dailyIntervalField();
```

daily 숫자 간격. [PageUp/PageDown](#pageuppagedown).

### weeklyIntervalField

```java
public WmEditableChoice weeklyIntervalField();
```

weekly 숫자 간격. [PageUp/PageDown](#pageuppagedown).

### monthlyIntervalField

```java
public WmEditableChoice monthlyIntervalField();
```

monthlyOrdinalRadio가 선택되면 [monthlyOrdinalIntervalField](#monthlyordinalintervalfield), 그 외는 [monthlyDayIntervalField](#monthlydayintervalfield)를 반환한다.

### monthlyDayIntervalField

```java
public WmEditableChoice monthlyDayIntervalField();
```

월 특정 날짜 간격. [PageUp/PageDown](#pageuppagedown).

### monthlyOrdinalIntervalField

```java
public WmEditableChoice monthlyOrdinalIntervalField();
```

월 순번 요일 간격. [PageUp/PageDown](#pageuppagedown).

### countField

```java
public WmEditableChoice countField();
```

종료 발생 횟수. [PageUp/PageDown](#pageuppagedown).

### dayField

```java
public WmEditableChoice dayField();
```

월 날짜1..31. [PageUp/PageDown](#pageuppagedown).

### yearDayField

```java
public WmEditableChoice yearDayField();
```

연 날짜1..31. [PageUp/PageDown](#pageuppagedown).

### ordinalSelector

```java
public WmChoice ordinalSelector();
```

월 첫째/둘째/셋째/넷째/마지막.

### groupSelector

```java
public WmChoice groupSelector();
```

월 날/평일/주말/개별 요일.

### monthSelector

```java
public WmChoice monthSelector();
```

현재 연 특정 날짜/순번 선택의 월.

### yearlyDayMonthSelector

```java
public WmChoice yearlyDayMonthSelector();
```

연 특정 날짜의 월.

### yearlyOrdinalMonthSelector

```java
public WmChoice yearlyOrdinalMonthSelector();
```

연 순번 요일의 월.

### yearOrdinalSelector

```java
public WmChoice yearOrdinalSelector();
```

연 요일 순번.

### yearGroupSelector

```java
public WmChoice yearGroupSelector();
```

연 요일 그룹.

### dailyIntervalRadio

```java
public RadioButton dailyIntervalRadio();
```

daily 간격 사용.

### dailyWeekdaysRadio

```java
public RadioButton dailyWeekdaysRadio();
```

주중 매일.

### monthlyDayRadio

```java
public RadioButton monthlyDayRadio();
```

월 특정 날짜.

### monthlyOrdinalRadio

```java
public RadioButton monthlyOrdinalRadio();
```

월 순번 요일.

### yearlyDayRadio

```java
public RadioButton yearlyDayRadio();
```

연 특정 날짜.

### yearlyOrdinalRadio

```java
public RadioButton yearlyOrdinalRadio();
```

연 순번 요일.

### neverRadio

```java
public RadioButton neverRadio();
```

종료 없음.

### untilRadio

```java
public RadioButton untilRadio();
```

종료 날짜.

### countRadio

```java
public RadioButton countRadio();
```

횟수 종료.

## 완료, 취소, 값

### setOnFinish

```java
public void setOnFinish(Runnable listener);
```

유효한 완료 또는 되풀이 제거에서 popup을 닫은 뒤 callback을 동기 실행한다. value()를 읽어 소유자가 저장하고 View를 닫는다. null은 callback 해제이며 non-null 참조는 교체까지 유지한다. 완료는 검증, callback의 `IllegalArgumentException`을 모두 잡아 validation box로 메시지를 표시한다. 되풀이 제거는 먼저 제거 상태, 빈 pattern을 설정하며 해당 callback 예외는 전파한다. 두 경로 모두 callback의 부수 효과를 되돌리지 않는다.

### setOnCancel

```java
public void setOnCancel(Runnable listener);
```

취소 명령에서 popup을 닫고 callback을 동기 실행하며 원본 저장 데이터는 변경하지 않는다. null은 해제이며 non-null 참조는 교체까지 유지한다. Callback 예외는 popup을 닫은 뒤 전파한다.

### value

```java
public Value value();
```

기간, 활성 유형, 간격/요일/날짜, 종료 범위를 검증하여 새 Value를 반환한다. UNTIL은 시작 날짜보다 빠를 수 없고 COUNT는 양수 최대3자리이다. 기간은 rule 주기 한계 이하. 위반은 `IllegalArgumentException`. 제거 상태이면 원래 start/until/duration을 유지한 NONE rule.

## 상태 보존

### saveInteractionState

```java
public Bundle saveInteractionState();
```

schema1로 원래 seed, 전체 입력 원문, 선택, wizard 페이지, 오류, 초점을 보존한다. 파싱/blur commit을 하지 않는다. nested popup과 IME 조합은 제외한다.

### initialValue

```java
public static Value initialValue(Bundle state);
```

snapshot에서 새 editor 생성용 원래 Value를 추출, 검증한다. 전체 draft 검증은 생성한 editor의 validateInteractionState에서 수행한다. schema1은 정확히16개 key, original seed는 정확히5개 key가 필요하다. 잘못된 seed/schema는 `IllegalArgumentException`.

### validateInteractionState

```java
public void validateInteractionState(Bundle state);
```

원래 seed가 이 editor와 일치해야 한다. 모든 child, 원문 길이, 후보, 선택, 요일, focus 일관성을 검증한다. 잘못되면 `IllegalArgumentException`. 아직 미완성인 draft의 최종 semantic validation은 value()에서 수행한다.

### restoreInteractionState

```java
public void restoreInteractionState(Bundle state);
```

모든 nested popup을 닫은 후 호출한다. 검증 후 조용히 전체 초안을 복원한다. 재진입부터 검사하며 전체 검증 후 열린 popup을 검사하고 소유자 값을 바꾼다. 열린 popup 또는 재진입은 `IllegalStateException`; 잘못된 snapshot은 `IllegalArgumentException`이다. 값 적용, 외부 listener 예외에 rollback은 보장하지 않는다.

## 언어, 팝업, 수명

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

공유 catalog 언어에 해당하는 Locale을 setLocale에 전달한다. 상태 보존, popup 처리는 setLocale 계약을 따른다.

### setLocale

```java
public void setLocale(Locale next);
```

요일 model bit, 기간 입력 grammar, 원문/캐럿을 보존하며 문구를 바꾼다. 유효한 기간의 원문이 이전 Locale의 `durationLabel(parsedMinutes)`와 같을 때만 다시 표시한다. 후보 목록 밖의 형식화된 기간도 포함한다. 그 외 원문은 편집될 때까지 기존 파싱 Locale을 유지한다. 일반 popup은 닫고 오류 message box는 relabel한다. 같은 Locale이면 popup 처리까지 no-op이다. null은 `IllegalArgumentException`.

### setError

```java
public void setError(String authored);
```

오류를 안내 문구로 변환한 뒤 확인 버튼이 있는 WmMessageBox로 표시합니다. null/빈 문자열은 오류를 지웁니다. 종료일 순서, 1..31 범위, 존재하지 않는 월/일, 빈 요일 선택 오류는 해당 안내로 바꿉니다. 그 밖의 비어 있지 않은 문자열에 한글 음절(가..힣)이 없으면 입력 대신 "올바른 되풀이 값을 입력하세요."를 언어 catalog로 표시합니다. 임의 영문 메시지를 그대로 표시하지 않습니다. 확인 후 이전 입력 초점, 캐럿을 복원합니다.

### closePopup

```java
public boolean closePopup();
```

팝업 callback 세대를 무효화하고 현재 overlay를 제거한다. 실제 팝업을 제거했으면 true, 없으면 false.

### back

```java
public boolean back();
```

하위 popup을 먼저 처리하고, 없으면 wizard의 이전 페이지/취소로 전달한다. 처리 여부를 반환한다.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Android 분리 hook. 현재 popup overlay를 닫는다.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

popup 글꼴, accent와 범위 제목의 굵은 글꼴을 갱신한다. 오류 message box는 유지하고 다른 popup은 닫는다. 자식 전체는 WmControls.restyleTree로 갱신한다.

## Value

```java
public static final class Value
```

### 상수와 필드

```java
public final WmRecurrenceRule rule;
public final WmDateTimeField.Value start;
public final WmDateTimeField.Value until;
public final int durationMinutes;
public final boolean allDay;
```

rule/start/until은 model, durationMinutes는 civil 분 기간, allDay는 종일 정책. until은 종료 날짜 후보이며 rule.endMode에 따라 사용한다.

### 생성자

```java
public Value(WmRecurrenceRule rule,WmDateTimeField.Value start,WmDateTimeField.Value until,int durationMinutes,boolean allDay);
```

rule/start/until null은 `IllegalArgumentException`. 날짜, duration 정책은 실제 editor 생성/완료 때 검증한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmRecurrenceEditor.java`
