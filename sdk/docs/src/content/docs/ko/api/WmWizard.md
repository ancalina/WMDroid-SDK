---
title: "WmWizard"
slug: ko/api/wmwizard
---
[API](/ko/api/)

보존된 단계 페이지와 취소, 뒤로, 다음, 마침 명령을 관리합니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmWizard.java`

```text
public final class WmWizard extends WmDialog implements WmControls.BackHandler,WmControls.RestyleListener
```

## NavigationGate

```text
public interface NavigationGate
boolean allow(int from,int to);
```

## OnPageChangedListener

```text
public interface OnPageChangedListener
void selected(int index);
```

## 생성자, addPage, selectPage, 페이지 조회

```text
public int addPage(View page);
public void selectPage(int index);
public WmWizard(Context context,Typeface font,Locale locale,int accent);
public int pageCount();
public int selectedPage();
```

font, locale는 필수입니다. page는 parent 없는 View이며 반환값은 index입니다. 잘못된 인자는 `IllegalArgumentException`, 잘못된 index는 `IndexOutOfBoundsException`입니다. 첫 page를 자동 선택합니다. selectPage는 gate를 우회합니다. 실제 페이지 변경 시 finishHere=false로 초기화하고 표시 갱신 뒤 selected callback을 동기로 호출합니다. 같은 index를 다시 선택하면 변경, callback이 없습니다.

## navigation, 완료 callback

```text
public void setFinishHere(boolean finishHere);
public void setNavigationGate(NavigationGate gate);
public void setOnPageChangedListener(OnPageChangedListener listener);
public void setOnCancel(Runnable listener);
public void setOnFinish(Runnable listener);
public TextView cancelButton();
public TextView errorField();
public void setError(CharSequence message);
public TextView backButton();
public TextView nextButton();
public TextView finishButton();
public boolean back();
```

Next, Back은 NavigationGate.allow(from,to)를 먼저 호출하며 false이면 이동하지 않습니다. setFinishHere(true)는 현재 단계에서 마지막 단계 전에도 Finish를 표시합니다. 실제 페이지 변경에서 해제하므로 다음 단계에도 필요하면 selected callback에서 다시 설정하세요. Cancel, Finish는 caller callback을 실행하며 dialog를 자동 닫지 않습니다. setError는 null, 빈 값에 숨김, 그 외 오류 문구를 표시합니다. button accessor는 내부 TextView 참조입니다. BackHandler는 현재 page를 먼저 호출한 뒤 이전 단계 이동을 시도합니다.

## saveInteractionState, validateInteractionState, restoreInteractionState

```text
public Bundle saveInteractionState();
public void validateInteractionState(Bundle state);
public void restoreInteractionState(Bundle state);
```

schema=1 Bundle은 페이지 수, 선택, finishHere, 오류, 직접 addPage에 전달한 페이지 자체가 WmScrollView일 때의 요청 스크롤 위치를 보관합니다. 다른 페이지는 -1이며 자손의 WmScrollView를 탐색하지 않습니다. 중첩 스크롤, 입력 draft, focus, popup은 호출자가 따로 저장합니다. 페이지 수, 각 index의 스크롤 소유 여부, 형식이 다르면 `IllegalArgumentException`입니다. restore는 먼저 전체 검증 후 변경하며 gate, callback을 호출하지 않습니다. notification 중 restore는 `IllegalStateException`입니다.

## setLocale, onWmRestyle, onMeasure

```text
public void setLocale(Locale locale);
protected void onMeasure(int widthSpec,int heightSpec);
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

setLocale는 명령 문구, 배치를 갱신하며 null은 `IllegalArgumentException`입니다. 현재 onWmRestyle hook은 아무 작업도 하지 않습니다. subtree restyle로 child font를 갱신하세요. 생성, 변경, callback은 main thread입니다.

언어 변경에는 setLocale(locale)를 호출하세요. 명령 문구와 언어별 버튼 배치를 갱신하며 onWmLanguageChanged hook은 제공하지 않습니다.
