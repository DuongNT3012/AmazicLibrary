package com.amazic.library.ads.native_ads;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.callback.NativeCallback;
import com.amazic.library.ads.splash_ads.AdmobAdsConfig;
import com.amazic.mylibrary.R;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdOptionsView;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdListener;
import com.google.android.gms.ads.LoadAdError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetaNativeManager {
    public static final Map<String, NativeAd> mapMetaNativeAfterInter = new HashMap<>();
    public static final Map<String, List<NativeAd>> mapMetaNativeSplash = new HashMap<>();
    // Thêm map lưu AdMob fallback song song với Meta (TH Meta load fail)
    public static final Map<String, List<com.google.android.gms.ads.nativead.NativeAd>>
            mapAdmobNativeSplash = new HashMap<>();
    private static final String TAG = "Admob";

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

    /// /        View btnClose = adView.findViewById(R.id.btn_close);
    /// /        if (btnClose != null) {
    /// /            btnClose.setOnClickListener(v -> {
    /// /                if (listener != null) listener.onClose();
    /// /            });
    /// /        }
//        fr.removeAllViews();
//        fr.addView(adView);
//    }

    // ─── PRELOAD SPLASH LIST dung cho sau Inter Splash ─────────────────────────────────────────

//    public static void preloadMetaNativeAfterInterSplash(Activity activity, String placementId,
//                                                         String adsKey) {
//        int targetCount = AdmobAdsConfig.getInstance().getNumberNativeFullShowSplash();
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
    public static void loadMetaNativeFullSplash(Activity activity, List<String> listIdNative,
                                                String adsKey, int targetCount,
                                                Runnable onFirstLoaded, Runnable onAllFailed) {
        // Clean Meta list
        List<NativeAd> oldList = mapMetaNativeSplash.get(adsKey);
        if (oldList != null) {
            for (NativeAd ad : oldList) destroyMetaNativeAd(ad);
            oldList.clear();
        }
        List<NativeAd> newList = new ArrayList<>();
        mapMetaNativeSplash.put(adsKey, newList);

        // Clean & init AdMob fallback list
        String admobKey = AdmobAdsConfig.getInstance().getKeyNativeFullAdmobSplash();
        List<com.google.android.gms.ads.nativead.NativeAd> fallbackList = new ArrayList<>();
        if (AdmobAdsConfig.getInstance().isUseNativeFullSplashAdmobWhenMetaFail()) {
            List<com.google.android.gms.ads.nativead.NativeAd> oldFallback = mapAdmobNativeSplash.get(admobKey);
            if (oldFallback != null) {
                for (com.google.android.gms.ads.nativead.NativeAd ad : oldFallback) ad.destroy();
                oldFallback.clear();
            }
            mapAdmobNativeSplash.put(admobKey, fallbackList);
        }

        String idMeta = AdmobAdsConfig.getInstance().getIdNativeMetaSplash();
        if (idMeta == null || idMeta.isEmpty()) {
            idMeta = listIdNative.get(0);
        }

        Log.d(TAG, "MetaNativeFullSplash: start load " + targetCount + " ads key=" + adsKey + " idMeta=" + idMeta);

        loadMetaNativeFullSplashSequentially(
                activity, idMeta, adsKey, targetCount,
                newList, fallbackList,
                0,
                new boolean[]{false},  // hasNotifiedFirst
                new boolean[]{false},  // hasNotifiedFail
                new int[]{0},          // pendingAdmobCount
                onFirstLoaded, onAllFailed
        );
    }

    private static void loadMetaNativeFullSplashSequentially(Activity activity, String placementId,
                                                             String adsKey, int targetCount,
                                                             List<NativeAd> list,
                                                             List<com.google.android.gms.ads.nativead.NativeAd> fallbackList,
                                                             int loadedCount,
                                                             boolean[] hasNotifiedFirst,
                                                             boolean[] hasNotifiedFail,
                                                             int[] pendingAdmobCount,
                                                             Runnable onFirstLoaded,
                                                             Runnable onAllFailed) {
        if (loadedCount >= targetCount) {
            Log.d(TAG, "MetaNativeFullSplash: Meta done total=" + list.size() + " pendingAdmob=" + pendingAdmobCount[0]);
            // Chờ AdMob pending xong mới quyết định onAllFailed

            if (AdmobAdsConfig.getInstance().isUseNativeFullSplashAdmobWhenMetaFail()) {
                if (list.isEmpty() && pendingAdmobCount[0] == 0
                        && !hasNotifiedFirst[0] && !hasNotifiedFail[0]) {
                    hasNotifiedFail[0] = true;
                    new Handler(Looper.getMainLooper()).post(onAllFailed);
                }
            } else {
                if (list.isEmpty() && !hasNotifiedFail[0]) {
                    hasNotifiedFail[0] = true;
                    new Handler(Looper.getMainLooper()).post(onAllFailed);
                }
            }

            return;
        }

        NativeAd nativeAd = new NativeAd(activity, placementId);
        nativeAd.loadAd(
                nativeAd.buildLoadAdConfig()
                        .withAdListener(new NativeAdListener() {
                            @Override
                            public void onError(Ad ad, AdError adError) {
                                Log.d(TAG, "MetaNativeFullSplash: fail slot=" + loadedCount
                                        + " idMeta=" + placementId + " err=" + adError.getErrorMessage());

                                if (AdmobAdsConfig.getInstance().isUseNativeFullSplashAdmobWhenMetaFail()) {
                                    String admobKey = AdmobAdsConfig.getInstance().getKeyNativeFullAdmobSplash();
                                    List<String> admobIds = AdmobApi.getInstance().getListIDByName(admobKey);

                                    if (admobIds != null && !admobIds.isEmpty()) {
                                        pendingAdmobCount[0]++;  // tăng TRƯỚC khi bắt đầu load
                                        Admob.getInstance().loadNativeAds(
                                                activity,
                                                admobIds,
                                                new NativeCallback() {
                                                    @Override
                                                    public void onNativeAdLoaded(com.google.android.gms.ads.nativead.NativeAd admobAd) {
                                                        super.onNativeAdLoaded(admobAd);
                                                        fallbackList.add(admobAd);
                                                        pendingAdmobCount[0]--;
                                                        Log.d(TAG, "MetaNativeFullSplash: admob fallback loaded total=" + fallbackList.size()
                                                                + " pendingAdmob=" + pendingAdmobCount[0]);

                                                        // Fire onFirstLoaded nếu Meta all fail và chưa ai fire
                                                        if (!hasNotifiedFirst[0]) {
                                                            hasNotifiedFirst[0] = true;
                                                            new Handler(Looper.getMainLooper()).post(onFirstLoaded);
                                                        }
                                                        // Nếu Meta slot khác đã fire onFirstLoaded trước
                                                        // → hasNotifiedFirst=true → không fire lại, chỉ add vào fallbackList
                                                    }

                                                    @Override
                                                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                                                        super.onAdFailedToLoad(loadAdError);
                                                        pendingAdmobCount[0]--;
                                                        Log.d(TAG, "MetaNativeFullSplash: admob fallback failed"
                                                                + " pendingAdmob=" + pendingAdmobCount[0]);

                                                        // Tất cả Meta + AdMob đều fail và không còn pending
                                                        if (list.isEmpty() && fallbackList.isEmpty()
                                                                && pendingAdmobCount[0] == 0
                                                                && !hasNotifiedFirst[0] && !hasNotifiedFail[0]) {
                                                            hasNotifiedFail[0] = true;
                                                            new Handler(Looper.getMainLooper()).post(onAllFailed);
                                                        }
                                                    }
                                                },
                                                admobKey
                                        );
                                    }
                                }

                                loadMetaNativeFullSplashSequentially(activity, placementId, adsKey,
                                        targetCount, list, fallbackList, loadedCount + 1,
                                        hasNotifiedFirst, hasNotifiedFail, pendingAdmobCount,
                                        onFirstLoaded, onAllFailed);
                            }

                            @Override
                            public void onAdLoaded(Ad ad) {
                                list.add((NativeAd) ad);
                                Log.d(TAG, "MetaNativeFullSplash: loaded " + list.size() + "/" + targetCount
                                        + " idMeta=" + placementId);
                                if (!hasNotifiedFirst[0]) {
                                    hasNotifiedFirst[0] = true;
                                    new Handler(Looper.getMainLooper()).post(onFirstLoaded);
                                }
                                loadMetaNativeFullSplashSequentially(activity, placementId, adsKey,
                                        targetCount, list, fallbackList, loadedCount + 1,
                                        hasNotifiedFirst, hasNotifiedFail, pendingAdmobCount,
                                        onFirstLoaded, onAllFailed);
                            }

                            @Override
                            public void onAdClicked(Ad ad) {
                            }

                            @Override
                            public void onLoggingImpression(Ad ad) {
                            }

                            @Override
                            public void onMediaDownloaded(Ad ad) {
                            }
                        }).build()
        );
    }

