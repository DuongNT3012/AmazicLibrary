package com.amazic.library.ads.native_ads;

import static com.amazic.library.ads.admob.Admob.limitString;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

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

public class NativeSqueezeBackManager {
    private static final String TAG = "NativeManager";

    private SqueezeBackAd backAd;
    private Activity currentActivity;
    private String remoteKey;
    private List<String> listId;

    public NativeSqueezeBackManager(@NonNull Activity activity, List<String> listId, String remoteKey) {
        this.currentActivity = activity;
        this.listId = listId;
        this.remoteKey = remoteKey;
    }

    public void loadAds() {
        if (!NetworkUtil.isNetworkActive(currentActivity) || listId.isEmpty() || !AdsConsentManager.getConsentResult(currentActivity) || !Admob.getInstance().getShowAllAds() || !RemoteConfigHelper.getInstance().get_config(currentActivity, remoteKey)) {
            Log.d(TAG, "Native Squeeze Back: loadAndShow Check Condition. Network: " + NetworkUtil.isNetworkActive(currentActivity) + "_IsEmpty: " + listId.isEmpty() + "_UMP:" + AdsConsentManager.getConsentResult(currentActivity) + "_showAll:" + Admob.getInstance().getShowAllAds() + "_remoteKey:" + RemoteConfigHelper.getInstance().get_config(currentActivity, remoteKey));
            return;
        }

        SqueezeBackAd.load(
                this.currentActivity,
                listId.get(0),
                new AdRequest.Builder().build(),
                null,
                new SqueezeBackAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull SqueezeBackAd squeezeBackAd) {
                        backAd = squeezeBackAd;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {

                    }
                },
                new SqueezeBackAdEventCallback() {
                    @Override
                    public void onAdShown() {

                    }

                    @Override
                    public void onAdHidden() {

                    }

                    @Override
                    public void onAdDestroyed() {

                    }

                    @Override
                    public void onAdClicked() {

                    }

                    @Override
                    public void onAdImpression() {

                    }

                    @Override
                    public void onAdPaid(AdValue adValue) {

                    }
                }
        );
    }

    public void showAds() {
        backAd.show();
    }

    public void loadAndShow() {
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
                        squeezeBackAd.show();
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
