package com.something15525.trimetgo.trimet_go.network;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class JSONParser {

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    private static JSONObject cachedJson = null;

    private static String lastResponseBody = "";

    private final String tag = JSONParser.class.getSimpleName();

    public JSONObject fetch(String str) throws Exception {
        Request request = new Request.Builder()
                .url(str)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            try (InputStream inputStream = response.body().byteStream()) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "iso-8859-1"), 8);
                StringBuilder sb = new StringBuilder();
                while (true) {
                    String line = bufferedReader.readLine();
                    if (line == null) {
                        break;
                    }
                    sb.append(line).append("\n");
                }
                lastResponseBody = sb.toString();
                try {
                    cachedJson = new JSONObject(lastResponseBody);
                } catch (JSONException e2) {
                    Log.e(this.tag, "Error parsing data " + e2.toString());
                }
                return cachedJson;
            } catch (Exception e3) {
                Log.e(this.tag, "Error reading stream " + e3.getMessage());
                throw e3;
            }
        } catch (Exception e4) {
            Log.e(this.tag, e4.getMessage());
            throw e4;
        }
    }
}
