package com.example.screenerapi.entity;

public class StockAdxCriteriaDto {
    private String isin;
    private String sym;
    private double pPerchange;
    private double pchange;
    private Long volume;
    private Long averageVolume;

    // Default constructor
    public StockAdxCriteriaDto() {}

    // Parameterized constructor
    public StockAdxCriteriaDto(String isin, String sym, double pPerchange, double pchange, Long volume, Long averageVolume) {
        this.isin = isin;
        this.sym = sym;
        this.pPerchange = pPerchange;
        this.pchange = pchange;
        this.volume = volume;
        this.averageVolume = averageVolume;
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

    public Long getVolume() {
        return volume;
    }
    public void setVolume(Long volume) {
        this.volume = volume;
    }

    public Long getAverageVolume() {
        return averageVolume;
    }
    public void setAverageVolume(Long averageVolume) {
        this.averageVolume = averageVolume;
    }
}
