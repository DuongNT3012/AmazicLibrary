package com.amazic.library.ads.admob;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.amazic.library.Utils.NetworkUtil;
import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.library.ads.call_api.AdsModel;
import com.amazic.library.ads.call_api.ApiService;
import com.amazic.library.ads.callback.ApiCallback;
import com.amazic.library.ads.callback.AppOpenCallback;
import com.amazic.library.ads.callback.InterCallback;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private String jsonIdAdsDefault = "";
    private boolean isSetId = false;
    private int timeOutCallApi = 12000;

    public int getListAdsSize() {
        if (listAds != null) {
            return listAds.size();
        } else {
            return 0;
        }
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
        if (listAds.get(nameAds.trim()) != null)
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
        isSetId = false;
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

        if (NetworkUtil.isNetworkActive(context)) {
            fetchData(callBack);
            //after 12s, if cannot call api -> set list id default
            new Handler().postDelayed(() -> {
                if (!isSetId) { //if not set id from api -> set list id default
                    convertJsonIdAdsDefaultToList(jsonIdAdsDefault);
                    isSetId = true;
                    Log.d(TAG, "convertJsonIdAdsDefaultToList: timeout: isSetId = true");
                    callBack.onReady();
                } else {
                    Log.d(TAG, "xxxxxx1");
                }
            }, timeOutCallApi);
        } else {
            callBack.onReady();
        }
    }

    public void convertJsonIdAdsDefaultToList(String jsonIdAds) {
        listAds.clear();
        try {
            JSONArray jsonArray = new JSONArray(jsonIdAds);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                String name = jsonObject.getString("name").trim();
                String ads_id = jsonObject.getString("ads_id");

                // check if 'name' exists
                List<String> listIDAds = listAds.get(name);
                if (listIDAds == null) {
                    listIDAds = new ArrayList<>();
                    listAds.put(name, listIDAds);
                }

                // add ads_id
                listIDAds.add(ads_id);
            }
            Log.d(TAG, "convertJsonIdAdsDefaultToList: " + listAds.size());

            for (Map.Entry<String, List<String>> entry : listAds.entrySet()) {
                Log.d(TAG, "Key: " + entry.getKey() + ", Value: " + entry.getValue());
            }
        } catch (Exception e) {
            Log.e(TAG, "convertJsonIdAdsDefaultToList: Exception: " + e.getMessage());
        }
    }

    private void fetchData(ApiCallback callBack) {
        Log.e(TAG, "fetchData: ");
        try {
            String appID_package = appIDRelease + "+" + packageName;
            Log.i(TAG, "link Server query :" + linkServer + "/api/getidv2/" + appID_package);
            apiService.callAds(appID_package).enqueue(new Callback<List<AdsModel>>() {
                @Override
                public void onResponse(@NonNull Call<List<AdsModel>> call, @NonNull Response<List<AdsModel>> response) {
                    Log.d(TAG, "onResponse: isSetId: " + isSetId);
                    if (!isSetId) {
                        if (response.body() == null || response.body().isEmpty()) {
                            callBack.onReady();
                            return;
                        }
                        Log.d(TAG, "onResponse: " + listAds.size());
                        for (AdsModel ads : response.body()) {
                            List<String> listIDAds = null;
                            if (listAds.containsKey(ads.getName())) {
                                listIDAds = listAds.get(ads.getName());
                            }
                            if (listIDAds == null) {
                                listIDAds = new ArrayList<>();
                            }
                            listIDAds.add(ads.getAds_id());
                            listAds.put(ads.getName().trim(), listIDAds);
                            Log.d(TAG, ads.getName().trim() + "_" + ads.getAds_id());
                        }
                        isSetId = true;
                        Log.d(TAG, "isSetId = true2, listAds size = " + listAds.size());
                        callBack.onReady();
                    } else {
                        Log.d(TAG, "xxxxxx2");
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<AdsModel>> call, @NonNull Throwable t) {
                    Log.e(TAG, "onFailure: " + t);
                    Log.d(TAG, "onFailure: isSetId: " + isSetId);
                    if (!isSetId) {
                        convertJsonIdAdsDefaultToList(jsonIdAdsDefault);
                        isSetId = true;
                        Log.d(TAG, "convertJsonIdAdsDefaultToList: onFailure: isSetId = true");
                        callBack.onReady();
                    } else {
                        Log.d(TAG, "xxxxxx3");
                    }
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "fetchData: Exception: isSetId: " + isSetId);
            if (!isSetId) {
                convertJsonIdAdsDefaultToList(jsonIdAdsDefault);
                isSetId = true;
                Log.d(TAG, "convertJsonIdAdsDefaultToList: Exception: isSetId = true");
                callBack.onReady();
            } else {
                Log.d(TAG, "xxxxxx4");
            }
        }
    }

    public void refreshCacheOnly(Context context, String linkServerRelease, String AppID, ApiCallback callBack) {
        if (!NetworkUtil.isNetworkActive(context)) return;

        String linkServerTemp = linkServer;
        String appIDTemp = appIDRelease;
        if (linkServerRelease != null && AppID != null) {
            if (!linkServerRelease.trim().equals("")
                    && (linkServerRelease.contains("http://")
                    || linkServerRelease.contains("https://"))) {
                linkServerTemp = linkServerRelease.trim();
                appIDTemp = AppID.trim();
            }
        }

        String packageName = context.getPackageName();
        String baseURL = linkServerTemp + "/api/";
        ApiService tempApiService = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ApiService.class);

        String appID_package = appIDTemp + "+" + packageName;
        Log.d(TAG, "refreshCacheOnly: baseURL = " + baseURL +" , appID_package =" + appID_package);

        tempApiService.callAds(appID_package).enqueue(new Callback<List<AdsModel>>() {
            @Override
            public void onResponse(@NonNull Call<List<AdsModel>> call,
                                   @NonNull Response<List<AdsModel>> response) {
                Log.d(TAG, "refreshCacheOnly: response.body() - " + response.body());

                if (response.body() != null && !response.body().isEmpty()) {
                    // Build lại JSON từ response để lưu cache
                    // Không đụng vào listAds đang dùng
                    StringBuilder sb = new StringBuilder("[");
                    List<AdsModel> list = response.body();
                    for (int i = 0; i < list.size(); i++) {
                        AdsModel ads = list.get(i);
                        sb.append("{")
                                .append("\"name\":\"").append(ads.getName()).append("\",")
                                .append("\"ads_id\":\"").append(ads.getAds_id()).append("\"")
                                .append("}");
                        if (i < list.size() - 1) sb.append(",");
                    }
                    sb.append("]");
                    jsonIdAdsDefault = sb.toString();
                    callBack.onReady();
                    Log.d(TAG, "refreshCacheOnly: success - " + jsonIdAdsDefault);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AdsModel>> call, @NonNull Throwable t) {
                Log.d(TAG, "refreshCacheOnly: failed - " + t.getMessage());
                // Không làm gì, giữ cache cũ
            }
        });
    }

    public void loadOpenAppAdSplashFloor(AppCompatActivity activity, String adsKey, AppOpenCallback appOpenCallback) {
        AppOpenManager.getInstance().loadAndShowAppOpenResumeSplash(activity, AdmobApi.getInstance().getListIDByName(adsKey), appOpenCallback);
    }

    public void loadInterAdSplashFloor(AppCompatActivity activity, String adsKey, InterCallback interCallback) {
        Admob.getInstance().loadAndShowInterAdSplash(activity, AdmobApi.getInstance().getListIDByName(adsKey), interCallback);
    }

    public void loadInterAdSplashFloorDelayAds(AppCompatActivity activity, String adsKey, InterCallback interCallback, String adsKeyNative, String remoteKeyNative) {
        Admob.getInstance().loadAndShowInterAdSplashDelay(activity, AdmobApi.getInstance().getListIDByName(adsKey), interCallback, adsKeyNative, remoteKeyNative);
    }

    public void loadAndShowIdInterAdSplashAsync(AppCompatActivity activity, String adsKey, InterCallback interCallback) {
        Admob.getInstance().loadAndShowIdInterAdSplashAsync(activity, AdmobApi.getInstance().getListIDByName(adsKey), interCallback);
    }

    public void loadOpenAppAdSplashLoop(AppCompatActivity activity, String adsKey, AppOpenCallback appOpenCallback) {
        AppOpenManager.getInstance().loadAndShowAppOpenResumeSplashLoop(activity, AdmobApi.getInstance().getListIDByName(adsKey), appOpenCallback);
    }

    public void loadInterAdSplashLoop(AppCompatActivity activity, String adsKey, InterCallback interCallback) {
        Admob.getInstance().loadAndShowInterAdSplashLoop(activity, AdmobApi.getInstance().getListIDByName(adsKey), interCallback);
    }

    public void loadAndShowInterAdPreloadingSplash(AppCompatActivity activity, String adsKey, InterCallback interCallback, String adsKeyNative, String remoteKeyNative) {
        Admob.getInstance().loadAndShowInterAdPreloadingSplashDelay(activity, AdmobApi.getInstance().getListIDByName(adsKey), interCallback, adsKeyNative, remoteKeyNative);
    }

    public void loadAndShowAppOpenAdPreloadingSplash(AppCompatActivity activity, String adsKey, AppOpenCallback appOpenCallback) {
        AppOpenManager.getInstance().loadAndShowAdPreloadingAppOpenSplash(activity, AdmobApi.getInstance().getListIDByName(adsKey), appOpenCallback);
    }

    public void loadAndShowNativeFullSplash(AppCompatActivity activity, String adsKeyNative, InterCallback interCallback, String remoteKeyNative) {
        Admob.getInstance().loadAndShowNativeFullSplashCount(activity, AdmobApi.getInstance().getListIDByName(adsKeyNative), interCallback, adsKeyNative, remoteKeyNative);
    }
}
