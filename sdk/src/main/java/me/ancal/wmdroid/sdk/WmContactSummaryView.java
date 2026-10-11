package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;

/** Captured Contacts Summary geometry, with owner-supplied display data.
 * The owner validates actions and performs calls/messages/opens. Native Notes is a
 * read-only tab; its separate editing and commit contract belongs to the owner.
 */
public final class WmContactSummaryView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener {
    public static final int CALL=0,SMS=1,ICON_NONE=-1,ICON_MOBILE=0,ICON_WORK=1,ICON_HOME=2,ICON_SMS=3;
    public static final int EMAIL=0,WEB=1;
    public static final class Phone {
        public final String key,label,number;
        public final int action,icon;
        public Phone(String key,String label,String number){this(key,label,number,CALL,ICON_WORK);}
        public Phone(String key,String label,String number,int action,int icon){
            if(key==null||key.isEmpty()||label==null||number==null)throw new IllegalArgumentException("Phone key, label and number required");
            if(action!=CALL&&action!=SMS||icon<ICON_NONE||icon>ICON_SMS)throw new IllegalArgumentException("Unknown contact phone action or icon");
            this.key=key;this.label=label;this.number=number;this.action=action;this.icon=icon;
        }
    }
    public static final class Link {
        public final String key,label,value;public final int kind;
        public Link(String key,String label,String value,int kind){
            if(key==null||key.isEmpty()||label==null||value==null)throw new IllegalArgumentException("Link key, label and value required");
            if(kind!=EMAIL&&kind!=WEB)throw new IllegalArgumentException("Unknown contact link kind");
            this.key=key;this.label=label;this.value=value;this.kind=kind;
        }
    }
    public static final class Address {
        public final String key,label,value;
        public Address(String key,String label,String value){
            if(key==null||key.isEmpty()||label==null||value==null)throw new IllegalArgumentException("Address key, label and value required");
            this.key=key;this.label=label;this.value=value;
        }
    }
    public interface Listener {void selected(Phone phone);void dial(Phone phone);default void message(Phone phone){}default void selectedLink(Link link){}default void openLink(Link link){}default void selectedAddress(Address address){}default void editAddress(Address address){}}
    public interface OnTabChangedListener {void changed(int tab);}
    private final Listener listener;
    private final FrameLayout client;
    private final LinearLayout summary,rows;
    private final NameHeader nameHeader;
    private final TextView notes,message,summaryTab,notesTab;
    private final WmTabStrip tabs;
    private final WmScrollView summaryScroll,notesScroll;
    private final ArrayList<ActionRow> rowViews=new ArrayList<>();
    private final ArrayList<AddressRow> addressViews=new ArrayList<>();
    private final Drawable[] icons=new Drawable[6];
    private OnTabChangedListener tabChanged;
    private Typeface regular,bold;
    private int selection,selectedTab;
    private String language="ko",name="",company="",jobTitle="",notesText="",authoredMessage;
    private Phone[] phones=new Phone[0];
    private Link[] links=new Link[0];
    private Address[] addresses=new Address[0];
    private Phone selected;
    private Link selectedLink;
    private Address selectedAddress;

