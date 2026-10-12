---
title: "WmTitleBar"
slug: ko/api/wmtitlebar
---
[API 색인](/ko/api/)

```java
public final class WmTitleBar extends LinearLayout
```

시작 영역, 상태 슬롯, 시계, X/OK를 결합하는 논리 VGA 제목줄. 데이터 갱신, 클릭 동작, 타이머는 호스트 소유. 기본 높이 52 논리 픽셀.

## 필드

```java
public static final int CLOSE_WIDTH=50;
public final LinearLayout start;
public final ImageView logo;
public final TextView caption;
```

CLOSE_WIDTH는 오른쪽 닫기/빈 공간 폭. start, logo, caption은 실제 자식 뷰이며 호스트가 시작 클릭과 caption 텍스트를 설정한다. logo는 start 리소스, 42×36px.

## 생성자

```java
public WmTitleBar(Context context,Typeface font,String text,WmBar paint)
public WmTitleBar(Context context,TextView caption,WmBar paint)
```

첫 overload는 SDK TextView 생성 후 위임. 두 번째는 아직 부모가 없는 non-null caption과 non-null paint 필요; 위반은 IllegalArgumentException. caption은 한 줄, 끝 말줄임, 가로 여백 14/4px로 설정. start는 남은 가로 공간을 유지하지만 터치 영역은 글자 폭으로 제한하며 ACTION_DOWN에 클릭한다.

## startTouchWidth / onLayout

```java
public int startTouchWidth()
protected void onLayout(boolean changed,int left,int top,int right,int bottom)
```

현재 caption 잉크 폭과 로고, 여백, 8px 터치 여유로 터치 폭 계산. 72..180px 목표를 start 실제 폭으로 제한한다. 배치 후 caption이 터치 범위에 완전히 들어오면 pressed/selected 색 적용, 넘으면 흰색을 유지해 눌림 배경과 글자가 끊기는 현상을 줄인다.

## addTray

```java
public WmTraySlot addTray(int kind,ImageView image)
```

CONNECTION, PHONE, VOLUME, BATTERY 순서로 슬롯 삽입하고 실제 슬롯 반환. kind는 WmTraySlot 0..3만 허용, 나머지는 IllegalArgumentException. 같은 종류를 다시 추가하면 IllegalStateException. image는 부모 없는 ImageView 필요. 슬롯 클릭은 호스트가 등록한다.

## setClock

```java
public void setClock(WmTrayClock replacement)
```

기존 시계를 제거하고 오른쪽 슬롯 앞에 새 시계 삽입. null은 제거. 같은 인스턴스는 무동작, 다른 부착된 인스턴스는 IllegalArgumentException. 폭 WRAP_CONTENT, 높이52, 오른쪽 여백4px. 시각 갱신과 클릭은 호출자 책임.

## 닫기, 예약 공간

```java
public ImageView closeButton(boolean ok)
public void reserveCloseSpace()
```

closeButton은 ok=true면 OK, false면 X로 갱신하고 실제 버튼 반환. 버튼 테두리 없음; 클릭, 접근성 설명은 호출자가 설정한다. 기존 예약 공간을 교체한다. reserveCloseSpace는 닫기 버튼을 제거하고 같은 50px 빈 공간 유지. 이전 버튼 참조는 새 버튼 생성 시 재사용되지 않는다.

## 리소스, 스타일

```java
public void reloadResources()
public void restyle(WmBar paint)
```

reloadResources는 시작 로고만 다시 로드한다. 슬롯 리소스, 상태는 각 슬롯에서 갱신. restyle은 non-null paint 필요(위반 IllegalArgumentException), 기존 배경, 선택색, 슬롯, 시계, 닫기 버튼을 갱신하며 뷰, 동작 유지.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmTitleBar.java`
