package com.amazic.library.Utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkUtil {
    private static boolean isConnectedNetwork(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = manager.getActiveNetworkInfo();
        return network != null && network.isConnected();
    }

    public static boolean isNetworkActive(Context context) {
        return context != null && isConnectedNetwork(context);
    }
}
