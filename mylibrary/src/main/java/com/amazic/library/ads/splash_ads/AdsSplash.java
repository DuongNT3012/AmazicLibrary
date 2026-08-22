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
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.ads.callback.InterCallback;
import com.amazic.library.ump.AdsConsentManager;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.interstitial.InterstitialAdPreloader;
import com.google.android.gms.ads.preload.PreloadCallbackV2;
import com.google.android.gms.ads.preload.PreloadConfiguration;

import java.util.Objects;

public class AdsSplash {

    private static final String TAG = "adssplash";

    private static volatile AdsSplash instance;

    private InterstitialAd mInterstitialAd;
    private boolean isLoading = false;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
        if (mInterstitialAd == null && !InterstitialAdPreloader.isAdAvailable(adUnitId)) {
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
            FullScreenContentCallback fullScreenContentCallback = new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_INTER_SPLASH_DISMISS);
                    mainHandler.post(interCallback::onAdDismissedFullScreenContent);
                    if (isNextActionOnDismiss) interCallback.onNextAction();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                    super.onAdFailedToShowFullScreenContent(adError);
                    AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_INTER_SPLASH_FAILED_SHOW);
                    mainHandler.post(interCallback::onAdFailedToShowFullScreenContent);
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent();
                    AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_INTER_SPLASH_SHOW);
                    mainHandler.post(interCallback::onAdShowedFullScreenContent);
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_INTER_SPLASH_IMPRESSION);
                    mainHandler.post(interCallback::onAdImpression);
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_INTER_SPLASH_CLICK);
                    mainHandler.post(interCallback::onAdClicked);
                }
            };

            if (!isNextActionOnDismiss) {
                interCallback.onNextAction();
            }
            mInterstitialAd.setFullScreenContentCallback(fullScreenContentCallback);
            mInterstitialAd.show(activity);
        };
        mainHandler.postDelayed(runnableShowInter, 250);
    }

    private void loadInterSplashLegacy(@NonNull Context context,
                                       @NonNull String adUnitId,
                                       @NonNull String remoteKey,
                                       @NonNull InterCallback interCallback
    ) {
        if (isLoading) {
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

        isLoading = true;
        AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_START_LOAD_SPLASH_LEGACY);
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAdLoadCallback callback = new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                super.onAdLoaded(interstitialAd);
                AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_LOADED_SPLASH_LEGACY);
                isLoading = false;
                interCallback.onAdLoaded(interstitialAd);
            }

            @Override
            public void onAdFailedToLoad(@NonNull com.google.android.gms.ads.LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Bundle bundle = new Bundle();
                String messageFailed = "code_" + loadAdError.getCode() + "_message_" + loadAdError.getMessage();
                if (messageFailed.length() > 100) {
                    messageFailed = messageFailed.substring(0, 100);
                }
                bundle.putString(KeyParameterEventSplash.KEY_FAILED_MESSAGE, messageFailed);
                AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_LOAD_FAILED_SPLASH_LEGACY, bundle);
                Log.e(TAG, "loadInterSplashLegacy: onAdFailedToLoad " + loadAdError.getMessage());
                isLoading = false;
                interCallback.onAdFailedToLoad();
            }
        };
        InterstitialAd.load(context, adUnitId, adRequest, callback);
    }

    private void loadInterSplashPreload(@NonNull Context context,
                                        @NonNull String adUnitId,
                                        @NonNull String remoteKey,
                                        int numberPreload,
                                        @NonNull InterCallback interCallback
    ) {
        if (checkNotAllowCondition(context, remoteKey, EventNameSplash.EVENT_LOAD_FAILED_SPLASH_CONDITION)) {
            interCallback.onAdFailedToLoad();
            return;
        }

        AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_START_LOAD_SPLASH_PRELOAD);

        PreloadConfiguration configuration = new PreloadConfiguration.Builder(adUnitId)
                .setBufferSize(numberPreload)
                .build();

        PreloadCallbackV2 callback = new PreloadCallbackV2() {
            @Override
            public void onAdsExhausted(@NonNull String s) {
                super.onAdsExhausted(s);
                Bundle bundle = new Bundle();
                String messageFailed = "ads_exhausted" + s;
                if (messageFailed.length() > 100) {
                    messageFailed = messageFailed.substring(0, 100);
                }
                bundle.putString(KeyParameterEventSplash.KEY_FAILED_MESSAGE, messageFailed);
                AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_LOAD_EXHAUSTED_SPLASH_PRELOAD, bundle);
            }

            @Override
            public void onAdPreloaded(@NonNull String s, @Nullable ResponseInfo responseInfo) {
                super.onAdPreloaded(s, responseInfo);
                AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_LOADED_SPLASH_PRELOAD);
                mainHandler.post(() -> interCallback.onAdLoaded(mInterstitialAd));
            }

            @Override
            public void onAdFailedToPreload(@NonNull String s, @NonNull AdError adError) {
                super.onAdFailedToPreload(s, adError);
                Bundle bundle = new Bundle();
                String messageFailed = "code_" + adError.getCode() + "_message_" + adError.getMessage();
                if (messageFailed.length() > 100) {
                    messageFailed = messageFailed.substring(0, 100);
                }
                Log.e(TAG, "onAdFailedToPreload: \n" + adError.getMessage());
                bundle.putString(KeyParameterEventSplash.KEY_FAILED_MESSAGE, messageFailed);
                AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_LOAD_FAILED_SPLASH_PRELOAD, bundle);
                mainHandler.post(interCallback::onAdFailedToLoad);
            }
        };
        InterstitialAdPreloader.start(adUnitId, configuration, callback);
    }

    public void cancelPreload(String adUnitId) {
        InterstitialAdPreloader.destroy(adUnitId);
    }

    public void loadAndShowLegacy(Activity activity, String adUnitId, String remoteKey, InterCallback interCallback) {
        loadInterSplashLegacy(activity, adUnitId, remoteKey, new InterCallback() {
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
                showInterSplash(activity, remoteKey, adUnitId, interCallback);
            }

            @Override
            public void onAdFailedToLoad() {
                interCallback.onAdFailedToLoad();
                if (!isLoading) interCallback.onNextAction();
            }
        });
    }

    private void pushAdToCache(InterstitialAd interstitialAd, String adUnitId) {
        interstitialAd.setOnPaidEventListener(adValue -> {
            //Adjust
            AdjustUtil.trackRevenue(interstitialAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, adUnitId, "inter_splash");
        });
        mInterstitialAd = interstitialAd;
    }

    public void loadAndShowPreload(Activity activity, String adUnitId, String remoteKey, int numberPreload, InterCallback interCallback) {
        loadInterSplashPreload(activity, adUnitId, remoteKey, numberPreload, new InterCallback() {
            @Override
            public void onAdLoaded(InterstitialAd interstitialAd) {
                Log.d(TAG, "onAdLoaded: " + InterstitialAdPreloader.getNumAdsAvailable(adUnitId));
                if (InterstitialAdPreloader.getNumAdsAvailable(adUnitId) >= numberPreload) {
                    pushAdToCache(Objects.requireNonNull(InterstitialAdPreloader.pollAd(adUnitId)), adUnitId);

                    EventTrackingHelper.getInstance(activity).logEvent(TAG+"_cancel_preload");
                    cancelPreload(adUnitId);
                    if (AdmobAdsConfig.getInstance().isTimeout()) {
                        Bundle bundle = new Bundle();
                        String messageFailed = "time_out_splash_screen";
                        bundle.putString(KeyParameterEventSplash.KEY_FAILED_MESSAGE, messageFailed);
                        AsyncSplash.Companion.getInstance().logEventStep(TAG, EventNameSplash.EVENT_SHOW_FAILED_SPLASH_TIMEOUT, bundle);
                        return;
                    }
                    showInterSplash(activity, remoteKey, adUnitId, interCallback);
                }
            }

            @Override
            public void onAdFailedToLoad() {
                interCallback.onAdFailedToLoad();
            }
        });
    }

    public void showCacheInterSplash(Activity activity, String adUnitId, String remoteKey, InterCallback interCallback) {
        if (mInterstitialAd == null) {
            mInterstitialAd = InterstitialAdPreloader.pollAd(adUnitId);
            cancelPreload(adUnitId);
            EventTrackingHelper.getInstance(activity).logEvent(TAG+"_cancel_preload");
        }
        showInterSplash(activity, remoteKey, adUnitId, interCallback);
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
}