---
title: "WmVolumeFeedback"
slug: ko/api/wmvolumefeedback
---
[API 색인](/ko/api/)

```java
public class WmVolumeFeedback
```

음량 조절 단계의 media, ring 미리보기 소리를 재생한다. 전역 음량을 변경하거나 Android 권한을 요청하지 않는다.

## 생성자

```java
public WmVolumeFeedback(Context context);
```

application Context를 보관하고 각 경로에 최대 동시 재생 `1`인 SoundPool을 만든다. 아래 WAV 리소스 3개를 각 pool에 비동기로 load한다. 파일 준비는 동기이며 파일당 상한은 `4 MiB`다. 생성과 리소스 변경 후 `play`는 worker 스레드에서 수행한다. 파일 준비의 IOException은 해당 소리를 사용할 수 없음으로 처리한다.

| Android API | media | ring |
| --- | --- | --- |
| 21 미만 | STREAM_MUSIC | STREAM_RING |
| 21 이상 | USAGE_MEDIA | USAGE_NOTIFICATION_RINGTONE |

API 21 이상은 CONTENT_TYPE_SONIFICATION을 사용한다. 이 클래스가 정한 재생 경로와 별도로 실제 출력은 시스템의 해당 경로 음량 설정을 따른다.

## play

```java
public synchronized void play(boolean ring,int step);
```

`ring=false`는 media pool, `true`는 ring pool을 선택한다. 값별 소리는 다음과 같다.

| step | 리소스 |
| --- | --- |
| 0 이하 | 재생하지 않음 |
| 1 | sounds/volume/quietest.wav |
| 5 | sounds/volume/loudest.wav |
| 다른 양수 | sounds/volume/default.wav |

리소스 revision이 바뀌면 기존 pool, 재생을 해제하고 다시 준비한다. 이 검사는 `step<=0`보다 먼저 수행한다. load가 성공 완료되지 않은 소리는 건너뛰며 완료 후 자동 재생하지 않는다. 필요하면 다시 호출한다.

각 pool의 기록 시각부터 `250 ms` 미만이면 건너뛴다. 기록은 uptime 기준이며 초기값, reload 후 값은 `0`이다. 준비된 소리의 재생을 시도할 때만 현재 시각으로 갱신한다. 건너뛴 호출은 갱신하지 않는다.

재생 직전 같은 pool의 이전 미리보기를 중지한다. 좌우 gain=`1`, priority=`1`, loop=`0`, rate=`1`로 한 번 재생한다. 실제 재생 성공 여부를 반환하지 않는다. release 후 호출은 아무 작업도 하지 않는다. play와 release는 객체 잠금으로 직렬화한다.

## release

```java
public synchronized void release();
```

두 pool의 load listener, 재생, 리소스를 해제하고 객체를 사용 종료 상태로 만든다. 여러 번 호출해도 추가 작업이 없다. 이후 play로 다시 활성화할 수 없다. reload, release 뒤 이전 pool의 늦은 load callback은 무시한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmVolumeFeedback.java`
