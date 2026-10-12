---
title: "WmPropertySheet"
slug: ko/api/wmpropertysheet
---
[API](/ko/api/)

보존된 페이지와 하단 탭, 선택, Apply/Cancel gate를 관리한다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmPropertySheet.java`

```text
public final class WmPropertySheet extends LinearLayout implements WmControls.BackHandler,WmControls.RestyleListener
public static final int PSN_SETACTIVE = -200;
public static final int PSN_KILLACTIVE = -201;
public static final int PSN_APPLY = -202;
public static final int PSN_RESET = -203;
public static final int PSN_QUERYCANCEL = -209;
public static final int PSNRET_NOERROR = 0;
public static final int PSNRET_INVALID = 1;
public static final int PSNRET_INVALID_NOCHANGEPAGE = 2;
```

## OnPageChangedListener

```text
public interface OnPageChangedListener
void selected(int index);
```

## OnPageNotificationListener

```text
public interface OnPageNotificationListener
int notifyPage(int index,int notification);
default int notifyPage(int index,int notification,int parameter);
```

## 생성자, addPage

```text
public int addPage(CharSequence title,View page);
public int addPage(int pageId,CharSequence title,View page);
public WmPropertySheet(Context context,Typeface font,int tabColor);
```

UI 스레드에서 생성, 변경하고 콜백도 같은 스레드에서 동기 실행한다. 본문은 높이 `40` 논리 픽셀의 탭 위에서 남은 높이를 채운다. 페이지 스크롤, 명령, 데이터 저장은 소유자가 담당한다.

제목, View는 null 불가이며 View는 부모가 없어야 한다. 위반하면 `IllegalArgumentException`이다. 페이지는 본문을 채운다. 첫 페이지는 페이지 알림, 선택 콜백 없이 선택한다. 나머지는 GONE이지만 연결을 유지한다.

명시적 pageId는 `1–65535`이며 중복을 허용한다. 자동 ID는 `pageCount()+1`을 먼저 시도하고, 사용할 수 없으면 `1`부터 첫 미사용 ID를 찾는다. ID가 소진되면 `IllegalStateException`이다. 두 overload는 0부터 시작하는 페이지 index를 반환한다. notification 중 추가도 `IllegalStateException`이다.

## selectPage, trySelectPage, restoreSelectedPage

```text
public void selectPage(int index);
public void restoreSelectedPage(int index);
public boolean trySelectPage(int index);
```

`trySelectPage`는 페이지 notification 중이면 index 검사 전에 false를 반환한다. 그 외 범위 밖 index는 `IndexOutOfBoundsException`이다. 현재 페이지 선택은 알림 없이 true다.

현재 페이지 KILLACTIVE→후보 SETACTIVE→UI commit, 포커스→선택 콜백 순서다. KILLACTIVE의 nonzero는 작업을 거부한다. SETACTIVE는 아래 규칙으로 후보를 결정한다.

| 반환 | 결과 |
| --- | --- |
| `0` | 후보 허용. |
| `-1` | 다음 index 시도; 마지막 페이지에서는 현재 선택 index로 돌아감. |
| 일치 pageId | 해당 ID의 첫 페이지로 redirect. |
| 그 외 | 후보 허용. |

최대 `pageCount()`회 알림 후 후보가 확정되지 않으면 false다. 현재 페이지가 최종 허용되면 선택 콜백은 없다. 작업 시작 시 notification listener를 고정하며 교체는 다음 작업에 적용한다. notification 예외는 전파하고 `finally`에서 notification guard를 해제한다.

선택이 바뀌고 window에 연결되어 있을 때만 활성화 포커스를 처리한다. 이전에 탭에 포커스가 있었으면 새 탭에 요청한다. 그 외에는 페이지에 forward 포커스를 요청하고 실패하면 탭에 요청한다. 포커스를 받은 EditText는 앱 콜백 전에 전체 텍스트를 선택한다. 콜백에서 다시 선택할 수 있으므로 반환 후 index는 콜백에 전달한 commit index와 다를 수 있다.

`selectPage`는 boolean 결과를 버린다. `restoreSelectedPage`는 index 검사 후 알림, 콜백, 활성화 포커스 처리 없이 commit한다. notification 중 복원은 index 검사 전에 `IllegalStateException`이다.

## applyPages, cancelPages

```text
public boolean applyPages(boolean closing);
public boolean cancelPages();
public boolean cancelPages(boolean exit);
```

빈 sheet, notification 재진입은 false다. Apply는 선택 페이지에 KILLACTIVE, Cancel은 QUERYCANCEL을 먼저 보낸다. gate의 nonzero는 페이지 처리를 시작하지 않고 false를 반환한다.

허용되면 changed가 false인 페이지도 포함하여 index 순서로 APPLY 또는 RESET을 보낸다. parameter `0`은 Apply/Cancel, `1`은 OK/exit다. `cancelPages()`는 `exit=false`다. 3인자 listener 기본 구현은 2인자 메서드에 전달한다.

반환 `0`은 해당 페이지의 changed flag를 지운다. nonzero는 처리를 중단하고 false를 반환한다. `2`는 선택을 유지하며, 그 외 모든 nonzero는 실패 페이지에서 SETACTIVE 후보 결정을 시도한다. 먼저 성공한 페이지는 clean 상태를 유지한다. listener 예외는 전파하며 이전 효과, flag는 rollback하지 않는다.

모든 페이지 성공 후 `applyPages(false)`는 현재 페이지에서 SETACTIVE를 다시 결정한다. redirect로 선택이 바뀌거나 후보 결정 실패로 false가 될 수 있다. `applyPages(true)`와 Cancel은 이 마지막 재활성화를 하지 않는다. 선택 변경의 포커스, 콜백 규칙은 `trySelectPage`와 같다.

true는 작업 허용 결과다. Activity, 창 종료는 소유자가 처리하며 sheet가 직접 닫지 않는다.

## setPageChanged, isPageChanged, hasChanges, 페이지 조회

```text
public int pageCount();
public View getClientView();
public int selectedPage();
public void setPageChanged(int index,boolean changed);
public boolean isPageChanged(int index);
public boolean hasChanges();
public void setPageTitle(int index,CharSequence title);
```

`pageCount`는 연결된 페이지 수, `selectedPage`는 첫 추가 전 `-1`이다. `getClientView`는 탭을 제외한 내부 본문 FrameLayout을 반환한다. 제거하거나 다른 부모로 옮기지 않는다.

`setPageChanged`는 flag만 설정한다. `isPageChanged`는 해당 flag, `hasChanges`는 하나라도 true인지 반환한다. 편집 내용으로 flag를 자동 갱신하지 않는다. 이 그룹의 index 인수 메서드는 잘못된 index에 `IndexOutOfBoundsException`을 발생시킨다.

`setPageTitle`은 index를 검사한 뒤 null title이면 `IllegalArgumentException`이다. 전달 텍스트를 번역 없이 표시하고 탭 너비를 다시 계산한다.

## listener, notifyPage, back, onWmRestyle

```text
public boolean dispatchKeyEvent(KeyEvent event);
public void setPageNotificationListener(OnPageNotificationListener listener);
public void setOnPageChangedListener(OnPageChangedListener listener);
public boolean back();
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

