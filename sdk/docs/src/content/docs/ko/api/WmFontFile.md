---
title: "WmFontFile"
slug: ko/api/wmfontfile
---
[API](/ko/api/)

가져온 글꼴을 SHA-256 파일명으로 저장하고 런처의 글꼴 URI를 해석합니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmFontFile.java`

## 선언

```text
public final class WmFontFile
public static final int MAX_BYTES = 16 * 1024 * 1024;
public static boolean isSource(String source);
public static File file(Context context,String hash);
public static String source(File file);
public static boolean available(Context context,String source);
public static synchronized File importFont(Context context,InputStream input)throws IOException;
public static synchronized File resolve(Context context,String source)throws IOException;
```

`importFont()`는 caller의 InputStream을 닫지 않습니다. 최대 16 MiB의 TTF·OTF를 검사하고 private `files/wm-fonts/<sha256>.font`에 저장합니다. 같은 내용은 기존 파일을 재사용합니다. null stream·잘못된 글꼴·크기 초과·hash 충돌·저장 실패는 `IOException`입니다. 파일 I/O는 worker에서 수행하세요.

sha256은 가져온 내용의 SHA-256을 나타내는 소문자 hex 64자리입니다.

`isSource()`는 `content://me.ancal.wmdroid.shell/fonts/<64자리 소문자 hex>`만 허용합니다. `file()`은 이 hash의 저장 경로를 반환하며 파일을 생성하지 않습니다. `source()`는 동일 형식의 .font 파일명을 URI로 바꿉니다. 잘못된 hash·파일명은 `IllegalArgumentException`입니다.

`available()`은 URI와 파일 크기 12바이트–16 MiB를 확인합니다. 글꼴 내용을 다시 검사하지 않습니다.

`resolve()`는 로컬 파일이 있으면 그 File을 반환합니다. 없으면 런처 provider에서 내려받아 hash를 검사하고 저장합니다. 자체적으로 연 provider stream은 닫습니다. 잘못된 URI·내용·접근 실패는 `IOException`입니다. Typeface가 사용하는 파일은 변경·삭제하지 마세요.
