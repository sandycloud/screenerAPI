package com.example.screenerapi.service;

import com.example.screenerapi.dto.StockInfoResponseDto;
import com.example.screenerapi.entity.StockInfo;
import com.example.screenerapi.repository.StockInfoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class StockInfoServiceTest {
    private final StockInfoRepository stockInfoRepository = mock(StockInfoRepository.class);
    private final StockInfoService stockInfoService = new StockInfoService(stockInfoRepository);

    @Test
    void mapsEntityFieldsForListLookupAndSearchResponses() {
        StockInfo stockInfo = new StockInfo();
        stockInfo.setId(42L);
        stockInfo.setIsin("INE123");
        stockInfo.setName("Example Ltd");
        stockInfo.setTimeAtLastDataFetch("1720000000");
        stockInfo.setSymbol("EXAMPLE");

        when(stockInfoRepository.findAll()).thenReturn(List.of(stockInfo));
        when(stockInfoRepository.findByIsin("INE123")).thenReturn(Optional.of(stockInfo));
        when(stockInfoRepository.findByNameContainingIgnoreCase("example"))
                .thenReturn(List.of(stockInfo));

        assertResponseFields(stockInfoService.getAllStockInfo().get(0));
        assertResponseFields(stockInfoService.getStockInfoByIsin("INE123").orElseThrow());
        assertResponseFields(stockInfoService.searchStockInfoByName("example").get(0));
    }

    @Test
    void returnsEmptyWhenIsinDoesNotExist() {
        when(stockInfoRepository.findByIsin("missing")).thenReturn(Optional.empty());

        assertFalse(stockInfoService.getStockInfoByIsin("missing").isPresent());
    }

    @Test
    void updateLastDataFetchHandlesConcurrentInsertRace() {
        StockInfo existing = new StockInfo();
        existing.setId(7L);
        existing.setIsin("INE123");
        existing.setName("Original");
        existing.setTimeAtLastDataFetch("100");

        when(stockInfoRepository.findByIsin("INE123")).thenReturn(Optional.empty(), Optional.of(existing));
        doThrow(new DataIntegrityViolationException("duplicate")).when(stockInfoRepository).saveAndFlush(any(StockInfo.class));

        stockInfoService.updateLastDataFetch("INE123", "Updated", 200L);

        ArgumentCaptor<StockInfo> captor = ArgumentCaptor.forClass(StockInfo.class);
        verify(stockInfoRepository, atLeastOnce()).save(captor.capture());
        StockInfo saved = captor.getValue();
        assertEquals("INE123", saved.getIsin());
        assertEquals("Updated", saved.getName());
        assertEquals("200", saved.getTimeAtLastDataFetch());
    }

    private void assertResponseFields(StockInfoResponseDto response) {
        assertEquals(42L, response.getId());
        assertEquals("INE123", response.getIsin());
        assertEquals("Example Ltd", response.getName());
        assertEquals("1720000000", response.getTimeAtLastDataFetch());
        assertEquals("EXAMPLE", response.getSymbol());
    }
}