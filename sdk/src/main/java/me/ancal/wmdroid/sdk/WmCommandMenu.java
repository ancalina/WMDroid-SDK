package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.util.ArrayList;

/** Logical-window command popup. Root uses the native command-centre anchor.
 * Child menus retain their parent and open beside its selected row, clamped to
 * the client area. Child placement/scroll metrics remain Android adaptations.
 */
public final class WmCommandMenu extends FrameLayout implements WmControls.BackHandler,WmControls.RestyleListener {
    private final WmCommandBar bar;
    private final boolean right;
    private final FrameLayout frame;
    /** Root menu; use activeMenu() for the currently open child. */
    public final WmRadioMenu menu;
    private final Runnable onDismiss;
    private final WmRadioMenu.Selection action;
    private final String selectedSuffix;
    private Typeface font;
    private int selection;
    private boolean dismissed;
    private final ArrayList<Level> levels=new ArrayList<>();
    private static final class Level {
        final WmMenuItem[] entries;final WmRadioMenu body;final FrameLayout frame;final int parentRow;
        Level(WmMenuItem[] entries,WmRadioMenu body,FrameLayout frame,int parentRow){this.entries=entries;this.body=body;this.frame=frame;this.parentRow=parentRow;}
    }
    /** Compatible flat API: callbacks receive the selected array index. */
    public WmCommandMenu(Context context,WmCommandBar bar,boolean right,Typeface font,String[] labels,int selected,String selectedSuffix,int selection,WmRadioMenu.Selection action,Runnable onDismiss){
        this(context,bar,right,font,legacy(labels,selected),selectedSuffix,selection,action,onDismiss);
    }
    /** Tree API: only enabled leaf commands dispatch, using WmMenuItem.id. */
    public WmCommandMenu(Context context,WmCommandBar bar,boolean right,Typeface font,WmMenuItem[] entries,String selectedSuffix,int selection,WmRadioMenu.Selection action,Runnable onDismiss){
        super(context);if(bar==null||action==null)throw new IllegalArgumentException("Command bar/action required");
        this.bar=bar;this.right=right;this.font=font;this.selectedSuffix=selectedSuffix;this.selection=selection;this.action=action;this.onDismiss=onDismiss;
        setOnClickListener(v->dismiss());Level root=addLevel(entries,-1);menu=root.body;frame=root.frame;
    }
    private static WmMenuItem[] legacy(String[] labels,int selected){
        if(labels==null||labels.length==0||selected < -1||selected>=labels.length)throw new IllegalArgumentException("Invalid command menu");
        WmMenuItem[] entries=new WmMenuItem[labels.length];for(int i=0;i<labels.length;i++)entries[i]=WmMenuItem.checked(i,labels[i],i==selected);return entries;
    }
    private Level addLevel(WmMenuItem[] entries,int parentRow){
        if(entries==null||entries.length==0)throw new IllegalArgumentException("Empty command menu");
        final int depth=levels.size();WmMenuItem[] copy=entries.clone();
        WmRadioMenu body=new WmRadioMenu(getContext(),font,copy,selectedSuffix,selection,index->choose(depth,copy,index));
        FrameLayout panel=new FrameLayout(getContext());panel.setBackgroundDrawable(body.getBackground());panel.setOnClickListener(v->{});body.setBackgroundDrawable(null);
        WmScrollView scroll=new WmScrollView(getContext(),body);WmPaint paint=new WmPaint(getContext(),false,WmPaint.Role.MENU);
        paint.addView(scroll,new FrameLayout.LayoutParams(-1,-1));panel.addView(paint,new FrameLayout.LayoutParams(-1,-1));
        Level level=new Level(copy,body,panel,parentRow);levels.add(level);addView(panel,new FrameLayout.LayoutParams(-2,-2));return level;
    }
    private void choose(int depth,WmMenuItem[] source,int index){
        if(dismissed||depth>=levels.size())return;Level parent=levels.get(depth);
        if(parent.entries!=source||index<0||index>=source.length)return;WmMenuItem item=parent.entries[index];
        if(!item.enabled||item.separator)return;
        if(item.hasChildren()){
            trim(depth+1);parent.body.setSubmenuSelected(index);addLevel(item.children(),index);requestLayout();
        }else{dismiss();action.choose(item.id);}
    }
    private void trim(int keep){
        for(int i=levels.size()-1;i>=keep;i--){Level child=levels.remove(i);removeView(child.frame);}
        if(!levels.isEmpty())levels.get(levels.size()-1).body.setSubmenuSelected(-1);
        requestLayout();
    }
    public WmRadioMenu activeMenu(){return levels.get(levels.size()-1).body;}
    public int depth(){return levels.size();}
    /** Consume Back for one child; false lets the containing shell close the root. */
    @Override public boolean back(){if(dismissed||levels.size()<2)return false;trim(levels.size()-1);return true;}
    private boolean end(){
        if(dismissed)return false;dismissed=true;bar.key(right).setSelected(false);bar.key(right).setPressed(false);
        for(Level level:levels)level.body.setSubmenuSelected(-1);return true;
    }
    public void dismiss(){
        if(!end())return;
        if(getParent() instanceof ViewGroup)((ViewGroup)getParent()).removeView(this);
        if(onDismiss!=null)onDismiss.run();
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        // Scrolling or choosing an ancestor closes only descendants, never its root.
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN&&!dismissed){
            for(int i=levels.size()-1;i>=0;i--){FrameLayout panel=levels.get(i).frame;if(event.getX()>=panel.getLeft()&&event.getX()<panel.getRight()&&event.getY()>=panel.getTop()&&event.getY()<panel.getBottom()){if(i+1<levels.size())trim(i+1);break;}}
        }
        return super.dispatchTouchEvent(event);
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getSize(widthSpec),height=MeasureSpec.getSize(heightSpec);setMeasuredDimension(width,height);
        int available=Math.max(0,height-2*WmMetrics.BAR);
        for(Level level:levels){
            int menuWidth=Math.min(width,Math.max(48,level.body.menuWidth()));level.body.measure(MeasureSpec.makeMeasureSpec(menuWidth,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));
            int naturalHeight=level.body.getMeasuredHeight();if(naturalHeight>available)menuWidth=Math.min(width,menuWidth+26);
            level.frame.measure(MeasureSpec.makeMeasureSpec(menuWidth,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.min(naturalHeight,available),MeasureSpec.EXACTLY));
        }
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        int anchor=right?getWidth()-WmCommandBar.KEY_WIDTH/2:WmCommandBar.KEY_WIDTH/2,bottom=getHeight()-WmMetrics.BAR;
        int left=Math.max(0,Math.min(getWidth()-frame.getMeasuredWidth(),anchor-frame.getMeasuredWidth()/2));
        frame.layout(left,bottom-frame.getMeasuredHeight(),left+frame.getMeasuredWidth(),bottom);
        for(int i=1;i<levels.size();i++){
            Level child=levels.get(i),parent=levels.get(i-1);View row=parent.body.row(child.parentRow);Rect bounds=new Rect(0,0,row.getWidth(),row.getHeight());offsetDescendantRectToMyCoords(row,bounds);
            int width=child.frame.getMeasuredWidth(),height=child.frame.getMeasuredHeight();
            int x=parent.frame.getRight()-2;if(x+width>getWidth())x=parent.frame.getLeft()-width+2;x=Math.max(0,Math.min(getWidth()-width,x));
            int y=Math.max(Math.min(WmMetrics.BAR,bottom-height),Math.min(bottom-height,bounds.top-2));
            child.frame.layout(x,y,x+width,y+height);
        }
    }
    /** Stores font/selection and requests measurement; update existing child rows
     * through WmControls.restyleTree on the complete command menu. */
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){this.font=regular;this.selection=selection;requestLayout();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(!dismissed)bar.key(right).setSelected(true);}
    @Override protected void onDetachedFromWindow(){boolean notify=end();super.onDetachedFromWindow();if(notify&&onDismiss!=null)onDismiss.run();}
}
