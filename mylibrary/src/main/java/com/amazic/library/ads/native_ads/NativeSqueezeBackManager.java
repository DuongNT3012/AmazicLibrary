package com.amazic.library.ads.native_ads;

import static com.amazic.library.ads.admob.Admob.limitString;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.amazic.library.Utils.AdjustUtil;
import com.amazic.library.Utils.EventTrackingHelper;
import com.amazic.library.Utils.NetworkUtil;
import com.amazic.library.Utils.RemoteConfigHelper;
import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ump.AdsConsentManager;
import com.google.ads.noninterruptive.squeezebackad.SqueezeBackAd;
import com.google.ads.noninterruptive.squeezebackad.SqueezeBackAdEventCallback;
import com.google.ads.noninterruptive.squeezebackad.SqueezeBackAdLoadCallback;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.LoadAdError;

import java.util.List;

public class NativeSqueezeBackManager implements LifecycleEventObserver {
    private static final String TAG = "NativeManager";

    private SqueezeBackAd backAd;
    private Activity currentActivity;
    private String remoteKey;
    private List<String> listId;
    private final LifecycleOwner lifecycleOwner;
    private CountDownTimer countDownTimer;
    private boolean isReloadAds = false;
    private boolean isAlwaysReloadOnResume = false;
    private long intervalReloadNative = 0;
    private boolean isPause = false;


    public NativeSqueezeBackManager(@NonNull Activity activity, LifecycleOwner lifecycleOwner, List<String> listId, String remoteKey) {
        this.currentActivity = activity;
        this.listId = listId;
        this.remoteKey = remoteKey;
        this.lifecycleOwner = lifecycleOwner;
        this.lifecycleOwner.getLifecycle().addObserver(this);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                Log.d(TAG, "onStateChanged: ON_CREATE");
                loadAndShow();
                break;
            case ON_RESUME:
                String valueLog = isPause + " && "+ (isReloadAds || isAlwaysReloadOnResume);
                Log.d(TAG, "onStateChanged: ON_RESUME\n"+valueLog);
                if(isPause && (isReloadAds || isAlwaysReloadOnResume)){
                    isReloadAds = false;
                    loadAndShow();
                }
                isPause = false;
                break;
            case ON_PAUSE:
                Log.d(TAG, "onStateChanged: ON_PAUSE");
                isPause = true;
                cancelAutoReloadNative();
                break;
            case ON_DESTROY:
                Log.d(TAG, "onStateChanged: ON_DESTROY");
                if(backAd != null){
                    backAd.destroy();
                }
                this.lifecycleOwner.getLifecycle().removeObserver(this);
                break;
        }
    }

    public void setIntervalReloadNative(long intervalReloadNative) {
        if (intervalReloadNative > 0) {
            this.intervalReloadNative = intervalReloadNative;
            countDownTimer = new CountDownTimer(this.intervalReloadNative, 1000) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    loadAndShow();
                }
            };
        }
    }

    public void startReloadNative() {
        Log.d(TAG, "startReloadNative: " + (countDownTimer != null)
                + " && " + (this.lifecycleOwner.getLifecycle().getCurrentState()));
        if (countDownTimer != null && this.lifecycleOwner.getLifecycle().getCurrentState() == Lifecycle.State.RESUMED) {
            countDownTimer.cancel();
            countDownTimer.start();
        }
    }

    public void cancelAutoReloadNative() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    public void setReloadAds() {
        isReloadAds = true;
    }

    public void setAlwaysReloadOnResume(boolean isAlwaysReloadOnResume) {
        this.isAlwaysReloadOnResume = isAlwaysReloadOnResume;
    }

