---
title: "WmNotesView"
slug: ko/api/wmnotesview
---
[API 색인](/ko/api/)

```java
public final class WmNotesView extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener,WmControls.BackHandler
```

문서 목록·폴더 필터·정렬과 일반 텍스트 편집 클라이언트. 항목/폴더 저장과 문서 열기는 소유 앱이 처리한다. 이 편집기는 일반 텍스트이며 PWI 그림·녹음·서식 편집은 제공하지 않는다.

## Entry

```java
public static final class Entry
public final String key,title,type,folder;public final long modified,size;
public Entry(String key,String title,long modified)
public Entry(String key,String title,long modified,long size,String type)
public Entry(String key,String title,long modified,long size,String type,String folder)
```

key는 non-null·비어 있지 않은 ID. title은 확장자 제외 표시 이름, type은 소유 앱의 확장자/분류, folder는 root면 빈 문자열. 문자열 non-null, size≥0 필요; 위반 시 `IllegalArgumentException`. modified는 Unix epoch ms, size는 소유 앱이 제공한 byte 수. 짧은 생성자는 생략된 size=0, type/folder=빈 문자열.

## Folder and callbacks

```java
public static final class Folder
public final String id,name;
public Folder(String id,String name)
public static final int ALL_FOLDERS=13729,MANAGE_FOLDERS=13730;
public interface FolderListener {void manage();}
public interface Listener {void open(Entry entry);}
public static final int RENAME=25513,DELETE=25512;
public interface ActionListener {void command(Entry entry,int command);}
```

Folder ID/이름은 non-null·비어 있지 않아야 하며 위반 시 `IllegalArgumentException`. 항목 한 번 탭은 선택 후 open. FolderListener를 설정하면 폴더 메뉴에 관리 명령 추가, manage로 소유 앱의 폴더 관리 열기. ActionListener를 설정하면 길게 누른 항목에 RENAME/DELETE 메뉴 제공; 실제 변경은 소유 앱이 수행한다.

## 생성자

```java
public WmNotesView(Context context,Typeface regular,Typeface bold,int selection,Listener listener)
```

두 글꼴과 listener 필수; null이면 `IllegalArgumentException`. 기본 sort=NAME, ascending=true, zoom=100%, 모든 폴더 표시. 목록 기본 행 높이 34 논리 픽셀이며 글꼴의 실제 글자 범위에 따라 늘어난다.

## Catalog and folders

```java
public void setEntries(Entry[] values)
public void setFolders(Folder[] values)
public void setOnFolderListener(FolderListener listener)
public String folderId()
public void setFolder(String id)
public Entry selected()
public void setOnActionListener(ActionListener listener)
```

setEntries/setFolders는 배열 복사 후 null 배열/항목·중복 key/ID 거부(`IllegalArgumentException`). 교체 시 현재 필터로 목록 구성, 항목 선택은 키로 유지하며 키가 없으면 해제한다. `selected()`는 선택 항목 또는 null 반환. 선택 폴더가 카탈로그에서 사라지면 모든 폴더로 복원. folderId의 null=모든 폴더, 빈 문자열=root, 나머지는 등록된 ID. setFolder는 모르는 ID를 거부하고 선택 해제·목록 맨 위 이동. root는 toolbar 메뉴 항목이 없으므로 API로 선택한다. 두 선택적 listener의 null은 관련 기능 제거; 변경 때 열린 관련 팝업 닫기.

## Sort and metadata

```java
public static final int SORT_NAME=0,SORT_DATE=1,SORT_SIZE=2,SORT_TYPE=3;
public int sortOrder()
public boolean sortAscending()
public void setSortOrder(int order,boolean ascending)
public void refreshMetadata()
public static String formatSize(long bytes)
```

setSortOrder는 0..3 외 값을 거부(`IllegalArgumentException`)하고 명시한 방향으로 정렬한다. 메뉴에서 현재 기준을 다시 선택하면 방향 반전. 이름/종류는 locale Collator, 날짜/크기는 수치 비교. 종류 정렬에서 빈 종류는 오름차순 마지막·내림차순 처음이며 같은 종류는 제목 비교. 나머지 동점은 수정 날짜·키 순으로 결정. refreshMetadata는 시스템 시간대·날짜·12/24시간 설정에 맞춰 목록 표시를 갱신하고 스크롤 위치 유지; 저장 I/O 없음. 오늘 수정 항목은 시간, 나머지는 짧은 날짜. formatSize는 음수 거부, <1024는 `b`, <1048576는 `k`, 나머지 `m`; 1024/1048576 단위에서 절반 이상 올림한 정수 문자열.

## Text editor

```java
public void edit(String text)
public void showList()
public boolean isEditing()
public EditText editor()
public String text()
public int zoomPercent()
public void setZoomPercent(int value)
public void setMessage(String text)
```

edit는 non-null 텍스트로 편집기를 채우고 커서/스크롤을 시작으로 이동하며 목록 숨김; null은 `IllegalArgumentException`. showList는 키보드 숨김·편집 포커스 해제·목록 복원, 텍스트는 유지하고 저장하지 않는다. editor는 실제 입력 컨트롤, text는 현재 초안. zoom은 75/100/150/200/300만 허용하며 다른 값은 `IllegalArgumentException`; 100% 글자 크기 28 논리 픽셀. setMessage non-null은 내용 영역을 literal 메시지로 덮고 null은 제거. edit/showList도 메시지 제거.

## Interaction state and Back

```java
public Bundle saveInteractionState()
public void restoreInteractionState(Bundle state)
public boolean back()
```

Bundle은 선택 key·폴더·zoom·정렬 방향/기준·편집 커서/스크롤/포커스만 저장. 문서 내용·카탈로그·목록 스크롤·목록/편집 모드는 소유 앱이 별도로 복원한다. 항목·폴더·초안을 먼저 공급하고 모드를 설정한 뒤 restore. null Bundle은 무동작. 없거나 필터 밖 선택 키는 무시하고 커서는 현재 길이로 clamp. 잘못된 폴더/zoom/정렬은 setter 예외로 실패하며 전체 transactional 복원은 아니다. Back은 폴더/작업/정렬 팝업만 닫고 처리 시 true; 편집 화면에서 목록 복귀는 소유 앱이 처리한다.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
```

재스타일/언어 변경은 열린 팝업을 닫고 글꼴·캡션·정렬·날짜/크기 열 폭을 갱신한다. 영어 목록은 bold 8pt@192dpi, 그 외 기본은 regular 24 논리 픽셀. 사용자 문서 제목/내용은 번역하지 않는다.

## View lifecycle

```java
protected void onWindowVisibilityChanged(int visibility)
protected void onSizeChanged(int w,int h,int oldw,int oldh)
protected void onDetachedFromWindow()
```

표시 시 metadata 갱신. 크기 변경은 편집기 최소 높이를 뷰 높이에 맞춤. detach는 열린 팝업 제거.


복원된 focus=true만 editor.requestFocus를 호출합니다. false는 현재 포커스를 유지하며 clearFocus하지 않습니다. 무포커스 상태가 필요하면 소유자가 복원 전 editor().clearFocus()로 준비합니다. 음수 스크롤은0으로 제한하며 비트랜잭션 적용 중 실패하면 앞서 적용한 상태가 남을 수 있습니다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmNotesView.java`
