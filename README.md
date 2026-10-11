# WMDroid SDK

[한국어](README.ko.md)

[한국어 개발 가이드](sdk/docs/src/content/docs/ko/index.md) · [English developer guide](sdk/docs/src/content/docs/en/index.md) · [API reference](sdk/docs/src/content/docs/en/api.md)

Publish Today modules from an Android app, or use the shared controls and shell
in a WM-style activity. A Today provider can belong to an app with a normal
Android UI. The launcher renders provider data and RemoteViews; it does not load
another app's view classes.

## Build

Run from the extracted SDK source root containing `tools/` and `sdk/`.
Replace the SDK/JDK paths with existing installations. AAR generation does not
require the APK backport binaries listed in [DEPENDENCIES.md](DEPENDENCIES.md).

```sh
python3 tools/build_sdk.py --sdk /path/to/android-sdk --jdk /path/to/jdk
```

Outputs: `sdk/build/WMDroid-sdk.aar` and `sdk/build/WMDroid-sdk-sources.zip`.

Build the sample from the separate `WMDroid-examples-sources.zip`, not from
this SDK source tree. The examples root contains `sample/`, `widgets/`, and
`today-probe/`; copy the AAR to that root's `libs/WMDroid-sdk.aar`.
Use Java 17, Android SDK `android-35`, and Gradle compatible with Android Gradle
Plugin `8.7.3`. No Gradle wrapper is included. Configure the Android SDK path.

Run from the examples root:

```sh
gradle :sample:app:assembleDebug
```

The sample APK is written under `sample/app/build/outputs/apk/`.
The Python SDK builder does not download SDKs, JDKs or dependencies.
Gradle resolves the examples' pinned dependencies from configured repositories;
an offline example build needs them in its Gradle cache.

The minimum Android API is 14. Features depend on Android version and permissions;
see [verification](sdk/docs/src/content/docs/en/verification.md) for the tested scope.

## Documentation

Requires Node.js 22.12.0 or newer. Run from the same SDK source root:

```sh
cd sdk/docs
npm ci
npm run build
npm run dev -- --host 127.0.0.1 --port 8000
```

Open `http://127.0.0.1:8000/ko/` or `http://127.0.0.1:8000/en/`.
The build writes `sdk/docs/dist/`. npm installs documentation dependencies only.
Search works in the production preview, not the development server:

```sh
npm run preview -- --host 127.0.0.1 --port 8001
```

Open `http://127.0.0.1:8001/ko/` to test search.

## Integration

| Task | Guide |
| --- | --- |
| Configure the AAR and manifest | [Getting started](sdk/docs/src/content/docs/en/getting-started.md) |
| Publish Today modules and handle interaction | [Today modules](sdk/docs/src/content/docs/en/widgets.md) |
| Use the shared shell, Start menu and applet lifecycle | [Applets](sdk/docs/src/content/docs/en/applets.md) |
| Use controls, tabs, dialogs and scrolling | [Common UI](sdk/docs/src/content/docs/en/ui.md) |
| Follow font, theme and resource-pack changes | [Fonts and resources](sdk/docs/src/content/docs/en/resources.md) |
| Check signatures, parameters and state contracts | [API reference](sdk/docs/src/content/docs/en/api.md) |

WM-style controls use logical VGA pixels. `WmMetrics` supplies common sizes;
`WmViewport` scales the surface. Do not apply Android density or sp scaling inside
that surface.

`WmApplet` follows the launcher's selected resource pack on its worker. Use
`onResourcesChanged()` to refresh app-owned artwork. Imported packs are runtime
data; the AAR and source ZIP do not include an original Microsoft or OEM pack.

## License

SDK code, templates and documentation use [Apache-2.0](sdk/LICENSE).
WMDroid-authored `sample/`, `widgets/`, and `today-probe/` app sources in the
separate examples source archive use WTFPL v2. They are not included in this SDK
source archive; preserve each example project's `LICENSE` when distributing it.
The bundled Gulim font retains OFL1.1. Preserve [NOTICE](sdk/NOTICE) and applicable
third-party licenses when distributing the SDK or an app that includes it.
[LICENSES.md](LICENSES.md) describes the launcher and SDK license scopes.
