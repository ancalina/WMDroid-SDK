---
title: "Verification and troubleshooting"
slug: en/verification
---

## Verification sequence

1. Prepare source and AAR from the same build.
2. Check archives, classes, the manifest, and assets.
3. Build the independent AAR consumer.
4. Run the app on API14 and your supported modern API.
5. Change fonts, theme, language, and scale while the app is open.
6. Check Start, Back, OK/X, and modal commands.
7. Enable widgets and notify completed data changes.
8. Check drafts after restart and process recreation.

Run these checks from the extracted public SDK source ZIP root, which contains `tools/` and `sdk/`.

```sh
python3 tools/check_sdk_aar.py
python3 tools/run_recurrence_rule_checks.py --jdk /path/to/jdk
```

The combined `WMDroid-public-sources.zip` includes the language catalog checker. Run it from that archive's root. The separate launcher source ZIP does not include this tool.

```sh
python3 tools/check_language_catalogs.py
```

`check_today_native_text_profiles.py` requires the private ROM/IDA workspace and is not included in public source ZIPs.

Compare visual behavior on a device. Static checks do not examine rendering.

## Troubleshooting

| Symptom | Expected condition/check | Next action | Contract |
| --- | --- | --- | --- |
| Module not listed. | Matching getAuthority/manifest authority; exported/enabled=true; VERSION=1; no readPermission. | Correct registration/metadata and reinstall. | [TodayProvider](/en/api/todayprovider/) |
| Module not displayed. | The module is enabled in this launcher’s Today items. | Enable it and confirm rows have valid id/title. | [TodayContract](/en/api/todaycontract/) |
| Custom content becomes text. | CUSTOM_VIEWS=true; provider package; height32–480; supported RemoteViews layout. | Correct metadata/layout/height and refresh with a new RemoteViews. | [TodayProvider](/en/api/todayprovider/) |
| Data is old. | changed after worker completion; older results cannot overwrite newer ones. | Fix completion notification/request ordering; do not wait in query. | [TodayProvider](/en/api/todayprovider/) |
| Duplicate SDK classes. | Exactly one AAR or source integration. | Remove the duplicate integration. | [Getting started](/en/getting-started/) |
| java.time fails on API14. | Core-library desugaring enabled and backport DEX present. | Inspect consumer build settings and APK. | [Getting started](/en/getting-started/) |
| Pack installation fails. | pack.json, mappings, safe paths, counts and sizes meet the contract. | Inspect IOException and correct pack data. | [WmResourcePack](/en/api/wmresourcepack/) |
| Font alignment differs. | WmFonts measurement/drawing, bold source, hybrid and single scaling. | Remeasure with matching face/size and remove duplicate scaling. | [WmFonts](/en/api/wmfonts/) |
| Start/theme differs. | Launcher visibility, STATE, theme template and revision. | Check host availability/revision and reapply changed artwork. | [WmShell](/en/api/wmshell/) |

## Documentation site check

Run from the SDK source archive or SDK repository root containing `sdk/docs/`. Node.js ≥22.12.0 and npm are required. `npm ci` installs the lockfile dependencies and requires registry access unless they are cached.

```sh
cd sdk/docs
npm ci
npm run build
```

For live editing:

```sh
npm run dev -- --host 127.0.0.1 --port 8000
```

Search is unavailable in the dev server. After `npm run build`, verify search in the built-site preview:

```sh
npm run preview -- --host 127.0.0.1 --port 8001
```

Check both languages: task navigation, class-reference links, code blocks, tables, and search results. Keep API identifiers unchanged in both translations.

<div data-pagefind-ignore>

In Korean, search `리소스팩`, `애플릿`, `시작하기`, `글꼴`, and `생성자` for relevant guides and APIs. Search `WmResources.assetFile`, `createWidget`, `WmEditableChoice.bindDropdown`, and `toRrule` in both languages: they should open the method sections of WmResources, TodayProvider, WmEditableChoice, and WmRecurrenceRule respectively. Confirm results use the current language without duplicate pages, then switch language and repeat the identifiers.

</div>
## Machine-readable contracts

Each document's HTML head links an English brief summary and complete compact reference. [llms.txt](/llms.txt) indexes these text files; [llms-full.txt](/llms-full.txt) contains all records. Korean pages link the corresponding English record. The 404 page has its own English record.

Brief summaries use each page's authored description. Complete references preserve declarations, parameters, conditions, negations, returns, exceptions, units, versions, and examples; only whitespace and display-only HTML wrappers are compacted. A build check rejects content loss. These files are not displayed in the normal page body and do not control an LLM's reading order.

## Publication configuration

Local builds default to `base=/` and do not require a public site URL. Set `WMDROID_DOCS_SITE` to the confirmed public origin and `WMDROID_DOCS_BASE` to the actual project path, or `/` for a root site. The Astro configuration reads these values; Markdown links/images, breadcrumbs, and contract links follow the base. Repository names and release URLs are not yet fixed.

A local project-path check needs no public URL:

```sh
WMDROID_DOCS_BASE=/documentation-check/ npm run build
```

Before publication, verify internal links, locale switching, search, and redirects under the chosen base. A build alone does not verify deployed behavior.
