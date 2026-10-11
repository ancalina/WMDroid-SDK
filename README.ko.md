# WMDroid SDK

[English](README.md)

Android 앱에서 Today 모듈을 제공하거나 공용 컨트롤과 셸로 WM 스타일 화면을 만듭니다. Today provider는 일반 Android UI를 가진 앱에도 포함할 수 있습니다. 런처는 provider 데이터와 RemoteViews를 표시하며 다른 앱의 View 클래스를 불러오지 않습니다.

## 빌드

`tools/`와 `sdk/`가 있는 SDK 소스 루트에서 실행합니다. SDK/JDK 경로는 설치된 도구로 바꾸세요. AAR 생성에는 [DEPENDENCIES.md](DEPENDENCIES.md)의 APK backport 바이너리가 필요하지 않습니다.

```sh
python3 tools/build_sdk.py --sdk /path/to/android-sdk --jdk /path/to/jdk
```

출력은 `sdk/build/WMDroid-sdk.aar`와 `sdk/build/WMDroid-sdk-sources.zip`입니다. Python SDK builder는 SDK, JDK, 의존성을 다운로드하지 않습니다.

예제는 이 SDK 트리가 아닌 별도 `WMDroid-examples-sources.zip`에서 빌드합니다. 예제 루트에는 `sample/`, `widgets/`, `today-probe/`가 있습니다. AAR를 예제 루트의 `libs/WMDroid-sdk.aar`로 복사하세요. Java 17, Android SDK `android-35`, Android Gradle Plugin `8.7.3`과 호환되는 Gradle이 필요합니다. Gradle wrapper는 포함하지 않습니다. Android SDK 경로도 설정해야 합니다.

예제 루트에서 실행합니다.

```sh
gradle :sample:app:assembleDebug
```

APK는 `sample/app/build/outputs/apk/`에 생성됩니다. Gradle은 설정된 저장소에서 고정된 의존성을 받으므로 오프라인 빌드에는 해당 Gradle 캐시가 필요합니다.

최소 Android API는 14입니다. 기능은 Android 버전과 권한에 따라 달라집니다. 검증 범위는 [검증 기록](sdk/docs/src/content/docs/ko/verification.md)을 확인하세요.

## 문서

Node.js 22.12.0 이상이 필요합니다. 같은 SDK 소스 루트에서 실행합니다.

```sh
cd sdk/docs
npm ci
npm run build
npm run dev -- --host 127.0.0.1 --port 8000
```

`http://127.0.0.1:8000/ko/` 또는 `http://127.0.0.1:8000/en/`을 엽니다. 출력은 `sdk/docs/dist/`이며 npm은 문서 의존성만 설치합니다. 검색은 개발 서버가 아닌 production preview에서 확인합니다.

```sh
npm run preview -- --host 127.0.0.1 --port 8001
```

검색 확인 주소는 `http://127.0.0.1:8001/ko/`입니다.

## 연동

| 작업 | 가이드 |
| --- | --- |
| AAR와 manifest 설정 | [시작하기](sdk/docs/src/content/docs/ko/getting-started.md) |
| Today 모듈과 사용자 조작 | [Today 모듈](sdk/docs/src/content/docs/ko/widgets.md) |
| 셸, Start 메뉴, applet 수명주기 | [Applets](sdk/docs/src/content/docs/ko/applets.md) |
| 컨트롤, 탭, 대화상자, 스크롤 | [공용 UI](sdk/docs/src/content/docs/ko/ui.md) |
| 폰트, 테마, 리소스팩 변경 | [폰트와 리소스](sdk/docs/src/content/docs/ko/resources.md) |
| 서명, 인자, 상태 계약 | [API](sdk/docs/src/content/docs/ko/api.md) |

WM 스타일 컨트롤은 논리 VGA 픽셀을 사용합니다. `WmMetrics`는 공통 크기를 제공하고 `WmViewport`는 화면을 확대·축소합니다. 해당 화면 내부에 Android density나 sp 배율을 적용하지 마세요.

`WmApplet`은 worker에서 런처가 선택한 리소스팩을 따릅니다. 앱 자체 artwork는 `onResourcesChanged()`에서 갱신하세요. 팩은 런타임 데이터이며 AAR와 소스 ZIP에는 원본 Microsoft/OEM 팩이 포함되지 않습니다.

## 라이선스

SDK 코드, 템플릿, 문서는 [Apache-2.0](sdk/LICENSE)입니다. 별도 예제 소스 archive의 WMDroid 작성 `sample/`, `widgets/`, `today-probe/` 앱 소스는 WTFPL v2입니다. 예제는 SDK 소스 archive에 포함되지 않으며 재배포할 때 각 프로젝트의 `LICENSE`를 유지해야 합니다.

포함된 Gulim 폰트는 OFL1.1을 유지합니다. SDK나 SDK를 포함한 앱을 배포할 때 [NOTICE](sdk/NOTICE)와 해당 제3자 라이선스를 보존하세요. 런처와 SDK의 범위는 [LICENSES.md](LICENSES.md)에 설명되어 있습니다.
