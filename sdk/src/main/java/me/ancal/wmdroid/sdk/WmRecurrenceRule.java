package me.ancal.wmdroid.sdk;

/** Immutable recurrence pattern and range, without provider or timezone policy.
 * Serializes newly authored rules only; it does not parse existing RRULEs.
 * Timed UNTIL is caller-converted basic UTC; all-day UNTIL is a basic date.
 * Years0001..9999 are accepted. This RFC codec does not imply CalendarProvider
 * expansion support: API14 YEARLY BYSETPOS and non-Sunday weekly anchors differ.
 * Leap seconds are rejected. Native input limits and UI ordering are not claimed.
 */
public final class WmRecurrenceRule {
    public static final int NONE=0,DAILY=1,WEEKLY=2,MONTHLY_DAY=3,
            MONTHLY_ORDINAL=4,YEARLY_DAY=5,YEARLY_ORDINAL=6;
    public static final int SUNDAY=1,MONDAY=2,TUESDAY=4,WEDNESDAY=8,
            THURSDAY=16,FRIDAY=32,SATURDAY=64;
    public static final int ALL=127,WEEKDAYS=62,WEEKEND=65;
    public static final int NEVER=0,COUNT=1,UNTIL=2;
    public final int pattern,interval,dayMask,day,month,ordinal,endMode,count;

    /** Native calstore028E0240 duration bound in minutes, not a recurrence
     * expansion estimate. Monthly uses28 days; yearly uses12 such months.
     * Multiweek masks scan one Sunday-based week, exactly as the native code.
     * Long arithmetic avoids native overflow for programmatic large intervals. */
    public long minimumPeriodMinutes(){
        switch(pattern){
            case NONE:return 0;
            case DAILY:return dayMask==WEEKDAYS?1440L:1440L*interval;
            case WEEKLY:
                long gap=7L*interval;int span=interval==1?14:7;
                for(int first=0;first<span;first++)if((dayMask&(1<<(first%7)))!=0)
                    for(int next=first+1;next<span;next++)
                        if(first%7!=next%7&&(dayMask&(1<<(next%7)))!=0)gap=Math.min(gap,next-first);
                return gap*1440L;
            case MONTHLY_DAY:case MONTHLY_ORDINAL:return 40320L*interval;
            case YEARLY_DAY:case YEARLY_ORDINAL:return 40320L*12;
            default:throw new IllegalStateException("Unknown recurrence pattern");
        }
    }
    /** Civil wall-clock duration, not elapsed time across a DST transition.
     * Negative values are rejected for every pattern. NONE has no upper bound.
     * Recurring rules allow equality and reject values above minimumPeriodMinutes()*60000. */
    public void validateAppointmentDuration(long civilMillis){
        if(civilMillis<0)throw new IllegalArgumentException("유효한 지속 시간을 입력하십시오.");
        long minimum=minimumPeriodMinutes();
        if(minimum>0&&civilMillis>minimum*60000L)
            throw new IllegalArgumentException("저장할 수 없습니다. 시작 날짜와 시간은 이전 항목이 끝난 시간보다 뒤에 와야 하고, 끝 날짜와 시간은 다음 항목이 시작되는 시간보다 앞에 와야 합니다.");
    }

    /** Native4205 compares the enabled lead alone with the minimum period.
     * Equality is rejected; no duration+lead sum is used. */
    public void validateAppointmentReminder(boolean enabled,long minutes){
        if(!enabled)return;
        if(minutes<0)throw new IllegalArgumentException("Negative reminder");
        if(pattern!=NONE&&minutes>=minimumPeriodMinutes())
            throw new IllegalArgumentException("저장할 수 없습니다. 미리 알림 시간은 이전 항목이 끝난 시간보다 뒤에 와야 합니다.");
    }

    private WmRecurrenceRule(int pattern,int interval,int dayMask,int day,
                             int month,int ordinal,int endMode,int count){
        this.pattern=pattern;this.interval=interval;this.dayMask=dayMask;this.day=day;
        this.month=month;this.ordinal=ordinal;this.endMode=endMode;this.count=count;
    }

