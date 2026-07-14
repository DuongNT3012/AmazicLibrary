package com.footballscores.livescore.soccerscores.sports;

import androidx.annotation.NonNull;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.application.AdsApplication;
import com.amazic.mylibrary.BuildConfig;

public class Application extends AdsApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity.class);
        Admob.getInstance().setTokenEventAdjust("xxxxxx");
    }

    @NonNull
    @Override
    public String getAppTokenAdjust() {
        return null;
    }

    @NonNull
    @Override
    public String getFacebookID() {
        return null;
    }

    @NonNull
    @Override
    public Boolean buildDebug() {
        return BuildConfig.DEBUG;
    }
}
