package com.amazic.library.Utils;

import android.util.Log;

import androidx.annotation.Nullable;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAdRevenue;
import com.adjust.sdk.AdjustEvent;
import com.amazic.library.ads.admob.Admob;
import com.google.android.libraries.ads.mobile.sdk.common.AdSourceResponseInfo;
import com.google.android.libraries.ads.mobile.sdk.common.AdValue;

public class AdjustUtil {
    public static void trackRevenue(@Nullable AdSourceResponseInfo loadedAdSourceInfo, AdValue adValue) {
        String adName = "";
        if (loadedAdSourceInfo != null) {
            adName = loadedAdSourceInfo.getName();
        }
        double valueMicros = adValue.getValueMicros() / 1000000d;
        Log.d("AdjustRevenue", "adName: " + adName + " - valueMicros: " + valueMicros);
        // send ad revenue info to Adjust
        AdjustAdRevenue adRevenue = new AdjustAdRevenue("admob_sdk");
        adRevenue.setRevenue(valueMicros, adValue.getCurrencyCode());
        adRevenue.setAdRevenueNetwork(adName);

        Adjust.trackAdRevenue(adRevenue);

        Log.d("AdjustRevenue", "trackRevenue: " + adValue.getCurrencyCode());
        if (!Admob.getInstance().getTokenEventAdjust().isEmpty()) {
            AdjustEvent event = new AdjustEvent(Admob.getInstance().getTokenEventAdjust());
            event.setRevenue(valueMicros, adValue.getCurrencyCode());
            Adjust.trackEvent(event);
            Log.d("AdjustRevenue", "track revenue by event: " + Admob.getInstance().getTokenEventAdjust() + " - " + adValue.getCurrencyCode());
        }
    }
}
