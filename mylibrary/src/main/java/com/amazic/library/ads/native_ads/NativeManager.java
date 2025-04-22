package com.amazic.library.ads.native_ads;

import android.app.Activity;
import android.os.CountDownTimer;
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
import com.google.android.gms.ads.nativead.NativeAdView;

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
    private final String remoteKey;
    private NativeAd myNativeAdSecond;
    private NativeAd myNativeAdFist;

    public void setIntervalReloadNative(long intervalReloadNative) {
        if (intervalReloadNative > 0) {
            this.intervalReloadNative = intervalReloadNative;
            countDownTimer = new CountDownTimer(this.intervalReloadNative, 1000) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    loadNativeFloor(builder.maxRequestReload);
                }
            };
        }
    }

    public void cancelAutoReloadNative(){
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    public void resumeAutoReloadNative(){
        if (countDownTimer != null) {
            countDownTimer.start();
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
                if (myNativeAdSecond != null) {
                    myNativeAdSecond.destroy();
                }
                if (myNativeAdFist != null) {
                    myNativeAdFist.destroy();
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
        if (Admob.getInstance().checkCondition(currentActivity, remoteKey)) {
            if (myNativeAdFist != null) {
                myNativeAdFist.destroy();
            }
            if (myNativeAdSecond != null) {
                myNativeAdSecond.destroy();
            }

            if (!builder.getListIdAdFirst().isEmpty()) {
                loadAdFirst();
            }
            if (!builder.getListIdAd().isEmpty()) {
                loadAdSecond();
            }
        }else{
            builder.nativeAdView.setVisibility(View.GONE);
            builder.nativeAdViewBackup.setVisibility(View.GONE);
            builder.shimmerFrameLayout.setVisibility(View.GONE);
        }
    }

    private void loadAdSecond() {
        Log.d(TAG, "loadAdSecond: ");
        Admob.getInstance().loadMultipleNativeAd(currentActivity, builder.getListIdAd().get(0),
                new NativeCallback() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        super.onNativeAdLoaded(nativeAd);
                        myNativeAdSecond = nativeAd;
                        Log.d(TAG, "onNativeAdLoaded: loadAdSecond");
                        builder.getCallback().onNativeAdLoaded(nativeAd);
                        builder.nativeAdView.setVisibility(View.VISIBLE);
                        builder.shimmerFrameLayout.setVisibility(View.GONE);
                        builder.nativeAdView.setNativeAd(myNativeAdSecond);
                        Admob.getInstance().populateNativeAdView(myNativeAdSecond, builder.nativeAdView);
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        Log.d(TAG, "onNativeAdLoaded: onAdFailedToLoad");
                        builder.getCallback().onAdFailedToLoad(loadAdError);
                        if (myNativeAdSecond == null) {
                            builder.shimmerFrameLayout.setVisibility(View.GONE);
                            builder.nativeAdView.setVisibility(View.GONE);
                            Admob.getInstance().populateNativeAdView(myNativeAdFist, builder.nativeAdView);
                        }
                        if (countDownTimer != null && NativeManager.this.lifecycleOwner.getLifecycle().getCurrentState() == Lifecycle.State.RESUMED) {
                            countDownTimer.cancel();
                            countDownTimer.start();
                        }
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        builder.getCallback().onAdImpression();
                        if (countDownTimer != null && NativeManager.this.lifecycleOwner.getLifecycle().getCurrentState() == Lifecycle.State.RESUMED) {
                            countDownTimer.cancel();
                            countDownTimer.start();
                            Log.d(TAG, "onAdImpression: countDownTimer");
                        }
                        Log.d(TAG, "onAdImpression: getListIdAd");
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        builder.getCallback().onAdClicked();
                    }

                    @Override
                    public void onAdShown(NativeAdView adView) {
                        super.onAdShown(adView);
                        builder.getCallback().onAdShown(adView);
                    }
                }, remoteKey, builder.maxRequestBackup);
    }

    private void loadAdFirst() {
        Log.d(TAG, "loadAdFirst: ");
        Admob.getInstance().loadMultipleNativeAd(currentActivity, builder.getListIdAdFirst().get(0),
                new NativeCallback() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        super.onNativeAdLoaded(nativeAd);
                        myNativeAdFist = nativeAd;
                        builder.nativeAdView.setVisibility(View.INVISIBLE);
                        builder.nativeAdViewBackup.setVisibility(View.VISIBLE);
                        builder.shimmerFrameLayout.setVisibility(View.GONE);
                        builder.nativeAdViewBackup.setNativeAd(myNativeAdFist);
                        Admob.getInstance().populateNativeAdView(myNativeAdFist, builder.nativeAdViewBackup);
                        builder.getCallback().onNativeAdLoaded(nativeAd);
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        if (myNativeAdFist == null) {
                            builder.shimmerFrameLayout.setVisibility(View.GONE);
                            builder.nativeAdViewBackup.setVisibility(View.GONE);
                            Admob.getInstance().populateNativeAdView(myNativeAdSecond, builder.nativeAdView);
                        }
                        builder.getCallback().onAdFailedToLoad(loadAdError);
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        builder.getCallback().onAdImpression();
                        Log.d(TAG, "onAdImpression: getListIdAdFirst");
                        if (myNativeAdSecond != null) {
                            builder.nativeAdView.setVisibility(View.VISIBLE);
                            Admob.getInstance().populateNativeAdView(myNativeAdSecond, builder.nativeAdView);
                        }
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        builder.getCallback().onAdClicked();
                    }

                    @Override
                    public void onAdShown(NativeAdView adView) {
                        super.onAdShown(adView);
                        builder.getCallback().onAdShown(adView);
                    }
                }, remoteKey, builder.maxRequest);
    }

    private void loadOldAdFormat(int maxRequest) {
        if (myNativeAdSecond != null) {
            myNativeAdSecond.destroy();
        }
        if (!builder.getListIdAd().isEmpty()) {
            myNativeAdSecond = Admob.getInstance().loadMultipleNativeAds1Id(currentActivity,
                    builder.getListIdAd().get(0),
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
                    },
                    () -> {
                        if (countDownTimer != null && this.lifecycleOwner.getLifecycle().getCurrentState() == Lifecycle.State.RESUMED) {
                            countDownTimer.cancel();
                            countDownTimer.start();
                        }
                    }, remoteKey, maxRequest);
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
}
