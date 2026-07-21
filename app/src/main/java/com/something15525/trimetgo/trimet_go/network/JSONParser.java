package com.something15525.trimetgo.trimet_go.network;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class JSONParser {

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build();

    private final String tag = JSONParser.class.getSimpleName();

    public JSONObject fetch(String str) throws Exception {
        URI uri = URI.create(str);
        String scheme = uri.getScheme();
        if (scheme == null || !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Only HTTPS endpoints are allowed.");
        }

        Request request = new Request.Builder()
                .url(str)
                .addHeader("Accept", "application/json")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unsuccessful response code: " + response.code());
            }
            if (response.body() == null) {
                throw new IOException("Response body is null.");
            }
            String responseBody = response.body().string();
            if (responseBody.trim().isEmpty()) {
                throw new JSONException("Response body is empty.");
            }
            return new JSONObject(responseBody);
        } catch (Exception e4) {
            Log.e(this.tag, "Failed to fetch JSON", e4);
            throw e4;
        }
    }
}
