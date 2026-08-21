package com.amazic.library.ads.splash_ads.older_version

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import com.amazic.library.Utils.EventTrackingHelper
import com.amazic.library.Utils.IDRemoteConfigHelper
import com.amazic.library.Utils.NetworkUtil
import com.amazic.library.Utils.RemoteConfigHelper
import com.amazic.library.Utils.SharePreferenceHelper
import com.amazic.library.ads.admob.Admob
import com.amazic.library.ads.admob.AdmobApi
import com.amazic.library.ads.app_open_ads.AppOpenManager
import com.amazic.library.ads.banner_ads.BannerBuilder
import com.amazic.library.ads.banner_ads.BannerManager
import com.amazic.library.ads.callback.BannerCallback
import com.amazic.library.ads.callback.InterCallback
import com.amazic.library.ads.splash_ads.AdmobAdsConfig
import com.amazic.library.organic.TechManager
import com.amazic.library.ump.AdsConsentManager
import com.amazic.mylibrary.R
import com.google.android.gms.ads.interstitial.InterstitialAd
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.Locale
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.round

class AsyncSplashOld {

    private val TAG = "AsyncSplash"
    private val config = AdmobAdsConfig()

    private var mActivity: AppCompatActivity? = null
    private var frAdsBannerSplash: FrameLayout? = null
    var onPrepareLoadInterOpenSplashAds: (() -> Unit)? = null

    // Timing captured for the "AsyncSplash_doneInit" analytics event.
    private var timeInitRemoteConfig = 0L
    private var timeInitAdsConsentManager = 0L


    private val remoteConfigTimeoutHandler = Handler(Looper.getMainLooper())
    private var pendingRemoteConfigTimeoutRunnable: Runnable? = null
    private var timeOutJob: Job? = null

