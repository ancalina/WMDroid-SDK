---
title: "WmTraySlot"
slug: ko/api/wmtrayslot
---
[API 색인](/ko/api/)

```java
public final class WmTraySlot extends FrameLayout
```

제목줄 상태 아이콘의 이미지·터치 영역·연결 애니메이션을 묶는 슬롯. 좌표는 논리 픽셀. 배터리는 일반 클릭(UP), 나머지는 DOWN 활성화.

## 상수·생성자

```java
public static final int CONNECTION=0,VOLUME=1,BATTERY=2,PHONE=3;
public final ImageView image;
public WmTraySlot(Context context,int kind,ImageView image)
```

kind는 0..3; 다른 값은 IllegalArgumentException. image는 실제 자식으로 추가되므로 부모 없는 non-null ImageView 필요. 그림32×32px·위쪽10px, 슬롯 높이52px·오른쪽 여백4px. 초기 폭은 연결22·배터리42·나머지38px. image 필드는 호출자가 상태 이미지·접근성 설명을 갱신할 때 사용.

## style

```java
public void style(WmBar bar)
```

bar의 제목 버튼 배경을 적용한다. bar는 non-null 필요. 이미지나 동작은 교체하지 않는다.

## connectionIcon

```java
public void connectionIcon(String id)
```

연결 슬롯만 사용; 다른 종류는 IllegalStateException. 허용 ID: signal·network_connected·network_data_connected·network_disconnected, 나머지(null 포함)는 IllegalArgumentException. 같은 ID면 무동작. 변경하면 애니메이션 정지 후 WmIcons 이미지 로드. 슬롯 폭은 각각22/32/34/34px, disconnected의 이미지 왼쪽 여백1px, 나머지2px. 그림 자체는 늘리지 않는다.

## connectionState

```java
public void connectionState(WmDevice.Network network)
```

다른 종류는 IllegalStateException. null 또는 연결/연결 중 모두 false면 disconnected. 연결 중이면 34px 슬롯·1px inset과26프레임×150ms 반복 애니메이션 사용. 팩 프레임을 읽을 수 없으면 network_data_connected 정지 이미지. 연결 완료는 Wi-Fi signal, cellular data_connected, 나머지 connected. 인터넷 검증 여부는 링크 연결 아이콘과 분리. 처음 연결 중 전환에서 그림을 로드하며 window token이 있고 window visibility가 VISIBLE일 때 재생합니다. 슬롯 자체의 visibility는 검사하지 않습니다.

## 애니메이션·리소스 수명

```java
public void stopConnectionAnimation()
public void reloadResources()
protected void onAttachedToWindow()
protected void onDetachedFromWindow()
```

stop은 재생만 멈추며 현재 그림·상태 유지. reloadResources는 재생 중지·캐시/ID 초기화; 현재 ImageView 그림을 즉시 교체하지 않으므로 다음 connectionState/connectionIcon으로 새 팩 반영. detach는 정지, attach는 connecting 캐시가 있으면 재시작. stop 이후에도 connectionState 호출이나 재부착으로 다시 시작할 수 있습니다. 재부착은 window visibility를 검사하지 않습니다.

## activateOnDown

```java
public static void activateOnDown(View button)
```

기존 OnTouchListener를 교체하고 Android 효과음 해제. DOWN에 pressed=true 후 performClick을 한 번 실행·소비한다. MOVE는 좌표가 버튼 안인지로 눌림 표시 변경. UP/CANCEL은 눌림 해제하며 추가 클릭 없음. 나머지 touch도 소비. 클릭 listener는 별도로 등록해야 한다.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmTraySlot.java`
