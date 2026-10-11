---
title: "Getting started"
slug: en/getting-started
---

## Tools

An Android SDK and Java 17 JDK are required. Default platform: `android-35`. Local APK builds require dependency binaries in `tools/cache` and stop if they are missing. AAR generation does not require these binaries. Local build tools do not download dependencies.

Run from the extracted source or repository root containing `tools/` and `sdk/`. Set `ANDROID_SDK_PATH` and `WMDROID_JDK_PATH` to your tool paths.

## Build the AAR

```sh
python3 tools/build_sdk.py \
  --sdk "$ANDROID_SDK_PATH" --jdk "$WMDROID_JDK_PATH"
```

Output directory: `sdk/build/`.
Output files: `WMDroid-sdk.aar`, `WMDroid-sdk-sources.zip`, and `sdk-build-report.json`.
`runtime_verified: false`: packaging checks do not include device tests.

## Gradle integration

Copy the AAR into the app module's `libs/` directory. Use Android build tools that support Java 17 classes and API14 core-library desugaring.

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

This is part of the app module configuration. Set project plugins and the namespace separately.

- API14 `java.time`: core-library desugaring is required. The AAR does not include the backport runtime.
- Single-DEX limit exceeded: configure legacy multidex.
- SDK integration: use either the AAR or sources. Using both creates duplicate classes.

## Build the separate examples project

Extract `WMDroid-examples-sources.zip` into its own directory. Its root contains `sample/`, `widgets/`, `today-probe/`, `settings.gradle`, and `build.gradle`. It contains neither SDK sources nor Python build tools. Copy `WMDroid-sdk.aar` to `libs/WMDroid-sdk.aar` at that root.

Use Java 17, Android SDK platform `android-35`, and Gradle compatible with the project's Android Gradle Plugin `8.7.3`. No Gradle wrapper is included. Configure the Android SDK path for Gradle. Gradle resolves the pinned dependencies from its configured repositories; an offline build needs them in the Gradle cache.

Run from the examples root:

```sh
gradle :sample:app:assembleDebug
gradle :widgets:app:assembleDebug :today-probe:app:assembleDebug
```

The three modules consume the same binary AAR. They enable core-library desugaring `2.1.5` and AndroidX multidex `2.0.1`; they do not compile a second copy of the SDK.

Run the sample Activity from the installed app. Its initial shell contains Counter and Info tabs, counter controls, and a Menu command. The small `DemoActivity` fragment in [Applets and shell](/en/applets/) is a different example: it displays `Ready` and a Close command. Build success alone does not verify either screen on a device.

After integration, follow [Applets and shell](/en/applets/) for an Activity or [Today modules](/en/widgets/) for a provider. See [Verification](/en/verification/) for archive and runtime checks.

## Build the separate launcher project

Extract `WMDroid-launcher-sources.zip` separately and copy the AAR to its root `libs/WMDroid-sdk.aar`. This project contains `app/` and `tools/build_apk.py`. Its Python APK builder requires the pinned dependency binaries listed in `tools/cache/desugar_jdk_libs/2.1.5/dependencies.json` and `tools/cache/multidex/2.0.1/dependencies.json`; it does not download them.

```sh
python3 tools/build_apk.py \
  --sdk "$ANDROID_SDK_PATH" --jdk "$WMDROID_JDK_PATH" \
  --sdk-aar libs/WMDroid-sdk.aar
```

Do not combine `--sdk-aar` and `--without-sdk-sources`. In a combined development tree, omitting both includes SDK sources; that is a different build layout from these separate binary-AAR consumers.

## Manifest

AAR declarations: `INTERNET`, `ACCESS_NETWORK_STATE`, `MODIFY_AUDIO_SETTINGS`, `ACCESS_NOTIFICATION_POLICY`, and `READ_PHONE_STATE`. The AAR also declares launcher and shell-provider visibility.

Declarations do not grant runtime permissions or special access. Inspect the final merged manifest. Before you remove an unused permission, verify that its calls fail safely.

## Match the distribution

`WMDroid-sdk-sources.zip` contains this documentation under `sdk/docs/`, SDK Java sources under `sdk/src/`, and theme templates under `sdk/templates/`. It can build the AAR using `tools/build_sdk.py`; it does not contain the examples or launcher app projects.

A local SDK build writes `sdk-build-report.json` beside the AAR and source ZIP in `sdk/build/`. Its `artifacts` entries record each file's `sha256` and `bytes`. Compare local outputs with that report. For a supplied distribution, use its supplied checksums or artifact manifest; do not assume the local build report is included. Today v1 and pack version are protocol identifiers, not SDK distribution versions.

```sh
sha256sum WMDroid-sdk.aar WMDroid-sdk-sources.zip
wc -c WMDroid-sdk.aar WMDroid-sdk-sources.zip
```

Run those commands in the directory containing the two files and compare with the matching manifest entries.

## Examples and distribution inputs

The separate `WMDroid-examples-sources.zip` contains three complete projects:

| Project | Package | Purpose |
| --- | --- | --- |
| `sample/` | `me.ancal.wmdroid.sample` | WM-style Activity, shell, and minimal Today provider. |
| `widgets/` | `me.ancal.wmdroid.widgets` | Weather, Transit, Splatnet, and Battery providers. |
| `today-probe/` | `org.example.todayprobe` | Ordinary Android Activity with two Today modules. |

For the shell example, read `sample/app/src/main/java/me/ancal/wmdroid/sample/SampleActivity.java`, `SampleProvider.java`, and `sample/app/src/main/AndroidManifest.xml` together. The SDK theme template is `sdk/templates/res/values/wm_applet_theme.xml` in the SDK source archive. Build commands for the separate examples are above; no `sdk/` prefix belongs before their project names.

Repository and release URLs will be added after publication destinations are confirmed. The archive names above identify the current package layouts without assuming an unpublished URL.
