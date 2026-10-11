package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Looper;
import android.text.StaticLayout;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared Today presentation for local modules and provider-backed rows.
 * Data, permissions and actions belong to the module's host, not this layout.
 */
public final class WmToday {
    private WmToday() {}

    /** BETWEEN separates visible modules, never drawing below the final one.
     * WHEN_FOLLOWED retains its existing equivalent behavior for older clients.
     */
    public enum Divider { ALWAYS, WHEN_FOLLOWED, BETWEEN }

    /** Optional text overrides; defaults are exactly the built-in Today row contract. */
    public static final class TextStyle {
        public static final TextStyle STANDARD=new TextStyle(WmMetrics.TODAY_FONT,WmMetrics.INSET,
            WmMetrics.TEXT_LEFT,WmMetrics.ICON_INSET,Gravity.LEFT);
        // Today WM_SIZE replaces the initial +1 width; reserve the 26px VGA scrollbar gutter.
        public static final TextStyle NATIVE_LINES=new TextStyle(WmMetrics.TODAY_FONT,WmMetrics.INSET,
            WmMetrics.TEXT_LEFT,26,Gravity.LEFT);
        public final int size,inset,left,right,alignment;
        public TextStyle(int size,int inset,int left,int right,int alignment){
            if(size<=0||size>WmMetrics.WIDTH||inset<0||left<0||right<0||left+right>=WmMetrics.WIDTH
                ||(alignment!=Gravity.LEFT&&alignment!=Gravity.CENTER_HORIZONTAL&&alignment!=Gravity.RIGHT))
                throw new IllegalArgumentException("Invalid Today text style");
            this.size=size;this.inset=inset;this.left=left;this.right=right;this.alignment=alignment;
        }
        public int lineHeight(){return Math.max(1,Math.round(WmMetrics.TODAY_LINE*size/(float)WmMetrics.TODAY_FONT));}
        public int height(int lines){int line=lineHeight();return lines<=1?Math.max(WmMetrics.TODAY_MIN,line)
            :lines*line+(lines-1)*WmMetrics.GAP+2*inset;}
    }

    public static final class Style {
        public final int text, selectedText, selection, divider;
        public Style(int text, int selectedText, int selection, int divider) {
            this.text=text;this.selectedText=selectedText;
            this.selection=selection;this.divider=divider;
        }
        private StateListDrawable background() {
            StateListDrawable result=new StateListDrawable();
            result.addState(new int[]{-android.R.attr.state_enabled},new ColorDrawable(Color.TRANSPARENT));
            for(int state:new int[]{android.R.attr.state_pressed,android.R.attr.state_selected,android.R.attr.state_focused})
                result.addState(new int[]{state},new ColorDrawable(selection));
            result.addState(new int[]{},new ColorDrawable(Color.TRANSPARENT));
            result.setEnterFadeDuration(0);result.setExitFadeDuration(0);return result;
        }
    }

    /** The same descriptor is used for built-in custom content and external data. */
    public static final class Module {
        public final String id;
        public final View content;
        public final Divider divider;
        private final View line;
        private Module(String id, View content, Divider divider, View line) {
            this.id=id;this.content=content;this.divider=divider;this.line=line;
        }
    }

    /** Ordinary timed appointment detail: native Today removes literal spaces from times.
     * All-day entries have no detail line. Multi-day continuation labels belong to the host.
     */
    public static String appointmentDetails(long start,long end,boolean allDay,String location,java.text.DateFormat timeFormat) {
        if(timeFormat==null)throw new IllegalArgumentException("Appointment time format required");
        if(allDay)return "";
        String detail=timeFormat.format(new java.util.Date(start)).replace(" ","");
        if(!sameAppointmentMinute(start,end,timeFormat))detail+="-"+timeFormat.format(new java.util.Date(end)).replace(" ","");
        if(location!=null&&!location.isEmpty())detail+=" ("+location+")";
        return detail;
    }

