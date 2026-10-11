---
title: "API 참조"
slug: ko/api
---

`me.ancal.wmdroid.sdk`

목적에 맞는 클래스를 선택하고 개별 참조에서 정확한 시그니처·제약을 확인하세요. 빌드 설정은 [시작하기](/ko/getting-started/)에 있습니다.

| 목적 | 시작 클래스 |
| --- | --- |
| 애플릿 생성 또는 창 구성 | [WmApplet](/ko/api/wmapplet/), [WmWindow](/ko/api/wmwindow/), [WmViewport](/ko/api/wmviewport/) |
| Today 데이터와 사용자 정의 내용 제공 | [TodayProvider](/ko/api/todayprovider/), [TodayContract](/ko/api/todaycontract/), [WmToday](/ko/api/wmtoday/) |
| Android View 스타일과 실시간 변경 처리 | [WmControls](/ko/api/wmcontrols/), [WmFonts](/ko/api/wmfonts/), [WmStrings](/ko/api/wmstrings/) |
| 날짜·약속·되풀이 초안 편집 | [WmDateField](/ko/api/wmdatefield/), [WmAppointmentEditor](/ko/api/wmappointmenteditor/), [WmRecurrenceEditor](/ko/api/wmrecurrenceeditor/) |
| 연락처·문서·미디어 표시 | [WmContactsView](/ko/api/wmcontactsview/), [WmNotesView](/ko/api/wmnotesview/), [WmMediaView](/ko/api/wmmediaview/) |
| 팩 설치 또는 카탈로그 조회 | [WmResources](/ko/api/wmresources/), [WmResourcePack](/ko/api/wmresourcepack/), [WmAssets](/ko/api/wmassets/) |

## 셸과 창

- [WmApplet](/ko/api/wmapplet/): 공유 제목바·명령바·시작 메뉴를 가진 Activity 기본 클래스입니다.
- [WmShell](/ko/api/wmshell/): 런처의 외형·시작 메뉴·리소스 상태를 조회하고 shell 명령 Intent를 만듭니다.
- [WmWindow](/ko/api/wmwindow/): 제목·client·하단 명령·팝업을 하나의 논리 화면에 배치하는 컨테이너.
- [WmViewport](/ko/api/wmviewport/): 한 논리 window를 물리 화면에 균일 배율로 배치합니다.
- [WmTitleBar](/ko/api/wmtitlebar/): 시작 영역·상태 슬롯·시계·X/OK를 결합하는 논리 VGA 제목줄.
- [WmBar](/ko/api/wmbar/): 제목바·하단 명령바와 버튼 배경을 그리는 Drawable.
- [WmCommandBar](/ko/api/wmcommandbar/): 좌우 명령과 중앙 입력기 영역을 배치하는 하단바입니다.
- [WmStartMenu](/ko/api/wmstartmenu/): 폴더·프로그램 항목을 스크롤 메뉴로 표시하고 선택을 전달하는 View.
- [WmStartBackground](/ko/api/wmstartbackground/): 시작 메뉴 Bitmap을 반복하고 검은 테두리를 그리는 Drawable.
- [WmTraySlot](/ko/api/wmtrayslot/): 제목줄 상태 아이콘의 이미지·터치 영역·연결 애니메이션을 묶는 슬롯.
- [WmTrayClock](/ko/api/wmtrayclock/): 제목줄용 디지털/아날로그 시계.
- [WmStatusPanel](/ko/api/wmstatuspanel/): Android 연결·음량·배터리·전화 상태 팝업 본문.
- [WmClockPanel](/ko/api/wmclockpanel/): 날짜·시각·배터리·소유 앱이 제공한 일정 두 줄을 표시하는 읽기 전용 팝업 본문.
- [WmCloseGlyph](/ko/api/wmcloseglyph/): 제목바의 OK 또는 X 그림과 눌림 색을 제공합니다.
- [WmSipButton](/ko/api/wmsipbutton/): 본문의 활성 editor에 연결된 입력기 toggle·picker 버튼입니다.
- [WmPointer](/ko/api/wmpointer/): 논리 화면 안에서 상대 이동·탭 클릭을 제공하는 핑거마우스 View입니다.
- [WmDevice](/ko/api/wmdevice/): 비행 모드·SIM·통신 서비스·활성 네트워크 상태를 읽습니다.

