---
title: "WmChoice"
slug: ko/api/wmchoice
---
[API](/ko/api/)

caller 소유 팝업과 연결하는 닫힌 단일 선택 필드입니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmChoice.java`

```text
public final class WmChoice extends TextView implements WmControls.RestyleListener
```

## OnChoiceListener

```text
public interface OnChoiceListener
void chosen(int index);
```

## OnOpenListener

```text
public interface OnOpenListener
void open(WmChoice source);
```

## 생성자 · setChoices · choices

```text
public WmChoice(Context context,Typeface font,String[] choices,int selected,int selection);
public void setChoices(String[] choices,int selected);
public String[] choices();
```

choices는 null 아닌 문자열 1개 이상입니다. selected는 유효 source index입니다. 잘못된 배열·문자열은 `IllegalArgumentException`, index는 `IndexOutOfBoundsException`입니다. 배열은 복제합니다. choices()도 복제 배열입니다. View 변경·callback은 main thread입니다.

키보드 선택을 활성화한 일반 팝업이 연결돼 있으면 `setChoices()`와 `setDisplaySort()`는 같은 팝업의 행을 교체하고 문자 검색을 초기화한다. 현재 표시 선택을 적용하며 선택 listener를 호출하지 않는다. 교체된 이전 행은 선택을 전달할 수 없다. 팝업·callback·호출자가 지정한 너비와 높이는 유지한다. 선택이 없으면 커서와 스크롤을 첫 행으로 초기화한다. 이 일괄 작업은 reset/add/select 동작을 적용한 것으로, 개별 native 콤보 메시지 API는 아니다.

## selectedIndex · setSelectedIndex · clearSelection · choose · chooseDisplayIndex · chooseCharacterDisplayIndex

```text
public void clearSelection();
public void setSelectedIndex(int index);
public void chooseCharacterDisplayIndex(int index);
public int selectedIndex();
public void chooseDisplayIndex(int index);
public void choose(int index);
public void setOnChoiceListener(OnChoiceListener listener);
```

selectedIndex는 source index이며 clearSelection 후 -1입니다. setSelectedIndex·clearSelection은 silent입니다. choose는 enabled이며 값이 바뀐 경우 listener를 호출합니다. chooseDisplayIndex는 표시 index를 source index로 변환합니다. chooseCharacterDisplayIndex는 같은 값이어도 listener를 호출합니다.

키보드 선택을 활성화한 열린 목록에서는 `setSelectedIndex()`가 표시 순서에 맞춰 선택·커서를 갱신하고 해당 행을 맨 위로 스크롤한다. 마지막 행에서는 스크롤 범위 끝으로 제한된다. `clearSelection()`은 목록 선택을 해제하며 커서·스크롤 위치·팝업을 유지한다. 사용자 선택은 보이는 영역으로만 스크롤한다.

## setDisplaySort · displaySortLocale · displayChoices · displaySelectedIndex

```text
public void setDisplaySort(Locale locale,int pinnedPrefixCount);
public Locale displaySortLocale();
public String[] displayChoices();
public int displaySelectedIndex();
```

locale는 필수, pinnedPrefixCount는 0–choices.length입니다. 잘못된 값은 `IllegalArgumentException`입니다. prefix를 유지하고 이후 표시 순서를 Collator SECONDARY로 정렬하며 [로 시작하는 label은 뒤에 둡니다. 원본 index·callback identity는 유지합니다. displayChoices는 새 표시 배열, displaySelectedIndex는 표시 index 또는 -1입니다. 새 choices에도 현재 sort 정책을 적용합니다.

## bindDropdown · isDroppedDown · setExtendedUi · isExtendedUi · searchCharacter

```text
public boolean onTouchEvent(MotionEvent event);
public boolean onKeyDown(int key,KeyEvent event);
public boolean onKeyUp(int key,KeyEvent event);
protected void onFocusChanged(boolean focused,int direction,android.graphics.Rect old);
public void setExtendedUi(boolean enabled);
protected void onDetachedFromWindow();
public boolean searchCharacter(char value);
public boolean isExtendedUi();
public void bindDropdown(View popup);
public boolean isDroppedDown();
public static void bindDropdown(View anchor,View popup);
public void setEnabled(boolean enabled);
public void setOnOpenListener(OnOpenListener listener);
```

setOnOpenListener는 click 때 팝업을 만들도록 callback을 등록합니다. bindDropdown은 표시 여부·키 전달을 연결하며 팝업의 생성·부착·닫기는 caller 책임입니다. static overload는 Choice·EditableChoice·DateField에 연결합니다. disable·detach는 연결을 해제합니다.

기본 closed Home/End는 첫·마지막 항목을 선택합니다. setExtendedUi(true)는 closed Down으로 팝업을 열고 Up·Home·End·Page·F4를 무시합니다. 일반 toggle key는 수정자 없는 F4와 Alt+Up/Down입니다. 닫혀 있으면 열기 callback을 호출하고 열려 있으면 연결 popup에 키를 전달합니다. extendedUi=true는 F4를 무시하고 닫힌 상태에서 수정자 없는 Down만 엽니다. Alt+Up/Down은 기존 toggle 경로를 유지합니다. searchCharacter는 UTF-16 한 단위 검색이며 disabled·열린 팝업·pressed이면 false입니다. 그 외에는 일치 후보가 없어도 true입니다. 일치가 있을 때만 선택을 적용하고 chosen callback을 동기로 호출하므로 반환값은 일치 여부가 아닙니다. 정렬 검색은 1초 동안 누적하고 비정렬 검색은 순환합니다. ACTION_MULTIPLE은 KEYCODE_UNKNOWN의 비어 있지 않은 문자열을 UTF-16 단위로 순서대로 검색합니다. Ctrl/Alt/Meta 또는 Backspace/Escape 외 제어 문자가 있으면 검색 전에 전체 문자열을 거부합니다. 도중에 대상이 입력을 거부하면 중단하며 앞선 callback 결과는 유지합니다. IME composition은 처리하지 않습니다.

## onWmRestyle · onMeasure · onDraw · drawableStateChanged

```text
protected void onMeasure(int widthSpec,int heightSpec);
protected void drawableStateChanged();
protected void onDraw(Canvas canvas);
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

font·selection 색·그림을 갱신합니다. 자동 높이는 공통 comboHeight·font row metric을 사용합니다.

![Android API 14 렌더](/WmChoice.png)
