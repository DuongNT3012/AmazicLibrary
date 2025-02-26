package com.amazic.library.update_app;

import static android.app.Activity.RESULT_OK;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.amazic.library.ads.app_open_ads.AppOpenManager;
import com.amazic.mylibrary.R;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;

public class UpdateApplicationManager {
    private static final String TAG = "UpdateAppManager";

    public static void checkVersionPlayStore(AppCompatActivity activity, boolean isForceUpdate, boolean isCancelableDialog, boolean isDirectToStore) {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(activity);

        // Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    // This example applies an immediate update. To apply a flexible update
                    // instead, pass in AppUpdateType.FLEXIBLE
                    /*&& appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)*/) {
                // Request the update.
                Log.d(TAG, "Request the update.");
                showDialogUpdate(activity, isForceUpdate, appUpdateManager, appUpdateInfo, isCancelableDialog, isDirectToStore);
            }
        }).addOnFailureListener(e -> {
            Log.d(TAG, "Request the update fail." + e.getMessage());
        });
    }

    private static void showDialogUpdate(AppCompatActivity activity, boolean isForceUpdate, AppUpdateManager appUpdateManager, AppUpdateInfo appUpdateInfo, boolean isCancelableDialog, boolean isDirectToStore) {
        Dialog dialog = new Dialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_update_app, null, false);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);
        dialog.setCancelable(isCancelableDialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        int width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.8);
        int height = ViewGroup.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setLayout(width, height);

        TextView tvNo = view.findViewById(R.id.tv_no);
        TextView tvOk = view.findViewById(R.id.tv_ok);

        if (isForceUpdate) {
            tvNo.setVisibility(View.GONE);
        }

        ActivityResultLauncher<IntentSenderRequest> activityResultLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        // handle callback
                        if (result.getResultCode() != RESULT_OK) {
                            Log.d(TAG, "Update flow failed! Result code: " + result.getResultCode());
                            // If the update is canceled or fails,
                            // you can request to start the update again.
                        } else {
                            dialog.dismiss();
                        }
                    }
                });

        tvNo.setOnClickListener(v -> dialog.dismiss());
        tvOk.setOnClickListener(v -> {
            AppOpenManager.getInstance().disableAppResumeWithActivity(activity.getClass());
            if (isDirectToStore) {
                try {
                    String packageName = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).packageName;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                    activity.startActivity(intent);
                } catch (Exception e) {
                    Log.d(TAG, "Open play store fail." + e.getMessage());
                }
            } else {
                appUpdateManager.startUpdateFlowForResult(
                        // Pass the intent that is returned by 'getAppUpdateInfo()'.
                        appUpdateInfo,
                        // an activity result launcher registered via registerForActivityResult
                        activityResultLauncher,
                        // Or pass 'AppUpdateType.FLEXIBLE' to newBuilder() for
                        // flexible updates.
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build());
            }
        });

        dialog.show();
    }
}
