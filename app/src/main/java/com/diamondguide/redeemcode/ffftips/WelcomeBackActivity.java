package com.diamondguide.redeemcode.ffftips;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.diamondguide.redeemcode.ffftips.databinding.ActivityWelcomeBackBinding;

public class WelcomeBackActivity extends AppCompatActivity {
    private ActivityWelcomeBackBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWelcomeBackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //AppOpenManager.getInstance().loadAd(this, AdmobApi.getInstance().getListIDAppOpenResume());

        binding.tvWelcomeBack.setOnClickListener(view -> {
//            AppOpenManager.getInstance().showAdIfAvailableWelcomeBack(this, AdmobApi.getInstance().getListIDAppOpenResume(), new AppOpenCallback(){
//                @Override
//                public void onAdDismissedFullScreenContent() {
//                    super.onAdDismissedFullScreenContent();
//                    finish();
//                }
//            });
            finish();
        });
    }
}
