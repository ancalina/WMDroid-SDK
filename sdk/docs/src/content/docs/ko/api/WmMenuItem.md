---
title: "WmMenuItem"
slug: ko/api/wmmenuitem
---
[API](/ko/api/)

명령, 체크, 구분선, 하위 메뉴의 불변 데이터입니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmMenuItem.java`

## 선언

```text
public final class WmMenuItem
public final int id;
public final String label;
public final boolean enabled;
public final boolean checked;
public final boolean separator;
public static WmMenuItem command(int id,String label);
public static WmMenuItem checked(int id,String label,boolean checked);
public static WmMenuItem submenu(String label,WmMenuItem... children);
public static WmMenuItem separator();
public WmMenuItem withEnabled(boolean enabled);
public WmMenuItem[] children();
public boolean hasChildren();
```

command, checked의 id는 선택 callback이 받는 caller 지정 값입니다. label은 필수입니다. submenu는 null 아닌 child 1개 이상이 필요합니다. 잘못된 값은 `IllegalArgumentException`입니다.

children은 생성 시 복제하고 children()도 복제 배열을 반환합니다. `withEnabled()`는 새 항목을 반환하며 원본은 유지합니다. separator는 disabled, 빈 label, id=0입니다. submenu의 id도 0입니다. 실제 표시, 선택은 [WmCommandMenu](/ko/api/wmcommandmenu/)가 담당합니다.
