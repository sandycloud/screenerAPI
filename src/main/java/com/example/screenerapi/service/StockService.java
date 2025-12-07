package com.example.screenerapi.service;

import com.example.screenerapi.entity.StockPrice5Min;
import com.example.screenerapi.repository.StockPrice5MinRepository;
import com.example.screenerapi.util.StockPriceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.List;
import com.example.screenerapi.entity.StockAdxCriteriaDto;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONObject;

//latest
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class StockService {
    // Application-wide storage for fetched ADX criteria stocks
    private final List<StockAdxCriteriaDto> adxCriteriaStocks = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    private StockPrice5MinRepository repository;

    @Value("${adx.api.request.body}")
    private String adxApiRequestBody;

    private final WebClient webClient = WebClient.builder().build();
    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StockService.class);

    static String tagCandles = "candles";
    static final String prevFetchInputTime = "prevTimestamp";
    static final String metaTag = "meta";
    static final String dataTag ="data";
    static final String iSinString = "Isin";

    public void firstFetchAndStoreCandles(String stockName, String isin, String candleTimeFrame,
                       Long fromTime, String externalApiUrl) {
        //fetchCandleData();
        String url = externalApiUrl + "?instrumentKey=NSE_EQ%7C" + isin + "&interval=I" +
                candleTimeFrame + "&from=" + fromTime + "&limit=500";
        log.info("Fetching candles for  URL: {}",  url);

        //MultiValueMap<String, String> cookiesMap = new LinkedMultiValueMap<>();
        //String cookies = "__cf_bm=0tLCFrY52qSsonOp4O6AH5NwBvulzTs6VNKhzg70B.Q-1750396274-1.0.1.1-ISge_3Ymgn5jvUq84vpKxj9oJVDbzJCD_N4iGCw4mpTIe4p3qPgfms52ybi2gRm8; "
        //        + "__cfruid=bafd8de0edd320d88b21169c94d92dba464ea988-1750396274; _cfuvid=3AqjpdIOzaiBVZIyXwixxlhAG9pK0da1KPmwxeiarcU-1750396274795-0.0.1.1-604800000;";
        /*cookiesMap.add("__cf_bm","fGp2lUoATWn52R_82TrBOa.Xg6b8g4LrOsTSt8Maj9E-1750590216-1.0.1.1-EDrLtHkHRmR8YVLgvLBYz4WfVG.GcX8jdBdVXs._XJPKZnKiTMThaqJxRnyHIYxW");
        cookiesMap.add("__cfruid","9337d56c8f222b370b0abfd420795df0964b52dc-1750590216");
        cookiesMap.add("_cfuvid","NkUnlgloEx.lh7aQ1JaGHT95P_umQ6mJeRyBzm99VwA-1750590216367-0.0.1.1-604800000");

        ResponseSpec resp1 =  webClient.get()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("User-Agent", "PostmanRuntime/7.29.2")*/
                //.header("Accept", "*/*")
                /*//.header("Accept-Encoding", "gzip, deflate, br")
                //.header("Connection", "keep-alive" )
                //.header("Cookies", cookies)
                .cookies(multiValMap -> multiValMap.addAll(cookiesMap));*/
                //.retrieve();

        /*String responseBody = webClient.get()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("User-Agent", "PostmanRuntime/7.29.2")
                //.header("Accept", "* /*")
                //.header("Accept-Encoding", "gzip, deflate, br")
                //.header("Connection", "keep-alive" )
                //.header("Cookies", cookies)
                .cookies(multiValMap -> multiValMap.addAll(cookiesMap))
                .retrieve()
                .bodyToMono(String.class)
                .block();*/
        /*log.info("resp Spec :",resp1.toString());
        String responseBody = resp1.bodyToMono(String.class)
                .block();*/

        JSONObject respJson = fetchJsonDataUsingCurl(externalApiUrl,isin, candleTimeFrame, fromTime, "500");
        if (respJson != null) {
            log.info("Response body for ISIN {}: {}", isin, respJson.length());
            //JSONObject json = new JSONObject(responseBody);
            processResponse(respJson, candleTimeFrame, stockName, isin);

            //fetch previous 500 lines
            JSONObject data = respJson.optJSONObject(dataTag);
            JSONObject objMeta= data.optJSONObject(metaTag);
            long prevTime = objMeta.optLongObject(prevFetchInputTime);
            log.info("previous timevalue :{}", prevTime);
            respJson = fetchJsonDataUsingCurl(externalApiUrl,isin, candleTimeFrame, prevTime, "500");
            processResponse(respJson,candleTimeFrame,stockName, isin);

            /*JSONObject data = json.optJSONObject("data");
            log.info("Fetched data for ISIN: {}, data:{}" ,isin, data != null ? data.opt("meta") : null);
            log.info("hmmmm,,, candles data {}", data != null ? data.toString() : "null");
            if (data != null && data.has("candles")) {
                log.info("candles data for stock: {}, ISIN:" ,isin);
                JSONArray candles = data.getJSONArray("candles");
                List<StockPrice5Min> entities = new ArrayList<>();
                log.info("Number of candles fetched: {}", candles.length());
                for (int i = 0; i < candles.length(); i++) {
                    JSONArray candle = candles.getJSONArray(i);
                    StockPrice5Min entity = new StockPrice5Min();
                    entity.setStockName(stockName);
                    entity.setTimeInMillis(candle.getLong(0));
                    entity.setOpen(candle.getDouble(1));
                    entity.setHigh(candle.getDouble(2));
                    entity.setLow(candle.getDouble(3));
                    entity.setClose(candle.getDouble(4));
                    entity.setVolume(candle.getLong(5));
                    entity.setAlternateVal(candle.getDouble(6));
                    entities.add(entity);
                }
                repository.saveAll(entities);
            }*/
            //fetch previous 500 lines
            //get the prev time from response

        }
    }
    private void processResponse(JSONObject inputJson, String timeframe, String stockName, String isin){
        JSONObject data = inputJson.optJSONObject(dataTag);
        //log.info("Fetched data for ISIN: {}, data:{}" ,isin, data != null ? data.opt("meta") : null);
        log.info("hmmmm,,, candles data {}", data != null ? data.toString() : "null");
        if (data != null && data.has("candles")) {
            //log.info("candles data for stock: {}, ISIN:" ,isin);
            JSONArray candles = data.getJSONArray(tagCandles);
            List<StockPrice5Min> entities = null;
            if (timeframe.equals("5")) {
                entities = new ArrayList<>();
            }

            log.info("Number of candles fetched: {}", candles.length());
            for (int i = 0; i < candles.length(); i++) {
                JSONArray candle = candles.getJSONArray(i);
                Long timeInMillis = candle.getLong(0);

                StockPrice5Min entity = repository.findByTimeInMillisAndIsin(timeInMillis, isin)
                        .orElse(new StockPrice5Min());

                entity.setIsin(isin);
                entity.setTimeInMillis(timeInMillis);
                entity.setOpen(candle.getDouble(1));
                entity.setHigh(candle.getDouble(2));
                entity.setLow(candle.getDouble(3));
                entity.setClose(candle.getDouble(4));
                entity.setVolume(candle.getLong(5));
                entity.setAlternateVal(candle.getDouble(6));
                entity.setDatetimestamp(StockPriceUtil.
                        convertMillisToLocalDateTime(candle.getLong(0)).toString());
                log.info("local date time:{}", StockPriceUtil.
                        convertMillisToLocalDateTime(candle.getLong(0)).toString());
                entities.add(entity);
            }
            repository.saveAll(entities);
        }

    }

    public List<StockPrice5Min> getRecentCandles(String stockName, Long time, int rows) {
        return repository.findRecentCandles(stockName, time, rows);
    }


    public JSONObject fetchJsonDataUsingCurl(String externalApi, String isin, String timeFrame, long fromTime, String limit){
        String temp= externalApi.concat("?instrumentKey=NSE_EQ%7C").concat(isin);
        temp = temp.concat("&interval=I").concat(timeFrame).concat("&from=").concat(""+ fromTime)
                        .concat("&limit=").concat(limit);
        log.info("Url: {}", temp);
        return fetchJsonDataUsingCurl(temp);
    }

    private JSONObject fetchJsonDataUsingCurl(String url) {
        try {
            log.info("using curl command");
            // Build the curl command
            String[] command = {"curl", "--location", "--request", "GET", url};

            // Execute the command
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            log.info("starting curl command: {}", command);
            Process process = processBuilder.start();

            // Read the output from the command
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            log.info("curl response line: {}", line);

            // Wait for the process to complete
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Curl command failed with exit code: " + exitCode);
            }
            log.info("response length: {}", responseBuilder.length());
            // Convert the response to a JSONObject
            return new JSONObject(responseBuilder.toString());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("error:" + e.getMessage());
            throw new RuntimeException("Error executing curl command: " + e.getMessage(), e);
        }
    }

    public void fetchCandleData (){
        log.info("fetching using okHttpCLient");
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, "");
        Request request = new Request.Builder()
                .url("https://service.upstox.com/chart/open/v3/candles?instrumentKey=NSE_EQ%7CINE238A01034&interval=I15&from=1750530599999&limit=500")
                .method("GET",null)
                .addHeader("Cookie", "__cf_bm=fGp2lUoATWn52R_82TrBOa.Xg6b8g4LrOsTSt8Maj9E-1750590216-1.0.1.1-EDrLtHkHRmR8YVLgvLBYz4WfVG.GcX8jdBdVXs._XJPKZnKiTMThaqJxRnyHIYxW; __cfruid=9337d56c8f222b370b0abfd420795df0964b52dc-1750590216; _cfuvid=NkUnlgloEx.lh7aQ1JaGHT95P_umQ6mJeRyBzm99VwA-1750590216367-0.0.1.1-604800000")
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
            log.info("response: {}", response.toString());
        }catch (Exception e) {
            log.error("Error fetching candle data: {}", e.getMessage());
        }
    }
    public static String sendGetRequest(String baseUrl, Map<String, Object> params, String cookies)
            throws Exception {
        StringBuilder urlBuilder = new StringBuilder(baseUrl);

        // Add query parameters
        if (params != null && !params.isEmpty()) {
            urlBuilder.append("?");
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String temp = entry.getValue().toString();
                System.out.println("query params value " + temp);
                urlBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue().toString(),
                                StandardCharsets.UTF_8.name()))
                        .append("&");
            }
            urlBuilder.deleteCharAt(urlBuilder.length() - 1); // Remove trailing '&'
            System.out.println("finaly URL " + urlBuilder.toString());
        }

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set request method
        connection.setRequestMethod("GET");

        // Set request headers
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "PostmanRuntime/7.29.2");
        if (cookies != null && !cookies.isEmpty()) {
            connection.setRequestProperty("Cookie", cookies);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection
                    .getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            System.out.println("response data:" + response.toString());
            in.close();
            return response.toString();
        } else {
            throw new RuntimeException("HTTP GET Request Failed with Error code : " + responseCode);
        }
    }

    public static void main(String[] args) {
        try {
            String baseUrl = "https://service.upstox.com/chart/open/v3/candles";
            //"https://service.upstox.com/chart/open/v3/candles?instrumentKey=NSE_EQ%7CINE238A01034&interval=I15&from=1750530599999&limit=500";// Example HTTPS URL

            // Example query parameters
            Map<String, Object> queryParams = Map.of("instrumentKey", "NSE_EQ%7CINE238A01034",
                    "interval", "I15", "from", 1750530599999L,"limit", "500");

            // Example cookies string (format: "name1=value1; name2=value2")
            String cookies = "__cf_bm=e4FrZEbcdC1Gif3N9bCi2Hefyvrr.Y0xAzXdMfXkIuE-1750593510-1.0.1.1-xA9DotDrDOgW8E4Qk0vPJiRgKPVYjEOVmNXdGhTLEamw_FmLcm0XJlhObRmZXHaT; __cfruid=c6d8f1882f00ecf02d12ec521176ce8f60a6ed81-1750590586; _cfuvid=ywKghrNenzsE96IEaOHVP6w6e8nso5_4.9XrUiE4SAM-1750590586089-0.0.1.1-604800000";

            //String jsonResponse = sendGetRequest(baseUrl, queryParams, cookies);
            JSONObject jsonResponse= new StockService() .fetchJsonDataUsingCurl("https://service.upstox.com/chart/open/v3/candles","INE238A01034", "15",1750530599999L,"5");
            System.out.println("JSON Response:\n" + jsonResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calls external API for ADX criteria stocks, parses and stores the result application-wide.
     * @return The latest List of StockAdxCriteriaDto
     */
    public List<StockAdxCriteriaDto> fetchAdxCriteriaStocks() {
        String url = "https://scanx-analytics.dhan.co/customscan/fetchdt";
        // Request JSON loaded from application.properties
        String requestBody = adxApiRequestBody;

        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        //RequestBody body = RequestBody.create(mediaType, requestBody);
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
                log.info("ADX downtrend API, response no. of stocks: {}", dataArr.length());
                if (dataArr != null) {
                    for (int i = 0; i < dataArr.length(); i++) {
                        JSONObject stock = dataArr.getJSONObject(i);
                        String isin = stock.optString(iSinString, "");
                        //TODO: add constants for string literals
                        String sym = stock.optString("Sym", "");
                        double pPerchange = stock.optDouble("PPerchange", 0.0);
                        double pchange = stock.optDouble("Pchange", 0.0);
                        parsedStockList.add(new StockAdxCriteriaDto(isin, sym, pPerchange, pchange));
                    }
                }
                // update application-wide list
                synchronized (adxCriteriaStocks) {
                    adxCriteriaStocks.clear();
                    adxCriteriaStocks.addAll(parsedStockList);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching ADX criteria stocks: " + e.getMessage(), e);
        }
        return parsedStockList;
    }

    public List<StockAdxCriteriaDto> getAdxCriteriaStocks() {
        // return a defensive copy to prevent accidental modification
        synchronized (adxCriteriaStocks) {
            return new ArrayList<>(adxCriteriaStocks);
        }
    }
}
