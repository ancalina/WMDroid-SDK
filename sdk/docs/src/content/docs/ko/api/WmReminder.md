---
title: "WmReminder"
slug: ko/api/wmreminder
---
[API 색인](/ko/api/)

```java
public final class WmReminder
```

상대 약속 알림의 불변 값이다. 알림 예약은 호출자가 수행한다. 단위 변경은 숫자를 유지하므로 총 분 수가 달라진다.

### 상수와 필드

```java
public static final int MINUTES = 0;
public static final int HOURS = 1;
public static final int DAYS = 2;
public static final int WEEKS = 3;
public static final int MAX_MINUTES = 9999 * 10080;
public final boolean enabled;
public final int quantity;
public final int unit;
```

`MINUTES=0`, `HOURS=1`, `DAYS=2`, `WEEKS=3`. `MAX_MINUTES=9999*10080` (100789920분). `enabled`는 알림 사용 여부, `quantity`는 0..9999, `unit`은 단위이다.

### 생성자

```java
public WmReminder(boolean enabled,int quantity,int unit);
```

수량 0..9999와 단위 0..3을 요구한다. 범위 밖이면 `IllegalArgumentException`. 비활성 값도 같은 검증을 적용한다.

### minutes

```java
public int minutes();
```

단위별 배수 1/60/1440/10080을 곱해 분을 반환한다. `enabled=false`여도 계산값을 반환한다.

### withEnabled

```java
public WmReminder withEnabled(boolean enabled);
```

수량, 단위를 유지한 새 값을 반환한다.

### withUnit

```java
public WmReminder withUnit(int unit);
```

수량, 사용 여부를 유지한 새 값을 반환한다. 총 분 수 보존 변환이 아니다. 잘못된 단위는 `IllegalArgumentException`.

### fromMinutes

```java
public static WmReminder fromMinutes(boolean enabled,int minutes);
```

분을 정확히 표현하는 가장 큰 단위를 선택한다. 0은 0주가 된다. 음수 또는 어느 단위로도 4자리 수량에 정확히 표현할 수 없으면 `IllegalArgumentException`.

### candidates

```java
public static String[] candidates(int unit);
```

새 후보 배열을 반환한다. 분: 1,5,10,15,30,45; 시간/일: 1..6; 주: 1..3. 단위가 0..3 밖이면 `IllegalArgumentException`.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmReminder.java`
