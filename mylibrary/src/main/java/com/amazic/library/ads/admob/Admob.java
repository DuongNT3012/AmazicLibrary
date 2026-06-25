package com.amazic.library.ads.admob;

import static com.amazic.library.Utils.EventTrackingHelper.time_splash_loading_ad_show;
import static com.amazic.library.Utils.EventTrackingHelper.time_splash_loading_show;
import static com.amazic.library.ads.splash_ads.AsyncSplash.DETECT_TEST_AD;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

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
import com.amazic.library.ads.admob.admob_interface.IOnAdsFailToLoad;
import com.amazic.library.ads.admob.admob_interface.IOnAdsImpression;
import com.amazic.library.ads.admob.admob_interface.IOnInitAdmobDone;
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.ads.callback.BannerCallback;
import com.amazic.library.ads.callback.InterCallback;
import com.amazic.library.ads.callback.NativeCallback;
import com.amazic.library.ads.callback.RewardedCallback;
import com.amazic.library.ads.callback.RewardedInterCallback;
import com.amazic.library.ads.collapse_banner_ads.CollapseBannerHelper;
import com.amazic.library.ads.native_ads.NativeAfterInterManager;
import com.amazic.library.ads.splash_ads.AsyncSplash;
import com.amazic.library.dialog.LoadingAdsDialog;
import com.amazic.library.organic.TechManager;
import com.amazic.library.ump.AdsConsentManager;
import com.amazic.library.view.NativeAfterInterActivity;
import com.amazic.mylibrary.R;
import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize;
import com.google.android.libraries.ads.mobile.sdk.banner.AdView;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdValue;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration;
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo;
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions;
import com.google.android.libraries.ads.mobile.sdk.initialization.AdapterStatus;
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader;
import com.google.android.libraries.ads.mobile.sdk.nativead.CustomNativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader;
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class Admob {
    private static Admob INSTANCE;
    private static final String TAG = "Admob";
    public LoadingAdsDialog loadingAdsDialog;
    public ArrayList<Integer> listAnimationDialogRaw = new ArrayList<>();
    private boolean isCustomAnimationDialog = false;
    private boolean isInterOrRewardedShowing = false;
    private boolean isShowAllAds = true;
    private InterstitialAd mInterstitialAdSplashHigh;
    private InterstitialAd mInterstitialAdSplash;
    private boolean isFailToShowAdSplash = false;
    private long timeInterval = 0L;
    private long lastTimeDismissInter = 0L;
    private long timeIntervalFromStart = 0L;
    private long timeStart = 0L;
    private String tokenEventAdjust = "";
    private final Handler handlerTimeoutSplash = new Handler(Looper.getMainLooper());
    private final Handler handlerTimeoutInter = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private boolean isSplashResume = true;
    private boolean openActivityAfterShowInterAds = true;
    private boolean isDetectTestAdByView = false;
    private int countClickInterSplashAds = 0;
    private NativeAd myNativeAd = null;
    private int timeOutCallSplashAds = 12000;
    private int timeOutCallInterAds = 12000;
    //Log event 26/04/2025
    private long timeSplashLoadingAdShow = 0;
    //fix event time_splash_loading_show
    private boolean isLoadInterSplashIdTimeout = false;
    private boolean isLoadInterAdsIdTimeout = false;
    public int timeHttpInter = -1;
    public int timeHttpNative = -1;
    public int timeHttpBanner = -1;
    public int timeHttpOpen = -1;

    //update request check time delay show ads splash
    private final Handler handlerDelayAdsSplash = new Handler(Looper.getMainLooper());
    private Runnable timerDelayRunnable;
    private boolean isTimerDelayFinished = false;
    private boolean isAdLoadAdsSplashFinished = false;
    private long startTime;
    private int timeDelayAdsSplash = 7000;
    private boolean isInitAdmobDone = false;
    //end

    //use to destroy banner
    private AdView adViewBanner;
    private AdView adViewBannerFragment;

    private String appID = "";

    public static Admob getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Admob();
        }
        return INSTANCE;
    }

    public void initAdmob(Activity activity, IOnInitAdmobDone iOnInitAdmobDone) {
        if (Objects.equals(appID, "")) return;
        resetVariable();
        initLoadingDialog(activity);
        Log.d("Admob", "initAdmob: application start");
        new Thread(() -> {
            // Initialize the SDK on a background thread.
            MobileAds.initialize(activity.getApplicationContext(), new InitializationConfig.Builder(getAppID()).setNativeValidatorDisabled().build(), initializationStatus -> {
                Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
                boolean isAdMobReady = false;

                for (String adapterClass : statusMap.keySet()) {
                    AdapterStatus status = statusMap.get(adapterClass);

                    if ((status != null ? status.getInitializationState() : null) != null &&
                            status.getInitializationState() == AdapterStatus.InitializationState.COMPLETE) {
                        Log.d("Admob_INIT", String.format("Thành công: Adapter %s đã khởi tạo.", status.getInitializationState()));

                        isAdMobReady = true;
                    } else {
                        Log.e("Admob_INIT", String.format("Thất bại: Adapter %s bị lỗi: %s", adapterClass, status.getDescription()));
                    }
                }

                Admob.getInstance().setIsInitAdmobDone(isAdMobReady);
                iOnInitAdmobDone.onInitAdmobDone(isAdMobReady);
            });
        }).start();
    }

    private void resetVariable() {
        this.isLoadInterSplashIdTimeout = false;
    }

    public void initLoadingDialog(Context context) {
        if (loadingAdsDialog == null) {
            loadingAdsDialog = new LoadingAdsDialog(context);
        }
    }

    public boolean checkCondition(Context context, String adsKey) {
        Log.d(TAG, "checkCondition: Network_" + NetworkUtil.isNetworkActive(context) + "_UMP_" + AdsConsentManager.getConsentResult(context) + "_showAllAds_" + isShowAllAds + "_IAP_" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig_" + RemoteConfigHelper.getInstance().get_config(context, adsKey));
        return NetworkUtil.isNetworkActive(context) && AdsConsentManager.getConsentResult(context) && isShowAllAds && /*!IAPManager.getInstance().isPurchase() &&*/ RemoteConfigHelper.getInstance().get_config(context, adsKey);
    }

    public String getAppID() {
        return appID;
    }

    public void setAppID(String appID) {
        this.appID = appID;
    }

    public int getTimeDelayWaitInterHigh() {
        return timeDelayWaitInterHigh;
    }

    public void setTimeDelayWaitInterHigh(int timeDelayWaitInterHigh) {
        this.timeDelayWaitInterHigh = timeDelayWaitInterHigh;
    }

    public boolean isCustomAnimationDialog() {
        return isCustomAnimationDialog;
    }

    public void setCustomAnimationDialog(boolean customAnimationDialog) {
        this.isCustomAnimationDialog = customAnimationDialog;
    }

    public void setCustomAnimationDialog(ArrayList<Integer> listAnimationDialogRaw) {
        this.isCustomAnimationDialog = true;
        this.listAnimationDialogRaw.clear();
        this.listAnimationDialogRaw.addAll(listAnimationDialogRaw);
    }

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

    public int getTimeOutCallInterAds() {
        return timeOutCallInterAds;
    }

    public void setTimeOutCallInterAds(int timeOutCallInterAds) {
        this.timeOutCallInterAds = timeOutCallInterAds;
    }

    public int getTimeOutCallSplashAds() {
        return timeOutCallSplashAds;
    }

    public void setTimeOutCallSplashAds(int timeOutCallSplashAds) {
        this.timeOutCallSplashAds = timeOutCallSplashAds;
    }

    public int getTimeDelayAdsSplash() {
        return timeDelayAdsSplash;
    }

    public void setTimeDelayAdsSplash(int timeDelay) {
        this.timeDelayAdsSplash = timeDelay;
    }

    public boolean isDetectTestAdByView() {
        return isDetectTestAdByView;
    }

    public void setDetectTestAdByView(boolean detectTestAdByView) {
        this.isDetectTestAdByView = detectTestAdByView;
    }

    public boolean isOpenActivityAfterShowInterAds() {
        return openActivityAfterShowInterAds;
    }

    public void setOpenActivityAfterShowInterAds(boolean openActivityAfterShowInterAds) {
        this.openActivityAfterShowInterAds = openActivityAfterShowInterAds;
    }

    public void setTimeStart(long timeStart) {
        this.timeStart = timeStart;
    }

    public long getTimeStart() {
        Log.d(TAG, "getTimeStart: " + (System.currentTimeMillis() - this.timeStart) / (1000) + "(s)");
        return this.timeStart;
    }

    public void setTimeInterval(long timeInterval, boolean resetLastTimeDismissInter) {
        if (resetLastTimeDismissInter) {
            this.lastTimeDismissInter = 0L;
        }
        this.timeInterval = timeInterval;
    }

    public void setTimeIntervalFromStart(long timeIntervalFromStart) {
        this.timeIntervalFromStart = timeIntervalFromStart;
    }

    public void setTokenEventAdjust(String tokenEventAdjust) {
        this.tokenEventAdjust = tokenEventAdjust;
    }

    public String getTokenEventAdjust() {
        return this.tokenEventAdjust;
    }

    public boolean isInterOrRewardedShowing() {
        return isInterOrRewardedShowing;
    }

    public void setShowAllAds(boolean isShowAllAds) {
        this.isShowAllAds = isShowAllAds;
    }

    public boolean getShowAllAds() {
        return isShowAllAds;
    }

    public void setIsInitAdmobDone(boolean isInitDone) {
        this.isInitAdmobDone = isInitDone;
    }

    public boolean getIsInitAdmobDone() {
        return isInitAdmobDone;
    }

    public void removeHandlerInterAds() {
        if (handlerTimeoutInter != null && runnable != null) {
            handlerTimeoutInter.removeCallbacks(runnable);
            handlerTimeoutInter.removeCallbacksAndMessages(null);
            //handlerTimeoutInter = null;
        }
    }

    public void removeHandlerSplashAds() {
        if (runnable != null) {
            handlerTimeoutSplash.removeCallbacks(runnable);
            handlerTimeoutSplash.removeCallbacksAndMessages(null);
            //handlerTimeoutSplash = null;
        }
    }

    private void dismissLoadingDialog() {
        try {
            loadingAdsDialog.dismiss();
        } catch (Exception e) {
            Log.e(TAG, "dismissLoadingDialog: " + e.getMessage());
        }
    }

    //================================Start inter ads================================
    public void loadInterAdsLoadAndShow(Activity activity, List<String> listIdInter, InterCallback interCallback, String remoteKey) {
        ArrayList<String> listIdInterTemp = new ArrayList<>(listIdInter);
        //Set timeout inter ads x(s) if cannot load
        isLoadInterAdsIdTimeout = false;
        runnable = () -> {
            Log.d(TAG, "loadInterAdsLoadAndShow: inter_ads_id_timeout: " + remoteKey);
            EventTrackingHelper.logEventWithAParam(activity, EventTrackingHelper.inter_ads_id_timeout, "remoteKey", remoteKey);
            if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                dismissLoadingDialog();
            }
            if (interCallback != null) {
                isLoadInterAdsIdTimeout = true;
                interCallback.onNextAction();
            }
            removeHandlerInterAds();
        };
        handlerTimeoutInter.postDelayed(runnable, timeOutCallInterAds);
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "INTER: Check condition. RemoteKey:" + remoteKey + ". Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdInterTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            isInterOrRewardedShowing = false;
            interCallback.onNextAction();
            removeHandlerInterAds();
            return;
        }
        if (System.currentTimeMillis() - lastTimeDismissInter < timeInterval) {
            Log.d(TAG, "INTER: Not show interstitial because the time interval. " + remoteKey);
            interCallback.onNextAction();
            removeHandlerInterAds();
            return;
        }
        if (System.currentTimeMillis() - timeStart < timeIntervalFromStart) {
            Log.d(TAG, "INTER: Not show interstitial because the time interval from start. " + remoteKey);
            interCallback.onNextAction();
            removeHandlerInterAds();
            return;
        }
        loadingAdsDialog = new LoadingAdsDialog(activity);
        if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
            loadingAdsDialog.show();
        }
        isInterOrRewardedShowing = true;
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        InterstitialAd.load(new AdRequest.Builder(listIdInterTemp.get(0)).build(),
                new AdLoadCallback<InterstitialAd>() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        interCallback.onAdLoaded(interstitialAd);
                        Log.i(TAG, "INTER: onAdLoaded. " + remoteKey);
                        if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                            dismissLoadingDialog();
                        }
                        showInterAdsLoadAndShow(activity, interstitialAd, interCallback, remoteKey);
                        removeHandlerInterAds();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.e(TAG, "INTER: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                        interCallback.onAdFailedToLoad();
                        if (!listIdInterTemp.isEmpty()) {
                            listIdInterTemp.remove(0);
                        }
                        if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                            dismissLoadingDialog();
                        }
                        loadInterAdsLoadAndShow(activity, listIdInterTemp, interCallback, remoteKey);
                    }
                });
    }

    public void showInterAdsLoadAndShow(Activity activity, InterstitialAd mInterstitialAd, InterCallback interCallback, String remoteKey) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "INTER: Check condition. RemoteKey:" + remoteKey + ". Network:" + NetworkUtil.isNetworkActive(activity) + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            interCallback.onNextAction();
            return;
        }
        if (mInterstitialAd == null) {
            Log.d(TAG, "INTER: The interstitial ad wasn't ready yet. " + remoteKey);
            interCallback.onNextAction();
            return;
        }
        if (!isLoadInterAdsIdTimeout && !activity.isFinishing() && !activity.isDestroyed()) {
            mInterstitialAd.setAdEventCallback(new InterstitialAdEventCallback() {
                @Override
                public void onAdPaid(@NonNull AdValue value) {
                    //Tracking revenue
                    AdjustUtil.trackRevenue(mInterstitialAd.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
                }

                @Override
                public void onAdClicked() {
                    AppOpenManager.isLastActionClickAd = true;
                    // Called when a click is recorded for an ad.
                    Log.d(TAG, "INTER: Ad was clicked. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                    interCallback.onAdClicked();
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    Log.d(TAG, "INTER: Ad dismissed fullscreen content. " + remoteKey);
                    interCallback.onAdDismissedFullScreenContent();
                    if (!openActivityAfterShowInterAds) {
                        interCallback.onNextAction();
                    }
                    isInterOrRewardedShowing = false;
                    lastTimeDismissInter = System.currentTimeMillis();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                    // Called when ad fails to show.
                    Log.e(TAG, "INTER: Ad failed to show fullscreen content. " + remoteKey);
                    interCallback.onAdFailedToShowFullScreenContent();
                    if (!openActivityAfterShowInterAds) {
                        interCallback.onNextAction();
                    }
                    isInterOrRewardedShowing = false;
                    removeHandlerInterAds();
                }

                @Override
                public void onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    Log.d(TAG, "INTER: Ad recorded an impression. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                    interCallback.onAdImpression();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    Log.d(TAG, "INTER: Ad showed fullscreen content. " + remoteKey);
                    interCallback.onAdShowedFullScreenContent();
                    isInterOrRewardedShowing = true;
                    removeHandlerInterAds();
                }
            });
            isInterOrRewardedShowing = true;
            if (openActivityAfterShowInterAds) {
                interCallback.onNextAction();
            }
            mInterstitialAd.setImmersiveMode(true);
            mInterstitialAd.show(activity);
        }
    }

    public void loadInterAdsLoadAndShowWithNativeAfterInter(Activity activity, List<String> listIdInter, InterCallback interCallback, String remoteKeyInter, String remoteKeyNative, String adsKeyNative) {
        boolean isShowNativeAfterInter = RemoteConfigHelper.getInstance().get_config(activity, remoteKeyNative);

        ArrayList<String> listIdInterTemp = new ArrayList<>(listIdInter);
        //Set timeout inter ads x(s) if cannot load
        isLoadInterAdsIdTimeout = false;
        runnable = () -> {
            Log.d(TAG, "loadInterAdsLoadAndShow: inter_ads_id_timeout: " + remoteKeyInter);
            EventTrackingHelper.logEventWithAParam(activity, EventTrackingHelper.inter_ads_id_timeout, "remoteKey", remoteKeyInter);
            if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                dismissLoadingDialog();
            }
            if (interCallback != null) {
                isLoadInterAdsIdTimeout = true;
                if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                    if (isShowNativeAfterInter) {
                        NativeAd nativeAd = NativeAfterInterManager.mapNativeAdsAfterInter.get(adsKeyNative);
                        if (nativeAd == null) {
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
            removeHandlerInterAds();
        };
        handlerTimeoutInter.postDelayed(runnable, timeOutCallInterAds);
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKeyInter)) {
            Log.d(TAG, "INTER: Check condition. RemoteKey:" + remoteKeyInter + ". Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdInterTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKeyInter));
            isInterOrRewardedShowing = false;
            interCallback.onNextAction();
            removeHandlerInterAds();
            return;
        }
        if (System.currentTimeMillis() - lastTimeDismissInter < timeInterval) {
            Log.d(TAG, "INTER: Not show interstitial because the time interval. " + remoteKeyInter);
            interCallback.onNextAction();
            removeHandlerInterAds();
            return;
        }
        if (System.currentTimeMillis() - timeStart < timeIntervalFromStart) {
            Log.d(TAG, "INTER: Not show interstitial because the time interval from start. " + remoteKeyInter);
            interCallback.onNextAction();
            removeHandlerInterAds();
            return;
        }
        loadingAdsDialog = new LoadingAdsDialog(activity);
        if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
            loadingAdsDialog.show();
        }
        isInterOrRewardedShowing = true;
        EventTrackingHelper.logEvent(activity, remoteKeyInter + "_true");
        InterstitialAd.load(new AdRequest.Builder(listIdInterTemp.get(0)).build(),
                new AdLoadCallback<InterstitialAd>() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        interCallback.onAdLoaded(interstitialAd);
                        Log.i(TAG, "INTER: onAdLoaded. " + remoteKeyInter);
                        if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                            dismissLoadingDialog();
                        }
                        showInterAdsLoadAndShowWithNativeAfterInter(activity, interstitialAd, interCallback, adsKeyNative, remoteKeyInter, isShowNativeAfterInter);
                        removeHandlerInterAds();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.e(TAG, "INTER: onAdFailedToLoad. " + loadAdError + ". " + remoteKeyInter);
                        interCallback.onAdFailedToLoad();
                        if (!listIdInterTemp.isEmpty()) {
                            listIdInterTemp.remove(0);
                        }
                        if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                            dismissLoadingDialog();
                        }
                        loadInterAdsLoadAndShow(activity, listIdInterTemp, interCallback, remoteKeyInter);
                    }
                });
    }

    public void showInterAdsLoadAndShowWithNativeAfterInter(Activity activity, InterstitialAd mInterstitialAd, InterCallback interCallback, String adsKeyNative, String remoteKeyInter, boolean isShowNativeAfterInter) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKeyInter)) {
            Log.d(TAG, "INTER: Check condition. RemoteKey:" + remoteKeyInter + ". Network:" + NetworkUtil.isNetworkActive(activity) + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKeyInter));
            interCallback.onNextAction();
            return;
        }
        if (mInterstitialAd == null) {
            Log.d(TAG, "INTER: The interstitial ad wasn't ready yet. " + remoteKeyInter);
            if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                if (isShowNativeAfterInter) {
                    NativeAd nativeAd = NativeAfterInterManager.mapNativeAdsAfterInter.get(adsKeyNative);
                    if (nativeAd == null) {
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
        if (!isLoadInterAdsIdTimeout && !activity.isFinishing() && !activity.isDestroyed()) {
            mInterstitialAd.setAdEventCallback(new InterstitialAdEventCallback() {
                @Override
                public void onAdPaid(@NonNull AdValue value) {
                    //Tracking revenue
                    AdjustUtil.trackRevenue(mInterstitialAd.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
                }

                @Override
                public void onAdClicked() {
                    AppOpenManager.isLastActionClickAd = true;
                    // Called when a click is recorded for an ad.
                    Log.d(TAG, "INTER: Ad was clicked. " + remoteKeyInter);
                    EventTrackingHelper.logEvent(activity, remoteKeyInter + "_click");
                    interCallback.onAdClicked();
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    Log.d(TAG, "INTER: Ad dismissed fullscreen content. " + remoteKeyInter + " ,openActivityAfterShowInterAds = " + openActivityAfterShowInterAds);
                    interCallback.onAdDismissedFullScreenContent();
                    if (!openActivityAfterShowInterAds) {
                        if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                            if (isShowNativeAfterInter) {
                                startNativeAfterInter(activity, interCallback);
                            } else {
                                interCallback.onNextAction();
                            }
                        } else {
                            interCallback.onNextAction();
                        }
                    } else {
                        /// can check neu truong hop load fail native after inter thi dismiss chuyen onnext
                        if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                            if (isShowNativeAfterInter) {
                                NativeAd nativeAd = NativeAfterInterManager.mapNativeAdsAfterInter.get(adsKeyNative);
                                if (nativeAd == null) {
                                    interCallback.onNextAction();
                                }

                            }
                        }
                    }
                    isInterOrRewardedShowing = false;
                    lastTimeDismissInter = System.currentTimeMillis();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                    // Called when ad fails to show.
                    Log.e(TAG, "INTER: Ad failed to show fullscreen content. " + remoteKeyInter);
                    interCallback.onAdFailedToShowFullScreenContent();
                    if (!openActivityAfterShowInterAds) {
                        if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                            if (isShowNativeAfterInter) {
                                startNativeAfterInter(activity, interCallback);
                            } else {
                                interCallback.onNextAction();
                            }
                        } else {
                            interCallback.onNextAction();
                        }
                    } else {
                        /// can check neu truong hop load fail native after inter thi dismiss chuyen onnext
                        if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                            if (isShowNativeAfterInter) {
                                NativeAd nativeAd = NativeAfterInterManager.mapNativeAdsAfterInter.get(adsKeyNative);
                                if (nativeAd == null) {
                                    interCallback.onNextAction();
                                }

                            }
                        }
                    }
                    isInterOrRewardedShowing = false;
                    removeHandlerInterAds();
                }

                @Override
                public void onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    Log.d(TAG, "INTER: Ad recorded an impression. " + remoteKeyInter);
                    EventTrackingHelper.logEvent(activity, remoteKeyInter + "_view");
                    interCallback.onAdImpression();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    Log.d(TAG, "INTER: Ad showed fullscreen content. " + remoteKeyInter);
                    interCallback.onAdShowedFullScreenContent();
                    isInterOrRewardedShowing = true;
                    removeHandlerInterAds();
                }
            });
            isInterOrRewardedShowing = true;
            if (openActivityAfterShowInterAds) {
                if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                    if (isShowNativeAfterInter) {
                        startNativeAfterInter(activity, interCallback);
                    } else {
                        interCallback.onNextAction();
                    }
                } else {
                    interCallback.onNextAction();
                }
            }
            mInterstitialAd.setImmersiveMode(true);
            mInterstitialAd.show(activity);
        }
    }

    private void startNativeAfterInter(Activity activity, InterCallback interCallback) {
        NativeAfterInterActivity.Companion.setInterCallback(interCallback);
        Intent intent = new Intent(activity, NativeAfterInterActivity.class);
        activity.startActivity(intent);
    }

    //Inter Preload
    public void loadInterAdPreload(Context context, List<String> listIdInter, InterCallback interCallback, String remoteKey) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(context) || listIdInter.isEmpty() || !AdsConsentManager.getConsentResult(context) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(context, remoteKey)) {
            Log.d(TAG, "INTER Ad Preload: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(context) + "_IdEmpty:" + listIdInter.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(context) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(context, remoteKey));
            interCallback.onAdFailedToLoad();
            return;
        }
        EventTrackingHelper.logEvent(context, remoteKey + "_true");

        Log.d(TAG, "INTER Ad Preload: number ad preloading = " + AsyncSplash.Companion.getInstance().getNumberPreloading());
        PreloadConfiguration configuration = new PreloadConfiguration(new AdRequest.Builder(listIdInter.get(0)).build(), AsyncSplash.Companion.getInstance().getNumberPreloading());

        PreloadCallback callback = new PreloadCallback() {
            @Override
            public void onAdFailedToPreload(@NonNull String preloadId, @NonNull LoadAdError adError) {
                EventTrackingHelper.logEvent(context, remoteKey + "inter_preload_failed");
                Log.d(TAG, "INTER Ad Preload: Preload ad " + preloadId + " failed to load with error: " + adError.getMessage());
                interCallback.onAdFailedToLoad();
            }

            @Override
            public void onAdPreloaded(@NonNull String preloadId, @NonNull ResponseInfo responseInfo) {
                EventTrackingHelper.logEvent(context, remoteKey + "inter_preload_loaded");
                Log.d(TAG, "INTER Ad Preload: Preload ad for " + preloadId + " is available.");
                interCallback.onAdLoaded(null);
            }

            @Override
            public void onAdsExhausted(@NonNull String preloadId) {
                Log.d(TAG, "INTER Ad Preload: Preload ad for " + preloadId + " is exhausted.");
            }
        };

        InterstitialAdPreloader.start(listIdInter.get(0), configuration, callback);

    }

    public void showInterAdPreload(Activity activity, List<String> listIdInter, InterCallback interCallback, boolean isShowLoading, String remoteKey, String adsKeyNative, boolean isShowNativeAfterInter) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdInter.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "INTER Ad Preload: Check condition. RemoteKey:" + remoteKey + ". Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty: " + listIdInter.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            interCallback.onNextAction();
            return;
        }
        if (System.currentTimeMillis() - lastTimeDismissInter < timeInterval) {
            Log.d(TAG, "INTER Ad Preload: Not show interstitial because the time interval. " + remoteKey);
            interCallback.onNextAction();
            return;
        }
        if (System.currentTimeMillis() - timeStart < timeIntervalFromStart) {
            Log.d(TAG, "INTER Ad Preload: Not show interstitial because the time interval from start. " + remoteKey);
            interCallback.onNextAction();
            return;
        }
        Log.d(TAG, "INTER Ad Preload: Check isAdAvailable InterstitialAdPreloader - " + InterstitialAdPreloader.isAdAvailable(listIdInter.get(0)));
        if (!InterstitialAdPreloader.isAdAvailable(listIdInter.get(0))) {
            Log.d(TAG, "INTER Ad Preload: The interstitial ad wasn't ready yet. " + remoteKey);
            Log.d(TAG, "INTER Ad Preload: InterstitialAdPreloader.isAdAvailable - getShowNativeAfterInter =  " + AsyncSplash.Companion.getInstance().getShowNativeAfterInter() + ", isShowNativeAfterInter = " + isShowNativeAfterInter);
            if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                if (isShowNativeAfterInter) {
                    NativeAd nativeAd = NativeAfterInterManager.mapNativeAdsAfterInter.get(adsKeyNative);
                    if (nativeAd == null) {
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
        if (isShowLoading) {
            loadingAdsDialog = new LoadingAdsDialog(activity);
            if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
                loadingAdsDialog.show();
            }
        }

        InterstitialAd ad = InterstitialAdPreloader.pollAd(listIdInter.get(0));
        if (ad != null) {
            ad.setAdEventCallback(new InterstitialAdEventCallback() {
                @Override
                public void onAdPaid(@NonNull AdValue value) {
                    AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
                }

                @Override
                public void onAdClicked() {
                    AppOpenManager.isLastActionClickAd = true;
                    // Called when a click is recorded for an ad.
                    Log.d(TAG, "INTER Ad Preload: Ad was clicked. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "inter_preload_click");
                    interCallback.onAdClicked();
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "INTER Ad Preload: Ad dismissed fullscreen content. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "inter_preload_dismiss");
                    interCallback.onAdDismissedFullScreenContent();

                    Log.d(TAG, "INTER Ad Preload: onAdDismissedFullScreenContent - getShowNativeAfterInter =  " + AsyncSplash.Companion.getInstance().getShowNativeAfterInter() + ", isShowNativeAfterInter = " + isShowNativeAfterInter + ", openActivityAfterShowInterAds = " + openActivityAfterShowInterAds);

                    if (!openActivityAfterShowInterAds) {
                        if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                            if (isShowNativeAfterInter) {
                                startNativeAfterInter(activity, interCallback);
                            } else {
                                interCallback.onNextAction();
                            }
                        } else {
                            interCallback.onNextAction();
                        }
                    } else {
                        /// can check neu truong hop load fail native after inter thi dismiss chuyen onnext
                        if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                            if (isShowNativeAfterInter) {
                                NativeAd nativeAd = NativeAfterInterManager.mapNativeAdsAfterInter.get(adsKeyNative);
                                if (nativeAd == null) {
                                    interCallback.onNextAction();
                                }

                            }
                        }
                    }
                    isInterOrRewardedShowing = false;
                    lastTimeDismissInter = System.currentTimeMillis();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                    Log.e(TAG, "INTER Ad Preload: Ad failed to show fullscreen content. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "inter_preload_failed_to_show");
                    interCallback.onAdFailedToShowFullScreenContent();
                    Log.d(TAG, "INTER Ad Preload: onAdFailedToShowFullScreenContent - getShowNativeAfterInter =  " + AsyncSplash.Companion.getInstance().getShowNativeAfterInter() + ", isShowNativeAfterInter = " + isShowNativeAfterInter + ", openActivityAfterShowInterAds = " + openActivityAfterShowInterAds);

                    if (!openActivityAfterShowInterAds) {
                        if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                            if (isShowNativeAfterInter) {
                                startNativeAfterInter(activity, interCallback);
                            } else {
                                interCallback.onNextAction();
                            }
                        } else {
                            interCallback.onNextAction();
                        }
                    } else {
                        /// can check neu truong hop load fail native after inter thi dismiss chuyen onnext
                        if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                            if (isShowNativeAfterInter) {
                                NativeAd nativeAd = NativeAfterInterManager.mapNativeAdsAfterInter.get(adsKeyNative);
                                if (nativeAd == null) {
                                    interCallback.onNextAction();
                                }

                            }
                        }
                    }
                    if (!activity.isFinishing() && !activity.isDestroyed() && isShowLoading && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
                    }
                    isInterOrRewardedShowing = false;
                    removeHandlerInterAds();
                }

                @Override
                public void onAdImpression() {
                    Log.d(TAG, "INTER Ad Preload: Ad recorded an impression. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "inter_preload_impression");
                    interCallback.onAdImpression();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "INTER Ad Preload: Ad showed fullscreen content. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "inter_preload_show_full_screen");
                    interCallback.onAdShowedFullScreenContent();
                    if (!activity.isFinishing() && !activity.isDestroyed() && isShowLoading && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
                    }
                    isInterOrRewardedShowing = true;
                    removeHandlerInterAds();
                }
            });
            isInterOrRewardedShowing = true;
            Log.d(TAG, "INTER Ad Preload: call show ads - getShowNativeAfterInter =  " + AsyncSplash.Companion.getInstance().getShowNativeAfterInter() + ", isShowNativeAfterInter = " + isShowNativeAfterInter + ", openActivityAfterShowInterAds = " + openActivityAfterShowInterAds);

            if (openActivityAfterShowInterAds) {
                if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                    if (isShowNativeAfterInter) {
                        startNativeAfterInter(activity, interCallback);
                    } else {
                        interCallback.onNextAction();
                    }
                } else {
                    interCallback.onNextAction();
                }
            }
            ad.show(activity);
        } else {
            Log.d(TAG, "INTER Ad Preload: not call show ads- getShowNativeAfterInter =  " + AsyncSplash.Companion.getInstance().getShowNativeAfterInter() + ", isShowNativeAfterInter = " + isShowNativeAfterInter + ", openActivityAfterShowInterAds = " + openActivityAfterShowInterAds);

            if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                if (isShowNativeAfterInter) {
                    NativeAd nativeAd = NativeAfterInterManager.mapNativeAdsAfterInter.get(adsKeyNative);
                    if (nativeAd == null) {
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

    }

    /// use load and show first ad preloading
    public void loadInterAdPreloadWithHandleTimeOut(Activity activity, List<String> listIdInter, InterCallback interCallback, String remoteKey, String remoteKeyNativeAfterInter, String adsKeyNativeAfterInter) {
        if (listIdInter.isEmpty()) {
            interCallback.onNextAction();
            return;
        }

        if (InterstitialAdPreloader.isAdAvailable(listIdInter.get(0))) {
            Log.d(TAG, "INTER Ad Preload: loadInterAdsLoadAndShow HAVE DATA => show inter preload " + remoteKey);
            interCallback.onAdLoaded(null);
        } else {
            boolean isShowNativeAfterInter;
            if (remoteKeyNativeAfterInter != "") {
                isShowNativeAfterInter = RemoteConfigHelper.getInstance().get_config(activity, remoteKeyNativeAfterInter);
            } else {
                isShowNativeAfterInter = false;
            }

            Log.d(TAG, "INTER Ad Preload: loadInterAdsLoadAndShow NO DATA => load and show inter preload, isShowNativeAfterInter =  " + isShowNativeAfterInter);

            ArrayList<String> listIdInterTemp = new ArrayList<>(listIdInter);
            //Set timeout inter ads x(s) if cannot load
            isLoadInterAdsIdTimeout = false;
            runnable = () -> {
                Log.d(TAG, "INTER Ad Preload: loadInterAdsLoadAndShow - inter_ads_id_timeout: " + remoteKey);
                EventTrackingHelper.logEventWithAParam(activity, EventTrackingHelper.inter_ads_id_timeout, "remoteKey", remoteKey);
                if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    dismissLoadingDialog();
                }
                if (interCallback != null) {
                    isLoadInterAdsIdTimeout = true;
                    Log.d(TAG, "INTER Ad Preload: loadInterAdsLoadAndShow - inter_ads_id_timeout: getShowNativeAfterInter = " + AsyncSplash.Companion.getInstance().getShowNativeAfterInter());
                    if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                        Log.d(TAG, "INTER Ad Preload: loadInterAdsLoadAndShow - inter_ads_id_timeout: isShowNativeAfterInter = " + isShowNativeAfterInter);
                        if (isShowNativeAfterInter) {
                            NativeAd nativeAd = NativeAfterInterManager.mapNativeAdsAfterInter.get(adsKeyNativeAfterInter);
                            Log.d(TAG, "INTER Ad Preload: loadInterAdsLoadAndShow - inter_ads_id_timeout: nativeAd = " + nativeAd);
                            if (nativeAd == null) {
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
                removeHandlerInterAds();
            };
            handlerTimeoutInter.postDelayed(runnable, timeOutCallInterAds);
            //Check condition
            if (!NetworkUtil.isNetworkActive(activity) || listIdInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
                Log.d(TAG, "INTER Ad Preload: loadAndShow: Check condition. RemoteKey:" + remoteKey + ". Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdInterTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
                isInterOrRewardedShowing = false;
                interCallback.onNextAction();
                removeHandlerInterAds();
                return;
            }
            if (System.currentTimeMillis() - lastTimeDismissInter < timeInterval) {
                Log.d(TAG, "INTER Ad Preload: loadAndshow: Not show interstitial because the time interval. " + remoteKey);
                interCallback.onNextAction();
                removeHandlerInterAds();
                return;
            }
            if (System.currentTimeMillis() - timeStart < timeIntervalFromStart) {
                Log.d(TAG, "INTER Ad Preload: loadAndShow: Not show interstitial because the time interval from start. " + remoteKey);
                interCallback.onNextAction();
                removeHandlerInterAds();
                return;
            }
            loadingAdsDialog = new LoadingAdsDialog(activity);
            if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
                loadingAdsDialog.show();
            }
            isInterOrRewardedShowing = true;
            EventTrackingHelper.logEvent(activity, remoteKey + "_true");


            PreloadConfiguration configuration = new PreloadConfiguration(new AdRequest.Builder(listIdInterTemp.get(0)).build(), AsyncSplash.Companion.getInstance().getNumberPreloading());

            final AtomicBoolean isFirstLoadAd = new AtomicBoolean(true);
            PreloadCallback callback = new PreloadCallback() {
                @Override
                public void onAdFailedToPreload(@NonNull String preloadId, @NonNull LoadAdError adError) {
                    EventTrackingHelper.logEvent(activity, remoteKey + "inter_preload_failed");
                    Log.d(TAG, "INTER Ad Preload - loadAndShow: onAdFailedToPreload - Preload ad " + preloadId + " failed to load with error: " + adError.getMessage());
                    if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
                    }

                    if (isFirstLoadAd.getAndSet(false)) {
                        Log.d(TAG, "INTER Ad Preload - loadAndShow: onAdFailedToPreload - getShowNativeAfterInter = " + AsyncSplash.Companion.getInstance().getShowNativeAfterInter());
                        if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                            if (isShowNativeAfterInter) {
                                Log.d(TAG, "INTER Ad Preload - loadAndShow: onAdFailedToPreload - show Native after inter");
                                startNativeAfterInter(activity, interCallback);
                            } else {
                                Log.d(TAG, "INTER Ad Preload - loadAndShow: onAdFailedToPreload - ONNEXT Not show Native after inter");
                                interCallback.onNextAction();
                            }
                        } else {
                            Log.d(TAG, "INTER Ad Preload - loadAndShow: onAdFailedToPreload -Onnext");
                            interCallback.onNextAction();
                        }
                    }
                    removeHandlerInterAds();
                }

                @Override
                public void onAdPreloaded(@NonNull String preloadId, @NonNull ResponseInfo responseInfo) {
                    EventTrackingHelper.logEvent(activity, remoteKey + "inter_preload_loaded");
                    Log.d(TAG, "INTER Ad Preload - loadAndShow: Preload ad for " + preloadId + " is available.");
                    if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
                    }
                    if (isFirstLoadAd.getAndSet(false)) {
                        interCallback.onAdLoaded(null);
                    }
                    removeHandlerInterAds();
                }

                @Override
                public void onAdsExhausted(@NonNull String preloadId) {
                    Log.d(TAG, "INTER Ad Preload - loadAdnShow: Preload ad for " + preloadId + " is exhausted.");
                }
            };
            InterstitialAdPreloader.start(listIdInterTemp.get(0), configuration, callback);
        }
    }

    public void loadAndShowInterAdPreloadingSplashDelay(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback, String adsKeyNative, String remoteKeyNative) {
        Log.d(TAG, "AdsSplash Inter preload: Bắt đầu tiến trình Load And Show Inter Delay ads...");
        NativeAfterInterManager.preloadNativeAfterInter(activity, adsKeyNative, remoteKeyNative);
        startTime = System.currentTimeMillis();

        boolean isConfigShowNativeAfterInter = RemoteConfigHelper.getInstance().get_config(activity, remoteKeyNative);

        boolean isEmptyListNativeAfterInter = AdmobApi.getInstance().getListIDByName(adsKeyNative).isEmpty();

        ArrayList<String> listIdInterTemp = new ArrayList<>(listIdInter);
        //Set timeout ads splash x(s) if cannot load
        runnable = () -> {
            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_id_timeout);
            if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                dismissLoadingDialog();
            }
            if (interCallback != null) {
                isLoadInterSplashIdTimeout = true;
                if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                    if (isConfigShowNativeAfterInter) {
                        if (isEmptyListNativeAfterInter) {
                            interCallback.onNextAction();
                        } else {
                            NativeAd nativeAd = NativeAfterInterManager.mapNativeAdsAfterInter.get(adsKeyNative);
                            if (nativeAd == null) {
                                interCallback.onNextAction();
                            } else {
                                startNativeAfterInter(activity, interCallback);
                            }
                        }
                    } else {
                        interCallback.onNextAction();
                    }
                } else {
                    interCallback.onNextAction();
                }
            }
            removeHandlerSplashAds();
        };
        handlerTimeoutSplash.postDelayed(runnable, timeOutCallSplashAds);

        //delay ads splash
        timerDelayRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "AdsSplash Inter preload: Đã đủ 7 giây đếm ngược.");
                isTimerDelayFinished = true;
                checkConditionAdPreloadingSplash(activity, listIdInterTemp, interCallback, isConfigShowNativeAfterInter, isEmptyListNativeAfterInter, adsKeyNative);
            }
        };
        handlerDelayAdsSplash.postDelayed(timerDelayRunnable, timeDelayAdsSplash);
        //end

        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds /*|| IAPManager.getInstance().isPurchase()*/) {
            Log.d(TAG, "AdsSplash Inter preload: Check condition loadAndShowInterAdSplash " + NetworkUtil.isNetworkActive(activity) + "_" + listIdInterTemp.isEmpty() + "_" + AdsConsentManager.getConsentResult(activity) + "_" + isShowAllAds + "_" /*+ IAPManager.getInstance().isPurchase()*/);
            interCallback.onNextAction();
            removeHandlerSplashAds();
            Bundle bundle = new Bundle();
            bundle.putString("failed_message", "lib_internet_" + NetworkUtil.isNetworkActive(activity)
                    + "_Consent_" + AdsConsentManager.getConsentResult(activity)
                    + "_isShowAllAds_" + isShowAllAds
            );
            EventTrackingHelper.logEventWithMultipleParams(activity, "splash_delay_failed", bundle);
            return;
        }

        //Log event
        Bundle bundle = new Bundle();
        boolean idCheck = AdmobApi.getInstance().getListAdsSize() > 0;
        bundle.putString(EventTrackingHelper.splash_detail, AdsConsentManager.getConsentResult(activity) + "_" + TechManager.getInstance().isTech(activity) + "_" + NetworkUtil.isNetworkActive(activity) + "_" + getShowAllAds() + "_" + idCheck + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        bundle.putString(EventTrackingHelper.ump, String.valueOf(AdsConsentManager.getConsentResult(activity)));
        bundle.putString(EventTrackingHelper.organic, String.valueOf(TechManager.getInstance().isTech(activity)));
        bundle.putString(EventTrackingHelper.haveinternet, String.valueOf(NetworkUtil.isNetworkActive(activity)));
        bundle.putString(EventTrackingHelper.showallad, String.valueOf(getShowAllAds()));
        bundle.putString(EventTrackingHelper.idcheck, String.valueOf(idCheck));
        bundle.putString(EventTrackingHelper.interremote + "_" + EventTrackingHelper.openremote + "_" + EventTrackingHelper.aoavalue, RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.inter_splash) + "_" + RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.open_splash) + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        EventTrackingHelper.logEventWithMultipleParams(activity, EventTrackingHelper.inter_splash_tracking, bundle);

        //log event can request
        EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_true);
        //end log event can request
        //time start load splash ads
        timeSplashLoadingAdShow = System.currentTimeMillis();

        Log.d(TAG, "AdsSplash Inter preload: number ad preloading = " + AsyncSplash.Companion.getInstance().getNumberPreloadingSplash());

        PreloadConfiguration configuration = new PreloadConfiguration(new AdRequest.Builder(listIdInterTemp.get(0)).build(), AsyncSplash.Companion.getInstance().getNumberPreloadingSplash());

        PreloadCallback callback = new PreloadCallback() {
            @Override
            public void onAdFailedToPreload(@NonNull String preloadId, @NonNull LoadAdError adError) {
                Log.d(TAG, "AdsSplash Inter preload: Preload ad " + preloadId + " failed to load with error: " + adError.getMessage());
                Bundle bundle = new Bundle();
                bundle.putString("failed_message", "load_" + adError.getMessage());
                EventTrackingHelper.logEventWithMultipleParams(activity, "splash_delay_failed", bundle);
                interCallback.onAdFailedToLoad();
                if (listIdInterTemp.size() > 1) {
                    listIdInterTemp.remove(0);
                    loadAndShowInterAdPreloadingSplashDelay(activity, listIdInterTemp, interCallback, adsKeyNative, remoteKeyNative);
                }
            }

            @Override
            public void onAdPreloaded(@NonNull String preloadId, @NonNull ResponseInfo responseInfo) {
                Log.i(TAG, "AdsSplash Inter preload: Ad loaded inter splash.");
                EventTrackingHelper.logEvent(activity, "splash_delay_true");
                isAdLoadAdsSplashFinished = true;
                Log.d(TAG, "AdsSplash Inter preload: Preload ad for " + preloadId + " is available.");
                interCallback.onAdLoaded(null);

                /// show ads
                checkConditionAdPreloadingSplash(activity, listIdInterTemp, interCallback, isConfigShowNativeAfterInter, isEmptyListNativeAfterInter, adsKeyNative);
                removeHandlerSplashAds();
            }

            @Override
            public void onAdsExhausted(@NonNull String preloadId) {
                Log.d(TAG, "AdsSplash Inter preload: Preload ad for " + preloadId + " is exhausted.");
            }
        };

        InterstitialAdPreloader.start(listIdInterTemp.get(0), configuration, callback);
    }

    private void checkConditionAdPreloadingSplash(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback, boolean isConfigShowNativeAfterInter, boolean isEmptyListNativeAfterInter, String adsKeyNative) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.d(TAG, "removeHandlerDelayAdsSplash");
            removeHandlerDelayAdsSplash();
            return;
        }
        if (isTimerDelayFinished && isAdLoadAdsSplashFinished) {
            String timeFormatted = String.format(Locale.US, "%.2f", (System.currentTimeMillis() - startTime) / 1000.0);
            Log.d(TAG, "AdsSplash Inter preload: ===> TỔNG THỜI GIAN CHỜ: " + timeFormatted + " giây , isEmptyListNativeAfterInter = " + isEmptyListNativeAfterInter);
            EventTrackingHelper.logEventWithAParam(activity, "Splash_time_wait", "time_to_step", timeFormatted);
            showInterAdPreloadingSplashDelay(activity, listIdInter, interCallback, isConfigShowNativeAfterInter, isEmptyListNativeAfterInter, adsKeyNative);
            removeHandlerDelayAdsSplash();
        }

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

        InterstitialAd ad = InterstitialAdPreloader.pollAd(listIdInter.get(0));

        if (ad == null) {
            Log.d(TAG, "AdsSplash Inter preload: The interstitial ad wasn't ready yet.");
            AppOpenManager.getInstance().setEnableResume(true);
            if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                if (isConfigShowNativeAfterInter) {
                    if (isEmptyListNativeAfterInter) {
                        interCallback.onNextAction();
                    } else {
                        NativeAd nativeAd = NativeAfterInterManager.mapNativeAdsAfterInter.get(adsKeyNative);
                        if (nativeAd == null) {
                            interCallback.onNextAction();
                        } else {
                            startNativeAfterInter(activity, interCallback);
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
                ad.setAdEventCallback(new InterstitialAdEventCallback() {
                    @Override
                    public void onAdPaid(@NonNull AdValue value) {
                        AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
                    }

                    @Override
                    public void onAdClicked() {
                        AppOpenManager.isLastActionClickAd = true;
                        Log.d(TAG, "AdsSplash Inter preload: Ad was clicked.");
                        interCallback.onAdClicked();
                        countClickInterSplashAds++;
                        int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                        if (splashOpenTimes == 1) {
                            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_click + "_" + countClickInterSplashAds);
                        }
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        //increase splash open
                        SharePreferenceHelper.setInt(activity, EventTrackingHelper.splash_open, SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1);
                        //end increase splash open
                        Log.d(TAG, "AdsSplash Inter preload: Ad dismissed fullscreen content.");
                        interCallback.onAdDismissedFullScreenContent();
                        AppOpenManager.getInstance().setEnableResume(true);
                        Log.d(TAG, "AdsSplash Inter preload: start check openActivityAfterShowInterAds = " + openActivityAfterShowInterAds);

                        if (!openActivityAfterShowInterAds) {
                            Log.d(TAG, "AdsSplash Inter preload: start check getShowNativeAfterInter = " + AsyncSplash.Companion.getInstance().getShowNativeAfterInter());
                            if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                                Log.d(TAG, "AdsSplash Inter preload: start check isConfigShowNativeAfterInter = " + isConfigShowNativeAfterInter + ", isEmptyListNativeAfterInter = " + isEmptyListNativeAfterInter);
                                if (isConfigShowNativeAfterInter) {
                                    if (isEmptyListNativeAfterInter) {
                                        interCallback.onNextAction();
                                    } else {
                                        NativeAd nativeAd = NativeAfterInterManager.mapNativeAdsAfterInter.get(adsKeyNative);
                                        if (nativeAd == null) {
                                            interCallback.onNextAction();
                                        } else {
                                            startNativeAfterInter(activity, interCallback);
                                        }
                                    }
                                } else {
                                    interCallback.onNextAction();
                                }
                            } else {
                                interCallback.onNextAction();
                            }
                        }
                        isInterOrRewardedShowing = false;
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                        //increase splash open
                        SharePreferenceHelper.setInt(activity, EventTrackingHelper.splash_open, SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1);
                        //end increase splash open
                        Log.e(TAG, "AdsSplash Inter preload: Ad failed to show fullscreen content.");
                        interCallback.onAdFailedToShowFullScreenContent();
                        if (isSplashResume && !openActivityAfterShowInterAds) {
                            if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                                if (isConfigShowNativeAfterInter) {
                                    if (isEmptyListNativeAfterInter) {
                                        interCallback.onNextAction();
                                    } else {
                                        NativeAd nativeAd = NativeAfterInterManager.mapNativeAdsAfterInter.get(adsKeyNative);
                                        if (nativeAd == null) {
                                            interCallback.onNextAction();
                                        } else {
                                            startNativeAfterInter(activity, interCallback);
                                        }
                                    }
                                } else {
                                    interCallback.onNextAction();
                                }
                            } else {
                                interCallback.onNextAction();
                            }
                        }
                        if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                            dismissLoadingDialog();
                        }
                        isFailToShowAdSplash = true;
                        AppOpenManager.getInstance().setEnableResume(true);
                        isInterOrRewardedShowing = false;
                        removeHandlerSplashAds();
                        //log event
                        EventTrackingHelper.logEventWithAParam(activity, EventTrackingHelper.inter_splash_showad_time, EventTrackingHelper.showad_time, "false_" + (System.currentTimeMillis() - AsyncSplash.Companion.getInstance().getTimeStartSplash()) / 1000);
                        //end log event
                    }

                    @Override
                    public void onAdImpression() {
                        InterstitialAdPreloader.destroy(listIdInter.get(0));
                        // Called when an impression is recorded for an ad.
                        Log.d(TAG, "AdsSplash Inter preload: Ad recorded an impression.");
                        interCallback.onAdImpression();
                        //log event
                        EventTrackingHelper.logEventWithAParam(activity, time_splash_loading_ad_show, time_splash_loading_show, String.valueOf((System.currentTimeMillis() - timeSplashLoadingAdShow) / 1000));
                        EventTrackingHelper.logEventWithAParam(activity, EventTrackingHelper.inter_splash_showad_time, EventTrackingHelper.showad_time, "true_" + (System.currentTimeMillis() - AsyncSplash.Companion.getInstance().getTimeStartSplash()) / 1000);
                        int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                        if (splashOpenTimes <= 3) {
                            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_impression + "_" + splashOpenTimes);
                        }
                        //end log event
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        // Called when ad is shown.
                        Log.d(TAG, "AdsSplash Inter preload: Ad showed fullscreen content.");
                        interCallback.onAdShowedFullScreenContent();
                        if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                            dismissLoadingDialog();
                        }
                        isInterOrRewardedShowing = true;
                        isFailToShowAdSplash = false;
                        removeHandlerSplashAds();
                    }
                });
                boolean isResumeState = ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
                Log.d(TAG, "AdsSplash Inter preload: ResumeState: " + isResumeState);
                if (isResumeState) {
                    loadingAdsDialog = new LoadingAdsDialog(activity);
                    if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
                        loadingAdsDialog.show();
                    }
                    isInterOrRewardedShowing = true;
                    AppOpenManager.getInstance().setEnableResume(false);
                    if (openActivityAfterShowInterAds) {
                        Log.d(TAG, "AdsSplash Inter preload: showInterAdsSplash: openActivityAfterShowInterAds = true, onNextAction");
                        if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                            if (isConfigShowNativeAfterInter) {
                                if (isEmptyListNativeAfterInter) {
                                    Log.d(TAG, "AdsSplash Inter preload: showInterAdsSplash: isEmptyListNativeAfterInter = " + isEmptyListNativeAfterInter);
                                    interCallback.onNextAction();
                                } else {
                                    NativeAd nativeAd = NativeAfterInterManager.mapNativeAdsAfterInter.get(adsKeyNative);
                                    if (nativeAd == null) {
                                        Log.d(TAG, "AdsSplash Inter preload: showInterAdsSplash: NATIVE AD NULL NOT Show Native After Inter");
                                        interCallback.onNextAction();
                                    } else {
                                        Log.d(TAG, "AdsSplash Inter preload: showInterAdsSplash: show Native After Inter");
                                        startNativeAfterInter(activity, interCallback);
                                    }
                                }
                            } else {
                                Log.d(TAG, "AdsSplash Inter preload: showInterAdsSplash: onNextAction");
                                interCallback.onNextAction();
                            }
                        } else {
                            Log.d(TAG, "AdsSplash Inter preload: showInterAdsSplash: onNextAction init set off Native After Inter");
                            interCallback.onNextAction();
                        }
                    }
                    Log.d(TAG, "AdsSplash Inter preload: showInterAdsSplash: show Inter");
                    ad.setImmersiveMode(true);
                    ad.show(activity);
                } else {
                    Log.e(TAG, "AdsSplash Inter preload: Fail to show on background.");
                    if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
                    }
                    isFailToShowAdSplash = true;
                    if (runnable != null) {
                        handlerTimeoutSplash.removeCallbacks(runnable);
                    }
                }
            }, 250);
        }
    }

    //End Inter Preload

    public void loadInterAds(Context context, List<String> listIdInter, InterCallback interCallback, String remoteKey) {
        ArrayList<String> listIdInterTemp = new ArrayList<>(listIdInter);
        //Check condition
        if (!NetworkUtil.isNetworkActive(context) || listIdInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(context) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(context, remoteKey)) {
            Log.d(TAG, "INTER: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(context) + "_IdEmpty:" + listIdInterTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(context) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(context, remoteKey));
            interCallback.onNextAction();
            return;
        }
        EventTrackingHelper.logEvent(context, remoteKey + "_true");
        InterstitialAd.load(
                new AdRequest.Builder(listIdInterTemp.get(0)).build(),
                new AdLoadCallback<InterstitialAd>() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        interCallback.onAdLoaded(interstitialAd);
                        Log.i(TAG, "INTER: onAdLoaded. " + remoteKey);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.e(TAG, "INTER: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                        interCallback.onAdFailedToLoad();
                        if (!listIdInterTemp.isEmpty()) {
                            listIdInterTemp.remove(0);
                        }
                        loadInterAds(context, listIdInterTemp, interCallback, remoteKey);
                    }
                });
    }

    public void showInterAds(Activity activity, InterstitialAd mInterstitialAd, InterCallback interCallback, boolean isShowLoading, String remoteKey) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "INTER: Check condition. RemoteKey:" + remoteKey + ". Network:" + NetworkUtil.isNetworkActive(activity) + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            interCallback.onNextAction();
            return;
        }
        if (System.currentTimeMillis() - lastTimeDismissInter < timeInterval) {
            Log.d(TAG, "INTER: Not show interstitial because the time interval. " + remoteKey);
            interCallback.onNextAction();
            return;
        }
        if (System.currentTimeMillis() - timeStart < timeIntervalFromStart) {
            Log.d(TAG, "INTER: Not show interstitial because the time interval from start. " + remoteKey);
            interCallback.onNextAction();
            return;
        }
        if (mInterstitialAd == null) {
            Log.d(TAG, "INTER: The interstitial ad wasn't ready yet. " + remoteKey);
            interCallback.onNextAction();
            return;
        }
        if (isShowLoading) {
            loadingAdsDialog = new LoadingAdsDialog(activity);
            if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
                loadingAdsDialog.show();
            }
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            mInterstitialAd.setAdEventCallback(new InterstitialAdEventCallback() {
                @Override
                public void onAdPaid(@NonNull AdValue value) {
                    //Tracking revenue
                    AdjustUtil.trackRevenue(mInterstitialAd.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
                }

                @Override
                public void onAdClicked() {
                    AppOpenManager.isLastActionClickAd = true;
                    // Called when a click is recorded for an ad.
                    Log.d(TAG, "INTER: Ad was clicked. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                    interCallback.onAdClicked();
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    Log.d(TAG, "INTER: Ad dismissed fullscreen content. " + remoteKey);
                    interCallback.onAdDismissedFullScreenContent();
                    if (!openActivityAfterShowInterAds) {
                        interCallback.onNextAction();
                    }
                    isInterOrRewardedShowing = false;
                    lastTimeDismissInter = System.currentTimeMillis();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                    // Called when ad fails to show.
                    Log.e(TAG, "INTER: Ad failed to show fullscreen content. " + remoteKey);
                    interCallback.onAdFailedToShowFullScreenContent();
                    if (!openActivityAfterShowInterAds) {
                        interCallback.onNextAction();
                    }
                    if (!activity.isFinishing() && !activity.isDestroyed() && isShowLoading && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
                    }
                    isInterOrRewardedShowing = false;
                }

                @Override
                public void onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    Log.d(TAG, "INTER: Ad recorded an impression. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                    interCallback.onAdImpression();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    Log.d(TAG, "INTER: Ad showed fullscreen content. " + remoteKey);
                    interCallback.onAdShowedFullScreenContent();
                    if (!activity.isFinishing() && !activity.isDestroyed() && isShowLoading && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
                    }
                    isInterOrRewardedShowing = true;
                }
            });
            isInterOrRewardedShowing = true;
            if (openActivityAfterShowInterAds) {
                interCallback.onNextAction();
            }
            mInterstitialAd.setImmersiveMode(true);
            mInterstitialAd.show(activity);
        }, 250);
    }

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
            if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
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
                mInterstitialAdSplash.setAdEventCallback(new InterstitialAdEventCallback() {
                    @Override
                    public void onAdPaid(@NonNull AdValue value) {
                        //Tracking revenue
                        Log.d(TAG, "onAdPaid: ");
                        if (mInterstitialAdSplash != null) {
                            AdjustUtil.trackRevenue(mInterstitialAdSplash.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
                        }
                    }

                    @Override
                    public void onAdClicked() {
                        AppOpenManager.isLastActionClickAd = true;
                        // Called when a click is recorded for an ad.
                        Log.d(TAG, "SPLASH: Ad was clicked.");
                        interCallback.onAdClicked();
                        countClickInterSplashAds++;
                        int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                        if (splashOpenTimes == 1) {
                            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_click + "_" + countClickInterSplashAds);
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
                        if (!openActivityAfterShowInterAds) {
                            if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
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
                        isInterOrRewardedShowing = false;
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                        //increase splash open
                        SharePreferenceHelper.setInt(activity, EventTrackingHelper.splash_open, SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1);
                        //end increase splash open
                        Log.e(TAG, "SPLASH: Ad failed to show fullscreen content.");
                        interCallback.onAdFailedToShowFullScreenContent();
                        if (isSplashResume && !openActivityAfterShowInterAds) {
                            if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
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
                        if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                            dismissLoadingDialog();
                        }
                        isFailToShowAdSplash = true;
                        AppOpenManager.getInstance().setEnableResume(true);
                        isInterOrRewardedShowing = false;
                        removeHandlerSplashAds();
                        //log event
                        EventTrackingHelper.logEventWithAParam(activity, EventTrackingHelper.inter_splash_showad_time, EventTrackingHelper.showad_time, "false_" + (System.currentTimeMillis() - AsyncSplash.Companion.getInstance().getTimeStartSplash()) / 1000);
                        //end log event
                    }

                    @Override
                    public void onAdImpression() {
                        // Called when an impression is recorded for an ad.
                        Log.d(TAG, "SPLASH: Ad recorded an impression.");
                        interCallback.onAdImpression();
                        //log event
                        EventTrackingHelper.logEventWithAParam(activity, time_splash_loading_ad_show, time_splash_loading_show, String.valueOf((System.currentTimeMillis() - timeSplashLoadingAdShow) / 1000));
                        EventTrackingHelper.logEventWithAParam(activity, EventTrackingHelper.inter_splash_showad_time, EventTrackingHelper.showad_time, "true_" + (System.currentTimeMillis() - AsyncSplash.Companion.getInstance().getTimeStartSplash()) / 1000);
                        int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                        if (splashOpenTimes <= 3) {
                            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_impression + "_" + splashOpenTimes);
                        }
                        //end log event
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        // Called when ad is shown.
                        Log.d(TAG, "SPLASH: Ad showed fullscreen content.");
                        mInterstitialAdSplash = null;
                        interCallback.onAdShowedFullScreenContent();
                        if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                            dismissLoadingDialog();
                        }
                        isInterOrRewardedShowing = true;
                        isFailToShowAdSplash = false;
                        removeHandlerSplashAds();
                    }
                });
                boolean isResumeState = ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
                Log.d(TAG, "SPLASH: ResumeState: " + isResumeState);
                if (isResumeState) {
                    loadingAdsDialog = new LoadingAdsDialog(activity);
                    if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
                        loadingAdsDialog.show();
                    }
                    isInterOrRewardedShowing = true;
                    AppOpenManager.getInstance().setEnableResume(false);
                    if (openActivityAfterShowInterAds) {
                        Log.d(TAG, "SPLASH: showInterAdsSplash: openActivityAfterShowInterAds = true, onNextAction");
                        if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
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
                    Log.d(TAG, "SPLASH: showInterAdsSplash: show Inter");
                    mInterstitialAdSplash.setImmersiveMode(true);
                    mInterstitialAdSplash.show(activity);
                } else {
                    Log.e(TAG, "SPLASH: Fail to show on background.");
                    if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
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
            Log.d(TAG, "SPLASH: The interstitial ad wasn't ready yet.");
            AppOpenManager.getInstance().setEnableResume(true);
            interCallback.onNextAction();
            return;
        }
        if (!isLoadInterSplashIdTimeout && !activity.isFinishing() && !activity.isDestroyed()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                mInterstitialAdSplash.setAdEventCallback(new InterstitialAdEventCallback() {
                    @Override
                    public void onAdPaid(@NonNull AdValue value) {
                        //Tracking revenue
                        AdjustUtil.trackRevenue(mInterstitialAdSplash.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
                    }

                    @Override
                    public void onAdClicked() {
                        AppOpenManager.isLastActionClickAd = true;
                        // Called when a click is recorded for an ad.
                        Log.d(TAG, "SPLASH: Ad was clicked.");
                        interCallback.onAdClicked();
                        countClickInterSplashAds++;
                        int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                        if (splashOpenTimes == 1) {
                            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_click + "_" + countClickInterSplashAds);
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
                        if (!openActivityAfterShowInterAds) {
                            interCallback.onNextAction();
                        }
                        isInterOrRewardedShowing = false;
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                        //increase splash open
                        SharePreferenceHelper.setInt(activity, EventTrackingHelper.splash_open, SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1);
                        //end increase splash open
                        Log.e(TAG, "SPLASH: Ad failed to show fullscreen content.");
                        interCallback.onAdFailedToShowFullScreenContent();
                        if (isSplashResume && !openActivityAfterShowInterAds) {
                            interCallback.onNextAction();
                        }
                        if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                            dismissLoadingDialog();
                        }
                        isFailToShowAdSplash = true;
                        AppOpenManager.getInstance().setEnableResume(true);
                        isInterOrRewardedShowing = false;
                        removeHandlerSplashAds();
                        //log event
                        EventTrackingHelper.logEventWithAParam(activity, EventTrackingHelper.inter_splash_showad_time, EventTrackingHelper.showad_time, "false_" + (System.currentTimeMillis() - AsyncSplash.Companion.getInstance().getTimeStartSplash()) / 1000);
                        //end log event
                    }

                    @Override
                    public void onAdImpression() {
                        // Called when an impression is recorded for an ad.
                        Log.d(TAG, "SPLASH: Ad recorded an impression.");
                        interCallback.onAdImpression();
                        //log event
                        EventTrackingHelper.logEventWithAParam(activity, time_splash_loading_ad_show, time_splash_loading_show, String.valueOf((System.currentTimeMillis() - timeSplashLoadingAdShow) / 1000));
                        EventTrackingHelper.logEventWithAParam(activity, EventTrackingHelper.inter_splash_showad_time, EventTrackingHelper.showad_time, "true_" + (System.currentTimeMillis() - AsyncSplash.Companion.getInstance().getTimeStartSplash()) / 1000);
                        int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                        if (splashOpenTimes <= 3) {
                            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_impression + "_" + splashOpenTimes);
                        }
                        //end log event
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        // Called when ad is shown.
                        Log.d(TAG, "SPLASH: Ad showed fullscreen content.");
                        mInterstitialAdSplash = null;
                        interCallback.onAdShowedFullScreenContent();
                        if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                            dismissLoadingDialog();
                        }
                        isInterOrRewardedShowing = true;
                        isFailToShowAdSplash = false;
                        removeHandlerSplashAds();
                    }
                });
                boolean isResumeState = ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
                Log.d(TAG, "SPLASH: ResumeState: " + isResumeState);
                if (isResumeState) {
                    loadingAdsDialog = new LoadingAdsDialog(activity);
                    if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
                        loadingAdsDialog.show();
                    }
                    isInterOrRewardedShowing = true;
                    AppOpenManager.getInstance().setEnableResume(false);
                    if (openActivityAfterShowInterAds) {
                        Log.d(TAG, "SPLASH: showInterAdsSplash: openActivityAfterShowInterAds = true, onNextAction");
                        interCallback.onNextAction();
                    }
                    mInterstitialAdSplash.setImmersiveMode(true);
                    mInterstitialAdSplash.show(activity);
                } else {
                    Log.e(TAG, "SPLASH: Fail to show on background.");
                    if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
                    }
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
                interSplash.setAdEventCallback(new InterstitialAdEventCallback() {
                    @Override
                    public void onAdPaid(@NonNull AdValue value) {
                        //Tracking revenue
                        AdjustUtil.trackRevenue(interSplash.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
                    }

                    @Override
                    public void onAdClicked() {
                        AppOpenManager.isLastActionClickAd = true;
                        // Called when a click is recorded for an ad.
                        Log.d(TAG, "SPLASH: Ad was clicked.");
                        interCallback.onAdClicked();
                        countClickInterSplashAds++;
                        int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                        if (splashOpenTimes == 1) {
                            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_click + "_" + countClickInterSplashAds);
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
                        if (!openActivityAfterShowInterAds) {
                            interCallback.onNextAction();
                        }
                        isInterOrRewardedShowing = false;
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                        //increase splash open
                        SharePreferenceHelper.setInt(activity, EventTrackingHelper.splash_open, SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1);
                        //end increase splash open
                        Log.e(TAG, "SPLASH: Ad failed to show fullscreen content.");
                        interCallback.onAdFailedToShowFullScreenContent();
                        if (isSplashResume && !openActivityAfterShowInterAds) {
                            interCallback.onNextAction();
                        }
                        if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                            dismissLoadingDialog();
                        }
                        isFailToShowAdSplash = true;
                        AppOpenManager.getInstance().setEnableResume(true);
                        isInterOrRewardedShowing = false;
                        removeHandlerSplashAds();
                        //log event
                        EventTrackingHelper.logEventWithAParam(activity, EventTrackingHelper.inter_splash_showad_time, EventTrackingHelper.showad_time, "false_" + (System.currentTimeMillis() - AsyncSplash.Companion.getInstance().getTimeStartSplash()) / 1000);
                        //end log event
                    }

                    @Override
                    public void onAdImpression() {
                        // Called when an impression is recorded for an ad.
                        Log.d(TAG, "SPLASH: Ad recorded an impression.");
                        interCallback.onAdImpression();
                        //log event
                        EventTrackingHelper.logEventWithAParam(activity, time_splash_loading_ad_show, time_splash_loading_show, String.valueOf((System.currentTimeMillis() - timeSplashLoadingAdShow) / 1000));
                        EventTrackingHelper.logEventWithAParam(activity, EventTrackingHelper.inter_splash_showad_time, EventTrackingHelper.showad_time, "true_" + (System.currentTimeMillis() - AsyncSplash.Companion.getInstance().getTimeStartSplash()) / 1000);
                        int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                        if (splashOpenTimes <= 3) {
                            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_impression + "_" + splashOpenTimes);
                        }
                        //end log event
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        // Called when ad is shown.
                        Log.d(TAG, "SPLASH: Ad showed fullscreen content.");
                        interCallback.onAdShowedFullScreenContent();
                        if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                            dismissLoadingDialog();
                        }
                        isInterOrRewardedShowing = true;
                        isFailToShowAdSplash = false;
                        removeHandlerSplashAds();
                    }
                });
                boolean isResumeState = ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
                Log.d(TAG, "SPLASH: ResumeState: " + isResumeState);
                if (isResumeState) {
                    loadingAdsDialog = new LoadingAdsDialog(activity);
                    if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
                        loadingAdsDialog.show();
                    }
                    isInterOrRewardedShowing = true;
                    AppOpenManager.getInstance().setEnableResume(false);
                    if (openActivityAfterShowInterAds) {
                        Log.d(TAG, "SPLASH: showInterAdsSplash: openActivityAfterShowInterAds = true, onNextAction");
                        interCallback.onNextAction();
                    }
                    interSplash.setImmersiveMode(true);
                    interSplash.show(activity);
                } else {
                    Log.e(TAG, "SPLASH: Fail to show on background.");
                    if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
                    }
                    isFailToShowAdSplash = true;
                    if (handlerTimeoutSplash != null && runnable != null) {
                        handlerTimeoutSplash.removeCallbacks(runnable);
                    }
                }
            }, 250);
        }
    }

    private boolean isShownInterSplashHigh = false;
    private boolean isShownInterSplashNormal = false;
    private int timeDelayWaitInterHigh = 2000;
    private boolean isHandledLoadAdsSplashFail = false;

    //load all id inter splash once
    public void loadAndShowIdInterAdSplashAsync(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback) {
        ArrayList<String> listIdInterTemp = new ArrayList<>(listIdInter);
        //Set timeout ads splash x(s) if cannot load
        runnable = () -> {
            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_id_timeout);
            if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                dismissLoadingDialog();
            }
            if (interCallback != null) {
                isLoadInterSplashIdTimeout = true;
                interCallback.onNextAction();
            }
            removeHandlerSplashAds();
        };
        handlerTimeoutSplash.postDelayed(runnable, timeOutCallSplashAds);

        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds /*|| IAPManager.getInstance().isPurchase()*/) {
            Log.d(TAG, "Check condition loadAndShowIdInterAdSplashAsync " + NetworkUtil.isNetworkActive(activity) + "_" + listIdInterTemp.isEmpty() + "_" + AdsConsentManager.getConsentResult(activity) + "_" + isShowAllAds + "_" /*+ IAPManager.getInstance().isPurchase()*/);

            Bundle bundle = new Bundle();
            bundle.putString("failed_message", "lib_internet_" + NetworkUtil.isNetworkActive(activity)
                    + "_Consent_" + AdsConsentManager.getConsentResult(activity)
                    + "_isShowAllAds_" + isShowAllAds
            );
            EventTrackingHelper.logEventWithMultipleParams(activity, "splash_asyn_failed", bundle);
            interCallback.onNextAction();
            removeHandlerSplashAds();
            return;
        }

        //Log event
        Bundle bundle = new Bundle();
        boolean idCheck = AdmobApi.getInstance().getListAdsSize() > 0;
        bundle.putString(EventTrackingHelper.splash_detail, AdsConsentManager.getConsentResult(activity) + "_" + TechManager.getInstance().isTech(activity) + "_" + NetworkUtil.isNetworkActive(activity) + "_" + getShowAllAds() + "_" + idCheck + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        bundle.putString(EventTrackingHelper.ump, String.valueOf(AdsConsentManager.getConsentResult(activity)));
        bundle.putString(EventTrackingHelper.organic, String.valueOf(TechManager.getInstance().isTech(activity)));
        bundle.putString(EventTrackingHelper.haveinternet, String.valueOf(NetworkUtil.isNetworkActive(activity)));
        bundle.putString(EventTrackingHelper.showallad, String.valueOf(getShowAllAds()));
        bundle.putString(EventTrackingHelper.idcheck, String.valueOf(idCheck));
        bundle.putString(EventTrackingHelper.interremote + "_" + EventTrackingHelper.openremote + "_" + EventTrackingHelper.aoavalue, RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.inter_splash) + "_" + RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.open_splash) + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        EventTrackingHelper.logEventWithMultipleParams(activity, EventTrackingHelper.inter_splash_tracking, bundle);

        //log event can request
        EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_true);
        //end log event can request
        //time start load splash ads
        timeSplashLoadingAdShow = System.currentTimeMillis();

        for (int i = 0; i < listIdInterTemp.size(); i++) {
            int index = i;
            Log.d(TAG, "SPLASH ID ASYNC: Start load inter splash. " + listIdInterTemp.get(index));
            InterstitialAd.load(new AdRequest.Builder(listIdInterTemp.get(index)).build(),
                    new AdLoadCallback<InterstitialAd>() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
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
                            EventTrackingHelper.logEventWithMultipleParams(activity, "splash_asyn_failed", bundleE);
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

    public void loadAndShowInterAdSplash(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback) {
        ArrayList<String> listIdInterTemp = new ArrayList<>(listIdInter);
        //Set timeout ads splash x(s) if cannot load
        runnable = () -> {
            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_id_timeout);
            if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                dismissLoadingDialog();
            }
            if (interCallback != null) {
                isLoadInterSplashIdTimeout = true;
                interCallback.onNextAction();
            }
            removeHandlerSplashAds();
        };
        handlerTimeoutSplash.postDelayed(runnable, timeOutCallSplashAds);

        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds /*|| IAPManager.getInstance().isPurchase()*/) {
            Log.d(TAG, "Check condition loadAndShowInterAdSplash " + NetworkUtil.isNetworkActive(activity) + "_" + listIdInterTemp.isEmpty() + "_" + AdsConsentManager.getConsentResult(activity) + "_" + isShowAllAds + "_" /*+ IAPManager.getInstance().isPurchase()*/);
            interCallback.onNextAction();
            removeHandlerSplashAds();
            return;
        }

        //Log event
        Bundle bundle = new Bundle();
        boolean idCheck = AdmobApi.getInstance().getListAdsSize() > 0;
        bundle.putString(EventTrackingHelper.splash_detail, AdsConsentManager.getConsentResult(activity) + "_" + TechManager.getInstance().isTech(activity) + "_" + NetworkUtil.isNetworkActive(activity) + "_" + getShowAllAds() + "_" + idCheck + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        bundle.putString(EventTrackingHelper.ump, String.valueOf(AdsConsentManager.getConsentResult(activity)));
        bundle.putString(EventTrackingHelper.organic, String.valueOf(TechManager.getInstance().isTech(activity)));
        bundle.putString(EventTrackingHelper.haveinternet, String.valueOf(NetworkUtil.isNetworkActive(activity)));
        bundle.putString(EventTrackingHelper.showallad, String.valueOf(getShowAllAds()));
        bundle.putString(EventTrackingHelper.idcheck, String.valueOf(idCheck));
        bundle.putString(EventTrackingHelper.interremote + "_" + EventTrackingHelper.openremote + "_" + EventTrackingHelper.aoavalue, RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.inter_splash) + "_" + RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.open_splash) + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        EventTrackingHelper.logEventWithMultipleParams(activity, EventTrackingHelper.inter_splash_tracking, bundle);

        //log event can request
        EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_true);
        //end log event can request
        //time start load splash ads
        timeSplashLoadingAdShow = System.currentTimeMillis();

        InterstitialAd.load(new AdRequest.Builder(listIdInterTemp.get(0)).build(),
                new AdLoadCallback<InterstitialAd>() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
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
        Log.d(TAG, "Bắt đầu tiến trình Load And Show Inter Delay ads...");
        NativeAfterInterManager.preloadNativeAfterInter(activity, adsKeyNative, remoteKeyNative);
        startTime = System.currentTimeMillis();

        boolean isConfigShowNativeAfterInter = RemoteConfigHelper.getInstance().get_config(activity, remoteKeyNative);

        boolean isEmptyListNativeAfterInter = AdmobApi.getInstance().getListIDByName(adsKeyNative).isEmpty();

        ArrayList<String> listIdInterTemp = new ArrayList<>(listIdInter);
        //Set timeout ads splash x(s) if cannot load
        runnable = () -> {
            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_id_timeout);
            if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                dismissLoadingDialog();
            }
            if (interCallback != null) {
                isLoadInterSplashIdTimeout = true;
                if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
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
            removeHandlerSplashAds();
        };
        handlerTimeoutSplash.postDelayed(runnable, timeOutCallSplashAds);

        //delay ads splash
        timerDelayRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Đã đủ 7 giây đếm ngược.");
                isTimerDelayFinished = true;
                checkConditionAdsSplash(activity, interCallback, isConfigShowNativeAfterInter, isEmptyListNativeAfterInter);
            }
        };
        handlerDelayAdsSplash.postDelayed(timerDelayRunnable, timeDelayAdsSplash);
        //end

        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds /*|| IAPManager.getInstance().isPurchase()*/) {
            Log.d(TAG, "Check condition loadAndShowInterAdSplash " + NetworkUtil.isNetworkActive(activity) + "_" + listIdInterTemp.isEmpty() + "_" + AdsConsentManager.getConsentResult(activity) + "_" + isShowAllAds + "_" /*+ IAPManager.getInstance().isPurchase()*/);
            interCallback.onNextAction();
            removeHandlerSplashAds();
            Bundle bundle = new Bundle();
            bundle.putString("failed_message", "lib_internet_" + NetworkUtil.isNetworkActive(activity)
                    + "_Consent_" + AdsConsentManager.getConsentResult(activity)
                    + "_isShowAllAds_" + isShowAllAds
            );
            EventTrackingHelper.logEventWithMultipleParams(activity, "splash_delay_failed", bundle);
            return;
        }

        //Log event
        Bundle bundle = new Bundle();
        boolean idCheck = AdmobApi.getInstance().getListAdsSize() > 0;
        bundle.putString(EventTrackingHelper.splash_detail, AdsConsentManager.getConsentResult(activity) + "_" + TechManager.getInstance().isTech(activity) + "_" + NetworkUtil.isNetworkActive(activity) + "_" + getShowAllAds() + "_" + idCheck + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        bundle.putString(EventTrackingHelper.ump, String.valueOf(AdsConsentManager.getConsentResult(activity)));
        bundle.putString(EventTrackingHelper.organic, String.valueOf(TechManager.getInstance().isTech(activity)));
        bundle.putString(EventTrackingHelper.haveinternet, String.valueOf(NetworkUtil.isNetworkActive(activity)));
        bundle.putString(EventTrackingHelper.showallad, String.valueOf(getShowAllAds()));
        bundle.putString(EventTrackingHelper.idcheck, String.valueOf(idCheck));
        bundle.putString(EventTrackingHelper.interremote + "_" + EventTrackingHelper.openremote + "_" + EventTrackingHelper.aoavalue, RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.inter_splash) + "_" + RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.open_splash) + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        EventTrackingHelper.logEventWithMultipleParams(activity, EventTrackingHelper.inter_splash_tracking, bundle);

        //log event can request
        EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_true);
        //end log event can request
        //time start load splash ads
        timeSplashLoadingAdShow = System.currentTimeMillis();

        InterstitialAd.load(new AdRequest.Builder(listIdInterTemp.get(0)).build(),
                new AdLoadCallback<InterstitialAd>() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        Log.i(TAG, "SPLASH: Ad was loaded inter splash.");
                        EventTrackingHelper.logEvent(activity, "splash_delay_true");
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
                        EventTrackingHelper.logEventWithMultipleParams(activity, "splash_delay_failed", bundle);
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
            removeHandlerDelayAdsSplash();
            return;
        }
        if (isTimerDelayFinished && isAdLoadAdsSplashFinished) {
            String timeFormatted = String.format(Locale.US, "%.2f", (System.currentTimeMillis() - startTime) / 1000.0);
            Log.d(TAG, "===> TỔNG THỜI GIAN CHỜ: " + timeFormatted + " giây");
            EventTrackingHelper.logEventWithAParam(activity, "Splash_time_wait", "time_to_step", timeFormatted);
            showInterAdsSplashDelay(activity, interCallback, isConfigShowNativeAfterInter, isEmptyListNativeAfterInter);
            removeHandlerDelayAdsSplash();
        }

    }

    public void removeHandlerDelayAdsSplash() {
        if (handlerDelayAdsSplash != null && timerDelayRunnable != null) {
            handlerDelayAdsSplash.removeCallbacks(timerDelayRunnable);
            handlerDelayAdsSplash.removeCallbacksAndMessages(null);
        }
    }

    public void loadAndShowInterAdSplashLoop(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback) {
        Log.d(TAG, "SPLASH: loadAndShowInterAdSplashLoop. " + listIdInter.toString());
        //Set timeout ads splash x(s) if cannot load
        runnable = () -> {
            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_id_timeout);
            if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                dismissLoadingDialog();
            }
            if (interCallback != null) {
                isLoadInterSplashIdTimeout = true;
                interCallback.onNextAction();
            }
            removeHandlerSplashAds();
        };
        handlerTimeoutSplash.postDelayed(runnable, timeOutCallSplashAds);
        // Check list id size
        if (listIdInter.isEmpty()) {
            Log.d(TAG, "SPLASH: loadAndShowInterAdSplashLoop: listIdInter is empty.");
            interCallback.onNextAction();
            removeHandlerSplashAds();
            return;
        }
        String idInterSplash = listIdInter.get(0);

        // If have action startActivity by timeout or no internet in splash, do not load ads.
        if (System.currentTimeMillis() - Admob.getInstance().getTimeStart() >= 8000 || AsyncSplash.Companion.getInstance().isTimeout() || AsyncSplash.Companion.getInstance().isNoInternetAction()) {
            Bundle bundle = new Bundle();
            bundle.putString("failed_message", "time_out_lib");
            EventTrackingHelper.logEventWithMultipleParams(activity, "splash_loop_failed", bundle);
            Log.d(TAG,
                    "SPLASH: If have action startActivity by timeout or no internet in splash, do not load ads. " + (System.currentTimeMillis() - Admob.getInstance().getTimeStart() >= 8000) + "_" + AsyncSplash.Companion.getInstance().isTimeout() + "_" + AsyncSplash.Companion.getInstance().isNoInternetAction());
            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_id_timeout_8s);
            interCallback.onNextAction();
            removeHandlerSplashAds();
            return;
        }

        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || idInterSplash.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds /*|| IAPManager.getInstance().isPurchase()*/) {
            Log.d(TAG, "SPLASH: Check condition loadAndShowInterAdSplash. Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + idInterSplash.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" /*+ IAPManager.getInstance().isPurchase()*/);
            Bundle bundle = new Bundle();
            bundle.putString("failed_message", "lib_internet_" + NetworkUtil.isNetworkActive(activity)
                    + "_Consent_" + AdsConsentManager.getConsentResult(activity)
                    + "_isShowAllAds_" + isShowAllAds
            );
            EventTrackingHelper.logEventWithMultipleParams(activity, "splash_loop_failed", bundle);
            interCallback.onNextAction();
            removeHandlerSplashAds();
            return;
        }

        //Log event
        Bundle bundle = new Bundle();
        boolean idCheck = AdmobApi.getInstance().getListAdsSize() > 0;
        bundle.putString(EventTrackingHelper.splash_detail, AdsConsentManager.getConsentResult(activity) + "_" + TechManager.getInstance().isTech(activity) + "_" + NetworkUtil.isNetworkActive(activity) + "_" + getShowAllAds() + "_" + idCheck + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        bundle.putString(EventTrackingHelper.ump, String.valueOf(AdsConsentManager.getConsentResult(activity)));
        bundle.putString(EventTrackingHelper.organic, String.valueOf(TechManager.getInstance().isTech(activity)));
        bundle.putString(EventTrackingHelper.haveinternet, String.valueOf(NetworkUtil.isNetworkActive(activity)));
        bundle.putString(EventTrackingHelper.showallad, String.valueOf(getShowAllAds()));
        bundle.putString(EventTrackingHelper.idcheck, String.valueOf(idCheck));
        bundle.putString(EventTrackingHelper.interremote + "_" + EventTrackingHelper.openremote + "_" + EventTrackingHelper.aoavalue, RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.inter_splash) + "_" + RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.open_splash) + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        EventTrackingHelper.logEventWithMultipleParams(activity, EventTrackingHelper.inter_splash_tracking, bundle);

        //log event can request
        EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_true);
        //end log event can request
        //time start load splash ads
        timeSplashLoadingAdShow = System.currentTimeMillis();

        EventTrackingHelper.logEvent(activity, "splash_loop_call");
        InterstitialAd.load(new AdRequest.Builder(idInterSplash).build(),
                new AdLoadCallback<InterstitialAd>() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        Log.i(TAG, "SPLASH: Ad was loaded inter splash loop. " + idInterSplash);
                        interCallback.onAdLoaded(interstitialAd);
                        mInterstitialAdSplash = interstitialAd;
                        EventTrackingHelper.logEvent(activity, "splash_loop_true");
                        showInterAdsSplash(activity, interCallback);
                        removeHandlerSplashAds();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Bundle bundle = new Bundle();
                        bundle.putString("failed_message", "load_" + loadAdError.getMessage());
                        EventTrackingHelper.logEventWithMultipleParams(activity, "splash_loop_failed", bundle);
                        Log.e(TAG, "SPLASH: Ad Failed To Load." + loadAdError);
                        interCallback.onAdFailedToLoad();
                        loadAndShowInterAdSplashLoop(activity, listIdInter, interCallback);
                    }
                });
    }

    public void onCheckShowSplashWhenFail(AppCompatActivity activity, InterCallback interCallback) {
        if (isFailToShowAdSplash) {
            Log.d(TAG, "SPLASH: onCheckShowSplashWhenFail.");
            if (!AsyncSplash.Companion.getInstance().getLoadAndShowIdInterAdSplashAsync()) {
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

    //================================end inter ads================================

    //================================Start banner ads================================
    public AdView loadBannerAdsBackupWithoutShow(Activity activity, List<String> listIdBanner, BannerCallback bannerCallback, String remoteKey) {
        ArrayList<String> listIdBannerTemp = new ArrayList<>(listIdBanner);
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdBannerTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "BANNER: Check condition: RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdBannerTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            bannerCallback.onAdFailedToLoad();
            return null;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "backup_true");
        //end log event can request ads
        // [START create_ad_view]
        // Create a new ad view.
        adViewBanner = new AdView(activity);
        AdSize adSize = getAdSize(activity);
        BannerAdRequest adRequest = new BannerAdRequest.Builder(listIdBannerTemp.get(0), adSize).build();

        adViewBanner.loadAd(
                adRequest,
                new AdLoadCallback<BannerAd>() {
                    @Override
                    public void onAdLoaded(BannerAd ad) {
                        Log.i(TAG, "BANNER: onAdLoaded. " + remoteKey);

                        //DetectTestAd
                        //Reset TechManager to false
                        if (AsyncSplash.Companion.getInstance().getUseTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)) {
                            TechManager.getInstance().detectedTech(activity, false);
                        }
                        if ((remoteKey.toLowerCase().trim().equals("banner_splash") || remoteKey.toLowerCase().trim().equals("banner_setting"))
                                && !AsyncSplash.Companion.getInstance().isDebug()
                                && AsyncSplash.Companion.getInstance().getUseTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)
                        ) {
                            boolean isTestAd = detectTestAd(adViewBanner);
                            Log.d(TAG, "BANNER: onAdImpression. isTestAd: " + isTestAd);
                            TechManager.getInstance().detectedTech(activity, isTestAd);

                            if (AsyncSplash.Companion.getInstance().getUseTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)
                                    && TechManager.getInstance().isTech(activity)
                                    && !AsyncSplash.Companion.getInstance().isDebug()) {
                                AsyncSplash.Companion.getInstance().turnOffSomeRemoteKeys(activity);
                            }
                        }
                        bannerCallback.onAdLoaded();

                        ad.setAdEventCallback(new BannerAdEventCallback() {
                            @Override
                            public void onAdClicked() {
                                AppOpenManager.isLastActionClickAd = true;
                                Log.d(TAG, "BANNER: onAdClicked. " + remoteKey);
                                EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                                bannerCallback.onAdClicked();
                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                BannerAdEventCallback.super.onAdDismissedFullScreenContent();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                                BannerAdEventCallback.super.onAdFailedToShowFullScreenContent(fullScreenContentError);
                            }

                            @Override
                            public void onAdImpression() {
                                Log.d(TAG, "BANNER: onAdImpression. " + remoteKey);
                                EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                                bannerCallback.onAdImpression();
                            }

                            @Override
                            public void onAdPaid(@NonNull AdValue value) {
                                //Tracking revenue
                                AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                BannerAdEventCallback.super.onAdShowedFullScreenContent();
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        Log.e(TAG, "BANNER: onAdFailedToLoad. " + adError + ". " + remoteKey);
                        bannerCallback.onAdFailedToLoad();
                        if (!listIdBannerTemp.isEmpty()) {
                            listIdBannerTemp.remove(0);
                        }
                        loadBannerAdsBackupWithoutShow(activity, listIdBannerTemp, bannerCallback, remoteKey);
                    }
                });
        // [END load_ad]
        return adViewBanner;
    }

    public AdView loadBannerAdsWithoutShow(Activity activity, List<String> listIdBanner, BannerCallback bannerCallback, String remoteKey) {
        ArrayList<String> listIdBannerTemp = new ArrayList<>(listIdBanner);
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdBannerTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "BANNER: Check condition: RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdBannerTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            bannerCallback.onAdFailedToLoad();
            return null;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads
        // [START create_ad_view]
        // Create a new ad view.
        adViewBanner = new AdView(activity);
        AdSize adSize = getAdSize(activity);
        BannerAdRequest adRequest = new BannerAdRequest.Builder(listIdBannerTemp.get(0), adSize).build();

        adViewBanner.loadAd(
                adRequest,
                new AdLoadCallback<BannerAd>() {
                    @Override
                    public void onAdLoaded(BannerAd ad) {
                        Log.i(TAG, "BANNER: onAdLoaded. " + remoteKey);

                        //DetectTestAd
                        //Reset TechManager to false
                        if (AsyncSplash.Companion.getInstance().getUseTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)) {
                            TechManager.getInstance().detectedTech(activity, false);
                        }
                        if ((remoteKey.toLowerCase().trim().equals("banner_splash") || remoteKey.toLowerCase().trim().equals("banner_setting"))
                                && !AsyncSplash.Companion.getInstance().isDebug()
                                && AsyncSplash.Companion.getInstance().getUseTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)
                        ) {
                            boolean isTestAd = detectTestAd(adViewBanner);
                            Log.d(TAG, "BANNER: onAdImpression. isTestAd: " + isTestAd);
                            TechManager.getInstance().detectedTech(activity, isTestAd);

                            if (AsyncSplash.Companion.getInstance().getUseTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)
                                    && TechManager.getInstance().isTech(activity)
                                    && !AsyncSplash.Companion.getInstance().isDebug()) {
                                AsyncSplash.Companion.getInstance().turnOffSomeRemoteKeys(activity);
                            }
                        }
                        bannerCallback.onAdLoaded();

                        ad.setAdEventCallback(new BannerAdEventCallback() {
                            @Override
                            public void onAdClicked() {
                                AppOpenManager.isLastActionClickAd = true;
                                Log.d(TAG, "BANNER: onAdClicked. " + remoteKey);
                                EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                                bannerCallback.onAdClicked();
                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                Log.i(TAG, "BANNER: onAdDismissedFullScreenContent. " + remoteKey);
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                                Log.i(TAG, "BANNER: onAdFailedToShowFullScreenContent. " + remoteKey);
                            }

                            @Override
                            public void onAdImpression() {
                                Log.d(TAG, "BANNER: onAdImpression. " + remoteKey);
                                EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                                bannerCallback.onAdImpression();
                            }

                            @Override
                            public void onAdPaid(@NonNull AdValue value) {
                                //Tracking revenue
                                AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                Log.i(TAG, "BANNER: onAdShowedFullScreenContent. " + remoteKey);
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        Log.e(TAG, "BANNER: onAdFailedToLoad. " + adError + ". " + remoteKey);
                        bannerCallback.onAdFailedToLoad();
                        if (!listIdBannerTemp.isEmpty()) {
                            listIdBannerTemp.remove(0);
                        }
                        loadBannerAdsWithoutShow(activity, listIdBannerTemp, bannerCallback, remoteKey);
                    }
                });
        // [END load_ad]
        return adViewBanner;
    }

    private void destroyBanner(AdView adView) {
        if (adView != null) {
            // Remove banner from view hierarchy.
            if (adView.getParent() instanceof ViewGroup) {
                ((ViewGroup) adView.getParent()).removeView(adView);
            }
            // Destroy the banner ad resources.
            adView.destroy();
            // Drop reference to the banner ad.
            adView = null;
        }
    }

    public void loadBannerAds(Activity activity, List<String> listIdBanner, FrameLayout adContainerView, BannerCallback bannerCallback, IOnAdsImpression iOnAdsImpression, String remoteKey) {
        destroyBanner(adViewBanner);
        ArrayList<String> listIdBannerTemp = new ArrayList<>(listIdBanner);
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        if (!isInitAdmobDone) {
            bannerCallback.onAdFailedToLoad();
            return;
        }
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdBannerTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "BANNER: Check condition: RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdBannerTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            bannerCallback.onAdFailedToLoad();
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads
        //Show loading shimmer
        View shimmerBanner = LayoutInflater.from(activity).inflate(R.layout.layout_shimmer_banner, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerBanner);
        }
        // [START create_ad_view]
        // Create a new ad view.
        adViewBanner = new AdView(activity);
        if (adContainerView != null) {
            adContainerView.addView(adViewBanner);
        }
        AdSize adSize = getAdSize(activity);
        BannerAdRequest adRequest = new BannerAdRequest.Builder(listIdBannerTemp.get(0), adSize).build();

        adViewBanner.loadAd(
                adRequest,
                new AdLoadCallback<BannerAd>() {
                    @Override
                    public void onAdLoaded(@NonNull BannerAd bannerAd) {
                        Log.i(TAG, "BANNER: onAdLoaded. " + remoteKey);
                        // Replace ad container with new ad view.
                        if (adContainerView != null) {
                            adContainerView.removeAllViews();
                            adContainerView.addView(adViewBanner);
                        }

                        //DetectTestAd
                        //Reset TechManager to false
                        if (AsyncSplash.Companion.getInstance().getUseTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)) {
                            TechManager.getInstance().detectedTech(activity, false);
                        }
                        Log.d(TAG, "BANNER: onAdLoaded: " + (remoteKey.toLowerCase().trim().equals("banner_splash") || remoteKey.toLowerCase().trim().equals("banner_setting"))
                                + " && " + !AsyncSplash.Companion.getInstance().isDebug()
                                + " && " + AsyncSplash.Companion.getInstance().getUseTechManagerOrDetectTestAd().equals(DETECT_TEST_AD));
                        if ((remoteKey.toLowerCase().trim().equals("banner_splash") || remoteKey.toLowerCase().trim().equals("banner_setting"))
                                && !AsyncSplash.Companion.getInstance().isDebug()
                                && AsyncSplash.Companion.getInstance().getUseTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)
                        ) {
                            boolean isTestAd = detectTestAd(adViewBanner);
                            Log.d(TAG, "BANNER: onAdImpression. isTestAd: " + isTestAd);
                            TechManager.getInstance().detectedTech(activity, isTestAd);

                            if (AsyncSplash.Companion.getInstance().getUseTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)
                                    && TechManager.getInstance().isTech(activity)
                                    && !AsyncSplash.Companion.getInstance().isDebug()) {
                                AsyncSplash.Companion.getInstance().turnOffSomeRemoteKeys(activity);
                            }
                        }
                        bannerCallback.onAdLoaded();

                        //callback
                        bannerAd.setAdEventCallback(
                                new BannerAdEventCallback() {
                                    @Override
                                    public void onAdPaid(@NonNull AdValue value) {
                                        BannerAdEventCallback.super.onAdPaid(value);
                                        Log.d(TAG, "BANNER: onAdPaid. " + remoteKey);
                                        //Adjust
                                        AdjustUtil.trackRevenue(bannerAd.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
                                    }

                                    @Override
                                    public void onAdImpression() {
                                        Log.d(TAG, "BANNER: onAdImpression. " + remoteKey);
                                        EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                                        bannerCallback.onAdImpression();
                                        //use for auto reload banner after x seconds
                                        iOnAdsImpression.onAdsImpression();
                                    }

                                    @Override
                                    public void onAdClicked() {
                                        AppOpenManager.isLastActionClickAd = true;
                                        Log.d(TAG, "BANNER: onAdClicked. " + remoteKey);
                                        EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                                        bannerCallback.onAdClicked();
                                    }

                                    @Override
                                    public void onAdShowedFullScreenContent() {
                                        // Banner ad showed.
                                        Log.d(TAG, "Banner ad showed full screen content. " + remoteKey);
                                    }

                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        // Banner ad dismissed.
                                        Log.d(TAG, "Banner ad dismissed full screen content. " + remoteKey);
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(
                                            @NonNull FullScreenContentError fullScreenContentError) {
                                        // Banner ad failed to show.
                                        Log.w(TAG, "Banner ad failed to show full screen content: " + fullScreenContentError + ". " + remoteKey);
                                    }
                                });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Log.e(TAG, "BANNER: onAdFailedToLoad. " + adError + ". " + remoteKey);
                            bannerCallback.onAdFailedToLoad();
                            if (!listIdBannerTemp.isEmpty()) {
                                listIdBannerTemp.remove(0);
                            }
                            loadBannerAds(activity, listIdBannerTemp, adContainerView, bannerCallback, iOnAdsImpression, remoteKey);
                        });
                    }
                });
    }

    private boolean detectTestAd(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View viewChild = viewGroup.getChildAt(i);
            if (viewChild instanceof ViewGroup) {
                if (detectTestAd((ViewGroup) viewChild))
                    return true;
            }
            if (viewChild instanceof TextView) {
                return true;
            }
        }
        return false;
    }

    //can load banner ads in fragment
    public void loadBannerAds(Context context, int adWidth, List<String> listIdBanner, FrameLayout adContainerView, BannerCallback bannerCallback, IOnAdsImpression iOnAdsImpression, String remoteKey) {
        destroyBanner(adViewBannerFragment);
        ArrayList<String> listIdBannerTemp = new ArrayList<>(listIdBanner);
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check condition
        if (!NetworkUtil.isNetworkActive(context) || listIdBannerTemp.isEmpty() || !AdsConsentManager.getConsentResult(context) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(context, remoteKey)) {
            Log.d(TAG, "BANNER: Check condition: RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(context) + "_IdEmpty:" + listIdBannerTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(context) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(context, remoteKey));
            bannerCallback.onAdFailedToLoad();
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(context, remoteKey + "_true");
        //end log event can request ads
        //Show loading shimmer
        View shimmerBanner = LayoutInflater.from(context).inflate(R.layout.layout_shimmer_banner, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerBanner);
        }
        // [START create_ad_view]
        // Create a new ad view.
        adViewBannerFragment = new AdView(context);
        if (adContainerView != null) {
            adContainerView.addView(adViewBannerFragment);
        }
        AdSize adSize = getAdSizeFragment(context, adWidth);
        BannerAdRequest adRequest = new BannerAdRequest.Builder(listIdBannerTemp.get(0), adSize).build();

        adViewBannerFragment.loadAd(
                adRequest,
                new AdLoadCallback<BannerAd>() {
                    @Override
                    public void onAdLoaded(@NonNull BannerAd bannerAd) {
                        Log.i(TAG, "BANNER: onAdLoaded. " + remoteKey);
                        // Replace ad container with new ad view.
                        if (adContainerView != null) {
                            adContainerView.removeAllViews();
                            adContainerView.addView(adViewBanner);
                        }

                        //DetectTestAd
                        //Reset TechManager to false
                        if (AsyncSplash.Companion.getInstance().getUseTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)) {
                            TechManager.getInstance().detectedTech(context, false);
                        }
                        if ((remoteKey.toLowerCase().trim().equals("banner_splash") || remoteKey.toLowerCase().trim().equals("banner_setting"))
                                && !AsyncSplash.Companion.getInstance().isDebug()
                                && AsyncSplash.Companion.getInstance().getUseTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)
                        ) {
                            boolean isTestAd = detectTestAd(adViewBannerFragment);
                            Log.d(TAG, "BANNER: onAdImpression. isTestAd: " + isTestAd);
                            TechManager.getInstance().detectedTech(context, isTestAd);

                            if (AsyncSplash.Companion.getInstance().getUseTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)
                                    && TechManager.getInstance().isTech(context)
                                    && !AsyncSplash.Companion.getInstance().isDebug()) {
                                AsyncSplash.Companion.getInstance().turnOffSomeRemoteKeys(context);
                            }
                        }
                        bannerCallback.onAdLoaded();

                        //callback
                        bannerAd.setAdEventCallback(
                                new BannerAdEventCallback() {
                                    @Override
                                    public void onAdPaid(@NonNull AdValue value) {
                                        BannerAdEventCallback.super.onAdPaid(value);
                                        Log.d(TAG, "BANNER: onAdPaid. " + remoteKey);
                                        //Adjust
                                        AdjustUtil.trackRevenue(bannerAd.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
                                    }

                                    @Override
                                    public void onAdImpression() {
                                        Log.d(TAG, "BANNER: onAdImpression. " + remoteKey);
                                        EventTrackingHelper.logEvent(context, remoteKey + "_view");
                                        bannerCallback.onAdImpression();
                                        //use for auto reload banner after x seconds
                                        iOnAdsImpression.onAdsImpression();
                                    }

                                    @Override
                                    public void onAdClicked() {
                                        AppOpenManager.isLastActionClickAd = true;
                                        Log.d(TAG, "BANNER: onAdClicked. " + remoteKey);
                                        EventTrackingHelper.logEvent(context, remoteKey + "_click");
                                        bannerCallback.onAdClicked();
                                    }

                                    @Override
                                    public void onAdShowedFullScreenContent() {
                                        // Banner ad showed.
                                        Log.d(TAG, "Banner ad showed full screen content. " + remoteKey);
                                    }

                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        // Banner ad dismissed.
                                        Log.d(TAG, "Banner ad dismissed full screen content. " + remoteKey);
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(
                                            @NonNull FullScreenContentError fullScreenContentError) {
                                        // Banner ad failed to show.
                                        Log.w(TAG, "Banner ad failed to show full screen content: " + fullScreenContentError + ". " + remoteKey);
                                    }
                                });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        Log.e(TAG, "BANNER: onAdFailedToLoad. " + adError + ". " + remoteKey);
                        bannerCallback.onAdFailedToLoad();
                        if (!listIdBannerTemp.isEmpty()) {
                            listIdBannerTemp.remove(0);
                        }
                        loadBannerAds(context, adWidth, listIdBannerTemp, adContainerView, bannerCallback, iOnAdsImpression, remoteKey);
                    }
                });
    }
    //end can load banner ads in fragment
    //================================End banner ads================================

    //================================Start collapse banner ads================================
    public AdView loadCollapseBanner(AppCompatActivity activity, List<String> listIdCollapseBanner, FrameLayout adContainerView, boolean isGravityBottom, BannerCallback bannerCallback, IOnAdsImpression iOnAdsImpression, IOnAdsFailToLoad iOnAdsFailToLoad, String collapseTypeClose, long valueCountDownOrCountClick, String remoteKey) {
        ArrayList<String> listIdCollapseBannerTemp = new ArrayList<>(listIdCollapseBanner);
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdCollapseBannerTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "COLLAPSE BANNER: Check condition. RemoteKey:" + remoteKey + "_Network: " + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdCollapseBannerTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            bannerCallback.onAdFailedToLoad();
            return null;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        //Show loading shimmer
        View shimmerBanner = LayoutInflater.from(activity).inflate(R.layout.layout_shimmer_banner, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerBanner);
        }
        AdView adView = new AdView(activity);
        AdSize adSize = getAdSize(activity);
        // Create an extra parameter that aligns the bottom of the expanded ad to
        // the bottom of the bannerView.
        Bundle extras = new Bundle();
        if (isGravityBottom) {
            extras.putString("collapsible", "bottom");
        } else {
            extras.putString("collapsible", "top");
        }
        BannerAdRequest bannerAdRequest = new BannerAdRequest.Builder(listIdCollapseBannerTemp.get(0), adSize)
                .setGoogleExtrasBundle(extras)
                .build();

        adView.loadAd(
                bannerAdRequest,
                new AdLoadCallback<BannerAd>() {
                    @Override
                    public void onAdLoaded(@NonNull BannerAd bannerAd) {
                        Log.i(TAG, "COLLAPSE BANNER: onAdLoaded. " + remoteKey);
                        bannerCallback.onAdLoaded();
                        // Replace ad container with new ad view.
                        if (adContainerView != null) {
                            activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
                                @Override
                                public void onResume(@NonNull LifecycleOwner owner) {
                                    DefaultLifecycleObserver.super.onResume(owner);
                                    adContainerView.removeAllViews();
                                    adContainerView.addView(adView);
                                }
                            });
                        }

                        //callback
                        bannerAd.setAdEventCallback(
                                new BannerAdEventCallback() {
                                    @Override
                                    public void onAdPaid(@NonNull AdValue value) {
                                        BannerAdEventCallback.super.onAdPaid(value);
                                        Log.d(TAG, "COLLAPSE BANNER: onAdPaid. " + remoteKey);
                                        //Adjust
                                        AdjustUtil.trackRevenue(bannerAd.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
                                    }

                                    @Override
                                    public void onAdImpression() {
                                        Log.d(TAG, "COLLAPSE BANNER: onAdImpression. " + remoteKey);
                                        EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                                        bannerCallback.onAdImpression();
                                        iOnAdsImpression.onAdsImpression();
                                    }

                                    @Override
                                    public void onAdClicked() {
                                        AppOpenManager.isLastActionClickAd = true;
                                        Log.d(TAG, "COLLAPSE BANNER: onAdClicked. " + remoteKey);
                                        EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                                        bannerCallback.onAdClicked();
                                    }

                                    @Override
                                    public void onAdShowedFullScreenContent() {
                                        // Banner ad showed.
                                        Log.d(TAG, "COLLAPSE BANNER: Banner ad showed full screen content. " + remoteKey);
                                        applyTechForCollapseBanner(collapseTypeClose, valueCountDownOrCountClick);
                                    }

                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        // Banner ad dismissed.
                                        Log.d(TAG, "COLLAPSE BANNER: Banner ad dismissed full screen content. " + remoteKey);
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(
                                            @NonNull FullScreenContentError fullScreenContentError) {
                                        // Banner ad failed to show.
                                        Log.w(TAG, "COLLAPSE BANNER: Banner ad failed to show full screen content: " + fullScreenContentError + ". " + remoteKey);
                                    }
                                });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        Log.e(TAG, "COLLAPSE BANNER: onAdFailedToLoad. " + adError + ". " + remoteKey);
                        bannerCallback.onAdFailedToLoad();
                        iOnAdsFailToLoad.onAdsFailToLoad();
                        if (!listIdCollapseBannerTemp.isEmpty()) {
                            listIdCollapseBannerTemp.remove(0);
                        }
                        loadCollapseBanner(activity, listIdCollapseBannerTemp, adContainerView, isGravityBottom, bannerCallback, iOnAdsImpression, iOnAdsFailToLoad, collapseTypeClose, valueCountDownOrCountClick, remoteKey);
                    }
                });
        return adView;
    }

    public AdView loadCollapseBanner(Context context, int adWidth, List<String> listIdCollapseBanner, FrameLayout adContainerView, boolean isGravityBottom, BannerCallback bannerCallback, IOnAdsImpression iOnAdsImpression, IOnAdsFailToLoad iOnAdsFailToLoad, String collapseTypeClose, long valueCountDownOrCountClick, String remoteKey) {
        ArrayList<String> listIdCollapseBannerTemp = new ArrayList<>(listIdCollapseBanner);
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check condition
        if (!NetworkUtil.isNetworkActive(context) || listIdCollapseBannerTemp.isEmpty() || !AdsConsentManager.getConsentResult(context) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(context, remoteKey)) {
            Log.d(TAG, "COLLAPSE BANNER: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(context) + "_IdEmpty:" + listIdCollapseBannerTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(context) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(context, remoteKey));
            bannerCallback.onAdFailedToLoad();
            return null;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(context, remoteKey + "_true");
        //end log event can request ads

        //Show loading shimmer
        View shimmerBanner = LayoutInflater.from(context).inflate(R.layout.layout_shimmer_banner, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerBanner);
        }
        AdView adView = new AdView(context);
        AdSize adSize = getAdSizeFragment(context, adWidth);
        // Create an extra parameter that aligns the bottom of the expanded ad to
        // the bottom of the bannerView.
        Bundle extras = new Bundle();
        if (isGravityBottom) {
            extras.putString("collapsible", "bottom");
        } else {
            extras.putString("collapsible", "top");
        }

        BannerAdRequest bannerAdRequest = new BannerAdRequest.Builder(listIdCollapseBannerTemp.get(0), adSize)
                .setGoogleExtrasBundle(extras)
                .build();

        adView.loadAd(
                bannerAdRequest,
                new AdLoadCallback<BannerAd>() {
                    @Override
                    public void onAdLoaded(@NonNull BannerAd bannerAd) {
                        Log.i(TAG, "COLLAPSE BANNER: onAdLoaded. " + remoteKey);
                        bannerCallback.onAdLoaded();
                        // Replace ad container with new ad view.
                        if (adContainerView != null) {
                            /*((AppCompatActivity) context).getLifecycle().addObserver(new DefaultLifecycleObserver() {
                                @Override
                                public void onResume(@NonNull LifecycleOwner owner) {
                                    DefaultLifecycleObserver.super.onResume(owner);
                                    adContainerView.removeAllViews();
                                    adContainerView.addView(adView);
                                }
                            });*/
                            try {
                                adContainerView.removeAllViews();
                                adContainerView.addView(adView);
                            } catch (Exception e) {

                            }
                        }

                        //callback
                        bannerAd.setAdEventCallback(
                                new BannerAdEventCallback() {
                                    @Override
                                    public void onAdPaid(@NonNull AdValue value) {
                                        BannerAdEventCallback.super.onAdPaid(value);
                                        Log.d(TAG, "COLLAPSE BANNER: onAdPaid. " + remoteKey);
                                        //Adjust
                                        AdjustUtil.trackRevenue(bannerAd.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
                                    }

                                    @Override
                                    public void onAdImpression() {
                                        Log.d(TAG, "COLLAPSE BANNER: onAdImpression. " + remoteKey);
                                        EventTrackingHelper.logEvent(context, remoteKey + "_view");
                                        bannerCallback.onAdImpression();
                                        iOnAdsImpression.onAdsImpression();
                                    }

                                    @Override
                                    public void onAdClicked() {
                                        AppOpenManager.isLastActionClickAd = true;
                                        Log.d(TAG, "COLLAPSE BANNER: onAdClicked. " + remoteKey);
                                        EventTrackingHelper.logEvent(context, remoteKey + "_click");
                                        bannerCallback.onAdClicked();
                                    }

                                    @Override
                                    public void onAdShowedFullScreenContent() {
                                        // Banner ad showed.
                                        Log.d(TAG, "COLLAPSE BANNER: Banner ad showed full screen content. " + remoteKey);
                                        applyTechForCollapseBanner(collapseTypeClose, valueCountDownOrCountClick);
                                    }

                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        // Banner ad dismissed.
                                        Log.d(TAG, "COLLAPSE BANNER: Banner ad dismissed full screen content. " + remoteKey);
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(
                                            @NonNull FullScreenContentError fullScreenContentError) {
                                        // Banner ad failed to show.
                                        Log.w(TAG, "COLLAPSE BANNER: Banner ad failed to show full screen content: " + fullScreenContentError + ". " + remoteKey);
                                    }
                                });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        Log.e(TAG, "COLLAPSE BANNER: onAdFailedToLoad. " + adError + ". " + remoteKey);
                        bannerCallback.onAdFailedToLoad();
                        iOnAdsFailToLoad.onAdsFailToLoad();
                        if (!listIdCollapseBannerTemp.isEmpty()) {
                            listIdCollapseBannerTemp.remove(0);
                        }
                        loadCollapseBanner(context, adWidth, listIdCollapseBannerTemp, adContainerView, isGravityBottom, bannerCallback, iOnAdsImpression, iOnAdsFailToLoad, collapseTypeClose, valueCountDownOrCountClick, remoteKey);
                    }
                });
        return adView;
    }

    private void applyTechForCollapseBanner(String collapseTypeClose, long valueCountDownOrCountClick) {
        if (CollapseBannerHelper.getWindowManagerViews() != null) {
            Log.d("ApplyTechForCollapse", "run: " + CollapseBannerHelper.getWindowManagerViews().size());
            CollapseBannerHelper.listChildViews.clear();
            for (int i = 0; i < CollapseBannerHelper.getWindowManagerViews().size(); i++) {
                Object object = CollapseBannerHelper.getWindowManagerViews().get(i);
                if (object instanceof ViewGroup && ((ViewGroup) object).getClass().getName().contains("android.widget.PopupWindow")) {
                    Log.d("CollapseBannerHelper", "ViewGroup: " + object + "\n=================================================================");
                    if (collapseTypeClose.equals(CollapseBannerHelper.COUNT_DOWN)) {
                        CollapseBannerHelper.getAllChildViews((ViewGroup) object, collapseTypeClose, valueCountDownOrCountClick, object);
                    } else if (collapseTypeClose.equals(CollapseBannerHelper.COUNT_CLICK)) {
                        CollapseBannerHelper.getAllChildViews((ViewGroup) object, collapseTypeClose, valueCountDownOrCountClick, object);
                    }
                }
            }
        }
    }

    //================================End collapse banner ads================================

    //Get the ad size with screen width.
    public AdSize getAdSize(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);
        Log.d(TAG, "getAdSize: adWith = " + adWidth);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }

    public int getScreenWidth(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);
        Log.d(TAG, "getAdSize: adWith = " + adWidth);
        return adWidth;
    }

    // Get the ad size with screen width.
    public AdSize getAdSizeDocAdmob(Activity activity) {
        DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
        int adWidthPixels = displayMetrics.widthPixels;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
            adWidthPixels = windowMetrics.getBounds().width();
        }

        float density = displayMetrics.density;
        int adWidth = (int) (adWidthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }

    public int getScreenWidthDocAdmob(Activity activity) {
        DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
        int adWidthPixels = displayMetrics.widthPixels;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
            adWidthPixels = windowMetrics.getBounds().width();
        }

        float density = displayMetrics.density;
        int adWidth = (int) (adWidthPixels / density);
        Log.d(TAG, "getAdSize: adWith = " + adWidth);
        return adWidth;
    }

    public AdSize getAdSizeFragment(Context context, int adWidth) {
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth);
    }

    //================================Start native ads================================
    public void loadNativeAds(Context activity, List<String> listIdNative, NativeCallback nativeCallback, String remoteKey) {
        ArrayList<String> listIdNativeTemp = new ArrayList<>(listIdNative);
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdNativeTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "NATIVE: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdNativeTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            nativeCallback.onAdFailedToLoad("NATIVE: Check condition");
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(false)
                .build();
        List<NativeAd.NativeAdType> adTypes = Arrays.asList(NativeAd.NativeAdType.NATIVE);
        NativeAdRequest adRequest = new NativeAdRequest.Builder(listIdNativeTemp.get(0), adTypes)
                .setVideoOptions(videoOptions)
                .build();

        NativeAdLoader.load(adRequest, new NativeAdLoaderCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                Log.e(TAG, "NATIVE: onAdFailedToLoad. " + adError + ". " + remoteKey);
                Bundle bundle = new Bundle();
                bundle.putString("failed_message", limitString(adError.getMessage(), 99));
                if (adError.getResponseInfo() != null && adError.getResponseInfo().getLoadedAdSourceResponseInfo() != null && adError.getMessage().toLowerCase().contains("no fill")) {
                    bundle.putString("no_fill_source", limitString(adError.getResponseInfo().getLoadedAdSourceResponseInfo().getName(), 99));
                }
                EventTrackingHelper.logEventWithMultipleParams(activity, remoteKey + "_failed", bundle);
                nativeCallback.onAdFailedToLoad(adError.getMessage());
                if (!listIdNativeTemp.isEmpty()) {
                    listIdNativeTemp.remove(0);
                }
                loadNativeAds(activity, listIdNativeTemp, nativeCallback, remoteKey);
            }

            @Override
            public void onAdLoadingCompleted() {
                NativeAdLoaderCallback.super.onAdLoadingCompleted();
                Log.d(TAG, "NATIVE: onAdLoadingCompleted. " + remoteKey);
            }

            @Override
            public void onBannerAdLoaded(@NonNull BannerAd bannerAd) {
                NativeAdLoaderCallback.super.onBannerAdLoaded(bannerAd);
                Log.d(TAG, "NATIVE: onBannerAdLoaded. " + remoteKey);
            }

            @Override
            public void onCustomNativeAdLoaded(@NonNull CustomNativeAd customNativeAd) {
                NativeAdLoaderCallback.super.onCustomNativeAdLoaded(customNativeAd);
                Log.d(TAG, "NATIVE: onCustomNativeAdLoaded. " + remoteKey);
            }

            @Override
            public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                Log.i(TAG, "NATIVE: onAdLoaded. " + remoteKey);
                nativeCallback.onNativeAdLoaded(nativeAd);
                nativeAd.setAdEventCallback(new NativeAdEventCallback() {
                    @Override
                    public void onAdClicked() {
                        NativeAdEventCallback.super.onAdClicked();
                        nativeCallback.onAdClicked();
                        AppOpenManager.isLastActionClickAd = true;
                        Log.d(TAG, "NATIVE: onAdClicked. " + ". " + remoteKey);
                        EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        NativeAdEventCallback.super.onAdDismissedFullScreenContent();
                        Log.d(TAG, "NATIVE: onAdDismissedFullScreenContent. " + remoteKey);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                        NativeAdEventCallback.super.onAdFailedToShowFullScreenContent(fullScreenContentError);
                        Log.d(TAG, "NATIVE: onAdFailedToShowFullScreenContent. " + remoteKey);
                    }

                    @Override
                    public void onAdImpression() {
                        NativeAdEventCallback.super.onAdImpression();
                        nativeCallback.onAdImpression();
                        Log.d(TAG, "NATIVE: onAdImpression. " + remoteKey);
                        EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                    }

                    @Override
                    public void onAdPaid(@NonNull AdValue value) {
                        Log.d(TAG, "NATIVE: onAdPaid. " + remoteKey);
                        //Tracking revenue
                        AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        NativeAdEventCallback.super.onAdShowedFullScreenContent();
                        Log.d(TAG, "NATIVE: onAdShowedFullScreenContent. " + remoteKey);
                    }
                });
            }
        });
    }

    public void loadNativeAdsBackup(Context activity, List<String> listIdNative, NativeCallback nativeCallback, String remoteKey) {
        ArrayList<String> listIdNativeTemp = new ArrayList<>(listIdNative);
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdNativeTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "NATIVE: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdNativeTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            nativeCallback.onAdFailedToLoad("NATIVE: Check condition");
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_backup_true");
        //end log event can request ads

        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(false)
                .build();
        List<NativeAd.NativeAdType> adTypes = Arrays.asList(NativeAd.NativeAdType.NATIVE);
        NativeAdRequest adRequest = new NativeAdRequest.Builder(listIdNativeTemp.get(0), adTypes)
                .setVideoOptions(videoOptions)
                .build();

        NativeAdLoader.load(adRequest, new NativeAdLoaderCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                Log.e(TAG, "NATIVE: onAdFailedToLoad. " + adError + ". " + remoteKey);
                Bundle bundle = new Bundle();
                bundle.putString("failed_message", limitString(adError.getMessage(), 99));
                if (adError.getResponseInfo() != null && adError.getResponseInfo().getLoadedAdSourceResponseInfo() != null && adError.getMessage().toLowerCase().contains("no fill")) {
                    bundle.putString("no_fill_source", limitString(adError.getResponseInfo().getLoadedAdSourceResponseInfo().getName(), 99));
                }
                EventTrackingHelper.logEventWithMultipleParams(activity, remoteKey + "_failed", bundle);
                nativeCallback.onAdFailedToLoad(adError.getMessage());
                if (!listIdNativeTemp.isEmpty()) {
                    listIdNativeTemp.remove(0);
                }
                loadNativeAds(activity, listIdNativeTemp, nativeCallback, remoteKey);
            }

            @Override
            public void onAdLoadingCompleted() {
                NativeAdLoaderCallback.super.onAdLoadingCompleted();
                Log.d(TAG, "NATIVE: onAdLoadingCompleted. " + remoteKey);
            }

            @Override
            public void onBannerAdLoaded(@NonNull BannerAd bannerAd) {
                NativeAdLoaderCallback.super.onBannerAdLoaded(bannerAd);
                Log.d(TAG, "NATIVE: onBannerAdLoaded. " + remoteKey);
            }

            @Override
            public void onCustomNativeAdLoaded(@NonNull CustomNativeAd customNativeAd) {
                NativeAdLoaderCallback.super.onCustomNativeAdLoaded(customNativeAd);
                Log.d(TAG, "NATIVE: onCustomNativeAdLoaded. " + remoteKey);
            }

            @Override
            public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                Log.i(TAG, "NATIVE: onAdLoaded. " + remoteKey);
                nativeCallback.onNativeAdLoaded(nativeAd);
                nativeAd.setAdEventCallback(new NativeAdEventCallback() {
                    @Override
                    public void onAdClicked() {
                        NativeAdEventCallback.super.onAdClicked();
                        nativeCallback.onAdClicked();
                        AppOpenManager.isLastActionClickAd = true;
                        Log.d(TAG, "NATIVE: onAdClicked. " + ". " + remoteKey);
                        EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        NativeAdEventCallback.super.onAdDismissedFullScreenContent();
                        Log.d(TAG, "NATIVE: onAdDismissedFullScreenContent. " + remoteKey);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                        NativeAdEventCallback.super.onAdFailedToShowFullScreenContent(fullScreenContentError);
                        Log.d(TAG, "NATIVE: onAdFailedToShowFullScreenContent. " + remoteKey);
                    }

                    @Override
                    public void onAdImpression() {
                        NativeAdEventCallback.super.onAdImpression();
                        nativeCallback.onAdImpression();
                        Log.d(TAG, "NATIVE: onAdImpression. " + remoteKey);
                        EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                    }

                    @Override
                    public void onAdPaid(@NonNull AdValue value) {
                        Log.d(TAG, "NATIVE: onAdPaid. " + remoteKey);
                        //Tracking revenue
                        AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        NativeAdEventCallback.super.onAdShowedFullScreenContent();
                        Log.d(TAG, "NATIVE: onAdShowedFullScreenContent. " + remoteKey);
                    }
                });
            }
        });
    }

    public static String limitString(String str, int maxLength) {
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    public void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        // Set the media view.
        MediaView mediaView = adView.findViewById(R.id.ad_media);

        // Set other ad assets.
        View viewHeadline = adView.findViewById(R.id.ad_headline);
        if (viewHeadline != null) {
            adView.setHeadlineView(viewHeadline);
        }
        View bodyView = adView.findViewById(R.id.ad_body);
        if (bodyView != null) {
            adView.setBodyView(bodyView);
        }
        View callToActionView = adView.findViewById(R.id.ad_call_to_action);
        if (callToActionView != null) {
            adView.setCallToActionView(callToActionView);
        }
        View iconView = adView.findViewById(R.id.ad_app_icon);
        if (iconView != null) {
            adView.setIconView(iconView);
        }
        View priceView = adView.findViewById(R.id.ad_price);
        if (priceView != null) {
            adView.setPriceView(priceView);
        }
        View starRatingView = adView.findViewById(R.id.ad_stars);
        if (starRatingView != null) {
            adView.setStarRatingView(starRatingView);
        }
        View storeView = adView.findViewById(R.id.ad_store);
        if (storeView != null) {
            adView.setStoreView(storeView);
        }
        View advertiserView = adView.findViewById(R.id.ad_advertiser);
        if (advertiserView != null) {
            adView.setAdvertiserView(advertiserView);
        }

        // The headline and mediaContent are guaranteed to be in every NativeAd.
        if (adView.getHeadlineView() != null && nativeAd.getHeadline() != null) {
            ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        }
        if (adView.getMediaView() != null && nativeAd.getMediaContent() != null) {
            adView.getMediaView().setMediaContent(nativeAd.getMediaContent());
        }

        // These assets aren't guaranteed to be in every NativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.getBody() == null) {
            if (adView.getBodyView() != null)
                adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            if (adView.getBodyView() != null) {
                adView.getBodyView().setVisibility(View.VISIBLE);
                ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
            }
        }

        if (nativeAd.getCallToAction() == null) {
            if (adView.getCallToActionView() != null) {
                adView.getCallToActionView().setVisibility(View.INVISIBLE);
            }
        } else {
            if (adView.getCallToActionView() != null) {
                adView.getCallToActionView().setVisibility(View.VISIBLE);
                ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
            }
        }

        if (nativeAd.getIcon() == null) {
            if (adView.getIconView() != null) {
                adView.getIconView().setVisibility(View.GONE);
            }
        } else {
            if (adView.getIconView() != null) {
                ((ImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
                adView.getIconView().setVisibility(View.VISIBLE);
            }
        }

        if (nativeAd.getPrice() == null) {
            if (adView.getPriceView() != null)
                adView.getPriceView().setVisibility(View.INVISIBLE);
        } else {
            if (adView.getPriceView() != null) {
                adView.getPriceView().setVisibility(View.VISIBLE);
                ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
            }
        }

        if (nativeAd.getStore() == null) {
            if (adView.getStoreView() != null)
                adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            if (adView.getStoreView() != null) {
                adView.getStoreView().setVisibility(View.VISIBLE);
                ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
            }
        }

        if (nativeAd.getStarRating() == null) {
            if (adView.getStarRatingView() != null)
                adView.getStarRatingView().setVisibility(View.INVISIBLE);
        } else {
            if (adView.getStarRatingView() != null) {
                ((RatingBar) adView.getStarRatingView()).setRating(nativeAd.getStarRating().floatValue());
                adView.getStarRatingView().setVisibility(View.VISIBLE);
            }
        }

        if (nativeAd.getAdvertiser() == null) {
            if (adView.getAdvertiserView() != null)
                adView.getAdvertiserView().setVisibility(View.INVISIBLE);
        } else {
            if (adView.getAdvertiserView() != null) {
                ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
                adView.getAdvertiserView().setVisibility(View.VISIBLE);
            }
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        if (mediaView != null) {
            adView.registerNativeAd(nativeAd, mediaView);
        }
    }
    //================================End native ads================================

    //================================Start reward ads================================

    public void loadRewardAds(Activity activity, List<String> listIdRewarded, RewardedCallback rewardedCallback, String remoteKey) {
        ArrayList<String> listIdRewardedTemp = new ArrayList<>(listIdRewarded);
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdRewardedTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "REWARD: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdRewardedTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            activity.runOnUiThread(() -> {
                rewardedCallback.onAdFailedToLoad();
                rewardedCallback.onNextAction();
            });
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        RewardedAd.load(new AdRequest.Builder(listIdRewardedTemp.get(0)).build(),
                new AdLoadCallback<RewardedAd>() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "REWARD: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                        activity.runOnUiThread(() -> {
                            rewardedCallback.onAdFailedToLoad();
                        });
                        if (!listIdRewardedTemp.isEmpty()) {
                            listIdRewardedTemp.remove(0);
                        }
                        loadRewardAds(activity, listIdRewardedTemp, rewardedCallback, remoteKey);
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        Log.i(TAG, "REWARD: onAdLoaded. " + remoteKey);
                        activity.runOnUiThread(() -> {
                            rewardedCallback.onAdLoaded(ad);
                        });
                    }
                });
    }

    public void showReward(Activity activity, RewardedAd rewardedAd, RewardedCallback rewardedCallback, String remoteKey) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "REWARD: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            activity.runOnUiThread(() -> {
                rewardedCallback.onAdFailedToLoad();
                rewardedCallback.onNextAction();
            });
            return;
        }
        if (rewardedAd == null) {
            Log.d(TAG, "REWARD: The rewarded ad wasn't ready yet.");
            activity.runOnUiThread(() -> {
                rewardedCallback.onAdFailedToShowFullScreenContent();
                rewardedCallback.onNextAction();
            });
            return;
        }
        loadingAdsDialog = new LoadingAdsDialog(activity);
        if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
            loadingAdsDialog.show();
        }
        rewardedAd.setAdEventCallback(new RewardedAdEventCallback() {
            @Override
            public void onAdClicked() {
                RewardedAdEventCallback.super.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "REWARD: onAdClicked. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                activity.runOnUiThread(() -> {
                    rewardedCallback.onAdClicked();
                });
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                RewardedAdEventCallback.super.onAdDismissedFullScreenContent();
                Log.d(TAG, "REWARD: onAdDismissedFullScreenContent. " + remoteKey);
                activity.runOnUiThread(() -> {
                    rewardedCallback.onAdDismissedFullScreenContent();
                    rewardedCallback.onNextAction();
                });
                isInterOrRewardedShowing = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                RewardedAdEventCallback.super.onAdFailedToShowFullScreenContent(fullScreenContentError);
                Log.e(TAG, "REWARD: onAdFailedToShowFullScreenContent. " + remoteKey);
                activity.runOnUiThread(() -> {
                    rewardedCallback.onAdFailedToShowFullScreenContent();
                    rewardedCallback.onNextAction();
                });
                if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    dismissLoadingDialog();
                }
            }

            @Override
            public void onAdImpression() {
                RewardedAdEventCallback.super.onAdImpression();
                Log.d(TAG, "REWARD: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                activity.runOnUiThread(() -> {
                    rewardedCallback.onAdImpression();
                });
            }

            @Override
            public void onAdPaid(@NonNull AdValue value) {
                //Tracking revenue
                AdjustUtil.trackRevenue(rewardedAd.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
            }

            @Override
            public void onAdShowedFullScreenContent() {
                RewardedAdEventCallback.super.onAdShowedFullScreenContent();
                Log.d(TAG, "REWARD: onAdShowedFullScreenContent. " + remoteKey);
                activity.runOnUiThread(() -> {
                    rewardedCallback.onAdShowedFullScreenContent();
                });
                if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    dismissLoadingDialog();
                }
                isInterOrRewardedShowing = true;
            }
        });
        rewardedAd.show(activity, rewardItem -> {
            Log.d(TAG, "REWARD: The user earned the reward. " + remoteKey);
            activity.runOnUiThread(() -> {
                rewardedCallback.onUserEarnedReward();
            });
        });
    }

    public void loadAndShowRewardAds(Activity activity, List<String> listIdRewarded, RewardedCallback rewardedCallback, String remoteKey) {
        ArrayList<String> listIdRewardedTemp = new ArrayList<>(listIdRewarded);
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdRewardedTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "REWARD: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdRewardedTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            activity.runOnUiThread(() -> {
                rewardedCallback.onAdFailedToLoad();
                rewardedCallback.onNextAction();
            });
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        loadingAdsDialog = new LoadingAdsDialog(activity);
        if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
            loadingAdsDialog.show();
        }

        RewardedAd.load(new AdRequest.Builder(listIdRewardedTemp.get(0)).build(),
                new AdLoadCallback<RewardedAd>() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "REWARD: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                        activity.runOnUiThread(() -> {
                            rewardedCallback.onAdFailedToLoad();
                        });
                        if (!listIdRewardedTemp.isEmpty()) {
                            listIdRewardedTemp.remove(0);
                        }
                        if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                            dismissLoadingDialog();
                        }
                        loadRewardAds(activity, listIdRewardedTemp, rewardedCallback, remoteKey);
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        Log.i(TAG, "REWARD: onAdLoaded. " + remoteKey);
                        activity.runOnUiThread(() -> {
                            rewardedCallback.onAdLoaded(ad);
                        });
                        if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                            dismissLoadingDialog();
                        }
                        showRewardLoadAndShow(activity, ad, rewardedCallback, remoteKey);
                    }
                });
    }

    public void showRewardLoadAndShow(Activity activity, RewardedAd rewardedAd, RewardedCallback rewardedCallback, String remoteKey) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "REWARD: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            activity.runOnUiThread(() -> {
                rewardedCallback.onAdFailedToLoad();
                rewardedCallback.onNextAction();
            });
            return;
        }
        if (rewardedAd == null) {
            Log.d(TAG, "REWARD: The rewarded ad wasn't ready yet.");
            activity.runOnUiThread(() -> {
                rewardedCallback.onAdFailedToShowFullScreenContent();
                rewardedCallback.onNextAction();
            });
            return;
        }
        rewardedAd.setAdEventCallback(new RewardedAdEventCallback() {
            @Override
            public void onAdPaid(@NonNull AdValue value) {
                //Tracking revenue
                AdjustUtil.trackRevenue(rewardedAd.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
            }

            @Override
            public void onAdClicked() {
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "REWARD: onAdClicked. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                activity.runOnUiThread(() -> {
                    rewardedCallback.onAdClicked();
                });
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "REWARD: onAdDismissedFullScreenContent. " + remoteKey);
                activity.runOnUiThread(() -> {
                    rewardedCallback.onAdDismissedFullScreenContent();
                    rewardedCallback.onNextAction();
                });
                isInterOrRewardedShowing = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                RewardedAdEventCallback.super.onAdFailedToShowFullScreenContent(fullScreenContentError);
                Log.e(TAG, "REWARD: onAdFailedToShowFullScreenContent. " + remoteKey);
                activity.runOnUiThread(() -> {
                    rewardedCallback.onAdFailedToShowFullScreenContent();
                    rewardedCallback.onNextAction();
                });
                if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    dismissLoadingDialog();
                }
            }

            @Override
            public void onAdImpression() {
                Log.d(TAG, "REWARD: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                activity.runOnUiThread(() -> {
                    rewardedCallback.onAdImpression();
                });
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "REWARD: onAdShowedFullScreenContent. " + remoteKey);
                rewardedCallback.onAdShowedFullScreenContent();
                if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    dismissLoadingDialog();
                }
                isInterOrRewardedShowing = true;
            }
        });
        rewardedAd.show(activity, rewardItem -> {
            Log.d(TAG, "REWARD: The user earned the reward. " + remoteKey);
            activity.runOnUiThread(() -> {
                rewardedCallback.onUserEarnedReward();
            });
        });
    }

    //reward preload
    public void loadAndCheckRewardPreload(
            Activity activity,
            List<String> listIdRewarded,
            RewardedCallback rewardedCallback,
            String remoteKey
    ) {
        if (listIdRewarded.isEmpty()) {
            activity.runOnUiThread(() -> {
                rewardedCallback.onNextAction();
            });
            return;
        }

        if (RewardedAdPreloader.isAdAvailable(listIdRewarded.get(0))) {
            Log.d(TAG, "REWARD Ad Preload - loadAndShow: HAVE DATA");
            activity.runOnUiThread(() -> {
                rewardedCallback.onAdLoaded(null);
            });
        } else {
            Log.d(TAG, "REWARD Ad Preload - loadAndShow: NO DATA");
            ArrayList<String> listIdRewardedTemp = new ArrayList<>(listIdRewarded);
            //Check condition
            if (!NetworkUtil.isNetworkActive(activity) || listIdRewardedTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
                Log.d(TAG, "REWARD Ad Preload - loadAndShow: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdRewardedTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
                activity.runOnUiThread(() -> {
                    rewardedCallback.onAdFailedToLoad();
                    rewardedCallback.onNextAction();
                });
                return;
            }
            //log event can request ads
            EventTrackingHelper.logEvent(activity, remoteKey + "_true");
            //end log event can request ads

            loadingAdsDialog = new LoadingAdsDialog(activity);
            if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
                loadingAdsDialog.show();
            }
            AdRequest adRequest = new AdRequest.Builder(listIdRewarded.get(0)).build();
            PreloadConfiguration configuration = new PreloadConfiguration(adRequest, AsyncSplash.Companion.getInstance().getNumberPreloading());

            final AtomicBoolean isFirstLoadAd = new AtomicBoolean(true);

            PreloadCallback callback = new PreloadCallback() {
                @Override
                public void onAdFailedToPreload(@NonNull String preloadId, @NonNull LoadAdError adError) {
                    Log.d(TAG, "REWARD Ad Preload  - loadAndShow: Preload ad " + preloadId + " failed to load with error: " + adError.getMessage());
                    if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
                    }
                    activity.runOnUiThread(() -> {
                        rewardedCallback.onNextAction();
                    });
                }

                @Override
                public void onAdPreloaded(@NonNull String preloadId, @NonNull ResponseInfo responseInfo) {
                    Log.d(TAG, "REWARD Ad Preload  - loadAndShow: Preload ad for " + preloadId + " is available.");
                    if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
                    }

                    if (isFirstLoadAd.getAndSet(false)) {
                        activity.runOnUiThread(() -> {
                            rewardedCallback.onAdLoaded(null);
                        });
                    }
                }

                @Override
                public void onAdsExhausted(@NonNull String preloadId) {
                    Log.d(TAG, "REWARD Ad Preload  - loadAndShow: Preload ad for " + preloadId + " is exhausted.");
                }
            };

            RewardedAdPreloader.start(listIdRewarded.get(0), configuration, callback);
        }
    }

    public void loadRewardAdPreload(Activity activity, List<String> listIdRewarded, RewardedCallback rewardedCallback, String remoteKey) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdRewarded.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "REWARD Ad Preload: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdRewarded.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            activity.runOnUiThread(() -> {
                rewardedCallback.onAdFailedToLoad();
            });
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        Log.d(TAG, "REWARD Ad Preload: number ad preloading = " + AsyncSplash.Companion.getInstance().getNumberPreloading());

        AdRequest adRequest = new AdRequest.Builder(listIdRewarded.get(0)).build();
        PreloadConfiguration configuration = new PreloadConfiguration(adRequest, AsyncSplash.Companion.getInstance().getNumberPreloading());

        PreloadCallback callback = new PreloadCallback() {
            @Override
            public void onAdFailedToPreload(@NonNull String preloadId, @NonNull LoadAdError adError) {
                Log.d(TAG, "REWARD Ad Preload: Preload ad " + preloadId + " failed to load with error: " + adError.getMessage());
            }

            @Override
            public void onAdPreloaded(@NonNull String s, @Nullable ResponseInfo responseInfo) {
                Log.d(TAG, "REWARD Ad Preload: Preload ad for " + s + " is available.");
            }

            @Override
            public void onAdsExhausted(@NonNull String preloadId) {
                Log.d(TAG, "REWARD Ad Preload: Preload ad for " + preloadId + " is exhausted.");
            }
        };

        RewardedAdPreloader.start(listIdRewarded.get(0), configuration, callback);
    }

    public void showRewardAdPreload(Activity activity, List<String> listIdRewarded, RewardedCallback rewardedCallback, String remoteKey) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "REWARD Ad Preload: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            activity.runOnUiThread(() -> {
                rewardedCallback.onNextAction();
            });
            return;
        }

        loadingAdsDialog = new LoadingAdsDialog(activity);
        if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
            loadingAdsDialog.show();
        }

        RewardedAd ad = RewardedAdPreloader.pollAd(listIdRewarded.get(0));
        if (ad != null) {
            ad.setAdEventCallback(new RewardedAdEventCallback() {
                @Override
                public void onAdPaid(@NonNull AdValue value) {
                    //Track revenue
                    AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
                }

                @Override
                public void onAdClicked() {
                    AppOpenManager.isLastActionClickAd = true;
                    Log.d(TAG, "REWARD Ad Preload: onAdClicked. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                    activity.runOnUiThread(() -> {
                        rewardedCallback.onAdClicked();
                    });
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "REWARD Ad Preload: onAdDismissedFullScreenContent. " + remoteKey);
                    activity.runOnUiThread(() -> {
                        rewardedCallback.onAdDismissedFullScreenContent();
                        rewardedCallback.onNextAction();
                    });
                    isInterOrRewardedShowing = false;
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                    Log.e(TAG, "REWARD Ad Preload: onAdFailedToShowFullScreenContent. " + remoteKey);
                    activity.runOnUiThread(() -> {
                        rewardedCallback.onAdFailedToShowFullScreenContent();
                        rewardedCallback.onNextAction();
                    });
                    if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
                    }
                }

                @Override
                public void onAdImpression() {
                    Log.d(TAG, "REWARD Ad Preload: onAdImpression. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                    activity.runOnUiThread(() -> {
                        rewardedCallback.onAdImpression();
                    });
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "REWARD Ad Preload: onAdShowedFullScreenContent. " + remoteKey);
                    activity.runOnUiThread(() -> {
                        rewardedCallback.onAdShowedFullScreenContent();
                    });
                    if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
                    }
                    isInterOrRewardedShowing = true;
                }
            });

            ad.show(activity, rewardItem -> {
                Log.d(TAG, "REWARD Ad Preload: The user earned the reward. " + remoteKey);
                activity.runOnUiThread(() -> {
                    rewardedCallback.onUserEarnedReward();
                });
            });

        } else {
            activity.runOnUiThread(() -> {
                rewardedCallback.onNextAction();
            });
        }
    }

    //end

    //================================End reward ads================================

    //================================Start reward inter================================
    public void loadRewardInterAds(Activity activity, List<String> listIdRewardedInter, RewardedInterCallback rewardedInterCallback, String remoteKey) {
        ArrayList<String> listIdRewardedInterTemp = new ArrayList<>(listIdRewardedInter);
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdRewardedInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "REWARD INTER: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdRewardedInterTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            activity.runOnUiThread(() -> {
                rewardedInterCallback.onAdFailedToLoad();
                rewardedInterCallback.onNextAction();
            });
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        RewardedInterstitialAd.load(new AdRequest.Builder(listIdRewardedInterTemp.get(0)).build(), new AdLoadCallback<RewardedInterstitialAd>() {
            @Override
            public void onAdLoaded(@NonNull RewardedInterstitialAd ad) {
                Log.i(TAG, "REWARD INTER: onAdLoaded. " + remoteKey);
                activity.runOnUiThread(() -> {
                    rewardedInterCallback.onAdLoaded(ad);
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "REWARD INTER: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                activity.runOnUiThread(() -> {
                    rewardedInterCallback.onAdFailedToLoad();
                });
                if (!listIdRewardedInterTemp.isEmpty()) {
                    listIdRewardedInterTemp.remove(0);
                }
                loadRewardInterAds(activity, listIdRewardedInterTemp, rewardedInterCallback, remoteKey);
            }
        });
    }

    public void showRewardInterAds(Activity activity, RewardedInterstitialAd rewardedInterstitialAd, RewardedInterCallback rewardedInterCallback, String remoteKey) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "REWARD INTER: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            activity.runOnUiThread(() -> {
                rewardedInterCallback.onAdFailedToLoad();
                rewardedInterCallback.onNextAction();
            });
            return;
        }
        if (rewardedInterstitialAd == null) {
            Log.d(TAG, "REWARD INTER: The rewarded inter ad wasn't ready yet.");
            activity.runOnUiThread(() -> {
                rewardedInterCallback.onAdFailedToShowFullScreenContent();
                rewardedInterCallback.onNextAction();
            });
            return;
        }
        loadingAdsDialog = new LoadingAdsDialog(activity);
        if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
            loadingAdsDialog.show();
        }
        rewardedInterstitialAd.setAdEventCallback(new RewardedInterstitialAdEventCallback() {
            @Override
            public void onAdPaid(@NonNull AdValue value) {
                //Tracking revenue
                AdjustUtil.trackRevenue(rewardedInterstitialAd.getResponseInfo().getLoadedAdSourceResponseInfo(), value);
            }

            @Override
            public void onAdClicked() {
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "REWARD INTER: onAdClicked. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                activity.runOnUiThread(() -> {
                    rewardedInterCallback.onAdClicked();
                });
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "REWARD INTER: onAdDismissedFullScreenContent. " + remoteKey);
                activity.runOnUiThread(() -> {
                    rewardedInterCallback.onAdDismissedFullScreenContent();
                    rewardedInterCallback.onNextAction();
                });
                isInterOrRewardedShowing = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                Log.e(TAG, "REWARD INTER: onAdFailedToShowFullScreenContent. " + remoteKey);
                activity.runOnUiThread(() -> {
                    rewardedInterCallback.onAdFailedToShowFullScreenContent();
                    rewardedInterCallback.onNextAction();
                });
                if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    dismissLoadingDialog();
                }
            }

            @Override
            public void onAdImpression() {
                Log.d(TAG, "REWARD INTER: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                activity.runOnUiThread(() -> {
                    rewardedInterCallback.onAdImpression();
                });
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "REWARD INTER: onAdShowedFullScreenContent. " + remoteKey);
                activity.runOnUiThread(() -> {
                    rewardedInterCallback.onAdShowedFullScreenContent();
                });
                if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    dismissLoadingDialog();
                }
                isInterOrRewardedShowing = true;
            }
        });
        rewardedInterstitialAd.show(activity, rewardItem -> {
            Log.d(TAG, "REWARD INTER: The user earned the reward. " + remoteKey);
            activity.runOnUiThread(() -> {
                rewardedInterCallback.onUserEarnedReward();
            });
        });
    }
    //================================End reward inter================================
}
