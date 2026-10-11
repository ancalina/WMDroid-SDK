---
title: "검증과 문제 해결"
slug: ko/verification
---

## 검증 순서

1. 현재 source와 AAR를 같은 빌드에서 준비하세요.
2. ZIP·클래스·manifest·asset 검사를 실행하세요.
3. 독립 AAR 소비 예제를 빌드하세요.
4. API14와 지원하는 최신 API에서 앱을 실행하세요.
5. 글꼴·테마·언어·배율을 앱이 열린 상태에서 바꾸세요.
6. 시작 메뉴, 뒤로 가기, OK/X와 모달 명령을 검사하세요.
7. 위젯을 활성화한 뒤 데이터 변경을 알려 주세요.
8. 앱 재시작과 process 재생성 후 초안을 검사하세요.

다음 검사는 `tools/`와 `sdk/`가 있는 공개 SDK 소스 ZIP의 압축 해제 루트에서 실행합니다.

```sh
python3 tools/check_sdk_aar.py
python3 tools/run_recurrence_rule_checks.py --jdk /path/to/jdk
```

통합 `WMDroid-public-sources.zip`에는 언어 catalog 검사기가 있습니다. 해당 아카이브 루트에서 실행하세요. 별도 런처 소스 ZIP에는 이 도구가 없습니다.

```sh
python3 tools/check_language_catalogs.py
```

`check_today_native_text_profiles.py`는 private ROM/IDA workspace 전용이며 공개 소스 ZIP에 포함되지 않습니다.

시각 동작은 기기에서 비교하세요. 정적 검사는 렌더링을 검사하지 않습니다.

## 문제 해결

| 증상 | 정상 조건·확인 위치 | 다음 조치 | 관련 계약 |
| --- | --- | --- | --- |
| 모듈이 목록에 없습니다. | getAuthority와 manifest authority 동일; exported/enabled=true; VERSION=1; readPermission 없음. | 등록 값을 일치시키고 metadata를 수정한 뒤 재설치합니다. | [TodayProvider](/ko/api/todayprovider/) |
| 모듈이 화면에 없습니다. | 현재 런처에서 해당 Today 모듈 활성화. | 모듈 활성화와 유효한 id/title 행 반환을 확인합니다. | [TodayContract](/ko/api/todaycontract/) |
| 사용자 정의 view가 text로 나옵니다. | CUSTOM_VIEWS=true; 같은 provider 패키지; 높이32–480; RemoteViews 지원 layout. | metadata·layout·반환 높이를 수정하고 새 RemoteViews로 갱신합니다. | [TodayProvider](/ko/api/todayprovider/) |
| 데이터가 오래되었습니다. | worker 완료 후 changed 호출; 오래된 결과가 새 결과를 덮지 않음. | 완료 통지·요청 순서 검사를 추가하고 query에서 대기하지 않습니다. | [TodayProvider](/ko/api/todayprovider/) |
| SDK 클래스가 중복됩니다. | AAR 또는 source 연결 중 하나. | 중복 SDK 연결을 제거합니다. | [시작하기](/ko/getting-started/) |
| API14 java.time이 실패합니다. | core-library desugaring 설정 및 backport DEX 포함. | 소비 앱 빌드 설정·APK를 검사합니다. | [시작하기](/ko/getting-started/) |
| 팩이 설치되지 않습니다. | pack.json·매핑·안전 경로·개수·크기가 계약 만족. | IOException 원인을 확인해 pack 데이터를 수정합니다. | [WmResourcePack](/ko/api/wmresourcepack/) |
| 글꼴 정렬이 다릅니다. | WmFonts 측정/그리기·bold source·hybrid·단일 배율 사용. | 동일 face/size로 재측정하고 중복 배율을 제거합니다. | [WmFonts](/ko/api/wmfonts/) |
| 시작 메뉴·theme이 다릅니다. | launcher visibility·STATE·theme template·revision 정상. | host 가용성·현재 revision을 확인하고 변경된 artwork를 갱신합니다. | [WmShell](/ko/api/wmshell/) |

## 문서 사이트 검사

`sdk/docs/`가 있는 SDK 소스 아카이브 또는 SDK 저장소 루트에서 실행하세요. Node.js ≥22.12.0·npm이 필요합니다. `npm ci`는 lockfile 의존성을 설치하며 캐시가 없으면 registry 접근이 필요합니다.

```sh
cd sdk/docs
npm ci
npm run build
```

실시간 편집:

```sh
npm run dev -- --host 127.0.0.1 --port 8000
```

dev 서버에서는 검색이 작동하지 않습니다. `npm run build` 후 빌드 결과 미리보기에서 검색을 검사하세요.

```sh
npm run preview -- --host 127.0.0.1 --port 8001
```

한·영 모두 목적별 탐색·클래스 참조 링크·코드 블록·표·검색 결과를 확인하세요. 번역에서도 API 식별자를 그대로 유지하세요.

<div data-pagefind-ignore>

검색 창에서 `리소스팩`, `애플릿`, `시작하기`, `글꼴`, `생성자`를 검색하여 관련 가이드·API를 여는지 확인하세요. `WmResources.assetFile`, `createWidget`, `WmEditableChoice.bindDropdown`, `toRrule`은 각각 WmResources·TodayProvider·WmEditableChoice·WmRecurrenceRule의 메서드 절을 열어야 합니다. 현재 언어의 결과만 표시하고 같은 페이지를 중복 표시하지 않는지 확인한 뒤 언어를 전환해 같은 식별자를 다시 검사하세요.

</div>
## 기계 판독용 계약

각 문서의 HTML head는 영문 짧은 요약과 전체 compact reference에 연결합니다. [llms.txt](/llms.txt)는 텍스트 파일 색인이며, [llms-full.txt](/llms-full.txt)는 전체 기록입니다. 한국어 페이지는 대응하는 영문 기록에 연결합니다. 404 페이지에도 영문 기록이 있습니다.

짧은 요약은 각 페이지에 작성된 설명을 사용합니다. 전체 참조는 선언·매개변수·조건·부정·반환·예외·단위·버전·예제를 보존하고, 공백과 표시용 HTML wrapper만 정리합니다. 빌드 검사는 정보 손실을 거부합니다. 이 파일은 일반 화면 본문에 표시되지 않으며, LLM이 읽는 순서를 강제하지 않습니다.

## 공개 사이트 설정

로컬 빌드의 기본값은 `base=/`이며 공개 site URL은 필요하지 않습니다. `WMDROID_DOCS_SITE`에 확정된 공개 origin, `WMDROID_DOCS_BASE`에 실제 프로젝트 경로를 지정하세요. 루트 사이트는 `/`를 사용합니다. Astro 설정이 이 값을 읽으며, Markdown 링크·이미지, breadcrumb, 계약 링크는 base를 따릅니다. 저장소 이름과 릴리즈 URL은 아직 확정되지 않았습니다.

공개 URL 없이 로컬 프로젝트 경로를 검사할 수 있습니다.

```sh
WMDROID_DOCS_BASE=/documentation-check/ npm run build
```

공개 전, 선택한 base에서 내부 링크·언어 전환·검색·redirect를 검사하세요. 빌드만으로 배포된 사이트의 동작이 검증되지는 않습니다.
