package com.example.screenerapi.service;

import com.example.screenerapi.entity.StockPrice5Min;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AdxService {
    public static class AdxResult {
        public double plusDI;
        public double minusDI;
        public double adx;
        public long timeInMillis;
    }

    public List<AdxResult> calculateAdx(List<StockPrice5Min> candles, int period) {
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

    private List<Double> smooth(List<Double> values, int period) {
        List<Double> result = new ArrayList<>();
        if (values.size() < period) return result;
        double sum = 0;
        for (int i = 0; i < period; i++) sum += values.get(i);
        result.add(sum / period);
        for (int i = period; i < values.size(); i++) {
            double prev = result.get(result.size() - 1);
            double val = (prev * (period - 1) + values.get(i)) / period;
            result.add(val);
        }
        return result;
    }
}
