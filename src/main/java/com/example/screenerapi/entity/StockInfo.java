package com.example.screenerapi.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "stock_info", indexes = {
    @Index(name = "idx_isin", columnList = "isin")
})
public class StockInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String isin;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String timeAtLastDataFetch;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIsin() { return isin; }
    public void setIsin(String isin) { this.isin = isin; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTimeAtLastDataFetch () { return timeAtLastDataFetch;}
    public void setTimeAtLastDataFetch (String timeAtLastDataFetch) { this.timeAtLastDataFetch = timeAtLastDataFetch;}

}
