package com.example.screenerapi.service;

import com.example.screenerapi.entity.StockPrice5Min;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class AdxService {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdxService.class);
    private static final DateTimeFormatter LOCAL_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final ZoneOffset LOCAL_TIME_OFFSET = ZoneOffset.of("+05:30");

    public static class AdxResult {
        public double plusDI;
        public double minusDI;
        public double adx;
        public long timeInMillis;
        public double dxValue;
        public String localTime;
    }

    public List<AdxResult> calculateAdx_old(List<StockPrice5Min> candles, int period) {
        if (candles == null || candles.size() < period + 1) return Collections.emptyList();
        List<Double> trList = new ArrayList<>();
        List<Double> plusDMList = new ArrayList<>();
        List<Double> minusDMList = new ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            double high = candles.get(i).getHigh();
            double low = candles.get(i).getLow();
            double prevHigh = candles.get(i - 1).getHigh();
            double prevLow = candles.get(i - 1).getLow();
            double prevClose = candles.get(i - 1).getClose();
            double tr = Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
            trList.add(tr);
            double plusDM = high - prevHigh > prevLow - low && high - prevHigh > 0 ? high - prevHigh : 0;
            double minusDM = prevLow - low > high - prevHigh && prevLow - low > 0 ? prevLow - low : 0;
            plusDMList.add(plusDM);
            minusDMList.add(minusDM);
        }
        List<Double> atrList = smooth(trList, period);
        List<Double> plusDIList = new ArrayList<>();
        List<Double> minusDIList = new ArrayList<>();
        for (int i = 0; i < atrList.size(); i++) {
            double atr = atrList.get(i);
            double plusDI = atr == 0 ? 0 : 100 * smooth(plusDMList, period).get(i) / atr;
            double minusDI = atr == 0 ? 0 : 100 * smooth(minusDMList, period).get(i) / atr;
            plusDIList.add(plusDI);
            minusDIList.add(minusDI);
        }
        List<Double> dxList = new ArrayList<>();
        for (int i = 0; i < plusDIList.size(); i++) {
            double plusDI = plusDIList.get(i);
            double minusDI = minusDIList.get(i);
            double dx = (plusDI + minusDI) == 0 ? 0 : 100 * Math.abs(plusDI - minusDI) / (plusDI + minusDI);
            dxList.add(dx);
        }
        List<Double> adxList = smooth(dxList, period);
        List<AdxResult> result = new ArrayList<>();
        for (int i = 0; i < adxList.size(); i++) {
            AdxResult r = new AdxResult();
            r.plusDI = plusDIList.get(i);
            r.minusDI = minusDIList.get(i);
            r.adx = adxList.get(i);
            r.timeInMillis = candles.get(i + period).getTimeInMillis();
            result.add(r);
        }
        return result;
    }

    public List<AdxResult> calculateAdx(List<StockPrice5Min> candles, int period) {
        if (candles == null || candles.size() < period + 1) return Collections.emptyList();
        List<Double> trList = new ArrayList<>();
        List<Double> plusDMList = new ArrayList<>();
        List<Double> minusDMList = new ArrayList<>();
        log.info("candles size: " + candles.size());
        for (int i = 1; i < candles.size(); i++) {
            double high = candles.get(i).getHigh();
            double low = candles.get(i).getLow();
            double prevHigh = candles.get(i - 1).getHigh();
            double prevLow = candles.get(i - 1).getLow();
            double prevClose = candles.get(i - 1).getClose();
            double tr = Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
            trList.add(tr);
            double plusDM = high - prevHigh > prevLow - low && high - prevHigh > 0 ? high - prevHigh : 0;
            double minusDM = prevLow - low > high - prevHigh && prevLow - low > 0 ? prevLow - low : 0;
            plusDMList.add(plusDM);
            minusDMList.add(minusDM);
        }
        List<Double> atrList = smooth(trList, period);
        List<Double> plusDIList = new ArrayList<>();
        List<Double> minusDIList = new ArrayList<>();
        List<Double> smoothedPlusDM = smooth(plusDMList, period);
        List<Double> smoothedMinusDM = smooth(minusDMList, period);
        for (int i = 0; i < atrList.size(); i++) {
            double atr = atrList.get(i);
            double plusDI = atr == 0 ? 0 : 100 * smoothedPlusDM.get(i) / atr;
            double minusDI = atr == 0 ? 0 : 100 * smoothedMinusDM.get(i) / atr;
            plusDIList.add(plusDI);
            minusDIList.add(minusDI);
        }
        List<Double> dxList = new ArrayList<>();
        for (int i = 0; i < plusDIList.size(); i++) {
            double plusDI = plusDIList.get(i);
            double minusDI = minusDIList.get(i);
            double dx = (plusDI + minusDI) == 0 ? 0 : 100 * Math.abs(plusDI - minusDI) / (plusDI + minusDI);
            dxList.add(dx);
        }
        List<Double> adxList = smooth(dxList, period);
        int offset = period - 1; // shift from DI/DX index-space to ADX index-space

        List<AdxResult> result = new ArrayList<>();
        for (int i = 0; i < adxList.size(); i++) {
            int diIndex = i + offset; // correct index into plusDIList/minusDIList/dxList

            AdxResult r = new AdxResult();
            r.plusDI = plusDIList.get(diIndex);
            r.minusDI = minusDIList.get(diIndex);
            r.dxValue = dxList.get(diIndex);          // was never populated before, see below
            r.adx = adxList.get(i);
            r.timeInMillis = candles.get(diIndex + period).getTimeInMillis();
            /*r.localTime = Instant.ofEpochMilli(r.timeInMillis)
                    .atOffset(LOCAL_TIME_OFFSET)
                    .format(LOCAL_TIME_FORMAT); */
            r.localTime = candles.get(diIndex + period).getDatetimestamp();
            
            result.add(r);
        }

        return result;
    }

    private List<Double> smooth(List<Double> values, int period) {
        List<Double> result = new ArrayList<>();
        if (values.size() < period) return result;
        double sum = 0;
        for (int i = 0; i < period; i++) sum += values.get(i);
        result.add(sum / period);
        for (int i = period; i < values.size(); i++) {
            double prev = result.get(result.size() - 1);
            double val = ((prev * (period - 1)) + values.get(i)) / period;
            result.add(val);
        }
        return result;
    }

    public static void main(String args[]){

    }
}
