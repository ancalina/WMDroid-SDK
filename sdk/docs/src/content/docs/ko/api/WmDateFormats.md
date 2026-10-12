---
title: "WmDateFormats"
slug: ko/api/wmdateformats
---
[API 색인](/ko/api/)

```java
final class WmDateFormats
```

package-private 날짜 형식 구현체. 직접 사용할 수 없다. [WmDateField](/ko/api/wmdatefield/)와 [WmCalendarHeader](/ko/api/wmcalendarheader/)가 사용한다. 짧은 날짜는 언어 `ko`이고 국가가 비어 있거나 `KR`이면 `yy-MM-dd`, 언어 `en`이고 국가가 비어 있거나 `US`이면 `M/d/yy`이다. 나머지 Locale은 Android의 SHORT 날짜 형식을 사용한다. 원본 era 예외, 다른 지역 override는 재현하지 않는다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmDateFormats.java`
