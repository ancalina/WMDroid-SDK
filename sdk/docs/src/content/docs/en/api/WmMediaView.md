---
title: "WmMediaView"
slug: en/api/wmmediaview
---
[API index](/en/api/)

```java
public final class WmMediaView extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener,WmControls.BackHandler
```

Audio/video client backed by Android MediaPlayer, with a queue, Now Playing, Library, and Properties panels. The owner supplies source selection, library data, and shell menus. Supported formats depend on device Android decoders. Video uses TextureView and requires hardware acceleration in the host window.

## Constructor

```java
public WmMediaView(Context context,Typeface font)
public WmMediaView(Context context,Typeface font,Typeface bold)
```

The two-argument constructor derives bold from font. Construct/operate on the UI thread. Initial state is EMPTY, volume 70%, with mute/repeat/shuffle=false. An internal serial worker handles source setup, bitmaps, metadata reads, and decoder release. The owner must obtain content read access and INTERNET permission for HTTP(S). Bitmap skins load from the resource pack; absent skins use colors/symbols. Skin and icon results use a main Looper Handler and can apply before window attachment. Bitmaps arriving after CLOSED are recycled.

## State and Listener

```java
public enum State { EMPTY, PREPARING, READY, PLAYING, PAUSED, ENDED, ERROR, CLOSED }
public interface Listener { void changed(WmMediaView view); }
public void setListener(Listener value)
public State state()
public int duration()
public int position()
public String error()
public String title()
```

EMPTY means no source; PREPARING is asynchronous setup; READY is playable; PLAYING is active playback; PAUSED is paused; ENDED reached the end; ERROR is playback failure; CLOSED is permanent closure. Duration/position use milliseconds and start at 0 before a source is prepared. Position refreshes about every 200ms during playback. Error is the last error string; title is the current source title. `changed` runs on state/queue/control refresh; null disables it. Callback counts are not playback-progress event counts.

## Sources and queue

```java
public void open(Uri uri,String title)
public WmMediaQueue.Entry[] playlist()
public int currentIndex()
public void setPlaylist(WmMediaQueue.Entry[] entries,int index,boolean autoPlay)
public void enqueue(WmMediaQueue.Entry[] entries)
public void playIndex(int index)
public void removeSource(int index)
public void moveSource(int from,int to)
public void clearPlaylist()
```

Sources follow `WmMediaQueue.Entry`: content or HTTP(S) URI. `open` replaces the queue with one source, requests autoplay, and hides Now Playing. `playlist` returns an array copy; currentIndex is -1 when absent. `setPlaylist` requires index=-1 to clear the source or an in-range index; autoPlay requests playback after preparation. Enqueue keeps the current source. PlayIndex plays an already-prepared current entry or prepares another entry for playback. Removing the current occurrence prepares its replacement using the existing playback intent; moving does not replace the decoder. ClearPlaylist returns to EMPTY. Invalid queue input/indices throw `IllegalArgumentException`; setPlaylist/enqueue/playIndex/removeSource/moveSource throw `IllegalStateException` in CLOSED. Asynchronous source-setup failures use a main Looper Handler and enter ERROR only while that source is still current.

## Playback

```java
public void play()
public void pause()
public void toggle()
public void stop()
public void seekTo(int millis)
```

Play records autoplay intent while PREPARING; when prepared it starts only after obtaining audio focus. Denied focus leaves playback stopped. Play from ENDED seeks to 0. Pause clears autoplay intent and, while PLAYING, enters PAUSED and abandons audio focus. Toggle pauses PLAYING, otherwise plays. Stop keeps the prepared source, seeks to 0, and enters READY. SeekTo clamps to 0..duration milliseconds when prepared and changes ENDED to PAUSED. Stop clears autoplay intent. During PREPARING, source preparation continues and waits in READY on completion. SeekTo does nothing when unprepared. Both transient and permanent audio-focus loss pause playback.

## Next, shuffle, repeat

```java
public boolean shuffle()
public void setShuffle(boolean value)
public boolean repeat()
public void setRepeat(boolean value)
public boolean canNext()
public boolean canPrevious()
public void next()
public void previous()
```

Shuffle uses the [WmMediaQueue](/en/api/wmmediaqueue/) permutation. Repeat wraps the queue and uses MediaPlayer looping for a single-entry queue. Next/previous change source when available and autoplay only if currently PLAYING. Natural completion autoplays the next entry or enters ENDED when none remains. CanNext can be true before any entry is selected.

## Volume and video

```java
public int volume()
public void setVolume(int percent)
public boolean muted()
public void setMuted(boolean value)
public boolean hasVideo()
public void setFullScreen(boolean value)
public boolean fullScreen()
```

Volume clamps player gain to 0..100% without changing system stream volume. Mute applies zero gain while retaining volume. HasVideo is true when prepared with positive video dimensions. SetFullScreen(true) does nothing without video. FullScreen fills this view’s video area; it does not change Activity/system-bar mode.

## Panels and owner commands

```java
public void setPlaylistCommandListener(WmMediaPlaylistView.Listener value)
@Deprecated public void setPlaylistSaveEnabled(boolean value)
public int selectedIndex()
public void selectPlaylist(int index)
public void playSelection()
public WmMediaLibraryView library()
public boolean isLibraryVisible()
public void showLibrary(boolean value)
public boolean isPlaylistVisible()
public void showPlaylist(boolean value)
public WmMediaPropertiesView properties()
public boolean isPropertiesVisible()
public void showProperties(int index)
public boolean back()
```

SelectedIndex/selectPlaylist address Now Playing selection, separate from currentIndex. PlaySelection plays the selection and hides the list. UP/DOWN/REMOVE/PLAY are handled internally; other playlist/library commands go to the owner listener, disabled by null. SetPlaylistSaveEnabled is a no-op compatibility API. ShowLibrary(true) hides the playlist; showPlaylist hides the library; both dismiss Properties. ShowProperties first displays the valid queue entry's title/URI and its positive duration if already known for the current entry. Only content URIs receive additional metadata reads on the worker; HTTP(S) receives basic information without a separate download. Unreadable fields can remain absent. `WmMediaPropertiesView.setValues` instead accepts strings supplied by the caller; a main Looper Handler applies results only while Properties is visible and the request generation matches. Results after CLOSED or a changed panel request are ignored. It retains the underlying panel/decoder. Back dismisses input dialogs, Properties, Library, Now Playing, then fullscreen, returning true when handled. Panel-show calls after CLOSED do nothing.

## Lifetime and geometry

```java
public void close()
protected void onDetachedFromWindow()
protected void onMeasure(int width,int height)
protected void onLayout(boolean changed,int l,int t,int r,int b)
```

Detachment only pauses. Call close when permanently discarding the client: it enters CLOSED, clears callbacks, releases icons/skins/Surface and audio focus, queues decoder release on the serial worker, then shuts it down. Close is idempotent. Do not reuse the closed view/panels. The base player client is 480×536 logical pixels; smaller surfaces shrink proportionally and additional height extends the middle area.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String value)
```

Restyling applies player body/title fonts. Language changes update player controls, seek accessibility text, and status captions. Shared `WmControls.restyleTree` traversal restyles Library/Now Playing/Properties children.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmMediaView.java`
