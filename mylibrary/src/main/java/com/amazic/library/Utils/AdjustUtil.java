package com.amazic.library.Utils;

import android.util.Log;

import androidx.annotation.Nullable;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAdRevenue;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.AdjustEvent;
import com.amazic.library.ads.admob.Admob;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdapterResponseInfo;

public class AdjustUtil {
    public static void trackRevenue(@Nullable AdapterResponseInfo loadedAdapterResponseInfo, AdValue adValue) {
        String adName = "";
        if (loadedAdapterResponseInfo != null)
            adName = loadedAdapterResponseInfo.getAdSourceName();
        double valueMicros = adValue.getValueMicros() / 1000000d;
        Log.d("AdjustRevenue", "adName: " + adName + " - valueMicros: " + valueMicros);
        // send ad revenue info to Adjust
        AdjustAdRevenue adRevenue = new AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB);
        adRevenue.setRevenue(valueMicros, adValue.getCurrencyCode());
        adRevenue.setAdRevenueNetwork(adName);
        Adjust.trackAdRevenue(adRevenue);
        Log.d("AdjustRevenue", "trackRevenue: " + adValue.getCurrencyCode());
        if (!Admob.getInstance().getTokenEventAdjust().equals("")) {
            AdjustEvent event = new AdjustEvent(Admob.getInstance().getTokenEventAdjust());
            event.setRevenue(valueMicros, adValue.getCurrencyCode());
            Adjust.trackEvent(event);
            Log.d("AdjustRevenue", "track revenue by event: " + Admob.getInstance().getTokenEventAdjust() + " - " + adValue.getCurrencyCode());
        }
    }
}
