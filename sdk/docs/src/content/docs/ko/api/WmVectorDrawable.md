---
title: "WmVectorDrawable"
slug: ko/api/wmvectordrawable
---
[API](/ko/api/)

팩의 v1 vector JSON을 Drawable로 읽고 bounds에 맞춰 그립니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmVectorDrawable.java`

## 선언

```text
public final class WmVectorDrawable extends Drawable
public static final int MAX_BYTES = 256 * 1024;
public static WmVectorDrawable read(InputStream input)throws IOException;
public ConstantState getConstantState();
public void draw(Canvas canvas);
public int getIntrinsicWidth();
public int getIntrinsicHeight();
public void setAlpha(int value);
public void setColorFilter(ColorFilter value);
public int getOpacity();
```

`read()`는 InputStream을 닫지 않습니다. 최대 256 KiB입니다. 규격, 숫자, JSON 오류는 `IOException`입니다.

필수 값: version=1, width, height(0 초과, 4096 이하), intrinsicWidth, intrinsicHeight(1–4096), scale(fit 또는 stretch), pixelGrid, compositeFilter(boolean), parts(1–512개). 전체 path 명령은 최대 10000개입니다. ARGB 색은 # 없는 8자리 hex입니다.

path 명령은 M, L(2값), C(6값), Q(4값), O(중심, 양수 반지름 3값), Z(0값)입니다. fillRule은 winding 또는 evenodd입니다. alpha는 0–255, stroke width는 0 초과 4096 이하입니다. gradient 색은 2–64개이며 stops 수가 같고 0–1 순서여야 합니다. 모든 숫자는 finite이며 절댓값 100000 이하입니다. transform은 9값 Matrix입니다.

fit은 종횡비를 유지해 중앙에 배치하고 stretch는 가로, 세로를 따로 늘립니다. pixelGrid는 정수 변환, skew 없는 배치에서만 AA를 끕니다. compositeFilter는 전체 그림에 alpha, filter를 적용합니다.

`getConstantState().newDrawable()`은 Paint 상태를 복제합니다. 각 소비자에 새 Drawable을 사용하세요. `setAlpha()`는 0–255로 제한합니다. `getOpacity()`는 TRANSLUCENT입니다. 파싱은 worker에서 수행하세요.

## JSON 구조

모든 root 필드는 matrix를 제외하고 필수입니다. parts의 각 객체는 fillRule, path, color, alpha가 필수입니다.

| 객체 | 필드와 값 |
| --- | --- |
| root | `version`: 정수1; `width`, `height`: 양수≤4096; `intrinsicWidth`, `intrinsicHeight`: 정수1–4096; `scale`: `fit` 또는 `stretch`; `pixelGrid`, `compositeFilter`: boolean; `parts`: 객체 배열1–512개; 선택 `matrix`: 숫자9개 배열 |
| part | `fillRule`: `winding` 또는 `evenodd`; `path`: 명령 배열의 배열; `color`: `AARRGGBB` 문자열; `alpha`: 정수0–255 |
| 선택 stroke | part의 `stroke`: 양수≤4096; 함께 필수인 `join`: `round` 또는 `miter`; cap은 BUTT |
| 선택 gradient | part의 `gradient` 객체: `type`은 `linear` 또는 `radial`; `points`는 linear `[x0,y0,x1,y1]`, radial `[cx,cy,radius]`이며 radius>0; `colors`는 ARGB 문자열2–64개; `stops`는 같은 개수의 비감소 숫자0–1; 선택 `matrix`는 숫자9개 배열 |

path 명령은 `['M',x,y]`처럼 명령 문자열 뒤에 좌표가 옵니다. 아래 JSON은 채운 검은 삼각형의 최소 예입니다.

```json
{"version":1,"width":24,"height":24,"intrinsicWidth":24,"intrinsicHeight":24,"scale":"fit","pixelGrid":false,"compositeFilter":false,"parts":[{"fillRule":"winding","color":"ff000000","alpha":255,"path":[["M",0,0],["L",24,0],["L",24,24],["Z"]]}]}
```
