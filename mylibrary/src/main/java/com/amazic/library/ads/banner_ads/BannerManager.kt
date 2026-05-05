package com.amazic.library.ads.banner_ads

import android.app.Activity
import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.amazic.library.ads.admob.Admob
import com.amazic.library.ads.admob.admob_interface.IOnAdsImpression
import com.amazic.library.ads.callback.BannerCallback
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BannerManager : LifecycleEventObserver {
    private val builder: BannerBuilder
    private var currentActivity: Activity? = null
    private val lifecycleOwner: LifecycleOwner
    private var isReloadAds = false
    private var isAlwaysReloadOnResume = false
    private var intervalReloadBanner: Long = 0
    private var isStop = false
    private var countDownTimer: CountDownTimer? = null
    private var context: Context? = null
    private var adWidth = 0
    private var isLoadBannerFragment = false
    private val remoteKey: String?
    var remoteKeySecondary: String = ""
    var remoteKeyBackup: String? = ""
    private var isLoadedBannerMain = false
    private var isLoadedBannerSecondary = false

    constructor(
        currentActivity: Activity,
        lifecycleOwner: LifecycleOwner,
        builder: BannerBuilder,
        remoteKey: String
    ) {
        this.isLoadBannerFragment = false
        this.builder = builder
        this.currentActivity = currentActivity
        this.remoteKey = remoteKey
        this.remoteKeySecondary = remoteKey
        this.remoteKeyBackup = remoteKey
        this.lifecycleOwner = lifecycleOwner
        this.lifecycleOwner.lifecycle.addObserver(this)
    }

    constructor(
        context: Context?,
        adWidth: Int,
        lifecycleOwner: LifecycleOwner,
        builder: BannerBuilder,
        remoteKey: String
    ) {
        this.isLoadBannerFragment = true
        this.builder = builder
        this.context = context
        this.adWidth = adWidth
        this.remoteKey = remoteKey
        this.remoteKeySecondary = remoteKey
        this.remoteKeyBackup = remoteKey
        this.lifecycleOwner = lifecycleOwner
        this.lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                Log.d(TAG, "onStateChanged: ON_CREATE")
                reloadAdNow()
            }

            Lifecycle.Event.ON_RESUME -> {
                if (countDownTimer != null && isStop) {
                    startReloadBanner()
                }
                val valueLog = isStop.toString() + " && " + (isReloadAds || isAlwaysReloadOnResume)
                Log.d(TAG, "onStateChanged: resume\n$valueLog")
                if (isStop && (isReloadAds || isAlwaysReloadOnResume)) {
                    isReloadAds = false
                    reloadAdNow()
                }
                isStop = false
            }

            Lifecycle.Event.ON_PAUSE -> {
                Log.d(TAG, "onStateChanged: ON_PAUSE")
                isStop = true
                if (countDownTimer != null) {
                    countDownTimer?.cancel()
                }
            }

            Lifecycle.Event.ON_DESTROY -> {
                Log.d(TAG, "onStateChanged: ON_DESTROY")
                if (builder.frContainer != null) {
                    builder.frContainer?.removeAllViews()
                }
                if (builder.bannerAdViewMain != null) {
                    builder.bannerAdViewMain?.destroy()
                }
                if (builder.bannerAdViewSecondary != null) {
                    builder.bannerAdViewSecondary?.destroy()
                }
                this.lifecycleOwner.lifecycle.removeObserver(this)
            }

            else -> {}
        }
    }

    private fun loadBanner(frContainer: FrameLayout) {
        Log.d(TAG, "loadBanner: " + builder.listIdAdMain)
        if (Admob.getInstance().showAllAds) {
            Admob.getInstance().loadBannerAds(
                currentActivity,
                builder.listIdAdMain,
                frContainer,
                builder.callBack,
                { this.startReloadBanner() },
                remoteKey
            )
        } else {
            frContainer.visibility = View.GONE
        }
    }

    private fun loadBannerFragment(frContainer: FrameLayout) {
        Log.d(TAG, "loadBanner: " + builder.listIdAdMain)
        if (Admob.getInstance().showAllAds) {
            Admob.getInstance().loadBannerAds(
                context,
                adWidth,
                builder.listIdAdMain,
                frContainer,
                builder.callBack,
                { this.startReloadBanner() },
                remoteKey
            )
        } else {
            frContainer.visibility = View.GONE
        }
    }

    fun setReloadAds() {
        isReloadAds = true
    }

    fun reloadAdNow() {
        if (builder.useNewAdLoading) {
            loadNewAdFormat()
        } else {
            loadOldAdFormat()
        }
    }

    private fun loadNewAdFormat() {
        if (remoteKeySecondary.isEmpty()) {
            if (!Admob.getInstance().checkCondition(currentActivity, remoteKey)) {
                if (builder.frContainer != null) {
                    builder.frContainer?.removeAllViews()
                }
                return
            }
        } else {
            if (!Admob.getInstance()
                    .checkCondition(currentActivity, remoteKey) && !Admob.getInstance()
                    .checkCondition(currentActivity, remoteKeySecondary)
            ) {
                if (builder.frContainer != null) {
                    builder.frContainer?.removeAllViews()
                }
                return
            }
        }
        loadMainBanner()
        loadSecondaryBanner()
    }

    private fun loadMainBanner() {
        isLoadedBannerMain = false
        if (builder.bannerAdViewMain != null) {
            builder.bannerAdViewMain?.destroy()
        }
        builder.bannerAdViewMain = Admob.getInstance().loadBannerAdsWithoutShow(
            currentActivity,
            builder.listIdAdMain,
            object : BannerCallback() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    MainScope().launch {
                        isLoadedBannerMain = true
                        builder.callBack.onAdLoaded()
                        Log.d(TAG, "onAdLoaded: Main")
                        if (builder.frContainer != null) {
                            builder.frContainer?.removeView(builder.bannerAdViewSecondary)
                            builder.frContainer?.addView(builder.bannerAdViewMain)
                            builder.frContainer?.removeView(builder.shimmerBanner)
                        }
                    }
                }

                override fun onAdFailedToLoad() {
                    super.onAdFailedToLoad()
                    MainScope().launch {
                        builder.callBack.onAdFailedToLoad()
                        Log.d(TAG, "onAdFailedToLoad: Main")
                        if (isLoadedBannerSecondary) {
                            if (builder.frContainer != null) {
                                builder.frContainer?.removeView(builder.shimmerBanner)
                            }
                        }
                        loadBannerBackup()
                    }
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    MainScope().launch {
                        builder.callBack.onAdImpression()
                        Log.d(TAG, "onAdImpression: Main")
                        startReloadBanner()
                    }
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    MainScope().launch {
                        builder.callBack.onAdClicked()
                        Log.d(TAG, "onAdClicked: Main")
                    }
                }
            },
            remoteKey
        )
    }

    private fun loadSecondaryBanner() {
        isLoadedBannerSecondary = false
        if (builder.bannerAdViewSecondary != null) {
            builder.bannerAdViewSecondary?.destroy()
        }
        builder.bannerAdViewSecondary = Admob.getInstance().loadBannerAdsWithoutShow(
            currentActivity,
            builder.listIdAdSecondary,
            object : BannerCallback() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    MainScope().launch {
                        isLoadedBannerSecondary = true
                        builder.callBack.onAdLoaded()
                        Log.d(TAG, "onAdLoaded: Secondary")
                        if (builder.frContainer != null) {
                            builder.frContainer?.removeView(builder.bannerAdViewMain)
                            builder.frContainer?.addView(builder.bannerAdViewSecondary)
                            builder.frContainer?.removeView(builder.shimmerBanner)
                        }
                    }
                }

                override fun onAdFailedToLoad() {
                    super.onAdFailedToLoad()
                    MainScope().launch {
                        builder.callBack.onAdFailedToLoad()
                        Log.d(TAG, "onAdFailedToLoad: Secondary")
                        if (isLoadedBannerMain) {
                            if (builder.frContainer != null) {
                                builder.frContainer?.removeView(builder.shimmerBanner)
                            }
                        }
                        loadBannerBackup()
                    }
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    MainScope().launch {
                        builder.callBack.onAdImpression()
                        Log.d(TAG, "onAdImpression: Secondary")
                        handleImpressionBannerSecondary()
                        startReloadBanner()
                    }
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    MainScope().launch {
                        builder.callBack.onAdClicked()
                        Log.d(TAG, "onAdClicked: Secondary")
                    }
                }
            },
            remoteKeySecondary
        )
    }

    private fun handleImpressionBannerSecondary() {
        try {
            if (isLoadedBannerMain) {
                MainScope().launch {
                    delay(1000)
                    if (builder.frContainer != null && builder.bannerAdViewSecondary != null) {
                        builder.frContainer?.removeView(builder.bannerAdViewSecondary)
                        if (builder.bannerAdViewMain != null) {
                            val parent = builder.bannerAdViewMain?.parent
                            if (parent is ViewGroup) {
                                parent.removeView(builder.bannerAdViewMain)
                            }
                            builder.frContainer?.addView(builder.bannerAdViewMain)
                        }
                    }
                    if (builder.frContainer != null && builder.shimmerBanner != null) {
                        builder.frContainer?.removeView(builder.shimmerBanner)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "handleImpressionBannerSecondary: " + e.message)
        }
    }

    private fun loadBannerBackup() {
        if (builder.bannerAdViewBackup != null) {
            builder.bannerAdViewBackup?.destroy()
        }
        builder.bannerAdViewBackup = Admob.getInstance().loadBannerAdsBackupWithoutShow(
            currentActivity,
            builder.listIdAdBackup,
            object : BannerCallback() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    MainScope().launch {
                        Log.d(TAG, "onAdLoaded: Backup")
                        if (builder.frContainer != null) {
                            builder.frContainer?.removeView(builder.bannerAdViewMain)
                            builder.frContainer?.removeView(builder.bannerAdViewSecondary)
                            builder.frContainer?.removeView(builder.shimmerBanner)
                            builder.frContainer?.addView(builder.bannerAdViewBackup)
                        }
                    }
                }

                override fun onAdFailedToLoad() {
                    super.onAdFailedToLoad()
                    MainScope().launch {
                        Log.d(TAG, "onAdFailedToLoad: Backup")
                        startReloadBanner()
                    }
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    MainScope().launch {
                        Log.d(TAG, "onAdImpression: Backup")
                        startReloadBanner()
                    }
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    MainScope().launch {
                        Log.d(TAG, "onAdClicked: Backup")
                    }
                }
            },
            remoteKeyBackup
        )
    }

    private fun loadOldAdFormat() {
        if (isLoadBannerFragment) {
            loadBannerFragment(builder.frContainer!!)
        } else {
            loadBanner(builder.frContainer!!)
        }
    }

    fun setAlwaysReloadOnResume(isAlwaysReloadOnResume: Boolean) {
        this.isAlwaysReloadOnResume = isAlwaysReloadOnResume
    }

    private fun startReloadBanner() {
        if (countDownTimer != null && this.lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED) {
            Log.d(TAG, "startReloadBanner: ")
            countDownTimer?.cancel()
            countDownTimer?.start()
        }
    }

    fun setIntervalReloadBanner(intervalReloadBanner: Long) {
        if (intervalReloadBanner > 0) {
            this.intervalReloadBanner = intervalReloadBanner
            MainScope().launch {
                countDownTimer = object : CountDownTimer(intervalReloadBanner, 1000) {
                    override fun onTick(l: Long) {
                    }

                    override fun onFinish() {
                        reloadAdNow()
                    }
                }
            }
        }
    }

    fun cancelAutoReloadBanner() {
        if (countDownTimer != null) {
            countDownTimer?.cancel()
        }
    }

    fun resumeAutoReloadBanner() {
        if (countDownTimer != null) {
            countDownTimer?.start()
        }
    }

    companion object {
        private const val TAG = "BannerManager"
    }
}
