---
title: "WmCommandMenu"
slug: ko/api/wmcommandmenu
---
[API](/ko/api/)

하단 명령에 연결된 스크롤, 하위 메뉴 팝업입니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmCommandMenu.java`

## WmCommandMenu

```text
public final class WmCommandMenu extends FrameLayout implements WmControls.BackHandler,WmControls.RestyleListener
public final WmRadioMenu menu;
public WmCommandMenu(Context context,WmCommandBar bar,boolean right,Typeface font,String[] labels,int selected,String selectedSuffix,int selection,WmRadioMenu.Selection action,Runnable onDismiss);
public WmCommandMenu(Context context,WmCommandBar bar,boolean right,Typeface font,WmMenuItem[] entries,String selectedSuffix,int selection,WmRadioMenu.Selection action,Runnable onDismiss);
public WmRadioMenu activeMenu();
public int depth();
public boolean back();
public void dismiss();
public boolean dispatchTouchEvent(MotionEvent event);
protected void onMeasure(int widthSpec,int heightSpec);
protected void onLayout(boolean changed,int l,int t,int r,int b);
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
protected void onAttachedToWindow();
protected void onDetachedFromWindow();
```

bar, action은 필수이며 entries는 비어 있으면 안 됩니다. 잘못된 값은 `IllegalArgumentException`입니다. tree API의 callback은 활성 leaf의 WmMenuItem.id를 받습니다. flat API는 선택한 labels index를 받습니다. flat selected는 -1 또는 유효 index입니다.

menu는 root, activeMenu는 현재 child, depth는 root 포함 개수입니다. submenu 선택은 root를 유지하고 child를 추가합니다. `back()`은 child 한 개를 닫으면 true, root뿐이거나 닫혔으면 false입니다. false일 때 owner가 root를 닫으세요.

leaf 선택은 먼저 dismiss하고 action을 실행합니다. dismiss는 parent에서 제거하고 onDismiss를 한 번 호출합니다. detach도 정리, onDismiss를 수행합니다. 닫힌 인스턴스는 재사용하지 마세요. 생성, 변경, callback은 main thread입니다. 현재 restyle hook은 font, selection을 저장하고 재측정을 요청합니다. 기존 row 갱신은 [WmControls.restyleTree(commandMenu, regular, bold, selection, tabColor)](/ko/api/wmcontrols/#restyletree)를 호출하세요.