## Today 계약과 배치

- [TodayContract](/ko/api/todaycontract/): 외부 Today provider와 런처가 공유하는 v1 URI·열·metadata·custom view 규격.
- [TodayProvider](/ko/api/todayprovider/): 캐시된 공개 데이터를 Today 행·선택적 RemoteViews로 제공하는 ContentProvider 기본 클래스.
- [WmToday](/ko/api/wmtoday/): Today 모듈 배치·행 텍스트·일정 선택·RemoteViews용 raster 변환.

## 공통 컨트롤

- [WmControls](/ko/api/wmcontrols/): 기본 Android View에 공통 글꼴·테마·언어·버튼·modal 동작을 적용하는 함수.
- [WmChoice](/ko/api/wmchoice/): caller 소유 팝업과 연결하는 닫힌 단일 선택 필드입니다.
- [WmEditableChoice](/ko/api/wmeditablechoice/): 직접 입력·후보 선택·caller 소유 팝업을 결합한 필드입니다.
- [WmCategoryPicker](/ko/api/wmcategorypicker/): 범주 체크 목록 초안과 새 이름 검증. 호출자가 catalog·확인·취소·저장을 관리한다.
- [WmCategoryNameForm](/ko/api/wmcategorynameform/): 측정한 새 범주 입력칸·본문 제목·구분선. 소유자가 확인과 저장을 담당한다.
- [WmBoxSelect](/ko/api/wmboxselect/): 텍스트 셀의 단일·다중 선택을 bit mask로 관리합니다.
- [WmThreeStateCheck](/ko/api/wmthreestatecheck/): 미선택·선택·부분 선택 상태를 순환하는 CheckBox입니다.
- [WmScrollView](/ko/api/wmscrollview/): 본문의 finger scroll과 WM 스크롤바를 함께 배치합니다.
- [WmScrollBar](/ko/api/wmscrollbar/): Host의 픽셀 위치를 조작하는 세로 스크롤바입니다.
- [WmTabStrip](/ko/api/wmtabstrip/): 본문 아래 탭과 너비 초과 시 이동 화살표를 배치합니다.
- [WmTabDrawable](/ko/api/wmtabdrawable/): 하단 탭의 선택·비선택 배경과 경계를 그립니다.
- [WmPropertySheet](/ko/api/wmpropertysheet/): 보존된 페이지와 하단 탭·선택·Apply/Cancel gate를 관리합니다.
- [WmListSettingsPane](/ko/api/wmlistsettingspane/): 목록과 오른쪽 명령 열을 배치하는 설정 컨테이너입니다.
- [WmSettingsView](/ko/api/wmsettingsview/): 소유 앱이 제공한 카테고리·아이콘 메타데이터로 설정 탭과 아이콘 그리드를 만든다.
- [WmIconCaption](/ko/api/wmiconcaption/): 아이콘 목록의 텍스트 잉크 영역에 선택 사각형을 그리는 TextView.

## 메뉴와 대화상자

- [WmMenuItem](/ko/api/wmmenuitem/): 명령·체크·구분선·하위 메뉴의 불변 데이터입니다.
- [WmCommandMenu](/ko/api/wmcommandmenu/): 하단 명령에 연결된 스크롤·하위 메뉴 팝업입니다.
- [WmRadioMenu](/ko/api/wmradiomenu/): 명령·radio·선택 목록의 행과 키보드 동작을 제공합니다.
- [WmPopup](/ko/api/wmpopup/): 꼬리 있는 팝업의 제목·본문을 배치합니다.
- [WmBubble](/ko/api/wmbubble/): 말풍선 팝업의 흰 본문·gradient 제목·꼬리·외곽선을 그립니다.
- [WmDialog](/ko/api/wmdialog/): 포커스에 맞춰 기본 버튼을 선택하고 Enter·Escape를 처리하는 내장 대화상자 컨테이너입니다.
- [WmMessageBox](/ko/api/wmmessagebox/): 완료 알림 또는 예·아니요 선택을 받는 modal 대화상자.
- [WmInputDialog](/ko/api/wminputdialog/): 단일 이름 입력과 완료·취소 명령을 가진 전체 client 대화상자입니다.
- [WmWizard](/ko/api/wmwizard/): 보존된 단계 페이지와 취소·뒤로·다음·마침 명령을 관리합니다.

