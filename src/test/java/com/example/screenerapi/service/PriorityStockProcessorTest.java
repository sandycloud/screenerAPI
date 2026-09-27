package com.example.screenerapi.service;

import com.example.screenerapi.entity.StockInfo;
import com.example.screenerapi.entity.StockPrice5Min;
import com.example.screenerapi.repository.StockPrice5MinRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

    private void invoke(String methodName) {
        try {
            Method method = PriorityStockProcessor.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(processor);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
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
}