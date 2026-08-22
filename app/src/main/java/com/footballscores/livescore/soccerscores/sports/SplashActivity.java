package com.footballscores.livescore.soccerscores.sports;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwnerKt;

import com.amazic.library.Utils.IDRemoteConfigHelper;
import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.callback.AppOpenCallback;
import com.amazic.library.ads.callback.InterCallback;
import com.amazic.library.ads.native_ads.NativeAfterInterManager;
import com.amazic.library.ads.splash_ads.AdmobAdsConfig;
import com.amazic.library.ads.splash_ads.AdsSplash;
import com.amazic.library.ads.splash_ads.AsyncSplash;
import com.amazic.library.update_app.UpdateApplicationManager;
import com.footballscores.livescore.soccerscores.sports.databinding.ActivitySplashBinding;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.InstallStatus;

import java.util.ArrayList;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;
    private InterCallback interCallback;
    private AppOpenCallback appOpenCallback;
    private String jsonIdAdsDefault =
            "[\n" +
            "  {\n" +
            "    \"id\": 4462,\n" +
            "    \"package_name\": \"com.footballscores.livescore.soccerscores\",\n" +
            "    \"app name\": \"EMF & Metal Detector\",\n" +
            "    \"app_id\": \"ca-app-pub-6485839283816071~1724815634\",\n" +
            "    \"name\": \"id_native_wb_2\",\n" +
            "    \"ads_id\": \"ca-app-pub-3940256099942544/2247696110\"\n" +
            "  }," +
            "  {\n" +
            "    \"id\": 4462,\n" +
            "    \"package_name\": \"com.footballscores.livescore.soccerscores\",\n" +
            "    \"app name\": \"EMF & Metal Detector\",\n" +
            "    \"app_id\": \"ca-app-pub-6485839283816071~1724815634\",\n" +
            "    \"name\": \"inter_all\",\n" +
            "    \"ads_id\": \"ca-app-pub-3940256099942544/1033173712\"\n" +
            "  }" +
            "]";

    private static final String ID_INTER= "ca-app-pub-3940256099942544/1033173712";
    public static AppUpdateManager appUpdateManager;
    public static InstallStateUpdatedListener installStateUpdatedListener;
    private boolean isHandleAsyncSplash = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        interCallback = new InterCallback() {
            @Override
            public void onNextAction() {
                super.onNextAction();
                startNextAct();
            }
        };

        appOpenCallback = new AppOpenCallback() {
            @Override
            public void onNextAction() {
                super.onNextAction();
                startNextAct();
            }
        };
        //User must update to the newest version to use the app
        UpdateApplicationManager.getInstance().init(this, new UpdateApplicationManager.IonUpdateApplication() {
            @Override
            public void onUpdateApplicationFail() {
                handleAsyncSplashJustOnce();
                Toast.makeText(SplashActivity.this, "Update Application Fail", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUpdateApplicationSuccess() {
                Toast.makeText(SplashActivity.this, "Update Application Success", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMustNotUpdateApplication() {
                handleAsyncSplashJustOnce();
            }

            @Override
            public void requestUpdateFail() {
                handleAsyncSplashJustOnce();
            }
        });
        /*RemoteConfigHelper.getInstance().fetchAllKeysAndTypes(SplashActivity.this, () -> {
            Admob.getInstance().setShowAllAds(RemoteConfigHelper.getInstance().get_config(SplashActivity.this, RemoteConfigHelper.show_all_ads));
            Admob.getInstance().setTimeInterval(RemoteConfigHelper.getInstance().get_config_long(SplashActivity.this, RemoteConfigHelper.interval_between_interstitial) * 1000);
            Admob.getInstance().setTimeIntervalFromStart(RemoteConfigHelper.getInstance().get_config_long(SplashActivity.this, RemoteConfigHelper.interval_interstitial_from_start) * 1000);
            AsyncSplash.Companion.getInstance().setUseAppUpdateManager(true); // Do not recall remote config
            if (RemoteConfigHelper.getInstance().get_config(SplashActivity.this, "force_update_version")) {
                //UpdateApplicationManager.getInstance().setUseImmediateUpdate();
                UpdateApplicationManager.getInstance().setUseFlexibleUpdate();
                appUpdateManager = UpdateApplicationManager.getInstance().checkVersionPlayStore(
                        SplashActivity.this,
                        true,
                        false,
                        "\uD83D\uDE80 New Update Available!",
                        "Upgrade now for a smoother experience, bug fixes for better performance. ⚡",
                        "Update Now",
                        "No"
                );
            } else {
                handleAsync();
            }
        });*/
        UpdateApplicationManager.getInstance().setUseFlexibleUpdate();
        appUpdateManager = UpdateApplicationManager.getInstance().checkVersionPlayStore(
                SplashActivity.this,
                true,
                false,
                "\uD83D\uDE80 New Update Available!",
                "Upgrade now for a smoother experience, bug fixes for better performance. ⚡",
                "Update Now",
                "No"
        );
        installStateUpdatedListener = installState -> {
            if (installState.installStatus() == InstallStatus.DOWNLOADING ||
                    installState.installStatus() == InstallStatus.FAILED ||
                    installState.installStatus() == InstallStatus.CANCELED ||
                    installState.installStatus() == InstallStatus.UNKNOWN
            ) {
                handleAsyncSplashJustOnce();
            } else if (installState.installStatus() == InstallStatus.DOWNLOADED) {
                Toast.makeText(getApplicationContext(), getString(R.string.updated_and_ready_welcome_back), Toast.LENGTH_SHORT).show();
                appUpdateManager.completeUpdate();
            }
        };
    }

    private void handleAsyncSplashJustOnce() {
        if (!isHandleAsyncSplash) {
            AsyncSplash.Companion.getInstance().init(this,  interCallback, "c193nrau3dhc", "", jsonIdAdsDefault);
            //AsyncSplash.Companion.getInstance().setUseTechManager(); //case use TechManager Organic
            AdmobAdsConfig.getInstance().setUseDetectTestAd(); //case use DetectTestAd
            //AsyncSplash.Companion.getInstance().setUseIdAdsFromRemoteConfig(true, "id_ads");
            AdmobAdsConfig.getInstance().setDebug(false); //use for TechManager, DetectTestAd
            IDRemoteConfigHelper.isUsingIdDebug = false;
//            AsyncSplash.Companion.getInstance().setLoadAndShowIdInterAdSplashAsync();
            AdmobAdsConfig.getInstance().setPreloadResumeAds(false);
            AdmobAdsConfig.getInstance().setUseAdPreloading(true);
            AdmobAdsConfig.getInstance().setAppPkg(getPackageName());
            AdmobAdsConfig.getInstance().setTimeOutInitAdmob(20_000);
            AdmobAdsConfig.getInstance().setNumberPreloadingSplash(5);
//            AsyncSplash.Companion.getInstance().setAsyncSplashAds();
            //AsyncSplash.Companion.getInstance().setLoopAdsSplash(true);
//            AsyncSplash.Companion.getInstance().setTimeOutSplash(12000);
            AdmobAdsConfig.getInstance().setTimeOutSplash(90_000);
//            AsyncSplash.Companion.getInstance().setUseIdAdsFromRemoteConfig("id_ads");
            //AsyncSplash.Companion.getInstance().setTimeOutCallIdRemoteConfig(5000);
//            ArrayList<ProductDetailCustom> listIAP = new ArrayList<>();
//            listIAP.add(new ProductDetailCustom(IAPManager.PRODUCT_ID_TEST, IAPManager.typeSub));
//            AsyncSplash.Companion.getInstance().setUseBilling(listIAP); //if app use IAP
            AdmobAdsConfig.getInstance().setInitResumeAdsNormal(); //init resume ads without welcome back
//            AsyncSplash.Companion.getInstance().setInitWelcomeBackBelowResumeAds(WelcomeBackActivity.class); //init resume ads with welcome back above
//            AsyncSplash.Companion.getInstance().setInitWelcomeBackBelowResumeAds(WelcomeBackActivity.class); //init resume ads with welcome back below
            ArrayList<String> listTurnOffRemote = new ArrayList<>();
            //listTurnOffRemote.add("native_wb");
            AsyncSplash.Companion.getInstance().setListTurnOffRemoteKeys(listTurnOffRemote); //set list off remote of TechManager
            ArrayList<String> listIdBannerSplash = new ArrayList<>();
            listIdBannerSplash.add("ca-app-pub-3940256099942544/6300978111");
            AdmobAdsConfig.getInstance().setKeyAdsOpenSplash("open_splash");
            AdmobAdsConfig.getInstance().setKeyAdsOpenResume("open_splash");
            Admob.getInstance().setOpenActivityAfterShowInterAds(false);
            AdmobAdsConfig.getInstance().setKeyIntervalBetweenInterstitial("interval_between_interstitial");
            AdmobAdsConfig.getInstance().setKeyIntervalInterstitialFromStart("interval_interstitial_from_start");
            AsyncSplash.Companion.getInstance().setShowBannerSplash(binding.bannerContainerView, listIdBannerSplash, "banner_splash");
            AsyncSplash.Companion.getInstance().setOnPrepareLoadInterOpenSplashAds(new Function0<Unit>() {
                @Override
                public Unit invoke() { //prepare load and show inter/open splash
                    //RemoteConfigHelper.getInstance().set_config(SplashActivity.this, "inter_splash", false);
                    //RemoteConfigHelper.getInstance().set_config(SplashActivity.this, "open_splash", false);
                    return null;
                }
            });
            /// use Native full change inter, open
//            AsyncSplash.Companion.getInstance().setUseNativeFullSplash(true);
//            AsyncSplash.Companion.getInstance().setNumberNativeFullShowSplash(3);
//            AsyncSplash.Companion.getInstance().setKeyNativeAfterInterSplash("native_after_inter");
            /// end

            /// use native meta test
            AdmobAdsConfig.getInstance().setUseNativeSplashMeta(true);
            AdmobAdsConfig.getInstance().setNumberNativeFullShowSplash(3);
            AdmobAdsConfig.getInstance().setKeyNativeFullMetaSplash("native_after_inter");
//            AsyncSplash.Companion.getInstance().setIdNativeMetaSplash("1439001763964762_1710436723487930");
            AdmobAdsConfig.getInstance().setKeyNativeFullAdmobSplash("native_after_inter");
            AdmobAdsConfig.getInstance().setUseNativeFullSplashAdmobWhenMetaFail(true);
            ///end

//            AsyncSplash.Companion.getInstance().setUseAdPreloading(true);
            /// show native after inter count
//            AsyncSplash.Companion.getInstance().setNumberNativeAfterInterSplash(3);
//            AsyncSplash.Companion.getInstance().setShowNativeAfterInter(true);
            /// end

            /// use cache id, config
//            AsyncSplash.Companion.getInstance().setUseCacheDataCallSplash(true);
            /// end

            /// change open, inter splash -> native full screen
//            AsyncSplash.Companion.getInstance().setUseNativeFullSplash(true);
//            AsyncSplash.Companion.getInstance().setNumberNativeAfterInterSplash(3);
            /// end


            AdmobAdsConfig.getInstance().setKeyAdsInterSplash("inter_all");
            AsyncSplash.Companion.getInstance().handleAsync(this,
                    LifecycleOwnerKt.getLifecycleScope(this), new Function0<Unit>() {
                        @Override
                        public Unit invoke() { //no internet
                            interCallback.onNextAction();
                            return null;
                        }
                    }, new Function0<Unit>() { //async splash done
                        @Override
                        public Unit invoke() {
                    /*ArrayList<Integer> listAnim = new ArrayList<>();
                    listAnim.add(R.raw.custom_loading);
                    Admob.getInstance().setCustomAnimationDialog(listAnim);
                    AppOpenManager.getInstance().setCustomAnimationDialog(listAnim);*/
                            NativeAfterInterManager.preloadNativeAfterInter(SplashActivity.this, "native_all", "native_after_inter");
//                            InterManager.loadInterAdPreload(SplashActivity.this, "inter_all", "inter_all");

                            return null;
                        }
                    });
            isHandleAsyncSplash = true;
        }
    }

    private void startNextAct() {
        Log.d("SplashActivity", "startNextAct. " + AdmobApi.getInstance().getListIDByName("resume_wb").size());
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        AdsSplash.getInstance().cancelPreload(ID_INTER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AsyncSplash.Companion.getInstance().checkShowSplashWhenFail(this);
        appUpdateManager.registerListener(installStateUpdatedListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        appUpdateManager.unregisterListener(installStateUpdatedListener);
    }
}
