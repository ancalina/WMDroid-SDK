---
title: "WmPwiDocument"
slug: ko/api/wmpwidocument
---
[API 색인](/ko/api/)

```java
public final class WmPwiDocument
```

일반 텍스트 저장형과 복구된 binary PWI 텍스트 record를 읽는 불변 모델. 원본 byte 보존·텍스트 투영·서식/저장 cache 조회를 제공한다. 편집기나 일반 PWI writer는 아니다.

## read

```java
public static WmPwiDocument read(byte[] input, Charset plainTextCharset) throws IOException
```

입력 배열 복사. input/charset null은 `IllegalArgumentException`. `{\pwi`로 시작하면 지원 binary profile 검증·파싱; 그렇지 않으면 지정 charset으로 strict decode. charset은 일반 저장형의 원래 기기 code page이며 binary의 UTF-16 토큰에는 적용하지 않는다. `{\rtf`/`{\pwd`는 `IOException`. 잘린 데이터·미지원 header/record 순서·객체·참조/count 불일치·남은 record·decode 오류도 IOException. 미지원 구조를 평문으로 조용히 변환하지 않는다.

## Content

```java
public boolean isBinary()
public String getPlainText()
public byte[] getOriginalBytes()
public List<Paragraph> getParagraphs()
public List<Font> getFonts()
public int getDefaultFontId()
public int getDefaultFontSizePoints()
public SavedView getSavedView()
```

isBinary는 지원 binary profile 여부. plainText는 binary에서 RichInk SF_UTEXT 투영(terminal NUL 제외), 일반 저장형에서는 decode 결과. originalBytes는 변경하지 않은 원본 복사. paragraphs/fonts는 수정 불가 목록; plain 저장형은 빈 목록/default=0/savedView=null. 문단 순서는 native slot 순서, 폰트는 파일 순서. defaultFontId는 폰트 ID, defaultFontSizePoints는 point 수이며 0이면 runtime 기본값이 필요할 수 있다.

## SavedView and Font

```java
public static final class SavedView
public final int documentScale, mode;
public static final class Font
public final int id, family, charset, pitch;
public final String faceName;
```

SavedView는 문서에 저장된 signed scale과 unsigned mode. 런타임 설정이 덮을 수 있으며 DPI/extendedBreaks를 포함하지 않는다. Font는 저장 메타데이터; id·family·charset·pitch는 native 값, faceName은 NUL 전까지 최대 31 UTF-16 코드 단위. 실제 font 파일·Typeface 생성/대체는 수행하지 않는다. 두 타입은 read 결과로만 얻는다.

## Font/default/color resolution

```java
public Font resolveFont(int id) throws IOException
public int resolveFontSizePoints(int points) throws IOException
public static int getNativeDefaultColorArgb(int index)
```

id/points는 0..65535; 범위 밖은 `IllegalArgumentException`. 0은 저장된 기본값으로 해석. font는 같은 ID의 첫 정의를 반환; 기본값 0/ID 없음은 IOException. fontSize는 해결된 point 수; 기본값 0은 IOException. nativeDefaultColor는 RichInk factory palette 0..31을 opaque ARGB로 반환, 범위 밖은 `IllegalArgumentException`. 문서 RGB나 런처 테마 색을 추정하지 않는다.

## Paragraph

```java
public static final class Paragraph
public static final int ALIGN_LEFT = 0, ALIGN_RIGHT = 1, ALIGN_CENTER = 2;
public final int id;
public final int logicalCharacterCount;
public final int cachedLineCount;
public final int layoutDirtyFromLine;
public String getPlainText()
public List<Token> getTokens()
public boolean isLayoutDirty()
public int getAlignment()
public int getFirstLineIndentNative()
public int getLeftIndentNative()
public int getRightIndentNative()
public boolean hasBullet()
public int getLevelIndentNative()
```

