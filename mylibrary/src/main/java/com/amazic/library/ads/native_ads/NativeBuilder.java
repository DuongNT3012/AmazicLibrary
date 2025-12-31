package com.amazic.library.ads.native_ads;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.callback.NativeCallback;
import com.amazic.mylibrary.R;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.nativead.NativeAdView;

import java.util.ArrayList;
import java.util.List;

public class NativeBuilder {
    private static final String TAG = "NativeBuilder";
    private NativeCallback callback = new NativeCallback();
    List<String> listIdAdMain = new ArrayList<>();
    List<String> listIdAdSecondary = new ArrayList<>();
    List<String> listIdAdBackup = new ArrayList<>();
    NativeAdView nativeAdViewMain;
    NativeAdView nativeAdViewSecondary;
    NativeAdView nativeMetaAdView;
    ShimmerFrameLayout shimmerFrameLayout;
    private FrameLayout flAd;
    private int layoutNativeAdmob;
    private int layoutNativeMeta;
    private int layoutShimmerNative;
    public boolean useNewAdLoading = false;
    public int maxRequestBackup = 1;
    public int maxRequest = 1;
    public int maxRequestReload = 1;

    public NativeBuilder(Context context, @NonNull FrameLayout flAd, @LayoutRes int idLayoutShimmer, @LayoutRes int idLayoutNative, @LayoutRes int idLayoutNativeMeta, boolean useNewAdLoading) {
        this.useNewAdLoading = useNewAdLoading;
        setLayoutAds(context, flAd, idLayoutShimmer, idLayoutNative, idLayoutNativeMeta);
    }

    public NativeBuilder(Context context, FrameLayout flAd, @LayoutRes int idLayoutShimmer, @LayoutRes int idLayoutNative, @LayoutRes int idLayoutNativeMeta) {
        setLayoutAds(context, flAd, idLayoutShimmer, idLayoutNative, idLayoutNativeMeta);
    }

    private void setLayoutAds(Context context, FrameLayout flAd, @LayoutRes int idLayoutShimmer, @LayoutRes int idLayoutNative, @LayoutRes int idLayoutNativeMeta) {
        this.flAd = flAd;
        this.layoutNativeAdmob = idLayoutNative;
        this.layoutNativeMeta = idLayoutNativeMeta;
        this.layoutShimmerNative = idLayoutShimmer;

        View _nativeAdView = LayoutInflater.from(context).inflate(idLayoutNative, null);
        View _nativeMetaAdView = LayoutInflater.from(context).inflate(idLayoutNativeMeta, null);
        View _shimmerFrameLayout = LayoutInflater.from(context).inflate(idLayoutShimmer, null);

        //layout native admob
        if (_nativeAdView instanceof NativeAdView) {
            nativeAdViewMain = (NativeAdView) LayoutInflater.from(context).inflate(idLayoutNative, null);
        } else {
            layoutNativeAdmob = com.amazic.mylibrary.R.layout.ads_native_large;
            nativeAdViewMain = (NativeAdView) LayoutInflater.from(context).inflate(com.amazic.mylibrary.R.layout.ads_native_large, null);
        }
        //layout native meta
        if (_nativeMetaAdView instanceof NativeAdView) {
            nativeMetaAdView = (NativeAdView) LayoutInflater.from(context).inflate(idLayoutNativeMeta, null);
        } else {
            layoutNativeMeta = R.layout.ads_native_meta_large;
            nativeMetaAdView = (NativeAdView) LayoutInflater.from(context).inflate(R.layout.ads_native_meta_large, null);
        }
        //shimmer native
        if (_shimmerFrameLayout instanceof ShimmerFrameLayout) {
            shimmerFrameLayout = (ShimmerFrameLayout) LayoutInflater.from(context).inflate(idLayoutShimmer, null);
        } else {
            layoutShimmerNative = R.layout.ads_shimmer_large;
            shimmerFrameLayout = (ShimmerFrameLayout) LayoutInflater.from(context).inflate(R.layout.ads_shimmer_large, null);
        }

        if (useNewAdLoading) {
            flAd.removeAllViews();
            nativeAdViewSecondary = (NativeAdView) LayoutInflater.from(context).inflate(idLayoutNativeMeta, null);
            nativeAdViewMain.setVisibility(View.GONE);
            nativeAdViewSecondary.setVisibility(View.GONE);
            flAd.addView(nativeAdViewSecondary);
            flAd.addView(nativeAdViewMain);
            flAd.addView(shimmerFrameLayout);
        }
    }

    public int getLayoutShimmerNative() {
        return this.layoutShimmerNative;
    }

    public int getLayoutNativeAdmob() {
        return this.layoutNativeAdmob;
    }

    public int getLayoutNativeMeta() {
        return this.layoutNativeMeta;
    }

    public FrameLayout getFlAd() {
        return this.flAd;
    }

    public List<String> getListIdAdMain() {
        return this.listIdAdMain;
    }

    public void setListIdAdMain(List<String> listIdAd) {
        this.listIdAdMain.clear();
        this.listIdAdMain.addAll(listIdAd);
    }

    public void setListIdAdMain(String nameIdAd) {
        this.listIdAdMain.clear();
        this.listIdAdMain.addAll(AdmobApi.getInstance().getListIDByName(nameIdAd));
    }

    public List<String> getListIdAdSecondary() {
        return this.listIdAdSecondary;
    }

    public void setListIdAdSecondary(List<String> listIdAd) {
        this.listIdAdSecondary.clear();
        this.listIdAdSecondary.addAll(listIdAd);
    }

    public void setListIdAdSecondary(String nameIdAd) {
        this.listIdAdSecondary.clear();
        this.listIdAdSecondary.addAll(AdmobApi.getInstance().getListIDByName(nameIdAd));
    }

    public List<String> getListIdAdBackup() {
        return this.listIdAdBackup;
    }

    public void setListIdAdBackup(List<String> listIdAd) {
        this.listIdAdBackup.clear();
        this.listIdAdBackup.addAll(listIdAd);
    }

    public void setListIdAdBackup(String nameIdAd) {
        this.listIdAdBackup.clear();
        this.listIdAdBackup.addAll(AdmobApi.getInstance().getListIDByName(nameIdAd));
    }

    public NativeCallback getCallback() {
        return callback;
    }

    public void setCallback(NativeCallback callback) {
        this.callback = callback;
    }
}
