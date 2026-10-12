---
title: "TodayContract"
slug: ko/api/todaycontract
---
[API](/ko/api/)

`sdk/src/main/java/me/ancal/wmdroid/sdk/TodayContract.java`

```java
public final class TodayContract
```

## Discovery

```java
public static final int VERSION = 1;
public static final String DISCOVER = "me.ancal.wmdroid.sdk.TODAY_MODULE";
public static final String VERSION_META = "me.ancal.wmdroid.sdk.VERSION";
public static final String VIEW_META = "me.ancal.wmdroid.sdk.CUSTOM_VIEWS";
```

provider intent-filter에 `DISCOVER` action, metadata에 `VERSION_META=1`을 선언하세요. custom view를 제공하면 `VIEW_META=true`도 선언합니다. [Manifest 예제](/ko/widgets/)를 따르세요.

호스트는 enabled, exported provider만 발견하며 단일 authority, version 1 metadata, `readPermission=null`을 요구합니다. API 19 이상에서는 discovery action으로 검색합니다. API 14–18에서는 전체 provider 목록에서 metadata로 검색합니다. 설치 후 Today 항목 설정에서 모듈을 켜야 조회합니다.

## rows

```java
public static Uri rows(String authority);
```

`content://<authority>/today/v1/rows`를 반환합니다. Manifest authority를 전달하세요. helper 자체는 authority를 검증하거나 language parameter를 추가하지 않습니다. 영어 조회에는 반환 URI에 `language=en`을 추가하세요. [query 계약](/ko/api/todayprovider/#query)을 따릅니다.

## COLUMNS

```java
public static final String[] COLUMNS = {"id", "title", "detail", "icon", "activity"};
```

`MatrixCursor` 행의 열 순서입니다. 공개 배열을 수정하면 provider 조회, 호스트 열 검색에 영향을 주므로 배열 내용을 변경하지 마세요.

| 열 | 값, 동작 |
| --- | --- |
| `id` | provider 안에서 고유하고 안정적인 문자열. 빈 ID는 건너뜀. |
| `title` | 공개 제목. 빈 title은 건너뜀. |
| `detail` | 공개 상세 문자열. 빈 문자열 허용. |
| `icon` | provider 패키지 drawable resource ID. `0`은 기본 아이콘. |
| `activity` | 같은 패키지 Activity 클래스명. `.`으로 시작하면 패키지명을 앞에 붙임. 빈 문자열은 실행 없음. |

행 실행 대상은 enabled, exported이며 `permission=null`인 Activity여야 합니다. 조건에 맞지 않으면 실행하지 않습니다.

호스트는 대상 Activity를 지정한 `Intent.ACTION_VIEW`로 실행하고, 행의 `id`를 문자열 extra `me.ancal.wmdroid.sdk.ROW`에 전달합니다. Activity에서 `getIntent().getStringExtra("me.ancal.wmdroid.sdk.ROW")`로 선택된 행을 식별하세요.

공개 provider에는 비밀 키, 계정 토큰, 개인 알림 내용을 넣지 마세요.

## Row limits

```java
public static final int MAX_ROWS = 8;
public static final int MAX_TEXT = 512;
```

호스트는 provider별 처음 8개 cursor 행만 검사합니다. 건너뛴 행도 이 한도에 포함됩니다. 문자열 `null`은 빈 문자열로 바꾸고 최대 512 UTF-16 code units로 자릅니다. 그 후 빈 ID, 빈 title, 중복 ID 행을 건너뜁니다. 중복 비교는 provider별 잘린 ID 기준입니다. 긴 ID가 같아지거나 surrogate pair 중간에서 잘릴 수 있으므로 원래 문자열을 한도 안에 유지하세요.

이 제한은 [`TodayProvider.fillRows`](/ko/api/todayprovider/#fillrows)에서 강제하지 않습니다.

## Custom content

```java
public static final String VIEW_METHOD = "today_widget";
public static final String VIEW_KEY = "view";
public static final String HEIGHT_KEY = "height";
public static final int WIDTH = 480;
public static final int MIN_HEIGHT = 32;
public static final int MAX_HEIGHT = 480;
```

`VIEW_METHOD`로 [`TodayProvider.call`](/ko/api/todayprovider/#call)을 호출합니다. content가 있으면 결과 Bundle의 `VIEW_KEY`에 `RemoteViews`, `HEIGHT_KEY`에 높이가 들어갑니다. 없으면 빈 Bundle입니다. `WIDTH`와 높이는 SDK 논리 VGA 픽셀이며 dp가 아닙니다. 호스트가 사용할 physical width는 `pixel_width` 옵션으로 전달합니다.

호스트는 provider 패키지의 `RemoteViews`와 32–480 높이만 받습니다. custom call 실패, 결과 검증 실패 시 같은 text 행을 유지합니다.

## Widget

```java
public static final class Widget
public final RemoteViews view;
public final int height;
public Widget(RemoteViews view,int height);
```

`view` 참조와 `height`를 보관하는 반환 객체입니다. `RemoteViews`를 복제하지 않으므로 반환 전에 설정을 끝내세요. `view=null`이면 `IllegalArgumentException("Widget view required")`, 높이가 32–480 밖이면 `IllegalArgumentException("Widget height outside 32..480")`입니다.

생성자는 view 패키지를 검사하지 않습니다. provider 패키지 검증은 `TodayProvider.call`에서 수행합니다.
