package com.example.screenerapi.controller;

import com.example.screenerapi.dto.StockInfoResponseDto;
import com.example.screenerapi.service.StockInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stockinfo")
public class StockInfoController {
    private final StockInfoService stockInfoService;

    public StockInfoController(StockInfoService stockInfoService) {
        this.stockInfoService = stockInfoService;
    }

    @PostMapping("/fetch-and-store")
    public ResponseEntity<?> fetchAndStoreStockInfo(@RequestBody Map<String, Object> payload) {
        String externalApiUrl = "https://ow-scanx-analytics.dhan.co/customscan/fetchdt";
        stockInfoService.fetchAndStoreStockInfo(externalApiUrl, payload);
        return ResponseEntity.ok().body("Stock info fetched and stored successfully");
    }

    // 1. Fetch all data
    @GetMapping("/all")
    public ResponseEntity<List<StockInfoResponseDto>> getAllStockInfo() {
        return ResponseEntity.ok(stockInfoService.getAllStockInfo());
    }

    // 2. Fetch by ISIN
    @GetMapping("/by-isin/{isin}")
    public ResponseEntity<StockInfoResponseDto> getByIsin(@PathVariable String isin) {
        return stockInfoService.getStockInfoByIsin(isin)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 3. Search by name (wildcard)
    @GetMapping("/search")
    public ResponseEntity<List<StockInfoResponseDto>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(stockInfoService.searchStockInfoByName(name));
    }
}
