package com.amazic.sample;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.callback.BannerCallback;
import com.amazic.library.ads.callback.InterCallback;
import com.amazic.library.ads.callback.NativeCallback;
import com.amazic.library.ads.callback.RewardedCallback;
import com.amazic.library.ads.callback.RewardedInterCallback;
import com.amazic.sample.databinding.ActivityMainBinding;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Admob.getInstance().loadBannerAds(this, binding.adViewContainer, new BannerCallback() {

        });

        Admob.getInstance().loadNativeAds(this, new NativeCallback() {
            @Override
            public void onNativeAdLoaded(NativeAd nativeAd) {
                super.onNativeAdLoaded(nativeAd);
                NativeAdView adView = (NativeAdView) getLayoutInflater().inflate(com.amazic.mylibrary.R.layout.layout_native_adview, binding.frAdsNative, false);
                Admob.getInstance().populateNativeAdView(nativeAd, adView);
                binding.frAdsNative.removeAllViews();
                binding.frAdsNative.addView(adView);
            }
        });

        binding.tvShowInter.setOnClickListener(view -> {
            Admob.getInstance().loadInterAds(this, new InterCallback() {
                @Override
                public void onAdLoaded(InterstitialAd interstitialAd) {
                    super.onAdLoaded(interstitialAd);
                    Admob.getInstance().showInterAds(MainActivity.this, interstitialAd, new InterCallback() {
                    });
                }
            });
        });
        binding.tvShowReward.setOnClickListener(view -> {
            Admob.getInstance().loadRewardAds(this, new RewardedCallback() {
            });
        });
        binding.tvShowRewardInter.setOnClickListener(view -> {
            Admob.getInstance().loadRewardInterAds(this, new RewardedInterCallback() {
            });
        });
    }
}