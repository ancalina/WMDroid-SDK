---
title: "WmRecurrenceChoice"
slug: ko/api/wmrecurrencechoice
---
[API 색인](/ko/api/)

```java
public final class WmRecurrenceChoice extends FrameLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

시작 날짜에 맞춘 한 번/매주/매월/매년 preset과 유형 편집 명령의 콤보. 팝업, 편집 마법사 수명은 호출자가 담당한다.

### 생성자

```java
public WmRecurrenceChoice(Context context,Typeface font,Locale locale,
                              WmDateTimeField.Value start,WmRecurrenceRule rule,int accent);
```

context/font/locale/start/rule은 필수(`IllegalArgumentException`). 시작에 일치하는 무기한 preset이면0..3, 그 외 규칙은 편집 항목4를 표시한다.

### choice

```java
public WmChoice choice();
```

소유된 WmChoice. 직접 commit할 때는 choose(index)를 사용하여 rule 변경과 callback을 함께 반영한다.

### rule

```java
public WmRecurrenceRule rule();
```

현재 불변 반복 값. 편집 명령을 눌러도 값은 그대로이다.

### startValue

```java
public WmDateTimeField.Value startValue();
```

소유자가 마지막 제공한 시작 값. 시각 초안이 미완성이면 새 시작 반영을 지연할 수 있다.

### setRule

```java
public void setRule(WmRecurrenceRule rule);
```

조용히 model을 교체하고 preset 문구/선택을 다시 계산한다. null은 `IllegalArgumentException`. rule 변경 callback을 호출하지 않는다.

### setStart

```java
public void setStart(WmDateTimeField.Value start);
```

시작 날짜, 시각만 바꾸고 preset 문구, 선택을 다시 계산합니다. rule()과 규칙 변경 callback은 유지합니다. 월요일 주간 규칙에서 시작일을 화요일로 바꾸면 규칙은 월요일로 유지되고 새 시작일 preset과 달라 편집 항목(index4)을 표시할 수 있습니다. null start는 IllegalArgumentException입니다.

### setLocale

```java
public void setLocale(Locale locale);
```

월/요일, preset 문구를 바꾼다. rule과 시작 값은 유지한다. null은 `IllegalArgumentException`.

### onWmLanguageChanged

```java
public void onWmLanguageChanged(String language);
```

공유 catalog 언어에 해당하는 Locale을 setLocale에 전달한다. 상태 보존, popup 처리는 setLocale 계약을 따른다.

### setOnRuleChanged

```java
public void setOnRuleChanged(OnRuleChangedListener listener);
```

preset commit으로 실제 규칙이 달라졌을 때 새 rule을 동기 전달한다. null로 해제한다.

### setOnEdit

```java
public void setOnEdit(Runnable callback);
```

index4 편집 선택을 처리하는 callback. 같은 항목을 반복 선택해도 실행한다. null이면 열지 않는다.

### choose

```java
public void choose(int index);
```

0:none,1:시작 요일의 매주,2:시작 일자의 매월,3:시작 월/일의 매년,4:edit callback. preset은 간격1/종료없음. 비활성은 무시하며 범위 밖은 `IndexOutOfBoundsException`. edit는 rule을 바꾸지 않는다.

### setEnabled

```java
public void setEnabled(boolean enabled);
```

자식 입력의 활성 상태를 함께 바꾼다. 비활성 중 사용자 변경을 차단한다.

### onWmRestyle

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor);
```

소유된 WmChoice에 style을 전달하고 배치를 요청한다.

## OnRuleChangedListener

```java
public interface OnRuleChangedListener
```

### changed

```java
void changed(WmRecurrenceRule rule);
```

preset commit으로 실제 규칙이 달라졌을 때 새 rule을 동기 전달한다. null로 해제한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmRecurrenceChoice.java`
