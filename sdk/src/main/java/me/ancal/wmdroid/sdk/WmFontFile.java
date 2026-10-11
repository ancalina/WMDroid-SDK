package me.ancal.wmdroid.sdk;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

/** Private immutable TTF/OTF imports. Caller owns the input stream.
 * Only the launcher's canonical read-only font URI is accepted for resolution.
 */
public final class WmFontFile {
    public static final int MAX_BYTES=16*1024*1024;
    private static final String PREFIX="content://me.ancal.wmdroid.shell/fonts/";
    private WmFontFile(){}
    private static boolean hash(String value){return value!=null&&value.matches("[0-9a-f]{64}");}
    public static boolean isSource(String source){return source!=null&&source.startsWith(PREFIX)&&hash(source.substring(PREFIX.length()));}
    public static File file(Context context,String hash){if(!hash(hash))throw new IllegalArgumentException("Invalid font hash");return new File(new File(context.getFilesDir(),"wm-fonts"),hash+".font");}
    public static String source(File file){String name=file.getName();if(!name.endsWith(".font")||!hash(name.substring(0,name.length()-5)))throw new IllegalArgumentException("Invalid font filename");return PREFIX+name.substring(0,name.length()-5);}
    public static boolean available(Context context,String source){if(!isSource(source))return false;File f=file(context,source.substring(PREFIX.length()));return f.isFile()&&f.length()>=12&&f.length()<=MAX_BYTES;}
    public static synchronized File importFont(Context context,InputStream input)throws IOException{return copy(context,input,null);}
    public static synchronized File resolve(Context context,String source)throws IOException {
        if(!isSource(source))throw new IOException("Untrusted font source");
        String expected=source.substring(PREFIX.length());File local=file(context,expected);
        if(available(context,source))return local;
        try(InputStream input=context.getContentResolver().openInputStream(Uri.parse(source))){if(input==null)throw new IOException("Font source unavailable");return copy(context,input,expected);}
    }
    private static File copy(Context context,InputStream input,String expected)throws IOException {
        if(input==null)throw new IOException("Font input missing");
        File dir=new File(context.getFilesDir(),"wm-fonts");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create font directory");
        File pending=File.createTempFile("import-",".tmp",dir);
        try {
            MessageDigest digest;try{digest=MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException failure){throw new IOException("SHA-256 unavailable",failure);}
            long size=0;try(FileOutputStream output=new FileOutputStream(pending)){byte[] bytes=new byte[8192];int count;while((count=input.read(bytes))!=-1){if(count==0){int one=input.read();if(one==-1)break;bytes[0]=(byte)one;count=1;}size+=count;if(size>MAX_BYTES)throw new IOException("Font exceeds 16 MiB");output.write(bytes,0,count);digest.update(bytes,0,count);}output.getFD().sync();}
            StringBuilder hex=new StringBuilder(64);for(byte b:digest.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));String value=hex.toString();
            if(expected!=null&&!expected.equals(value))throw new IOException("Font hash mismatch");
            validate(pending);
            try{if(Typeface.createFromFile(pending)==null)throw new IOException("Invalid font face");}catch(RuntimeException failure){throw new IOException("Invalid font face",failure);}
            File target=file(context,value);if(target.exists()){if(!same(pending,target))throw new IOException("Font cache collision");return target;}
            if(!pending.renameTo(target))throw new IOException("Cannot publish font");return target;
        } finally {if(pending.exists())pending.delete();}
    }
    private static boolean same(File a,File b)throws IOException {
        if(!b.isFile()||a.length()!=b.length())return false;
        try(RandomAccessFile left=new RandomAccessFile(a,"r");RandomAccessFile right=new RandomAccessFile(b,"r")){byte[] l=new byte[8192],r=new byte[8192];int n;while((n=left.read(l))!=-1){right.readFully(r,0,n);for(int i=0;i<n;i++)if(l[i]!=r[i])return false;}return true;}
    }
    static void validate(File file)throws IOException {
        try(RandomAccessFile in=new RandomAccessFile(file,"r")){
            long size=in.length();if(size<12||size>MAX_BYTES)throw new IOException("Truncated font");
            int kind=in.readInt();boolean cff=kind==0x4f54544f;if(kind!=0x00010000&&!cff)throw new IOException("Only TTF and OTF are supported");
            int count=in.readUnsignedShort();long directory=12L+16L*count;if(count<1||count>256||directory>size)throw new IOException("Invalid font directory");
            in.seek(12);Set<Integer> tags=new HashSet<>();
            for(int i=0;i<count;i++){int tag=in.readInt();in.readInt();long offset=in.readInt()&0xffffffffL,length=in.readInt()&0xffffffffL;if(!tags.add(tag)||length==0||offset<directory||offset>size||length>size-offset)throw new IOException("Invalid font table");}
            if(!tags.contains(0x636d6170)||!tags.contains(0x68656164)||!tags.contains(0x6d617870))throw new IOException("Required font tables missing");
            if(cff?!(tags.contains(0x43464620)||tags.contains(0x43464632)):!(tags.contains(0x676c7966)&&tags.contains(0x6c6f6361)))throw new IOException("Font outlines missing");
        }
    }
}
