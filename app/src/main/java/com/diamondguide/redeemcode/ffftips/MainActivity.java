package com.diamondguide.redeemcode.ffftips;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amazic.library.Utils.RemoteConfigHelper;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.ads.banner_ads.BannerBuilder;
import com.amazic.library.ads.banner_ads.BannerManager;
import com.amazic.library.ads.callback.AppOpenCallback;
import com.amazic.library.ads.callback.BannerCallback;
import com.amazic.library.ads.callback.InterCallback;
import com.amazic.library.ads.callback.RewardedCallback;
import com.amazic.library.ads.callback.RewardedInterCallback;
import com.amazic.library.ads.collapse_banner_ads.CollapseBannerBuilder;
import com.amazic.library.ads.collapse_banner_ads.CollapseBannerManager;
import com.amazic.library.ads.inter_ads.InterManager;
import com.amazic.library.ads.native_ads.NativeBuilder;
import com.amazic.library.ads.native_ads.NativeManager;
import com.amazic.library.ads.reward_ads.RewardManager;
import com.amazic.library.ads.reward_inter_ads.RewardInterManager;
import com.amazic.library.organic.TechManager;
import com.amazic.library.update_app.UpdateApplicationManager;
import com.diamondguide.redeemcode.ffftips.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private boolean earnedReward = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d("MainActivity", "onCreate.");
        Log.d("MainActivity", "TechManager: " + TechManager.getInstance().isTech(this));

        Log.d("MainActivityRemote", "banner_splash: " + RemoteConfigHelper.getInstance().get_config(this, RemoteConfigHelper.banner_splash));
        Log.d("MainActivityRemote", "inter_splash: " + RemoteConfigHelper.getInstance().get_config(this, RemoteConfigHelper.inter_splash));
        Log.d("MainActivityRemote", "open_splash: " + RemoteConfigHelper.getInstance().get_config(this, RemoteConfigHelper.open_splash));
        Log.d("MainActivityRemote", "rate: " + RemoteConfigHelper.getInstance().get_config_string(this, RemoteConfigHelper.rate_aoa_inter_splash));
        Log.d("MainActivityRemote", "interval start: " + RemoteConfigHelper.getInstance().get_config_long(this, RemoteConfigHelper.interval_interstitial_from_start));
        Log.d("MainActivityRemote", "interval reloadNative: " + RemoteConfigHelper.getInstance().get_config_long(this, RemoteConfigHelper.interval_reload_native));
        AppOpenManager.getInstance().isShowAdResumeAfterAdClick = false;
        //bannerManager.setAlwaysReloadOnResume(true);
        //bannerManager.setIntervalReloadBanner(5000L);

        CollapseBannerBuilder collapseBannerBuilder = new CollapseBannerBuilder().isIdApi();
        CollapseBannerManager collapseBannerManager = new CollapseBannerManager(this, binding.adViewContainer, this, collapseBannerBuilder,"fdkfjkd");
        collapseBannerManager.setAlwaysReloadOnResume(true);

        NativeBuilder nativeBuilder = new NativeBuilder(
                this, binding.frAdsNative,
                com.amazic.mylibrary.R.layout.layout_shimmer_native,
                com.amazic.mylibrary.R.layout.layout_native_adview,
                com.amazic.mylibrary.R.layout.layout_native_adview);
        nativeBuilder.setListIdAd(AdmobApi.getInstance().getListIDNativeAll());
        nativeBuilder.maxRequest = 3;
        NativeManager nativeManager = new NativeManager(this, this, nativeBuilder, "native_all");
        nativeManager.setIntervalReloadNative(2000);
        //nativeManager.setAlwaysReloadOnResume(true);
        //nativeManager.setIntervalReloadNative(3000L);

        //InterManager.loadInterAds(this, "inter_all");
        binding.tvShowInter.setOnClickListener(view -> {
            InterManager.loadAndShowInterAds(this, "inter_all", new InterCallback() {
                @Override
                public void onNextAction() {
                    super.onNextAction();
                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
        });
        RewardManager.loadRewardAds(this, "rewarded");
        binding.tvShowReward.setOnClickListener(view -> {
            RewardManager.showRewardAds(this, "rewarded", new RewardedCallback() {
                @Override
                public void onNextAction() {
                    super.onNextAction();
                    Toast.makeText(MainActivity.this, "Show Reward Ads On next action.", Toast.LENGTH_SHORT).show();
                }
            }, true);
        });
        RewardInterManager.loadRewardInterAds(this, "rewarded_inter");
        binding.tvShowRewardInter.setOnClickListener(view -> {
            RewardInterManager.showRewardInterAds(this, "rewarded_inter", new RewardedInterCallback() {
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
        UpdateApplicationManager.checkVersionPlayStore(this, false, true, true);
    }
}