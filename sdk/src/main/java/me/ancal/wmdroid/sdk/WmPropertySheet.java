package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;

/** Caller-owned property pages with the shared bottom tab strip. Pages stay
 * attached when hidden; this control owns no footer, scrolling or page state. */
public final class WmPropertySheet extends LinearLayout implements WmControls.BackHandler,WmControls.RestyleListener {
    public static final int PSN_SETACTIVE=-200,PSN_KILLACTIVE=-201,PSN_APPLY=-202,PSN_RESET=-203,PSN_QUERYCANCEL=-209;
    public static final int PSNRET_NOERROR=0,PSNRET_INVALID=1,PSNRET_INVALID_NOCHANGEPAGE=2;
    public interface OnPageChangedListener {void selected(int index);}
    /** Return0 to allow; KILLACTIVE nonzero vetoes. SETACTIVE -1 skips forward,
     * otherwise a matching positive page ID redirects to that page. */
    public interface OnPageNotificationListener {
        int notifyPage(int index,int notification);
        /** APPLY/RESET parameter0 is Apply/Cancel, parameter1 is OK/exit. */
        default int notifyPage(int index,int notification,int parameter){return notifyPage(index,notification);}
    }
    private final FrameLayout body;
    private final WmTabStrip strip;
    private final ArrayList<View> pages=new ArrayList<View>();
    private final ArrayList<TextView> tabs=new ArrayList<TextView>();
    private final ArrayList<Integer> pageIDs=new ArrayList<Integer>();
    private final ArrayList<Boolean> changedPages=new ArrayList<Boolean>();
    private Typeface font;
    private int tabColor,selected=-1;
    private OnPageChangedListener listener;
    private OnPageNotificationListener notifications;
    private boolean notifying;

