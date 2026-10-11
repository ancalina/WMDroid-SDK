package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

/** Retained-page wizard following PIMDLG dialog20020's cancel/back/next/finish
 * arrangement. Geometry projects the observed SDK emulator base units7/14 at
 * logical192DPI; custom fonts/OEM profiles remain adaptations.
 * Callers own page validation and completion. No shell footer or provider work. */
public final class WmWizard extends WmDialog implements WmControls.BackHandler,WmControls.RestyleListener {
    public interface OnPageChangedListener {void selected(int index);}
    public interface NavigationGate {boolean allow(int from,int to);}
    private final FrameLayout body,forward,buttons;
    private boolean korean;
    private final ArrayList<View> pages=new ArrayList<View>();
    private final TextView cancel,back,next,finish,error;
    private int selected=-1;
    private boolean finishHere,notifying;
    private OnPageChangedListener pageChanged;
    private NavigationGate gate;
    private Runnable cancelAction,finishAction;
    public WmWizard(Context context,Typeface font,Locale locale,int accent){
        super(context);if(font==null||locale==null)throw new IllegalArgumentException("Wizard arguments required");
        setOrientation(VERTICAL);setBackgroundColor(Color.WHITE);
        // Visible across stages; native modal validation presentation is pending.
        error=WmControls.text(context,font,"");error.setTextColor(0xff800000);error.setPadding(8,4,8,4);error.setVisibility(GONE);addView(error,new LayoutParams(-1,-2));
        body=new FrameLayout(context);addView(body,new LayoutParams(-1,324));
        buttons=new FrameLayout(context);
        cancel=button(font,accent);back=button(font,accent);next=button(font,accent);finish=button(font,accent);
        buttons.addView(cancel,new FrameLayout.LayoutParams(112,42));buttons.addView(back,new FrameLayout.LayoutParams(112,42));
        forward=new FrameLayout(context);forward.addView(next,new FrameLayout.LayoutParams(-1,-1));forward.addView(finish,new FrameLayout.LayoutParams(-1,-1));buttons.addView(forward,new FrameLayout.LayoutParams(112,42));
        addView(buttons,new LayoutParams(-1,42));setLocale(locale);
        cancel.setOnClickListener(v->{if(cancelAction!=null)cancelAction.run();});
        back.setOnClickListener(v->navigate(selected-1));next.setOnClickListener(v->navigate(selected+1));
        finish.setOnClickListener(v->{if(finishAction!=null)finishAction.run();});setCancelButton(cancel);refresh();
    }
    private TextView button(Typeface font,int accent){TextView v=WmControls.text(getContext(),font,"");v.setGravity(Gravity.CENTER);WmControls.button(v,accent);v.setFocusableInTouchMode(true);return v;}
    public int addPage(View page){
        if(page==null||page.getParent()!=null)throw new IllegalArgumentException("Unattached wizard page required");
        pages.add(page);body.addView(page,new FrameLayout.LayoutParams(-1,-1));if(selected<0)selected=0;refresh();return pages.size()-1;
    }
    public int pageCount(){return pages.size();}
    public int selectedPage(){return selected;}
    /** Bypasses gates. A real change clears finishHere, refreshes, then calls selected.
     * Selecting the current index is a no-op. */
    public void selectPage(int index){
        if(index<0||index>=pages.size())throw new IndexOutOfBoundsException("Invalid wizard page");
        if(index==selected)return;selected=index;finishHere=false;refresh();if(pageChanged!=null){boolean previous=notifying;notifying=true;try{pageChanged.selected(index);}finally{notifying=previous;}}
    }
    /** Stores direct WmScrollView page positions; other pages use -1. Does not traverse
     * nested scroll owners; callers save those, drafts, focus and popup state separately. */
    public Bundle saveInteractionState(){
        Bundle state=new Bundle();state.putInt("schema",1);state.putInt("count",pages.size());state.putInt("page",selected);state.putBoolean("finish_here",finishHere);state.putString("error",error.getText().toString());
        int[] scroll=new int[pages.size()];for(int i=0;i<scroll.length;i++)scroll[i]=pages.get(i) instanceof WmScrollView?((WmScrollView)pages.get(i)).getRequestedScrollPosition():-1;state.putIntArray("scroll",scroll);return state;
    }
    public void validateInteractionState(Bundle state){
        if(state==null||state.size()!=6)throw new IllegalArgumentException("Wizard interaction schema1 required");
        for(String key:new String[]{"schema","count","page"})if(!(state.get(key) instanceof Integer))throw new IllegalArgumentException("Wizard integer required");
        if(state.getInt("schema")!=1||state.getInt("count")!=pages.size()||!(state.get("finish_here") instanceof Boolean)||!(state.get("error") instanceof String)||!(state.get("scroll") instanceof int[]))throw new IllegalArgumentException("Invalid wizard interaction");
        int page=state.getInt("page");if(pages.isEmpty()?page!=-1:page<0||page>=pages.size())throw new IllegalArgumentException("Invalid wizard page");
        int[] scroll=state.getIntArray("scroll");if(scroll.length!=pages.size())throw new IllegalArgumentException("Wizard page scrolls required");for(int i=0;i<scroll.length;i++)if(pages.get(i) instanceof WmScrollView?scroll[i]<0:scroll[i]!=-1)throw new IllegalArgumentException("Invalid wizard scroll owner");
    }
    /** Silent: bypasses navigation validation and page callbacks; validates before mutation. */
    public void restoreInteractionState(Bundle state){
        if(notifying)throw new IllegalStateException("Cannot restore wizard during notification");validateInteractionState(state);
        selected=state.getInt("page");finishHere=state.getBoolean("finish_here");setError(state.getString("error"));refresh();int[] scroll=state.getIntArray("scroll");for(int i=0;i<scroll.length;i++)if(pages.get(i) instanceof WmScrollView)((WmScrollView)pages.get(i)).scrollToPosition(scroll[i]);
    }
    private boolean navigate(int index){
        if(index<0||index>=pages.size())return false;
        if(gate!=null&&!gate.allow(selected,index))return false;
        boolean advancing=index>selected;selectPage(index);
        // PIMDLG027E7BA4 posts WM_NEXTDLGCTL for these transitions only.
        if(advancing&&finish.getVisibility()==VISIBLE)(selected==pages.size()-1?finish:back).requestFocus();
        else if(!advancing&&selected==0)next.requestFocus();
        return true;
    }
    /** Early Finish for the current page (e.g. no-repeat pattern); actual selectPage
     * changes clear this flag, so set it again for subsequent eligible pages. */
    public void setFinishHere(boolean finishHere){this.finishHere=finishHere;refresh();}
    public void setNavigationGate(NavigationGate gate){this.gate=gate;}
    public void setOnPageChangedListener(OnPageChangedListener listener){pageChanged=listener;}
    public void setOnCancel(Runnable listener){cancelAction=listener;}
    public void setOnFinish(Runnable listener){finishAction=listener;}
    public TextView cancelButton(){return cancel;}
    public TextView errorField(){return error;}
    public void setError(CharSequence message){error.setText(message==null?"":message);error.setVisibility(error.length()==0?GONE:VISIBLE);}
    public TextView backButton(){return back;}
    public TextView nextButton(){return next;}
    public TextView finishButton(){return finish;}
    public void setLocale(Locale locale){
        if(locale==null)throw new IllegalArgumentException("Locale required");
        String language=WmStrings.language(getContext(),android.os.Build.VERSION.SDK_INT>=21?Modern.tag(locale):locale.toString().replace('_','-'));
        korean="ko".equals(language);requestLayout();
        TextView[] views={cancel,back,next,finish};String[] labels={"취소","뒤로","다음","마침"};
        for(int i=0;i<views.length;i++)WmControls.bindText(views[i],language,labels[i]);
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=View.MeasureSpec.getSize(widthSpec),available=View.MeasureSpec.getSize(heightSpec);
        int height=42;for(TextView button:new TextView[]{cancel,back,next,finish})height=Math.max(height,(int)Math.ceil(button.getPaint().getFontMetrics().bottom-button.getPaint().getFontMetrics().top)+button.getPaddingTop()+button.getPaddingBottom());
        int shift=width-480;int[] x={korean?118:76,238,356},w={korean?112:156,112,112};View[] controls={cancel,back,forward};
        if(width<404){int gap=6,cell=Math.max(1,(width-12-2*gap)/3);for(int i=0;i<3;i++){x[i]=6+i*(cell+gap);w[i]=cell;}shift=0;}
        for(int i=0;i<3;i++){FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)controls[i].getLayoutParams();int left=Math.max(0,x[i]+shift);if(lp.leftMargin!=left||lp.width!=w[i]||lp.height!=height){lp.leftMargin=left;lp.width=w[i];lp.height=height;controls[i].forceLayout();buttons.forceLayout();}}
        buttons.getLayoutParams().height=height;
        error.measure(widthSpec,View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));int errorHeight=error.getVisibility()==GONE?0:error.getMeasuredHeight();
        body.getLayoutParams().height=View.MeasureSpec.getMode(heightSpec)==View.MeasureSpec.UNSPECIFIED?324:Math.max(0,Math.min(324,available-height-errorHeight));
        super.onMeasure(widthSpec,heightSpec);
    }
    private static final class Modern {static String tag(Locale locale){return locale.toLanguageTag();}}
    private void refresh(){
        for(int i=0;i<pages.size();i++)pages.get(i).setVisibility(i==selected?VISIBLE:GONE);
        back.setEnabled(selected>0);boolean done=selected>=0&&(selected==pages.size()-1||finishHere);
        next.setVisibility(done?GONE:VISIBLE);next.setEnabled(selected>=0&&selected<pages.size()-1);
        finish.setVisibility(done?VISIBLE:GONE);finish.setEnabled(done);
        refreshDialogDefault();
    }
    @Override public boolean back(){
        if(selected>=0&&WmControls.back(pages.get(selected)))return true;
        return selected>0&&navigate(selected-1);
    }
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){}
}
