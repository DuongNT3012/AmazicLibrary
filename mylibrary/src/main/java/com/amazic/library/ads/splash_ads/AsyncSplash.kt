package com.amazic.library.ads.splash_ads

import android.content.Context
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
import com.amazic.library.Utils.EventTrackingHelper.time_splash_check
import com.amazic.library.Utils.NetworkUtil
import com.amazic.library.Utils.RemoteConfigHelper
import com.amazic.library.Utils.SharePreferenceHelper
import com.amazic.library.ads.admob.Admob
import com.amazic.library.ads.admob.AdmobApi
import com.amazic.library.ads.app_open_ads.AppOpenManager
import com.amazic.library.ads.banner_ads.BannerBuilder
import com.amazic.library.ads.banner_ads.BannerManager
import com.amazic.library.ads.callback.ApiCallback
import com.amazic.library.ads.callback.AppOpenCallback
import com.amazic.library.ads.callback.BannerCallback
import com.amazic.library.ads.callback.InterCallback
/*import com.amazic.library.iap.BillingCallback
import com.amazic.library.iap.IAPManager
import com.amazic.library.iap.ProductDetailCustom*/ //comment for billing
import com.amazic.library.organic.TechManager
import com.amazic.library.ump.AdsConsentManager
import com.amazic.mylibrary.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.system.measureTimeMillis

class AsyncSplash {
    private val TAG = "AsyncSplash"
    private var isTech = false
    private var adsSplash: AdsSplash? = null
    private var jsonIdAdsDefault = ""
    private var timeOutCallApi = 4000
    private var adjustKey = ""
    private var linkServer = ""
    private var appId = ""
    private var isShowAdsSplash = false
    private var isTimeout = false
    private var isNoInternetAction = false
    private var initWelcomeBack = "Normal"
    private var welcomeBackClass: Class<*>? = null
    private var isShowBannerSplash = false
    private var frAdsBannerSplash: FrameLayout? = null
    private var listIdBannerSplash: MutableList<String> =
        arrayListOf("ca-app-pub-3940256099942544/6300978111")
    private var adsKey: String = ""
    private var listTurnOffRemoteKeys: MutableList<String> = mutableListOf()
    private var activity: AppCompatActivity? = null
    private var interCallback: InterCallback? = null
    private var appOpenCallback: AppOpenCallback? = null
    private var isDebug = false
    private var isUseBilling = false

    //private var listProductDetailCustoms: ArrayList<ProductDetailCustom> = arrayListOf() //comment for billing
    private var timeOutSplash = 12000L
    private var isLoopAdsSplash = false
    private var useTechManagerOrDetectTestAd = DETECT_TEST_AD

    //1.use for log event time out 12s
    private var initRemoteConfig = false
    private var initAdmobApi = false
    private var initAdsConsentManager = false
    private var initBilling = false
    private var initTechManager = false

    //
    private var isUseIdAdsFromRemoteConfig = false
    private var isPreloadResumeAds = true
    private var isAsyncSplashAds = false

    //ad preloading
    private var isUseAdPreloading = false
    private var numberPreloading = 3
    private var numberPreloadingSplash = 1

    private var isShowNativeAfterInter = false

    private var isUseNativeSplash = false
    private var isUseNativeFullSplash = false
    private var isUseCacheDataCallSplash = false
    private var isUseDetectionVPNOrEmulator = false

    //1.end
    //2.use for log event
    private var timeStartSplash = System.currentTimeMillis()
    //2.end

    //Use for inter and open splash 1, 2, 3...
    private var keyAdsInterSplash = "inter_splash"
    private var keyAdsOpenSplash = "open_splash"
    private var keyAdsOpenResume = ""

    //key native after inter splash
    private var keyNativeAfterInterSplash = "native_after_inter"
    private var numberNativeAfterInterSplash = 1

    //
    private var isUseAppUpdateManager = false
    private var remoteKeyIdAdsServer = "id_ads"
    private var onPrepareLoadInterOpenSplashAds: (() -> Unit?)? = null

    //Log event 26/04/2025
    private var timeSplashCheck = System.currentTimeMillis()

    //check internet speed
    private var urlCheckInternetSpeed = "https://www.google.com/"

    //Set loadAndShowIdInterAdSplashAsync 30/05/2025
    private var loadAndShowIdInterAdSplashAsync = false

    //Set timeout call id remote config
    private var timeOutCallIdRemoteConfig = 4000L
    private var isSetId = false

    //Set key interval inter
    private var keyIntervalBetweenInterstitial = "interval_between_interstitial"
    private var keyIntervalInterstitialFromStart = "interval_interstitial_from_start"
    private var timeStep1 = System.currentTimeMillis()
    private var timeLastStep = System.currentTimeMillis()
//    private var onInitAdmobDone: (() -> Unit)? = null

    fun normalizeFirebaseEventName(input: String): String {
        if (input.isBlank()) return DEFAULT_EVENT_NAME

        var name = input
            .trim()
            .lowercase()

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
        val bundle = Bundle()
        val time_between_step = "${System.currentTimeMillis() - timeLastStep}"
        val time_to_step = "${System.currentTimeMillis() - timeStep1}"
        bundle.putString("time_between_step", time_between_step)
        bundle.putString("time_to_step", time_to_step)
        EventTrackingHelper.logEventWithMultipleParams(
            activity,
            normalizeFirebaseEventName("AsyncSplash_$step"),
            bundle
        )
        timeLastStep = System.currentTimeMillis()
    }

    companion object {
        private const val MAX_EVENT_NAME_LENGTH = 40
        private const val DEFAULT_EVENT_NAME = "event_unknown"
        const val TECH_MANAGER = "TechManager"
        const val DETECT_TEST_AD = "DetectTestAd"
        private var INSTANCE: AsyncSplash? = null
        fun getInstance(): AsyncSplash {
            if (INSTANCE == null) {
                INSTANCE = AsyncSplash()
            }
            return INSTANCE as AsyncSplash
        }
    }

