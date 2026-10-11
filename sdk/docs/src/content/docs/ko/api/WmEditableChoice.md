---
title: "WmEditableChoice"
slug: ko/api/wmeditablechoice
---
[API](/ko/api/)

직접 입력·후보 선택·caller 소유 팝업을 결합한 필드입니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmEditableChoice.java`

```text
public final class WmEditableChoice extends EditText implements WmControls.RestyleListener
```

## OnOpenListener

```text
public interface OnOpenListener
void open(WmEditableChoice source);
```

## OnValueChangedListener

```text
public interface OnValueChangedListener
void changed(String value);
```

## 생성자 · value · setValue · choose

```text
public String value();
public void setValue(String value);
public void choose(int index);
public WmEditableChoice(Context context,Typeface font,String[] candidates,String value,int selection);
public int dropdownSelectedIndex();
public void setOnValueChangedListener(OnValueChangedListener listener);
```

후보 배열·문자열·value는 null이면 `IllegalArgumentException`입니다. 후보 0개는 허용합니다. candidates()는 복제 배열입니다. setValue는 silent이며 직접 editing은 enabled 상태에서 실제 문자열 변경을 listener에 알립니다. choose는 유효 index가 필요하며 아니면 `IndexOutOfBoundsException`입니다. enabled일 때 후보 값을 적용하고 문자열 변경에만 listener를 호출합니다. 같은 문자열도 후보 index는 갱신합니다. 작업·callback은 main thread입니다.

## setCandidates · setCandidateSort · addCandidate · insertCandidate · removeCandidate · clearCandidates

```text
public String[] candidates();
public void setCandidates(String[] candidates);
public void setCandidateSort(java.util.Locale locale);
public int addCandidate(String label);
public int insertCandidate(int index,String label);
public int removeCandidate(int index);
public void clearCandidates();
```

setCandidates는 배열을 복제하고 후보 내용이 달라지면 후보 선택 index를 -1, 후보 탐색 위치를 0으로 초기화합니다. 입력 문자열과 텍스트 선택 범위는 변경하지 않습니다. 기존 nonempty popup의 captured 행은 자동 교체하지 않습니다. setCandidateSort는 이후 addCandidate에만 적용하며 null은 append입니다. 기존 배열을 정렬하지 않습니다. insertCandidate의 -1은 append이며 정렬을 우회합니다. 반환은 삽입 index입니다.

removeCandidate는 남은 개수를 반환합니다. 긴 목록의 선택 후보 삭제는 caption을 비우지만 마지막 후보 삭제는 literal text를 유지합니다. clearCandidates는 후보와 caption을 모두 비웁니다. 변경은 silent입니다. index 범위 오류는 `IndexOutOfBoundsException`입니다. captured·reordered·non-keyboard popup은 변경 전에 닫으세요. 일치하지 않는 연결 목록에서 변경하면 `IllegalStateException`입니다. 연결 callback은 현재 source index를 사용해야 합니다.

## open · bindDropdown · isDroppedDown

```text
public void bindDropdown(View popup);
public void open();
public boolean isDroppedDown();
public void setOnOpenListener(OnOpenListener listener);
```

open은 exact, 다음 prefix 후보로 text를 완성한 후 open listener를 호출합니다. 완성은 일반 edit 변경 경로로 listener에 알립니다. 포커스가 있으면 전체를 선택합니다. caller가 popup을 만들고 부착·닫습니다. bindDropdown은 팝업 키·수명을 연결합니다. dropdownSelectedIndex는 현재 draft의 첫 prefix match이며 없음은 -1입니다.

`WmRadioMenu`를 연결할 때는 `menu.enableChoiceKeyboard(selected, live, close)`를 먼저 호출한 뒤 `field.bindDropdown(popup)`을 호출한다. `WmEditableChoice.bindDropdown` 호출 시점에 준비된 메뉴만 입력 필드의 포커스를 유지하도록 설정한다. 팝업 생성·부착·제거와 close callback은 호출자가 관리한다. 닫을 때 `field.bindDropdown(null)`로 연결을 해제한다.

다음은 open listener 안의 연결 코드다. `field`는 부착된 입력 필드, `popupLayer`는 호출자 소유 `FrameLayout`, `font`·`accent`는 현재 스타일이다. 후보가 비어 있지 않고 팝업이 모두 들어갈 공간이 있다는 전제다. UI 스레드에서 호출하며 팝업이 열린 동안 후보를 변경하지 않는다. 팝업 위치와 바깥 터치·화면 해제 시 `close.run()` 호출은 소유자가 연결한다.

```java
String[] choices = field.candidates();
int selected = field.dropdownSelectedIndex();
FrameLayout popup = new FrameLayout(field.getContext());
Runnable close = () -> {
    field.bindDropdown(null);
    popupLayer.removeView(popup);
};
WmRadioMenu menu = new WmRadioMenu(field.getContext(), font,
        choices, selected, "", accent, index -> {
    field.choose(index);
    close.run();
});
menu.enableChoiceKeyboard(selected, field::choose, close);
popup.addView(menu, new FrameLayout.LayoutParams(
        menu.menuWidth(), menu.menuHeight()));
field.bindDropdown(popup);
popupLayer.addView(popup, new FrameLayout.LayoutParams(
        menu.menuWidth(), menu.menuHeight()));
```

## setExtendedUi · isExtendedUi · 키보드

```text
public void setExtendedUi(boolean enabled);
public boolean onKeyDown(int key,android.view.KeyEvent event);
public boolean onKeyUp(int key,android.view.KeyEvent event);
public boolean onTouchEvent(MotionEvent event);
public void setOnCandidateNavigationListener(WmRadioMenu.Selection listener);
public boolean isExtendedUi();
public void setClosedDropdownClientHeight(int height);
public int getClosedDropdownClientHeight();
```

setOnCandidateNavigationListener는 closed 후보 navigation의 commit을 owner에 위임합니다. 없으면 choose를 호출합니다. setExtendedUi(true)는 closed Up/Down으로 열고 Page·F4는 무시합니다. 기본 Up/Down은 후보 이동입니다. closed Page 이동에는 setClosedDropdownClientHeight가 필요합니다. 높이는 frame·caption 제외 논리 픽셀, -1은 editor에 위임, 0은 유효, -2 이하는 `IllegalArgumentException`입니다. 열린 Enter/Escape는 마지막 commit을 유지하며 닫습니다. getClosedDropdownClientHeight는 설정한 값을 반환합니다. 기본값은 -1입니다.

## saveInteractionState · validateInteractionState · restoreInteractionState

```text
public Bundle saveInteractionState();
public void restoreInteractionState(Bundle state);
public static void validateInteractionState(Bundle state,int candidateCount,int textLength);
```

schema=1은 후보 수·선택·caret·text selection을 보관합니다. 먼저 owner 후보와 literal draft를 복원하세요. validate는 형식·count·범위를 검사하며 실패는 `IllegalArgumentException`입니다. restore는 silent이고 팝업이 열렸으면 `IllegalStateException`입니다. popup·gesture·extendedUi 정책은 snapshot에 없습니다.

## setEnabled · onWmRestyle · lifecycle

```text
protected void onMeasure(int widthSpec,int heightSpec);
protected void onFocusChanged(boolean focused,int direction,android.graphics.Rect old);
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
protected void onDetachedFromWindow();
public void setEnabled(boolean enabled);
```

화살표 gesture·보관 key·dropdown 연결을 해제합니다. restyle은 값 listener를 호출하지 않고 font·frame을 갱신합니다.
