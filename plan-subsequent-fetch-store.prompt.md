## Plan: Subsequent Fetch and Store Stock Data API

### TL;DR
Implement a new API endpoint `/api/stock/subsequentfetch-and-store` that fetches stock candle data from an external API starting from current time going backwards, until reaching the maximum `time_in_millis` already stored in the database for that ISIN. Uses upsert logic (update if exists, insert if new) with batch processing for optimal memory and performance.

---

### Steps

#### Phase 1: Repository Enhancement
1. **Add `findMaxTimeInMillisByIsin` method** to `StockPrice5MinRepository`
   - Native SQL query: `SELECT MAX(time_in_millis) FROM stock_price_5min WHERE isin = ?1`
   - Returns `Optional<Long>` for null safety

#### Phase 2: Service Layer Implementation
2. **Add `subsequentFetchAndStoreCandles` method** to `StockService`
   - Parameters: `stockName`, `isin`, `candleTimeFrame`, `fromTime` (current time), `externalApiUrl`
   - Flow:
     a. Call repository to get `maxTimeInMillis` for the ISIN
     b. If no data exists (Optional.empty()), delegate to `firstFetchAndStoreCandles` or fetch from `fromTime` backwards
     c. If data exists, fetch candles from external API in batches (configurable limit, default 100) starting from `fromTime` going backwards
     d. Continue fetching until the fetched candle's time <= `maxTimeInMillis` (inclusive)
     e. For each batch, process candles with upsert logic (update all fields if exists, insert if new) using a **new method** (not modifying existing `processResponse`)
     f. Use batch save (`saveAll`) for efficiency
     g. Properly close/cleanup resources (ProcessBuilder, BufferedReader, etc.)

3. **Add new method `processResponseForSubsequentFetch`** in `StockService`
   - Similar to `processResponse` but generic for any timeframe
   - Uses `findByTimeInMillisAndIsin().orElse(new StockPrice5Min())` for upsert
   - Returns list of entities for batch save
   - Does NOT modify existing `processResponse` method

4. **Make batch size configurable**
   - Add `@Value("${subsequent.fetch.batch.size:100}")` private int subsequentFetchBatchSize;
   - Default to 100 if not configured

5. **Add metrics for monitoring**
   - Log fetch duration per batch
   - Log total records processed
   - Log API errors count
   - Log memory usage (optional)

6. **Optimize memory usage**
   - Process and save in batches, clear lists after save
   - Use try-with-resources for ProcessBuilder, BufferedReader
   - Avoid creating unnecessary objects in loops

#### Phase 3: Controller (Already Exists)
5. **Verify `StockController.subsequentFetchAndStore`** - already mapped to `/api/stock/subsequentfetch-and-store`
   - Calls `stockService.subsequentFetchAndStoreCandles`
   - Returns success response

#### Phase 4: Testing & Verification
6. **Test scenarios:**
   - Stock with existing data (normal case)
   - Stock with no existing data (edge case)
   - Large dataset (memory/performance)
   - Concurrent requests
   - API error handling

---

### Relevant Files

| File | Changes |
|------|---------|
| `src/main/java/com/example/screenerapi/repository/StockPrice5MinRepository.java` | Add `findMaxTimeInMillisByIsin(String isin)` method |
| `src/main/java/com/example/screenerapi/service/StockService.java` | Add `subsequentFetchAndStoreCandles` method; add new `processResponseForSubsequentFetch` method; add configurable batch size; add metrics logging |
| `src/main/java/com/example/screenerapi/controller/StockController.java` | Verify existing endpoint works (no changes needed) |
| `src/main/resources/application.properties` | Add `subsequent.fetch.batch.size=100` property |

---

### Verification

1. **Unit/Integration Tests:**
   - Test `findMaxTimeInMillisByIsin` returns correct max time
   - Test subsequent fetch stops at max time (inclusive)
   - Test upsert updates existing records correctly
   - Test batch processing with configurable limit
   - Test memory cleanup (no resource leaks)
   - Test metrics logging (duration, records, errors)

2. **Manual API Test:**
   ```bash
   curl -X POST http://localhost:8080/api/stock/subsequentfetch-and-store \
     -H "Content-Type: application/json" \
     -d '{"stockName":"RELIANCE","isin":"INE002A01018","candleTimeFrame":"5","fromTime":1724342400000}'
   ```

3. **Database Verification:**
   - Check no duplicate `isin + time_in_millis` rows
   - Verify data goes back to max time (inclusive)
   - Verify all fields updated on existing records

4. **Metrics Verification:**
   - Check logs for fetch duration per batch
   - Check logs for total records processed
   - Check logs for API errors (if any)

---

### Decisions

- **Batch size**: Configurable via `subsequent.fetch.batch.size` property (default 100)
- **Upsert logic**: Update ALL fields (open, high, low, close, volume, alternateVal, datetimestamp)
- **Timeframe support**: Generic - works with '5', '15', '30', etc.
- **Stop condition**: Fetch until candle time <= maxTimeInMillis (inclusive)
- **Resource management**: try-with-resources for ProcessBuilder, BufferedReader, HTTP connections
- **Parallel processing**: Not needed for subsequent fetch (sequential backwards fetch is simpler and safer)
- **ProcessResponse**: NOT modified - new method `processResponseForSubsequentFetch` created instead
- **Metrics**: Log fetch duration, records processed, API errors per batch

---

### Further Considerations

1. **Rate limiting**: External API may have rate limits - consider adding delay between batches
2. **Error handling**: What if external API fails mid-fetch? Current implementation throws RuntimeException
3. **Transaction boundaries**: Each batch save should be in its own transaction for large datasets
4. **Monitoring**: Add metrics for fetch duration, records processed, API errors