    fun init(
        activity: AppCompatActivity,
        appOpenCallback: AppOpenCallback,
        interCallback: InterCallback,
        adjustKey: String,
        linkServer: String,
        appId: String,
        jsonIdAdsDefault: String
    ) {
        resetVarToDefault()
        this.activity = activity
        EventTrackingHelper.logEvent(activity, "${TAG}_INIT")
        timeStep1 = System.currentTimeMillis()
        timeLastStep = System.currentTimeMillis()
        this.adjustKey = adjustKey
        this.jsonIdAdsDefault = jsonIdAdsDefault
        this.linkServer = linkServer
        this.appId = appId
        this.appOpenCallback = appOpenCallback
        this.interCallback = interCallback
    }

    private fun resetVarToDefault() {
        this.isTech = false
        this.jsonIdAdsDefault = ""
        this.adjustKey = ""
        this.linkServer = ""
        this.appId = ""
        this.isShowAdsSplash = false
        this.isTimeout = false
        this.isNoInternetAction = false
        this.initWelcomeBack = "Normal"
        this.welcomeBackClass = null
        this.isShowBannerSplash = false
        this.listIdBannerSplash = arrayListOf("ca-app-pub-3940256099942544/6300978111")
        this.listTurnOffRemoteKeys = mutableListOf()
        this.isDebug = false
        this.isUseBilling = false
        //this.listProductDetailCustoms = arrayListOf() //comment for billing
        this.timeOutSplash = 12000L
        this.isLoopAdsSplash = false
        this.useTechManagerOrDetectTestAd = DETECT_TEST_AD
        this.initRemoteConfig = false
        this.initAdmobApi = false
        this.initAdsConsentManager = false
        this.initBilling = false
        this.initTechManager = false
        this.isUseIdAdsFromRemoteConfig = false
        this.isPreloadResumeAds = true
        this.numberPreloading = 3
        this.numberPreloadingSplash = 1
        this.isUseAdPreloading = false
        this.isAsyncSplashAds = false
        this.keyAdsInterSplash = "inter_splash"
        this.keyAdsOpenSplash = "open_splash"
        this.keyAdsOpenResume = ""
        this.loadAndShowIdInterAdSplashAsync = false
        this.timeOutCallIdRemoteConfig = 4000L
        this.isSetId = false
        this.keyIntervalBetweenInterstitial = "interval_between_interstitial"
        this.keyIntervalInterstitialFromStart = "interval_interstitial_from_start"
        this.keyNativeAfterInterSplash = "native_after_inter"
        this.numberNativeAfterInterSplash = 1
        this.isShowNativeAfterInter = false
        this.isUseNativeSplash = false
        this.isUseNativeFullSplash = false
        this.isUseCacheDataCallSplash = false
        this.isUseDetectionVPNOrEmulator = false
    }

    fun setKeyIntervalBetweenInterstitial(keyIntervalBetweenInterstitial: String) {
        this.keyIntervalBetweenInterstitial = keyIntervalBetweenInterstitial
    }

    fun setKeyIntervalInterstitialFromStart(keyIntervalInterstitialFromStart: String) {
        this.keyIntervalInterstitialFromStart = keyIntervalInterstitialFromStart
    }

    fun setTimeOutCallIdRemoteConfig(timeOutCallIdRemoteConfig: Long) {
        this.timeOutCallIdRemoteConfig = timeOutCallIdRemoteConfig
    }

    fun getLoadAndShowIdInterAdSplashAsync(): Boolean {
        return this.loadAndShowIdInterAdSplashAsync
    }

    fun setLoadAndShowIdInterAdSplashAsync() {
        this.loadAndShowIdInterAdSplashAsync = true
    }

    fun setUrlCheckInternetSpeed(urlCheckInternetSpeed: String) {
        this.urlCheckInternetSpeed = urlCheckInternetSpeed
    }

    fun setTimeSplashCheck() {
        this.timeSplashCheck = System.currentTimeMillis()
    }

    fun getTimeSplashCheck(): Long {
        return this.timeSplashCheck
    }

    fun setOnPrepareLoadInterOpenSplashAds(onPrepareLoadInterOpenSplashAds: () -> Unit) {
        this.onPrepareLoadInterOpenSplashAds = onPrepareLoadInterOpenSplashAds
    }

//    fun setOnInitAdmobDone(onInitAdmobDone: () -> Unit) {
//        this.onInitAdmobDone = onInitAdmobDone
//    }

    fun setUseAppUpdateManager() {
        this.isUseAppUpdateManager = true
    }

    fun setKeyAdsInterSplash(keyAdsInterSplash: String) {
        this.keyAdsInterSplash = keyAdsInterSplash
    }

    fun setKeyAdsOpenSplash(keyAdsOpenSplash: String) {
        this.keyAdsOpenSplash = keyAdsOpenSplash
    }

    fun setKeyAdsOpenResume(keyAdsOpenResume: String) {
        this.keyAdsOpenResume = keyAdsOpenResume
    }

    fun getKeyAdsOpenResume(): String {
        return this.keyAdsOpenResume
    }

    fun setKeyNativeAfterInterSplash(key: String) {
        this.keyNativeAfterInterSplash = key
    }

    fun setNumberNativeAfterInterSplash(count: Int) {
        this.numberNativeAfterInterSplash = count
    }

    fun getNumberNativeAfterInterSplash(): Int {
        return this.numberNativeAfterInterSplash
    }

    fun setAsyncSplashAds() { //Show splash ads without wait any thing
        this.isAsyncSplashAds = true
    }

    fun setPreloadResumeAds(isPreloadResumeAds: Boolean) { //Set can preload resume ads or not (Note: set false when you need to load and show resume ads)
        this.isPreloadResumeAds = isPreloadResumeAds
    }

    fun getPreloadResumeAds(): Boolean {
        return this.isPreloadResumeAds
    }

    fun setKeyNumberPreloading(keyNumber: String) {
        var number: Int =
            RemoteConfigHelper.getInstance().get_config_long(activity, keyNumber).toInt()
        this.numberPreloading = number
    }

    fun getNumberPreloading(): Int {
        return this.numberPreloading
    }

    fun setNumberPreloadingSplash(number: Int) {
        this.numberPreloadingSplash = number
    }

