---
title: "WmSipButton"
slug: ko/api/wmsipbutton
---
[API](/ko/api/)

```java
public final class WmSipButton extends LinearLayout
```

소유자 안의 유효한 편집기에 키보드 표시, 입력 방법 선택을 연결한다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmSipButton.java`

## 생성자

```java
public WmSipButton(Context context,View owner,WmBar bar);
```

UI 스레드에서 생성, 변경한다. `owner`는 편집기 또는 편집기를 포함한 UI 루트이며 null이면 `IllegalArgumentException`이 발생한다. 소유자 참조는 생성 후 바꾸지 않는다. `bar`는 null이 아닌 softkey 모드의 [WmBar](/ko/api/wmbar/)여야 한다. title 모드는 배경 적용 중 `IllegalStateException`을 발생시킨다.

초기 상태는 GONE이다. 키보드 아이콘 영역은 `52×52`, 화살표 영역은 `20×52` SDK 논리 픽셀이다. 아이콘을 중앙에 배치하며 두 자식은 포커스를 받지 않고 Android 터치 효과음을 사용하지 않는다. 접근성 설명은 한국어로 바인딩하며 이후 [WmControls.localizeTree](/ko/api/wmcontrols/)로 바꿀 수 있다.

아이콘은 [WmIcons.load](/ko/api/wmicons/)로 `sip_keyboard`를 읽는다. 팩 벡터가 없거나 읽기에 실패하면 내장 `sip_keyboard` 그림으로 대체한다.

## 터치 동작

키보드 아이콘은 영역 안에서 완료한 UP에 실행한다. 먼저 `refresh()`하고 유효한 편집기가 없으면 종료한다. 키보드가 열려 있으면 해당 editor token으로 숨김을 요청한다. 닫혀 있으면 편집기의 `requestFocus()`가 성공한 경우만 `SHOW_IMPLICIT`으로 표시를 요청한다. IME 요청의 성공 여부는 반환하지 않으며 실제 상태는 후속 `refresh()`에서 확인한다.

화살표는 DOWN에서 `refresh()`한 뒤 유효한 편집기와 열린 키보드가 모두 있을 때 Android 입력 방법 선택 창을 요청한다. UP에서 다시 요청하지 않는다. 포인터 수, 영역 이탈, CANCEL의 눌림 취소는 [WmCommandBar](/ko/api/wmcommandbar/)의 키 터치 규칙을 따른다. DOWN에서 실행한 선택 창 요청은 이후 CANCEL로 되돌리지 않는다.

## refresh

```java
public void refresh();
```

[WmControls.inputMethodOwner](/ko/api/wmcontrols/)로 현재 입력 소유자를 구한다. 편집기는 아래 조건을 모두 만족해야 한다.

- `isShown()`과 `isEnabled()`가 true다.
- `onCheckIsTextEditor()`가 true이고 window token이 있다.
- 생성자 `owner` 안에 있고 현재 입력 소유자 안에도 있다.

조건에 맞는 현재 포커스가 있으면 선택한다. 없으면 기존 편집기가 여전히 유효한 동안 유지하고, 유효하지 않으면 입력 소유자 트리를 마지막 자식부터 재귀 탐색한다. 입력 소유자가 없거나 유효한 편집기가 없으면 전체 버튼을 GONE으로 한다.

키보드 표시 상태도 갱신한다. API30 이상은 root `WindowInsets.Type.ime()`을 사용하며 insets가 null이면 false다. 이전 버전은 `rootHeight - visibleFrame.bottom > 100*density`일 때 열린 것으로 판단한다. 이 휴리스틱은 실제 IME 표시와 다를 수 있다. 화살표는 열린 것으로 판단할 때만 VISIBLE이다.

## isInputMethodVisible

```java
public boolean isInputMethodVisible();
```

마지막 `refresh()`에서 판단한 키보드 표시 상태를 반환한다. 호출 자체는 조회를 갱신하지 않는다. 편집기의 유효 여부나 이 버튼의 VISIBLE 상태를 뜻하지 않는다. 생성 직후와 detach 직후는 false다.

## restyle

```java
public void restyle(WmBar bar);
```

두 자식의 눌림 배경을 전달한 softkey `bar`로 교체한다. `bar` 조건은 생성자와 같다. `WmResources.revision`이 바뀌면 아이콘도 다시 읽는다. revision이 같으면 아이콘을 유지한다. 편집기, 키보드 상태, 소유자, 접근성 언어는 갱신하지 않는다.

## onAttachedToWindow

```java
protected void onAttachedToWindow();
```

superclass 호출 후 global-layout 리스너를 연결하고 `refresh()`를 실행한다. 연결된 동안 global layout마다 편집기와 키보드 상태를 갱신한다.

## onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

살아 있는 ViewTreeObserver에서 리스너를 제거한다. observer, 편집기 참조를 지우고 키보드 상태를 false로 하며 두 자식의 눌림, 터치 armed 상태를 취소한 뒤 superclass를 호출한다. 키보드를 숨기지는 않는다. 재연결하면 다시 observer를 등록한다.
