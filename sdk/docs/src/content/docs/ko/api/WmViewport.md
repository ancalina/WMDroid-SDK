---
title: "WmViewport"
slug: ko/api/wmviewport
---
[API](/ko/api/)

한 논리 window를 물리 화면에 균일 배율로 배치합니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmViewport.java`

```text
public class WmViewport extends ViewGroup
```

## 생성자, setContent, content, addView

```text
public final void setContent(View content);
public final View content();
public WmViewport(Context context);
public void addView(View child,int index,ViewGroup.LayoutParams params);
```

null content는 `IllegalArgumentException`입니다. 기존 child를 교체하고 smoothing layer를 적용합니다. 동일 View는 그대로 유지합니다. content는 현재 child 또는 null입니다. 추가 child는 `IllegalStateException`입니다. 다른 parent에 붙은 View는 먼저 분리하세요. 모든 View 변경은 main thread입니다.

## scalePercent, setScalePercent, getLogicalWidth, getLogicalHeight, getScale

```text
public final int getLogicalWidth();
public final void setScalePercent(int value);
public final float getScale();
public static int scalePercent(int value);
public final int getLogicalHeight();
```

배율 percent는 50–100으로 제한하며 기본 100입니다. 논리 너비는 round(480*100/percent), 실제 배율은 물리 너비/논리 너비입니다. 물리 너비가 없으면 getScale은 0입니다. 화면 높이는 이 균일 배율과 inset에서 계산하며 getLogicalHeight는 측정된 child 높이입니다.

## setTopInset, setBottomInset, getUsableHeight

```text
public final void setTopInset(int pixels);
public final int getUsableHeight();
public final void setBottomInset(int pixels);
```

inset은 물리 픽셀이며 음수는 0입니다. usableHeight는 전체 높이에서 두 inset을 뺀 값이며 최소 0입니다. 좌표 변환에 inset을 다시 적용하지 마세요.

## applyDisplay, screenMode

```text
public final void applyDisplay(Activity owner,int scale,String mode);
public static String screenMode(String value);
```

owner window에 배율, 화면 정책을 적용합니다. mode는 full(전체), black(상단 검은 안전 영역), status(Android 상태바)입니다. 다른 문자열은 full입니다. keyboard 정책은 observeImeInsets 이후 적용합니다.

## observeImeInsets

```text
public final void observeImeInsets();
```

API 30 이상은 ADJUST_NOTHING, IME inset을 사용합니다. 이전 버전은 ADJUST_RESIZE, visible frame 차이로 감지합니다. floating keyboard의 정확한 높이는 이전 경로에서 확인하지 못할 수 있습니다. detach는 observer, bottom inset을 해제합니다.

## setTouchSoundLevel, getTouchSoundLevel

```text
public final void setTouchSoundLevel(int level);
public final int getTouchSoundLevel();
```

0=끄기, 1=작게, 2=크게입니다. 범위 밖은 `IllegalArgumentException`입니다. 처리된 DOWN에만 소리를 냅니다. attach 시 player를 만들고 off, detach 시 해제합니다.

## onViewportBoundsChanged, lifecycle, drawing, touch

```text
public boolean dispatchTouchEvent(MotionEvent event);
protected void onAttachedToWindow();
protected void onDetachedFromWindow();
protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight);
protected void onMeasure(int widthSpec,int heightSpec);
protected void dispatchDraw(Canvas canvas);
protected void onLayout(boolean changed,int left,int top,int right,int bottom);
protected void onViewportBoundsChanged();
```

크기, 배율, inset 변경 때 호출하는 protected hook입니다. subclass의 좌표 의존 feedback을 취소하세요.
