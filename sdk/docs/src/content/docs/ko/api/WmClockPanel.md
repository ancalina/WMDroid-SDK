---
title: "WmClockPanel"
slug: ko/api/wmclockpanel
---
[API 색인](/ko/api/)

```java
public class WmClockPanel extends LinearLayout implements WmControls.RestyleListener
```

날짜, 시각, 배터리, 소유 앱이 제공한 일정 두 줄을 표시하는 읽기 전용 팝업 본문. 팝업 틀, 닫기, 시간 제한은 소유 앱이 담당한다. 자체 타이머나 일정 조회 없음. 좌표와 텍스트 크기는 논리 픽셀.

## 생성자

```java
public WmClockPanel(Context context, Typeface regular, String language)
```

언어를 WmStrings로 해석하고 현재 날짜, 시각, 배터리를 읽는다. 본문은 일반 글꼴 24px, 바깥 여백 8px. 초기 일정 제목은 “일정 연결 필요”, 시각은 빈 문자열.

## refreshClock / time

```java
public final void refreshClock()
protected final String time(long millis)
```

refreshClock은 시스템 시각, 시간대, 12/24시간 설정과 배터리 sticky broadcast를 다시 읽고 이미지, 접근성 설명을 갱신한다. 일정 내용은 유지. time은 Unix epoch 밀리초를 현재 시간대, 언어로 포맷한다. 24시간은 HH:mm, 12시간은 a h:mm.

## setAppointment

```java
public final void setAppointment(String title, String when)
```

제목과 일정 시각을 그대로 표시한다. 날짜 파싱, 일정 저장 없음. null은 해당 TextView를 비운다.

## language / text

```java
public final void language(String value)
protected final String text(String authored)
```

language는 언어를 해석하고 시계, 배터리 설명을 즉시 갱신한다. 이미 설정한 일정 내용은 번역하지 않는다. text는 현재 언어 카탈로그에서 SDK 작성 문자열을 조회한다.

## batteryIcon

```java
public static String batteryIcon(int percent, boolean external, boolean charging)
```

`WmIcons.load(context, key)`에 전달하는 String 카탈로그 키를 반환합니다. Android 정수 resource ID가 아닙니다. 외부 전원에서는 charging=true면 battery_charge, 아니면 battery_full. 배터리 전원에서는 0..20 critical, 21..40 low, 41..60 half, 61..80 medium, 81..100 full. 범위 밖 값은 full. 접두어는 모두 battery_.

## 스타일, 부착

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
protected void onAttachedToWindow()
```

리소스 revision이 바뀌었을 때 restyle은 refreshClock을 호출한다. 글꼴, 색 인수는 이 구현에서 직접 적용하지 않는다. 부착 시 시계 갱신. 반복 갱신은 소유 앱이 예약해야 한다.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmClockPanel.java`
