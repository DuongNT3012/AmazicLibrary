package com.amazic.library.update_app;

import static android.app.Activity.RESULT_OK;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.amazic.library.Utils.EventTrackingHelper;
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
    private static final String TAG = "UpdateApplicationManager";
    private static Dialog dialog;
    private static ProgressBar progressBar;
    public static void checkVersionPlayStore(AppCompatActivity activity,
                                             boolean isForceUpdate,
                                             boolean isCancelableDialog,
                                             IonUpdateApplication ionUpdateApplication,
                                             String title,
                                             String content,
                                             String positiveText,
                                             String negativeText) {
        ActivityResultLauncher<IntentSenderRequest> activityResultLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        // handle callback
                        activity.runOnUiThread(() -> {
                            if (result.getResultCode() != RESULT_OK) {
                                EventTrackingHelper.logEvent(activity, "update_application_not_ok_" + result.getResultCode());
                                progressBar.setVisibility(View.GONE);
                                Log.d(TAG, "Update flow failed! Result code: " + result.getResultCode());
                                // If the update is canceled or fails,
                                // you can request to start the update again.
                                ionUpdateApplication.onUpdateApplicationFail();
                            } else {
                                EventTrackingHelper.logEvent(activity, "update_application_ok");
                                Log.d(TAG, "Update flow success.");
                                progressBar.setVisibility(View.GONE);
                                dialog.dismiss();
                                ionUpdateApplication.onUpdateApplicationSuccess();
                            }
                        });
                    }
                });
        EventTrackingHelper.logEvent(activity, "check_version_play_store");
        Log.d(TAG, "Check version play store.");
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(activity);

        // Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    // This example applies an immediate update. To apply a flexible update
                    // instead, pass in AppUpdateType.FLEXIBLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                // Request the update.
                EventTrackingHelper.logEvent(activity, "update_available");
                Log.d(TAG, "Update available.");
                initDialogUpdate(activity,
                        activityResultLauncher,
                        isForceUpdate,
                        appUpdateManager,
                        appUpdateInfo,
                        isCancelableDialog,
                        title,
                        content,
                        positiveText,
                        negativeText);
            } else {
                EventTrackingHelper.logEvent(activity, "update_not_available");
                Log.d(TAG, "Update not available.");
                ionUpdateApplication.onMustNotUpdateApplication();
            }
        }).addOnFailureListener(e -> {
            EventTrackingHelper.logEvent(activity, "request_update_fail");
            Log.d(TAG, "Request the update fail." + e.getMessage());
            ionUpdateApplication.requestUpdateFail();
        });
    }

    private static void initDialogUpdate(AppCompatActivity activity,
                                         ActivityResultLauncher<IntentSenderRequest> activityResultLauncher,
                                         boolean isForceUpdate,
                                         AppUpdateManager appUpdateManager,
                                         AppUpdateInfo appUpdateInfo,
                                         boolean isCancelableDialog,
                                         String title,
                                         String content,
                                         String positiveText,
                                         String negativeText) {
        dialog = new Dialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_update_app, null, false);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);
        dialog.setCancelable(isCancelableDialog);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.8);
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }

        TextView tvTitle = view.findViewById(R.id.tv_title);
        TextView tvContent = view.findViewById(R.id.tv_content);
        TextView tvNo = view.findViewById(R.id.tv_no);
        TextView tvOk = view.findViewById(R.id.tv_ok);
        progressBar = view.findViewById(R.id.progress_bar);

        tvTitle.setText(title);
        tvContent.setText(content);
        tvNo.setText(negativeText);
        tvOk.setText(positiveText);

        if (isForceUpdate) {
            tvNo.setVisibility(View.GONE);
        }

        tvNo.setOnClickListener(v -> dialog.dismiss());
        tvOk.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            AppOpenManager.getInstance().disableAppResumeWithActivity(activity.getClass());
            EventTrackingHelper.logEvent(activity, "start_update_flow_for_result");
            Log.d(TAG, "Start update flow for result.");
            appUpdateManager.startUpdateFlowForResult(
                    // Pass the intent that is returned by 'getAppUpdateInfo()'.
                    appUpdateInfo,
                    // an activity result launcher registered via registerForActivityResult
                    activityResultLauncher,
                    // Or pass 'AppUpdateType.FLEXIBLE' to newBuilder() for
                    // flexible updates.
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build());
        });

        dialog.show();
    }

    public interface IonUpdateApplication {
        void onUpdateApplicationFail();

        void onUpdateApplicationSuccess();

        void onMustNotUpdateApplication();

        void requestUpdateFail();
    }
}
