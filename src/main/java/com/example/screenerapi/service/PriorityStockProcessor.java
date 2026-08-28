package com.example.screenerapi.service;

import com.example.screenerapi.entity.StockInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class PriorityStockProcessor implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(PriorityStockProcessor.class);

    private final ScanxClient scanxClient;
    private final StockService stockService;
    private final StockInfoService stockInfoService;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final ReentrantLock processingGate = new ReentrantLock(true);
    private final ReentrantLock coordinationLock = new ReentrantLock(true);
    private final Condition priorityFinished = coordinationLock.newCondition();

    @Value("${stock.processor.enabled:false}")
    private boolean enabled;
    @Value("${external.api.url}")
    private String externalApiUrl;
    @Value("${scanx.api.url:https://scanx-analytics.dhan.co/customscan/v2/fetchdt}")
    private String scanxUrl;
    @Value("${scanx.uptrend.request.body:}")
    private String uptrendRequest;
    @Value("${scanx.downtrend.request.body:}")
    private String downtrendRequest;
    @Value("${scanx.unusual-volume.request.body:}")
    private String unusualVolumeRequest;
    @Value("${stock.processor.interval.minutes:5}")
    private long intervalMinutes;
    @Value("${stock.processor.low-priority.interval.minutes:1}")
    private long lowPriorityIntervalMinutes;
    @Value("${stock.processor.low-priority.mode:repeat}")
    private String lowPriorityMode;
    @Value("${nse_index:}")
    private String nseIndices;
    @Value("${bse_index:}")
    private String bseIndices;

    private volatile boolean running;
    private volatile boolean highPriorityRunning;
    private ScheduledFuture<?> highPriorityTask;
    private ScheduledFuture<?> lowPriorityTask;

    public PriorityStockProcessor(ScanxClient scanxClient, StockService stockService,
                                  StockInfoService stockInfoService) {
        this.scanxClient = scanxClient;
        this.stockService = stockService;
        this.stockInfoService = stockInfoService;
    }

    @Override
    public void start() {
        if (!enabled || running) {
            return;
        }
        running = true;
        long initialDelay = initialDelaySeconds();
        highPriorityTask = executor.scheduleAtFixedRate(this::runHighPriority, initialDelay,
                Math.max(1, intervalMinutes) * 60, TimeUnit.SECONDS);
        if ("one-pass".equalsIgnoreCase(lowPriorityMode)) {
            lowPriorityTask = executor.schedule(this::runLowPriority, initialDelay, TimeUnit.SECONDS);
        } else {
            lowPriorityTask = executor.scheduleAtFixedRate(this::runLowPriority, initialDelay,
                    Math.max(1, lowPriorityIntervalMinutes) * 60, TimeUnit.SECONDS);
        }
        log.info("Processor scheduler enabled; first cycle in {} seconds", initialDelay);
    }

    long initialDelaySeconds() {
        Instant now = Instant.now();
        long epochMinutes = now.getEpochSecond() / 60;
        long nextBoundary = ((epochMinutes / 5) + 1) * 5;
        return Math.max(0, (nextBoundary + 1) * 60 - now.getEpochSecond());
    }

    private void runHighPriority() {
        coordinationLock.lock();
        try {
            if (!running || highPriorityRunning) {
                return;
            }
            highPriorityRunning = true;
            priorityFinished.signalAll();
        } finally {
            coordinationLock.unlock();
        }

        processingGate.lock();
        long started = System.currentTimeMillis();
        log.info("HIGH Priority cycle started");
        try {
            Map<String, ScanxStock> stocks = new LinkedHashMap<>();
            addStocks(stocks, scanxClient.fetch(scanxUrl, uptrendRequest));
            addStocks(stocks, scanxClient.fetch(scanxUrl, downtrendRequest));
            addIndices(stocks);
            ProcessingCounts counts = processHighPriorityStocks(stocks.values());
            log.info("HIGH Priority cycle finished: candidates={}, processed={}, failed={}, durationMs={}",
                    stocks.size(), counts.processed, counts.failed, System.currentTimeMillis() - started);
        } catch (Exception exception) {
            log.error("HIGH Priority cycle failed", exception);
        } finally {
            processingGate.unlock();
            coordinationLock.lock();
            try {
                highPriorityRunning = false;
                priorityFinished.signalAll();
            } finally {
                coordinationLock.unlock();
            }
        }
    }

    private ProcessingCounts processHighPriorityStocks(Iterable<ScanxStock> stocks) {
        ProcessingCounts counts = new ProcessingCounts();
        for (ScanxStock stock : stocks) {
            try {
                processStock(stock, "HIGH Priority");
                counts.processed++;
            } catch (Exception exception) {
                counts.failed++;
                log.error("HIGH Priority failed for ISIN={}", stock.getIsin(), exception);
            }
        }
        return counts;
    }

    private void runLowPriority() {
        if (!awaitPriorityFinished()) {
            return;
        }
        long started = System.currentTimeMillis();
        log.info("LOW Priority cycle started");
        ProcessingCounts counts = new ProcessingCounts();
        try {
            List<ScanxStock> stocks = scanxClient.fetch(scanxUrl, unusualVolumeRequest);
            for (ScanxStock stock : stocks) {
                if (!awaitPriorityFinished()) {
                    log.info("LOW Priority paused before next stock");
                    break;
                }
                processingGate.lock();
                try {
                    if (!isPriorityIdle()) {
                        log.info("LOW Priority paused before processing ISIN={}", stock.getIsin());
                        break;
                    }
                    StockInfo info = stockInfoService.findByIsin(stock.getIsin());
                    if (recentlyProcessed(info)) {
                        counts.skipped++;
                        log.info("LOW Priority skipped recently processed ISIN={}", stock.getIsin());
                        continue;
                    }
                    try {
                        processStock(stock, "LOW Priority");
                        counts.processed++;
                    } catch (Exception exception) {
                        counts.failed++;
                        log.error("LOW Priority failed for ISIN={}", stock.getIsin(), exception);
                    }
                } finally {
                    processingGate.unlock();
                }
            }
            log.info("LOW Priority cycle finished: candidates={}, processed={}, skipped={}, failed={}, durationMs={}",
                    stocks.size(), counts.processed, counts.skipped, counts.failed,
                    System.currentTimeMillis() - started);
        } catch (Exception exception) {
            log.error("LOW Priority cycle failed", exception);
        }
    }

    private boolean awaitPriorityFinished() {
        coordinationLock.lock();
        try {
            while (running && highPriorityRunning) {
                log.info("LOW Priority paused while HIGH Priority is running");
                try {
                    priorityFinished.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return running;
        } finally {
            coordinationLock.unlock();
        }
    }

    private boolean isPriorityIdle() {
        coordinationLock.lock();
        try {
            return running && !highPriorityRunning;
        } finally {
            coordinationLock.unlock();
        }
    }

    private boolean recentlyProcessed(StockInfo info) {
        if (info == null || info.getTimeAtLastDataFetch() == null) {
            return false;
        }
        try {
            long lastFetch = Long.parseLong(info.getTimeAtLastDataFetch());
            return lastFetch >= System.currentTimeMillis() - Math.max(1, intervalMinutes) * 60_000;
        } catch (NumberFormatException exception) {
            log.warn("Ignoring invalid last fetch time for ISIN {}", info.getIsin());
            return false;
        }
    }

    private void processStock(ScanxStock stock, String priority) {
        long started = System.currentTimeMillis();
        if (stock.getIsin() == null || stock.getIsin().isBlank()) {
            throw new IllegalArgumentException("Stock identity is required");
        }
        stockService.subsequentFetchAndStoreCandles(stock.getDisplayName(), stock.getIsin(), "5",
                System.currentTimeMillis(), externalApiUrl);
        String name = stock.getDisplayName() == null ? stock.getSymbol() : stock.getDisplayName();
        stockInfoService.updateLastDataFetch(stock.getIsin(), name, System.currentTimeMillis());
        log.info("{} processed ISIN={} durationMs={}", priority, stock.getIsin(),
                System.currentTimeMillis() - started);
    }

    private void addStocks(Map<String, ScanxStock> target, List<ScanxStock> stocks) {
        if (stocks == null) {
            return;
        }
        for (ScanxStock stock : stocks) {
            if (stock != null && stock.getIsin() != null && !stock.getIsin().isBlank()) {
                target.putIfAbsent(stock.getIsin(), stock);
            }
        }
    }

    private void addIndices(Map<String, ScanxStock> target) {
        addIndexValues(target, nseIndices);
        addIndexValues(target, bseIndices);
    }

    private void addIndexValues(Map<String, ScanxStock> target, String values) {
        if (values == null || values.isBlank()) {
            return;
        }
        for (String value : values.split(",")) {
            String index = value.trim();
            if (!index.isEmpty()) {
                target.putIfAbsent(index, new ScanxStock(index, index, index, java.util.Collections.emptyMap()));
            }
        }
    }

    private static class ProcessingCounts {
        private int processed;
        private int skipped;
        private int failed;
    }

    @Override
    public void stop() {
        coordinationLock.lock();
        try {
            running = false;
            priorityFinished.signalAll();
        } finally {
            coordinationLock.unlock();
        }
        if (highPriorityTask != null) {
            highPriorityTask.cancel(false);
        }
        if (lowPriorityTask != null) {
            lowPriorityTask.cancel(false);
        }
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("Processor executor did not terminate within shutdown timeout");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while stopping processor executor");
        }
    }

    @Override public boolean isRunning() { return running; }
    @Override public int getPhase() { return Integer.MAX_VALUE; }
    @Override public boolean isAutoStartup() { return true; }
}