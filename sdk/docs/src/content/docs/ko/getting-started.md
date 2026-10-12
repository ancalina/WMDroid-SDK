---
title: "시작하기"
slug: ko/getting-started
---

## 도구

Android SDK, Java 17 JDK 필요. 기본 플랫폼: `android-35`. 로컬 APK 빌드는 `tools/cache`의 의존성 바이너리가 필요하며, 없으면 중단합니다. AAR 생성에는 이 바이너리가 필요하지 않습니다. 로컬 빌드 도구는 의존성을 다운로드하지 않습니다.

`tools/`와 `sdk/`가 있는 압축 해제 소스 또는 저장소 루트에서 실행하세요. `ANDROID_SDK_PATH`, `WMDROID_JDK_PATH`에 자신의 도구 경로를 지정하세요.

## AAR 생성

```sh
python3 tools/build_sdk.py \
  --sdk "$ANDROID_SDK_PATH" --jdk "$WMDROID_JDK_PATH"
```

출력 디렉터리: `sdk/build/`.
출력 파일: `WMDroid-sdk.aar`, `WMDroid-sdk-sources.zip`, `sdk-build-report.json`.
`runtime_verified: false`: 패키징 검사는 기기 검사를 포함하지 않습니다.

## Gradle 연결

AAR를 앱 모듈의 `libs/`에 복사하세요. Java 17 클래스와 API14 core-library desugaring을 지원하는 Android 빌드 도구를 사용하세요.

```groovy
android {
    compileSdk 35
    defaultConfig { minSdk 14 }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
        coreLibraryDesugaringEnabled true
    }
}
dependencies {
    implementation files('libs/WMDroid-sdk.aar')
    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:2.1.5'
}
```

앱 모듈 설정 일부입니다. project plugin, namespace는 별도로 지정하세요.

- API14 `java.time`: core-library desugaring 필수. AAR에 backport 런타임은 없습니다.
- 단일 DEX 한도 초과: legacy multidex 설정 필수.
- SDK 연결: AAR 또는 소스 중 하나만 사용하세요. 둘 다 넣으면 클래스가 중복됩니다.

## 별도 예제 프로젝트 빌드

`WMDroid-examples-sources.zip`을 별도 디렉터리에 압축 해제하세요. 루트에는 `sample/`, `widgets/`, `today-probe/`, `settings.gradle`, `build.gradle`이 있습니다. SDK 소스와 Python 빌드 도구는 포함하지 않습니다. 같은 루트의 `libs/WMDroid-sdk.aar`에 AAR를 복사하세요.

Java 17, Android SDK 플랫폼 `android-35`, 프로젝트의 Android Gradle Plugin `8.7.3`과 호환되는 Gradle을 준비하세요. Gradle wrapper는 포함하지 않습니다. Gradle에서 사용할 Android SDK 경로를 지정하세요. Gradle은 설정된 저장소에서 고정 버전 의존성을 가져오므로, 오프라인 빌드는 해당 의존성이 Gradle cache에 있어야 합니다.

예제 루트에서 실행하세요.

```sh
gradle :sample:app:assembleDebug
gradle :widgets:app:assembleDebug :today-probe:app:assembleDebug
```

세 모듈은 같은 바이너리 AAR를 사용합니다. core-library desugaring `2.1.5`와 AndroidX multidex `2.0.1`을 설정하며, SDK 소스를 중복 컴파일하지 않습니다.

설치한 앱에서 샘플 Activity를 실행하세요. 초기 셸에는 카운터, 정보 탭, 카운터 조작, 메뉴 명령이 있습니다. [애플릿과 셸](/ko/applets/)의 작은 `DemoActivity` fragment는 별도 예제이며 `Ready`와 Close 명령을 표시합니다. 빌드 성공만으로 두 화면의 기기 동작이 검증되지는 않습니다.

연결 후 Activity는 [애플릿과 셸](/ko/applets/), provider는 [Today 모듈](/ko/widgets/)을 따르세요. 아카이브, 실행 검사는 [검증](/ko/verification/)에 있습니다.

## 별도 런처 프로젝트 빌드

