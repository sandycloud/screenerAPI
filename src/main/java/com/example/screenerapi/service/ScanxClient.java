package com.example.screenerapi.service;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScanxClient {
    private static final MediaType JSON = MediaType.parse("application/json");
    private final OkHttpClient client = new OkHttpClient();

    public List<ScanxStock> fetch(String url, String requestBody) {
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody, JSON))
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IllegalStateException("ScanX request failed with HTTP status " + response.code());
            }
            return parse(response.body().string());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to fetch ScanX stock list", exception);
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
            JSONArray row = rows.getJSONArray(rowIndex);
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