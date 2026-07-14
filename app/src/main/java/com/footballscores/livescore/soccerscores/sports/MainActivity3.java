package com.footballscores.livescore.soccerscores.sports;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.banner_ads.BannerPictureInPictureManager;
import com.footballscores.livescore.soccerscores.sports.databinding.ActivityMain3Binding;
import com.google.ads.noninterruptive.pictureinpicturead.PictureInPictureAd;

public class MainActivity3 extends AppCompatActivity {

    private ActivityMain3Binding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMain3Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvClose.setOnClickListener(view -> {
            finish();
        });

        BannerPictureInPictureManager banner = new BannerPictureInPictureManager(this, this, AdmobApi.getInstance().getListIDByName("banner_all"), "native_all");
        banner.setIntervalReloadBanner(7000);
        banner.setAlwaysReloadOnResume(true);
        banner.setPosition(PictureInPictureAd.AdPosition.TOP_RIGHT);
    }
}