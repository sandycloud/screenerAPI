package com.example.screenerapi.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "stock_price_5min", indexes = {
        @Index(name = "idx_isin_time", columnList = "isin, timeInMillis")
})
public class StockPrice5Min {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String isin;

    @Column(nullable = false)
    private Long timeInMillis;

    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Long volume;
    private Double alternateVal;

    @Column(nullable = true)
    private String datetimestamp;

    @Column(precision = 5)
    private Double adxValue;

    @Column( precision = 5)
    private Double plusDIValue;

    @Column( precision = 5)
    private Double minusDIValue;

    @Column(nullable = true)
    private Long averageVolume;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIsin() { return isin; }
    public void setIsin(String isin) { this.isin = isin; }
    public Long getTimeInMillis() { return timeInMillis; }
    public void setTimeInMillis(Long timeInMillis) { this.timeInMillis = timeInMillis; }
    public Double getOpen() { return open; }
    public void setOpen(Double open) { this.open = open; }
    public Double getHigh() { return high; }
    public void setHigh(Double high) { this.high = high; }
    public Double getLow() { return low; }
    public void setLow(Double low) { this.low = low; }
    public Double getClose() { return close; }
    public void setClose(Double close) { this.close = close; }
    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }
    public Double getAlternateVal() { return alternateVal; }
    public void setAlternateVal(Double alternateVal) { this.alternateVal = alternateVal; }
    public String getDatetimestamp() { return datetimestamp; }
    public void setDatetimestamp(String datetimestamp) { this.datetimestamp = datetimestamp; }

    public Double getAdxValue() { return adxValue; }
    public void setAdxValue(Double adxValue) { this.adxValue = adxValue; }
    public Double getPlusDIValue() { return plusDIValue; }
    public void setPlusDIValue(Double plusDIValue) { this.plusDIValue = plusDIValue; }
    public Double getMinusDIValue() { return minusDIValue; }
    public void setMinusDIValue(Double minusDIValue) { this.minusDIValue = minusDIValue; }
    public Long getAverageVolume() { return averageVolume; }
    public void setAverageVolume(Long averageVolume) { this.averageVolume = averageVolume; }
}
