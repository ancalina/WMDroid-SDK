package me.ancal.wmdroid.sdk;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/** ROM NLS defaults and PIM kind10 captions; see design/date-format-evidence.json.
 * Other regions retain the platform format until their native profile is verified. */
final class WmDateFormats {
    private WmDateFormats(){}
    private static boolean region(Locale locale,String language,String country){return language.equals(locale.getLanguage())&&(locale.getCountry().isEmpty()||country.equals(locale.getCountry()));}
    /** ko with empty/KR country uses yy-MM-dd; en with empty/US uses M/d/yy.
     * Other Locales use Android SHORT date format. */
    static DateFormat shortDate(Locale locale){
        if(region(locale,"ko","KR"))return new SimpleDateFormat("yy-MM-dd",locale);
        if(region(locale,"en","US"))return new SimpleDateFormat("M/d/yy",locale);
        return DateFormat.getDateInstance(DateFormat.SHORT,locale);
    }
    /** PIM kind5: short-date order with a four-digit year. Native era exceptions remain unported. */
    static SimpleDateFormat fullDate(Locale locale){
        DateFormat source=shortDate(locale);
        if(!(source instanceof SimpleDateFormat))throw new IllegalArgumentException("Locale short date pattern required");
        return new SimpleDateFormat(fullYearPattern(((SimpleDateFormat)source).toPattern()),locale);
    }
    /** PIM kind9 on Korean949/English1252: weekday prefix and full-year short date.
     * Other code pages/era overrides retain an SDK fallback pending native profiles. */
    static SimpleDateFormat recurrenceDate(Locale locale){return new SimpleDateFormat("EEE "+fullDate(locale).toPattern(),locale);}
    static String fullYearPattern(String pattern){
        StringBuilder full=new StringBuilder();boolean quoted=false;
        for(int i=0;i<pattern.length();i++){
            char ch=pattern.charAt(i);
            if(ch=='\''){full.append(ch);if(i+1<pattern.length()&&pattern.charAt(i+1)=='\'')full.append(pattern.charAt(++i));else quoted=!quoted;}
            else if(ch=='y'&&!quoted){full.append("yyyy");while(i+1<pattern.length()&&pattern.charAt(i+1)=='y')i++;}
            else full.append(ch);
        }
        return full.toString();
    }
    static DateFormat pickerDate(Locale locale){
        if(region(locale,"en","US"))return new SimpleDateFormat("MMM dd, yy",locale);
        return shortDate(locale);
    }
}
