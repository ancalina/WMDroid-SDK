---
title: "WmIconOrder"
slug: ko/api/wmiconorder
---
[API 색인](/ko/api/)

```java
final class WmIconOrder
```

내부 아이콘 목록 삽입 비교기. 클래스와 compareForInsert는 package-private이며 외부 SDK API가 아니다. 설정 목록의 제목 순서를 설명하는 구현 참조.

## 삽입 비교

```java
static int compareForInsert(String existing,String incoming)
```

incoming의 최대 260 UTF-16 code unit을 고정 소문자 테이블로 비교한다. existing이 끝나면 NUL로 비교. 첫 차이에서 변환된 existing-incoming을 반환한다. 양쪽 값이 모두 NUL이면 뒤를 검사하지 않고 0을 반환한다. NUL을 만나지 않으면 incoming 길이 또는 260 UTF-16 단위까지 검사하며 해당 범위가 같으면 0이다. incoming이 같거나 기존 제목의 접두어이면 새 항목을 기존 항목 앞에 삽입한다. locale Collator·현대 Unicode case folding과 다르며 비교 인수를 뒤집어 일반 Comparator처럼 쓰는 계약이 아니다. null 인수 처리 없음.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmIconOrder.java`
