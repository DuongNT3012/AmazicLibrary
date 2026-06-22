package com.amazic.library.ads.splash_ads

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.amazic.library.Utils.EventTrackingHelper
import com.amazic.library.Utils.RemoteConfigHelper
import com.amazic.library.ads.banner_ads.BannerBuilder
import com.amazic.library.ads.banner_ads.BannerManager
import com.amazic.library.ads.callback.AppOpenCallback
import com.amazic.library.ads.callback.BannerCallback
import com.amazic.library.ads.callback.InterCallback
import com.amazic.library.organic.TechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.system.measureTimeMillis

private const val TAG = "Admob"

// ---------------------------------------------------------------------------
// Ads-display helpers — extracted from AsyncSplash
// ---------------------------------------------------------------------------

internal fun AsyncSplash.loadBannerSplash(
    activity: AppCompatActivity?,
    lifecycleOwner: LifecycleOwner,
    frAdsBanner: FrameLayout?,
    listIdBannerSplash: MutableList<String>,
    adsKey: String
) {
    if (!isShowBannerSplash) {
        frAdsBanner?.visibility = View.GONE
        return
    }

    Log.d(TAG, "loadBannerSplash.")
    // Reset TechManager detection flag before loading banner
    if (useTechManagerOrDetectTestAd == AsyncSplash.DETECT_TEST_AD) {
        TechManager.getInstance().detectedTech(activity, false)
    }
    frAdsBanner?.visibility = View.VISIBLE

    val bannerBuilder = BannerBuilder(frAdsBanner).apply {
        setListIdAdMain(listIdBannerSplash)
        callBack = object : BannerCallback() {
            override fun onAdImpression() {
                super.onAdImpression()
                if (useTechManagerOrDetectTestAd == AsyncSplash.DETECT_TEST_AD
                    && TechManager.getInstance().isTech(activity)
                    && !isDebug
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
    }
    activity?.let { BannerManager(it, lifecycleOwner, bannerBuilder, adsKey) }
}

internal fun AsyncSplash.showAdsSplash(
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
        adsSplash?.showAdsSplashApi(
            activity,
            appOpenCallback,
            interCallback,
            keyNativeAfterInter,
            keyNativeAfterInter
        )
        Log.d(TAG, "showAdsSplash.")
        isShowAdsSplash = true
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
            val buffer = ByteArray(1024 * 8) // 8 KB buffer
            var totalBytesRead = 0L

            val timeTakenMillis = measureTimeMillis {
                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    totalBytesRead += bytesRead
                    if (totalBytesRead >= 1 * 1024 * 1024) break // Limit to 1 MB
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
