---
title: "WmPaint"
slug: ko/api/wmpaint
---
[API](/ko/api/)

옛 기기 동작 설정에 따라 client 영역을 위에서 아래로 표시하는 컨테이너.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmPaint.java`

```text
public class WmPaint extends FrameLayout
```

## Options

```text
public static final class Options
public boolean enabled = true;
public boolean scan = true;
public boolean repaint = false;
public int delay = 120;
public int lines = 64;
public int scale = 1000;
```

## Role

```text
public enum Role { MENU, POPUP, FORM, GRID, CALENDAR }
```

## 생성자 · readOptions

```text
public WmPaint(Context context,boolean delayAlreadyServed);
public WmPaint(Context context,boolean delayAlreadyServed,Role role);
protected Options readOptions();
```

생성·변경은 UI thread에서 수행하세요. 기본 role은 `FORM`입니다. role을 지정할 때 null을 전달하지 마세요. `delayAlreadyServed`는 attach 시 초기 대기 시간을 생략합니다. `readOptions()`는 현재 `WmShell.State`를 읽습니다. subclass는 이 hook에서 다른 Options를 반환할 수 있습니다.

## Options · Role

```text
public static Options from(WmShell.State state);
```

`enabled`는 기능 사용, `scan`은 위에서 아래로 표시, `repaint`는 표시 완료 후 한 번 깜빡임입니다. `delay` 기본 120 ms, `lines` 기본 64 논리 픽셀, `scale` 기본 1000입니다. scale 1000은 1배입니다. `Options.from(state)`는 현재 설정을 새 Options로 복사합니다.

실행 시 raw delay는 0–1500 ms, lines는 8–256, scale은 250–4000으로 제한합니다. 초기 대기는 `round(clamp(delay,0,1500)*clamp(scale,250,4000)/1000)` ms이며 최대 6000 ms입니다. delayServed이면 0입니다. 각 band의 기본 간격은 16 ms입니다. Role 배수는 MENU 0.5, POPUP 1, FORM 1.25, GRID 1.5, CALENDAR 1.75입니다. band 간격에 role과 scale을 곱합니다. MENU의 초기 delay는 1/4입니다. repaint는 완료 후 60 ms 대기와 40 ms 공백에 scale을 적용합니다.

## replay

```text
public void replay();
public void replay(boolean delayServed);
public void replay(View client);
public void replay(View client,boolean delayServed);
```

인자 없는 호출은 전체 영역을 다시 표시합니다. `delayServed=true`는 초기 delay를 생략합니다. client 인자는 이 컨테이너의 자손 View여야 합니다. null·자신·외부 View는 `IllegalArgumentException`입니다. client 밖은 유지하고 client 안만 다시 표시합니다. window에 연결되지 않았거나 보이지 않으면 실행하지 않습니다. layout을 기다린 뒤 실제 clip 영역을 사용합니다. disabled이면 표시를 지연하지 않습니다.

## isActive · cancel · stopReplay

```text
public boolean isActive();
public static void stopReplay(View root);
public void cancel();
```

`isActive()`는 표시 진행 중인지 반환합니다. `cancel()`은 예약 callback·가려진 영역·gesture 차단을 해제합니다. `stopReplay(root)`는 root와 자손의 활성 표시를 완료합니다. 진행 중이던 touch는 UP 또는 CANCEL까지 차단합니다.

## lifecycle · drawing · touch

```text
protected void onAttachedToWindow();
protected void onLayout(boolean changed,int left,int top,int right,int bottom);
protected void onDetachedFromWindow();
protected void onVisibilityChanged(View changedView,int visibility);
protected void onWindowVisibilityChanged(int visibility);
protected void dispatchDraw(Canvas canvas);
public boolean dispatchTouchEvent(MotionEvent event);
```

attach 시 생성자 설정으로 표시를 시작합니다. detach 또는 숨김 상태는 예약 작업을 취소합니다. 진행 중 touch는 소비합니다. 이미 자식으로 전달한 gesture 중 표시를 시작하면 자식에게 CANCEL을 보내고 남은 gesture를 소비합니다. lifecycle·drawing·touch override는 superclass를 호출해 이 상태를 유지하세요.
