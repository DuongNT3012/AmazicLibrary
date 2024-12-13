package com.diamondguide.redeemcode.ffftips;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amazic.library.Utils.RemoteConfigHelper;
import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.ads.banner_ads.BannerBuilder;
import com.amazic.library.ads.banner_ads.BannerManager;
import com.amazic.library.ads.callback.AppOpenCallback;
import com.amazic.library.ads.callback.BannerCallback;
import com.amazic.library.ads.callback.InterCallback;
import com.amazic.library.ads.callback.RewardedCallback;
import com.amazic.library.ads.callback.RewardedInterCallback;
import com.amazic.library.ads.inter_ads.InterManager;
import com.amazic.library.ads.native_ads.NativeBuilder;
import com.amazic.library.ads.native_ads.NativeManager;
import com.amazic.library.ads.reward_ads.RewardManager;
import com.amazic.library.ads.reward_inter_ads.RewardInterManager;
import com.amazic.library.organic.TechManager;
import com.diamondguide.redeemcode.ffftips.databinding.ActivityMainBinding;
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

        Log.d("MainActivity", "onCreate.");

        Log.d("MainActivityRemote", "banner_splash: " + RemoteConfigHelper.getInstance().get_config(this, RemoteConfigHelper.banner_splash));
        Log.d("MainActivityRemote", "inter_splash: " + RemoteConfigHelper.getInstance().get_config(this, RemoteConfigHelper.inter_splash));
        Log.d("MainActivityRemote", "open_splash: " + RemoteConfigHelper.getInstance().get_config(this, RemoteConfigHelper.open_splash));
        Log.d("MainActivityRemote", "rate: " + RemoteConfigHelper.getInstance().get_config_string(this, RemoteConfigHelper.rate_aoa_inter_splash));
        Log.d("MainActivityRemote", "interval start: " + RemoteConfigHelper.getInstance().get_config_long(this, RemoteConfigHelper.interval_interstitial_from_start));
        Log.d("MainActivityRemote", "interval reloadNative: " + RemoteConfigHelper.getInstance().get_config_long(this, RemoteConfigHelper.interval_reload_native));

        BannerBuilder bannerBuilder = new BannerBuilder().isIdApi();
        bannerBuilder.setCallBack(new BannerCallback(){
            @Override
            public void onAdImpression() {
                super.onAdImpression();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("MainActivity", "run: " + TechManager.getInstance().isTech(MainActivity.this));
                    }
                }, 3000);
            }
        });
        BannerManager bannerManager = new BannerManager(this, binding.adViewContainer, this, bannerBuilder, "banner_all");
        //bannerManager.setAlwaysReloadOnResume(true);
        //bannerManager.setIntervalReloadBanner(5000L);

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
        NativeManager nativeManager = new NativeManager(this, this, nativeBuilder, "native_all");
        //nativeManager.setAlwaysReloadOnResume(true);
        //nativeManager.setIntervalReloadNative(3000L);

        InterManager.loadInterAds(this, "inter_all");
        binding.tvShowInter.setOnClickListener(view -> {
            InterManager.showInterAds(this, "inter_all", new InterCallback(){
                @Override
                public void onNextAction() {
                    super.onNextAction();
                    Toast.makeText(MainActivity.this, "On next action.", Toast.LENGTH_SHORT).show();
                }
            }, true);
        });
        RewardManager.loadRewardAds(this, "rewarded");
        binding.tvShowReward.setOnClickListener(view -> {
            RewardManager.showRewardAds(this,"rewarded",new RewardedCallback(){
                @Override
                public void onNextAction() {
                    super.onNextAction();
                    Toast.makeText(MainActivity.this, "Show Reward Ads On next action.", Toast.LENGTH_SHORT).show();
                }
            }, true);
        });
        RewardInterManager.loadRewardInterAds(this, "rewarded_inter");
        binding.tvShowRewardInter.setOnClickListener(view -> {
            RewardInterManager.showRewardInterAds(this, "rewarded_inter", new RewardedInterCallback(){
                @Override
                public void onNextAction() {
                    super.onNextAction();
                    Toast.makeText(MainActivity.this, "Show Reward Inter Ads On next action.", Toast.LENGTH_SHORT).show();
                }
            }, true);
        });
        binding.tvShowOpenResume.setOnClickListener(view -> {
            AppOpenManager.getInstance().loadAndShowAppOpenResumeSplash(MainActivity.this, AdmobApi.getInstance().getListIDAppOpenResume(), new AppOpenCallback() {
                @Override
                public void onNextAction() {
                    super.onNextAction();
                    Toast.makeText(MainActivity.this, "On next action open resume.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}