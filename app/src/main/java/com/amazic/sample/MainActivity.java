package com.amazic.sample;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.amazic.sample.databinding.ActivityMainBinding;
import com.amazic.library.detect_test_ad.DetectTestAd;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        DetectTestAd.getInstance().setShowAds(true, this);
    }
}