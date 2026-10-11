package me.ancal.wmdroid.sdk;

import android.net.Uri;
import java.util.ArrayList;
import java.util.Random;

/** Ordered media sources; duplicates are valid. Reordering preserves the current
 * occurrence. Shuffle walks a permutation rather than repeating random tracks.
 * The native shuffle/history algorithm has not been recovered. UI-thread owner. */
public final class WmMediaQueue {
    public static final class Entry {
        public final Uri uri;public final String title;
        public Entry(Uri uri,String title){
            if(uri==null||!("content".equals(uri.getScheme())||"http".equals(uri.getScheme())||"https".equals(uri.getScheme())))throw new IllegalArgumentException("Picker content URI or HTTP(S) URL required");
            if("content".equals(uri.getScheme())){if(!uri.isHierarchical()||uri.getAuthority()==null||uri.getAuthority().isEmpty())throw new IllegalArgumentException("Content provider required");}else WmBrowserView.normalizeAddress(uri.toString());
            this.uri=uri;String name=title==null?uri.getLastPathSegment():title;this.title=name==null?"":name.length()>256?name.substring(0,256):name;
        }
    }
    private final ArrayList<Entry> entries=new ArrayList<>();
    private final Random random=new Random();
    private int current=-1;
    private boolean shuffle;
    private int[] order=new int[0];
    public Entry[] entries(){return entries.toArray(new Entry[0]);}
    public int size(){return entries.size();}public int currentIndex(){return current;}
    private static void validate(Entry[] values){if(values==null)throw new IllegalArgumentException("Sources required");for(Entry entry:values)if(entry==null)throw new IllegalArgumentException("Null source");}
    public void replace(Entry[] values,int selected){validate(values);if(selected< -1||selected>=values.length)throw new IllegalArgumentException("Current index outside sources");entries.clear();java.util.Collections.addAll(entries,values);current=selected;reorder();}
    public void append(Entry[] values){validate(values);java.util.Collections.addAll(entries,values);reorder();}
    public void select(int index){check(index);current=index;reorder();}
    public boolean remove(int index){check(index);boolean active=index==current;entries.remove(index);if(entries.isEmpty())current=-1;else if(active)current=Math.min(index,entries.size()-1);else if(index<current)current--;reorder();return active;}
    public void move(int from,int to){check(from);check(to);if(from==to)return;Entry entry=entries.remove(from);entries.add(to,entry);if(current==from)current=to;else if(from<current&&current<=to)current--;else if(to<=current&&current<from)current++;reorder();}
    public void clear(){entries.clear();current=-1;reorder();}
    public boolean shuffle(){return shuffle;}public void setShuffle(boolean value){if(shuffle==value)return;shuffle=value;reorder();}
    private void check(int index){if(index<0||index>=entries.size())throw new IllegalArgumentException("Source index outside queue");}
    private int cursor(){if(current<0)return -1;if(!shuffle)return current;for(int n=0;n<order.length;n++)if(order[n]==current)return n;throw new IllegalStateException("Shuffle order missing current source");}
    public boolean canAdvance(int direction,boolean repeat){if(direction!=1&&direction!=-1)throw new IllegalArgumentException("Direction must be -1 or1");if(entries.isEmpty())return false;int at=cursor();if(at<0)return direction==1;return repeat||at+direction>=0&&at+direction<entries.size();}
    /** Returns -1 at the end without changing the current occurrence. */
    public int advance(int direction,boolean repeat){if(!canAdvance(direction,repeat))return -1;int next=cursor()+direction;
        if(next>=entries.size()){if(shuffle&&entries.size()>1){reorder();next=1;}else next=0;}
        if(next<0)next=entries.size()-1;current=shuffle?order[next]:next;return current;
    }
    private void reorder(){order=new int[entries.size()];for(int n=0;n<order.length;n++)order[n]=n;if(!shuffle||order.length<2)return;
        int first=current<0?0:current;order[first]=0;order[0]=first;
        for(int n=order.length-1;n>1;n--){int pick=1+random.nextInt(n);int value=order[n];order[n]=order[pick];order[pick]=value;}
    }
}
