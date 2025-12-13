package com.example.screenerapi.service;

import com.example.screenerapi.entity.StockPrice5Min;
import com.example.screenerapi.entity.StockInfo;
import com.example.screenerapi.entity.StockAdxCriteriaDto;
import com.example.screenerapi.repository.StockPrice5MinRepository;
import com.example.screenerapi.repository.StockInfoRepository;
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
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.*;

import org.json.JSONArray;
import org.json.JSONObject;

//latest
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.URL;

@Service
public class StockService {
    // Application-wide storage for fetched ADX criteria stocks
    private final List<StockAdxCriteriaDto> adxCriteriaStocks = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    private StockPrice5MinRepository repository;

    @Autowired
    private StockInfoRepository stockInfoRepository;

    @Autowired
    private FilterStocks filterStocks;

    @Value("${external.api.url}")
    private String externalApiUrl;

    @Value("${priceData.fetch.limit}")
    private int priceDataFetchLimit;

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
     * Then processes each stock with thread pool and returns filtered list.
     * @param timeframe The timeframe for fetching stock data
     * @return The filtered List of StockAdxCriteriaDto based on volume criteria
     */
    public List<StockAdxCriteriaDto> fetchAdxCriteriaStocks(String timeframe, String trend) {
        // First, fetch the stocks matching filter from the External API
        List<StockAdxCriteriaDto> parsedStockList = filterStocks
                    .fetchAdxCriteriaStocksFromApi(trend);

        // update application-wide list
        synchronized (adxCriteriaStocks) {
            adxCriteriaStocks.clear();
            adxCriteriaStocks.addAll(parsedStockList);
        }

        if (parsedStockList.isEmpty()) {
            log.warn("No stocks found from ADX criteria API");
            return parsedStockList;
        }

        log.info("Starting thread pool processing for {} stocks with timeframe: {}", parsedStockList.size(), timeframe);

        // Create a blocking queue for communication between threads
        BlockingQueue<ProcessedStock> processedStocksQueue = new LinkedBlockingQueue<>();
        
        // Thread pool with 2 threads
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        
        // Thread 1: Fetch and persist stock data
        Future<?> thread1Future = executorService.submit(() -> {
            processStocksDataFetch(parsedStockList, timeframe, processedStocksQueue);
        });
        
        // Thread 2: Calculate volume moving average
        Future<?> thread2Future = executorService.submit(() -> {
            processVolumeMovingAverage(processedStocksQueue, parsedStockList.size());
        });

        // Wait for both threads to complete
        try {
            thread1Future.get();
            thread2Future.get();
        } catch (Exception e) {
            log.error("Error in thread pool execution: " + e.getMessage(), e);
        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Main thread: Filter stocks based on volume criteria
        return filterStocksByVolumeCriteria(parsedStockList);
    }

    /**
     * Helper class to pass processed stock information between threads
     */
    private static class ProcessedStock {
        String isin;
        String sym;
        boolean processed;
        
        ProcessedStock(String isin, String sym) {
            this.isin = isin;
            this.sym = sym;
            this.processed = false;
        }
    }

    /**
     * Thread 1: Fetch and persist stock data for each stock
     */
    private void processStocksDataFetch(List<StockAdxCriteriaDto> stocks, String timeframe, 
                                BlockingQueue<ProcessedStock> processedQueue) {
        for (StockAdxCriteriaDto stock : stocks) {
            try {
                String isin = stock.getIsin();
                String sym = stock.getSym();
                log.info("Processing stock: ISIN={}, Symbol={}", isin, sym);

                // Get last fetch time from StockInfo
                //StockInfo stockInfo = stockInfoRepository.findByIsin(isin);
                Long fromTime;
                
                /*if (stockInfo != null && stockInfo.getTimeAtLastDataFetch() != null && 
                    !stockInfo.getTimeAtLastDataFetch().isEmpty()) {
                    try {
                        fromTime = Long.parseLong(stockInfo.getTimeAtLastDataFetch()) +10000;
                    } catch (NumberFormatException e) {
                        log.warn("Invalid timeAtLastDataFetch format for ISIN: {}, using previous business day", isin);
                        fromTime = StockPriceUtil.getPreviousBusinessDay9AMInEpoch();
                    }
                } else {
                    // Use previous business day 9:15 AM
                    fromTime = StockPriceUtil.getPreviousBusinessDay9AMInEpoch();
                    log.info("No previous fetch time for ISIN: {}, using previous business day 9:00 AM: {}", isin, fromTime);
                }*/
                fromTime = StockPriceUtil.getCurrentTimeInEpoch();
                // Fetch stock data using curl
                JSONObject respJson = fetchJsonDataUsingCurl(externalApiUrl, isin, timeframe, fromTime, String.valueOf(priceDataFetchLimit));
                log.info("fetched stock price ,vol data from exteranl API symbol={}", sym);
                if (respJson != null) {
                    // Process and persist the response
                    Long lastCandleCloseTime = processResponseAndGetLastCandleTime(respJson, timeframe, sym, isin);
                    
                    // Update StockInfo with last candle close time
                    /*if (lastCandleCloseTime != null) {
                        if (stockInfo == null) {
                            stockInfo = new StockInfo();
                            stockInfo.setIsin(isin);
                            stockInfo.setName(sym);
                        }
                        stockInfo.setTimeAtLastDataFetch(String.valueOf(lastCandleCloseTime));
                        stockInfoRepository.save(stockInfo);
                        log.info("Updated StockInfo for ISIN: {} with last candle time: {}", isin, lastCandleCloseTime);
                    }*/
                    
                    // Signal Thread 2 that this stock is ready for processing
                    processedQueue.put(new ProcessedStock(isin, sym));
                    log.info("Stock processing completed for ISIN: {}", isin);
                }
            } catch (Exception e) {
                log.error("Error processing stock ISIN: {}, Symbol: {} - {}", 
                         stock.getIsin(), stock.getSym(), e.getMessage(), e);
            }
        }
        
        // Signal completion by adding a sentinel (null marker)
        try {
            processedQueue.put(new ProcessedStock(null, null));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread 1 interrupted while signaling completion", e);
        }
    }

    /**
     * Thread 2: Calculate 15-period volume moving average as stocks are processed
     */
    private void processVolumeMovingAverage(BlockingQueue<ProcessedStock> processedQueue, int totalStocks) {
        int processedCount = 0;
        boolean done = false;
        
        while (!done && processedCount < totalStocks) {
            try {
                ProcessedStock processedStock = processedQueue.take();
                
                // Check for completion sentinel
                if (processedStock.isin == null) {
                    done = true;
                    break;
                }
                
                String isin = processedStock.isin;
                log.info("Calculating volume moving average for ISIN: {}", isin);
                
                // Get the latest 15 candles for this stock, ordered by time descending
                List<StockPrice5Min> candles = repository.findRecentCandles(isin, System.currentTimeMillis(), 15);
                
                if (candles.size() >= 15) {
                    // Calculate 15-period volume moving average (average of last 15 candles)
                    long sum = 0;
                    int validVolumes = 0;
                    for (StockPrice5Min candle : candles) {
                        if (candle.getVolume() != null) {
                            sum += candle.getVolume();
                            validVolumes++;
                        }
                    }
                    
                    if (validVolumes > 0) {
                        long averageVolume = sum / validVolumes;
                        
                        // Update the latest candle with the average volume
                        StockPrice5Min latestCandle = candles.get(0); // findRecentCandles returns DESC order (latest first)
                        latestCandle.setAverageVolume(averageVolume);
                        repository.save(latestCandle);
                        
                        log.info("Updated average volume for ISIN: {} = {} (based on {} candles)", 
                                isin, averageVolume, validVolumes);
                    }
                } else {
                    log.warn("Not enough candles (have {}, need 15) for ISIN: {}", candles.size(), isin);
                    long sum = 0;
                    int validVolumes = 0;
                    for (StockPrice5Min candle : candles) {
                        if (candle.getVolume() != null) {
                            sum += candle.getVolume();
                            validVolumes++;
                        }
                    }
                    if (validVolumes > 0) {
                        long averageVolume = sum / validVolumes;
                        
                        // Update the latest candle with the average volume
                        StockPrice5Min latestCandle = candles.get(0); // findRecentCandles returns DESC order (latest first)
                        latestCandle.setAverageVolume(averageVolume);
                        repository.save(latestCandle);
                        
                        log.info("Updated average volume for ISIN: {} = {} (based on {} candles)", 
                                isin, averageVolume, validVolumes);
                    }
                }
                
                processedCount++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread 2 interrupted", e);
                break;
            } catch (Exception e) {
                log.error("Error calculating volume moving average: {}", e.getMessage(), e);
                processedCount++; // Continue processing other stocks
            }
        }
        log.info("Volume moving average calculation completed for {} stocks", processedCount);
    }

    /**
     * Main thread: Filter stocks based on volume criteria
     * Returns stocks where latest Volume >= 0.75 * Latest Average Volume
     */
    private List<StockAdxCriteriaDto> filterStocksByVolumeCriteria(List<StockAdxCriteriaDto> stocks) {
        List<StockAdxCriteriaDto> filteredStocks = new ArrayList<>();
        
        for (StockAdxCriteriaDto stock : stocks) {
            try {
                // Get the latest candle for this stock
                List<StockPrice5Min> latestCandles = repository.findRecentCandles(stock.getIsin(), 
                                                                                  System.currentTimeMillis(), 1);
                
                if (!latestCandles.isEmpty()) {
                    StockPrice5Min latestCandle = latestCandles.get(0);
                    
                    if (latestCandle.getVolume() != null && latestCandle.getAverageVolume() != null) {
                        long volume = latestCandle.getVolume();
                        long avgVolume = latestCandle.getAverageVolume();

                        // Check if volume >= 0.75 * average volume
                        if (avgVolume > 0 && volume >= (0.75 * avgVolume)) {
                            // Set volume and average volume on the DTO
                            stock.setVolume(volume);
                            stock.setAverageVolume(avgVolume);
                            filteredStocks.add(stock);
                            log.info("Stock passed volume criteria: ISIN={}, Volume={}, AvgVolume={}",
                                    stock.getIsin(), volume, avgVolume);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error filtering stock ISIN: {} - {}", stock.getIsin(), e.getMessage(), e);
            }
        }
        
        log.info("Filtered {} stocks out of {} based on volume criteria", filteredStocks.size(), stocks.size());
        return filteredStocks;
    }

    /**
     * Process response and return the last candle close time
     * Returns null if no candles found
     */
    private Long processResponseAndGetLastCandleTime(JSONObject inputJson, String timeframe, 
                                                     String stockName, String isin) {
        JSONObject data = inputJson.optJSONObject(dataTag);
        Long lastCandleTime = null;
        
        if (data != null && data.has("candles")) {
            JSONArray candles = data.getJSONArray(tagCandles);
            List<StockPrice5Min> entities = new ArrayList<>();
            
            log.info("Number of candles fetched: {}", candles.length());
            //the upstox api returns candles in reverse chronological order,
            //so first candle is the latest candle.So use the time of 1st candle.
            if(candles != null && candles.length() >0)
                lastCandleTime = candles.getJSONArray(0).getLong(0);
            
            // Commented out original sequential processing for performance improvement
            /*
            for (int i = 0; i < candles.length(); i++) {
                JSONArray candle = candles.getJSONArray(i);
                Long timeInMillis = candle.getLong(0);
                StockPrice5Min entity;
                //if(i ==0)
                entity = repository.findByTimeInMillisAndIsin(timeInMillis, isin)
                        .orElse(new StockPrice5Min());
                //else
                //    entity = new StockPrice5Min();

                entity.setIsin(isin);
                entity.setTimeInMillis(timeInMillis);
                entity.setOpen(candle.getDouble(1));
                entity.setHigh(candle.getDouble(2));
                entity.setLow(candle.getDouble(3));
                entity.setClose(candle.getDouble(4));
                entity.setVolume(candle.getLong(5));
                entity.setAlternateVal(candle.getDouble(6));
                entity.setDatetimestamp(StockPriceUtil.convertMillisToLocalDateTime(timeInMillis).toString());
                entities.add(entity);

            }
            */

            // More efficient parallel processing using Java streams
            entities = java.util.stream.IntStream.range(0, candles.length())
                    .parallel()
                    .mapToObj(i -> {
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
                        entity.setDatetimestamp(StockPriceUtil.convertMillisToLocalDateTime(timeInMillis).toString());

                        return entity;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            repository.saveAll(entities);
            repository.flush();
            entities.clear();
            entities = null;
        }
        
        return lastCandleTime;
        //return StockPriceUtil.getCurrentTimeInEpoch();
    }

    public List<StockAdxCriteriaDto> getAdxCriteriaStocks() {
        // return a defensive copy to prevent accidental modification
        synchronized (adxCriteriaStocks) {
            return new ArrayList<>(adxCriteriaStocks);
        }
    }
}
