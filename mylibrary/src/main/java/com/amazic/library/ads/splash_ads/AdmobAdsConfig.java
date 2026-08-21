package com.amazic.library.ads.splash_ads;

import android.content.Context;

import com.amazic.library.Utils.RemoteConfigHelper;
import com.amazic.library.ads.callback.InterCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdmobAdsConfig {


    public static final String WELCOME_BACK_NORMAL = "Normal";
    public static final String WELCOME_BACK_BELOW = "Below";
    public static final String WELCOME_BACK_ABOVE = "Above";
    public static final String  DETECT_TEST_AD = "DetectTestAd";
    public static final String  TECH_MANAGER = "TechManager";

    private static AdmobAdsConfig INSTANCE = null;

    public static AdmobAdsConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AdmobAdsConfig();
        }
        return INSTANCE;
    }
    
    private boolean isTech = false;
    private String jsonIdAdsDefault = "";
    private int timeOutCallApi = 4000;
    private String adjustKey = "";
    private String appId = "";
    private String appPkg = "";
    private boolean isShowAdsSplash = false;
    private boolean isTimeout = false;
    private boolean isNoInternetAction = false;
    private String initWelcomeBack = "Normal";
    private Class<?> welcomeBackClass = null;
    private boolean isShowBannerSplash = false;
    private List<String> listIdBannerSplash =
            new ArrayList<>(Arrays.asList("ca-app-pub-3940256099942544/6300978111"));
    private String adsKey = "";
    private List<String> listTurnOffRemoteKeys = new ArrayList<>();
    private InterCallback interCallback = null;
    private boolean isDebug = false;
    private boolean isUseBilling = false;

    //private List<ProductDetailCustom> listProductDetailCustoms = new ArrayList<>(); //comment for billing
    private long timeOutSplash = 12000L;
    private long timeOutInitAdmob = 12000L;
    private boolean isLoopAdsSplash = false;
    private String useTechManagerOrDetectTestAd = DETECT_TEST_AD;

    //1.use for log event time out 12s
    private boolean initRemoteConfig = false;
    private boolean initAdmobApi = false;
    private boolean initAdsConsentManager = false;
    private boolean initBilling = false;
    private boolean initTechManager = false;

    //
    private boolean isUseIdAdsFromRemoteConfig = false;
    private boolean isPreloadResumeAds = true;
    private boolean isAsyncSplashAds = false;

    //ad preloading
    private boolean isUseAdPreloading = false;
    private int numberPreloading = 3;
    private int numberPreloadingSplash = 1;
    private boolean isShowNativeAfterInter = false;

    //    private boolean isUseNativeSplash = false;
    private boolean isUseNativeFullSplash = false;
    private boolean isUseCacheDataCallSplash = false;
    private boolean isUseDetectionVPNOrEmulator = false;

    private boolean isUseNativeFullSplashAdmobWhenMetaFail = false;

    //1.end
    //2.use for log event
    private long timeStartSplash = System.currentTimeMillis();
    //2.end

    //Use for inter and open splash 1, 2, 3...
    private String keyAdsInterSplash = "inter_splash";
    private String keyAdsOpenSplash = "open_splash";
    private String keyAdsOpenResume = "";

    //key native after inter splash
    private String keyNativeAfterInterSplash = "native_after_inter";

    //
    private int numberNativeFullShowSplash = 1;

    //id set test native meta
    private String idNativeMetaSplash = "";
    private String keyNativeFullMetaSplash = "native_meta_splash";
    //end

    //native full splash admob when meta fail
    private String keyNativeFullAdmobSplash = "";
    //end

    //native meta splash
    private boolean isUseNativeSplashMeta = false;
    //end

    private boolean isUseAppUpdateManager = false;
    private String remoteKeyIdAdsServer = "id_ads";

    //Log event 26/04/2025
    private long timeSplashCheck = System.currentTimeMillis();

    //check internet speed
    private String urlCheckInternetSpeed = "https://www.google.com/";

    //Set loadAndShowIdInterAdSplashAsync 30/05/2025
    private boolean loadAndShowIdInterAdSplashAsync = false;

    //Set timeout call id remote config
    private long timeOutCallIdRemoteConfig = 4000L;
    private boolean isSetId = false;

    //Set key interval inter
    private String keyIntervalBetweenInterstitial = "interval_between_interstitial";
    private String keyIntervalInterstitialFromStart = "interval_interstitial_from_start";
    private long timeStep1 = System.currentTimeMillis();
    private long timeLastStep = System.currentTimeMillis();
