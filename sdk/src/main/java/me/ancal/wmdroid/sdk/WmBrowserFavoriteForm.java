package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** browsres dialog4167 field IDs/order, shared controls and literal drafts.
 * Owner supplies real destinations, the location popup, validation, persistence
 * and Add/Cancel commands. Field geometry matches the official 480x640 WM6.1 emulator capture.
 * Destination ordering and owner dialog/button placement remain separate contracts.
 */
public final class WmBrowserFavoriteForm extends LinearLayout implements WmControls.LanguageListener {
    public static final int NAME=12337,URL=12338,LOCATION=12340;
    public static final int NAME_LABEL=12356,URL_LABEL=12357,LOCATION_LABEL=12358;
    public static final int NAME_LIMIT=256,URL_LIMIT=2048;
    private final TextView[] labels=new TextView[3];
    private final EditText name,url;
    private final WmChoice location;

    public WmBrowserFavoriteForm(Context context,Typeface font,String name,String url,String[] locations,int selected,int accent){
        super(context);
        if(font==null||name==null||url==null)throw new IllegalArgumentException("Favorite font and draft required");
        setOrientation(VERTICAL);setPadding(12,12,12,12);setBackgroundColor(WmControls.SURFACE);
        this.name=input(context,font,NAME,NAME_LIMIT,InputType.TYPE_CLASS_TEXT,name);
        this.url=input(context,font,URL,URL_LIMIT,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI,url);
        location=new WmChoice(context,font,locations,selected,accent);location.setId(LOCATION);
        location.setPadding(4,3,30,3); // WM6.1.4 fixed inset2 plus measured nonclient border2.
        int[] ids={NAME_LABEL,URL_LABEL,LOCATION_LABEL};
        android.view.View[] fields={this.name,this.url,location};
        for(int n=0;n<labels.length;n++){
            labels[n]=WmControls.text(context,font,"");labels[n].setId(ids[n]);
            LayoutParams labelFit=new LayoutParams(-1,42);labelFit.topMargin=n==0?0:8;addView(labels[n],labelFit);
            addView(fields[n],new LayoutParams(n==2?330:-1,n==2?34:42));
        }
        onWmLanguageChanged(null);
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        // The native folder selector is narrower; clamp it for small SDK clients.
        LayoutParams fit=(LayoutParams)location.getLayoutParams();
        int available=MeasureSpec.getMode(widthSpec)==MeasureSpec.UNSPECIFIED?330:Math.max(0,MeasureSpec.getSize(widthSpec)-getPaddingLeft()-getPaddingRight());
        fit.width=Math.min(330,available);
        super.onMeasure(widthSpec,heightSpec);
    }
    private static EditText input(Context context,Typeface font,int id,int limit,int type,String value){
        EditText input=new EditText(context);WmControls.input(input,font);input.setId(id);input.setSingleLine(true);input.setInputType(type);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(limit)});input.setText(value);return input;
    }
    public EditText nameField(){return name;}
    public EditText urlField(){return url;}
    /** Owned location selector. Owner registers setOnOpenListener, supplies and
     * binds/removes the popup, then commits original locations index with choose. */
    public WmChoice locationField(){return location;}
    @Override public void onWmLanguageChanged(String language){
        String[] captions={"이름:","주소(URL):","위치 지정:"};
        for(int n=0;n<labels.length;n++)WmControls.bindText(labels[n],language,captions[n]);
    }
}
