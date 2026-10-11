package me.ancal.wmdroid.sdk;

import android.os.Handler;
import android.os.Looper;
import java.text.Collator;

/** Process-local UI-thread adaptation of GWES A90F8/A5650/A5570.
 * Shared by closed fields and explicitly enabled ordinary popup lists.
 * Native system-wide ownership/window timers/NLS remain separate contracts. */
final class WmCharacterSearch {
    private WmCharacterSearch(){}
    private static final char[] buffer=new char[257];
    private static int length;
    private static final Handler timer=new Handler(Looper.getMainLooper());
    private static final Runnable expire=()->length=0;
    static void reset(){length=0;timer.removeCallbacks(expire);}
    static String characters(android.view.KeyEvent event){
        if(event.getAction()!=android.view.KeyEvent.ACTION_MULTIPLE||event.getKeyCode()!=android.view.KeyEvent.KEYCODE_UNKNOWN||(event.getMetaState()&(android.view.KeyEvent.META_CTRL_MASK|android.view.KeyEvent.META_ALT_MASK|android.view.KeyEvent.META_META_MASK))!=0)return null;
        String text=event.getCharacters();if(text==null||text.isEmpty())return null;
        for(int i=0;i<text.length();i++)if(text.charAt(i)<32&&text.charAt(i)!=8&&text.charAt(i)!=27)return null;
        return text;
    }
    static int search(char value,String[] labels,int[] order,int selected,boolean sorted,Collator comparator){
        if(value==27){reset();buffer[0]=0;return -1;}
        if(value==8){
            if(length==0||length==256)return -1;
            // ARM A9208/A920C/A9218: NUL at OLD length, then decrement.
            buffer[length--]=0;if(!sorted)return -1;
        }else{
            if(length==256)return -1;
            buffer[length++]=value;buffer[length]=0;
        }
        int start=sorted?0:selected+1;
        if(sorted){timer.removeCallbacks(expire);timer.postDelayed(expire,1000);}else length=0;
        int match=find(labels,order,start,comparator);
        if(match<0&&length>0){
            int old=length--;
            if(old==2&&buffer[0]==buffer[1]){buffer[1]=0;match=find(labels,order,selected+1,comparator);}
        }
        return match;
    }
    private static int find(String[] labels,int[] order,int start,Collator comparator){
        if(buffer[0]==0)return -1;
        for(int offset=0;offset<labels.length;offset++){
            int display=(start+offset)%labels.length;String label=labels[order==null?display:order[display]];int first=0;
            if(buffer[0]!='['&&!label.isEmpty()&&label.charAt(0)=='['){first=1;if(first<label.length()&&label.charAt(first)=='-')first++;}
            for(int unit=0;unit<buffer.length;unit++){
                char query=buffer[unit];if(query==0)return display;
                if(first+unit>=label.length())break;
                char a=Character.toUpperCase(query),b=Character.toUpperCase(label.charAt(first+unit));
                if(a!=b){if(comparator.compare(String.valueOf(a),String.valueOf(b))==0)return display;break;}
            }
        }
        return -1;
    }
}
