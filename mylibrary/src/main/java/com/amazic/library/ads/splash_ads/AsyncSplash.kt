package com.amazic.library.ads.splash_ads

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

    //use for log event
    private var timeStartSplash = System.currentTimeMillis()

    companion object {
        private var INSTANCE: AsyncSplash? = null
        fun getInstance(): AsyncSplash {
            if (INSTANCE == null) {
                INSTANCE = AsyncSplash()
            }
            return INSTANCE as AsyncSplash
        }
    }

    fun init(activity: AppCompatActivity, appOpenCallback: AppOpenCallback, interCallback: InterCallback, adjustKey: String, linkServer: String, appId: String, jsonIdAdsDefault: String) {
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

    fun handleAsync(lifecycleOwner: LifecycleOwner, lifecycleCoroutineScope: LifecycleCoroutineScope, onNoInternetAction: () -> Unit) {
        Admob.getInstance().timeStart = System.currentTimeMillis()
        timeStartSplash = System.currentTimeMillis()
        lifecycleCoroutineScope.launch {
            delay(timeOutSplash)
            Log.d(TAG, "Timeout check $isShowAdsSplash $isNoInternetAction ")
            if (!isShowAdsSplash && !isNoInternetAction) {
                //increase splash open
                SharePreferenceHelper.setInt(activity, EventTrackingHelper.splash_open, SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1)
                //end increase splash open
                if (isTech) {
                    turnOffSomeRemoteKeys(activity)
                }
                interCallback?.onNextAction()
                Log.d(TAG, "Timeout Splash.")
                isTimeout = true
            }
            return@launch
        }
        if (NetworkUtil.isNetworkActive(activity)) {
            lifecycleCoroutineScope.launch {
                val asyncAdmobApi = async { initAdmobApi(activity) }
                val asyncRemoteConfig = async { initRemoteConfig(activity) }
                val asyncUMP = async { initAdsConsentManager(activity) }
                val asyncBilling = async { initBilling() }
                val asyncTechManager = async { initTechManager(activity) }
                try {
                    //wait to load banner splash (banner splash fix id, don't use api to reduce time load splash)
                    awaitAll(asyncRemoteConfig, asyncUMP, asyncBilling, asyncTechManager)
                    if (isTech) {
                        turnOffSomeRemoteKeys(activity)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    loadBannerSplash(activity, lifecycleOwner, frAdsBannerSplash, listIdBannerSplash, adsKey)
                }
                try {
                    //wait to load inter or open splash
                    asyncAdmobApi.await()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    var rateAoaInterSplash: String = RemoteConfigHelper.getInstance().get_config_string(activity, RemoteConfigHelper.rate_aoa_inter_splash)
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
        RemoteConfigHelper.getInstance().fetchAllKeysAndTypes(activity) {
            Admob.getInstance().showAllAds = RemoteConfigHelper.getInstance().get_config(activity, RemoteConfigHelper.show_all_ads)
            Admob.getInstance().setTimeInterval(
                RemoteConfigHelper.getInstance().get_config_long(activity, RemoteConfigHelper.interval_between_interstitial) * 1000
            )
            Admob.getInstance().setTimeIntervalFromStart(
                RemoteConfigHelper.getInstance().get_config_long(activity, RemoteConfigHelper.interval_interstitial_from_start) * 1000
            )
            continuation.resume(Unit)
            Log.d(TAG, "initRemoteConfig.")
        }
    }

    private suspend fun initAdsConsentManager(activity: AppCompatActivity?) = suspendCoroutine<Unit> { continuation ->
        val adsConsentManager = AdsConsentManager(activity)
        adsConsentManager.requestUMP {
            if (it) {
                Admob.getInstance().initAdmob(activity) {}
                activity?.let { it1 -> AppOpenManager.getInstance().disableAppResumeWithActivity(it1.javaClass) }
            }
            continuation.resume(Unit)
            Log.d(TAG, "initAdsConsentManager.")
        }
    }

    private suspend fun initTechManager(activity: AppCompatActivity?) = suspendCoroutine<Unit> { continuation ->
        TechManager.getInstance().getResult(isDebug, activity, adjustKey) {
            if (it) {
                isTech = true
                AppOpenManager.getInstance().isEnableResume = false
            }
            continuation.resume(Unit)
            Log.d(TAG, "initTechManager.")
        }
    }

    private suspend fun initAdmobApi(activity: AppCompatActivity?) = suspendCoroutine<Unit> { continuation ->
        AdmobApi.getInstance().jsonIdAdsDefault = jsonIdAdsDefault
        AdmobApi.getInstance().timeOutCallApi = 4000
        AdmobApi.getInstance().init(activity, linkServer, appId, object : ApiCallback() {
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
                                AppOpenManager.getInstance().initWelcomeBackBelowAdsResume(activity, AdmobApi.getInstance().getListIDByName(RemoteConfigHelper.resume_wb), it)
                                AppOpenManager.getInstance().disableAppResumeWithActivity(it) //disable resume welcome back
                            }
                            activity?.let { AppOpenManager.getInstance().disableAppResumeWithActivity(it.javaClass) } //disable resume splash
                        }
                    }

                    "Above" -> {
                        if (AdmobApi.getInstance().getListIDByName(RemoteConfigHelper.resume_wb).isNotEmpty()) {
                            welcomeBackClass?.let {
                                AppOpenManager.getInstance().initWelcomeBackAboveAdsResume(activity, AdmobApi.getInstance().getListIDByName(RemoteConfigHelper.resume_wb), it)
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
                continuation.resume(Unit)
                Log.d(TAG, "initAdmobApi.")
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
                        Log.d(TAG, "initBilling.")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    super.onBillingServiceDisconnected()
                    if (!isResumed) {
                        isResumed = true
                        continuation.resume(Unit)
                        Log.d(TAG, "initBillingFail.")
                    }
                }
            })
        } else {
            continuation.resume(Unit)
            Log.d(TAG, "Not use billing.")
        }
    }

    private fun loadBannerSplash(activity: AppCompatActivity?, lifecycleOwner: LifecycleOwner, frAdsBanner: FrameLayout?, listIdBannerSplash: MutableList<String>, adsKey: String) {
        if (isShowBannerSplash) {
            frAdsBanner?.visibility = View.VISIBLE
            val bannerBuilder = BannerBuilder()
            bannerBuilder.setListId(listIdBannerSplash)
            bannerBuilder.callBack = object : BannerCallback() {
                override fun onAdFailedToLoad() {
                    super.onAdFailedToLoad()
                    frAdsBanner?.visibility = View.GONE
                }
            }
            activity?.let { BannerManager(it, frAdsBanner, lifecycleOwner, bannerBuilder, adsKey) }
        } else {
            frAdsBanner?.visibility = View.GONE
        }
    }

    private fun showAdsSplash(activity: AppCompatActivity?, appOpenCallback: AppOpenCallback?, interCallback: InterCallback?) {
        Log.d(TAG, "showAdsSplash check $isTimeout $isNoInternetAction")
        if (!isTimeout && !isNoInternetAction) {
            adsSplash?.showAdsSplashApi(activity, appOpenCallback, interCallback)
            Log.d(TAG, "showAdsSplash.")
            isShowAdsSplash = true
        }
    }
}