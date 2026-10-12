---
title: "WmBoxSelect"
slug: ko/api/wmboxselect
---
[API](/ko/api/)

텍스트 셀의 단일, 다중 선택을 bit mask로 관리합니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmBoxSelect.java`

## 선언

```text
public final class WmBoxSelect extends LinearLayout implements WmControls.RestyleListener
public WmBoxSelect(Context context,Typeface font,String[] labels,int mask,boolean multi,int accent);
public String[] choices();
public void setProportionalWidths(boolean enabled);
public int selectedMask();
public TextView cell(int index);
public void setChoices(String[] values,int selected);
public void setSelectedMask(int selected);
public void choose(int index);
public void setOnSelectionListener(OnSelectionListener listener);
public void setEnabled(boolean enabled);
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```



labels는 null 아닌 문자열 1–30개입니다. 생성, setChoices에서 복제합니다. choices는 복제 배열을 반환합니다. font는 필수입니다. 잘못된 값은 `IllegalArgumentException`입니다.

mask의 bit i는 셀 i입니다. 음수, 존재하지 않는 bit, 단일 모드의 여러 bit는 허용하지 않습니다. `choose(index)`는 먼저 인덱스를 검사하고 컨테이너 자체가 비활성이면 값을 바꾸지 않습니다. 활성 상태에서는 비트를 toggle합니다. 개별 cell의 enabled 값은 검사하지 않습니다. 단일 모드에서도 현재 선택을 해제할 수 있습니다. 실제 변경 후 listener를 동기로 호출합니다. setter는 listener를 호출하지 않습니다. 잘못된 cell index는 `IndexOutOfBoundsException`입니다.

기본 셀은 높이 48 논리 픽셀, 같은 너비입니다. proportionalWidths는 측정된 텍스트 너비에 남은 공간을 동일 분배합니다. cell은 내부 TextView 참조입니다. 직접 child 구조를 변경하지 마세요. accent 인자는 현재 그림에 사용하지 않습니다.

## WmBoxSelect.OnSelectionListener

```text
public interface OnSelectionListener
void selected(int mask);
```
