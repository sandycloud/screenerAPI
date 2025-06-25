package com.example.screenerapi.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class StockPriceUtil {

    public static LocalDateTime convertMillisToLocalDateTime(Long timeInMillis) {
        if (timeInMillis == null) {
            throw new IllegalArgumentException("Time in millis cannot be null");
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timeInMillis), ZoneId.systemDefault());
    }

}
