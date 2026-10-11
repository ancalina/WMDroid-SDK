---
title: "WmControls"
slug: ko/api/wmcontrols
---
[API 색인](/ko/api/)

```java
public final class WmControls
```

Android View의 글꼴·테마·작성 문구와 modal 입력을 관리한다. 생성·binding·traversal은 UI 스레드에서 호출한다. 치수는 [WmMetrics](/ko/api/wmmetrics/)의 논리 픽셀이다. 스타일은 View별 weak key에 저장하며 같은 View의 후속 호출에 유지된다. Binding setter는 같은 View의 기존 text/hint/description도 전달한 language로 다시 반영한다. 기존 format binding의 예외는 새 hint/description 등록 후 발생할 수 있다.

## 사용 준비

### 상수

```java
public static final int ROW_HEIGHT = 44;
public static final int TAB_HEIGHT = WmMetrics.TAB;
public static final int SURFACE = Color.WHITE;
public static final int FACE = 0xffc9e7e9;
public static final int SHADOW = 0xff808080;
public static final int FRAME = Color.BLACK;
```

ROW_HEIGHT/TAB_HEIGHT는 논리 픽셀의 기본 행/탭 높이이며 나머지는 기본 ARGB 색이다. 라이브 테마에는 고정 FACE 대신 `WmPalette.face(selection)`을 사용한다.

## 문구 연결·언어 갱신

### bindText

```java
public static void bindText(TextView view,String language,String authored);
public static void bindText(TextView view,String language,String authored,String key);
```

작성 문구를 번역해 즉시 표시하고 이후 `localizeTree`에 사용할 binding을 저장한다. key가 null이면 원문으로, 그 외에는 [WmStrings](/ko/api/wmstrings/)의 문맥 key로 조회한다. 기존 format binding은 해제한다. authored null 또는 EditText는 변경 전 `IllegalArgumentException`. 사용자 입력·provider 값은 이 binding에 등록하지 않는다.

### bindFormat

```java
public static void bindFormat(TextView view,String language,String pattern,Object... values);
```

번역한 pattern에 `String.format`을 적용합니다. Locale은 `WmStrings.language(view.getContext(), language)`로 선택한 앱 카탈로그 언어에서 만듭니다. 요청 태그가 없으면 상위 언어 또는 ko로 fallback한 Locale입니다. EditText 또는 null pattern/values는 `IllegalArgumentException`; 잘못된 format은 `IllegalFormatException`. 처음 format 검증은 기존 binding 교체 전 수행한다. 이후 언어 변경에서 다른 번역의 format이 잘못되면 다시 예외가 발생할 수 있다.

values 배열은 얕게 복사하며 요소는 그대로 유지하고 번역하지 않는다. 새 `bindText`/`bindFormat`까지 요소 참조를 보관한다. 요소가 binding 대상 View를 직접/간접 참조하면 weak-key 스타일 저장소에서도 해당 View의 수거를 막을 수 있다.

### bindHint

```java
public static void bindHint(TextView view,String language,String authored);
```

작성 placeholder의 binding을 저장하고 번역한 hint를 즉시 반영한다. authored null은 `IllegalArgumentException`. EditText에도 사용 가능하며 입력값·caret·listener는 변경하지 않는다.

### bindDescription

```java
public static void bindDescription(View view,String language,String authored);
```

작성 접근성 설명의 binding을 저장하고 번역한 contentDescription을 즉시 반영한다. authored null은 `IllegalArgumentException`.

### localizeTree

```java
public static void localizeTree(View root,String language);
```

null root는 무동작. root의 등록된 text/hint/description을 먼저 갱신하고 자식을 index 순서로 처리한 뒤 해당 View의 `LanguageListener`를 호출한다. 숨김/비활성 View도 처리한다. 같은 문구이면 setter를 생략한다. 등록된 tab의 명시적 width가 0 이상이면 번역한 문구로 너비를 다시 계산한다.

등록되지 않은 문구와 EditText 입력은 유지한다. callback/format 예외는 전파되며 이미 갱신한 View를 되돌리지 않는다. callback에 전달하는 language는 호출 인수 그대로이다.

## 컨트롤 생성·스타일

### text

```java
public static TextView text(Context context,Typeface font,CharSequence value);
```

