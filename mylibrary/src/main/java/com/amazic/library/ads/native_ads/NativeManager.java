package com.amazic.library.ads.native_ads;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.amazic.library.Utils.RemoteConfigHelper;
import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.callback.NativeCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;

public class NativeManager implements LifecycleEventObserver {
    private static final String TAG = "NativeManager";
    private final NativeBuilder builder;
    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private final String remoteKey;
    private boolean isReloadAds = false;
    private boolean isAlwaysReloadOnResume = false;
    private long intervalReloadNative = 0;
    private boolean isStop = false;
    private boolean isTimerRunning = false;
    private CountDownTimer countDownTimer;
    private String remoteKeySecondary = "";
    private String remoteKeyBackup = "";
    private NativeAd myNativeAdMain;
    private NativeAd myNativeAdSecondary;
    private Handler handlerTimeoutCallNative = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private int timeOutCallAds = 12000;
    private boolean canLoadMainNative = true;
    private boolean canLoadSecondaryNative = true;
    private String remoteKeyAdNativeDisplayOrder = null;
    private int randomPercentShowNativeMain = 100;
    private boolean isShowNativeSecond = false;
    private boolean isFailedMain = false;
    private boolean isFailedSecondary = false;

    public NativeManager(@NonNull Context context, LifecycleOwner lifecycleOwner, NativeBuilder builder, String remoteKey) {
        this.builder = builder;
        this.context = context;
        this.remoteKey = remoteKey;
        this.remoteKeySecondary = remoteKey;
        this.remoteKeyBackup = remoteKey;
        this.lifecycleOwner = lifecycleOwner;
        this.lifecycleOwner.getLifecycle().addObserver(this);
    }

    public NativeManager(@NonNull Context context, LifecycleOwner lifecycleOwner, NativeBuilder builder, String remoteKey, String remoteKeySecondary) {
        this.builder = builder;
        this.context = context;
        this.remoteKey = remoteKey;
        this.remoteKeySecondary = remoteKeySecondary;
        this.remoteKeyBackup = remoteKey;
        this.lifecycleOwner = lifecycleOwner;
        this.lifecycleOwner.getLifecycle().addObserver(this);
    }

    public NativeManager(@NonNull Context context, LifecycleOwner lifecycleOwner, NativeBuilder builder, String remoteKey, String remoteKeySecondary, String remoteKeyAdNativeDisplayOrder) {
        this.builder = builder;
        this.context = context;
        this.remoteKey = remoteKey;
        this.remoteKeySecondary = remoteKeySecondary;
        this.remoteKeyBackup = remoteKey;
        this.lifecycleOwner = lifecycleOwner;
        this.lifecycleOwner.getLifecycle().addObserver(this);
        this.remoteKeyAdNativeDisplayOrder = remoteKeyAdNativeDisplayOrder;
    }

    public NativeManager(@NonNull Context context, LifecycleOwner lifecycleOwner, NativeBuilder builder, String remoteKey, String remoteKeySecondary, String remoteKeyAdNativeDisplayOrder, String remoteKeyBackup) {
        this.builder = builder;
        this.context = context;
        this.remoteKey = remoteKey;
        this.remoteKeySecondary = remoteKeySecondary;
        this.remoteKeyBackup = remoteKeyBackup;
        this.lifecycleOwner = lifecycleOwner;
        this.lifecycleOwner.getLifecycle().addObserver(this);
        this.remoteKeyAdNativeDisplayOrder = remoteKeyAdNativeDisplayOrder;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                Log.d(TAG, "onStateChanged: ON_CREATE");
                randomPercentShowNativeMain = (int) (Math.random() * 101);
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
        isFailedMain = false;
        isFailedSecondary = false;
        if (builder.useNewAdLoading) {
            loadNewAdFormat();
        } else loadOldAdFormat(maxRequest);
    }

    private void handleTimeoutCallNative() {
        //Set timeout call native x(s) if cannot load
        runnable = () -> {
            if (!canLoadMainNative || !canLoadSecondaryNative) {
                canLoadMainNative = true;
                canLoadSecondaryNative = true;
                startReloadNative();
            }
            if (handlerTimeoutCallNative != null) {
                handlerTimeoutCallNative = null;
            }
        };
        if (handlerTimeoutCallNative != null) {
            handlerTimeoutCallNative.postDelayed(runnable, timeOutCallAds);
        }
    }

