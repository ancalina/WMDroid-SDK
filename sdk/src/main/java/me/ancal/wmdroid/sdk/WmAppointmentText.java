package me.ancal.wmdroid.sdk;

/** Appointment text at the native save boundary; editing retains the raw draft. */
public final class WmAppointmentText {
    private WmAppointmentText() {}

    /** Native UTF-16 strings terminate at NUL; only an empty subject uses resource4209. */
    public static String subjectForSave(String subject,String noSubject) {
        if(subject==null||noSubject==null||noSubject.isEmpty()||noSubject.indexOf('\0')>=0)
            throw new IllegalArgumentException("Subject and nonempty fallback required");
        int end=subject.indexOf('\0');
        String text=end<0?subject:subject.substring(0,end);
        return text.isEmpty()?noSubject:text;
    }
}
