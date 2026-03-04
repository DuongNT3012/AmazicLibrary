package com.amazic.library.ads.inter_ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.amazic.library.Utils.RemoteConfigHelper;
import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.ads.callback.InterCallback;
import com.amazic.library.ads.callback.NativeCallback;
import com.amazic.library.ads.native_ads.NativeAfterInterManager;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.nativead.NativeAd;

import java.util.HashMap;
import java.util.Map;

public class InterManager {
    private static final String TAG = "InterManager";
    public static final Map<String, InterstitialAd> listInter = new HashMap<>();

    public static void loadAndShowInterAds(Activity activity, String adsKey, String remoteKey, InterCallback interCallback) {
        Admob.getInstance().loadInterAdsLoadAndShow(activity, AdmobApi.getInstance().getListIDByName(adsKey), interCallback, remoteKey);
    }

    public static void loadAndShowInterAdsWithNativeAfterInter(Activity activity, String adsKeyInter, String remoteKeyInter, InterCallback interCallback, String adsKeyNative, String remoteKeyNative) {
        NativeAfterInterManager.preloadNativeAfterInter(activity, adsKeyNative, remoteKeyNative);
        Admob.getInstance().loadInterAdsLoadAndShowWithNativeAfterInter(activity, AdmobApi.getInstance().getListIDByName(adsKeyInter), interCallback, remoteKeyInter, remoteKeyNative);
    }

    public static void loadInterAds(Context context, String adsKey, String remoteKey) {
        if (listInter.get(adsKey) == null) {
            Admob.getInstance().loadInterAds(context, AdmobApi.getInstance().getListIDByName(adsKey), new InterCallback() {
                @Override
                public void onAdLoaded(InterstitialAd interstitialAd) {
                    super.onAdLoaded(interstitialAd);
                    listInter.put(adsKey, interstitialAd);
                    Log.d(TAG, "onAdLoaded: " + listInter);
                }
            }, remoteKey);
        } else {
            Log.d(TAG, "Inter already loaded. (inter != null)");
        }
    }

    public static void showInterAds(Activity activity, String adsKey, String remoteKey, InterCallback interCallback, boolean isShowLoading, boolean isReloadInterAfterShow) {
        Admob.getInstance().showInterAds(activity, listInter.get(adsKey), new InterCallback() {
            @Override
            public void onNextAction() {
                super.onNextAction();
                interCallback.onNextAction();
                listInter.put(adsKey, null);
                if (isReloadInterAfterShow) {
                    loadInterAds(activity, adsKey, remoteKey);
                }
                Log.d(TAG, "onNextAction: " + listInter);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
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
        }, isShowLoading, remoteKey);
    }

    //preload
    public static void loadInterAdPreload(Context context, String adsKey, String remoteKey) {
        Admob.getInstance().loadInterAdPreload(context, AdmobApi.getInstance().getListIDByName(adsKey), new InterCallback() {
            @Override
            public void onAdLoaded(InterstitialAd interstitialAd) {

            }
        }, remoteKey);
    }

    public static void showInterAdPreload(Activity activity, String adsKey, String remoteKeyInter, String remoteKeyNativeAfterInter, String adsKeyNativeAfterInter, InterCallback interCallback, boolean isShowLoading){
        NativeAfterInterManager.preloadNativeAfterInter(activity, adsKeyNativeAfterInter, remoteKeyNativeAfterInter);
        boolean isShowNativeAfterInter = RemoteConfigHelper.getInstance().get_config(activity, remoteKeyNativeAfterInter);

        Admob.getInstance().showInterAdPreload(activity, AdmobApi.getInstance().getListIDByName(adsKey), new InterCallback(){
            @Override
            public void onNextAction() {
                super.onNextAction();
                interCallback.onNextAction();
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
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
        }, isShowLoading, remoteKeyInter, isShowNativeAfterInter);
    }

}