새 TextView에 [textStyle](#textstyle)을 적용한 뒤 value를 설정하여 반환한다. text binding은 생성하지 않으므로 언어 전환이 필요하면 별도로 등록한다.

### textStyle

```java
public static void textStyle(TextView view,Typeface font);
```

font를 적용하고 [WmFonts.attach](/ko/api/wmfonts/)로 등록한다. 등록된 bold 역할은 font.isBold()로 결정하며 null font는 regular 역할이다. 크기는 `WmMetrics.FONT` 픽셀, gravity는 CENTER_VERTICAL, includeFontPadding=false, line spacing은 0/1이다. 일반 문구는 검정, disabled는 SHADOW이며 기본 터치음은 끈다.

API ≤15에서는 EditText가 아닌 non-Spannable 문구를 SPANNABLE buffer로 바꾼다. API ≥21은 font feature "kern", API ≥23은 break strategy 0을 설정한다. API ≥26은 Android 기본 focus highlight overlay를 끄며 SDK가 그리는 focus 표시는 유지한다. minimum size·padding·tint·기존 input/check/button/entry/tab 역할은 초기화하지 않는다.

### input

```java
public static void input(EditText view,Typeface font);
```

[textStyle](#textstyle) 후 input 역할을 등록한다. SURFACE 배경과 FRAME 색 `WmMetrics.BORDER` 테두리, padding (4,2,4,2)를 적용하고 모든 minimum width/height를 0으로 설정한다. API ≥21은 background tint를 해제한다. 입력값·input type·listener는 교체하지 않는다.

### check

```java
public static void check(CheckBox view,Typeface font);
```

[textStyle](#textstyle) 후 check 역할과 SDK 체크 표시를 적용한다. indicator의 intrinsic 크기는 `WmMetrics.INDICATOR`; checked 상태는 유지한다. 배경을 투명으로 바꾸고 padding/minimum size를 초기화한 뒤 indicator와 문구 사이 간격을 확보한다. 간격은 `WmMetrics.EDGE+round(measure("0"))/2`의 정수 계산이며 기존 compound padding의 부족분만 추가한다. API ≥21은 button tint를 해제한다. listener는 유지한다.

### listCheck

```java
public static void listCheck(CheckBox view,Typeface font);
```

[textStyle](#textstyle) 후 두 상태의 목록 indicator를 적용한다. intrinsic cell은 32×32 논리 픽셀이고 검정/흰색 glyph는 28×28이며 각 변에 2픽셀 여백이 있다. 일반 [check](#check)는 30×30 indicator(`WmMetrics.INDICATOR`)를 사용한다.

checked 상태와 listener는 유지한다. 배경을 투명으로 바꾸고 padding/minimum size를 초기화한 뒤 32픽셀 예약에 필요한 compound padding의 부족분만 추가한다. font에서 계산한 문구 간격은 추가하지 않으며 restyle에서도 같다. API ≥21은 button/background tint와 state list animator를 해제한다.

checked만 indicator 이미지를 바꾼다. enabled/pressed/focused 상태는 검정/흰색 그림을 바꾸지 않는다. disabled 문구는 textStyle을 따르며 클릭 동작은 Android CheckBox를 따른다. enabled 상태·행 높이·LayoutParams는 바꾸지 않는다.

### radio

```java
public static void radio(android.widget.RadioButton view,Typeface font);
```

[check](#check)와 같은 문구/간격 규칙을 사용하며 SDK 라디오 표시를 적용한다. checked 상태와 listener는 유지한다. API ≥21은 button/background tint와 state list animator를 해제한다.

### tabWidth

```java
public static int tabWidth(TextView view);
```

현재 Paint와 문구를 [WmFonts.measure](/ko/api/wmfonts/)로 측정한 `ceil(width)+32` 논리 픽셀을 반환한다. font나 문구 변경 후 다시 계산하며 LayoutParams를 직접 바꾸지는 않는다.

### tab

```java
public static void tab(TextView view,boolean selected,int tabColor);
```

tab 역할을 등록하고 [WmTabDrawable](/ko/api/wmtabdrawable/) 배경·selected 상태·터치음 꺼짐을 적용한다. selected 값이 달라지고 부모가 WmTabStrip이면 strip에 변경을 통지한다. 너비·click listener를 설정하거나 형제 선택을 해제하지 않는다.

### background

```java
public static StateListDrawable background(int normal,int selected);
```

새 StateListDrawable을 반환한다. disabled는 다른 상태보다 우선하여 normal을 사용한다. enabled의 pressed/selected/focused는 selected, 그 외는 normal이다. enter/exit fade는 0이며 인수는 ARGB 색이다.

### entry

```java
public static void entry(TextView view,int selection);
```

entry 역할을 등록하며 button/tab 역할을 해제한다. 배경은 [background](#background)의 SURFACE/selection이다. enabled의 pressed/selected/focused 문구는 흰색, 일반은 검정, disabled는 SHADOW이다. 기본 터치음은 끄지만 font·padding·click listener는 설정하지 않는다.

### entryBackground

```java
public static void entryBackground(View view,int selection);
```

SURFACE/selection 상태 배경을 적용하고 이후 restyle 대상에 등록한다. 기본 터치음은 끈다. TextView 문구 색은 변경하지 않는다.

### faceBackground

```java
public static void faceBackground(View view,int selection);
```

`WmPalette.face(selection)` 배경을 적용하고 이후 restyle에서 해당 배경색을 갱신하도록 등록한다.

### button

```java
public static void button(TextView view);
public static void button(TextView view,int selection);
```

push button 역할을 등록하고 entry/tab 역할을 해제한다. 아직 text 역할이 없으면 현재 Typeface로 textStyle을 적용한다. 기본 selection은 `0xff149ca7`. gravity=CENTER, 각 padding=`WmMetrics.EDGE`이며 default button이면 BORDER를 더한다. minimum size와 기본 터치음은 초기화한다. API ≥21은 background tint·state animator·elevation을 해제한다.

일반 배경은 selection 기반 face와 검정 BORDER 테두리이다. enabled pressed는 검정 배경·흰색 문구이며 focus만으로 눌림색이 되지 않는다. disabled 문구는 `0xffbfbfbf`; 강제 pressed여도 테두리는 기본 두께이다. click listener와 기본 Enter 대상은 별도로 설정한다.

### setDefaultButton

```java
public static void setDefaultButton(TextView view,boolean enabled);
```

등록된 push button만 허용하며 그 외/null은 `IllegalArgumentException`. 같은 값이면 무동작. true는 테두리를 2*BORDER로 바꾸고 각 padding에 BORDER를 더하며 false는 그 추가분을 뺀다. disabled 상태의 표시 테두리는 기본 두께이다. enabled 인수는 default 표시이며 View의 활성 상태를 바꾸지 않는다. Enter 대상은 [dialogButtonKey](#dialogbuttonkey) 인수로 별도 지정한다.

### lockFont

```java
public static void lockFont(View view);
```

해당 View의 등록된 text 역할에만 restyle 글꼴 교체 잠금을 설정한다. 자손에 전파하지 않는다. 테마·smoothing과 custom RestyleListener는 계속 처리하며 textStyle의 직접 호출도 막지 않는다. 공개 해제 API는 없다.

### restyleTree

```java
public static void restyleTree(View root,Typeface font,int selection,int tabColor);
public static void restyleTree(View root,Typeface regular,Typeface bold,int selection,int tabColor);
```

null root는 무동작. 현재 View에 smoothing과 등록 스타일을 반영하고 자식을 index 순서로 처리한 뒤 해당 View의 `RestyleListener`를 호출한다. 숨김/비활성 View도 처리한다. 한 font overload는 regular/bold에 같은 값을 사용한다.

잠기지 않은 등록 text의 bold/button 역할은 bold가 null이 아니면 bold, 그 외는 regular를 사용한다. 합성 bold는 만들지 않는다. tab의 명시적 width가 0 이상이면 현재 문구로 다시 계산한다. check/radio의 문구 간격과 highlight도 다시 계산한다. 여러 배경 역할이 겹치면 entryBackground→faceBackground→tab/button/entry 순으로 적용한다. text 역할의 분기 우선순위는 tab→button→entry→check이다.

View 트리·문구 binding·click listener는 교체하지 않는다. 글꼴/너비 변경과 custom callback은 layout·스크롤 결과에 영향을 줄 수 있다. callback 예외는 전파되며 이전 변경을 되돌리지 않는다.

### select

```java
public static void select(View container,View chosen);
```

ViewGroup의 직접 자식 중 chosen과 동일한 객체만 selected=true, 나머지는 false로 설정한다. chosen이 null/직접 자식이 아니면 모두 해제한다. null/non-ViewGroup container는 무동작. 초점·자손 선택·callback은 처리하지 않는다.

## 뒤로 가기·모달·키보드

### back

```java
public static boolean back(View root);
```

null 또는 visibility가 VISIBLE 아닌 root는 false. root가 BackHandler이면 즉시 해당 반환값을 사용하며 false여도 자손을 찾지 않는다. 그 외 ViewGroup은 자식을 역순으로 검색하여 첫 true에서 중단한다. 처리한 항목이 없으면 false. isEnabled나 window/상위 visibility는 확인하지 않으며 callback 예외는 전파한다.

### blocksOwnerCommands

```java
public static boolean blocksOwnerCommands(View root);
```

null 또는 visibility가 VISIBLE 아닌 root는 false. root의 ModalOwner가 true이면 true, 그 외는 자식을 역순 검색하여 첫 차단 항목에서 true를 반환한다. 없으면 false. isEnabled·window/상위 visibility는 검사하지 않는다. 소유자 명령 callback에서 [ownerCommandsAllowed](#ownercommandsallowed)와 함께 사용한다.

### inputMethodOwner

```java
public static View inputMethodOwner(View root);
```

null 또는 visibility가 VISIBLE 아닌 root는 null. root가 blocking ModalOwner이면 지정한 입력 subtree에서 다시 검사한다. 지정값 null/자기 자신/숨김은 null이다. 그 외에는 자식을 역순 검색하여 첫 blocking subtree로 내려가며 차단 항목이 없으면 root 자체를 반환한다.

반환값은 입력 후보 subtree이며 editor나 enabled 상태를 보장하지 않는다. 지정 View가 실제 자손인지 검사하지 않는다. 지정 대상 탐색 중 같은 View를 다시 방문하면 null을 반환하며 아래 페이지 editor로 넘어가지 않는다.

### ownerCommandsAllowed

```java
public static boolean ownerCommandsAllowed(View command);
```

command의 parent chain을 탐색한다. WmWindow보다 먼저 blocking ModalOwner를 만나면 modal 내부 명령으로 true를 반환한다. 먼저 WmWindow를 만나면 해당 window의 ownerCommandsAllowed 결과를 사용하며 둘 다 없으면 true. command는 null 불가이며 null은 `NullPointerException`. command 자신의 ModalOwner 상태·enabled·visibility는 검사하지 않으므로 callback의 다른 사용 조건을 별도로 확인한다.

### isEnterKey

```java
public static boolean isEnterKey(int keyCode);
```

KEYCODE_ENTER 또는 KEYCODE_NUMPAD_ENTER만 true이다. event action/modifier는 받지 않는다.

### dialogButtonKey

```java
public static boolean dialogButtonKey(ViewGroup dialog,KeyEvent event,TextView cancel);
public static boolean dialogButtonKey(ViewGroup dialog,KeyEvent event,TextView cancel,TextView storedDefault);
```

dialog/event null은 `IllegalArgumentException`. Enter/NUMPAD_ENTER/Escape의 DOWN/UP만 처리한다. 다른 키/action, blocking 자식 modal, dialog 소유 범위 밖 focus는 false이다. 세 인수 overload는 storedDefault=null이다.

Escape 대상은 cancel이다. Enter는 focused push button을 사용하며 focus가 dialog 자체 또는 등록된 plain Static text일 때만 storedDefault를 사용한다. Static text는 정확히 TextView 클래스이고 text 역할만 있으며 input/check/button/entry/tab 역할은 없는 경우이다. EditText·다른 focus·null focus는 default로 대체하지 않는다.

대상이 등록된 push button이고 이 dialog 소유 범위에 있으면 true를 반환한다. WmDialog 자식 경계를 넘어 있는 버튼은 제외한다. 대상부터 dialog까지 enabled이며 visibility=VISIBLE이고 취소되지 않은 DOWN일 때만 performClick을 호출한다. disabled/hidden/canceled DOWN 및 UP도 유효 대상이면 클릭 없이 true이다. modifier/repeat count는 제한하지 않으므로 반복 DOWN은 재클릭할 수 있다. click 예외는 전파된다.

## BackHandler

```java
public interface BackHandler
```

### back

```java
boolean back();
```

중첩 컨트롤이 Back을 처리하면 true를 반환한다. [back(root)](#back)는 이 반환값을 즉시 사용한다. false를 반환해도 같은 컨트롤의 자손으로 전달하지 않는다.

## LanguageListener

```java
public interface LanguageListener
```

### onWmLanguageChanged

```java
void onWmLanguageChanged(String language);
```

[localizeTree](#localizetree)가 해당 View의 binding과 자손을 갱신한 뒤 전달한 language로 동기 호출한다. 직접 그리는 작성 UI를 갱신하며 사용자/provider 데이터는 소유자가 유지한다. 예외는 traversal을 중단한다.

## RestyleListener

```java
public interface RestyleListener
```

### onWmRestyle

```java
void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

[restyleTree](#restyletree)가 해당 View와 자손의 등록 스타일 처리 후 원래 인수를 전달하여 동기 호출한다. 직접 그리는 View의 색/geometry 갱신은 구현자가 처리한다. lockFont는 이 callback을 막지 않으며 예외는 traversal을 중단한다.

## ModalOwner

```java
public interface ModalOwner
```

### ModalOwner.blocksOwnerCommands

```java
boolean blocksOwnerCommands();
```

true이면 해당 modal subtree 밖 소유자의 명령을 차단한다. 한 번의 재귀 탐색에서도 여러 번 동기 호출할 수 있으며 반환값을 캐시하지 않는다. modal 자체 컨트롤은 [ownerCommandsAllowed](#ownercommandsallowed)의 ancestor 규칙으로 허용한다.

### ModalOwner.inputMethodOwner

```java
default View inputMethodOwner();
```

기본 반환은 null이며 blocking modal의 아래 페이지 editor 접근을 막는다. 필요하면 modal의 입력 subtree를 지정한다. 공개 [inputMethodOwner(root)](#inputmethodowner)는 지정값에서 다시 modal 탐색을 수행하며 직접 self/숨김/간접 순환은 null을 반환한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmControls.java`
