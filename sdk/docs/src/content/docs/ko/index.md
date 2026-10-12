---
title: "WMDroid SDK"
slug: ko
---

WM 스타일 Activity와 Today provider를 위한 Android UI 라이브러리입니다. 최소 Android API14. 패키지: `me.ancal.wmdroid.sdk`.

## 연결 방식 선택

| 목적 | 가이드 | 시작 API |
| --- | --- | --- |
| Android 앱에 SDK 연결 | [AAR 생성과 연결](/ko/getting-started/) | AAR 또는 소스 연결 |
| 제목, 시작 메뉴, 하단 명령을 가진 Activity 생성 | [애플릿과 셸](/ko/applets/) | [WmApplet](/ko/api/wmapplet/) |
| 기존 앱에서 오늘 화면에 내용 제공 | [Today 모듈](/ko/widgets/) | [TodayProvider](/ko/api/todayprovider/), [TodayContract](/ko/api/todaycontract/) |
| 앱이 소유한 창에 컨트롤 배치 | [공통 UI](/ko/ui/) | [WmControls](/ko/api/wmcontrols/), [WmWindow](/ko/api/wmwindow/) |
| 글꼴, 카탈로그, 리소스팩 사용 | [글꼴과 리소스](/ko/resources/) | [WmFonts](/ko/api/wmfonts/), [WmResources](/ko/api/wmresources/), [WmAssets](/ko/api/wmassets/) |

앱이 데이터, 권한, 초안 저장을 관리합니다. `WmApplet`은 공통 셸을 제공합니다. Today provider는 기존 Activity를 교체하지 않고 런처에 행을 제공합니다.

[검증과 문제 해결](/ko/verification/)에서 AAR 소비 앱, API14 동작, 실시간 설정, provider 갱신을 검사하세요. 문서 수정의 용어, 정보 보존 기준은 [작성 규칙](/ko/style/)에 있습니다.

AAR, 소스 ZIP은 제공된 artifact checksum과 대조하세요. 로컬 SDK 빌드는 `sdk-build-report.json`도 생성합니다. [시작하기의 대조 절](/ko/getting-started/#배포본-대조)을 확인하세요.
