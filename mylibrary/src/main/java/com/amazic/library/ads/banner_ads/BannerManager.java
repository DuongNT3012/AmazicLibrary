package com.amazic.library.ads.banner_ads;

import android.app.Activity;
import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.amazic.library.ads.admob.Admob;

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
    private FrameLayout frContainer;
    private boolean isLoadBannerFragment = false;
    private String adsKey;

    public void setIntervalReloadBanner(long intervalReloadBanner) {
        if (intervalReloadBanner > 0) {
            this.intervalReloadBanner = intervalReloadBanner;
            countDownTimer = new CountDownTimer(this.intervalReloadBanner, 1000) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    if (isLoadBannerFragment) {
                        loadBannerFragment(frContainer);
                    } else {
                        loadBanner(frContainer);
                    }
                }
            };
        }
    }

    public BannerManager(@NonNull Activity currentActivity, FrameLayout frContainer, LifecycleOwner lifecycleOwner, BannerBuilder builder, String adsKey) {
        this.isLoadBannerFragment = false;
        this.builder = builder;
        this.currentActivity = currentActivity;
        this.frContainer = frContainer;
        this.adsKey = adsKey;
        this.lifecycleOwner = lifecycleOwner;
        this.lifecycleOwner.getLifecycle().addObserver(this);
    }

    public BannerManager(Context context, int adWidth, FrameLayout frContainer, LifecycleOwner lifecycleOwner, BannerBuilder builder, String adsKey) {
        this.isLoadBannerFragment = true;
        this.builder = builder;
        this.context = context;
        this.adWidth = adWidth;
        this.frContainer = frContainer;
        this.adsKey = adsKey;
        this.lifecycleOwner = lifecycleOwner;
        this.lifecycleOwner.getLifecycle().addObserver(this);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                Log.d(TAG, "onStateChanged: ON_CREATE");
                if (isLoadBannerFragment) {
                    loadBannerFragment(frContainer);
                } else {
                    loadBanner(frContainer);
                }
                break;
            case ON_RESUME:
                if (countDownTimer != null && isStop) {
                    countDownTimer.start();
                }
                String valueLog = isStop + " && " + (isReloadAds || isAlwaysReloadOnResume);
                Log.d(TAG, "onStateChanged: resume\n" + valueLog);
                if (isStop && (isReloadAds || isAlwaysReloadOnResume)) {
                    isReloadAds = false;
                    if (isLoadBannerFragment) {
                        loadBannerFragment(frContainer);
                    } else {
                        loadBanner(frContainer);
                    }
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
                this.lifecycleOwner.getLifecycle().removeObserver(this);
                break;
        }
    }

    private void loadBanner(FrameLayout frContainer) {
        Log.d(TAG, "loadBanner: " + builder.getListId());
        if (Admob.getInstance().getShowAllAds()) {
            Admob.getInstance().loadBannerAds(currentActivity, builder.getListId(), frContainer, builder.getCallBack(), () -> {
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                    countDownTimer.start();
                }
            }, adsKey);
        } else {
            frContainer.setVisibility(View.GONE);
        }
    }

    private void loadBannerFragment(FrameLayout frContainer) {
        Log.d(TAG, "loadBanner: " + builder.getListId());
        if (Admob.getInstance().getShowAllAds()) {
            Admob.getInstance().loadBannerAds(context, adWidth, builder.getListId(), frContainer, builder.getCallBack(), () -> {
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                    countDownTimer.start();
                }
            }, adsKey);
        } else {
            frContainer.setVisibility(View.GONE);
        }
    }


    public void setReloadAds() {
        isReloadAds = true;
    }

    public void reloadAdNow() {
        if (isLoadBannerFragment) {
            loadBannerFragment(frContainer);
        } else {
            loadBanner(frContainer);
        }
    }

    public void setAlwaysReloadOnResume(boolean isAlwaysReloadOnResume) {
        this.isAlwaysReloadOnResume = isAlwaysReloadOnResume;
    }
}
