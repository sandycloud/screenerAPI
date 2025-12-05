package com.example.screenerapi.entity;

public class StockAdxCriteriaDto {
    private String isin;
    private String sym;
    private double pPerchange;
    private double pchange;

    // Default constructor
    public StockAdxCriteriaDto() {}

    // Parameterized constructor
    public StockAdxCriteriaDto(String isin, String sym, double pPerchange, double pchange) {
        this.isin = isin;
        this.sym = sym;
        this.pPerchange = pPerchange;
        this.pchange = pchange;
    }

    public String getIsin() {
        return isin;
    }
    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getSym() {
        return sym;
    }
    public void setSym(String sym) {
        this.sym = sym;
    }

    public double getPPerchange() {
        return pPerchange;
    }
    public void setPPerchange(double pPerchange) {
        this.pPerchange = pPerchange;
    }

    public double getPchange() {
        return pchange;
    }
    public void setPchange(double pchange) {
        this.pchange = pchange;
    }
}
