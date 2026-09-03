package com.example.screenerapi.service;

import com.example.screenerapi.entity.StockInfo;
import com.example.screenerapi.repository.StockInfoRepository;
import com.example.screenerapi.repository.StockPrice5MinRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Runs once at application startup and deletes {@code stock_price_5min} rows whose
 * {@code time_in_millis} is older than 3 weeks (21 days). Errors are logged and
 * swallowed so the application keeps running.
 */
@Component
public class StockPrice5MinCleanup implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(StockPrice5MinCleanup.class);
    private static final long THREE_WEEKS_MS = 21L * 24L * 60L * 60L * 1000L;

    private final StockPrice5MinRepository repository;
    private final StockInfoRepository stockInfoRepository;

    //to Bypass: set stock.cleanup.enabled=false in any
    // application-*.properties to disable without recompiling.
    @Value("${stock.cleanup.enabled:true}")
    private boolean enabled;

    public StockPrice5MinCleanup(StockPrice5MinRepository repository, StockInfoRepository stockInfoRepository) {
        this.repository = repository;
        this.stockInfoRepository = stockInfoRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        long started = System.currentTimeMillis();
        long cutoff = System.currentTimeMillis() - THREE_WEEKS_MS;
        
        try {
            List<String> isins = repository.findDistinctIsinByTimeInMillisLessThan(cutoff);
            List<StockInfo> stockInfos = stockInfoRepository.findByIsinIn(isins);
            if (stockInfos.isEmpty()) {
                return;
            }
            int deleted = repository.deleteByTimeInMillisLessThan(cutoff);

            Map<String, String> nameByIsin = stockInfos.stream()
                    .filter(info -> info.getIsin() != null && info.getName() != null)
                    .collect(Collectors.toMap(StockInfo::getIsin, StockInfo::getName));
            List<String> stocks = isins.stream()
                    .map(isin -> nameByIsin.getOrDefault(isin, isin))
                    .collect(Collectors.toList());
            log.info("stock_price_5min cleanup: deleted={} rows for stocks={} in {} ms",
                    deleted, stocks, System.currentTimeMillis() - started);
        } catch (Exception exception) {
            log.error("stock_price_5min cleanup failed after {} ms",
                    System.currentTimeMillis() - started, exception);
        }
    }
}