---
title: "WmMediaView"
slug: ko/api/wmmediaview
---
[API 색인](/ko/api/)

```java
public final class WmMediaView extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener,WmControls.BackHandler
```

Android MediaPlayer 기반 오디오/비디오 클라이언트. 재생 큐·지금 재생·라이브러리·속성 패널을 묶는다. 소유 앱이 소스 선택, 라이브러리 데이터, 셸 메뉴를 제공한다. 지원 형식은 기기의 Android decoder에 따른다. 영상은 TextureView를 사용하므로 호스트 창의 하드웨어 가속이 필요하다.

## 생성자

```java
public WmMediaView(Context context,Typeface font)
public WmMediaView(Context context,Typeface font,Typeface bold)
```

두 인수 생성자는 font에서 bold를 만든다. UI 스레드에서 생성/조작한다. 초기 상태 EMPTY, 볼륨 70%, mute/repeat/shuffle=false. 내부 직렬 worker는 소스 설정·비트맵·메타데이터 읽기·decoder 해제에 사용한다. 소유 앱은 content 읽기 권한과 HTTP(S)용 INTERNET 권한을 확보해야 한다. 비트맵 스킨은 리소스팩에서 읽고 없으면 색/기호로 표시한다. 스킨·아이콘 결과는 main Looper Handler로 전달해 window에 붙기 전에도 적용한다. CLOSED 뒤 도착한 비트맵은 recycle한다.

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

EMPTY=소스 없음, PREPARING=비동기 준비, READY=재생 가능, PLAYING=재생 중, PAUSED=일시 중지, ENDED=끝 도달, ERROR=재생 오류, CLOSED=영구 종료. duration/position 단위는 ms, 새 소스 준비 전 0. position은 재생 중 약 200ms 간격으로 갱신. error는 마지막 오류 문자열, title은 현재 소스 제목. `changed`는 상태/큐/컨트롤 refresh마다 호출하며 null listener는 해제. 콜백 수를 재생 진행 이벤트 수로 해석하지 않는다.

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

소스 계약은 `WmMediaQueue.Entry`: content 또는 HTTP(S) URI. `open`은 큐를 한 항목으로 교체해 자동 재생하고 지금 재생 패널을 숨긴다. `playlist`는 배열 복사, currentIndex는 없음 -1. `setPlaylist`의 index=-1은 소스 제거, 그 외는 범위 내 값; autoPlay는 준비 후 재생 요청. enqueue는 현재 소스 유지. playIndex는 준비된 현재 항목이면 play, 다른 항목은 새로 준비 후 재생. 현재 발생 제거 시 다음 선택을 기존 재생 의도에 맞춰 준비; move는 decoder를 교체하지 않는다. clearPlaylist는 EMPTY로 복귀. 큐의 잘못된 입력/인덱스는 `IllegalArgumentException`, CLOSED에서 setPlaylist/enqueue/playIndex/removeSource/moveSource는 `IllegalStateException`. 비동기 소스 준비 실패는 main Looper Handler로 전달하며 해당 소스가 아직 현재 소스일 때만 ERROR로 전환한다.

## Playback

```java
public void play()
public void pause()
public void toggle()
public void stop()
public void seekTo(int millis)
```

play는 PREPARING에서 준비 후 재생 요청을 저장하고, 준비된 상태에서는 audio focus 확보 후 시작. focus 거부면 시작하지 않는다. ENDED에서 play는 0으로 돌아간다. pause는 준비 후 자동 재생 요청도 해제하며 PLAYING이면 PAUSED 전환·audio focus 반납. toggle은 PLAYING이면 pause, 아니면 play. stop은 준비된 소스를 유지한 채 0으로 이동해 READY. seekTo는 준비된 상태에서 0..duration ms로 clamp; ENDED이면 PAUSED. stop은 준비 후 자동 재생 요청을 해제한다. PREPARING에서 호출하면 준비는 계속되고 완료 후 READY에서 대기한다. 준비되지 않은 상태의 seekTo는 무동작. 영구/일시 audio focus 손실 모두 pause한다.

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

shuffle은 [WmMediaQueue](/ko/api/wmmediaqueue/) permutation, repeat는 큐 경계 순환이며 한 항목 큐에서는 MediaPlayer looping. 다음/이전은 가능한 경우 소스를 바꾸고 현재 PLAYING 상태일 때만 자동 재생. 자연 완료는 다음 항목을 자동 재생하고 더 없으면 ENDED. 초기 선택 없음에서 canNext=true일 수 있다.

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

볼륨은 player gain 0..100%로 clamp하며 시스템 스트림 볼륨을 바꾸지 않는다. mute는 gain을 0으로 만들고 저장한 volume은 유지. hasVideo는 준비된 상태이며 폭/높이 양수일 때 true. 비디오가 없으면 setFullScreen(true)는 무동작. fullScreen은 이 뷰의 비디오 영역을 채우며 Activity/system bar 모드는 바꾸지 않는다.

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

selectedIndex/selectPlaylist는 지금 재생 목록의 선택이며 currentIndex와 별개. playSelection은 선택을 재생하고 목록 숨김. UP/DOWN/REMOVE/PLAY는 내부 처리, 다른 지금 재생 명령과 라이브러리 명령은 owner listener에 전달; null 해제. setPlaylistSaveEnabled는 무동작 호환 API. showLibrary(true)는 목록 숨김, showPlaylist는 라이브러리 숨김; 둘 다 속성 패널 닫기. showProperties는 유효 큐 인덱스의 제목·URI를 먼저 표시하며 현재 항목의 양수 duration을 알고 있으면 길이도 표시한다. content URI만 worker에서 추가 메타데이터를 읽고 HTTP(S)는 별도 다운로드 없이 기본 정보만 표시한다. 읽을 수 없는 개별 정보는 빠질 수 있다. `WmMediaPropertiesView.setValues`는 호출자가 문자열을 직접 공급하는 API이다; main Looper Handler에서 속성 패널이 보이고 요청 generation이 일치할 때만 결과를 적용한다. CLOSED 뒤 또는 패널 요청이 바뀐 뒤 결과는 무시한다. underlying panel/decoder는 유지. Back은 입력 dialog, 속성, 라이브러리, 목록, fullscreen 순서로 닫고 처리한 경우 true. CLOSED의 show 패널 호출은 무동작.

## Lifetime and geometry

```java
public void close()
protected void onDetachedFromWindow()
protected void onMeasure(int width,int height)
protected void onLayout(boolean changed,int l,int t,int r,int b)
```

detach는 pause만 수행한다. 영구 폐기 시 close 필수: CLOSED 전환·콜백 제거·아이콘/스킨/Surface 해제·audio focus 반납·직렬 worker에 decoder 해제 예약 후 shutdown. close 반복 호출 가능. 종료 후 뷰/패널을 재사용하지 않는다. 기본 player client는 480×536 논리 픽셀; 좁은 공간에서는 종횡비를 유지해 축소하고 추가 높이는 화면 중간에 반영한다.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String value)
```

재스타일은 player의 본문/제목 글꼴 적용. 언어 변경은 player 버튼·재생 위치·상태 캡션에 적용한다. 라이브러리/목록/속성 자식의 재스타일은 공통 `WmControls.restyleTree` 경로에서 처리한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmMediaView.java`
