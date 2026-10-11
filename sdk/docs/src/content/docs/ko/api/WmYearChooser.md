---
title: "WmYearChooser"
slug: ko/api/wmyearchooser
---
[API 색인](/ko/api/)

```java
public final class WmYearChooser extends WmDialog implements WmControls.RestyleListener,WmControls.BackHandler,WmControls.LanguageListener
```

1900..2999 연도를 입력하는 소유자 삽입형 dialog View. 완료/취소 후 실제 View 제거는 소유자가 수행한다.

### 상수와 필드

```java
public static final int MIN_YEAR = 1900;
public static final int MAX_YEAR = 2999;
```

MIN_YEAR=1900, MAX_YEAR=2999.

### 생성자

```java
public WmYearChooser(Context context,Typeface font,Locale locale,int year,int selection,Listener listener);
```

초기 연도 범위와 non-null Locale/listener를 요구한다(`IllegalArgumentException`). ASCII 숫자 최대4자리. 부착 후 입력 키보드를 요청한다.

### nearbyYears

```java
public static int[] nearbyYears(int current);
```

current-1부터current+3까지1900..2999로 제한한 새 배열. current 자체가 범위 밖이면 `IllegalArgumentException`.

### otherLabel

```java
public static String otherLabel(Locale locale);
public static String otherLabel(Context context,Locale locale);
```

Locale-only는 한국어 `기타...`, 나머지 `Other...`. Context overload는 언어 catalog를 적용한다. null Locale은 `IllegalArgumentException`.

### input

```java
public EditText input();
```

소유된 EditText. 초기 전체 선택과4자리 숫자 제한을 유지하여 사용한다.

### setLocale

```java
public void setLocale(Locale locale);
```

제목·라벨·오류·명령을 갱신한다. 입력 초안은 유지한다. null은 `IllegalArgumentException`.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

확인/취소/연도 문구를 갱신한다. 입력한 연도 원문은 유지한다.

### submit

```java
public boolean submit();
```

유효1900..2999이면 완료 flag 설정·키보드 숨김·chosen 호출 후 true. 잘못된 입력은 오류 표시/전체선택 후 false. 이미 완료했으면 false.

### cancel

```java
public void cancel();
```

미완료이면 완료 flag 설정·키보드 숨김·cancel callback. 이후 호출은 무시한다.

### back

```java
public boolean back();
```

미완료 dialog를 cancel하고 true. 이미 완료이면 false.

### onAttachedToWindow

```java
protected void onAttachedToWindow();
```

연도 입력의 소프트 키보드 표시 요청을 post한다.

### onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

pending 키보드 요청을 제거하고 현재 키보드를 숨긴다.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

파생 배경색만 갱신한다. 입력 원문·선택은 유지한다. 자식 글꼴은 WmControls.restyleTree로 갱신한다.

## Listener

```java
public interface Listener
```

### chosen

```java
void chosen(int year);
```

1900..2999의 유효 연도로 submit()이 성공할 때 해당 호출 안에서 동기로 실행합니다. 완료 상태 설정·키보드 숨김 뒤 호출하며 취소 시에는 호출하지 않습니다. 소유자가 다이얼로그 View를 제거합니다.

### cancel

```java
void cancel();
```

미완료 상태에서 cancel() 또는 back()으로 취소할 때 동기로 실행합니다. submit 성공 뒤에는 호출하지 않습니다. 인스턴스 하나에서 chosen 또는 cancel 중 하나만 최대 한 번 호출합니다. 실제 View 제거는 소유자가 담당합니다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmYearChooser.java`
