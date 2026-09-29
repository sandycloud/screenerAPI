package com.example.screenerapi.dto;

public class StockInfoResponseDto {
    private final Long id;
    private final String isin;
    private final String name;
    private final String timeAtLastDataFetch;
    private final String symbol;

    public StockInfoResponseDto(Long id, String isin, String name, String timeAtLastDataFetch, String symbol) {
        this.id = id;
        this.isin = isin;
        this.name = name;
        this.timeAtLastDataFetch = timeAtLastDataFetch;
        this.symbol = symbol;
    }

    public Long getId() {
        return id;
    }

    public String getIsin() {
        return isin;
    }

    public String getName() {
        return name;
    }

    public String getTimeAtLastDataFetch() {
        return timeAtLastDataFetch;
    }

    public String getSymbol() {
        return symbol;
    }
}