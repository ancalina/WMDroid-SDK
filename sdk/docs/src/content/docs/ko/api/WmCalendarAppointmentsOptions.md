---
title: "WmCalendarAppointmentsOptions"
slug: ko/api/wmcalendarappointmentsoptions
---
[API 색인](/ko/api/)

```java
public final class WmCalendarAppointmentsOptions extends FrameLayout implements WmControls.BackHandler,WmControls.RestyleListener,WmControls.LanguageListener
```

논리 너비480px에서 원본 대화상자 좌표를 사용한다. 표시 심볼은 오른쪽252px 열에42px 셀6개로 배치한다. 좁은 화면은 셀을 줄바꿈하고 계정 행을 아래로 이동한다. 줄바꿈, 큰 글꼴은 해당 행 높이를 늘린다. 세로 넘침은 [WmScrollView](/ko/api/wmscrollview/)가 처리한다.

새 약속의 기본 알림, 표시 심볼, 회의 요청 계정을 설정한다. 실제 알림 예약, 계정 조회, 메일 전송, 저장은 호출자가 수행한다.

닫힌 수량 필드의 Page Up/Down은 내부 목록 높이210px 프로필을 사용한다. 기본 글꼴 크기에서는 후보6개의 처음, 끝으로 이동한다. 큰 글꼴은 한 페이지의 후보 수를 줄인다. 높이는 원본7/14 DLU, 기본 시스템 글꼴 프로필 기준이다. 다른 생성 시점 글꼴과 실제 GDI 목록 사각형은 미측정이다. 열린 수량 목록도 같은 클라이언트 높이를 사용하며 현재 글꼴의 완전한 행 단위로 높이를 줄인다. 페이지 가장자리에서는 표시 높이가 추가로 줄어들 수 있다.

단위, 계정 목록의 숨은 클라이언트 높이 기준은 각각214px, 166px다. 완전한 행 단위로 줄이고 페이지 가장자리에서 높이를 제한한다. 수량 필드와 같은 원본 기본 글꼴 가정을 사용한다.

### 상수와 필드

```java
public static final int REMINDER_ENABLED = 24020;
public static final int REMINDER_QUANTITY = 24021;
public static final int REMINDER_UNIT = 24022;
public static final int DISPLAY_ICONS = 25010;
public static final int MAIL_ACCOUNT = 21165;
public static final int MINUTES = 0;
public static final int HOURS = 1;
public static final int DAYS = 2;
public static final int WEEKS = 3;
```

필드 ID로 소유한 자식 컨트롤을 찾는다. value, choice, checked-change, focus listener를 교체하지 않는다. 페이지가 이 listener로 상태 동기화, 빈 수량 복원을 처리한다. 조용한 복원은 자식 직접 변경 대신 setOptions, setAccounts를 사용한다.

### 생성자

```java
public WmCalendarAppointmentsOptions(Context context,Typeface font,Locale locale,Options options,int selection,Listener listener);
public WmCalendarAppointmentsOptions(Context context,Typeface font,Typeface bold,Locale locale,Options options,int selection,Listener listener);
```

두 글꼴 생성자는 서구권 제목에 전달받은 bold를 사용한다. 단일 글꼴 생성자는 두 역할에 font를 사용하며 Bold를 합성하지 않는다. bold가 null이면 IllegalArgumentException이다. 페이지 Locale이 한, 중, 일이면24px Regular를 사용한다. 나머지는 라벨, 체크박스에21px Regular, 제목에21px Bold를 사용한다. 콤보는24px이다. 이는192dpi 요청 글꼴 높이이며 원본 GDI 메트릭 보정까지 재현하지 않는다. 제목, 일반 라벨은 왼쪽 위에 정렬한다. setLocale은 역할을 즉시 바꾸고 restyleTree는 두 글꼴을 갱신한다.

font/locale/options는 null 불가(`IllegalArgumentException`). 전달한 Options가 모든 초기 알림, 표시 설정을 정하며 초기화는 통지하지 않는다. 계정이 없는 초기 상태는 계정 콤보 비활성이다. listener는 null 가능하며 페이지 수명 동안 참조를 유지한다. 교체 API는 없다. View 조작은 UI 스레드에서 수행한다.

### options

```java
public Options options();
```

마지막 유효 수량과 현재 enable/unit/mask의 새 Options. 숫자 입력을 비우면 이전 수량을 보존하며 그 편집 자체는 통지하지 않는다. 다른 필드 변경은 보존한 수량으로 통지한다. 초점을 잃으면 빈 필드에 그 수량을 통지 없이 다시 표시한다. 알림을 비활성화하면 수량, 단위 편집을 비활성화하고 팝업을 닫으며 값은 유지한다.

### setOptions

```java
public void setOptions(Options options);
```

팝업을 닫고 조용히 필드를 복원한다. 다음 중복 사용자 통지 기준도 이 값으로 설정한다. null은 `IllegalArgumentException`. 선택 계정, 계정 callback은 바꾸지 않는다.

