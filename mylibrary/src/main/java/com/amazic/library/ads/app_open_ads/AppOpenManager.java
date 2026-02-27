package com.amazic.library.ads.app_open_ads;

import android.app.Activity;
import android.app.Application;
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
import com.amazic.library.ads.callback.AppOpenCallback;
import com.amazic.library.ads.splash_ads.AsyncSplash;
import com.amazic.library.dialog.LoadingAdsResumeDialog;
import com.amazic.library.organic.TechManager;
import com.amazic.library.ump.AdsConsentManager;
import com.google.android.gms.ads.AdActivity;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.appopen.AppOpenAdPreloader;
import com.google.android.gms.ads.preload.PreloadCallbackV2;
import com.google.android.gms.ads.preload.PreloadConfiguration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AppOpenManager implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private static final String TAG = "AppOpenManager";
    private static AppOpenManager INSTANCE;
    private AppOpenAd appOpenAdSplash = null;
    private boolean isLoadingAdSplash = false;
    private AppOpenAd appOpenAd = null;
    private boolean isLoadingAd = false;
    private boolean isShowingAd = false;
    private long loadTime = 0;
    private Activity currentActivity;
    private Application application;
    private LoadingAdsResumeDialog loadingAdsResumeDialog;
    public ArrayList<Integer> listAnimationDialogRaw = new ArrayList<>();
    private boolean isCustomAnimationDialog = false;
    private final List<String> listIdOpenResumeAd = new ArrayList<>();
    private boolean isFailToShowAdSplash = false;
    private final ArrayList<Class> disabledAppOpenList = new ArrayList<>();
    private boolean isShowWelcomeBelowAdsResume = false;
    private Class welcomeBackClass = null;
    private Handler handlerTimeoutSplash = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private boolean isSplashResume = true;
    private int countClickInterSplashAds = 0;

    public boolean isEnableResume() {
        return isEnableResume;
    }

    public void setEnableResume(boolean enableResume) {
        isEnableResume = enableResume;
    }

    private boolean isEnableResume = true;
    private String remoteKey = "open_resume";
    public static boolean isLastActionClickAd = false;
    public boolean isShowAdResumeAfterAdClick = true;

    public static AppOpenManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AppOpenManager();
        }
        return INSTANCE;
    }

    public void init(Activity activity, List<String> listIdOpenResume) {
        this.currentActivity = activity;
        this.listIdOpenResumeAd.clear();
        this.listIdOpenResumeAd.addAll(listIdOpenResume);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        setApplication(activity.getApplication());
    }

    public void initWelcomeBackBelowAdsResume(Activity activity, List<String> listIdOpenResume, Class welcomeBackClass) {
        this.currentActivity = activity;
        this.listIdOpenResumeAd.clear();
        this.listIdOpenResumeAd.addAll(listIdOpenResume);
        this.welcomeBackClass = welcomeBackClass;
        this.isShowWelcomeBelowAdsResume = true;
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        setApplication(activity.getApplication());
    }

    public void initWelcomeBackAboveAdsResume(Activity activity, List<String> listIdOpenResume, Class welcomeBackClass) {
        this.currentActivity = activity;
        this.listIdOpenResumeAd.clear();
        this.listIdOpenResumeAd.addAll(listIdOpenResume);
        this.welcomeBackClass = welcomeBackClass;
        this.isShowWelcomeBelowAdsResume = false;
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        setApplication(activity.getApplication());
    }

    public void setApplication(Application application) {
        this.application = application;
        this.application.registerActivityLifecycleCallbacks(this);
    }

    public boolean isCustomAnimationDialog() {
        return isCustomAnimationDialog;
    }

    public void setCustomAnimationDialog(boolean customAnimationDialog) {
        this.isCustomAnimationDialog = customAnimationDialog;
    }

    public void setCustomAnimationDialog(ArrayList<Integer> listAnimationDialogRaw) {
        this.isCustomAnimationDialog = true;
        this.listAnimationDialogRaw = listAnimationDialogRaw;
    }

    public AppOpenAd getAppOpenAdSplash() {
        return appOpenAdSplash;
    }

    public void setAppOpenAdSplash(AppOpenAd appOpenAdSplash) {
        this.appOpenAdSplash = appOpenAdSplash;
    }

    public boolean isShowingAd() {
        return isShowingAd;
    }

    public void disableAppResumeWithActivity(@NonNull Class activityClass) {
        if (!disabledAppOpenList.contains(activityClass)) {
            Log.d(TAG, "disableAppResumeWithActivity: " + activityClass.getName());
            disabledAppOpenList.add(activityClass);
        }
    }

    public void enableAppResumeWithActivity(@NonNull Class activityClass) {
        Log.d(TAG, "enableAppResumeWithActivity: " + activityClass.getName());
        disabledAppOpenList.remove(activityClass);
    }

    /**
     * Utility method to check if ad was loaded more than n hours ago.
     */
    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        long dateDifference = (new Date()).getTime() - this.loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }

    private boolean isAdAvailable() {
        Log.d(TAG, "isAdAvailable: appOpenAd = " + appOpenAd + "-wasLoadTimeLessThanNHoursAgo: " + wasLoadTimeLessThanNHoursAgo(4));
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
    }

    private boolean isAdSplashAvailable() {
        Log.d(TAG, "SPLASH: isAdSplashAvailable: appOpenAd = " + appOpenAdSplash);
        return appOpenAdSplash != null;
    }

    //===========================Start load ads, show ads resume in normal activity============================//
    //load and show ads resume
    public void loadAndShowResumeAds(Activity activity, List<String> listIdOpenResume, AppOpenCallback appOpenCallback, String remoteKey) {
        ArrayList<String> listIdOpenResumeTemp = new ArrayList<>(listIdOpenResume);
        // Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdOpenResumeTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds() || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdOpenResumeTemp.size() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + Admob.getInstance().getShowAllAds() + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            appOpenCallback.onAdFailedToLoad();
            return;
        }
        // Do not load ad if one is already loading.
        if (isLoadingAd) {
            Log.d(TAG, "Do not load ad if there is an unused ad or one is already loading.");
            return;
        }
        // Do not load ad if there is an unused ad.
        /*if (isAdAvailable()) {
            appOpenCallback.onAdLoaded(this.appOpenAd);
            Log.d(TAG, "Do not load ad if there is an unused ad.");
            return;
        }*/
        isLoadingAd = true;

        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        loadingAdsResumeDialog = new LoadingAdsResumeDialog(activity);
        if (!loadingAdsResumeDialog.isShowing()) {
            loadingAdsResumeDialog.show();
        }

        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(activity, listIdOpenResumeTemp.get(0), request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                Log.i(TAG, "onAdLoaded. " + remoteKey);
                appOpenAd = ad;
                isLoadingAd = false;
                loadTime = (new Date()).getTime();
                appOpenCallback.onAdLoaded(ad);
                showAdIfAvailableWelcomeBackLoadAndShow(activity, listIdOpenResume, appOpenCallback, remoteKey, false);
                //Tracking revenue
                ad.setOnPaidEventListener(adValue -> {
                    //Adjust
                    ad.getResponseInfo();
                    AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                isLoadingAd = false;
                if (!listIdOpenResumeTemp.isEmpty()) {
                    listIdOpenResumeTemp.remove(0);
                }
                appOpenCallback.onAdFailedToLoad();
                loadAndShowResumeAds(activity, listIdOpenResumeTemp, appOpenCallback, remoteKey);
            }
        });
    }

    public void showAdIfAvailableWelcomeBackLoadAndShow(@NonNull final Activity activity, List<String> listIdOpenResume, AppOpenCallback appOpenCallback, String remoteKey, boolean isCheckDisableWithClass) {
        //Ads resume is disabled.
        if (!isEnableResume) {
            Log.d(TAG, "WELCOME BACK: Ads resume is disabled.");
            return;
        }
        // If the app open ad is not available yet, invoke the callback then load the ad.
        /*if (!isAdAvailable()) {
            Log.d(TAG, "WELCOME BACK: The app open ad is not ready yet.");
            //onShowAdCompleteListener.onShowAdComplete();
            loadAd(activity, listIdOpenResume, appOpenCallback, adsKey);
            return;
        }*/
        // If the app open ad is already showing, do not show the ad again.
        if (isShowingAd) {
            Log.d(TAG, "WELCOME BACK: The app open ad is already showing.");
            return;
        }
        // Not show open ads if inter is showing
        if (Admob.getInstance().isInterOrRewardedShowing()) {
            Log.d(TAG, "WELCOME BACK: Not show open ads because inter is showing.");
            return;
        }
        // Not show ads resume when activity is disabled
        if (isCheckDisableWithClass) {
            for (Class activityDisabled : disabledAppOpenList) {
                if (activityDisabled != null)
                    if (activityDisabled.getName().equals(currentActivity.getClass().getName())) {
                        Log.d(TAG, "onStart: activity is disabled " + activityDisabled.getName());
                        return;
                    }
            }
        }
        //show welcome back activity
        if (welcomeBackClass != null && currentActivity.getClass() != welcomeBackClass && currentActivity.getClass() != AdActivity.class) {
            currentActivity.startActivity(new Intent(currentActivity, welcomeBackClass));
            if (!this.isShowWelcomeBelowAdsResume) {
                return;
            }
        }
        // Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdOpenResume.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds() || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "WELCOME BACK: Check condition showAdIfAvailableWelcomeBackLoadAndShow. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IDEmpty:" + listIdOpenResume.size() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + Admob.getInstance().getShowAllAds() + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            if (appOpenCallback != null) {
                appOpenCallback.onAdFailedToShowFullScreenContent();
            }
            return;
        }

        if (appOpenAd != null) {
            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {

                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "WELCOME BACK: onAdDismissedFullScreenContent. " + remoteKey);
                    appOpenAd = null;
                    isShowingAd = false;
                    //loadAd(activity, listIdOpenResume, appOpenCallback, adsKey);
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdDismissedFullScreenContent();
                    }
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.e(TAG, "WELCOME BACK: onAdFailedToShowFullScreenContent. " + adError + ". " + remoteKey);
                    appOpenAd = null;
                    isShowingAd = false;
                    if (loadingAdsResumeDialog != null && loadingAdsResumeDialog.isShowing()) {
                        loadingAdsResumeDialog.dismiss();
                    }
                    //loadAd(activity, listIdOpenResume, appOpenCallback, adsKey);
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdFailedToShowFullScreenContent();
                    }
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "WELCOME BACK: onAdShowedFullScreenContent. " + remoteKey);
                    if (loadingAdsResumeDialog != null && loadingAdsResumeDialog.isShowing()) {
                        loadingAdsResumeDialog.dismiss();
                    }
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdShowedFullScreenContent();
                    }
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    AppOpenManager.isLastActionClickAd = true;
                    Log.d(TAG, "WELCOME BACK: onAdClicked. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdClicked();
                    }
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    Log.d(TAG, "WELCOME BACK: onAdImpression. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdImpression();
                    }
                }
            });
            isShowingAd = true;
            appOpenAd.show(activity);
        }
    }

    //end load and show ads resume
    public void loadAd(Activity activity, List<String> listIdOpenResume, AppOpenCallback appOpenCallback, String remoteKey) {
        ArrayList<String> listIdOpenResumeTemp = new ArrayList<>(listIdOpenResume);
        // Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdOpenResumeTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds() || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "Check condition. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdOpenResumeTemp.size() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + Admob.getInstance().getShowAllAds() + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            appOpenCallback.onAdFailedToLoad();
            return;
        }
        // Do not load ad if one is already loading.
        if (isLoadingAd) {
            Log.d(TAG, "Do not load ad if there is an unused ad or one is already loading.");
            return;
        }
        // Do not load ad if there is an unused ad.
        if (isAdAvailable()) {
            appOpenCallback.onAdLoaded(this.appOpenAd);
            Log.d(TAG, "Do not load ad if there is an unused ad.");
            return;
        }
        isLoadingAd = true;

        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(activity, listIdOpenResumeTemp.get(0), request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                Log.i(TAG, "onAdLoaded. " + remoteKey);
                appOpenAd = ad;
                isLoadingAd = false;
                loadTime = (new Date()).getTime();
                appOpenCallback.onAdLoaded(ad);
                //Tracking revenue
                ad.setOnPaidEventListener(adValue -> {
                    //Adjust
                    ad.getResponseInfo();
                    AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                isLoadingAd = false;
                if (!listIdOpenResumeTemp.isEmpty()) {
                    listIdOpenResumeTemp.remove(0);
                }
                appOpenCallback.onAdFailedToLoad();
                loadAd(activity, listIdOpenResumeTemp, appOpenCallback, remoteKey);
            }
        });
    }

    public void loadAdNotCheckRemote(Activity activity, List<String> listIdOpenResume, String remoteKey) {
        ArrayList<String> listIdOpenResumeTemp = new ArrayList<>(listIdOpenResume);
        // Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdOpenResumeTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds() /*|| IAPManager.getInstance().isPurchase()*/) {
            Log.d(TAG, "Check condition loadAdNotCheckRemote. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdOpenResumeTemp.size() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + Admob.getInstance().getShowAllAds() + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            return;
        }
        // Do not load ad if there is an unused ad or one is already loading.
        if (isLoadingAd || isAdAvailable()) {
            Log.d(TAG, "Do not load ad if there is an unused ad or one is already loading.");
            return;
        }
        isLoadingAd = true;
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(activity, listIdOpenResumeTemp.get(0), request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                Log.i(TAG, "onAdLoaded. " + remoteKey);
                appOpenAd = ad;
                isLoadingAd = false;
                loadTime = (new Date()).getTime();
                //Tracking revenue
                ad.setOnPaidEventListener(adValue -> {
                    //Adjust
                    ad.getResponseInfo();
                    AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                isLoadingAd = false;
                if (!listIdOpenResumeTemp.isEmpty()) {
                    listIdOpenResumeTemp.remove(0);
                }
                loadAdNotCheckRemote(activity, listIdOpenResumeTemp, remoteKey);
            }
        });
    }

    public void loadAd(Activity activity, List<String> listIdOpenResume, String remoteKey) {
        ArrayList<String> listIdOpenResumeTemp = new ArrayList<>(listIdOpenResume);
        // Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdOpenResumeTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds() || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "Check condition loadAd. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdOpenResumeTemp.size() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + Admob.getInstance().getShowAllAds() + "_UMP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            return;
        }
        // Do not load ad if there is an unused ad or one is already loading.
        if (isLoadingAd || isAdAvailable()) {
            Log.d(TAG, "Do not load ad if there is an unused ad or one is already loading.");
            return;
        }
        isLoadingAd = true;

        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(activity, listIdOpenResumeTemp.get(0), request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                Log.i(TAG, "onAdLoaded. " + remoteKey);
                appOpenAd = ad;
                isLoadingAd = false;
                loadTime = (new Date()).getTime();
                //Tracking revenue
                ad.setOnPaidEventListener(adValue -> {
                    //Adjust
                    ad.getResponseInfo();
                    AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                isLoadingAd = false;
                if (!listIdOpenResumeTemp.isEmpty()) {
                    listIdOpenResumeTemp.remove(0);
                }
                loadAd(activity, listIdOpenResumeTemp, remoteKey);
            }
        });
    }

    public void showAdIfAvailable(@NonNull final Activity activity, List<String> listIdOpenResume, AppOpenCallback appOpenCallback, String remoteKey) {
        Log.d(TAG, "Ads Click:" + isLastActionClickAd + " && " + !isShowAdResumeAfterAdClick);
        if (isLastActionClickAd && !isShowAdResumeAfterAdClick) {
            isLastActionClickAd = false;
            return;
        }
        //Ads resume is disabled
        if (!isEnableResume) {
            Log.d(TAG, "Ads resume is disabled.");
            return;
        }
        // If the app open ad is not available yet, invoke the callback then load the ad.
        if (!isAdAvailable()) {
            Log.d(TAG, "The app open ad is not ready yet.");
            //onShowAdCompleteListener.onShowAdComplete();
            loadAd(activity, listIdOpenResume, remoteKey);
            return;
        }
        // If the app open ad is already showing, do not show the ad again.
        if (isShowingAd) {
            Log.d(TAG, "The app open ad is already showing.");
            return;
        }
        // Not show open ads if inter is showing
        if (Admob.getInstance().isInterOrRewardedShowing()) {
            Log.d(TAG, "Not show open ads because inter is showing.");
            return;
        }
        //Not show open ads because currentActivity is null.
        if (currentActivity == null) {
            Log.d(TAG, "Not show open ads because currentActivity is null.");
            return;
        }
        // Not show ads resume when activity is disabled
        for (Class activityDisabled : disabledAppOpenList) {
            if (activityDisabled != null)
                if (activityDisabled.getName().equals(currentActivity.getClass().getName())) {
                    Log.d(TAG, "onStart: activity is disabled " + activityDisabled.getName());
                    return;
                }
        }
        //show welcome back activity
        if (welcomeBackClass != null && currentActivity.getClass() != welcomeBackClass && currentActivity.getClass() != AdActivity.class) {
            currentActivity.startActivity(new Intent(currentActivity, welcomeBackClass));
            if (!this.isShowWelcomeBelowAdsResume) {
                return;
            }
        }
        // Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdOpenResume.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds() || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "Check condition showAdIfAvailable. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdOpenResume.size() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + Admob.getInstance().getShowAllAds() + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            if (appOpenCallback != null) {
                appOpenCallback.onAdFailedToShowFullScreenContent();
            }
            return;
        }
        loadingAdsResumeDialog = new LoadingAdsResumeDialog(activity);
        if (!loadingAdsResumeDialog.isShowing()) {
            loadingAdsResumeDialog.show();
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {

                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed fullscreen content. " + remoteKey);
                    appOpenAd = null;
                    isShowingAd = false;
                    loadAd(activity, listIdOpenResume, remoteKey);
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdDismissedFullScreenContent();
                    }
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.d(TAG, "Ad Failed To Show FullScreen Content. " + adError + ". " + remoteKey);
                    appOpenAd = null;
                    isShowingAd = false;
                    if (loadingAdsResumeDialog != null && loadingAdsResumeDialog.isShowing()) {
                        loadingAdsResumeDialog.dismiss();
                    }
                    loadAd(activity, listIdOpenResume, remoteKey);
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdFailedToShowFullScreenContent();
                    }
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when fullscreen content is shown.
                    Log.d(TAG, "Ad showed fullscreen content. " + remoteKey);
                    if (loadingAdsResumeDialog != null && loadingAdsResumeDialog.isShowing()) {
                        loadingAdsResumeDialog.dismiss();
                    }
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdShowedFullScreenContent();
                    }
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    AppOpenManager.isLastActionClickAd = true;
                    Log.d(TAG, "onAdClicked. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdClicked();
                    }
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    Log.d(TAG, "onAdImpression. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdImpression();
                    }
                }
            });
            isShowingAd = true;
            appOpenAd.show(activity);
        }, 250);
    }

    //Ad Preload
    public void loadAdPreloadNotCheckRemote(Activity activity, List<String> listIdOpenResume, String remoteKey) {
        // Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdOpenResume.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds() /*|| IAPManager.getInstance().isPurchase()*/) {
            Log.d(TAG, "APP Open Preload: Check condition loadAdNotCheckRemote. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdOpenResume.size() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + Admob.getInstance().getShowAllAds() + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            return;
        }
        // Do not load ad if there is an unused ad or one is already loading.
        if (isLoadingAd || isAdAvailable()) {
            Log.d(TAG, "APP Open Preload: Do not load ad if there is an unused ad or one is already loading.");
            return;
        }
        isLoadingAd = true;

        //log event can request ads
        EventTrackingHelper.logEvent(activity, remoteKey + "_true");
        //end log event can request ads

        PreloadConfiguration configuration = new PreloadConfiguration.Builder(listIdOpenResume.get(0)).setBufferSize(AsyncSplash.Companion.getInstance().getNumberPreloading()).build();

        PreloadCallbackV2 callback = new PreloadCallbackV2() {
            @Override
            public void onAdFailedToPreload(@NonNull String s, @NonNull AdError adError) {
                Log.d(TAG, "APP Open Preload: Preload ad " + s + " had an error : " + adError.getMessage() + ".");
            }

            @Override
            public void onAdPreloaded(@NonNull String s, @Nullable ResponseInfo responseInfo) {
                Log.d(TAG, "APP Open Preload: Preload ad for " + s + " is available.");
            }

            @Override
            public void onAdsExhausted(@NonNull String s) {
                Log.d(TAG, "APP Open Preload: Preload ad  " + s + " is exhausted.");
            }
        };

        AppOpenAdPreloader.start(listIdOpenResume.get(0), configuration, callback);
    }

    private void showAdPreload(@NonNull final Activity activity, List<String> listIdOpenResume, AppOpenCallback appOpenCallback, String remoteKey) {
        Log.d(TAG, "APP Open Preload: Ads Click:" + isLastActionClickAd + " && " + !isShowAdResumeAfterAdClick);
        if (isLastActionClickAd && !isShowAdResumeAfterAdClick) {
            isLastActionClickAd = false;
            return;
        }
        //Ads resume is disabled
        if (!isEnableResume) {
            Log.d(TAG, "APP Open Preload: Ads resume is disabled.");
            return;
        }
        // If the app open ad is not available yet, invoke the callback then load the ad.
//        if (!isAdAvailable()) {
//            Log.d(TAG, " APP Open Preload: The app open ad is not ready yet.");
//            //onShowAdCompleteListener.onShowAdComplete();
//            loadAdPreloadNotCheckRemote(activity, listIdOpenResume, remoteKey);
//            return;
//        }
        // If the app open ad is already showing, do not show the ad again.
        if (isShowingAd) {
            Log.d(TAG, "APP Open Preload: The app open ad is already showing.");
            return;
        }
        // Not show open ads if inter is showing
        if (Admob.getInstance().isInterOrRewardedShowing()) {
            Log.d(TAG, "APP Open Preload: Not show open ads because inter is showing.");
            return;
        }
        //Not show open ads because currentActivity is null.
        if (currentActivity == null) {
            Log.d(TAG, "APP Open Preload: Not show open ads because currentActivity is null.");
            return;
        }
        // Not show ads resume when activity is disabled
        for (Class activityDisabled : disabledAppOpenList) {
            if (activityDisabled != null)
                if (activityDisabled.getName().equals(currentActivity.getClass().getName())) {
                    Log.d(TAG, "APP Open Preload: onStart: activity is disabled " + activityDisabled.getName());
                    return;
                }
        }
        //show welcome back activity
        if (welcomeBackClass != null && currentActivity.getClass() != welcomeBackClass && currentActivity.getClass() != AdActivity.class) {
            currentActivity.startActivity(new Intent(currentActivity, welcomeBackClass));
            if (!this.isShowWelcomeBelowAdsResume) {
                return;
            }
        }
        // Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdOpenResume.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds() || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "APP Open Preload: Check condition showAdIfAvailable. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdOpenResume.size() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + Admob.getInstance().getShowAllAds() + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            if (appOpenCallback != null) {
                appOpenCallback.onAdFailedToShowFullScreenContent();
            }
            return;
        }
        loadingAdsResumeDialog = new LoadingAdsResumeDialog(activity);
        if (!loadingAdsResumeDialog.isShowing()) {
            loadingAdsResumeDialog.show();
        }

        AppOpenAd ad = AppOpenAdPreloader.pollAd(listIdOpenResume.get(0));

        if (ad != null) {
            ad.setOnPaidEventListener(
                    adValue -> {
                        ad.getResponseInfo();
                        AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                    }
            );

            ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "APP Open Preload: Ad dismissed fullscreen content. " + remoteKey);
                    appOpenAd = null;
                    isShowingAd = false;
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdDismissedFullScreenContent();
                    }
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.d(TAG, "APP Open Preload: Ad Failed To Show FullScreen Content. " + adError + ". " + remoteKey);
                    appOpenAd = null;
                    isShowingAd = false;
                    if (loadingAdsResumeDialog != null && loadingAdsResumeDialog.isShowing()) {
                        loadingAdsResumeDialog.dismiss();
                    }
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdFailedToShowFullScreenContent();
                    }
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "APP Open Preload: Ad showed fullscreen content. " + remoteKey);
                    if (loadingAdsResumeDialog != null && loadingAdsResumeDialog.isShowing()) {
                        loadingAdsResumeDialog.dismiss();
                    }
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdShowedFullScreenContent();
                    }
                }

                @Override
                public void onAdClicked() {
                    AppOpenManager.isLastActionClickAd = true;
                    Log.d(TAG, "APP Open Preload: onAdClicked. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdClicked();
                    }
                }

                @Override
                public void onAdImpression() {
                    Log.d(TAG, "APP Open Preload: onAdImpression. " + remoteKey);
                    EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdImpression();
                    }
                }
            });
            isShowingAd = true;
            ad.show(activity);
        }

    }

    //end

    public void showAdIfAvailableWelcomeBack(@NonNull final Activity activity, List<String> listIdOpenResume, AppOpenCallback appOpenCallback, String remoteKey) {
        // Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdOpenResume.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds() || /*IAPManager.getInstance().isPurchase() ||*/ !RemoteConfigHelper.getInstance().get_config(activity, remoteKey)) {
            Log.d(TAG, "WELCOME BACK: Check condition showAdIfAvailableWelcomeBack. RemoteKey:" + remoteKey + "_Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdOpenResume.size() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + Admob.getInstance().getShowAllAds() + "_IAP:" + /*IAPManager.getInstance().isPurchase() +*/ "_RemoteConfig:" + RemoteConfigHelper.getInstance().get_config(activity, remoteKey));
            if (appOpenCallback != null) {
                appOpenCallback.onAdFailedToShowFullScreenContent();
            }
            return;
        }
        //Ads resume is disabled.
        if (!isEnableResume) {
            Log.d(TAG, "WELCOME BACK: Ads resume is disabled.");
            return;
        }
        // If the app open ad is not available yet, invoke the callback then load the ad.
        if (!isAdAvailable()) {
            Log.d(TAG, "WELCOME BACK: The app open ad is not ready yet.");
            //onShowAdCompleteListener.onShowAdComplete();
            loadAd(activity, listIdOpenResume, appOpenCallback, remoteKey);
            return;
        }
        // If the app open ad is already showing, do not show the ad again.
        if (isShowingAd) {
            Log.d(TAG, "WELCOME BACK: The app open ad is already showing.");
            return;
        }
        // Not show open ads if inter is showing
        if (Admob.getInstance().isInterOrRewardedShowing()) {
            Log.d(TAG, "WELCOME BACK: Not show open ads because inter is showing.");
            return;
        }
        //show welcome back activity
        if (welcomeBackClass != null && currentActivity.getClass() != welcomeBackClass && currentActivity.getClass() != AdActivity.class) {
            currentActivity.startActivity(new Intent(currentActivity, welcomeBackClass));
            if (!this.isShowWelcomeBelowAdsResume) {
                return;
            }
        }
        loadingAdsResumeDialog = new LoadingAdsResumeDialog(activity);
        if (!loadingAdsResumeDialog.isShowing()) {
            loadingAdsResumeDialog.show();
        }

        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {

            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "WELCOME BACK: onAdDismissedFullScreenContent. " + remoteKey);
                appOpenAd = null;
                isShowingAd = false;
                loadAd(activity, listIdOpenResume, appOpenCallback, remoteKey);
                if (appOpenCallback != null) {
                    appOpenCallback.onAdDismissedFullScreenContent();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "WELCOME BACK: onAdFailedToShowFullScreenContent. " + adError + ". " + remoteKey);
                appOpenAd = null;
                isShowingAd = false;
                if (loadingAdsResumeDialog != null && loadingAdsResumeDialog.isShowing()) {
                    loadingAdsResumeDialog.dismiss();
                }
                loadAd(activity, listIdOpenResume, appOpenCallback, remoteKey);
                if (appOpenCallback != null) {
                    appOpenCallback.onAdFailedToShowFullScreenContent();
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "WELCOME BACK: onAdShowedFullScreenContent. " + remoteKey);
                if (loadingAdsResumeDialog != null && loadingAdsResumeDialog.isShowing()) {
                    loadingAdsResumeDialog.dismiss();
                }
                if (appOpenCallback != null) {
                    appOpenCallback.onAdShowedFullScreenContent();
                }
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
                Log.d(TAG, "WELCOME BACK: onAdClicked. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_click");
                if (appOpenCallback != null) {
                    appOpenCallback.onAdClicked();
                }
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                Log.d(TAG, "WELCOME BACK: onAdImpression. " + remoteKey);
                EventTrackingHelper.logEvent(activity, remoteKey + "_view");
                if (appOpenCallback != null) {
                    appOpenCallback.onAdImpression();
                }
            }
        });
        isShowingAd = true;
        appOpenAd.show(activity);
    }
    //===========================End load ads, show ads resume in normal activity============================//

    //===========================Start load ads, show ads resume in splash============================//
    public void showAdSplashIfAvailable(@NonNull final AppCompatActivity activity, AppOpenCallback appOpenCallback) {
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
        // If the app open ad is already showing, do not show the ad again.
        if (isShowingAd) {
            Log.d(TAG, "SPLASH: The app open ad is already showing.");
            return;
        }
        // Not show open ads if inter is showing
        if (Admob.getInstance().isInterOrRewardedShowing()) {
            Log.d(TAG, "SPLASH: Not show open ads because inter is showing.");
            return;
        }
        // If the app open ad is not available yet, invoke the callback then load the ad.
        /*if (!isAdSplashAvailable()) {
            Log.d(TAG, "SPLASH: The app open ad is not ready yet.");
            //onShowAdCompleteListener.onShowAdComplete();
            return;
        }*/

        loadingAdsResumeDialog = new LoadingAdsResumeDialog(activity);
        if (!loadingAdsResumeDialog.isShowing() && !activity.isDestroyed()) {
            loadingAdsResumeDialog.show();
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            appOpenAdSplash.setFullScreenContentCallback(new FullScreenContentCallback() {

                @Override
                public void onAdDismissedFullScreenContent() {
                    //increase splash open
                    SharePreferenceHelper.setInt(activity, EventTrackingHelper.splash_open, SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1);
                    //end increase splash open

                    Log.d(TAG, "SPLASH: Ad dismissed fullscreen content.");
                    appOpenAdSplash = null;
                    isShowingAd = false;

                    appOpenCallback.onAdDismissedFullScreenContent();
                    appOpenCallback.onNextAction();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.e(TAG, "SPLASH: Ad Failed To Show FullScreen Content. " + adError);
                    //appOpenAdSplash = null;
                    isShowingAd = false;

                    if (loadingAdsResumeDialog != null && loadingAdsResumeDialog.isShowing()) {
                        loadingAdsResumeDialog.dismiss();
                    }
                    appOpenCallback.onAdFailedToShowFullScreenContent();
                    if (isSplashResume) {
                        //increase splash open
                        SharePreferenceHelper.setInt(activity, EventTrackingHelper.splash_open, SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1);
                        //end increase splash open
                        appOpenCallback.onNextAction();
                    }
                    isFailToShowAdSplash = true;
                    if (handlerTimeoutSplash != null && runnable != null) {
                        handlerTimeoutSplash.removeCallbacks(runnable);
                    }
                    //log event
                    EventTrackingHelper.logEventWithAParam(activity, EventTrackingHelper.inter_splash_showad_time, EventTrackingHelper.showad_time, "false_" + (System.currentTimeMillis() - AsyncSplash.Companion.getInstance().getTimeStartSplash()) / 1000);
                    //end log event
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when fullscreen content is shown.
                    Log.d(TAG, "SPLASH: Ad showed fullscreen content.");
                    if (loadingAdsResumeDialog != null && loadingAdsResumeDialog.isShowing()) {
                        loadingAdsResumeDialog.dismiss();
                    }
                    appOpenCallback.onAdShowedFullScreenContent();
                    isFailToShowAdSplash = false;
                    if (handlerTimeoutSplash != null && runnable != null) {
                        handlerTimeoutSplash.removeCallbacks(runnable);
                    }
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    AppOpenManager.isLastActionClickAd = true;
                    Log.d(TAG, "SPLASH: onAdClicked.");
                    countClickInterSplashAds++;
                    int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                    if (splashOpenTimes == 1) {
                        EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_click + "_" + countClickInterSplashAds);
                    }
                    appOpenCallback.onAdClicked();
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    Log.d(TAG, "SPLASH: onAdImpression.");
                    appOpenCallback.onAdImpression();
                    //log event
                    EventTrackingHelper.logEventWithAParam(activity, EventTrackingHelper.inter_splash_showad_time, EventTrackingHelper.showad_time, "true_" + (System.currentTimeMillis() - AsyncSplash.Companion.getInstance().getTimeStartSplash()) / 1000);
                    int splashOpenTimes = SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1);
                    if (splashOpenTimes <= 3) {
                        EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_impression + "_" + splashOpenTimes);
                    }
                    //end log event
                }
            });
            if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                isShowingAd = true;
                appOpenAdSplash.show(activity);
            } else {
                Log.e(TAG, "SPLASH: Fail to show on background.");
                if (loadingAdsResumeDialog != null && loadingAdsResumeDialog.isShowing()) {
                    loadingAdsResumeDialog.dismiss();
                }
                isFailToShowAdSplash = true;
                if (handlerTimeoutSplash != null && runnable != null) {
                    handlerTimeoutSplash.removeCallbacks(runnable);
                }
            }
        }, 250);
    }

    public void loadAndShowAppOpenResumeSplash(AppCompatActivity activity, List<String> listIdOpenResume, AppOpenCallback appOpenCallback) {
        ArrayList<String> listIdOpenResumeTemp = new ArrayList<>(listIdOpenResume);
        //Set timeout ads splash 20s if cannot load
        runnable = () -> {
            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_id_timeout);
            if (appOpenCallback != null) {
                appOpenCallback.onNextAction();
            }
            if (handlerTimeoutSplash != null) {
                handlerTimeoutSplash = null;
            }
        };
        if (handlerTimeoutSplash != null) {
            handlerTimeoutSplash.postDelayed(runnable, Admob.getInstance().getTimeOutCallSplashAds());
        }

        // Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdOpenResumeTemp.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds() /*|| IAPManager.getInstance().isPurchase()*/) {
            Log.d(TAG, "SPLASH: Check condition loadAndShowAppOpenResumeSplash. Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + listIdOpenResumeTemp.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + Admob.getInstance().getShowAllAds() + "_IAP:" /*+ IAPManager.getInstance().isPurchase()*/);
            appOpenCallback.onNextAction();
            if (handlerTimeoutSplash != null && runnable != null) {
                handlerTimeoutSplash.removeCallbacks(runnable);
                handlerTimeoutSplash.removeCallbacksAndMessages(null);
                handlerTimeoutSplash = null;
            }
            return;
        }

        // Do not load ad if there is an unused ad or one is already loading.
        if (isLoadingAdSplash) {
            Log.d(TAG, "SPLASH: Do not load ad if there is an unused ad or one is already loading.");
            return;
        }
        if (isAdSplashAvailable()) {
            showAdSplashIfAvailable(activity, appOpenCallback);
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
        EventTrackingHelper.logEventWithMultipleParams(activity, EventTrackingHelper.inter_splash_tracking, bundle);

        //log event can request
        EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_true);
        //end log event can request

        isLoadingAdSplash = true;
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(activity, listIdOpenResumeTemp.get(0), request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                // Called when an app open ad has loaded.
                Log.i(TAG, "SPLASH: Ad was loaded.");
                appOpenAdSplash = ad;
                isLoadingAdSplash = false;
                appOpenCallback.onAdLoaded(ad);
                showAdSplashIfAvailable(activity, appOpenCallback);
                //Tracking revenue
                ad.setOnPaidEventListener(adValue -> {
                    //Adjust
                    ad.getResponseInfo();
                    AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                });

                if (handlerTimeoutSplash != null && runnable != null) {
                    handlerTimeoutSplash.removeCallbacks(runnable);
                    handlerTimeoutSplash.removeCallbacksAndMessages(null);
                    handlerTimeoutSplash = null;
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Called when an app open ad has failed to load.
                Log.e(TAG, "SPLASH: Ad Failed To Load. " + loadAdError);
                isLoadingAdSplash = false;
                if (!listIdOpenResumeTemp.isEmpty()) {
                    listIdOpenResumeTemp.remove(0);
                }
                loadAndShowAppOpenResumeSplash(activity, listIdOpenResumeTemp, appOpenCallback);
                appOpenCallback.onAdFailedToLoad();
            }
        });
    }

    public void loadAndShowAppOpenResumeSplashLoop(AppCompatActivity activity, List<String> listIdOpenResume, AppOpenCallback appOpenCallback) {
        Log.d(TAG, "SPLASH: loadAndShowAppOpenResumeSplashLoop: " + listIdOpenResume.toString());
        //Set timeout ads splash 20s if cannot load
        runnable = () -> {
            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_id_timeout);
            if (appOpenCallback != null) {
                appOpenCallback.onNextAction();
            }
            if (handlerTimeoutSplash != null) {
                handlerTimeoutSplash = null;
            }
        };
        if (handlerTimeoutSplash != null) {
            handlerTimeoutSplash.postDelayed(runnable, Admob.getInstance().getTimeOutCallSplashAds());
        }
        // Check list id size
        if (listIdOpenResume.isEmpty()) {
            Log.d(TAG, "SPLASH: loadAndShowAppOpenResumeSplashLoop: listIdOpenResume is empty.");
            appOpenCallback.onNextAction();
            if (handlerTimeoutSplash != null && runnable != null) {
                handlerTimeoutSplash.removeCallbacks(runnable);
                handlerTimeoutSplash.removeCallbacksAndMessages(null);
                handlerTimeoutSplash = null;
            }
            return;
        }
        String idOpenResume = listIdOpenResume.get(0);

        // If have action startActivity by timeout or no internet in splash, do not load ads.
        if (System.currentTimeMillis() - Admob.getInstance().getTimeStart() >= 8000 || AsyncSplash.Companion.getInstance().getTimeout() || AsyncSplash.Companion.getInstance().getNoInternetAction()) {
            Log.d(TAG, "SPLASH: If have action startActivity by timeout or no internet in splash, do not load ads. " + (System.currentTimeMillis() - Admob.getInstance().getTimeStart() >= 8000) + "_" + AsyncSplash.Companion.getInstance().getTimeout() + "_" + AsyncSplash.Companion.getInstance().getNoInternetAction());
            EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_id_timeout_8s);
            appOpenCallback.onNextAction();
            if (handlerTimeoutSplash != null && runnable != null) {
                handlerTimeoutSplash.removeCallbacks(runnable);
                handlerTimeoutSplash.removeCallbacksAndMessages(null);
                handlerTimeoutSplash = null;
            }
            return;
        }

        // Check condition
        if (!NetworkUtil.isNetworkActive(activity) || idOpenResume.isEmpty() || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds() /*|| IAPManager.getInstance().isPurchase()*/) {
            Log.d(TAG, "Check condition loadAndShowAppOpenResumeSplash. Network:" + NetworkUtil.isNetworkActive(activity) + "_IdEmpty:" + idOpenResume.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(activity) + "_ShowAllAds:" + Admob.getInstance().getShowAllAds() + "_IAP:" /*+ IAPManager.getInstance().isPurchase()*/);
            appOpenCallback.onNextAction();
            if (handlerTimeoutSplash != null && runnable != null) {
                handlerTimeoutSplash.removeCallbacks(runnable);
                handlerTimeoutSplash.removeCallbacksAndMessages(null);
                handlerTimeoutSplash = null;
            }
            return;
        }

        // Do not load ad if there is an unused ad or one is already loading.
        if (isLoadingAdSplash) {
            Log.d(TAG, "SPLASH: Do not load ad if there is an unused ad or one is already loading.");
            return;
        }
        if (isAdSplashAvailable()) {
            showAdSplashIfAvailable(activity, appOpenCallback);
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
        EventTrackingHelper.logEventWithMultipleParams(activity, EventTrackingHelper.inter_splash_tracking, bundle);

        //log event can request
        EventTrackingHelper.logEvent(activity, EventTrackingHelper.inter_splash_true);
        //end log event can request

        isLoadingAdSplash = true;
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(activity, idOpenResume, request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                // Called when an app open ad has loaded.
                Log.i(TAG, "SPLASH: Ad was loaded open splash loop. " + idOpenResume);
                appOpenAdSplash = ad;
                isLoadingAdSplash = false;
                appOpenCallback.onAdLoaded(ad);
                showAdSplashIfAvailable(activity, appOpenCallback);
                //Tracking revenue
                ad.setOnPaidEventListener(adValue -> {
                    //Adjust
                    ad.getResponseInfo();
                    AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                });

                if (handlerTimeoutSplash != null && runnable != null) {
                    handlerTimeoutSplash.removeCallbacks(runnable);
                    handlerTimeoutSplash.removeCallbacksAndMessages(null);
                    handlerTimeoutSplash = null;
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Called when an app open ad has failed to load.
                Log.e(TAG, "SPLASH: Ad Failed To Load." + loadAdError);
                isLoadingAdSplash = false;
                loadAndShowAppOpenResumeSplashLoop(activity, listIdOpenResume, appOpenCallback);
                appOpenCallback.onAdFailedToLoad();
            }
        });
    }

    public void onCheckShowSplashWhenFail(@NonNull final AppCompatActivity activity, AppOpenCallback appOpenCallback) {
        if (isFailToShowAdSplash) {
            showAdSplashIfAvailable(activity, appOpenCallback);
        }
    }
    //===========================End load ads, show ads resume in splash============================//

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        currentActivity = activity;
        if (AsyncSplash.Companion.getInstance().getKeyAdsOpenResume().isEmpty()) {
            if (AsyncSplash.Companion.getInstance().getInitResumeAdsType().equals("Normal")) {
                remoteKey = "open_resume";
            } else {
                remoteKey = "resume_wb";
            }
        } else {
            remoteKey = AsyncSplash.Companion.getInstance().getKeyAdsOpenResume();
        }
        Log.d(TAG, "onActivityStarted: " + currentActivity + "-RemoteKey: " + remoteKey);
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
        Log.d(TAG, "onStart: " + currentActivity + "-RemoteKey: " + remoteKey);
        if (AsyncSplash.Companion.getInstance().getUseAdPreloadingResume()) {
            showAdPreload(currentActivity, listIdOpenResumeAd, null, remoteKey);
        } else {
            if (AsyncSplash.Companion.getInstance().getPreloadResumeAds()) {
                showAdIfAvailable(currentActivity, listIdOpenResumeAd, null, remoteKey);
            } else {
                showAdIfAvailableWelcomeBackLoadAndShow(currentActivity, listIdOpenResumeAd, null, remoteKey, true);
            }
        }
    }
}
