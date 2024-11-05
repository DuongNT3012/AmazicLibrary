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
import com.amazic.library.Utils.NetworkUtil;
import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.callback.AppOpenCallback;
import com.amazic.library.dialog.LoadingAdsResumeDialog;
import com.amazic.library.ump.AdsConsentManager;
import com.google.android.gms.ads.AdActivity;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

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
    private List<String> listIdOpenResumeAd = new ArrayList<>();
    private boolean isFailToShowAdSplash = false;
    private final ArrayList<Class> disabledAppOpenList = new ArrayList<>();
    private boolean isShowWelcomeBelowAdsResume = false;
    private Class welcomeBackClass = null;
    private Handler handlerTimeoutSplash = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private boolean isSplashResume = true;

    public boolean isEnableResume() {
        return isEnableResume;
    }

    public void setEnableResume(boolean enableResume) {
        isEnableResume = enableResume;
    }

    private boolean isEnableResume = true;

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
        Log.d(TAG, "SPLASH: isAdAvailable: appOpenAd = " + appOpenAdSplash);
        return appOpenAdSplash != null;
    }

    //===========================Start load ads, show ads resume in normal activity============================//
    public void loadAd(Activity activity, List<String> listIdOpenResume, AppOpenCallback appOpenCallback) {
        // Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdOpenResume.size() == 0 || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds()) {
            Log.d(TAG, "Check condition.");
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
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(activity, listIdOpenResume.get(0), request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                // Called when an app open ad has loaded.
                Log.d(TAG, "Ad was loaded.");
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
                // Called when an app open ad has failed to load.
                Log.d(TAG, "Ad Failed To Load.");
                Log.d(TAG, loadAdError.getMessage());
                isLoadingAd = false;
                listIdOpenResume.remove(0);
                appOpenCallback.onAdFailedToLoad(loadAdError);
                loadAd(activity, listIdOpenResume, appOpenCallback);
            }
        });
    }

    public void loadAd(Activity activity, List<String> listIdOpenResume) {
        // Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdOpenResume.size() == 0 || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds()) {
            Log.d(TAG, "Check condition.");
            return;
        }
        // Do not load ad if there is an unused ad or one is already loading.
        if (isLoadingAd || isAdAvailable()) {
            Log.d(TAG, "Do not load ad if there is an unused ad or one is already loading.");
            return;
        }
        isLoadingAd = true;
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(activity, listIdOpenResume.get(0), request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                // Called when an app open ad has loaded.
                Log.d(TAG, "Ad was loaded.");
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
                // Called when an app open ad has failed to load.
                Log.d(TAG, "Ad Failed To Load.");
                Log.d(TAG, loadAdError.getMessage());
                isLoadingAd = false;
                listIdOpenResume.remove(0);
                loadAd(activity, listIdOpenResume);
            }
        });
    }

    public void showAdIfAvailable(@NonNull final Activity activity, List<String> listIdOpenResume, AppOpenCallback appOpenCallback) {
        //Ads resume is disabled
        if (!isEnableResume){
            Log.d(TAG, "Ads resume is disabled");
            return;
        }
        // If the app open ad is not available yet, invoke the callback then load the ad.
        if (!isAdAvailable()) {
            Log.d(TAG, "The app open ad is not ready yet.");
            //onShowAdCompleteListener.onShowAdComplete();
            loadAd(activity, listIdOpenResume);
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
                    Log.d(TAG, "onStart: activity is disabled");
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
        loadingAdsResumeDialog = new LoadingAdsResumeDialog(activity);
        if (!loadingAdsResumeDialog.isShowing()) {
            loadingAdsResumeDialog.show();
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {

                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when fullscreen content is dismissed.
                    // Set the reference to null so isAdAvailable() returns false.
                    Log.d(TAG, "Ad dismissed fullscreen content.");
                    appOpenAd = null;
                    isShowingAd = false;

                    loadAd(activity, listIdOpenResume);
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdDismissedFullScreenContent();
                    }
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    // Called when fullscreen content failed to show.
                    // Set the reference to null so isAdAvailable() returns false.
                    Log.d(TAG, "Ad Failed To Show FullScreen Content");
                    Log.d(TAG, adError.getMessage());
                    appOpenAd = null;
                    isShowingAd = false;

                    if (loadingAdsResumeDialog != null && loadingAdsResumeDialog.isShowing()) {
                        loadingAdsResumeDialog.dismiss();
                    }
                    loadAd(activity, listIdOpenResume);
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdFailedToShowFullScreenContent();
                    }
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when fullscreen content is shown.
                    Log.d(TAG, "Ad showed fullscreen content.");
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
                    Log.d(TAG, "onAdClicked.");
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdClicked();
                    }
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    Log.d(TAG, "onAdImpression");
                    if (appOpenCallback != null) {
                        appOpenCallback.onAdImpression();
                    }
                }
            });
            isShowingAd = true;
            appOpenAd.show(activity);
        }, 250);
    }

    public void showAdIfAvailableWelcomeBack(@NonNull final Activity activity, List<String> listIdOpenResume, AppOpenCallback appOpenCallback) {
        //Ads resume is disabled.
        if (!isEnableResume){
            Log.d(TAG, "Ads resume is disabled.");
            return;
        }
        // If the app open ad is not available yet, invoke the callback then load the ad.
        if (!isAdAvailable()) {
            Log.d(TAG, "The app open ad is not ready yet.");
            //onShowAdCompleteListener.onShowAdComplete();
            loadAd(activity, listIdOpenResume);
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
                // Called when fullscreen content is dismissed.
                // Set the reference to null so isAdAvailable() returns false.
                Log.d(TAG, "Ad dismissed fullscreen content.");
                appOpenAd = null;
                isShowingAd = false;

                loadAd(activity, listIdOpenResume);
                if (appOpenCallback != null) {
                    appOpenCallback.onAdDismissedFullScreenContent();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                // Called when fullscreen content failed to show.
                // Set the reference to null so isAdAvailable() returns false.
                Log.d(TAG, "Ad Failed To Show FullScreen Content");
                Log.d(TAG, adError.getMessage());
                appOpenAd = null;
                isShowingAd = false;

                if (loadingAdsResumeDialog != null && loadingAdsResumeDialog.isShowing()) {
                    loadingAdsResumeDialog.dismiss();
                }
                loadAd(activity, listIdOpenResume);
                if (appOpenCallback != null) {
                    appOpenCallback.onAdFailedToShowFullScreenContent();
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                // Called when fullscreen content is shown.
                Log.d(TAG, "Ad showed fullscreen content.");
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
                Log.d(TAG, "onAdClicked.");
                if (appOpenCallback != null) {
                    appOpenCallback.onAdClicked();
                }
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                Log.d(TAG, "onAdImpression");
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
        if (!isAdSplashAvailable()) {
            Log.d(TAG, "SPLASH: The app open ad is not ready yet.");
            //onShowAdCompleteListener.onShowAdComplete();
            return;
        }

        loadingAdsResumeDialog = new LoadingAdsResumeDialog(activity);
        if (!loadingAdsResumeDialog.isShowing()) {
            loadingAdsResumeDialog.show();
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            appOpenAdSplash.setFullScreenContentCallback(new FullScreenContentCallback() {

                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when fullscreen content is dismissed.
                    // Set the reference to null so isAdAvailable() returns false.
                    Log.d(TAG, "SPLASH: Ad dismissed fullscreen content.");
                    appOpenAdSplash = null;
                    isShowingAd = false;

                    appOpenCallback.onAdDismissedFullScreenContent();
                    appOpenCallback.onNextAction();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    // Called when fullscreen content failed to show.
                    // Set the reference to null so isAdAvailable() returns false.
                    Log.d(TAG, "SPLASH: Ad Failed To Show FullScreen Content");
                    //appOpenAdSplash = null;
                    isShowingAd = false;

                    if (loadingAdsResumeDialog != null && loadingAdsResumeDialog.isShowing()) {
                        loadingAdsResumeDialog.dismiss();
                    }
                    appOpenCallback.onAdFailedToShowFullScreenContent();
                    if (isSplashResume) {
                        appOpenCallback.onNextAction();
                    }
                    isFailToShowAdSplash = true;
                    if (handlerTimeoutSplash != null && runnable != null) {
                        handlerTimeoutSplash.removeCallbacks(runnable);
                    }
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
                    Log.d(TAG, "SPLASH: onAdClicked.");
                    appOpenCallback.onAdClicked();
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    Log.d(TAG, "SPLASH: onAdImpression");
                    appOpenCallback.onAdImpression();
                }
            });
            if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                isShowingAd = true;
                appOpenAdSplash.show(activity);
            } else {
                Log.d(TAG, "Fail to show on background.");
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
        //Set timeout ads splash 20s if cannot load
        runnable = () -> {
            if (appOpenCallback != null) {
                appOpenCallback.onNextAction();
            }
        };
        //handlerTimeoutSplash.postDelayed(runnable, 20000);
        // Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdOpenResume.size() == 0 || !AdsConsentManager.getConsentResult(activity) || !Admob.getInstance().getShowAllAds()) {
            Log.d(TAG, "SPLASH: Check condition.");
            appOpenCallback.onNextAction();
            if (handlerTimeoutSplash != null && runnable != null) {
                handlerTimeoutSplash.removeCallbacks(runnable);
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
        isLoadingAdSplash = true;
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(activity, listIdOpenResume.get(0), request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                // Called when an app open ad has loaded.
                Log.d(TAG, "SPLASH: Ad was loaded.");
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
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Called when an app open ad has failed to load.
                Log.d(TAG, "SPLASH: Ad Failed To Load.");
                isLoadingAdSplash = false;
                listIdOpenResume.remove(0);
                loadAndShowAppOpenResumeSplash(activity, listIdOpenResume, appOpenCallback);
                appOpenCallback.onAdFailedToLoad(loadAdError);
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
        Log.d(TAG, "onActivityStarted: " + currentActivity);
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
        Log.d(TAG, "onStart: " + currentActivity);
        showAdIfAvailable(currentActivity, listIdOpenResumeAd, null);
    }
}
