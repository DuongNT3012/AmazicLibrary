package com.google.ads.mediation.sample.customevent;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ads.mediation.sample.customevent.util.FirebaseAnalyticsUtil;
import com.google.ads.mediation.sample.sdk.SampleRewardedAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/**
 * Rewarded custom event loader for the SampleSDK.
 */
public class RewardedCustomEventLoader {

    /**
     * Represents a {@link SampleRewardedAd}.
     */

    /**
     * Configuration for requesting the rewarded ad from the third party network.
     */
    private final MediationRewardedAdConfiguration mediationRewardedAdConfiguration;

    /**
     * A {@link MediationAdLoadCallback} that handles any callback when a Sample rewarded ad finishes
     * loading.
     */
    private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
            mediationAdLoadCallback;

    /**
     * Used to forward rewarded video ad events to the Google Mobile Ads SDK.
     */
    private MediationRewardedAdCallback rewardedAdCallback;

    /**
     * Tag used for log statements
     */
    private static final String TAG = "RewardedCustomEvent";

    public RewardedCustomEventLoader(
            MediationRewardedAdConfiguration adConfiguration,
            MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> adLoadCallback) {
        this.mediationRewardedAdConfiguration = adConfiguration;
        this.mediationAdLoadCallback = adLoadCallback;
    }

    /**
     * Loads the rewarded ad from the third party ad network.
     */
    public void loadAd() {
        Log.i(TAG, "Begin loading rewarded ad.");
        String serverParameter = mediationRewardedAdConfiguration.getServerParameters().getString("parameter");
        if (TextUtils.isEmpty(serverParameter)) {
            mediationAdLoadCallback.onFailure(CustomEventError.createCustomEventNoAdIdError());
            return;
        }
        Log.d(TAG, "Received server parameter.");

        Context context = mediationRewardedAdConfiguration.getContext();

        // Log event to Firebase
        FirebaseAnalyticsUtil.logEventMediationAdx(context, FirebaseAnalyticsUtil.REWARD);

        AdManagerAdRequest adRequest = new AdManagerAdRequest.Builder().build();
        RewardedAd.load(context, serverParameter, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mediationAdLoadCallback.onFailure(new AdError(loadAdError.getCode(), loadAdError.getMessage(), loadAdError.getDomain()));
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                rewardedAdCallback = mediationAdLoadCallback.onSuccess(new MediationRewardedAd() {
                    @Override
                    public void showAd(@NonNull Context context) {
                        rewardedAd.show((Activity) context, rewardItem -> {
                            if (rewardedAdCallback != null) {
                                rewardedAdCallback.onUserEarnedReward();
                            }
                        });
                    }
                });
                rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        super.onAdFailedToShowFullScreenContent(adError);
                        if (rewardedAdCallback != null)
                            rewardedAdCallback.onAdFailedToShow(adError);
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        if (rewardedAdCallback != null)
                            rewardedAdCallback.reportAdImpression();
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent();
                        if (rewardedAdCallback != null)
                            rewardedAdCallback.onAdClosed();
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (rewardedAdCallback != null)
                            rewardedAdCallback.reportAdClicked();
                    }
                });
            }
        });
    }
}
