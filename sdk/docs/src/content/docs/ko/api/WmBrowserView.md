---
title: "WmBrowserView"
slug: ko/api/wmbrowserview
---
[API 색인](/ko/api/)

```java
public final class WmBrowserView extends LinearLayout implements WmControls.BackHandler
```

주소 입력줄과 Android `WebView`를 묶은 브라우저 클라이언트. 제목, 메뉴, 하단 명령, Activity 수명 연결은 소유 앱이 제공한다. 웹 엔진은 Android WebView.

## 생성자

```java
public WmBrowserView(Context context,Typeface font)
```

주소 입력은 최대 2048 UTF-16 코드 단위. JavaScript와 DOM storage 활성화; 파일, content 접근과 위치 기능 비활성화. API16 이상은 file-URL 접근, API21 이상은 mixed content도 차단한다. 네트워크 페이지에는 앱의 `INTERNET` 권한 필요. View/WebView 호출은 UI 스레드에서 수행한다.

## normalizeAddress

```java
public static String normalizeAddress(String value)
```

`String.trim()`으로 앞뒤 U+0020 이하 문자를 제거한다. 빈 문자열과 정확한 `about:blank`는 내부 빈 문서로 반환. 스킴이 없으면 `https://` 추가 후 ASCII URI로 반환한다. null, 2048자 초과, 제어문자, 잘못된 URI, HTTP(S) 외 스킴, host 없음, user-info 포함, port 65535 초과는 `IllegalArgumentException`. 길이 제한은 스킴 추가 전 문자열에 적용된다.

## Navigation

```java
public void navigate(String value)
public boolean canForward()
public boolean canBack()
public void forward()
public void refresh()
public void stop()
public boolean back()
```

`navigate`는 주소 검증 후 입력줄 갱신, 페이지 로드, 웹 포커스 이동, 키보드 숨김. 닫힌 상태에서도 주소를 먼저 검증한다. `canForward`/`canBack`은 닫힌 상태에서 false. `forward`는 가능한 경우에만 이동, `refresh`는 재로드. `stop`은 로드를 중지하고 loading=false로 알린다. `back`은 뒤로 이동을 수행했으면 true, 기록이 없으면 false.

## State and controls

```java
public EditText addressField()
public WebView webView()
public String url()
public String pageTitle()
public boolean isLoading()
public void showAddress(boolean visible)
public boolean isAddressVisible()
```

컨트롤 접근자는 실제 소유 View 반환. `url()`은 현재 주소이며 주소가 없거나 닫혔으면 빈 문자열. `pageTitle()`과 `isLoading()`은 마지막 콜백 상태. `showAddress(true)`는 입력줄에 포커스를 주고 모두 선택한다. `webView()`로 직접 설정을 바꾸면 위 보안/탐색 정책도 바뀔 수 있다.

## Listener

```java
public interface Listener {void changed(WmBrowserView browser);void invalidAddress(String address);}
public void setListener(Listener listener)
```

null 리스너는 콜백 해제. `changed`는 페이지 시작, 완료, 제목, 명시적 중지 상태를 전달한다. `invalidAddress`는 주소 입력줄의 Go 동작에서 검증 실패 시 원래 입력을 전달한다. `navigate` 직접 호출의 오류는 호출자에게 예외로 전달된다.

## Lifecycle

```java
public void pause()
public void resume()
public void close()
protected void onDetachedFromWindow()
protected void onAttachedToWindow()
```

`pause`/`resume`은 WebView 수명 호출; detach/attach에서도 자동 호출한다. `close`는 반복 호출 가능하며 로드 중지, 리스너 해제, WebView 제거, destroy를 수행한다. 소유 앱이 영구 종료 때 `close`를 호출해야 한다. 이후 탐색, pause/resume은 실행되지 않는다.


스킴은 시작의 `[A-Za-z][A-Za-z0-9+.-]*:`로 판정합니다. 포트 주소에는 HTTP(S)를 명시하세요. 길이는 trim 후 스킴 추가 전2048 UTF-16 코드 단위입니다.

| 입력 | 결과 |
| --- | --- |
|example.com/path|https://example.com/path|
|https://example.com:8080/path|동일 주소|
|example.com:8080/path|IllegalArgumentException: example.com이 스킴으로 판정됨|
|빈 값 또는 about:blank|about:blank|
|https://user@example.com/|IllegalArgumentException: user-info 거부|

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmBrowserView.java`
