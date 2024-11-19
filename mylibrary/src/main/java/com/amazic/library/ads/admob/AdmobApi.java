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
    private String jsonIdAdsDefault = "[{\"id\":14,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"inter_splash\",\"ads_id\":\"ca-app-pub-3940256099942544\\/3419835294\"},{\"id\":15,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"policy_inter_splash\",\"ads_id\":\"ca-app-pub-3940256099942544\\/3419835294\"},{\"id\":16,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"policy_inter_theme\",\"ads_id\":\"ca-app-pub-3940256099942544\\/1033173712\"},{\"id\":17,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"banner_all\",\"ads_id\":\"ca-app-pub-3940256099942544\\/6300978111\"},{\"id\":18,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"open_splash\",\"ads_id\":\"ca-app-pub-3940256099942544\\/9257395921\"},{\"id\":19,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"inter_all\",\"ads_id\":\"ca-app-pub-3940256099942544\\/1033173712\"},{\"id\":20,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"policy_open_splash\",\"ads_id\":\"ca-app-pub-3940256099942544\\/3419835294\"},{\"id\":21,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"inter_intro\",\"ads_id\":\"ca-app-pub-3940256099942544\\/1033173712\"},{\"id\":91,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"collapse_banner\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2014213617\"},{\"id\":2326,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_preview\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2327,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_theme\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2425,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_emi\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2426,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_result\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2427,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_welcome\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2428,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_success\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2435,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"rewarded_animation\",\"ads_id\":\"ca-app-pub-3940256099942544\\/5224354917\"},{\"id\":2436,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_preview\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2437,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_apply\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2438,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_ringtone\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2439,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_gallery\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2440,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"inter_info\",\"ads_id\":\"ca-app-pub-3940256099942544\\/1033173712\"},{\"id\":2441,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_home\",\"ads_id\":\"11\"},{\"id\":2442,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_home\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2443,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_welcome\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2448,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_guide\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2449,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_configuration\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2450,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_merge_audio\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2451,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_merge_video\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2452,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_cutter\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2453,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_process\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2454,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"inter_splash\",\"ads_id\":\"ca-app-pub-3940256099942544\\/1033173712\"},{\"id\":2455,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"inter_choose\",\"ads_id\":\"ca-app-pub-3940256099942544\\/1033173712\"},{\"id\":2456,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_item\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2465,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_detail\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2466,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_file\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2469,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_intro\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2470,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_language\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2471,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"inter_guide\",\"ads_id\":\"ca-app-pub-3940256099942544\\/1033173712\"},{\"id\":2472,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_per\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2473,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"appopen_resume\",\"ads_id\":\"ca-app-pub-3940256099942544\\/9257395921\"},{\"id\":2474,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_stop_watch\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2475,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_timer\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2476,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_history\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2477,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"inter_welcome_back\",\"ads_id\":\"ca-app-pub-3940256099942544\\/1033173712\"},{\"id\":2478,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"native_crop\",\"ads_id\":\"ca-app-pub-3940256099942544\\/2247696110\"},{\"id\":2479,\"package_name\":null,\"app name\":\"Api test\",\"app_id\":\"ca-app-pub-4973559944609228~2346710863\",\"name\":\"banner\",\"ads_id\":\"ca-app-pub-3940256099942544\\/6300978111\"}]";
    private boolean isSetId = false;
    private int timeOutCallApi = 12000;

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
                    Log.d(TAG, "isSetId = true1");
                    callBack.onReady();
                }else {
                    Log.d(TAG, "xxxxxx1");
                }
            }, timeOutCallApi);
        } else {
            callBack.onReady();
        }
    }

    private void convertJsonIdAdsDefaultToList(String jsonIdAds) {
        try {
            ArrayList<AdsModel> listAdsModel = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(jsonIdAds);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                int id = jsonObject.getInt("id");
                String app_id = jsonObject.getString("app_id");
                String name = jsonObject.getString("name");
                String ads_id = jsonObject.getString("ads_id");

                AdsModel adsModel = new AdsModel(id, app_id, name, ads_id);
                listAdsModel.add(adsModel);

                for (AdsModel ads : listAdsModel) {
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
            }
            Log.d(TAG, "convertJsonIdAdsDefaultToList: " + listAds.size());
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "convertJsonIdAdsDefaultToList: Exception: Invalid json");
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
                            listAds.put(ads.getName().toLowerCase().trim(), listIDAds);
                        }
                        isSetId = true;
                        Log.d(TAG, "isSetId = true2");
                        callBack.onReady();
                    }else {
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
                        Log.d(TAG, "isSetId = true3");
                        callBack.onReady();
                    }else {
                        Log.d(TAG, "xxxxxx3");
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "fetchData: Exception: isSetId: " + isSetId);
            if (!isSetId) {
                convertJsonIdAdsDefaultToList(jsonIdAdsDefault);
                isSetId = true;
                Log.d(TAG, "isSetId = true4");
                callBack.onReady();
            }else {
                Log.d(TAG, "xxxxxx4");
            }
        }
    }

    public void loadOpenAppAdSplashFloor(AppCompatActivity activity, AppOpenCallback appOpenCallback) {
        AppOpenManager.getInstance().loadAndShowAppOpenResumeSplash(activity, AdmobApi.getInstance().getListIDOpenSplash(), appOpenCallback);
    }

    public void loadInterAdSplashFloor(AppCompatActivity activity, InterCallback interCallback) {
        Admob.getInstance().loadAndShowInterAdSplash(activity, AdmobApi.getInstance().getListIDInterSplash(), interCallback);
    }
}
