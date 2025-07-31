package com.amazic.library.ads.splash_ads;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeoChecker {

    private static final Set<String> EU_COUNTRIES = new HashSet<>(Arrays.asList(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE",
            "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT",
            "RO", "SK", "SI", "ES", "SE"
    ));

    public interface GeoCallback {
        void onResult(boolean isInEurope);
    }

    public static void isDeviceInEurope(Context context, GeoCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            boolean result = false;
            try {
                URL url = new URL("http://ip-api.com/json/");
                JSONObject json = getJsonObject(url);
                String countryCode = json.optString("countryCode", "").toUpperCase();

                result = EU_COUNTRIES.contains(countryCode);

            } catch (Exception e) {
                Log.e("GeoChecker", "Error checking EU status", e);
            }

            boolean finalResult = result;
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(finalResult));
        });
    }

    @NonNull
    private static JSONObject getJsonObject(URL url) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("GET");
        connection.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder responseBuilder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            responseBuilder.append(line);
        }

        reader.close();

        JSONObject json = new JSONObject(responseBuilder.toString());
        return json;
    }
}
