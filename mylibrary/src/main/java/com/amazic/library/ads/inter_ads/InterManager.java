package com.amazic.library.ads.inter_ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.callback.InterCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;

import java.util.HashMap;
import java.util.Map;

public class InterManager {
    private static final String TAG = "InterManager";
    private static final Map<String, InterstitialAd> listInter = new HashMap<>();

    public static void loadInterAds(Context context, String adsKey) {
        if (listInter.get(adsKey) == null) {
            Admob.getInstance().loadInterAds(context, AdmobApi.getInstance().getListIDByName(adsKey), new InterCallback() {
                @Override
                public void onAdLoaded(InterstitialAd interstitialAd) {
                    super.onAdLoaded(interstitialAd);
                    listInter.put(adsKey, interstitialAd);
                    Log.d(TAG, "onAdLoaded: " + listInter);
                }
            }, adsKey);
        } else {
            Log.d(TAG, "Inter already loaded. (inter != null)");
        }
    }

    public static void showInterAds(Activity activity, String adsKey, InterCallback interCallback, boolean isReloadInterAfterShow) {
        Admob.getInstance().showInterAds(activity, listInter.get(adsKey), new InterCallback() {
            @Override
            public void onNextAction() {
                super.onNextAction();
                interCallback.onNextAction();
                listInter.put(adsKey, null);
                if (isReloadInterAfterShow) {
                    loadInterAds(activity, adsKey);
                }
                Log.d(TAG, "onNextAction: " + listInter);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                interCallback.onAdClicked();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                interCallback.onAdDismissedFullScreenContent();
            }

            @Override
            public void onAdFailedToLoad() {
                super.onAdFailedToLoad();
                interCallback.onAdFailedToLoad();
            }

            @Override
            public void onAdFailedToShowFullScreenContent() {
                super.onAdFailedToShowFullScreenContent();
                interCallback.onAdFailedToShowFullScreenContent();
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                interCallback.onAdImpression();
            }

            @Override
            public void onAdLoaded(InterstitialAd interstitialAd) {
                super.onAdLoaded(interstitialAd);
                interCallback.onAdLoaded(interstitialAd);
            }

            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                interCallback.onAdShowedFullScreenContent();
            }
        }, adsKey);
    }
}
