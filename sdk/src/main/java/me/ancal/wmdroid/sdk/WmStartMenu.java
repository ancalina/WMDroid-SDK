package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.BaseAdapter;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/** Shared Start rows and folder navigation in logical VGA coordinates. */
public final class WmStartMenu extends FrameLayout implements WmControls.BackHandler {
    private List<WmShell.Entry> entries=new ArrayList<>();private Typeface font;private int accent,textColor=Color.BLACK,maxHeight=Integer.MAX_VALUE,width=188;
    public interface Select {void accept(WmShell.Entry entry);}
    private WmIconPolicy iconPolicy=new WmIconPolicy(false,"all","[]");
    private final Select selected;private final ListView rows;private final MenuAdapter adapter=new MenuAdapter();
    private final List<Integer> displayed=new ArrayList<>();
    private final java.util.Map<List<String>,Drawable.ConstantState> icons=new java.util.HashMap<>();
    private int generation,iconGeneration,contentHeight;private String iconResources="";private boolean preserveHeldState;
    private final List<String> parents=new ArrayList<>();private String parent="",backLabel="뒤로";
    public WmStartMenu(Context context,List<WmShell.Entry> entries,Typeface font,int accent,Drawable background,Select selected){
        super(context);this.selected=selected;setPadding(2,2,2,2);setClickable(true);
        rows=new ListView(context){
            @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
                if(!preserveHeldState){super.onLayout(changed,left,top,right,bottom);return;}
                // Native row setup normalizes held flags even for caption-only layout.
                List<MenuRow> retained=new ArrayList<>();List<Integer> indices=new ArrayList<>();List<boolean[]> held=new ArrayList<>();
                for(int i=0;i<getChildCount();i++)if(getChildAt(i) instanceof MenuRow){MenuRow cell=(MenuRow)getChildAt(i);retained.add(cell);indices.add(cell.index);held.add(new boolean[]{cell.isPressed(),cell.isSelected()});}
                super.onLayout(changed,left,top,right,bottom);preserveHeldState=false;
                for(int i=0;i<retained.size();i++){MenuRow cell=retained.get(i);if(cell.getParent()==this&&cell.boundGeneration==generation&&cell.index==indices.get(i)){cell.setPressed(held.get(i)[0]);cell.setSelected(held.get(i)[1]);}}
            }
        };rows.setPadding(0,0,0,0);rows.setDivider(null);rows.setDividerHeight(0);rows.setSelector(new ColorDrawable(Color.TRANSPARENT));
        rows.setBackgroundColor(Color.TRANSPARENT);rows.setCacheColorHint(Color.TRANSPARENT);rows.setVerticalFadingEdgeEnabled(false);rows.setVerticalScrollBarEnabled(false);rows.setHorizontalScrollBarEnabled(false);rows.setOverScrollMode(View.OVER_SCROLL_NEVER);rows.setSoundEffectsEnabled(false);
        rows.setAdapter(adapter);rows.setOnItemClickListener((list,view,position,id)->{if(view instanceof MenuRow)dispatch((MenuRow)view);});
        WmPaint paint=new WmPaint(context,false,WmPaint.Role.MENU);paint.addView(rows,new FrameLayout.LayoutParams(-1,-1));addView(paint,new FrameLayout.LayoutParams(-1,-1));refresh(entries,font,accent,background);
    }
    public void refresh(List<WmShell.Entry> entries,Typeface font,int accent,Drawable background){
        boolean changed=!sameEntries(entries,false),captionsChanged=!sameEntries(entries,true),appearanceChanged=this.font!=font||this.accent!=accent;
        this.entries=entries==null?new ArrayList<>():new ArrayList<>(entries);this.font=font;this.accent=accent;
        String resources=WmResources.revision(getContext());boolean resourcesChanged=!resources.equals(iconResources);
        if(resourcesChanged){iconResources=resources;icons.clear();iconGeneration++;}
        GradientDrawable border=new GradientDrawable();border.setColor(Color.TRANSPARENT);border.setStroke(2,Color.BLACK);
        if(background==null){background=WmShell.startBackground(getContext());setTextColor(background instanceof ColorDrawable?Color.BLACK:WmShell.state(getContext()).startTextColor);}
        setBackgroundDrawable(new LayerDrawable(new Drawable[]{background,border}));
        if(!parent.isEmpty()&&!hasFolder(parent)){parent="";parents.clear();changed=true;}if(changed||displayed.isEmpty())render();else if(captionsChanged||appearanceChanged||resourcesChanged)restyleRows();
    }
    private boolean sameEntries(List<WmShell.Entry> next,boolean compareTitles){
        if(next==null)return entries.isEmpty();if(entries.size()!=next.size())return false;
        for(int i=0;i<entries.size();i++){WmShell.Entry a=entries.get(i),b=next.get(i);
            if(a.folder!=b.folder||!TextUtils.equals(a.id,b.id)||!TextUtils.equals(a.parent,b.parent)||(compareTitles&&!TextUtils.equals(a.title,b.title))||!TextUtils.equals(a.icon,b.icon)||!TextUtils.equals(a.component,b.component)||!TextUtils.equals(a.command,b.command))return false;
        }return true;
    }
    public void setTextColor(int color){
        if(textColor==color)return;textColor=color;
        // Appearance setters must not rebuild rows and decode every icon again.
        for(int i=0;i<rows.getChildCount();i++){
            ViewGroup cell=(ViewGroup)rows.getChildAt(i);
            for(int j=0;j<cell.getChildCount();j++)if(cell.getChildAt(j) instanceof TextView)((TextView)cell.getChildAt(j)).setTextColor(captionColors());
        }
    }
    public void setBackLabel(String value){String next=value==null?"":value;if(next.equals(backLabel))return;backLabel=next;if(!parent.isEmpty())restyleRows();}
    public void setMaxHeight(int logicalPixels){int next=Math.max(0,logicalPixels);if(next!=maxHeight){maxHeight=next;requestLayout();}}
    public void setIconsUnframed(boolean value){setIconPolicy(new WmIconPolicy(value,"all","[]"));}
    public void setIconPolicy(WmIconPolicy value){
        WmIconPolicy next=value==null?new WmIconPolicy(false,"all","[]"):value;
        if(iconPolicy.enabled==next.enabled&&iconPolicy.mode.equals(next.mode)&&iconPolicy.json.equals(next.json))return;
        WmIconPolicy previous=iconPolicy;iconPolicy=next;icons.clear();iconGeneration++;
        for(int i=0;i<rows.getChildCount();i++)if(rows.getChildAt(i) instanceof MenuRow){
            MenuRow cell=(MenuRow)rows.getChildAt(i);if(cell.boundGeneration!=generation||cell.index<0)continue;WmShell.Entry entry=entries.get(cell.index);
            if(previous.allows(entry.component)!=next.allows(entry.component))cell.image.setImageDrawable(rowIcon(entry));
            cell.iconVersion=iconGeneration;
        }
    }
    public int menuWidth(){return width;}
    /** Returns true when Back navigated to the preceding folder. */
    @Override public boolean back(){if(parent.isEmpty())return false;do{parent=parents.isEmpty()?"":parents.remove(parents.size()-1);}while(!parent.isEmpty()&&!hasFolder(parent));render();return true;}
    public void dismissFolders(){boolean changed=!parent.isEmpty();parent="";parents.clear();if(changed)render();rows.setSelection(0);clearFocus();for(int i=0;i<rows.getChildCount();i++){rows.getChildAt(i).setPressed(false);rows.getChildAt(i).setSelected(false);}}
    private boolean hasFolder(String id){for(WmShell.Entry entry:entries)if(entry.folder&&id.equals(entry.id))return true;return false;}
    private void render(){
        preserveHeldState=false;generation++;iconGeneration++;icons.clear();displayed.clear();contentHeight=4;
        if(!parent.isEmpty()){displayed.add(-1);contentHeight+=36;}
        for(int i=0;i<entries.size();i++){
            WmShell.Entry entry=entries.get(i);if(!parent.equals(entry.parent==null?"":entry.parent))continue;
            if(parent.isEmpty()&&"programs".equals(entry.command)){displayed.add(-2);contentHeight+=10;}
            displayed.add(i);contentHeight+=36;
        }
        // Reset old viewport sync before a folder/topology width change can remember it.
        resizeToCaptions();rows.setAdapter(adapter);rows.setSelection(0);requestLayout();
    }
    private void selectEntry(WmShell.Entry entry){
        if(entry.folder){if(entry.id==null||entry.id.isEmpty()||entry.id.equals(parent)||parents.contains(entry.id))return;parents.add(parent);parent=entry.id;render();}
        else if(selected!=null)selected.accept(entry);
    }
    private final class MenuRow extends FrameLayout {
        final ImageView image;final TextView caption;int index=-2,boundGeneration=-1,iconVersion=-1;
        MenuRow(){
            super(WmStartMenu.this.getContext());setSoundEffectsEnabled(false);
            image=new ImageView(getContext());image.setScaleType(ImageView.ScaleType.FIT_CENTER);FrameLayout.LayoutParams art=new FrameLayout.LayoutParams(32,32);art.leftMargin=8;art.topMargin=2;addView(image,art);
            caption=WmControls.text(getContext(),font,"");caption.setSingleLine(true);caption.setEllipsize(TextUtils.TruncateAt.END);caption.setDuplicateParentStateEnabled(true);caption.setGravity(Gravity.CENTER_VERTICAL);
            FrameLayout.LayoutParams text=new FrameLayout.LayoutParams(-1,32);text.leftMargin=48;text.topMargin=2;text.rightMargin=16;addView(caption,text);
            setOnClickListener(view->dispatch(this));
        }
    }
    private void dispatch(MenuRow row){
        if(row.getParent()!=rows||row.boundGeneration!=generation)return;
        if(row.index==-1){back();return;}if(row.index>=0&&row.index<entries.size())selectEntry(entries.get(row.index));
    }
    private final class MenuAdapter extends BaseAdapter {
        public int getCount(){return displayed.size();}
        public Object getItem(int position){return displayed.get(position);}
        public long getItemId(int position){return position;}
        public int getViewTypeCount(){return 2;}
        public int getItemViewType(int position){return displayed.get(position)==-2?1:0;}
        public boolean areAllItemsEnabled(){return false;}
        public boolean isEnabled(int position){return displayed.get(position)!=-2;}
        public View getView(int position,View recycled,ViewGroup owner){
            int index=displayed.get(position);
            if(index==-2){
                FrameLayout gap=recycled instanceof FrameLayout?(FrameLayout)recycled:new FrameLayout(getContext());
                if(gap.getChildCount()==0){View line=new View(getContext());line.setTag("start-separator");FrameLayout.LayoutParams border=new FrameLayout.LayoutParams(-1,2);border.topMargin=4;gap.addView(line,border);}
                gap.getChildAt(0).setBackgroundColor(accent);gap.setTag(-2);gap.setLayoutParams(new AbsListView.LayoutParams(-1,10));return gap;
            }
            MenuRow cell=recycled instanceof MenuRow?(MenuRow)recycled:new MenuRow();
            boolean same=cell.boundGeneration==generation&&cell.index==index;
            if(!same){cell.setPressed(false);cell.setSelected(false);cell.clearFocus();}
            bind(cell,index,!same||cell.iconVersion!=iconGeneration);cell.setLayoutParams(new AbsListView.LayoutParams(-1,36));return cell;
        }
    }
    private String caption(int index){return index==-1?"‹ "+backLabel:entries.get(index).title==null?"":entries.get(index).title;}
    private void bind(MenuRow cell,int index,boolean artwork){
        cell.index=index;cell.boundGeneration=generation;cell.setTag(index);
        String title=caption(index);if(!TextUtils.equals(cell.caption.getText(),title))cell.caption.setText(title);
        WmControls.textStyle(cell.caption,font);cell.caption.setTextColor(captionColors());cell.setBackgroundDrawable(WmControls.background(Color.TRANSPARENT,selection()));
        cell.image.setVisibility(index<0?View.GONE:View.VISIBLE);
        if(index>=0){WmShell.Entry entry=entries.get(index);cell.image.setTag(entry);if(artwork)cell.image.setImageDrawable(rowIcon(entry));}
        else if(artwork)cell.image.setImageDrawable(null);
        cell.iconVersion=iconGeneration;
    }
    private Drawable rowIcon(WmShell.Entry entry){
        // A topology/navigation generation owns its decoded pixels; row Drawables keep independent state.
        List<String> key=java.util.Arrays.asList(entry.icon,entry.component,entry.command,Boolean.toString(entry.folder));
        if(icons.containsKey(key)){Drawable.ConstantState state=icons.get(key);return state==null?null:state.newDrawable(getResources()).mutate();}
        Drawable image=icon(entry);Drawable.ConstantState state=image==null?null:image.getConstantState();
        if(image==null||state!=null)icons.put(key,state);return image==null?null:image.mutate();
    }
    private void restyleRows(){
        preserveHeldState=true;
        for(int i=0;i<rows.getChildCount();i++){
            View child=rows.getChildAt(i);
            if(child instanceof MenuRow){MenuRow cell=(MenuRow)child;if(cell.boundGeneration==generation)bind(cell,cell.index,cell.iconVersion!=iconGeneration);}
            else if(child instanceof ViewGroup&&((ViewGroup)child).getChildCount()>0)((ViewGroup)child).getChildAt(0).setBackgroundColor(accent);
        }
        resizeToCaptions();
    }
    private void resizeToCaptions(){
        width=188;TextView measuring=WmControls.text(getContext(),font,"");
        for(int index:displayed)if(index!=-2&&width<342)width=Math.max(width,(int)Math.ceil(WmFonts.measure(measuring.getPaint(),caption(index)))+68);
        width=Math.min(342,width);ViewGroup.LayoutParams params=getLayoutParams();if(params!=null&&params.width>=0&&params.width!=width){params.width=width;setLayoutParams(params);}requestLayout();
    }
    /** Standalone fallback. Launcher shells share the ROM Start texture. */
    public static Drawable windowBackground(int accent){
        return new ColorDrawable(Color.WHITE);
    }
    private int selection(){return selection(accent);}
    private ColorStateList captionColors(){return new ColorStateList(new int[][]{{android.R.attr.state_pressed},{android.R.attr.state_selected},{android.R.attr.state_focused},{}},new int[]{Color.WHITE,Color.WHITE,Color.WHITE,textColor});}
    private static int selection(int accent){if(WmColor.isGray(accent))return WmColor.gray(0xff149ca7);float[] hsv=new float[3],theme=new float[3];Color.colorToHSV(0xff149ca7,hsv);Color.colorToHSV(accent,theme);hsv[0]=theme[0];return Color.HSVToColor(hsv);}
    private Drawable icon(WmShell.Entry entry){
        String value=entry.icon;
        if(value==null||value.isEmpty())return entry.folder?new BitmapDrawable(getResources(),WmResources.emojiBitmap("📁",64,64)):null;
        try{Uri uri=Uri.parse(value);String scheme=uri.getScheme();
            if("android.resource".equals(scheme)){
                android.content.res.Resources resources=getContext().getPackageName().equals(uri.getAuthority())?getResources():getContext().getPackageManager().getResourcesForApplication(uri.getAuthority());java.util.List<String> path=uri.getPathSegments();
                int id=path.size()==1?Integer.parseInt(path.get(0)):path.size()==2?resources.getIdentifier(path.get(1),path.get(0),uri.getAuthority()):0;Drawable source=id==0?null:resources.getDrawable(id);
                if(source!=null&&entry.component.isEmpty()&&"me.ancal.wmdroid".equals(uri.getAuthority())){
                    // Built-in resource IDs now identify transparent geometry placeholders.
                    // Use the same optional pack paths as the launcher, not placeholder pixels.
                    String name=resources.getResourceEntryName(id);
                    if("wm_phone_hd".equals(name))return WmIcons.load(getContext(),"phone");
                    String asset;switch(name){
                        case "wm_apps":asset="launcher/icons/apps.png";break;
                        case "wm_battery":asset="launcher/icons/battery.png";break;
                        case "wm_battery_charge":asset="status/battery/charging.png";break;
                        case "wm_battery_full":asset="status/battery/full.png";break;
                        case "wm_battery_low":asset="status/battery/low.png";break;
                        case "wm_battery_medium":asset="status/battery/medium.png";break;
                        case "wm_bluetooth_status":asset="status/bluetooth.png";break;
                        case "wm_browser":asset="launcher/icons/browser.png";break;
                        case "wm_calendar":asset="launcher/icons/calendar.png";break;
                        case "wm_clock":asset="launcher/icons/clock.png";break;
                        case "wm_contacts":asset="launcher/icons/contacts.png";break;
                        case "wm_cpl_about":asset="settings/about.png";break;
                        case "wm_cpl_backlight":asset="settings/backlight.png";break;
                        case "wm_cpl_bluetooth":asset="settings/bluetooth.png";break;
                        case "wm_cpl_connections":asset="settings/connections.png";break;
                        case "wm_cpl_language":asset="settings/language.png";break;
                        case "wm_cpl_lock":asset="settings/lock.png";break;
                        case "wm_cpl_menu":asset="settings/menu.png";break;
                        case "wm_cpl_performance":asset="settings/performance.png";break;
                        case "wm_cpl_power":asset="settings/power.png";break;
                        case "wm_cpl_sounds":asset="settings/sounds.png";break;
                        case "wm_cpl_today":asset="settings/today.png";break;
                        case "wm_cpl_wifi":asset="settings/wifi.png";break;
                        case "wm_cpl_wireless":asset="settings/wireless.png";break;
                        case "wm_help":asset="launcher/icons/help.png";break;
                        case "wm_lock":asset="library/today/lock.png";break;
                        case "wm_mail":asset="launcher/icons/mail.png";break;
                        case "wm_media":asset="launcher/icons/media.png";break;
                        case "wm_notes":asset="launcher/icons/notes.png";break;
                        case "wm_ok":asset="titlebar/ok.png";break;
                        case "wm_owner":asset="launcher/icons/owner.png";break;
                        case "wm_phone":asset="launcher/icons/phone.png";break;
                        case "wm_settings_app_icons":asset="settings/app-icons.png";break;
                        case "wm_settings_fonts":asset="settings/fonts.png";break;
                        case "wm_settings_mouse":asset="settings/mouse.png";break;
                        case "wm_settings_theme":asset="settings/theme.png";break;
                        case "wm_signal":asset="status/cellular-signal.png";break;
                        case "wm_start":return WmIcons.load(getContext(),"start");
                        case "wm_start_programs":asset="launcher/icons/start-programs.png";break;
                        case "wm_start_settings":asset="launcher/icons/start-settings.png";break;
                        case "wm_start_today":asset="launcher/icons/start-today.png";break;
                        case "wm_tasks":asset="launcher/icons/tasks.png";break;
                        case "wm_volume":asset="status/volume.png";break;
                        case "wm_wireless":asset="library/today/wireless.png";break;
                        default:asset=null;
                    }
                    if(asset!=null){
                        String symbol;switch(entry.command){
                            case "today":symbol="📅";break;case "calendar":symbol="📅";break;case "contacts":symbol="👥";break;
                            case "notes":symbol="📝";break;case "messages":symbol="✉️";break;case "browser":symbol="🌐";break;
                            case "media":symbol="🎵";break;case "programs":symbol="📁";break;case "settings":symbol="⚙️";break;
                            default:symbol="❓";
                        }
                        Bitmap bitmap=WmResources.iconBitmap(getContext(),asset,64,64,symbol);
                        BitmapDrawable image=new BitmapDrawable(getResources(),bitmap);image.setFilterBitmap(true);return image;
                    }
                }
                if(source!=null&&iconPolicy.allows(entry.component)){
                    int size=Math.max(32,Math.round(32*getResources().getDisplayMetrics().widthPixels/480f));
                    Bitmap rendered=WmAppIcons.render(source,size,true);return rendered==null?source:new BitmapDrawable(getResources(),rendered);
                }
                return source;
            }
            if("content".equals(scheme)&&!"me.ancal.wmdroid.shell".equals(uri.getAuthority()))return null;
            if(!"content".equals(scheme))return null;
            try(InputStream input=getContext().getContentResolver().openInputStream(uri)){if(input==null)return null;Bitmap bitmap=BitmapFactory.decodeStream(input);if(bitmap==null)return null;BitmapDrawable drawable=new BitmapDrawable(getResources(),bitmap);drawable.setFilterBitmap(true);return drawable;}
        }catch(Exception absent){return entry.folder?new BitmapDrawable(getResources(),WmResources.emojiBitmap("📁",64,64)):null;}
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int available=MeasureSpec.getSize(heightSpec);int height=Math.min(contentHeight,MeasureSpec.getMode(heightSpec)==MeasureSpec.UNSPECIFIED?maxHeight:Math.min(maxHeight,available));
        int widthMode=MeasureSpec.getMode(widthSpec);int requested=widthMode==MeasureSpec.UNSPECIFIED?width:Math.min(width,MeasureSpec.getSize(widthSpec));
        super.onMeasure(MeasureSpec.makeMeasureSpec(requested,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY));
    }
}
