package com.amazic.library.ads.banner_ads;

import android.app.Activity;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.callback.BannerCallback;

public class BannerManager implements LifecycleEventObserver {
    private static final String TAG = "BannerManager";
    private final BannerBuilder builder;
    private Activity currentActivity;
    private final LifecycleOwner lifecycleOwner;
    private boolean isReloadAds = false;
    private boolean isAlwaysReloadOnResume = false;
    private long intervalReloadBanner = 0;
    private boolean isStop = false;
    private CountDownTimer countDownTimer;
    private Context context;
    private int adWidth;
    private boolean isLoadBannerFragment = false;
    private final String remoteKey;
    private String remoteKeySecondary = "";
    private String remoteKeyBackup = "";
    private boolean isLoadedBannerMain = false;
    private boolean isLoadedBannerSecondary = false;

    public BannerManager(@NonNull Activity currentActivity, LifecycleOwner lifecycleOwner, BannerBuilder builder, String remoteKey) {
        this.isLoadBannerFragment = false;
        this.builder = builder;
        this.currentActivity = currentActivity;
        this.remoteKey = remoteKey;
        this.remoteKeySecondary = remoteKey;
        this.remoteKeyBackup = remoteKey;
        this.lifecycleOwner = lifecycleOwner;
        this.lifecycleOwner.getLifecycle().addObserver(this);
    }

    public BannerManager(Context context, int adWidth, LifecycleOwner lifecycleOwner, BannerBuilder builder, String remoteKey) {
        this.isLoadBannerFragment = true;
        this.builder = builder;
        this.context = context;
        this.adWidth = adWidth;
        this.remoteKey = remoteKey;
        this.remoteKeySecondary = remoteKey;
        this.remoteKeyBackup = remoteKey;
        this.lifecycleOwner = lifecycleOwner;
        this.lifecycleOwner.getLifecycle().addObserver(this);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                Log.d(TAG, "onStateChanged: ON_CREATE");
                reloadAdNow();
                break;
            case ON_RESUME:
                if (countDownTimer != null && isStop) {
                    startReloadBanner();
                }
                String valueLog = isStop + " && " + (isReloadAds || isAlwaysReloadOnResume);
                Log.d(TAG, "onStateChanged: resume\n" + valueLog);
                if (isStop && (isReloadAds || isAlwaysReloadOnResume)) {
                    isReloadAds = false;
                    reloadAdNow();
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
                Log.d(TAG, "onStateChanged: ON_DESTROY");
                if (builder.getFrContainer() != null) {
                    builder.getFrContainer().removeAllViews();
                }
                if (builder.bannerAdViewMain != null) {
                    builder.bannerAdViewMain.destroy();
                }
                if (builder.bannerAdViewSecondary != null) {
                    builder.bannerAdViewSecondary.destroy();
                }
                this.lifecycleOwner.getLifecycle().removeObserver(this);
                break;
        }
    }

    private void loadBanner(FrameLayout frContainer) {
        Log.d(TAG, "loadBanner: " + builder.getListIdAdMain());
        if (Admob.getInstance().getShowAllAds()) {
            Admob.getInstance().loadBannerAds(currentActivity, builder.getListIdAdMain(), frContainer, builder.getCallBack(), this::startReloadBanner, remoteKey);
        } else {
            frContainer.setVisibility(View.GONE);
        }
    }

    private void loadBannerFragment(FrameLayout frContainer) {
        Log.d(TAG, "loadBanner: " + builder.getListIdAdMain());
        if (Admob.getInstance().getShowAllAds()) {
            Admob.getInstance().loadBannerAds(context, adWidth, builder.getListIdAdMain(), frContainer, builder.getCallBack(), this::startReloadBanner, remoteKey);
        } else {
            frContainer.setVisibility(View.GONE);
        }
    }

    public void setReloadAds() {
        isReloadAds = true;
    }

    public void reloadAdNow() {
        if (builder.useNewAdLoading) {
            loadNewAdFormat();
        } else {
            loadOldAdFormat();
        }
    }

    private void loadNewAdFormat() {
        if (remoteKeySecondary.isEmpty()) {
            if (!Admob.getInstance().checkCondition(currentActivity, remoteKey)) {
                if (builder.getFrContainer() != null) {
                    builder.getFrContainer().removeAllViews();
                }
                return;
            }
        } else {
            if (!Admob.getInstance().checkCondition(currentActivity, remoteKey) && !Admob.getInstance().checkCondition(currentActivity, remoteKeySecondary)) {
                if (builder.getFrContainer() != null) {
                    builder.getFrContainer().removeAllViews();
                }
                return;
            }
        }
        loadMainBanner();
        loadSecondaryBanner();
    }

