package com.example.screenerapi.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StockPriceSchemaMigration {
    private final JdbcTemplate jdbcTemplate;

    public StockPriceSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void enforceUniqueCandleIdentity() {
        jdbcTemplate.execute("DELETE FROM stock_price_5min "
                + "WHERE id NOT IN (SELECT MIN(id) FROM stock_price_5min GROUP BY isin, time_in_millis)");
        jdbcTemplate.execute("DROP INDEX IF EXISTS idx_isin_time");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_isin_time "
                + "ON stock_price_5min (isin, time_in_millis)");
    }
}