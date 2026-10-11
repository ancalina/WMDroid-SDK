package me.ancal.wmdroid.sdk;

import java.util.Locale;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

/** Minute captions shared by PIM editors and Calendar consumers. */
public final class WmTimeFormat {
    private WmTimeFormat() {}

    /** caldll02904E68 DetailsBar: preserve literal location; no parentheses when empty. */
    public static String calendarDetails(String time,String location){
        if(time==null||location==null)throw new IllegalArgumentException("Calendar detail captions required");
        return location.isEmpty()?time:time+" ("+location+")";
    }

    /** Java date-format pattern; caller supplies Android's 12/24-hour preference.
     * KO/KR and US ordering follows ROM NLS with TIME_NOSECONDS. Neutral Korean
     * uses the KR profile. Other locales retain the SDK's existing AM/PM suffix;
     * native regional overrides and patterns are not reproduced. */
    public static String minutePattern(Locale locale,boolean hour24) {
        if(locale==null)throw new IllegalArgumentException("Locale required");
        if(hour24)return "HH:mm";
        return "ko".equals(locale.getLanguage())&&(locale.getCountry().isEmpty()||"KR".equals(locale.getCountry()))?"a h:mm":"h:mm a";
    }

    /** Reusable UI-thread formatter for native PIMUTIL307 Agenda captions.
     * Inputs are civil minutes, as in PIMUTIL308; callers own timezone conversion
     * and duration validation. This formatter never clips or reorders endpoints. */
    public static final class Agenda {
        private final SimpleDateFormat clock;
        private final GregorianCalendar work=new GregorianCalendar(TimeZone.getTimeZone("UTC"),Locale.US);
        private final String multiday;
        public Agenda(Locale locale,boolean hour24,String multiday) {
            if(multiday==null||multiday.isEmpty())throw new IllegalArgumentException("Multiday caption required");
            clock=new SimpleDateFormat(minutePattern(locale,hour24),locale);clock.setTimeZone(work.getTimeZone());
            work.setGregorianChange(new Date(Long.MIN_VALUE));work.setLenient(false);this.multiday=multiday;
        }
        /** visibleDay's time is ignored. Midnight ends on a later date compare
         * against the previous civil day, but display their actual midnight.
         * Both endpoints show start-end; only start shows start-multiday; only end
         * shows multiday-end; neither shows multiday. Equal visible endpoints show
         * one time. Literal " AM" then " PM" are compacted to a/p, dropping the tail.
         * Null values are invalid; callers validate endpoint order and duration. */
        public String format(WmDateTimeField.Value visibleDay,WmDateTimeField.Value start,WmDateTimeField.Value end) {
            if(visibleDay==null||start==null||end==null)throw new IllegalArgumentException("Civil dates required");
            boolean starts=sameDay(visibleDay,start),ends;
            if(end.hour==0&&end.minute==0&&!sameDay(start,end)){
                work.clear();work.set(end.year,end.month-1,end.day,12,0,0);work.add(Calendar.DAY_OF_MONTH,-1);
                ends=work.get(Calendar.ERA)==GregorianCalendar.AD&&visibleDay.year==work.get(Calendar.YEAR)&&visibleDay.month==work.get(Calendar.MONTH)+1&&visibleDay.day==work.get(Calendar.DAY_OF_MONTH);
            }else ends=sameDay(visibleDay,end);
            if(!starts&&!ends)return multiday;
            String first=starts?minute(start):multiday;
            return start.equals(end)?first:first+"-"+(ends?minute(end):multiday);
        }
        private String minute(WmDateTimeField.Value value) {
            work.clear();work.set(2000,0,1,value.hour,value.minute,0);String text=clock.format(work.getTime());
            // PIMUTIL163 compacts only these literal English markers.
            int marker=text.indexOf(" AM");char suffix='a';
            if(marker<0){marker=text.indexOf(" PM");suffix='p';}
            return marker<0?text:text.substring(0,marker)+suffix;
        }
        private static boolean sameDay(WmDateTimeField.Value a,WmDateTimeField.Value b){return a.year==b.year&&a.month==b.month&&a.day==b.day;}
    }
}
