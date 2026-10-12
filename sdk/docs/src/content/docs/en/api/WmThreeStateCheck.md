---
title: "WmThreeStateCheck"
slug: en/api/wmthreestatecheck
---
[API](/en/api/)

CheckBox that cycles through unchecked, checked, and indeterminate states.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmThreeStateCheck.java`

## Declaration

```text
public final class WmThreeStateCheck extends CheckBox implements WmControls.LanguageListener
public static final int UNCHECKED = 0;
public static final int CHECKED = 1;
public static final int INDETERMINATE = 2;
public WmThreeStateCheck(Context context,Typeface font);
public int getCheckState();
public void setCheckState(int state);
public void setChecked(boolean checked);
public boolean isAutomatic();
public void setAutomatic(boolean enabled);
public void setOnCheckStateListener(OnCheckStateListener listener);
public void toggle();
public void onWmLanguageChanged(String language);
public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info);
public void onInitializeAccessibilityEvent(AccessibilityEvent event);
public Parcelable onSaveInstanceState();
public void onRestoreInstanceState(Parcelable value);
```

## State and changes

UNCHECKED=0, CHECKED=1, and INDETERMINATE=2. getCheckState returns this state. Inherited isChecked is true only for CHECKED.

### setCheckState, setChecked

setCheckState throws `IllegalArgumentException` for negative values and clamps values above 2 to 2. setChecked(true/false) selects 1/0. Setters do not call OnCheckStateListener. The inherited boolean listener follows CheckBox behavior.

### setAutomatic, toggle

automatic defaults to true. An enabled automatic toggle cycles 0→1→2→0 and calls the state listener after a change. Toggling inside the callback does not reenter the listener. disabled or automatic=false makes toggle do nothing.

### onSaveInstanceState, onRestoreInstanceState

SavedState saves and restores state and automatic. The indeterminate accessibility description uses the current language. Change state on the main thread.


## WmThreeStateCheck.OnCheckStateListener

```text
public interface OnCheckStateListener
void changed(int state);
```

## WmThreeStateCheck.SavedState

```text
public static final class SavedState extends BaseSavedState
public void writeToParcel(Parcel parcel,int flags);
public static final Parcelable.Creator<SavedState> CREATOR;
```


![Android API 14 render](/WmThreeStateCheck.png)
