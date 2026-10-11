package me.ancal.wmdroid.sdk;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
/** Compatibility logo wrapper: optional pack artwork or a system emoji. No embedded logo paths. */
public final class WmLogo extends Drawable {
 private final Drawable image;
 WmLogo(Drawable artwork){image=artwork;}
 public WmLogo(){image=WmIcons.fallback("start");}
 public WmLogo(Context context){image=WmIcons.load(context,"start");}
 @Override protected void onBoundsChange(Rect bounds){
  if(image instanceof WmVectorDrawable){image.setBounds(bounds);return;}
  int nativeWidth=image.getIntrinsicWidth(),nativeHeight=image.getIntrinsicHeight();
  float scale=Math.min(bounds.width()/(float)nativeWidth,bounds.height()/(float)nativeHeight);
  int width=Math.round(nativeWidth*scale),height=Math.round(nativeHeight*scale);
  int left=bounds.left+(bounds.width()-width)/2,top=bounds.top+(bounds.height()-height)/2;
  image.setBounds(left,top,left+width,top+height);
 }
 @Override public void draw(Canvas canvas){image.draw(canvas);}
 @Override public int getIntrinsicWidth(){return 41;}
 @Override public int getIntrinsicHeight(){return 36;}
 @Override public void setAlpha(int value){image.setAlpha(value);invalidateSelf();}
 @Override public void setColorFilter(ColorFilter value){image.setColorFilter(value);invalidateSelf();}
 @Override public int getOpacity(){return image.getOpacity();}
}
