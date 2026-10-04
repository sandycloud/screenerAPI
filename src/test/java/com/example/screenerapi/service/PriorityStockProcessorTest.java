package com.example.screenerapi.service;

import com.example.screenerapi.entity.StockInfo;
import com.example.screenerapi.entity.StockPrice5Min;
import com.example.screenerapi.repository.StockPrice5MinRepository;
import com.example.screenerapi.service.AdxService.AdxResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriorityStockProcessorTest {
    private final ScanxClient scanxClient = mock(ScanxClient.class);
    private final StockService stockService = mock(StockService.class);
    private final StockInfoService stockInfoService = mock(StockInfoService.class);
    private final StockPrice5MinRepository stockPriceRepository = mock(StockPrice5MinRepository.class);
    private final PriorityStockProcessor processor =
            new PriorityStockProcessor(scanxClient, stockService, stockInfoService, stockPriceRepository);

    @AfterEach
    void stopProcessor() {
        processor.stop();
    }

    // -----------------------------------------------------------------------
    // Pre-existing tests (unchanged)
    // -----------------------------------------------------------------------

    @Test
    void highPriorityWaitsForCurrentLowPriorityStockThenContinues() throws Exception {
        ScanxStock lowStock = new ScanxStock("INE123", "TEST", "Test", Collections.emptyMap());
        CountDownLatch lowStockStarted = new CountDownLatch(1);
        CountDownLatch releaseLowStock = new CountDownLatch(1);
        CountDownLatch highStockCompleted = new CountDownLatch(1);

        setField("running", true);
        setField("scanxUrl", "http://scanx.test");
        setField("externalApiUrl", "http://provider.test");
        setField("intervalMinutes", 5L);
        setField("uptrendRequest", "high");
        setField("downtrendRequest", "high-down");
        setField("unusualVolumeRequest", "low");
        when(scanxClient.fetch(anyString(), anyString())).thenAnswer(invocation ->
                "low".equals(invocation.getArgument(1)) ? List.of(lowStock) : Collections.emptyList());
        when(stockInfoService.findByIsin(anyString())).thenReturn((StockInfo) null);
        doAnswer(invocation -> {
            lowStockStarted.countDown();
            assertTrue(releaseLowStock.await(2, TimeUnit.SECONDS));
            return null;
        }).when(stockService).subsequentFetchAndStoreCandles(
                anyString(), anyString(), anyString(), anyLong(), anyString());

        Thread lowThread = new Thread(() -> invoke("runLowPriority"));
        lowThread.start();
        assertTrue(lowStockStarted.await(2, TimeUnit.SECONDS));

        Thread highThread = new Thread(() -> {
            invoke("runHighPriority");
            highStockCompleted.countDown();
        });
        highThread.start();

        assertTrue(!highStockCompleted.await(200, TimeUnit.MILLISECONDS));
        releaseLowStock.countDown();
        assertTrue(highStockCompleted.await(2, TimeUnit.SECONDS));
        lowThread.join(2_000);
        highThread.join(2_000);
    }

    @Test
    void highPriorityDeduplicatesListsAppendsIndexAndContinuesAfterFailure() throws Exception {
        ScanxStock first = new ScanxStock("INE1", "ONE", "One", Collections.emptyMap());
        ScanxStock duplicate = new ScanxStock("INE2", "TWO", "Two", Collections.emptyMap());
        ScanxStock third = new ScanxStock("INE3", "THREE", "Three", Collections.emptyMap());

        setField("running", true);
        setField("scanxUrl", "http://scanx.test");
        setField("externalApiUrl", "http://provider.test");
        setField("useOldOrNewUrl", "old");
        setField("uptrendRequest", "up");
        setField("downtrendRequest", "down");
        setField("nseIndices", "Nifty 50");
        when(scanxClient.fetch(anyString(), anyString())).thenAnswer(invocation ->
                "up".equals(invocation.getArgument(1))
                        ? List.of(first, duplicate)
                        : List.of(duplicate, third));
        doAnswer(invocation -> {
            if ("INE2".equals(invocation.getArgument(1))) {
                throw new IllegalStateException("simulated candle failure");
            }
            return null;
        }).when(stockService).subsequentFetchAndStoreCandles(
                any(), anyString(), anyString(), anyLong(), anyString());

        invoke("runHighPriority");

        verify(stockService, times(4)).subsequentFetchAndStoreCandles(
                any(), anyString(), anyString(), anyLong(), anyString());
        verify(stockInfoService, times(3)).updateLastDataFetch(
                anyString(), any(), anyLong());
    }

    @Test
    void lowPrioritySkipsStockProcessedWithinPriorityInterval() throws Exception {
        ScanxStock stock = new ScanxStock("INE123", "TEST", "Test", Collections.emptyMap());
        StockInfo info = new StockInfo();
        info.setIsin(stock.getIsin());
        info.setTimeAtLastDataFetch(String.valueOf(System.currentTimeMillis()));

        setField("running", true);
        setField("scanxUrl", "http://scanx.test");
        setField("unusualVolumeRequest", "low");
        setField("intervalMinutes", 5L);
        when(scanxClient.fetch(anyString(), anyString())).thenReturn(List.of(stock));
        when(stockInfoService.findByIsin(stock.getIsin())).thenReturn(info);

        invoke("runLowPriority");

        verify(stockService, never()).subsequentFetchAndStoreCandles(
                anyString(), anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    void schedulerUsesConfiguredInitialAlignment() throws Exception {
        setField("running", false);
        setField("enabled", false);

        assertTrue(processor.initialDelaySeconds() >= 0);
        assertFalse(processor.isRunning());
    }

    @Test
    void calculatesTrailingAverageVolumeForEachCandle() throws Exception {
        List<StockPrice5Min> candles = new ArrayList<>();
        Long[] volumes = {10L, null, 30L, 40L, 50L, 60L, 70L, 80L,
                90L, 100L, 110L, 120L, 130L, 140L, 150L, 160L};
        for (int index = volumes.length - 1; index >= 0; index--) {
            StockPrice5Min candle = new StockPrice5Min();
            candle.setIsin("INE123");
            candle.setTimeInMillis((long) index);
            candle.setVolume(volumes[index]);
            candles.add(candle);
        }
        when(stockPriceRepository.findRecentCandles(anyString(), anyLong(), eq(120)))
                .thenReturn(candles);

        invokeAverageVolume("INE123");

        assertEquals(10L, candles.get(0).getAverageVolume());
        assertEquals(10L, candles.get(1).getAverageVolume());
        assertEquals(79L, candles.get(13).getAverageVolume());
        assertEquals(84L, candles.get(14).getAverageVolume());
        assertEquals(95L, candles.get(15).getAverageVolume());
        verify(stockPriceRepository).saveAll(candles);
    }

    // -----------------------------------------------------------------------
    // runAdx unit tests
    // -----------------------------------------------------------------------

    /**
     * Test 1: Happy path — runAdx processes all stocks queued in
     * stocksPendingAdx and drains the map afterwards.
     *
     * Setup:
     *  - Two stocks (INE_A, INE_B) placed in stocksPendingAdx.
     *  - Both have candles with missing ADX values (count > 0).
     *  - getAdxValues returns one AdxResult per ISIN.
     *  - findByIsinAndTimeInMillisIn returns one candle per ISIN, all ADX
     *    fields null so all three will be written.
     *  - findRecentCandles returns empty list — average-volume step skipped.
     *
     * Assertions:
     *  - saveAll is called once per ISIN (2 times total).
     *  - stocksPendingAdx is empty after the call — map was fully drained.
     */
    @Test
    void runAdx_processesAllPendingStocksAndDrainsPendingMap() throws Exception {
        String isinA = "INE_A";
        String isinB = "INE_B";
        long timeA = 1000L;
        long timeB = 2000L;

        AdxResult resultA = buildAdxResult(timeA, 25.0, 30.0, 20.0);
        AdxResult resultB = buildAdxResult(timeB, 18.0, 22.0, 15.0);

        StockPrice5Min candleA = buildCandle(isinA, timeA, null, null, null);
        StockPrice5Min candleB = buildCandle(isinB, timeB, null, null, null);

        when(stockPriceRepository.countCandlesWithMissingAdxValues(isinA)).thenReturn(1L);
        when(stockPriceRepository.countCandlesWithMissingAdxValues(isinB)).thenReturn(1L);

        when(stockService.getAdxValues(eq(isinA), anyLong(), eq(120), eq(14)))
                .thenReturn(List.of(resultA));
        when(stockService.getAdxValues(eq(isinB), anyLong(), eq(120), eq(14)))
                .thenReturn(List.of(resultB));

        when(stockPriceRepository.findByIsinAndTimeInMillisIn(eq(isinA), anyList()))
                .thenReturn(List.of(candleA));
        when(stockPriceRepository.findByIsinAndTimeInMillisIn(eq(isinB), anyList()))
                .thenReturn(List.of(candleB));

        when(stockPriceRepository.findRecentCandles(anyString(), anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());

        putStockPending(isinA, new ScanxStock(isinA, "A", "StockA", Collections.emptyMap()));
        putStockPending(isinB, new ScanxStock(isinB, "B", "StockB", Collections.emptyMap()));

        invokeRunAdx();

        verify(stockPriceRepository, times(2)).saveAll(anyList());
        assertTrue(getStocksPendingAdx().isEmpty(),
                "stocksPendingAdx should be empty after runAdx processes all pending stocks");
    }

    /**
     * Test 2: Empty pending map — runAdx should be a complete no-op.
     *
     * Setup:
     *  - stocksPendingAdx starts empty (no stocks queued).
     *
     * Assertions:
     *  - countCandlesWithMissingAdxValues is never called.
     *  - getAdxValues is never called.
     *  - saveAll is never called.
     */
    @Test
    void runAdx_emptyPendingMap_doesNothing() throws Exception {
        invokeRunAdx();

        verify(stockPriceRepository, never())
                .countCandlesWithMissingAdxValues(anyString());
        verify(stockService, never())
                .getAdxValues(anyString(), anyLong(), anyInt(), anyInt());
        verify(stockPriceRepository, never()).saveAll(anyList());
    }

    /**
     * Test 3: Exception thrown during one stock's ADX processing must not
     * prevent the remaining stocks from being processed.
     *
     * Setup:
     *  - Two stocks: INE_FAIL and INE_OK in stocksPendingAdx.
     *  - countCandlesWithMissingAdxValues throws RuntimeException for INE_FAIL.
     *  - INE_OK follows the normal happy-path (1 candle updated).
     *
     * Assertions:
     *  - saveAll is called exactly once (for INE_OK only).
     *  - stocksPendingAdx is empty — both stocks removed regardless of failure.
     */
    @Test
    void runAdx_exceptionForOneStockDoesNotStopOthers() throws Exception {
        String isinFail = "INE_FAIL";
        String isinOk   = "INE_OK";
        long timeOk     = 5000L;

        AdxResult resultOk  = buildAdxResult(timeOk, 20.0, 25.0, 18.0);
        StockPrice5Min candleOk = buildCandle(isinOk, timeOk, null, null, null);

        when(stockPriceRepository.countCandlesWithMissingAdxValues(isinFail))
                .thenThrow(new RuntimeException("simulated DB failure for INE_FAIL"));

        when(stockPriceRepository.countCandlesWithMissingAdxValues(isinOk)).thenReturn(1L);
        when(stockService.getAdxValues(eq(isinOk), anyLong(), eq(120), eq(14)))
                .thenReturn(List.of(resultOk));
        when(stockPriceRepository.findByIsinAndTimeInMillisIn(eq(isinOk), anyList()))
                .thenReturn(List.of(candleOk));
        when(stockPriceRepository.findRecentCandles(anyString(), anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());

        putStockPending(isinFail,
                new ScanxStock(isinFail, "FAIL", "StockFail", Collections.emptyMap()));
        putStockPending(isinOk,
                new ScanxStock(isinOk, "OK", "StockOk", Collections.emptyMap()));

        invokeRunAdx();

        // INE_OK was still processed despite INE_FAIL throwing
        verify(stockPriceRepository, times(1)).saveAll(anyList());
        assertTrue(getStocksPendingAdx().isEmpty(),
                "stocksPendingAdx should be empty after runAdx, even when one stock failed");
    }

    /**
     * Test 4: When all candles for an ISIN already have ADX values
     * (countCandlesWithMissingAdxValues == 0), persistMissingAdxValues must
     * return early without calling getAdxValues or saveAll.
     *
     * Setup:
     *  - One stock in stocksPendingAdx.
     *  - countCandlesWithMissingAdxValues returns 0.
     *
     * Assertions:
     *  - getAdxValues is never called (early-return fired).
     *  - saveAll is never called.
     */
    @Test
    void runAdx_skipsAdxCalculationWhenNoCandlesHaveMissingValues() throws Exception {
        String isin = "INE_COMPLETE";
        when(stockPriceRepository.countCandlesWithMissingAdxValues(isin)).thenReturn(0L);

        putStockPending(isin,
                new ScanxStock(isin, "C", "Complete", Collections.emptyMap()));

        invokeRunAdx();

        verify(stockService, never())
                .getAdxValues(anyString(), anyLong(), anyInt(), anyInt());
        verify(stockPriceRepository, never()).saveAll(anyList());
    }

    /**
     * Test 5: persistMissingAdxValues must update only the fields that are
     * currently null. Fields already populated must not be overwritten.
     *
     * Setup:
     *  - One candle: adxValue=null, plusDIValue=null, minusDIValue=99.9.
     *  - AdxResult provides adx=25.0, plusDI=30.0, minusDI=15.0.
     *
     * Assertions:
     *  - candle.adxValue   == 25.0   (null → updated)
     *  - candle.plusDIValue == 30.0  (null → updated)
     *  - candle.minusDIValue == 99.9 (non-null → unchanged)
     *  - saveAll is called exactly once (at least one field changed).
     */
    @Test
    void runAdx_updatesOnlyNullAdxFields_doesNotOverwriteExistingValues() throws Exception {
        String isin = "INE_PARTIAL";
        long time   = 3000L;

        // minusDIValue already populated — must remain 99.9 after the call
        StockPrice5Min candle = buildCandle(isin, time, null, null, 99.9);
        AdxResult result      = buildAdxResult(time, 25.0, 30.0, 15.0);

        when(stockPriceRepository.countCandlesWithMissingAdxValues(isin)).thenReturn(1L);
        when(stockService.getAdxValues(eq(isin), anyLong(), eq(120), eq(14)))
                .thenReturn(List.of(result));
        when(stockPriceRepository.findByIsinAndTimeInMillisIn(eq(isin), anyList()))
                .thenReturn(List.of(candle));
        when(stockPriceRepository.findRecentCandles(anyString(), anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());

        putStockPending(isin,
                new ScanxStock(isin, "P", "Partial", Collections.emptyMap()));

        invokeRunAdx();

        assertNotNull(candle.getAdxValue(),    "adxValue should have been set from null");
        assertNotNull(candle.getPlusDIValue(), "plusDIValue should have been set from null");
        assertEquals(25.0, candle.getAdxValue(),    0.001);
        assertEquals(30.0, candle.getPlusDIValue(), 0.001);
        assertEquals(99.9, candle.getMinusDIValue(), 0.001,
                "minusDIValue was already set and must not be overwritten");

        verify(stockPriceRepository, times(1)).saveAll(anyList());
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    private void invoke(String methodName) {
        try {
            Method method = PriorityStockProcessor.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(processor);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    /** Convenience wrapper so test bodies read clearly. */
    private void invokeRunAdx() {
        invoke("runAdx");
    }

    private void invokeAverageVolume(String isin) {
        try {
            Method method = PriorityStockProcessor.class
                    .getDeclaredMethod("calculateAndPersistAverageVolume", String.class);
            method.setAccessible(true);
            method.invoke(processor, isin);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private void setField(String name, Object value) throws Exception {
        Field field = PriorityStockProcessor.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(processor, value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, ScanxStock> getStocksPendingAdx() throws Exception {
        Field field = PriorityStockProcessor.class.getDeclaredField("stocksPendingAdx");
        field.setAccessible(true);
        return (Map<String, ScanxStock>) field.get(processor);
    }

    private void putStockPending(String isin, ScanxStock stock) throws Exception {
        getStocksPendingAdx().put(isin, stock);
    }

    /**
     * Builds an {@link AdxResult} with the given field values.
     */
    private AdxResult buildAdxResult(long timeInMillis, double adx,
                                     double plusDI, double minusDI) {
        AdxResult result    = new AdxResult();
        result.timeInMillis = timeInMillis;
        result.adx          = adx;
        result.plusDI       = plusDI;
        result.minusDI      = minusDI;
        return result;
    }

    /**
     * Builds a {@link StockPrice5Min} with the given ADX field values.
     * Pass {@code null} for any ADX argument to leave that field unset.
     */
    private StockPrice5Min buildCandle(String isin, long timeInMillis,
                                       Double adxValue, Double plusDIValue,
                                       Double minusDIValue) {
        StockPrice5Min candle = new StockPrice5Min();
        candle.setIsin(isin);
        candle.setTimeInMillis(timeInMillis);
        candle.setAdxValue(adxValue);
        candle.setPlusDIValue(plusDIValue);
        candle.setMinusDIValue(minusDIValue);
        return candle;
    }
}