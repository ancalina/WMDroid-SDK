---
title: "WmPwiView"
slug: ko/api/wmpwiview
---
[API 색인](/ko/api/)

```java
public final class WmPwiView extends View implements WmControls.LanguageListener
```

읽기 전용 binary PWI 텍스트 뷰. 복구한 줄바꿈/좌표 계산에 Android 글꼴 측정을 사용하며 저장 줄 cache를 사용하지 않는다. 소유 앱은 기기 DPI, scale, break profile과 저장 폰트의 명시적 매핑을 제공한다.

## Profile

```java
public static final class Profile
public final int mode, dpiX, dpiY, documentScale;
public final boolean extendedBreaks, gridTextAdjustment;
public Profile(int mode, int dpiX, int dpiY, int documentScale, boolean extendedBreaks, boolean gridTextAdjustment)
```

dpiX/dpiY/documentScale은 양수, DPI×scale이 signed int 범위 안이며 native unit0 계수가 양수여야 한다; 위반 시 `IllegalArgumentException`. mode는 native view mode로 별도 범위 clamp 없음. extendedBreaks는 text wrap과 정상 font scale 260/240 선택. gridTextAdjustment는 mode0 baseline +1과 grid font cap -1 보정에 사용. DPI는 런처 화면 density에서 자동 추정하지 않는다.

## FontResolver

```java
public interface FontResolver {
    Typeface resolve(WmPwiDocument.Font font, WmPwiDocument.CharacterFormat format) throws IOException;
}
```

저장 faceName과 전체 weight/italic 상태에 맞는 실제 Typeface 반환. null은 기본 폰트 대체가 아니라 IOException. 매핑 실패는 IOException으로 전달한다. 생성자에서 필요한 style을 해결하며 동일 style은 캐시한다.

## 생성자

```java
public WmPwiView(Context context, WmPwiDocument document, Profile profile, FontResolver resolver) throws IOException
```

document/profile/resolver non-null 필수, 위반 시 `IllegalArgumentException`. 일반 PWI 저장형, bullet, 객체/첨부, 확장 서식, highlight, 알 수 없는 underline operand, 미지원 색 index, 미해결 font/size, zero-height/overflow font는 IOException. underline/strike는 Android decoration. 글꼴을 조용히 바꾸거나 문서를 평문으로 대체하지 않는다. 생성자/reflow는 UI 스레드에서 수행하며 문서, profile은 뷰 수명 동안 고정.

## Layout state

```java
public WmPwiDocument getDocument()
public Profile getProfile()
public int getLineCount()
public int getContentHeight()
public String getLayoutError()
```

document/profile은 제공한 객체 반환. lineCount는 마지막 성공 reflow에서 그릴 줄 수, contentHeight는 그리기 내용의 Android px 높이; reflow 전/실패 시 0. layoutError는 성공 시 null, 실패 시 오류 메시지.

## 언어

```java
public void onWmLanguageChanged(String language)
```

`pwi.layout_error` 키로 오류 content description을 갱신한다. `WmControls.localizeTree`에서 호출한다. 문서 텍스트, `getLayoutError()`는 유지하며 reflow하지 않는다. reflow 성공 시 content description을 문서 텍스트로 복원한다. UI 스레드에서 호출한다.

## reflow and drawing

```java
public void reflow(int width) throws IOException
protected void onMeasure(int widthSpec,int heightSpec)
protected void onDraw(Canvas canvas)
```

width는 비음수 Android px; 음수는 `IllegalArgumentException`. native 폭으로 변환, 재측정, 줄바꿈, 그리기 run을 다시 구성. 좌표/높이 overflow, 측정/format 오류는 IOException이며 기존 그리기 목록도 비우고 height=0/error를 설정한 뒤 예외 전달. 폭이 바뀌면 onMeasure가 자동 reflow; 이 경로는 IOException을 저장하고 빈 화면으로 측정한다. onDraw는 clip에 보이는 준비된 run만 그리며 글꼴 측정/줄바꿈을 하지 않는다. 같은 폭의 자동 측정은 기존 실패 상태도 캐시하므로 재시도는 명시적으로 reflow 호출.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmPwiView.java`
