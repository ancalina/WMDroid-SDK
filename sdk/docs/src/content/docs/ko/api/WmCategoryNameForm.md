---
title: "WmCategoryNameForm"
slug: ko/api/wmcategorynameform
---
```java
public final class WmCategoryNameForm extends FrameLayout implements WmControls.LanguageListener,WmControls.RestyleListener,WmControls.BackHandler
public WmCategoryNameForm(Context context, Typeface regular, Typeface bold, int accent)
public EditText nameField()
public void showNameWarning(Runnable acknowledged)
public void onWmLanguageChanged(String language)
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public boolean back()
```

새 범주 본문. `regular`와 `bold`는 non-null이어야 한다. 소유자가 앱 제목, 완료·취소 명령, 검증과 저장을 담당한다. 연결 시 입력 포커스와 소프트 키보드를 요청한다. 분리 시 키보드 숨김을 요청한다.

`nameField()`는 한 줄 편집기를 반환한다. 입력 한도는 UTF-16 코드 단위 255개다. 원문을 trim·검증·저장하지 않는다. 언어·테마 변경은 입력과 선택 범위를 유지한다. 제목 색은 `accent`를 따른다. 제목과 라벨은 bold 역할을 사용한다.

| 상수 | 값 | 컨트롤 |
|---|---:|---|
| `HEADING` | 32850 | 새 범주 본문 제목 |
| `LABEL` | 25960 | 범주 라벨 |
| `NAME` | 24033 | 이름 편집기 |
| `NAME_LIMIT` | 255 | 입력 한도 |

좌표는 논리 VGA 픽셀이다. 화면 전체를 한 번 배율 변환한다.

| 요소 | x, y | 폭 × 높이 — 480px 기준 |
|---|---|---|
| 본문 제목 | 8, 10 | 420 × 36 |
| 구분선 | 0, 50 | 480 × 2 |
| 라벨 | 8, 72 | 120 × 40 |
| 편집기 | 132, 72 | 340 × 40 |

구분선은 본문 폭을 채운다. 편집기는 오른쪽 여백 8px을 유지한다. 편집기 테두리와 구분선은 공식 USA WM6.1.4 에뮬레이터에서 측정했다. 제목·라벨 사각형은 측정한 글자 영역에 템플릿을 투영한 값이다. 글꼴 크기 24/26은 Android 이식 값이다. GDI 래스터와 한국어·OEM 기하 구조는 미검증이다.

원본 새 범주 흐름에서는 빈 값이나 ASCII 공백만 있는 값의 완료는 삽입 없이 닫힌다. 이미 있는 이름도 체크를 바꾸지 않고 닫힌다. 다른 이름은 [WmCategoryPicker.addCategory](/ko/api/wmcategorypicker/)로 추가한다. 잘못된 입력은 새 범주 창을 유지한다. 취소는 입력을 버린다. 폼 자체는 이 소유자 동작을 수행하지 않는다.

`showNameWarning()`는 쉼표·세미콜론 경고를 표시한다. 입력을 검증하지 않는다. 경고는 하나만 열린다. 입력과 소유자 명령을 차단한다. OK 또는 `back()`은 경고를 제거하고 선택 범위를 복원한 뒤 nullable 콜백을 한 번 호출한다. 분리는 콜백 없이 경고를 제거한다. Activity 재생성 시 오류 상태 저장·복원은 소유자가 담당한다. 경고 배치는 공통 MessageBox 이식 구현이며 원본 GDI 일치는 미검증이다.

열린 경고가 없으면 `back()`은 false를 반환한다.

UI thread에서 생성·변경하세요. 전체 폼의 언어는 `WmControls.localizeTree(form, language)`, font·색은 `WmControls.restyleTree(form, regular, bold, selection, tabColor)`로 갱신합니다. onWmLanguageChanged는 작성 문구·입력 설명을 갱신합니다. onWmRestyle은 제목·라벨의 bold font와 제목 색을 갱신하므로 입력 필드까지 갱신하려면 트리 경로를 사용하세요.
