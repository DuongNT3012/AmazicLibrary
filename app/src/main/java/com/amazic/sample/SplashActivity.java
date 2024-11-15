package com.amazic.sample;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.ads.callback.ApiCallback;
import com.amazic.library.ads.callback.AppOpenCallback;
import com.amazic.library.ads.callback.InterCallback;
import com.amazic.library.ads.splash_ads.AdsSplash;
import com.amazic.library.organic.TechManager;
import com.amazic.sample.databinding.ActivitySplashBinding;
import com.google.android.gms.ads.interstitial.InterstitialAd;

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;
    private InterCallback interCallback;
    private AppOpenCallback appOpenCallback;
    private AdsSplash adsSplash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        interCallback = new InterCallback() {
            @Override
            public void onNextAction() {
                super.onNextAction();
                startNextAct();
            }
        };

        appOpenCallback = new AppOpenCallback() {
            @Override
            public void onNextAction() {
                super.onNextAction();
                startNextAct();
            }
        };

        Admob.getInstance().setOpenActivityAfterShowInterAds(true);
        //Admob.getInstance().setDetectTestAdByView(true);
        Admob.getInstance().initAdmob(this, () -> {
            AdmobApi.getInstance().setTimeOutCallApi(12000);
            AdmobApi.getInstance().setJsonIdAdsDefault("");
            AdmobApi.getInstance().init(getApplicationContext(), "", getString(R.string.app_id), new ApiCallback() {
                @Override
                public void onReady() {
                    super.onReady();
                    //AppOpenManager.getInstance().initWelcomeBackBelowAdsResume(AdmobApi.getInstance().getListIDAppOpenResume(), WelcomeBackActivity.class);
                    //AppOpenManager.getInstance().initWelcomeBackAboveAdsResume(SplashActivity.this, AdmobApi.getInstance().getListIDAppOpenResume(), WelcomeBackActivity.class);
                    AppOpenManager.getInstance().init(SplashActivity.this, AdmobApi.getInstance().getListIDAppOpenResume());
                    AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity.class);
                    AppOpenManager.getInstance().disableAppResumeWithActivity(WelcomeBackActivity.class);
                    //Admob.getInstance().setTimeInterval(5000);
                    //Admob.getInstance().setTimeIntervalFromStart(20000);
                    adsSplash = AdsSplash.init(true, true, "0_100");
                    adsSplash.showAdsSplashApi(SplashActivity.this, appOpenCallback, interCallback);
                }
            });
        });
    }

    private void startNextAct(){
        Log.d("SplashActivity", "startNextAct.");
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adsSplash != null) {
            adsSplash.onCheckShowSplashWhenFail(this, appOpenCallback, interCallback);
        }
    }
}
