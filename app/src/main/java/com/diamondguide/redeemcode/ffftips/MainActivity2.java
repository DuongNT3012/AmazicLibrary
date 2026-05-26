package com.diamondguide.redeemcode.ffftips;

import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.banner_ads.BannerPictureInPictureManager;
import com.amazic.library.ads.native_ads.NativeSqueezeBackManager;
import com.diamondguide.redeemcode.ffftips.databinding.ActivityMain2Binding;
import com.diamondguide.redeemcode.ffftips.databinding.ActivityMainBinding;

public class MainActivity2 extends AppCompatActivity {

    private ActivityMain2Binding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMain2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //ads
        NativeSqueezeBackManager backManager = new NativeSqueezeBackManager(this, this, AdmobApi.getInstance().getListIDByName("native_all"), "native_all", true);
        backManager.setIntervalReloadNative(7000);
        backManager.setAlwaysReloadOnResume(true);

        binding.tvClose.setOnClickListener(view -> {
            finish();
        });

        binding.tvShowNative.setOnClickListener(view -> {
            backManager.showAds();
        });

        binding.tvHideNative.setOnClickListener(view -> {
            backManager.hideAds();
        });


    }
}