package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/** Shared live Android status body; WmPopup owns the SHBoxEx chrome.
 * Wide bubble/footer and volume controls follow the unscaled VGA captures.
 */
public class WmStatusPanel extends LinearLayout implements WmControls.RestyleListener {
    public interface Host { void launch(Intent intent); void changed(); void backlight(); void hide(); void settings(); String text(String authored); int color(int original); void feedback(boolean ring,int step); void release(); default void attached(){} }
    private Typeface regular;
    private final Host host;
    private final int kind;
    private final AudioManager audio;
    private TextView details,hideButton;
    private final java.util.List<TextView> modes=new java.util.ArrayList<>();
    private Level media, ring;
    private ImageView speaker,phone;
    private String artworkRevision;
    private boolean policyActionAdded;
    private final Runnable poll=new Runnable(){public void run(){refresh();postDelayed(this,1000);}};
    public WmStatusPanel(Context context,Typeface regular,int kind,Host host){
        super(context);this.regular=regular;this.host=host;this.kind=kind;
        audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        setOrientation(VERTICAL);setPadding(8,0,8,21);
        details=label("");details.setGravity(Gravity.TOP);details.setPadding(0,4,0,0);
        if(kind==1){
            setPadding(0,2,0,20);addView(details,new LayoutParams(-1,-2));
            details.setVisibility(GONE);
            FrameLayout form=new FrameLayout(context);
            // DIALOG17410 positions validated against the480x640 VGA capture:
            // effective DLU scale about3.5, not the old blanket4px conversion.
            media=new Level(context,AudioManager.STREAM_MUSIC);ring=new Level(context,AudioManager.STREAM_RING);
            place(form,media,10,38,56,189);place(form,ring,80,38,56,189);
            speaker=new ImageView(context);speaker.setImageDrawable(WmIcons.load(context,"volume_system"));speaker.setColorFilter(Color.BLACK);place(form,speaker,20,9,32,32);
            phone=new ImageView(context);phone.setImageDrawable(WmIcons.load(context,"volume_ring"));phone.setColorFilter(Color.BLACK);place(form,phone,90,9,32,32);
            String[] names={host.text("설정"),host.text("진동"),host.text("해제")};int[] values={AudioManager.RINGER_MODE_NORMAL,AudioManager.RINGER_MODE_VIBRATE,AudioManager.RINGER_MODE_SILENT};
            for(int i=0;i<3;i++){final int value=values[i];TextView choice=label(names[i]);choice.setCompoundDrawablePadding(10);choice.setFocusableInTouchMode(true);choice.setOnTouchListener((v,event)->{if(event.getActionMasked()==MotionEvent.ACTION_DOWN)v.requestFocus();return false;});choice.setOnClickListener(v->{v.requestFocus();ringer(value);});modes.add(choice);place(form,choice,14,228+i*35,126,35);}
            addView(form,new LayoutParams(-1,333));
        }else if(kind==0){
            FrameLayout managerRow=new FrameLayout(context);TextView manager=link(host.text("무선 전송 관리자"),()->host.launch(new Intent(Settings.ACTION_WIRELESS_SETTINGS)));
            FrameLayout.LayoutParams managerPosition=new FrameLayout.LayoutParams(-2,36,Gravity.RIGHT);managerPosition.topMargin=7;managerRow.addView(manager,managerPosition);addView(managerRow,new LayoutParams(-1,47));
            separator();addView(details,new LayoutParams(-1,120));separator();footer();
        }else if(kind==3){
            addView(details,new LayoutParams(-1,44));separator();footer();
        }else{
            addView(details,new LayoutParams(-1,-2));
            action(host.text("배터리 설정"),Settings.ACTION_BATTERY_SAVER_SETTINGS);
            addView(link(host.text("화면 조명 설정"),host::backlight),new LayoutParams(-1,44));separator();footer();
        }
        refresh();
        artworkRevision=WmResources.revision(context);
    }
    private void place(FrameLayout form,View view,int x,int y,int width,int height){FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(width,height);p.leftMargin=x;p.topMargin=y;form.addView(view,p);}
    private TextView label(String value){TextView v=WmControls.text(getContext(),regular,value);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        this.regular=regular;String revision=WmResources.revision(getContext());
        if(!revision.equals(artworkRevision)){artworkRevision=revision;if(speaker!=null){speaker.setImageDrawable(WmIcons.load(getContext(),"volume_system"));phone.setImageDrawable(WmIcons.load(getContext(),"volume_ring"));}}
        if(hideButton!=null)styleHide();invalidate();if(media!=null)media.invalidate();if(ring!=null)ring.invalidate();
    }
    private void styleHide(){android.graphics.drawable.GradientDrawable frame=new android.graphics.drawable.GradientDrawable();frame.setColor(Color.TRANSPARENT);frame.setStroke(2,Color.BLACK);hideButton.setBackgroundDrawable(new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{WmControls.background(host.color(0xffc9e7e9),host.color(0xff149ca7)),frame}));}
    private TextView link(String caption,Runnable run){TextView v=label(caption);v.setTextColor(0xff0000ff);v.setPaintFlags(v.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);v.setSoundEffectsEnabled(false);v.setOnClickListener(w->run.run());return v;}
    private void action(String caption,String setting){addView(link(caption,()->host.launch(new Intent(setting))),new LayoutParams(-1,44));}
    private void separator(){View line=new View(getContext());line.setBackgroundColor(Color.BLACK);addView(line,new LayoutParams(-1,2));}
    private void footer(){
        FrameLayout row=new FrameLayout(getContext());FrameLayout.LayoutParams settingsPosition=new FrameLayout.LayoutParams(-2,36);settingsPosition.topMargin=7;row.addView(link(host.text("설정"),host::settings),settingsPosition);
        TextView hide=label(host.text("숨기기"));hideButton=hide;hide.setGravity(Gravity.CENTER);hide.setSoundEffectsEnabled(false);styleHide();hide.setOnClickListener(v->host.hide());
        FrameLayout.LayoutParams hidePosition=new FrameLayout.LayoutParams(80,36,Gravity.RIGHT);hidePosition.rightMargin=4;hidePosition.topMargin=7;row.addView(hide,hidePosition);addView(row,new LayoutParams(-1,54));
    }
    private static boolean needsPolicy(int current,int value,int api,boolean granted){return api>=24&&!granted&&(current==AudioManager.RINGER_MODE_SILENT)!=(value==AudioManager.RINGER_MODE_SILENT);}
    private boolean policyGranted(){return android.os.Build.VERSION.SDK_INT<23||PolicyAccess.granted(getContext());}
    private static final class PolicyAccess {static boolean granted(Context c){android.app.NotificationManager manager=(android.app.NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);return manager!=null&&manager.isNotificationPolicyAccessGranted();}}
    private void audioAccess(){
        details.setVisibility(VISIBLE);boolean policy=android.os.Build.VERSION.SDK_INT>=23&&!policyGranted();
        details.setText(host.text(policy?"무음 전환은 방해 금지 접근이 필요합니다.":"이 음량은 시스템 소리 설정에서 변경하세요."));
        if(!policyActionAdded){policyActionAdded=true;action(host.text(policy?"방해 금지 접근 설정":"소리 설정"),policy?Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS:Settings.ACTION_SOUND_SETTINGS);}
    }
    private void ringer(int value){
        if(audio.getRingerMode()==value){refresh();return;}
        if(needsPolicy(audio.getRingerMode(),value,android.os.Build.VERSION.SDK_INT,policyGranted())){audioAccess();return;}
        try{
            if(audio.getRingerMode()==AudioManager.RINGER_MODE_NORMAL&&value!=AudioManager.RINGER_MODE_NORMAL)WmShell.rememberMusic(getContext(),audio.getStreamVolume(AudioManager.STREAM_MUSIC));
            audio.setRingerMode(value);
            audio.setStreamVolume(AudioManager.STREAM_MUSIC,value==AudioManager.RINGER_MODE_NORMAL?WmShell.savedMusic(getContext(),audio.getStreamVolume(AudioManager.STREAM_MUSIC)):0,0);
            details.setText("");details.setVisibility(GONE);
        }
        catch(SecurityException denied){audioAccess();refresh();host.changed();return;}
        refresh();host.changed();
    }
    public void refresh(){
        if(kind==1){
            int r=audio.getRingerMode();int selected=r==AudioManager.RINGER_MODE_NORMAL?0:r==AudioManager.RINGER_MODE_VIBRATE?1:2;for(int i=0;i<modes.size();i++){Radio dot=new Radio(i==selected);dot.setBounds(0,0,30,30);modes.get(i).setCompoundDrawables(dot,null,null,null);modes.get(i).setContentDescription(modes.get(i).getText()+(i==selected?host.text(" 선택됨"):""));}
            media.update();ring.update();
        }else if(kind==0){
            ConnectivityManager cm=(ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            WmDevice.Network c=WmDevice.network(getContext());
            String connection=c==null?host.text("연결 안 됨"):c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?"Wi-Fi":c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)?host.text("이동통신"):c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)?host.text("유선"):host.text("네트워크");
            String description=c==null?host.text("활성화된 데이터 세션이 없습니다. 필요한 경우 프로그램에서 자동으로 연결합니다. 연결 설정을 변경하려면 [설정]을 누르십시오."):connection+"\n"+(c.connecting?host.text("연결 중..."):c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)?host.text("인터넷 연결됨"):host.text("인터넷 연결 확인 안 됨"));
            if(!description.contentEquals(details.getText()))details.setText(description);
        }else if(kind==3){
            WmDevice.Phone phone=WmDevice.phone(getContext());boolean airplane=phone.airplane;String description=airplane?"":host.text(phone.caption());
            if(!description.contentEquals(details.getText()))details.setText(description);
            int height=airplane?11:Math.max(44,details.getLineHeight()+8);if(details.getLayoutParams().height!=height){details.getLayoutParams().height=height;details.requestLayout();}
        }else{
            Intent b=getContext().registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level=b==null?-1:b.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),scale=b==null?0:b.getIntExtra(BatteryManager.EXTRA_SCALE,0);
            int state=b==null?-1:b.getIntExtra(BatteryManager.EXTRA_STATUS,-1);
            String charging=state==BatteryManager.BATTERY_STATUS_FULL?host.text("충전 완료"):state==BatteryManager.BATTERY_STATUS_CHARGING?host.text("충전 중"):host.text("배터리 사용 중");
            PowerManager power=(PowerManager)getContext().getSystemService(Context.POWER_SERVICE);
            details.setText(host.text("주 배터리: ")+(level>=0&&scale>0?level*100/scale+"%":host.text("알 수 없음"))+"\n"+charging+host.text("\n절전 모드: ")+((android.os.Build.VERSION.SDK_INT>=21&&power.isPowerSaveMode())?host.text("켜짐"):host.text("꺼짐")));
        }
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();host.attached();postDelayed(poll,1000);if(kind==1){int mode=audio.getRingerMode();if(mode==AudioManager.RINGER_MODE_NORMAL)media.requestFocus();else modes.get(mode==AudioManager.RINGER_MODE_VIBRATE?1:2).requestFocus();}}
    @Override protected void onDetachedFromWindow(){removeCallbacks(poll);host.release();super.onDetachedFromWindow();}
    private final class Level extends View {
        private final Paint paint=new Paint();private final int stream;private int value,max;
        Level(Context c,int stream){super(c);this.stream=stream;setFocusableInTouchMode(true);setSoundEffectsEnabled(false);update();}
        void update(){max=audio.getStreamMaxVolume(stream);value=audio.getStreamVolume(stream);setContentDescription((stream==AudioManager.STREAM_MUSIC?host.text("시스템"):host.text("벨소리"))+host.text(" 음량 ")+value+" / "+max);invalidate();}
        @Override protected void onDraw(Canvas c){
            float top=20,bottom=getHeight()-20,y=bottom-(bottom-top)*value/Math.max(1,max);
            if(hasFocus()){paint.setColor(host.color(0xff00f0ff));c.drawRect(0,1,getWidth(),2,paint);c.drawRect(0,getHeight()-1,getWidth(),getHeight(),paint);c.drawRect(0,1,1,getHeight(),paint);c.drawRect(getWidth()-1,1,getWidth(),getHeight(),paint);}
            paint.setColor(Color.WHITE);c.drawRect(16,15,20,getHeight()-12,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(Color.BLACK);c.drawRect(16,15,20,getHeight()-12,paint);paint.setStyle(Paint.Style.FILL);
            for(int i=0;i<=5;i++){float tick=top+(bottom-top)*i/5;c.drawRect(38,tick,43,tick+2,paint);}
            Path thumb=new Path();thumb.moveTo(3,y-8);thumb.lineTo(25,y-8);thumb.lineTo(33,y);thumb.lineTo(25,y+8);thumb.lineTo(3,y+8);thumb.close();
            paint.setColor(Color.WHITE);c.drawPath(thumb,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(Color.BLACK);c.drawPath(thumb,paint);paint.setStyle(Paint.Style.FILL);
        }
        @Override protected void onFocusChanged(boolean gain,int direction,android.graphics.Rect previous){super.onFocusChanged(gain,direction,previous);invalidate();}
        private void set(float y){int step=Math.max(0,Math.min(5,Math.round((getHeight()-20-y)/Math.max(1,getHeight()-40)*5)));int next=Math.round(step*max/5f);try{int old=value;audio.setStreamVolume(stream,next,0);update();host.changed();if(value!=old)host.feedback(stream==AudioManager.STREAM_RING,step);}catch(SecurityException denied){audioAccess();}}
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE||e.getAction()==MotionEvent.ACTION_UP){getParent().requestDisallowInterceptTouchEvent(true);requestFocus();set(e.getY());if(e.getAction()==MotionEvent.ACTION_UP)performClick();return true;}return true;}
        @Override public boolean onKeyDown(int key,android.view.KeyEvent e){
            int direction=key==android.view.KeyEvent.KEYCODE_DPAD_RIGHT||key==android.view.KeyEvent.KEYCODE_DPAD_UP?1:key==android.view.KeyEvent.KEYCODE_DPAD_LEFT||key==android.view.KeyEvent.KEYCODE_DPAD_DOWN?-1:0;
            if(direction!=0){int step=Math.round(value*5f/Math.max(1,max))+direction;set(getHeight()-20-(getHeight()-40)*step/5f);return true;}return super.onKeyDown(key,e);
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
    private static final class Radio extends android.graphics.drawable.Drawable {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final boolean selected;
        Radio(boolean selected){this.selected=selected;}
        public void draw(Canvas c){p.setColor(Color.WHITE);c.drawCircle(15,15,14,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.BLACK);c.drawCircle(15,15,14,p);p.setStyle(Paint.Style.FILL);if(selected){p.setColor(Color.BLACK);c.drawCircle(15,15,9,p);}}
        public void setAlpha(int alpha){}public void setColorFilter(android.graphics.ColorFilter f){}public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
    }
}
