---
title: "WMDroid SDK"
slug: en
---

Android UI library for WM-style Activities and Today providers. Minimum Android API14. Package: `me.ancal.wmdroid.sdk`.

## Choose an integration

| Goal | Guide | Entry API |
| --- | --- | --- |
| Add the SDK to an Android app | [Build and integrate the AAR](/en/getting-started/) | AAR or source integration |
| Create an Activity with title, Start, and bottom commands | [Applets and shell](/en/applets/) | [WmApplet](/en/api/wmapplet/) |
| Publish Today content from an existing app | [Today modules](/en/widgets/) | [TodayProvider](/en/api/todayprovider/), [TodayContract](/en/api/todaycontract/) |
| Compose controls in a caller-owned window | [Shared UI](/en/ui/) | [WmControls](/en/api/wmcontrols/), [WmWindow](/en/api/wmwindow/) |
| Load fonts, catalogs, or a resource pack | [Fonts and resources](/en/resources/) | [WmFonts](/en/api/wmfonts/), [WmResources](/en/api/wmresources/), [WmAssets](/en/api/wmassets/) |

The app owns its data, permissions, and draft storage. `WmApplet` supplies the shared shell. A Today provider supplies rows to the launcher without replacing the app's Activity.

Use [verification and troubleshooting](/en/verification/) to check an AAR consumer, API14 behavior, live settings, and provider updates. [Writing rules](/en/style/) define terminology and requirements for documentation changes.

Match the AAR and source ZIP against the supplied artifact checksums. Local SDK builds also produce `sdk-build-report.json`; see [distribution matching](/en/getting-started/#match-the-distribution).
