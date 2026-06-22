package com.amazic.library.ads.splash_ads

import android.content.Context
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.amazic.library.Utils.RemoteConfigHelper
import com.amazic.library.ads.callback.AppOpenCallback
import com.amazic.library.ads.callback.InterCallback

/**
 * Holds all state/config fields for
 * Extracted from AsyncSplash to keep the main class lean.
 */
open class AsyncSplashConfig {

    var isTech = false
    var adsSplash: AdsSplash? = null
    var jsonIdAdsDefault = ""
    var timeOutCallApi = 4000
    var adjustKey = ""
    var linkServer = ""
    var appId = ""
    var isShowAdsSplash = false
    var isTimeout = false
    var isNoInternetAction = false
    var initWelcomeBack = "Normal"
    var welcomeBackClass: Class<*>? = null
    var isShowBannerSplash = false
    var frAdsBannerSplash: FrameLayout? = null
    var listIdBannerSplash: MutableList<String> =
        arrayListOf("ca-app-pub-3940256099942544/6300978111")
    var adsKey: String = ""
    val listTurnOffRemoteKeys: MutableList<String> = mutableListOf()
    var activity: AppCompatActivity? = null
    var interCallback: InterCallback? = null
    var appOpenCallback: AppOpenCallback? = null
    var isDebug = false
    var isUseBilling = false

    var timeOutSplash = 12000L
    var isLoopAdsSplash = false
    var useTechManagerOrDetectTestAd = AsyncSplash.DETECT_TEST_AD

    // flags used to log event timeout 12s
    var initRemoteConfig = false
    var initAdmobApi = false
    var initAdsConsentManager = false
    var initBilling = false
    var initTechManager = false
    var initAdmob = false

    var isUseIdAdsFromRemoteConfig = false
    var isPreloadResumeAds = true
    var isAsyncSplashAds = false

    // ad preloading
    var isUseAdPreloading = false
    var numberPreloading = 3
    var numberPreloadingSplash = 1

    var isShowNativeAfterInter = false

    var timeStartSplash = System.currentTimeMillis()

    var keyAdsInterSplash = "inter_splash"
    var keyAdsOpenSplash = "open_splash"
    var keyAdsOpenResume = ""
    var keyNativeAfterInter = "native_after_inter"

    var isUseAppUpdateManager = false
    var remoteKeyIdAdsServer = "id_ads"
    var onPrepareLoadInterOpenSplashAds: (() -> Unit?)? = null

    var timeSplashCheck = System.currentTimeMillis()

    var urlCheckInternetSpeed = "http://207.148.116.90/app/poster/avatar/sale5.png"

    var loadAndShowIdInterAdSplashAsync = false

    var timeOutCallIdRemoteConfig = 4000L
    var isSetId = false

    var keyIntervalBetweenInterstitial = "interval_between_interstitial"
    var keyIntervalInterstitialFromStart = "interval_interstitial_from_start"
    var timeStep1 = System.currentTimeMillis()
    var timeLastStep = System.currentTimeMillis()

    // timing metrics used in logEventDoneInit
    var timeInitAdmobApi = 0L
    var timeInitRemoteConfig = 0L
    var timeInitAdsConsentManager = 0L
    var timeInitTechManager = 0L

    fun resetVarToDefault() {
        isTech = false
        jsonIdAdsDefault = ""
        adjustKey = ""
        linkServer = ""
        appId = ""
        isShowAdsSplash = false
        isTimeout = false
        isNoInternetAction = false
        initWelcomeBack = "Normal"
        welcomeBackClass = null
        isShowBannerSplash = false
        listIdBannerSplash = arrayListOf("ca-app-pub-3940256099942544/6300978111")
        listTurnOffRemoteKeys.clear()
        isDebug = false
        isUseBilling = false
        timeOutSplash = 12000L
        isLoopAdsSplash = false
        useTechManagerOrDetectTestAd = AsyncSplash.DETECT_TEST_AD
        initRemoteConfig = false
        initAdmobApi = false
        initAdsConsentManager = false
        initBilling = false
        initTechManager = false
        initAdmob = false
        isUseIdAdsFromRemoteConfig = false
        isPreloadResumeAds = true
        numberPreloading = 3
        numberPreloadingSplash = 1
        isUseAdPreloading = false
        isAsyncSplashAds = false
        keyAdsInterSplash = "inter_splash"
        keyAdsOpenSplash = "open_splash"
        keyAdsOpenResume = ""
        loadAndShowIdInterAdSplashAsync = false
        timeOutCallIdRemoteConfig = 4000L
        isSetId = false
        keyIntervalBetweenInterstitial = "interval_between_interstitial"
        keyIntervalInterstitialFromStart = "interval_interstitial_from_start"
        keyNativeAfterInter = "native_after_inter"
        isShowNativeAfterInter = false
    }

    fun setUseDetectTestAd(){
        useTechManagerOrDetectTestAd = AsyncSplash.DETECT_TEST_AD
    }

    fun setKeyNumberPreloading(keyNumber: String) {
        numberPreloading = RemoteConfigHelper.getInstance()
            .get_config_long(activity, keyNumber).toInt()
    }

    fun getShowNativeAfterInter(): Boolean = isShowNativeAfterInter

    /** Use id ads from remote config (Key remote: id_ads) */
    fun setUseIdAdsFromRemoteConfig(remoteKey: String) {
        isUseIdAdsFromRemoteConfig = true
        remoteKeyIdAdsServer = remoteKey
        timeOutCallApi = 0
    }

    /** Call on resume of splash screen — reshow splash ads when show failed */
    fun checkShowSplashWhenFail() {
        adsSplash?.onCheckShowSplashWhenFail(activity, appOpenCallback, interCallback)
    }

    fun setInitResumeAdsNormal() {
        initWelcomeBack = "Normal"
        isPreloadResumeAds = true
    }

    fun setInitWelcomeBackBelowResumeAds(welcomeBackClass: Class<*>) {
        initWelcomeBack = "Below"
        this.welcomeBackClass = welcomeBackClass
        isPreloadResumeAds = true
    }

    fun setInitWelcomeBackAboveResumeAds(welcomeBackClass: Class<*>) {
        initWelcomeBack = "Above"
        this.welcomeBackClass = welcomeBackClass
        isPreloadResumeAds = false
    }

    fun getInitResumeAdsType(): String = initWelcomeBack

    fun setShowBannerSplash(
        frAdsBannerSplash: FrameLayout,
        listIdBannerSplash: MutableList<String>,
        adsKey: String
    ) {
        isShowBannerSplash = true
        this.frAdsBannerSplash = frAdsBannerSplash
        this.listIdBannerSplash.clear()
        this.listIdBannerSplash.addAll(listIdBannerSplash)
        this.adsKey = adsKey
    }

    fun setListTurnOffRemoteKeys(keys: MutableList<String>) {
        listTurnOffRemoteKeys.clear()
        listTurnOffRemoteKeys.addAll(keys)
    }

    fun turnOffSomeRemoteKeys(activity: Context?) {
        listTurnOffRemoteKeys.forEach {
            RemoteConfigHelper.getInstance().set_config(activity, it, false)
        }
    }
}
