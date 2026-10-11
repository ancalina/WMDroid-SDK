---
title: "WmContactSubcardView"
slug: ko/api/wmcontactsubcardview
---
[API 색인](/ko/api/)

```java
public final class WmContactSubcardView extends FrameLayout implements WmControls.RestyleListener
```

연락처 이름 또는 주소를 구성 요소별로 입력한다. 소유 앱이 레이블 순서와 결과 조합을 정의하며 `WmContactDetailsView.showSubcard`에 표시할 수 있다.

## 생성자

```java
public static final int NAME=0,ADDRESS=1;
public WmContactSubcardView(Context context,Typeface regular,int kind,String[] labels,String[] values,int selection)
```

`NAME`은 정확히 6개, `ADDRESS`는 5개 레이블/값 필요. 배열을 복사한다. null 글꼴, 다른 kind, 배열 길이 불일치, null 배열/문자열은 `IllegalArgumentException`. 주소의 0·4번 필드는 여러 줄 입력; 나머지는 한 줄.

## Draft

```java
public String[] values()
public EditText editor(int index)
public void setValues(String[] values)
public void setLabels(String[] values)
```

`values()`는 현재 초안의 배열 복사. `editor(index)`는 0부터 시작하는 실제 입력 컨트롤; 범위 밖이면 배열 인덱스 예외. `setValues`는 생성자와 같은 문자열 개수 검증 후 교체하며 사용자 변경 콜백을 보내지 않는다. 커서는 새 문자열 길이에 맞춰 제한한다. `setLabels`는 레이블과 접근성 설명을 갱신하며 값은 보존한다.

## Changes

```java
public interface OnChangedListener {void changed(int index,String value);}
public void setOnChangedListener(OnChangedListener listener)
```

TextWatcher에서 실제 값이 바뀌면 0부터 시작하는 필드 인덱스와 새 문자열을 동기로 전달합니다. 사용자 입력뿐 아니라 editor(index).setText(...)도 통지합니다. setValues는 통지를 억제하며 같은 값은 통지하지 않습니다. null은 콜백 해제. 저장이나 Apply/Cancel은 소유 앱이 처리한다.

## Geometry and style

```java
public int desiredHeight()
protected void onMeasure(int widthSpec,int heightSpec)
protected void onLayout(boolean changed,int left,int top,int right,int bottom)
protected void dispatchDraw(Canvas canvas)
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
```

`desiredHeight`는 NAME 248, ADDRESS 328 논리 픽셀. 기본 폭 350; 측정 제약에 맞춰 폭 조절. 레이아웃은 순서대로 필드를 배치하며 2픽셀 테두리를 마지막에 그린다. 재스타일은 regular·선택 색을 갱신하고 null regular를 거부한다.

소스: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmContactSubcardView.java`
