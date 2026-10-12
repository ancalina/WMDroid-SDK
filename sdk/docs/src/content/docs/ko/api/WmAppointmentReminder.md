---
title: "WmAppointmentReminder"
slug: ko/api/wmappointmentreminder
---
[API 색인](/ko/api/)

```java
public final class WmAppointmentReminder extends LinearLayout implements WmControls.LanguageListener
```

약속의 상대 알림 사용 콤보와 수량, 단위를 표시한다. 비활성 알림은 숫자 줄을 숨긴다. 소유자가 각 필드의 팝업을 제공한다.

### 생성자

```java
public WmAppointmentReminder(Context context,Typeface font,Locale locale,WmReminder initial,int selection);
```

font/locale/initial은 null 불가(`IllegalArgumentException`). initial의 enabled, quantity, unit으로 각 필드를 초기화합니다. selection은 단위 인덱스가 아닌 Android 선택 강조색 int입니다. 수량은 ASCII 숫자 최대4자리이며 단위 변경은 수량을 유지합니다.

### value

```java
public WmReminder value();
```

현재 유효 숫자 또는 마지막 commit 수량으로 새 `WmReminder`를 반환한다. 미완성 초안을 commit하거나 바꾸지 않는다.

### saveInteractionState

```java
public Bundle saveInteractionState();
```

schema1 Bundle로 commit fallback, 숫자 원문, 후보, 편집 상호작용을 보존한다. blur commit을 수행하지 않는다.

### validateInteractionState

```java
public static void validateInteractionState(Bundle state);
```

완전한 schema1 snapshot을 검증한다. draft는 빈 값 또는 ASCII 1..4자리 숫자이다. 잘못된 스키마, 값, 후보, 편집 상태는 `IllegalArgumentException`.

### restoreInteractionState

```java
public void restoreInteractionState(Bundle state);
```

팝업을 닫은 상태에서 조용히 복원한다. 초점 배치는 소유자가 담당한다. 열린 enable/unit/quantity 팝업은 `IllegalStateException`; 잘못된 snapshot은 `IllegalArgumentException`.

### commitQuantity

```java
public void commitQuantity();
```

유효한 1..4자리 ASCII 숫자(0 포함)를 commit한다. 빈/잘못된 입력은 마지막 수량의 문자열로 되돌린다. 일반 blur에서도 호출된다.

### enabledChoice

```java
public WmChoice enabledChoice();
```

소유된 알림 없음/사용 콤보를 반환한다. 열기 listener로 popup을 연결한다.

### quantityField

```java
public WmEditableChoice quantityField();
```

소유된 수량 편집 콤보를 반환한다. 열기 listener로 후보 popup을 연결한다.

### unitChoice

```java
public WmChoice unitChoice();
```

소유된 분/시간/일/주 콤보를 반환한다. 열기 listener로 popup을 연결한다.

### setEditingEnabled

```java
public void setEditingEnabled(boolean enabled);
```

전체 입력 편집 정책을 지정한다. 알림 사용 값과 수량은 유지하며, 수량 줄은 알림 사용 여부에 따라 숨기거나 표시한다.

### setLocale

```java
public void setLocale(Locale locale);
```

사용/단위 문구만 새 catalog로 바꾼다. 수량, 선택은 유지한다. null은 `IllegalArgumentException`.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

공유 catalog 언어의 Locale을 setLocale에 전달하여 알림 사용, 단위 문구를 바꾼다. 수량과 선택은 유지한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmAppointmentReminder.java`
