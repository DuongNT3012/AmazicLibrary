package com.amazic.library.ads.admob;

import static com.amazic.library.ads.splash_ads.AsyncSplash.DETECT_TEST_AD;

import android.app.Activity;
import android.content.Context;
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
import com.amazic.library.ads.splash_ads.AsyncSplash;
import com.amazic.library.dialog.LoadingAdsDialog;
import com.amazic.library.iap.IAPManager;
import com.amazic.library.organic.TechManager;
import com.amazic.library.ump.AdsConsentManager;
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
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

import java.util.ArrayList;
import java.util.List;

public class Admob {
    private static Admob INSTANCE;
    private static final String TAG = "Admob";
    public LoadingAdsDialog loadingAdsDialog;
    public int animationDialogRaw = R.raw.custom_loading;
    private boolean isCustomAnimationDialog = false;
    private boolean isInterOrRewardedShowing = false;
    private boolean isShowAllAds = true;
    private InterstitialAd mInterstitialAdSplash;
    private boolean isFailToShowAdSplash = false;
    private long timeInterval = 0L;
    private long lastTimeDismissInter = 0L;
    private long timeIntervalFromStart = 0L;
    private long timeStart = 0L;
    private String tokenEventAdjust = "";
    private Handler handlerTimeoutSplash = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private boolean isSplashResume = true;
    private boolean openActivityAfterShowInterAds = true;
    private boolean isDetectTestAdByView = false;
    private int countClickInterSplashAds = 0;
    private NativeAd myNativeAd = null;
    private int timeOutCallAds = 12000;

