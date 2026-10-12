---
title: "WmFonts"
slug: ko/api/wmfonts
---
[API](/ko/api/)

글꼴 로드, 하이브리드 fallback, TextView 연결, Canvas 텍스트 측정을 제공합니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmFonts.java`

```java
public final class WmFonts
public static final String SYSTEM_SOURCE = "system:default";
public static final String DEFAULT_SOURCE = "fonts/gulim/Gulim-Regular.ttf";
```

## PreparedLine

```java
public static final class PreparedLine
public final float width;
```

## load(Context, String, boolean, boolean), family(...)

```java
@Deprecated public static boolean isCached(String source,boolean hybrid,boolean heavy);
public static boolean isCached(Context context,String source,boolean hybrid,boolean heavy);
public static Typeface load(Context c,String source,boolean hybrid,boolean heavy);
public static Typeface family(Context c,String id,String fallbackPath,boolean heavy);
```

source는 system:default, asset 경로 또는 WmFontFile URI입니다. system source는 DEFAULT, DEFAULT_BOLD를 반환합니다. 다른 source는 worker에서 준비하세요. 캐시 key는 app, 팩 revision, source, hybrid, heavy입니다. isCached(context,...)는 로드 없이 검사합니다. deprecated context 없는 overload는 항상 false입니다.

hybrid=true는 Tahoma를 우선 사용하고 없는 glyph에 선택 face를 사용합니다. heavy는 Tahoma Bold를 선택합니다. 선택 파일의 outline을 합성 bold로 바꾸지 않습니다. bold 역할에는 bold source를 전달하세요. Tahoma가 없으면 선택 face를 사용합니다. family는 fallbackPath에 hybrid=true로 load하는 이전 진입점이며 id는 캐시 구분에 사용하지 않습니다.

선택 asset이 없으면 DEFAULT_SOURCE 굴림을 시도하고 그마저 없으면 시스템 face입니다. prepare 실패는 시스템 face입니다. 준비 이후 가져온 source가 유효하지 않으면 `IllegalArgumentException`입니다.

## attach(TextView, Typeface)

```java
public static void attach(TextView view,Typeface face);
```

setTypeface 이후 main thread에서 호출하세요. 현재, 이후 텍스트에 필요한 fallback span을 적용하고 smoothing을 적용합니다. 다른 span, 선택, IME composition은 유지합니다. font를 바꾸면 다시 호출하세요.

## measure(Paint, String), ellipsize(String, Paint, float)

```java
public static float measure(Paint paint,String text);
public static String ellipsize(String text,Paint paint,float width);
```

같은 linked face로 너비를 계산하거나 END ellipsis 문자열을 반환합니다. width는 Paint 좌표의 픽셀입니다. Canvas draw와 동일한 font, textSize를 사용하세요.

## prepare(Paint, String), PreparedLine.draw(Canvas, float, float)

```java
public static PreparedLine prepare(Paint paint,String text);
public void draw(Canvas canvas,float x,float baseline);
```

paint, text가 null이면 `IllegalArgumentException`입니다. Paint, text의 snapshot을 만들고 width를 저장합니다. 반복 draw는 다시 측정하지 않습니다. x는 Paint.Align에 따른 기준, baseline은 text baseline입니다. font, 내용, 크기가 바뀌면 새 PreparedLine을 준비하세요. draw는 내부 Paint 상태를 바꾸므로 같은 객체를 동시에 그리지 마세요.

## draw(...), drawCenteredListCaption(...)

```java
public static void draw(Canvas canvas,String text,float x,float baseline,Paint paint);
public static void drawCenteredListCaption(Canvas canvas,String text,RectF bounds,Paint paint);
```

draw는 linked face, shaping을 적용합니다. x, baseline은 Canvas 좌표입니다. centered caption은 bounds 안 중앙 정렬, 초과 시 왼쪽 정렬과 ASCII ...를 적용합니다. prefix는 최대 259 UTF-16 단위이며 code point를 분리하지 않습니다. 빈 bounds는 그리지 않습니다. null caption drawing 인자는 `IllegalArgumentException`입니다.

## centerBaseline(...), textBounds(...)

```java
public static float centerBaseline(Paint paint,float top,float bottom,String text);
public static void textBounds(Paint paint,String text,Rect ink);
```

centerBaseline은 실제 ink 기준의 중앙 baseline을 반환합니다. 빈 text는 font metrics를 사용합니다. textBounds는 caller의 Rect를 비운 뒤 각 linked-face run을 원점에서 측정한 ink의 합집합으로 교체합니다. run의 가로 advance를 더하지 않으므로 전체 문자열을 그린 가로 경계가 아닙니다. 전체 advance는 measure 또는 PreparedLine.width를 사용하세요. textBounds의 paint, text, ink null은 `IllegalArgumentException`입니다. centerBaseline의 text null 또는 빈 text와 paint null은 `NullPointerException`입니다. 비어 있지 않은 text에서 paint null은 `IllegalArgumentException`입니다.

## smoothing

```java
public static void setSmoothing(boolean enabled);
public static boolean isSmoothing();
public static void applySmoothingLayer(android.view.View view);
public static void applySmoothing(TextView view);
```

setSmoothing은 process 전체 정책이며 기본 true입니다. 기존 View에는 applySmoothing 또는 restyle을 적용하세요. API 18 미만의 off 모드는 applySmoothingLayer가 software surface를 사용하고 다시 on이면 기존 layer를 복원합니다. off는 text AA 정책이며 원본 bitmap font의 픽셀 모양을 보장하지 않습니다.
