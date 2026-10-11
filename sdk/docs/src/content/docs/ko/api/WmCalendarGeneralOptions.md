---
title: "WmCalendarGeneralOptions"
slug: ko/api/wmcalendargeneraloptions
---
[API 색인](/ko/api/)

```java
public final class WmCalendarGeneralOptions extends FrameLayout implements WmControls.BackHandler,WmControls.RestyleListener,WmControls.LanguageListener
```

일정의 시작 보기·첫 요일·주 열 수·30분 행·주 번호 설정 페이지. 사용자 변경을 즉시 전달하며 저장/실제 Calendar 적용은 호출자가 수행한다. 생성자 Options가 초기값을 정하며 UI 스레드에서 사용합니다.

### 상수와 필드

```java
public static final int START_VIEW = 21163;
public static final int FIRST_DAY = 21161;
public static final int WEEK_DAYS = 21126;
public static final int HALF_HOUR = 21175;
public static final int WEEK_NUMBERS = 21162;
```

START_VIEW=21163, FIRST_DAY=21161, WEEK_DAYS=21126, HALF_HOUR=21175, WEEK_NUMBERS=21162는 필드 View id이다. `findViewById()`로 접근하면 실제 자식 control을 얻는다. 내부 open/choice/checked listener를 교체하면 팝업 처리·옵션 통지가 끊어진다. 조용한 model 변경에는 `setOptions()`를 사용한다.

### 생성자

```java
public WmCalendarGeneralOptions(Context context,Typeface font,Locale locale,Options options,int selection,Listener listener);
public WmCalendarGeneralOptions(Context context,Typeface font,Typeface bold,Locale locale,Options options,int selection,Listener listener);
```

두 글꼴 생성자는 서구권 제목에 전달받은 bold를 사용한다. 단일 글꼴 생성자는 두 역할에 font를 사용하며 Bold를 합성하지 않는다. bold가 null이면 IllegalArgumentException이다. 페이지 Locale이 한·중·일이면24px Regular를 사용한다. 나머지는 라벨·체크박스에21px Regular, 제목에21px Bold를 사용한다. 콤보는24px이다. 이는192dpi 요청 글꼴 높이이며 원본 GDI 메트릭 보정까지 재현하지 않는다. 제목·일반 라벨은 왼쪽 위에 정렬한다. setLocale은 역할을 즉시 바꾸고 restyleTree는 두 글꼴을 갱신한다.

font/locale/options는 null 불가(`IllegalArgumentException`). 전달한 Options가 모든 초기 설정을 정하며 별도 기본값은 적용하지 않는다. 초기 setter는 통지하지 않는다. listener는 null 가능하며 페이지 수명 동안 참조를 유지한다. Callback 교체 API는 없다. View 조작은 UI 스레드에서 수행한다.

### options

```java
public Options options();
```

현재 필드값의 새 불변 Options.

### setOptions

```java
public void setOptions(Options options);
```

팝업을 닫고 모든 필드를 조용히 복원한다. listener를 호출하지 않는다. null은 `IllegalArgumentException`.

### setLocale

```java
public void setLocale(Locale locale);
```

문구와 요일 이름을 바꾸고 팝업을 닫으며 선택값을 유지한다. 같은 Locale도 갱신한다. 문구는 [WmStrings.language](/ko/api/wmstrings/#language)가 선택한 가용 catalog를 사용하고, 요일 이름은 전달한 Locale의 DateFormatSymbols를 직접 사용한다. null은 `IllegalArgumentException`.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

공유 catalog 언어에 해당하는 Locale을 setLocale에 전달한다. 상태 보존·popup 처리는 setLocale 계약을 따른다.

### 선택 팝업

해당 choice 자식이 활성이고 페이지 폭·높이가 양수일 때만 연다. 필드 위·아래 가용 공간에 [WmScrollView](/ko/api/wmscrollview/)로 메뉴를 배치한다. 키보드 이동은 popup이 열린 상태에서 즉시 선택을 적용한다. Popup을 닫아도 이전 옵션으로 되돌리지 않는다. 완료된 터치 선택은 popup을 닫은 뒤 적용한다.

### closePopup

```java
public boolean closePopup();
```

팝업 callback 세대를 무효화하고 현재 overlay를 제거한다. 실제 팝업을 제거했으면 true, 없으면 false.

### back

```java
public boolean back();
```

현재 팝업을 닫은 결과를 반환한다. 팝업이 없으면 false.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

현재 팝업을 닫고 다음 팝업의 글꼴·선택색을 저장한다. 기존 자식의 스타일은 WmControls.restyleTree로 갱신한다.

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
public final int startView;
public final int firstDayOfWeek;
public final int weekDays;
public final boolean halfHour;
public final boolean weekNumbers;
```

startView: Agenda0/Day1/Week2/Month3/Year4. firstDayOfWeek: Sunday/Monday. weekDays:5/6/7. halfHour:30분 행, false는60분. weekNumbers:주 번호 표시.

### 생성자

```java
public Options(int startView,int firstDayOfWeek,int weekDays,boolean halfHour,boolean weekNumbers);
```

startView0..4, Sunday/Monday, weekDays5..7을 검증한다. 범위 밖이면 `IllegalArgumentException`.

## Listener

```java
public interface Listener
```

### changed

```java
void changed(Options options);
```

필드 변경 후 combo/check 통지에 새 Options를 동기 전달한다. 프로그램적 `choose()`·값을 바꾸는 CheckBox `setChecked()`도 통지한다. `setOptions()`는 페이지 callback을 억제한다. Choice는 선택 인덱스가 같아도 통지할 수 있다. Callback 예외는 변경된 필드를 되돌리지 않고 전파된다. 소유자가 옵션을 적용·저장한다.


## 크기와 테마

DLU는 원본 대화상자의 글꼴 기반 배치 단위입니다. RCML 역할은 원본 테마의 색상·글꼴 역할입니다.

논리 너비480px에서 원본 대화상자 좌표를 사용한다. 한국어 선택 필드는212px, 다른 언어는210px에서 시작한다. 넓은 화면은 필드 오른쪽을 확장한다. 좁은 화면은 열을 줄이며, 줄바꿈·큰 글꼴은 행 높이를 늘린다. 세로 넘침은 [WmScrollView](/ko/api/wmscrollview/)가 처리한다.

선택 목록은 숨은 목록 클라이언트222px 기준으로 완전한 행만 표시한다. 페이지 가장자리에서는 높이가 더 줄어든다. 원본7/14 DLU 기본 글꼴 생성 후 RCML 글꼴 적용 프로필이다. 실제 GDI 사각형과 다른 원본 생성 글꼴은 미측정이다.

제목은 RCML 역할15 색을 사용한다. 테마 색상은240단계로 변환하며 채도는 최대, 밝기는144/240이다. 회색 테마에는 원본 정수 명도 계산을 적용한다. 테마 변경 시 제목과 설정값을 유지한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmCalendarGeneralOptions.java`
