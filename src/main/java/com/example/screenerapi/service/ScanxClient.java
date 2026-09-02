package com.example.screenerapi.service;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class ScanxClient {
    private static final MediaType JSON = MediaType.parse("application/json");
    private final OkHttpClient client = new OkHttpClient();
    private static final Logger log = LoggerFactory.getLogger(PriorityStockProcessor.class);

    @Value("${scanx.auth:}")
    private String authToken;

    public List<ScanxStock> fetch(String url, String requestBody) {
        if (authToken == null || authToken.isBlank()) {
            throw new IllegalStateException("Missing ScanX auth token. Pass it as a command-line argument: --scanx.auth=<token>");
        }

        Request request = new Request.Builder()
                .url(url)
                //.post(RequestBody.create(requestBody, JSON))
                .method("POST", RequestBody.create(requestBody, JSON))
                //.header("Content-Type", "text/plain")
                //.header("Content-Type", "application/json")
                //.header("Auth", authToken)
                .addHeader("Content-Type", "application/json")
                .build();

        log.info("URL: {} ;request body: {}",url,requestBody);
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IllegalStateException("ScanX request failed with HTTP status " + response.code());
            }
            String responseBody = response.body().string();
            log.info("response length :{}", responseBody.length());
            response.close();
            //return parse(responseBody);
            return parseOld(responseBody);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to fetch ScanX stock list", exception);
        }finally {
            request =null;
        }
    }

    public List<ScanxStock> fetchNew(String url, String requestBody) {
        if (authToken == null || authToken.isBlank()) {
            throw new IllegalStateException("Missing ScanX auth token. Pass it as a command-line argument: --scanx.auth=<token>");
        }

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody, JSON))
                .header("Content-Type", "application/json")
                //.header("Auth", authToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IllegalStateException("ScanX request failed with HTTP status " + response.code());
            }
            return parse(response.body().string());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to fetch ScanX stock list", exception);
        }finally {
            request =null;
        }
    }

    static List<ScanxStock> parse(String responseBody) {
        JSONObject root = new JSONObject(responseBody);
        if (root.optInt("code", 0) != 0) {
            throw new IllegalStateException("ScanX returned code " + root.optInt("code"));
        }

        JSONArray headers = root.optJSONArray("headers");
        JSONArray rows = root.optJSONArray("data");
        if (headers == null || rows == null) {
            return new ArrayList<>();
        }

        Map<String, Integer> positions = new HashMap<>();
        for (int index = 0; index < headers.length(); index++) {
            positions.put(headers.getString(index), index);
        }

        List<ScanxStock> stocks = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rows.length(); rowIndex++) {
            Object rawRow = rows.get(rowIndex);
            if (!(rawRow instanceof JSONArray)) {
                continue;
            }
            JSONArray row = (JSONArray) rawRow;
            if (row.length() < headers.length()) {
                continue;
            }
            String isin = stringValue(row, positions.get("Isin"));
            if (isin == null || isin.isBlank()) {
                continue;
            }
            stocks.add(new ScanxStock(
                    isin,
                    stringValue(row, positions.get("Sym")),
                    stringValue(row, positions.get("DispSym")),
                    values(row, headers)));
        }
        return stocks;
    }

    static List<ScanxStock> parseOld(String responseBody) {
        JSONObject root = new JSONObject(responseBody);
        if (root.optInt("code", 0) != 0) {
            throw new IllegalStateException("ScanX returned code " + root.optInt("code"));
        }

        JSONArray rows = root.optJSONArray("data");
        if (rows == null) {
            return new ArrayList<>();
        }

        List<ScanxStock> stocks = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rows.length(); rowIndex++) {
            Object rawRow = rows.get(rowIndex);
            if (!(rawRow instanceof JSONObject)) {
                continue;
            }
            JSONObject row = (JSONObject) rawRow;
            String isin = row.optString("Isin", null);
            if (isin == null || isin.isBlank()) {
                continue;
            }
            stocks.add(new ScanxStock(
                    isin,
                    row.optString("Sym", null),
                    row.optString("DispSym", null),
                    row.toMap()));
        }
        return stocks;
    }

    private static Map<String, Object> values(JSONArray row, JSONArray headers) {
        Map<String, Object> values = new HashMap<>();
        for (int index = 0; index < headers.length() && index < row.length(); index++) {
            values.put(headers.getString(index), row.isNull(index) ? null : row.get(index));
        }
        return values;
    }

    private static String stringValue(JSONArray row, Integer position) {
        if (position == null || position < 0 || position >= row.length() || row.isNull(position)) {
            return null;
        }
        return String.valueOf(row.get(position));
    }
}