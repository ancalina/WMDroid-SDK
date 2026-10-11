package me.ancal.wmdroid.sdk;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Independent authored wire examples, not a recurrence expander or provider test. */
public final class WmRecurrenceRuleChecks {
    private static int passed;
    private interface Attempt {void run();}
    public static void main(String[] args) {
        check(WmRecurrenceRule.none().toRrule(null)==null,"none has no wire rule");
        wire(WmRecurrenceRule.daily(1,false),"FREQ=DAILY");
        wire(WmRecurrenceRule.daily(3,false),"FREQ=DAILY;INTERVAL=3");
        wire(WmRecurrenceRule.daily(1,true),"FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR");
        wire(WmRecurrenceRule.weekly(2,WmRecurrenceRule.SUNDAY|WmRecurrenceRule.MONDAY|WmRecurrenceRule.FRIDAY),"FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,FR,SU");
        wire(WmRecurrenceRule.monthlyDay(2,31),"FREQ=MONTHLY;INTERVAL=2;BYMONTHDAY=31");
        wire(WmRecurrenceRule.monthlyOrdinal(1,2,WmRecurrenceRule.TUESDAY),"FREQ=MONTHLY;BYDAY=2TU");
        wire(WmRecurrenceRule.monthlyOrdinal(1,-1,WmRecurrenceRule.WEEKDAYS),"FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-1");
        wire(WmRecurrenceRule.monthlyOrdinal(1,1,WmRecurrenceRule.WEEKEND),"FREQ=MONTHLY;BYDAY=SA,SU;BYSETPOS=1");
        wire(WmRecurrenceRule.monthlyOrdinal(1,-5,WmRecurrenceRule.ALL),"FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR,SA,SU;BYSETPOS=-5");
        wire(WmRecurrenceRule.yearlyDay(2,29),"FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=29");
        wire(WmRecurrenceRule.yearlyOrdinal(11,4,WmRecurrenceRule.THURSDAY),"FREQ=YEARLY;BYMONTH=11;BYDAY=4TH");
        WmRecurrenceRule base=WmRecurrenceRule.weekly(1,WmRecurrenceRule.MONDAY);
        WmRecurrenceRule count=base.withCount(1),until=count.withUntil();
        wire(count,"FREQ=WEEKLY;BYDAY=MO;COUNT=1");
        check("FREQ=WEEKLY;BYDAY=MO;UNTIL=20000229T235959Z".equals(until.toRrule("20000229T235959Z")),"UNTIL replaces COUNT");
        wire(until.withCount(9),"FREQ=WEEKLY;BYDAY=MO;COUNT=9");
        wire(until.withoutEnd(),"FREQ=WEEKLY;BYDAY=MO");
        check(base.endMode==WmRecurrenceRule.NEVER&&base.count==0&&count.endMode==WmRecurrenceRule.COUNT&&count.count==1&&until.count==0,"range copies do not mutate source");
        WmRecurrenceRule equal=WmRecurrenceRule.weekly(1,WmRecurrenceRule.MONDAY).withCount(1);
        check(count.equals(equal)&&equal.equals(count)&&count.hashCode()==equal.hashCode(),"value equality/hashCode");
        check(!count.equals(base)&&!count.equals(null)&&!count.equals("rule"),"equality distinguishes range/type/null");
        check(Modifier.isFinal(WmRecurrenceRule.class.getModifiers()),"immutable type final");
        for(Field field:WmRecurrenceRule.class.getFields())if(!Modifier.isStatic(field.getModifiers()))check(Modifier.isFinal(field.getModifiers())&&field.getType()==int.class,"public immutable scalar "+field.getName());
        invalid(()->WmRecurrenceRule.daily(0,false),"zero interval");
        invalid(()->WmRecurrenceRule.daily(2,true),"weekday mode cannot skip multiweek intervals");
        invalid(()->WmRecurrenceRule.weekly(-1,1),"negative interval");
        invalid(()->WmRecurrenceRule.weekly(1,0),"empty mask");
        invalid(()->WmRecurrenceRule.weekly(1,128),"unknown weekday bit");
        invalid(()->WmRecurrenceRule.monthlyDay(1,0),"zero monthday");
        invalid(()->WmRecurrenceRule.monthlyDay(1,32),"monthday32");
        invalid(()->WmRecurrenceRule.monthlyOrdinal(1,0,1),"zero ordinal");
        invalid(()->WmRecurrenceRule.monthlyOrdinal(1,6,1),"ordinal6");
        invalid(()->WmRecurrenceRule.yearlyOrdinal(1,-6,1),"ordinal minus6");
        invalid(()->WmRecurrenceRule.yearlyDay(4,31),"April31");
        invalid(()->WmRecurrenceRule.yearlyDay(2,30),"February30");
        invalid(()->WmRecurrenceRule.yearlyDay(13,1),"month13");
        invalid(()->base.withCount(0),"zero count");
        invalid(()->WmRecurrenceRule.none().withCount(1),"none count");
        invalid(()->WmRecurrenceRule.none().withUntil(),"none until");
        invalid(()->base.toRrule("20280101T000000Z"),"NEVER rejects supplied UNTIL");
        invalid(()->count.toRrule("20280101T000000Z"),"COUNT rejects supplied UNTIL");
        invalid(()->until.toRrule(null),"UNTIL requires caller UTC timestamp");
        check("FREQ=WEEKLY;BYDAY=MO;UNTIL=20000229".equals(until.toAllDayRrule("20000229")),"all-day date-valued UNTIL");
        invalid(()->until.toAllDayRrule("20000229T235959Z"),"all-day rejects timed UNTIL");
        invalid(()->until.toAllDayRrule("21000229"),"all-day century leap boundary");
        invalid(()->base.toAllDayRrule("20280101"),"all-day NEVER rejects supplied UNTIL");
        for(String accepted:new String[]{"00010101T000000Z","16000229T000000Z","24000229T120000Z","99991231T235959Z"})check(until.toRrule(accepted).endsWith(";UNTIL="+accepted),"strict Gregorian valid "+accepted);
        for(String rejected:new String[]{"19000229T000000Z","21000229T000000Z","00000101T000000Z","20260431T000000Z","20260100T000000Z","20261301T000000Z","20260101T240000Z","20260101T006000Z","20260101T000060Z","20260101","2026-01-01T00:00:00Z","20260101t000000z","20260101T000000+0900","２０２６0101T000000Z","20260101T000000Z;COUNT=2","20260101T000000Z\r\nEXDATE:20260102"," 20260101T000000Z"})invalid(()->until.toRrule(rejected),"strict UTC/injection reject "+rejected.replace('\n',' ').replace('\r',' '));
        minimumPeriodChecks();
        System.out.println("PASS "+passed+" pure JVM assertions; no provider, DST conversion, native pixel or recurrence expansion claim.");
    }
    private static void minimumPeriodChecks(){
        WmRecurrenceRule none=WmRecurrenceRule.none();
        check(none.minimumPeriodMinutes()==0,"NONE minimum is unlimited sentinel");
        none.validateAppointmentReminder(true,Long.MAX_VALUE);check(true,"NONE reminder bound bypass");
        none.validateAppointmentReminder(false,-1);check(true,"disabled raw reminder bypass");
        invalid(()->none.validateAppointmentReminder(true,-1),"enabled negative reminder boundary");
        try{WmRecurrenceRule.daily(1,false).validateAppointmentReminder(true,1440);throw new AssertionError("Expected native4205");}
        catch(IllegalArgumentException expected){check("저장할 수 없습니다. 미리 알림 시간은 이전 항목이 끝난 시간보다 뒤에 와야 합니다.".equals(expected.getMessage()),"native4205 exact message");}
        none.validateAppointmentDuration(1);
        none.validateAppointmentDuration(Long.MAX_VALUE);
        none.validateAppointmentDuration(0);check(true,"NONE zero duration accepted");
        invalid(()->none.validateAppointmentDuration(-1),"NONE negative duration");
        try{
            WmRecurrenceRule.daily(1,false).validateAppointmentDuration(86400001L);
            throw new AssertionError("Expected native4203 duration error");
        }catch(IllegalArgumentException expected){
            check("저장할 수 없습니다. 시작 날짜와 시간은 이전 항목이 끝난 시간보다 뒤에 와야 하고, 끝 날짜와 시간은 다음 항목이 시작되는 시간보다 앞에 와야 합니다.".equals(expected.getMessage()),"native resource4203 exact duration message");
        }
        minimum(WmRecurrenceRule.daily(3,false),4320,"daily3");
        minimum(WmRecurrenceRule.daily(1,true),1440,"weekdays include adjacent days");
        minimum(WmRecurrenceRule.weekly(1,WmRecurrenceRule.WEEKEND),1440,"weekly Sat/Sun crosses boundary");
        minimum(WmRecurrenceRule.weekly(2,WmRecurrenceRule.WEEKEND),8640,"biweekly Sat/Sun uses native same-week gap6");
        minimum(WmRecurrenceRule.weekly(3,WmRecurrenceRule.MONDAY),30240,"single weekday repeats every3weeks");
        minimum(WmRecurrenceRule.weekly(1,WmRecurrenceRule.SUNDAY|WmRecurrenceRule.FRIDAY),2880,"weekly Fri/Sun boundary gap2");
        minimum(WmRecurrenceRule.weekly(2,WmRecurrenceRule.SUNDAY|WmRecurrenceRule.FRIDAY),7200,"biweekly Sun/Fri native gap5");
        minimum(WmRecurrenceRule.weekly(2,WmRecurrenceRule.ALL),1440,"multiweek adjacent selections");
        minimum(WmRecurrenceRule.monthlyDay(2,31),80640,"monthday31 uses28day estimate");
        minimum(WmRecurrenceRule.monthlyOrdinal(3,-1,WmRecurrenceRule.WEEKDAYS),120960,"monthly last weekday uses28day estimate");
        minimum(WmRecurrenceRule.yearlyDay(2,29),483840,"yearly leap day uses336day estimate");
        minimum(WmRecurrenceRule.yearlyOrdinal(11,4,WmRecurrenceRule.THURSDAY),483840,"yearly ordinal uses336day estimate");
        minimum(WmRecurrenceRule.daily(Integer.MAX_VALUE,false),3092376451680L,"daily interval multiplication does not overflow int");
        minimum(WmRecurrenceRule.weekly(Integer.MAX_VALUE,WmRecurrenceRule.SATURDAY),21646635161760L,"weekly interval multiplication does not overflow int");
        minimum(WmRecurrenceRule.monthlyDay(Integer.MAX_VALUE,1),86586540647040L,"monthly interval multiplication does not overflow int");
        minimum(WmRecurrenceRule.monthlyOrdinal(Integer.MAX_VALUE,1,WmRecurrenceRule.ALL),86586540647040L,"ordinal interval multiplication does not overflow int");
    }
    private static void minimum(WmRecurrenceRule rule,long minutes,String name){
        check(rule.minimumPeriodMinutes()==minutes,"minimum "+name);
        WmRecurrenceRule[] ranges={rule,rule.withCount(1),rule.withUntil(),rule.withCount(999).withoutEnd()};
        for(WmRecurrenceRule ranged:ranges){
            check(ranged.minimumPeriodMinutes()==minutes,"minimum range invariant "+name);
            ranged.validateAppointmentReminder(true,minutes-1);check(true,"reminder below bound "+name);
            ranged.validateAppointmentReminder(true,0);check(true,"zero reminder below bound "+name);
            invalid(()->ranged.validateAppointmentReminder(true,minutes),"reminder equality "+name);
            invalid(()->ranged.validateAppointmentReminder(true,minutes+1),"reminder above bound "+name);
            invalid(()->ranged.validateAppointmentReminder(true,Long.MAX_VALUE),"unbounded reminder "+name);
            ranged.validateAppointmentReminder(false,Long.MAX_VALUE);check(true,"disabled reminder bound bypass "+name);
            ranged.validateAppointmentDuration(1);
            ranged.validateAppointmentDuration(minutes*60000L);
            invalid(()->ranged.validateAppointmentDuration(minutes*60000L+1),"minimum+1ms "+name);
            ranged.validateAppointmentDuration(0);check(true,"zero duration accepted "+name);
            invalid(()->ranged.validateAppointmentDuration(-1),"negative duration "+name);
            invalid(()->ranged.validateAppointmentDuration(Long.MAX_VALUE),"unbounded recurring duration "+name);
        }
    }
    private static void wire(WmRecurrenceRule rule,String expected){check(expected.equals(rule.toRrule(null)),"wire "+expected);}
    private static void invalid(Attempt attempt,String name){try{attempt.run();}catch(IllegalArgumentException expected){passed++;return;}throw new AssertionError("Expected IllegalArgumentException: "+name);}
    private static void check(boolean value,String name){if(!value)throw new AssertionError(name);passed++;}
}
