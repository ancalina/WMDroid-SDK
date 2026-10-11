package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.HashSet;
import java.util.ArrayList;

/** Caller-owned settings metadata and callbacks, with retained category pages.
 * Keys must be globally unique across categories. No settings are read or written.
 * Grid and caption origins model native192DPI metrics; Android caption raster remains provisional.
 */
public class WmSettingsView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener {
    public interface Listener {void open(String key);}
    /** Immutable metadata; the supplied Drawable remains caller-owned. */
    public static final class Item {
        public final String key,authoredCaption;
        public final Drawable icon;
        public Item(String key,String authoredCaption,Drawable icon){
            requireText(key,"Item key");requireText(authoredCaption,"Item caption");
            if(icon==null)throw new IllegalArgumentException("Item icon required");
            this.key=key;this.authoredCaption=authoredCaption;this.icon=icon;
        }
    }
    public static final class Category {
        public final String authoredTitle;
        private final Item[] items;
        public Category(String authoredTitle,Item[] items){
            requireText(authoredTitle,"Category title");
            if(items==null)throw new IllegalArgumentException("Category items required");
            this.authoredTitle=authoredTitle;this.items=items.clone();
            for(Item item:this.items)if(item==null)throw new IllegalArgumentException("Category item required");
        }
        public Item[] items(){return items.clone();}
    }
    // Native icon point is2*CYBORDER; paint applies -CYEDGE+2*CYBORDER.
    // Label begins at icon point+icon height+2*CYEDGE. See settings-caption-origin-evidence.json.
    private static final int ICON_TOP=4*WmMetrics.BORDER-WmMetrics.EDGE;
    private static final int CAPTION_TOP=2*WmMetrics.BORDER+64+2*WmMetrics.EDGE;
    private WmPaint host;
    private final WmPropertySheet sheet;
    private final WmPaint[] pages;
    private final LinearLayout[] grids;
    private final Category[] categories;
    private Typeface font;
    private final Listener open;
    private int selection,face,selectedGroup;
    private boolean restoring;
    private LinearLayout rows;

