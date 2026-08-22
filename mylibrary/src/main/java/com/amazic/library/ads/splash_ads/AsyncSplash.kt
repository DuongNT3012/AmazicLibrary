package com.amazic.library.ads.splash_ads

import android.app.Activity
import android.os.Bundle
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
import com.amazic.library.ads.admob.Admob
import com.amazic.library.ads.admob.AdmobApi
import com.amazic.library.ads.app_open_ads.AppOpenManager
import com.amazic.library.ads.banner_ads.BannerBuilder
import com.amazic.library.ads.banner_ads.BannerManager
import com.amazic.library.ads.callback.BannerCallback
import com.amazic.library.ads.callback.InterCallback
import com.amazic.library.organic.TechManager
import com.amazic.library.ump.AdsConsentManager
import com.google.android.gms.ads.interstitial.InterstitialAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class AsyncSplash {

    private val TAG = "AsyncSplash"
    val config = AdmobAdsConfig.getInstance()
    private var mActivity: AppCompatActivity? = null
    private var frAdsBannerSplash: FrameLayout? = null
    var onPrepareLoadInterOpenSplashAds: (() -> Unit)? = null
    private var remoteKeyBanner: String = ""
    private var timeoutSplashJob: Job? = null
    private var isFailToShowInterSplash = false
    private var isStartingLoadSplash = false

    companion object {
        private const val MAX_EVENT_NAME_LENGTH = 40
        private const val DEFAULT_EVENT_NAME = "event_unknown"

        @Volatile
        private var INSTANCE: AsyncSplash? = null

        /** Thread-safe (double-checked locking) singleton accessor. */
        fun getInstance(): AsyncSplash =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AsyncSplash().also { INSTANCE = it }
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
        EventTrackingHelper.getInstance(activity).logEvent("${TAG}_INIT")
        config.clear()
        isStartingLoadSplash = false
        remoteKeyBanner = ""
        frAdsBannerSplash = null
        onPrepareLoadInterOpenSplashAds = null
        AdmobApi.getInstance().init(activity.applicationContext)
        IDRemoteConfigHelper.setUpDefaultValue(activity.applicationContext, jsonIdAdsDefault)
        mActivity = activity
        config.timeStep1 = System.currentTimeMillis()
        config.timeLastStep = System.currentTimeMillis()
        config.adjustKey = adjustKey
        config.jsonIdAdsDefault = jsonIdAdsDefault
        config.appId = appId
        config.interCallback = callbackInternSplash(interCallback)
    }

    private fun callbackInternSplash(interCallback: InterCallback): InterCallback {
        return object : InterCallback() {
            override fun onNextAction() {
                super.onNextAction()
                interCallback.onNextAction()
                isStartingLoadSplash = false
            }

            override fun onAdClicked() {
                super.onAdClicked()
                interCallback.onAdClicked()
                isStartingLoadSplash = false
            }

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                interCallback.onAdDismissedFullScreenContent()
                isStartingLoadSplash = false
            }

            override fun onAdFailedToLoad() {
                super.onAdFailedToLoad()
                interCallback.onAdFailedToLoad()
                isStartingLoadSplash = false
            }

            override fun onAdFailedToShowFullScreenContent() {
                super.onAdFailedToShowFullScreenContent()
                isFailToShowInterSplash = true
                interCallback.onAdFailedToShowFullScreenContent()
                isStartingLoadSplash = false
            }

            override fun onAdImpression() {
                super.onAdImpression()
                interCallback.onAdImpression()
                isStartingLoadSplash = false
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd?) {
                super.onAdLoaded(interstitialAd)
                interCallback.onAdLoaded(interstitialAd)
                isStartingLoadSplash = false
            }

            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
                isFailToShowInterSplash = false
                isStartingLoadSplash = false
                timeoutSplashJob?.cancel()
                interCallback.onAdShowedFullScreenContent()
            }

        }
    }

    fun handleAsync(
        lifecycleOwner: LifecycleOwner,
        lifecycleCoroutineScope: LifecycleCoroutineScope,
        onNoInternetAction: () -> Unit,
        onAsyncSplashDone: () -> Unit
    ) {
        isFailToShowInterSplash = false
        Admob.getInstance().timeStart = System.currentTimeMillis()
        config.timeStartSplash = System.currentTimeMillis()
        logEventStep("handleAsync")
        if (mActivity == null) {
            throw Exception("Activity is null")
        }
        if (!NetworkUtil.isNetworkActive(mActivity)) {
            logEventStep("NoInternet")
            onNoInternetAction.invoke()
            return
        }
        lifecycleCoroutineScope.launch {
            runAsyncInitAndShowAds(mActivity!!) {
                initWelcomeBack(mActivity)
                loadBannerSplash(
                    mActivity,
                    lifecycleOwner,
                    frAdsBannerSplash,
                    config.listIdBannerSplash,
                    remoteKeyBanner
                )
                onAsyncSplashDone()
            }
        }
    }

    private suspend fun runAsyncInitAndShowAds(
        activity: AppCompatActivity,
        onAsyncDoneRemoteConsent: () -> Unit
    ) = coroutineScope {
        logEventStep("StartAsyncInit")
        val remoteConfigJob = async { runCatching { initRemoteConfig(activity) } }
        val consentJob = async { runCatching { initAdsConsentManager(activity) } }
        launch {
            awaitAll(remoteConfigJob, consentJob)
            onAsyncDoneRemoteConsent()
        }

        consentJob.await()
        timeoutSplashJob?.cancel()
        timeoutSplashJob = runTimeOutSplash()
        logEventStep("DoneAsyncInit")
        logEventStep("StartWaitAdmobInit")
        val totalTimeWaitInit = waitingInitAdmob()
        logEventStep("DoneWaitAdmobInit", Bundle().apply { putString("checkInit", totalTimeWaitInit.toString()) })
        loadInterSplash(activity, config.isUseAdPreloading && Admob.getInstance().isInitAdmobDone)
    }

    private fun CoroutineScope.runTimeOutSplash(): Job = launch {
        config.isTimeout = false
        delay(config.timeOutSplash)
        if (mActivity == null) {
            logEventStep("TimeoutActivityNull")
            return@launch
        }
        logEventStep("Timeout")
        isFailToShowInterSplash = true
        if (isStartingLoadSplash)
            config.interCallback.onNextAction()
        else
            loadAndShowInterSplash(mActivity!!, config.isUseAdPreloading && Admob.getInstance().isInitAdmobDone)
        config.isTimeout = true
    }

    private suspend fun waitingInitAdmob(): Int {
        var totalTimeWaitInit1 = 0
        while (!Admob.getInstance().isInitAdmobDone && totalTimeWaitInit1 < config.timeOutInitAdmob) {
            delay(200)
            totalTimeWaitInit1 += 200
        }
        return totalTimeWaitInit1
    }

    private fun loadAndShowInterSplash(activity: AppCompatActivity, isUseAdPreloading: Boolean) {
        val adUnitId = IDRemoteConfigHelper.getID(activity, config.keyAdsInterSplash)
        if (adUnitId == null) {
            logEventStep(EventNameSplash.EVENT_LOAD_FAILED_SPLASH_ID_NULL)
            return
        }
        val numberPreloadSplash = RemoteConfigHelper.getInstance().get_config_long(activity, "number_ad_preload_splash")
        config.numberPreloadingSplash = numberPreloadSplash.toInt()
        if (config.isTimeout) {
            logEventStep(EventNameSplash.EVENT_SHOW_FAILED_SPLASH_TIMEOUT)
            return
        }
        isStartingLoadSplash = true
        logEventStep("StartLoadAndShowInter", Bundle().apply { putBoolean("isUseAdPreloading", isUseAdPreloading) })
        AdsSplash.getInstance()
            .loadAndShow(activity, adUnitId, config.keyAdsInterSplash, config.numberPreloadingSplash, config.interCallback, isUseAdPreloading)
    }


    private fun loadInterSplash(activity: AppCompatActivity, isUseAdPreloading: Boolean) {
        val adUnitId = IDRemoteConfigHelper.getID(activity, config.keyAdsInterSplash)
        if (adUnitId == null) {
            logEventStep(EventNameSplash.EVENT_LOAD_FAILED_SPLASH_ID_NULL)
            return
        }
        val numberPreloadSplash = RemoteConfigHelper.getInstance().get_config_long(activity, "number_ad_preload_splash")
        config.numberPreloadingSplash = numberPreloadSplash.toInt()
        if (config.isTimeout) {
            logEventStep(EventNameSplash.EVENT_SHOW_FAILED_SPLASH_TIMEOUT)
            return
        }
        isStartingLoadSplash = true
        logEventStep("StartLoadOnly", Bundle().apply { putBoolean("isUseAdPreloading", isUseAdPreloading) })
        AdsSplash.getInstance()
            .loadAd(activity, adUnitId, config.keyAdsInterSplash, config.numberPreloadingSplash, config.interCallback, isUseAdPreloading)
    }

    private suspend fun initAdsConsentManager(activity: AppCompatActivity?) = suspendCancellableCoroutine { continuation ->
        if (config.initAdsConsentManager) {
            continuation.resume(Unit)
            return@suspendCancellableCoroutine
        }
        val adsConsentManager = AdsConsentManager(activity)

        val startTime = System.currentTimeMillis()
        adsConsentManager.requestUMP { _ ->
            config.initAdsConsentManager = true
            EventTrackingHelper.getInstance(mActivity).logEventWithMultipleParams(
                normalizeFirebaseEventName("DoneInitConsent"),
                Bundle().apply { putString("time_between_step", formatStepTime(System.currentTimeMillis() - startTime)) }
            )
            continuation.resume(Unit)
        }
    }

    private suspend fun initRemoteConfig(activity: AppCompatActivity?) = suspendCancellableCoroutine { continuation ->
        val startTime = System.currentTimeMillis()
        RemoteConfigHelper.getInstance().fetchAllKeysAndTypes(activity) {
            config.initRemoteConfig = true
            EventTrackingHelper.getInstance(mActivity).logEventWithMultipleParams(
                normalizeFirebaseEventName("DoneInitRemoteConfig"),
                Bundle().apply { putString("time_between_step", formatStepTime(System.currentTimeMillis() - startTime)) }
            )
            continuation.resume(Unit)
        }
    }

    fun checkShowSplashWhenFail(activity: Activity) { // Call on resume of splash screen (reshow splash ads when show fails)
        val adUnitId = IDRemoteConfigHelper.getID(activity, config.keyAdsInterSplash)
        if (adUnitId == null) {
            logEventStep(EventNameSplash.EVENT_LOAD_FAILED_SPLASH_ID_NULL)
            return
        }
        if (isFailToShowInterSplash) AdsSplash.getInstance().showCacheInterSplash(mActivity, adUnitId, config.keyAdsInterSplash, config.interCallback)
    }

    // endregion

    // region Public API - simple config setters/getters
    // These are thin pass-throughs onto AdmobAdsConfig; kept as-is (same names/signatures)
    // so existing call sites don't need to change.

    fun setShowBannerSplash(
        frAdsBannerSplash: FrameLayout,
        listIdBannerSplash: MutableList<String>,
        adsKey: String
    ) {
        config.isShowBannerSplash = true
        this.frAdsBannerSplash = frAdsBannerSplash
        config.listIdBannerSplash.clear()
        config.listIdBannerSplash.addAll(listIdBannerSplash)
        remoteKeyBanner = adsKey
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

    fun logEventStep(tag: String, step: String, bundle: Bundle) {
        bundle.apply {
            putString("time_between_step", formatStepTime(System.currentTimeMillis() - config.timeLastStep))
            putString("time_to_step", formatStepTime(System.currentTimeMillis() - config.timeStep1))
            putString("isTimeout", config.isTimeout.toString())
        }
        EventTrackingHelper.getInstance(mActivity).logEventWithMultipleParams(
            normalizeFirebaseEventName("${tag}_$step"),
            bundle
        )
        config.timeLastStep = System.currentTimeMillis()
    }

    fun logEventStep(tag: String, step: String) {
        logEventStep(tag, step, Bundle())
    }

    fun logEventStep(step: String, bundle: Bundle) {
        logEventStep(TAG, step, bundle)
    }

    fun logEventStep(step: String) {
        logEventStep(TAG, step, Bundle())
    }

    private fun formatStepTime(timeMs: Long): String {
        val seconds = timeMs / 1000.0

        val step = if (seconds < 10) 0.5 else 5.0
        val rounded = kotlin.math.round(seconds / step) * step

        return String.format(Locale.US, "%.1f", rounded)
    }

    // region Banner splash

    private fun loadBannerSplash(
        activity: AppCompatActivity?,
        lifecycleOwner: LifecycleOwner,
        frAdsBanner: FrameLayout?,
        listIdBannerSplash: MutableList<String>,
        remoteKey: String
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
                        AdmobAdsConfig.getInstance().turnOffSomeRemoteKeys(activity)
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
        activity?.let { BannerManager(it, lifecycleOwner, bannerBuilder, remoteKey) }
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
        loadAdPreloadResume()
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