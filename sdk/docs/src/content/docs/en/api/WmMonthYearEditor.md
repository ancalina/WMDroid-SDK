---
title: "WmMonthYearEditor"
slug: en/api/wmmonthyeareditor
---
[API index](/en/api/)

```java
final class WmMonthYearEditor extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

Package-private inline year editor managed by [WmCalendarPicker](/en/api/wmcalendarpicker/). Enter/ESC/Back/blur all attempt commit rather than rollback. Out-of-range drafts retain the previous year.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmMonthYearEditor.java`