    public WmPropertySheet(Context context,Typeface font,int tabColor){
        super(context);this.font=font;this.tabColor=tabColor;setOrientation(VERTICAL);
        body=new FrameLayout(context);addView(body,new LayoutParams(-1,0,1));
        strip=new WmTabStrip(context,tabColor);addView(strip,new LayoutParams(-1,WmMetrics.TAB));
    }
    public int addPage(CharSequence title,View page){
        if(notifying)throw new IllegalStateException("Cannot add pages during page notification");
        int pageId=pages.size()+1;
        if(pageId>65535||pageIDs.contains(pageId)){
            pageId=1;while(pageId<=65535&&pageIDs.contains(pageId))pageId++;
            if(pageId>65535)throw new IllegalStateException("No unused page IDs");
        }
        return addPage(pageId,title,page);
    }
    /** Page IDs are dialog-template identifiers, independent of tab index.
     * Duplicate numeric IDs are allowed; redirect lookup uses the first match. */
    public int addPage(int pageId,CharSequence title,View page){
        if(notifying)throw new IllegalStateException("Cannot add pages during page notification");
        if(pageId<1||pageId>65535)throw new IllegalArgumentException("Page ID1..65535 required");
        if(title==null||page==null)throw new IllegalArgumentException("Page title and view required");
        if(page.getParent()!=null)throw new IllegalArgumentException("Page already attached");
        final int index=pages.size();
        TextView tab=strip.addTab(title.toString(),font,()->selectPage(index));
        pages.add(page);tabs.add(tab);pageIDs.add(pageId);changedPages.add(false);body.addView(page,new FrameLayout.LayoutParams(-1,-1));
        if(selected<0)selected=0;
        refreshPages();return index;
    }
    public int pageCount(){return pages.size();}
    /** Content region excluding the persistent tab strip, for partial presentation. */
    public View getClientView(){return body;}
    /** -1 until the first page is added. */
    public int selectedPage(){return selected;}
    public void selectPage(int index){trySelectPage(index);}
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        int key=event.getKeyCode();
        if(!isEnabled()||selected<0||!event.isCtrlPressed()||(key!=KeyEvent.KEYCODE_TAB&&key!=KeyEvent.KEYCODE_PAGE_UP&&key!=KeyEvent.KEYCODE_PAGE_DOWN))return super.dispatchKeyEvent(event);
        if(event.getAction()==KeyEvent.ACTION_UP)return true;
        if(event.getAction()!=KeyEvent.ACTION_DOWN)return super.dispatchKeyEvent(event);
        // commctrl329F4AC: ordinary property sheets wrap before dialog dispatch.
        int direction=key==KeyEvent.KEYCODE_PAGE_UP||key==KeyEvent.KEYCODE_TAB&&event.isShiftPressed()?-1:1;
        trySelectPage((selected+direction+pages.size())%pages.size());
        return true;
    }
    /** Lifecycle restoration only: validates the index, without page notifications/listeners. */
    public void restoreSelectedPage(int index){if(notifying)throw new IllegalStateException("Cannot restore page during notification");checkIndex(index);commitPage(index);}
    /** Notifications precede the UI commit. Pages remain eagerly attached; this is
     * not the complete native property-sheet message loop or lazy dialog creation. */
    public boolean trySelectPage(int index){
        if(notifying)return false;
        checkIndex(index);if(index==selected)return true;
        OnPageNotificationListener snapshot=notifications;
        int approved;boolean changed;
        notifying=true;
        try{
            if(snapshot!=null&&selected>=0&&snapshot.notifyPage(selected,PSN_KILLACTIVE)!=0)return false;
            boolean tabFocus=strip.hasFocus();
            approved=resolvePage(index,snapshot);
            if(approved<0)return false;
            changed=activatePage(approved,tabFocus);
        }finally{notifying=false;}
        // Preserve existing post-commit reentrant selection from app callbacks.
        if(changed&&listener!=null)listener.selected(approved);
        return true;
    }
    private int resolvePage(int index,OnPageNotificationListener snapshot){
        if(snapshot==null)return index;
        int candidate=index;
        for(int count=0;count<pages.size();count++){
            int result=snapshot.notifyPage(candidate,PSN_SETACTIVE);
            if(result==0)return candidate;
            if(result==-1)candidate=candidate+1<pages.size()?candidate+1:selected;
            else{
                int redirected=pageIDs.indexOf(result);
                if(redirected<0)return candidate;
                candidate=redirected;
            }
        }
        return -1;
    }
    private boolean commitPage(int approved){
        if(approved<0||approved==selected)return false;
        selected=approved;refreshPages();return true;
    }
    private boolean activatePage(int approved,boolean tabFocus){
        boolean changed=commitPage(approved);
        if(changed&&getWindowToken()!=null){
            // commctrl329E894/329E768: activation owns focus, before app callbacks.
            // ponytail: Android forward traversal substitutes GetNextDlgTabItem;
            // replace when exact HWND dialog order is available.
            if(tabFocus||!pages.get(selected).requestFocus(View.FOCUS_FORWARD))tabs.get(selected).requestFocus();
            else{
                View focus=pages.get(selected).findFocus();
                if(focus instanceof EditText)((EditText)focus).selectAll();
            }
        }
        return changed;
    }
    public void setPageChanged(int index,boolean changed){checkIndex(index);changedPages.set(index,changed);}
    public boolean isPageChanged(int index){checkIndex(index);return changedPages.get(index);}
    public boolean hasChanges(){return changedPages.contains(Boolean.TRUE);}
    /** closing=false sends Apply, true sends OK notification arguments. Returns true
     * when all steps approve. A veto/exception does not roll back prior page effects
     * or cleared changed flags. The caller completes its Activity/window. */
    public boolean applyPages(boolean closing){return finishPages(true,closing);}
    /** Cancel/reset with exit=false; see cancelPages(boolean). */
    public boolean cancelPages(){return cancelPages(false);}
    /** Query cancel then reset pages with the exit argument (1 or 0). Returns true
     * when all approve; prior effects/changed flags survive later vetoes or exceptions.
     * Does not close the Activity/window. */
    public boolean cancelPages(boolean exit){return finishPages(false,exit);}
    private boolean finishPages(boolean applying,boolean closing){
        if(notifying||selected<0)return false;
        OnPageNotificationListener snapshot=notifications;
        int approved=-1;boolean changed=false,success=true;
        notifying=true;
        try{
            int gate=applying?PSN_KILLACTIVE:PSN_QUERYCANCEL;
            if(snapshot!=null&&snapshot.notifyPage(selected,gate)!=0)return false;
            boolean tabFocus=strip.hasFocus();
            int notification=applying?PSN_APPLY:PSN_RESET;
            for(int index=0;index<pages.size();index++){
                int result=snapshot==null?0:snapshot.notifyPage(index,notification,closing?1:0);
                if(result!=PSNRET_NOERROR){
                    success=false;
                    if(result!=PSNRET_INVALID_NOCHANGEPAGE)approved=resolvePage(index,snapshot);
                    break;
                }
                // Native partial apply/reset: prior successful pages stay clean
                // even if a later page vetoes or throws. No rollback is implied.
                changedPages.set(index,false);
            }
            if(success&&applying&&!closing){approved=resolvePage(selected,snapshot);if(approved<0)success=false;}
            changed=activatePage(approved,tabFocus);
        }finally{notifying=false;}
        if(changed&&listener!=null)listener.selected(approved);
        return success;
    }
    public void setPageNotificationListener(OnPageNotificationListener listener){notifications=listener;}
    public void setPageTitle(int index,CharSequence title){
        checkIndex(index);if(title==null)throw new IllegalArgumentException("Page title required");
        TextView tab=tabs.get(index);tab.setText(title.toString());
        tab.setLayoutParams(new LinearLayout.LayoutParams(WmControls.tabWidth(tab),WmMetrics.TAB));
    }
    public void setOnPageChangedListener(OnPageChangedListener listener){this.listener=listener;}
    private void checkIndex(int index){if(index<0||index>=pages.size())throw new IndexOutOfBoundsException("Invalid page index");}
    private void refreshPages(){
        for(int i=0;i<pages.size();i++){
            pages.get(i).setVisibility(i==selected?View.VISIBLE:View.GONE);
            WmControls.tab(tabs.get(i),i==selected,tabColor);
        }
    }
    @Override public boolean back(){return selected>=0&&WmControls.back(pages.get(selected));}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        font=regular;this.tabColor=tabColor;
        strip.onWmRestyle(regular,bold,selection,tabColor);
        for(TextView tab:tabs){tab.setTypeface(regular);WmFonts.attach(tab,regular);tab.setLayoutParams(new LinearLayout.LayoutParams(WmControls.tabWidth(tab),WmMetrics.TAB));}
        refreshPages();
    }
}
