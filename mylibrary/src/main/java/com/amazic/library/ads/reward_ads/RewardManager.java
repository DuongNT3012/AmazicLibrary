package com.amazic.library.ads.reward_ads;

import android.app.Activity;
import android.util.Log;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.ads.callback.RewardedCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;

import java.util.HashMap;
import java.util.Map;

public class RewardManager {
    private static final String TAG = "RewardManager";
    private static final Map<String, RewardedAd> listReward = new HashMap<>();
    public static void loadRewardAds(Activity activity, String adsKey, String remoteKey) {
        if (listReward.get(adsKey) == null) {
            Admob.getInstance().loadRewardAds(activity, AdmobApi.getInstance().getListIDByName(adsKey), new RewardedCallback() {
                @Override
                public void onAdLoaded(RewardedAd ad) {
                    super.onAdLoaded(ad);
                    listReward.put(adsKey, ad);
                    Log.d(TAG, "onAdLoaded: " + listReward);
                }
            }, remoteKey);
        } else {
            Log.d(TAG, "Reward already loaded. (Reward != null)");
        }
    }

    public static void showRewardAds(Activity activity, String adsKey, String remoteKey, RewardedCallback rewardedCallback, boolean isReloadRewardAfterShow) {
        Admob.getInstance().showReward(activity, listReward.get(adsKey), new RewardedCallback() {
            @Override
            public void onNextAction() {
                super.onNextAction();
                rewardedCallback.onNextAction();
                Log.d(TAG, "onNextAction: " + listReward);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                AppOpenManager.isLastActionClickAd = true;
                rewardedCallback.onAdClicked();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                rewardedCallback.onAdDismissedFullScreenContent();
            }

            @Override
            public void onAdFailedToLoad() {
                super.onAdFailedToLoad();
                rewardedCallback.onAdFailedToLoad();
            }

            @Override
            public void onAdFailedToShowFullScreenContent() {
                super.onAdFailedToShowFullScreenContent();
                rewardedCallback.onAdFailedToShowFullScreenContent();
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                rewardedCallback.onAdImpression();
            }

            @Override
            public void onAdLoaded(RewardedAd ad) {
                super.onAdLoaded(ad);
                rewardedCallback.onAdLoaded(ad);
            }

            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                rewardedCallback.onAdShowedFullScreenContent();
            }

            @Override
            public void onUserEarnedReward() {
                super.onUserEarnedReward();
                rewardedCallback.onUserEarnedReward();
            }
        }, remoteKey);
        listReward.put(adsKey, null);
        if (isReloadRewardAfterShow) {
            loadRewardAds(activity, adsKey, remoteKey);
        }
    }
}
