---
title: "WmPwiDocument"
slug: en/api/wmpwidocument
---
[API index](/en/api/)

```java
public final class WmPwiDocument
```

Immutable reader model for plain-text storage and recovered binary PWI text records. Preserves original bytes and exposes projected text, formats, and saved caches. It is not an editor or general PWI writer.

## read

```java
public static WmPwiDocument read(byte[] input, Charset plainTextCharset) throws IOException
```

Copies the input array. Null input/charset throws `IllegalArgumentException`. A `{\pwi` prefix selects validated binary-profile parsing; otherwise the specified charset strictly decodes plain storage. Supply the original device code page for plain storage; binary UTF-16 tokens do not use it. `{\rtf`/`{\pwd` throw `IOException`. Truncation, unsupported headers/record order/objects, reference/count mismatches, trailing records, and decoding errors also throw IOException. Unsupported binary structure is not silently flattened.

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

IsBinary identifies the supported binary profile. PlainText is the binary RichInk SF_UTEXT projection without terminal NUL, or decoded plain-storage text. OriginalBytes returns an unchanged-input copy. Paragraphs/fonts are unmodifiable lists; plain storage yields empty lists, defaults=0, and savedView=null. Paragraphs follow native slot order; fonts follow file order. DefaultFontId is an ID; defaultFontSizePoints uses points. Zero can require a runtime default.

## SavedView and Font

```java
public static final class SavedView
public final int documentScale, mode;
public static final class Font
public final int id, family, charset, pitch;
public final String faceName;
```

SavedView contains the persisted signed scale and unsigned mode. Runtime settings can override it; it does not include DPI/extendedBreaks. Font is serialized metadata: id/family/charset/pitch are native values, and faceName is at most 31 UTF-16 code units before NUL. It does not load/substitute font files or create a Typeface. Both types come from read results.

## Font/default/color resolution

```java
public Font resolveFont(int id) throws IOException
public int resolveFontSizePoints(int points) throws IOException
public static int getNativeDefaultColorArgb(int index)
```

Id/points must be 0..65535; otherwise `IllegalArgumentException`. Zero resolves through the stored default. Font returns the first definition with that ID; unresolved default=0 or a missing ID throws IOException. FontSize returns the resolved point size; unresolved default=0 throws IOException. NativeDefaultColor returns opaque ARGB from RichInk factory palette indices 0..31; other indices throw `IllegalArgumentException`. It does not infer document RGB or launcher theme colors.

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

Id is the record ID. LogicalCharacterCount excludes format tokens and counts a CRLF control once. GetTokens returns an unmodifiable list in original order; getPlainText returns this paragraph’s projection. CachedLineCount/layoutDirtyFromLine describe the saved cache; 0xffff means clean and isLayoutDirty=false. Alignment is 0=left/1=right/2=center; other stored values resolve to left. FirstLineIndent is a signed delta; left/right clamp negative stored values to 0. Bullet is a format flag separate from literal `*`. LevelIndent is the unsigned-WORD indent for native mode1. All Native coordinates use logical units, not Android px/dp/points.

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

Both getters belong to Paragraph. The line list is unmodifiable; counts returns an array copy. Caches depend on original-device fonts/width and can be stale when dirty. HorizontalOffset=0 requests native alignment calculation. VerticalOffset is the signed descriptor Y; its exact meaning remains unverified and it is not a baseline. Ascent/descent are saved byte metrics using independent maxima across runs plus ceil/floor halves of leading. These getters do not reflow.

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

GetCharacterFormat belongs to Paragraph. Offset is 0..logicalCharacterCount, otherwise `IllegalArgumentException`. Returns the format immediately before the logical token, including trailing formats at the end offset. It scans the paragraph linearly and throws IOException for extended formats. CharacterFormat is native scalar state. Font ID/size=0 refers to document defaults; colorIndex is a native palette index. Weight operands 1..9 map to 100..900, others to 0; initial weight is 400. Style booleans test nonzero operands. GetNativeOperand accepts only kinds 7..16 or throws `IllegalArgumentException`, retaining raw unknown underline variants 15/16.

## Token

```java
public static final class Token
public final int nativeType;
public final int formatKind;
public final int controlCode;
public final char character;
public byte[] getEncodedBytes()
```

NativeType 1/2=character, 4=width-bearing space token, 5=format, 6=control. FormatKind/controlCode are -1 when inapplicable; character is 0 for noncharacter tokens. SF_UTEXT projection includes characters ≥U+0020, ASCII space for type4, CRLF for control0, and TAB for control4. EncodedBytes returns an exact token copy including uninterpreted fields. Reader token acceptance differs from scalar-format support, so successful reading does not guarantee styled drawing support.


## Logical offsets and plain-text indices

logicalOffset is a PWI logical-token position, not a Java String index. Format tokens do not advance it; ordinary tokens without visible output still count. control0(CRLF) counts1 logically but plain `\r\n` occupies2 UTF-16 units. Hypothetical tokens A,control0,B have logicalCharacterCount3 and plain `A\r\nB` length4. getCharacterFormat(2) returns format before B, while plain index2 points to LF. End offsets include trailing formats. Paragraph saved-line counts and [WmPwiTextLayout.Line](/en/api/wmpwitextlayout/#layout-and-line) logicalStart/logicalCount use this rule too.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmPwiDocument.java`