    fun getNumberPreloadingSplash(): Int {
        return this.numberPreloadingSplash
    }

    fun setUseAdPreloading(isUsePreLoading: Boolean) {
        this.isUseAdPreloading = isUsePreLoading
    }

    fun getUseAdPreloading(): Boolean {
        return this.isUseAdPreloading
    }

    fun setShowNativeAfterInter(isShowNativeAfterInter: Boolean) {
        this.isShowNativeAfterInter = isShowNativeAfterInter
    }

    fun getShowNativeAfterInter(): Boolean {
        return this.isShowNativeAfterInter
    }

    fun setUseNativeSplash(isUse: Boolean) {
        this.isUseNativeSplash = isUse
    }

    fun getUseNativeSplash(): Boolean {
        return this.isUseNativeSplash
    }

    fun setUseNativeFullSplash(isUse: Boolean) {
        this.isUseNativeFullSplash = isUse
    }

    fun getUseNativeFullSplash(): Boolean {
        return this.isUseNativeFullSplash
    }

    fun setUseCacheDataCallSplash(isUse: Boolean) {
        this.isUseCacheDataCallSplash = isUse
    }

    fun getUseCacheDataCallSplash(): Boolean {
        return this.isUseCacheDataCallSplash
    }

    fun setUseDetectionVPNOrEmulator(isUse: Boolean) {
        this.isUseDetectionVPNOrEmulator = isUse
    }

    fun getUseDetectionVPNOrEmulator(): Boolean {
        return this.isUseDetectionVPNOrEmulator
    }

    fun setUseIdAdsFromRemoteConfig(remoteKeyIdAdsServer: String) { //Use id ads from remote config or not (Key remote: id_ads)
        this.isUseIdAdsFromRemoteConfig = true
        this.remoteKeyIdAdsServer = remoteKeyIdAdsServer
        this.timeOutCallApi = 0
    }

    fun setTimeOutCallApi(timeOutCallApi: Int) { //Timeout call id ads from server
        this.timeOutCallApi = timeOutCallApi
    }

    fun getUserTechManagerOrDetectTestAd(): String {
        return this.useTechManagerOrDetectTestAd
    }

    fun setUseTechManager() { //Use TechManager (Organic) or not
        this.useTechManagerOrDetectTestAd = TECH_MANAGER
    }

    fun setUseDetectTestAd() {
        this.useTechManagerOrDetectTestAd = DETECT_TEST_AD
    }

    fun setLoopAdsSplash() { //Load loop splash ads when load fail if time splash < 8s
        this.isLoopAdsSplash = true
    }

    fun getShowAdsSplash(): Boolean {
        return this.isShowAdsSplash
    }

    fun getTimeout(): Boolean {
        return this.isTimeout
    }

    fun getNoInternetAction(): Boolean {
        return this.isNoInternetAction
    }

    fun getTimeStartSplash(): Long {
        return this.timeStartSplash
    }

    fun setTimeOutSplash(timeOutSplash: Long) {
        this.timeOutSplash = timeOutSplash
    }

    /*fun setUseBilling(listProductDetailCustoms: ArrayList<ProductDetailCustom>) { //If need use IAP
        this.isUseBilling = true
        this.listProductDetailCustoms.clear()
        this.listProductDetailCustoms.addAll(listProductDetailCustoms)
    }*/ //comment for billing

    fun setDebug(isDebug: Boolean) { //Use for TechManager or DetectTestAd
        this.isDebug = isDebug
    }

    fun getDebug(): Boolean {
        return this.isDebug
    }

    fun checkShowSplashWhenFail() { //Call on resume of splash screen (Reshow splash ads when show fail)
        if (adsSplash != null) {
            adsSplash?.onCheckShowSplashWhenFail(activity, appOpenCallback, interCallback)
        }
    }

    fun setInitResumeAdsNormal() {
        this.initWelcomeBack = "Normal"
        this.isPreloadResumeAds = true
    }

    fun setInitWelcomeBackBelowResumeAds(welcomeBackClass: Class<*>) {
        this.initWelcomeBack = "Below"
        this.welcomeBackClass = welcomeBackClass
        this.isPreloadResumeAds = true
    }

    fun setInitWelcomeBackAboveResumeAds(welcomeBackClass: Class<*>) {
        this.initWelcomeBack = "Above"
        this.welcomeBackClass = welcomeBackClass
        this.isPreloadResumeAds = false
    }

    fun getInitResumeAdsType(): String {
        return this.initWelcomeBack
    }

    fun setShowBannerSplash(
        frAdsBannerSplash: FrameLayout,
        listIdBannerSplash: MutableList<String>,
        adsKey: String
    ) {
        this.isShowBannerSplash = true
        this.frAdsBannerSplash = frAdsBannerSplash
        this.listIdBannerSplash.clear()
        this.listIdBannerSplash.addAll(listIdBannerSplash)
        this.adsKey = adsKey
    }

    fun setListTurnOffRemoteKeys(listTurnOffRemoteKeys: MutableList<String>) {
        this.listTurnOffRemoteKeys.clear()
        this.listTurnOffRemoteKeys.addAll(listTurnOffRemoteKeys)
    }

