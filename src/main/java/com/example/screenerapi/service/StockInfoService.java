package com.example.screenerapi.service;

import com.example.screenerapi.dto.StockInfoResponseDto;
import com.example.screenerapi.entity.StockInfo;
import com.example.screenerapi.repository.StockInfoRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StockInfoService {
    private final StockInfoRepository stockInfoRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    public StockInfoService(StockInfoRepository stockInfoRepository) {
        this.stockInfoRepository = stockInfoRepository;
    }

    public StockInfo findByIsin(String isin) {
        return stockInfoRepository.findByIsin(isin).orElse(null);
    }

    public List<StockInfoResponseDto> getAllStockInfo() {
        return stockInfoRepository.findAll().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    public Optional<StockInfoResponseDto> getStockInfoByIsin(String isin) {
        return stockInfoRepository.findByIsin(isin).map(this::toResponseDto);
    }

    public List<StockInfoResponseDto> searchStockInfoByName(String name) {
        return stockInfoRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    public void updateLastDataFetch(String isin, String name, long fetchTime) {
        StockInfo info = stockInfoRepository.findByIsin(isin).orElseGet(StockInfo::new);
        if (info.getIsin() == null) {
            info.setIsin(isin);
        }
        if (name != null && !name.isBlank()) {
            info.setName(name);
        } else if (info.getName() == null) {
            info.setName(isin);
        }
        info.setTimeAtLastDataFetch(String.valueOf(fetchTime));
        stockInfoRepository.save(info);
    }

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
                        StockInfo existing = stockInfoRepository.findByIsin(isin).orElse(null);
                        if (existing != null) {
                            existing.setName(name);
                            existing.setSymbol((String) obj.get("Sym"));
                            stockInfoRepository.save(existing);
                            existing = null;
                        } else {
                            StockInfo info = new StockInfo();
                            info.setIsin(isin);
                            info.setName(name);
                            info.setSymbol((String) obj.get("Sym"));
                            stockInfoRepository.save(info);
                            info = null;
                        }
                    }
                }
            }
        }
    }

    private StockInfoResponseDto toResponseDto(StockInfo stockInfo) {
        return new StockInfoResponseDto(
                stockInfo.getId(),
                stockInfo.getIsin(),
                stockInfo.getName(),
                stockInfo.getTimeAtLastDataFetch(),
                stockInfo.getSymbol());
    }
}
