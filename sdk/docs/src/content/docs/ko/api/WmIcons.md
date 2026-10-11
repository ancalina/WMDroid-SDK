---
title: "WmIcons"
slug: ko/api/wmicons
---
[API](/ko/api/)

상태 아이콘을 활성 리소스팩 또는 내장 대체 그림에서 불러옵니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmIcons.java`

## 선언

```text
public final class WmIcons extends Drawable
public static Drawable load(android.content.Context context,String id);
public static void prepare(android.content.Context context);
public static Drawable fallback(String id);
public static Drawable load(String id);
public void draw(Canvas canvas);
public int getIntrinsicWidth();
public int getIntrinsicHeight();
public void setAlpha(int value);
public void setColorFilter(ColorFilter filter);
public int getOpacity();
```

id: network_connected, network_disconnected, network_data_connected, cellular_off, cellular_ready, radio_off, start, phone, volume_system, volume_ring, volume, volume_muted, volume_vibrate, signal, battery_charge, battery_full, battery_medium, battery_low, battery_half, battery_critical, bluetooth, wireless, lock, sip_keyboard. 다른 값은 `IllegalArgumentException`입니다.

`load(context,id)`는 pack vector→pack raster→내장 그림 순서로 조회합니다. start는 raster를 먼저 사용합니다. vector intrinsic 크기가 규격과 다르면 다음 fallback을 사용합니다. start는 41×36, phone은 64×64, 나머지는 32×32 논리 픽셀입니다.

`load(id)`는 팩을 조회하지 않습니다. wireless·lock은 기존 수작업 벡터를 내장한 Drawable, phone은 시스템 emoji 대체 그림, 나머지는 내장 Drawable입니다. `fallback("bluetooth")`는 내장 scalable 그림을 반환하며, 다른 id는 emoji BitmapDrawable을 반환합니다. `prepare()`는 팩 변경 후 worker에서 호출해 첫 UI 조회의 파싱·decode를 준비하세요.

Drawable은 bounds에 맞춰 그립니다. 각 소비자에 별도 인스턴스를 사용하세요. 내장 `setAlpha()`는 caller가 0–255 범위를 지켜야 합니다. `getOpacity()`는 TRANSLUCENT입니다.
