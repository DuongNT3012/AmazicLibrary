package com.amazic.library.ads.native_ads;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.appcompat.widget.AppCompatButton;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.callback.NativeCallback;
import com.amazic.library.ads.splash_ads.AsyncSplash;
import com.amazic.library.view.NativeAfterInterActivity;
import com.amazic.mylibrary.R;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NativeAfterInterManager {
    private static final String TAG = "Admob";
    public static final Map<String, NativeAd> mapNativeAdsAfterInter = new HashMap<>();
    public static final Map<String, List<NativeAd>> mapNativeAdsAfterInterSplash = new HashMap<>();

    public static void preloadNativeAfterInter(Activity activity, String adsKey, String remoteKey) {
        NativeAfterInterActivity.Companion.setAdsKey(adsKey);
        NativeAfterInterActivity.Companion.setRemoteKey(remoteKey);
        Log.d(TAG, "NativeAfterInterManager: preloadNativeAfterInter - list is Empty: " + AdmobApi.getInstance().getListIDByName(adsKey).isEmpty() + ", adskey = " + mapNativeAdsAfterInter.get(adsKey));
        if (mapNativeAdsAfterInter.get(adsKey) == null && !AdmobApi.getInstance().getListIDByName(adsKey).isEmpty()) {
            Log.d(TAG, "NativeAfterInterManager: 1.preloadNativeAfterInter." + AdmobApi.getInstance().getListIDByName(adsKey));
            Admob.getInstance().loadNativeAds(
                    activity,
                    AdmobApi.getInstance().getListIDByName(adsKey),
                    new NativeCallback() {
                        @Override
                        public void onNativeAdLoaded(NativeAd nativeAd) {
                            super.onNativeAdLoaded(nativeAd);
                            mapNativeAdsAfterInter.put(adsKey, nativeAd);
                            Log.d(TAG, "NativeAfterInterManager: onNativeAdLoaded: " + mapNativeAdsAfterInter);
                        }

                        @Override
                        public void onAdFailedToLoad(LoadAdError loadAdError) {
                            super.onAdFailedToLoad(loadAdError);
                            mapNativeAdsAfterInter.put(adsKey, null);
                            Log.d(TAG, "NativeAfterInterManager: 1.onAdFailedToLoad: " + loadAdError.getMessage());
                        }
                    }, remoteKey
            );
        }
    }

    public static void showPreloadNativeAfterInter(FrameLayout fr, Activity activity, String adsKey, String remoteKey, OnCloseNativeListener listener) {
        Log.d(TAG, "NativeAfterInterManager: showPreloadNativeAfterInter: adsKey = " + adsKey);
        int idLayoutNative = R.layout.native_after_inter;
        NativeAd nativeAd = mapNativeAdsAfterInter.get(adsKey);
        if (nativeAd != null) {
            Log.d(TAG, "NativeAfterInterManager: NativeAd Show");
            LayoutInflater layoutInflater = LayoutInflater.from(fr.getContext());

            NativeAdView adView = (NativeAdView) layoutInflater.inflate(idLayoutNative, fr, false);

            AppCompatButton btnClose = adView.findViewById(R.id.btn_close);
            btnClose.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onClose();
                }
            });
            fr.removeAllViews();
            fr.addView(adView);
            Admob.getInstance().populateNativeAdView(nativeAd, adView);
        } else {
            Log.d(TAG, "NativeAfterInterManager: NativeAd NULL onNext");
            if (listener != null) {
                listener.onFail();
            }
        }
        mapNativeAdsAfterInter.put(adsKey, null);
        preloadNativeAfterInter(activity, adsKey, remoteKey);

    }

    /// NATIVE AFTER INTER SPLASH
    public static void preloadNativeAfterInterSplash(Activity activity, String adsKey, String remoteKey) {
        NativeAfterInterActivity.Companion.setAdsKey(adsKey);
        NativeAfterInterActivity.Companion.setRemoteKey(remoteKey);

        List<String> listId = AdmobApi.getInstance().getListIDByName(adsKey);
        if (listId.isEmpty()) {
            Log.d(TAG, "NativeAfterInterManager Splash: no IDs for " + adsKey);
            return;
        }

        int targetCount = AsyncSplash.Companion.getInstance().getNumberNativeFullShowSplash();
        Log.d(TAG, "NativeAfterInterManager Splash: preload " + targetCount + " native ads");

        // Clear list cũ
        List<NativeAd> oldList = mapNativeAdsAfterInterSplash.get(adsKey);
        if (oldList != null) {
            for (NativeAd ad : oldList) ad.destroy();
            oldList.clear();
        }

        List<NativeAd> newList = new ArrayList<>();
        mapNativeAdsAfterInterSplash.put(adsKey, newList);

        loadNativeSequentiallySplash(activity, adsKey, remoteKey, targetCount, newList, 0);
    }

    private static void loadNativeSequentiallySplash(Activity activity, String adsKey, String remoteKey, int targetCount, List<NativeAd> list, int loadedCount) {
        if (loadedCount >= targetCount) {
            Log.d(TAG, "NativeAfterInterManager Splash: preload done, total = " + list.size());
            return;
        }
        Admob.getInstance().loadNativeAds(
                activity,
                AdmobApi.getInstance().getListIDByName(adsKey),
                new NativeCallback() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        super.onNativeAdLoaded(nativeAd);
                        list.add(nativeAd);
                        Log.d(TAG, "NativeAfterInterManager Splash: loaded " + list.size() + "/" + targetCount);
                        loadNativeSequentiallySplash(activity, adsKey, remoteKey, targetCount, list, loadedCount + 1);
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        Log.d(TAG, "NativeAfterInterManager Splash: failed at " + loadedCount + ": " + loadAdError.getMessage());
                        loadNativeSequentiallySplash(activity, adsKey, remoteKey, targetCount, list, loadedCount + 1);
                    }
                }, remoteKey
        );
    }

    public static boolean hasNativeAfterInterSplash(String adsKey) {
        List<NativeAd> list = mapNativeAdsAfterInterSplash.get(adsKey);
        return list != null && !list.isEmpty();
    }

    public static void showNativeAdInFrameSplash(FrameLayout fr, NativeAd nativeAd) {
        if (nativeAd == null) return;
        NativeAdView adView = (NativeAdView) LayoutInflater.from(fr.getContext())
                .inflate(R.layout.native_after_inter, fr, false);
        // Ẩn btn_close trong layout native, dùng overlay button của activity thay thế
        if (adView.findViewById(R.id.btn_close) != null) {
            adView.findViewById(R.id.btn_close).setVisibility(android.view.View.GONE);
        }
        fr.removeAllViews();
        fr.addView(adView);
        Admob.getInstance().populateNativeAdView(nativeAd, adView);
    }

    /// NATIVE FULL SPLASH (thay thế hoàn toàn inter_splash)
    public static void loadNativeFullSplash(Activity activity, String adsKey, String remoteKey, int targetCount, Runnable onFirstLoaded, Runnable onAllFailed) {
        NativeAfterInterActivity.Companion.setAdsKey(adsKey);
        NativeAfterInterActivity.Companion.setRemoteKey(remoteKey);

        // Clear old list
        List<NativeAd> oldList = mapNativeAdsAfterInterSplash.get(adsKey);
        if (oldList != null) {
            for (NativeAd ad : oldList) ad.destroy();
            oldList.clear();
        }
        List<NativeAd> newList = new ArrayList<>();
        mapNativeAdsAfterInterSplash.put(adsKey, newList);

        Log.d(TAG, "NativeFullSplash: start load " + targetCount + " native ads for key=" + adsKey);
        loadNativeFullSplashSequentially(activity, adsKey, remoteKey, targetCount, newList, 0,
                new boolean[]{false}, new boolean[]{false}, onFirstLoaded, onAllFailed);
    }

    private static void loadNativeFullSplashSequentially(Activity activity, String adsKey, String remoteKey, int targetCount, List<NativeAd> list, int loadedCount, boolean[] hasNotifiedFirst, boolean[] hasNotifiedFail, Runnable onFirstLoaded, Runnable onAllFailed) {
        if (loadedCount >= targetCount) {
            Log.d(TAG, "NativeFullSplash: loading done, total loaded = " + list.size());
            if (list.isEmpty() && !hasNotifiedFail[0]) {
                hasNotifiedFail[0] = true;
                new Handler(Looper.getMainLooper()).post(onAllFailed);
            }
            return;
        }
        Admob.getInstance().loadNativeAds(activity, AdmobApi.getInstance().getListIDByName(adsKey),
                new NativeCallback() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        super.onNativeAdLoaded(nativeAd);
                        list.add(nativeAd);
                        Log.d(TAG, "NativeFullSplash: loaded " + list.size() + "/" + targetCount);
                        if (!hasNotifiedFirst[0]) {
                            hasNotifiedFirst[0] = true;
                            List<NativeAd> mapList = mapNativeAdsAfterInterSplash.get(adsKey);
                            Log.d(TAG, "NativeFullSplash: [before navigate] adsKey='" + adsKey
                                    + "', list.size=" + list.size()
                                    + ", mapHasKey=" + mapNativeAdsAfterInterSplash.containsKey(adsKey)
                                    + ", mapListSize=" + (mapList != null ? mapList.size() : "null")
                                    + ", isSameRef=" + (mapList == list));
                            new Handler(Looper.getMainLooper()).post(onFirstLoaded);
                        }
                        loadNativeFullSplashSequentially(activity, adsKey, remoteKey, targetCount, list, loadedCount + 1, hasNotifiedFirst, hasNotifiedFail, onFirstLoaded, onAllFailed);
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        Log.d(TAG, "NativeFullSplash: fail at slot " + loadedCount + ": " + loadAdError.getMessage());
                        loadNativeFullSplashSequentially(activity, adsKey, remoteKey, targetCount, list, loadedCount + 1, hasNotifiedFirst, hasNotifiedFail, onFirstLoaded, onAllFailed);
                    }
                }, remoteKey);
    }

    public interface OnCloseNativeListener {
        void onClose();

        void onFail();
    }
}

