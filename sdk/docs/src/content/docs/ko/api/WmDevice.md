---
title: "WmDevice"
slug: ko/api/wmdevice
---
[API](/ko/api/)

비행 모드, SIM, 통신 서비스, 활성 네트워크 상태를 읽습니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmDevice.java`

## 선언

```text
public final class WmDevice
public static boolean airplane(Context c);
public static Phone phone(Context context);
public static Network network(Context c);
```

## WmDevice.Network

```text
public static class Network
public final boolean connected;
public final boolean connecting;
public final boolean wifi;
public final boolean cellular;
public final boolean ethernet;
public final boolean validated;
public Network(boolean connected,boolean wifi,boolean cellular,boolean ethernet,boolean validated);
public Network(boolean connected,boolean connecting,boolean wifi,boolean cellular,boolean ethernet,boolean validated);
public boolean hasTransport(int transport);
public boolean hasCapability(int capability);
```

## WmDevice.Phone

```text
public static final class Phone
public final boolean airplane;
public final int sim;
public final int service;
public final String operator;
public Phone(boolean airplane,int sim,int service,String operator);
public String icon();
public String caption();
```

`airplane()`은 Settings의 비행 모드 값을 반환합니다. `phone()`은 Phone snapshot을 반환합니다. sim, service 값은 Android TelephonyManager, ServiceState 상수입니다. service를 조회할 수 없으면 -1, operator가 없으면 빈 문자열입니다. 서비스 조회는 API 26 이상입니다. 권한, 기능 오류는 snapshot의 알 수 없는 값으로 처리합니다.

Phone.icon은 radio_off, cellular_ready, cellular_off 중 하나입니다. SIM READY만으로 등록 완료를 판단하지 않습니다. caption은 상태에 맞는 한국어 authored 문자열 또는 operator이며 WmStrings로 번역할 수 있습니다.

`network()`는 활성, 연결 중 Network snapshot을 반환합니다. 연결이 없거나 서비스, ACCESS_NETWORK_STATE 권한이 없으면 null입니다. API 23 이상에서 먼저 NetworkCapabilities를 조회합니다. 결과가 없으면 같은 API에서도 NetworkInfo 경로로 fallback합니다. 이 경로는 활성 connected/connecting 네트워크를 먼저, 없으면 알려진 전체 네트워크를 조회하며 validated=false입니다. hasTransport는 0=cellular, 1=wifi, 3=ethernet만, hasCapability는 16=validated만 지원합니다. snapshot은 자동 갱신하지 않습니다. owner가 시스템 변경 후 다시 조회하세요.
