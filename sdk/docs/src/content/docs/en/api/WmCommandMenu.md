---
title: "WmCommandMenu"
slug: en/api/wmcommandmenu
---
[API](/en/api/)

Scrollable command popup with submenus anchored to a bottom command.

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

bar and action are required; entries must not be empty. Invalid values throw `IllegalArgumentException`. The tree API delivers the enabled leaf's WmMenuItem.id. The flat API delivers the selected labels index. Flat selected is -1 or a valid index.

menu is the root; activeMenu is the current child; depth includes the root. Selecting a submenu retains the root and adds a child. `back()` returns true after closing one child, or false when only the root remains or the popup is closed. The owner closes the root after false.

A leaf selection dismisses before running action. dismiss removes the popup and calls onDismiss once. Detachment also cleans up and calls onDismiss. Do not reuse a closed instance. Construction, changes, and callbacks use the main thread. The current restyle hook stores font and selection and requests measurement. Use [WmControls.restyleTree(commandMenu, regular, bold, selection, tabColor)](/en/api/wmcontrols/#restyletree) to update existing rows.
