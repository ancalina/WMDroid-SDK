---
title: "WmIcons"
slug: en/api/wmicons
---
[API](/en/api/)

Loads status icons from the active resource pack or bundled fallbacks.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmIcons.java`

## Declaration

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

IDs: network_connected, network_disconnected, network_data_connected, cellular_off, cellular_ready, radio_off, start, phone, volume_system, volume_ring, volume, volume_muted, volume_vibrate, signal, battery_charge, battery_full, battery_medium, battery_low, battery_half, battery_critical, bluetooth, wireless, lock, and sip_keyboard. Other values throw `IllegalArgumentException`.

`load(context,id)` tries pack vector, pack raster, then bundled drawing. start tries raster first. A vector with incorrect intrinsic dimensions uses the next fallback. Logical dimensions are 41×36 for start, 64×64 for phone, and 32×32 for other IDs.

`load(id)` does not read the pack. wireless and lock use Drawables with the existing manually authored vectors; phone uses a system-emoji fallback; other IDs use bundled Drawables. `fallback("bluetooth")` returns the bundled scalable drawing; other IDs return an emoji BitmapDrawable. Call `prepare()` on a worker after pack changes to prepare parsing and decoding before the first UI lookup.

Drawables render within their bounds. Use a separate instance for each consumer. Callers must keep the bundled `setAlpha()` value within 0–255. `getOpacity()` returns TRANSLUCENT.
