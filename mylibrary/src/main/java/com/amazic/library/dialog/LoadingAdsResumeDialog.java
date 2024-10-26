package com.amazic.library.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.amazic.mylibrary.R;

public class LoadingAdsResumeDialog extends Dialog {
    public LoadingAdsResumeDialog(@NonNull Context context) {
        super(context, R.style.AppTheme);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_loading_ads_resume);
    }
}
