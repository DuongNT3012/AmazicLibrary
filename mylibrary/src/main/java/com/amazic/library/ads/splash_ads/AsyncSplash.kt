package com.amazic.library.ads.splash_ads

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import com.amazic.library.Utils.EventTrackingHelper
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
import com.amazic.library.iap.BillingCallback
import com.amazic.library.iap.IAPManager
import com.amazic.library.iap.ProductDetailCustom
import com.amazic.library.organic.TechManager
import com.amazic.library.ump.AdsConsentManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AsyncSplash {
    private val TAG = "AsyncSplash"
    private var isTech = false
    private var adsSplash: AdsSplash? = null
    private var jsonIdAdsDefault = ""
    private var adjustKey = ""
    private var linkServer = ""
    private var appId = ""
    private var isShowAdsSplash = false
    private var isTimeout = false
    private var isNoInternetAction = false
    private var initWelcomeBack = "Normal"
    private var welcomeBackClass: Class<*>? = null
    private var isShowBannerSplash = true
    private var frAdsBannerSplash: FrameLayout? = null
    private var listIdBannerSplash: MutableList<String> = arrayListOf("ca-app-pub-3940256099942544/6300978111")
    private var adsKey: String = ""
    private var listTurnOffRemoteKeys: MutableList<String> = mutableListOf()
    private var activity: AppCompatActivity? = null
    private var interCallback: InterCallback? = null
    private var appOpenCallback: AppOpenCallback? = null
    private var isDebug = false
    private var isUseBilling = false
    private var listProductDetailCustoms: ArrayList<ProductDetailCustom> = arrayListOf()
    private var timeOutSplash = 12000L
    private var isLoopAdsSplash = false
    private var useTechManagerOrDetectTestAd = DETECT_TEST_AD

    //1.use for log event time out 12s
    private var initRemoteConfig = false
    private var initAdmobApi = false
    private var initAdsConsentManager = false
    private var initBilling = false
    private var initTechManager = false

    //1.end
    //2.use for log event
    private var timeStartSplash = System.currentTimeMillis()
    //2.end

    companion object {
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
        this.isShowBannerSplash = true
        this.listIdBannerSplash = arrayListOf("ca-app-pub-3940256099942544/6300978111")
        this.listTurnOffRemoteKeys = mutableListOf()
        this.isDebug = false
        this.isUseBilling = false
        this.listProductDetailCustoms = arrayListOf()
        this.timeOutSplash = 12000L
        this.isLoopAdsSplash = false
        this.useTechManagerOrDetectTestAd = DETECT_TEST_AD
        this.initRemoteConfig = false
        this.initAdmobApi = false
        this.initAdsConsentManager = false
        this.initBilling = false
        this.initTechManager = false
    }

    fun getUserTechManagerOrDetectTestAd(): String {
        return this.useTechManagerOrDetectTestAd
    }

    fun setUseTechManager() {
        this.useTechManagerOrDetectTestAd = TECH_MANAGER
    }

    fun setUseDetectTestAd() {
        this.useTechManagerOrDetectTestAd = DETECT_TEST_AD
    }

    fun setLoopAdsSplash(isLoopAdsSplash: Boolean) {
        this.isLoopAdsSplash = isLoopAdsSplash
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

    fun setUseBilling(listProductDetailCustoms: ArrayList<ProductDetailCustom>) {
        this.isUseBilling = true
        this.listProductDetailCustoms.clear()
        this.listProductDetailCustoms.addAll(listProductDetailCustoms)
    }

    fun setDebug(isDebug: Boolean) {
        this.isDebug = isDebug
    }

    fun getDebug(): Boolean {
        return this.isDebug
    }

    fun checkShowSplashWhenFail() {
        if (adsSplash != null) {
            adsSplash?.onCheckShowSplashWhenFail(activity, appOpenCallback, interCallback)
        }
    }

    fun setInitResumeAdsNormal() {
        this.initWelcomeBack = "Normal"
    }

    fun setInitWelcomeBackBelowResumeAds(welcomeBackClass: Class<*>) {
        this.initWelcomeBack = "Below"
        this.welcomeBackClass = welcomeBackClass
    }

    fun setInitWelcomeBackAboveResumeAds(welcomeBackClass: Class<*>) {
        this.initWelcomeBack = "Above"
        this.welcomeBackClass = welcomeBackClass
    }

    fun getInitResumeAdsType(): String {
        return this.initWelcomeBack
    }

    fun setShowBannerSplash(isShowBannerSplash: Boolean, frAdsBannerSplash: FrameLayout, listIdBannerSplash: MutableList<String>, adsKey: String) {
        this.isShowBannerSplash = isShowBannerSplash
        this.frAdsBannerSplash = frAdsBannerSplash
        this.listIdBannerSplash.clear()
        this.listIdBannerSplash.addAll(listIdBannerSplash)
        this.adsKey = adsKey
    }

    fun setListTurnOffRemoteKeys(listTurnOffRemoteKeys: MutableList<String>) {
        this.listTurnOffRemoteKeys.clear()
        this.listTurnOffRemoteKeys.addAll(listTurnOffRemoteKeys)
    }

    fun handleAsync(context: Context, lifecycleOwner: LifecycleOwner, lifecycleCoroutineScope: LifecycleCoroutineScope, onNoInternetAction: () -> Unit) {
        Admob.getInstance().timeStart = System.currentTimeMillis()
        timeStartSplash = System.currentTimeMillis()
        lifecycleCoroutineScope.launch {
            delay(timeOutSplash)
            Log.d(TAG, "Timeout check $isShowAdsSplash $isNoInternetAction ")
            if (!isShowAdsSplash && !isNoInternetAction) {
                //1.log event timeout splash 12s
                val bundle = Bundle()
                bundle.putString("timeout_12s_detail", "${initAdmobApi}_${initRemoteConfig}_${initAdsConsentManager}_${initBilling}_${initTechManager}")
                bundle.putString("initAdmobApi", initAdmobApi.toString())
                bundle.putString("initRemoteConfig", initRemoteConfig.toString())
                bundle.putString("initAdsConsentManager", initAdsConsentManager.toString())
                bundle.putString("initBilling", initBilling.toString())
                bundle.putString("initTechManager", initTechManager.toString())
                EventTrackingHelper.logEventWithMultipleParams(context, "Timeout_Splash_12s", bundle)
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
            EventTrackingHelper.logEvent(activity, "splash_have_internet")
            lifecycleCoroutineScope.launch {
                val asyncAdmobApi = async { initAdmobApi(activity) }
                val asyncRemoteConfig = async { initRemoteConfig(activity) }
                val asyncUMP = async { initAdsConsentManager(activity) }
                val asyncBilling = async { initBilling() }
                val asyncTechManager = async { initTechManager(activity) }
                try {
                    //wait to load banner splash (banner splash fix id, don't use api to reduce time load splash)
                    awaitAll(asyncRemoteConfig, asyncUMP, asyncBilling, asyncTechManager)
                    if (useTechManagerOrDetectTestAd == TECH_MANAGER && isTech && !isDebug) {
                        turnOffSomeRemoteKeys(activity)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    //val asyncBannerSplash = async { loadBannerSplash(activity, lifecycleOwner, frAdsBannerSplash, listIdBannerSplash, adsKey) }
                    lifecycleCoroutineScope.launch {
                        loadBannerSplash(activity, lifecycleOwner, frAdsBannerSplash, listIdBannerSplash, adsKey)
                    }
                    try {
                        //wait to load inter or open splash
                        if (useTechManagerOrDetectTestAd == TECH_MANAGER) {
                            asyncAdmobApi.await()
                        } else {
                            //awaitAll(asyncBannerSplash, asyncAdmobApi)
                            asyncAdmobApi.await()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        lifecycleCoroutineScope.launch {
                            var rateAoaInterSplash: String =
                                RemoteConfigHelper.getInstance().get_config_string(activity, RemoteConfigHelper.rate_aoa_inter_splash)
                            if (rateAoaInterSplash.isEmpty()) {
                                rateAoaInterSplash = "0_100"
                            }
                            val isShowOpenSplash: Boolean = RemoteConfigHelper.getInstance().get_config(activity, RemoteConfigHelper.open_splash)
                            val isShowInterSplash: Boolean = RemoteConfigHelper.getInstance().get_config(activity, RemoteConfigHelper.inter_splash)
                            adsSplash = AdsSplash.init(isShowOpenSplash, isShowInterSplash, rateAoaInterSplash)
                            adsSplash?.setLoopAdsSplash(isLoopAdsSplash)
                            showAdsSplash(activity, appOpenCallback, interCallback)
                        }
                    }
                }
            }
        } else {
            if (!isShowAdsSplash && !isTimeout) {
                onNoInternetAction.invoke()
                isNoInternetAction = true
            }
        }
    }

    private fun turnOffSomeRemoteKeys(activity: AppCompatActivity?) {
        listTurnOffRemoteKeys.forEach {
            Log.d(TAG, "turnOffSomeRemoteKeys: $it")
            RemoteConfigHelper.getInstance().set_config(activity, it, false)
        }
    }

    private suspend fun initRemoteConfig(activity: AppCompatActivity?) = suspendCoroutine<Unit> { continuation ->
        var isResumed = false
        RemoteConfigHelper.getInstance().fetchAllKeysAndTypes(activity) {
            Admob.getInstance().showAllAds = RemoteConfigHelper.getInstance().get_config(activity, RemoteConfigHelper.show_all_ads)
            Admob.getInstance().setTimeInterval(
                RemoteConfigHelper.getInstance().get_config_long(activity, RemoteConfigHelper.interval_between_interstitial) * 1000
            )
            Admob.getInstance().setTimeIntervalFromStart(
                RemoteConfigHelper.getInstance().get_config_long(activity, RemoteConfigHelper.interval_interstitial_from_start) * 1000
            )
            if (!isResumed) {
                isResumed = true
                continuation.resume(Unit)
                initRemoteConfig = true
                Log.d(TAG, "initRemoteConfig.")
            }
        }
    }

    private suspend fun initAdsConsentManager(activity: AppCompatActivity?) = suspendCoroutine { continuation ->
        val adsConsentManager = AdsConsentManager(activity)
        var isResumed = false
        adsConsentManager.requestUMP {
            if (!isResumed) {
                isResumed = true
                if (it) {
                    Admob.getInstance().initAdmob(activity) {}
                    activity?.let { it1 -> AppOpenManager.getInstance().disableAppResumeWithActivity(it1.javaClass) }
                }
                continuation.resume(Unit)
                initAdsConsentManager = true
                Log.d(TAG, "initAdsConsentManager.")
            }
        }
    }

    private suspend fun initTechManager(activity: AppCompatActivity?) = suspendCoroutine<Unit> { continuation ->
        var isResumed = false
        if (useTechManagerOrDetectTestAd == TECH_MANAGER) {
            TechManager.getInstance().getResult(isDebug, activity, adjustKey) {
                if (it) {
                    isTech = true
                    AppOpenManager.getInstance().isEnableResume = false
                }
                if (!isResumed) {
                    isResumed = true
                    continuation.resume(Unit)
                    initTechManager = true
                    Log.d(TAG, "initTechManager.")
                }
            }
        } else {
            continuation.resume(Unit)
            initTechManager = true
            Log.d(TAG, "initTechManager else.")
        }
    }

    private suspend fun initAdmobApi(activity: AppCompatActivity?) = suspendCoroutine<Unit> { continuation ->
        AdmobApi.getInstance().jsonIdAdsDefault = jsonIdAdsDefault
        AdmobApi.getInstance().timeOutCallApi = 4000
        AdmobApi.getInstance().init(activity, linkServer, appId, object : ApiCallback() {
            private var isResumed = false
            override fun onReady() {
                super.onReady()
                when (initWelcomeBack) {
                    "Normal" -> {
                        if (AdmobApi.getInstance().listIDAppOpenResume.isNotEmpty()) {
                            AppOpenManager.getInstance().init(activity, AdmobApi.getInstance().listIDAppOpenResume)
                            activity?.let { AppOpenManager.getInstance().disableAppResumeWithActivity(it.javaClass) } //disable resume splash
                        }
                    }

                    "Below" -> {
                        if (AdmobApi.getInstance().getListIDByName(RemoteConfigHelper.resume_wb).isNotEmpty()) {
                            welcomeBackClass?.let {
                                AppOpenManager.getInstance()
                                    .initWelcomeBackBelowAdsResume(activity, AdmobApi.getInstance().getListIDByName(RemoteConfigHelper.resume_wb), it)
                                AppOpenManager.getInstance().disableAppResumeWithActivity(it) //disable resume welcome back
                            }
                            activity?.let { AppOpenManager.getInstance().disableAppResumeWithActivity(it.javaClass) } //disable resume splash
                        }
                    }

                    "Above" -> {
                        if (AdmobApi.getInstance().getListIDByName(RemoteConfigHelper.resume_wb).isNotEmpty()) {
                            welcomeBackClass?.let {
                                AppOpenManager.getInstance()
                                    .initWelcomeBackAboveAdsResume(activity, AdmobApi.getInstance().getListIDByName(RemoteConfigHelper.resume_wb), it)
                                AppOpenManager.getInstance().disableAppResumeWithActivity(it) //disable resume welcome back
                            }
                            activity?.let { AppOpenManager.getInstance().disableAppResumeWithActivity(it.javaClass) } //disable resume splash
                        }
                    }

                    else -> {
                        if (AdmobApi.getInstance().listIDAppOpenResume.isNotEmpty()) {
                            AppOpenManager.getInstance().init(activity, AdmobApi.getInstance().listIDAppOpenResume)
                            activity?.let { AppOpenManager.getInstance().disableAppResumeWithActivity(it.javaClass) } //disable resume splash
                        }
                    }
                }
                if (!isResumed) {
                    isResumed = true
                    continuation.resume(Unit)
                    initAdmobApi = true
                    Log.d(TAG, "initAdmobApi.")
                }
            }
        })
    }

    private suspend fun initBilling() = suspendCoroutine<Unit> { continuation ->
        if (isUseBilling) {
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
        } else {
            continuation.resume(Unit)
            initBilling = true
            Log.d(TAG, "Not use billing.")
        }
    }

    private fun loadBannerSplash(
        activity: AppCompatActivity?,
        lifecycleOwner: LifecycleOwner,
        frAdsBanner: FrameLayout?,
        listIdBannerSplash: MutableList<String>,
        adsKey: String
    ) {
        Log.d(TAG, "loadBannerSplash.")
        if (isShowBannerSplash) {
            //Reset TechManager to false
            if (useTechManagerOrDetectTestAd == DETECT_TEST_AD && isDebug) {
                TechManager.getInstance().detectedTech(activity, false)
            }
            frAdsBanner?.visibility = View.VISIBLE
            val bannerBuilder = BannerBuilder()
            bannerBuilder.setListId(listIdBannerSplash)
            bannerBuilder.callBack = object : BannerCallback() {
                override fun onAdImpression() {
                    super.onAdImpression()
                    if (useTechManagerOrDetectTestAd == DETECT_TEST_AD && TechManager.getInstance().isTech(activity) && !isDebug) {
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
            activity?.let { BannerManager(it, frAdsBanner, lifecycleOwner, bannerBuilder, adsKey) }
        } else {
            frAdsBanner?.visibility = View.GONE
        }
    }

    /*private suspend fun loadBannerSplash(
        activity: AppCompatActivity?,
        lifecycleOwner: LifecycleOwner,
        frAdsBanner: FrameLayout?,
        listIdBannerSplash: MutableList<String>,
        adsKey: String
    ) = suspendCoroutine<Unit> { continuation ->
        if (isShowBannerSplash) {
            //Reset TechManager to false
            if (useTechManagerOrDetectTestAd == DETECT_TEST_AD && isDebug) {
                TechManager.getInstance().detectedTech(activity, false)
            }
            frAdsBanner?.visibility = View.VISIBLE
            val bannerBuilder = BannerBuilder()
            bannerBuilder.setListId(listIdBannerSplash)
            bannerBuilder.callBack = object : BannerCallback() {
                private var isResumed = false
                override fun onAdImpression() {
                    super.onAdImpression()
                    if (useTechManagerOrDetectTestAd == DETECT_TEST_AD && TechManager.getInstance().isTech(activity) && !isDebug) {
                        turnOffSomeRemoteKeys(activity)
                    }
                    if (!isResumed) {
                        isResumed = true
                        continuation.resume(Unit)
                        Log.d(TAG, "showBannerSplash.")
                    }
                }

                override fun onAdFailedToLoad() {
                    super.onAdFailedToLoad()
                    frAdsBanner?.visibility = View.GONE
                    if (!isResumed) {
                        isResumed = true
                        continuation.resume(Unit)
                        Log.d(TAG, "loadFailBannerSplash.")
                    }
                }
            }
            activity?.let { BannerManager(it, frAdsBanner, lifecycleOwner, bannerBuilder, adsKey) }
        } else {
            frAdsBanner?.visibility = View.GONE
            continuation.resume(Unit)
        }
    }*/

    private fun showAdsSplash(activity: AppCompatActivity?, appOpenCallback: AppOpenCallback?, interCallback: InterCallback?) {
        Log.d(TAG, "showAdsSplash check $isTimeout $isNoInternetAction")
        if (!isTimeout && !isNoInternetAction) {
            adsSplash?.showAdsSplashApi(activity, appOpenCallback, interCallback)
            Log.d(TAG, "showAdsSplash.")
            isShowAdsSplash = true
        }
    }
}