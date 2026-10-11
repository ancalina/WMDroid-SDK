---
title: "WmContactSummaryView"
slug: en/api/wmcontactsummaryview
---
[API index](/en/api/)

```java
public final class WmContactSummaryView extends LinearLayout implements WmControls.RestyleListener,WmControls.LanguageListener
```

Displays a contact name, company, job title, phone/link/address actions, and a read-only Notes tab. The owner performs calls, messages, email, and address editing through callbacks.

## Phone

```java
public static final int CALL=0,SMS=1,ICON_NONE=-1,ICON_MOBILE=0,ICON_WORK=1,ICON_HOME=2,ICON_SMS=3;
public static final class Phone
public final String key,label,number;
public final int action,icon;
public Phone(String key,String label,String number)
public Phone(String key,String label,String number,int action,int icon)
```

The three-argument constructor uses CALL and ICON_WORK. `key` is a nonempty action ID; `label` and `number` are literal display strings. A null/empty key, null strings, another action, or an icon outside -1..3 throws `IllegalArgumentException`. Number syntax is not validated here.

## Link and Address

```java
public static final int EMAIL=0,WEB=1;
public static final class Link
public final String key,label,value;public final int kind;
public Link(String key,String label,String value,int kind)
public static final class Address
public final String key,label,value;
public Address(String key,String label,String value)
```

Both types require a non-null, nonempty key and non-null label/value. Link kind accepts only EMAIL/WEB. Violations throw `IllegalArgumentException`. Values remain literal strings; the owner parses destinations and decides which actions are allowed.

## Callbacks

```java
public interface Listener {void selected(Phone phone);void dial(Phone phone);default void message(Phone phone){}default void selectedLink(Link link){}default void openLink(Link link){}default void selectedAddress(Address address){}default void editAddress(Address address){}}
public interface OnTabChangedListener {void changed(int tab);}
```

A phone tap invokes `selected`, then `dial` for CALL or `message` for SMS. A link invokes `selectedLink`, then `openLink`; an address invokes `selectedAddress`, then `editAddress`. Default methods perform no action. The tab callback runs only when the Summary/Notes tab actually changes.

## Constructor

```java
public WmContactSummaryView(Context context,Typeface regular,Typeface bold,int selection,Listener listener)
```

Both fonts and the listener are required; null throws `IllegalArgumentException`. The selection color highlights action rows.

## setContact

```java
public void setContact(String name,Phone[] phones,String notes)
public void setContact(String name,String company,String jobTitle,Phone[] phones,Link[] links,String notes)
public void setContact(String name,String company,String jobTitle,Phone[] phones,Link[] links,Address[] addresses,String notes)
```

Shorter overloads use empty values for omitted strings/arrays. Null name/company/job title/notes become empty strings. Arrays are copied; null arrays/entries or duplicate keys across all action arrays throw `IllegalArgumentException` before mutation. Selection is retained by key, otherwise defaults to the first phone, then link, then address. Identical content avoids rebuilding; changes preserve scroll positions in both tabs.

## Selection and tabs

```java
public Phone selected()
public Link selectedLink()
public Address selectedAddress()
public int selectedTab()
public void setOnTabChangedListener(OnTabChangedListener listener)
public void setTab(int tab)
public void setMessage(String authored)
```

Selection getters return the active entry of that type, otherwise null. Tab 0 is Summary; 1 is Notes; other values throw `IllegalArgumentException`. A null tab listener disables callbacks. Non-null `setMessage` text covers the content while keeping tabs visible. Null restores the selected tab.

## Style

```java
public void onWmRestyle(Typeface regular,Typeface bold,int selection,int tabColor)
public void onWmLanguageChanged(String language)
```

Updates fonts and selection color; null fonts throw `IllegalArgumentException`. Language changes apply to SDK-authored captions and preserve user data.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmContactSummaryView.java`
