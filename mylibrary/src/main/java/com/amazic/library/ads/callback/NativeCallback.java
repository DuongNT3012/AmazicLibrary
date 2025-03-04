package com.amazic.library.ads.callback;

import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

public class NativeCallback {
    public void onNativeAdLoaded(NativeAd nativeAd){}
    public void onAdFailedToLoad(){}
    public void onAdImpression(){}
    public void onAdClicked(){}
    public void onAdShown(NativeAdView adView){}
}
