---
title: "WmDocumentFolders"
slug: ko/api/wmdocumentfolders
---
[API 색인](/ko/api/)

```java
public final class WmDocumentFolders extends FrameLayout implements WmControls.LanguageListener,WmControls.RestyleListener,WmControls.BackHandler,WmControls.ModalOwner
```

평면 문서 폴더 목록과 생성, 이름 변경, 삭제 명령. 폴더 쓰기는 모두 소유 앱이 수행한다.

## Commands and Listener

```java
public static final int NEW=1009,RENAME=1010,DELETE=1012,LIST=1044,NAME_LIMIT=64;
public interface Listener {void command(int command,String folder);void rename(String folder,String name);}
```

NEW/DELETE 버튼은 `command`에 ID와 선택 폴더 ID(없으면 null)를 전달. RENAME은 내부 라벨 편집 시작. `rename`은 검증된 초안과 폴더 ID를 전달하며 소유 앱이 완료 후 `setFolders`로 갱신해야 한다. NEW를 받은 소유 앱은 생성 후 `editName`으로 이름 편집 시작 가능. LIST는 목록 View ID, NAME_LIMIT는 64 UTF-16 코드 단위.

## 생성자

```java
public WmDocumentFolders(Context context,Typeface regular,Typeface bold,int accent,Listener listener)
```

두 글꼴, 리스너 필수; null이면 `IllegalArgumentException`. `accent`는 제목과 선택 색.

## Catalog and selection

```java
public void setFolders(WmNotesView.Folder[] values,String current)
public String selectedId()
public void editName(String id)
public boolean isEditing()
```

배열 복사 후 null 배열/항목, 중복 ID 거부(`IllegalArgumentException`). `current`는 열린 문서의 폴더 아이콘 상태용이며 목록 선택과 별개. 기존 선택 ID 유지, 없으면 선택 null; 진행 중 이름 편집은 종료. 표시 순서는 locale Collator와 ID tie-break. `editName`은 알려진 ID를 선택해 전체 이름 선택; null/없는 ID/busy 상태는 무동작. `isEditing`은 라벨 입력기 유무.

## Commit and Back

```java
public boolean commitEditing()
public boolean back()
```

반환값은 저장 성공 여부가 아닙니다. busy이면 false, 편집이 없거나 이름이 같아 편집을 끝내면 true입니다. 앞뒤 ASCII 공백을 제거하고64 UTF-16 코드 단위, WmDocumentProperties.validName을 검증하며 잘못된 이름은 메시지, false입니다. 변경된 유효 이름은 Listener.rename에 동기로 전달하고 항상 false입니다. callback이 setFolders로 즉시 편집을 끝내도 false입니다.

자동 busy 설정은 없습니다. 비동기 저장 소유자는 setBusy(true)→저장→UI 스레드에서 setFolders→setBusy(false) 순서로 처리합니다. 실패 후 재편집도 busy를 해제합니다. 중단된 명령, 행 선택은 자동 재실행하지 않습니다. Back은 메시지부터 닫고 busy는 소비하며 이름 편집을 취소해도 생성된 폴더를 삭제하지 않습니다. 취소할 편집이 없으면 false입니다.

## Busy and input ownership

```java
public void setBusy(boolean value)
public boolean blocksOwnerCommands()
public View inputMethodOwner()
```

busy는 버튼, 입력기 비활성화와 owner-command 차단에 사용. `blocksOwnerCommands`는 busy 값을 반환. `inputMethodOwner`는 열린 이름 입력기 또는 null.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
```

언어 변경은 제목, 버튼 캡션과 행 글자 크기를 갱신한다. 재스타일은 저장한 글꼴/선택 색과 현재 행 색을 갱신한다. 기존 행 입력값은 유지한다.

## Detachment

```java
protected void onDetachedFromWindow()
```

분리 시 추가된 메시지 상자를 제거한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmDocumentFolders.java`
