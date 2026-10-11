---
title: "WmMediaQueue"
slug: en/api/wmmediaqueue
---
[API index](/en/api/)

```java
public final class WmMediaQueue
```

Ordered playback queue that permits duplicate sources. Reordering preserves the current occurrence. Own and mutate it on the UI thread; it has no internal synchronization.

## Entry

```java
public static final class Entry
public final Uri uri;public final String title;
public Entry(Uri uri,String title)
```

URI accepts lowercase `content`, `http`, or `https` schemes only. Content URIs must be hierarchical with an authority; HTTP(S) addresses are validated by `WmBrowserView.normalizeAddress`. Invalid/null URIs throw `IllegalArgumentException`. The supplied URI is stored; the caller must obtain read access. A null title uses the URI’s last path segment, then an empty string if absent. Titles are truncated to 256 UTF-16 code units.

## Constructor

```java
public WmMediaQueue()
```

Starts empty with currentIndex=-1 and shuffle=false.

## Read queue

```java
public Entry[] entries()
public int size()
public int currentIndex()
```

`entries` returns an array copy in current order. `size` counts occurrences, including duplicates. `currentIndex` is the current occurrence’s array index, or -1 for no selection.

## Replace and append

```java
public void replace(Entry[] values,int selected)
public void append(Entry[] values)
public void select(int index)
public void clear()
```

Null arrays/entries throw `IllegalArgumentException`. `replace` replaces all entries and requires selected=-1 or an in-range index. `append` adds entries while retaining current selection. `select` changes selection to a valid index. `clear` empties the queue and selection. Each mutation rebuilds shuffle order.

## Remove and move

```java
public boolean remove(int index)
public void move(int from,int to)
```

All indices must be in range or `IllegalArgumentException` is thrown. `remove` returns true when removing the current occurrence. Removing it selects the next position (previous position at the end), or -1 for an empty queue. Removing an earlier occurrence decrements the current index. `move` relocates one occurrence and preserves the identity of the current occurrence.

## Shuffle and advance

```java
public boolean shuffle()
public void setShuffle(boolean value)
public boolean canAdvance(int direction,boolean repeat)
public int advance(int direction,boolean repeat)
```

Direction must be -1 (previous) or 1 (next); other values throw `IllegalArgumentException`. Empty queues yield canAdvance=false/advance=-1. With no selection, only next is available. At a boundary with repeat=false, advance returns -1 without changing selection. Successful `advance` returns the new current array index. Repeat=true wraps. Shuffle uses a permutation, placing the current occurrence first and shuffling the rest; repeated wrapping can create a new order. Equivalence to the original shuffle/history algorithm remains unverified.

Source: `sdk/src/main/java/me/ancal/wmdroid/sdk/WmMediaQueue.java`
