---
title: "WmCommandBar"
slug: ko/api/wmcommandbar
---
[API](/ko/api/)

```java
public final class WmCommandBar extends LinearLayout implements WmControls.RestyleListener
```

좌우 명령 키와 중앙 입력기 영역을 가진 하단바.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmCommandBar.java`

## 배치와 centre

```java
public static final int KEY_WIDTH = 180;
public static final int CENTRE_WIDTH = 120;
public final LinearLayout centre;
```

좌우 키는 각각 `180×52` SDK 논리 픽셀이다. `centre`는 두 키 사이의 남은 너비를 차지하며 자식을 중앙 정렬한다. 논리 너비 `480`에서 중앙 너비는 `CENTRE_WIDTH == 120`이다. 바 너비가 바뀌면 중앙 너비만 달라진다.

`centre`는 내부 컨테이너 참조다. 커스텀 중앙 UI와 `inputMethods`의 입력기 버튼을 동시에 넣지 않는다.

## 생성자

```java
public WmCommandBar(Context context,Typeface font,int accent);
public WmCommandBar(Context context,Typeface font,WmBar paint);
```

UI 스레드에서 생성, 변경하고 콜백도 UI 스레드에서 처리한다. 초기 좌우 키는 빈 문자열이며 비활성 상태다.

`accent` 생성자는 `new WmBar(context,true,accent)`를 사용한다. `paint` 생성자는 전달한 배경을 그대로 사용한다. `paint`는 null이 아닌 softkey 모드의 [WmBar](/ko/api/wmbar/)여야 한다. title 모드 배경은 키 스타일 적용 중 `IllegalStateException`을 발생시킨다.

## key

```java
public TextView key(boolean right);
```

`right == true`면 오른쪽, false면 왼쪽 내부 TextView를 반환한다. 키의 클릭, 터치 리스너를 교체하면 아래 명령 실행 규칙이 적용되지 않으므로 명령은 `setCommand` 또는 `setMenu`로 연결한다.

## inputMethods

```java
public void inputMethods(View owner);
```

`owner == null`이면 기존 입력기 버튼만 제거하고 커스텀 중앙 자식은 유지한다. non-null owner는 먼저 현재 중앙 자식을 검사한다. 기존 입력기 버튼 외의 자식이 있으면 `IllegalStateException`을 발생시키며 기존 버튼, 입력 소유자, observer를 유지한다. 검사를 통과하면 기존 버튼을 제거하고 새 [WmSipButton](/ko/api/wmsipbutton/)을 연결한다.

`owner`는 입력할 편집기 또는 편집기를 포함한 UI 루트다. 버튼의 편집기 선택, 표시 조건은 `WmSipButton`을 따른다. 커스텀 중앙 자식은 소유자가 직접 제거해야 한다.

## setCommands, setCommand

```java
public void setCommands(String left,Runnable leftAction,String right,Runnable rightAction);
public void setCommand(boolean right,String label,Runnable action);
```

`setCommands`는 왼쪽, 오른쪽 순서로 두 일반 명령을 연결한다. `setCommand`는 `right`로 한쪽을 선택한다. `label == null`은 빈 문자열, `action == null`은 키 비활성화다. 라벨을 번역하지 않고 그대로 표시한다. 연결을 바꾸면 기존 클릭, 터치 리스너를 교체하고 pressed, selected 상태를 지운다.

단일 포인터 DOWN으로 시작하여 키 안에서 UP하면 `action`을 한 번 실행한다. MOVE로 키 밖에 나가면 취소하며 다시 들어와도 재활성화하지 않는다. 다중 포인터, CANCEL, 비활성 키는 실행하지 않고 눌림을 해제한다.

실행은 `key.performClick()`을 거치며 키 활성 상태와 [WmControls.ownerCommandsAllowed](/ko/api/wmcontrols/)를 확인한다. 콜백을 지연하거나 작업 큐로 넘기지 않는다.

## setMenu

```java
public void setMenu(boolean right,String label,Runnable action);
```

선택한 키를 DOWN 시 실행하는 메뉴 명령으로 연결한다. 인수, 라벨, 비활성화, 명령 소유자 검사 규칙은 `setCommand`와 같다. UP에서는 다시 실행하지 않는다. DOWN 이후 영역 이탈, CANCEL은 눌림 표시를 지우지만 이미 실행한 콜백을 되돌리지 않는다.

메뉴 생성, 표시, 닫기는 `action`에서 소유자가 처리한다.

## restyle, onWmRestyle

```java
public void restyle(Typeface font,WmBar paint);
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

`restyle`은 바 배경, 양쪽 키의 글꼴, 글꼴 바인딩, 스타일, 연결된 입력기 버튼 배경을 갱신한다. 명령 라벨, 콜백, 활성 상태와 커스텀 중앙 자식은 유지한다. `paint` 조건은 생성자와 같다.

`onWmRestyle`은 `regular`와 `new WmBar(getContext(),true,selection)`으로 갱신한다. `bold`와 `tabColor`는 사용하지 않는다.

## styleKey

```java
public void styleKey(TextView key);
public static void styleKey(TextView key,WmBar paint);
```

인스턴스 메서드는 현재 바 배경, static 메서드는 전달한 softkey 배경으로 지정 TextView의 표시만 꾸민다. `key`와 `paint`는 null 불가이며 title 모드 배경은 `IllegalStateException`을 발생시킨다.

중앙 정렬, 좌우 여백 `8`, 위아래 여백 `0`, 터치 효과음 비활성화, 배경의 softButton을 적용한다. 텍스트 색은 비활성 회색 `0xff808080`, pressed, selected `paint.color(15)`, 기본 `paint.color(14)` 순서다. 글꼴, 크기, 배치, 라벨, 리스너, 활성 상태는 바꾸지 않는다. 커스텀 키의 동작 연결은 소유자 몫이다.

## shouldDelayChildPressedState

```java
public boolean shouldDelayChildPressedState();
```

항상 false를 반환하여 자식 눌림 표시를 부모가 지연하지 않는다.

## onDetachedFromWindow

```java
protected void onDetachedFromWindow();
```

등록된 터치의 armed 상태와 양쪽 키의 pressed, selected 상태를 지우고 superclass를 호출한다. 명령 연결과 중앙 자식은 유지한다.
