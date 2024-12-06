package com.amazic.library.ads.admob;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
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

import java.util.List;
import java.util.Locale;

public class Admob {
    private static Admob INSTANCE;
    private static final String TAG = "Admob";
    private LoadingAdsDialog loadingAdsDialog;
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

    public static Admob getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Admob();
        }
        return INSTANCE;
    }

    public void initAdmob(Activity activity, IOnInitAdmobDone iOnInitAdmobDone) {
        new Thread(() -> {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(activity, initializationStatus -> {
                Log.d(TAG, "initAdmob: " + initializationStatus.getAdapterStatusMap());
                iOnInitAdmobDone.onInitAdmobDone();
            });
        }).start();
    }

    public boolean isDetectTestAdByView() {
        return isDetectTestAdByView;
    }

    public void setDetectTestAdByView(boolean detectTestAdByView) {
        isDetectTestAdByView = detectTestAdByView;
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
    public void loadInterAds(Context context, List<String> listIdInter, InterCallback interCallback, String adsKey) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(context) || listIdInter.isEmpty() || !AdsConsentManager.getConsentResult(context) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(context, adsKey)) {
            interCallback.onNextAction();
            return;
        }
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(context, listIdInter.get(0), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        interCallback.onAdLoaded(interstitialAd);
                        Log.i(TAG, "onAdLoaded");
                        //Tracking revenue
                        interstitialAd.setOnPaidEventListener(adValue -> {
                            //Adjust
                            AdjustUtil.trackRevenue(interstitialAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.d(TAG, loadAdError.toString());
                        interCallback.onAdFailedToLoad();
                        listIdInter.remove(0);
                        loadInterAds(context, listIdInter, interCallback, adsKey);
                    }
                });
    }

    public void showInterAds(Activity activity, InterstitialAd mInterstitialAd, InterCallback interCallback, String adsKey) {
        if (System.currentTimeMillis() - lastTimeDismissInter < timeInterval) {
            Log.d(TAG, "Not show interstitial because the time interval.");
            interCallback.onNextAction();
            return;
        }
        if (System.currentTimeMillis() - timeStart < timeIntervalFromStart) {
            Log.d(TAG, "Not show interstitial because the time interval from start.");
            interCallback.onNextAction();
            return;
        }
        if (mInterstitialAd == null) {
            Log.d(TAG, "The interstitial ad wasn't ready yet.");
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
                    // Called when a click is recorded for an ad.
                    Log.d(TAG, "Ad was clicked.");
                    EventTrackingHelper.logEvent(activity, adsKey + "_click");
                    interCallback.onAdClicked();
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    Log.d(TAG, "Ad dismissed fullscreen content.");
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
                    Log.e(TAG, "Ad failed to show fullscreen content.");
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
                    Log.d(TAG, "Ad recorded an impression.");
                    EventTrackingHelper.logEvent(activity, adsKey + "_view");
                    interCallback.onAdImpression();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    Log.d(TAG, "Ad showed fullscreen content.");
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
                Log.d(TAG, "onSplashResume: " + true);
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                DefaultLifecycleObserver.super.onStop(owner);
                isSplashResume = false;
                Log.d(TAG, "onSplashResume: " + false);
            }
        });
        if (mInterstitialAdSplash == null) {
            Log.d(TAG, "The interstitial ad wasn't ready yet.");
            AppOpenManager.getInstance().setEnableResume(true);
            interCallback.onNextAction();
            return;
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            mInterstitialAdSplash.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdClicked() {
                    // Called when a click is recorded for an ad.
                    Log.d(TAG, "Ad was clicked.");
                    interCallback.onAdClicked();
                    countClickInterSplashAds++;
                    int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                    if (splashOpenTimes == 1) {
                        EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_click + "_" + countClickInterSplashAds);
                    }
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    Log.d(TAG, "Ad dismissed fullscreen content.");
                    interCallback.onAdDismissedFullScreenContent();
                    AppOpenManager.getInstance().setEnableResume(true);
                    if (!openActivityAfterShowInterAds) {
                        interCallback.onNextAction();
                    }
                    isInterOrRewardedShowing = false;
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    // Called when ad fails to show.
                    Log.e(TAG, "Ad failed to show fullscreen content.");
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
                    Log.d(TAG, "Ad recorded an impression.");
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
                    Log.d(TAG, "Ad showed fullscreen content.");
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
            Log.d(TAG, "ResumeState: " + isResumeState);
            if (isResumeState) {
                loadingAdsDialog = new LoadingAdsDialog(activity);
                if (!loadingAdsDialog.isShowing() && !activity.isDestroyed()) {
                    loadingAdsDialog.show();
                }
                isInterOrRewardedShowing = true;
                AppOpenManager.getInstance().setEnableResume(false);
                if (openActivityAfterShowInterAds) {
                    //increase splash open
                    SharePreferenceHelper.setInt(activity, EventTrackingHelper.splash_open, SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1);
                    //end increase splash open
                    Log.d(TAG, "showInterAdsSplash: openActivityAfterShowInterAds = true, onNextAction");
                    interCallback.onNextAction();
                }
                mInterstitialAdSplash.show(activity);
            } else {
                Log.d(TAG, "Fail to show on background.");
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
            handlerTimeoutSplash.postDelayed(runnable, 20000);
        }

        //Log event
        Bundle bundle = new Bundle();
        boolean idCheck = AdmobApi.getInstance().getListAdsSize() > 0;
        bundle.putString(EventTrackingHelper.splash_detail, AdsConsentManager.getConsentResult(activity) + "_" + TechManager.getInstance().isTech(activity) + "_" + NetworkUtil.isNetworkActive(activity) + "_" + getShowAllAds() + "_" + idCheck + "_" + RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.inter_splash) + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        bundle.putString(EventTrackingHelper.ump, String.valueOf(AdsConsentManager.getConsentResult(activity)));
        bundle.putString(EventTrackingHelper.organic, String.valueOf(TechManager.getInstance().isTech(activity)));
        bundle.putString(EventTrackingHelper.haveinternet, String.valueOf(NetworkUtil.isNetworkActive(activity)));
        bundle.putString(EventTrackingHelper.showallad, String.valueOf(getShowAllAds()));
        bundle.putString(EventTrackingHelper.idcheck, String.valueOf(idCheck));
        bundle.putString(EventTrackingHelper.interremote + "_" + EventTrackingHelper.openremote + "_" + EventTrackingHelper.aoavalue, RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.inter_splash) + "_" + RemoteConfigHelper.getInstance().get_config(activity, EventTrackingHelper.open_splash) + "_" + RemoteConfigHelper.getInstance().get_config_string(activity, EventTrackingHelper.rate_aoa_inter_splash));
        EventTrackingHelper.logEventWithMultipleParams(activity, EventTrackingHelper.inter_splash_tracking, bundle);

        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdInter.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase()) {
            interCallback.onNextAction();
            if (handlerTimeoutSplash != null && runnable != null) {
                handlerTimeoutSplash.removeCallbacks(runnable);
                handlerTimeoutSplash.removeCallbacksAndMessages(null);
                handlerTimeoutSplash = null;
            }
            return;
        }

        //log event can request
        EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_true);
        //end log event can request

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(activity, listIdInter.get(0), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        Log.i(TAG, "onAdLoaded");
                        interCallback.onAdLoaded(interstitialAd);
                        mInterstitialAdSplash = interstitialAd;
                        showInterAdsSplash(activity, interCallback);

                        if (handlerTimeoutSplash != null && runnable != null) {
                            handlerTimeoutSplash.removeCallbacks(runnable);
                            handlerTimeoutSplash.removeCallbacksAndMessages(null);
                            handlerTimeoutSplash = null;
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.d(TAG, loadAdError.toString());
                        interCallback.onAdFailedToLoad();
                        listIdInter.remove(0);
                        loadAndShowInterAdSplash(activity, listIdInter, interCallback);
                    }
                });
    }

    public void onCheckShowSplashWhenFail(AppCompatActivity activity, InterCallback interCallback) {
        if (isFailToShowAdSplash) {
            showInterAdsSplash(activity, interCallback);
        }
    }

    //================================end inter ads================================

    //================================Start banner ads================================
    public void loadBannerAds(Activity activity, List<String> listIdBanner, FrameLayout adContainerView, BannerCallback bannerCallback, IOnAdsImpression iOnAdsImpression, String adsKey) {
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check network
        if (!NetworkUtil.isNetworkActive(activity) || listIdBanner.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, adsKey)) {
            bannerCallback.onAdFailedToLoad();
            return;
        }
        //Show loading shimmer
        View shimmerBanner = LayoutInflater.from(activity).inflate(R.layout.layout_shimmer_banner, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerBanner);
        }
        // [START create_ad_view]
        // Create a new ad view.
        AdView adView = new AdView(activity);
        adView.setAdUnitId(listIdBanner.get(0));
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
                EventTrackingHelper.logEvent(activity, adsKey + "_click");
                bannerCallback.onAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                bannerCallback.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                bannerCallback.onAdFailedToLoad();
                listIdBanner.remove(0);
                loadBannerAds(activity, listIdBanner, adContainerView, bannerCallback, iOnAdsImpression, adsKey);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                EventTrackingHelper.logEvent(activity, adsKey + "_view");
                bannerCallback.onAdImpression();
                //use for auto reload banner after x seconds
                iOnAdsImpression.onAdsImpression();
                //TechManager
                if (isDetectTestAdByView) {
                    getAllChildViews(activity, adView);
                }
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                // Replace ad container with new ad view.
                if (adContainerView != null) {
                    adContainerView.removeAllViews();
                    adContainerView.addView(adView);
                }
                bannerCallback.onAdLoaded();

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
                bannerCallback.onAdOpened();
            }

            @Override
            public void onAdSwipeGestureClicked() {
                super.onAdSwipeGestureClicked();
                bannerCallback.onAdSwipeGestureClicked();
            }
        });
        // [END load_ad]
    }

    public void getAllChildViews(Context context, ViewGroup viewGroup) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childView = viewGroup.getChildAt(i);
            Log.d("TestAdManager", "getAllChildViews: " + childView.getClass().getName());
            if (childView instanceof TextView && ((TextView) childView).getText().toString().toLowerCase().contains("Test Ad".toLowerCase())) {
                Log.d("TestAdManager", "Find TextView: " + ((TextView) childView).getText().toString().toLowerCase());
                TechManager.getInstance().detectedTech(context, true);
            }
            if (childView instanceof ViewGroup) {
                getAllChildViews(context, (ViewGroup) childView);
            }
        }
    }

    //can load banner ads in fragment
    public void loadBannerAds(Context context, int adWidth, List<String> listIdBanner, FrameLayout adContainerView, BannerCallback bannerCallback, IOnAdsImpression iOnAdsImpression, String adsKey) {
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check network
        if (!NetworkUtil.isNetworkActive(context) || listIdBanner.isEmpty() || !AdsConsentManager.getConsentResult(context) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(context, adsKey)) {
            bannerCallback.onAdFailedToLoad();
            return;
        }
        //Show loading shimmer
        View shimmerBanner = LayoutInflater.from(context).inflate(R.layout.layout_shimmer_banner, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerBanner);
        }
        // [START create_ad_view]
        // Create a new ad view.
        AdView adView = new AdView(context);
        adView.setAdUnitId(listIdBanner.get(0));
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
                EventTrackingHelper.logEvent(context, adsKey + "_click");
                bannerCallback.onAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                bannerCallback.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                bannerCallback.onAdFailedToLoad();
                listIdBanner.remove(0);
                loadBannerAds(context, adWidth, listIdBanner, adContainerView, bannerCallback, iOnAdsImpression, adsKey);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                EventTrackingHelper.logEvent(context, adsKey + "_view");
                bannerCallback.onAdImpression();
                //use for auto reload banner after x seconds
                iOnAdsImpression.onAdsImpression();
                //TechManager
                if (isDetectTestAdByView) {
                    getAllChildViews(context, adView);
                }
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                // Replace ad container with new ad view.
                if (adContainerView != null) {
                    adContainerView.removeAllViews();
                    adContainerView.addView(adView);
                }
                bannerCallback.onAdLoaded();
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
                bannerCallback.onAdOpened();
            }

            @Override
            public void onAdSwipeGestureClicked() {
                super.onAdSwipeGestureClicked();
                bannerCallback.onAdSwipeGestureClicked();
            }
        });
        // [END load_ad]
    }
    //end can load banner ads in fragment
    //================================End banner ads================================

    //================================Start collapse banner ads================================
    public AdView loadCollapseBanner(Activity activity, List<String> listIdCollapseBanner, FrameLayout adContainerView, boolean isGravityBottom, BannerCallback bannerCallback, IOnAdsImpression iOnAdsImpression, String collapseTypeClose, long valueCountDownOrCountClick, String adsKey) {
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check network
        if (!NetworkUtil.isNetworkActive(activity) || listIdCollapseBanner.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, adsKey)) {
            bannerCallback.onAdFailedToLoad();
            return null;
        }
        //Show loading shimmer
        View shimmerBanner = LayoutInflater.from(activity).inflate(R.layout.layout_shimmer_banner, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerBanner);
        }
        AdView adView = new AdView(activity);
        adView.setAdUnitId(listIdCollapseBanner.get(0));

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
                EventTrackingHelper.logEvent(activity, adsKey + "_click");
                bannerCallback.onAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                bannerCallback.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                bannerCallback.onAdFailedToLoad();
                listIdCollapseBanner.remove(0);
                loadCollapseBanner(activity, listIdCollapseBanner, adContainerView, isGravityBottom, bannerCallback, iOnAdsImpression, collapseTypeClose, valueCountDownOrCountClick, adsKey);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                EventTrackingHelper.logEvent(activity, adsKey + "_view");
                bannerCallback.onAdImpression();
                iOnAdsImpression.onAdsImpression();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
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
                bannerCallback.onAdOpened();
                applyTechForCollapseBanner(collapseTypeClose, valueCountDownOrCountClick);
            }

            @Override
            public void onAdSwipeGestureClicked() {
                super.onAdSwipeGestureClicked();
                bannerCallback.onAdSwipeGestureClicked();
            }
        });
        return adView;
    }

    public AdView loadCollapseBanner(Context context, int adWidth, List<String> listIdCollapseBanner, FrameLayout adContainerView, boolean isGravityBottom, BannerCallback bannerCallback, IOnAdsImpression iOnAdsImpression, String collapseTypeClose, long valueCountDownOrCountClick, String adsKey) {
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check network
        if (!NetworkUtil.isNetworkActive(context) || listIdCollapseBanner.isEmpty() || !AdsConsentManager.getConsentResult(context) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(context, adsKey)) {
            bannerCallback.onAdFailedToLoad();
            return null;
        }
        //Show loading shimmer
        View shimmerBanner = LayoutInflater.from(context).inflate(R.layout.layout_shimmer_banner, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerBanner);
        }
        AdView adView = new AdView(context);
        adView.setAdUnitId(listIdCollapseBanner.get(0));

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
                EventTrackingHelper.logEvent(context, adsKey + "_click");
                bannerCallback.onAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                bannerCallback.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                bannerCallback.onAdFailedToLoad();
                listIdCollapseBanner.remove(0);
                loadCollapseBanner(context, adWidth, listIdCollapseBanner, adContainerView, isGravityBottom, bannerCallback, iOnAdsImpression, collapseTypeClose, valueCountDownOrCountClick, adsKey);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                EventTrackingHelper.logEvent(context, adsKey + "_view");
                bannerCallback.onAdImpression();
                iOnAdsImpression.onAdsImpression();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
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
                bannerCallback.onAdOpened();
                applyTechForCollapseBanner(collapseTypeClose, valueCountDownOrCountClick);
            }

            @Override
            public void onAdSwipeGestureClicked() {
                super.onAdSwipeGestureClicked();
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
    private AdSize getAdSize(Activity activity) {
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

    private AdSize getAdSizeFragment(Context context, int adWidth) {
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth);
    }

    //================================Start native ads================================
    public void loadNativeAds(Activity activity, List<String> listIdNative, NativeCallback nativeCallback, String adsKey) {
        //Check network
        if (!NetworkUtil.isNetworkActive(activity) || listIdNative.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, adsKey)) {
            nativeCallback.onAdFailedToLoad();
            return;
        }
        AdLoader.Builder builder = new AdLoader.Builder(activity, listIdNative.get(0));
        builder.forNativeAd(nativeAd -> {
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
                String error = String.format(Locale.getDefault(), "domain: %s, code: %d, message: %s", loadAdError.getDomain(), loadAdError.getCode(), loadAdError.getMessage());
                Log.d(TAG, "Failed to load native ad with error " + error);
                nativeCallback.onAdFailedToLoad();
                listIdNative.remove(0);
                loadNativeAds(activity, listIdNative, nativeCallback, adsKey);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                EventTrackingHelper.logEvent(activity, adsKey + "_click");
            }
        }).build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public void loadNativeAds(Activity activity, List<String> listIdNative, FrameLayout adContainerView, int layoutNative, int layoutNativeMeta, int layoutShimmerNative, boolean setShowNativeAfterLoaded, NativeCallback nativeCallback, IOnAdsImpression iOnAdsImpression, String adsKey) {
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check network
        if (!NetworkUtil.isNetworkActive(activity) || listIdNative.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, adsKey)) {
            nativeCallback.onAdFailedToLoad();
            return;
        }
        //Show loading shimmer
        View shimmerNative = LayoutInflater.from(activity).inflate(layoutShimmerNative, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerNative);
        }
        AdLoader.Builder builder = new AdLoader.Builder(activity, listIdNative.get(0));
        // OnLoadedListener implementation.
        builder.forNativeAd(nativeAd -> {
            // If this callback occurs after the activity is destroyed, you must call
            // destroy and return or you may get a memory leak.
                    /*boolean isDestroyed = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        isDestroyed = activity.isDestroyed();
                    }
                    if (isDestroyed || activity.isFinishing() || activity.isChangingConfigurations()) {
                        nativeAd.destroy();
                        return;
                    }
                    // You must call destroy on old ads when you are done with them,
                    // otherwise you will have a memory leak.
                    if (nativeAd != null) {
                        nativeAd.destroy();
                    }*/
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
                Admob.getInstance().populateNativeAdView(activity, adsKey, nativeAd, adView);
                if (adContainerView != null) {
                    adContainerView.removeAllViews();
                    adContainerView.addView(adView);
                }
            }
            iOnAdsImpression.onAdsImpression();
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
                String error = String.format(Locale.getDefault(), "domain: %s, code: %d, message: %s", loadAdError.getDomain(), loadAdError.getCode(), loadAdError.getMessage());
                Log.d(TAG, "Failed to load native ad with error " + error);
                nativeCallback.onAdFailedToLoad();
                listIdNative.remove(0);
                loadNativeAds(activity, listIdNative, adContainerView, layoutNative, layoutNativeMeta, layoutShimmerNative, setShowNativeAfterLoaded, nativeCallback, iOnAdsImpression, adsKey);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                EventTrackingHelper.logEvent(activity, adsKey + "_click");
            }
        }).build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public void loadNativeAds(Activity activity, List<String> listIdNative, FrameLayout adContainerView, int layoutNative, int layoutNativeMeta, int layoutShimmerNative, boolean setShowNativeAfterLoaded, NativeCallback nativeCallback, String adsKey) {
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check network
        if (!NetworkUtil.isNetworkActive(activity) || listIdNative.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, adsKey)) {
            nativeCallback.onAdFailedToLoad();
            return;
        }
        //Show loading shimmer
        View shimmerNative = LayoutInflater.from(activity).inflate(layoutShimmerNative, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerNative);
        }
        AdLoader.Builder builder = new AdLoader.Builder(activity, listIdNative.get(0));
        // OnLoadedListener implementation.
        builder.forNativeAd(nativeAd -> {
            // If this callback occurs after the activity is destroyed, you must call
            // destroy and return or you may get a memory leak.
                    /*boolean isDestroyed = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        isDestroyed = activity.isDestroyed();
                    }
                    if (isDestroyed || activity.isFinishing() || activity.isChangingConfigurations()) {
                        nativeAd.destroy();
                        return;
                    }
                    // You must call destroy on old ads when you are done with them,
                    // otherwise you will have a memory leak.
                    if (nativeAd != null) {
                        nativeAd.destroy();
                    }*/
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
                Admob.getInstance().populateNativeAdView(activity, adsKey, nativeAd, adView);
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
                String error = String.format(Locale.getDefault(), "domain: %s, code: %d, message: %s", loadAdError.getDomain(), loadAdError.getCode(), loadAdError.getMessage());
                Log.d(TAG, "Failed to load native ad with error " + error);
                nativeCallback.onAdFailedToLoad();
                listIdNative.remove(0);
                loadNativeAds(activity, listIdNative, adContainerView, layoutNative, layoutNativeMeta, layoutShimmerNative, setShowNativeAfterLoaded, nativeCallback, adsKey);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                EventTrackingHelper.logEvent(activity, adsKey + "_click");
            }
        }).build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public void populateNativeAdView(Context context, String adsKey, NativeAd nativeAd, NativeAdView adView) {
        EventTrackingHelper.logEvent(context, adsKey + "_view");
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
        if (adView.getHeadlineView() != null) {
            ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        }
        if (adView.getMediaView() != null) {
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
    public void loadRewardAds(Activity activity, List<String> listIdRewarded, RewardedCallback rewardedCallback, String adsKey) {
        //Check network
        if (!NetworkUtil.isNetworkActive(activity) || listIdRewarded.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, adsKey)) {
            rewardedCallback.onAdFailedToLoad();
            return;
        }
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(activity, listIdRewarded.get(0),
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error.
                        Log.d(TAG, loadAdError.toString());
                        rewardedCallback.onAdFailedToLoad();
                        listIdRewarded.remove(0);
                        loadRewardAds(activity, listIdRewarded, rewardedCallback, adsKey);
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        Log.d(TAG, "Ad was loaded.");
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

    public void showReward(Activity activity, RewardedAd rewardedAd, RewardedCallback rewardedCallback, String adsKey) {
        if (rewardedAd == null) {
            Log.d(TAG, "The rewarded ad wasn't ready yet.");
            rewardedCallback.onAdFailedToShowFullScreenContent();
            return;
        }
        loadingAdsDialog = new LoadingAdsDialog(activity);
        if (!loadingAdsDialog.isShowing() && !activity.isDestroyed()) {
            loadingAdsDialog.show();
        }
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(TAG, "Ad was clicked.");
                EventTrackingHelper.logEvent(activity, adsKey + "_click");
                rewardedCallback.onAdClicked();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Set the ad reference to null so you don't show the ad a second time.
                Log.d(TAG, "Ad dismissed fullscreen content.");
                rewardedCallback.onAdDismissedFullScreenContent();
                isInterOrRewardedShowing = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show fullscreen content.");
                rewardedCallback.onAdFailedToShowFullScreenContent();
                if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    loadingAdsDialog.dismiss();
                }
            }

            @Override
            public void onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(TAG, "Ad recorded an impression.");
                EventTrackingHelper.logEvent(activity, adsKey + "_view");
                rewardedCallback.onAdImpression();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "Ad showed fullscreen content.");
                rewardedCallback.onAdShowedFullScreenContent();
                if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    loadingAdsDialog.dismiss();
                }
                isInterOrRewardedShowing = true;
            }
        });
        rewardedAd.show(activity, rewardItem -> {
            // Handle the reward.
            Log.d(TAG, "The user earned the reward.");
            int rewardAmount = rewardItem.getAmount();
            String rewardType = rewardItem.getType();
            rewardedCallback.onUserEarnedReward();
        });
    }
    //================================End reward ads================================

    //================================Start reward inter================================
    public void loadRewardInterAds(Activity activity, List<String> listIdRewardedInter, RewardedInterCallback rewardedInterCallback, String adsKey) {
        //Check network
        if (!NetworkUtil.isNetworkActive(activity) || listIdRewardedInter.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds || IAPManager.getInstance().isPurchase() || !RemoteConfigHelper.getInstance().get_config(activity, adsKey)) {
            rewardedInterCallback.onAdFailedToLoad();
            return;
        }
        RewardedInterstitialAd.load(activity, listIdRewardedInter.get(0),
                new AdRequest.Builder().build(), new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedInterstitialAd ad) {
                        Log.d(TAG, "Ad was loaded.");
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
                        Log.d(TAG, loadAdError.toString());
                        rewardedInterCallback.onAdFailedToLoad();
                        listIdRewardedInter.remove(0);
                        loadRewardInterAds(activity, listIdRewardedInter, rewardedInterCallback, adsKey);
                    }
                });
    }

    public void showRewardInterAds(Activity activity, RewardedInterstitialAd rewardedInterstitialAd, RewardedInterCallback rewardedInterCallback, String adsKey) {
        if (rewardedInterstitialAd == null) {
            Log.d(TAG, "The rewarded inter ad wasn't ready yet.");
            rewardedInterCallback.onAdFailedToShowFullScreenContent();
            return;
        }
        loadingAdsDialog = new LoadingAdsDialog(activity);
        if (!loadingAdsDialog.isShowing() && !activity.isDestroyed()) {
            loadingAdsDialog.show();
        }
        rewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(TAG, "Ad was clicked.");
                EventTrackingHelper.logEvent(activity, adsKey + "_click");
                rewardedInterCallback.onAdClicked();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Set the ad reference to null so you don't show the ad a second time.
                Log.d(TAG, "Ad dismissed fullscreen content.");
                rewardedInterCallback.onAdDismissedFullScreenContent();
                isInterOrRewardedShowing = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show fullscreen content.");
                rewardedInterCallback.onAdFailedToShowFullScreenContent();
                if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    loadingAdsDialog.dismiss();
                }
            }

            @Override
            public void onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(TAG, "Ad recorded an impression.");
                EventTrackingHelper.logEvent(activity, adsKey + "_view");
                rewardedInterCallback.onAdImpression();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "Ad showed fullscreen content.");
                rewardedInterCallback.onAdShowedFullScreenContent();
                if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    loadingAdsDialog.dismiss();
                }
                isInterOrRewardedShowing = true;
            }
        });
        rewardedInterstitialAd.show(activity, rewardItem -> rewardedInterCallback.onUserEarnedReward());
    }
    //================================End reward inter================================
}
