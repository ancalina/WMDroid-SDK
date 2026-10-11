---
title: "WmPwiTextLayout"
slug: ko/api/wmpwitextlayout
---
[API 색인](/ko/api/)

```java
public final class WmPwiTextLayout
```

PWI 텍스트 토큰의 native 줄바꿈. 소유 앱의 측정 콜백을 사용하므로 Android/GDI 글꼴 결과는 그 콜백에 따른다. 좌표·폭은 native 논리 단위.

## Measurer and Measurement

```java
public interface Measurer {
    Measurement measure(String text, WmPwiDocument.CharacterFormat format, int availableWidth) throws IOException;
}
public static final class Measurement
public final int fittedCharacters, width, ascent, descent;
public Measurement(int fittedCharacters, int width, int ascent, int descent)
```

콜백은 text prefix 중 들어가는 UTF-16 코드 단위 수와 그 폭·ascent/descent 반환. fittedCharacters는 0..text.length, ascent/descent는 0..255. availableWidth가 `Integer.MAX_VALUE`면 무제한 측정, 음수도 전달될 수 있다. 폭은 ≥0; 예외는 availableWidth<0·fit=0·width=availableWidth인 native empty fit. 생성자는 검증하지 않지만 layout은 null/잘못된 결과에 `IOException`. fit=0 허용; 줄 진행은 layout이 별도로 보장한다. 측정 콜백의 IOException은 그대로 전달된다.

## layout and Line

```java
public static List<Line> layout(WmPwiDocument.Paragraph paragraph, int mode, int frameLeft, int frameWidth, boolean extendedBreaks, Measurer measurer) throws IOException
public static final class Line
public final int logicalStart, logicalCount, width, ascent, descent;
```

paragraph/measurer non-null·frameWidth≥0 필요, 위반 시 `IllegalArgumentException`. 반환 목록은 수정 불가. Line은 논리 토큰 범위·측정 폭·최대 ascent/descent; 보이지 않는 native 토큰도 count에 포함, format 토큰은 제외. 화면 glyph 재구성은 호출자 책임. mode는 `WmPwiLayout.wrapStart`의 들여쓰기 규칙에 전달. extendedBreaks는 비ASCII 문자 경계와 여닫는 구두점 보정 사용. type3/7 객체·미지원 토큰·확장 format·산술 overflow·잘못된 측정은 `IOException`.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmPwiTextLayout.java`