    private void loadMainBanner() {
        isLoadedBannerMain = false;
        if (builder.bannerAdViewMain != null) {
            builder.bannerAdViewMain.destroy();
        }
        builder.bannerAdViewMain = Admob.getInstance().loadBannerAdsWithoutShow(currentActivity, builder.getListIdAdMain(), new BannerCallback() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                isLoadedBannerMain = true;
                builder.getCallBack().onAdLoaded();
                Log.d(TAG, "onAdLoaded: Main");
                if (builder.getFrContainer() != null) {
                    builder.getFrContainer().removeView(builder.bannerAdViewSecondary);
                    builder.getFrContainer().addView(builder.bannerAdViewMain);
                    builder.getFrContainer().removeView(builder.shimmerBanner);
                }
            }

            @Override
            public void onAdFailedToLoad() {
                super.onAdFailedToLoad();
                builder.getCallBack().onAdFailedToLoad();
                Log.d(TAG, "onAdFailedToLoad: Main");
                if (isLoadedBannerSecondary) {
                    builder.getFrContainer().removeView(builder.shimmerBanner);
                }
                loadBannerBackup();
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                builder.getCallBack().onAdImpression();
                Log.d(TAG, "onAdImpression: Main");
                startReloadBanner();
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                builder.getCallBack().onAdClicked();
                Log.d(TAG, "onAdClicked: Main");
            }
        }, remoteKey);
    }

    private void loadSecondaryBanner() {
        isLoadedBannerSecondary = false;
        if (builder.bannerAdViewSecondary != null) {
            builder.bannerAdViewSecondary.destroy();
        }
        builder.bannerAdViewSecondary = Admob.getInstance().loadBannerAdsWithoutShow(currentActivity, builder.getListIdAdSecondary(), new BannerCallback() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                isLoadedBannerSecondary = true;
                builder.getCallBack().onAdLoaded();
                Log.d(TAG, "onAdLoaded: Secondary");
                if (builder.getFrContainer() != null) {
                    builder.getFrContainer().removeView(builder.bannerAdViewMain);
                    builder.getFrContainer().addView(builder.bannerAdViewSecondary);
                    builder.getFrContainer().removeView(builder.shimmerBanner);
                }
            }

            @Override
            public void onAdFailedToLoad() {
                super.onAdFailedToLoad();
                builder.getCallBack().onAdFailedToLoad();
                Log.d(TAG, "onAdFailedToLoad: Secondary");
                if (isLoadedBannerMain) {
                    builder.getFrContainer().removeView(builder.shimmerBanner);
                }
                loadBannerBackup();
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                builder.getCallBack().onAdImpression();
                Log.d(TAG, "onAdImpression: Secondary");
                handleImpressionBannerSecondary();
                startReloadBanner();
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                builder.getCallBack().onAdClicked();
                Log.d(TAG, "onAdClicked: Secondary");
            }
        }, remoteKeySecondary);
    }

    private void handleImpressionBannerSecondary() {
        try {
            if (isLoadedBannerMain) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (builder.getFrContainer() != null && builder.bannerAdViewSecondary != null) {
                        builder.getFrContainer().removeView(builder.bannerAdViewSecondary);
                        if (builder.bannerAdViewMain != null) {
                            ViewParent parent = builder.bannerAdViewMain.getParent();
                            if (parent instanceof ViewGroup) {
                                ((ViewGroup) parent).removeView(builder.bannerAdViewMain);
                            }
                            builder.getFrContainer().addView(builder.bannerAdViewMain);
                        }
                    }
                    if (builder.getFrContainer() != null && builder.shimmerBanner != null) {
                        builder.getFrContainer().removeView(builder.shimmerBanner);
                    }
                }, 1000);
            }
        } catch (Exception e) {
            Log.d(TAG, "handleImpressionBannerSecondary: " + e.getMessage());
        }
    }

    private void loadBannerBackup() {
        if (builder.bannerAdViewBackup != null) {
            builder.bannerAdViewBackup.destroy();
        }
        builder.bannerAdViewBackup = Admob.getInstance().loadBannerAdsBackupWithoutShow(currentActivity, builder.getListIdAdBackup(), new BannerCallback() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                Log.d(TAG, "onAdLoaded: Backup");
                if (builder.getFrContainer() != null) {
                    builder.getFrContainer().removeView(builder.bannerAdViewMain);
                    builder.getFrContainer().removeView(builder.bannerAdViewSecondary);
                    builder.getFrContainer().removeView(builder.shimmerBanner);
                    builder.getFrContainer().addView(builder.bannerAdViewBackup);
                }
            }

            @Override
            public void onAdFailedToLoad() {
                super.onAdFailedToLoad();
                Log.d(TAG, "onAdFailedToLoad: Backup");
                startReloadBanner();
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                Log.d(TAG, "onAdImpression: Backup");
                startReloadBanner();
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                Log.d(TAG, "onAdClicked: Backup");
            }
        }, remoteKeyBackup);
    }

    private void loadOldAdFormat() {
        if (isLoadBannerFragment) {
            loadBannerFragment(builder.getFrContainer());
        } else {
            loadBanner(builder.getFrContainer());
        }
    }

    public void setAlwaysReloadOnResume(boolean isAlwaysReloadOnResume) {
        this.isAlwaysReloadOnResume = isAlwaysReloadOnResume;
    }

    private void startReloadBanner() {
        if (countDownTimer != null && this.lifecycleOwner.getLifecycle().getCurrentState() == Lifecycle.State.RESUMED) {
            Log.d(TAG, "startReloadBanner: ");
            countDownTimer.cancel();
            countDownTimer.start();
        }
    }

    public String getRemoteKeyBackup() {
        return remoteKeyBackup;
    }

    public void setRemoteKeyBackup(String remoteKeyBackup) {
        this.remoteKeyBackup = remoteKeyBackup;
    }

    public String getRemoteKeySecondary() {
        return remoteKeySecondary;
    }

    public void setRemoteKeySecondary(String remoteKeySecondary) {
        this.remoteKeySecondary = remoteKeySecondary;
    }

    public void setIntervalReloadBanner(long intervalReloadBanner) {
        if (intervalReloadBanner > 0) {
            this.intervalReloadBanner = intervalReloadBanner;
            countDownTimer = new CountDownTimer(this.intervalReloadBanner, 1000) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    reloadAdNow();
                }
            };
        }
    }

    public void cancelAutoReloadBanner() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    public void resumeAutoReloadBanner() {
        if (countDownTimer != null) {
            countDownTimer.start();
        }
    }
}
