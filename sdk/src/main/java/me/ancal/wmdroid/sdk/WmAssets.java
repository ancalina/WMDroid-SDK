package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.JsonReader;
import android.util.JsonToken;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/** Cataloged artwork and media, available without a launcher process or provider.
 * Optional catalogs are streamed once per app/pack generation, then kept immutable.
 * Missing catalogs yield an empty inventory. Catalogs are bounded4MiB/8192 entries; previews8MiB/4MP.
 * Native dimensions, names and IDs are preserved; requestedPixels only reduces
 * the decoded preview. AVI/WAVE and original GIF/JPEG/CUR bytes use open().
 */
public final class WmAssets {
    private WmAssets(){}
    private static final Map<Context,Index> CACHE=new WeakHashMap<Context,Index>();

    /** Immutable catalog entry. Provider/resource/source fields describe provenance, not lookup paths. */
    public static final class Entry {
        public final String id,type,label,labelKo,provider,resourceId,asset,originalAsset,sourceHash;
        public final int width,height;
        private Entry(String id,String type,String label,String labelKo,String provider,
                      String resourceId,String asset,String originalAsset,String hash,int width,int height){
            this.id=id;this.type=type;this.label=label;this.labelKo=labelKo;this.provider=provider;
            this.resourceId=resourceId;this.asset=asset;this.originalAsset=originalAsset;
            this.sourceHash=hash;this.width=width;this.height=height;
        }
        public String label(Locale locale){return locale!=null&&"ko".equals(locale.getLanguage())&&!labelKo.isEmpty()?labelKo:label;}
    }
    private static final class Index {
        final WmResourcePack pack;
        final List<Entry> all;
        final Map<String,Entry> ids;
        final Map<String,List<Entry>> types;
        Index(List<Entry> entries,WmResourcePack pack)throws IOException{
            this.pack=pack;
            Map<String,Entry> byId=new LinkedHashMap<String,Entry>();
            Map<String,List<Entry>> byType=new HashMap<String,List<Entry>>();
            for(Entry entry:entries){
                if(byId.put(entry.id,entry)!=null)throw new IOException("Duplicate catalog resource ID: "+entry.id);
                List<Entry> group=byType.get(entry.type);
                if(group==null){group=new ArrayList<Entry>();byType.put(entry.type,group);}group.add(entry);
            }
            Map<String,List<Entry>> immutableTypes=new HashMap<String,List<Entry>>();
            for(Map.Entry<String,List<Entry>> group:byType.entrySet())immutableTypes.put(group.getKey(),Collections.unmodifiableList(group.getValue()));
            all=Collections.unmodifiableList(entries);ids=Collections.unmodifiableMap(byId);types=Collections.unmodifiableMap(immutableTypes);
        }
    }
    private static synchronized Index index(Context context)throws IOException{
        Context app=context.getApplicationContext();if(app==null)app=context;
        WmResourcePack pack=WmResources.prepare(app);Index cached=CACHE.get(app);if(cached!=null&&cached.pack==pack)return cached;
        List<Entry> entries=new ArrayList<Entry>();
        for(String path:new String[]{"metadata/library.json","metadata/folders.json"}){
            boolean folders="metadata/folders.json".equals(path);
            try{read(app,path,folders?"icons":"assets",folders,entries);}
            catch(FileNotFoundException missing){} // Resource-free apps have no artwork catalog.
        }
        Index loaded=new Index(entries,pack);CACHE.put(app,loaded);return loaded;
    }
    /** All library resources plus named folders, in catalog order. */
    public static List<Entry> catalog(Context context)throws IOException{return index(context).all;}
    /** Types include icon, folder, bitmap, cursor, png, gif, jpeg, bmp,
     * animation_avi and audio_wav. An unknown type returns an empty list. */
    public static List<Entry> list(Context context,String type)throws IOException{
        if(type==null)return catalog(context);List<Entry> found=index(context).types.get(type);
        return found==null?Collections.<Entry>emptyList():found;
    }
    /** Returns null for an unregistered ID; asset paths are never accepted as IDs. */
    public static Entry byId(Context context,String id)throws IOException{return index(context).ids.get(id);}
    private static Entry require(Context context,String id)throws IOException{
        Entry entry=byId(context,id);if(entry==null)throw new FileNotFoundException("Unknown catalog resource ID: "+id);return entry;
    }
    /** Opens original GIF/JPEG/CUR when present; otherwise the registered asset.
     * AVI/WAVE remain byte-for-byte original streams. The caller closes it. */
    public static InputStream open(Context context,String id)throws IOException{
        Entry entry=require(context,id);return WmResources.openAsset(context,entry.originalAsset.isEmpty()?entry.asset:entry.originalAsset);
    }
    /** Decode a PNG preview with a bounds pass and power-of-two sampling.
     * The largest returned bitmap dimension is at most requestedPixels, with
     * its original aspect ratio. Small native artwork is never enlarged.
     */
    public static Drawable loadDrawable(Context context,String id,int requestedPixels)throws IOException{
        if(requestedPixels<=0)throw new IllegalArgumentException("requestedPixels must be positive");
        Entry entry=require(context,id);if(!entry.asset.endsWith(".png"))throw new IOException("Resource has no PNG preview: "+id);
        java.io.File preview=WmResources.assetFile(context,entry.asset,8*1024*1024);BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
        try(InputStream stream=new java.io.FileInputStream(preview)){BitmapFactory.decodeStream(stream,null,bounds);}
        if(bounds.outWidth<=0||bounds.outHeight<=0||bounds.outWidth>4096||bounds.outHeight>4096||(long)bounds.outWidth*bounds.outHeight>4*1024*1024)throw new IOException("Invalid PNG preview: "+id);
        int largest=Math.max(bounds.outWidth,bounds.outHeight),sample=1;
        while(largest/(long)sample>requestedPixels*2L&&sample<(1<<29))sample*=2;
        BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=sample;
        options.inScaled=false;options.inPreferredConfig=Bitmap.Config.ARGB_8888;
        Bitmap bitmap;try(InputStream stream=new java.io.FileInputStream(preview)){bitmap=BitmapFactory.decodeStream(stream,null,options);}
        if(bitmap==null)throw new IOException("Cannot decode PNG preview: "+id);
        int decoded=Math.max(bitmap.getWidth(),bitmap.getHeight());
        if(decoded>requestedPixels){
            int width=Math.max(1,(int)(bitmap.getWidth()*(requestedPixels/(double)decoded)));
            int height=Math.max(1,(int)(bitmap.getHeight()*(requestedPixels/(double)decoded)));
            Bitmap reduced=Bitmap.createScaledBitmap(bitmap,width,height,true);
            if(reduced!=bitmap){bitmap.recycle();bitmap=reduced;}
        }
        bitmap.setDensity(Bitmap.DENSITY_NONE);
        return new BitmapDrawable(context.getResources(),bitmap);
    }
    private static void read(Context context,String path,String array,boolean folders,List<Entry> output)throws IOException{
        try(JsonReader reader=new JsonReader(new InputStreamReader(new java.io.FileInputStream(WmResources.assetFile(context,path,4*1024*1024)),"UTF-8"))){
            boolean found=false;reader.beginObject();
            while(reader.hasNext()){
                String name=reader.nextName();if(!array.equals(name)){reader.skipValue();continue;}
                found=true;reader.beginArray();while(reader.hasNext()){if(output.size()>=8192)throw new IOException("Too many catalog entries");output.add(entry(reader,folders));}reader.endArray();
            }
            reader.endObject();if(!found)throw new IOException("Missing resource catalog entries: "+path);
        }catch(IllegalStateException|NumberFormatException invalid){throw new IOException("Invalid resource catalog",invalid);}
    }
    private static Entry entry(JsonReader reader,boolean folder)throws IOException{
        String id="",type=folder?"folder":"",label="",labelKo="",provider="",resource="",asset="",original="",hash="";int width=0,height=0;
        reader.beginObject();while(reader.hasNext()){
            String key=reader.nextName();
            if(reader.peek()==JsonToken.NULL){reader.nextNull();continue;}
            if("id".equals(key))id=reader.nextString();
            else if("kind".equals(key)&&!folder)type=reader.nextString();
            else if("label".equals(key))label=reader.nextString();
            else if("label_ko".equals(key))labelKo=reader.nextString();
            else if("provider".equals(key))provider=reader.nextString();
            else if("resource_id".equals(key)||"group_resource_id".equals(key))resource=reader.nextString();
            else if("asset".equals(key))asset=reader.nextString();
            else if("original_asset".equals(key))original=reader.nextString();
            else if("source_hash".equals(key)||"source_sha256".equals(key))hash=reader.nextString();
            else if("width".equals(key))width=reader.nextInt();
            else if("height".equals(key))height=reader.nextInt();
            else reader.skipValue();
        }reader.endObject();
        if(id.isEmpty()||id.length()>160||type.isEmpty()||type.length()>80||width<0||height<0||width>4096||height>4096||!safe(asset)||(!original.isEmpty()&&!safe(original)))throw new IOException("Invalid resource catalog entry: "+id);
        return new Entry(id,type,label.isEmpty()?id:label,labelKo,provider,resource,asset,original,hash,width,height);
    }
    private static boolean safe(String path){
        if(!(path.startsWith("library/")||path.startsWith("folders/"))||path.indexOf('\\')>=0||path.indexOf(':')>=0||path.indexOf('\0')>=0)return false;
        for(String part:path.split("/",-1))if(part.isEmpty()||".".equals(part)||"..".equals(part))return false;
        return true;
    }
}
