---
title: "WmAppointmentText"
slug: en/api/wmappointmenttext
---
[API index](/en/api/)

```java
public final class WmAppointmentText
```

Applies NUL termination and empty-subject fallback at the appointment save boundary. Keep the editing draft separately.

### subjectForSave

```java
public static String subjectForSave(String subject,String noSubject);
```

Returns text before the first NUL, or `noSubject` if that text is empty. Preserves whitespace-only subjects. Throws `IllegalArgumentException` for null arguments, an empty fallback, or a fallback containing NUL.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmAppointmentText.java`
