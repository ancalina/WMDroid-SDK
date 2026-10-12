---
title: "TodayProvider"
slug: ko/api/todayprovider
---
[API](/ko/api/)

`sdk/src/main/java/me/ancal/wmdroid/sdk/TodayProvider.java`

```java
public abstract class TodayProvider extends ContentProvider
```

캐시된 공개 Today 행과 선택적 `RemoteViews`를 제공하는 `ContentProvider` 기본 클래스. [Manifest 등록](/ko/widgets/) 후 아래 두 abstract hook을 구현하세요.

## getAuthority

```java
protected abstract String getAuthority();
```

Manifest의 `android:authorities` 값을 반환합니다. 조회 URI와 변경 알림 URI에 사용하므로 provider 수명 동안 같은 authority를 반환하세요.

## fillRows

```java
protected abstract void fillRows(MatrixCursor rows,String language);
```

제공된 cursor에 [v1 열 순서](/ko/api/todaycontract/#columns)의 행을 추가합니다. `language`는 provider 앱에 포함된 언어 파일의 정규화된 태그입니다. 지역 태그가 없으면 가장 가까운 상위 언어 파일을 사용합니다. 잘못되었거나 지원하지 않는 태그는 `"ko"`입니다. 기본 클래스는 행 개수, 문자열 길이, ID 중복을 검증하지 않습니다. [호스트 제한](/ko/api/todaycontract/#row-limits)을 지키세요.

`query`, `call`과 이들이 호출하는 hook은 UI thread를 가정하면 안 됩니다. 공유 캐시를 동시 호출에 안전하게 관리하고 네트워크 응답을 기다리지 마세요. hook 예외는 호출자에게 전파됩니다. 반환 후 cursor를 보관하거나 수정하지 마세요.

## query

```java
public final Cursor query(Uri uri,String[] projection,String selection,String[] args,String order);
```

`uri`는 [rows URI](/ko/api/todaycontract/#rows)여야 합니다. 비교 시 query parameter만 제거합니다. fragment는 허용하지 않습니다. `selection`, `args`, `order`는 모두 `null`이어야 합니다. 조건을 어기면 `IllegalArgumentException("Unsupported Today query")`입니다.

`projection`을 무시하고 전체 `COLUMNS`를 가진 새 `MatrixCursor`에 `fillRows`를 호출합니다. `language` parameter는 위 언어 태그 정규화를 따릅니다. 예를 들어 앱에 `en`만 있고 `en-gb`가 없으면 `en-GB`는 `en`으로 바뀝니다. 다른 query parameter는 행 선택에 사용하지 않습니다.

반환 cursor의 notification URI는 parameter를 포함한 입력 `uri`입니다. 호출자가 cursor를 닫습니다. 조회는 비동기 리소스 동기화를 요청하지만 완료를 기다리지 않습니다.

## createWidget

```java
protected TodayContract.Widget createWidget(String id,Bundle options);
```

기본 구현은 `null`을 반환합니다. custom content를 제공하려면 override하고 provider metadata에 `CUSTOM_VIEWS=true`를 선언하세요. `id`에 해당하는 content가 없으면 `null`로 text 행을 유지합니다. `call`은 ID가 실제 조회 행에 있는지 확인하지 않으므로 hook에서 처리하세요.

`options`는 호출별 새 Bundle입니다. 아래 정규화된 값만 들어갑니다. 폰트 경로는 파일 접근 권한을 부여하지 않습니다. [공유 리소스](/ko/api/wmresources/)를 사용하려면 동기화 완료 전에도 fallback으로 그릴 수 있어야 합니다.

| 옵션 | 정규화, 기본값 |
| --- | --- |
| `width` | 입력과 무관하게 480 논리 픽셀. |
| `pixel_width` | 480–1440 physical pixels로 제한. 기본 480. |
| `language` | 앱에 포함된 정규화된 언어 태그. 상위 언어로 fallback. 기본 `"ko"`. |
| `accent` / `text_color` | ARGB int. 기본 `0xff149ca7` / `0xff000000`. |
| `font` | null/없음: `"fonts/malgun/regular.ttf"`. 빈 문자열은 유지. |
| `font_bold` | 기본 빈 문자열. |
| `hybrid` | boolean. 기본 `true`. |

다른 `extras` key는 hook에 전달하지 않습니다.

## call

```java
public final Bundle call(String method,String arg,Bundle extras);
```

`method`는 `VIEW_METHOD`(`"today_widget"`), `arg`는 비어 있지 않은 행 ID이며 최대 512 UTF-16 code units입니다. 다른 method는 `IllegalArgumentException("Unsupported Today call")`, 잘못된 ID는 `IllegalArgumentException("Invalid Today row ID")`입니다.

`extras=null`이면 기본 옵션을 사용합니다. 옵션을 정규화한 후 `createWidget(arg, options)`를 호출합니다. 결과가 `null`이면 빈 Bundle입니다. 그 외에는 `VIEW_KEY`의 `RemoteViews`와 `HEIGHT_KEY`의 높이를 반환합니다. view의 패키지가 provider 패키지와 다르면 `IllegalArgumentException("Widget resources must belong to provider package")`입니다. 리소스 동기화 완료를 기다리지 않습니다.

## changed

```java
protected final void changed();
```

비동기 데이터 갱신 후 호출하세요. query parameter 없는 `rows(getAuthority())` URI에 `notifyChange(uri, null)`을 호출해 호스트의 재조회를 요청합니다.

## onCreate

```java
public boolean onCreate();
```

현재 리소스 revision을 기록하고 비동기 동기화를 요청한 뒤 `true`를 반환합니다. override하면 superclass를 호출하세요.

provider 인스턴스는 런처 `WmShell.STATE`를 관찰합니다. `onCreate`, `query`, `call`, state 변경은 공용 단일 worker에 리소스 동기화를 요청합니다. 진행 중 중복 요청은 후속 요청 하나로 합칩니다. 완료 시 revision이 바뀌고 아직 중지되지 않았으면 `changed()`를 호출합니다. 런처 패키지 자체는 이 동기화를 건너뜁니다.

observer 등록 실패(`RuntimeException`)와 동기화의 `IOException`은 무시합니다. 기존 로컬 리소스/fallback을 사용하세요. 다른 unchecked 예외까지 처리하는 계약은 아닙니다.

## shutdown

```java
public void shutdown();
```

중지 상태를 설정하고 observer와 후속 동기화 요청을 제거한 뒤 superclass를 호출합니다. 이미 실행 중인 동기화 task나 공용 worker를 종료하지 않습니다. override하면 superclass를 호출하세요.

## getType

```java
public final String getType(Uri uri);
```

URI 검증 없이 `"vnd.android.cursor.dir/vnd.me.ancal.wmdroid.module.v1"`을 반환합니다. 조회 가능 여부는 `query`에서 검증합니다.

## insert, update, delete

```java
public final Uri insert(Uri u,ContentValues v);
public final int update(Uri u,ContentValues v,String s,String[] a);
public final int delete(Uri u,String s,String[] a);
```

모든 호출에서 `UnsupportedOperationException("Read-only")`를 던집니다.
