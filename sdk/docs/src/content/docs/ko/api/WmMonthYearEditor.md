---
title: "WmMonthYearEditor"
slug: ko/api/wmmonthyeareditor
---
[API 색인](/ko/api/)

```java
final class WmMonthYearEditor extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

package-private 인라인 연도 편집 구현체. [WmCalendarPicker](/ko/api/wmcalendarpicker/)가 관리한다. Enter/ESC/Back/blur는 모두 입력 확정을 시도하며 취소 rollback이 아니다. 범위 밖 초안은 이전 연도를 유지한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmMonthYearEditor.java`
