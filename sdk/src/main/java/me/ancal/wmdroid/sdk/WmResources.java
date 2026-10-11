package me.ancal.wmdroid.sdk;

import android.content.Context;
import java.io.*;
import java.util.Map;
import java.util.WeakHashMap;

/** Shared local pack resolver. Prepare/install on a worker before rendering.
 * Missing assets use packaged defaults when available.
 * Independent WM applets can mirror the host's data-only snapshot on a worker.
 */
public final class WmResources {
    private static final Map<Context,WmResourcePack> PACKS=new WeakHashMap<>();
    private static final Map<Context,Files> FILES=new WeakHashMap<>();
    private static final class Files {
        final WmResourcePack pack;final Map<String,File> entries=new java.util.HashMap<>();
        Files(WmResourcePack pack){this.pack=pack;}
    }
    private static final Map<Context,Vectors> VECTORS=new WeakHashMap<>();
    private static final class Vectors {
        final WmResourcePack pack;
        final Map<String,android.graphics.drawable.Drawable.ConstantState> entries=new java.util.HashMap<>();
        Vectors(WmResourcePack pack){this.pack=pack;}
    }
    private WmResources(){}
    private static Context owner(Context context){Context app=context.getApplicationContext();return app==null?context:app;}
    private static File store(Context context){return new File(context.getFilesDir(),"wmdroid-resource-packs");}
    private static File shellStore(Context context){return new File(context.getFilesDir(),"wmdroid-shell-resource-packs");}
    private static WmResourcePack selected(Context context)throws IOException {
        if(!WmShell.PACKAGE.equals(context.getPackageName())&&new File(shellStore(context),"source").isFile())return WmResourcePack.active(shellStore(context));
        WmResourcePack local=WmResourcePack.active(store(context));
        return local!=null?local:WmResourcePack.active(shellStore(context));
    }
    /** No disk reads. Prepare on a worker first; install changes this identity. */
    public static String revision(Context context){Context app=owner(context);synchronized(PACKS){WmResourcePack pack=PACKS.get(app);return app.getFilesDir().getAbsolutePath()+"|"+(pack==null?"bundled":pack.generation());}}
    public static WmResourcePack prepare(Context context)throws IOException {
        Context app=owner(context);
        synchronized(PACKS){if(!PACKS.containsKey(app))PACKS.put(app,selected(app));return PACKS.get(app);}
    }
    /** Follow the launcher on a worker; applets do not need their own pack setting.
     * Unavailable hosts keep the current selection; an explicit empty generation selects
     * bundled assets without falling back to a remaining local pack. Successful sync
     * can replace a local install with the host mirror.
     * Returns true only when the selected snapshot changed. No network or signature gate.
     */
    public static synchronized boolean syncFromShell(Context context)throws IOException {
        Context app=owner(context);prepare(app);
        if(WmShell.PACKAGE.equals(app.getPackageName()))return false;
        String generation;
        try(android.database.Cursor rows=app.getContentResolver().query(WmShell.RESOURCES,new String[]{"generation"},null,null,null)){
            if(rows==null)return false;
            int column=rows.getColumnIndex("generation");
            if(column<0||!rows.moveToFirst()||rows.isNull(column))throw new IOException("Invalid shell pack revision");
            generation=rows.getString(column);
        }catch(RuntimeException unavailable){return false;}
        if(!generation.isEmpty()&&!generation.matches("pack-[0-9a-f-]{36}"))throw new IOException("Invalid shell pack revision");
        File mirror=shellStore(app),source=new File(mirror,"source");
        if(generation.isEmpty())return mirrorSnapshot(app,generation,null);
        if(source.isFile()&&source.length()<=128&&new File(mirror,"active").isFile()){
            byte[] token=new byte[(int)source.length()];try(DataInputStream in=new DataInputStream(new FileInputStream(source))){in.readFully(token);}
            if(generation.equals(new String(token,"UTF-8"))){
                WmResourcePack cached=prepare(app);
                if(cached!=null&&cached.storedIn(mirror))return false;
                WmResourcePack pack=WmResourcePack.active(mirror);synchronized(PACKS){PACKS.put(app,pack);}WmIcons.prepare(app);return cached!=pack;
            }
        }
        android.net.Uri snapshot=WmShell.RESOURCES.buildUpon().appendPath(generation).build();
        try(InputStream input=app.getContentResolver().openInputStream(snapshot)){return mirrorSnapshot(app,generation,input);}
        catch(RuntimeException unavailable){throw new IOException("Shell pack transfer unavailable",unavailable);}
    }
    private static boolean mirrorSnapshot(Context app,String generation,InputStream input)throws IOException {
        WmResourcePack before=prepare(app),pack;File mirror=shellStore(app),source=new File(mirror,"source");
        if(generation.isEmpty()){
            if(before==null&&source.isFile()&&source.length()==0&&!new File(mirror,"active").exists())return false;
            WmResourcePack.deactivate(mirror);pack=null;
        }else{
            pack=WmResourcePack.install(mirror,input);
        }
        if(!mirror.isDirectory()&&!mirror.mkdirs())throw new IOException("Cannot create shell pack cache");
        File pending=File.createTempFile("source-",".tmp",mirror);
        try{
            try(FileOutputStream out=new FileOutputStream(pending)){out.write(generation.getBytes("UTF-8"));out.getFD().sync();}
            if(!pending.renameTo(source))throw new IOException("Cannot save shell pack revision");
        }finally{if(pending.exists())pending.delete();}
        synchronized(PACKS){PACKS.put(app,pack);}
        if(before==pack)return false;
        WmIcons.prepare(app);return true;
    }
    /** Use this entry point instead of modifying the managed store directly.
     * Pack extraction never holds the reader cache lock; existing readers keep
     * their prior snapshot until the verified pack is activated.
     */
    public static synchronized WmResourcePack install(Context context,InputStream input)throws IOException {
        Context app=owner(context);prepare(app);
        WmResourcePack pack=WmResourcePack.install(store(app),input);
        synchronized(PACKS){PACKS.put(app,pack);}
        WmIcons.prepare(app);notifyHost(app);return pack;
    }
    /** Deactivate the local pack and recalculate selection; external apps prioritize
     * a saved mirror source record. Existing snapshots remain usable. Run on a worker. */
    public static synchronized void reset(Context context)throws IOException {
        Context app=owner(context);WmResourcePack.deactivate(store(app));
        synchronized(PACKS){PACKS.put(app,selected(app));}
        notifyHost(app);
    }
    private static void notifyHost(Context app){if(WmShell.PACKAGE.equals(app.getPackageName()))app.getContentResolver().notifyChange(WmShell.STATE,null);}
    public static InputStream open(Context context,String id)throws IOException {
        WmResourcePack pack=prepare(context);if(pack==null)throw new FileNotFoundException("No resource pack installed");return pack.open(id);
    }
    /** Pack vectors are parsed once per app/generation, including cached absence.
     * Prewarm on an import/startup worker; each call returns a fresh Drawable.
     * A cold call performs bounded local parsing, never network access.
     */
    public static android.graphics.drawable.Drawable vector(Context context,String id)throws IOException {
        if(id==null||!id.matches("[A-Za-z0-9][A-Za-z0-9._/-]{0,159}"))throw new IllegalArgumentException("Invalid vector ID");
        Context app=owner(context);WmResourcePack pack=prepare(app);
        synchronized(VECTORS){
            Vectors cache=VECTORS.get(app);if(cache==null||cache.pack!=pack){cache=new Vectors(pack);VECTORS.put(app,cache);}
            if(!cache.entries.containsKey(id)){
                android.graphics.drawable.Drawable.ConstantState state=null;
                if(pack!=null&&pack.ids().contains(id))try(InputStream in=pack.open(id)){state=WmVectorDrawable.read(in).getConstantState();}
                catch(IOException invalid){cache.entries.put(id,null);throw invalid;}
                cache.entries.put(id,state);
            }
            android.graphics.drawable.Drawable.ConstantState state=cache.entries.get(id);
            if(state==null)throw new FileNotFoundException("Vector unavailable: "+id);
            return state.newDrawable();
        }
    }
    /** Optional native icon artwork; cached per immutable pack, never bundled. */
    static android.graphics.drawable.Drawable iconRaster(Context context,String path,int width,int height)throws IOException {
        Context app=owner(context);WmResourcePack pack=prepare(app);String id="assets/"+path;
        synchronized(VECTORS){
            Vectors cache=VECTORS.get(app);if(cache==null||cache.pack!=pack){cache=new Vectors(pack);VECTORS.put(app,cache);}
            if(!cache.entries.containsKey(id)){
                android.graphics.drawable.Drawable.ConstantState state=null;
                if(pack!=null&&pack.ids().contains(id))try{
                    android.graphics.drawable.BitmapDrawable image=new android.graphics.drawable.BitmapDrawable((android.content.res.Resources)null,bitmap(app,path,width,height));
                    image.setFilterBitmap(true);state=image.getConstantState();
                }catch(IOException invalid){cache.entries.put(id,null);throw invalid;}
                cache.entries.put(id,state);
            }
            android.graphics.drawable.Drawable.ConstantState state=cache.entries.get(id);
            if(state==null)throw new FileNotFoundException("Icon raster unavailable");
            return state.newDrawable().mutate();
        }
    }
    /** Role paths resolve through assets/&lt;path&gt; in the selected resource pack.
     */
    public static InputStream openAsset(Context context,String path)throws IOException {
        WmResourcePack pack=prepare(context);String id="assets/"+path;
        return pack!=null&&pack.ids().contains(id)?pack.open(id):context.getAssets().open(path);
    }
    /** File-backed media. Packs already have immutable files; legacy assets are
     * copied once to a unique cache file. Never overwrite a file held by a font
     * or SoundPool, including during pack replacement. Call on a worker.
     * limit is bytes,1..67108864; invalid limits throw IllegalArgumentException,
     * files above the limit throw IOException.
     */
    public static File assetFile(Context context,String path,int limit)throws IOException {
        if(limit<1||limit>WmResourcePack.MAX_FILE_BYTES)throw new IllegalArgumentException("Invalid media size limit");
        Context app=owner(context);WmResourcePack pack=prepare(app);String id="assets/"+path;
        if(pack!=null&&pack.ids().contains(id)){
            File file=pack.file(id);if(!file.isFile()||file.length()>limit)throw new IOException("Media asset unavailable or too large");return file;
        }
        synchronized(FILES){
            Files cache=FILES.get(app);if(cache==null||cache.pack!=pack){cache=new Files(pack);FILES.put(app,cache);}
            File ready=cache.entries.get(path);if(ready!=null&&ready.isFile()){if(ready.length()>limit)throw new IOException("Media asset too large");return ready;}
            File file=File.createTempFile("wmdroid-resource-",".data",app.getCacheDir());boolean complete=false;
            java.security.MessageDigest digest=sha256();
            try {
                try(InputStream in=app.getAssets().open(path);FileOutputStream out=new FileOutputStream(file)){
                    byte[] buffer=new byte[8192];int n;long size=0;
                    while((n=in.read(buffer))!=-1){size+=n;if(size>limit)throw new IOException("Media asset too large");digest.update(buffer,0,n);out.write(buffer,0,n);}out.getFD().sync();
                }
                StringBuilder name=new StringBuilder("wmdroid-resource-");for(byte value:digest.digest())name.append(String.format(java.util.Locale.US,"%02x",value&255));
                File shared=new File(app.getCacheDir(),name.append(".data").toString());
                if(shared.exists()){
                    if(!shared.isFile()||shared.length()!=file.length()||!sameBytes(file,shared))throw new IOException("Resource cache content mismatch");
                    if(!file.delete())throw new IOException("Cannot remove temporary resource copy");
                }else if(!file.renameTo(shared))throw new IOException("Cannot publish resource cache");
                cache.entries.put(path,shared);complete=true;return shared;
            }finally{if(!complete)file.delete();}
        }
        // ponytail: legacy copies live until cache eviction; remove this path when bundled assets are stripped.
    }
    private static java.security.MessageDigest sha256()throws IOException {
        try{return java.security.MessageDigest.getInstance("SHA-256");}catch(java.security.NoSuchAlgorithmException missing){throw new IOException("SHA-256 unavailable",missing);}
    }
    private static boolean sameBytes(File first,File second)throws IOException {
        byte[] a=new byte[8192],b=new byte[8192];
        try(InputStream in=new FileInputStream(first);java.io.DataInputStream other=new java.io.DataInputStream(new FileInputStream(second))){
            int n;while((n=in.read(a))!=-1){
                try{other.readFully(b,0,n);}catch(java.io.EOFException shorter){return false;}
                for(int i=0;i<n;i++)if(a[i]!=b[i])return false;
            }
            return other.read()==-1;
        }
    }
    /** Fixed-geometry raster assets: reject oversized or wrong-size images before allocation. */
    public static android.graphics.Bitmap bitmap(Context context,String path,int width,int height)throws IOException {
        if(width<1||height<1||width>4096||height>4096||(long)width*height>1048576)throw new IllegalArgumentException("Invalid asset geometry");
        byte[] bytes;
        try(InputStream in=openAsset(context,path);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[4096];int n;
            while((n=in.read(buffer))!=-1){if(out.size()+n>256*1024)throw new IOException("Raster asset too large");out.write(buffer,0,n);}bytes=out.toByteArray();
        }
        android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inJustDecodeBounds=true;
        android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        if(options.outWidth!=width||options.outHeight!=height)throw new IOException("Raster asset geometry mismatch");
        options.inJustDecodeBounds=false;options.inScaled=false;
        android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        if(bitmap==null)throw new IOException("Raster asset unavailable");bitmap.setDensity(android.graphics.Bitmap.DENSITY_NONE);return bitmap;
    }
    /** Preserve original pixels when available; no-pack icons keep identical bounds. */
    public static android.graphics.Bitmap iconBitmap(Context context,String path,int width,int height,String symbol){
        try{return bitmap(context,path,width,height);}catch(IOException absent){return emojiBitmap(symbol,width,height);}
    }
    /** System emoji only, never vendor font/artwork. Unsupported glyphs use a square. */
    public static android.graphics.Bitmap emojiBitmap(String symbol,int width,int height){
        if(symbol==null||symbol.trim().isEmpty()||symbol.length()>32||width<1||height<1||width>4096||height>4096||(long)width*height>1048576)throw new IllegalArgumentException("Invalid emoji geometry or symbol");
        android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);paint.setColor(android.graphics.Color.BLACK);paint.setTextSize(Math.min(width,height)*0.85f);
        if(android.os.Build.VERSION.SDK_INT>=23&&!Glyph.has(paint,symbol))symbol="\u25a1";
        android.graphics.Rect ink=new android.graphics.Rect();paint.getTextBounds(symbol,0,symbol.length(),ink);
        if(ink.isEmpty()){symbol="\u25a1";paint.getTextBounds(symbol,0,symbol.length(),ink);}
        float fit=Math.min(1f,Math.min((float)Math.max(1,width-2)/Math.max(1,ink.width()),(float)Math.max(1,height-2)/Math.max(1,ink.height())));
        paint.setTextSize(paint.getTextSize()*fit);paint.getTextBounds(symbol,0,symbol.length(),ink);
        android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(width,height,android.graphics.Bitmap.Config.ARGB_8888);image.setDensity(android.graphics.Bitmap.DENSITY_NONE);
        android.graphics.Canvas canvas=new android.graphics.Canvas(image);canvas.clipRect(0,0,width,height);
        canvas.drawText(symbol,(width-ink.width())/2f-ink.left,(height-ink.height())/2f-ink.top,paint);return image;
    }
    private static final class Glyph {static boolean has(android.graphics.Paint paint,String symbol){return paint.hasGlyph(symbol);}}

}
