package com.footballscores.livescore.soccerscores.sports;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.footballscores.livescore.soccerscores.sports.databinding.ActivityWelcomeBackBinding;

public class WelcomeBackActivity extends AppCompatActivity {
    private ActivityWelcomeBackBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWelcomeBackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        /*AppOpenManager.getInstance().loadAd(this, AdmobApi.getInstance().getListIDAppOpenResume(), new AppOpenCallback(){
            @Override
            public void onAdLoaded(AppOpenAd ad) {
                super.onAdLoaded(ad);
                Toast.makeText(WelcomeBackActivity.this, "onAdLoaded", Toast.LENGTH_SHORT).show();
            }
        }, "resume_wb");*/

        binding.tvWelcomeBack.setOnClickListener(view -> {
            Toast.makeText(this, "ClickTvWelcomeBack", Toast.LENGTH_SHORT).show();
            //finish();
            /*AppOpenManager.getInstance().showAdIfAvailableWelcomeBack(this, AdmobApi.getInstance().getListIDByName("resume_wb"), new AppOpenCallback(){
                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    finish();
                }

                @Override
                public void onAdFailedToShowFullScreenContent() {
                    super.onAdFailedToShowFullScreenContent();
                    finish();
                }
            }, "resume_wb");*/
            //load and show
            /*AppOpenManager.getInstance().loadAndShowResumeAds(this, AdmobApi.getInstance().getListIDByName(A), new AppOpenCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    finish();
                }

                @Override
                public void onAdFailedToShowFullScreenContent() {
                    super.onAdFailedToShowFullScreenContent();
                    finish();
                }

                @Override
                public void onAdFailedToLoad() {
                    super.onAdFailedToLoad();
                    finish();
                }
            }, "resume_wb");*/
            finish();
        });
    }
}
