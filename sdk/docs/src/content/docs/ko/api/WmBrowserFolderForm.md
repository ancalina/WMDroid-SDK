---
title: "WmBrowserFolderForm"
slug: ko/api/wmbrowserfolderform
---
[API 색인](/ko/api/)

```java
public final class WmBrowserFolderForm extends LinearLayout implements WmControls.LanguageListener
```

즐겨찾기 폴더 이름 입력 폼. 폴더 생성·검증·저장은 소유 앱이 수행한다.

## 생성자

```java
public WmBrowserFolderForm(Context context,Typeface font)
public WmBrowserFolderForm(Context context,Typeface font,String draft)
```

첫 생성자는 빈 초안 사용. `font` 또는 `draft`가 null이면 `IllegalArgumentException`. 한 줄 입력이며 길이 제한은 `WmDocumentProperties.NAME_LIMIT`인 128 UTF-16 코드 단위.

## Name control

```java
public static final int NAME=12339,NAME_LABEL=12356;
public EditText nameField()
public void onWmLanguageChanged(String language)
```

`NAME`과 `NAME_LABEL`은 입력/레이블 View ID. `nameField()`는 현재 초안을 읽거나 변경할 실제 입력 컨트롤. 언어 변경은 이름 레이블만 갱신한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmBrowserFolderForm.java`
