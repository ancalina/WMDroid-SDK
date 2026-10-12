---
title: "WmRadioMenu"
slug: ko/api/wmradiomenu
---
[API](/ko/api/)

명령, radio, 선택 목록의 행과 키보드 동작을 제공합니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmRadioMenu.java`

```text
public final class WmRadioMenu extends LinearLayout implements WmControls.RestyleListener
```

## Selection

```text
public interface Selection
void choose(int index);
```

## 생성자, menuWidth, menuHeight, row, setSubmenuSelected

```text
public WmRadioMenu(Context context,Typeface font,String[] labels,int selected,String selectedSuffix,int selection,Selection action);
public WmRadioMenu(Context context,Typeface font,WmMenuItem[] items,String selectedSuffix,int selection,Selection action);
public View row(int index);
public int menuWidth();
public int menuHeight();
public void setSubmenuSelected(int index);
```

action은 필수입니다. tree items는 비어 있으면 안 되며 각 항목은 필수입니다. flat labels는 빈 배열 허용, selected는 -1 또는 유효 index입니다. 잘못된 인자는 `IllegalArgumentException`입니다. 생성 callback은 id가 아닌 row index를 받습니다. WmCommandMenu가 이를 leaf id로 변환합니다.

menuWidth는 측정 label에 따른 최대 480 논리 픽셀, menuHeight는 잘리지 않은 전체 높이입니다. caller가 viewport 높이를 제한하세요. row는 label View 참조이며 separator, 잘못된 index에 null입니다. setSubmenuSelected는 child가 있는 활성 행만 강조하며 -1은 해제합니다. View, callback은 main thread입니다.

## enableChoiceKeyboard, choose, 키보드

```text
public void enableChoiceKeyboard(int selected,Selection live,Runnable close);
public boolean onKeyDown(int key,KeyEvent event);
public boolean onKeyUp(int key,KeyEvent event);
public boolean dispatchKeyEvent(KeyEvent event);
```

일반 선택 목록에만 사용합니다. 모든 항목은 enabled이며 separator, submenu가 없어야 합니다. icon row는 허용하지 않습니다. selected=-1 또는 유효 index, live, close 필수이며 오류는 `IllegalArgumentException`입니다. navigation은 즉시 live callback으로 commit하며 닫지 않습니다. Enter/Escape, toggle은 close를 실행합니다. Escape는 마지막 값을 되돌리지 않습니다. touch는 생성 callback을 사용합니다. popup과 callback 수명은 caller가 관리합니다.

## choiceDropdownHeight

```text
public int choiceDropdownHeight(int clientHeight);
```

clientHeight는 frame을 제외한 목록 높이 상한이며 논리 픽셀 단위이다. 음수는 `IllegalArgumentException`이다. 먼저 [enableChoiceKeyboard](#enablechoicekeyboard-choose-키보드)를 호출해야 한다. 미설정 또는 빈 목록은 `IllegalStateException`이다.

반환값은 `paddingTop+paddingBottom+min(itemCount,clientHeight/rowHeight)*rowHeight`이다. 정수 나눗셈으로 완전한 행까지만 포함한다. rowHeight는 첫 행의 layout 높이이며 choice keyboard 준비, restyle에서 현재 글꼴 metric으로 갱신한다. clientHeight가 0이면 위, 아래 padding만 반환한다.

## enableChoiceCharacterSearch, searchCharacter

```text
public void enableChoiceCharacterSearch(Locale sortedLocale,Selection live);
public boolean searchCharacter(char value);
```

choice keyboard 준비 후 사용하며 live는 필수입니다. 오류는 `IllegalArgumentException`입니다. null locale은 비정렬 순환, 그 외 1초 누적 검색입니다. 하나의 UTF-16 단위를 받습니다. 활성, attached, shown이며 touch capture가 없을 때만 처리하고 그 외 false입니다. 처리 가능한 상태에서는 일치 후보가 없어도 true입니다. 일치가 있을 때 등록한 문자 검색 callback을 동기로 호출합니다. callback index는 표시 순서입니다. source mapping은 caller 책임입니다.

ACTION_MULTIPLE은 KEYCODE_UNKNOWN의 비어 있지 않은 문자열을 UTF-16 단위로 검색합니다. Ctrl/Alt/Meta 또는 Backspace/Escape 외 제어 문자가 있으면 검색 전에 전체 문자열을 거부합니다. callback이 메뉴를 비활성화하거나 분리하면 중단하며 앞선 선택 결과는 유지합니다. IME composition은 처리하지 않습니다.

## setIconRows

```text
public void setIconRows(Bitmap[] icons,Typeface regular,Typeface bold,boolean useBold,float textSize,int selection);
```

명령 행을 높이 36, 아이콘 32×32, 텍스트 left=40 논리 픽셀로 바꿉니다. 배열 길이는 항목 수와 같으며 separator만 null이어야 합니다. recycled Bitmap, submenu, choice keyboard, null fonts, 양수 finite 아닌 textSize는 `IllegalArgumentException`입니다. 배열만 복제하고 Bitmap 소유는 caller가 유지합니다. 사용 중 recycle하지 마세요. useBold는 bold font 역할을 선택합니다.

## setEnabled, onWmRestyle, lifecycle

```text
protected void onLayout(boolean changed,int left,int top,int right,int bottom);
protected void onFocusChanged(boolean focused,int direction,Rect old);
protected void onDetachedFromWindow();
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
public void setEnabled(boolean enabled);
```

현재 행 font, 색, 측정 너비를 갱신합니다. choice 목록은 font 높이 변경 후 fractional row scroll 위치를 보존합니다. detach는 owner 연결, touch, 문자 검색 상태를 정리합니다.
