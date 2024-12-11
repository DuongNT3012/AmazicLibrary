package com.amazic.library.ads.reward_inter_ads;

import android.app.Activity;
import android.util.Log;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.callback.RewardedInterCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;

import java.util.HashMap;
import java.util.Map;

public class RewardInterManager {
    private static final String TAG = "RewardInterManager";
    private static final Map<String, RewardedInterstitialAd> listRewardInter = new HashMap<>();

    public static void loadRewardInterAds(Activity activity, String adsKey) {
        if (listRewardInter.get(adsKey) == null) {
            Admob.getInstance().loadRewardInterAds(activity, AdmobApi.getInstance().getListIDByName(adsKey), new RewardedInterCallback() {
                @Override
                public void onAdLoaded(RewardedInterstitialAd ad) {
                    super.onAdLoaded(ad);
                    listRewardInter.put(adsKey, ad);
                    Log.d(TAG, "onAdLoaded: " + listRewardInter);
                }
            }, adsKey);
        } else {
            Log.d(TAG, "Reward Inter already loaded. (Reward Inter != null)");
        }
    }

    public static void showRewardInterAds(Activity activity, String adsKey, RewardedInterCallback rewardedInterCallback, boolean isReloadRewardAfterShow) {
        Admob.getInstance().showRewardInterAds(activity, listRewardInter.get(adsKey), new RewardedInterCallback() {
            @Override
            public void onNextAction() {
                super.onNextAction();
                rewardedInterCallback.onNextAction();
                listRewardInter.put(adsKey, null);
                if (isReloadRewardAfterShow) {
                    loadRewardInterAds(activity, adsKey);
                }
                Log.d(TAG, "onNextAction: " + listRewardInter);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                rewardedInterCallback.onAdClicked();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                rewardedInterCallback.onAdDismissedFullScreenContent();
            }

            @Override
            public void onAdFailedToLoad() {
                super.onAdFailedToLoad();
                rewardedInterCallback.onAdFailedToLoad();
            }

            @Override
            public void onAdFailedToShowFullScreenContent() {
                super.onAdFailedToShowFullScreenContent();
                rewardedInterCallback.onAdFailedToShowFullScreenContent();
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                rewardedInterCallback.onAdImpression();
            }

            @Override
            public void onAdLoaded(RewardedInterstitialAd ad) {
                super.onAdLoaded(ad);
                rewardedInterCallback.onAdLoaded(ad);
            }

            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                rewardedInterCallback.onAdShowedFullScreenContent();
            }

            @Override
            public void onUserEarnedReward() {
                super.onUserEarnedReward();
                rewardedInterCallback.onUserEarnedReward();
            }
        }, adsKey);
    }
}
