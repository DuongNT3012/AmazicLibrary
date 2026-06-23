package com.amazic.library.ads.splash_ads

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.amazic.library.Utils.EventTrackingHelper
import com.amazic.library.Utils.RemoteConfigHelper
import com.amazic.library.ads.admob.Admob
import com.amazic.library.ads.admob.AdmobApi
import com.amazic.library.ads.app_open_ads.AppOpenManager
import com.amazic.library.ads.callback.ApiCallback
import com.amazic.library.organic.TechManager
import com.amazic.library.ump.AdsConsentManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "AsyncSplash"

// ---------------------------------------------------------------------------
// Suspend initialiser functions — extracted from AsyncSplash
// ---------------------------------------------------------------------------

internal suspend fun AsyncSplash.initBilling() = suspendCoroutine<Unit> { continuation ->
    /*if (isUseBilling) {
        IAPManager.getInstance().initBilling(activity, listProductDetailCustoms, object : BillingCallback() {
            private var isResumed = false
            override fun onBillingSetupFinished(resultCode: Int) {
                super.onBillingSetupFinished(resultCode)
                if (!isResumed) { isResumed = true; continuation.resume(Unit); initBilling = true }
            }
            override fun onBillingServiceDisconnected() {
                super.onBillingServiceDisconnected()
                if (!isResumed) { isResumed = true; continuation.resume(Unit); initBilling = true }
            }
        })
    } else {*/ //comment for billing
    continuation.resume(Unit)
    initBilling = true
    Log.d(TAG, "Not use billing.")
    //} //comment for billing
}

internal suspend fun AsyncSplash.initAdsConsentManager(activity: AppCompatActivity?) =
    suspendCoroutine { continuation ->
        timeInitAdsConsentManager = 0L
        val startTime = System.currentTimeMillis()
        val adsConsentManager = AdsConsentManager(activity)
        var isResumed = false
        adsConsentManager.requestUMP {
            if (!isResumed) {
                isResumed = true
                if (it) {
                    Admob.getInstance().initAdmob(activity) { isSuccessfully ->
                        initAdmob = isSuccessfully
                        Log.d(TAG, "initAdmob.")
                    }
                    activity?.let { act ->
                        AppOpenManager.getInstance().disableAppResumeWithActivity(act.javaClass)
                    }
                }
                timeInitAdsConsentManager = System.currentTimeMillis() - startTime
                initAdsConsentManager = true
                continuation.resume(Unit)
                Log.d(TAG, "initAdsConsentManager.")
            }
        }
    }

internal suspend fun AsyncSplash.initTechManager(activity: AppCompatActivity?) =
    suspendCoroutine<Unit> { continuation ->
        var isResumed = false
        timeInitTechManager = 0L
        val startTime = System.currentTimeMillis()
        if (useTechManagerOrDetectTestAd == AsyncSplash.TECH_MANAGER) {
            TechManager.getInstance().getResult(isDebug, activity, adjustKey) {
                if (it) {
                    isTech = true
                    AppOpenManager.getInstance().isEnableResume = false
                }
                timeInitTechManager = System.currentTimeMillis() - startTime
                if (!isResumed) {
                    isResumed = true
                    continuation.resume(Unit)
                    initTechManager = true
                    Log.d(TAG, "initTechManager.")
                }
            }
        } else {
            timeInitTechManager = System.currentTimeMillis() - startTime
            continuation.resume(Unit)
            initTechManager = true
            Log.d(TAG, "initTechManager else.")
        }
    }

internal suspend fun AsyncSplash.initAdmobApi(activity: AppCompatActivity?) =
    suspendCoroutine<Unit> { continuation ->
        if (!isUseIdAdsFromRemoteConfig && activity != null) {
            val startTime = System.currentTimeMillis()
            AdmobApi.getInstance().jsonIdAdsDefault = jsonIdAdsDefault
            AdmobApi.getInstance().timeOutCallApi = timeOutCallApi
            AdmobApi.getInstance().init(activity, linkServer, object : ApiCallback() {
                private var isResumed = false
                override fun onReady() {
                    super.onReady()
                    if (initAdmobType == AsyncSplashConfig.INIT_ADMOB_INT_API) {
                        Admob.getInstance().appID = AdmobApi.getInstance().appId
                        if (initAdsConsentManager && !initAdmob) {
                            Admob.getInstance().initAdmob(activity) { isSuccessfully ->
                                EventTrackingHelper.logEvent(activity, "initAdmob_Api_$isSuccessfully")
                                initAdmob = isSuccessfully
                                Log.d(TAG, "initAdmob.")
                            }
                        }
                    }
                    activity.lifecycleScope.launch {
                        while (!Admob.getInstance().isInitAdmobDone) {
                            delay(200)
                        }
                        Log.d(TAG, "initAdmobApi onReady: ${Admob.getInstance().isInitAdmobDone}")
                        initWelcomeBack(activity)
                        if (!isResumed) {
                            isResumed = true
                            continuation.resume(Unit)
                            initAdmobApi = true
                            Log.d(TAG, "initAdmobApi.")
                        }
                    }
                }
            })
            timeInitAdmobApi = System.currentTimeMillis() - startTime
        } else {
            continuation.resume(Unit)
        }
    }

