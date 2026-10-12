---
title: "애플릿과 셸"
slug: ko/applets
---


## 애플릿 생성

```java
package org.example.demo;

import android.os.Bundle;
import android.widget.LinearLayout;
import me.ancal.wmdroid.sdk.WmApplet;

public final class DemoActivity extends WmApplet {
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        caption("Demo");
        content.addView(label("Ready"),
            new LinearLayout.LayoutParams(-1, -2));
        closeButton(false, null);
        softKeys("Close", this::finish, "", null);
    }
}
```

```xml
<activity android:name=".DemoActivity"
    android:exported="true"
    android:screenOrientation="portrait"
    android:theme="@style/WmAppletTheme">
    <meta-data android:name="me.ancal.wmdroid.sdk.WM_STYLE"
        android:value="true" />
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

Manifest 요소는 `<application>` 안에 넣으세요. `sdk/templates/res/values/wm_applet_theme.xml`을 앱 `res/values/`로 복사하세요. 앱 또는 Activity에 `android:theme="@style/WmAppletTheme"`를 지정하세요.

## subclass API

```text
protected LinearLayout content;
protected Typeface regular, bold;
protected int accent;
protected WmWindow shellWindow;
protected final void caption(String value);
protected final TextView label(String value);
protected final void softKeys(String left, Runnable leftAction,
                              String right, Runnable rightAction);
protected final void closeButton(boolean ok, Runnable action);
protected void onShellStateChanged(WmShell.State state);
protected void onResourcesChanged();
```

UI API는 main thread에서 호출하세요. `super.onCreate(saved)` 이후에 `content`를 사용하세요.

- `label()`: 현재 regular face를 적용한 TextView 반환. parent에 붙이지 않습니다.
- `softKeys()`: 하단 명령 교체, 열린 command menu 닫기. null callback은 명령 비활성화.
- `closeButton()`: `ok=false`는 X, `true`는 OK. null action은 Activity 종료. 저장은 callback에서 구현하세요. 열린 `WmInputDialog`가 있으면 OK, accept 동작이 우선합니다.
- `onShellStateChanged()`: 셸 상태 반영 후 앱 표시 갱신. 편집 값을 유지하세요.
- `onResourcesChanged()`: pack 변경 후 artwork 캐시 갱신. editor를 재생성하지 마세요.

## 상태와 복원

`WmApplet`은 resumed 상태에서 런처 글꼴, 테마, 언어, 배율, 메뉴 변경을 관찰합니다. 사용자 정의 컨테이너는 `WmControls.RestyleListener`, `LanguageListener`를 구현하세요.

앱 데이터, 초안 저장은 앱의 책임입니다. `onSaveInstanceState()` 또는 앱 저장소를 사용하세요. 컨트롤에 `saveInteractionState()`가 있으면 대응하는 `restoreInteractionState()`로 복원하세요.

시작 메뉴는 런처 menu snapshot을 사용합니다. 호스트 상태, 리소스 동기화와 호스트 명령은 사용 가능한 런처가 필요합니다.

값은 앱 저장소 또는 instance state에 보관하고, 상호작용 상태는 같은 타입, 설정의 컨트롤에서 짝지어 저장, 복원하세요. 다음은 앱 소유 WmDateField `dateField`의 fragment입니다. UI 스레드에서 실행하며, onCreate에서 컨트롤을 같은 범위, locale, format으로 먼저 다시 만든 뒤 값→상호작용 순서로 복원합니다. 저장된 범위 밖 값이나 유효하지 않은 상태의 IllegalArgumentException은 앱이 처리하세요. restoreInteractionState는 focus를 요청하지 않습니다.

```java
// onSaveInstanceState, before the superclass call:
out.putString("date_value", me.ancal.wmdroid.sdk.WmDateField.formatValue(dateField.value()));
out.putBundle("date_interaction", dateField.saveInteractionState());
// onCreate, after recreating dateField and its configuration:
if (saved != null && saved.containsKey("date_value")) {
    dateField.setValue(me.ancal.wmdroid.sdk.WmDateField.parseValue(saved.getString("date_value")));
    dateField.restoreInteractionState(saved.getBundle("date_interaction"));
}
```

[WmDateField 계약](/ko/api/wmdatefield/#saveinteractionstate)

## 셸 명령

[WmShell.command](/ko/api/wmshell/#commandstring)는 Intent만 만들며 호출자가 실행합니다. 현재 WMDroid host의 TodayActivity.runShellCommand는 다음 값을 처리합니다. 다른 host/버전까지 지원을 보장하지 않으며 unknown 값은 동작을 실행하지 않습니다. 연락처 저장 중에는 명령을 무시할 수 있습니다.

| command | 대상 |
| --- | --- |
| `today` | 현재 대화상자 닫기; 닫기를 거부하면 유지 |
| `tasks`, `calendar`, `contacts`, `notes` | 작업, 일정, 연락처, 메모 |
| `messages`, `phone`, `browser`, `media` | 메시지, 전화, 브라우저, 미디어 |
| `programs`, `settings`, `help` | 프로그램, 설정, 도움말 |

## 실행 예제 연결

가이드 코드는 설명용 fragment입니다. 전체 Activity, Provider, Manifest 연결은 별도 `WMDroid-examples-sources.zip`의 `sample/`에 있습니다. `sample/app/src/main/java/me/ancal/wmdroid/sample/SampleActivity.java`, `SampleProvider.java`, `sample/app/src/main/AndroidManifest.xml`을 함께 확인하세요. SDK 소스 아카이브는 `sdk/templates/res/values/wm_applet_theme.xml`을 제공합니다. [프로젝트 빌드와 배포 준비물](/ko/getting-started/#예제와-배포-준비물)을 확인하세요.
