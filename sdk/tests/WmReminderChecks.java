package me.ancal.wmdroid.sdk;

/** Independent native reminder examples; no Android/provider/scheduling claim. */
public final class WmReminderChecks {
    private static int passed;
    private interface Attempt {void run();}
    public static void main(String[] args){
        int[] factors={1,60,1440,10080};
        String[][] candidates={{"1","5","10","15","30","45"},{"1","2","3","4","5","6"},{"1","2","3","4","5","6"},{"1","2","3"}};
        for(int unit=0;unit<4;unit++){
            final int nativeUnit=unit;
            WmReminder reminder=new WmReminder(true,7,unit);
            check(reminder.minutes()==7*factors[unit],"factor unit"+unit);
            check(new WmReminder(false,0,unit).minutes()==0,"zero accepted unit"+unit);
            check(new WmReminder(true,9999,unit).minutes()==9999*factors[unit],"four-digit maximum unit"+unit);
            invalid(()->new WmReminder(true,-1,nativeUnit),"negative quantity unit"+unit);
            invalid(()->new WmReminder(true,10000,nativeUnit),"five-digit quantity unit"+unit);
            for(int next=0;next<4;next++){
                WmReminder changed=reminder.withUnit(next);
                check(changed.enabled&&changed.quantity==7&&changed.unit==next&&changed.minutes()==7*factors[next],"unit change keeps quantity "+unit+"->"+next);
            }
            WmReminder disabled=reminder.withEnabled(false);
            check(!disabled.enabled&&disabled.quantity==7&&disabled.unit==unit&&disabled.minutes()==reminder.minutes(),"disable retains offset unit"+unit);
            check(disabled.withEnabled(true).enabled,"reenable unit"+unit);
            check(reminder.enabled&&reminder.unit==unit&&reminder.quantity==7,"source immutable unit"+unit);
            String[] first=WmReminder.candidates(unit);
            check(java.util.Arrays.equals(first,candidates[unit]),"native candidate order unit"+unit);
            first[0]="mutated";
            check(java.util.Arrays.equals(WmReminder.candidates(unit),candidates[unit]),"candidate defensive copy unit"+unit);
        }
        converted(0,0,3);converted(1,1,0);converted(59,59,0);converted(60,1,1);
        converted(90,90,0);converted(1440,1,2);converted(10080,1,3);converted(20160,2,3);
        converted(9999,9999,0);converted(599940,9999,1);converted(14398560,9999,2);
        converted(100789920,9999,3);
        check(WmReminder.MAX_MINUTES==100789920,"native maximum9999weeks");
        for(int value:new int[]{-1,Integer.MIN_VALUE,10000,10081,100789921,Integer.MAX_VALUE}){
            invalid(()->WmReminder.fromMinutes(true,value),"unrepresentable minutes "+value);
            invalid(()->WmReminder.fromMinutes(false,value),"disabled unrepresentable minutes "+value);
        }
        invalid(()->new WmReminder(true,1,-1),"negative unit");
        invalid(()->new WmReminder(true,1,4),"unit4");
        invalid(()->new WmReminder(true,1,0).withUnit(4),"unit change bounds");
        invalid(()->WmReminder.candidates(-1),"negative candidate unit");
        invalid(()->WmReminder.candidates(4),"candidate unit4");
        System.out.println("PASS "+passed+" pure JVM reminder assertions; no provider, scheduling or native runtime claim.");
    }
    private static void converted(int minutes,int quantity,int unit){
        for(boolean enabled:new boolean[]{false,true}){
            WmReminder reminder=WmReminder.fromMinutes(enabled,minutes);
            check(reminder.enabled==enabled&&reminder.quantity==quantity&&reminder.unit==unit&&reminder.minutes()==minutes,"largest exact unit "+minutes+" enabled="+enabled);
        }
    }
    private static void invalid(Attempt attempt,String name){try{attempt.run();}catch(IllegalArgumentException expected){passed++;return;}throw new AssertionError("Expected IllegalArgumentException: "+name);}
    private static void check(boolean value,String name){if(!value)throw new AssertionError(name);passed++;}
}