    /** Paint-policy bits:2 all-day,4 crosses midnight,8 ongoing,32 starts today,64 ends today. */
    public static String appointmentDetails(long start,long end,int displayFlags,String location,java.text.DateFormat timeFormat,String continuation){
        if((displayFlags&4)==0)return appointmentDetails(start,end,(displayFlags&2)!=0,location,timeFormat);
        if(timeFormat==null||continuation==null||continuation.isEmpty())throw new IllegalArgumentException("Appointment format and continuation label required");
        if((displayFlags&2)!=0)return "";
        boolean labelFirst=(displayFlags&32)==0&&(displayFlags&8)!=0;
        String detail=labelFirst?continuation:timeFormat.format(new java.util.Date(start)).replace(" ","");
        if((displayFlags&64)==0){if(!labelFirst)detail+="-"+continuation;}
        else if(!sameAppointmentMinute(start,end,timeFormat))detail+="-"+timeFormat.format(new java.util.Date(end)).replace(" ","");
        if(location!=null&&!location.isEmpty())detail+=" ("+location+")";
        return detail;
    }

    /** Already-selected occurrence; selection and continuation labels belong to the host. */
    public static final class Appointment {
        public final String subject,detail;
        public Appointment(String subject,String detail){
            this.subject=subject==null?"":subject.replace('\n',' ');
            this.detail=detail==null?"":detail.replace('\n',' ');
        }
    }

    /** Civil-day metadata supplied by the calendar adapter; day0 means today. */
    public static final class CalendarEntry {
        public final long start,end;
        public final boolean allDay;
        public final int dayOffset,minuteOfDay;
        public final Appointment appointment;
        public CalendarEntry(long start,long end,boolean allDay,int dayOffset,int minuteOfDay,Appointment appointment){
            if(end<start||dayOffset<0||minuteOfDay<0||minuteOfDay>=1440||appointment==null)
                throw new IllegalArgumentException("Invalid Today calendar entry");
            this.start=start;this.end=end;this.allDay=allDay;this.dayOffset=dayOffset;
            this.minuteOfDay=minuteOfDay;this.appointment=appointment;
        }
    }

    /** Original flags:1 show all-day,2 next future minute group; bit4 is internal. */
    public static List<CalendarEntry> selectAppointments(List<CalendarEntry> entries,long now,int flags){
        if(entries==null)throw new IllegalArgumentException("Calendar entries required");
        List<CalendarEntry> sorted=new ArrayList<>();int nearest=Integer.MAX_VALUE;
        for(CalendarEntry entry:entries){
            if(entry==null)throw new IllegalArgumentException("Null calendar entry");
            if(entry.allDay){if((flags&1)!=0&&entry.dayOffset==0)sorted.add(entry);}
            else if(entry.end>=now){sorted.add(entry);if(entry.dayOffset>0)nearest=Math.min(nearest,entry.dayOffset);}
        }
        Collections.sort(sorted,new java.util.Comparator<CalendarEntry>(){
            public int compare(CalendarEntry a,CalendarEntry b){
                if(a.allDay!=b.allDay)return a.allDay?-1:1;
                return a.start<b.start?-1:a.start>b.start?1:0;
            }
        });
        List<CalendarEntry> visible=new ArrayList<>();CalendarEntry firstFuture=null;
        for(CalendarEntry entry:sorted){
            if(!entry.allDay){
                if(entry.dayOffset>0&&entry.dayOffset!=nearest)continue;
                if(entry.start>now){
                    if(firstFuture!=null){
                        if((flags&2)!=0&&(entry.dayOffset!=firstFuture.dayOffset||entry.minuteOfDay!=firstFuture.minuteOfDay))continue;
                        if((flags&2)==0&&(flags&4)==0&&entry.dayOffset>firstFuture.dayOffset)continue;
                    }else firstFuture=entry;
                }
            }
            visible.add(entry);
        }
        return visible;
    }