### accountId

```java
public String accountId();
```

현재 선택 계정 id. 계정이 없으면 빈 문자열.

### setAccounts

```java
public void setAccounts(String[] ids,String[] labels,String selectedId,OnAccountListener listener);
```

동일 길이 ids/labels 배열을 복사한다. id는 unique/nonempty/non-null, label은 non-null. nonempty 목록은 selectedId가 정확히 하나와 일치해야 한다. 빈 배열은 selectedId="". 위반은 `IllegalArgumentException`이며 교체 전 검증한다. 계정 복원은 통지하지 않는다. 성공하면 팝업을 닫고 계정 callback을 교체하며 배열이 비어 있지 않을 때만 콤보를 활성화한다. null listener는 계정 통지를 해제한다. non-null listener는 다음 성공한 `setAccounts()` 호출까지 참조를 유지한다.

### setLocale

```java
public void setLocale(Locale locale);
```

설정 문구, 단위, 심볼 설명을 바꾸고 팝업을 닫는다. 사용자 계정 이름은 유지한다. null은 `IllegalArgumentException`.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

공유 catalog 언어에 해당하는 Locale을 setLocale에 전달한다. 상태 보존, popup 처리는 setLocale 계약을 따른다.

### closePopup

```java
public boolean closePopup();
```

팝업 callback 세대를 무효화하고 현재 overlay를 제거한다. 실제 팝업을 제거했으면 true, 없으면 false. 키보드 이동은 팝업이 열린 동안 선택값을 적용하며 닫아도 그 값을 복원하지 않는다.

### back

```java
public boolean back();
```

현재 팝업을 닫은 결과를 반환한다. 팝업이 없으면 false.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

현재 팝업을 닫고 다음 팝업의 글꼴, 선택색을 저장한다. 기존 자식의 스타일은 WmControls.restyleTree로 갱신한다.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

Android 분리 hook. 현재 popup overlay를 닫는다.

## Options

```java
public static final class Options
```

### 상수와 필드

```java
public final boolean enabled;
public final int quantity;
public final int unit;
public final int displayMask;
```

enabled는 새 약속 알림 기본값이다. quantity는 0–99, unit은 MINUTES, HOURS, DAYS, WEEKS다. displayMask bit 0–5는 각각 미리 알림, 되풀이, 메모, 장소, 참석자, 비공개 심볼을 제어한다.

### 생성자

```java
public Options(boolean enabled,int quantity,int unit,int displayMask);
```

수량0..99, 단위0..3, mask0..63 범위 밖이면 `IllegalArgumentException`.

### minutes

```java
public int minutes();
```

단위 배수1/60/1440/10080을 곱한 분. enabled와 무관하게 계산한다.

## Listener

```java
public interface Listener
```

### changed

```java
void changed(Options options);
```

유효 설정 변경 시 새 Options를 동기로 전달합니다. 일반 복원은 setOptions를 사용합니다.

| 동작 | options() | 통지 |
| --- | --- | --- |
| 활성 수량 유효 숫자 편집 | 새 수량 | 설정이 다르면 |
| 빈 수량 편집 | 이전 유효 수량 | 이 편집만으로는 없음 |
| 활성 수량/단위 choose | 선택한 값 | 설정이 다르면 |
| 알림 체크/심볼 변경 | 새 값 | 설정이 다르면 |
| setOptions | 전체 설정 | 없음 |
| 자식 수량 setValue | 보존 수량 유지, 표시 draft만 변경 | 없음 |
| 자식 단위 setSelectedIndex | 새 단위 | 없음 |

비활성 수량/단위 choice는 무시합니다. 빈 수량은 blur에서 이전 값으로 무통지 복원합니다. callback 전 중복 비교 기준을 갱신하므로 예외는 값과 기준을 되돌리지 않고 전파하며 같은 설정을 재시도해도 통지하지 않습니다. 소유자가 적용, 저장합니다.

## OnAccountListener

```java
public interface OnAccountListener
```

### chosen

```java
void chosen(String id);
```

기존 선택과 다른 계정을 선택하면 id를 동기 전달한다. 프로그램적 계정 choice `choose()`도 포함한다. Callback 전에 `accountId()`를 갱신하며 callback 예외는 이전 계정으로 되돌리지 않고 전파된다. 같은 id를 다시 선택하면 통지하지 않으며 계정 콤보가 disabled이면 choice를 무시한다. 조용한 계정 `setSelectedIndex()`는 자식 선택만 바꾸고 accountId()는 갱신하지 않는다. 계정 복원은 `setAccounts()`를 사용한다. 계정 이름은 호출자 제공 label이며 번역하지 않는다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmCalendarAppointmentsOptions.java`

제목은 RCML 역할15 색을 사용한다. 테마 색상은240단계로 변환하며 채도는 최대, 밝기는144/240이다. 회색 테마에는 원본 정수 명도 계산을 적용한다. 테마 변경 시 제목과 설정값을 유지한다.
