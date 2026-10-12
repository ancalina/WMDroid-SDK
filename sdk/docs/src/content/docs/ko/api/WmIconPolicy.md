---
title: "WmIconPolicy"
slug: ko/api/wmiconpolicy
---
[API](/ko/api/)

Activity별 adaptive icon 처리 허용, 숨김 목록을 해석하는 값 객체.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmIconPolicy.java`

## 선언

```text
public final class WmIconPolicy
public final boolean enabled;
public final String mode;
public final String json;
public WmIconPolicy(boolean enabled,String mode,String json);
public static WmIconPolicy read(SharedPreferences preferences);
public boolean allows(String component);
public static boolean hidden(SharedPreferences preferences,String component);
public static String canonical(String value);
```

mode는 `all`, `blacklist`, `whitelist`. 다른 값은 all로 처리합니다. json은 component 문자열 배열이며 null은 `[]`. 잘못된 JSON은 잡아서 목록을 비운 상태로 사용합니다.

`allows(component)`는 enabled=false, null, 빈 문자열이면 false. all은 나머지 모두 허용, whitelist는 목록 포함만, blacklist는 목록 미포함만 허용합니다. component는 canonical 형태로 비교합니다.

`read()`는 `android_icons_unframed`(false), `android_icons_mode`(all), `android_icons_list`([]) preference를 읽습니다. `hidden()`은 별도 `hidden_apps` 목록을 조회합니다. `canonical()`은 ComponentName을 정규화하고 파싱 불가한 문자열은 그대로, null은 빈 문자열로 반환합니다.
