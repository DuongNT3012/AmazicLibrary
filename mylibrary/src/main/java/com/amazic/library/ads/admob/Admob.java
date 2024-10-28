package com.amazic.library.ads.admob;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.amazic.library.Utils.AdjustUtil;
import com.amazic.library.Utils.NetworkUtil;
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.ads.callback.BannerCallback;
import com.amazic.library.ads.callback.InterCallback;
import com.amazic.library.ads.callback.NativeCallback;
import com.amazic.library.ads.callback.RewardedCallback;
import com.amazic.library.ads.callback.RewardedInterCallback;
import com.amazic.library.ads.collapse_banner_ads.CollapseBannerHelper;
import com.amazic.library.dialog.LoadingAdsDialog;
import com.amazic.library.ump.AdsConsentManager;
import com.amazic.mylibrary.R;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

import java.util.List;
import java.util.Locale;

public class Admob {
    private static Admob INSTANCE;
    private static final String TAG = "Admob";
    private LoadingAdsDialog loadingAdsDialog;
    private boolean isInterOrRewardedShowing = false;
    private boolean isShowAllAds = true;
    private InterstitialAd mInterstitialAdSplash;
    private boolean isFailToShowAdSplash = false;
    private long timeInterval = 0L;
    private long lastTimeDismissInter = 0L;
    private long timeIntervalFromStart = 0L;
    private long timeStart = 0L;
    private String tokenEventAdjust = "";

