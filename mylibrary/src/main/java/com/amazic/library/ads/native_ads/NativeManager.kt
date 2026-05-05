package com.amazic.library.ads.native_ads

import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.amazic.library.Utils.RemoteConfigHelper
import com.amazic.library.ads.admob.Admob
import com.amazic.library.ads.callback.NativeCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NativeManager : LifecycleEventObserver {
    private val builder: NativeBuilder
    private val context: Context
    private val lifecycleOwner: LifecycleOwner
    private var isReloadAds = false
    private var isAlwaysReloadOnResume = false
    private var intervalReloadNative: Long = 0
    private var isStop = false
    private var isTimerRunning = false
    private var countDownTimer: CountDownTimer? = null
    private val remoteKey: String?
    private var remoteKeySecondary = ""
    private var remoteKeyBackup: String? = ""
    private var myNativeAdMain: NativeAd? = null
    private var myNativeAdSecondary: NativeAd? = null
    private var handlerTimeoutCallNative: Handler? = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    var timeOutCallAds: Int = 12000
    private var canLoadMainNative = true
    private var canLoadSecondaryNative = true
    private var remoteKeyAdNativeDisplayOrder: String? = null
    private var randomPercentShowNativeMain = 100
    private var isShowNativeSecond = false

    constructor(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        builder: NativeBuilder,
        remoteKey: String
    ) {
        this.builder = builder
        this.context = context
        this.remoteKey = remoteKey
        this.remoteKeySecondary = remoteKey
        this.remoteKeyBackup = remoteKey
        this.lifecycleOwner = lifecycleOwner
        this.lifecycleOwner.lifecycle.addObserver(this)
    }

    constructor(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        builder: NativeBuilder,
        remoteKey: String?,
        remoteKeySecondary: String
    ) {
        this.builder = builder
        this.context = context
        this.remoteKey = remoteKey
        this.remoteKeySecondary = remoteKeySecondary
        this.remoteKeyBackup = remoteKey
        this.lifecycleOwner = lifecycleOwner
        this.lifecycleOwner.lifecycle.addObserver(this)
    }

    constructor(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        builder: NativeBuilder,
        remoteKey: String?,
        remoteKeySecondary: String,
        remoteKeyAdNativeDisplayOrder: String?
    ) {
        this.builder = builder
        this.context = context
        this.remoteKey = remoteKey
        this.remoteKeySecondary = remoteKeySecondary
        this.remoteKeyBackup = remoteKey
        this.lifecycleOwner = lifecycleOwner
        this.lifecycleOwner.lifecycle.addObserver(this)
        this.remoteKeyAdNativeDisplayOrder = remoteKeyAdNativeDisplayOrder
    }

    constructor(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        builder: NativeBuilder,
        remoteKey: String?,
        remoteKeySecondary: String,
        remoteKeyAdNativeDisplayOrder: String?,
        remoteKeyBackup: String?
    ) {
        this.builder = builder
        this.context = context
        this.remoteKey = remoteKey
        this.remoteKeySecondary = remoteKeySecondary
        this.remoteKeyBackup = remoteKeyBackup
        this.lifecycleOwner = lifecycleOwner
        this.lifecycleOwner.lifecycle.addObserver(this)
        this.remoteKeyAdNativeDisplayOrder = remoteKeyAdNativeDisplayOrder
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                Log.d(TAG, "onStateChanged: ON_CREATE")
                randomPercentShowNativeMain = (Math.random() * 101).toInt()
                loadNativeFloor(builder.maxRequest)
            }

            Lifecycle.Event.ON_RESUME -> {
                if (countDownTimer != null && isStop) {
                    isTimerRunning = true
                    countDownTimer?.start()
                }
                val valueLog = isStop.toString() + " && " + (isReloadAds || isAlwaysReloadOnResume)
                Log.d(TAG, "onStateChanged: resume\n$valueLog")
                if (isStop && (isReloadAds || isAlwaysReloadOnResume)) {
                    isReloadAds = false
                    loadNativeFloor(builder.maxRequestReload)
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
                if (myNativeAdMain != null) {
                    myNativeAdMain?.destroy()
                }
                if (myNativeAdSecondary != null) {
                    myNativeAdSecondary?.destroy()
                }
                Log.d(TAG, "onStateChanged: ON_DESTROY")
                this.lifecycleOwner.lifecycle.removeObserver(this)
            }

            else -> {}
        }
    }

    private fun loadNativeFloor(maxRequest: Int) {
        Log.d(TAG, "loadNativeFloor: " + builder.useNewAdLoading)
        isFailedMain = false
        isFailedSecondary = false
        if (builder.useNewAdLoading) {
            loadNewAdFormat()
        }
    }

    private fun handleTimeoutCallNative() {
        //Set timeout call native x(s) if cannot load
        runnable = Runnable {
            if (!canLoadMainNative || !canLoadSecondaryNative) {
                canLoadMainNative = true
                canLoadSecondaryNative = true
                startReloadNative()
            }
            if (handlerTimeoutCallNative != null) {
                handlerTimeoutCallNative = null
            }
        }
        if (handlerTimeoutCallNative != null) {
            handlerTimeoutCallNative?.postDelayed(runnable!!, timeOutCallAds.toLong())
        }
    }

    private fun loadNewAdFormat() {
        if (remoteKeySecondary.isEmpty()) {
            if (!Admob.getInstance().checkCondition(context, remoteKey)) {
                builder.shimmerFrameLayout?.visibility = View.GONE
                if (builder.nativeAdViewMain != null) builder.nativeAdViewMain?.visibility =
                    View.GONE
                if (builder.nativeAdViewSecondary != null) builder.nativeAdViewSecondary?.visibility =
                    View.GONE
                return
            }
        } else {
            if (!Admob.getInstance().checkCondition(context, remoteKey) && !Admob.getInstance()
                    .checkCondition(context, remoteKeySecondary)
            ) {
                builder.shimmerFrameLayout?.visibility = View.GONE
                if (builder.nativeAdViewMain != null) builder.nativeAdViewMain?.visibility =
                    View.GONE
                if (builder.nativeAdViewSecondary != null) builder.nativeAdViewSecondary?.visibility =
                    View.GONE
                return
            }
        }

        if (myNativeAdMain == null && myNativeAdSecondary == null) builder.shimmerFrameLayout?.visibility =
            View.VISIBLE
        if (!Admob.getInstance().checkCondition(context, remoteKey)) {
            if (builder.nativeAdViewMain != null) builder.nativeAdViewMain?.visibility = View.GONE
        }
        if (!Admob.getInstance().checkCondition(context, remoteKeySecondary)) {
            if (builder.nativeAdViewSecondary != null) builder.nativeAdViewSecondary?.visibility =
                View.GONE
        }
        //check show native Main Or Second first
        if (remoteKeyAdNativeDisplayOrder != null) {
            val percentRemote = RemoteConfigHelper.getInstance().get_config_long(context, remoteKeyAdNativeDisplayOrder)
            isShowNativeSecond = randomPercentShowNativeMain > percentRemote
            Log.d(
                TAG,
                "percent show Native Second: randomPercentShowNativeMain = $randomPercentShowNativeMain, percentRemote = $percentRemote"
            )
        }

        //
        handleTimeoutCallNative()
        if (isShowNativeSecond) {
            //show ads native second len dau
            loadSecondaryNative()
            loadMainNative()
        } else {
            //show ads native main len dau
            loadMainNative()
            loadSecondaryNative()
        }
    }

    private fun loadMainNative() {
        Log.d(TAG, "loadMainNative: $canLoadMainNative")
        if (canLoadMainNative) {
            canLoadMainNative = false
            if (myNativeAdMain != null) myNativeAdMain?.destroy()
            Admob.getInstance()
                .loadNativeAds(context, builder.listIdAdMain, object : NativeCallback() {
                    override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                        super.onNativeAdLoaded(nativeAd)
                        MainScope().launch {
                            canLoadMainNative = true
                            Log.d(TAG, "onNativeAdLoaded: Main")
                            builder.callback?.onNativeAdLoaded(nativeAd)
                            showNativeMain(nativeAd)
                        }
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        MainScope().launch {
                            Log.d(TAG, "onAdImpression: Main")
                            builder.callback?.onAdImpression()
                            handleImpressionNative()
                            startReloadNative()
                        }
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        MainScope().launch {
                            builder.callback?.onAdClicked()
                            Log.d(TAG, "onAdClicked: Main")
                        }
                    }

                    override fun onAdFailedToLoad(message: String?) {
                        super.onAdFailedToLoad(message)
                        MainScope().launch {
                            canLoadMainNative = true
                            Log.e(TAG, "onAdFailedToLoad: Main\n$message")
                            builder.callback?.onAdFailedToLoad(message)
                            if (myNativeAdSecondary != null) {
                                builder.shimmerFrameLayout?.visibility = View.GONE
                            }
                            loadNativeBackup(true)
                        }
                    }
                }, remoteKey)
        }
    }

    private fun loadSecondaryNative() {
        Log.d(TAG, "loadSecondaryNative: $canLoadSecondaryNative")
        if (canLoadSecondaryNative) {
            canLoadSecondaryNative = false
            if (myNativeAdSecondary != null) myNativeAdSecondary?.destroy()
            Admob.getInstance()
                .loadNativeAds(context, builder.listIdAdSecondary, object : NativeCallback() {
                    override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                        super.onNativeAdLoaded(nativeAd)
                        MainScope().launch {
                            canLoadSecondaryNative = true
                            builder.callback?.onNativeAdLoaded(nativeAd)
                            Log.d(TAG, "onNativeAdLoaded: Secondary")
                            showNativeSecondary(nativeAd)
                        }
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        MainScope().launch {
                            Log.d(TAG, "onAdImpression: Secondary")
                            builder.callback?.onAdImpression()
                            handleImpressionNative()
                            startReloadNative()
                        }
                    }

                    override fun onAdFailedToLoad(message: String?) {
                        super.onAdFailedToLoad(message)
                        MainScope().launch {
                            canLoadSecondaryNative = true
                            Log.e(TAG, "onAdFailedToLoad: Secondary\n$message")
                            builder.callback?.onAdFailedToLoad(message)
                            if (myNativeAdMain != null) {
                                builder.shimmerFrameLayout?.visibility = View.GONE
                            }
                            loadNativeBackup(false)
                        }
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        MainScope().launch {
                            Log.d(TAG, "onAdClicked: Secondary")
                            builder.callback?.onAdClicked()
                        }
                    }
                }, remoteKeySecondary)
        }
    }

    private fun showNativeMain(nativeAd: NativeAd?) {
        myNativeAdMain = nativeAd
        Admob.getInstance().populateNativeAdView(nativeAd, builder.nativeAdViewMain)
        builder.nativeAdViewMain?.visibility = View.VISIBLE
        if (builder.nativeAdViewSecondary != null) builder.nativeAdViewSecondary?.visibility =
            View.GONE
        builder.shimmerFrameLayout?.visibility = View.GONE
    }

    private fun showNativeSecondary(nativeAd: NativeAd?) {
        if (builder.nativeAdViewSecondary != null) {
            myNativeAdSecondary = nativeAd
            Admob.getInstance().populateNativeAdView(nativeAd, builder.nativeAdViewSecondary)
            builder.nativeAdViewSecondary?.visibility = View.VISIBLE
            builder.nativeAdViewMain?.visibility = View.GONE
            builder.shimmerFrameLayout?.visibility = View.GONE
        }
    }

    private fun handleImpressionNative() {
        if (myNativeAdMain != null) {
            MainScope().launch {
                delay(800)
                if (isShowNativeSecond) {
                    Log.d(TAG, "onAdImpression: Second - isShowNativeSecond = $isShowNativeSecond")
                    //show ads native second len dau
                    if (builder.nativeAdViewMain != null) builder.nativeAdViewMain?.visibility =
                        View.GONE
                    builder.nativeAdViewSecondary?.visibility = View.VISIBLE
                    builder.shimmerFrameLayout?.visibility = View.GONE
                } else {
                    Log.d(TAG, "onAdImpression: Second - isShowNativeSecond = $isShowNativeSecond")
                    //show ads native main len dau
                    if (builder.nativeAdViewSecondary != null) builder.nativeAdViewSecondary?.visibility =
                        View.GONE
                    builder.nativeAdViewMain?.visibility = View.VISIBLE
                    builder.shimmerFrameLayout?.visibility = View.GONE
                }
            }
        }
    }

    private var isFailedMain = false
    private var isFailedSecondary = false

    private fun loadNativeBackup(isMainNative: Boolean) {
        Admob.getInstance()
            .loadNativeAdsBackup(context, builder.listIdAdBackup, object : NativeCallback() {
                override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                    super.onNativeAdLoaded(nativeAd)
                    MainScope().launch {
                        Log.d(TAG, "onNativeAdLoaded: Backup $isMainNative")
                        if (isMainNative) {
                            showNativeMain(nativeAd)
                        } else {
                            showNativeSecondary(nativeAd)
                        }
                    }
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    MainScope().launch {
                        Log.d(TAG, "onAdImpression: Backup $isMainNative")
                        startReloadNative()
                    }
                }

                override fun onAdFailedToLoad(message: String?) {
                    super.onAdFailedToLoad(message)
                    MainScope().launch {
                        Log.e(TAG, "onAdFailedToLoad: Backup ")
                        if (isMainNative) {
                            isFailedMain = true
                        } else {
                            isFailedSecondary = true
                        }
                        if (isFailedMain && isFailedSecondary && builder.shimmerFrameLayout != null) {
                            builder.shimmerFrameLayout?.visibility = View.GONE
                        }
                        startReloadNative()
                    }
                }
            }, remoteKey)
    }

    fun setIntervalReloadNative(intervalReloadNative: Long) {
        if (intervalReloadNative > 0) {
            this.intervalReloadNative = intervalReloadNative
            countDownTimer = object : CountDownTimer(this.intervalReloadNative, 1000) {
                override fun onTick(l: Long) {
                }

                override fun onFinish() {
                    isTimerRunning = false
                    loadNativeFloor(builder.maxRequestReload)
                }
            }
        }
    }

    fun cancelAutoReloadNative() {
        if (countDownTimer != null) {
            countDownTimer?.cancel()
        }
    }

    private fun startReloadNative() {
        Log.d(
            TAG, ("startReloadNative: " + (countDownTimer != null)
                    + " && " + (this.lifecycleOwner.lifecycle.currentState)
                    + " && " + !isTimerRunning)
        )
        if (countDownTimer != null && this.lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED && !isTimerRunning) {
            Log.d(TAG, "startReloadNative: ok")
            isTimerRunning = true
            countDownTimer?.cancel()
            countDownTimer?.start()
        }
    }

    fun setReloadAds() {
        isReloadAds = true
    }

    fun reloadAdNow() {
        loadNativeFloor(builder.maxRequestReload)
    }

    fun setAlwaysReloadOnResume(isAlwaysReloadOnResume: Boolean) {
        this.isAlwaysReloadOnResume = isAlwaysReloadOnResume
    }

    fun setRemoteKeyAdNativeDisplayOrder(remoteKeyAdNativeDisplayOrder: String?) {
        this.remoteKeyAdNativeDisplayOrder = remoteKeyAdNativeDisplayOrder
    }

    companion object {
        private const val TAG = "NativeManager"
    }
}
