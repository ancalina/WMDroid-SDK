---
title: "WmSettingsView"
slug: ko/api/wmsettingsview
---
[API 색인](/ko/api/)

```java
public class WmSettingsView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

소유 앱이 제공한 카테고리, 아이콘 메타데이터로 설정 탭과 아이콘 그리드를 만든다. 설정값 읽기, 쓰기 없음. 각 페이지, 컨트롤을 유지하며 논리 픽셀로 배치한다.

## Item / Category

```java
public static final class Item
public final String key,authoredCaption;
public final Drawable icon;
public Item(String key,String authoredCaption,Drawable icon)
public static final class Category
public final String authoredTitle;
public Category(String authoredTitle,Item[] items)
public Item[] items()
```

Item의 key, caption, Category의 title은 non-null, 비어 있지 않아야 한다. icon과 items 배열, 각 항목도 non-null; 위반은 IllegalArgumentException. Item 필드는 불변 참조이며 Drawable은 소유 앱 소유. Category는 항목 배열을 복사하고 items()도 복사 반환. key는 전체 카테고리에서 고유해야 한다.

## 생성자, Listener

```java
public interface Listener {void open(String key);}
public WmSettingsView(Context context,Typeface font,String language,int selection,int face,Category[] categories,Runnable onTab,Listener open)
```

categories는 non-null, 최소 한 개, null 항목 없음, 전역 key 중복 없음. onTab/open은 필수. 위반은 IllegalArgumentException. 아이콘 클릭은 open(key). 페이지 변경은 onTab 후 부착된 페이지 replay. 모든 페이지를 처음부터 생성한다. 아이콘 64px, 가로 간격 150px, 최소 행 높이 136px; 글꼴이 크면 행 높이 확장. 화면 폭에 따라 열 수 변경, 아이콘은 현재 표시 캡션을 WM 소문자 대응표로 UTF-16 비교해 정렬합니다. 새 캡션의 비교 길이는 최대260 UTF-16 코드 단위이며 동률은 기존 항목 앞에 삽입합니다. Item[] 순서를 그대로 유지하지 않으며 언어 변경 후 번역 캡션으로 다시 정렬합니다. [삽입 비교, NUL 규칙](/ko/api/wmiconorder/)을 확인하세요.

## 페이지 접근, 선택

```java
public WmPropertySheet propertySheet()
public int selectedPage()
public void select(int group)
protected WmPaint createSurface(Context context)
```

propertySheet는 실제 탭 컨테이너 반환. selectedPage는 선택된 0-based 인덱스. select는 property sheet의 활성화 규칙을 사용하며 veto 가능; 잘못된 인덱스는 IndexOutOfBoundsException. createSurface는 페이지 생성 때 호출되는 확장 지점이며 기본 GRID 역할 WmPaint 반환. 서브클래스에서 본문을 교체하지 않고 표면 옵션을 조정할 수 있다.

## setIcon

```java
public void setIcon(String key,Drawable icon)
```

해당 아이콘을 제자리 교체한다. 페이지, 스크롤, 선택, 콜백 유지. null/빈 key, null icon, 모르는 key는 IllegalArgumentException. 메타데이터 Item.icon은 변경하지 않는다.

## 상호작용 상태

```java
public Bundle saveInteractionState()
public void restoreInteractionState(Bundle state)
```

schema=1, 선택 카테고리 인덱스, 페이지별 비음수 스크롤 위치만 저장. 동일 카테고리/항목 순서로 재생성해야 한다. null restore는 무동작. 정확히 세 키, 타입, schema, 페이지 범위, 스크롤 배열 길이, 비음수 값 검증 후 변경; 잘못된 snapshot은 IllegalArgumentException. 활성화 veto는 IllegalStateException이며 스크롤은 유지. 복원은 onTab/replay를 생략하지만 property-sheet 알림은 실행한다. 스크롤은 layout 뒤 적용하며 범위 안으로 clamp. 설정값은 별도 저장.

## 외형, 언어

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
public void refreshAppearance(Typeface font,String language)
```

restyle은 글꼴, 선택색, 탭색 상태와 행 높이를 갱신한다. language callback은 카테고리 제목과 현재 캡션 순서를 갱신한다. refreshAppearance는 restyleTree와 localizeTree를 실행해 기존 전체 컨트롤을 갱신한다. 페이지, 스크롤, 선택 identity 유지.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmSettingsView.java`
