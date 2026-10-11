package me.ancal.wmdroid.sdk;

/** Relative appointment reminder; no provider or scheduling policy.
 * Native input permits four digits, including zero. Unit changes preserve
 * quantity, not total minutes. Initial minute values must round-trip exactly. */
public final class WmReminder {
    public static final int MINUTES=0,HOURS=1,DAYS=2,WEEKS=3;
    public static final int MAX_MINUTES=9999*10080;
    private static final int[] FACTORS={1,60,1440,10080};
    public final boolean enabled;
    public final int quantity,unit;
    public WmReminder(boolean enabled,int quantity,int unit){
        if(quantity<0||quantity>9999||unit<0||unit>3)throw new IllegalArgumentException("Invalid relative reminder");
        this.enabled=enabled;this.quantity=quantity;this.unit=unit;
    }
    public int minutes(){return quantity*FACTORS[unit];}
    public WmReminder withEnabled(boolean enabled){return new WmReminder(enabled,quantity,unit);}
    public WmReminder withUnit(int unit){return new WmReminder(enabled,quantity,unit);}
    /** Largest exact native unit; zero selects weeks, as the native converter.
     * Reject nonrepresentable initial values instead of truncating their text. */
    public static WmReminder fromMinutes(boolean enabled,int minutes){
        if(minutes<0)throw new IllegalArgumentException("Negative reminder");
        for(int unit=3;unit>=0;unit--)if(minutes%FACTORS[unit]==0){
            int quantity=minutes/FACTORS[unit];
            if(quantity<=9999)return new WmReminder(enabled,quantity,unit);
        }
        throw new IllegalArgumentException("Reminder cannot be represented by native four-digit input");
    }
    /** Native per-unit candidate order; arbitrary four-digit values also work. */
    public static String[] candidates(int unit){
        switch(unit){
            case MINUTES:return new String[]{"1","5","10","15","30","45"};
            case HOURS:case DAYS:return new String[]{"1","2","3","4","5","6"};
            case WEEKS:return new String[]{"1","2","3"};
            default:throw new IllegalArgumentException("Invalid reminder unit");
        }
    }
}
