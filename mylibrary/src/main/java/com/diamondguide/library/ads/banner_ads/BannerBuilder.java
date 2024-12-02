package com.diamondguide.library.ads.banner_ads;

import com.diamondguide.library.ads.admob.AdmobApi;
import com.diamondguide.library.ads.callback.BannerCallback;

import java.util.ArrayList;
import java.util.List;

public class BannerBuilder {
    private BannerCallback callBack = new BannerCallback();
    private final List<String> listId = new ArrayList<>();

    public BannerBuilder() {
    }

    public BannerBuilder setListId(List<String> listId) {
        this.listId.clear();
        this.listId.addAll(listId);
        return this;
    }

    public BannerBuilder setCallBack(BannerCallback callBack) {
        this.callBack = callBack;
        return this;
    }

    public BannerBuilder isIdApi() {
        this.listId.clear();
        this.listId.addAll(AdmobApi.getInstance().getListIDBannerAll());
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