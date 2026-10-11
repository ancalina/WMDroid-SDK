package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import java.util.HashSet;
import java.util.Set;

/** Native handled-DOWN feedback, decoded asynchronously on the media stream. */
public final class WmTouchFeedback {
    private static final int STREAM=AudioManager.STREAM_MUSIC;
    private final SoundPool pool;
    private final int[] sounds=new int[2];
    private final Set<Integer> ready=new HashSet<>();
    private boolean released;

    public WmTouchFeedback(Context context){
        pool=Build.VERSION.SDK_INT<21?new SoundPool(1,STREAM,0):new SoundPool.Builder().setMaxStreams(1)
            .setAudioAttributes(new AudioAttributes.Builder().setLegacyStreamType(STREAM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        pool.setOnLoadCompleteListener((loaded,id,status)->{synchronized(WmTouchFeedback.this){if(!released&&status==0)ready.add(id);}});
        String[] names={"soft","loud"};
        for(int i=0;i<names.length;i++){
            String path=WmVolumeFeedback.prepareAsset(context,"sounds/screen-tap/"+names[i]+".wav","wmdroid-touch-"+names[i]+".wav");
            sounds[i]=path==null?0:pool.load(path,1);
        }
    }
    /** 0 off, 1 soft, 2 loud. Device media volume and mute remain authoritative. */
    public synchronized void play(int level){
        if(released||level<1||level>2)return;
        int id=sounds[level-1];if(ready.contains(id))pool.play(id,1,1,1,0,1);
    }
    public synchronized void release(){
        if(released)return;released=true;pool.setOnLoadCompleteListener(null);ready.clear();pool.release();
    }
}
