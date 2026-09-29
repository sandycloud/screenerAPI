package com.example.screenerapi.repository;

import com.example.screenerapi.entity.StockInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockInfoRepository extends JpaRepository<StockInfo, Long> {
    Optional<StockInfo> findByIsin(String isin);
    List<StockInfo> findByNameContainingIgnoreCase(String name);

    List<StockInfo> findByIsinIn(List<String> isins);
}
