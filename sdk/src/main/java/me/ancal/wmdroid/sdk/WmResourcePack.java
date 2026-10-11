package me.ancal.wmdroid.sdk;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.json.*;

/** Data-only version2 ZIPs with caller-defined resource IDs. Install off the UI thread; the caller owns input.
 * An immutable snapshot keeps its files while a new pack is activated.
 * This store does not decode artwork, execute files or include vendor assets.
 */
public final class WmResourcePack {
    public static final int MAX_FILES=4096,MAX_FILE_BYTES=64*1024*1024;
    public static final long MAX_BYTES=256L*1024*1024;
    private static final int MAX_MANIFEST=256*1024;
    private static final Charset UTF8=Charset.forName("UTF-8");
    private final File directory;
    private final Map<String,String> assets;
    private WmResourcePack(File directory,Map<String,String> assets){this.directory=directory;this.assets=Collections.unmodifiableMap(assets);}
    public Set<String> ids(){return assets.keySet();}
    public String generation(){return directory.getName();}
    boolean storedIn(File store){return directory.getParentFile().equals(store);}
    /** Read-only data transfer of this immutable snapshot. Caller owns output. */
    public void writeZip(OutputStream output)throws IOException {
        java.util.zip.ZipOutputStream zip=new java.util.zip.ZipOutputStream(new BufferedOutputStream(new FilterOutputStream(output){
            @Override public void write(byte[] bytes,int offset,int length)throws IOException {out.write(bytes,offset,length);}
            @Override public void close()throws IOException {flush();}
        },32768));
        // The local pipe favors transfer latency over archive size; bytes and CRC validation stay unchanged.
        zip.setLevel(java.util.zip.Deflater.BEST_SPEED);
        try {
            Set<String> paths=new TreeSet<>(assets.values());paths.add("pack.json");
            byte[] buffer=new byte[8192];long total=0;
            for(String path:paths){
                File source=child(directory,path);long limit=path.equals("pack.json")?MAX_MANIFEST:MAX_FILE_BYTES;
                if(!source.isFile()||source.length()>limit)throw new IOException("Pack file unavailable or too large");
                zip.putNextEntry(new ZipEntry(path));long size=0;
                try(InputStream in=new FileInputStream(source)){int n;while((n=in.read(buffer))!=-1){size+=n;total+=n;if(size>limit||total>MAX_BYTES)throw new IOException("Exported pack too large");zip.write(buffer,0,n);}}
                zip.closeEntry();
            }
        }finally{zip.close();}
    }
    public InputStream open(String id)throws IOException {
        return new FileInputStream(file(id));
    }
    /** Package-private: media constructors need immutable files, never mutate these. */
    File file(String id)throws IOException {
        String path=assets.get(id);if(path==null)throw new FileNotFoundException("Resource ID not in pack: "+id);
        return child(directory,path);
    }
    /** Null means no active pack; an unreadable installed pack is an explicit error. */
    public static synchronized WmResourcePack active(File store)throws IOException {
        File pointer=new File(store,"active");if(!pointer.exists())return null;
        String generation=new String(read(pointer,128),UTF8);
        if(!generation.matches("pack-[0-9a-f-]{36}"))throw new IOException("Invalid pack pointer");
        File directory=child(store,generation);
        return new WmResourcePack(directory,manifest(directory));
    }
    /** Deactivate without deleting files held by existing readers. */
    public static synchronized void deactivate(File store)throws IOException {
        File pointer=new File(store,"active");
        if(pointer.exists()&&!pointer.delete())throw new IOException("Cannot deactivate resource pack");
    }
    /** Publish only after every ZIP entry and mapped file has been checked.
     * Failed imports never change active. Old snapshots remain available.
     */
    public static synchronized WmResourcePack install(File store,InputStream input)throws IOException {
        if(input==null)throw new IOException("Pack input missing");
        if(!store.isDirectory()&&!store.mkdirs())throw new IOException("Cannot create pack store");
        File stage=new File(store,"pack-"+UUID.randomUUID());
        if(!stage.mkdir())throw new IOException("Cannot create pack stage");
        File pointer=null;boolean published=false;
        try {
            Set<String> names=new HashSet<>(),files=new HashSet<>();long total=0;
            // Close the ZIP reader but leave the caller's stream open.
            try(ZipInputStream zip=new ZipInputStream(new FilterInputStream(input){
                private long compressed;
                private void count(int n)throws IOException {if(n>0&&(compressed+=n)>MAX_BYTES)throw new IOException("Compressed pack too large");}
                @Override public int read()throws IOException {int n=in.read();count(n<0?0:1);return n;}
                @Override public int read(byte[] b,int off,int len)throws IOException {int n=in.read(b,off,len);count(n);return n;}
                @Override public void close(){}
            })){
                ZipEntry entry;byte[] buffer=new byte[8192];
                while((entry=zip.getNextEntry())!=null){
                    String name=entry.getName();boolean folder=entry.isDirectory();
                    if(folder)name=name.substring(0,name.length()-1);
                    File target=child(stage,name);
                    if(names.size()>=MAX_FILES*2||!names.add(name))throw new IOException("Duplicate or excessive ZIP entries");
                    if(folder){if(!target.isDirectory()&&!target.mkdirs())throw new IOException("Cannot create pack folder");continue;}
                    if(!name.equals("pack.json")&&!name.matches(".*\\.(png|gif|bmp|jpg|jpeg|webp|ico|cur|svg|ttf|otf|wav|ogg|avi|skn|pwi|json)"))throw new IOException("Unsupported pack file type");
                    if(files.size()>=MAX_FILES||!files.add(name))throw new IOException("Too many pack files");
                    File parent=target.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("Cannot create pack folder");
                    long size=0,limit=name.equals("pack.json")?MAX_MANIFEST:MAX_FILE_BYTES;
                    try(FileOutputStream output=new FileOutputStream(target)){
                        int n;while((n=zip.read(buffer))!=-1){size+=n;total+=n;if(size>limit||total>MAX_BYTES)throw new IOException("Inflated pack too large");output.write(buffer,0,n);}
                    }
                    zip.closeEntry();
                }
            }
            Map<String,String> mapping=manifest(stage);
            Set<String> expected=new HashSet<>(mapping.values());expected.add("pack.json");
            if(!files.equals(expected))throw new IOException("Pack contains missing or unmapped files");
            // Flush the validated stage as a batch so eMMC can commit many file writes together.
            for(String name:files)try(FileInputStream file=new FileInputStream(child(stage,name))){file.getFD().sync();}
            pointer=File.createTempFile("active-",".tmp",store);
            try(FileOutputStream out=new FileOutputStream(pointer)){out.write(stage.getName().getBytes(UTF8));out.getFD().sync();}
            // Android uses same-filesystem POSIX rename: readers see old or new pointer.
            if(!pointer.renameTo(new File(store,"active")))throw new IOException("Cannot activate pack");
            published=true;return new WmResourcePack(stage,mapping);
        }finally{
            if(pointer!=null&&pointer.exists())pointer.delete();
            if(!published)discard(stage);
        }
        // ponytail: old generations retained for snapshot/rollback; add explicit pruning with the pack manager.
    }
    private static Map<String,String> manifest(File directory)throws IOException {
        try {
            JSONObject root=new JSONObject(new String(read(new File(directory,"pack.json"),MAX_MANIFEST),UTF8));
            Object version=root.get("version");if(!(version instanceof Number)||((Number)version).doubleValue()!=2)throw new IOException("Unsupported pack version");
            JSONObject entries=root.getJSONObject("assets");if(entries.length()==0||entries.length()>MAX_FILES-1)throw new IOException("Invalid resource count");
            Map<String,String> result=new HashMap<>();Iterator<String> keys=entries.keys();
            while(keys.hasNext()){
                String id=keys.next();if(!id.matches("[A-Za-z0-9][A-Za-z0-9._/-]{0,159}"))throw new IOException("Invalid resource ID");
                Object value=entries.get(id);if(!(value instanceof String))throw new IOException("Resource path must be text");
                String path=(String)value;File file=child(directory,path);
                if(path.equals("pack.json")||!file.isFile())throw new IOException("Mapped resource missing");
                result.put(id,path);
            }
            return result;
        }catch(JSONException invalid){throw new IOException("Invalid pack manifest",invalid);}
    }
    private static File child(File parent,String name)throws IOException {
        if(name==null||name.length()>240||!name.matches("[A-Za-z0-9._/-]+"))throw new IOException("Invalid pack path");
        for(String part:name.split("/",-1))if(part.isEmpty()||part.equals(".")||part.equals(".."))throw new IOException("Unsafe pack path");
        File result=new File(parent,name);
        if(!result.getCanonicalPath().startsWith(parent.getCanonicalPath()+File.separator))throw new IOException("Pack path escapes store");
        return result;
    }
    private static byte[] read(File file,int limit)throws IOException {
        try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] bytes=new byte[4096];int n;while((n=in.read(bytes))!=-1){if(out.size()+n>limit)throw new IOException("Pack metadata too large");out.write(bytes,0,n);}return out.toByteArray();
        }
    }
    private static void discard(File file){File[] children=file.listFiles();if(children!=null)for(File child:children)discard(child);file.delete();}
}