id는 record ID. logicalCharacterCount는 format 토큰 제외 논리 수; CRLF control은 하나로 센다. getTokens는 수정 불가 원본 순서, getPlainText는 해당 문단 투영. cachedLineCount와 layoutDirtyFromLine은 저장 cache 정보, 0xffff는 clean이며 isLayoutDirty=false. alignment는 0=left/1=right/2=center, 다른 저장값은 left. firstLineIndent는 signed delta, left/right는 음수 저장값을 0으로 clamp. bullet은 서식 flag이며 문자 `*`와 별개. levelIndent는 native mode1용 unsigned WORD 들여쓰기. 모든 Native 좌표는 논리 단위이며 Android px/dp/point가 아니다.

## Saved line cache

```java
public List<CachedLine> getCachedLines()
public int[] getCachedLineCharacterCounts()
public static final class CachedLine
public final int logicalCharacterCount;
public final int horizontalOffsetNative, widthNative;
public final int verticalOffsetNative;
public final int ascentWithLeadingNative, descentWithLeadingNative;
```

두 getter는 Paragraph의 메서드. line 목록은 수정 불가, counts는 배열 복사. cache는 원기기의 글꼴/폭에 종속하며 dirty이면 오래된 값일 수 있다. horizontalOffset=0은 native alignment 계산을 요청하는 값. verticalOffset은 signed descriptor Y; 정확한 의미 미검증이며 baseline으로 사용하지 않는다. ascent/descent는 run별 독립 최댓값에 leading을 ceil/floor 반으로 나누어 더한 저장 byte 지표. 이 getter는 reflow를 수행하지 않는다.

## CharacterFormat

```java
public CharacterFormat getCharacterFormat(int logicalOffset) throws IOException
public static final class CharacterFormat
public int getNativeOperand(int kind)
public int getFontId()
public int getFontSizePoints()
public int getColorIndex()
public int getFontWeight()
public boolean isItalic()
public boolean isUnderlined()
public boolean isStruckOut()
public boolean isHighlighted()
```

getCharacterFormat은 Paragraph 메서드. offset=0..logicalCharacterCount, 그 외 `IllegalArgumentException`; 해당 토큰 직전 서식, 끝 offset에서는 trailing format 포함 반환. 문단을 선형 탐색하며 확장 서식은 IOException. CharacterFormat은 native scalar 상태. font ID/size=0은 문서 기본값, colorIndex는 native palette 인덱스. weight operand 1..9는 100..900, 그 외 0; 초기 weight=400. 스타일 boolean은 해당 operand nonzero. getNativeOperand는 kind 7..16만 허용하며 위반 시 `IllegalArgumentException`; 15/16의 알 수 없는 underline 변형도 raw 값으로 유지한다.

## Token

```java
public static final class Token
public final int nativeType;
public final int formatKind;
public final int controlCode;
public final char character;
public byte[] getEncodedBytes()
```

nativeType 1/2=문자, 4=폭 포함 공백 토큰, 5=서식, 6=control. formatKind/controlCode는 해당되지 않으면 -1, character는 문자 외 토큰에서 0. SF_UTEXT 투영은 문자 U+0020 이상, type4의 ASCII 공백, control0의 CRLF, control4의 TAB만 텍스트에 넣는다. encodedBytes는 해석하지 않은 필드를 포함한 정확한 토큰 복사. getter의 scalar 서식 지원과 reader의 허용 토큰 범위는 다르므로 read 성공이 styled drawing 지원을 보장하지 않는다.


## 논리 offset과 평문 인덱스

logicalOffset은 Java String 인덱스가 아닌 PWI 논리 토큰 위치입니다. format 토큰은 증가하지 않고 문자 출력 없는 일반 토큰도 셉니다. control0(CRLF)은 논리1이지만 평문 `\r\n`은 UTF-16 단위2입니다. 가상 토큰 A,control0,B의 logicalCharacterCount는3이고 평문 `A\r\nB` 길이는4입니다. getCharacterFormat(2)는 B 직전 서식이지만 평문 index2는 LF입니다. 끝 offset은 trailing format도 반영합니다. Paragraph의 저장 줄 수와 [WmPwiTextLayout.Line](/ko/api/wmpwitextlayout/#layout-and-line) logicalStart/logicalCount도 이 규칙입니다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmPwiDocument.java`
