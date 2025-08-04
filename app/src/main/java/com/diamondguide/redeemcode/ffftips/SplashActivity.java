package com.diamondguide.redeemcode.ffftips;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwnerKt;

import com.amazic.library.Utils.RemoteConfigHelper;
import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.ads.callback.AppOpenCallback;
import com.amazic.library.ads.callback.InterCallback;
import com.amazic.library.ads.splash_ads.AsyncSplash;
/*import com.amazic.library.iap.IAPManager;
import com.amazic.library.iap.ProductDetailCustom;*/
import com.amazic.library.update_app.UpdateApplicationManager;
import com.diamondguide.redeemcode.ffftips.databinding.ActivitySplashBinding;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.InstallStatus;

import java.util.ArrayList;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;
    private InterCallback interCallback;
    private AppOpenCallback appOpenCallback;
    private String jsonIdAdsDefault = "[{\"id\":14,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"inter_splash\",\"ads_id\":\"ca-app-pub-3940256099942544\\/3419835294\"},{\"id\":15,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"policy_inter_splash\",\"ads_id\":\"ca-app-pub-3940256099942544\\/3419835294\"},{\"id\":16,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"policy_inter_theme\",\"ads_id\":\"ca-app-pub-3940256099942544\\/1033173712\"},{\"id\":17,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"banner_all\",\"ads_id\":\"ca-app-pub-3940256099942544\\/6300978111\"},{\"id\":18,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"open_splash\",\"ads_id\":\"ca-app-pub-3940256099942544\\/9257395921\"},{\"id\":19,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"inter_all\",\"ads_id\":\"ca-app-pub-3940256099942544\\/1033173712\"},{\"id\":20,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"policy_open_splash\",\"ads_id\":\"ca-app-pub-3940256099942544\\/3419835294\"},{\"id\":21,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"inter_intro\",\"ads_id\":\"ca-app-pub-3940256099942544\\/1033173712\"},{\"id\":91,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"collapse_banner\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2014213617\"},{\"id\":2326,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_preview\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2327,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_theme\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2425,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_emi\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2426,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_result\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2427,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_welcome\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2428,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_success\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2435,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"rewarded_animation\",\"ads_id\":\"ca-app-pub-3940256099942544\\/5224354917\"},{\"id\":2436,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_preview\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2437,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_apply\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2438,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_ringtone\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2439,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_gallery\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2440,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"inter_info\",\"ads_id\":\"ca-app-pub-3940256099942544\\/1033173712\"},{\"id\":2441,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_home\",\"ads_id\":\"11\"},{\"id\":2442,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_home\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2443,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_welcome\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2448,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_guide\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2449,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_configuration\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2450,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_merge_audio\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2451,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_merge_video\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2452,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_cutter\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2453,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_process\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2454,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"inter_splash\",\"ads_id\":\"ca-app-pub-3940256099942544\\/1033173712\"},{\"id\":2455,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"inter_choose\",\"ads_id\":\"ca-app-pub-3940256099942544\\/1033173712\"},{\"id\":2456,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_item\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2465,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_detail\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2466,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_file\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2469,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_intro\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2470,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_language\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2471,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"inter_guide\",\"ads_id\":\"ca-app-pub-3940256099942544\\/1033173712\"},{\"id\":2472,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_per\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2473,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"appopen_resume\",\"ads_id\":\"ca-app-pub-3940256099942544\\/9257395921\"},{\"id\":2474,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_stop_watch\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2475,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_timer\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2476,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_history\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2477,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"inter_welcome_back\",\"ads_id\":\"ca-app-pub-3940256099942544\\/1033173712\"},{\"id\":2478,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_crop\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2479,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"banner\",\"ads_id\":\"ca-app-pub-3940256099942544\\/6300978111\"}]";
    public static AppUpdateManager appUpdateManager;
    public static InstallStateUpdatedListener installStateUpdatedListener;
    private boolean isHandleAsyncSplash = false;

    /*[{"id":14,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"inter_splash","ads_id":"ca-app-pub-3940256099942544\/3419835294"},{"id":15,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"policy_inter_splash","ads_id":"ca-app-pub-3940256099942544\/3419835294"},{"id":16,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"policy_inter_theme","ads_id":"ca-app-pub-3940256099942544\/1033173712"},{"id":17,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"banner_all","ads_id":"ca-app-pub-3940256099942544\/6300978111"},{"id":18,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"open_splash","ads_id":"ca-app-pub-3940256099942544\/9257395921"},{"id":19,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"inter_all","ads_id":"ca-app-pub-3940256099942544\/1033173712"},{"id":20,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"policy_open_splash","ads_id":"ca-app-pub-3940256099942544\/3419835294"},{"id":21,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"inter_intro","ads_id":"ca-app-pub-3940256099942544\/1033173712"},{"id":91,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"collapse_banner","ads_id":"ca-app-pub-3940256099942544\/2014213617"},{"id":2326,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_preview","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2327,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_theme","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2425,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_emi","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2426,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_result","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2427,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_welcome","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2428,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_success","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2435,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"rewarded_animation","ads_id":"ca-app-pub-3940256099942544\/5224354917"},{"id":2436,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_preview","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2437,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_apply","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2438,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_ringtone","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2439,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_gallery","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2440,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"inter_info","ads_id":"ca-app-pub-3940256099942544\/1033173712"},{"id":2441,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_home","ads_id":"11"},{"id":2442,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_home","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2443,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_welcome","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2448,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_guide","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2449,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_configuration","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2450,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_merge_audio","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2451,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_merge_video","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2452,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_cutter","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2453,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_process","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2454,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"inter_splash","ads_id":"ca-app-pub-3940256099942544\/1033173712"},{"id":2455,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"inter_choose","ads_id":"ca-app-pub-3940256099942544\/1033173712"},{"id":2456,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_item","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2465,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_detail","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2466,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_file","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2469,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_intro","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2470,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_language","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2471,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"inter_guide","ads_id":"ca-app-pub-3940256099942544\/1033173712"},{"id":2472,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_per","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2473,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"appopen_resume","ads_id":"ca-app-pub-3940256099942544\/9257395921"},{"id":2474,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_stop_watch","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2475,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_timer","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2476,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_history","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2477,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"inter_welcome_back","ads_id":"ca-app-pub-3940256099942544\/1033173712"},{"id":2478,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"native_crop","ads_id":"ca-app-pub-3940256099942544\/2247696110"},{"id":2479,"package_name":null,"app name":"Api test","app_id":"ca-app-pub-4973559944609228~2346710863","name":"banner","ads_id":"ca-app-pub-3940256099942544\/6300978111"}]*/

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
            AsyncSplash.Companion.getInstance().init(this, appOpenCallback, interCallback, "c193nrau3dhc", "", "", jsonIdAdsDefault);
            //AsyncSplash.Companion.getInstance().setUseTechManager(); //case use TechManager Organic
            AsyncSplash.Companion.getInstance().setUseDetectTestAd(); //case use DetectTestAd
            //AsyncSplash.Companion.getInstance().setUseIdAdsFromRemoteConfig(true, "id_ads");
            AsyncSplash.Companion.getInstance().setDebug(true); //use for TechManager, DetectTestAd
            AsyncSplash.Companion.getInstance().setLoadAndShowIdInterAdSplashAsync();
            AsyncSplash.Companion.getInstance().setPreloadResumeAds(false);
            AsyncSplash.Companion.getInstance().setAsyncSplashAds();
            //AsyncSplash.Companion.getInstance().setLoopAdsSplash(true);
            AsyncSplash.Companion.getInstance().setTimeOutSplash(12000);
            AsyncSplash.Companion.getInstance().setTimeOutCallApi(0);
