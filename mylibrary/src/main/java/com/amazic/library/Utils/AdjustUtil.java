package com.amazic.library.Utils;

import android.util.Log;

import androidx.annotation.Nullable;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAdRevenue;
import com.adjust.sdk.AdjustEvent;
import com.amazic.library.ads.admob.Admob;
import com.google.android.libraries.ads.mobile.sdk.common.AdValue;
import com.google.android.libraries.ads.mobile.sdk.common.AdSourceResponseInfo;

public class AdjustUtil {
    public static void trackRevenue(@Nullable AdSourceResponseInfo loadedAdSourceResponseInfo, AdValue adValue, String adUnitId, String adFormat) {
        String adName = "";
        if (loadedAdSourceResponseInfo != null)
            adName = loadedAdSourceResponseInfo.getName();
        double valueMicros = adValue.getValueMicros() / 1000000d;
        Log.d("AdjustRevenue", "adName: " + adName + " - valueMicros: " + valueMicros);
        // send ad revenue info to Adjust
        AdjustAdRevenue adRevenue = new AdjustAdRevenue("admob_sdk");
        adRevenue.setRevenue(valueMicros, adValue.getCurrencyCode());
        adRevenue.setAdRevenueNetwork(adName);
        adRevenue.setAdRevenueUnit(adUnitId);
        adRevenue.setAdRevenuePlacement(adFormat);
        adRevenue.addPartnerParameter("ad_unit_id", adUnitId);
        adRevenue.addPartnerParameter("ad_format", adFormat);
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
