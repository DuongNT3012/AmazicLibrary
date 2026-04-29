package com.diamondguide.redeemcode.ffftips;

import androidx.annotation.NonNull;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.application.AdsApplication;

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
        return "";
    }

    @NonNull
    @Override
    public String getFacebookID() {
        return "";
    }

    @NonNull
    @Override
    public String getAppID() {
        return "ca-app-pub-3940256099942544~3347511713";
    }
}
