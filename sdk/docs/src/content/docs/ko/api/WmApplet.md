---
title: "WmApplet"
slug: ko/api/wmapplet
---
[API](/ko/api/)

공유 제목바·명령바·시작 메뉴를 가진 Activity 기본 클래스입니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmApplet.java`

```text
public abstract class WmApplet extends Activity
public static final String LANGUAGE = "me.ancal.wmdroid.sdk.LANGUAGE";
public static final String ACCENT = "me.ancal.wmdroid.sdk.ACCENT";
public static final String FONT = "me.ancal.wmdroid.sdk.FONT";
public static final String HYBRID = "me.ancal.wmdroid.sdk.HYBRID";
protected LinearLayout content;
protected Typeface regular;
protected Typeface bold;
protected int accent;
protected WmWindow shellWindow;
```

## onCreate(Bundle) · lifecycle

```text
public void onCreate(Bundle saved);
protected void onResume();
protected void onPause();
protected void onDestroy();
```

subclass는 super.onCreate 후 content에 본문 View를 추가합니다. regular·bold·accent·shellWindow는 그때부터 사용할 수 있습니다. content는 세로 LinearLayout입니다. 등록 예제는 [애플릿](/ko/applets/)을 참고하세요. View·hook·callback은 main thread입니다.

resume은 host 상태·폰트·리소스를 갱신하고 시계·tray·STATE observer를 시작합니다. pause는 observer·timer·열린 메뉴·팝업을 정리합니다. destroy는 font worker를 종료합니다. lifecycle override는 superclass를 호출하세요.

## onShellStateChanged(State) · onResourcesChanged()

```text
protected void onShellStateChanged(WmShell.State state);
protected void onResourcesChanged();
```

외형 상태 적용 후 hook을 호출합니다. 본문 언어·앱 소유 이미지 캐시를 갱신하세요. editor를 재생성할 필요는 없습니다. 리소스 동기화는 worker에서 수행합니다. 최초 font는 onCreate의 상태 적용 중 main thread에서 동기 로드될 수 있습니다. 이후 cache가 준비되지 않은 font 전환은 worker에서 준비합니다. hook은 main thread에서 실행합니다. onResourcesChanged는 chrome이 관찰한 리소스 revision이 바뀔 때 호출하며 최초 적용도 포함합니다. 이어 onShellStateChanged를 상태 적용마다 호출하므로 값이 같아도 다시 호출될 수 있습니다. font 준비 완료 통지는 아니며 worker 완료 뒤 상태를 다시 적용하면 hook도 다시 호출될 수 있습니다.

## caption(String) · label(String) · entry(String, Runnable)

```text
protected final void caption(String value);
protected final TextView label(String value);
protected final TextView entry(String value,Runnable action);
```

caption은 제목을 변경합니다. label은 공통 font의 새 TextView를 반환합니다. entry는 선택 배경·padding·click action을 적용한 새 TextView입니다. action은 필수입니다. 반환 View를 caller가 content에 추가합니다.

## closeButton(boolean, Runnable)

```text
protected final void closeButton(boolean ok,Runnable action);
```

ok=true는 OK, false는 X입니다. action=null은 finish입니다. 활성 WmInputDialog가 있으면 OK로 입력 accept를 수행합니다. modal owner가 차단하면 close action을 실행하지 않습니다.

## softKeys(...) · softKeyMenu(...)

```text
protected final TextView softKey(String value,boolean right,Runnable action);
protected final void softKeys(String left,Runnable leftAction,String right,Runnable rightAction);
protected final void softKeyMenu(boolean right,String caption,Runnable action);
```

softKeys는 좌우 명령을 교체하며 기존 command menu를 닫습니다. action=null은 disabled입니다. softKeyMenu는 DOWN에서 여는 메뉴 key입니다. softKey는 별도 TextView를 반환하며 footer에 자동 삽입하지 않습니다.

## showCommandMenu(...) · dismissCommandMenu()

```text
protected final WmCommandMenu showCommandMenu(boolean right,String[] labels,int selected,WmRadioMenu.Selection action);
protected final WmCommandMenu showCommandMenu(boolean right,WmMenuItem[] items,WmRadioMenu.Selection action);
protected final void dismissCommandMenu();
```

flat overload는 row index, tree overload는 leaf id를 callback에 전달합니다. Start·팝업·기존 menu를 먼저 닫습니다. 반환 menu의 수명은 dismiss·pause까지입니다. selected=-1은 radio 표시 없음입니다. 자세한 인자 규격은 [WmCommandMenu](/ko/api/wmcommandmenu/)를 따릅니다.

## showPopup(String, View, int, int) · dismissPopup()

```text
protected final WmPopup showPopup(String caption,View body,int width,int anchor);
protected final void dismissPopup();
```

width는 1–480, anchor는 논리 surface의 x좌표입니다. 범위 밖 width는 `IllegalArgumentException`입니다. body는 parent 없는 View입니다. Start·기존 팝업·명령 menu를 먼저 닫고 body에 현재 restyle을 적용합니다. 외부 tap·Back·pause·Start로 닫힙니다. 닫힌 반환 객체에 비동기 UI 갱신을 하지 마세요.

## onTray(int, int) · launchSystem(Intent)

```text
protected void onTray(final int kind,int anchor);
protected void launchSystem(Intent intent);
```

기본 tray hook은 연결·볼륨·전원·전화 panel을 엽니다. kind는 WmTraySlot 상수입니다. launchSystem은 애니메이션 없이 시스템 Activity를 시도하며 실패하면 기본 Settings를 시도합니다. 앱별 navigation이 필요하면 override하세요.

## onClock() · onClockMenu() · onClockPanelShown(WmClockPanel)

```text
protected void onClock();
protected void onClockMenu();
protected void onClockPanelShown(WmClockPanel panel);
protected final void analogClock(boolean analog);
```

기본 clock popup은 7초 후 닫힙니다. onClockPanelShown에서 앱 자신의 권한·일정 데이터를 제공합니다. 비동기 작업은 panel detach 후 업데이트를 취소·무시하세요. clock menu는 공유 analog 선택을 저장합니다. analogClock은 현재 applet 표시만 변경합니다.

## shade(int) · tab(...) · tabSelected(...) · viewport()

```text
protected final int shade(int original);
protected final TextView tab(String value,boolean selected,Runnable action);
protected final void tabSelected(TextView tab,boolean selected);
protected final WmViewport viewport();
```

shade는 원본 ARGB 색의 hue를 accent에 맞추고 무채색 accent에는 grayscale을 적용합니다. tab은 새 탭 TextView를 반환합니다. tabSelected는 기존 선택 표시를 갱신합니다. viewport는 배율·inset용 WmViewport 참조입니다.

## onBackPressed() · finish()

```text
public void onBackPressed();
public void finish();
```

Back은 child command menu→팝업→Start→본문 BackHandler→finish 순서입니다. finish는 전환 animation 0을 요청합니다.