## 글꼴·리소스·표시

- [WmMetrics](/ko/api/wmmetrics/): 배율 변환 전 공통 컨트롤의 논리 픽셀 치수와 높이 계산.
- [WmFonts](/ko/api/wmfonts/): 글꼴 로드·하이브리드 fallback·TextView 연결·Canvas 텍스트 측정을 제공합니다.
- [WmFontFile](/ko/api/wmfontfile/): 가져온 글꼴을 SHA-256 파일명으로 저장하고 런처의 글꼴 URI를 해석합니다.
- [WmResources](/ko/api/wmresources/): 앱의 선택 리소스팩·런처 snapshot·asset fallback을 해석합니다.
- [WmResourcePack](/ko/api/wmresourcepack/): version 2 ZIP 리소스팩을 검증·설치하고 불변 snapshot을 제공합니다.
- [WmAssets](/ko/api/wmassets/): 리소스팩의 카탈로그 항목을 조회하고 원본 파일·미리보기를 엽니다.
- [WmIcons](/ko/api/wmicons/): 상태 아이콘을 활성 리소스팩 또는 내장 대체 그림에서 불러옵니다.
- [WmAppIcons](/ko/api/wmappicons/): Android 앱 아이콘을 정사각형 Bitmap으로 렌더링합니다.
- [WmIconPolicy](/ko/api/wmiconpolicy/): Activity별 adaptive icon 처리 허용·숨김 목록을 해석하는 값 객체.
- [WmLogo](/ko/api/wmlogo/): 시작 버튼 artwork를 bounds에 맞춰 표시하는 Drawable.
- [WmVectorDrawable](/ko/api/wmvectordrawable/): 팩의 v1 vector JSON을 Drawable로 읽고 bounds에 맞춰 그립니다.
- [WmStrings](/ko/api/wmstrings/): i18n JSON 카탈로그의 문자열과 언어 fallback을 조회합니다.
- [WmColor](/ko/api/wmcolor/): WM 계열 HSB 색 변환과 기존 Bitmap의 색조 변경.
- [WmPalette](/ko/api/wmpalette/): 선택색에서 highlight·control face 색을 얻고 ARGB를 보간하는 함수.
- [WmPaint](/ko/api/wmpaint/): 옛 기기 동작 설정에 따라 client 영역을 위에서 아래로 표시하는 컨테이너.
- [WmTouchFeedback](/ko/api/wmtouchfeedback/): 팩의 soft/loud touch WAV를 비동기 SoundPool로 재생.
- [WmTouchSoundOptions](/ko/api/wmtouchsoundoptions/): touch sound 활성화와 soft/loud 선택 컨트롤.
- [WmVolumeFeedback](/ko/api/wmvolumefeedback/): 음량 조절 단계에 따라 media·ring preview 소리를 재생.

## 일정·날짜·시간

