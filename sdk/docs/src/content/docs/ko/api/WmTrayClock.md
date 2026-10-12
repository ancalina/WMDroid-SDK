---
title: "WmTrayClock"
slug: ko/api/wmtrayclock
---
[API 색인](/ko/api/)

```java
public final class WmTrayClock extends View
```

제목줄용 디지털/아날로그 시계. 부모가 제목줄 전체를 배율 변환한다. 자체 타이머, 팝업 없음; 호스트가 refresh를 호출한다.

## 생성자

```java
public WmTrayClock(Context context, Typeface regular, int pressedTextColor)
```

일반 글꼴 24 논리 px, 기본 흰색 디지털 시계 생성 후 현재 시각 갱신. pressedTextColor는 디지털 pressed/selected 색. 기본 언어 ko, 키보드 포커스 가능.

## language / refresh

```java
public void language(String language)
public void refresh(Date now, boolean analog)
```

language는 다음 refresh에 사용할 언어만 저장한다. refresh는 non-null Date, analog 모드를 적용하고 invalidate; 자연 폭이 바뀌면 requestLayout. 현재 시스템 시간대, 12/24시간 설정 사용(HH:mm 또는 h:mm, AM/PM 없음). ko/en은 숫자 포맷 Locale.KOREAN, 다른 언어는 카탈로그 locale. 아날로그 폭40px, 디지털은 측정 문자 폭 올림+6px. 분침은 5분 단위, 시침은 정시 단위 정수 테이블 사용. 접근성 설명에는 시각과 모드 포함.

## 측정, 그리기

```java
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
protected void drawableStateChanged()
protected void onDraw(Canvas canvas)
```

자연 폭과 높이52px를 부모 measure spec으로 제한한다. 상태 변경은 다시 그리기. 디지털은 글리프 경계 기준 중앙 정렬. 아날로그는 흰색 링, 바늘; 원본의 정확한 펜 raster, 런타임 바늘 색은 미확정.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmTrayClock.java`
