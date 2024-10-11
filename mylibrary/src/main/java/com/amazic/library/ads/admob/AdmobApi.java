package com.amazic.library.ads.admob;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.util.Log;

import com.amazic.library.ads.call_api.AdsModel;
import com.amazic.library.ads.call_api.ApiService;
import com.amazic.library.ads.callback.ApiCallback;
import com.amazic.library.ads.callback.BannerCallback;
import com.google.android.gms.ads.AdView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AdmobApi {
    private String TAG = "AdmobApi";
    private ApiService apiService;
    private String linkServer = "http://language-master.top";
    private String packageName = "";
    public String appIDRelease = "ca-app-pub-4973559944609228~2346710863";
    private static volatile AdmobApi INSTANCE;
    private Context context;

    LinkedHashMap<String, List<String>> listAds = new LinkedHashMap<>();

    public List<String> getListIDOpenSplash() {
        return getListIDByName("open_splash");
    }

    public List<String> getListIDNativeLanguage() {
        return getListIDByName("native_language");
    }

    public List<String> getListIDNativeIntro() {
        return getListIDByName("native_intro");
    }

    public List<String> getListIDNativePermission() {
        return getListIDByName("native_permission");
    }

    public List<String> getListIDNativeAll() {
        return getListIDByName("native_all");
    }

    public List<String> getListIDInterSplash() {
        return getListIDByName("inter_splash");
    }

    public List<String> getListIDInterAll() {
        return getListIDByName("inter_all");
    }

    public List<String> getListIDBannerAll() {
        return getListIDByName("banner_all");
    }

    public List<String> getListIDCollapseBannerAll() {
        return getListIDByName("collapse_banner");
    }

    public List<String> getListIDInterIntro() {
        return getListIDByName("inter_intro");
    }

    public List<String> getListIDAppOpenResume() {
        return getListIDByName("open_resume");
    }

    public List<String> getListIDByName(String nameAds) {
        List<String> list = new ArrayList<>();
        if (listAds.get(nameAds.toLowerCase().trim()) != null)
            list.addAll(Objects.requireNonNull(listAds.get(nameAds)));
        return list;
    }

    Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    public static synchronized AdmobApi getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AdmobApi();
        }
        return INSTANCE;
    }

    public void init(Context context, String linkServerRelease, String AppID, ApiCallback callBack) {
        this.context = context;
        listAds.clear();
        this.packageName = context.getPackageName();
        if (linkServerRelease != null && AppID != null) {
            if (!linkServerRelease.trim().equals("")
                    && (linkServerRelease.contains("http://")
                    || linkServerRelease.contains("https://"))) {
                this.linkServer = linkServerRelease.trim();
                this.appIDRelease = AppID.trim();
            }
        }

        String baseURL = linkServer + "/api/";
        apiService = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ApiService.class);

        Log.i(TAG, "link Server:" + baseURL);
        if (isNetworkConnected()) {
            fetchData(callBack);
        } else {
            new Handler().postDelayed(() -> callBack.onReady(), 2000);
        }

    }

    private void fetchData(ApiCallback callBack) {
        Log.e(TAG, "fetchData: ");
        try {
            String appID_package = appIDRelease + "+" + packageName;
            Log.i(TAG, "link Server query :" + linkServer + "/api/getidv2/" + appID_package);
            apiService.callAds(appID_package).enqueue(new Callback<List<AdsModel>>() {
                @Override
                public void onResponse(Call<List<AdsModel>> call, Response<List<AdsModel>> response) {
                    if (response.body() == null) {
                        new Handler().postDelayed(() -> callBack.onReady(), 2000);
                        return;
                    }
                    if (response.body().size() == 0) {
                        new Handler().postDelayed(() -> callBack.onReady(), 2000);
                        return;
                    }
                    for (AdsModel ads : response.body()) {
                        List<String> listIDAds = null;
                        if (listAds.containsKey(ads.getName())) {
                            listIDAds = listAds.get(ads.getName());
                        }
                        if (listIDAds == null) {
                            listIDAds = new ArrayList<>();
                        }
                        listIDAds.add(ads.getAds_id());
                        listAds.put(ads.getName().toLowerCase().trim(), listIDAds);
                    }
                    callBack.onReady();
                }

                @Override
                public void onFailure(Call<List<AdsModel>> call, Throwable t) {
                    Log.e(TAG, "onFailure: " + t.toString());
                    new Handler().postDelayed(() -> callBack.onReady(), 2000);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            new Handler().postDelayed(() -> callBack.onReady(), 2000);
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    /*public void loadBanner(final Activity activity) {
        Admob.getInstance().loadBannerFloor(activity, getListIDBannerAll());
    }

    public void loadBanner(final Activity activity, BannerCallback bannerCallBack) {
        Admob.getInstance().loadBannerFloor(activity, getListIDBannerAll(), bannerCallBack);
    }

    public void loadCollapsibleBanner(final Activity activity) {
        Admob.getInstance().loadCollapsibleBannerFloor(activity, getListIDCollapseBannerAll(), "bottom");
    }

    public void loadCollapsibleBanner(final Activity activity, BannerCallback bannerCallBack) {
        Admob.getInstance().loadCollapsibleBannerFloor(activity, getListIDCollapseBannerAll(), "bottom", bannerCallBack);
    }

    public AdView loadCollapsibleBannerFloorWithReload(final Activity activity, BannerCallback bannerCallBack) {
        return Admob.getInstance().loadCollapsibleBannerFloorWithReload(activity, getListIDCollapseBannerAll(), "bottom", bannerCallBack);
    }*/
}
