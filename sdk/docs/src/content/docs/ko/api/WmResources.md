---
title: "WmResources"
slug: ko/api/wmresources
---
[API](/ko/api/)

앱의 선택 리소스팩, 런처 snapshot, asset fallback을 해석합니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmResources.java`

```text
public final class WmResources
```

## prepare(Context), revision(Context)

```text
public static String revision(Context context);
public static WmResourcePack prepare(Context context)throws IOException;
```

prepare는 선택 snapshot 또는 null을 반환하고 앱별로 캐시합니다. 첫 호출은 disk I/O이며 worker에서 호출하세요. revision은 disk 없이 현재 준비된 app, generation 식별 문자열을 반환합니다. 직접 store를 변경하면 캐시를 갱신하지 못합니다. install, reset을 사용하세요.

## syncFromShell(Context)

```text
public static synchronized boolean syncFromShell(Context context)throws IOException;
```

worker에서 런처 팩을 로컬 불변 mirror로 동기화한다. 선택 snapshot이 바뀌면 true. host 조회가 불가능하면 현재 선택을 변경하지 않고 false를 반환한다. host의 빈 generation은 mirror 선택을 null로 갱신하고 bundled asset fallback을 사용한다. 남은 local pack으로 자동 복귀하지 않는다. 잘못된 generation, 전송, 설치 오류는 `IOException`.

## install(Context, InputStream), reset(Context)

```text
public static synchronized WmResourcePack install(Context context,InputStream input)throws IOException;
public static synchronized void reset(Context context)throws IOException;
```

install은 검증된 local snapshot을 반환하고 reader cache, 아이콘 준비를 갱신합니다. input은 caller가 닫습니다. reset은 local pack을 해제하고 선택을 다시 계산합니다. mirror가 있으면 계속 사용할 수 있습니다. 둘 다 worker에서 호출하며 오류는 `IOException`입니다.

외부 앱의 최초 prepare와 reset 후 선택은 저장된 mirror source 기록을 우선한다. install은 검증된 local pack을 현재 프로세스 선택으로 즉시 적용한다. 이후 syncFromShell이 성공하면 host mirror가 다시 선택될 수 있으므로 local install이 다음 동기화 뒤에도 유지된다고 가정하지 않는다. reset은 local 선택을 해제하고 이 우선순위로 다시 계산한다.

## open(Context, String), openAsset(Context, String)

```text
public static InputStream open(Context context,String id)throws IOException;
public static InputStream openAsset(Context context,String path)throws IOException;
```

open은 pack id만 조회합니다. pack이 없거나 id가 없으면 `FileNotFoundException`입니다. openAsset은 `assets/<path>` pack alias를 우선 조회한 뒤 Android asset을 엽니다. caller가 stream을 닫습니다.

## vector(Context, String)

```text
public static android.graphics.drawable.Drawable vector(Context context,String id)throws IOException;
```

v1 JSON vector를 generation별로 한 번 파싱하고 매번 새 Drawable을 반환합니다. 없는 값도 캐시합니다. id 형식 오류는 `IllegalArgumentException`, 없음은 `FileNotFoundException`, 파싱 실패는 `IOException`입니다. 첫 파싱은 worker에서 준비하세요.

## assetFile(Context, String, int)

```text
public static File assetFile(Context context,String path,int limit)throws IOException;
```

`WmResources.assetFile`은 immutable pack 파일 또는 asset의 cache 복사본을 반환합니다. limit은 최대 파일 크기의 바이트 수이며 1..67,108,864(64 MiB)를 허용한다. 범위 밖은 `IllegalArgumentException`입니다. 크기 초과, 내용 충돌, I/O 오류는 `IOException`입니다. worker에서 호출하세요. font, sound player가 사용하는 반환 파일을 변경하지 마세요.

## bitmap(Context, String, int, int), iconBitmap(...)

```text
public static android.graphics.Bitmap bitmap(Context context,String path,int width,int height)throws IOException;
public static android.graphics.Bitmap iconBitmap(Context context,String path,int width,int height,String symbol);
```

width, height는 1–4096이며 곱은 1048576 이하입니다. 범위 밖은 `IllegalArgumentException`입니다. raster 최대 256 KiB, 정확한 지정 크기만 허용하며 실패는 `IOException`입니다. 결과 density는 DENSITY_NONE입니다. iconBitmap은 I/O 실패 때 같은 크기 emoji Bitmap을 반환합니다. 반환 Bitmap은 caller 소유입니다.

## emojiBitmap(String, int, int)

```text
public static android.graphics.Bitmap emojiBitmap(String symbol,int width,int height);
```

symbol은 공백 제외 비어 있지 않은 최대 32 UTF-16 단위 문자열입니다. 크기 제한은 bitmap과 같습니다. 잘못된 값은 `IllegalArgumentException`입니다. 시스템 font를 사용하고 지원되지 않는 glyph는 빈 사각형을 사용합니다.
