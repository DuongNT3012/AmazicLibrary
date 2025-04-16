package com.amazic.library.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;

import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.mylibrary.R;
import com.amazic.mylibrary.databinding.DialogLoadingAdsResumeBinding;

public class LoadingAdsResumeDialog extends Dialog {
    private DialogLoadingAdsResumeBinding binding;

    public LoadingAdsResumeDialog(@NonNull Context context) {
        super(context, R.style.AppTheme);
        binding = DialogLoadingAdsResumeBinding.inflate(LayoutInflater.from(context));
        if (AppOpenManager.getInstance().isCustomAnimationDialog()) {
            setUseAnimationView(AppOpenManager.getInstance().animationDialogRaw);
        } else {
            setUseProgressBar();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(binding.getRoot());
    }

    public void setUseAnimationView(int resId) {
        binding.progressBar.setVisibility(View.GONE);
        binding.animationView.setVisibility(View.VISIBLE);
        binding.animationView.setAnimation(resId);
        binding.animationView.playAnimation();
    }

    public void setUseProgressBar() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.animationView.setVisibility(View.GONE);
    }
}