//    public static void loadMetaNativeFullSplash(Activity activity, List<String> listIdNative,
//                                                String adsKey, int targetCount,
//                                                Runnable onFirstLoaded, Runnable onAllFailed) {
//        //Clean Meta list
//        List<NativeAd> oldList = mapMetaNativeSplash.get(adsKey);
//        if (oldList != null) {
//            for (NativeAd ad : oldList) destroyMetaNativeAd(ad);
//            oldList.clear();
//        }
//        List<NativeAd> newList = new ArrayList<>();
//        mapMetaNativeSplash.put(adsKey, newList);
//
//        //create Admob list
//        String admobKey = AdmobAdsConfig.getInstance().getKeyNativeFullAdmobSplash();
//        List<com.google.android.gms.ads.nativead.NativeAd> fallbackList = new ArrayList<>();
//        if (AdmobAdsConfig.getInstance().getUseNativeFullSplashAdmobWhenMetaFail()) {
//            List<com.google.android.gms.ads.nativead.NativeAd> oldFallback = mapAdmobNativeSplash.get(admobKey);
//            if (oldFallback != null) {
//                for (com.google.android.gms.ads.nativead.NativeAd ad : oldFallback) ad.destroy();
//                oldFallback.clear();
//            }
//            mapAdmobNativeSplash.put(admobKey, fallbackList);
//        }
//
//        String idMeta = "";
//        if (!AdmobAdsConfig.getInstance().getIdNativeMetaSplash().equals("")) {
//            idMeta = AdmobAdsConfig.getInstance().getIdNativeMetaSplash();
//        } else {
//            idMeta = listIdNative.get(0);
//        }
//        Log.d(TAG, "MetaNativeFullSplash: start load " + targetCount + " ads key=" + adsKey + " idMeta = " + idMeta);
//        loadMetaNativeFullSplashSequentially(activity, idMeta, adsKey, targetCount, newList, fallbackList, 0,
//                new boolean[]{false}, new boolean[]{false}, new int[]{0}, onFirstLoaded, onAllFailed);
//    }
//
//    private static void loadMetaNativeFullSplashSequentially(Activity activity, String placementId,
//                                                             String adsKey, int targetCount,
//                                                             List<NativeAd> list,
//                                                             List<com.google.android.gms.ads.nativead.NativeAd> fallbackList,
//                                                             int loadedCount,
//                                                             boolean[] hasNotifiedFirst,
//                                                             boolean[] hasNotifiedFail,
//                                                             int[] pendingAdmobCount,
//                                                             Runnable onFirstLoaded,
//                                                             Runnable onAllFailed) {
//        if (loadedCount >= targetCount) {
//            Log.d(TAG, "MetaNativeFullSplash: done total=" + list.size());
//            if (list.isEmpty() && !hasNotifiedFail[0]) {
//                hasNotifiedFail[0] = true;
//                new Handler(Looper.getMainLooper()).post(onAllFailed);
//            }
//            return;
//        }
//        NativeAd nativeAd = new NativeAd(activity, placementId);
//        nativeAd.loadAd(
//                nativeAd.buildLoadAdConfig()
//                        .withAdListener(new NativeAdListener() {
//                            @Override
//                            public void onError(Ad ad, AdError adError) {
//                                Log.d(TAG, "MetaNativeFullSplash: fail slot=" + loadedCount + ", idMeta = " + placementId + " err=" + adError.getErrorMessage());
//
//                                // Load AdMob native song song, không block Meta tiếp tục
//                                Admob.getInstance().loadNativeAds(
//                                        activity,
//                                        AdmobApi.getInstance().getListIDByName(AdmobAdsConfig.getInstance().getKeyNativeFullAdmobSplash()),
//                                        new NativeCallback() {
//                                            @Override
//                                            public void onNativeAdLoaded(com.google.android.gms.ads.nativead.NativeAd nativeAd) {
//                                                super.onNativeAdLoaded(nativeAd);
//                                                fallbackList.add(nativeAd);
//                                                Log.d(TAG, "MetaNativeFullSplash: admob fallback loaded, total=" + fallbackList.size());
//                                            }
//
//                                            @Override
//                                            public void onAdFailedToLoad(LoadAdError loadAdError) {
//                                                super.onAdFailedToLoad(loadAdError);
//                                                Log.d(TAG, "MetaNativeFullSplash: admob fallback also failed");
//                                            }
//                                        },
//                                        AdmobAdsConfig.getInstance().getKeyNativeFullAdmobSplash()
//                                );
//
//                                loadMetaNativeFullSplashSequentially(activity, placementId, adsKey,
//                                        targetCount, list, fallbackList, loadedCount + 1,
//                                        hasNotifiedFirst, hasNotifiedFail,pendingAdmobCount, onFirstLoaded, onAllFailed);
//                            }
//
//                            @Override
//                            public void onAdLoaded(Ad ad) {
//                                list.add((NativeAd) ad);
//                                Log.d(TAG, "MetaNativeFullSplash: loaded " + list.size() + "/" + targetCount + ", idMeta = " + placementId);
//                                if (!hasNotifiedFirst[0]) {
//                                    hasNotifiedFirst[0] = true;
//                                    new Handler(Looper.getMainLooper()).post(onFirstLoaded);
//                                }
//                                loadMetaNativeFullSplashSequentially(activity, placementId, adsKey,
//                                        targetCount, list, fallbackList, loadedCount + 1,
//                                        hasNotifiedFirst, hasNotifiedFail,pendingAdmobCount, onFirstLoaded, onAllFailed);
//                            }
//
//                            @Override
//                            public void onAdClicked(Ad ad) {
//                            }
//
//                            @Override
//                            public void onLoggingImpression(Ad ad) {
//                            }
//
//                            @Override
//                            public void onMediaDownloaded(Ad ad) {
//                            }
//                        }).build()
//        );
//    }

    // ─── SHOW IN FRAME (splash) ──────────────────────────────────────

    public static void showMetaNativeAdInFrameSplash(FrameLayout fr, NativeAd nativeAd) {
        if (nativeAd == null || !nativeAd.isAdLoaded() || nativeAd.isAdInvalidated()) return;
        View adView = LayoutInflater.from(fr.getContext())
                .inflate(R.layout.native_meta_splash, fr, false);
//        View btnClose = adView.findViewById(R.id.btn_close);
//        if (btnClose != null) btnClose.setVisibility(View.GONE);
        fr.removeAllViews();
        fr.addView(adView);

        bindMetaNativeAdView(adView, nativeAd);
    }

    // ─── BIND DATA → VIEW ────────────────────────────────────────────

    private static void bindMetaNativeAdView(View adView, NativeAd nativeAd) {
        NativeAdLayout nativeAdLayout = adView.findViewById(R.id.native_ad_container);
        LinearLayout adChoicesContainer = adView.findViewById(R.id.ad_choices_container);
        AdOptionsView adOptionsView = new AdOptionsView(adView.getContext(), nativeAd, nativeAdLayout);
        adChoicesContainer.removeAllViews();
        adChoicesContainer.addView(adOptionsView, 0);

        MediaView mediaView = adView.findViewById(R.id.native_ad_media);
        MediaView adIconView = adView.findViewById(R.id.native_ad_icon);
        TextView tvTitle = adView.findViewById(R.id.native_ad_title);
        TextView tvBody = adView.findViewById(R.id.native_ad_body);
        Button btnCta = adView.findViewById(R.id.native_ad_call_to_action);
        TextView tvSponsor = adView.findViewById(R.id.native_ad_sponsored_label);

        if (tvTitle != null) tvTitle.setText(nativeAd.getAdvertiserName());
        if (tvBody != null) tvBody.setText(nativeAd.getAdBodyText());
        if (btnCta != null) btnCta.setText(nativeAd.getAdCallToAction());
        if (tvSponsor != null) tvSponsor.setText("Sponsored");

        // Bắt buộc phải registerViewForInteraction — Meta mới track impression và click
        List<View> clickableViews = new ArrayList<>();
        if (btnCta != null) clickableViews.add(btnCta);
        if (tvTitle != null) clickableViews.add(tvTitle);

        nativeAd.registerViewForInteraction(adView, mediaView, adIconView, clickableViews);
    }

    // ─── HELPER ──────────────────────────────────────────────────────

    public static boolean hasMetaNativeAfterInterSplash(String adsKey) {
        List<NativeAd> list = mapMetaNativeSplash.get(adsKey);
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

        List<NativeAd> splashList = mapMetaNativeSplash.get(adsKey);
        if (splashList != null) {
            for (NativeAd ad : splashList) destroyMetaNativeAd(ad);
            splashList.clear();
        }
        mapMetaNativeSplash.remove(adsKey);
    }

    public static boolean hasAdmobFallback(String adsKey) {
        List<com.google.android.gms.ads.nativead.NativeAd> list = mapAdmobNativeSplash.get(adsKey);
        return list != null && !list.isEmpty();
    }

    public static com.google.android.gms.ads.nativead.NativeAd pollAdmobFallback(String adsKey) {
        List<com.google.android.gms.ads.nativead.NativeAd> list = mapAdmobNativeSplash.get(adsKey);
        if (list != null && !list.isEmpty()) return list.remove(0);
        return null;
    }

    // ─── INTERFACE ───────────────────────────────────────────────────

    public interface OnCloseNativeListener {
        void onClose();

        void onFail();
    }
}
