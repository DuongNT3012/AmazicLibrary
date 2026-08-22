package com.amazic.library.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.amazic.mylibrary.R;
import com.google.firebase.remoteconfig.ConfigUpdate;
import com.google.firebase.remoteconfig.ConfigUpdateListener;
import com.google.firebase.remoteconfig.ConfigUpdateListenerRegistration;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue;

import java.util.ArrayList;
import java.util.Map;

public class RemoteConfigHelper {
    private static final String TAG = "RemoteConfigHelper";
    public static String id_ads = "id_ads";
    public static String show_all_ads = "show_all_ads";
    public static String interval_between_interstitial = "interval_between_interstitial";
    public static String interval_interstitial_from_start = "interval_interstitial_from_start";
    public static String rate_aoa_inter_splash = "rate_aoa_inter_splash";
    public static String interval_reload_native = "interval_reload_native";
    public static String banner_splash = "banner_splash";
    public static String open_splash = "open_splash";
    public static String inter_splash = "inter_splash";
    public static String native_language = "native_language";
    public static String native_interest = "native_interest";
    public static String native_intro = "native_intro";
    public static String native_intro_full = "native_intro_full";
    public static String inter_intro = "inter_intro";
    public static String native_permission = "native_permission";
    public static String banner_all = "banner_all";
    public static String resume_wb = "resume_wb";
    public static String native_wb = "native_wb";
    private static RemoteConfigHelper INSTANCE;
    private final ArrayList<String> listRemoteStringName = new ArrayList<>();
    private final ArrayList<String> listRemoteBooleanName = new ArrayList<>();
    private final ArrayList<String> listRemoteLongName = new ArrayList<>();
    private ConfigUpdateListenerRegistration configUpdateListenerRegistration;

