package com.amazic.library.ads.banner_ads;

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
import com.amazic.library.ump.AdsConsentManager;
import com.google.ads.noninterruptive.pictureinpicturead.PictureInPictureAd;
import com.google.ads.noninterruptive.pictureinpicturead.PictureInPictureAdEventCallback;
import com.google.ads.noninterruptive.pictureinpicturead.PictureInPictureAdLoadCallback;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.LoadAdError;

import java.util.List;

public class BannerPictureInPictureManager implements LifecycleEventObserver {
    private static final String TAG = "BannerManager";
    private Activity currentActivity;
    private PictureInPictureAd pipAd;
    private String remoteKey;
    private List<String> listId;
    private CountDownTimer countDownTimer;
    private boolean isReloadAds = false;
    private boolean isAlwaysReloadOnResume = false;
    private long intervalReloadBanner = 0;
    private final LifecycleOwner lifecycleOwner;
    private boolean isPause = false;
    private PictureInPictureAd.AdPosition positionPIP = PictureInPictureAd.AdPosition.BOTTOM_RIGHT;
    private boolean isLoading = false; // Flag check state load


    public BannerPictureInPictureManager(@NonNull Activity activity, LifecycleOwner lifecycleOwner, List<String> listId, String remoteKey) {
        this.currentActivity = activity;
        this.listId = listId;
        this.remoteKey = remoteKey;
        this.lifecycleOwner = lifecycleOwner;
        this.lifecycleOwner.getLifecycle().addObserver(this);
        this.pipAd = new PictureInPictureAd(activity, false);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                Log.d(TAG, "onStateChanged: ON_CREATE");
                loadAndShow();
                break;
            case ON_RESUME:
                String valueLog = isPause + " && " + (isReloadAds || isAlwaysReloadOnResume);
                Log.d(TAG, "onStateChanged: ON_RESUME\n" + valueLog);
                if (isPause && (isReloadAds || isAlwaysReloadOnResume)) {
                    isReloadAds = false;
                    loadAndShow();
                }
                isPause = false;
                break;
            case ON_PAUSE:
                Log.d(TAG, "onStateChanged: ON_PAUSE");
                isPause = true;
                cancelAutoReloadBanner();
                break;
            case ON_DESTROY:
                Log.d(TAG, "onStateChanged: ON_DESTROY");
                if (pipAd != null) {
                    pipAd.destroy();
                }
                this.lifecycleOwner.getLifecycle().removeObserver(this);
                break;
        }
    }

    public void setPosition(PictureInPictureAd.AdPosition position) {
        this.positionPIP = position;
    }

    public void setReloadAds() {
        isReloadAds = true;
    }

    public void setAlwaysReloadOnResume(boolean isAlwaysReloadOnResume) {
        this.isAlwaysReloadOnResume = isAlwaysReloadOnResume;
    }

    public void setIntervalReloadBanner(long intervalReloadBanner) {
        if (intervalReloadBanner > 0) {
            Log.d(TAG, "setIntervalReloadBanner: start countDown reload");
            this.intervalReloadBanner = intervalReloadBanner;
            countDownTimer = new CountDownTimer(this.intervalReloadBanner, 1000) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "setIntervalReloadBanner: reload");
                    loadAndShow();
                }
            };
        }
    }

    private void startReloadBanner() {
        if (countDownTimer != null && this.lifecycleOwner.getLifecycle().getCurrentState() == Lifecycle.State.RESUMED) {
            countDownTimer.cancel();
            countDownTimer.start();
        }
    }

    public void cancelAutoReloadBanner() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    public void resumeAutoReloadBanner() {
        if (countDownTimer != null) {
            countDownTimer.start();
        }
    }

    private void loadAndShow() {
        if (isLoading) {
            Log.d(TAG, "Picture In Picture: loadAndShow - Ad is already loading, ignore this request ");
            return;
        }
        if (pipAd != null) {
            pipAd.destroy();
        }

        if (!NetworkUtil.isNetworkActive(currentActivity) || listId.isEmpty() || !AdsConsentManager.getConsentResult(currentActivity) || !Admob.getInstance().getShowAllAds() || !RemoteConfigHelper.getInstance().get_config(currentActivity, remoteKey)) {
            Log.d(TAG, "Picture In Picture: loadAndShow Check Condition. Network: " + NetworkUtil.isNetworkActive(currentActivity) + "_IsEmpty: " + listId.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(currentActivity) + "_showAll:" + Admob.getInstance().getShowAllAds() + "_remoteKey:" + RemoteConfigHelper.getInstance().get_config(currentActivity, remoteKey));

            Bundle bundle = new Bundle();
            bundle.putString("failed_message", "network_" + NetworkUtil.isNetworkActive(currentActivity) + "_isId_" + listId.isEmpty() + "_ump_" + listId.isEmpty() + "_isshowads_" + Admob.getInstance().getShowAllAds() + "_remote_" + RemoteConfigHelper.getInstance().get_config(currentActivity, remoteKey));

            EventTrackingHelper.logEventWithMultipleParams(currentActivity, remoteKey + "_fail", bundle);
            return;
        }
        isLoading = true;
        Log.d(TAG, "Picture In Picture: loadAndShow - start Load request true");
        EventTrackingHelper.logEvent(currentActivity, remoteKey + "_true");
        pipAd.load(listId.get(0),
                new AdRequest.Builder().build(),
                new PictureInPictureAdLoadCallback() {
                    @Override
                    public void onAdLoaded() {
                        isLoading = false;
                        Log.d(TAG, "Picture In Picture Ad loaded.");
                        if (lifecycleOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                            pipAd.show(currentActivity, positionPIP);
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        isLoading = false;
                        Log.d(TAG, "Picture In Picture Failed to load: " +
                                loadAdError.getMessage());

                        Bundle bundle = new Bundle();
                        bundle.putString("failed_message", loadAdError.getMessage());
                        EventTrackingHelper.logEventWithMultipleParams(currentActivity, remoteKey + "_loadfail", bundle);

                        //interval reload
                        startReloadBanner();
                    }
                },
                new PictureInPictureAdEventCallback() {
                    @Override
                    public void onAdHidden() {
                        Log.d(TAG, "Picture In Picture Ad hidden.");
                    }

                    @Override
                    public void onAdDestroyed() {
                        Log.d(TAG, "Picture In Picture Ad destroyed.");
                    }

                    @Override
                    public void onAdClicked() {
                        Log.d(TAG, "Picture In Picture Ad clicked.");
                    }

                    @Override
                    public void onAdOpened() {
                        Log.d(TAG, "Picture In Picture Ad opened.");
                    }

                    @Override
                    public void onAdClosed() {
                        Log.d(TAG, "Picture In Picture Ad closed.");
                    }

                    @Override
                    public void onAdImpression() {
                        Log.d(TAG, "Picture In Picture Ad impression.");
                        EventTrackingHelper.logEvent(currentActivity, remoteKey + "_view");

                        //interval reload
                        startReloadBanner();
                    }

                    @Override
                    public void onAdPaid(AdValue value) {
                        Log.d(TAG, "Picture In Picture Ad Paid with: " +
                                value.getValueMicros() + " " + value.getCurrencyCode());
                        //Adjust
                        AdjustUtil.trackRevenue(null, value, listId.get(0), remoteKey);
                    }
                });
    }
}