- [WmCalendarView](/ko/api/wmcalendarview/): 일/5일/6일/주 시간표와 선택 상세를 표시한다.
- [WmCalendarHeader](/ko/api/wmcalendarheader/): Calendar의 보기별 날짜 도구 모음.
- [WmAgendaView](/ko/api/wmagendaview/): 하루 약속을 종일 목록·시각 스트립·시간순 두 줄 목록으로 표시한다.
- [WmMonthView](/ko/api/wmmonthview/): Calendar 월 보기의 42칸 날짜·선택 효과·일정 장식을 표시한다.
- [WmLunarCalendar](/ko/api/wmlunarcalendar/): 선택적 리소스팩 데이터로 한국 음력 날짜와 캡션을 구한다.
- [WmMonthCalendar](/ko/api/wmmonthcalendar/): 날짜 선택 팝업의 42칸 달력 몸체.
- [WmMonthCalendarHeader](/ko/api/wmmonthcalendarheader/): 날짜 드롭다운의 월·연도 제목과 좌우 버튼.
- [WmYearView](/ko/api/wmyearview/): Calendar 연도 보기.
- [WmYearChooser](/ko/api/wmyearchooser/): 1900..2999 연도를 입력하는 소유자 삽입형 dialog View.
- [WmCalendarPicker](/ko/api/wmcalendarpicker/): 월 제목·달력·월 목록·인라인 연도 편집을 결합한 날짜 드롭다운.
- [WmDateField](/ko/api/wmdatefield/): nullable 날짜 전용 분할 입력 View.
- [WmDateTimeField](/ko/api/wmdatetimefield/): 날짜 분할 필드와 편집 가능한 시각 콤보를 결합한다.
- [WmTimeFormat](/ko/api/wmtimeformat/): 일정 설명과 분 단위 시각 문구를 만든다.
- [WmTaskEditor](/ko/api/wmtaskeditor/): nullable 날짜·우선 순위·완료·범주·보안 수준을 갖춘 작업/메모 편집기.
- [WmAppointmentEditor](/ko/api/wmappointmenteditor/): 약속/메모 두 페이지 편집 카드.
- [WmAppointmentText](/ko/api/wmappointmenttext/): 약속 저장 경계에서 제목의 NUL 종료와 빈 제목 대체를 처리한다.
- [WmAppointmentReminder](/ko/api/wmappointmentreminder/): 약속의 상대 알림 사용 콤보와 수량·단위를 표시한다.
- [WmReminder](/ko/api/wmreminder/): 상대 약속 알림의 불변 값이다.
- [WmCalendarDuration](/ko/api/wmcalendarduration/): 약속 편집기의 지속 시간과 CalendarProvider `DURATION`을 변환한다.
- [WmCalendarSymbols](/ko/api/wmcalendarsymbols/): 약속 metadata 심볼 Drawable.
- [WmCalendarGeneralOptions](/ko/api/wmcalendargeneraloptions/): 일정의 시작 보기·첫 요일·주 열 수·30분 행·주 번호 설정 페이지.
- [WmCalendarAppointmentsOptions](/ko/api/wmcalendarappointmentsoptions/): 새 약속의 기본 알림·표시 심볼·회의 요청 계정을 설정한다.
- [WmRecurrenceRule](/ko/api/wmrecurrencerule/): 반복 유형과 종료 조건을 보존하는 불변 값이다.
- [WmRecurrenceChoice](/ko/api/wmrecurrencechoice/): 시작 날짜에 맞춘 한 번/매주/매월/매년 preset과 유형 편집 명령의 콤보.
- [WmRecurrenceEditor](/ko/api/wmrecurrenceeditor/): Provider 없는 반복 편집 마법사.

## 전화와 연락처

- [WmPhoneKeypad](/ko/api/wmphonekeypad/): 4×4 전화 키패드.
- [WmPhoneView](/ko/api/wmphoneview/): 번호 입력·상태·음성 사서함·키패드 클라이언트.
- [WmSpeedDialView](/ko/api/wmspeeddialview/): 단축 다이얼 슬롯과 연락처 이름 목록.
- [WmContactsView](/ko/api/wmcontactsview/): 검색·영문 첫 글자 그룹·연락처 목록을 표시한다.
- [WmContactSummaryView](/ko/api/wmcontactsummaryview/): 연락처 이름·회사·직함과 전화/링크/주소 작업, 읽기 전용 메모 탭을 표시한다.
- [WmContactDetailsView](/ko/api/wmcontactdetailsview/): 연락처 초안을 자세히·메모 탭으로 편집한다.
- [WmContactSubcardView](/ko/api/wmcontactsubcardview/): 연락처 이름 또는 주소를 구성 요소별로 입력한다.

