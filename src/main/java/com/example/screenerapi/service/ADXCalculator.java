package com.example.screenerapi.service;
import java.util.*;

public class ADXCalculator {

    public static class PriceBar {
        double high;
        double low;
        double close;

        public PriceBar(double high, double low, double close) {
            this.high = high;
            this.low = low;
            this.close = close;
        }
    }

    public static class ADXResult {
        public List<Double> plusDI;
        public List<Double> minusDI;
        public List<Double> adx;

        public ADXResult(List<Double> plusDI, List<Double> minusDI, List<Double> adx) {
            this.plusDI = plusDI;
            this.minusDI = minusDI;
            this.adx = adx;
        }
    }

    public static ADXResult calculateADX(List<PriceBar> bars, int period) {
        List<Double> plusDM = new ArrayList<>();
        List<Double> minusDM = new ArrayList<>();
        List<Double> tr = new ArrayList<>();

        // Calculate True Range and Directional Movements
        for (int i = 1; i < bars.size(); i++) {
            PriceBar today = bars.get(i);
            PriceBar yesterday = bars.get(i - 1);

            double upMove = today.high - yesterday.high;
            double downMove = yesterday.low - today.low;

            plusDM.add((upMove > downMove && upMove > 0) ? upMove : 0);
            minusDM.add((downMove > upMove && downMove > 0) ? downMove : 0);

            double trVal = Math.max(today.high - today.low,
                                Math.max(Math.abs(today.high - yesterday.close),
                                         Math.abs(today.low - yesterday.close)));
            tr.add(trVal);
        }

        // Smooth the DM and TR values using Wilder’s smoothing technique
        List<Double> smPlusDM = smooth(plusDM, period);
        List<Double> smMinusDM = smooth(minusDM, period);
        List<Double> smTR = smooth(tr, period);

        List<Double> plusDI = new ArrayList<>();
        List<Double> minusDI = new ArrayList<>();
        List<Double> dx = new ArrayList<>();

        for (int i = 0; i < smTR.size(); i++) {
            double pDI = 100 * (smPlusDM.get(i) / smTR.get(i));
            double mDI = 100 * (smMinusDM.get(i) / smTR.get(i));
            plusDI.add(pDI);
            minusDI.add(mDI);

            double dxVal = 100 * Math.abs(pDI - mDI) / (pDI + mDI);
            dx.add(dxVal);
        }
        System.out.println("DX is :" +dx);
        List<Double> adx = smooth(dx, period);

        return new ADXResult(plusDI, minusDI, adx);
    }

    // Wilder’s smoothing method (EMA-like but simpler)
    private static List<Double> smooth(List<Double> values, int period) {
        List<Double> smoothed = new ArrayList<>();
        double sum = 0.0;

        for (int i = 0; i < values.size(); i++) {
            if (i < period) {
                sum += Double.isNaN(values.get(i))? 0: values.get(i);
                if (i == period - 1) {
                    smoothed.add(sum);
                } else {
                    smoothed.add(0.0);
                }
            } else {
                double prev = smoothed.get(i - 1);
                //double newSmoothed = prev - (prev / period) + values.get(i);
                double newSmoothed = prev + ((values.get(i) -prev)/period) ;
                smoothed.add(newSmoothed);
            }
        }
        return smoothed;
    }

    public static void main(String[] args) {
        List<PriceBar> bars = new ArrayList<>();
        // Add your historical OHLC data here:
        // bars.add(new PriceBar(high, low, close));

       // adding data oldest to newest, axis bank, 10:55AM to 13:10
       // Example dummy data
        bars.add(new PriceBar(1211.3, 1208.6, 1210.9));
        bars.add(new PriceBar(1211.4, 1209.9, 1210.4));
        bars.add(new PriceBar(1210.8, 1209.6, 1210.6));
        bars.add(new PriceBar(1211.2, 1210.2, 1211.1));
        bars.add(new PriceBar(1211.2, 1209.7, 1210.1));
        bars.add(new PriceBar(1212.0, 1209.1, 1211.2));
        bars.add(new PriceBar(1211.9, 1210.3, 1211.0));
        bars.add(new PriceBar(1211.6, 1210.4, 1211.1));
        bars.add(new PriceBar(1212.6, 1210.7, 1212.4));
        bars.add(new PriceBar(1213.8, 1211.6, 1213.8));
        bars.add(new PriceBar(1214.3, 1212.4, 1213.7));
        bars.add(new PriceBar(1213.8, 1211.9, 1212.1));
        bars.add(new PriceBar(1212.3, 1210.7, 1211.7));
        bars.add(new PriceBar(1211.8, 1210.0, 1210.7));
        bars.add(new PriceBar(1212.1, 1210.5, 1211.5));
        bars.add(new PriceBar(1212.6, 1211.1, 1212.0));
        bars.add(new PriceBar(1212.7, 1211.6, 1212.4));
        bars.add(new PriceBar(1212.7, 1212.0, 1212.6));
        bars.add(new PriceBar(1212.7, 1211.1, 1212.0));
        bars.add(new PriceBar(1212.5, 1211.2, 1211.8));
        bars.add(new PriceBar(1212.5, 1211.1, 1212.1));
        bars.add(new PriceBar(1213.6, 1212.1, 1213.1));
        bars.add(new PriceBar(1213.5, 1212.1, 1212.9));
        bars.add(new PriceBar(1213.9, 1212.4, 1213.6));
        bars.add(new PriceBar(1214.1, 1212.7, 1214.0));
        bars.add(new PriceBar(1214.0, 1212.9, 1213.6));
        bars.add(new PriceBar(1214.2, 1212.9, 1213.5));
        bars.add(new PriceBar(1214.3, 1213.2, 1213.8));

        int period = 14;
        System.out.println("ADX period: " +period);
        ADXResult result = calculateADX(bars, period);

        System.out.println("Plus DI: " + result.plusDI);
        System.out.println("Minus DI: " + result.minusDI);
        System.out.println("ADX: " + result.adx);
    }
}
