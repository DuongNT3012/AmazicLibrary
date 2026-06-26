package com.amazic.library.ads.splash_ads

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import com.amazic.library.Utils.EventTrackingHelper
import com.amazic.library.Utils.EventTrackingHelper.time_splash_check
import com.amazic.library.Utils.NetworkUtil
import com.amazic.library.Utils.SharePreferenceHelper
import com.amazic.library.ads.admob.Admob
import com.amazic.library.ads.callback.AppOpenCallback
import com.amazic.library.ads.callback.InterCallback
import com.amazic.library.application.AdsApplication
import com.amazic.library.organic.TechManager
import com.amazic.mylibrary.R
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * AsyncSplash orchestrates the splash-screen initialisation flow:
 *   1. Async init of AdmobApi, RemoteConfig, UMP consent, Billing, TechManager
 *   2. Banner splash load
 *   3. Interstitial / App-Open splash show
 *   4. Resume-ad preload
 *
 * State lives in [AsyncSplashConfig].
 * Setters/getters are in AsyncSplashSetters.kt (extension functions).
 * Init coroutines are in AsyncSplashInitializers.kt.
 * Welcome-back / resume logic is in AsyncSplashWelcomeBack.kt.
 * Ad-display helpers are in AsyncSplashAdsHelper.kt.
 * Event-logging helpers are in AsyncSplashEventLogger.kt.
 */
class AsyncSplash : AsyncSplashConfig() {

    private val TAG = "AsyncSplash"

