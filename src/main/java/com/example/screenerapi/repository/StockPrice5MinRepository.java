package com.example.screenerapi.repository;

import com.example.screenerapi.entity.StockPrice5Min;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockPrice5MinRepository extends JpaRepository<StockPrice5Min, Long> {
    List<StockPrice5Min> findByIsinAndTimeInMillisBetweenOrderByTimeInMillisAsc(String isin, Long from, Long to);

    @Query(value = "SELECT * FROM stock_price_5min WHERE isin = ?1 AND time_in_millis <= ?2 ORDER BY time_in_millis DESC LIMIT ?3", nativeQuery = true)
    List<StockPrice5Min> findRecentCandles(String isin, Long time, int rows);

    Optional<StockPrice5Min> findByTimeInMillis(Long timeInMillis);

    Optional<StockPrice5Min> findByTimeInMillisAndIsin(Long timeInMillis, String isin);
    Optional <StockPrice5Min> findByIsin (String isin);

}
