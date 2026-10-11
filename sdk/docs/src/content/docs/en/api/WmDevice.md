---
title: "WmDevice"
slug: en/api/wmdevice
---
[API](/en/api/)

Reads airplane mode, SIM, cellular service, and active network state.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmDevice.java`

## Declaration

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

`airplane()` returns the Settings airplane-mode value. `phone()` returns a Phone snapshot. sim and service use Android TelephonyManager and ServiceState constants. Unavailable service is -1; unavailable operator is an empty string. Service lookup requires API 26 or later. Permission and unsupported-feature errors leave unknown snapshot values.

Phone.icon returns radio_off, cellular_ready, or cellular_off. SIM READY alone does not establish network registration. caption returns a Korean authored state string or operator; WmStrings can translate authored strings.

`network()` returns an active or connecting Network snapshot. No connection, missing service, or missing ACCESS_NETWORK_STATE permission returns null. API 23 or later first queries NetworkCapabilities. If unavailable, it falls back to NetworkInfo even on the same API level. This path first checks the active connected/connecting network, then all known networks, and reports validated=false. hasTransport supports only 0=cellular, 1=wifi, and 3=ethernet. hasCapability supports only 16=validated. Snapshots do not update automatically. Query again after system changes.
