---
title: "Today 모듈"
slug: ko/widgets
---


## 연결

일반 Android Activity를 유지하고 `TodayProvider`만 추가할 수 있습니다. 일반 Activity에는 `WM_STYLE`을 선언하지 마세요. 행의 `activity`는 같은 패키지의 exported Activity 이름입니다. 실행이 필요 없으면 빈 문자열을 사용하세요.

```java
package org.example.demo;

import android.database.MatrixCursor;
import me.ancal.wmdroid.sdk.TodayProvider;

public final class DemoProvider extends TodayProvider {
    @Override protected String getAuthority() {
        return "org.example.demo.today";
    }
    @Override protected void fillRows(MatrixCursor rows, String language) {
        boolean english = "en".equals(language);
        rows.addRow(new Object[] {
            "status", english ? "Status" : "상태",
            english ? "Ready" : "준비됨", 0,
            "org.example.demo.DemoActivity"
        });
    }
}
```

이 DemoProvider는 `ko`·`en` 문구만 제공한다. `language`는 밑줄을 하이픈으로 바꾸고 소문자로 정규화한 카탈로그 태그이다. 요청 카탈로그가 없으면 포함된 가장 가까운 상위 태그, 일치가 없으면 `ko`를 사용한다. 예: `en-gb` 없이 `en`만 포함하면 `en-GB` 요청은 `en`으로 전달된다.

```xml
<provider android:name=".DemoProvider"
    android:authorities="org.example.demo.today"
    android:exported="true"
    android:label="Demo">
    <intent-filter>
        <action android:name="me.ancal.wmdroid.sdk.TODAY_MODULE" />
    </intent-filter>
    <meta-data android:name="me.ancal.wmdroid.sdk.VERSION"
        android:value="1" />
</provider>
```

Manifest 요소는 `<application>` 안에 넣으세요. `DemoActivity`도 exported로 선언하세요. 런처 오늘 항목 설정에서 모듈을 활성화하세요. 설치만으로 활성화되지 않습니다.

## Provider API

```text
protected abstract String getAuthority();
protected abstract void fillRows(MatrixCursor rows, String language);
protected TodayContract.Widget createWidget(String id, Bundle options);
protected final void changed();
// TodayContract
public static Uri rows(String authority);
// TodayContract.Widget
public Widget(RemoteViews view, int height);
```

- `getAuthority()`: Manifest와 같은 고유 authority를 반환합니다.
- `fillRows()`: 공개 행을 rows에 추가합니다. `language`는 provider 앱에 포함된 정규화된 언어 태그입니다. 지역 태그가 없으면 가장 가까운 상위 언어를 사용하고, 잘못되었거나 지원하지 않는 태그는 `ko`입니다. cached data를 즉시 반환하세요.
- `createWidget()`: 안정적인 행 ID의 custom content를 반환합니다. null이면 text 행을 사용합니다.
- `changed()`: rows URI에 변경을 알려 호스트 재조회를 요청합니다.
- `TodayContract.rows()`: `content://<authority>/today/v1/rows` 반환.
- `Widget()`: view 참조·height 보관. null view·범위 밖 height는 `IllegalArgumentException`.

provider 호출에서 네트워크·큰 파일 읽기를 기다리지 마세요. provider callback에서 UI thread를 가정하지 마세요. Activity 존재를 가정하지 마세요. background 작업 완료 후 `changed()`를 호출하세요. 이전 요청 결과가 새 상태를 덮지 않게 요청 순서를 관리하세요.

`TodayProvider`는 리소스 변경을 worker에서 동기화합니다. `onCreate()`·`shutdown()`을 override하면 superclass를 호출하세요.

커스텀 뷰 갱신 중에는 마지막으로 검증된 뷰를 유지합니다. 내용은 새 응답이 올 때까지 이전 값일 수 있습니다. null·유효하지 않은 결과·호출 실패는 텍스트 행으로 전환합니다. 삭제된 행이나 다른 앱 버전의 뷰는 재사용하지 않습니다.

inflate 실패 시 텍스트 행을 사용하며, 같은 `RemoteViews` 객체를 다시 inflate하지 않습니다. 새 객체는 다시 시도합니다.

## v1 행

열 순서: `id`, `title`, `detail`, `icon`, `activity`.

| 열 | 값 |
| --- | --- |
| `id` | provider 내부의 고유·안정적 문자열. 빈 값 불가 |
| `title` | 비어 있지 않은 공개 제목 |
| `detail` | 공개 설명. 빈 값 허용 |
| `icon` | provider 패키지의 drawable resource ID. `0`: 기본값 |
| `activity` | 같은 패키지의 exported Activity. 빈 값: 실행 없음 |