    private void loadNewAdFormat() {
        Log.d(TAG, "loadNewAdFormat: ");
        if (remoteKeySecondary.isEmpty()) {
            if (!Admob.getInstance().checkCondition(context, remoteKey)) {
                builder.shimmerFrameLayout.setVisibility(View.GONE);
                if (builder.nativeAdViewMain != null)
                    builder.nativeAdViewMain.setVisibility(View.GONE);
                if (builder.nativeAdViewSecondary != null)
                    builder.nativeAdViewSecondary.setVisibility(View.GONE);
                return;
            }
        } else {
            if (!Admob.getInstance().checkCondition(context, remoteKey) && !Admob.getInstance().checkCondition(context, remoteKeySecondary)) {
                builder.shimmerFrameLayout.setVisibility(View.GONE);
                if (builder.nativeAdViewMain != null)
                    builder.nativeAdViewMain.setVisibility(View.GONE);
                if (builder.nativeAdViewSecondary != null)
                    builder.nativeAdViewSecondary.setVisibility(View.GONE);
                return;
            }
        }

        if (myNativeAdMain == null && myNativeAdSecondary == null)
            builder.shimmerFrameLayout.setVisibility(View.VISIBLE);
        if (!Admob.getInstance().checkCondition(context, remoteKey)) {
            if (builder.nativeAdViewMain != null) builder.nativeAdViewMain.setVisibility(View.GONE);
        }
        if (!Admob.getInstance().checkCondition(context, remoteKeySecondary)) {
            if (builder.nativeAdViewSecondary != null)
                builder.nativeAdViewSecondary.setVisibility(View.GONE);
        }
        //check show native Main Or Second first
        if (remoteKeyAdNativeDisplayOrder != null) {
            long percentRemote = RemoteConfigHelper.getInstance().get_config_long(context, remoteKeyAdNativeDisplayOrder);
            isShowNativeSecond = randomPercentShowNativeMain > percentRemote;
            Log.d(TAG, "percent show Native Second: randomPercentShowNativeMain = " + randomPercentShowNativeMain + ", percentRemote = " + percentRemote);
        }
        //

        handleTimeoutCallNative();
        if (isShowNativeSecond) {
            //show ads native second len dau
            loadSecondaryNative();
            loadMainNative();
        } else {
            //show ads native main len dau
            loadMainNative();
            loadSecondaryNative();
        }
    }

    private void loadMainNative() {
        Log.d(TAG, "loadMainNative: " + canLoadMainNative);
        if (canLoadMainNative) {
            canLoadMainNative = false;
            if (myNativeAdMain != null) myNativeAdMain.destroy();
            Admob.getInstance().loadNativeAds(context, builder.getListIdAdMain(), new NativeCallback() {
                @Override
                public void onNativeAdLoaded(NativeAd nativeAd) {
                    super.onNativeAdLoaded(nativeAd);
                    canLoadMainNative = true;
                    Log.d(TAG, "onNativeAdLoaded: Main");
                    builder.getCallback().onNativeAdLoaded(nativeAd);
                    showNativeMain(nativeAd);
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    Log.d(TAG, "onAdImpression: Main");
                    builder.getCallback().onAdImpression();
                    handleImpressionNative();
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
                    canLoadMainNative = true;
                    Log.e(TAG, "onAdFailedToLoad: Main\n" + loadAdError.getMessage());
                    builder.getCallback().onAdFailedToLoad(loadAdError);
                    if (myNativeAdSecondary != null) {
                        builder.shimmerFrameLayout.setVisibility(View.GONE);
                    }
                    loadNativeBackup(true);
                }
            }, remoteKey);
        }
    }

