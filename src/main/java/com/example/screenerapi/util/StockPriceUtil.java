package com.example.screenerapi.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.DayOfWeek;

public class StockPriceUtil {

    public static LocalDateTime convertMillisToLocalDateTime(Long timeInMillis) {
        if (timeInMillis == null) {
            throw new IllegalArgumentException("Time in millis cannot be null");
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timeInMillis), ZoneId.systemDefault());
    }

    /**
     * Calculates the previous business day at 9:00 AM in epoch milliseconds format.
     * Business days are Monday through Friday. Weekends (Saturday and Sunday) are skipped.
     * @return Epoch milliseconds for previous business day at 9:00 AM
     */
    public static Long getPreviousBusinessDay9AMInEpoch() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate previousBusinessDay = today.minusDays(1);
        
        // Keep going back until we find a business day (Monday-Friday)
        DayOfWeek dayOfWeek = previousBusinessDay.getDayOfWeek();
        while (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            previousBusinessDay = previousBusinessDay.minusDays(1);
            dayOfWeek = previousBusinessDay.getDayOfWeek();
        }
        
        LocalDateTime previousBusinessDay9AM = LocalDateTime.of(previousBusinessDay, LocalTime.of(9, 15));
        return previousBusinessDay9AM.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * Returns the current time in epoch milliseconds format.
     * @return Current time as epoch milliseconds
     */
    public static Long getCurrentTimeInEpoch() {
        return LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

}
