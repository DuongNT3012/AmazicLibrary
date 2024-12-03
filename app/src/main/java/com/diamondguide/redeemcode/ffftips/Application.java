package com.diamondguide.redeemcode.ffftips;

import androidx.annotation.NonNull;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.application.AdsApplication;

public class Application extends AdsApplication {
    @Override
    public void onCreate() {
        super.onCreate();
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
}
