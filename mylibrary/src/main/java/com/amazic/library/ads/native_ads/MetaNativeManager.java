package com.amazic.library.ads.native_ads;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.amazic.library.ads.splash_ads.AsyncSplash;
import com.amazic.mylibrary.R;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetaNativeManager {
    private static final String TAG = "Admob";

    public static final Map<String, NativeAd> mapMetaNativeAfterInter = new HashMap<>();
    public static final Map<String, List<NativeAd>> mapMetaNativeAfterInterSplash = new HashMap<>();

//    // ─── PRELOAD SINGLE ──────────────────────────────────────────────
//
//    public static void preloadMetaNativeAfterInter(Activity activity, String placementId, String adsKey) {
//        Log.d(TAG, "MetaNativeManager: preload adsKey=" + adsKey);
//        if (mapMetaNativeAfterInter.get(adsKey) != null) {
//            Log.d(TAG, "MetaNativeManager: already have ad for " + adsKey);
//            return;
//        }
//        if (placementId == null || placementId.isEmpty()) {
//            Log.d(TAG, "MetaNativeManager: placementId empty");
//            return;
//        }
//        NativeAd nativeAd = new NativeAd(activity, placementId);
//        nativeAd.loadAd(
//                nativeAd.buildLoadAdConfig()
//                        .withAdListener(new NativeAdListener() {
//                            @Override
//                            public void onError(Ad ad, AdError adError) {
//                                Log.d(TAG, "MetaNativeManager: preload failed adsKey=" + adsKey + " err=" + adError.getErrorMessage());
//                                mapMetaNativeAfterInter.put(adsKey, null);
//                            }
//
//                            @Override
//                            public void onAdLoaded(Ad ad) {
//                                Log.d(TAG, "MetaNativeManager: preload loaded adsKey=" + adsKey);
//                                mapMetaNativeAfterInter.put(adsKey, (NativeAd) ad);
//                            }
//
//                            @Override public void onAdClicked(Ad ad) {}
//                            @Override public void onLoggingImpression(Ad ad) {}
//                            @Override public void onMediaDownloaded(Ad ad) {}
//                        }).build()
//        );
//    }
//
//    // ─── SHOW SINGLE ─────────────────────────────────────────────────
//
//    public static void showPreloadMetaNativeAfterInter(FrameLayout fr, Activity activity,
//                                                       String placementId, String adsKey,
//                                                       OnCloseNativeListener listener) {
//        Log.d(TAG, "MetaNativeManager: show adsKey=" + adsKey);
//        NativeAd nativeAd = mapMetaNativeAfterInter.get(adsKey);
//
//        if (nativeAd != null && nativeAd.isAdLoaded() && !nativeAd.isAdInvalidated()) {
//            showMetaNativeAdInFrame(fr, nativeAd, listener);
//        } else {
//            Log.d(TAG, "MetaNativeManager: no valid ad → onFail");
//            if (listener != null) listener.onFail();
//        }
//
//        mapMetaNativeAfterInter.put(adsKey, null);
//        preloadMetaNativeAfterInter(activity, placementId, adsKey);
//    }
//
//    private static void showMetaNativeAdInFrame(FrameLayout fr, NativeAd nativeAd,
//                                                OnCloseNativeListener listener) {
//        View adView = LayoutInflater.from(fr.getContext())
//                .inflate(R.layout.native_meta_splash, fr, false);
//        bindMetaNativeAdView(adView, nativeAd);
//
////        View btnClose = adView.findViewById(R.id.btn_close);
////        if (btnClose != null) {
////            btnClose.setOnClickListener(v -> {
////                if (listener != null) listener.onClose();
////            });
////        }
//        fr.removeAllViews();
//        fr.addView(adView);
//    }

    // ─── PRELOAD SPLASH LIST dung cho sau Inter Splash ─────────────────────────────────────────

//    public static void preloadMetaNativeAfterInterSplash(Activity activity, String placementId,
//                                                         String adsKey) {
//        int targetCount = AsyncSplash.Companion.getInstance().getNumberNativeAfterInterSplash();
//
//        Log.d(TAG, "MetaNativeManager Splash: preload " + targetCount + " ads adsKey=" + adsKey);
//        List<NativeAd> oldList = mapMetaNativeAfterInterSplash.get(adsKey);
//        if (oldList != null) {
//            for (NativeAd ad : oldList) destroyMetaNativeAd(ad);
//            oldList.clear();
//        }
//        List<NativeAd> newList = new ArrayList<>();
//        mapMetaNativeAfterInterSplash.put(adsKey, newList);
//        loadMetaNativeSequentiallySplash(activity, placementId, adsKey, targetCount, newList, 0);
//    }
//
//    private static void loadMetaNativeSequentiallySplash(Activity activity, String placementId,
//                                                         String adsKey, int targetCount,
//                                                         List<NativeAd> list, int loadedCount) {
//        if (loadedCount >= targetCount) {
//            Log.d(TAG, "MetaNativeManager Splash: done total=" + list.size());
//            return;
//        }
//        NativeAd nativeAd = new NativeAd(activity, placementId);
//        nativeAd.loadAd(
//                nativeAd.buildLoadAdConfig()
//                        .withAdListener(new NativeAdListener() {
//                            @Override
//                            public void onError(Ad ad, AdError adError) {
//                                Log.d(TAG, "MetaNativeManager Splash: fail slot=" + loadedCount + " err=" + adError.getErrorMessage());
//                                loadMetaNativeSequentiallySplash(activity, placementId, adsKey,
//                                        targetCount, list, loadedCount + 1);
//                            }
//
//                            @Override
//                            public void onAdLoaded(Ad ad) {
//                                list.add((NativeAd) ad);
//                                Log.d(TAG, "MetaNativeManager Splash: loaded " + list.size() + "/" + targetCount);
//                                loadMetaNativeSequentiallySplash(activity, placementId, adsKey,
//                                        targetCount, list, loadedCount + 1);
//                            }
//
//                            @Override public void onAdClicked(Ad ad) {}
//                            @Override public void onLoggingImpression(Ad ad) {}
//                            @Override public void onMediaDownloaded(Ad ad) {}
//                        }).build()
//        );
//    }

    // ─── NATIVE FULL SPLASH (thay inter_splash) ──────────────────────

    public static void loadMetaNativeFullSplash(Activity activity, String placementId,
                                                String adsKey, int targetCount,
                                                Runnable onFirstLoaded, Runnable onAllFailed) {
        List<NativeAd> oldList = mapMetaNativeAfterInterSplash.get(adsKey);
        if (oldList != null) {
            for (NativeAd ad : oldList) destroyMetaNativeAd(ad);
            oldList.clear();
        }
        List<NativeAd> newList = new ArrayList<>();
        mapMetaNativeAfterInterSplash.put(adsKey, newList);

        Log.d(TAG, "MetaNativeFullSplash: start load " + targetCount + " ads key=" + adsKey);
        loadMetaNativeFullSplashSequentially(activity, placementId, adsKey, targetCount, newList, 0,
                new boolean[]{false}, new boolean[]{false}, onFirstLoaded, onAllFailed);
    }

    private static void loadMetaNativeFullSplashSequentially(Activity activity, String placementId,
                                                             String adsKey, int targetCount,
                                                             List<NativeAd> list, int loadedCount,
                                                             boolean[] hasNotifiedFirst,
                                                             boolean[] hasNotifiedFail,
                                                             Runnable onFirstLoaded,
                                                             Runnable onAllFailed) {
        if (loadedCount >= targetCount) {
            Log.d(TAG, "MetaNativeFullSplash: done total=" + list.size());
            if (list.isEmpty() && !hasNotifiedFail[0]) {
                hasNotifiedFail[0] = true;
                new Handler(Looper.getMainLooper()).post(onAllFailed);
            }
            return;
        }
        NativeAd nativeAd = new NativeAd(activity, placementId);
        nativeAd.loadAd(
                nativeAd.buildLoadAdConfig()
                        .withAdListener(new NativeAdListener() {
                            @Override
                            public void onError(Ad ad, AdError adError) {
                                Log.d(TAG, "MetaNativeFullSplash: fail slot=" + loadedCount + " err=" + adError.getErrorMessage());
                                loadMetaNativeFullSplashSequentially(activity, placementId, adsKey,
                                        targetCount, list, loadedCount + 1,
                                        hasNotifiedFirst, hasNotifiedFail, onFirstLoaded, onAllFailed);
                            }

                            @Override
                            public void onAdLoaded(Ad ad) {
                                list.add((NativeAd) ad);
                                Log.d(TAG, "MetaNativeFullSplash: loaded " + list.size() + "/" + targetCount);
                                if (!hasNotifiedFirst[0]) {
                                    hasNotifiedFirst[0] = true;
                                    new Handler(Looper.getMainLooper()).post(onFirstLoaded);
                                }
                                loadMetaNativeFullSplashSequentially(activity, placementId, adsKey,
                                        targetCount, list, loadedCount + 1,
                                        hasNotifiedFirst, hasNotifiedFail, onFirstLoaded, onAllFailed);
                            }

                            @Override public void onAdClicked(Ad ad) {}
                            @Override public void onLoggingImpression(Ad ad) {}
                            @Override public void onMediaDownloaded(Ad ad) {}
                        }).build()
        );
    }

    // ─── SHOW IN FRAME (splash) ──────────────────────────────────────

    public static void showMetaNativeAdInFrameSplash(FrameLayout fr, NativeAd nativeAd) {
        if (nativeAd == null || !nativeAd.isAdLoaded() || nativeAd.isAdInvalidated()) return;
        View adView = LayoutInflater.from(fr.getContext())
                .inflate(R.layout.native_meta_splash, fr, false);
//        View btnClose = adView.findViewById(R.id.btn_close);
//        if (btnClose != null) btnClose.setVisibility(View.GONE);
        bindMetaNativeAdView(adView, nativeAd);
        fr.removeAllViews();
        fr.addView(adView);
    }

    // ─── BIND DATA → VIEW ────────────────────────────────────────────

    private static void bindMetaNativeAdView(View adView, NativeAd nativeAd) {
        MediaView mediaView   = adView.findViewById(R.id.native_ad_media);
        MediaView adIconView = adView.findViewById(R.id.native_ad_icon);
        TextView tvTitle      = adView.findViewById(R.id.native_ad_title);
        TextView tvBody       = adView.findViewById(R.id.native_ad_body);
        Button btnCta         = adView.findViewById(R.id.native_ad_call_to_action);
        TextView tvSponsor    = adView.findViewById(R.id.native_ad_sponsored_label);

        if (tvTitle != null)   tvTitle.setText(nativeAd.getAdvertiserName());
        if (tvBody != null)    tvBody.setText(nativeAd.getAdBodyText());
        if (btnCta != null)    btnCta.setText(nativeAd.getAdCallToAction());
        if (tvSponsor != null) tvSponsor.setText("Sponsored");

        // Bắt buộc phải registerViewForInteraction — Meta mới track impression và click
        List<View> clickableViews = new ArrayList<>();
        if (btnCta != null)  clickableViews.add(btnCta);
        if (tvTitle != null) clickableViews.add(tvTitle);

        nativeAd.registerViewForInteraction(adView, mediaView, adIconView, clickableViews);
    }

    // ─── HELPER ──────────────────────────────────────────────────────

    public static boolean hasMetaNativeAfterInterSplash(String adsKey) {
        List<NativeAd> list = mapMetaNativeAfterInterSplash.get(adsKey);
        return list != null && !list.isEmpty();
    }

    // Destroy đúng cách cho Meta — unregisterView trước, rồi destroy
    public static void destroyMetaNativeAd(NativeAd ad) {
        if (ad == null) return;
        try {
            ad.unregisterView();
            ad.destroy();
        } catch (Exception e) {
            Log.e(TAG, "MetaNativeManager: destroy error=" + e.getMessage());
        }
    }

    public static void destroyAll(String adsKey) {
//        destroyMetaNativeAd(mapMetaNativeAfterInter.get(adsKey));
//        mapMetaNativeAfterInter.remove(adsKey);

        List<NativeAd> splashList = mapMetaNativeAfterInterSplash.get(adsKey);
        if (splashList != null) {
            for (NativeAd ad : splashList) destroyMetaNativeAd(ad);
            splashList.clear();
        }
        mapMetaNativeAfterInterSplash.remove(adsKey);
    }

    // ─── INTERFACE ───────────────────────────────────────────────────

    public interface OnCloseNativeListener {
        void onClose();
        void onFail();
    }
}
