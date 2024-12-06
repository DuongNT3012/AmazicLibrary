package com.diamondguide.redeemcode.ffftips;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.ads.callback.AppOpenCallback;
import com.diamondguide.redeemcode.ffftips.databinding.ActivityWelcomeBackBinding;
import com.google.android.gms.ads.appopen.AppOpenAd;

public class WelcomeBackActivity extends AppCompatActivity {
    private ActivityWelcomeBackBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWelcomeBackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppOpenManager.getInstance().loadAd(this, AdmobApi.getInstance().getListIDAppOpenResume(), new AppOpenCallback(){
            @Override
            public void onAdLoaded(AppOpenAd ad) {
                super.onAdLoaded(ad);
                Toast.makeText(WelcomeBackActivity.this, "onAdLoaded", Toast.LENGTH_SHORT).show();
            }
        }, "open_resume");

        binding.tvWelcomeBack.setOnClickListener(view -> {
            AppOpenManager.getInstance().showAdIfAvailableWelcomeBack(this, AdmobApi.getInstance().getListIDAppOpenResume(), new AppOpenCallback(){
                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    finish();
                }
            }, "open_resume");
            finish();
        });
    }
}
