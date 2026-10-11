---
title: "공통 UI"
slug: ko/ui
---

## 좌표·배율

[WmMetrics](/ko/api/wmmetrics/)는 논리 VGA 좌표를 정의합니다. [WmViewport](/ko/api/wmviewport/)는 전체 표면에 배율을 적용합니다.
자식에 `sp`·기기 배율을 다시 적용하지 마세요. 작은 UI 배율에서는 논리 폭이 480보다 넓어질 수 있습니다. 오른쪽 끝은 현재 폭을 사용하세요.

| 상수 | 논리 픽셀 |
| --- | ---: |
| `WIDTH` | 480 |
| `BAR` | 52 |
| `FONT` / `LINE` | 24 / 30 |
| `INSET` / `BORDER` / `EDGE` | 8 / 2 / 4 |
| `TEXT_LEFT` / `ICON` | 56 / 32 |
| `TODAY_FONT` / `TODAY_LINE` | 23 / 28 |
| `TODAY_MIN` / `TAB` | 40 / 40 |

## 컨트롤 선택

| 필요 기능 | API | 선택 기준 |
| --- | --- | --- |
| 창 구성 | [WmWindow](/ko/api/wmwindow/), [WmTitleBar](/ko/api/wmtitlebar/), [WmCommandBar](/ko/api/wmcommandbar/) | 제목·client·하단 명령을 조합합니다. Activity 셸은 [WmApplet](/ko/api/wmapplet/)을 사용합니다. |
| 탭 | [WmTabStrip](/ko/api/wmtabstrip/), [WmPropertySheet](/ko/api/wmpropertysheet/) | 페이지 유지와 Apply/Cancel 조건에는 속성 시트를 사용합니다. |
| 스크롤 | [WmScrollView](/ko/api/wmscrollview/), [WmScrollBar](/ko/api/wmscrollbar/) | 앱이 pixel-offset Host를 관리하면 scrollbar만 사용합니다. |
| 글자·입력 스타일 | [WmControls](/ko/api/wmcontrols/) | Android text·input·check·radio View에 스타일을 적용합니다. |
| 선택 필드 | [WmChoice](/ko/api/wmchoice/), [WmEditableChoice](/ko/api/wmeditablechoice/) | 고정 선택 또는 직접 입력. caller가 팝업을 소유합니다. |
| 글자 셀 선택 | [WmBoxSelect](/ko/api/wmboxselect/) | 단일·다중 선택을 bit mask로 관리합니다. |
| 명령 메뉴 | [WmMenuItem](/ko/api/wmmenuitem/), [WmCommandMenu](/ko/api/wmcommandmenu/) | 명령·체크·구분선·하위 메뉴를 구성합니다. |
| 꼬리형 팝업 | [WmPopup](/ko/api/wmpopup/), [WmBubble](/ko/api/wmbubble/) | 팝업 컨테이너와 배경 Drawable입니다. |
| 모달 대화상자 | [WmDialog](/ko/api/wmdialog/), [WmMessageBox](/ko/api/wmmessagebox/) | 내장 대화상자 또는 확인/Yes-No 선택을 사용합니다. |

Android 기본 ripple·theme widget을 섞으면 누름 효과·치수가 달라질 수 있습니다. 공유 컨트롤을 우선 사용하세요.

## 최소 화면 조립

[WmApplet](/ko/api/wmapplet/)의 onCreate에서 super 호출 뒤 실행하는 예입니다. 부모의 현재 가용 폭을 사용하므로 480px 폭을 따로 고정하지 않습니다.

```java
android.widget.LinearLayout row = new android.widget.LinearLayout(this);
row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
content.addView(row, new android.widget.LinearLayout.LayoutParams(-1, -2));
android.widget.TextView status = me.ancal.wmdroid.sdk.WmControls.text(
        this, regular, "Ready");
row.addView(status, new android.widget.LinearLayout.LayoutParams(0, -2, 1));
```

직접 입력 없이 하나를 고르면 WmChoice, 입력과 후보 선택을 함께 제공하면 WmEditableChoice, 셀 bit mask 선택에는 WmBoxSelect를 사용하세요. 팝업의 생성·위치·제거는 호출자 책임입니다. [WmEditableChoice 연결 예제](/ko/api/wmeditablechoice/#open--binddropdown--isdroppeddown)의 준비→bindDropdown→부착 순서를 따르세요. 전체 트리 갱신의 callback·상태 보존 제약은 [WmControls.restyleTree](/ko/api/wmcontrols/#restyletree)에서 확인하세요.

## Today 배치

[WmToday](/ko/api/wmtoday/)는 `Layout.register()`·`Layout.arrange()`를 제공합니다.
`WmToday.Row`: `setCaption()`, `setCaptionLines()`, `setAppointments()`.

```text
public Module register(String id, View content, int height, Divider divider);
public void arrange(List<String> preferredOrder, Set<String> hidden);
public void setCaption(CharSequence text);
public void setCaptionLines(CharSequence text);
public void setAppointments(List<Appointment> entries);
```

View 생성·변경은 main thread에서 수행하세요.

- `register()`: module 반환. content의 LayoutParams·배경을 설정합니다. Layout에는 아직 붙이지 않습니다. 같은 ID는 등록 교체.
- `register()` 입력: 비어 있지 않은 ID, null이 아닌 content·divider, height≥0. 위반 시 `IllegalArgumentException`.
- `arrange()`: 지정 순서 적용. 알 수 없는·중복 ID 무시. 새 등록은 뒤에 추가. hidden ID는 숨김.
- `setCaption()`: 일반 행 text 측정·배치.
- `setCaptionLines()`: 명시된 줄 유지. 수평 overflow는 줄바꿈·ellipsis 없이 자름.
- `setAppointments()`: 제목·상세 별도 배치. null·빈 목록·null 항목은 `IllegalArgumentException`.

| 값 | 동작 |
| --- | --- |
| `Divider.BETWEEN` | 마지막 표시 module 아래 선 없음 |
| `Divider.WHEN_FOLLOWED` | `BETWEEN`과 같은 호환 동작 |
| `Divider.ALWAYS` | 마지막 module 아래에도 선 있음 |
| `TextStyle.STANDARD` | 기본 Today text 계약 |
| `TextStyle.NATIVE_LINES` | 오른쪽 gutter 26 논리 픽셀 예약 |

일정 전용 높이를 다른 행 유형에 적용하지 마세요. divider는 module 밖에서 호스트가 관리합니다.

## 스타일·언어·명령

`WmControls`:

```text
public static void restyleTree(View root, Typeface regular, Typeface bold,
                               int selection, int tabColor);
public static void localizeTree(View root, String language);
public static boolean back(View root);
```

`restyleTree()`로 공유 style을 갱신하세요. `localizeTree()`는 연결된 text만 갱신합니다. 앱 데이터·사용자 입력은 번역하지 마세요. 사용자 정의 View는 `RestyleListener`·`LanguageListener`를 구현하세요.

`ModalOwner`는 소유 창 명령을 막습니다. `BackHandler`는 내부 Back을 처리합니다. `back(root)`는 handler를 탐색하고 처리 여부를 반환합니다. Back과 OK를 같은 저장 동작으로 처리하지 마세요.
