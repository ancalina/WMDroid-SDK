package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.SystemClock;
import java.util.HashSet;
import java.util.Set;

/** VoiceBar 2655A20: Quietest at step1, SystemDefault at2..4, Loudest at5.
 * Default.wav is a ROM fallback: the SystemDefault runtime alias is not recovered.
 * Independent pools route previews to media/ring without changing global volumes.
 */
public class WmVolumeFeedback {
    private final Context context;private String revision;private boolean released;
    private final SoundPool[] pools=new SoundPool[2];private final int[][] sounds=new int[2][3];private final Set<Integer>[] ready;private final long[] last=new long[2];private final int[] playing=new int[2];
    @SuppressWarnings("unchecked") public WmVolumeFeedback(Context context){this.context=context.getApplicationContext();ready=new Set[]{new HashSet<Integer>(),new HashSet<Integer>()};reload();}
    private void reload(){clearPools();revision=WmResources.revision(context);String[] ids={prepare(context,"quietest"),prepare(context,"default"),prepare(context,"loudest")};
        for(int i=0;i<2;i++){final int pool=i;pools[i]=android.os.Build.VERSION.SDK_INT<21?new SoundPool(1,i==0?android.media.AudioManager.STREAM_MUSIC:android.media.AudioManager.STREAM_RING,0):new SoundPool.Builder().setMaxStreams(1).setAudioAttributes(new AudioAttributes.Builder().setUsage(i==0?AudioAttributes.USAGE_MEDIA:AudioAttributes.USAGE_NOTIFICATION_RINGTONE).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();pools[i].setOnLoadCompleteListener((p,id,status)->{synchronized(WmVolumeFeedback.this){if(!released&&p==pools[pool]&&status==0)ready[pool].add(id);}});for(int j=0;j<3;j++)sounds[i][j]=ids[j]==null?0:pools[i].load(ids[j],1);}
    }
    private static String prepare(Context context,String name){
        return prepareAsset(context,"sounds/volume/"+name+".wav","wmdroid-volume-"+name+".wav");
    }
    static String prepareAsset(Context context,String asset,String cacheName){
        try{return WmResources.assetFile(context,asset,4*1024*1024).getAbsolutePath();}
        catch(java.io.IOException unavailable){return null;}
    }
    public synchronized void play(boolean ring,int step){if(released)return;if(!WmResources.revision(context).equals(revision))reload();int pool=ring?1:0;if(step<=0)return;long now=SystemClock.uptimeMillis();if(now-last[pool]<250)return;int id=sounds[pool][step==1?0:step==5?2:1];if(!ready[pool].contains(id))return;last[pool]=now;if(playing[pool]!=0)pools[pool].stop(playing[pool]);playing[pool]=pools[pool].play(id,1,1,1,0,1);}
    private void clearPools(){for(int i=0;i<pools.length;i++){SoundPool pool=pools[i];pools[i]=null;if(pool!=null){pool.setOnLoadCompleteListener(null);pool.release();}ready[i].clear();java.util.Arrays.fill(sounds[i],0);last[i]=0;playing[i]=0;}}
    public synchronized void release(){if(released)return;released=true;clearPools();}
}
