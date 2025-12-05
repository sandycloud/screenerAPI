package com.example.screenerapi.controller;

import com.example.screenerapi.entity.StockPrice5Min;
import com.example.screenerapi.service.AdxService;
import com.example.screenerapi.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.example.screenerapi.entity.StockAdxCriteriaDto;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/stock")

public class StockController {
    @Autowired
    private StockService stockService;
    @Autowired
    private AdxService adxService;

    @Value("${external.api.url}")
    private String externalApiUrl;

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StockController.class);

    @PostMapping("/firstfetch-and-store")
    //first time fetch and populate th stock data
    public ResponseEntity<?> firstFetchAndStore(@RequestBody Map<String, Object> req) {
        String stockName = (String) req.get("stockName");
        String isin = (String) req.get("isin");
        String candleTimeFrame = (String) req.get("candleTimeFrame");
        Long fromTime = ((Number) req.get("fromTime")).longValue();
        //create a log entry for the ISIN value 
        
        log.info("isin value: {}, url value :{}", isin, externalApiUrl);
        stockService.firstFetchAndStoreCandles(stockName, isin, candleTimeFrame, fromTime, externalApiUrl);
        return ResponseEntity.ok(Collections.singletonMap("status", "success"));
    }

    @GetMapping("/adx-criteria-stocks")
    public ResponseEntity<List<StockAdxCriteriaDto>> getAdxCriteriaStocks() {
        return ResponseEntity.ok(stockService.fetchAdxCriteriaStocks());
    }

    @GetMapping("/adx")
    public ResponseEntity<?> getAdx(
            @RequestParam String stockName,
            @RequestParam Long time,
            @RequestParam int rows,
            @RequestParam(defaultValue = "14") int period
    ) {
        List<StockPrice5Min> candles = stockService.getRecentCandles(stockName, time, rows + period);
        Collections.reverse(candles); // oldest first
        List<AdxService.AdxResult> adxResults = adxService.calculateAdx(candles, period);
        return ResponseEntity.ok(adxResults);
    }
}
