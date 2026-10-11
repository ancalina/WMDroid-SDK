---
title: "WmInputDialog"
slug: ko/api/wminputdialog
---
[API](/ko/api/)

단일 이름 입력과 완료·취소 명령을 가진 전체 client 대화상자입니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmInputDialog.java`

## WmInputDialog

```text
public final class WmInputDialog extends FrameLayout implements WmControls.BackHandler,WmControls.ModalOwner,WmControls.RestyleListener,WmControls.LanguageListener
public WmInputDialog(Context context,Typeface font,Typeface bold,String labelText,int accent,int maxLength,Submit submit);
public static WmInputDialog find(View root);
public EditText editor();
public boolean isClosed();
public void onDismiss(Runnable action);
public void accept();
public void setBusy(boolean value);
public void dismiss();
public boolean back();
public boolean blocksOwnerCommands();
public View inputMethodOwner();
protected void onAttachedToWindow();
protected void onDetachedFromWindow();
public void onWmLanguageChanged(String language);
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```



font·bold·labelText·submit은 필수이며 maxLength는 0보다 커야 합니다. 잘못된 인자는 `IllegalArgumentException`입니다. editor는 한 줄 EditText이며 maxLength는 Android LengthFilter의 UTF-16 코드 단위 제한입니다. 예: `A`는1단위, `😀`는2단위입니다.

`accept()`는 String.trim()으로 양끝의 U+0020 이하 문자를 제거한 문자열을 submit에 전달합니다. 빈 값은 포커스를 되돌립니다. busy·closed에서는 아무 작업도 하지 않습니다. submit callback은 main thread에서 실행되며 자동으로 닫지 않습니다. 완료 후 dismiss 또는 오류 표시를 caller가 결정하세요.

`setBusy(true)`는 입력·명령을 비활성화합니다. busy이면 dismiss·Back도 닫지 않습니다. dismiss는 keyboard를 숨기고 parent에서 제거하며 onDismiss를 한 번 실행합니다. 외부 detach도 busy와 관계없이 closed로 바꾸고 submit을 해제하며 등록된 onDismiss를 한 번 호출합니다. dismiss에서 처리한 onDismiss는 중복 호출하지 않습니다. 닫힌 dialog는 재사용하지 마세요.

`find()`는 visible subtree에서 마지막 child부터 찾아 최상단 열린 dialog를 반환합니다. 없으면 null입니다. owner 명령은 차단하고 입력기 대상은 내부 panel입니다. 언어·restyle hook은 label·editor·명령을 갱신합니다.

## WmInputDialog.Submit

```text
public interface Submit
void submit(String value);
```
