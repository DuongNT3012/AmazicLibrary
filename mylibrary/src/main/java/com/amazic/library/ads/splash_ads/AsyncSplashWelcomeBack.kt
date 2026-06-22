package com.amazic.library.ads.splash_ads

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.amazic.library.Utils.RemoteConfigHelper
import com.amazic.library.ads.admob.AdmobApi
import com.amazic.library.ads.app_open_ads.AppOpenManager

private const val TAG = "Admob"

// ---------------------------------------------------------------------------
// Welcome-back / resume ads setup — extracted from AsyncSplash
// ---------------------------------------------------------------------------

/**
 * Initialises app-open (resume) ads according to the chosen welcome-back mode:
 *   "Normal"  — standard resume init
 *   "Below"   — welcome-back screen sits *below* resume ad
 *   "Above"   — welcome-back screen sits *above* resume ad
 *   else      — same as Normal but uses the default resume list
 */
internal fun AsyncSplash.initWelcomeBack(activity: AppCompatActivity?) {
    when (initWelcomeBack) {
        "Normal" -> handleNormalWelcomeBack(activity)
        "Below"  -> handleBelowWelcomeBack(activity)
        "Above"  -> handleAboveWelcomeBack(activity)
        else     -> handleElseWelcomeBack(activity)
    }
}

private fun AsyncSplash.handleNormalWelcomeBack(activity: AppCompatActivity?) {
    val listIdResume = resolveResumeIds(useDefault = true)
    if (listIdResume.isEmpty()) return

    if (!isUseAdPreloading) {
        Log.d(TAG, "APP Open Preload: normal - isUseAdPreloading=$isUseAdPreloading, isPreloadResumeAds=$isPreloadResumeAds")
        if (isPreloadResumeAds) {
            AppOpenManager.getInstance().loadAdNotCheckRemote(activity, listIdResume, keyAdsOpenResume)
        }
    }
    AppOpenManager.getInstance().init(activity, listIdResume)
    activity?.let { AppOpenManager.getInstance().disableAppResumeWithActivity(it.javaClass) }
}

private fun AsyncSplash.handleBelowWelcomeBack(activity: AppCompatActivity?) {
    val listIdResume = resolveResumeIds(useDefault = false, fallbackKey = RemoteConfigHelper.resume_wb, fallbackKeyName = "resume_wb")
    if (listIdResume.isEmpty()) return

    if (!isUseAdPreloading) {
        Log.d(TAG, "APP Open Preload: below - isUseAdPreloading=$isUseAdPreloading, isPreloadResumeAds=$isPreloadResumeAds")
        if (isPreloadResumeAds) {
            AppOpenManager.getInstance().loadAdNotCheckRemote(activity, listIdResume, keyAdsOpenResume)
        }
    }
    welcomeBackClass?.let {
        AppOpenManager.getInstance().initWelcomeBackBelowAdsResume(activity, listIdResume, it)
        AppOpenManager.getInstance().disableAppResumeWithActivity(it) // disable resume welcome back
    }
    activity?.let { AppOpenManager.getInstance().disableAppResumeWithActivity(it.javaClass) }
}

private fun AsyncSplash.handleAboveWelcomeBack(activity: AppCompatActivity?) {
    val listIdResume = resolveResumeIds(useDefault = false, fallbackKey = RemoteConfigHelper.resume_wb, fallbackKeyName = "resume_wb")
    if (listIdResume.isEmpty()) return

    if (!isUseAdPreloading) {
        Log.d(TAG, "APP Open Preload: above - isUseAdPreloading=$isUseAdPreloading, isPreloadResumeAds=$isPreloadResumeAds")
        if (isPreloadResumeAds) {
            AppOpenManager.getInstance().loadAdNotCheckRemote(activity, listIdResume, keyAdsOpenResume)
        }
    }
    welcomeBackClass?.let {
        AppOpenManager.getInstance().initWelcomeBackAboveAdsResume(activity, listIdResume, it)
        AppOpenManager.getInstance().disableAppResumeWithActivity(it) // disable resume welcome back
    }
    activity?.let { AppOpenManager.getInstance().disableAppResumeWithActivity(it.javaClass) }
}

private fun AsyncSplash.handleElseWelcomeBack(activity: AppCompatActivity?) {
    val listIdResume = resolveResumeIds(useDefault = true)
    if (listIdResume.isEmpty()) return

    if (!isUseAdPreloading) {
        Log.d(TAG, "APP Open Preload: else - isUseAdPreloading=$isUseAdPreloading, isPreloadResumeAds=$isPreloadResumeAds")
        if (isPreloadResumeAds) {
            AppOpenManager.getInstance().loadAdNotCheckRemote(activity, listIdResume, keyAdsOpenResume)
        }
    }
    AppOpenManager.getInstance().init(activity, listIdResume)
    activity?.let { AppOpenManager.getInstance().disableAppResumeWithActivity(it.javaClass) }
}

/**
 * Resolves the resume-ad ID list.
 * If [keyAdsOpenResume] is set it takes priority; otherwise falls back to
 * the given [fallbackKey] or the global default list.
 */
private fun AsyncSplash.resolveResumeIds(
    useDefault: Boolean,
    fallbackKey: String? = null,
    fallbackKeyName: String? = null
): MutableList<String> {
    val list = mutableListOf<String>()
    if (keyAdsOpenResume.isNotEmpty()) {
        list.addAll(AdmobApi.getInstance().getListIDByName(keyAdsOpenResume))
    } else if (!useDefault && fallbackKey != null) {
        list.addAll(AdmobApi.getInstance().getListIDByName(fallbackKey))
        keyAdsOpenResume = fallbackKeyName ?: fallbackKey
    } else {
        list.addAll(AdmobApi.getInstance().listIDAppOpenResume)
        keyAdsOpenResume = "open_resume"
    }
    return list
}

/** Preloads resume ad when [isUseAdPreloading] is enabled. */
internal fun AsyncSplash.loadAdPreloadResume() {
    if (!isUseAdPreloading) return

    val listIdResume = mutableListOf<String>()
    if (keyAdsOpenResume.isNotEmpty()) {
        listIdResume.addAll(AdmobApi.getInstance().getListIDByName(keyAdsOpenResume))
    } else {
        listIdResume.addAll(AdmobApi.getInstance().listIDAppOpenResume)
        keyAdsOpenResume = "open_resume"
    }
    if (listIdResume.isNotEmpty()) {
        Log.d(TAG, "APP Open Preload: start loadAdPreloadNotCheckRemote")
        AppOpenManager.getInstance().loadAdPreloadNotCheckRemote(activity, listIdResume, keyAdsOpenResume)
    }
}