| 호스트 조건 | 제한 |
| --- | --- |
| provider | exported·enabled·version 1. readPermission 불가 |
| 행 수 | provider당 처음 8행만 읽음 |
| 문자열 | 최대 512자로 자름 |
| 제외 | 빈 ID·제목, 중복 ID |
| language query | provider 앱의 언어 태그로 정규화. 가장 가까운 상위 언어로 fallback. 기본 `ko` |

공개 provider는 다른 앱도 조회할 수 있습니다. 행·옵션에 비밀 키·개인 알림·계정 토큰을 넣지 마세요.

```text
public final Cursor query(Uri uri, String[] projection, String selection,
                          String[] args, String order);
public final Bundle call(String method, String arg, Bundle extras);
```

`query()`는 rows URI만 허용합니다. selection·args·order가 null이 아니면 `IllegalArgumentException`입니다. query parameter는 URI 비교에서 제외합니다. projection과 관계없이 v1 전체 열을 반환합니다. caller가 Cursor를 닫아야 합니다. insert·update·delete는 `UnsupportedOperationException("Read-only")`입니다.

## 한 APK의 여러 모듈

- 여러 행: 하나의 provider에서 고유 ID별로 반환합니다.
- 개별 활성화: provider를 나누고 각각 고유 authority·discovery metadata를 지정합니다.
- 현재 예제: `WidgetsProvider.Weather`, `.Transit`, `.Splatnet`, `.Battery`.


## Custom content

provider metadata에 `me.ancal.wmdroid.sdk.CUSTOM_VIEWS=true`를 추가하세요.

```java
@Override protected me.ancal.wmdroid.sdk.TodayContract.Widget createWidget(
        String id, android.os.Bundle options) {
    if (!"status".equals(id)) return null;
    android.widget.RemoteViews view = new android.widget.RemoteViews(
        getContext().getPackageName(), R.layout.today_widget);
    view.setTextViewText(R.id.caption, "Ready");
    return new me.ancal.wmdroid.sdk.TodayContract.Widget(view, 40);
}
```

앱의 `res/layout/today_widget.xml`을 다음과 같이 만드세요. 위 Provider metadata에 `CUSTOM_VIEWS=true`도 추가하세요.

```xml
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/caption"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center_vertical" />
```

앱이 `today_widget` layout·`caption` ID를 제공해야 합니다. `RemoteViews` 지원 컨트롤만 사용하세요. layout은 provider 패키지에 속해야 합니다. 다른 패키지 view이면 `IllegalArgumentException`입니다.

`Widget.height`: 32–480 논리 픽셀, dp 아님. 반환 전 view 설정을 끝내세요. 호스트 검증 실패나 지원하지 않는 layout은 text 행으로 돌아갑니다. 큰 Bitmap은 Binder 전송 한도에 영향을 줍니다. 응답 크기를 줄이세요.

| options | 정규화·기본값 |
| --- | --- |
| `width` | 480 논리 픽셀 |
| `pixel_width` | 480–1440으로 제한. 기본 480 |
| `language` | provider 앱에 포함된 정규화된 언어 태그. 상위 언어로 fallback. 기본 `ko` |
| `accent` | ARGB. 기본 `0xff149ca7` |
| `text_color` | ARGB. 기본 `0xff000000` |
| `font` | 기본 `fonts/malgun/regular.ttf` |
| `font_bold` | 기본 빈 문자열 |
| `hybrid` | 기본 true |

위 값은 `TodayProvider.call()`의 기본값입니다. 호스트가 값을 전달하면 기본값 대신 사용합니다. 폰트 경로는 파일 접근 권한이 아닙니다.

`call()`은 method `today_widget`, 비어 있지 않은 ID 최대 512자만 허용합니다. 그 외는 `IllegalArgumentException`입니다. custom view가 있으면 Bundle의 `view`·`height`를 반환합니다. 없으면 빈 Bundle입니다.

행 선택·외부 divider는 호스트가 관리합니다. content layout에 중복 divider를 넣지 마세요. 마지막 항목 아래 선은 [공통 UI](/ko/ui/)의 `BETWEEN` 정책으로 제거합니다.

## 실제 Provider 예제

별도 `WMDroid-examples-sources.zip`의 `widgets/app/src/main/java/me/ancal/wmdroid/widgets/WidgetsProvider.java`와 `widgets/app/src/main/AndroidManifest.xml`은 Weather·Transit·Splatnet·Battery 연동 예제입니다. 최소 provider는 `sample/app/src/main/java/me/ancal/wmdroid/sample/SampleProvider.java`입니다. `today-probe/`는 `WmApplet`을 상속하지 않는 일반 Android Activity에서 독립 Today 모듈 두 개를 제공하는 예제입니다.

위 RemoteViews XML은 ID·생성 연결을 설명하는 fragment이며 전체 WidgetsProvider 프로젝트를 대신하지 않습니다. 바이너리 AAR로 [별도 예제 빌드](/ko/getting-started/#별도-예제-프로젝트-빌드)를 진행하세요.
