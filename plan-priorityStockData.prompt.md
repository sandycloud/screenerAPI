## Plan: Priority Stock Data Processing

Implement two Spring-managed background processors for approximately 220 derivative-traded Indian stocks:
- High priority: ScanX ADX uptrend/downtrend stocks plus configured NSE/BSE indices.
- Low priority: ScanX unusual-volume stocks processed only while high priority is idle.
Existing public flows remain unchanged.

**Steps**

### Phase 1: Contracts and configuration
1. Add a dedicated ScanX client/parser contract for the three `v2/fetchdt` POST requests. Parse positional rows through the response `headers` array, not fixed indexes. Preserve `Isin`, `Sym`, `DispSym`, and the requested momentum/volume fields.
2. Keep the uptrend mapping aligned with the supplied contract: `Isin` is the ninth field after `DispSym` (zero-based row index 8). Resolve identities through header names and validate that `Isin` is present and nonblank before candle processing.
3. Add properties for the ScanX URL, three request bodies, scheduler enablement, five-minute cycle, startup alignment, low-priority cycle interval/mode, page/count settings, and optional inter-stock delay.
4. Extend `StockInfo`/its repository only as needed to retain stock name/symbol-to-ISIN mapping and the last successful fetch timestamp. Use a consistent epoch-millis or timestamp representation for numeric comparisons.

### Phase 2: Thread-safe coordinator
5. Add two managed single-thread processor loops or equivalent Spring-managed executors. Calculate initial delay so both first run at the next five-minute boundary plus one minute; E.g. if startup at 13:03 therefore runs at 13:06. Repeat high priority every configurable five minutes.
6. Define thread-safe high-running state, low pause/resume signalling, shutdown handling, and a shared processing gate so candle writes never occur concurrently.
7. When high priority is due, signal low priority to pause. Low priority finishes its current stock, stops between stocks, and does not interrupt an active API/database operation. High priority then owns processing until its list completes and signals low priority to resume.
8. Release all signals/gates in `finally` blocks on exceptions, cancellation, upstream failures, and shutdown. Bound executor creation and avoid per-cycle leaks.

### Phase 3: High-priority workflow
9. Log a tagged cycle start, fetch uptrend and downtrend lists, validate response codes/rows, combine and deduplicate by ISIN.
10. Append `nse_index` and `bse_index`. Use `NSE_INDEX`, `BSE_INDEX`, or `NSE_EQ` provider instrument types correctly.
11. Process each asset sequentially through `StockService.subsequentFetchAndStoreCandles` using current epoch milliseconds. After each successful fetch, upsert `stock_info` and update `timeAtLastDataFetch`. This update applies to both processors.
12. Log per-stock success/failure and one cycle completion record with counts and total duration, while avoiding full payload/candle-level noise.  Tag all High priority processing related logs with 'HIGH Priority'.

### Phase 4: Secondary workflow
13. Fetch the unusual-volume list at a configurable low-priority interval/cycle mode and parse its header-driven rows.
14. Before each stock, compare its last successful `stock_info` fetch time with `now - highPriorityInterval`. Skip stocks processed within that window; otherwise call `subsequentFetchAndStoreCandles` and update `stock_info` on success.
15. Check the pause state before fetching and between stocks. On priority activation, finish only the current stock, release the gate, and wait. Tag all low-priority start/end, pause/resume, skip, success, and failure logs with `LOW`.
16. Support configurable continuous-repeat or one-pass low-priority behavior, with a default that cannot starve priority processing.

### Phase 5: Preserve existing flows
17. Do not alter existing controller endpoints or first-fetch/ADX public behavior. Reuse `FilterStocks` only through a shared client/parser extraction that preserves its DTO contract.
18. Harden `subsequentFetchAndStoreCandles` only where scheduler correctness requires it: apply the max-timestamp boundary before persistence, find oldest time by value, count actual rows, prevent repeated-page loops, define no-existing-data behavior, and correct index URL construction. Keep existing `processResponse` unchanged unless compatibility tests require otherwise.
19. Add database uniqueness for `(isin,time_in_millis)` if scheduler retries/concurrent callers can create duplicates, and verify SQLite/JPA schema behavior under the active profile.

### Phase 6: Tests and documentation
20. Test header-driven parsing for all three fixtures, including differing fields, reordered headers, missing optional values, malformed rows, and uptrend `Isin` mapping.
21. Test scheduler alignment, priority deduplication/index appending, pause/resume, current-stock completion, no concurrent writes, low-priority cutoff skipping, and failure/shutdown recovery.
22. Test `stock_info` timestamp updates, subsequent-fetch boundaries/upserts, duplicate prevention, and correct NSE/BSE URLs.
23. Update `README.md` and profile properties. Run `mvnw.cmd test` and `mvnw.cmd package`; use mocked ScanX/provider APIs for manual end-to-end checks.

**Relevant files**
- `e:/projects/java/screenerNewApi/src/main/java/com/example/screenerapi/service/StockService.java` - candle fetch, subsequent fetch, and provider URL behavior.
- `e:/projects/java/screenerNewApi/src/main/java/com/example/screenerapi/service/FilterStocks.java` - current ScanX POST and DTO mapping.
- `e:/projects/java/screenerNewApi/src/main/java/com/example/screenerapi/entity/StockInfo.java` and `repository/StockInfoRepository.java` - identity and last-fetch tracking.
- `e:/projects/java/screenerNewApi/src/main/java/com/example/screenerapi/entity/StockPrice5Min.java` and `repository/StockPrice5MinRepository.java` - candle uniqueness/persistence.
- `e:/projects/java/screenerNewApi/src/main/java/com/example/screenerapi/controller/StockController.java` - existing public flows to preserve.
- `e:/projects/java/screenerNewApi/src/main/resources/application.properties`, `application-dev.properties`, and `application-test.properties` - scheduler and API configuration.
- New coordinator, ScanX parser/client, DTO, and test classes under the existing package structure.

**Verification**
1. Assert startup alignment, including 13:03 -> 13:06.
2. Assert HDFCBANK unusual-volume, BANKBARODA downtrend, and GLENMARK uptrend parsing.
3. Assert uptrend `Isin` maps from header index 8 and is validated as nonblank.
4. Assert priority deduplication, index URLs, timestamps, low-priority cutoff, and mutual exclusion.
5. Assert pause/resume and exception/interruption/shutdown paths do not deadlock.
6. Run `mvnw.cmd test` and `mvnw.cmd package`, then inspect tagged cycle timing logs.

**Decisions**
- Both processors start as Spring-managed background services at the next five-minute boundary plus one minute.
- High priority repeats every five minutes by default; low-priority mode/interval is configurable.
- The skip timestamp is updated after successful processing by either processor.
- The uptrend response already supplies `Isin` as the ninth header/row field after `DispSym`; parse by header name and validate it.
- Separate processor threads may be used, but a shared gate prevents concurrent candle database writes.
- Existing flows remain compatible except for internal fixes required for scheduler correctness.