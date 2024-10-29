package com.amazic.sample;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.ads.callback.AppOpenCallback;
import com.amazic.sample.databinding.ActivityWelcomeBackBinding;

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
