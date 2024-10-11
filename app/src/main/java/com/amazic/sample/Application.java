package com.amazic.sample;

import androidx.annotation.NonNull;

import com.amazic.library.application.AdsApplication;

public class Application extends AdsApplication {
    @Override
    public void onCreate() {
        super.onCreate();
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

    @Override
    public Boolean buildDebug() {
        return false;
    }
}