## 문서와 미디어

- [WmNotesView](/ko/api/wmnotesview/): 문서 목록·폴더 필터·정렬과 일반 텍스트 편집 클라이언트.
- [WmPwiDocument](/ko/api/wmpwidocument/): 일반 텍스트 저장형과 복구된 binary PWI 텍스트 record를 읽는 불변 모델.
- [WmPwiLayout](/ko/api/wmpwilayout/): 복구한 RichInk의 좌표 변환·들여쓰기·줄 위치 계산.
- [WmPwiTextLayout](/ko/api/wmpwitextlayout/): PWI 텍스트 토큰의 native 줄바꿈.
- [WmPwiView](/ko/api/wmpwiview/): 읽기 전용 binary PWI 텍스트 뷰.
- [WmDocumentFolders](/ko/api/wmdocumentfolders/): 평면 문서 폴더 목록과 생성·이름 변경·삭제 명령.
- [WmDocumentProperties](/ko/api/wmdocumentproperties/): 문서 이름 바꾸기·폴더/저장 위치 이동 폼.
- [WmBrowserView](/ko/api/wmbrowserview/): 주소 입력줄과 Android `WebView`를 묶은 브라우저 클라이언트.
- [WmBrowserFavoriteForm](/ko/api/wmbrowserfavoriteform/): 즐겨찾기 이름·주소·저장 위치를 입력하는 폼.
- [WmBrowserFolderForm](/ko/api/wmbrowserfolderform/): 즐겨찾기 폴더 이름 입력 폼.
- [WmBrowserFavoritesView](/ko/api/wmbrowserfavoritesview/): 폴더와 링크를 같은 목록에서 표시한다.
- [WmMediaView](/ko/api/wmmediaview/): Android MediaPlayer 기반 오디오/비디오 클라이언트.
- [WmMediaQueue](/ko/api/wmmediaqueue/): 중복 소스를 허용하는 순서형 재생 큐.
- [WmMediaLibraryView](/ko/api/wmmedialibraryview/): 소유 앱의 미디어 계층을 제목·아이콘 행으로 표시한다.
- [WmMediaPlaylistView](/ko/api/wmmediaplaylistview/): 지금 재생 목록, 선택 행, 위/아래 이동·추가·제거·속성 툴바.
- [WmMediaPropertiesView](/ko/api/wmmediapropertiesview/): 미디어 메타데이터 스냅샷을 레이블/값 쌍으로 표시한다.

## 내부 구현

다음 클래스는 package-private입니다. 연결된 public API를 사용하세요.

- [WmCharacterSearch](/ko/api/wmcharactersearch/): 선택 필드·팝업 목록의 키보드 문자 검색 상태를 공유하는 package-private 구현입니다.
- [WmComboGlyph](/ko/api/wmcomboglyph/): 선택 필드의 화살표·팝업 연결·키 전달을 관리하는 package-private 구현입니다.
- [WmDateFormats](/ko/api/wmdateformats/): package-private 날짜 형식 구현체.
- [WmIconOrder](/ko/api/wmiconorder/): 내부 아이콘 목록 삽입 비교기.
- [WmMonthYearEditor](/ko/api/wmmonthyeareditor/): package-private 인라인 연도 편집 구현체.

## 비슷한 이름의 컨트롤

| 목적 | API |
| --- | --- |
| 일정 월 보기와 일정 장식 | [WmMonthView](/ko/api/wmmonthview/) |
| 날짜 선택 팝업의 42칸 본문 | [WmMonthCalendar](/ko/api/wmmonthcalendar/) |
| 월 제목·달력·월 목록·연도 입력을 묶은 선택기 | [WmCalendarPicker](/ko/api/wmcalendarpicker/) |
| 고정 선택·직접 입력·셀 선택 | [공통 UI 선택 기준](/ko/ui/#컨트롤-선택) |
