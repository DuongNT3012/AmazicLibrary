package com.amazic.library.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;

import com.amazic.library.ads.admob.Admob;
import com.amazic.mylibrary.R;
import com.amazic.mylibrary.databinding.DialogLoadingAdsBinding;

import java.util.Random;

public class LoadingAdsDialog extends Dialog {
    private DialogLoadingAdsBinding binding;

    public LoadingAdsDialog(@NonNull Context context) {
        super(context, R.style.AppTheme);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DialogLoadingAdsBinding.inflate(LayoutInflater.from(getContext()));
        setContentView(binding.getRoot());
        setCancelable(false);
        if (Admob.getInstance().isCustomAnimationDialog()) {
            Random random = new Random();
            int randomIndex = random.nextInt(Admob.getInstance().listAnimationDialogRaw.size());
            int randomElement = Admob.getInstance().listAnimationDialogRaw.get(randomIndex);
            setUseAnimationView(randomElement);
        } else {
            setUseProgressBar();
        }
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
