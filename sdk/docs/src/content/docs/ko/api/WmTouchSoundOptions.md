---
title: "WmTouchSoundOptions"
slug: ko/api/wmtouchsoundoptions
---
[API](/ko/api/)

```java
public final class WmTouchSoundOptions extends LinearLayout
```

터치음 사용 여부와 음량 단계 선택 UI. 설정 저장과 [WmTouchFeedback](/ko/api/wmtouchfeedback/) 재생 호출은 소유자가 담당한다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmTouchSoundOptions.java`

## 생성자

```java
public WmTouchSoundOptions(Context context,Typeface font,String language,boolean on,int level,Listener listener);
```

UI 스레드에서 생성한다. `on`은 초기 사용 여부, `level`은 `1`(작게) 또는 `2`(크게)다. `level`이 범위 밖이거나 `listener`가 null이면 `IllegalArgumentException`이 발생한다. 사용을 꺼도 유효한 `level`이 필요하다.

초기 선택값을 설정한 뒤 리스너를 등록하므로 생성 중 콜백은 없다. `on == false`이면 두 음량 선택을 비활성화하고 선택값은 유지한다.

`font`는 공유 체크박스·라디오·텍스트 스타일에 적용한다. `language`는 [WmControls.bindText](/ko/api/wmcontrols/#bindtext)의 언어 바인딩에 사용한다. 이후 언어 변경은 `WmControls.localizeTree`로 적용한다.

설명·체크박스·음량 선택은 각각 높이 `44` SDK 논리 픽셀이다. 바깥 여백은 왼쪽·위·오른쪽 `8`, 아래 `0`이다. 음량 선택 행은 왼쪽 여백 `36`과 너비 `140`인 라디오 두 개를 사용한다.

## Listener.changed

```java
public interface Listener
void changed(boolean enabled,int level);
```

체크박스나 라디오 선택이 바뀌면 해당 변경 처리 안에서 동기 호출한다. `enabled`는 현재 사용 여부, `level`은 현재 선택한 `1` 또는 `2`다.

체크박스 변경은 두 라디오의 활성 상태를 먼저 바꾼 뒤 콜백을 호출한다. 사용을 끄거나 다시 켜도 음량 선택은 유지한다. 음량 선택은 사용 여부를 바꾸지 않는다.

콜백에서 설정을 저장하고 이후 처리한 터치에 사용할 재생 정책을 갱신한다. 이 컨트롤 자체는 효과음을 재생하지 않는다.