    /** Captions use WM lowercase UTF-16 insertion comparison (incoming limit 260 units);
     * ties insert before existing items. Language changes sort translated captions again. */
    public WmSettingsView(Context context,Typeface font,String language,int selection,int face,Category[] categories,Runnable onTab,Listener open){
        super(context);
        if(categories==null||categories.length==0)throw new IllegalArgumentException("Settings categories required");
        if(onTab==null||open==null)throw new IllegalArgumentException("Settings callbacks required");
        this.categories=categories.clone();
        HashSet<String> keys=new HashSet<String>();
        for(Category category:this.categories){
            if(category==null)throw new IllegalArgumentException("Settings category required");
            for(Item item:category.items)if(!keys.add(item.key))throw new IllegalArgumentException("Duplicate settings key: "+item.key);
        }
        this.font=font;this.selection=selection;this.face=face;this.open=open;
        pages=new WmPaint[categories.length];grids=new LinearLayout[categories.length];
        setOrientation(VERTICAL);setBackgroundColor(Color.WHITE);
        sheet=new WmPropertySheet(context,font,face);
        for(int group=0;group<pages.length;group++){
            pages[group]=buildPage(group,language);
            sheet.addPage(WmStrings.text(context,language,this.categories[group].authoredTitle),pages[group]);
        }
        host=pages[0];rows=grids[0];
        sheet.setOnPageChangedListener(group->{
            selectedGroup=group;host=pages[group];rows=grids[group];
            if(!restoring){onTab.run();if(host.getWindowToken()!=null)host.replay();}
        });
        addView(sheet,new LayoutParams(-1,-1));
    }
    private static void requireText(String value,String name){
        if(value==null||value.isEmpty())throw new IllegalArgumentException(name+" required");
    }
    /** Hosts may adapt presentation options without replacing the page contents. */
    protected WmPaint createSurface(Context context){return new WmPaint(context,true,WmPaint.Role.GRID);}
    /** Replace artwork in place; preserve pages, scroll, selection and callbacks. */
    public void setIcon(String key,Drawable icon){
        requireText(key,"Item key");if(icon==null)throw new IllegalArgumentException("Item icon required");
        for(LinearLayout grid:grids){View view=grid.findViewWithTag("setting-icon:"+key);if(view instanceof ImageView){((ImageView)view).setImageDrawable(icon);return;}}
        throw new IllegalArgumentException("Unknown Settings key: "+key);
    }
    public WmPropertySheet propertySheet(){return sheet;}
    public int selectedPage(){return sheet.selectedPage();}
    public void select(int group){sheet.selectPage(group);}
    /** Schema1 stores only category index and each scroll offset. Recreate with
     * the same category/item ordering; settings values remain the owner's data. */
    public Bundle saveInteractionState(){
        Bundle state=new Bundle();state.putInt("schema",1);state.putInt("page",selectedPage());
        int[] positions=new int[pages.length];
        for(int i=0;i<pages.length;i++)positions[i]=((WmScrollView)pages[i].getChildAt(0)).getRequestedScrollPosition();
        state.putIntArray("scroll",positions);return state;
    }
    /** Null is a no-op. Invalid snapshots are rejected before changing controls.
     * Grid callbacks/replay are silent; property-sheet activation rules still apply.
     * A veto leaves offsets unchanged. Offsets wait for layout and clamp naturally. */
    public void restoreInteractionState(Bundle state){
        if(state==null)return;
        Object schema=state.get("schema"),page=state.get("page"),scroll=state.get("scroll");
        if(state.size()!=3||!(schema instanceof Integer)||(Integer)schema!=1||!(page instanceof Integer)||!(scroll instanceof int[]))throw new IllegalArgumentException("Invalid Settings interaction state");
        int index=(Integer)page;int[] positions=(int[])scroll;
        if(index<0||index>=pages.length||positions.length!=pages.length)throw new IllegalArgumentException("Compatible Settings categories required");
        for(int position:positions)if(position<0)throw new IllegalArgumentException("Nonnegative Settings scroll offsets required");
        restoring=true;try{if(!sheet.trySelectPage(index))throw new IllegalStateException("Settings category activation vetoed");}finally{restoring=false;}
        for(int i=0;i<pages.length;i++)((WmScrollView)pages[i].getChildAt(0)).scrollToPosition(positions[i]);
    }
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        font=regular;this.selection=selection;face=tabColor;resizeRows();
    }
    @Override public void onWmLanguageChanged(String language){
        for(int group=0;group<categories.length;group++){
            sheet.setPageTitle(group,WmStrings.text(getContext(),language,categories[group].authoredTitle));
            ((SettingsGrid)grids[group]).sortCells();
        }
    }
    /** Updates existing controls; page, scroll and selection identities survive. */
    public void refreshAppearance(Typeface font,String language){
        WmControls.restyleTree(this,font,font,selection,face);
        WmControls.localizeTree(this,language);
    }
    private void resizeRows(){for(LinearLayout grid:grids)if(grid!=null)resizeRows(grid);}
    private void resizeRows(LinearLayout rows){
        if(rows.getChildCount()==0)return;
        TextView caption=(TextView)((LinearLayout)((LinearLayout)rows.getChildAt(0)).getChildAt(0)).getChildAt(1);
        int height=Math.max(136,CAPTION_TOP+2*caption.getLineHeight());
        for(int i=0;i<rows.getChildCount();i++){
            LinearLayout row=(LinearLayout)rows.getChildAt(i);android.view.ViewGroup.LayoutParams size=row.getLayoutParams();
            if(size.height!=height){size.height=height;row.setLayoutParams(size);}
            for(int j=0;j<row.getChildCount();j++){
                View cell=row.getChildAt(j);size=cell.getLayoutParams();
                if(size.height!=height){size.height=height;cell.setLayoutParams(size);}
                if(cell instanceof LinearLayout){View label=((LinearLayout)cell).getChildAt(1);size=label.getLayoutParams();if(size.height!=height-CAPTION_TOP){size.height=height-CAPTION_TOP;label.setLayoutParams(size);}}
            }
        }
    }
    /** Native default metrics at192DPI: icon64, horizontal spacing150,
     * vertical spacing136; icon top4 and caption top76. Caption raster remains provisional.
     * Row wrappers may change on width changes; controls and metadata do not.
     */
    private final class SettingsGrid extends LinearLayout {
        private final LinearLayout[] cells;
        private final ArrayList<LinearLayout> ordered=new ArrayList<LinearLayout>();
        private int columns=3;
        private View[] blanks=new View[0];
        SettingsGrid(int group,String language){
            super(WmSettingsView.this.getContext());setOrientation(VERTICAL);
            Item[] items=categories[group].items;cells=new LinearLayout[items.length];
            for(int i=0;i<items.length;i++){
                Item item=items[i];LinearLayout cell=new LinearLayout(getContext());cells[i]=cell;
                if(android.os.Build.VERSION.SDK_INT>=26)cell.setDefaultFocusHighlightEnabled(false);
                cell.setOrientation(VERTICAL);cell.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);cell.setPadding(0,ICON_TOP,0,0);
                cell.setTag("setting:"+item.key);WmControls.bindDescription(cell,language,item.authoredCaption);
                ImageView image=new ImageView(getContext());image.setTag("setting-icon:"+item.key);image.setImageDrawable(item.icon);image.setScaleType(ImageView.ScaleType.FIT_CENTER);cell.addView(image,new LayoutParams(64,64));
                WmIconCaption caption=new WmIconCaption(getContext(),font,selection);WmControls.bindText(caption,language,item.authoredCaption);caption.setDuplicateParentStateEnabled(true);
                int height=Math.max(136,CAPTION_TOP+2*caption.getLineHeight());
                LayoutParams captionLayout=new LayoutParams(LayoutParams.MATCH_PARENT,height-CAPTION_TOP);captionLayout.leftMargin=WmMetrics.EDGE;captionLayout.rightMargin=WmMetrics.EDGE;captionLayout.topMargin=CAPTION_TOP-ICON_TOP-64;cell.addView(caption,captionLayout);
                cell.setLayoutParams(new LayoutParams(150,height));
                cell.setOnFocusChangeListener((v,focused)->v.setActivated(focused&&!v.isInTouchMode()));
                cell.setSoundEffectsEnabled(false);cell.setOnClickListener(v->open.open(item.key));
            }
            sortCells();
        }
        void sortCells(){
            ArrayList<LinearLayout> next=new ArrayList<LinearLayout>();
            // Preserve native insertion semantics, including equal/prefix ties.
            for(LinearLayout cell:cells){
                String incoming=((TextView)cell.getChildAt(1)).getText().toString();int lo=0,hi=next.size();
                while(lo<hi){int mid=(lo+hi)>>>1;String existing=((TextView)next.get(mid).getChildAt(1)).getText().toString();
                    if(WmIconOrder.compareForInsert(existing,incoming)>=0)hi=mid;else lo=mid+1;}
                next.add(lo,cell);
            }
            if(!ordered.equals(next)||getChildCount()==0){ordered.clear();ordered.addAll(next);reflow();}
        }
        private void reflow(){
            int count=(cells.length+columns-1)/columns;
            for(int i=0;i<getChildCount();i++)((LinearLayout)getChildAt(i)).removeAllViews();
            while(getChildCount()>count)removeViewAt(getChildCount()-1);
            int height=cells.length==0?136:Math.max(136,CAPTION_TOP+2*((TextView)cells[0].getChildAt(1)).getLineHeight());
            while(getChildCount()<count)addView(new LinearLayout(getContext()),new LayoutParams(-1,height));
            int remaining=count*columns-cells.length;
            if(blanks.length!=remaining){blanks=new View[remaining];for(int i=0;i<remaining;i++)blanks[i]=new View(getContext());}
            for(int i=0;i<count*columns;i++){
                View cell=i<ordered.size()?ordered.get(i):blanks[i-ordered.size()];
                ((LinearLayout)getChildAt(i/columns)).addView(cell,new LayoutParams(150,height));
            }
        }
        @Override protected void onMeasure(int widthSpec,int heightSpec){
            int width=MeasureSpec.getSize(widthSpec);
            if(MeasureSpec.getMode(widthSpec)!=MeasureSpec.UNSPECIFIED){int next=Math.max(1,width/150);if(columns!=next){columns=next;reflow();}}
            super.onMeasure(widthSpec,heightSpec);
        }
    }
    private WmPaint buildPage(int group,String language){
        WmPaint surface=createSurface(getContext());
        SettingsGrid rows=new SettingsGrid(group,language);grids[group]=rows;
        WmScrollView scroll=new WmScrollView(getContext(),rows,LayoutParams.WRAP_CONTENT,face);surface.addView(scroll,new FrameLayout.LayoutParams(-1,-1));return surface;
    }
}