    public static WmRecurrenceRule none(){return rule(NONE,1,0,0,0,0);}
    public static WmRecurrenceRule daily(int interval,boolean weekdays){
        positive(interval,"Interval");
        if(weekdays&&interval!=1)throw new IllegalArgumentException("Weekdays require interval1");
        return rule(DAILY,interval,weekdays?WEEKDAYS:0,0,0,0);
    }
    public static WmRecurrenceRule weekly(int interval,int dayMask){
        positive(interval,"Interval");mask(dayMask);return rule(WEEKLY,interval,dayMask,0,0,0);
    }
    public static WmRecurrenceRule monthlyDay(int interval,int day){
        positive(interval,"Interval");day(day);return rule(MONTHLY_DAY,interval,0,day,0,0);
    }
    public static WmRecurrenceRule monthlyOrdinal(int interval,int ordinal,int dayMask){
        positive(interval,"Interval");ordinal(ordinal);mask(dayMask);
        return rule(MONTHLY_ORDINAL,interval,dayMask,0,0,ordinal);
    }
    public static WmRecurrenceRule yearlyDay(int month,int day){
        month(month);day(day);
        if(day>monthDays(2000,month))throw new IllegalArgumentException("Impossible month/day");
        return rule(YEARLY_DAY,1,0,day,month,0);
    }
    public static WmRecurrenceRule yearlyOrdinal(int month,int ordinal,int dayMask){
        month(month);ordinal(ordinal);mask(dayMask);
        return rule(YEARLY_ORDINAL,1,dayMask,0,month,ordinal);
    }
    private static WmRecurrenceRule rule(int pattern,int interval,int dayMask,int day,int month,int ordinal){
        return new WmRecurrenceRule(pattern,interval,dayMask,day,month,ordinal,NEVER,0);
    }
    public WmRecurrenceRule withCount(int count){
        if(pattern==NONE)throw new IllegalArgumentException("No recurrence cannot have an end range");
        positive(count,"Count");return ranged(COUNT,count);
    }
    /** UNTIL date and UTC conversion remain caller-owned until serialization. */
    public WmRecurrenceRule withUntil(){
        if(pattern==NONE)throw new IllegalArgumentException("No recurrence cannot have an end range");
        return ranged(UNTIL,0);
    }
    public WmRecurrenceRule withoutEnd(){return endMode==NEVER?this:ranged(NEVER,0);}
    private WmRecurrenceRule ranged(int endMode,int count){
        return new WmRecurrenceRule(pattern,interval,dayMask,day,month,ordinal,endMode,count);
    }

