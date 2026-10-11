package me.ancal.wmdroid.sdk;

/** Native subject save-boundary examples; no Android/provider runtime claim. */
public final class WmAppointmentTextChecks {
    private static int passed;
    private interface Attempt {void run();}
    public static void main(String[] args){
        same("제목 없음",WmAppointmentText.subjectForSave("","제목 없음"),"empty Korean fallback");
        same("No subject",WmAppointmentText.subjectForSave("","No subject"),"empty authored English fallback");
        same(" \t\n",WmAppointmentText.subjectForSave(" \t\n","No subject"),"whitespace is not trimmed");
        same("회의 café \uD83D\uDE00",WmAppointmentText.subjectForSave("회의 café \uD83D\uDE00","제목 없음"),"ordinary Unicode retained");
        same("Subject",WmAppointmentText.subjectForSave("Subject","No subject"),"ordinary subject retained");
        same("제목 없음",WmAppointmentText.subjectForSave("\0ignored","제목 없음"),"initial NUL selects Korean fallback");
        same("No subject",WmAppointmentText.subjectForSave("\0ignored","No subject"),"initial NUL selects English fallback");
        same("회의 \uD83D\uDE00",WmAppointmentText.subjectForSave("회의 \uD83D\uDE00\0ignored","No subject"),"internal UTF-16 NUL terminates subject");
        same("first",WmAppointmentText.subjectForSave("first\0second\0third","No subject"),"only first NUL matters");
        same(" ",WmAppointmentText.subjectForSave(" \0ignored","No subject"),"nonempty whitespace prefix retained");
        same(" ",WmAppointmentText.subjectForSave(""," "),"nonempty whitespace fallback permitted");
        invalid(()->WmAppointmentText.subjectForSave(null,"No subject"),"null subject");
        invalid(()->WmAppointmentText.subjectForSave("",null),"null fallback");
        invalid(()->WmAppointmentText.subjectForSave("",""),"empty fallback");
        invalid(()->WmAppointmentText.subjectForSave("","\0No subject"),"initial NUL fallback");
        invalid(()->WmAppointmentText.subjectForSave("","No\0subject"),"internal NUL fallback");
        invalid(()->WmAppointmentText.subjectForSave("Existing","bad\0fallback"),"unused fallback still validated");
        System.out.println("PASS "+passed+" pure JVM appointment subject assertions; no Provider runtime claim.");
    }
    private static void same(String expected,String actual,String label){if(!expected.equals(actual))throw new AssertionError(label+": "+actual);passed++;}
    private static void invalid(Attempt attempt,String label){try{attempt.run();}catch(IllegalArgumentException expected){passed++;return;}throw new AssertionError("Expected IllegalArgumentException: "+label);}
}
