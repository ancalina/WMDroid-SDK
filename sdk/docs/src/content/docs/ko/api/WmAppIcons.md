---
title: "WmAppIcons"
slug: ko/api/wmappicons
---
[API](/ko/api/)

Android 앱 아이콘을 정사각형 Bitmap으로 렌더링합니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmAppIcons.java`

## 선언

```text
public final class WmAppIcons
public static Bitmap render(Drawable source,int pixels,boolean unframed);
```

`render()`의 pixels는 출력 한 변의 픽셀 수입니다. source가 null이면 null을 반환합니다. 그 외 pixels≤0은 `IllegalArgumentException`입니다.

새 ARGB_8888 Bitmap을 반환하며 density는 DENSITY_NONE입니다. source bounds는 draw 실패 때도 복원합니다. 반환 Bitmap은 caller가 소유합니다. 공유 Drawable을 동시에 렌더링하지 마세요.

unframed는 API 26 이상에서 AdaptiveIconDrawable foreground 추출을 시도합니다. alpha>16 픽셀의 경계를 잘라 종횡비를 유지하고 중앙 배치하며 출력 각 변에 최소 `pixels*0.04` 여백을 둡니다. 유효한 경계가 없거나 alpha>0 픽셀 중 R, G, B가 모두235보다 큰 픽셀이 95%를 초과하면 전체 아이콘을 사용합니다. 다른 Drawable은 전체를 그립니다.
