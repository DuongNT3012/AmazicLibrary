package com.amazic.library.ads.splash_ads;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.amazic.library.Utils.RemoteConfigHelper;
import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.ads.callback.AppOpenCallback;
import com.amazic.library.ads.callback.InterCallback;

import java.util.Random;

public class AdsSplash {
    private static final String TAG = "AdsSplash";
    private STATE state = STATE.INTER;

    enum STATE {INTER, OPEN}

    private boolean isLoopAdsSplash = false;
    private String keyAdsInterSplash = "inter_splash";
    private String keyAdsOpenSplash = "open_splash";

    public void setKeyAdsInterSplash(String keyAdsInterSplash) {
        this.keyAdsInterSplash = keyAdsInterSplash;
    }

    public void setKeyAdsOpenSplash(String keyAdsOpenSplash) {
        this.keyAdsOpenSplash = keyAdsOpenSplash;
    }

    public static AdsSplash init(boolean showOpen, boolean showInter, String rate) {
        AdsSplash adsSplash = new AdsSplash();
        Log.d(TAG, "init: ");
        if (showInter && showOpen) {
            adsSplash.checkShowInterOpenSplash(rate);
        } else if (showInter) {
            adsSplash.setState(STATE.INTER);
        } else if (showOpen) {
            adsSplash.setState(STATE.OPEN);
        } else {
            /// TH sai set default Inter
            adsSplash.setState(STATE.INTER);
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
            /// TH sai set default Inter
            setState(STATE.INTER);
        }
    }

    public void setState(STATE state) {
        this.state = state;
    }

    public STATE getState() {
        return state;
    }

    public void showAdsSplashApi(AppCompatActivity activity, AppOpenCallback appOpenCallback, InterCallback interCallback, String adsKeyNative, String remoteKeyNative) {
        Log.d(TAG, "state show: " + getState());
        if (AsyncSplash.Companion.getInstance().getUseAdPreloading()) {
            Log.d(TAG, "AdsSplash preload: USE Preload " +getState());
            if (getState() == STATE.OPEN) {
                AdmobApi.getInstance().loadAndShowAppOpenAdPreloadingSplash(activity,keyAdsOpenSplash, appOpenCallback);
            } else {
                AdmobApi.getInstance().loadAndShowInterAdPreloadingSplash(activity, keyAdsInterSplash, interCallback, adsKeyNative, remoteKeyNative);
            }
        } else {
            Log.d(TAG, "AdsSplash preload: USE Normal "+getState());
            if (getState() == STATE.OPEN) {
                if (this.isLoopAdsSplash) {
                    AdmobApi.getInstance().loadOpenAppAdSplashLoop(activity, keyAdsOpenSplash, appOpenCallback);
                } else {
                    AdmobApi.getInstance().loadOpenAppAdSplashFloor(activity, keyAdsOpenSplash, appOpenCallback);
                }
            } else {
                if (this.isLoopAdsSplash) {
                    Log.d(TAG, "Show Ads 1");
                    AdmobApi.getInstance().loadInterAdSplashLoop(activity, keyAdsInterSplash, interCallback);
                } else {
                    if (!AsyncSplash.Companion.getInstance().getLoadAndShowIdInterAdSplashAsync()) {
                        Log.d(TAG, "Show Ads 2");
//                    AdmobApi.getInstance().loadInterAdSplashFloor(activity, keyAdsInterSplash, interCallback);
                        AdmobApi.getInstance().loadInterAdSplashFloorDelayAds(activity, keyAdsInterSplash, interCallback, adsKeyNative, remoteKeyNative);
                    } else {
                        Log.d(TAG, "Show Ads 3");
                        AdmobApi.getInstance().loadAndShowIdInterAdSplashAsync(activity, keyAdsInterSplash, interCallback);
                    }
                }
            }
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