//            ArrayList<ProductDetailCustom> listIAP = new ArrayList<>();
//            listIAP.add(new ProductDetailCustom(IAPManager.PRODUCT_ID_TEST, IAPManager.typeSub));
//            AsyncSplash.Companion.getInstance().setUseBilling(listIAP); //if app use IAP
            //AsyncSplash.Companion.getInstance().setInitResumeAdsNormal(); //init resume ads without welcome back
            AsyncSplash.Companion.getInstance().setInitWelcomeBackAboveResumeAds(WelcomeBackActivity.class); //init resume ads with welcome back above
            //AsyncSplash.Companion.getInstance().setInitWelcomeBackBelowResumeAds(WelcomeBackActivity.class); //init resume ads with welcome back below
            ArrayList<String> listTurnOffRemote = new ArrayList<>();
            listTurnOffRemote.add("native_wb");
            AsyncSplash.Companion.getInstance().setListTurnOffRemoteKeys(listTurnOffRemote); //set list off remote of TechManager
            ArrayList<String> listIdBannerSplash = new ArrayList<>();
            listIdBannerSplash.add("ca-app-pub-3940256099942544/6300978111");
            AsyncSplash.Companion.getInstance().setKeyAdsInterSplash("inter_splash");
            AsyncSplash.Companion.getInstance().setKeyAdsOpenSplash("open_splash");
            AsyncSplash.Companion.getInstance().setShowBannerSplash(binding.bannerContainerView, listIdBannerSplash, "banner_splash");
            AsyncSplash.Companion.getInstance().setOnPrepareLoadInterOpenSplashAds(new Function0<Unit>() {
                @Override
                public Unit invoke() { //prepare load and show inter/open splash
                    //RemoteConfigHelper.getInstance().set_config(SplashActivity.this, "inter_splash", false);
                    //RemoteConfigHelper.getInstance().set_config(SplashActivity.this, "open_splash", false);
                    return null;
                }
            });
            AsyncSplash.Companion.getInstance().handleAsync(this, this,
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
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AsyncSplash.Companion.getInstance().checkShowSplashWhenFail();
        appUpdateManager.registerListener(installStateUpdatedListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        appUpdateManager.unregisterListener(installStateUpdatedListener);
    }
}
