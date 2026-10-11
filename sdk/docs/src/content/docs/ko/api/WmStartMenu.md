---
title: "WmStartMenu"
slug: ko/api/wmstartmenu
---
[API](/ko/api/)

폴더·프로그램 항목을 스크롤 메뉴로 표시하고 선택을 전달하는 View.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmStartMenu.java`

```text
public final class WmStartMenu extends FrameLayout implements WmControls.BackHandler
```

## Select

```text
public interface Select
void accept(WmShell.Entry entry);
```

## 생성자 · refresh

```text
public WmStartMenu(Context context,List<WmShell.Entry> entries,Typeface font,int accent,Drawable background,Select selected);
public void refresh(List<WmShell.Entry> entries,Typeface font,int accent,Drawable background);
```

UI thread에서 호출하세요. entries가 null이면 빈 목록입니다. 목록은 복사하지만 Entry 객체는 공유합니다. 선택 callback은 leaf 항목을 누르면 호출됩니다. 폴더는 메뉴 안에서 열립니다.

`ListView`는 화면에 표시할 행을 생성하고 스크롤할 때 재사용합니다. 메뉴 폭은 화면 밖 항목을 포함한 현재 폴더의 모든 제목과 뒤로 문자열로 계산합니다. `refresh()`는 글꼴·색·제목만 바뀌면 표시 중인 행을 갱신하고 현재 표시 위치를 유지합니다. 항목 구조 변경이나 폴더 이동은 표시 위치를 처음으로 초기화합니다. 현재 폴더가 삭제되면 루트로 이동합니다. background가 null이면 런처 설정에서 시작 메뉴 배경을 읽습니다. 새 행의 아이콘·배경은 동기적으로 읽을 수 있습니다. Entry.icon은 `android.resource://` 또는 `content://me.ancal.wmdroid.shell/...`만 읽습니다. 다른 content authority·file·HTTP URI는 아이콘을 반환하지 않습니다.

## setTextColor · setBackLabel

```text
public void setTextColor(int color);
public void setBackLabel(String value);
```

현재 행의 텍스트 색 또는 뒤로 항목의 표시 문자열을 바꿉니다. null 뒤로 문자열은 빈 문자열입니다. 아이콘을 다시 읽지 않습니다.

## setMaxHeight · menuWidth · onMeasure

```text
public void setMaxHeight(int logicalPixels);
public int menuWidth();
protected void onMeasure(int widthSpec,int heightSpec);
```

높이·폭의 단위는 논리 픽셀입니다. 최대 높이는 음수를 0으로 제한합니다. 메뉴 폭은 188부터 제목에 맞춰 늘고 최대 342입니다. 측정 결과는 parent의 사용 가능 크기를 넘지 않습니다.

## setIconsUnframed · setIconPolicy

```text
public void setIconsUnframed(boolean value);
public void setIconPolicy(WmIconPolicy value);
```

`setIconsUnframed()`는 전체 앱에 정책을 적용합니다. `setIconPolicy()`는 앱별 정책을 받습니다. null 정책은 전체 비활성입니다. 정책이 바뀐 앱 아이콘만 다시 처리합니다. [WmIconPolicy](/ko/api/wmiconpolicy/)를 참조하세요.

## back · dismissFolders

```text
public boolean back();
public void dismissFolders();
```

`back()`은 이전 폴더로 이동하면 true, 이미 루트면 false입니다. 삭제된 폴더는 건너뜁니다. `dismissFolders()`는 루트로 돌아가고 폴더 이력·scroll·pressed·selected·focus를 초기화합니다.

## windowBackground

```text
public static Drawable windowBackground(int accent);
```

흰색 ColorDrawable을 반환합니다. accent 인자는 결과에 영향을 주지 않습니다.

## Select

`accept(entry)`는 선택된 프로그램 항목을 전달합니다. callback에서 앱 실행 또는 메뉴 닫기를 수행하세요.
