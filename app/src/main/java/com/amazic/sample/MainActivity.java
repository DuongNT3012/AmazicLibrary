package com.amazic.sample;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.callback.BannerCallback;
import com.amazic.library.ads.callback.InterCallback;
import com.amazic.library.ads.callback.NativeCallback;
import com.amazic.library.ads.callback.RewardedCallback;
import com.amazic.library.ads.callback.RewardedInterCallback;
import com.amazic.sample.databinding.ActivityMainBinding;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.rewarded.RewardedAd;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private boolean earnedReward = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        /*Admob.getInstance().loadBannerAds(this, AdmobApi.getInstance().getListIDBannerAll(), binding.adViewContainer, new BannerCallback() {

        });*/
        Admob.getInstance().loadCollapseBanner(
                this,
                AdmobApi.getInstance().getListIDCollapseBannerAll(),
                binding.adViewContainer,
                true,
                new BannerCallback()
        );

        Admob.getInstance().loadNativeAds(
                this,
                AdmobApi.getInstance().getListIDNativeAll(),
                binding.frAdsNative,
                com.amazic.mylibrary.R.layout.layout_native_adview,
                com.amazic.mylibrary.R.layout.layout_shimmer_native,
                true,
                new NativeCallback()
        );

        binding.tvShowInter.setOnClickListener(view -> {
            Admob.getInstance().loadInterAds(this, AdmobApi.getInstance().getListIDInterAll(), new InterCallback() {
                @Override
                public void onAdLoaded(InterstitialAd interstitialAd) {
                    super.onAdLoaded(interstitialAd);
                    Admob.getInstance().showInterAds(MainActivity.this, interstitialAd, new InterCallback() {
                    });
                }
            });
        });
        binding.tvShowReward.setOnClickListener(view -> {
            Admob.getInstance().loadRewardAds(this, AdmobApi.getInstance().getListIDByName("rewarded"), new RewardedCallback() {
                @Override
                public void onUserEarnedReward() {
                    super.onUserEarnedReward();
                    earnedReward = true;
                }

                @Override
                public void onAdLoaded(RewardedAd ad) {
                    super.onAdLoaded(ad);
                    Admob.getInstance().showReward(MainActivity.this, ad, new RewardedCallback() {

                    });
                }
            });
        });
        binding.tvShowRewardInter.setOnClickListener(view -> {
            Admob.getInstance().loadRewardInterAds(this, AdmobApi.getInstance().getListIDByName("rewarded"), new RewardedInterCallback() {

            });
        });
    }
}