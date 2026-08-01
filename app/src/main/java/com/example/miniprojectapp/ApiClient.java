package com.example.miniprojectapp;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {

    public static final String BASE_URL = BuildConfig.API_BASE_URL;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static OkHttpClient client;

    public static OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }

    /**
     * POST request with JSON body
     */
    public static JSONObject post(String endpoint, JSONObject jsonBody) throws IOException, org.json.JSONException {
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = getClient().newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";
            return new JSONObject(responseBody);
        }
    }

    /**
     * POST with JWT auth header
     */
    public static JSONObject postAuth(String endpoint, JSONObject jsonBody, String token) throws IOException, org.json.JSONException {
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = getClient().newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";
            return new JSONObject(responseBody);
        }
    }

    /**
     * GET request with JWT auth header
     */
    public static JSONObject getAuth(String endpoint, String token) throws IOException, org.json.JSONException {
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = getClient().newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";
            return new JSONObject(responseBody);
        }
    }

    /**
     * GET request without auth
     */
    public static JSONObject get(String url) throws IOException, org.json.JSONException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = getClient().newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";
            return new JSONObject(responseBody);
        }
    }
}
