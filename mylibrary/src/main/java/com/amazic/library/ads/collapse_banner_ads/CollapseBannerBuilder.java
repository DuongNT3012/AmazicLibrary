package com.amazic.library.ads.collapse_banner_ads;

import com.amazic.library.ads.admob.AdmobApi;
import com.amazic.library.ads.callback.BannerCallback;

import java.util.ArrayList;
import java.util.List;

public class CollapseBannerBuilder {
    private BannerCallback callBack = new BannerCallback();
    private final List<String> listId = new ArrayList<>();
    private boolean isGravityBottom = true;

    public CollapseBannerBuilder() {
    }

    public boolean getBannerGravity(){
        return this.isGravityBottom;
    }

    public void setBannerGravity(boolean isGravityBottom){
        this.isGravityBottom = isGravityBottom;
    }

    public CollapseBannerBuilder setListId(List<String> listId) {
        this.listId.clear();
        this.listId.addAll(listId);
        return this;
    }

    public CollapseBannerBuilder setCallBack(BannerCallback callBack) {
        this.callBack = callBack;
        return this;
    }

    public CollapseBannerBuilder isIdApi() {
        this.listId.clear();
        this.listId.addAll(AdmobApi.getInstance().getListIDCollapseBannerAll());
        return this;
    }

    public void setListIdAd(String nameIdAd) {
        this.listId.clear();
        this.listId.addAll(AdmobApi.getInstance().getListIDByName(nameIdAd));
    }

    public BannerCallback getCallBack() {
        return callBack;
    }

    public List<String> getListId() {
        return listId;
    }
}