//    public void loadAds() {
//        if (!NetworkUtil.isNetworkActive(currentActivity) || listId.isEmpty() || !AdsConsentManager.getConsentResult(currentActivity) || !Admob.getInstance().getShowAllAds() || !RemoteConfigHelper.getInstance().get_config(currentActivity, remoteKey)) {
//            Log.d(TAG, "Native Squeeze Back: loadAndShow Check Condition. Network: " + NetworkUtil.isNetworkActive(currentActivity) + "_IsEmpty: " + listId.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(currentActivity) + "_showAll:" + Admob.getInstance().getShowAllAds() + "_remoteKey:" + RemoteConfigHelper.getInstance().get_config(currentActivity, remoteKey));
//            Bundle bundle = new Bundle();
//            bundle.putString("failed_message", "network_" + NetworkUtil.isNetworkActive(currentActivity) + "_isId_" + listId.isEmpty() + "_ump_" + listId.isEmpty() + "_isshowads_" + Admob.getInstance().getShowAllAds() + "_remote_" + RemoteConfigHelper.getInstance().get_config(currentActivity, remoteKey));
//
//            EventTrackingHelper.logEventWithMultipleParams(currentActivity, remoteKey + "_fail", bundle);
//            return;
//        }
//        EventTrackingHelper.logEvent(currentActivity, remoteKey+"_true");
//        SqueezeBackAd.load(
//                this.currentActivity,
//                listId.get(0),
//                new AdRequest.Builder().build(),
//                null,
//                new SqueezeBackAdLoadCallback() {
//                    @Override
//                    public void onAdLoaded(@NonNull SqueezeBackAd squeezeBackAd) {
//                        Log.d(TAG, "Native Squeeze Back: Ad loaded.");
//                        backAd = squeezeBackAd;
//                    }
//
//                    @Override
//                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                        Log.d(TAG, "Native Squeeze Back: failed to load.");
//                        Bundle bundle = new Bundle();
//                        bundle.putString("failed_message", loadAdError.getMessage());
//                        EventTrackingHelper.logEventWithMultipleParams(currentActivity, remoteKey + "_loadfail", bundle);
//                    }
//                },
//                new SqueezeBackAdEventCallback() {
//                    @Override
//                    public void onAdShown() {
//                        Log.d(TAG, "Native Squeeze Back: Ad Shown.");
//                    }
//
//                    @Override
//                    public void onAdHidden() {
//                        Log.d(TAG, "Native Squeeze Back: Ad Hidden.");
//                    }
//
//                    @Override
//                    public void onAdDestroyed() {
//                        Log.d(TAG, "Native Squeeze Back: Ad Destroyed.");
//                    }
//
//                    @Override
//                    public void onAdClicked() {
//                        Log.d(TAG, "Native Squeeze Back: Ad Click.");
//                    }
//
//                    @Override
//                    public void onAdImpression() {
//                        Log.d(TAG, "Native Squeeze Back: Ad Impression.");
//                        EventTrackingHelper.logEvent(currentActivity,remoteKey+"_view");
//                    }
//
//                    @Override
//                    public void onAdPaid(AdValue adValue) {
//                        Log.d(TAG, "Native Squeeze Back: Ad Paid.");
//                        //Adjust
//                        AdjustUtil.trackRevenue(null, adValue, listId.get(0), remoteKey);
//                    }
//                }
//        );
//    }
//
//    public void showAds() {
//        backAd.show();
//    }

    private void loadAndShow() {
        if (backAd != null) {
            backAd.destroy();
        }
        if (!NetworkUtil.isNetworkActive(currentActivity) || listId.isEmpty() || !AdsConsentManager.getConsentResult(currentActivity) || !Admob.getInstance().getShowAllAds() || !RemoteConfigHelper.getInstance().get_config(currentActivity, remoteKey)) {
            Log.d(TAG, "Native Squeeze Back: loadAndShow Check Condition. Network: " + NetworkUtil.isNetworkActive(currentActivity) + "_IsEmpty: " + listId.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(currentActivity) + "_showAll:" + Admob.getInstance().getShowAllAds() + "_remoteKey:" + RemoteConfigHelper.getInstance().get_config(currentActivity, remoteKey));

            Bundle bundle = new Bundle();
            bundle.putString("failed_message", "internet_" + NetworkUtil.isNetworkActive(currentActivity) + "_IsEmpty_" + listId.isEmpty() + "_UMP_" + AdsConsentManager.getConsentResult(currentActivity) + "_showAll_" + Admob.getInstance().getShowAllAds() + "_remoteKey_" + RemoteConfigHelper.getInstance().get_config(currentActivity, remoteKey));

            EventTrackingHelper.logEventWithMultipleParams(
                    currentActivity,
                    remoteKey + "_config_failed",
                    bundle
            );
            return;
        }
        EventTrackingHelper.logEvent(currentActivity, remoteKey + "_true");
        SqueezeBackAd.load(
                this.currentActivity,
                listId.get(0),
                new AdRequest.Builder().build(),
                null,
                new SqueezeBackAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull SqueezeBackAd squeezeBackAd) {
                        Log.d(TAG, "Native Squeeze Back: onAdLoaded. " + remoteKey);
                        backAd = squeezeBackAd;
                        backAd.show();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Native Squeeze Back: onAdFailedToLoad. " + loadAdError + ". " + remoteKey);
                        Bundle bundle = new Bundle();
                        bundle.putString("failed_message", limitString(loadAdError.getMessage(), 99));
                        if (loadAdError.getResponseInfo() != null && loadAdError.getResponseInfo().getLoadedAdapterResponseInfo() != null && loadAdError.getMessage().toLowerCase().contains("no fill")) {
                            bundle.putString("no_fill_source", limitString(loadAdError.getResponseInfo().getLoadedAdapterResponseInfo().getAdSourceName(), 99));
                        }
                        EventTrackingHelper.logEventWithMultipleParams(currentActivity, remoteKey + "_failed", bundle);
                        //
                        startReloadNative();
                    }
                },
                new SqueezeBackAdEventCallback() {
                    @Override
                    public void onAdShown() {
                        Log.d(TAG, "Native Squeeze Back: onAdShown. " + remoteKey);
                    }

                    @Override
                    public void onAdHidden() {
                        Log.d(TAG, "Native Squeeze Back: onAdHidden. " + remoteKey);
                        EventTrackingHelper.logEvent(currentActivity, remoteKey + "_hidden");
                    }

                    @Override
                    public void onAdDestroyed() {
                        Log.d(TAG, "Native Squeeze Back: onAdDestroyed. " + remoteKey);
                    }

                    @Override
                    public void onAdClicked() {
                        Log.d(TAG, "Native Squeeze Back: onAdClicked. " + remoteKey);
                        EventTrackingHelper.logEvent(currentActivity, remoteKey + "_click");
                    }

                    @Override
                    public void onAdImpression() {
                        Log.d(TAG, "Native Squeeze Back: onAdImpression. " + remoteKey);
                        EventTrackingHelper.logEvent(currentActivity, remoteKey + "_view");
                        //
                        startReloadNative();
                    }

                    @Override
                    public void onAdPaid(AdValue adValue) {
                        //Adjust
                        AdjustUtil.trackRevenue(null, adValue, listId.get(0), remoteKey);
                    }
                }
        );
    }
}
