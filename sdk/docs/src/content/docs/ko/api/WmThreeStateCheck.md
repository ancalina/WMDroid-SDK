---
title: "WmThreeStateCheck"
slug: ko/api/wmthreestatecheck
---
[API](/ko/api/)

미선택·선택·부분 선택 상태를 순환하는 CheckBox입니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmThreeStateCheck.java`

## 선언

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

## 상태와 변경

UNCHECKED=0, CHECKED=1, INDETERMINATE=2입니다. getCheckState는 이 상태를 반환합니다. inherited isChecked는 CHECKED에만 true입니다.

### setCheckState · setChecked

setCheckState는 음수에 `IllegalArgumentException`, 2 초과에 2를 적용합니다. setChecked(true/false)는 1/0입니다. setter는 OnCheckStateListener를 호출하지 않습니다. inherited boolean listener의 동작은 CheckBox를 따릅니다.

### setAutomatic · toggle

automatic 기본값은 true입니다. 활성 automatic toggle은 0→1→2→0이며 실제 변경 후 state listener를 호출합니다. callback 중 다시 toggle해도 listener를 재진입하지 않습니다. disabled·automatic=false이면 toggle은 아무 작업도 하지 않습니다.

### onSaveInstanceState · onRestoreInstanceState

상태·automatic은 SavedState로 저장·복원합니다. 부분 선택 설명은 현재 언어를 사용합니다. main thread에서 변경하세요.


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


![Android API 14 렌더](/WmThreeStateCheck.png)
