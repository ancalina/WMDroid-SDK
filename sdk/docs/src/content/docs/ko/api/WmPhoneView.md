---
title: "WmPhoneView"
slug: ko/api/wmphoneview
---
[API 색인](/ko/api/)

```java
public final class WmPhoneView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

번호 입력·상태·음성 사서함·키패드 클라이언트. 실제 통화, 음성 사서함 설정, 연락처/기록 데이터는 소유 앱이 제공한다.

## 생성자·Listener

```java
public interface Listener {void dial(String number);void voicemail();void speedDial();void history();}
public final WmPhoneKeypad keypad;
public WmPhoneView(Context context,Typeface regular,Typeface bold,int selection,Listener listener)
```

두 글꼴·listener 필수; null은 `IllegalArgumentException`. keypad는 이 뷰가 소유한 컨트롤. 숫자 키는 내부 버퍼에 추가·지우기. Talk는 번호가 비어 있지 않을 때 dial에 정규화된 번호 전달. 빈 버퍼에서 활성 음성 사서함 행을 탭하면 voicemail. 단축 다이얼/통화 기록은 각 콜백 전달.

## Number

```java
public static String dialable(String value)
public String number()
public void setNumber(String value)
public void setCountryIso(String country)
```

dialable은 입력 최대 256 UTF-16 코드 단위, 결과 최대 80. ASCII `0`..`9`·`*#+,;` 유지; ASCII 공백·괄호·하이픈·점 제거. null/그 외 문자/길이 초과는 `IllegalArgumentException`. 빈 문자열 허용. number는 서식 없는 버퍼, setNumber는 검증 후 표시 갱신. setCountryIso는 2 ASCII 영문자 또는 빈 문자열, null/잘못된 형식 거부. 국가 코드는 대문자로 저장하며 UI 언어와 별개. API21 이상·국가 코드 있음은 국가별 Android 번호 서식, 나머지는 legacy 서식 사용.

## Status and keypad

```java
public void setStatus(String authored)
public void setVoicemailAvailable(boolean available)
public boolean isKeypadVisible()
public void setKeypadVisible(boolean visible)
```

setStatus는 non-null 상태 문자열; null은 `IllegalArgumentException`. SDK 번역 조회로 표시한다. 음성 사서함 true는 빈 번호에서 사서함 행 표시, false는 숨김. 실제 사서함 번호를 만들거나 조회하지 않는다. keypadVisible은 키패드를 VISIBLE/GONE으로 바꾸며 번호를 유지한다.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
```

글꼴과 선택 색을 갱신한다. null 글꼴은 `IllegalArgumentException`. 언어 변경은 SDK가 작성한 캡션에 적용하며 사용자 데이터는 그대로 유지한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmPhoneView.java`
