/*
 * Copyright (C) 2015 Google, Inc.
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
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ads.mediation.sample.customevent.util.FirebaseAnalyticsUtil;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.nativead.NativeAdOptions;

/**
 * Native custom event loader for the SampleSDK.
 */
public class NativeCustomEventLoader {

    /**
     * Configuration for requesting the native ad from the third party network.
     */
    private final MediationNativeAdConfiguration mediationNativeAdConfiguration;

    /**
     * Callback that fires on loading success or failure.
     */
    private final MediationAdLoadCallback<com.google.android.gms.ads.mediation.NativeAdMapper, MediationNativeAdCallback>
            mediationAdLoadCallback;

    /**
     * Tag used for log statements
     */
    private static final String TAG = "NativeCustomEvent";

    /**
     * Callback for native ad events. The usual link/click tracking handled through callback methods
     * are handled through the GMA SDK, described here:
     * https://developers.google.com/admob/android/custom-events/native#impression_and_click_events
     */
    @SuppressWarnings("unused")
    private MediationNativeAdCallback mediationNativeAdCallback;

    public NativeCustomEventLoader(
            @NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
            @NonNull MediationAdLoadCallback<com.google.android.gms.ads.mediation.NativeAdMapper, MediationNativeAdCallback>
                    mediationAdLoadCallback) {
        this.mediationNativeAdConfiguration = mediationNativeAdConfiguration;
        this.mediationAdLoadCallback = mediationAdLoadCallback;
    }

    /**
     * Loads the native ad from the third party ad network.
     */
    public void loadAd() {
        Context context = mediationNativeAdConfiguration.getContext();
        // All custom events have a server parameter named "parameter" that returns back the parameter
        // entered into the AdMob UI when defining the custom event.
        String serverParameter = mediationNativeAdConfiguration.getServerParameters().getString("parameter");
        //log event to firebase
        FirebaseAnalyticsUtil.logEventMediationAdx(context, FirebaseAnalyticsUtil.NATIVE + "_" + serverParameter);
        Log.d(TAG, "Received server parameter: " + serverParameter);
        if (TextUtils.isEmpty(serverParameter)) {
            FirebaseAnalyticsUtil.logEventMediationAdx(context, "id_empty", new Bundle());
            Log.d(TAG, "Id empty");
            mediationAdLoadCallback.onFailure(CustomEventError.createCustomEventNoAdIdError());
            return;
        }
        Log.d(TAG, "Start load native ad: " + serverParameter);

        AdLoader.Builder builder = new AdLoader.Builder(context, serverParameter);
        // OnLoadedListener implementation.
        builder.forNativeAd(nativeAd -> {
            Log.d(TAG, "onAdLoad success headline: " + nativeAd.getHeadline());
            NativeAdMapper mappedAd = new NativeAdMapper(nativeAd, context);
            mediationNativeAdCallback = mediationAdLoadCallback.onSuccess(mappedAd);
            mappedAd.setMediationNativeAdCallback(mediationNativeAdCallback);
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = mediationNativeAdConfiguration.getNativeAdOptions();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.d(TAG, "onAdFailedToLoad." + loadAdError.getMessage());
                mediationAdLoadCallback.onFailure(new AdError(loadAdError.getCode(), loadAdError.getMessage(), loadAdError.getDomain()));
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                if (mediationNativeAdCallback != null)
                    mediationNativeAdCallback.reportAdImpression();
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (mediationNativeAdCallback != null)
                    mediationNativeAdCallback.reportAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                if (mediationNativeAdCallback != null)
                    mediationNativeAdCallback.onAdClosed();
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                if (mediationNativeAdCallback != null)
                    mediationNativeAdCallback.onAdOpened();
            }
        }).build();
        adLoader.loadAd(new AdManagerAdRequest.Builder().build());
    }
}
