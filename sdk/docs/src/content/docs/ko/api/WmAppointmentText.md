---
title: "WmAppointmentText"
slug: ko/api/wmappointmenttext
---
[API 색인](/ko/api/)

```java
public final class WmAppointmentText
```

약속 저장 경계에서 제목의 NUL 종료와 빈 제목 대체를 처리한다. 편집 중인 원문은 호출자가 보존한다.

### subjectForSave

```java
public static String subjectForSave(String subject,String noSubject);
```

첫 NUL 이전 텍스트를 반환한다. 결과가 빈 문자열이면 `noSubject`를 반환한다. 공백만 있는 제목은 그대로 유지한다. `subject` 또는 `noSubject`가 null이거나, 대체 제목이 비어 있거나 NUL을 포함하면 `IllegalArgumentException`.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmAppointmentText.java`
