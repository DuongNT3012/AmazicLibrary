package com.diamondguide.redeemcode.ffftips;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.ads.banner_ads.BannerBuilder;
import com.amazic.library.ads.banner_ads.BannerManager;
import com.amazic.library.ads.callback.AppOpenCallback;
import com.amazic.library.ads.callback.InterCallback;
import com.amazic.library.ads.callback.RewardedCallback;
import com.amazic.library.ads.callback.RewardedInterCallback;
import com.amazic.library.ads.inter_ads.InterManager;
import com.amazic.library.ads.native_ads.NativeBuilder;
import com.amazic.library.ads.native_ads.NativeManager;
import com.amazic.library.ads.reward_ads.RewardManager;
import com.amazic.library.ads.reward_inter_ads.RewardInterManager;
import com.diamondguide.redeemcode.ffftips.databinding.ActivityMainBinding;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private boolean earnedReward = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        NativeBuilder nativeBuilder = new NativeBuilder(
                this, binding.frAdsNative,
                com.amazic.mylibrary.R.layout.layout_shimmer_native,
                com.amazic.mylibrary.R.layout.layout_native_adview,
                com.amazic.mylibrary.R.layout.layout_native_adview,
                false);
        nativeBuilder.maxRequest = 10;
        nativeBuilder.maxRequestReload = 10;
        nativeBuilder.setListIdAdMain(AdmobApi.getInstance().getListIDByName("native_wb"));
        nativeBuilder.setListIdAdSecondary(AdmobApi.getInstance().getListIDByName("native_wb"));
        nativeBuilder.setListIdAdBackup(List.of("ca-app-pub-3940256099942544/1044960115"));
        NativeManager nativeManager = new NativeManager(this, this, nativeBuilder, "native_wb");
        nativeManager.setIntervalReloadNative(4000);
        nativeManager.setAlwaysReloadOnResume(true);
        /*BannerBuilder bannerBuilder = new BannerBuilder(this, binding.frAdsNative, true);
        bannerBuilder.setListIdAdMain(AdmobApi.getInstance().getListIDByName("banner_all"));
        bannerBuilder.setListIdAdSecondary(AdmobApi.getInstance().getListIDByName("banner_all"));
        bannerBuilder.setListIdAdBackup(AdmobApi.getInstance().getListIDByName("banner_all"));
        BannerManager bannerManager = new BannerManager(this, this, bannerBuilder, "banner_all");
        bannerManager.setIntervalReloadBanner(4000);
        bannerManager.setAlwaysReloadOnResume(true);*/

        //InterManager.loadInterAds(this, "inter_all");
        binding.tvShowInter.setOnClickListener(view -> {
            InterManager.loadAndShowInterAds(this, "inter_all", "inter_all", new InterCallback() {
                @Override
                public void onNextAction() {
                    super.onNextAction();
                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
        });
        RewardManager.loadRewardAds(this, "rewarded", "rewarded");
        binding.tvShowReward.setOnClickListener(view -> {
            RewardManager.showRewardAds(this, "rewarded", "rewarded", new RewardedCallback() {
                @Override
                public void onNextAction() {
                    super.onNextAction();
                    Toast.makeText(MainActivity.this, "Show Reward Ads On next action.", Toast.LENGTH_SHORT).show();
                    RewardManager.showRewardAds(MainActivity.this, "rewarded", "rewarded", new RewardedCallback() {
                        @Override
                        public void onNextAction() {
                            super.onNextAction();
                            Toast.makeText(MainActivity.this, "Show Reward Ads On next action.", Toast.LENGTH_SHORT).show();
                        }
                    }, true);
                }
            }, true);
        });
        RewardInterManager.loadRewardInterAds(this, "rewarded_inter", "rewarded_inter");
        binding.tvShowRewardInter.setOnClickListener(view -> {
            RewardInterManager.showRewardInterAds(this, "rewarded_inter", "rewarded_inter", new RewardedInterCallback() {
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