package me.ancal.wmdroid.sdk;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;

/** Read-only system state; missing services/permissions degrade without prompts. */
public final class WmDevice {
    private WmDevice(){}
    public static boolean airplane(Context c){return Build.VERSION.SDK_INT>=17?Settings.Global.getInt(c.getContentResolver(),Settings.Global.AIRPLANE_MODE_ON,0)!=0:Settings.System.getInt(c.getContentResolver(),Settings.System.AIRPLANE_MODE_ON,0)!=0;}
    /** SIM readiness is separate from registration with a cellular network. */
    public static final class Phone {
        public final boolean airplane;public final int sim,service;public final String operator;
        public Phone(boolean airplane,int sim,int service,String operator){this.airplane=airplane;this.sim=sim;this.service=service;this.operator=operator==null?"":operator;}
        public String icon(){
            if(airplane||service==android.telephony.ServiceState.STATE_POWER_OFF)return "radio_off";
            return sim==android.telephony.TelephonyManager.SIM_STATE_READY&&(service==android.telephony.ServiceState.STATE_IN_SERVICE||service<0&&!operator.isEmpty())?"cellular_ready":"cellular_off";
        }
        public String caption(){
            if(airplane||service==android.telephony.ServiceState.STATE_POWER_OFF)return "전화 해제";
            if(sim==android.telephony.TelephonyManager.SIM_STATE_ABSENT)return "SIM 없음";
            if(sim==android.telephony.TelephonyManager.SIM_STATE_PIN_REQUIRED)return "SIM PIN 필요";
            if(sim==android.telephony.TelephonyManager.SIM_STATE_PUK_REQUIRED)return "SIM PUK 필요";
            if(service==android.telephony.ServiceState.STATE_EMERGENCY_ONLY)return "긴급 통화만 가능";
            if(service==android.telephony.ServiceState.STATE_OUT_OF_SERVICE)return "서비스 없음";
            return "cellular_ready".equals(icon())&&!operator.isEmpty()?operator:"전화 상태 확인 필요";
        }
    }
    public static Phone phone(Context context){
        android.telephony.TelephonyManager manager=(android.telephony.TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        int sim=android.telephony.TelephonyManager.SIM_STATE_UNKNOWN,service=-1;String operator="";
        if(manager!=null){
            try{sim=manager.getSimState();operator=manager.getNetworkOperatorName();}catch(SecurityException|UnsupportedOperationException unavailable){}
            if(Build.VERSION.SDK_INT>=26)try{service=PhoneService.state(manager);}catch(SecurityException|UnsupportedOperationException unavailable){}
        }
        return new Phone(airplane(context),sim,service,operator);
    }
    private static final class PhoneService {
        static int state(android.telephony.TelephonyManager manager){android.telephony.ServiceState state=Build.VERSION.SDK_INT>=33?manager.getServiceState(android.telephony.TelephonyManager.INCLUDE_LOCATION_DATA_NONE):manager.getServiceState();return state==null?-1:state.getState();}
    }
    public static class Network {
        public final boolean connected,connecting,wifi,cellular,ethernet,validated;
        public Network(boolean connected,boolean wifi,boolean cellular,boolean ethernet,boolean validated){this(connected,false,wifi,cellular,ethernet,validated);}
        public Network(boolean connected,boolean connecting,boolean wifi,boolean cellular,boolean ethernet,boolean validated){this.connected=connected;this.connecting=connecting;this.wifi=wifi;this.cellular=cellular;this.ethernet=ethernet;this.validated=validated;}
        public boolean hasTransport(int transport){return transport==1?wifi:transport==0?cellular:transport==3&&ethernet;}
        public boolean hasCapability(int capability){return capability==16&&validated;}
    }
    public static Network network(Context c){
        ConnectivityManager manager=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);if(manager==null)return null;
        try{
            if(Build.VERSION.SDK_INT>=23){Network active=Modern.network(manager);if(active!=null)return active;}
            NetworkInfo n=manager.getActiveNetworkInfo();if(n!=null&&n.isConnected())return legacy(n);
            if(n!=null&&n.isConnectedOrConnecting())return legacy(n);
            NetworkInfo[] known=manager.getAllNetworkInfo();if(known!=null)for(NetworkInfo candidate:known)if(candidate!=null&&candidate.isConnectedOrConnecting())return legacy(candidate);
            return null;
        }catch(SecurityException unavailable){return null;}
    }
    private static Network legacy(NetworkInfo n){return new Network(n.isConnected(),!n.isConnected()&&n.isConnectedOrConnecting(),n.getType()==ConnectivityManager.TYPE_WIFI,n.getType()==ConnectivityManager.TYPE_MOBILE,n.getType()==ConnectivityManager.TYPE_ETHERNET,false);}
    private static final class Modern {
        static Network network(ConnectivityManager manager){NetworkCapabilities n=manager.getNetworkCapabilities(manager.getActiveNetwork());return n==null?null:new Network(true,n.hasTransport(1),n.hasTransport(0),n.hasTransport(3),n.hasCapability(16));}
    }
}
