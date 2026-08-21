package com.amazic.library.Utils;

import android.content.Context;

import com.amazic.library.ads.call_api.AdsModel;
import com.google.gson.Gson;

import java.util.List;

import javax.annotation.Nullable;

public class IDRemoteConfigHelper {
    private IDRemoteConfigHelper() {
    }

    public static boolean isUsingIdDebug = true;
    public static boolean isTurnOnAds = true;

    private static final String NATIVE_ID_TEST = "ca-app-pub-3940256099942544/2247696110";
    private static final String INTER_ID_TEST = "ca-app-pub-3940256099942544/1033173712";
    private static final String RESUME_ID_TEST = "ca-app-pub-3940256099942544/9257395921";
    private static final String BANNER_ID_TEST = "ca-app-pub-3940256099942544/9214589741";
    private static final String REWARD_ID_TEST = "ca-app-pub-3940256099942544/5224354917";
    private static final String COLLAPSE_ID_TEST = "ca-app-pub-3940256099942544/2014213617";

    @Nullable
    public static String getID(Context context, String key) {
        RemoteConfigHelper remoteConfigHelper = new RemoteConfigHelper();
        if (isUsingIdDebug) {
            if (key.toLowerCase().startsWith("id_native") || key.toLowerCase().startsWith("native")) {
                return NATIVE_ID_TEST;
            }
            if (key.toLowerCase().startsWith("id_inter") || key.toLowerCase().startsWith("inter")) {
                return INTER_ID_TEST;
            }
            if (key.toLowerCase().startsWith("id_resume") || key.toLowerCase().startsWith("resume")) {
                return RESUME_ID_TEST;
            }
            if (key.toLowerCase().startsWith("id_open") || key.toLowerCase().startsWith("open")) {
                return RESUME_ID_TEST;
            }
            if (key.toLowerCase().startsWith("id_banner") || key.toLowerCase().startsWith("banner")) {
                return BANNER_ID_TEST;
            }
            if (key.toLowerCase().startsWith("id_reward") || key.toLowerCase().startsWith("reward")) {
                return REWARD_ID_TEST;
            }
            if (key.toLowerCase().startsWith("id_collapse") || key.toLowerCase().startsWith("collapse")) {
                return COLLAPSE_ID_TEST;
            }
        } else if (isTurnOnAds) {
            String adsId = remoteConfigHelper.get_config_string(context, key);
            if (adsId != null) {
                return adsId;
            } else {
                return remoteConfigHelper.get_config_string(context, key + "_default");
            }
        }
        return null;
    }

    public static void setUpDefaultValue(Context context, String jsonDefault) {
        RemoteConfigHelper remoteConfigHelper = new RemoteConfigHelper();

        if (remoteConfigHelper.get_config(context, "is_set_default", false)) return;

        Gson gson = new Gson();
        List<AdsModel> ids = gson.fromJson(jsonDefault, new com.google.gson.reflect.TypeToken<List<AdsModel>>() {
        });
        ids.forEach(adsModel -> {
            String nameIdDefault = adsModel.getName() + "_default";
            if (!nameIdDefault.startsWith("id_")) nameIdDefault = "id_" + nameIdDefault;
            remoteConfigHelper.set_config_string(context, nameIdDefault, adsModel.getAds_id());
        });
        remoteConfigHelper.set_config(context, "is_set_default", true);
    }
}
