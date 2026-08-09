package com.amazic.library.update_app;

import static android.app.Activity.RESULT_CANCELED;
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
import android.widget.Toast;

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
    private static UpdateApplicationManager INSTANCE;
    private Dialog dialog;
    private ProgressBar progressBar;
    private TextView tvOk;
    private ActivityResultLauncher<IntentSenderRequest> activityResultLauncher;
    private IonUpdateApplication ionUpdateApplication;
    private int updateType = AppUpdateType.IMMEDIATE;//must update then can use app

    public static UpdateApplicationManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UpdateApplicationManager();
        }
        return INSTANCE;
    }
    // or AppUpdateType.FLEXIBLE: can use app when updating app

    public void setUseImmediateUpdate() {
        this.updateType = AppUpdateType.IMMEDIATE;
    }

    public void setUseFlexibleUpdate() {
        this.updateType = AppUpdateType.FLEXIBLE;
    }

    public void init(AppCompatActivity activity, IonUpdateApplication ionUpdateApplication) {
        this.ionUpdateApplication = ionUpdateApplication;
        activityResultLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    // handle callback
                    activity.runOnUiThread(() -> {
                        if (tvOk != null) {
                            tvOk.setEnabled(true);
                        }
                        if (result.getResultCode() != RESULT_OK) {
                            if (result.getResultCode() == RESULT_CANCELED) {
                                EventTrackingHelper.logEvent(activity, "update_application_not_ok_cancel");
                                Log.d(TAG, "update_application_not_ok_cancel: " + result.getResultCode());
                            } else {
                                EventTrackingHelper.logEvent(activity, "update_application_not_ok_fail");
                                Log.d(TAG, "update_application_not_ok_fail: " + result.getResultCode());
                            }
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            // If the update is canceled or fails,
                            // you can request to start the update again.
                            this.ionUpdateApplication.onUpdateApplicationFail();
                        } else {
                            EventTrackingHelper.logEvent(activity, "update_application_ok");
                            Log.d(TAG, "Update flow success.");
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            if (dialog != null && dialog.isShowing()) {
                                dialog.dismiss();
                            }
                            this.ionUpdateApplication.onUpdateApplicationSuccess();
                        }
                    });
                });
    }

    public AppUpdateManager checkVersionPlayStore(AppCompatActivity activity,
                                                  boolean isForceUpdate,
                                                  boolean isCancelableDialog,
                                                  String title,
                                                  String content,
                                                  String positiveText,
                                                  String negativeText) {
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
                    && appUpdateInfo.isUpdateTypeAllowed(updateType)) {
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
                this.ionUpdateApplication.onMustNotUpdateApplication();
            }
        }).addOnFailureListener(e -> {
            if (e.getMessage() != null) {
                EventTrackingHelper.logEventWithAParam(activity, "request_update_fail", "message", limitString(e.getMessage(), 100));
            } else {
                EventTrackingHelper.logEvent(activity, "request_update_fail");
            }
            Log.d(TAG, "Request the update fail." + e.getMessage());
            this.ionUpdateApplication.requestUpdateFail();
        });
        return appUpdateManager;
    }

    private void initDialogUpdate(AppCompatActivity activity,
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
        tvOk = view.findViewById(R.id.tv_ok);
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
            tvOk.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            AppOpenManager.getInstance().disableAppResumeWithActivity(activity.getClass());
            EventTrackingHelper.logEvent(activity, "start_update_flow_for_result");
            Log.d(TAG, "Start update flow for result.");
            if (activityResultLauncher != null) {
                appUpdateManager.startUpdateFlowForResult(
                        // Pass the intent that is returned by 'getAppUpdateInfo()'.
                        appUpdateInfo,
                        // an activity result launcher registered via registerForActivityResult
                        activityResultLauncher,
                        // Or pass 'AppUpdateType.FLEXIBLE' to newBuilder() for
                        // flexible updates.
                        AppUpdateOptions.newBuilder(updateType).build());
            } else {
                Log.d(TAG, "Call init UpdateApplicationManager first!");
                Toast.makeText(activity, "Call init UpdateApplicationManager first!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private String limitString(String str, int maxLength) {
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    public interface IonUpdateApplication {
        void onUpdateApplicationFail();

        void onUpdateApplicationSuccess();

        void onMustNotUpdateApplication();

        void requestUpdateFail();
    }
}