`WMDroid-launcher-sources.zip`을 별도로 압축 해제하고 루트의 `libs/WMDroid-sdk.aar`에 AAR를 복사하세요. 이 프로젝트에는 `app/`과 `tools/build_apk.py`가 있습니다. Python APK 빌더는 `tools/cache/desugar_jdk_libs/2.1.5/dependencies.json`과 `tools/cache/multidex/2.0.1/dependencies.json`에 명시된 고정 의존성 바이너리가 필요하며, 이를 다운로드하지 않습니다.

```sh
python3 tools/build_apk.py \
  --sdk "$ANDROID_SDK_PATH" --jdk "$WMDROID_JDK_PATH" \
  --sdk-aar libs/WMDroid-sdk.aar
```

`--sdk-aar`와 `--without-sdk-sources`는 함께 사용하지 마세요. 통합 개발 소스에서 둘 다 생략하면 SDK 소스를 컴파일합니다. 별도 프로젝트의 바이너리 AAR 사용 방식과는 다른 빌드 구조입니다.

## Manifest

AAR 선언: `INTERNET`, `ACCESS_NETWORK_STATE`, `MODIFY_AUDIO_SETTINGS`, `ACCESS_NOTIFICATION_POLICY`, `READ_PHONE_STATE`. 런처, 셸 provider visibility도 선언합니다.

선언은 runtime permission, 특별 접근을 부여하지 않습니다. 최종 merged manifest를 확인하세요. 사용하지 않는 권한을 제거하기 전에 해당 호출의 안전한 실패를 검사하세요.

## 배포본 대조

`WMDroid-sdk-sources.zip`에는 `sdk/docs/`의 문서, `sdk/src/`의 SDK Java 소스, `sdk/templates/`의 테마 원본이 있습니다. `tools/build_sdk.py`로 AAR를 생성할 수 있으며, 예제와 런처 앱 프로젝트는 포함하지 않습니다.

로컬 SDK 빌드는 `sdk/build/`의 AAR, 소스 ZIP 옆에 `sdk-build-report.json`을 생성합니다. `artifacts`에는 각 파일의 `sha256`, `bytes`가 기록됩니다. 로컬 출력은 이 보고서와 대조하세요. 제공받은 배포본은 함께 제공된 checksum 또는 artifact manifest와 대조하며, 로컬 빌드 보고서가 반드시 포함된다고 가정하지 마세요. Today v1, pack version은 프로토콜 식별자이며 SDK 배포 버전이 아닙니다.

```sh
sha256sum WMDroid-sdk.aar WMDroid-sdk-sources.zip
wc -c WMDroid-sdk.aar WMDroid-sdk-sources.zip
```

두 파일이 있는 디렉터리에서 실행하고 manifest의 같은 파일 항목과 비교하세요.

## 예제와 배포 준비물

별도 `WMDroid-examples-sources.zip`에는 완전한 프로젝트 세 개가 있습니다.

| 프로젝트 | 패키지 | 용도 |
| --- | --- | --- |
| `sample/` | `me.ancal.wmdroid.sample` | WM 스타일 Activity, 셸, 최소 Today provider. |
| `widgets/` | `me.ancal.wmdroid.widgets` | Weather, Transit, Splatnet, Battery provider. |
| `today-probe/` | `org.example.todayprobe` | 일반 Android Activity와 Today 모듈 두 개. |

셸 예제는 `sample/app/src/main/java/me/ancal/wmdroid/sample/SampleActivity.java`, `SampleProvider.java`, `sample/app/src/main/AndroidManifest.xml`을 함께 읽으세요. SDK 테마 원본 `sdk/templates/res/values/wm_applet_theme.xml`은 SDK 소스 아카이브에 있습니다. 별도 예제 빌드 명령은 위에 있으며, 예제 프로젝트 이름 앞에는 `sdk/`가 붙지 않습니다.

저장소, 릴리즈 URL은 공개 위치가 확정된 뒤 추가합니다. 위 아카이브 이름은 아직 공개되지 않은 URL을 가정하지 않고 현재 묶음의 구조를 나타냅니다.
