package com.amazic.library.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue;

import java.util.ArrayList;
import java.util.Map;

public class RemoteConfigHelper {
    private static final String TAG = "RemoteConfigHelper";
    private static RemoteConfigHelper INSTANCE;
    private ArrayList<String> listRemoteStringName = new ArrayList<>();
    private ArrayList<String> listRemoteBooleanName = new ArrayList<>();
    private ArrayList<String> listRemoteLongName = new ArrayList<>();

    public static RemoteConfigHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RemoteConfigHelper();
        }
        return INSTANCE;
    }

    public void fetchAllKeysAndTypes(Context context) {
        FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        firebaseRemoteConfig.reset();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        FirebaseRemoteConfig.getInstance().setConfigSettingsAsync(configSettings);

        FirebaseRemoteConfig.getInstance().fetchAndActivate().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Map<String, FirebaseRemoteConfigValue> allValues = firebaseRemoteConfig.getAll();
                for (Map.Entry<String, FirebaseRemoteConfigValue> entry : allValues.entrySet()) {
                    String key = entry.getKey();
                    FirebaseRemoteConfigValue value = entry.getValue();
                    String valueType = determineValueType(value);
                    switch (valueType) {
                        case "String":
                            listRemoteStringName.clear();
                            listRemoteStringName.add(key);
                            break;
                        case "Boolean":
                            listRemoteBooleanName.clear();
                            listRemoteBooleanName.add(key);
                            break;
                        case "Long":
                            listRemoteLongName.clear();
                            listRemoteLongName.add(key);
                            break;
                        default:
                            break;
                    }
                    Log.d(TAG, "Key: " + key + ", Type: " + valueType);
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
            } else {
                Log.d(TAG, "Failed to fetch Remote Config values.");
            }
        });
    }

    private String determineValueType(FirebaseRemoteConfigValue value) {
        try {
            value.asString();
            return "String";
        } catch (Exception ignored) {
        }

        try {
            value.asBoolean();
            return "Boolean";
        } catch (Exception ignored) {
        }

        try {
            value.asLong();
            return "Long";
        } catch (Exception ignored) {
        }

        return "Unknown";
    }

    public boolean getRemoteConfigBoolean(String adUnitId) {
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        return mFirebaseRemoteConfig.getBoolean(adUnitId);
    }

    public long getRemoteConfigLong(String adUnitId) {
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        return mFirebaseRemoteConfig.getLong(adUnitId);
    }

    public String getRemoteConfigString(String adUnitId) {
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        return mFirebaseRemoteConfig.getString(adUnitId);
    }

    public boolean get_config(Context context, String name_config) {
        SharedPreferences pre = context.getSharedPreferences("remote_fill", Context.MODE_PRIVATE);
        return pre.getBoolean(name_config, true);
    }

    public void set_config(Context context, String name_config, boolean config) {
        SharedPreferences pre = context.getSharedPreferences("remote_fill", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putBoolean(name_config, config);
        editor.apply();
    }

    public String get_config_string(Context context, String name_config) {
        SharedPreferences pre = context.getSharedPreferences("remote_fill", Context.MODE_PRIVATE);
        return pre.getString(name_config, "0_100");
    }

    public void set_config_string(Context context, String name_config, String config) {
        SharedPreferences pre = context.getSharedPreferences("remote_fill", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putString(name_config, config);
        editor.apply();
    }

    public void set_config_long(Context context, String name_config, Long config) {
        SharedPreferences pre = context.getSharedPreferences("remote_fill", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putLong(name_config, config);
        editor.apply();
    }

    public Long get_config_long(Context context, String name_config) {
        SharedPreferences pre = context.getSharedPreferences("remote_fill", Context.MODE_PRIVATE);
        return pre.getLong(name_config, 0);
    }
}
