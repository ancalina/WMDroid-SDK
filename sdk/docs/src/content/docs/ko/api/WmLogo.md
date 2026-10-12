---
title: "WmLogo"
slug: ko/api/wmlogo
---
[API](/ko/api/)

시작 버튼 artwork를 bounds에 맞춰 표시하는 Drawable.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmLogo.java`

## 선언

```text
public final class WmLogo extends Drawable
public WmLogo();
public WmLogo(Context context);
protected void onBoundsChange(Rect bounds);
public void draw(Canvas canvas);
public int getIntrinsicWidth();
public int getIntrinsicHeight();
public void setAlpha(int value);
public void setColorFilter(ColorFilter value);
public int getOpacity();
```

인자 없는 constructor는 fallback을 사용합니다. Context constructor는 현재 pack의 start icon을 사용합니다.

intrinsic size는41×36입니다. raster는 종횡비를 유지하며 bounds 중앙에 배치합니다. WmVectorDrawable은 bounds 전체를 사용합니다. alpha, colorFilter, opacity는 내부 artwork에 위임합니다.
