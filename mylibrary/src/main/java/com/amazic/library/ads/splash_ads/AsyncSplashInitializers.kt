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
// Shared helpers
// ---------------------------------------------------------------------------

/**
 * Gọi Admob.initAdmob nếu consent đã xong nhưng admob chưa được init.
 * Trả về ngay nếu điều kiện không thoả.
 */
private fun AsyncSplash.initAdmobIfNeeded(
    activity: AppCompatActivity?,
    eventSuffix: String,
) {
    if (!initAdsConsentManager || initAdmob) return
    Admob.getInstance().initAdmob(activity) { isSuccessfully ->
        initAdmob = isSuccessfully
        if (eventSuffix.isNotEmpty()) {
            EventTrackingHelper.logEvent(activity, "initAdmob_${eventSuffix}_$isSuccessfully")
        }
        Log.d(TAG, "initAdmob.")
    }
}

/**
 * Đợi Admob init xong rồi gọi initWelcomeBack, sau đó thực thi [onReady].
 * Dùng trong lifecycleScope của activity.
 */
private fun AsyncSplash.waitAdmobThenProceed(
    activity: AppCompatActivity,
    onReady: () -> Unit,
) {
    activity.lifecycleScope.launch {
        while (!Admob.getInstance().isInitAdmobDone) {
            delay(200)
        }
        Log.d(TAG, "admob isInitAdmobDone = true")
        initWelcomeBack(activity)
        onReady()
    }
}

/**
 * Đo thời gian thực thi của [block] và lưu vào [setter].
 */
private inline fun measureElapsed(setter: (Long) -> Unit, block: () -> Unit) {
    val start = System.currentTimeMillis()
    block()
    setter(System.currentTimeMillis() - start)
}

// ---------------------------------------------------------------------------
// Suspend initialiser functions
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
    suspendCoroutine<Unit> { continuation ->
        timeInitAdsConsentManager = 0L
        var isResumed = false
        val startTime = System.currentTimeMillis()

        AdsConsentManager(activity).requestUMP { granted ->
            if (isResumed) return@requestUMP
            isResumed = true

            if (granted) {
                Admob.getInstance().initAdmob(activity) { isSuccessfully ->
                    initAdmob = isSuccessfully
                    Log.d(TAG, "initAdmob.")
                }
                activity?.let {
                    AppOpenManager.getInstance().disableAppResumeWithActivity(it.javaClass)
                }
            }

            timeInitAdsConsentManager = System.currentTimeMillis() - startTime
            initAdsConsentManager = true
            continuation.resume(Unit)
            Log.d(TAG, "initAdsConsentManager.")
        }
    }

internal suspend fun AsyncSplash.initTechManager(activity: AppCompatActivity?) =
    suspendCoroutine<Unit> { continuation ->
        timeInitTechManager = 0L
        val startTime = System.currentTimeMillis()

        fun resume(tag: String) {
            timeInitTechManager = System.currentTimeMillis() - startTime
            continuation.resume(Unit)
            initTechManager = true
            Log.d(TAG, tag)
        }

        if (useTechManagerOrDetectTestAd == AsyncSplash.TECH_MANAGER) {
            TechManager.getInstance().getResult(isDebug, activity, adjustKey) { success ->
                if (success) {
                    isTech = true
                    AppOpenManager.getInstance().isEnableResume = false
                }
                resume("initTechManager.")
            }
        } else {
            resume("initTechManager else.")
        }
    }

internal suspend fun AsyncSplash.initAdmobApi(activity: AppCompatActivity?) =
    suspendCoroutine<Unit> { continuation ->
        if (isUseIdAdsFromRemoteConfig || activity == null) {
            continuation.resume(Unit)
            return@suspendCoroutine
        }

        val startTime = System.currentTimeMillis()

        AdmobApi.getInstance().apply {
            jsonIdAdsDefault = this@initAdmobApi.jsonIdAdsDefault
            timeOutCallApi = this@initAdmobApi.timeOutCallApi
            init(activity, linkServer, object : ApiCallback() {
                override fun onReady() {
                    super.onReady()
                    if (initAdmobType == AsyncSplashConfig.INIT_ADMOB_INT_API) {
                        Admob.getInstance().appID = AdmobApi.getInstance().appId
                        initAdmobIfNeeded(activity, eventSuffix = "Api")
                    }
                    waitAdmobThenProceed(activity) {
                        continuation.resume(Unit)
                        initAdmobApi = true
                        Log.d(TAG, "initAdmobApi.")
                    }
                }
            })
        }
        timeInitAdmobApi = System.currentTimeMillis() - startTime
    }

