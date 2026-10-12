---
title: "WmAssets"
slug: ko/api/wmassets
---
[API](/ko/api/)

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmAssets.java`

```java
public final class WmAssets
```

## catalog

```java
public static List<Entry> catalog(Context context)throws IOException;
```

`metadata/library.json`의 `assets` 배열과 `metadata/folders.json`의 `icons` 배열을 순서대로 읽어 읽기 전용 List를 반환합니다. 없는 카탈로그는 항목을 추가하지 않습니다. 폴더 항목의 type은 `"folder"`, library 항목은 `kind` 값입니다.

공식 추출기의 폴더 ID는 `yellow-upright-folder-variant-01`처럼 역할 기반 canonical ID입니다. library ID에는 아직 원본 형태의 식별자가 남아 있으므로 전체가 역할 기반 ID라고 가정하지 마세요. 항목 선택에는 실제 `Entry.id`를 사용합니다.

캐시는 application Context(없으면 전달 Context)와 활성 팩 객체별로 유지합니다. 팩 객체가 바뀌면 다시 읽습니다. 같은 팩 파일의 직접 편집은 캐시를 갱신하지 않으므로 편집한 ZIP을 다시 설치하세요. 반환된 기존 List는 이전 항목을 유지하지만 `open`, `loadDrawable`은 현재 팩의 ID를 조회합니다. 최초 카탈로그 읽기, 이미지 decode는 worker에서 수행하세요.

카탈로그 파일당 최대 4 MiB, 두 카탈로그 합계 최대 8192개 항목입니다. ID는 비어 있지 않은 최대 160 UTF-16 code units, type은 비어 있지 않은 최대 80 UTF-16 code units입니다. 카탈로그 width, height는 0–4096입니다. 중복 ID, 잘못된 경로, 규격 오류는 `IOException`이며 실패한 카탈로그는 캐시하지 않습니다.

## list

```java
public static List<Entry> list(Context context,String type)throws IOException;
```

`type=null`은 전체 카탈로그, 일치 항목이 없으면 빈 읽기 전용 List를 반환합니다. 비교는 대소문자를 구분합니다. 알려진 type은 icon, folder, bitmap, cursor, png, gif, jpeg, bmp, animation_avi, audio_wav이며 고정 enum은 아닙니다.

## byId

```java
public static Entry byId(Context context,String id)throws IOException;
```

현재 카탈로그의 ID가 일치하면 같은 캐시 Entry를 반환하며 미등록 ID는 `null`입니다. ID 조회는 카탈로그를 읽으므로 카탈로그 오류는 `IOException`으로 전파됩니다.

## Entry

```java
public static final class Entry
public final String id;
public final String type;
public final String label;
public final String labelKo;
public final String provider;
public final String resourceId;
public final String asset;
public final String originalAsset;
public final String sourceHash;
public final int width;
public final int height;
```

Entry 필드는 카탈로그 snapshot 값이며 외부 생성자는 없습니다. label이 비어 있으면 ID를 label로 사용합니다. labelKo, provider, resourceId, originalAsset, sourceHash는 없으면 빈 문자열입니다. width, height는 카탈로그 치수이며 0도 허용합니다.

provider, resourceId, sourceHash는 원본 provenance입니다. 카탈로그의 `resource_id` 또는 `group_resource_id`는 resourceId, `source_hash` 또는 `source_sha256`는 sourceHash가 됩니다. 사용자 팩을 읽을 때 이 hash와 원본 이미지 hash를 비교하지 않습니다. 이미지, 폰트, 사운드 편집 후에도 안전 검사 조건은 적용됩니다.

asset은 미리보기 또는 기본 파일의 논리 경로, originalAsset은 선택적 원본 파일의 논리 경로입니다. 둘 다 `library/` 또는 `folders/`로 시작해야 합니다. 빈 구간, `.`, `..`, backslash, colon, NUL은 거절합니다. [리소스팩 매핑](/ko/api/wmresourcepack/)에서 `assets/<path>` ID를 실제 ZIP path에 연결합니다.

## Entry.label

```java
public String label(Locale locale);
```

locale 언어가 `"ko"`이며 labelKo가 비어 있지 않으면 labelKo, 그 외에는 label을 반환합니다. `locale=null`도 label을 반환합니다.

## open

```java
public static InputStream open(Context context,String id)throws IOException;
```

originalAsset이 있으면 원본, 그 외에는 asset을 엽니다. GIF/JPEG/CUR, AVI/WAVE를 decode하지 않고 바이트 스트림으로 반환합니다. 미등록 ID는 `FileNotFoundException`이며 파일, I/O 오류는 전파됩니다. caller가 InputStream을 닫습니다.

## loadDrawable

```java
public static Drawable loadDrawable(Context context,String id,int requestedPixels)throws IOException;
```

출력 Bitmap의 최대 한 변을 requestedPixels로 제한합니다. 0 이하이면 조회 전 `IllegalArgumentException`입니다. 미등록 ID는 `FileNotFoundException`, `.png`가 아닌 asset, 잘못된 이미지 bounds, decode 실패는 `IOException`입니다.

preview는 최대 8 MiB, 한 변 4096픽셀, 전체 4×1024×1024픽셀입니다. bounds 확인과 power-of-two sampling 후 필요하면 축소합니다. 종횡비를 유지하며 확대하지 않고 density는 DENSITY_NONE입니다. metadata의 width, height 대신 실제 decode bounds를 검사합니다. 반환 BitmapDrawable의 Bitmap은 caller가 사용 수명을 관리합니다.
