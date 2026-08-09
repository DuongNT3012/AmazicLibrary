package com.amazic.library.ads.banner_ads;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.amazic.library.ads.callback.BannerCallback;
import com.amazic.mylibrary.R;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.List;

public class BannerBuilder {
    public boolean useNewAdLoading = false;
    public AdView bannerAdViewMain;
    public AdView bannerAdViewSecondary;
    public AdView bannerAdViewBackup;
    public List<String> listIdAdMain = new ArrayList<>();
    public List<String> listIdAdSecondary = new ArrayList<>();
    public List<String> listIdAdBackup = new ArrayList<>();
    public View shimmerBanner;
    private BannerCallback callBack = new BannerCallback();
    private FrameLayout frContainer;

    public BannerBuilder(FrameLayout frContainer) {
        this.frContainer = frContainer;
    }

    public BannerBuilder(Activity activity, FrameLayout frContainer, boolean useNewAdLoading) {
        this.useNewAdLoading = useNewAdLoading;
        this.frContainer = frContainer;
        //Show loading shimmer
        shimmerBanner = LayoutInflater.from(activity).inflate(R.layout.layout_shimmer_banner, null);
        if (frContainer != null) {
            frContainer.addView(shimmerBanner);
        }
    }

    public BannerCallback getCallBack() {
        return callBack;
    }

    public BannerBuilder setCallBack(BannerCallback callBack) {
        this.callBack = callBack;
        return this;
    }

    public List<String> getListIdAdMain() {
        return listIdAdMain;
    }

    public void setListIdAdMain(List<String> listIdAdMain) {
        this.listIdAdMain = listIdAdMain;
    }

    public List<String> getListIdAdSecondary() {
        return listIdAdSecondary;
    }

    public void setListIdAdSecondary(List<String> listIdAdSecondary) {
        this.listIdAdSecondary = listIdAdSecondary;
    }

    public List<String> getListIdAdBackup() {
        return listIdAdBackup;
    }

    public void setListIdAdBackup(List<String> listIdAdBackup) {
        this.listIdAdBackup = listIdAdBackup;
    }

    public FrameLayout getFrContainer() {
        return frContainer;
    }

    public void setFrContainer(FrameLayout frContainer) {
        this.frContainer = frContainer;
    }
}