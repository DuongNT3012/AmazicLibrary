package com.amazic.library.ads.native_ads;

import android.app.Activity;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.callback.NativeCallback;
import com.google.android.gms.ads.LoadAdError;
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
    private boolean isTimerRunning = false;
    private CountDownTimer countDownTimer;
    private final String remoteKey;
    private String remoteKeySecondary = "native_all_2";
    private NativeAd myNativeAdMain;
    private NativeAd myNativeAdSecondary;

    public void setIntervalReloadNative(long intervalReloadNative) {
        if (intervalReloadNative > 0) {
            this.intervalReloadNative = intervalReloadNative;
            countDownTimer = new CountDownTimer(this.intervalReloadNative, 1000) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    isTimerRunning = false;
                    loadNativeFloor(builder.maxRequestReload);
                }
            };
        }
    }

    public void cancelAutoReloadNative() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    public NativeManager(@NonNull Activity currentActivity, LifecycleOwner lifecycleOwner, NativeBuilder builder, String remoteKey) {
        this.builder = builder;
        this.currentActivity = currentActivity;
        this.remoteKey = remoteKey;
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
                    isTimerRunning = true;
                    countDownTimer.start();
                }
                String valueLog = isStop + " && " + (isReloadAds || isAlwaysReloadOnResume);
                Log.d(TAG, "onStateChanged: resume\n" + valueLog);
                if (isStop && (isReloadAds || isAlwaysReloadOnResume)) {
                    isReloadAds = false;
                    loadNativeFloor(builder.maxRequestReload);
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
                if (myNativeAdMain != null) {
                    myNativeAdMain.destroy();
                }
                if (myNativeAdSecondary != null) {
                    myNativeAdSecondary.destroy();
                }
                Log.d(TAG, "onStateChanged: ON_DESTROY");
                this.lifecycleOwner.getLifecycle().removeObserver(this);
                break;
        }
    }

    private void loadNativeFloor(int maxRequest) {
        Log.d(TAG, "loadNativeFloor: " + builder.useNewAdLoading);
        if (builder.useNewAdLoading) {
            loadNewAdFormat();
        } else loadOldAdFormat(maxRequest);
    }

    private void loadNewAdFormat() {
        if (!Admob.getInstance().checkCondition(currentActivity, remoteKey) && !Admob.getInstance().checkCondition(currentActivity, remoteKeySecondary)) {
            builder.shimmerFrameLayout.setVisibility(View.GONE);
            if (builder.nativeAdViewMain != null)
                builder.nativeAdViewMain.setVisibility(View.GONE);
            if (builder.nativeAdViewSecondary != null)
                builder.nativeAdViewSecondary.setVisibility(View.GONE);
            return;
        }
        if (!Admob.getInstance().checkCondition(currentActivity, remoteKey)) {
            if (builder.nativeAdViewMain != null)
                builder.nativeAdViewMain.setVisibility(View.GONE);
            return;
        }
        if (!Admob.getInstance().checkCondition(currentActivity, remoteKeySecondary)) {
            if (builder.nativeAdViewSecondary != null)
                builder.nativeAdViewSecondary.setVisibility(View.GONE);
            return;
        }
        loadMainNative();
        loadSecondaryNative();
    }

    private void loadMainNative() {
        Log.d(TAG, "loadMainNative:");
        if (myNativeAdMain != null) myNativeAdMain.destroy();
        Admob.getInstance().loadNativeAds(currentActivity,
                builder.getListIdAdMain(),
                new NativeCallback() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        super.onNativeAdLoaded(nativeAd);
                        Log.d(TAG, "onNativeAdLoaded: Main");
                        builder.getCallback().onNativeAdLoaded(nativeAd);
                        showNativeMain(nativeAd);
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        Log.d(TAG, "onAdImpression: Main");
                        builder.getCallback().onAdImpression();
                        startReloadNative();
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        builder.getCallback().onAdClicked();
                        Log.d(TAG, "onAdClicked: Main");
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        Log.e(TAG, "onAdFailedToLoad: Main\n" + loadAdError.getMessage());
                        builder.getCallback().onAdFailedToLoad(loadAdError);
                        if (myNativeAdSecondary != null) {
                            builder.shimmerFrameLayout.setVisibility(View.GONE);
                        }
                        loadNativeBackup(true);
                    }
                }, remoteKey);
    }


    private void loadSecondaryNative() {
        Log.d(TAG, "loadSecondaryNative:");
        if (myNativeAdSecondary != null) myNativeAdSecondary.destroy();
        Admob.getInstance().loadNativeAds(currentActivity,
                builder.getListIdAdSecondary(),
                new NativeCallback() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        super.onNativeAdLoaded(nativeAd);
                        builder.getCallback().onNativeAdLoaded(nativeAd);
                        Log.d(TAG, "onNativeAdLoaded: Secondary");
                        showNativeSecondary(nativeAd);
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        Log.d(TAG, "onAdImpression: Secondary");
                        builder.getCallback().onAdImpression();
                        handleImpressionNativeSecondary();
                        startReloadNative();
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        Log.d(TAG, "onAdFailedToLoad: Secondary\n" + loadAdError.getMessage());
                        builder.getCallback().onAdFailedToLoad(loadAdError);
                        if (myNativeAdMain != null) {
                            builder.shimmerFrameLayout.setVisibility(View.GONE);
                        }
                        loadNativeBackup(false);
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        Log.d(TAG, "onAdClicked: Secondary");
                        builder.getCallback().onAdClicked();
                    }

                }, remoteKeySecondary);
    }

    private void showNativeMain(NativeAd nativeAd) {
        myNativeAdMain = nativeAd;
        Admob.getInstance().populateNativeAdView(nativeAd, builder.nativeAdViewMain);
        builder.nativeAdViewMain.setVisibility(View.VISIBLE);
        if (builder.nativeAdViewSecondary != null)
            builder.nativeAdViewSecondary.setVisibility(View.GONE);
        builder.shimmerFrameLayout.setVisibility(View.GONE);
    }

    private void showNativeSecondary(NativeAd nativeAd) {
        if (builder.nativeAdViewSecondary != null) {
            myNativeAdSecondary = nativeAd;
            Admob.getInstance().populateNativeAdView(nativeAd, builder.nativeAdViewSecondary);
            builder.nativeAdViewSecondary.setVisibility(View.VISIBLE);
            builder.nativeAdViewMain.setVisibility(View.GONE);
            builder.shimmerFrameLayout.setVisibility(View.GONE);
        }
    }


    private void handleImpressionNativeSecondary() {
        if (myNativeAdMain != null) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (builder.nativeAdViewSecondary != null)
                    builder.nativeAdViewSecondary.setVisibility(View.GONE);
                builder.nativeAdViewMain.setVisibility(View.VISIBLE);
                builder.shimmerFrameLayout.setVisibility(View.GONE);
            }, 800);
        }
    }

    private void loadNativeBackup(boolean isMainNative) {
        Admob.getInstance().loadNativeAdsBackup(currentActivity,
                builder.getListIdAdBackup(),
                new NativeCallback() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        super.onNativeAdLoaded(nativeAd);
                        Log.d(TAG, "onNativeAdLoaded: Backup " + isMainNative);
                        if (isMainNative) {
                            showNativeMain(nativeAd);
                        } else {
                            showNativeSecondary(nativeAd);
                        }
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        Log.d(TAG, "onAdImpression: Backup " + isMainNative);
                        startReloadNative();
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        startReloadNative();
                    }
                }, remoteKey);
    }

    private void loadOldAdFormat(int maxRequest) {
        if (myNativeAdMain != null) {
            myNativeAdMain.destroy();
        }
        if (!builder.getListIdAdMain().isEmpty()) {
            myNativeAdMain = Admob.getInstance().loadMultipleNativeAds1Id(currentActivity,
                    builder.getListIdAdMain().get(0),
                    builder.getFlAd(),
                    builder.getLayoutNativeAdmob(),
                    builder.getLayoutNativeMeta(),
                    builder.getLayoutShimmerNative(),
                    true,
                    builder.getCallback(),
                    this::startReloadNative,
                    () -> {
                        startReloadNative();
                        loadNativeBackup(true);
                    }, remoteKey, maxRequest);
        }
    }

    private void startReloadNative() {
        if (countDownTimer != null && this.lifecycleOwner.getLifecycle().getCurrentState() == Lifecycle.State.RESUMED && !isTimerRunning) {
            Log.d(TAG, "startReloadNative: ");
            isTimerRunning = true;
            countDownTimer.cancel();
            countDownTimer.start();
        }
    }

    public void setReloadAds() {
        isReloadAds = true;
    }

    public void reloadAdNow() {
        loadNativeFloor(builder.maxRequestReload);
    }

    public void setAlwaysReloadOnResume(boolean isAlwaysReloadOnResume) {
        this.isAlwaysReloadOnResume = isAlwaysReloadOnResume;
    }

    public String getRemoteKeySecondary() {
        return remoteKeySecondary;
    }

    public void setRemoteKeySecondary(String remoteKeySecondary) {
        this.remoteKeySecondary = remoteKeySecondary;
    }
}
