package com.amazic.sample;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.app_open.AppOpenManager;
import com.amazic.library.ads.callback.ApiCallback;
import com.amazic.sample.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Admob.getInstance().initAdmob(this, () -> {
            AdmobApi.getInstance().init(getApplicationContext(), "", getString(R.string.app_id), new ApiCallback() {
                @Override
                public void onReady() {
                    super.onReady();
                    Log.d("SplashActivity", "onReady: " + AdmobApi.getInstance().getListIDBannerAll());
                    Log.d("SplashActivity", "onReady: " + AdmobApi.getInstance().getListIDAppOpenResume());
                    AppOpenManager.getInstance().init(getApplication(), AdmobApi.getInstance().getListIDAppOpenResume(), null);
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                }
            });
        });
    }
}
