---
title: "WmIconOrder"
slug: en/api/wmiconorder
---
[API index](/en/api/)

```java
final class WmIconOrder
```

Internal icon-list insertion comparator. Both the class and compareForInsert are package-private, not external SDK APIs. This reference explains settings-caption ordering.

## Insertion comparison

```java
static int compareForInsert(String existing,String incoming)
```

Compares at most 260 incoming UTF-16 code units using a fixed lowercase table. Missing existing units compare as NUL. Returns converted existing-incoming at the first difference. If both values are NUL, stops immediately and returns zero without comparing later units. Otherwise compares through the incoming length or 260 UTF-16 units and returns zero if that range matches. Equal incoming captions and incoming prefixes insert before existing captions. This is not a locale Collator or modern Unicode case folding; it is an insertion relation, not a general Comparator with interchangeable arguments. Null inputs are not handled.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmIconOrder.java`
