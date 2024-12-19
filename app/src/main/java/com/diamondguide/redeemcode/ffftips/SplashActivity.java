package com.diamondguide.redeemcode.ffftips;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwnerKt;

import com.amazic.library.ads.admob.Admob;
import com.amazic.library.ads.callback.AppOpenCallback;
import com.amazic.library.ads.callback.InterCallback;
import com.amazic.library.ads.splash_ads.AsyncSplash;
import com.amazic.library.iap.BillingCallback;
import com.amazic.library.iap.IAPManager;
import com.amazic.library.iap.ProductDetailCustom;
import com.diamondguide.redeemcode.ffftips.databinding.ActivitySplashBinding;

import java.util.ArrayList;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;
    private InterCallback interCallback;
    private AppOpenCallback appOpenCallback;

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

            @Override
            public void onAdFailedToLoad() {
                super.onAdFailedToLoad();
                AsyncSplash.Companion.getInstance().reloadAdsSplash(SplashActivity.this, appOpenCallback, interCallback);
            }
        };

        appOpenCallback = new AppOpenCallback() {
            @Override
            public void onNextAction() {
                super.onNextAction();
                startNextAct();
            }

            @Override
            public void onAdFailedToLoad() {
                super.onAdFailedToLoad();
                AsyncSplash.Companion.getInstance().reloadAdsSplash(SplashActivity.this, appOpenCallback, interCallback);
            }
        };

        Admob.getInstance().setOpenActivityAfterShowInterAds(true);
        //Admob.getInstance().setDetectTestAdByView(true);
        Admob.getInstance().setTimeStart(System.currentTimeMillis());
        //Admob.getInstance().setTimeIntervalFromStart(20000);
        //Admob.getInstance().setTimeInterval(1000);

        AsyncSplash.Companion.getInstance().init(this, appOpenCallback, interCallback, "c193nrau3dhc", "", "", "");
        AsyncSplash.Companion.getInstance().setTimeOutSplash(120);
        ArrayList<ProductDetailCustom> listIAP = new ArrayList<>();
        listIAP.add(new ProductDetailCustom(IAPManager.PRODUCT_ID_TEST, IAPManager.typeSub));
        AsyncSplash.Companion.getInstance().setUseBilling(listIAP); //if app use IAP
        //AsyncSplash.Companion.getInstance().setInitResumeAdsNormal(); //init resume ads without welcome back
        AsyncSplash.Companion.getInstance().setInitWelcomeBackAboveResumeAds(WelcomeBackActivity.class); //init resume ads with welcome back above
        //AsyncSplash.Companion.getInstance().setInitWelcomeBackBelowResumeAds(WelcomeBackActivity.class); //init resume ads with welcome back below
        AsyncSplash.Companion.getInstance().setDebug(true); //use for TechManager
        ArrayList<String> listTurnOffRemote = new ArrayList<>();
        listTurnOffRemote.add("banner_splash");
        AsyncSplash.Companion.getInstance().setListTurnOffRemoteKeys(listTurnOffRemote); //set list off remote of TechManager
        AsyncSplash.Companion.getInstance().handleAsync(this, LifecycleOwnerKt.getLifecycleScope(this), new Function0<Unit>() {
            @Override
            public Unit invoke() {
                interCallback.onNextAction();
                return null;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        AsyncSplash.Companion.getInstance().checkShowSplashWhenFail();
    }

    private void startNextAct() {
        Log.d("SplashActivity", "startNextAct.");
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
