package com.amazic.library.ads.splash_ads.older_version;

import static com.amazic.library.Utils.EventTrackingHelper.time_splash_loading_ad_show;
import static com.amazic.library.Utils.EventTrackingHelper.time_splash_loading_show;
import static java.lang.Math.round;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.amazic.library.Utils.AdjustUtil;
import com.amazic.library.Utils.EventTrackingHelper;
import com.amazic.library.Utils.NetworkUtil;
import com.amazic.library.Utils.RemoteConfigHelper;
import com.amazic.library.Utils.SharePreferenceHelper;
import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.ads.callback.InterCallback;
import com.amazic.library.ads.native_ads.MetaNativeManager;
import com.amazic.library.ads.native_ads.NativeAfterInterManager;
import com.amazic.library.ads.splash_ads.AdmobAdsConfig;
import com.amazic.library.ads.splash_ads.AsyncSplash;
import com.amazic.library.dialog.LoadingAdsDialog;
import com.amazic.library.organic.TechManager;
import com.amazic.library.ump.AdsConsentManager;
import com.amazic.library.view.NativeAfterInterActivity;
import com.amazic.library.view.NativeMetaSplashActivity;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.interstitial.InterstitialAdPreloader;
import com.google.android.gms.ads.preload.PreloadCallbackV2;
import com.google.android.gms.ads.preload.PreloadConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AdsSplash centralizes every interstitial / native-full ad flow shown on the app's Splash
 * screen (Admob preload API, legacy load+show API, high/normal priority async variant, and the
 * native-full-screen splash variants for Admob + Meta).
 * <p>
 * It was extracted out of {@link Admob} (which used to contain ALL ad formats in a single
 * ~4900-line file) so that splash logic can be read, modified and extended on its own, without
 * scrolling through banner/native/reward/reward-inter code that has nothing to do with splash.
 * <p>
 * Shared, cross-format state (the loading dialog, the global "show ads" switch, whether an
 * activity should be opened after an inter is dismissed, and whether an inter/reward ad is
 * currently showing) still lives on {@link Admob#getInstance()} and is read/written through it,
 * since those flags are shared across inter/native/reward flows too.
 * <p>
 * NOTE: unlike the old shared {@code runnable}/{@code handlerTimeoutSplash} pair in {@link Admob}
 * (which was also reused by the inter-ads and reward-ads timeout logic - a latent bug where
 * cancelling one timeout could silently cancel another), this class now owns its own
 * {@link #handlerTimeoutSplash} and {@link #runnable}, so splash timeouts can no longer be
 * clobbered by unrelated inter/reward ad loads.
 */
public class AdsSplashOld {

    private static final String TAG = "AdsSplash";
    private static AdsSplashOld INSTANCE;
    // Timeout handling for splash ad loading (own Handler + Runnable - see class javadoc).
    private final Handler handlerTimeoutSplash = new Handler(Looper.getMainLooper());
    // Currently held interstitial ad instances for the splash screen.
    private InterstitialAd mInterstitialAdSplashHigh;

    //================================ State ================================
    private InterstitialAd mInterstitialAdSplash;
    private Runnable runnable;
    private int timeOutCallSplashAds = 12000;
    private boolean isLoadInterSplashIdTimeout = false;
    // Delay/"wait for load" bookkeeping used by the preloading-splash-delay flow.
    private Runnable timerDelayRunnable;
    private boolean isTimerDelayFinished = false;
    private boolean isAdLoadAdsSplashFinished = false;

    private Long timeStartCalInterSplash = 0L;
    private Long timeLastCalInterSplash = 0L;
    private Boolean isFirstLoadedInterSplash = false;

    // Show-state / click / result tracking.
    private boolean isSplashResume = true;
    private boolean isFailToShowAdSplash = false;
    private int countClickInterSplashAds = 0;
    private long timeSplashLoadingAdShow = 0;
    // High vs normal priority ad race (loadAndShowIdInterAdSplashAsync).
    private boolean isShownInterSplashHigh = false;
    private boolean isShownInterSplashNormal = false;
    private int timeDelayWaitInterHigh = 2000;
    private boolean isHandledLoadAdsSplashFail = false;

    private AdsSplashOld() {
    }

    public static AdsSplashOld getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AdsSplashOld();
        }
        return INSTANCE;
    }

    //================================ Getters / setters ================================

    public InterstitialAd getInterstitialAdSplashHigh() {
        return mInterstitialAdSplashHigh;
    }

    public void setInterstitialAdSplashHigh(InterstitialAd mInterstitialAdSplashHigh) {
        this.mInterstitialAdSplashHigh = mInterstitialAdSplashHigh;
    }

    public InterstitialAd getInterstitialAdSplash() {
        return mInterstitialAdSplash;
    }

    public void setInterstitialAdSplash(InterstitialAd mInterstitialAdSplash) {
        this.mInterstitialAdSplash = mInterstitialAdSplash;
    }

    public int getTimeOutCallSplashAds() {
        return timeOutCallSplashAds;
    }

    public void setTimeOutCallSplashAds(int timeOutCallSplashAds) {
        this.timeOutCallSplashAds = timeOutCallSplashAds;
    }

    public int getTimeDelayWaitInterHigh() {
        return timeDelayWaitInterHigh;
    }

    public void setTimeDelayWaitInterHigh(int timeDelayWaitInterHigh) {
        this.timeDelayWaitInterHigh = timeDelayWaitInterHigh;
    }

    public void removeHandlerSplashAds() {
        if (runnable != null) {
            handlerTimeoutSplash.removeCallbacks(runnable);
            handlerTimeoutSplash.removeCallbacksAndMessages(null);
        }
    }

    /**
     * Called from {@code Admob.resetVariable()} at the start of {@code initAdmob()} so a stale
     * timeout flag from a previous splash run doesn't block the next one.
     */
    public void resetSplashTimeoutFlag() {
        this.isLoadInterSplashIdTimeout = false;
    }

    //================================ Navigation helpers ================================

    private void startNativeAfterInterSplash(Activity activity, InterCallback interCallback) {
        NativeAfterInterActivity.Companion.setInterCallback(interCallback);
        NativeAfterInterActivity.Companion.setSplashMode(true);
        Intent intent = new Intent(activity, NativeAfterInterActivity.class);
        activity.startActivity(intent);
    }

    private void startNativeMetaSplash(Activity activity, InterCallback interCallback) {
        NativeMetaSplashActivity.Companion.setInterCallback(interCallback);
        Intent intent = new Intent(activity, NativeMetaSplashActivity.class);
        activity.startActivity(intent);
    }

    //================================ Native Full Splash (Admob) ================================
    // Shows a full-screen native ad repeatedly (up to a remote-config-controlled count) on splash.

    public void loadAndShowNativeFullSplashCount(AppCompatActivity activity, List<String> listIdNative, InterCallback interCallback, String adsKeyNative, String remoteKeyNative) {
        long nativeFullSplashStartTime = System.currentTimeMillis();
        Log.d(TAG, "AdsSplash Native Full Splash: Bắt đầu tiến trình Load And Show Native Full Splash...");

        // Check basic conditions
        if (!NetworkUtil.isNetworkActive(activity) || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds()) {
            Log.d(TAG, "AdsSplash Native Full Splash: condition failed → onNextAction");
            if (interCallback != null) interCallback.onNextAction();
            return;
        }

        boolean isConfigShowNative = RemoteConfigHelper.getInstance().get_config(activity, remoteKeyNative);
        if (!isConfigShowNative) {
            Log.d(TAG, "AdsSplash Native Full Splash: remote config OFF → onNextAction");
            if (interCallback != null) interCallback.onNextAction();
            return;
        }

        if (listIdNative == null || listIdNative.isEmpty()) {
            Log.d(TAG, "AdsSplash Native Full Splash: no IDs → onNextAction");
            if (interCallback != null) interCallback.onNextAction();
            return;
        }

        int targetCount = AdmobAdsConfig.getInstance().getNumberNativeFullShowSplash();

        // Timeout handler
        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        boolean[] hasNavigated = {false};
        Runnable timeoutRunnable = () -> {
            if (!hasNavigated[0]) {
                hasNavigated[0] = true;
                Log.d(TAG, "AdsSplash Native Full Splash: timeout → onNextAction");
                if (interCallback != null) interCallback.onNextAction();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, timeOutCallSplashAds);

        NativeAfterInterManager.loadNativeFullSplash(
                activity, adsKeyNative, remoteKeyNative, targetCount,
                () -> {
                    // First native loaded → navigate immediately
                    if (!hasNavigated[0]) {
                        hasNavigated[0] = true;
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        long elapsed = System.currentTimeMillis() - nativeFullSplashStartTime;
                        Log.d(TAG, "AdsSplash Native Full Splash: first loaded → startNativeAfterInterSplash | time=" + elapsed + "ms (" + (elapsed / 1000f) + "s)");
                        NativeAfterInterActivity.Companion.setAdsKey(adsKeyNative);
                        NativeAfterInterActivity.Companion.setRemoteKey(remoteKeyNative);
                        startNativeAfterInterSplash(activity, interCallback);
                    }
                },
                () -> {
                    // All slots failed, nothing loaded
                    if (!hasNavigated[0]) {
                        hasNavigated[0] = true;
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        Log.d(TAG, "AdsSplash Native Full Splash: all failed → onNextAction");
                        if (interCallback != null) interCallback.onNextAction();
                    }
                }
        );
    }


    //================================ Native Full Splash (Meta, with Admob fallback) ================================

    public void loadAndShowMetaNativeFullSplashCount(AppCompatActivity activity, InterCallback interCallback) {
        long nativeFullSplashStartTime = System.currentTimeMillis();

        String adsKeyNative = AdmobAdsConfig.getInstance().getKeyNativeFullMetaSplash();
        String remoteKeyNative = AdmobAdsConfig.getInstance().getKeyNativeFullMetaSplash();
        Log.d(TAG, "AdsSplash META Native Full Splash: Bắt đầu tiến trình Load And Show Native Full Splash... adsKeyNative = " + adsKeyNative + ", useNativeFullAdmobWhenMetaFail = " + AdmobAdsConfig.getInstance().isUseNativeFullSplashAdmobWhenMetaFail());

        List<String> listIdNative = AdmobApi.getInstance().getListIDByName(adsKeyNative);
        // Check basic conditions
        if (!NetworkUtil.isNetworkActive(activity) || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds()) {
            Log.d(TAG, "AdsSplash META Native Full Splash: condition failed → onNextAction");
            if (interCallback != null) interCallback.onNextAction();
            return;
        }

        boolean isConfigShowNative = RemoteConfigHelper.getInstance().get_config(activity, remoteKeyNative);
        if (!isConfigShowNative) {
            Log.d(TAG, "AdsSplash META Native Full Splash: remote config OFF → onNextAction");
            if (interCallback != null) interCallback.onNextAction();
            return;
        }

        if (listIdNative == null || listIdNative.isEmpty()) {
            Log.d(TAG, "AdsSplash META Native Full Splash: no IDs → onNextAction");
            if (interCallback != null) interCallback.onNextAction();
            return;
        }

        int targetCount = AdmobAdsConfig.getInstance().getNumberNativeFullShowSplash();

        // Timeout handler
        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        boolean[] hasNavigated = {false};
        Runnable timeoutRunnable = () -> {
            if (!hasNavigated[0]) {
                hasNavigated[0] = true;
                Log.d(TAG, "AdsSplash META Native Full Splash: timeout → onNextAction");
                if (interCallback != null) interCallback.onNextAction();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, timeOutCallSplashAds);

        MetaNativeManager.loadMetaNativeFullSplash(
                activity, listIdNative, adsKeyNative, targetCount,
                () -> {
                    // First native loaded → navigate immediately
                    if (!hasNavigated[0]) {
                        hasNavigated[0] = true;
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        long elapsed = System.currentTimeMillis() - nativeFullSplashStartTime;
                        Log.d(TAG, "AdsSplash META Native Full Splash: first loaded → startNativeAfterInterSplash | time=" + elapsed + "ms (" + (elapsed / 1000f) + "s)");
                        NativeMetaSplashActivity.Companion.setAdsKey(adsKeyNative);
                        NativeMetaSplashActivity.Companion.setRemoteKey(remoteKeyNative);
                        startNativeMetaSplash(activity, interCallback);
                    }
                },
                () -> {
                    // All slots failed, nothing loaded
                    if (!hasNavigated[0]) {
                        hasNavigated[0] = true;
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        Log.d(TAG, "AdsSplash META Native Full Splash: all failed → onNextAction");
                        if (interCallback != null) interCallback.onNextAction();
                    }
                }
        );
    }


    //================================ Inter Splash - Preloading API (new Google preload API) ================================
    // load...Delay: kicks off preloading + starts the "wait until both timer and ad are ready" gate.
    // checkCondition...: the gate itself - only shows once BOTH the min-wait timer and the ad load are done.
    // showInterAdPreloadingSplashDelay: builds the FullScreenContentCallback and actually calls show().

    public void loadAndShowInterAdPreloadingSplash(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback, String adsKeyNative, String remoteKeyNative) {
        Long currentTime = System.currentTimeMillis();
        loadAndShowInterAdPreloadingSplash(activity, listIdInter, interCallback, adsKeyNative, remoteKeyNative, currentTime, currentTime);
    }

    public void loadAndShowInterAdPreloadingSplash(AppCompatActivity activity,
                                                   List<String> listIdInter,
                                                   InterCallback interCallback,
                                                   String adsKeyNative, String remoteKeyNative,
                                                   Long timeStep1, Long timeLastStep) {
        EventTrackingHelper.getInstance(activity).logEvent("splash_preload_start_check");
        ArrayList<String> listIdInterTemp = new ArrayList<>(listIdInter);
        timeStartCalInterSplash = timeStep1;
        timeLastCalInterSplash = timeLastStep;
        isFirstLoadedInterSplash = true;
        isTimerDelayFinished = true;
        if (!NetworkUtil.isNetworkActive(activity) || listIdInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds() /*|| IAPManager.getInstance().isPurchase()*/) {
            Log.d(TAG, "AdsSplash Inter preload: Check condition loadAndShowInterAdSplash " + NetworkUtil.isNetworkActive(activity) + "_" + listIdInterTemp.isEmpty() + "_" + AdsConsentManager.getConsentResult(activity) + "_" + Admob.getInstance().getShowAllAds() + "_" /*+ IAPManager.getInstance().isPurchase()*/);
            interCallback.onNextAction();
            removeHandlerSplashAds();
            Bundle bundle = new Bundle();
            bundle.putString("failed_message", "internet_" + NetworkUtil.isNetworkActive(activity)
                    + "_Con_" + AdsConsentManager.getConsentResult(activity)
                    + "_idEmpty_" + listIdInterTemp.isEmpty()
                    + "_showAllAds_" + Admob.getInstance().getShowAllAds()
            );
            EventTrackingHelper.getInstance(activity).logEventWithMultipleParams("splash_preload_failed_check", bundle);
            logEventSplash(activity, "splash_preload_end_failed", bundle);
            return;
        }
        PreloadConfiguration configuration = new PreloadConfiguration.Builder(listIdInterTemp.get(0))
                .setBufferSize(AdmobAdsConfig.getInstance().getNumberPreloadingSplash())
                .build();

        boolean isConfigShowNativeAfterInter = RemoteConfigHelper.getInstance().get_config(activity, remoteKeyNative);

        boolean isEmptyListNativeAfterInter = AdmobApi.getInstance().getListIDByName(adsKeyNative).isEmpty();

        Log.d(TAG, "loadAndShowInterAdPreloadingSplash: " + listIdInterTemp.get(0));
        EventTrackingHelper.getInstance(activity).logEvent("splash_preload_start_call");
        PreloadCallbackV2 callback = new PreloadCallbackV2() {
            @Override
            public void onAdFailedToPreload(@NonNull String s, @NonNull AdError adError) {
                super.onAdFailedToPreload(s, adError);
                Log.d(TAG, "AdsSplash Inter preload: Preload ad " + s + " failed to load with error: " + adError.getMessage());
                Bundle bundle = new Bundle();
                bundle.putString("failed_message", "load_" + adError.getCode() + "_" + adError.getMessage());
                bundle.putString("time_to_step", String.format(Locale.US, "%.1f", (System.currentTimeMillis() - timeStartCalInterSplash) / 1000.0));
                EventTrackingHelper.getInstance(activity).logEventWithMultipleParams("splash_preload_failed", bundle);
                interCallback.onAdFailedToLoad();
                if (listIdInterTemp.size() > 1) {
                    listIdInterTemp.remove(0);
                    loadAndShowInterAdPreloadingSplashDelay(activity, listIdInterTemp, interCallback, adsKeyNative, remoteKeyNative);
                }
            }

            @Override
            public void onAdPreloaded(@NonNull String s, @Nullable ResponseInfo responseInfo) {
                super.onAdPreloaded(s, responseInfo);
                Bundle bundle = new Bundle();
                if (isFirstLoadedInterSplash) {
                    timeLastCalInterSplash = System.currentTimeMillis();
                    bundle.putString("time_to_step", String.format(Locale.US, "%.1f",
                            (System.currentTimeMillis() - timeStartCalInterSplash) / 1000.0));
                    bundle.putString("time_between_step", String.format(Locale.US, "%.1f",
                            (System.currentTimeMillis() - timeLastCalInterSplash) / 1000.0));
                    isFirstLoadedInterSplash = false;
                    EventTrackingHelper.getInstance(activity).logEventWithMultipleParams("splash_preload_loaded_first", bundle);
                }
                Log.i(TAG, "AdsSplash Inter preload: Ad loaded inter splash.");
                EventTrackingHelper.getInstance(activity).logEventWithMultipleParams("splash_preload_loaded", bundle);
                isAdLoadAdsSplashFinished = true;
                Log.d(TAG, "AdsSplash Inter preload: Preload ad for " + s + " is available.");
                interCallback.onAdLoaded(null);

                Log.d(TAG, "onAdPreloaded have ad data: " + InterstitialAdPreloader.isAdAvailable(listIdInter.get(0)));
                //get data ad inter
                mInterstitialAdSplash = InterstitialAdPreloader.pollAd(listIdInter.get(0));
                Log.d(TAG, "onAdPreloaded done: ad = " + mInterstitialAdSplash);

                //destroy preload ads
                InterstitialAdPreloader.destroy(listIdInter.get(0));
                /// show ads
                checkConditionAdPreloadingSplash(activity, listIdInterTemp, interCallback, isConfigShowNativeAfterInter, isEmptyListNativeAfterInter, adsKeyNative);
                removeHandlerSplashAds();
            }


            @Override
            public void onAdsExhausted(@NonNull String s) {
                super.onAdsExhausted(s);
                if (!isFirstLoadedInterSplash) {
                    logEventSplash(activity, "splash_preload_failed_exhausted");
                }
                Log.d(TAG, "AdsSplash Inter preload: Preload ad for " + s + " is exhausted.");
            }
        };

        InterstitialAdPreloader.start(listIdInterTemp.get(0), configuration, callback);
    }

    public void loadAndShowInterAdPreloadingSplashDelay(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback, String adsKeyNative, String remoteKeyNative) {
        Log.d(TAG, "AdsSplash Inter preload: Bắt đầu tiến trình Load And Show Inter Delay ads...");
        new Handler(Looper.getMainLooper()).post(() ->
                NativeAfterInterManager.preloadNativeAfterInterSplash(activity, adsKeyNative, remoteKeyNative)
        );

        boolean isConfigShowNativeAfterInter = RemoteConfigHelper.getInstance().get_config(activity, remoteKeyNative);

        boolean isEmptyListNativeAfterInter = AdmobApi.getInstance().getListIDByName(adsKeyNative).isEmpty();

        ArrayList<String> listIdInterTemp = new ArrayList<>(listIdInter);
        //Set timeout ads splash x(s) if cannot load
//        runnable = () -> {
//            EventTrackingHelper.getInstance(activity).logEvent(EventTrackingHelper.inter_splash_id_timeout);
//            if (!activity.isFinishing() && !activity.isDestroyed() && Admob.getInstance().loadingAdsDialog != null && Admob.getInstance().loadingAdsDialog.isShowing()) {
//                Admob.getInstance().dismissLoadingDialog();
//            }
//            if (interCallback != null) {
//                isLoadInterSplashIdTimeout = true;
//                if (AdmobAdsConfig.getInstance().isShowNativeAfterInter()) {
//                    if (isConfigShowNativeAfterInter) {
//                        if (isEmptyListNativeAfterInter) {
//                            interCallback.onNextAction();
//                        } else {
//                            if (!NativeAfterInterManager.hasNativeAfterInterSplash(adsKeyNative)) {
//                                interCallback.onNextAction();
//                            } else {
//                                startNativeAfterInterSplash(activity, interCallback);
//                            }
//                        }
//                    } else {
//                        interCallback.onNextAction();
//                    }
//                } else {
//                    interCallback.onNextAction();
//                }
//            }
//            removeHandlerSplashAds();
//        };
//        handlerTimeoutSplash.postDelayed(runnable, timeOutCallSplashAds);

        //delay ads splash
//        if (AdmobAdsConfig.getInstance().getUseNativeSplash()) {
//            timerDelayRunnable = new Runnable() {
//                @Override
//                public void run() {
//                    Log.d(TAG, "AdsSplash Inter preload: Đã đủ 7 giây đếm ngược.");
//                    isTimerDelayFinished = true;
//
//                    //get data ad inter
//                    if (mInterstitialAdSplash == null) {
//                        mInterstitialAdSplash = InterstitialAdPreloader.pollAd(listIdInter.get(0));
//                        Log.d(TAG, "Get data ad Timeout 7s: " + InterstitialAdPreloader.isAdAvailable(listIdInter.get(0)) + ", ad = " + mInterstitialAdSplash);
//
//                        //destroy preload ads
//                        InterstitialAdPreloader.destroy(listIdInter.get(0));
//                    }
//
//                    checkConditionAdPreloadingSplash(activity, listIdInterTemp, interCallback, isConfigShowNativeAfterInter, isEmptyListNativeAfterInter, adsKeyNative);
//                }
//            };
//        } else {
//            Log.d(TAG, "AdsSplash Inter preload: không dùng chờ 7 giây đếm ngược.");
        isTimerDelayFinished = true;
//        }
//        handlerDelayAdsSplash.postDelayed(timerDelayRunnable, timeDelayAdsSplash);
        //end

        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds() /*|| IAPManager.getInstance().isPurchase()*/) {
            Log.d(TAG, "AdsSplash Inter preload: Check condition loadAndShowInterAdSplash " + NetworkUtil.isNetworkActive(activity) + "_" + listIdInterTemp.isEmpty() + "_" + AdsConsentManager.getConsentResult(activity) + "_" + Admob.getInstance().getShowAllAds() + "_" /*+ IAPManager.getInstance().isPurchase()*/);
            interCallback.onNextAction();
            removeHandlerSplashAds();
            Bundle bundle = new Bundle();
            bundle.putString("failed_message", "lib_internet_" + NetworkUtil.isNetworkActive(activity)
                    + "_Consent_" + AdsConsentManager.getConsentResult(activity)
                    + "_isShowAllAds_" + Admob.getInstance().getShowAllAds()
            );
            EventTrackingHelper.getInstance(activity).logEventWithMultipleParams("splash_delay_failed", bundle);
            return;
        }

        //Log event
        Bundle bundle = new Bundle();
        boolean idCheck = AdmobApi.getInstance().getListAdsSize() > 0;
        bundle.putString(EventTrackingHelper.splash_detail, AdsConsentManager.getConsentResult(activity) + "_" + TechManager.getInstance().isTech(activity) + "_" + NetworkUtil.isNetworkActive(activity) + "_" + Admob.getInstance().getShowAllAds() + "_" + idCheck + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        bundle.putString(EventTrackingHelper.ump, String.valueOf(AdsConsentManager.getConsentResult(activity)));
        bundle.putString(EventTrackingHelper.organic, String.valueOf(TechManager.getInstance().isTech(activity)));
        bundle.putString(EventTrackingHelper.haveinternet, String.valueOf(NetworkUtil.isNetworkActive(activity)));
        bundle.putString(EventTrackingHelper.showallad, String.valueOf(Admob.getInstance().getShowAllAds()));
        bundle.putString(EventTrackingHelper.idcheck, String.valueOf(idCheck));
        bundle.putString(EventTrackingHelper.interremote + "_" + EventTrackingHelper.openremote + "_" + EventTrackingHelper.aoavalue, RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.inter_splash) + "_" + RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.open_splash) + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        EventTrackingHelper.getInstance(activity).logEventWithMultipleParams(EventTrackingHelper.inter_splash_tracking, bundle);

        //log event can request
        EventTrackingHelper.getInstance(activity).logEvent(EventTrackingHelper.inter_splash_true);
        //end log event can request
        //time start load splash ads
        timeSplashLoadingAdShow = System.currentTimeMillis();

        Log.d(TAG, "AdsSplash Inter preload: number ad preloading = " + AdmobAdsConfig.getInstance().getNumberPreloadingSplash());

        PreloadConfiguration configuration = new PreloadConfiguration.Builder(listIdInterTemp.get(0)).setBufferSize(AdmobAdsConfig.getInstance().getNumberPreloadingSplash()).build();

        PreloadCallbackV2 callback = new PreloadCallbackV2() {
            @Override
            public void onAdFailedToPreload(@NonNull String s, @NonNull AdError adError) {
                super.onAdFailedToPreload(s, adError);
                Log.d(TAG, "AdsSplash Inter preload: Preload ad " + s + " failed to load with error: " + adError.getMessage());
                Bundle bundle = new Bundle();
                bundle.putString("failed_message", "load_" + adError.getCode() + "_" + adError.getMessage());
                EventTrackingHelper.getInstance(activity).logEventWithMultipleParams("splash_delay_failed", bundle);
                interCallback.onAdFailedToLoad();
                if (listIdInterTemp.size() > 1) {
                    listIdInterTemp.remove(0);
                    loadAndShowInterAdPreloadingSplashDelay(activity, listIdInterTemp, interCallback, adsKeyNative, remoteKeyNative);
                }
            }

            @Override
            public void onAdPreloaded(@NonNull String s, @Nullable ResponseInfo responseInfo) {
                super.onAdPreloaded(s, responseInfo);
                Log.i(TAG, "AdsSplash Inter preload: Ad loaded inter splash.");
                EventTrackingHelper.getInstance(activity).logEvent("splash_delay_true");
                isAdLoadAdsSplashFinished = true;
                Log.d(TAG, "AdsSplash Inter preload: Preload ad for " + s + " is available.");
                interCallback.onAdLoaded(null);

                Log.d(TAG, "onAdPreloaded have ad data: " + InterstitialAdPreloader.isAdAvailable(listIdInter.get(0)));
                //get data ad inter
                mInterstitialAdSplash = InterstitialAdPreloader.pollAd(listIdInter.get(0));
                Log.d(TAG, "onAdPreloaded done: ad = " + mInterstitialAdSplash);

                //destroy preload ads
                InterstitialAdPreloader.destroy(listIdInter.get(0));
                /// show ads
                checkConditionAdPreloadingSplash(activity, listIdInterTemp, interCallback, isConfigShowNativeAfterInter, isEmptyListNativeAfterInter, adsKeyNative);
                removeHandlerSplashAds();
            }


            @Override
            public void onAdsExhausted(@NonNull String s) {
                super.onAdsExhausted(s);
                if (!isFirstLoadedInterSplash) {
                    logEventSplash(activity, "splash_preload_failed_exhausted");
                }
                Log.d(TAG, "AdsSplash Inter preload: Preload ad for " + s + " is exhausted.");
            }
        };

        InterstitialAdPreloader.start(listIdInterTemp.get(0), configuration, callback);
    }

    private void checkConditionAdPreloadingSplash(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback, boolean isConfigShowNativeAfterInter, boolean isEmptyListNativeAfterInter, String adsKeyNative) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.d(TAG, "removeHandlerDelayAdsSplash");
            if (activity != null) {
                Bundle bundle = new Bundle();
                bundle.putString("failed_message", "finishing_" + activity.isFinishing() + "destroyed_" + activity.isDestroyed());
                logEventSplash(activity, "splash_preload_failed_activity", bundle);
            }
            removeHandlerDelayAdsSplash();
            return;
        }

        if (isTimerDelayFinished && isAdLoadAdsSplashFinished) {
            String timeFormatted = String.format(Locale.US, "%.1f", (System.currentTimeMillis() - timeStartCalInterSplash) / 1000.0);
            Log.d(TAG, "AdsSplash Inter preload: ===> TỔNG THỜI GIAN CHỜ: " + timeFormatted + " giây , isEmptyListNativeAfterInter = " + isEmptyListNativeAfterInter);
            logEventSplash(activity, "splash_preload_start_show");
            EventTrackingHelper.getInstance(activity).logEventWithAParam("Splash_time_wait", "time_to_step", timeFormatted);
            showInterAdPreloadingSplashDelay(activity, listIdInter, interCallback, isConfigShowNativeAfterInter, isEmptyListNativeAfterInter, adsKeyNative);
            removeHandlerDelayAdsSplash();
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("failed_message", "timerDelayFi_" + isTimerDelayFinished + "_adLoadAdsFi_" + isAdLoadAdsSplashFinished);
            logEventSplash(activity, "splash_preload_failed_show", bundle);
            interCallback.onNextAction();
        }
    }

    private void logEventSplash(AppCompatActivity activity, String eventName, Bundle bundle) {
        bundle.putString("time_to_step", String.format(Locale.US, "%.1f", (System.currentTimeMillis() - timeStartCalInterSplash) / 1000.0));
        bundle.putString("time_between_step", String.format(Locale.US, "%.1f", (System.currentTimeMillis() - timeLastCalInterSplash) / 1000.0));
        bundle.putString("isTimeout", AdmobAdsConfig.getInstance().isTimeout() + "");
        EventTrackingHelper.getInstance(activity).logEventWithMultipleParams(eventName, bundle);
        timeLastCalInterSplash = System.currentTimeMillis();
    }

    private void logEventSplash(AppCompatActivity activity, String eventName) {
        Bundle bundle = new Bundle();
        bundle.putString("time_to_step", formatStepTime(System.currentTimeMillis() - timeStartCalInterSplash));
        bundle.putString("time_between_step", formatStepTime(System.currentTimeMillis() - timeLastCalInterSplash));
        bundle.putString("isTimeout", AdmobAdsConfig.getInstance().isTimeout() + "");
        EventTrackingHelper.getInstance(activity).logEventWithMultipleParams(eventName, bundle);
        timeLastCalInterSplash = System.currentTimeMillis();
    }

    private String formatStepTime(Long timeMs) {
        float seconds = timeMs / 1000f;

        float step = (seconds < 10) ? 0.5f : 5.0f;
        float rounded = round(seconds / step) * step;

        return String.format(Locale.US, "%.1f", rounded);
    }

    public void showInterAdPreloadingSplashDelay(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback, boolean isConfigShowNativeAfterInter, boolean isEmptyListNativeAfterInter, String adsKeyNative) {
        countClickInterSplashAds = 0;
        activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                DefaultLifecycleObserver.super.onResume(owner);
                isSplashResume = true;
                Log.d(TAG, "AdsSplash Inter preload: onSplashResume: " + true);
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                DefaultLifecycleObserver.super.onStop(owner);
                isSplashResume = false;
                Log.d(TAG, "AdsSplash Inter preload: onSplashResume: " + false);
            }
        });
