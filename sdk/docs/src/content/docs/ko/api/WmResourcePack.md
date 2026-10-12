---
title: "WmResourcePack"
slug: ko/api/wmresourcepack
---
[API](/ko/api/)

v2 ZIP 리소스팩을 검증, 설치하고 불변 snapshot을 제공합니다.

`sdk/src/main/java/me/ancal/wmdroid/sdk/WmResourcePack.java`

```java
public final class WmResourcePack
public static final int MAX_FILES = 4096;
public static final int MAX_FILE_BYTES = 64 * 1024 * 1024;
public static final long MAX_BYTES = 256L * 1024 * 1024;
```

## install(File, InputStream)

```java
public static synchronized WmResourcePack install(File store,InputStream input)throws IOException;
```

worker에서 호출하세요. input은 caller 소유이며 닫지 않습니다. null input, 규격, 경로, 파일, I/O 오류는 `IOException`입니다. 모든 파일 검증 후 active pointer를 교체합니다. 실패 시 이전 active를 유지합니다. 반환 snapshot은 이전 snapshot과 독립적입니다.

최대 파일 4096개, 파일당 64 MiB, 압축, 해제 각각 전체 256 MiB입니다. pack.json은 256 KiB, version=2, assets mapping이며 1–4095개의 id를 허용합니다. id는 `[A-Za-z0-9][A-Za-z0-9._/-]{0,159}`입니다. path는 최대 240자이며 절대 경로, 빈 구간, `.`, `..`를 허용하지 않습니다. 누락, 매핑 안 된 파일, 중복 ZIP entry는 거절합니다. 파일 확장자는 png,gif,bmp,jpg,jpeg,webp,ico,cur,svg,ttf,otf,wav,ogg,avi,skn,pwi,json입니다.

version 2만 설치, active 읽기를 허용하며 다른 version은 `IOException("Unsupported pack version")`입니다. `assets`는 logical ID→ZIP path 매핑입니다. ID와 path가 같을 필요는 없으며 여러 ID가 같은 파일을 가리킬 수 있습니다. path는 `[A-Za-z0-9._/-]+` 형식과 canonical store 경계를 검사합니다. 폴더를 포함한 ZIP entry는 최대 8192개이며 확장자는 위의 소문자 값만 허용합니다.

공식 추출기는 `assets/<role-path>` ID와 role별 ZIP path를 출력합니다. 카탈로그 path는 `metadata/library.json`, `metadata/folders.json`입니다. 사용자 팩도 안전한 별도 path로 매핑할 수 있습니다. 가져오기 전에 이미지, 폰트, 사운드, 매핑을 편집해도 되며 원본 pixel hash 일치는 설치 조건이 아닙니다. 파일 크기, 경로, 확장자, 매핑 검사는 그대로 적용합니다.

## active(File), deactivate(File)

```java
public static synchronized WmResourcePack active(File store)throws IOException;
public static synchronized void deactivate(File store)throws IOException;
```

active가 없으면 null입니다. 설치된 pointer, manifest를 읽을 수 없으면 `IOException`입니다. deactivate는 active 선택만 해제합니다. 기존 snapshot 파일은 유지하므로 열린 reader를 계속 사용할 수 있습니다. 오래된 generation은 자동 삭제하지 않습니다.

## ids(), generation(), open(String)

```java
public Set<String> ids();
public String generation();
public InputStream open(String id)throws IOException;
```

ids는 읽기 전용 Set, generation은 snapshot directory 이름입니다. open은 mapped id InputStream을 반환하며 caller가 닫습니다. 없는 id는 `FileNotFoundException`입니다. snapshot 파일을 직접 변경하지 마세요.

## writeZip(OutputStream)

```java
public void writeZip(OutputStream output)throws IOException;
```

현재 snapshot의 mapped 파일, manifest를 ZIP으로 보냅니다. 저장된 ID, path를 보존하며 canonical role path로 다시 이름을 바꾸지 않습니다. output은 caller 소유이며 닫지 않습니다. 최대 크기, 파일 가용성을 검사하며 실패는 `IOException`입니다. worker에서 호출하세요. 런처, 앱 사용은 캐시를 갱신하는 [WmResources](/ko/api/wmresources/)를 통해 설치하세요.
