package com.amazic.library.view

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
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

    companion object {
        var interCallback: InterCallback? = null
        var adsKey: String = "native_after_inter"
        var remoteKey: String = "native_after_inter"
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {

        window.hideNavigation()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_native_after_inter)

        frAds = findViewById(R.id.fr_ads)
        Log.d("Admob", "SPLASH: showInterAdsSplash: Show Screen Native After Inter");
        NativeAfterInterManager.showPreloadNativeAfterInter(
            frAds,
            this,
            adsKey,
            remoteKey,
            object : NativeAfterInterManager.OnCloseNativeListener {
                override fun onClose() {
                    interCallback?.onNextAction()
                    finish()
                }

                override fun onFail() {
                    finish()
                }
            }
        )

    }

    override fun onDestroy() {
        super.onDestroy()
        adsKey = ""
        remoteKey = ""
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