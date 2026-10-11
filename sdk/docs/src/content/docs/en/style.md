---
title: "Writing rules"
slug: en/style
---

- Confirm declarations, conditions, returns, and errors against current source. Mark required, uncertain facts with `Needs verification`.
- Keep guides organized by task. Link each API name to its class reference; keep exact signatures and API-specific contracts there.
- Preserve identifiers, code, numbers, units, defaults, and required or prohibited conditions.
- Retain thread, asynchronous, ownership, lifetime, and permission constraints.
- State shared constraints once. Add only API-specific differences.
- Use one concept per sentence. Use complete sentences preserving actor, conditions, target, units, order, and return meaning.
- Use numbered steps only when order matters.
- Keep explanations and examples needed to execute, interpret results, and diagnose errors. Separate sentences with different conditions.
- Update paired pages, Javadoc, code, and links without changing the API.

## Terms

| 한국어 | English | Meaning |
| --- | --- | --- |
| 애플릿 | applet | An app screen using the shared WM-style shell. |
| 셸 | shell | Titles, status, Start, command bars, and shared state. |
| Today 모듈 | Today module | A unit of Today-screen content. |
| 행 | row | A v1 data item returned by a provider. |
| 논리 픽셀 | logical pixel | A UI coordinate before whole-screen scaling. |
| 리소스팩 | resource pack | A validated data ZIP. |
| 호스트 | host | The launcher that displays modules and supplies shell state. |
| 스냅샷 | snapshot | An immutable data group for one point in time. |
| UI 스레드 | UI thread | Android main thread; creates and changes Views. |
| 호출자 | caller | Code invoking this API. |
| 소유자 | owner | Code responsible for attachment, removal, release, and persistence. |
| 콜백 | callback | Registered function invoked by an API; thread and timing have separate contracts. |
| 텍스트 | text | Displayed wording or input values, distinguished by the API. |
| 캐럿 | caret | Input insertion or candidate navigation position; name the target. |
| 후보 선택 | candidate selection | Candidate index, separate from text selection. |
| 반환 | return | A call’s result value. |
| 전달 | pass/forward | Sending a value or event to another target. |
| 통지 | notify | Reporting state or results through callbacks or another channel. |