    companion object {
        private const val MAX_EVENT_NAME_LENGTH = 40
        private const val DEFAULT_EVENT_NAME = "event_unknown"
        private const val PREF_REMOTE_FETCHED_FLAG = "remote_fetched_once"
        private const val PREF_REMOTE_FILL = "remote_fill"
        @Volatile
        private var INSTANCE: AsyncSplashOld? = null

        /** Thread-safe (double-checked locking) singleton accessor. */
        fun getInstance(): AsyncSplashOld =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AsyncSplashOld().also { INSTANCE = it }
            }
    }

    // region Public API - lifecycle

    fun init(
        activity: AppCompatActivity,
        interCallback: InterCallback,
        adjustKey: String,
        appId: String,
        jsonIdAdsDefault: String
    ) {
        config.clear()
        frAdsBannerSplash = null
        onPrepareLoadInterOpenSplashAds = null
        mActivity = activity
        EventTrackingHelper.getInstance(activity).logEvent("${TAG}_INIT")
        config.timeStep1 = System.currentTimeMillis()
        config.timeLastStep = System.currentTimeMillis()
        config.adjustKey = adjustKey
        config.jsonIdAdsDefault = jsonIdAdsDefault
        config.appId = appId
        config.interCallback = interCallback
        IDRemoteConfigHelper.setUpDefaultValue(activity.applicationContext, config.jsonIdAdsDefault)
    }

    fun handleAsync(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        lifecycleCoroutineScope: LifecycleCoroutineScope,
        onNoInternetAction: () -> Unit,
        onAsyncSplashDone: () -> Unit
    ) {
        logEventStep("handleAsync")
        Admob.getInstance().timeStart = System.currentTimeMillis()
        config.timeStartSplash = System.currentTimeMillis()

        launchTimeoutWatcher(lifecycleCoroutineScope, context)
        AdmobApi.getInstance().init(context.applicationContext)
        if (NetworkUtil.isNetworkActive(mActivity)) {
            logEventStep("AsyncInternet")
            EventTrackingHelper.getInstance(mActivity).logEvent("splash_have_internet_original")
            lifecycleCoroutineScope.launch {
                runAsyncInitAndShowAds(lifecycleOwner, lifecycleCoroutineScope, onAsyncSplashDone)
            }
        } else {
            logEventStep("AsyncNoInternet")
            if (!config.isShowAdsSplash && !config.isTimeout) {
                onNoInternetAction.invoke()
                config.isNoInternetAction = true
            }
        }
    }

    fun checkShowSplashWhenFail() { // Call on resume of splash screen (reshow splash ads when show fails)
        AdsSplashOld.getInstance().onCheckShowSplashWhenFail(mActivity, callbackInternSplash(config.interCallback))
    }

    fun turnOffSomeRemoteKeys(activity: Context?) {
        config.listTurnOffRemoteKeys.forEach {
            Log.d(TAG, "turnOffSomeRemoteKeys: $it")
            RemoteConfigHelper.getInstance().set_config(activity, it, false)
        }
    }

    // endregion

    // region Public API - simple config setters/getters
    // These are thin pass-throughs onto AdmobAdsConfig; kept as-is (same names/signatures)
    // so existing call sites don't need to change.

    fun setAppPkg(
        appPackageName: String,
    ) {
        config.appPkg = appPackageName
    }

    fun setKeyIntervalBetweenInterstitial(keyIntervalBetweenInterstitial: String) {
        config.keyIntervalBetweenInterstitial = keyIntervalBetweenInterstitial
    }

    fun setKeyIntervalInterstitialFromStart(keyIntervalInterstitialFromStart: String) {
        config.keyIntervalInterstitialFromStart = keyIntervalInterstitialFromStart
    }

    fun setTimeOutCallIdRemoteConfig(timeOutCallIdRemoteConfig: Long) {
        config.timeOutCallIdRemoteConfig = timeOutCallIdRemoteConfig
    }

    fun getLoadAndShowIdInterAdSplashAsync(): Boolean = config.loadAndShowIdInterAdSplashAsync

    fun setLoadAndShowIdInterAdSplashAsync() {
        config.loadAndShowIdInterAdSplashAsync = true
    }

    fun setUrlCheckInternetSpeed(urlCheckInternetSpeed: String) {
        config.urlCheckInternetSpeed = urlCheckInternetSpeed
    }

    fun setTimeSplashCheck() {
        config.timeSplashCheck = System.currentTimeMillis()
    }

    fun getTimeSplashCheck(): Long = config.timeSplashCheck

    fun setOnPrepareLoadInterOpenSplashAds(onPrepareLoadInterOpenSplashAds: () -> Unit) {
        this.onPrepareLoadInterOpenSplashAds = onPrepareLoadInterOpenSplashAds
    }

    fun setUseAppUpdateManager() {
        config.isUseAppUpdateManager = true
    }

    fun setKeyAdsInterSplash(keyAdsInterSplash: String) {
        config.keyAdsInterSplash = keyAdsInterSplash
    }

    fun setKeyAdsOpenSplash(keyAdsOpenSplash: String) {
        config.keyAdsOpenSplash = keyAdsOpenSplash
    }

    fun setKeyAdsOpenResume(keyAdsOpenResume: String) {
        config.keyAdsOpenResume = keyAdsOpenResume
    }

    fun getKeyAdsOpenResume(): String = config.keyAdsOpenResume

    fun setKeyNativeAfterInterSplash(key: String) {
        config.keyNativeAfterInterSplash = key
    }

    fun setKeyNativeFullMetaSplash(key: String) {
        config.keyNativeFullMetaSplash = key
    }

    fun getKeyNativeFullMetaSplash(): String = config.keyNativeFullMetaSplash

    fun setKeyNativeFullAdmobSplash(key: String) {
        config.keyNativeFullAdmobSplash = key
    }

    fun getKeyNativeFullAdmobSplash(): String = config.keyNativeFullAdmobSplash

    fun setIdNativeMetaSplash(id: String) {
        config.idNativeMetaSplash = id
    }

    fun getIdNativeMetaSplash(): String = config.idNativeMetaSplash

    fun setNumberNativeFullShowSplash(count: Int) {
        config.numberNativeFullShowSplash = count
    }

    fun getNumberNativeFullShowSplash(): Int = config.numberNativeFullShowSplash

    fun setAsyncSplashAds() { // Show splash ads without waiting for anything
        config.isAsyncSplashAds = true
    }

    fun setPreloadResumeAds(isPreloadResumeAds: Boolean) {
        // Whether resume ads can be preloaded (set false when you need to load-and-show resume ads directly).
        config.isPreloadResumeAds = isPreloadResumeAds
    }

    fun getPreloadResumeAds(): Boolean = config.isPreloadResumeAds

    fun setKeyNumberPreloading(keyNumber: String) {
        config.numberPreloading =
            RemoteConfigHelper.getInstance().get_config_long(mActivity, keyNumber).toInt()
    }

    fun getNumberPreloading(): Int = config.numberPreloading

    fun setNumberPreloadingSplash(number: Int) {
        config.numberPreloadingSplash = number
    }

    fun getNumberPreloadingSplash(): Int = config.numberPreloadingSplash

    fun setUseAdPreloading(isUsePreLoading: Boolean) {
        config.isUseAdPreloading = isUsePreLoading
    }

    fun getUseAdPreloading(): Boolean = config.isUseAdPreloading

    fun setShowNativeAfterInter(isShowNativeAfterInter: Boolean) {
        config.isShowNativeAfterInter = isShowNativeAfterInter
    }

    fun getShowNativeAfterInter(): Boolean = config.isShowNativeAfterInter

    fun setUseNativeSplashMeta(isUse: Boolean) {
        config.isUseNativeSplashMeta = isUse
    }

    fun getIsUseNativeSplashMeta(): Boolean = config.isUseNativeSplashMeta

    fun setUseNativeFullSplash(isUse: Boolean) {
        config.isUseNativeFullSplash = isUse
    }

    fun getUseNativeFullSplash(): Boolean = config.isUseNativeFullSplash

    fun setUseCacheDataCallSplash(isUse: Boolean) {
        config.isUseCacheDataCallSplash = isUse
    }

    fun getUseCacheDataCallSplash(): Boolean = config.isUseCacheDataCallSplash

    fun setUseDetectionVPNOrEmulator(isUse: Boolean) {
        config.isUseDetectionVPNOrEmulator = isUse
    }

    fun getUseDetectionVPNOrEmulator(): Boolean = config.isUseDetectionVPNOrEmulator

    fun setUseNativeFullSplashAdmobWhenMetaFail(isUse: Boolean) {
        config.isUseNativeFullSplashAdmobWhenMetaFail = isUse
    }

    fun getUseNativeFullSplashAdmobWhenMetaFail(): Boolean = config.isUseNativeFullSplashAdmobWhenMetaFail

    fun setUseIdAdsFromRemoteConfig(remoteKeyIdAdsServer: String) {
        // Use id ads from remote config or not (remote key: id_ads)
        config.setUseIdAdsFromRemoteConfig(remoteKeyIdAdsServer)
    }

    fun setTimeOutCallApi(timeOutCallApi: Int) { // Timeout for calling id ads from server
        config.timeOutCallApi = timeOutCallApi
    }

    fun getUserTechManagerOrDetectTestAd(): String = config.useTechManagerOrDetectTestAd

    fun setUseTechManager() { // Use TechManager (Organic) or not
        config.useTechManagerOrDetectTestAd = AdmobAdsConfig.TECH_MANAGER
    }

    fun setUseDetectTestAd() {
        config.useTechManagerOrDetectTestAd = AdmobAdsConfig.DETECT_TEST_AD
    }

    fun setLoopAdsSplash() { // Load loop splash ads on load failure if elapsed time < 8s
        config.isLoopAdsSplash = true
    }

    fun getShowAdsSplash(): Boolean = config.isShowAdsSplash

    fun getTimeout(): Boolean = config.isTimeout

    fun getNoInternetAction(): Boolean = config.isNoInternetAction

    fun getTimeStartSplash(): Long = config.timeStartSplash

    fun setTimeOutSplash(timeOutSplash: Long) {
        config.timeOutSplash = timeOutSplash
    }

    fun setTimeOutInitAdmob(timeOutInitAdmob: Long) {
        config.timeOutInitAdmob = timeOutInitAdmob
    }

    fun setDebug(isDebug: Boolean) { // Used by TechManager / DetectTestAd
        config.isDebug = isDebug
    }

    fun getDebug(): Boolean = config.isDebug

    fun getInitResumeAdsType(): String = config.initWelcomeBack

    fun setShowBannerSplash(
        frAdsBannerSplash: FrameLayout,
        listIdBannerSplash: MutableList<String>,
        adsKey: String
    ) {
        config.isShowBannerSplash = true
        this.frAdsBannerSplash = frAdsBannerSplash
        config.listIdBannerSplash.clear()
        config.listIdBannerSplash.addAll(listIdBannerSplash)
        config.adsKey = adsKey
    }

    fun setListTurnOffRemoteKeys(listTurnOffRemoteKeys: MutableList<String>) {
        config.listTurnOffRemoteKeys.clear()
        config.listTurnOffRemoteKeys.addAll(listTurnOffRemoteKeys)
    }

    // endregion

    // region Event logging helpers

    fun normalizeFirebaseEventName(input: String): String {
        if (input.isBlank()) return DEFAULT_EVENT_NAME

        var name = input.trim().lowercase()
        name = name.replace(Regex("[^a-z0-9_]"), "_")

        if (name.firstOrNull()?.isLetter() != true) {
            name = "e_$name"
        }
        if (name.length > MAX_EVENT_NAME_LENGTH) {
            name = name.take(MAX_EVENT_NAME_LENGTH)
        }
        return name.ifBlank { DEFAULT_EVENT_NAME }
    }

    private fun logEventStep(step: String) {
        val bundle = Bundle().apply {
            putString("time_between_step", formatStepTime(System.currentTimeMillis() - config.timeLastStep))
            putString("time_to_step", formatStepTime(System.currentTimeMillis() - config.timeStep1))
            putString("isTimeout", config.isTimeout.toString())
        }
        EventTrackingHelper.getInstance(mActivity).logEventWithMultipleParams(
            normalizeFirebaseEventName("AsyncSplash_$step"),
            bundle
        )
        config.timeLastStep = System.currentTimeMillis()
    }

    private fun formatStepTime(timeMs: Long): String {
        val seconds = timeMs / 1000.0

        val step = if (seconds < 10) 0.5 else 5.0
        val rounded = round(seconds / step) * step

        return String.format(Locale.US, "%.1f", rounded)
    }
    // region Timeout watcher

    /** Watches for the overall splash timeout and fires [InterCallback.onNextAction] if nothing showed in time. */
    private fun launchTimeoutWatcher(lifecycleCoroutineScope: LifecycleCoroutineScope, context: Context) {
        timeOutJob = lifecycleCoroutineScope.launch {
            delay(config.timeOutSplash)
            Log.d(TAG, "Timeout check ${config.isShowAdsSplash} ${config.isNoInternetAction}")
            logEventStep("AsyncTimeout")

            val bundleEvent = Bundle().apply {
                putString("isTimeout", "${config.isTimeout}")
                putString("isNoInternetAction", "${config.isNoInternetAction}")
            }
            EventTrackingHelper.getInstance(mActivity).logEventWithMultipleParams(
                normalizeFirebaseEventName("AsyncSplash_VAsyncTimeout"),
                bundleEvent
            )

            if (!config.isShowAdsSplash && !config.isNoInternetAction) {
                logTimeoutSplashNextScreenEvent(context)
                incrementSplashOpenCount()
                turnOffRemoteKeysIfTech(mActivity)
                config.interCallback?.onNextAction()
                Log.d(TAG, "Timeout Splash.")
                config.isTimeout = true
            }
        }
    }

    private fun logTimeoutSplashNextScreenEvent(context: Context) {
        val bundle = Bundle().apply {
            putString(
                "timeout_splash_next_screen_detail",
                "${config.initAdmobApi}_${config.initRemoteConfig}_${config.initAdsConsentManager}_" +
                        "${config.initBilling}_${config.initTechManager}"
            )
            putBoolean("isInitAdmob", Admob.getInstance().isInitAdmobDone)
            putString("initAdmobApi", config.initAdmobApi.toString())
            putString("initRemoteConfig", config.initRemoteConfig.toString())
            putString("initAdsConsentManager", config.initAdsConsentManager.toString())
            putString("initBilling", config.initBilling.toString())
            putString("initTechManager", config.initTechManager.toString())
        }
        EventTrackingHelper.getInstance(context).logEventWithMultipleParams("timeout_splash_next_screen", bundle)
    }

    private fun incrementSplashOpenCount() {
        SharePreferenceHelper.setInt(
            mActivity,
            EventTrackingHelper.splash_open,
            SharePreferenceHelper.getInt(mActivity, EventTrackingHelper.splash_open, 1) + 1
        )
    }

    /** Turns off the configured remote keys if the current user is detected as tech/test-ad traffic. */
    private fun turnOffRemoteKeysIfTech(activity: Context?) {
        val isTechTraffic = when (config.useTechManagerOrDetectTestAd) {
            AdmobAdsConfig. TECH_MANAGER -> config.isTech
            AdmobAdsConfig.DETECT_TEST_AD -> TechManager.getInstance().isTech(activity)
            else -> false
        }
        if (isTechTraffic && !config.isDebug) {
            turnOffSomeRemoteKeys(activity)
        }
    }

    // endregion

    // region Main async init + ad show flow

    private suspend fun runAsyncInitAndShowAds(
        lifecycleOwner: LifecycleOwner,
        lifecycleCoroutineScope: LifecycleCoroutineScope,
        onAsyncSplashDone: () -> Unit
    ) {
        logEventStep("StartAsyncInit")
        val asyncRemoteConfig = lifecycleCoroutineScope.async { initRemoteConfig(mActivity, lifecycleCoroutineScope) }
        val asyncUMP = lifecycleCoroutineScope.async { initAdsConsentManager(mActivity) }
        try {
            // Wait for banner splash prerequisites (banner splash uses a fixed id, so we
            // don't need the ads-server API call to reduce splash load time).
            if (!config.isAsyncSplashAds) {
                awaitAll(asyncRemoteConfig, asyncUMP)
                turnOffRemoteKeysIfTech(mActivity)
            } else {
                awaitAll(asyncUMP)
            }
            initWelcomeBack(activity = mActivity)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            loadBannerSplash(
                mActivity,
                lifecycleOwner,
                frAdsBannerSplash,
                config.listIdBannerSplash,
                config.adsKey
            )

            logEventStep("DoneAsyncInit")
            onPrepareLoadInterOpenSplashAds?.invoke()

            logEventStep("StartAdSplash")
            var totalTimeWainInit = 0
            var isUseAdPreloading = config.isUseAdPreloading

            while (!Admob.getInstance().isInitAdmobDone && totalTimeWainInit < config.timeOutInitAdmob) {
                delay(200)
                totalTimeWainInit += 200
            }
            if (!Admob.getInstance().isInitAdmobDone) {
                isUseAdPreloading = false
            }
            showAdsSplash(mActivity, config.interCallback, isUseAdPreloading)

            if (config.isAsyncSplashAds) {
                awaitAll(asyncRemoteConfig)
                turnOffRemoteKeysIfTech(mActivity)
            }

            applyRandomSplashDialogAnimations()
            onAsyncSplashDone.invoke()

            loadAdPreloadResume()
        }
    }

    private fun applyRandomSplashDialogAnimations() {
        val listAnim = arrayListOf(
            R.raw.ads_1, R.raw.ads_2, R.raw.ads_3, R.raw.ads_4, R.raw.ads_5,
            R.raw.ads_6, R.raw.ads_7, R.raw.ads_8, R.raw.ads_9, R.raw.ads_10,
        )
        Admob.getInstance().setCustomAnimationDialog(listAnim)
        AppOpenManager.getInstance().setCustomAnimationDialog(listAnim)
    }

    private fun showAdsSplash(activity: AppCompatActivity?, interCallback: InterCallback?, isUseAdPreloading: Boolean) {
        val bundle = Bundle().apply {
            putString("isTimeout", "${config.isTimeout}")
            putString("isNoInternetAction", "${config.isNoInternetAction}")
        }
        EventTrackingHelper.getInstance(activity).logEventWithMultipleParams(
            normalizeFirebaseEventName("AsyncSplash_showAdsSplash"), bundle
        )
        if (!config.isTimeout && !config.isNoInternetAction) {
            val time = (System.currentTimeMillis() - config.timeStartSplash) / 1000
            Log.d(TAG, "----------")
            Log.d(TAG, "showAdsSplash: Time show Ads = $time")
            logEventStep("start_call_splash")
            if (isUseAdPreloading)
                startCallPreloadSplash(activity, interCallback)
            else
                startCallNormalSplash(activity, interCallback)
            Log.d(TAG, "showAdsSplash.")
        } else {
            val bundle = Bundle()
            bundle.putString("failed_message", "isTimeout_${config.isTimeout}_noInternetAction_${config.isNoInternetAction}")
            EventTrackingHelper.getInstance(activity).logEventWithMultipleParams("start_call_splash_failed", bundle)
        }
    }

    private fun startCallPreloadSplash(activity: AppCompatActivity?, interCallback: InterCallback?) {
        AdsSplashOld.getInstance().loadAndShowInterAdPreloadingSplash(
            activity,
            AdmobApi.getInstance().getListIDByName(config.keyAdsInterSplash),
            callbackInternSplash(interCallback),
            config.keyNativeAfterInterSplash,
            config.keyNativeAfterInterSplash,
            config.timeStep1,
            config.timeLastStep
        )
    }

    private fun startCallNormalSplash(activity: AppCompatActivity?, interCallback: InterCallback?) {
        if (config.isLoopAdsSplash) {
            Log.d(TAG, "Show Ads 1")
            AdsSplashOld.getInstance().loadAndShowInterAdSplashLoop(
                activity,
                AdmobApi.getInstance().getListIDByName(config.keyAdsInterSplash),
                callbackInternSplash(interCallback),
                config.timeStep1,
                config.timeLastStep
            )
        } else {
            if (!config.loadAndShowIdInterAdSplashAsync) {
                Log.d(TAG, "Show Ads 2")
                AdsSplashOld.getInstance().loadAndShowInterAdSplashDelay(
                    activity,
                    AdmobApi.getInstance().getListIDByName(config.keyAdsInterSplash),
                    callbackInternSplash(interCallback),
                    config.keyNativeAfterInterSplash, config.keyNativeAfterInterSplash,
                    config.timeStep1,
                    config.timeLastStep
                )
            } else {
                Log.d(TAG, "Show Ads 3")
                AdsSplashOld.getInstance().loadAndShowIdInterAdSplashAsync(
                    activity,
                    AdmobApi.getInstance().getListIDByName(config.keyAdsInterSplash),
                    callbackInternSplash(interCallback),
                    config.timeStep1,
                    config.timeLastStep
                )
            }
        }
    }

    private fun callbackInternSplash(interCallback: InterCallback?): InterCallback = object : InterCallback() {
        override fun onAdLoaded(interstitialAd: InterstitialAd?) {
            super.onAdLoaded(interstitialAd)
            interCallback?.onAdLoaded(interstitialAd)
        }

        override fun onAdFailedToLoad() {
            super.onAdFailedToLoad()
            interCallback?.onAdFailedToLoad()
        }

        override fun onAdClicked() {
            super.onAdClicked()
            interCallback?.onAdClicked()
        }

        override fun onAdDismissedFullScreenContent() {
            super.onAdDismissedFullScreenContent()
            interCallback?.onAdDismissedFullScreenContent()
        }

        override fun onAdFailedToShowFullScreenContent() {
            super.onAdFailedToShowFullScreenContent()
            interCallback?.onAdFailedToShowFullScreenContent()
        }

        override fun onAdImpression() {
            super.onAdImpression()
            interCallback?.onAdImpression()
            timeOutJob?.cancel()
            timeOutJob = null
        }

        override fun onAdShowedFullScreenContent() {
            super.onAdShowedFullScreenContent()
            timeOutJob?.cancel()
            timeOutJob = null
            interCallback?.onAdShowedFullScreenContent()
        }

        override fun onNextAction() {
            super.onNextAction()
            timeOutJob?.cancel()
            timeOutJob = null
            interCallback?.onNextAction()
        }
    }

    // endregion

    // region Banner splash

    private fun loadBannerSplash(
        activity: AppCompatActivity?,
        lifecycleOwner: LifecycleOwner,
        frAdsBanner: FrameLayout?,
        listIdBannerSplash: MutableList<String>,
        adsKey: String
    ) {
        if (!config.isShowBannerSplash) {
            frAdsBanner?.visibility = View.GONE
            return
        }

        Log.d(TAG, "loadBannerSplash.")
        if (config.useTechManagerOrDetectTestAd == AdmobAdsConfig.DETECT_TEST_AD) {
            TechManager.getInstance().detectedTech(activity, false)
        }
        frAdsBanner?.visibility = View.VISIBLE

        val bannerBuilder = BannerBuilder(frAdsBanner).apply {
            setListIdAdMain(listIdBannerSplash)
            callBack = object : BannerCallback() {
                override fun onAdImpression() {
                    super.onAdImpression()
                    if (config.useTechManagerOrDetectTestAd == AdmobAdsConfig.DETECT_TEST_AD &&
                        TechManager.getInstance().isTech(activity) && !config.isDebug
                    ) {
                        turnOffSomeRemoteKeys(activity)
                    }
                    Log.d(TAG, "showBannerSplash.")
                }

                override fun onAdFailedToLoad() {
                    super.onAdFailedToLoad()
                    frAdsBanner?.visibility = View.GONE
                    Log.d(TAG, "loadFailBannerSplash.")
                }
            }
        }
        activity?.let { BannerManager(it, lifecycleOwner, bannerBuilder, adsKey) }
    }

    // endregion

    // region Billing (currently a no-op)

    // Real IAP wiring intentionally left as a stub: this build doesn't use billing.
    // Re-enable by calling IAPManager.getInstance().initBilling(...) here and
    // resuming the continuation from its callback instead of resuming immediately.
    private suspend fun initBilling() = suspendCoroutine<Unit> { continuation ->
        continuation.resume(Unit)
        config.initBilling = true
        Log.d(TAG, "Not use billing.")
    }

    // endregion

    // region Remote config init

    private suspend fun initRemoteConfig(
        activity: AppCompatActivity?,
        lifecycleCoroutineScope: LifecycleCoroutineScope
    ) = suspendCoroutine<Unit> { continuation ->
        timeInitRemoteConfig = System.currentTimeMillis()

        if (config.isUseAppUpdateManager) {
            continuation.resume(Unit)
            return@suspendCoroutine
        }

        System.currentTimeMillis()
        EventTrackingHelper.getInstance(activity).logEvent("initRemoteConfig")

        val prefs = activity?.getSharedPreferences(PREF_REMOTE_FILL, Context.MODE_PRIVATE)
        val hasBeenFetchedBefore = prefs?.getBoolean(PREF_REMOTE_FETCHED_FLAG, false) ?: false

        if (activity != null) scheduleRemoteConfigIdTimeoutFallback(activity)

        Log.d(
            TAG,
            "check initRemoteConfig: hasBeenFetchedBefore=$hasBeenFetchedBefore, " +
                    "isUseCacheDataCallSplash=${config.isUseCacheDataCallSplash}"
        )

        if (hasBeenFetchedBefore && config.isUseCacheDataCallSplash) {
            initRemoteConfigFromCache(activity, lifecycleCoroutineScope, continuation)
        } else {
            initRemoteConfigFromNetwork(activity, prefs, continuation)
        }
    }

    /** Fires once, as a fallback, if we're still waiting on remote config after [AdmobAdsConfig.timeOutCallIdRemoteConfig]. */
    private fun scheduleRemoteConfigIdTimeoutFallback(activity: AppCompatActivity) {
        cancelPendingRemoteConfigTimeout()
        val activityRef = WeakReference(activity)
        val runnable = Runnable {
            if (config.isUseIdAdsFromRemoteConfig && !config.isSetId) {
                val act = activityRef.get()
//                AdmobApi.getInstance().jsonIdAdsDefault = config.jsonIdAdsDefault
//                AdmobApi.getInstance().convertJsonIdAdsDefaultToList(config.jsonIdAdsDefault)
                config.isSetId = true
                Log.d(TAG, "Timeout Remote Config: Id ads size = ${AdmobApi.getInstance().listAdsSize}")
                EventTrackingHelper.getInstance(act).logEvent("timeout_call_id_remote_config")
                initWelcomeBack(act) // 17.09.2025
            }
        }
        pendingRemoteConfigTimeoutRunnable = runnable
        remoteConfigTimeoutHandler.postDelayed(runnable, config.timeOutCallIdRemoteConfig)
    }

    private fun cancelPendingRemoteConfigTimeout() {
        pendingRemoteConfigTimeoutRunnable?.let { remoteConfigTimeoutHandler.removeCallbacks(it) }
        pendingRemoteConfigTimeoutRunnable = null
    }

    private fun initRemoteConfigFromCache(
        activity: AppCompatActivity?,
        lifecycleCoroutineScope: LifecycleCoroutineScope,
        continuation: Continuation<Unit>
    ) {
        Log.d(TAG, "initRemoteConfig: using SharedPreferences cache")

        applyGeneralRemoteConfig(
            activity,
            intervalBetweenKey = config.keyIntervalBetweenInterstitial,
            intervalFromStartKey = config.keyIntervalInterstitialFromStart
        )

        if (config.isUseIdAdsFromRemoteConfig && !config.isSetId && activity != null) {
            val jsonFromSP = RemoteConfigHelper.getInstance().get_config_string(activity, RemoteConfigHelper.id_ads)
            applyIdAdsJson(activity, jsonFromSP, successEvent = "set_id_remote_config")
            initWelcomeBack(activity)
            EventTrackingHelper.getInstance(activity).logEvent("set_id_default_case_fail_remote")
        }

        config.initRemoteConfig = true
        cancelPendingRemoteConfigTimeout()
        // Refresh in the background so the next session's cache is up to date.
        // Uses the caller's lifecycle scope so this is cancelled with the splash screen
        // instead of leaking an unscoped coroutine.
        lifecycleCoroutineScope.launch {
            RemoteConfigHelper.getInstance().fetchAllKeysAndTypes(activity) {
                Log.d(TAG, "initRemoteConfig: background fetch done, isSuccess=$it")
            }
        }
        EventTrackingHelper.getInstance(activity).logEventWithAParam(
            "done_init_remote_cache",
            "time_between_step",
            String.format(Locale.US, "%.1f", (System.currentTimeMillis() - timeInitRemoteConfig) / 1000f)
        )
        continuation.resume(Unit) // Resume immediately; cache is trusted.
    }

    private fun initRemoteConfigFromNetwork(
        activity: AppCompatActivity?,
        prefs: SharedPreferences?,
        continuation: Continuation<Unit>
    ) {
        Log.d(TAG, "initRemoteConfig: first time, fetching from Firebase")
        val isResumed = false

        RemoteConfigHelper.getInstance().fetchAllKeysAndTypes(activity) { isSuccess ->
            if (isSuccess && config.isUseCacheDataCallSplash) {
                prefs?.edit()?.putBoolean(PREF_REMOTE_FETCHED_FLAG, true)?.apply()
            }

            if (config.isUseIdAdsFromRemoteConfig && !config.isSetId && activity != null) {
                val jsonFromRemote = RemoteConfigHelper.getInstance().get_config_string(activity, RemoteConfigHelper.id_ads)
                val gotIdFromRemote = jsonFromRemote.contains("app_id") && isSuccess
                applyIdAdsJson(
                    activity,
                    json = if (gotIdFromRemote) jsonFromRemote else config.jsonIdAdsDefault,
                    successEvent = if (gotIdFromRemote) "set_id_remote_config" else "set_id_default_case_fail_remote"
                )
                initWelcomeBack(activity) // 17.09.2025
            }

            applyGeneralRemoteConfig(
                activity,
                intervalBetweenKey = RemoteConfigHelper.interval_between_interstitial,
                intervalFromStartKey = RemoteConfigHelper.interval_interstitial_from_start
            )

            if (!isResumed) {
                cancelPendingRemoteConfigTimeout()

                EventTrackingHelper.getInstance(activity).logEventWithAParam(
                    "done_init_remote_new",
                    "time_between_step",
                    String.format(Locale.US, "%.1f", (System.currentTimeMillis() - timeInitRemoteConfig) / 1000f)
                )
                config.initRemoteConfig = true
                continuation.resume(Unit)
            }
        }
    }

    /** Applies `show_all_ads` + interstitial interval remote config values onto [Admob]. */
    private fun applyGeneralRemoteConfig(activity: AppCompatActivity?, intervalBetweenKey: String, intervalFromStartKey: String) {
        Log.d(TAG, "show_all_ads = ${RemoteConfigHelper.getInstance().get_config(activity, RemoteConfigHelper.show_all_ads)}")
        Admob.getInstance().showAllAds = RemoteConfigHelper.getInstance().get_config(activity, RemoteConfigHelper.show_all_ads)
        Admob.getInstance().setTimeInterval(
            RemoteConfigHelper.getInstance().get_config_long(activity, intervalBetweenKey) * 1000, true
        )
        Admob.getInstance().setTimeIntervalFromStart(
            RemoteConfigHelper.getInstance().get_config_long(activity, intervalFromStartKey) * 1000
        )
    }

    /** Sets [AdmobApi]'s ad-unit-id json from either the remote-config value or the app's built-in default. */
    private fun applyIdAdsJson(activity: AppCompatActivity, json: String, successEvent: String) {
//        AdmobApi.getInstance().convertJsonIdAdsDefaultToList(json)
        IDRemoteConfigHelper.setUpDefaultValue(activity.applicationContext, json)
        config.isSetId = true
        Log.d(TAG, "Set id ads ($successEvent): Id ads size = ${AdmobApi.getInstance().listAdsSize}")
        EventTrackingHelper.getInstance(activity).logEvent(successEvent)
    }

    // endregion

    // region UMP / consent init

    private suspend fun initAdsConsentManager(activity: AppCompatActivity?) = suspendCoroutine<Unit> { continuation ->
        timeInitAdsConsentManager = System.currentTimeMillis()
        val adsConsentManager = AdsConsentManager(activity)
        val isResumed = false

        adsConsentManager.requestUMP { canInitAds ->
            if (isResumed) return@requestUMP

            if (canInitAds) {
                Admob.getInstance().initAdmob(activity) { /* no-op */ }
                activity?.let { AppOpenManager.getInstance().disableAppResumeWithActivity(it.javaClass) }
            }
            EventTrackingHelper.getInstance(activity).logEventWithAParam(
                "done_init_consent",
                "time_between_step",
                String.format(Locale.US, "%.1f", (System.currentTimeMillis() - timeInitAdsConsentManager) / 1000f)
            )
            config.initAdsConsentManager = true
            continuation.resume(Unit)
            Log.d(TAG, "initAdsConsentManager.")
        }
    }

    // endregion

    // region Welcome-back / app-open resume ads
    private fun initWelcomeBack(activity: AppCompatActivity?) {
        when (config.initWelcomeBack) {
            AdmobAdsConfig.WELCOME_BACK_NORMAL -> initWelcomeBackWith(
                activity, logTag = "normal",
                defaultKeyName = "open_resume",
                defaultIds = { AdmobApi.getInstance().listIDAppOpenResume }
            ) { listIdResume ->
                AppOpenManager.getInstance().init(activity, listIdResume)
            }

            AdmobAdsConfig.WELCOME_BACK_BELOW -> initWelcomeBackWith(
                activity, logTag = "below",
                defaultKeyName = "resume_wb",
                defaultIds = { AdmobApi.getInstance().getListIDByName(RemoteConfigHelper.resume_wb) }
            ) { listIdResume ->
                config.welcomeBackClass?.let {
                    AppOpenManager.getInstance().initWelcomeBackBelowAdsResume(activity, listIdResume, it)
                    AppOpenManager.getInstance().disableAppResumeWithActivity(it) // disable resume welcome back
                }
            }

            AdmobAdsConfig.WELCOME_BACK_ABOVE -> initWelcomeBackWith(
                activity, logTag = "above",
                defaultKeyName = "resume_wb",
                defaultIds = { AdmobApi.getInstance().getListIDByName(RemoteConfigHelper.resume_wb) }
            ) { listIdResume ->
                config.welcomeBackClass?.let {
                    AppOpenManager.getInstance().initWelcomeBackAboveAdsResume(activity, listIdResume, it)
                    AppOpenManager.getInstance().disableAppResumeWithActivity(it) // disable resume welcome back
                }
            }

            else -> initWelcomeBackWith(
                activity, logTag = "else",
                defaultKeyName = "open_resume",
                defaultIds = { AdmobApi.getInstance().listIDAppOpenResume }
            ) { listIdResume ->
                AppOpenManager.getInstance().init(activity, listIdResume)
            }
        }
    }

    private inline fun initWelcomeBackWith(
        activity: AppCompatActivity?,
        logTag: String,
        defaultKeyName: String,
        defaultIds: () -> List<String>,
        modeSpecificInit: (List<String>) -> Unit
    ) {
        val listIdResume = mutableListOf<String>()
        if (config.keyAdsOpenResume.isNotEmpty()) {
            listIdResume.addAll(AdmobApi.getInstance().getListIDByName(config.keyAdsOpenResume))
        } else {
            listIdResume.addAll(defaultIds())
            config.keyAdsOpenResume = defaultKeyName
        }

        if (listIdResume.isEmpty()) return

        if (!config.isUseAdPreloading) {
            Log.d(
                TAG,
                "APP Open Preload: $logTag - isUseAdPreloading=${config.isUseAdPreloading}, " +
                        "isPreloadResumeAds=${config.isPreloadResumeAds}"
            )
            if (config.isPreloadResumeAds) {
                AppOpenManager.getInstance().loadAdNotCheckRemote(activity, listIdResume, config.keyAdsOpenResume)
            }
        }

        modeSpecificInit(listIdResume)

        activity?.let {
            AppOpenManager.getInstance().disableAppResumeWithActivity(it.javaClass)
        } // disable resume splash
    }

    private fun loadAdPreloadResume() {
        if (!config.isUseAdPreloading) return

        val listIdResume = mutableListOf<String>()
        if (config.keyAdsOpenResume.isNotEmpty()) {
            listIdResume.addAll(AdmobApi.getInstance().getListIDByName(config.keyAdsOpenResume))
        } else {
            listIdResume.addAll(AdmobApi.getInstance().listIDAppOpenResume)
            config.keyAdsOpenResume = "open_resume"
        }

        if (listIdResume.isNotEmpty()) {
            Log.d(TAG, "APP Open Preload: start loadAdPreloadNotCheckRemote")
            AppOpenManager.getInstance().loadAdPreloadNotCheckRemote(mActivity, listIdResume, config.keyAdsOpenResume)
        }
    }

    // endregion
}