    public static Admob getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Admob();
        }
        return INSTANCE;
    }

    public void initAdmob(Activity activity, IOnInitAdmobDone iOnInitAdmobDone) {
        initLoadingDialog(activity);
        new Thread(() -> {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(activity, initializationStatus -> {
                Log.d(TAG, "initAdmob: " + initializationStatus.getAdapterStatusMap());
                iOnInitAdmobDone.onInitAdmobDone();
            });
        }).start();
    }

    public void initLoadingDialog(Context context) {
        if (loadingAdsDialog == null) {
            loadingAdsDialog = new LoadingAdsDialog(context);
        }
    }

    public boolean checkCondition(Context context, String adsKey) {
        Log.d(TAG, "checkCondition: Network_" + NetworkUtil.isNetworkActive(context) + "_UMP_" + AdsConsentManager.getConsentResult(context) + "_showAllAds_" + isShowAllAds + "_IAP_" + IAPManager.getInstance().isPurchase() + "_RemoteConfig_" + RemoteConfigHelper.getInstance().get_config(context, adsKey));
        return NetworkUtil.isNetworkActive(context) && AdsConsentManager.getConsentResult(context) && isShowAllAds && !IAPManager.getInstance().isPurchase() && RemoteConfigHelper.getInstance().get_config(context, adsKey);
    }

    public boolean isCustomAnimationDialog() {
        return isCustomAnimationDialog;
    }

    public void setCustomAnimationDialog(boolean customAnimationDialog, int animationDialogRaw) {
        this.isCustomAnimationDialog = customAnimationDialog;
        this.animationDialogRaw = animationDialogRaw;
    }

    public void setCustomAnimationDialog(boolean customAnimationDialog) {
        this.isCustomAnimationDialog = customAnimationDialog;
    }

    public InterstitialAd getInterstitialAdSplash() {
        return mInterstitialAdSplash;
    }

    public void setInterstitialAdSplash(InterstitialAd mInterstitialAdSplash) {
        this.mInterstitialAdSplash = mInterstitialAdSplash;
    }

    public int getTimeOutCallAds() {
        return timeOutCallAds;
    }

    public void setTimeOutCallAds(int timeOutCallAds) {
        this.timeOutCallAds = timeOutCallAds;
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

    public void setTimeInterval(long timeInterval) {
        this.lastTimeDismissInter = 0L;
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

    //================================Start inter ads================================
    public void loadInterAdsLoadAndShow(Activity activity, List<String> listIdInter, InterCallback interCallback, String remoteKey) {
        ArrayList<String> listIdInterTemp = new ArrayList<>(listIdInter);
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "INTER: Check condition. RemoteKey:" + remoteKey + ". Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdInterTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                loadingAdsDialog.dismiss();
            }
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
        loadingAdsDialog = new LoadingAdsDialog(activity);
        if (!loadingAdsDialog.isShowing()) {
            loadingAdsDialog.show();
        }
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(activity, listIdInterTemp.get(0), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        interCallback.onAdLoaded(interstitialAd);
                        Log.i(TAG, "INTER: onAdLoaded. " + remoteKey);
                        showInterAdsLoadAndShow(activity, interstitialAd, interCallback, remoteKey);
                        //Tracking revenue
                        interstitialAd.setOnPaidEventListener(adValue -> {
                            //Adjust
                            AdjustUtil.trackRevenue(interstitialAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.e(TAG, "INTER: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                        interCallback.onAdFailedToLoad();
                        if (!listIdInterTemp.isEmpty()) {
                            listIdInterTemp.remove(0);
                        }
                        if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                            loadingAdsDialog.dismiss();
                        }
                        loadInterAdsLoadAndShow(activity, listIdInterTemp, interCallback, remoteKey);
                    }
                });
    }

    public void showInterAdsLoadAndShow(Activity activity, InterstitialAd mInterstitialAd, InterCallback interCallback, String remoteKey) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "INTER: Check condition. RemoteKey:" + remoteKey + ". Network:" + NetworkUtil.isNetworkActive(activity) + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                loadingAdsDialog.dismiss();
            }
            interCallback.onNextAction();
            return;
        }
        if (mInterstitialAd == null) {
            Log.d(TAG, "INTER: The interstitial ad wasn't ready yet. " + remoteKey);
            if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                loadingAdsDialog.dismiss();
            }
            interCallback.onNextAction();
            return;
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
                    if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        loadingAdsDialog.dismiss();
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
                    if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        loadingAdsDialog.dismiss();
                    }
                    isInterOrRewardedShowing = true;
                }
            });
            isInterOrRewardedShowing = true;
            if (openActivityAfterShowInterAds) {
                interCallback.onNextAction();
            }
            mInterstitialAd.show(activity);
        }, 250);
    }

    public void loadInterAds(Context context, List<String> listIdInter, InterCallback interCallback, String remoteKey) {
        ArrayList<String> listIdInterTemp = new ArrayList<>(listIdInter);
        //Check condition
        if (!NetworkUtil.isNetworkActive(context) || listIdInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(context) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(context, remoteKey)) {
            Log.d(TAG, "INTER: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(context) + "_IdEmpty:" + listIdInterTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(context) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(context, remoteKey));
            interCallback.onNextAction();
            return;
        }
        EventTrackingHelper.logEvent(context, remoteKey + "_true");
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, listIdInterTemp.get(0), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        interCallback.onAdLoaded(interstitialAd);
                        Log.i(TAG, "INTER: onAdLoaded. " + remoteKey);
                        //Tracking revenue
                        interstitialAd.setOnPaidEventListener(adValue -> {
                            //Adjust
                            AdjustUtil.trackRevenue(interstitialAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                        });
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

    public void showInterAds(Activity activity, InterstitialAd mInterstitialAd, InterCallback interCallback, String remoteKey) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "INTER: Check condition. RemoteKey:" + remoteKey + ". Network:" + NetworkUtil.isNetworkActive(activity) + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
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
        loadingAdsDialog = new LoadingAdsDialog(activity);
        if (!loadingAdsDialog.isShowing()) {
            loadingAdsDialog.show();
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
                    if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        loadingAdsDialog.dismiss();
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
                    if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        loadingAdsDialog.dismiss();
                    }
                    isInterOrRewardedShowing = true;
                }
            });
            isInterOrRewardedShowing = true;
            if (openActivityAfterShowInterAds) {
                interCallback.onNextAction();
            }
            mInterstitialAd.show(activity);
        }, 250);
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
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    //increase splash open
                    SharePreferenceHelper.setInt(activity, EventTrackingHelper.splash_open, SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1);
                    //end increase splash open
                    Log.e(TAG, "SPLASH: Ad failed to show fullscreen content.");
                    interCallback.onAdFailedToShowFullScreenContent();
                    if (isSplashResume && !openActivityAfterShowInterAds) {
                        interCallback.onNextAction();
                    }
                    if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        loadingAdsDialog.dismiss();
                    }
                    isFailToShowAdSplash = true;
                    isInterOrRewardedShowing = false;
                    if (handlerTimeoutSplash != null && runnable != null) {
                        handlerTimeoutSplash.removeCallbacks(runnable);
                    }
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
                    if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        loadingAdsDialog.dismiss();
                    }
                    isInterOrRewardedShowing = true;
                    isFailToShowAdSplash = false;
                    if (handlerTimeoutSplash != null && runnable != null) {
                        handlerTimeoutSplash.removeCallbacks(runnable);
                    }
                }
            });
            boolean isResumeState = ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
            Log.d(TAG, "SPLASH: ResumeState: " + isResumeState);
            if (isResumeState) {
                loadingAdsDialog = new LoadingAdsDialog(activity);
                if (!loadingAdsDialog.isShowing() && !activity.isDestroyed()) {
                    loadingAdsDialog.show();
                }
                isInterOrRewardedShowing = true;
                AppOpenManager.getInstance().setEnableResume(false);
                if (openActivityAfterShowInterAds) {
                    Log.d(TAG, "SPLASH: showInterAdsSplash: openActivityAfterShowInterAds = true, onNextAction");
                    interCallback.onNextAction();
                }
                mInterstitialAdSplash.show(activity);
            } else {
                Log.e(TAG, "SPLASH: Fail to show on background.");
                if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    loadingAdsDialog.dismiss();
                }
                isFailToShowAdSplash = true;
                if (handlerTimeoutSplash != null && runnable != null) {
                    handlerTimeoutSplash.removeCallbacks(runnable);
                }
            }
        }, 250);
    }

    public void loadAndShowInterAdSplash(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback) {
        ArrayList<String> listIdInterTemp = new ArrayList<>(listIdInter);
        //Set timeout ads splash 20s if cannot load
        runnable = () -> {
            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_id_timeout);
            if (interCallback != null) {
                interCallback.onNextAction();
            }
            if (handlerTimeoutSplash != null) {
                handlerTimeoutSplash = null;
            }
        };
        if (handlerTimeoutSplash != null) {
            handlerTimeoutSplash.postDelayed(runnable, timeOutCallAds);
        }

        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase()) {
            Log.d(TAG, "Check condition loadAndShowInterAdSplash " + NetworkUtil.isNetworkActive(activity) + "_" + listIdInterTemp.isEmpty() + "_" + AdsConsentManager.getConsentResult(activity) + "_" + isShowAllAds + "_" + IAPManager.getInstance().isPurchase());
            interCallback.onNextAction();
            if (handlerTimeoutSplash != null && runnable != null) {
                handlerTimeoutSplash.removeCallbacks(runnable);
                handlerTimeoutSplash.removeCallbacksAndMessages(null);
                handlerTimeoutSplash = null;
            }
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

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(activity, listIdInterTemp.get(0), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        Log.i(TAG, "SPLASH: Ad was loaded inter splash.");
                        interCallback.onAdLoaded(interstitialAd);
                        mInterstitialAdSplash = interstitialAd;
                        showInterAdsSplash(activity, interCallback);

                        if (handlerTimeoutSplash != null && runnable != null) {
                            handlerTimeoutSplash.removeCallbacks(runnable);
                            handlerTimeoutSplash.removeCallbacksAndMessages(null);
                            handlerTimeoutSplash = null;
                        }
                        //Tracking revenue
                        interstitialAd.setOnPaidEventListener(adValue -> {
                            //Adjust
                            AdjustUtil.trackRevenue(interstitialAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                        });
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

    public void loadAndShowInterAdSplashLoop(AppCompatActivity activity, List<String> listIdInter, InterCallback interCallback) {
        Log.d(TAG, "SPLASH: loadAndShowInterAdSplashLoop. " + listIdInter.toString());
        //Set timeout ads splash 20s if cannot load
        runnable = () -> {
            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_id_timeout);
            if (interCallback != null) {
                interCallback.onNextAction();
            }
            if (handlerTimeoutSplash != null) {
                handlerTimeoutSplash = null;
            }
        };
        if (handlerTimeoutSplash != null) {
            handlerTimeoutSplash.postDelayed(runnable, timeOutCallAds);
        }
        // Check list id size
        if (listIdInter.isEmpty()) {
            Log.d(TAG, "SPLASH: loadAndShowInterAdSplashLoop: listIdInter is empty.");
            interCallback.onNextAction();
            if (handlerTimeoutSplash != null && runnable != null) {
                handlerTimeoutSplash.removeCallbacks(runnable);
                handlerTimeoutSplash.removeCallbacksAndMessages(null);
                handlerTimeoutSplash = null;
            }
            return;
        }
        String idInterSplash = listIdInter.get(0);

        // If have action startActivity by timeout or no internet in splash, do not load ads.
        if (System.currentTimeMillis() - Admob.getInstance().getTimeStart() >= 8000 || AsyncSplash.Companion.getInstance().getTimeout() || AsyncSplash.Companion.getInstance().getNoInternetAction()) {
            Log.d(TAG, "SPLASH: If have action startActivity by timeout or no internet in splash, do not load ads. " + (System.currentTimeMillis() - Admob.getInstance().getTimeStart() >= 8000) + "_" + AsyncSplash.Companion.getInstance().getTimeout() + "_" + AsyncSplash.Companion.getInstance().getNoInternetAction());
            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_id_timeout_8s);
            interCallback.onNextAction();
            if (handlerTimeoutSplash != null && runnable != null) {
                handlerTimeoutSplash.removeCallbacks(runnable);
                handlerTimeoutSplash.removeCallbacksAndMessages(null);
                handlerTimeoutSplash = null;
            }
            return;
        }

        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || idInterSplash.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase()) {
            Log.d(TAG, "SPLASH: Check condition loadAndShowInterAdSplash. Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + idInterSplash.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase());
            interCallback.onNextAction();
            if (handlerTimeoutSplash != null && runnable != null) {
                handlerTimeoutSplash.removeCallbacks(runnable);
                handlerTimeoutSplash.removeCallbacksAndMessages(null);
                handlerTimeoutSplash = null;
            }
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

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(activity, idInterSplash, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        Log.i(TAG, "SPLASH: Ad was loaded inter splash loop. " + idInterSplash);
                        interCallback.onAdLoaded(interstitialAd);
                        mInterstitialAdSplash = interstitialAd;
                        showInterAdsSplash(activity, interCallback);

                        if (handlerTimeoutSplash != null && runnable != null) {
                            handlerTimeoutSplash.removeCallbacks(runnable);
                            handlerTimeoutSplash.removeCallbacksAndMessages(null);
                            handlerTimeoutSplash = null;
                        }
                        //Tracking revenue
                        interstitialAd.setOnPaidEventListener(adValue -> {
                            //Adjust
                            AdjustUtil.trackRevenue(interstitialAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.e(TAG, "SPLASH: Ad Failed To Load." + loadAdError);
                        interCallback.onAdFailedToLoad();
                        loadAndShowInterAdSplashLoop(activity, listIdInter, interCallback);
                    }
                });
    }

    public void onCheckShowSplashWhenFail(AppCompatActivity activity, InterCallback interCallback) {
        if (isFailToShowAdSplash) {
            Log.d(TAG, "SPLASH: onCheckShowSplashWhenFail.");
            showInterAdsSplash(activity, interCallback);
        }
    }

    //================================end inter ads================================

    //================================Start banner ads================================
    public void loadBannerAds(Activity activity, List<String> listIdBanner, FrameLayout adContainerView, BannerCallback bannerCallback, IOnAdsImpression iOnAdsImpression, String remoteKey) {
        ArrayList<String> listIdBannerTemp = new ArrayList<>(listIdBanner);
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdBannerTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "BANNER: Check condition: RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdBannerTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
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
        AdRequest adRequest = new AdRequest.Builder().build();
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
                    Log.d(TAG, "BANNER: onAdImpression. isTestAd: " + isTestAd);
                    TechManager.getInstance().detectedTech(activity, isTestAd);

                    if (AsyncSplash.Companion.getInstance().getUserTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)
                            && TechManager.getInstance().isTech(activity)
                            && !AsyncSplash.Companion.getInstance().getDebug()) {
                        AsyncSplash.Companion.getInstance().turnOffSomeRemoteKeys(activity);
                    }
                }

                //Tracking revenue
                adView.setOnPaidEventListener(adValue -> {
                    //Adjust
                    if (adView.getResponseInfo() != null) {
                        AdjustUtil.trackRevenue(adView.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                    }
                });
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

    //can load banner ads in fragment
    public void loadBannerAds(Context context, int adWidth, List<String> listIdBanner, FrameLayout adContainerView, BannerCallback bannerCallback, IOnAdsImpression iOnAdsImpression, String remoteKey) {
        ArrayList<String> listIdBannerTemp = new ArrayList<>(listIdBanner);
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check network
        if (!NetworkUtil.isNetworkActive(context) || listIdBannerTemp.isEmpty() || !AdsConsentManager.getConsentResult(context) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(context, remoteKey)) {
            Log.d(TAG, "BANNER: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(context) + "_IdEmpty:" + listIdBannerTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(context) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(context, remoteKey));
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
        AdRequest adRequest = new AdRequest.Builder().build();
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
                    Log.d(TAG, "BANNER: onAdImpression. isTestAd: " + isTestAd);
                    TechManager.getInstance().detectedTech(context, isTestAd);

                    if (AsyncSplash.Companion.getInstance().getUserTechManagerOrDetectTestAd().equals(DETECT_TEST_AD)
                            && TechManager.getInstance().isTech(context)
                            && !AsyncSplash.Companion.getInstance().getDebug()) {
                        AsyncSplash.Companion.getInstance().turnOffSomeRemoteKeys(context);
                    }
                }
                //Tracking revenue
                adView.setOnPaidEventListener(adValue -> {
                    //Adjust
                    if (adView.getResponseInfo() != null) {
                        AdjustUtil.trackRevenue(adView.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                    }
                });
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
    //end can load banner ads in fragment
    //================================End banner ads================================

    //================================Start collapse banner ads================================
    public AdView loadCollapseBanner(Activity activity, List<String> listIdCollapseBanner, FrameLayout adContainerView, boolean isGravityBottom, BannerCallback bannerCallback, IOnAdsImpression iOnAdsImpression, String collapseTypeClose, long valueCountDownOrCountClick, String remoteKey) {
        ArrayList<String> listIdCollapseBannerTemp = new ArrayList<>(listIdCollapseBanner);
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdCollapseBannerTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "COLLAPSE BANNER: Check condition. RemoteKey:" + remoteKey + "_Network: " + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdCollapseBannerTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
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
                if (!listIdCollapseBannerTemp.isEmpty()) {
                    listIdCollapseBannerTemp.remove(0);
                }
                loadCollapseBanner(activity, listIdCollapseBannerTemp, adContainerView, isGravityBottom, bannerCallback, iOnAdsImpression, collapseTypeClose, valueCountDownOrCountClick, remoteKey);
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
                Log.i(TAG, "COLLAPSE BANNER: onAdLoaded. " + remoteKey);
                bannerCallback.onAdLoaded();
                // Replace ad container with new ad view.
                if (adContainerView != null) {
                    adContainerView.removeAllViews();
                    adContainerView.addView(adView);
                }
                //Tracking revenue
                adView.setOnPaidEventListener(adValue -> {
                    //Adjust
                    if (adView.getResponseInfo() != null) {
                        AdjustUtil.trackRevenue(adView.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                    }
                });
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

    public AdView loadCollapseBanner(Context context, int adWidth, List<String> listIdCollapseBanner, FrameLayout adContainerView, boolean isGravityBottom, BannerCallback bannerCallback, IOnAdsImpression iOnAdsImpression, String collapseTypeClose, long valueCountDownOrCountClick, String remoteKey) {
        ArrayList<String> listIdCollapseBannerTemp = new ArrayList<>(listIdCollapseBanner);
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check condition
        if (!NetworkUtil.isNetworkActive(context) || listIdCollapseBannerTemp.isEmpty() || !AdsConsentManager.getConsentResult(context) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(context, remoteKey)) {
            Log.d(TAG, "COLLAPSE BANNER: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(context) + "_IdEmpty:" + listIdCollapseBannerTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(context) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(context, remoteKey));
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
                if (!listIdCollapseBannerTemp.isEmpty()) {
                    listIdCollapseBannerTemp.remove(0);
                }
                loadCollapseBanner(context, adWidth, listIdCollapseBannerTemp, adContainerView, isGravityBottom, bannerCallback, iOnAdsImpression, collapseTypeClose, valueCountDownOrCountClick, remoteKey);
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
                Log.i(TAG, "COLLAPSE BANNER: onAdLoaded. " + remoteKey);
                bannerCallback.onAdLoaded();
                // Replace ad container with new ad view.
                if (adContainerView != null) {
                    adContainerView.removeAllViews();
                    adContainerView.addView(adView);
                }
                //Tracking revenue
                adView.setOnPaidEventListener(adValue -> {
                    //Adjust
                    if (adView.getResponseInfo() != null) {
                        AdjustUtil.trackRevenue(adView.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                    }
                });
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
    public void loadNativeAds(Activity activity, List<String> listIdNative, NativeCallback nativeCallback, String remoteKey) {
        ArrayList<String> listIdNativeTemp = new ArrayList<>(listIdNative);
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdNativeTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "NATIVE: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdNativeTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            nativeCallback.onAdFailedToLoad(new LoadAdError(2025, "Check condition", "Check condition", new AdError(2025, "Check condition", "Check condition"), null));
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        AdLoader.Builder builder = new AdLoader.Builder(activity, listIdNativeTemp.get(0));
        builder.forNativeAd(nativeAd -> {
            Log.i(TAG, "NATIVE: onAdLoaded. " + remoteKey);
            nativeCallback.onNativeAdLoaded(nativeAd);
            //Tracking revenue
            nativeAd.setOnPaidEventListener(adValue -> {
                //Adjust
                if (nativeAd.getResponseInfo() != null) {
                    AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                }
            });
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "NATIVE: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                EventTrackingHelper.logEventWithAParam(activity, remoteKey + "_failed", "failed_message", limitString(loadAdError.getMessage(), 40));
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

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public void loadMultipleNativeAds(Activity activity, List<String> listIdNative, NativeCallback nativeCallback, String remoteKey, int maxRequest) {
        ArrayList<String> listIdNativeTemp = new ArrayList<>(listIdNative);
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdNativeTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "NATIVE: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdNativeTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            nativeCallback.onAdFailedToLoad(new LoadAdError(2025, "Check condition", "Check condition", new AdError(2025, "Check condition", "Check condition"), null));
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        AdLoader.Builder builder = new AdLoader.Builder(activity, listIdNativeTemp.get(0));
        builder.forNativeAd(nativeAd -> {
            Log.i(TAG, "NATIVE: onAdLoaded. " + remoteKey);
            nativeCallback.onNativeAdLoaded(nativeAd);
            //Tracking revenue
            nativeAd.setOnPaidEventListener(adValue -> {
                //Adjust
                if (nativeAd.getResponseInfo() != null) {
                    AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                }
            });
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "NATIVE: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                nativeCallback.onAdFailedToLoad(loadAdError);
                EventTrackingHelper.logEventWithAParam(activity, remoteKey + "_failed", "failed_message", limitString(loadAdError.getMessage(), 40));
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

        adLoader.loadAds(new AdRequest.Builder().build(), maxRequest);
    }

    public void loadMultipleNativeAd(Activity activity, String idNative, NativeCallback nativeCallback, String remoteKey, int maxRequest) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "NATIVE: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            nativeCallback.onAdFailedToLoad(new LoadAdError(2025, "Check condition", "Check condition", new AdError(2025, "Check condition", "Check condition"), null));
            return;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        AdLoader.Builder builder = new AdLoader.Builder(activity, idNative);
        builder.forNativeAd(nativeAd -> {
            Log.i(TAG, "NATIVE: onAdLoaded. " + remoteKey);
            nativeCallback.onNativeAdLoaded(nativeAd);
            //Tracking revenue
            nativeAd.setOnPaidEventListener(adValue -> {
                //Adjust
                if (nativeAd.getResponseInfo() != null) {
                    AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                }
            });
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "NATIVE: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                nativeCallback.onAdFailedToLoad(loadAdError);
                EventTrackingHelper.logEventWithAParam(activity, remoteKey + "_failed", "failed_message", limitString(loadAdError.getMessage(), 40));
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

        adLoader.loadAds(new AdRequest.Builder().build(), maxRequest);
    }

    public NativeAd loadNativeAds(Activity activity, List<String> listIdNative, FrameLayout adContainerView, int layoutNative, int layoutNativeMeta, int layoutShimmerNative, boolean setShowNativeAfterLoaded, NativeCallback nativeCallback, IOnAdsImpression iOnAdsImpression, String remoteKey) {
        ArrayList<String> listIdNativeTemp = new ArrayList<>(listIdNative);
        if (adContainerView != null) {
            while (adContainerView.getChildCount() > 0) {
                adContainerView.removeViewAt(0);
            }
        }
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdNativeTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "NATIVE: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdNativeTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            nativeCallback.onAdFailedToLoad(new LoadAdError(2025, "Check condition", "Check condition", new AdError(2025, "Check condition", "Check condition"), null));
            return null;
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
            //Tracking revenue
            nativeAd.setOnPaidEventListener(adValue -> {
                //Adjust
                if (nativeAd.getResponseInfo() != null) {
                    AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                }
            });
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "NATIVE: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                EventTrackingHelper.logEventWithAParam(activity, remoteKey + "_failed", "failed_message", limitString(loadAdError.getMessage(), 40));
                nativeCallback.onAdFailedToLoad(loadAdError);
                if (!listIdNativeTemp.isEmpty()) {
                    listIdNativeTemp.remove(0);
                }
                loadNativeAds(activity, listIdNativeTemp, adContainerView, layoutNative, layoutNativeMeta, layoutShimmerNative, setShowNativeAfterLoaded, nativeCallback, iOnAdsImpression, remoteKey);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                nativeCallback.onAdImpression();
                iOnAdsImpression.onAdsImpression();
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

        adLoader.loadAd(new AdRequest.Builder().build());

        return myNativeAd;
    }

    public static String limitString(String str, int maxLength) {
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    public NativeAd loadMultipleNativeAds(Activity activity, List<String> listIdNative, FrameLayout adContainerView, int layoutNative, int layoutNativeMeta, int layoutShimmerNative, boolean setShowNativeAfterLoaded, NativeCallback nativeCallback, IOnAdsImpression iOnAdsImpression, String remoteKey, int maxRequest) {
        ArrayList<String> listIdNativeTemp = new ArrayList<>(listIdNative);
        if (adContainerView != null) {
            while (adContainerView.getChildCount() > 0) {
                adContainerView.removeViewAt(0);
            }
        }
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdNativeTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "NATIVE: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IDEmpty:" + listIdNativeTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            nativeCallback.onAdFailedToLoad(new LoadAdError(2025, "Check condition", "Check condition", new AdError(2025, "Check condition", "Check condition"), null));
            return null;
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
                    adView = (NativeAdView) activity.getLayoutInflater().inflate(layoutNativeMeta, adContainerView, false);
                } else {
                    adView = (NativeAdView) activity.getLayoutInflater().inflate(layoutNative, adContainerView, false);
                }
                Admob.getInstance().populateNativeAdView(nativeAd, adView);
                if (adContainerView != null) {
                    adContainerView.removeView(shimmerNative);
                    adContainerView.addView(adView);
                }
                nativeCallback.onAdShown(adView);
            }
            //Tracking revenue
            nativeAd.setOnPaidEventListener(adValue -> {
                //Adjust
                if (nativeAd.getResponseInfo() != null) {
                    AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                }
            });
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "NATIVE: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                EventTrackingHelper.logEventWithAParam(activity, remoteKey + "_failed", "failed_message", limitString(loadAdError.getMessage(), 40));
                nativeCallback.onAdFailedToLoad(loadAdError);
                if (!listIdNativeTemp.isEmpty()) {
                    listIdNativeTemp.remove(0);
                }
                loadMultipleNativeAds(activity, listIdNativeTemp, adContainerView, layoutNative, layoutNativeMeta, layoutShimmerNative, setShowNativeAfterLoaded, nativeCallback, iOnAdsImpression, remoteKey, maxRequest);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                nativeCallback.onAdImpression();
                iOnAdsImpression.onAdsImpression();
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

        adLoader.loadAds(new AdRequest.Builder().build(), maxRequest);

        return myNativeAd;
    }

    public NativeAd loadMultipleNativeAds1Id(Activity activity, String idNative, FrameLayout adContainerView, int layoutNative, int layoutNativeMeta, int layoutShimmerNative, boolean setShowNativeAfterLoaded, NativeCallback nativeCallback, IOnAdsImpression iOnAdsImpression, IOnAdsFailToLoad iOnAdsFailToLoad, String remoteKey, int maxRequest) {
        if (adContainerView != null) {
            while (adContainerView.getChildCount() > 0) {
                adContainerView.removeViewAt(0);
            }
        }
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || idNative.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "NATIVE: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + idNative.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            nativeCallback.onAdFailedToLoad(new LoadAdError(2025, "Check condition", "Check condition", new AdError(2025, "Check condition", "Check condition"), null));
            return null;
        }
        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        //Show loading shimmer
        View shimmerNative = LayoutInflater.from(activity).inflate(layoutShimmerNative, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerNative);
        }
        AdLoader.Builder builder = new AdLoader.Builder(activity, idNative);
        // OnLoadedListener implementation.
        builder.forNativeAd(nativeAd -> {
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
                    adView = (NativeAdView) activity.getLayoutInflater().inflate(layoutNativeMeta, adContainerView, false);
                } else {
                    adView = (NativeAdView) activity.getLayoutInflater().inflate(layoutNative, adContainerView, false);
                }
                Admob.getInstance().populateNativeAdView(nativeAd, adView);
                if (adContainerView != null) {
                    adContainerView.removeView(shimmerNative);
                    adContainerView.addView(adView);
                }
                nativeCallback.onAdShown(adView);
            }
            //Tracking revenue
            nativeAd.setOnPaidEventListener(adValue -> {
                //Adjust
                if (nativeAd.getResponseInfo() != null) {
                    AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                }
            });
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "NATIVE: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                EventTrackingHelper.logEventWithAParam(activity, remoteKey + "_failed", "failed_message", limitString(loadAdError.getMessage(), 40));
                nativeCallback.onAdFailedToLoad(loadAdError);
                iOnAdsFailToLoad.onAdsFailToLoad();
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                nativeCallback.onAdImpression();
                iOnAdsImpression.onAdsImpression();
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

        adLoader.loadAds(new AdRequest.Builder().build(), maxRequest);

        return myNativeAd;
    }

    public void loadNativeAds(Activity activity, List<String> listIdNative, FrameLayout adContainerView, int layoutNative, int layoutNativeMeta, int layoutShimmerNative, boolean setShowNativeAfterLoaded, NativeCallback nativeCallback, String remoteKey) {
        ArrayList<String> listIdNativeTemp = new ArrayList<>(listIdNative);
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdNativeTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "NATIVE: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdNativeTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
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
            //Tracking revenue
            nativeAd.setOnPaidEventListener(adValue -> {
                //Adjust
                if (nativeAd.getResponseInfo() != null) {
                    AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                }
            });
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "NATIVE: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                EventTrackingHelper.logEventWithAParam(activity, remoteKey + "_failed", "failed_message", limitString(loadAdError.getMessage(), 40));
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

        adLoader.loadAd(new AdRequest.Builder().build());
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
        if (!NetworkUtil.isNetworkActive(activity) || listIdRewardedTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "REWARD: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdRewardedTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
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
                        Log.i(TAG, "REWARD: onAdLoaded. " + remoteKey);
                        rewardedCallback.onAdLoaded(ad);
                        //Tracking revenue
                        ad.setOnPaidEventListener(adValue -> {
                            //Adjust
                            ad.getResponseInfo();
                            AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                        });
                    }
                });
    }

    public void showReward(Activity activity, RewardedAd rewardedAd, RewardedCallback rewardedCallback, String remoteKey) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "REWARD: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
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
        if (!loadingAdsDialog.isShowing() && !activity.isDestroyed()) {
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
                if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    loadingAdsDialog.dismiss();
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
                if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    loadingAdsDialog.dismiss();
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
    //================================End reward ads================================

    //================================Start reward inter================================
    public void loadRewardInterAds(Activity activity, List<String> listIdRewardedInter, RewardedInterCallback rewardedInterCallback, String remoteKey) {
        ArrayList<String> listIdRewardedInterTemp = new ArrayList<>(listIdRewardedInter);
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdRewardedInterTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "REWARD INTER: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdRewardedInterTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
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
                        Log.i(TAG, "REWARD INTER: onAdLoaded. " + remoteKey);
                        rewardedInterCallback.onAdLoaded(ad);
                        //Tracking revenue
                        ad.setOnPaidEventListener(adValue -> {
                            //Adjust
                            ad.getResponseInfo();
                            AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                        });
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
        if (!NetworkUtil.isNetworkActive(activity) || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "REWARD INTER: Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + isShowAllAds + "_IAP:" + IAPManager.getInstance().isPurchase() + "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
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
        if (!loadingAdsDialog.isShowing() && !activity.isDestroyed()) {
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
                if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    loadingAdsDialog.dismiss();
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
                if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    loadingAdsDialog.dismiss();
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