//    private Function0<Unit> onInitAdmobDone = null;

    /**
     * Reset toàn bộ cấu hình về giá trị mặc định ban đầu (tương đương resetVarToDefault()
     * trước đây trong AsyncSplash).
     */
    public void clear() {
        isTech = false;
        jsonIdAdsDefault = "";
        timeOutCallApi = 4000;
        adjustKey = "";
        appId = "";
        isShowAdsSplash = false;
        isTimeout = false;
        isNoInternetAction = false;
        initWelcomeBack = "Normal";
        welcomeBackClass = null;
        isShowBannerSplash = false;
        listIdBannerSplash = new ArrayList<>(Arrays.asList("ca-app-pub-3940256099942544/6300978111"));
        adsKey = "";
        listTurnOffRemoteKeys = new ArrayList<>();
        interCallback = null;
        isDebug = false;
        isUseBilling = false;
        timeOutSplash = 12000L;
        isLoopAdsSplash = false;
        useTechManagerOrDetectTestAd = DETECT_TEST_AD;
        initRemoteConfig = false;
        initAdmobApi = false;
        initAdsConsentManager = false;
        initBilling = false;
        initTechManager = false;
        isUseIdAdsFromRemoteConfig = false;
        isPreloadResumeAds = true;
        isAsyncSplashAds = false;
        isUseAdPreloading = false;
        numberPreloading = 3;
        numberPreloadingSplash = 1;
        isShowNativeAfterInter = false;
        isUseNativeFullSplash = false;
        isUseCacheDataCallSplash = false;
        isUseDetectionVPNOrEmulator = false;
        isUseNativeFullSplashAdmobWhenMetaFail = false;
        timeStartSplash = System.currentTimeMillis();
        keyAdsInterSplash = "inter_splash";
        keyAdsOpenSplash = "open_splash";
        keyAdsOpenResume = "";
        keyNativeAfterInterSplash = "native_after_inter";
        numberNativeFullShowSplash = 1;
        idNativeMetaSplash = "";
        keyNativeFullMetaSplash = "native_meta_splash";
        keyNativeFullAdmobSplash = "";
        isUseNativeSplashMeta = false;
        isUseAppUpdateManager = false;
        remoteKeyIdAdsServer = "id_ads";
        timeSplashCheck = System.currentTimeMillis();
        urlCheckInternetSpeed = "https://www.google.com/";
        loadAndShowIdInterAdSplashAsync = false;
        timeOutCallIdRemoteConfig = 4000L;
        isSetId = false;
        keyIntervalBetweenInterstitial = "interval_between_interstitial";
        keyIntervalInterstitialFromStart = "interval_interstitial_from_start";
        timeStep1 = System.currentTimeMillis();
        timeLastStep = System.currentTimeMillis();
    }

    public boolean isTech() {
        return isTech;
    }

    public void setTech(boolean tech) {
        isTech = tech;
    }

    public String getJsonIdAdsDefault() {
        return jsonIdAdsDefault;
    }

    public void setJsonIdAdsDefault(String jsonIdAdsDefault) {
        this.jsonIdAdsDefault = jsonIdAdsDefault;
    }

    public int getTimeOutCallApi() {
        return timeOutCallApi;
    }

    public void setTimeOutCallApi(int timeOutCallApi) {
        this.timeOutCallApi = timeOutCallApi;
    }

    public String getAdjustKey() {
        return adjustKey;
    }

    public void setAdjustKey(String adjustKey) {
        this.adjustKey = adjustKey;
    }

    public String getAppPkg() {
        return appPkg;
    }

    public void setAppPkg(String appId) {
        this.appPkg = appId;
    }
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public boolean isShowAdsSplash() {
        return isShowAdsSplash;
    }

    public void setShowAdsSplash(boolean showAdsSplash) {
        isShowAdsSplash = showAdsSplash;
    }

    public boolean isTimeout() {
        return isTimeout;
    }

    public void setTimeout(boolean timeout) {
        isTimeout = timeout;
    }

    public boolean isNoInternetAction() {
        return isNoInternetAction;
    }

    public void setNoInternetAction(boolean noInternetAction) {
        isNoInternetAction = noInternetAction;
    }

    public String getInitWelcomeBack() {
        return initWelcomeBack;
    }

    public void setInitResumeAdsNormal() {
        initWelcomeBack = WELCOME_BACK_NORMAL;
        isPreloadResumeAds = true;
    }

    public void setInitWelcomeBackBelowResumeAds(Class<?> welcomeBackClass) {
        initWelcomeBack = WELCOME_BACK_BELOW;
        this.welcomeBackClass = welcomeBackClass;
        isPreloadResumeAds = true;
    }

    public void setInitWelcomeBackAboveResumeAds(Class<?> welcomeBackClass) {
        initWelcomeBack = WELCOME_BACK_ABOVE;
        this.welcomeBackClass = welcomeBackClass;
        isPreloadResumeAds = false;
    }
    public Class<?> getWelcomeBackClass() {
        return welcomeBackClass;
    }

    public void setWelcomeBackClass(Class<?> welcomeBackClass) {
        this.welcomeBackClass = welcomeBackClass;
    }

    public boolean isShowBannerSplash() {
        return isShowBannerSplash;
    }

    public void setShowBannerSplash(boolean showBannerSplash) {
        isShowBannerSplash = showBannerSplash;
    }

    public List<String> getListIdBannerSplash() {
        return listIdBannerSplash;
    }

    public void setListIdBannerSplash(List<String> listIdBannerSplash) {
        this.listIdBannerSplash = listIdBannerSplash;
    }

    public String getAdsKey() {
        return adsKey;
    }

    public void setAdsKey(String adsKey) {
        this.adsKey = adsKey;
    }

    public List<String> getListTurnOffRemoteKeys() {
        return listTurnOffRemoteKeys;
    }

    public void setListTurnOffRemoteKeys(List<String> listTurnOffRemoteKeys) {
        this.listTurnOffRemoteKeys = listTurnOffRemoteKeys;
    }

    public InterCallback getInterCallback() {
        return interCallback;
    }

    public void setInterCallback(InterCallback interCallback) {
        this.interCallback = interCallback;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public void setDebug(boolean debug) {
        isDebug = debug;
    }

    public boolean isUseBilling() {
        return isUseBilling;
    }

    public void setUseBilling(boolean useBilling) {
        isUseBilling = useBilling;
    }

    public long getTimeOutSplash() {
        return timeOutSplash;
    }

    public void setTimeOutSplash(long timeOutSplash) {
        this.timeOutSplash = timeOutSplash;
    }
    public long getTimeOutInitAdmob() {
        return timeOutInitAdmob;
    }

    public void setTimeOutInitAdmob(long timeOutInitAdmob) {
        this.timeOutInitAdmob = timeOutInitAdmob;
    }

    public boolean isLoopAdsSplash() {
        return isLoopAdsSplash;
    }

    public void setLoopAdsSplash(boolean loopAdsSplash) {
        isLoopAdsSplash = loopAdsSplash;
    }

    public String getUseTechManagerOrDetectTestAd() {
        return useTechManagerOrDetectTestAd;
    }

    public void setUseDetectTestAd() {
        useTechManagerOrDetectTestAd = DETECT_TEST_AD;
    }
    public void setUseTechManagerOrDetectTestAd(String useTechManagerOrDetectTestAd) {
        this.useTechManagerOrDetectTestAd = useTechManagerOrDetectTestAd;
    }

    // Lưu ý: các field init* KHÔNG có tiền tố "is" (đúng như tên biến gốc trong Kotlin),
    // nên getter phải là getXxx() (không phải isXxx()) để Kotlin sinh ra property
    // "initRemoteConfig" thay vì "isInitRemoteConfig".
    public boolean getInitRemoteConfig() {
        return initRemoteConfig;
    }

    public void setInitRemoteConfig(boolean initRemoteConfig) {
        this.initRemoteConfig = initRemoteConfig;
    }

    public boolean getInitAdmobApi() {
        return initAdmobApi;
    }

    public void setInitAdmobApi(boolean initAdmobApi) {
        this.initAdmobApi = initAdmobApi;
    }

    public boolean getInitAdsConsentManager() {
        return initAdsConsentManager;
    }

    public void setInitAdsConsentManager(boolean initAdsConsentManager) {
        this.initAdsConsentManager = initAdsConsentManager;
    }

    public boolean getInitBilling() {
        return initBilling;
    }

    public void setInitBilling(boolean initBilling) {
        this.initBilling = initBilling;
    }

    public boolean getInitTechManager() {
        return initTechManager;
    }

    public void setInitTechManager(boolean initTechManager) {
        this.initTechManager = initTechManager;
    }

    public boolean isUseIdAdsFromRemoteConfig() {
        return isUseIdAdsFromRemoteConfig;
    }

    public void setUseIdAdsFromRemoteConfig(boolean useIdAdsFromRemoteConfig) {
        isUseIdAdsFromRemoteConfig = useIdAdsFromRemoteConfig;
    }

    public boolean isPreloadResumeAds() {
        return isPreloadResumeAds;
    }

    public void setPreloadResumeAds(boolean preloadResumeAds) {
        isPreloadResumeAds = preloadResumeAds;
    }

    public boolean isAsyncSplashAds() {
        return isAsyncSplashAds;
    }

    public void setAsyncSplashAds(boolean asyncSplashAds) {
        isAsyncSplashAds = asyncSplashAds;
    }

    public boolean isUseAdPreloading() {
        return isUseAdPreloading;
    }

    public void setUseAdPreloading(boolean useAdPreloading) {
        isUseAdPreloading = useAdPreloading;
    }

    public int getNumberPreloading() {
        return numberPreloading;
    }

    public void setNumberPreloading(int numberPreloading) {
        this.numberPreloading = numberPreloading;
    }

    public int getNumberPreloadingSplash() {
        return numberPreloadingSplash;
    }

    public void setNumberPreloadingSplash(int numberPreloadingSplash) {
        this.numberPreloadingSplash = numberPreloadingSplash;
    }

    public boolean isShowNativeAfterInter() {
        return isShowNativeAfterInter;
    }

    public void setShowNativeAfterInter(boolean showNativeAfterInter) {
        isShowNativeAfterInter = showNativeAfterInter;
    }

    public boolean isUseNativeFullSplash() {
        return isUseNativeFullSplash;
    }

    public void setUseNativeFullSplash(boolean useNativeFullSplash) {
        isUseNativeFullSplash = useNativeFullSplash;
    }

    public boolean isUseCacheDataCallSplash() {
        return isUseCacheDataCallSplash;
    }

    public void setUseCacheDataCallSplash(boolean useCacheDataCallSplash) {
        isUseCacheDataCallSplash = useCacheDataCallSplash;
    }

    public boolean isUseDetectionVPNOrEmulator() {
        return isUseDetectionVPNOrEmulator;
    }

    public void setUseDetectionVPNOrEmulator(boolean useDetectionVPNOrEmulator) {
        isUseDetectionVPNOrEmulator = useDetectionVPNOrEmulator;
    }

    public boolean isUseNativeFullSplashAdmobWhenMetaFail() {
        return isUseNativeFullSplashAdmobWhenMetaFail;
    }

    public void setUseNativeFullSplashAdmobWhenMetaFail(boolean useNativeFullSplashAdmobWhenMetaFail) {
        isUseNativeFullSplashAdmobWhenMetaFail = useNativeFullSplashAdmobWhenMetaFail;
    }

    public long getTimeStartSplash() {
        return timeStartSplash;
    }

    public void setTimeStartSplash(long timeStartSplash) {
        this.timeStartSplash = timeStartSplash;
    }

    public String getKeyAdsInterSplash() {
        return keyAdsInterSplash;
    }

    public void setKeyAdsInterSplash(String keyAdsInterSplash) {
        this.keyAdsInterSplash = keyAdsInterSplash;
    }

    public String getKeyAdsOpenSplash() {
        return keyAdsOpenSplash;
    }

    public void setKeyAdsOpenSplash(String keyAdsOpenSplash) {
        this.keyAdsOpenSplash = keyAdsOpenSplash;
    }

    public String getKeyAdsOpenResume() {
        return keyAdsOpenResume;
    }

    public void setKeyAdsOpenResume(String keyAdsOpenResume) {
        this.keyAdsOpenResume = keyAdsOpenResume;
    }

    public String getKeyNativeAfterInterSplash() {
        return keyNativeAfterInterSplash;
    }

    public void setKeyNativeAfterInterSplash(String keyNativeAfterInterSplash) {
        this.keyNativeAfterInterSplash = keyNativeAfterInterSplash;
    }

    public int getNumberNativeFullShowSplash() {
        return numberNativeFullShowSplash;
    }

    public void setNumberNativeFullShowSplash(int numberNativeFullShowSplash) {
        this.numberNativeFullShowSplash = numberNativeFullShowSplash;
    }

    public String getIdNativeMetaSplash() {
        return idNativeMetaSplash;
    }

    public void setIdNativeMetaSplash(String idNativeMetaSplash) {
        this.idNativeMetaSplash = idNativeMetaSplash;
    }

    public String getKeyNativeFullMetaSplash() {
        return keyNativeFullMetaSplash;
    }

    public void setKeyNativeFullMetaSplash(String keyNativeFullMetaSplash) {
        this.keyNativeFullMetaSplash = keyNativeFullMetaSplash;
    }

    public String getKeyNativeFullAdmobSplash() {
        return keyNativeFullAdmobSplash;
    }

    public void setKeyNativeFullAdmobSplash(String keyNativeFullAdmobSplash) {
        this.keyNativeFullAdmobSplash = keyNativeFullAdmobSplash;
    }

    public boolean isUseNativeSplashMeta() {
        return isUseNativeSplashMeta;
    }

    public void setUseNativeSplashMeta(boolean useNativeSplashMeta) {
        isUseNativeSplashMeta = useNativeSplashMeta;
    }

    public boolean isUseAppUpdateManager() {
        return isUseAppUpdateManager;
    }

    public void setUseAppUpdateManager(boolean useAppUpdateManager) {
        isUseAppUpdateManager = useAppUpdateManager;
    }

    public String getRemoteKeyIdAdsServer() {
        return remoteKeyIdAdsServer;
    }

    public void setUseIdAdsFromRemoteConfig(String remoteKeyIdAdsServer) {
        // Use id ads from remote config or not (remote key: id_ads)
        this.isUseIdAdsFromRemoteConfig = true;
        this.remoteKeyIdAdsServer = remoteKeyIdAdsServer;
        this.timeOutCallApi = 0;
    }

    public long getTimeSplashCheck() {
        return timeSplashCheck;
    }

    public void setTimeSplashCheck(long timeSplashCheck) {
        this.timeSplashCheck = timeSplashCheck;
    }

    public String getUrlCheckInternetSpeed() {
        return urlCheckInternetSpeed;
    }

    public void setUrlCheckInternetSpeed(String urlCheckInternetSpeed) {
        this.urlCheckInternetSpeed = urlCheckInternetSpeed;
    }

    // Không có tiền tố "is" trong tên biến gốc -> dùng getXxx() thay vì isXxx()
    public boolean getLoadAndShowIdInterAdSplashAsync() {
        return loadAndShowIdInterAdSplashAsync;
    }

    public void setLoadAndShowIdInterAdSplashAsync(boolean loadAndShowIdInterAdSplashAsync) {
        this.loadAndShowIdInterAdSplashAsync = loadAndShowIdInterAdSplashAsync;
    }

    public long getTimeOutCallIdRemoteConfig() {
        return timeOutCallIdRemoteConfig;
    }

    public void setTimeOutCallIdRemoteConfig(long timeOutCallIdRemoteConfig) {
        this.timeOutCallIdRemoteConfig = timeOutCallIdRemoteConfig;
    }

    public boolean isSetId() {
        return isSetId;
    }

    public void setSetId(boolean setId) {
        isSetId = setId;
    }

    public String getKeyIntervalBetweenInterstitial() {
        return keyIntervalBetweenInterstitial;
    }

    public void setKeyIntervalBetweenInterstitial(String keyIntervalBetweenInterstitial) {
        this.keyIntervalBetweenInterstitial = keyIntervalBetweenInterstitial;
    }

    public String getKeyIntervalInterstitialFromStart() {
        return keyIntervalInterstitialFromStart;
    }

    public void setKeyIntervalInterstitialFromStart(String keyIntervalInterstitialFromStart) {
        this.keyIntervalInterstitialFromStart = keyIntervalInterstitialFromStart;
    }

    public long getTimeStep1() {
        return timeStep1;
    }

    public void setTimeStep1(long timeStep1) {
        this.timeStep1 = timeStep1;
    }

    public long getTimeLastStep() {
        return timeLastStep;
    }

    public void setTimeLastStep(long timeLastStep) {
        this.timeLastStep = timeLastStep;
    }


    public void turnOffSomeRemoteKeys(Context activity) {
        listTurnOffRemoteKeys.forEach(remote -> {
            RemoteConfigHelper.getInstance().set_config(activity, remote, false);
        });
    }
}