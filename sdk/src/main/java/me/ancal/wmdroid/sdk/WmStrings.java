package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.WeakHashMap;
import org.json.JSONException;
import org.json.JSONObject;

/** Exact authored/context keys. Never translate user/provider data or normalize keys.
 * Catalogs belong to each AssetManager, so an app's overrides cannot leak to another.
 */
public final class WmStrings {
    private WmStrings(){}
    private static final Map<AssetManager,Catalogs> CACHE=new WeakHashMap<AssetManager,Catalogs>();
    private static final class Catalogs {
        final Map<String,JSONObject> files=new HashMap<String,JSONObject>();
        String[] languages;
    }
    private static String tag(String value){
        if(value==null)return "ko";
        String result=value.replace('_','-').toLowerCase(Locale.US);
        return result.matches("[a-z]{2,3}(-[a-z0-9]{2,8})*")?result:"ko";
    }
    private static String parent(String language){int dash=language.lastIndexOf('-');return dash<0?null:language.substring(0,dash);}
    /** Packaged catalog tags; ko/en first, then other tags in lexical order. */
    public static synchronized String[] languages(Context context){
        AssetManager assets=context.getAssets();Catalogs state=cache(assets);
        if(state.languages==null){
            TreeSet<String> names=new TreeSet<String>();
            try{for(String file:assets.list("i18n"))if(file.endsWith(".json")){String name=file.substring(0,file.length()-5);if(name.equals(tag(name)))names.add(name);}}
            catch(IOException error){throw new IllegalStateException("UI catalogs could not be listed",error);}
            if(!names.remove("ko")||!names.remove("en"))throw new IllegalStateException("Korean and English UI catalogs required");
            String[] values=new String[names.size()+2];values[0]="ko";values[1]="en";int i=2;for(String name:names)values[i++]=name;
            state.languages=values;
        }
        return state.languages.clone();
    }
    /** Normalize an available tag, its packaged parent, or the Korean fallback. */
    public static String language(Context context,String value){
        String[] available=languages(context);
        for(String candidate=tag(value);candidate!=null;candidate=parent(candidate))for(String name:available)if(name.equals(candidate))return name;
        return "ko";
    }
    /** Full tag on API21+, primary language/country on earlier Android. */
    public static Locale locale(String language){
        String value=tag(language);
        if(Build.VERSION.SDK_INT>=21)return Locale.forLanguageTag(value);
        String[] parts=value.split("-");String country="";
        for(int i=1;i<parts.length;i++)if(parts[i].matches("[a-z]{2}|[0-9]{3}")){country=parts[i].toUpperCase(Locale.US);break;}
        return new Locale(parts[0],country);
    }
    public static String text(Context context,String language,String authored){
        if(authored==null)return null;
        String result=lookup(context,tag(language),authored,false);return result==null?authored:result;
    }
    /** Context key throughout the fallback hierarchy, then ordinary authored key. */
    public static String text(Context context,String language,String authored,String key){
        if(authored==null)return null;
        String result=key==null?null:lookup(context,tag(language),key,true);
        return result==null?text(context,language,authored):result;
    }
    private static synchronized String lookup(Context context,String language,String key,boolean contextual){
        AssetManager assets=context.getAssets();Catalogs state=cache(assets);
        for(String candidate=language;candidate!=null;candidate=parent(candidate)){
            JSONObject values=catalog(assets,state,candidate);
            if(values!=null&&values.has(key)){String value=values.optString(key);if(!contextual||!"ko".equals(candidate)||!key.equals(value))return value;}
        }
        // Korean identity keys are ordinary authored entries, not contextual
        // overrides: preserve a consumer's distinct Korean authored fallback.
        if(!"ko".equals(language)){JSONObject values=catalog(assets,state,"ko");if(values.has(key)){String value=values.optString(key);if(!contextual||!key.equals(value))return value;}}
        return null;
    }
    private static Catalogs cache(AssetManager assets){Catalogs state=CACHE.get(assets);if(state==null){state=new Catalogs();CACHE.put(assets,state);}return state;}
    private static JSONObject catalog(AssetManager assets,Catalogs state,String language){
        if(state.files.containsKey(language))return state.files.get(language);
        JSONObject values=null;
        try(InputStream input=assets.open("i18n/"+language+".json")){
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;
            while((count=input.read(buffer))!=-1)bytes.write(buffer,0,count);
            values=new JSONObject(new String(bytes.toByteArray(),Charset.forName("UTF-8")));
            for(Iterator<String> keys=values.keys();keys.hasNext();){String key=keys.next();if(!(values.get(key) instanceof String))throw new JSONException("String UI value required: "+key);}
        }catch(FileNotFoundException absent){
            if("ko".equals(language)||"en".equals(language))throw new IllegalStateException("UI catalog missing: "+language,absent);
        }catch(IOException|JSONException invalid){throw new IllegalStateException("UI catalog could not be loaded: "+language,invalid);}
        state.files.put(language,values);return values;
    }
}
