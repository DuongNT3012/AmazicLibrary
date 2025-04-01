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

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.ads.mediation.sample.customevent.util.FirebaseAnalyticsUtil;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.admanager.AdManagerAdView;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;

/**
 * Banner custom event loader for the SampleSDK.
 */
public class BannerCustomEventLoader {

    /**
     * View to contain the sample banner ad.
     */
    private AdManagerAdView adView;

    /**
     * Configuration for requesting the banner ad from the third party network.
     */
    private final MediationBannerAdConfiguration mediationBannerAdConfiguration;

    /**
     * Callback that fires on loading success or failure.
     */
    private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
            mediationAdLoadCallback;

    /**
     * Callback for banner ad events.
     */
    private MediationBannerAdCallback bannerAdCallback;

    /**
     * Tag used for log statements
     */
    private static final String TAG = "BannerCustomEvent";

    public BannerCustomEventLoader(
            @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
            @NonNull
            MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
                    mediationAdLoadCallback) {
        this.mediationBannerAdConfiguration = mediationBannerAdConfiguration;
        this.mediationAdLoadCallback = mediationAdLoadCallback;
    }

    /**
     * Loads a banner ad from the third party ad network.
     */
    public void loadAd() {
        // All custom events have a server parameter named "parameter" that returns back the parameter
        // entered into the AdMob UI when defining the custom event.
        Log.i(TAG, "Begin loading banner ad.");
        String serverParameter = mediationBannerAdConfiguration.getServerParameters().getString("parameter");
        if (TextUtils.isEmpty(serverParameter)) {
            mediationAdLoadCallback.onFailure(CustomEventError.createCustomEventNoAdIdError());
            return;
        }
        Log.d(TAG, "Received server parameter.");

        Context context = mediationBannerAdConfiguration.getContext();
        //log event to firebase
        FirebaseAnalyticsUtil.logEventMediationAdx(context, FirebaseAnalyticsUtil.BANNER);

        adView = new AdManagerAdView(context);
        adView.setAdUnitId(serverParameter);
        adView.setAdSize(mediationBannerAdConfiguration.getAdSize());
        adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                bannerAdCallback = mediationAdLoadCallback.onSuccess(new MediationBannerAd() {
                    @NonNull
                    @Override
                    public View getView() {
                        return adView;
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                mediationAdLoadCallback.onFailure(new AdError(loadAdError.getCode(), loadAdError.getMessage(), loadAdError.getDomain()));
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                if (bannerAdCallback != null)
                    bannerAdCallback.reportAdImpression();
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (bannerAdCallback != null)
                    bannerAdCallback.reportAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                if (bannerAdCallback != null)
                    bannerAdCallback.onAdClosed();
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                if (bannerAdCallback != null)
                    bannerAdCallback.onAdOpened();
            }
        });
        adView.loadAd(new AdManagerAdRequest.Builder().build());
    }
}
