package com.amazic.library.ads.native_ads;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.List;

/**
 * Native "Squeeze Back" ads.
 * <p>
 * NOT SUPPORTED with the GMA Next-Gen SDK: the Squeeze Back format came from the
 * {@code noninterruptive-sdk} AAR, which is built on top of the legacy Mobile Ads SDK
 * ({@code com.google.android.gms.ads}) and cannot run next to the GMA Next-Gen SDK. The Next-Gen
 * SDK has no Squeeze Back format (only Picture-in-Picture, see
 * {@link com.amazic.library.ads.banner_ads.BannerPictureInPictureManager}).
 * <p>
 * The public API is kept so apps still compile, but every call is a no-op (no ad is requested).
 */
@Deprecated
public class NativeSqueezeBackManager implements LifecycleEventObserver {
    private static final String TAG = "NativeManager";
    private final LifecycleOwner lifecycleOwner;
    private final String remoteKey;

    public NativeSqueezeBackManager(@NonNull Activity activity, LifecycleOwner lifecycleOwner, List<String> listId, String remoteKey) {
        this(activity, lifecycleOwner, listId, remoteKey, false);
    }

    public NativeSqueezeBackManager(@NonNull Activity activity, LifecycleOwner lifecycleOwner, List<String> listId, String remoteKey, boolean isUsePreload) {
        this.remoteKey = remoteKey;
        this.lifecycleOwner = lifecycleOwner;
        this.lifecycleOwner.getLifecycle().addObserver(this);
        logNotSupported();
    }

    private void logNotSupported() {
        Log.w(TAG, "Native Squeeze Back is not supported by the GMA Next-Gen SDK, no ad is loaded. remoteKey=" + remoteKey);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            this.lifecycleOwner.getLifecycle().removeObserver(this);
        }
    }

    public void setIntervalReloadNative(long intervalReloadNative) {
    }

    public void startReloadNative() {
    }

    public void cancelAutoReloadNative() {
    }

    public void setReloadAds() {
    }

    public void setAlwaysReloadOnResume(boolean isAlwaysReloadOnResume) {
    }

    public void loadAds() {
        logNotSupported();
    }

    public void showAds() {
        logNotSupported();
    }

    public void hideAds() {
    }

    public void destroyAds() {
    }
}
