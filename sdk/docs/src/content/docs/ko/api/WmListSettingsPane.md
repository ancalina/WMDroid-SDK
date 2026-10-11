---
title: "WmListSettingsPane"
slug: ko/api/wmlistsettingspane
---
[API 색인](/ko/api/)

```java
public final class WmListSettingsPane extends LinearLayout
```

목록과 오른쪽 명령 버튼 열을 배치한다. 호출자가 목록 스크롤·선택·저장과 버튼 동작을 제공한다. UI 스레드에서 생성한다. 아래 크기는 논리 픽셀이다.

## 생성자

```java
public WmListSettingsPane(Context context,View list,View... buttons);
```

`list`와 각 버튼을 컨테이너의 자식으로 추가한다. `list`와 버튼은 부모가 없어야 한다. null 목록·버튼 배열·버튼, 부모가 있는 View, 중복 버튼, 목록을 버튼으로 재사용하면 `IllegalArgumentException`이다. 빈 버튼 배열은 허용한다.

바깥 여백은 좌우 `16`, 위아래 `4`이다. 버튼은 배열 순서대로 세로 배치하며 크기 `144×40`, 아래 간격 `8`을 사용한다. 목록과 명령 열은 컨테이너의 남은 높이를 채운다. 이 컨테이너 자체는 스크롤하지 않는다.

## listFrame

```java
public final FrameLayout listFrame;
```

전달한 목록의 부모. 명령 열을 제외한 남은 너비를 채운다. 흰 배경과 회색 `2` 픽셀 테두리를 사용하며, 같은 크기의 안쪽 여백으로 목록을 테두리에서 분리한다. 목록은 이 안쪽 영역을 채운다.

## commands

```java
public final LinearLayout commands;
```

전달한 버튼들의 부모. 너비는 `152`, 왼쪽 안쪽 여백은 `8`이다. 컨테이너 너비가 늘어나도 명령 열 너비는 유지하고 목록 영역만 늘린다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmListSettingsPane.java`
