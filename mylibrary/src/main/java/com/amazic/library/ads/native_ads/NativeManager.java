package com.amazic.library.ads.native_ads;

import android.app.Activity;
import android.os.CountDownTimer;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.amazic.library.ads.admob.Admob;
import com.google.android.gms.ads.nativead.NativeAd;

public class NativeManager implements LifecycleEventObserver {
    private static final String TAG = "NativeManager";
    private final NativeBuilder builder;
    private final Activity currentActivity;
    private final LifecycleOwner lifecycleOwner;
    private boolean isReloadAds = false;
    private boolean isAlwaysReloadOnResume = false;
    private long intervalReloadNative = 0;
    private boolean isStop = false;
    private CountDownTimer countDownTimer;
    private final String adsKey;
    private NativeAd myNativeAd;

    public void setIntervalReloadNative(long intervalReloadNative) {
        if (intervalReloadNative > 0) {
            this.intervalReloadNative = intervalReloadNative;
            countDownTimer = new CountDownTimer(this.intervalReloadNative, 1000) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    loadNativeFloor(1);
                }
            };
        }
    }

    public NativeManager(@NonNull Activity currentActivity, LifecycleOwner lifecycleOwner, NativeBuilder builder, String adsKey) {
        this.builder = builder;
        this.currentActivity = currentActivity;
        this.adsKey = adsKey;
        this.lifecycleOwner = lifecycleOwner;
        this.lifecycleOwner.getLifecycle().addObserver(this);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                Log.d(TAG, "onStateChanged: ON_CREATE");
                loadNativeFloor(builder.maxRequest);
                break;
            case ON_RESUME:
                if (countDownTimer != null && isStop) {
                    countDownTimer.start();
                }
                String valueLog = isStop + " && " + (isReloadAds || isAlwaysReloadOnResume);
                Log.d(TAG, "onStateChanged: resume\n" + valueLog);
                if (isStop && (isReloadAds || isAlwaysReloadOnResume)) {
                    isReloadAds = false;
                    loadNativeFloor(1);
                }
                isStop = false;
                break;
            case ON_PAUSE:
                Log.d(TAG, "onStateChanged: ON_PAUSE");
                isStop = true;
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
                break;
            case ON_DESTROY:
                if (myNativeAd != null) {
                    myNativeAd.destroy();
                }
                Log.d(TAG, "onStateChanged: ON_DESTROY");
                this.lifecycleOwner.getLifecycle().removeObserver(this);
                break;
        }
    }

    private void loadNativeFloor(int maxRequest) {
        if (myNativeAd != null) {
            myNativeAd.destroy();
        }
        myNativeAd = Admob.getInstance().loadMultipleNativeAds(currentActivity,
                builder.getListIdAd(),
                builder.getFlAd(),
                builder.getLayoutNativeAdmob(),
                builder.getLayoutNativeMeta(),
                builder.getLayoutShimmerNative(),
                true,
                builder.getCallback(),
                () -> {
                    if (countDownTimer != null && this.lifecycleOwner.getLifecycle().getCurrentState() == Lifecycle.State.RESUMED) {
                        countDownTimer.cancel();
                        countDownTimer.start();
                    }
                }, adsKey, maxRequest);
    }

    public void setReloadAds() {
        isReloadAds = true;
    }

    public void reloadAdNow() {
        loadNativeFloor(1);
    }

    public void setAlwaysReloadOnResume(boolean isAlwaysReloadOnResume) {
        this.isAlwaysReloadOnResume = isAlwaysReloadOnResume;
    }
}
