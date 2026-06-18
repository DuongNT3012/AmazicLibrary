package com.diamondguide.redeemcode.ffftips;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
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
import com.amazic.library.ads.collapse_banner_ads.CollapseBannerBuilder;
import com.amazic.library.ads.collapse_banner_ads.CollapseBannerManager;
import com.amazic.library.ads.inter_ads.InterManager;
import com.amazic.library.ads.native_ads.NativeBuilder;
import com.amazic.library.ads.native_ads.NativeManager;
import com.amazic.library.ads.reward_ads.RewardManager;
import com.amazic.library.ads.reward_inter_ads.RewardInterManager;
import com.diamondguide.redeemcode.ffftips.databinding.ActivityMainBinding;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private int currentApiVersion;
    private ActivityMainBinding binding;
    private boolean earnedReward = false;

    @SuppressLint("NewApi")
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT && hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentApiVersion = android.os.Build.VERSION.SDK_INT;

        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(flags);
            final View decorView = getWindow().getDecorView();
            decorView
                    .setOnSystemUiVisibilityChangeListener(visibility -> {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            decorView.setSystemUiVisibility(flags);
                        }
                    });
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NativeBuilder nativeBuilder = new NativeBuilder(
                this, binding.frAdsNative,
                com.amazic.mylibrary.R.layout.layout_shimmer_native,
                R.layout.native_large_ads_with_button_above,
                com.amazic.mylibrary.R.layout.layout_native_adview,
                true);
        //nativeBuilder.maxRequest = 10;
        //nativeBuilder.maxRequestReload = 10;
        NativeManager nativeManager = new NativeManager(this, this, nativeBuilder, "native_wb", "native_wb", "ad_native_display_order");
        nativeManager.setIntervalReloadNative(5000);
        nativeManager.setAlwaysReloadOnResume(true);
        nativeBuilder.setListIdAdMain(List.of("ca-app-pub-3940256099942544/2247696110"));
        nativeBuilder.setListIdAdSecondary(List.of("ca-app-pub-3940256099942544/2247696110"));
        nativeBuilder.setListIdAdBackup(List.of("ca-app-pub-3940256099942544/1045960115"));
        new Handler().postDelayed(() -> {
            Log.d("NativeManager", "update id");
            nativeBuilder.setListIdAdMain(List.of("ca-app-pub-3940256099942544/2247696110"));
            nativeBuilder.setListIdAdSecondary(List.of("ca-app-pub-3940256099942544/2247696110"));
            nativeBuilder.setListIdAdBackup(List.of("ca-app-pub-3940256099942544/1044960115"));
        }, 20000);
        BannerBuilder bannerBuilder = new BannerBuilder(this, binding.adViewContainer, true);
        bannerBuilder.setListIdAdMain(AdmobApi.getInstance().getListIDByName("banner_all"));
        bannerBuilder.setListIdAdSecondary(AdmobApi.getInstance().getListIDByName("banner_all"));
        bannerBuilder.setListIdAdBackup(AdmobApi.getInstance().getListIDByName("banner_all"));
        BannerManager bannerManager = new BannerManager(this, this, bannerBuilder, "banner_all");
        bannerManager.setIntervalReloadBanner(4000);
        bannerManager.setAlwaysReloadOnResume(true);

        /*CollapseBannerBuilder collapseBannerBuilder = new CollapseBannerBuilder();
        collapseBannerBuilder.setListId(AdmobApi.getInstance().getListIDByName("collapse_banner"));
        CollapseBannerManager collapseBannerManager = new CollapseBannerManager(this, binding.adViewContainer, this, collapseBannerBuilder, "collapse_banner");
        collapseBannerManager.setIntervalReloadBanner(4000);
        collapseBannerManager.setAlwaysReloadOnResume(true);*/

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

        binding.tvShowInterNativeAfter.setOnClickListener(view -> {
            InterManager.loadAndShowInterAdsWithNativeAfterInter(
                    this,
                    "inter_all",
                    "inter_all",
                    new InterCallback() {
                        @Override
                        public void onNextAction() {
                            super.onNextAction();
                            Intent intent = new Intent(MainActivity.this, MainActivity2.class);
                            startActivity(intent);
//                            finish();
                        }
                    },
                    "native_after_inter",
                    "native_after_inter"
            );
        });

        binding.tvShowInterPreload.setOnClickListener(view -> {
//            InterManager.showInterAdPreload(
//                    this,
//                    "inter_all",
//                    "inter_all",
//                    new InterCallback() {
//                        @Override
//                        public void onNextAction() {
//                            super.onNextAction();
//                            Intent intent = new Intent(MainActivity.this, MainActivity2.class);
//                            startActivity(intent);
//                        }
//                    }, true
//            );
            InterManager.loadAndShowInterAdsPreload(
                    this,
                    "inter_all",
                    "inter_all",
                    new InterCallback(){
                        @Override
                        public void onNextAction() {
                            super.onNextAction();
                            Intent intent = new Intent(MainActivity.this, MainActivity2.class);
                            startActivity(intent);
                        }
                    }
            );
        });
        //RewardManager.loadRewardAds(this, "rewarded", "rewarded");
        binding.tvShowReward.setOnClickListener(view -> {
            /*RewardManager.showRewardAds(this, "rewarded", "rewarded", new RewardedCallback() {
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
            }, true);*/
            RewardManager.loadAndShowRewardAds(this, "rewarded", new RewardedCallback() {
                @Override
                public void onNextAction() {
                    super.onNextAction();
                    Toast.makeText(MainActivity.this, "Show Reward Ads On next action.", Toast.LENGTH_SHORT).show();
                }
            }, "rewarded");

//            RewardManager.loadAndShowRewardAdsPreload(
//                    this,
//                    "rewarded",
//                    "rewarded",
//                    new RewardedCallback() {
//                        @Override
//                        public void onNextAction() {
//                            super.onNextAction();
//
//                        }
//
//                        @Override
//                        public void onUserEarnedReward() {
//                            super.onUserEarnedReward();
//
//                        }
//                    }
//            );
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
            Log.d("AppOpenManager", "list: " + AdmobApi.getInstance().getListIDAppOpenResume());
            Log.d("AppOpenManager", "list intro: " + AdmobApi.getInstance().getListIDInterIntro());
            AppOpenManager.getInstance().loadAndShowAppOpenResumeSplash(MainActivity.this, AdmobApi.getInstance().getListIDByName("open_splash"), new AppOpenCallback() {
                @Override
                public void onNextAction() {
                    super.onNextAction();
                    Toast.makeText(MainActivity.this, "On next action open resume.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //AppOpenManager.getInstance().disableAppResumeWithActivity(getClass());
    }
}