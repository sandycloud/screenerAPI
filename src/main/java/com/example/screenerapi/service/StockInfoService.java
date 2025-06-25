package com.example.screenerapi.service;

import com.example.screenerapi.entity.StockInfo;
import com.example.screenerapi.repository.StockInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class StockInfoService {
    @Autowired
    private StockInfoRepository stockInfoRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    public void fetchAndStoreStockInfo(String externalApiUrl, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(externalApiUrl, request, Map.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            if (data != null) {
                for (Map<String, Object> obj : data) {
                    String isin = (String) obj.get("Isin");
                    String name = (String) obj.get("DispSym");
                    if (isin != null && name != null) {
                        StockInfo existing = stockInfoRepository.findByIsin(isin);
                        if (existing != null) {
                            existing.setName(name);
                            stockInfoRepository.save(existing);
                        } else {
                            StockInfo info = new StockInfo();
                            info.setIsin(isin);
                            info.setName(name);
                            stockInfoRepository.save(info);
                        }
                    }
                }
            }
        }
    }
}