    private fun logEventDoneInit() {
        val bundle = Bundle()
        val time = String.format(
            Locale.US, "%.1f_%.2f_%.3f_%.4f",
            timeInitAdmobApi / 1000f,
            timeInitRemoteConfig / 1000f,
            timeInitAdsConsentManager / 1000f,
            timeInitTechManager / 1000f
        )
        bundle.putString("time_between_step", time)
        EventTrackingHelper.logEventWithMultipleParams(
            activity,
            normalizeFirebaseEventName("AsyncSplash_doneInit"),
            bundle
        )
        timeLastStep = System.currentTimeMillis()
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
        timeStartSplash = System.currentTimeMillis()
        lifecycleCoroutineScope.launch {
            delay(timeOutSplash)
            Log.d(TAG, "Timeout check $isShowAdsSplash $isNoInternetAction ")
            logEventStep("AsyncTimeout")

            val bundleEvent = Bundle()
            bundleEvent.putString("isTimeout", "$isTimeout")
            bundleEvent.putString("isNoInternetAction", "$isNoInternetAction")
            EventTrackingHelper.logEventWithMultipleParams(
                activity,
                normalizeFirebaseEventName("AsyncSplash_VAsyncTimeout"),
                bundleEvent
            )
            if (!isShowAdsSplash && !isNoInternetAction) {
                //1.log event timeout splash 12s
                val bundle = Bundle()
                bundle.putString(
                    "timeout_splash_next_screen_detail",
                    "${initAdmobApi}_${initRemoteConfig}_${initAdsConsentManager}_${initBilling}_${initTechManager}"
                )
                bundle.putString("initAdmobApi", initAdmobApi.toString())
                bundle.putString("initRemoteConfig", initRemoteConfig.toString())
                bundle.putString("initAdsConsentManager", initAdsConsentManager.toString())
                bundle.putString("initBilling", initBilling.toString())
                bundle.putString("initTechManager", initTechManager.toString())
                EventTrackingHelper.logEventWithMultipleParams(
                    context,
                    "timeout_splash_next_screen",
                    bundle
                )
                //1.end log event timeout splash 12s
                //2.increase splash open
                SharePreferenceHelper.setInt(
                    activity,
                    EventTrackingHelper.splash_open,
                    SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1
                )
                //2.end increase splash open
                if (useTechManagerOrDetectTestAd == TECH_MANAGER) {
                    if (isTech && !isDebug) {
                        turnOffSomeRemoteKeys(activity)
                    }
                } else if (useTechManagerOrDetectTestAd == DETECT_TEST_AD) {
                    if (TechManager.getInstance().isTech(activity) && !isDebug) {
                        turnOffSomeRemoteKeys(activity)
                    }
                }
                interCallback?.onNextAction()
                Log.d(TAG, "Timeout Splash.")
                isTimeout = true
            }
            return@launch
        }
        if (NetworkUtil.isNetworkActive(activity)) {
            logEventStep("AsyncInternet")
            EventTrackingHelper.logEvent(activity, "splash_have_internet_original")
            measureDownloadSpeed(urlCheckInternetSpeed) { speedMbps ->
                Log.d(TAG, "measureDownloadSpeed log event: ${speedMbps.toInt()}")
                EventTrackingHelper.logEventWithAParam(
                    activity,
                    "splash_have_internet",
                    "internet_speed",
                    speedMbps.toInt().toString()
                )
            }
            lifecycleCoroutineScope.launch {
                logEventStep("StartAsyncInit")
                val asyncAdmobApi = async { initAdmobApi(activity) }
                val asyncRemoteConfig = async { initRemoteConfig(activity) }
                val asyncUMP = async { initAdsConsentManager(activity) }
                val asyncBilling = async { initBilling() }
                val asyncTechManager = async { initTechManager(activity) }
                try {
                    //wait to load banner splash (banner splash fix id, don't use api to reduce time load splash)
                    if (!isAsyncSplashAds) {
                        awaitAll(asyncRemoteConfig, asyncUMP, asyncBilling, asyncTechManager)
                        if (useTechManagerOrDetectTestAd == TECH_MANAGER && isTech && !isDebug) {
                            turnOffSomeRemoteKeys(activity)
                        }
                    } else {
                        awaitAll(asyncUMP)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    logEventStep("DoneAsyncInit")
                    loadBannerSplash(
                        activity,
                        lifecycleOwner,
                        frAdsBannerSplash,
                        listIdBannerSplash,
                        adsKey
                    )
                    try {
                        logEventStep("StartIDApi")
                        //wait to load inter or open splash
                        asyncAdmobApi.await()
                        logEventStep("DoneIDApi")
                    } catch (e: Exception) {
                        logEventStep("IDApiFailed")
                        e.printStackTrace()
                    } finally {
                        logEventDoneInit()
                        val timeAsync = (System.currentTimeMillis() - timeSplashCheck) / 1000
                        EventTrackingHelper.logEventWithAParam(
                            activity,
                            time_splash_check,
                            time_splash_check,
                            timeAsync.toString()
                        )
                        onPrepareLoadInterOpenSplashAds?.invoke()
//                        lifecycleCoroutineScope.launch {
                        logEventStep("StartAdSplash")
                        var rateAoaInterSplash: String =
                            RemoteConfigHelper.getInstance().get_config_string(
                                activity,
                                RemoteConfigHelper.rate_aoa_inter_splash
                            )
                        if (rateAoaInterSplash.isEmpty()) {
                            rateAoaInterSplash = "0_100"
                        }
                        val isShowOpenSplash: Boolean = RemoteConfigHelper.getInstance()
                            .get_config(activity, keyAdsOpenSplash)
                        val isShowInterSplash: Boolean = RemoteConfigHelper.getInstance()
                            .get_config(activity, keyAdsInterSplash)
                        adsSplash = AdsSplash.init(
                            isShowOpenSplash,
                            isShowInterSplash,
                            rateAoaInterSplash
                        )
                        adsSplash?.setKeyAdsInterSplash(keyAdsInterSplash)
                        adsSplash?.setKeyAdsOpenSplash(keyAdsOpenSplash)
                        adsSplash?.setLoopAdsSplash(isLoopAdsSplash)
                        showAdsSplash(activity, appOpenCallback, interCallback)
//                        }
                        if (isAsyncSplashAds) {
                            awaitAll(asyncRemoteConfig, asyncTechManager)
                            if (useTechManagerOrDetectTestAd == TECH_MANAGER && isTech && !isDebug) {
                                turnOffSomeRemoteKeys(activity)
                            }
                        }
//                        lifecycleCoroutineScope.launch {
                        //set list random animation
                        val listAnim = arrayListOf(
                            R.raw.ads_1,
                            R.raw.ads_2,
                            R.raw.ads_3,
                            R.raw.ads_4,
                            R.raw.ads_5,
                            R.raw.ads_6,
                            R.raw.ads_7,
                            R.raw.ads_8,
                            R.raw.ads_9,
                            R.raw.ads_10,
                        )
                        Admob.getInstance().setCustomAnimationDialog(listAnim)
                        AppOpenManager.getInstance().setCustomAnimationDialog(listAnim)
                        //end
                        onAsyncSplashDone.invoke()

                        ///load ad preload resume
                        loadAdPreloadResume()
//                        }
                    }
                }
            }
        } else {
            logEventStep("AsyncNoInternet")
            if (!isShowAdsSplash && !isTimeout) {
                onNoInternetAction.invoke()
                isNoInternetAction = true
            }
        }
    }

    fun turnOffSomeRemoteKeys(activity: Context?) {
        listTurnOffRemoteKeys.forEach {
            Log.d(TAG, "turnOffSomeRemoteKeys: $it")
            RemoteConfigHelper.getInstance().set_config(activity, it, false)
        }
    }

    fun measureDownloadSpeed(
        urlCheckInternetSpeed: String,
        onResult: (speedMbps: Double) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL(urlCheckInternetSpeed).openConnection()
                connection.connect()

                val inputStream = connection.getInputStream()
                val buffer = ByteArray(1024 * 8) // 8KB buffer
                var totalBytesRead = 0L

                val timeTakenMillis = measureTimeMillis {
                    while (true) {
                        val bytesRead = inputStream.read(buffer)
                        if (bytesRead == -1) break
                        totalBytesRead += bytesRead
                        if (totalBytesRead >= 1 * 1024 * 1024) break // Limit 1MB download
                    }
                    inputStream.close()
                }

                val speedBytesPerSec = totalBytesRead / (timeTakenMillis / 1000.0)
                val speedMbps = (speedBytesPerSec * 8) / (1024 * 1024) // Byte/s → Mbps

                withContext(Dispatchers.Main) {
                    Log.d(TAG, "measureDownloadSpeed: $speedMbps")
                    onResult(speedMbps)
                }
            } catch (e: Exception) {
                Log.d(TAG, "measureDownloadSpeed: ${e.message}")
                withContext(Dispatchers.Main) {
                    onResult(0.0)
                }
            }
        }
    }

    private val PREF_REMOTE_FETCHED_FLAG = "remote_fetched_once"
    private var timeInitRemoteConfig = 0L
    private suspend fun initRemoteConfig(
        activity: AppCompatActivity?
    ) = suspendCoroutine<Unit> { continuation ->
        timeInitRemoteConfig = 0L
        if (isUseAppUpdateManager) {
            continuation.resume(Unit)
            return@suspendCoroutine
        } else {
            val startTimeInitRemoteConfig = System.currentTimeMillis()
            EventTrackingHelper.logEvent(activity, "initRemoteConfig")

            // check flag trong cùng SharedPreferences "remote_fill"
            val prefs = activity?.getSharedPreferences("remote_fill", Context.MODE_PRIVATE)
            val hasBeenFetchedBefore = prefs?.getBoolean(PREF_REMOTE_FETCHED_FLAG, false) ?: false


            //Handle remote config id ads timeout
            Handler(Looper.getMainLooper()).postDelayed({
                if (isUseIdAdsFromRemoteConfig && !isSetId) {
                    AdmobApi.getInstance().jsonIdAdsDefault = jsonIdAdsDefault
                    AdmobApi.getInstance().convertJsonIdAdsDefaultToList(jsonIdAdsDefault)
                    isSetId = true
                    Log.d(
                        TAG,
                        "Timeout Remote Config: Id ads size = ${AdmobApi.getInstance().listAdsSize}"
                    )
                    EventTrackingHelper.logEvent(activity, "timeout_call_id_remote_config")
                    initWelcomeBack(activity)//17.09.2025
                    timeInitRemoteConfig = System.currentTimeMillis() - startTimeInitRemoteConfig
                }
            }, timeOutCallIdRemoteConfig)

            Log.d(
                TAG,
                "check initRemoteConfig: hasBeenFetchedBefore = $hasBeenFetchedBefore, isUseCacheDataCallSplash = $isUseCacheDataCallSplash"
            )

            if (hasBeenFetchedBefore && isUseCacheDataCallSplash) {
                Log.d(TAG, "initRemoteConfig: using SharedPreferences cache")

                // apply config từ SP ngay (get_config đọc từ SP → instant)
                Admob.getInstance().showAllAds = RemoteConfigHelper.getInstance()
                    .get_config(activity, RemoteConfigHelper.show_all_ads)
                Admob.getInstance().setTimeInterval(
                    RemoteConfigHelper.getInstance().get_config_long(
                        activity, keyIntervalBetweenInterstitial
                    ) * 1000, true
                )
                Admob.getInstance().setTimeIntervalFromStart(
                    RemoteConfigHelper.getInstance().get_config_long(
                        activity, keyIntervalInterstitialFromStart
                    ) * 1000
                )
                if (isUseIdAdsFromRemoteConfig && !isSetId) {
                    // get_config_string cũng đọc từ SP → instant
                    val jsonFromSP = RemoteConfigHelper.getInstance()
                        .get_config_string(activity, RemoteConfigHelper.id_ads)
                    if (jsonFromSP.contains("app_id")) {
                        AdmobApi.getInstance().jsonIdAdsDefault = jsonFromSP
                        AdmobApi.getInstance().convertJsonIdAdsDefaultToList(jsonFromSP)
                        isSetId = true
                        EventTrackingHelper.logEvent(activity, "set_id_remote_config")
                    } else {
                        AdmobApi.getInstance().jsonIdAdsDefault = jsonIdAdsDefault
                        AdmobApi.getInstance().convertJsonIdAdsDefaultToList(jsonIdAdsDefault)
                        isSetId = true
                    }
                    initWelcomeBack(activity)
                    EventTrackingHelper.logEvent(
                        activity,
                        "set_id_default_case_fail_remote"
                    )
                }

                timeInitRemoteConfig = System.currentTimeMillis() - startTimeInitRemoteConfig
                initRemoteConfig = true
                continuation.resume(Unit) // resume ngay
                Log.d(
                    TAG,
                    "initRemoteConfig: END using SharedPreferences cache - ${timeInitRemoteConfig / 1000}"
                )
                // Fetch ngầm để update SP cho session sau
                CoroutineScope(Dispatchers.Main).launch {
                    RemoteConfigHelper.getInstance().fetchAllKeysAndTypes(activity) {
                        Log.d(TAG, "initRemoteConfig: background fetch done, isSuccess=$it")
                    }
                }
            } else {
                Log.d(TAG, "initRemoteConfig: first time, fetching from Firebase")
                var isResumed = false
                RemoteConfigHelper.getInstance().fetchAllKeysAndTypes(activity) { isSuccess ->
                    if (isSuccess && isUseCacheDataCallSplash) {
                        prefs?.edit()?.putBoolean(PREF_REMOTE_FETCHED_FLAG, true)?.apply()
                    }
                    //Handle remote config id ads
                    if (isUseIdAdsFromRemoteConfig && !isSetId) {
                        val jsonIdAdsFromRemoteConfig = RemoteConfigHelper.getInstance()
                            .get_config_string(activity, RemoteConfigHelper.id_ads)
                        if (jsonIdAdsFromRemoteConfig.contains("app_id") && isSuccess) { //get id ads from remote config successfully
                            AdmobApi.getInstance().jsonIdAdsDefault = jsonIdAdsFromRemoteConfig
                            AdmobApi.getInstance()
                                .convertJsonIdAdsDefaultToList(jsonIdAdsFromRemoteConfig)
                            isSetId = true
                            Log.d(
                                TAG,
                                "Set id ads from remote: Id ads size = ${AdmobApi.getInstance().listAdsSize}"
                            )
                            EventTrackingHelper.logEvent(activity, "set_id_remote_config")
                        } else {
                            AdmobApi.getInstance().jsonIdAdsDefault = jsonIdAdsDefault
                            AdmobApi.getInstance().convertJsonIdAdsDefaultToList(jsonIdAdsDefault)
                            isSetId = true
                            Log.d(
                                TAG,
                                "Set id ads default case fail remote: Id ads size = ${AdmobApi.getInstance().listAdsSize}"
                            )
                            EventTrackingHelper.logEvent(
                                activity,
                                "set_id_default_case_fail_remote"
                            )
                        }
                        initWelcomeBack(activity)//17.09.2025
                    }
                    //Handle General
                    Log.d(
                        TAG,
                        "show_all_ads = ${
                            RemoteConfigHelper.getInstance()
                                .get_config(activity, RemoteConfigHelper.show_all_ads)
                        }"
                    )
                    Admob.getInstance().showAllAds = RemoteConfigHelper.getInstance()
                        .get_config(activity, RemoteConfigHelper.show_all_ads)
                    Admob.getInstance().setTimeInterval(
                        RemoteConfigHelper.getInstance().get_config_long(
                            activity,
                            RemoteConfigHelper.interval_between_interstitial
                        ) * 1000, true
                    )
                    Admob.getInstance().setTimeIntervalFromStart(
                        RemoteConfigHelper.getInstance().get_config_long(
                            activity,
                            RemoteConfigHelper.interval_interstitial_from_start
                        ) * 1000
                    )
                    timeInitRemoteConfig = System.currentTimeMillis() - startTimeInitRemoteConfig
                    if (!isResumed) {
                        isResumed = true
                        continuation.resume(Unit)
                        initRemoteConfig = true
                        Log.d(TAG, "initRemoteConfig. time - ${timeInitRemoteConfig / 1000}")
                    }
                    Log.d(
                        TAG,
                        "initRemoteConfig: END first time, fetching from Firebase - ${timeInitRemoteConfig / 1000}"
                    )

                }
            }
        }
    }

    private var timeInitAdsConsentManager = 0L
    private suspend fun initAdsConsentManager(activity: AppCompatActivity?) =
        suspendCoroutine { continuation ->
            timeInitAdsConsentManager = 0L
            val startTimeInitAdsConsentManager = System.currentTimeMillis()
            val adsConsentManager = AdsConsentManager(activity)
            var isResumed = false
            adsConsentManager.requestUMP {
                if (!isResumed) {
                    isResumed = true
                    if (it) {
                        Admob.getInstance().initAdmob(activity) {
//                            onInitAdmobDone?.invoke()
                        }
                        activity?.let { it1 ->
                            AppOpenManager.getInstance().disableAppResumeWithActivity(it1.javaClass)
                        }
                    }
                    timeInitAdsConsentManager =
                        System.currentTimeMillis() - startTimeInitAdsConsentManager
                    initAdsConsentManager = true
                    continuation.resume(Unit)
                    Log.d(TAG, "initAdsConsentManager.")
                }
            }
        }

    private var timeInitTechManager = 0L
    private suspend fun initTechManager(activity: AppCompatActivity?) =
        suspendCoroutine<Unit> { continuation ->
            var isResumed = false
            timeInitTechManager = 0L
            val startTimeInitTechManager = System.currentTimeMillis()
            if (useTechManagerOrDetectTestAd == TECH_MANAGER) {
                TechManager.getInstance().getResult(isDebug, activity, adjustKey) {
                    if (it) {
                        isTech = true
                        AppOpenManager.getInstance().isEnableResume = false
                    }
                    timeInitTechManager = System.currentTimeMillis() - startTimeInitTechManager
                    if (!isResumed) {
                        isResumed = true
                        continuation.resume(Unit)
                        initTechManager = true
                        Log.d(TAG, "initTechManager.")
                    }
                }
            } else {
                timeInitTechManager = System.currentTimeMillis() - startTimeInitTechManager
                continuation.resume(Unit)
                initTechManager = true
                Log.d(TAG, "initTechManager else.")
            }
        }

    private var timeInitAdmobApi = 0L
    private val PREF_CACHED_AD_IDS = "cached_ad_ids"
    private suspend fun initAdmobApi(activity: AppCompatActivity?) =
        suspendCoroutine<Unit> { continuation ->
            if (!isUseIdAdsFromRemoteConfig) {
                val startTimeInitAdmobApi = System.currentTimeMillis()
                AdmobApi.getInstance().jsonIdAdsDefault = jsonIdAdsDefault
                AdmobApi.getInstance().timeOutCallApi = timeOutCallApi

                val prefs = activity?.getSharedPreferences("MY_PRE", Context.MODE_PRIVATE)
                val cachedJson = prefs?.getString(PREF_CACHED_AD_IDS, "") ?: ""

                Log.d(
                    TAG,
                    "check initAdmobApi: cachedJson.isNotEmpty() = ${cachedJson.isNotEmpty()}, isUseCacheDataCallSplash = $isUseCacheDataCallSplash"
                )
                if (cachedJson.isNotEmpty() && isUseCacheDataCallSplash) {
                    Log.d(TAG, "initAdmobApi: using cache id Ads")

                    AdmobApi.getInstance().convertJsonIdAdsDefaultToList(cachedJson)
                    initWelcomeBack(activity)
                    initAdmobApi = true
                    timeInitAdmobApi = System.currentTimeMillis() - startTimeInitAdmobApi
                    continuation.resume(Unit)
                    Log.d(TAG, "initAdmobApi: END using cache id Ads - ${timeInitAdmobApi / 1000}")


                    // Refresh ngầm cho session sau
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            withContext(Dispatchers.Main) {
                                AdmobApi.getInstance().refreshCacheOnly(
                                    activity,
                                    linkServer,
                                    appId,
                                    object : ApiCallback() {
                                        override fun onReady() {
                                            super.onReady()
                                            val newJson = AdmobApi.getInstance().jsonIdAdsDefault
                                            if (newJson.isNotEmpty()) {
                                                prefs?.edit()
                                                    ?.putString(PREF_CACHED_AD_IDS, newJson)
                                                    ?.apply()
                                                Log.d(TAG, "initAdmobApi: background cache update")
                                            }
                                        }
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "initAdmobApi: background refresh failed - ${e.message}")
                        }
                    }
                } else {
                    Log.d(TAG, "initAdmobApi: first time, init ads")

                    AdmobApi.getInstance()
                        .init(activity, linkServer, appId, object : ApiCallback() {
                            private var isResumed = false
                            override fun onReady() {
                                super.onReady()
                                //save cached json id
                                if (isUseCacheDataCallSplash) {
                                    val json = AdmobApi.getInstance().jsonIdAdsDefault
                                    if (json.isNotEmpty()) {
                                        prefs?.edit()?.putString(PREF_CACHED_AD_IDS, json)?.apply()
                                        Log.d(TAG, "initAdmobApi: cache saved")
                                    }
                                }

                                initWelcomeBack(activity)
                                if (!isResumed) {
                                    isResumed = true
                                    continuation.resume(Unit)
                                    initAdmobApi = true
                                    Log.d(TAG, "initAdmobApi.")
                                }
                            }
                        })
                    timeInitAdmobApi = System.currentTimeMillis() - startTimeInitAdmobApi
                    Log.d(TAG, "initAdmobApi:END first time, init ads - ${timeInitAdmobApi / 1000}")
                }
            } else {
                continuation.resume(Unit)
            }
        }

    private fun initWelcomeBack(activity: AppCompatActivity?) {
        when (initWelcomeBack) {
            "Normal" -> {
                val listIdResume = mutableListOf<String>()
                if (keyAdsOpenResume.isNotEmpty()) {
                    listIdResume.addAll(AdmobApi.getInstance().getListIDByName(keyAdsOpenResume))
                } else {
                    listIdResume.addAll(AdmobApi.getInstance().listIDAppOpenResume)
                    keyAdsOpenResume = "open_resume"
                }
                if (listIdResume.isNotEmpty()) {
                    if (!isUseAdPreloading) {
                        Log.d(
                            TAG,
                            "APP Open Preload: normal - isUseAdPreloading = $isUseAdPreloading, isPreloadResumeAds = $isPreloadResumeAds"
                        )
                        if (isPreloadResumeAds) {
                            AppOpenManager.getInstance()
                                .loadAdNotCheckRemote(activity, listIdResume, keyAdsOpenResume)
                        }
                    }
                    AppOpenManager.getInstance().init(activity, listIdResume)
                    activity?.let {
                        AppOpenManager.getInstance().disableAppResumeWithActivity(it.javaClass)
                    } //disable resume splash
                }
            }

            "Below" -> {
                val listIdResume = mutableListOf<String>()
                if (keyAdsOpenResume.isNotEmpty()) {
                    listIdResume.addAll(AdmobApi.getInstance().getListIDByName(keyAdsOpenResume))
                } else {
                    listIdResume.addAll(
                        AdmobApi.getInstance().getListIDByName(RemoteConfigHelper.resume_wb)
                    )
                    keyAdsOpenResume = "resume_wb"
                }
                if (listIdResume.isNotEmpty()) {
                    if (!isUseAdPreloading) {
                        Log.d(
                            TAG,
                            "APP Open Preload: below - isUseAdPreloading = $isUseAdPreloading ,isPreloadResumeAds = $isPreloadResumeAds"
                        )
                        if (isPreloadResumeAds) {
                            AppOpenManager.getInstance()
                                .loadAdNotCheckRemote(activity, listIdResume, keyAdsOpenResume)
                        }
                    }
                    welcomeBackClass?.let {
                        AppOpenManager.getInstance()
                            .initWelcomeBackBelowAdsResume(activity, listIdResume, it)
                        AppOpenManager.getInstance()
                            .disableAppResumeWithActivity(it) //disable resume welcome back
                    }
                    activity?.let {
                        AppOpenManager.getInstance().disableAppResumeWithActivity(it.javaClass)
                    } //disable resume splash
                }
            }

            "Above" -> {
                val listIdResume = mutableListOf<String>()
                if (keyAdsOpenResume.isNotEmpty()) {
                    listIdResume.addAll(AdmobApi.getInstance().getListIDByName(keyAdsOpenResume))
                } else {
                    listIdResume.addAll(
                        AdmobApi.getInstance().getListIDByName(RemoteConfigHelper.resume_wb)
                    )
                    keyAdsOpenResume = "resume_wb"
                }
                if (listIdResume.isNotEmpty()) {
                    if (!isUseAdPreloading) {
                        Log.d(
                            TAG,
                            "APP Open Preload: above - isUseAdPreloading = $isUseAdPreloading, isPreloadResumeAds = $isPreloadResumeAds"
                        )
                        if (isPreloadResumeAds) {
                            AppOpenManager.getInstance()
                                .loadAdNotCheckRemote(activity, listIdResume, keyAdsOpenResume)
                        }
                    }
                    welcomeBackClass?.let {
                        AppOpenManager.getInstance()
                            .initWelcomeBackAboveAdsResume(activity, listIdResume, it)
                        AppOpenManager.getInstance()
                            .disableAppResumeWithActivity(it) //disable resume welcome back
                    }
                    activity?.let {
                        AppOpenManager.getInstance().disableAppResumeWithActivity(it.javaClass)
                    } //disable resume splash
                }
            }

            else -> {
                val listIdResume = mutableListOf<String>()
                if (keyAdsOpenResume.isNotEmpty()) {
                    listIdResume.addAll(AdmobApi.getInstance().getListIDByName(keyAdsOpenResume))
                } else {
                    listIdResume.addAll(AdmobApi.getInstance().listIDAppOpenResume)
                    keyAdsOpenResume = "open_resume"
                }
                if (listIdResume.isNotEmpty()) {
                    if (!isUseAdPreloading) {
                        Log.d(
                            TAG,
                            "APP Open Preload: else - isUseAdPreloading = $isUseAdPreloading, isPreloadResumeAds = $isPreloadResumeAds"
                        )
                        if (isPreloadResumeAds) {
                            AppOpenManager.getInstance()
                                .loadAdNotCheckRemote(activity, listIdResume, keyAdsOpenResume)
                        }
                    }
                    AppOpenManager.getInstance().init(activity, listIdResume)
                    activity?.let {
                        AppOpenManager.getInstance().disableAppResumeWithActivity(it.javaClass)
                    } //disable resume splash
                }
            }
        }
    }

    private fun loadAdPreloadResume() {
        if (isUseAdPreloading) {
            val listIdResume = mutableListOf<String>()
            if (keyAdsOpenResume.isNotEmpty()) {
                listIdResume.addAll(AdmobApi.getInstance().getListIDByName(keyAdsOpenResume))
            } else {
                listIdResume.addAll(AdmobApi.getInstance().listIDAppOpenResume)
                keyAdsOpenResume = "open_resume"
            }
            if (listIdResume.isNotEmpty()) {
                Log.d(
                    TAG,
                    "APP Open Preload: start loadAdPreloadNotCheckRemote"
                )
                AppOpenManager.getInstance().loadAdPreloadNotCheckRemote(
                    activity,
                    listIdResume,
                    keyAdsOpenResume
                )
            }
        }
    }

    private suspend fun initBilling() = suspendCoroutine<Unit> { continuation ->
        /*if (isUseBilling) {
            //check if app use billing -> initBilling
            IAPManager.getInstance().initBilling(activity, listProductDetailCustoms, object : BillingCallback() {
                private var isResumed = false
                override fun onBillingSetupFinished(resultCode: Int) {
                    super.onBillingSetupFinished(resultCode)
                    if (!isResumed) {
                        isResumed = true
                        continuation.resume(Unit)
                        initBilling = true
                        Log.d(TAG, "initBilling.")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    super.onBillingServiceDisconnected()
                    if (!isResumed) {
                        isResumed = true
                        continuation.resume(Unit)
                        initBilling = true
                        Log.d(TAG, "initBillingFail.")
                    }
                }
            })
        } else {*/ //comment for billing
        continuation.resume(Unit)
        initBilling = true
        Log.d(TAG, "Not use billing.")
        //} //comment for billing
    }

    private fun loadBannerSplash(
        activity: AppCompatActivity?,
        lifecycleOwner: LifecycleOwner,
        frAdsBanner: FrameLayout?,
        listIdBannerSplash: MutableList<String>,
        adsKey: String
    ) {
        if (isShowBannerSplash) {
            Log.d(TAG, "loadBannerSplash.")
            //Reset TechManager to false
            if (useTechManagerOrDetectTestAd == DETECT_TEST_AD) {
                TechManager.getInstance().detectedTech(activity, false)
            }
            frAdsBanner?.visibility = View.VISIBLE
            val bannerBuilder = BannerBuilder(frAdsBanner)
            bannerBuilder.setListIdAdMain(listIdBannerSplash)
            bannerBuilder.callBack = object : BannerCallback() {
                override fun onAdImpression() {
                    super.onAdImpression()
                    if (useTechManagerOrDetectTestAd == DETECT_TEST_AD && TechManager.getInstance()
                            .isTech(activity) && !isDebug
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
            activity?.let { BannerManager(it, lifecycleOwner, bannerBuilder, adsKey) }
        } else {
            frAdsBanner?.visibility = View.GONE
        }
    }

    private fun showAdsSplash(
        activity: AppCompatActivity?,
        appOpenCallback: AppOpenCallback?,
        interCallback: InterCallback?
    ) {
        Log.d(TAG, "showAdsSplash check $isTimeout $isNoInternetAction")
        val bundle = Bundle()
        bundle.putString("isTimeout", "$isTimeout")
        bundle.putString("isNoInternetAction", "$isNoInternetAction")
        EventTrackingHelper.logEventWithMultipleParams(
            activity,
            normalizeFirebaseEventName("AsyncSplash_showAdsSplash"),
            bundle
        )
        if (!isTimeout && !isNoInternetAction) {
            isShowAdsSplash = true
            val time = (System.currentTimeMillis() - timeStartSplash) / 1000
            Log.d(TAG, "----------")
            Log.d(TAG, "showAdsSplash: Time show Ads = $time")
            adsSplash?.showAdsSplashApi(
                activity,
                appOpenCallback,
                interCallback,
                keyNativeAfterInterSplash,
                keyNativeAfterInterSplash
            )
            Log.d(TAG, "showAdsSplash.")
        }
    }
}