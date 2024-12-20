package com.amazic.library.ads.splash_ads;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.ads.callback.AppOpenCallback;
import com.amazic.library.ads.callback.InterCallback;

import java.util.Random;

public class AdsSplash {
    private static final String TAG = "AdsSplash";
    private STATE state = STATE.NO_ADS;

    enum STATE {INTER, OPEN, NO_ADS}

    private boolean isLoopAdsSplash = false;

    public static AdsSplash init(boolean showOpen, boolean showInter, String rate) {
        AdsSplash adsSplash = new AdsSplash();
        Log.d(TAG, "init: ");
        if (!Admob.getInstance().getShowAllAds()) {
            adsSplash.setState(STATE.NO_ADS);
        } else if (showInter && showOpen) {
            adsSplash.checkShowInterOpenSplash(rate);
        } else if (showInter) {
            adsSplash.setState(STATE.INTER);
        } else if (showOpen) {
            adsSplash.setState(STATE.OPEN);
        } else {
            adsSplash.setState(STATE.NO_ADS);
        }
        return adsSplash;
    }

    public void setLoopAdsSplash(boolean isLoopAdsSplash) {
        this.isLoopAdsSplash = isLoopAdsSplash;
    }

    private void checkShowInterOpenSplash(String rate) {
        int rateInter;
        int rateOpen;
        try {
            rateInter = Integer.parseInt(rate.trim().split("_")[1].trim());
            rateOpen = Integer.parseInt(rate.trim().split("_")[0].trim());
        } catch (Exception e) {
            Log.d(TAG, "checkShowInterOpenSplash: ");
            rateInter = 0;
            rateOpen = 0;
        }
        Log.d(TAG, "rateInter: " + rateInter + " - rateOpen: " + rateOpen);
        Log.d(TAG, "rateInter: " + rateInter + " - rateOpen: " + rateOpen);
        if (rateInter >= 0 && rateOpen >= 0 && rateInter + rateOpen == 100) {
            boolean isShowOpenSplash = new Random().nextInt(100) + 1 < rateOpen;
            setState(isShowOpenSplash ? STATE.OPEN : STATE.INTER);
        } else {
            setState(STATE.NO_ADS);
        }
    }

    public void setState(STATE state) {
        this.state = state;
    }

    public STATE getState() {
        return state;
    }

    public void showAdsSplashApi(AppCompatActivity activity, AppOpenCallback appOpenCallback, InterCallback interCallback) {
        Log.d(TAG, "state show: " + getState());
        if (getState() == STATE.OPEN) {
            if (this.isLoopAdsSplash) {
                AdmobApi.getInstance().loadOpenAppAdSplashLoop(activity, appOpenCallback);
            } else {
                AdmobApi.getInstance().loadOpenAppAdSplashFloor(activity, appOpenCallback);
            }
        } else if (getState() == STATE.INTER) {
            if (this.isLoopAdsSplash) {
                AdmobApi.getInstance().loadInterAdSplashLoop(activity, interCallback);
            } else {
                AdmobApi.getInstance().loadInterAdSplashFloor(activity, interCallback);
            }
        } else {
            interCallback.onNextAction();
        }
    }

    public void onCheckShowSplashWhenFail(AppCompatActivity activity, AppOpenCallback appOpenCallback, InterCallback interCallback) {
        if (getState() == STATE.OPEN) {
            AppOpenManager.getInstance().onCheckShowSplashWhenFail(activity, appOpenCallback);
        } else if (getState() == STATE.INTER) {
            Admob.getInstance().onCheckShowSplashWhenFail(activity, interCallback);
        }
    }
}