//        Log.d(TAG, "1.Ads Inter destroy: " + InterstitialAdPreloader.isAdAvailable(listIdInter.get(0)));

//        mInterstitialAdSplash = InterstitialAdPreloader.pollAd(listIdInter.get(0));
//
//        //destroy preload ads
//        InterstitialAdPreloader.destroy(listIdInter.get(0));
//        Log.d(TAG, "2.Ads Inter destroy: " + InterstitialAdPreloader.isAdAvailable(listIdInter.get(0)));

        if (mInterstitialAdSplash == null) {
            logEventSplash(activity, "splash_preload_failed_inter");
            Log.d(TAG, "AdsSplash Inter preload: The interstitial ad wasn't ready yet.");
            AppOpenManager.getInstance().setEnableResume(true);
            if (AdmobAdsConfig.getInstance().isShowNativeAfterInter()) {
                if (isConfigShowNativeAfterInter) {
                    if (isEmptyListNativeAfterInter) {
                        interCallback.onNextAction();
                    } else {
                        if (!NativeAfterInterManager.hasNativeAfterInterSplash(adsKeyNative)) {
                            interCallback.onNextAction();
                        } else {
                            startNativeAfterInterSplash(activity, interCallback);
                        }
                    }
                } else {
                    interCallback.onNextAction();
                }
            } else {
                interCallback.onNextAction();
            }
            return;
        }

        if (!isLoadInterSplashIdTimeout && !activity.isFinishing() && !activity.isDestroyed()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                logEventSplash(activity, "splash_preload_start_show_inter");
                mInterstitialAdSplash.setOnPaidEventListener(
                        adValue -> {
                            AdjustUtil.trackRevenue(mInterstitialAdSplash.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdInter.get(0), "inter_splash_preloading");
                        }
                );

                mInterstitialAdSplash.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdClicked() {
                        AppOpenManager.isLastActionClickAd = true;
                        Log.d(TAG, "AdsSplash Inter preload: Ad was clicked.");
                        interCallback.onAdClicked();
                        countClickInterSplashAds++;
                        int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                        if (splashOpenTimes == 1) {
                            EventTrackingHelper.getInstance(activity).logEvent(EventTrackingHelper.inter_splash_click + "_" + countClickInterSplashAds);
                        }
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        logEventSplash(activity, "step_inter_splash_dismiss");
                        //increase splash open
                        SharePreferenceHelper.setInt(activity, EventTrackingHelper.splash_open, SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1);
                        //end increase splash open
                        Log.d(TAG, "AdsSplash Inter preload: Ad dismissed fullscreen content.");
                        interCallback.onAdDismissedFullScreenContent();
                        AppOpenManager.getInstance().setEnableResume(true);
                        Log.d(TAG, "AdsSplash Inter preload: start check Admob.getInstance().isOpenActivityAfterShowInterAds() = " + Admob.getInstance().isOpenActivityAfterShowInterAds());

                        Log.d(TAG, "AdsSplash Inter preload: start check isShowNativeAfterInter = " + AdmobAdsConfig.getInstance().isShowNativeAfterInter());
                        if (AdmobAdsConfig.getInstance().isShowNativeAfterInter()) {
                            Log.d(TAG, "AdsSplash Inter preload: start check isConfigShowNativeAfterInter = " + isConfigShowNativeAfterInter + ", isEmptyListNativeAfterInter = " + isEmptyListNativeAfterInter);
                            if (isConfigShowNativeAfterInter) {
                                if (isEmptyListNativeAfterInter) {
                                    interCallback.onNextAction();
                                } else {
                                    if (!NativeAfterInterManager.hasNativeAfterInterSplash(adsKeyNative)) {
                                        interCallback.onNextAction();
                                    } else {
                                        startNativeAfterInterSplash(activity, interCallback);
                                    }
                                }
                            } else {
                                interCallback.onNextAction();
                            }
                        } else {
                            if (!Admob.getInstance().isOpenActivityAfterShowInterAds()) {
                                interCallback.onNextAction();
                            }
                        }
                        Admob.getInstance().setInterOrRewardedShowing(false);
                        mInterstitialAdSplash = null;
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        //increase splash open
                        logEventSplash(activity, "step_inter_splash_show_failded");
                        SharePreferenceHelper.setInt(activity, EventTrackingHelper.splash_open, SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1);
                        //end increase splash open
                        Log.e(TAG, "AdsSplash Inter preload: Ad failed to show fullscreen content.");
                        interCallback.onAdFailedToShowFullScreenContent();
                        if (isSplashResume || AdmobAdsConfig.getInstance().isShowNativeAfterInter()) {
                            if (isConfigShowNativeAfterInter) {
                                if (isEmptyListNativeAfterInter) {
                                    interCallback.onNextAction();
                                } else {
                                    if (!NativeAfterInterManager.hasNativeAfterInterSplash(adsKeyNative)) {
                                        interCallback.onNextAction();
                                    } else {
                                        startNativeAfterInterSplash(activity, interCallback);
                                    }
                                }
                            } else {
                                interCallback.onNextAction();
                            }
                        } else {
                            if (!Admob.getInstance().isOpenActivityAfterShowInterAds()) {
                                interCallback.onNextAction();
                            }
                        }
                        if (!activity.isFinishing() && !activity.isDestroyed() && Admob.getInstance().loadingAdsDialog != null && Admob.getInstance().loadingAdsDialog.isShowing()) {
                            Admob.getInstance().dismissLoadingDialog();
                        }
                        isFailToShowAdSplash = true;
                        AppOpenManager.getInstance().setEnableResume(true);
                        Admob.getInstance().setInterOrRewardedShowing(false);
                        removeHandlerSplashAds();
                        //log event
                        EventTrackingHelper.getInstance(activity).logEventWithAParam(EventTrackingHelper.inter_splash_showad_time, EventTrackingHelper.showad_time, "false_" + (System.currentTimeMillis() - AdmobAdsConfig.getInstance().getTimeStartSplash()) / 1000);
                        //end log event
                        mInterstitialAdSplash = null;
                    }

                    @Override
                    public void onAdImpression() {
                        logEventSplash(activity, "step_inter_splash_impress");
                        // Called when an impression is recorded for an ad.
                        Log.d(TAG, "AdsSplash Inter preload: Ad impression. time - " + (System.currentTimeMillis() - timeSplashLoadingAdShow) / 1000);
                        interCallback.onAdImpression();
                        //log event
                        EventTrackingHelper.getInstance(activity).logEventWithAParam(time_splash_loading_ad_show, time_splash_loading_show, String.valueOf((System.currentTimeMillis() - timeSplashLoadingAdShow) / 1000));
                        EventTrackingHelper.getInstance(activity).logEventWithAParam(EventTrackingHelper.inter_splash_showad_time, EventTrackingHelper.showad_time, "true_" + (System.currentTimeMillis() - AdmobAdsConfig.getInstance().getTimeStartSplash()) / 1000);
                        int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                        if (splashOpenTimes <= 3) {
                            EventTrackingHelper.getInstance(activity).logEvent(EventTrackingHelper.inter_splash_impression + "_" + splashOpenTimes);
                        }
                        //end log event
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        logEventSplash(activity, "step_inter_splash_show");
                        // Called when ad is shown.
                        Log.d(TAG, "AdsSplash Inter preload: Ad showed fullscreen content.");
                        interCallback.onAdShowedFullScreenContent();
                        if (!activity.isFinishing() && !activity.isDestroyed() && Admob.getInstance().loadingAdsDialog != null && Admob.getInstance().loadingAdsDialog.isShowing()) {
                            Admob.getInstance().dismissLoadingDialog();
                        }
                        Admob.getInstance().setInterOrRewardedShowing(true);
                        isFailToShowAdSplash = false;
                        removeHandlerSplashAds();
                    }
                });
                boolean isResumeState = activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
                Log.d(TAG, "AdsSplash Inter preload: ResumeState: " + isResumeState);
                if (isResumeState) {
                    Admob.getInstance().loadingAdsDialog = new LoadingAdsDialog(activity);
                    if (!activity.isFinishing() && !activity.isDestroyed() && !Admob.getInstance().loadingAdsDialog.isShowing()) {
                        Admob.getInstance().loadingAdsDialog.show();
                    }
                    Admob.getInstance().setInterOrRewardedShowing(true);
                    AppOpenManager.getInstance().setEnableResume(false);

                    if (Admob.getInstance().isOpenActivityAfterShowInterAds()) {
                        Log.d(TAG, "AdsSplash Inter preload: showInterAdsSplash: Admob.getInstance().isOpenActivityAfterShowInterAds() = true, onNextAction");
                        if (!AdmobAdsConfig.getInstance().isShowNativeAfterInter()) {
                            Log.d(TAG, "AdsSplash Inter preload: showInterAdsSplash: onNextAction init set off Native After Inter");
                            interCallback.onNextAction();
                        }
                    }
                    Log.d(TAG, "AdsSplash Inter preload: showInterAdsSplash: show Inter");
                    mInterstitialAdSplash.setImmersiveMode(true);
                    logEventSplash(activity, "step_inter_splash_call_show");
                    mInterstitialAdSplash.show(activity);
                } else {
                    Log.e(TAG, "AdsSplash Inter preload: Fail to show on background.");
                    logEventSplash(activity, "step_inter_splash_in_bg");
                    if (!activity.isFinishing() && !activity.isDestroyed() && Admob.getInstance().loadingAdsDialog != null && Admob.getInstance().loadingAdsDialog.isShowing()) {
                        Admob.getInstance().dismissLoadingDialog();
                    }
                    isFailToShowAdSplash = true;
                    if (runnable != null) {
                        handlerTimeoutSplash.removeCallbacks(runnable);
                    }
                }
            }, 50);
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("failed_message", "timeout_" + timeOutCallSplashAds + "_finish_" + activity.isFinishing() + "_destroyed_" + activity.isDestroyed());
            logEventSplash(activity, "splash_preload_start_show_failed", bundle);
        }
    }
    //End Inter Preload

    //================================ Inter Splash - Legacy load+show API ================================
    // showInterAdsSplashDelay / showInterAdsSplash / showInterAdsSplashAsync are three variants of the
    // same "build FullScreenContentCallback + show()" logic, differing only in whether they wait on the
    // preloading-delay gate, and whether the ad instance is passed in (Async) or read from field state.

    public void showInterAdsSplashDelay(AppCompatActivity activity, InterCallback interCallback, boolean isConfigShowNativeAfterInter, boolean isEmptyListNativeAfterInter) {
        countClickInterSplashAds = 0;
        activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                DefaultLifecycleObserver.super.onResume(owner);
                isSplashResume = true;
                Log.d(TAG, "SPLASH: onSplashResume: " + true);
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                DefaultLifecycleObserver.super.onStop(owner);
                isSplashResume = false;
                Log.d(TAG, "SPLASH: onSplashResume: " + false);
            }
        });
        if (mInterstitialAdSplash == null) {
            Log.d(TAG, "SPLASH: The interstitial ad wasn't ready yet.");
            AppOpenManager.getInstance().setEnableResume(true);
            logEventSplash(activity, "inter_splash_showad_false_ad_null");
            if (AdmobAdsConfig.getInstance().isShowNativeAfterInter()) {
                if (isConfigShowNativeAfterInter) {
                    if (isEmptyListNativeAfterInter) {
                        interCallback.onNextAction();
                    } else {
                        startNativeAfterInter(activity, interCallback);
                    }
                } else {
                    interCallback.onNextAction();
                }
            } else {
                interCallback.onNextAction();
            }
            return;
        }
        if (!isLoadInterSplashIdTimeout && !activity.isFinishing() && !activity.isDestroyed()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                mInterstitialAdSplash.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdClicked() {
                        AppOpenManager.isLastActionClickAd = true;
                        // Called when a click is recorded for an ad.
                        Log.d(TAG, "SPLASH: Ad was clicked.");
                        interCallback.onAdClicked();
                        countClickInterSplashAds++;
                        int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                        if (splashOpenTimes == 1) {
                            EventTrackingHelper.getInstance(activity).logEvent(EventTrackingHelper.inter_splash_click + "_" + countClickInterSplashAds);
                        }
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        logEventSplash(activity, "step_inter_splash_dismiss");
                        //increase splash open
                        SharePreferenceHelper.setInt(activity, EventTrackingHelper.splash_open, SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1);
                        //end increase splash open
                        Log.d(TAG, "SPLASH: Ad dismissed fullscreen content.");
                        interCallback.onAdDismissedFullScreenContent();
                        AppOpenManager.getInstance().setEnableResume(true);
                        if (!Admob.getInstance().isOpenActivityAfterShowInterAds()) {
                            if (AdmobAdsConfig.getInstance().isShowNativeAfterInter()) {
                                if (isConfigShowNativeAfterInter) {
                                    if (isEmptyListNativeAfterInter) {
                                        interCallback.onNextAction();
                                    } else {
                                        startNativeAfterInter(activity, interCallback);
                                    }
                                } else {
                                    interCallback.onNextAction();
                                }
                            } else {
                                interCallback.onNextAction();
                            }
                        }
                        Admob.getInstance().setInterOrRewardedShowing(false);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        logEventSplash(activity, "step_inter_splash_show_failded");
                        //increase splash open
                        SharePreferenceHelper.setInt(activity, EventTrackingHelper.splash_open, SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1);
                        //end increase splash open
                        Log.e(TAG, "SPLASH: Ad failed to show fullscreen content.");
                        interCallback.onAdFailedToShowFullScreenContent();
                        if (isSplashResume && !Admob.getInstance().isOpenActivityAfterShowInterAds()) {
                            if (AdmobAdsConfig.getInstance().isShowNativeAfterInter()) {
                                if (isConfigShowNativeAfterInter) {
                                    if (isEmptyListNativeAfterInter) {
                                        interCallback.onNextAction();
                                    } else {
                                        startNativeAfterInter(activity, interCallback);
                                    }
                                } else {
                                    interCallback.onNextAction();
                                }
                            } else {
                                interCallback.onNextAction();
                            }
                        }
                        if (!activity.isFinishing() && !activity.isDestroyed() && Admob.getInstance().loadingAdsDialog != null && Admob.getInstance().loadingAdsDialog.isShowing()) {
                            Admob.getInstance().dismissLoadingDialog();
                        }
                        isFailToShowAdSplash = true;
                        AppOpenManager.getInstance().setEnableResume(true);
                        Admob.getInstance().setInterOrRewardedShowing(false);
                        removeHandlerSplashAds();
                        //log event
                        Bundle bundle = new Bundle();
                        bundle.putString(EventTrackingHelper.showad_time, "false_" + (System.currentTimeMillis() - AdmobAdsConfig.getInstance().getTimeStartSplash()) / 1000);
                        bundle.putString("failed_message", "show_" + adError.getMessage());
                        EventTrackingHelper.getInstance(activity).logEventWithMultipleParams("inter_splash_showad_false_cb", bundle);
                        //end log event
                    }

                    @Override
                    public void onAdImpression() {
                        logEventSplash(activity, "step_inter_splash_impress");
                        // Called when an impression is recorded for an ad.
                        Log.d(TAG, "SPLASH: Ad recorded an impression.");
                        interCallback.onAdImpression();
                        //log event
                        EventTrackingHelper.getInstance(activity).logEventWithAParam(time_splash_loading_ad_show, time_splash_loading_show, String.valueOf((System.currentTimeMillis() - timeSplashLoadingAdShow) / 1000));
                        EventTrackingHelper.getInstance(activity).logEventWithAParam(EventTrackingHelper.inter_splash_showad_time, EventTrackingHelper.showad_time, "true_" + (System.currentTimeMillis() - AdmobAdsConfig.getInstance().getTimeStartSplash()) / 1000);
                        int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                        if (splashOpenTimes <= 3) {
                            EventTrackingHelper.getInstance(activity).logEvent(EventTrackingHelper.inter_splash_impression + "_" + splashOpenTimes);
                        }
                        //end log event
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        logEventSplash(activity, "step_inter_splash_show");
                        // Called when ad is shown.
                        Log.d(TAG, "SPLASH: Ad showed fullscreen content.");
                        mInterstitialAdSplash = null;
                        interCallback.onAdShowedFullScreenContent();
                        if (!activity.isFinishing() && !activity.isDestroyed() && Admob.getInstance().loadingAdsDialog != null && Admob.getInstance().loadingAdsDialog.isShowing()) {
                            Admob.getInstance().dismissLoadingDialog();
                        }
                        Admob.getInstance().setInterOrRewardedShowing(true);
                        isFailToShowAdSplash = false;
                        removeHandlerSplashAds();
                    }
                });
                boolean isResumeState = ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
                Log.d(TAG, "SPLASH: ResumeState: " + isResumeState);
                if (isResumeState) {
                    Admob.getInstance().loadingAdsDialog = new LoadingAdsDialog(activity);
                    if (!activity.isFinishing() && !activity.isDestroyed() && !Admob.getInstance().loadingAdsDialog.isShowing()) {
                        Admob.getInstance().loadingAdsDialog.show();
                    }
                    Admob.getInstance().setInterOrRewardedShowing(true);
                    AppOpenManager.getInstance().setEnableResume(false);
                    if (Admob.getInstance().isOpenActivityAfterShowInterAds()) {
                        Log.d(TAG, "SPLASH: showInterAdsSplash: Admob.getInstance().isOpenActivityAfterShowInterAds() = true, onNextAction");
                        if (AdmobAdsConfig.getInstance().isShowNativeAfterInter()) {
                            if (isConfigShowNativeAfterInter) {
                                if (isEmptyListNativeAfterInter) {
                                    Log.d(TAG, "SPLASH: showInterAdsSplash: isEmptyListNativeAfterInter = " + isEmptyListNativeAfterInter);
                                    interCallback.onNextAction();
                                } else {
                                    Log.d(TAG, "SPLASH: showInterAdsSplash: show Native After Inter");
                                    startNativeAfterInter(activity, interCallback);
                                }
                            } else {
                                Log.d(TAG, "SPLASH: showInterAdsSplash: onNextAction");
                                interCallback.onNextAction();
                            }
                        } else {
                            Log.d(TAG, "SPLASH: showInterAdsSplash: onNextAction init set off Native After Inter");
                            interCallback.onNextAction();
                        }
                    }
                    logEventSplash(activity, "step_inter_splash_call_show");
                    Log.d(TAG, "SPLASH: showInterAdsSplash: show Inter");
                    mInterstitialAdSplash.setImmersiveMode(true);
                    mInterstitialAdSplash.show(activity);
                } else {
                    logEventSplash(activity, "step_inter_splash_in_bg");
                    EventTrackingHelper.getInstance(activity).logEvent("inter_splash_showad_false_app_pause");
                    Log.e(TAG, "SPLASH: Fail to show on background.");
                    if (!activity.isFinishing() && !activity.isDestroyed() && Admob.getInstance().loadingAdsDialog != null && Admob.getInstance().loadingAdsDialog.isShowing()) {
                        Admob.getInstance().dismissLoadingDialog();
                    }
                    isFailToShowAdSplash = true;
                    if (runnable != null) {
                        handlerTimeoutSplash.removeCallbacks(runnable);
                    }
                }
            }, 250);
        }
    }


    public void showInterAdsSplash(AppCompatActivity activity, InterCallback interCallback) {
        countClickInterSplashAds = 0;
        activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                DefaultLifecycleObserver.super.onResume(owner);
                isSplashResume = true;
                Log.d(TAG, "SPLASH: onSplashResume: " + true);
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                DefaultLifecycleObserver.super.onStop(owner);
                isSplashResume = false;
                Log.d(TAG, "SPLASH: onSplashResume: " + false);
            }
        });
        if (mInterstitialAdSplash == null) {
            logEventSplash(activity, "splash_normal_failed_inter_null");
            Log.d(TAG, "SPLASH: The interstitial ad wasn't ready yet.");
            AppOpenManager.getInstance().setEnableResume(true);
            interCallback.onNextAction();
            return;
        }
        if (!isLoadInterSplashIdTimeout && !activity.isFinishing() && !activity.isDestroyed()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                mInterstitialAdSplash.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdClicked() {
                        AppOpenManager.isLastActionClickAd = true;
                        // Called when a click is recorded for an ad.
                        Log.d(TAG, "SPLASH: Ad was clicked.");
                        interCallback.onAdClicked();
                        countClickInterSplashAds++;
                        int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                        if (splashOpenTimes == 1) {
                            EventTrackingHelper.getInstance(activity).logEvent(EventTrackingHelper.inter_splash_click + "_" + countClickInterSplashAds);
                        }
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        logEventSplash(activity, "step_inter_splash_dismiss");
                        //increase splash open
                        SharePreferenceHelper.setInt(activity, EventTrackingHelper.splash_open, SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1);
                        //end increase splash open
                        Log.d(TAG, "SPLASH: Ad dismissed fullscreen content.");
                        interCallback.onAdDismissedFullScreenContent();
                        AppOpenManager.getInstance().setEnableResume(true);
                        if (!Admob.getInstance().isOpenActivityAfterShowInterAds()) {
                            interCallback.onNextAction();
                        }
                        Admob.getInstance().setInterOrRewardedShowing(false);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        logEventSplash(activity, "step_inter_splash_show_failded");
                        //increase splash open
                        SharePreferenceHelper.setInt(activity, EventTrackingHelper.splash_open, SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1);
                        //end increase splash open
                        Log.e(TAG, "SPLASH: Ad failed to show fullscreen content.");
                        interCallback.onAdFailedToShowFullScreenContent();
                        if (isSplashResume && !Admob.getInstance().isOpenActivityAfterShowInterAds()) {
                            interCallback.onNextAction();
                        }
                        if (!activity.isFinishing() && !activity.isDestroyed() && Admob.getInstance().loadingAdsDialog != null && Admob.getInstance().loadingAdsDialog.isShowing()) {
                            Admob.getInstance().dismissLoadingDialog();
                        }
                        isFailToShowAdSplash = true;
                        AppOpenManager.getInstance().setEnableResume(true);
                        Admob.getInstance().setInterOrRewardedShowing(false);
                        removeHandlerSplashAds();
                        //log event
                        EventTrackingHelper.getInstance(activity).logEventWithAParam(EventTrackingHelper.inter_splash_showad_time, EventTrackingHelper.showad_time, "false_" + (System.currentTimeMillis() - AdmobAdsConfig.getInstance().getTimeStartSplash()) / 1000);
                        //end log event
                    }

                    @Override
                    public void onAdImpression() {
                        logEventSplash(activity, "step_inter_splash_impress");
                        // Called when an impression is recorded for an ad.
                        Log.d(TAG, "SPLASH: Ad recorded an impression.");
                        interCallback.onAdImpression();
                        //log event
                        EventTrackingHelper.getInstance(activity).logEventWithAParam(time_splash_loading_ad_show, time_splash_loading_show, String.valueOf((System.currentTimeMillis() - timeSplashLoadingAdShow) / 1000));
                        EventTrackingHelper.getInstance(activity).logEventWithAParam(EventTrackingHelper.inter_splash_showad_time, EventTrackingHelper.showad_time, "true_" + (System.currentTimeMillis() - AdmobAdsConfig.getInstance().getTimeStartSplash()) / 1000);
                        int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                        if (splashOpenTimes <= 3) {
                            EventTrackingHelper.getInstance(activity).logEvent(EventTrackingHelper.inter_splash_impression + "_" + splashOpenTimes);
                        }
                        //end log event
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        logEventSplash(activity, "step_inter_splash_show");
                        // Called when ad is shown.
                        Log.d(TAG, "SPLASH: Ad showed fullscreen content.");
                        mInterstitialAdSplash = null;
                        interCallback.onAdShowedFullScreenContent();
                        if (!activity.isFinishing() && !activity.isDestroyed() && Admob.getInstance().loadingAdsDialog != null && Admob.getInstance().loadingAdsDialog.isShowing()) {
                            Admob.getInstance().dismissLoadingDialog();
                        }
                        Admob.getInstance().setInterOrRewardedShowing(true);
                        isFailToShowAdSplash = false;
                        removeHandlerSplashAds();
                    }
                });
                boolean isResumeState = ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
                Log.d(TAG, "SPLASH: ResumeState: " + isResumeState);
                if (isResumeState) {
                    Admob.getInstance().loadingAdsDialog = new LoadingAdsDialog(activity);
                    if (!activity.isFinishing() && !activity.isDestroyed() && !Admob.getInstance().loadingAdsDialog.isShowing()) {
                        Admob.getInstance().loadingAdsDialog.show();
                    }
                    Admob.getInstance().setInterOrRewardedShowing(true);
                    AppOpenManager.getInstance().setEnableResume(false);
                    if (Admob.getInstance().isOpenActivityAfterShowInterAds()) {
                        Log.d(TAG, "SPLASH: showInterAdsSplash: Admob.getInstance().isOpenActivityAfterShowInterAds() = true, onNextAction");
                        interCallback.onNextAction();
                    }
                    logEventSplash(activity, "step_inter_splash_call_show");
                    mInterstitialAdSplash.setImmersiveMode(true);
                    mInterstitialAdSplash.show(activity);
                } else {
                    Log.e(TAG, "SPLASH: Fail to show on background.");
                    if (!activity.isFinishing() && !activity.isDestroyed() && Admob.getInstance().loadingAdsDialog != null && Admob.getInstance().loadingAdsDialog.isShowing()) {
                        Admob.getInstance().dismissLoadingDialog();
                    }
                    logEventSplash(activity, "step_inter_splash_in_bg");
                    isFailToShowAdSplash = true;
                    if (runnable != null) {
                        handlerTimeoutSplash.removeCallbacks(runnable);
                    }
                }
            }, 250);
        }
    }


    public void showInterAdsSplashAsync(InterstitialAd interSplash, AppCompatActivity activity, InterCallback interCallback) {
        countClickInterSplashAds = 0;
        activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                DefaultLifecycleObserver.super.onResume(owner);
                isSplashResume = true;
                Log.d(TAG, "SPLASH: onSplashResume: " + true);
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                DefaultLifecycleObserver.super.onStop(owner);
                isSplashResume = false;
                Log.d(TAG, "SPLASH: onSplashResume: " + false);
            }
        });
        if (interSplash == null) {
            Log.d(TAG, "SPLASH: The interstitial ad wasn't ready yet.");
            AppOpenManager.getInstance().setEnableResume(true);
            interCallback.onNextAction();
            return;
        }
        if (!isLoadInterSplashIdTimeout && !activity.isFinishing() && !activity.isDestroyed()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                interSplash.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdClicked() {
                        AppOpenManager.isLastActionClickAd = true;
                        // Called when a click is recorded for an ad.
                        Log.d(TAG, "SPLASH: Ad was clicked.");
                        interCallback.onAdClicked();
                        countClickInterSplashAds++;
                        int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                        if (splashOpenTimes == 1) {
                            EventTrackingHelper.getInstance(activity).logEvent(EventTrackingHelper.inter_splash_click + "_" + countClickInterSplashAds);
                        }
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        //increase splash open
                        SharePreferenceHelper.setInt(activity, EventTrackingHelper.splash_open, SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1);
                        //end increase splash open
                        Log.d(TAG, "SPLASH: Ad dismissed fullscreen content.");
                        interCallback.onAdDismissedFullScreenContent();
                        AppOpenManager.getInstance().setEnableResume(true);
                        if (!Admob.getInstance().isOpenActivityAfterShowInterAds()) {
                            interCallback.onNextAction();
                        }
                        Admob.getInstance().setInterOrRewardedShowing(false);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        //increase splash open
                        SharePreferenceHelper.setInt(activity, EventTrackingHelper.splash_open, SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1);
                        //end increase splash open
                        Log.e(TAG, "SPLASH: Ad failed to show fullscreen content.");
                        interCallback.onAdFailedToShowFullScreenContent();
                        if (isSplashResume && !Admob.getInstance().isOpenActivityAfterShowInterAds()) {
                            interCallback.onNextAction();
                        }
                        if (!activity.isFinishing() && !activity.isDestroyed() && Admob.getInstance().loadingAdsDialog != null && Admob.getInstance().loadingAdsDialog.isShowing()) {
                            Admob.getInstance().dismissLoadingDialog();
                        }
                        isFailToShowAdSplash = true;
                        AppOpenManager.getInstance().setEnableResume(true);
                        Admob.getInstance().setInterOrRewardedShowing(false);
                        removeHandlerSplashAds();
                        //log event
                        EventTrackingHelper.getInstance(activity).logEventWithAParam(EventTrackingHelper.inter_splash_showad_time, EventTrackingHelper.showad_time, "false_" + (System.currentTimeMillis() - AdmobAdsConfig.getInstance().getTimeStartSplash()) / 1000);
                        //end log event
                    }

                    @Override
                    public void onAdImpression() {
                        logEventSplash(activity, "step_inter_splash_impress");
                        // Called when an impression is recorded for an ad.
                        Log.d(TAG, "SPLASH: Ad recorded an impression.");
                        interCallback.onAdImpression();
                        //log event
                        EventTrackingHelper.getInstance(activity).logEventWithAParam(time_splash_loading_ad_show, time_splash_loading_show, String.valueOf((System.currentTimeMillis() - timeSplashLoadingAdShow) / 1000));
                        EventTrackingHelper.getInstance(activity).logEventWithAParam(EventTrackingHelper.inter_splash_showad_time, EventTrackingHelper.showad_time, "true_" + (System.currentTimeMillis() - AdmobAdsConfig.getInstance().getTimeStartSplash()) / 1000);
                        int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                        if (splashOpenTimes <= 3) {
                            EventTrackingHelper.getInstance(activity).logEvent(EventTrackingHelper.inter_splash_impression + "_" + splashOpenTimes);
                        }
                        //end log event
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        logEventSplash(activity, "step_inter_splash_show");
                        // Called when ad is shown.
                        Log.d(TAG, "SPLASH: Ad showed fullscreen content.");
                        interCallback.onAdShowedFullScreenContent();
                        if (!activity.isFinishing() && !activity.isDestroyed() && Admob.getInstance().loadingAdsDialog != null && Admob.getInstance().loadingAdsDialog.isShowing()) {
                            Admob.getInstance().dismissLoadingDialog();
                        }
                        Admob.getInstance().setInterOrRewardedShowing(true);
                        isFailToShowAdSplash = false;
                        removeHandlerSplashAds();
                    }
                });
                boolean isResumeState = ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
                Log.d(TAG, "SPLASH: ResumeState: " + isResumeState);
                if (isResumeState) {
                    Admob.getInstance().loadingAdsDialog = new LoadingAdsDialog(activity);
                    if (!activity.isFinishing() && !activity.isDestroyed() && !Admob.getInstance().loadingAdsDialog.isShowing()) {
                        Admob.getInstance().loadingAdsDialog.show();
                    }
                    Admob.getInstance().setInterOrRewardedShowing(true);
                    AppOpenManager.getInstance().setEnableResume(false);
                    if (Admob.getInstance().isOpenActivityAfterShowInterAds()) {
                        Log.d(TAG, "SPLASH: showInterAdsSplash: Admob.getInstance().isOpenActivityAfterShowInterAds() = true, onNextAction");
                        interCallback.onNextAction();
                    }
                    interSplash.setImmersiveMode(true);
                    interSplash.show(activity);
                } else {
                    Log.e(TAG, "SPLASH: Fail to show on background.");
                    if (!activity.isFinishing() && !activity.isDestroyed() && Admob.getInstance().loadingAdsDialog != null && Admob.getInstance().loadingAdsDialog.isShowing()) {
                        Admob.getInstance().dismissLoadingDialog();
                    }
                    isFailToShowAdSplash = true;
                    if (runnable != null) {
                        handlerTimeoutSplash.removeCallbacks(runnable);
                    }
                }
            }, 250);
        }
    }

    //================================ Inter Splash - High/Normal priority async race ================================
    // Loads a "high" placement and a "normal" placement together; whichever becomes available first
    // (with a small grace delay favoring the high placement) is the one that gets shown.

    public void loadAndShowIdInterAdSplashAsync(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback) {
        Long currentTime = System.currentTimeMillis();
        loadAndShowIdInterAdSplashAsync(activity, listIdInter, interCallback, currentTime, currentTime);
    }

    public void loadAndShowIdInterAdSplashAsync(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback,
                                                Long timeStep1, Long timeLastStep) {
        timeStartCalInterSplash = timeStep1;
        timeLastCalInterSplash = timeLastStep;
        ArrayList<String> listIdInterTemp = new ArrayList<>(listIdInter);
        //Set timeout ads splash x(s) if cannot load
//        runnable = () -> {
//            EventTrackingHelper.getInstance(activity).logEvent(EventTrackingHelper.inter_splash_id_timeout);
//            if (!activity.isFinishing() && !activity.isDestroyed() && Admob.getInstance().loadingAdsDialog != null && Admob.getInstance().loadingAdsDialog.isShowing()) {
//                Admob.getInstance().dismissLoadingDialog();
//            }
//            if (interCallback != null) {
//                isLoadInterSplashIdTimeout = true;
//                interCallback.onNextAction();
//            }
//            removeHandlerSplashAds();
//        };
//        handlerTimeoutSplash.postDelayed(runnable, timeOutCallSplashAds);

        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds() /*|| IAPManager.getInstance().isPurchase()*/) {
            Log.d(TAG, "Check condition loadAndShowIdInterAdSplashAsync " + NetworkUtil.isNetworkActive(activity) + "_" + listIdInterTemp.isEmpty() + "_" + AdsConsentManager.getConsentResult(activity) + "_" + Admob.getInstance().getShowAllAds() + "_" /*+ IAPManager.getInstance().isPurchase()*/);

            Bundle bundle = new Bundle();
            bundle.putString("failed_message", "lib_internet_" + NetworkUtil.isNetworkActive(activity)
                    + "_Consent_" + AdsConsentManager.getConsentResult(activity)
                    + "_isShowAllAds_" + Admob.getInstance().getShowAllAds()
            );
            logEventSplash(activity, "splash_asyn_condition_failed", bundle);
            interCallback.onNextAction();
            removeHandlerSplashAds();
            return;
        }

        //Log event
        Bundle bundle = new Bundle();
        boolean idCheck = AdmobApi.getInstance().getListAdsSize() > 0;
        bundle.putString(EventTrackingHelper.splash_detail, AdsConsentManager.getConsentResult(activity) + "_" + TechManager.getInstance().isTech(activity) + "_" + NetworkUtil.isNetworkActive(activity) + "_" + Admob.getInstance().getShowAllAds() + "_" + idCheck + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        bundle.putString(EventTrackingHelper.ump, String.valueOf(AdsConsentManager.getConsentResult(activity)));
        bundle.putString(EventTrackingHelper.organic, String.valueOf(TechManager.getInstance().isTech(activity)));
        bundle.putString(EventTrackingHelper.haveinternet, String.valueOf(NetworkUtil.isNetworkActive(activity)));
        bundle.putString(EventTrackingHelper.showallad, String.valueOf(Admob.getInstance().getShowAllAds()));
        bundle.putString(EventTrackingHelper.idcheck, String.valueOf(idCheck));
        bundle.putString(EventTrackingHelper.interremote + "_" + EventTrackingHelper.openremote + "_" + EventTrackingHelper.aoavalue, RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.inter_splash) + "_" + RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.open_splash) + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        EventTrackingHelper.getInstance(activity).logEventWithMultipleParams(EventTrackingHelper.inter_splash_tracking, bundle);

        //log event can request
        EventTrackingHelper.getInstance(activity).logEvent(EventTrackingHelper.inter_splash_true);
        //end log event can request
        //time start load splash ads
        timeSplashLoadingAdShow = System.currentTimeMillis();

        AdRequest adRequest = new AdRequest.Builder().build();
        for (int i = 0; i < listIdInterTemp.size(); i++) {
            int index = i;
            Log.d(TAG, "SPLASH ID ASYNC: Start load inter splash. " + listIdInterTemp.get(index));
            InterstitialAd.load(activity, listIdInterTemp.get(index), adRequest,
                    new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                            //Tracking revenue
                            interstitialAd.setOnPaidEventListener(adValue -> {
                                //Adjust
                                AdjustUtil.trackRevenue(interstitialAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdInterTemp.get(0), "inter_splash");
                            });
                            Log.i(TAG, "SPLASH ID ASYNC: Ad was loaded inter splash. " + listIdInterTemp.get(index));
                            interCallback.onAdLoaded(interstitialAd);
                            if (index == 0) {
                                mInterstitialAdSplashHigh = interstitialAd;
                                if (!isShownInterSplashNormal) {
                                    isShownInterSplashHigh = true;
                                    showInterAdsSplashAsync(mInterstitialAdSplashHigh, activity, interCallback);
                                    Log.d(TAG, "SPLASH ID ASYNC: Show inter splash high. " + listIdInterTemp.get(index));
                                }
                            } else {
                                mInterstitialAdSplash = interstitialAd;
                                new Handler().postDelayed(() -> {
                                    if (!isShownInterSplashHigh && !isShownInterSplashNormal) {
                                        showInterAdsSplashAsync(mInterstitialAdSplash, activity, interCallback);
                                        isShownInterSplashNormal = true;
                                        Log.d(TAG, "SPLASH ID ASYNC: Show inter splash. " + listIdInterTemp.get(index));
                                    }
                                }, timeDelayWaitInterHigh);
                            }
                            removeHandlerSplashAds();
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            // Handle the error
                            Log.e(TAG, "SPLASH ID ASYNC: Fail to load inter splash. " + loadAdError);
                            Bundle bundleE = new Bundle();
                            bundleE.putString("failed_message", "load_" + loadAdError.getMessage());
                            logEventSplash(activity, "splash_asyn_load_failed", bundleE);
                            if (!isLoadInterSplashIdTimeout) {
                                if (listIdInterTemp.size() <= 1) {
                                    interCallback.onAdFailedToLoad();
                                    interCallback.onNextAction();
                                } else {
                                    if (index != 0 && !isHandledLoadAdsSplashFail) {
                                        interCallback.onAdFailedToLoad();
                                        interCallback.onNextAction();
                                        isHandledLoadAdsSplashFail = true;
                                    }
                                }
                            }
                        }
                    });
        }
    }

    //================================ Inter Splash - Simple load+show, single ID at a time ================================

    public void loadAndShowInterAdSplash(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback) {
        ArrayList<String> listIdInterTemp = new ArrayList<>(listIdInter);
        //Set timeout ads splash x(s) if cannot load
//        runnable = () -> {
//            EventTrackingHelper.getInstance(activity).logEvent(EventTrackingHelper.inter_splash_id_timeout);
//            if (!activity.isFinishing() && !activity.isDestroyed() && Admob.getInstance().loadingAdsDialog != null && Admob.getInstance().loadingAdsDialog.isShowing()) {
//                Admob.getInstance().dismissLoadingDialog();
//            }
//            if (interCallback != null) {
//                isLoadInterSplashIdTimeout = true;
//                interCallback.onNextAction();
//            }
//            removeHandlerSplashAds();
//        };
//        handlerTimeoutSplash.postDelayed(runnable, timeOutCallSplashAds);

        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds() /*|| IAPManager.getInstance().isPurchase()*/) {
            Log.d(TAG, "Check condition loadAndShowInterAdSplash " + NetworkUtil.isNetworkActive(activity) + "_" + listIdInterTemp.isEmpty() + "_" + AdsConsentManager.getConsentResult(activity) + "_" + Admob.getInstance().getShowAllAds() + "_" /*+ IAPManager.getInstance().isPurchase()*/);
            interCallback.onNextAction();
            removeHandlerSplashAds();
            return;
        }

        //Log event
        Bundle bundle = new Bundle();
        boolean idCheck = AdmobApi.getInstance().getListAdsSize() > 0;
        bundle.putString(EventTrackingHelper.splash_detail, AdsConsentManager.getConsentResult(activity) + "_" + TechManager.getInstance().isTech(activity) + "_" + NetworkUtil.isNetworkActive(activity) + "_" + Admob.getInstance().getShowAllAds() + "_" + idCheck + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        bundle.putString(EventTrackingHelper.ump, String.valueOf(AdsConsentManager.getConsentResult(activity)));
        bundle.putString(EventTrackingHelper.organic, String.valueOf(TechManager.getInstance().isTech(activity)));
        bundle.putString(EventTrackingHelper.haveinternet, String.valueOf(NetworkUtil.isNetworkActive(activity)));
        bundle.putString(EventTrackingHelper.showallad, String.valueOf(Admob.getInstance().getShowAllAds()));
        bundle.putString(EventTrackingHelper.idcheck, String.valueOf(idCheck));
        bundle.putString(EventTrackingHelper.interremote + "_" + EventTrackingHelper.openremote + "_" + EventTrackingHelper.aoavalue, RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.inter_splash) + "_" + RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.open_splash) + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        EventTrackingHelper.getInstance(activity).logEventWithMultipleParams(EventTrackingHelper.inter_splash_tracking, bundle);

        //log event can request
        EventTrackingHelper.getInstance(activity).logEvent(EventTrackingHelper.inter_splash_true);
        //end log event can request
        //time start load splash ads
        timeSplashLoadingAdShow = System.currentTimeMillis();

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(activity, listIdInterTemp.get(0), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        //Tracking revenue
                        interstitialAd.setOnPaidEventListener(adValue -> {
                            //Adjust
                            AdjustUtil.trackRevenue(interstitialAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdInterTemp.get(0), "inter_splash");
                        });
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        Log.i(TAG, "SPLASH: Ad was loaded inter splash.");
                        interCallback.onAdLoaded(interstitialAd);
                        mInterstitialAdSplash = interstitialAd;
                        showInterAdsSplash(activity, interCallback);
                        removeHandlerSplashAds();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.e(TAG, "SPLASH: Fail to load inter splash. " + loadAdError);
                        interCallback.onAdFailedToLoad();
                        if (!listIdInterTemp.isEmpty()) {
                            listIdInterTemp.remove(0);
                        }
                        loadAndShowInterAdSplash(activity, listIdInterTemp, interCallback);
                    }
                });
    }

    public void loadAndShowInterAdSplashDelay(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback, String adsKeyNative, String remoteKeyNative) {
        Long currentTime = System.currentTimeMillis();
        loadAndShowInterAdSplashDelay(activity, listIdInter, interCallback, adsKeyNative, remoteKeyNative, currentTime, currentTime);
    }

    public void loadAndShowInterAdSplashDelay(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback,
                                              String adsKeyNative, String remoteKeyNative,
                                              Long timeStep1, Long timeLastStep) {
        timeStartCalInterSplash = timeStep1;
        timeLastCalInterSplash = timeLastStep;
        Log.d(TAG, "Bắt đầu tiến trình Load And Show Inter Delay ads...");
        NativeAfterInterManager.preloadNativeAfterInter(activity, adsKeyNative, remoteKeyNative);

        boolean isConfigShowNativeAfterInter = RemoteConfigHelper.getInstance().get_config(activity, remoteKeyNative);

        boolean isEmptyListNativeAfterInter = AdmobApi.getInstance().getListIDByName(adsKeyNative).isEmpty();

        ArrayList<String> listIdInterTemp = new ArrayList<>(listIdInter);
        //Set timeout ads splash x(s) if cannot load
//        runnable = () -> {
//            EventTrackingHelper.getInstance(activity).logEvent(EventTrackingHelper.inter_splash_id_timeout);
//            if (!activity.isFinishing() && !activity.isDestroyed() && Admob.getInstance().loadingAdsDialog != null && Admob.getInstance().loadingAdsDialog.isShowing()) {
//                Admob.getInstance().dismissLoadingDialog();
//            }
//            if (interCallback != null) {
//                isLoadInterSplashIdTimeout = true;
//                if (AdmobAdsConfig.getInstance().isShowNativeAfterInter()) {
//                    if (isConfigShowNativeAfterInter) {
//                        if (isEmptyListNativeAfterInter) {
//                            interCallback.onNextAction();
//                        } else {
//                            startNativeAfterInter(activity, interCallback);
//                        }
//                    } else {
//                        interCallback.onNextAction();
//                    }
//                } else {
//                    interCallback.onNextAction();
//                }
//            }
//            removeHandlerSplashAds();
//        };
//        handlerTimeoutSplash.postDelayed(runnable, timeOutCallSplashAds);

        //delay ads splash
//        if (AdmobAdsConfig.getInstance().getUseNativeSplash()) {
//            timerDelayRunnable = new Runnable() {
//                @Override
//                public void run() {
//                    Log.d(TAG, "Đã đủ 7 giây đếm ngược.");
//                    isTimerDelayFinished = true;
//                    checkConditionAdsSplash(activity, interCallback, isConfigShowNativeAfterInter, isEmptyListNativeAfterInter);
//                }
//            };
//        } else {
//            Log.d(TAG, "Không dùng chờ 7 giây đếm ngược.");
        isTimerDelayFinished = true;
//        }
//        handlerDelayAdsSplash.postDelayed(timerDelayRunnable, timeDelayAdsSplash);
        //end

        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds() /*|| IAPManager.getInstance().isPurchase()*/) {
            Log.d(TAG, "Check condition loadAndShowInterAdSplash " + NetworkUtil.isNetworkActive(activity) + "_" + listIdInterTemp.isEmpty() + "_" + AdsConsentManager.getConsentResult(activity) + "_" + Admob.getInstance().getShowAllAds() + "_" /*+ IAPManager.getInstance().isPurchase()*/);
            interCallback.onNextAction();
            removeHandlerSplashAds();
            Bundle bundle = new Bundle();
            bundle.putString("failed_message", "lib_internet_" + NetworkUtil.isNetworkActive(activity)
                    + "_Consent_" + AdsConsentManager.getConsentResult(activity)
                    + "_isShowAllAds_" + Admob.getInstance().getShowAllAds()
            );
            logEventSplash(activity, "splash_delay_condition_failed", bundle);
            return;
        }

        //Log event
        Bundle bundle = new Bundle();
        boolean idCheck = AdmobApi.getInstance().getListAdsSize() > 0;
        bundle.putString(EventTrackingHelper.splash_detail, AdsConsentManager.getConsentResult(activity) + "_" + TechManager.getInstance().isTech(activity) + "_" + NetworkUtil.isNetworkActive(activity) + "_" + Admob.getInstance().getShowAllAds() + "_" + idCheck + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        bundle.putString(EventTrackingHelper.ump, String.valueOf(AdsConsentManager.getConsentResult(activity)));
        bundle.putString(EventTrackingHelper.organic, String.valueOf(TechManager.getInstance().isTech(activity)));
        bundle.putString(EventTrackingHelper.haveinternet, String.valueOf(NetworkUtil.isNetworkActive(activity)));
        bundle.putString(EventTrackingHelper.showallad, String.valueOf(Admob.getInstance().getShowAllAds()));
        bundle.putString(EventTrackingHelper.idcheck, String.valueOf(idCheck));
        bundle.putString(EventTrackingHelper.interremote + "_" + EventTrackingHelper.openremote + "_" + EventTrackingHelper.aoavalue, RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.inter_splash) + "_" + RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.open_splash) + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        EventTrackingHelper.getInstance(activity).logEventWithMultipleParams(EventTrackingHelper.inter_splash_tracking, bundle);

        //log event can request
        EventTrackingHelper.getInstance(activity).logEvent(EventTrackingHelper.inter_splash_true);
        //end log event can request
        //time start load splash ads
        timeSplashLoadingAdShow = System.currentTimeMillis();

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(activity, listIdInterTemp.get(0), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        logEventSplash(activity, "splash_delay_loaded");
                        //Tracking revenue
                        interstitialAd.setOnPaidEventListener(adValue -> {
                            //Adjust
                            AdjustUtil.trackRevenue(interstitialAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdInterTemp.get(0), "inter_splash");
                        });
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        Log.i(TAG, "SPLASH: Ad was loaded inter splash.");
                        EventTrackingHelper.getInstance(activity).logEvent("splash_delay_true");
                        interCallback.onAdLoaded(interstitialAd);
                        mInterstitialAdSplash = interstitialAd;
                        isAdLoadAdsSplashFinished = true;
                        checkConditionAdsSplash(activity, interCallback, isConfigShowNativeAfterInter, isEmptyListNativeAfterInter);
                        removeHandlerSplashAds();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.e(TAG, "SPLASH: Fail to load inter splash. " + loadAdError);
                        Bundle bundle = new Bundle();
                        bundle.putString("failed_message", "load_" + loadAdError.getMessage());
                        logEventSplash(activity, "splash_delay_load_failed", bundle);
                        interCallback.onAdFailedToLoad();
                        if (listIdInterTemp.size() > 1) {
                            listIdInterTemp.remove(0);
                            loadAndShowInterAdSplash(activity, listIdInterTemp, interCallback);
                        }
                    }
                });
    }

    private void checkConditionAdsSplash(AppCompatActivity activity, InterCallback interCallback, boolean isConfigShowNativeAfterInter, boolean isEmptyListNativeAfterInter) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.d(TAG, "removeHandlerDelayAdsSplash");
            EventTrackingHelper.getInstance(activity).logEvent("inter_splash_showad_false_acti_null");
            Bundle bundle = new Bundle();
            bundle.putString("failed_message", "isTimerDelayFinished_" + isTimerDelayFinished
                    + "_isAdLoadAdsSplashFinished_" + isAdLoadAdsSplashFinished
            );
            logEventSplash(activity, "check_condition_ads_splash_failed", bundle);
            removeHandlerDelayAdsSplash();
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString("message", "isTimerDelayFinished_" + isTimerDelayFinished
                + "_isAdLoadAdsSplashFinished_" + isAdLoadAdsSplashFinished
        );
        EventTrackingHelper.getInstance(activity).logEventWithMultipleParams("check_condition_ads_splash", bundle);

        if (isTimerDelayFinished && isAdLoadAdsSplashFinished) {
            String timeFormatted = String.format(Locale.US, "%.1f", (System.currentTimeMillis() - timeStartCalInterSplash) / 1000.0);
            Log.d(TAG, "===> TỔNG THỜI GIAN CHỜ: " + timeFormatted + " giây");
            EventTrackingHelper.getInstance(activity).logEventWithAParam("Splash_time_wait", "time_to_step", timeFormatted);
            showInterAdsSplashDelay(activity, interCallback, isConfigShowNativeAfterInter, isEmptyListNativeAfterInter);
            removeHandlerDelayAdsSplash();
        }

    }

    public void removeHandlerDelayAdsSplash() {
//        if (handlerDelayAdsSplash != null && timerDelayRunnable != null) {
//            handlerDelayAdsSplash.removeCallbacks(timerDelayRunnable);
//            handlerDelayAdsSplash.removeCallbacksAndMessages(null);
//        }
    }

    //================================ Inter Splash - Waterfall loop across a list of ad unit IDs ================================

    public void loadAndShowInterAdSplashLoop(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback) {
        Long currentTime = System.currentTimeMillis();
        loadAndShowInterAdSplashLoop(activity, listIdInter, interCallback, currentTime, currentTime);
    }

    public void loadAndShowInterAdSplashLoop(AppCompatActivity activity,
                                             List<String> listIdInter,
                                             InterCallback interCallback,
                                             Long timeStep1, Long timeLastStep) {
        Log.d(TAG, "SPLASH: loadAndShowInterAdSplashLoop. " + listIdInter.toString());
        timeStartCalInterSplash = timeStep1;
        timeLastCalInterSplash = timeLastStep;
        // Check list id size
        if (listIdInter.isEmpty()) {
            logEventSplash(activity, "splash_loop_id_empty");
            Log.d(TAG, "SPLASH: loadAndShowInterAdSplashLoop: listIdInter is empty.");
            interCallback.onNextAction();
            removeHandlerSplashAds();
            return;
        }
        String idInterSplash = listIdInter.get(0);

        // If have action startActivity by timeout or no internet in splash, do not load ads.
//        if (System.currentTimeMillis() - Admob.getInstance().getTimeStart() >= 8000 || AdmobAdsConfig.getInstance().isTimeout() || AdmobAdsConfig.getInstance().getNoInternetAction()) {
//            Bundle bundle = new Bundle();
//            bundle.putString("failed_message", "time_out_lib");
//            EventTrackingHelper.getInstance(activity).logEventWithMultipleParams("splash_loop_failed", bundle);
//            Log.d(TAG, "SPLASH: If have action startActivity by timeout or no internet in splash, do not load ads. " + (System.currentTimeMillis() - Admob.getInstance().getTimeStart() >= 8000) + "_" + AdmobAdsConfig.getInstance().isTimeout() + "_" + AdmobAdsConfig.getInstance().getNoInternetAction());
//            EventTrackingHelper.getInstance(activity).logEvent(EventTrackingHelper.inter_splash_id_timeout_8s);
//            interCallback.onNextAction();
//            removeHandlerSplashAds();
//            return;
//        }

        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || idInterSplash.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds() /*|| IAPManager.getInstance().isPurchase()*/) {
            Log.d(TAG, "SPLASH: Check condition loadAndShowInterAdSplash. Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + idInterSplash.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + Admob.getInstance().getShowAllAds() + "_IAP:" /*+ IAPManager.getInstance().isPurchase()*/);
            Bundle bundle = new Bundle();
            bundle.putString("failed_message", "lib_internet_" + NetworkUtil.isNetworkActive(activity)
                    + "_Consent_" + AdsConsentManager.getConsentResult(activity)
                    + "_isShowAllAds_" + Admob.getInstance().getShowAllAds()
            );
            logEventSplash(activity, "splash_loop_condition_failed", bundle);
            interCallback.onNextAction();
            removeHandlerSplashAds();
            return;
        }

        //Log event
        Bundle bundle = new Bundle();
        boolean idCheck = AdmobApi.getInstance().getListAdsSize() > 0;
        bundle.putString(EventTrackingHelper.splash_detail, AdsConsentManager.getConsentResult(activity) + "_" + TechManager.getInstance().isTech(activity) + "_" + NetworkUtil.isNetworkActive(activity) + "_" + Admob.getInstance().getShowAllAds() + "_" + idCheck + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        bundle.putString(EventTrackingHelper.ump, String.valueOf(AdsConsentManager.getConsentResult(activity)));
        bundle.putString(EventTrackingHelper.organic, String.valueOf(TechManager.getInstance().isTech(activity)));
        bundle.putString(EventTrackingHelper.haveinternet, String.valueOf(NetworkUtil.isNetworkActive(activity)));
        bundle.putString(EventTrackingHelper.showallad, String.valueOf(Admob.getInstance().getShowAllAds()));
        bundle.putString(EventTrackingHelper.idcheck, String.valueOf(idCheck));
        bundle.putString(EventTrackingHelper.interremote + "_" + EventTrackingHelper.openremote + "_" + EventTrackingHelper.aoavalue, RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.inter_splash) + "_" + RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.open_splash) + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        EventTrackingHelper.getInstance(activity).logEventWithMultipleParams(EventTrackingHelper.inter_splash_tracking, bundle);

        //log event can request
        EventTrackingHelper.getInstance(activity).logEvent(EventTrackingHelper.inter_splash_true);
        //end log event can request
        //time start load splash ads
        timeSplashLoadingAdShow = System.currentTimeMillis();

        AdRequest adRequest = new AdRequest.Builder().build();
        logEventSplash(activity, "splash_loop_start_call");
        InterstitialAd.load(activity, idInterSplash, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        logEventSplash(activity, "splash_loop_loaded");
                        //Tracking revenue
                        interstitialAd.setOnPaidEventListener(adValue -> {
                            //Adjust
                            AdjustUtil.trackRevenue(interstitialAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdInter.get(0), "inter_splash");
                        });
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        Log.i(TAG, "SPLASH: Ad was loaded inter splash loop. " + idInterSplash);
                        interCallback.onAdLoaded(interstitialAd);
                        mInterstitialAdSplash = interstitialAd;
                        EventTrackingHelper.getInstance(activity).logEvent("splash_loop_true");
                        showInterAdsSplash(activity, interCallback);
                        removeHandlerSplashAds();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Bundle bundle = new Bundle();
                        bundle.putString("failed_message", "load_" + loadAdError.getMessage());
                        logEventSplash(activity, "splash_loop_load_failed", bundle);
                        Log.e(TAG, "SPLASH: Ad Failed To Load." + loadAdError);
                        interCallback.onAdFailedToLoad();
                        loadAndShowInterAdSplashLoop(activity, listIdInter, interCallback);
                    }
                });
    }


    //================================ Fail-safe ================================
    // Called by the host Activity (e.g. onResume) to retry showing the splash inter if a previous
    // attempt to show it failed (isFailToShowAdSplash).

    public void onCheckShowSplashWhenFail(AppCompatActivity activity, InterCallback interCallback) {
        if (isFailToShowAdSplash) {
            Log.d(TAG, "SPLASH: onCheckShowSplashWhenFail.");
            if (!AdmobAdsConfig.getInstance().getLoadAndShowIdInterAdSplashAsync()) {
                showInterAdsSplash(activity, interCallback);
            } else {
                if (mInterstitialAdSplashHigh != null) {
                    showInterAdsSplashAsync(mInterstitialAdSplashHigh, activity, interCallback);
                } else {
                    showInterAdsSplashAsync(mInterstitialAdSplash, activity, interCallback);
                }
            }
        }
    }

    private void startNativeAfterInter(Activity activity, InterCallback interCallback) {
        NativeAfterInterActivity.Companion.setInterCallback(interCallback);
        NativeAfterInterActivity.Companion.setSplashMode(false);
        Intent intent = new Intent(activity, NativeAfterInterActivity.class);
        activity.startActivity(intent);
    }
}