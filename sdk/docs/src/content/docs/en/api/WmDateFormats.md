---
title: "WmDateFormats"
slug: en/api/wmdateformats
---
[API index](/en/api/)

```java
final class WmDateFormats
```

Package-private date-format implementation; not directly callable. Used by [WmDateField](/en/api/wmdatefield/) and [WmCalendarHeader](/en/api/wmcalendarheader/). Short dates use `yy-MM-dd` when language is `ko` and country is empty or `KR`, and `M/d/yy` when language is `en` and country is empty or `US`. Other Locales use Android SHORT date format. Native era exceptions/regional overrides are not reproduced.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmDateFormats.java`
