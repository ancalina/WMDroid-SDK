---
title: "WmDialog"
slug: ko/api/wmdialog
---
[API 색인](/ko/api/)

```java
public class WmDialog extends LinearLayout
```

내장 대화상자의 기본 버튼 표시와 Enter, Escape 명령을 관리한다. UI 스레드에서 사용한다.

## 생성자

```java
public WmDialog(Context context);
```

빈 컨테이너를 만든다. 자식 배치, 내용, 명령 callback은 호출자가 구성한다.

## setDefaultButton, setCancelButton

```java
public final void setDefaultButton(TextView button);
public final void setCancelButton(TextView button);
```

버튼을 자식으로 추가한 뒤 [WmControls.button](/ko/api/wmcontrols/) 스타일을 적용하고 등록한다. 이 대화상자의 자손인 SDK push button만 허용하며, 중첩 WmDialog의 버튼은 제외한다. 위반은 `IllegalArgumentException`이다. null은 해당 등록을 해제한다.

`setDefaultButton`은 저장된 기본 버튼을 바꾸고 즉시 기본 표시를 갱신한다. null로 해제해도 포커스된 버튼은 기본으로 표시될 수 있다. `setCancelButton`은 Escape 명령을 바꾸며 기본 표시를 갱신하지 않는다.

## refreshDialogDefault

```java
protected final void refreshDialogDefault();
```

포커스가 소유된 SDK push button이고 버튼부터 대화상자까지 모든 View가 enabled, VISIBLE이면 그 버튼을 기본으로 표시한다. 그 외에는 저장된 기본 버튼을 사용한다. 저장된 버튼이 더 이상 이 대화상자 소유가 아니면 표시를 비운다. 이전 기본 표시를 제거한 뒤 새 표시를 적용한다. 기본 표시는 키 실행 가능 여부를 보장하지 않는다.

## dispatchKeyEvent

```java
public boolean dispatchKeyEvent(KeyEvent event);
```

기본 표시를 갱신하고 [WmControls.dialogButtonKey](/ko/api/wmcontrols/)로 처리한 뒤, 처리하지 않은 이벤트를 상위 구현에 전달한다. 처리 결과를 반환한다.

Enter, NUMPAD_ENTER는 포커스된 SDK push button을 대상으로 한다. 포커스가 대화상자 자체이거나 SDK plain text이면 저장된 기본 버튼을 대상으로 한다. 편집 필드, 다른 컨트롤, 포커스 없음은 저장된 기본 버튼으로 대체하지 않는다. Escape는 등록된 취소 버튼을 대상으로 한다.

자식 modal이 소유자 명령을 차단하거나 포커스가 중첩 대화상자에 있으면 명령을 실행하지 않는다. 소유된 push button 대상이 있으면 DOWN, UP을 소비한다. 취소되지 않은 DOWN에서만 클릭하며, 버튼부터 대화상자까지 모두 enabled, VISIBLE이어야 한다. 비활성, 숨김 대상의 키도 소비하지만 클릭하지 않는다.

## onAttachedToWindow, onDetachedFromWindow

```java
protected void onAttachedToWindow();
protected void onDetachedFromWindow();
```

attach 시 전역 포커스 변경 observer를 등록하고 기본 표시를 갱신한다. detach 시 observer와 현재 기본 표시를 제거한다. 저장된 기본, 취소 버튼 등록은 유지한다. override 시 상위 hook을 호출하여 이 수명 관리를 보존한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmDialog.java`
