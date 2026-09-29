package com.example.screenerapi.controller;

import com.example.screenerapi.dto.StockInfoResponseDto;
import com.example.screenerapi.service.StockInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockInfoController.class)
class StockInfoControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockInfoService stockInfoService;

    @Test
    void allEndpointReturnsDtoFields() throws Exception {
        when(stockInfoService.getAllStockInfo()).thenReturn(List.of(stockInfoResponse()));

        mockMvc.perform(get("/api/stockinfo/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].isin").value("INE123"))
                .andExpect(jsonPath("$[0].name").value("Example Ltd"))
                .andExpect(jsonPath("$[0].timeAtLastDataFetch").value("1720000000"))
                .andExpect(jsonPath("$[0].symbol").value("EXAMPLE"));
    }

    @Test
    void byIsinEndpointReturnsDtoOrNotFound() throws Exception {
        when(stockInfoService.getStockInfoByIsin("INE123"))
                .thenReturn(Optional.of(stockInfoResponse()));
        when(stockInfoService.getStockInfoByIsin("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/stockinfo/by-isin/INE123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isin").value("INE123"))
                .andExpect(jsonPath("$.symbol").value("EXAMPLE"));

        mockMvc.perform(get("/api/stockinfo/by-isin/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchEndpointReturnsDtoList() throws Exception {
        when(stockInfoService.searchStockInfoByName("Example"))
                .thenReturn(List.of(stockInfoResponse()));

        mockMvc.perform(get("/api/stockinfo/search").param("name", "Example"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isin").value("INE123"))
                .andExpect(jsonPath("$[0].name").value("Example Ltd"));
    }

    private StockInfoResponseDto stockInfoResponse() {
        return new StockInfoResponseDto(42L, "INE123", "Example Ltd", "1720000000", "EXAMPLE");
    }
}