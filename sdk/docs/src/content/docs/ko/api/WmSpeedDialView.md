---
title: "WmSpeedDialView"
slug: ko/api/wmspeeddialview
---
[API 색인](/ko/api/)

```java
public final class WmSpeedDialView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

단축 다이얼 슬롯과 연락처 이름 목록. 탭은 선택만 변경하며 전화·편집·저장은 소유 앱이 처리한다.

## Entry

```java
public static final class Entry
public final int slot;public final String name,number;
public Entry(int slot,String name,String number)
```

slot은 1..99, name은 non-null·`String.trim()` 후 비어 있지 않아야 한다. 위반 시 `IllegalArgumentException`. name은 원문 보존. null number는 빈 값으로 바꾸고 `WmPhoneView.dialable` 검증/정규화. 슬롯 1의 표시 이름은 언어에 따른 음성 사서함 캡션.

## 생성자·선택

```java
public WmSpeedDialView(Context context,Typeface regular,Typeface bold,int selection,Entry[] entries)
public Entry selected()
public void setEntries(Entry[] entries)
```

setEntries는 null 배열/항목·중복 슬롯을 변경 전에 거부(`IllegalArgumentException`). 배열 복사·입력 순서 유지. 기존 슬롯 선택 유지; 아니면 첫 항목, 빈 목록은 null. selected는 현재 선택 데이터.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
```

재스타일/언어 변경은 행을 다시 만들고 선택을 유지한다. regular는 슬롯 번호, bold는 이름에 사용하며 selection은 선택 행 색.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmSpeedDialView.java`
