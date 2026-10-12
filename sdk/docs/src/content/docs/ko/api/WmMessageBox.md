---
title: "WmMessageBox"
slug: ko/api/wmmessagebox
---
[API](/ko/api/)

완료 알림 또는 예, 아니요 선택을 받는 modal 대화상자.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmMessageBox.java`

```text
public final class WmMessageBox extends FrameLayout implements WmControls.BackHandler,WmControls.RestyleListener,WmControls.LanguageListener,WmControls.ModalOwner
```

## Icon

```text
public enum Icon { WARNING, QUESTION }
```

## 생성자, confirm

```text
public WmMessageBox(Context context,Typeface font,Locale locale,String caption,String message,int accent,Runnable acknowledged);
public static WmMessageBox confirm(Context context,Typeface font,Locale locale,String caption,String message,int accent,boolean defaultYes,WmRadioMenu.Selection answer);
public static WmMessageBox confirm(Context context,Typeface font,Locale locale,String caption,String message,int accent,boolean defaultYes,Icon icon,WmRadioMenu.Selection answer);
```

UI thread에서 생성, 변경하세요. font, locale, caption, message는 null이면 `IllegalArgumentException`입니다. 일반 생성자는 OK 버튼을 표시합니다. acknowledged는 null도 가능합니다.

`confirm()`은 예, 아니요 버튼을 표시합니다. 기본 icon은 WARNING입니다. icon, answer는 null이면 `IllegalArgumentException`입니다. `defaultYes`가 초기 focus를 결정합니다. answer는 예에 0, 아니요에 1을 받습니다. 선택, OK callback은 한 번만 호출됩니다. callback에서 View를 제거하세요. 이 클래스는 자신을 parent에서 제거하지 않습니다. detach는 callback을 호출하지 않고 해제합니다.

## captionField, messageField, okButton, yesButton, noButton

```text
public TextView captionField();
public TextView messageField();
public ImageView okButton();
public TextView yesButton();
public TextView noButton();
```

내부 표시 View를 반환합니다. 일반 메시지의 yesButton, noButton은 null입니다. 텍스트 변경에는 아래 binding API를 사용하면 언어 변경도 유지됩니다.

## setCaptionContext, setMessageFormat, setLocale

```text
public void setCaptionContext(String key);
public void setMessageFormat(String pattern,Object... values);
public void setLocale(Locale locale);
```

caption key는 번역 catalog의 문맥 key입니다. null은 작성 문자열로 돌아갑니다. message format은 [WmControls.bindFormat](/ko/api/wmcontrols/#bindformat)의 규격을 따릅니다. 값 배열은 복사됩니다. `setLocale(null)`은 `IllegalArgumentException`입니다. 언어를 바꿔도 format 값은 유지됩니다.

## back, blocksOwnerCommands, focusSearch

```text
public boolean back();
public boolean blocksOwnerCommands();
public View focusSearch(View focused,int direction);
```

`back()`은 일반 메시지를 확인하거나 confirm에서 아니요를 선택하고 true를 반환합니다. `blocksOwnerCommands()`는 true입니다. focus는 OK 또는 예, 아니요 버튼 안에 머뭅니다. 입력기는 아래 editor에 연결되지 않습니다.

## lifecycle, onMeasure, onWmLanguageChanged, onWmRestyle

```text
protected void onMeasure(int widthSpec,int heightSpec);
protected void onAttachedToWindow();
protected void onDetachedFromWindow();
public void onWmLanguageChanged(String language);
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

attach 시 기본 버튼에 focus를 줍니다. 긴 메시지는 사용 가능 높이에 맞춰 scroll합니다. 언어, 테마 callback은 제목, 본문, 버튼, 아이콘을 갱신합니다. title은 bold, 본문은 regular입니다.
