package com.amazic.library.ads.callback;

import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView;

public class NativeCallback {
    public void onNativeAdLoaded(NativeAd nativeAd) {
    }

    public void onAdFailedToLoad(LoadAdError loadAdError) {
    }

    public void onAdImpression() {
    }

    public void onAdClicked() {
    }

    public void onAdShown(NativeAdView adView) {
    }
}
