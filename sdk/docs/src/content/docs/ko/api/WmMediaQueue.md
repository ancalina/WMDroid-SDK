---
title: "WmMediaQueue"
slug: ko/api/wmmediaqueue
---
[API 색인](/ko/api/)

```java
public final class WmMediaQueue
```

중복 소스를 허용하는 순서형 재생 큐. 재정렬은 현재 항목의 발생 위치를 유지한다. UI 스레드에서 소유·변경하며 자체 동기화는 제공하지 않는다.

## Entry

```java
public static final class Entry
public final Uri uri;public final String title;
public Entry(Uri uri,String title)
```

URI는 소문자 스킴 `content`, `http`, `https`만 허용. content는 계층형이며 authority 필수; HTTP(S)는 `WmBrowserView.normalizeAddress`로 검증한다. 위반/null URI는 `IllegalArgumentException`. 전달 URI 자체를 저장하므로 별도 읽기 권한은 호출자가 확보한다. null title은 URI 마지막 경로, 그것도 없으면 빈 문자열; 제목은 256 UTF-16 코드 단위로 제한.

## 생성자

```java
public WmMediaQueue()
```

빈 큐, currentIndex=-1, shuffle=false로 시작한다.

## Read queue

```java
public Entry[] entries()
public int size()
public int currentIndex()
```

`entries`는 현재 순서의 배열 복사. `size`는 발생 수이며 중복도 센다. `currentIndex`는 현재 발생의 배열 인덱스 또는 선택 없음 -1.

## Replace and append

```java
public void replace(Entry[] values,int selected)
public void append(Entry[] values)
public void select(int index)
public void clear()
```

배열/항목 null은 `IllegalArgumentException`. `replace`는 전체 교체하며 selected=-1 또는 배열 범위 내 인덱스 필요. `append`는 뒤에 추가하고 현재 선택은 유지. `select`는 유효 인덱스로 선택 변경. `clear`는 큐와 현재 선택을 비운다. 각 변경은 shuffle 순서를 다시 구성한다.

## Remove and move

```java
public boolean remove(int index)
public void move(int from,int to)
```

모든 인덱스는 범위 내여야 하며 위반 시 `IllegalArgumentException`. `remove` 반환 true는 현재 발생을 제거했음을 뜻한다. 현재 항목 제거 시 다음 위치(마지막이면 이전 위치) 선택, 빈 큐는 -1. 현재보다 앞 항목 제거 시 인덱스 감소. `move`는 한 발생을 지정 위치로 옮기고 현재 발생의 정체성을 유지한다.

## Shuffle and advance

```java
public boolean shuffle()
public void setShuffle(boolean value)
public boolean canAdvance(int direction,boolean repeat)
public int advance(int direction,boolean repeat)
```

direction은 -1(이전) 또는 1(다음); 다른 값은 `IllegalArgumentException`. 빈 큐에서는 canAdvance=false/advance=-1. 선택 없으면 다음만 가능. repeat=false로 경계에 도달하면 -1을 반환하고 현재 선택을 유지한다. `advance` 성공 시 새 현재 배열 인덱스 반환. repeat=true는 순환. shuffle은 임의 반복 추첨 대신 permutation을 사용하며 현재 발생을 첫 위치에 두고 나머지를 섞는다. 반복 순환에서 새 순서를 생성할 수 있다. 원본 shuffle/history 알고리즘과 동일함은 미검증.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmMediaQueue.java`
