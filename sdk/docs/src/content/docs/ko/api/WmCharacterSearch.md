---
title: "WmCharacterSearch"
slug: ko/api/wmcharactersearch
---
[API](/ko/api/)

선택 필드·팝업 목록의 키보드 문자 검색 상태를 공유하는 package-private 구현입니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmCharacterSearch.java`

## 선언

```text
final class WmCharacterSearch

```

외부 앱은 [WmChoice](/ko/api/wmchoice/), [WmEditableChoice](/ko/api/wmeditablechoice/), [WmRadioMenu](/ko/api/wmradiomenu/)의 키보드 API를 사용하세요. 검색 상태는 main thread에서 관리합니다.
