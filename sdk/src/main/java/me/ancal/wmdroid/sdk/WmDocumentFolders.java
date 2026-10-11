package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.Collator;
import java.util.Arrays;
import java.util.HashSet;

/** DocList dialog13443: New/Rename/Delete plus flat folders. Owner owns every write.
 * New creates immediately in the owner, then editName starts native-style label editing.
 * Back cancels only the label edit; it never undoes an already-created folder. */
public final class WmDocumentFolders extends FrameLayout implements WmControls.LanguageListener,WmControls.RestyleListener,WmControls.BackHandler,WmControls.ModalOwner {
    public static final int NEW=1009,RENAME=1010,DELETE=1012,LIST=1044,NAME_LIMIT=64;
    public interface Listener {void command(int command,String folder);void rename(String folder,String name);}
    private final Listener listener;
    private final LinearLayout rows;
    private final TextView heading;
    private final TextView[] buttons=new TextView[3];
    private WmNotesView.Folder[] folders=new WmNotesView.Folder[0];
    private Typeface regular,bold;
    private int accent;
    private String language="ko",selected,current;
    private boolean busy;
    private EditText edit;
    private Bitmap icon,activeIcon;
    private String text(String authored){return WmStrings.text(getContext(),language,authored);}
    private java.util.Locale locale(){return WmStrings.locale("en".equals(language)?"en-US":"ko".equals(language)?"ko-KR":language);}
    private boolean english(){return "en".equals(language)||language.startsWith("en-");}
    public WmDocumentFolders(Context context,Typeface regular,Typeface bold,int accent,Listener listener){
        super(context);if(regular==null||bold==null||listener==null)throw new IllegalArgumentException("Folder fonts/listener required");
        this.regular=regular;this.bold=bold;this.accent=accent;this.listener=listener;setBackgroundColor(Color.WHITE);
        refreshArtwork();
        LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);addView(content,new LayoutParams(-1,-1));
        FrameLayout band=new FrameLayout(context);heading=WmControls.text(context,regular,"");heading.setId(2069);heading.setPadding(16,0,0,0);heading.setTextColor(accent);band.addView(heading,new LayoutParams(-1,48));
        View rule=new View(context);rule.setBackgroundColor(Color.BLACK);band.addView(rule,new LayoutParams(-1,2,Gravity.BOTTOM));content.addView(band,new LinearLayout.LayoutParams(-1,50));
        LinearLayout actions=new LinearLayout(context);actions.setPadding(6,6,8,6);int[] ids={NEW,RENAME,DELETE};
        for(int i=0;i<3;i++){final int command=ids[i];TextView button=WmControls.text(context,bold,"");button.setId(command);WmControls.button(button,accent);buttons[i]=button;LinearLayout.LayoutParams fit=new LinearLayout.LayoutParams(0,42,1);if(i>0)fit.leftMargin=8;actions.addView(button,fit);button.setOnClickListener(v->{if(busy)return;if(edit!=null&&!commitEditing())return;if(command==RENAME)editName(selected);else listener.command(command,selected);});}
        content.addView(actions,new LinearLayout.LayoutParams(-1,54));rows=new LinearLayout(context);rows.setId(LIST);rows.setOrientation(LinearLayout.VERTICAL);content.addView(new WmScrollView(context,rows),new LinearLayout.LayoutParams(-1,0,1));captions();
    }
    private String artworkRevision;
    private boolean refreshArtwork(){
        if(artworkRevision==null)try{WmResources.prepare(getContext());}catch(java.io.IOException absent){}
        String revision=WmResources.revision(getContext());if(revision.equals(artworkRevision))return false;
        icon=load("files/folder-closed.png");activeIcon=load("files/folder-selected.png");artworkRevision=revision;return true;
    }
    private void refreshRowIcons(){
        for(int i=0;i<rows.getChildCount();i++){FrameLayout row=(FrameLayout)rows.getChildAt(i);Bitmap image=row.getTag().equals(current)?activeIcon:icon;
            for(int j=0;j<row.getChildCount();j++)if(row.getChildAt(j) instanceof android.widget.ImageView)((android.widget.ImageView)row.getChildAt(j)).setImageBitmap(image);
        }
    }
    private Bitmap load(String name){return WmResources.iconBitmap(getContext(),name,32,32,name.contains("selected")?"📂":"📁");}
    public void setFolders(WmNotesView.Folder[] values,String current){
        if(values==null)throw new IllegalArgumentException("Folders required");HashSet<String> ids=new HashSet<>();WmNotesView.Folder[] copy=values.clone();boolean retained=false;
        for(WmNotesView.Folder value:copy){if(value==null||!ids.add(value.id))throw new IllegalArgumentException("Duplicate/null folder");if(value.id.equals(selected))retained=true;}
        this.current=current;folders=copy;if(!retained)selected=null;edit=null;rebuild();
    }
    public String selectedId(){return selected;}
    public void setBusy(boolean value){busy=value;if(edit!=null)edit.setEnabled(!value);captions();}
    @Override public boolean blocksOwnerCommands(){return busy;}
    @Override public View inputMethodOwner(){return edit;}
    public boolean isEditing(){return edit!=null;}
    public void editName(String id){if(id==null||busy)return;for(WmNotesView.Folder folder:folders)if(folder.id.equals(id)){selected=id;edit=null;rebuild();for(int i=0;i<rows.getChildCount();i++){FrameLayout row=(FrameLayout)rows.getChildAt(i);if(id.equals(row.getTag())){row.removeViewAt(0);edit=new EditText(getContext());WmControls.input(edit,regular);edit.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);edit.setSingleLine(true);edit.setText(folder.name);edit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(NAME_LIMIT)});edit.setImeOptions(EditorInfo.IME_ACTION_DONE);LayoutParams fit=new LayoutParams(-1,-1);fit.leftMargin=36;row.addView(edit,fit);edit.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_DONE||event!=null&&event.getKeyCode()==KeyEvent.KEYCODE_ENTER&&event.getAction()==KeyEvent.ACTION_DOWN){commitEditing();return true;}return false;});edit.requestFocus();edit.selectAll();break;}}captions();return;}}
    /** Return is not save success: changed valid names call rename synchronously
     * and always return false, even after callback setFolders. No automatic busy
     * or command replay; async owner sets busy, then refreshes and releases it. */
    public boolean commitEditing(){
        if(busy)return false;if(edit==null)return true;String value=edit.getText().toString();int start=0,end=value.length();while(start<end&&value.charAt(start)==' ')start++;while(end>start&&value.charAt(end-1)==' ')end--;value=value.substring(start,end);
        if(value.length()>NAME_LIMIT||!WmDocumentProperties.validName(value)){
            final EditText source=edit;final int selectionStart=source.getSelectionStart(),selectionEnd=source.getSelectionEnd();final WmMessageBox[] box=new WmMessageBox[1];
            box[0]=new WmMessageBox(getContext(),regular,locale(),"폴더","올바른 폴더 이름을 입력하세요.",accent,()->{
                removeView(box[0]);if(edit==source){source.requestFocus();if(selectionStart>=0&&selectionEnd>=0&&selectionStart<=source.length()&&selectionEnd<=source.length())source.setSelection(selectionStart,selectionEnd);}
            });box[0].setCaptionContext("notes.folders.title");box[0].onWmRestyle(regular,bold,accent,0);addView(box[0],new LayoutParams(-1,-1));return false;
        }
        for(WmNotesView.Folder folder:folders)if(folder.id.equals(selected)&&folder.name.equals(value)){edit=null;rebuild();return true;}
        listener.rename(selected,value);return false;
    }
    @Override public boolean back(){for(int i=getChildCount()-1;i>=1;i--)if(WmControls.back(getChildAt(i)))return true;if(busy)return true;if(edit==null)return false;edit=null;rebuild();return true;}
    private void captions(){heading.setText(text("폴더 추가/삭제"));String[] labels={text("새로 만들기"),text("이름 바꾸기"),text("삭제")};for(int i=0;i<3;i++){buttons[i].setText(labels[i]);buttons[i].setEnabled(!busy&&(i==0||selected!=null));}}
    private void rebuild(){
        rows.removeAllViews();Collator order=Collator.getInstance(locale());order.setStrength(Collator.SECONDARY);Arrays.sort(folders,(a,b)->{int n=order.compare(a.name,b.name);return n==0?a.id.compareTo(b.id):n;});
        for(WmNotesView.Folder folder:folders){FrameLayout row=new FrameLayout(getContext());row.setTag(folder.id);TextView label=WmControls.text(getContext(),regular,folder.name);label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,english()?8*192f/72:24);label.setPadding(36,0,2,0);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);WmControls.entry(label,accent);boolean active=folder.id.equals(selected);label.setTextColor(active?Color.WHITE:Color.BLACK);if(active)label.setBackgroundColor(accent);row.addView(label,new LayoutParams(-1,-1));Bitmap image=folder.id.equals(current)&&activeIcon!=null?activeIcon:icon;if(image!=null){android.widget.ImageView glyph=new android.widget.ImageView(getContext());glyph.setImageBitmap(image);row.addView(glyph,new LayoutParams(32,32,Gravity.LEFT|Gravity.CENTER_VERTICAL));}label.setOnClickListener(v->{if(busy)return;if(edit!=null&&!commitEditing())return;selected=folder.id;rebuild();});rows.addView(row,new LinearLayout.LayoutParams(-1,Math.max(34,(int)Math.ceil(label.getPaint().getFontMetrics().descent-label.getPaint().getFontMetrics().ascent)+2)));}captions();
    }
    private void updateRows(){for(int i=0;i<rows.getChildCount();i++){FrameLayout row=(FrameLayout)rows.getChildAt(i);for(int j=0;j<row.getChildCount();j++)if(row.getChildAt(j) instanceof TextView&&!(row.getChildAt(j) instanceof EditText)){TextView label=(TextView)row.getChildAt(j);boolean active=row.getTag().equals(selected);label.setTextColor(active?Color.WHITE:Color.BLACK);label.setBackgroundColor(active?accent:Color.TRANSPARENT);label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,english()?8*192f/72:24);}}}
    @Override public void onWmLanguageChanged(String language){this.language=WmStrings.language(getContext(),language);captions();updateRows();}
    @Override public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor){this.regular=regular;this.bold=bold;accent=selection;if(refreshArtwork())refreshRowIcons();heading.setTextColor(selection);updateRows();}
    @Override protected void onDetachedFromWindow(){for(int i=getChildCount()-1;i>=1;i--)if(getChildAt(i) instanceof WmMessageBox)removeViewAt(i);super.onDetachedFromWindow();}
}
