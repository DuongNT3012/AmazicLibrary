package com.amazic.sample;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.banner_ads.BannerBuilder;
import com.amazic.library.ads.banner_ads.BannerManager;
import com.amazic.library.ads.callback.InterCallback;
import com.amazic.library.ads.callback.RewardedCallback;
import com.amazic.library.ads.callback.RewardedInterCallback;
import com.amazic.library.ads.native_ads.NativeBuilder;
import com.amazic.library.ads.native_ads.NativeManager;
import com.amazic.sample.databinding.ActivityMainBinding;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private boolean earnedReward = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BannerBuilder bannerBuilder = new BannerBuilder().isIdApi();
        BannerManager bannerManager = new BannerManager(this, binding.adViewContainer, this, bannerBuilder);
        bannerManager.setAlwaysReloadOnResume(true);
        bannerManager.setIntervalReloadBanner(5000L);

        /*CollapseBannerBuilder collapseBannerBuilder = new CollapseBannerBuilder().isIdApi();
        CollapseBannerManager collapseBannerManager = new CollapseBannerManager(this, binding.adViewContainer, this, collapseBannerBuilder);
        collapseBannerManager.setAlwaysReloadOnResume(true);
        collapseBannerManager.setIntervalReloadBanner(5000L);*/

        NativeBuilder nativeBuilder = new NativeBuilder(
                this, binding.frAdsNative,
                com.amazic.mylibrary.R.layout.layout_shimmer_native,
                com.amazic.mylibrary.R.layout.layout_native_adview,
                com.amazic.mylibrary.R.layout.layout_native_adview);
        nativeBuilder.setListIdAd(AdmobApi.getInstance().getListIDNativeAll());
        NativeManager nativeManager = new NativeManager(this, this, nativeBuilder);
        nativeManager.setAlwaysReloadOnResume(true);
        nativeManager.setIntervalReloadNative(5000L);

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
            Admob.getInstance().loadRewardInterAds(this, AdmobApi.getInstance().getListIDByName("rewarded_inter"), new RewardedInterCallback() {
                @Override
                public void onAdLoaded(RewardedInterstitialAd ad) {
                    super.onAdLoaded(ad);
                    Admob.getInstance().showRewardInterAds(MainActivity.this, ad, new RewardedInterCallback() {

                    });
                }
            });
        });
    }
}