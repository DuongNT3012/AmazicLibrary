package com.diamondguide.library.ads.native_ads;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;

import com.diamondguide.library.ads.admob.AdmobApi;
import com.diamondguide.library.ads.callback.NativeCallback;
import com.amazic.mylibrary.R;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.nativead.NativeAdView;

import java.util.ArrayList;
import java.util.List;

public class NativeBuilder {
    private static final String TAG = "NativeBuilder";
    private NativeCallback callback = new NativeCallback();
    List<String> listIdAd = new ArrayList<>();
    NativeAdView nativeAdView;
    NativeAdView nativeMetaAdView;
    ShimmerFrameLayout shimmerFrameLayout;
    private FrameLayout flAd;
    private int layoutNativeAdmob;
    private int layoutNativeMeta;
    private int layoutShimmerNative;

    public NativeBuilder(Context context, FrameLayout flAd, @LayoutRes int idLayoutShimmer, @LayoutRes int idLayoutNative, @LayoutRes int idLayoutNativeMeta) {
        setLayoutAds(context, flAd, idLayoutShimmer, idLayoutNative, idLayoutNativeMeta);
    }

    private void setLayoutAds(Context context, FrameLayout flAd, @LayoutRes int idLayoutShimmer, @LayoutRes int idLayoutNative, @LayoutRes int idLayoutNativeMeta) {
        View _nativeAdView = LayoutInflater.from(context).inflate(idLayoutNative, null);
        View _nativeMetaAdView = LayoutInflater.from(context).inflate(idLayoutNativeMeta, null);
        View _shimmerFrameLayout = LayoutInflater.from(context).inflate(idLayoutShimmer, null);

        //layout native admob
        if (_nativeAdView instanceof NativeAdView) {
            nativeAdView = (NativeAdView) _nativeAdView;
        } else {
            nativeAdView = (NativeAdView) LayoutInflater.from(context).inflate(com.amazic.mylibrary.R.layout.ads_native_large, null);
        }
        //layout native meta
        if (_nativeMetaAdView instanceof NativeAdView) {
            nativeMetaAdView = (NativeAdView) _nativeMetaAdView;
        } else {
            nativeMetaAdView = (NativeAdView) LayoutInflater.from(context).inflate(R.layout.ads_native_meta_large, null);
        }
        //shimmer native
        if (_shimmerFrameLayout instanceof ShimmerFrameLayout) {
            shimmerFrameLayout = (ShimmerFrameLayout) _shimmerFrameLayout;
        } else {
            shimmerFrameLayout = (ShimmerFrameLayout) LayoutInflater.from(context).inflate(R.layout.ads_shimmer_large, null);
        }

        this.flAd = flAd;
        this.layoutNativeAdmob = idLayoutNative;
        this.layoutNativeMeta = idLayoutNativeMeta;
        this.layoutShimmerNative = idLayoutShimmer;
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

    public List<String> getListIdAd() {
        return this.listIdAd;
    }

    public void setListIdAd(List<String> listIdAd) {
        this.listIdAd.clear();
        this.listIdAd.addAll(listIdAd);
    }

    public void setListIdAd(String nameIdAd) {
        this.listIdAd.clear();
        this.listIdAd.addAll(AdmobApi.getInstance().getListIDByName(nameIdAd));
    }

    public NativeCallback getCallback() {
        return callback;
    }

    public void setCallback(NativeCallback callback) {
        this.callback = callback;
    }
}
