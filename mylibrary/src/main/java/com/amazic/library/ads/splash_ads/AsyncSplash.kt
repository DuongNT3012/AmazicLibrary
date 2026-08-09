package com.amazic.library.ads.splash_ads

/*import com.amazic.library.iap.BillingCallback
import com.amazic.library.iap.IAPManager
import com.amazic.library.iap.ProductDetailCustom*/ //comment for billing
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
import com.amazic.library.ads.callback.BannerCallback
import com.amazic.library.ads.callback.InterCallback
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
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class  AsyncSplash {
    private val TAG = "AsyncSplash"
    private val config = AdmobAdsConfig()

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
        val time_between_step = "${System.currentTimeMillis() - config.timeLastStep}"
        val time_to_step = "${System.currentTimeMillis() - config.timeStep1}"
        bundle.putString("time_between_step", time_between_step)
        bundle.putString("time_to_step", time_to_step)
        EventTrackingHelper.logEventWithMultipleParams(
            config.activity,
            normalizeFirebaseEventName("AsyncSplash_$step"),
            bundle
        )
        config.timeLastStep = System.currentTimeMillis()
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
        interCallback: InterCallback,
        adjustKey: String,
        linkServer: String,
        appId: String,
        jsonIdAdsDefault: String
    ) {
        config.clear()
        config.activity = activity
        EventTrackingHelper.logEvent(activity, "${TAG}_INIT")
        config.timeStep1 = System.currentTimeMillis()
        config.timeLastStep = System.currentTimeMillis()
        config.adjustKey = adjustKey
        config.jsonIdAdsDefault = jsonIdAdsDefault
        config.linkServer = linkServer
        config.appId = appId
        config.interCallback = interCallback
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

    fun getLoadAndShowIdInterAdSplashAsync(): Boolean {
        return config.loadAndShowIdInterAdSplashAsync
    }

    fun setLoadAndShowIdInterAdSplashAsync() {
        config.loadAndShowIdInterAdSplashAsync = true
    }

    fun setUrlCheckInternetSpeed(urlCheckInternetSpeed: String) {
        config.urlCheckInternetSpeed = urlCheckInternetSpeed
    }

    fun setTimeSplashCheck() {
        config.timeSplashCheck = System.currentTimeMillis()
    }

    fun getTimeSplashCheck(): Long {
        return config.timeSplashCheck
    }

    fun setOnPrepareLoadInterOpenSplashAds(onPrepareLoadInterOpenSplashAds: () -> Unit) {
        config.onPrepareLoadInterOpenSplashAds = onPrepareLoadInterOpenSplashAds
    }

//    fun setOnInitAdmobDone(onInitAdmobDone: () -> Unit) {
//        this.onInitAdmobDone = onInitAdmobDone
//    }

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

    fun getKeyAdsOpenResume(): String {
        return config.keyAdsOpenResume
    }

    fun setKeyNativeAfterInterSplash(key: String) {
        config.keyNativeAfterInterSplash = key
    }

    fun setKeyNativeFullMetaSplash(key: String) {
        config.keyNativeFullMetaSplash = key
    }

    fun getKeyNativeFullMetaSplash(): String {
        return config.keyNativeFullMetaSplash
    }

    fun setKeyNativeFullAdmobSplash(key: String) {
        config.keyNativeFullAdmobSplash = key
    }

    fun getKeyNativeFullAdmobSplash(): String {
        return config.keyNativeFullAdmobSplash
    }

    fun setIdNativeMetaSplash(id: String) {
        config.idNativeMetaSplash = id
    }

    fun getIdNativeMetaSplash(): String {
        return config.idNativeMetaSplash
    }

    fun setNumberNativeFullShowSplash(count: Int) {
        config.numberNativeFullShowSplash = count
    }

    fun getNumberNativeFullShowSplash(): Int {
        return config.numberNativeFullShowSplash
    }

    fun setAsyncSplashAds() { //Show splash ads without wait any thing
        config.isAsyncSplashAds = true
    }

    fun setPreloadResumeAds(isPreloadResumeAds: Boolean) { //Set can preload resume ads or not (Note: set false when you need to load and show resume ads)
        config.isPreloadResumeAds = isPreloadResumeAds
    }

    fun getPreloadResumeAds(): Boolean {
        return config.isPreloadResumeAds
    }

    fun setKeyNumberPreloading(keyNumber: String) {
        var number: Int =
            RemoteConfigHelper.getInstance().get_config_long(config.activity, keyNumber).toInt()
        config.numberPreloading = number
    }

    fun getNumberPreloading(): Int {
        return config.numberPreloading
    }

    fun setNumberPreloadingSplash(number: Int) {
        config.numberPreloadingSplash = number
    }

    fun getNumberPreloadingSplash(): Int {
        return config.numberPreloadingSplash
    }

    fun setUseAdPreloading(isUsePreLoading: Boolean) {
        config.isUseAdPreloading = isUsePreLoading
    }

    fun getUseAdPreloading(): Boolean {
        return config.isUseAdPreloading
    }

    fun setShowNativeAfterInter(isShowNativeAfterInter: Boolean) {
        config.isShowNativeAfterInter = isShowNativeAfterInter
    }

    fun getShowNativeAfterInter(): Boolean {
        return config.isShowNativeAfterInter
    }

//    fun setUseNativeSplash(isUse: Boolean) {
//        this.isUseNativeSplash = isUse
//    }
//
//    fun getUseNativeSplash(): Boolean {
//        return this.isUseNativeSplash
//    }

    fun setUseNativeSplashMeta(isUse: Boolean) {
        config.isUseNativeSplashMeta = isUse
    }

    fun getIsUseNativeSplashMeta(): Boolean {
        return config.isUseNativeSplashMeta
    }

    fun setUseNativeFullSplash(isUse: Boolean) {
        config.isUseNativeFullSplash = isUse
    }

    fun getUseNativeFullSplash(): Boolean {
        return config.isUseNativeFullSplash
    }

    fun setUseCacheDataCallSplash(isUse: Boolean) {
        config.isUseCacheDataCallSplash = isUse
    }

    fun getUseCacheDataCallSplash(): Boolean {
        return config.isUseCacheDataCallSplash
    }

    fun setUseDetectionVPNOrEmulator(isUse: Boolean) {
        config.isUseDetectionVPNOrEmulator = isUse
    }

    fun getUseDetectionVPNOrEmulator(): Boolean {
        return config.isUseDetectionVPNOrEmulator
    }

    fun setUseNativeFullSplashAdmobWhenMetaFail(isUse: Boolean) {
        config.isUseNativeFullSplashAdmobWhenMetaFail = isUse
    }

    fun getUseNativeFullSplashAdmobWhenMetaFail(): Boolean {
        return config.isUseNativeFullSplashAdmobWhenMetaFail
    }

    fun setUseIdAdsFromRemoteConfig(remoteKeyIdAdsServer: String) { //Use id ads from remote config or not (Key remote: id_ads)
        config.isUseIdAdsFromRemoteConfig = true
        config.remoteKeyIdAdsServer = remoteKeyIdAdsServer
        config.timeOutCallApi = 0
    }

    fun setTimeOutCallApi(timeOutCallApi: Int) { //Timeout call id ads from server
        config.timeOutCallApi = timeOutCallApi
    }

    fun getUserTechManagerOrDetectTestAd(): String {
        return config.useTechManagerOrDetectTestAd
    }

    fun setUseTechManager() { //Use TechManager (Organic) or not
        config.useTechManagerOrDetectTestAd = TECH_MANAGER
    }

    fun setUseDetectTestAd() {
        config.useTechManagerOrDetectTestAd = DETECT_TEST_AD
    }

    fun setLoopAdsSplash() { //Load loop splash ads when load fail if time splash < 8s
        config.isLoopAdsSplash = true
    }

    fun getShowAdsSplash(): Boolean {
        return config.isShowAdsSplash
    }

    fun getTimeout(): Boolean {
        return config.isTimeout
    }

    fun getNoInternetAction(): Boolean {
        return config.isNoInternetAction
    }

    fun getTimeStartSplash(): Long {
        return config.timeStartSplash
    }

    fun setTimeOutSplash(timeOutSplash: Long) {
        config.timeOutSplash = timeOutSplash
    }

    /*fun setUseBilling(listProductDetailCustoms: ArrayList<ProductDetailCustom>) { //If need use IAP
        config.isUseBilling = true
        this.listProductDetailCustoms.clear()
        this.listProductDetailCustoms.addAll(listProductDetailCustoms)
    }*/ //comment for billing

    fun setDebug(isDebug: Boolean) { //Use for TechManager or DetectTestAd
        config.isDebug = isDebug
    }

    fun getDebug(): Boolean {
        return config.isDebug
    }

    fun checkShowSplashWhenFail() { //Call on resume of splash screen (Reshow splash ads when show fail)
        AdsSplash.getInstance().onCheckShowSplashWhenFail(config.activity, config.interCallback)
    }

    fun setInitResumeAdsNormal() {
        config.initWelcomeBack = "Normal"
        config.isPreloadResumeAds = true
    }

    fun setInitWelcomeBackBelowResumeAds(welcomeBackClass: Class<*>) {
        config.initWelcomeBack = "Below"
        config.welcomeBackClass = welcomeBackClass
        config.isPreloadResumeAds = true
    }

    fun setInitWelcomeBackAboveResumeAds(welcomeBackClass: Class<*>) {
        config.initWelcomeBack = "Above"
        config.welcomeBackClass = welcomeBackClass
        config.isPreloadResumeAds = false
    }

    fun getInitResumeAdsType(): String {
        return config.initWelcomeBack
    }

    fun setShowBannerSplash(
        frAdsBannerSplash: FrameLayout,
        listIdBannerSplash: MutableList<String>,
        adsKey: String
    ) {
        config.isShowBannerSplash = true
        config.frAdsBannerSplash = frAdsBannerSplash
        config.listIdBannerSplash.clear()
        config.listIdBannerSplash.addAll(listIdBannerSplash)
        config.adsKey = adsKey
    }

    fun setListTurnOffRemoteKeys(listTurnOffRemoteKeys: MutableList<String>) {
        config.listTurnOffRemoteKeys.clear()
        config.listTurnOffRemoteKeys.addAll(listTurnOffRemoteKeys)
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
            config.activity,
            normalizeFirebaseEventName("AsyncSplash_doneInit"),
            bundle
        )
        config.timeLastStep = System.currentTimeMillis()
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
        lifecycleCoroutineScope.launch {
            delay(config.timeOutSplash)
            Log.d(TAG, "Timeout check ${config.isShowAdsSplash} ${config.isNoInternetAction} ")
            logEventStep("AsyncTimeout")

            val bundleEvent = Bundle()
            bundleEvent.putString("isTimeout", "${config.isTimeout}")
            bundleEvent.putString("isNoInternetAction", "${config.isNoInternetAction}")
            EventTrackingHelper.logEventWithMultipleParams(
                config.activity,
                normalizeFirebaseEventName("AsyncSplash_VAsyncTimeout"),
                bundleEvent
            )
            if (!config.isShowAdsSplash && !config.isNoInternetAction) {
                //1.log event timeout splash 12s
                val bundle = Bundle()
                bundle.putString(
                    "timeout_splash_next_screen_detail",
                    "${config.initAdmobApi}_${config.initRemoteConfig}_${config.initAdsConsentManager}_${config.initBilling}_${config.initTechManager}"
                )
                bundle.putString("initAdmobApi", config.initAdmobApi.toString())
                bundle.putString("initRemoteConfig", config.initRemoteConfig.toString())
                bundle.putString("initAdsConsentManager", config.initAdsConsentManager.toString())
                bundle.putString("initBilling", config.initBilling.toString())
                bundle.putString("initTechManager", config.initTechManager.toString())
                EventTrackingHelper.logEventWithMultipleParams(
                    context,
                    "timeout_splash_next_screen",
                    bundle
                )
                //1.end log event timeout splash 12s
                //2.increase splash open
                SharePreferenceHelper.setInt(
                    config.activity,
                    EventTrackingHelper.splash_open,
                    SharePreferenceHelper.getInt(config.activity, EventTrackingHelper.splash_open, 1) + 1
                )
                //2.end increase splash open
                if (config.useTechManagerOrDetectTestAd == TECH_MANAGER) {
                    if (config.isTech && !config.isDebug) {
                        turnOffSomeRemoteKeys(config.activity)
                    }
                } else if (config.useTechManagerOrDetectTestAd == DETECT_TEST_AD) {
                    if (TechManager.getInstance().isTech(config.activity) && !config.isDebug) {
                        turnOffSomeRemoteKeys(config.activity)
                    }
                }
                config.interCallback?.onNextAction()
                Log.d(TAG, "Timeout Splash.")
                config.isTimeout = true
            }
            return@launch
        }
        if (NetworkUtil.isNetworkActive(config.activity)) {
            logEventStep("AsyncInternet")
            EventTrackingHelper.logEvent(config.activity, "splash_have_internet_original")
            lifecycleCoroutineScope.launch {
                logEventStep("StartAsyncInit")
                val asyncAdmobApi = async { initAdmobApi(config.activity) }
                val asyncRemoteConfig = async { initRemoteConfig(config.activity) }
                val asyncUMP = async { initAdsConsentManager(config.activity) }
                val asyncBilling = async { initBilling() }
                val asyncTechManager = async { initTechManager(config.activity) }
                try {
                    //wait to load banner splash (banner splash fix id, don't use api to reduce time load splash)
                    if (!config.isAsyncSplashAds) {
                        awaitAll(asyncRemoteConfig, asyncUMP, asyncBilling, asyncTechManager)
                        if (config.useTechManagerOrDetectTestAd == TECH_MANAGER && config.isTech && !config.isDebug) {
                            turnOffSomeRemoteKeys(config.activity)
                        }
                    } else {
                        awaitAll(asyncUMP)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    logEventStep("DoneAsyncInit")
                    loadBannerSplash(
                        config.activity,
                        lifecycleOwner,
                        config.frAdsBannerSplash,
                        config.listIdBannerSplash,
                        config.adsKey
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
                        val timeAsync = (System.currentTimeMillis() - config.timeSplashCheck) / 1000
                        EventTrackingHelper.logEventWithAParam(
                            config.activity,
                            time_splash_check,
                            time_splash_check,
                            timeAsync.toString()
                        )
                        config.onPrepareLoadInterOpenSplashAds?.invoke()
//                        lifecycleCoroutineScope.launch {
                        logEventStep("StartAdSplash")
                        var rateAoaInterSplash: String =
                            RemoteConfigHelper.getInstance().get_config_string(
                                config.activity,
                                RemoteConfigHelper.rate_aoa_inter_splash
                            )
                        if (rateAoaInterSplash.isEmpty()) {
                            rateAoaInterSplash = "0_100"
                        }
                        val isShowOpenSplash: Boolean = RemoteConfigHelper.getInstance()
                            .get_config(config.activity, config.keyAdsOpenSplash)
                        val isShowInterSplash: Boolean = RemoteConfigHelper.getInstance()
                            .get_config(config.activity, config.keyAdsInterSplash)
                        showAdsSplash(config.activity, config.interCallback)
//                        }
                        if (config.isAsyncSplashAds) {
                            awaitAll(asyncRemoteConfig, asyncTechManager)
                            if (config.useTechManagerOrDetectTestAd == TECH_MANAGER && config.isTech && !config.isDebug) {
                                turnOffSomeRemoteKeys(config.activity)
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
            if (!config.isShowAdsSplash && !config.isTimeout) {
                onNoInternetAction.invoke()
                config.isNoInternetAction = true
            }
        }
    }

    fun turnOffSomeRemoteKeys(activity: Context?) {
        config.listTurnOffRemoteKeys.forEach {
            Log.d(TAG, "turnOffSomeRemoteKeys: $it")
            RemoteConfigHelper.getInstance().set_config(activity, it, false)
        }
    }

    private val PREF_REMOTE_FETCHED_FLAG = "remote_fetched_once"
    private var timeInitRemoteConfig = 0L
    private suspend fun initRemoteConfig(
        activity: AppCompatActivity?
    ) = suspendCoroutine<Unit> { continuation ->
        timeInitRemoteConfig = 0L
        if (config.isUseAppUpdateManager) {
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
                if (config.isUseIdAdsFromRemoteConfig && !config.isSetId) {
                    AdmobApi.getInstance().jsonIdAdsDefault = config.jsonIdAdsDefault
                    AdmobApi.getInstance().convertJsonIdAdsDefaultToList(config.jsonIdAdsDefault)
                    config.isSetId = true
                    Log.d(
                        TAG,
                        "Timeout Remote Config: Id ads size = ${AdmobApi.getInstance().listAdsSize}"
                    )
                    EventTrackingHelper.logEvent(activity, "timeout_call_id_remote_config")
                    initWelcomeBack(activity)//17.09.2025
                    timeInitRemoteConfig = System.currentTimeMillis() - startTimeInitRemoteConfig
                }
            }, config.timeOutCallIdRemoteConfig)

            Log.d(
                TAG,
                "check initRemoteConfig: hasBeenFetchedBefore = $hasBeenFetchedBefore, isUseCacheDataCallSplash = ${config.isUseCacheDataCallSplash}"
            )

            if (hasBeenFetchedBefore && config.isUseCacheDataCallSplash) {
                Log.d(TAG, "initRemoteConfig: using SharedPreferences cache")

                // apply config từ SP ngay (get_config đọc từ SP → instant)
                Admob.getInstance().showAllAds = RemoteConfigHelper.getInstance()
                    .get_config(activity, RemoteConfigHelper.show_all_ads)
                Admob.getInstance().setTimeInterval(
                    RemoteConfigHelper.getInstance().get_config_long(
                        activity, config.keyIntervalBetweenInterstitial
                    ) * 1000, true
                )
                Admob.getInstance().setTimeIntervalFromStart(
                    RemoteConfigHelper.getInstance().get_config_long(
                        activity, config.keyIntervalInterstitialFromStart
                    ) * 1000
                )
                if (config.isUseIdAdsFromRemoteConfig && !config.isSetId) {
                    // get_config_string cũng đọc từ SP → instant
                    val jsonFromSP = RemoteConfigHelper.getInstance()
                        .get_config_string(activity, RemoteConfigHelper.id_ads)
                    if (jsonFromSP.contains("app_id")) {
                        AdmobApi.getInstance().jsonIdAdsDefault = jsonFromSP
                        AdmobApi.getInstance().convertJsonIdAdsDefaultToList(jsonFromSP)
                        config.isSetId = true
                        EventTrackingHelper.logEvent(activity, "set_id_remote_config")
                    } else {
                        AdmobApi.getInstance().jsonIdAdsDefault = config.jsonIdAdsDefault
                        AdmobApi.getInstance().convertJsonIdAdsDefaultToList(config.jsonIdAdsDefault)
                        config.isSetId = true
                    }
                    initWelcomeBack(activity)
                    EventTrackingHelper.logEvent(
                        activity,
                        "set_id_default_case_fail_remote"
                    )
                }

                timeInitRemoteConfig = System.currentTimeMillis() - startTimeInitRemoteConfig
                config.initRemoteConfig = true
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
                    if (isSuccess && config.isUseCacheDataCallSplash) {
                        prefs?.edit()?.putBoolean(PREF_REMOTE_FETCHED_FLAG, true)?.apply()
                    }
                    //Handle remote config id ads
                    if (config.isUseIdAdsFromRemoteConfig && !config.isSetId) {
                        val jsonIdAdsFromRemoteConfig = RemoteConfigHelper.getInstance()
                            .get_config_string(activity, RemoteConfigHelper.id_ads)
                        if (jsonIdAdsFromRemoteConfig.contains("app_id") && isSuccess) { //get id ads from remote config successfully
                            AdmobApi.getInstance().jsonIdAdsDefault = jsonIdAdsFromRemoteConfig
                            AdmobApi.getInstance()
                                .convertJsonIdAdsDefaultToList(jsonIdAdsFromRemoteConfig)
                            config.isSetId = true
                            Log.d(
                                TAG,
                                "Set id ads from remote: Id ads size = ${AdmobApi.getInstance().listAdsSize}"
                            )
                            EventTrackingHelper.logEvent(activity, "set_id_remote_config")
                        } else {
                            AdmobApi.getInstance().jsonIdAdsDefault = config.jsonIdAdsDefault
                            AdmobApi.getInstance().convertJsonIdAdsDefaultToList(config.jsonIdAdsDefault)
                            config.isSetId = true
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
                        config.initRemoteConfig = true
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
                    config.initAdsConsentManager = true
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
            if (config.useTechManagerOrDetectTestAd == TECH_MANAGER) {
                TechManager.getInstance().getResult(config.isDebug, activity, config.adjustKey) {
                    if (it) {
                        config.isTech = true
                        AppOpenManager.getInstance().isEnableResume = false
                    }
                    timeInitTechManager = System.currentTimeMillis() - startTimeInitTechManager
                    if (!isResumed) {
                        isResumed = true
                        continuation.resume(Unit)
                        config.initTechManager = true
                        Log.d(TAG, "initTechManager.")
                    }
                }
            } else {
                timeInitTechManager = System.currentTimeMillis() - startTimeInitTechManager
                continuation.resume(Unit)
                config.initTechManager = true
                Log.d(TAG, "initTechManager else.")
            }
        }

    private var timeInitAdmobApi = 0L
    private val PREF_CACHED_AD_IDS = "cached_ad_ids"
    private suspend fun initAdmobApi(activity: AppCompatActivity?) =
        suspendCoroutine<Unit> { continuation ->
            if (!config.isUseIdAdsFromRemoteConfig) {
                val startTimeInitAdmobApi = System.currentTimeMillis()
                AdmobApi.getInstance().jsonIdAdsDefault = config.jsonIdAdsDefault
                AdmobApi.getInstance().timeOutCallApi = config.timeOutCallApi

                val prefs = activity?.getSharedPreferences("MY_PRE", Context.MODE_PRIVATE)
                val cachedJson = prefs?.getString(PREF_CACHED_AD_IDS, "") ?: ""

                Log.d(
                    TAG,
                    "check initAdmobApi: cachedJson.isNotEmpty() = ${cachedJson.isNotEmpty()}, isUseCacheDataCallSplash = ${config.isUseCacheDataCallSplash}"
                )
                if (cachedJson.isNotEmpty() && config.isUseCacheDataCallSplash) {
                    Log.d(TAG, "initAdmobApi: using cache id Ads")

                    AdmobApi.getInstance().convertJsonIdAdsDefaultToList(cachedJson)
                    initWelcomeBack(activity)
                    config.initAdmobApi = true
                    timeInitAdmobApi = System.currentTimeMillis() - startTimeInitAdmobApi
                    continuation.resume(Unit)
                    Log.d(TAG, "initAdmobApi: END using cache id Ads - ${timeInitAdmobApi / 1000}")


                    // Refresh ngầm cho session sau
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            withContext(Dispatchers.Main) {
                                AdmobApi.getInstance().refreshCacheOnly(
                                    activity,
                                    config.linkServer,
                                    config.appId,
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
                        .init(activity, config.linkServer, config.appId, object : ApiCallback() {
                            private var isResumed = false
                            override fun onReady() {
                                super.onReady()
                                //save cached json id
                                if (config.isUseCacheDataCallSplash) {
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
                                    config.initAdmobApi = true
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
        when (config.initWelcomeBack) {
            "Normal" -> {
                val listIdResume = mutableListOf<String>()
                if (config.keyAdsOpenResume.isNotEmpty()) {
                    listIdResume.addAll(AdmobApi.getInstance().getListIDByName(config.keyAdsOpenResume))
                } else {
                    listIdResume.addAll(AdmobApi.getInstance().listIDAppOpenResume)
                    config.keyAdsOpenResume = "open_resume"
                }
                if (listIdResume.isNotEmpty()) {
                    if (!config.isUseAdPreloading) {
                        Log.d(
                            TAG,
                            "APP Open Preload: normal - isUseAdPreloading = ${config.isUseAdPreloading}, isPreloadResumeAds = ${config.isPreloadResumeAds}"
                        )
                        if (config.isPreloadResumeAds) {
                            AppOpenManager.getInstance()
                                .loadAdNotCheckRemote(activity, listIdResume, config.keyAdsOpenResume)
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
                if (config.keyAdsOpenResume.isNotEmpty()) {
                    listIdResume.addAll(AdmobApi.getInstance().getListIDByName(config.keyAdsOpenResume))
                } else {
                    listIdResume.addAll(
                        AdmobApi.getInstance().getListIDByName(RemoteConfigHelper.resume_wb)
                    )
                    config.keyAdsOpenResume = "resume_wb"
                }
                if (listIdResume.isNotEmpty()) {
                    if (!config.isUseAdPreloading) {
                        Log.d(
                            TAG,
                            "APP Open Preload: below - isUseAdPreloading = ${config.isUseAdPreloading} ,isPreloadResumeAds = ${config.isPreloadResumeAds}"
                        )
                        if (config.isPreloadResumeAds) {
                            AppOpenManager.getInstance()
                                .loadAdNotCheckRemote(activity, listIdResume, config.keyAdsOpenResume)
                        }
                    }
                    config.welcomeBackClass?.let {
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
                if (config.keyAdsOpenResume.isNotEmpty()) {
                    listIdResume.addAll(AdmobApi.getInstance().getListIDByName(config.keyAdsOpenResume))
                } else {
                    listIdResume.addAll(
                        AdmobApi.getInstance().getListIDByName(RemoteConfigHelper.resume_wb)
                    )
                    config.keyAdsOpenResume = "resume_wb"
                }
                if (listIdResume.isNotEmpty()) {
                    if (!config.isUseAdPreloading) {
                        Log.d(
                            TAG,
                            "APP Open Preload: above - isUseAdPreloading = ${config.isUseAdPreloading}, isPreloadResumeAds = ${config.isPreloadResumeAds}"
                        )
                        if (config.isPreloadResumeAds) {
                            AppOpenManager.getInstance()
                                .loadAdNotCheckRemote(activity, listIdResume, config.keyAdsOpenResume)
                        }
                    }
                    config.welcomeBackClass?.let {
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
                if (config.keyAdsOpenResume.isNotEmpty()) {
                    listIdResume.addAll(AdmobApi.getInstance().getListIDByName(config.keyAdsOpenResume))
                } else {
                    listIdResume.addAll(AdmobApi.getInstance().listIDAppOpenResume)
                    config.keyAdsOpenResume = "open_resume"
                }
                if (listIdResume.isNotEmpty()) {
                    if (!config.isUseAdPreloading) {
                        Log.d(
                            TAG,
                            "APP Open Preload: else - isUseAdPreloading = ${config.isUseAdPreloading}, isPreloadResumeAds = ${config.isPreloadResumeAds}"
                        )
                        if (config.isPreloadResumeAds) {
                            AppOpenManager.getInstance()
                                .loadAdNotCheckRemote(activity, listIdResume, config.keyAdsOpenResume)
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
        if (config.isUseAdPreloading) {
            val listIdResume = mutableListOf<String>()
            if (config.keyAdsOpenResume.isNotEmpty()) {
                listIdResume.addAll(AdmobApi.getInstance().getListIDByName(config.keyAdsOpenResume))
            } else {
                listIdResume.addAll(AdmobApi.getInstance().listIDAppOpenResume)
                config.keyAdsOpenResume = "open_resume"
            }
            if (listIdResume.isNotEmpty()) {
                Log.d(
                    TAG,
                    "APP Open Preload: start loadAdPreloadNotCheckRemote"
                )
                AppOpenManager.getInstance().loadAdPreloadNotCheckRemote(
                    config.activity,
                    listIdResume,
                    config.keyAdsOpenResume
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
        config.initBilling = true
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
        if (config.isShowBannerSplash) {
            Log.d(TAG, "loadBannerSplash.")
            //Reset TechManager to false
            if (config.useTechManagerOrDetectTestAd == DETECT_TEST_AD) {
                TechManager.getInstance().detectedTech(activity, false)
            }
            frAdsBanner?.visibility = View.VISIBLE
            val bannerBuilder = BannerBuilder(frAdsBanner)
            bannerBuilder.setListIdAdMain(listIdBannerSplash)
            bannerBuilder.callBack = object : BannerCallback() {
                override fun onAdImpression() {
                    super.onAdImpression()
                    if (config.useTechManagerOrDetectTestAd == DETECT_TEST_AD && TechManager.getInstance()
                            .isTech(activity) && !config.isDebug
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
        interCallback: InterCallback?
    ) {
        Log.d(TAG, "showAdsSplash check ${config.isTimeout} ${config.isNoInternetAction}")
        val bundle = Bundle()
        bundle.putString("isTimeout", "${config.isTimeout}")
        bundle.putString("isNoInternetAction", "${config.isNoInternetAction}")
        EventTrackingHelper.logEventWithMultipleParams(
            activity,
            normalizeFirebaseEventName("AsyncSplash_showAdsSplash"),
            bundle
        )
        if (!config.isTimeout && !config.isNoInternetAction) {
            config.isShowAdsSplash = true
            val time = (System.currentTimeMillis() - config.timeStartSplash) / 1000
            Log.d(TAG, "----------")
            Log.d(TAG, "showAdsSplash: Time show Ads = $time")
            AdsSplash.getInstance().loadAndShowInterAdSplashDelay(
                activity,
                AdmobApi.getInstance().getListIDByName(config.keyAdsInterSplash),
                interCallback,
                config.keyNativeAfterInterSplash,
                config.keyNativeAfterInterSplash
            )
            Log.d(TAG, "showAdsSplash.")
        }
    }
}