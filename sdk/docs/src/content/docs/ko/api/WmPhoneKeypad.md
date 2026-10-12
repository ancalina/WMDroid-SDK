---
title: "WmPhoneKeypad"
slug: ko/api/wmphonekeypad
---
[API 색인](/ko/api/)

```java
public final class WmPhoneKeypad extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

4×4 전화 키패드. 숫자 입력과 지우기, 단축 다이얼, 통화 기록, 통화 버튼을 소유 앱 콜백에 전달한다.

## 생성자, 배치

```java
public static final int WIDTH=480,HEIGHT=294;
public WmPhoneKeypad(Context context,Typeface regular,Typeface bold,Listener listener)
protected void onMeasure(int widthSpec,int heightSpec)
protected void onLayout(boolean changed,int left,int top,int right,int bottom)
```

두 글꼴과 listener 필수; null이면 `IllegalArgumentException`. WIDTH/HEIGHT는 논리 픽셀. 측정 공간이 작으면 같은 배율로 축소하고 큰 공간에서는 키 자체를 늘리지 않는다. 남는 폭은 좌우 중앙 정렬, 남는 높이는 위쪽에 둔다.

## Listener

```java
public interface Listener {
    void digit(char digit);
    void backspace();
    void speedDial();
    void history();
    void talk();
}
```

digit은 `0`..`9`, `*`, `#`만 전달. `+`는 0 키의 표시 텍스트이며 길게 눌러 입력하는 동작은 없다. 지우기 등 콜백은 완성된 클릭에서 실행; 손가락이 키 밖으로 이동하거나 여러 손가락/취소 이벤트가 오면 누름 취소.

## Enable and style

```java
public void setEnabled(boolean enabled)
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
```

enabled는 각 키에 전파하고 비활성화 때 누름 상태 제거. 재스타일은 두 글꼴만 적용하며 null 글꼴 거부. 언어는 작업 캡션/접근성 설명 갱신. 선택/탭 색은 키패드 고정 스킨을 바꾸지 않는다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmPhoneKeypad.java`
