package com.example.screenerapi.service;

import java.util.Map;

public class ScanxStock {
    private final String isin;
    private final String symbol;
    private final String displayName;
    private final Map<String, Object> values;

    public ScanxStock(String isin, String symbol, String displayName, Map<String, Object> values) {
        this.isin = isin;
        this.symbol = symbol;
        this.displayName = displayName;
        this.values = values;
    }

    public String getIsin() { return isin; }
    public String getSymbol() { return symbol; }
    public String getDisplayName() { return displayName; }
    public Map<String, Object> getValues() { return values; }
}