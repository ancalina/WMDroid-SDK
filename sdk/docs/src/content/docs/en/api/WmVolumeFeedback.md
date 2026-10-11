---
title: "WmVolumeFeedback"
slug: en/api/wmvolumefeedback
---
[API index](/en/api/)

```java
public class WmVolumeFeedback
```

Plays media/ring preview sounds for volume-control steps. Does not change global volume or request Android permissions.

## Constructor

```java
public WmVolumeFeedback(Context context);
```

Retains the application Context and creates a SoundPool with maximum `1` simultaneous stream for each route. Asynchronously loads the three WAV resources below into each pool. File preparation is synchronous, with a `4 MiB` limit per file. Construct and call `play` after resource changes on a worker thread. An IOException during file preparation makes that sound unavailable.

| Android API | media | ring |
| --- | --- | --- |
| Before 21 | STREAM_MUSIC | STREAM_RING |
| 21 or later | USAGE_MEDIA | USAGE_NOTIFICATION_RINGTONE |

API 21 or later uses CONTENT_TYPE_SONIFICATION. Actual output also follows the system volume setting for the selected route.

## play

```java
public synchronized void play(boolean ring,int step);
```

`ring=false` selects the media pool; `true` selects the ring pool. Steps map to these sounds.

| step | Resource |
| --- | --- |
| 0 or less | No playback |
| 1 | sounds/volume/quietest.wav |
| 5 | sounds/volume/loudest.wav |
| Other positive values | sounds/volume/default.wav |

A resource revision change releases existing pools/playback and prepares new pools. This check runs before `step<=0`. Sounds without a successful completed load are skipped, with no automatic playback when loading completes. Call again if needed.

Skips playback less than `250 ms` after that pool's recorded time. Time uses uptime; the initial and post-reload value is `0`. Only an attempted playback of a ready sound records the current time. Skipped calls do not update it.

Stops the previous preview on the same pool immediately before playback. Plays once with left/right gain=`1`, priority=`1`, loop=`0`, and rate=`1`. Does not return playback success. Calls after release do nothing. play and release are serialized by the object lock.

## release

```java
public synchronized void release();
```

Releases both pools' load listeners, playback, and resources, ending use of this object. Repeated calls do no additional work. Later play calls cannot reactivate it. Late load callbacks from old pools are ignored after reload or release.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmVolumeFeedback.java`