    private void loadSecondaryNative() {
        Log.d(TAG, "loadSecondaryNative: " + canLoadSecondaryNative);
        if (canLoadSecondaryNative) {
            canLoadSecondaryNative = false;
            if (myNativeAdSecondary != null) myNativeAdSecondary.destroy();
            Admob.getInstance().loadNativeAds(context, builder.getListIdAdSecondary(), new NativeCallback() {
                @Override
                public void onNativeAdLoaded(NativeAd nativeAd) {
                    super.onNativeAdLoaded(nativeAd);
                    canLoadSecondaryNative = true;
                    builder.getCallback().onNativeAdLoaded(nativeAd);
                    Log.d(TAG, "onNativeAdLoaded: Secondary");
                    showNativeSecondary(nativeAd);
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    Log.d(TAG, "onAdImpression: Secondary");
                    builder.getCallback().onAdImpression();
//                    handleImpressionNativeSecondary();
                    handleImpressionNative();
                    startReloadNative();
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    canLoadSecondaryNative = true;
                    Log.e(TAG, "onAdFailedToLoad: Secondary\n" + loadAdError.getMessage());
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

    private void handleImpressionNative() {
        if (myNativeAdMain != null) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isShowNativeSecond) {
                    Log.d(TAG, "onAdImpression: Second - isShowNativeSecond = " + isShowNativeSecond);
                    //show ads native second len dau
                    if (builder.nativeAdViewMain != null)
                        builder.nativeAdViewMain.setVisibility(View.GONE);
                    builder.nativeAdViewSecondary.setVisibility(View.VISIBLE);
                    builder.shimmerFrameLayout.setVisibility(View.GONE);
                } else {
                    Log.d(TAG, "onAdImpression: Second - isShowNativeSecond = " + isShowNativeSecond);
                    //show ads native main len dau
                    if (builder.nativeAdViewSecondary != null)
                        builder.nativeAdViewSecondary.setVisibility(View.GONE);
                    builder.nativeAdViewMain.setVisibility(View.VISIBLE);
                    builder.shimmerFrameLayout.setVisibility(View.GONE);
                }
            }, 800);
        }
    }

    private void loadNativeBackup(boolean isMainNative) {
        Admob.getInstance().loadNativeAdsBackup(context, builder.getListIdAdBackup(), new NativeCallback() {
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
                Log.e(TAG, "onAdFailedToLoad: Backup ");
                if (isMainNative) {
                    isFailedMain = true;
                } else {
                    isFailedSecondary = true;
                }
                if (isFailedMain && isFailedSecondary && builder.shimmerFrameLayout != null) {
                    builder.shimmerFrameLayout.setVisibility(View.GONE);
                }
                startReloadNative();
            }
        }, remoteKey);
    }

    private void loadOldAdFormat(int maxRequest) {
        Log.d(TAG, "loadOldAdFormat: ");
        if (myNativeAdMain != null) {
            myNativeAdMain.destroy();
        }
        if (!builder.getListIdAdMain().isEmpty()) {
            myNativeAdMain = Admob.getInstance().loadMultipleNativeAds1Id(context, builder.getListIdAdMain().get(0), builder.getFlAd(), builder.getLayoutNativeAdmob(), builder.getLayoutNativeMeta(), builder.getLayoutShimmerNative(), true, builder.getCallback(), this::startReloadNative, () -> {
                startReloadNative();
                loadNativeBackup(true);
            }, remoteKey, maxRequest);
        }
    }

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

    private void startReloadNative() {
        Log.d(TAG, "startReloadNative: " + (countDownTimer != null)
                + " && " + (this.lifecycleOwner.getLifecycle().getCurrentState())
                + " && " + !isTimerRunning);
        if (countDownTimer != null && this.lifecycleOwner.getLifecycle().getCurrentState() == Lifecycle.State.RESUMED && !isTimerRunning) {
            Log.d(TAG, "startReloadNative: ok");
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

    public int getTimeOutCallAds() {
        return timeOutCallAds;
    }

    public void setTimeOutCallAds(int timeOutCallAds) {
        this.timeOutCallAds = timeOutCallAds;
    }

    public void setRemoteKeyAdNativeDisplayOrder(String remoteKeyAdNativeDisplayOrder) {
        this.remoteKeyAdNativeDisplayOrder = remoteKeyAdNativeDisplayOrder;
    }
}
