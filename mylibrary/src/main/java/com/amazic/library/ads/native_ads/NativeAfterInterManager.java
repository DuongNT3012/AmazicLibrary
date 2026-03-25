package com.amazic.library.ads.native_ads;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.appcompat.widget.AppCompatButton;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.callback.NativeCallback;
import com.amazic.library.view.NativeAfterInterActivity;
import com.amazic.mylibrary.R;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

import java.util.HashMap;
import java.util.Map;

public class NativeAfterInterManager {
    private static final String TAG = "Admob";
    public static final Map<String, NativeAd> mapNativeAdsAfterInter = new HashMap<>();

    public static void preloadNativeAfterInter(Activity activity, String adsKey, String remoteKey) {
        NativeAfterInterActivity.Companion.setAdsKey(adsKey);
        NativeAfterInterActivity.Companion.setRemoteKey(remoteKey);
        Log.d(TAG, "NativeAfterInterManager: preloadNativeAfterInter - list is Empty: "+AdmobApi.getInstance().getListIDByName(adsKey).isEmpty() + ", adskey = "+mapNativeAdsAfterInter.get(adsKey));
        if (mapNativeAdsAfterInter.get(adsKey) == null || !AdmobApi.getInstance().getListIDByName(adsKey).isEmpty()) {
            Log.d(TAG, "NativeAfterInterManager: 1.preloadNativeAfterInter."+ AdmobApi.getInstance().getListIDByName(adsKey));
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
                if(listener != null){
                    listener.onClose();
                }
            });
            fr.removeAllViews();
            fr.addView(adView);
            Admob.getInstance().populateNativeAdView(nativeAd, adView);
        }else {
            Log.d(TAG, "NativeAfterInterManager: NativeAd NULL onNext");
            if(listener != null){
                listener.onFail();
            }
        }
        mapNativeAdsAfterInter.put(adsKey, null);
        preloadNativeAfterInter(activity, adsKey, remoteKey);

    }

    public interface OnCloseNativeListener {
        void onClose();
        void onFail();
    }
}