    public WmContactSummaryView(Context context,Typeface regular,Typeface bold,int selection,Listener listener){
        super(context);if(regular==null||bold==null||listener==null)throw new IllegalArgumentException("Summary fonts and listener required");
        this.regular=regular;this.bold=bold;this.selection=selection;this.listener=listener;
        refreshArtwork();
        setOrientation(VERTICAL);setSoundEffectsEnabled(false);setBackgroundColor(Color.WHITE);
        client=new FrameLayout(context);addView(client,new LayoutParams(-1,0,1));
        summary=new LinearLayout(context);summary.setOrientation(VERTICAL);client.addView(summary,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout content=new LinearLayout(context);content.setOrientation(VERTICAL);nameHeader=new NameHeader(context);content.addView(nameHeader,new LayoutParams(-1,41));
        rows=new LinearLayout(context);rows.setOrientation(VERTICAL);content.addView(rows,new LayoutParams(-1,-2));summaryScroll=new WmScrollView(context,content);summary.addView(summaryScroll,new LayoutParams(-1,0,1));
        notes=WmControls.text(context,regular,"");notes.setGravity(Gravity.TOP|Gravity.LEFT);notes.setPadding(8,8,8,8);notes.setTextIsSelectable(false);notesScroll=new WmScrollView(context,notes);client.addView(notesScroll,new FrameLayout.LayoutParams(-1,-1));
        message=WmControls.text(context,regular,"");message.setGravity(Gravity.CENTER);message.setPadding(12,0,12,0);message.setBackgroundColor(Color.WHITE);client.addView(message,new FrameLayout.LayoutParams(-1,-1));
        tabs=new WmTabStrip(context,WmPalette.face(selection));summaryTab=tabs.addTab("요약",regular,()->setTab(0));notesTab=tabs.addTab("메모",regular,()->setTab(1));addView(tabs,new LayoutParams(-1,WmMetrics.TAB));
        onWmLanguageChanged(language);onWmRestyle(regular,bold,selection,WmPalette.face(selection));render();
    }
    private String artworkRevision;
    private void refreshArtwork(){
        if(artworkRevision==null)try{WmResources.prepare(getContext());}catch(java.io.IOException absent){}
        String revision=WmResources.revision(getContext());if(revision.equals(artworkRevision))return;
        String[] assets={"contacts/mobile.png","contacts/call.png","contacts/home.png","contacts/sms.png","contacts/email.png","contacts/web.png"};
        for(int i=0;i<icons.length;i++)icons[i]=loadIcon(getContext(),assets[i]);
        artworkRevision=revision;
    }
    private static Drawable loadIcon(Context context,String asset){
        String symbol;switch(asset){case "contacts/mobile.png":symbol="📱";break;case "contacts/call.png":symbol="📞";break;case "contacts/home.png":symbol="🏠";break;case "contacts/sms.png":symbol="💬";break;case "contacts/email.png":symbol="✉️";break;default:symbol="🌐";}
        return new BitmapDrawable(context.getResources(),WmResources.iconBitmap(context,asset,32,32,symbol));
    }
    public Phone selected(){return selected;}
    public Link selectedLink(){return selectedLink;}
    public Address selectedAddress(){return selectedAddress;}
    public int selectedTab(){return selectedTab;}
    public void setOnTabChangedListener(OnTabChangedListener listener){tabChanged=listener;}
    public void setTab(int tab){if(tab<0||tab>1)throw new IllegalArgumentException("Summary tab 0 or 1 required");if(selectedTab==tab)return;selectedTab=tab;render();if(tabChanged!=null)tabChanged.changed(tab);}
    /** User/provider strings are literal; validation completes before mutation. */
    public void setContact(String name,Phone[] phones,String notes){setContact(name,"","",phones,new Link[0],notes);}
    public void setContact(String name,String company,String jobTitle,Phone[] phones,Link[] links,String notes){
        setContact(name,company,jobTitle,phones,links,new Address[0],notes);
    }
    public void setContact(String name,String company,String jobTitle,Phone[] phones,Link[] links,Address[] addresses,String notes){
        if(phones==null||links==null||addresses==null)throw new IllegalArgumentException("Phones, links and addresses required");Phone[] copy=phones.clone();Link[] linkCopy=links.clone();Address[] addressCopy=addresses.clone();HashSet<String> keys=new HashSet<>();Phone retained=null;Link retainedLink=null;Address retainedAddress=null;
        String selectedKey=selected!=null?selected.key:selectedLink!=null?selectedLink.key:selectedAddress!=null?selectedAddress.key:null;
        for(Phone phone:copy){if(phone==null||!keys.add(phone.key))throw new IllegalArgumentException("Null or duplicate action key");if(phone.key.equals(selectedKey))retained=phone;}
        for(Link link:linkCopy){if(link==null||!keys.add(link.key))throw new IllegalArgumentException("Null or duplicate action key");if(link.key.equals(selectedKey))retainedLink=link;}
        for(Address address:addressCopy){if(address==null||!keys.add(address.key))throw new IllegalArgumentException("Null or duplicate action key");if(address.key.equals(selectedKey))retainedAddress=address;}
        String nextName=name==null?"":name,nextCompany=company==null?"":company,nextTitle=jobTitle==null?"":jobTitle,nextNotes=notes==null?"":notes;
        boolean same=nextName.equals(this.name)&&nextCompany.equals(this.company)&&nextTitle.equals(this.jobTitle)&&nextNotes.equals(notesText)&&copy.length==this.phones.length&&linkCopy.length==this.links.length&&addressCopy.length==this.addresses.length;
        for(int i=0;same&&i<copy.length;i++)same=copy[i].key.equals(this.phones[i].key)&&copy[i].label.equals(this.phones[i].label)&&copy[i].number.equals(this.phones[i].number)&&copy[i].action==this.phones[i].action&&copy[i].icon==this.phones[i].icon;
        for(int i=0;same&&i<linkCopy.length;i++)same=linkCopy[i].key.equals(this.links[i].key)&&linkCopy[i].label.equals(this.links[i].label)&&linkCopy[i].value.equals(this.links[i].value)&&linkCopy[i].kind==this.links[i].kind;
        for(int i=0;same&&i<addressCopy.length;i++)same=addressCopy[i].key.equals(this.addresses[i].key)&&addressCopy[i].label.equals(this.addresses[i].label)&&addressCopy[i].value.equals(this.addresses[i].value);
        if(same)return;
        int summaryOffset=summaryScroll.getRequestedScrollPosition(),notesOffset=notesScroll.getRequestedScrollPosition();
        this.name=nextName;this.company=nextCompany;this.jobTitle=nextTitle;notesText=nextNotes;this.phones=copy;this.links=linkCopy;this.addresses=addressCopy;selected=retained;selectedLink=retainedLink;selectedAddress=retainedAddress;
        if(selected==null&&selectedLink==null&&selectedAddress==null){if(copy.length>0)selected=copy[0];else if(linkCopy.length>0)selectedLink=linkCopy[0];else if(addressCopy.length>0)selectedAddress=addressCopy[0];}
        rows.removeAllViews();rowViews.clear();addressViews.clear();for(Phone phone:copy){PhoneRow row=new PhoneRow(getContext(),phone);rowViews.add(row);rows.addView(row,new LayoutParams(-1,64));}
        for(Link link:linkCopy){LinkRow row=new LinkRow(getContext(),link);rowViews.add(row);rows.addView(row,new LayoutParams(-1,64));}
        for(Address address:addressCopy){AddressRow row=new AddressRow(getContext(),address);addressViews.add(row);rows.addView(row,new LayoutParams(-1,-2));}
        this.notes.setText(notesText);nameHeader.setLayoutParams(new LayoutParams(-1,41+(this.company.isEmpty()?0:29)+(this.jobTitle.isEmpty()?0:29)));nameHeader.captionDirty=true;nameHeader.setContentDescription(this.name+(this.company.isEmpty()?"":"\n"+this.company)+(this.jobTitle.isEmpty()?"":"\n"+this.jobTitle));nameHeader.invalidate();render();summaryScroll.scrollToPosition(summaryOffset);notesScroll.scrollToPosition(notesOffset);
    }
    /** Authored loading/permission/error text owns the client; tabs remain. */
    public void setMessage(String authored){authoredMessage=authored;render();}
    private void render(){
        boolean showMessage=authoredMessage!=null;message.setVisibility(showMessage?VISIBLE:GONE);message.setText(showMessage?WmStrings.text(getContext(),language,authoredMessage):"");
        summary.setVisibility(!showMessage&&selectedTab==0?VISIBLE:GONE);notesScroll.setVisibility(!showMessage&&selectedTab==1?VISIBLE:GONE);
        WmControls.tab(summaryTab,selectedTab==0,WmPalette.face(selection));WmControls.tab(notesTab,selectedTab==1,WmPalette.face(selection));
        for(ActionRow row:rowViews){row.setSelected(selected!=null&&selected.key.equals(row.key)||selectedLink!=null&&selectedLink.key.equals(row.key));row.invalidate();}
        for(AddressRow row:addressViews){row.setSelected(selectedAddress!=null&&selectedAddress.key.equals(row.address.key));row.colors();}
    }
    private void choose(Phone phone){selected=phone;selectedLink=null;selectedAddress=null;render();listener.selected(phone);if(phone.action==SMS)listener.message(phone);else listener.dial(phone);}
    private void choose(Link link){selected=null;selectedLink=link;selectedAddress=null;render();listener.selectedLink(link);listener.openLink(link);}
    private void choose(Address address){selected=null;selectedLink=null;selectedAddress=address;render();listener.selectedAddress(address);listener.editAddress(address);}
    @Override public void onWmLanguageChanged(String language){this.language=language;WmControls.bindText(summaryTab,language,"요약");WmControls.bindText(notesTab,language,"메모");render();}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){
        if(regular==null||bold==null)throw new IllegalArgumentException("Summary fonts required");refreshArtwork();this.regular=regular;this.bold=bold;this.selection=selection;
        int summaryOffset=summaryScroll.getRequestedScrollPosition(),notesOffset=notesScroll.getRequestedScrollPosition();
        notes.setTypeface(regular);WmFonts.attach(notes,regular);message.setTypeface(regular);WmFonts.attach(message,regular);
        WmControls.restyleTree(summaryScroll,regular,bold,selection,tabColor);WmControls.restyleTree(notesScroll,regular,bold,selection,tabColor);WmControls.restyleTree(tabs,regular,bold,selection,tabColor);
        for(AddressRow row:addressViews)row.restyle();
        nameHeader.captionDirty=true;nameHeader.invalidate();for(ActionRow row:rowViews){row.captionDirty=true;row.invalidate();}render();summaryScroll.scrollToPosition(summaryOffset);notesScroll.scrollToPosition(notesOffset);
    }
    private final class NameHeader extends View {
        final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);boolean captionDirty=true;String caption="",companyCaption="",titleCaption="";
        NameHeader(Context context){super(context);setBackgroundColor(Color.WHITE);}
        @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){captionDirty=true;}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);ink.setTextSize(24);ink.setColor(Color.BLACK);
            if(captionDirty){float width=Math.max(0,getWidth()-16);ink.setTypeface(bold);caption=WmFonts.ellipsize(name,ink,width);ink.setTypeface(regular);companyCaption=WmFonts.ellipsize(company,ink,width);titleCaption=WmFonts.ellipsize(jobTitle,ink,width);captionDirty=false;}
            ink.setTypeface(bold);WmFonts.draw(canvas,caption,8,26,ink);ink.setTypeface(regular);int baseline=26;
            if(!company.isEmpty()){baseline+=29;WmFonts.draw(canvas,companyCaption,8,baseline,ink);}if(!jobTitle.isEmpty()){baseline+=29;WmFonts.draw(canvas,titleCaption,8,baseline,ink);}
            ink.setColor(0xffbdbebd);canvas.drawRect(0,getHeight()-1,getWidth(),getHeight(),ink);
        }
    }
    private abstract class ActionRow extends View {
        final String key,label,value;final int icon;final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);final WmCommandBar.KeyTouch touch=new WmCommandBar.KeyTouch(false);
        boolean captionDirty=true;String labelCaption="",numberCaption="";
        ActionRow(Context context,String key,String label,String value,int icon){super(context);this.key=key;this.label=label;this.value=value;this.icon=icon;setClickable(true);setSoundEffectsEnabled(false);setContentDescription(label+" "+value);setOnTouchListener(touch);}
        @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){captionDirty=true;}
        @Override protected void drawableStateChanged(){super.drawableStateChanged();invalidate();}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);boolean highlighted=isPressed()||isSelected();if(highlighted)canvas.drawColor(WmPalette.highlight(selection));
            Drawable drawable=icon==ICON_NONE?null:icons[icon];if(drawable!=null){drawable.setBounds(10,4,42,36);drawable.draw(canvas);}
            float width=Math.max(0,getWidth()-54-8);ink.setTextSize(24);
            if(captionDirty){ink.setTypeface(bold);labelCaption=WmFonts.ellipsize(label,ink,width);ink.setTypeface(regular);numberCaption=WmFonts.ellipsize(value,ink,width);captionDirty=false;}
            ink.setColor(highlighted?Color.WHITE:Color.BLACK);ink.setTypeface(bold);WmFonts.draw(canvas,labelCaption,54,26,ink);ink.setTypeface(regular);WmFonts.draw(canvas,numberCaption,54,52,ink);ink.setColor(0xffbdbebd);canvas.drawRect(0,62,getWidth(),64,ink);
        }
        abstract void activate();
        @Override public boolean performClick(){if(!isEnabled())return false;super.performClick();activate();return true;}
        @Override protected void onDetachedFromWindow(){touch.cancel(this);super.onDetachedFromWindow();}
    }
    private final class PhoneRow extends ActionRow {
        final Phone phone;
        PhoneRow(Context context,Phone phone){super(context,phone.key,phone.label,phone.number,phone.icon);this.phone=phone;}
        @Override void activate(){choose(phone);}
    }
    private final class LinkRow extends ActionRow {
        final Link link;
        LinkRow(Context context,Link link){super(context,link.key,link.label,link.value,link.kind==EMAIL?4:5);this.link=link;}
        @Override void activate(){choose(link);}
    }
    private final class AddressRow extends FrameLayout {
        final Address address;final TextView label,value;final WmCommandBar.KeyTouch touch=new WmCommandBar.KeyTouch(false);
        AddressRow(Context context,Address address){
            super(context);this.address=address;label=WmControls.text(context,regular,address.label);value=WmControls.text(context,regular,address.value);
            label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setGravity(Gravity.TOP|Gravity.RIGHT);value.setGravity(Gravity.TOP|Gravity.LEFT);
            addView(label,new FrameLayout.LayoutParams(-2,-2));addView(value,new FrameLayout.LayoutParams(-2,-2));restyle();
            setClickable(true);setSoundEffectsEnabled(false);setContentDescription(address.label+" "+address.value);setOnTouchListener(touch);
        }
        void restyle(){for(TextView text:new TextView[]{label,value}){text.setTypeface(regular);WmFonts.attach(text,regular);text.setLineSpacing(0,1);text.setLineSpacing(25-text.getLineHeight(),1);}requestLayout();colors();}
        void colors(){boolean highlighted=isPressed()||isSelected();setBackgroundColor(highlighted?WmPalette.highlight(selection):Color.WHITE);if(label!=null){label.setTextColor(highlighted?Color.WHITE:Color.BLACK);value.setTextColor(highlighted?Color.WHITE:Color.BLACK);}}
        @Override protected void drawableStateChanged(){super.drawableStateChanged();colors();}
        @Override protected void onMeasure(int widthSpec,int heightSpec){
            int width=MeasureSpec.getSize(widthSpec),available=Math.max(1,width-16),freeHeight=MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED);
            label.measure(MeasureSpec.makeMeasureSpec(available,MeasureSpec.AT_MOST),freeHeight);
            // Native column bound is unproven: reserve the actual label width plus an 8px gap.
            value.measure(MeasureSpec.makeMeasureSpec(Math.max(1,available-label.getMeasuredWidth()-8),MeasureSpec.EXACTLY),freeHeight);
            setMeasuredDimension(width,resolveSize(12+25*Math.max(1,value.getLineCount()),heightSpec));
        }
        @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
            int labelLeft=getWidth()-8-label.getMeasuredWidth(),labelTop=26-label.getBaseline(),valueTop=26-value.getBaseline();
            label.layout(labelLeft,labelTop,labelLeft+label.getMeasuredWidth(),labelTop+label.getMeasuredHeight());value.layout(8,valueTop,8+value.getMeasuredWidth(),valueTop+value.getMeasuredHeight());
        }
        @Override public boolean performClick(){if(!isEnabled())return false;super.performClick();choose(address);return true;}
        @Override protected void onDetachedFromWindow(){touch.cancel(this);super.onDetachedFromWindow();}
    }
}
