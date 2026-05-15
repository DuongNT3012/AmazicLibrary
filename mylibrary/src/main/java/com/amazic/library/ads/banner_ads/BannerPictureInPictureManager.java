package com.amazic.library.ads.banner_ads;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ads.noninterruptive.pictureinpicturead.PictureInPictureAd;
import com.google.ads.noninterruptive.pictureinpicturead.PictureInPictureAdEventCallback;
import com.google.ads.noninterruptive.pictureinpicturead.PictureInPictureAdLoadCallback;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.LoadAdError;

public class BannerPictureInPictureManager {
    private static final String TAG = "BannerManager";
    private Activity currentActivity;
    private PictureInPictureAd pipAd;
    private String remoteKey;
    private String adsKey;

    public BannerPictureInPictureManager(@NonNull Activity activity, String adsKey) {
        this.currentActivity = activity;
        this.adsKey = adsKey;
        this.pipAd = new PictureInPictureAd(activity, false);
    }

    public void loadBanner() {
        pipAd.load(adsKey,
                new AdRequest.Builder().build(),
                new PictureInPictureAdLoadCallback() {
                    @Override
                    public void onAdLoaded() {
                        Log.d(TAG, "Picture In Picture Ad loaded.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.d(TAG, "Picture In Picture Failed to load: " +
                                loadAdError.getMessage());
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
                    }

                    @Override
                    public void onAdPaid(AdValue value) {
                        Log.d(TAG, "Picture In Picture Ad Paid with: " +
                                value.getValueMicros() + " " + value.getCurrencyCode());
                    }
                });
    }

    public void show() {
        pipAd.show(currentActivity, PictureInPictureAd.AdPosition.BOTTOM_RIGHT);
    }

    public void loadAndShow(){
        pipAd.load(adsKey,
                new AdRequest.Builder().build(),
                new PictureInPictureAdLoadCallback() {
                    @Override
                    public void onAdLoaded() {
                        Log.d(TAG, "Picture In Picture Ad loaded.");
                        pipAd.show(currentActivity, PictureInPictureAd.AdPosition.BOTTOM_RIGHT);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.d(TAG, "Picture In Picture Failed to load: " +
                                loadAdError.getMessage());
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
                    }

                    @Override
                    public void onAdPaid(AdValue value) {
                        Log.d(TAG, "Picture In Picture Ad Paid with: " +
                                value.getValueMicros() + " " + value.getCurrencyCode());
                    }
                });
    }

}
