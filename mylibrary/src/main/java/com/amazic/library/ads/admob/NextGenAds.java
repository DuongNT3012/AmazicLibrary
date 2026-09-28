package com.amazic.library.ads.admob;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd;
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdPreloader;
import com.google.android.libraries.ads.mobile.sdk.banner.AdView;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdValue;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo;
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader;
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for the GMA Next-Gen SDK.
 * <p>
 * Differences with the legacy SDK that this class hides from the rest of the library:
 * <ul>
 *     <li>The SDK MUST be initialized (with the AdMob app id) before any ad is loaded, otherwise it
 *     throws. {@link #runWhenSdkReady(Context, Runnable)} queues work until initialization is done
 *     (and starts initialization if nobody did it yet).</li>
 *     <li>Load / event / preload callbacks are invoked on a background thread. The legacy SDK
 *     used the main thread and all the library code touches UI inside callbacks, so every
 *     callback is re-dispatched to the main thread by the {@code onMain(...)} wrappers.</li>
 * </ul>
 */
public final class NextGenAds {
    private static final String TAG = "NextGenAds";
    private static final String META_APP_ID = "com.google.android.gms.ads.APPLICATION_ID";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Object LOCK = new Object();
    private static final List<Runnable> PENDING = new ArrayList<>();
    private static final List<Runnable> INIT_DONE_LISTENERS = new ArrayList<>();
    private static volatile boolean sdkReady = false;
    private static volatile boolean initStarted = false;

    private NextGenAds() {
    }

    //region init
    /**
     * Reads the AdMob app id: the one set with {@link Admob#setAppID(String)} first, otherwise
     * the {@code com.google.android.gms.ads.APPLICATION_ID} meta-data of the app manifest
     * (still required by the UMP SDK, so every app already has it).
     */
    @NonNull
    public static String getAppId(@NonNull Context context) {
        String appId = Admob.getInstance().getAppID();
        if (appId != null && !appId.isEmpty()) return appId;
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = info.metaData;
            if (metaData != null) {
                String value = metaData.getString(META_APP_ID);
                if (value != null) return value;
            }
        } catch (Exception e) {
            Log.e(TAG, "getAppId: " + e.getMessage());
        }
        return "";
    }

    public static boolean isSdkReady() {
        return sdkReady;
    }

    /**
     * Initializes the SDK on a background thread (only once). {@code onAdapterInitDone} is called
     * on the main thread when all mediation adapters finished initializing.
     */
    public static void initialize(@NonNull Context context, @Nullable Runnable onAdapterInitDone) {
        final Context appContext = context.getApplicationContext();
        synchronized (LOCK) {
            if (onAdapterInitDone != null) {
                if (Admob.getInstance().getIsInitAdmobDone()) {
                    runOnMain(onAdapterInitDone);
                } else {
                    INIT_DONE_LISTENERS.add(onAdapterInitDone);
                }
            }
            if (initStarted) return;
            initStarted = true;
        }
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            try {
                // setNativeValidatorDisabled(): same as the legacy meta-data NATIVE_AD_DEBUGGER_ENABLED=false
                InitializationConfig config = new InitializationConfig.Builder(getAppId(appContext))
                        .setNativeValidatorDisabled()
                        .build();
                MobileAds.initialize(appContext, config, initializationStatus -> {
                    Log.d(TAG, "Adapters initialized in " + (System.currentTimeMillis() - startTime) + "ms: " + initializationStatus.getAdapterStatusMap());
                    List<Runnable> listeners;
                    synchronized (LOCK) {
                        Admob.getInstance().setIsInitAdmobDone(true);
                        listeners = new ArrayList<>(INIT_DONE_LISTENERS);
                        INIT_DONE_LISTENERS.clear();
                    }
                    for (Runnable r : listeners) runOnMain(r);
                });
            } catch (Throwable t) {
                Log.e(TAG, "MobileAds.initialize failed: " + t.getMessage(), t);
            }
            // The SDK can load ads as soon as initialize() returned (no need to wait for adapters).
            List<Runnable> pending;
            synchronized (LOCK) {
                sdkReady = true;
                pending = new ArrayList<>(PENDING);
                PENDING.clear();
            }
            Log.d(TAG, "SDK ready after " + (System.currentTimeMillis() - startTime) + "ms, run " + pending.size() + " pending request(s)");
            for (Runnable r : pending) runOnMain(r);
        }).start();
    }

    /**
     * Runs {@code task} on the main thread once the SDK is initialized (immediately if it already
     * is). Every ad load / preload of the library goes through here.
     */
    public static void runWhenSdkReady(@NonNull Context context, @NonNull Runnable task) {
        synchronized (LOCK) {
            if (!sdkReady) {
                PENDING.add(task);
                if (!initStarted) initialize(context, null);
                return;
            }
        }
        runOnMain(task);
    }
    //endregion

    //region threading
    public static void runOnMain(@NonNull Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            MAIN.post(runnable);
        }
    }

    public static <T> AdLoadCallback<T> onMain(@NonNull AdLoadCallback<T> callback) {
        return new AdLoadCallback<T>() {
            @Override
            public void onAdLoaded(@NonNull T ad) {
                runOnMain(() -> callback.onAdLoaded(ad));
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                runOnMain(() -> callback.onAdFailedToLoad(adError));
            }
        };
    }

    public static NativeAdLoaderCallback onMain(@NonNull NativeAdLoaderCallback callback) {
        return new NativeAdLoaderCallback() {
            @Override
            public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                runOnMain(() -> callback.onNativeAdLoaded(nativeAd));
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                runOnMain(() -> callback.onAdFailedToLoad(adError));
            }

            @Override
            public void onAdLoadingCompleted() {
                runOnMain(callback::onAdLoadingCompleted);
            }
        };
    }

    public static PreloadCallback onMain(@NonNull PreloadCallback callback) {
        return new PreloadCallback() {
            @Override
            public void onAdPreloaded(@NonNull String preloadId, @NonNull ResponseInfo responseInfo) {
                runOnMain(() -> callback.onAdPreloaded(preloadId, responseInfo));
            }

            @Override
            public void onAdFailedToPreload(@NonNull String preloadId, @NonNull LoadAdError adError) {
                runOnMain(() -> callback.onAdFailedToPreload(preloadId, adError));
            }

            @Override
            public void onAdsExhausted(@NonNull String preloadId) {
                runOnMain(() -> callback.onAdsExhausted(preloadId));
            }
        };
    }

    public static InterstitialAdEventCallback onMain(@NonNull InterstitialAdEventCallback c) {
        return new InterstitialAdEventCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                runOnMain(c::onAdShowedFullScreenContent);
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                runOnMain(c::onAdDismissedFullScreenContent);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError error) {
                runOnMain(() -> c.onAdFailedToShowFullScreenContent(error));
            }

            @Override
            public void onAdImpression() {
                runOnMain(c::onAdImpression);
            }

            @Override
            public void onAdClicked() {
                runOnMain(c::onAdClicked);
            }

            @Override
            public void onAdPaid(@NonNull AdValue value) {
                runOnMain(() -> c.onAdPaid(value));
            }
        };
    }

    public static RewardedAdEventCallback onMain(@NonNull RewardedAdEventCallback c) {
        return new RewardedAdEventCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                runOnMain(c::onAdShowedFullScreenContent);
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                runOnMain(c::onAdDismissedFullScreenContent);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError error) {
                runOnMain(() -> c.onAdFailedToShowFullScreenContent(error));
            }

            @Override
            public void onAdImpression() {
                runOnMain(c::onAdImpression);
            }

            @Override
            public void onAdClicked() {
                runOnMain(c::onAdClicked);
            }

            @Override
            public void onAdPaid(@NonNull AdValue value) {
                runOnMain(() -> c.onAdPaid(value));
            }
        };
    }

    public static RewardedInterstitialAdEventCallback onMain(@NonNull RewardedInterstitialAdEventCallback c) {
        return new RewardedInterstitialAdEventCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                runOnMain(c::onAdShowedFullScreenContent);
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                runOnMain(c::onAdDismissedFullScreenContent);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError error) {
                runOnMain(() -> c.onAdFailedToShowFullScreenContent(error));
            }

            @Override
            public void onAdImpression() {
                runOnMain(c::onAdImpression);
            }

            @Override
            public void onAdClicked() {
                runOnMain(c::onAdClicked);
            }

            @Override
            public void onAdPaid(@NonNull AdValue value) {
                runOnMain(() -> c.onAdPaid(value));
            }
        };
    }

    public static AppOpenAdEventCallback onMain(@NonNull AppOpenAdEventCallback c) {
        return new AppOpenAdEventCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                runOnMain(c::onAdShowedFullScreenContent);
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                runOnMain(c::onAdDismissedFullScreenContent);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError error) {
                runOnMain(() -> c.onAdFailedToShowFullScreenContent(error));
            }

            @Override
            public void onAdImpression() {
                runOnMain(c::onAdImpression);
            }

            @Override
            public void onAdClicked() {
                runOnMain(c::onAdClicked);
            }

            @Override
            public void onAdPaid(@NonNull AdValue value) {
                runOnMain(() -> c.onAdPaid(value));
            }
        };
    }

    public static BannerAdEventCallback onMain(@NonNull BannerAdEventCallback c) {
        return new BannerAdEventCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                runOnMain(c::onAdShowedFullScreenContent);
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                runOnMain(c::onAdDismissedFullScreenContent);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError error) {
                runOnMain(() -> c.onAdFailedToShowFullScreenContent(error));
            }

            @Override
            public void onAdImpression() {
                runOnMain(c::onAdImpression);
            }

            @Override
            public void onAdClicked() {
                runOnMain(c::onAdClicked);
            }

            @Override
            public void onAdPaid(@NonNull AdValue value) {
                runOnMain(() -> c.onAdPaid(value));
            }
        };
    }

    public static NativeAdEventCallback onMain(@NonNull NativeAdEventCallback c) {
        return new NativeAdEventCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                runOnMain(c::onAdShowedFullScreenContent);
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                runOnMain(c::onAdDismissedFullScreenContent);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError error) {
                runOnMain(() -> c.onAdFailedToShowFullScreenContent(error));
            }

            @Override
            public void onAdImpression() {
                runOnMain(c::onAdImpression);
            }

            @Override
            public void onAdClicked() {
                runOnMain(c::onAdClicked);
            }

            @Override
            public void onAdPaid(@NonNull AdValue value) {
                runOnMain(() -> c.onAdPaid(value));
            }
        };
    }
    //endregion

    //region load helpers (wait for init + callbacks on main thread)
    public static void loadInterstitial(@NonNull Context context, @NonNull String adUnitId, @NonNull AdLoadCallback<InterstitialAd> callback) {
        runWhenSdkReady(context, () -> InterstitialAd.load(new AdRequest.Builder(adUnitId).build(), onMain(callback)));
    }

    public static void loadRewarded(@NonNull Context context, @NonNull String adUnitId, @NonNull AdLoadCallback<RewardedAd> callback) {
        runWhenSdkReady(context, () -> RewardedAd.load(new AdRequest.Builder(adUnitId).build(), onMain(callback)));
    }

    public static void loadRewardedInterstitial(@NonNull Context context, @NonNull String adUnitId, @NonNull AdLoadCallback<RewardedInterstitialAd> callback) {
        runWhenSdkReady(context, () -> RewardedInterstitialAd.load(new AdRequest.Builder(adUnitId).build(), onMain(callback)));
    }

    public static void loadAppOpen(@NonNull Context context, @NonNull String adUnitId, @NonNull AdLoadCallback<AppOpenAd> callback) {
        runWhenSdkReady(context, () -> AppOpenAd.load(new AdRequest.Builder(adUnitId).build(), onMain(callback)));
    }

    public static void loadNative(@NonNull Context context, @NonNull NativeAdRequest request, @NonNull NativeAdLoaderCallback callback) {
        runWhenSdkReady(context, () -> NativeAdLoader.load(request, onMain(callback)));
    }

    public static void loadNative(@NonNull Context context, @NonNull NativeAdRequest request, int numberOfAds, @NonNull NativeAdLoaderCallback callback) {
        runWhenSdkReady(context, () -> NativeAdLoader.load(request, Math.max(1, Math.min(numberOfAds, 5)), onMain(callback)));
    }

    // Preloader accessors that are safe to call before the SDK is initialized.
    public static boolean isInterstitialPreloaded(@NonNull String adUnitId) {
        try {
            return sdkReady && InterstitialAdPreloader.isAdAvailable(adUnitId);
        } catch (Throwable t) {
            Log.e(TAG, "isInterstitialPreloaded: " + t.getMessage());
            return false;
        }
    }

    public static int getInterstitialPreloadCount(@NonNull String adUnitId) {
        try {
            return sdkReady ? InterstitialAdPreloader.getNumAdsAvailable(adUnitId) : 0;
        } catch (Throwable t) {
            Log.e(TAG, "getInterstitialPreloadCount: " + t.getMessage());
            return 0;
        }
    }

    @Nullable
    public static InterstitialAd pollInterstitial(@NonNull String adUnitId) {
        try {
            return sdkReady ? InterstitialAdPreloader.pollAd(adUnitId) : null;
        } catch (Throwable t) {
            Log.e(TAG, "pollInterstitial: " + t.getMessage());
            return null;
        }
    }

    public static boolean isRewardedPreloaded(@NonNull String adUnitId) {
        try {
            return sdkReady && RewardedAdPreloader.isAdAvailable(adUnitId);
        } catch (Throwable t) {
            Log.e(TAG, "isRewardedPreloaded: " + t.getMessage());
            return false;
        }
    }

    @Nullable
    public static RewardedAd pollRewarded(@NonNull String adUnitId) {
        try {
            return sdkReady ? RewardedAdPreloader.pollAd(adUnitId) : null;
        } catch (Throwable t) {
            Log.e(TAG, "pollRewarded: " + t.getMessage());
            return null;
        }
    }

    public static boolean isAppOpenPreloaded(@NonNull String adUnitId) {
        try {
            return sdkReady && AppOpenAdPreloader.isAdAvailable(adUnitId);
        } catch (Throwable t) {
            Log.e(TAG, "isAppOpenPreloaded: " + t.getMessage());
            return false;
        }
    }

    @Nullable
    public static AppOpenAd pollAppOpen(@NonNull String adUnitId) {
        try {
            return sdkReady ? AppOpenAdPreloader.pollAd(adUnitId) : null;
        } catch (Throwable t) {
            Log.e(TAG, "pollAppOpen: " + t.getMessage());
            return null;
        }
    }

    public static void destroyInterstitialPreload(@NonNull String adUnitId) {
        try {
            if (sdkReady) InterstitialAdPreloader.destroy(adUnitId);
        } catch (Throwable t) {
            Log.e(TAG, "destroyInterstitialPreload: " + t.getMessage());
        }
    }

    public static void loadBanner(@NonNull Context context, @NonNull AdView adView, @NonNull BannerAdRequest request, @NonNull AdLoadCallback<BannerAd> callback) {
        runWhenSdkReady(context, () -> adView.loadAd(request, onMain(callback)));
    }
    //endregion
}
