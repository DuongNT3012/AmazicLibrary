package com.footballscores.livescore.soccerscores.sports;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.native_ads.NativeSqueezeBackManager;
import com.footballscores.livescore.soccerscores.sports.databinding.ActivityMain2Binding;
import com.footballscores.livescore.soccerscores.sports.databinding.ActivityMainBinding;

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