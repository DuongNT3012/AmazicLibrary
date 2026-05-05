package com.amazic.library.ads.native_ads

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.amazic.library.ads.admob.AdmobApi
import com.amazic.library.ads.callback.NativeCallback
import com.amazic.mylibrary.R
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

class NativeBuilder {
    var callback: NativeCallback? = NativeCallback()
    val listIdAdMain: MutableList<String> = mutableListOf()
    val listIdAdSecondary: MutableList<String> = mutableListOf()
    val listIdAdBackup: MutableList<String> = mutableListOf()
    var nativeAdViewMain: NativeAdView? = null
    var nativeAdViewSecondary: NativeAdView? = null
    var nativeMetaAdView: NativeAdView? = null
    var shimmerFrameLayout: ShimmerFrameLayout? = null
    var flAd: FrameLayout? = null
        private set
    var layoutNativeAdmob: Int = 0
        private set
    var layoutNativeMeta: Int = 0
        private set
    var layoutShimmerNative: Int = 0
        private set
    var useNewAdLoading: Boolean = false
    var maxRequestBackup: Int = 1
    var maxRequest: Int = 1
    var maxRequestReload: Int = 1

    constructor(
        context: Context?,
        flAd: FrameLayout,
        @LayoutRes idLayoutShimmer: Int,
        @LayoutRes idLayoutNative: Int,
        @LayoutRes idLayoutNativeMeta: Int,
        useNewAdLoading: Boolean
    ) {
        this.useNewAdLoading = useNewAdLoading
        setLayoutAds(context, flAd, idLayoutShimmer, idLayoutNative, idLayoutNativeMeta)
    }

    constructor(
        context: Context?,
        flAd: FrameLayout,
        @LayoutRes idLayoutShimmer: Int,
        @LayoutRes idLayoutNative: Int,
        @LayoutRes idLayoutNativeMeta: Int
    ) {
        setLayoutAds(context, flAd, idLayoutShimmer, idLayoutNative, idLayoutNativeMeta)
    }

    private fun setLayoutAds(
        context: Context?,
        flAd: FrameLayout,
        @LayoutRes idLayoutShimmer: Int,
        @LayoutRes idLayoutNative: Int,
        @LayoutRes idLayoutNativeMeta: Int
    ) {
        this.flAd = flAd
        this.layoutNativeAdmob = idLayoutNative
        this.layoutNativeMeta = idLayoutNativeMeta
        this.layoutShimmerNative = idLayoutShimmer

        val _nativeAdView = LayoutInflater.from(context).inflate(idLayoutNative, null)
        val _nativeMetaAdView = LayoutInflater.from(context).inflate(idLayoutNativeMeta, null)
        val _shimmerFrameLayout = LayoutInflater.from(context).inflate(idLayoutShimmer, null)

        //layout native admob
        if (_nativeAdView is NativeAdView) {
            nativeAdViewMain =
                LayoutInflater.from(context).inflate(idLayoutNative, null) as NativeAdView
        } else {
            layoutNativeAdmob = R.layout.ads_native_large
            nativeAdViewMain = LayoutInflater.from(context)
                .inflate(R.layout.ads_native_large, null) as NativeAdView
        }
        //layout native meta
        if (_nativeMetaAdView is NativeAdView) {
            nativeMetaAdView =
                LayoutInflater.from(context).inflate(idLayoutNativeMeta, null) as NativeAdView?
        } else {
            layoutNativeMeta = R.layout.ads_native_meta_large
            nativeMetaAdView = LayoutInflater.from(context)
                .inflate(R.layout.ads_native_meta_large, null) as NativeAdView?
        }
        //shimmer native
        if (_shimmerFrameLayout is ShimmerFrameLayout) {
            shimmerFrameLayout =
                LayoutInflater.from(context).inflate(idLayoutShimmer, null) as ShimmerFrameLayout?
        } else {
            layoutShimmerNative = R.layout.ads_shimmer_large
            shimmerFrameLayout = LayoutInflater.from(context)
                .inflate(R.layout.ads_shimmer_large, null) as ShimmerFrameLayout?
        }

        if (useNewAdLoading) {
            flAd.removeAllViews()
            nativeAdViewSecondary =
                LayoutInflater.from(context).inflate(idLayoutNativeMeta, null) as NativeAdView
            nativeAdViewMain?.visibility = View.GONE
            nativeAdViewSecondary?.visibility = View.GONE
            flAd.addView(nativeAdViewSecondary)
            flAd.addView(nativeAdViewMain)
            flAd.addView(shimmerFrameLayout)
        }
    }

    fun setListIdAdMain(listIdAd: MutableList<String>) {
        this.listIdAdMain.clear()
        this.listIdAdMain.addAll(listIdAd)
    }

    fun setListIdAdMain(nameIdAd: String) {
        this.listIdAdMain.clear()
        this.listIdAdMain.addAll(AdmobApi.getInstance().getListIDByName(nameIdAd))
    }

    fun setListIdAdSecondary(listIdAd: MutableList<String>) {
        this.listIdAdSecondary.clear()
        this.listIdAdSecondary.addAll(listIdAd)
    }

    fun setListIdAdSecondary(nameIdAd: String) {
        this.listIdAdSecondary.clear()
        this.listIdAdSecondary.addAll(AdmobApi.getInstance().getListIDByName(nameIdAd))
    }

    fun setListIdAdBackup(listIdAd: MutableList<String>) {
        this.listIdAdBackup.clear()
        this.listIdAdBackup.addAll(listIdAd)
    }

    fun setListIdAdBackup(nameIdAd: String) {
        this.listIdAdBackup.clear()
        this.listIdAdBackup.addAll(AdmobApi.getInstance().getListIDByName(nameIdAd))
    }

    companion object {
        private const val TAG = "NativeBuilder"
    }
}
