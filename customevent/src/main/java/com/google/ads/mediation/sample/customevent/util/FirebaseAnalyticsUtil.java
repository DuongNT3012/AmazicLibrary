package com.google.ads.mediation.sample.customevent.util;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

public class FirebaseAnalyticsUtil {
    public static String INTER = "inter";
    public static String BANNER = "banner";
    public static String NATIVE = "native";
    public static String REWARD = "reward";
    private static final String TAG = "FirebaseAnalyticsUtil";

    public static void logEventMediationAdmob(Context context, String adsType) {
        Bundle bundle = new Bundle();
        Log.e(TAG, "Mediation Admob :" + adsType);
        FirebaseAnalytics.getInstance(context).logEvent("mediation_admob: " + adsType, bundle);
    }

    public static void logEventMediationAdx(Context context, String adsType) {
        Bundle bundle = new Bundle();
        Log.e(TAG, "MediationAdmob Adx :" + adsType);
        FirebaseAnalytics.getInstance(context).logEvent("mediation_adx: " + adsType, bundle);
    }

    public static void logEventMediationAdx(Context context, String name, Bundle bundle) {
        Log.e(TAG, "MediationAdmob Adx :" + name);
        FirebaseAnalytics.getInstance(context).logEvent(name, bundle);
    }
}
