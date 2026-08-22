package com.amazic.library.ads.splash_ads;

public class EventNameSplash {

    // ===== AdsSplash (legacy load) =====
    public static final String EVENT_START_LOAD_SPLASH_LEGACY = "start_load_splash_legacy"; // used 1x - AdsSplash.loadInterSplashLegacy
    public static final String EVENT_LOADED_SPLASH_LEGACY = "loaded_splash_legacy"; // used 1x - AdsSplash.loadInterSplashLegacy (onAdLoaded)
    public static final String EVENT_LOAD_FAILED_SPLASH_LEGACY = "load_failed_splash_legacy"; // used 1x - AdsSplash.loadInterSplashLegacy (onAdFailedToLoad)

    // ===== AdsSplash (preload) =====
    public static final String EVENT_START_LOAD_SPLASH_PRELOAD = "start_load_splash_preload"; // used 1x - AdsSplash.loadInterSplashPreload
    public static final String EVENT_LOADED_SPLASH_PRELOAD = "loaded_splash_preload"; // used 1x - AdsSplash.loadInterSplashPreload (onAdPreloaded)
    public static final String EVENT_LOAD_FAILED_SPLASH_PRELOAD = "load_failed_splash_preload"; // used 1x - AdsSplash.loadInterSplashPreload (onAdFailedToPreload)
    public static final String EVENT_LOAD_EXHAUSTED_SPLASH_PRELOAD = "load_exhaust_splash_preload"; // used 1x - AdsSplash.loadInterSplashPreload (onAdsExhausted)

    // ===== Điều kiện load/show chung =====
    public static final String EVENT_LOAD_FAILED_SPLASH_CONDITION = "load_failed_splash_condition"; // used 2x - AdsSplash.loadInterSplashLegacy + loadInterSplashPreload
    public static final String EVENT_LOAD_FAILED_SPLASH_ID_NULL = "load_failed_splash_id_null"; // used 3x - AsyncSplash.loadAndShowInterSplash + loadInterSplash + checkShowSplashWhenFail

    // ===== Show splash =====
    public static final String EVENT_START_SHOW_SPLASH = "start_show_splash"; // used 1x - AdsSplash.showInterSplash

    public static final String EVENT_SHOW_FAILED_SPLASH_AD_NULL = "show_failed_splash_ad_null"; // used 1x - AdsSplash.showInterSplash (ad chưa sẵn sàng)
    public static final String EVENT_SHOW_FAILED_SPLASH_ACTIVITY = "show_failed_splash_activity"; // used 1x - AdsSplash.showInterSplash (activity finishing/destroyed)
    public static final String EVENT_SHOW_FAILED_SPLASH_APP_BG = "show_failed_splash_app_bg"; // used 1x - AdsSplash.showInterSplash (app in background)
    public static final String EVENT_SHOW_FAILED_SPLASH_CONDITION = "show_failed_splash_condition"; // used 1x - AdsSplash.showInterSplash (checkNotAllowCondition)
    public static final String EVENT_SHOW_FAILED_SPLASH_TIMEOUT = "show_failed_splash_timeout"; // used 6x - AdsSplash.loadAndShowLegacy(2) + loadAndShowPreload(2) + AsyncSplash.loadAndShowInterSplash + loadInterSplash

    // ===== FullScreenContentCallback =====
    public static final String EVENT_INTER_SPLASH_DISMISS = "inter_splash_dismiss"; // used 1x - AdsSplash.showInterSplash (onAdDismissedFullScreenContent)
    public static final String EVENT_INTER_SPLASH_FAILED_SHOW = "inter_splash_failed_show"; // used 1x - AdsSplash.showInterSplash (onAdFailedToShowFullScreenContent)
    public static final String EVENT_INTER_SPLASH_CLICK = "inter_splash_click"; // used 1x - AdsSplash.showInterSplash (onAdClicked)
    public static final String EVENT_INTER_SPLASH_SHOW = "inter_splash_show"; // used 1x - AdsSplash.showInterSplash (onAdShowedFullScreenContent)
    public static final String EVENT_INTER_SPLASH_IMPRESSION = "inter_splash_impression"; // used 1x - AdsSplash.showInterSplash (onAdImpression)

    // ===== AsyncSplash (async init flow) - bổ sung, trước đây là literal rời rạc =====
    public static final String EVENT_HANDLE_ASYNC = "handleAsync"; // used 1x - AsyncSplash.handleAsync (bắt đầu luồng)
    public static final String EVENT_NO_INTERNET = "NoInternet"; // used 1x - AsyncSplash.handleAsync (không có mạng)
    public static final String EVENT_START_ASYNC_INIT = "StartAsyncInit"; // used 1x - AsyncSplash.runAsyncInitAndShowAds (bắt đầu chạy song song remote config + consent)
    public static final String EVENT_DONE_ASYNC_INIT = "DoneAsyncInit"; // used 1x - AsyncSplash.runAsyncInitAndShowAds (xong consent, bắt đầu chờ Admob init)
    public static final String EVENT_START_WAIT_ADMOB_INIT = "StartWaitAdmobInit"; // used 1x - AsyncSplash.runAsyncInitAndShowAds
    public static final String EVENT_DONE_WAIT_ADMOB_INIT = "DoneWaitAdmobInit"; // used 1x - AsyncSplash.runAsyncInitAndShowAds
    public static final String EVENT_TIMEOUT_ACTIVITY_NULL = "TimeoutActivityNull"; // used 1x - AsyncSplash.runTimeOutSplash (timeout nhưng activity đã null)
    public static final String EVENT_TIMEOUT_SPLASH = "Timeout"; // used 1x - AsyncSplash.runTimeOutSplash (timeout thực sự xảy ra)
    public static final String EVENT_START_LOAD_AND_SHOW_INTER = "StartLoadAndShowInter"; // used 1x - AsyncSplash.loadAndShowInterSplash
    public static final String EVENT_START_LOAD_ONLY = "StartLoadOnly"; // used 1x - AsyncSplash.loadInterSplash (chỉ load, không show - dùng lại sau timeout)
    public static final String EVENT_DONE_INIT_CONSENT = "DoneInitConsent"; // used 1x - AsyncSplash.initAdsConsentManager
    public static final String EVENT_DONE_INIT_REMOTE_CONFIG = "DoneInitRemoteConfig"; // used 1x - AsyncSplash.initRemoteConfig
}