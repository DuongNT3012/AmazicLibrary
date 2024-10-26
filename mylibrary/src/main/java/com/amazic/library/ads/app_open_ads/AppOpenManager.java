package com.amazic.library.ads.app_open_ads;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.amazic.library.ads.Utils.NetworkUtil;
import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.callback.AppOpenCallback;
import com.amazic.library.dialog.LoadingAdsResumeDialog;
import com.amazic.library.ump.AdsConsentManager;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

import java.util.Date;
import java.util.List;

public class AppOpenManager implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private static final String TAG = "AppOpenManager";
    private static AppOpenManager INSTANCE;
    private AppOpenAd appOpenAd = null;
    private boolean isLoadingAd = false;
    private boolean isShowingAd = false;
    private long loadTime = 0;
    private Activity currentActivity;
    private Application application;
    private LoadingAdsResumeDialog loadingAdsResumeDialog;
    private AppOpenCallback appOpenCallback;
    private List<String> listIdOpenResumeAd;

    public static AppOpenManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AppOpenManager();
        }
        return INSTANCE;
    }

    public boolean isShowingAd() {
        return isShowingAd;
    }

    public void init(Application application, List<String> listIdOpenResume, AppOpenCallback appOpenCallback) {
        this.listIdOpenResumeAd = listIdOpenResume;
        this.appOpenCallback = appOpenCallback;
        this.application = application;
        this.application.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
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
        Log.d(TAG, "isAdAvailable: appOpenAd = : " + appOpenAd + "-wasLoadTimeLessThanNHoursAgo: " + wasLoadTimeLessThanNHoursAgo(4));
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
    }

    public void loadAd(Activity activity, List<String> listIdOpenResume, AppOpenCallback appOpenCallback) {
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
                appOpenCallback.onAdLoaded(ad);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Called when an app open ad has failed to load.
                Log.d(TAG, "Ad Failed To Load.");
                Log.d(TAG, loadAdError.getMessage());
                isLoadingAd = false;
                listIdOpenResume.remove(0);
                loadAd(activity, listIdOpenResume, appOpenCallback);
                appOpenCallback.onAdFailedToLoad(loadAdError);
            }
        });
    }

    public void showAdIfAvailable(@NonNull final Activity activity, List<String> listIdOpenResume, AppOpenCallback appOpenCallback) {
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
        // If the app open ad is not available yet, invoke the callback then load the ad.
        if (!isAdAvailable()) {
            Log.d(TAG, "The app open ad is not ready yet.");
            //onShowAdCompleteListener.onShowAdComplete();
            loadAd(activity, listIdOpenResume, appOpenCallback);
            return;
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

                if (loadingAdsResumeDialog != null && loadingAdsResumeDialog.isShowing()) {
                    loadingAdsResumeDialog.dismiss();
                }
                loadAd(activity, listIdOpenResume, appOpenCallback);
                appOpenCallback.onAdDismissedFullScreenContent();
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
                loadAd(activity, listIdOpenResume, appOpenCallback);
                appOpenCallback.onAdFailedToShowFullScreenContent();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                // Called when fullscreen content is shown.
                Log.d(TAG, "Ad showed fullscreen content.");
                appOpenCallback.onAdShowedFullScreenContent();
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                Log.d(TAG, "onAdClicked.");
                appOpenCallback.onAdClicked();
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                Log.d(TAG, "onAdImpression");
                appOpenCallback.onAdImpression();
            }
        });
        isShowingAd = true;
        appOpenAd.show(activity);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        Log.d(TAG, "onActivityStarted: " + currentActivity);
        currentActivity = activity;
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
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (currentActivity != null) {
                showAdIfAvailable(currentActivity, listIdOpenResumeAd, appOpenCallback);
            }
        }, 150);
    }
}