internal suspend fun AsyncSplash.initRemoteConfig(
    activity: AppCompatActivity?
) = suspendCoroutine<Unit> { continuation ->
    timeInitRemoteConfig = 0L
    if (isUseAppUpdateManager) {
        continuation.resume(Unit)
        return@suspendCoroutine
    }

    val startTime = System.currentTimeMillis()
    EventTrackingHelper.logEvent(activity, "initRemoteConfig")

    // Fallback timeout handler
    Handler(Looper.getMainLooper()).postDelayed({
        if (isUseIdAdsFromRemoteConfig && !isSetId) {
            AdmobApi.getInstance().jsonIdAdsDefault = jsonIdAdsDefault
            AdmobApi.getInstance().convertJsonIdAdsDefaultToList(jsonIdAdsDefault)
            isSetId = true
            Log.d(TAG, "Timeout Remote Config: Id ads size = ${AdmobApi.getInstance().listAdsSize}")
            EventTrackingHelper.logEvent(activity, "timeout_call_id_remote_config")
            initWelcomeBack(activity)
            timeInitRemoteConfig = System.currentTimeMillis() - startTime
        }
    }, timeOutCallIdRemoteConfig)

    var isResumed = false
    RemoteConfigHelper.getInstance().fetchAllKeysAndTypes(activity) { isSuccess ->
        if (initAdmobType == AsyncSplashConfig.INIT_ADMOB_IN_FIREBASE) {
            Admob.getInstance().appID = RemoteConfigHelper.getInstance()
                .get_config_string(activity, RemoteConfigHelper.id_ads)
            if (initAdsConsentManager && !initAdmob) {
                Admob.getInstance().initAdmob(activity) { isSuccessfully ->
                    initAdmob = isSuccessfully
                    EventTrackingHelper.logEvent(activity, "initAdmob_Remote_$isSuccessfully")
                    Log.d(TAG, "initAdmob.")
                }
            }
        }
        activity?.lifecycleScope?.launch {
            while (!Admob.getInstance().isInitAdmobDone) {
                delay(200)
            }
            Log.d(TAG, "initAdmobApi onReady: ${Admob.getInstance().isInitAdmobDone}")
            initWelcomeBack(activity)
            if (!isResumed) {
                isResumed = true
                continuation.resume(Unit)
                initAdmobApi = true
                Log.d(TAG, "initAdmobApi.")
            }
        }
        if (isUseIdAdsFromRemoteConfig && !isSetId) {
            val jsonFromRemote = RemoteConfigHelper.getInstance()
                .get_config_string(activity, RemoteConfigHelper.id_ads)
            if (jsonFromRemote.contains("app_id") && isSuccess) {
                AdmobApi.getInstance().jsonIdAdsDefault = jsonFromRemote
                AdmobApi.getInstance().convertJsonIdAdsDefaultToList(jsonFromRemote)
                isSetId = true
                Log.d(TAG, "Set id ads from remote: Id ads size = ${AdmobApi.getInstance().listAdsSize}")
                EventTrackingHelper.logEvent(activity, "set_id_remote_config")
            } else {
                AdmobApi.getInstance().jsonIdAdsDefault = jsonIdAdsDefault
                AdmobApi.getInstance().convertJsonIdAdsDefaultToList(jsonIdAdsDefault)
                isSetId = true
                Log.d(TAG, "Set id ads default case fail remote: Id ads size = ${AdmobApi.getInstance().listAdsSize}")
                EventTrackingHelper.logEvent(activity, "set_id_default_case_fail_remote")
            }

        }

        Log.d(TAG, "show_all_ads = ${RemoteConfigHelper.getInstance().get_config(activity, RemoteConfigHelper.show_all_ads)}")
        Admob.getInstance().showAllAds = RemoteConfigHelper.getInstance()
            .get_config(activity, RemoteConfigHelper.show_all_ads)
        Admob.getInstance().setTimeInterval(
            RemoteConfigHelper.getInstance().get_config_long(
                activity, RemoteConfigHelper.interval_between_interstitial
            ) * 1000, true
        )
        Admob.getInstance().setTimeIntervalFromStart(
            RemoteConfigHelper.getInstance().get_config_long(
                activity, RemoteConfigHelper.interval_interstitial_from_start
            ) * 1000
        )
        timeInitRemoteConfig = System.currentTimeMillis() - startTime
        if (!isResumed) {
            isResumed = true
            continuation.resume(Unit)
            initRemoteConfig = true
            Log.d(TAG, "initRemoteConfig.")
        }
    }
}
