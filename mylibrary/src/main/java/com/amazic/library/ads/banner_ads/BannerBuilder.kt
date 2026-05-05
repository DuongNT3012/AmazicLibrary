package com.amazic.library.ads.banner_ads

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.amazic.library.ads.callback.BannerCallback
import com.amazic.mylibrary.R
import com.google.android.libraries.ads.mobile.sdk.banner.AdView

class BannerBuilder {
    var callBack: BannerCallback = BannerCallback()
    var frContainer: FrameLayout? = null
    var useNewAdLoading: Boolean = false

    var bannerAdViewMain: AdView? = null
    var bannerAdViewSecondary: AdView? = null
    var bannerAdViewBackup: AdView? = null

    val listIdAdMain: MutableList<String> = mutableListOf()
    val listIdAdSecondary: MutableList<String> = mutableListOf()
    val listIdAdBackup: MutableList<String> = mutableListOf()

    var shimmerBanner: View? = null

    // Secondary constructor to match your first Java constructor
    constructor(frContainer: FrameLayout?) {
        this.frContainer = frContainer
    }

    // Secondary constructor to match your second Java constructor
    constructor(activity: Activity, frContainer: FrameLayout?, useNewAdLoading: Boolean) {
        this.useNewAdLoading = useNewAdLoading
        this.frContainer = frContainer

        // Show loading shimmer
        shimmerBanner = LayoutInflater.from(activity).inflate(R.layout.layout_shimmer_banner, null)
        frContainer?.addView(shimmerBanner)
    }

    // Builder-style pattern for Callback
    fun setCallBack(callBack: BannerCallback): BannerBuilder {
        this.callBack = callBack
        return this
    }

    fun setListIdAdMain(listIdAdMain: MutableList<String>): BannerBuilder {
        this.listIdAdMain.clear()
        this.listIdAdMain.addAll(listIdAdMain)
        return this
    }

    fun setListIdAdSecondary(listIdAdSecondary: MutableList<String>): BannerBuilder {
        this.listIdAdSecondary.clear()
        this.listIdAdSecondary.addAll(listIdAdSecondary)
        return this
    }

    fun setListIdAdBackup(listIdAdBackup: MutableList<String>): BannerBuilder {
        this.listIdAdBackup.clear()
        this.listIdAdBackup.addAll(listIdAdBackup)
        return this
    }
}