    public static Admob getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Admob();
        }
        return INSTANCE;
    }

    public void initAdmob(Activity activity, IOnInitAdmobDone iOnInitAdmobDone) {
        timeStart = System.currentTimeMillis();
        AppOpenManager.getInstance().setApplication(activity.getApplication());
        new Thread(() -> {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(activity, initializationStatus -> {
                Log.d(TAG, "initAdmob: " + initializationStatus.getAdapterStatusMap());
                iOnInitAdmobDone.onInitAdmobDone();
            });
        }).start();
    }

    public void setTimeInterval(long timeInterval) {
        this.lastTimeDismissInter = 0L;
        this.timeInterval = timeInterval;
    }

    public void setTimeIntervalFromStart(long timeIntervalFromStart) {
        this.timeIntervalFromStart = timeIntervalFromStart;
    }

    public void setTokenEventAdjust(String tokenEventAdjust) {
        this.tokenEventAdjust = tokenEventAdjust;
    }

    public String getTokenEventAdjust() {
        return this.tokenEventAdjust;
    }

    public boolean isInterOrRewardedShowing() {
        return isInterOrRewardedShowing;
    }

    public void setShowAllAds(boolean isShowAllAds) {
        this.isShowAllAds = isShowAllAds;
    }

    public boolean getShowAllAds() {
        return isShowAllAds;
    }

    //================================Start inter ads================================
    public void loadInterAds(Context context, List<String> listIdInter, InterCallback interCallback) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(context) || listIdInter.size() == 0 || !AdsConsentManager.getConsentResult(context) || !isShowAllAds) {
            interCallback.onNextAction();
            return;
        }
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(context, listIdInter.get(0), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        interCallback.onAdLoaded(interstitialAd);
                        Log.i(TAG, "onAdLoaded");
                        //Tracking revenue
                        interstitialAd.setOnPaidEventListener(adValue -> {
                            //Adjust
                            AdjustUtil.trackRevenue(interstitialAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.d(TAG, loadAdError.toString());
                        interCallback.onAdFailedToLoad();
                        listIdInter.remove(0);
                        loadInterAds(context, listIdInter, interCallback);
                    }
                });
    }

    public void showInterAds(Activity activity, InterstitialAd mInterstitialAd, InterCallback interCallback) {
        if (System.currentTimeMillis() - lastTimeDismissInter < timeInterval) {
            Log.d(TAG, "Not show interstitial because the time interval.");
            interCallback.onNextAction();
            return;
        }
        if (System.currentTimeMillis() - timeStart < timeIntervalFromStart) {
            Log.d(TAG, "Not show interstitial because the time interval from start.");
            interCallback.onNextAction();
            return;
        }
        if (mInterstitialAd == null) {
            Log.d(TAG, "The interstitial ad wasn't ready yet.");
            interCallback.onNextAction();
            return;
        }
        loadingAdsDialog = new LoadingAdsDialog(activity);
        if (!loadingAdsDialog.isShowing()) {
            loadingAdsDialog.show();
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdClicked() {
                    // Called when a click is recorded for an ad.
                    Log.d(TAG, "Ad was clicked.");
                    interCallback.onAdClicked();
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    Log.d(TAG, "Ad dismissed fullscreen content.");
                    interCallback.onAdDismissedFullScreenContent();
                    interCallback.onNextAction();
                    isInterOrRewardedShowing = false;
                    lastTimeDismissInter = System.currentTimeMillis();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    // Called when ad fails to show.
                    Log.e(TAG, "Ad failed to show fullscreen content.");
                    interCallback.onAdFailedToShowFullScreenContent();
                    interCallback.onNextAction();
                    if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        loadingAdsDialog.dismiss();
                    }
                }

                @Override
                public void onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    Log.d(TAG, "Ad recorded an impression.");
                    interCallback.onAdImpression();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    Log.d(TAG, "Ad showed fullscreen content.");
                    interCallback.onAdShowedFullScreenContent();
                    if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        loadingAdsDialog.dismiss();
                    }
                    isInterOrRewardedShowing = true;
                }
            });
            mInterstitialAd.show(activity);
        }, 250);
    }

    public void showInterAdsSplash(Activity activity, InterCallback interCallback) {
        if (mInterstitialAdSplash == null) {
            Log.d(TAG, "The interstitial ad wasn't ready yet.");
            interCallback.onNextAction();
            return;
        }
        loadingAdsDialog = new LoadingAdsDialog(activity);
        if (!loadingAdsDialog.isShowing()) {
            loadingAdsDialog.show();
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            mInterstitialAdSplash.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdClicked() {
                    // Called when a click is recorded for an ad.
                    Log.d(TAG, "Ad was clicked.");
                    interCallback.onAdClicked();
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    Log.d(TAG, "Ad dismissed fullscreen content.");
                    interCallback.onAdDismissedFullScreenContent();
                    interCallback.onNextAction();
                    isInterOrRewardedShowing = false;
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    // Called when ad fails to show.
                    Log.e(TAG, "Ad failed to show fullscreen content.");
                    interCallback.onAdFailedToShowFullScreenContent();
                    //interCallback.onNextAction();
                    if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        loadingAdsDialog.dismiss();
                    }
                    isFailToShowAdSplash = true;
                }

                @Override
                public void onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    Log.d(TAG, "Ad recorded an impression.");
                    interCallback.onAdImpression();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    Log.d(TAG, "Ad showed fullscreen content.");
                    interCallback.onAdShowedFullScreenContent();
                    if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                        loadingAdsDialog.dismiss();
                    }
                    isInterOrRewardedShowing = true;
                    isFailToShowAdSplash = false;
                }
            });
            mInterstitialAdSplash.show(activity);
        }, 250);
    }

    public void loadAndShowInterAdSplash(Activity activity, List<String> listIdInter, InterCallback interCallback) {
        //Check condition
        if (!NetworkUtil.isNetworkActive(activity) || listIdInter.size() == 0 || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds) {
            interCallback.onNextAction();
            return;
        }
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(activity, listIdInter.get(0), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        Log.i(TAG, "onAdLoaded");
                        interCallback.onAdLoaded(interstitialAd);
                        mInterstitialAdSplash = interstitialAd;
                        showInterAdsSplash(activity, interCallback);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.d(TAG, loadAdError.toString());
                        interCallback.onAdFailedToLoad();
                        listIdInter.remove(0);
                        loadAndShowInterAdSplash(activity, listIdInter, interCallback);
                    }
                });
    }

    public void onCheckShowSplashWhenFail(Activity activity, InterCallback interCallback) {
        if (isFailToShowAdSplash) {
            showInterAdsSplash(activity, interCallback);
        }
    }

    //================================end inter ads================================

    //================================Start banner ads================================
    public void loadBannerAds(Activity activity, List<String> listIdBanner, FrameLayout adContainerView, BannerCallback bannerCallback, IOnAdsImpression iOnAdsImpression) {
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check network
        if (!NetworkUtil.isNetworkActive(activity) || listIdBanner.size() == 0 || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds) {
            return;
        }
        //Show loading shimmer
        View shimmerBanner = LayoutInflater.from(activity).inflate(R.layout.layout_shimmer_banner, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerBanner);
        }
        // [START create_ad_view]
        // Create a new ad view.
        AdView adView = new AdView(activity);
        adView.setAdUnitId(listIdBanner.get(0));
        adView.setAdSize(getAdSize(activity));
        // [END create_ad_view]

        // [START load_ad]
        // Start loading the ad in the background.
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                bannerCallback.onAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                bannerCallback.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                bannerCallback.onAdFailedToLoad();
                listIdBanner.remove(0);
                loadBannerAds(activity, listIdBanner, adContainerView, bannerCallback, iOnAdsImpression);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                bannerCallback.onAdImpression();
                //use for auto reload banner after x seconds
                iOnAdsImpression.onAdsImpression();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                // Replace ad container with new ad view.
                if (adContainerView != null) {
                    adContainerView.removeAllViews();
                    adContainerView.addView(adView);
                }
                bannerCallback.onAdLoaded();

                //Tracking revenue
                adView.setOnPaidEventListener(adValue -> {
                    //Adjust
                    if (adView.getResponseInfo() != null) {
                        AdjustUtil.trackRevenue(adView.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                    }
                });
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                bannerCallback.onAdOpened();
            }

            @Override
            public void onAdSwipeGestureClicked() {
                super.onAdSwipeGestureClicked();
                bannerCallback.onAdSwipeGestureClicked();
            }
        });
        // [END load_ad]
    }

    //can load banner ads in fragment
    public void loadBannerAds(Context context, int adWidth, List<String> listIdBanner, FrameLayout adContainerView, BannerCallback bannerCallback, IOnAdsImpression iOnAdsImpression) {
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check network
        if (!NetworkUtil.isNetworkActive(context) || listIdBanner.size() == 0 || !AdsConsentManager.getConsentResult(context) || !isShowAllAds) {
            return;
        }
        //Show loading shimmer
        View shimmerBanner = LayoutInflater.from(context).inflate(R.layout.layout_shimmer_banner, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerBanner);
        }
        // [START create_ad_view]
        // Create a new ad view.
        AdView adView = new AdView(context);
        adView.setAdUnitId(listIdBanner.get(0));
        adView.setAdSize(getAdSizeFragment(context, adWidth));
        // [END create_ad_view]

        // [START load_ad]
        // Start loading the ad in the background.
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                bannerCallback.onAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                bannerCallback.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                bannerCallback.onAdFailedToLoad();
                listIdBanner.remove(0);
                loadBannerAds(context, adWidth, listIdBanner, adContainerView, bannerCallback, iOnAdsImpression);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                bannerCallback.onAdImpression();
                //use for auto reload banner after x seconds
                iOnAdsImpression.onAdsImpression();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                // Replace ad container with new ad view.
                if (adContainerView != null) {
                    adContainerView.removeAllViews();
                    adContainerView.addView(adView);
                }
                bannerCallback.onAdLoaded();
                //Tracking revenue
                adView.setOnPaidEventListener(adValue -> {
                    //Adjust
                    if (adView.getResponseInfo() != null) {
                        AdjustUtil.trackRevenue(adView.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                    }
                });
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                bannerCallback.onAdOpened();
            }

            @Override
            public void onAdSwipeGestureClicked() {
                super.onAdSwipeGestureClicked();
                bannerCallback.onAdSwipeGestureClicked();
            }
        });
        // [END load_ad]
    }
    //end can load banner ads in fragment
    //================================End banner ads================================

    //================================Start collapse banner ads================================
    public AdView loadCollapseBanner(Activity activity, List<String> listIdCollapseBanner, FrameLayout adContainerView, boolean isGravityBottom, BannerCallback bannerCallback, IOnAdsImpression iOnAdsImpression, String collapseTypeClose, long valueCountDownOrCountClick) {
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check network
        if (!NetworkUtil.isNetworkActive(activity) || listIdCollapseBanner.size() == 0 || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds) {
            return null;
        }
        //Show loading shimmer
        View shimmerBanner = LayoutInflater.from(activity).inflate(R.layout.layout_shimmer_banner, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerBanner);
        }
        AdView adView = new AdView(activity);
        adView.setAdUnitId(listIdCollapseBanner.get(0));

        AdSize adSize = getAdSize(activity);
        adView.setAdSize(adSize);
        // Create an extra parameter that aligns the bottom of the expanded ad to
        // the bottom of the bannerView.
        Bundle extras = new Bundle();
        if (isGravityBottom) {
            extras.putString("collapsible", "bottom");
        } else {
            extras.putString("collapsible", "top");
        }

        AdRequest adRequest = new AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                .build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                bannerCallback.onAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                bannerCallback.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                bannerCallback.onAdFailedToLoad();
                listIdCollapseBanner.remove(0);
                loadCollapseBanner(activity, listIdCollapseBanner, adContainerView, isGravityBottom, bannerCallback, iOnAdsImpression, collapseTypeClose, valueCountDownOrCountClick);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                bannerCallback.onAdImpression();
                iOnAdsImpression.onAdsImpression();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                bannerCallback.onAdLoaded();
                // Replace ad container with new ad view.
                if (adContainerView != null) {
                    adContainerView.removeAllViews();
                    adContainerView.addView(adView);
                }
                //Tracking revenue
                adView.setOnPaidEventListener(adValue -> {
                    //Adjust
                    if (adView.getResponseInfo() != null) {
                        AdjustUtil.trackRevenue(adView.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                    }
                });
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                bannerCallback.onAdOpened();
                applyTechForCollapseBanner(collapseTypeClose, valueCountDownOrCountClick);
            }

            @Override
            public void onAdSwipeGestureClicked() {
                super.onAdSwipeGestureClicked();
                bannerCallback.onAdSwipeGestureClicked();
            }
        });
        return adView;
    }

    public AdView loadCollapseBanner(Context context, int adWidth, List<String> listIdCollapseBanner, FrameLayout adContainerView, boolean isGravityBottom, BannerCallback bannerCallback, IOnAdsImpression iOnAdsImpression, String collapseTypeClose, long valueCountDownOrCountClick) {
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check network
        if (!NetworkUtil.isNetworkActive(context) || listIdCollapseBanner.size() == 0 || !AdsConsentManager.getConsentResult(context) || !isShowAllAds) {
            return null;
        }
        //Show loading shimmer
        View shimmerBanner = LayoutInflater.from(context).inflate(R.layout.layout_shimmer_banner, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerBanner);
        }
        AdView adView = new AdView(context);
        adView.setAdUnitId(listIdCollapseBanner.get(0));

        AdSize adSize = getAdSizeFragment(context, adWidth);
        adView.setAdSize(adSize);
        // Create an extra parameter that aligns the bottom of the expanded ad to
        // the bottom of the bannerView.
        Bundle extras = new Bundle();
        if (isGravityBottom) {
            extras.putString("collapsible", "bottom");
        } else {
            extras.putString("collapsible", "top");
        }

        AdRequest adRequest = new AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                .build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                bannerCallback.onAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                bannerCallback.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                bannerCallback.onAdFailedToLoad();
                listIdCollapseBanner.remove(0);
                loadCollapseBanner(context, adWidth, listIdCollapseBanner, adContainerView, isGravityBottom, bannerCallback, iOnAdsImpression, collapseTypeClose, valueCountDownOrCountClick);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                bannerCallback.onAdImpression();
                iOnAdsImpression.onAdsImpression();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                bannerCallback.onAdLoaded();
                // Replace ad container with new ad view.
                if (adContainerView != null) {
                    adContainerView.removeAllViews();
                    adContainerView.addView(adView);
                }
                //Tracking revenue
                adView.setOnPaidEventListener(adValue -> {
                    //Adjust
                    if (adView.getResponseInfo() != null) {
                        AdjustUtil.trackRevenue(adView.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                    }
                });
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                bannerCallback.onAdOpened();
                applyTechForCollapseBanner(collapseTypeClose, valueCountDownOrCountClick);
            }

            @Override
            public void onAdSwipeGestureClicked() {
                super.onAdSwipeGestureClicked();
                bannerCallback.onAdSwipeGestureClicked();
            }
        });
        return adView;
    }

    private void applyTechForCollapseBanner(String collapseTypeClose, long valueCountDownOrCountClick) {
        if (CollapseBannerHelper.getWindowManagerViews() != null) {
            Log.d("TAGvvv", "run: " + CollapseBannerHelper.getWindowManagerViews().size());
        }
        CollapseBannerHelper.listChildViews.clear();
        for (int i = 0; i < CollapseBannerHelper.getWindowManagerViews().size(); i++) {
            Object object = CollapseBannerHelper.getWindowManagerViews().get(i);
            if (object instanceof ViewGroup && ((ViewGroup) object).getClass().getName().contains("android.widget.PopupWindow")) {
                Log.d("CollapseBannerHelper", "ViewGroup: " + object + "\n=================================================================");
                if (collapseTypeClose.equals(CollapseBannerHelper.COUNT_DOWN)) {
                    CollapseBannerHelper.getAllChildViews((ViewGroup) object, collapseTypeClose, valueCountDownOrCountClick, object);
                } else if (collapseTypeClose.equals(CollapseBannerHelper.COUNT_CLICK)) {
                    CollapseBannerHelper.getAllChildViews((ViewGroup) object, collapseTypeClose, valueCountDownOrCountClick, object);
                }
            }
        }
    }

    //================================End collapse banner ads================================

    //Get the ad size with screen width.
    private AdSize getAdSize(Activity activity) {
        DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
        int adWidthPixels = displayMetrics.widthPixels;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
            adWidthPixels = windowMetrics.getBounds().width();
        }

        float density = displayMetrics.density;
        int adWidth = (int) (adWidthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }

    private AdSize getAdSizeFragment(Context context, int adWidth) {
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth);
    }

    //================================Start native ads================================
    public void loadNativeAds(Activity activity, List<String> listIdNative, FrameLayout adContainerView, int layoutNative, int layoutNativeMeta, int layoutShimmerNative, boolean setShowNativeAfterLoaded, NativeCallback nativeCallback, IOnAdsImpression iOnAdsImpression) {
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check network
        if (!NetworkUtil.isNetworkActive(activity) || listIdNative.size() == 0 || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds) {
            return;
        }
        //Show loading shimmer
        View shimmerNative = LayoutInflater.from(activity).inflate(layoutShimmerNative, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerNative);
        }
        AdLoader.Builder builder = new AdLoader.Builder(activity, listIdNative.get(0));
        builder.forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
            // OnLoadedListener implementation.
            @Override
            public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                // If this callback occurs after the activity is destroyed, you must call
                // destroy and return or you may get a memory leak.
                        /*boolean isDestroyed = false;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            isDestroyed = activity.isDestroyed();
                        }
                        if (isDestroyed || activity.isFinishing() || activity.isChangingConfigurations()) {
                            nativeAd.destroy();
                            return;
                        }
                        // You must call destroy on old ads when you are done with them,
                        // otherwise you will have a memory leak.
                        if (nativeAd != null) {
                            nativeAd.destroy();
                        }*/
                nativeCallback.onNativeAdLoaded(nativeAd);
                if (setShowNativeAfterLoaded) {
                    NativeAdView adView;
                    String mediationAdapterClassName = "";
                    if (nativeAd.getResponseInfo() != null) {
                        mediationAdapterClassName = nativeAd.getResponseInfo().getMediationAdapterClassName();
                    }
                    if (mediationAdapterClassName != null && mediationAdapterClassName.toLowerCase().contains("facebook")) {
                        adView = (NativeAdView) activity.getLayoutInflater().inflate(layoutNativeMeta, adContainerView, false);
                    } else {
                        adView = (NativeAdView) activity.getLayoutInflater().inflate(layoutNative, adContainerView, false);
                    }
                    Admob.getInstance().populateNativeAdView(nativeAd, adView);
                    if (adContainerView != null) {
                        adContainerView.removeAllViews();
                        adContainerView.addView(adView);
                    }
                }
                iOnAdsImpression.onAdsImpression();
                //Tracking revenue
                nativeAd.setOnPaidEventListener(adValue -> {
                    //Adjust
                    if (nativeAd.getResponseInfo() != null) {
                        AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                    }
                });
            }
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                String error = String.format(Locale.getDefault(), "domain: %s, code: %d, message: %s", loadAdError.getDomain(), loadAdError.getCode(), loadAdError.getMessage());
                Log.d(TAG, "Failed to load native ad with error " + error);
                nativeCallback.onAdFailedToLoad();
                listIdNative.remove(0);
                loadNativeAds(activity, listIdNative, adContainerView, layoutNative, layoutNativeMeta, layoutShimmerNative, setShowNativeAfterLoaded, nativeCallback, iOnAdsImpression);
            }
        }).build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public void loadNativeAds(Activity activity, List<String> listIdNative, FrameLayout adContainerView, int layoutNative, int layoutNativeMeta, int layoutShimmerNative, boolean setShowNativeAfterLoaded, NativeCallback nativeCallback) {
        if (adContainerView != null) {
            adContainerView.removeAllViews();
        }
        //Check network
        if (!NetworkUtil.isNetworkActive(activity) || listIdNative.size() == 0 || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds) {
            return;
        }
        //Show loading shimmer
        View shimmerNative = LayoutInflater.from(activity).inflate(layoutShimmerNative, null);
        if (adContainerView != null) {
            adContainerView.addView(shimmerNative);
        }
        AdLoader.Builder builder = new AdLoader.Builder(activity, listIdNative.get(0));
        builder.forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
            // OnLoadedListener implementation.
            @Override
            public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                // If this callback occurs after the activity is destroyed, you must call
                // destroy and return or you may get a memory leak.
                        /*boolean isDestroyed = false;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            isDestroyed = activity.isDestroyed();
                        }
                        if (isDestroyed || activity.isFinishing() || activity.isChangingConfigurations()) {
                            nativeAd.destroy();
                            return;
                        }
                        // You must call destroy on old ads when you are done with them,
                        // otherwise you will have a memory leak.
                        if (nativeAd != null) {
                            nativeAd.destroy();
                        }*/
                nativeCallback.onNativeAdLoaded(nativeAd);
                if (setShowNativeAfterLoaded) {
                    NativeAdView adView;
                    String mediationAdapterClassName = "";
                    if (nativeAd.getResponseInfo() != null) {
                        mediationAdapterClassName = nativeAd.getResponseInfo().getMediationAdapterClassName();
                    }
                    if (mediationAdapterClassName != null && mediationAdapterClassName.toLowerCase().contains("facebook")) {
                        adView = (NativeAdView) activity.getLayoutInflater().inflate(layoutNativeMeta, adContainerView, false);
                    } else {
                        adView = (NativeAdView) activity.getLayoutInflater().inflate(layoutNative, adContainerView, false);
                    }
                    Admob.getInstance().populateNativeAdView(nativeAd, adView);
                    if (adContainerView != null) {
                        adContainerView.removeAllViews();
                        adContainerView.addView(adView);
                    }
                }
                //Tracking revenue
                nativeAd.setOnPaidEventListener(adValue -> {
                    //Adjust
                    if (nativeAd.getResponseInfo() != null) {
                        AdjustUtil.trackRevenue(nativeAd.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                    }
                });
            }
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                String error = String.format(Locale.getDefault(), "domain: %s, code: %d, message: %s", loadAdError.getDomain(), loadAdError.getCode(), loadAdError.getMessage());
                Log.d(TAG, "Failed to load native ad with error " + error);
                nativeCallback.onAdFailedToLoad();
                listIdNative.remove(0);
                loadNativeAds(activity, listIdNative, adContainerView, layoutNative, layoutNativeMeta, layoutShimmerNative, setShowNativeAfterLoaded, nativeCallback);
            }
        }).build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        // Set the media view.
        MediaView mediaView = (MediaView) adView.findViewById(R.id.ad_media);
        if (mediaView != null) {
            adView.setMediaView(mediaView);
        }

        // Set other ad assets.
        View viewHeadline = adView.findViewById(R.id.ad_headline);
        if (viewHeadline != null) {
            adView.setHeadlineView(viewHeadline);
        }
        View bodyView = adView.findViewById(R.id.ad_body);
        if (bodyView != null) {
            adView.setBodyView(bodyView);
        }
        View callToActionView = adView.findViewById(R.id.ad_call_to_action);
        if (callToActionView != null) {
            adView.setCallToActionView(callToActionView);
        }
        View iconView = adView.findViewById(R.id.ad_app_icon);
        if (iconView != null) {
            adView.setIconView(iconView);
        }
        View priceView = adView.findViewById(R.id.ad_price);
        if (priceView != null) {
            adView.setPriceView(priceView);
        }
        View starRatingView = adView.findViewById(R.id.ad_stars);
        if (starRatingView != null) {
            adView.setStarRatingView(starRatingView);
        }
        View storeView = adView.findViewById(R.id.ad_store);
        if (storeView != null) {
            adView.setStoreView(storeView);
        }
        View advertiserView = adView.findViewById(R.id.ad_advertiser);
        if (advertiserView != null) {
            adView.setAdvertiserView(advertiserView);
        }

        // The headline and mediaContent are guaranteed to be in every NativeAd.
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        adView.getMediaView().setMediaContent(nativeAd.getMediaContent());

        // These assets aren't guaranteed to be in every NativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.getBody() == null) {
            adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            adView.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            adView.getCallToActionView().setVisibility(View.INVISIBLE);
        } else {
            adView.getCallToActionView().setVisibility(View.VISIBLE);
            ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getPrice() == null) {
            adView.getPriceView().setVisibility(View.INVISIBLE);
        } else {
            adView.getPriceView().setVisibility(View.VISIBLE);
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        }

        if (nativeAd.getStore() == null) {
            adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            adView.getStoreView().setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null) {
            adView.getStarRatingView().setVisibility(View.INVISIBLE);
        } else {
            ((RatingBar) adView.getStarRatingView()).setRating(nativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.INVISIBLE);
        } else {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd);

        // Get the video controller for the ad. One will always be provided, even if the ad doesn't
        // have a video asset.
        VideoController vc = nativeAd.getMediaContent().getVideoController();

        // Updates the UI to say whether or not this ad has a video asset.
        if (nativeAd.getMediaContent() != null && nativeAd.getMediaContent().hasVideoContent()) {

            // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
            // VideoController will call methods on this object when events occur in the video
            // lifecycle.
            vc.setVideoLifecycleCallbacks(
                    new VideoController.VideoLifecycleCallbacks() {
                        @Override
                        public void onVideoEnd() {
                            // Publishers should allow native ads to complete video playback before
                            // refreshing or replacing them with another ad in the same UI location.
                            Log.d(TAG, "Video status: Video playback has ended.");
                            super.onVideoEnd();
                        }
                    });
        } else {
            Log.d(TAG, "Video status: Ad does not contain a video asset.");
        }
    }
    //================================End native ads================================

    //================================Start reward ads================================
    public void loadRewardAds(Activity activity, List<String> listIdRewarded, RewardedCallback rewardedCallback) {
        //Check network
        if (!NetworkUtil.isNetworkActive(activity) || listIdRewarded.size() == 0 || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds) {
            return;
        }
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(activity, listIdRewarded.get(0),
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error.
                        Log.d(TAG, loadAdError.toString());
                        rewardedCallback.onAdFailedToLoad();
                        listIdRewarded.remove(0);
                        loadRewardAds(activity, listIdRewarded, rewardedCallback);
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        Log.d(TAG, "Ad was loaded.");
                        rewardedCallback.onAdLoaded(ad);
                        //Tracking revenue
                        ad.setOnPaidEventListener(adValue -> {
                            //Adjust
                            ad.getResponseInfo();
                            AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                        });
                    }
                });
    }

    public void showReward(Activity activity, RewardedAd rewardedAd, RewardedCallback rewardedCallback) {
        if (rewardedAd == null) {
            Log.d(TAG, "The rewarded ad wasn't ready yet.");
            return;
        }
        loadingAdsDialog = new LoadingAdsDialog(activity);
        if (!loadingAdsDialog.isShowing()) {
            loadingAdsDialog.show();
        }
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(TAG, "Ad was clicked.");
                rewardedCallback.onAdClicked();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Set the ad reference to null so you don't show the ad a second time.
                Log.d(TAG, "Ad dismissed fullscreen content.");
                rewardedCallback.onAdDismissedFullScreenContent();
                isInterOrRewardedShowing = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show fullscreen content.");
                rewardedCallback.onAdFailedToShowFullScreenContent();
                if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    loadingAdsDialog.dismiss();
                }
            }

            @Override
            public void onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(TAG, "Ad recorded an impression.");
                rewardedCallback.onAdImpression();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "Ad showed fullscreen content.");
                rewardedCallback.onAdShowedFullScreenContent();
                if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    loadingAdsDialog.dismiss();
                }
                isInterOrRewardedShowing = true;
            }
        });
        rewardedAd.show(activity, new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                // Handle the reward.
                Log.d(TAG, "The user earned the reward.");
                int rewardAmount = rewardItem.getAmount();
                String rewardType = rewardItem.getType();
                rewardedCallback.onUserEarnedReward();
            }
        });
    }
    //================================End reward ads================================

    //================================Start reward inter================================
    public void loadRewardInterAds(Activity activity, List<String> listIdRewardedInter, RewardedInterCallback rewardedInterCallback) {
        //Check network
        if (!NetworkUtil.isNetworkActive(activity) || listIdRewardedInter.size() == 0 || !AdsConsentManager.getConsentResult(activity) || !isShowAllAds) {
            return;
        }
        RewardedInterstitialAd.load(activity, listIdRewardedInter.get(0),
                new AdRequest.Builder().build(), new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedInterstitialAd ad) {
                        Log.d(TAG, "Ad was loaded.");
                        rewardedInterCallback.onAdLoaded(ad);
                        //Tracking revenue
                        ad.setOnPaidEventListener(adValue -> {
                            //Adjust
                            ad.getResponseInfo();
                            AdjustUtil.trackRevenue(ad.getResponseInfo().getLoadedAdapterResponseInfo(), adValue);
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.d(TAG, loadAdError.toString());
                        rewardedInterCallback.onAdFailedToLoad();
                        listIdRewardedInter.remove(0);
                        loadRewardInterAds(activity, listIdRewardedInter, rewardedInterCallback);
                    }
                });
    }

    public void showRewardInterAds(Activity activity, RewardedInterstitialAd rewardedInterstitialAd, RewardedInterCallback rewardedInterCallback) {
        if (rewardedInterstitialAd == null) {
            Log.d(TAG, "The rewarded inter ad wasn't ready yet.");
            return;
        }
        loadingAdsDialog = new LoadingAdsDialog(activity);
        if (!loadingAdsDialog.isShowing()) {
            loadingAdsDialog.show();
        }
        rewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(TAG, "Ad was clicked.");
                rewardedInterCallback.onAdClicked();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Set the ad reference to null so you don't show the ad a second time.
                Log.d(TAG, "Ad dismissed fullscreen content.");
                rewardedInterCallback.onAdDismissedFullScreenContent();
                isInterOrRewardedShowing = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show fullscreen content.");
                rewardedInterCallback.onAdFailedToShowFullScreenContent();
                if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    loadingAdsDialog.dismiss();
                }
            }

            @Override
            public void onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(TAG, "Ad recorded an impression.");
                rewardedInterCallback.onAdImpression();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "Ad showed fullscreen content.");
                rewardedInterCallback.onAdShowedFullScreenContent();
                if (loadingAdsDialog != null && loadingAdsDialog.isShowing()) {
                    loadingAdsDialog.dismiss();
                }
                isInterOrRewardedShowing = true;
            }
        });
        rewardedInterstitialAd.show(activity, new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                rewardedInterCallback.onUserEarnedReward();
            }
        });
    }
    //================================End reward inter================================
}
