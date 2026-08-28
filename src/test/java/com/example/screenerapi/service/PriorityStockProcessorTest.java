package com.example.screenerapi.service;

import com.example.screenerapi.entity.StockInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PriorityStockProcessorTest {
    private final ScanxClient scanxClient = mock(ScanxClient.class);
    private final StockService stockService = mock(StockService.class);
    private final StockInfoService stockInfoService = mock(StockInfoService.class);
    private final PriorityStockProcessor processor =
            new PriorityStockProcessor(scanxClient, stockService, stockInfoService);

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

    private void invoke(String methodName) {
        try {
            Method method = PriorityStockProcessor.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(processor);
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