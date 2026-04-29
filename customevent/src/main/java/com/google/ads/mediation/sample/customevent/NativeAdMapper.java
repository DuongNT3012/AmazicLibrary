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
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.ads.mediation.sample.sdk.SampleNativeAd;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;

import java.text.NumberFormat;
import java.util.Map;

/**
 * A {@link com.google.android.gms.ads.mediation.NativeAdMapper} extension to map {@link SampleNativeAd} instances to the Mobile
 * Ads SDK's {@link com.google.android.gms.ads.nativead.NativeAd} interface.
 */
public class NativeAdMapper extends com.google.android.gms.ads.mediation.NativeAdMapper {
    private final String TAG = "NativeAdMapper";
    private final NativeAd nativeAd;
    private MediationNativeAdCallback mediationNativeAdCallback;
    private MediaView mediaView;

    public void setMediationNativeAdCallback(MediationNativeAdCallback mediationNativeAdCallback) {
        this.mediationNativeAdCallback = mediationNativeAdCallback;
    }

    public NativeAdMapper(@NonNull NativeAd nativeAd, Context context) {
        this.nativeAd = nativeAd;
        if (nativeAd.getHeadline() != null)
            setHeadline(nativeAd.getHeadline());
        if (nativeAd.getBody() != null)
            setBody(nativeAd.getBody());
        if (nativeAd.getCallToAction() != null)
            setCallToAction(nativeAd.getCallToAction());
        if (nativeAd.getStarRating() != null)
            setStarRating(nativeAd.getStarRating());
        if (nativeAd.getStore() != null)
            setStore(nativeAd.getStore());
        if (nativeAd.getIcon() != null)
            setIcon(nativeAd.getIcon());
        if (nativeAd.getAdvertiser() != null)
            setAdvertiser(nativeAd.getAdvertiser());

        if (nativeAd.getMediaContent() != null) {
            if (nativeAd.getMediaContent().hasVideoContent()) {
                Log.d(TAG, "==================\nNativeAdMapper: video");
                MediaView mediaView = new MediaView(context);
                mediaView.setMediaContent(nativeAd.getMediaContent());
                setMediaView(mediaView);
                VideoController videoController = nativeAd.getMediaContent().getVideoController();
                videoController.setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
                    @Override
                    public void onVideoStart() {
                        // Optional: log or animate UI
                        Log.d(TAG, "onVideoStart: ");
                    }

                    @Override
                    public void onVideoEnd() {
                        // Optional: UI handling after video ends
                        Log.d(TAG, "onVideoEnd: ");
                    }
                });
            } else {
                Log.d(TAG, "==================\nNativeAdMapper: image");
                setImages(nativeAd.getImages());
            }
        }
        if (nativeAd.getPrice() != null) {
            try {
                double price = Double.parseDouble(nativeAd.getPrice());
                NumberFormat formatter = NumberFormat.getCurrencyInstance();
                String priceString = formatter.format(price);
                setPrice(priceString);
            } catch (Exception e) {
                try {
                    setPrice(nativeAd.getPrice());
                } catch (Exception exception) {
                }
            }
        }

        setOverrideClickHandling(false);
        setOverrideImpressionRecording(false);

        try {
            if (nativeAd.getAdChoicesInfo() != null) {
                NativeAd.Image adChoicesImage = nativeAd.getAdChoicesInfo().getImages().get(0);
                ImageView adChoicesImageView = new ImageView(context);
                adChoicesImageView.setImageDrawable(adChoicesImage.getDrawable());
                setAdChoicesContent(adChoicesImageView);
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void recordImpression() {
        Log.d(TAG, "recordImpression.");
        if (mediationNativeAdCallback != null) {
            Log.d(TAG, "Reporting ad impression...");
            mediationNativeAdCallback.reportAdImpression();
        }
        super.recordImpression();
    }

    @Override
    public void handleClick(@NonNull View view) {
        Log.d(TAG, "handleClick.");
        if (mediationNativeAdCallback != null) {
            Log.d(TAG, "Reporting ad click...");
            mediationNativeAdCallback.reportAdClicked();
        }
        super.handleClick(view);
    }

    // The Sample SDK doesn't do its own impression/click tracking, instead relies on its
    // publishers calling the recordImpression and handleClick methods on its native ad object. So
    // there's no need to pass it a reference to the View being used to display the native ad. If
    // your mediated network does need a reference to the view, the following method can be used
    // to provide one.
    @Override
    public void trackViews(@NonNull View containerView,
                           @NonNull Map<String, View> clickableAssetViews,
                           @NonNull Map<String, View> nonClickableAssetViews) {
        super.trackViews(containerView, clickableAssetViews, nonClickableAssetViews);
        // If your ad network SDK does its own impression tracking, here is where you can track the
        // top level native ad view and its individual asset views.
        Log.d(TAG, "trackViews called " + clickableAssetViews.size() + "_" + nonClickableAssetViews.size());
        mediaView = containerView.findViewById(R.id.ad_media);
    }

    @Override
    public void untrackView(@NonNull View view) {
        super.untrackView(view);
        // Here you would remove any trackers from the View added in trackView.
    }
}
