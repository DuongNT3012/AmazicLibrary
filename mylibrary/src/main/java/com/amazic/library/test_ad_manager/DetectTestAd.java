package com.amazic.library.test_ad_manager;

public class DetectTestAd {
    public static DetectTestAd INSTANCE;

    private boolean showAds = false;
    private boolean isTestAd = false;

    public void setShowAds(boolean showAds) {
        this.showAds = showAds;
    }

    public static DetectTestAd getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DetectTestAd();
        }
        return INSTANCE;
    }

    public void detectedTestAd(boolean isTestAd) {
        this.isTestAd = isTestAd;
    }

    public boolean isTestAd() {
        return this.isTestAd && !this.showAds;
    }
}
