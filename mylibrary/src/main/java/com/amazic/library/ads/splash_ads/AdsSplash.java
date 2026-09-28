package com.amazic.library.ads.splash_ads;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazic.library.Utils.AdjustUtil;
import com.amazic.library.Utils.EventTrackingHelper;
import com.amazic.library.Utils.NetworkUtil;
import com.amazic.library.Utils.RemoteConfigHelper;
import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.NextGenAds;
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.ads.callback.InterCallback;
import com.amazic.library.ump.AdsConsentManager;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdValue;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback;

public class AdsSplash {

    private static final String TAG = "adssplash";

    private static volatile AdsSplash instance;

    private InterstitialAd mInterstitialAd;
    private final AtomicBoolean isLoading = new AtomicBoolean(false);

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private interface ILoadingAdSplashListener {
        void onLoading();
    }

    public static AdsSplash getInstance() {
        if (instance == null) {
            synchronized (AdsSplash.class) {
                if (instance == null) {
                    instance = new AdsSplash();
                }
            }
        }
        return instance;
    }

    private void showInterSplash(@NonNull Activity activity,
                                 @NonNull String adUnitId, @NonNull String remoteKey,
                                 @NonNull InterCallback interCallback) {
        if (mInterstitialAd == null && !NextGenAds.isInterstitialPreloaded(adUnitId)) {
            AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_SHOW_FAILED_SPLASH_AD_NULL);
            interCallback.onAdFailedToLoad();
            interCallback.onNextAction();
            return;
        }
        if (checkNotAllowCondition(activity, remoteKey, EventNameSplash.EVENT_SHOW_FAILED_SPLASH_CONDITION)) {
            interCallback.onAdFailedToShowFullScreenContent();
            interCallback.onNextAction();
            return;
        }

        AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_START_SHOW_SPLASH);
        Runnable runnableShowInter = () -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                mainHandler.post(interCallback::onAdFailedToShowFullScreenContent);
                AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_SHOW_FAILED_SPLASH_ACTIVITY);
                return;
            }

            if (AppOpenManager.getInstance().isAppInBackground) {
                mainHandler.post(interCallback::onAdFailedToShowFullScreenContent);
                AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_SHOW_FAILED_SPLASH_APP_BG);
                return;
            }
            boolean isNextActionOnDismiss = Admob.getInstance().isOpenActivityAfterShowInterAds();
            InterstitialAd interstitialAd = mInterstitialAd;
            if (interstitialAd == null) {
                mainHandler.post(interCallback::onAdFailedToShowFullScreenContent);
                AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_SHOW_FAILED_SPLASH_AD_NULL);
                return;
            }
            InterstitialAdEventCallback adEventCallback = new InterstitialAdEventCallback() {
                @Override
                public void onAdPaid(@NonNull AdValue adValue) {
                    //Tracking revenue (Adjust)
                    AdjustUtil.trackRevenue(interstitialAd.getResponseInfo().getLoadedAdSourceResponseInfo(), adValue, adUnitId, "inter_splash");
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_INTER_SPLASH_DISMISS);
                    mainHandler.post(interCallback::onAdDismissedFullScreenContent);
                    if (isNextActionOnDismiss) interCallback.onNextAction();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError adError) {
                    AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_INTER_SPLASH_FAILED_SHOW);
                    mainHandler.post(interCallback::onAdFailedToShowFullScreenContent);
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    mInterstitialAd = null;
                    AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_INTER_SPLASH_SHOW);
                    mainHandler.post(interCallback::onAdShowedFullScreenContent);
                }

                @Override
                public void onAdImpression() {
                    AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_INTER_SPLASH_IMPRESSION);
                    mainHandler.post(interCallback::onAdImpression);
                }

                @Override
                public void onAdClicked() {
                    AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_INTER_SPLASH_CLICK);
                    mainHandler.post(interCallback::onAdClicked);
                }
            };

            if (!isNextActionOnDismiss) {
                interCallback.onNextAction();
            }
            // Next-Gen SDK calls ad events on a background thread -> dispatch them on the main thread.
            interstitialAd.setAdEventCallback(NextGenAds.onMain(adEventCallback));
            interstitialAd.show(activity);
        };
        mainHandler.postDelayed(runnableShowInter, 250);
    }

    private void loadInterSplashLegacy(@NonNull Context context,
                                       @NonNull String adUnitId,
                                       @NonNull String remoteKey,
                                       @NonNull InterCallback interCallback,
                                       @Nullable ILoadingAdSplashListener loadingListener
    ) {
        if (isLoading.get()) {
            if (loadingListener != null) loadingListener.onLoading();
            Log.d(TAG, "loadInterSplashLegacy: already loading");
            return;
        }

        if (mInterstitialAd != null) {
            Log.d(TAG, "loadInterSplashLegacy: already loaded");
            interCallback.onAdLoaded(mInterstitialAd);
            return;
        }

        if (checkNotAllowCondition(context, remoteKey, EventNameSplash.EVENT_LOAD_FAILED_SPLASH_CONDITION)) {
            interCallback.onAdFailedToLoad();
            return;
        }

        isLoading.set(true);
        AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_START_LOAD_SPLASH_LEGACY);
        AdLoadCallback<InterstitialAd> callback = new AdLoadCallback<InterstitialAd>() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_LOADED_SPLASH_LEGACY);
                isLoading.set(false);
                interCallback.onAdLoaded(interstitialAd);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Bundle bundle = new Bundle();
                String messageFailed = "code_" + loadAdError.getCode().getValue() + "_message_" + loadAdError.getMessage();
                if (messageFailed.length() > 100) {
                    messageFailed = messageFailed.substring(0, 100);
                }
                bundle.putString(KeyParameterEventSplash.KEY_FAILED_MESSAGE, messageFailed);
                AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_LOAD_FAILED_SPLASH_LEGACY, bundle);
                Log.e(TAG, "loadInterSplashLegacy: onAdFailedToLoad " + loadAdError.getMessage());
                isLoading.set(false);
                interCallback.onAdFailedToLoad();
            }
        };
        NextGenAds.loadInterstitial(context, adUnitId, callback);
    }

    private void loadInterSplashPreload(@NonNull Context context,
                                        @NonNull String adUnitId,
                                        @NonNull String remoteKey,
                                        int numberPreload,
                                        @NonNull InterCallback interCallback,
                                        @Nullable ILoadingAdSplashListener loadingListener
    ) {
        if (isLoading.get()) {
            if (loadingListener != null) loadingListener.onLoading();
            Log.d(TAG, "loadInterSplashLegacy: already loading");
            return;
        }

        if (checkNotAllowCondition(context, remoteKey, EventNameSplash.EVENT_LOAD_FAILED_SPLASH_CONDITION)) {
            interCallback.onAdFailedToLoad();
            return;
        }

        AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_START_LOAD_SPLASH_PRELOAD);

        PreloadConfiguration configuration = new PreloadConfiguration(new AdRequest.Builder(adUnitId).build(), numberPreload);

        PreloadCallback callback = new PreloadCallback() {
            @Override
            public void onAdsExhausted(@NonNull String s) {
                Bundle bundle = new Bundle();
                String messageFailed = "ads_exhausted" + s;
                if (messageFailed.length() > 100) {
                    messageFailed = messageFailed.substring(0, 100);
                }
                bundle.putString(KeyParameterEventSplash.KEY_FAILED_MESSAGE, messageFailed);
                AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_LOAD_EXHAUSTED_SPLASH_PRELOAD, bundle);
            }

            @Override
            public void onAdPreloaded(@NonNull String s, @NonNull ResponseInfo responseInfo) {
                AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_LOADED_SPLASH_PRELOAD);
                mainHandler.post(() -> interCallback.onAdLoaded(mInterstitialAd));
            }

            @Override
            public void onAdFailedToPreload(@NonNull String s, @NonNull LoadAdError adError) {
                Bundle bundle = new Bundle();
                String messageFailed = "code_" + adError.getCode().getValue() + "_message_" + adError.getMessage();
                if (messageFailed.length() > 100) {
                    messageFailed = messageFailed.substring(0, 100);
                }
                Log.e(TAG, "onAdFailedToPreload: \n" + adError.getMessage());
                bundle.putString(KeyParameterEventSplash.KEY_FAILED_MESSAGE, messageFailed);
                AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_LOAD_FAILED_SPLASH_PRELOAD, bundle);
                mainHandler.post(interCallback::onAdFailedToLoad);
            }
        };
        isLoading.set(true);
        NextGenAds.runWhenSdkReady(context, () -> isLoading.set(InterstitialAdPreloader.start(adUnitId, configuration, callback)));
    }

    private void pushAdToCache(InterstitialAd interstitialAd, String adUnitId) {
        mInterstitialAd = interstitialAd;
    }

    private static boolean checkNotAllowCondition(
            @NonNull Context context,
            @NonNull String remoteKey,
            @NonNull String nameLogEventFirebase
    ) {
        if (!NetworkUtil.isNetworkActive(context)
                || !AdsConsentManager.getConsentResult(context)
                || !RemoteConfigHelper.getInstance().get_config(context, remoteKey)
                || !Admob.getInstance().getShowAllAds()
        ) {
            String condition = NetworkUtil.isNetworkActive(context) + "_" +
                    AdsConsentManager.getConsentResult(context) + "_" +
                    RemoteConfigHelper.getInstance().get_config(context, remoteKey) + "_" +
                    Admob.getInstance().getShowAllAds();
            Log.e(TAG, "loadInterSplash: condition failed " + condition);
            Bundle bundle = new Bundle();
            bundle.putString(KeyParameterEventSplash.KEY_FAILED_MESSAGE, "condition_" + condition);
            AsyncSplash.Companion.getInstance().logEventStep(TAG, nameLogEventFirebase, bundle);
            return true;
        }
        return false;
    }

    public void loadAd(Context context, String adUnitId, String remoteKey, int numberPreload, InterCallback interCallback,
                      boolean isUseAdPreloading) {
        if (isUseAdPreloading) {
            loadInterSplashPreload(context, adUnitId, remoteKey, numberPreload, interCallback, null);
        } else {
            loadInterSplashLegacy(context, adUnitId, remoteKey, interCallback, null);
        }
    }

    public void cancelPreload(String adUnitId) {
        Log.d(TAG, "cancelPreload: ");
        NextGenAds.destroyInterstitialPreload(adUnitId);
        isLoading.set(false);
    }

    public void loadAndShowLegacy(Activity activity, String adUnitId, String remoteKey, InterCallback interCallback) {
        loadInterSplashLegacy(activity.getApplicationContext(), adUnitId, remoteKey, new InterCallback() {
            @Override
            public void onAdLoaded(InterstitialAd interstitialAd) {
                if (AdmobAdsConfig.getInstance().isTimeout()) {
                    Bundle bundle = new Bundle();
                    String messageFailed = "time_out_splash_screen";
                    bundle.putString(KeyParameterEventSplash.KEY_FAILED_MESSAGE, messageFailed);
                    AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_SHOW_FAILED_SPLASH_TIMEOUT, bundle);
                    return;
                }

                pushAdToCache(interstitialAd, adUnitId);
                showInterSplash(activity, adUnitId, remoteKey, interCallback);
            }

            @Override
            public void onAdFailedToLoad() {
                interCallback.onAdFailedToLoad();
                if (!isLoading.get()) interCallback.onNextAction();
            }
        }, () -> {
            if (mInterstitialAd != null){
                if (AdmobAdsConfig.getInstance().isTimeout()) {
                    Bundle bundle = new Bundle();
                    String messageFailed = "time_out_splash_screen";
                    bundle.putString(KeyParameterEventSplash.KEY_FAILED_MESSAGE, messageFailed);
                    AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_SHOW_FAILED_SPLASH_TIMEOUT, bundle);
                    return;
                }

                showInterSplash(activity, adUnitId, remoteKey, interCallback);
            }
        });
    }

    public void loadAndShow(Activity activity, String adUnitId, String remoteKey, int numberPreload, InterCallback interCallback, boolean isUseAdPreloading) {
        if (isUseAdPreloading) {
            AdsSplash.getInstance()
                    .loadAndShowPreload(activity, adUnitId, remoteKey, numberPreload, interCallback);
        } else {
            AdsSplash.getInstance().loadAndShowLegacy(activity, adUnitId, remoteKey, interCallback);
        }
    }

    public void loadAndShowPreload(Activity activity, String adUnitId, String remoteKey, int numberPreload, InterCallback interCallback) {
        loadInterSplashPreload(activity.getApplicationContext(), adUnitId, remoteKey, numberPreload, new InterCallback() {
            @Override
            public void onAdLoaded(InterstitialAd interstitialAd) {
                Log.d(TAG, "onAdLoaded: " + NextGenAds.getInterstitialPreloadCount(adUnitId));
                if (NextGenAds.getInterstitialPreloadCount(adUnitId) >= numberPreload) {
                    pushAdToCache(Objects.requireNonNull(NextGenAds.pollInterstitial(adUnitId)), adUnitId);
                    cancelPreload(adUnitId);
                    EventTrackingHelper.getInstance(activity).logEvent(TAG+"_cancel_preload");

                    if (AdmobAdsConfig.getInstance().isTimeout()) {
                        Bundle bundle = new Bundle();
                        String messageFailed = "time_out_splash_screen";
                        bundle.putString(KeyParameterEventSplash.KEY_FAILED_MESSAGE, messageFailed);
                        AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_SHOW_FAILED_SPLASH_TIMEOUT, bundle);
                        return;
                    }
                    showInterSplash(activity, adUnitId, remoteKey, interCallback);
                }
            }

            @Override
            public void onAdFailedToLoad() {
                interCallback.onAdFailedToLoad();
            }
        },()->{
            if (NextGenAds.getInterstitialPreloadCount(adUnitId) >= numberPreload) {
                pushAdToCache(Objects.requireNonNull(NextGenAds.pollInterstitial(adUnitId)), adUnitId);

                if (AdmobAdsConfig.getInstance().isTimeout()) {
                    Bundle bundle = new Bundle();
                    String messageFailed = "time_out_splash_screen";
                    bundle.putString(KeyParameterEventSplash.KEY_FAILED_MESSAGE, messageFailed);
                    AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_SHOW_FAILED_SPLASH_TIMEOUT, bundle);
                    return;
                }
                showInterSplash(activity, adUnitId, remoteKey, interCallback);
            }
        });
    }

    public void showCacheInterSplash(Activity activity, String adUnitId, String remoteKey, InterCallback interCallback) {
        Log.d(TAG, "showCacheInterSplash: " + (mInterstitialAd == null) + " available: " + NextGenAds.isInterstitialPreloaded(adUnitId));
        if (mInterstitialAd == null) {
            Log.d(TAG, "showCacheInterSplash: " + NextGenAds.getInterstitialPreloadCount(adUnitId));
            mInterstitialAd = NextGenAds.pollInterstitial(adUnitId);
            cancelPreload(adUnitId);
            EventTrackingHelper.getInstance(activity).logEvent(TAG+"_cancel_preload");
        }
        showInterSplash(activity, adUnitId, remoteKey, interCallback);
    }

     public void clear(){
         mInterstitialAd = null;
         isLoading.set(false);
     }
}