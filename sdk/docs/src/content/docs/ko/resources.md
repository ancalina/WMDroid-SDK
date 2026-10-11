---
title: "글꼴과 리소스"
slug: ko/resources
---

클래스별 계약은 [WmResources](/ko/api/wmresources/), [WmResourcePack](/ko/api/wmresourcepack/), [WmAssets](/ko/api/wmassets/), [WmFonts](/ko/api/wmfonts/)에서 확인하세요.

## 팩 설치 순서

1. 앱이 연 InputStream을 worker에서 [WmResources.install(context, input)](/ko/api/wmresources/#installcontext-inputstream--resetcontext)에 전달하고 앱에서 닫습니다. 실패 IOException을 처리하세요.
2. 설치 완료 뒤 revision을 읽습니다. 설치 직후 local pack이 현재 프로세스에서 선택됩니다. 낮은 수준의 [WmResourcePack.install](/ko/api/wmresourcepack/)은 store 작업이며 이 선택 진입점과 구별하세요.
3. UI 스레드에서 화면의 리소스를 다시 적용하고 앱 소유 artwork 캐시를 무효화합니다. install 완료만으로 기존 View 그림이 교체되지 않습니다.
4. 이후 호스트 동기화가 성공하면 mirror가 다시 선택될 수 있습니다. host 불가·빈 generation·reset의 선택 규칙은 [WmResources](/ko/api/wmresources/)를 따릅니다.

## 가져온 글꼴 적용

worker에서 [WmFontFile.importFont](/ko/api/wmfontfile/)로 가져오고 파일의 `source()` URI를 얻습니다. URI 형식은 `content://me.ancal.wmdroid.shell/fonts/<64 lowercase hex digits>`입니다. `resolve()`는 provider 파일을 검증해 local 저장합니다. caller InputStream은 caller가 닫으세요. worker에서 [WmFonts.load](/ko/api/wmfonts/#loadcontext-string-boolean-boolean--family)를 준비한 뒤 UI 스레드에서 setTypeface·attach 또는 restyleTree로 화면에 적용하세요. 파일을 사용하는 Typeface가 있는 동안 변경·삭제하지 마세요.

## 리소스 API

```text
public static WmResourcePack prepare(Context context) throws IOException;
public static synchronized boolean syncFromShell(Context context) throws IOException;
public static String revision(Context context);
public static synchronized WmResourcePack install(Context context, InputStream input)
    throws IOException;
public static synchronized void reset(Context context) throws IOException;
public static InputStream open(Context context, String id) throws IOException;
public static InputStream openAsset(Context context, String path) throws IOException;
```

disk 읽기·설치·동기화는 worker에서 실행하세요. `revision()`은 disk를 읽지 않습니다. UI를 변경하는 호출은 main thread에서 실행하세요.

- `prepare()`: 선택 pack을 캐시해 반환. pack이 없으면 null.
- `syncFromShell()`: 선택 snapshot이 바뀌면 true. 런처 자신의 호출은 false.
- `revision()`: 준비된 캐시의 식별자. artwork 캐시 무효화에 사용하세요.
- `install()`: 검증·활성화한 pack 반환. caller가 input을 닫아야 합니다.
- `reset()`: local pack 해제. host mirror가 선택될 수 있습니다. 기존 snapshot은 계속 사용할 수 있습니다.
- `open()`: pack ID의 stream 반환. pack·ID가 없으면 `FileNotFoundException`.
- `openAsset()`: pack의 `assets/<path>` 우선. 없으면 앱 assets 사용.

`IOException`을 처리하세요. 반환된 stream은 caller가 닫아야 합니다. store를 직접 수정하지 마세요. 설치 중 기존 reader는 이전 snapshot을 유지합니다.

## 런처 pack 동기화

`WmApplet`·`TodayProvider`가 worker에서 동기화합니다. 별도 renderer는 `syncFromShell()`을 호출하세요. 앱에 원본 pack을 다시 포함할 필요 없습니다.

- 호스트 사용 불가: 마지막 검증 snapshot 유지.
- 호스트가 빈 pack을 명시: mirror 해제.
- revision 변경: 앱 고유 artwork 캐시 갱신.

## Pack ZIP

`pack.json`은 ZIP 루트에 둡니다. version은 숫자 `2`입니다. assets는 ID를 ZIP 내부 상대 경로에 연결합니다.

```json
{
  "version": 2,
  "assets": {
    "assets/example.png": "images/example.png"
  }
}
```

이 매핑에는 실제 `images/example.png`가 필요합니다.

| 조건 | 제한 |
| --- | --- |
| ID | `[A-Za-z0-9][A-Za-z0-9._/-]{0,159}` |
| 파일 | `pack.json` 포함 최대 4096개. 디렉터리 포함 ZIP 항목 최대 8192개. 리소스 파일당 64MiB, 압축 입력·전체 추출 각각 256MiB |
| Manifest | `pack.json` 최대 256KiB. asset 매핑 1–4095개 |
| 검증 | 안전한 ZIP 경로·중복 항목 없음. 파일 집합은 `pack.json`과 매핑 경로로 정확히 구성. 미매핑 파일 거절 |
| 실패 | 검증·파일·활성화 실패 시 `IOException` |

pack이 없으면 public fallback을 사용하세요. 폰트 기본값은 OFL Gulim입니다. 추가 에셋의 재배포 조건을 확인하세요.

## 글꼴 API

```text
public static Typeface load(Context c, String source, boolean hybrid, boolean heavy);
public static void attach(TextView view, Typeface face);
public static float measure(Paint paint, String text);
public static void draw(Canvas canvas, String text, float x, float baseline, Paint paint);
public static float centerBaseline(Paint paint, float top, float bottom, String text);
public static void setSmoothing(boolean enabled);
```

| 매개변수 | 계약 |
| --- | --- |
| `source` | `SYSTEM_SOURCE="system:default"`, `DEFAULT_SOURCE="fonts/gulim/Gulim-Regular.ttf"`, asset 경로 또는 imported source |
| `hybrid` | Tahoma를 우선 사용하고 선택 face로 미지원 글리프를 보완합니다. Tahoma 파일이 없으면 선택 face를 사용합니다. |
| `heavy` | bold 역할 요청입니다. non-hybrid 파일 face의 weight를 자동 변환하지 않습니다. bold가 필요하면 대응 source를 지정하세요. |

`load()`는 face를 캐시해 반환합니다. cold load는 disk를 읽으므로 worker에서 실행하세요. pack 준비의 `IOException`은 시스템 fallback을 선택합니다. 준비 후 imported 파일 해석·face 생성의 `IOException`은 `IllegalArgumentException`으로 변환합니다. asset 로드 실패는 시스템 fallback을 사용할 수 있습니다.

`attach()`는 main thread에서 호출하세요. Canvas text는 `measure()`·`draw()`로 같은 하이브리드 측정·그리기 계약을 사용하세요. `centerBaseline()`은 top·bottom·text에 맞춘 baseline을 반환합니다.

`setSmoothing(false)`는 process antialiasing 정책을 끕니다. 원본 Gulim bitmap strike와는 다를 수 있습니다.

`setTypeface()`·`Paint.measureText()`만 사용하면 하이브리드 폭·baseline이 달라질 수 있습니다. Android 버전·hinting·배율에 따라 글리프가 달라집니다. 글꼴 변경 후 font·layout 회귀 검사를 실행하세요.

## 테마·언어

`WmPalette`, `WmColor`, `WmPaint`와 공유 컨트롤을 사용하세요. 모든 색을 accent로 바꾸지 마세요. 표면·테두리·선택 역할을 유지하세요. `WmStrings`와 앱 locale 리소스를 사용하세요. 동적 text는 language callback에서 갱신하세요.
