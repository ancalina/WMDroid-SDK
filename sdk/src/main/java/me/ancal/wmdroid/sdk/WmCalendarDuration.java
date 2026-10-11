package me.ancal.wmdroid.sdk;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

/** CalendarProvider fixed elapsed duration adaptation, not nominal civil-day
 * arithmetic across DST. RFC5545 week/day/time forms are supported. The legacy
 * Android provider form P<n>S is read only; canonical output uses PT<n>S.
 * Negative/fractional/overflow durations are rejected, not silently truncated. */
public final class WmCalendarDuration {
    private static final Pattern RFC=Pattern.compile("\\+?P(?:(\\d+)W|(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)?)");
    private static final Pattern LEGACY=Pattern.compile("\\+?P(\\d+)S");
    private WmCalendarDuration(){}
    /** PIMDLG027EA880 editor units: bare numbers are hours, not minutes.
     * Unit aliases follow Korean ROM4036..4039. Decimal parsing uses Java's
     * locale parser with grouping disabled; trim-length must be 1..255.
     * Case-insensitive units: min/mins/minute/minutes/minute(s)/m/분;
     * hour/hours/hr/hrs/hour(s)/h/시간 or none; day/days/day(s)/d/일;
     * week/weeks/wk/wks/week(s)/w/주. Nonnegative explicit-unit values truncate to whole minutes
     * after the native94370400-minute cap. Bare zero cannot finish natively. */
    public static int parseEditorMinutes(String value,Locale locale){
        if(value==null||locale==null)throw editorError();
        String input=value.trim();if(input.length()==0||input.length()>255)throw editorError();
        NumberFormat format=NumberFormat.getNumberInstance(locale);format.setGroupingUsed(false);
        if(!(format instanceof DecimalFormat))throw editorError();
        ((DecimalFormat)format).setParseBigDecimal(true);
        ParsePosition position=new ParsePosition(0);Number number=format.parse(input,position);
        if(!(number instanceof BigDecimal)||position.getIndex()==0)throw editorError();
        BigDecimal amount=(BigDecimal)number;if(amount.signum()<0)throw editorError();
        String unit=input.substring(position.getIndex()).trim().toLowerCase(Locale.US);
        String[][] aliases={{"min","mins","minute","minutes","minute(s)","m","분"},
            {"","hour","hours","hr","hrs","hour(s)","h","시간"},
            {"day","days","day(s)","d","일"},{"week","weeks","wk","wks","week(s)","w","주"}};
        int[] factors={1,60,1440,10080};int factor=0;
        for(int i=0;i<aliases.length;i++)for(String alias:aliases[i])if(alias.equals(unit))factor=factors[i];
        if(factor==0)throw editorError();
        int minutes=amount.multiply(BigDecimal.valueOf(factor)).min(BigDecimal.valueOf(94370400)).intValue();
        if(minutes==0&&unit.length()==0)throw editorError();return minutes;
    }
    private static IllegalArgumentException editorError(){return new IllegalArgumentException("유효한 지속 시간을 입력하십시오.");}
    public static long parseRfcMillis(String value){return parse(value,false);}
    public static long parseProviderMillis(String value){return parse(value,true);}
    private static long parse(String value,boolean legacy){
        if(value==null)throw new IllegalArgumentException("Duration required");
        Matcher matcher=RFC.matcher(value);
        if(!matcher.matches()){
            Matcher old=LEGACY.matcher(value);if(legacy&&old.matches())return scaled(old.group(1),1000L);
            throw new IllegalArgumentException("Invalid Calendar duration");
        }
        if(value.endsWith("T"))throw new IllegalArgumentException("Empty duration time part");
        long result=0;boolean component=false;long[] factors={604800000L,86400000L,3600000L,60000L,1000L};
        for(int i=1;i<=5;i++)if(matcher.group(i)!=null){component=true;long amount=scaled(matcher.group(i),factors[i-1]);if(result>Long.MAX_VALUE-amount)throw new IllegalArgumentException("Duration overflow");result+=amount;}
        if(!component)throw new IllegalArgumentException("Empty duration");return result;
    }
    private static long scaled(String number,long factor){
        long amount;try{amount=Long.parseLong(number);}catch(NumberFormatException invalid){throw new IllegalArgumentException("Duration overflow");}
        if(amount<0||amount>Long.MAX_VALUE/factor)throw new IllegalArgumentException("Duration overflow");return amount*factor;
    }
    /** Exact whole-second elapsed output; no implicit rounding. */
    public static String timed(long elapsedMillis){if(elapsedMillis<0||elapsedMillis%1000!=0)throw new IllegalArgumentException("Whole nonnegative seconds required");return "PT"+(elapsedMillis/1000)+"S";}
    public static String allDay(int days){if(days<1)throw new IllegalArgumentException("Positive all-day duration required");return "P"+days+"D";}
    /** DTEND wins if present. Otherwise a recurring event must supply DURATION. */
    public static long eventEnd(long begin,Long explicitEnd,String duration){
        if(explicitEnd!=null)return explicitEnd;
        long length=parseProviderMillis(duration);if(begin>Long.MAX_VALUE-length)throw new IllegalArgumentException("Event end overflow");return begin+length;
    }
}