    companion object {
        const val TECH_MANAGER = "TechManager"
        const val DETECT_TEST_AD = "DetectTestAd"

        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: AsyncSplash? = null

        fun getInstance(): AsyncSplash {
            if (INSTANCE == null) {
                INSTANCE = AsyncSplash()
            }
            return INSTANCE as AsyncSplash
        }
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    fun init(
        activity: AppCompatActivity,
        appOpenCallback: AppOpenCallback,
        interCallback: InterCallback,
        adjustKey: String,
        linkServer: String,
        type: String,
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
        this.initAdmobType = type
        this.appOpenCallback = appOpenCallback
        this.interCallback = interCallback
        if (type != INIT_ADMOB_INT_API && type != INIT_ADMOB_IN_FIREBASE) {
            throw IllegalAccessException("type: $type must be INIT_ADMOB_INT_API or INIT_ADMOB_IN_FIREBASE")
        }
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

        // --- Timeout watchdog ---
        lifecycleCoroutineScope.launch {
            delay(timeOutInitAdmobInSplash)
            if (!initAdmob && activity != null) {
                if (Admob.getInstance().appID.isEmpty()) {
                    Admob.getInstance().appID = (activity!!.application as AdsApplication).getAppAdsIDDefault()
                }
                Admob.getInstance().initAdmob(activity) {
                    initAdmob = it
                }
            }
            delay(timeOutSplash - timeOutInitAdmobInSplash)
            Log.d(TAG, "Timeout check $isShowAdsSplash $isNoInternetAction")
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
                logTimeoutEvent(context)
                incrementSplashOpen()
                handleTechOnTimeout()
                interCallback?.onNextAction()
                Log.d(TAG, "Timeout Splash.")
                isTimeout = true
            }
        }

        // --- Network-dependent flow ---
        if (NetworkUtil.isNetworkActive(activity)) {
            Log.d(TAG, "isNetworkActive: true")
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
                val asyncAdmobApi      = async { initAdmobApi(activity) }
                val asyncRemoteConfig  = async { initRemoteConfig(activity) }
                val asyncUMP           = async { initAdsConsentManager(activity) }
                val asyncBilling       = async { initBilling() }
                val asyncTechManager   = async { initTechManager(activity) }

                try {
                    if (!isAsyncSplashAds) {
                        awaitAll(asyncRemoteConfig, /*asyncUMP,*/ asyncBilling, asyncTechManager)
                        if (useTechManagerOrDetectTestAd == TECH_MANAGER && isTech && !isDebug) {
                            turnOffSomeRemoteKeys(activity)
                        }
                        asyncUMP.await()
                    } else {
                        awaitAll(asyncUMP)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    Log.d(TAG, "handleAsync: finally")
                    logEventStep("DoneAsyncInit")

                    // Load banner (non-blocking)
                    lifecycleCoroutineScope.launch {
                        loadBannerSplash(activity, lifecycleOwner, frAdsBannerSplash, listIdBannerSplash, adsKey)
                    }

                    // Wait for ID API then show splash
                    try {
                        logEventStep("StartIDApi")
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

                        // Show splash ad
                        lifecycleCoroutineScope.launch {
                            logEventStep("StartAdSplash")
                            initAndShowSplashAd()
                        }

                        // Await remaining async tasks if in async-splash mode
                        if (isAsyncSplashAds) {
                            awaitAll(asyncRemoteConfig, asyncTechManager)
                            if (useTechManagerOrDetectTestAd == TECH_MANAGER && isTech && !isDebug) {
                                turnOffSomeRemoteKeys(activity)
                            }
                        }

                        // Post-splash setup
                        lifecycleCoroutineScope.launch {
                            setCustomAnimations()
                            onAsyncSplashDone.invoke()
                            loadAdPreloadResume()
                        }
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

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private fun logTimeoutEvent(context: Context) {
        val bundle = Bundle()
        bundle.putString(
            "timeout_splash_next_screen_detail",
            "${initAdmobApi}_${initRemoteConfig}_${initAdsConsentManager}_${initBilling}_${initTechManager}_${initAdmob}"
        )
        bundle.putString("initAdmobApi", initAdmobApi.toString())
        bundle.putString("initRemoteConfig", initRemoteConfig.toString())
        bundle.putString("initAdsConsentManager", initAdsConsentManager.toString())
        bundle.putString("initBilling", initBilling.toString())
        bundle.putString("initTechManager", initTechManager.toString())
        bundle.putString("initAdmob", initAdmob.toString())
        EventTrackingHelper.logEventWithMultipleParams(context, "timeout_splash_next_screen", bundle)
    }

    private fun incrementSplashOpen() {
        SharePreferenceHelper.setInt(
            activity,
            EventTrackingHelper.splash_open,
            SharePreferenceHelper.getInt(activity, EventTrackingHelper.splash_open, 1) + 1
        )
    }

    private fun handleTechOnTimeout() {
        if (useTechManagerOrDetectTestAd == TECH_MANAGER) {
            if (isTech && !isDebug) turnOffSomeRemoteKeys(activity)
        } else if (useTechManagerOrDetectTestAd == DETECT_TEST_AD) {
            if (TechManager.getInstance().isTech(activity) && !isDebug) turnOffSomeRemoteKeys(activity)
        }
    }

    private fun initAndShowSplashAd() {
        var rateAoaInterSplash = com.amazic.library.Utils.RemoteConfigHelper.getInstance()
            .get_config_string(activity, com.amazic.library.Utils.RemoteConfigHelper.rate_aoa_inter_splash)
        if (rateAoaInterSplash.isEmpty()) rateAoaInterSplash = "0_100"

        val isShowOpenSplash = com.amazic.library.Utils.RemoteConfigHelper.getInstance()
            .get_config(activity, keyAdsOpenSplash)
        val isShowInterSplash = com.amazic.library.Utils.RemoteConfigHelper.getInstance()
            .get_config(activity, keyAdsInterSplash)

        Log.d(TAG, "handleAsync: rate - $rateAoaInterSplash - $isShowOpenSplash - $isShowInterSplash")

        adsSplash = AdsSplash.init(isShowOpenSplash, isShowInterSplash, rateAoaInterSplash).apply {
            setKeyAdsInterSplash(keyAdsInterSplash)
            setKeyAdsOpenSplash(keyAdsOpenSplash)
            setLoopAdsSplash(isLoopAdsSplash)
        }
        showAdsSplash(activity, appOpenCallback, interCallback)
    }

    private fun setCustomAnimations() {
        val listAnim = arrayListOf(
            R.raw.ads_1, R.raw.ads_2, R.raw.ads_3, R.raw.ads_4, R.raw.ads_5,
            R.raw.ads_6, R.raw.ads_7, R.raw.ads_8, R.raw.ads_9, R.raw.ads_10,
        )
        Admob.getInstance().setCustomAnimationDialog(listAnim)
        com.amazic.library.ads.app_open_ads.AppOpenManager.getInstance().setCustomAnimationDialog(listAnim)
    }
}
