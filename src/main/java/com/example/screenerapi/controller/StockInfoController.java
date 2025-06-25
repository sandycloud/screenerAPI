package com.example.screenerapi.controller;

import com.example.screenerapi.service.StockInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.example.screenerapi.entity.StockInfo;
import com.example.screenerapi.repository.StockInfoRepository;

@RestController
@RequestMapping("/api/stockinfo")
public class StockInfoController {
    @Autowired
    private StockInfoService stockInfoService;

    @Autowired
    private StockInfoRepository stockInfoRepository;

    @PostMapping("/fetch-and-store")
    public ResponseEntity<?> fetchAndStoreStockInfo(@RequestBody Map<String, Object> payload) {
        String externalApiUrl = "https://ow-scanx-analytics.dhan.co/customscan/fetchdt";
        stockInfoService.fetchAndStoreStockInfo(externalApiUrl, payload);
        return ResponseEntity.ok().body("Stock info fetched and stored successfully");
    }

    // 1. Fetch all data
    @GetMapping("/all")
    public ResponseEntity<List<StockInfo>> getAllStockInfo() {
        return ResponseEntity.ok(stockInfoRepository.findAll());
    }

    // 2. Fetch by ISIN
    @GetMapping("/by-isin/{isin}")
    public ResponseEntity<StockInfo> getByIsin(@PathVariable String isin) {
        StockInfo info = stockInfoRepository.findByIsin(isin);
        if (info != null) {
            return ResponseEntity.ok(info);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 3. Search by name (wildcard)
    @GetMapping("/search")
    public ResponseEntity<List<StockInfo>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(stockInfoRepository.findByNameContainingIgnoreCase(name));
    }
}
