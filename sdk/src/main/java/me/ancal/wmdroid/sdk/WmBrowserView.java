package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import java.net.URI;
import java.net.URISyntaxException;

/** Hosted browser client; the owner supplies title, softkeys, menus and lifecycle.
 * Native IE command IDs come from browsres4096. Web rendering is Android WebView,
 * not Trident. Address-row geometry and web scrollbars remain provisional. */
public final class WmBrowserView extends LinearLayout implements WmControls.BackHandler {
    public interface Listener {void changed(WmBrowserView browser);void invalidAddress(String address);}
    private final EditText address;
    private final WebView web;
    private Listener listener;
    private boolean closed,loading;
    private String title="";
    public WmBrowserView(Context context,Typeface font){
        super(context);setOrientation(VERTICAL);setBackgroundColor(0xffffffff);
        address=new EditText(context);WmControls.input(address,font);address.setSingleLine(true);
        address.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
        address.setImeOptions(EditorInfo.IME_ACTION_GO);address.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2048)});
        address.setContentDescription("URL");addView(address,new LayoutParams(-1,WmControls.ROW_HEIGHT));
        web=new WebView(context);addView(web,new LayoutParams(-1,0,1));
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setGeolocationEnabled(false);
        settings.setSupportZoom(true);settings.setBuiltInZoomControls(true);settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);settings.setLoadWithOverviewMode(true);
        if(Build.VERSION.SDK_INT>=16){settings.setAllowFileAccessFromFileURLs(false);settings.setAllowUniversalAccessFromFileURLs(false);}
        if(Build.VERSION.SDK_INT>=21)settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,String url){return !allowed(url);}
            @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap icon){if(closed)return;loading=true;sync(url);}
            @Override public void onPageFinished(WebView view,String url){if(closed)return;loading=false;sync(url);}
        });
        web.setWebChromeClient(new WebChromeClient(){@Override public void onReceivedTitle(WebView view,String value){if(closed)return;title=value==null?"":value;notifyChanged();}});
        address.setOnEditorActionListener((view,action,event)->{if(action!=EditorInfo.IME_ACTION_GO)return false;try{navigate(address.getText().toString());}catch(IllegalArgumentException invalid){if(listener!=null)listener.invalidAddress(address.getText().toString());}return true;});
    }
    /** User navigation permits HTTP(S) only; blank is an internal home document. */
    /** Scheme matches [A-Za-z][A-Za-z0-9+.-]*:; specify HTTP(S) explicitly for
     * host:port addresses. Trim-length limit is2048 UTF-16 code units before
     * adding https://. Reject non-HTTP(S) schemes and user-info. */
    public static String normalizeAddress(String value){
        if(value==null)throw new IllegalArgumentException("URL required");String text=value.trim();
        if(text.isEmpty()||text.equals("about:blank"))return "about:blank";
        if(text.length()>2048)throw new IllegalArgumentException("URL exceeds2048 characters");
        for(int i=0;i<text.length();i++)if(Character.isISOControl(text.charAt(i)))throw new IllegalArgumentException("URL contains control characters");
        if(!text.matches("^[A-Za-z][A-Za-z0-9+.-]*:.*"))text="https://"+text;
        try{URI uri=new URI(text);String scheme=uri.getScheme();if(!("https".equalsIgnoreCase(scheme)||"http".equalsIgnoreCase(scheme))||uri.getHost()==null||uri.getRawUserInfo()!=null||uri.getPort()>65535)throw new IllegalArgumentException("HTTP(S) host required");return uri.toASCIIString();}catch(URISyntaxException invalid){throw new IllegalArgumentException("Invalid URL",invalid);}
    }
    private static boolean allowed(String url){try{return normalizeAddress(url).equals(url)||url.equalsIgnoreCase(normalizeAddress(url));}catch(IllegalArgumentException invalid){return false;}}
    public void setListener(Listener listener){this.listener=listener;}
    public EditText addressField(){return address;}
    public WebView webView(){return web;}
    public String url(){return closed?"":web.getUrl()==null?"":web.getUrl();}
    public String pageTitle(){return title;}
    public boolean isLoading(){return loading;}
    public boolean canForward(){return !closed&&web.canGoForward();}
    public boolean canBack(){return !closed&&web.canGoBack();}
    public void navigate(String value){String next=normalizeAddress(value);if(closed)return;address.setText(next);web.loadUrl(next);web.requestFocus();((android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(address.getWindowToken(),0);}
    public void showAddress(boolean visible){address.setVisibility(visible?VISIBLE:GONE);if(visible){address.requestFocus();address.selectAll();}}
    public boolean isAddressVisible(){return address.getVisibility()==VISIBLE;}
    public void forward(){if(canForward())web.goForward();}
    public void refresh(){if(!closed)web.reload();}
    public void stop(){if(!closed){web.stopLoading();loading=false;notifyChanged();}}
    @Override public boolean back(){if(!canBack())return false;web.goBack();return true;}
    public void pause(){if(!closed)web.onPause();}
    public void resume(){if(!closed)web.onResume();}
    /** Idempotent; remove the WebView before destroying its native state. */
    public void close(){if(closed)return;closed=true;listener=null;web.stopLoading();removeView(web);web.destroy();}
    private void sync(String url){if(!address.hasFocus())address.setText(url==null?"":url);notifyChanged();}
    private void notifyChanged(){if(listener!=null)listener.changed(this);}
    @Override protected void onDetachedFromWindow(){pause();super.onDetachedFromWindow();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();resume();}
}