internal suspend fun AsyncSplash.initRemoteConfig(
    activity: AppCompatActivity?,
) = suspendCoroutine<Unit> { continuation ->
    timeInitRemoteConfig = 0L

    if (isUseAppUpdateManager) {
        continuation.resume(Unit)
        return@suspendCoroutine
    }

    val startTime = System.currentTimeMillis()
    EventTrackingHelper.logEvent(activity, "initRemoteConfig")

    var isResumed = false
    fun resumeOnce(tag: String) {
        if (isResumed) return
        isResumed = true
        timeInitRemoteConfig = System.currentTimeMillis() - startTime
        continuation.resume(Unit)
        initRemoteConfig = true
        Log.d(TAG, tag)
    }

    Handler(Looper.getMainLooper()).postDelayed({
        if (isUseIdAdsFromRemoteConfig && !isSetId) {
            AdmobApi.getInstance().run {
                jsonIdAdsDefault = this@initRemoteConfig.jsonIdAdsDefault
                convertJsonIdAdsDefaultToList(jsonIdAdsDefault)
            }
            isSetId = true
            Log.d(TAG, "Timeout Remote Config: Id ads size = ${AdmobApi.getInstance().listAdsSize}")
            EventTrackingHelper.logEvent(activity, "timeout_call_id_remote_config")
            initWelcomeBack(activity)
            resumeOnce("initRemoteConfig timeout.")
        }
    }, timeOutCallIdRemoteConfig)

    RemoteConfigHelper.getInstance().fetchAllKeysAndTypes(activity) { isSuccess ->
        if (initAdmobType == AsyncSplashConfig.INIT_ADMOB_IN_FIREBASE) {
            Admob.getInstance().appID = RemoteConfigHelper.getInstance()
                .get_config_string(activity, appIdAds)
            Log.d(TAG, "initRemoteConfig appID = ${Admob.getInstance().appID}")
            initAdmobIfNeeded(activity, eventSuffix = "Remote")
        }

        RemoteConfigHelper.getInstance().run {
            Admob.getInstance().apply {
                showAllAds = get_config(activity, RemoteConfigHelper.show_all_ads)
                Log.d(TAG, "show_all_ads = $showAllAds")
                setTimeInterval(
                    get_config_long(activity, RemoteConfigHelper.interval_between_interstitial) * 1000,
                    true
                )
                setTimeIntervalFromStart(
                    get_config_long(activity, RemoteConfigHelper.interval_interstitial_from_start) * 1000
                )
            }
        }

        if (isUseIdAdsFromRemoteConfig && !isSetId) {
            val remoteJson = RemoteConfigHelper.getInstance()
                .get_config_string(activity, RemoteConfigHelper.id_ads)
            val (json, logTag, event) = if (remoteJson.contains("app_id") && isSuccess) {
                Triple(remoteJson, "Set id ads from remote", "set_id_remote_config")
            } else {
                Triple(jsonIdAdsDefault, "Set id ads default case fail remote", "set_id_default_case_fail_remote")
            }
            AdmobApi.getInstance().run {
                jsonIdAdsDefault = json
                convertJsonIdAdsDefaultToList(json)
            }
            isSetId = true
            Log.d(TAG, "$logTag: Id ads size = ${AdmobApi.getInstance().listAdsSize}")
            EventTrackingHelper.logEvent(activity, event)
        }

        activity?.let {
            waitAdmobThenProceed(it) { resumeOnce("initAdmobApi (via RemoteConfig).") }
        } ?: resumeOnce("initRemoteConfig (no activity).")
    }
}