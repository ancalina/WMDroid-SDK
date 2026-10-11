---
title: "WmTouchFeedback"
slug: en/api/wmtouchfeedback
---
[API index](/en/api/)

```java
public final class WmTouchFeedback
```

Plays soft/loud feedback for touches handled by the owner. The owner calls `play` according to touch handling and the sound-level setting. Output follows device media volume and mute; global volume is unchanged.

## Constructor

```java
public WmTouchFeedback(Context context);
```

Prepares `sounds/screen-tap/soft.wav` and `sounds/screen-tap/loud.wav` in a SoundPool with maximum `1` simultaneous stream. File preparation is synchronous, with a `4 MiB` limit per file. Construct on a worker thread. An IOException during preparation makes that sound unavailable. SoundPool loading completes asynchronously.

Before API 21, uses the STREAM_MUSIC constructor. API 21 or later uses AudioAttributes with the same legacy stream and CONTENT_TYPE_SONIFICATION. Retains sounds prepared at construction; release the old object and create a new one to use changed resources.

## play

```java
public synchronized void play(int level);
```

| level | Playback |
| --- | --- |
| 1 | soft |
| 2 | loud |
| Other values | No playback |

Only successfully loaded sounds play. Skips incomplete/failed loads, file-preparation failures, and calls after release. Skipped playback does not run automatically when loading completes. Call again on a later touch.

Plays once with left/right gain=`1`, priority=`1`, loop=`0`, and rate=`1`. Does not return playback success. play, release, and load completion are serialized by the object lock.

## release

```java
public synchronized void release();
```

Call when the owner finishes using the object. Releases the load listener, ready state, pool, and playback. Repeated calls do no additional work; later play calls do nothing. A load callback queued before release cannot update readiness while released.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmTouchFeedback.java`