    private static boolean sameAppointmentMinute(long start,long end,java.text.DateFormat format){
        java.util.Calendar template=format.getCalendar();
        // shortcut: calendar-less custom formatters keep epoch-minute comparison; supply a Calendar for civil-time rules.
        if(template==null)return appointmentMinute(start)==appointmentMinute(end);
        java.util.Calendar first=(java.util.Calendar)template.clone(),last=(java.util.Calendar)template.clone();
        first.setTimeInMillis(start);last.setTimeInMillis(end);
        for(int field:new int[]{java.util.Calendar.ERA,java.util.Calendar.YEAR,java.util.Calendar.MONTH,
                java.util.Calendar.DAY_OF_MONTH,java.util.Calendar.HOUR_OF_DAY,java.util.Calendar.MINUTE})
            if(first.get(field)!=last.get(field))return false;
        return true;
    }

    private static long appointmentMinute(long time){long minute=time/60000L;return time<0&&time%60000L!=0?minute-1:minute;}

    /** Owns registration, order, visibility and separators; retains its constructor Style.
     * To change background/divider Style, create a new Layout and reapply modules/order/hidden. */
    public static final class Layout extends LinearLayout {
        private final Style style;
        private final Map<String,Module> modules=new LinkedHashMap<>();
        private final Map<String,View[]> views=new LinkedHashMap<>();
        private final Map<String,View[]> readOnlyViews=Collections.unmodifiableMap(views);
        public Layout(Context context, Style style) {
            super(context);this.style=style;setOrientation(VERTICAL);
        }
        public Module register(String id, View content, int height, Divider divider) {
            if(id==null||id.isEmpty()||content==null||height<0||divider==null)
                throw new IllegalArgumentException("A Today module needs an ID, content, height and divider policy");
            content.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,height));
            content.setBackgroundDrawable(style.background());
            Module previous=modules.get(id);
            View line=previous!=null&&previous.content==content?previous.line:new View(getContext());line.setBackgroundColor(style.divider);
            line.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,WmMetrics.BORDER));
            Module module=new Module(id,content,divider,line);
            modules.put(id,module);views.put(id,new View[]{content,line});return module;
        }
        /** Live registration-order view for host settings and selection cleanup. */
        public Map<String,View[]> rows() { return readOnlyViews; }
        public void remove(String id) { modules.remove(id);views.remove(id); }
        public void removePrefix(String prefix) {
            for(String id:new ArrayList<>(modules.keySet()))if(id.startsWith(prefix)) {
                modules.remove(id);views.remove(id);
            }
        }
        /** Unknown/duplicate IDs are ignored; newly registered modules append. */
        public void arrange(List<String> preferredOrder, Set<String> hidden) {
            List<String> order=new ArrayList<>();
            for(String id:preferredOrder)if(modules.containsKey(id)&&!order.contains(id))order.add(id);
            for(String id:modules.keySet())if(!order.contains(id))order.add(id);
            String last=null;
            for(String id:order)if(!hidden.contains(id))last=id;
            List<View> desired=new ArrayList<>();
            for(String id:order) {
                Module module=modules.get(id);boolean visible=!hidden.contains(id);
                module.content.setVisibility(visible?VISIBLE:GONE);
                boolean followed=!id.equals(last);
                boolean separator=module.divider==Divider.ALWAYS
                    || ((module.divider==Divider.WHEN_FOLLOWED||module.divider==Divider.BETWEEN)&&followed);
                module.line.setVisibility(visible&&separator?VISIBLE:GONE);
                desired.add(module.content);desired.add(module.line);
            }
            for(int i=getChildCount()-1;i>=0;i--)if(!desired.contains(getChildAt(i)))removeViewAt(i);
            for(int i=0;i<desired.size();i++) {
                View child=desired.get(i);
                if(getChildAt(i)==child)continue;
                if(child.getParent()==this)removeView(child);
                addView(child,i);
            }
        }
    }

    /** ROM-derived icon, font, line box and immediate selection for all text rows. */
    public static final class Row extends FrameLayout {
        public final TextView caption;
        private final TextStyle typography;
        private final Typeface font;
        private LinearLayout appointments;
        public Row(Context context, ImageView icon, Typeface font, Style style, CharSequence text) {
            this(context,icon,font,style,text,TextStyle.STANDARD);
        }
        public Row(Context context, ImageView icon, Typeface font, Style style, CharSequence text,TextStyle typography) {
            super(context);setBackgroundDrawable(style.background());
            if(typography==null)throw new IllegalArgumentException("Today text style required");this.typography=typography;this.font=font;
            FrameLayout.LayoutParams picture=new FrameLayout.LayoutParams(WmMetrics.ICON,WmMetrics.ICON);
            picture.leftMargin=WmMetrics.ICON_INSET;picture.topMargin=WmMetrics.ICON_INSET;
            addView(icon,picture);
            caption=new TextView(context);caption.setTypeface(font);WmFonts.attach(caption,font);
            caption.setTextSize(TypedValue.COMPLEX_UNIT_PX,typography.size);caption.setIncludeFontPadding(false);
            caption.setLineSpacing(0,1);caption.setGravity(Gravity.CENTER_VERTICAL);
            if(Build.VERSION.SDK_INT>=21)caption.setFontFeatureSettings("kern");
            if(Build.VERSION.SDK_INT>=23)caption.setBreakStrategy(0);
            caption.setDuplicateParentStateEnabled(true);
            caption.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_pressed},{android.R.attr.state_selected},{}},
                new int[]{style.selectedText,style.selectedText,style.text}));
            caption.setText(text);
            FrameLayout.LayoutParams position=new FrameLayout.LayoutParams(WmMetrics.WIDTH-WmMetrics.TEXT_LEFT,WmMetrics.TODAY_MIN);
            position.leftMargin=WmMetrics.TEXT_LEFT;addView(caption,position);
            setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,WmMetrics.TODAY_MIN));
            setCaption(text);
        }
        /** Measures the styled text, including hybrid-font spans, before fitting. */
        public void setCaption(CharSequence text) { fitCaption(text,true); }
        /** Preserve explicit lines and clip overflow, without wrapping or adding ellipsis. */
        public void setCaptionLines(CharSequence text) { fitCaption(text,false); }
        /** Native title/detail blocks: no inner gap, half-line gap between appointments. */
        public void setAppointments(List<Appointment> entries) {
            if(entries==null||entries.isEmpty())throw new IllegalArgumentException("Appointments required");
            for(Appointment entry:entries)if(entry==null)throw new IllegalArgumentException("Null appointment");
            clearAppointments();appointments=new LinearLayout(getContext());appointments.setOrientation(LinearLayout.VERTICAL);
            appointments.setDuplicateParentStateEnabled(true);
            int width=WmMetrics.WIDTH-typography.left-typography.right,line=typography.lineHeight();
            int height=2*typography.inset;StringBuilder full=new StringBuilder();
            for(Appointment entry:entries){
                if(full.length()>0)full.append('\n');full.append(entry.subject);
                TextView subject=appointmentText(entry.subject);
                StaticLayout measured=new StaticLayout(subject.getText(),subject.getPaint(),width,
                    android.text.Layout.Alignment.ALIGN_NORMAL,1,0,false);
                int subjectHeight=Math.max(1,measured.getLineCount())*line;
                subject.setLineSpacing(line-subject.getLineHeight(),1);
                LinearLayout.LayoutParams position=new LinearLayout.LayoutParams(width,subjectHeight);
                if(appointments.getChildCount()>0){position.topMargin=line/2;height+=line/2;}
                appointments.addView(subject,position);height+=subjectHeight;
                if(!entry.detail.isEmpty()){
                    full.append('\n').append(entry.detail);TextView detail=appointmentText(entry.detail);
                    CharSequence styled=detail.getText();float available=width-detail.getPaint().measureText("...");
                    int low=0,high=styled.length();
                    while(low<high){int mid=(low+high+1)/2;
                        if(android.text.Layout.getDesiredWidth(styled.subSequence(0,mid),detail.getPaint())<=available)low=mid;else high=mid-1;}
                    if(low<styled.length()){
                        if(low>0&&Character.isHighSurrogate(styled.charAt(low-1)))low--;
                        detail.setText(entry.detail.substring(0,low)+"...");
                    }
                    detail.setHorizontallyScrolling(true);detail.setGravity(Gravity.BOTTOM|typography.alignment);
                    detail.setContentDescription(entry.detail);
                    appointments.addView(detail,new LinearLayout.LayoutParams(width,line));height+=line;
                }
            }
            caption.setText(full);caption.setVisibility(GONE);setContentDescription(full);
            FrameLayout.LayoutParams position=new FrameLayout.LayoutParams(width,height-2*typography.inset);
            position.leftMargin=typography.left;position.topMargin=typography.inset;addView(appointments,position);
            setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,Math.max(WmMetrics.TODAY_MIN,height)));
        }
        private TextView appointmentText(String text){
            TextView view=new TextView(getContext());view.setTypeface(font);WmFonts.attach(view,font);
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX,typography.size);view.setIncludeFontPadding(false);
            view.setTextColor(caption.getTextColors());view.setDuplicateParentStateEnabled(true);
            view.setGravity(Gravity.TOP|typography.alignment);
            if(Build.VERSION.SDK_INT>=21)view.setFontFeatureSettings("kern");
            if(Build.VERSION.SDK_INT>=23)view.setBreakStrategy(0);
            view.setText(text);return view;
        }
        private void clearAppointments(){
            if(appointments!=null){removeView(appointments);appointments=null;}
            caption.setVisibility(VISIBLE);setContentDescription(null);
        }
        private void fitCaption(CharSequence text,boolean wrap) {
            clearAppointments();
            caption.setHorizontallyScrolling(!wrap);
            caption.setText(text);caption.setLineSpacing(0,1);
            int width=WmMetrics.WIDTH-typography.left-typography.right;
            int lines=1;
            if(wrap){
                StaticLayout layout=new StaticLayout(caption.getText(),caption.getPaint(),width,
                    android.text.Layout.Alignment.ALIGN_NORMAL,1,0,false);
                lines=layout.getLineCount();
            }else for(int i=0;i<caption.length();i++)if(caption.getText().charAt(i)=='\n')lines++;
            int height=typography.height(lines);
            caption.setLineSpacing(typography.lineHeight()+WmMetrics.GAP-caption.getLineHeight(),1);
            caption.setGravity((lines>1?Gravity.TOP:Gravity.CENTER_VERTICAL)|typography.alignment);
            caption.setPadding(0,lines>1?typography.inset:0,typography.right,0);
            FrameLayout.LayoutParams position=new FrameLayout.LayoutParams(WmMetrics.WIDTH-typography.left,height);
            position.leftMargin=typography.left;caption.setLayoutParams(position);
            setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,height));
        }
    }

    /** Draw the actual SDK row for a standard RemoteViews ImageView. UI thread only.
     * The row must be unattached. Height is logical VGA pixels; callers own the bitmap.
     * No divider is drawn: the host's Layout owns between-row separators.
     */
    public static Bitmap rasterize(Row row,int pixelWidth,int height){
        if(Looper.myLooper()!=Looper.getMainLooper())throw new IllegalStateException("Today rows render on the UI thread");
        if(row==null||row.getParent()!=null||pixelWidth<=0||pixelWidth>1440||height<TodayContract.MIN_HEIGHT||height>TodayContract.MAX_HEIGHT)
            throw new IllegalArgumentException("Invalid unattached Today raster surface");
        row.measure(View.MeasureSpec.makeMeasureSpec(WmMetrics.WIDTH,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
        row.layout(0,0,WmMetrics.WIDTH,height);
        Bitmap result=Bitmap.createBitmap(pixelWidth,Math.max(1,Math.round(height*pixelWidth/(float)WmMetrics.WIDTH)),Bitmap.Config.ARGB_8888);
        result.setDensity(Bitmap.DENSITY_NONE);Canvas canvas=new Canvas(result);float scale=pixelWidth/(float)WmMetrics.WIDTH;canvas.scale(scale,scale);row.draw(canvas);return result;
    }
}
