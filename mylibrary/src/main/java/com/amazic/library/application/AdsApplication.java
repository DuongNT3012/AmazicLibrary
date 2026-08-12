package com.amazic.library.application;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.LogLevel;
import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.splash_ads.AsyncSplash;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AudienceNetworkAds;
import com.google.android.gms.ads.MobileAds;

public abstract class AdsApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "AdsApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Admob.getInstance().setTimeStart(System.currentTimeMillis());
        initAdmob();
        initMeta();
        setUpAdjust();
        registerActivityLifecycleCallbacks(this);
    }

    private void initAdmob() {
        new Thread(() -> {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(this, initializationStatus -> {
                Log.d("Admob", "initAdmob: application - " + initializationStatus.getAdapterStatusMap());
                Admob.getInstance().setIsInitAdmobDone(true);
            });
        }).start();
    }

    private void initMeta() {
//        if (AsyncSplash.Companion.getInstance().getIsUseNativeSplashMeta()) {
        /// chi dung khi debug test ads meta
        Log.d("Admob", "initMeta: Value Test Mode Meta = " + Admob.getInstance().getTestModeMeta());
        if (Admob.getInstance().getTestModeMeta()) {
            Log.d("Admob", "initMeta - check buildDebug setTestMode = " + buildDebug());
            AdSettings.setTestMode(buildDebug());
        } else {
            AdSettings.setTestMode(Admob.getInstance().getTestModeMeta());
        }

        if (!AudienceNetworkAds.isInitialized(this)) {
            AudienceNetworkAds.buildInitSettings(this)
                    .withInitListener(result -> Log.d("Admob", "initMeta: " + result.getMessage()))
                    .initialize();
        }
//        }
    }

    private void setUpAdjust() {
        String environment;
        environment = buildDebug() ? AdjustConfig.ENVIRONMENT_SANDBOX : AdjustConfig.ENVIRONMENT_PRODUCTION;
        AdjustConfig config = new AdjustConfig(this, getAppTokenAdjust(), environment);
        config.setLogLevel(LogLevel.VERBOSE);
        config.setFbAppId(getFacebookID());
        config.setDefaultTracker(getAppTokenAdjust());
        config.enableSendingInBackground();
        Adjust.initSdk(config);
        // Enable the SDK
        Adjust.enable();
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        Adjust.onResume();
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        Adjust.onPause();
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        Log.d(TAG, "onActivityDestroyed: ");
    }

    @NonNull
    public abstract String getAppTokenAdjust();

    @NonNull
    public abstract String getFacebookID();

    @NonNull
    public abstract Boolean buildDebug();
}