두 listener setter는 null로 등록을 해제할 수 있다. OnPageChangedListener는 선택이 바뀌고 notification guard가 해제된 뒤에만 index를 받는다. 콜백에서 새 선택을 시작할 수 있다. OnPageNotificationListener는 위 선택, Apply, Cancel 판정을 반환한다.

`dispatchKeyEvent`는 Ctrl+Tab, Ctrl+Page Down으로 앞으로, Ctrl+Shift+Tab, Ctrl+Page Up으로 뒤로 이동하며 양 끝에서 순환한다. DOWN에서 `trySelectPage`를 호출하고 선택이 거부돼도 이벤트를 소비한다. 해당 UP도 소비한다. 비활성 sheet, 빈 sheet, 다른 키, 다른 action은 superclass에 전달한다. 추가 modifier가 있어도 이 Ctrl 경로를 사용한다.

`back`은 선택 페이지의 `WmControls.back`에 위임하고 빈 sheet는 false다. 다른 페이지로 이동하거나 Cancel을 호출하지 않는다.

`onWmRestyle`은 탭 strip, 탭 글꼴, 글꼴 바인딩, 너비, 색, 페이지 표시 상태를 갱신한다. 선택, changed flag는 유지한다. 페이지 본문은 restyle하지 않으므로 소유자가 별도로 적용한다.