    public static RemoteConfigHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RemoteConfigHelper();
        }
        return INSTANCE;
    }

    private static String determineValueType(String value) {
        if (value == null || value.isEmpty()) {
            return "String";
        }
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return "Boolean";
        }
        try {
            Long.parseLong(value);
            return "Long";
        } catch (NumberFormatException ignored) {
        }
        try {
            Double.parseDouble(value);
            return "Double";
        } catch (NumberFormatException ignored) {
        }
        return "String";
    }

    public void fetchAllKeysAndTypes(Context context, IOnFetchDone iOnFetchDone) {
        FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(60)
                .build();
        FirebaseRemoteConfig.getInstance().setConfigSettingsAsync(configSettings);
        FirebaseRemoteConfig.getInstance().setDefaultsAsync(R.xml.remote_config_defaults);

        FirebaseRemoteConfig.getInstance().fetchAndActivate().addOnCompleteListener(task -> {
            boolean isSuccess = false;
            if (task.isSuccessful()) {
                Map<String, FirebaseRemoteConfigValue> allValues = firebaseRemoteConfig.getAll();
                listRemoteStringName.clear();
                listRemoteBooleanName.clear();
                listRemoteLongName.clear();
                for (Map.Entry<String, FirebaseRemoteConfigValue> entry : allValues.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue().asString();
                    String valueType = determineValueType(value);
                    switch (valueType) {
                        case "String":
                            listRemoteStringName.add(key);
                            break;
                        case "Boolean":
                            listRemoteBooleanName.add(key);
                            break;
                        case "Long":
                            listRemoteLongName.add(key);
                            break;
                        default:
                            break;
                    }
                    Log.d(TAG, "Key: " + key + ", Value: " + value + ", Type: " + valueType);
                }
                //if have a change from remote config
                if (task.getResult()) {
                    for (String key : listRemoteStringName) {
                        set_config_string(context, key, getRemoteConfigString(key));
                    }
                    for (String key : listRemoteBooleanName) {
                        set_config(context, key, getRemoteConfigBoolean(key));
                    }
                    for (String key : listRemoteLongName) {
                        set_config_long(context, key, getRemoteConfigLong(key));
                    }
                }
                isSuccess = true;
                if (configUpdateListenerRegistration == null) {
                    listenForConfigUpdates(context, null);
                }
            } else {
                Log.d(TAG, "Failed to fetch Remote Config values.");
            }
            iOnFetchDone.onFetchDone(isSuccess);
        });
    }

    public void listenForConfigUpdates(Context context, IOnFetchDone iOnFetchDone) {
        FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        if (configUpdateListenerRegistration != null) {
            configUpdateListenerRegistration.remove();
        }
        configUpdateListenerRegistration = firebaseRemoteConfig.addOnConfigUpdateListener(new ConfigUpdateListener() {
            @Override
            public void onUpdate(@NonNull ConfigUpdate configUpdate) {
                Log.d(TAG, "Updated keys: " + configUpdate.getUpdatedKeys());

                firebaseRemoteConfig.activate().addOnCompleteListener(task -> {
                    boolean isSuccess = task.isSuccessful();
                    if (isSuccess) {
                        for (String key : configUpdate.getUpdatedKeys()) {
                            String value = firebaseRemoteConfig.getValue(key).asString();
                            String valueType = determineValueType(value);
                            switch (valueType) {
                                case "String":
                                    set_config_string(context, key, firebaseRemoteConfig.getString(key));
                                    break;
                                case "Boolean":
                                    set_config(context, key, firebaseRemoteConfig.getBoolean(key));
                                    break;
                                case "Long":
                                    set_config_long(context, key, firebaseRemoteConfig.getLong(key));
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                    if (iOnFetchDone != null) {
                        iOnFetchDone.onFetchDone(isSuccess);
                    }
                });
            }

            @Override
            public void onError(@NonNull FirebaseRemoteConfigException error) {
                Log.e(TAG, "Config update error", error);
                if (iOnFetchDone != null) {
                    iOnFetchDone.onFetchDone(false);
                }
            }
        });
    }

    /**
     * Gọi khi không cần lắng nghe nữa (VD: onDestroy) để tránh leak listener.
     */
    public void removeConfigUpdateListener() {
        if (configUpdateListenerRegistration != null) {
            configUpdateListenerRegistration.remove();
            configUpdateListenerRegistration = null;
        }
    }
    private boolean getRemoteConfigBoolean(String adUnitId) {
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        return mFirebaseRemoteConfig.getBoolean(adUnitId);
    }

    private long getRemoteConfigLong(String adUnitId) {
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        return mFirebaseRemoteConfig.getLong(adUnitId);
    }

    private String getRemoteConfigString(String adUnitId) {
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        return mFirebaseRemoteConfig.getString(adUnitId);
    }

    public boolean get_config(Context context, String name_config) {
        SharedPreferences pre = context.getSharedPreferences("remote_fill", Context.MODE_PRIVATE);
        return pre.getBoolean(name_config, true);
    }
    public boolean get_config(Context context, String name_config, boolean defaultValue) {
        SharedPreferences pre = context.getSharedPreferences("remote_fill", Context.MODE_PRIVATE);
        return pre.getBoolean(name_config, defaultValue);
    }

    public void set_config(Context context, String name_config, boolean config) {
        SharedPreferences pre = context.getSharedPreferences("remote_fill", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putBoolean(name_config, config);
        editor.apply();
    }

    public void set_config_commit(Context context, String name_config, boolean config) {
        SharedPreferences pre = context.getSharedPreferences("remote_fill", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putBoolean(name_config, config);
        editor.commit();
    }

    public String get_config_string(Context context, String name_config) {
        SharedPreferences pre = context.getSharedPreferences("remote_fill", Context.MODE_PRIVATE);
        return pre.getString(name_config, null);
    }

    public String get_config_string(Context context, String name_config, String defaultValue) {
        SharedPreferences pre = context.getSharedPreferences("remote_fill", Context.MODE_PRIVATE);
        return pre.getString(name_config, defaultValue);
    }

    public void set_config_string(Context context, String name_config, String config) {
        SharedPreferences pre = context.getSharedPreferences("remote_fill", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putString(name_config, config);
        editor.apply();
    }

    public void set_config_string_commit(Context context, String name_config, String config) {
        SharedPreferences pre = context.getSharedPreferences("remote_fill", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putString(name_config, config);
        editor.commit();
    }

    public void set_config_long(Context context, String name_config, Long config) {
        SharedPreferences pre = context.getSharedPreferences("remote_fill", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putLong(name_config, config);
        editor.apply();
    }

    public void set_config_long_commit(Context context, String name_config, Long config) {
        SharedPreferences pre = context.getSharedPreferences("remote_fill", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putLong(name_config, config);
        editor.commit();
    }

    public Long get_config_long(Context context, String name_config) {
        SharedPreferences pre = context.getSharedPreferences("remote_fill", Context.MODE_PRIVATE);
        return pre.getLong(name_config, 0);
    }
    public interface IOnFetchDone {
        void onFetchDone(boolean isSuccess);
    }
}
