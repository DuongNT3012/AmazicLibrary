/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ads.mediation.sample.customevent;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ads.mediation.sample.customevent.util.FirebaseAnalyticsUtil;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback;
import com.google.android.gms.ads.formats.AdManagerAdViewOptions;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

/**
 * Interstitial custom event loader for the SampleSDK.
 */
public class InterstitialCustomEventLoader {

    /**
     * A sample third party SDK interstitial ad.
     */
    //private AdManagerInterstitialAd mInterstitialAd;

    /**
     * Configuration for requesting the interstitial ad from the third party network.
     */
    private final MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration;

    /**
     * Callback that fires on loading success or failure.
     */
    private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
            mediationAdLoadCallback;

    /**
     * Callback for interstitial ad events.
     */
    private MediationInterstitialAdCallback interstitialAdCallback;

    /**
     * Tag used for log statements
     */
    private static final String TAG = "InterstitialCustomEvent";

    public InterstitialCustomEventLoader(
            MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
            MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
                    mediationAdLoadCallback) {
        this.mediationInterstitialAdConfiguration = mediationInterstitialAdConfiguration;
        this.mediationAdLoadCallback = mediationAdLoadCallback;
    }

    /**
     * Loads the interstitial ad from the third party ad network.
     */
    public void loadAd() {
        // All custom events have a server parameter named "parameter" that returns back the parameter
        // entered into the AdMob UI when defining the custom event.
        Log.i(TAG, "Begin loading interstitial ad.");
        String serverParameter = mediationInterstitialAdConfiguration.getServerParameters().getString("parameter");
        if (TextUtils.isEmpty(serverParameter)) {
            mediationAdLoadCallback.onFailure(CustomEventError.createCustomEventNoAdIdError());
            return;
        }
        Log.d(TAG, "Received server parameter.");

        Context context = mediationInterstitialAdConfiguration.getContext();

        //log event to firebase
        FirebaseAnalyticsUtil.logEventMediationAdx(context, FirebaseAnalyticsUtil.INTER);

        AdManagerAdRequest adRequest = new AdManagerAdRequest.Builder().build();
        AdManagerInterstitialAd.load(context, serverParameter, adRequest, new AdManagerInterstitialAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mediationAdLoadCallback.onFailure(new AdError(loadAdError.getCode(), loadAdError.getMessage(), loadAdError.getDomain()));
            }

            @Override
            public void onAdLoaded(@NonNull AdManagerInterstitialAd interstitialAd) {
                interstitialAdCallback = mediationAdLoadCallback.onSuccess(new MediationInterstitialAd() {
                    @Override
                    public void showAd(@NonNull Context context) {
                        interstitialAd.show((Activity) context);
                    }
                });
                interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        super.onAdFailedToShowFullScreenContent(adError);
                        if (interstitialAdCallback != null)
                            interstitialAdCallback.onAdFailedToShow(adError);
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        if (interstitialAdCallback != null)
                            interstitialAdCallback.reportAdImpression();
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent();
                        if (interstitialAdCallback != null)
                            interstitialAdCallback.onAdClosed();
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (interstitialAdCallback != null)
                            interstitialAdCallback.reportAdClicked();
                    }
                });
            }
        });
    }
}
