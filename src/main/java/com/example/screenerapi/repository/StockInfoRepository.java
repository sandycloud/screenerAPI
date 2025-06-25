package com.example.screenerapi.repository;

import com.example.screenerapi.entity.StockInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StockInfoRepository extends JpaRepository<StockInfo, Long> {
    StockInfo findByIsin(String isin);
    List<StockInfo> findByNameContainingIgnoreCase(String name);
}
