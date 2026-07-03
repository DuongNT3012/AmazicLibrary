package com.amazic.library.view

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.amazic.library.ads.callback.InterCallback
import com.amazic.library.ads.native_ads.NativeAfterInterManager
import com.amazic.mylibrary.R

class NativeAfterInterActivity : AppCompatActivity() {
    private lateinit var frAds: FrameLayout

    // Splash mode
    private lateinit var ivClose: ImageButton
    private lateinit var tvClose: TextView
    private var currentAdIndex = 0
    private var countdownTimer: CountDownTimer? = null
    private val CLOSE_DELAY_MS = 5000L

    companion object {
        var interCallback: InterCallback? = null
        var adsKey: String = "native_after_inter"
        var remoteKey: String = "native_after_inter"
        var isSplashMode: Boolean = false

//        fun setAdsKey(key: String) { adsKey = key }
//        fun setRemoteKey(key: String) { remoteKey = key }
//        fun setInterCallback(callback: InterCallback?) { interCallback = callback }
//        fun setSplashMode(isSplash: Boolean) { isSplashMode = isSplash }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {

        window.hideNavigation()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_native_after_inter)

        frAds = findViewById(R.id.fr_ads)
        ivClose = findViewById(R.id.iv_close)
        tvClose = findViewById(R.id.tv_close)
        ivClose.visibility = View.GONE
        tvClose.visibility = View.GONE

        Log.d("Admob", "NativeAfterInter: isSplashMode = $isSplashMode")

        if (isSplashMode) {
            initSplashMode()
        } else {
            initNormalMode()
        }

    }

    private fun initNormalMode() {
        ivClose.visibility = View.GONE
        tvClose.visibility = View.GONE
        NativeAfterInterManager.showPreloadNativeAfterInter(
            frAds,
            this,
            adsKey,
            remoteKey,
            object : NativeAfterInterManager.OnCloseNativeListener {
                override fun onClose() {
                    Log.d("Admob", "NativeAfterInter Normal: onClose")
                    interCallback?.onNextAction()
                    finish()
                }

                override fun onFail() {
                    Log.d("Admob", "NativeAfterInter Normal: onFail")
                    interCallback?.onNextAction()
                    finish()
                }
            }
        )
    }

    ///splash
    private fun initSplashMode() {
        val adList = NativeAfterInterManager.mapNativeAdsAfterInterSplash[adsKey]
        Log.d("Admob", "initSplashMode: adsKey='$adsKey'" +
                ", mapKeys=${NativeAfterInterManager.mapNativeAdsAfterInterSplash.keys}" +
                ", mapListSize=${NativeAfterInterManager.mapNativeAdsAfterInterSplash[adsKey]?.size}")
        if (adList.isNullOrEmpty()) {
            Log.d("Admob", "NativeAfterInter Splash: no ads → onNextAction")
            interCallback?.onNextAction()
            finish()
            return
        }

        ivClose.setOnClickListener {
            onCloseCurrentAd()
        }
        tvClose.setOnClickListener {
            onCloseCurrentAd()
        }

        showAdAt(0)
    }

    private fun showAdAt(index: Int) {
        val adList = NativeAfterInterManager.mapNativeAdsAfterInterSplash[adsKey]
        val nativeAd = adList?.getOrNull(index)

        if (nativeAd == null) {
            Log.d("Admob", "NativeAfterInter Splash: no ad at index $index → onNextAction")
            interCallback?.onNextAction()
            finish()
            return
        }

        Log.d(
            "Admob",
            "NativeAfterInter Splash: show ad ${index + 1}/${adList?.size} | responseId=${nativeAd.responseInfo?.responseId}"
        )
        currentAdIndex = index
        ivClose.visibility = View.GONE
        tvClose.visibility = View.GONE

        val slideDistance = resources.displayMetrics.widthPixels.toFloat()

        if (frAds.childCount == 0) {
            // Ad đầu tiên — kéo vào từ phải
            NativeAfterInterManager.showNativeAdInFrameSplash(frAds, nativeAd)
            frAds.translationX = slideDistance
            frAds.animate()
                .translationX(0f)
                .setDuration(350)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { startCloseButtonCountdown() }
                .start()
        } else {
            // Đẩy ad hiện tại ra trái → kéo ad mới vào từ phải
            frAds.animate()
                .translationX(-slideDistance)
                .setDuration(300)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction {
                    NativeAfterInterManager.showNativeAdInFrameSplash(frAds, nativeAd)
                    frAds.translationX = slideDistance
                    frAds.animate()
                        .translationX(0f)
                        .setDuration(300)
                        .setInterpolator(DecelerateInterpolator())
                        .withEndAction { startCloseButtonCountdown() }
                        .start()
                }
                .start()
        }
    }

    private fun startCloseButtonCountdown() {
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(CLOSE_DELAY_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(
                    "Admob",
                    "NativeAfterInter Splash: close button in ${millisUntilFinished / 1000}s"
                )
            }

            override fun onFinish() {
                Log.d(
                    "Admob",
                    "NativeAfterInter Splash: show close button at ad ${currentAdIndex + 1}"
                )

                if (currentAdIndex % 2 == 0) {
                    ivClose.visibility = View.VISIBLE
                    tvClose.visibility = View.GONE
                } else {
                    ivClose.visibility = View.GONE
                    tvClose.visibility = View.VISIBLE
                }
            }
        }.start()
    }

    private fun onCloseCurrentAd() {
        countdownTimer?.cancel()
        ivClose.visibility = View.GONE
        tvClose.visibility = View.GONE

        val nextIndex = currentAdIndex + 1
        val adList = NativeAfterInterManager.mapNativeAdsAfterInterSplash[adsKey]

        if (nextIndex < (adList?.size ?: 0)) {
            Log.d("Admob", "NativeAfterInter Splash: move to ad ${nextIndex + 1}")
            showAdAt(nextIndex)
        } else {
            Log.d("Admob", "NativeAfterInter Splash: all ads shown → onNextAction")
            interCallback?.onNextAction()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
        if (isSplashMode) {
            NativeAfterInterManager.mapNativeAdsAfterInterSplash[adsKey]?.forEach { it.destroy() }
            NativeAfterInterManager.mapNativeAdsAfterInterSplash.remove(adsKey)
        }
        adsKey = ""
        remoteKey = ""
        isSplashMode = false
    }


    fun Window.hideNavigation() {
        if (setFullScreenWallpaper()) return

        decorView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            val activityRoot = decorView
            activityRoot.getWindowVisibleDisplayFrame(rect)
            if (setFullScreenWallpaper()) return@addOnGlobalLayoutListener
        }
    }

    private fun Window.setFullScreenWallpaper(): Boolean {
        val windowInsetsController: WindowInsetsControllerCompat? =
            if (Build.VERSION.SDK_INT >= 30) {
                ViewCompat.getWindowInsetsController(decorView)
            } else {
                WindowInsetsControllerCompat(this, decorView)
            }

        if (windowInsetsController == null) {
            return true
        }
        setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        )
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        windowInsetsController.hide(WindowInsetsCompat.Type.systemGestures())
        return false
    }
}