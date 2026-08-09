package com.amazic.library.ads.admob;

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
import androidx.lifecycle.LifecycleOwner;

import com.amazic.library.Utils.AdjustUtil;
import com.amazic.library.Utils.EventTrackingHelper;
import com.amazic.library.Utils.NetworkUtil;
import com.amazic.library.Utils.RemoteConfigHelper;
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
import com.amazic.library.ads.splash_ads.AdsSplash;
import com.amazic.library.ads.splash_ads.AsyncSplash;
import com.amazic.library.dialog.LoadingAdsDialog;
import com.amazic.library.organic.TechManager;
import com.amazic.library.ump.AdsConsentManager;
import com.amazic.library.view.NativeAfterInterActivity;
import com.amazic.mylibrary.R;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.interstitial.InterstitialAdPreloader;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.preload.PreloadCallbackV2;
import com.google.android.gms.ads.preload.PreloadConfiguration;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAdPreloader;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Admob {
    private static final String TAG = "Admob";
    private static Admob INSTANCE;
    private final Handler handlerTimeoutInter = new Handler(Looper.getMainLooper());
    private final Handler handlerTimeoutReward = new Handler(Looper.getMainLooper());
    public LoadingAdsDialog loadingAdsDialog;
    public ArrayList<Integer> listAnimationDialogRaw = new ArrayList<>();
    public int timeHttpInter = -1;
    public int timeHttpNative = -1;
    public int timeHttpBanner = -1;
    public int timeHttpOpen = -1;
    private boolean isCustomAnimationDialog = false;
    private boolean isInterOrRewardedShowing = false;
    private boolean isShowAllAds = true;
    private long timeInterval = 0L;
    private long lastTimeDismissInter = 0L;
    private long timeIntervalFromStart = 0L;
    private long timeStart = 0L;
    private String tokenEventAdjust = "";
    private Runnable runnable;
    private boolean openActivityAfterShowInterAds = true;
    private boolean isDetectTestAdByView = false;
    private NativeAd myNativeAd = null;
    private int timeOutCallInterAds = 12000;
    private final int timeOutCallRewardAds = 12000;
    private boolean isLoadInterAdsIdTimeout = false;
    private boolean isLoadRewardAdsIdTimeout = false;
    private boolean isInitAdmobDone = false;
    // NOTE: all splash-screen interstitial/native-full-splash state used to live here too.
    // It now lives in AdsSplash (see AdsSplash.java) - use AdsSplash.getInstance() for it.

    public static Admob getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Admob();
        }
        return INSTANCE;
    }

    public static String limitString(String str, int maxLength) {
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    public void initAdmob(Activity activity, IOnInitAdmobDone iOnInitAdmobDone) {
        resetVariable();
        initLoadingDialog(activity);
//        new Thread(() -> {
//            // Initialize the Google Mobile Ads SDK on a background thread.
//            MobileAds.initialize(activity, initializationStatus -> {
//                Log.d(TAG, "initAdmob: " + initializationStatus.getAdapterStatusMap());
//                setIsInitAdmobDone(true);
//                iOnInitAdmobDone.onInitAdmobDone();
//            });
//        }).start();
    }

    private void resetVariable() {
        AdsSplash.getInstance().resetSplashTimeoutFlag();
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

    /**
     * @deprecated moved to {@link AdsSplash#getTimeDelayWaitInterHigh()}. Kept here so existing
     * call sites (Admob.getInstance().getTimeDelayWaitInterHigh()) keep compiling.
     */
    @Deprecated
    public int getTimeDelayWaitInterHigh() {
        return AdsSplash.getInstance().getTimeDelayWaitInterHigh();
    }

    /**
     * @deprecated moved to {@link AdsSplash#setTimeDelayWaitInterHigh(int)}.
     */
    @Deprecated
    public void setTimeDelayWaitInterHigh(int timeDelayWaitInterHigh) {
        AdsSplash.getInstance().setTimeDelayWaitInterHigh(timeDelayWaitInterHigh);
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

    /**
     * @deprecated moved to {@link AdsSplash#getInterstitialAdSplashHigh()}.
     */
    @Deprecated
    public InterstitialAd getInterstitialAdSplashHigh() {
        return AdsSplash.getInstance().getInterstitialAdSplashHigh();
    }

    /**
     * @deprecated moved to {@link AdsSplash#setInterstitialAdSplashHigh(InterstitialAd)}.
     */
    @Deprecated
    public void setInterstitialAdSplashHigh(InterstitialAd mInterstitialAdSplashHigh) {
        AdsSplash.getInstance().setInterstitialAdSplashHigh(mInterstitialAdSplashHigh);
    }

    /**
     * @deprecated moved to {@link AdsSplash#getInterstitialAdSplash()}.
     */
    @Deprecated
    public InterstitialAd getInterstitialAdSplash() {
        return AdsSplash.getInstance().getInterstitialAdSplash();
    }

    /**
     * @deprecated moved to {@link AdsSplash#setInterstitialAdSplash(InterstitialAd)}.
     */
    @Deprecated
    public void setInterstitialAdSplash(InterstitialAd mInterstitialAdSplash) {
        AdsSplash.getInstance().setInterstitialAdSplash(mInterstitialAdSplash);
    }

    public int getTimeOutCallInterAds() {
        return timeOutCallInterAds;
    }

    public void setTimeOutCallInterAds(int timeOutCallInterAds) {
        this.timeOutCallInterAds = timeOutCallInterAds;
    }

    /**
     * @deprecated moved to {@link AdsSplash#getTimeOutCallSplashAds()}.
     */
    @Deprecated
    public int getTimeOutCallSplashAds() {
        return AdsSplash.getInstance().getTimeOutCallSplashAds();
    }

//    public int getTimeDelayNativeSplash() {
//        return timeDelayAdsSplash;
//    }

//    public void setTimeDelayNativeSplash(int timeDelay) {
//        this.timeDelayAdsSplash = timeDelay;
//    }

    /**
     * @deprecated moved to {@link AdsSplash#setTimeOutCallSplashAds(int)}.
     */
    @Deprecated
    public void setTimeOutCallSplashAds(int timeOutCallSplashAds) {
        AdsSplash.getInstance().setTimeOutCallSplashAds(timeOutCallSplashAds);
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

    public long getTimeStart() {
        Log.d(TAG, "getTimeStart: " + (System.currentTimeMillis() - this.timeStart) / (1000) + "(s)");
        return this.timeStart;
    }

    public void setTimeStart(long timeStart) {
        this.timeStart = timeStart;
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

    public String getTokenEventAdjust() {
        return this.tokenEventAdjust;
    }

    public void setTokenEventAdjust(String tokenEventAdjust) {
        this.tokenEventAdjust = tokenEventAdjust;
    }

    public boolean isInterOrRewardedShowing() {
        return isInterOrRewardedShowing;
    }

    public void setInterOrRewardedShowing(boolean isInterOrRewardedShowing) {
        this.isInterOrRewardedShowing = isInterOrRewardedShowing;
    }

    public boolean getShowAllAds() {
        return isShowAllAds;
    }

    public void setShowAllAds(boolean isShowAllAds) {
        this.isShowAllAds = isShowAllAds;
    }

    public boolean getIsInitAdmobDone() {
        return isInitAdmobDone;
    }

    public void setIsInitAdmobDone(boolean isInitDone) {
        this.isInitAdmobDone = isInitDone;
    }

    public void removeHandlerInterAds() {
        if (handlerTimeoutInter != null && runnable != null) {
            handlerTimeoutInter.removeCallbacks(runnable);
            handlerTimeoutInter.removeCallbacksAndMessages(null);
            //handlerTimeoutInter = null;
        }
    }

    /**
     * @deprecated moved to {@link AdsSplash#removeHandlerSplashAds()}. Kept here so existing
     * call sites (Admob.getInstance().removeHandlerSplashAds()) keep compiling.
     */
    @Deprecated
    public void removeHandlerSplashAds() {
        AdsSplash.getInstance().removeHandlerSplashAds();
    }

    public void removeHandlerRewardAds() {
        if (handlerTimeoutReward != null && runnable != null) {
            handlerTimeoutReward.removeCallbacks(runnable);
            handlerTimeoutReward.removeCallbacksAndMessages(null);
        }
    }

    /**
     * Not private anymore: {@link AdsSplash} (and any other extracted ad-format class) also
     * needs to dismiss the shared loading dialog.
     */
    public void dismissLoadingDialog() {
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
        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        if (timeHttpInter != -1) adRequestBuilder.setHttpTimeoutMillis(timeHttpInter);
        AdRequest adRequest = adRequestBuilder.build();
        InterstitialAd.load(activity, listIdInterTemp.get(0), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        //Tracking revenue
                        interstitialAd.setOnPaidEventListener(adValue -> {
                            //Adjust
                            AdjustUtil.trackRevenue(interstitialAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdInterTemp.get(0), remoteKey);
                        });
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
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
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
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
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
        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        if (timeHttpInter != -1) adRequestBuilder.setHttpTimeoutMillis(timeHttpInter);
        AdRequest adRequest = adRequestBuilder.build();
        InterstitialAd.load(activity, listIdInterTemp.get(0), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        //Tracking revenue
                        interstitialAd.setOnPaidEventListener(adValue -> {
                            //Adjust
                            AdjustUtil.trackRevenue(interstitialAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdInterTemp.get(0), remoteKeyInter);
                        });
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
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
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
//                    if (!openActivityAfterShowInterAds) {
                    if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                        if (isShowNativeAfterInter) {
                            startNativeAfterInter(activity, interCallback);
                        } else {
                            interCallback.onNextAction();
                        }
                    } else {
                        if (!openActivityAfterShowInterAds) {
                            interCallback.onNextAction();
                        }
                    }
//                    } else {
//                        /// can check neu truong hop load fail native after inter thi dismiss chuyen onnext
//                        if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
//                            if (isShowNativeAfterInter) {
//                                NativeAd nativeAd = NativeAfterInterManager.mapNativeAdsAfterInter.get(adsKeyNative);
//                                if (nativeAd == null) {
//                                    interCallback.onNextAction();
//                                }
//
//                            }
//                        }
//                    }
                    isInterOrRewardedShowing = false;
                    lastTimeDismissInter = System.currentTimeMillis();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    // Called when ad fails to show.
                    Log.e(TAG, "INTER: Ad failed to show fullscreen content. " + remoteKeyInter);
                    interCallback.onAdFailedToShowFullScreenContent();
//                    if (!openActivityAfterShowInterAds) {
                    if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                        if (isShowNativeAfterInter) {
                            startNativeAfterInter(activity, interCallback);
                        } else {
                            interCallback.onNextAction();
                        }
                    } else {
                        if (!openActivityAfterShowInterAds) {
                            interCallback.onNextAction();
                        }
                    }
//                    } else {
//                        /// can check neu truong hop load fail native after inter thi dismiss chuyen onnext
//                        if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
//                            if (isShowNativeAfterInter) {
//                                NativeAd nativeAd = NativeAfterInterManager.mapNativeAdsAfterInter.get(adsKeyNative);
//                                if (nativeAd == null) {
//                                    interCallback.onNextAction();
//                                }
//
//                            }
//                        }
//                    }
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
//                    if (isShowNativeAfterInter) {
//                        startNativeAfterInter(activity, interCallback);
//                    } else {
//                        interCallback.onNextAction();
//                    }
                } else {
                    interCallback.onNextAction();
                }
            }
            mInterstitialAd.setImmersiveMode(true);
            mInterstitialAd.show(activity);
        }
    }

    // startNativeAfterInterSplash / startNativeMetaSplash moved to AdsSplash (splash-only).

    private void startNativeAfterInter(Activity activity, InterCallback interCallback) {
        NativeAfterInterActivity.Companion.setInterCallback(interCallback);
        NativeAfterInterActivity.Companion.setSplashMode(false);
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
        PreloadConfiguration configuration = new PreloadConfiguration.Builder(listIdInter.get(0)).setBufferSize(AsyncSplash.Companion.getInstance().getNumberPreloading()).build();

        PreloadCallbackV2 callback = new PreloadCallbackV2() {
            @Override
            public void onAdFailedToPreload(@NonNull String s, @NonNull AdError adError) {
                super.onAdFailedToPreload(s, adError);
                EventTrackingHelper.logEvent(context, remoteKey + "inter_preload_failed");
                Log.d(TAG, "INTER Ad Preload: Preload ad " + s + " failed to load with error: " + adError.getMessage());
                interCallback.onAdFailedToLoad();
            }

            @Override
            public void onAdPreloaded(@NonNull String s, @Nullable ResponseInfo responseInfo) {
                super.onAdPreloaded(s, responseInfo);
                EventTrackingHelper.logEvent(context, remoteKey + "inter_preload_loaded");
                Log.d(TAG, "INTER Ad Preload: Preload ad for " + s + " is available.");
                interCallback.onAdLoaded(null);
            }


            @Override
            public void onAdsExhausted(@NonNull String s) {
                super.onAdsExhausted(s);
                Log.d(TAG, "INTER Ad Preload: Preload ad for " + s + " is exhausted.");
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
            ad.setOnPaidEventListener(
                    adValue -> {
                        AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdInter.get(0), remoteKey);
                    }
            );

            ad.setFullScreenContentCallback(new FullScreenContentCallback() {
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

//                    if (!openActivityAfterShowInterAds) {
                    if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                        if (isShowNativeAfterInter) {
                            startNativeAfterInter(activity, interCallback);
                        } else {
                            interCallback.onNextAction();
                        }
                    } else {
                        if (!openActivityAfterShowInterAds) {
                            interCallback.onNextAction();
                        }
                    }
//                    } else {
//                        /// can check neu truong hop load fail native after inter thi dismiss chuyen onnext
//                        if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
//                            if (isShowNativeAfterInter) {
//                                NativeAd nativeAd = NativeAfterInterManager.mapNativeAdsAfterInter.get(adsKeyNative);
//                                if (nativeAd == null) {
//                                    interCallback.onNextAction();
//                                }
//
//                            }
//                        }
//                    }
                    isInterOrRewardedShowing = false;
                    lastTimeDismissInter = System.currentTimeMillis();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.e(TAG, "INTER Ad Preload: Ad failed to show fullscreen content. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "inter_preload_failed_to_show");
                    interCallback.onAdFailedToShowFullScreenContent();
                    Log.d(TAG, "INTER Ad Preload: onAdFailedToShowFullScreenContent - getShowNativeAfterInter =  " + AsyncSplash.Companion.getInstance().getShowNativeAfterInter() + ", isShowNativeAfterInter = " + isShowNativeAfterInter + ", openActivityAfterShowInterAds = " + openActivityAfterShowInterAds);

//                    if (!openActivityAfterShowInterAds) {
                    if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
                        if (isShowNativeAfterInter) {
                            startNativeAfterInter(activity, interCallback);
                        } else {
                            interCallback.onNextAction();
                        }
                    } else {
                        if (!openActivityAfterShowInterAds) {
                            interCallback.onNextAction();
                        }
                    }
//                    } else {
//                        /// can check neu truong hop load fail native after inter thi dismiss chuyen onnext
//                        if (AsyncSplash.Companion.getInstance().getShowNativeAfterInter()) {
//                            if (isShowNativeAfterInter) {
//                                NativeAd nativeAd = NativeAfterInterManager.mapNativeAdsAfterInter.get(adsKeyNative);
//                                if (nativeAd == null) {
//                                    interCallback.onNextAction();
//                                }
//
//                            }
//                        }
//                    }
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
                    super.onAdShowedFullScreenContent();
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
//                    if (isShowNativeAfterInter) {
//                        startNativeAfterInter(activity, interCallback);
//                    } else {
//                        interCallback.onNextAction();
//                    }
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

    // ---- Splash-screen ad flows (native-full-splash + inter preloading-API splash) ----
    // Moved to AdsSplash for readability. Thin @Deprecated delegates kept below so existing
    // call sites (Admob.getInstance().xxxSplash(...)) keep compiling; new code should call
    // AdsSplash.getInstance() directly.

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


            PreloadConfiguration configuration = new PreloadConfiguration.Builder(listIdInterTemp.get(0)).setBufferSize(AsyncSplash.Companion.getInstance().getNumberPreloading()).build();

            final AtomicBoolean isFirstLoadAd = new AtomicBoolean(true);
            PreloadCallbackV2 callback = new PreloadCallbackV2() {
                @Override
                public void onAdFailedToPreload(@NonNull String s, @NonNull AdError adError) {
                    super.onAdFailedToPreload(s, adError);
                    EventTrackingHelper.logEvent(activity, remoteKey + "inter_preload_failed");
                    Log.d(TAG, "INTER Ad Preload - loadAndShow: onAdFailedToPreload - Preload ad " + s + " failed to load with error: " + adError.getMessage());
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
                public void onAdPreloaded(@NonNull String s, @Nullable ResponseInfo responseInfo) {
                    super.onAdPreloaded(s, responseInfo);
                    EventTrackingHelper.logEvent(activity, remoteKey + "inter_preload_loaded");
                    Log.d(TAG, "INTER Ad Preload - loadAndShow: Preload ad for " + s + " is available.");
                    if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
                    }
                    if (isFirstLoadAd.getAndSet(false)) {
                        interCallback.onAdLoaded(null);
                    }
                    removeHandlerInterAds();

                }


                @Override
                public void onAdsExhausted(@NonNull String s) {
                    super.onAdsExhausted(s);
                    Log.d(TAG, "INTER Ad Preload - loadAdnShow: Preload ad for " + s + " is exhausted.");
                }
            };

            InterstitialAdPreloader.start(listIdInterTemp.get(0), configuration, callback);
        }

    }

    /**
     * @deprecated moved to {@link AdsSplash#loadAndShowNativeFullSplashCount}.
     */
    @Deprecated
    public void loadAndShowNativeFullSplashCount(AppCompatActivity activity, List<String> listIdNative, InterCallback interCallback, String adsKeyNative, String remoteKeyNative) {
        AdsSplash.getInstance().loadAndShowNativeFullSplashCount(activity, listIdNative, interCallback, adsKeyNative, remoteKeyNative);
    }

    /**
     * @deprecated moved to {@link AdsSplash#loadAndShowMetaNativeFullSplashCount}.
     */
    @Deprecated
    public void loadAndShowMetaNativeFullSplashCount(AppCompatActivity activity, InterCallback interCallback) {
        AdsSplash.getInstance().loadAndShowMetaNativeFullSplashCount(activity, interCallback);
    }

    /**
     * @deprecated moved to {@link AdsSplash#loadAndShowInterAdPreloadingSplashDelay}.
     */
    @Deprecated
    public void loadAndShowInterAdPreloadingSplashDelay(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback, String adsKeyNative, String remoteKeyNative) {
        AdsSplash.getInstance().loadAndShowInterAdPreloadingSplashDelay(activity, listIdInter, interCallback, adsKeyNative, remoteKeyNative);
    }

    /**
     * @deprecated moved to {@link AdsSplash#showInterAdPreloadingSplashDelay}.
     */
    @Deprecated
    public void showInterAdPreloadingSplashDelay(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback, boolean isConfigShowNativeAfterInter, boolean isEmptyListNativeAfterInter, String adsKeyNative) {
        AdsSplash.getInstance().showInterAdPreloadingSplashDelay(activity, listIdInter, interCallback, isConfigShowNativeAfterInter, isEmptyListNativeAfterInter, adsKeyNative);
    }

    public void loadInterAds(Context context, List<String> listIdInter, InterCallback interCallback, String remoteKey) {
        ArrayList<String> listIdInterTemp = new ArrayList<>(listIdInter);
        //Check condition
        if (!NetworkUtil.isNetworkActive(context) || listIdInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(context) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(context, remoteKey)) {
            Log.d(TAG, "INTER: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(context) + "_IdEmpty:" + listIdInterTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(context) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(context, remoteKey));
            interCallback.onNextAction();
            return;
        }
        EventTrackingHelper.logEvent(context, remoteKey + "_true");
        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        if (timeHttpInter != -1) adRequestBuilder.setHttpTimeoutMillis(timeHttpInter);
        AdRequest adRequest = adRequestBuilder.build();
        InterstitialAd.load(context, listIdInterTemp.get(0), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        //Tracking revenue
                        interstitialAd.setOnPaidEventListener(adValue -> {
                            //Adjust
                            AdjustUtil.trackRevenue(interstitialAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdInterTemp.get(0), remoteKey);
                        });
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
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
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
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
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

    /**
     * @deprecated moved to {@link AdsSplash#showInterAdsSplashDelay}.
     */
    @Deprecated
    public void showInterAdsSplashDelay(AppCompatActivity activity, InterCallback interCallback, boolean isConfigShowNativeAfterInter, boolean isEmptyListNativeAfterInter) {
        AdsSplash.getInstance().showInterAdsSplashDelay(activity, interCallback, isConfigShowNativeAfterInter, isEmptyListNativeAfterInter);
    }

    /**
     * @deprecated moved to {@link AdsSplash#showInterAdsSplash}.
     */
    @Deprecated
    public void showInterAdsSplash(AppCompatActivity activity, InterCallback interCallback) {
        AdsSplash.getInstance().showInterAdsSplash(activity, interCallback);
    }

    /**
     * @deprecated moved to {@link AdsSplash#showInterAdsSplashAsync}.
     */
    @Deprecated
    public void showInterAdsSplashAsync(InterstitialAd interSplash, AppCompatActivity activity, InterCallback interCallback) {
        AdsSplash.getInstance().showInterAdsSplashAsync(interSplash, activity, interCallback);
    }

    /**
     * @deprecated moved to {@link AdsSplash#loadAndShowIdInterAdSplashAsync}.
     */
    @Deprecated
    public void loadAndShowIdInterAdSplashAsync(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback) {
        AdsSplash.getInstance().loadAndShowIdInterAdSplashAsync(activity, listIdInter, interCallback);
    }

    /**
     * @deprecated moved to {@link AdsSplash#loadAndShowInterAdSplash}.
     */
    @Deprecated
    public void loadAndShowInterAdSplash(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback) {
        AdsSplash.getInstance().loadAndShowInterAdSplash(activity, listIdInter, interCallback);
    }

    /**
     * @deprecated moved to {@link AdsSplash#loadAndShowInterAdSplashDelay}.
     */
    @Deprecated
    public void loadAndShowInterAdSplashDelay(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback, String adsKeyNative, String remoteKeyNative) {
        AdsSplash.getInstance().loadAndShowInterAdSplashDelay(activity, listIdInter, interCallback, adsKeyNative, remoteKeyNative);
    }

    /**
     * @deprecated moved to {@link AdsSplash#removeHandlerDelayAdsSplash}.
     */
    @Deprecated
    public void removeHandlerDelayAdsSplash() {
        AdsSplash.getInstance().removeHandlerDelayAdsSplash();
    }

    /**
     * @deprecated moved to {@link AdsSplash#loadAndShowInterAdSplashLoop}.
     */
    @Deprecated
    public void loadAndShowInterAdSplashLoop(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback) {
        AdsSplash.getInstance().loadAndShowInterAdSplashLoop(activity, listIdInter, interCallback);
    }


    //================================end inter ads================================

    /**
     * @deprecated moved to {@link AdsSplash#onCheckShowSplashWhenFail}.
     */
    @Deprecated
    public void onCheckShowSplashWhenFail(AppCompatActivity activity, InterCallback interCallback) {
        AdsSplash.getInstance().onCheckShowSplashWhenFail(activity, interCallback);
    }

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
        AdView adView = new AdView(activity);
        adView.setAdUnitId(listIdBannerTemp.get(0));
        adView.setAdSize(getAdSize(activity));
        // [END create_ad_view]

        // [START load_ad]
        // Start loading the ad in the background.
        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        if (timeHttpBanner != -1) adRequestBuilder.setHttpTimeoutMillis(timeHttpBanner);
        AdRequest adRequest = adRequestBuilder.build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "BANNER: onAdClicked. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                bannerCallback.onAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                Log.d(TAG, "BANNER: onAdClosed. " + remoteKey);
                bannerCallback.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.e(TAG, "BANNER: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                bannerCallback.onAdFailedToLoad();
                if (!listIdBannerTemp.isEmpty()) {
                    listIdBannerTemp.remove(0);
                }
                loadBannerAdsBackupWithoutShow(activity, listIdBannerTemp, bannerCallback, remoteKey);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                Log.d(TAG, "BANNER: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                bannerCallback.onAdImpression();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                //Tracking revenue
                adView.setOnPaidEventListener(adValue -> {
                    //Adjust
                    if (adView.getResponseInfo() != null) {
                        AdjustUtil.trackRevenue(adView.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdBannerTemp.get(0), remoteKey);
                    }
                });
                Log.i(TAG, "BANNER: onAdLoaded. " + remoteKey);

                //DetectTestAd
                //Reset TechManager to false
                if (AsyncSplash.Companion.getInstance().getUserTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)) {
                    TechManager.getInstance().detectedTech(activity, false);
                }
                if ((remoteKey.toLowerCase().trim().equals("banner_splash") || remoteKey.toLowerCase().trim().equals("banner_setting"))
                        && !AsyncSplash.Companion.getInstance().getDebug()
                        && AsyncSplash.Companion.getInstance().getUserTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)
                ) {
                    boolean isTestAd = detectTestAd(adView);
                    EventTrackingHelper.logEvent(activity, "device_test_" + isTestAd + "_" + adRequest.isTestDevice(activity));
                    Log.d(TAG, "BANNER: onAdImpression. isTestAd: " + isTestAd);
                    TechManager.getInstance().detectedTech(activity, isTestAd);

                    if (AsyncSplash.Companion.getInstance().getUserTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)
                            && TechManager.getInstance().isTech(activity)
                            && !AsyncSplash.Companion.getInstance().getDebug()) {
                        AsyncSplash.Companion.getInstance().turnOffSomeRemoteKeys(activity);
                    }
                }

                bannerCallback.onAdLoaded();
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                Log.d(TAG, "BANNER: onAdOpened. " + remoteKey);
                bannerCallback.onAdOpened();
            }

            @Override
            public void onAdSwipeGestureClicked() {
                super.onAdSwipeGestureClicked();
                Log.d(TAG, "BANNER: onAdSwipeGestureClicked. " + remoteKey);
                bannerCallback.onAdSwipeGestureClicked();
            }
        });
        // [END load_ad]
        return adView;
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
        AdView adView = new AdView(activity);
        adView.setAdUnitId(listIdBannerTemp.get(0));
        adView.setAdSize(getAdSize(activity));
        // [END create_ad_view]

        // [START load_ad]
        // Start loading the ad in the background.
        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        if (timeHttpBanner != -1) adRequestBuilder.setHttpTimeoutMillis(timeHttpBanner);
        AdRequest adRequest = adRequestBuilder.build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "BANNER: onAdClicked. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                bannerCallback.onAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                Log.d(TAG, "BANNER: onAdClosed. " + remoteKey);
                bannerCallback.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.e(TAG, "BANNER: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                bannerCallback.onAdFailedToLoad();
                if (!listIdBannerTemp.isEmpty()) {
                    listIdBannerTemp.remove(0);
                }
                loadBannerAdsWithoutShow(activity, listIdBannerTemp, bannerCallback, remoteKey);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                Log.d(TAG, "BANNER: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                bannerCallback.onAdImpression();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                //Tracking revenue
                adView.setOnPaidEventListener(adValue -> {
                    //Adjust
                    if (adView.getResponseInfo() != null) {
                        AdjustUtil.trackRevenue(adView.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdBannerTemp.get(0), remoteKey);
                    }
                });
                Log.i(TAG, "BANNER: onAdLoaded. " + remoteKey);

                //DetectTestAd
                //Reset TechManager to false
                if (AsyncSplash.Companion.getInstance().getUserTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)) {
                    TechManager.getInstance().detectedTech(activity, false);
                }
                if ((remoteKey.toLowerCase().trim().equals("banner_splash") || remoteKey.toLowerCase().trim().equals("banner_setting"))
                        && !AsyncSplash.Companion.getInstance().getDebug()
                        && AsyncSplash.Companion.getInstance().getUserTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)
                ) {
                    boolean isTestAd = detectTestAd(adView);
                    EventTrackingHelper.logEvent(activity, "device_test_" + isTestAd + "_" + adRequest.isTestDevice(activity));
                    Log.d(TAG, "BANNER: onAdImpression. isTestAd: " + isTestAd);
                    TechManager.getInstance().detectedTech(activity, isTestAd);

                    if (AsyncSplash.Companion.getInstance().getUserTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)
                            && TechManager.getInstance().isTech(activity)
                            && !AsyncSplash.Companion.getInstance().getDebug()) {
                        AsyncSplash.Companion.getInstance().turnOffSomeRemoteKeys(activity);
                    }
                }

                bannerCallback.onAdLoaded();
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                Log.d(TAG, "BANNER: onAdOpened. " + remoteKey);
                bannerCallback.onAdOpened();
            }

            @Override
            public void onAdSwipeGestureClicked() {
                super.onAdSwipeGestureClicked();
                Log.d(TAG, "BANNER: onAdSwipeGestureClicked. " + remoteKey);
                bannerCallback.onAdSwipeGestureClicked();
            }
        });
        // [END load_ad]
        return adView;
    }

    public void loadBannerAds(Activity activity, List<String> listIdBanner, FrameLayout adContainerView, BannerCallback bannerCallback, IOnAdsImpression iOnAdsImpression, String remoteKey) {
        ArrayList<String> listIdBannerTemp = new ArrayList<>(listIdBanner);
        if (adContainerView != null) {
            adContainerView.removeAllViews();
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
        AdView adView = new AdView(activity);
        adView.setAdUnitId(listIdBannerTemp.get(0));
        adView.setAdSize(getAdSize(activity));
        // [END create_ad_view]

        // [START load_ad]
        // Start loading the ad in the background.
        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        if (timeHttpBanner != -1) adRequestBuilder.setHttpTimeoutMillis(timeHttpBanner);
        AdRequest adRequest = adRequestBuilder.build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "BANNER: onAdClicked. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                bannerCallback.onAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                Log.d(TAG, "BANNER: onAdClosed. " + remoteKey);
                bannerCallback.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.e(TAG, "BANNER: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                bannerCallback.onAdFailedToLoad();
                if (!listIdBannerTemp.isEmpty()) {
                    listIdBannerTemp.remove(0);
                }
                loadBannerAds(activity, listIdBannerTemp, adContainerView, bannerCallback, iOnAdsImpression, remoteKey);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                Log.d(TAG, "BANNER: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                bannerCallback.onAdImpression();
                //use for auto reload banner after x seconds
                iOnAdsImpression.onAdsImpression();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                //Tracking revenue
                adView.setOnPaidEventListener(adValue -> {
                    //Adjust
                    if (adView.getResponseInfo() != null) {
                        AdjustUtil.trackRevenue(adView.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdBannerTemp.get(0), remoteKey);
                    }
                });
                Log.i(TAG, "BANNER: onAdLoaded. " + remoteKey);
                // Replace ad container with new ad view.
                if (adContainerView != null) {
                    adContainerView.removeAllViews();
                    adContainerView.addView(adView);
                }

                //DetectTestAd
                //Reset TechManager to false
                if (AsyncSplash.Companion.getInstance().getUserTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)) {
                    TechManager.getInstance().detectedTech(activity, false);
                }
                if ((remoteKey.toLowerCase().trim().equals("banner_splash") || remoteKey.toLowerCase().trim().equals("banner_setting"))
                        && !AsyncSplash.Companion.getInstance().getDebug()
                        && AsyncSplash.Companion.getInstance().getUserTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)
                ) {
                    boolean isTestAd = detectTestAd(adView);
                    EventTrackingHelper.logEvent(activity, "device_test_" + isTestAd + "_" + adRequest.isTestDevice(activity));
                    Log.d(TAG, "BANNER: onAdImpression. isTestAd: " + isTestAd);
                    TechManager.getInstance().detectedTech(activity, isTestAd);

                    if (AsyncSplash.Companion.getInstance().getUserTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)
                            && TechManager.getInstance().isTech(activity)
                            && !AsyncSplash.Companion.getInstance().getDebug()) {
                        AsyncSplash.Companion.getInstance().turnOffSomeRemoteKeys(activity);
                    }
                }

                bannerCallback.onAdLoaded();
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                Log.d(TAG, "BANNER: onAdOpened. " + remoteKey);
                bannerCallback.onAdOpened();
            }

            @Override
            public void onAdSwipeGestureClicked() {
                super.onAdSwipeGestureClicked();
                Log.d(TAG, "BANNER: onAdSwipeGestureClicked. " + remoteKey);
                bannerCallback.onAdSwipeGestureClicked();
            }
        });
        // [END load_ad]
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
    //end can load banner ads in fragment
    //================================End banner ads================================

    //can load banner ads in fragment
    public void loadBannerAds(Context context, int adWidth, List<String> listIdBanner, FrameLayout adContainerView, BannerCallback bannerCallback, IOnAdsImpression iOnAdsImpression, String remoteKey) {
        ArrayList<String> listIdBannerTemp = new ArrayList<>(listIdBanner);
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check network
        if (!NetworkUtil.isNetworkActive(context) || listIdBannerTemp.isEmpty() || !AdsConsentManager.getConsentResult(context) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(context, remoteKey)) {
            Log.d(TAG, "BANNER: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(context) + "_IdEmpty:" + listIdBannerTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(context) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(context, remoteKey));
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
        AdView adView = new AdView(context);
        adView.setAdUnitId(listIdBannerTemp.get(0));
        adView.setAdSize(getAdSizeFragment(context, adWidth));
        // [END create_ad_view]

        // [START load_ad]
        // Start loading the ad in the background.
        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        if (timeHttpBanner != -1) adRequestBuilder.setHttpTimeoutMillis(timeHttpBanner);
        AdRequest adRequest = adRequestBuilder.build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "BANNER: onAdClicked. " + remoteKey);
                EventTrackingHelper.logEvent(context, remoteKey + "_click");
                bannerCallback.onAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                Log.d(TAG, "BANNER: onAdClosed. " + remoteKey);
                bannerCallback.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.e(TAG, "BANNER: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                bannerCallback.onAdFailedToLoad();
                if (!listIdBannerTemp.isEmpty()) {
                    listIdBannerTemp.remove(0);
                }
                loadBannerAds(context, adWidth, listIdBannerTemp, adContainerView, bannerCallback, iOnAdsImpression, remoteKey);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                Log.d(TAG, "BANNER: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(context, remoteKey + "_view");
                bannerCallback.onAdImpression();
                //use for auto reload banner after x seconds
                iOnAdsImpression.onAdsImpression();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                //Tracking revenue
                adView.setOnPaidEventListener(adValue -> {
                    //Adjust
                    if (adView.getResponseInfo() != null) {
                        AdjustUtil.trackRevenue(adView.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdBannerTemp.get(0), remoteKey);
                    }
                });
                Log.i(TAG, "BANNER: onAdLoaded. " + remoteKey);
                // Replace ad container with new ad view.
                if (adContainerView != null) {
                    adContainerView.removeAllViews();
                    adContainerView.addView(adView);
                }
                //DetectTestAd
                //Reset TechManager to false
                if (AsyncSplash.Companion.getInstance().getUserTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)) {
                    TechManager.getInstance().detectedTech(context, false);
                }
                if ((remoteKey.toLowerCase().trim().equals("banner_splash") || remoteKey.toLowerCase().trim().equals("banner_setting"))
                        && !AsyncSplash.Companion.getInstance().getDebug()
                        && AsyncSplash.Companion.getInstance().getUserTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)
                ) {
                    boolean isTestAd = detectTestAd(adView);
                    EventTrackingHelper.logEvent(context, "device_test_" + isTestAd + "_" + adRequest.isTestDevice(context));
                    Log.d(TAG, "BANNER: onAdImpression. isTestAd: " + isTestAd);
                    TechManager.getInstance().detectedTech(context, isTestAd);

                    if (AsyncSplash.Companion.getInstance().getUserTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)
                            && TechManager.getInstance().isTech(context)
                            && !AsyncSplash.Companion.getInstance().getDebug()) {
                        AsyncSplash.Companion.getInstance().turnOffSomeRemoteKeys(context);
                    }
                }
                bannerCallback.onAdLoaded();
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                Log.d(TAG, "BANNER: onAdOpened. " + remoteKey);
                bannerCallback.onAdOpened();
            }

            @Override
            public void onAdSwipeGestureClicked() {
                super.onAdSwipeGestureClicked();
                Log.d(TAG, "BANNER: onAdSwipeGestureClicked. " + remoteKey);
                bannerCallback.onAdSwipeGestureClicked();
            }
        });
        // [END load_ad]
    }

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
        adView.setAdUnitId(listIdCollapseBannerTemp.get(0));

        AdSize adSize = getAdSize(activity);
        adView.setAdSize(adSize);
        // Create an extra parameter that aligns the bottom of the expanded ad to
        // the bottom of the bannerView.
        Bundle extras = new Bundle();
        if (isGravityBottom) {
            extras.putString("collapsible", "bottom");
        } else {
            extras.putString("collapsible", "top");
        }

        AdRequest adRequest = new AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                .build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "COLLAPSE BANNER: onAdClicked. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                bannerCallback.onAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                Log.d(TAG, "COLLAPSE BANNER: onAdClosed. " + remoteKey);
                bannerCallback.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.e(TAG, "COLLAPSE BANNER: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                bannerCallback.onAdFailedToLoad();
                iOnAdsFailToLoad.onAdsFailToLoad();
                if (!listIdCollapseBannerTemp.isEmpty()) {
                    listIdCollapseBannerTemp.remove(0);
                }
                loadCollapseBanner(activity, listIdCollapseBannerTemp, adContainerView, isGravityBottom, bannerCallback, iOnAdsImpression, iOnAdsFailToLoad, collapseTypeClose, valueCountDownOrCountClick, remoteKey);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                Log.d(TAG, "COLLAPSE BANNER: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                bannerCallback.onAdImpression();
                iOnAdsImpression.onAdsImpression();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                //Tracking revenue
                adView.setOnPaidEventListener(adValue -> {
                    //Adjust
                    if (adView.getResponseInfo() != null) {
                        AdjustUtil.trackRevenue(adView.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdCollapseBannerTemp.get(0), remoteKey);
                    }
                });
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
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                Log.d(TAG, "COLLAPSE BANNER: onAdOpened. " + remoteKey);
                bannerCallback.onAdOpened();
                applyTechForCollapseBanner(collapseTypeClose, valueCountDownOrCountClick);
            }

            @Override
            public void onAdSwipeGestureClicked() {
                super.onAdSwipeGestureClicked();
                Log.d(TAG, "COLLAPSE BANNER: onAdSwipeGestureClicked. " + remoteKey);
                bannerCallback.onAdSwipeGestureClicked();
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
        adView.setAdUnitId(listIdCollapseBannerTemp.get(0));

        AdSize adSize = getAdSizeFragment(context, adWidth);
        adView.setAdSize(adSize);
        // Create an extra parameter that aligns the bottom of the expanded ad to
        // the bottom of the bannerView.
        Bundle extras = new Bundle();
        if (isGravityBottom) {
            extras.putString("collapsible", "bottom");
        } else {
            extras.putString("collapsible", "top");
        }

        AdRequest adRequest = new AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                .build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "COLLAPSE BANNER: onAdClicked. " + remoteKey);
                EventTrackingHelper.logEvent(context, remoteKey + "_click");
                bannerCallback.onAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                Log.d(TAG, "COLLAPSE BANNER: onAdClosed. " + remoteKey);
                bannerCallback.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.e(TAG, "COLLAPSE BANNER: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                bannerCallback.onAdFailedToLoad();
                iOnAdsFailToLoad.onAdsFailToLoad();
                if (!listIdCollapseBannerTemp.isEmpty()) {
                    listIdCollapseBannerTemp.remove(0);
                }
                loadCollapseBanner(context, adWidth, listIdCollapseBannerTemp, adContainerView, isGravityBottom, bannerCallback, iOnAdsImpression, iOnAdsFailToLoad, collapseTypeClose, valueCountDownOrCountClick, remoteKey);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                Log.d(TAG, "COLLAPSE BANNER: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(context, remoteKey + "_view");
                bannerCallback.onAdImpression();
                iOnAdsImpression.onAdsImpression();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                //Tracking revenue
                adView.setOnPaidEventListener(adValue -> {
                    //Adjust
                    if (adView.getResponseInfo() != null) {
                        AdjustUtil.trackRevenue(adView.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdCollapseBannerTemp.get(0), remoteKey);
                    }
                });
                Log.i(TAG, "COLLAPSE BANNER: onAdLoaded. " + remoteKey);
                bannerCallback.onAdLoaded();
                // Replace ad container with new ad view.
                if (adContainerView != null) {
                    ((AppCompatActivity) context).getLifecycle().addObserver(new DefaultLifecycleObserver() {
                        @Override
                        public void onResume(@NonNull LifecycleOwner owner) {
                            DefaultLifecycleObserver.super.onResume(owner);
                            adContainerView.removeAllViews();
                            adContainerView.addView(adView);
                        }
                    });
                }
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                Log.d(TAG, "COLLAPSE BANNER: onAdOpened. " + remoteKey);
                bannerCallback.onAdOpened();
                applyTechForCollapseBanner(collapseTypeClose, valueCountDownOrCountClick);
            }

            @Override
            public void onAdSwipeGestureClicked() {
                super.onAdSwipeGestureClicked();
                Log.d(TAG, "COLLAPSE BANNER: onAdSwipeGestureClicked. " + remoteKey);
                bannerCallback.onAdSwipeGestureClicked();
            }
        });
        return adView;
    }

    //================================End collapse banner ads================================

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
            nativeCallback.onAdFailedToLoad(new LoadAdError(2025, "Check condition", "Check condition", new AdError(2025, "Check condition", "Check condition"), null));
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        AdLoader.Builder builder = new AdLoader.Builder(activity, listIdNativeTemp.get(0));
        builder.forNativeAd(nativeAd -> {
            //Tracking revenue
            nativeAd.setOnPaidEventListener(adValue -> {
                //Adjust
                if (nativeAd.getResponseInfo() != null) {
                    AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdNativeTemp.get(0), remoteKey);
                }
            });
            Log.i(TAG, "NATIVE: onAdLoaded. " + remoteKey);
            nativeCallback.onNativeAdLoaded(nativeAd);
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "NATIVE: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                Bundle bundle = new Bundle();
                bundle.putString("failed_message", limitString(loadAdError.getMessage(), 99));
                if (loadAdError.getResponseInfo() != null && loadAdError.getResponseInfo().getLoadedAdapterResponseInfo() != null && loadAdError.getMessage().toLowerCase().contains("no fill")) {
                    bundle.putString("no_fill_source", limitString(loadAdError.getResponseInfo().getLoadedAdapterResponseInfo().getAdSourceName(), 99));
                }
                EventTrackingHelper.logEventWithMultipleParams(activity, remoteKey + "_failed", bundle);
                nativeCallback.onAdFailedToLoad(loadAdError);
                if (!listIdNativeTemp.isEmpty()) {
                    listIdNativeTemp.remove(0);
                }
                loadNativeAds(activity, listIdNativeTemp, nativeCallback, remoteKey);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                nativeCallback.onAdImpression();
                Log.d(TAG, "NATIVE: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_view");
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                nativeCallback.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "NATIVE: onAdClicked. " + ". " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_click");
            }
        }).build();

        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        if (timeHttpNative != -1) adRequestBuilder.setHttpTimeoutMillis(timeHttpNative);
        AdRequest adRequest = adRequestBuilder.build();
        adLoader.loadAd(adRequest);
    }

    public void loadNativeAdsBackup(Context activity, List<String> listIdNative, NativeCallback nativeCallback, String remoteKey) {
        ArrayList<String> listIdNativeTemp = new ArrayList<>(listIdNative);
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdNativeTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "NATIVE: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdNativeTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            nativeCallback.onAdFailedToLoad(new LoadAdError(2025, "Check condition", "Check condition", new AdError(2025, "Check condition", "Check condition"), null));
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_backup_true");
        //end log event can request ads

        AdLoader.Builder builder = new AdLoader.Builder(activity, listIdNativeTemp.get(0));
        builder.forNativeAd(nativeAd -> {
            //Tracking revenue
            nativeAd.setOnPaidEventListener(adValue -> {
                //Adjust
                if (nativeAd.getResponseInfo() != null) {
                    AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdNativeTemp.get(0), remoteKey);
                }
            });
            Log.i(TAG, "NATIVE: onAdLoaded. " + remoteKey);
            nativeCallback.onNativeAdLoaded(nativeAd);
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "NATIVE: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);

                Bundle bundle = new Bundle();
                bundle.putString("failed_message", limitString(loadAdError.getMessage(), 99));
                if (loadAdError.getResponseInfo() != null && loadAdError.getResponseInfo().getLoadedAdapterResponseInfo() != null && loadAdError.getMessage().toLowerCase().contains("no fill")) {
                    bundle.putString("no_fill_source", limitString(loadAdError.getResponseInfo().getLoadedAdapterResponseInfo().getAdSourceName(), 99));
                }
                EventTrackingHelper.logEventWithMultipleParams(activity, remoteKey + "_backup_failed", bundle);
                nativeCallback.onAdFailedToLoad(loadAdError);
                if (!listIdNativeTemp.isEmpty()) {
                    listIdNativeTemp.remove(0);
                }
                loadNativeAds(activity, listIdNativeTemp, nativeCallback, remoteKey);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                nativeCallback.onAdImpression();
                Log.d(TAG, "NATIVE: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_backup_view");
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                nativeCallback.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "NATIVE: onAdClicked. " + ". " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_backup_click");
            }
        }).build();

        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        if (timeHttpNative != -1) adRequestBuilder.setHttpTimeoutMillis(timeHttpNative);
        AdRequest adRequest = adRequestBuilder.build();
        adLoader.loadAd(adRequest);
    }

    public void loadMultipleNativeAds(Context activity, List<String> listIdNative, NativeCallback nativeCallback, String remoteKey, int maxRequest) {
        ArrayList<String> listIdNativeTemp = new ArrayList<>(listIdNative);
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdNativeTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "NATIVE: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdNativeTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            nativeCallback.onAdFailedToLoad(new LoadAdError(2025, "Check condition", "Check condition", new AdError(2025, "Check condition", "Check condition"), null));
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        AdLoader.Builder builder = new AdLoader.Builder(activity, listIdNativeTemp.get(0));
        builder.forNativeAd(nativeAd -> {
            //Tracking revenue
            nativeAd.setOnPaidEventListener(adValue -> {
                //Adjust
                if (nativeAd.getResponseInfo() != null) {
                    AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdNativeTemp.get(0), remoteKey);
                }
            });
            Log.i(TAG, "NATIVE: onAdLoaded. " + remoteKey);
            nativeCallback.onNativeAdLoaded(nativeAd);
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "NATIVE: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                Bundle bundle = new Bundle();
                bundle.putString("failed_message", limitString(loadAdError.getMessage(), 99));
                if (loadAdError.getResponseInfo() != null && loadAdError.getResponseInfo().getLoadedAdapterResponseInfo() != null && loadAdError.getMessage().toLowerCase().contains("no fill")) {
                    bundle.putString("no_fill_source", limitString(loadAdError.getResponseInfo().getLoadedAdapterResponseInfo().getAdSourceName(), 99));
                }
                EventTrackingHelper.logEventWithMultipleParams(activity, remoteKey + "_failed", bundle);
                nativeCallback.onAdFailedToLoad(loadAdError);
                if (!listIdNativeTemp.isEmpty()) {
                    listIdNativeTemp.remove(0);
                }
                loadNativeAds(activity, listIdNativeTemp, nativeCallback, remoteKey);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                nativeCallback.onAdImpression();
                Log.d(TAG, "NATIVE: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_view");
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                nativeCallback.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "NATIVE: onAdClicked. " + ". " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_click");
            }
        }).build();

        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        if (timeHttpNative != -1) adRequestBuilder.setHttpTimeoutMillis(timeHttpNative);
        AdRequest adRequest = adRequestBuilder.build();
        adLoader.loadAds(adRequest, maxRequest);
    }

    public void loadMultipleNativeAd(Context activity, String idNative, NativeCallback nativeCallback, String remoteKey, int maxRequest) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "NATIVE: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            nativeCallback.onAdFailedToLoad(new LoadAdError(2025, "Check condition", "Check condition", new AdError(2025, "Check condition", "Check condition"), null));
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        AdLoader.Builder builder = new AdLoader.Builder(activity, idNative);
        builder.forNativeAd(nativeAd -> {
            //Tracking revenue
            nativeAd.setOnPaidEventListener(adValue -> {
                //Adjust
                if (nativeAd.getResponseInfo() != null) {
                    AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, idNative, remoteKey);
                }
            });
            Log.i(TAG, "NATIVE: onAdLoaded. " + remoteKey);
            nativeCallback.onNativeAdLoaded(nativeAd);
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "NATIVE: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                Bundle bundle = new Bundle();
                bundle.putString("failed_message", limitString(loadAdError.getMessage(), 99));
                if (loadAdError.getResponseInfo() != null && loadAdError.getResponseInfo().getLoadedAdapterResponseInfo() != null && loadAdError.getMessage().toLowerCase().contains("no fill")) {
                    bundle.putString("no_fill_source", limitString(loadAdError.getResponseInfo().getLoadedAdapterResponseInfo().getAdSourceName(), 99));
                }
                EventTrackingHelper.logEventWithMultipleParams(activity, remoteKey + "_failed", bundle);
                nativeCallback.onAdFailedToLoad(loadAdError);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                nativeCallback.onAdImpression();
                Log.d(TAG, "NATIVE: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_view");
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                nativeCallback.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "NATIVE: onAdClicked. " + ". " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_click");
            }
        }).build();

        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        if (timeHttpNative != -1) adRequestBuilder.setHttpTimeoutMillis(timeHttpNative);
        AdRequest adRequest = adRequestBuilder.build();
        adLoader.loadAds(adRequest, maxRequest);
    }

    public NativeAd loadNativeAds(Context context, List<String> listIdNative, FrameLayout adContainerView, int layoutNative, int layoutNativeMeta, int layoutShimmerNative, boolean setShowNativeAfterLoaded, NativeCallback nativeCallback, IOnAdsImpression iOnAdsImpression, String remoteKey) {
        ArrayList<String> listIdNativeTemp = new ArrayList<>(listIdNative);
        if (adContainerView != null) {
            while (adContainerView.getChildCount() > 0) {
                adContainerView.removeViewAt(0);
            }
        }
        //Check condition
        if (!NetworkUtil.isNetworkActive(context) || listIdNativeTemp.isEmpty() || !AdsConsentManager.getConsentResult(context) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(context, remoteKey)) {
            Log.d(TAG, "NATIVE: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(context) + "_IdEmpty:" + listIdNativeTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(context) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(context, remoteKey));
            nativeCallback.onAdFailedToLoad(new LoadAdError(2025, "Check condition", "Check condition", new AdError(2025, "Check condition", "Check condition"), null));
            return null;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(context, remoteKey + "_true");
        //end log event can request ads

        //Show loading shimmer
        View shimmerNative = LayoutInflater.from(context).inflate(layoutShimmerNative, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerNative);
        }
        AdLoader.Builder builder = new AdLoader.Builder(context, listIdNativeTemp.get(0));
        // OnLoadedListener implementation.
        builder.forNativeAd(nativeAd -> {
            //Tracking revenue
            nativeAd.setOnPaidEventListener(adValue -> {
                //Adjust
                if (nativeAd.getResponseInfo() != null) {
                    AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdNativeTemp.get(0), remoteKey);
                }
            });
            myNativeAd = nativeAd;
            Log.i(TAG, "NATIVE: onAdLoaded. " + remoteKey);
            nativeCallback.onNativeAdLoaded(nativeAd);
            if (setShowNativeAfterLoaded) {
                NativeAdView adView;
                String mediationAdapterClassName = "";
                if (nativeAd.getResponseInfo() != null) {
                    mediationAdapterClassName = nativeAd.getResponseInfo().getMediationAdapterClassName();
                }
                if (mediationAdapterClassName != null && mediationAdapterClassName.toLowerCase().contains("facebook")) {
                    adView = (NativeAdView) LayoutInflater.from(context).inflate(layoutNativeMeta, null);
                } else {
                    adView = (NativeAdView) LayoutInflater.from(context).inflate(layoutNative, null);
                }
                Admob.getInstance().populateNativeAdView(nativeAd, adView);
                if (adContainerView != null) {
                    adContainerView.removeAllViews();
                    adContainerView.addView(adView);
                }
            }
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "NATIVE: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                Bundle bundle = new Bundle();
                bundle.putString("failed_message", limitString(loadAdError.getMessage(), 99));
                if (loadAdError.getResponseInfo() != null && loadAdError.getResponseInfo().getLoadedAdapterResponseInfo() != null && loadAdError.getMessage().toLowerCase().contains("no fill")) {
                    bundle.putString("no_fill_source", limitString(loadAdError.getResponseInfo().getLoadedAdapterResponseInfo().getAdSourceName(), 99));
                }
                EventTrackingHelper.logEventWithMultipleParams(context, remoteKey + "_failed", bundle);
                nativeCallback.onAdFailedToLoad(loadAdError);
                if (!listIdNativeTemp.isEmpty()) {
                    listIdNativeTemp.remove(0);
                }
                loadNativeAds(context, listIdNativeTemp, adContainerView, layoutNative, layoutNativeMeta, layoutShimmerNative, setShowNativeAfterLoaded, nativeCallback, iOnAdsImpression, remoteKey);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                nativeCallback.onAdImpression();
                iOnAdsImpression.onAdsImpression();
                Log.d(TAG, "NATIVE: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(context, remoteKey + "_view");
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                nativeCallback.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "NATIVE: onAdClicked. " + remoteKey);
                EventTrackingHelper.logEvent(context, remoteKey + "_click");
            }
        }).build();

        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        if (timeHttpNative != -1) adRequestBuilder.setHttpTimeoutMillis(timeHttpNative);
        AdRequest adRequest = adRequestBuilder.build();
        adLoader.loadAd(adRequest);

        return myNativeAd;
    }

    public NativeAd loadMultipleNativeAds(Context context, List<String> listIdNative, FrameLayout adContainerView, int layoutNative, int layoutNativeMeta, int layoutShimmerNative, boolean setShowNativeAfterLoaded, NativeCallback nativeCallback, IOnAdsImpression iOnAdsImpression, String remoteKey, int maxRequest) {
        ArrayList<String> listIdNativeTemp = new ArrayList<>(listIdNative);
        if (adContainerView != null) {
            while (adContainerView.getChildCount() > 0) {
                adContainerView.removeViewAt(0);
            }
        }
        //Check condition
        if (!NetworkUtil.isNetworkActive(context) || listIdNativeTemp.isEmpty() || !AdsConsentManager.getConsentResult(context) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(context, remoteKey)) {
            Log.d(TAG, "NATIVE: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(context) + "_IDEmpty:" + listIdNativeTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(context) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(context, remoteKey));
            nativeCallback.onAdFailedToLoad(new LoadAdError(2025, "Check condition", "Check condition", new AdError(2025, "Check condition", "Check condition"), null));
            return null;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(context, remoteKey + "_true");
        //end log event can request ads

        //Show loading shimmer
        View shimmerNative = LayoutInflater.from(context).inflate(layoutShimmerNative, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerNative);
        }
        AdLoader.Builder builder = new AdLoader.Builder(context, listIdNativeTemp.get(0));
        // OnLoadedListener implementation.
        builder.forNativeAd(nativeAd -> {
            //Tracking revenue
            nativeAd.setOnPaidEventListener(adValue -> {
                //Adjust
                if (nativeAd.getResponseInfo() != null) {
                    AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdNativeTemp.get(0), remoteKey);
                }
            });
            myNativeAd = nativeAd;
            Log.i(TAG, "NATIVE: onAdLoaded. " + remoteKey);
            nativeCallback.onNativeAdLoaded(nativeAd);
            if (setShowNativeAfterLoaded) {
                NativeAdView adView;
                String mediationAdapterClassName = "";
                if (nativeAd.getResponseInfo() != null) {
                    mediationAdapterClassName = nativeAd.getResponseInfo().getMediationAdapterClassName();
                }
                if (mediationAdapterClassName != null && mediationAdapterClassName.toLowerCase().contains("facebook")) {
                    adView = (NativeAdView) LayoutInflater.from(context).inflate(layoutNativeMeta, null);
                } else {
                    adView = (NativeAdView) LayoutInflater.from(context).inflate(layoutNative, null);
                }
                Admob.getInstance().populateNativeAdView(nativeAd, adView);
                if (adContainerView != null) {
                    adContainerView.removeView(shimmerNative);
                    adContainerView.addView(adView);
                }
                nativeCallback.onAdShown(adView);
            }
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "NATIVE: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                Bundle bundle = new Bundle();
                bundle.putString("failed_message", limitString(loadAdError.getMessage(), 99));
                if (loadAdError.getResponseInfo() != null && loadAdError.getResponseInfo().getLoadedAdapterResponseInfo() != null && loadAdError.getMessage().toLowerCase().contains("no fill")) {
                    bundle.putString("no_fill_source", limitString(loadAdError.getResponseInfo().getLoadedAdapterResponseInfo().getAdSourceName(), 99));
                }
                EventTrackingHelper.logEventWithMultipleParams(context, remoteKey + "_failed", bundle);
                nativeCallback.onAdFailedToLoad(loadAdError);
                if (!listIdNativeTemp.isEmpty()) {
                    listIdNativeTemp.remove(0);
                }
                loadMultipleNativeAds(context, listIdNativeTemp, adContainerView, layoutNative, layoutNativeMeta, layoutShimmerNative, setShowNativeAfterLoaded, nativeCallback, iOnAdsImpression, remoteKey, maxRequest);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                nativeCallback.onAdImpression();
                iOnAdsImpression.onAdsImpression();
                Log.d(TAG, "NATIVE: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(context, remoteKey + "_view");
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                nativeCallback.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "NATIVE: onAdClicked. " + remoteKey);
                EventTrackingHelper.logEvent(context, remoteKey + "_click");
            }
        }).build();

        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        if (timeHttpNative != -1) adRequestBuilder.setHttpTimeoutMillis(timeHttpNative);
        AdRequest adRequest = adRequestBuilder.build();
        adLoader.loadAds(adRequest, maxRequest);

        return myNativeAd;
    }

    public NativeAd loadMultipleNativeAds1Id(Context context, String idNative, FrameLayout adContainerView, int layoutNative, int layoutNativeMeta, int layoutShimmerNative, boolean setShowNativeAfterLoaded, NativeCallback nativeCallback, IOnAdsImpression iOnAdsImpression, IOnAdsFailToLoad iOnAdsFailToLoad, String remoteKey, int maxRequest) {
        if (adContainerView != null) {
            while (adContainerView.getChildCount() > 0) {
                adContainerView.removeViewAt(0);
            }
        }
        //Check condition
        if (!NetworkUtil.isNetworkActive(context) || idNative.isEmpty() || !AdsConsentManager.getConsentResult(context) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(context, remoteKey)) {
            Log.d(TAG, "NATIVE: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(context) + "_IdEmpty:" + idNative.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(context) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(context, remoteKey));
            nativeCallback.onAdFailedToLoad(new LoadAdError(2025, "Check condition", "Check condition", new AdError(2025, "Check condition", "Check condition"), null));
            return null;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(context, remoteKey + "_true");
        //end log event can request ads

        //Show loading shimmer
        View shimmerNative = LayoutInflater.from(context).inflate(layoutShimmerNative, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerNative);
        }
        AdLoader.Builder builder = new AdLoader.Builder(context, idNative);
        // OnLoadedListener implementation.
        builder.forNativeAd(nativeAd -> {
            //Tracking revenue
            nativeAd.setOnPaidEventListener(adValue -> {
                //Adjust
                if (nativeAd.getResponseInfo() != null) {
                    AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, idNative, remoteKey);
                }
            });
            myNativeAd = nativeAd;
            Log.i(TAG, "NATIVE: onAdLoaded. " + remoteKey);
            nativeCallback.onNativeAdLoaded(nativeAd);
            if (setShowNativeAfterLoaded) {
                NativeAdView adView;
                String mediationAdapterClassName = "";
                if (nativeAd.getResponseInfo() != null) {
                    mediationAdapterClassName = nativeAd.getResponseInfo().getMediationAdapterClassName();
                }
                if (mediationAdapterClassName != null && mediationAdapterClassName.toLowerCase().contains("facebook")) {
                    adView = (NativeAdView) LayoutInflater.from(context).inflate(layoutNativeMeta, null);
                } else {
                    adView = (NativeAdView) LayoutInflater.from(context).inflate(layoutNative, null);
                }
                Admob.getInstance().populateNativeAdView(nativeAd, adView);
                if (adContainerView != null) {
                    adContainerView.removeView(shimmerNative);
                    adContainerView.addView(adView);
                }
                nativeCallback.onAdShown(adView);
            }
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "NATIVE: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                Bundle bundle = new Bundle();
                bundle.putString("failed_message", limitString(loadAdError.getMessage(), 99));
                if (loadAdError.getResponseInfo() != null && loadAdError.getResponseInfo().getLoadedAdapterResponseInfo() != null && loadAdError.getMessage().toLowerCase().contains("no fill")) {
                    bundle.putString("no_fill_source", limitString(loadAdError.getResponseInfo().getLoadedAdapterResponseInfo().getAdSourceName(), 99));
                }
                EventTrackingHelper.logEventWithMultipleParams(context, remoteKey + "_failed", bundle);
                nativeCallback.onAdFailedToLoad(loadAdError);
                iOnAdsFailToLoad.onAdsFailToLoad();
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                nativeCallback.onAdImpression();
                iOnAdsImpression.onAdsImpression();
                Log.d(TAG, "NATIVE: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(context, remoteKey + "_view");
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                nativeCallback.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "NATIVE: onAdClicked. " + remoteKey);
                EventTrackingHelper.logEvent(context, remoteKey + "_click");
            }
        }).build();

        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        if (timeHttpNative != -1) adRequestBuilder.setHttpTimeoutMillis(timeHttpNative);
        AdRequest adRequest = adRequestBuilder.build();
        adLoader.loadAds(adRequest, maxRequest);

        return myNativeAd;
    }

    public void loadNativeAds(Activity activity, List<String> listIdNative, FrameLayout adContainerView, int layoutNative, int layoutNativeMeta, int layoutShimmerNative, boolean setShowNativeAfterLoaded, NativeCallback nativeCallback, String remoteKey) {
        ArrayList<String> listIdNativeTemp = new ArrayList<>(listIdNative);
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdNativeTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "NATIVE: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdNativeTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            nativeCallback.onAdFailedToLoad(new LoadAdError(2025, "Check condition", "Check condition", new AdError(2025, "Check condition", "Check condition"), null));
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        //Show loading shimmer
        View shimmerNative = LayoutInflater.from(activity).inflate(layoutShimmerNative, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerNative);
        }
        AdLoader.Builder builder = new AdLoader.Builder(activity, listIdNativeTemp.get(0));
        // OnLoadedListener implementation.
        builder.forNativeAd(nativeAd -> {
            //Tracking revenue
            nativeAd.setOnPaidEventListener(adValue -> {
                //Adjust
                if (nativeAd.getResponseInfo() != null) {
                    AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdNativeTemp.get(0), remoteKey);
                }
            });
            Log.i(TAG, "NATIVE: onAdLoaded. " + remoteKey);
            nativeCallback.onNativeAdLoaded(nativeAd);
            if (setShowNativeAfterLoaded) {
                NativeAdView adView;
                String mediationAdapterClassName = "";
                if (nativeAd.getResponseInfo() != null) {
                    mediationAdapterClassName = nativeAd.getResponseInfo().getMediationAdapterClassName();
                }
                if (mediationAdapterClassName != null && mediationAdapterClassName.toLowerCase().contains("facebook")) {
                    adView = (NativeAdView) activity.getLayoutInflater().inflate(layoutNativeMeta, adContainerView, false);
                } else {
                    adView = (NativeAdView) activity.getLayoutInflater().inflate(layoutNative, adContainerView, false);
                }
                Admob.getInstance().populateNativeAdView(nativeAd, adView);
                if (adContainerView != null) {
                    adContainerView.removeAllViews();
                    adContainerView.addView(adView);
                }
            }
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "NATIVE: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                Bundle bundle = new Bundle();
                bundle.putString("failed_message", limitString(loadAdError.getMessage(), 99));
                if (loadAdError.getResponseInfo() != null && loadAdError.getResponseInfo().getLoadedAdapterResponseInfo() != null && loadAdError.getMessage().toLowerCase().contains("no fill")) {
                    bundle.putString("no_fill_source", limitString(loadAdError.getResponseInfo().getLoadedAdapterResponseInfo().getAdSourceName(), 99));
                }
                EventTrackingHelper.logEventWithMultipleParams(activity, remoteKey + "_failed", bundle);
                nativeCallback.onAdFailedToLoad(loadAdError);
                if (!listIdNativeTemp.isEmpty()) {
                    listIdNativeTemp.remove(0);
                }
                loadNativeAds(activity, listIdNativeTemp, adContainerView, layoutNative, layoutNativeMeta, layoutShimmerNative, setShowNativeAfterLoaded, nativeCallback, remoteKey);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                nativeCallback.onAdImpression();
                Log.d(TAG, "NATIVE: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_view");
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                nativeCallback.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "NATIVE: onAdClicked. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_click");
            }
        }).build();

        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        if (timeHttpNative != -1) adRequestBuilder.setHttpTimeoutMillis(timeHttpNative);
        AdRequest adRequest = adRequestBuilder.build();
        adLoader.loadAd(adRequest);
    }

    public void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        // Set the media view.
        MediaView mediaView = adView.findViewById(R.id.ad_media);
        if (mediaView != null) {
            adView.setMediaView(mediaView);
        }

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
        adView.setNativeAd(nativeAd);

        // Get the video controller for the ad. One will always be provided, even if the ad doesn't
        // have a video asset.
        VideoController vc = null;
        if (nativeAd.getMediaContent() != null) {
            vc = nativeAd.getMediaContent().getVideoController();
        }
        // Updates the UI to say whether or not this ad has a video asset.
        if (nativeAd.getMediaContent() != null && nativeAd.getMediaContent().hasVideoContent()) {

            // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
            // VideoController will call methods on this object when events occur in the video
            // lifecycle.
            if (vc != null) {
                vc.setVideoLifecycleCallbacks(
                        new VideoController.VideoLifecycleCallbacks() {
                            @Override
                            public void onVideoEnd() {
                                // Publishers should allow native ads to complete video playback before
                                // refreshing or replacing them with another ad in the same UI location.
                                Log.d(TAG, "Video status: Video playback has ended.");
                                super.onVideoEnd();
                            }
                        });
            }
        } else {
            Log.d(TAG, "Video status: Ad does not contain a video asset.");
        }
    }
    //================================End native ads================================

    //================================Start reward ads================================
    public void loadRewardAds(Activity activity, List<String> listIdRewarded, RewardedCallback rewardedCallback, String remoteKey) {
        ArrayList<String> listIdRewardedTemp = new ArrayList<>(listIdRewarded);
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdRewardedTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "REWARD: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdRewardedTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            rewardedCallback.onAdFailedToLoad();
            rewardedCallback.onNextAction();
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(activity, listIdRewardedTemp.get(0),
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "REWARD: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                        rewardedCallback.onAdFailedToLoad();
                        if (!listIdRewardedTemp.isEmpty()) {
                            listIdRewardedTemp.remove(0);
                        }
                        loadRewardAds(activity, listIdRewardedTemp, rewardedCallback, remoteKey);
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        //Tracking revenue
                        ad.setOnPaidEventListener(adValue -> {
                            //Adjust
                            ad.getResponseInfo();
                            AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdRewardedTemp.get(0), remoteKey);
                        });
                        Log.i(TAG, "REWARD: onAdLoaded. " + remoteKey);
                        rewardedCallback.onAdLoaded(ad);
                    }
                });
    }

    public void showReward(Activity activity, RewardedAd rewardedAd, RewardedCallback rewardedCallback, String remoteKey) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "REWARD: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            rewardedCallback.onAdFailedToLoad();
            rewardedCallback.onNextAction();
            return;
        }
        if (rewardedAd == null) {
            Log.d(TAG, "REWARD: The rewarded ad wasn't ready yet.");
            rewardedCallback.onAdFailedToShowFullScreenContent();
            rewardedCallback.onNextAction();
            return;
        }
        loadingAdsDialog = new LoadingAdsDialog(activity);
        if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
            loadingAdsDialog.show();
        }
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "REWARD: onAdClicked. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                rewardedCallback.onAdClicked();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "REWARD: onAdDismissedFullScreenContent. " + remoteKey);
                rewardedCallback.onAdDismissedFullScreenContent();
                rewardedCallback.onNextAction();
                isInterOrRewardedShowing = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "REWARD: onAdFailedToShowFullScreenContent. " + remoteKey);
                rewardedCallback.onAdFailedToShowFullScreenContent();
                rewardedCallback.onNextAction();
                if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    dismissLoadingDialog();
                }
            }

            @Override
            public void onAdImpression() {
                Log.d(TAG, "REWARD: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                rewardedCallback.onAdImpression();
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
            int rewardAmount = rewardItem.getAmount();
            String rewardType = rewardItem.getType();
            rewardedCallback.onUserEarnedReward();
        });
    }

    public void loadAndShowRewardAds(Activity activity, List<String> listIdRewarded, RewardedCallback rewardedCallback, String remoteKey) {
        ArrayList<String> listIdRewardedTemp = new ArrayList<>(listIdRewarded);
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdRewardedTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "REWARD: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdRewardedTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            rewardedCallback.onAdFailedToLoad();
            rewardedCallback.onNextAction();
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        loadingAdsDialog = new LoadingAdsDialog(activity);
        if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
            loadingAdsDialog.show();
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(activity, listIdRewardedTemp.get(0),
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "REWARD: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                        rewardedCallback.onAdFailedToLoad();
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
                        //Tracking revenue
                        ad.setOnPaidEventListener(adValue -> {
                            //Adjust
                            ad.getResponseInfo();
                            AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdRewardedTemp.get(0), remoteKey);
                        });
                        Log.i(TAG, "REWARD: onAdLoaded. " + remoteKey);
                        rewardedCallback.onAdLoaded(ad);
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
            rewardedCallback.onAdFailedToLoad();
            rewardedCallback.onNextAction();
            return;
        }
        if (rewardedAd == null) {
            Log.d(TAG, "REWARD: The rewarded ad wasn't ready yet.");
            rewardedCallback.onAdFailedToShowFullScreenContent();
            rewardedCallback.onNextAction();
            return;
        }
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "REWARD: onAdClicked. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                rewardedCallback.onAdClicked();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "REWARD: onAdDismissedFullScreenContent. " + remoteKey);
                rewardedCallback.onAdDismissedFullScreenContent();
                rewardedCallback.onNextAction();
                isInterOrRewardedShowing = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "REWARD: onAdFailedToShowFullScreenContent. " + remoteKey);
                rewardedCallback.onAdFailedToShowFullScreenContent();
                rewardedCallback.onNextAction();
                if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    dismissLoadingDialog();
                }
            }

            @Override
            public void onAdImpression() {
                Log.d(TAG, "REWARD: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                rewardedCallback.onAdImpression();
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
            int rewardAmount = rewardItem.getAmount();
            String rewardType = rewardItem.getType();
            rewardedCallback.onUserEarnedReward();
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
            Log.d(TAG, "REWARD Ad Preload - listIdRewarded: empty");
            rewardedCallback.onNextAction();
            return;
        }

        //timeout
        isLoadRewardAdsIdTimeout = false;
        runnable = () -> {
            Log.d(TAG, "REWARD Ad Preload - timeout: " + remoteKey);
            EventTrackingHelper.logEventWithAParam(activity, EventTrackingHelper.reward_ads_id_timeout, "remoteKey", remoteKey);
            if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                dismissLoadingDialog();
                rewardedCallback.onNextAction();
                removeHandlerRewardAds();
            }
        };
        handlerTimeoutReward.postDelayed(runnable, timeOutCallRewardAds);
        //
        if (RewardedAdPreloader.isAdAvailable(listIdRewarded.get(0))) {
            Log.d(TAG, "REWARD Ad Preload - loadAndShow: HAVE DATA");
            rewardedCallback.onAdLoaded(null);
            removeHandlerRewardAds();
        } else {
            Log.d(TAG, "REWARD Ad Preload - loadAndShow: NO DATA");
            ArrayList<String> listIdRewardedTemp = new ArrayList<>(listIdRewarded);
            //Check condition
            if (!NetworkUtil.isNetworkActive(activity) || listIdRewardedTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
                Log.d(TAG, "REWARD Ad Preload - loadAndShow: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdRewardedTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
                rewardedCallback.onAdFailedToLoad();
                rewardedCallback.onNextAction();
                removeHandlerRewardAds();
                return;
            }
            //log event can request ads
            EventTrackingHelper.logEvent(activity, remoteKey + "_true");
            //end log event can request ads

            loadingAdsDialog = new LoadingAdsDialog(activity);
            if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
                loadingAdsDialog.show();
            }

            PreloadConfiguration configuration = new PreloadConfiguration.Builder(listIdRewarded.get(0)).setBufferSize(AsyncSplash.Companion.getInstance().getNumberPreloading()).build();

            final AtomicBoolean isFirstLoadAd = new AtomicBoolean(true);

            PreloadCallbackV2 callback = new PreloadCallbackV2() {
                @Override
                public void onAdFailedToPreload(@NonNull String s, @NonNull AdError adError) {
                    Log.d(TAG, "REWARD Ad Preload  - loadAndShow: Preload ad " + s + " failed to load with error: " + adError.getMessage());
                    if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
                    }
                    rewardedCallback.onNextAction();
                    removeHandlerRewardAds();
                }

                @Override
                public void onAdPreloaded(@NonNull String s, @Nullable ResponseInfo responseInfo) {
                    Log.d(TAG, "REWARD Ad Preload  - loadAndShow: Preload ad for " + s + " is available.");
                    if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
                    }

                    if (isFirstLoadAd.getAndSet(false)) {
                        rewardedCallback.onAdLoaded(null);
                        removeHandlerRewardAds();
                    }
                }

                @Override
                public void onAdsExhausted(@NonNull String s) {
                    super.onAdsExhausted(s);
                    Log.d(TAG, "REWARD Ad Preload  - loadAndShow: Preload ad for " + s + " is exhausted.");
                }
            };

            RewardedAdPreloader.start(listIdRewarded.get(0), configuration, callback);
        }
    }

    public void loadRewardAdPreload(Activity activity, List<String> listIdRewarded, RewardedCallback rewardedCallback, String remoteKey) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdRewarded.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "REWARD Ad Preload: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdRewarded.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            rewardedCallback.onAdFailedToLoad();
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        Log.d(TAG, "REWARD Ad Preload: number ad preloading = " + AsyncSplash.Companion.getInstance().getNumberPreloading());

        PreloadConfiguration configuration = new PreloadConfiguration.Builder(listIdRewarded.get(0)).setBufferSize(AsyncSplash.Companion.getInstance().getNumberPreloading()).build();

        PreloadCallbackV2 callback = new PreloadCallbackV2() {
            @Override
            public void onAdFailedToPreload(@NonNull String s, @NonNull AdError adError) {
                Log.d(TAG, "REWARD Ad Preload: Preload ad " + s + " failed to load with error: " + adError.getMessage());
            }

            @Override
            public void onAdPreloaded(@NonNull String s, @Nullable ResponseInfo responseInfo) {
                Log.d(TAG, "REWARD Ad Preload: Preload ad for " + s + " is available.");
            }

            @Override
            public void onAdsExhausted(@NonNull String s) {
                super.onAdsExhausted(s);
                Log.d(TAG, "REWARD Ad Preload: Preload ad for " + s + " is exhausted.");
            }
        };

        RewardedAdPreloader.start(listIdRewarded.get(0), configuration, callback);
    }

    public void showRewardAdPreload(Activity activity, List<String> listIdRewarded, RewardedCallback rewardedCallback, String remoteKey) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "REWARD Ad Preload: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            rewardedCallback.onNextAction();
            return;
        }

        loadingAdsDialog = new LoadingAdsDialog(activity);
        if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
            loadingAdsDialog.show();
        }

        RewardedAd ad = RewardedAdPreloader.pollAd(listIdRewarded.get(0));
        if (ad != null) {
            ad.setOnPaidEventListener(
                    adValue -> {
                        ad.getResponseInfo();
                        AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdRewarded.get(0), remoteKey);
                    }
            );

            ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdClicked() {
                    AppOpenManager.isLastActionClickAd = true;
                    Log.d(TAG, "REWARD Ad Preload: onAdClicked. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                    rewardedCallback.onAdClicked();
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "REWARD Ad Preload: onAdDismissedFullScreenContent. " + remoteKey);
                    rewardedCallback.onAdDismissedFullScreenContent();
                    rewardedCallback.onNextAction();
                    isInterOrRewardedShowing = false;
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.e(TAG, "REWARD Ad Preload: onAdFailedToShowFullScreenContent. " + remoteKey);
                    rewardedCallback.onAdFailedToShowFullScreenContent();
                    rewardedCallback.onNextAction();
                    if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
                    }
                }

                @Override
                public void onAdImpression() {
                    Log.d(TAG, "REWARD Ad Preload: onAdImpression. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                    rewardedCallback.onAdImpression();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "REWARD Ad Preload: onAdShowedFullScreenContent. " + remoteKey);
                    rewardedCallback.onAdShowedFullScreenContent();
                    if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        dismissLoadingDialog();
                    }
                    isInterOrRewardedShowing = true;
                }
            });

            ad.show(activity, rewardItem -> {
                Log.d(TAG, "REWARD Ad Preload: The user earned the reward. " + remoteKey);
                rewardedCallback.onUserEarnedReward();
            });

        } else {
            rewardedCallback.onNextAction();
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
            rewardedInterCallback.onAdFailedToLoad();
            rewardedInterCallback.onNextAction();
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        RewardedInterstitialAd.load(activity, listIdRewardedInterTemp.get(0),
                new AdRequest.Builder().build(), new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedInterstitialAd ad) {
                        //Tracking revenue
                        ad.setOnPaidEventListener(adValue -> {
                            //Adjust
                            ad.getResponseInfo();
                            AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdapterResponseInfo(), adValue, listIdRewardedInterTemp.get(0), remoteKey);
                        });
                        Log.i(TAG, "REWARD INTER: onAdLoaded. " + remoteKey);
                        rewardedInterCallback.onAdLoaded(ad);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "REWARD INTER: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                        rewardedInterCallback.onAdFailedToLoad();
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
            rewardedInterCallback.onAdFailedToLoad();
            rewardedInterCallback.onNextAction();
            return;
        }
        if (rewardedInterstitialAd == null) {
            Log.d(TAG, "REWARD INTER: The rewarded inter ad wasn't ready yet.");
            rewardedInterCallback.onAdFailedToShowFullScreenContent();
            rewardedInterCallback.onNextAction();
            return;
        }
        loadingAdsDialog = new LoadingAdsDialog(activity);
        if (!activity.isFinishing() && !activity.isDestroyed() && !loadingAdsDialog.isShowing()) {
            loadingAdsDialog.show();
        }
        rewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "REWARD INTER: onAdClicked. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                rewardedInterCallback.onAdClicked();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "REWARD INTER: onAdDismissedFullScreenContent. " + remoteKey);
                rewardedInterCallback.onAdDismissedFullScreenContent();
                rewardedInterCallback.onNextAction();
                isInterOrRewardedShowing = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "REWARD INTER: onAdFailedToShowFullScreenContent. " + remoteKey);
                rewardedInterCallback.onAdFailedToShowFullScreenContent();
                rewardedInterCallback.onNextAction();
                if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    dismissLoadingDialog();
                }
            }

            @Override
            public void onAdImpression() {
                Log.d(TAG, "REWARD INTER: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                rewardedInterCallback.onAdImpression();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "REWARD INTER: onAdShowedFullScreenContent. " + remoteKey);
                rewardedInterCallback.onAdShowedFullScreenContent();
                if (!activity.isFinishing() && !activity.isDestroyed() && loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    dismissLoadingDialog();
                }
                isInterOrRewardedShowing = true;
            }
        });
        rewardedInterstitialAd.show(activity, rewardItem -> {
            Log.d(TAG, "REWARD INTER: The user earned the reward. " + remoteKey);
            rewardedInterCallback.onUserEarnedReward();
        });
    }
    //================================End reward inter================================
}