    /** Timed RRULE without the RRULE: prefix. UNTIL requires caller-converted
     * Gregorian UTC yyyyMMddTHHmmssZ; years0001..9999, ASCII digits, no leap seconds.
     * Other end modes require null, including NONE which then returns null.
     * Invalid arguments throw IllegalArgumentException. */
    public String toRrule(String untilUtc){
        return serialize(untilUtc,false);
    }
    /** All-day RRULE without the RRULE: prefix. UNTIL requires Gregorian
     * yyyyMMdd, years0001..9999 and ASCII digits, without time or Z.
     * Other end modes require null, including NONE which then returns null.
     * Invalid arguments throw IllegalArgumentException. */
    public String toAllDayRrule(String untilDate){return serialize(untilDate,true);}
    private String serialize(String untilUtc,boolean allDay){
        if(endMode==UNTIL){if(allDay)validateDate(untilUtc);else validateUntil(untilUtc);}
        else if(untilUtc!=null)throw new IllegalArgumentException("UNTIL supplied for another range");
        if(pattern==NONE)return null;
        StringBuilder value=new StringBuilder("FREQ=");
        switch(pattern){
            case DAILY:value.append(dayMask==WEEKDAYS?"WEEKLY":"DAILY");break;
            case WEEKLY:value.append("WEEKLY");break;
            case MONTHLY_DAY:case MONTHLY_ORDINAL:value.append("MONTHLY");break;
            case YEARLY_DAY:case YEARLY_ORDINAL:value.append("YEARLY");break;
            default:throw new IllegalStateException("Unknown pattern");
        }
        if(interval>1)value.append(";INTERVAL=").append(interval);
        if(month!=0)value.append(";BYMONTH=").append(month);
        if(day!=0)value.append(";BYMONTHDAY=").append(day);
        boolean singleOrdinal=ordinal!=0&&(dayMask&(dayMask-1))==0;
        if(dayMask!=0){value.append(";BYDAY=");if(singleOrdinal)value.append(ordinal);appendDays(value,dayMask);}
        if(ordinal!=0&&!singleOrdinal)value.append(";BYSETPOS=").append(ordinal);
        if(endMode==COUNT)value.append(";COUNT=").append(count);
        else if(endMode==UNTIL)value.append(";UNTIL=").append(untilUtc);
        return value.toString();
    }
    private static void appendDays(StringBuilder value,int dayMask){
        boolean separator=false;
        for(int index=0;index<7;index++){
            int day=(index+1)%7;
            if((dayMask&(1<<day))==0)continue;
            if(separator)value.append(',');separator=true;
            switch(day){
                case 0:value.append("SU");break;
                case 1:value.append("MO");break;
                case 2:value.append("TU");break;
                case 3:value.append("WE");break;
                case 4:value.append("TH");break;
                case 5:value.append("FR");break;
                case 6:value.append("SA");break;
                default:throw new IllegalStateException("Unknown weekday");
            }
        }
    }
    private static void validateDate(String value){
        if(value==null||value.length()!=8)throw new IllegalArgumentException("UNTIL requires basic Gregorian date");
        int year=digits(value,0,4),month=digits(value,4,6),day=digits(value,6,8);
        if(year<1||month<1||month>12||day<1||day>monthDays(year,month))throw new IllegalArgumentException("Invalid Gregorian UNTIL date");
    }
    private static void validateUntil(String value){
        if(value==null||value.length()!=16||value.charAt(8)!='T'||value.charAt(15)!='Z')
            throw new IllegalArgumentException("UNTIL requires basic UTC date/time");
        int year=digits(value,0,4),month=digits(value,4,6),day=digits(value,6,8);
        int hour=digits(value,9,11),minute=digits(value,11,13),second=digits(value,13,15);
        if(year<1||month<1||month>12||day<1||day>monthDays(year,month)
                ||hour>23||minute>59||second>59)
            throw new IllegalArgumentException("Invalid UTC Gregorian UNTIL");
    }
    private static int digits(String value,int start,int end){
        int result=0;
        for(int index=start;index<end;index++){
            char digit=value.charAt(index);
            if(digit<'0'||digit>'9')throw new IllegalArgumentException("UNTIL requires ASCII digits");
            result=result*10+digit-'0';
        }
        return result;
    }
    private static int monthDays(int year,int month){
        switch(month){
            case 2:return year%4==0&&(year%100!=0||year%400==0)?29:28;
            case 4:case 6:case 9:case 11:return 30;
            default:return 31;
        }
    }
    private static void positive(int value,String name){if(value<=0)throw new IllegalArgumentException(name+" must be positive");}
    private static void day(int value){if(value<1||value>31)throw new IllegalArgumentException("Day outside1..31");}
    private static void month(int value){if(value<1||value>12)throw new IllegalArgumentException("Month outside1..12");}
    private static void ordinal(int value){if(value==0||value< -5||value>5)throw new IllegalArgumentException("Ordinal outside-5..-1/1..5");}
    private static void mask(int value){if(value<=0||(value&~ALL)!=0)throw new IllegalArgumentException("Invalid weekday mask");}

    @Override public boolean equals(Object other){
        if(this==other)return true;if(!(other instanceof WmRecurrenceRule))return false;
        WmRecurrenceRule rule=(WmRecurrenceRule)other;
        return pattern==rule.pattern&&interval==rule.interval&&dayMask==rule.dayMask
                &&day==rule.day&&month==rule.month&&ordinal==rule.ordinal
                &&endMode==rule.endMode&&count==rule.count;
    }
    @Override public int hashCode(){
        int value=pattern;value=value*31+interval;value=value*31+dayMask;
        value=value*31+day;value=value*31+month;value=value*31+ordinal;
        value=value*31+endMode;return value*31+count;
    }
}
