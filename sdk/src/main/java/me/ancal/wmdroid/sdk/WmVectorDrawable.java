package me.ancal.wmdroid.sdk;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import org.json.*;

/** Bounded, data-only Canvas vectors for resource packs, including API14.
 * No SVG scripts, external URLs, embedded code or artwork constants.
 * The caller owns input; decode on a worker before attaching the Drawable.
 */
public final class WmVectorDrawable extends Drawable {
    public static final int MAX_BYTES=256*1024;
    private final ArrayList<Path> paths=new ArrayList<>();
    private final ArrayList<Paint> paints=new ArrayList<>();
    private final ArrayList<Integer> alphas=new ArrayList<>();
    private final Matrix group=new Matrix(),matrix=new Matrix();
    private final float[] transform=new float[9];
    private final Paint composite=new Paint();
    private float width,height;
    private int intrinsicWidth,intrinsicHeight,opacity=255;
    private boolean fit,pixelGrid,compositeFilter;
    private ColorFilter filter;
    private WmVectorDrawable(){}
    public static WmVectorDrawable read(InputStream input)throws IOException {
        if(input==null)throw new IOException("Vector input missing");
        try {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;
            while((n=input.read(buffer))!=-1){if(bytes.size()+n>MAX_BYTES)throw new IOException("Vector exceeds 256 KiB");bytes.write(buffer,0,n);}
            JSONObject data=new JSONObject(new String(bytes.toByteArray(),Charset.forName("UTF-8")));
            if(integer(data.get("version"),1,1)!=1)throw new IOException("Unsupported vector version");
            WmVectorDrawable result=new WmVectorDrawable();
            result.width=positive(data.get("width"),4096);result.height=positive(data.get("height"),4096);
            result.intrinsicWidth=integer(data.get("intrinsicWidth"),1,4096);result.intrinsicHeight=integer(data.get("intrinsicHeight"),1,4096);
            String mode=data.getString("scale");if(!mode.equals("fit")&&!mode.equals("stretch"))throw new IOException("Invalid vector scale");result.fit=mode.equals("fit");
            result.pixelGrid=bool(data,"pixelGrid");result.compositeFilter=bool(data,"compositeFilter");
            if(data.has("matrix"))result.group.setValues(values(data.getJSONArray("matrix"),9));
            JSONArray parts=data.getJSONArray("parts");if(parts.length()<1||parts.length()>512)throw new IOException("Invalid vector part count");
            int commands=0;
            for(int i=0;i<parts.length();i++){
                JSONObject part=parts.getJSONObject(i);Path path=new Path();String fill=part.getString("fillRule");
                if(!fill.equals("winding")&&!fill.equals("evenodd"))throw new IOException("Invalid fill rule");path.setFillType(fill.equals("evenodd")?Path.FillType.EVEN_ODD:Path.FillType.WINDING);
                JSONArray ops=part.getJSONArray("path");if((commands+=ops.length())>10000)throw new IOException("Too many vector commands");
                for(int j=0;j<ops.length();j++){
                    JSONArray op=ops.getJSONArray(j);String kind=op.getString(0);int count;
                    switch(kind){case "M":case "L":count=2;break;case "C":count=6;break;case "Q":count=4;break;case "O":count=3;break;case "Z":count=0;break;default:throw new IOException("Unsupported path command");}
                    if(op.length()!=count+1)throw new IOException("Invalid path arguments");float[] v=new float[count];for(int k=0;k<count;k++)v[k]=number(op.get(k+1));
                    switch(kind){case "M":path.moveTo(v[0],v[1]);break;case "L":path.lineTo(v[0],v[1]);break;case "C":path.cubicTo(v[0],v[1],v[2],v[3],v[4],v[5]);break;case "Q":path.quadTo(v[0],v[1],v[2],v[3]);break;case "O":if(v[2]<=0)throw new IOException("Invalid circle radius");path.addCircle(v[0],v[1],v[2],Path.Direction.CW);break;case "Z":path.close();break;}
                }
                Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(color(part.getString("color")));
                if(part.has("stroke")){
                    paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(positive(part.get("stroke"),4096));
                    String join=part.getString("join");if(!join.equals("round")&&!join.equals("miter"))throw new IOException("Invalid stroke join");paint.setStrokeJoin(join.equals("round")?Paint.Join.ROUND:Paint.Join.MITER);paint.setStrokeCap(Paint.Cap.BUTT);
                }
                if(part.has("gradient")){
                    JSONObject gradient=part.getJSONObject("gradient");String kind=gradient.getString("type");
                    if(!kind.equals("linear")&&!kind.equals("radial"))throw new IOException("Invalid gradient type");
                    float[] points=values(gradient.getJSONArray("points"),kind.equals("linear")?4:3);
                    JSONArray colors=gradient.getJSONArray("colors"),stops=gradient.getJSONArray("stops");
                    if(colors.length()<2||colors.length()>64||stops.length()!=colors.length())throw new IOException("Invalid gradient stops");
                    int[] c=new int[colors.length()];float[] s=new float[c.length];
                    for(int j=0;j<c.length;j++){c[j]=color(colors.getString(j));s[j]=number(stops.get(j));if(s[j]<0||s[j]>1||(j>0&&s[j]<s[j-1]))throw new IOException("Unordered gradient stops");}
                    Shader shader;if(kind.equals("linear"))shader=new LinearGradient(points[0],points[1],points[2],points[3],c,s,Shader.TileMode.CLAMP);
                    else {if(points[2]<=0)throw new IOException("Invalid gradient radius");shader=new RadialGradient(points[0],points[1],points[2],c,s,Shader.TileMode.CLAMP);}
                    if(gradient.has("matrix")){Matrix local=new Matrix();local.setValues(values(gradient.getJSONArray("matrix"),9));shader.setLocalMatrix(local);}
                    paint.setShader(shader);
                }
                int alpha=integer(part.get("alpha"),0,255);paint.setAlpha(alpha);
                result.paths.add(path);result.paints.add(paint);result.alphas.add(alpha);
            }
            return result;
        }catch(JSONException|IllegalArgumentException invalid){throw new IOException("Invalid vector data",invalid);}
    }
    /** Cache parsed geometry; each UI consumer receives independent mutable paints/state. */
    @Override public ConstantState getConstantState(){
        return new ConstantState(){
            @Override public Drawable newDrawable(){
                WmVectorDrawable copy=new WmVectorDrawable();
                copy.width=width;copy.height=height;copy.intrinsicWidth=intrinsicWidth;copy.intrinsicHeight=intrinsicHeight;
                copy.fit=fit;copy.pixelGrid=pixelGrid;copy.compositeFilter=compositeFilter;copy.group.set(group);
                copy.paths.addAll(paths);copy.alphas.addAll(alphas);
                for(Paint paint:paints)copy.paints.add(new Paint(paint));
                return copy;
            }
            @Override public int getChangingConfigurations(){return 0;}
        };
    }
    private static boolean bool(JSONObject data,String key)throws JSONException,IOException {Object value=data.get(key);if(!(value instanceof Boolean))throw new IOException("Expected vector boolean");return (Boolean)value;}
    private static float number(Object value)throws IOException {
        if(!(value instanceof Number))throw new IOException("Expected vector number");double n=((Number)value).doubleValue();if(Double.isNaN(n)||Double.isInfinite(n)||Math.abs(n)>100000)throw new IOException("Vector number out of range");return (float)n;
    }
    private static float positive(Object value,int max)throws IOException {float n=number(value);if(n<=0||n>max)throw new IOException("Invalid vector dimension");return n;}
    private static int integer(Object value,int min,int max)throws IOException {number(value);double n=((Number)value).doubleValue();if(n<min||n>max||n!=(int)n)throw new IOException("Invalid vector integer");return (int)n;}
    private static float[] values(JSONArray array,int count)throws JSONException,IOException {if(array.length()!=count)throw new IOException("Invalid matrix/gradient size");float[] values=new float[count];for(int i=0;i<count;i++)values[i]=number(array.get(i));return values;}
    private static int color(String value)throws IOException {if(!value.matches("[0-9a-fA-F]{8}"))throw new IOException("Invalid ARGB color");return (int)Long.parseLong(value,16);}
    @Override public void draw(Canvas canvas){
        if(getBounds().isEmpty()||opacity==0)return;int saved=canvas.save();
        float sx=getBounds().width()/width,sy=getBounds().height()/height;
        if(fit){float scale=Math.min(sx,sy);canvas.translate(getBounds().left+(getBounds().width()-width*scale)/2,getBounds().top+(getBounds().height()-height*scale)/2);canvas.scale(scale,scale);}
        else {canvas.translate(getBounds().left,getBounds().top);canvas.scale(sx,sy);}
        if(compositeFilter&&(opacity!=255||filter!=null))canvas.saveLayer(0,0,width,height,composite,Canvas.ALL_SAVE_FLAG);
        canvas.concat(group);boolean antialias=true;
        if(pixelGrid){canvas.getMatrix(matrix);matrix.getValues(transform);antialias=transform[Matrix.MSKEW_X]!=0||transform[Matrix.MSKEW_Y]!=0;for(int i=0;i<6;i++)antialias|=Math.abs(transform[i]-Math.round(transform[i]))>0.0001f;}
        for(int i=0;i<paths.size();i++){Paint paint=paints.get(i);if(pixelGrid)paint.setAntiAlias(antialias);paint.setAlpha(compositeFilter?alphas.get(i):alphas.get(i)*opacity/255);canvas.drawPath(paths.get(i),paint);}
        canvas.restoreToCount(saved);
    }
    @Override public int getIntrinsicWidth(){return intrinsicWidth;}
    @Override public int getIntrinsicHeight(){return intrinsicHeight;}
    @Override public void setAlpha(int value){opacity=Math.max(0,Math.min(255,value));composite.setAlpha(opacity);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter value){filter=value;if(compositeFilter)composite.setColorFilter(value);else for(Paint paint:paints)paint.setColorFilter(value);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
