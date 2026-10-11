package me.ancal.wmdroid.sdk;

/** Fixed elapsed duration contract only; no CalendarProvider or DST runtime. */
public final class WmCalendarDurationChecks {
    private static int passed;
    private interface Attempt {void run();}
    public static void main(String[] args){
        equal(1209600000L,WmCalendarDuration.parseRfcMillis("P2W"),"two weeks");
        equal(172800000L,WmCalendarDuration.parseRfcMillis("P2D"),"two days");
        equal(93784000L,WmCalendarDuration.parseRfcMillis("P1DT2H3M4S"),"day and time components");
        equal(3600000L,WmCalendarDuration.parseRfcMillis("+PT1H"),"explicit positive sign");
        equal(90000L,WmCalendarDuration.parseRfcMillis("PT90S"),"units need not be clock-normalized");
        equal(0,WmCalendarDuration.parseRfcMillis("PT0S"),"zero timed duration");
        equal(3600000L,WmCalendarDuration.parseProviderMillis("P3600S"),"legacy provider seconds");
        equal(3600000L,WmCalendarDuration.parseProviderMillis("PT3600S"),"canonical provider seconds");
        invalid(()->WmCalendarDuration.parseRfcMillis("P3600S"),"legacy not RFC");
        check("PT3600S".equals(WmCalendarDuration.timed(3600000L)),"canonical timed output");
        check("PT0S".equals(WmCalendarDuration.timed(0)),"exact zero output");
        check("P2D".equals(WmCalendarDuration.allDay(2)),"positive all-day output");
        invalid(()->WmCalendarDuration.timed(1001),"no fractional-second rounding");
        invalid(()->WmCalendarDuration.timed(-1000),"negative timed output");
        invalid(()->WmCalendarDuration.allDay(0),"zero all-day output");
        invalid(()->WmCalendarDuration.allDay(-1),"negative all-day output");
        for(String bad:new String[]{"","P","PT","P1DT","-PT1S","PT-1S","PT1.5S","P1W1D","P1WT1H","PT1S1M","PT1M1H","P1D1D","P1Y","P1M"," PT1S","PT1S\n","pt1s","P9223372036854775808D","PT9223372036854776S","P106751991167DT7H12M56S"}){
            invalid(()->WmCalendarDuration.parseRfcMillis(bad),"invalid RFC "+bad.replace('\n',' '));
            invalid(()->WmCalendarDuration.parseProviderMillis(bad),"invalid provider "+bad.replace('\n',' '));
        }
        invalid(()->WmCalendarDuration.parseProviderMillis(null),"null duration");
        invalid(()->WmCalendarDuration.parseProviderMillis("P9223372036854776S"),"legacy seconds overflow");
        long largestWholeSeconds=9223372036854775000L;
        equal(largestWholeSeconds,WmCalendarDuration.parseRfcMillis("PT9223372036854775S"),"largest representable whole-second duration");
        check("PT9223372036854775S".equals(WmCalendarDuration.timed(largestWholeSeconds)),"large output no narrowing");
        equal(42,WmCalendarDuration.eventEnd(100,Long.valueOf(42),"bad"),"explicit DTEND wins even invalid duration");
        equal(0,WmCalendarDuration.eventEnd(100,Long.valueOf(0),null),"explicit zero DTEND is present");
        equal(86401000L,WmCalendarDuration.eventEnd(1000,null,"P1D"),"absent DTEND uses fixed elapsed day");
        equal(3601000L,WmCalendarDuration.eventEnd(1000,null,"P3600S"),"event adapter reads legacy duration");
        equal(-1000,WmCalendarDuration.eventEnd(-2000,null,"PT1S"),"pre-epoch begin remains signed");
        equal(Long.MAX_VALUE,WmCalendarDuration.eventEnd(Long.MAX_VALUE-1000,null,"PT1S"),"event end exact long boundary");
        invalid(()->WmCalendarDuration.eventEnd(Long.MAX_VALUE-999,null,"PT1S"),"event end addition overflow");
        invalid(()->WmCalendarDuration.eventEnd(0,null,null),"missing both end and duration");
        equal(120,WmCalendarDuration.parseEditorMinutes("2",java.util.Locale.KOREAN),"native bare number means hours");
        equal(135,WmCalendarDuration.parseEditorMinutes("2.25시간",java.util.Locale.KOREAN),"quarter-hour localized input adaptation");
        equal(90,WmCalendarDuration.parseEditorMinutes("1,5 hours",java.util.Locale.GERMAN),"locale decimal separator");
        equal(1,WmCalendarDuration.parseEditorMinutes("1.9 min",java.util.Locale.US),"native whole-minute truncation");
        equal(94370400,WmCalendarDuration.parseEditorMinutes("999999999 weeks",java.util.Locale.US),"native duration cap");
        for(String zero:new String[]{"0 min","0 hours","0 days","0 weeks","0 분","0.001 min","0.001 시간"})
            equal(0,WmCalendarDuration.parseEditorMinutes(zero,java.util.Locale.KOREAN),"native explicit-unit zero "+zero);
        String[][] units={{"min","mins","minute","minutes","minute(s)","m","분"},
            {"hour","hours","hr","hrs","hour(s)","h","시간"},
            {"day","days","day(s)","d","일"},{"week","weeks","wk","wks","week(s)","w","주"}};
        int[] factors={1,60,1440,10080};
        for(int i=0;i<units.length;i++)for(String unit:units[i])
            equal(2*factors[i],WmCalendarDuration.parseEditorMinutes(" 2 "+unit.toUpperCase(java.util.Locale.US)+" ",java.util.Locale.US),"ROM alias "+unit);
        for(String bad:new String[]{"","0","-2 hours","NaN hours","Infinity hours","2 months","2 h junk","2 h 3 min","1,5 hours","1e3 hours"})
            invalid(()->WmCalendarDuration.parseEditorMinutes(bad,java.util.Locale.US),"invalid editor input "+bad);
        invalid(()->WmCalendarDuration.parseEditorMinutes(null,java.util.Locale.US),"null editor input");
        invalid(()->WmCalendarDuration.parseEditorMinutes("2",null),"null editor locale");
        invalid(()->WmCalendarDuration.parseEditorMinutes(new String(new char[256]).replace('\0','1'),java.util.Locale.US),"native255-character limit");
        System.out.println("PASS "+passed+" pure JVM duration assertions; no provider/DST runtime claim.");
    }
    private static void equal(long expected,long actual,String name){check(expected==actual,name+" expected="+expected+" actual="+actual);}
    private static void invalid(Attempt attempt,String name){try{attempt.run();}catch(IllegalArgumentException expected){passed++;return;}throw new AssertionError("Expected IllegalArgumentException: "+name);}
    private static void check(boolean condition,String name){if(!condition)throw new AssertionError(name);passed++;}
}
