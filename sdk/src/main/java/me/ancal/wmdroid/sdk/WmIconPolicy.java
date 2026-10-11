package me.ancal.wmdroid.sdk;

import android.content.ComponentName;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;

/** Immutable per-activity adaptive-icon policy, shared by Programs and SDK Start menus.
 * Blacklist excludes listed activities; whitelist includes only listed activities.
 * An existing boolean preference without a mode retains the previous all-app behavior.
 */
public final class WmIconPolicy {
    public final boolean enabled;
    public final String mode,json;
    private final Set<String> listed=new HashSet<>();
    public WmIconPolicy(boolean enabled,String mode,String json){
        this.enabled=enabled;this.mode="blacklist".equals(mode)||"whitelist".equals(mode)?mode:"all";
        this.json=json==null?"[]":json;
        try{JSONArray a=new JSONArray(this.json);for(int i=0;i<a.length();i++)if(a.opt(i) instanceof String)listed.add(canonical(a.getString(i)));}catch(JSONException invalid){}
    }
    public static WmIconPolicy read(SharedPreferences preferences){return new WmIconPolicy(preferences.getBoolean("android_icons_unframed",false),preferences.getString("android_icons_mode","all"),preferences.getString("android_icons_list","[]"));}
    public boolean allows(String component){if(!enabled||component==null||component.isEmpty())return false;boolean found=listed.contains(canonical(component));return "all".equals(mode)||("whitelist".equals(mode)?found:!found);}
    public static boolean hidden(SharedPreferences preferences,String component){
        if(component==null||component.isEmpty())return false;
        try{JSONArray a=new JSONArray(preferences.getString("hidden_apps","[]"));String id=canonical(component);for(int i=0;i<a.length();i++)if(a.opt(i) instanceof String&&id.equals(canonical(a.getString(i))))return true;}catch(JSONException invalid){}
        return false;
    }
    public static String canonical(String value){ComponentName c=ComponentName.unflattenFromString(value==null?"":value);return c==null?(value==null?"":value):c.flattenToString();}
}
