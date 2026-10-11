---
title: "WmPointer"
slug: ko/api/wmpointer
---
[API](/ko/api/)

논리 화면 안에서 상대 이동·탭 클릭을 제공하는 핑거마우스 View입니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmPointer.java`

## 선언

```text
public final class WmPointer extends View
public WmPointer(Context context,FrameLayout target,Runnable disable);
public float pointerX();
public float pointerY();
protected void onDraw(Canvas canvas);
public boolean onTouchEvent(MotionEvent event);
public boolean performClick();
protected void onDetachedFromWindow();
```

target은 이벤트를 받을 FrameLayout, disable은 길게 눌렀을 때 실행할 callback입니다. target 좌표와 pointer View 좌표가 일치하도록 배치하세요. 생성·이벤트는 main thread에서 처리합니다.

pointerX·pointerY는 논리 픽셀 좌표입니다. 시작은 (240,320)입니다. 이동은 현재 View 경계 안으로 제한합니다. MOVE에서 시작점 기준 `abs(x−startX)+abs(y−startY)>8`이면 탭·길게 누르기를 취소합니다. 단위는 논리 픽셀이며 누적 이동 거리가 아닙니다. 한 번 초과하면 시작점으로 돌아와도 그 gesture의 취소는 유지됩니다. 길게 누르기 시간은 ViewConfiguration.getLongPressTimeout()입니다.

`performClick()`은 target에 DOWN·UP을 전달합니다. 두 이벤트 시간 차이는 20 ms입니다. 다중 터치는 해당 gesture 전체를 취소합니다. detach는 대기 중 길게 누르기를 취소합니다. 화면 밖 앱 입력은 전달하지 않습니다.
