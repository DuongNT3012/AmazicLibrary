package com.amazic.library.ads.collapse_banner_ads;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.admob_interface.IOnAdsFailToLoad;
import com.amazic.library.ads.admob.admob_interface.IOnAdsImpression;
import com.google.android.gms.ads.AdView;

public class CollapseBannerManager implements LifecycleEventObserver {
    private static final String TAG = "CollapseBannerManager";
    private final CollapseBannerBuilder builder;
    private final LifecycleOwner lifecycleOwner;
    public AdView adView;
    private AppCompatActivity currentActivity;
    private boolean isReloadAds = false;
    private boolean isAlwaysReloadOnResume = false;
    private long intervalReloadBanner = 0;
    private boolean isStop = false;
    private CountDownTimer countDownTimer;
    private Context context;
    private int adWidth;
    private final FrameLayout frContainer;
    private boolean isLoadBannerFragment = false;
    private final String remoteKey;
    private boolean isAutoReload = true;

    public CollapseBannerManager(@NonNull AppCompatActivity currentActivity, FrameLayout frContainer, LifecycleOwner lifecycleOwner, CollapseBannerBuilder builder, String remoteKey) {
        this.isLoadBannerFragment = false;
        this.builder = builder;
        this.currentActivity = currentActivity;
        this.frContainer = frContainer;
        this.remoteKey = remoteKey;
        this.lifecycleOwner = lifecycleOwner;
        this.lifecycleOwner.getLifecycle().addObserver(this);
    }

    public CollapseBannerManager(Context context, int adWidth, FrameLayout frContainer, LifecycleOwner lifecycleOwner, CollapseBannerBuilder builder, String remoteKey) {
        this.isLoadBannerFragment = true;
        this.builder = builder;
        this.context = context;
        this.adWidth = adWidth;
        this.frContainer = frContainer;
        this.remoteKey = remoteKey;
        this.lifecycleOwner = lifecycleOwner;
        this.lifecycleOwner.getLifecycle().addObserver(this);
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
                    if (isLoadBannerFragment) {
                        loadCollapseBannerFragment(frContainer);
                    } else {
                        loadCollapseBanner(frContainer);
                    }
                }
            };
        }
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                Log.d(TAG, "onStateChanged: ON_CREATE");
                if (isLoadBannerFragment) {
                    loadCollapseBannerFragment(frContainer);
                } else {
                    loadCollapseBanner(frContainer);
                }
                break;
            case ON_RESUME:
                if (isAutoReload) {
                    if (countDownTimer != null && isStop) {
                        startReloadCollapse();
                    }
                    String valueLog = isStop + " && " + (isReloadAds || isAlwaysReloadOnResume);
                    Log.d(TAG, "onStateChanged: resume\n" + valueLog);
                    if (isStop && (isReloadAds || isAlwaysReloadOnResume)) {
                        isReloadAds = false;
                        if (isLoadBannerFragment) {
                            loadCollapseBannerFragment(frContainer);
                        } else {
                            loadCollapseBanner(frContainer);
                        }
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

    private void loadCollapseBanner(FrameLayout frContainer) {
        Log.d(TAG, "loadBanner: " + builder.getListId());
        if (Admob.getInstance().getShowAllAds()) {
            if (adView != null) {
                adView.destroy();
            }
            adView = Admob.getInstance().loadCollapseBanner(currentActivity, builder.getListId(), frContainer, builder.getBannerGravity(), builder.getCallBack()
                    , new IOnAdsImpression() {
                        @Override
                        public void onAdsImpression() {
                            startReloadCollapse();
                        }
                    }, new IOnAdsFailToLoad() {
                        @Override
                        public void onAdsFailToLoad() {
                            startReloadCollapse();
                        }
                    }, builder.getCollapseTypeClose(), builder.getValueCountDownOrCountClick(), remoteKey);
        } else {
            frContainer.setVisibility(View.GONE);
        }
    }

    private void startReloadCollapse() {
        if (countDownTimer != null && this.lifecycleOwner.getLifecycle().getCurrentState() == Lifecycle.State.RESUMED) {
            countDownTimer.cancel();
            countDownTimer.start();
        }
    }

    private void loadCollapseBannerFragment(FrameLayout frContainer) {
        Log.d(TAG, "loadBanner: " + builder.getListId());
        if (Admob.getInstance().getShowAllAds()) {
            if (adView != null) {
                adView.destroy();
            }
            adView = Admob.getInstance().loadCollapseBanner(context, adWidth, builder.getListId(), frContainer, builder.getBannerGravity(), builder.getCallBack()
                    , new IOnAdsImpression() {
                        @Override
                        public void onAdsImpression() {
                            startReloadCollapse();
                        }
                    }, new IOnAdsFailToLoad() {
                        @Override
                        public void onAdsFailToLoad() {
                            startReloadCollapse();
                        }
                    }, builder.getCollapseTypeClose(), builder.getValueCountDownOrCountClick(), remoteKey);
        } else {
            frContainer.setVisibility(View.GONE);
        }
    }

    public void destroyCollapseBanner() {
        if (adView != null) {
            adView.destroy();
        }
    }

    public void cancelAutoReloadCollapseBanner() {
        isAutoReload = false;
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    public void resumeAutoReloadCollapseBanner() {
        isAutoReload = true;
        if (countDownTimer != null) {
            countDownTimer.start();
        }
    }

    public void setReloadAds() {
        isReloadAds = true;
    }

    public void reloadAdNow() {
        if (isLoadBannerFragment) {
            loadCollapseBannerFragment(frContainer);
        } else {
            loadCollapseBanner(frContainer);
        }
    }

    public void setAlwaysReloadOnResume(boolean isAlwaysReloadOnResume) {
        this.isAlwaysReloadOnResume = isAlwaysReloadOnResume;
    }
}
