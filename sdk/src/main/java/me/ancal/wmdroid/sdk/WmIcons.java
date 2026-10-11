package me.ancal.wmdroid.sdk;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.util.ArrayList;
/** Authored status and Today icons. Draws paths at display scale. */
public final class WmIcons extends Drawable {
 private final ArrayList<Path> paths=new ArrayList<>();
 private final ArrayList<Paint> paints=new ArrayList<>();
 private final ArrayList<Integer> alphas=new ArrayList<>();
 private int opacity=255;
 private boolean pixelGrid;
 private final Matrix matrix=new Matrix();
 private final float[] transform=new float[9];
 private float width=32,height=32;
 private static final String[] IDS={"network_connected","network_disconnected","network_data_connected","cellular_off","cellular_ready","radio_off","start","phone","volume_system","volume_ring","volume","volume_muted","volume_vibrate","signal","battery_charge","battery_full","battery_medium","battery_low","battery_half","battery_critical","wireless","lock","bluetooth","sip_keyboard"};
 private static void known(String id){for(String name:IDS)if(name.equals(id))return;throw new IllegalArgumentException("Unknown WM icon: "+id);}
 /** Pack overrides take priority over bundled status and Today drawings. */
 public static Drawable load(android.content.Context context,String id){
  known(id);
  if("start".equals(id))try{return raster(context,id);}catch(java.io.IOException absent){}
  try{
   Drawable icon=WmResources.vector(context,"vectors/"+id);
   int width="start".equals(id)?41:"phone".equals(id)?64:32,height="start".equals(id)?36:width;
   if(icon.getIntrinsicWidth()!=width||icon.getIntrinsicHeight()!=height)throw new java.io.IOException("Icon intrinsic geometry mismatch");
   return icon;
  }catch(java.io.IOException unavailable){try{return raster(context,id);}catch(java.io.IOException absent){return load(id);}}
 }
 private static Drawable raster(android.content.Context context,String id)throws java.io.IOException {
  if("start".equals(id))return new WmLogo(WmResources.iconRaster(context,"launcher/start-logo.png",256,256));
  if("phone".equals(id))return WmResources.iconRaster(context,"launcher/icons/phone.png",64,64);
  if("wireless".equals(id))return WmResources.iconRaster(context,"launcher/icons/wireless.png",32,32);
  if("lock".equals(id))return WmResources.iconRaster(context,"launcher/icons/lock.png",32,32);
  throw new java.io.FileNotFoundException("No native icon mapping: "+id);
 }
 /** Warm known raster/vector artwork on a worker after pack activation.
  * Individual IOException failures are skipped; load(context,id) still falls back. */
 public static void prepare(android.content.Context context){for(String id:IDS){try{raster(context,id);}catch(java.io.IOException absent){}try{WmResources.vector(context,"vectors/"+id);}catch(java.io.IOException absent){}}}
 /** Resource-free alternative with unchanged intrinsic dimensions; Bluetooth uses an authored vector, other icons use system emoji. */
 public static Drawable fallback(String id){
  known(id);if("bluetooth".equals(id))return load(id);String symbol;
  if(id.startsWith("battery"))symbol="battery_charge".equals(id)?"⚡":"🔋";
  else if(id.startsWith("volume"))symbol="volume_muted".equals(id)?"🔇":"volume_vibrate".equals(id)?"📳":"volume_ring".equals(id)?"🔔":"🔊";
  else if(id.startsWith("cellular")||"radio_off".equals(id)||"phone".equals(id))symbol="📞";
  else if("start".equals(id))symbol="🪟";
  else if("lock".equals(id))symbol="🔓";
  else if("sip_keyboard".equals(id))symbol="⌨️";
  else symbol="network_disconnected".equals(id)?"❌":"📶";
  int width="start".equals(id)?41:"phone".equals(id)?64:32,height="start".equals(id)?36:width;
  return new android.graphics.drawable.BitmapDrawable((android.content.res.Resources)null,WmResources.emojiBitmap(symbol,width,height));
 }
 public static Drawable load(String id){
  if("start".equals(id))return new WmLogo();
  if(id==null)throw new IllegalArgumentException("icon id is null");
  switch(id){
   case "network_connected":return new WmIcons(id);
   case "network_disconnected":return new WmIcons(id);
   case "network_data_connected":return new WmIcons(id);
   case "cellular_off":return new WmIcons(id);
   case "cellular_ready":return new WmIcons(id);
   case "radio_off":return new WmIcons(id);
   case "phone":return fallback(id);
   case "volume_system":return new WmIcons(id);
   case "volume_ring":return new WmIcons(id);
   case "volume":return new WmIcons(id);
   case "volume_muted":return new WmIcons(id);
   case "volume_vibrate":return new WmIcons(id);
   case "signal":return new WmIcons(id);
   case "battery_charge":return new WmIcons(id);
   case "battery_full":return new WmIcons(id);
   case "battery_medium":return new WmIcons(id);
   case "battery_low":return new WmIcons(id);
   case "battery_half":return new WmIcons(id);
   case "battery_critical":return new WmIcons(id);
   case "wireless":return new WmIcons(id);
   case "lock":return new WmIcons(id);
   case "bluetooth":return new WmIcons(id);
   case "sip_keyboard":return new WmIcons(id);
   default:throw new IllegalArgumentException("Unknown WM icon: "+id);
  }
 }
 private WmIcons(String id){ switch(id){
case "bluetooth":{
// Authored Bluetooth symbol: black glyph in a white oval, drawn at display scale.
{Path p=new Path();p.addOval(new RectF(4,0,28,32),Path.Direction.CW);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xff000000);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.addOval(new RectF(5,1,27,31),Path.Direction.CW);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(9,9);p.lineTo(23,22);p.lineTo(15,29);p.lineTo(15,3);p.lineTo(23,10);p.lineTo(9,23);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xff000000);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setStrokeCap(Paint.Cap.BUTT);paint.setStrokeJoin(Paint.Join.MITER);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "network_connected":{width=32.0f;height=32.0f;
{Path p=new Path();p.moveTo(0.0f,2.0f);p.lineTo(28.0f,2.0f);p.lineTo(28.0f,24.0f);p.lineTo(11.0f,24.0f);p.lineTo(14.0f,20.0f);p.lineTo(24.0f,20.0f);p.lineTo(24.0f,8.0f);p.lineTo(4.0f,8.0f);p.lineTo(4.0f,16.0f);p.lineTo(0.0f,16.0f);p.close();p.moveTo(14.0f,4.0f);p.lineTo(18.0f,4.0f);p.lineTo(18.0f,6.0f);p.lineTo(14.0f,6.0f);p.close();p.moveTo(20.0f,4.0f);p.lineTo(24.0f,4.0f);p.lineTo(24.0f,6.0f);p.lineTo(20.0f,6.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(0.0f,18.0f);p.lineTo(4.0f,22.0f);p.lineTo(9.0f,17.0f);p.lineTo(12.0f,20.0f);p.lineTo(4.0f,28.0f);p.lineTo(0.0f,24.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "network_disconnected":{width=32.0f;height=32.0f;
{Path p=new Path();p.moveTo(9.0f,4.0f);p.lineTo(11.0f,4.0f);p.lineTo(11.0f,9.0f);p.lineTo(9.0f,9.0f);p.close();p.moveTo(13.0f,4.0f);p.lineTo(17.0f,4.0f);p.lineTo(17.0f,1.0f);p.lineTo(23.0f,6.5f);p.lineTo(17.0f,12.0f);p.lineTo(17.0f,9.0f);p.lineTo(13.0f,9.0f);p.close();p.moveTo(7.0f,11.0f);p.lineTo(7.0f,14.0f);p.lineTo(11.0f,14.0f);p.lineTo(11.0f,19.0f);p.lineTo(7.0f,19.0f);p.lineTo(7.0f,22.0f);p.lineTo(1.0f,16.5f);p.close();p.moveTo(13.0f,14.0f);p.lineTo(15.0f,14.0f);p.lineTo(15.0f,19.0f);p.lineTo(13.0f,19.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(18.5f,17.0f);p.lineTo(19.5f,17.0f);p.lineTo(24.0f,21.5f);p.lineTo(28.5f,17.0f);p.lineTo(29.5f,17.0f);p.lineTo(31.0f,18.5f);p.lineTo(31.0f,19.5f);p.lineTo(26.5f,24.0f);p.lineTo(31.0f,28.5f);p.lineTo(31.0f,29.5f);p.lineTo(29.5f,31.0f);p.lineTo(28.5f,31.0f);p.lineTo(24.0f,26.5f);p.lineTo(19.5f,31.0f);p.lineTo(18.5f,31.0f);p.lineTo(17.0f,29.5f);p.lineTo(17.0f,28.5f);p.lineTo(21.5f,24.0f);p.lineTo(17.0f,19.5f);p.lineTo(17.0f,18.5f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "network_data_connected":{width=32.0f;height=32.0f;
{Path p=new Path();p.moveTo(6.0f,6.0f);p.lineTo(8.0f,6.0f);p.lineTo(8.0f,12.0f);p.lineTo(6.0f,12.0f);p.close();p.moveTo(10.0f,6.0f);p.lineTo(22.0f,6.0f);p.lineTo(22.0f,2.0f);p.lineTo(32.0f,9.0f);p.lineTo(22.0f,17.0f);p.lineTo(22.0f,12.0f);p.lineTo(10.0f,12.0f);p.close();p.moveTo(8.0f,16.0f);p.lineTo(8.0f,20.0f);p.lineTo(14.0f,20.0f);p.lineTo(14.0f,26.0f);p.lineTo(8.0f,26.0f);p.lineTo(8.0f,30.0f);p.lineTo(0.0f,23.0f);p.close();p.moveTo(16.0f,20.0f);p.lineTo(20.0f,20.0f);p.lineTo(20.0f,26.0f);p.lineTo(16.0f,26.0f);p.close();p.moveTo(22.0f,20.0f);p.lineTo(24.0f,20.0f);p.lineTo(24.0f,26.0f);p.lineTo(22.0f,26.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "cellular_off":{width=32.0f;height=32.0f;
{Path p=new Path();p.moveTo(1.0f,4.0f);p.lineTo(4.0f,4.0f);p.lineTo(7.0f,7.0f);p.lineTo(10.0f,4.0f);p.lineTo(13.0f,4.0f);p.lineTo(13.0f,7.0f);p.lineTo(10.0f,10.0f);p.lineTo(13.0f,13.0f);p.lineTo(13.0f,16.0f);p.lineTo(10.0f,16.0f);p.lineTo(7.0f,13.0f);p.lineTo(4.0f,16.0f);p.lineTo(1.0f,16.0f);p.lineTo(1.0f,13.0f);p.lineTo(4.0f,10.0f);p.lineTo(1.0f,7.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(12.0f,9.0f);p.lineTo(25.0f,9.0f);p.lineTo(31.0f,15.0f);p.lineTo(31.0f,29.0f);p.lineTo(5.0f,29.0f);p.lineTo(5.0f,16.0f);p.lineTo(9.0f,16.0f);p.lineTo(9.0f,25.0f);p.lineTo(27.0f,25.0f);p.lineTo(27.0f,19.0f);p.lineTo(21.0f,13.0f);p.lineTo(14.0f,13.0f);p.lineTo(12.0f,11.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "cellular_ready":{width=32.0f;height=32.0f;
{Path p=new Path();p.moveTo(0.0f,1.0f);p.lineTo(20.0f,1.0f);p.lineTo(20.0f,6.0f);p.lineTo(12.0f,14.0f);p.lineTo(12.0f,24.0f);p.lineTo(8.0f,24.0f);p.lineTo(8.0f,14.0f);p.lineTo(0.0f,6.0f);p.close();p.moveTo(5.0f,5.0f);p.lineTo(10.0f,10.0f);p.lineTo(15.0f,5.0f);p.close();p.moveTo(8.0f,28.0f);p.lineTo(12.0f,28.0f);p.lineTo(12.0f,32.0f);p.lineTo(8.0f,32.0f);p.close();p.setFillType(Path.FillType.EVEN_ODD);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "radio_off":{width=32.0f;height=32.0f;
{Path p=new Path();p.moveTo(1.0f,1.0f);p.lineTo(21.0f,1.0f);p.lineTo(21.0f,5.5f);p.lineTo(13.0f,13.5f);p.lineTo(13.0f,23.0f);p.lineTo(9.0f,23.0f);p.lineTo(9.0f,13.5f);p.lineTo(1.0f,5.5f);p.close();p.moveTo(6.5f,5.0f);p.lineTo(15.5f,5.0f);p.lineTo(11.0f,9.5f);p.close();p.setFillType(Path.FillType.EVEN_ODD);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(18.5f,17.0f);p.lineTo(19.5f,17.0f);p.lineTo(24.0f,21.5f);p.lineTo(28.5f,17.0f);p.lineTo(29.5f,17.0f);p.lineTo(31.0f,18.5f);p.lineTo(31.0f,19.5f);p.lineTo(26.5f,24.0f);p.lineTo(31.0f,28.5f);p.lineTo(31.0f,29.5f);p.lineTo(29.5f,31.0f);p.lineTo(28.5f,31.0f);p.lineTo(24.0f,26.5f);p.lineTo(19.5f,31.0f);p.lineTo(18.5f,31.0f);p.lineTo(17.0f,29.5f);p.lineTo(17.0f,28.5f);p.lineTo(21.5f,24.0f);p.lineTo(17.0f,19.5f);p.lineTo(17.0f,18.5f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "volume_system":{width=32.0f;height=32.0f;
{Path p=new Path();p.moveTo(2.0f,5.0f);p.quadTo(2.0f,2.0f,5.0f,2.0f);p.lineTo(17.0f,2.0f);p.quadTo(20.0f,2.0f,20.0f,5.0f);p.lineTo(20.0f,26.0f);p.quadTo(20.0f,29.0f,17.0f,29.0f);p.lineTo(5.0f,29.0f);p.quadTo(2.0f,29.0f,2.0f,26.0f);p.close();p.moveTo(5.0f,5.0f);p.lineTo(5.0f,21.0f);p.lineTo(17.0f,21.0f);p.lineTo(17.0f,5.0f);p.close();p.moveTo(6.0f,24.0f);p.lineTo(6.0f,26.0f);p.lineTo(8.0f,26.0f);p.lineTo(8.0f,24.0f);p.close();p.moveTo(10.0f,24.0f);p.lineTo(10.0f,26.0f);p.lineTo(12.0f,26.0f);p.lineTo(12.0f,24.0f);p.close();p.moveTo(14.0f,24.0f);p.lineTo(14.0f,26.0f);p.lineTo(16.0f,26.0f);p.lineTo(16.0f,24.0f);p.close();p.moveTo(22.0f,11.0f);p.lineTo(27.0f,7.0f);p.lineTo(28.0f,9.0f);p.lineTo(23.0f,13.0f);p.close();p.moveTo(23.0f,16.0f);p.lineTo(31.0f,16.0f);p.lineTo(31.0f,18.0f);p.lineTo(23.0f,18.0f);p.close();p.moveTo(22.0f,21.0f);p.lineTo(23.0f,19.0f);p.lineTo(28.0f,23.0f);p.lineTo(27.0f,25.0f);p.close();p.setFillType(Path.FillType.EVEN_ODD);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xff000000);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "volume_ring":{width=32.0f;height=32.0f;
{Path p=new Path();p.moveTo(6.0f,25.0f);p.cubicTo(5.0f,18.0f,12.0f,10.0f,19.0f,7.0f);p.lineTo(19.0f,6.0f);p.lineTo(24.0f,6.0f);p.lineTo(27.0f,9.0f);p.lineTo(27.0f,12.0f);p.lineTo(24.0f,15.0f);p.lineTo(21.0f,15.0f);p.lineTo(19.0f,13.0f);p.cubicTo(16.0f,13.0f,13.0f,17.0f,13.0f,20.0f);p.lineTo(15.0f,22.0f);p.lineTo(15.0f,25.0f);p.lineTo(12.0f,28.0f);p.lineTo(9.0f,28.0f);p.close();p.moveTo(1.0f,12.0f);p.lineTo(7.0f,12.0f);p.lineTo(7.0f,14.0f);p.lineTo(1.0f,14.0f);p.close();p.moveTo(3.0f,4.0f);p.lineTo(5.0f,4.0f);p.lineTo(10.0f,9.0f);p.lineTo(8.0f,11.0f);p.close();p.moveTo(11.0f,1.0f);p.lineTo(13.0f,1.0f);p.lineTo(13.0f,7.0f);p.lineTo(11.0f,7.0f);p.close();p.setFillType(Path.FillType.EVEN_ODD);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xff000000);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "volume":{width=32.0f;height=32.0f;
{Path p=new Path();p.moveTo(0.0f,12.0f);p.lineTo(7.0f,12.0f);p.lineTo(15.0f,4.0f);p.lineTo(16.0f,4.0f);p.lineTo(16.0f,28.0f);p.lineTo(15.0f,28.0f);p.lineTo(7.0f,20.0f);p.lineTo(0.0f,20.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(20.0f,11.0f);p.lineTo(27.0f,4.0f);p.lineTo(29.0f,4.0f);p.lineTo(29.0f,6.0f);p.lineTo(22.0f,13.0f);p.close();p.moveTo(20.0f,15.0f);p.lineTo(30.0f,15.0f);p.lineTo(30.0f,17.0f);p.lineTo(20.0f,17.0f);p.close();p.moveTo(20.0f,20.0f);p.lineTo(22.0f,20.0f);p.lineTo(29.0f,27.0f);p.lineTo(29.0f,28.0f);p.lineTo(27.0f,28.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "volume_muted":{width=32.0f;height=32.0f;
{Path p=new Path();p.moveTo(0.0f,12.0f);p.lineTo(7.0f,12.0f);p.lineTo(15.0f,4.0f);p.lineTo(16.0f,4.0f);p.lineTo(16.0f,28.0f);p.lineTo(15.0f,28.0f);p.lineTo(7.0f,20.0f);p.lineTo(0.0f,20.0f);p.close();p.moveTo(20.0f,9.0f);p.lineTo(25.0f,14.0f);p.lineTo(30.0f,9.0f);p.lineTo(32.0f,11.0f);p.lineTo(27.0f,16.0f);p.lineTo(32.0f,21.0f);p.lineTo(30.0f,23.0f);p.lineTo(25.0f,18.0f);p.lineTo(20.0f,23.0f);p.lineTo(18.0f,21.0f);p.lineTo(23.0f,16.0f);p.lineTo(18.0f,11.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "volume_vibrate":{width=32.0f;height=32.0f;
{Path p=new Path();p.moveTo(9.0f,2.0f);p.lineTo(23.0f,2.0f);p.lineTo(26.0f,5.0f);p.lineTo(26.0f,27.0f);p.lineTo(23.0f,30.0f);p.lineTo(9.0f,30.0f);p.lineTo(6.0f,27.0f);p.lineTo(6.0f,5.0f);p.close();p.moveTo(10.0f,5.0f);p.lineTo(22.0f,5.0f);p.lineTo(22.0f,22.0f);p.lineTo(10.0f,22.0f);p.close();p.moveTo(10.0f,24.0f);p.lineTo(13.0f,24.0f);p.lineTo(13.0f,27.0f);p.lineTo(10.0f,27.0f);p.close();p.moveTo(15.0f,24.0f);p.lineTo(18.0f,24.0f);p.lineTo(18.0f,27.0f);p.lineTo(15.0f,27.0f);p.close();p.moveTo(20.0f,24.0f);p.lineTo(23.0f,24.0f);p.lineTo(23.0f,27.0f);p.lineTo(20.0f,27.0f);p.close();p.moveTo(2.0f,7.0f);p.lineTo(5.0f,7.0f);p.lineTo(5.0f,13.0f);p.lineTo(2.0f,16.0f);p.lineTo(2.0f,23.0f);p.lineTo(0.0f,23.0f);p.lineTo(0.0f,15.0f);p.lineTo(2.0f,12.0f);p.close();p.moveTo(28.0f,9.0f);p.lineTo(31.0f,9.0f);p.lineTo(31.0f,17.0f);p.lineTo(29.0f,20.0f);p.lineTo(29.0f,25.0f);p.lineTo(27.0f,25.0f);p.lineTo(27.0f,18.0f);p.lineTo(28.0f,15.0f);p.close();p.setFillType(Path.FillType.EVEN_ODD);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "signal":{width=32.0f;height=32.0f;
{Path p=new Path();p.addCircle(9.0f,9.0f,8.0f,Path.Direction.CW);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setStyle(Paint.Style.STROKE);paint.setColor(0xffffffff);paint.setStrokeWidth(2.0f);paint.setStrokeCap(Paint.Cap.BUTT);paint.setStrokeJoin(Paint.Join.MITER);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.addCircle(9.0f,9.0f,3.0f,Path.Direction.CW);p.moveTo(8.0f,12.0f);p.lineTo(10.0f,12.0f);p.lineTo(10.0f,17.0f);p.lineTo(12.0f,17.0f);p.lineTo(12.0f,21.0f);p.lineTo(13.0f,21.0f);p.lineTo(13.0f,24.0f);p.lineTo(14.0f,24.0f);p.lineTo(14.0f,30.0f);p.lineTo(4.0f,30.0f);p.lineTo(4.0f,24.0f);p.lineTo(5.0f,24.0f);p.lineTo(5.0f,21.0f);p.lineTo(6.0f,21.0f);p.lineTo(6.0f,17.0f);p.lineTo(8.0f,17.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "battery_charge":{width=32.0f;height=32.0f;
{Path p=new Path();p.moveTo(4.0f,5.0f);p.lineTo(18.0f,5.0f);p.lineTo(18.0f,7.0f);p.lineTo(16.0f,9.0f);p.lineTo(8.0f,9.0f);p.lineTo(8.0f,13.0f);p.lineTo(4.0f,13.0f);p.lineTo(4.0f,19.0f);p.lineTo(8.0f,19.0f);p.lineTo(8.0f,23.0f);p.lineTo(12.0f,23.0f);p.lineTo(18.0f,27.0f);p.lineTo(4.0f,27.0f);p.lineTo(4.0f,21.0f);p.lineTo(0.0f,21.0f);p.lineTo(0.0f,11.0f);p.lineTo(4.0f,11.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(18.0f,9.0f);p.lineTo(21.0f,9.0f);p.quadTo(24.0f,10.0f,24.0f,13.0f);p.lineTo(26.0f,13.0f);p.cubicTo(30.0f,10.0f,30.0f,6.0f,26.0f,3.0f);p.lineTo(23.0f,0.0f);p.lineTo(26.0f,0.0f);p.cubicTo(33.0f,3.0f,34.0f,11.0f,29.0f,16.0f);p.quadTo(27.0f,18.0f,24.0f,18.0f);p.lineTo(24.0f,20.0f);p.quadTo(24.0f,22.0f,21.0f,23.0f);p.lineTo(18.0f,23.0f);p.lineTo(18.0f,19.0f);p.lineTo(12.0f,19.0f);p.lineTo(12.0f,17.0f);p.lineTo(18.0f,17.0f);p.lineTo(18.0f,15.0f);p.lineTo(12.0f,15.0f);p.lineTo(12.0f,13.0f);p.lineTo(18.0f,13.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "battery_full":{width=32.0f;height=32.0f;
{Path p=new Path();p.moveTo(4.0f,5.0f);p.lineTo(32.0f,5.0f);p.lineTo(32.0f,9.0f);p.lineTo(8.0f,9.0f);p.lineTo(8.0f,13.0f);p.lineTo(4.0f,13.0f);p.lineTo(4.0f,19.0f);p.lineTo(8.0f,19.0f);p.lineTo(8.0f,23.0f);p.lineTo(32.0f,23.0f);p.lineTo(32.0f,27.0f);p.lineTo(4.0f,27.0f);p.lineTo(4.0f,21.0f);p.lineTo(0.0f,21.0f);p.lineTo(0.0f,11.0f);p.lineTo(4.0f,11.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(10.0f,11.0f);p.lineTo(14.0f,11.0f);p.lineTo(14.0f,21.0f);p.lineTo(10.0f,21.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(16.0f,11.0f);p.lineTo(20.0f,11.0f);p.lineTo(20.0f,21.0f);p.lineTo(16.0f,21.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(22.0f,11.0f);p.lineTo(26.0f,11.0f);p.lineTo(26.0f,21.0f);p.lineTo(22.0f,21.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(28.0f,11.0f);p.lineTo(32.0f,11.0f);p.lineTo(32.0f,21.0f);p.lineTo(28.0f,21.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "battery_medium":{width=32.0f;height=32.0f;
{Path p=new Path();p.moveTo(4.0f,5.0f);p.lineTo(32.0f,5.0f);p.lineTo(32.0f,9.0f);p.lineTo(8.0f,9.0f);p.lineTo(8.0f,13.0f);p.lineTo(4.0f,13.0f);p.lineTo(4.0f,19.0f);p.lineTo(8.0f,19.0f);p.lineTo(8.0f,23.0f);p.lineTo(32.0f,23.0f);p.lineTo(32.0f,27.0f);p.lineTo(4.0f,27.0f);p.lineTo(4.0f,21.0f);p.lineTo(0.0f,21.0f);p.lineTo(0.0f,11.0f);p.lineTo(4.0f,11.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(10.0f,11.0f);p.lineTo(14.0f,11.0f);p.lineTo(14.0f,21.0f);p.lineTo(10.0f,21.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(16.0f,11.0f);p.lineTo(20.0f,11.0f);p.lineTo(20.0f,21.0f);p.lineTo(16.0f,21.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(22.0f,11.0f);p.lineTo(26.0f,11.0f);p.lineTo(26.0f,21.0f);p.lineTo(22.0f,21.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "battery_low":{width=32.0f;height=32.0f;
{Path p=new Path();p.moveTo(4.0f,5.0f);p.lineTo(32.0f,5.0f);p.lineTo(32.0f,9.0f);p.lineTo(8.0f,9.0f);p.lineTo(8.0f,13.0f);p.lineTo(4.0f,13.0f);p.lineTo(4.0f,19.0f);p.lineTo(8.0f,19.0f);p.lineTo(8.0f,23.0f);p.lineTo(32.0f,23.0f);p.lineTo(32.0f,27.0f);p.lineTo(4.0f,27.0f);p.lineTo(4.0f,21.0f);p.lineTo(0.0f,21.0f);p.lineTo(0.0f,11.0f);p.lineTo(4.0f,11.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(10.0f,11.0f);p.lineTo(14.0f,11.0f);p.lineTo(14.0f,21.0f);p.lineTo(10.0f,21.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "battery_half":{width=32.0f;height=32.0f;
{Path p=new Path();p.moveTo(4.0f,5.0f);p.lineTo(32.0f,5.0f);p.lineTo(32.0f,9.0f);p.lineTo(8.0f,9.0f);p.lineTo(8.0f,13.0f);p.lineTo(4.0f,13.0f);p.lineTo(4.0f,19.0f);p.lineTo(8.0f,19.0f);p.lineTo(8.0f,23.0f);p.lineTo(32.0f,23.0f);p.lineTo(32.0f,27.0f);p.lineTo(4.0f,27.0f);p.lineTo(4.0f,21.0f);p.lineTo(0.0f,21.0f);p.lineTo(0.0f,11.0f);p.lineTo(4.0f,11.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(10.0f,11.0f);p.lineTo(14.0f,11.0f);p.lineTo(14.0f,21.0f);p.lineTo(10.0f,21.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(16.0f,11.0f);p.lineTo(20.0f,11.0f);p.lineTo(20.0f,21.0f);p.lineTo(16.0f,21.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "battery_critical":{width=32.0f;height=32.0f;
{Path p=new Path();p.moveTo(4.0f,5.0f);p.lineTo(22.0f,5.0f);p.lineTo(22.0f,9.0f);p.lineTo(8.0f,9.0f);p.lineTo(8.0f,13.0f);p.lineTo(4.0f,13.0f);p.lineTo(4.0f,19.0f);p.lineTo(8.0f,19.0f);p.lineTo(8.0f,23.0f);p.lineTo(31.0f,23.0f);p.lineTo(31.0f,27.0f);p.lineTo(4.0f,27.0f);p.lineTo(4.0f,21.0f);p.lineTo(0.0f,21.0f);p.lineTo(0.0f,11.0f);p.lineTo(4.0f,11.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.addCircle(27.0f,19.0f,2.0f,Path.Direction.CW);p.moveTo(26.0f,0.0f);p.lineTo(28.0f,0.0f);p.quadTo(30.0f,0.0f,30.0f,3.0f);p.lineTo(30.0f,6.0f);p.lineTo(29.0f,7.0f);p.lineTo(29.0f,11.0f);p.lineTo(28.0f,11.0f);p.lineTo(28.0f,15.0f);p.lineTo(26.0f,15.0f);p.lineTo(26.0f,11.0f);p.lineTo(25.0f,11.0f);p.lineTo(25.0f,7.0f);p.lineTo(24.0f,6.0f);p.lineTo(24.0f,3.0f);p.quadTo(24.0f,0.0f,26.0f,0.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "wireless":{width=32.0f;height=32.0f;
{Path p=new Path();p.addCircle(16.0f,16.0f,15.0f,Path.Direction.CW);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setShader(new LinearGradient(16.0f,1.0f,16.0f,31.0f,new int[]{0xff5bb8fa,0xff4991df,0xff2855ad},new float[]{0.0f,0.5f,1.0f},Shader.TileMode.CLAMP));paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(7.0f,8.0f);p.cubicTo(3.0f,14.0f,3.0f,20.0f,7.0f,25.0f);p.moveTo(25.0f,8.0f);p.cubicTo(29.0f,14.0f,29.0f,20.0f,25.0f,25.0f);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setStyle(Paint.Style.STROKE);paint.setColor(0xffd5e8fb);paint.setStrokeWidth(4.0f);paint.setStrokeCap(Paint.Cap.BUTT);paint.setStrokeJoin(Paint.Join.ROUND);paths.add(p);paints.add(paint);alphas.add(31);}
{Path p=new Path();p.moveTo(7.0f,8.0f);p.cubicTo(3.0f,14.0f,3.0f,20.0f,7.0f,25.0f);p.moveTo(25.0f,8.0f);p.cubicTo(29.0f,14.0f,29.0f,20.0f,25.0f,25.0f);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setStyle(Paint.Style.STROKE);paint.setColor(0xffd5e8fb);paint.setStrokeWidth(2.0f);paint.setStrokeCap(Paint.Cap.BUTT);paint.setStrokeJoin(Paint.Join.ROUND);paths.add(p);paints.add(paint);alphas.add(204);}
{Path p=new Path();p.moveTo(10.0f,11.0f);p.cubicTo(7.0f,15.0f,7.0f,20.0f,10.0f,23.0f);p.moveTo(22.0f,11.0f);p.cubicTo(25.0f,15.0f,25.0f,20.0f,22.0f,23.0f);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setStyle(Paint.Style.STROKE);paint.setColor(0xffe5f0fd);paint.setStrokeWidth(4.0f);paint.setStrokeCap(Paint.Cap.BUTT);paint.setStrokeJoin(Paint.Join.ROUND);paths.add(p);paints.add(paint);alphas.add(31);}
{Path p=new Path();p.moveTo(10.0f,11.0f);p.cubicTo(7.0f,15.0f,7.0f,20.0f,10.0f,23.0f);p.moveTo(22.0f,11.0f);p.cubicTo(25.0f,15.0f,25.0f,20.0f,22.0f,23.0f);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setStyle(Paint.Style.STROKE);paint.setColor(0xffe5f0fd);paint.setStrokeWidth(2.0f);paint.setStrokeCap(Paint.Cap.BUTT);paint.setStrokeJoin(Paint.Join.ROUND);paths.add(p);paints.add(paint);alphas.add(219);}
{Path p=new Path();p.addCircle(16.0f,16.0f,3.0f,Path.Direction.CW);p.moveTo(15.0f,18.0f);p.lineTo(17.0f,18.0f);p.lineTo(18.0f,29.0f);p.lineTo(14.0f,29.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "lock":{width=32.0f;height=32.0f;
{Path p=new Path();p.moveTo(15.0f,19.0f);p.lineTo(15.0f,6.0f);p.quadTo(15.0f,1.0f,20.0f,1.0f);p.lineTo(23.0f,1.0f);p.quadTo(29.0f,1.0f,29.0f,7.0f);p.lineTo(29.0f,16.0f);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setStyle(Paint.Style.STROKE);paint.setColor(0xff383939);paint.setStrokeWidth(3.0f);paint.setStrokeCap(Paint.Cap.BUTT);paint.setStrokeJoin(Paint.Join.ROUND);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(14.0f,18.0f);p.lineTo(14.0f,6.0f);p.quadTo(14.0f,2.0f,19.0f,2.0f);p.lineTo(22.0f,2.0f);p.quadTo(27.0f,2.0f,27.0f,7.0f);p.lineTo(27.0f,15.0f);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setStyle(Paint.Style.STROKE);paint.setColor(0xffa0a3a3);paint.setStrokeWidth(2.0f);paint.setStrokeCap(Paint.Cap.BUTT);paint.setStrokeJoin(Paint.Join.ROUND);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(15.0f,17.0f);p.lineTo(15.0f,6.0f);p.quadTo(15.0f,3.0f,19.0f,3.0f);p.lineTo(22.0f,3.0f);p.quadTo(26.0f,3.0f,26.0f,7.0f);p.lineTo(26.0f,15.0f);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setStyle(Paint.Style.STROKE);paint.setColor(0xffe8e8e8);paint.setStrokeWidth(1.0f);paint.setStrokeCap(Paint.Cap.BUTT);paint.setStrokeJoin(Paint.Join.ROUND);paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(2.0f,17.0f);p.lineTo(19.0f,17.0f);p.lineTo(19.0f,31.0f);p.lineTo(2.0f,31.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setShader(new LinearGradient(10.0f,17.0f,10.0f,31.0f,new int[]{0xffffcf24,0xffffad00,0xffd67a00},new float[]{0.0f,0.35f,1.0f},Shader.TileMode.CLAMP));paths.add(p);paints.add(paint);alphas.add(255);}
{Path p=new Path();p.moveTo(3.0f,19.0f);p.lineTo(19.0f,19.0f);p.lineTo(19.0f,20.0f);p.lineTo(3.0f,20.0f);p.close();p.moveTo(3.0f,21.0f);p.lineTo(19.0f,21.0f);p.lineTo(19.0f,22.0f);p.lineTo(3.0f,22.0f);p.close();p.moveTo(3.0f,23.0f);p.lineTo(19.0f,23.0f);p.lineTo(19.0f,24.0f);p.lineTo(3.0f,24.0f);p.close();p.moveTo(3.0f,25.0f);p.lineTo(19.0f,25.0f);p.lineTo(19.0f,26.0f);p.lineTo(3.0f,26.0f);p.close();p.moveTo(3.0f,27.0f);p.lineTo(19.0f,27.0f);p.lineTo(19.0f,28.0f);p.lineTo(3.0f,28.0f);p.close();p.moveTo(3.0f,29.0f);p.lineTo(19.0f,29.0f);p.lineTo(19.0f,30.0f);p.lineTo(3.0f,30.0f);p.close();p.moveTo(18.0f,18.0f);p.lineTo(19.0f,18.0f);p.lineTo(19.0f,31.0f);p.lineTo(18.0f,31.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffc78100);paths.add(p);paints.add(paint);alphas.add(153);}
{Path p=new Path();p.moveTo(2.0f,17.0f);p.lineTo(19.0f,17.0f);p.lineTo(19.0f,18.0f);p.lineTo(2.0f,18.0f);p.close();p.moveTo(2.0f,18.0f);p.lineTo(3.0f,18.0f);p.lineTo(3.0f,31.0f);p.lineTo(2.0f,31.0f);p.close();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffdf58);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
case "sip_keyboard":{width=32.0f;height=32.0f;
pixelGrid=true;
{Path p=new Path();p.moveTo(1.0f,6.0f);p.lineTo(31.0f,6.0f);p.lineTo(31.0f,7.0f);p.lineTo(32.0f,7.0f);p.lineTo(32.0f,26.0f);p.lineTo(31.0f,26.0f);p.lineTo(31.0f,27.0f);p.lineTo(1.0f,27.0f);p.lineTo(1.0f,26.0f);p.lineTo(0.0f,26.0f);p.lineTo(0.0f,7.0f);p.lineTo(1.0f,7.0f);p.close();p.moveTo(2.0f,9.0f);p.lineTo(5.0f,9.0f);p.lineTo(5.0f,12.0f);p.lineTo(2.0f,12.0f);p.close();p.moveTo(6.0f,9.0f);p.lineTo(9.0f,9.0f);p.lineTo(9.0f,12.0f);p.lineTo(6.0f,12.0f);p.close();p.moveTo(10.0f,9.0f);p.lineTo(13.0f,9.0f);p.lineTo(13.0f,12.0f);p.lineTo(10.0f,12.0f);p.close();p.moveTo(14.0f,9.0f);p.lineTo(17.0f,9.0f);p.lineTo(17.0f,12.0f);p.lineTo(14.0f,12.0f);p.close();p.moveTo(18.0f,9.0f);p.lineTo(21.0f,9.0f);p.lineTo(21.0f,12.0f);p.lineTo(18.0f,12.0f);p.close();p.moveTo(22.0f,9.0f);p.lineTo(25.0f,9.0f);p.lineTo(25.0f,12.0f);p.lineTo(22.0f,12.0f);p.close();p.moveTo(26.0f,9.0f);p.lineTo(30.0f,9.0f);p.lineTo(30.0f,12.0f);p.lineTo(26.0f,12.0f);p.close();p.moveTo(2.0f,13.0f);p.lineTo(6.0f,13.0f);p.lineTo(6.0f,16.0f);p.lineTo(2.0f,16.0f);p.close();p.moveTo(7.0f,13.0f);p.lineTo(10.0f,13.0f);p.lineTo(10.0f,16.0f);p.lineTo(7.0f,16.0f);p.close();p.moveTo(11.0f,13.0f);p.lineTo(14.0f,13.0f);p.lineTo(14.0f,16.0f);p.lineTo(11.0f,16.0f);p.close();p.moveTo(15.0f,13.0f);p.lineTo(18.0f,13.0f);p.lineTo(18.0f,16.0f);p.lineTo(15.0f,16.0f);p.close();p.moveTo(19.0f,13.0f);p.lineTo(22.0f,13.0f);p.lineTo(22.0f,16.0f);p.lineTo(19.0f,16.0f);p.close();p.moveTo(23.0f,13.0f);p.lineTo(30.0f,13.0f);p.lineTo(30.0f,16.0f);p.lineTo(23.0f,16.0f);p.close();p.moveTo(2.0f,17.0f);p.lineTo(7.0f,17.0f);p.lineTo(7.0f,20.0f);p.lineTo(2.0f,20.0f);p.close();p.moveTo(8.0f,17.0f);p.lineTo(11.0f,17.0f);p.lineTo(11.0f,20.0f);p.lineTo(8.0f,20.0f);p.close();p.moveTo(12.0f,17.0f);p.lineTo(15.0f,17.0f);p.lineTo(15.0f,20.0f);p.lineTo(12.0f,20.0f);p.close();p.moveTo(16.0f,17.0f);p.lineTo(19.0f,17.0f);p.lineTo(19.0f,20.0f);p.lineTo(16.0f,20.0f);p.close();p.moveTo(20.0f,17.0f);p.lineTo(23.0f,17.0f);p.lineTo(23.0f,20.0f);p.lineTo(20.0f,20.0f);p.close();p.moveTo(24.0f,17.0f);p.lineTo(30.0f,17.0f);p.lineTo(30.0f,20.0f);p.lineTo(24.0f,20.0f);p.close();p.moveTo(2.0f,21.0f);p.lineTo(5.0f,21.0f);p.lineTo(5.0f,24.0f);p.lineTo(2.0f,24.0f);p.close();p.moveTo(6.0f,21.0f);p.lineTo(9.0f,21.0f);p.lineTo(9.0f,24.0f);p.lineTo(6.0f,24.0f);p.close();p.moveTo(10.0f,21.0f);p.lineTo(22.0f,21.0f);p.lineTo(22.0f,24.0f);p.lineTo(10.0f,24.0f);p.close();p.moveTo(23.0f,21.0f);p.lineTo(26.0f,21.0f);p.lineTo(26.0f,24.0f);p.lineTo(23.0f,24.0f);p.close();p.moveTo(27.0f,21.0f);p.lineTo(30.0f,21.0f);p.lineTo(30.0f,24.0f);p.lineTo(27.0f,24.0f);p.close();p.setFillType(Path.FillType.EVEN_ODD);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffffffff);paths.add(p);paints.add(paint);alphas.add(255);}
break;}
} }
 @Override public void draw(Canvas canvas){int save=canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/width,getBounds().height()/height);boolean antialias=true;if(pixelGrid){canvas.getMatrix(matrix);matrix.getValues(transform);antialias=transform[Matrix.MSKEW_X]!=0||transform[Matrix.MSKEW_Y]!=0;for(int index=0;index<6;index++)antialias|=Math.abs(transform[index]-Math.round(transform[index]))>0.0001f;}for(int i=0;i<paths.size();i++){Paint p=paints.get(i);if(pixelGrid)p.setAntiAlias(antialias);p.setAlpha(alphas.get(i)*opacity/255);canvas.drawPath(paths.get(i),p);}canvas.restoreToCount(save);}
 @Override public int getIntrinsicWidth(){return (int)width;}
 @Override public int getIntrinsicHeight(){return (int)height;}
 @Override public void setAlpha(int value){opacity=value;invalidateSelf();}
 @Override public void setColorFilter(ColorFilter filter){for(Paint p:paints)p.setColorFilter(filter);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
