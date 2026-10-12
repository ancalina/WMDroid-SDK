---
title: "WmTouchFeedback"
slug: ko/api/wmtouchfeedback
---
[API 색인](/ko/api/)

```java
public final class WmTouchFeedback
```

소유자가 처리한 터치의 soft/loud 효과음을 재생한다. 소유자가 터치 처리와 음량 설정에 맞춰 `play`를 호출한다. 출력은 기기 media 음량, 무음을 따르며 전역 음량을 바꾸지 않는다.

## 생성자

```java
public WmTouchFeedback(Context context);
```

최대 동시 재생 `1`인 SoundPool에 `sounds/screen-tap/soft.wav`, `sounds/screen-tap/loud.wav`를 준비한다. 파일 준비는 동기이며 파일당 상한은 `4 MiB`다. worker 스레드에서 생성한다. 준비 중 IOException은 해당 소리를 사용할 수 없음으로 처리한다. SoundPool load 완료는 비동기다.

API 21 미만은 STREAM_MUSIC 생성자를 사용한다. API 21 이상은 같은 legacy stream을 지정한 AudioAttributes와 CONTENT_TYPE_SONIFICATION을 사용한다. 생성 시 준비한 소리를 유지하므로 변경된 리소스를 사용하려면 기존 객체를 release하고 새 객체를 만든다.

## play

```java
public synchronized void play(int level);
```

| level | 재생 |
| --- | --- |
| 1 | soft |
| 2 | loud |
| 그 외 | 재생하지 않음 |

성공적으로 load된 소리만 재생한다. load 미완료, 실패, 파일 준비 실패, release 후 호출은 건너뛴다. 건너뛴 재생은 load 완료 후 자동 실행하지 않는다. 이후 터치에서 다시 호출한다.

좌우 gain=`1`, priority=`1`, loop=`0`, rate=`1`로 한 번 재생한다. 실제 성공 여부는 반환하지 않는다. play, release, load 완료 처리는 객체 잠금으로 직렬화한다.

## release

```java
public synchronized void release();
```

소유자가 사용을 끝낼 때 호출한다. load listener, 준비 상태, pool과 재생을 해제한다. 여러 번 호출해도 추가 작업이 없으며 이후 play는 아무 작업도 하지 않는다. 해제 전에 대기 중이던 load callback도 released 상태에서는 준비 목록을 갱신하지 않는다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmTouchFeedback.java`
