package com.example.screenerapi.service;

import com.example.screenerapi.entity.StockAdxCriteriaDto;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FilterStocks {

    private static final Logger log = LoggerFactory.getLogger(FilterStocks.class);
    private static final String dataTag = "data";
    private static final String iSinString = "Isin";

    @Value("${adx.uptrend5Min.request.body}")
    private String adxuptrend5MinApiRequestBody;

    @Value("${adx.downtrend5Min.request.body}")
    private String adxDowntrend5MinRequestBody;

    @Value("${adx.api.url}")
    private String adxApiUrl;

    /**
     * Calls external API for ADX criteria stocks and parses the result.
     * @return The latest List of StockAdxCriteriaDto
     */
    public List<StockAdxCriteriaDto> fetchAdxCriteriaStocksFromApi(String trend) {
        String url = adxApiUrl;
        // Request JSON loaded from application.properties
        String requestBody = null;
        if(trend.equalsIgnoreCase("down"))
            requestBody = adxDowntrend5MinRequestBody;
        else
            requestBody = adxuptrend5MinApiRequestBody;

        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(requestBody, mediaType);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        List<StockAdxCriteriaDto> parsedStockList = new ArrayList<>();
        log.info("before external API call");
        try (Response response = client.newCall(request).execute()) {
            log.info("call ADX uptrend API, response: {}", response.toString());
            if (response.body() != null) {
                String respStr = response.body().string();
                JSONObject root = new JSONObject(respStr);
                JSONArray dataArr = root.optJSONArray(dataTag);
                log.info("ADX downtrend API, response no. of stocks: {}", dataArr != null ? dataArr.length() : 0);
                if (dataArr != null) {
                    for (int i = 0; i < dataArr.length(); i++) {
                        JSONObject stock = dataArr.getJSONObject(i);
                        String isin = stock.optString(iSinString, "");
                        //TODO: add constants for string literals
                        String sym = stock.optString("Sym", "");
                            double prcPerChange = stock.optDouble("PPerchange", 0.0);
                            // Round to two decimal places
                            prcPerChange = Math.round(prcPerChange * 100.0) / 100.0;
                        double pchange = stock.optDouble("Pchange", 0.0);
                        Long volume = stock.optLong("Volume", 0L);
                        Long averageVolume = stock.optLong("AverageVolume", 0L);
                        parsedStockList.add(new StockAdxCriteriaDto(isin, sym, prcPerChange, pchange, volume, averageVolume));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error fetching ADX criteria stocks: " + e.getMessage(), e);
        }
        return parsedStockList;
    }
}