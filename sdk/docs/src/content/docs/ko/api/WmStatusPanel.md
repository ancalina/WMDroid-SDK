---
title: "WmStatusPanel"
slug: ko/api/wmstatuspanel
---
[API 색인](/ko/api/)

```java
public class WmStatusPanel extends LinearLayout implements WmControls.RestyleListener
```

Android 연결, 음량, 배터리, 전화 상태 팝업 본문. WmPopup이 틀을 담당한다. 부착 동안 1초마다 상태를 읽는다. 호스트는 실행, 닫기, 설정, 효과음과 번역/테마 매핑을 제공한다.

## Host

```java
public interface Host {
    void launch(Intent intent);
    void changed();
    void backlight();
    void hide();
    void settings();
    String text(String authored);
    int color(int original);
    void feedback(boolean ring,int step);
    void release();
    default void attached(){}
}
```

launch는 Android 설정 Intent 처리, backlight는 화면 조명 UI, hide는 팝업 닫기, settings는 호스트 설정 열기. text는 작성 문자열 번역, color는 원본 ARGB 색의 테마 색 반환. changed는 음량/벨 모드 변경 시 상태 갱신 통지; 벨 모드 변경 SecurityException 뒤에도 호출될 수 있다. feedback은 실제 음량이 바뀔 때 호출되며 ring=true면 STREAM_RING, false면 STREAM_MUSIC, step은 0..5. attached는 부착 시 호출(default 무동작), release는 매 detach마다 효과음 등 호스트 자원 해제에 사용. 콜백은 UI 이벤트, UI polling 경로에서 동기 실행.

## 생성자

```java
public WmStatusPanel(Context context,Typeface regular,int kind,Host host)
```

kind=0 연결, 1 음량, 3 전화; 그 외 값은 배터리 본문. host는 생성 중 사용하므로 non-null 필요. 초기 상태를 즉시 읽는다. 연결은 네트워크 종류/연결 중/인터넷 검증, 전화는 WmDevice.Phone 상태, 배터리는 충전, 잔량, API21+ 절전 표시. 음량은 음악/벨 0..5 단계와 일반/진동/무음 선택을 제공한다. 일반 모드에서 벗어나면 음악 음량 저장, 0 설정, 복귀 시 저장 음량 복원. 사용자가 연결, 배터리 설정 링크를 누르면 Host.launch 호출.

## refresh

```java
public void refresh()
```

현재 선택 종류의 Android 상태를 다시 읽고 표시한다. 음량 변경에는 플랫폼 MODIFY_AUDIO_SETTINGS 권한이 필요. API24+에서 무음 진입/해제에 방해 금지 접근이 없으면 변경 대신 안내, 접근 설정 링크 표시. 시스템 변경 SecurityException도 안내로 처리한다. 권한 승인, 설정 Intent 실행, 밝기 조절은 호스트 책임.

## 스타일, 수명

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
protected void onAttachedToWindow()
protected void onDetachedFromWindow()
```

restyle은 regular 참조, 숨기기 버튼 테마, 음량 컨트롤을 갱신하고 리소스 revision 변경 시 음량 아이콘 재로드. 부착 시 Host.attached와 1초 polling 예약; 음량 UI는 현재 모드 컨트롤에 포커스. detach는 polling 취소 후 Host.release. 재부착 시 다시 예약한다.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmStatusPanel.java`
